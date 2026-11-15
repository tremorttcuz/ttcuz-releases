package cat.ttcuz.framework;

import android.content.SharedPreferences;

import com.bytedance.ies.abmock.SettingsManager;

import java.util.HashMap;
import java.util.Map;

/**
 * The switches TikTok ships turned off for you.
 *
 * Every feature in the app is behind a named flag, and the value of each is
 * decided per account on the server. Two people on the same version get
 * different apps: voice comments, the splash advertisement, features already
 * written and shipped and simply not switched on for every account.
 *
 * The flag is read through one class whose name is real,
 * `com.bytedance.ies.abmock.SettingsManager`, with one static per type -- no
 * receiver, so a rewritten read keeps the very shape it had. Those reads come
 * through here instead: a flag on the list below is
 * answered by the mod, and every other flag in the app -- there are tens of
 * thousands -- is passed straight back to TikTok untouched.
 *
 * Each override belongs to a group with a switch of its own, so this is a
 * menu rather than a patch: nothing here is forced on anybody.
 *
 * Every flag name below was checked against 46.9.42 rather than copied from a
 * list. One that a later release drops is simply never asked for.
 */
public final class Flags {

    private Flags() {}

    // the groups, each with a switch on the mod's screen
    public static final int ADS = 0;
    public static final int VOICE = 1;
    public static final int BANNER = 2;
    /** TikTok's left-aligned profile experiment, off: the classic centred header (on by default). */
    public static final int CLASSIC_PROFILE = 3;
    /** Danmaku: comments flying over the video, and in landscape. */
    public static final int DANMAKU = 4;

    public static final String KEY_VOICE = "flag_voice";
    public static final String KEY_BANNER = "flag_profile_banner";
    public static final String KEY_CLASSIC_PROFILE = "flag_classic_profile";
    public static final String KEY_DANMAKU = "flag_danmaku";

    private static final class Override {
        final int group;
        final Object value;

        Override(int group, Object value) {
            this.group = group;
            this.value = value;
        }
    }

    private static final Map<String, Override> OVERRIDES = new HashMap<String, Override>();

    private static void put(int group, Object value, String... keys) {
        for (String key : keys) OVERRIDES.put(key, new Override(group, value));
    }

    static {
        // A splash advertisement never becomes a post, so the feed filter never
        // sees it. This is the only place it can be turned off.
        put(ADS, Boolean.FALSE,
                "enable_normal_splash_ad", "enable_normal_splash_ad_ab", "splash_ad_enable",
                "enable_live_splash", "search_ad_refactor_enable", "search_enable_mix_adgap",
                "commerce_enable");

        put(VOICE, Integer.valueOf(1), "audio_comment_publish");
        put(BANNER,Integer.valueOf(1),"profile_bg_in_allow_list","profile_bg_enable_consumption_group");
        put(CLASSIC_PROFILE, Integer.valueOf(0), "tux_profile_icon_update", "profile_left_align", "profile_left_align_ab_group",
                "profile_left_align_avatar_and_info_merge", "profile_left_align_pronouns", "profile_left_align_recommend_card",
                "profile_left_align_avatar_opt", "profile_left_align_avatar_at_right_opt",
                "profile_left_align_right_avatar_nickname_opt_v2", "enable_profile_left_align_second_line_two_lines_show",
                "profile_left_align_relation_info_font_size_opt", "profile_left_align_right_avatar_large_font_opt",
                "profile_left_align_bio_style_opt", "profile_left_align_cta_style_opt",
                "profile_left_align_advance_feature_style_opt", "profile_left_align_right_avatar_nickname_wordbreak_opt");
        put(DANMAKU, Integer.valueOf(1), "danmaku_exp", "landscape_support_danmaku");
    }

    // ------------------------------------------------------------ the switches

    public static boolean isOn(int group) {
        if (group == ADS) return Feed.isEnabled();
        if (group == BANNER) return true; // always on, no switch on the screen
        if (group == CLASSIC_PROFILE) return false; // the classic-profile option is gone: TikTok's own profile
        if (group == DANMAKU) return true; // running comments are always on, no switch on the screen
        return bool(keyOf(group));
    }

    private static String keyOf(int group) {
        switch (group) {
            case BANNER: return KEY_BANNER;
            case CLASSIC_PROFILE: return KEY_CLASSIC_PROFILE;
            case DANMAKU: return KEY_DANMAKU;
            default: return KEY_VOICE;
        }
    }

    public static boolean isOn(String key) {
        if (KEY_DANMAKU.equals(key)) return true;
        return bool(key);
    }

    public static void set(String key, boolean on) {
        SharedPreferences prefs = prefs();
        if (prefs != null) prefs.edit().putBoolean(key, on).apply();
    }

