package cat.ttcuz.framework;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.lang.ref.WeakReference;

/**
 * Back to the same video at the same second. Every time a TikTok screen pauses, the video playing and how far it got
 * are written down. When TikTok comes back and that video plays from the start, it is moved to where it was; when
 * TikTok had been closed (or Android cleared it) and opens on a fresh feed, the video is fetched by its id (TikTok's
 * own request for one video) and put first in the feed itself, and it goes on from that second with a quiet word.
 */
final class VideoResume {
    private VideoResume() {}

    static final String KEY = "resume_video";
    private static final String AID = "resume_aid", AT = "resume_at", WHEN = "resume_when";
    /** A place older than this is not gone back to: the feed starts as TikTok gives it. */
    static final long FRESH = 3 * 3600_000L;
    /** Less than this into a video, starting it over loses nothing. */
    private static final long WORTH = 2000L, END = 1500L;

    private static final Handler main = new Handler(Looper.getMainLooper());
    private static volatile Boolean on;

    /** The video playing now and its player (main thread). */
    private static String aid = "";
    private static WeakReference<Object> player = new WeakReference<>(null);
    /** The place to come back to, while TikTok is coming back; null once found or given up. */
    private static String wantedAid;
    private static long wantedAt, wantedUntil;
    private static boolean cold, plainStart;
    /** The saved video, fetched for a fresh feed, and whether it went into the feed already. */
    private static volatile Object waitingPost;
    private static volatile boolean placed, saidSo;

    private static SharedPreferences prefs() {
        Context context = Ttcuz.context();
        return context == null ? null : context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
    }

    static boolean on() {
        Boolean known = on;
        if (known != null) return known;
        SharedPreferences p = prefs();
        boolean value = p == null || p.getBoolean(KEY, true);
        on = value;
        return value;
    }

    static void set(boolean value) {
        on = value;
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean(KEY, value).apply();
        if (!value) wantedAid = null;
    }

    /** The process started for this kind of screen: plain = from the icon or recents, not a link or notification. */
    static void coldStart(boolean plain) {
        cold = true;
        plainStart = plain;
    }

    /** TikTok came on screen: the first video to play is held against the saved place. */
    static void came() {
        wantedAid = null;
        SharedPreferences p = on() ? prefs() : null;
        if (p == null) { cold = false; return; }
        String saved = p.getString(AID, "");
        long at = p.getLong(AT, 0), when = p.getLong(WHEN, 0), age = System.currentTimeMillis() - when;
        if (saved.isEmpty() || at < WORTH || age < 0 || age > FRESH) { cold = false; return; }
        wantedAid = saved;
        wantedAt = at;
        // a cold start has the splash, maybe an advert, and the feed's first load before any video plays
        wantedUntil = SystemClock.uptimeMillis() + (cold ? 25_000L : 8_000L);
        if (cold && plainStart) fetch(saved);
    }

    /** TikTok's own request for one video by its id (DetailApi, as the promoted videos use), off the main thread. */
    private static void fetch(final String id) {
        waitingPost = null;
        placed = false;
        saidSo = false;
        Net.away("resume video", () -> {
            try {
                Object post = Class.forName("com.ss.android.ugc.aweme.detail.api.DetailApi")
                        .getMethod("LIZ", int.class, int.class, String.class, String.class).invoke(null, 0, 0, id, "homepage_hot");
                if (post != null && id.equals(NativeRead.aid(post)) && id.equals(wantedAid)) waitingPost = post;
            } catch (Throwable error) {
                Diary.note("resume fetch " + id + ": " + (error.getCause() != null ? error.getCause() : error));
            }
        });
    }

    /**
     * A feed page passed through Feed.getItems: the saved video goes first in it, once. A page of bare videos only (the
     * feeds); a page that already has it is left alone. If the fetch was not back for the first page, the next one takes it.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void into(java.util.List items) {
        Object post = waitingPost;
        if (post == null || placed || items == null || items.isEmpty()) return;
        try {
            if (!(items.get(0) instanceof com.ss.android.ugc.aweme.feed.model.Aweme)) return;
            String id = NativeRead.aid(post);
            for (Object item : items) if (id.equals(NativeRead.aid(item))) { placed = true; return; }
            items.add(0, post);
            placed = true;
            Diary.note("resume: " + id + " put first in the feed");
        } catch (Throwable error) { Diary.note("resume into feed: " + error); }
    }

    /** A video became the one playing (PlayerTools.resumed). */
    static void playing(String id, Object manager) {
        if (id == null || id.isEmpty() || manager == null) return;
        aid = id;
        player = new WeakReference<>(manager);
        String want = wantedAid;
        if (want == null) return;
        if (SystemClock.uptimeMillis() > wantedUntil) { wantedAid = null; cold = false; return; }
        if (want.equals(id)) {
            boolean fromFeed = cold && placed;
            wantedAid = null;
            cold = false;
            seekWhenReady(manager, id, wantedAt);
            // a quiet word that this is the video from before, not a new one
            if (fromFeed && !saidSo) { saidSo = true; Screen.say(Text.t("Продолжаем видео, на котором вы остановились")); }
            return;
        }
        // a fresh feed after a plain launch: the saved video is on its way into it (see into); wait for it
        if (cold && plainStart) return;
        // back from the background on another video: TikTok (or the person) chose it, it stays
        if (!cold) wantedAid = null;
    }

    /** A TikTok screen paused: write down the video and the second. */
    static void remember() {
        if (!on() || aid.isEmpty()) return;
        // still finding the way back: the saved place stays until the video gets there
        if (wantedAid != null && SystemClock.uptimeMillis() <= wantedUntil) return;
        Object manager = player.get();
        if (manager == null) return;
        try {
            long at = NativeRead.number(NativeRead.get(manager, "getCurrentPosition"));
            long duration = NativeRead.number(NativeRead.get(manager, "getDuration"));
            if (duration <= 0 || at < 0 || at > duration) return;
            SharedPreferences p = prefs();
            if (p != null) p.edit().putString(AID, aid).putLong(AT, at).putLong(WHEN, System.currentTimeMillis()).apply();
        } catch (Throwable error) { Diary.note("resume save: " + error); }
    }

    /** Once the player knows its length: if the video started over, move it to where it was. */
    private static void seekWhenReady(final Object manager, final String id, final long target) {
        final long began = SystemClock.uptimeMillis();
        main.post(new Runnable() {
            @Override public void run() {
                // swiped on in the meantime
                if (!id.equals(aid) || player.get() != manager) return;
                long duration = NativeRead.number(NativeRead.get(manager, "getDuration"));
                if (duration <= 0) {
                    if (SystemClock.uptimeMillis() - began < 5000L) main.postDelayed(this, 120L);
                    return;
                }
                long at = NativeRead.number(NativeRead.get(manager, "getCurrentPosition"));
                // TikTok kept its place by itself, or the place was the very end
                if (target > duration - END || at >= target - END) return;
                try {
                    NativeRead.floating(manager, "seek", PlayerTools.percent(target, duration));
                    Diary.note("resume: " + id + " at " + target + " ms");
                } catch (Throwable error) { Diary.note("resume seek: " + error); }
            }
        });
    }
}
