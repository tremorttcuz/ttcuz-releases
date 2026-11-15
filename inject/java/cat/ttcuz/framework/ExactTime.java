package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * The date and time instead of "2 d." under a comment and in the list of who reposted a video.
 *
 * TikTok 47.2.41 writes those through two formatters (X.0J86.LJFF(ms) and X.0He9.LIZJ(ms, context)); the build sends
 * the calls made in the comment cell to {@link Comments} and those in the reposters' list to {@link Reposts}. Off, the
 * call goes on to TikTok's own formatter.
 */
public final class ExactTime {
    private ExactTime() {}

    static final String COMMENTS = "exact_comment_time", REPOSTS = "exact_repost_time";

    static boolean on(String key) {
        Context context = Ttcuz.context();
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(key, false);
    }

    static void set(String key, boolean value) {
        Context context = Ttcuz.context();
        if (context != null) context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply();
    }

    /** "14.03 18:42" this year, "14.03.2024 18:42" before it. */
    static String format(long millis) {
        if (millis <= 0) return "";
        Calendar then = Calendar.getInstance(), now = Calendar.getInstance();
        then.setTimeInMillis(millis);
        String pattern = then.get(Calendar.YEAR) == now.get(Calendar.YEAR) ? "dd.MM HH:mm" : "dd.MM.yyyy HH:mm";
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(millis));
    }

    private static volatile Method shortFormat, contextFormat;

    static String theirs(long millis) {
        try {
            Method method = shortFormat;
            if (method == null) shortFormat = method = Class.forName("X.0J86").getMethod("LJFF", long.class);
            return (String) method.invoke(null, millis);
        } catch (Throwable error) {
            return format(millis);
        }
    }

    static String theirs(long millis, Context context) {
        try {
            Method method = contextFormat;
            if (method == null) contextFormat = method = Class.forName("X.0He9").getMethod("LIZJ", long.class, Context.class);
            return (String) method.invoke(null, millis, context);
        } catch (Throwable error) {
            return format(millis);
        }
    }

    /** The comment cell's two time formatters. */
    public static final class Comments {
        private Comments() {}
        public static String LJFF(long millis) { return on(COMMENTS) ? format(millis) : theirs(millis); }
        public static String LIZJ(long millis, Context context) { return on(COMMENTS) ? format(millis) : theirs(millis, context); }
    }

    /** The reposters' list. */
    public static final class Reposts {
        private Reposts() {}
        public static String LJFF(long millis) { return on(REPOSTS) ? format(millis) : theirs(millis); }
        public static String LIZJ(long millis, Context context) { return on(REPOSTS) ? format(millis) : theirs(millis, context); }
        public static String LJIIJ(long millis) { return on(REPOSTS) ? format(millis) : theirsShort(millis); }
    }

    private static volatile Method shorterFormat;

    /** X.0J86.LJIIJ(ms): the repost lists' own "2d". */
    static String theirsShort(long millis) {
        try {
            Method method = shorterFormat;
            if (method == null) shorterFormat = method = Class.forName("X.0J86").getMethod("LJIIJ", long.class);
            return (String) method.invoke(null, millis);
        } catch (Throwable error) {
            return format(millis);
        }
    }
}
