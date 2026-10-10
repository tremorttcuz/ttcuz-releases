package cat.narezany.margyt;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;

/**
 * Material 3 Expressive, drawn here.
 *
 * Material's own library is not available to this build (adding it means adding
 * resources, and this build never touches the resource table), so the parts of
 * M3 Expressive that matter are drawn in code:
 *
 *  - the shape library: cookie, clover, sunny, flower, burst and the rest, as
 *    smooth scalloped outlines that can morph from a circle and rotate;
 *  - segmented lists: one tile per row, large outer corners, small inner ones,
 *    a thin gap between them, and corners that spring open while pressed;
 *  - leading icons set inside a shape that changes colour and turns when the
 *    thing it stands for is switched on;
 *  - a thick, rounded slider with a standing handle;
 *  - round tonal buttons that become rounded squares under the finger.
 *
 * Nothing here holds an activity, and nothing needs a resource.
 */
final class Expressive {

    private Expressive() {}

    // ------------------------------------------------------------- shapes

    static final int COOKIE9 = 0, CLOVER4 = 1, SUNNY = 2, FLOWER6 = 3,
            BURST12 = 4, PENTAGON = 5, ARCH3 = 6, COOKIE7 = 7;
    static final int KINDS = 8;

    /** Lobes of each shape. */
    private static final int[] LOBES = {9, 4, 8, 6, 12, 5, 3, 7};
    /** How deep the valleys go: 0 is a circle, 0.5 would touch the middle. */
    private static final float[] DEPTH = {0.050f, 0.160f, 0.110f, 0.140f, 0.040f, 0.075f, 0.100f, 0.065f};

    static int lobes(int kind) {
        return LOBES[wrap(kind)];
    }

    private static int wrap(int kind) {
        return ((kind % KINDS) + KINDS) % KINDS;
    }

    /**
     * An expressive outline inside a circle of radius `radius` at (cx, cy).
     *
     * `amount` runs from 0 (a plain circle) to 1 (the full shape), which is the
     * whole of a morph. `turn` rotates it in degrees; a turn of 360 / lobes is a
     * full lobe and looks like the shape came back to where it started.
     */
    static void shape(Path out, int kind, float cx, float cy, float radius, float amount, float turn) {
        int k = wrap(kind);
        int n = LOBES[k];
        float depth = DEPTH[k] * Math.max(0f, amount);
        int steps = Math.max(96, n * 16);
        double start = Math.toRadians(turn) - Math.PI / 2.0;
        out.reset();
        for (int i = 0; i < steps; i++) {
            double t = Math.PI * 2.0 * i / steps;
            double wave = Math.cos(n * (t - start));
            // tips reach the radius, valleys sit `depth * 2` below it
            double r = radius * (1.0 - depth * (1.0 - wave));
            float x = (float) (cx + r * Math.cos(t));
            float y = (float) (cy + r * Math.sin(t));
            if (i == 0) out.moveTo(x, y);
            else out.lineTo(x, y);
        }
        out.close();
    }

    /** Distance from the middle at one angle: what `shape` draws, as a number. */
    static float reach(int kind, float amount, float turn, double angle) {
        int k = wrap(kind);
        int n = LOBES[k];
        float depth = DEPTH[k] * Math.max(0f, amount);
        double start = Math.toRadians(turn) - Math.PI / 2.0;
        double wave = Math.cos(n * (angle - start));
        return (float) (1.0 - depth * (1.0 - wave));
    }

