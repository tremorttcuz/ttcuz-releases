import test from 'node:test';
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { normalizeMusicList, normalizeStyle, profileRoute } from '../server/cloudflare/profiles.mjs';

const token = 'a'.repeat(64), hash = createHash('sha256').update(token).digest('hex');
const list = 'ttcuz-music 2\n123\tЗвук TikTok\tАвтор\thttps://sf16.example-cdn.com/obj/audio.mp3?signature=abc\n';
const style = {firstColour:0,lastColour:0,middleColour:0,direction:0,speed:50,offsetX:0,offsetY:0,badgeSize:100,bold:false,glow:false,animated:false,threeColours:false};
function database(shared = false) {
  const rows = new Map([['111',{owner_hash:hash,verified:1,metadata:JSON.stringify({musicOn:shared}),music:list}]]);
  return {rows,prepare(sql) { let args; return {bind(...a){args=a;return this;},
    async first() {
      if(sql.includes('INSERT INTO limits')) return {hits:1};
      const row=rows.get(args[0]);
      if(sql.startsWith('SELECT music ') && row?.owner_hash !== args[1]) return null;
      return row || null;
    },async run() {
      if(sql.startsWith('UPDATE profiles SET music=')) {
        const row=rows.get(args[1]);if(row?.owner_hash !== args[2])return {meta:{changes:0}};
        row.music=args[0];return {meta:{changes:1}};
      }
      if(sql.startsWith('UPDATE profiles SET metadata=NULL')) {
        const row=rows.get(args[1]);if(row){row.metadata=null;if(sql.includes('music=NULL'))row.music=null;}
      }
      return {meta:{changes:1}};
    }};}};
}
function post(route, body) { return new Request('https://example.com/v1/music/'+route,{method:'POST',body:JSON.stringify(body)}); }
test('text format preserves signed links and all 200 tracks without binary payloads', () => {
  assert.equal(normalizeMusicList(list),list);
  assert.equal(normalizeMusicList('ttcuz-music 1\n7\tOld\tAuthor\n'),'ttcuz-music 2\n7\tOld\tAuthor\t\n');
  const link='https://sf16.example-cdn.com/obj/'+ 'x'.repeat(1600);
  const text='ttcuz-music 2\n'+Array.from({length:200},(_,i)=>`${i}\tТрек\tАвтор\t${link}\n`).join('');
  assert.equal(normalizeMusicList(text),text);
  for(const invalid of [list+list.split('\n')[1]+'\n','data:audio/mp3;base64,AAAA','ttcuz-music 2\n1\ta\tb\tfile:///x\n',
    'ttcuz-music 2\n1\ta\tb\thttps://127.0.0.1/x\n','ttcuz-music 2\n1\ta\tb\thttps://user@a.example.com/x\n',
    'ttcuz-music 2\n1\ta\tb\thttps://a.example.com:99999/x\n']) assert.throws(()=>normalizeMusicList(invalid));
});

