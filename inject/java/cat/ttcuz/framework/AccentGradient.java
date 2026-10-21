package cat.ttcuz.framework;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.widget.ImageView;
import android.widget.TextView;

/** Static two-colour accents. Each owner keeps its shader until bounds or colours change. */
final class AccentGradient {

    private static final String ON = "accent_gradient_on",
        COLOUR = "accent_gradient_colour",
        MIX = "accent_gradient_mix",
        ANGLE = "accent_gradient_angle",
        WIDTH = "accent_gradient_width";
    private static volatile State state;
    private static final java.util.Map<GradientDrawable, Integer> shapes =
        java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private static final class State {

        final boolean on;
        final int colour, mix, angle, width;
        final float x, y;

        State(SharedPreferences p) {
            boolean enabled = false;
            int second = 0xFF006A6A,
                amount = 100,
                degrees = 45,
                spread = 100;
            try {
                enabled = p.getBoolean(ON, false);
                second = p.getInt(COLOUR, second);
                amount = p.getInt(MIX, amount);
                degrees = p.getInt(ANGLE, degrees);
                spread = p.getInt(WIDTH, spread);
            } catch (ClassCastException ignored) {}
            on = enabled;
            colour = second | 0xFF000000;
            mix = Math.max(0, Math.min(100, amount));
            angle = Math.floorMod(degrees, 360);
            width = Math.max(10, Math.min(200, spread));
            x = (float) Math.cos(Math.toRadians(angle));
            y = (float) Math.sin(Math.toRadians(angle));
        }
    }

    private static State state() {
        State known = state;
        if (known == null && Ttcuz.context() != null) {
            known = new State(AccountAppearance.prefs(Ttcuz.context()));
            state = known;
        }
        return known;
    }

    static boolean enabled() {
        State s = state();
        return s != null && s.on;
    }

    static int colour() {
        State s = state();
        return s == null ? 0xFF006A6A : s.colour;
    }

    static int mix() {
        State s = state();
        return s == null ? 100 : s.mix;
    }

    static int angle() {
        State s = state();
        return s == null ? 45 : s.angle;
    }

    static int width() {
        State s = state();
        return s == null ? 100 : s.width;
    }

    static boolean matches(int colour) {
        return colour >>> 24 != 0 && (colour & 0xFFFFFF) == (Accent.colour() & 0xFFFFFF);
    }

    static int end() {
        return Expressive.mix(Accent.colour(), colour(), mix() / 100f);
    }

    static int surface() {
        return enabled() ? Expressive.mix(Accent.colour(), end(), .5f) : Accent.colour();
    }

    static String cacheKey() {
        return enabled() ? "|gradient2|" + end() + "|" + angle() + "|" + width() : "";
    }

    static int pixel(int source, int target, int x, int y, int w, int h) {
        if (!enabled() || target != Accent.colour()) return Palette.map(
            source,
            Accent.TIKTOK,
            target
        );
        State s = state();
        float half = (Math.max(1, Math.abs(w * s.x) + Math.abs(h * s.y)) * s.width) / 200f;
        float progress = ((x + .5f - w / 2f) * s.x + (y + .5f - h / 2f) * s.y) / (half * 2) + .5f;
        return Palette.map(
            source,
            Accent.TIKTOK,
            Expressive.mix(target, end(), Math.max(0, Math.min(1, progress)))
        );
    }

    static void reload() {
        state = null;
    }

    static void set(boolean on, int colour, int mix) {
        set(on, colour, mix, angle(), width());
    }

    static void set(boolean on, int colour, int mix, int angle, int width) {
        if (Ttcuz.context() == null) return;
        AccountAppearance.prefs(Ttcuz.context())
            .edit()
            .putBoolean(ON, on)
            .putInt(COLOUR, colour | 0xFF000000)
            .putInt(MIX, Math.max(0, Math.min(100, mix)))
            .putInt(ANGLE, Math.floorMod(angle, 360))
            .putInt(WIDTH, Math.max(10, Math.min(200, width)))
            .apply();
        reload();
        Accent.forget();
        Themes.repaintVisible();
        Accent.refreshLikeViews();
    }

    static void shape(GradientDrawable shape, int solid) {
        shapes.put(shape, solid);
        if (enabled() && (solid & 0xFFFFFF) == (Accent.colour() & 0xFFFFFF)) {
            int alpha = solid & 0xFF000000;
            GradientDrawable.Orientation[] directions = {
                GradientDrawable.Orientation.LEFT_RIGHT,
                GradientDrawable.Orientation.TL_BR,
                GradientDrawable.Orientation.TOP_BOTTOM,
                GradientDrawable.Orientation.TR_BL,
                GradientDrawable.Orientation.RIGHT_LEFT,
                GradientDrawable.Orientation.BR_TL,
                GradientDrawable.Orientation.BOTTOM_TOP,
                GradientDrawable.Orientation.BL_TR,
            };
            shape.setOrientation(directions[((angle() + 22) / 45) % 8]);
            shape.setColors(new int[] { solid, alpha | (end() & 0xFFFFFF) });
        } else shape.setColor(solid);
    }

