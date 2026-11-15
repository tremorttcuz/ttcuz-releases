import test from 'node:test';
import assert from 'node:assert/strict';
import {pluginMessage,notifyPlugin} from '../server/cloudflare/discord.mjs';

test('a plugin submission is described with a review link', () => {
  const text = pluginMessage({meta:{id:'cat.demo',name:'Demo',version:'1.0',author:'a, b',description:'does\nthings'},handle:'someone',files:3},'https://ttcuz.ttcuz1337.workers.dev');
  assert.match(text, /Demo/);
  assert.match(text, /@someone/);
  assert.match(text, /файлов: 3/);
  assert.match(text, /https:\/\/ttcuz\.ttcuz1337\.workers\.dev\/review\.html/);
});

test('without a bot token nothing is sent and nothing throws', async () => {
  assert.deepEqual(await notifyPlugin({}, {meta:{id:'x'}}, 'https://x'), {sent:false,reason:'discord_unconfigured'});
});
