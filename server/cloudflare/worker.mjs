import {crashRoute} from './crashes.mjs';
import {shield} from './shield.mjs';
import {pluginRoute,announcePending} from './plugins.mjs';
import {discordStatus} from './discord.mjs';
import {admin, refusal} from './admin.mjs';
import { awardedBadges, profileRoute } from './profiles.mjs';
import { releaseVersion } from './releases.mjs';
import { appearanceRoute } from './appearance.mjs';
import { statsRoute, notePresence, cleanPresence } from './stats.mjs';
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
  async fetch(request, env, ctx) {
    try {return await route(request,env,ctx);}
    catch(error) {
      console.error('ttcuz route failed',new URL(request.url).pathname,String(error?.message || error));
      return new Response(JSON.stringify({error:'storage_unavailable'}),{
        status:503,headers:{'content-type':'application/json','cache-control':'no-store','access-control-allow-origin':'*'}});
    }
  },
  // every 30 minutes: plugins waiting for review that never reached Discord (one cheap query when there are none)
  async scheduled(event,env,ctx) {
    ctx.waitUntil(announcePending(env,'https://ttcuz.ttcuz1337.workers.dev',3).then(done=>{
      if(done.error)console.error('ttcuz announce failed',done.error);
    },error=>console.error('ttcuz announce failed',String(error?.message||error))));
    // people not seen for a week leave the "who uses ttcuz" table
    ctx.waitUntil(cleanPresence(env).catch(error=>console.error('ttcuz presence clean failed',String(error?.message||error))));
  },
};
async function route(request,env,ctx) {
    const blocked=await shield(request,env);
    if(blocked)return blocked;
    const stats=await statsRoute(request,env);
    if(stats)return stats;
    const appearance=await appearanceRoute(request,env);
    if(appearance)return appearance;
    if (new URL(request.url).pathname === "/v1/admin/discord") {
      // the bot, its invite link and the channels it found on the mod's server (admin PIN)
      const who = await admin(request, env);
      if (who !== "ok") return refusal(who, (body, status) => json(body, status));
      return json(await discordStatus(env));
    }
    const plugin = await pluginRoute(request,env,ctx);
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
      // the phone may say it is here (once in ten minutes): counted for /v1/stats
      notePresence(request, env, ctx);
      let awarded = [];
      try { awarded = await awardedBadges(env); } catch { /* Keep built-in badge available. */ }
      const badges = [{
        id: "creator_trem0r1_1",
        colour: "#000000",
        image: "",
        title: "@trem0r1_1",
        text: "Создатель ttcuz",
        button: "Профиль TikTok",
        users: [creatorUid],
      }, ...awarded];
      // The tag is of the badges alone (not of "now"): a phone that has them gets 304 and nothing to download or
      // parse. Without it every phone pulled and re-read the whole list every three minutes.
      const hash = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(JSON.stringify(badges)));
      const tag = '"' + Array.from(new Uint8Array(hash).slice(0, 12), b => b.toString(16).padStart(2, "0")).join("") + '"';
      const headers = {"etag": tag, "cache-control": "public, max-age=15", "access-control-allow-origin": "*"};
      if ((request.headers.get("if-none-match") || "") === tag) return new Response(null, {status: 304, headers});
      return new Response(JSON.stringify({badges, now: Math.floor(Date.now() / 1000)}), {
        headers: {...headers, "content-type": "application/json; charset=utf-8"},
      });
    }
    if (path === "/version") {
      return releaseVersion(request);
    }
    if (path === "/" || path === "/guide.html") {
      const guideUrl = "https://raw.githubusercontent.com/tremorttcuz/ttcuz-releases/main/guide.html";
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
