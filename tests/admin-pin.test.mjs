import test from 'node:test';
import assert from 'node:assert/strict';
import {webcrypto} from 'node:crypto';
if(!globalThis.crypto)globalThis.crypto=webcrypto;
import {admin} from '../server/cloudflare/admin.mjs';

// limits rows only: what the PIN check counts
function limits(){
  const rows=new Map();
  return {rows,prepare(sql){return {args:[],bind(...a){this.args=a;return this;},
    async first(){return rows.has(this.args[0])?{hits:rows.get(this.args[0])}:null;},
    async run(){rows.set(this.args[0],(rows.get(this.args[0])||0)+1);return {};}};}};
}
const ask=(pin,ip='1.1.1.1')=>new Request('https://mod/plugins/native/admin/pending',{headers:{'x-admin-pin':pin,'cf-connecting-ip':ip}});
// a test secret only: the real PIN is never in the code
const PIN='test-pin-only-in-tests';

test('the PIN set as a Worker secret opens moderation and nothing is counted',async()=>{
  const PROFILES=limits();
  assert.equal(await admin(ask(PIN),{PROFILES,ADMIN_PIN:PIN}),'ok');
  assert.equal(PROFILES.rows.size,0);
});

test('without the secret the PIN door is shut, whatever is typed',async()=>{
  const PROFILES=limits();
  assert.equal(await admin(ask(PIN),{PROFILES}),'none');
  assert.equal(await admin(ask('49759178'),{PROFILES}),'none');
  assert.equal(await admin(ask(PIN),{PROFILES,ADMIN_PIN:'short'}),'none');
});

test('no PIN, a malformed PIN and a wrong PIN are refused',async()=>{
  const PROFILES=limits();
  assert.equal(await admin(new Request('https://mod/x'),{PROFILES,ADMIN_PIN:PIN}),'none');
  assert.equal(await admin(ask('abc'),{PROFILES,ADMIN_PIN:PIN}),'wrong');
  assert.equal(await admin(ask('wrong-pin-12345'),{PROFILES,ADMIN_PIN:PIN}),'wrong');
  assert.equal(await admin(ask(PIN+'x'),{PROFILES,ADMIN_PIN:PIN}),'wrong');
});

test('five wrong PINs lock that address for the hour, even with the right one; others are not locked',async()=>{
  const PROFILES=limits();
  for(let i=0;i<5;i++)assert.equal(await admin(ask('wrong-pin-000'+i),{PROFILES,ADMIN_PIN:PIN}),'wrong');
  assert.equal(await admin(ask(PIN),{PROFILES,ADMIN_PIN:PIN}),'locked');
  assert.equal(await admin(ask(PIN,'2.2.2.2'),{PROFILES,ADMIN_PIN:PIN}),'ok');
});

test('a Worker token works only when it is long enough, and an empty one never matches',async()=>{
  const PROFILES=limits();
  const token='a-long-worker-token-1234';
  assert.equal(await admin(new Request('https://mod/x',{headers:{authorization:'Bearer '+token}}),{PROFILES,PLUGIN_ADMIN_TOKEN:token}),'ok');
  assert.equal(await admin(new Request('https://mod/x',{headers:{authorization:'Bearer t0ken'}}),{PROFILES,PLUGIN_ADMIN_TOKEN:'t0ken'}),'none');
  assert.equal(await admin(new Request('https://mod/x',{headers:{authorization:'Bearer '}}),{PROFILES,PLUGIN_ADMIN_TOKEN:''}),'none');
});
