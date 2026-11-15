package cat.ttcuz.framework;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Where the download island lives: a layer over the screen, in the activity's own window.
 *
 * It used to be a window of its own, resized to open and to close. A window's new size reaches the screen a frame or
 * two after it is asked for, so the island's shape and its window were never in step and the island jerked on every
 * opening. Here nothing is ever resized: the island's view is as big as the open island from the start and draws only
 * its shape inside, which changes on the render thread from frame to frame. Touches outside the shape are not taken
 * (DownloadMark answers only inside it), so the layer is invisible to everything under it.
 */
final class IslandLayer {
    final Activity owner;
    final FrameLayout layer;
    final DownloadMark mark;

    IslandLayer(Activity owner, DownloadMark mark) {
        this.owner = owner;
        this.mark = mark;
        layer = new FrameLayout(owner);
        layer.setClipChildren(false);
        layer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams at = new FrameLayout.LayoutParams(DownloadIsland.openWidth(owner), DownloadIsland.openHeight(owner),
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        at.topMargin = statusBar(owner) + dp(56);
        layer.addView(mark, at);
        ((ViewGroup) owner.getWindow().getDecorView()).addView(layer,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    boolean isShowing() {
        return layer.getParent() != null && layer.isAttachedToWindow() && !owner.isFinishing();
    }

    void dismiss() {
        try {
            if (layer.getParent() instanceof ViewGroup) ((ViewGroup) layer.getParent()).removeView(layer);
        } catch (Throwable ignored) {
        }
    }

    private int dp(int value) {
        return Math.round(value * owner.getResources().getDisplayMetrics().density);
    }

    private static int statusBar(Activity activity) {
        int id = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id == 0 ? Math.round(24 * activity.getResources().getDisplayMetrics().density) : activity.getResources().getDimensionPixelSize(id);
    }
}
