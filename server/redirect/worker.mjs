// The old address of the ttcuz server, kept alive on a second free Cloudflare account for one job only: every link
// to it -- the plugin store, the review page, the guide, any path -- is sent to the same place on the new address.
// Nothing is served here and nothing is stored; the app itself talks only to the new address.
export const TARGET = 'https://ttcuz.ttcuz1337.workers.dev';

export function moved(request) {
  const from = new URL(request.url);
  return Response.redirect(TARGET + from.pathname + from.search, 301);
}

export default { fetch: moved };
