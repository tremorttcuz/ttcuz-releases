package cat.ttcuz.framework;

import android.app.Activity;
import android.content.Context;

import com.ss.android.ugc.aweme.im.message.template.card.StickerTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Saving a sticker out of a conversation, which TikTok does not offer.
 *
 * Touching one goes through `MessageListStickerClickAbility`, whose name is
 * real, and it is handed a `StickerTemplate`, whose name is real as well.
 * Five of its methods take that type -- the obfuscator renamed the methods,
 * not the types -- and all five are rewritten to come through here. Whatever
 * the app was going to do still happens: the sticker is noted on the way past
 * and the call is passed straight on.
 *
 * The receiver arrives as a plain Object rather than as the interface, and
 * deliberately: a call site names whatever static type it is holding, which is
 * the class implementing the interface and not the interface itself, so the
 * rewrite cannot ask for one particular owner. Which is why the call onwards
 * goes through reflection -- on a tap, where it costs nothing.
 *
 * The address inside the template is found by walking the object rather than
 * by naming its parts, because the parts are obfuscated and a walk of a few
 * dozen fields costs nothing on a tap. Anything that looks like an http
 * address and belongs to a sticker will do; the largest is preferred, since
 * these models keep the same picture at three sizes.
 */
public final class Stickers {
    static void reloadSettings(){cached=null;}

    private Stickers() {}

public static final String KEY = "save_stickers";

    private static volatile Boolean cached;

