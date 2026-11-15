package cat.ttcuz.framework;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;

/**
 * The head of the main settings page: a black card on a dark theme and a white one on a light
 * theme, the mod's opaque icon on the left, its name, version, and an "update" pill.
 *
 * Flat Material 3 shapes (cookie, clover, sunny, flower, burst, squircle, triangle, pill, circle)
 * glide over the card as if on ice: no friction, bouncing off the edges and now and then off each
 * other. Dark theme: white and grey shapes. Light theme: dark and grey shapes. No gradients, no glass.
 *
 * The whole card is a button (check for updates); a tap on the icon goes to {@link #setIconTap}.
 */
final class ModHero extends View {
    /** The design is drawn on a 1080 x 520 board and scaled to the width it gets. */
    private static final float W = 1080f, H = 520f;

    // kind, x0, y0, diameter, vx, vy (board units per second), spin (degrees per second), tone 0..3
    private static final float[][] SPRITES = {
            {0, 900,  90, 200,  -34,  21,   9, 0},
            {1, 200, 440, 140,   29, -26,  -8, 2},
            {3, 1000, 430, 180,  -27, -19,  -7, 3},
            {4,  90,  90, 100,   33,  24,  10, 2},
            {6, 760, 300,  95,  -31,  17, -11, 0},
            {7, 330,  40, 170,   36,  14,   6, 3},
    };
    /** How much of a shape's half-size counts as solid when two meet (the pill is long and thin). */
    private static final float[] SOLID = {0.92f, 0.88f, 0.9f, 0.88f, 0.92f, 0.95f, 0.85f, 0.62f, 1f};
    /**
     * Nothing here ever changes speed in one step. Edges and other shapes push back like a soft
     * spring, so a shape slows, turns and eases away over about half a second; speed is steered
     * back to each shape's own cruising speed, and the spin eases back to its own pace.
     */
    private static final float SPRING = 12f, WALL = 12f, STEER = 0.35f, EASE_SPIN = 0.8f, TICK = 1f / 120f;
    private static final float SLOW = 0.65f;

    // flat tones, no gradients: dark theme = white and grey, light theme = dark and grey
    private static final int[] TONES_DARK = {0x2EFFFFFF, 0xFF1F1F21, 0xFF2D2D30, 0xFF424246};
    private static final int[] TONES_LIGHT = {0x2E000000, 0xFFEFEFF1, 0xFFE2E2E5, 0xFFCFCFD3};

    // the icon's glyph, in the coordinates of the 1932 px source artwork
    private static final float ART_CX = 961f, ART_CY = 937f, ART_R = 761f, GLYPH = 178f;

    private final boolean dark;
    private final int accent;
    private final int n = SPRITES.length;
    private final float[] px = new float[n], py = new float[n], vx = new float[n], vy = new float[n];
    private final float[] turn = new float[n], spin = new float[n], rad = new float[n], mass = new float[n];
    private final float[] goal = new float[n], baseSpin = new float[n], ax = new float[n], ay = new float[n];
    private float clock, owed;
    private long last;
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path[] shapes = new Path[9];
    private final Path badge, bar = new Path(), left = new Path(), right = new Path();
    private final Path clip = new Path();
    private final RectF box = new RectF(), pill = new RectF(), iconHit = new RectF();
    private final float slop;
    private ValueAnimator drift;
    private float downX, downY;
    private Runnable iconTap;

