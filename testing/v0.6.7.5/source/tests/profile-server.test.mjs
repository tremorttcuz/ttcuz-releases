import test from 'node:test';
import assert from 'node:assert/strict';
import { CREATOR_UID, findOwner, findPublicUid, normalizeStyle, challenge, digest, awardedBadges, profileRoute } from '../server/cloudflare/profiles.mjs';
import worker from '../server/cloudflare/worker.mjs';

test('proof must occur in the exact profile biography', () => {
  const owner = { id: '123', uniqueId: 'owner', signature: 'hello\nttcuz-code' };
  assert.equal(findOwner({ user: owner }, '123', 'owner', 'ttcuz-code'), true);
  assert.equal(findOwner({ user: owner }, '999', 'owner', 'ttcuz-code'), false);
  assert.equal(findOwner({ user: owner }, '123', 'other', 'ttcuz-code'), false);
  assert.equal(findOwner({ user: {...owner, signature:'prefix-ttcuz-code'} }, '123', 'owner', 'ttcuz-code'), false);
  assert.equal(findOwner({ user: {...owner, signature:''}, video:{ text:'ttcuz-code' } }, '123', 'owner', 'ttcuz-code'), false);
});
test('public profile lookup matches the exact handle and numeric UID', () => {
  assert.equal(findPublicUid({UserModule:{users:{one:{id:123,uniqueId:'trem0r1_1'}}}},'trem0r1_1'),'123');
  assert.equal(findPublicUid({id:'123',uniqueId:'not-the-handle'},'trem0r1_1'),null);
  assert.equal(findPublicUid({id:'not-numeric',uniqueId:'trem0r1_1'},'trem0r1_1'),null);
});
const style = { firstColour:-1,lastColour:-1,middleColour:0,direction:0,speed:50,
  offsetX:0,offsetY:0,badgeSize:100,threeColours:false,bold:false,glow:false,animated:false };
test('style validation preserves disabled gradient and rejects unbounded values', () => {
  assert.equal(normalizeStyle({...style,firstColour:0,lastColour:0},'123',42).firstColour,0);
  assert.equal(normalizeStyle({...style,firstColour:0,lastColour:0},'123',42).crownOffsetX,0);
  for (const change of [{speed:101},{direction:4},{offsetX:1000},{badgeSize:0},{bold:'true'},{firstColour:Infinity}]) {
    assert.throws(()=>normalizeStyle({...style,...change},'123',42));
  }
  assert.throws(()=>normalizeStyle({...style,crownOffsetY:101},'123',42));
});
test('proof changes when UID or device secret changes', async () => {
  const code = await challenge('123','a'.repeat(64));
  assert.match(code,/^ttcuz-[a-f0-9]{16}$/);
  assert.notEqual(code,await challenge('124','a'.repeat(64)));
  assert.notEqual(code,await challenge('123','b'.repeat(64)));
});
test('public writes cannot change profiles without ownership proof', async () => {
  const database = { prepare(sql) { return { bind() { return this; },
    async first() { return sql.includes('limits') ? {hits:1} : {owner_hash:'another-owner'}; },
    async run() { return {meta:{changes:1}}; } }; } };
  const request = new Request('https://example.com/v1/publish', {
    method:'POST', body:JSON.stringify({uid:'123',token:'a'.repeat(64),style}),
  });
  const result = await profileRoute(request,{PROFILES:database});
  assert.equal(result.status,403);
  assert.equal((await result.json()).error,'verify_required');
});
test('missing storage is reported and unrelated pages stay untouched', async () => {
  assert.equal(await profileRoute(new Request('https://example.com/guide.html'),{}),null);
  assert.equal((await profileRoute(new Request('https://example.com/v1/profiles/123'),{})).status,503);
});

function profileHtml(user) {
  return '<html><script id="__UNIVERSAL_DATA_FOR_REHYDRATION__" type="application/json">'
    + JSON.stringify({UserModule:{users:{[user.uniqueId]:user}}})
    + '</script></html>';
}

test('profile proof follows TikTok redirects and verifies the exact public bio', async () => {
  const token = 'e'.repeat(64), uid = '7491898648855512119';
  const code = await challenge(uid, token);
  const database = memoryD1(null);
  const previous = globalThis.fetch;
  let requests = 0;
  globalThis.fetch = async (_url, options) => {
    requests++;
    assert.equal(options.redirect, 'follow');
    assert.equal(options.cache, 'no-store');
    assert.equal(options.headers['Cache-Control'], 'no-cache');
    assert.match(_url, /ttcuz_verify=/);
    return new Response(profileHtml({id:uid, uniqueId:'trem0r1_1',
      signature:'мой профиль\n' + code}), {status:200});
  };
  try {
    const response = await profileRoute(new Request('https://example.com/v1/verify', {
      method:'POST', body:JSON.stringify({uid, token, handle:'trem0r1_1'}),
    }), {PROFILES:database});
    assert.equal(response.status,200);
    assert.deepEqual(await response.json(),{ok:true});
    assert.equal(requests,1);
  } finally { globalThis.fetch = previous; }
});

