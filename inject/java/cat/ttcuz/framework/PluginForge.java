package cat.ttcuz.framework;

import android.content.Context;
import android.util.Base64;
import dalvik.system.DexClassLoader;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Builds a plugin on the phone: the editor's Java goes through ECJ into class files, D8 turns those into a dex,
 * and the dex and the manifest are packed as a .ctt the engine installs like any other.
 *
 * ECJ and D8 ride in the apk as dex jars (assets/ttcuz/plugin-build). The one thing that cannot ride along is
 * android.jar, Google's to hand out: it is read straight out of Google's platform zip with HTTP ranges, so the
 * phone downloads the 25 MB of android.jar rather than the 64 MB archive, once, and checks it against its
 * SHA-256. Both compilers run inside TikTok on a worker thread; a failure is reported, never an exit.
 */
final class PluginForge {
    interface Progress { void step(String words); }

    private static final String PLATFORM = "https://dl.google.com/android/repository/platform-35_r02.zip";
    // where android-35/android.jar sits in that zip, and what it must be once inflated
    private static final long JAR_AT = 7548111L;
    private static final long JAR_PACKED = 25011700L;
    private static final long JAR_SIZE = 27092450L;
    private static final String JAR_SHA = "4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b";
    private static final String[] TOOLS = {"ecj.jar", "d8.jar", "sdk.jar"};
    private static final Object LOCK = new Object();
    private static ClassLoader ecj, d8;

    private PluginForge() {}

    private static File tools(Context context) {
        File dir = new File(context.getFilesDir(), "ttcuz-plugin-build");
        dir.mkdirs();
        return dir;
    }

    /** Whether the first build still has android.jar to download. */
    static boolean needsDownload(Context context) {
        File jar = new File(tools(context), "android.jar");
        if (!jar.isFile() || jar.length() != JAR_SIZE) return true;
        for (String[] one : REMOTE) { File tool = new File(tools(context), one[0]); if (!tool.isFile() || tool.length() != Long.parseLong(one[1])) return true; }
        return false;
    }

