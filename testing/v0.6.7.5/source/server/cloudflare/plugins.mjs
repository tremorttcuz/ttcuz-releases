import {digest} from './profiles.mjs';

const reply=(body,status=200)=>Response.json(body,{status,headers:{
  'cache-control':'no-store','access-control-allow-origin':'*',
  'access-control-allow-methods':'GET,POST,OPTIONS','access-control-allow-headers':'content-type,authorization',
}});
const object=value=>value!==null&&typeof value==='object'&&!Array.isArray(value);
const cleanText=(value,max)=>typeof value==='string'&&value.length>0&&value.length<=max&&!/[\p{Cc}\p{Cf}]/u.test(value);
const only=(value,allowed)=>object(value)&&Object.keys(value).every(key=>allowed.includes(key));

export function validateScript(value){
  const raw=JSON.stringify(value);
  if(!object(value)||new TextEncoder().encode(raw).length>65536)throw new Error('Плагин больше 64 КБ или неверный формат');
  if(!only(value,['schema','id','name','version','author','rows','names','colours','icon'])||value.schema!==1)throw new Error('Неизвестный формат');
  if(value.icon){const b=unpack64(value.icon,32768);if(b[0]!==137||b[1]!==80||b[2]!==78||b[3]!==71)throw new Error('invalid_icon');}
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

function authorized(request,env){
  const secret=env.PLUGIN_ADMIN_TOKEN||env.CRASH_ADMIN_TOKEN;
  if(!secret)return false;
  const a=new TextEncoder().encode(request.headers.get('authorization')||'');
  const b=new TextEncoder().encode('Bearer '+secret);
  if(a.length!==b.length)return false;let diff=0;for(let i=0;i<a.length;i++)diff|=a[i]^b[i];return diff===0;
}
async function creator(body,env){
  if(body?.uid!=='7491898648855512119'||!/^[a-f0-9]{64}$/.test(body?.token||''))return false;
  const owner=await env.PROFILES.prepare('SELECT owner_hash,verified FROM profiles WHERE uid=?').bind(body.uid).first();
  return !!owner?.verified && owner.owner_hash===await digest(body.token);
}
function unpack64(value,max){
  if(typeof value!=='string'||value.length>Math.ceil(max/3)*4)throw new Error('file_too_large');
  const plain=atob(value);if(plain.length>max)throw new Error('file_too_large');
  return Uint8Array.from(plain,c=>c.charCodeAt(0));
}

export async function pluginRoute(request,env){
  const path=new URL(request.url).pathname;
  if(!path.startsWith('/plugins/'))return null;
  if(request.method==='OPTIONS')return reply({},204);
  if(path==='/plugins/catalog.json'){
    if(request.method!=='GET'&&request.method!=='HEAD')return reply({error:'method'},405);
    const base=await env.ASSETS.fetch(new Request(new URL('/plugins/catalog.json',request.url),request));
    let catalog={plugins:[]};try{catalog=await base.json();}catch{}
    if(env.PROFILES){const rows=await env.PROFILES.prepare('SELECT metadata FROM plugin_packages ORDER BY updated DESC LIMIT 100').all();
      const dynamic=(rows.results||[]).map(row=>JSON.parse(row.metadata));
      const ids=new Set(dynamic.map(p=>p.id));catalog.plugins=[...dynamic,...(catalog.plugins||[]).filter(p=>!ids.has(p.id))].slice(0,100);}
    const response=reply(catalog);return request.method==='HEAD'?new Response(null,{headers:response.headers}):response;
  }
  if(!env.PROFILES)return reply({error:'storage_unavailable'},503);
  const file=path.match(/^\/plugins\/files\/([A-Za-z0-9][A-Za-z0-9._-]{0,79})\/(archive\.ctt|icon\.png)$/);
  if(file && ['GET','HEAD'].includes(request.method)){
    const row=await env.PROFILES.prepare('SELECT archive,icon FROM plugin_packages WHERE id=?').bind(file[1]).first();
    const value=file[2]==='archive.ctt'?row?.archive:row?.icon;
    if(!value)return reply({error:'not_found'},404);
    const bytes=unpack64(value,1048576);
    return new Response(request.method==='HEAD'?null:bytes,{headers:{'content-type':file[2]==='icon.png'?'image/png':'application/zip','cache-control':'no-store','access-control-allow-origin':'*'}});
  }
  if(path==='/plugins/admin' && request.method==='POST'){
    let body;try{body=await readJson(request,1500000);}catch(error){return reply({error:error.message},400);}
    if(!await creator(body,env))return reply({error:'creator_verification_required'},403);
    if(body.action==='list'){
      const scripts=await env.PROFILES.prepare("SELECT digest,plugin_id,source,state,added FROM script_submissions WHERE state IN ('approved','pending') ORDER BY added DESC LIMIT 100").all();
      const packages=await env.PROFILES.prepare('SELECT metadata FROM plugin_packages ORDER BY updated DESC LIMIT 100').all();
      return reply({scripts:scripts.results||[],packages:(packages.results||[]).map(row=>JSON.parse(row.metadata))});
    }
    if(body.action==='remove'){
      if(!/^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$/.test(body.id||''))return reply({error:'invalid_id'},400);
      if(body.kind==='package')await env.PROFILES.prepare('DELETE FROM plugin_packages WHERE id=?').bind(body.id).run();
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
        const hash=Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',bytes)),n=>n.toString(16).padStart(2,'0')).join('');meta={id:m.id,name:m.name,version:m.version,author:m.author,about:m.about,tiktok:m.tiktok,sha256:hash,size:bytes.length,url:'/plugins/files/'+m.id+'/archive.ctt',icon:''};
        if(body.icon){const image=unpack64(body.icon,32768);if(image[0]!==137||image[1]!==80||image[2]!==78||image[3]!==71)throw new Error('invalid_icon');icon=body.icon;meta.icon='/plugins/files/'+m.id+'/icon.png';}
      }catch(error){return reply({error:error.message},400);}
      await env.PROFILES.prepare('INSERT INTO plugin_packages(id,metadata,archive,icon,updated) VALUES(?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET metadata=excluded.metadata,archive=excluded.archive,icon=excluded.icon,updated=excluded.updated').bind(meta.id,JSON.stringify(meta),body.archive,icon,Date.now()).run();
      return reply({ok:true});
    }
    return reply({error:'invalid_action'},400);
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
    if(!authorized(request,env))return reply({error:env.PLUGIN_ADMIN_TOKEN||env.CRASH_ADMIN_TOKEN?'admin_required':'admin_unconfigured'},env.PLUGIN_ADMIN_TOKEN||env.CRASH_ADMIN_TOKEN?403:503);
    const rows=await env.PROFILES.prepare("SELECT digest,plugin_id,source,added FROM script_submissions WHERE state='pending' ORDER BY added LIMIT 100").all();
    return reply({submissions:rows.results||[]});
  }
  if(path==='/plugins/scripts/admin/review'&&request.method==='POST'){
    if(!authorized(request,env))return reply({error:env.PLUGIN_ADMIN_TOKEN||env.CRASH_ADMIN_TOKEN?'admin_required':'admin_unconfigured'},env.PLUGIN_ADMIN_TOKEN||env.CRASH_ADMIN_TOKEN?403:503);
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
