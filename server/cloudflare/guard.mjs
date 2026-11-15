// What a plugin sent for review must pass before a person ever looks at it.
//
// Three checks, cheapest first:
//   1. the Java must be readable: obfuscated code (meaningless or look-alike names, hidden unicode escapes,
//      minified lines, encrypted strings, embedded binaries) is refused outright -- a reviewer cannot judge it;
//   2. nothing may load code the reviewer never saw (DexClassLoader, native libraries, exec), and the package the
//      phone built must hold exactly the classes of the sources sent with it (read from the dex itself), with no
//      extra files beside them;
//   3. Gemini reads the sources and says what the plugin does and whether anything in it is dangerous.
// 1 and 2 refuse the submission. 3 runs after it is stored: an obfuscated verdict rejects it, the rest is advice
// shown on the review page and in the Discord message.

const SHORT_OK = new Set(['i','j','k','n','m','x','y','z','e','t','c','v','s','r','g','b','a','w','h','p','id','ok','in','on','to','at','dp','px','db','io','up','it','ex','el','fn','cb','os','is','ms','dt','dx','dy']);
const KEYWORDS = new Set(['if','for','while','switch','catch','return','new','else','do','try','synchronized','super','this','throw','case','assert']);
const HIDDEN_CODE = [
  [/\bDexClassLoader\b|\bInMemoryDexClassLoader\b|\bPathClassLoader\b|\bBaseDexClassLoader\b|\bDexFile\b/, 'загружает чужой код (DexClassLoader)'],
  [/\bdefineClass\s*\(/, 'создаёт классы на лету (defineClass)'],
  [/\bSystem\s*\.\s*(?:load|loadLibrary)\s*\(|\bRuntime\s*\.\s*getRuntime\s*\(\s*\)\s*\.\s*(?:load|loadLibrary)\s*\(/, 'загружает нативную библиотеку'],
  [/\bRuntime\s*\.\s*getRuntime\s*\(\s*\)\s*\.\s*exec\s*\(|\bProcessBuilder\b/, 'запускает команды системы'],
  [/\bjavax\s*\.\s*crypto\b[\s\S]*\bdoFinal\s*\(/, 'расшифровывает данные в коде'],
];
const DEX_HIDDEN = ['Ldalvik/system/DexClassLoader;','Ldalvik/system/InMemoryDexClassLoader;','Ldalvik/system/PathClassLoader;','Ldalvik/system/DexFile;','Ljava/lang/ProcessBuilder;'];

/** Comments and the insides of string and char literals blanked out, line breaks kept. */
function codeOnly(text){
  // text blocks ("""...""") first: they span lines, and their words are text, not code
  return text.replace(/"""[\s\S]*?"""|\/\*[\s\S]*?\*\/|\/\/[^\n]*|"(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*'/g,m=>m.startsWith('"""')?'""'+m.replace(/[^\n]/g,''):m.startsWith('"')?'""':m.startsWith("'")?"''":m.replace(/[^\n]/g,' '));
}
function literals(text){return [...text.matchAll(/"((?:\\.|[^"\\\n])*)"/g)].map(m=>m[1]);}

/** Reasons the sources look obfuscated or hide code; empty when they read like code a person wrote. */
export function inspectSources(files){
  const reasons=new Set();
  const names=[];
  let lines=0,chars=0;
  for(const file of files){
    const text=String(file.text||'');
    for(const line of text.split('\n')){lines++;chars+=line.length;if(line.length>1500)reasons.add('строки длиной в тысячи символов (минифицированный код): '+file.path);}
    if(/[^\x00-\x7F]/.test(codeOnly(text)))reasons.add('имена не латиницей (возможна подмена букв): '+file.path);
    const code=codeOnly(text);
    if((code.match(/\\u[0-9a-fA-F]{4}/g)||[]).length>=1)reasons.add('код спрятан в \\u-последовательностях: '+file.path);
    for(const m of code.matchAll(/\b(?:class|interface|enum|record)\s+([A-Za-z_$][\w$]*)/g))names.push(m[1]);
    for(const m of code.matchAll(/[\w>\]]\s+([A-Za-z_$][\w$]*)\s*\([^;{)]*\)\s*(?:throws\s+[\w.,\s]+)?\{/g))if(!KEYWORDS.has(m[1]))names.push(m[1]);
    for(const m of code.matchAll(/\b(?:final\s+)?(?:int|long|short|byte|char|float|double|boolean|var|String|Object|[A-Z][\w$]*)(?:<[^<>;]*>)?(?:\[\])*\s+([A-Za-z_$][\w$]*)\s*(?=[=;,):])/g))names.push(m[1]);
    // numbers or bytes by the hundred in one initializer: a payload, not logic
    for(const m of code.matchAll(/\{[\s\d,xXa-fA-FlL+\-()(byte)(char)]{1200,}\}/g))if((m[0].match(/,/g)||[]).length>200){reasons.add('в коде зашит большой массив данных: '+file.path);break;}
    if((code.match(/\(\s*char\s*\)\s*\(?[^;\n]*\^/g)||[]).length>=2)reasons.add('строки расшифровываются через XOR: '+file.path);
    for(const value of literals(text))if(value.length>600&&/^[A-Za-z0-9+/=_-]+$/.test(value)){reasons.add('длинная закодированная строка (base64): '+file.path);break;}
    for(const [pattern,why] of HIDDEN_CODE)if(pattern.test(code))reasons.add(why+': '+file.path);
  }
  if(lines>0&&chars/lines>220)reasons.add('средняя строка длиннее 220 символов — код сжат');
  const declared=names.filter(n=>n.length>0);
  if(declared.length>=8){
    const short=declared.filter(n=>n.length<=2&&!SHORT_OK.has(n.toLowerCase())).length;
    if(short/declared.length>0.4)reasons.add('большинство имён из одной-двух букв ('+short+' из '+declared.length+')');
  }
  const confusing=declared.filter(n=>/^[Il1|]{3,}$|^[O0o]{3,}$|^_{2,}$|^\$+[\w$]*\$$|^[Il1O0]{5,}$/.test(n));
  if(confusing.length>=2)reasons.add('похожие друг на друга имена вроде '+confusing.slice(0,3).join(', '));
  return [...reasons];
}

// ---------------------------------------------------------------- the package

const u16=(b,o)=>b[o]|b[o+1]<<8;
const u32=(b,o)=>(b[o]|b[o+1]<<8|b[o+2]<<16|b[o+3]<<24)>>>0;

/** The files of a ZIP (stored or deflated), up to 8 MB unpacked in all. */
export async function unzip(bytes){
  let end=-1;
  for(let i=bytes.length-22;i>=Math.max(0,bytes.length-65557);i--)if(u32(bytes,i)===0x06054b50){end=i;break;}
  if(end<0)throw new Error('archive_unreadable');
  const count=u16(bytes,end+10);let at=u32(bytes,end+16);
  const out=new Map();let total=0;
  for(let n=0;n<count;n++){
    if(u32(bytes,at)!==0x02014b50)throw new Error('archive_unreadable');
    const method=u16(bytes,at+10),packed=u32(bytes,at+20),size=u32(bytes,at+24),nameLength=u16(bytes,at+28),extra=u16(bytes,at+30),comment=u16(bytes,at+32),local=u32(bytes,at+42);
    const name=new TextDecoder().decode(bytes.subarray(at+46,at+46+nameLength));
    at+=46+nameLength+extra+comment;
    if(name.endsWith('/'))continue;
    total+=size;if(total>8*1048576)throw new Error('archive_too_large');
    const start=local+30+u16(bytes,local+26)+u16(bytes,local+28);
    const data=bytes.subarray(start,start+packed);
    if(method===0)out.set(name,data);
    else if(method===8){
      const stream=new Blob([data]).stream().pipeThrough(new DecompressionStream('deflate-raw'));
      out.set(name,new Uint8Array(await new Response(stream).arrayBuffer()));
    }else throw new Error('archive_unreadable');
  }
  return out;
}

function uleb(b,o){let value=0,shift=0,byte;do{byte=b[o++];value|=(byte&0x7f)<<shift;shift+=7;}while(byte&0x80&&shift<35);return [value>>>0,o];}

/** The classes a dex defines and every string in it (type names included). */
export function readDex(b){
  if(b.length<0x70||String.fromCharCode(...b.subarray(0,3))!=='dex')throw new Error('dex_unreadable');
  const stringsSize=u32(b,0x38),stringsAt=u32(b,0x3C),typesSize=u32(b,0x40),typesAt=u32(b,0x44),classesSize=u32(b,0x60),classesAt=u32(b,0x64);
  if(stringsSize>200000||typesSize>65536||classesSize>65536)throw new Error('dex_unreadable');
  const strings=new Array(stringsSize);
  for(let i=0;i<stringsSize;i++){
    let [,o]=uleb(b,u32(b,stringsAt+i*4));let end=o;while(end<b.length&&b[end]!==0)end++;
    strings[i]=new TextDecoder().decode(b.subarray(o,end));
  }
  const types=new Array(typesSize);for(let i=0;i<typesSize;i++)types[i]=strings[u32(b,typesAt+i*4)];
  const classes=[];for(let i=0;i<classesSize;i++)classes.push(types[u32(b,classesAt+i*32)]);
  return {classes,strings:new Set(strings)};
}

/** Top-level and nested type names the sources declare, as dex descriptors without the nesting suffixes. */
function sourceTypes(files){
  const out=new Set();
  for(const file of files){
    const code=codeOnly(String(file.text||''));
    const pkg=(code.match(/^\s*package\s+([\w.]+)\s*;/m)||[])[1]||'';
    for(const m of code.matchAll(/\b(?:class|interface|enum|record)\s+([A-Za-z_$][\w$]*)/g))out.add('L'+(pkg?pkg.replace(/\./g,'/')+'/':'')+m[1]);
  }
  return out;
}

/** Reasons the package does not match its sources; empty when it does. */
export async function inspectPackage(archive,files,meta){
  const reasons=[];
  let entries;try{entries=await unzip(archive);}catch(error){return ['пакет не читается: '+error.message];}
  // what archivers add on their own (META-INF of a signed or jarred package) is not the plugin's
  for(const name of [...entries.keys()])if(/^META-INF\//.test(name))entries.delete(name);
  for(const name of entries.keys())if(!/^(?:manifest\.json|classes\d*\.dex|icon\.(?:png|gif))$/.test(name))reasons.push('в пакете лишний файл: '+name+(name.includes('/')?' (файлы должны лежать в корне архива, без папки)':''));
  try{const manifest=JSON.parse(new TextDecoder().decode(entries.get('manifest.json')||new Uint8Array()));
    if(manifest.id!==meta.id||manifest.entry!==meta.entry)reasons.push('manifest.json пакета не совпадает с отправленным');}
  catch{reasons.push('в пакете нет manifest.json');}
  const declared=sourceTypes(files);
  let dexes=0;
  for(const [name,bytes] of entries){
    if(!/^classes\d*\.dex$/.test(name))continue;dexes++;
    let dex;try{dex=readDex(bytes);}catch{reasons.push(name+' не читается');continue;}
    for(const type of dex.classes){
      // Lpkg/Outer$Inner$1; and synthetic lambdas -> Lpkg/Outer; then each nesting level must be declared
      if(/^L\$r8\$|^Lj\$\//.test(type))continue; // D8's own backports, not the plugin's code
      const bare=type.replace(/;$/,'');const parts=bare.split('$').filter(Boolean);
      if(!declared.has(parts[0])){reasons.push('в dex есть класс '+type.slice(1,-1).replace(/\//g,'.')+', которого нет в присланных исходниках — пакет собран из другой версии кода или не все файлы .java приложены');if(reasons.length>8)break;}
    }
    for(const hidden of DEX_HIDDEN)if(dex.strings.has(hidden))reasons.push('dex загружает чужой код: '+hidden);
  }
  if(!dexes)reasons.push('в пакете нет classes.dex');
  return reasons;
}

// ---------------------------------------------------------------- Gemini

const MODELS=['gemini-2.5-flash','gemini-flash-latest','gemini-2.0-flash'];
const PROMPT=`Ты проверяешь плагин для мода ttcuz (Android, TikTok). Плагин — Java-класс, наследник TtcuzPlugin; через PluginContext он может менять интерфейс, текст, ленту, ходить в сеть, просить разрешения Android и вызывать скрытые методы TikTok через рефлексию.
Прочитай весь код и ответь строго JSON без пояснений вокруг:
{"verdict":"safe"|"suspicious"|"dangerous","obfuscated":true|false,"summary":"что делает плагин, 1-3 предложения по-русски","permissions":["какие возможности он реально использует"],"issues":["конкретные проблемы по-русски с именем файла"]}
dangerous — кража данных или токенов, скрытая отправка данных, загрузка чужого кода, вредные действия от имени пользователя, порча аккаунта. suspicious — непонятная сеть, лишние разрешения, рефлексия к аккаунту/сессии без нужды. obfuscated=true, если имена бессмысленны или код намеренно запутан (зашифрованные строки, сжатие, обход проверки).`;

/** Gemini's reading of the sources, or {verdict:'unknown'} when it cannot answer. Never throws. */
export async function geminiReview(env,meta,files,budget=45000){
  const until=Date.now()+budget;
  const key=env&&env.GEMINI_API_KEY;
  if(!key)return {verdict:'unknown',summary:'Gemini не настроен',issues:[]};
  let sources='';
  for(const file of files){const part='\n===== '+file.path+' =====\n'+file.text;if(sources.length+part.length>400000){sources+='\n[часть файлов не поместилась]';break;}sources+=part;}
  const body={contents:[{role:'user',parts:[{text:PROMPT+'\n\nmanifest: '+JSON.stringify(meta)+'\n'+sources}]}],
    generationConfig:{temperature:0,responseMimeType:'application/json'}};
  for(const model of [env.GEMINI_MODEL,...MODELS].filter(Boolean)){
    const left=until-Date.now();
    if(left<3000)return {verdict:'unknown',summary:'Gemini не успел ответить',issues:[]};
    try{
      const response=await fetch('https://generativelanguage.googleapis.com/v1beta/models/'+model+':generateContent',{
        method:'POST',headers:{'content-type':'application/json','x-goog-api-key':key},body:JSON.stringify(body),signal:AbortSignal.timeout(left)});
      if(response.status===404)continue;
      if(!response.ok)return {verdict:'unknown',summary:'Gemini ответил '+response.status,issues:[]};
      const answer=await response.json();
      const text=(answer?.candidates?.[0]?.content?.parts||[]).map(p=>p.text||'').join('');
      return normalise(JSON.parse(text.replace(/^```(?:json)?|```$/g,'').trim()),model);
    }catch(error){return {verdict:'unknown',summary:'Gemini недоступен: '+String(error?.message||error).slice(0,120),issues:[]};}
  }
  return {verdict:'unknown',summary:'нет доступной модели Gemini',issues:[]};
}

export function normalise(value,model){
  const list=v=>Array.isArray(v)?v.filter(x=>typeof x==='string').map(x=>x.slice(0,300)).slice(0,12):[];
  return {verdict:['safe','suspicious','dangerous'].includes(value?.verdict)?value.verdict:'unknown',obfuscated:value?.obfuscated===true,
    summary:typeof value?.summary==='string'?value.summary.slice(0,600):'',permissions:list(value?.permissions),issues:list(value?.issues),model:model||''};
}