    /** Builds the editor's draft into a .ctt in the cache and returns it. */
    static File build(Context context, JSONObject draft, Progress progress) throws Exception {
        synchronized (LOCK) {
            File tools = tools(context);
            progress.step(Text.t("Подготовка инструментов…"));
            for (String tool : TOOLS) tool(context, tool, new File(tools, tool), progress);
            File android = new File(tools, "android.jar");
            if (android.length() != JAR_SIZE) fetchAndroidJar(android, progress);

            JSONObject meta = draft.getJSONObject("metadata");
            File work = new File(context.getCacheDir(), "ttcuz-plugin-build");
            PluginPackage.delete(work);
            File src = new File(work, "src"), classes = new File(work, "classes"), dex = new File(work, "dex");
            if (!classes.mkdirs() | !dex.mkdirs()) throw new IOException(Text.t("Не удалось подготовить папку сборки"));
            List<String> sources = new ArrayList<>();
            JSONArray files = draft.getJSONArray("files");
            for (int i = 0; i < files.length(); i++) {
                JSONObject file = files.getJSONObject(i);
                File out = new File(src, file.getString("path").substring("java/".length()));
                if (!out.getCanonicalPath().startsWith(src.getCanonicalPath() + File.separator))
                    throw new IOException(Text.t("Некорректный или повторяющийся путь Java-файла"));
                out.getParentFile().mkdirs();
                write(out, file.getString("text").getBytes(StandardCharsets.UTF_8));
                sources.add(out.getAbsolutePath());
            }

            progress.step(Text.t("Компиляция Java…"));
            compile(tools, android, classes, sources);
            File entry = new File(classes, meta.getString("entry").replace('.', '/') + ".class");
            if (!entry.isFile())
                throw new IOException(Text.t("Главный класс не найден среди скомпилированных") + ": " + meta.getString("entry"));

            progress.step(Text.t("Перевод в dex…"));
            List<String> compiled = new ArrayList<>();
            collect(classes, compiled);
            dex(tools, android, dex, compiled);
            File classesDex = new File(dex, "classes.dex");
            if (!classesDex.isFile()) throw new IOException(Text.t("D8 не создал classes.dex"));
            if (classesDex.length() > 20L * 1024 * 1024) throw new IOException(Text.t("classes.dex больше 20 МБ"));

            progress.step(Text.t("Упаковка…"));
            JSONObject manifest = new JSONObject(meta.toString());
            if (!manifest.has("api")) manifest.put("api", 1);
            File ctt = new File(work, meta.getString("id") + ".ctt");
            try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(ctt)))) {
                zip.putNextEntry(new ZipEntry("manifest.json"));
                zip.write(manifest.toString(2).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry("classes.dex"));
                copy(new java.io.FileInputStream(classesDex), zip, null);
                zip.closeEntry();
                String icon = draft.optString("icon");
                if (!icon.isEmpty()) {
                    zip.putNextEntry(new ZipEntry("icon.png"));
                    zip.write(Base64.decode(icon, Base64.DEFAULT));
                    zip.closeEntry();
                }
            }
            return ctt;
        }
    }

    /**
     * Builds an island (one Java file whose public class extends cat.ttcuz.island.Island) into a dex with the same
     * tools; returns classes.dex in the cache. The island API comes from sdk.jar, so only the island's own classes
     * are in the dex.
     */
    static File buildIsland(Context context, String entry, String source, Progress progress) throws Exception {
        synchronized (LOCK) {
            File tools = tools(context);
            progress.step(Text.t("Подготовка инструментов…"));
            for (String tool : TOOLS) tool(context, tool, new File(tools, tool), progress);
            File android = new File(tools, "android.jar");
            if (android.length() != JAR_SIZE) fetchAndroidJar(android, progress);
            File work = new File(context.getCacheDir(), "ttcuz-island-build");
            PluginPackage.delete(work);
            File src = new File(work, "src"), classes = new File(work, "classes"), dex = new File(work, "dex");
            if (!classes.mkdirs() | !dex.mkdirs()) throw new IOException(Text.t("Не удалось подготовить папку сборки"));
            File out = new File(src, entry.replace('.', '/') + ".java");
            out.getParentFile().mkdirs();
            write(out, source.getBytes(StandardCharsets.UTF_8));
            List<String> sources = new ArrayList<>();
            sources.add(out.getAbsolutePath());
            progress.step(Text.t("Компиляция Java…"));
            compile(tools, android, classes, sources);
            if (!new File(classes, entry.replace('.', '/') + ".class").isFile())
                throw new IOException(Text.t("Главный класс не найден среди скомпилированных") + ": " + entry);
            progress.step(Text.t("Перевод в dex…"));
            List<String> compiled = new ArrayList<>();
            collect(classes, compiled);
            dex(tools, android, dex, compiled);
            File classesDex = new File(dex, "classes.dex");
            if (!classesDex.isFile()) throw new IOException(Text.t("D8 не создал classes.dex"));
            if (classesDex.length() > 4L * 1024 * 1024) throw new IOException(Text.t("Островок больше 4 МБ"));
            return classesDex;
        }
    }

    // ------------------------------------------------------------------ the tools

    /**
     * The compilers are not in the apk (they are 8 MB nobody needs until the first plugin or island): fetched once from
     * the mod's release, checked against size and SHA-256, and kept. {name, size, sha256}.
     */
    private static final String TOOLS_URL = "https://github.com/tremorttcuz/ttcuz-releases/releases/download/tools-1/";
    private static final String[][] REMOTE = {
            {"d8.jar", "6468675", "a105ba026a643948dc4d0f6a6cdcbb5c8227130ad765ec2925bb8fb08d984b66"},
            {"ecj.jar", "1305373", "06eea51bd75a9a3bbf605eae3979ac6268517375a279837b2498173686451e34"}};

    /** A tool ready in `target`: from the apk when it carries it, else downloaded (once) and checked. */
    private static void tool(Context context, String name, File target, final Progress progress) throws Exception {
        String[] remote = null;
        for (String[] one : REMOTE) if (one[0].equals(name)) remote = one;
        String[] carried = context.getAssets().list("ttcuz/plugin-build");
        boolean inApk = carried != null && java.util.Arrays.asList(carried).contains(name);
        if (remote == null || inApk) { unpack(context, name, target); return; }
        long size = Long.parseLong(remote[1]);
        if (target.isFile() && target.length() == size) return;
        progress.step(Text.t("Скачивание компилятора…"));
        File partial = new File(target.getPath() + ".part");
        partial.delete();
        boolean got = Net.download(TOOLS_URL + name, partial, (percent, have, total) ->
                progress.step(Text.t("Скачивание компилятора…") + " " + Math.min(100, have * 100 / Math.max(1, size)) + "%"));
        if (!got || partial.length() != size || !remote[2].equals(sha256(partial))) {
            partial.delete();
            throw new IOException(Text.t("Не удалось скачать компилятор. Проверьте интернет и попробуйте ещё раз."));
        }
        if (target.exists()) { target.setWritable(true); target.delete(); }
        if (!partial.renameTo(target)) throw new IOException(Text.t("Не удалось подготовить инструменты сборки"));
        target.setReadOnly();
        if (name.equals("ecj.jar")) ecj = null;
        if (name.equals("d8.jar")) d8 = null;
    }

    private static String sha256(File file) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new java.io.FileInputStream(file)) {
            byte[] block = new byte[65536];
            for (int n; (n = in.read(block)) > 0; ) sha.update(block, 0, n);
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : sha.digest()) hex.append(String.format(Locale.ROOT, "%02x", b));
        return hex.toString();
    }

    /** Copies a tool out of the apk when it is missing or the apk carries a different one; loaders want it read-only. */
    private static void unpack(Context context, String name, File target) throws IOException {
        String asset = "ttcuz/plugin-build/" + name;
        long size;
        try (android.content.res.AssetFileDescriptor fd = context.getAssets().openFd(asset)) { size = fd.getLength(); }
        catch (IOException compressed) { size = -1; }
        if (target.isFile() && size >= 0 && target.length() == size) return;
        File partial = new File(target.getPath() + ".part");
        try (InputStream in = context.getAssets().open(asset); OutputStream out = new FileOutputStream(partial)) {
            copy(in, out, null);
        }
        target.delete();
        if (!partial.renameTo(target)) throw new IOException(Text.t("Не удалось подготовить инструменты сборки"));
        target.setReadOnly();
        if (name.equals("ecj.jar")) ecj = null;
        if (name.equals("d8.jar")) d8 = null;
    }

    private static ClassLoader loader(File jar) {
        File opt = new File(jar.getParentFile(), "oat");
        opt.mkdirs();
        // the boot loader as parent: TikTok's own classes stay out of the compilers' way
        return new DexClassLoader(jar.getAbsolutePath(), opt.getAbsolutePath(), null, Object.class.getClassLoader());
    }

    /** android.jar, inflated straight out of Google's platform zip from the bytes it occupies there. */
    private static void fetchAndroidJar(File target, Progress progress) throws Exception {
        progress.step(Text.t("Загрузка android.jar с Google (25 МБ, один раз)…"));
        File partial = new File(target.getPath() + ".part");
        HttpURLConnection connection = (HttpURLConnection) new URL(PLATFORM).openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(30000);
        // raw bytes: Android asks for gzip by default and then cannot unpack a range of a zip
        connection.setRequestProperty("Accept-Encoding", "identity");
        connection.setRequestProperty("Range", "bytes=" + JAR_AT + "-" + (JAR_AT + 30 + 512 + JAR_PACKED - 1));
        try {
            if (connection.getResponseCode() != 206) throw new IOException("HTTP " + connection.getResponseCode());
            try (InputStream raw = new java.io.BufferedInputStream(connection.getInputStream(), 65536)) {
                byte[] head = new byte[30];
                readFully(raw, head);
                if (head[0] != 'P' || head[1] != 'K' || head[2] != 3 || head[3] != 4 || (head[8] & 255) != 8)
                    throw new IOException(Text.t("Google изменил архив платформы"));
                // the local header's name and extra field come before the data
                long skip = ((head[26] & 255) | (head[27] & 255) << 8) + ((head[28] & 255) | (head[29] & 255) << 8);
                while (skip > 0) { long n = raw.skip(skip); if (n <= 0) throw new IOException("EOF"); skip -= n; }
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                Inflater inflater = new Inflater(true);
                try (InputStream in = new InflaterInputStream(raw, inflater, 65536);
                     OutputStream out = new FileOutputStream(partial)) {
                    final long[] last = {0};
                    copy(in, out, (done) -> {
                        sha.update(done.bytes, 0, done.count);
                        long now = done.total * 100 / JAR_SIZE;
                        if (now != last[0]) {
                            last[0] = now;
                            progress.step(Text.t("Загрузка android.jar с Google (25 МБ, один раз)…") + " " + now + "%");
                        }
                    });
                } finally {
                    inflater.end();
                }
                StringBuilder hex = new StringBuilder();
                for (byte b : sha.digest()) hex.append(String.format(Locale.ROOT, "%02x", b & 255));
                if (partial.length() != JAR_SIZE || !JAR_SHA.contentEquals(hex)) {
                    partial.delete();
                    throw new IOException(Text.t("android.jar не прошёл проверку SHA-256"));
                }
            }
        } finally {
            connection.disconnect();
        }
        if (!partial.renameTo(target)) throw new IOException(Text.t("Не удалось сохранить android.jar"));
    }

    private static void compile(File tools, File android, File classes, List<String> sources) throws Exception {
        if (ecj == null) ecj = loader(new File(tools, "ecj.jar"));
        List<String> args = new ArrayList<>();
        String[] options = {"-1.8", "-proc:none", "-nowarn", "-encoding", "UTF-8",
                "-bootclasspath", android.getAbsolutePath(), "-cp", new File(tools, "sdk.jar").getAbsolutePath(),
                "-d", classes.getAbsolutePath()};
        for (String option : options) args.add(option);
        args.addAll(sources);
        StringWriter said = new StringWriter();
        PrintWriter writer = new PrintWriter(said);
        Class<?> main = ecj.loadClass("org.eclipse.jdt.internal.compiler.batch.Main");
        Class<?> progress = ecj.loadClass("org.eclipse.jdt.core.compiler.CompilationProgress");
        boolean ok;
        try {
            ok = (Boolean) main.getMethod("compile", String[].class, PrintWriter.class, PrintWriter.class, progress)
                    .invoke(null, args.toArray(new String[0]), writer, writer, null);
        } catch (InvocationTargetException error) {
            throw new IOException("ECJ: " + error.getCause(), error.getCause());
        }
        writer.flush();
        if (!ok) throw new IOException(Text.t("Ошибка компиляции") + ":\n" + tidy(said.toString(), sources));
    }

    private static void dex(File tools, File android, File out, List<String> classes) throws Exception {
        if (d8 == null) d8 = loader(new File(tools, "d8.jar"));
        List<String> args = new ArrayList<>();
        String[] options = {"--release", "--min-api", "26", "--lib", android.getAbsolutePath(),
                "--classpath", new File(tools, "sdk.jar").getAbsolutePath(), "--output", out.getAbsolutePath()};
        for (String option : options) args.add(option);
        args.addAll(classes);
        final StringBuilder errors = new StringBuilder();
        Class<?> handlerType = d8.loadClass("com.android.tools.r8.DiagnosticsHandler");
        Object handler = Proxy.newProxyInstance(d8, new Class<?>[]{handlerType}, (proxy, method, params) -> {
            switch (method.getName()) {
                case "error":
                    try { errors.append(params[0].getClass().getMethod("getDiagnosticMessage").invoke(params[0])).append('\n'); }
                    catch (Exception ignored) { errors.append(params[0]).append('\n'); }
                    return null;
                case "modifyDiagnosticsLevel": return params[0];
                case "hashCode": return System.identityHashCode(proxy);
                case "equals": return proxy == params[0];
                case "toString": return "ttcuz-d8";
                default: return null;
            }
        });
        Class<?> command = d8.loadClass("com.android.tools.r8.D8Command");
        Class<?> origin = d8.loadClass("com.android.tools.r8.origin.Origin");
        try {
            Object builder = command.getMethod("parse", String[].class, origin, handlerType)
                    .invoke(null, args.toArray(new String[0]), origin.getMethod("root").invoke(null), handler);
            Object built = builder.getClass().getMethod("build").invoke(builder);
            d8.loadClass("com.android.tools.r8.D8").getMethod("run", command).invoke(null, built);
        } catch (InvocationTargetException error) {
            String why = errors.length() > 0 ? errors.toString().trim() : String.valueOf(error.getCause());
            throw new IOException("D8: " + why, error.getCause());
        }
    }

    /** ECJ's report with the build folder taken out of the paths, so a line reads as the file the person wrote. */
    private static String tidy(String report, List<String> sources) {
        String text = report;
        if (!sources.isEmpty()) {
            String first = sources.get(0);
            int cut = first.indexOf("/src/");
            if (cut > 0) text = text.replace(first.substring(0, cut + 5), "java/");
        }
        text = text.trim();
        return text.length() > 4000 ? text.substring(0, 4000) + "…" : text;
    }

    // ------------------------------------------------------------------ files

    private interface Chunk { void took(Piece piece) throws Exception; }

    private static final class Piece { byte[] bytes; int count; long total; }

    private static void copy(InputStream in, OutputStream out, Chunk chunk) throws IOException {
        Piece piece = new Piece();
        piece.bytes = new byte[65536];
        try (InputStream source = in) {
            for (int n; (n = source.read(piece.bytes)) > 0; ) {
                out.write(piece.bytes, 0, n);
                piece.count = n;
                piece.total += n;
                if (chunk != null) {
                    try { chunk.took(piece); }
                    catch (Exception error) { throw new IOException(error); }
                }
            }
        }
    }

    private static void readFully(InputStream in, byte[] into) throws IOException {
        for (int at = 0; at < into.length; ) {
            int n = in.read(into, at, into.length - at);
            if (n < 0) throw new IOException("EOF");
            at += n;
        }
    }

    private static void write(File file, byte[] bytes) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) { out.write(bytes); }
    }

    private static void collect(File dir, List<String> into) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) collect(child, into);
            else if (child.getName().endsWith(".class")) into.add(child.getAbsolutePath());
        }
    }
}
