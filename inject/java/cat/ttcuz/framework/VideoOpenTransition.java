package cat.ttcuz.framework;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** Native window expansion from a profile card; observes input without replacing TikTok clicks. */
public final class VideoOpenTransition {

    private static final WeakHashMap<Activity, Gesture> gestures = new WeakHashMap<>();
    private static final long READY_MS = 700;

    private VideoOpenTransition() {}

    private static Activity activity(Context context) {
        for (int depth = 0; context != null && depth < 12; depth++) {
            if (context instanceof Activity) return (Activity) context;
            if (!(context instanceof ContextWrapper)) break;
            Context next = ((ContextWrapper) context).getBaseContext();
            if (next == context) break;
            context = next;
        }
        return null;
    }

    static void watch(View grid) {
        Activity owner = activity(grid.getContext());
        if (owner != null) IslandTouch.hook(owner);
    }

    static void clear(Activity owner) {
        gestures.remove(owner);
    }

    static void touch(Activity owner, MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            clear(owner);
            View cell = ProfileGrid.cellAt(
                owner.getWindow().getDecorView(),
                event.getRawX(),
                event.getRawY()
            );
            if (cell != null && event.getPointerCount() == 1) gestures.put(
                owner,
                new Gesture(cell, event, ViewConfiguration.get(owner).getScaledTouchSlop())
            );
            return;
        }
        Gesture gesture = gestures.get(owner);
        if (gesture == null) return;
        if (
            action == MotionEvent.ACTION_CANCEL ||
            action == MotionEvent.ACTION_POINTER_DOWN ||
            event.getPointerCount() != 1 ||
            Math.hypot(event.getRawX() - gesture.x, event.getRawY() - gesture.y) > gesture.slop
        ) {
            clear(owner);
        } else if (action == MotionEvent.ACTION_UP) {
            View cell = gesture.cell.get();
            if (
                event.getEventTime() - gesture.downTime >=
                    ViewConfiguration.getLongPressTimeout() ||
                cell == null ||
                cell !=
                    ProfileGrid.cellAt(
                        owner.getWindow().getDecorView(),
                        event.getRawX(),
                        event.getRawY()
                    )
            ) {
                clear(owner);
            } else {
                // Arm before native ACTION_UP dispatch, where TikTok synchronously opens its route.
                gesture.ready = SystemClock.uptimeMillis();
            }
        }
    }

    /** Called by the verified ActivityRoute immediately before it reads its launch options. */
    public static void prepare(Context context, Intent intent, Object route) {
        try {
            Activity owner = activity(context);
            Gesture gesture = gestures.remove(owner);
            if (
                gesture == null ||
                gesture.ready < 0 ||
                SystemClock.uptimeMillis() - gesture.ready > READY_MS ||
                intent == null ||
                route == null ||
                !Motion.enabled(owner)
            ) return;
            ComponentName component = intent.getComponent();
            if (
                component == null || !owner.getPackageName().equals(component.getPackageName())
            ) return;
            String name = component.getClassName();
            if (
                !name.equals("com.ss.android.ugc.aweme.detail.ui.DetailActivity") &&
                !name.equals("com.ss.android.ugc.aweme.ui.activity.PostModeDetailActivity")
            ) return;
            // The router's options path bypasses result callbacks. Keep those and existing transitions intact.
            if (
                !Boolean.FALSE.equals(NativeRead.get(route, "hasRequestCode")) ||
                NativeRead.get(route, "getAnimationBundle") != null
            ) return;
            View cell = gesture.cell.get();
            if (
                !ProfileLayout.onScreen(cell, owner.getWindow().getDecorView().getRootView())
            ) return;
            Rect visible = new Rect();
            if (!cell.getLocalVisibleRect(visible)) return;
            visible.intersect(0, 0, cell.getWidth(), cell.getHeight());
            if (visible.isEmpty()) return;
            Bundle options = ActivityOptions.makeScaleUpAnimation(
                cell,
                visible.left,
                visible.top,
                visible.width(),
                visible.height()
            ).toBundle();
            route.getClass().getMethod("setAnimationBundle", Bundle.class).invoke(route, options);
        } catch (Throwable error) {
            // A missing route API or unsupported window must never block the original launch.
            Diary.note("video open: " + error);
        }
    }

    private static final class Gesture {

        final WeakReference<View> cell;
        final float x, y;
        final long downTime;
        final int slop;
        long ready = -1;

        Gesture(View source, MotionEvent event, int slop) {
            cell = new WeakReference<>(source);
            x = event.getRawX();
            y = event.getRawY();
            downTime = event.getDownTime();
            this.slop = slop;
        }
    }
}
