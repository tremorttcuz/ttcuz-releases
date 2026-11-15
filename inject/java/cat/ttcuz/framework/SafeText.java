package cat.ttcuz.framework;

import android.app.Activity;
import android.content.Context;

/**
 * TikTok's in-app push (the "shared a live with you" banner, X/0GbU) reads its words from a context whose resources
 * do not see TikTok's NxString tables, and the whole app closed on Resources$NotFoundException 0x7f11419f. The words
 * are looked up as asked, then through the screen on top (whose resources do see them); a banner without its words
 * is better than TikTok closing.
 */
public final class SafeText {
    private SafeText() {}

    public static String getString(Context context, int id) {
        try { return context.getString(id); } catch (RuntimeException missing) { /* below */ }
        Activity top = Screen.now();
        if (top != null && top != context) {
            try { return top.getString(id); } catch (RuntimeException alsoMissing) { /* below */ }
        }
        Diary.note("text " + Integer.toHexString(id) + " missing: left empty");
        return "";
    }
}
