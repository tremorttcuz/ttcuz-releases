import { digest } from './profiles.mjs';
export async function recoverDevice(env, uid, tokenHash, secret, owner) {
  if (!owner || !/^[a-f0-9]{64}$/.test(secret || '')) return owner;
  const deviceHash = await digest(secret);
  if (owner.owner_hash !== tokenHash) {
    const trusted = await env.PROFILES.prepare(
      'SELECT device_hash FROM appearance_devices WHERE uid=? AND device_hash=?',
    )
      .bind(uid, deviceHash)
      .first();
    if (!trusted) return owner;
    // Compare and swap prevents a stale request replacing a newer owner token.
    await env.PROFILES.prepare('UPDATE profiles SET owner_hash=? WHERE uid=? AND owner_hash=?')
      .bind(tokenHash, uid, owner.owner_hash)
      .run();
    owner = await env.PROFILES.prepare('SELECT owner_hash,verified FROM profiles WHERE uid=?')
      .bind(uid)
      .first();
  }
  if (owner.owner_hash === tokenHash)
    await env.PROFILES.prepare(
      'INSERT OR IGNORE INTO appearance_devices(uid,device_hash) VALUES(?,?)',
    )
      .bind(uid, deviceHash)
      .run();
  return owner;
}
