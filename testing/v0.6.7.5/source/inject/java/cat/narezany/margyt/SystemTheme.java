package cat.narezany.margyt;

import android.app.Activity;

/**
 * TikTok's own light/dark setting always starts from the system theme: on every launch the app-wide
 * night mode is put back to "follow system", and each screen that has its own override is reset too.
 * Done by reflection because AppCompat may be renamed or stripped in a given TikTok build; any failure
 * leaves TikTok exactly as it was.
 */
final class SystemTheme {
    private SystemTheme() {}

    private static final int FOLLOW_SYSTEM = -1;
    private static boolean appWide;

    /** Once per process, as soon as a context exists. */
    static synchronized void app() {
        if (appWide) return;
        appWide = true;
        try {
            Class<?> delegate = Class.forName("androidx.appcompat.app.AppCompatDelegate");
            delegate.getMethod("setDefaultNightMode", int.class).invoke(null, FOLLOW_SYSTEM);
        } catch (Throwable error) {
            Diary.note("system theme (app): " + error);
        }
    }

    /** Each screen: drop a per-screen night override, which would beat the app-wide mode. */
    static void screen(Activity activity) {
        if (activity == null) return;
        app();
        try {
            Object delegate = activity.getClass().getMethod("getDelegate").invoke(activity);
            if (delegate == null) return;
            Class<?> type = Class.forName("androidx.appcompat.app.AppCompatDelegate");
            if (!type.isInstance(delegate)) return;
            int now = (Integer) type.getMethod("getLocalNightMode").invoke(delegate);
            if (now != FOLLOW_SYSTEM && now != -100) type.getMethod("setLocalNightMode", int.class).invoke(delegate, FOLLOW_SYSTEM);
        } catch (Throwable ignored) {
            // not an AppCompat screen
        }
    }
}
