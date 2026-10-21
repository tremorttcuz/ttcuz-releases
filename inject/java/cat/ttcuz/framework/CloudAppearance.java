package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.util.concurrent.*;
import org.json.JSONArray;
import org.json.JSONObject;

/** Private account backup. Disk and network work never runs on the UI thread. */
final class CloudAppearance {

    private static final ScheduledExecutorService worker =
        Executors.newSingleThreadScheduledExecutor();
    private static String selected, ready;
    private static ScheduledFuture<?> pending;
    private static SharedPreferences global, own;
    private static volatile boolean restoring;
    private static long revision;
    private static final SharedPreferences.OnSharedPreferenceChangeListener listener = (
        prefs,
        key
    ) -> {
        if (!restoring && cosmetic(key)) changed();
    };

    private static boolean cosmetic(String key) {
        return (
            key != null &&
            key.matches(
                "(accent(?:_custom|_gradient_\\w+)?|like_color|comment_like_color|font|font_emoji|avatar_shape|loading_style|fps_lock|profile_layout|repost_pos|seekbar|dates|dim(?:_\\w+)?|island_\\w+|nav_\\w+|navigation_\\w+|theme_\\w+|appearance_color_\\w+|profile_(?:gradient|nickname|badge|crown)_\\w+|profile_display_name|rail_\\w+)"
            )
        );
    }

    static synchronized boolean ready(String uid) {
        ensure(uid);
        return uid.equals(ready);
    }

    /** A deliberate local edit must not wait for the private backup endpoint. */
    static synchronized boolean publicationReady(String uid) {
        ensure(uid);
        return (
            uid != null &&
            uid.equals(selected) &&
            (uid.equals(ready) || own.getBoolean("_cloud_dirty", false))
        );
    }

    static synchronized void ensure(String uid) {
        Context context = Ttcuz.context();
        if (context == null || uid == null) return;
        if (uid.equals(selected)) return;
        if (global != null) global.unregisterOnSharedPreferenceChangeListener(listener);
        if (own != null) own.unregisterOnSharedPreferenceChangeListener(listener);
        selected = uid;
        ready = null;
        ++revision;
        global = context.getSharedPreferences(Ttcuz.PREFS, 0);
        own = AccountAppearance.prefs(context);
        global.registerOnSharedPreferenceChangeListener(listener);
        own.registerOnSharedPreferenceChangeListener(listener);
        worker.execute(() -> hydrate(uid));
    }

    static synchronized void changed() {
        String uid = Account.liveId();
        ensure(uid);
        if (uid == null) return;
        ++revision;
        own.edit().putBoolean("_cloud_dirty", true).apply();
        if (pending != null) pending.cancel(false);
        pending = worker.schedule(() -> save(uid), 2, TimeUnit.SECONDS);
    }

    private static synchronized boolean current(String uid) {
        return uid.equals(selected) && uid.equals(Account.liveId());
    }

    private static void hydrate(String uid) {
        try {
            if (!current(uid)) {
                synchronized (CloudAppearance.class) {
                    if (uid.equals(selected)) worker.schedule(
                        () -> hydrate(uid),
                        5,
                        TimeUnit.SECONDS
                    );
                }
                return;
            }
            CloudProfileProvider cloud = new CloudProfileProvider(Ttcuz.context());
            JSONObject result = cloud.appearance(uid, new JSONObject().put("action", "read"));
            JSONObject snapshot = result.optJSONObject("snapshot");
            if (snapshot != null && !own.getBoolean("_cloud_dirty", false)) {
                // An upgrade keeps existing local choices; a clean reinstall restores the backup.
                boolean local = false;
                for (String key : own.getAll().keySet())
                    if (cosmetic(key)) {
                        local = true;
                        break;
                    }
                if (own.getBoolean("_cloud_bound", false) || !local) restore(uid, snapshot, cloud);
            }
            synchronized (CloudAppearance.class) {
                if (!current(uid)) return;
                ready = uid;
                own.edit().putBoolean("_cloud_bound", true).apply();
            }
            save(uid);
            TtcuzProfileSync.publishOwn();
        } catch (Throwable error) {
            Diary.note("appearance restore: " + error.getClass().getSimpleName());
            worker.schedule(() -> hydrate(uid), 30, TimeUnit.SECONDS);
        }
    }

