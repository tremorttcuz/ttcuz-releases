package cat.narezany.margyt;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/**
 * The channel card in "About": a flat Telegram-blue card with a big paper plane flying out of
 * its lower left corner, the channel name, a neon "news / updates" line and a white
 * "Subscribe" pill. The whole card is the button.
 */
final class ChannelBanner extends View {
    static final String URL = "https://t.me/ttcuz" + "mod";
    /** The design is drawn on a 2172 x 724 board (the size of the banner artwork) and scaled to the width it gets. */
    private static final float W = 2172f, H = 724f;
    private static final int BLUE = 0xFF2AB2FC, FOLD = 0xFFC2DAF0, PALE = 0xFFF1F7FD;
    private static final float[] BODY = {793, 146, 0, 412, 0, 545, 70, 571, 97, 740, 494, 740};
    private static final float[] SHADE = {665, 232, 70, 571, 97, 740, 150, 740, 195, 615};
    private static final float[] TIP = {195, 615, 270, 684, 225, 740, 150, 740};
    private static final float[] GAP = {270, 684, 315, 740, 225, 740};

    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape = new Path(), clip = new Path();
    private final RectF box = new RectF(), pill = new RectF();
    private ValueAnimator pulse;
    private float phase;

    ChannelBanner(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);
        setContentDescription(Text.CHANNEL_TITLE + " ttcuz. " + Text.CHANNEL_NEWS_A + Text.CHANNEL_NEWS_B + ". " + Text.CHANNEL_SUBSCRIBE);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        if (width <= 0) width = Math.round(340 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(width, Math.round(width * H / W));
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (pulse != null) pulse.cancel();
        pulse = ValueAnimator.ofFloat(0f, 1f);
        pulse.setDuration(2200L);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.addUpdateListener(a -> { phase = (Float) a.getAnimatedValue(); invalidate(); });
        pulse.start();
    }

    @Override protected void onDetachedFromWindow() {
        if (pulse != null) { pulse.cancel(); pulse = null; }
        super.onDetachedFromWindow();
    }

    @Override protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), s = w / W;
        if (w <= 0) return;
        ink.setShader(null);
        ink.clearShadowLayer();
        ink.setStyle(Paint.Style.FILL);

        box.set(0, 0, w, H * s);
        clip.reset();
        clip.addRoundRect(box, 96 * s, 96 * s, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        paint(ink, BLUE);
        canvas.drawRect(box, ink);

        // The plane, bleeding off the left and bottom edges of the card.
        polygon(canvas, BODY, s, 0xFFFFFFFF);
        polygon(canvas, SHADE, s, FOLD);
        polygon(canvas, TIP, s, PALE);
        polygon(canvas, GAP, s, BLUE);
        canvas.restore();

        // Title.
        ink.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        ink.setTextAlign(Paint.Align.LEFT);
        paint(ink, 0xCCFFFFFF);
        ink.setTextSize(70 * s);
        canvas.drawText(Text.CHANNEL_TITLE, 930 * s, 190 * s, ink);
        ink.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        paint(ink, 0xFFFFFFFF);
        ink.setTextSize(150 * s);
        canvas.drawText("ttcuz", 922 * s, 340 * s, ink);
        ink.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

        // Neon line: a wide soft halo, a tight one, then the bright core on top.
        ink.setTextSize(84 * s);
        float breath = 0.55f + 0.45f * phase;
        int halo = 0xFF7DF9FF;
        paint(ink, argb(Math.round(0xFF * breath), 0x7DF9FF));
        ink.setShadowLayer((22 + 10 * phase) * s, 0, 0, halo);
        canvas.drawText(Text.CHANNEL_NEWS_A, 930 * s, 470 * s, ink);
        canvas.drawText(Text.CHANNEL_NEWS_B, 930 * s, 570 * s, ink);
        ink.setShadowLayer((8 + 4 * phase) * s, 0, 0, halo);
        paint(ink, 0xFFE9FEFF);
        canvas.drawText(Text.CHANNEL_NEWS_A, 930 * s, 470 * s, ink);
        canvas.drawText(Text.CHANNEL_NEWS_B, 930 * s, 570 * s, ink);
        ink.clearShadowLayer();

        // "Subscribe" pill.
        pill.set(1500 * s, 110 * s, 2072 * s, 250 * s);
        paint(ink, isPressed() ? 0xE6FFFCF7 : 0xFFFFFCF7);
        canvas.drawRoundRect(pill, 70 * s, 70 * s, ink);
        paint(ink, 0xFF2C93CE);
        ink.setTextAlign(Paint.Align.CENTER);
        float size = 56 * s;
        ink.setTextSize(size);
        float room = pill.width() * 0.86f, need = ink.measureText(Text.CHANNEL_SUBSCRIBE);
        if (need > room) ink.setTextSize(size * room / need);
        Paint.FontMetrics m = ink.getFontMetrics();
        canvas.drawText(Text.CHANNEL_SUBSCRIBE, pill.centerX(), pill.centerY() - (m.ascent + m.descent) / 2f, ink);
        ink.setTextAlign(Paint.Align.LEFT);
    }

    private void polygon(Canvas canvas, float[] xy, float s, int colour) {
        shape.reset();
        shape.moveTo(xy[0] * s, xy[1] * s);
        for (int i = 2; i < xy.length; i += 2) shape.lineTo(xy[i] * s, xy[i + 1] * s);
        shape.close();
        paint(ink, colour);
        canvas.drawPath(shape, ink);
    }

    /**
     * A packed colour onto the brush.
     *
     * Paint.setARGB wants the four components apart, and Paint.setColor is not
     * usable from inside the mod: every call to it is rewritten to the accent,
     * which is the point of that rewrite and not what a banner drawing its own
     * colours wants. So the packed value is taken apart here.
     */
    private static void paint(Paint brush, int colour) {
        brush.setARGB((colour >>> 24) & 0xFF, (colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF);
    }

    private static int argb(int alpha, int rgb) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }
}
