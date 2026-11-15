package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Videos already seen do not come back in For You.
 *
 * TikTok now and then shows a video again. Every video the feed lands on is remembered here (at once, or after it
 * has played a few seconds) and left out of the next pages Feed reads. The list lives in a small file of ids and
 * times; it is bounded, and entries older than the chosen span are forgotten.
 */
final class WatchedVideos {
    private WatchedVideos() {}

    static final String KEY_ON = "watched_hide", KEY_KEEP = "watched_keep_days", KEY_AFTER = "watched_after_seconds";
    /** How long a video is remembered, in days (0: always), and after how many seconds of playing it counts. */
    static final int[] KEEP_DAYS = {0, 1, 7, 30};
    static final int[] AFTER_SECONDS = {0, 3, 5, 10};
    private static final int MOST = 20000;

    private static final Handler main = new Handler(Looper.getMainLooper());
    private static final LinkedHashMap<String, Long> seen = new LinkedHashMap<>(256, .75f, false);
    private static volatile boolean loaded, loading;
    private static boolean dirty, saving;
    private static String waiting;
    private static final Runnable count = () -> {
        String id = waiting;
        waiting = null;
        if (id != null && id.equals(VideoActions.currentId())) mark(id);
    };

    private static SharedPreferences prefs() {
        Context context = Ttcuz.context();
        return context == null ? null : context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
    }

    static boolean enabled() {
        SharedPreferences store = prefs();
        return store != null && store.getBoolean(KEY_ON, false);
    }

    static void setEnabled(boolean on) {
        SharedPreferences store = prefs();
        if (store != null) store.edit().putBoolean(KEY_ON, on).apply();
        if (on) load();
    }

    static int keepDays() {
        SharedPreferences store = prefs();
        return store == null ? 0 : store.getInt(KEY_KEEP, 0);
    }

    static void setKeepDays(int days) {
        SharedPreferences store = prefs();
        if (store != null) store.edit().putInt(KEY_KEEP, days).apply();
    }

    static int afterSeconds() {
        SharedPreferences store = prefs();
        return store == null ? 0 : store.getInt(KEY_AFTER, 0);
    }

    static void setAfterSeconds(int seconds) {
        SharedPreferences store = prefs();
        if (store != null) store.edit().putInt(KEY_AFTER, seconds).apply();
    }

    static int size() {
        synchronized (seen) { return seen.size(); }
    }

    /** The feed landed on a video. */
    static void selected(Object post) {
        if (post == null || !enabled() || Feed.live(post)) return;
        String id = NativeRead.aid(post);
        if (id == null || id.isEmpty() || id.equals("0")) return;
        load();
        int after = afterSeconds();
        main.removeCallbacks(count);
        if (after <= 0) { mark(id); return; }
        waiting = id;
        main.postDelayed(count, after * 1000L);
    }

    /** Whether Feed should leave this post out. */
    static boolean hides(Object post) {
        // a live room is not a video one has seen: only the "hide live" switch leaves it out
        if (!loaded || Feed.live(post)) return false;
        String id = NativeRead.aid(post);
        if (id == null || id.isEmpty() || id.equals("0")) return false;
        Long when;
        synchronized (seen) { when = seen.get(id); }
        if (when == null) return false;
        int days = keepDays();
        return days <= 0 || System.currentTimeMillis() - when < days * 86_400_000L;
    }

    static void forget() {
        synchronized (seen) { seen.clear(); dirty = true; }
        save();
    }

    private static void mark(String id) {
        synchronized (seen) {
            seen.remove(id);
            seen.put(id, System.currentTimeMillis());
            if (seen.size() > MOST) {
                Iterator<String> oldest = seen.keySet().iterator();
                while (seen.size() > MOST && oldest.hasNext()) { oldest.next(); oldest.remove(); }
            }
            dirty = true;
        }
        save();
    }

    private static File file() {
        Context context = Ttcuz.context();
        return context == null ? null : new File(context.getFilesDir(), "ttcuz/watched.txt");
    }

    private static void load() {
        if (loaded || loading) return;
        loading = true;
        Net.away("watched videos", () -> {
            try {
                File from = file();
                if (from != null && from.isFile()) {
                    String text = new String(Net.read(from), java.nio.charset.StandardCharsets.UTF_8);
                    long cutoff = keepDays() > 0 ? System.currentTimeMillis() - keepDays() * 86_400_000L : 0L;
                    synchronized (seen) {
                        for (String line : text.split("\n")) {
                            int space = line.indexOf(' ');
                            if (space <= 0) continue;
                            try {
                                long when = Long.parseLong(line.substring(space + 1).trim());
                                if (when >= cutoff && !seen.containsKey(line.substring(0, space))) seen.put(line.substring(0, space), when);
                            } catch (NumberFormatException ignored) { }
                        }
                    }
                }
            } catch (Throwable error) {
                Diary.note("watched videos: " + error);
            } finally {
                loaded = true;
                loading = false;
            }
        });
    }

    /** Written a moment after the last change, off the main thread, through a temporary file. */
    private static void save() {
        synchronized (seen) {
            if (saving) return;
            saving = true;
        }
        main.postDelayed(() -> Net.away("watched videos save", () -> {
            StringBuilder out = new StringBuilder();
            synchronized (seen) {
                saving = false;
                if (!dirty) return;
                dirty = false;
                for (Map.Entry<String, Long> entry : seen.entrySet()) out.append(entry.getKey()).append(' ').append(entry.getValue()).append('\n');
            }
            try {
                File to = file();
                if (to == null) return;
                if (to.getParentFile() != null) to.getParentFile().mkdirs();
                File temporary = new File(to.getPath() + ".tmp");
                Net.save(temporary, out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                if (!temporary.renameTo(to)) temporary.delete();
            } catch (Throwable error) {
                Diary.note("watched videos save: " + error);
            }
        }), 2000L);
    }
}
