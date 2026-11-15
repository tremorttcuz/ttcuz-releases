// Stars and reviews under store plugins.
//
// Anyone can read them (the store page, the store in the app). Writing needs an account: the ttcuz account of the
// phone (its uid and the token its cloud profile was made with) or a Google account signed in on the store page.
// One review per account per plugin; sending again changes it, zero stars takes it away. The admin can remove any.
// A plugin still waiting for review can be rated too, when the one who sent it allowed that.
import {digest} from './profiles.mjs';
import {postTo} from './discord.mjs';
import {googleUser} from './google.mjs';
import {webAccount} from './accounts.mjs';
import {noteVote} from './stats.mjs';

const TABLE = 'CREATE TABLE IF NOT EXISTS plugin_reviews(plugin_id TEXT NOT NULL,uid TEXT NOT NULL,stars INTEGER NOT NULL,text TEXT NOT NULL,handle TEXT,added INTEGER NOT NULL,updated INTEGER NOT NULL,PRIMARY KEY(plugin_id,uid))';
const ID = /^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$/;
// likes and dislikes of reviews and of replies, and the replies themselves (as on CStore)
const MORE = [
  'CREATE TABLE IF NOT EXISTS review_votes(plugin_id TEXT NOT NULL,review_uid TEXT NOT NULL,voter TEXT NOT NULL,value INTEGER NOT NULL,PRIMARY KEY(plugin_id,review_uid,voter))',
  'CREATE TABLE IF NOT EXISTS review_replies(id INTEGER PRIMARY KEY AUTOINCREMENT,plugin_id TEXT NOT NULL,review_uid TEXT NOT NULL,uid TEXT NOT NULL,text TEXT NOT NULL,handle TEXT,added INTEGER NOT NULL)',
  'CREATE INDEX IF NOT EXISTS review_replies_plugin ON review_replies(plugin_id,review_uid)',
  'CREATE TABLE IF NOT EXISTS reply_votes(reply_id INTEGER NOT NULL,voter TEXT NOT NULL,value INTEGER NOT NULL,PRIMARY KEY(reply_id,voter))',
  // likes and dislikes of the plugins themselves
  'CREATE TABLE IF NOT EXISTS plugin_likes(plugin_id TEXT NOT NULL,voter TEXT NOT NULL,value INTEGER NOT NULL,PRIMARY KEY(plugin_id,voter))',
];
let made = false;
export async function reviewTable(env) {
  if (made) return;
  await env.PROFILES.prepare(TABLE).run();
  for (const sql of MORE) await env.PROFILES.prepare(sql).run();
  made = true;
}
const refOf = async uid => (await digest('review:' + uid)).slice(0, 16);
const all = async (env, sql, ...args) => { try { return (await env.PROFILES.prepare(sql).bind(...args).all()).results || []; } catch { return []; } };

const clean = text => String(text || '').replace(/[\u0000-\u0008\u000b-\u001f\u007f]/g, '').replace(/\n{3,}/g, '\n\n').trim();

/** The account behind a write: Google's ("g:..."), or the phone's uid when its token matches the profile; else null. */
export async function account(env, body, request) {
  const web = request ? await webAccount(request, env) : null;
  if (web) return {uid: web.uid, name: web.profile.username || '', needsName: !web.profile.username};
  const phone = await phoneAccount(env, body);
  return phone ? {uid: phone, name: ''} : null;
}
async function phoneAccount(env, body) {
  if (!/^\d{1,24}$/.test(body?.uid || '') || !/^[a-f0-9]{64}$/.test(body?.token || '')) return null;
  const row = await env.PROFILES.prepare('SELECT owner_hash FROM profiles WHERE uid=?').bind(body.uid).first();
  return row && row.owner_hash === await digest(body.token) ? body.uid : null;
}

/** {plugin_id: {average, count, up, dn}} for the catalog: two queries for all of them. */
let ratingMemo = null;
/** After a review changes, the next catalog counts again. */
export function forgetRatings() { ratingMemo = null; }
export async function ratings(env) {
  // one GROUP BY over every review: kept a minute per isolate, the catalog is read far more often than rated
  if (ratingMemo && ratingMemo.until > Date.now()) return ratingMemo.value;
  await reviewTable(env);
  const rows = await env.PROFILES.prepare('SELECT plugin_id,AVG(stars) AS average,COUNT(*) AS count FROM plugin_reviews GROUP BY plugin_id').all();
  const out = {};
  for (const r of rows.results || []) out[r.plugin_id] = {average: Math.round(r.average * 10) / 10, count: r.count};
  for (const r of await all(env, 'SELECT plugin_id,SUM(value=1) AS up,SUM(value=-1) AS dn FROM plugin_likes GROUP BY plugin_id')) {
    const one = out[r.plugin_id] || (out[r.plugin_id] = {average: 0, count: 0});
    one.up = r.up || 0; one.dn = r.dn || 0;
  }
  ratingMemo = {value: out, until: Date.now() + 60000};
  return out;
}

