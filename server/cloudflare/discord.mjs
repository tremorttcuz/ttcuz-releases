/**
 * Crash reports, delivered to a person rather than a table.
 *
 * A crash is only useful if somebody reads it while it is still interesting,
 * so every accepted report is pushed to Discord as a direct message. The bot
 * token and the recipient live in Worker secrets and are never seen by
 * clients -- a phone can write to the crash table and nothing else.
 *
 * Delivery is best effort by construction: a report is stored before this is
 * called, and every failure here is swallowed into the returned reason. Losing
 * a notification is a nuisance; losing the report, or failing a client that
 * did nothing wrong, is not.
 */

const API = 'https://discord.com/api/v10';
const MESSAGE_LIMIT = 1900;
/**
 * A crash is stored before it is announced, and the phone is waiting on the
 * answer. Discord answering slowly must not become the crash upload failing,
 * so every call here gives up rather than holding the request open.
 */
const TIMEOUT = 5000;

export const configured = env => Boolean(
  env && typeof env.DISCORD_BOT_TOKEN === 'string' && env.DISCORD_BOT_TOKEN
);

const clip = (value, limit) => {
  const text = String(value == null ? '' : value);
  return text.length > limit ? text.slice(0, limit) + '…' : text;
};

const plain = value => String(value == null ? '' : value).replace(/`/g, "'");

/** The report as the client sent it: an envelope, or a bare stack from 0.5.8. */
export function parseCrash(row) {
  const raw = String(row?.stack ?? '');
  const time = Number(row?.occurred_at ?? row?.time) || 0;
  const base = {
    id: String(row?.id ?? ''),
    version: String(row?.version ?? '?'),
    sdk: Number(row?.sdk) || 0,
    time,
    at: time ? new Date(time).toISOString().replace('T', ' ').slice(0, 19) + ' UTC' : '?',
    stack: raw,
    meta: {},
  };
  if (!raw.startsWith('{')) return base;
  let envelope;
  try {
    envelope = JSON.parse(raw);
  } catch {
    return base;
  }
  if (!envelope || typeof envelope !== 'object') return base;
  return {
    ...base,
    stack: typeof envelope.stack === 'string' && envelope.stack ? envelope.stack : raw,
    meta: envelope.meta && typeof envelope.meta === 'object' ? envelope.meta : {},
  };
}

/** One line that says what threw, and with what. */
function headline(crash) {
  const info = crash.meta.crash || {};
  const first = crash.stack.split('\n', 1)[0] || '';
  const type = info.type || first.split(':')[0] || 'Error';
  const message = info.message
    || (first.includes(':') ? first.slice(first.indexOf(':') + 1).trim() : '');
  return message ? `${type}: ${message}` : type;
}

/** Where it happened, skipping the frames that are ours. */
function blame(crash) {
  const info = crash.meta.crash || {};
  if (info.site) return info.site;
  const frames = crash.stack.split('\n').filter(line => /^\s+at /.test(line));
  const pick = frames.find(line => !/cat\.narezany\.ttcuz/.test(line)) || frames[0];
  return pick ? pick.trim().replace(/^at /, '') : 'unknown';
}

/** A key/value block, one per line, with the empty ones left out. */
function block(pairs) {
  return pairs
    .filter(([, value]) => value !== '' && value != null && value !== false)
    .map(([label, value]) => `**${label}:** ${plain(value)}`)
    .join('\n');
}

const minutes = seconds => {
  const value = Number(seconds);
  if (!Number.isFinite(value) || value <= 0) return '';
  return `${Math.floor(value / 60)} min ${Math.round(value % 60)} s`;
};

const mb = value => (Number.isFinite(Number(value)) ? `${value} MB` : '');

/** The whole report as one message, trimmed to what Discord will accept. */
export function crashMessage(crash) {
  const info = crash.meta.crash || {};
  const device = crash.meta.device || {};
  const memory = crash.meta.memory || {};
  const mod = crash.meta.mod || {};

  const hardware = [device.manufacturer, device.model].filter(Boolean).join(' ')
    || device.brand
    || 'unknown';
  const android = info.androidRelease || `API ${crash.sdk}`;

  const head = [
    `**Crash in ttcuz ${crash.version}** · ${crash.at}`,
    block([
      ['What', headline(crash)],
      ['Where', blame(crash)],
      ['Thread', info.thread],
      ['Kind', info.fatal ? 'fatal' : 'caught'],
      ['Handler', info.handler],
    ]),
  ].filter(Boolean).join('\n');

  const environment = [
    '**Environment**',
    block([
      ['Device', hardware],
      ['Android', android],
      ['TikTok', mod.tiktok],
      ['Mod', mod.version || crash.version],
      ['Process uptime', minutes(mod.uptimeSeconds)],
      ['RAM free', memory.freeMb != null && memory.totalMb != null
        ? `${memory.freeMb} of ${memory.totalMb} MB`
        : ''],
      ['Heap used', mb(memory.javaHeapMb)],
      ['Heap limit', mb(memory.javaHeapMaxMb)],
      ['Storage free', mb(memory.storageFreeMb)],
    ]),
  ].join('\n');

  const stack = plain(clip(crash.stack, 12000));
  const room = MESSAGE_LIMIT - head.length - environment.length - 40;
  const shown = stack.length > room
    ? stack.slice(0, Math.max(0, room)) + '\n… trimmed'
    : stack;
  const footer = `\`id ${crash.id} · sdk ${crash.sdk} · v${crash.version}\``;
  return `${head}\n\n${environment}\n\n**Stack**\n\`\`\`\n${shown}\n\`\`\`\n${footer}`;
}