    static final class Fill {

        private int first,
            last,
            degrees = -1,
            spread = -1;
        private float width, height;
        private Shader shader;

        void apply(Paint paint, float w, float h, int solid) {
            if (!enabled()) {
                paint.setShader(null);
                return;
            }
            int target =
                (solid & 0xFFFFFF) == (Accent.colour() & 0xFFFFFF)
                    ? (solid & 0xFF000000) | (end() & 0xFFFFFF)
                    : Expressive.mix(solid, end(), .18f);
            int angle = angle(),
                scale = AccentGradient.width();
            if (
                shader == null ||
                first != solid ||
                last != target ||
                width != w ||
                height != h ||
                degrees != angle ||
                spread != scale
            ) {
                first = solid;
                last = target;
                width = w;
                height = h;
                degrees = angle;
                spread = scale;
                float[] points = endpoints(w, h, angle, scale);
                shader = new LinearGradient(
                    points[0],
                    points[1],
                    points[2],
                    points[3],
                    first,
                    last,
                    Shader.TileMode.CLAMP
                );
            }
            paint.setShader(shader);
        }
    }

    /** Projection across the actual bounds keeps any angle correct on a narrow control. */
    static float[] endpoints(float w, float h, int degrees, int spread) {
        w = Math.max(1, w);
        h = Math.max(1, h);
        double radians = Math.toRadians(degrees);
        float x = (float) Math.cos(radians),
            y = (float) Math.sin(radians);
        float half = (Math.max(1f, Math.abs(w * x) + Math.abs(h * y)) * spread) / 200f;
        return new float[] {
            w / 2 - x * half,
            h / 2 - y * half,
            w / 2 + x * half,
            h / 2 + y * half,
        };
    }

    /** Keep the native outline, state and animation; colour only its alpha mask. */
    static Drawable wrap(Drawable source, int colour) {
        if (source == null) return null;
        if (source instanceof Tint) {
            Tint tint = (Tint) source;
            if (enabled() && matches(colour)) {
                tint.colour = colour;
                tint.invalidateSelf();
                return tint;
            }
            source = tint.source;
        }
        return enabled() && matches(colour) ? new Tint(source, colour) : source;
    }

    static Drawable base(Drawable drawable) {
        return drawable instanceof Tint ? ((Tint) drawable).source : drawable;
    }

    static Drawable drawable(Drawable drawable, GradientDrawable original) {
        Integer colour = shapes.get(original);
        return colour == null ? drawable(drawable) : wrap(drawable, colour);
    }

    static Drawable drawable(Drawable drawable) {
        if (!enabled()) return drawable;
        if (drawable instanceof android.graphics.drawable.ColorDrawable) return wrap(
            drawable,
            ((android.graphics.drawable.ColorDrawable) drawable).getColor()
        );
        if (drawable instanceof GradientDrawable && android.os.Build.VERSION.SDK_INT >= 24) {
            Integer colour = shapes.get(drawable);
            if (colour != null) return wrap(drawable, colour);
            android.content.res.ColorStateList fill = ((GradientDrawable) drawable).getColor();
            if (fill != null && !fill.isStateful()) return wrap(drawable, fill.getDefaultColor());
        }
        return drawable;
    }

    static void image(ImageView view, int colour) {
        if (view == null || Accent.nativeArtworkView(view)) return;
        Drawable old = view.getDrawable(),
            next = wrap(old, colour);
        if (old != next) view.setImageDrawable(next);
        if (next instanceof Tint) {
            Tint tint = (Tint) next;
            tint.source.setCallback(tint.callback);
            android.content.res.ColorStateList nativeTint = view.getImageTintList();
            // Explicit native colour filters can override an unrelated white tint list.
            // That stale list must not suppress the selected heart's gradient.
            tint.colours =
                nativeTint != null &&
                matches(
                    nativeTint.getColorForState(
                        view.getDrawableState(),
                        nativeTint.getDefaultColor()
                    )
                )
                    ? nativeTint
                    : null;
        }
    }

    private static final int TEXT_FILL = 0x54544746;

