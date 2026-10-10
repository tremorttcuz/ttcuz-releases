package cat.narezany.margyt;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Every picture in TikTok, replaceable.
 *
 * The problem to solve is naming. A drawable is asked for by a number, and the
 * names that number used to have were stripped out of this build long before
 * anybody saw it -- so a pack cannot say "replace the heart" and there is no
 * table anywhere that would translate it.
 *
 * What there is, is the path inside the apk. Android keeps it for every file
 * resource and will hand it over for a number: `Resources.getValue` answers
 * with `res/ap/a.png`, and that is the same string in everybody's copy of the
 * same TikTok. So a pack is a zip of files at those paths, and swapping one is
 * a lookup on the path the number resolves to.
 *
 * Which also makes a pack easy to build: the mod writes one out of the very
 * apk it is running from, so what a person edits is exactly what the app uses,
 * at the size it uses it.
 *
 * A pack belongs to the version it was drawn against. TikTok renames its own
 * resource files between releases, and a path that no longer exists is simply
 * a file nothing asks for -- so an old pack loses pictures rather than
 * breaking anything.
 */
public final class Textures {

    private Textures() {}

    public static final String KEY_ON = "textures_on";
    public static final String KEY_PACK = "textures_pack";

    /** What the file that describes a pack is called. */
    public static final String MANIFEST = "manifest.json";

    /** What a pack is called. A zip underneath, and nothing but a zip. */
    public static final String KIND = ".cuztx";
    private static final String LEGACY_KIND = ".margytex";

    // ---------------------------------------------------------- the switches

    private static volatile Boolean on;
    private static volatile String chosen;

    public static boolean isEnabled() {
        Boolean known = on;
        if (known != null) return known.booleanValue();
        SharedPreferences prefs = prefs();
        boolean value = prefs != null && prefs.getBoolean(KEY_ON, false);
        on = Boolean.valueOf(value);
        return value;
    }

    public static void setEnabled(boolean enabled) {
        on = Boolean.valueOf(enabled);
        SharedPreferences prefs = prefs();
        if (prefs != null) prefs.edit().putBoolean(KEY_ON, enabled).apply();
        forget();
    }

    /** Which pack is on. Only ever one: two packs would fight over a path. */
    public static String pack() {
        String known = chosen;
        if (known != null) return known;
        SharedPreferences prefs = prefs();
        String value = prefs == null ? "" : prefs.getString(KEY_PACK, "");
        chosen = value;
        return value;
    }

    public static void choose(String name) {
        chosen = name == null ? "" : name;
        SharedPreferences prefs = prefs();
        if (prefs != null) prefs.edit().putString(KEY_PACK, chosen).apply();
        forget();
    }

    private static void forget() {
        synchronized (drawn) {
            drawn.clear();
        }
        inside = null;
        looked = false;
        Accent.dropLikeCache();
        RecolourCache.clear();
    }

    // ------------------------------------------------------- what is installed

    /** Where packs are kept, one folder each. */
    public static File shelf(Context context) {
        return new File(context.getFilesDir(), "margyt/textures");
    }

    public static List<Pack> installed(Context context) {
        List<Pack> out = new ArrayList<Pack>();
        File[] all = shelf(context).listFiles();
        if (all == null) return out;
        for (File one : all) {
            if (!one.isFile() || !(one.getName().endsWith(KIND)
                    || one.getName().endsWith(LEGACY_KIND)
                    || one.getName().endsWith(".ttcuztex"))) continue;
            out.add(read(one));
        }
        return out;
    }

    /** A pack, as its own manifest describes it. */
    public static final class Pack {
        public final String file;
        public final String name;
        public final String author;
        public final String about;
        /** The TikTok it was drawn against, or empty for any. */
        public final String tiktok;

        Pack(String file, String name, String author, String about, String tiktok) {
            this.file = file;
            this.name = name;
            this.author = author;
            this.about = about;
            this.tiktok = tiktok;
        }

        /** Whether this pack is meant for the TikTok underneath. */
        public boolean fits() {
            return tiktok.length() == 0 || tiktok.equals(Version.TIKTOK);
        }
    }

    private static Pack read(File zip) {
        String name = zip.getName();
        String author = "";
        String about = "";
        String tiktok = "";
        try {
            ZipFile open = new ZipFile(zip);
            try {
                ZipEntry entry = open.getEntry(MANIFEST);
                if (entry != null) {
                    InputStream in = open.getInputStream(entry);
                    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int got;
                    while ((got = in.read(buffer)) > 0) out.write(buffer, 0, got);
                    in.close();
                    JSONObject said = new JSONObject(new String(out.toByteArray(), "UTF-8"));
                    name = said.optString("name", name);
                    author = said.optString("author", "");
                    about = said.optString("about", said.optString("description", ""));
                    tiktok = said.optString("tiktok", "");
                }
            } finally {
                open.close();
            }
        } catch (Throwable ignored) {
        }
        return new Pack(zip.getName(), name, author, about, tiktok);
    }

