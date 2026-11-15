import {recoverDevice} from './device.mjs';
const UID = /^[0-9]{1,24}$/;
const TOKEN = /^[a-f0-9]{64}$/;
export const CREATOR_UID = '7491898648855512119';
export const COMMUNITY_BADGE_ID = 'community';
// The badges the creator can hand out. Adding one is a PNG in public/badges/
// and a line here: the app reads this list from the server, so it needs no update.
export const BADGE_CATALOG = [
  { id: 'community', title: 'Отличие ttcuz', colour: '#8DD1B0', image: '' },
  { id: 'star',    title: 'Звезда',   colour: '#FFD54F', image: 'badges/star.png' },
  { id: 'heart',   title: 'Сердце',   colour: '#F06292', image: 'badges/heart.png' },
  { id: 'bolt',    title: 'Молния',   colour: '#FFB300', image: 'badges/bolt.png' },
  { id: 'diamond', title: 'Алмаз',    colour: '#4FC3F7', image: 'badges/diamond.png' },
  { id: 'check',   title: 'Галочка',  colour: '#64B5F6', image: 'badges/check.png' },
  { id: 'shield',  title: 'Щит',      colour: '#90A4AE', image: 'badges/shield.png' },
  { id: 'flower',  title: 'Цветок',   colour: '#F8BBD0', image: 'badges/flower.png' },
  { id: 'moon',    title: 'Луна',     colour: '#B39DDB', image: 'badges/moon.png' },
  { id: 'note',    title: 'Нота',     colour: '#81C784', image: 'badges/note.png' },
];
const BADGE_IDS = new Map(BADGE_CATALOG.map(b => [b.id, b]));
async function customCatalog(env) {
  if (!env.PROFILES) return [];
  try {
    const result = await env.PROFILES.prepare('SELECT id,title FROM custom_badges ORDER BY created_at LIMIT 190').all();
    return (result.results || []).filter(row => /^custom_[a-f0-9]{20}$/.test(row.id || '')
      && typeof row.title === 'string').map(row => ({id:row.id,title:row.title,
        colour:'',image:'badges/' + row.id + '.png'}));
  } catch { return []; }
}
async function catalog(env) { return [...BADGE_CATALOG, ...await customCatalog(env)]; }
const reply = (body, status = 200) => Response.json(body, {
  status, headers: { 'cache-control': 'no-store', 'access-control-allow-origin': '*' },
});
export async function digest(value) {
  const hash = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value));
  return Array.from(new Uint8Array(hash), n => n.toString(16).padStart(2, '0')).join('');
}
export async function challenge(uid, token) {
  return 'ttcuz-' + (await digest('verify:' + uid + ':' + token)).slice(0, 16);
}

// Only trust the user object itself, never unrelated text elsewhere on the page.
function codeIn(text, code) {
  for (let at = text.indexOf(code); at >= 0; at = text.indexOf(code, at + 1)) {
    const before = text[at - 1], after = text[at + code.length];
    if (!(before && /[A-Za-z0-9-]/.test(before)) && !(after && /[A-Za-z0-9-]/.test(after))) return true;
  }
  return false;
}
export function findOwner(value, uid, handle, code, depth = 0) {
  if (!value || typeof value !== 'object' || depth > 24) return false;
  if (String(value.id) === uid && typeof value.uniqueId === 'string'
      && value.uniqueId.toLowerCase() === handle.toLowerCase()
      && typeof value.signature === 'string'
      // the code as a word of its own: an emoji or a full stop may touch it, a letter, digit or dash may not
      && codeIn(value.signature, code)) return true;
  return Object.values(value).some(v => findOwner(v, uid, handle, code, depth + 1));
}
export function findPublicUid(value, handle, depth = 0) {
  if (!value || typeof value !== 'object' || depth > 24) return null;
  const rawId = value.id ?? value.uid;
  const id = rawId == null || (typeof rawId === 'number' && !Number.isSafeInteger(rawId)) ? '' : String(rawId);
  const nickname = value.uniqueId ?? value.unique_id;
  if (UID.test(id)
      && typeof nickname === 'string'
      && nickname.toLowerCase() === handle.toLowerCase()) return id;
  for (const child of Object.values(value)) {
    const found = findPublicUid(child, handle, depth + 1);
    if (found) return found;
  }
  return null;
}
function scriptJson(text) {
  try { return JSON.parse(text); } catch { /* TikTok can wrap the object in an assignment. */ }
  const first = text.indexOf('{'), last = text.lastIndexOf('}');
  if (first < 0 || last <= first) return null;
  try { return JSON.parse(text.slice(first, last + 1)); } catch { return null; }
}
async function lookupProfile(handle) {
  for(const url of ['https://www.tiktok.com/@'+encodeURIComponent(handle),'https://www.tiktok.com/embed/@'+encodeURIComponent(handle),'https://www.tiktok.com/api/user/detail/?uniqueId='+encodeURIComponent(handle)]) {
    try {const found=await lookupProfilePage(handle,url);if(found)return found;}catch {}
  }
  return null;
}
async function lookupProfilePage(handle,url) {
  const result = await fetch(url, {
    // TikTok routinely redirects public profile URLs. Rejecting redirects made
    // valid profiles look unreachable, including for badge grants by handle.
    redirect: 'follow', signal: AbortSignal.timeout(10000),
    headers: { 'User-Agent': 'Mozilla/5.0', 'Accept': 'text/html' },
  });
  if (!result.ok) return null;
  const reader = result.body.getReader();
  const chunks = []; let total = 0;
  for (;;) {
    const { value, done } = await reader.read();
    if (done) break;
    total += value.byteLength;
    if (total > 2 * 1024 * 1024) { await reader.cancel(); return null; }
    chunks.push(value);
  }
  const bytes = new Uint8Array(total); let offset = 0;
  for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.length; }
  const html = new TextDecoder().decode(bytes);
  const json=scriptJson(html);if(json){const found=findPublicUid(json,handle);if(found)return found;}
  for (const match of html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/gi)) {
    const parsed = scriptJson(match[1]);
    if (parsed) { const found = findPublicUid(parsed, handle); if (found) return found; }
  }
  return null;
}
/** The headers of an ordinary phone browser: with a bare user agent TikTok often answers with an empty shell. */
const BROWSER = {
  'User-Agent': 'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36',
  'Accept': 'text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8',
  'Accept-Language': 'en-US,en;q=0.9,ru;q=0.8',
  'Cache-Control': 'no-cache', 'Pragma': 'no-cache',
};
/**
 * Whether the account's public bio carries the code. TikTok serves a profile from several places and caches each for
 * a while, so a fresh bio may show on one before another: each is asked in turn, past any cache, until one has it.
 * The code must sit on the object whose id is this uid, so a code in somebody else's bio proves nothing.
 */
