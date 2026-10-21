package cat.ttcuz.framework;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native counterpart of the supplied motion lab: 240-unit dial, 300/26 spring, 220ms advance. */
final class TimeDial extends LinearLayout {
    private static final android.view.animation.PathInterpolator COLOUR_EASE =
        new android.view.animation.PathInterpolator(.25f, .1f, .25f, 1f);
    private static final android.view.animation.PathInterpolator SHAPE_SPRING =
        new android.view.animation.PathInterpolator(.34f, 1.56f, .64f, 1f);
    private final Skin skin;
    private final TextView hours, minutes, am, pm;
    private final LinearLayout header;
    private final Dial dial;
    private int hour, minute;
    private boolean afternoon, minuteMode;
    private SoundSession sounds;
    private long lastFeedback;
    private final Runnable advance = () -> mode(true);

    TimeDial(Context context, Skin skin, int value) {
        super(context);
        this.skin = skin;
        int h24 = Math.max(0, Math.min(1439, value)) / 60;
        hour = h24 % 12 == 0 ? 12 : h24 % 12;
        minute = Math.max(0, Math.min(1439, value)) % 60;
        afternoon = h24 >= 12;
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setPadding(0, dp(8), 0, dp(8));
        header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        hours = button("Часы", 46); minutes = button("Минуты", 46);
        header.addView(hours, new LayoutParams(0, dp(72), 1));
        TextView colon = new TextView(context);
        colon.setText(":"); colon.setTextSize(46); colon.setTextColor(skin.muted());
        colon.setGravity(Gravity.CENTER);
        LayoutParams colonSize = new LayoutParams(dp(20), dp(72));
        colonSize.leftMargin = colonSize.rightMargin = dp(4);
        header.addView(colon, colonSize);
        header.addView(minutes, new LayoutParams(0, dp(72), 1));
        LinearLayout periods = new LinearLayout(context);
        periods.setOrientation(VERTICAL);
        GradientDrawable periodBorder = new GradientDrawable();
        periodBorder.setColor(android.graphics.Color.TRANSPARENT);
        periodBorder.setStroke(dp(1), skin.muted()); periodBorder.setCornerRadius(dp(8));
        periods.setBackground(periodBorder); periods.setClipToOutline(true);
        am = button("До полудня", 13); am.setText("AM");
        pm = button("После полудня", 13); pm.setText("PM");
        periods.addView(am, new LayoutParams(dp(48), dp(36)));
        periods.addView(pm, new LayoutParams(dp(48), dp(36)));
        LayoutParams periodSize = new LayoutParams(dp(48), dp(72));
        periodSize.leftMargin = dp(16);
        header.addView(periods, periodSize);
        addView(header, new LayoutParams(dp(268), dp(72)));
        dial = new Dial(context);
        LayoutParams dialSize = new LayoutParams(dp(240), dp(240));
        dialSize.topMargin = dp(4);
        addView(dial, dialSize);
        hours.setOnClickListener(v -> mode(false));
        minutes.setOnClickListener(v -> mode(true));
        am.setOnClickListener(v -> period(false));
        pm.setOnClickListener(v -> period(true));
        update();
    }

