import test from 'node:test';
import assert from 'node:assert/strict';
import {webcrypto} from 'node:crypto';
if (!globalThis.crypto) globalThis.crypto = webcrypto;
import {postTo, removeMessage, notifyCrash, CHANNELS} from '../server/cloudflare/discord.mjs';

// A fake Discord: one server with the four channels, and a record of what the bot did.
function discord() {
  const done = [];
  const list = [{id: '11', type: 0, name: 'плагины-на-проверку'}, {id: '12', type: 0, name: 'проверенные-плагины'},
    {id: '13', type: 0, name: 'магазин'}, {id: '14', type: 0, name: 'крашлоги'}, {id: '15', type: 2, name: 'магазин'}];
  globalThis.fetch = async (url, init = {}) => {
    const path = new URL(url).pathname.replace('/api/v10', ''), method = init.method || 'GET';
    done.push(method + ' ' + path);
    if (path === '/guilds/77700001') return Response.json({id: '77700001'});
    if (path === '/guilds/77700001/channels') return Response.json(list);
    if (method === 'POST' && path.startsWith('/channels/')) return Response.json({id: 'm' + done.length});
    if (method === 'DELETE') return new Response(null, {status: 204});
    return new Response('{}', {status: 404});
  };
  return done;
}

test('news goes to its own channel, found by name', async () => {
  const done = discord();
  const env = {DISCORD_BOT_TOKEN: 't', DISCORD_GUILD_ID: '77700001'};
  const review = await postTo(env, 'review', 'hi');
  assert.equal(review.sent, true); assert.equal(review.channel, '11');
  assert.equal((await postTo(env, 'approved', 'ok')).channel, '12');
  assert.equal((await postTo(env, 'store', 'new')).channel, '13');
  assert.ok(await removeMessage(env, review));
  assert.ok(done.includes('DELETE /channels/11/messages/' + review.id));
  const crash = await notifyCrash(env, {id: 'c', version: '0.7.5', stack: 'java.lang.Error: x', occurred_at: 1});
  assert.equal(crash.channel, '14');
  assert.deepEqual(Object.keys(CHANNELS), ['review', 'approved', 'store', 'crashes']);
});

test('without the server everything still arrives as a direct message', async () => {
  const done = discord();
  globalThis.fetch = async (url, init = {}) => {
    const path = new URL(url).pathname.replace('/api/v10', '');
    done.push((init.method || 'GET') + ' ' + path);
    if (path === '/users/@me/channels') return Response.json({id: 'dm'});
    return Response.json({id: 'x'});
  };
  const sent = await notifyCrash({DISCORD_BOT_TOKEN: 't', DISCORD_USER_ID: '5'}, {id: 'c', stack: 'x', occurred_at: 1});
  assert.equal(sent.sent, true);
  assert.ok(done.includes('POST /channels/dm/messages'));
});
