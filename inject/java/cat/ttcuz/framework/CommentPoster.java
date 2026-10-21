package cat.ttcuz.framework;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/** One cached opening frame; decoding and disk access never run in a drawing callback. */
final class CommentPoster {

    private static File source, pending;
    private static Bitmap frame;
    private static final ArrayList<WeakReference<View>> waiting = new ArrayList<>();

    static synchronized Bitmap get(File file) {
        return file.equals(source) ? frame : null;
    }

    static synchronized void warm(File file, View view) {
        if (file == null || file.equals(source)) return;
        if (view != null) waiting.add(new WeakReference<>(view));
        if (file.equals(pending)) return;
        pending = file;
        Net.away("comment opening frame", () -> {
            Bitmap loaded = read(file);
            ArrayList<WeakReference<View>> notify;
            synchronized (CommentPoster.class) {
                if (!file.equals(pending)) return;
                pending = null;
                source = file;
                frame = loaded;
                notify = new ArrayList<>(waiting);
                waiting.clear();
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                for (WeakReference<View> ref : notify) {
                    View target = ref.get();
                    if (target != null && target.isAttachedToWindow()) target.invalidate();
                }
            });
        });
    }

    private static int sample(int width, int height, int edge) {
        int value = 1;
        while (Math.max(width, height) / value > edge) value *= 2;
        return value;
    }

    private static Bitmap read(File file) {
        File saved = new File(file.getPath() + ".preview.jpg");
        if (saved.isFile()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(saved.getPath(), options);
            options.inSampleSize = sample(
                options.outWidth,
                options.outHeight,
                DevicePerformance.previewEdge()
            );
            options.inJustDecodeBounds = false;
            Bitmap known;
            try {
                known = BitmapFactory.decodeFile(saved.getPath(), options);
            } catch (OutOfMemoryError unavailable) {
                return null;
            }
            if (known != null) return known;
        }
        MediaMetadataRetriever reader = new MediaMetadataRetriever();
        try {
            reader.setDataSource(file.getPath());
            int w = Integer.parseInt(
                reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            );
            int h = Integer.parseInt(
                reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            );
            int edge = DevicePerformance.previewEdge();
            // Older retrievers cannot request a scaled frame before allocating it.
            if (Build.VERSION.SDK_INT < 27 && (long) w * h > 2097152L) return null;
            float scale = Math.min(1f, edge / (float) Math.max(w, h));
            Bitmap decoded =
                Build.VERSION.SDK_INT >= 27
                    ? reader.getScaledFrameAtTime(
                          0,
                          MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                          Math.max(1, Math.round(w * scale)),
                          Math.max(1, Math.round(h * scale))
                      )
                    : reader.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (decoded != null && Math.max(decoded.getWidth(), decoded.getHeight()) > edge) {
                float fit = edge / (float) Math.max(decoded.getWidth(), decoded.getHeight());
                Bitmap small = Bitmap.createScaledBitmap(
                    decoded,
                    Math.max(1, Math.round(decoded.getWidth() * fit)),
                    Math.max(1, Math.round(decoded.getHeight() * fit)),
                    true
                );
                if (small != decoded) decoded.recycle();
                decoded = small;
            }
            if (decoded != null) {
                try (FileOutputStream out = new FileOutputStream(saved)) {
                    decoded.compress(Bitmap.CompressFormat.JPEG, 85, out);
                } catch (Exception ignored) {
                    // A full cache directory must not discard the in-memory opening frame.
                }
            }
            return decoded;
        } catch (Exception | OutOfMemoryError error) {
            Diary.note("comment opening frame: " + error.getClass().getSimpleName());
            return null;
        } finally {
            try {
                reader.release();
            } catch (Exception ignored) {}
        }
    }

    static synchronized void discard(File file) {
        if (file == null) return;
        new File(file.getPath() + ".preview.jpg").delete();
        if (file.equals(source)) {
            source = null;
            frame = null;
        }
        if (file.equals(pending)) {
            pending = null;
            waiting.clear();
        }
    }
}