    public static boolean isEnabled() {
        Boolean known = cached;
        if (known != null) return known;
        try {
            android.content.Context context = Ttcuz.context();
            if (context == null) return true;  // on until there is somewhere to read from
            boolean on = context.getSharedPreferences(Ttcuz.PREFS,
                    android.content.Context.MODE_PRIVATE).getBoolean(KEY, true);
            cached = on;
            return on;
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static void setEnabled(boolean enabled) {
        cached = enabled;
        try {
            android.content.Context context = Ttcuz.context();
            if (context == null) return;
            context.getSharedPreferences(Ttcuz.PREFS, android.content.Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY, enabled).apply();
        } catch (Throwable ignored) {
        }
    }

    private static volatile String latest;

    // ------------------------------------------------ where the taps land

    public static void UP(Object ability, Object view, Object a, Object b,
                          StickerTemplate sticker) {
        seen(sticker);
        onwards(ability, "UP", view, a, b, sticker);
    }

    public static void tR1(Object ability, Object view, Object a, Object b,
                           StickerTemplate sticker) {
        seen(sticker);
        onwards(ability, "tR1", view, a, b, sticker);
    }

    public static void dy1(Object ability, Object view, Object manager, Object a,
                           StickerTemplate sticker) {
        seen(sticker);
        onwards(ability, "dy1", view, manager, a, sticker);
    }

    public static void Yt1(Object ability, Object a, Object b, StickerTemplate sticker) {
        seen(sticker);
        onwards(ability, "Yt1", a, b, sticker);
    }

    // TikTok 47.2.41's names for the same five
    public static void PQ1(Object ability, Object view, Object a, Object message, StickerTemplate sticker) {
        seen(sticker);
        onwards(ability, "PQ1", view, a, message, sticker);
    }

    public static void GP(Object ability, Object view, Object a, Object message, StickerTemplate sticker) {
        seen(sticker);
        onwards(ability, "GP", view, a, message, sticker);
    }

    public static void mE2(Object ability, Object image, Object manager, Object message, StickerTemplate sticker) {
        seen(sticker);
        onwards(ability, "mE2", image, manager, message, sticker);
    }

    public static void xQ1(Object ability, Object corner, StickerTemplate sticker) {
        seen(sticker);
        onwards(ability, "xQ1", corner, sticker);
    }

    /** Called as a chat's messages arrive (StoreStickerCardReusedUISlot.Aj), not on a tap: no offer to save from here. */
    public static void Yn0(Object ability, Object session, StickerTemplate sticker, Object done) {
        onwards(ability, "Yn0", session, sticker, done);
    }

    public static void XQ1(Object ability, Object corner, StickerTemplate sticker) {
        seen(sticker);
        onwards(ability, "XQ1", corner, sticker);
    }

    /**
     * Hand the call back to TikTok.
     *
     * By name and by how many arguments it takes, rather than by their types:
     * the types are obfuscated, and the mod is holding the very objects that
     * were about to be passed, so there is nothing to resolve. If this fails
     * the tap does nothing -- which is why it happens last, after the mod has
     * already noted what it wanted.
     */
    private static void onwards(Object ability, String name, Object... args) {
        if (ability == null) return;
        try {
            for (Method method : ability.getClass().getMethods()) {
                if (!method.getName().equals(name)) continue;
                if (method.getParameterTypes().length != args.length) continue;
                method.setAccessible(true);
                method.invoke(ability, args);
                return;
            }
            Diary.note("sticker tap: no " + name + " taking " + args.length);
        } catch (Throwable error) {
            Diary.note("sticker tap: " + error);
        }
    }

    private static volatile long latestAt, tappedAt = -10_000L;

    /** A tap (a short touch that did not move) just ended somewhere in the app's window. */
    static void tapped() { tappedAt = android.os.SystemClock.uptimeMillis(); }
    private static final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    /** No sheet came up for the sticker (a chat's sticker has none): the old floating button after all. */
    private static final Runnable fallback = () -> Screen.offer(Text.SAVE_STICKER, 0.46f, Stickers::save);

    static void seen(Object sticker) {
        if (!isEnabled()) return;
        try {
            String url = find(sticker, new HashSet<Object>(), 0);
            if (url == null) return;
            latest = url;
            latestAt = android.os.SystemClock.uptimeMillis();
            // the sticker's own sheet gets a download icon beside its close button (sheetShown); without a sheet,
            // the floating button
            // TikTok passes here when a sticker loads too (opening a chat, scrolling): only a tap a moment ago counts
            main.removeCallbacks(fallback);
            if (latestAt - tappedAt < 450L) main.postDelayed(fallback, 900L);
        } catch (Throwable error) {
            Diary.note("sticker: " + error);
        }
    }

    /**
     * Every dialog TikTok shows passes here (DownloadChoice.show). One that opens right after a sticker was touched
     * is that sticker's sheet: a download icon goes next to its close button, at the top right.
     */
    static void sheetShown(android.app.Dialog dialog) {
        if (!isEnabled() || latest == null || android.os.SystemClock.uptimeMillis() - latestAt > 1500L
                || latestAt - tappedAt > 1500L) return;
        android.view.Window window = dialog.getWindow();
        if (window == null) return;
        final android.view.View decor = window.getDecorView();
        // the sheet fills in a moment after it is shown: tried a few times until its close button is there
        for (long delay : new long[]{120L, 350L, 700L})
            decor.postDelayed(() -> { if (attach(decor)) main.removeCallbacks(fallback); }, delay);
        main.removeCallbacks(fallback);
        main.postDelayed(fallback, 1200L);
    }

    private static final int ICON_TAG = 0x7e0c0360;

    private static boolean attach(android.view.View decor) {
        if (!(decor instanceof android.widget.FrameLayout)) return false;
        if (decor.findViewWithTag(ICON_TAG) != null) return true;
        final android.view.View close = closeButton(decor);
        if (close == null) return false;
        float density = decor.getResources().getDisplayMetrics().density;
        final int size = Math.round(36 * density);
        final android.widget.ImageView icon = new android.widget.ImageView(decor.getContext());
        int colour = inkOf(close);
        icon.setImageDrawable(SettingsGlyph.make("download", colour));
        icon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        int pad = Math.round(7 * density);
        icon.setPadding(pad, pad, pad, pad);
        // round, as the sheet's own buttons: the icon's colour, faint, behind it
        android.graphics.drawable.GradientDrawable round = new android.graphics.drawable.GradientDrawable();
        round.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        round.setColor((colour & 0x00FFFFFF) | 0x26000000);
        icon.setBackground(round);
        icon.setTag(ICON_TAG);
        icon.setContentDescription(Text.SAVE_STICKER);
        icon.setOnClickListener(v -> { v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK); save(); });
        Motion.touch(icon);
        icon.setVisibility(android.view.View.INVISIBLE);
        ((android.widget.FrameLayout) decor).addView(icon, new android.widget.FrameLayout.LayoutParams(size, size));
        // the sheet slides in and can be dragged: the icon follows the close button every frame
        final int gap = Math.round(8 * density);
        final int[] at = new int[2], base = new int[2];
        final android.view.ViewTreeObserver.OnPreDrawListener follow = () -> {
            if (!close.isAttachedToWindow() || !close.isShown()) { icon.setVisibility(android.view.View.INVISIBLE); return true; }
            close.getLocationInWindow(at);
            decor.getLocationInWindow(base);
            float x = at[0] - base[0] - size - gap, y = at[1] - base[1] + (close.getHeight() - size) / 2f;
            if (icon.getTranslationX() != x) icon.setTranslationX(x);
            if (icon.getTranslationY() != y) icon.setTranslationY(y);
            if (icon.getVisibility() != android.view.View.VISIBLE) icon.setVisibility(android.view.View.VISIBLE);
            return true;
        };
        decor.getViewTreeObserver().addOnPreDrawListener(follow);
        icon.addOnAttachStateChangeListener(new android.view.View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(android.view.View v) {}
            @Override public void onViewDetachedFromWindow(android.view.View v) {
                android.view.ViewTreeObserver tree = decor.getViewTreeObserver();
                if (tree.isAlive()) tree.removeOnPreDrawListener(follow);
            }
        });
        return true;
    }