    private static boolean bool(String key) {
        try {
            SharedPreferences prefs = prefs();
            return prefs != null && prefs.getBoolean(key, key.equals(KEY_VOICE) || key.equals(KEY_BANNER) || key.equals(KEY_CLASSIC_PROFILE)
                    || key.equals("unlimited_comments") || key.equals("unlimited_reposts")
                    || key.equals("unlimited_hashtags"));
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** TikTok's own (left-aligned) profile is always shown: the classic-profile option was removed. */
    public static boolean newProfile() {
        return true;
    }

    private static SharedPreferences prefs() {
        android.content.Context context = Ttcuz.context();
        if (context == null) return null;
        return context.getSharedPreferences(Ttcuz.PREFS, android.content.Context.MODE_PRIVATE);
    }

    /** The override for a flag, or null when the mod has nothing to say. */
    private static Object of(String key) {
        Override override = OVERRIDES.get(key);
        if (override == null) return null;
        if (override.group == CLASSIC_PROFILE) {
            if (!isOn(CLASSIC_PROFILE)) return null; // nothing forced: TikTok decides, as without the mod
            // The left-aligned profile is TikTok's new one; the switch picks between it and the classic layout.
            if ("profile_left_align_ab_group".equals(key)) return null;
            if ("profile_left_align".equals(key)) return Integer.valueOf(newProfile() ? 1 : 0);
            if (!newProfile()) return Integer.valueOf(0);
            if ("profile_left_align_avatar_and_info_merge".equals(key) || "profile_left_align_pronouns".equals(key)
                    || "profile_left_align_recommend_card".equals(key)) return Integer.valueOf(1);
            return "profile_left_align_avatar_opt".equals(key) && ProfileHeader.oldGroup() ? Integer.valueOf(1) : null;
        }
        return isOn(override.group) ? override.value : null;
    }

    private static Object builtIn(String key) {
        if ("tt_comment_repost".equals(key) || "tt_comment_repost_interaction".equals(key)
                || "tt_audio_comment_repost".equals(key) || "enable_self_repost".equals(key) || "long_press_speed_up_lock".equals(key)) return Integer.valueOf(1);
        return of(key);
    }
    public static Boolean forcedBoolean(String key) {Object v=builtIn(key);return v instanceof Number?Boolean.valueOf(((Number)v).doubleValue()!=0):v instanceof Boolean?(Boolean)v:null;}
    public static Integer forcedInt(String key) {Object v=builtIn(key);return v instanceof Number?Integer.valueOf(((Number)v).intValue()):v instanceof Boolean?Integer.valueOf((Boolean)v?1:0):null;}
    public static Long forcedLong(String key) {Object v=builtIn(key);return v instanceof Number?Long.valueOf(((Number)v).longValue()):null;}
    public static Float forcedFloat(String key) {Object v=builtIn(key);return v instanceof Number?Float.valueOf(((Number)v).floatValue()):null;}
    public static Double forcedDouble(String key) {Object v=builtIn(key);return v instanceof Number?Double.valueOf(((Number)v).doubleValue()):null;}
    public static String forcedString(String key) {Object v=builtIn(key);return v instanceof String?(String)v:null;}

    /** Verified 47.2.41 experiment reader; only three literal-key callers are patched. */
    public static int nativeInt(Object reader,int scope,int fallback,String key,boolean cache){
        Object override=builtIn(key);if(override instanceof Integer)return ((Integer)override).intValue();
        if(reader==null)return fallback;
        try{java.lang.reflect.Method method=reader.getClass().getMethod("LJIIJJI",Integer.TYPE,Integer.TYPE,String.class,Boolean.TYPE);method.setAccessible(true);return ((Number)method.invoke(reader,scope,fallback,key,cache)).intValue();}catch(Throwable error){return fallback;}
    }
    // ------------------------------------------------- where the reads land

    public static boolean flag(String key, boolean fallback) {
        Object value = builtIn(key);
        if (value instanceof Boolean) return ((Boolean) value).booleanValue();
        if (value instanceof Integer) return ((Integer) value).intValue() != 0;
        return SettingsManager.LIZ(key, fallback);
    }

    public static int flag(String key, int fallback) {
        Object value = builtIn(key);
        if (value instanceof Integer) return ((Integer) value).intValue();
        if (value instanceof Boolean) return ((Boolean) value).booleanValue() ? 1 : 0;
        return SettingsManager.LJ(key, fallback);
    }

    public static long flag(String key, long fallback) {
        Object value = builtIn(key);
        if (value instanceof Integer) return ((Integer) value).intValue();
        return SettingsManager.LJFF(key, fallback);
    }

    public static String flag(String key, String fallback) {
        Object value = builtIn(key);
        if (value instanceof String) return (String) value;
        return SettingsManager.LJI(key, fallback);
    }

    public static float flag(String key, float fallback) {
        Object value = builtIn(key);
        if (value instanceof Integer) return ((Integer) value).intValue();
        return SettingsManager.LIZJ(key, fallback);
    }

    public static double flag(String key, double fallback) {
        Object value = builtIn(key);
        if (value instanceof Integer) return ((Integer) value).intValue();
        return SettingsManager.LIZIZ(key, fallback);
    }
}
