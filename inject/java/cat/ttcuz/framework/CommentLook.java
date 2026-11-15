package cat.ttcuz.framework;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * How the comments sheet sits over the video: whether the video makes room for it (TikTok shrinks it upward; with
 * "keep video" it stays full size and the sheet lies over it), and how see-through the sheet is.
 */
public final class CommentLook {
    private CommentLook() {}

    static final String KEEP = "comments_keep_video", CLEAR = "comments_transparency";
    private static volatile Boolean keep;
    private static volatile int clear = -1;

    private static android.content.SharedPreferences prefs() {
        Context context = Ttcuz.context();
        return context == null ? null : context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
    }

    /** Read by TikTok's shrink component (a ported hook): true and the video is not shrunk for the sheet. */
    public static boolean keepVideo() {
        Boolean known = keep;
        if (known != null) return known;
        android.content.SharedPreferences p = prefs();
        boolean value = p != null && p.getBoolean(KEEP, false);
        keep = value;
        return value;
    }

    static void setKeepVideo(boolean on) {
        keep = on;
        android.content.SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean(KEEP, on).apply();
    }

    /** How see-through the sheet is, 0 (as TikTok draws it) to 90 percent. */
    static int transparency() {
        if (clear >= 0) return clear;
        android.content.SharedPreferences p = prefs();
        clear = p == null ? 0 : Math.max(0, Math.min(90, p.getInt(CLEAR, 0)));
        return clear;
    }

    static void setTransparency(int value) {
        clear = Math.max(0, Math.min(90, value));
        android.content.SharedPreferences p = prefs();
        if (p != null) p.edit().putInt(CLEAR, clear).apply();
    }

    /** Each drawable's own alpha before we touched it, to give it back when transparency goes to 0. */
    private static final Map<Drawable, Integer> was = new WeakHashMap<>();

    /**
     * The sheet was built or came back. Only the sheet itself is touched, never the screen around it: the view TikTok
     * hands over covers the whole screen (the sheet is moved down inside it), so the sheet is found as the outermost
     * view in it that starts below the top tenth of the screen. Its own background takes the chosen transparency;
     * every other wide background inside it (the header, the bar where you write) goes clear, so the whole sheet is
     * one layer of one tone. A bar TikTok keeps beside the sheet, under it, takes the sheet's tone.
     */
    /** The see-through amount used: the chosen one. */
    static int clearness() {
        return transparency();
    }

    private static final int WATCHED = 0x54434C4B; // "TCLK"
    /** The sheet styled last and the look it got: a layout pass that changes neither costs one comparison. */
    private static java.lang.ref.WeakReference<View> styled = new java.lang.ref.WeakReference<>(null);
    private static int styledLook = -1;

    /**
     * The comments' view was made. TikTok makes it once and only hides and shows it afterwards (and slides it in from
     * below), so it is not styled here and now -- at this moment the sheet is usually not shown yet -- but watched:
     * each layout pass in which the sheet is shown styles it, once per sheet and look.
     */
    static void apply(final View root) {
        if (root == null) return;
        if (root.getTag(WATCHED) == null) {
            root.setTag(WATCHED, Boolean.TRUE);
            root.getViewTreeObserver().addOnGlobalLayoutListener(() -> settle(root));
        }
        styled = new java.lang.ref.WeakReference<>(null);
        root.post(() -> settle(root));
    }

