package cat.ttcuz.framework;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;

import org.json.JSONObject;

/**
 * A nickname effect made by the person: what floats around the name, how many, how big, how it moves and in what
 * colour -- a drawn shape or a small picture of their own.
 *
 * Kept as a little JSON object (the picture inside it as a PNG of at most 48 px, in base64), stored with the own
 * profile style and published with it, so everybody with the mod sees the same effect on that name.
 */
final class ParticleRecipe {
    static final int DOT = 0, SPARKLE = 1, HEART = 2, STAR = 3, FLAKE = 4, PICTURE = 5;
    static final int TWINKLE = 0, RISE = 1, FALL = 2, DRIFT = 3, ORBIT = 4, BURST = 5;
    static final int NAME_COLOUR = 0, OWN_COLOUR = 1, RAINBOW = 2, PICTURE_COLOURS = 3;
    static final int MOST_IMAGE = 8000; // base64 characters

    int shape = SPARKLE, count = 8, size = 6, vary = 40, speed = 100, motion = TWINKLE, wiggle = 30, spin = 0;
    boolean fade = true;
    int colourMode = NAME_COLOUR, colour = 0xFFFFD54F, opacity = 90, spread = 30;
    String image = "";

    private Bitmap picture;
    private String pictureFrom;

    ParticleRecipe copy() { ParticleRecipe r = parse(json()); return r == null ? new ParticleRecipe() : r; }

    String json() {
        try {
            JSONObject j = new JSONObject();
            j.put("v", 1).put("shape", shape).put("count", count).put("size", size).put("vary", vary).put("speed", speed)
                    .put("motion", motion).put("wiggle", wiggle).put("spin", spin).put("fade", fade)
                    .put("colourMode", colourMode).put("colour", colour).put("opacity", opacity).put("spread", spread);
            if (image != null && !image.isEmpty()) j.put("image", image);
            return j.toString();
        } catch (Throwable error) {
            return "{}";
        }
    }

    /** A recipe from its JSON, every number held to its range; null when it is not one. */
    static ParticleRecipe parse(String text) {
        if (text == null || text.isEmpty() || text.length() > MOST_IMAGE + 600) return null;
        try {
            JSONObject j = new JSONObject(text);
            ParticleRecipe r = new ParticleRecipe();
            r.shape = clamp(j.optInt("shape", SPARKLE), 0, 5);
            r.count = clamp(j.optInt("count", 8), 1, 24);
            r.size = clamp(j.optInt("size", 6), 2, 28);
            r.vary = clamp(j.optInt("vary", 40), 0, 100);
            r.speed = clamp(j.optInt("speed", 100), 10, 300);
            r.motion = clamp(j.optInt("motion", TWINKLE), 0, 5);
            r.wiggle = clamp(j.optInt("wiggle", 30), 0, 100);
            r.spin = clamp(j.optInt("spin", 0), -360, 360);
            r.fade = j.optBoolean("fade", true);
            r.colourMode = clamp(j.optInt("colourMode", NAME_COLOUR), 0, 3);
            r.colour = j.optInt("colour", 0xFFFFD54F) | 0xFF000000;
            r.opacity = clamp(j.optInt("opacity", 90), 10, 100);
            r.spread = clamp(j.optInt("spread", 30), 0, 100);
            String picture = j.optString("image", "");
            r.image = picture.length() <= MOST_IMAGE && picture.matches("[A-Za-z0-9+/=]*") ? picture : "";
            if (r.shape == PICTURE && r.image.isEmpty()) r.shape = SPARKLE;
            return r;
        } catch (Throwable error) {
            return null;
        }
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    /** A picture chosen for the effect, made small: a square PNG of at most 48 px, transparency kept. */
    static String encodePicture(Bitmap source) {
        if (source == null) return "";
        int side = Math.min(source.getWidth(), source.getHeight());
        if (side <= 0) return "";
        int target = Math.min(48, side);
        Bitmap square = Bitmap.createBitmap(target, target, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(square);
        int left = (source.getWidth() - side) / 2, top = (source.getHeight() - side) / 2;
        c.drawBitmap(source, new android.graphics.Rect(left, top, left + side, top + side), new RectF(0, 0, target, target),
                new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));
        for (int quality = target; quality >= 16; quality -= 8) {
            Bitmap shrunk = quality == target ? square : Bitmap.createScaledBitmap(square, quality, quality, true);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            shrunk.compress(Bitmap.CompressFormat.PNG, 100, out);
            String text = android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP);
            if (text.length() <= MOST_IMAGE) return text;
        }
        return "";
    }

    private Bitmap picture() {
        if (image == null || image.isEmpty()) return null;
        if (picture != null && image.equals(pictureFrom)) return picture;
        try {
            byte[] bytes = android.util.Base64.decode(image, android.util.Base64.DEFAULT);
            picture = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            pictureFrom = image;
        } catch (Throwable error) {
            picture = null;
        }
        return picture;
    }

    // ------------------------------------------------------------------ drawing

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path shapePath = new Path();
    private final RectF box = new RectF();

