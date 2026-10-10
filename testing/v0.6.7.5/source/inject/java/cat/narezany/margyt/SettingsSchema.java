package cat.narezany.margyt;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;
import java.util.Set;

/** One-time migration of the old mod's local settings. */
final class SettingsSchema {
    private SettingsSchema() {}

    static final int VERSION = 1;
    private static final String KEY_VERSION = "schemaVersion";
    private static volatile boolean checked;

    static synchronized void ensure(Context context) {
        if (checked || context == null) return;
        try {
            SharedPreferences target = context.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE);
            if (target.getInt(KEY_VERSION, 0) >= VERSION) {
                checked = true;
                return;
            }
            SharedPreferences old = context.getSharedPreferences("margyt", Context.MODE_PRIVATE);
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
                    try { edit.putStringSet(key, (Set<String>) value); }
                    catch (ClassCastException ignored) { /* malformed legacy value */ }
                }
            }
            edit.putInt(KEY_VERSION, VERSION);
            checked = edit.commit();
        } catch (Throwable error) {
            Diary.note("settings migration: " + error);
        }
    }
}