    private static void settle(View root) {android.os.Trace.beginSection("ttcuz comment look");try{
        try {
            if (!root.isAttachedToWindow()) return;
            int clear = clearness(), look = clear;
            View known = styled.get();
            if (known != null && styledLook == look && known.isShown()) return;
            if (clear == 0) { if (!was.isEmpty()) restore(); styledLook = look; return; }
            View sheet = sheetOf(root);
            if (sheet == null) return;
            int alpha = Math.round(255 * (100 - clear) / 100f), width = root.getWidth();
            layer(sheet, width, alpha, false, 0);
            int sheetTop = topIn(sheet, root);
            if (sheet.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) sheet.getParent();
                for (int i = 0; i < parent.getChildCount(); i++) {
                    View bar = parent.getChildAt(i);
                    if (bar == sheet || !bar.isShown() || bar.getWidth() < width * .9f || bar.getHeight() > root.getHeight() * .25f) continue;
                    int barTop = topIn(bar, root);
                    if (barTop < sheetTop) continue;
                    // over the sheet's own colour it goes clear; beside it, it takes the same colour
                    boolean over = barTop + bar.getHeight() <= sheetTop + sheet.getHeight();
                    layer(bar, width, alpha, over, 0);
                }
            }
            styled = new java.lang.ref.WeakReference<>(sheet);
            styledLook = look;
            Diary.note("comment look: " + clear + "% see-through");
        } catch (Throwable error) { Diary.note("comment look: " + error); }
    }finally{android.os.Trace.endSection();}}

    /** A view's top inside root, by layout alone: a sheet still sliding in (translated) is found where it will rest. */
    private static int topIn(View view, View root) {
        int top = 0;
        for (View at = view; at != null && at != root; at = at.getParent() instanceof View ? (View) at.getParent() : null) top += at.getTop();
        return top;
    }

    /** The comments sheet inside TikTok's full-screen view: the outermost shown wide view below its top tenth. */
    private static View sheetOf(View root) {
        if (!root.isShown()) return null;
        int height = root.getHeight(), limit = height / 10, width = root.getWidth();
        if (root.getTop() >= limit) return root;
        java.util.ArrayDeque<View> queue = new java.util.ArrayDeque<>();
        queue.add(root);
        for (int seen = 0; !queue.isEmpty() && seen < 400; seen++) {
            View view = queue.poll();
            if (view != root && view.getVisibility() != View.VISIBLE) continue;
            if (view != root && view.getWidth() >= width * .9f && view.getHeight() > 0) {
                int top = topIn(view, root);
                if (top >= limit && top < height) return view;
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) queue.add(group.getChildAt(i));
            }
        }
        return null;
    }

    /**
     * A wide colour with nothing coloured under it carries the sheet's tone; one over a coloured ancestor goes clear.
     * So every part of the sheet is coloured exactly once, wherever TikTok put its colours.
     */
    private static void layer(View view, int width, int alpha, boolean covered, int depth) {
        if (view == null || depth > 12) return;
        if (paint(view, width, covered ? 0 : alpha)) covered = true;
        if (!(view instanceof ViewGroup)) return;
        // the rows of the list keep their own look
        for (Class<?> type = view.getClass(); type != null; type = type.getSuperclass())
            if (type.getName().equals("androidx.recyclerview.widget.RecyclerView")) return;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) layer(group.getChildAt(i), width, alpha, covered, depth + 1);
    }

    /** The comments closed (or transparency went to 0): every background as TikTok drew it. */
    static void restore() {
        for (Map.Entry<Drawable, Integer> e : new java.util.ArrayList<>(was.entrySet())) if (e.getKey() != null) e.getKey().setAlpha(e.getValue());
        was.clear();
        styled = new java.lang.ref.WeakReference<>(null);
    }

    /** Sets a wide view's background to `alpha` of its own; true when it had one to set. */
    private static boolean paint(View view, int width, int alpha) {
        Drawable background = view.getBackground();
        if (background == null || width <= 0 || view.getWidth() < width * .9f) return false;
        // a press ripple, a picture or an animation is not the sheet's colour; any other background is (a colour, a
        // shape, an inset, a layer, a state list, TikTok's own kinds: the bar where you write uses those)
        if (background instanceof android.graphics.drawable.RippleDrawable
                || background instanceof android.graphics.drawable.BitmapDrawable || background instanceof android.graphics.drawable.Animatable) return false;
        if (background instanceof android.graphics.drawable.ColorDrawable
                && (((android.graphics.drawable.ColorDrawable) background).getColor() >>> 24) == 0) return false;
        Drawable own = background.mutate();
        if (own != background) view.setBackground(own);
        if (!was.containsKey(own)) was.put(own, own.getAlpha());
        int base = was.get(own);
        int target = Math.round(base * alpha / 255f);
        if (own.getAlpha() != target) own.setAlpha(target);
        return true;
    }
}
