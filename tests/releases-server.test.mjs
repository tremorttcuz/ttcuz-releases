import test from 'node:test';
import assert from 'node:assert/strict';
import {releaseVersion} from '../server/cloudflare/releases.mjs';

test('update feed accepts only a stable release with its repository APK', async () => {
  const originalFetch = globalThis.fetch;
  const originalCaches = globalThis.caches;
  let stored;
  globalThis.caches = {default:{async match(){return undefined;},async put(key,value){stored=value;}}};
  const asset = {name:'ttcuz.apk',state:'uploaded',size:123,
    browser_download_url:'https://github.com/tremorttcuz/ttcuz-releases/releases/download/v0.1.4/ttcuz.apk'};
  let release = {tag_name:'v0.1.4',assets:[asset],body:'changes',draft:false,prerelease:false};
  globalThis.fetch = async () => Response.json(release);
  const request = new Request('https://example.com/version');
  try {
    const valid = await (await releaseVersion(request)).json();
    assert.equal(valid.version,'0.1.4');
    assert.equal(valid.url,asset.browser_download_url);
    assert.ok(stored);
    for (const change of [{draft:true},{prerelease:true},{tag_name:'nightly'},
      {assets:[]},{assets:[{...asset,browser_download_url:'https://other.example/ttcuz.apk'}]}]) {
      const saved=release;
      release={...saved,...change};
      const unavailable = await (await releaseVersion(request)).json();
      assert.equal(unavailable.version,'0.6.9.0');
      assert.equal(unavailable.url,'https://github.com/tremorttcuz/ttcuz-releases/releases/download/v0.6.9.0/ttcuz-0.6.9.0.apk');
      release=saved;
    }
    globalThis.fetch = async () => new Response(null,{status:404});
    const fallback = await (await releaseVersion(request)).json();
    assert.equal(fallback.version,'0.6.9.0');
    assert.equal(fallback.url,'https://github.com/tremorttcuz/ttcuz-releases/releases/download/v0.6.9.0/ttcuz-0.6.9.0.apk');
  } finally {
    globalThis.fetch=originalFetch;
    globalThis.caches=originalCaches;
  }
});

test('update feed accepts a release-tagged APK asset', async () => {
  const originalFetch=globalThis.fetch,originalCaches=globalThis.caches;
  globalThis.caches={default:{async match(){return undefined;},async put(){}}};
  const asset={name:'ttcuz-0.5.0.apk',state:'uploaded',size:123,
    browser_download_url:'https://github.com/tremorttcuz/ttcuz-releases/releases/download/v0.5.0/ttcuz-0.5.0.apk'};
  globalThis.fetch=async()=>Response.json({tag_name:'v0.5.0',assets:[asset],body:'changes'});
  try{const result=await(await releaseVersion(new Request('https://example.com/version'))).json();assert.equal(result.url,asset.browser_download_url);}
  finally{globalThis.fetch=originalFetch;globalThis.caches=originalCaches;}
});

test('the armv7 channel: its own pre-release and apk, never the main one', async () => {
  const originalFetch=globalThis.fetch,originalCaches=globalThis.caches;
  globalThis.caches={default:{async match(){return undefined;},async put(){}}};
  const base='https://github.com/tremorttcuz/ttcuz-releases/releases/download/';
  const main={tag_name:'v0.9.0',draft:false,prerelease:false,body:'notes',
    assets:[{name:'ttcuz-0.9.0.apk',state:'uploaded',size:5,browser_download_url:base+'v0.9.0/ttcuz-0.9.0.apk'}]};
  const v7={tag_name:'v0.9.0-armv7',draft:false,prerelease:true,
    assets:[{name:'ttcuz-0.9.0-armv7.apk',state:'uploaded',size:5,browser_download_url:base+'v0.9.0-armv7/ttcuz-0.9.0-armv7.apk'}]};
  const source={tag_name:'v0.9.0-source',draft:false,prerelease:true,assets:[]};
  globalThis.fetch=async url=>String(url).endsWith('/releases/latest')?Response.json(main):Response.json([source,v7,main]);
  try{
    const result=await(await releaseVersion(new Request('https://example.com/version'))).json();
    assert.equal(result.url,base+'v0.9.0/ttcuz-0.9.0.apk');
    assert.equal(result.version_armv7,'0.9.0');
    assert.equal(result.url_armv7,base+'v0.9.0-armv7/ttcuz-0.9.0-armv7.apk');
    globalThis.fetch=async url=>String(url).endsWith('/releases/latest')?Response.json(main):Response.json([source,main]);
    const none=await(await releaseVersion(new Request('https://example.com/version'))).json();
    assert.equal(none.url_armv7,undefined);
  }finally{globalThis.fetch=originalFetch;globalThis.caches=originalCaches;}
});
