package cat.ttcuz.framework;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * How the feed turns from one video to the next, like a launcher's scroll effects: cube, depth, stack, zoom, tilt,
 * fade. TikTok's feed is its own VerticalViewPager, which calls a page transformer (a ViewPager.PageTransformer it
 * keeps in a field) for every page on every scroll frame with the page's position: 0 in view, -1 above, +1 below.
 * The mod puts its own transformer there; "none" takes it out again and every page is put back as it was.
 */
final class FeedScroll {
    private FeedScroll() {}

    static final String KEY = "feed_scroll_effect", KEY_STRENGTH = "feed_scroll_strength",
            KEY_DEPTH = "feed_scroll_depth", KEY_BLUR = "feed_scroll_blur";
    static final int NONE = 0, CUBE = 1, DEPTH = 2, STACK = 3, ZOOM = 4, TILT = 5, FADE = 6;
    static final String[] NAMES = {"Без эффекта", "Куб", "Глубина", "Стопка", "Отдаление", "Наклон", "Затухание"};
    private static final String PAGER = "com.ss.android.ugc.aweme.common.widget.VerticalViewPager";

    private static volatile int effect = -1, strength = -1;

    private static android.content.SharedPreferences prefs() {
        Context context = Ttcuz.context();
        return context == null ? null : context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
    }

    static int effect() {
        if (effect >= 0) return effect;
        android.content.SharedPreferences p = prefs();
        effect = p == null ? NONE : Math.max(0, Math.min(NAMES.length - 1, p.getInt(KEY, NONE)));
        return effect;
    }

    /** 10..100 percent of the full effect. */
    static int strength() {
        if (strength >= 0) return strength;
        android.content.SharedPreferences p = prefs();
        strength = p == null ? 70 : Math.max(10, Math.min(100, p.getInt(KEY_STRENGTH, 70)));
        return strength;
    }

    static void set(int value) {
        effect = Math.max(0, Math.min(NAMES.length - 1, value));
        android.content.SharedPreferences p = prefs();
        if (p != null) p.edit().putInt(KEY, effect).apply();
        refresh();
    }

    static void setStrength(int value) {
        strength = Math.max(10, Math.min(100, value));
        android.content.SharedPreferences p = prefs();
        if (p != null) p.edit().putInt(KEY_STRENGTH, strength).apply();
    }

    static String name() { return Text.t(NAMES[effect()]); }

    private static volatile Boolean depth, blur;

    /** On top of any effect: the page that leaves sinks back and darkens, as if it fell away from the screen. */
    static boolean depth() {
        Boolean known = depth;
        if (known != null) return known;
        android.content.SharedPreferences p = prefs();
        depth = p != null && p.getBoolean(KEY_DEPTH, false);
        return depth;
    }

    /** On top of any effect: the pages blur while they pass (Android 12+). */
    static boolean blur() {
        Boolean known = blur;
        if (known != null) return known;
        android.content.SharedPreferences p = prefs();
        blur = p != null && p.getBoolean(KEY_BLUR, false) && Blur.gpu() && Ttcuz.is64();
        return blur;
    }

    static void setDepth(boolean value) {
        depth = value;
        android.content.SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean(KEY_DEPTH, value).apply();
        refresh();
    }

    static void setBlur(boolean value) {
        blur = value && Blur.gpu();
        android.content.SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean(KEY_BLUR, value).apply();
        refresh();
    }

    /** Any transform at all: an effect, or depth or blur on their own. */
    private static boolean active() { return effect() != NONE || depth() || blur(); }

    /** Each feed pager we hold, with the transformer it had before. */
    private static final Map<View, Object> pagers = new WeakHashMap<>();
    private static WeakReference<Activity> main = new WeakReference<>(null);

    /** The main screen came up or back: its feeds are found (now and once things have laid out) and fitted. */
    static void attach(final Activity activity) {
        if (activity == null) return;
        main = new WeakReference<>(activity);
        final View decor = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
        if (decor == null) return;
        // a slow phone builds its feeds later: looked for again a few times while it settles
        decor.post(FeedScroll::refresh);
        for (long later : new long[]{600L, 1500L, 4000L, 9000L}) decor.postDelayed(FeedScroll::refresh, later);
    }

    /**
     * Every second and a half from the island's own check (main screen only): the known feeds keep the transformer
     * (TikTok may set its own again), and a screen without any known feed is searched once more. A field read per
     * feed; the tree is walked only while no feed is known.
     */
    static void check() {
        if (!active() && pagers.isEmpty()) return;
        boolean none = true;
        for (View pager : new java.util.ArrayList<>(pagers.keySet())) if (pager != null && pager.isAttachedToWindow()) { none = false; fit(pager); }
        if (none) refresh();
    }

