package cat.ttcuz.framework;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * An illustration for the glass setting: a small feed frame (colour fields and stripes, tinted by the accent) with the
 * navigation island floating over it, drawn as frosted or as liquid glass. Drawn once per size and colour into its own
 * layer: it is a picture, not a live capture, so it costs nothing while the settings scroll.
 */
final class GlassPreview extends View {
    private final boolean liquid, plastic;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tile = new RectF(), island = new RectF(), pill = new RectF();
    private final Path clip = new Path(), islandPath = new Path(), bevelPath = new Path();
    private final float density;
    private int accent;

    GlassPreview(Context context, boolean liquid) { this(context, liquid ? Blur.LIQUID : Blur.FROSTED); }

    GlassPreview(Context context, int style) {
        super(context);
        this.style = style;
        this.liquid = style == Blur.LIQUID;
        this.plastic = style == Blur.PLASTIC;
        density = getResources().getDisplayMetrics().density;
        accent = Accent.colour();
        setLayerType(LAYER_TYPE_HARDWARE, null);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void accent(int colour) { if (colour != accent) { accent = colour; invalidate(); } }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        float r = 16 * density;
        tile.set(0, 0, w, h);
        clip.reset(); clip.addRoundRect(tile, r, r, Path.Direction.CW);
        float side = 12 * density, height = Math.min(44 * density, h * .3f);
        island.set(side, h - side - height, w - side, h - side);
        islandPath.reset(); islandPath.addRoundRect(island, height / 2f, height / 2f, Path.Direction.CW);
        float pw = Math.min(island.width() * .38f, 64 * density), ph = height - 10 * density;
        pill.set(island.left + 5 * density, island.centerY() - ph / 2f, island.left + 5 * density + pw, island.centerY() + ph / 2f);
    }

    /** The scene behind: the accent and its neighbours as soft fields, with bright stripes so refraction shows. */
    private void scene(Canvas canvas, float blur) {
        int a = accent, b = NavigationIsland.blend(accent, 0xFFFFC857, .7f), c = NavigationIsland.blend(accent, 0xFF4CC9F0, .75f);
        paint.setShader(new LinearGradient(0, 0, tile.width(), tile.height(), 0xFF141824, 0xFF2A1638, Shader.TileMode.CLAMP));
        canvas.drawRect(tile, paint);
        float w = tile.width(), h = tile.height(), spread = 1f + blur;
        blob(canvas, w * .28f, h * .45f, w * .42f * spread, a, blur);
        blob(canvas, w * .78f, h * .62f, w * .36f * spread, b, blur);
        blob(canvas, w * .52f, h * .95f, w * .34f * spread, c, blur);
        paint.setShader(null);
        int alpha = Math.round(140 * (1f - Math.min(1f, blur)));
        if (alpha > 0) for (int i = 0; i < 9; i++) {
            paint.setColor((i % 3 == 0 ? a : i % 3 == 1 ? b : c) & 0xFFFFFF | alpha << 24);
            canvas.drawRect(w * (i + .3f) / 9f, h * .5f, w * (i + .55f) / 9f, h, paint);
        }
    }

    private void blob(Canvas canvas, float x, float y, float r, int colour, float blur) {
        int core = (colour & 0xFFFFFF) | (Math.round(255 * (1f - .35f * Math.min(1f, blur))) << 24);
        paint.setShader(new RadialGradient(x, y, r, core, colour & 0xFFFFFF, Shader.TileMode.CLAMP));
        canvas.drawCircle(x, y, r, paint);
    }

    /** A real screenshot of this style's island, when the build carries one (assets/ttcuz/island/<style>.png). */
    private static final android.graphics.Bitmap[] shots = new android.graphics.Bitmap[3];
    private static final boolean[] shotsTried = new boolean[3];
    private final int style;
    private final android.graphics.Rect from = new android.graphics.Rect();
    private final Paint picture = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

    private android.graphics.Bitmap shot() {
        if (style < 0 || style > 2) return null;
        if (!shotsTried[style]) {
            shotsTried[style] = true;
            String name = style == Blur.LIQUID ? "liquid" : style == Blur.PLASTIC ? "plastic" : "frosted";
            try (java.io.InputStream input = getContext().getAssets().open("ttcuz/island/" + name + ".png")) {
                shots[style] = android.graphics.BitmapFactory.decodeStream(input);
            } catch (Throwable missing) {
                shots[style] = null;
            }
        }
        return shots[style];
    }

