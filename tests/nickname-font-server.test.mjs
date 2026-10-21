import test from 'node:test';
import assert from 'node:assert/strict';
import {
  normalizeStyle,
  profileRoute,
  digest,
  validateNicknameFont,
} from '../server/cloudflare/profiles.mjs';

const token = 'a'.repeat(64);
const style = {
  firstColour: -1,
  lastColour: -1,
  middleColour: 0,
  direction: 0,
  speed: 50,
  offsetX: 0,
  offsetY: 0,
  badgeSize: 100,
  threeColours: false,
  bold: false,
  glow: false,
  animated: false,
};
function font() {
  const bytes = new Uint8Array(32),
    view = new DataView(bytes.buffer);
  view.setUint32(0, 0x00010000);
  view.setUint16(4, 1);
  view.setUint32(20, 28);
  view.setUint32(24, 4);
  return bytes;
}
function encoded(bytes) {
  return Buffer.from(bytes).toString('base64');
}
function database(owner) {
  return {
    owner,
    font: null,
    prepare(sql) {
      let args;
      const db = this;
      return {
        bind(...values) {
          args = values;
          return this;
        },
        async first() {
          if (sql.includes('INSERT INTO limits')) return { hits: 1 };
          if (sql.includes('FROM nickname_fonts'))
            return db.font && db.font.hash === args[1] ? db.font : null;
          if (sql.includes('FROM profiles')) return db.owner;
          throw Error(sql);
        },
        async run() {
          if (sql.startsWith('DELETE FROM limits')) return { meta: { changes: 0 } };
          if (sql.startsWith('INSERT INTO nickname_fonts'))
            db.font = { hash: args[1], font: args[2] };
          else if (sql.startsWith('UPDATE profiles SET metadata=')) db.owner.metadata = args[0];
          else throw Error(sql);
          return { meta: { changes: 1 } };
        },
        async all() {
          return { results: [] };
        },
      };
    },
  };
}
async function send(db, path, body) {
  return profileRoute(
    new Request('https://example.com' + path, {
      method: 'POST',
      body: JSON.stringify({ uid: '123', token, ...body }),
    }),
    { PROFILES: db },
  );
}
test('font directory is bounded, accepts TTF/OTF and rejects traversal/overflow tables', () => {
  const bytes = font();
  assert.deepEqual(validateNicknameFont(encoded(bytes)), bytes);
  new DataView(bytes.buffer).setUint32(0, 0x4f54544f);
  assert.deepEqual(validateNicknameFont(encoded(bytes)), bytes);
  new DataView(bytes.buffer).setUint32(20, 0xffffffff);
  assert.throws(() => validateNicknameFont(encoded(bytes)));
  assert.throws(() => validateNicknameFont(encoded(new Uint8Array(1048577))));
  assert.throws(() => validateNicknameFont('data:application/font;base64,AAAA'));
  assert.throws(() =>
    normalizeStyle({ ...style, nicknameFont: 'file', nicknameFontHash: '../font' }, '123', 1),
  );
  assert.throws(() => normalizeStyle({ ...style, nicknameFont: 'https://evil/font' }, '123', 1));
});
test('only owner can upload; published metadata gates the exact font hash', async () => {
  const db = database({ owner_hash: await digest(token), verified: 1 });
  let response = await send(db, '/v1/nickname-font', {
    token: 'b'.repeat(64),
    font: encoded(font()),
  });
  assert.equal(response.status, 403);
  assert.equal(db.font, null);
  db.owner.verified = 0;
  response = await send(db, '/v1/nickname-font', { font: 'broken' });
  assert.equal(response.status, 400);
  response = await send(db, '/v1/nickname-font', { font: encoded(font()) });
  assert.equal(response.status, 200);
  const result = await response.json();
  assert.match(result.hash, /^[a-f0-9]{64}$/);
  const path = '/v1/nickname-font/123/' + result.hash;
  const get = (method) =>
    profileRoute(new Request('https://example.com' + path, { method }), { PROFILES: db });
  assert.equal((await get('GET')).status, 404);
  response = await send(db, '/v1/publish', {
    style: { ...style, nicknameFont: 'file', nicknameFontHash: result.hash },
    badge: null,
  });
  assert.equal(response.status, 200);
  response = await get('GET');
  assert.equal(response.status, 200);
  assert.deepEqual(new Uint8Array(await response.arrayBuffer()), font());
  assert.equal(response.headers.get('x-content-type-options'), 'nosniff');
  assert.equal((await get('HEAD')).status, 200);
  db.owner.metadata = '';
  assert.equal((await get('GET')).status, 404);
  response = await send(db, '/v1/publish', {
    style: { ...style, nicknameFont: 'file', nicknameFontHash: 'f'.repeat(64) },
    badge: null,
  });
  assert.equal(response.status, 400);
});
test('chunked font upload is bounded while normal publication retains its smaller limit', async () => {
  const db = database({ owner_hash: await digest(token), verified: 1 });
  for (const [path, size] of [
    ['/v1/nickname-font', 1500001],
    ['/v1/publish', 65537],
  ]) {
    const response = await profileRoute(
      new Request('https://example.com' + path, { method: 'POST', body: 'x'.repeat(size) }),
      { PROFILES: db },
    );
    assert.equal(response.status, 413);
    assert.equal(db.font, null);
  }
});
