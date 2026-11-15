package cat.ttcuz.framework;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import cat.ttcuz.framework.Badges;
import cat.ttcuz.framework.Net;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

final class TtyBadges {
    private static final String SOURCE = "https://tiktokyou.yzewe.ru/api/v1/badges";
    private static Bitmap picture;
    private static volatile String tag;
    static final String PICTURE = "tty:supporter";
    private static final Badges.Badge SUPPORTER = new Badges.Badge("tty.supporter", PICTURE, 0, Text.t("Поддержал ТТЮ"), Text.t("Поддержал ТТЮ"), Text.t("Понятно"));

    private TtyBadges() {
    }

    static void start(Context context) {
        final Context applicationContext = context.getApplicationContext();
        Net.away("TTY badge cache", new Runnable() { // from class: cat.ttcuz.framework.TtyBadges$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                TtyBadges.lambda$start$0(applicationContext);
            }
        });
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() { // from class: cat.ttcuz.framework.TtyBadges.1
            @Override // java.lang.Runnable
            public void run() {
                // only while TikTok is on screen: in the background this woke the phone every ten minutes
                if (Screen.now() != null) TtyBadges.refresh(applicationContext);
                handler.postDelayed(this, 600000L);
            }
        });
    }

    static /* synthetic */ void lambda$start$0(Context context) {
        picture(context);
        byte[] read = Net.read(file(context));
        if (read != null) {
            apply(read);
        }
    }

    static void refresh(final Context context) {
        Net.away("TTY badges", new Runnable() { // from class: cat.ttcuz.framework.TtyBadges$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                TtyBadges.lambda$refresh$1(context);
            }
        });
    }

    static /* synthetic */ void lambda$refresh$1(Context context) {
        Net.Answer fetch = Net.fetch(SOURCE, tag);
        if (fetch == null || fetch.unchanged || fetch.body == null || !apply(fetch.body)) {
            return;
        }
        tag = fetch.tag;
        Net.save(file(context), fetch.body);
    }

    private static File file(Context context) {
        return new File(context.getFilesDir(), "ttcuz/tty-badges.json");
    }

    static boolean apply(byte[] bArr) {
        if (bArr.length > 2097152) {
            return false;
        }
        try {
            JSONArray jSONArray = new JSONArray(new String(bArr, "UTF-8"));
            if (jSONArray.length() > 50000) {
                return false;
            }
            HashMap hashMap = new HashMap();
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject optJSONObject = jSONArray.optJSONObject(i);
                if (optJSONObject == null) {
                    return false;
                }
                String optString = optJSONObject.optString("uid", "");
                if (Ids.uid(optString)) {
                    String lowerCase = optJSONObject.optString("badgeUrl", "").toLowerCase(Locale.ROOT);
                    String lowerCase2 = optJSONObject.optString("text", "").toLowerCase(Locale.ROOT);
                    if (!optJSONObject.optBoolean("isDeveloper", false) && !lowerCase.equals("developer") && !lowerCase.contains("etg.png") && !lowerCase.contains("exteragram") && !lowerCase2.contains("exteragram")) {
                        hashMap.put(optString, new Badges.Badge[]{SUPPORTER});
                    }
                }
            }
            Badges.applyTty(hashMap, SUPPORTER);
            return true;
        } catch (Exception e) {
            Diary.note("TTY badges: " + String.valueOf(e));
            return false;
        }
    }

    static synchronized Bitmap picture(Context context) {
        synchronized (TtyBadges.class) {
            Bitmap bitmap = picture;
            if (bitmap != null) {
                return bitmap;
            }
            try {
                InputStream open = context.getAssets().open("ttcuz/tty-supporter.png");
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4;
                    Bitmap bitmap2 = null;
                    Bitmap decodeStream = BitmapFactory.decodeStream(open, null, options);
                    if (decodeStream != null) {
                        bitmap2 = Badge.crop(decodeStream);
                    }
                    picture = bitmap2;
                    if (open != null) {
                        open.close();
                    }
                } catch (Throwable th) {
                    if (open != null) {
                        try {
                            open.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } catch (Exception e) {
                Diary.note("TTY badge image: " + String.valueOf(e));
            }
            return picture;
        }
    }
}
