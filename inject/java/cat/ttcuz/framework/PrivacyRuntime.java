package cat.ttcuz.framework;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import java.lang.ref.WeakReference;
import java.util.Objects;

/** Connect the imported senders to existing switches and the foreground lifecycle. */
final class PrivacyRuntime {

    private static final Handler main = new Handler(Looper.getMainLooper());
    private static PrivacyAdapters adapters;
    private static String account;
    private static WeakReference<Activity> foreground = new WeakReference<>(null);
    private static final Runnable tick = new Runnable() {
        public void run() {
            if (foreground.get() == null) return;
            update();
            if (Privacy.any()) main.postDelayed(this, 1000);
        }
    };

    private PrivacyRuntime() {}

    private static boolean flag(String key) {
        switch (key) {
            case "ghost":
                return Privacy.any();
            case "ghost_online":
                return Privacy.hideOnline();
            case "ghost_stories":
                return Privacy.hideStories();
            case "ghost_read":
                return Privacy.hideRead();
            case "ghost_typing":
                return Privacy.hideTyping();
            case "ghost_profiles":
                return Privacy.hideProfiles();
            default:
                return false;
        }
    }

    static void start() {
        main.post(() -> {
            if (adapters != null || !Privacy.any()) return;
            try {
                account = Account.liveId();
                adapters = new PrivacyAdapters(PrivacyRuntime::flag, Diary::note);
                adapters.bootstrap();
            } catch (Throwable error) {
                Diary.note("privacy bootstrap: " + error);
            }
        });
    }

    static void resumed(Activity activity) {
        foreground = new WeakReference<>(activity);
        main.removeCallbacks(tick);
        tick.run();
    }

    static void paused(Activity activity) {
        if (foreground.get() != activity) return;
        foreground.clear();
        main.removeCallbacks(tick);
    }

    static void settingsChanged() {
        main.post(() -> {
            main.removeCallbacks(tick);
            if (foreground.get() != null) tick.run();
        });
    }

    private static void update() {
        try {
            String uid = Account.liveId();
            if (!Objects.equals(account, uid)) {
                if (adapters != null) adapters.close();
                adapters = null;
                account = uid;
            }
            if (!Privacy.any()) return;
            if (adapters == null) {
                adapters = new PrivacyAdapters(PrivacyRuntime::flag, Diary::note);
                adapters.bootstrap();
            }
            adapters.apply();
        } catch (Throwable error) {
            Diary.note("privacy adapters: " + error);
        }
    }
}