async function verifyOwner(uid, handle, code) {
  const at = encodeURIComponent(handle), bust = Date.now();
  const places = [
    // the embed card: it carries the profile (id, uniqueId, signature) where the profile page itself answers a
    // server with an empty anti-bot shell
    'https://www.tiktok.com/embed/@' + at + '?ttcuz_verify=' + bust,
    'https://www.tiktok.com/@' + at + '?ttcuz_verify=' + bust,
    'https://www.tiktok.com/@' + at + '?lang=en&ttcuz_verify=' + bust,
    'https://m.tiktok.com/@' + at + '?ttcuz_verify=' + bust,
  ];
  for (const url of places) {
    try { if (await bioHas(url, uid, handle, code)) return true; } catch {}
  }
  return false;
}
async function bioHas(url, uid, handle, code) {
  const result = await fetch(url, {
    cache: 'no-store', redirect: 'follow', signal: AbortSignal.timeout(9000), headers: BROWSER,
    cf: { cacheTtl: 0, cacheEverything: false },
  });
  if (!result.ok) return false;
  const reader = result.body.getReader();
  const chunks = []; let total = 0;
  for (;;) {
    const { value, done } = await reader.read();
    if (done) break;
    total += value.byteLength;
    if (total > 3 * 1024 * 1024) { await reader.cancel(); return false; }
    chunks.push(value);
  }
  const bytes = new Uint8Array(total); let offset = 0;
  for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.length; }
  const html = new TextDecoder().decode(bytes);
  for (const match of html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/gi)) {
    const parsed = scriptJson(match[1]);
    if (parsed && findOwner(parsed, uid, handle, code)) return true;
  }
  return false;
}
/**
 * A person's music list, kept as plain text and never as audio. A header line, then one line per
 * sound: `id<TAB>title<TAB>artist<TAB>link`. The link is where the phone can fetch the sound again
 * (an https address, or empty); the sound itself never reaches the server. Only the owner can read
 * the list back. Version 1 lines had no link and are still accepted.
 */
export function normalizeMusicList(input) {
  if (typeof input !== 'string' || input.length > 1048576) throw new Error('invalid music list');
  const lines = input.replace(/\n+$/, '').split('\n');
  const fields = lines[0] === 'ttcuz-music 3' ? 5 : lines[0] === 'ttcuz-music 2' ? 4 : lines[0] === 'ttcuz-music 1' ? 3 : 0;
  if (!fields || lines.length > 201) throw new Error('invalid music list');
  const out = [fields === 5 ? 'ttcuz-music 3' : 'ttcuz-music 2'], seen = new Set();
  for (const line of lines.slice(1)) {
    const parts = line.split('\t');
    if (parts.length !== fields) throw new Error('invalid music list');
    const [id, title, artist, link = '', cover = ''] = parts;
    if (!/^[A-Za-z0-9_.-]{1,64}$/.test(id) || seen.has(id)) throw new Error('invalid music list');
    for (const text of [title, artist])
      if (text.length > 120 || /[\x00-\x1f\x7f]/.test(text)) throw new Error('invalid music list');
    for(const address of [link,cover]) if (address) {
      const link=address;
      let parsed; try { parsed = new URL(link); } catch { throw new Error('invalid music list'); }
      if (link.length > 2048 || /[\s\x00-\x1f\x7f]/.test(link) || !link.startsWith('https://')
          || parsed.protocol !== 'https:' || parsed.username || parsed.password
          || !/^[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)+$/.test(parsed.hostname)
          || /^[0-9.]+$/.test(parsed.hostname) || parsed.hostname.toLowerCase() === 'localhost')
        throw new Error('invalid music list');
    }
    seen.add(id);
    out.push((fields===5?[id,title.trim(),artist.trim(),link,cover]:[id,title.trim(),artist.trim(),link]).join('\t'));
  }
  return out.join('\n') + '\n';
}