    private static JSONObject values(SharedPreferences prefs) throws Exception {
        JSONObject values = new JSONObject();
        for (java.util.Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (!cosmetic(entry.getKey())) continue;
            Object value = entry.getValue();
            String type =
                value instanceof String
                    ? "string"
                    : value instanceof Boolean
                      ? "boolean"
                      : value instanceof Integer
                        ? "int"
                        : value instanceof Long
                          ? "long"
                          : value instanceof Float
                            ? "float"
                            : null;
            if (type != null) values.put(
                entry.getKey(),
                new JSONObject().put("type", type).put("value", value)
            );
        }
        return values;
    }

    private static String hash(File file) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        try (java.io.InputStream in = new java.io.FileInputStream(file)) {
            byte[] b = new byte[16384];
            int n;
            while ((n = in.read(b)) != -1) digest.update(b, 0, n);
        }
        StringBuilder text = new StringBuilder();
        for (byte b : digest.digest())
            text.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return text.toString();
    }

    private static void files(File root, File dir, JSONArray list) throws Exception {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File file : children) {
            if (
                !file.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator)
            ) continue;
            if (file.isDirectory()) files(root, file, list);
            else if (
                file.isFile() &&
                !file.getName().endsWith(".cloud") &&
                !file.getName().endsWith(".tmp") &&
                file.length() > 0 &&
                file.length() <= 64L * 1024 * 1024
            ) {
                if (list.length() >= 32) throw new java.io.IOException("Too many appearance files");
                String name = root.toPath().relativize(file.toPath()).toString().replace('\\', '/');
                list.put(
                    new JSONObject()
                        .put("name", name)
                        .put("hash", hash(file))
                        .put("size", file.length())
                );
            }
        }
    }

    private static void save(String uid) {
        try {
            long change;
            synchronized (CloudAppearance.class) {
                if (!current(uid) || !uid.equals(ready)) return;
                change = revision;
            }
            SharedPreferences localGlobal, localOwn;
            synchronized (CloudAppearance.class) {
                if (!current(uid)) return;
                localGlobal = global;
                localOwn = own;
            }
            CloudProfileProvider cloud = new CloudProfileProvider(Ttcuz.context());
            File root = new File(Ttcuz.context().getFilesDir(), "ttcuz/accounts/" + uid);
            JSONArray files = new JSONArray();
            files(root, root, files);
            JSONObject snapshot = new JSONObject()
                .put("schema", 1)
                .put("global", values(localGlobal))
                .put("account", values(localOwn))
                .put("files", files);
            JSONArray complete = cloud
                .appearance(
                    uid,
                    new JSONObject().put("action", "prepare").put("snapshot", snapshot)
                )
                .getJSONArray("complete");
            java.util.Set<String> known = new java.util.HashSet<>();
            for (int i = 0; i < complete.length(); i++) known.add(complete.getString(i));
            for (int i = 0; i < files.length(); i++) {
                JSONObject f = files.getJSONObject(i);
                if (known.contains(f.getString("hash"))) continue;
                try (
                    java.io.InputStream in = new java.io.FileInputStream(
                        new File(root, f.getString("name"))
                    )
                ) {
                    byte[] block = new byte[49152];
                    int part = 0;
                    for (;;) {
                        int size = 0,
                            n;
                        while (
                                size < block.length &&
                                (n = in.read(block, size, block.length - size)) != -1
                            )
                            size += n;
                        if (size == 0) break;
                        if (!current(uid)) return;
                        cloud.appearance(
                            uid,
                            new JSONObject()
                                .put("action", "chunk")
                                .put("hash", f.getString("hash"))
                                .put("part", part++)
                                .put(
                                    "data",
                                    android.util.Base64.encodeToString(
                                        block,
                                        0,
                                        size,
                                        android.util.Base64.NO_WRAP
                                    )
                                )
                        );
                    }
                }
            }
            if (!current(uid)) return;
            for (int i = 0; i < files.length(); i++) {
                JSONObject f = files.getJSONObject(i);
                if (
                    !f.getString("hash").equals(hash(new File(root, f.getString("name"))))
                ) throw new java.io.IOException("Appearance file changed during backup");
            }
            cloud.appearance(uid, new JSONObject().put("action", "save").put("snapshot", snapshot));
            synchronized (CloudAppearance.class) {
                if (current(uid) && change == revision) own.edit()
                    .putBoolean("_cloud_dirty", false)
                    .apply();
            }
        } catch (Throwable error) {
            Diary.note("appearance backup: " + error.getClass().getSimpleName());
            worker.schedule(() -> save(uid), 30, TimeUnit.SECONDS);
        }
    }

    private static void restore(String uid, JSONObject snapshot, CloudProfileProvider cloud)
        throws Exception {
        File root = new File(Ttcuz.context().getFilesDir(), "ttcuz/accounts/" + uid);
        JSONArray files = snapshot.getJSONArray("files");
        java.util.List<File[]> staged = new java.util.ArrayList<>();
        try {
            for (int i = 0; i < files.length(); i++) {
                JSONObject f = files.getJSONObject(i);
                String name = f.getString("name"),
                    sha = f.getString("hash");
                long length = f.getLong("size");
                File target = new File(root, name);
                if (
                    !target
                        .getCanonicalPath()
                        .startsWith(root.getCanonicalPath() + File.separator) ||
                    length <= 0 ||
                    length > 64L * 1024 * 1024
                ) throw new java.io.IOException("Invalid backup file");
                if (
                    target.isFile() && target.length() == length && sha.equals(hash(target))
                ) continue;
                target.getParentFile().mkdirs();
                File temp = new File(target.getPath() + ".cloud");
                staged.add(new File[] { temp, target });
                try (java.io.OutputStream out = new java.io.FileOutputStream(temp)) {
                    for (int part = 0; part < (length + 49151) / 49152; part++) {
                        if (!current(uid)) throw new java.io.IOException("Account changed");
                        String encoded = cloud
                            .appearance(
                                uid,
                                new JSONObject()
                                    .put("action", "file")
                                    .put("hash", sha)
                                    .put("part", part)
                            )
                            .getString("data");
                        byte[] block = android.util.Base64.decode(
                            encoded,
                            android.util.Base64.DEFAULT
                        );
                        if (
                            block.length != Math.min(49152, length - (long) part * 49152)
                        ) throw new java.io.IOException("Invalid chunk");
                        out.write(block);
                    }
                }
                if (
                    temp.length() != length || !sha.equals(hash(temp))
                ) throw new java.io.IOException("Invalid backup hash");
            }
            synchronized (CloudAppearance.class) {
                if (!current(uid)) return;
                for (File[] pair : staged)
                    java.nio.file.Files.move(
                        pair[0].toPath(),
                        pair[1].toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );
            }
            java.util.concurrent.CountDownLatch applied = new java.util.concurrent.CountDownLatch(
                1
            );
            final Throwable[] failure = { null };
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                try {
                    if (!current(uid)) throw new java.io.IOException("Account changed");
                    restoring = true;
                    apply(global, snapshot.getJSONObject("global"));
                    apply(own, snapshot.getJSONObject("account"));
                } catch (Throwable error) {
                    failure[0] = error;
                } finally {
                    restoring = false;
                    applied.countDown();
                }
            });
            applied.await();
            if (failure[0] != null) throw new java.io.IOException(
                "Cannot restore appearance",
                failure[0]
            );
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (!current(uid)) return;
                Accent.reload();
                AppearanceColors.reload();
                Fonts.reload();
                Themes.forget();
                LikeColors.reload();
                ProfileStyle.reload();
                FeedRail.reloadSettings();
                Themes.repaintVisible();
            });
        } finally {
            for (File[] pair : staged) pair[0].delete();
        }
    }

    private static void apply(SharedPreferences prefs, JSONObject values) throws Exception {
        SharedPreferences.Editor edit = prefs.edit();
        for (String key : prefs.getAll().keySet()) if (cosmetic(key)) edit.remove(key);
        java.util.Iterator<String> keys = values.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!cosmetic(key)) continue;
            JSONObject e = values.getJSONObject(key);
            switch (e.getString("type")) {
                case "string":
                    edit.putString(key, e.getString("value"));
                    break;
                case "boolean":
                    edit.putBoolean(key, e.getBoolean("value"));
                    break;
                case "int":
                    edit.putInt(key, e.getInt("value"));
                    break;
                case "long":
                    edit.putLong(key, e.getLong("value"));
                    break;
                case "float":
                    edit.putFloat(key, (float) e.getDouble("value"));
                    break;
            }
        }
        if (!edit.commit()) throw new java.io.IOException("Cannot restore preferences");
    }
}
