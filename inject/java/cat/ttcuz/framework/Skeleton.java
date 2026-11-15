package cat.ttcuz.framework;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Placeholders in the shape of what is loading: rounded blocks in a faint tone of the text colour with a soft band
 * of light running across them. Every block reads the same clock, so the band crosses a whole list at once instead
 * of each block shimmering on its own. A block asks for frames only while it is drawn.
 */
final class Skeleton {
    private Skeleton() {}

    private static final long SWEEP = 1300L;

    /** One rounded block, `radius` px at the corners (-1: a circle). Use it as a view's background. */
    static Drawable block(Skin skin, float radius) { return new Block(skin.text, radius); }

    /** Plugin cards: an icon, a name, a line under it and a short description. */
    static View storeCards(Context c, Skin skin, int count) {
        LinearLayout column = new LinearLayout(c);
        column.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < count; i++) {
            LinearLayout card = new LinearLayout(c);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(c, 14), dp(c, 14), dp(c, 14), dp(c, 14));
            android.graphics.drawable.GradientDrawable back = new android.graphics.drawable.GradientDrawable();
            back.setCornerRadius(dp(c, 20));
            back.setColor((skin.text & 0xFFFFFF) | 0x0D000000);
            card.setBackground(back);
            LinearLayout top = new LinearLayout(c);
            top.setGravity(Gravity.CENTER_VERTICAL);
            top.addView(bone(c, skin, dp(c, 14)), new LinearLayout.LayoutParams(dp(c, 46), dp(c, 46)));
            LinearLayout words = new LinearLayout(c);
            words.setOrientation(LinearLayout.VERTICAL);
            words.setPadding(dp(c, 12), 0, 0, 0);
            words.addView(bone(c, skin, dp(c, 6)), sized(c, 14, 0, .62f));
            words.addView(bone(c, skin, dp(c, 6)), sized(c, 11, 8, .38f));
            top.addView(words, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            card.addView(top);
            card.addView(bone(c, skin, dp(c, 6)), sized(c, 11, 14, 1f));
            card.addView(bone(c, skin, dp(c, 6)), sized(c, 11, 8, .7f));
            LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            gap.bottomMargin = dp(c, 10);
            column.addView(card, gap);
        }
        return column;
    }

    /** People: a round picture and a name, `count` rows. */
    static View people(Context c, Skin skin, int count) {
        LinearLayout column = new LinearLayout(c);
        column.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < count; i++) {
            LinearLayout row = new LinearLayout(c);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int pad = dp(c, 10);
            row.setPadding(pad, pad, pad, pad);
            row.addView(bone(c, skin, -1), new LinearLayout.LayoutParams(dp(c, 36), dp(c, 36)));
            LinearLayout words = new LinearLayout(c);
            words.setOrientation(LinearLayout.VERTICAL);
            words.setPadding(pad, 0, 0, 0);
            // names of different lengths, so the rows do not look stamped out
            words.addView(bone(c, skin, dp(c, 6)), sized(c, 13, 0, .45f + .2f * ((i * 37) % 5) / 4f));
            row.addView(words, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            column.addView(row);
        }
        return column;
    }

    private static View bone(Context c, Skin skin, float radius) {
        View view = new View(c);
        view.setBackground(block(skin, radius));
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return view;
    }

    /** A block `heightDp` tall and about `share` of the row's width, `topDp` below the one above it. */
    private static LinearLayout.LayoutParams sized(Context c, int heightDp, int topDp, float share) {
        LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(c, heightDp));
        at.topMargin = dp(c, topDp);
        at.rightMargin = Math.round((1f - Math.max(0f, Math.min(1f, share))) * c.getResources().getDisplayMetrics().widthPixels * .55f);
        return at;
    }

    private static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    private static final class Block extends Drawable implements Runnable {
        private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG), light = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF box = new RectF();
        private final Matrix slide = new Matrix();
        private final float radius;
        private final int tone;
        private LinearGradient band;
        private int bandFor = -1;

        Block(int text, float radius) {
            this.radius = radius;
            this.tone = text & 0xFFFFFF;
            base.setColor(tone | 0x17000000);
        }

        @Override public void draw(Canvas canvas) {
            android.graphics.Rect b = getBounds();
            if (b.isEmpty()) return;
            box.set(b);
            float r = radius < 0 ? Math.min(b.width(), b.height()) / 2f : radius;
            canvas.drawRoundRect(box, r, r, base);
            // one band as wide as the screen sweeps from left to right; where it is is read off the clock
            int screen = Math.max(1, Math.round(b.width() * 3f));
            if (band == null || bandFor != screen) {
                bandFor = screen;
                band = new LinearGradient(0, 0, screen / 3f, 0, new int[]{tone, tone | 0x1A000000, tone},
                        new float[]{0f, .5f, 1f}, Shader.TileMode.CLAMP);
            }
            float t = (SystemClock.uptimeMillis() % SWEEP) / (float) SWEEP;
            slide.setTranslate(b.left - screen / 3f + t * (b.width() + screen / 3f), 0);
            band.setLocalMatrix(slide);
            light.setShader(band);
            canvas.drawRoundRect(box, r, r, light);
            scheduleSelf(this, SystemClock.uptimeMillis() + 16L);
        }

        @Override public void getOutline(android.graphics.Outline outline) {
            android.graphics.Rect b = getBounds();
            outline.setRoundRect(b, radius < 0 ? Math.min(b.width(), b.height()) / 2f : radius);
        }

        @Override public void run() { invalidateSelf(); }
        @Override public void setAlpha(int alpha) { base.setAlpha(alpha * 0x17 / 255); light.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