    /**
     * One frame over a nickname w x h: seeds holds three random numbers per particle (place, phase, size), fixed per
     * view so the particles do not jump between frames.
     */
    void draw(Canvas canvas, int w, int h, float d, long now, int nameColour, float[] seeds) {
        float reachX = w * spread / 200f, reachY = h * (.2f + spread / 100f);
        int n = Math.min(count, seeds.length / 3);
        Bitmap art = shape == PICTURE ? picture() : null;
        for (int i = 0; i < n; i++) {
            float sx = seeds[i * 3], sp = seeds[i * 3 + 1], sz = seeds[i * 3 + 2];
            float period = (2600f + sp * 2200f) * 100f / speed;
            float t = ((now + sp * 9000f) % period) / period;
            float wig = (float) Math.sin((t * 2 + sx) * Math.PI * 2) * wiggle / 100f * 4 * d;
            float x, y, life;
            switch (motion) {
                case RISE: x = -reachX + sx * (w + 2 * reachX) + wig; y = h + reachY - t * (h + 2 * reachY); life = (float) Math.sin(t * Math.PI); break;
                case FALL: x = -reachX + sx * (w + 2 * reachX) + wig; y = -reachY + t * (h + 2 * reachY); life = (float) Math.sin(t * Math.PI); break;
                case DRIFT: x = -reachX + ((sx + t) % 1f) * (w + 2 * reachX); y = -reachY / 2 + sp * (h + reachY) + wig; life = (float) Math.sin(((sx + t) % 1f) * Math.PI); break;
                case ORBIT: {
                    double a = (t + sx) * Math.PI * 2;
                    x = w / 2f + (float) Math.cos(a) * (w / 2f + reachX * .5f); y = h / 2f + (float) Math.sin(a) * (h / 2f + reachY * .5f) + wig;
                    life = 1f; break;
                }
                case BURST: {
                    double a = sx * Math.PI * 2;
                    float r = t * (Math.max(w, h) / 2f + reachX);
                    x = w / 2f + (float) Math.cos(a) * r; y = h / 2f + (float) Math.sin(a) * r * .5f; life = 1f - t; break;
                }
                default: x = -reachX + sx * (w + 2 * reachX) + wig; y = -reachY / 2 + sp * (h + reachY); life = (float) Math.max(0, Math.sin(t * Math.PI * 2)); break;
            }
            float alpha = (fade ? life : 1f) * opacity / 100f;
            if (alpha <= .01f) continue;
            float r = size * d / 2f * (1f - vary / 100f * sz);
            int c;
            switch (colourMode) {
                case OWN_COLOUR: c = colour; break;
                case RAINBOW: c = Color.HSVToColor(new float[]{((sx * 360f) + now / 20f) % 360f, .65f, 1f}); break;
                case PICTURE_COLOURS: c = 0xFFFFFFFF; break;
                default: c = nameColour; break;
            }
            int save = canvas.save();
            canvas.translate(x, y);
            if (spin != 0) canvas.rotate(spin * (now / 1000f) * (sp > .5f ? 1 : -1) + sx * 360f);
            paint.setAlpha(255);
            if (art != null) {
                paint.setColorFilter(colourMode == PICTURE_COLOURS ? null : new PorterDuffColorFilter(c, PorterDuff.Mode.SRC_IN));
                paint.setAlpha(Math.round(255 * alpha));
                box.set(-r, -r, r, r);
                canvas.drawBitmap(art, null, box, paint);
                paint.setColorFilter(null);
            } else {
                paint.setColor(c);
                paint.setAlpha(Math.round(255 * alpha));
                switch (shape) {
                    case SPARKLE: sparkle(canvas, r); break;
                    case HEART: heart(canvas, r * .7f); break;
                    case STAR: star(canvas, r); break;
                    case FLAKE: flake(canvas, r); break;
                    default: canvas.drawCircle(0, 0, r * .6f, paint); break;
                }
            }
            canvas.restoreToCount(save);
        }
    }

    private void sparkle(Canvas c, float r) {
        shapePath.reset();
        shapePath.moveTo(0, -r); shapePath.quadTo(0, 0, r, 0); shapePath.quadTo(0, 0, 0, r); shapePath.quadTo(0, 0, -r, 0); shapePath.quadTo(0, 0, 0, -r);
        c.drawPath(shapePath, paint);
    }

    private void heart(Canvas c, float r) {
        shapePath.reset();
        shapePath.moveTo(0, r);
        shapePath.cubicTo(-r * 2, -r * .2f, -r, -r * 1.6f, 0, -r * .5f);
        shapePath.cubicTo(r, -r * 1.6f, r * 2, -r * .2f, 0, r);
        c.drawPath(shapePath, paint);
    }

    private void star(Canvas c, float r) {
        shapePath.reset();
        for (int k = 0; k < 10; k++) {
            double a = -Math.PI / 2 + k * Math.PI / 5;
            float rr = k % 2 == 0 ? r : r * .45f;
            float px = (float) Math.cos(a) * rr, py = (float) Math.sin(a) * rr;
            if (k == 0) shapePath.moveTo(px, py); else shapePath.lineTo(px, py);
        }
        shapePath.close();
        c.drawPath(shapePath, paint);
    }

    private void flake(Canvas c, float r) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, r * .18f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int k = 0; k < 3; k++) {
            double a = k * Math.PI / 3;
            float px = (float) Math.cos(a) * r, py = (float) Math.sin(a) * r;
            c.drawLine(-px, -py, px, py, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }
}
