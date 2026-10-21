package cat.ttcuz.framework;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** TikTokYou's public supporter list; independent of the former plugin runtime. */
final class TtyBadges {

    static final String PICTURE = "tty:supporter";
    private static final String SOURCE = "https://tiktokyou.yzewe.ru/api/v1/badges";
    private static final Badges.Badge SUPPORTER = new Badges.Badge(
        "tty.supporter",
        PICTURE,
        0,
        "Поддержал ТТЮ",
        "Поддержал ТТЮ",
        "Понятно"
    );
    private static volatile String tag;
    private static Bitmap picture;

    private TtyBadges() {}

    static void start(Context context) {
        Context application = context.getApplicationContext();
        Net.away("TTY badge cache", () -> {
            picture(application);
            byte[] cache = Net.read(file(application));
            if (cache != null) apply(cache);
        });
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.post(
            new Runnable() {
                public void run() {
                    refresh(application);
                    handler.postDelayed(this, 10 * 60 * 1000L);
                }
            }
        );
    }

    static void refresh(Context context) {
        Net.away("TTY badges", () -> {
            Net.Answer answer = Net.fetch(SOURCE, tag);
            if (answer == null || answer.unchanged || answer.body == null) return;
            if (apply(answer.body)) {
                tag = answer.tag;
                Net.save(file(context), answer.body);
            }
        });
    }

    private static File file(Context context) {
        return new File(context.getFilesDir(), "ttcuz/tty-badges.json");
    }

    static boolean apply(byte[] bytes) {
        if (bytes.length > 2 * 1024 * 1024) return false;
        try {
            JSONArray rows = new JSONArray(new String(bytes, "UTF-8"));
            if (rows.length() > 50_000) return false;
            Map<String, Badges.Badge[]> users = new HashMap<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null) return false;
                String uid = row.optString("uid", "");
                if (!uid.matches("[0-9]{1,24}")) continue;
                String image = row.optString("badgeUrl", "").toLowerCase(java.util.Locale.ROOT);
                String text = row.optString("text", "").toLowerCase(java.util.Locale.ROOT);
                if (
                    row.optBoolean("isDeveloper", false) ||
                    image.equals("developer") ||
                    image.contains("etg.png") ||
                    image.contains("exteragram") ||
                    text.contains("exteragram")
                ) continue;
                users.put(uid, new Badges.Badge[] { SUPPORTER });
            }
            Badges.applyTty(users, SUPPORTER);
            return true;
        } catch (Exception error) {
            Diary.note("TTY badges: " + error);
            return false;
        }
    }

    static synchronized Bitmap picture(Context context) {
        if (picture != null) return picture;
        try (java.io.InputStream in = context.getAssets().open("ttcuz/tty-supporter.png")) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            Bitmap decoded = BitmapFactory.decodeStream(in, null, options);
            picture = decoded == null ? null : cat.ttcuz.framework.Badge.crop(decoded);
        } catch (Exception error) {
            Diary.note("TTY badge image: " + error);
        }
        return picture;
    }
}
