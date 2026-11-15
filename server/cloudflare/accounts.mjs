// Store page profiles of Google accounts: a unique @username, a display name and an avatar.
//
// The username is what identifies a person in reviews and in the queue: lower-case latin letters, digits, "_" and
// ".", 3 to 24 long, never two alike (case does not matter). The display name is free text. The avatar is a small
// picture the page makes (at most 48 KB), served with a long cache under a URL that changes with it.
import {googleUser, issueSession} from './google.mjs';
import {digest} from './profiles.mjs';

const TABLE = 'CREATE TABLE IF NOT EXISTS web_users(uid TEXT PRIMARY KEY,username TEXT UNIQUE COLLATE NOCASE,display TEXT NOT NULL,avatar TEXT,updated INTEGER NOT NULL)';
const NAME = /^[a-z0-9](?:[a-z0-9_.]{1,22})[a-z0-9]$/;
const RESERVED = new Set(['admin', 'administrator', 'moderator', 'mod', 'ttcuz', 'support', 'system', 'root', 'google', 'tiktok', 'null', 'undefined', 'me', 'help', 'official', 'store', 'review', 'plugins']);
let made = false;
async function table(env) { if (!made) { await env.PROFILES.prepare(TABLE).run(); made = true; } }

const clean = text => String(text || '').replace(/[\u0000-\u001f\u007f<>]/g, '').replace(/\s+/g, ' ').trim();

/** Why a username cannot be taken, or '' when it is fine. */
export function usernameProblem(name) {
  if (!NAME.test(name) || /[_.]{2}/.test(name)) return 'От 3 до 24 символов: латинские буквы, цифры, «_» и «.», не в начале и не в конце, не два подряд.';
  if (RESERVED.has(name) || name.startsWith('ttcuz')) return 'Это имя занято системой.';
  return '';
}

/** The public face of a profile: what reviews and the queue show. */
export function face(row) {
  if (!row) return null;
  return {username: row.username || '', display: row.display || '', avatar: row.avatar ? '/plugins/web/avatar/' + row.username + '?v=' + row.updated : ''};
}

/** The signed-in person with their profile (made on first sign-in from the Google name), or null. */
export async function webAccount(request, env) {
  const user = await googleUser(request, env);
  if (!user) return null;
  await table(env);
  let row = await env.PROFILES.prepare('SELECT uid,username,display,avatar IS NOT NULL AS avatar,updated FROM web_users WHERE uid=?').bind(user.uid).first();
  if (!row) {
    row = {uid: user.uid, username: null, display: clean(user.name).slice(0, 40) || 'Пользователь', avatar: 0, updated: Date.now()};
    await env.PROFILES.prepare('INSERT OR IGNORE INTO web_users(uid,username,display,updated) VALUES(?,?,?,?)').bind(row.uid, null, row.display, row.updated).run();
  }
  return {...user, profile: row};
}

