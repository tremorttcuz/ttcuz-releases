package cat.ttcuz.framework;

import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewTreeObserver;
import java.lang.ref.WeakReference;

public final class InboxFilter {
    private static final int TAG = 1414809156;

    private InboxFilter() {
    }

    public static void bind(Object obj, View view) {
        Object field = NativeRead.field(obj, "LLJLLIL");
        if (!(field instanceof View) || view == null) {
            return;
        }
        View view2 = (View) field;
        if (view2.getTag(TAG) instanceof Binding) {
            return;
        }
        Object field2 = NativeRead.field(view2, "LL");
        if (field2 instanceof Paint) {
            Binding binding = new Binding(view2, view, (Paint) field2);
            view2.setTag(TAG, binding);
            view2.addOnAttachStateChangeListener(binding);
            if (view2.isAttachedToWindow()) {
                binding.onViewAttachedToWindow(view2);
            }
        }
    }

    private static final class Binding implements View.OnAttachStateChangeListener, ViewTreeObserver.OnPreDrawListener {
        final WeakReference<View> bar;
        int colour;
        int direction;
        final View fade;
        ViewTreeObserver observer;
        final Paint paint;
        Shader shader;
        int width;

        Binding(View view, View view2, Paint paint) {
            this.fade = view;
            this.bar = new WeakReference<>(view2);
            this.paint = paint;
        }

        @Override // android.view.View.OnAttachStateChangeListener
        public void onViewAttachedToWindow(View view) {
            ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
            this.observer = viewTreeObserver;
            if (viewTreeObserver.isAlive()) {
                this.observer.addOnPreDrawListener(this);
            }
            onPreDraw();
        }

        @Override // android.view.View.OnAttachStateChangeListener
        public void onViewDetachedFromWindow(View view) {
            ViewTreeObserver viewTreeObserver = this.observer;
            if (viewTreeObserver != null && viewTreeObserver.isAlive()) {
                this.observer.removeOnPreDrawListener(this);
            }
            this.observer = null;
        }

        @Override // android.view.ViewTreeObserver.OnPreDrawListener
        public boolean onPreDraw() {android.os.Trace.beginSection("ttcuz inbox filter");try{
            View view = this.bar.get();
            if (view != null && this.fade.getWidth() > 0) {
                int background = InboxFilter.background(view);
                int width = this.fade.getWidth();
                int layoutDirection = this.fade.getLayoutDirection();
                if (this.shader == null || this.paint.getShader() != this.shader || this.colour != background || this.width != width || this.direction != layoutDirection) {
                    this.colour = background;
                    this.width = width;
                    this.direction = layoutDirection;
                    LinearGradient linearGradient = new LinearGradient(layoutDirection == 1 ? 0.0f : width, 0.0f, layoutDirection == 1 ? width : 0.0f, 0.0f, new int[]{background, background, 16777215 & background}, new float[]{0.0f, 0.6f, 1.0f}, Shader.TileMode.CLAMP);
                    this.shader = linearGradient;
                    this.paint.setShader(linearGradient);
                }
            }
            return true;
        }finally{android.os.Trace.endSection();}}
    }

    static int background(View view) {
        for (int i = 0; view != null && i < 12; i++) {
            if (view.getBackground() instanceof ColorDrawable) {
                int color = ((ColorDrawable) view.getBackground()).getColor();
                if ((color >>> 24) == 255) {
                    return color;
                }
            }
            Object parent = view.getParent();
            view = parent instanceof View ? (View) parent : null;
        }
        return Themes.isDark() ? -16777216 : -1;
    }
}
