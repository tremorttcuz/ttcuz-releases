package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;
import java.util.Set;

/** One-time migration of the old mod's local settings. */
final class SettingsSchema {

    private SettingsSchema() {}

    static final int VERSION = 2;
    private static final String LEGACY = new String(new char[] { 109, 97, 114, 103, 121, 116 });
    private static final String KEY_VERSION = "schemaVersion";
    private static volatile boolean checked;

    static synchronized void ensure(Context context) {
        if (checked || context == null) return;
        try {
            SharedPreferences target = context.getSharedPreferences(
                Ttcuz.PREFS,
                Context.MODE_PRIVATE
            );
            if (target.getInt(KEY_VERSION, 0) >= VERSION) {
                checked = true;
                return;
            }
            SharedPreferences old = context.getSharedPreferences(LEGACY, Context.MODE_PRIVATE);
            migrateFiles(
                new java.io.File(context.getFilesDir(), LEGACY),
                new java.io.File(context.getFilesDir(), "ttcuz")
            );
            SharedPreferences.Editor edit = target.edit();
            for (Map.Entry<String, ?> entry : old.getAll().entrySet()) {
                String key = entry.getKey();
                if (target.contains(key) || KEY_VERSION.equals(key)) continue;
                Object value = entry.getValue();
                if (value instanceof String) edit.putString(key, (String) value);
                else if (value instanceof Boolean) edit.putBoolean(key, (Boolean) value);
                else if (value instanceof Integer) edit.putInt(key, (Integer) value);
                else if (value instanceof Long) edit.putLong(key, (Long) value);
                else if (value instanceof Float) edit.putFloat(key, (Float) value);
                else if (value instanceof Set) {
                    try {
                        edit.putStringSet(key, (Set<String>) value);
                    } catch (ClassCastException ignored) {
                        /* malformed legacy value */
                    }
                }
            }
            edit.remove("textures_on")
                .remove("textures_pack")
                .remove("title_note_text")
                .remove("title_note_hidden");
            edit.putBoolean("icon_pending", true);
            edit.putInt(KEY_VERSION, VERSION);
            checked = edit.commit();
        } catch (Throwable error) {
            Diary.note("settings migration: " + error);
        }
    }

    /** Preserve old fonts, archives and caches without replacing current files. */
    private static void migrateFiles(java.io.File from, java.io.File to)
        throws java.io.IOException {
        if (!from.exists()) return;
        if (from.isDirectory()) {
            if (!to.isDirectory() && !to.mkdirs()) throw new java.io.IOException(
                "Cannot create data directory"
            );
            java.io.File[] files = from.listFiles();
            if (files == null) throw new java.io.IOException("Cannot read previous data directory");
            for (java.io.File file : files) {
                if (file.getName().equals("textures") || file.getName().equals("plugins")) continue;
                migrateFiles(file, new java.io.File(to, file.getName()));
            }
        } else if (!to.exists()) {
            if (from.renameTo(to)) return;
            java.io.File temporary = new java.io.File(to.getPath() + ".migration");
            try (
                java.io.InputStream input = new java.io.FileInputStream(from);
                java.io.OutputStream output = new java.io.FileOutputStream(temporary)
            ) {
                byte[] bytes = new byte[16384];
                int count;
                while ((count = input.read(bytes)) != -1) output.write(bytes, 0, count);
            } catch (java.io.IOException error) {
                temporary.delete();
                throw error;
            }
            if (!temporary.renameTo(to)) {
                temporary.delete();
                throw new java.io.IOException("Cannot migrate data file");
            }
        }
    }
}
