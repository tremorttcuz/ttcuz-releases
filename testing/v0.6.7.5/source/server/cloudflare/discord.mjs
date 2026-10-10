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
  const pick = frames.find(line => !/cat\.narezany\.margyt/.test(line)) || frames[0];
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
export async function notifyCrash(env, row) {
  if (!configured(env)) return { sent: false, reason: 'discord_unconfigured' };
  const token = env.DISCORD_BOT_TOKEN;
  try {
    const content = crashMessage(parseCrash(row));
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