test('badge lookup follows redirects and resolves the handle to its real UID', async () => {
  const token = 'f'.repeat(64), database = memoryD1(await digest(token));
  const previous = globalThis.fetch;
  globalThis.fetch = async (_url, options) => {
    assert.equal(options.redirect, 'follow');
    return new Response(profileHtml({id:'123456', uniqueId:'friend_name', signature:''}),
      {status:200});
  };
  try {
    const response = await badgeWrite(database,token,'/v1/badges/grant',
      {badgeId:'community',targetHandle:'@friend_name'});
    assert.equal(response.status,200);
    assert.equal((await response.json()).changed,true);
    assert.deepEqual(await awardedBadges({PROFILES:database}),[{
      id:'community',colour:'#8DD1B0',image:'',title:'Отличие ttcuz',
      text:'Значок выдан создателем ttcuz',button:'Профиль TikTok',users:['123456'],
    }]);
  } finally { globalThis.fetch = previous; }
});

function memoryD1(ownerHash) {
  const awards = new Map();
  return {
    awards,
    prepare(sql) {
      let values = [];
      return {
        bind(...args) { values = args; return this; },
        async first() {
          if (sql.includes('INSERT INTO limits')) return {hits:1};
          if (sql.includes('SELECT owner_hash')) return ownerHash ? {owner_hash:ownerHash,verified:1} : null;
          return null;
        },
        async all() {
          return {results:Array.from(awards, ([key, awarded_at]) => {
            const [uid,badge_id] = key.split(':'); return {uid,badge_id,awarded_at};
          })};
        },
        async run() {
          if (sql.includes('INSERT OR IGNORE INTO badge_awards')) {
            const [uid,badge_id,awarded_at] = values;
            const key = uid + ':' + badge_id;
            const changed = !awards.has(key);
            if (changed) awards.set(key,awarded_at);
            return {meta:{changes:changed?1:0}};
          }
          if (sql.includes('DELETE FROM badge_awards')) {
            const changed = awards.delete(values[0] + ':' + values[1]);
            return {meta:{changes:changed?1:0}};
          }
          return {meta:{changes:1}};
        },
      };
    },
  };
}

async function badgeWrite(database, token, path, extra = {}) {
  return profileRoute(new Request('https://example.com' + path, {
    method:'POST', body:JSON.stringify({uid:'7491898648855512119',token,...extra}),
  }), {PROFILES:database});
}

test('creator-only badge grants are readable, idempotent, and revocable', async () => {
  const token = 'c'.repeat(64);
  const database = memoryD1(await digest(token));
  const grant = await badgeWrite(database,token,'/v1/badges/grant',
    {badgeId:'community',targetUid:'123456'});
  assert.equal(grant.status,200);
  assert.equal((await grant.json()).changed,true);
  assert.deepEqual(await awardedBadges({PROFILES:database}),[{
    id:'community',colour:'#8DD1B0',image:'',title:'Отличие ttcuz',
    text:'Значок выдан создателем ttcuz',button:'Профиль TikTok',users:['123456'],
  }]);
  const duplicate = await badgeWrite(database,token,'/v1/badges/grant',
    {badgeId:'community',targetUid:'123456'});
  assert.equal((await duplicate.json()).changed,false);
  const remove = await badgeWrite(database,token,'/v1/badges/revoke',
    {badgeId:'community',targetUid:'123456'});
  assert.equal((await remove.json()).changed,true);
  assert.deepEqual(await awardedBadges({PROFILES:database}),[]);
});

test('badge grants reject an unverified caller and unknown badge', async () => {
  const token = 'd'.repeat(64);
  const database = memoryD1(await digest('not-the-device-token'));
  const denied = await badgeWrite(database,token,'/v1/badges/grant',
    {badgeId:'community',targetUid:'123456'});
  assert.equal(denied.status,403);
  const authorized = memoryD1(await digest(token));
  const self = await badgeWrite(authorized,token,'/v1/badges/grant',
    {badgeId:'community',targetUid:'7491898648855512119'});
  assert.equal(self.status,200);
  const unknown = await badgeWrite(authorized,token,'/v1/badges/grant',
    {badgeId:'arbitrary',targetUid:'123456'});
  assert.equal(unknown.status,400);
});

