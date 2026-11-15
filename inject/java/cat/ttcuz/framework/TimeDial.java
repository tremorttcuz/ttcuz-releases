package cat.ttcuz.framework;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import cat.ttcuz.framework.TimeDial;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

final class TimeDial extends LinearLayout {
    private static final PathInterpolator COLOUR_EASE = new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);
    private static final PathInterpolator SHAPE_SPRING = new PathInterpolator(0.34f, 1.56f, 0.64f, 1.0f);
    private final Runnable advance;
    private boolean afternoon;
    private final TextView am;
    private final Dial dial;
    private final LinearLayout header;
    private int hour;
    private final TextView hours;
    private long lastFeedback;
    private int minute;
    private boolean minuteMode;
    private final TextView minutes;
    private final TextView pm;
    private final Skin skin;
    private SoundSession sounds;
    /** 24-hour time: hours 0..23, no AM/PM, a second ring of hours inside the first. */
    private boolean h24;
    private final LinearLayout periods;
    private final TextView format12, format24;
    private final java.util.List<TextView> presetChips = new java.util.ArrayList<>();

    /** Jumps to a time of day (0..1439), as if it had been picked on the dial. */
    private void setTime(int day) {
        int h = Math.max(0, Math.min(1439, day)) / 60;
        this.minute = Math.max(0, Math.min(1439, day)) % 60;
        this.afternoon = h >= 12;
        this.hour = this.h24 ? h : (h % 12 == 0 ? 12 : h % 12);
        removeCallbacks(this.advance);
        this.minuteMode = false;
        feedback();
        update();
        this.dial.labels();
        this.dial.go();
    }

    private String clock(int day) {
        int h = day / 60, m = day % 60;
        if (this.h24) return String.format(Locale.ROOT, "%02d:%02d", h, m);
        return String.format(Locale.ROOT, "%d:%02d %s", h % 12 == 0 ? 12 : h % 12, m, h >= 12 ? "PM" : "AM");
    }

    static float nearestAngle(float f, float f2) {
        return (f + ((((f2 - f) % 360.0f) + 540.0f) % 360.0f)) - 180.0f;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void restored_new_0() {
        mode(true);
    }

    TimeDial(Context context, Skin skin, int i) {
        super(context);
        this.advance = new Runnable() { // from class: cat.ttcuz.framework.TimeDial$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                TimeDial.this.restored_new_0();
            }
        };
        this.skin = skin;
        boolean system24 = android.text.format.DateFormat.is24HourFormat(context);
        try { this.h24 = context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).getBoolean(TimeWheel.KEY_24, system24); }
        catch (Throwable ignored) { this.h24 = system24; }
        int max = Math.max(0, Math.min(1439, i)) / 60;
        int i2 = max % 12;
        this.hour = h24 ? max : i2 == 0 ? 12 : i2;
        this.minute = Math.max(0, Math.min(1439, i)) % 60;
        this.afternoon = max >= 12;
        setOrientation(1);
        setGravity(1);
        setPadding(0, dp(8.0f), 0, dp(8.0f));
        LinearLayout linearLayout = new LinearLayout(context);
        this.header = linearLayout;
        linearLayout.setGravity(16);
        TextView button = button(Text.t("Часы"), 46);
        this.hours = button;
        TextView button2 = button(Text.t("Минуты"), 46);
        this.minutes = button2;
        linearLayout.addView(button, new LinearLayout.LayoutParams(0, dp(72.0f), 1.0f));
        TextView textView = new TextView(context);
        textView.setText(":");
        textView.setTextSize(46.0f);
        textView.setTextColor(skin.muted());
        textView.setGravity(17);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(20.0f), dp(72.0f));
        int dp = dp(4.0f);
        layoutParams.rightMargin = dp;
        layoutParams.leftMargin = dp;
        linearLayout.addView(textView, layoutParams);
        linearLayout.addView(button2, new LinearLayout.LayoutParams(0, dp(72.0f), 1.0f));
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(1);
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(0);
        gradientDrawable.setStroke(dp(1.0f), skin.muted());
        gradientDrawable.setCornerRadius(dp(8.0f));
        linearLayout2.setBackground(gradientDrawable);
        linearLayout2.setClipToOutline(true);
        TextView button3 = button(Text.t("До полудня"), 13);
        this.am = button3;
        button3.setText("AM");
        TextView button4 = button(Text.t("После полудня"), 13);
        this.pm = button4;
        button4.setText("PM");
        linearLayout2.addView(button3, new LinearLayout.LayoutParams(dp(48.0f), dp(36.0f)));
        linearLayout2.addView(button4, new LinearLayout.LayoutParams(dp(48.0f), dp(36.0f)));
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(dp(48.0f), dp(72.0f));
        layoutParams2.leftMargin = dp(16.0f);
        linearLayout.addView(linearLayout2, layoutParams2);
        this.periods = linearLayout2;
        addView(linearLayout, new LinearLayout.LayoutParams(dp(296.0f), dp(72.0f)));
        Dial dial = new Dial(context);
        this.dial = dial;
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(dp(272.0f), dp(272.0f));
        layoutParams3.topMargin = dp(4.0f);
        addView(dial, layoutParams3);
        // 12- or 24-hour time, as a connected pair under the dial
        LinearLayout formats = new LinearLayout(context);
        formats.setGravity(17);
        this.format12 = button(Text.t("12 ч"), 14);
        this.format12.setText(Text.t("12 ч"));
        this.format24 = button(Text.t("24 ч"), 14);
        this.format24.setText(Text.t("24 ч"));
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(dp(96.0f), dp(40.0f));
        formats.addView(this.format12, half);
        LinearLayout.LayoutParams otherHalf = new LinearLayout.LayoutParams(dp(96.0f), dp(40.0f));
        otherHalf.leftMargin = dp(4.0f);
        formats.addView(this.format24, otherHalf);
        LinearLayout.LayoutParams formatsParams = new LinearLayout.LayoutParams(-2, -2);
        formatsParams.topMargin = dp(12.0f);
        addView(formats, formatsParams);
        // the usual times, one tap each
        android.widget.HorizontalScrollView presetStrip = new android.widget.HorizontalScrollView(context);
        presetStrip.setHorizontalScrollBarEnabled(false);
        LinearLayout presets = new LinearLayout(context);
        presets.setPadding(dp(4.0f), 0, dp(4.0f), 0);
        for (final int at : new int[]{9 * 60, 12 * 60, 18 * 60, 21 * 60, 23 * 60}) {
            TextView preset = button(Text.t("Время"), 14);
            preset.setTag(at);
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(-2, dp(36.0f));
            size.rightMargin = dp(6.0f);
            preset.setPadding(dp(14.0f), 0, dp(14.0f), 0);
            presets.addView(preset, size);
            preset.setOnClickListener(v -> setTime(at));
            this.presetChips.add(preset);
        }
        presetStrip.addView(presets);
        LinearLayout.LayoutParams presetParams = new LinearLayout.LayoutParams(-2, -2);
        presetParams.topMargin = dp(10.0f);
        addView(presetStrip, presetParams);
        this.format12.setOnClickListener(v -> format(false));
        this.format24.setOnClickListener(v -> format(true));
        button.setOnClickListener(new View.OnClickListener() { // from class: cat.ttcuz.framework.TimeDial$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                TimeDial.this.restored_new_1(view);
            }
        });
        button2.setOnClickListener(new View.OnClickListener() { // from class: cat.ttcuz.framework.TimeDial$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                TimeDial.this.restored_new_2(view);
            }
        });
        button3.setOnClickListener(new View.OnClickListener() { // from class: cat.ttcuz.framework.TimeDial$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                TimeDial.this.restored_new_3(view);
            }
        });
        button4.setOnClickListener(new View.OnClickListener() { // from class: cat.ttcuz.framework.TimeDial$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                TimeDial.this.restored_new_4(view);
            }
        });
        update();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void restored_new_1(View view) {
        mode(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void restored_new_2(View view) {
        mode(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void restored_new_3(View view) {
        period(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void restored_new_4(View view) {
        period(true);
    }

    int minuteOfDay() {
        if (this.h24) return this.hour * 60 + this.minute;
        return (((this.hour % 12) + (this.afternoon ? 12 : 0)) * 60) + this.minute;
    }

    /** Switches between 12- and 24-hour time, keeping the chosen time, and remembers the choice. */
    private void format(boolean on) {
        if (on == this.h24) return;
        int day = minuteOfDay();
        this.h24 = on;
        try { getContext().getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).edit().putBoolean(TimeWheel.KEY_24, on).apply(); }
        catch (Throwable ignored) { }
        int h = day / 60;
        this.afternoon = h >= 12;
        this.hour = on ? h : (h % 12 == 0 ? 12 : h % 12);
        feedback();
        update();
        this.dial.labels();
        this.dial.go();
    }

    @Override // android.widget.LinearLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        if (this.header != null && this.dial != null) {
            int dp = View.MeasureSpec.getMode(i) == 0 ? dp(296.0f) : Math.max(0, (View.MeasureSpec.getSize(i) - getPaddingLeft()) - getPaddingRight());
            this.header.getLayoutParams().width = Math.min(dp(296.0f), dp);
            int min = Math.min(dp(272.0f), dp);
            ViewGroup.LayoutParams layoutParams = this.dial.getLayoutParams();
            this.dial.getLayoutParams().height = min;
            layoutParams.width = min;
        }
        super.onMeasure(i, i2);
    }

    /** An hour of a 24-hour dial: the outer ring holds 0..11, the inner one 12..23. */
    static int hourAtAngle(double d, boolean inner) {
        int round = (int) Math.round((((d % 360.0d) + 360.0d) % 360.0d) / 30) % 12;
        return inner ? round + 12 : round;
    }

    static int valueAtAngle(double d, boolean z) {
        int round = (int) Math.round((((d % 360.0d) + 360.0d) % 360.0d) / (z ? 6 : 30));
        if (z) {
            return round % 60;
        }
        int i = round % 12;
        if (i == 0) {
            return 12;
        }
        return i;
    }

    private int dp(float f) {
        return Math.round(f * getResources().getDisplayMetrics().density);
    }

    private TextView button(String str, int i) {
        Chip chip = new Chip(getContext());
        float f = i;
        chip.setTextSize(2, f);
        chip.setTypeface(Typeface.create(Fonts.SANS, 0));
        chip.setFontFeatureSettings("tnum");
        chip.setIncludeFontPadding(false);
        chip.setGravity(17);
        chip.setFocusable(true);
        chip.setClickable(true);
        chip.setContentDescription(str);
        if (i > 24) {
            chip.setSingleLine(true);
            if (Build.VERSION.SDK_INT >= 26) {
                chip.setAutoSizeTextTypeUniformWithConfiguration(24, i, 1, 2);
            } else {
                chip.setTextSize(f / Math.max(1.0f, getResources().getConfiguration().fontScale));
            }
        }
        return chip;
    }

    private void chip(TextView textView, boolean z, float f) {
        int i = 0;
        boolean z2 = textView == this.am || textView == this.pm;
        if (textView == this.format12 || textView == this.format24 || this.presetChips.contains(textView)) {
            // the chosen one filled in the accent with readable text, the others quiet: clear at any accent
            int accent = Accent.colour();
            ((Chip) textView).select(z, dp(z ? 20.0f : 10.0f), z ? accent : Expressive.mix(this.skin.card, this.skin.text, 0.08f));
            int ink = (((Color.red(accent) * 299) + (Color.green(accent) * 587)) + (Color.blue(accent) * 114)) / 1000 > 150 ? 0xFF111111 : 0xFFFFFFFF;
            textView.setTextColor(z ? ink : this.skin.text);
            return;
        }
        if (z) {
            i = Expressive.mix(this.skin.card, Accent.colour(), 0.3f);
        } else if (!z2) {
            i = Expressive.mix(this.skin.card, this.skin.text, 0.08f);
        }
        ((Chip) textView).select(z, z2 ? 0.0f : dp(f), i);
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class Chip extends TextView {
        private final GradientDrawable background;
        private int colour;
        private boolean initialized;
        private float radius;
        private int targetColour;
        private float targetRadius;
        private ValueAnimator transition;

        Chip(Context context) {
            super(context);
            GradientDrawable gradientDrawable = new GradientDrawable();
            this.background = gradientDrawable;
            setBackground(gradientDrawable);
            setTextColor(TimeDial.this.skin.text);
        }

        void select(boolean z, float f, int i) {
            if (this.initialized && isSelected() == z && this.targetRadius == f && this.targetColour == i) {
                return;
            }
            setSelected(z);
            ValueAnimator valueAnimator = this.transition;
            if (valueAnimator != null) {
                valueAnimator.cancel();
            }
            this.targetRadius = f;
            this.targetColour = i;
            if (!this.initialized || !isAttachedToWindow() || !isShown() || !Motion.enabled(getContext())) {
                this.initialized = true;
                finish();
                return;
            }
            final float f2 = this.radius;
            final int i2 = this.colour;
            ValueAnimator ofFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
            this.transition = ofFloat;
            ofFloat.setDuration(300L);
            this.transition.setInterpolator(new LinearInterpolator());
            this.transition.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: cat.ttcuz.framework.TimeDial$Chip$$ExternalSyntheticLambda0
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                    TimeDial.Chip.this.restored_select_0(f2, i2, valueAnimator2);
                }
            });
            this.transition.start();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void restored_select_0(float f, int i, ValueAnimator valueAnimator) {
            float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            this.radius = f + ((this.targetRadius - f) * TimeDial.SHAPE_SPRING.getInterpolation(floatValue));
            this.colour = Expressive.mix(i, this.targetColour, TimeDial.COLOUR_EASE.getInterpolation(Math.min(1.0f, floatValue * 1.5f)));
            this.background.setCornerRadius(this.radius);
            this.background.setColor(this.colour);
        }

        private void finish() {
            ValueAnimator valueAnimator = this.transition;
            if (valueAnimator != null) {
                valueAnimator.cancel();
                this.transition = null;
            }
            float f = this.targetRadius;
            this.radius = f;
            this.colour = this.targetColour;
            this.background.setCornerRadius(f);
            this.background.setColor(this.colour);
        }

        @Override // android.view.View
        protected void onWindowVisibilityChanged(int i) {
            super.onWindowVisibilityChanged(i);
            if (i == 0 || !this.initialized) {
                return;
            }
            finish();
        }

        @Override // android.view.View
        protected void onDetachedFromWindow() {
            finish();
            super.onDetachedFromWindow();
        }
    }

    private void update() {
        int i;
        StringBuilder sb;
        String num = this.h24 ? String.format(Locale.ROOT, "%02d", Integer.valueOf(this.hour)) : Integer.toString(this.hour);
        if (this.periods != null) this.periods.setVisibility(this.h24 ? View.GONE : View.VISIBLE);
        if (this.format12 != null) { chip(this.format12, !this.h24, 0.0f); chip(this.format24, this.h24, 0.0f); }
        for (TextView preset : this.presetChips) {
            int at = (Integer) preset.getTag();
            String words = clock(at);
            if (!words.contentEquals(preset.getText())) preset.setText(words);
            chip(preset, at == minuteOfDay(), 0.0f);
        }
        String format = String.format(Locale.ROOT, "%02d", Integer.valueOf(this.minute));
        if (!num.contentEquals(this.hours.getText())) {
            this.hours.setText(num);
        }
        if (!format.contentEquals(this.minutes.getText())) {
            this.minutes.setText(format);
        }
        TextView textView = this.hours;
        boolean z = this.minuteMode;
        chip(textView, !z, z ? 16.0f : 12.0f);
        TextView textView2 = this.minutes;
        boolean z2 = this.minuteMode;
        chip(textView2, z2, z2 ? 12.0f : 16.0f);
        chip(this.am, !this.afternoon, 0.0f);
        chip(this.pm, this.afternoon, 0.0f);
        Dial dial = this.dial;
        if (dial != null) {
            if (this.minuteMode) {
                i = this.minute;
                sb = new StringBuilder(Text.t("Минуты") + ": ");
            } else {
                i = this.hour;
                sb = new StringBuilder(Text.t("Часы") + ": ");
            }
            sb.append(i);
            dial.setContentDescription(sb.toString() + ". " + Text.t("Стрелки меняют значение") + ". " + String.format(Locale.ROOT, "%02d:%02d", Integer.valueOf(minuteOfDay() / 60), Integer.valueOf(this.minute)));
        }
    }

    private void mode(boolean z) {
        removeCallbacks(this.advance);
        if (this.minuteMode == z) {
            return;
        }
        this.minuteMode = z;
        update();
        this.dial.labels();
        this.dial.go();
    }

    private void period(boolean z) {
        if (this.afternoon == z) {
            return;
        }
        this.afternoon = z;
        feedback();
        update();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void select(int i) {
        boolean z = this.minuteMode;
        int i2 = z ? this.minute : this.hour;
        if (z) {
            this.minute = ((i % 60) + 60) % 60;
        } else if (this.h24) {
            this.hour = ((i % 24) + 24) % 24;
        } else {
            this.hour = ((i % 12) + 12) % 12;
        }
        if (!z && !this.h24 && this.hour == 0) {
            this.hour = 12;
        }
        if (i2 != (z ? this.minute : this.hour)) {
            feedback();
            update();
        }
        this.dial.go();
    }

    private void feedback() {
        long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis - this.lastFeedback < 50) {
            return;
        }
        this.lastFeedback = uptimeMillis;
        performHapticFeedback(4);
        SoundSession soundSession = this.sounds;
        if (soundSession != null) {
            soundSession.play(getContext().getApplicationContext());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class SoundSession {
        private AudioTrack sound;
        private final ExecutorService worker;

        private SoundSession() {
            this.worker = Executors.newSingleThreadExecutor(new ThreadFactory() { // from class: cat.ttcuz.framework.TimeDial$SoundSession$$ExternalSyntheticLambda1
                @Override // java.util.concurrent.ThreadFactory
                public final Thread newThread(Runnable runnable) {
                    return TimeDial.SoundSession.restored_new_0(runnable);
                }
            });
        }

        static /* synthetic */ Thread restored_new_0(Runnable runnable) {
            Thread thread = new Thread(runnable, "ttcuz-clock-sound");
            thread.setDaemon(true);
            return thread;
        }

        void play(final Context context) {
            this.worker.execute(new Runnable() { // from class: cat.ttcuz.framework.TimeDial$SoundSession$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    TimeDial.SoundSession.this.restored_play_1(context);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void restored_play_1(Context context) {
            try {
                AudioManager audioManager = (AudioManager) context.getSystemService("audio");
                if (audioManager == null || audioManager.getRingerMode() != 2 || Settings.System.getInt(context.getContentResolver(), "sound_effects_enabled", 1) == 0) {
                    return;
                }
                if (this.sound == null) {
                    this.sound = TimeDial.woodenClick();
                }
                this.sound.stop();
                this.sound.reloadStaticData();
                this.sound.play();
            } catch (RuntimeException unused) {
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void restored_close_2() {
            AudioTrack audioTrack = this.sound;
            if (audioTrack != null) {
                audioTrack.release();
                this.sound = null;
            }
        }

        void close() {
            this.worker.execute(new Runnable() { // from class: cat.ttcuz.framework.TimeDial$SoundSession$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    TimeDial.SoundSession.this.restored_close_2();
                }
            });
            this.worker.shutdown();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static AudioTrack woodenClick() {
        short[] sArr = new short[617];
        for (int i = 0; i < 617; i++) {
            double t = i / 22050.0d, d = -t;
            sArr[i] = (short) (int) ((Math.sin(6157.5216010359945d * t) + (Math.sin(10430.087609918113d * t) * 0.35d)) * (1.0d - Math.exp(1800.0d * d)) * Math.exp(d * 185.0d) * 0.12d * 32767.0d);
        }
        AudioTrack build = new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(13).setContentType(4).build()).setAudioFormat(new AudioFormat.Builder().setEncoding(2).setSampleRate(22050).setChannelMask(4).build()).setBufferSizeInBytes(1234).setTransferMode(0).build();
        build.write(sArr, 0, 617);
        return build;
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.sounds = new SoundSession();
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        removeCallbacks(this.advance);
        this.dial.stop();
        SoundSession soundSession = this.sounds;
        this.sounds = null;
        if (soundSession != null) {
            soundSession.close();
        }
        super.onDetachedFromWindow();
    }

    /* JADX INFO: Access modifiers changed from: private */
    final class Dial extends View {
        private float angle;
        private boolean dragging;
        private ValueAnimator fade;
        private final Runnable frame;
        private final String[] hourLabels;
        private final String[] innerLabels = new String[12];
        private float labelAlpha;
        private final float[] labelX;
        private final float[] labelY;
        private final String[] minuteLabels;
        private boolean minuteLabelsShown;
        private final Paint paint;
        private long previous;
        private boolean running;
        private float target;
        private float velocity;

        Dial(Context context) {
            super(context);
            Paint paint = new Paint(1);
            this.paint = paint;
            this.hourLabels = new String[12];
            this.minuteLabels = new String[12];
            this.labelX = new float[12];
            this.labelY = new float[12];
            float f = (TimeDial.this.hour % 12) * 30;
            this.angle = f;
            this.target = f;
            this.labelAlpha = 1.0f;
            this.frame = new Runnable() { // from class: cat.ttcuz.framework.TimeDial$Dial$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    TimeDial.Dial.this.step();
                }
            };
            setFocusable(true);
            setClickable(true);
            paint.setTypeface(Typeface.create("sans-serif-medium", 0));
            paint.setTextAlign(Paint.Align.CENTER);
            int i = 0;
            while (i < 12) {
                double d = (i * 3.141592653589793d) / 6.0d;
                this.labelX[i] = ((float) Math.sin(d)) * 88.0f;
                this.labelY[i] = ((float) Math.cos(d)) * (-88.0f);
                this.hourLabels[i] = Integer.toString(i == 0 ? 12 : i);
                this.minuteLabels[i] = String.format(Locale.ROOT, "%02d", Integer.valueOf(i * 5));
                this.innerLabels[i] = Integer.toString(i + 12);
                i++;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void go() {
            this.target = TimeDial.nearestAngle(this.angle, TimeDial.this.minuteMode ? TimeDial.this.minute * 6 : (TimeDial.this.hour % 12) * 30);
            if (!Motion.enabled(getContext())) {
                stop();
                this.angle = this.target;
                this.velocity = 0.0f;
                invalidate();
                return;
            }
            if (this.running) {
                return;
            }
            this.running = true;
            this.previous = System.nanoTime();
            postOnAnimation(this.frame);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void step() {
            long nanoTime = System.nanoTime();
            float min = Math.min(0.05f, (nanoTime - this.previous) / 1.0E9f);
            this.previous = nanoTime;
            while (min > 0.0f) {
                float min2 = Math.min(min, 0.008333334f);
                float f = this.velocity;
                float f2 = this.angle;
                float f3 = f + ((((f2 - this.target) * (-300.0f)) - (26.0f * f)) * min2);
                this.velocity = f3;
                this.angle = f2 + (f3 * min2);
                min -= min2;
            }
            if (Math.abs(this.angle - this.target) < 0.001f && Math.abs(this.velocity) < 0.01f) {
                this.angle = this.target;
                this.velocity = 0.0f;
                this.running = false;
            }
            invalidate();
            if (this.running) {
                postOnAnimation(this.frame);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void stop() {
            this.running = false;
            removeCallbacks(this.frame);
            ValueAnimator valueAnimator = this.fade;
            if (valueAnimator != null) {
                valueAnimator.cancel();
            }
            this.labelAlpha = 1.0f;
            this.minuteLabelsShown = TimeDial.this.minuteMode;
        }

        @Override // android.view.View
        protected void onWindowVisibilityChanged(int i) {
            super.onWindowVisibilityChanged(i);
            if (i != 0) {
                stop();
                this.angle = this.target;
                this.velocity = 0.0f;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void labels() {
            ValueAnimator valueAnimator = this.fade;
            if (valueAnimator != null) {
                valueAnimator.cancel();
            }
            if (!Motion.enabled(getContext())) {
                this.minuteLabelsShown = TimeDial.this.minuteMode;
                invalidate();
                return;
            }
            ValueAnimator ofFloat = ValueAnimator.ofFloat(1.0f, 0.0f, 1.0f);
            this.fade = ofFloat;
            ofFloat.setDuration(280L);
            this.fade.setInterpolator(new LinearInterpolator());
            this.fade.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: cat.ttcuz.framework.TimeDial$Dial$$ExternalSyntheticLambda0
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                    TimeDial.Dial.this.restored_labels_0(valueAnimator2);
                }
            });
            this.fade.start();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void restored_labels_0(ValueAnimator valueAnimator) {
            float interpolation;
            float animatedFraction = valueAnimator.getAnimatedFraction();
            if (animatedFraction < 0.5f) {
                interpolation = 1.0f - TimeDial.COLOUR_EASE.getInterpolation(animatedFraction * 2.0f);
            } else {
                interpolation = TimeDial.COLOUR_EASE.getInterpolation((animatedFraction - 0.5f) * 2.0f);
            }
            this.labelAlpha = interpolation;
            if (valueAnimator.getAnimatedFraction() >= 0.5f) {
                this.minuteLabelsShown = TimeDial.this.minuteMode;
            }
            invalidate();
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            boolean twoRings = TimeDial.this.h24 && !this.minuteLabelsShown;
            boolean inner = twoRings && !TimeDial.this.minuteMode && TimeDial.this.hour >= 12;
            float reach = inner ? 56.0f : 88.0f;
            float min = Math.min(getWidth(), getHeight()) / 240.0f;
            int save = canvas.save();
            canvas.translate(getWidth() / 2.0f, getHeight() / 2.0f);
            canvas.scale(min, min);
            this.paint.setColor(Expressive.mix(TimeDial.this.skin.card, TimeDial.this.skin.text, 0.08f));
            canvas.drawCircle(0.0f, 0.0f, 112.0f, this.paint);
            double radians = Math.toRadians(this.angle);
            float sin = (float) Math.sin(radians);
            float f = -((float) Math.cos(radians));
            int colour = Accent.colour();
            this.paint.setColor(colour);
            this.paint.setStrokeWidth(2.0f);
            canvas.drawLine(0.0f, 0.0f, sin * (reach - 20.0f), f * (reach - 20.0f), this.paint);
            canvas.drawCircle(0.0f, 0.0f, 3.0f, this.paint);
            canvas.drawCircle(sin * reach, f * reach, 20.0f, this.paint);
            if (TimeDial.this.minuteMode && TimeDial.this.minute % 5 != 0) {
                // a minute between the labels: a small dot in the selector shows the exact place
                this.paint.setColor((((Color.red(colour) * 299) + (Color.green(colour) * 587)) + (Color.blue(colour) * 114)) / 1000 > 150 ? 0xFF111111 : 0xFFFFFFFF);
                canvas.drawCircle(sin * reach, f * reach, 3.0f, this.paint);
                this.paint.setColor(colour);
            }
            this.paint.setTextSize(16.0f);
            float f2 = (-(this.paint.ascent() + this.paint.descent())) / 2.0f;
            int i = (((Color.red(colour) * 299) + (Color.green(colour) * 587)) + (Color.blue(colour) * 114)) / 1000 > 150 ? -15658735 : -1;
            for (int i2 = 0; i2 < 12; i2++) {
                boolean under = Math.abs(((((this.angle - ((float) (i2 * 30))) % 360.0f) + 540.0f) % 360.0f) - 180.0f) < 14.0f;
                this.paint.setTextSize(16.0f);
                this.paint.setColor(under && !inner ? i : TimeDial.this.skin.text);
                this.paint.setAlpha(Math.round(this.labelAlpha * 255.0f));
                String outer = this.minuteLabelsShown ? this.minuteLabels[i2]
                        : twoRings ? String.format(Locale.ROOT, "%02d", Integer.valueOf(i2)) : this.hourLabels[i2];
                canvas.drawText(outer, this.labelX[i2], this.labelY[i2] + f2, this.paint);
                if (twoRings) {
                    // the inner ring: 12..23, smaller and quieter, as in Material's 24-hour dial
                    this.paint.setTextSize(13.0f);
                    float f3 = (-(this.paint.ascent() + this.paint.descent())) / 2.0f;
                    this.paint.setColor(under && inner ? i : Expressive.mix(TimeDial.this.skin.text, TimeDial.this.skin.card, 0.25f));
                    this.paint.setAlpha(Math.round(this.labelAlpha * 255.0f));
                    canvas.drawText(this.innerLabels[i2], this.labelX[i2] * 56.0f / 88.0f, this.labelY[i2] * 56.0f / 88.0f + f3, this.paint);
                }
            }
            this.paint.setAlpha(255);
            canvas.restoreToCount(save);
        }

        private void position(MotionEvent motionEvent) {
            float dx = motionEvent.getX() - (getWidth() / 2.0f), dy = motionEvent.getY() - (getHeight() / 2.0f);
            double degrees = (Math.toDegrees(Math.atan2(dx, -dy)) + 360.0d) % 360.0d;
            TimeDial timeDial = TimeDial.this;
            if (timeDial.h24 && !timeDial.minuteMode) {
                // nearer the centre than halfway between the rings: the inner ring
                float scale = Math.min(getWidth(), getHeight()) / 240.0f;
                timeDial.select(TimeDial.hourAtAngle(degrees, Math.hypot(dx, dy) < 72.0f * scale));
                return;
            }
            timeDial.select(TimeDial.valueAtAngle(degrees, timeDial.minuteMode));
        }

        @Override // android.view.View
        public boolean onTouchEvent(MotionEvent motionEvent) {
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked == 0) {
                removeCallbacks(TimeDial.this.advance);
                TimeDial timeDial = TimeDial.this;
                timeDial.removeCallbacks(timeDial.advance);
                this.dragging = true;
                getParent().requestDisallowInterceptTouchEvent(true);
                position(motionEvent);
                return true;
            }
            if (actionMasked != 1) {
                if (actionMasked == 2) {
                    if (this.dragging) {
                        position(motionEvent);
                    }
                    return true;
                }
                if (actionMasked == 3) {
                    this.dragging = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
                return super.onTouchEvent(motionEvent);
            }
            if (this.dragging) {
                position(motionEvent);
                this.dragging = false;
                performClick();
                if (!TimeDial.this.minuteMode) {
                    TimeDial timeDial2 = TimeDial.this;
                    timeDial2.postDelayed(timeDial2.advance, 220L);
                }
            }
            getParent().requestDisallowInterceptTouchEvent(false);
            return true;
        }

        @Override // android.view.View
        public boolean performClick() {
            super.performClick();
            return true;
        }

        @Override // android.view.View, android.view.KeyEvent.Callback
        public boolean onKeyDown(int i, KeyEvent keyEvent) {
            int i2 = (i == 19 || i == 22) ? 1 : (i == 20 || i == 21) ? -1 : 0;
            if (i2 != 0) {
                TimeDial timeDial = TimeDial.this;
                timeDial.select((timeDial.minuteMode ? TimeDial.this.minute : TimeDial.this.hour) + i2);
                return true;
            }
            return super.onKeyDown(i, keyEvent);
        }

        @Override // android.view.View
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
            super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
            accessibilityNodeInfo.setClassName("android.widget.SeekBar");
            accessibilityNodeInfo.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(0, TimeDial.this.minuteMode || TimeDial.this.h24 ? 0.0f : 1.0f, TimeDial.this.minuteMode ? 59.0f : TimeDial.this.h24 ? 23.0f : 12.0f, TimeDial.this.minuteMode ? TimeDial.this.minute : TimeDial.this.hour));
            accessibilityNodeInfo.addAction(4096);
            accessibilityNodeInfo.addAction(8192);
        }

        @Override // android.view.View
        public boolean performAccessibilityAction(int i, Bundle bundle) {
            if (i == 4096 || i == 8192) {
                TimeDial timeDial = TimeDial.this;
                timeDial.select((timeDial.minuteMode ? TimeDial.this.minute : TimeDial.this.hour) + (i == 4096 ? 1 : -1));
                return true;
            }
            return super.performAccessibilityAction(i, bundle);
        }
    }
}
