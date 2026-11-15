package cat.ttcuz.framework;

import android.content.Context;
import android.content.res.Resources;
import java.util.Locale;

/**
 * TikTok's in-app message banners (X.0GbU, restored through data/transplants.json) ask for strings TikTok 47.2.41's
 * own resource shrinking removed; without this every such banner crashed the app with NotFoundException. A missing
 * string of the string type becomes the mod's own words instead.
 */
public final class NotificationStrings {
    private NotificationStrings() {
    }

    /** The banner's right-hand button ("Reply"), the string that crashed it most. */
    private static final int REPLY = 0x7f1140b9;

    public static String getString(Context context, int i) {
        try {
            return context.getString(i);
        } catch (Resources.NotFoundException e) {
            if ((i & (-65536)) != 2131820544) {
                throw e;
            }
            String language = Locale.getDefault().getLanguage();
            if (i == REPLY) return "ru".equals(language) ? "Ответить" : "uk".equals(language) ? "Відповісти" : "Reply";
            return "ru".equals(language) ? "Новое сообщение" : "uk".equals(language) ? "Нове повідомлення" : "New message";
        }
    }
}
