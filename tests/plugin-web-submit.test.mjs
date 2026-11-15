import test from 'node:test';
import assert from 'node:assert/strict';
import {webcrypto} from 'node:crypto';
if(!globalThis.crypto)globalThis.crypto=webcrypto;
import {pluginRoute} from '../server/cloudflare/plugins.mjs';
import {shield} from '../server/cloudflare/shield.mjs';
import {makeCtt,base64} from './helpers/plugin-package.mjs';

function database(){
  const rows=[];let next=1;
  const statement=sql=>({args:[],bind(...a){this.args=a;return this;},
    async run(){return {};},
    async first(){
      if(sql.startsWith('INSERT INTO limits'))return {hits:1};
      if(sql.startsWith('SELECT uid,username,display'))return {uid:this.args[0],username:'dan.ik',display:'Dan Ik',avatar:0,updated:1};
      if(sql.startsWith('SELECT COUNT(*)'))return {count:0};
      if(sql.startsWith('INSERT INTO native_submissions')){const [plugin_id,metadata,sources,archive,icon,handle,uid,added,ip,banner,reviews_open]=this.args;rows.push({id:next,plugin_id,metadata,sources,archive,icon,handle,uid,added,banner,reviews_open,state:'pending'});return {id:next++};}
      if(sql.startsWith('SELECT sources,icon,banner FROM native_submissions'))return rows.find(r=>r.id===this.args[0])||null;
      return null;
    },
    async all(){if(sql.includes("FROM native_submissions WHERE state='pending'"))return {results:rows.map(r=>({...r,banner:r.banner?r.banner.length:0,packed:r.archive.length}))};return {results:[]};}});
  return {rows,prepare:statement};
}

test('a Google account sends a plugin from the store page; everyone sees it waiting, with its code and banner',async()=>{
  const real=globalThis.fetch;
  globalThis.fetch=async url=>String(url).includes('tokeninfo')?Response.json({aud:'client.apps',email_verified:'true',sub:'1234567890',name:'Dan Ik',exp:String(Math.floor(Date.now()/1000)+600)}):new Response('',{status:404});
  try{
    const PROFILES=database(),env={PROFILES,GOOGLE_CLIENT_ID:'client.apps'};
    const jpeg=btoa(String.fromCharCode(0xFF,0xD8,0xFF,0xE0,...new Array(20).fill(0)));
    const body={archive:base64(makeCtt('dan.web','dan.web.Main',['Ldan/web/Main;'],{},{version:'1.0',author:'dan'})),files:[{path:'java/dan/web/Main.java',text:'package dan.web; public class Main {}'}],banner:jpeg,reviewsOpen:true};
    const send=headers=>pluginRoute(new Request('https://mod/plugins/web/submit',{method:'POST',headers,body:JSON.stringify(body)}),env);
    assert.equal((await send({})).status,401);
    const token='a.b.c',sent=await send({'x-google-token':token});
    assert.equal(sent.status,202,await sent.clone().text());
    assert.equal(PROFILES.rows[0].uid,'g:1234567890');assert.equal(PROFILES.rows[0].handle,'dan.ik');assert.equal(PROFILES.rows[0].reviews_open,1);
    const queue=(await (await pluginRoute(new Request('https://mod/plugins/native/queue'),env)).json()).queue;
    assert.equal(queue[0].plugin.id,'dan.web');assert.equal(queue[0].reviewsOpen,true);assert.ok(queue[0].banner);assert.equal(queue[0].uid,undefined);
    const files=await (await pluginRoute(new Request('https://mod'+'/plugins/native/public/1/sources'),env)).json();
    assert.equal(files.files[0].path,'java/dan/web/Main.java');
    const banner=await pluginRoute(new Request('https://mod'+queue[0].banner),env);
    assert.equal(banner.headers.get('content-type'),'image/jpeg');
    // the shield lets a signed-in page write, and nothing else from a browser
    const at=(p,h)=>shield(new Request('https://mod'+p,{method:'POST',headers:{'user-agent':'Mozilla',...h},body:'{}'}),{});
    assert.equal(await at('/plugins/web/submit',{'x-google-token':token}),null);
    assert.equal(await at('/plugins/reviews/dan.web',{'x-google-token':token}),null);
    assert.equal((await at('/plugins/native/submit',{'x-google-token':token})).status,403);
  }finally{globalThis.fetch=real;}
});

import {usernameProblem} from '../server/cloudflare/accounts.mjs';
test('usernames: latin, 3..24, no system names',()=>{
  for(const ok of ['dan','dan_ik','d.an.ik','user123'])assert.equal(usernameProblem(ok),'',ok);
  for(const bad of ['da','_dan','dan_','d__an','Дан','admin','ttcuz_bot','a'.repeat(25),'dan ik'])assert.notEqual(usernameProblem(bad),'',bad);
});
