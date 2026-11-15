import test from 'node:test';
import assert from 'node:assert/strict';
import {webcrypto} from 'node:crypto';
if(!globalThis.crypto)globalThis.crypto=webcrypto;
import {pluginRoute,catalogChanged} from '../server/cloudflare/plugins.mjs';
import {forgetRatings} from '../server/cloudflare/reviews.mjs';
import {digest} from '../server/cloudflare/profiles.mjs';

// D1 stand-in: profiles, the store row, limits and the reviews table.
function database(profiles){
  const reviews=new Map();
  const statement=sql=>({args:[],bind(...a){this.args=a;return this;},
    async run(){
      const a=this.args;
      if(sql.startsWith('INSERT INTO plugin_reviews')){const k=a[0]+'|'+a[1],old=reviews.get(k);reviews.set(k,{plugin_id:a[0],uid:a[1],stars:a[2],text:a[3],handle:a[4],added:old?.added??a[5],updated:a[6]});}
      if(sql.startsWith('DELETE FROM plugin_reviews WHERE plugin_id=? AND uid=?'))reviews.delete(a[0]+'|'+a[1]);
      return {};
    },
    async first(){
      const a=this.args;
      if(sql.startsWith('SELECT owner_hash FROM profiles'))return profiles[a[0]]?{owner_hash:profiles[a[0]]}:null;
      if(sql.startsWith('SELECT metadata FROM plugin_packages'))return a[0]==='nick.up'?{metadata:JSON.stringify({id:'nick.up',name:'Up'})}:null;
      if(sql.startsWith('INSERT INTO limits'))return {hits:1};
      if(sql.startsWith('SELECT stars FROM plugin_reviews'))return reviews.get(a[0]+'|'+a[1])||null;
      return null;
    },
    async all(){
      if(sql.includes('FROM plugin_reviews')&&sql.includes('plugin_id=?')&&!sql.startsWith('SELECT uid FROM'))return {results:[...reviews.values()].filter(r=>r.plugin_id===this.args[0])};
      if(sql.includes('GROUP BY plugin_id')&&!sql.includes('plugin_likes')&&!sql.includes('plugin_reactions')){const by={};for(const r of reviews.values())(by[r.plugin_id]??=[]).push(r.stars);return {results:Object.entries(by).map(([plugin_id,l])=>({plugin_id,average:l.reduce((x,y)=>x+y)/l.length,count:l.length}))};}
      if(sql.includes('FROM plugin_packages'))return {results:[{metadata:JSON.stringify({id:'nick.up',name:'Up'})}]};
      return {results:[]};
    }});
  return {reviews,prepare:statement,async batch(l){for(const s of l)await s.run();}};
}
const call=(env,path,body)=>pluginRoute(new Request('https://mod'+path,body?{method:'POST',body:JSON.stringify(body)}:{}),env);

