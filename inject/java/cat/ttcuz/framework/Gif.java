package cat.ttcuz.framework;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.SystemClock;

/**
 * Animated pictures for badges and plugin icons: GIFs read frame by frame, videos turned into GIFs, and a small
 * drawable that plays them.
 *
 * Kept cheap on purpose. A GIF is cut into at most {@link #MAX_FRAMES} frames once, already at the size it is drawn
 * at, so playing it is one bitmap per frame and nothing is decoded while scrolling. The drawable asks for its next
 * frame only after it was drawn: a badge that is off screen is not drawn, so it stops by itself, with no timer
 * left running anywhere.
 */
public final class Gif {
    private Gif() {}

    static final int MAX_FRAMES = 30;
    /** about 15 frames a second: smooth for a badge, half the work of 30 */
    static final int FRAME_MS = 66;

    /** Frames of one animation, all the same size, each shown for frameMs. */
    public static final class Frames {
        public final Bitmap[] frames;
        public final int frameMs;
        Frames(Bitmap[] frames, int frameMs) { this.frames = frames; this.frameMs = Math.max(20, frameMs); }
        public int width() { return frames[0].getWidth(); }
        public int height() { return frames[0].getHeight(); }
        int bytes() { int n = 0; for (Bitmap b : frames) n += b.getByteCount(); return n; }
    }

    public static boolean isGif(byte[] b) {
        return b != null && b.length > 13 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8';
    }

    // ------------------------------------------------------------------ reading

