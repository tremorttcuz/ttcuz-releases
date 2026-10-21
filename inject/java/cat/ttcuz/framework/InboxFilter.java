package cat.ttcuz.framework;

import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewTreeObserver;
import java.lang.ref.WeakReference;

/** Correct only the native inbox filter fade, using the surface behind the bar. */
public final class InboxFilter {
    private static final int TAG = 0x54544644;

    private InboxFilter() {}

    public static void bind(Object holder, View bar) {
        Object value = NativeRead.field(holder, "LLJLLIL");
        if (!(value instanceof View) || bar == null) return;
        View fade = (View) value;
        if (fade.getTag(TAG) instanceof Binding) return;
        Object ink = NativeRead.field(fade, "LL");
        if (!(ink instanceof Paint)) return;
        Binding binding = new Binding(fade, bar, (Paint) ink);
        fade.setTag(TAG, binding);
        fade.addOnAttachStateChangeListener(binding);
        if (fade.isAttachedToWindow()) binding.onViewAttachedToWindow(fade);
    }

    private static final class Binding implements View.OnAttachStateChangeListener,
        ViewTreeObserver.OnPreDrawListener {
        final View fade;
        final WeakReference<View> bar;
        final Paint paint;
        ViewTreeObserver observer;
        Shader shader;
        int colour, width, direction;

        Binding(View fade, View bar, Paint paint) {
            this.fade = fade;
            this.bar = new WeakReference<>(bar);
            this.paint = paint;
        }

        public void onViewAttachedToWindow(View view) {
            observer = view.getViewTreeObserver();
            if (observer.isAlive()) observer.addOnPreDrawListener(this);
            onPreDraw();
        }

        public void onViewDetachedFromWindow(View view) {
            if (observer != null && observer.isAlive()) observer.removeOnPreDrawListener(this);
            observer = null;
        }

        public boolean onPreDraw() {
            View surface = bar.get();
            if (surface == null || fade.getWidth() <= 0) return true;
            int next = background(surface);
            int size = fade.getWidth(), rtl = fade.getLayoutDirection();
            if (shader == null || paint.getShader() != shader || colour != next || width != size || direction != rtl) {
                colour = next;
                width = size;
                direction = rtl;
                // Preserve the surface RGB at zero alpha; transparent black creates a grey fringe.
                shader = new LinearGradient(
                    rtl == View.LAYOUT_DIRECTION_RTL ? 0 : size, 0,
                    rtl == View.LAYOUT_DIRECTION_RTL ? size : 0, 0,
                    new int[] {next, next, next & 0x00ffffff},
                    new float[] {0, .6f, 1}, Shader.TileMode.CLAMP
                );
                paint.setShader(shader);
            }
            return true;
        }
    }

    static int background(View view) {
        for (int depth = 0; view != null && depth < 12; depth++) {
            if (view.getBackground() instanceof ColorDrawable) {
                int colour = ((ColorDrawable) view.getBackground()).getColor();
                if ((colour >>> 24) == 255) return colour;
            }
            Object parent = view.getParent();
            view = parent instanceof View ? (View) parent : null;
        }
        return Themes.isDark() ? 0xff000000 : 0xffffffff;
    }
}