test('reviews: an account rates once, changes it, takes it away; the catalog shows the stars',async()=>{
  const token='a'.repeat(64),other='b'.repeat(64);
  const PROFILES=database({'1':await digest(token),'2':await digest(other)});
  const env={PROFILES,ADMIN_PIN:'test-pin-only-in-tests',ASSETS:{async fetch(){return Response.json({plugins:[]});}}};
  assert.equal((await call(env,'/plugins/reviews/nick.up',{uid:'1',token:other,stars:5})).status,403,'someone else\'s token');
  assert.equal((await call(env,'/plugins/reviews/nick.gone',{uid:'1',token,stars:5})).status,404);
  assert.equal((await call(env,'/plugins/reviews/nick.up',{uid:'1',token,stars:9})).status,400);
  let r=await (await call(env,'/plugins/reviews/nick.up',{uid:'1',token,stars:5,text:'Класс',handle:'@nick'})).json();
  assert.equal(r.count,1);assert.deepEqual(r.mine,{stars:5,text:'Класс'});
  r=await (await call(env,'/plugins/reviews/nick.up',{uid:'1',token,stars:3,text:'Норм'})).json();
  assert.equal(r.count,1,'a second send changes the same review');assert.equal(r.average,3);
  await call(env,'/plugins/reviews/nick.up',{uid:'2',token:other,stars:4});
  const read=await (await call(env,'/plugins/reviews/nick.up')).json();
  assert.equal(read.count,2);assert.equal(read.average,3.5);assert.deepEqual(read.histogram,[0,0,1,1,0]);
  assert.ok(read.reviews.every(x=>!('uid' in x)),'no uid in public answers');
  catalogChanged();forgetRatings();
  const catalog=await (await call(env,'/plugins/catalog.json')).json();
  assert.deepEqual(catalog.plugins[0].rating,{average:3.5,count:2});
  r=await (await call(env,'/plugins/reviews/nick.up',{uid:'1',token,stars:0})).json();
  assert.equal(r.count,1);assert.equal(r.mine,undefined);
  // the admin removes the other one by its ref
  const env2={...env,PROFILES:{...PROFILES,prepare:sql=>{const st=PROFILES.prepare(sql);if(sql.startsWith('SELECT uid FROM plugin_reviews'))st.all=async()=>({results:[...PROFILES.reviews.values()].map(x=>({uid:x.uid}))});return st;}}};
  const ref=read.reviews.find(x=>x.stars===4).ref;assert.match(ref,/^[a-f0-9]{16}$/);
  const removed=await pluginRoute(new Request('https://mod/plugins/reviews/nick.up/remove',{method:'POST',headers:{'x-admin-pin':'test-pin-only-in-tests'},body:JSON.stringify({ref})}),env2);
  assert.equal(removed.status,200);assert.equal(PROFILES.reviews.size,0);
});

test('votes and replies: one vote per account, not on one\'s own; replies under a review, deleted by their author',async()=>{
  const token='a'.repeat(64),other='b'.repeat(64);
  const reviews=new Map([['nick.up|1',{plugin_id:'nick.up',uid:'1',stars:5,text:'',handle:'a',added:1,updated:1}]]);
  const votes=new Map(),replies=[];let next=1;
  const profiles={'1':await digest(token),'2':await digest(other)};
  const prepare=sql=>({args:[],bind(...a){this.args=a;return this;},
    async run(){const a=this.args;
      if(sql.startsWith('INSERT INTO review_votes'))votes.set(a[1]+'|'+a[2],a[3]);
      if(sql.startsWith('DELETE FROM review_votes WHERE plugin_id=? AND review_uid=? AND voter=?'))votes.delete(a[1]+'|'+a[2]);
      if(sql.startsWith('DELETE FROM review_replies WHERE id=?'))replies.splice(replies.findIndex(r=>r.id===a[0]),1);
      return {};},
    async first(){const a=this.args;
      if(sql.startsWith('SELECT owner_hash FROM profiles'))return profiles[a[0]]?{owner_hash:profiles[a[0]]}:null;
      if(sql.startsWith('INSERT INTO limits'))return {hits:1};
      if(sql.startsWith('SELECT COUNT(*) AS n FROM review_replies'))return {n:replies.length};
      if(sql.startsWith('INSERT INTO review_replies')){const r={id:next++,plugin_id:a[0],review_uid:a[1],uid:a[2],text:a[3],handle:a[4],added:a[5]};replies.push(r);return {id:r.id};}
      if(sql.startsWith('SELECT uid FROM review_replies'))return replies.find(r=>r.id===a[0])||null;
      return null;},
    async all(){
      if(sql.startsWith('SELECT uid FROM plugin_reviews'))return {results:[...reviews.values()].map(r=>({uid:r.uid}))};
      if(sql.includes('FROM plugin_reviews'))return {results:[...reviews.values()]};
      if(sql.startsWith('SELECT review_uid,SUM'))return {results:[{review_uid:'1',up:[...votes.values()].filter(v=>v===1).length,dn:[...votes.values()].filter(v=>v===-1).length}]};
      if(sql.includes('FROM review_replies r LEFT JOIN'))return {results:replies};
      return {results:[]};}});
  const env={PROFILES:{prepare}};
  const call=(path,body)=>pluginRoute(new Request('https://mod'+path,body?{method:'POST',body:JSON.stringify(body)}:{}),env);
  const ref=(await (await call('/plugins/reviews/nick.up')).json()).reviews[0].ref;
  assert.equal((await call('/plugins/reviews/nick.up/vote',{uid:'1',token,ref,value:1})).status,400,'not on one\'s own review');
  assert.equal((await call('/plugins/reviews/nick.up/vote',{uid:'2',token:other,ref,value:1})).status,200);
  assert.equal((await call('/plugins/reviews/nick.up/vote',{uid:'2',token:other,ref,value:5})).status,400);
  const sent=await (await call('/plugins/reviews/nick.up/reply',{uid:'2',token:other,ref,text:'  Спасибо!  '})).json();
  assert.equal(sent.reply.text,'Спасибо!');
  const read=await (await call('/plugins/reviews/nick.up')).json();
  assert.equal(read.reviews[0].up,1);assert.equal(read.reviews[0].replies[0].text,'Спасибо!');
  assert.equal((await call('/plugins/reviews/nick.up/replies/'+sent.reply.id+'/delete',{uid:'1',token})).status,403,'only its author');
  assert.equal((await call('/plugins/reviews/nick.up/replies/'+sent.reply.id+'/delete',{uid:'2',token:other})).status,200);
  assert.equal(replies.length,0);
});

