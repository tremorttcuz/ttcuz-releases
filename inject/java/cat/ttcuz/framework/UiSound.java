package cat.ttcuz.framework;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/**
 * The mod's own small sounds: a dry bony click on a tap, and a double knock -- "clack-clack" -- when a page opens.
 * Made here from a few decaying tones and a hair of noise rather than shipped as files; each is a few thousand samples,
 * played from a static track kept for reuse. They play at the media volume, as the rest of TikTok's sound does,
 * whatever the ringer is set to; switched off in the settings, they are not played at all.
 */
final class UiSound {
    private UiSound() {}

    static final String KEY = "ui_sounds";
    private static final int RATE = 44100;
    private static volatile Boolean on;
    private static AudioTrack tap, open, unlock;

    static boolean on() {
        Boolean known = on;
        if (known != null) return known;
        Context context = Ttcuz.context();
        known = context == null || context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true);
        on = known;
        return known;
    }

    static void set(boolean value) {
        on = value;
        Context context = Ttcuz.context();
        if (context != null) context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, value).apply();
    }

    private static long lastTap;

    /** One click per tap: the screen and the row under the finger may both ask for it. */
    static void tap() {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastTap < 80L) return;
        lastTap = now;
        play(TAP);
    }
    static void open() { play(OPEN); }
    /** Opened: TikTok, or the chats, after the fingerprint or the code. */
    static void unlocked() { play(UNLOCK); }
    private static final int TAP = 0, OPEN = 1, UNLOCK = 2;

    private static synchronized void play(int which) {
        if (!on()) return;
        try {
            AudioTrack track = which == TAP ? tap : which == OPEN ? open : unlock;
            if (track == null) {
                short[] samples = which == TAP ? bone(0, 2300f, 1.0f) : which == OPEN ? knock() : chime();
                track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                        .setAudioFormat(new AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                        .setBufferSizeInBytes(samples.length * 2).setTransferMode(AudioTrack.MODE_STATIC).build();
                track.write(samples, 0, samples.length);
                if (which == TAP) tap = track; else if (which == OPEN) open = track; else unlock = track;
            }
            if (track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) track.stop();
            track.reloadStaticData();
            track.play();
        } catch (Throwable error) { Diary.note("ui sound: " + error); }
    }

    /**
     * The unlock: minimal, two soft sine notes a fifth apart (E6 then B6), 70 ms each with a gentle attack and a long
     * fade, the second overlapping the first: a small "ding-dong" upward, quiet.
     */
    private static short[] chime() {
        int note = RATE * 260 / 1000, step = RATE * 70 / 1000;
        short[] out = new short[step + note];
        float[] pitch = {1318.5f, 1975.5f};
        for (int k = 0; k < 2; k++) {
            int from = k * step;
            for (int i = 0; i < note; i++) {
                double t = (double) i / RATE;
                double v = Math.sin(2 * Math.PI * pitch[k] * t) * Math.min(1.0, t / .006) * Math.exp(-t / .07) * .22;
                int at = from + i;
                out[at] = (short) Math.max(-32768, Math.min(32767, out[at] + Math.round(v * 32767)));
            }
        }
        return out;
    }

    /** The open sound: two bony clicks 55 ms apart, the second lower, as two knuckles knocking. */
    private static short[] knock() {
        short[] first = bone(0, 2300f, .9f), second = bone(7, 1500f, 1f);
        int gap = RATE * 55 / 1000;
        short[] out = new short[gap + second.length];
        for (int i = 0; i < first.length; i++) out[i] = first[i];
        for (int i = 0; i < second.length; i++) out[gap + i] = (short) Math.max(-32768, Math.min(32767, out[gap + i] + second[i]));
        return out;
    }

    /**
     * One bony click, 30 ms: a hard knock of noise for the first millisecond, then two inharmonic tones (a hollow,
     * woody body, ratio 1 : 2.76 like a struck bar) dying in a few milliseconds. Seeded noise, so it never varies.
     */
    static short[] bone(int seed, float pitch, float loud) {
        int n = RATE * 30 / 1000;
        short[] out = new short[n];
        java.util.Random noise = new java.util.Random(31 + seed);
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE;
            double body = Math.sin(2 * Math.PI * pitch * t) * Math.exp(-t / .0045)
                    + .45 * Math.sin(2 * Math.PI * pitch * 2.76 * t) * Math.exp(-t / .0022);
            double hit = (noise.nextDouble() * 2 - 1) * Math.exp(-t / .0006);
            double attack = Math.min(1.0, i / 20.0);
            out[i] = (short) Math.round(Math.max(-1, Math.min(1, (body * .55 + hit * .35) * attack * loud * .5)) * 32767);
        }
        return out;
    }
}
