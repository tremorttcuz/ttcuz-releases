// A real (if tiny) .ctt for server tests: a stored ZIP holding manifest.json and a dex that defines the given classes.
const enc=new TextEncoder();
const le=(n,size)=>{const b=new Uint8Array(size);for(let i=0;i<size;i++)b[i]=(n>>>(8*i))&255;return b;};
const join=parts=>{const out=new Uint8Array(parts.reduce((n,p)=>n+p.length,0));let at=0;for(const p of parts){out.set(p,at);at+=p.length;}return out;};

/** A dex with only what the server reads: strings, types and class definitions. */
export function makeDex(classes,extraStrings=[]){
  const strings=[...new Set([...classes,...extraStrings])].sort();
  const header=0x70,stringIds=header,typeIds=stringIds+strings.length*4,classDefs=typeIds+classes.length*4,data=classDefs+classes.length*32;
  const datas=[];let at=data;const offsets=[];
  for(const s of strings){offsets.push(at);const bytes=join([new Uint8Array([s.length&0x7f]),enc.encode(s),new Uint8Array([0])]);datas.push(bytes);at+=bytes.length;}
  const head=new Uint8Array(header);head.set(enc.encode('dex\n035\0'));
  head.set(le(strings.length,4),0x38);head.set(le(stringIds,4),0x3C);head.set(le(classes.length,4),0x40);head.set(le(typeIds,4),0x44);
  head.set(le(classes.length,4),0x60);head.set(le(classDefs,4),0x64);
  const ids=join(offsets.map(o=>le(o,4)));
  const types=join(classes.map(c=>le(strings.indexOf(c),4)));
  const defs=join(classes.map((c,i)=>join([le(i,4),new Uint8Array(28)])));
  return join([head,ids,types,defs,...datas]);
}

/** A ZIP with every entry stored (no compression). */
export function makeZip(entries){
  const locals=[],centrals=[];let offset=0;
  for(const [name,data] of Object.entries(entries)){
    const n=enc.encode(name),body=typeof data==='string'?enc.encode(data):data;
    const local=join([le(0x04034b50,4),le(20,2),le(0,2),le(0,2),le(0,4),le(0,4),le(body.length,4),le(body.length,4),le(n.length,2),le(0,2),n,body]);
    centrals.push(join([le(0x02014b50,4),le(20,2),le(20,2),le(0,2),le(0,2),le(0,4),le(0,4),le(body.length,4),le(body.length,4),le(n.length,2),le(0,2),le(0,2),le(0,2),le(0,2),le(0,4),le(offset,4),n]));
    locals.push(local);offset+=local.length;
  }
  const central=join(centrals);
  const end=join([le(0x06054b50,4),le(0,2),le(0,2),le(centrals.length,2),le(centrals.length,2),le(central.length,4),le(offset,4),le(0,2)]);
  return join([...locals,central,end]);
}

export const base64=bytes=>btoa(String.fromCharCode(...bytes));
export function makeCtt(id,entry,classes,extra={},manifest={}){
  return makeZip({'manifest.json':JSON.stringify({id,entry,name:id,api:1,...manifest}),'classes.dex':makeDex(classes),...extra});
}
