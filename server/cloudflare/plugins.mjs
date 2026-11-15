import {reviewRoute,ratings,account} from './reviews.mjs';
import {googleUser} from './google.mjs';
import {accountRoute,webAccount,face} from './accounts.mjs';
import {unzip} from './guard.mjs';
import {notifyPlugin,postTo,removeMessage,editMessage,approvedMessage,storeMessage,pluginMessage} from './discord.mjs';
import {inspectSources,inspectPackage,geminiReview} from './guard.mjs';
import {digest} from './profiles.mjs';
import {admin,refusal} from './admin.mjs';
import {noteDownload,downloadCounts,pluginHistory} from './stats.mjs';

// cache: how long a browser may keep the answer; public reads say so, so a page asks the worker less often
const reply=(body,status=200,cache='no-store')=>Response.json(body,{status,headers:{
  'cache-control':cache,'access-control-allow-origin':'*',
  'access-control-allow-methods':'GET,POST,OPTIONS','access-control-allow-headers':'content-type,authorization,x-admin-pin,x-google-token',
}});
const object=value=>value!==null&&typeof value==='object'&&!Array.isArray(value);
const cleanText=(value,max)=>typeof value==='string'&&value.length>0&&value.length<=max&&!/[\p{Cc}\p{Cf}]/u.test(value);
const only=(value,allowed)=>object(value)&&Object.keys(value).every(key=>allowed.includes(key));

