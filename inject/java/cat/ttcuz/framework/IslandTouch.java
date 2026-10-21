package cat.ttcuz.framework;

import android.app.Activity;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Hold the island's selection indicator, drag it across the tabs, release to choose one.
 *
 * Everything outside that indicator passes through untouched: the gesture only starts when a touch
 * lands on the indicator and stays still for the hold time. Until then TikTok sees every event as
 * usual (taps, the seekbar above the bar, scrolling). When the hold fires, TikTok's own tab gets a
 * CANCEL (so nothing is clicked) and the window swallows the rest of the gesture; on release a
 * normal tap is replayed on the chosen tab, so TikTok does its own routing.
 */
final class IslandTouch {

    private static final Map<Window, Boolean> hooked = new WeakHashMap<Window, Boolean>();
    private static final long HOLD_MS = 300L;

    private IslandTouch() {}

    static void hook(final Activity activity) {
        final Window window = activity.getWindow();
        if (window == null) return;
        final Window.Callback base = window.getCallback();
        if (base == null) return;
        synchronized (hooked) {
            if (hooked.containsKey(window)) return;
            hooked.put(window, Boolean.TRUE);
        }
        final Tracker tracker = new Tracker(activity, base);
        window.setCallback(
            (Window.Callback) Proxy.newProxyInstance(
                base.getClass().getClassLoader(),
                new Class<?>[] { Window.Callback.class },
                (proxy, method, args) -> {
                    if (
                        args != null &&
                        args.length == 1 &&
                        args[0] instanceof MotionEvent &&
                        method.getName().equals("dispatchTouchEvent")
                    ) {
                        try {
                            VideoOpenTransition.touch(activity, (MotionEvent) args[0]);
                        } catch (Throwable error) {
                            VideoOpenTransition.clear(activity);
                            Diary.note("video touch: " + error);
                        }
                        try {
                            if (tracker.touch((MotionEvent) args[0])) return Boolean.TRUE;
                        } catch (Throwable error) {
                            tracker.reset();
                            Diary.note("island drag: " + error);
                        }
                    }
                    try {
                        return method.invoke(base, args);
                    } catch (InvocationTargetException x) {
                        throw x.getCause();
                    }
                }
            )
        );
    }

    private static final class Tracker {

        final Activity activity;
        final Window.Callback base;
        final int slop;
        final Runnable fire = new Runnable() {
            public void run() {
                lift();
            }
        };
        boolean armed, lifted;
        float downX, downY;
        long downTime;

        Tracker(Activity activity, Window.Callback base) {
            this.activity = activity;
            this.base = base;
            slop = ViewConfiguration.get(activity).getScaledTouchSlop();
        }

        View decor() {
            return activity.getWindow().getDecorView();
        }

        boolean touch(MotionEvent e) {
            int action = e.getActionMasked();
            if (lifted) {
                if (action == MotionEvent.ACTION_DOWN) reset();
                else {
                    if (action == MotionEvent.ACTION_MOVE) {
                        if (
                            NavigationIsland.move(activity, e.getRawX())
                        ) decor().performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    } else if (
                        action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
                    ) release(action == MotionEvent.ACTION_UP);
                    return true;
                }
            }
            if (action == MotionEvent.ACTION_DOWN) {
                reset();
                if (NavigationIsland.hit(activity, e.getRawX(), e.getRawY())) {
                    armed = true;
                    downX = e.getRawX();
                    downY = e.getRawY();
                    downTime = e.getDownTime();
                    decor().postDelayed(fire, HOLD_MS);
                }
            } else if (
                armed &&
                (action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_CANCEL ||
                    action == MotionEvent.ACTION_POINTER_DOWN ||
                    (action == MotionEvent.ACTION_MOVE &&
                        Math.hypot(e.getRawX() - downX, e.getRawY() - downY) > slop))
            ) reset();
            return false;
        }

        void lift() {
            if (!armed) return;
            armed = false;
            MotionEvent cancel = MotionEvent.obtain(
                downTime,
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_CANCEL,
                downX,
                downY,
                0
            );
            try {
                base.dispatchTouchEvent(cancel);
            } catch (Throwable error) {
                Diary.note("island drag: " + error);
            } finally {
                cancel.recycle();
            }
            lifted = true;
            NavigationIsland.begin(activity, downX);
            decor().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }

        void release(boolean commit) {
            lifted = false;
            float[] at = NavigationIsland.end(activity, commit);
            if (at == null) return;
            long now = SystemClock.uptimeMillis();
            MotionEvent down = MotionEvent.obtain(
                now,
                now,
                MotionEvent.ACTION_DOWN,
                at[0],
                at[1],
                0
            );
            MotionEvent up = MotionEvent.obtain(
                now,
                now + 30,
                MotionEvent.ACTION_UP,
                at[0],
                at[1],
                0
            );
            try {
                base.dispatchTouchEvent(down);
                base.dispatchTouchEvent(up);
            } catch (Throwable error) {
                Diary.note("island drag: " + error);
            } finally {
                down.recycle();
                up.recycle();
            }
        }

        void reset() {
            decor().removeCallbacks(fire);
            armed = false;
            if (lifted) {
                lifted = false;
                NavigationIsland.end(activity, false);
            }
        }
    }
}
