package cat.ttcuz.framework;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.json.JSONObject;

final class PluginPackage {
    static final long MAX_EXPANDED = 536870912;

    PluginPackage() {
    }

    static final class Metadata {
        final int api;
        final String author;
        final String description;
        final String entry;
        final String id;
        final String name;
        final String tiktok;
        final String version;
        /** What it says it needs: ttcuz permissions (network, interface...) and android.permission.* names. */
        final java.util.List<String> permissions;
        /** Why, in the author's words, per permission (manifest "uses", written from // @uses comments). */
        final java.util.Map<String, String> uses;

        Metadata(JSONObject jSONObject) throws Exception {
            String string = jSONObject.getString("id");
            this.id = string;
            if (!string.matches("[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}") || string.equals(".") || string.equals("..")) {
                throw new IOException(Text.t("Некорректный ID плагина"));
            }
            // name_ru, description_uk...: the phone's language first, then the plain field
            String language = java.util.Locale.getDefault().getLanguage();
            this.name = jSONObject.optString("name_" + language, jSONObject.optString("name", string));
            this.version = jSONObject.optString("version", "1.0");
            this.author = jSONObject.optString("author", "");
            this.description = jSONObject.optString("description_" + language, jSONObject.optString("description", ""));
            String string2 = jSONObject.getString("entry");
            this.entry = string2;
            if (string2.isEmpty() || string2.length() > 512) {
                throw new IOException(Text.t("Не указан класс плагина"));
            }
            this.api = jSONObject.optInt("api", 1);
            this.tiktok = jSONObject.optString("tiktok", "");
            java.util.List<String> asked = new java.util.ArrayList<>();
            org.json.JSONArray list = jSONObject.optJSONArray("permissions");
            for (int i = 0; list != null && i < list.length() && i < 32; i++) {
                String name = list.optString(i, "").trim();
                if (name.matches("[A-Za-z][\\w.]{1,95}") && !asked.contains(name)) asked.add(name);
            }
            this.permissions = java.util.Collections.unmodifiableList(asked);
            java.util.Map<String, String> why = new java.util.HashMap<>();
            JSONObject uses = jSONObject.optJSONObject("uses");
            if (uses != null) {
                java.util.Iterator<String> keys = uses.keys();
                while (keys.hasNext() && why.size() < 32) {
                    String key = keys.next(), text = uses.optString(key, "").trim();
                    if (key.matches("[A-Za-z][\\w.]{1,95}") && !text.isEmpty()) why.put(key, text.length() > 300 ? text.substring(0, 300) : text);
                }
            }
            this.uses = java.util.Collections.unmodifiableMap(why);
        }
    }

    private static volatile String running;

    /** The TikTok this mod runs in (its versionName), read once; "47.2.41" when it cannot be read. */
    static String tiktokVersion() {
        String known = running;
        if (known != null) return known;
        try {
            android.content.Context context = Ttcuz.context();
            // remembered only once it is really read: asked before the app is up, the answer is a guess
            if (context != null) return running = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Throwable ignored) {}
        return "47.2.41";
    }

    /** A plugin for any TikTok, for the one the mod runs in, or for 47.2.41 (whose plugins use the mod's API and still work). */
    static boolean forThisTikTok(String tiktok) {
        return tiktok == null || tiktok.isEmpty() || tiktok.equals("47.2.41") || tiktok.equals(tiktokVersion());
    }

    static void compatible(Metadata metadata) throws IOException {
        if (metadata.api < 1 || metadata.api > cat.ttcuz.framework.plugin.TtcuzPlugin.API) {
            throw new IOException(Text.t("Нужен ttcuz Plugin API ") + metadata.api);
        }
        if (forThisTikTok(metadata.tiktok)) {
            return;
        }
        throw new IOException(Text.t("Плагин для TikTok ") + metadata.tiktok);
    }

    static File child(File file, String str) throws IOException {
        if (str.isEmpty() || str.startsWith("/") || str.contains("\\") || str.contains(":")) {
            throw new IOException(Text.t("Некорректный путь архива"));
        }
        File canonicalFile = new File(file, str).getCanonicalFile();
        if (canonicalFile.getPath().startsWith(file.getCanonicalPath() + File.separator)) {
            return canonicalFile;
        }
        throw new IOException(Text.t("Путь за пределами плагина"));
    }

    static Metadata read(File file) throws Exception {
        File child = child(file, "manifest.json");
        if (!child.isFile() || child.length() > 65536) {
            throw new IOException(Text.t("Нет корректного manifest.json"));
        }
        return new Metadata(new JSONObject(new String(readBytes(child), StandardCharsets.UTF_8)));
    }

