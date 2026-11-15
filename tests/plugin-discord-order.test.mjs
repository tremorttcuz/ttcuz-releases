import test from 'node:test';
import assert from 'node:assert/strict';
import {webcrypto} from 'node:crypto';
if(!globalThis.crypto)globalThis.crypto=webcrypto;
import {pluginRoute} from '../server/cloudflare/plugins.mjs';
import {makeCtt,base64} from './helpers/plugin-package.mjs';

// The review message must reach Discord before Gemini answers: work after the reply is cut off ~30 s later,
// and waiting for a slow Gemini used to mean no message at all.
function database(){
  const rows=[];let next=1;
  const statement=sql=>({bind(...args){this.args=args;return this;},
    async run(){
      const r=rows[0];
      if(r&&sql.startsWith('UPDATE native_submissions SET discord='))r.discord=this.args[0];
      if(r&&sql.startsWith('UPDATE native_submissions SET review='))r.review=this.args[0];
      return {};
    },
    async first(){
      if(sql.startsWith('INSERT INTO limits'))return {hits:1};
      if(sql.startsWith('SELECT COUNT(*)'))return {count:0};
      if(sql.startsWith('INSERT INTO native_submissions')){rows.push({id:next++});return {id:next-1};}
      return null;
    },
    async all(){return {results:[]};}});
  return {rows,prepare:statement,async batch(list){for(const s of list)await s.run();}};
}

test('the review message is posted first and Gemini\'s verdict is written into it',async()=>{
  const calls=[],real=globalThis.fetch;
  globalThis.fetch=async(url,init={})=>{
    url=String(url);calls.push((init.method||'GET')+' '+url.replace(/^https:\/\/[^/]+/,''));
    if(url.endsWith('/guilds/123456'))return Response.json({id:'123456'});
    if(url.includes('/guilds/123456/channels'))return Response.json([{id:'c1',type:0,name:'плагины-на-проверку'}]);
    if(url.includes('/channels/c1/messages')&&init.method==='POST')return Response.json({id:'m1'});
    if(url.includes('/messages/m1'))return Response.json({id:'m1'});
    if(url.includes('generativelanguage'))return Response.json({candidates:[{content:{parts:[{text:'{"verdict":"safe","summary":"ok","obfuscated":false}'}]}}]});
    return new Response('',{status:404});
  };
  try{
    const PROFILES=database(),jobs=[];
    const env={PROFILES,DISCORD_BOT_TOKEN:'t',DISCORD_GUILD_ID:'123456',GEMINI_API_KEY:'k'};
    const plugin={metadata:{id:'nick.order',name:'Order',version:'1.0',author:'nick',entry:'nick.order.Main'},
      files:[{path:'java/nick/order/Main.java',text:'package nick.order; public class Main {}'}],
      archive:base64(makeCtt('nick.order','nick.order.Main',['Lnick/order/Main;'])),handle:'nick',uid:'1'};
    const answer=await pluginRoute(new Request('https://mod/plugins/native/submit',{method:'POST',body:JSON.stringify(plugin)}),env,{waitUntil:p=>jobs.push(p)});
    assert.equal(answer.status,202);
    await Promise.all(jobs);
    const post=calls.findIndex(c=>c.startsWith('POST /api/v10/channels/c1/messages')||c.startsWith('POST')&&c.includes('/channels/c1/messages'));
    const gemini=calls.findIndex(c=>c.includes('generateContent'));
    const edit=calls.findIndex(c=>c.startsWith('PATCH')&&c.includes('/messages/m1'));
    assert.ok(post>=0&&gemini>post&&edit>gemini,calls.join('\n'));
    assert.equal(JSON.parse(PROFILES.rows[0].discord).id,'m1');
    assert.equal(JSON.parse(PROFILES.rows[0].review).verdict,'safe');
  }finally{globalThis.fetch=real;}
});
