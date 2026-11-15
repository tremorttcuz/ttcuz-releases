package cat.ttcuz.framework;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import java.lang.ref.WeakReference;
import java.util.Objects;

final class PrivacyRuntime {
    private static String account;
    private static PrivacyAdapters adapters;
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static WeakReference<Activity> foreground = new WeakReference<>(null);
    private static final Runnable tick = new Runnable() { // from class: cat.ttcuz.framework.PrivacyRuntime.1
        @Override // java.lang.Runnable
        public void run() {
            if (PrivacyRuntime.foreground.get() == null) {
                return;
            }
            PrivacyRuntime.update();
            if (Privacy.any()) {
                PrivacyRuntime.main.postDelayed(this, 1000L);
            }
        }
    };

    private PrivacyRuntime() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean flag(String str) {
        str.hashCode();
        switch (str) {
            case "ghost_online":
                return Privacy.hideOnline();
            case "ghost_typing":
                return Privacy.hideTyping();
            case "ghost_stories":
                return Privacy.hideStories();
            case "ghost":
                return Privacy.any();
            case "ghost_read":
                return Privacy.hideRead();
            case "ghost_profiles":
                return Privacy.hideProfiles();
            default:
                return false;
        }
    }

    static void start() {
        main.post(new Runnable() { // from class: cat.ttcuz.framework.PrivacyRuntime$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                PrivacyRuntime.lambda$start$0();
            }
        });
    }

    static /* synthetic */ void lambda$start$0() {
        if (adapters == null && Privacy.any()) {
            try {
                account = Account.liveId();
                PrivacyAdapters privacyAdapters = new PrivacyAdapters(PrivacyRuntime::flag, Diary::note);
                adapters = privacyAdapters;
                privacyAdapters.bootstrap();
            } catch (Throwable th) {
                Diary.note("privacy bootstrap: " + String.valueOf(th));
            }
        }
    }

    static void resumed(Activity activity) {
        foreground = new WeakReference<>(activity);
        Handler handler = main;
        Runnable runnable = tick;
        handler.removeCallbacks(runnable);
        runnable.run();
    }

    static void paused(Activity activity) {
        if (foreground.get() != activity) {
            return;
        }
        foreground.clear();
        main.removeCallbacks(tick);
    }

    static void settingsChanged() {
        main.post(new Runnable() { // from class: cat.ttcuz.framework.PrivacyRuntime$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                PrivacyRuntime.lambda$settingsChanged$1();
            }
        });
    }

    static /* synthetic */ void lambda$settingsChanged$1() {
        Handler handler = main;
        Runnable runnable = tick;
        handler.removeCallbacks(runnable);
        if (foreground.get() != null) {
            runnable.run();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void update() {
        try {
            String liveId = Account.liveId();
            if (!Objects.equals(account, liveId)) {
                PrivacyAdapters privacyAdapters = adapters;
                if (privacyAdapters != null) {
                    privacyAdapters.close();
                }
                adapters = null;
                account = liveId;
            }
            if (Privacy.any()) {
                if (adapters == null) {
                    PrivacyAdapters privacyAdapters2 = new PrivacyAdapters(PrivacyRuntime::flag, Diary::note);
                    adapters = privacyAdapters2;
                    privacyAdapters2.bootstrap();
                }
                adapters.apply();
            }
        } catch (Throwable th) {
            Diary.note("privacy adapters: " + String.valueOf(th));
        }
    }
}
