package cat.ttcuz.framework;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.audiofx.Visualizer;
import android.os.Build;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * The phone taps along with the music: the sound TikTok plays is read as a spectrum (Android's Visualizer on the
 * output, which is why Android asks about the microphone once; the microphone itself is never opened), and each
 * kick of the bass, or each beat of the whole mix, is a short vibration as strong as the beat.
 *
 * It runs only while TikTok is on screen, never in the camera (it would shake the recording), and is silent when
 * the media volume is at zero.
 */
final class BeatHaptics {
    private BeatHaptics() {}

    static final String KEY = "beat_haptics", MODE = "beat_haptics_mode", POWER = "beat_haptics_power";
    static final int BASS = 0, RHYTHM = 1;
    static final String[] MODES = {"Басы", "Весь ритм"};
    static final String[] POWERS = {"Слабо", "Средне", "Сильно"};
    private static final int[] LENGTH = {14, 22, 32}, TOP = {110, 190, 255};

    private static volatile Boolean on;
    private static volatile int mode = -1, power = -1;

    // main thread only
    private static Visualizer visualizer;
    private static boolean camera;
    private static final Detector detector = new Detector();
    private static float[] previous;
    private static Vibrator vibrator;
    private static AudioManager audio;

    private static SharedPreferences prefs() {
        Context context = Ttcuz.context();
        return context == null ? null : context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
    }

    static boolean on() {
        Boolean known = on;
        if (known != null) return known;
        SharedPreferences p = prefs();
        boolean value = p != null && p.getBoolean(KEY, false);
        on = value;
        return value;
    }

    static void set(boolean value) {
        on = value;
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean(KEY, value).apply();
        update();
    }

    static int mode() {
        if (mode >= 0) return mode;
        SharedPreferences p = prefs();
        mode = p == null ? BASS : Math.max(0, Math.min(1, p.getInt(MODE, BASS)));
        return mode;
    }

    static void setMode(int value) {
        mode = Math.max(0, Math.min(1, value));
        SharedPreferences p = prefs();
        if (p != null) p.edit().putInt(MODE, mode).apply();
        detector.reset(mode);
        previous = null;
    }

    static int power() {
        if (power >= 0) return power;
        SharedPreferences p = prefs();
        power = p == null ? 1 : Math.max(0, Math.min(2, p.getInt(POWER, 1)));
        return power;
    }

    static void setPower(int value) {
        power = Math.max(0, Math.min(2, value));
        SharedPreferences p = prefs();
        if (p != null) p.edit().putInt(POWER, power).apply();
    }

