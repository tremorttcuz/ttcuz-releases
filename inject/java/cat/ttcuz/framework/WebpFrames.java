package cat.ttcuz.framework;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * An animated WebP taken apart frame by frame, with the time each frame is shown, and put back together as a GIF.
 *
 * Android decodes a WebP picture but gives no frames of an animated one, so the container is read here: the canvas
 * size (VP8X), then each ANMF chunk -- its place on the canvas, how long it shows, whether it is blended over what is
 * there and whether it is cleared afterwards -- whose image is wrapped as a WebP of its own and decoded by Android.
 * The frames are laid on a timeline of equal steps (a GIF has one delay per frame and the encoder one for all), so a
 * frame that shows twice as long is simply there twice: the speed is the sticker's own.
 */
final class WebpFrames {
    private WebpFrames() {}

    private static final int MOST_SIDE = 320;
    /** Frames kept: fewer on a phone that keeps TikTok to a small heap. */
    private static final int MOST_FRAMES = MemoryGuard.tight() ? 60 : 120;

    static boolean animated(byte[] data) {
        if (data == null || data.length < 30 || !tag(data, 0, "RIFF") || !tag(data, 8, "WEBP") || !tag(data, 12, "VP8X")) return false;
        return (data[20] & 0x02) != 0; // the animation flag of VP8X
    }

    /** The animation as a GIF of at most maxBytes, or null when it could not be read. */
    static byte[] toGif(byte[] data, int maxBytes) {
        try {
            Gif.Frames frames = decode(data);
            return frames == null ? null : Gif.encode(frames, maxBytes);
        } catch (Throwable error) {
            Diary.note("webp frames: " + error);
            return null;
        }
    }

    private static Gif.Frames decode(byte[] data) {
        int canvasW = u24(data, 24) + 1, canvasH = u24(data, 27) + 1;
        if (canvasW <= 0 || canvasH <= 0 || canvasW > 4096 || canvasH > 4096) return null;
        float scale = Math.min(1f, MOST_SIDE / (float) Math.max(canvasW, canvasH));
        int outW = Math.max(1, Math.round(canvasW * scale)), outH = Math.max(1, Math.round(canvasH * scale));

        List<int[]> places = new ArrayList<>();   // x, y, w, h, duration, flags, offset, length
        boolean alphaHeader = (data[20] & 0x10) != 0;
        for (int at = 12; at + 8 <= data.length; ) {
            String kind = new String(data, at, 4, java.nio.charset.StandardCharsets.US_ASCII);
            int size = (int) u32(data, at + 4);
            if (size < 0 || at + 8 + size > data.length) break;
            if (kind.equals("ANMF") && size >= 16) {
                int p = at + 8;
                places.add(new int[]{u24(data, p) * 2, u24(data, p + 3) * 2, u24(data, p + 6) + 1, u24(data, p + 9) + 1,
                        u24(data, p + 12), data[p + 15] & 0xFF, p + 16, size - 16});
                if (places.size() > 600) break;
            }
            at += 8 + size + (size & 1);
        }
        if (places.isEmpty()) return null;

        // the timeline: one step the length of the shortest frame (20 ms at least), at most MOST_FRAMES steps
        int shortest = Integer.MAX_VALUE, total = 0;
        for (int[] f : places) { int d = Math.max(20, f[4]); shortest = Math.min(shortest, d); total += d; }
        int step = Math.max(shortest, (int) Math.ceil(total / (double) MOST_FRAMES));

        Bitmap canvasBitmap = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(canvasBitmap);
        Paint over = new Paint(Paint.FILTER_BITMAP_FLAG), replace = new Paint(Paint.FILTER_BITMAP_FLAG), clear = new Paint();
        replace.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        clear.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        List<Bitmap> out = new ArrayList<>();
        int[] previous = null;
        long clock = 0, emitted = 0;
        for (int[] f : places) {
            if (previous != null && (previous[5] & 0x01) != 0) canvas.drawRect(previous[0], previous[1], previous[0] + previous[2], previous[1] + previous[3], clear);
            Bitmap piece = frame(data, f[6], f[7], f[2], f[3], alphaHeader);
            if (piece != null) {
                Rect to = new Rect(f[0], f[1], f[0] + f[2], f[1] + f[3]);
                canvas.drawBitmap(piece, null, to, (f[5] & 0x02) != 0 ? replace : over);
                piece.recycle();
            }
            previous = f;
            clock += Math.max(20, f[4]);
            // as many steps of the timeline as this frame covers
            while (emitted * step < clock && out.size() < MOST_FRAMES) {
                out.add(scale == 1f ? canvasBitmap.copy(Bitmap.Config.ARGB_8888, false)
                        : Bitmap.createScaledBitmap(canvasBitmap, outW, outH, true));
                emitted++;
            }
            if (out.size() >= MOST_FRAMES) break;
        }
        canvasBitmap.recycle();
        return out.isEmpty() ? null : new Gif.Frames(out.toArray(new Bitmap[0]), step);
    }

    /** One frame's image (ALPH + VP8, or VP8L, or VP8) as a WebP of its own, decoded. */
    private static Bitmap frame(byte[] data, int from, int length, int w, int h, boolean alpha) {
        boolean hasAlpha = length >= 4 && tag(data, from, "ALPH");
        ByteArrayOutputStream body = new ByteArrayOutputStream(length + 32);
        if (hasAlpha) {
            // ALPH needs the extended header that says there is alpha
            body.write('V'); body.write('P'); body.write('8'); body.write('X');
            le32(body, 10);
            body.write(0x10); body.write(0); body.write(0); body.write(0);
            le24(body, w - 1); le24(body, h - 1);
        }
        body.write(data, from, length);
        byte[] inner = body.toByteArray();
        ByteArrayOutputStream file = new ByteArrayOutputStream(inner.length + 12);
        file.write('R'); file.write('I'); file.write('F'); file.write('F');
        le32(file, inner.length + 4);
        file.write('W'); file.write('E'); file.write('B'); file.write('P');
        file.write(inner, 0, inner.length);
        byte[] webp = file.toByteArray();
        return BitmapFactory.decodeByteArray(webp, 0, webp.length);
    }

    private static boolean tag(byte[] data, int at, String name) {
        if (at + 4 > data.length) return false;
        for (int i = 0; i < 4; i++) if (data[at + i] != name.charAt(i)) return false;
        return true;
    }

    private static int u24(byte[] d, int at) { return (d[at] & 0xFF) | (d[at + 1] & 0xFF) << 8 | (d[at + 2] & 0xFF) << 16; }

    private static long u32(byte[] d, int at) { return (d[at] & 0xFFL) | (d[at + 1] & 0xFFL) << 8 | (d[at + 2] & 0xFFL) << 16 | (d[at + 3] & 0xFFL) << 24; }

    private static void le32(ByteArrayOutputStream out, int v) { out.write(v); out.write(v >> 8); out.write(v >> 16); out.write(v >> 24); }

    private static void le24(ByteArrayOutputStream out, int v) { out.write(v); out.write(v >> 8); out.write(v >> 16); }
}