    ModHero(Context context, boolean dark, int accent) {
        super(context);
        this.dark = dark;
        this.accent = accent;
        this.slop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClickable(true);
        setFocusable(true);
        setContentDescription("ttcuz " + Version.MOD + ". " + Text.UPDATE_CHECK);

        shapes[0] = polar(9, 0.060f);   // cookie
        shapes[1] = polar(4, 0.230f);   // clover
        shapes[2] = polar(8, 0.120f);   // sunny
        shapes[3] = polar(6, 0.180f);   // flower
        shapes[4] = polar(12, 0.085f);  // burst
        shapes[5] = squircle();
        shapes[6] = triangle();
        shapes[7] = new Path();
        shapes[7].addRoundRect(new RectF(-1f, -0.42f, 1f, 0.42f), 0.42f, 0.42f, Path.Direction.CW);
        shapes[8] = new Path();
        shapes[8].addCircle(0f, 0f, 1f, Path.Direction.CW);
        badge = polar(8, 0.060f);
        for (int i = 0; i < n; i++) {
            float[] sp = SPRITES[i];
            px[i] = sp[1];
            py[i] = sp[2];
            vx[i] = sp[4] * SLOW;
            vy[i] = sp[5] * SLOW;
            goal[i] = (float) Math.hypot(vx[i], vy[i]);
            spin[i] = baseSpin[i] = sp[6] * SLOW;
            turn[i] = i * 37f;
            rad[i] = sp[3] / 2f * SOLID[(int) sp[0]];
            mass[i] = rad[i] * rad[i];
        }
        settle();

        bar.moveTo(690, 773);
        bar.lineTo(1280, 773);
        left.moveTo(815, 645);
        left.cubicTo(885, 725, 915, 830, 908, 905);
        left.cubicTo(900, 1060, 800, 1170, 675, 1215);
        right.moveTo(1194, 773);
        right.cubicTo(1198, 960, 1150, 1110, 1080, 1245);
    }

    void setIconTap(Runnable tap) {
        iconTap = tap;
    }

    // ------------------------------------------------------------------ shapes

    private static Path polar(int lobes, float amp) {
        Path p = new Path();
        int n = 144;
        float max = 1f + amp;
        for (int i = 0; i <= n; i++) {
            double a = 2 * Math.PI * i / n;
            float r = (1f + amp * (float) Math.cos(lobes * a)) / max;
            float x = (float) (r * Math.sin(a)), y = (float) (-r * Math.cos(a));
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.close();
        return p;
    }

    private static Path squircle() {
        Path p = new Path();
        int n = 144;
        for (int i = 0; i <= n; i++) {
            double a = 2 * Math.PI * i / n;
            double c = Math.cos(a), s = Math.sin(a);
            float x = (float) (Math.signum(c) * Math.pow(Math.abs(c), 0.5));
            float y = (float) (Math.signum(s) * Math.pow(Math.abs(s), 0.5));
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.close();
        return p;
    }

    /** A triangle with its corners rounded off: a regular polygon's radius blended with a circle. */
    private static Path triangle() {
        Path p = new Path();
        int n = 144;
        double sector = 2 * Math.PI / 3, half = Math.PI / 3;
        double max = 0;
        double[] radius = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            double a = 2 * Math.PI * i / n;
            double within = ((a % sector) + sector) % sector;
            double poly = Math.cos(half) / Math.cos(within - half);
            radius[i] = 0.62 * poly + 0.38;
            if (radius[i] > max) max = radius[i];
        }
        for (int i = 0; i <= n; i++) {
            double a = 2 * Math.PI * i / n;
            float x = (float) (radius[i] / max * Math.sin(a)), y = (float) (-radius[i] / max * Math.cos(a));
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.close();
        return p;
    }

    // --------------------------------------------------------------- lifecycle

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        if (width <= 0) width = Math.round(340 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(width, Math.round(width * H / W));
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float s = w / W;
        iconHit.set(80 * s, 110 * s, 380 * s, 410 * s);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (drift != null) drift.cancel();
        boolean animate = true;
        if (Build.VERSION.SDK_INT >= 26) animate = ValueAnimator.areAnimatorsEnabled();
        if (!animate) return;
        last = 0;
        drift = ValueAnimator.ofFloat(0f, 1f);
        drift.setDuration(1000L);
        drift.setInterpolator(new LinearInterpolator());
        drift.setRepeatCount(ValueAnimator.INFINITE);
        drift.addUpdateListener(a -> {
            long now = System.nanoTime();
            if (last != 0) step(Math.min(0.05f, (now - last) / 1e9f));
            last = now;
            invalidate();
        });
        drift.start();
    }

    @Override protected void onDetachedFromWindow() {
        if (drift != null) { drift.cancel(); drift = null; }
        super.onDetachedFromWindow();
    }

    // ----------------------------------------------------------------- the ice

    /** Push apart anything that starts out overlapping, so the first frame is already legal. */
    private void settle() {
        for (int pass = 0; pass < 12; pass++) {
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    float dx = px[j] - px[i], dy = py[j] - py[i], reach = rad[i] + rad[j];
                    float d = (float) Math.sqrt(dx * dx + dy * dy);
                    if (d >= reach) continue;
                    float nx = d < 0.0001f ? 1f : dx / d, ny = d < 0.0001f ? 0f : dy / d;
                    float over = reach - d, share = mass[i] + mass[j];
                    px[i] -= nx * over * mass[j] / share;
                    py[i] -= ny * over * mass[j] / share;
                    px[j] += nx * over * mass[i] / share;
                    py[j] += ny * over * mass[i] / share;
                }
            }
        }
    }