/** One plugin's reviews: the average, how many of each star, the newest 60 (no uid, only the nickname). */
export async function reviewsOf(env, id, uid = '') {
  await reviewTable(env);
  let rows;
  try {
    rows = await env.PROFILES.prepare('SELECT r.uid,r.stars,r.text,r.handle,r.added,r.updated,w.username AS wname,w.display AS wdisplay,w.avatar IS NOT NULL AS wavatar,w.updated AS wupdated FROM plugin_reviews r LEFT JOIN web_users w ON w.uid=r.uid WHERE r.plugin_id=? ORDER BY r.updated DESC LIMIT 200').bind(id).all();
  } catch {
    // no site profiles yet: the reviews alone
    rows = await env.PROFILES.prepare('SELECT uid,stars,text,handle,added,updated FROM plugin_reviews WHERE plugin_id=? ORDER BY updated DESC LIMIT 200').bind(id).all();
  }
  const list = rows.results || [], histogram = [0, 0, 0, 0, 0];
  let sum = 0;
  for (const r of list) { histogram[r.stars - 1]++; sum += r.stars; }
  const mine = uid ? list.find(r => r.uid === uid) : null;
  // the votes of every review, this account's own votes, and the replies with their votes: five reads, all by plugin
  const counts = new Map(), my = new Map(), replies = new Map(), rcount = new Map(), rmy = new Map();
  if (list.length) {
    for (const v of await all(env, 'SELECT review_uid,SUM(value=1) AS up,SUM(value=-1) AS dn FROM review_votes WHERE plugin_id=? GROUP BY review_uid', id)) counts.set(v.review_uid, v);
    if (uid) for (const v of await all(env, 'SELECT review_uid,value FROM review_votes WHERE plugin_id=? AND voter=?', id, uid)) my.set(v.review_uid, v.value);
    for (const v of await all(env, 'SELECT v.reply_id,SUM(v.value=1) AS up,SUM(v.value=-1) AS dn FROM reply_votes v JOIN review_replies r ON r.id=v.reply_id WHERE r.plugin_id=? GROUP BY v.reply_id', id)) rcount.set(v.reply_id, v);
    if (uid) for (const v of await all(env, 'SELECT v.reply_id,v.value FROM reply_votes v JOIN review_replies r ON r.id=v.reply_id WHERE r.plugin_id=? AND v.voter=?', id, uid)) rmy.set(v.reply_id, v.value);
    for (const r of await all(env, 'SELECT r.id,r.review_uid,r.uid,r.text,r.handle,r.added,w.username AS wname,w.display AS wdisplay,w.avatar IS NOT NULL AS wavatar,w.updated AS wupdated FROM review_replies r LEFT JOIN web_users w ON w.uid=r.uid WHERE r.plugin_id=? ORDER BY r.added LIMIT 2000', id)) {
      const c = rcount.get(r.id) || {};
      const one = {id: r.id, text: r.text, handle: r.wname || r.handle || '', ...(r.wdisplay ? {name: r.wdisplay} : {}),
        ...(r.wavatar && r.wname ? {avatar: '/plugins/web/avatar/' + r.wname + '?v=' + r.wupdated} : {}),
        added: r.added, up: c.up || 0, dn: c.dn || 0, my: rmy.get(r.id) || 0, ...(uid && r.uid === uid ? {mine: true} : {})};
      if (!replies.has(r.review_uid)) replies.set(r.review_uid, []);
      replies.get(r.review_uid).push(one);
    }
    // the most useful reply first, then the oldest
    for (const l of replies.values()) l.sort((a, b) => (b.up - b.dn) - (a.up - a.dn) || a.added - b.added);
  }
  return {
    average: list.length ? Math.round(sum / list.length * 10) / 10 : 0,
    count: list.length,
    histogram,
    // ref: a short hash of the account, so the admin can remove one review without the uid ever being shown
    reviews: await Promise.all(list.slice(0, 60).map(async r => ({ref: (await digest('review:' + r.uid)).slice(0, 16), stars: r.stars, text: r.text, handle: r.wname || r.handle || '',
      ...(r.wdisplay ? {name: r.wdisplay} : {}), ...(r.wavatar && r.wname ? {avatar: '/plugins/web/avatar/' + r.wname + '?v=' + r.wupdated} : {}),
      added: r.added, updated: r.updated, up: counts.get(r.uid)?.up || 0, dn: counts.get(r.uid)?.dn || 0, my: my.get(r.uid) || 0,
      replies: replies.get(r.uid) || [], ...(uid && r.uid === uid ? {mine: true} : {})}))),
    ...(mine ? {mine: {stars: mine.stars, text: mine.text}} : {}),
  };
}