    private static byte[] readBytes(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                byte[] bArr = new byte[8192];
                while (true) {
                    int read = fileInputStream.read(bArr);
                    if (read == -1) {
                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        byteArrayOutputStream.close();
                        fileInputStream.close();
                        return byteArray;
                    }
                    byteArrayOutputStream.write(bArr, 0, read);
                }
            } finally {
            }
        } catch (Throwable th) {
            try {
                fileInputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    static Metadata extract(InputStream inputStream, File file) throws Exception {
        if (!file.mkdirs()) {
            throw new IOException(Text.t("Не удалось создать папку плагина"));
        }
        HashSet hashSet = new HashSet();
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        try {
            byte[] bArr = new byte[32768];
            long j = 0;
            int i = 0;
            while (true) {
                ZipEntry nextEntry = zipInputStream.getNextEntry();
                if (nextEntry == null) {
                    zipInputStream.close();
                    Metadata read = read(file);
                    dexFiles(file);
                    return read;
                }
                i++;
                if (i > 16384) {
                    throw new IOException(Text.t("Слишком много файлов"));
                }
                File child = child(file, nextEntry.getName());
                if (!hashSet.add(child.getCanonicalPath())) {
                    throw new IOException(Text.t("Повторяющийся файл"));
                }
                if (nextEntry.isDirectory()) {
                    if (zipInputStream.read() != -1) {
                        throw new IOException(Text.t("Данные вместо каталога"));
                    }
                    if (!child.isDirectory() && !child.mkdirs()) {
                        throw new IOException(Text.t("Не удалось создать каталог"));
                    }
                } else {
                    File parentFile = child.getParentFile();
                    if (!parentFile.isDirectory() && !parentFile.mkdirs()) {
                        throw new IOException(Text.t("Не удалось создать каталог"));
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(child);
                    try {
                        if (child.getName().endsWith(".dex") && !child.setReadOnly()) {
                            throw new IOException(Text.t("DEX не удалось сделать read-only"));
                        }
                        while (true) {
                            int read2 = zipInputStream.read(bArr);
                            if (read2 == -1) {
                                fileOutputStream.close();
                                break;
                            }
                            j += read2;
                            if (j > MAX_EXPANDED) {
                                throw new IOException(Text.t("Распакованный плагин больше 512 МиБ"));
                            }
                            fileOutputStream.write(bArr, 0, read2);
                        }
                    } finally {
                    }
                }
            }
        } catch (Throwable th) {
            try {
                zipInputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    static List<File> dexFiles(File file) throws IOException {
        ArrayList arrayList = new ArrayList();
        File child = child(file, "classes.dex");
        if (!child.isFile()) {
            throw new IOException(Text.t("Нет classes.dex"));
        }
        arrayList.add(child);
        int i = 2;
        while (true) {
            File child2 = child(file, "classes" + i + ".dex");
            if (!child2.isFile()) {
                break;
            }
            arrayList.add(child2);
            i++;
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            File file2 = (File) it.next();
            RandomAccessFile randomAccessFile = new RandomAccessFile(file2, "r");
            try {
                byte[] bArr = new byte[8];
                randomAccessFile.readFully(bArr);
                if (bArr[0] != 100 || bArr[1] != 101 || bArr[2] != 120 || bArr[3] != 10 || bArr[7] != 0 || randomAccessFile.length() < 112) {
                    throw new IOException(Text.t("Повреждённый DEX: ") + file2.getName());
                }
                randomAccessFile.close();
                if (!file2.setReadOnly()) {
                    throw new IOException(Text.t("DEX не удалось сделать read-only"));
                }
            } catch (Throwable th) {
                try {
                    randomAccessFile.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
        return arrayList;
    }

    static void delete(File file) throws IOException {
        File parentFile = file.getAbsoluteFile().getParentFile();
        if (parentFile != null) {
            File file2 = new File(parentFile.getCanonicalFile(), file.getName());
            if (!file2.equals(file2.getCanonicalFile())) {
                if (!file.delete()) {
                    throw new IOException(Text.t("Не удалось удалить ссылку"));
                }
                return;
            }
        }
        File[] listFiles = file.listFiles();
        if (listFiles != null) {
            for (File file3 : listFiles) {
                delete(file3);
            }
        }
        if (!file.exists() || file.delete()) {
            return;
        }
        file.setWritable(true, true);
        if (file.delete()) {
            return;
        }
        throw new IOException(Text.t("Не удалось удалить ") + file.getName());
    }
}
