package cat.ttcuz.framework;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;

/**
 * A line chart in the manner of Telegram's statistics: smooth lines, the first one filled with a fading gradient,
 * a few quiet grid lines with their values, dates along the bottom, and a finger on it shows that day -- a line down,
 * a dot on each series and a card with the date and the values. Changing the range (7, 30, 90 days) or the data
 * slides the scale to its new height instead of jumping; the lines draw in from the left when it first appears.
 */
final class TrendChart extends View {
    /** One line: its name, colour and a value per day (oldest first). */
    static final class Series {
        final String name;
        final int colour;
        final float[] values;
        Series(String name, int colour, float[] values) { this.name = name; this.colour = colour; this.values = values; }
    }

    private Series[] series = new Series[0];
    /** The days, as days since 1970 (UTC), one per value. */
    private long[] days = new long[0];
    private int range = 30;
    private final float density;
    private final int ink, muted, grid, card;
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG), fill = new Paint(Paint.ANTI_ALIAS_FLAG),
            text = new Paint(Paint.ANTI_ALIAS_FLAG), rule = new Paint(Paint.ANTI_ALIAS_FLAG), dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path(), area = new Path();
    private final RectF plot = new RectF(), tip = new RectF();
    /** The scale's top as shown now and as wanted; the share of the lines drawn in so far. */
    private float shownMax = 1f, wantedMax = 1f, reveal = 0f;
    private ValueAnimator scaleAnim, revealAnim;
    /** The day under the finger (index in the visible range), or -1. */
    private int touched = -1;
    private Shader gradient;
    private float gradientFor = -1f;

    TrendChart(Context context, int ink, int muted, int card) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        this.ink = ink;
        this.muted = muted;
        this.card = card;
        this.grid = (muted & 0xFFFFFF) | 0x26000000;
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(2.2f * density);
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setStrokeCap(Paint.Cap.ROUND);
        text.setTextSize(11 * density);
        text.setColor(muted);
        rule.setStrokeWidth(Math.max(1f, density * .7f));
        rule.setColor(grid);
        setMinimumHeight(Math.round(220 * density));
    }

    void set(long[] days, Series... series) {
        this.days = days;
        this.series = series;
        touched = -1;
        rescale(true);
        if (reveal < 1f) {
            revealAnim = ValueAnimator.ofFloat(0f, 1f);
            revealAnim.setDuration(700L);
            revealAnim.setInterpolator(Motion.EMPHASIZED);
            revealAnim.addUpdateListener(a -> { reveal = (float) a.getAnimatedValue(); invalidate(); });
            if (Motion.enabled(getContext())) revealAnim.start(); else reveal = 1f;
        }
        invalidate();
    }

    /** Shows the last `days` days; the scale slides to the new range's top. */
    void range(int days) {
        range = Math.max(2, days);
        touched = -1;
        rescale(false);
        invalidate();
    }

    private int first() { return Math.max(0, days.length - range); }

    private void rescale(boolean now) {
        float max = 0f;
        for (Series s : series) for (int i = first(); i < s.values.length; i++) max = Math.max(max, s.values[i]);
        float target = nice(max);
        wantedMax = target;
        if (now || !Motion.enabled(getContext())) { shownMax = target; return; }
        if (scaleAnim != null) scaleAnim.cancel();
        scaleAnim = ValueAnimator.ofFloat(shownMax, target);
        scaleAnim.setDuration(360L);
        scaleAnim.setInterpolator(Motion.BOTH);
        scaleAnim.addUpdateListener(a -> { shownMax = (float) a.getAnimatedValue(); invalidate(); });
        scaleAnim.start();
    }

    /** A round top for the scale: 1, 2, 5 times a power of ten, at least 4 so a quiet chart has whole steps. */
    static float nice(float max) {
        if (max <= 4f) return 4f;
        double step = Math.pow(10, Math.floor(Math.log10(max / 4)));
        for (double k : new double[]{1, 2, 2.5, 5, 10}) if (k * step * 4 >= max) return (float) (k * step * 4);
        return (float) (step * 40);
    }

    @Override protected void onDraw(Canvas canvas) {
        if (days.length == 0 || series.length == 0) return;
        float left = 34 * density, bottom = getHeight() - 24 * density;
        plot.set(left, 14 * density, getWidth() - 8 * density, bottom);
        int from = first(), count = days.length - from;
        // the grid: four steps with their values
        for (int g = 0; g <= 4; g++) {
            float y = plot.bottom - plot.height() * g / 4f;
            canvas.drawLine(plot.left, y, plot.right, y, rule);
            text.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(short_(shownMax * g / 4f), plot.left - 6 * density, y + 4 * density, text);
        }
        // dates: about five, evenly
        text.setTextAlign(Paint.Align.CENTER);
        int labels = Math.min(5, count);
        for (int k = 0; k < labels; k++) {
            int i = from + Math.round((count - 1) * (labels == 1 ? 0f : k / (float) (labels - 1)));
            canvas.drawText(date(days[i]), x(i - from, count), getHeight() - 6 * density, text);
        }
        int save = canvas.save();
        canvas.clipRect(0, 0, plot.left + plot.width() * reveal + line.getStrokeWidth(), getHeight());
        for (int s = series.length - 1; s >= 0; s--) {
            Series one = series[s];
            smooth(one.values, from, count);
            if (s == 0) {
                // the first line is filled down to the axis with its colour fading out
                if (gradientFor != plot.height() + one.colour) {
                    gradientFor = plot.height() + one.colour;
                    gradient = new LinearGradient(0, plot.top, 0, plot.bottom, (one.colour & 0xFFFFFF) | 0x55000000, one.colour & 0xFFFFFF, Shader.TileMode.CLAMP);
                }
                area.set(path);
                area.lineTo(x(count - 1, count), plot.bottom);
                area.lineTo(x(0, count), plot.bottom);
                area.close();
                fill.setShader(gradient);
                canvas.drawPath(area, fill);
            }
            line.setColor(one.colour);
            canvas.drawPath(path, line);
        }
        canvas.restoreToCount(save);
        if (touched >= 0 && touched < count) touchCard(canvas, from, count);
    }

    /** The values from..from+count as a smooth curve (Catmull-Rom through every day) into `path`. */
    private void smooth(float[] values, int from, int count) {
        path.reset();
        for (int i = 0; i < count; i++) {
            float x = x(i, count), y = y(values[from + i]);
            if (i == 0) { path.moveTo(x, y); continue; }
            float x0 = x(Math.max(0, i - 2), count), y0 = y(values[from + Math.max(0, i - 2)]);
            float x1 = x(i - 1, count), y1 = y(values[from + i - 1]);
            float x3 = x(Math.min(count - 1, i + 1), count), y3 = y(values[from + Math.min(count - 1, i + 1)]);
            // control points a sixth of the way along the neighbours' slope; clamped to the plot so a peak never dips below 0
            float c1x = x1 + (x - x0) / 6f, c1y = Math.min(plot.bottom, y1 + (y - y0) / 6f);
            float c2x = x - (x3 - x1) / 6f, c2y = Math.min(plot.bottom, y - (y3 - y1) / 6f);
            path.cubicTo(c1x, c1y, c2x, c2y, x, y);
        }
    }

    private float x(int i, int count) { return count <= 1 ? plot.centerX() : plot.left + plot.width() * i / (count - 1); }

    private float y(float value) { return plot.bottom - plot.height() * Math.min(1f, value / Math.max(1e-3f, shownMax)); }

    /** The finger's day: a line down, a dot on each line, and a card with the date and each value. */
    private void touchCard(Canvas canvas, int from, int count) {
        float cx = x(touched, count);
        canvas.drawLine(cx, plot.top, cx, plot.bottom, rule);
        for (Series one : series) {
            float cy = y(one.values[from + touched]);
            dot.setStyle(Paint.Style.FILL);
            dot.setColor(card);
            canvas.drawCircle(cx, cy, 5 * density, dot);
            dot.setStyle(Paint.Style.STROKE);
            dot.setStrokeWidth(2.2f * density);
            dot.setColor(one.colour);
            canvas.drawCircle(cx, cy, 5 * density, dot);
        }
        text.setTextAlign(Paint.Align.LEFT);
        float pad = 10 * density, rowH = 17 * density, width = 0;
        String head = date(days[from + touched]);
        text.setTextSize(12 * density);
        width = text.measureText(head);
        for (Series one : series) width = Math.max(width, text.measureText(one.name + "  " + Math.round(one.values[from + touched])) + 14 * density);
        float w = width + 2 * pad, h = pad * 2 + rowH * (series.length + 1) - 4 * density;
        float left = cx + 12 * density;
        if (left + w > getWidth()) left = cx - 12 * density - w;
        tip.set(left, plot.top, left + w, plot.top + h);
        dot.setStyle(Paint.Style.FILL);
        dot.setColor(card);
        dot.setShadowLayer(8 * density, 0, 2 * density, 0x33000000);
        canvas.drawRoundRect(tip, 12 * density, 12 * density, dot);
        dot.clearShadowLayer();
        float y = tip.top + pad + 12 * density;
        text.setColor(ink);
        text.setFakeBoldText(true);
        canvas.drawText(head, tip.left + pad, y, text);
        text.setFakeBoldText(false);
        for (Series one : series) {
            y += rowH;
            dot.setColor(one.colour);
            canvas.drawCircle(tip.left + pad + 4 * density, y - 4 * density, 4 * density, dot);
            text.setColor(ink);
            canvas.drawText(one.name, tip.left + pad + 14 * density, y, text);
            String value = String.valueOf(Math.round(one.values[from + touched]));
            text.setTextAlign(Paint.Align.RIGHT);
            text.setFakeBoldText(true);
            canvas.drawText(value, tip.right - pad, y, text);
            text.setFakeBoldText(false);
            text.setTextAlign(Paint.Align.LEFT);
        }
        text.setTextSize(11 * density);
        text.setColor(muted);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (days.length == 0) return false;
        int count = days.length - first();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                getParent().requestDisallowInterceptTouchEvent(true);
                int at = Math.round((event.getX() - plot.left) / Math.max(1f, plot.width()) * (count - 1));
                at = Math.max(0, Math.min(count - 1, at));
                if (at != touched) { touched = at; if (Motion.haptics()) performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK); invalidate(); }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            default:
                return true;
        }
    }

    private static final String[] MONTHS = {"янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"};

    static String date(long day) {
        java.util.Calendar at = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        at.setTimeInMillis(day * 86_400_000L);
        return at.get(java.util.Calendar.DAY_OF_MONTH) + " " + Text.t(MONTHS[at.get(java.util.Calendar.MONTH)]);
    }

    /** 1234 → "1.2K", 12 → "12". */
    static String short_(float value) {
        if (value >= 1_000_000f) return trim(value / 1_000_000f) + "M";
        if (value >= 1000f) return trim(value / 1000f) + "K";
        return String.valueOf(Math.round(value));
    }

    private static String trim(float v) { return v >= 10f ? String.valueOf(Math.round(v)) : String.valueOf(Math.round(v * 10f) / 10f); }
}
