package cat.ttcuz.framework;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The buttons beside a video in the feed, as the settings' "Кнопки в ленте" ask:
 *
 * - no "Follow" plus under the author's picture;
 * - no long press on like (repost), on comments (quick emoji) or on favourites (collections): a hold does nothing,
 *   a tap is a tap;
 * - no story ring round the author's picture, and a tap on it opens the profile;
 * - a follow only on a second tap.
 *
 * Holds and taps are caught in the window, before TikTok sees them (IslandTouch hands every touch here), so it does
 * not matter how TikTok listens for a hold on each button.
 */
public final class FeedButtons {
    private FeedButtons() {}

    static final String HIDE_FOLLOW = "feed_hide_follow", NO_LIKE_HOLD = "feed_no_like_hold",
            NO_COMMENT_HOLD = "feed_no_comment_hold", NO_FAV_HOLD = "feed_no_fav_hold",
            NO_STORY_RING = "feed_no_story_ring", DOUBLE_FOLLOW = "feed_double_tap_follow";

    /** The follow button inside the author's picture (TikTok 47.7.3 ids, as FeedAvatarDefaultAssem finds them): its touch area, plus and animation. */
    private static final int FOLLOW_AREA = 0x7f0a303c, FOLLOW_PLUS = 0x7f0a304e, FOLLOW_ANIMATION = 0x7f0a3021,
            AVATAR = 0x7f0a9ba6;

    private static SharedPreferences prefs() {
        Context context = Ttcuz.context();
        return context == null ? null : context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
    }

    static boolean on(String key) {
        SharedPreferences store = prefs();
        return store != null && store.getBoolean(key, false);
    }

    static void set(String key, boolean value) {
        SharedPreferences store = prefs();
        if (store != null) store.edit().putBoolean(key, value).apply();
        if (key.equals(HIDE_FOLLOW)) refreshFollow();
    }

    // ------------------------------------------------------------------ the author's picture

    /** Follow areas and avatars by the component they belong to, to read the author from at a tap. */
    private static final Map<View, WeakReference<Object>> followAreas = new WeakHashMap<>(), avatars = new WeakHashMap<>();
    private static final Map<View, Integer> wanted = new WeakHashMap<>();

    /** FeedAvatarDefaultAssem was created or bound. */
    static void avatar(Object component) {
        if (component == null || !component.getClass().getName().endsWith("FeedAvatarDefaultAssem")) return;
        // FeedAvatarDefaultAssem is not a slot assem: it has no getContentView, its view is ReusedUIAssem.LJJIZ()
        Object content = NativeRead.get(component, "getContentView");
        if (!(content instanceof View)) content = NativeRead.get(component, "getContainerView");
        if (!(content instanceof View)) return;
        View root = (View) content;
        WeakReference<Object> owner = new WeakReference<>(component);
        View area = root.findViewById(FOLLOW_AREA);
        View avatar = root.findViewById(AVATAR);
        synchronized (followAreas) {
            if (area != null) followAreas.put(area, owner);
            if (avatar != null) avatars.put(avatar, owner);
        }
        if (area != null) applyFollow(area);
    }

    private static void applyFollow(View area) {
        boolean hide = on(HIDE_FOLLOW);
        Integer theirs;
        synchronized (followAreas) { theirs = wanted.get(area); }
        int next = hide ? View.INVISIBLE : theirs == null ? area.getVisibility() : theirs;
        if (area.getVisibility() != next) area.setVisibility(next);
        area.setClickable(!hide);
    }

    private static void refreshFollow() {
        java.util.List<View> areas;
        synchronized (followAreas) { areas = new java.util.ArrayList<>(followAreas.keySet()); }
        for (View area : areas) if (area != null) area.post(() -> applyFollow(area));
    }

    /**
     * Every View.setVisibility in TikTok passes FeedRail first, which asks here: TikTok showing the follow area
     * again (after a follow state change) is remembered, and the area stays hidden.
     */
    static boolean visibility(View view, int visibility) {
        boolean known;
        synchronized (followAreas) {
            known = followAreas.containsKey(view);
            if (known) wanted.put(view, visibility);
        }
        if (!known || !on(HIDE_FOLLOW)) return false;
        if (view.getVisibility() != View.INVISIBLE) view.setVisibility(View.INVISIBLE);
        return true;
    }

    /** The feed avatar's story ring (X.0B5v) strokes its arcs here: none at all when the ring is switched off. */
    public static void drawArc(Canvas canvas, RectF oval, float start, float sweep, boolean center, Paint paint) {
        if (on(NO_STORY_RING)) return;
        AvatarShape.drawArc(canvas, oval, start, sweep, center, paint);
    }

    // ------------------------------------------------------------------ the buttons' holds, at their source

    private static volatile Method likeLongClick, commentHold;

    /** VideoDiggAssem.Lq(view): like's long click. Switched off it is taken and nothing happens. */
    public static boolean Lq(Object assem, View view) {
        if (on(NO_LIKE_HOLD)) return true;
        try {
            Method method = likeLongClick;
            if (method == null) likeLongClick = method = assem.getClass().getMethod("Lq", View.class);
            return Boolean.TRUE.equals(method.invoke(assem, view));
        } catch (Throwable error) {
            Diary.note("like hold: " + error);
            return false;
        }
    }

    /** VideoCommentAssem.Pq(): the comment button's hold (quick emoji). */
    public static void Pq(Object assem) {
        if (on(NO_COMMENT_HOLD)) return;
        try {
            Method method = commentHold;
            if (method == null) commentHold = method = assem.getClass().getMethod("Pq");
            method.invoke(assem);
        } catch (Throwable error) {
            Diary.note("comment hold: " + error);
        }
    }

