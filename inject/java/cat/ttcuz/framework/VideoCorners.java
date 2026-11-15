package cat.ttcuz.framework;

import android.app.Activity;
import android.graphics.Outline;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Videos as MD3 containers: the player of the feed and of an opened video gets the Extra Large shape (28 dp corners).
 *
 * The rounded view is the outermost ancestor of the video surface that is exactly the video's size, so the clip
 * follows the picture itself (a landscape clip becomes a rounded card) and never cuts the controls laid over the page.
 * The clip is an outline: RenderThread does it in hardware, nothing is redrawn and no frame costs more. The search runs
 * only after a layout change, at most eight times a second, with a node budget; the result is cached per surface.
 *
 * Rounded ahead, not live: the kind of view that holds a video (TikTok's own page container) is learned from the first
 * real video, and from then on every view of that kind is rounded as soon as it is laid out -- the next page of the feed
 * while it still waits off screen, a cover before its video starts -- so no corner is ever seen turning round.
 * Switching it off restores each view's own outline provider and clip flag.
 */
final class VideoCorners {
    private VideoCorners() {}

    public static final String KEY = "video_corners";
    static final float CORNER_DP = 28f;
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
        if (!value) restoreAll();
        for (Watch watch : watches.values()) { watch.dirty = true; View root = watch.root.get(); if (root != null) root.invalidate(); }
    }

    static void reloadSettings() { on = null; setEnabled(isEnabled()); }

    private static final String[] SCREENS = {
            "com.ss.android.ugc.aweme.main.MainActivity",
            "com.ss.android.ugc.aweme.detail.ui.DetailActivity",
    };

    /** What a view had before we rounded it. */
    private static final class Before {
        final ViewOutlineProvider provider; final boolean clip;
        Before(View view) { provider = view.getOutlineProvider(); clip = view.getClipToOutline(); }
    }
    private static final Map<View, Before> rounded = new WeakHashMap<>();
    private static final Map<View, Watch> watches = new WeakHashMap<>();

    private static final ViewOutlineProvider SHAPE = new ViewOutlineProvider() {
        @Override public void getOutline(View view, Outline outline) {
            float r = CORNER_DP * view.getResources().getDisplayMetrics().density;
            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), Math.min(r, Math.min(view.getWidth(), view.getHeight()) / 2f));
        }
    };

    static void watch(Activity activity) {
        String name = activity.getClass().getName(); boolean wanted = false;
        for (String screen : SCREENS) wanted |= screen.equals(name);
        if (!wanted) return;
        readHolders();
        View root = activity.getWindow().getDecorView();
        Watch watch = watches.get(root);
        if (watch == null) { watch = new Watch(root); watches.put(root, watch); }
        watch.attach();
    }

    static void stop(Activity activity) {
        Watch watch = watches.get(activity.getWindow().getDecorView());
        if (watch != null) watch.detach();
    }

    private static void restoreAll() {
        for (Map.Entry<View, Before> e : rounded.entrySet()) {
            View view = e.getKey(); if (view == null) continue;
            if (view.getOutlineProvider() == SHAPE) view.setOutlineProvider(e.getValue().provider);
            view.setClipToOutline(e.getValue().clip);
        }
        rounded.clear();
    }

    private static final class Watch implements ViewTreeObserver.OnPreDrawListener, ViewTreeObserver.OnGlobalLayoutListener {
        final WeakReference<View> root;
        WeakReference<ViewTreeObserver> observer = new WeakReference<>(null);
        boolean dirty = true; long next;
        Watch(View root) { this.root = new WeakReference<>(root); }

        void attach() {
            View view = root.get(); if (view == null) return;
            ViewTreeObserver now = view.getViewTreeObserver(), old = observer.get();
            if (now == old) return;
            if (old != null && old.isAlive()) { old.removeOnPreDrawListener(this); old.removeOnGlobalLayoutListener(this); }
            if (!now.isAlive()) return;
            now.addOnPreDrawListener(this); now.addOnGlobalLayoutListener(this);
            observer = new WeakReference<>(now); dirty = true;
        }

        void detach() {
            ViewTreeObserver old = observer.get();
            if (old != null && old.isAlive()) { old.removeOnPreDrawListener(this); old.removeOnGlobalLayoutListener(this); }
            observer = new WeakReference<>(null);
        }

        @Override public void onGlobalLayout() { dirty = true; }

        @Override public boolean onPreDraw() {android.os.Trace.beginSection("ttcuz video corners");try{
            if (!dirty) return true;
            long now = android.os.SystemClock.uptimeMillis();
            if (now < next) return true;
            View view = root.get();
            if (view == null || !view.hasWindowFocus() || !isEnabled()) { dirty = false; return true; }
            next = now + 120L; dirty = false;
            try { scan(view, view.getWidth(), view.getHeight(), 0, new int[]{4000}); }
            catch (Throwable error) { Diary.note("video corners: " + error); }
            return true;
        }finally{android.os.Trace.endSection();}}
    }

    /** Finds large video surfaces and rounds the outermost same-sized ancestor of each. */
    private static void scan(View view, int screenW, int screenH, int depth, int[] budget) {
        if (view == null || depth > 40 || budget[0]-- <= 0 || view.getVisibility() != View.VISIBLE) return;
        boolean large = view.getWidth() >= screenW * .5f && view.getHeight() >= screenH * .2f;
        if (view instanceof TextureView || view instanceof SurfaceView) {
            if (large) { View card = card(view); learn(card); round(card); }
            return;
        }
        // a page that will hold a video, before the video is there
        if (large && holders.contains(view.getClass())) round(view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) scan(group.getChildAt(i), screenW, screenH, depth + 1, budget);
        }
    }

    /** The outermost ancestor showing exactly the video: same size, not a scrolling list. */
    static View card(View surface) {
        View best = surface; int w = surface.getWidth(), h = surface.getHeight();
        Object parent = surface.getParent();
        for (int i = 0; i < 6 && parent instanceof ViewGroup; i++) {
            ViewGroup group = (ViewGroup) parent;
            String type = group.getClass().getName();
            if (type.contains("RecyclerView") || type.contains("ViewPager") || type.contains("ListView")) break;
            if (Math.abs(group.getWidth() - w) > 2 || Math.abs(group.getHeight() - h) > 2) break;
            best = group; parent = group.getParent();
        }
        return best;
    }

    /** TikTok's own classes seen holding a video; Android's plain layouts are never learned (they hold anything). */
    private static final java.util.Set<Class<?>> holders = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private static void learn(View card) {
        if (card instanceof TextureView || card instanceof SurfaceView) return;
        String name = card.getClass().getName();
        if (name.startsWith("android.") || name.startsWith("androidx.") || name.startsWith("com.google.")) return;
        if (!holders.add(card.getClass())) return;
        // kept, so the next start rounds ahead from its very first video
        StringBuilder names = new StringBuilder();
        for (Class<?> type : holders) names.append(names.length() == 0 ? "" : ",").append(type.getName());
        android.content.Context context = Ttcuz.context();
        if (context != null) context.getSharedPreferences(Ttcuz.PREFS, android.content.Context.MODE_PRIVATE).edit().putString(HOLDERS, names.toString()).apply();
    }
    private static final String HOLDERS = "video_corner_holders";
    private static boolean holdersRead;
    private static void readHolders() {
        if (holdersRead) return;
        android.content.Context context = Ttcuz.context();
        if (context == null) return;
        holdersRead = true;
        String names = context.getSharedPreferences(Ttcuz.PREFS, android.content.Context.MODE_PRIVATE).getString(HOLDERS, "");
        for (String name : names.split(",")) {
            if (name.isEmpty()) continue;
            try { holders.add(Class.forName(name, false, VideoCorners.class.getClassLoader())); } catch (Throwable ignored) { }
        }
    }

    private static void round(View view) {
        if (view.getOutlineProvider() == SHAPE && view.getClipToOutline()) return;
        if (!rounded.containsKey(view)) rounded.put(view, new Before(view));
        view.setOutlineProvider(SHAPE);
        view.setClipToOutline(true);
    }
}
