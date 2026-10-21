import { digest, CREATOR_UID } from './profiles.mjs';
import { recoverDevice } from './device.mjs';

const CHUNK = 49152,
  MAX_FILE = 64 * 1024 * 1024,
  MAX_TOTAL = 128 * 1024 * 1024;
const reply = (value, status = 200) =>
  new Response(JSON.stringify(value), {
    status,
    headers: { 'content-type': 'application/json', 'cache-control': 'no-store' },
  });
const hashPattern = /^[a-f0-9]{64}$/;
export function cosmeticKey(key) {
  return /^(accent(?:_custom|_gradient_\w+)?|like_color|comment_like_color|font|font_emoji|avatar_shape|loading_style|fps_lock|profile_layout|repost_pos|seekbar|dates|dim(?:_\w+)?|island_\w+|nav_\w+|navigation_\w+|theme_\w+|appearance_color_\w+|profile_(?:gradient|nickname|badge|crown)_\w+|profile_display_name|rail_\w+)$/.test(
    key,
  );
}
export function validateSnapshot(value) {
  if (!value || value.schema !== 1 || !Array.isArray(value.files) || value.files.length > 32)
    throw new Error('invalid_snapshot');
  const clean = { schema: 1, global: {}, account: {}, files: [] };
  for (const scope of ['global', 'account']) {
    const values = value[scope];
    if (
      !values ||
      typeof values !== 'object' ||
      Array.isArray(values) ||
      Object.keys(values).length > 256
    )
      throw new Error('invalid_settings');
    for (const [key, entry] of Object.entries(values)) {
      if (
        !cosmeticKey(key) ||
        !entry ||
        !['string', 'boolean', 'int', 'long', 'float'].includes(entry.type)
      )
        throw new Error('invalid_setting');
      const v = entry.value;
      if (
        entry.type === 'string'
          ? typeof v !== 'string' || v.length > 16384
          : entry.type === 'boolean'
            ? typeof v !== 'boolean'
            : !Number.isFinite(v) ||
              Math.abs(v) > Number.MAX_SAFE_INTEGER ||
              (entry.type !== 'float' && !Number.isInteger(v))
      )
        throw new Error('invalid_value');
      if (entry.type === 'int' && (v < -2147483648 || v > 2147483647))
        throw new Error('invalid_value');
      clean[scope][key] = { type: entry.type, value: v };
    }
  }
  let total = 0;
  const names = new Set();
  for (const file of value.files) {
    if (
      !file ||
      !/^[A-Za-z0-9_-][A-Za-z0-9_.\/-]{0,150}$/.test(file.name) ||
      file.name.split('/').some((p) => !p || p === '.' || p === '..') ||
      names.has(file.name) ||
      !hashPattern.test(file.hash) ||
      !Number.isInteger(file.size) ||
      file.size <= 0 ||
      file.size > MAX_FILE
    )
      throw new Error('invalid_file');
    total += file.size;
    if (total > MAX_TOTAL) throw new Error('too_large');
    names.add(file.name);
    clean.files.push({ name: file.name, hash: file.hash, size: file.size });
  }
  if (JSON.stringify(clean).length > 131072) throw new Error('too_large');
  return clean;
}
async function body(request) {
  if (Number(request.headers.get('content-length')) > 196608) throw new Error('too_large');
  const reader = request.body?.getReader();
  if (!reader) throw new Error('invalid_body');
  const parts = [];
  let size = 0;
  for (;;) {
    const { value, done } = await reader.read();
    if (done) break;
    size += value.length;
    if (size > 196608) {
      await reader.cancel();
      throw new Error('too_large');
    }
    parts.push(value);
  }
  const bytes = new Uint8Array(size);
  let at = 0;
  for (const p of parts) {
    bytes.set(p, at);
    at += p.length;
  }
  return JSON.parse(new TextDecoder().decode(bytes));
}
export async function appearanceRoute(request, env) {
  if (new URL(request.url).pathname !== '/v1/appearance') return null;
  if (request.method !== 'POST') return reply({ error: 'method_not_allowed' }, 405);
  if (!env.PROFILES) return reply({ error: 'storage_unavailable' }, 503);
  let data;
  try {
    data = await body(request);
  } catch (error) {
    return reply({ error: error.message === 'too_large' ? 'too_large' : 'invalid_body' }, 400);
  }
  if (
    !data ||
    !/^[0-9]{1,24}$/.test(data.uid || '') ||
    !/^[a-f0-9]{64}$/.test(data.token || '') ||
    !['read', 'prepare', 'save', 'chunk', 'file'].includes(data.action)
  )
    return reply({ error: 'invalid_request' }, 400);
  const hash = await digest(data.token);
  let owner = await env.PROFILES.prepare('SELECT owner_hash,verified FROM profiles WHERE uid=?')
    .bind(data.uid)
    .first();
  owner = await recoverDevice(env, data.uid, hash, data.device, owner);
  if (owner && owner.owner_hash !== hash) return reply({ error: 'verify_required' }, 403);
  if (data.uid === CREATOR_UID && !owner?.verified) return reply({ error: 'verify_required' }, 403);
  if (!owner) {
    if (data.action === 'read') return reply({ ok: true, snapshot: null });
    await env.PROFILES.prepare(
      'INSERT OR IGNORE INTO profiles(uid,owner_hash,verified,updated_at) VALUES(?,?,0,?)',
    )
      .bind(data.uid, hash, Date.now())
      .run();
    owner = await env.PROFILES.prepare('SELECT owner_hash,verified FROM profiles WHERE uid=?')
      .bind(data.uid)
      .first();
    if (owner?.owner_hash !== hash) return reply({ error: 'verify_required' }, 403);
  }
  if (data.action === 'read') {
    const saved = await env.PROFILES.prepare(
      'SELECT snapshot,updated_at FROM appearance_backups WHERE uid=?',
    )
      .bind(data.uid)
      .first();
    return reply({
      ok: true,
      verified: !!owner.verified,
      snapshot: saved ? JSON.parse(saved.snapshot) : null,
      updatedAt: saved?.updated_at || 0,
    });
  }
  if (data.action === 'prepare') {
    let snapshot;
    try {
      snapshot = validateSnapshot(data.snapshot);
    } catch (error) {
      return reply({ error: error.message }, 400);
    }
    const active = await env.PROFILES.prepare('SELECT snapshot FROM appearance_backups WHERE uid=?')
      .bind(data.uid)
      .first();
    const keep = [
      ...new Set(
        [...snapshot.files, ...(active ? JSON.parse(active.snapshot).files : [])].map(
          (f) => f.hash,
        ),
      ),
    ];
    if (keep.length)
      await env.PROFILES.prepare(
        `DELETE FROM appearance_files WHERE uid=? AND hash NOT IN (${keep.map(() => '?').join(',')})`,
      )
        .bind(data.uid, ...keep)
        .run();
    else
      await env.PROFILES.prepare('DELETE FROM appearance_files WHERE uid=?').bind(data.uid).run();
    await env.PROFILES.prepare(
      'INSERT INTO appearance_staging(uid,snapshot,updated_at) VALUES(?,?,?) ON CONFLICT(uid) DO UPDATE SET snapshot=excluded.snapshot,updated_at=excluded.updated_at',
    )
      .bind(data.uid, JSON.stringify(snapshot), Date.now())
      .run();
    const complete = [];
    for (const file of snapshot.files) {
      const count = await env.PROFILES.prepare(
        'SELECT COUNT(*) AS parts,SUM(length(data)) AS size,MIN(part) AS lo,MAX(part) AS hi FROM appearance_files WHERE uid=? AND hash=?',
      )
        .bind(data.uid, file.hash)
        .first();
      if (
        count?.parts === Math.ceil(file.size / CHUNK) &&
        count.size === file.size &&
        count.lo === 0 &&
        count.hi === count.parts - 1
      )
        complete.push(file.hash);
    }
    return reply({ ok: true, complete });
  }
  if (data.action === 'save') {
    let snapshot;
    try {
      snapshot = validateSnapshot(data.snapshot);
    } catch (error) {
      return reply({ error: error.message }, 400);
    }
    for (const file of snapshot.files) {
      const actual = await env.PROFILES.prepare(
        'SELECT COUNT(*) AS parts,SUM(length(data)) AS size,MIN(part) AS lo,MAX(part) AS hi FROM appearance_files WHERE uid=? AND hash=?',
      )
        .bind(data.uid, file.hash)
        .first();
      const count = Math.ceil(file.size / CHUNK);
      if (
        actual?.parts !== count ||
        actual.size !== file.size ||
        actual.lo !== 0 ||
        actual.hi !== count - 1
      )
        return reply({ error: 'incomplete_files' }, 409);
    }
    const now = Date.now();
    await env.PROFILES.prepare(
      'INSERT INTO appearance_backups(uid,snapshot,updated_at) VALUES(?,?,?) ON CONFLICT(uid) DO UPDATE SET snapshot=excluded.snapshot,updated_at=excluded.updated_at',
    )
      .bind(data.uid, JSON.stringify(snapshot), now)
      .run();
    return reply({ ok: true, updatedAt: now });
  }
  if (
    !hashPattern.test(data.hash || '') ||
    !Number.isInteger(data.part) ||
    data.part < 0 ||
    data.part >= Math.ceil(MAX_FILE / CHUNK)
  )
    return reply({ error: 'invalid_file' }, 400);
  if (data.action === 'file') {
    const active = await env.PROFILES.prepare('SELECT snapshot FROM appearance_backups WHERE uid=?')
      .bind(data.uid)
      .first();
    const file = active
      ? JSON.parse(active.snapshot).files.find((f) => f.hash === data.hash)
      : null;
    if (!file || data.part >= Math.ceil(file.size / CHUNK))
      return reply({ error: 'not_found' }, 404);
    const row = await env.PROFILES.prepare(
      'SELECT data FROM appearance_files WHERE uid=? AND hash=? AND part=?',
    )
      .bind(data.uid, data.hash, data.part)
      .first();
    if (!row) return reply({ error: 'not_found' }, 404);
    const bytes = new Uint8Array(row.data);
    return reply({ ok: true, data: btoa(String.fromCharCode(...bytes)) });
  }
  if (typeof data.data !== 'string' || data.data.length > 65536)
    return reply({ error: 'invalid_chunk' }, 400);
  const staging = await env.PROFILES.prepare('SELECT snapshot FROM appearance_staging WHERE uid=?')
    .bind(data.uid)
    .first();
  const planned = staging
    ? JSON.parse(staging.snapshot).files.find((f) => f.hash === data.hash)
    : null;
  if (!planned || data.part >= Math.ceil(planned.size / CHUNK))
    return reply({ error: 'invalid_chunk' }, 400);
  let bytes;
  try {
    bytes = Uint8Array.from(atob(data.data), (c) => c.charCodeAt(0));
  } catch {
    return reply({ error: 'invalid_chunk' }, 400);
  }
  if (bytes.length !== Math.min(CHUNK, planned.size - data.part * CHUNK))
    return reply({ error: 'invalid_chunk' }, 400);
  await env.PROFILES.prepare(
    'INSERT OR IGNORE INTO appearance_files(uid,hash,part,data) VALUES(?,?,?,?)',
  )
    .bind(data.uid, data.hash, data.part, bytes.buffer)
    .run();
  return reply({ ok: true });
}
