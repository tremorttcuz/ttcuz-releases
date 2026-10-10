const UID = /^[0-9]{1,24}$/;
const TOKEN = /^[a-f0-9]{64}$/;
const reply = (body, status = 200) => Response.json(body, {
  status, headers: { 'cache-control': 'no-store', 'access-control-allow-origin': '*' },
});
async function digest(value) {
  const hash = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value));
  return Array.from(new Uint8Array(hash), n => n.toString(16).padStart(2, '0')).join('');
}
async function challenge(uid, token) {
  return 'ttcuz-' + (await digest('verify:' + uid + ':' + token)).slice(0, 16);
}

// Only trust the user object itself, never unrelated text elsewhere on the page.
function findOwner(value, uid, handle, code, depth = 0) {
  if (!value || typeof value !== 'object' || depth > 24) return false;
  if (String(value.id) === uid && typeof value.uniqueId === 'string'
      && value.uniqueId.toLowerCase() === handle.toLowerCase()
      && typeof value.signature === 'string'
      && value.signature.split(/\s+/).includes(code)) return true;
  return Object.values(value).some(v => findOwner(v, uid, handle, code, depth + 1));
}
async function verifyOwner(uid, handle, code) {
  const result = await fetch('https://www.tiktok.com/@' + encodeURIComponent(handle), {
    redirect: 'error', signal: AbortSignal.timeout(10000),
    headers: { 'User-Agent': 'Mozilla/5.0', 'Accept': 'text/html' },
  });
  if (!result.ok) return false;
  const reader = result.body.getReader();
  const chunks = []; let total = 0;
  for (;;) {
    const { value, done } = await reader.read();
    if (done) break;
    total += value.byteLength;
    if (total > 2 * 1024 * 1024) { await reader.cancel(); return false; }
    chunks.push(value);
  }
  const bytes = new Uint8Array(total); let offset = 0;
  for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.length; }
  const html = new TextDecoder().decode(bytes);
  for (const match of html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/gi)) {
    try { if (findOwner(JSON.parse(match[1]), uid, handle, code)) return true; }
    catch { /* Not a JSON script. */ }
  }
  return false;
}
function normalizeStyle(input, uid, now) {
  const fields = ['firstColour', 'lastColour', 'middleColour', 'direction', 'speed', 'offsetX', 'offsetY', 'badgeSize'];
  if (!input || fields.some(k => !Number.isInteger(input[k]))) throw new Error('invalid style');
  for (const k of fields.slice(0, 3)) if (input[k] < -2147483648 || input[k] > 2147483647) throw new Error('invalid colour');
  if (input.direction < 0 || input.direction > 3 || input.speed < 0 || input.speed > 100
      || Math.abs(input.offsetX) > 100 || Math.abs(input.offsetY) > 100
      || input.badgeSize < 25 || input.badgeSize > 300) throw new Error('invalid range');
  const gradientWidth = input.gradientWidth == null ? 100 : input.gradientWidth;
  const gradientStop = input.gradientStop == null ? 50 : input.gradientStop;
  if (!Number.isInteger(gradientWidth) || gradientWidth < 50 || gradientWidth > 200
      || !Number.isInteger(gradientStop) || gradientStop < 5 || gradientStop > 95) throw new Error('invalid gradient');
  const displayName = input.displayName == null ? '' : input.displayName;
  if (typeof displayName !== 'string' || displayName.length > 80 || /[\x00-\x1f\x7f]/.test(displayName)) throw new Error('invalid name');
  const output = { profileId: uid, updatedAt: now, displayName: displayName.trim(), gradientWidth, gradientStop };
  for (const k of fields) output[k] = input[k];
  for (const k of ['threeColours', 'bold', 'glow', 'animated']) {
    if (typeof input[k] !== 'boolean') throw new Error('invalid flag');
    output[k] = input[k];
  }
  return output;
}
function png(input) {
  if (input == null || input === '') return null;
  if (typeof input !== 'string' || input.length > 45000 || !/^[A-Za-z0-9+/]*={0,2}$/.test(input)) throw new Error('invalid badge');
  const bytes = Uint8Array.from(atob(input), c => c.charCodeAt(0));
  const signature = [137,80,78,71,13,10,26,10];
  if (bytes.length < 33 || signature.some((b,i) => bytes[i] !== b)
      || String.fromCharCode(...bytes.slice(12,16)) !== 'IHDR') throw new Error('invalid PNG');
  const view = new DataView(bytes.buffer);
  const w = view.getUint32(16), h = view.getUint32(20);
  if (!w || !h || w > 256 || h > 256) throw new Error('invalid dimensions');
  return input;
}
async function profileRoute(request, env) {
  const url = new URL(request.url);
  if (!url.pathname.startsWith('/v1/')) return null;
  if (!env.PROFILES) return reply({ error: 'storage_unavailable' }, 503);
  const read = url.pathname.match(/^\/v1\/profiles\/([0-9]{1,24})(\/badge)?$/);
  if (read && request.method === 'GET') {
    const row = await env.PROFILES.prepare('SELECT metadata,badge,updated_at FROM profiles WHERE uid=?').bind(read[1]).first();
    if (!row?.metadata) return reply({ error: 'not_found' }, 404);
    if (read[2]) return row.badge ? new Response(Uint8Array.from(atob(row.badge), c=>c.charCodeAt(0)), {
      headers: {'content-type':'image/png','cache-control':'public,max-age=300','x-content-type-options':'nosniff'},
    }) : reply({error:'not_found'},404);
    const metadata = JSON.parse(row.metadata);
    metadata.badgeUrl = row.badge ? url.origin + '/v1/profiles/' + read[1] + '/badge?v=' + row.updated_at : null;
    return reply(metadata);
  }
  if (request.method !== 'POST' || !['/v1/verify','/v1/publish','/v1/remove'].includes(url.pathname)) return reply({error:'not_found'},404);
  const now = Date.now(), interval = 60000;
  const key = await digest((request.headers.get('cf-connecting-ip') || 'unknown') + ':' + Math.floor(now / interval));
  const quota = await env.PROFILES.prepare('INSERT INTO limits(key,hits,until) VALUES(?,1,?) ON CONFLICT(key) DO UPDATE SET hits=hits+1 RETURNING hits').bind(key,now+interval).first();
  if (quota.hits > 20) return reply({error:'rate_limited'},429);
  await env.PROFILES.prepare('DELETE FROM limits WHERE until < ?').bind(now-interval).run();
  if (Number(request.headers.get('content-length')) > 65536) return reply({error:'too_large'},413);
  // Bound chunked bodies as well as bodies carrying Content-Length.
  const reader = request.body?.getReader(); let total = 0; const parts = [];
  if (!reader) return reply({error:'invalid_body'},400);
  for (;;) { const {value,done}=await reader.read(); if(done)break; total+=value.length;
    if(total>65536){await reader.cancel();return reply({error:'too_large'},413);} parts.push(value); }
  const bytes=new Uint8Array(total);let pos=0;for(const p of parts){bytes.set(p,pos);pos+=p.length;}
  let body; try { body = JSON.parse(new TextDecoder().decode(bytes)); } catch { return reply({error:'invalid_json'},400); }
  if (!body || !UID.test(body.uid || '') || !TOKEN.test(body.token || '')) return reply({error:'invalid_identity'},400);
  const hash = await digest(body.token);
  const owner = await env.PROFILES.prepare('SELECT owner_hash FROM profiles WHERE uid=?').bind(body.uid).first();
  if (url.pathname === '/v1/verify') {
    if (owner?.owner_hash === hash) return reply({ok:true});
    if (typeof body.handle !== 'string' || !/^[A-Za-z0-9_.]{2,24}$/.test(body.handle)) return reply({error:'invalid_handle'},400);
    let verified = false;
    try { verified = await verifyOwner(body.uid, body.handle, await challenge(body.uid, body.token)); } catch {}
    if (!verified) return reply({error:'verification_unavailable_or_code_missing'},403);
    // A new device can reclaim only by proving control of the same TikTok account.
    await env.PROFILES.prepare('INSERT INTO profiles(uid,owner_hash,updated_at) VALUES(?,?,?) ON CONFLICT(uid) DO UPDATE SET owner_hash=excluded.owner_hash').bind(body.uid,hash,Date.now()).run();
    return reply({ok:true});
  }
  if (!owner || owner.owner_hash !== hash) return reply({error:'verify_required'},403);
  if (url.pathname === '/v1/remove') {
    await env.PROFILES.prepare('UPDATE profiles SET metadata=NULL,badge=NULL,updated_at=? WHERE uid=? AND owner_hash=?').bind(Date.now(),body.uid,hash).run();
    return reply({ok:true});
  }
  let metadata, badge;
  try { metadata=normalizeStyle(body.style,body.uid,Date.now()); badge=png(body.badge); }
  catch { return reply({error:'invalid_style'},400); }
  const result = await env.PROFILES.prepare('UPDATE profiles SET metadata=?,badge=?,updated_at=? WHERE uid=? AND owner_hash=?')
    .bind(JSON.stringify(metadata),badge,metadata.updatedAt,body.uid,hash).run();
  return result.meta.changes ? reply({ok:true}) : reply({error:'verify_required'},403);
}

