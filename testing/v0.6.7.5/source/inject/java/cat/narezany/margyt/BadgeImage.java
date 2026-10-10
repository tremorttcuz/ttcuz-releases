package cat.narezany.margyt;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

/** Import a bounded picture onto a transparent square, preserving all colours. */
final class BadgeImage {
    static byte[] read(android.content.Context context, android.net.Uri uri) throws Exception {
        byte[] bytes;
        try (java.io.InputStream in = context.getContentResolver().openInputStream(uri);
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            if (in == null) throw new Exception("Не удалось открыть картинку");
            byte[] block = new byte[16384]; int got;
            while ((got = in.read(block)) != -1) {
                if (out.size() + got > 4 * 1024 * 1024) throw new Exception("Выберите картинку размером до 4 МБ");
                out.write(block, 0, got);
            }
            bytes = out.toByteArray();
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        if (bounds.outWidth < 16 || bounds.outHeight < 16 || bounds.outWidth > 4096 || bounds.outHeight > 4096)
            throw new Exception("Нужна картинка от 16 до 4096 пикселей по каждой стороне");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = Math.max(1, Math.max(bounds.outWidth,bounds.outHeight) / 256);
        Bitmap source = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        if (source == null) throw new Exception("Не удалось прочитать картинку");
        try {
            for (int size = 128; size >= 32; size -= 16) {
                Bitmap square = Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);
                try {
                    float ratio = Math.min((float)size/source.getWidth(),(float)size/source.getHeight());
                    int w = Math.max(1,Math.round(source.getWidth()*ratio));
                    int h = Math.max(1,Math.round(source.getHeight()*ratio));
                    new Canvas(square).drawBitmap(source,null,
                            new Rect((size-w)/2,(size-h)/2,(size+w)/2,(size+h)/2),
                            new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
                    java.io.ByteArrayOutputStream encoded = new java.io.ByteArrayOutputStream();
                    square.compress(Bitmap.CompressFormat.PNG,100,encoded);
                    if (encoded.size() <= 32768) return encoded.toByteArray();
                } finally { square.recycle(); }
            }
        } finally { source.recycle(); }
        throw new Exception("Картинка слишком сложная. Выберите более простую");
    }
}
