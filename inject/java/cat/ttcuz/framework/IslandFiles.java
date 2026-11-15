package cat.ttcuz.framework;

import android.content.Context;
import cat.ttcuz.island.Island;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;

/**
 * The islands a person can choose for the bottom bar: Java classes extending cat.ttcuz.island.Island.
 *
 * Two kinds. The examples come compiled with the mod (cat.ttcuz.island.examples) and their sources ride along in
 * assets/ttcuz/islands to start one's own from. One's own are written (or pasted, or opened) as a Java file and
 * compiled on the phone with the plugin editor's tools (ECJ and D8, PluginForge.buildIsland) into
 * files/ttcuz/islands/<id>/: the source, classes.dex (read-only, as Android wants loaded code) and meta.json.
 *
 * Not plugins: an island gets the bar to draw and the touches on it, nothing else of the mod's API. One is chosen at a
 * time ("" is the mod's own island); one that throws or brings TikTok down is switched off.
 */
final class IslandFiles {
    private IslandFiles() {}

    static final String KEY = "island_choice";
    static final String BUILTIN = "builtin:", OWN = "own:";
    /** The examples in the mod: class simple name → shown name. */
    static final String[][] EXAMPLES = {
            {"Starter", "Мой островок"}, {"Neon", "Неон"}, {"Dock", "Док"}, {"Compact", "Три кнопки"}, {"Aurora", "Аврора"}};

    private static volatile Island current;
    private static volatile boolean loaded;
    /** The class of the island drawing now, to know its frames in a crash. */
    private static volatile String drawingClass;

    static File folder(Context context) {
        File dir = new File(context.getFilesDir(), "ttcuz/islands");
        if (!dir.isDirectory()) dir.mkdirs();
        return dir;
    }

    /** One's own islands: {key, name, author}, by name. */
    static List<String[]> installed() {
        List<String[]> out = new ArrayList<>();
        Context context = Ttcuz.context();
        if (context == null) return out;
        File[] dirs = folder(context).listFiles(File::isDirectory);
        if (dirs == null) return out;
        for (File dir : dirs) {
            try {
                JSONObject meta = new JSONObject(read(new File(dir, "meta.json")));
                out.add(new String[]{OWN + dir.getName(), meta.optString("name", dir.getName()), meta.optString("author", "")});
            } catch (Throwable broken) { Diary.note("island " + dir.getName() + ": " + broken); }
        }
        out.sort((a, b) -> a[1].compareToIgnoreCase(b[1]));
        return out;
    }

