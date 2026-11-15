import test from 'node:test';
import assert from 'node:assert/strict';
import {webcrypto} from 'node:crypto';
if(!globalThis.crypto)globalThis.crypto=webcrypto;
import {pluginRoute} from '../server/cloudflare/plugins.mjs';
import {digest} from '../server/cloudflare/profiles.mjs';

const hex=async text=>Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(text))),n=>n.toString(16).padStart(2,'0')).join('');
const ownerOf=async uid=>(await hex('ttcuz-plugin-owner:'+uid)).slice(0,32);

// D1 stand-in: profiles, one store plugin owned by account 1, and the daily history.
function database(profiles,owner){
  const reactions=new Map(),daily=new Map();
  const statement=sql=>({sql,args:[],bind(...a){this.args=a;return this;},
    async run(){
      const a=this.args;
      if(sql.startsWith('INSERT INTO plugin_reactions'))reactions.set(a[0]+'|'+a[1],{plugin_id:a[0],voter:a[1],emoji:a[2]});
      if(sql.startsWith('DELETE FROM plugin_reactions'))reactions.delete(a[0]+'|'+a[1]);
      if(sql.startsWith('INSERT INTO plugin_daily')){const k=a[0]+'|'+a[1],row=daily.get(k)||{id:a[0],day:a[1],downloads:0,likes:0,dislikes:0};row[sql.match(/plugin_daily\((?:id,day,)(\w+)\)/)[1]]++;daily.set(k,row);}
      return {};
    },
    async first(){
      const a=this.args;
      if(sql.startsWith('SELECT owner_hash FROM profiles'))return profiles[a[0]]?{owner_hash:profiles[a[0]]}:null;
      if(sql.startsWith('SELECT owner_hash,verified FROM profiles'))return profiles[a[0]]?{owner_hash:profiles[a[0]],verified:0}:null;
      if(sql.startsWith('SELECT 1 AS ok FROM plugin_packages'))return a[0]==='nick.up'?{ok:1}:null;
      if(sql.startsWith('SELECT metadata FROM plugin_packages'))return a[0]==='nick.up'?{metadata:JSON.stringify({id:'nick.up',name:'Up',owner})}:null;
      if(sql.startsWith('INSERT INTO limits'))return {hits:1};
      return null;
    },
    async all(){
      const a=this.args;
      if(sql.startsWith('SELECT emoji,COUNT(*)')){const by={};for(const r of reactions.values())if(r.plugin_id===a[0])by[r.emoji]=(by[r.emoji]||0)+1;return {results:Object.entries(by).map(([emoji,n])=>({emoji,n}))};}
      if(sql.startsWith('SELECT day,downloads'))return {results:[...daily.values()].filter(r=>r.id===a[0]&&r.day>=a[1])};
      return {results:[]};
    }});
  return {reactions,daily,prepare:statement,async batch(l){return Promise.all(l.map(s=>s.run().then(()=>({results:[]}))));}};
}
const call=(env,path,body)=>pluginRoute(new Request('https://mod'+path,body?{method:'POST',body:JSON.stringify(body)}:{}),env);

test('statistics: the author sees them, nobody else does',async()=>{
  const author='a'.repeat(64),fan='b'.repeat(64);
  const PROFILES=database({'1':await digest(author),'2':await digest(fan)},await ownerOf('1'));
  const env={PROFILES};
  assert.equal((await call(env,'/plugins/stats/nick.up',{})).status,403,'no account');
  const refused=await call(env,'/plugins/stats/nick.up',{uid:'2',token:fan});
  assert.equal(refused.status,403);assert.equal((await refused.json()).error,'not_owner');
  assert.equal((await call(env,'/plugins/stats/nick.up',{uid:'1',token:fan})).status,403,'the author\'s id with another token');
  assert.equal((await call(env,'/plugins/stats/nick.gone',{uid:'1',token:author})).status,404);
  assert.equal((await call(env,'/plugins/stats/nick.up')).status,405,'not by GET');
  const stats=await (await call(env,'/plugins/stats/nick.up',{uid:'1',token:author})).json();
  assert.equal(stats.ok,true);
  assert.equal(stats.days.length,90);
  assert.ok('downloads' in stats&&'up' in stats&&'dn' in stats&&!('reactions' in stats));
});
