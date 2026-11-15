// Signing in on the store page with a Google account.
//
// The page gets an ID token from Google (Google Identity Services) and sends it with every write as the header
// x-google-token. Google itself checks it (tokeninfo): signed for this site's client id, not expired, a verified
// address. Nothing is stored and no secret is needed; a checked token is remembered until it expires, so a page
// that sends several things in a row costs Google one call. The account is "g:" + Google's id for the person.

const known = new Map();

// ---------------------------------------------------------------- the site's own session
//
// Google's token lives an hour, and its silent renewal fails often (blocked third-party sign-in, a cool-down after
// the prompt was closed): the page then forgot the person. So a checked Google sign-in is answered with a session of
// the site's own -- "s1.<data>.<signature>", HMAC-SHA256 signed, valid 30 days and renewed as it is used -- that the
// page keeps in localStorage and sends in the same header. The key is made once and kept in D1; no setup needed.

export const SESSION_DAYS = 30;
let sessionKey = null;
async function key(env) {
  if (sessionKey) return sessionKey;
  let secret = String(env.SESSION_SECRET || '');
  if (!secret && env.PROFILES) {
    await env.PROFILES.prepare('CREATE TABLE IF NOT EXISTS site_secrets(name TEXT PRIMARY KEY,value TEXT NOT NULL)').run();
    const fresh = Array.from(crypto.getRandomValues(new Uint8Array(32)), b => b.toString(16).padStart(2, '0')).join('');
    await env.PROFILES.prepare("INSERT OR IGNORE INTO site_secrets(name,value) VALUES('session',?)").bind(fresh).run();
    secret = (await env.PROFILES.prepare("SELECT value FROM site_secrets WHERE name='session'").first())?.value || '';
  }
  if (!secret) return null;
  sessionKey = await crypto.subtle.importKey('raw', new TextEncoder().encode(secret), {name: 'HMAC', hash: 'SHA-256'}, false, ['sign', 'verify']);
  return sessionKey;
}
const b64url = bytes => btoa(String.fromCharCode(...bytes)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
const unb64url = text => Uint8Array.from(atob(text.replace(/-/g, '+').replace(/_/g, '/')), c => c.charCodeAt(0));

/** A session for a signed-in person: {session, until}. */
export async function issueSession(env, user, now = Date.now()) {
  const k = await key(env);
  if (!k) return null;
  const until = now + SESSION_DAYS * 86400000;
  const data = b64url(new TextEncoder().encode(JSON.stringify({u: user.uid, n: user.name, p: user.picture, e: until})));
  const sig = b64url(new Uint8Array(await crypto.subtle.sign('HMAC', k, new TextEncoder().encode('s1.' + data))));
  return {session: 's1.' + data + '.' + sig, until};
}

async function sessionUser(env, token) {
  const parts = token.split('.');
  if (parts.length !== 3 || parts[0] !== 's1') return null;
  const k = await key(env);
  if (!k) return null;
  try {
    const ok = await crypto.subtle.verify('HMAC', k, unb64url(parts[2]), new TextEncoder().encode('s1.' + parts[1]));
    if (!ok) return null;
    const d = JSON.parse(new TextDecoder().decode(unb64url(parts[1])));
    if (!(d.e > Date.now()) || !/^g:\d{5,40}$/.test(d.u || '')) return null;
    return {uid: d.u, name: String(d.n || '').slice(0, 40), picture: String(d.p || ''), until: d.e, session: true};
  } catch {
    return null;
  }
}

/** {uid, name, picture} of the person signed in (by a Google token or a site session), or null. */
export async function googleUser(request, env) {
  const token = request.headers.get('x-google-token') || '';
  if (token.startsWith('s1.') && token.length < 4096) return sessionUser(env, token);
  const client = String(env.GOOGLE_CLIENT_ID || '');
  if (!client || !/^[\w-]+\.[\w-]+\.[\w-]+$/.test(token) || token.length > 4096) return null;
  const now = Date.now();
  const cached = known.get(token);
  if (cached) { if (cached.until > now) return cached.user; known.delete(token); }
  try {
    const response = await fetch('https://oauth2.googleapis.com/tokeninfo?id_token=' + encodeURIComponent(token), {signal: AbortSignal.timeout(8000)});
    if (!response.ok) return null;
    const g = await response.json();
    const until = Number(g.exp) * 1000;
    if (g.aud !== client || String(g.email_verified) !== 'true' || !/^\d{5,40}$/.test(g.sub || '') || !(until > now)) return null;
    const user = {
      uid: 'g:' + g.sub,
      name: String(g.name || g.given_name || String(g.email || '').split('@')[0] || '').replace(/[\u0000-\u001f\u007f]/g, '').slice(0, 40),
      picture: /^https:\/\/[\w.-]+\.googleusercontent\.com\//.test(g.picture || '') ? g.picture : '',
    };
    if (known.size > 500) known.clear();
    known.set(token, {user, until});
    return user;
  } catch {
    return null;
  }
}