    /** The favourite button's hold (collections) asks first. */
    public static boolean favoriteHold() { return !on(NO_FAV_HOLD); }

    // ------------------------------------------------------------------ touches, before TikTok

    private static final Handler main = new Handler(Looper.getMainLooper());
    /** The gesture now being held back: swallowed until it ends. */
    private static boolean swallowing, guarding;
    private static float downX, downY;
    private static WeakReference<View> armedFollow = new WeakReference<>(null);
    private static long armedAt;
    private static Runnable holdCancel;

    /**
     * One touch event of the window. True when it is taken here and must not reach TikTok. `cancel` sends TikTok a
     * CANCEL for the gesture it has seen so far.
     */
    static boolean touch(Activity activity, MotionEvent event, java.util.function.Consumer<MotionEvent> deliver) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            swallowing = false;
            guarding = false;
            cancelHold();
            downX = event.getRawX();
            downY = event.getRawY();
            // double tap to follow: the first tap only arms it
            View follow = hitFollow(downX, downY);
            if (follow != null && on(DOUBLE_FOLLOW) && !on(HIDE_FOLLOW)) {
                long now = SystemClock.uptimeMillis();
                if (armedFollow.get() != follow || now - armedAt > 1500L) {
                    armedFollow = new WeakReference<>(follow);
                    armedAt = now;
                    swallowing = true;
                    follow.animate().cancel();
                    follow.setScaleX(1.25f); follow.setScaleY(1.25f);
                    follow.animate().scaleX(1f).scaleY(1f).setDuration(260L).start();
                    follow.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                    Screen.say(Text.t("Нажмите ещё раз, чтобы подписаться"));
                    return true;
                }
                armedFollow = new WeakReference<>(null);
            }
            // the author's picture with its ring hidden opens the profile, never the stories
            if (on(NO_STORY_RING)) {
                Object owner = hitAvatar(downX, downY);
                if (owner != null) { swallowing = true; pendingProfile = new WeakReference<>(owner); return true; }
            }
            return false;
        }
        if (action == MotionEvent.ACTION_UP && event.getEventTime() - event.getDownTime() < 500L
                && Math.hypot(event.getRawX() - downX, event.getRawY() - downY) < ViewConfiguration.get(activity).getScaledTouchSlop())
            Stickers.tapped();
        if (swallowing) {
            if (action == MotionEvent.ACTION_UP) {
                swallowing = false;
                Object owner = pendingProfile.get();
                pendingProfile = new WeakReference<>(null);
                if (owner != null && Math.hypot(event.getRawX() - downX, event.getRawY() - downY) < ViewConfiguration.get(activity).getScaledTouchSlop() * 2)
                    openProfile(activity, owner);
            } else if (action == MotionEvent.ACTION_CANCEL) {
                swallowing = false;
                pendingProfile = new WeakReference<>(null);
            }
            return true;
        }
        if (guarding && (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_MOVE && Math.hypot(event.getRawX() - downX, event.getRawY() - downY) > ViewConfiguration.get(activity).getScaledTouchSlop())) {
            guarding = false;
            cancelHold();
        }
        return false;
    }

    private static WeakReference<Object> pendingProfile = new WeakReference<>(null);

    private static void cancelHold() {
        if (holdCancel != null) main.removeCallbacks(holdCancel);
        holdCancel = null;
    }

    private static final int[] at = new int[2];

    static boolean contains(View view, float x, float y) {
        if (view == null || !view.isShown() || view.getAlpha() < .05f) return false;
        view.getLocationOnScreen(at);
        return x >= at[0] && x < at[0] + view.getWidth() && y >= at[1] && y < at[1] + view.getHeight();
    }

    private static View hitFollow(float x, float y) {
        synchronized (followAreas) {
            for (View area : followAreas.keySet()) if (area != null && contains(area, x, y)) return area;
        }
        return null;
    }

    private static Object hitAvatar(float x, float y) {
        synchronized (followAreas) {
            for (Map.Entry<View, WeakReference<Object>> entry : avatars.entrySet()) {
                View avatar = entry.getKey();
                if (avatar == null || !contains(avatar, x, y)) continue;
                View follow = null;
                for (View area : followAreas.keySet()) if (area != null && contains(area, x, y)) follow = area;
                if (follow != null && follow.getVisibility() == View.VISIBLE) return null;
                return entry.getValue().get();
            }
        }
        return null;
    }

    /** The author of the video in that cell, opened the way TikTok opens a profile: its router. */
    private static void openProfile(Activity activity, Object component) {
        try {
            Object post = NativeRead.get(NativeRead.item(component), "getAweme");
            Object author = NativeRead.get(post, "getAuthor");
            Object uid = NativeRead.get(author, "getUid"), secUid = NativeRead.get(author, "getSecUid");
            if (uid == null) return;
            Class<?> router = Class.forName("com.bytedance.router.SmartRouter");
            Object route = router.getMethod("buildRoute", Context.class, String.class).invoke(null, activity, "aweme://user/profile/" + uid);
            if (secUid != null) route = route.getClass().getMethod("withParam", String.class, String.class).invoke(route, "sec_user_id", String.valueOf(secUid));
            route = route.getClass().getMethod("withParam", String.class, String.class).invoke(route, "enter_from", "homepage_hot");
            route.getClass().getMethod("open").invoke(route);
        } catch (Throwable error) {
            Diary.note("feed profile: " + error);
        }
    }
}
