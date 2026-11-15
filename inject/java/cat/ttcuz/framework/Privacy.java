package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;

public final class Privacy {
    public static final String ONLINE = "privacy_hide_online";
    public static final String PROFILES = "privacy_hide_profile_views";
    public static final String READ = "privacy_hide_read";
    public static final String SECTION = "Приватность";
    public static final String STORIES = "privacy_hide_story_views";
    public static final String TYPING = "privacy_hide_typing";

    private Privacy() {
    }

    public static boolean enabled(String str) {
        Context context = Ttcuz.context();
        if (context == null) {
            return false;
        }
        try {
            return preferences(context).getBoolean(str, false);
        } catch (ClassCastException unused) {
            return false;
        }
    }

    public static void set(String str, boolean z) {
        if (ONLINE.equals(str) || STORIES.equals(str) || READ.equals(str) || TYPING.equals(str) || PROFILES.equals(str)) {
            Context context = Ttcuz.context();
            if (context != null) {
                preferences(context).edit().putBoolean(str, z).apply();
            }
            PrivacyRuntime.settingsChanged();
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("ttcuz", 0);
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

    public static Integer fetchAsReport(Integer num) {
        if (hideOnline()) {
            return 0;
        }
        return num;
    }
}