    /** Advance by dt seconds in fixed small ticks, so a slow frame never makes a jump. */
    private void step(float dt) {
        owed += dt;
        while (owed >= TICK) {
            owed -= TICK;
            tick(TICK);
        }
    }

    private void tick(float h) {
        clock += h;
        for (int i = 0; i < n; i++) {
            float r = rad[i], fx = 0f, fy = 0f;
            // the edges: a soft cushion instead of a wall
            if (px[i] < r) fx += WALL * (r - px[i]);
            if (px[i] > W - r) fx -= WALL * (px[i] - (W - r));
            if (py[i] < r) fy += WALL * (r - py[i]);
            if (py[i] > H - r) fy -= WALL * (py[i] - (H - r));
            // glide back to the shape's own cruising speed, in the direction it already goes
            float v = (float) Math.hypot(vx[i], vy[i]);
            if (v < 0.001f) { vx[i] = goal[i]; v = goal[i]; }
            float steer = (goal[i] - v) * STEER;
            fx += vx[i] / v * steer;
            fy += vy[i] / v * steer;
            ax[i] = fx;
            ay[i] = fy;
        }
        // shapes that meet lean on each other through the same kind of spring
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                float dx = px[j] - px[i], dy = py[j] - py[i], reach = rad[i] + rad[j];
                float d2 = dx * dx + dy * dy;
                if (d2 >= reach * reach) continue;
                float d = (float) Math.sqrt(d2);
                float nx = d < 0.0001f ? 1f : dx / d, ny = d < 0.0001f ? 0f : dy / d;
                float push = SPRING * (reach - d), share = mass[i] + mass[j];
                ax[i] -= nx * push * 2f * mass[j] / share;
                ay[i] -= ny * push * 2f * mass[j] / share;
                ax[j] += nx * push * 2f * mass[i] / share;
                ay[j] += ny * push * 2f * mass[i] / share;
            }
        }
        for (int i = 0; i < n; i++) {
            vx[i] += ax[i] * h;
            vy[i] += ay[i] * h;
            px[i] += vx[i] * h;
            py[i] += vy[i] * h;
            spin[i] += (baseSpin[i] - spin[i]) * EASE_SPIN * h;
            turn[i] += spin[i] * h;
        }
    }

    @Override protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                setPressed(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(e.getX() - downX) > slop || Math.abs(e.getY() - downY) > slop) setPressed(false);
                return true;
            case MotionEvent.ACTION_UP: {
                boolean was = isPressed();
                setPressed(false);
                if (was) {
                    if (iconTap != null && iconHit.contains(e.getX(), e.getY())) iconTap.run();
                    else performClick();
                }
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    // ------------------------------------------------------------------- draw

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), s = w / W;
        if (w <= 0) return;
        ink.setStyle(Paint.Style.FILL);

        int ground = dark ? 0xFF0B0B0C : 0xFFFFFFFF;
        int words = dark ? 0xFFFFFFFF : 0xFF0B0B0C;

        box.set(0, 0, w, H * s);
        clip.reset();
        clip.addRoundRect(box, 56 * s, 56 * s, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        paint(ink, ground);
        canvas.drawRect(box, ink);

        // The shapes on the ice.
        int[] tones = dark ? TONES_DARK : TONES_LIGHT;
        for (int i = 0; i < n; i++) {
            float[] sp = SPRITES[i];
            paint(ink, tones[(int) sp[7]]);
            canvas.save();
            float lift = 5f * (float) Math.sin(clock * 0.8f + i * 1.7f);
            canvas.translate(px[i] * s, (py[i] + lift) * s);
            canvas.rotate(turn[i]);
            float k = sp[3] / 2f * s * (1f + 0.02f * (float) Math.sin(clock * 0.6f + i * 2.3f));
            canvas.scale(k, k);
            canvas.drawPath(shapes[(int) sp[0]], ink);
            canvas.restore();
        }
        canvas.restore();

        if (!dark) {
            ink.setStyle(Paint.Style.STROKE);
            ink.setStrokeWidth(Math.max(1f, 1.5f * s));
            paint(ink, 0x24000000);
            box.inset(ink.getStrokeWidth() / 2f, ink.getStrokeWidth() / 2f);
            canvas.drawRoundRect(box, 56 * s, 56 * s, ink);
            ink.setStyle(Paint.Style.FILL);
        }

        // The icon: opaque scalloped badge with the glyph on it.
        float cx = 230 * s, cy = 260 * s, r = 150 * s;
        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(r, r);
        paint(ink, dark ? 0xFFE9E9EB : 0xFF111113);
        canvas.drawPath(badge, ink);
        canvas.restore();
        canvas.save();
        canvas.translate(cx, cy);
        float g = r / ART_R;
        canvas.scale(g, g);
        canvas.translate(-ART_CX, -ART_CY);
        ink.setStyle(Paint.Style.STROKE);
        ink.setStrokeWidth(GLYPH);
        ink.setStrokeCap(Paint.Cap.ROUND);
        ink.setStrokeJoin(Paint.Join.ROUND);
        paint(ink, dark ? 0xFF121212 : 0xFFFFFFFF);
        canvas.drawPath(bar, ink);
        canvas.drawPath(left, ink);
        canvas.drawPath(right, ink);
        canvas.restore();
        ink.setStyle(Paint.Style.FILL);
        ink.setStrokeWidth(0f);

        // Name and version.
        ink.setTextAlign(Paint.Align.LEFT);
        ink.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        paint(ink, words);
        ink.setTextSize(104 * s);
        canvas.drawText("ttcuz", 428 * s, 214 * s, ink);
        ink.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint(ink, (words & 0xFFFFFF) | 0xB0000000);
        ink.setTextSize(36 * s);
        canvas.drawText("v" + Version.MOD + "  \u00B7  TikTok " + Version.TIKTOK, 430 * s, 272 * s, ink);

        // The update pill.
        String label = Updater.newer() ? Text.UPDATE_THERE_IS + " " + Updater.latest() : Text.UPDATE_CHECK;
        float room = (W - 40 - 430) * s;
        ink.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        ink.setTextSize(34 * s);
        float need = ink.measureText(label), pad = 40 * s;
        if (need > room - 2 * pad) {
            ink.setTextSize(34 * s * (room - 2 * pad) / need);
            need = ink.measureText(label);
        }
        pill.set(430 * s, 326 * s, 430 * s + need + 2 * pad, 416 * s);
        int fill = dark ? 0xFFFFFFFF : 0xFF0B0B0C;
        if (isPressed()) fill = (fill & 0xFFFFFF) | 0xCC000000;
        paint(ink, fill);
        canvas.drawRoundRect(pill, 45 * s, 45 * s, ink);
        paint(ink, dark ? 0xFF0B0B0C : 0xFFFFFFFF);
        ink.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics m = ink.getFontMetrics();
        canvas.drawText(label, pill.centerX(), pill.centerY() - (m.ascent + m.descent) / 2f, ink);
        ink.setTextAlign(Paint.Align.LEFT);
    }

    /**
     * A packed colour onto the brush; Paint.setColor is rewritten to the accent inside the mod,
     * so the value is taken apart here (see ChannelBanner).
     */
    private static void paint(Paint brush, int colour) {
        brush.setARGB((colour >>> 24) & 0xFF, (colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF);
    }

    private static int argb(int alpha, int rgb) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }
}