    /** The output can be read only with the "record audio" permission granted. */
    static boolean allowed(Context context) {
        return context != null && context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    /** A screen came on top: the camera screens get no vibration. */
    static void screen(Activity activity) {
        camera = Dim.creationActivity(activity.getClass().getName().toLowerCase(java.util.Locale.ROOT));
        update();
    }

    /** Listening starts or stops to match: on, TikTok on screen, not the camera, permission given. */
    static void update() {
        Context context = Ttcuz.context();
        boolean wanted = on() && Visits.visible() && !camera && allowed(context);
        if (wanted == (visualizer != null)) return;
        if (wanted) begin(); else end();
    }

    /**
     * The Visualizer hands out its newest 1024 samples (about 21 ms) whenever asked; its own callback asks 20 times a
     * second and so hears less than half of the music, missing many kicks. Here it is asked every 20 ms, on a thread of
     * its own, so every hit is heard; the thread is also where the Visualizer is let go.
     */
    private static android.os.HandlerThread thread;
    private static android.os.Handler beat;

    private static void begin() {
        Visualizer made = null;
        try {
            made = new Visualizer(0);
            made.setEnabled(false);
            int[] range = Visualizer.getCaptureSizeRange();
            made.setCaptureSize(Math.max(range[0], Math.min(range[1], 1024)));
            made.setScalingMode(Visualizer.SCALING_MODE_NORMALIZED);
            made.setEnabled(true);
            visualizer = made;
            detector.reset(mode());
            previous = null;
            thread = new android.os.HandlerThread("ttcuz-beat");
            thread.start();
            beat = new android.os.Handler(thread.getLooper());
            final Visualizer listening = made;
            final byte[] fft = new byte[listening.getCaptureSize()];
            final int rate = listening.getSamplingRate();
            final android.os.Handler reading = beat;
            reading.post(new Runnable() {
                @Override public void run() {
                    if (visualizer != listening) return;
                    try {
                        if (listening.getFft(fft) == Visualizer.SUCCESS) {
                            float energy = mode() == BASS ? bass(fft, rate) : flux(fft, rate);
                            float strength = detector.feed(energy, SystemClock.uptimeMillis());
                            if (strength > 0) buzz(strength);
                        }
                    } catch (Throwable error) { Diary.note("beat read: " + error); return; }
                    reading.postDelayed(this, 20L);
                }
            });
        } catch (Throwable error) {
            if (made != null) try { made.release(); } catch (Throwable ignored) {}
            visualizer = null;
            Diary.note("beat haptics: " + error);
        }
    }

    private static void end() {
        final Visualizer had = visualizer;
        visualizer = null;
        final android.os.HandlerThread worker = thread;
        android.os.Handler on = beat;
        thread = null;
        beat = null;
        Runnable letGo = () -> {
            if (had != null) {
                try { had.setEnabled(false); } catch (Throwable ignored) {}
                try { had.release(); } catch (Throwable ignored) {}
            }
            if (worker != null) worker.quitSafely();
        };
        // on the reading thread, after its last read: never released under a read in progress
        if (on != null) on.post(letGo); else letGo.run();
    }

    /** Energy between 30 and 180 Hz: the kick and the bass line. `rate` is in millihertz, as the Visualizer gives it. */
    static float bass(byte[] fft, int rate) {
        int n = fft.length;
        float bin = rate / 1000f / n, energy = 0;
        for (int k = 1; k < n / 2; k++) {
            float hz = k * bin;
            if (hz < 30) continue;
            if (hz > 180) break;
            float re = fft[2 * k], im = fft[2 * k + 1];
            energy += re * re + im * im;
        }
        return energy;
    }

    /** How much louder the mix got since the last frame, up to 6 kHz (spectral flux): every hit, not only the bass. */
    static float flux(byte[] fft, int rate) {
        int n = fft.length;
        float bin = rate / 1000f / n;
        int top = Math.min(n / 2 - 1, bin > 0 ? (int) (6000 / bin) : n / 2 - 1);
        float[] now = new float[top + 1];
        float flux = 0;
        for (int k = 1; k <= top; k++) {
            float re = fft[2 * k], im = fft[2 * k + 1];
            now[k] = (float) Math.sqrt(re * re + im * im);
            if (previous != null && previous.length == now.length) flux += Math.max(0, now[k] - previous[k]);
        }
        previous = now;
        return flux;
    }

    private static void buzz(float strength) {
        try {
            Context context = Ttcuz.context();
            if (context == null) return;
            if (audio == null) audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            // nothing heard, nothing felt
            if (audio != null && audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) return;
            if (vibrator == null) {
                vibrator = Build.VERSION.SDK_INT >= 31
                        ? ((android.os.VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator()
                        : (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (vibrator == null || !vibrator.hasVibrator()) return;
            int p = power();
            VibrationEffect effect = vibrator.hasAmplitudeControl()
                    ? VibrationEffect.createOneShot(LENGTH[p], Math.max(1, Math.min(255, Math.round(TOP[p] * (.35f + .65f * strength)))))
                    // a motor without strength steps: a stronger beat is a longer tap
                    : VibrationEffect.createOneShot(Math.max(8, Math.round(LENGTH[p] * (.5f + .5f * strength))), VibrationEffect.DEFAULT_AMPLITUDE);
            vibrator.vibrate(effect);
        } catch (Throwable error) {
            Diary.note("beat buzz: " + error);
            // a phone that refuses vibration: the feature turns itself off (on the main thread, where it is run)
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> set(false));
        }
    }

    /**
     * A beat is a read above the last second (mean plus 0.8 deviations, and at least 15% above the mean), rising,
     * over a floor that silence never reaches, and at least 110 ms after the last one. Its strength (0..1)
     * is how far above that line it is; 0 is no beat.
     */
    static final class Detector {
        private final float[] past = new float[48];
        private int count, at;
        private float before, floor = 150;
        private long lastBeat = -1000;

        void reset(int mode) {
            count = 0;
            at = 0;
            before = 0;
            lastBeat = -1000;
            floor = mode == BASS ? 150 : 30;
        }

        float feed(float energy, long now) {
            int n = Math.min(count, past.length);
            float mean = 0;
            for (int i = 0; i < n; i++) mean += past[i];
            mean = n > 0 ? mean / n : 0;
            float spread = 0;
            for (int i = 0; i < n; i++) spread += (past[i] - mean) * (past[i] - mean);
            spread = n > 0 ? (float) Math.sqrt(spread / n) : 0;
            past[at] = energy;
            at = (at + 1) % past.length;
            count++;
            float rising = before;
            before = energy;
            if (n < 12 || energy < floor || energy <= rising) return 0;
            float line = mean + Math.max(.8f * spread, .15f * mean);
            if (energy < line || now - lastBeat < 110) return 0;
            lastBeat = now;
            return Math.min(1f, .4f + .6f * (energy - line) / (line + 1e-3f));
        }
    }
}
