package cat.ttcuz.framework;

import android.content.Context;

/**
 * The strip of suggested stickers over the chat's text field, while typing. On by TikTok's own choice; switched off,
 * its panel (TypingRecommendationPanelAssem) is never given a set of suggestions to show: the build makes each of its
 * methods that would show one ask {@link #enabled()} first. A change shows from the next suggestion on.
 */
public final class StickerStrip {
    private StickerStrip() {}

    static final String KEY = "dm_sticker_suggestions";
    public static boolean enabled() {
        Context context = Ttcuz.context();
        return context == null || context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true);
    }

    static void set(boolean on) {
        Context context = Ttcuz.context();
        if (context != null) context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply();
    }

}
