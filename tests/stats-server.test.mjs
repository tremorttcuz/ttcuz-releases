import test from 'node:test';
import assert from 'node:assert/strict';
import {webcrypto} from 'node:crypto';
if(!globalThis.crypto)globalThis.crypto=webcrypto;
import {statsRoute,notePresence,noteDownload,downloadCounts,cleanPresence,noteVote,pluginHistory,HISTORY_DAYS} from '../server/cloudflare/stats.mjs';

// a little D1: presence and plugin_downloads kept in maps, the few statements stats.mjs sends
function database(){
  const presence=new Map(),downloads=new Map(),daily=new Map();
  const prepare=sql=>({sql,args:[],bind(...args){this.args=args;return this;},
    async run(){
      if(sql.startsWith('INSERT INTO presence'))presence.set(this.args[0],this.args[1]);
      else if(sql.startsWith('INSERT INTO plugin_downloads'))downloads.set(this.args[0],(downloads.get(this.args[0])||0)+1);
      else if(sql.startsWith('DELETE FROM presence')){for(const [k,v] of presence)if(v<this.args[0])presence.delete(k);}
      else if(sql.startsWith('INSERT INTO plugin_daily')){
        const key=this.args[0]+'|'+this.args[1],row=daily.get(key)||{id:this.args[0],day:this.args[1],downloads:0,likes:0,dislikes:0};
        const column=sql.match(/plugin_daily\(id,day,(\w+)\)/)[1];row[column]++;daily.set(key,row);}
      else if(sql.startsWith('DELETE FROM plugin_daily'))for(const [k,v] of daily)if(v.day<this.args[0])daily.delete(k);
      return {};},
    async all(){
      if(sql.startsWith('SELECT id,count'))return{results:[...downloads].map(([id,count])=>({id,count}))};
      if(sql.startsWith('SELECT SUM(seen'))return{results:[{now:[...presence.values()].filter(v=>v>this.args[0]).length,day:[...presence.values()].filter(v=>v>this.args[1]).length,week:[...presence.values()].filter(v=>v>this.args[2]).length}]};
      if(sql.startsWith('SELECT COALESCE'))return{results:[{downloads:[...downloads.values()].reduce((a,b)=>a+b,0)}]};
      if(sql.startsWith('SELECT day,downloads'))return{results:[...daily.values()].filter(r=>r.id===this.args[0]&&r.day>=this.args[1]).sort((a,b)=>a.day-b.day)};
      return{results:[]};}});
  // like D1: a batch runs its writes and returns what its reads find
  return {presence,downloads,daily,prepare,async batch(list){return Promise.all(list.map(q=>q.sql.startsWith('SELECT')?q.all():q.run().then(()=>({results:[]}))));}};
}
const waiting=[];const ctx={waitUntil(p){waiting.push(p);}};
const settle=()=>Promise.all(waiting.splice(0));

test('presence counts each phone once, keeps only a hash, and ignores what is not an id',async()=>{
  const db=database(),env={PROFILES:db};
  const here=id=>new Request('https://mod/badges',{headers:{'x-ttcuz-here':id}});
  notePresence(here('a'.repeat(32)),env,ctx);notePresence(here('a'.repeat(32)),env,ctx);
  notePresence(here('b'.repeat(32)),env,ctx);notePresence(here('not-an-id'),env,ctx);
  notePresence(new Request('https://mod/badges'),env,ctx);
  await settle();
  assert.equal(db.presence.size,2);
  assert.ok(![...db.presence.keys()].includes('a'.repeat(32)));
  const stats=await (await statsRoute(new Request('https://mod/v1/stats'),env)).json();
  assert.deepEqual(stats,{now:2,day:2,week:2,downloads:0});
});

test('downloads are counted per plugin and summed in the stats; old presence is cleaned',async()=>{
  const db=database(),env={PROFILES:db};
  noteDownload('x.one',env,ctx);noteDownload('x.one',env,ctx);noteDownload('x.two',env,ctx);await settle();
  assert.deepEqual(await downloadCounts(env),{'x.one':2,'x.two':1});
  db.presence.set('old',Date.now()-9*24*3600*1000);db.presence.set('new',Date.now());
  await cleanPresence(env);
  assert.deepEqual([...db.presence.keys()],['new']);
  const stats=await (await statsRoute(new Request('https://mod/v1/stats'),env)).json();
  assert.equal(stats.downloads,3);assert.equal(stats.now,1);
  assert.equal(await statsRoute(new Request('https://mod/v1/other'),env),null);
});

test('each plugin has a day-by-day history: downloads, and likes and dislikes as they are given',async()=>{
  const db=database(),env={PROFILES:db};
  noteDownload('x.one',env,ctx);noteDownload('x.one',env,ctx);noteDownload('x.two',env,ctx);await settle();
  await noteVote(env,'x.one',0,1);await noteVote(env,'x.one',0,-1);
  // changing nothing, or taking a vote back, counts nothing
  await noteVote(env,'x.one',1,1);await noteVote(env,'x.one',1,0);
  const days=await pluginHistory(env,'x.one');
  assert.equal(days.length,HISTORY_DAYS);
  const last=days[days.length-1];
  assert.deepEqual({downloads:last.downloads,likes:last.likes,dislikes:last.dislikes},{downloads:2,likes:1,dislikes:1});
  assert.ok(days.slice(0,-1).every(d=>d.downloads===0&&d.likes===0&&d.dislikes===0));
  assert.equal(days[1].day-days[0].day,1);
  assert.equal((await pluginHistory(env,'x.two')).at(-1).downloads,1);
});
