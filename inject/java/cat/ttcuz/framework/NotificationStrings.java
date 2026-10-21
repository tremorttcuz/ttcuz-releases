package cat.ttcuz.framework;

import android.content.Context;
import android.content.res.Resources;
import java.util.Locale;

/** Only the native IM notification preview uses this missing-string fallback. */
public final class NotificationStrings {

    private NotificationStrings() {}

    public static String getString(Context context, int id) {
        try {
            return context.getString(id);
        } catch (Resources.NotFoundException missing) {
            // Never reinterpret drawable/colour ids or suppress unrelated failures.
            if ((id & 0xffff0000) != 0x7f110000) throw missing;
            String language = Locale.getDefault().getLanguage();
            if ("ru".equals(language)) return "Новое сообщение";
            if ("uk".equals(language)) return "Нове повідомлення";
            return "New message";
        }
    }
}
