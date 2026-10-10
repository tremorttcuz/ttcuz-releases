import test from 'node:test';
import assert from 'node:assert/strict';
import {crashMessage,parseCrash,notifyCrash,configured} from '../server/cloudflare/discord.mjs';

const row={id:'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',version:'0.5.9',sdk:34,occurred_at:1759700000000,
  stack:JSON.stringify({v:1,stack:'java.lang.IllegalStateException: boom\n at com.ss.android.Screen.onDraw(Screen.java:42)\n at cat.narezany.margyt.Heart.tint(Heart.java:9)',
    meta:{crash:{type:'java.lang.IllegalStateException',message:'boom',site:'com.ss.android.Screen.onDraw(Screen.java:42)',fatal:true,thread:'main',androidRelease:'14'},
      device:{manufacturer:'Google',model:'Pixel 8'},memory:{freeMb:900,totalMb:3800,javaHeapMb:120,javaHeapMaxMb:512,storageFreeMb:2048},
      mod:{version:'0.5.9',tiktok:'47.2.41',uptimeSeconds:310},thread:'main',handler:'default'}})};

test('a crash reads as something a person can act on', () => {
  const crash=parseCrash(row);
  assert.equal(crash.stack.split('\n')[0],'java.lang.IllegalStateException: boom');
  assert.equal(crash.meta.device.model,'Pixel 8');
  assert.ok(crash.at.endsWith('UTC'));
  const message=crashMessage(crash);
  for(const wanted of ['Crash in ttcuz 0.5.9','IllegalStateException: boom','Screen.onDraw(Screen.java:42)',
    'Pixel 8','Android:** 14','47.2.41','5 min 10 s','900 of 3800 MB','2048 MB',crash.id]){
    assert.ok(message.includes(wanted),`message is missing ${wanted}`);
  }
  // Our own frame is not where the blame goes when a better one exists.
  assert.ok(!message.includes('**Where:** cat.narezany.margyt'));
  // The whole thing has to fit in one Discord message.
  assert.ok(message.length<=2000,`message is ${message.length} characters`);
});

test('a report from before the envelope still reads', () => {
  const old=parseCrash({id:'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',version:'0.5.8',sdk:33,occurred_at:1759700000000,
    stack:'java.lang.RuntimeException: plain\n at com.ss.android.Screen.onDraw(Screen.java:7)'});
  assert.deepEqual(old.meta,{});
  const message=crashMessage(old);
  assert.ok(message.includes('RuntimeException: plain'));
  assert.ok(message.includes('Screen.onDraw(Screen.java:7)'));
  assert.ok(message.includes('Android:** API 33'));
});

test('the threads that were not crashing are kept, since that is where the lock is', () => {
  const crash=parseCrash({...row,stack:JSON.stringify({v:1,stack:'x\n at a.B(c.D:1)',meta:{
    crash:{type:'x',message:'stuck'},device:{manufacturer:'Google',model:'Pixel 8'},
    memory:{freeMb:12,totalMb:3800,lowMemory:true},mod:{version:'0.5.9'},
    threads:[{name:'RenderThread',state:'BLOCKED',daemon:false,stack:'a.B(c.D:1)'}]}})});
  assert.equal(crash.meta.threads[0].state,'BLOCKED');
  assert.equal(crash.meta.memory.lowMemory,true);
});