    /**
     * A GIF's frames, cropped to a square (cropX/cropY 0..100 place the window, zoom ≥ 100 shrinks it) and scaled to
     * size × size; size 0 keeps the GIF's own size (no crop). Null when it is not a GIF Android can read.
     */
    @SuppressWarnings("deprecation")
    public static Frames decode(byte[] gif, int size, int cropX, int cropY, int zoom) {
        if (!isGif(gif)) return null;
        android.graphics.Movie movie = android.graphics.Movie.decodeByteArray(gif, 0, gif.length);
        if (movie == null || movie.width() <= 0 || movie.height() <= 0 || (long) movie.width() * movie.height() > 4_000_000L) return null;
        int duration = movie.duration();
        int count = duration <= 0 ? 1 : Math.max(1, Math.min(MAX_FRAMES, duration / FRAME_MS));
        int step = duration <= 0 ? 100 : Math.max(20, duration / count);
        Bitmap canvasBitmap = Bitmap.createBitmap(movie.width(), movie.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(canvasBitmap);
        Bitmap[] out = new Bitmap[count];
        for (int i = 0; i < count; i++) {
            canvasBitmap.eraseColor(0);
            movie.setTime(Math.min(Math.max(0, duration - 1), i * step));
            movie.draw(canvas, 0, 0);
            out[i] = size <= 0 ? canvasBitmap.copy(Bitmap.Config.ARGB_8888, false) : square(canvasBitmap, size, cropX, cropY, zoom);
        }
        canvasBitmap.recycle();
        return new Frames(out, step);
    }

    /** The first few seconds of a video as frames, cropped and scaled like {@link #decode}. */
    public static Frames fromVideo(Context context, Uri uri, int size, int cropX, int cropY, int zoom, int maxMs) {
        android.media.MediaMetadataRetriever reader = new android.media.MediaMetadataRetriever();
        try {
            reader.setDataSource(context, uri);
            long length = 0;
            try { length = Long.parseLong(reader.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)); } catch (Throwable ignored) { }
            long span = Math.max(FRAME_MS, Math.min(maxMs, length > 0 ? length : maxMs));
            int count = (int) Math.max(1, Math.min(MAX_FRAMES, span / FRAME_MS));
            int step = (int) (span / count);
            java.util.List<Bitmap> out = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                long at = (long) i * step * 1000L;
                Bitmap frame;
                if (android.os.Build.VERSION.SDK_INT >= 27) frame = reader.getScaledFrameAtTime(at, android.media.MediaMetadataRetriever.OPTION_CLOSEST, 480, 480);
                else frame = reader.getFrameAtTime(at, android.media.MediaMetadataRetriever.OPTION_CLOSEST);
                if (frame == null) continue;
                // size 0 keeps the video's own frame (at most 480 a side), as decode() keeps a GIF's
                out.add(size <= 0 ? frame.copy(Bitmap.Config.ARGB_8888, false) : square(frame, size, cropX, cropY, zoom));
                frame.recycle();
            }
            return out.isEmpty() ? null : new Frames(out.toArray(new Bitmap[0]), step);
        } catch (Throwable error) {
            Diary.note("gif from video: " + error);
            return null;
        } finally {
            try { reader.release(); } catch (Throwable ignored) { }
        }
    }

    /** The same square window as the badge's still picture, scaled to size. */
    static Bitmap square(Bitmap source, int size, int cropX, int cropY, int zoom) {
        int side = Math.min(source.getWidth(), source.getHeight());
        int window = Math.max(1, side * 100 / Math.max(100, zoom));
        int left = (source.getWidth() - window) * Math.max(0, Math.min(100, cropX)) / 100;
        int top = (source.getHeight() - window) * Math.max(0, Math.min(100, cropY)) / 100;
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        new Canvas(out).drawBitmap(source, new Rect(left, top, left + window, top + window), new Rect(0, 0, size, size),
                new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));
        return out;
    }

    // ------------------------------------------------------------------ writing

    /**
     * Frames → a looping GIF of at most maxBytes. If it comes out larger, it is written again with every other frame
     * (each shown twice as long), then smaller, until it fits; null when even that does not.
     */
    public static byte[] encode(Frames frames, int maxBytes) {
        Frames current = frames;
        for (int attempt = 0; attempt < 5 && current != null; attempt++) {
            byte[] out = write(current);
            if (out.length <= maxBytes) return out;
            current = attempt % 2 == 0 ? halveFrames(current) : shrink(current, .8f);
        }
        return null;
    }

    private static Frames halveFrames(Frames f) {
        if (f.frames.length < 4) return shrink(f, .8f);
        Bitmap[] half = new Bitmap[(f.frames.length + 1) / 2];
        for (int i = 0; i < half.length; i++) half[i] = f.frames[i * 2];
        return new Frames(half, f.frameMs * 2);
    }

    private static Frames shrink(Frames f, float k) {
        int w = Math.max(16, Math.round(f.width() * k)), h = Math.max(16, Math.round(f.height() * k));
        Bitmap[] out = new Bitmap[f.frames.length];
        for (int i = 0; i < out.length; i++) out[i] = Bitmap.createScaledBitmap(f.frames[i], w, h, true);
        return new Frames(out, f.frameMs);
    }

    private static byte[] write(Frames f) {
        int w = f.width(), h = f.height();
        int[][] pixels = new int[f.frames.length][w * h];
        for (int i = 0; i < pixels.length; i++) f.frames[i].getPixels(pixels[i], 0, w, 0, 0, w, h);
        return GifWriter.write(pixels, w, h, f.frameMs);
    }

    // ------------------------------------------------------------------ playing

    /**
     * Plays frames inside its bounds. The next frame is asked for only from draw(): nothing runs while it is not
     * on screen. In a TextView (a badge in a nickname) the view is told directly, since a span's drawable has no
     * callback of its own.
     */
    public static final class Player extends Drawable implements Runnable {
        final Frames frames;
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private Callback held; // Drawable keeps its callback weakly; this keeps the TextView's one alive
        private int alpha = 255;

        public Player(Frames frames) { this.frames = frames; }

        /** For a drawable inside a TextView's text: redraw that view when the next frame is due. */
        public Player in(final android.widget.TextView view) {
            final java.lang.ref.WeakReference<android.widget.TextView> target = new java.lang.ref.WeakReference<>(view);
            held = new Callback() {
                @Override public void invalidateDrawable(Drawable who) { android.view.View v = target.get(); if (v != null) v.invalidate(); }
                @Override public void scheduleDrawable(Drawable who, Runnable what, long when) { android.view.View v = target.get(); if (v != null) v.postDelayed(what, Math.max(0, when - SystemClock.uptimeMillis())); }
                @Override public void unscheduleDrawable(Drawable who, Runnable what) { android.view.View v = target.get(); if (v != null) v.removeCallbacks(what); }
            };
            setCallback(held);
            return this;
        }

        private boolean scheduled;

        @Override public void draw(Canvas canvas) {
            int n = frames.frames.length;
            long now = SystemClock.uptimeMillis();
            int index = (int) ((now / frames.frameMs) % n);
            Bitmap frame = frames.frames[index];
            if (!frame.isRecycled()) canvas.drawBitmap(frame, null, getBounds(), paint);
            if (n > 1 && !scheduled && Motion.enabled(null)) {
                scheduled = true;
                scheduleSelf(this, now + frames.frameMs - now % frames.frameMs);
            }
        }

        @Override public void run() { scheduled = false; invalidateSelf(); }
        @Override public void setAlpha(int a) { alpha = a; paint.setAlpha(a); }
        @Override public int getAlpha() { return alpha; }
        @Override public void setColorFilter(ColorFilter f) { paint.setColorFilter(f); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        @Override public int getIntrinsicWidth() { return frames.width(); }
        @Override public int getIntrinsicHeight() { return frames.height(); }
    }
}
