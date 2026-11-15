import test from 'node:test';
import assert from 'node:assert/strict';
import {webcrypto} from 'node:crypto';
if(!globalThis.crypto)globalThis.crypto=webcrypto;
import {pluginRoute} from '../server/cloudflare/plugins.mjs';
import {makeCtt,base64} from './helpers/plugin-package.mjs';

// A tiny in-memory stand-in for D1, enough for the native submission queue and the store.
function database(){
  const rows=[],packages=new Map();let next=1;
  const statement=sql=>({sql,args:[],bind(...args){this.args=args;return this;},
    async run(){
      if(sql.startsWith("UPDATE native_submissions SET state='superseded'"))for(const r of rows)if(r.plugin_id===this.args[0]&&(sql.includes('uid=?')?r.uid===this.args[1]:r.handle===this.args[1])&&r.state==='pending')r.state='superseded';
      if(sql.startsWith("UPDATE native_submissions SET state='rejected'"))rows.find(r=>r.id===this.args[0]).state='rejected';
      if(sql.startsWith("UPDATE native_submissions SET state='approved'"))rows.find(r=>r.id===this.args[0]).state='approved';
      if(sql.startsWith('INSERT INTO plugin_packages'))packages.set(this.args[0],{metadata:this.args[1],archive:this.args[2],icon:this.args[3]});
      return {};
    },
    async first(){
      if(sql.startsWith('INSERT INTO limits'))return {hits:1};
      if(sql.startsWith('SELECT COUNT(*)'))return {count:rows.filter(r=>r.state==='pending').length};
      if(sql.startsWith('INSERT INTO native_submissions')){const [plugin_id,metadata,sources,archive,icon,handle,uid,added]=this.args;const r={id:next++,plugin_id,metadata,sources,archive,icon,handle,uid,added,state:'pending'};rows.push(r);return {id:r.id};}
      if(sql.startsWith('SELECT * FROM native_submissions'))return rows.find(r=>r.id===this.args[0]&&r.state==='pending')||null;
      if(sql.startsWith('SELECT archive,icon FROM plugin_packages'))return packages.get(this.args[0])||null;
      return null;
    },
    async all(){
      if(sql.includes('FROM native_submissions'))return {results:rows.filter(r=>r.state==='pending').map(r=>({...r,packed:r.archive.length}))};
      if(sql.includes('FROM plugin_packages'))return {results:[...packages.values()].map(p=>({metadata:p.metadata}))};
      return {results:[]};
    }});
  return {rows,packages,prepare:statement,async batch(list){for(const s of list)await s.run();}};
}

const png=btoa(String.fromCharCode(137,80,78,71,13,10,26,10));
const ctt=base64(makeCtt('nick.hello','nick.hello.Main',['Lnick/hello/Main;']));
const plugin={metadata:{id:'nick.hello',name:'Hello',version:'1.0',author:'nick, friend',entry:'nick.hello.Main',description:'Says hi'},
  files:[{path:'java/nick/hello/Main.java',text:'package nick.hello; public class Main {}'}],archive:ctt,icon:png,handle:'@nick',uid:'123'};
const post=(env,path,body,headers={})=>pluginRoute(new Request('https://mod'+path,{method:'POST',headers,body:JSON.stringify(body)}),env);

test('the editor submits, an admin approves, the store lists it with its authors',async()=>{
  const PROFILES=database();
  const env={PROFILES,PLUGIN_ADMIN_TOKEN:'secret-worker-token-0001',ADMIN_PIN:'test-pin-only-in-tests',ASSETS:{async fetch(){return Response.json({plugins:[]});}}};
  const sent=await post(env,'/plugins/native/submit',plugin);
  assert.equal(sent.status,202);assert.equal((await sent.json()).status,'pending');
  assert.equal(PROFILES.rows[0].handle,'nick');

  assert.equal((await pluginRoute(new Request('https://mod/plugins/native/admin/pending'),env)).status,403);
  const byPin=await pluginRoute(new Request('https://mod/plugins/native/admin/pending',{headers:{'x-admin-pin':'test-pin-only-in-tests'}}),env);
  assert.equal(byPin.status,200);
  const pending=await (await pluginRoute(new Request('https://mod/plugins/native/admin/pending',{headers:{authorization:'Bearer secret-worker-token-0001'}}),env)).json();
  assert.equal(pending.submissions.length,1);assert.equal(pending.submissions[0].files[0].path,'java/nick/hello/Main.java');

  const review=await post(env,'/plugins/native/admin/review',{id:pending.submissions[0].id,action:'approve'},{authorization:'Bearer secret-worker-token-0001'});
  assert.equal(review.status,200);
  const catalog=await (await pluginRoute(new Request('https://mod/plugins/catalog.json'),env)).json();
  const listed=catalog.plugins[0];
  assert.equal(listed.id,'nick.hello');assert.equal(listed.author,'nick, friend');assert.equal(listed.by,'nick');
  assert.equal(listed.format,'ctt');assert.match(listed.icon,/^\/plugins\/files\/nick\.hello\/icon\.png\?v=[0-9a-f]{12}$/);
  const file=await pluginRoute(new Request('https://mod/plugins/files/nick.hello/archive.ctt'),env);
  assert.deepEqual(new Uint8Array(await file.arrayBuffer()),Uint8Array.from(atob(ctt),c=>c.charCodeAt(0)));
});

test('bad submissions are refused before they reach the queue',async()=>{
  const PROFILES=database();const env={PROFILES};
  for(const broken of [{...plugin,archive:btoa('nope')},{...plugin,files:[{path:'../x.java',text:''}]},{...plugin,metadata:{...plugin.metadata,entry:'Main'}},{...plugin,icon:btoa('GIF89a')}]){
    assert.equal((await post(env,'/plugins/native/submit',broken)).status,400);
  }
  assert.equal(PROFILES.rows.length,0);
});

test('a second send from the same author replaces the one still waiting; reject leaves the store alone',async()=>{
  const PROFILES=database();const env={PROFILES,PLUGIN_ADMIN_TOKEN:'secret-worker-token-0001',ADMIN_PIN:'test-pin-only-in-tests'};
  await post(env,'/plugins/native/submit',plugin);await post(env,'/plugins/native/submit',plugin);
  assert.deepEqual(PROFILES.rows.map(r=>r.state),['superseded','pending']);
  const answer=await post(env,'/plugins/native/admin/review',{id:2,action:'reject'},{authorization:'Bearer secret-worker-token-0001'});
  assert.equal((await answer.json()).status,'rejected');assert.equal(PROFILES.packages.size,0);
});

test('obfuscated code or a package that is not built from the sources never reaches the queue',async()=>{
  const PROFILES=database();
  const env={PROFILES,ADMIN_PIN:'test-pin-only-in-tests',ASSETS:{async fetch(){return Response.json({plugins:[]});}}};
  const obfuscated={...plugin,files:[{path:'java/nick/hello/Main.java',text:'package nick.hello; public class Main { int q; int w; int f; int u; void l(){} void o(){} void d(){} void bb(){} }'}]};
  const refused=await post(env,'/plugins/native/submit',obfuscated);
  assert.equal(refused.status,422);assert.equal((await refused.json()).error,'obfuscated');
  const swapped={...plugin,archive:base64(makeCtt('nick.hello','nick.hello.Main',['Lnick/hello/Main;','Lnick/hello/Hidden;']))};
  const mismatch=await post(env,'/plugins/native/submit',swapped);
  assert.equal(mismatch.status,422);assert.equal((await mismatch.json()).error,'package_mismatch');
  assert.equal(PROFILES.rows.length,0);
});