// Public appearance only: never tokens, audio, URLs or playback state.
const MUSIC_FLAGS=new Set(['music_glow','music_glow_reactive','music_cover','music_spectrum','music_wave','music_queue','music_artist','music_times','music_show_shuffle','music_skip','music_show_repeat','music_add']);
const MUSIC_COLORS=new Set(['music_surface','music_text','music_accent','music_glow_left','music_glow_right']);
const MUSIC_RANGES={music_glow_intensity:[0,65],music_glow_height:[20,85],music_glow_speed:[0,200],music_glow_sensitivity:[25,250],music_radius:[12,40],music_icons:[18,28],music_pill:[120,200],music_title_size:[14,20],music_artist_size:[11,16],music_cover_size:[48,80],music_slider:[0,4],music_button_shape:[0,4],music_cover_shape:[0,4]};
function normalizeMusicLook(input){
  if(input==null)return {};
  if(typeof input!=='object'||Array.isArray(input)||Object.keys(input).length>40)throw new Error('invalid music appearance');
  const result={};for(const [key,value] of Object.entries(input)){
    if(MUSIC_FLAGS.has(key)){if(typeof value!=='boolean')throw new Error('invalid music flag');}
    else if(MUSIC_COLORS.has(key)){if(!Number.isInteger(value)||value < -2147483648||value>2147483647)throw new Error('invalid music colour');}
    else if(MUSIC_RANGES[key]){const [lo,hi]=MUSIC_RANGES[key];if(!Number.isInteger(value)||value<lo||value>hi)throw new Error('invalid music range');}
    else throw new Error('unknown music appearance');
    result[key]=value;
  }return result;
}

// A person's own nickname particles (particles 5): numbers held to their ranges and a PNG of at most 48 px in base64.
// Rebuilt key by key, so nothing but these fields is ever stored or handed to other phones.
const RECIPE_RANGES={shape:[0,5],count:[1,24],size:[2,28],vary:[0,100],speed:[10,300],motion:[0,5],wiggle:[0,100],spin:[-360,360],colourMode:[0,3],opacity:[10,100],spread:[0,100]};
export function normalizeParticleRecipe(value){
  if(typeof value!=='string'||value.length>8600)throw new Error('invalid particles');
  let input;try{input=JSON.parse(value);}catch{throw new Error('invalid particles');}
  if(!input||typeof input!=='object'||Array.isArray(input))throw new Error('invalid particles');
  const out={v:1};
  for(const [key,[lo,hi]] of Object.entries(RECIPE_RANGES)){
    const v=input[key];if(!Number.isInteger(v)||v<lo||v>hi)throw new Error('invalid particles');out[key]=v;
  }
  if(typeof input.fade!=='boolean'||!Number.isInteger(input.colour)||input.colour< -2147483648||input.colour>2147483647)throw new Error('invalid particles');
  out.fade=input.fade;out.colour=input.colour;
  const image=input.image==null?'':input.image;
  if(typeof image!=='string'||image.length>8000||!/^[A-Za-z0-9+/=]*$/.test(image)||(image&&!image.startsWith('iVBORw0KGgo')))throw new Error('invalid particles');
  if(image)out.image=image;
  if(out.shape===5&&!image)throw new Error('invalid particles');
  return JSON.stringify(out);
}

// Missing extensions mean "this client cannot edit them"; explicit false/0/empty still clears.
const PROFILE_EXTENSIONS=['crownOffsetX','crownOffsetY','displayName','gradientWidth','gradientStop','gradientMotion','appearEffect','particles','particleRecipe','commentColour','tag','gradientColours','profileLayout',
  'musicOn','musicPlace','musicSlot','musicFx','musicFy','musicAccent','musicLook','musicTitle','musicArtist'];
const BASE_STYLE={firstColour:0,lastColour:0,middleColour:0,direction:0,speed:50,offsetX:0,offsetY:0,
  badgeSize:100,threeColours:false,bold:false,glow:false,animated:false,crownOffsetX:0,crownOffsetY:0};
