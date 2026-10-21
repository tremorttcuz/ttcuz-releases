const REPOSITORY = 'tremorttcuz/ttcuz-releases';
export async function releaseVersion(request) {
  // Version the cache key so a previous deployment's cached "no release" fallback
  // cannot hide a newly published GitHub release after the worker is updated.
  const key = new Request(new URL('/release-version-cache-v11', request.url).toString());
  const cached = await caches.default.match(key);
  if (cached) return cached;
  const fallback = {
    version: '0.6.8.1',
    url: 'https://github.com/tremorttcuz/ttcuz-releases/releases/download/v0.6.8.1/ttcuz.apk',
    notes_ru: 'Свои темы выключены по умолчанию.',
  };
  try {
    const response = await fetch(
      'https://api.github.com/repos/' + REPOSITORY + '/releases/latest',
      {
        headers: { Accept: 'application/vnd.github+json', 'User-Agent': 'ttcuz-updates' },
        signal: AbortSignal.timeout(10000),
      },
    );
    if (!response.ok) return Response.json(fallback);
    const release = await response.json();
    const version = String(release.tag_name || '').replace(/^v/, '');
    const apk =
      Array.isArray(release.assets) &&
      release.assets.find(
        (asset) =>
          (asset.name === 'ttcuz.apk' || asset.name === `ttcuz-${version}.apk`) &&
          asset.state === 'uploaded' &&
          asset.size > 0,
      );
    const prefix = 'https://github.com/' + REPOSITORY + '/releases/download/';
    if (
      !/^\d+(\.\d+){1,3}$/.test(version) ||
      !apk ||
      release.draft ||
      release.prerelease ||
      typeof apk.browser_download_url !== 'string' ||
      !apk.browser_download_url.startsWith(prefix)
    )
      return Response.json(fallback);
    const result = Response.json(
      {
        version,
        url: apk.browser_download_url,
        notes_ru: String(release.body || '').slice(0, 4000),
      },
      {
        headers: { 'cache-control': 'public,max-age=300', 'access-control-allow-origin': '*' },
      },
    );
    await caches.default.put(key, result.clone());
    return result;
  } catch {
    return Response.json(fallback);
  }
}
