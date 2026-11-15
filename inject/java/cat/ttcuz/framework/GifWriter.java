package cat.ttcuz.framework;

import java.io.ByteArrayOutputStream;

/**
 * The GIF encoder behind {@link Gif}, plain Java: one palette of up to 255 colours for all the frames (median cut),
 * index 255 transparent, each frame LZW-packed. Kept apart from Android so it can be checked on a computer.
 */
final class GifWriter {
    private GifWriter() {}

    private static final int TRANSPARENT = 255;

    /** ARGB frames, w × h each, shown frameMs each → a GIF89a that loops for ever. */
    static byte[] write(int[][] pixels, int w, int h, int frameMs) {
        int[] palette = palette(pixels);
        int[] map = new int[32768];
        java.util.Arrays.fill(map, -1);
        ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024);
        writeAscii(out, "GIF89a");
        short16(out, w); short16(out, h);
        out.write(0xF7); out.write(TRANSPARENT); out.write(0);
        for (int i = 0; i < 256; i++) { int c = i < palette.length ? palette[i] : 0; out.write((c >> 16) & 255); out.write((c >> 8) & 255); out.write(c & 255); }
        // loop for ever
        out.write(0x21); out.write(0xFF); out.write(11); writeAscii(out, "NETSCAPE2.0"); out.write(3); out.write(1); short16(out, 0); out.write(0);
        int delay = Math.max(2, Math.round(frameMs / 10f));
        byte[] indices = new byte[w * h];
        for (int[] frame : pixels) {
            out.write(0x21); out.write(0xF9); out.write(4); out.write((2 << 2) | 1); short16(out, delay); out.write(TRANSPARENT); out.write(0);
            out.write(0x2C); short16(out, 0); short16(out, 0); short16(out, w); short16(out, h); out.write(0);
            for (int p = 0; p < frame.length; p++) {
                int c = frame[p];
                if ((c >>> 24) < 128) { indices[p] = (byte) TRANSPARENT; continue; }
                int key = ((c >> 9) & 0x7C00) | ((c >> 6) & 0x3E0) | ((c >> 3) & 0x1F);
                int index = map[key];
                if (index < 0) map[key] = index = nearest(palette, key);
                indices[p] = (byte) index;
            }
            out.write(8);
            lzw(indices, out);
        }
        out.write(0x3B);
        return out.toByteArray();
    }

    /** Up to 255 colours for all the frames together: median cut over a 15-bit histogram. */
    private static int[] palette(int[][] frames) {
        int[] counts = new int[32768];
        for (int[] frame : frames) {
            int stride = Math.max(1, frame.length * frames.length / 300_000);
            for (int p = 0; p < frame.length; p += stride) {
                int c = frame[p];
                if ((c >>> 24) < 128) continue;
                counts[((c >> 9) & 0x7C00) | ((c >> 6) & 0x3E0) | ((c >> 3) & 0x1F)]++;
            }
        }
        int used = 0;
        for (int n : counts) if (n > 0) used++;
        int[] colours = new int[Math.max(1, used)];
        int k = 0;
        for (int i = 0; i < counts.length; i++) if (counts[i] > 0) colours[k++] = i;
        if (used == 0) return new int[]{0};
        java.util.List<int[]> boxes = new java.util.ArrayList<>(); // [from, to) into colours
        boxes.add(new int[]{0, used});
        while (boxes.size() < 255) {
            int best = -1, bestScore = 0, bestAxis = 0;
            for (int b = 0; b < boxes.size(); b++) {
                int[] box = boxes.get(b);
                if (box[1] - box[0] < 2) continue;
                int[] range = range(colours, box);
                int axis = range[0] >= range[1] && range[0] >= range[2] ? 0 : range[1] >= range[2] ? 1 : 2;
                int score = range[axis];
                if (score > bestScore) { bestScore = score; best = b; bestAxis = axis; }
            }
            if (best < 0) break;
            int[] box = boxes.get(best);
            final int shift = bestAxis == 0 ? 10 : bestAxis == 1 ? 5 : 0;
            Integer[] part = new Integer[box[1] - box[0]];
            for (int i = 0; i < part.length; i++) part[i] = colours[box[0] + i];
            java.util.Arrays.sort(part, (a, b2) -> ((a >> shift) & 31) - ((b2 >> shift) & 31));
            long total = 0;
            for (Integer c : part) total += counts[c];
            long half = 0;
            int cut = 1;
            for (int i = 0; i < part.length - 1; i++) { half += counts[part[i]]; if (half * 2 >= total) { cut = i + 1; break; } cut = i + 1; }
            for (int i = 0; i < part.length; i++) colours[box[0] + i] = part[i];
            boxes.set(best, new int[]{box[0], box[0] + cut});
            boxes.add(new int[]{box[0] + cut, box[1]});
        }
        int[] palette = new int[boxes.size()];
        for (int b = 0; b < boxes.size(); b++) {
            int[] box = boxes.get(b);
            long r = 0, g = 0, bl = 0, n = 0;
            for (int i = box[0]; i < box[1]; i++) {
                int c = colours[i], weight = counts[c];
                r += (long) ((c >> 10) & 31) * weight; g += (long) ((c >> 5) & 31) * weight; bl += (long) (c & 31) * weight; n += weight;
            }
            n = Math.max(1, n);
            palette[b] = (expand(r / n) << 16) | (expand(g / n) << 8) | expand(bl / n);
        }
        return palette;
    }

    private static int expand(long five) { int v = (int) five; return (v << 3) | (v >> 2); }

    private static int[] range(int[] colours, int[] box) {
        int[] lo = {31, 31, 31}, hi = {0, 0, 0};
        for (int i = box[0]; i < box[1]; i++) {
            int c = colours[i];
            int[] v = {(c >> 10) & 31, (c >> 5) & 31, c & 31};
            for (int a = 0; a < 3; a++) { lo[a] = Math.min(lo[a], v[a]); hi[a] = Math.max(hi[a], v[a]); }
        }
        return new int[]{hi[0] - lo[0], hi[1] - lo[1], hi[2] - lo[2]};
    }

    private static int nearest(int[] palette, int key) {
        int r = expand((key >> 10) & 31), g = expand((key >> 5) & 31), b = expand(key & 31);
        int best = 0, bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < palette.length; i++) {
            int c = palette[i];
            int dr = ((c >> 16) & 255) - r, dg = ((c >> 8) & 255) - g, db = (c & 255) - b;
            // green counts most for the eye
            int d = dr * dr * 3 + dg * dg * 4 + db * db * 2;
            if (d < bestDistance) { bestDistance = d; best = i; }
        }
        return best;
    }

    /** GIF's LZW with an 8-bit minimum code size, written in sub-blocks of up to 255 bytes. */
    static void lzw(byte[] indices, ByteArrayOutputStream out) {
        final int min = 8, clear = 1 << min, end = clear + 1;
        Bits bits = new Bits(out);
        java.util.HashMap<Integer, Integer> table = new java.util.HashMap<>(8192);
        int size = min + 1, next = end + 1;
        bits.put(clear, size);
        int prefix = indices[0] & 255;
        for (int i = 1; i < indices.length; i++) {
            int k = indices[i] & 255;
            int key = (prefix << 8) | k;
            Integer known = table.get(key);
            if (known != null) { prefix = known; continue; }
            bits.put(prefix, size);
            // the decoder adds each entry one code later than here; widening after writing keeps both in step
            if (next > (1 << size) - 1 && size < 12) size++;
            if (next < 4096) table.put(key, next++);
            else { bits.put(clear, size); table.clear(); size = min + 1; next = end + 1; }
            prefix = k;
        }
        bits.put(prefix, size);
        if (next > (1 << size) - 1 && size < 12) size++;
        bits.put(end, size);
        bits.finish();
    }

    private static final class Bits {
        final ByteArrayOutputStream out;
        final byte[] block = new byte[255];
        int used, acc, count;
        Bits(ByteArrayOutputStream out) { this.out = out; }
        void put(int code, int size) {
            acc |= code << count;
            count += size;
            while (count >= 8) { add(acc & 255); acc >>>= 8; count -= 8; }
        }
        void add(int b) { block[used++] = (byte) b; if (used == 255) flush(); }
        void flush() { if (used == 0) return; out.write(used); out.write(block, 0, used); used = 0; }
        void finish() { if (count > 0) add(acc & 255); acc = 0; count = 0; flush(); out.write(0); }
    }

    private static void writeAscii(ByteArrayOutputStream out, String s) { for (int i = 0; i < s.length(); i++) out.write(s.charAt(i)); }
    private static void short16(ByteArrayOutputStream out, int v) { out.write(v & 255); out.write((v >> 8) & 255); }

}