test('public badges endpoint merges creator and awarded badges', async () => {
  const database = {prepare() { return {async all() { return {results:[
    {badge_id:'community',uid:'333',awarded_at:1},
  ]}; }}; }};
  const response = await worker.fetch(new Request('https://example.com/badges'),
    {PROFILES:database});
  const body = await response.json();
  assert.equal(body.badges.length,2);
  assert.equal(body.badges[0].id,'creator_trem0r1_1');
  assert.deepEqual(body.badges[1].users,['333']);
});

test('the first device publishes colours without a bio code, but the badge image waits', async () => {
  const rows = new Map();
  const database = { prepare(sql) { let v = []; return { bind(...a) { v = a; return this; },
    async first() {
      if (sql.includes('INSERT INTO limits')) return {hits:1};
      if (sql.includes('SELECT owner_hash')) return rows.get(v[0]) || null;
      return null; },
    async run() {
      if (sql.includes('INSERT OR IGNORE INTO profiles') && !rows.has(v[0])) rows.set(v[0],{owner_hash:v[1],verified:0});
      return {meta:{changes:1}}; } }; } };
  const send = (uid, token) => profileRoute(new Request('https://example.com/v1/publish', {
    method:'POST', body:JSON.stringify({uid,token,style}) }), {PROFILES:database});
  assert.equal((await send('555','a'.repeat(64))).status,200);
  assert.equal((await send('555','a'.repeat(64))).status,200);
  assert.equal((await send('555','b'.repeat(64))).status,403);
  assert.equal((await send('7491898648855512119','a'.repeat(64))).status,403);
});

test('the creator can grant any badge from the catalog, and unknown ones are refused', async () => {
  const token = 'c'.repeat(64), database = memoryD1(await digest(token));
  const previous = globalThis.fetch;
  globalThis.fetch = async () => new Response(profileHtml({id:'123456', uniqueId:'friend_name', signature:''}), {status:200});
  try {
    const star = await badgeWrite(database,token,'/v1/badges/grant',{badgeId:'star',targetHandle:'@friend_name'});
    assert.equal(star.status,200);
    const unknown = await badgeWrite(database,token,'/v1/badges/grant',{badgeId:'nope',targetHandle:'@friend_name'});
    assert.equal(unknown.status,400);
    const shown = await awardedBadges({PROFILES:database});
    assert.equal(shown[0].id,'star');
    assert.equal(shown[0].image,'badges/star.png');
  } finally { globalThis.fetch = previous; }
});
test('the badge catalog is public and lists pictures the app may fetch', async () => {
  const response = await profileRoute(new Request('https://example.com/v1/badge-catalog'),{});
  assert.equal(response.status,200);
  const { badges } = await response.json();
  assert.ok(badges.length >= 5);
  for (const b of badges) assert.match(b.image, /^(badges\/[a-z0-9_-]+\.png)?$/);
});

function customD1(ownerHash) {
  const base = memoryD1(ownerHash), customs = new Map(), original = base.prepare;
  base.customs = customs;
  base.prepare = sql => {
    if (!sql.includes('custom_badges')) return original(sql);
    let values = [];
    return {
      bind(...v) { values = v; return this; },
      async first() { return customs.get(values[0]) || null; },
      async all() { return {results:[...customs.values()]}; },
      async run() {
        const [id,title,png,created_at] = values;
        customs.set(id,{id,title,png,created_at}); return {meta:{changes:1}};
      },
    };
  };
  return base;
}
const tinyPng = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+j4ZkAAAAASUVORK5CYII=';

test('custom badge upload, public image, grant and revoke preserve original colours', async () => {
  const token = '8'.repeat(64), database = customD1(await digest(token));
  const upload = await badgeWrite(database,token,'/v1/badges/create',{title:'Мой знак',badge:tinyPng});
  assert.equal(upload.status,200);
  const {badgeId} = await upload.json(); assert.match(badgeId,/^custom_[a-f0-9]{20}$/);
  const list = await profileRoute(new Request('https://example.com/v1/badge-catalog'),{PROFILES:database});
  const custom = (await list.json()).badges.find(b => b.id === badgeId);
  assert.equal(custom.title,'Мой знак'); assert.equal(custom.colour,'');
  const image = await profileRoute(new Request('https://example.com/' + custom.image),{PROFILES:database});
  assert.equal(image.headers.get('content-type'),'image/png');
  assert.deepEqual(Buffer.from(await image.arrayBuffer()),Buffer.from(tinyPng,'base64'));
  const head = await profileRoute(new Request('https://example.com/' + custom.image,{method:'HEAD'}),{PROFILES:database});
  assert.equal((await head.arrayBuffer()).byteLength,0);
  const grant = await badgeWrite(database,token,'/v1/badges/grant',{badgeId,targetUid:'345678'});
  assert.equal(grant.status,200); const awarded = await awardedBadges({PROFILES:database});
  assert.equal(awarded[0].colour,''); assert.equal(awarded[0].image,custom.image);
  assert.deepEqual(awarded[0].users,['345678']);
  await badgeWrite(database,token,'/v1/badges/revoke',{badgeId,targetUid:'345678'});
  assert.deepEqual(await awardedBadges({PROFILES:database}),[]);
});

