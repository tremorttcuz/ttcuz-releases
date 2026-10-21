import test from 'node:test';
import assert from 'node:assert/strict';
import { recoverDevice } from '../server/cloudflare/device.mjs';
import { digest } from '../server/cloudflare/profiles.mjs';
import { appearanceRoute, validateSnapshot } from '../server/cloudflare/appearance.mjs';
function database(owner) {
  const devices = new Set();
  return {
    devices,
    owner,
    prepare(sql) {
      let args;
      const db = this;
      return {
        bind(...v) {
          args = v;
          return this;
        },
        async first() {
          if (sql.includes('appearance_devices'))
            return devices.has(args[1]) ? { device_hash: args[1] } : null;
          if (sql.includes('FROM profiles')) return db.owner;
          if (sql.includes('appearance_backups')) return null;
          throw Error(sql);
        },
        async run() {
          if (sql.startsWith('UPDATE profiles')) {
            if (db.owner.owner_hash === args[2]) db.owner = { ...db.owner, owner_hash: args[0] };
          } else if (sql.includes('INSERT OR IGNORE INTO appearance_devices')) devices.add(args[1]);
          else throw Error(sql);
          return { meta: { changes: 1 } };
        },
      };
    },
  };
}
test('only a registered device can recover; verified status survives reinstall', async () => {
  const secret = 'd'.repeat(64),
    old = await digest('a'.repeat(64)),
    next = await digest('b'.repeat(64));
  const db = database({ owner_hash: old, verified: 1 });
  assert.equal(
    (await recoverDevice({ PROFILES: db }, '123', next, secret, db.owner)).owner_hash,
    old,
  );
  assert.equal(db.devices.size, 0);
  await recoverDevice({ PROFILES: db }, '123', old, secret, db.owner);
  assert.equal(db.devices.size, 1);
  const recovered = await recoverDevice({ PROFILES: db }, '123', next, secret, db.owner);
  assert.equal(recovered.owner_hash, next);
  assert.equal(recovered.verified, 1);
  assert.equal(
    (await recoverDevice({ PROFILES: db }, '123', old, 'e'.repeat(64), db.owner)).owner_hash,
    next,
  );
});
test('username alone cannot read a private backup or register a device', async () => {
  const db = database({ owner_hash: await digest('a'.repeat(64)), verified: 1 });
  const request = new Request('https://example.com/v1/appearance', {
    method: 'POST',
    body: JSON.stringify({
      uid: '123',
      token: 'b'.repeat(64),
      handle: 'owner',
      device: 'd'.repeat(64),
      action: 'read',
    }),
  });
  const response = await appearanceRoute(request, { PROFILES: db });
  assert.equal(response.status, 403);
  assert.equal(db.devices.size, 0);
});
test('backup validation preserves types and rejects traversal and identity settings', () => {
  const snapshot = {
    schema: 1,
    global: { avatar_shape: { type: 'int', value: 7 } },
    account: { profile_gradient_colours: { type: 'string', value: '[-1,-2]' } },
    files: [],
  };
  assert.deepEqual(validateSnapshot(snapshot), snapshot);
  assert.throws(() =>
    validateSnapshot({ ...snapshot, account: { token_123: { type: 'string', value: 'secret' } } }),
  );
  assert.throws(() =>
    validateSnapshot({
      ...snapshot,
      files: [{ name: '../font', hash: 'a'.repeat(64), size: 123 }],
    }),
  );
});