test('plugin likes: one per account, counted in the answer and in the catalog',async()=>{
  forgetRatings();catalogChanged();
  const token='c'.repeat(64);
  const likes=new Map();
  const base=database({'7':await digest(token)});
  const PROFILES={prepare(sql){const st=base.prepare(sql);const run=st.run,first=st.first,all=st.all;
    st.run=async function(){const a=this.args;if(sql.startsWith('INSERT INTO plugin_likes'))likes.set(a[0]+'|'+a[1],a[2]);if(sql.startsWith('DELETE FROM plugin_likes'))likes.delete(a[0]+'|'+a[1]);return run.call(this);};
    st.first=async function(){if(sql.startsWith('SELECT 1 AS ok FROM plugin_packages'))return this.args[0]==='nick.up'?{ok:1}:null;
      if(sql.startsWith('SELECT SUM(value=1)')){const v=[...likes.values()];return {up:v.filter(x=>x===1).length,dn:v.filter(x=>x===-1).length};}return first.call(this);};
    st.all=async function(){if(sql.includes('FROM plugin_likes')){const v=[...likes.values()];return {results:v.length?[{plugin_id:'nick.up',up:v.filter(x=>x===1).length,dn:v.filter(x=>x===-1).length}]:[]};}return all.call(this);};
    return st;}};
  const env={PROFILES,ADMIN_PIN:'test-pin-only-in-tests',ASSETS:{async fetch(){return Response.json({plugins:[]});}}};
  let answer=await (await call(env,'/plugins/reviews/nick.up/like',{uid:'7',token,value:1})).json();
  assert.deepEqual([answer.ok,answer.up,answer.dn],[true,1,0]);
  answer=await (await call(env,'/plugins/reviews/nick.up/like',{uid:'7',token,value:-1})).json();
  assert.deepEqual([answer.up,answer.dn],[0,1]);
  const catalog=await (await call(env,'/plugins/catalog.json')).json();
  assert.equal(catalog.plugins[0].rating.dn,1);
  answer=await (await call(env,'/plugins/reviews/gone.one/like',{uid:'7',token,value:1})).json();
  assert.equal(answer.error,'not_found');
  answer=await (await call(env,'/plugins/reviews/nick.up/like',{uid:'7',token:'d'.repeat(64),value:1})).json();
  assert.equal(answer.error,'account_required');
});
