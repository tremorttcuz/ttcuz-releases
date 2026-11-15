import test from 'node:test';
import assert from 'node:assert/strict';
import {webcrypto} from 'node:crypto';
if(!globalThis.crypto)globalThis.crypto=webcrypto;
import {pluginRoute,newerVersion,sourceChanges} from '../server/cloudflare/plugins.mjs';
import {pluginMessage,storeMessage,approvedMessage} from '../server/cloudflare/discord.mjs';
import {makeCtt,base64} from './helpers/plugin-package.mjs';

// Enough of D1 for a submission, its approval and the store row.
function database(){
  const rows=[],packages=new Map();let next=1;
  const statement=sql=>({args:[],bind(...args){this.args=args;return this;},
    async run(){
      if(sql.startsWith("UPDATE native_submissions SET state='superseded'"))for(const r of rows)if(r.plugin_id===this.args[0]&&r.handle===this.args[1]&&r.state==='pending')r.state='superseded';
      if(sql.startsWith("UPDATE native_submissions SET state='approved'"))rows.find(r=>r.id===this.args[0]).state='approved';
      if(sql.startsWith('INSERT INTO plugin_packages'))packages.set(this.args[0],{metadata:this.args[1],archive:this.args[2],icon:this.args[3]});
      return {};
    },
    async first(){
      if(sql.startsWith('INSERT INTO limits'))return {hits:1};
      if(sql.startsWith('SELECT COUNT(*)'))return {count:0};
      if(sql.startsWith('SELECT metadata FROM plugin_packages WHERE id=?'))return packages.get(this.args[0])||null;
      if(sql.startsWith("SELECT sources FROM native_submissions"))return [...rows].reverse().find(r=>r.plugin_id===this.args[0]&&r.state==='approved')||null;
      if(sql.startsWith('INSERT INTO native_submissions')){const [plugin_id,metadata,sources,archive,icon,handle,uid,added]=this.args;rows.push({id:next,plugin_id,metadata,sources,archive,icon,handle,uid,added,state:'pending'});return {id:next++};}
      if(sql.startsWith('SELECT * FROM native_submissions'))return rows.find(r=>r.id===this.args[0]&&r.state==='pending')||null;
      return null;
    },
    async all(){return {results:[...packages.values()].map(p=>({metadata:p.metadata}))};}});
  return {rows,packages,prepare:statement,async batch(list){for(const s of list)await s.run();}};
}
const archive=base64(makeCtt('nick.up','nick.up.Main',['Lnick/up/Main;']));
const send=(version,text,{uid='42',handle='nick',changes}={})=>({metadata:{id:'nick.up',name:'Up',version,author:'nick',entry:'nick.up.Main',...(changes?{changes}:{})},
  files:[{path:'java/nick/up/Main.java',text}],archive,handle,uid});
const post=(env,path,body,headers={})=>pluginRoute(new Request('https://mod'+path,{method:'POST',headers,body:JSON.stringify(body)}),env);

test('versions compare as numbers',()=>{
  assert.ok(newerVersion('1.10','1.9'));assert.ok(newerVersion('2','1.9.9'));assert.ok(newerVersion('1.0.1','1.0'));
  assert.ok(!newerVersion('1.0','1.0'));assert.ok(!newerVersion('1.9','1.10'));assert.ok(!newerVersion('1.0','1.0.1'));
});

test('the author sends an update: checked, shown as an update, approved as one; others and old versions are refused',async()=>{
  const PROFILES=database(),env={PROFILES,PLUGIN_ADMIN_TOKEN:'secret-worker-token-0001'},admin={authorization:'Bearer secret-worker-token-0001'};
  assert.equal((await post(env,'/plugins/native/submit',send('1.0','package nick.up;\npublic class Main {}'))).status,202);
  assert.equal((await post(env,'/plugins/native/admin/review',{id:1,action:'approve'},admin)).status,200);
  const stored=JSON.parse(PROFILES.packages.get('nick.up').metadata);assert.ok(stored.owner);
  const catalog=await (await pluginRoute(new Request('https://mod/plugins/catalog.json'),{...env,ASSETS:{async fetch(){return Response.json({plugins:[]});}}})).json();
  assert.equal(catalog.plugins[0].owner,undefined,'the owner hash never reaches the catalog');

  assert.equal((await post(env,'/plugins/native/submit',send('1.1','package nick.up;\npublic class Main {}',{uid:'7',handle:'thief'}))).status,403);
  assert.equal((await post(env,'/plugins/native/submit',send('1.0','package nick.up;\npublic class Main {}'))).status,409);

  const sent=await post(env,'/plugins/native/submit',send('1.1','package nick.up;\npublic class Main { int n; }',{changes:'Счётчик'}));
  assert.equal(sent.status,202);assert.deepEqual((await sent.json()).update,{from:'1.0'});
  const meta=JSON.parse(PROFILES.rows[1].metadata);
  assert.equal(meta.update.from,'1.0');assert.deepEqual(meta.update.diff.changed,['java/nick/up/Main.java']);
  const message=pluginMessage({meta,handle:'nick',files:1},'https://mod');
  assert.match(message,/Обновление плагина/);assert.match(message,/v1\.0 → v1\.1/);assert.match(message,/Что нового:\*\* Счётчик/);

  const approved=await (await post(env,'/plugins/native/admin/review',{id:2,action:'approve'},admin)).json();
  assert.equal(approved.plugin.previous,'1.0');assert.equal(approved.plugin.owner,undefined);
  assert.match(storeMessage('updated',approved.plugin,'https://mod'),/Обновлён в магазине.*v1\.0 → v1\.1/);
  assert.match(approvedMessage(approved.plugin,'https://mod'),/Обновление проверено/);
});

test('changes count lines and files',()=>{
  const d=sourceChanges([{path:'a',text:'1\n2'},{path:'b',text:'x'}],[{path:'a',text:'1\n3'},{path:'c',text:'y'}]);
  assert.deepEqual([d.added,d.changed,d.removed,d.plus,d.minus],[['c'],['a'],['b'],2,2]);
});
