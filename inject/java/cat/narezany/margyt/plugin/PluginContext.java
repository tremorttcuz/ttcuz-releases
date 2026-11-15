package cat.narezany.margyt.plugin;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;

/**
 * Compatibility API: what a plugin of the other format is handed (see MargyPlugin for why the name is fixed). Each
 * call is answered by ttcuz through cat.ttcuz.framework.GuestPlugin; the settings rows a plugin registers become rows
 * of its ttcuz settings page.
 */
public final class PluginContext {
    public interface Diarist { void note(String line); }
    public interface Selected { void selected(int index); }
    public interface Downloaded { void finished(File file, boolean success); }
    public interface Tapped { void tapped(); }

    /** A row the plugin registered, kept until ttcuz draws the plugin's settings page. */
    public static final class Row {
        public final String kind, title, detail, key; public final String[] options; public final int min, max, initial;
        public final boolean flag; public final Tapped action;
        Row(String kind, String title, String detail, String key, String[] options, int min, int max, int initial, boolean flag, Tapped action) {
            this.kind = kind; this.title = title; this.detail = detail; this.key = key; this.options = options;
            this.min = min; this.max = max; this.initial = initial; this.flag = flag; this.action = action;
        }
    }

    /** What ttcuz does for this API; set once by GuestPlugin. */
    public interface Host {
        SharedPreferences accountPrefs(String id);
        String currentVideoId();
        long currentVideoPublishedTime();
        void choose(String title, String[] options, Selected callback);
        String accountId();
        int accentColour();
        android.app.Activity screen();
        void copyText(String text);
        void download(String id, String url, String name, Downloaded callback);
        void showWindow(String title, String message);
        void offer(String id, String label, Tapped action);
        void progress(String id, String what, int percent);
        void progressDone(String id);
        void progressGone(String id);
        byte[] fetch(String url);
        void away(String id, String what, Runnable work);
    }

    private static volatile Host host;
    public static void host(Host value) { host = value; }

    private final Context context;
    private final String id;
    private final File folder;
    private final Diarist diarist;
    private final java.util.List<Row> rows = new java.util.concurrent.CopyOnWriteArrayList<>();
    /** Set when the plugin is turned off or removed: from then on nothing it asks for reaches TikTok. */
    private volatile boolean closed;
    public void close() { closed = true; }

    public PluginContext(Context context, String id, File folder, Diarist diarist) {
        this.context = context.getApplicationContext();
        this.id = id;
        this.folder = folder;
        this.diarist = diarist;
    }

    public java.util.List<Row> rows() { return rows; }

    public Context context() { return context; }
    public String id() { return id; }
    public File folder() { return folder; }
    public SharedPreferences prefs() { return context.getSharedPreferences("ttcuz_plugin_" + id, 0); }
    public SharedPreferences accountPrefs() { return host.accountPrefs(id); }
    public String currentVideoId() { return host.currentVideoId(); }
    public long currentVideoPublishedTime() { return host.currentVideoPublishedTime(); }

    public String getString(String key, String fallback) { return prefs().getString(key, fallback); }
    public void putString(String key, String value) {
        if (key == null || key.length() > 80 || value == null || value.length() > 8192) throw new IllegalArgumentException("setting size");
        prefs().edit().putString(key, value).apply();
    }
    public boolean getBoolean(String key, boolean def) { return prefs().getBoolean(key, def); }
    public int getInt(String key, int def) { return prefs().getInt(key, def); }

    public void choose(String title, String[] options, Selected callback) { if (closed) return; host.choose(title, options, callback); }
    public String accountId() { return host.accountId(); }
    public int accentColour() { return host.accentColour(); }
    public android.app.Activity activity() { if (closed) return null; return host.screen(); }
    public android.app.Activity screen() { if (closed) return null; return host.screen(); }
    public void onUiThread(Runnable action) { if (closed) return; new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> { if (!closed) action.run(); }); }
    public void copyText(String text) { if (closed) return; host.copyText(text); }
    public void download(String url, String name, Downloaded callback) { if (closed) return;
        if (name == null || !name.matches("[A-Za-z0-9._-]{1,80}") || name.equals(".") || name.equals("..")) throw new IllegalArgumentException("file name");
        host.download(id, url, name, callback);
    }

    public void addSettingsRow(String title, String detail, Tapped action) { rows.add(new Row("row", title, detail, null, null, 0, 0, 0, false, action)); }
    public void addSettingsSection(String title) { rows.add(new Row("section", title, null, null, null, 0, 0, 0, false, null)); }
    public void addSettingsToggle(String title, String detail, String key, boolean def) { rows.add(new Row("toggle", title, detail, key, null, 0, 0, 0, def, null)); }
    public void addSettingsChoice(String title, String detail, String key, String[] options, int def) { rows.add(new Row("choice", title, detail, key, options, 0, 0, def, false, null)); }
    public void addSettingsSlider(String title, String detail, String key, int min, int max, int def) { rows.add(new Row("slider", title, detail, key, null, min, max, def, false, null)); }

    public void showWindow(String title, String message) { if (closed) return; host.showWindow(title, message); }
    public void offer(String label, Tapped action) { if (closed) return; host.offer(id, label, action); }
    public void progress(String what, int percent) { if (closed) return; host.progress(id, what, percent); }
    public void progressDone() { host.progressDone(id); }
    public void progressGone() { host.progressGone(id); }
    /** null rather than an exception when it cannot be had. */
    public byte[] fetch(String url) { if (closed) return null; try { return host.fetch(url); } catch (Throwable error) { return null; } }
    public void away(String what, Runnable work) { if (closed) return; host.away(id, what, () -> { if (!closed) work.run(); }); }
    public void log(String line) { if (diarist != null) diarist.note(line); }
}
