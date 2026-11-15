package cat.ttcuz.framework;

import android.graphics.ColorMatrixColorFilter;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewTreeObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * What a plugin with "effects" draws over views: blur, motion blur, colour matrices and AGSL shaders, all as
 * RenderEffect on the render thread (Android 12+; older phones simply get none). Everything a plugin set is
 * taken off again when it stops.
 */
public final class PluginEffects {
    private PluginEffects() {}

    public static boolean supported() { return Build.VERSION.SDK_INT >= 31; }
    public static boolean shaders() { return Build.VERSION.SDK_INT >= 33; }

    /** view → the plugin that put an effect on it */
    private static final Map<View, String> owners = new WeakHashMap<>();
    /** view → its shader, so a uniform can be changed later */
    private static final Map<View, Object> shaders = new WeakHashMap<>();
    /** what each plugin runs: motion blurs and frame loops, stopped when it stops */
    private static final Map<String, List<Runnable>> running = new java.util.HashMap<>();

    private static void own(String plugin, View view) { synchronized (owners) { owners.put(view, plugin); } }

    private static void keep(String plugin, Runnable stop) {
        synchronized (running) {
            List<Runnable> list = running.get(plugin);
            if (list == null) running.put(plugin, list = new ArrayList<>());
            list.add(stop);
        }
    }

    public static boolean blur(String plugin, View view, float x, float y) {
        if (!supported() || view == null) return false;
        own(plugin, view);
        float rx = Math.max(0f, Math.min(150f, x)), ry = Math.max(0f, Math.min(150f, y));
        view.setRenderEffect(rx < 0.5f && ry < 0.5f ? null : RenderEffect.createBlurEffect(Math.max(rx, 0.01f), Math.max(ry, 0.01f), Shader.TileMode.CLAMP));
        return true;
    }

    public static boolean matrix(String plugin, View view, float[] matrix) {
        if (!supported() || view == null) return false;
        if (matrix == null || matrix.length != 20) throw new IllegalArgumentException("a colour matrix has 20 numbers");
        own(plugin, view);
        view.setRenderEffect(RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(matrix)));
        return true;
    }

    /** An AGSL shader over the view: it reads the view as "uniform shader content". Returns the RuntimeShader. */
    public static Object shader(String plugin, View view, String agsl) {
        if (!shaders() || view == null) return null;
        android.graphics.RuntimeShader shader = new android.graphics.RuntimeShader(agsl);
        own(plugin, view);
        synchronized (shaders) { shaders.put(view, shader); }
        view.setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "content"));
        return shader;
    }

    /** Changes a float uniform of the view's shader and redraws with it. */
    public static boolean uniform(View view, String name, float... values) {
        if (!shaders() || view == null) return false;
        Object found;
        synchronized (shaders) { found = shaders.get(view); }
        if (!(found instanceof android.graphics.RuntimeShader)) return false;
        android.graphics.RuntimeShader shader = (android.graphics.RuntimeShader) found;
        shader.setFloatUniform(name, values);
        view.setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "content"));
        return true;
    }

    public static void clear(View view) {
        if (!supported() || view == null) return;
        synchronized (shaders) { shaders.remove(view); }
        synchronized (owners) { owners.remove(view); }
        view.setRenderEffect(null);
    }

    /**
     * Blurs the view along the way it moves on screen, as much as it moved since the last frame. It listens to the
     * frames the view's window draws anyway, so a view that stands still costs nothing; the last frame of a movement
     * clears the blur. Returns what stops it.
     */
    public static Runnable motionBlur(final String plugin, final View view, final float strength) {
        if (!supported() || view == null) return () -> {};
        own(plugin, view);
        final int[] at = new int[2], last = {Integer.MIN_VALUE, 0};
        final float[] now = {0f, 0f};
        final float scale = Math.max(0.05f, Math.min(4f, strength));
        final ViewTreeObserver.OnPreDrawListener listener = () -> {
            if (!view.isAttachedToWindow()) return true;
            view.getLocationOnScreen(at);
            if (last[0] != Integer.MIN_VALUE) {
                // a little smoothing, so one uneven frame does not flash
                float dx = Math.abs(at[0] - last[0]) * scale * 0.6f, dy = Math.abs(at[1] - last[1]) * scale * 0.6f;
                float nx = Math.min(60f, now[0] * 0.35f + dx * 0.65f), ny = Math.min(60f, now[1] * 0.35f + dy * 0.65f);
                if (nx < 0.6f) nx = 0f;
                if (ny < 0.6f) ny = 0f;
                if (Math.abs(nx - now[0]) > 0.3f || Math.abs(ny - now[1]) > 0.3f || (nx == 0f && ny == 0f && (now[0] != 0f || now[1] != 0f))) {
                    now[0] = nx; now[1] = ny;
                    view.setRenderEffect(nx == 0f && ny == 0f ? null : RenderEffect.createBlurEffect(Math.max(nx, 0.01f), Math.max(ny, 0.01f), Shader.TileMode.CLAMP));
                }
            }
            last[0] = at[0]; last[1] = at[1];
            return true;
        };
        final View.OnAttachStateChangeListener attach = new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) { last[0] = Integer.MIN_VALUE; v.getViewTreeObserver().addOnPreDrawListener(listener); }
            @Override public void onViewDetachedFromWindow(View v) { v.getViewTreeObserver().removeOnPreDrawListener(listener); }
        };
        view.addOnAttachStateChangeListener(attach);
        if (view.isAttachedToWindow()) view.getViewTreeObserver().addOnPreDrawListener(listener);
        final Runnable stop = () -> view.post(() -> {
            view.removeOnAttachStateChangeListener(attach);
            view.getViewTreeObserver().removeOnPreDrawListener(listener);
            view.setRenderEffect(null);
        });
        keep(plugin, stop);
        return stop;
    }

    /** A callback on every frame until stopped (for the plugin's own animations). */
    public static Runnable everyFrame(String plugin, final java.util.function.LongConsumer frame) {
        final boolean[] on = {true};
        final Choreographer.FrameCallback[] tick = new Choreographer.FrameCallback[1];
        tick[0] = nanos -> {
            if (!on[0]) return;
            try { frame.accept(nanos); }
            catch (Throwable error) { on[0] = false; Diary.note("plugin " + plugin + " frame: " + error); return; }
            Choreographer.getInstance().postFrameCallback(tick[0]);
        };
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> Choreographer.getInstance().postFrameCallback(tick[0]));
        Runnable stop = () -> on[0] = false;
        keep(plugin, stop);
        return stop;
    }

    /** The plugin stopped: its loops end and its effects come off. */
    public static void stop(final String plugin) {
        List<Runnable> stops;
        synchronized (running) { stops = running.remove(plugin); }
        if (stops != null) for (Runnable stop : stops) try { stop.run(); } catch (Throwable ignored) { }
        final List<View> views = new ArrayList<>();
        synchronized (owners) { for (Map.Entry<View, String> e : owners.entrySet()) if (plugin.equals(e.getValue())) views.add(e.getKey()); }
        if (!views.isEmpty() && supported()) new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> { for (View v : views) clear(v); });
    }
}
