package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;

/** Outgoing presence and receipts; incoming status requests stay native. */
public final class Privacy {

    private Privacy() {}

    public static final String ONLINE = "privacy_hide_online";
    public static final String STORIES = "privacy_hide_story_views";
    public static final String READ = "privacy_hide_read";
    public static final String TYPING = "privacy_hide_typing";
    public static final String PROFILES = "privacy_hide_profile_views";
    public static final String SECTION = "Приватность";

    public static boolean enabled(String key) {
        Context context = Ttcuz.context();
        if (context == null) return false;
        try {
            return preferences(context).getBoolean(key, false);
        } catch (ClassCastException malformedPreference) {
            return false;
        }
    }

    public static void set(String key, boolean enabled) {
        if (
            !ONLINE.equals(key) &&
            !STORIES.equals(key) &&
            !READ.equals(key) &&
            !TYPING.equals(key) &&
            !PROFILES.equals(key)
        ) return;
        Context context = Ttcuz.context();
        if (context != null) preferences(context).edit().putBoolean(key, enabled).apply();
        PrivacyRuntime.settingsChanged();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
    }

    public static boolean hideOnline() {
        return enabled(ONLINE);
    }

    public static boolean hideStories() {
        return enabled(STORIES);
    }

    public static boolean hideRead() {
        return enabled(READ);
    }

    public static boolean hideTyping() {
        return enabled(TYPING);
    }

    public static boolean hideProfiles() {
        return enabled(PROFILES);
    }

    static boolean any() {
        return hideOnline() || hideStories() || hideRead() || hideTyping() || hideProfiles();
    }

    /** TikTok's status fetch also publishes our presence unless this is zero. */
    public static Integer fetchAsReport(Integer nativeValue) {
        return hideOnline() ? Integer.valueOf(0) : nativeValue;
    }
}
