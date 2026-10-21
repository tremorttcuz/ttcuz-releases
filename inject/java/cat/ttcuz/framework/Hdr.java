package cat.ttcuz.framework;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.view.View;
import android.view.Window;

/**
 * HDR video at the brightness of everything else.
 *
 * The video still plays. What goes away is the extra brightness: a headroom
 * of one, which means "no more than white".
 *
 * Asking the window is not enough, and that is why this looked switched off
 * when it was on. TikTok decodes video in its own native player -- there is
 * not one call to `MediaCodec.configure` in the whole apk -- and hands the
 * frames to a surface of its own. A surface is composited on its own layer,
 * where the window's headroom does not reach, so the layer goes on asking the
 * display for the bright end while everything around it is dimmed to make
 * room.
 *
 * So each surface is told as well, and told again when the feed puts a new one
 * on screen. That needs Android 15 for a surface and Android 14 for the older
 * way of saying the same thing. Below those there is nothing an app can ask.
 */
public final class Hdr {

    private static final float FLAT = 1.0f;

    private Hdr() {}

    public static boolean isEnabled() {
        return android.os.Build.VERSION.SDK_INT >= 34;
    }

    // ------------------------------------------------------------ the window

    /**
     * Flatten HDR on this window, or leave the window alone.
     *
     * Leaving it alone is the point of the second half. Asking for
     * COLOR_MODE_HDR when the setting is off is not "as it was": it puts the
     * window into a mode TikTok never asked for, and every ordinary colour in
     * it gets mapped through that -- which is why screens went pale and white
     * came out tinted.
     */
    public static void apply(Activity activity) {
        if (activity == null || !isEnabled()) return;
        try {
            Window window = activity.getWindow();
            if (window == null) return;
            if (Build.VERSION.SDK_INT >= 35) {
                window.setDesiredHdrHeadroom(FLAT);
            }
            window.setColorMode(ActivityInfo.COLOR_MODE_DEFAULT);
            flatten(window.getDecorView());
            watch(window.getDecorView());
        } catch (Throwable error) {
            Diary.note("hdr: " + error);
        }
    }

    // ------------------------------------------------------- the surfaces

    /** Our own tag, so a screen is only watched once. */
    private static final int WATCHED = 0x4D617248;

    /** How often the tree is walked again while things move about. */
    private static final long QUIET = 400;

    /**
     * Tell every surface on this screen to stay at the plain brightness.
     *
     * Kept shallow and cheap: the walk stops at views that cannot hold one,
     * and it is only ever done while the setting is on.
     */
    static void flatten(View view) {
        if (view != null && isEnabled()) Sliced.walk(view, SURFACE_VISIT);
    }

    private static final Sliced.Visit SURFACE_VISIT = Hdr::surface;

    private static void surface(View view) {
        if (view == null || Build.VERSION.SDK_INT < 34) return;
        try {
            if (view instanceof android.view.SurfaceView) {
                android.view.SurfaceView surface = (android.view.SurfaceView) view;
                if (Build.VERSION.SDK_INT >= 35) {
                    surface.setDesiredHdrHeadroom(FLAT);
                } else {
                    android.view.SurfaceControl control = surface.getSurfaceControl();
                    if (control != null && control.isValid()) {
                        try (
                            android.view.SurfaceControl.Transaction transaction =
                                new android.view.SurfaceControl.Transaction()
                        ) {
                            transaction.setExtendedRangeBrightness(control, FLAT, FLAT).apply();
                        }
                    }
                }
                return;
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Walk the screen again when it changes, because the feed recycles.
     *
     * One listener per window, throttled: a video that scrolls into view
     * brings a surface that has never been told anything.
     */
    private static void watch(final View decor) {
        if (decor == null || Build.VERSION.SDK_INT < 34) return;
        try {
            if (decor.getTag(WATCHED) != null) return;
            decor.setTag(WATCHED, Boolean.TRUE);
            decor.getViewTreeObserver().addOnGlobalLayoutListener(
                new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                    private long walked;
                    private boolean pending;

                    @Override
                    public void onGlobalLayout() {
                        if (!isEnabled() || pending || !decor.hasWindowFocus()) return;
                        long now = android.os.SystemClock.uptimeMillis();
                        if (now - walked < QUIET) return;
                        pending = true;
                        decor.postOnAnimation(() -> {
                            pending = false;
                            if (!decor.isAttachedToWindow() || !decor.hasWindowFocus()) return;
                            walked = android.os.SystemClock.uptimeMillis();
                            flatten(decor);
                        });
                    }
                }
            );
        } catch (Throwable error) {
            Diary.note("hdr: " + error);
        }
    }
}
