package cat.ttcuz.framework;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Four dots and a round keypad, as a phone's own lock screen draws them. A digit fills its dot with a small pop;
 * four digits are handed to the listener, which says whether they were right: wrong shakes the dots and buzzes,
 * right fills them with the accent. The keys come in one after another when the pad appears.
 */
final class PinPad extends LinearLayout {
    static final int LENGTH = 4;

    interface Listener {
        /** Four digits were entered: true when they are right (the pad shows it), false to shake and start over. */
        boolean entered(String pin);
    }

    private final Listener listener;
    private final StringBuilder typed = new StringBuilder();
    private final Dots dots;
    private final TextView title, note;
    private final LinearLayout keys;
    private final float density;
    private final int ink, accent;
    private boolean locked;

    PinPad(Context context, Skin skin, String heading, Listener listener) {
        super(context);
        this.listener = listener;
        density = context.getResources().getDisplayMetrics().density;
        ink = skin.text;
        accent = Accent.colour();
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);

        title = new TextView(context);
        title.setText(heading);
        title.setTextColor(ink);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setGravity(Gravity.CENTER);
        addView(title, new LayoutParams(-2, -2));

        dots = new Dots(context);
        LayoutParams dotsAt = new LayoutParams(dp(LENGTH * 28), dp(28));
        dotsAt.topMargin = dp(22);
        addView(dots, dotsAt);

        note = new TextView(context);
        note.setTextColor(skin.muted());
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        note.setGravity(Gravity.CENTER);
        note.setMinHeight(dp(20));
        LayoutParams noteAt = new LayoutParams(-2, -2);
        noteAt.topMargin = dp(8);
        addView(note, noteAt);