export async function accountRoute(request, env, path, reply) {
  if (path === '/plugins/web/me' && request.method === 'POST') {
    const who = await webAccount(request, env);
    if (!who) return reply({error: 'sign_in', hint: 'Войдите через Google заново.'}, 401);
    // a Google sign-in, or a session in its last week, gets a (new) 30-day session of the site's own
    const renew = !who.session || who.until - Date.now() < 7 * 86400000;
    const session = renew ? await issueSession(env, who) : null;
    return reply({...face(who.profile), google: who.picture, fresh: !who.profile.username, ...(session ? {session: session.session, sessionUntil: session.until} : {})}, 200, 'no-store');
  }
  const check = path.match(/^\/plugins\/web\/username\/([^/]{1,40})$/);
  if (check && request.method === 'GET') {
    // is this username free: the page asks once the person stops typing
    const name = decodeURIComponent(check[1]).toLowerCase();
    const problem = usernameProblem(name);
    if (problem) return reply({free: false, hint: problem}, 200, 'public, max-age=60');
    await table(env);
    const row = await env.PROFILES.prepare('SELECT uid FROM web_users WHERE username=?').bind(name).first();
    return reply({free: !row, hint: row ? 'Это имя уже занято.' : ''}, 200, 'no-store');
  }
  const picture = path.match(/^\/plugins\/web\/avatar\/([a-z0-9_.]{3,24})$/);
  if (picture && request.method === 'GET') {
    await table(env);
    const row = await env.PROFILES.prepare('SELECT avatar FROM web_users WHERE username=?').bind(picture[1]).first();
    if (!row?.avatar) return reply({error: 'not_found'}, 404);
    const bytes = Uint8Array.from(atob(row.avatar), c => c.charCodeAt(0));
    const type = bytes[0] === 0xFF ? 'image/jpeg' : bytes[0] === 137 ? 'image/png' : 'image/webp';
    // the URL carries ?v=<when it changed>, so a browser keeps it for a year and never asks again
    return new Response(bytes, {headers: {'content-type': type, 'cache-control': 'public, max-age=31536000, immutable', 'access-control-allow-origin': '*'}});
  }
  if (path === '/plugins/web/profile' && request.method === 'POST') {
    const who = await webAccount(request, env);
    if (!who) return reply({error: 'sign_in', hint: 'Войдите через Google заново.'}, 401);
    let body;
    try { const raw = await request.text(); if (raw.length > 80000) throw 0; body = JSON.parse(raw); } catch { return reply({error: 'json'}, 400); }
    const hour = Math.floor(Date.now() / 3600000);
    const quota = await env.PROFILES.prepare('INSERT INTO limits(key,hits,until) VALUES(?,1,?) ON CONFLICT(key) DO UPDATE SET hits=hits+1,until=excluded.until RETURNING hits')
      .bind('webprofile:' + await digest(who.uid + ':' + hour), (hour + 1) * 3600000).first();
    if (quota?.hits > 30) return reply({error: 'rate_limited', hint: 'Слишком много изменений за час.'}, 429);
    const row = who.profile, next = {username: row.username, display: row.display};
    if (body.display !== undefined) {
      next.display = clean(body.display).slice(0, 40);
      if (!next.display) return reply({error: 'display', hint: 'Имя не может быть пустым.'}, 400);
    }
    if (body.username !== undefined) {
      const name = String(body.username || '').toLowerCase().replace(/^@/, '');
      const problem = usernameProblem(name);
      if (problem) return reply({error: 'username', hint: problem}, 400);
      next.username = name;
    }
    let avatar;
    if (body.avatar === null) avatar = null;
    else if (typeof body.avatar === 'string') {
      try {
        const bytes = Uint8Array.from(atob(body.avatar), c => c.charCodeAt(0));
        const ok = bytes.length <= 49152 && ((bytes[0] === 0xFF && bytes[1] === 0xD8) || bytes[0] === 137 || (bytes[8] === 87 && bytes[9] === 69));
        if (!ok) throw 0;
        avatar = body.avatar;
      } catch { return reply({error: 'avatar', hint: 'Аватарка — картинка до 48 КБ.'}, 400); }
    }
    if (!next.username) return reply({error: 'username', hint: 'Выберите имя пользователя.'}, 400);
    const now = Date.now();
    try {
      await env.PROFILES.prepare('UPDATE web_users SET username=?,display=?,updated=?' + (avatar !== undefined ? ',avatar=?' : '') + ' WHERE uid=?')
        .bind(...[next.username, next.display, now, ...(avatar !== undefined ? [avatar] : []), who.uid]).run();
    } catch (error) {
      if (/UNIQUE/i.test(String(error?.message || error))) return reply({error: 'taken', hint: 'Это имя уже занято.'}, 409);
      throw error;
    }
    const hasAvatar = avatar === undefined ? !!row.avatar : !!avatar;
    return reply({...face({username: next.username, display: next.display, avatar: hasAvatar, updated: now}), fresh: false});
  }
  return null;
}