export function normalizeStyle(input, uid, now, previous) {
  if(input&&typeof input==='object'&&!Array.isArray(input)&&previous&&typeof previous==='object'){
    const retained={};for(const key of PROFILE_EXTENSIONS)if(!Object.hasOwn(input,key)&&Object.hasOwn(previous,key))retained[key]=previous[key];
    input={...retained,...input};
  }
  const fields = ['firstColour', 'lastColour', 'middleColour', 'direction', 'speed', 'offsetX', 'offsetY', 'badgeSize'];
  if (!input || fields.some(k => !Number.isInteger(input[k]))) throw new Error('invalid style');
  const crownOffsetX = input.crownOffsetX == null ? 0 : input.crownOffsetX;
  const crownOffsetY = input.crownOffsetY == null ? 0 : input.crownOffsetY;
  if (!Number.isInteger(crownOffsetX) || !Number.isInteger(crownOffsetY)) throw new Error('invalid style');
  for (const k of fields.slice(0, 3)) if (input[k] < -2147483648 || input[k] > 2147483647) throw new Error('invalid colour');
  if (input.direction < 0 || input.direction > 3 || input.speed < 0 || input.speed > 100
      || Math.abs(input.offsetX) > 100 || Math.abs(input.offsetY) > 100
      || Math.abs(crownOffsetX) > 100 || Math.abs(crownOffsetY) > 100
      || input.badgeSize < 25 || input.badgeSize > 300) throw new Error('invalid range');
  const gradientWidth = input.gradientWidth == null ? 100 : input.gradientWidth;
  const gradientStop = input.gradientStop == null ? 50 : input.gradientStop;
  if (!Number.isInteger(gradientWidth) || gradientWidth < 50 || gradientWidth > 200
      || !Number.isInteger(gradientStop) || gradientStop < 5 || gradientStop > 95) throw new Error('invalid gradient');
  // How an animated gradient moves: 0 flow, 1 sway, 2 breathe, 3 spin, 4 shine.
  const gradientMotion = input.gradientMotion == null ? 0 : input.gradientMotion;
  if (!Number.isInteger(gradientMotion) || gradientMotion < 0 || gradientMotion > 4) throw new Error('invalid gradient');
  // How the nickname shows up (0 none, 1 fade, 2 rise, 3 pop, 4 type), what floats around it (0 none, 1 sparkles,
  // 2 snow, 3 hearts, 4 stars), and the colour of the person's own comments for everyone (0: TikTok's own).
  const appearEffect = input.appearEffect == null ? 0 : input.appearEffect;
  const particles = input.particles == null ? 0 : input.particles;
  const commentColour = input.commentColour == null ? 0 : input.commentColour;
  if (!Number.isInteger(appearEffect) || appearEffect < 0 || appearEffect > 4 || !Number.isInteger(particles) || particles < 0 || particles > 5
      || !Number.isInteger(commentColour) || commentColour < -2147483648 || commentColour > 2147483647) throw new Error('invalid effect');
  // The tag is the server's: written from the nick_tags table on publish, never taken from the phone as it is.
  const tag = input.tag == null ? '' : input.tag;
  if (typeof tag !== 'string' || (tag !== '' && !TAG.test(tag))) throw new Error('invalid tag');
  const displayName = input.displayName == null ? '' : input.displayName;
  if (typeof displayName !== 'string' || displayName.length > 80 || /[\x00-\x1f\x7f]/.test(displayName)) throw new Error('invalid name');
  // Music island: optional, off when absent. Only a title and an artist are public -- never audio or a link.
  const profileLayout = input.profileLayout == null ? 0 : input.profileLayout;
  if (!Number.isInteger(profileLayout) || profileLayout < 0 || profileLayout > 2) throw new Error('invalid layout');
  const musicOn = input.musicOn == null ? false : input.musicOn;
  const musicPlace = input.musicPlace == null ? 0 : input.musicPlace;
  const musicSlot = input.musicSlot == null ? 1 : input.musicSlot;
  const musicFx=input.musicFx==null?1000:input.musicFx,musicFy=input.musicFy==null?850:input.musicFy,musicAccent=input.musicAccent==null?0:input.musicAccent;
  if(!Number.isInteger(musicFx)||musicFx<0||musicFx>1000||!Number.isInteger(musicFy)||musicFy<0||musicFy>1000||!Number.isInteger(musicAccent)||musicAccent < -2147483648||musicAccent>2147483647)throw new Error('invalid music position');
  const musicLook=normalizeMusicLook(input.musicLook);
  const musicTitle = input.musicTitle == null ? '' : input.musicTitle;
  const musicArtist = input.musicArtist == null ? '' : input.musicArtist;
  if (typeof musicOn !== 'boolean' || !Number.isInteger(musicPlace) || musicPlace < 0 || musicPlace > 2
      || !Number.isInteger(musicSlot) || musicSlot < 0 || musicSlot > 2) throw new Error('invalid music');
  for (const text of [musicTitle, musicArtist]) {
    if (typeof text !== 'string' || text.length > 120 || /[\x00-\x1f\x7f]/.test(text)) throw new Error('invalid music');
  }
  const gradientColours=input.gradientColours;
  if(gradientColours!=null && (!Array.isArray(gradientColours)||gradientColours.length<2||gradientColours.length>8
      ||gradientColours.some(c=>!Number.isInteger(c)||c < -2147483648||c>2147483647)))throw new Error('invalid gradient colours');
  const particleRecipe = particles === 5 ? normalizeParticleRecipe(input.particleRecipe) : '';
  const output = { profileId: uid, updatedAt: now, displayName: displayName.trim(), gradientWidth, gradientStop, gradientMotion, appearEffect, particles, particleRecipe, commentColour, tag };
  if(gradientColours!=null)output.gradientColours=[...gradientColours];
  for (const k of fields) output[k] = input[k];
  output.profileLayout = profileLayout;
  output.musicOn = musicOn; output.musicPlace = musicPlace; output.musicSlot = musicSlot;
  output.musicFx=musicFx;output.musicFy=musicFy;output.musicAccent=musicAccent;output.musicLook=musicLook;
  output.musicTitle = musicTitle.trim(); output.musicArtist = musicArtist.trim();
  output.crownOffsetX = crownOffsetX;
  output.crownOffsetY = crownOffsetY;
  for (const k of ['threeColours', 'bold', 'glow', 'animated']) {
    if (typeof input[k] !== 'boolean') throw new Error('invalid flag');
    output[k] = input[k];
  }
  return output;
}

/** Stable v1 fields for every archived client. Unsupported extensions cannot hide basic style. */
export function publicProfileStyle(stored,uid,updatedAt) {
  if(!stored||typeof stored!=='object'||Array.isArray(stored))throw new Error('invalid stored profile');
  const now=Date.now(),time=Number.isSafeInteger(updatedAt)&&updatedAt>0?Math.min(updatedAt,now):now;
  try{return normalizeStyle({...BASE_STYLE,...stored},uid,time);}
  catch{
    const base={...BASE_STYLE};for(const key of Object.keys(BASE_STYLE))if(Object.hasOwn(stored,key))base[key]=stored[key];
    return normalizeStyle(base,uid,time);
  }
}

/** Assignments from D1 are public; changes to them are creator-authenticated. */
export async function awardedBadges(env) {
  const available = await catalog(env);
  const ids = new Set(available.map(b => b.id));
  const grouped = new Map();
  try {
    const result = await env.PROFILES.prepare(
      'SELECT badge_id,uid FROM badge_awards ORDER BY awarded_at LIMIT 5000').all();
    for (const row of result.results || []) {
      if (!ids.has(row.badge_id) || !UID.test(row.uid || '')) continue;
      if (!grouped.has(row.badge_id)) grouped.set(row.badge_id, []);
      grouped.get(row.badge_id).push(row.uid);
    }
  } catch { /* The regular feed remains available during a schema rollout. */ }
  return available.filter(b => grouped.has(b.id)).map(b => ({
    id: b.id,
    colour: b.colour,
    image: b.image,
    title: b.id === COMMUNITY_BADGE_ID ? 'Отличие ttcuz' : b.title,
    text: 'Значок выдан создателем ttcuz',
    button: 'Профиль TikTok',
    users: grouped.get(b.id),
  }));
}
// ------------------------------------------------------------------ nickname tags

