package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.widget.TextView;
import java.io.File;

/**
 * One typeface, everywhere TikTok writes.
 *
 * There is no single place an app's font is decided, so this works from two
 * ends. Anywhere TikTok sets a typeface itself the call is rewritten to come
 * through here and is answered with the chosen one. And every piece of text on
 * its way into a view already passes the mod -- that is how a badge becomes a
 * picture -- so the font is set there too, which reaches the great many views
 * that never ask for a typeface at all and simply inherit one.
 *
 * The choices are the ones the phone already has, plus any `.ttf` or `.otf`
 * file you point at: it is copied into the mod's own folder, because a file
 * picked out of Downloads is a borrowed handle that will not be readable on
 * the next start.
 */
public final class Fonts {

    private Fonts() {}

    public static final String KEY = "font";
    public static final String KEY_EMOJI = "font_emoji";

    /** Bundled packs are listed by the manifest; custom fonts use the picker. */
    public static final String TWEMOJI = "twemoji";
    public static final String NOTO = "noto";
    public static final String BLOBMOJI = "blobmoji";
    public static final String APPLE_MFFM = "apple_mffm";
    public static final String EMOJI_FILE = "emoji_file";

    /**
     * Every pack rides inside the apk. Nothing is fetched, ever.
     *
     * The manifest maps each choice to an asset, with no runtime download.
     */
    public static final String TWEMOJI_ASSET = "ttcuz/twemoji.ttf";

    private static String packAsset(Context context, String which) {
        return EmojiPackManager.asset(context, which);
    }

    /** The names of the ones that need no file. */
    public static final String SYSTEM = "";
    public static final String SANS = "sans-serif";
    public static final String SANS_LIGHT = "sans-serif-light";
    public static final String SANS_CONDENSED = "sans-serif-condensed";
    public static final String SERIF = "serif";
    public static final String MONOSPACE = "monospace";
    public static final String CURSIVE = "cursive";
    public static final String FILE = "file";

    public static final String[] EMOJI_PACKS = { SYSTEM, TWEMOJI, NOTO, BLOBMOJI, APPLE_MFFM };

    public static final String[] PRESETS = {
        SYSTEM,
        SANS,
        SANS_LIGHT,
        SANS_CONDENSED,
        SERIF,
        MONOSPACE,
        CURSIVE,
    };

    private static volatile String chosen;
    private static volatile String chosenEmoji;
    private static volatile Typeface face;
    private static volatile boolean looked;

    // ---------------------------------------------------------- the choice

    public static String name() {
        String known = chosen;
        if (known != null) return known;
        SharedPreferences prefs = prefs();
        if (prefs == null) return SYSTEM;
        String value = prefs.getString(KEY, SYSTEM);
        chosen = value;
        return value;
    }

    public static void choose(String value) {
        if (value == null) value = SYSTEM;
        chosen = value;
        forget();
        SharedPreferences prefs = prefs();
        if (prefs != null) prefs.edit().putString(KEY, value).commit();
        new android.os.Handler(android.os.Looper.getMainLooper()).post(Themes::repaintVisible);
    }

    public static String emoji() {
        String known = chosenEmoji;
        if (known != null) return known;
        SharedPreferences prefs = prefs();
        if (prefs == null) return SYSTEM;
        String value = prefs.getString(KEY_EMOJI, SYSTEM);
        chosenEmoji = value;
        return value;
    }

    public static void chooseEmoji(Context context, String value) {
        if (value == null) value = SYSTEM;
        if (context == null) context = Ttcuz.context();
        if (EmojiPackManager.find(context, value) == null) value = SYSTEM;
        chosenEmoji = value;
        forget();
        SharedPreferences prefs = prefs();
        if (prefs != null) prefs.edit().putString(KEY_EMOJI, value).commit();
        new android.os.Handler(android.os.Looper.getMainLooper()).post(Themes::repaintVisible);
    }

    private static void forget() {
        face = null;
        looked = false;
        styleBase = null;
        styled = new Typeface[4];
        emojiTypeface = null;
        emojiLoaded = false;
    }

    static void reload() {
        chosen = null;
        chosenEmoji = null;
        forget();
    }

    /** Every pack is in the apk, so one is ready the moment it is chosen. */
    public static boolean emojiReady(Context context) {
        return EmojiPackManager.ready(context, emoji());
    }

    /**
     * Twemoji as a typeface, straight out of the apk.
     *
     * Used for the flags beside the countries whatever else is chosen: a flag
     * is the one emoji where the phone's own is often a pair of letters in a
     * box, and this one is always a flag.
     */
    public static Typeface twemoji(Context context) {
        Typeface known = bundled;
        if (known != null) return known;
        try {
            String asset = packAsset(context, TWEMOJI);
            if (asset == null) return null;
            known = Typeface.createFromAsset(context.getAssets(), asset);
            bundled = known;
            return known;
        } catch (Throwable error) {
            Diary.note("twemoji: " + error);
            return null;
        }
    }

