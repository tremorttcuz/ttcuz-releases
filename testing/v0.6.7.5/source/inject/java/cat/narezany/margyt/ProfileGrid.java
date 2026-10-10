package cat.narezany.margyt;

import android.graphics.Outline;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import java.lang.ref.WeakReference;

/**
 * Rounds the video cards of a profile grid, on every profile. The grid is found by its look rather than
 * by a class name TikTok may change: a recycling list showing at least two cells about a third of its
 * width wide and taller than wide. Wide rows (the banner above the videos) are left as they are.
 */
final class ProfileGrid {
    private ProfileGrid() {}

    private static final int WATCHED = 0x7e0c0338, ROUNDED = 0x7e0c0339;
    /** Every tab of a profile (videos, reposts, favourites, likes) is a list of its own: all of them are kept, not only the first found. */
    private static final java.util.List<WeakReference<ViewGroup>> grids = new java.util.ArrayList<>();
    private static long searched;
    private static final ViewOutlineProvider CORNERS = new ViewOutlineProvider() {
        @Override public void getOutline(View view, Outline outline) {
            float density = view.getResources().getDisplayMetrics().density;
            int gap = Math.round(1.5f * density);
            outline.setRoundRect(gap, gap, Math.max(gap + 1, view.getWidth() - gap),
                    Math.max(gap + 1, view.getHeight() - gap), 14f * density);
        }
    };

    private static boolean onScreen(View view) {
        if (view == null || !view.isAttachedToWindow() || !view.isShown()) return false;
        android.graphics.Rect r = new android.graphics.Rect();
        return view.getGlobalVisibleRect(r) && r.right > r.left && r.bottom > r.top;
    }

    private static boolean known(ViewGroup list) {
        for (int i = grids.size() - 1; i >= 0; i--) {
            ViewGroup held = grids.get(i).get();
            if (held == null || !held.isAttachedToWindow()) { grids.remove(i); continue; }
            if (held == list) return true;
        }
        return false;
    }

    private static boolean anyShowing() {
        for (WeakReference<ViewGroup> ref : grids) if (onScreen(ref.get())) return true;
        return false;
    }

    /** Called while a profile header is bound: looks for lists that are showing and keeps rounding every one of them. */
    static void tick(View anchor) {
        try {
            long time = SystemClock.uptimeMillis();
            if (anchor == null || time - searched < 300) return;
            searched = time;
            View root = anchor.getRootView();
            // Scrolling the header away must not stop the work: a tab that is already known keeps the profile "on screen".
            if (!ProfileLayout.visible(root) && !anyShowing()) return;
            java.util.List<ViewGroup> found = new java.util.ArrayList<>();
            find(root, found, 0);
            for (ViewGroup list : found) {
                if (!known(list)) grids.add(new WeakReference<>(list));
                watch(list);
                round(list);
            }
        } catch (Throwable error) {
            Diary.note("profile grid: " + error);
        }
    }

    private static void watch(final ViewGroup list) {
        if (Boolean.TRUE.equals(list.getTag(WATCHED))) return;
        list.setTag(WATCHED, true);
        list.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                if (list.isShown()) round(list);
                return true;
            }
        });
    }

    private static void round(ViewGroup list) {
        int width = list.getWidth();
        if (width <= 0) return;
        for (int i = 0; i < list.getChildCount(); i++) {
            View cell = list.getChildAt(i);
            if (!isCell(cell, width) || Boolean.TRUE.equals(cell.getTag(ROUNDED))) continue;
            cell.setTag(ROUNDED, true);
            cell.setOutlineProvider(CORNERS);
            cell.setClipToOutline(true);
        }
    }

    private static boolean isCell(View cell, int listWidth) {
        int w = cell.getWidth();
        return w > listWidth * .28f && w < listWidth * .4f && cell.getHeight() > w * 1.1f;
    }

    private static boolean recycler(View view) {
        for (Class<?> type = view.getClass(); type != null && type != View.class; type = type.getSuperclass())
            if (type.getName().endsWith("RecyclerView")) return true;
        return false;
    }

    /** Collects every list on screen that shows at least two video-card cells; the tabs of one profile are separate lists. */
    private static void find(View view, java.util.List<ViewGroup> out, int depth) {
        if (!(view instanceof ViewGroup) || !view.isShown() || depth > 40) return;
        ViewGroup group = (ViewGroup) view;
        if (group.getWidth() > 0) {
            int cells = 0;
            for (int i = 0; i < group.getChildCount(); i++) if (isCell(group.getChildAt(i), group.getWidth())) cells++;
            if (cells >= 2 && (recycler(group) || onScreen(group))) { out.add(group); return; }
        }
        for (int i = 0; i < group.getChildCount(); i++) find(group.getChildAt(i), out, depth + 1);
    }
}