/** 2–16 letters, digits, "_", ".", "-" or single spaces. */
const TAG = /^(?=.{2,16}$)[\p{L}\p{N}_.-]+(?: [\p{L}\p{N}_.-]+)*$/u;
/** Letters that look alike are one letter: "TТ" or "ttcuz"/"ttсuz" (a Cyrillic с) are the same tag. */
const LOOKALIKE = {'а':'a','в':'b','е':'e','ё':'e','к':'k','м':'m','н':'h','о':'o','р':'p','с':'c','т':'t','у':'y','х':'x','і':'i','ї':'i','ј':'j','ѕ':'s','ԁ':'d','0':'o','1':'l'};
export function tagKey(tag) {
  let key = '';
  for (const ch of tag.normalize('NFKC').toLowerCase()) { if ('_.- '.includes(ch)) continue; key += LOOKALIKE[ch] || ch; }
  return key.replace(/l/g, 'i');
}
let promosReady = false;
async function promoTable(env) {
  if (promosReady) return;
  await env.PROFILES.prepare('CREATE TABLE IF NOT EXISTS promos(aid TEXT PRIMARY KEY,until INTEGER NOT NULL,weight INTEGER NOT NULL,added INTEGER NOT NULL)').run();
  promosReady = true;
}
let tagsReady = false;
async function tagTable(env) {
  if (tagsReady) return;
  await env.PROFILES.prepare('CREATE TABLE IF NOT EXISTS nick_tags(tag_key TEXT PRIMARY KEY,uid TEXT NOT NULL UNIQUE,tag TEXT NOT NULL,created INTEGER NOT NULL)').run();
  tagsReady = true;
}
/** The tag an account holds, or ''. */
async function ownTag(env, uid) {
  try { await tagTable(env); const row = await env.PROFILES.prepare('SELECT tag FROM nick_tags WHERE uid=?').bind(uid).first(); return row?.tag || ''; }
  catch { return ''; }
}
/**
 * Takes a tag for an account (its old one is let go), or lets it go ('' ). The first to take a tag keeps it: anyone
 * else asking for the same one, written in any case or with look-alike letters, is told it is taken. The stored
 * profile is changed too, so the directory's stamp moves and phones read the new tag on their next look.
 */
async function claimTag(env, uid, wanted) {
  await tagTable(env);
  const tag = typeof wanted === 'string' ? wanted.trim().replace(/\s+/g, ' ') : '';
  if (tag !== '' && !TAG.test(tag)) return {error:'invalid_tag', status:400};
  const now = Date.now();
  if (tag === '') await env.PROFILES.prepare('DELETE FROM nick_tags WHERE uid=?').bind(uid).run();
  else {
    const key = tagKey(tag);
    if (key.length < 2) return {error:'invalid_tag', status:400};
    const holder = await env.PROFILES.prepare('SELECT uid FROM nick_tags WHERE tag_key=?').bind(key).first();
    if (holder && holder.uid !== uid) return {error:'tag_taken', status:409};
    await env.PROFILES.batch([
      env.PROFILES.prepare('DELETE FROM nick_tags WHERE uid=? AND tag_key<>?').bind(uid, key),
      env.PROFILES.prepare('INSERT INTO nick_tags(tag_key,uid,tag,created) VALUES(?,?,?,?) ON CONFLICT(tag_key) DO UPDATE SET tag=excluded.tag WHERE nick_tags.uid=excluded.uid').bind(key, uid, tag, now),
    ]);
    const mine = await env.PROFILES.prepare('SELECT uid FROM nick_tags WHERE tag_key=?').bind(key).first();
    if (mine?.uid !== uid) return {error:'tag_taken', status:409};
  }
  await env.PROFILES.prepare("UPDATE profiles SET metadata=json_set(metadata,'$.tag',?),updated_at=? WHERE uid=? AND metadata IS NOT NULL").bind(tag, now, uid).run();
  return {ok:true, tag};
}

