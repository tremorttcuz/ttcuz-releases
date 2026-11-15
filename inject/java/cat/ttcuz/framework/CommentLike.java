package cat.ttcuz.framework;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import java.util.Locale;

/**
 * A double tap on a comment likes it, as a double tap on a video does -- and a double tap on a liked comment takes
 * the like back.
 *
 * A single tap on a comment opens the reply, so with this on a tap waits a moment for a second one: if none comes the
 * tap goes through as it was; if one does, the comment's own heart is pressed instead.
 */
public final class CommentLike {
    private CommentLike() {}

    static final String KEY = "comment_double_tap_like";
    private static final int TAG = 0x5454434C;
    private static final Handler main = new Handler(Looper.getMainLooper());

    static boolean enabled() {
        Context context = Ttcuz.context();
        return context != null && context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false);
    }

    static void set(boolean on) {
        Context context = Ttcuz.context();
        if (context != null) context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply();
    }

    private static volatile java.lang.reflect.Method tap, like, managing;
    private static Runnable pending;
    private static java.lang.ref.WeakReference<Object> pendingCell = new java.lang.ref.WeakReference<>(null);
    private static long pendingAt;

    /**
     * BaseCommentCell.D7(): a tap on a comment (reply). With this on the tap waits for a second one; two on the same
     * comment are TikTok's own double-tap like, F7() -- the heart, its animation and the request, as TikTok does them.
     */
    public static void D7(Object cell) {
        if (!enabled() || call(cell, "i7", true)) { call(cell, "D7", false); return; }
        long now = android.os.SystemClock.uptimeMillis();
        int window = ViewConfiguration.getDoubleTapTimeout() + 60;
        if (pending != null && pendingCell.get() == cell && now - pendingAt <= window) {
            main.removeCallbacks(pending);
            pending = null;
            // TikTok's double-tap like only ever likes: a liked comment is unliked by its own heart, as a press would
            if (liked(cell)) unlike(cell); else call(cell, "F7", false);
            return;
        }
        if (pending != null) { main.removeCallbacks(pending); pending.run(); }
        pendingCell = new java.lang.ref.WeakReference<>(cell);
        pendingAt = now;
        final Runnable[] self = new Runnable[1];
        self[0] = () -> { if (pending == self[0]) pending = null; call(cell, "D7", false); };
        pending = self[0];
        main.postDelayed(pending, window);
    }

    /** D7 (tap), F7 (double-tap like) or i7 (managing comments: taps select, no likes); i7's answer is returned. */
    private static boolean call(Object cell, String name, boolean answer) {
        try {
            java.lang.reflect.Method method = name.equals("D7") ? tap : name.equals("F7") ? like : managing;
            if (method == null) {
                method = cell.getClass().getMethod(name);
                if (name.equals("D7")) tap = method; else if (name.equals("F7")) like = method; else managing = method;
            }
            Object result = method.invoke(cell);
            return answer && Boolean.TRUE.equals(result);
        } catch (Throwable error) {
            Diary.note("comment tap " + name + ": " + error);
            return false;
        }
    }

    /** Whether this account liked the cell's comment: O6() is its Comment, isUserDigged() TikTok's own answer. */
    private static boolean liked(Object cell) {
        try {
            Object comment = cell.getClass().getMethod("O6").invoke(cell);
            return comment != null && Boolean.TRUE.equals(comment.getClass().getMethod("isUserDigged").invoke(comment));
        } catch (Throwable error) { Diary.note("comment liked: " + error); return false; }
    }

    /** Presses the heart of the cell's row (RecyclerView.ViewHolder.itemView), which takes the like back. */
    private static void unlike(Object cell) {
        try {
            Object row = Class.forName("androidx.recyclerview.widget.RecyclerView$ViewHolder").getField("itemView").get(cell);
            if (row instanceof View) like((View) row);
        } catch (Throwable error) { Diary.note("comment unlike: " + error); }
    }

    /** Kept for the comment rows that call it; taps are now taken at D7. */
    static void attach(View row) {}

    /** The comment's own heart, found by what it says it is, or else the right-hand button with a picture in it. */
    private static void like(View row) {
        View heart = find(row);
        if (heart == null) { Diary.note("comment like: no heart in " + row.getClass().getName()); return; }
        heart.performClick();
        row.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
    }

    private static View find(View row) {
        View[] best = {null};
        float[] right = {-1};
        walk(row, row, best, right);
        return best[0];
    }

    private static void walk(View row, View view, View[] best, float[] right) {
        if (view.getVisibility() != View.VISIBLE) return;
        CharSequence said = view.getContentDescription();
        if (said != null && view.isClickable()) {
            String text = said.toString().toLowerCase(Locale.ROOT);
            boolean dislike = text.contains("dislike") || text.contains("не нрав") || text.contains("дизлайк");
            if (!dislike && (text.contains("like") || text.contains("нрав") || text.contains("лайк") || text.contains("вподоб"))) {
                best[0] = view; right[0] = Float.MAX_VALUE; return;
            }
        }
        if (right[0] < Float.MAX_VALUE && view != row && view.isClickable() && view.getWidth() < row.getWidth() / 3
                && hasPicture(view)) {
            float x = rightEdge(row, view);
            if (x > right[0]) { right[0] = x; best[0] = view; }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) walk(row, group.getChildAt(i), best, right);
        }
    }

    private static boolean hasPicture(View view) {
        if (view instanceof android.widget.ImageView) return true;
        if (!(view instanceof ViewGroup)) return false;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) if (hasPicture(group.getChildAt(i))) return true;
        return false;
    }

    private static float rightEdge(View row, View view) {
        float x = view.getWidth();
        for (View v = view; v != null && v != row; v = v.getParent() instanceof View ? (View) v.getParent() : null) x += v.getX();
        return x;
    }
}
