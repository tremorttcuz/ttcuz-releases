package cat.ttcuz.framework;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.LinearLayout;
import java.util.HashMap;
import java.util.Map;

/** Builds the shaped icons used by the custom settings screen. */
final class SettingsIconFactory {

    private static final Map<String, Integer> SHAPES = shapes();
    private static final Map<String, Bitmap> ICONS = new HashMap<>();

    private SettingsIconFactory() {}

    static View create(
        Activity activity,
        Skin skin,
        String name,
        boolean toggle,
        int sizeDp,
        int tonal,
        int ink,
        int onAccent
    ) {
        Drawable vector = SettingsGlyph.make(name, 0xFFFFFFFF);
        if (vector == null) {
            Bitmap picture = pictureFor(activity, name);
            if (picture != null) vector = new android.graphics.drawable.BitmapDrawable(
                activity.getResources(),
                picture
            );
        }

        int accent = Accent.colour();
        Expressive.Badge badge = toggle
            ? new Expressive.Badge(
                  activity,
                  shapeFor(name),
                  vector,
                  sizeDp,
                  Expressive.mix(skin.card, skin.text, 0.14f),
                  accent,
                  skin.muted(),
                  onAccent
              )
            : new Expressive.Badge(
                  activity,
                  shapeFor(name),
                  vector,
                  sizeDp,
                  tonal,
                  tonal,
                  ink,
                  ink
              );
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(
            dp(activity, sizeDp),
            dp(activity, sizeDp)
        );
        size.rightMargin = dp(activity, 16);
        badge.setLayoutParams(size);
        return badge;
    }

    private static Map<String, Integer> shapes() {
        Map<String, Integer> shapes = new HashMap<>();
        shapes.put("palette", Expressive.COOKIE9);
        shapes.put("group", Expressive.CLOVER4);
        shapes.put("play_circle", Expressive.SUNNY);
        shapes.put("tune", Expressive.FLOWER6);
        shapes.put("info", Expressive.COOKIE9);
        shapes.put("extension", Expressive.COOKIE7);
        shapes.put("chat", Expressive.BURST12);
        return shapes;
    }

    private static int shapeFor(String name) {
        Integer fixed = SHAPES.get(name);
        if (fixed != null) return fixed;
        int[] even = {
            Expressive.COOKIE9,
            Expressive.CLOVER4,
            Expressive.SUNNY,
            Expressive.FLOWER6,
            Expressive.BURST12,
            Expressive.COOKIE7,
        };
        return even[(name.hashCode() & 0x7FFFFFFF) % even.length];
    }

    private static Bitmap pictureFor(Activity activity, String name) {
        Bitmap bitmap = ICONS.get(name);
        if (bitmap != null) return bitmap;
        try {
            String data = Icons.PNG.get(name);
            if (data == null) return null;
            byte[] png = android.util.Base64.decode(data, android.util.Base64.DEFAULT);
            bitmap = android.graphics.BitmapFactory.decodeByteArray(png, 0, png.length);
            if (bitmap != null) ICONS.put(name, bitmap);
        } catch (Throwable ignored) {}
        return bitmap;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
