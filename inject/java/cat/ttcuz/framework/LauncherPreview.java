package cat.ttcuz.framework;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

/** Equal visible artwork size in the settings strip, independent of transparent source margins. */
final class LauncherPreview {
    private LauncherPreview() {}
    private static final int SIZE = 192, ART_SIZE = 136;
    private static final java.util.Map<String, Bitmap> cache = new java.util.HashMap<>();
    private static int accent;

    static synchronized Bitmap get(String which) {
        int current = Accent.colour();
        Bitmap known = cache.get(which);
        if (known != null && (!Launcher.ACCENT.equals(which) || accent == current)) return known;
        Bitmap source = Launcher.preview(which);
        if (source == null) return null;
        Bitmap result = normalize(source);
        cache.put(which, result);
        if (Launcher.ACCENT.equals(which)) accent = current;
        return result;
    }

    private static Bitmap normalize(Bitmap source) {
        int width = source.getWidth(), height = source.getHeight();
        int[] pixels = new int[width * height];
        source.getPixels(pixels, 0, width, 0, 0, width, height);
        int left = width, top = height, right = -1, bottom = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (Color.alpha(pixels[y * width + x]) <= 16) continue;
                left = Math.min(left, x); top = Math.min(top, y);
                right = Math.max(right, x); bottom = Math.max(bottom, y);
            }
        }
        Bitmap result = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        if (right < left) return result;
        Rect crop = new Rect(left, top, right + 1, bottom + 1);
        float scale = ART_SIZE / (float) Math.max(crop.width(), crop.height());
        float w = crop.width() * scale, h = crop.height() * scale;
        RectF to = new RectF((SIZE-w)/2f, (SIZE-h)/2f, (SIZE+w)/2f, (SIZE+h)/2f);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(to, ART_SIZE * 0.24f, ART_SIZE * 0.24f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, crop, to, paint);
        return result;
    }
}
