package cat.ttcuz.framework;

import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The feed under the island. TikTok lays the feed out above the bottom bar and paints the bar's strip black, so the
 * glass of the island had nothing but black behind it. This lets the feed run on under the bar, the way TikTok's own
 * immersive layout does, so the frosted or liquid glass shows the video through it.
 *
 * It finds the container stacked directly above the bar's own container (same parent, its bottom on the bar's top),
 * drops the bottom margin or the "bottom to top of the bar" constraint that keeps it there, keeps the bar drawn above
 * it, and clears the opaque backgrounds between the island and that parent. Each change remembers what it replaced;
 * switching the setting off, or the island, puts all of it back. Nothing is touched while the island is not attached.
 */
final class FeedUnderBar {
    private FeedUnderBar() {}

    public static final String KEY = "feed_under_bar";
    private static volatile Boolean on;

    static boolean isEnabled() {
        Boolean known = on; if (known != null) return known;
        android.content.Context context = Ttcuz.context(); if (context == null) return true;
        boolean value = true;
        try { value = context.getSharedPreferences(Ttcuz.PREFS, android.content.Context.MODE_PRIVATE).getBoolean(KEY, true); } catch (Throwable ignored) {}
        on = value; return value;
    }

    static void setEnabled(boolean value) {
        on = value;
        android.content.Context context = Ttcuz.context();
        if (context != null) context.getSharedPreferences(Ttcuz.PREFS, android.content.Context.MODE_PRIVATE).edit().putBoolean(KEY, value).apply();
        if (!value) restore();
    }

    static void reloadSettings() { on = null; if (!isEnabled()) restore(); }

    /** What a view had before: a margin, a constraint, a background, a z. */
    private static final class Before {
        int margin = Integer.MIN_VALUE, toTop = Integer.MIN_VALUE, toBottom = Integer.MIN_VALUE, height = Integer.MIN_VALUE, visibility = Integer.MIN_VALUE;
        android.graphics.drawable.Drawable background; boolean cleared;
        float z = Float.NaN; android.view.ViewOutlineProvider outline;
    }
    private static final Map<View, Before> changed = new WeakHashMap<>();
    private static final int[] at = new int[2], other = new int[2];

    /** Called by the island once per structural change of the bar (not per frame). */
    static void apply(ViewGroup rail) {
        // Switched off for good: TikTok's own strip under the bar stays as TikTok lays it out. Moving the feed under
        // it shifted the captions and made the player relay out. Anything an older build changed is put back.
        if (!changed.isEmpty()) restore();
    }

    // TikTok 47.2.41: the feed's column is the video pager and, under it, a spacer as tall as the old bottom bar
    // (app:id/cnk). That spacer is the black strip under the island; the feed box itself (app:id/ona) stops above
    // the gesture area. Read from the live view tree; checked by shape before anything is changed.
    private static final int FEED_SPACER = 0x7f0a0e8a, FEED_BOX = 0x7f0a4eaa;

