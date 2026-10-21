package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * What the mod saw, kept so it can be read off the phone.
 *
 * A mod cannot be attached to a debugger and nobody is going to run logcat
 * against a modded TikTok. So the few things worth knowing -- did the start-up
 * hook run, which screen was that, what was its view made of, did the row go in
 * -- are written down here and shown at the bottom of Ttcuz's own settings.
 *
 * It survives the app being killed, which is the whole point: reaching Ttcuz's
 * settings can mean leaving TikTok first, and a diary that lives only in memory
 * is empty by the time anyone reads it.
 */
public final class Diary {

    private Diary() {}

    private static final String KEY = "diary";
    /**
     * A file of its own. It used to be one more entry among the settings,
     * and saving a settings file means writing all of it: every few seconds
     * that something was noted, every setting on the phone was written out
     * again, and Android makes the screen wait for those writes whenever an
     * activity is left. On a phone whose settings had grown that was a
     * stall, again and again, that nothing short of clearing the app's data
     * made better.
     *
     * Not named after the mod's settings files: the settings screen watches
     * those to say whether a restart is owed, and this one changes by itself.
     */
    private static final String FILE = "diary_ttcuz";
    private static final int KEEP = 40;
    private static final int KEEP_DEV = 400;

    private static final List<String> LINES = new ArrayList<String>();
    private static boolean loaded;

    /**
     * Saving is held back, because writing is the expensive half.
     *
     * A note is cheap and there are a hundred places that write one. Rebuilding
     * the whole diary and handing it to the preferences on each of those is
     * work in the middle of whatever the app was doing.
     */
    private static boolean dirty;
    private static long savedAt;
    private static final long REST = 3000;

    /** One formatter, because making one costs more than using it. */
    private static final SimpleDateFormat CLOCK = new SimpleDateFormat("HH:mm:ss", Locale.US);

    public static void note(String line) {
        String stamped = stamp() + "  " + line;
        synchronized (LINES) {
            load();
            String last = LINES.isEmpty() ? "" : LINES.get(LINES.size() - 1);
            if (last.length() > 10 && last.substring(10).equals(stamped.substring(10))) return;
            LINES.add(stamped);
            int keep = DevMode.isOn() ? KEEP_DEV : KEEP;
            while (LINES.size() > keep) LINES.remove(0);
            dirty = true;
            long now = android.os.SystemClock.uptimeMillis();
            if (now - savedAt >= REST) {
                savedAt = now;
                save();
            }
        }
    }

    /** A line only a developer wants. Written when the developer switch is on. */
    public static void detail(String line) {
        if (DevMode.isOn()) note("· " + line);
    }

    public static List<String> lines() {
        synchronized (LINES) {
            load();
            if (dirty) save();
            return new ArrayList<String>(LINES);
        }
    }

    public static void clear() {
        synchronized (LINES) {
            LINES.clear();
            loaded = true;
            save();
        }
    }

    // ------------------------------------------------------------- storage

    private static void load() {
        if (loaded) return;
        SharedPreferences prefs = prefs();
        if (prefs == null) return; // no context yet: try again next time
        loaded = true;
        String stored = prefs.getString(KEY, "");
        if (stored.length() == 0) {
            // what an older version kept among the settings: brought over
            // once, and taken out of there so it stops being written with them
            try {
                SharedPreferences settings = Ttcuz.context().getSharedPreferences(
                    Ttcuz.PREFS,
                    Context.MODE_PRIVATE
                );
                if (settings.contains(KEY)) {
                    stored = settings.getString(KEY, "");
                    settings.edit().remove(KEY).apply();
                }
            } catch (Throwable ignored) {}
        }
        if (stored.length() > 0) LINES.addAll(Arrays.asList(stored.split("\n")));
    }

    private static void save() {
        dirty = false;
        SharedPreferences prefs = prefs();
        if (prefs == null) return;
        StringBuilder out = new StringBuilder();
        for (String line : LINES) {
            if (out.length() > 0) out.append('\n');
            out.append(line);
        }
        prefs.edit().putString(KEY, out.toString()).apply();
    }

    private static SharedPreferences prefs() {
        try {
            Context context = Ttcuz.context();
            if (context == null) return null;
            return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String stamp() {
        synchronized (CLOCK) {
            return CLOCK.format(new Date());
        }
    }
}