export function validateScript(value){
  const raw=JSON.stringify(value);
  if(!object(value)||new TextEncoder().encode(raw).length>65536)throw new Error('Плагин больше 64 КБ или неверный формат');
  if(!only(value,['schema','id','name','version','author','rows','names','colours','icon'])||value.schema!==1)throw new Error('Неизвестный формат');
  if(value.icon)iconCheck(value.icon);
  if(!cleanText(value.id,80)||!/^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$/.test(value.id))throw new Error('Некорректный ID');
  for(const [key,max] of [['name',80],['version',32],['author',80]])if(!cleanText(value[key],max))throw new Error('Некорректное поле: '+key);
  for(const key of ['rows','names','colours']){
    const items=value[key]??[];
    if(!Array.isArray(items)||items.length>16)throw new Error('Не больше 16 команд в списке');
    for(const item of items){
      if(key==='rows'){
        if(!only(item,['title','action','value'])||!cleanText(item.title,80)||!['message','copy','window'].includes(item.action)||!cleanText(item.value,1024))throw new Error('Некорректная команда');
      }else if(key==='names'){
        if(!only(item,['uid','value'])||!cleanText(item.uid,32)||!/^[0-9]{1,32}$/.test(item.uid)||!cleanText(item.value,80))throw new Error('Некорректное имя');
      }else if(!only(item,['from','to'])||!/^#[a-fA-F0-9]{6}$/.test(item.from||'')||!/^#[a-fA-F0-9]{6}$/.test(item.to||''))throw new Error('Цвет: #RRGGBB');
    }
  }
  return value;
}

function canonical(value){
  if(Array.isArray(value))return '['+value.map(canonical).join(',')+']';
  if(object(value))return '{'+Object.keys(value).sort().map(key=>JSON.stringify(key)+':'+canonical(value[key])).join(',')+'}';
  return JSON.stringify(value);
}

async function readJson(request,limit){
  if(Number(request.headers.get('content-length'))>limit)throw new Error('Слишком большой запрос');
  const reader=request.body?.getReader();if(!reader)throw new Error('Пустой запрос');
  const parts=[];let size=0;
  for(;;){const {done,value}=await reader.read();if(done)break;size+=value.length;if(size>limit){await reader.cancel();throw new Error('Слишком большой запрос');}parts.push(value);}
  const bytes=new Uint8Array(size);let at=0;for(const part of parts){bytes.set(part,at);at+=part.length;}
  return JSON.parse(new TextDecoder().decode(bytes));
}

const CREATOR_HINT='Нужен PIN-код администратора (заголовок X-Admin-Pin) или подтверждённый телефон владельца.';
async function creator(body,env){
  if(body?.uid!=='7491898648855512119'||!/^[a-f0-9]{64}$/.test(body?.token||''))return false;
  const owner=await env.PROFILES.prepare('SELECT owner_hash,verified FROM profiles WHERE uid=?').bind(body.uid).first();
  return !!owner?.verified && owner.owner_hash===await digest(body.token);
}
/** A plugin's picture: a PNG of up to 32 KB, or an animated GIF of up to 96 KB. Returns the base64 it was given. */
function iconCheck(value){
  const big=typeof value==='string'&&value.startsWith('R0lGOD');
  const b=unpack64(value,big?98304:32768);
  const head=String.fromCharCode(...b.slice(0,6));
  if(head==='GIF89a'||head==='GIF87a'){
    // a real GIF: a header, a size of at most 512 px a side, and its trailer
    const w=b[6]|b[7]<<8,h=b[8]|b[9]<<8;
    if(b.length<14||!w||!h||w>512||h>512||b[b.length-1]!==0x3B)throw new Error('invalid_icon');
    return value;
  }
  if(b[0]!==137||b[1]!==80||b[2]!==78||b[3]!==71)throw new Error('invalid_icon');
  return value;
}
function bannerCheck(value){
  const banner=unpack64(value,300000);
  const jpeg=banner[0]===0xFF&&banner[1]===0xD8,webp=banner[8]===87&&banner[9]===69&&banner[10]===66&&banner[11]===80,png=banner[0]===137&&banner[1]===80;
  if(!jpeg&&!webp&&!png)throw new Error('invalid_banner');
  return value;
}
const pictureType=bytes=>bytes[0]===0xFF?'image/jpeg':bytes[0]===137?'image/png':bytes[0]===71&&bytes[1]===73?'image/gif':'image/webp';
function unpack64(value,max){
  if(typeof value!=='string'||value.length>Math.ceil(max/3)*4)throw new Error('file_too_large');
  const plain=atob(value);if(plain.length>max)throw new Error('file_too_large');
  return Uint8Array.from(plain,c=>c.charCodeAt(0));
}

const NATIVE_TABLE="CREATE TABLE IF NOT EXISTS native_submissions(id INTEGER PRIMARY KEY AUTOINCREMENT,plugin_id TEXT NOT NULL,metadata TEXT NOT NULL,sources TEXT NOT NULL,archive TEXT NOT NULL,icon TEXT,handle TEXT,uid TEXT,state TEXT NOT NULL DEFAULT 'pending',added INTEGER NOT NULL,ip_hash TEXT,review TEXT,discord TEXT)";
// tables made before the Gemini review have no review column yet
let tablesReady=false;
// the catalog is read on every store visit; one isolate answers the same one for 30 s, a browser keeps it 60 s
let catalogMemo=null;
const CATALOG_CACHE='public, max-age=60, stale-while-revalidate=300';
/** After anything that changes the store, the next catalog is read fresh. */
export function catalogChanged(){catalogMemo=null;}
async function nativeTable(env){
  if(tablesReady)return;
  await env.PROFILES.prepare(NATIVE_TABLE).run();
  for(const column of ['review TEXT','discord TEXT','banner TEXT','reviews_open INTEGER'])try{await env.PROFILES.prepare('ALTER TABLE native_submissions ADD COLUMN '+column).run();}catch{}
  try{await env.PROFILES.prepare('ALTER TABLE plugin_packages ADD COLUMN banner TEXT').run();}catch{}
  tablesReady=true;
}
/** What a plugin says it needs: ttcuz names (network, interface...) and Android permissions. */
export function permissionList(value){
  if(value==null)return [];
  if(!Array.isArray(value)||value.length>32)throw new Error('invalid_permissions');
  const out=[];for(const p of value){if(typeof p!=='string'||!/^[A-Za-z][\w.]{1,95}$/.test(p))throw new Error('invalid_permissions');if(!out.includes(p))out.push(p);}
  return out;
}
const JAVA_PATH=/^java\/(?:[A-Za-z_$][\w$]*\/)*[A-Za-z_$][\w$]*\.java$/;
const ENTRY=/^[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+$/;
/** 1.10 > 1.9 > 1.9-beta: numbers compared as numbers, a version that has more parts wins a tie. */
export function newerVersion(next,old){
  const parts=v=>String(v||'').split(/[.\-+ ]/).map(x=>/^\d+$/.test(x)?Number(x):x);
  const a=parts(next),b=parts(old);
  for(let i=0;i<Math.max(a.length,b.length);i++){
    const x=a[i],y=b[i];if(x===undefined)return false;if(y===undefined)return true;
    if(typeof x==='number'&&typeof y==='number'){if(x!==y)return x>y;continue;}
    if(String(x)!==String(y))return typeof x==='number'||(typeof y!=='number'&&String(x)>String(y));
  }
  return false;
}
/** Who may update a plugin in the store: the account that first sent it (its id is kept only as a hash). */
async function ownerOf(uid){return uid?(await sha256(new TextEncoder().encode('ttcuz-plugin-owner:'+uid))).slice(0,32):'';}
/** Files added, changed and removed since the version in the store, with lines added and removed. */
export function sourceChanges(before,after){
  const old=new Map((before||[]).map(f=>[f.path,f.text])),out={added:[],changed:[],removed:[],plus:0,minus:0};
  for(const f of after){
    if(!old.has(f.path)){out.added.push(f.path);out.plus+=f.text.split('\n').length;continue;}
    const was=old.get(f.path);old.delete(f.path);if(was===f.text)continue;
    out.changed.push(f.path);
    const count=list=>{const m=new Map();for(const l of list)m.set(l,(m.get(l)||0)+1);return m;};
    const a=count(was.split('\n')),b=count(f.text.split('\n'));
    for(const [line,n] of b)out.plus+=Math.max(0,n-(a.get(line)||0));
    for(const [line,n] of a)out.minus+=Math.max(0,n-(b.get(line)||0));
  }
  for(const [path,text] of old){out.removed.push(path);out.minus+=text.split('\n').length;}
  return out;
}
/**
 * A checked submission joins the queue: obfuscated code and packages not built from their sources are refused, an
 * update has to come from the plugin's owner with a newer version; then it is stored, Discord is told and Gemini reads it.
 */
async function accept(request,env,ctx,item){
    // unreadable code, hidden code or a package that is not built from these sources never reaches the queue
    const unreadable=inspectSources(item.files);
    if(unreadable.length)return reply({error:'obfuscated',reasons:unreadable,hint:'Код запутан или прячет другой код, такой плагин не принимается: '+unreadable.join('; ')},422);
    const mismatch=await inspectPackage(item.bytes,item.files,item.meta);
    if(mismatch.length)return reply({error:'package_mismatch',reasons:mismatch,hint:'Пакет не совпадает с исходниками: '+mismatch.join('; ')},422);
    const now=Date.now(),hour=Math.floor(now/3600000),ip=request.headers.get('cf-connecting-ip')||'unknown';
    const key='native-submit:'+await digest(ip+':'+hour);
    const quota=await env.PROFILES.prepare('INSERT INTO limits(key,hits,until) VALUES(?,1,?) ON CONFLICT(key) DO UPDATE SET hits=hits+1,until=excluded.until RETURNING hits').bind(key,(hour+1)*3600000).first();
    if(quota?.hits>6)return reply({error:'rate_limited'},429);
    await nativeTable(env);
    // A plugin already in the store: only its author may send a new version, and it has to be newer. The review
    // then shows it as an update: from which version, what changed in the code, and the author's "what's new".
    const listed=await env.PROFILES.prepare('SELECT metadata FROM plugin_packages WHERE id=?').bind(item.meta.id).first();
    if(listed){
      let was={};try{was=JSON.parse(listed.metadata);}catch{}
      const owner=await ownerOf(item.uid);
      const mine=was.owner?owner&&owner===was.owner:(was.by&&item.handle&&was.by.toLowerCase()===item.handle.toLowerCase());
      if(!mine)return reply({error:'not_owner',hint:'Плагин с ID '+item.meta.id+' уже есть в магазине, и обновлять его может только автор. Если это ваш плагин, отправьте его с того же аккаунта TikTok; если нет, смените ID.'},403);
      if(!newerVersion(item.meta.version,was.version))return reply({error:'version_not_newer',hint:'В магазине уже версия '+was.version+'. Поднимите версию (например, '+String(was.version||'1.0').replace(/(\d+)(?!.*\d)/,n=>String(Number(n)+1))+') и отправьте снова.'},409);
      const last=await env.PROFILES.prepare("SELECT sources FROM native_submissions WHERE plugin_id=? AND state='approved' ORDER BY added DESC LIMIT 1").bind(item.meta.id).first();
      let before=null;try{before=last?.sources?JSON.parse(last.sources):null;}catch{}
      item.meta.update={from:String(was.version||''),...(before?{diff:sourceChanges(before,item.files)}:{})};
      if(item.meta.update.diff){const d=item.meta.update.diff;for(const k of ['added','changed','removed'])d[k]=d[k].slice(0,20);}
    }
    const pending=await env.PROFILES.prepare("SELECT COUNT(*) AS count FROM native_submissions WHERE state='pending'").first();
    if((pending?.count||0)>=300)return reply({error:'queue_full'},429);
    // a newer version from the same sender replaces the one still waiting
    if(item.uid)await env.PROFILES.prepare("UPDATE native_submissions SET state='superseded' WHERE plugin_id=? AND uid=? AND state='pending'").bind(item.meta.id,item.uid).run();
    else await env.PROFILES.prepare("UPDATE native_submissions SET state='superseded' WHERE plugin_id=? AND handle=? AND state='pending'").bind(item.meta.id,item.handle).run();
    const row=await env.PROFILES.prepare("INSERT INTO native_submissions(plugin_id,metadata,sources,archive,icon,handle,uid,state,added,ip_hash,banner,reviews_open) VALUES(?,?,?,?,?,?,?,'pending',?,?,?,?) RETURNING id")
      .bind(item.meta.id,JSON.stringify(item.meta),item.sources,item.archive,item.icon,item.handle,item.uid,now,key,item.banner||null,item.reviewsOpen?1:0).first();
    // Stored first; then, after the phone has its answer, Discord is told at once and Gemini's reading is written
    // into that same message. Work after a reply is cut off about 30 s later, so the message never waits for Gemini
    // (it used to, and a slow Gemini meant no message at all); the cron sends whatever still got lost.
    const origin=new URL(request.url).origin,id=row?.id??null;
    const later=(async()=>{
      const card={meta:item.meta,handle:item.handle,files:item.files.length};
      const told=await notifyPlugin(env,{...card,review:{verdict:'pending'}},origin);
      if(!told.sent)console.error('ttcuz plugin notify failed',told.reason);
      // where the review message is, so a decision can take it out of the review channel
      else if(told.channel&&told.id)try{await env.PROFILES.prepare('UPDATE native_submissions SET discord=? WHERE id=?').bind(JSON.stringify({channel:told.channel,id:told.id}),id).run();}catch{}
      const review=await geminiReview(env,item.meta,item.files,22000);
      try{
        await env.PROFILES.prepare('UPDATE native_submissions SET review=? WHERE id=?').bind(JSON.stringify(review),id).run();
        if(review.obfuscated)await env.PROFILES.prepare("UPDATE native_submissions SET state='rejected' WHERE id=? AND state='pending'").bind(id).run();
      }catch(error){console.error('ttcuz plugin review store failed',String(error?.message||error));}
      if(told.sent&&told.channel&&told.id)await editMessage(env,{channel:told.channel,id:told.id},pluginMessage({...card,review},origin));
    })();
    if(ctx&&typeof ctx.waitUntil==='function')ctx.waitUntil(later);else await later;
    return reply({status:'pending',id,update:item.meta.update?{from:item.meta.update.from}:undefined},202);
}
async function sha256(bytes){return Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',bytes)),n=>n.toString(16).padStart(2,'0')).join('');}
// A plugin from the in-app editor: its manifest, its Java, the .ctt the phone built from that Java, and a picture.
function nativeSubmission(body){
  const m=body?.metadata;if(!object(m)||!/^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$/.test(m.id||''))throw new Error('invalid_id');
  for(const [key,max] of [['name',80],['version',32],['author',80]])if(!cleanText(m[key],max))throw new Error('invalid_'+key);
  if(!ENTRY.test(m.entry||''))throw new Error('invalid_entry');
  const meta={id:m.id,name:m.name.trim(),version:m.version.trim(),author:m.author.trim(),
    about:typeof m.description==='string'?m.description.trim().slice(0,240):'',entry:m.entry,
    tiktok:typeof m.tiktok==='string'?m.tiktok.slice(0,32):'',permissions:permissionList(m.permissions)};
  const changes=typeof m.changes==='string'?m.changes.trim().slice(0,500):'';if(changes)meta.changes=changes;
  if(!Array.isArray(body.files)||body.files.length<1||body.files.length>32)throw new Error('invalid_files');
  const paths=new Set();
  for(const f of body.files){
    if(!object(f)||typeof f.path!=='string'||!JAVA_PATH.test(f.path)||paths.has(f.path)||typeof f.text!=='string'||f.text.length>240000)throw new Error('invalid_file');
    paths.add(f.path);
  }
  const sources=JSON.stringify(body.files.map(f=>({path:f.path,text:f.text})));if(sources.length>1000000)throw new Error('sources_too_large');
  const bytes=unpack64(body.archive,500000);if(bytes[0]!==80||bytes[1]!==75||bytes[2]!==3||bytes[3]!==4)throw new Error('invalid_archive');
  let icon=null;
  if(body.icon)icon=iconCheck(body.icon);
  const handle=typeof body.handle==='string'?body.handle.replace(/^@/,'').replace(/[^\w.]/g,'').slice(0,40):'';
  const uid=/^\d{1,24}$/.test(body.uid||'')?body.uid:'';
  return {meta,sources,files:body.files.map(f=>({path:f.path,text:f.text})),bytes,archive:body.archive,icon,handle,uid};
}

export async function pluginRoute(request,env,ctx){
  const path=new URL(request.url).pathname;
  if(!path.startsWith('/plugins/'))return null;
  if(request.method==='OPTIONS')return new Response(null,{status:204,headers:reply({}).headers});
  if(path==='/plugins/catalog.json'){
    if(request.method!=='GET'&&request.method!=='HEAD')return reply({error:'method'},405);
    if(catalogMemo&&catalogMemo.until>Date.now()){const response=reply(catalogMemo.body,200,CATALOG_CACHE);return request.method==='HEAD'?new Response(null,{headers:response.headers}):response;}
    const base=await env.ASSETS.fetch(new Request(new URL('/plugins/catalog.json',request.url),request));
    let catalog={plugins:[]};try{catalog=await base.json();}catch{}
    if(env.PROFILES){const rows=await env.PROFILES.prepare('SELECT metadata FROM plugin_packages ORDER BY updated DESC LIMIT 100').all();
      // the stars of each plugin, in the same answer, so a list never asks per plugin
      let stars={};try{stars=await ratings(env);}catch{}
      // how often each was downloaded, shown beside the install button
      let downloads={};try{downloads=await downloadCounts(env);}catch{}
      const dynamic=(rows.results||[]).map(row=>{const meta=JSON.parse(row.metadata);delete meta.owner;if(stars[meta.id])meta.rating=stars[meta.id];meta.downloads=downloads[meta.id]||0;
        // the package's hash versions its pictures: a new version is a new URL, so browsers keep them for a year
        const v=String(meta.sha256||'').slice(0,12);if(v&&meta.icon)meta.icon+='?v='+v;
        // the banner is versioned by its own picture: it can change without a new version of the plugin
        const bv=meta.bannerV||v;if(bv&&meta.banner)meta.banner+='?v='+bv;
        return meta;});
      const ids=new Set(dynamic.map(p=>p.id));catalog.plugins=[...dynamic,...(catalog.plugins||[]).filter(p=>!ids.has(p.id))].slice(0,100);}
    catalogMemo={body:catalog,until:Date.now()+30000};
    const response=reply(catalog,200,CATALOG_CACHE);return request.method==='HEAD'?new Response(null,{headers:response.headers}):response;
  }
  if(!env.PROFILES)return reply({error:'storage_unavailable'},503);
  // a plugin's statistics: its author's (and the creator's) to see, nobody else's
  const statsFor=path.match(/^\/plugins\/stats\/([A-Za-z0-9][A-Za-z0-9._-]{0,79})$/);
  if(statsFor){
    if(request.method!=='POST')return reply({error:'method'},405);
    let body;try{body=await readJson(request,8000);}catch(error){return reply({error:error.message},400);}
    const who=await account(env,body,request),boss=await creator(body,env);
    if(!who&&!boss)return reply({error:'account_required',hint:'Войдите через Google на сайте или откройте магазин в приложении ttcuz.'},403);
    const id=statsFor[1];
    const row=await env.PROFILES.prepare('SELECT metadata FROM plugin_packages WHERE id=?').bind(id).first();
    if(!row)return reply({error:'not_found',hint:'Плагина нет в магазине.'},404);
    let meta={};try{meta=JSON.parse(row.metadata);}catch{}
    if(!boss&&!(meta.owner&&who&&meta.owner===await ownerOf(who.uid)))return reply({error:'not_owner',hint:'Статистику видит только автор плагина.'},403);
    let downloads=0,rating={};
    try{downloads=(await downloadCounts(env))[id]||0;}catch{}
    try{rating=(await ratings(env))[id]||{};}catch{}
    const days=await pluginHistory(env,id);
    return reply({ok:true,downloads,up:rating.up||0,dn:rating.dn||0,average:rating.average||0,reviews:rating.count||0,days});
  }
  {const reviewed=await reviewRoute(request,env,path,reply,admin);if(reviewed)return reviewed;}
  const file=path.match(/^\/plugins\/files\/([A-Za-z0-9][A-Za-z0-9._-]{0,79})\/(archive\.ctt|icon\.png|banner)$/);
  if(file && ['GET','HEAD'].includes(request.method)){
    if(file[2]==='banner')await nativeTable(env);
    const row=await env.PROFILES.prepare(file[2]==='banner'?'SELECT banner FROM plugin_packages WHERE id=?':'SELECT archive,icon FROM plugin_packages WHERE id=?').bind(file[1]).first();
    const value=file[2]==='archive.ctt'?row?.archive:file[2]==='banner'?row?.banner:row?.icon;
    if(!value)return reply({error:'not_found'},404);
    if(file[2]==='archive.ctt'&&request.method==='GET')noteDownload(file[1],env,ctx);
    const bytes=unpack64(value,1048576);
    const type=file[2]==='archive.ctt'?'application/zip':pictureType(bytes);
    const versioned=new URL(request.url).searchParams.has('v');
    return new Response(request.method==='HEAD'?null:bytes,{headers:{'content-type':type,'cache-control':file[2]==='archive.ctt'?'public, max-age=60':versioned?'public, max-age=31536000, immutable':'public, max-age=3600','access-control-allow-origin':'*'}});
  }
  // A published plugin's banner, set, changed or taken away by its author (from the store page or the app) or by
  // the creator -- no new version needed. "mine" lists the plugins the asking account may do this for.
  if((path==='/plugins/banner'||path==='/plugins/banner/mine')&&request.method==='POST'){
    let body;try{body=await readJson(request,500000);}catch(error){return reply({error:error.message},400);}
    const who=await account(env,body,request),boss=await creator(body,env);
    if(!who&&!boss)return reply({error:'account_required',hint:'Войдите через Google на сайте или откройте магазин в приложении ttcuz.'},403);
    const mineHash=who?await ownerOf(who.uid):'';
    if(path==='/plugins/banner/mine'){
      const rows=await env.PROFILES.prepare('SELECT metadata FROM plugin_packages ORDER BY updated DESC LIMIT 100').all();
      const ids=[];for(const row of rows.results||[]){try{const m=JSON.parse(row.metadata);if(boss||(m.owner&&m.owner===mineHash))ids.push(m.id);}catch{}}
      return reply({ids});
    }
    if(!/^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$/.test(body.id||''))return reply({error:'invalid_id'},400);
    const row=await env.PROFILES.prepare('SELECT metadata FROM plugin_packages WHERE id=?').bind(body.id).first();
    if(!row)return reply({error:'not_found',hint:'Плагина нет в магазине.'},404);
    const meta=JSON.parse(row.metadata);
    if(!boss&&!(meta.owner&&meta.owner===mineHash))return reply({error:'not_owner',hint:'Баннер может поменять только автор плагина.'},403);
    let banner=null;
    if(body.banner){try{banner=bannerCheck(body.banner);}catch{return reply({error:'invalid_banner',hint:'Баннер: JPEG, PNG или WebP до 300 КБ.'},400);}}
    if(banner){meta.banner='/plugins/files/'+meta.id+'/banner';meta.bannerV=(await digest(banner)).slice(0,12);}else{delete meta.banner;delete meta.bannerV;}
    await nativeTable(env);
    await env.PROFILES.prepare('UPDATE plugin_packages SET banner=?,metadata=? WHERE id=?').bind(banner,JSON.stringify(meta),meta.id).run();
    catalogChanged();
    return reply({ok:true,banner:meta.banner?meta.banner+'?v='+meta.bannerV:''});
  }
  if(path==='/plugins/admin' && request.method==='POST'){
    let body;try{body=await readJson(request,1500000);}catch(error){return reply({error:error.message},400);}
    // the verified creator's phone, or the admin PIN
    if(!await creator(body,env)){const who=await admin(request,env);if(who==='locked'||who==='wrong')return refusal(who,reply);if(who!=='ok')return reply({error:'creator_verification_required',hint:CREATOR_HINT},403);}
    if(body.action==='list'){
      const scripts=await env.PROFILES.prepare("SELECT digest,plugin_id,source,state,added FROM script_submissions WHERE state IN ('approved','pending') ORDER BY added DESC LIMIT 100").all();
      const packages=await env.PROFILES.prepare('SELECT metadata FROM plugin_packages ORDER BY updated DESC LIMIT 100').all();
      return reply({scripts:scripts.results||[],packages:(packages.results||[]).map(row=>JSON.parse(row.metadata))});
    }
    if(body.action==='remove'){
      if(!/^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$/.test(body.id||''))return reply({error:'invalid_id'},400);
      if(body.kind==='package'){
        const was=await env.PROFILES.prepare('SELECT metadata FROM plugin_packages WHERE id=?').bind(body.id).first();
        catalogChanged();
        await env.PROFILES.prepare('DELETE FROM plugin_packages WHERE id=?').bind(body.id).run();
        if(was){let meta={id:body.id};try{meta=JSON.parse(was.metadata);}catch{}await postTo(env,'store',storeMessage('removed',meta,new URL(request.url).origin));}
      }
      else await env.PROFILES.prepare('DELETE FROM script_submissions WHERE plugin_id=?').bind(body.id).run();
      return reply({ok:true});
    }
    if(body.action==='publish-script'){
      let value,source,hash;try{value=validateScript(body.source);source=canonical(value);hash=await digest(source);}catch(error){return reply({error:error.message},400);}
      await env.PROFILES.batch([
        env.PROFILES.prepare("UPDATE script_submissions SET state='superseded' WHERE plugin_id=? AND state='approved'").bind(value.id),
        env.PROFILES.prepare("INSERT INTO script_submissions(digest,plugin_id,source,state,added,ip_hash) VALUES(?,?,?,'approved',?,'creator') ON CONFLICT(digest) DO UPDATE SET state='approved',added=excluded.added").bind(hash,value.id,source,Date.now())
      ]);return reply({ok:true});
    }
    if(body.action==='publish-package'){
      let bytes,meta,icon=null;try{
        bytes=unpack64(body.archive,1048576);if(bytes[0]!==80||bytes[1]!==75||bytes[2]!==3||bytes[3]!==4)throw new Error('invalid_archive');
        const m=body.metadata;if(!object(m)||!/^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$/.test(m.id||''))throw new Error('invalid_id');
        for(const [key,max] of [['name',80],['version',32],['author',80]])if(!cleanText(m[key],max))throw new Error('invalid_'+key);
        if(typeof m.about!=='string'||m.about.length>240||typeof m.tiktok!=='string'||m.tiktok.length>32)throw new Error('invalid_metadata');
        const hash=Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',bytes)),n=>n.toString(16).padStart(2,'0')).join('');meta={id:m.id,name:m.name,version:m.version,author:m.author,about:m.about,tiktok:m.tiktok,permissions:permissionList(m.permissions),by:typeof body.by==='string'?body.by.replace(/^@/,'').replace(/[^\w.]/g,'').slice(0,40):'',verified:true,sha256:hash,size:bytes.length,format:'ctt',url:'/plugins/files/'+m.id+'/archive.ctt',icon:''};
        if(body.icon){icon=iconCheck(body.icon);meta.icon='/plugins/files/'+m.id+'/icon.png';}
      }catch(error){return reply({error:error.message},400);}
      const before=await env.PROFILES.prepare('SELECT metadata FROM plugin_packages WHERE id=?').bind(meta.id).first();
      let was=null;try{was=before?JSON.parse(before.metadata):null;}catch{}
      if(was?.owner)meta.owner=was.owner;if(was)meta.previous=String(was.version||'');
      catalogChanged();
      await env.PROFILES.prepare('INSERT INTO plugin_packages(id,metadata,archive,icon,updated) VALUES(?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET metadata=excluded.metadata,archive=excluded.archive,icon=excluded.icon,updated=excluded.updated').bind(meta.id,JSON.stringify(meta),body.archive,icon,Date.now()).run();
      await postTo(env,'store',storeMessage(was?'updated':'added',meta,new URL(request.url).origin));
      return reply({ok:true});
    }
    return reply({error:'invalid_action'},400);
  }
  if(path==='/plugins/native/submit'&&request.method==='POST'){
    let item;
    try{const body=await readJson(request,1800000);item=nativeSubmission(body);
      // a banner from the app too (a 3:1 JPEG, WebP or PNG), as the store page sends it
      if(body.banner)item.banner=bannerCheck(body.banner);}
    catch(error){return reply({error:String(error.message||'invalid')},400);}
    return accept(request,env,ctx,item);
  }
  if(path==='/plugins/web/config'){
    // what the store page needs to offer Google sign-in; empty until the client id is set
    return reply({google:String(env.GOOGLE_CLIENT_ID||'')});
  }
  {const answered=await accountRoute(request,env,path,reply);if(answered)return answered;}
  if(path==='/plugins/web/submit'&&request.method==='POST'){
    // the store page: a signed-in Google account sends a built .ctt with its Java sources, a picture and a banner
    const user=await webAccount(request,env);
    if(!user)return reply({error:'sign_in',hint:'Войдите через Google, чтобы отправить плагин.'},401);
    if(!user.profile.username)return reply({error:'username',hint:'Сначала выберите имя пользователя в профиле.'},400);
    let item;
    try{
      const body=await readJson(request,1800000);
      const packed=unpack64(body.archive,500000);
      const entries=await unzip(packed);
      const manifest=JSON.parse(new TextDecoder().decode(entries.get('manifest.json')||new Uint8Array()));
      if(typeof body.changes==='string')manifest.changes=body.changes;
      item=nativeSubmission({metadata:manifest,files:body.files,archive:body.archive,icon:body.icon,handle:user.name.replace(/\s+/g,'_')});
      // the sender is the profile's @username
      item.uid=user.uid;item.handle=user.profile.username;
      if(body.banner)item.banner=bannerCheck(body.banner);
      item.reviewsOpen=body.reviewsOpen===true;
    }catch(error){return reply({error:String(error.message||'invalid'),hint:'Нужен собранный .ctt и его исходники .java: '+String(error.message||'')},400);}
    return accept(request,env,ctx,item);
  }
  if(path==='/plugins/native/queue'&&request.method==='GET'){
    // everything waiting for review, for everyone to see: what it is, who sent it, what Gemini thought; never the uid
    await nativeTable(env);
    const rows=await env.PROFILES.prepare("SELECT id,plugin_id,metadata,icon,handle,added,review,reviews_open,length(banner) AS banner,length(archive) AS packed FROM native_submissions WHERE state='pending' ORDER BY added DESC LIMIT 100").all();
    const parsed=v=>{try{return v?JSON.parse(v):null;}catch{return null;}};
    return reply({queue:(rows.results||[]).map(r=>{const review=parsed(r.review);return {id:r.id,plugin:JSON.parse(r.metadata),handle:r.handle||'',added:r.added,size:Math.floor((r.packed||0)*3/4),
      icon:r.icon?'/plugins/native/public/'+r.id+'/icon.png':'',banner:r.banner?'/plugins/native/public/'+r.id+'/banner':'',reviewsOpen:!!r.reviews_open,
      review:review?{verdict:review.obfuscated?'dangerous':review.verdict,summary:review.summary||''}:null};})},200,'public, max-age=30');
  }
  {
    const shown=path.match(/^\/plugins\/native\/public\/(\d{1,9})(?:\/(icon\.png|banner|sources))?$/);
    if(shown&&request.method==='GET'){
      // one waiting plugin: its sources for anyone to read, its picture and its banner
      await nativeTable(env);
      const r=await env.PROFILES.prepare("SELECT sources,icon,banner FROM native_submissions WHERE id=? AND state='pending'").bind(Number(shown[1])).first();
      if(!r)return reply({error:'not_found'},404);
      if(shown[2]==='icon.png'||shown[2]==='banner'){
        const value=shown[2]==='banner'?r.banner:r.icon;if(!value)return reply({error:'not_found'},404);
        const bytes=unpack64(value,1048576);
        const type=pictureType(bytes);
        return new Response(bytes,{headers:{'content-type':type,'cache-control':'public, max-age=86400','access-control-allow-origin':'*'}});
      }
      return reply({files:JSON.parse(r.sources)},200,'public, max-age=600');
    }
  }
  if(path==='/plugins/native/admin/pending'){
    if(request.method!=='GET')return reply({error:'method'},405);
    {const who=await admin(request,env);if(who!=='ok')return refusal(who,reply);}
    await nativeTable(env);
    const rows=await env.PROFILES.prepare("SELECT id,plugin_id,metadata,sources,icon,handle,uid,added,review,reviews_open,length(banner) AS banner,length(archive) AS packed FROM native_submissions WHERE state='pending' ORDER BY added LIMIT 50").all();
    const parsed=v=>{try{return v?JSON.parse(v):null;}catch{return null;}};
    return reply({submissions:(rows.results||[]).map(r=>({id:r.id,plugin:JSON.parse(r.metadata),files:JSON.parse(r.sources),icon:r.icon||'',handle:r.handle||'',uid:r.uid||'',added:r.added,size:Math.floor((r.packed||0)*3/4),banner:r.banner?'/plugins/native/public/'+r.id+'/banner':'',reviewsOpen:!!r.reviews_open,via:String(r.uid||'').startsWith('g:')?'site':'app',review:parsed(r.review)}))});
  }
  if(path==='/plugins/native/admin/announce'&&request.method==='POST'){
    // Puts every plugin still waiting for review into the review channel, once: the ones sent before the bot was on
    // the server, or while Discord was down. Ones that never had Gemini's reading get it first.
    {const who=await admin(request,env);if(who!=='ok')return refusal(who,reply);}
    const done=await announcePending(env,new URL(request.url).origin,20);
    return done.error?reply({error:'discord',hint:done.error,posted:done.posted},502):reply({posted:done.posted});
  }
  if(path==='/plugins/native/admin/review'&&request.method==='POST'){
    {const who=await admin(request,env);if(who!=='ok')return refusal(who,reply);}
    let body;try{body=await readJson(request,1024);}catch{return reply({error:'json'},400);}
    if(!Number.isInteger(body?.id)||!['approve','reject'].includes(body?.action))return reply({error:'review'},400);
    await nativeTable(env);
    const row=await env.PROFILES.prepare("SELECT * FROM native_submissions WHERE id=? AND state='pending'").bind(body.id).first();
    if(!row)return reply({error:'not_found'},404);
    const reviewMessage=(()=>{try{return row.discord?JSON.parse(row.discord):null;}catch{return null;}})();
    if(body.action==='reject'){
      await env.PROFILES.prepare("UPDATE native_submissions SET state='rejected' WHERE id=?").bind(row.id).run();
      await removeMessage(env,reviewMessage);
      return reply({status:'rejected',id:row.id});
    }
    const m=JSON.parse(row.metadata),bytes=unpack64(row.archive,500000);
    const before=await env.PROFILES.prepare('SELECT metadata FROM plugin_packages WHERE id=?').bind(m.id).first();
    let was=null;try{was=before?JSON.parse(before.metadata):null;}catch{}
    const meta={id:m.id,name:m.name,version:m.version,author:m.author,by:row.handle||'',about:m.about||'',tiktok:m.tiktok||'',permissions:Array.isArray(m.permissions)?m.permissions:[],
      sha256:await sha256(bytes),size:bytes.length,format:'ctt',verified:true,url:'/plugins/files/'+m.id+'/archive.ctt',icon:row.icon?'/plugins/files/'+m.id+'/icon.png':''};
    // the first author stays the owner; a store entry the admin put there gets this sender as its owner
    meta.owner=was?.owner||await ownerOf(row.uid);if(!meta.owner)delete meta.owner;
    // the banner goes along; an update without one keeps the old one
    const banner=row.banner||null;if(banner){meta.banner='/plugins/files/'+m.id+'/banner';meta.bannerV=(await digest(banner)).slice(0,12);}else if(was?.banner){meta.banner=was.banner;if(was.bannerV)meta.bannerV=was.bannerV;}
    if(m.changes)meta.changes=m.changes;
    if(was)meta.previous=String(was.version||'');
    catalogChanged();
    await env.PROFILES.batch([
      env.PROFILES.prepare('INSERT INTO plugin_packages(id,metadata,archive,icon,updated,banner) VALUES(?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET metadata=excluded.metadata,archive=excluded.archive,icon=excluded.icon,updated=excluded.updated,banner=COALESCE(excluded.banner,plugin_packages.banner)').bind(meta.id,JSON.stringify(meta),row.archive,row.icon,Date.now(),banner),
      env.PROFILES.prepare("UPDATE native_submissions SET state='approved' WHERE id=?").bind(row.id),
    ]);
    // out of the review channel, into the approved one, and the store's channel says it is there
    const origin=new URL(request.url).origin;
    await removeMessage(env,reviewMessage);
    await postTo(env,'approved',approvedMessage(meta,origin));
    await postTo(env,'store',storeMessage(was?'updated':'added',meta,origin));
    const shown={...meta};delete shown.owner;
    return reply({status:'approved',id:row.id,plugin:shown});
  }
  if(path==='/plugins/scripts'&&request.method==='GET'){
    const rows=await env.PROFILES.prepare("SELECT digest,source FROM script_submissions WHERE state='approved' ORDER BY added DESC LIMIT 100").all();
    const scripts=[];
    for(const row of rows.results||[])try{
      const value=validateScript(JSON.parse(row.source));
      if(await digest(canonical(value))===row.digest)scripts.push(value);
    }catch{}
    return reply({scripts});
  }
  if(path==='/plugins/scripts/submit'&&request.method==='POST'){
    let value,source,hash;
    try{value=validateScript(await readJson(request,65536));source=canonical(value);hash=await digest(source);}
    catch(error){return reply({error:String(error.message||'invalid')},400);}
    const now=Date.now(),hour=Math.floor(now/3600000),ip=request.headers.get('cf-connecting-ip')||'unknown';
    const key='plugin-submit:'+await digest(ip+':'+hour);
    const quota=await env.PROFILES.prepare('INSERT INTO limits(key,hits,until) VALUES(?,1,?) ON CONFLICT(key) DO UPDATE SET hits=hits+1,until=excluded.until RETURNING hits').bind(key,(hour+1)*3600000).first();
    if(quota?.hits>10)return reply({error:'rate_limited'},429);
    const pending=await env.PROFILES.prepare("SELECT COUNT(*) AS count FROM script_submissions WHERE state='pending'").first();
    if((pending?.count||0)>=1000)return reply({error:'queue_full'},429);
    await env.PROFILES.prepare("INSERT OR IGNORE INTO script_submissions(digest,plugin_id,source,state,added,ip_hash) VALUES(?,?,?,'pending',?,?)").bind(hash,value.id,source,now,key).run();
    return reply({status:'pending',digest:hash},202);
  }
  if(path==='/plugins/scripts/admin/pending'){
    if(request.method!=='GET')return reply({error:'method'},405);
    {const who=await admin(request,env);if(who!=='ok')return refusal(who,reply);}
    const rows=await env.PROFILES.prepare("SELECT digest,plugin_id,source,added FROM script_submissions WHERE state='pending' ORDER BY added LIMIT 100").all();
    return reply({submissions:rows.results||[]});
  }
  if(path==='/plugins/scripts/admin/review'&&request.method==='POST'){
    {const who=await admin(request,env);if(who!=='ok')return refusal(who,reply);}
    let body;try{body=await readJson(request,1024);}catch{return reply({error:'json'},400);}
    if(!/^[a-f0-9]{64}$/.test(body?.digest||'')||!['approve','reject'].includes(body?.action))return reply({error:'review'},400);
    const row=await env.PROFILES.prepare("SELECT plugin_id,source FROM script_submissions WHERE digest=? AND state='pending'").bind(body.digest).first();
    if(!row)return reply({error:'not_found'},404);
    try{validateScript(JSON.parse(row.source));if(await digest(canonical(JSON.parse(row.source)))!==body.digest)return reply({error:'digest_mismatch'},409);}catch{return reply({error:'invalid_submission'},409);}
    const state=body.action==='approve'?'approved':'rejected';
    if(state==='approved')await env.PROFILES.batch([
      env.PROFILES.prepare("UPDATE script_submissions SET state='superseded' WHERE plugin_id=? AND state='approved'").bind(row.plugin_id),
      env.PROFILES.prepare("UPDATE script_submissions SET state=? WHERE digest=? AND state='pending'").bind(state,body.digest),
    ]);
    else await env.PROFILES.prepare('UPDATE script_submissions SET state=? WHERE digest=? AND state=\'pending\'').bind(state,body.digest).run();
    return reply({status:state,digest:body.digest});
  }
  return reply({error:'not_found'},404);
}

/**
 * Every plugin still waiting that has no message in the review channel gets one; one that never had Gemini's reading
 * gets it, written into its message. Ones sent in the last minute are still being handled by their own submit. Run by the admin's "announce" and by the cron, so a submission whose message was
 * lost (Discord down, the worker cut off) still reaches the channel within minutes.
 */
export async function announcePending(env,origin,limit=5){
  await nativeTable(env);
  const rows=await env.PROFILES.prepare("SELECT id,metadata,sources,handle,review,discord FROM native_submissions WHERE state='pending' AND added<? AND (discord IS NULL OR discord='' OR review IS NULL OR review='') ORDER BY added LIMIT ?").bind(Date.now()-60000,limit).all();
  const posted=[];
  for(const row of rows.results||[]){
    const meta=JSON.parse(row.metadata);let files=[];try{files=JSON.parse(row.sources);}catch{}
    let review=null;try{review=row.review?JSON.parse(row.review):null;}catch{}
    let where=null;try{where=row.discord?JSON.parse(row.discord):null;}catch{}
    const card={meta,handle:row.handle,files:files.length};
    if(!where){
      const told=await postTo(env,'review',pluginMessage({...card,review:review||{verdict:'pending'}},origin));
      if(!told.sent)return {error:told.reason,posted};
      where={channel:told.channel,id:told.id};
      await env.PROFILES.prepare('UPDATE native_submissions SET discord=? WHERE id=?').bind(JSON.stringify(where),row.id).run();
      posted.push(row.id);
    }
    if(!review){
      review=await geminiReview(env,meta,files,20000);
      await env.PROFILES.prepare('UPDATE native_submissions SET review=? WHERE id=?').bind(JSON.stringify(review),row.id).run();
      if(review.obfuscated)await env.PROFILES.prepare("UPDATE native_submissions SET state='rejected' WHERE id=? AND state='pending'").bind(row.id).run();
      await editMessage(env,where,pluginMessage({...card,review},origin));
    }
  }
  return {posted};
}
