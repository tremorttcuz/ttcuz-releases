// How many people use ttcuz, and how often each store plugin was downloaded.
//
// Nothing new is asked of the phones: the app already asks for /badges every three minutes while TikTok is open,
// and once in ten minutes it adds an anonymous random id (x-ttcuz-here) to that request. The id is kept only as a
// hash, with the time it was last seen: "now" is who was seen in the last twelve minutes. A download is counted
// when the store hands out a plugin's archive. Both are one small write; the totals are read once a minute at most.
const HERE = /^[0-9a-f]{32}$/;
const NOW_WINDOW = 12 * 60 * 1000, DAY = 24 * 3600 * 1000, WEEK = 7 * DAY;
let ready = false;

async function tables(env) {
  if (ready) return;
  await env.PROFILES.batch([
    env.PROFILES.prepare('CREATE TABLE IF NOT EXISTS presence (id TEXT PRIMARY KEY, seen INTEGER NOT NULL)'),
    env.PROFILES.prepare('CREATE INDEX IF NOT EXISTS presence_seen ON presence(seen)'),
    env.PROFILES.prepare('CREATE TABLE IF NOT EXISTS plugin_downloads (id TEXT PRIMARY KEY, count INTEGER NOT NULL DEFAULT 0)'),
    // a plugin's day by day, for its author's charts: downloads, and the likes and dislikes given that day
    env.PROFILES.prepare('CREATE TABLE IF NOT EXISTS plugin_daily (id TEXT NOT NULL, day INTEGER NOT NULL, downloads INTEGER NOT NULL DEFAULT 0, likes INTEGER NOT NULL DEFAULT 0, dislikes INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id, day))'),
  ]);
  ready = true;
}

async function hash(text) {
  const bytes = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text));
  return Array.from(new Uint8Array(bytes).slice(0, 16), b => b.toString(16).padStart(2, '0')).join('');
}

/** The phone said it is here: written after the answer has gone, so /badges is not slower for it. */
export function notePresence(request, env, ctx) {
  const id = (request.headers.get('x-ttcuz-here') || '').toLowerCase();
  if (!HERE.test(id) || !env.PROFILES || !ctx) return;
  ctx.waitUntil((async () => {
    await tables(env);
    await env.PROFILES.prepare('INSERT INTO presence(id,seen) VALUES(?,?) ON CONFLICT(id) DO UPDATE SET seen=excluded.seen')
      .bind(await hash('ttcuz-here:' + id), Date.now()).run();
  })().catch(error => console.error('ttcuz presence failed', String(error?.message || error))));
}

/** A store plugin's archive was handed out. */
export function noteDownload(id, env, ctx) {
  if (!env.PROFILES || !ctx) return;
  ctx.waitUntil((async () => {
    await tables(env);
    await env.PROFILES.batch([
      env.PROFILES.prepare('INSERT INTO plugin_downloads(id,count) VALUES(?,1) ON CONFLICT(id) DO UPDATE SET count=count+1').bind(id),
      env.PROFILES.prepare('INSERT INTO plugin_daily(id,day,downloads) VALUES(?,?,1) ON CONFLICT(id,day) DO UPDATE SET downloads=downloads+1').bind(id, today()),
    ]);
  })().catch(error => console.error('ttcuz download count failed', String(error?.message || error))));
}

const today = () => Math.floor(Date.now() / DAY);
/** How many days a chart shows at most, and how long a day is kept. */
export const HISTORY_DAYS = 90;
const KEEP_DAYS = 400;

/** A like or dislike was given (or changed to one): counted on today. Taking one back counts nothing. */
export async function noteVote(env, id, before, after) {
  if (!env.PROFILES || before === after || (after !== 1 && after !== -1)) return;
  await tables(env);
  const column = after === 1 ? 'likes' : 'dislikes';
  await env.PROFILES.prepare(`INSERT INTO plugin_daily(id,day,${column}) VALUES(?,?,1) ON CONFLICT(id,day) DO UPDATE SET ${column}=${column}+1`).bind(id, today()).run();
}

/** The last HISTORY_DAYS days of a plugin, oldest first, every day there (a quiet day is zeros): [{day,downloads,likes,dislikes}]. */
export async function pluginHistory(env, id) {
  const last = today(), first = last - HISTORY_DAYS + 1, byDay = new Map();
  if (env.PROFILES) {
    await tables(env);
    const rows = await env.PROFILES.prepare('SELECT day,downloads,likes,dislikes FROM plugin_daily WHERE id=? AND day>=? ORDER BY day').bind(id, first).all();
    for (const row of rows.results || []) byDay.set(row.day, row);
  }
  const out = [];
  for (let day = first; day <= last; day++) {
    const row = byDay.get(day);
    out.push({day, downloads: row?.downloads || 0, likes: row?.likes || 0, dislikes: row?.dislikes || 0});
  }
  return out;
}

/** Every plugin's downloads, for the catalog: {id: count}. */
export async function downloadCounts(env) {
  if (!env.PROFILES) return {};
  await tables(env);
  const rows = await env.PROFILES.prepare('SELECT id,count FROM plugin_downloads').all();
  const out = {};
  for (const row of rows.results || []) out[row.id] = row.count;
  return out;
}

/** Ids not seen for over a week go (from the half-hourly cron): the table holds a week of people at most. */
export async function cleanPresence(env) {
  if (!env.PROFILES) return;
  await tables(env);
  await env.PROFILES.prepare('DELETE FROM presence WHERE seen < ?').bind(Date.now() - WEEK - DAY).run();
  await env.PROFILES.prepare('DELETE FROM plugin_daily WHERE day < ?').bind(today() - KEEP_DAYS).run();
}

/** GET /v1/stats: {now, day, week, downloads}. Kept a minute in the edge cache, so many phones cost one read. */
export async function statsRoute(request, env) {
  if (new URL(request.url).pathname !== '/v1/stats') return null;
  if (request.method !== 'GET') return null;
  const cache = globalThis.caches?.default, key = new Request(new URL('/v1/stats-cache', request.url).toString());
  const cached = cache && await cache.match(key);
  if (cached) return cached;
  let body = {now: 0, day: 0, week: 0, downloads: 0};
  if (env.PROFILES) {
    await tables(env);
    const at = Date.now();
    const [people, files] = await env.PROFILES.batch([
      env.PROFILES.prepare('SELECT SUM(seen > ?) AS now, SUM(seen > ?) AS day, SUM(seen > ?) AS week FROM presence').bind(at - NOW_WINDOW, at - DAY, at - WEEK),
      env.PROFILES.prepare('SELECT COALESCE(SUM(count),0) AS downloads FROM plugin_downloads'),
    ]);
    const p = people.results?.[0] || {}, f = files.results?.[0] || {};
    body = {now: p.now || 0, day: p.day || 0, week: p.week || 0, downloads: f.downloads || 0};
  }
  const response = new Response(JSON.stringify(body), {headers: {
    'content-type': 'application/json; charset=utf-8', 'cache-control': 'public, max-age=60', 'access-control-allow-origin': '*'}});
  if (cache) await cache.put(key, response.clone());
  return response;
}
