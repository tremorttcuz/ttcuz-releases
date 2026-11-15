package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * ayumoments: a message the sender deletes ("unsend") stays in the chat, marked with a bin beside its time.
 *
 * TikTok 47.2.41 applies every unsend through one method of its IM SDK (X.0h5k.LIZJ(serverId, uid, body, type)); the
 * build sends both of its calls here. With the switch on the id is remembered and TikTok is told no id came with the
 * notice, the case it already handles by leaving the chat as it is. MessageTimes shows the bin for remembered ids.
 */
public final class DeletedMessages {
    private DeletedMessages() {}

    static final String KEY = "ayu_keep_deleted", IDS = "ayu_deleted_ids";
    private static final int MOST = 3000;
    private static volatile Set<String> ids;
    private static volatile Method original;

    private static SharedPreferences prefs() {
        Context context = Ttcuz.context();
        return context == null ? null : context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
    }

    static boolean enabled() {
        SharedPreferences store = prefs();
        return store != null && store.getBoolean(KEY, false);
    }

    static void set(boolean on) {
        SharedPreferences store = prefs();
        if (store != null) store.edit().putBoolean(KEY, on).apply();
    }

    private static Set<String> ids() {
        Set<String> known = ids;
        if (known != null) return known;
        SharedPreferences store = prefs();
        Set<String> loaded = new HashSet<>();
        if (store != null) loaded.addAll(store.getStringSet(IDS, new HashSet<>()));
        return ids = loaded;
    }

    static boolean deleted(long serverId) {
        if (serverId == 0) return false;
        Set<String> known = ids();
        synchronized (known) { return known.contains(Long.toString(serverId)); }
    }

    private static void remember(String serverId) {
        Set<String> known = ids();
        Set<String> copy;
        synchronized (known) {
            known.add(serverId);
            if (known.size() > MOST) {
                // the oldest are not known by age here; dropping any one of the smallest ids keeps the newest
                String smallest = null;
                for (String id : known) if (smallest == null || id.length() < smallest.length() || (id.length() == smallest.length() && id.compareTo(smallest) < 0)) smallest = id;
                known.remove(smallest);
            }
            copy = new HashSet<>(known);
        }
        SharedPreferences store = prefs();
        if (store != null) store.edit().putStringSet(IDS, copy).apply();
    }

    /** X.0h5k.LIZJ: the unsend of one message. Returns TikTok's own result object. */
    public static Object recall(Object updater, String serverId, String uid, Object body, int type) throws Throwable {
        String id = serverId;
        if (serverId != null && !serverId.isEmpty()) PluginKit.event("dm:deleted", "id", serverId);
        if (enabled() && id != null && !id.isEmpty()) {
            remember(id);
            id = "";
            MessageTimes.refresh();
        }
        Method method = original;
        if (method == null) {
            for (Method candidate : updater.getClass().getDeclaredMethods()) {
                Class<?>[] types = candidate.getParameterTypes();
                if (candidate.getName().equals("LIZJ") && types.length == 4 && types[0] == String.class
                        && types[1] == String.class && types[3] == int.class) { method = candidate; break; }
            }
            if (method == null) throw new IllegalStateException("recall method not found");
            method.setAccessible(true);
            original = method;
        }
        try {
            return method.invoke(updater, id, uid, body, type);
        } catch (java.lang.reflect.InvocationTargetException error) {
            throw error.getCause();
        }
    }

    /**
     * The bin beside the time of a deleted message, in the time's own colour and size: on the outer side of the time,
     * after it for a message you received, before it for one you sent (the ticks of a sent one stay after it).
     * The message itself (its whole row) is shown faded, so it reads as gone at a glance.
     */
    static void mark(TextView time, View row, boolean deleted, boolean incoming) {
        Drawable[] now = time.getCompoundDrawables();
        boolean binStart = now[0] instanceof Bin, binEnd = now[2] instanceof Bin;
        if (!deleted) {
            if (binStart || binEnd) time.setCompoundDrawables(binStart ? null : now[0], null, binEnd ? null : now[2], null);
            if (row != null && Boolean.TRUE.equals(row.getTag(FADED))) { row.setLayerType(View.LAYER_TYPE_NONE, null); row.setTag(FADED, null); }
            return;
        }
        // faded through a layer of its own rather than View.setAlpha: the list animates its rows' alpha (they appear,
        // they move) and set it back to 1 under us; nothing in TikTok touches a row's layer paint
        if (row != null && !Boolean.TRUE.equals(row.getTag(FADED))) {
            android.graphics.Paint half = new android.graphics.Paint();
            half.setAlpha(128);
            row.setLayerType(View.LAYER_TYPE_HARDWARE, half);
            row.setTag(FADED, Boolean.TRUE);
        }
        // always right of the time; on one's own message it takes the place of the ticks
        if (binEnd) return;
        Drawable icon = SettingsGlyph.make("trash_mod", time.getCurrentTextColor());
        if (icon == null) return;
        Bin bin = new Bin(icon);
        int size = Math.round(time.getTextSize() * 1.05f);
        bin.setBounds(0, 0, size, size);
        time.setCompoundDrawables(binStart ? null : now[0], null, bin, null);
        time.setCompoundDrawablePadding(Math.round(time.getTextSize() * .25f));
    }

    private static final int FADED = 0x7e0c0361;

    /** The bin's drawable, told apart from the read ticks that may share the time label. */
    private static final class Bin extends android.graphics.drawable.InsetDrawable {
        Bin(Drawable icon) { super(icon, 0); }
    }
}