    /** The chosen island's key, or "" for the mod's own. */
    static String chosen() {
        Context context = Ttcuz.context();
        if (context == null) return "";
        return context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).getString(KEY, "");
    }

    static String nameOf(String key) {
        if (key == null || key.isEmpty()) return Text.t("Островок мода");
        if (key.startsWith(BUILTIN)) for (String[] one : EXAMPLES) if (key.equals(BUILTIN + one[0])) return Text.t(one[1]);
        for (String[] one : installed()) if (one[0].equals(key)) return one[1];
        return key;
    }

    /** The island drawing now, or null for the mod's own. Made once per choice. */
    static Island active() {
        if (loaded) return current;
        Context context = Ttcuz.context();
        String key = chosen();
        Island made = null;
        if (context != null && !key.isEmpty()) {
            try { made = make(context, key); }
            catch (Throwable broken) { Diary.note("island " + key + ": " + broken); Screen.say(Text.t("Островок не запустился") + ": " + why(broken)); }
        }
        current = made;
        drawingClass = made == null ? null : made.getClass().getName();
        loaded = context != null;
        return made;
    }

    private static Island make(Context context, String key) throws Exception {
        if (key.startsWith(BUILTIN)) {
            String name = key.substring(BUILTIN.length());
            if (!name.matches("[A-Za-z]+")) throw new IllegalArgumentException(key);
            return (Island) Class.forName("cat.ttcuz.island.examples." + name).getDeclaredConstructor().newInstance();
        }
        if (!key.startsWith(OWN)) throw new IllegalArgumentException(key);
        File dir = new File(folder(context), key.substring(OWN.length()));
        JSONObject meta = new JSONObject(read(new File(dir, "meta.json")));
        File dex = new File(dir, "classes.dex");
        dex.setReadOnly();
        File cache = new File(context.getCodeCacheDir(), "ttcuz-islands");
        cache.mkdirs();
        ClassLoader loader = new dalvik.system.DexClassLoader(dex.getPath(), cache.getPath(), null, IslandFiles.class.getClassLoader());
        Object island = loader.loadClass(meta.getString("entry")).getDeclaredConstructor().newInstance();
        if (!(island instanceof Island)) throw new IllegalArgumentException(Text.t("Класс не наследует Island"));
        return (Island) island;
    }

    static String why(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String said = root.getMessage();
        return root.getClass().getSimpleName() + (said == null ? "" : ": " + said);
    }

    private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([A-Za-z_][\\w.]*)\\s*;"),
            CLASS = Pattern.compile("public\\s+(?:final\\s+)?class\\s+([A-Za-z_]\\w*)"),
            NAME = Pattern.compile("(?m)^\\s*//\\s*@name\\s+(.+?)\\s*$"), AUTHOR = Pattern.compile("(?m)^\\s*//\\s*@author\\s+(.+?)\\s*$");

    /** What a source says about itself: {package, class, name, author}. Throws when it has no public class. */
    static String[] describe(String source) {
        if (source == null || source.trim().isEmpty()) throw new IllegalArgumentException(Text.t("Пусто"));
        if (source.length() > 200 * 1024) throw new IllegalArgumentException(Text.t("Файл больше 200 КБ"));
        Matcher p = PACKAGE.matcher(source), c = CLASS.matcher(source), n = NAME.matcher(source), a = AUTHOR.matcher(source);
        String pkg = p.find() ? p.group(1) : "", cls = c.find() ? c.group(1) : null;
        if (cls == null) throw new IllegalArgumentException(Text.t("Нет public class … extends Island (или Styled)"));
        if (pkg.startsWith("cat.ttcuz")) throw new IllegalArgumentException(Text.t("Пакет cat.ttcuz.* занят модом: возьмите свой, например my.islands"));
        String name = n.find() ? n.group(1) : cls, author = a.find() ? a.group(1) : "";
        return new String[]{pkg, cls, name.length() > 40 ? name.substring(0, 40) : name, author.length() > 40 ? author.substring(0, 40) : author};
    }

    /**
     * Compiles an island on the phone and keeps it (a new one, or in place of `key`); returns its key. Runs on a
     * worker thread; throws with the compiler's words when the Java is wrong.
     */
    static String compile(Context context, String key, String source, PluginForge.Progress progress) throws Exception {
        String[] about = describe(source);
        String id = key != null && key.startsWith(OWN) ? key.substring(OWN.length())
                : about[2].toLowerCase(java.util.Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "-").replaceAll("^-|-$", "");
        if (id.isEmpty()) id = "island";
        String entry = about[0].isEmpty() ? about[1] : about[0] + "." + about[1];
        File built = PluginForge.buildIsland(context, entry, source, progress);
        File dir = new File(folder(context), id);
        dir.mkdirs();
        File dex = new File(dir, "classes.dex");
        if (dex.exists()) { dex.setWritable(true); dex.delete(); }
        if (!built.renameTo(dex)) copy(built, dex);
        write(new File(dir, "Island.java"), source.trim());
        write(new File(dir, "meta.json"), new JSONObject().put("name", about[2]).put("author", about[3]).put("entry", entry).toString());
        return OWN + id;
    }

    /** The source of an island: one's own as written, an example from the mod's assets. */
    static String source(Context context, String key) {
        try {
            if (key.startsWith(BUILTIN)) {
                try (java.io.InputStream in = context.getAssets().open("ttcuz/islands/" + key.substring(BUILTIN.length()) + ".java")) {
                    // an example opened to change becomes one's own, in one's own package
                    return readAll(in).replaceFirst("package cat\\.ttcuz\\.island\\.examples;", "package my.islands;");
                }
            }
            return read(new File(new File(folder(context), key.substring(OWN.length())), "Island.java"));
        } catch (Throwable missing) { return ""; }
    }

    static void delete(String key) {
        Context context = Ttcuz.context();
        if (context == null || !key.startsWith(OWN)) return;
        if (key.equals(chosen())) choose("");
        File dir = new File(folder(context), key.substring(OWN.length()));
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) { f.setWritable(true); f.delete(); }
        dir.delete();
    }

    /** "" for the mod's own island, or a key. */
    static void choose(String key) {
        Context context = Ttcuz.context();
        if (context != null) context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).edit().putString(KEY, key == null ? "" : key).commit();
        reload();
    }

    private static void reload() {
        Island old = current;
        current = null;
        loaded = false;
        if (old != null) try { old.onRelease(); } catch (Throwable ignored) {}
        active();
        new android.os.Handler(android.os.Looper.getMainLooper()).post(NavigationIsland::painterChanged);
    }

    /** The island threw while working: the mod's own island again, and a word why. */
    static void fail(Island island, Throwable error) {
        if (island != current) return;
        Diary.note("island: " + error);
        Screen.say(Text.t("Островок выключен из-за ошибки") + ": " + why(error));
        choose("");
    }

    // ------------------------------------------------------------ crashes

    /** The island it switched off after a crash, kept until the next start says so. */
    private static final String CRASHED = "island_crashed", DRAWN = "island_drawn_at";
    private static boolean drawnNoted;

    /** The chosen island began drawing in this process: a native crash after this moment may be its doing. */
    static void drawing() {
        if (drawnNoted) return;
        drawnNoted = true;
        Context context = Ttcuz.context();
        if (context != null) context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).edit().putLong(DRAWN, System.currentTimeMillis()).commit();
    }

    /**
     * TikTok is going down with a Java error. When an island file draws and the error passes through the island's
     * code (or is the stack running out, which is what a touch loop looks like), the island is switched off now,
     * written at once, so the next start has the mod's own island.
     */
    static void crashed(Context context, Throwable error) {
        if (context == null || error == null) return;
        android.content.SharedPreferences prefs = context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
        String chosen = prefs.getString(KEY, "");
        if (chosen.isEmpty() || !fromIsland(error)) return;
        prefs.edit().putString(KEY, "").putString(CRASHED, chosen).commit();
    }

    private static boolean fromIsland(Throwable error) {
        for (int depth = 0; error != null && depth < 6; depth++, error = error.getCause()) {
            if (error instanceof StackOverflowError) return true;
            for (StackTraceElement frame : error.getStackTrace()) {
                String name = frame.getClassName();
                if (name.startsWith("cat.ttcuz.framework.Island") || name.startsWith("cat.ttcuz.framework.NavigationIsland")
                        || name.startsWith("cat.ttcuz.island.") || (drawingClass != null && name.startsWith(drawingClass))) return true;
            }
        }
        return false;
    }

    /**
     * On the first screen after a start: a crash the island caused last time is told about; and a native crash
     * (a shader the GPU could not take, which no Java handler sees) that came while an island file was drawing
     * switches that island off too.
     */
    static void afterCrash(Context context) {
        if (context == null) return;
        android.content.SharedPreferences prefs = context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
        String chosen = prefs.getString(KEY, ""), off = prefs.getString(CRASHED, "");
        if (off.isEmpty() && !chosen.isEmpty() && android.os.Build.VERSION.SDK_INT >= 30) {
            try {
                android.app.ActivityManager manager = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                List<android.app.ApplicationExitInfo> exits = manager.getHistoricalProcessExitReasons(context.getPackageName(), 0, 1);
                long drawn = prefs.getLong(DRAWN, Long.MAX_VALUE);
                if (!exits.isEmpty()) {
                    android.app.ApplicationExitInfo last = exits.get(0);
                    if (last.getReason() == android.app.ApplicationExitInfo.REASON_CRASH_NATIVE && last.getTimestamp() > drawn) {
                        off = chosen;
                        prefs.edit().putString(KEY, "").commit();
                    }
                }
            } catch (Throwable error) { Diary.note("island exit check: " + error); }
        }
        prefs.edit().remove(DRAWN).apply();
        if (off.isEmpty()) return;
        prefs.edit().remove(CRASHED).apply();
        current = null;
        loaded = false;
        String name = off;
        name = nameOf(off);
        Diary.note("island " + off + " switched off after a crash");
        Screen.say(Text.t("Островок выключен: из-за него TikTok вылетел") + " — " + name);
    }

    static String read(File file) throws java.io.IOException {
        try (java.io.InputStream in = new java.io.FileInputStream(file)) { return readAll(in); }
    }

    private static String readAll(java.io.InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] block = new byte[8192];
        for (int n; (n = in.read(block)) > 0 && buffer.size() <= 256 * 1024; ) buffer.write(block, 0, n);
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void write(File file, String text) throws java.io.IOException {
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) { out.write(text.getBytes(StandardCharsets.UTF_8)); }
    }

    private static void copy(File from, File to) throws java.io.IOException {
        try (java.io.InputStream in = new java.io.FileInputStream(from); java.io.FileOutputStream out = new java.io.FileOutputStream(to)) {
            byte[] block = new byte[65536];
            for (int n; (n = in.read(block)) > 0; ) out.write(block, 0, n);
        }
    }
}
