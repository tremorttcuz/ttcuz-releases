import assert from 'node:assert/strict';import {webcrypto} from 'node:crypto';if(!globalThis.crypto)globalThis.crypto=webcrypto;
import {validReport,reportPayload,crashRoute} from '../server/cloudflare/crashes.mjs';
const report={id:'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',version:'0.3.5',sdk:35,stack:'java.lang.IllegalStateException\n at mod.Screen(Screen.java:1)',time:Date.now()};
const meta={crash:{type:'java.lang.IllegalStateException',message:'boom',site:'com.ss.android.Screen.onDraw(Screen.java:42)',fatal:true},device:{manufacturer:'Google',model:'Pixel 8'},memory:{freeMb:900,totalMb:3800},mod:{version:'0.3.5',tiktok:'47.2.41'},thread:'main',androidRelease:'14'};

// The shape 0.5.8 sent, and everything before it: a bare stack, no metadata.
assert(validReport(report));
assert.equal(reportPayload(report),report.stack);
// The same report with the envelope a newer client sends.
assert(validReport({...report,meta}));
assert.deepEqual(JSON.parse(reportPayload({...report,meta})).meta,meta);
assert.equal(JSON.parse(reportPayload({...report,meta})).stack,report.stack);

// Bounds: a stack over the limit, an API level that cannot exist, a version
// that is not one, and metadata of the wrong shape or with a field that is
// too long to be one.
assert(!validReport({...report,stack:'a'.repeat(12001)}));
assert(!validReport({...report,sdk:'35'}));
assert(!validReport({...report,sdk:500}));
assert(!validReport({...report,version:'bad'}));
assert(!validReport({...report,meta:'meta'}));
assert(!validReport({...report,meta:{crash:'boom'}}));
assert(!validReport({...report,meta:{crash:{message:'x'.repeat(200)}}}));
assert(!validReport({...report,meta:{thread:'t'.repeat(200)}}));
assert(validReport({...report,meta:{memory:{freeMb:900},crash:{fatal:true}}}));
assert(!validReport({...report,meta:{memory:{freeMb:'lots'}}}));
// A corrected build says 0.6.5.1, and a report from it is not a bad report.
assert(validReport({...report,version:'0.6.5.1'}));
assert(validReport({...report,version:'0.6.5'}));
assert(!validReport({...report,version:'0.6.5.1.2'}));
assert(!validReport({...report,version:'0.6'}));
assert(!validReport({...report,version:'v0.6.5'}));

let stored=[],hits=1;const env={CRASH_ADMIN_TOKEN:'private',PROFILES:{prepare(sql){return{bind(...values){this.values=values;return this;},async first(){return{hits};},async run(){if(sql.startsWith('INSERT OR IGNORE INTO crash_reports'))stored.push(this.values);},async all(){return{results:stored.map(values=>({id:values[0],version:values[1],sdk:values[2],stack:values[3],occurred_at:values[4],received_at:values[5]}))};}};}}};
const post=body=>new Request('https://mod/v1/crashes',{method:'POST',body:JSON.stringify(body)});
// No bot token yet: the report is taken all the same, and the answer says why
// nobody was told, so an unconfigured server is visible rather than silent.
const accepted=await crashRoute(post(report),env);assert.equal(accepted.status,202);assert.equal((await accepted.json()).notice,'discord_unconfigured');
assert.equal(stored.length,1);assert.equal(stored[0].length,6);
assert.equal(stored[0][3],report.stack);
// With an envelope, what is stored is the envelope rather than the stack.
stored=[];hits=1;
await crashRoute(post({...report,meta}),env);
assert.equal(JSON.parse(stored[0][3]).meta.device.model,'Pixel 8');
assert.equal((await crashRoute(post({...report,stack:''}),env)).status,400);
assert.equal((await crashRoute(post({...report,stack:'x'.repeat(20000)}),env)).status,413);
hits=11;assert.equal((await crashRoute(post(report),env)).status,429);
assert.equal((await crashRoute(new Request('https://mod/v1/admin/crashes'),env)).status,403);
assert.equal((await crashRoute(new Request('https://mod/v1/admin/crashes',{headers:{authorization:'Bearer private'}}),env)).status,200);
// Re-sending one Discord dropped, by id, out of the table.
hits=1;stored=[];
await crashRoute(post(report),env);
const again=await crashRoute(new Request('https://mod/v1/admin/crashes?notify='+report.id,{headers:{authorization:'Bearer private'}}),env);
assert.equal(again.status,502);
assert.equal((await again.json()).reason,'discord_unconfigured');
assert.equal((await crashRoute(new Request('https://mod/v1/admin/crashes?notify=nope',{headers:{authorization:'Bearer private'}}),env)).status,404);
assert.equal(await crashRoute(new Request('https://mod/other'),env),null);
console.log('Crash report validation, envelope, size limit, quota, admin access, re-send: OK');