    /** Whether the build carries the screenshots: the settings then stack the tiles, one island across each. */
    boolean real() { return shot() != null; }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        android.graphics.Bitmap real = shot();
        if (real == null || MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED) { super.onMeasure(widthSpec, heightSpec); return; }
        // the island at its own proportions: as wide as the tile, as tall as that makes it
        int w = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(w, Math.round(w * (float) real.getHeight() / real.getWidth()));
    }

    @Override protected void onDraw(Canvas canvas) {
        android.graphics.Bitmap real = shot();
        if (real != null) {
            // the screenshot is already cut to the island's capsule, transparent around it
            from.set(0, 0, real.getWidth(), real.getHeight());
            canvas.drawBitmap(real, from, tile, picture);
            return;
        }
        if (tile.isEmpty()) return;
        int s = canvas.save(); canvas.clipPath(clip);
        scene(canvas, 0f);
        if (liquid) {
            // Lifted off the frame by a wide, faint shadow, as iOS does.
            paint.setShader(null); paint.setColor(0xFF24262B);
            paint.setShadowLayer(10 * density, 0, 3 * density, 0x38000000);
            canvas.drawRoundRect(island, island.height() / 2f, island.height() / 2f, paint);
            paint.clearShadowLayer();
        }
        int g = canvas.save(); canvas.clipPath(islandPath);
        if (liquid) {
            // Clear in the middle; across the bevel the glass bends in, compressed, what lies just beyond its edge.
            scene(canvas, 0f);
            float bevel = island.height() * .24f;
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                bevelPath.reset();
                bevelPath.addRoundRect(island.left + bevel, island.top + bevel, island.right - bevel, island.bottom - bevel,
                        island.height() / 2f - bevel, island.height() / 2f - bevel, Path.Direction.CW);
                int ring = canvas.save(); canvas.clipOutPath(bevelPath);
                canvas.scale(.84f, .62f, island.centerX(), island.centerY());
                scene(canvas, 0f);
                canvas.restoreToCount(ring);
            }
            paint.setShader(null); paint.setColor(0x26101218); canvas.drawRect(island, paint);
        } else if (plastic) {
            // Plastic: a solid tonal surface, nothing of the frame shows through.
            paint.setShader(null); paint.setColor(NavigationIsland.blend(0xFF24262B, accent, .10f)); canvas.drawRect(island, paint);
        } else {
            // Frosted: the frame dissolves into soft colour under a milky tonal veil.
            scene(canvas, 1.2f);
            paint.setShader(null); paint.setColor(0xB824262B); canvas.drawRect(island, paint);
        }
        canvas.restoreToCount(g);
        paint.setShader(null); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(Math.max(1f, density));
        if (liquid) {
            // The specular hairline: bright on top, almost gone on the sides, a weaker reflection at the bottom.
            paint.setShader(new LinearGradient(0, island.top, 0, island.bottom, new int[]{0xB3FFFFFF, 0x26FFFFFF, 0x14FFFFFF, 0x59FFFFFF}, new float[]{0f, .32f, .62f, 1f}, Shader.TileMode.CLAMP));
        } else paint.setColor(0x22FFFFFF);
        float half = paint.getStrokeWidth() / 2f, round = island.height() / 2f - half;
        canvas.drawRoundRect(island.left + half, island.top + half, island.right - half, island.bottom - half, round, round, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        // The selected pill and four icon dots, so it reads as the bottom bar.
        paint.setColor(NavigationIsland.blend(0xFF24262B, accent, .45f) & 0xFFFFFF | 0xE6000000);
        canvas.drawRoundRect(pill, pill.height() / 2f, pill.height() / 2f, paint);
        paint.setColor(0xFFF2F0F5);
        canvas.drawCircle(pill.left + pill.height() / 2f + 2 * density, pill.centerY(), 5 * density, paint);
        paint.setColor(0x99F2F0F5);
        float step = (island.right - pill.right) / 4f;
        for (int i = 0; i < 3; i++) {
            float x = pill.right + step * (i + .7f);
            if (i == 1) { paint.setColor(accent); canvas.drawCircle(x, island.centerY(), 9 * density, paint); paint.setColor(0x99F2F0F5); }
            else canvas.drawCircle(x, island.centerY(), 5 * density, paint);
        }
        canvas.restoreToCount(s);
    }
}