    /**
     * The colour the close button is really drawn in: it is drawn into a small picture and its solid pixels averaged.
     * Its tint alone was not enough -- 47.7.3's has none, the icon came out white on the white sheet: there and working,
     * but not to be seen. Dark grey when nothing solid is drawn.
     */
    private static int inkOf(android.view.View close) {
        try {
            if (close instanceof android.widget.ImageView && ((android.widget.ImageView) close).getImageTintList() != null)
                return ((android.widget.ImageView) close).getImageTintList().getDefaultColor() | 0xFF000000;
            int w = Math.max(1, Math.min(close.getWidth(), 96)), h = Math.max(1, Math.min(close.getHeight(), 96));
            android.graphics.Bitmap shot = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(shot);
            android.graphics.drawable.Drawable mark = close instanceof android.widget.ImageView ? ((android.widget.ImageView) close).getDrawable() : null;
            if (mark != null) {
                // the cross itself, not the round backing behind it
                android.graphics.Rect was = new android.graphics.Rect(mark.getBounds());
                mark.setBounds(0, 0, w, h);
                mark.draw(canvas);
                mark.setBounds(was);
            } else {
                canvas.scale(w / (float) Math.max(1, close.getWidth()), h / (float) Math.max(1, close.getHeight()));
                close.draw(canvas);
            }
            long r = 0, g = 0, b = 0, n = 0;
            int[] pixels = new int[w * h];
            shot.getPixels(pixels, 0, w, 0, 0, w, h);
            shot.recycle();
            for (int p : pixels) if ((p >>> 24) > 200) { r += (p >> 16) & 255; g += (p >> 8) & 255; b += p & 255; n++; }
            if (n > 0) return 0xFF000000 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
        } catch (Throwable error) { Diary.note("sticker icon colour: " + error); }
        return 0xFF5C5C66;
    }

    /** The sheet's close button: named so, or else the highest small clickable picture, the furthest right. */
    private static android.view.View closeButton(android.view.View root) {
        android.view.View[] named = {null}, best = {null};
        walk(root, named, best, new int[2], new int[2]);
        return named[0] != null ? named[0] : best[0];
    }