    static int mix(int from, int to, float amount) {
        float t = Math.max(0f, Math.min(1f, amount));
        return Color.argb(
                Math.round(Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * t),
                Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * t),
                Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * t),
                Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t));
    }

    private static float dp(Context context, float value) {
        return value * context.getResources().getDisplayMetrics().density;
    }

    // -------------------------------------------------------------- tiles

    /**
     * One rounded tile whose corners move.
     *
     * At rest the top and bottom pairs of corners are whatever the list put
     * there; while pressed every corner heads for `pressed`. A button that is
     * round does the opposite way round -- it is told a small `pressed` and
     * turns into a rounded square. The move is a short spring.
     */
    static final class Tile extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF box = new RectF();
        private final float[] radii = new float[8];
        private final boolean mask;
        private final boolean animate;
        private float top, bottom, pressedRadius;
        private float press;
        private boolean down;
        private ValueAnimator motion;

        Tile(int colour, float top, float bottom, float pressedRadius, boolean mask, boolean animate) {
            this.top = top;
            this.bottom = bottom;
            this.pressedRadius = pressedRadius;
            this.mask = mask;
            this.animate = animate;
            paint.setColor(colour);
        }

        @Override
        public boolean isStateful() {
            return !mask;
        }

        @Override
        protected boolean onStateChange(int[] state) {
            boolean now = false;
            for (int one : state) {
                if (one == android.R.attr.state_pressed) now = true;
            }
            if (now == down) return false;
            down = now;
            moveTo(now ? 1f : 0f);
            return true;
        }

        private void moveTo(float target) {
            if (motion != null) motion.cancel();
            if (!animate) {
                press = target;
                invalidateSelf();
                return;
            }
            motion = ValueAnimator.ofFloat(press, target);
            motion.setDuration(target > press ? 260L : 200L);
            motion.setInterpolator(target > press ? new OvershootInterpolator(1.8f) : new DecelerateInterpolator());
            motion.addUpdateListener(a -> {
                press = (Float) a.getAnimatedValue();
                invalidateSelf();
            });
            motion.start();
        }

        /** The corner radii as drawn right now, top pair then bottom pair. */
        float[] corners(float height) {
            float limit = height / 2f;
            float t = Math.min(top, limit), b = Math.min(bottom, limit);
            float p = Math.min(pressedRadius, limit);
            float amount = Math.max(0f, press);
            return new float[] {
                    clamp(t + (p - t) * amount, limit), clamp(b + (p - b) * amount, limit)};
        }

        private static float clamp(float value, float limit) {
            return Math.max(0f, Math.min(limit, value));
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.isEmpty()) return;
            box.set(bounds);
            float[] c = corners(bounds.height());
            radii[0] = radii[1] = radii[2] = radii[3] = c[0];
            radii[4] = radii[5] = radii[6] = radii[7] = c[1];
            path.reset();
            path.addRoundRect(box, radii, Path.Direction.CW);
            canvas.drawPath(path, paint);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /** A tile with a ripple clipped to its own shape. */
    static Drawable tile(Context context, int fill, int ripple, float top, float bottom, float pressed) {
        boolean animate = Motion.enabled(context);
        return new RippleDrawable(ColorStateList.valueOf(ripple),
                new Tile(fill, top, bottom, pressed, false, animate),
                new Tile(0xFFFFFFFF, top, bottom, pressed, true, false));
    }

    // -------------------------------------------------------------- group

    /**
     * A segmented list: every child becomes a tile of its own.
     *
     * The first tile has large top corners, the last large bottom ones, and
     * between them the corners are small, with a thin gap -- the grouped list
     * of M3 Expressive. A child that already paints its own background (a
     * preview, a banner) is left as it is.
     */
    static final class Group extends LinearLayout {
        private final int fill, ripple;
        private final float outer, inner, gap, pressed;

        Group(Context context, int fill, int ripple) {
            super(context);
            setOrientation(VERTICAL);
            this.fill = fill;
            this.ripple = ripple;
            this.outer = dp(context, 26);
            this.inner = dp(context, 6);
            this.gap = dp(context, 2);
            this.pressed = dp(context, 28);
        }

        @Override
        public void onViewAdded(View child) {
            super.onViewAdded(child);
            restyle();
        }

        @Override
        public void onViewRemoved(View child) {
            super.onViewRemoved(child);
            restyle();
        }

        private static boolean ours(View view) {
            Drawable background = view.getBackground();
            if (background == null) return true;
            if (background instanceof RippleDrawable) return true;
            return background instanceof android.graphics.drawable.ColorDrawable
                    && Color.alpha(((android.graphics.drawable.ColorDrawable) background).getColor()) == 0;
        }

        private void restyle() {
            int count = getChildCount(), first = -1, last = -1;
            for (int i = 0; i < count; i++) {
                if (getChildAt(i).getVisibility() == GONE) continue;
                if (first < 0) first = i;
                last = i;
            }
            for (int i = 0; i < count; i++) {
                View view = getChildAt(i);
                if (view.getVisibility() == GONE) continue;
                ViewGroup.LayoutParams given = view.getLayoutParams();
                LinearLayout.LayoutParams params = given instanceof LinearLayout.LayoutParams
                        ? (LinearLayout.LayoutParams) given
                        : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                int want = i == first ? 0 : Math.round(gap);
                if (params.topMargin != want || params != given) {
                    params.topMargin = want;
                    view.setLayoutParams(params);
                }
                if (!ours(view)) continue;
                view.setBackground(tile(getContext(), fill, ripple,
                        i == first ? outer : inner, i == last ? outer : inner, pressed));
            }
        }
    }

    // -------------------------------------------------------------- badge

    /**
     * A leading icon inside an expressive shape.
     *
     * Off, the shape is quiet; on, it fills with the accent and makes one full
     * turn, landing exactly where it started.
     */
    static final class Badge extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final int kind, size, glyphSize;
        private final Drawable glyph;
        private final int fillOff, fillOn, inkOff, inkOn;
        private float state;
        private ValueAnimator motion;

        Badge(Context context, int kind, Drawable glyph, int sizeDp,
              int fillOff, int fillOn, int inkOff, int inkOn) {
            super(context);
            this.kind = wrap(kind);
            this.glyph = glyph;
            this.size = Math.round(dp(context, sizeDp));
            this.glyphSize = Math.round(dp(context, sizeDp * 0.55f));
            this.fillOff = fillOff;
            this.fillOn = fillOn;
            this.inkOff = inkOff;
            this.inkOn = inkOn;
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        /** Whether this is lit; animated when asked and the phone allows motion. */
        void set(boolean on, boolean animated) {
            float target = on ? 1f : 0f;
            if (motion != null) motion.cancel();
            if (!animated || !Motion.enabled(getContext())) {
                state = target;
                invalidate();
                return;
            }
            motion = ValueAnimator.ofFloat(state, target);
            // One full, even revolution that settles exactly on the resting outline.
            // No overshoot: it used to swing past the end and come back, so the shape
            // never seemed to finish its turn.
            motion.setDuration(520L);
            motion.setInterpolator(new android.animation.TimeInterpolator() {
                @Override public float getInterpolation(float t) {
                    float u = 1f - t;
                    return 1f - u * u * u; // cubic ease-out: quick start, soft landing
                }
            });
            motion.addUpdateListener(a -> {
                state = (Float) a.getAnimatedValue();
                invalidate();
            });
            motion.start();
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            setMeasuredDimension(size, size);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            // Tips used to touch the edge of the view and were shaved flat by it. A margin
            // of 6% (never under 1px) keeps every lobe round, whatever the turn.
            float half = Math.min(getWidth(), getHeight()) / 2f;
            float radius = half - Math.max(1f, half * 0.06f);
            shape(path, kind, cx, cy, radius, 1f, state * 360f);
            paint.setColor(mix(fillOff, fillOn, state));
            canvas.drawPath(path, paint);
            if (glyph != null) {
                glyph.setBounds(Math.round(cx - glyphSize / 2f), Math.round(cy - glyphSize / 2f),
                        Math.round(cx + glyphSize / 2f), Math.round(cy + glyphSize / 2f));
                glyph.setColorFilter(mix(inkOff, inkOn, state), PorterDuff.Mode.SRC_IN);
                glyph.draw(canvas);
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            if (motion != null) {
                motion.cancel();
                motion = null;
            }
            super.onDetachedFromWindow();
        }
    }

    // ------------------------------------------------------------- button

    /** A round tonal button that becomes a rounded square while it is held. */
    static View roundButton(Context context, Drawable glyph, int fill, int ripple, int ink, int sizeDp) {
        final int size = Math.round(dp(context, sizeDp));
        final int pad = Math.round(dp(context, sizeDp * 0.27f));
        FrameLayout box = new FrameLayout(context);
        box.setBackground(tile(context, fill, ripple, size / 2f, size / 2f, dp(context, sizeDp * 0.30f)));
        android.widget.ImageView picture = new android.widget.ImageView(context);
        glyph.setColorFilter(ink, PorterDuff.Mode.SRC_IN);
        picture.setImageDrawable(glyph);
        box.addView(picture, new FrameLayout.LayoutParams(size - pad * 2, size - pad * 2, Gravity.CENTER));
        box.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        return box;
    }

    // ------------------------------------------------------------- slider

    /**
     * The M3 Expressive slider, drawn rather than assembled from drawables.
     *
     * Layout, left to right: the active track, a gap, the handle, a gap, the
     * inactive track and a stop dot. The outer ends of both tracks are fully
     * round, the ends beside the handle are nearly square, and everything is
     * centred on one line -- so nothing can drift apart, as the stacked
     * drawables did. The handle narrows while it is held.
     */
    static final class M3Slider extends SeekBar {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF box = new RectF();
        private final float[] radii = new float[8];
        private final float track, outer, inner, gap, handle, handleHeld, handleHeight, stop;
        private final int active, quiet;

        M3Slider(Context context, int active, int quiet) {
            super(context);
            this.active = active;
            this.quiet = quiet;
            track = dp(context, 16);
            outer = track / 2f;
            inner = dp(context, 2);
            gap = dp(context, 6);
            handle = dp(context, 4);
            handleHeld = dp(context, 2);
            handleHeight = dp(context, 40);
            stop = dp(context, 4);
            // The platform still does the touch and accessibility work. Its own thumb
            // is invisible but as wide as the handle, so the finger maps to the same
            // place the handle is drawn at; nothing is padded.
            GradientDrawable thumb = new GradientDrawable();
            thumb.setColor(0);
            thumb.setSize(Math.round(handle), Math.round(handleHeight));
            setThumb(thumb);
            setThumbOffset(0);
            setSplitTrack(false);
            setPadding(0, 0, 0, 0);
            setBackground(null);
        }

        /** Fraction 0..1 of the way along. */
        private float fraction() {
            int max = getMax();
            return max <= 0 ? 0f : Math.max(0f, Math.min(1f, getProgress() / (float) max));
        }

        private void bar(Canvas canvas, float left, float right, float cy, float leftRadius, float rightRadius, int colour) {
            float width = right - left;
            if (width < 1f) return;
            float limit = Math.min(width / 2f, track / 2f);
            float l = Math.min(leftRadius, limit), r = Math.min(rightRadius, limit);
            box.set(left, cy - track / 2f, right, cy + track / 2f);
            radii[0] = radii[1] = radii[6] = radii[7] = l;
            radii[2] = radii[3] = radii[4] = radii[5] = r;
            path.reset();
            path.addRoundRect(box, radii, Path.Direction.CW);
            paint.setColor(colour);
            canvas.drawPath(path, paint);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float width = getWidth(), cy = getHeight() / 2f;
            if (width <= handle) return;
            boolean held = isPressed();
            float hw = held ? handleHeld : handle;
            // the handle's centre runs from half a handle in to half a handle short of the end,
            // which is exactly where the platform puts its own thumb
            float cx = handle / 2f + fraction() * (width - handle);
            float activeEnd = cx - hw / 2f - gap;
            float quietStart = cx + hw / 2f + gap;
            int ink = isEnabled() ? active : quiet;

            bar(canvas, 0f, activeEnd, cy, outer, inner, ink);
            bar(canvas, quietStart, width, cy, inner, outer, quiet);

            // the stop dot sits inside the inactive track, a track-radius from the end
            float dotX = width - outer;
            if (dotX - stop / 2f > quietStart + stop) {
                paint.setColor(ink);
                canvas.drawCircle(dotX, cy, stop / 2f, paint);
            }

            box.set(cx - hw / 2f, cy - handleHeight / 2f, cx + hw / 2f, cy + handleHeight / 2f);
            float round = hw / 2f;
            radii[0] = radii[1] = radii[2] = radii[3] = radii[4] = radii[5] = radii[6] = radii[7] = round;
            path.reset();
            path.addRoundRect(box, radii, Path.Direction.CW);
            paint.setColor(ink);
            canvas.drawPath(path, paint);
        }
    }

    /** A ready slider: lay it out MATCH_PARENT wide and 44dp tall. */
    static M3Slider slider(Context context, int accent, int quiet) {
        return new M3Slider(context, accent, quiet);
    }

    // ------------------------------------------------------------- search

    /** A magnifying glass on the 24-unit grid the other glyphs use. */
    static final class Magnifier extends Drawable {
        private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);

        Magnifier(int colour) {
            ink.setStyle(Paint.Style.STROKE);
            ink.setStrokeCap(Paint.Cap.ROUND);
            ink.setColor(colour);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            int saved = canvas.save();
            canvas.translate(b.left, b.top);
            float unit = b.width() / 24f;
            canvas.scale(unit, unit);
            ink.setStrokeWidth(2.4f);
            canvas.drawCircle(10.5f, 10.5f, 6.5f, ink);
            canvas.drawLine(15.4f, 15.4f, 20.4f, 20.4f, ink);
            canvas.restoreToCount(saved);
        }

        @Override public void setAlpha(int alpha) { ink.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(ColorFilter filter) { ink.setColorFilter(filter); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
