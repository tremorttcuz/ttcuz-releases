package cat.ttcuz.framework;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;
import android.view.View;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

final class CommentCadence {
    private static final LinkedHashMap<String, Float> rates = new LinkedHashMap<>();

    CommentCadence() {
    }

    static void request(final File file, final Consumer<Float> consumer) {
        Net.away("comment video cadence", new Runnable() { // from class: cat.ttcuz.framework.CommentCadence$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                consumer.accept(Float.valueOf(CommentCadence.read(file)));
            }
        });
    }

    /** The video track's frame rate: the container's own figure, else measured over up to 120 samples (2 s). */
    static synchronized float read(File file) {
        String key = file.getPath() + ":" + file.length() + ":" + file.lastModified();
        Float known = rates.get(key);
        if (known != null) return known;
        float rate = 0f;
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(file.getPath());
            int track = -1;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString("mime");
                if (mime != null && mime.startsWith("video/")) {
                    track = i;
                    if (format.containsKey("frame-rate")) {
                        try { rate = format.getFloat("frame-rate"); }
                        catch (ClassCastException integer) { rate = format.getInteger("frame-rate"); }
                    }
                    break;
                }
            }
            if (track >= 0 && !valid(rate)) {
                extractor.selectTrack(track);
                long first = extractor.getSampleTime(), last = first;
                int frames = 0;
                while (first >= 0 && frames < 120 && extractor.advance()) {
                    long time = extractor.getSampleTime();
                    if (time <= last) break;
                    frames++;
                    last = time;
                    if (time - first >= 2000000L) break;
                }
                if (frames > 0 && last > first) rate = frames * 1000000f / (last - first);
            }
        } catch (Exception ignored) {
            rate = 0f;
        } finally {
            extractor.release();
        }
        if (!valid(rate)) rate = 0f;
        if (rates.size() >= 8) rates.remove(rates.keySet().iterator().next());
        rates.put(key, rate);
        return rate;
    }

    private static boolean valid(float f) {
        return !Float.isNaN(f) && !Float.isInfinite(f) && f >= 1.0f && f <= 240.0f;
    }

    static void apply(View view, Surface surface, float f) {
        if (f == 0.0f || valid(f)) {
            try {
                if (Build.VERSION.SDK_INT >= 35) {
                    view.setRequestedFrameRate(f == 0.0f ? Float.NaN : f);
                }
                if (surface != null && Build.VERSION.SDK_INT >= 31) {
                    surface.setFrameRate(f, 1, 0);
                } else {
                    if (surface == null || Build.VERSION.SDK_INT < 30) {
                        return;
                    }
                    surface.setFrameRate(f, 1);
                }
            } catch (RuntimeException unused) {
            }
        }
    }
}