test('v3 preserves real cover URLs with audio references and rejects binary image payloads',async()=>{
 const cover='https://p16.example-cdn.com/tos/cover.jpeg?signature=original';
 const text='ttcuz-music 3\n123\tЗвук TikTok\tАвтор\thttps://sf16.example-cdn.com/audio.mp3\t'+cover+'\n';
 assert.equal(normalizeMusicList(text),text);
 const db=database(true),env={PROFILES:db};
 assert.equal((await profileRoute(post('save',{uid:'111',token,list:text}),env)).status,200);
 assert.equal((await(await profileRoute(new Request('https://example.com/v1/music/111'),env)).json()).list,text);
 for(const invalid of ['data:image/png;base64,AAAA','http://cdn.example.com/a.png','https://127.0.0.1/a.png'])assert.throws(()=>normalizeMusicList(text.replace(cover,invalid)));
 const long='https://p16.example-cdn.com/'+ 'x'.repeat(1950);
 const full='ttcuz-music 3\n'+Array.from({length:200},(_,i)=>`${i}\tТрек\tАвтор\t${long}\t${long}\n`).join('');
 assert.equal(normalizeMusicList(full),full);assert.equal((await profileRoute(post('save',{uid:'111',token,list:full}),env)).status,200);
});
test('owner saves and restores text; another device cannot read the private list', async () => {
  const db=database();const env={PROFILES:db};
  const changed=list.replace('Звук TikTok','Новый звук');
  assert.equal((await profileRoute(post('save',{uid:'111',token,list:changed}),env)).status,200);
  assert.equal(db.rows.get('111').music,changed);
  const loaded=await profileRoute(post('load',{uid:'111',token}),env);
  assert.equal((await loaded.json()).list,changed);
  assert.equal((await profileRoute(post('load',{uid:'111',token:'b'.repeat(64)}),env)).status,403);
  assert.equal((await profileRoute(post('save',{uid:'111',token,list:'data:audio/mp3;base64,AAAA'}),env)).status,400);
  assert.equal(db.rows.get('111').music,changed);
});
test('public playback is gated by explicit sharing and closes when sharing is disabled', async () => {
  const db=database();const env={PROFILES:db};const request=()=>new Request('https://example.com/v1/music/111');
  assert.equal((await profileRoute(request(),env)).status,404);
  db.rows.get('111').metadata=JSON.stringify({musicOn:true});
  assert.equal((await (await profileRoute(request(),env)).json()).list,list);
  db.rows.get('111').metadata=JSON.stringify({musicOn:false});
  assert.equal((await profileRoute(request(),env)).status,404);
  db.rows.get('111').metadata=null;
  assert.equal((await profileRoute(request(),env)).status,404);
});
test('long text bodies are accepted for music only, while other endpoints retain their limit', async () => {
  const db=database();const env={PROFILES:db};
  const big='ttcuz-music 2\n'+Array.from({length:200},(_,i)=>`${i}\tТрек\tАвтор\thttps://cdn.example.com/${'x'.repeat(1600)}\n`).join('');
  assert.equal((await profileRoute(post('save',{uid:'111',token,list:big}),env)).status,200);
  assert.equal(db.rows.get('111').music,big);
  const oversize=new Request('https://example.com/v1/publish',{method:'POST',body:'x'.repeat(70000)});
  assert.equal((await profileRoute(oversize,env)).status,413);
});
test('removing public appearance closes playback without deleting the private backup', async () => {
  const db=database(true),env={PROFILES:db};
  const remove=new Request('https://example.com/v1/remove',{method:'POST',body:JSON.stringify({uid:'111',token})});
  assert.equal((await profileRoute(remove,env)).status,200);
  assert.equal((await profileRoute(new Request('https://example.com/v1/music/111'),env)).status,404);
  assert.equal((await (await profileRoute(post('load',{uid:'111',token}),env)).json()).list,list);
});
test('layout is optional for older clients and strictly bounded for new clients', () => {
  assert.equal(normalizeStyle(style,'111',42).profileLayout,0);
  for(const profileLayout of [0,1,2])assert.equal(normalizeStyle({...style,profileLayout},'111',42).profileLayout,profileLayout);
  for(const profileLayout of [-1,3,1.5,'1',null]) {
    if(profileLayout===null)continue;
    assert.throws(()=>normalizeStyle({...style,profileLayout},'111',42));
  }
});

test('public player position and bounded appearance round trip without exposing private keys',()=>{
  const look={music_slider:1,music_button_shape:3,music_glow:true,music_glow_speed:95,music_accent:-1250069};
  const result=normalizeStyle({...style,musicFx:120,musicFy:930,musicAccent:-1250069,musicLook:look},'111',42);
  assert.equal(result.musicFx,120);assert.equal(result.musicFy,930);assert.deepEqual(result.musicLook,look);
  assert.equal(normalizeStyle(style,'111',42).musicFx,1000);
  for(const bad of [{musicFx:-1},{musicFy:1001},{musicFx:1.5},{musicLook:{token:'private'}},{musicLook:{music_slider:5}},{musicLook:{music_glow:'true'}},{musicLook:{music_pill:300}},{musicLook:[]}])assert.throws(()=>normalizeStyle({...style,...bad},'111',42));
});
test('a visitor cannot publish a new player position using another owner token',async()=>{
  const env={PROFILES:database(true)};
  const request=new Request('https://example.com/v1/publish',{method:'POST',body:JSON.stringify({uid:'111',token:'b'.repeat(64),style:{...style,musicFx:0,musicFy:0}})});
  assert.equal((await profileRoute(request,env)).status,403);
});
