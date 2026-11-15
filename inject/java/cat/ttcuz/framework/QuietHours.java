package cat.ttcuz.framework;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.Locale;

/**
 * Quiet hours: between two times of day (across midnight too) TikTok's notifications still arrive, but without sound,
 * vibration or a heads-up -- nothing is lost, it simply waits in the shade.
 *
 * A notification is made silent by putting it in a group of its own whose alert goes to the summary alone (and there
 * is no summary): the system's own way to let a notification in quietly, on every Android since 8.
 */
final class QuietHours {
    private QuietHours() {}

    static final String ON = "quiet_hours_on", FROM = "quiet_hours_from", TO = "quiet_hours_to";
    /** Minutes after midnight: 23:00 to 08:00 until chosen otherwise. */
    static final int DEFAULT_FROM = 23 * 60, DEFAULT_TO = 8 * 60;
    private static final String GROUP = "ttcuz.quiet";

    private static SharedPreferences prefs() {
        Context context = Ttcuz.context();
        return context == null ? null : AccountAppearance.prefs(context);
    }

    static boolean enabled() { SharedPreferences p = prefs(); return p != null && p.getBoolean(ON, false); }
    static int from() { SharedPreferences p = prefs(); return p == null ? DEFAULT_FROM : p.getInt(FROM, DEFAULT_FROM); }
    static int to() { SharedPreferences p = prefs(); return p == null ? DEFAULT_TO : p.getInt(TO, DEFAULT_TO); }

    static void set(boolean on) { SharedPreferences p = prefs(); if (p != null) p.edit().putBoolean(ON, on).apply(); }
    static void setRange(int from, int to) { SharedPreferences p = prefs(); if (p != null) p.edit().putInt(FROM, from).putInt(TO, to).apply(); }

    /** Whether now falls in the quiet hours (from is included, to is not; a range past midnight wraps). */
    static boolean now() {
        if (!enabled()) return false;
        Calendar clock = Calendar.getInstance();
        int minute = clock.get(Calendar.HOUR_OF_DAY) * 60 + clock.get(Calendar.MINUTE);
        int a = from(), b = to();
        if (a == b) return false;
        return a < b ? minute >= a && minute < b : minute >= a || minute < b;
    }

    /** The notification as it is, or -- in the quiet hours -- the same one, quiet. */
    static Notification soften(Notification notification) {
        if (notification == null || android.os.Build.VERSION.SDK_INT < 26 || !now()) return notification;
        try {
            Context context = Ttcuz.context();
            if (context == null) return notification;
            Notification.Builder builder = Notification.Builder.recoverBuilder(context, notification);
            builder.setGroup(GROUP).setGroupAlertBehavior(Notification.GROUP_ALERT_SUMMARY).setOnlyAlertOnce(true);
            return builder.build();
        } catch (Throwable error) {
            Diary.note("quiet hours: " + error);
            return notification;
        }
    }

    /** "23:00 – 08:00". */
    static String range() { return clock(from()) + " – " + clock(to()); }

    static String clock(int minutes) { return String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60); }
}
