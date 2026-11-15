import test from 'node:test';
import assert from 'node:assert/strict';
import { profileRoute, digest, CREATOR_UID } from '../server/cloudflare/profiles.mjs';

function database(owners) {
  const promos = new Map();
  const statement = (sql, args = []) => ({
    bind(...v) { return statement(sql, v); },
    async first() { if (sql.includes('INTO limits')) return {hits:1}; if (sql.startsWith('SELECT owner_hash')) return owners.get(args[0]) || null; return null; },
    async run() {
      if (sql.startsWith('DELETE FROM promos')) promos.delete(args[0]);
      else if (sql.startsWith('INSERT INTO promos')) promos.set(args[0], {aid:args[0], until:args[1], weight:args[2]});
      return {meta:{changes:1}};
    },
    async all() { if (sql.includes('FROM promos')) return {results:[...promos.values()].filter(p => p.until > args[0])}; return {results:[]}; },
  });
  return { promos, prepare: sql => statement(sql) };
}
const token = 'a'.repeat(64);
const promote = (uid, body) => new Request('https://example.com/v1/promote', {method:'POST', body:JSON.stringify({uid, token, ...body})});

test('only the proven creator promotes; everyone reads the list', async () => {
  const hash = await digest(token);
  const db = database(new Map([[CREATOR_UID, {owner_hash:hash, verified:1}], ['5', {owner_hash:hash, verified:1}]])), env = {PROFILES:db};
  assert.equal((await profileRoute(promote('5', {aid:'7300000000000000001', days:3}), env)).status, 403);
  assert.equal((await profileRoute(promote(CREATOR_UID, {aid:'abc', days:3}), env)).status, 400);
  assert.equal((await profileRoute(promote(CREATOR_UID, {aid:'7300000000000000001', days:91}), env)).status, 400);
  const ok = await profileRoute(promote(CREATOR_UID, {aid:'7300000000000000001', days:3, weight:3}), env);
  assert.equal(ok.status, 200);
  const list = await (await profileRoute(new Request('https://example.com/v1/promos'), env)).json();
  assert.deepEqual(list.promos.map(p => [p.aid, p.weight]), [['7300000000000000001', 3]]);
  assert.equal((await profileRoute(promote(CREATOR_UID, {aid:'7300000000000000001', days:0}), env)).status, 200);
  assert.equal(db.promos.size, 0);
});
