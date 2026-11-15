const REPOSITORY = 'tremorttcuz/ttcuz-releases';
const ARMV7_PREFIX = 'https://github.com/' + REPOSITORY + '/releases/download/';

/**
 * The armv7 channel: the newest release tagged "v<version>-armv7" (made from the armv7 branch, marked a pre-release so
 * GitHub's "latest" stays the main build) with its "ttcuz-<version>-armv7.apk". The 32-bit build reads only these
 * two fields; nothing when there is no such release. One more request, cached with the main answer.
 */
async function armv7() {
  try {
    const response = await fetch('https://api.github.com/repos/' + REPOSITORY + '/releases?per_page=30', {
      headers:{'Accept':'application/vnd.github+json','User-Agent':'ttcuz-updates'},
      signal:AbortSignal.timeout(10000),
    });
    if (!response.ok) return {};
    const list = await response.json();
    if (!Array.isArray(list)) return {};
    for (const release of list) {
      const match = /^v(\d+(?:\.\d+){1,3})-armv7$/.exec(String(release.tag_name || ''));
      if (!match || release.draft) continue;
      const apk = Array.isArray(release.assets) && release.assets.find(asset =>
        asset.name === `ttcuz-${match[1]}-armv7.apk` && asset.state === 'uploaded' && asset.size > 0
        && typeof asset.browser_download_url === 'string' && asset.browser_download_url.startsWith(ARMV7_PREFIX));
      if (apk) return {version_armv7:match[1],url_armv7:apk.browser_download_url};
    }
    return {};
  } catch { return {}; }
}

export async function releaseVersion(request) {
  // Version the cache key so a previous deployment's cached "no release" fallback
  // cannot hide a newly published GitHub release after the worker is updated.
  const key = new Request(new URL('/release-version-cache-v13', request.url).toString());
  const cached = await caches.default.match(key);
  if (cached) return cached;
  const fallback = {version:'0.6.9.0',
    url:'https://github.com/tremorttcuz/ttcuz-releases/releases/download/v0.6.9.0/ttcuz-0.6.9.0.apk',
    notes_ru:'Сборка плагинов в приложении, новый список плагинов, исправления.'};
  // the armv7 channel stands on its own: it is answered even when the main release is missing
  const v7 = await armv7();
  Object.assign(fallback, v7);
  try {
    const response = await fetch('https://api.github.com/repos/' + REPOSITORY + '/releases/latest', {
      headers:{'Accept':'application/vnd.github+json','User-Agent':'ttcuz-updates'},
      signal:AbortSignal.timeout(10000),
    });
    if (!response.ok) return Response.json(fallback);
    const release = await response.json();
    const version = String(release.tag_name || '').replace(/^v/,'');
    const apk = Array.isArray(release.assets) && release.assets.find(asset =>
      (asset.name === 'ttcuz.apk' || asset.name === `ttcuz-${version}.apk`)
      && asset.state === 'uploaded' && asset.size > 0);
    const prefix = 'https://github.com/' + REPOSITORY + '/releases/download/';
    if (!/^\d+(\.\d+){1,3}$/.test(version) || !apk || release.draft || release.prerelease
        || typeof apk.browser_download_url !== 'string' || !apk.browser_download_url.startsWith(prefix))
      return Response.json(fallback);
    const result = Response.json({version,url:apk.browser_download_url,
      notes_ru:String(release.body || '').slice(0,4000),...v7}, {
      headers:{'cache-control':'public,max-age=300','access-control-allow-origin':'*'},
    });
    await caches.default.put(key,result.clone());
    return result;
  } catch { return Response.json(fallback); }
}
