import test from 'node:test';
import assert from 'node:assert/strict';
import {webcrypto} from 'node:crypto';
if(!globalThis.crypto)globalThis.crypto=webcrypto;
import {pluginRoute,validateScript} from '../server/cloudflare/plugins.mjs';

const script={schema:1,id:'demo.commands',name:'Demo',version:'1.0',author:'ttcuz',rows:[{title:'Hi',action:'window',value:'Hello'}]};
const hex=async value=>Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(value))),n=>n.toString(16).padStart(2,'0')).join('');
const source='{"author":"ttcuz","id":"demo.commands","name":"Demo","rows":[{"action":"window","title":"Hi","value":"Hello"}],"schema":1,"version":"1.0"}';

test('plugin scripts accept bounded commands and reject unknown executable fields',()=>{
  assert.equal(validateScript(script).id,'demo.commands');
  assert.throws(()=>validateScript({...script,eval:'alert(1)'}));
  assert.throws(()=>validateScript({...script,rows:[{title:'Bad',action:'exec',value:'cmd'}]}));
  assert.throws(()=>validateScript({...script,name:'x'.repeat(81)}));
});

test('binary catalog is served from Worker assets and submissions enter the review queue',async()=>{
  let inserted;
  const env={ASSETS:{async fetch(){return Response.json({plugins:[]});}},PROFILES:{prepare(sql){const query={sql,args:[],bind(...args){this.args=args;return this;},async first(){if(sql.startsWith('INSERT INTO limits'))return{hits:1};if(sql.startsWith('SELECT COUNT(*)'))return{count:0};return null;},async run(){if(sql.startsWith('INSERT OR IGNORE INTO script_submissions'))inserted=this.args;return{};},async all(){if(sql.includes("state='approved'"))return{results:[{digest:await hex(source),source}]};return{results:[]};}};return query;}}};
  const catalog=await pluginRoute(new Request('https://mod/plugins/catalog.json'),env);
  assert.deepEqual(await catalog.json(),{plugins:[]});
  const submit=await pluginRoute(new Request('https://mod/plugins/scripts/submit',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify(script)}),env);
  assert.equal(submit.status,202);assert.equal((await submit.json()).status,'pending');assert.equal(inserted[1],script.id);
  const published=await pluginRoute(new Request('https://mod/plugins/scripts'),env);
  assert.deepEqual((await published.json()).scripts,[JSON.parse(source)]);
});

test('plugin review endpoints fail closed without the admin secret',async()=>{
  const response=await pluginRoute(new Request('https://mod/plugins/scripts/admin/pending'),{PROFILES:{}});
  assert.equal(response.status,503);
});

test('store administration requires the verified creator device token, never just a UID',async()=>{
 const token='a'.repeat(64),hash=await hex(token);let writes=0;
 const env={PROFILES:{prepare(sql){return{bind(){return this;},async first(){return{owner_hash:hash,verified:1};},async run(){writes++;},async all(){return{results:[]};}};}}};
 const send=body=>pluginRoute(new Request('https://mod/plugins/admin',{method:'POST',body:JSON.stringify(body)}),env);
 for(const identity of [{uid:'123',token},{uid:'7491898648855512119',token:'b'.repeat(64)},{uid:'7491898648855512119'}]){
  assert.equal((await send({...identity,action:'remove',id:'demo.commands',kind:'script'})).status,403);
 }
 assert.equal(writes,0);
 const response=await send({uid:'7491898648855512119',token,action:'remove',id:'demo.commands',kind:'script'});
 assert.equal(response.status,200);assert.equal((await response.json()).ok,true);assert.equal(writes,1);
});

test('creator publishes scripts directly and replaces the earlier store version',async()=>{
 const token='a'.repeat(64);let statements;
 const env={PROFILES:{prepare(sql){return{sql,bind(...args){this.args=args;return this;},async first(){return{owner_hash:await hex(token),verified:1};}};},async batch(value){statements=value;}}};
 const response=await pluginRoute(new Request('https://mod/plugins/admin',{method:'POST',body:JSON.stringify({uid:'7491898648855512119',token,action:'publish-script',source:script})}),env);
 assert.equal(response.status,200);assert.equal((await response.json()).ok,true);
 assert.match(statements[0].sql,/superseded/);assert.equal(statements[1].args[1],script.id);assert.equal(statements[1].args[2],source);
});

test('uploaded package bytes, catalog digest and download remain identical',async()=>{
 const token='a'.repeat(64),bytes=new Uint8Array([80,75,3,4,1,2,3]),archive=btoa(String.fromCharCode(...bytes));let saved;
 const env={ASSETS:{async fetch(){return Response.json({plugins:[]});}},PROFILES:{prepare(sql){return{bind(...args){this.args=args;return this;},async first(){return sql.includes('owner_hash')?{owner_hash:await hex(token),verified:1}:{archive,icon:null};},async run(){saved=this.args;},async all(){return{results:saved?[{metadata:saved[1]}]:[]};}};}}};
 const metadata={id:'test.plugin',name:'Test',version:'1',author:'Admin',about:'',tiktok:''};
 const response=await pluginRoute(new Request('https://mod/plugins/admin',{method:'POST',body:JSON.stringify({uid:'7491898648855512119',token,action:'publish-package',metadata,archive})}),env);
 assert.equal(response.status,200);
 const catalog=await (await pluginRoute(new Request('https://mod/plugins/catalog.json'),env)).json();
 const hash=Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',bytes)),n=>n.toString(16).padStart(2,'0')).join('');
 assert.equal(catalog.plugins[0].sha256,hash);assert.equal(catalog.plugins[0].size,bytes.length);
 const download=await pluginRoute(new Request('https://mod'+catalog.plugins[0].url),env);
 assert.deepEqual(new Uint8Array(await download.arrayBuffer()),bytes);
});