const starLine = n => '★'.repeat(n) + '☆'.repeat(5 - n);

export async function reviewRoute(request, env, path, reply, admin) {
  const extra = path.match(/^\/plugins\/reviews\/([A-Za-z0-9][A-Za-z0-9._-]{0,79})\/(vote|like|reply|replies\/(\d{1,10})\/(vote|delete))$/);
  if (extra) return request.method === 'POST' ? talk(request, env, extra[1], extra[2].startsWith('replies') ? extra[4] : extra[2], extra[3] ? Number(extra[3]) : 0, reply, admin) : reply({error: 'method'}, 405);
  const match = path.match(/^\/plugins\/reviews\/([A-Za-z0-9][A-Za-z0-9._-]{0,79})(\/remove)?$/);
  if (!match) return null;
  const id = match[1];
  if (!ID.test(id)) return reply({error: 'invalid_id'}, 400);
  if (request.method === 'GET') {
    // "mine" only for an account that proves itself: the signed-in Google one, or the phone's uid it asks with
    const google = await googleUser(request, env);
    const uid = google ? google.uid : new URL(request.url).searchParams.get('uid') || '';
    const own = google || /^\d{1,24}$/.test(uid);
    return reply(await reviewsOf(env, id, own ? uid : ''), 200, own ? 'no-store' : 'public, max-age=30');
  }
  if (request.method !== 'POST') return reply({error: 'method'}, 405);
  let body;
  try { const raw = await request.text(); if (raw.length > 8000) throw 0; body = JSON.parse(raw); } catch { return reply({error: 'json'}, 400); }
  await reviewTable(env);
  if (match[2]) {
    // the admin takes a review away
    const who = await admin(request, env);
    if (who !== 'ok') return reply({error: who === 'locked' ? 'locked' : 'forbidden'}, who === 'locked' ? 429 : 403);
    if (!/^[a-f0-9]{16}$/.test(body?.ref || '')) return reply({error: 'invalid_review'}, 400);
    const rows = await env.PROFILES.prepare('SELECT uid FROM plugin_reviews WHERE plugin_id=?').bind(id).all();
    for (const r of rows.results || [])
      if ((await digest('review:' + r.uid)).slice(0, 16) === body.ref) { await env.PROFILES.prepare('DELETE FROM plugin_reviews WHERE plugin_id=? AND uid=?').bind(id, r.uid).run(); await dropThread(env, id, r.uid); }
    return reply({ok: true, ...(await reviewsOf(env, id))});
  }
  const who = await account(env, body, request);
  if (!who) return reply({error: 'account_required', hint: 'Войдите через Google на сайте или оставьте отзыв из приложения ttcuz.'}, 403);
  if (who.needsName) return reply({error: 'username', hint: 'Сначала выберите имя пользователя в профиле.'}, 400);
  const uid = who.uid;
  let listed = await env.PROFILES.prepare('SELECT metadata FROM plugin_packages WHERE id=?').bind(id).first();
  if (!listed) {
    // not in the store yet: only while it waits for review, and only if its sender opened it to reviews
    try { listed = await env.PROFILES.prepare("SELECT metadata FROM native_submissions WHERE plugin_id=? AND state='pending' AND reviews_open=1 ORDER BY added DESC LIMIT 1").bind(id).first(); } catch { listed = null; }
    if (!listed) return reply({error: 'not_found', hint: 'Этот плагин нельзя оценить: его нет в магазине, а автор не открыл отзывы на время проверки.'}, 404);
  }
  const hour = Math.floor(Date.now() / 3600000);
  const quota = await env.PROFILES.prepare('INSERT INTO limits(key,hits,until) VALUES(?,1,?) ON CONFLICT(key) DO UPDATE SET hits=hits+1,until=excluded.until RETURNING hits')
    .bind('review:' + await digest(uid + ':' + hour), (hour + 1) * 3600000).first();
  if (quota?.hits > 20) return reply({error: 'rate_limited', hint: 'Слишком много отзывов за час, попробуйте позже.'}, 429);
  const stars = body.stars;
  if (stars === 0) {
    await env.PROFILES.prepare('DELETE FROM plugin_reviews WHERE plugin_id=? AND uid=?').bind(id, uid).run();
    await dropThread(env, id, uid);
    forgetRatings();
    return reply({ok: true, ...(await reviewsOf(env, id, uid))});
  }
  if (!Number.isInteger(stars) || stars < 1 || stars > 5) return reply({error: 'invalid_stars'}, 400);
  const text = clean(body.text).slice(0, 600);
  const handle = (who.name || String(body.handle || '').replace(/^@/, '')).replace(/[^\w.]/g, '').slice(0, 40);
  const now = Date.now();
  const before = await env.PROFILES.prepare('SELECT stars FROM plugin_reviews WHERE plugin_id=? AND uid=?').bind(id, uid).first();
  await env.PROFILES.prepare('INSERT INTO plugin_reviews(plugin_id,uid,stars,text,handle,added,updated) VALUES(?,?,?,?,?,?,?) ON CONFLICT(plugin_id,uid) DO UPDATE SET stars=excluded.stars,text=excluded.text,handle=excluded.handle,updated=excluded.updated')
    .bind(id, uid, stars, text, handle, now, now).run();
  forgetRatings();
  if (!before) {
    // a new review is worth a line in the store channel; edits are not
    let name = id;
    try { name = JSON.parse(listed.metadata).name || id; } catch {}
    await postTo(env, 'store', `${starLine(stars)} **Отзыв о плагине** ${String(name).slice(0, 80)}${handle ? ' от @' + handle : ''}${text ? '\n> ' + text.slice(0, 300).replace(/\n/g, '\n> ') : ''}`);
  }
  return reply({ok: true, ...(await reviewsOf(env, id, uid))});
}

