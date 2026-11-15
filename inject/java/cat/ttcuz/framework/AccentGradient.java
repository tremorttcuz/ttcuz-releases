package cat.ttcuz.framework;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextPaint;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

final class AccentGradient {
    private static final String ANGLE = "accent_gradient_angle";
    private static final String COLOUR = "accent_gradient_colour";
    private static final String MIX = "accent_gradient_mix";
    private static final String ON = "accent_gradient_on";
    private static final int TEXT_FILL = 1414809414;
    private static final String WIDTH = "accent_gradient_width";
    private static volatile State state;
    private static final Map<GradientDrawable, Integer> shapes = Collections.synchronizedMap(new WeakHashMap());
    private static final Map<Paint, Fill> paints = Collections.synchronizedMap(new WeakHashMap());

    AccentGradient() {
    }

    private static final class State {
        final int angle;
        final int colour;
        final int mix;
        final boolean on;
        final int width;
        final float x;
        final float y;

        State(SharedPreferences prefs) {
            boolean enabled = false;
            int second = 0xFF006A6A, blend = 100, turn = 45, span = 100;
            try { enabled = prefs.getBoolean(AccentGradient.ON, false); } catch (ClassCastException ignored) { }
            try { second = prefs.getInt(AccentGradient.COLOUR, second); blend = prefs.getInt(AccentGradient.MIX, 100); } catch (ClassCastException ignored) { }
            try { turn = prefs.getInt(AccentGradient.ANGLE, 45); span = prefs.getInt(AccentGradient.WIDTH, 100); } catch (ClassCastException ignored) { }
            this.on = enabled;
            this.colour = second | 0xFF000000;
            this.mix = Math.max(0, Math.min(100, blend));
            this.angle = Math.floorMod(turn, 360);
            this.width = Math.max(10, Math.min(200, span));
            this.x = (float) Math.cos(Math.toRadians(this.angle));
            this.y = (float) Math.sin(Math.toRadians(this.angle));
        }
    }

    private static State state() {
        State state2 = state;
        if (state2 != null || Ttcuz.context() == null) {
            return state2;
        }
        State state3 = new State(AccountAppearance.prefs(Ttcuz.context()));
        state = state3;
        return state3;
    }

    static boolean enabled() {
        State state2 = state();
        return state2 != null && state2.on;
    }

    static int colour() {
        State state2 = state();
        if (state2 == null) {
            return -16749974;
        }
        return state2.colour;
    }

    static int mix() {
        State state2 = state();
        if (state2 == null) {
            return 100;
        }
        return state2.mix;
    }

    static int angle() {
        State state2 = state();
        if (state2 == null) {
            return 45;
        }
        return state2.angle;
    }

    static int width() {
        State state2 = state();
        if (state2 == null) {
            return 100;
        }
        return state2.width;
    }

    static boolean matches(int i) {
        return (i >>> 24) != 0 && (i & 16777215) == (16777215 & Accent.colour());
    }

    static int end() {
        return Expressive.mix(Accent.colour(), colour(), mix() / 100.0f);
    }

    static int surface() {
        return enabled() ? Expressive.mix(Accent.colour(), end(), 0.5f) : Accent.colour();
    }

    static String cacheKey() {
        if (!enabled()) {
            return "";
        }
        return "|gradient2|" + end() + "|" + angle() + "|" + width();
    }

    static int pixel(int i, int i2, int i3, int i4, int i5, int i6) {
        if (!enabled() || i2 != Accent.colour()) {
            return Palette.map(i, -119723, i2);
        }
        State state2 = state();
        float f = i5;
        float f2 = i6;
        return Palette.map(i, -119723, Expressive.mix(i2, end(), Math.max(0.0f, Math.min(1.0f, (((((i3 + 0.5f) - (f / 2.0f)) * state2.x) + (((i4 + 0.5f) - (f2 / 2.0f)) * state2.y)) / (((Math.max(1.0f, Math.abs(state2.x * f) + Math.abs(state2.y * f2)) * state2.width) / 200.0f) * 2.0f)) + 0.5f))));
    }

    static void reload() {
        state = null;
    }

    static void set(boolean z, int i, int i2) {
        set(z, i, i2, angle(), width());
    }

    static void set(boolean z, int i, int i2, int i3, int i4) {
        if (Ttcuz.context() == null) {
            return;
        }
        AccountAppearance.prefs(Ttcuz.context()).edit().putBoolean(ON, z).putInt(COLOUR, i | (-16777216)).putInt(MIX, Math.max(0, Math.min(100, i2))).putInt(ANGLE, Math.floorMod(i3, 360)).putInt(WIDTH, Math.max(10, Math.min(200, i4))).apply();
        reload();
        Accent.forget();
        Themes.repaintVisible();
        Accent.refreshLikeViews();
    }