    /** Take a zip somebody picked and keep it. */
    public static boolean install(Context context, android.net.Uri uri) {
        try {
            File shelf = shelf(context);
            shelf.mkdirs();
            String name = "pack-" + System.currentTimeMillis() + KIND;
            File out = new File(shelf, name);

            InputStream in = context.getContentResolver().openInputStream(uri);
            if (in == null) return false;
            FileOutputStream sink = new FileOutputStream(out);
            byte[] buffer = new byte[16384];
            int got;
            while ((got = in.read(buffer)) > 0) sink.write(buffer, 0, got);
            sink.close();
            in.close();

            // a zip that holds nothing the app asks for is not a texture pack
            ZipFile test = new ZipFile(out);
            int pictures = 0;
            java.util.Enumeration<? extends ZipEntry> entries = test.entries();
            while (entries.hasMoreElements()) {
                if (entries.nextElement().getName().startsWith("res/")) pictures++;
            }
            test.close();
            if (pictures == 0) {
                out.delete();
                Diary.note("textures: nothing under res/ in that zip");
                return false;
            }
            Diary.note("textures: " + name + " installed, " + pictures + " pictures");
            return true;
        } catch (Throwable error) {
            Diary.note("textures: " + error);
            return false;
        }
    }

    public static void remove(Context context, String file) {
        try {
            new File(shelf(context), file).delete();
            if (file.equals(pack())) choose("");
            forget();
        } catch (Throwable ignored) {
        }
    }

    // -------------------------------------------------------- writing one out