/** A badge picture: a PNG of up to 32 KB, or an animated GIF of up to 110 KB; at most 256 px a side. */
function picture(input) {
  if (input == null || input === '') return null;
  if (typeof input !== 'string' || input.length > 152000 || !/^[A-Za-z0-9+/]*={0,2}$/.test(input)) throw new Error('invalid badge');
  if (!input.startsWith('R0lGOD')) return png(input);
  const bytes = Uint8Array.from(atob(input), c => c.charCodeAt(0));
  const head = String.fromCharCode(...bytes.slice(0,6));
  if (bytes.length < 14 || bytes.length > 112640 || (head !== 'GIF89a' && head !== 'GIF87a') || bytes[bytes.length-1] !== 0x3B) throw new Error('invalid GIF');
  const w = bytes[6] | bytes[7] << 8, h = bytes[8] | bytes[9] << 8;
  if (!w || !h || w > 256 || h > 256) throw new Error('invalid dimensions');
  return input;
}
const imageType = (base64) => base64 && base64.startsWith('R0lGOD') ? 'image/gif' : 'image/png';
function png(input) {
  if (input == null || input === '') return null;
  if (typeof input !== 'string' || input.length > 45000 || !/^[A-Za-z0-9+/]*={0,2}$/.test(input)) throw new Error('invalid badge');
  const bytes = Uint8Array.from(atob(input), c => c.charCodeAt(0));
  const signature = [137,80,78,71,13,10,26,10];
  if (bytes.length < 33 || bytes.length > 32768 || signature.some((b,i) => bytes[i] !== b)
      || String.fromCharCode(...bytes.slice(12,16)) !== 'IHDR') throw new Error('invalid PNG');
  const view = new DataView(bytes.buffer);
  const w = view.getUint32(16), h = view.getUint32(20);
  if (!w || !h || w > 256 || h > 256) throw new Error('invalid dimensions');
  return input;
}
export async function profileRoute(request, env) {
  const url = new URL(request.url);
  const customImage = url.pathname.match(/^\/badges\/(custom_[a-f0-9]{20})\.png$/);
  if (customImage && (request.method === 'GET' || request.method === 'HEAD')) {
    if (!env.PROFILES) return reply({error:'storage_unavailable'},503);
    const row = await env.PROFILES.prepare('SELECT png FROM custom_badges WHERE id=?').bind(customImage[1]).first();
    if (!row?.png) return reply({error:'not_found'},404);
    return new Response(request.method === 'HEAD' ? null : Uint8Array.from(atob(row.png), c=>c.charCodeAt(0)), {
      headers:{'content-type':'image/png','cache-control':'public,max-age=31536000,immutable',
        'x-content-type-options':'nosniff'},
    });
  }
  if (!url.pathname.startsWith('/v1/')) return null;
  // The videos the creator promotes, for every phone with the mod. One small list, cached at the edge for ten
  // minutes and read by a phone at most once an hour: a few thousand phones are a few requests to D1.
  if (url.pathname === '/v1/promos' && request.method === 'GET') {
    if (!env.PROFILES) return reply({promos:[]});
    const key = new Request(url.origin + '/v1/promos-cache');
    const store = globalThis.caches?.default;
    const kept = store && await store.match(key);
    if (kept) return kept;
    let rows = [];
    try { await promoTable(env); rows = (await env.PROFILES.prepare('SELECT aid,until,weight FROM promos WHERE until>? ORDER BY added DESC LIMIT 20').bind(Date.now()).all()).results || []; } catch {}
    const response = new Response(JSON.stringify({promos: rows.map(r => ({aid:r.aid, until:r.until, weight:r.weight}))}), {headers:{
      'content-type':'application/json; charset=utf-8','cache-control':'public, max-age=600','access-control-allow-origin':'*'}});
    if (store) await store.put(key, response.clone());
    return response;
  }
  if (url.pathname === '/v1/badge-catalog' && request.method === 'GET')
    return reply({ badges: await catalog(env) });
  if (!env.PROFILES) return reply({ error: 'storage_unavailable' }, 503);
  const publicMusic = url.pathname.match(/^\/v1\/music\/([0-9]{1,24})$/);
  if (publicMusic && request.method === 'GET') {
    const row = await env.PROFILES.prepare('SELECT metadata,music FROM profiles WHERE uid=?').bind(publicMusic[1]).first();
    let metadata; try { metadata = row?.metadata && JSON.parse(row.metadata); } catch {}
    if (metadata?.musicOn !== true) return reply({error:'not_found'},404);
    let list; try { list = normalizeMusicList(row.music || 'ttcuz-music 2\n'); }
    catch { return reply({error:'invalid_music'},400); }
    return reply({ok:true,list});
  }
  // Which accounts have a ttcuz profile, and when each last changed. Phones read this every half hour and
  // only ask for the profiles on it, instead of asking about every account they scroll past.
  if (url.pathname === '/v1/directory' && request.method === 'GET') {
    const key = new Request(url.origin + '/v1/directory-cache');
    const store = globalThis.caches?.default;
    const kept = store && await store.match(key);
    if (kept) return kept;
    const rows = await env.PROFILES.prepare('SELECT uid,updated_at FROM profiles WHERE metadata IS NOT NULL').all();
    const profiles = {};
    for (const row of rows.results || []) profiles[row.uid] = row.updated_at;
    const response = new Response(JSON.stringify({ profiles, now: Date.now() }), { headers: {
      'content-type': 'application/json; charset=utf-8', 'cache-control': 'public, max-age=120',
      'access-control-allow-origin': '*' } });
    if (store) await store.put(key, response.clone());
    return response;
  }
  const read = url.pathname.match(/^\/v1\/profiles\/([0-9]{1,24})(\/badge)?$/);
  if (read && (request.method === 'GET'||request.method === 'HEAD')) {
    const row = await env.PROFILES.prepare('SELECT metadata,badge,updated_at FROM profiles WHERE uid=?').bind(read[1]).first();
    if (!row?.metadata) return reply({ error: 'not_found' }, 404);
    // the address carries ?v=<updated_at>: a new badge is a new address, so this one can be cached for long
    if (read[2]) return row.badge ? new Response(request.method==='HEAD'?null:Uint8Array.from(atob(row.badge), c=>c.charCodeAt(0)), {
      headers: {'content-type':imageType(row.badge),'cache-control':'public,max-age=86400','x-content-type-options':'nosniff'},
    }) : reply({error:'not_found'},404);
    const metadata = publicProfileStyle(JSON.parse(row.metadata),read[1],row.updated_at);
    // versioned by the picture itself: changing a colour does not make every phone download the badge again
    metadata.badgeUrl = row.badge ? url.origin + '/v1/profiles/' + read[1] + '/badge?v=' + (await digest(row.badge)).slice(0,12) : null;
    const response=reply(metadata);return request.method==='HEAD'?new Response(null,{status:response.status,headers:response.headers}):response;
  }
  if (request.method !== 'POST' || !['/v1/verify','/v1/publish','/v1/remove',
      '/v1/device','/v1/badges/awards','/v1/badges/grant','/v1/badges/revoke','/v1/badges/create','/v1/music/save','/v1/music/load','/v1/tag','/v1/promote'].includes(url.pathname)) return reply({error:'not_found'},404);
  const now = Date.now(), interval = 60000;
  const key = await digest((request.headers.get('cf-connecting-ip') || 'unknown') + ':' + Math.floor(now / interval));
  const quota = await env.PROFILES.prepare('INSERT INTO limits(key,hits,until) VALUES(?,1,?) ON CONFLICT(key) DO UPDATE SET hits=hits+1 RETURNING hits').bind(key,now+interval).first();
  if (quota.hits > 20) return reply({error:'rate_limited'},429);
  await env.PROFILES.prepare('DELETE FROM limits WHERE until < ?').bind(now-interval).run();
  const bodyLimit = url.pathname === '/v1/music/save' ? 2097152 : url.pathname === '/v1/publish' ? 196608 : 65536;
  if (Number(request.headers.get('content-length')) > bodyLimit) return reply({error:'too_large'},413);
  // Bound chunked bodies as well as bodies carrying Content-Length.
  const reader = request.body?.getReader(); let total = 0; const parts = [];
  if (!reader) return reply({error:'invalid_body'},400);
  for (;;) { const {value,done}=await reader.read(); if(done)break; total+=value.length;
    if(total>bodyLimit){await reader.cancel();return reply({error:'too_large'},413);} parts.push(value); }
  const bytes=new Uint8Array(total);let pos=0;for(const p of parts){bytes.set(p,pos);pos+=p.length;}
  let body; try { body = JSON.parse(new TextDecoder().decode(bytes)); } catch { return reply({error:'invalid_json'},400); }
  if (!body || !UID.test(body.uid || '') || !TOKEN.test(body.token || '')) return reply({error:'invalid_identity'},400);
  const hash = await digest(body.token);
  let owner = await env.PROFILES.prepare('SELECT owner_hash,verified FROM profiles WHERE uid=?').bind(body.uid).first();
  owner = await recoverDevice(env,body.uid,hash,body.device,owner);
  if (url.pathname === '/v1/device') return owner?.owner_hash===hash ? reply({ok:true,verified:!!owner.verified}) : reply({error:'verify_required'},403);
  if (url.pathname === '/v1/promote') {
    // only the creator's proven account: a video for days (0 takes it back), shown rarely, now and then, or often
    if (body.uid !== CREATOR_UID || !owner || owner.owner_hash !== hash || !owner.verified)
      return reply({error:'creator_verification_required'},403);
    const aid = String(body.aid || ''), days = body.days, weight = body.weight == null ? 2 : body.weight;
    if (!/^\d{6,24}$/.test(aid) || !Number.isInteger(days) || days < 0 || days > 90 || ![1, 2, 3].includes(weight)) return reply({error:'invalid_promo'},400);
    await promoTable(env);
    if (days === 0) await env.PROFILES.prepare('DELETE FROM promos WHERE aid=?').bind(aid).run();
    else await env.PROFILES.prepare('INSERT INTO promos(aid,until,weight,added) VALUES(?,?,?,?) ON CONFLICT(aid) DO UPDATE SET until=excluded.until,weight=excluded.weight')
      .bind(aid, Date.now() + days * 86400000, weight, Date.now()).run();
    try { await globalThis.caches?.default?.delete(new Request(url.origin + '/v1/promos-cache')); } catch {}
    const rows = (await env.PROFILES.prepare('SELECT aid,until,weight FROM promos WHERE until>? ORDER BY added DESC LIMIT 20').bind(Date.now()).all()).results || [];
    return reply({ok:true, promos: rows});
  }
  if (['/v1/badges/awards','/v1/badges/grant','/v1/badges/revoke','/v1/badges/create'].includes(url.pathname)) {
    if (body.uid !== CREATOR_UID || !owner || owner.owner_hash !== hash || !owner.verified)
      return reply({error:'creator_verification_required'},403);
    if (url.pathname === '/v1/badges/awards') {
      const rows=await env.PROFILES.prepare('SELECT uid,badge_id,awarded_at FROM badge_awards ORDER BY awarded_at DESC LIMIT 500').all();
      const badges=await catalog(env);
      return reply({ok:true,awards:(rows.results||[]).map(r=>({uid:r.uid,badgeId:r.badge_id,awardedAt:r.awarded_at,title:badges.find(b=>b.id===r.badge_id)?.title||r.badge_id}))});
    }
    if (url.pathname === '/v1/badges/create') {
      let picture;
      try { picture = png(body.badge); } catch { return reply({error:'invalid_badge'},400); }
      const title = typeof body.title === 'string' ? body.title.trim() : '';
      if (!picture || !title || title.length > 40 || /[\u0000-\u001f]/.test(title))
        return reply({error:'invalid_badge'},400);
      const id = 'custom_' + (await digest(picture)).slice(0,20);
      let existing;
      try { existing = await env.PROFILES.prepare('SELECT id FROM custom_badges WHERE id=?').bind(id).first(); }
      catch { return reply({error:'badge_storage_unavailable'},503); }
      if (!existing && (await customCatalog(env)).length >= 190) return reply({error:'catalog_full'},409);
      await env.PROFILES.prepare('INSERT INTO custom_badges(id,title,png,created_at) VALUES(?,?,?,?) ON CONFLICT(id) DO UPDATE SET title=excluded.title')
        .bind(id,title,picture,Date.now()).run();
      return reply({ok:true,badgeId:id});
    }
    let target = body.targetUid;
    if (!UID.test(target || '') && typeof body.targetHandle === 'string'
        && /^@?[A-Za-z0-9_.]{2,24}$/.test(body.targetHandle)) {
      try { target = await lookupProfile(body.targetHandle.replace(/^@/, '')); }
      catch { target = null; }
      if (!target) return reply({error:'target_lookup_unavailable'},502);
    }
    if (!UID.test(target || ''))
      return reply({error:'invalid_target'},400);
    if (typeof body.badgeId !== 'string' || !(await catalog(env)).some(b => b.id === body.badgeId))
      return reply({error:'unknown_badge'},400);
    if (url.pathname.endsWith('/grant')) {
      const result = await env.PROFILES.prepare(
        'INSERT OR IGNORE INTO badge_awards(uid,badge_id,awarded_at) VALUES(?,?,?)')
        .bind(target, body.badgeId, Date.now()).run();
      return reply({ok:true,changed:!!result.meta?.changes});
    }
    const result = await env.PROFILES.prepare(
      'DELETE FROM badge_awards WHERE uid=? AND badge_id=?')
      .bind(target, body.badgeId).run();
    return reply({ok:true,changed:!!result.meta?.changes});
  }
  if (url.pathname === '/v1/verify') {
    if (owner?.owner_hash === hash && owner.verified) return reply({ok:true});
    if (typeof body.handle !== 'string' || !/^[A-Za-z0-9_.]{2,24}$/.test(body.handle)) return reply({error:'invalid_handle'},400);
    let verified = false;
    try { verified = await verifyOwner(body.uid, body.handle, await challenge(body.uid, body.token)); } catch {}
    if (!verified) return reply({error:'verification_unavailable_or_code_missing'},403);
    // A new device can reclaim only by proving control of the same TikTok account.
    await env.PROFILES.prepare('INSERT INTO profiles(uid,owner_hash,verified,updated_at) VALUES(?,?,1,?) ON CONFLICT(uid) DO UPDATE SET owner_hash=excluded.owner_hash,verified=1').bind(body.uid,hash,Date.now()).run();
    return reply({ok:true});
  }
  // Colours and effects need no bio code: the first device to publish for an id
  // owns it. A later device meets verify_required and proves itself once with the
  // bio code (/v1/verify), which takes the id over. The creator's id never works
  // this way, and a custom badge image is held back until the id is verified.
  if (!owner && (url.pathname === '/v1/publish' || url.pathname === '/v1/music/save' || url.pathname === '/v1/tag') && body.uid !== CREATOR_UID) {
    await env.PROFILES.prepare('INSERT OR IGNORE INTO profiles(uid,owner_hash,verified,updated_at) VALUES(?,?,0,?)').bind(body.uid,hash,Date.now()).run();
    owner = await env.PROFILES.prepare('SELECT owner_hash,verified FROM profiles WHERE uid=?').bind(body.uid).first();
  }
  if (!owner || owner.owner_hash !== hash) return reply({error:'verify_required'},403);
  if (body.uid === CREATOR_UID && !owner.verified) return reply({error:'verify_required'},403);
  if (url.pathname === '/v1/tag') {
    // the account's own phone (its token) holds the tag, as it holds the profile: kept in nick_tags whether or not
    // the profile was published yet, and written into it on every publish
    const result = await claimTag(env, body.uid, body.tag);
    return result.ok ? reply(result) : reply({error:result.error}, result.status);
  }
  if (url.pathname === '/v1/music/load') {
    const row = await env.PROFILES.prepare('SELECT music FROM profiles WHERE uid=? AND owner_hash=?').bind(body.uid,hash).first();
    return reply({ok:true,list:row?.music || ''});
  }
  if (url.pathname === '/v1/music/save') {
    let list; try { list = normalizeMusicList(body.list); } catch { return reply({error:'invalid_music'},400); }
    const saved = await env.PROFILES.prepare('UPDATE profiles SET music=? WHERE uid=? AND owner_hash=?').bind(list,body.uid,hash).run();
    return saved.meta.changes ? reply({ok:true}) : reply({error:'verify_required'},403);
  }
  if (url.pathname === '/v1/remove') {
    // Removing public appearance must not erase the owner's private music backup.
    await env.PROFILES.prepare('UPDATE profiles SET metadata=NULL,badge=NULL,updated_at=? WHERE uid=? AND owner_hash=?').bind(Date.now(),body.uid,hash).run();
    return reply({ok:true});
  }
  let metadata, badge;
  // Ownership is checked above; compatibility never lets a different device merge a style.
  const saved=await env.PROFILES.prepare('SELECT metadata,badge FROM profiles WHERE uid=? AND owner_hash=?').bind(body.uid,hash).first();
  let previous;try{if(saved?.metadata)previous=publicProfileStyle(JSON.parse(saved.metadata),body.uid,Date.now());}catch{}
  try { metadata=normalizeStyle(body.style,body.uid,Date.now(),previous); badge=picture(body.badge); }
  catch { return reply({error:'invalid_style'},400); }
  // The phone sends its badge only when the picture itself changed; a style change alone keeps the one stored.
  if (body.keepBadge === true && body.badge == null && saved?.badge) badge = saved.badge;
  metadata.tag = await ownTag(env, body.uid);
  const badgeHeld = !!badge && !owner.verified;
  if (badgeHeld) badge = null;
  const result = await env.PROFILES.prepare('UPDATE profiles SET metadata=?,badge=?,updated_at=? WHERE uid=? AND owner_hash=?')
    .bind(JSON.stringify(metadata),badge,metadata.updatedAt,body.uid,hash).run();
  return result.meta.changes ? reply({ok:true,badgeHeld}) : reply({error:'verify_required'},403);
}
