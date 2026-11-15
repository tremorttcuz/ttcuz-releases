package cat.ttcuz.framework;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What Plugin API 4 does inside the mod: finding views by a selector, the view tree as text, taps and swipes,
 * views hidden for as long as a plugin runs, the video player, TikTok's events and plugins calling one another.
 * PluginContext checks the permission and hands over to these.
 */
public final class PluginKit {
    private PluginKit() {}

    private static final Handler main = new Handler(Looper.getMainLooper());

    // ------------------------------------------------------------------ selectors

    /**
     * Views under root that match. A selector is one of
     *   #name   resource id name ("title", or "com.zhiliaoapp.musically:id/title")
     *   @text   text containing this, any case;  @=text  exactly this text
     *   ~text   content description containing this, any case
     *   .Name   class: its simple name, or its full name
     * and several of them joined by "|" match any one.
     */
    public static List<View> query(View root, String selector) {
        List<View> out = new ArrayList<>();
        if (root == null || selector == null || selector.trim().isEmpty()) return out;
        String[] any = selector.split("\\|");
        walk(root, any, out, 0);
        return out;
    }

    private static void walk(View view, String[] any, List<View> out, int depth) {
        if (depth > 64 || out.size() >= 500) return;
        for (String one : any) if (matches(view, one.trim())) { out.add(view); break; }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) walk(group.getChildAt(i), any, out, depth + 1);
        }
    }

    static boolean matches(View view, String selector) {
        if (selector.length() < 2) return false;
        char kind = selector.charAt(0);
        String value = selector.substring(1);
        switch (kind) {
            case '#': {
                String name = idName(view);
                if (name == null) return false;
                int colon = value.indexOf(":id/");
                return name.equals(colon >= 0 ? value.substring(colon + 4) : value);
            }
            case '@': {
                if (!(view instanceof TextView)) return false;
                CharSequence text = ((TextView) view).getText();
                if (text == null) return false;
                if (value.startsWith("=")) return text.toString().equals(value.substring(1));
                return text.toString().toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT));
            }
            case '~': {
                CharSequence said = view.getContentDescription();
                return said != null && said.toString().toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT));
            }
            case '.': {
                String name = view.getClass().getName();
                return name.equals(value) || view.getClass().getSimpleName().equals(value) || name.endsWith("." + value);
            }
            default:
                return false;
        }
    }

    private static String idName(View view) {
        int id = view.getId();
        if (id == View.NO_ID || (id >>> 24) == 0) return null;
        try { return view.getResources().getResourceEntryName(id); } catch (Throwable missing) { return null; }
    }

    /** The view tree as indented text: class, id name, text, description, size; for finding a selector. */
    public static String tree(View root, int maxDepth) {
        StringBuilder out = new StringBuilder();
        dump(root, out, 0, Math.max(1, Math.min(64, maxDepth)));
        return out.toString();
    }

    private static void dump(View view, StringBuilder out, int depth, int max) {
        if (view == null || depth > max || out.length() > 200_000) return;
        for (int i = 0; i < depth; i++) out.append("  ");
        out.append(view.getClass().getSimpleName());
        String name = idName(view);
        if (name != null) out.append(" #").append(name);
        if (view instanceof TextView && ((TextView) view).getText() != null && ((TextView) view).getText().length() > 0) {
            String text = ((TextView) view).getText().toString().replace('\n', ' ');
            out.append(" @\"").append(text.length() > 60 ? text.substring(0, 60) + "…" : text).append('"');
        }
        if (view.getContentDescription() != null) out.append(" ~\"").append(view.getContentDescription()).append('"');
        out.append(' ').append(view.getWidth()).append('x').append(view.getHeight());
        if (view.getVisibility() != View.VISIBLE) out.append(view.getVisibility() == View.GONE ? " gone" : " invisible");
        if (view.isClickable()) out.append(" clickable");
        out.append('\n');
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) dump(group.getChildAt(i), out, depth + 1, max);
        }
    }

    /** Where a view is on the screen: {left, top, right, bottom}. */
    public static int[] bounds(View view) {
        int[] at = new int[2];
        view.getLocationOnScreen(at);
        return new int[]{at[0], at[1], at[0] + view.getWidth(), at[1] + view.getHeight()};
    }

    // ------------------------------------------------------------------ touches

    /** A tap at a point of the screen, delivered as a finger would. */
    public static void tap(Activity activity, float x, float y) {
        long now = SystemClock.uptimeMillis();
        send(activity, now, now, MotionEvent.ACTION_DOWN, x, y);
        send(activity, now, now + 40, MotionEvent.ACTION_UP, x, y);
    }

    /** A drag from one point to another over `millis`, in frame-sized steps, without blocking. */
    public static void swipe(Activity activity, float x1, float y1, float x2, float y2, long millis) {
        final long down = SystemClock.uptimeMillis(), span = Math.max(50L, Math.min(5000L, millis));
        send(activity, down, down, MotionEvent.ACTION_DOWN, x1, y1);
        final int steps = (int) Math.max(2, span / 16);
        for (int i = 1; i <= steps; i++) {
            final float t = i / (float) steps;
            final boolean last = i == steps;
            main.postDelayed(() -> send(activity, down, SystemClock.uptimeMillis(), last ? MotionEvent.ACTION_UP : MotionEvent.ACTION_MOVE,
                    x1 + (x2 - x1) * t, y1 + (y2 - y1) * t), span * i / steps);
        }
    }

    private static void send(Activity activity, long down, long at, int action, float x, float y) {
        if (activity == null || activity.isFinishing()) return;
        MotionEvent event = MotionEvent.obtain(down, at, action, x, y, 0);
        try { activity.dispatchTouchEvent(event); } finally { event.recycle(); }
    }

    // ------------------------------------------------------------------ views kept hidden

    /** Per plugin: selectors whose views stay hidden while it runs. */
    private static final Map<String, List<String>> hidden = new ConcurrentHashMap<>();
    private static final Map<View, Boolean> watching = new WeakHashMap<>();
    private static final Map<View, Integer> before = new WeakHashMap<>();

    public static void hide(String plugin, String selector) {
        List<String> list = hidden.get(plugin);
        if (list == null) { list = new java.util.concurrent.CopyOnWriteArrayList<>(); hidden.put(plugin, list); }
        if (!list.contains(selector)) list.add(selector);
        Activity now = Screen.now();
        if (now != null) main.post(() -> apply(now));
    }

    public static void unhide(String plugin, String selector) {
        List<String> list = hidden.get(plugin);
        if (list != null) list.remove(selector);
        restore();
    }

    /** A plugin stopped: its hidden views come back. */
    public static void forget(String plugin) {
        hidden.remove(plugin);
        expose.remove(plugin);
        main.post(PluginKit::restore);
    }

    private static void restore() {
        synchronized (before) {
            for (Map.Entry<View, Integer> entry : new ArrayList<>(before.entrySet())) {
                View view = entry.getKey();
                if (view == null) continue;
                boolean still = false;
                for (List<String> list : hidden.values()) for (String s : list) still |= matches(view, s);
                if (!still) { view.setVisibility(entry.getValue()); before.remove(view); }
            }
        }
    }

    /** Called for each screen that comes forward: rules are applied now and after every layout of it. */
    static void apply(Activity activity) {
        if (hidden.isEmpty() || activity == null) return;
        final View decor = activity.getWindow().getDecorView();
        synchronized (watching) {
            if (!watching.containsKey(decor)) {
                watching.put(decor, Boolean.TRUE);
                final long[] last = {0};
                decor.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override public void onGlobalLayout() {
                        if (hidden.isEmpty()) return;
                        long now = SystemClock.uptimeMillis();
                        if (now - last[0] < 200) return;
                        last[0] = now;
                        sweep(decor);
                    }
                });
            }
        }
        sweep(decor);
    }

    private static void sweep(View decor) {
        for (List<String> list : hidden.values()) for (String selector : list) for (View view : query(decor, selector)) {
            if (view.getVisibility() == View.GONE) continue;
            synchronized (before) { if (!before.containsKey(view)) before.put(view, view.getVisibility()); }
            view.setVisibility(View.GONE);
        }
    }

    // ------------------------------------------------------------------ the player

    /** The video on screen (TikTok's Aweme), or null. */
    public static Object video() { return VideoActions.downloadPost(); }

    public static String videoId() { return VideoActions.currentId(); }

    private static Object call(Object target, String name, Object... args) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.getName().equals(name) || method.getParameterTypes().length != args.length) continue;
                method.setAccessible(true);
                return method.invoke(target, args);
            }
        }
        throw new NoSuchMethodException(name);
    }

    public static boolean playing() {
        Object player = PlayerTools.player();
        try { return player != null && Boolean.TRUE.equals(call(player, "isPlaying")); } catch (Exception error) { return false; }
    }

    /** TikTok 47.2.41's own tryPausePlay / resumePlay of the player in front. */
    public static boolean pause() {
        Object player = PlayerTools.player();
        try { if (player == null) return false; call(player, "LJLI", Boolean.FALSE); return true; } catch (Exception error) { Diary.note("plugin pause: " + error); return false; }
    }

    public static boolean play() {
        Object player = PlayerTools.player();
        try { if (player == null) return false; call(player, "LJIILLIIL"); return true; } catch (Exception error) { Diary.note("plugin play: " + error); return false; }
    }

    /** Playback speed of the video in front, 0.5 to 3. */
    public static boolean speed(float value) {
        Object player = PlayerTools.player();
        if (player == null || !PlayerTools.validSpeed(value)) return false;
        try { NativeRead.floating(player, "LIZ", value); return true; } catch (Exception error) { Diary.note("plugin speed: " + error); return false; }
    }

    /** Saves a post as the mod's own download button does (video, without the watermark when that is on). */
    public static void save(Object post) { VideoActions.save(post, false); }

    // ------------------------------------------------------------------ plugins calling one another

    private static final Map<String, Map<String, cat.ttcuz.framework.plugin.PluginContext.Callable>> expose = new ConcurrentHashMap<>();

    public static void expose(String plugin, String name, cat.ttcuz.framework.plugin.PluginContext.Callable callable) {
        Map<String, cat.ttcuz.framework.plugin.PluginContext.Callable> own = expose.get(plugin);
        if (own == null) { own = new ConcurrentHashMap<>(); expose.put(plugin, own); }
        if (callable == null) own.remove(name); else own.put(name, callable);
    }

    /** Calls what another running plugin exposed; null when it is not there. */
    public static Object call(String plugin, String name, Object argument) throws Exception {
        Map<String, cat.ttcuz.framework.plugin.PluginContext.Callable> own = expose.get(plugin);
        cat.ttcuz.framework.plugin.PluginContext.Callable callable = own == null ? null : own.get(name);
        return callable == null ? null : callable.call(argument);
    }

    // ------------------------------------------------------------------ TikTok's events

    /** An event for onTikTok: built only when some plugin listens. */
    static void event(String name, Object... pairs) {
        if (!PluginEngine.hasTikTokHooks()) return;
        Map<String, Object> data = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) data.put(String.valueOf(pairs[i]), pairs[i + 1]);
        PluginEngine.tiktok(name, java.util.Collections.unmodifiableMap(data));
    }

    /** The common fields of a post, for events about it. */
    static Object[] post(Object post) {
        Object author = NativeRead.get(post, "getAuthor");
        return new Object[]{"id", NativeRead.aid(post), "author", NativeRead.get(author, "getUniqueId"),
                "authorId", NativeRead.get(author, "getUid"), "description", NativeRead.get(post, "getDesc"), "post", post};
    }

    private static final Map<Activity, Boolean> front = new WeakHashMap<>();

    /** Foreground and background of the whole app, from the screens' own resume and pause. */
    static void resumed(Activity activity) {
        boolean was;
        synchronized (front) { was = !front.isEmpty(); front.put(activity, Boolean.TRUE); }
        if (!was) event("app:foreground", "screen", activity.getClass().getName());
        apply(activity);
    }

    static void paused(Activity activity) {
        boolean now;
        synchronized (front) { front.remove(activity); now = front.isEmpty(); }
        if (now) main.postDelayed(() -> { boolean empty; synchronized (front) { empty = front.isEmpty(); } if (empty) event("app:background"); }, 700L);
    }
}