test('discord is told once, in the right conversation, with the report in it', async () => {
  const original=globalThis.fetch;
  const calls=[];
  globalThis.fetch=async(url,options)=>{
    calls.push({url:String(url),options});
    return String(url).endsWith('/users/@me/channels')?Response.json({id:'1535779199896256593',type:1}):Response.json({id:'1'});
  };
  try{
    const env={DISCORD_BOT_TOKEN:'token',DISCORD_USER_ID:'964942614732361791'};
    assert.ok(configured(env));
    assert.deepEqual(await notifyCrash(env,row),{sent:true});
    // A person's id is not a channel: the conversation is opened first, with
    // the recipient, and the report goes to the channel that comes back.
    assert.equal(calls.length,2);
    assert.equal(calls[0].url,'https://discord.com/api/v10/users/@me/channels');
    assert.equal(calls[0].options.method,'POST');
    assert.equal(JSON.parse(calls[0].options.body).recipient_id,'964942614732361791');
    assert.equal(calls[1].url,'https://discord.com/api/v10/channels/1535779199896256593/messages');
    assert.equal(calls[1].options.headers.authorization,'Bot token');
    const body=JSON.parse(calls[1].options.body);
    assert.ok(body.content.includes('Crash in ttcuz 0.5.9'));
    // A crash that quotes somebody's @everyone must not actually mention them.
    assert.deepEqual(body.allowed_mentions,{parse:[]});
    // The calls give up rather than holding a phone's upload open.
    assert.ok(calls[0].options.signal&&calls[1].options.signal);
  } finally { globalThis.fetch=original; }
});

test('the conversation is opened once and remembered', async () => {
  const original=globalThis.fetch;
  const calls=[];
  globalThis.fetch=async(url)=>{calls.push(String(url));return String(url).endsWith('/users/@me/channels')?Response.json({id:'777',type:1}):Response.json({id:'1'});};
  try{
    const env={DISCORD_BOT_TOKEN:'token',DISCORD_USER_ID:'111222333444555666'};
    assert.deepEqual(await notifyCrash(env,row),{sent:true});
    assert.deepEqual(await notifyCrash(env,row),{sent:true});
    assert.equal(calls.filter(url=>url.endsWith('/users/@me/channels')).length,1,'opened twice');
    assert.equal(calls.filter(url=>url.endsWith('/messages')).length,2);
  } finally { globalThis.fetch=original; }
});

test('a conversation that was deleted is reopened rather than lost', async () => {
  const original=globalThis.fetch;
  const calls=[];let channels=0;
  globalThis.fetch=async(url)=>{
    calls.push(String(url));
    if(String(url).endsWith('/users/@me/channels')){channels++;return Response.json({id:'channel-'+channels,type:1});}
    // The first channel is gone by the time the message is written.
    return String(url).includes('channel-1')?new Response('{"message":"Unknown Channel","code":10003}',{status:404}):Response.json({id:'1'});
  };
  try{
    const env={DISCORD_BOT_TOKEN:'token',DISCORD_USER_ID:'999888777666555444'};
    assert.deepEqual(await notifyCrash(env,row),{sent:true});
    assert.equal(channels,2,'the channel was not reopened');
    assert.ok(calls[calls.length-1].includes('channel-2'));
  } finally { globalThis.fetch=original; }
});

test('a refusal is reported rather than thrown, and never with the token', async () => {
  const original=globalThis.fetch;
  globalThis.fetch=async()=>new Response('{"message":"401: Unauthorized"}',{status:401});
  try{
    const failed=await notifyCrash({DISCORD_BOT_TOKEN:'secret-token',DISCORD_USER_ID:'555444333222111000'},row);
    assert.equal(failed.sent,false);
    assert.ok(failed.reason.startsWith('dm open: 401'),failed.reason);
    assert.ok(!failed.reason.includes('secret-token'));
  } finally { globalThis.fetch=original; }
});

test('a missing recipient is said so, and nothing is called', async () => {
  const original=globalThis.fetch;
  let called=0;
  globalThis.fetch=async()=>{called++;return Response.json({});};
  try{
    const result=await notifyCrash({DISCORD_BOT_TOKEN:'token'},row);
    assert.equal(result.sent,false);
    assert.ok(result.reason.includes('no recipient'),result.reason);
    assert.equal(called,0);
  } finally { globalThis.fetch=original; }
});

test('no token means no attempt at all', async () => {
  const original=globalThis.fetch;
  let called=0;
  globalThis.fetch=async()=>{called++;return Response.json({});};
  try{
    assert.deepEqual(await notifyCrash({},row),{sent:false,reason:'discord_unconfigured'});
    assert.equal(called,0);
    assert.equal(configured({DISCORD_BOT_TOKEN:''}),false);
  } finally { globalThis.fetch=original; }
});
