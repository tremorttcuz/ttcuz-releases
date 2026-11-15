package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Switches that are on from the start. A changed default does not reach a phone
 * that already stored "off", so the new ones are written once, here.
 */
final class Defaults {
    private Defaults() {}

    private static final int VERSION = 1;
    private static final String KEY_VERSION = "defaultsVersion";
    private static volatile boolean checked;

    static synchronized void apply(Context context) {
        if (checked || context == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
            if (prefs.getInt(KEY_VERSION, 0) >= VERSION) { checked = true; return; }
            checked = prefs.edit()
                    .putBoolean(TextLimits.COMMENTS, true)
                    .putBoolean(TextLimits.REPOSTS, true)
                    .putBoolean(TextLimits.HASHTAGS, true)
                    .putBoolean(Dates.KEY, true)
                    .putBoolean(Download.KEY, true)
                    .putBoolean(Download.KEY_ALWAYS, true)
                    .putBoolean(Comments.KEY_COPY, true)
                    .putBoolean(Flags.KEY_BANNER, true)
                    .putInt(KEY_VERSION, VERSION)
                    .commit();
        } catch (Throwable error) {
            Diary.note("defaults: " + error.getClass().getSimpleName());
        }
    }
}