    /**
     * Write every picture in the apk into a zip somebody can edit.
     *
     * Read out of the running apk rather than fetched: what comes out is
     * exactly what this build draws, at the sizes it draws it, and there is
     * nothing to keep in step with anything.
     */
    public static File export(Context context, boolean withXml) {
        try {
            String apk = context.getApplicationInfo().sourceDir;
            File out = new File(downloads(), "ttcuz-" + Version.TIKTOK
                    + (withXml ? "-full" : "") + KIND);
            File parent = out.getParentFile();
            if (parent != null) parent.mkdirs();

            ZipFile source = new ZipFile(apk);
            ZipOutputStream sink = new ZipOutputStream(new FileOutputStream(out));
            int written = 0;
            try {
                sink.putNextEntry(new ZipEntry(MANIFEST));
                sink.write(template().getBytes("UTF-8"));
                sink.closeEntry();

                List<ZipEntry> wanted = new ArrayList<ZipEntry>();
                java.util.Enumeration<? extends ZipEntry> entries = source.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (wanted(entry.getName(), withXml)) wanted.add(entry);
                }

                byte[] buffer = new byte[32768];
                for (ZipEntry entry : wanted) {
                    sink.putNextEntry(new ZipEntry(entry.getName()));
                    InputStream in = source.getInputStream(entry);
                    int got;
                    while ((got = in.read(buffer)) > 0) sink.write(buffer, 0, got);
                    in.close();
                    sink.closeEntry();
                    written++;
                    // counted up front so this can say how far along it is;
                    // without that it was a line of text that arrived when the
                    // work was already done and then stayed on the screen
                    if ((written % 40) == 0 || written == wanted.size()) {
                        Screen.progress(Text.TEXTURES_EXPORTING,
                                (int) (written * 100L / Math.max(1, wanted.size())));
                    }
                }
            } finally {
                sink.close();
                source.close();
            }
            Screen.progressDone();
            Diary.note("textures: " + written + " pictures written to " + out);
            return out;
        } catch (Throwable error) {
            Screen.progressGone();
            Diary.note("textures: " + error);
            return null;
        }
    }

    /**
     * Whether a file in the apk belongs in a pack.
     *
     * The pictures always. The compiled xml only if asked for: there are
     * thousands of them, most are layouts and selectors that nobody can edit
     * by hand, and the handful worth having -- the vector drawables -- are
     * not worth the other thousands by default.
     */
    private static boolean wanted(String name, boolean withXml) {
        boolean ours = name.startsWith("res/") || name.startsWith("assets/");
        if (!ours) return false;
        if (name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".jpg")) {
            return true;
        }
        // the animations, which are the moving parts: the heart filling in,
        // the spinners, the little flourishes. Text, and editable as text.
        if (name.endsWith(".json")) return true;
        return withXml && name.endsWith(".xml");
    }

    private static String template() {
        StringBuilder out = new StringBuilder();
        out.append("{\n");
        out.append("  \"name\": \"My pack\",\n");
        out.append("  \"author\": \"@you\",\n");
        out.append("  \"about\": \"What this pack changes\",\n");
        out.append("  \"tiktok\": \"").append(Version.TIKTOK).append("\",\n");
        out.append("  \"_note\": \"Replace any file under res/ and keep its path. ");
        out.append("Delete the ones you have not changed -- anything missing is ");
        out.append("simply left as TikTok drew it.\"\n");
        out.append("}\n");
        return out.toString();
    }

    private static File downloads() {
        File folder = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS);
        return new File(folder, "ttcuz");
    }

    // --------------------------------------------------------- swapping one in

    private static volatile ZipFile inside;
    private static volatile boolean looked;

    private static ZipFile open(Context context) {
        if (looked) return inside;
        looked = true;
        try {
            String name = pack();
            if (name.length() == 0) return null;
            File zip = new File(shelf(context), name);
            if (!zip.isFile()) return null;
            // a pack that names a TikTok it was drawn against is not opened on
            // any other one: the paths inside are that build's paths, and
            // swapping half a screenful of pictures is worse than none
            Pack said = read(zip);
            if (!said.fits()) {
                Diary.note("textures: " + said.name + " is for TikTok " + said.tiktok
                        + " and this is " + Version.TIKTOK);
                return null;
            }
            inside = new ZipFile(zip);
        } catch (Throwable error) {
            Diary.note("textures: " + error);
            inside = null;
        }
        return inside;
    }

    private static final Map<String, Bitmap> drawn = new HashMap<String, Bitmap>();

    /**
     * The picture a pack has for this resource, or nothing.
     *
     * The number is turned into the path it lives at inside the apk, and that
     * path is what the pack is keyed by. Anything the pack does not carry
     * answers null at once, which is the common case and has to be cheap.
     */
    public static Drawable forResource(Context context, int id) {
        if (!isEnabled() || context == null || id == 0) return null;
        try {
            ZipFile zip = open(context);
            if (zip == null) return null;

            TypedValue where = new TypedValue();
            context.getResources().getValue(id, where, true);
            if (where.string == null) return null;
            String path = where.string.toString();
            if (!path.startsWith("res/")) return null;

            Bitmap known;
            synchronized (drawn) {
                if (drawn.containsKey(path)) known = drawn.get(path);
                else known = null;
                if (known != null) return new BitmapDrawable(context.getResources(), known);
                if (drawn.containsKey(path)) return null;   // looked, not there
            }

            ZipEntry entry = instead(zip, path);
            Bitmap bitmap = null;
            if (entry != null) {
                InputStream in = zip.getInputStream(entry);
                bitmap = BitmapFactory.decodeStream(in);
                in.close();
            }
            synchronized (drawn) {
                drawn.put(path, bitmap);
            }
            return bitmap == null ? null : new BitmapDrawable(context.getResources(), bitmap);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * The entry a pack offers for a path, allowing a picture for a vector.
     *
     * A vector drawable is compiled xml and nobody edits one by hand, so a
     * pack that wants to replace the heart puts a png at the heart's path with
     * the extension changed. Which is the only way a person can replace one of
     * those at all.
     */
    private static ZipEntry instead(ZipFile zip, String path) {
        ZipEntry entry = zip.getEntry(path);
        if (entry != null) return entry;
        if (!path.endsWith(".xml")) return null;
        String bare = path.substring(0, path.length() - 4);
        for (String kind : new String[]{".png", ".webp", ".jpg"}) {
            entry = zip.getEntry(bare + kind);
            if (entry != null) return entry;
        }
        return null;
    }

    /** A raw resource a pack has replaced -- an animation, usually. */
    public static InputStream stream(Context context, int id) {
        if (!isEnabled() || context == null || id == 0) return null;
        try {
            ZipFile zip = open(context);
            if (zip == null) return null;
            TypedValue where = new TypedValue();
            context.getResources().getValue(id, where, true);
            if (where.string == null) return null;
            return read(zip, where.string.toString());
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** The same, for something read out of the apk's assets by name. */
    public static InputStream asset(String name) {
        if (!isEnabled() || name == null) return null;
        try {
            Context context = Margy.context();
            ZipFile zip = open(context);
            if (zip == null) return null;
            return read(zip, "assets/" + name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static InputStream read(ZipFile zip, String path) {
        try {
            ZipEntry entry = zip.getEntry(path);
            if (entry == null) return null;
            // copied out rather than handed over: the caller closes the stream
            // and closing one of these would close the whole pack with it
            InputStream in = zip.getInputStream(entry);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[16384];
            int got;
            while ((got = in.read(buffer)) > 0) out.write(buffer, 0, got);
            in.close();
            return new java.io.ByteArrayInputStream(out.toByteArray());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static SharedPreferences prefs() {
        Context context = Margy.context();
        if (context == null) return null;
        return context.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE);
    }
}