    private static void walk(android.view.View view, android.view.View[] named, android.view.View[] best, int[] at, int[] other) {
        if (view.getVisibility() != android.view.View.VISIBLE || named[0] != null) return;
        float d = view.getResources().getDisplayMetrics().density;
        boolean small = view.getWidth() > 0 && view.getWidth() < Math.round(72 * d) && view.getHeight() < Math.round(72 * d);
        if (small && view.isClickable()) {
            CharSequence said = view.getContentDescription();
            String words = said == null ? "" : said.toString().toLowerCase(java.util.Locale.ROOT);
            if (words.contains("закры") || words.contains("close") || words.contains("закри")) { named[0] = view; return; }
            view.getLocationOnScreen(at);
            if (best[0] == null) best[0] = view;
            else {
                best[0].getLocationOnScreen(other);
                int slack = Math.round(12 * d);
                if (at[1] < other[1] - slack || Math.abs(at[1] - other[1]) <= slack && at[0] > other[0]) best[0] = view;
            }
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) walk(group.getChildAt(i), named, best, at, other);
        }
    }

    // ---------------------------------------------------------- the walk

    private static final int DEEP = 4;

    /** The best http address anywhere inside an object, or null. */
    private static String find(Object thing, Set<Object> seen, int depth) {
        if (thing == null || depth > DEEP) return null;
        if (thing instanceof String) {
            String value = (String) thing;
            return value.startsWith("http") ? value : null;
        }
        if (thing instanceof List) {
            String best = null;
            for (Object item : (List) thing) {
                String found = find(item, seen, depth + 1);
                if (found != null && (best == null || found.length() > best.length())) best = found;
            }
            return best;
        }

        Class<?> type = thing.getClass();
        String name = type.getName();
        // the framework and the language are not where a sticker's url is
        if (name.startsWith("java.") || name.startsWith("android.")) return null;
        if (!seen.add(thing)) return null;

        String best = null;
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                    field.setAccessible(true);
                    String found = find(field.get(thing), seen, depth + 1);
                    // the same picture arrives at three sizes and the longest
                    // url is reliably the largest of them
                    if (found != null && (best == null || found.length() > best.length())) {
                        best = found;
                    }
                } catch (Throwable ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return best;
    }

    // --------------------------------------------------------- saving it

    private static void save() {
        final String url = latest;
        final Context context = Ttcuz.context();
        if (url == null || context == null) return;
        if(!StoragePermissionActivity.ensure(context,()->save()))return;
        Net.away("sticker", new Runnable() {
            @Override
            public void run() {
                byte[] data = Net.bytes(url);
                String at = null;
                if (data != null) {
                    // what it is, from its first bytes: a GIF or a video is kept as it is (its own speed), an animated
                    // WebP becomes a GIF at its own speed, a still picture stays a picture
                    if (Gif.isGif(data)) at = Gallery.save(context, data, Gallery.name("sticker", url, "gif"), "image/gif");
                    else if (data.length > 12 && data[4] == 'f' && data[5] == 't' && data[6] == 'y' && data[7] == 'p')
                        at = Gallery.save(context, data, Gallery.name("sticker", url, "mp4"), "video/mp4");
                    else if (WebpFrames.animated(data)) {
                        byte[] gif = WebpFrames.toGif(data, 8 * 1024 * 1024);
                        at = gif != null ? Gallery.save(context, gif, Gallery.name("sticker", url, "gif"), "image/gif")
                                : Gallery.save(context, data, Gallery.name("sticker", url, "webp"), "image/webp");
                    } else {
                        boolean webp = data.length > 12 && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P';
                        at = Gallery.save(context, data, Gallery.name("sticker", url, webp ? "webp" : "png"), webp ? "image/webp" : "image/png");
                    }
                }
                Screen.say(at == null ? Text.STICKER_FAILED : Text.SAVED);
            }
        });
    }

    /** Whether there is anything to save, for the settings screen to ask. */
    public static boolean have() {
        return latest != null;
    }
}