/** The channel a person is written to, remembered for the isolate's lifetime. */
const openChannels = new Map();

/**
 * The id of the DM channel with the person who gets these.
 *
 * A user id is not a channel id: sending to the person's own id is answered
 * with "Unknown Channel", which is how this was first shipped. The channel is
 * opened with the recipient, which is idempotent and returns the existing
 * conversation, and it is opened again only when Discord says the remembered
 * one is gone.
 */
async function dmChannel(token, env, forget) {
  const person = String(env.DISCORD_USER_ID || '').trim();
  if (!person) throw new Error('discord: no recipient configured');
  if (forget) openChannels.delete(person);
  else if (openChannels.has(person)) return openChannels.get(person);
  const response = await fetch(`${API}/users/@me/channels`, {
    method: 'POST',
    headers: {
      authorization: `Bot ${token}`,
      'content-type': 'application/json',
      'user-agent': 'ttcuz-crash (worker, 1.0)',
    },
    // Discord will not open a conversation with somebody the bot has never
    // talked to and does not share a server with; that refusal is reported
    // rather than retried, and is fixed by writing to the bot once.
    body: JSON.stringify({ recipient_id: person }),
    signal: AbortSignal.timeout(TIMEOUT),
  });
  if (!response.ok) {
    throw new Error(`dm open: ${response.status} ${clip(await response.text(), 200)}`);
  }
  const channel = await response.json();
  if (!channel || !channel.id) throw new Error('dm open: no channel in the answer');
  openChannels.set(person, channel.id);
  return channel.id;
}

/** Send one crash as a DM. Returns whether it landed, and why not if it did not. */
// ---------------------------------------------------------------- the server's channels
//
// The mod's Discord server ("Просмотр плагинов", DISCORD_GUILD_ID) has a channel for each kind of news, found by
// name, so nothing but the server's id has to be configured:
//   плагины-на-проверку -- a plugin waiting for review; its message goes away once it is decided
//   проверенные-плагины -- the approved ones, moved here from the review channel
//   магазин             -- what appears in or leaves the store
//   крашлоги            -- crash reports
// Without the server (or before the bot joins it) everything still arrives as a direct message, as before.

export const CHANNELS = {review: 'плагины-на-проверку', approved: 'проверенные-плагины', store: 'магазин', crashes: 'крашлоги'};
const sameName = name => String(name || '').toLowerCase().replace(/ё/g, 'е').replace(/[\s_]+/g, '-');
let known = null;

async function call(env, method, path, body) {
  return fetch(API + path, {
    method,
    headers: {authorization: `Bot ${env.DISCORD_BOT_TOKEN}`, 'content-type': 'application/json', 'user-agent': 'ttcuz (worker, 1.0)'},
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(TIMEOUT),
  });
}

/** The server's id: DISCORD_GUILD_ID, which may also be the id of one of its channels. */
async function guildOf(env) {
  const given = String(env.DISCORD_GUILD_ID || '').trim();
  if (!/^\d{5,25}$/.test(given)) return null;
  const asGuild = await call(env, 'GET', `/guilds/${given}`);
  if (asGuild.ok) return given;
  const asChannel = await call(env, 'GET', `/channels/${given}`);
  if (asChannel.ok) return (await asChannel.json()).guild_id || null;
  throw new Error(`discord server ${given}: ${asGuild.status} (is the bot invited?)`);
}

