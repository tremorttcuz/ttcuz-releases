package cat.ttcuz.island;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * The bottom bar as the mod sees it this frame, handed to an {@link Island}. Fields marked "in" are the mod's, filled
 * again before every frame. Fields marked "out" are the island's to set in {@link Island#layout(Bar)}: they start at
 * where the mod itself would put things.
 *
 * Coordinates are the bar's own: (0, 0) is its top left corner, width × height in pixels.
 */
public abstract class Bar {
    /** What each button opens: {@link #role}[i] is one of these. */
    public static final int HOME = 0, FRIENDS = 1, CREATE = 2, INBOX = 3, PROFILE = 4;

    /** in: the bar's size in pixels, and pixels per dp. */
    public int width, height;
    public float density;
    /** in: how many buttons there are (up to 5) and which one is the create button (-1: none). */
    public int count, create = -1;
    /** in: each button's role (HOME, FRIENDS, CREATE, INBOX, PROFILE), -1 past count. */
    public final int[] role = new int[5];
    /** in: the button TikTok shows as open, the one before it (-1: none), and how far the change is (0 → 1). */
    public int selected = -1, previous = -1;
    public float progress = 1f;
    /** in: each button's name as TikTok writes it, in the phone's language (null for the create button). */
    public final String[] label = new String[5];
    /** in: Google's Material Symbol for each button, outlined and filled, in a 24 × 24 box (null for the create button). */
    public final Path[] icon = new Path[5], iconFilled = new Path[5];
    /** in: the plus of the create button, 24 × 24. */
    public Path plus;
    /** in: the button is under the finger right now. */
    public final boolean[] pressed = new boolean[5];
    /** in: TikTok draws a picture on this button itself (a friend's avatar on Friends): leave the icon out. */
    public final boolean[] picture = new boolean[5];
    /** in: the accent and the theme's colours (ARGB), and whether the theme is dark. */
    public int accent, text, muted, card;
    public boolean dark;
    /** in: the glass chosen in the settings: 0 frosted, 1 liquid, 2 plastic. */
    public int glass;
    /** in: the time of this frame, in milliseconds (SystemClock.uptimeMillis). */
    public long time;

    /** out: the island's shape, and its corner radius in pixels. */
    public final RectF body = new RectF();
    public float radius;
    /** out: the centre of each button's icon (unread counts follow it). */
    public final float[] x = new float[5], y = new float[5];
    /** out: each button's tap area. A tap on the bar outside every area does nothing. */
    public final RectF[] hit = {new RectF(), new RectF(), new RectF(), new RectF(), new RectF()};

    /** Draw again on the next frame, and every frame after, while on (for animations). */
    public abstract void animate(boolean on);

    /** Draws the blurred video behind the bar, cut to rect and radius. Nothing where blur is off. */
    public abstract void drawBackdrop(Canvas canvas, RectF rect, float radius);

    /** An AGSL shader to paint with (Paint.setShader), made once per source. android.graphics.RuntimeShader, or null before Android 13. */
    public abstract Object shader(String agsl);

    /** Sets a float uniform of a shader from {@link #shader} or {@link #effect}. */
    public abstract void uniform(Object shader, String name, float... values);

    /** An AGSL shader over all the bar shows, backdrop included; it reads the bar as "uniform shader content". null takes it off. */
    public abstract Object effect(String agsl);

    /** Opens button `tab` as a tap on it would (TikTok does its own routing). */
    public abstract void open(int tab);

    /** A short haptic tick. */
    public abstract void tick();

    /** The MD3 "Sunny" shape (the mod's create button), centred on (0, 0), `size` pixels across. */
    public abstract Path sunny(float size);

    // ------------------------------------------------------------ help for drawing

    /** dp → pixels. */
    public float dp(float value) { return value * density; }

    /** Draws a 24 × 24 path (an icon, the plus) centred at (cx, cy), `size` pixels wide. */
    public void drawSymbol(Canvas canvas, Path path, float cx, float cy, float size, Paint paint) {
        if (path == null) return;
        int save = canvas.save();
        canvas.translate(cx - size / 2f, cy - size / 2f);
        canvas.scale(size / 24f, size / 24f);
        canvas.drawPath(path, paint);
        canvas.restoreToCount(save);
    }

    /** a → b by amount (0..1), alpha included. */
    public static int mix(int a, int b, float amount) {
        float k = Math.max(0f, Math.min(1f, amount));
        return Color.argb(Math.round(Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * k),
                Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * k),
                Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * k),
                Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * k));
    }

    /** The colour with its alpha multiplied by opacity (0..1). */
    public static int fade(int colour, float opacity) {
        return (colour & 0xFFFFFF) | (Math.round(Color.alpha(colour) * Math.max(0f, Math.min(1f, opacity))) << 24);
    }

    /** White or near black, whichever reads on this colour. */
    public static int on(int colour) {
        return (.299f * Color.red(colour) + .587f * Color.green(colour) + .114f * Color.blue(colour)) / 255f > .55f ? 0xFF1B1B1F : 0xFFFFFFFF;
    }
}
