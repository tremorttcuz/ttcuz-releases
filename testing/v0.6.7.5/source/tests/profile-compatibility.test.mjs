import test from 'node:test';
import assert from 'node:assert/strict';
import {digest,normalizeStyle,publicProfileStyle,profileRoute} from '../server/cloudflare/profiles.mjs';
const uid='12345678901234567890',token='a'.repeat(64),png='iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+j4ZkAAAAASUVORK5CYII=';
const old={firstColour:-2532037,lastColour:-14222892,middleColour:-13891028,direction:1,speed:65,offsetX:3,offsetY:4,badgeSize:125,threeColours:true,bold:true,glow:true,animated:true};
const modern={...old,displayName:'Имя 🌸',gradientWidth:175,gradientStop:25,profileLayout:2,musicOn:true,musicFx:120,musicFy:740,musicAccent:-2532037,musicTitle:'Трек',musicArtist:'Автор',musicLook:{music_slider:1,music_glow:true}};
async function database(style=modern){
 const row={owner_hash:await digest(token),verified:1,metadata:JSON.stringify(style),badge:png,updated_at:1700000000000,music:'ttcuz-music 2\n'};
 return {row,prepare(sql){let args=[];return{bind(...v){args=v;return this;},async first(){if(sql.includes('INSERT INTO limits'))return{hits:1};if(args[0]!==uid)return null;if(sql.includes('AND owner_hash=?')&&args[1]!==row.owner_hash)return null;return row;},async run(){if(sql.startsWith('UPDATE profiles SET metadata=?,badge=')){if(args[3]!==uid||args[4]!==row.owner_hash)return{meta:{changes:0}};row.metadata=args[0];row.badge=args[1];row.updated_at=args[2];}if(sql.startsWith('UPDATE profiles SET metadata=NULL')){row.metadata=null;row.badge=null;}return{meta:{changes:1}};}};}};
}
const request=(method,path,body)=>new Request('https://ttcuz.daniilsolovatulin.workers.dev'+path,{method,...(body?{body:JSON.stringify(body)}:{})});
test('legacy publication keeps extensions it cannot edit while updating real basic gradient',async()=>{
 const db=await database();const result=await profileRoute(request('POST','/v1/publish',{uid,token,style:{...old,firstColour:-1},badge:png}),{PROFILES:db});assert.equal(result.status,200);
 const saved=JSON.parse(db.row.metadata);assert.equal(saved.firstColour,-1);for(const key of ['displayName','gradientWidth','gradientStop','profileLayout','musicOn','musicFx','musicFy','musicLook'])assert.deepEqual(saved[key],modern[key]);assert.equal(db.row.badge,png);
});
test('modern explicit clears still remove extensions and cannot be restored by compatibility',()=>{
 const previous=normalizeStyle(modern,uid,1),reset={...old,displayName:'',gradientWidth:100,gradientStop:50,profileLayout:0,musicOn:false,musicFx:0,musicFy:0,musicLook:{},musicTitle:'',musicArtist:''};
 const saved=normalizeStyle(reset,uid,2,previous);for(const key of Object.keys(reset))assert.deepEqual(saved[key],reset[key]);
});
test('stable public profile fills legacy defaults and exposes no private database fields',async()=>{
 const db=await database({firstColour:old.firstColour,lastColour:old.lastColour,owner_hash:'private',token:'private',profileId:'999'});
 const response=await profileRoute(request('GET','/v1/profiles/'+uid),{PROFILES:db}),body=await response.json();assert.equal(response.status,200);assert.equal(body.profileId,uid);assert.equal(body.firstColour,old.firstColour);assert.equal(body.middleColour,0);assert.equal(body.speed,50);assert.equal(body.badgeSize,100);assert.equal(body.threeColours,false);assert.equal(body.updatedAt,1700000000000);assert.match(body.badgeUrl,/\/v1\/profiles\/12345678901234567890\/badge\?v=/);assert.equal('token' in body,false);assert.equal('owner_hash' in body,false);assert(Buffer.byteLength(JSON.stringify(body))<16384);
});
test('unknown or broken extensions leave a valid legacy gradient and badge available',async()=>{
 const db=await database({...modern,musicLook:{future_slider:99}}),response=await profileRoute(request('GET','/v1/profiles/'+uid),{PROFILES:db}),body=await response.json();assert.equal(body.firstColour,old.firstColour);assert.equal(body.lastColour,old.lastColour);assert.equal(body.threeColours,true);assert.equal(body.bold,true);assert(body.badgeUrl);assert.equal(body.musicOn,false);
 assert.throws(()=>publicProfileStyle({...old,firstColour:'invalid'},uid,1));
});
test('PNG GET and HEAD support older image clients and respect hidden profiles',async()=>{
 const db=await database();const env={PROFILES:db},path='/v1/profiles/'+uid+'/badge';
 const get=await profileRoute(request('GET',path),env);assert.equal(get.headers.get('content-type'),'image/png');assert.deepEqual(Buffer.from(await get.arrayBuffer()),Buffer.from(png,'base64'));
 const head=await profileRoute(request('HEAD',path),env);assert.equal(head.status,200);assert.equal(head.headers.get('content-type'),'image/png');assert.equal((await head.arrayBuffer()).byteLength,0);
 const metadataHead=await profileRoute(request('HEAD','/v1/profiles/'+uid),env);assert.equal(metadataHead.status,200);assert.equal((await metadataHead.arrayBuffer()).byteLength,0);
 db.row.metadata=null;assert.equal((await profileRoute(request('GET',path),env)).status,404);assert.equal((await profileRoute(request('GET','/v1/profiles/'+uid),env)).status,404);
});
test('compatibility merge cannot bypass ownership or badge verification',async()=>{
 const db=await database(),before=db.row.metadata;
 assert.equal((await profileRoute(request('POST','/v1/publish',{uid,token:'b'.repeat(64),style:old,badge:png}),{PROFILES:db})).status,403);assert.equal(db.row.metadata,before);
 db.row.verified=0;const reply=await profileRoute(request('POST','/v1/publish',{uid,token,style:old,badge:png}),{PROFILES:db});assert.equal((await reply.json()).badgeHeld,true);assert.equal(db.row.badge,null);assert.equal(JSON.parse(db.row.metadata).firstColour,old.firstColour);
});