/** {review, approved, store, crashes} -> channel id, for the channels found; looked up at most once an hour. */
export async function channels(env, fresh = false) {
  if (!configured(env) || !env.DISCORD_GUILD_ID) return {};
  if (!fresh && known && known.given === env.DISCORD_GUILD_ID && Date.now() - known.at < 3600000) return known.ids;
  const guild = await guildOf(env);
  if (!guild) return {};
  const response = await call(env, 'GET', `/guilds/${guild}/channels`);
  if (!response.ok) throw new Error(`discord channels: ${response.status}`);
  const list = await response.json(), ids = {};
  for (const [key, name] of Object.entries(CHANNELS)) {
    const found = list.find(c => (c.type === 0 || c.type === 5) && sameName(c.name) === sameName(name));
    if (found) ids[key] = found.id;
  }
  known = {given: env.DISCORD_GUILD_ID, at: Date.now(), guild, ids};
  return ids;
}

/** Posts to one of the server's channels; {sent, channel, id} or the reason it could not. */
export async function postTo(env, key, content) {
  if (!configured(env)) return {sent: false, reason: 'discord_unconfigured'};
  try {
    const channel = (await channels(env))[key];
    if (!channel) return {sent: false, reason: 'no_channel'};
    const response = await call(env, 'POST', `/channels/${channel}/messages`, {content: clip(content, MESSAGE_LIMIT), allowed_mentions: {parse: []}});
    if (!response.ok) return {sent: false, reason: `discord ${response.status}: ${clip(await response.text(), 200)}`};
    return {sent: true, channel, id: (await response.json()).id};
  } catch (error) {
    return {sent: false, reason: clip(error?.message || error, 200)};
  }
}

/** Rewrites one of the bot's own messages (the review line once Gemini has answered). */
export async function editMessage(env, where, content) {
  if (!configured(env) || !where?.channel || !where?.id) return false;
  try { const r = await call(env, 'PATCH', `/channels/${where.channel}/messages/${where.id}`, {content: clip(content, MESSAGE_LIMIT), allowed_mentions: {parse: []}}); return r.ok; }
  catch { return false; }
}

/** Takes one of the bot's own messages away (a review that has been decided). */
export async function removeMessage(env, where) {
  if (!configured(env) || !where?.channel || !where?.id) return false;
  try { const r = await call(env, 'DELETE', `/channels/${where.channel}/messages/${where.id}`); return r.ok || r.status === 404; }
  catch { return false; }
}

/** The channel when there is one, a direct message otherwise. */
async function deliver(env, key, compose) {
  const posted = await postTo(env, key, compose());
  if (posted.sent || posted.reason === 'discord_unconfigured') return posted;
  const direct = await send(env, compose);
  return direct.sent ? {...direct, fallback: posted.reason} : {...direct, reason: posted.reason + '; ' + direct.reason};
}

export async function notifyCrash(env, row) {
  return deliver(env, 'crashes', () => crashMessage(parseCrash(row)));
}

const by = meta => meta?.by ? ' от @' + plain(clip(meta.by, 40)) : '';

export function approvedMessage(meta, origin) {
  const head = meta.previous
    ? `✅ **Обновление проверено и опубликовано:** ${plain(clip(meta.name || meta.id, 80))} \`${plain(clip(meta.id, 80))}\` v${plain(clip(meta.previous, 20))} → v${plain(clip(meta.version || '?', 20))}${by(meta)}`
    : `✅ **Проверен и опубликован:** ${plain(clip(meta.name || meta.id, 80))} \`${plain(clip(meta.id, 80))}\` v${plain(clip(meta.version || '?', 20))}${by(meta)}`;
  return [head,
    meta.changes ? '**Что нового:** ' + plain(clip(meta.changes, 400)) : '',
    meta.author ? 'Авторы: ' + plain(clip(meta.author, 120)) : '',
    Array.isArray(meta.permissions) && meta.permissions.length ? 'Разрешения: ' + meta.permissions.map(p => '`' + plain(p) + '`').join(' ') : '',
    meta.about ? '> ' + plain(clip(meta.about, 300)).replace(/\n/g, '\n> ') : '',
    `${origin}/store.html`].filter(Boolean).join('\n');
}

export function storeMessage(kind, meta, origin) {
  if (kind === 'removed') return `🗑️ **Убран из магазина:** ${plain(clip(meta.name || meta.id, 80))} \`${plain(clip(meta.id, 80))}\``;
  if (kind === 'updated') return [`🔄 **Обновлён в магазине:** ${plain(clip(meta.name || meta.id, 80))} ${meta.previous ? 'v' + plain(clip(meta.previous, 20)) + ' → ' : ''}v${plain(clip(meta.version || '?', 20))}${by(meta)}`,
    meta.changes ? '**Что нового:** ' + plain(clip(meta.changes, 400)) : '', `${origin}/store.html`].filter(Boolean).join('\n');
  return [`🛒 **Новый в магазине:** ${plain(clip(meta.name || meta.id, 80))} v${plain(clip(meta.version || '?', 20))}${by(meta)}`,
    meta.about ? '> ' + plain(clip(meta.about, 300)).replace(/\n/g, '\n> ') : '', `${origin}/store.html`].filter(Boolean).join('\n');
}

