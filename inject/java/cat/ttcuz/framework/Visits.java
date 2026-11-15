package cat.ttcuz.framework;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

/**
 * When TikTok comes on screen and goes away, for the features that care about the visit rather than one screen:
 * the fingerprint lock, beat vibration and picking the video up where it was left.
 */
final class Visits implements Application.ActivityLifecycleCallbacks {
    private static boolean started;
    private static int visible;
    private static boolean first = true;

    static synchronized void start(Application app) {
        if (started) return;
        app.registerActivityLifecycleCallbacks(new Visits());
        started = true;
        AppLock.start(app);
    }

    private static final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    private static final java.util.List<Runnable> afterFrame = new java.util.ArrayList<>();
    private static boolean framed;

    /**
     * Work that TikTok's first frame need not wait for: run once that frame is drawn, or after 3 s when the process
     * was started with no screen at all (an alarm, a notification), so it always runs.
     */
    static synchronized void afterFirstFrame(Runnable work) {
        if (framed) { main.post(work); return; }
        if (afterFrame.isEmpty()) main.postDelayed(Visits::framed, 3000L);
        afterFrame.add(work);
    }

    private static synchronized void framed() {
        if (framed) return;
        framed = true;
        main.removeCallbacksAndMessages(null);
        for (Runnable work : afterFrame) main.post(() -> { try { work.run(); } catch (Throwable error) { Diary.note("late start: " + error); } });
        afterFrame.clear();
    }

    /** True while some TikTok screen is on screen. */
    static boolean visible() { return visible > 0; }

    @Override public void onActivityCreated(Activity activity, Bundle state) {
        if (!first) return;
        first = false;
        // the screen the process was started for: a plain launch (icon, recents) may pick up the last video, a link or
        // a notification opens what it points to
        Intent intent = activity.getIntent();
        String action = intent == null ? null : intent.getAction();
        boolean plain = intent == null || (intent.getData() == null && (action == null || Intent.ACTION_MAIN.equals(action)));
        VideoResume.coldStart(plain);
    }

    @Override public void onActivityStarted(Activity activity) {
        if (visible++ > 0) return;
        AppLock.came();
        VideoResume.came();
        BeatHaptics.update();
    }

    @Override public void onActivityResumed(Activity activity) {
        if (!framed && activity.getWindow() != null) {
            final android.view.View decor = activity.getWindow().getDecorView();
            final android.view.ViewTreeObserver.OnDrawListener[] once = new android.view.ViewTreeObserver.OnDrawListener[1];
            once[0] = () -> {
                // a listener cannot be taken off from inside onDraw: off on the next turn, with the late work
                decor.post(() -> { if (decor.getViewTreeObserver().isAlive()) decor.getViewTreeObserver().removeOnDrawListener(once[0]); framed(); });
            };
            decor.getViewTreeObserver().addOnDrawListener(once[0]);
        }
        AppLock.resumed(activity);
        BeatHaptics.screen(activity);
    }

    @Override public void onActivityPaused(Activity activity) {
        VideoResume.remember();
        AppLock.paused(activity);
    }

    @Override public void onActivityStopped(Activity activity) {
        if (visible > 0 && --visible > 0) return;
        AppLock.left();
        BeatHaptics.update();
    }

    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}

    @Override public void onActivityDestroyed(Activity activity) { AppLock.destroyed(activity); }
}