    private static void openFeed(View rail) {
        View root = rail.getRootView();
        float dp = root.getResources().getDisplayMetrics().density;
        View spacer = root.findViewById(FEED_SPACER);
        if (spacer != null && spacer.getParent() instanceof android.widget.LinearLayout
                && ((android.widget.LinearLayout) spacer.getParent()).getOrientation() == android.widget.LinearLayout.VERTICAL
                && spacer.getWidth() >= root.getWidth() * .9f && spacer.getHeight() <= 120 * dp) {
            keepClosed(spacer);
        }
        View box = root.findViewById(FEED_BOX);
        if (box != null && box.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) box.getLayoutParams();
            if (params.bottomMargin > 0 && params.bottomMargin <= 64 * dp) {
                Before b = changed.get(box); if (b == null) { b = new Before(); changed.put(box, b); }
                if (b.margin == Integer.MIN_VALUE) b.margin = params.bottomMargin;
                params.bottomMargin = 0;
                box.setLayoutParams(params);
                Diary.note("feed under bar: feed runs to the bottom edge");
            }
        }
    }

    private static final int GUARD = 0x7f300f01;

    /**
     * TikTok sets the spacer's height again whenever the bar changes, so it is hidden instead and kept hidden. The
     * check runs only when the feed's column is laid out again (that is when TikTok can have shown it), never per
     * frame: a steady feed costs nothing. The pager above it, the feed, takes the room.
     */
    private static void keepClosed(final View spacer) {
        Before b = changed.get(spacer); if (b == null) { b = new Before(); changed.put(spacer, b); }
        if (b.visibility == Integer.MIN_VALUE) b.visibility = spacer.getVisibility();
        if (spacer.getVisibility() != View.GONE) { spacer.setVisibility(View.GONE); Diary.note("feed under bar: spacer under the feed hidden"); }
        if (spacer.getTag(GUARD) != null || !(spacer.getParent() instanceof ViewGroup)) return;
        final ViewGroup column = (ViewGroup) spacer.getParent();
        View.OnLayoutChangeListener guard = (v, l, t, r, bottom, ol, ot, or, ob) -> {
            if (!isEnabled() || spacer.getParent() != column) return;
            // posted: changing a child from inside its parent's layout pass would be ignored until the next one
            if (spacer.getVisibility() != View.GONE) { column.post(() -> { if (isEnabled()) spacer.setVisibility(View.GONE); }); return; }
            View pager = previous(column, spacer);
            ViewGroup.LayoutParams params = pager == null ? null : pager.getLayoutParams();
            // a pager given a fixed height keeps the strip even with the spacer gone: it fills the column instead
            if (params != null && params.height > 0 && pager.getHeight() < column.getHeight() - 2) {
                column.post(() -> {
                    Before p = changed.get(pager); if (p == null) { p = new Before(); changed.put(pager, p); }
                    if (p.height == Integer.MIN_VALUE) p.height = params.height;
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    pager.setLayoutParams(params);
                });
            }
        };
        spacer.setTag(GUARD, guard);
        column.addOnLayoutChangeListener(guard);
    }

    private static View previous(ViewGroup column, View child) {
        int at = column.indexOfChild(child);
        for (int i = at - 1; i >= 0; i--) if (column.getChildAt(i).getVisibility() == View.VISIBLE) return column.getChildAt(i);
        return null;
    }

    /** The full-width sibling whose bottom sits on the bar's top: the feed's container. */
    private static View above(ViewGroup parent, View bar) {
        bar.getLocationOnScreen(at);
        int top = at[1];
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child == bar || child.getVisibility() != View.VISIBLE) continue;
            if (child.getWidth() < parent.getWidth() * .9f || child.getHeight() < parent.getHeight() * .5f) continue;
            child.getLocationOnScreen(other);
            if (Math.abs(other[1] + child.getHeight() - top) <= 3) return child;
        }
        return null;
    }

    private static void extend(View feed, View bar, ViewGroup parent) {
        ViewGroup.LayoutParams params = feed.getLayoutParams();
        Before before = changed.get(feed);
        boolean moved = false;
        if (params instanceof ViewGroup.MarginLayoutParams && ((ViewGroup.MarginLayoutParams) params).bottomMargin > 0) {
            if (before == null) { before = new Before(); changed.put(feed, before); }
            if (before.margin == Integer.MIN_VALUE) before.margin = ((ViewGroup.MarginLayoutParams) params).bottomMargin;
            ((ViewGroup.MarginLayoutParams) params).bottomMargin = 0; moved = true;
        }
        // ConstraintLayout: bottom_toTopOf="@id/bar" becomes bottom_toBottomOf="parent".
        Field toTop = field(params, "bottomToTop"), toBottom = field(params, "bottomToBottom");
        if (toTop != null && toBottom != null) {
            try {
                if (toTop.getInt(params) == bar.getId() && bar.getId() != View.NO_ID) {
                    if (before == null) { before = new Before(); changed.put(feed, before); }
                    if (before.toTop == Integer.MIN_VALUE) { before.toTop = toTop.getInt(params); before.toBottom = toBottom.getInt(params); }
                    toTop.setInt(params, -1); toBottom.setInt(params, 0); moved = true;
                }
            } catch (IllegalAccessException ignored) {}
        }
        if (!moved) return;
        feed.setLayoutParams(params);
        // The bar must stay drawn above the feed that now runs under it.
        if (parent.indexOfChild(bar) < parent.indexOfChild(feed)) {
            Before b = changed.get(bar); if (b == null) { b = new Before(); changed.put(bar, b); }
            if (Float.isNaN(b.z)) { b.z = bar.getTranslationZ(); b.outline = bar.getOutlineProvider(); }
            bar.setOutlineProvider(null); bar.setTranslationZ(1f);
        }
        Diary.note("feed under bar: on");
    }

    /** Between the island and the container that holds the bar, nothing opaque may cover the feed. */
    private static void clearBackgrounds(View rail, View top) {
        Object up = rail.getParent();
        while (up instanceof View) {
            View v = (View) up;
            if (v.getBackground() != null) {
                Before b = changed.get(v); if (b == null) { b = new Before(); changed.put(v, b); }
                if (!b.cleared) { b.background = v.getBackground(); b.cleared = true; }
                v.setBackground(null);
            }
            if (v == top) break;
            up = v.getParent();
        }
    }

    private static Field field(Object params, String name) {
        try { Field f = params.getClass().getField(name); return f.getType() == int.class ? f : null; } catch (Throwable ignored) { return null; }
    }

    static void restore() {
        for (Map.Entry<View, Before> e : changed.entrySet()) {
            View v = e.getKey(); Before b = e.getValue(); if (v == null) continue;
            ViewGroup.LayoutParams params = v.getLayoutParams();
            if (b.margin != Integer.MIN_VALUE && params instanceof ViewGroup.MarginLayoutParams) ((ViewGroup.MarginLayoutParams) params).bottomMargin = b.margin;
            if (b.toTop != Integer.MIN_VALUE) {
                Field toTop = field(params, "bottomToTop"), toBottom = field(params, "bottomToBottom");
                try { if (toTop != null) toTop.setInt(params, b.toTop); if (toBottom != null) toBottom.setInt(params, b.toBottom); } catch (IllegalAccessException ignored) {}
            }
            if (b.height != Integer.MIN_VALUE && params != null) params.height = b.height;
            if (b.margin != Integer.MIN_VALUE || b.toTop != Integer.MIN_VALUE || b.height != Integer.MIN_VALUE) v.setLayoutParams(params);
            if (b.cleared) v.setBackground(b.background);
            if (b.visibility != Integer.MIN_VALUE) v.setVisibility(b.visibility);
            if (!Float.isNaN(b.z)) { v.setTranslationZ(b.z); v.setOutlineProvider(b.outline); }
        }
        changed.clear();
    }
}
