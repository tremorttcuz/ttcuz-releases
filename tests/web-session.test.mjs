import test from 'node:test';
import assert from 'node:assert/strict';
import { googleUser, issueSession } from '../server/cloudflare/google.mjs';

function database() {
  const rows = new Map();
  const statement = (sql, args = []) => ({
    bind(...v) { return statement(sql, v); },
    async run() { if (sql.startsWith('INSERT OR IGNORE INTO site_secrets') && !rows.has('session')) rows.set('session', args[0]); return {meta:{changes:1}}; },
    async first() { return sql.startsWith('SELECT value FROM site_secrets') ? {value: rows.get('session')} : null; },
  });
  return { rows, prepare: sql => statement(sql) };
}
const ask = (token) => new Request('https://example.com/plugins/web/me', {method:'POST', headers:{'x-google-token': token}});

test('a site session keeps the person signed in for 30 days and cannot be forged', async () => {
  const env = {PROFILES: database(), GOOGLE_CLIENT_ID: 'x.apps.googleusercontent.com'};
  const person = {uid: 'g:123456789', name: 'Nick', picture: ''};
  const {session, until} = await issueSession(env, person);
  assert.ok(until > Date.now() + 29 * 86400000);
  const back = await googleUser(ask(session), env);
  assert.equal(back.uid, 'g:123456789'); assert.equal(back.name, 'Nick'); assert.equal(back.session, true);
  // another account in the data, the same signature: refused
  const [, data, sig] = session.split('.');
  const forged = btoa(JSON.stringify({u:'g:999999999', n:'X', p:'', e:until})).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'');
  assert.equal(await googleUser(ask('s1.' + forged + '.' + sig), env), null);
  assert.equal(await googleUser(ask('s1.' + data + '.' + sig.slice(0, -2) + 'AA'), env), null);
  // an expired one: refused
  const old = await issueSession(env, person, Date.now() - 31 * 86400000);
  assert.equal(await googleUser(ask(old.session), env), null);
});