        keys = new LinearLayout(context);
        keys.setOrientation(VERTICAL);
        LayoutParams keysAt = new LayoutParams(-2, -2);
        keysAt.topMargin = dp(18);
        addView(keys, keysAt);
        String[][] rows = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"", "0", "<"}};
        int index = 0;
        for (String[] row : rows) {
            LinearLayout line = new LinearLayout(context);
            line.setOrientation(HORIZONTAL);
            for (String label : row) line.addView(key(context, label, index++), keyParams());
            keys.addView(line);
        }
    }

    private int dp(float value) { return Math.round(value * density); }

    private LayoutParams keyParams() {
        LayoutParams at = new LayoutParams(dp(72), dp(72));
        at.setMargins(dp(10), dp(6), dp(10), dp(6));
        return at;
    }

    /** One round key; empty is a spacer, "<" erases. They pop in one after another. */
    private View key(Context context, final String label, int index) {
        View key;
        if (label.equals("<")) {
            key = new Erase(context);
            key.setContentDescription(Text.t("Стереть"));
            key.setOnClickListener(v -> erase());
        } else {
            TextView digit = new TextView(context);
            digit.setText(label);
            digit.setTextColor(ink);
            digit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
            digit.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            digit.setGravity(Gravity.CENTER);
            key = digit;
            if (!label.isEmpty()) {
                GradientDrawable round = new GradientDrawable();
                round.setShape(GradientDrawable.OVAL);
                round.setColor((ink & 0xFFFFFF) | 0x14000000);
                key.setBackground(new RippleDrawable(ColorStateList.valueOf((accent & 0xFFFFFF) | 0x40000000), round, null));
                key.setOnClickListener(v -> type(label));
            }
        }
        if (Motion.enabled(context)) {
            key.setAlpha(0f);
            key.setScaleX(.6f);
            key.setScaleY(.6f);
            key.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(40L + index * 22L).setDuration(320L)
                    .setInterpolator(Motion.EMPHASIZED).start();
        }
        return key;
    }

    void heading(String text) { title.setText(text); }

    void note(String text) { note.setText(text); }

    /** Keys pressed while locked (after too many wrong tries) do nothing. */
    void lock(boolean on) {
        locked = on;
        keys.animate().alpha(on ? .35f : 1f).setDuration(200L).start();
    }

    private void type(String digit) {
        if (locked || typed.length() >= LENGTH) return;
        typed.append(digit);
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        UiSound.tap();
        dots.fill(typed.length());
        if (typed.length() < LENGTH) return;
        final String pin = typed.toString();
        // the fourth dot is seen filled before the answer
        postDelayed(() -> {
            if (listener.entered(pin)) {
                performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                dots.success();
            } else {
                wrong();
            }
        }, 120L);
    }

    private void erase() {
        if (locked || typed.length() == 0) return;
        typed.setLength(typed.length() - 1);
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        UiSound.tap();
        dots.fill(typed.length());
    }

    /** The dots shake, the phone buzzes, and the pad starts over. */
    void wrong() {
        performHapticFeedback(HapticFeedbackConstants.REJECT);
        float d = density;
        ObjectAnimator shake = ObjectAnimator.ofFloat(dots, "translationX", 0, 14 * d, -14 * d, 10 * d, -10 * d, 5 * d, -5 * d, 0);
        shake.setDuration(420L);
        shake.start();
        dots.error();
        postDelayed(this::clear, 420L);
    }

    void clear() {
        typed.setLength(0);
        dots.fill(0);
    }

    /** The four dots: each fills with a small pop; red while wrong, the accent once right. */
    private final class Dots extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] fill = new float[LENGTH];
        private int shown, colour;

        Dots(Context context) {
            super(context);
            colour = ink;
        }

        void fill(int count) {
            colour = ink;
            for (int i = 0; i < LENGTH; i++) {
                final int dot = i;
                float want = i < count ? 1f : 0f;
                if (fill[i] == want) continue;
                if (!Motion.enabled(getContext())) { fill[i] = want; continue; }
                ValueAnimator a = ValueAnimator.ofFloat(fill[i], want);
                a.setDuration(want > 0 ? 260L : 160L);
                // over 1 on the way in: a small pop
                a.setInterpolator(want > 0 ? new android.view.animation.OvershootInterpolator(3f) : Motion.BOTH);
                a.addUpdateListener(v -> { fill[dot] = (Float) v.getAnimatedValue(); invalidate(); });
                a.start();
            }
            shown = count;
            invalidate();
        }

        void error() { colour = 0xFFE5484D; invalidate(); }

        void success() { colour = accent; invalidate(); }

        @Override protected void onDraw(Canvas canvas) {
            float step = getWidth() / (float) LENGTH, cy = getHeight() / 2f, r = 6.5f * density;
            for (int i = 0; i < LENGTH; i++) {
                float cx = step * (i + .5f);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1.6f * density);
                paint.setColor(colour == ink ? (ink & 0xFFFFFF) | 0x80000000 : colour);
                canvas.drawCircle(cx, cy, r, paint);
                float f = Math.max(0f, fill[i]);
                if (f <= 0f) continue;
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(colour);
                canvas.drawCircle(cx, cy, r * f, paint);
            }
        }
    }

    /** The erase key: a backspace shape, drawn rather than taken from a font that may not have it. */
    private final class Erase extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path shape = new Path();

        Erase(Context context) {
            super(context);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            GradientDrawable round = new GradientDrawable();
            round.setShape(GradientDrawable.OVAL);
            round.setColor(0);
            setBackground(new RippleDrawable(ColorStateList.valueOf((accent & 0xFFFFFF) | 0x40000000), round, round));
        }

        @Override protected void onDraw(Canvas canvas) {
            float s = density, cx = getWidth() / 2f, cy = getHeight() / 2f, w = 26 * s, h = 18 * s;
            shape.reset();
            shape.moveTo(cx - w / 2f + 7 * s, cy - h / 2f);
            shape.lineTo(cx + w / 2f, cy - h / 2f);
            shape.lineTo(cx + w / 2f, cy + h / 2f);
            shape.lineTo(cx - w / 2f + 7 * s, cy + h / 2f);
            shape.lineTo(cx - w / 2f, cy);
            shape.close();
            float x = cx + 3 * s, d = 3.5f * s;
            shape.moveTo(x - d, cy - d); shape.lineTo(x + d, cy + d);
            shape.moveTo(x + d, cy - d); shape.lineTo(x - d, cy + d);
            paint.setStrokeWidth(1.8f * s);
            paint.setColor(ink);
            canvas.drawPath(shape, paint);
        }
    }
}