    static void text(TextView view) {
        if (view instanceof android.widget.EditText) return;
        Object known = view.getTag(TEXT_FILL);
        Fill fill = known instanceof Fill ? (Fill) known : null;
        if (fill == null && (!enabled() || !matches(view.getCurrentTextColor()))) return;
        if (fill == null) {
            fill = new Fill();
            view.setTag(TEXT_FILL, fill);
            view.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> text((TextView) v));
        }
        Paint paint = view.getPaint();
        if (enabled() && matches(view.getCurrentTextColor())) fill.apply(
            paint,
            Math.max(1, view.getWidth() - view.getPaddingLeft() - view.getPaddingRight()),
            Math.max(1, view.getTextSize()),
            view.getCurrentTextColor()
        );
        else if (paint.getShader() == fill.shader) paint.setShader(null);
        view.invalidate();
    }

    private static final java.util.Map<Paint, Fill> paints = java.util.Collections.synchronizedMap(
        new java.util.WeakHashMap<>()
    );

    static void paint(Paint paint, int colour) {
        Fill fill = paints.get(paint);
        if (
            enabled() &&
            matches(colour) &&
            (paint.getShader() == null || (fill != null && paint.getShader() == fill.shader))
        ) {
            if (fill == null) {
                fill = new Fill();
                paints.put(paint, fill);
            }
            float density =
                Ttcuz.context() == null
                    ? 1
                    : Ttcuz.context().getResources().getDisplayMetrics().density;
            fill.apply(
                paint,
                Math.max(24 * density, paint.getTextSize()),
                Math.max(24 * density, paint.getTextSize()),
                colour
            );
        } else if (fill != null && paint.getShader() == fill.shader) paint.setShader(null);
    }

    private static final class Tint extends Drawable implements Drawable.Callback {

        final Drawable source;
        final Drawable.Callback callback = new WeakCallback(this);
        android.content.res.ColorStateList colours;
        int colour;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Fill fill = new Fill();

        Tint(Drawable source, int colour) {
            this.source = source;
            this.colour = colour;
            setBounds(source.getBounds());
            setState(source.getState());
            setLevel(source.getLevel());
            source.setCallback(callback);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        }

        @Override
        public void draw(Canvas canvas) {
            if (source.getCallback() != callback) source.setCallback(callback);
            Rect b = getBounds();
            int current = colours == null ? colour : colours.getColorForState(getState(), colour);
            if (!enabled() || !matches(current)) {
                source.draw(canvas);
                return;
            }
            int layer = canvas.saveLayer(b.left, b.top, b.right, b.bottom, null);
            source.draw(canvas);
            canvas.translate(b.left, b.top);
            // The native drawable already carries its alpha (including disabled states).
            fill.apply(paint, b.width(), b.height(), current | 0xFF000000);
            canvas.drawRect(0, 0, b.width(), b.height(), paint);
            canvas.restoreToCount(layer);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            source.setBounds(bounds);
        }

        @Override
        public void setAlpha(int alpha) {
            source.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public int getAlpha() {
            return source.getAlpha();
        }

        @Override
        public void setColorFilter(ColorFilter filter) {
            source.setColorFilter(filter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }

        @Override
        public int getIntrinsicWidth() {
            return source.getIntrinsicWidth();
        }

        @Override
        public int getIntrinsicHeight() {
            return source.getIntrinsicHeight();
        }

        @Override
        public int getMinimumWidth() {
            return source.getMinimumWidth();
        }

        @Override
        public int getMinimumHeight() {
            return source.getMinimumHeight();
        }

        @Override
        public boolean getPadding(Rect padding) {
            return source.getPadding(padding);
        }

        @Override
        public boolean isStateful() {
            return source.isStateful() || (colours != null && colours.isStateful());
        }

        @Override
        protected boolean onStateChange(int[] state) {
            boolean changed = source.setState(state);
            invalidateSelf();
            return changed;
        }

        @Override
        protected boolean onLevelChange(int level) {
            return source.setLevel(level);
        }

        @Override
        public boolean setVisible(boolean visible, boolean restart) {
            return super.setVisible(visible, restart) | source.setVisible(visible, restart);
        }

        @Override
        public void jumpToCurrentState() {
            source.jumpToCurrentState();
        }

        @Override
        public void getOutline(android.graphics.Outline outline) {
            source.getOutline(outline);
        }

        @Override
        public void setHotspot(float x, float y) {
            source.setHotspot(x, y);
        }

        @Override
        public void setHotspotBounds(int l, int t, int r, int b) {
            source.setHotspotBounds(l, t, r, b);
        }

        @Override
        public void invalidateDrawable(Drawable who) {
            invalidateSelf();
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable task, long when) {
            scheduleSelf(task, when);
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable task) {
            unscheduleSelf(task);
        }
    }

    /** A cached original must not retain its replacement through Drawable.Callback. */
    private static final class WeakCallback implements Drawable.Callback {

        final java.lang.ref.WeakReference<Tint> target;

        WeakCallback(Tint tint) {
            target = new java.lang.ref.WeakReference<>(tint);
        }

        @Override
        public void invalidateDrawable(Drawable who) {
            Tint tint = target.get();
            if (tint != null) tint.invalidateSelf();
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable task, long when) {
            Tint tint = target.get();
            if (tint != null) tint.scheduleSelf(task, when);
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable task) {
            Tint tint = target.get();
            if (tint != null) tint.unscheduleSelf(task);
        }
    }
}
