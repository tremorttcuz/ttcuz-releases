package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * How the navigation island moves, and which icons it draws.
 *
 * Motion: one choice scales every spring of the island (the selection, the press, the turning Sunny) and the flying
 * Liquid Glass drop together: how stiff (fast) they are, how little they are damped (how much they overshoot and
 * wobble), and how much the drop swells and stretches in flight. "Still" moves nothing: every change lands at once.
 *
 * Glass: the light lens (one texture read a pixel, see Blur.LIGHT_SHADER) or the heavy, physical one.
 */
final class IslandMotion {
    private IslandMotion() {}

    /**
     * The island moves as designed (the presets are gone); only the drop swells a little more than the design's own
     * while it travels between tabs: 35 % wider (was 24 %) and 30 % taller (was 32 %), always inside the island.
     */
    static boolean moves(Context context) { return Motion.enabled(context); }

    static float stiffness() { return 1f; }
    static float damping() { return 1f; }
    static float swell() { return 1f; }
    static float wide() { return 35f / 24f; }
    static float tall() { return 30f / 32f; }
    /** The video's bubble style, taken out with the presets. */
    static boolean round() { return false; }
    static float swellStiffness() { return 1f; }
    static float swellReach() { return .35f; }
    /** The island's glass is the physical lens, as it was before the light one. */
    static boolean lightGlass() { return false; }
    static boolean lightLens() { return false; }

    private static SharedPreferences prefs() {
        Context context = Ttcuz.context();
        return context == null ? null : context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
    }

    // ------------------------------------------------- the icons: Material 3 or TikTok's own

    static final String ICONS_KEY = "island_icons_tiktok";
    static final String[] ICON_NAMES = {"Material 3", "Как в TikTok"};
    private static volatile Boolean tiktokIcons;

    static boolean tiktokIcons() {
        Boolean known = tiktokIcons;
        if (known != null) return known;
        SharedPreferences p = prefs();
        boolean value = p != null && p.getBoolean(ICONS_KEY, false);
        tiktokIcons = value;
        return value;
    }

    static void setTiktokIcons(boolean value) {
        tiktokIcons = value;
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean(ICONS_KEY, value).apply();
        NavigationIsland.recollect();
    }

    /** The grid TikTok's icon is drawn on before it becomes a shape: fine enough that its steps are under a pixel. */
    private static final int GRID = 96;

    /**
     * The icon TikTok shows in a tab (whatever it is this month: its new outlines, a Lottie frame) as a 24 x 24 path,
     * so the island draws, tints, magnifies and cross-fades it exactly as it does a Material symbol. The drawable is
     * drawn once into an alpha mask, its covered runs become a Region, and the Region's outline is the path. Null when
     * there is nothing to trace (no drawable, an empty picture): the Material symbol is kept then.
     */
    static android.graphics.Path trace(android.widget.ImageView view) {
        android.graphics.drawable.Drawable drawable = view == null ? null : view.getDrawable();
        if (drawable == null) return null;
        android.graphics.Bitmap mask = null;
        android.graphics.Rect was = drawable.copyBounds();
        int alpha = drawable.getAlpha();
        try {
            mask = android.graphics.Bitmap.createBitmap(GRID, GRID, android.graphics.Bitmap.Config.ALPHA_8);
            // keep the icon's own proportions, centred, as the tab shows it
            int w = drawable.getIntrinsicWidth(), h = drawable.getIntrinsicHeight();
            float k = w > 0 && h > 0 ? Math.min(GRID / (float) w, GRID / (float) h) : 1f;
            int dw = w > 0 && h > 0 ? Math.round(w * k) : GRID, dh = w > 0 && h > 0 ? Math.round(h * k) : GRID;
            drawable.setBounds((GRID - dw) / 2, (GRID - dh) / 2, (GRID + dw) / 2, (GRID + dh) / 2);
            // the island hides the native icon by its alpha: drawn whole here, then hidden again
            drawable.setAlpha(255);
            drawable.draw(new android.graphics.Canvas(mask));
            java.nio.ByteBuffer pixels = java.nio.ByteBuffer.allocate(mask.getRowBytes() * GRID);
            mask.copyPixelsToBuffer(pixels);
            byte[] a = pixels.array();
            int row = mask.getRowBytes();
            android.graphics.Region shape = new android.graphics.Region();
            boolean any = false;
            for (int y = 0; y < GRID; y++) {
                int x = 0;
                while (x < GRID) {
                    while (x < GRID && (a[y * row + x] & 0xFF) < 110) x++;
                    int start = x;
                    while (x < GRID && (a[y * row + x] & 0xFF) >= 110) x++;
                    if (x > start) { shape.op(start, y, x, y + 1, android.graphics.Region.Op.UNION); any = true; }
                }
            }
            if (!any) return null;
            android.graphics.Path path = shape.getBoundaryPath();
            android.graphics.Matrix scale = new android.graphics.Matrix();
            scale.setScale(24f / GRID, 24f / GRID);
            path.transform(scale);
            return path;
        } catch (Throwable error) {
            Diary.note("island icon: " + error);
            return null;
        } finally {
            drawable.setBounds(was);
            drawable.setAlpha(alpha);
            if (mask != null) mask.recycle();
        }
    }


}