const REPOSITORY = 'daniilsolovatulin-cyber/ttcuz-releases';
async function releaseVersion(request) {
  const key = new Request(new URL('/release-version-cache', request.url).toString());
  const cached = await caches.default.match(key);
  if (cached) return cached;
  const fallback = {version:'0.1.0',url:'',notes_ru:'Публичный APK пока не опубликован.'};
  try {
    const response = await fetch('https://api.github.com/repos/' + REPOSITORY + '/releases/latest', {
      headers:{'Accept':'application/vnd.github+json','User-Agent':'ttcuz-updates'},
      signal:AbortSignal.timeout(10000), redirect:'error',
    });
    if (!response.ok) return Response.json(fallback);
    const release = await response.json();
    const version = String(release.tag_name || '').replace(/^v/,'');
    const apk = Array.isArray(release.assets) && release.assets.find(asset =>
      asset.name === 'ttcuz.apk' && asset.state === 'uploaded' && asset.size > 0);
    const prefix = 'https://github.com/' + REPOSITORY + '/releases/download/';
    if (!/^\d+(\.\d+){1,3}$/.test(version) || !apk || release.draft || release.prerelease
        || typeof apk.browser_download_url !== 'string' || !apk.browser_download_url.startsWith(prefix))
      return Response.json(fallback);
    const result = Response.json({version,url:apk.browser_download_url,
      notes_ru:String(release.body || '').slice(0,4000)}, {
      headers:{'cache-control':'public,max-age=300','access-control-allow-origin':'*'},
    });
    await caches.default.put(key,result.clone());
    return result;
  } catch { return Response.json(fallback); }
}