    int minuteOfDay() { return (hour % 12 + (afternoon ? 12 : 0)) * 60 + minute; }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        // Match the reference's flex-shrink and max-width:100% on narrow panels.
        if (header != null && dial != null) {
            int available = MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED ? dp(268)
                : Math.max(0, MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight());
            header.getLayoutParams().width = Math.min(dp(268), available);
            int size = Math.min(dp(240), available);
            dial.getLayoutParams().width = dial.getLayoutParams().height = size;
        }
        super.onMeasure(widthSpec, heightSpec);
    }

    static float nearestAngle(float current, float desired) {
        return current + ((desired - current) % 360 + 540) % 360 - 180;
    }

    static int valueAtAngle(double degrees, boolean minutes) {
        double normalized = (degrees % 360 + 360) % 360;
        int value = (int) Math.round(normalized / (minutes ? 6 : 30));
        return minutes ? value % 60 : value % 12 == 0 ? 12 : value % 12;
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView button(String description, int size) {
        TextView text = new Chip(getContext());
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        text.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        text.setFontFeatureSettings("tnum"); text.setIncludeFontPadding(false);
        text.setGravity(Gravity.CENTER); text.setFocusable(true); text.setClickable(true);
        text.setContentDescription(description);
        if (size > 24) {
            text.setSingleLine(true);
            if (android.os.Build.VERSION.SDK_INT >= 26)
                text.setAutoSizeTextTypeUniformWithConfiguration(24, size, 1, TypedValue.COMPLEX_UNIT_SP);
            else text.setTextSize(size / Math.max(1f, getResources().getConfiguration().fontScale));
        }
        return text;
    }
    private void chip(TextView text, boolean selected, float radius) {
        boolean period = text == am || text == pm;
        int colour = selected ? Expressive.mix(skin.card, Accent.colour(), .30f)
            : period ? android.graphics.Color.TRANSPARENT : Expressive.mix(skin.card, skin.text, .08f);
        ((Chip) text).select(selected, period ? 0 : dp(radius), colour);
    }

    /** CSS timing from the reference, with one cached drawable and interruptible motion. */
    private final class Chip extends TextView {
        private final GradientDrawable background = new GradientDrawable();
        private ValueAnimator transition;
        private float radius, targetRadius;
        private int colour, targetColour;
        private boolean initialized;
        Chip(Context context) {
            super(context); setBackground(background); setTextColor(skin.text);
        }
        void select(boolean selected, float endRadius, int endColour) {
            if (initialized && isSelected() == selected && targetRadius == endRadius && targetColour == endColour)
                return;
            setSelected(selected);
            if (transition != null) transition.cancel();
            targetRadius = endRadius; targetColour = endColour;
            if (!initialized || !isAttachedToWindow() || !isShown() || !Motion.enabled(getContext())) {
                initialized = true; finish(); return;
            }
            float startRadius = radius;
            int startColour = colour;
            transition = ValueAnimator.ofFloat(0, 1); transition.setDuration(300);
            transition.setInterpolator(new android.view.animation.LinearInterpolator());
            transition.addUpdateListener(a -> {
                float t = (Float) a.getAnimatedValue();
                radius = startRadius + (targetRadius - startRadius) * SHAPE_SPRING.getInterpolation(t);
                colour = Expressive.mix(startColour, targetColour,
                    COLOUR_EASE.getInterpolation(Math.min(1, t * 1.5f)));
                background.setCornerRadius(radius); background.setColor(colour);
            });
            transition.start();
        }
        private void finish() {
            if (transition != null) { transition.cancel(); transition = null; }
            radius = targetRadius; colour = targetColour;
            background.setCornerRadius(radius); background.setColor(colour);
        }
        @Override protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);
            if (visibility != VISIBLE && initialized) finish();
        }
        @Override protected void onDetachedFromWindow() {
            finish(); super.onDetachedFromWindow();
        }
    }
    private void update() {
        String h = Integer.toString(hour), m = String.format(Locale.ROOT, "%02d", minute);
        if (!h.contentEquals(hours.getText())) hours.setText(h);
        if (!m.contentEquals(minutes.getText())) minutes.setText(m);
        chip(hours, !minuteMode, minuteMode ? 16 : 12);
        chip(minutes, minuteMode, minuteMode ? 12 : 16);
        chip(am, !afternoon, 0); chip(pm, afternoon, 0);
        if (dial != null) dial.setContentDescription((minuteMode ? "Минуты: " + minute : "Часы: " + hour)
            + ". Стрелки меняют значение. " + String.format(Locale.ROOT, "%02d:%02d",
                minuteOfDay() / 60, minute));
    }
    private void mode(boolean minutes) {
        removeCallbacks(advance);
        if (minuteMode == minutes) return;
        minuteMode = minutes;
        update(); dial.labels(); dial.go();
    }
    private void period(boolean pmValue) {
        if (afternoon == pmValue) return;
        afternoon = pmValue; feedback(); update();
    }
    private void select(int value) {
        int old = minuteMode ? minute : hour;
        if (minuteMode) minute = (value % 60 + 60) % 60;
        else hour = ((value % 12 + 12) % 12); // zero is twelve on the clock
        if (!minuteMode && hour == 0) hour = 12;
        if (old != (minuteMode ? minute : hour)) {
            feedback(); update();
        }
        dial.go();
    }

    private void feedback() {
        long now = SystemClock.uptimeMillis();
        if (now - lastFeedback < 50) return;
        lastFeedback = now;
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        SoundSession session = sounds;
        if (session != null) session.play(getContext().getApplicationContext());
    }

    private static final class SoundSession {
        private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "ttcuz-clock-sound"); thread.setDaemon(true); return thread;
        });
        private AudioTrack sound; // private to this session's worker
        void play(Context context) {
            worker.execute(() -> {
            try {
                AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (manager == null || manager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) return;
                if (android.provider.Settings.System.getInt(context.getContentResolver(),
                    android.provider.Settings.System.SOUND_EFFECTS_ENABLED, 1) == 0) return;
                if (sound == null) sound = woodenClick();
                sound.stop(); sound.reloadStaticData(); sound.play();
            } catch (RuntimeException ignored) {}
            });
        }
        void close() {
            worker.execute(() -> { if (sound != null) { sound.release(); sound = null; } });
            worker.shutdown();
        }
    }
    /** Two damped resonances, quiet attack and 28ms tail: a soft wooden click. */
    private static AudioTrack woodenClick() {
        int rate = 22050, count = rate * 28 / 1000;
        short[] pcm = new short[count];
        for (int i = 0; i < count; i++) {
            double t = i / (double) rate;
            double wave = Math.sin(2 * Math.PI * 980 * t) + .35 * Math.sin(2 * Math.PI * 1660 * t);
            pcm[i] = (short) (wave * (1 - Math.exp(-t * 1800)) * Math.exp(-t * 185) * .12 * 32767);
        }
        AudioTrack track = new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(count * 2).setTransferMode(AudioTrack.MODE_STATIC).build();
        track.write(pcm, 0, count);
        return track;
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        sounds = new SoundSession();
    }
    @Override protected void onDetachedFromWindow() {
        removeCallbacks(advance);
        dial.stop();
        SoundSession session = sounds;
        sounds = null;
        if (session != null) session.close();
        super.onDetachedFromWindow();
    }

    private final class Dial extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final String[] hourLabels = new String[12], minuteLabels = new String[12];
        private final float[] labelX = new float[12], labelY = new float[12];
        private float angle = (hour % 12) * 30, velocity, target = angle, labelAlpha = 1;
        private long previous;
        private boolean running, dragging, minuteLabelsShown;
        private ValueAnimator fade;
        private final Runnable frame = this::step;
        Dial(Context context) {
            super(context); setFocusable(true); setClickable(true);
            paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            paint.setTextAlign(Paint.Align.CENTER);
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6;
                labelX[i] = 88 * (float) Math.sin(a); labelY[i] = -88 * (float) Math.cos(a);
                hourLabels[i] = Integer.toString(i == 0 ? 12 : i);
                minuteLabels[i] = String.format(Locale.ROOT, "%02d", i * 5);
            }
        }
        private void go() {
            float desired = minuteMode ? minute * 6 : (hour % 12) * 30;
            target = nearestAngle(angle, desired);
            if (!Motion.enabled(getContext())) {
                stop(); angle = target; velocity = 0; invalidate(); return;
            }
            if (!running) { running = true; previous = System.nanoTime(); postOnAnimation(frame); }
        }
        private void step() {
            long now = System.nanoTime();
            float dt = Math.min(.05f, (now - previous) / 1e9f); previous = now;
            while (dt > 0) {
                float h = Math.min(dt, 1f / 120f);
                velocity += (-300 * (angle - target) - 26 * velocity) * h;
                angle += velocity * h; dt -= h;
            }
            if (Math.abs(angle - target) < .001f && Math.abs(velocity) < .01f) {
                angle = target; velocity = 0; running = false;
            }
            invalidate();
            if (running) postOnAnimation(frame);
        }
        private void stop() {
            running = false; removeCallbacks(frame);
            if (fade != null) fade.cancel();
            labelAlpha = 1; minuteLabelsShown = minuteMode;
        }
        @Override protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);
            if (visibility != VISIBLE) { stop(); angle = target; velocity = 0; }
        }
        private void labels() {
            if (fade != null) fade.cancel();
            if (!Motion.enabled(getContext())) { minuteLabelsShown = minuteMode; invalidate(); return; }
            fade = ValueAnimator.ofFloat(1, 0, 1); fade.setDuration(280);
            fade.setInterpolator(new android.view.animation.LinearInterpolator());
            fade.addUpdateListener(a -> {
                float t = a.getAnimatedFraction();
                labelAlpha = t < .5f ? 1 - COLOUR_EASE.getInterpolation(t * 2)
                    : COLOUR_EASE.getInterpolation((t - .5f) * 2);
                if (a.getAnimatedFraction() >= .5f) minuteLabelsShown = minuteMode;
                invalidate();
            }); fade.start();
        }
        @Override protected void onDraw(Canvas canvas) {
            float scale = Math.min(getWidth(), getHeight()) / 240f;
            int save = canvas.save(); canvas.translate(getWidth() / 2f, getHeight() / 2f);
            canvas.scale(scale, scale);
            paint.setColor(Expressive.mix(skin.card, skin.text, .08f)); canvas.drawCircle(0, 0, 112, paint);
            double a = Math.toRadians(angle);
            float x = (float) Math.sin(a), y = -(float) Math.cos(a);
            int accent = Accent.colour(); paint.setColor(accent); paint.setStrokeWidth(2);
            canvas.drawLine(0, 0, x * 68, y * 68, paint);
            canvas.drawCircle(0, 0, 3, paint); canvas.drawCircle(x * 88, y * 88, 20, paint);
            paint.setTextSize(16);
            float baseline = -(paint.ascent() + paint.descent()) / 2;
            int onAccent = (android.graphics.Color.red(accent) * 299 + android.graphics.Color.green(accent) * 587
                + android.graphics.Color.blue(accent) * 114) / 1000 > 150 ? 0xFF111111 : 0xFFFFFFFF;
            for (int i = 0; i < 12; i++) {
                float difference = Math.abs(((angle - i * 30) % 360 + 540) % 360 - 180);
                paint.setColor(difference < 14 ? onAccent : skin.text);
                paint.setAlpha(Math.round(labelAlpha * 255));
                canvas.drawText(minuteLabelsShown ? minuteLabels[i] : hourLabels[i], labelX[i], labelY[i] + baseline, paint);
            }
            paint.setAlpha(255); canvas.restoreToCount(save);
        }
        private void position(MotionEvent event) {
            float x = event.getX() - getWidth() / 2f, y = event.getY() - getHeight() / 2f;
            double degrees = (Math.toDegrees(Math.atan2(x, -y)) + 360) % 360;
            select(valueAtAngle(degrees, minuteMode));
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    removeCallbacks(advance); TimeDial.this.removeCallbacks(advance);
                    dragging = true; getParent().requestDisallowInterceptTouchEvent(true); position(event); return true;
                case MotionEvent.ACTION_MOVE:
                    if (dragging) position(event); return true;
                case MotionEvent.ACTION_UP:
                    if (dragging) {
                        position(event); dragging = false; performClick();
                        if (!minuteMode) TimeDial.this.postDelayed(advance, 220);
                    }
                    getParent().requestDisallowInterceptTouchEvent(false); return true;
                case MotionEvent.ACTION_CANCEL:
                    dragging = false; getParent().requestDisallowInterceptTouchEvent(false); return true;
            }
            return super.onTouchEvent(event);
        }
        @Override public boolean performClick() { super.performClick(); return true; }
        @Override public boolean onKeyDown(int key, KeyEvent event) {
            int direction = key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_RIGHT ? 1
                : key == KeyEvent.KEYCODE_DPAD_DOWN || key == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 0;
            if (direction != 0) { select((minuteMode ? minute : hour) + direction); return true; }
            return super.onKeyDown(key, event);
        }
        @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName("android.widget.SeekBar");
            info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT,
                minuteMode ? 0 : 1, minuteMode ? 59 : 12, minuteMode ? minute : hour));
            info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        }
        @Override public boolean performAccessibilityAction(int action, android.os.Bundle args) {
            if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD || action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
                select((minuteMode ? minute : hour) + (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ? 1 : -1));
                return true;
            }
            return super.performAccessibilityAction(action, args);
        }
    }
}