    static void shape(GradientDrawable gradientDrawable, int i) {
        if (!enabled() && shapes.isEmpty()) { gradientDrawable.setColor(i); return; }
        shapes.put(gradientDrawable, Integer.valueOf(i));
        if (enabled() && (i & 16777215) == (Accent.colour() & 16777215)) {
            gradientDrawable.setOrientation(new GradientDrawable.Orientation[]{GradientDrawable.Orientation.LEFT_RIGHT, GradientDrawable.Orientation.TL_BR, GradientDrawable.Orientation.TOP_BOTTOM, GradientDrawable.Orientation.TR_BL, GradientDrawable.Orientation.RIGHT_LEFT, GradientDrawable.Orientation.BR_TL, GradientDrawable.Orientation.BOTTOM_TOP, GradientDrawable.Orientation.BL_TR}[((angle() + 22) / 45) % 8]);
            gradientDrawable.setColors(new int[]{i, (16777215 & end()) | ((-16777216) & i)});
        } else {
            gradientDrawable.setColor(i);
        }
    }

    /**
     * The gradient across one box, for views the mod draws itself (switches, sliders). Remembers the last box and
     * colours, so a view that redraws every animation frame builds no new shader. Null when the gradient is off or
     * the colour is not the accent: draw it flat then.
     */
    static final class Across {
        private Shader shader;
        private float l, t, r, b;
        private int from, to, angle = -1, spread = -1;

        Shader get(float left, float top, float right, float bottom, int colour) {
            if (!enabled() || !matches(colour)) return null;
            int end = (end() & 0xFFFFFF) | (colour & 0xFF000000);
            if (shader == null || left != l || top != t || right != r || bottom != b || colour != from || end != to
                    || angle() != angle || width() != spread) {
                l = left; t = top; r = right; b = bottom; from = colour; to = end; angle = angle(); spread = width();
                float[] at = endpoints(right - left, bottom - top, angle, spread);
                shader = new LinearGradient(left + at[0], top + at[1], left + at[2], top + at[3], from, to, Shader.TileMode.CLAMP);
            }
            return shader;
        }
    }

    static final class Fill {
        private int first;
        private float height;
        private int last;
        private Shader shader;
        private float width;
        private int degrees = -1;
        private int spread = -1;

        Fill() {
        }

        void apply(Paint paint, float f, float f2, int i) {
            int mix;
            if (!AccentGradient.enabled()) {
                paint.setShader(null);
                return;
            }
            if ((i & 16777215) == (Accent.colour() & 16777215)) {
                mix = (16777215 & AccentGradient.end()) | ((-16777216) & i);
            } else {
                mix = Expressive.mix(i, AccentGradient.end(), 0.18f);
            }
            int angle = AccentGradient.angle();
            int width = AccentGradient.width();
            if (this.shader == null || this.first != i || this.last != mix || this.width != f || this.height != f2 || this.degrees != angle || this.spread != width) {
                this.first = i;
                this.last = mix;
                this.width = f;
                this.height = f2;
                this.degrees = angle;
                this.spread = width;
                float[] endpoints = AccentGradient.endpoints(f, f2, angle, width);
                this.shader = new LinearGradient(endpoints[0], endpoints[1], endpoints[2], endpoints[3], this.first, this.last, Shader.TileMode.CLAMP);
            }
            paint.setShader(this.shader);
        }
    }

    static float[] endpoints(float f, float f2, int i, int i2) {
        float max = Math.max(1.0f, f);
        float max2 = Math.max(1.0f, f2);
        double radians = Math.toRadians(i);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float max3 = (Math.max(1.0f, Math.abs(max * cos) + Math.abs(max2 * sin)) * i2) / 200.0f;
        float f3 = max / 2.0f;
        float f4 = cos * max3;
        float f5 = max2 / 2.0f;
        float f6 = sin * max3;
        return new float[]{f3 - f4, f5 - f6, f3 + f4, f5 + f6};
    }

