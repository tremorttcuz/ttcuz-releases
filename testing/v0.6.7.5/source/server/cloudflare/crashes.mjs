import {digest} from './profiles.mjs';
import {notifyCrash} from './discord.mjs';
const reply=(body,status=200)=>Response.json(body,{status,headers:{'cache-control':'no-store'}});

/** The longest stack a client may send; the Java side keeps to the same one. */
const STACK_LIMIT=12000;
/** Room for the envelope around a stack: device, thread, memory, versions. */
const META_LIMIT=12000;
/** The longest free-text field inside the metadata, such as a device model. */
const FIELD_LIMIT=160;

/**
 * The fields each group may carry, and what kind of thing each one is.
 *
 * A group is a fixed shape rather than a bag of anything: numbers that are
 * meant to be megabytes and flags that are meant to be booleans are checked
 * as such, so a sender that gets one wrong is told rather than believed.
 * Nothing here is required -- a report that carries a device and no memory is
 * still a report.
 */
const GROUPS = {
 crash: { type: 'string', message: 'string', site: 'string', thread: 'string', threadId: 'number', threadState: 'string', androidRelease: 'string', fatal: 'boolean' },
 device: { manufacturer: 'string', brand: 'string', model: 'string', device: 'string', hardware: 'string', product: 'string', board: 'string', fingerprint: 'string', abis: 'string' },
 memory: { javaHeapMb: 'number', javaHeapMaxMb: 'number', nativeHeapMb: 'number', freeMb: 'number', totalMb: 'number', thresholdMb: 'number', storageFreeMb: 'number', uptimeSeconds: 'number', lowMemory: 'boolean' },
 mod: { version: 'string', tiktok: 'string' },
};

/**
 * Whether the metadata a client volunteered is usable.
 *
 * Metadata is what turns "something threw" into something that can be fixed --
 * the device, the thread, how much memory was left -- so it is checked rather
 * than trusted. It is optional: 0.5.8 and everything before it sends none at
 * all, and those reports are still worth keeping.
 */
function validMeta(meta){
 if(meta==null)return true;
 if(typeof meta!=='object'||Array.isArray(meta))return false;
 for(const [key,kind] of Object.entries({thread:'string',handler:'string'})){
  const field=meta[key];
  if(field==null)continue;
  if(typeof field!==kind||field.length>FIELD_LIMIT)return false;
 }
 for(const [group,fields] of Object.entries(GROUPS)){
  const values=meta[group];
  if(values==null)continue;
  if(typeof values!=='object'||Array.isArray(values))return false;
  for(const [key,value] of Object.entries(values)){
   const kind=fields[key];
   if(kind==null||value==null)continue;
   if(kind==='number'){if(!Number.isFinite(value))return false;continue;}
   if(typeof value!==kind)return false;
   if(kind==='string'&&value.length>FIELD_LIMIT)return false;
  }
 }
 // The stacks of the threads that were not crashing, which is where a deadlock
 // or a stuck pool shows up first.
 const threads=meta.threads;
 if(threads!=null){
  if(!Array.isArray(threads)||threads.length>64)return false;
  for(const thread of threads){
   if(!thread||typeof thread!=='object'||Array.isArray(thread))return false;
   if(thread.name!=null&&(typeof thread.name!=='string'||thread.name.length>FIELD_LIMIT))return false;
   if(thread.state!=null&&(typeof thread.state!=='string'||thread.state.length>FIELD_LIMIT))return false;
   if(thread.daemon!=null&&typeof thread.daemon!=='boolean')return false;
   if(thread.stack!=null&&(typeof thread.stack!=='string'||thread.stack.length>FIELD_LIMIT))return false;
  }
 }
 return true;
}

/**
 * A version a build could call itself: three parts, or four with a correction.
 *
 * Four is not decoration -- 0.6.5.1 is a fix on top of 0.6.5, and the mod's own
 * update check and the release feed both read versions that way -- so a report
 * from a corrected build must not be refused for saying so.
 */
const VERSION=/^\d+\.\d+\.\d+(\.\d+)?$/;

export function validReport(body){return Boolean(body)&&typeof body==='object'&&/^[a-f0-9-]{36}$/.test(body.id||'')&&VERSION.test(body.version||'')&&Number.isInteger(body.sdk)&&body.sdk>=23&&body.sdk<=100&&typeof body.stack==='string'&&body.stack.length>0&&body.stack.length<=STACK_LIMIT&&Number.isFinite(body.time)&&validMeta(body.meta);}