/** A review is gone: its votes, its replies and their votes go too. */
async function dropThread(env, id, reviewUid) {
  try {
    await env.PROFILES.prepare('DELETE FROM reply_votes WHERE reply_id IN (SELECT id FROM review_replies WHERE plugin_id=? AND review_uid=?)').bind(id, reviewUid).run();
    await env.PROFILES.prepare('DELETE FROM review_replies WHERE plugin_id=? AND review_uid=?').bind(id, reviewUid).run();
    await env.PROFILES.prepare('DELETE FROM review_votes WHERE plugin_id=? AND review_uid=?').bind(id, reviewUid).run();
  } catch { /* nothing to drop */ }
}

/**
 * Votes and replies. A vote is 1 (useful), -1 (not) or 0 (taken back); one per account, and not on one's own review.
 * A reply is up to 400 characters, at most 100 under a review; its author or the admin can delete it.
 */
async function talk(request, env, id, action, replyId, reply, admin) {
  let body;
  try { const raw = await request.text(); if (raw.length > 4000) throw 0; body = raw ? JSON.parse(raw) : {}; } catch { return reply({error: 'json'}, 400); }
  await reviewTable(env);
  // the admin may delete any reply without an account
  if (action === 'delete' && request.headers.get('x-admin-pin') && await admin(request, env) === 'ok') {
    await env.PROFILES.prepare('DELETE FROM reply_votes WHERE reply_id=?').bind(replyId).run();
    await env.PROFILES.prepare('DELETE FROM review_replies WHERE id=? AND plugin_id=?').bind(replyId, id).run();
    return reply({ok: true});
  }
  const who = await account(env, body, request);
  if (!who) return reply({error: 'account_required', hint: 'Войдите через Google, чтобы отвечать и оценивать отзывы.'}, 403);
  if (who.needsName) return reply({error: 'username', hint: 'Сначала выберите имя пользователя в профиле.'}, 400);
  const hour = Math.floor(Date.now() / 3600000), kind = action === 'reply' ? 'reply' : 'vote';
  const quota = await env.PROFILES.prepare('INSERT INTO limits(key,hits,until) VALUES(?,1,?) ON CONFLICT(key) DO UPDATE SET hits=hits+1,until=excluded.until RETURNING hits')
    .bind(kind + ':' + await digest(who.uid + ':' + hour), (hour + 1) * 3600000).first();
  if (quota?.hits > (kind === 'reply' ? 30 : 300)) return reply({error: 'rate_limited', hint: 'Слишком часто, попробуйте позже.'}, 429);
  const value = body.value;
  if (action === 'like') {
    // a like or dislike of the plugin itself; 0 takes it back
    if (![1, -1, 0].includes(value)) return reply({error: 'invalid_vote'}, 400);
    const listed = await env.PROFILES.prepare('SELECT 1 AS ok FROM plugin_packages WHERE id=?').bind(id).first();
    if (!listed) return reply({error: 'not_found', hint: 'Этого плагина нет в магазине.'}, 404);
    const was = await env.PROFILES.prepare('SELECT value FROM plugin_likes WHERE plugin_id=? AND voter=?').bind(id, who.uid).first();
    if (value === 0) await env.PROFILES.prepare('DELETE FROM plugin_likes WHERE plugin_id=? AND voter=?').bind(id, who.uid).run();
    else await env.PROFILES.prepare('INSERT INTO plugin_likes(plugin_id,voter,value) VALUES(?,?,?) ON CONFLICT(plugin_id,voter) DO UPDATE SET value=excluded.value').bind(id, who.uid, value).run();
    forgetRatings();
    try { await noteVote(env, id, was?.value || 0, value); } catch { /* the chart misses one; the vote stands */ }
    const n = await env.PROFILES.prepare('SELECT SUM(value=1) AS up,SUM(value=-1) AS dn FROM plugin_likes WHERE plugin_id=?').bind(id).first();
    return reply({ok: true, up: n?.up || 0, dn: n?.dn || 0, my: value});
  }
  if (replyId) {
    const target = await env.PROFILES.prepare('SELECT uid FROM review_replies WHERE id=? AND plugin_id=?').bind(replyId, id).first();
    if (!target) return reply({error: 'not_found'}, 404);
    if (action === 'delete') {
      if (target.uid !== who.uid) return reply({error: 'forbidden', hint: 'Удалить можно только свой ответ.'}, 403);
      await env.PROFILES.prepare('DELETE FROM reply_votes WHERE reply_id=?').bind(replyId).run();
      await env.PROFILES.prepare('DELETE FROM review_replies WHERE id=?').bind(replyId).run();
      return reply({ok: true});
    }
    if (![1, -1, 0].includes(value)) return reply({error: 'invalid_vote'}, 400);
    if (target.uid === who.uid) return reply({error: 'own', hint: 'Свой ответ оценить нельзя.'}, 400);
    if (value === 0) await env.PROFILES.prepare('DELETE FROM reply_votes WHERE reply_id=? AND voter=?').bind(replyId, who.uid).run();
    else await env.PROFILES.prepare('INSERT INTO reply_votes(reply_id,voter,value) VALUES(?,?,?) ON CONFLICT(reply_id,voter) DO UPDATE SET value=excluded.value').bind(replyId, who.uid, value).run();
    return reply({ok: true});
  }
  // the review a vote or a reply is for, found by its ref
  if (!/^[a-f0-9]{16}$/.test(body.ref || '')) return reply({error: 'invalid_review'}, 400);
  let reviewUid = null;
  for (const r of await all(env, 'SELECT uid FROM plugin_reviews WHERE plugin_id=?', id)) if (await refOf(r.uid) === body.ref) reviewUid = r.uid;
  if (!reviewUid) return reply({error: 'not_found', hint: 'Отзыв уже удалён.'}, 404);
  if (action === 'vote') {
    if (![1, -1, 0].includes(value)) return reply({error: 'invalid_vote'}, 400);
    if (reviewUid === who.uid) return reply({error: 'own', hint: 'Свой отзыв оценить нельзя.'}, 400);
    if (value === 0) await env.PROFILES.prepare('DELETE FROM review_votes WHERE plugin_id=? AND review_uid=? AND voter=?').bind(id, reviewUid, who.uid).run();
    else await env.PROFILES.prepare('INSERT INTO review_votes(plugin_id,review_uid,voter,value) VALUES(?,?,?,?) ON CONFLICT(plugin_id,review_uid,voter) DO UPDATE SET value=excluded.value').bind(id, reviewUid, who.uid, value).run();
    return reply({ok: true});
  }
  const text = clean(body.text).slice(0, 400);
  if (!text) return reply({error: 'empty', hint: 'Напишите ответ.'}, 400);
  const many = await env.PROFILES.prepare('SELECT COUNT(*) AS n FROM review_replies WHERE plugin_id=? AND review_uid=?').bind(id, reviewUid).first();
  if ((many?.n || 0) >= 100) return reply({error: 'too_many', hint: 'Под этим отзывом уже 100 ответов.'}, 400);
  const now = Date.now();
  const row = await env.PROFILES.prepare('INSERT INTO review_replies(plugin_id,review_uid,uid,text,handle,added) VALUES(?,?,?,?,?,?) RETURNING id').bind(id, reviewUid, who.uid, text, who.name || '', now).first();
  return reply({ok: true, reply: {id: row?.id, text, handle: who.name || '', added: now, up: 0, dn: 0, my: 0, mine: true}});
}
