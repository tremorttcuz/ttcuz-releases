package cat.narezany.margyt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/** A crisp, tightly drawn crown; the old raster badge contained speckled edges. */
final class CreatorCrown {
    private CreatorCrown() {}
    private static Bitmap shape;

    static synchronized Drawable drawable(Context context) {
        try {
            if (shape == null) shape = make();
            return new BitmapDrawable(context.getResources(), shape) {
                @Override public void draw(Canvas canvas) {
                    setColorFilter(Themes.isDark() ? 0xFFFFFFFF : 0xFF000000,
                            PorterDuff.Mode.SRC_IN);
                    super.draw(canvas);
                }
            };
        } catch (Throwable error) {
            Diary.note("creator crown: " + error);
            return null;
        }
    }

    private static Bitmap make() {
        final int width = 256, height = 190;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xFFFFFFFF);
        paint.setStyle(Paint.Style.FILL);

        Path crown = new Path();
        crown.moveTo(27, 42);
        crown.quadTo(31, 38, 36, 47);
        crown.lineTo(62, 112);
        crown.quadTo(65, 121, 72, 113);
        crown.lineTo(121, 25);
        crown.quadTo(128, 12, 135, 25);
        crown.lineTo(184, 113);
        crown.quadTo(191, 121, 194, 112);
        crown.lineTo(220, 47);
        crown.quadTo(225, 38, 229, 42);
        crown.lineTo(211, 153);
        crown.quadTo(210, 160, 202, 160);
        crown.lineTo(54, 160);
        crown.quadTo(46, 160, 45, 153);
        crown.close();
        canvas.drawPath(crown, paint);

        RectF band = new RectF(44, 151, 212, 176);
        canvas.drawRoundRect(band, 9, 9, paint);

        // Three clean round tips preserve the beaded character of the mark
        // without the grain and stray pixels in the previous bitmap.
        canvas.drawCircle(31, 39, 12, paint);
        canvas.drawCircle(128, 18, 13, paint);
        canvas.drawCircle(225, 39, 12, paint);
        canvas.drawCircle(64, 164, 4, paint);
        canvas.drawCircle(128, 164, 4, paint);
        canvas.drawCircle(192, 164, 4, paint);
        return bitmap;
    }
}
