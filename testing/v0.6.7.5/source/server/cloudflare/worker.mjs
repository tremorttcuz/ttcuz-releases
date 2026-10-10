import {crashRoute} from './crashes.mjs';
import {pluginRoute} from './plugins.mjs';
import { awardedBadges, profileRoute } from './profiles.mjs';
import { releaseVersion } from './releases.mjs';
const creatorUid = "7491898648855512119";

const json = (body, status = 200) => new Response(JSON.stringify(body), {
  status,
  headers: {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "public, max-age=15",
    "access-control-allow-origin": "*",
  },
});

export default {
  async fetch(request, env) {
    try {return await route(request,env);}
    catch(error) {
      console.error('ttcuz route failed',new URL(request.url).pathname,String(error?.message || error));
      return new Response(JSON.stringify({error:'storage_unavailable'}),{
        status:503,headers:{'content-type':'application/json','cache-control':'no-store','access-control-allow-origin':'*'}});
    }
  },
};
async function route(request,env) {
    const plugin = await pluginRoute(request,env);
    if(plugin)return plugin;
    const crash = await crashRoute(request,env);
    if(crash)return crash;
    const profile = await profileRoute(request, env);
    if (profile) return profile;
    if (request.method !== "GET" && request.method !== "HEAD") {
      return json({ error: "method not allowed" }, 405);
    }

    const path = new URL(request.url).pathname;
    if (path === "/health") return json({ ok: true, project: "ttcuz" });
    if (path === "/badges") {
      let awarded = [];
      try { awarded = await awardedBadges(env); } catch { /* Keep built-in badge available. */ }
      return json({
        badges: [{
          id: "creator_trem0r1_1",
          colour: "#000000",
          image: "",
          title: "@trem0r1_1",
          text: "Создатель ttcuz",
          button: "Профиль TikTok",
          users: [creatorUid],
        }, ...awarded],
        now: Math.floor(Date.now() / 1000),
      });
    }
    if (path === "/version") {
      return releaseVersion(request);
    }
    if (path === "/" || path === "/guide.html") {
      const guideUrl = "https://raw.githubusercontent.com/daniilsolovatulin-cyber/ttcuz-releases/main/guide.html";
      const key = new Request(new URL("/guide-cache", request.url).toString());
      const cached = await caches.default.match(key);
      if (cached) return cached;
      try {
        const guide = await fetch(guideUrl, { signal: AbortSignal.timeout(10000) });
        if (!guide.ok) return new Response("Руководство временно недоступно.", { status: 502 });
        const response = new Response(guide.body, { headers: {
          "content-type": "text/html; charset=utf-8",
          "cache-control": "public, max-age=300",
        }});
        await caches.default.put(key, response.clone());
        return response;
      } catch {
        return new Response("Руководство временно недоступно.", { status: 502 });
      }
    }
    return env.ASSETS.fetch(request);
}