/**
 * What goes in the stack column.
 *
 * A report with nothing but a stack is stored as that stack, exactly as 0.5.8
 * stored it. A report that carries metadata stores the stack wrapped in an
 * envelope with it: the table keeps what a phone sent, and the shape of the
 * envelope stays the client's to change, which is one less migration every
 * time a useful field is thought of.
 */
export function reportPayload(body){
 if(body.meta==null)return body.stack;
 const saved=JSON.stringify({v:1,stack:body.stack,meta:body.meta});
 return saved.length>STACK_LIMIT+META_LIMIT?null:saved;
}

export async function crashRoute(request,env){
 const path=new URL(request.url).pathname;if(!['/v1/crashes','/v1/admin/crashes'].includes(path))return null;
 if(!env.PROFILES)return reply({error:'storage_unavailable'},503);
 if(path==='/v1/admin/crashes'){
  if(request.method!=='GET')return reply({error:'method'},405);
  if(!env.CRASH_ADMIN_TOKEN||request.headers.get('authorization')!=='Bearer '+env.CRASH_ADMIN_TOKEN)return reply({error:'admin_required'},403);
  const rows=await env.PROFILES.prepare('SELECT id,version,sdk,stack,occurred_at,received_at FROM crash_reports ORDER BY received_at DESC LIMIT 100').all();
  const reports=rows.results||[];
  // ?notify=<id> re-sends one that Discord dropped: the report is already in
  // the table, so it is sent from there rather than waited for from the phone.
  const again=new URL(request.url).searchParams.get('notify');
  if(again){
   const row=reports.find(report=>report.id===again);
   if(!row)return reply({error:'no_such_report'},404);
   const result=await notifyCrash(env,row);
   return reply({report:row,...result},result.sent?200:502);
  }
  return reply({reports});
 }
 if(request.method!=='POST')return reply({error:'method'},405);
 const now=Date.now();const key='crash:'+await digest((request.headers.get('cf-connecting-ip')||'unknown')+':'+Math.floor(now/3600000));
 const quota=await env.PROFILES.prepare('INSERT INTO limits(key,hits,until) VALUES(?,1,?) ON CONFLICT(key) DO UPDATE SET hits=hits+1 RETURNING hits').bind(key,now+3600000).first();if(quota.hits>10)return reply({error:'rate_limited'},429);
 if(Number(request.headers.get('content-length'))>16384)return reply({error:'too_large'},413);
 const reader=request.body?.getReader();if(!reader)return reply({error:'body'},400);const parts=[];let size=0;
 for(;;){const {done,value}=await reader.read();if(done)break;size+=value.length;if(size>16384){await reader.cancel();return reply({error:'too_large'},413);}parts.push(value);}
 const bytes=new Uint8Array(size);let offset=0;for(const part of parts){bytes.set(part,offset);offset+=part.length;}let body;try{body=JSON.parse(new TextDecoder().decode(bytes));}catch{return reply({error:'json'},400);}
 if(!validReport(body))return reply({error:'report'},400);
 const payload=reportPayload(body);if(payload==null)return reply({error:'report'},400);
 const occurred=Math.max(now-2592000000,Math.min(now,body.time));
 await env.PROFILES.prepare('INSERT OR IGNORE INTO crash_reports(id,version,sdk,stack,occurred_at,received_at) VALUES(?,?,?,?,?,?)').bind(body.id,body.version,body.sdk,payload,occurred,now).run();
 // A person reads these, so the report is handed to Discord before the answer
 // goes back -- but the answer depends only on the report being stored, and a
 // notification that did not land is said so rather than retried from here.
 let notified=false,notice=null;
 try{
  const result=await notifyCrash(env,{id:body.id,version:body.version,sdk:body.sdk,stack:payload,occurred_at:occurred,received_at:now});
  notified=result.sent;notice=result.sent?null:result.reason;
 }catch(error){
  console.error('ttcuz crash notify failed',String(error?.message||error));
  notice='notify_failed';
 }
 await env.PROFILES.prepare('DELETE FROM crash_reports WHERE received_at < ?').bind(now-2592000000).run();
 await env.PROFILES.prepare('DELETE FROM limits WHERE until < ?').bind(now).run();
 return reply({ok:true,notified,...(notice?{notice}:{})},202);
}