/** What the admin page shows: who the bot is, the link that invites it, which channels it found. */
export async function discordStatus(env) {
  if (!configured(env)) return {configured: false};
  const me = await call(env, 'GET', '/users/@me');
  const bot = me.ok ? await me.json() : null;
  const out = {configured: true, bot: bot ? {id: bot.id, name: bot.username} : null, guild: env.DISCORD_GUILD_ID || null};
  if (bot) out.invite = `https://discord.com/oauth2/authorize?client_id=${bot.id}&scope=bot&permissions=84992`;
  try { out.channels = await channels(env, true); out.missing = Object.keys(CHANNELS).filter(k => !out.channels[k]).map(k => CHANNELS[k]); }
  catch (error) { out.error = String(error?.message || error); }
  return out;
}

/** A plugin waiting for review, as a DM with a link to the review page. Best effort, like a crash. */
export function pluginMessage(item, origin) {
  const meta = item?.meta || {};
  const files = item?.files ?? 0;
  const update = meta.update;
  const diff = update?.diff;
  return clip([
    update ? '🔄 **Обновление плагина на проверку**' : '🧩 **Новый плагин на проверку**',
    `**${plain(clip(meta.name || meta.id || '?', 80))}** \`${plain(clip(meta.id || '?', 80))}\` ${update ? 'v' + plain(clip(update.from || '?', 20)) + ' → ' : ''}v${plain(clip(meta.version || '?', 20))}`,
    `от ${item?.handle ? '@' + plain(clip(item.handle, 40)) : 'без ника'}${meta.author ? ' · авторы: ' + plain(clip(meta.author, 120)) : ''}`,
    `файлов: ${files}`,
    diff ? `изменения в коде: +${diff.plus} −${diff.minus} строк` + [['новые', diff.added], ['изменены', diff.changed], ['удалены', diff.removed]]
      .filter(([, list]) => list?.length).map(([word, list]) => ` · ${word}: ` + list.slice(0, 6).map(f => '`' + plain(clip(f.replace(/^java\//, ''), 60)) + '`').join(', ')).join('') : '',
    meta.changes ? '**Что нового:** ' + plain(clip(meta.changes, 400)) : '',
    verdictLine(item?.review),
    ...(Array.isArray(item?.review?.issues) ? item.review.issues.slice(0, 5).map(issue => '• ' + plain(clip(issue, 200))) : []),
    meta.description ? '> ' + plain(clip(meta.description, 300)).replace(/\n/g, '\n> ') : '',
    `Проверить: ${origin}/review.html`,
  ].filter(Boolean).join('\n'), MESSAGE_LIMIT);
}

const VERDICTS = {pending: '⏳ Gemini проверяет код…', safe: '🟢 Gemini: безопасен', suspicious: '🟡 Gemini: подозрительный', dangerous: '🔴 Gemini: опасен', unknown: '⚪ Gemini: не проверил'};
function verdictLine(review) {
  if (!review) return '';
  if (review.verdict === 'pending') return VERDICTS.pending;
  const head = review.obfuscated ? '⛔ Gemini: код запутан — заявка отклонена автоматически' : (VERDICTS[review.verdict] || VERDICTS.unknown);
  return head + (review.summary ? ' — ' + plain(clip(review.summary, 400)) : '');
}

export async function notifyPlugin(env, item, origin) {
  return deliver(env, 'review', () => pluginMessage(item, origin));
}

async function send(env, compose) {
  if (!configured(env)) return { sent: false, reason: 'discord_unconfigured' };
  const token = env.DISCORD_BOT_TOKEN;
  try {
    const content = compose();
    for (const forget of [false, true]) {
      const channel = await dmChannel(token, env, forget);
      const response = await fetch(`${API}/channels/${channel}/messages`, {
        method: 'POST',
        headers: {
          authorization: `Bot ${token}`,
          'content-type': 'application/json',
          'user-agent': 'ttcuz-crash (worker, 1.0)',
        },
        body: JSON.stringify({ content, allowed_mentions: { parse: [] } }),
        signal: AbortSignal.timeout(TIMEOUT),
      });
      if (response.ok) return { sent: true };
      // A remembered channel that no longer exists is opened once more, so a
      // conversation deleted on either side does not silence the next crash.
      if (response.status !== 404 || forget) {
        return {
          sent: false,
          reason: `discord ${response.status}: ${clip(await response.text(), 200)}`,
        };
      }
    }
    return { sent: false, reason: 'discord: no channel' };
  } catch (error) {
    return { sent: false, reason: clip(error?.message || error, 200) };
  }
}
