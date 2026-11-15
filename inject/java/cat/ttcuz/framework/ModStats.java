package cat.ttcuz.framework;

import android.content.Context;
import org.json.JSONObject;

/**
 * How many people use ttcuz: right now, today, this week, and how often store plugins were downloaded.
 *
 * No request of its own while TikTok is used: the badges are asked for every three minutes anyway, and once in ten
 * minutes that request carries this phone's random id (made here, never tied to the account; the server keeps only
 * its hash). The totals are fetched only when the settings show them, at most once a minute.
 */
final class ModStats {
    private ModStats() {}

    static final String HEADER = "X-Ttcuz-Here";
    private static final String KEY = "presence_id";
    private static final long EVERY = 10 * 60 * 1000L, FRESH = 60 * 1000L;
    private static long said;

    /** The random id to send with this request, or null when it was sent in the last ten minutes. */
    static synchronized String here(Context context) {
        long now = System.currentTimeMillis();
        if (context == null || now - said < EVERY) return null;
        android.content.SharedPreferences prefs = context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
        String id = prefs.getString(KEY, "");
        if (!id.matches("[0-9a-f]{32}")) {
            byte[] random = new byte[16];
            new java.security.SecureRandom().nextBytes(random);
            StringBuilder hex = new StringBuilder();
            for (byte b : random) hex.append(String.format(java.util.Locale.ROOT, "%02x", b));
            id = hex.toString();
            prefs.edit().putString(KEY, id).apply();
        }
        said = now;
        return id;
    }

    /** The numbers as last fetched: now, today, this week, downloads; -1 while unknown. */
    static volatile long now = -1, day = -1, week = -1, downloads = -1;
    private static volatile long fetched;
    private static volatile boolean asking;

    interface Then { void got(); }

    /** Fetches the numbers when they are older than a minute; then runs on the main thread. */
    static void refresh(final Then then) {
        if (System.currentTimeMillis() - fetched < FRESH) { if (then != null) then.got(); return; }
        if (asking) return;
        asking = true;
        Net.away("mod stats", () -> {
            try {
                Net.Answer answer = Net.fetch(Server.ROOT + "/v1/stats", null);
                if (answer != null && answer.body != null) {
                    JSONObject json = new JSONObject(new String(answer.body, "UTF-8"));
                    now = json.optLong("now", 0); day = json.optLong("day", 0);
                    week = json.optLong("week", 0); downloads = json.optLong("downloads", 0);
                    fetched = System.currentTimeMillis();
                }
            } catch (Throwable error) { Diary.note("mod stats: " + error); }
            finally { asking = false; }
            if (then != null) new android.os.Handler(android.os.Looper.getMainLooper()).post(then::got);
        });
    }

    /** Whether the "N сейчас в ttcuz" pill shows under the title (hidden by holding it, back from Tools). */
    static boolean shown(Context context) {
        return context == null || context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).getBoolean("online_chip", true);
    }

    static void setShown(Context context, boolean on) {
        if (context != null) context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).edit().putBoolean("online_chip", on).apply();
    }

    /** 1234 → "1 234", as Russian writes long numbers. */
    static String number(long value) {
        if (value < 0) return "…";
        String digits = String.valueOf(value);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) out.append(' ');
            out.append(digits.charAt(i));
        }
        return out.toString();
    }
}
