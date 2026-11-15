package cat.ttcuz.framework;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Manifest-backed emoji font packs with system fallback and cached metadata. */
public final class EmojiPackManager {
    private EmojiPackManager() {}

    public static final class Pack {
        public final String id;
        public final String name;
        public final String version;
        public final String asset;
        public final int minAndroid;
        /** A pack that is not in the app: downloaded once from url, checked against size and sha256. */
        public final String url, sha256;
        public final long size;

        Pack(String id, String name, String version, String asset, int minAndroid) {
            this(id, name, version, asset, minAndroid, null, null, 0);
        }

        Pack(String id, String name, String version, String asset, int minAndroid, String url, String sha256, long size) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.asset = asset;
            this.minAndroid = minAndroid;
            this.url = url;
            this.sha256 = sha256;
            this.size = size;
        }

        boolean remote() { return url != null; }
    }

    private static volatile List<Pack> cached;

    public static List<Pack> packs(Context context) {
        if (context == null) context = Ttcuz.context();
        List<Pack> known = cached;
        if (known != null) return known;
        ArrayList<Pack> found = new ArrayList<Pack>();
        found.add(new Pack(Fonts.SYSTEM, "System", "device", null, 1));
        if (context != null) {
            try {
                InputStream input = context.getAssets().open("ttcuz/emoji/packs.json");
                byte[] raw;
                try {
                    java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                    byte[] block = new byte[4096];
                    int read;
                    while ((read = input.read(block)) != -1) {
                        if (bytes.size() + read > 65536) throw new IllegalArgumentException("emoji manifest too large");
                        bytes.write(block, 0, read);
                    }
                    raw = bytes.toByteArray();
                } finally { input.close(); }
                JSONArray entries = new JSONObject(new String(raw, "UTF-8"))
                        .optJSONArray("packs");
                if (entries != null) for (int i = 0; i < entries.length(); i++) {
                    JSONObject item = entries.optJSONObject(i);
                    if (item == null) continue;
                    String id = item.optString("id", "");
                    String name = item.optString("name", "");
                    String version = item.optString("version", "");
                    String asset = item.optString("asset", "");
                    int min = item.optInt("minAndroid", 29);
                    String url = item.optString("url", ""), sum = item.optString("sha256", "").toLowerCase(java.util.Locale.ROOT);
                    long size = item.optLong("size", 0);
                    boolean remote = url.startsWith("https://") && url.length() <= 300 && sum.matches("[0-9a-f]{64}") && size > 0 && size < 128L * 1048576;
                    if (!id.matches("[a-z0-9_-]{1,32}") || name.length() < 1
                            || name.length() > 48 || version.length() > 24
                            || !(remote || validAsset(asset)) || min < 1 || min > 100) continue;
                    if (contains(found, id)) continue;
                    found.add(remote ? new Pack(id, name, version, null, min, url, sum, size) : new Pack(id, name, version, asset, min));
                }
            } catch (Throwable error) {
                Diary.note("emoji manifest: " + error.getClass().getSimpleName());
            }
        }
        found.add(new Pack(Fonts.EMOJI_FILE, "Custom font", "user", null, 29));
        known = Collections.unmodifiableList(found);
        if (context != null) cached = known;
        return known;
    }

    public static Pack find(Context context, String id) {
        for (Pack pack : packs(context)) if (pack.id.equals(id)) return pack;
        return null;
    }

    public static String asset(Context context, String id) {
        Pack pack = find(context, id);
        return pack == null || pack.asset == null ? null : "ttcuz/" + pack.asset;
    }

    public static boolean ready(Context context, String id) {
        Pack pack = find(context, id);
        if (pack == null) return false;
        if (Fonts.EMOJI_FILE.equals(id))
            return context != null && Fonts.emojiFile(context, id).isFile();
        if (pack.remote()) {
            if (context == null || android.os.Build.VERSION.SDK_INT < pack.minAndroid) return false;
            java.io.File file = Fonts.emojiFile(context, id);
            return file.isFile() && file.length() == pack.size;
        }
        if (pack.asset == null) return true;
        if (android.os.Build.VERSION.SDK_INT < pack.minAndroid || context == null) return false;
        InputStream input = null;
        try {
            input = context.getAssets().open("ttcuz/" + pack.asset);
            return true;
        } catch (Throwable ignored) { return false; }
        finally { if (input != null) try { input.close(); } catch (Throwable ignored) {} }
    }

    /** The download running now (pack id), and how far it is, 0..100. */
    static volatile String fetching;
    static volatile int fetched;

    interface Done { void done(boolean ok); }

    /**
     * Downloads a pack into the app's files: to a temporary file first, then its size and sha256 are checked and only
     * then is it put in place, so a half or wrong file is never used. Progress goes to `along` (any thread); `done` runs
     * on the main thread.
     */
    static void download(final Context context, final Pack pack, final Net.Along along, final Done done) {
        if (context == null || pack == null || !pack.remote() || fetching != null) return;
        fetching = pack.id;
        fetched = 0;
        final java.io.File target = Fonts.emojiFile(context, pack.id), temporary = new java.io.File(target.getPath() + ".part");
        final DownloadTasks.Control control = DownloadTasks.begin("emoji-" + pack.id, pack.name, Text.t("Эмодзи"), () -> download(context, pack, along, done));
        control.limit = pack.size + 1048576;
        Net.away("emoji pack", () -> {
            boolean ok = false;
            try {
                java.io.File parent = target.getParentFile();
                if (parent != null) parent.mkdirs();
                final int[] told = {-1};
                boolean got = Net.download(pack.url, temporary, (percent, have, total) -> {
                    int shown = (int) Math.min(100, have * 100 / Math.max(1, pack.size));
                    fetched = shown;
                    // once per percent, not per block read: the screen gets a steady 100 updates, not thousands
                    if (shown == told[0]) return;
                    told[0] = shown;
                    DownloadTasks.progress("emoji-" + pack.id, shown);
                    if (along != null) along.at(shown, have, pack.size);
                }, control);
                if (got && temporary.length() == pack.size && pack.sha256.equals(digest(temporary))) {
                    if (target.exists()) target.delete();
                    ok = temporary.renameTo(target);
                } else if (got) Diary.note("emoji pack " + pack.id + ": size or sha256 wrong");
            } catch (Throwable error) { Diary.note("emoji pack " + pack.id + ": " + error); }
            finally {
                temporary.delete();
                fetching = null;
                DownloadTasks.finish("emoji-" + pack.id, ok);
                final boolean result = ok;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> { if (done != null) done.done(result); });
            }
        });
    }

    private static String digest(java.io.File file) throws Exception {
        java.security.MessageDigest sha = java.security.MessageDigest.getInstance("SHA-256");
        try (InputStream in = new java.io.FileInputStream(file)) {
            byte[] block = new byte[65536];
            for (int n; (n = in.read(block)) > 0; ) sha.update(block, 0, n);
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : sha.digest()) hex.append(String.format(java.util.Locale.ROOT, "%02x", b));
        return hex.toString();
    }

    public static String displayName(Context context, String id) {
        Pack pack = find(context, id);
        return pack == null ? "System" : pack.name;
    }

    public static void clearCache() { cached = null; }

    private static boolean contains(List<Pack> packs, String id) {
        for (Pack pack : packs) if (pack.id.equals(id)) return true;
        return false;
    }

    private static boolean validAsset(String path) {
        return path != null && path.length() <= 120
                && path.matches("[A-Za-z0-9_.-]+(?:/[A-Za-z0-9_.-]+)*\\.(?:ttf|otf)")
                && !path.contains("..") && !path.startsWith("/");
    }
}