    static Drawable wrap(Drawable drawable, int i) {
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof Tint) {
            Tint tint = (Tint) drawable;
            if (enabled() && matches(i)) {
                tint.colour = i;
                tint.invalidateSelf();
                return tint;
            }
            drawable = tint.source;
        }
        return (enabled() && matches(i)) ? new Tint(drawable, i) : drawable;
    }

    static Drawable base(Drawable drawable) {
        return drawable instanceof Tint ? ((Tint) drawable).source : drawable;
    }

    static Drawable drawable(Drawable drawable, GradientDrawable gradientDrawable) {
        Integer num = shapes.get(gradientDrawable);
        return num == null ? drawable(drawable) : wrap(drawable, num.intValue());
    }

    static Drawable drawable(Drawable drawable) {
        if (!enabled()) {
            return drawable;
        }
        if (drawable instanceof ColorDrawable) {
            return wrap(drawable, ((ColorDrawable) drawable).getColor());
        }
        if (!(drawable instanceof GradientDrawable) || Build.VERSION.SDK_INT < 24) {
            return drawable;
        }
        Integer num = shapes.get(drawable);
        if (num != null) {
            return wrap(drawable, num.intValue());
        }
        ColorStateList color = ((GradientDrawable) drawable).getColor();
        return (color == null || color.isStateful()) ? drawable : wrap(drawable, color.getDefaultColor());
    }

    static void image(ImageView imageView, int i) {
        if (imageView == null || (!enabled() && !(imageView.getDrawable() instanceof Tint))) return;
        if (Accent.nativeArtworkView(imageView)) {
            return;
        }
        Drawable drawable = imageView.getDrawable();
        Drawable wrap = wrap(drawable, i);
        if (drawable != wrap) {
            imageView.setImageDrawable(wrap);
        }
        if (wrap instanceof Tint) {
            Tint tint = (Tint) wrap;
            tint.source.setCallback(tint.callback);
            ColorStateList imageTintList = imageView.getImageTintList();
            if (imageTintList == null || !matches(imageTintList.getColorForState(imageView.getDrawableState(), imageTintList.getDefaultColor()))) {
                imageTintList = null;
            }
            tint.colours = imageTintList;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void text(TextView textView) {
        if (textView instanceof EditText) {
            return;
        }
        Object tag = textView.getTag(TEXT_FILL);
        Fill fill = tag instanceof Fill ? (Fill) tag : null;
        if (fill != null || (enabled() && matches(textView.getCurrentTextColor()))) {
            if (fill == null) {
                fill = new Fill();
                textView.setTag(TEXT_FILL, fill);
                textView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: cat.ttcuz.framework.AccentGradient$$ExternalSyntheticLambda1
                    @Override // android.view.View.OnLayoutChangeListener
                    public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                        AccentGradient.text((TextView) view);
                    }
                });
            }
            TextPaint paint = textView.getPaint();
            if (enabled() && matches(textView.getCurrentTextColor())) {
                fill.apply(paint, Math.max(1, (textView.getWidth() - textView.getPaddingLeft()) - textView.getPaddingRight()), Math.max(1.0f, textView.getTextSize()), textView.getCurrentTextColor());
            } else if (paint.getShader() == fill.shader) {
                paint.setShader(null);
            }
            textView.invalidate();
        }
    }

    static void paint(Paint paint, int i) {
        // Paint.setColor runs every frame: with the gradient off and nothing filled yet, cost nothing
        if (!enabled() && paints.isEmpty()) return;
        Map<Paint, Fill> map = paints;
        Fill fill = map.get(paint);
        if (enabled() && matches(i) && (paint.getShader() == null || (fill != null && paint.getShader() == fill.shader))) {
            if (fill == null) {
                fill = new Fill();
                map.put(paint, fill);
            }
            float f = (Ttcuz.context() == null ? 1.0f : Ttcuz.context().getResources().getDisplayMetrics().density) * 24.0f;
            fill.apply(paint, Math.max(f, paint.getTextSize()), Math.max(f, paint.getTextSize()), i);
            return;
        }
        if (fill == null || paint.getShader() != fill.shader) {
            return;
        }
        paint.setShader(null);
    }

    private static final class Tint extends Drawable implements Drawable.Callback {
        final Drawable.Callback callback;
        int colour;
        ColorStateList colours;
        final Fill fill;
        final Paint paint;
        final Drawable source;

        @Override // android.graphics.drawable.Drawable
        public int getOpacity() {
            return -3;
        }

        Tint(Drawable drawable, int i) {
            WeakCallback weakCallback = new WeakCallback(this);
            this.callback = weakCallback;
            Paint paint = new Paint(1);
            this.paint = paint;
            this.fill = new Fill();
            this.source = drawable;
            this.colour = i;
            setBounds(drawable.getBounds());
            setState(drawable.getState());
            setLevel(drawable.getLevel());
            drawable.setCallback(weakCallback);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        }

        @Override // android.graphics.drawable.Drawable
        public void draw(Canvas canvas) {
            Drawable.Callback callback = this.source.getCallback();
            Drawable.Callback callback2 = this.callback;
            if (callback != callback2) {
                this.source.setCallback(callback2);
            }
            Rect bounds = getBounds();
            ColorStateList colorStateList = this.colours;
            int colorForState = colorStateList == null ? this.colour : colorStateList.getColorForState(getState(), this.colour);
            if (!AccentGradient.enabled() || !AccentGradient.matches(colorForState)) {
                this.source.draw(canvas);
                return;
            }
            int saveLayer = canvas.saveLayer(bounds.left, bounds.top, bounds.right, bounds.bottom, null);
            this.source.draw(canvas);
            canvas.translate(bounds.left, bounds.top);
            this.fill.apply(this.paint, bounds.width(), bounds.height(), colorForState | (-16777216));
            canvas.drawRect(0.0f, 0.0f, bounds.width(), bounds.height(), this.paint);
            canvas.restoreToCount(saveLayer);
        }

        @Override // android.graphics.drawable.Drawable
        protected void onBoundsChange(Rect rect) {
            this.source.setBounds(rect);
        }

        @Override // android.graphics.drawable.Drawable
        public void setAlpha(int i) {
            this.source.setAlpha(i);
            invalidateSelf();
        }

        @Override // android.graphics.drawable.Drawable
        public int getAlpha() {
            return this.source.getAlpha();
        }

        @Override // android.graphics.drawable.Drawable
        public void setColorFilter(ColorFilter colorFilter) {
            this.source.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override // android.graphics.drawable.Drawable
        public int getIntrinsicWidth() {
            return this.source.getIntrinsicWidth();
        }

        @Override // android.graphics.drawable.Drawable
        public int getIntrinsicHeight() {
            return this.source.getIntrinsicHeight();
        }

        @Override // android.graphics.drawable.Drawable
        public int getMinimumWidth() {
            return this.source.getMinimumWidth();
        }

        @Override // android.graphics.drawable.Drawable
        public int getMinimumHeight() {
            return this.source.getMinimumHeight();
        }

        @Override // android.graphics.drawable.Drawable
        public boolean getPadding(Rect rect) {
            return this.source.getPadding(rect);
        }

        @Override // android.graphics.drawable.Drawable
        public boolean isStateful() {
            ColorStateList colorStateList;
            return this.source.isStateful() || ((colorStateList = this.colours) != null && colorStateList.isStateful());
        }

        @Override // android.graphics.drawable.Drawable
        protected boolean onStateChange(int[] iArr) {
            boolean state = this.source.setState(iArr);
            invalidateSelf();
            return state;
        }

        @Override // android.graphics.drawable.Drawable
        protected boolean onLevelChange(int i) {
            return this.source.setLevel(i);
        }

        @Override // android.graphics.drawable.Drawable
        public boolean setVisible(boolean z, boolean z2) {
            return this.source.setVisible(z, z2) | super.setVisible(z, z2);
        }

        @Override // android.graphics.drawable.Drawable
        public void jumpToCurrentState() {
            this.source.jumpToCurrentState();
        }

        @Override // android.graphics.drawable.Drawable
        public void getOutline(Outline outline) {
            this.source.getOutline(outline);
        }

        @Override // android.graphics.drawable.Drawable
        public void setHotspot(float f, float f2) {
            this.source.setHotspot(f, f2);
        }

        @Override // android.graphics.drawable.Drawable
        public void setHotspotBounds(int i, int i2, int i3, int i4) {
            this.source.setHotspotBounds(i, i2, i3, i4);
        }

        @Override // android.graphics.drawable.Drawable.Callback
        public void invalidateDrawable(Drawable drawable) {
            invalidateSelf();
        }

        @Override // android.graphics.drawable.Drawable.Callback
        public void scheduleDrawable(Drawable drawable, Runnable runnable, long j) {
            scheduleSelf(runnable, j);
        }

        @Override // android.graphics.drawable.Drawable.Callback
        public void unscheduleDrawable(Drawable drawable, Runnable runnable) {
            unscheduleSelf(runnable);
        }
    }

    private static final class WeakCallback implements Drawable.Callback {
        final WeakReference<Tint> target;

        WeakCallback(Tint tint) {
            this.target = new WeakReference<>(tint);
        }

        @Override // android.graphics.drawable.Drawable.Callback
        public void invalidateDrawable(Drawable drawable) {
            Tint tint = this.target.get();
            if (tint != null) {
                tint.invalidateSelf();
            }
        }

        @Override // android.graphics.drawable.Drawable.Callback
        public void scheduleDrawable(Drawable drawable, Runnable runnable, long j) {
            Tint tint = this.target.get();
            if (tint != null) {
                tint.scheduleSelf(runnable, j);
            }
        }

        @Override // android.graphics.drawable.Drawable.Callback
        public void unscheduleDrawable(Drawable drawable, Runnable runnable) {
            Tint tint = this.target.get();
            if (tint != null) {
                tint.unscheduleSelf(runnable);
            }
        }
    }
}
