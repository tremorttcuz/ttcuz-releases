import test from 'node:test';
import assert from 'node:assert/strict';
import {webcrypto} from 'node:crypto';
if(!globalThis.crypto)globalThis.crypto=webcrypto;
import {profileRoute} from '../server/cloudflare/profiles.mjs';

test('the directory lists only accounts with a profile, with when each changed',async()=>{
  let asked;
  const env={PROFILES:{prepare(sql){asked=sql;return {async all(){return {results:[{uid:'111',updated_at:5},{uid:'222',updated_at:9}]};}};}}};
  const response=await profileRoute(new Request('https://mod/v1/directory'),env);
  assert.equal(response.status,200);
  const body=await response.json();
  assert.deepEqual(body.profiles,{'111':5,'222':9});
  assert.match(asked,/metadata IS NOT NULL/);
  assert.match(response.headers.get('cache-control'),/max-age/);
});