    /** Puts the chosen effect on every feed of the main screen, or takes it off. */
    static void refresh() {
        Activity activity = main.get();
        View decor = activity == null || activity.getWindow() == null ? null : activity.getWindow().getDecorView();
        if (decor != null) find(decor, 0);
        for (Map.Entry<View, Object> e : new java.util.ArrayList<>(pagers.entrySet())) fit(e.getKey());
    }

    private static void find(View view, int depth) {
        if (view == null || depth > 30) return;
        if (view.getClass().getName().equals(PAGER)) { if (!pagers.containsKey(view)) pagers.put(view, NO_TRANSFORMER); return; }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) find(group.getChildAt(i), depth + 1);
        }
    }

    private static final Object NO_TRANSFORMER = new Object();
    private static final Map<Class<?>, Field> fields = new WeakHashMap<>();
    private static final Map<Class<?>, Object> ours = new WeakHashMap<>();

    /** The pager's transformer field: of an interface type whose one method takes (View, float). */
    private static Field field(Class<?> type) {
        if (fields.containsKey(type)) return fields.get(type);
        Field found = null;
        for (Class<?> c = type; c != null && found == null && c != ViewGroup.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                Class<?> t = f.getType();
                if (!t.isInterface() || t.getDeclaredMethods().length != 1) continue;
                Class<?>[] args = t.getDeclaredMethods()[0].getParameterTypes();
                if (args.length == 2 && args[0] == View.class && args[1] == float.class && t.getName().startsWith("X.")) { found = f; break; }
            }
        }
        if (found != null) found.setAccessible(true);
        fields.put(type, found);
        return found;
    }

    private static void fit(View pager) {
        try {
            Field slot = field(pager.getClass());
            if (slot == null) return;
            Object now = slot.get(pager), mine = transformer(slot.getType());
            if (!active()) {
                if (now == mine) {
                    Object was = pagers.get(pager);
                    slot.set(pager, was == NO_TRANSFORMER ? null : was);
                    reset((ViewGroup) pager);
                }
                return;
            }
            if (now != mine) {
                pagers.put(pager, now == null ? NO_TRANSFORMER : now);
                slot.set(pager, mine);
            }
        } catch (Throwable error) { Diary.note("feed scroll: " + error); }
    }

    /** One transformer object per interface type: a proxy that calls transform. */
    private static Object transformer(final Class<?> type) {
        Object made = ours.get(type);
        if (made != null) return made;
        made = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (args != null && args.length == 2 && args[0] instanceof View) {
                try { transform((View) args[0], (Float) args[1]); } catch (Throwable error) { Diary.note("feed scroll: " + error); }
                return null;
            }
            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
            if (method.getName().equals("equals")) return proxy == args[0];
            if (method.getName().equals("toString")) return "ttcuz feed scroll";
            return null;
        });
        ours.put(type, made);
        return made;
    }

    private static void reset(ViewGroup pager) {
        for (int i = 0; i < pager.getChildCount(); i++) clear(pager.getChildAt(i));
    }

    /** What we put on a page: its dimming foreground (and the one TikTok had), the blur radius and camera set. */
    private static final class Look {
        android.graphics.drawable.Drawable before;
        final android.graphics.drawable.ColorDrawable dim = new android.graphics.drawable.ColorDrawable(0xFF000000);
        float blurred = 0f, camera = -1f;
        boolean dimming;
    }
    private static final Map<View, Look> looks = new WeakHashMap<>();

    /** A page back as TikTok placed it; nothing is written when it already is (pages off screen cost nothing). */
    private static void clear(View page) {
        if (page.getRotationX() != 0f) page.setRotationX(0f);
        if (page.getScaleX() != 1f) page.setScaleX(1f);
        if (page.getScaleY() != 1f) page.setScaleY(1f);
        if (page.getAlpha() != 1f) page.setAlpha(1f);
        if (page.getTranslationY() != 0f) page.setTranslationY(0f);
        if (page.getTranslationZ() != 0f) page.setTranslationZ(0f);
        Look look = looks.get(page);
        if (look == null) return;
        if (look.dimming) { page.setForeground(look.before); look.dimming = false; }
        if (look.blurred != 0f && Build.VERSION.SDK_INT >= 31) { page.setRenderEffect(null); look.blurred = 0f; }
    }

    private static Look look(View page) {
        Look look = looks.get(page);
        if (look == null) { look = new Look(); looks.put(page, look); }
        return look;
    }

    /**
     * One page, `p` pages away from the one in view (negative above): the chosen effect, as strong as chosen, then depth
     * and blur on top. Every size is a share of the page (the cube's camera too), so it looks the same on every phone.
     * Fading darkens with a black foreground instead of the page's alpha: a translucent full-screen page with a video
     * in it costs an offscreen copy every frame, a black rectangle costs nothing. Only what changed is written.
     */
    private static final float DEAD = .03f;

    static void transform(View page, float p) {
        int kind = effect();
        boolean deep = depth(), soft = blur();
        // a page at rest is left exactly as TikTok placed it: exactly in view, or at ±1 and past (off screen). Everything
        // below goes to nothing as p goes to 0, continuously: no cut-off near the end of a swipe (a page held in place
        // by a shift of p * h jumped by that shift when the effect was dropped there)
        if ((kind == NONE && !deep && !soft) || p == 0f || p <= -1f || p >= 1f) { clear(page); return; }
        // which page is drawn over which is decided only while the pages are clearly apart: the settling overshoot
        // around 0 used to swap them for a frame, a blink
        boolean apart = Math.abs(p) >= .03f;
        // a dead zone, but a soft one: the first 3 % of a page's way counts as nothing and the effect grows from zero
        // past it. Holding a video (for 2x) jitters the pager by a hair; with no dead zone the effect followed that
        // jitter and the video jumped, and a hard cut-off jumped at its edge
        float k = strength() / 100f, h = page.getHeight();
        float raw = Math.max(-1f, Math.min(1f, p)), past = Math.max(0f, Math.abs(raw) - DEAD) / (1f - DEAD);
        if (past == 0f) { clear(page); return; }
        float c = Math.signum(raw) * past, a = past;
        p = c;
        float rotation = 0f, scale = 1f, shift = 0f, z = 0f, dark = 0f, pivotY = h / 2f;
        switch (kind) {
            case CUBE:
                // the pages are the faces of a cube turning over its horizontal edge
                pivotY = p < 0 ? h : 0f;
                rotation = -90f * k * c;
                break;
            case DEPTH:
                // the one leaving sinks back and darkens; the one coming slides over it
                if (p < 0) { scale = 1f - .25f * k * a; shift = -p * h; dark = .6f * k * a; z = apart ? -1f : 0f; } else z = apart ? 1f : 0f;
                break;
            case STACK:
                // the current page stays put and the next one is laid over it like a card
                if (p < 0) { shift = -p * h; scale = 1f - .08f * k * a; dark = .3f * k * a; z = apart ? -1f : 0f; } else z = apart ? 1f : 0f;
                break;
            case ZOOM:
                // both pages draw back a little while they pass
                scale = 1f - .15f * k * a;
                dark = .3f * k * a;
                break;
            case TILT:
                rotation = 30f * k * c;
                break;
            case FADE:
                dark = k * a;
                break;
            default:
                break;
        }
        if (deep) {
            // depth: further from the middle is further away -- smaller and darker
            scale *= 1f - .12f * a;
            dark = Math.min(1f, dark + .35f * a);
        }
        if (page.getPivotY() != pivotY) page.setPivotY(pivotY);
        if (page.getPivotX() != page.getWidth() / 2f) page.setPivotX(page.getWidth() / 2f);
        if (page.getRotationX() != rotation) {
            // the camera 3.5 page heights away: the same perspective on every screen. Android divides the value by the
            // screen's dpi and the camera works in 1/72 inch, so h * 3.5 * 160 / 72 * dpi / 160 puts it at 3.5 h.
            Look look = look(page);
            if (look.camera != h) { page.setCameraDistance(h * 3.5f / 72f * page.getResources().getDisplayMetrics().densityDpi); look.camera = h; }
            page.setRotationX(rotation);
        }
        if (page.getScaleX() != scale) { page.setScaleX(scale); page.setScaleY(scale); }
        if (page.getTranslationY() != shift) page.setTranslationY(shift);
        if (page.getTranslationZ() != z) page.setTranslationZ(z);
        if (page.getAlpha() != 1f) page.setAlpha(1f);
        Look look = look(page);
        int alpha = Math.round(255 * Math.max(0f, Math.min(1f, dark)));
        if (alpha > 0) {
            if (!look.dimming) { look.before = page.getForeground(); page.setForeground(look.dim); look.dimming = true; }
            if (look.dim.getAlpha() != alpha) look.dim.setAlpha(alpha);
        } else if (look.dimming) { page.setForeground(look.before); look.dimming = false; }
        if (soft && Build.VERSION.SDK_INT >= 31) {
            // up to 2.5 % of the page's height, in 2 px steps: a new blur object only when the radius really changes
            float radius = Math.round(h * .025f * k * a / 2f) * 2f;
            if (radius != look.blurred) {
                page.setRenderEffect(radius > 0f ? android.graphics.RenderEffect.createBlurEffect(radius, radius, android.graphics.Shader.TileMode.CLAMP) : null);
                look.blurred = radius;
            }
        }
    }
}