    private static volatile Typeface bundled;
    private static Typeface emojiTypeface;
    private static String emojiTypefacePack;
    private static volatile boolean emojiLoaded;

    static synchronized Typeface emojiTypeface(Context context, String which) {
        if (
            context == null || SYSTEM.equals(which) || android.os.Build.VERSION.SDK_INT < 29
        ) return null;
        if (emojiLoaded && which.equals(emojiTypefacePack)) return emojiTypeface;
        emojiTypefacePack = which;
        emojiLoaded = false;
        emojiTypeface = null;
        try {
            if (!EmojiPackManager.ready(context, which)) return null;
            String asset = packAsset(context, which);
            emojiTypeface =
                asset == null
                    ? Typeface.createFromFile(emojiFile(context, which))
                    : Typeface.createFromAsset(context.getAssets(), asset);
            emojiLoaded = emojiTypeface != null;
        } catch (Throwable error) {
            Diary.note("emoji font: " + error);
        }
        return emojiTypeface;
    }

    private static volatile boolean warmingEmoji;

    static void warmEmoji() {
        if (warmingEmoji || emojiLoaded || SYSTEM.equals(emoji()) || android.os.Build.VERSION.SDK_INT < 29) return;
        Context context = Ttcuz.context();
        if (context == null) return;
        warmingEmoji = true;
        Net.away("emoji font warmup", () -> {
            try {
                if (emojiTypeface(context, emoji()) != null) EmojiText.warm(context);
            } finally { warmingEmoji = false; }
        });
    }

    private static void applyEmoji(TextView view) {
        EmojiText.apply(view, emojiTypeface(view.getContext(), emoji()));
    }

    /** Where a font picked out of the phone's storage is kept. */
    public static File file(Context context) {
        return AccountAppearance.file(context, "font");
    }

    /**
     * Take a font the person picked.
     *
     * Copied rather than remembered: the uri a picker hands over is readable
     * now and not after a restart, and a font that vanishes overnight would be
     * a mystery rather than a setting.
     */
    public static boolean take(Context context, android.net.Uri uri) {
        return take(context, uri, false);
    }

    public static boolean take(Context context, android.net.Uri uri, boolean forEmoji) {
        final SharedPreferences destination = AccountAppearance.prefs(context);
        final String owner = AccountAppearance.scope(context);
        File temporary = null;
        try {
            File out = forEmoji ? emojiFile(context, EMOJI_FILE) : file(context);
            File parent = out.getParentFile();
            if (parent != null) parent.mkdirs();
            temporary = new File(parent, out.getName() + ".tmp");
            java.io.InputStream in = context.getContentResolver().openInputStream(uri);
            if (in == null) return false;
            java.io.FileOutputStream sink = new java.io.FileOutputStream(temporary);
            try {
                byte[] buffer = new byte[16384];
                int read;
                long total = 0;
                while ((read = in.read(buffer)) > 0) {
                    total += read;
                    if (total > (forEmoji ? 64L : 24L) * 1024 * 1024) return false;
                    sink.write(buffer, 0, read);
                }
            } finally {
                sink.close();
                in.close();
            }

            // refuse it here rather than have every screen fall back silently
            Typeface test = Typeface.createFromFile(temporary);
            if (test == null) return false;
            if (!temporary.renameTo(out)) return false;
            if (
                !destination
                    .edit()
                    .putString(forEmoji ? KEY_EMOJI : KEY, forEmoji ? EMOJI_FILE : FILE)
                    .commit()
            ) return false;
            if (owner.equals(AccountAppearance.scope(context))) reload();
            return true;
        } catch (Throwable error) {
            Diary.note("font: " + error);
            return false;
        } finally {
            if (temporary != null && temporary.isFile()) temporary.delete();
        }
    }

    static File emojiFile(Context context, String which) {
        if (EMOJI_FILE.equals(which)) return AccountAppearance.file(context, "emoji-custom");
        return new File(context.getFilesDir(), "ttcuz/emoji-" + which);
    }

    // ----------------------------------------------------------- using it

    /** The chosen typeface, or null to leave whatever was there alone. */
    public static Typeface chosenFace() {
        if (looked) return face;
        looked = true;
        face = build();
        return face;
    }

    /** Text and emoji have independent typefaces; EmojiText owns emoji spans. */
    private static Typeface build() {
        Context context = Ttcuz.context();
        if (context == null) looked = false;
        return build(context, name());
    }

    /** Preview keeps the selected text font; the sample applies emoji spans separately. */
    public static Typeface preview(Context context, String emojiPack) {
        return build(context, name());
    }

    private static Typeface build(Context context, String letters) {
        try {
            if (FILE.equals(letters)) {
                File font = context == null ? null : file(context);
                return font != null && font.isFile() ? Typeface.createFromFile(font) : null;
            }
            if (!SYSTEM.equals(letters)) return Typeface.create(letters, Typeface.NORMAL);
        } catch (Throwable error) {
            Diary.note("font: " + error);
        }
        return null;
    }