test('custom uploads require creator proof, bounded PNG and a valid title', async () => {
  const token = '9'.repeat(64), database = customD1(await digest(token));
  const denied = await badgeWrite(database,'a'.repeat(64),'/v1/badges/create',{title:'A',badge:tinyPng});
  assert.equal(denied.status,403); assert.equal(database.customs.size,0);
  for (const invalid of [{title:'',badge:tinyPng},{title:'a'.repeat(41),badge:tinyPng},
    {title:'A',badge:'dGVzdA=='},{title:'A',badge:Buffer.alloc(33000).toString('base64')}]) {
    assert.equal((await badgeWrite(database,token,'/v1/badges/create',invalid)).status,400);
  }
  const first = await badgeWrite(database,token,'/v1/badges/create',{title:'Первая',badge:tinyPng});
  const again = await badgeWrite(database,token,'/v1/badges/create',{title:'Вторая',badge:tinyPng});
  assert.equal((await first.json()).badgeId,(await again.json()).badgeId);
  assert.equal(database.customs.size,1);
});

test('blocked nickname lookup reports the real failure and UID grants still work', async () => {
  const token = '7'.repeat(64), database = memoryD1(await digest(token)), previous = globalThis.fetch;
  globalThis.fetch = async () => new Response('blocked',{status:403});
  try {
    const failed = await badgeWrite(database,token,'/v1/badges/grant',{badgeId:'star',targetHandle:'correct_name'});
    assert.equal(failed.status,502); assert.equal((await failed.json()).error,'target_lookup_unavailable');
    const direct = await badgeWrite(database,token,'/v1/badges/grant',{badgeId:'star',targetUid:'123456'});
    assert.equal(direct.status,200);
  } finally { globalThis.fetch = previous; }
});

test('profile ids never lose precision and alternate profile keys are accepted', () => {
  assert.equal(findPublicUid({uid:'12345678901234567890',unique_id:'friend'},'FRIEND'),'12345678901234567890');
  assert.equal(findPublicUid({id:12345678901234567890,uniqueId:'friend'},'friend'),null);
});

 test('resolved UID remains authoritative when TikTok blocks the accompanying handle',async()=>{
 const token='8'.repeat(64),database=memoryD1(await digest(token)),previous=globalThis.fetch;let calls=0;
 globalThis.fetch=async()=>{calls++;return new Response('blocked',{status:403});};
 try{const response=await badgeWrite(database,token,'/v1/badges/grant',{badgeId:'star',targetUid:'987654',targetHandle:'known_friend'});assert.equal(response.status,200);assert.equal(calls,0);}
 finally{globalThis.fetch=previous;}
 });

 test('verified administrator may grant and revoke ordinary badges on own profile',async()=>{
 const token='8'.repeat(64),database=memoryD1(await digest(token));
 const grant=await badgeWrite(database,token,'/v1/badges/grant',{badgeId:'star',targetUid:CREATOR_UID});
 assert.equal(grant.status,200);
 const revoke=await badgeWrite(database,token,'/v1/badges/revoke',{badgeId:'star',targetUid:CREATOR_UID});
 assert.equal(revoke.status,200);
 });

 test('shared display name survives normalization; invalid names rejected',()=>{
 const style={firstColour:0,lastColour:0,middleColour:0,direction:0,speed:50,offsetX:0,offsetY:0,badgeSize:100,threeColours:false,bold:false,glow:false,animated:false};
 assert.equal(normalizeStyle({...style,displayName:' Имя 🌸 '},'123',42).displayName,'Имя 🌸');
 assert.equal(normalizeStyle(style,'123',42).displayName,'');
 for(const displayName of ['a'.repeat(81),'bad\nname',42])assert.throws(()=>normalizeStyle({...style,displayName},'123',42));
 });

test('gradient geometry is shared with bounded defaults for older clients',()=>{
 const style={firstColour:0,lastColour:0,middleColour:0,direction:0,speed:50,offsetX:0,offsetY:0,badgeSize:100,threeColours:false,bold:false,glow:false,animated:false};
 const old=normalizeStyle(style,'123',42);assert.equal(old.gradientWidth,100);assert.equal(old.gradientStop,50);
 const custom=normalizeStyle({...style,gradientWidth:175,gradientStop:25},'123',42);assert.equal(custom.gradientWidth,175);assert.equal(custom.gradientStop,25);
 for(const options of [{gradientWidth:49},{gradientWidth:201},{gradientWidth:100.5},{gradientStop:4},{gradientStop:96}])assert.throws(()=>normalizeStyle({...style,...options},'123',42));
});
