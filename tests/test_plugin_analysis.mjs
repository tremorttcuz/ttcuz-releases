import test from 'node:test';
import assert from 'node:assert/strict';
import {reviewSource,analyzeSource} from '../server/cloudflare/plugin-analysis.mjs';
import {submission,nativePluginRoute} from '../server/cloudflare/native-plugins.mjs';
import {projectZip} from '../server/cloudflare/public/plugin-project.mjs';
import {pluginRoute} from '../server/cloudflare/plugins.mjs';
import {digest} from '../server/cloudflare/profiles.mjs';
const files=[{path:'java/ttcuz/Example.java',text:'package ttcuz; public class Example { public int number() { return 42; } }'}];
const value={metadata:{id:'ttcuz.example',name:'Example',version:'1.0',author:'Tremor',entry:'ttcuz.Example'},files};
test('publication accepts readable sources and rejects concealed code and duplicate paths',()=>{
 assert.equal(submission(value).metadata.api,1);
 for(const text of ['class a {}','class Example { String payload="'+'a'.repeat(300)+'"; }','class Example { DexClassLoader loader; }','class Example { String name="\\u0041"; }'])assert.throws(()=>reviewSource({files:[{...files[0],text}]}));
 assert.throws(()=>reviewSource({files:[...files,...files]}));
 assert.throws(()=>reviewSource({files:[{...files[0],path:'../Example.java'}]}));
});
test('Google key goes only to the official endpoint and code is untrusted input',async()=>{
 const original=globalThis.fetch;let sent;
 globalThis.fetch=async(url,options)=>{sent={url,options};return Response.json({candidates:[{content:{parts:[{text:JSON.stringify({risk:'medium',findings:['Example.java: network access'],summary:'Проверьте сеть'})}]}}]});};
 try{const result=await analyzeSource({GEMINI_API_KEY:'server-secret'},files);assert.equal(result.status,'complete');assert.match(sent.url,/^https:\/\/generativelanguage.googleapis.com\//);assert.equal(sent.options.headers['x-goog-api-key'],'server-secret');assert.ok(!sent.options.body.includes('server-secret'));assert.match(JSON.parse(sent.options.body).systemInstruction.parts[0].text,/недоверенными/);
 globalThis.fetch=async()=>{throw Error('server-secret');};assert.ok(!JSON.stringify(await analyzeSource({GEMINI_API_KEY:'server-secret'},files)).includes('server-secret'));}finally{globalThis.fetch=original;}
});
test('Google failures never become a low-risk result',async()=>{
 assert.equal((await analyzeSource({},files)).status,'unavailable');
 const original=globalThis.fetch;globalThis.fetch=async()=>new Response('provider error',{status:401});
 try{const result=await analyzeSource({GEMINI_API_KEY:'secret'},files);assert.equal(result.reason,'google_http_401');assert.equal(result.risk,undefined);}finally{globalThis.fetch=original;}
});
test('owner routes cannot be used with an unverified identity',async()=>{
 const response=await nativePluginRoute(new Request('https://ttcuz/plugins/native/review',{method:'POST'}),{PROFILES:{}},async()=>({digest:'a'.repeat(64),action:'approve'}),async()=>false);
 assert.equal(response.status,403);
});
test('source project is a ZIP with manifest and readable Java, not a disguised executable',async()=>{
 const bytes=new Uint8Array(await projectZip(value).arrayBuffer());assert.deepEqual([...bytes.slice(0,4)],[80,75,3,4]);const text=new TextDecoder().decode(bytes);assert.match(text,/manifest.json/);assert.match(text,/java\/ttcuz\/Example.java/);assert.ok(!text.includes('classes.dex'));
});

test('a temporary owner session expires and cannot create another session',async()=>{
 const deviceToken='a'.repeat(64), deviceHash=await digest(deviceToken);let sessionHash, until;
 const env={PROFILES:{prepare(sql){return{args:[],bind(...args){this.args=args;return this;},async first(){if(sql.includes('owner_hash'))return{owner_hash:deviceHash,verified:1};if(sql.includes('SELECT until'))return this.args[0]===sessionHash?{until}:null;return null;},async run(){if(sql.startsWith('INSERT INTO limits'))[sessionHash,,until]=[this.args[0],0,this.args[1]];},async all(){return{results:[]};}};}}};
 const request=(path,body)=>new Request('https://ttcuz'+path,{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify(body)});
 const made=await pluginRoute(request('/plugins/session',{uid:'7491898648855512119',token:deviceToken}),env);
 assert.equal(made.status,200);const {session}=await made.json();assert.match(session,/^[a-f0-9]{64}$/);
 assert.equal((await pluginRoute(request('/plugins/native/list',{session}),env)).status,200);
 assert.equal((await pluginRoute(request('/plugins/session',{session}),env)).status,403);
 until=Date.now()-1;assert.equal((await pluginRoute(request('/plugins/native/list',{session}),env)).status,403);
});

test('Discord outage leaves a readable submission pending in storage',async()=>{
 let stored;
 const env={PROFILES:{prepare(sql){return{args:[],bind(...args){this.args=args;return this;},async first(){if(sql.startsWith('INSERT INTO limits'))return{hits:1};if(sql.startsWith('SELECT COUNT'))return{count:0};return null;},async run(){if(sql.startsWith('INSERT OR IGNORE INTO native_plugin_submissions'))stored=this.args;}};}}};
 const response=await pluginRoute(new Request('https://ttcuz/plugins/native/submit',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify(value)}),env);
 assert.equal(response.status,202);assert.equal(stored[1],value.metadata.id);assert.deepEqual(JSON.parse(stored[3]),files);
});
