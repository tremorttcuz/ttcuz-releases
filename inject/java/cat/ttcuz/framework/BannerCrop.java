package cat.ttcuz.framework;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;

/**
 * A plugin's banner, chosen by eye: the picture in a 3:1 frame, moved with a finger and brought closer with a
 * slider -- the frame is exactly what the store card will show. Gives a 1200×400 JPEG of at most 280 KB as base64.
 */
final class BannerCrop {
    private BannerCrop() {}

    interface Done { void on(String base64); }

    static void show(final Activity activity, final Skin skin, final Uri uri, final Done done) {
        Net.away("banner open", () -> {
            final Bitmap picture = read(activity, uri);
            activity.runOnUiThread(() -> {
                if (picture == null) { Screen.say(Text.t("Это не картинка")); return; }
                final Frame frame = new Frame(activity, picture);
                LinearLayout box = new LinearLayout(activity);
                box.setOrientation(LinearLayout.VERTICAL);
                box.addView(frame, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                SeekBar zoom = new SeekBar(activity);
                zoom.setMax(200);
                zoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar bar, int value, boolean user) { frame.zoom = 1f + value / 100f; frame.invalidate(); }
                    @Override public void onStartTrackingTouch(SeekBar bar) { }
                    @Override public void onStopTrackingTouch(SeekBar bar) { }
                });
                int pad = Math.round(8 * activity.getResources().getDisplayMetrics().density);
                zoom.setPadding(pad * 2, pad * 2, pad * 2, pad);
                box.addView(zoom);
                Panel panel = Panel.with(activity, skin, Text.t("Баннер"));
                panel.text(Text.t("Передвиньте картинку и выберите, что будет видно над карточкой плагина."));
                panel.view(box);
                panel.primary(Text.t("Готово"), () -> Net.away("banner save", () -> {
                    final String out = frame.render();
                    activity.runOnUiThread(() -> { if (out == null) Screen.say(Text.t("Картинка слишком сложная, выберите попроще")); else done.on(out); });
                }));
                panel.quiet(Text.t("Отмена"), null);
                panel.show();
            });
        });
    }

    /** The picture, at most about 2400 px wide: enough for a 1200 px banner even brought twice as close. */
    private static Bitmap read(Activity activity, Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (java.io.InputStream in = activity.getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(in, null, bounds); }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            while (bounds.outWidth / (options.inSampleSize * 2) >= 2400) options.inSampleSize *= 2;
            try (java.io.InputStream in = activity.getContentResolver().openInputStream(uri)) { return BitmapFactory.decodeStream(in, null, options); }
        } catch (Throwable error) {
            return null;
        }
    }

    /** The 3:1 window over the picture; (cx, cy) is the point of the picture at its centre, 0..1. */
    private static final class Frame extends View {
        final Bitmap picture;
        final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG), edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        final android.graphics.Path clip = new android.graphics.Path();
        float zoom = 1f, cx = .5f, cy = .5f, lastX, lastY;

        Frame(Activity activity, Bitmap picture) {
            super(activity);
            this.picture = picture;
            edge.setStyle(Paint.Style.STROKE);
            edge.setColor(0x99FFFFFF);
            edge.setStrokeWidth(2 * getResources().getDisplayMetrics().density);
        }

        @Override protected void onMeasure(int w, int h) { int width = MeasureSpec.getSize(w); setMeasuredDimension(width, width / 3); }

        private float scale(float w, float h) { return Math.max(w / picture.getWidth(), h / picture.getHeight()) * zoom; }

        private void keepInside(float w, float h) {
            float k = scale(w, h), halfW = w / 2f / k / picture.getWidth(), halfH = h / 2f / k / picture.getHeight();
            cx = Math.max(halfW, Math.min(1 - halfW, cx));
            cy = Math.max(halfH, Math.min(1 - halfH, cy));
        }

        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight(), r = 18 * getResources().getDisplayMetrics().density;
            keepInside(w, h);
            clip.reset();
            clip.addRoundRect(new RectF(0, 0, w, h), r, r, android.graphics.Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clip);
            canvas.drawColor(0xFF000000);
            float k = scale(w, h);
            float left = w / 2f - cx * picture.getWidth() * k, top = h / 2f - cy * picture.getHeight() * k;
            canvas.drawBitmap(picture, null, new RectF(left, top, left + picture.getWidth() * k, top + picture.getHeight() * k), paint);
            canvas.restore();
            canvas.drawRoundRect(new RectF(1, 1, w - 1, h - 1), r, r, edge);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) { lastX = e.getX(); lastY = e.getY(); getParent().requestDisallowInterceptTouchEvent(true); return true; }
            if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
                float k = scale(getWidth(), getHeight());
                cx -= (e.getX() - lastX) / k / picture.getWidth();
                cy -= (e.getY() - lastY) / k / picture.getHeight();
                lastX = e.getX(); lastY = e.getY();
                invalidate();
            }
            return true;
        }

        /** The frame as the store will show it, 1200×400. */
        String render() {
            Bitmap out = Bitmap.createBitmap(1200, 400, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(out);
            canvas.drawColor(0xFF000000);
            float k = scale(1200, 400);
            float left = 600 - cx * picture.getWidth() * k, top = 200 - cy * picture.getHeight() * k;
            canvas.drawBitmap(picture, null, new RectF(left, top, left + picture.getWidth() * k, top + picture.getHeight() * k), paint);
            for (int quality : new int[]{86, 74, 62, 50}) {
                java.io.ByteArrayOutputStream jpeg = new java.io.ByteArrayOutputStream();
                out.compress(Bitmap.CompressFormat.JPEG, quality, jpeg);
                if (jpeg.size() <= 280000) return android.util.Base64.encodeToString(jpeg.toByteArray(), android.util.Base64.NO_WRAP);
            }
            return null;
        }
    }
}