    /**
     * Set the font on a view that is about to show text.
     *
     * Keeps the weight the view already had: a bold name stays bold, because
     * what is being changed is the shape of the letters and not the emphasis.
     */
    private static final class OwnedFont {

        Typeface original, applied;

        OwnedFont(Typeface original) {
            this.original = original;
        }
    }

    private static final java.util.Map<TextView, OwnedFont> ownedFonts =
        java.util.Collections.synchronizedMap(new java.util.WeakHashMap<TextView, OwnedFont>());
    private static Typeface styleBase;
    private static Typeface[] styled = new Typeface[4];

    public static void applyTree(android.view.View root) {
        if (chosenFace() == null && ownedFonts.isEmpty() && !EmojiText.active()) return;
        applyTree(root, 0, new int[] { 4000 });
    }

    private static void applyTree(android.view.View root, int depth, int[] budget) {
        if (root == null || depth > 40 || --budget[0] < 0) return;
        if (root instanceof TextView) apply((TextView) root);
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount() && budget[0] > 0; i++) applyTree(
                group.getChildAt(i),
                depth + 1,
                budget
            );
        }
    }

    public static void apply(TextView view) {
        if (view == null) return;
        String kind = view.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        if ((kind.contains("icon") && !kind.contains("emoticon"))) return;
        try {
            applyEmoji(view);
            Typeface wanted = chosenFace(),
                had = view.getTypeface();
            OwnedFont mine = ownedFonts.get(view);
            if (wanted == null) {
                if (mine != null) {
                    if (had == mine.applied) view.setTypeface(mine.original);
                    ownedFonts.remove(view);
                }
                return;
            }
            if (mine == null) {
                mine = new OwnedFont(had);
                ownedFonts.put(view, mine);
            } else if (had != mine.applied) mine.original = had;
            Typeface out = keep(wanted, mine.original);
            if (out != had) view.setTypeface(out);
            mine.applied = out;
        } catch (Throwable ignored) {}
    }

    public static void setText(TextView view, int resource) {
        view.setText(resource);
        apply(view);
    }

    public static void setHint(TextView view, int resource) {
        view.setHint(resource);
        apply(view);
    }

    public static void setHint(TextView view, CharSequence text) {
        view.setHint(text);
        apply(view);
    }

    public static void setTextAppearance(TextView view, int resource) {
        view.setTextAppearance(resource);
        apply(view);
    }

    public static void setTextAppearance(TextView view, Context context, int resource) {
        view.setTextAppearance(context, resource);
        apply(view);
    }

    // --------------------------------------------- where TikTok sets one

    public static void setTypeface(TextView view, Typeface face) {
        Typeface wanted = chosenFace();
        String kind = view.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        boolean replace = wanted != null && !(kind.contains("icon") && !kind.contains("emoticon"));
        view.setTypeface(replace ? keep(wanted, face) : face);
        if (replace) {
            OwnedFont mine = new OwnedFont(face);
            mine.applied = view.getTypeface();
            ownedFonts.put(view, mine);
        } else ownedFonts.remove(view);
        if (!(kind.contains("icon") && !kind.contains("emoticon"))) applyEmoji(view);
    }

    public static void setTypeface(TextView view, Typeface face, int style) {
        Typeface wanted = chosenFace();
        String kind = view.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        boolean replace = wanted != null && !(kind.contains("icon") && !kind.contains("emoticon"));
        view.setTypeface(replace ? wanted : face, style);
        if (replace) {
            OwnedFont mine = new OwnedFont(Typeface.create(face, style));
            mine.applied = view.getTypeface();
            ownedFonts.put(view, mine);
        } else ownedFonts.remove(view);
        if (!(kind.contains("icon") && !kind.contains("emoticon"))) applyEmoji(view);
    }

    public static Typeface setTypeface(android.graphics.Paint paint, Typeface face) {
        Typeface wanted = chosenFace();
        return paint.setTypeface(wanted == null ? face : keep(wanted, face));
    }

    /** Ours, at the weight theirs was going to be. */
    private static Typeface keep(Typeface wanted, Typeface theirs) {
        try {
            int style = theirs == null ? Typeface.NORMAL : theirs.getStyle();
            if (style == Typeface.NORMAL) return wanted;
            if (style < 0 || style >= 4) return Typeface.create(wanted, style);
            synchronized (Fonts.class) {
                if (styleBase != wanted) {
                    styleBase = wanted;
                    styled = new Typeface[4];
                }
                if (styled[style] == null) styled[style] = Typeface.create(wanted, style);
                return styled[style];
            }
        } catch (Throwable ignored) {
            return wanted;
        }
    }

    private static SharedPreferences prefs() {
        Context context = Ttcuz.context();
        if (context == null) return null;
        return AccountAppearance.prefs(context);
    }
}