const creatorUid = "7491898648855512119";

const json = (body, status = 200) => new Response(JSON.stringify(body), {
  status,
  headers: {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "public, max-age=60",
    "access-control-allow-origin": "*",
  },
});

export default {
  async fetch(request, env) {
    const profile = await profileRoute(request, env);
    if (profile) return profile;
    if (request.method !== "GET" && request.method !== "HEAD") {
      return json({ error: "method not allowed" }, 405);
    }

    const path = new URL(request.url).pathname;
    if (path === "/health") return json({ ok: true, project: "ttcuz" });
    if (path === "/badges") {
      return json({
        badges: [{
          id: "creator_trem0r1_1",
          colour: "#000000",
          image: "",
          title: "@trem0r1_1",
          text: "Создатель ttcuz",
          button: "Профиль TikTok",
          users: [creatorUid],
        }],
        now: Math.floor(Date.now() / 1000),
      });
    }
    if (path === "/version") {
      return releaseVersion(request);
    }
    if (path === "/" || path === "/guide.html") {
      const guideUrl = "https://raw.githubusercontent.com/daniilsolovatulin-cyber/ttcuz-releases/main/guide.html";
      const key = new Request(new URL("/guide-cache", request.url).toString());
      const cached = await caches.default.match(key);
      if (cached) return cached;
      try {
        const guide = await fetch(guideUrl, { signal: AbortSignal.timeout(10000) });
        if (!guide.ok) return new Response("Руководство временно недоступно.", { status: 502 });
        const response = new Response(guide.body, { headers: {
          "content-type": "text/html; charset=utf-8",
          "cache-control": "public, max-age=300",
        }});
        await caches.default.put(key, response.clone());
        return response;
      } catch {
        return new Response("Руководство временно недоступно.", { status: 502 });
      }
    }
    return env.ASSETS.fetch(request);
  },
};
