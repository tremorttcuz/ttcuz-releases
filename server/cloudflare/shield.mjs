// The first thing every request meets: cheap checks before any route or database work.
//
// A flood cannot cost money on the free plan, but it can spend the day's allowance and starve the phones. So:
// - per address, Cloudflare's own rate limiter (no database writes): reads and writes counted apart;
// - writes must come from the app (it always says User-Agent: ttcuz) or from the review page;
// - oversized bodies and unknown methods are refused before they are read.
const WRITES_FROM_BROWSER = new Set(['/plugins/native/admin/review']);
const MAX_BODY = 1900000;

const refuse = (status, error) => new Response(JSON.stringify({error}), {status, headers:{
  'content-type':'application/json','cache-control':'no-store','access-control-allow-origin':'*','retry-after':'60'}});

export async function shield(request, env) {
  const method = request.method;
  if (!['GET','HEAD','POST','OPTIONS'].includes(method)) return refuse(405, 'method');
  if (method === 'OPTIONS') return null;
  const path = new URL(request.url).pathname;
  const ip = request.headers.get('cf-connecting-ip') || 'unknown';
  const writing = method === 'POST';
  if (writing) {
    const length = Number(request.headers.get('content-length') || 0);
    if (length > MAX_BODY) return refuse(413, 'too_large');
    const agent = request.headers.get('user-agent') || '';
    if (agent !== 'ttcuz' && !WRITES_FROM_BROWSER.has(path) && !path.startsWith('/v1/admin')
        && !(path === '/plugins/admin' && request.headers.get('x-admin-pin'))
        && !(/^\/plugins\/reviews\/[^/]+\/(remove|replies\/\d+\/delete)$/.test(path) && request.headers.get('x-admin-pin'))
        // the store page, signed in with Google: sending a plugin, a review
        && !((path.startsWith('/plugins/web/') || path === '/plugins/banner' || path === '/plugins/banner/mine' || /^\/plugins\/reviews\/[^/]+(\/(vote|like|reply|replies\/\d+\/(vote|delete)))?$/.test(path)) && request.headers.get('x-google-token'))) return refuse(403, 'app_only');
  }
  const limiter = writing ? env.WRITE_LIMIT : env.READ_LIMIT;
  if (limiter) {
    try {
      const {success} = await limiter.limit({key: ip});
      if (!success) return refuse(429, 'slow_down');
    } catch { /* the limiter is advisory: never take the server down with it */ }
  }
  return null;
}
