package cat.ttcuz.framework;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.json.JSONObject;

/** Archive/file correctness only; no code, author, permission or capability filtering. */
final class PluginPackage {

    static final long MAX_EXPANDED = 512L * 1024 * 1024;

    static final class Metadata {

        final String id, name, version, author, description, entry, tiktok;
        final int api;

        Metadata(JSONObject json) throws Exception {
            id = json.getString("id");
            if (
                !id.matches("[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}") || id.equals(".") || id.equals("..")
            ) throw new IOException("Некорректный ID плагина");
            name = json.optString("name", id);
            version = json.optString("version", "1.0");
            author = json.optString("author", "");
            description = json.optString("description", "");
            entry = json.getString("entry");
            if (entry.isEmpty() || entry.length() > 512) throw new IOException(
                "Не указан класс плагина"
            );
            api = json.optInt("api", 1);
            tiktok = json.optString("tiktok", "");
        }
    }

    static void compatible(Metadata metadata) throws IOException {
        if (metadata.api != cat.ttcuz.framework.plugin.TtcuzPlugin.API) throw new IOException(
            "Нужен ttcuz Plugin API " + metadata.api
        );
        if (
            !metadata.tiktok.isEmpty() && !metadata.tiktok.equals(Version.TIKTOK)
        ) throw new IOException("Плагин для TikTok " + metadata.tiktok);
    }

    static File child(File root, String name) throws IOException {
        if (
            name.isEmpty() || name.startsWith("/") || name.contains("\\") || name.contains(":")
        ) throw new IOException("Некорректный путь архива");
        File file = new File(root, name).getCanonicalFile();
        String prefix = root.getCanonicalPath() + File.separator;
        if (!file.getPath().startsWith(prefix)) throw new IOException("Путь за пределами плагина");
        return file;
    }

    static Metadata read(File folder) throws Exception {
        File file = child(folder, "manifest.json");
        if (!file.isFile() || file.length() > 65536) throw new IOException(
            "Нет корректного manifest.json"
        );
        return new Metadata(
            new JSONObject(new String(readBytes(file), java.nio.charset.StandardCharsets.UTF_8))
        );
    }

    private static byte[] readBytes(File file) throws IOException {
        try (
            InputStream in = new FileInputStream(file);
            ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            byte[] block = new byte[8192];
            int n;
            while ((n = in.read(block)) != -1) out.write(block, 0, n);
            return out.toByteArray();
        }
    }

    static Metadata extract(InputStream input, File destination) throws Exception {
        if (!destination.mkdirs()) throw new IOException("Не удалось создать папку плагина");
        long total = 0;
        int count = 0;
        Set<String> names = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            byte[] block = new byte[32768];
            while ((entry = zip.getNextEntry()) != null) {
                if (++count > 16384) throw new IOException("Слишком много файлов");
                File target = child(destination, entry.getName());
                if (!names.add(target.getCanonicalPath())) throw new IOException(
                    "Повторяющийся файл"
                );
                if (entry.isDirectory()) {
                    if (zip.read() != -1) throw new IOException("Данные вместо каталога");
                    if (!target.isDirectory() && !target.mkdirs()) throw new IOException(
                        "Не удалось создать каталог"
                    );
                    continue;
                }
                File parent = target.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException(
                    "Не удалось создать каталог"
                );
                try (OutputStream out = new FileOutputStream(target)) {
                    // Android 14: mark code read-only immediately after opening, before writing.
                    if (
                        target.getName().endsWith(".dex") && !target.setReadOnly()
                    ) throw new IOException("DEX не удалось сделать read-only");
                    int n;
                    while ((n = zip.read(block)) != -1) {
                        total += n;
                        if (total > MAX_EXPANDED) throw new IOException(
                            "Распакованный плагин больше 512 МиБ"
                        );
                        out.write(block, 0, n);
                    }
                }
            }
        }
        Metadata metadata = read(destination);
        dexFiles(destination);
        return metadata;
    }

    static List<File> dexFiles(File folder) throws IOException {
        ArrayList<File> files = new ArrayList<>();
        File first = child(folder, "classes.dex");
        if (!first.isFile()) throw new IOException("Нет classes.dex");
        files.add(first);
        for (int n = 2; ; n++) {
            File next = child(folder, "classes" + n + ".dex");
            if (!next.isFile()) break;
            files.add(next);
        }
        for (File file : files) {
            try (RandomAccessFile in = new RandomAccessFile(file, "r")) {
                byte[] header = new byte[8];
                in.readFully(header);
                if (
                    header[0] != 'd' ||
                    header[1] != 'e' ||
                    header[2] != 'x' ||
                    header[3] != '\n' ||
                    header[7] != 0 ||
                    in.length() < 112
                ) throw new IOException("Повреждённый DEX: " + file.getName());
            }
            if (!file.setReadOnly()) throw new IOException("DEX не удалось сделать read-only");
        }
        return files;
    }

    /** Only called with a root already produced by child() or by our staging directory. */
    static void delete(File file) throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null) {
            File normalized = new File(parent.getCanonicalFile(), file.getName());
            if (!normalized.equals(normalized.getCanonicalFile())) {
                if (!file.delete()) throw new IOException("Не удалось удалить ссылку");
                return;
            }
        }
        File[] children = file.listFiles();
        if (children != null) for (File child : children) delete(child);
        if (file.exists() && !file.delete()) {
            file.setWritable(true, true);
            if (!file.delete()) throw new IOException("Не удалось удалить " + file.getName());
        }
    }
}
