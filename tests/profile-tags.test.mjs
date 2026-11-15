import test from 'node:test';
import assert from 'node:assert/strict';
import { normalizeStyle, tagKey, profileRoute, digest } from '../server/cloudflare/profiles.mjs';

const style = { firstColour:-1,lastColour:-1,middleColour:0,direction:0,speed:50,
  offsetX:0,offsetY:0,badgeSize:100,threeColours:false,bold:false,glow:false,animated:false };

test('look-alike letters, case and separators make one tag', () => {
  assert.equal(tagKey('TTCUZ'), tagKey('ttcuz'));
  assert.equal(tagKey('ttсuz'), tagKey('ttcuz'));          // a Cyrillic с
  assert.equal(tagKey('t.t_c-u z'), tagKey('ttcuz'));
  assert.equal(tagKey('n1ck'), tagKey('nick'));
  assert.equal(tagKey('n0va'), tagKey('nova'));
  assert.notEqual(tagKey('ttcuz'), tagKey('ttcux'));
});

test('new style fields are bounded, and the tag must look like one', () => {
  const out = normalizeStyle({...style, gradientMotion:4, appearEffect:3, particles:2, commentColour:-16711936, tag:'ttcuz'}, '1', 42);
  assert.equal(out.gradientMotion, 4); assert.equal(out.appearEffect, 3); assert.equal(out.particles, 2);
  assert.equal(out.commentColour, -16711936); assert.equal(out.tag, 'ttcuz');
  for (const bad of [{gradientMotion:5},{appearEffect:-1},{particles:9},{commentColour:1.5},{tag:'x'},{tag:'a'.repeat(17)},{tag:'bad\ntag'},{tag:'two  spaces'}])
    assert.throws(() => normalizeStyle({...style, ...bad}, '1', 42));
  assert.equal(normalizeStyle(style, '1', 42).tag, '');
});

/** Enough of D1 for the tag routes: profiles, limits and nick_tags. */
function database() {
  const profiles = new Map(), tags = new Map();
  const statement = (sql, args = []) => ({
    bind(...values) { return statement(sql, values); },
    async first() {
      if (sql.includes('INTO limits')) return {hits:1};
      if (sql.startsWith('SELECT owner_hash')) return profiles.get(args[0]) || null;
      if (sql.startsWith('SELECT uid FROM nick_tags WHERE tag_key')) { const r = tags.get(args[0]); return r ? {uid:r.uid} : null; }
      if (sql.startsWith('SELECT tag FROM nick_tags WHERE uid')) { for (const r of tags.values()) if (r.uid === args[0]) return {tag:r.tag}; return null; }
      return null;
    },
    async run() {
      if (sql.startsWith('DELETE FROM nick_tags WHERE uid=? AND tag_key<>?')) { for (const [k, r] of tags) if (r.uid === args[0] && k !== args[1]) tags.delete(k); }
      else if (sql.startsWith('DELETE FROM nick_tags WHERE uid')) { for (const [k, r] of tags) if (r.uid === args[0]) tags.delete(k); }
      else if (sql.startsWith('INSERT INTO nick_tags')) { const r = tags.get(args[0]); if (!r) tags.set(args[0], {uid:args[1], tag:args[2]}); else if (r.uid === args[1]) r.tag = args[2]; }
      return {meta:{changes:1}};
    },
    async all() { return {results:[]}; },
  });
  return { profiles, tags, prepare: sql => statement(sql), async batch(list) { for (const s of list) await s.run(); return []; } };
}
const token = 'a'.repeat(64);
const claim = (uid, tag) => new Request('https://example.com/v1/tag', {method:'POST', body:JSON.stringify({uid, token, tag})});

test('the first account keeps a tag; anyone else, in any spelling, is told it is taken', async () => {
  const db = database(), env = {PROFILES:db};
  const hash = await digest(token);
  db.profiles.set('1', {owner_hash:hash, verified:1});
  db.profiles.set('2', {owner_hash:hash, verified:1});
  db.profiles.set('3', {owner_hash:hash, verified:0});
  let r = await profileRoute(claim('1', 'Nova'), env);
  assert.equal(r.status, 200); assert.equal((await r.json()).tag, 'Nova');
  for (const copy of ['nova', 'NOVA', 'n0va', 'N.O.V.A', 'nоva']) {   // the last one with a Cyrillic о
    r = await profileRoute(claim('2', copy), env);
    assert.equal(r.status, 409, copy); assert.equal((await r.json()).error, 'tag_taken');
  }
  // the account's own phone holds a tag, verified or not; a phone with someone else's token does not
  assert.equal((await profileRoute(claim('3', 'other'), env)).status, 200);
  db.profiles.set('4', {owner_hash:'f'.repeat(64), verified:1});
  assert.equal((await profileRoute(claim('4', 'mine'), env)).status, 403);
  // a new tag lets the old one go; then someone else may take it
  assert.equal((await profileRoute(claim('1', 'Star'), env)).status, 200);
  assert.equal((await profileRoute(claim('2', 'nova'), env)).status, 200);
  assert.equal([...db.tags.values()].filter(t => t.uid === '1').length, 1);
  // letting a tag go
  assert.equal((await profileRoute(claim('1', ''), env)).status, 200);
  assert.equal([...db.tags.values()].some(t => t.uid === '1'), false);
  assert.equal((await profileRoute(claim('2', 'x'), env)).status, 400);
});
