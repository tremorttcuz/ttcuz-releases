package cat.ttcuz.framework;

import android.content.SharedPreferences;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * A fake flame for a chat: the number of days TikTok shows, and which of its own flames (lit orange, fading grey).
 *
 * TikTok's streaks come from Gson, which makes them without running a constructor, so nothing is changed in the
 * objects themselves. Instead every place in TikTok that reads a streak's number or the two times its flame's state
 * comes from is patched to ask here (dexpatch: "streak fake read"): a chat with a fake gets the fake, every other
 * chat its own value. TikTok then picks the milestone art and the flame for that by itself. The mod's own streak
 * keeping (Streaks) reads the fields by reflection, so it always sees the real streak and renews it as before.
 * Nothing is sent anywhere; only this phone sees it.
 */
public final class StreakFake {
    private StreakFake() {}

    /** keep the flame as it is, lit orange, fading grey */
    public static final int AS_IS = 0, ORANGE = 1, GREY = 2;
    private static final String KEY = "streak_fake_";
    private static volatile Map<String, int[]> settings;

    private static SharedPreferences prefs() {
        android.content.Context c = Ttcuz.context();
        return c == null ? null : c.getSharedPreferences(Ttcuz.PREFS, 0);
    }

    /** {days (-1: as is), colour} for a chat, or null when it has no fake. */
    public static int[] of(String conversation) {
        if (conversation == null) return null;
        Map<String, int[]> all = settings;
        if (all == null) {
            all = new HashMap<>();
            SharedPreferences p = prefs();
            if (p != null) for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
                if (!e.getKey().startsWith(KEY) || !(e.getValue() instanceof String)) continue;
                String[] parts = ((String) e.getValue()).split("\\|");
                try { all.put(e.getKey().substring(KEY.length()), new int[]{Integer.parseInt(parts[0]), parts.length > 1 ? Integer.parseInt(parts[1]) : AS_IS}); }
                catch (NumberFormatException ignored) { }
            }
            settings = all;
        }
        return all.get(conversation);
    }

    /** Sets a chat's fake (days -1 keeps the real number); null removes it. Applies from the next time TikTok reads the chat. */
    static void set(String conversation, int[] fake) {
        SharedPreferences p = prefs();
        if (p == null || conversation == null) return;
        if (fake == null || (fake[0] < 0 && fake[1] == AS_IS)) p.edit().remove(KEY + conversation).apply();
        else p.edit().putString(KEY + conversation, Math.max(-1, Math.min(99999, fake[0])) + "|" + fake[1]).apply();
        settings = null;
        Diary.note("streak fake: " + (fake == null ? "off" : fake[0] + " days, colour " + fake[1]));
    }

    /** The streak as the server sent it: the objects are never changed, so it is the object itself. */
    public static Object real(Object data) { return data; }

    /** The real number of days of a chat, or -1 when it is not known. */
    static int realDays(String conversation) {
        Object kept;
        synchronized (seen) { kept = seen.get(conversation); }
        return kept == null ? -1 : (int) raw(kept, STREAK);
    }

    private static String conversation(Object data) {
        Object c = data == null ? null : NativeRead.field(data, "convId");
        return c instanceof String ? (String) c : null;
    }

    /** No longer used: kept so an older patched constructor still finds it. */
    public static void adjust(Object data) { }

    // ------------------------------------------------------------------ the patched reads

    private static final int STREAK = 0, ACTIVE_BEFORE = 1, END_AT = 2;
    private static final String[] NAMES = {"streak", "activeBefore", "endAt"};
    private static volatile Field[] fields;
    /** conversation → the last streak TikTok read for it (for the settings sheet's "real" number) */
    private static final Map<String, Object> seen = new HashMap<>();

    private static long raw(Object data, int which) {
        try {
            Field[] f = fields;
            if (f == null || f[0].getDeclaringClass() != data.getClass()) {
                f = new Field[3];
                for (int i = 0; i < 3; i++) { f[i] = data.getClass().getDeclaredField(NAMES[i]); f[i].setAccessible(true); }
                fields = f;
            }
            return which == STREAK ? f[which].getInt(data) : f[which].getLong(data);
        } catch (Throwable error) {
            return 0;
        }
    }

    /** TikTok reading a streak's number of days. */
    public static int streak(Object data) {
        int real = (int) raw(data, STREAK);
        int[] fake = fake(data);
        return fake != null && fake[0] >= 0 ? fake[0] : real;
    }

    /** TikTok reading until when the flame is lit. */
    public static long activeBefore(Object data) {
        long real = raw(data, ACTIVE_BEFORE);
        int[] fake = fake(data);
        if (fake == null || fake[1] == AS_IS) return real;
        long unit = raw(data, END_AT) > 100000000000L ? 1000L : 1L, now = System.currentTimeMillis() / 1000L;
        return (fake[1] == ORANGE ? now + 2 * 86400L : now - 60L) * unit;
    }

    /** TikTok reading until when the flame can still be saved. */
    public static long endAt(Object data) {
        long real = raw(data, END_AT);
        int[] fake = fake(data);
        if (fake == null || fake[1] == AS_IS) return real;
        long unit = real > 100000000000L ? 1000L : 1L, now = System.currentTimeMillis() / 1000L;
        return (fake[1] == ORANGE ? now + 3 * 86400L : now + 86400L) * unit;
    }

    private static int[] fake(Object data) {
        if (data == null) return null;
        String conversation = conversation(data);
        if (conversation == null) return null;
        int[] fake = of(conversation);
        if (fake != null) synchronized (seen) { if (seen.size() > 256) seen.clear(); seen.put(conversation, data); }
        return fake;
    }
}
