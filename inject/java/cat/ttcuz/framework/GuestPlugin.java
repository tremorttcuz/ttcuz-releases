package cat.ttcuz.framework;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.ViewGroup;
import android.widget.TextView;
import cat.narezany.margyt.plugin.MargyPlugin;
import cat.narezany.margyt.plugin.PluginContext;
import cat.ttcuz.framework.plugin.PluginSettings;
import cat.ttcuz.framework.plugin.TtcuzPlugin;
import java.io.File;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A plugin of the other plugin format, run as a ttcuz plugin.
 *
 * Such a package is laid out like a .ctt (manifest.json, classes.dex, icon.png) and installs the same way, under any
 * file name. Its main class extends the compatibility base class instead of TtcuzPlugin; this adapter holds it, hands
 * it its context, and turns each ttcuz hook into the hook it knows. It counts as Plugin API 1: no permission list.
 */
public final class GuestPlugin extends TtcuzPlugin {
    private final MargyPlugin guest;
    private final Set<String> hooks;
    private PluginContext context;

    static boolean isGuest(Class<?> type) { return MargyPlugin.class.isAssignableFrom(type); }

    GuestPlugin(MargyPlugin guest) {
        this.guest = guest;
        this.hooks = hooksOf(guest.getClass());
        Bridge.install();
    }

    /** Rows it registered, or a window of its own. */
    boolean hasSettings() { return (context != null && !context.rows().isEmpty()) || hooks.contains("onOpenSettings"); }

    /** The ttcuz hooks this plugin really uses, so the engine calls only those (onColour is on the drawing path). */
    Set<String> hooks() { return hooks; }

    private static Set<String> hooksOf(Class<?> type) {
        Set<String> out = new HashSet<>();
        for (Method method : type.getMethods()) {
            if (method.getDeclaringClass() == MargyPlugin.class || method.getDeclaringClass() == Object.class) continue;
            switch (method.getName()) {
                case "onStart": out.add("onStart"); break;
                case "onActivityCreated": out.add("onActivityCreated"); break;
                case "onActivityResumed": out.add("onActivityResumed"); break;
                case "onActivityPaused": out.add("onActivityPaused"); break;
                case "onOpenSettings": out.add("onOpenSettings"); break;
                case "onAccountChanged": out.add("onAccountChanged"); break;
                case "onSettingsChanged": out.add("onPreferenceChanged"); break;
                case "onColour": out.add("onColour"); break;
                case "onRegion": out.add("onRegion"); break;
                case "onScreen": out.add("onScreen"); break;
                case "onFeed": out.add("onFeed"); break;
                case "onName": out.add("onNickname"); break;
                case "onText": out.add("onText"); break;
                case "onStop": out.add("onStop"); break;
                default: break;
            }
        }
        return out;
    }

    @Override
    public void onLoad() {
        final String id = ttcuz().id;
        context = new PluginContext(ttcuz().app(), id, ttcuz().bundle(), line -> Diary.note("plugin " + id + ": " + line));
        guest.attach(context);
    }

    @Override public void onStart() { context.rows().clear(); guest.onStart(ttcuz().app()); }
    @Override public void onStop() { try { guest.onStop(); } finally { if (context != null) context.close(); } }
    @Override public void onActivityCreated(Activity activity) { guest.onActivityCreated(activity); }
    @Override public void onActivityResumed(Activity activity) { guest.onActivityResumed(activity); }
    @Override public void onActivityPaused(Activity activity) { guest.onActivityPaused(activity); }
    @Override public void onAccountChanged(String uid) { guest.onAccountChanged(uid); }
    @Override public void onPreferenceChanged(String scope, String key) { guest.onSettingsChanged(key); }
    @Override public int onColour(int colour) { return guest.onColour(colour); }
    @Override public String onRegion(String key, String value) { return guest.onRegion(key, value); }
    @Override public List onFeed(List posts) { return guest.onFeed(posts); }
    @Override public String onNickname(String uid, String name) { return guest.onName(uid, name); }
    @Override public CharSequence onText(TextView view, CharSequence text) { return guest.onText(text); }
    @Override public void onScreen(Activity activity, ViewGroup content) { guest.onScreen(activity, activity.getClass().getName()); }

    @Override
    public boolean onOpenSettings(Activity activity) {
        return guest.onOpenSettings(activity);
    }

    /** The rows the plugin registered, as its ttcuz settings page. */
    @Override
    public void onSettings(PluginSettings page) {
        for (final PluginContext.Row row : context.rows()) {
            try {
                switch (row.kind) {
                    case "section": page.text(row.title); break;
                    case "row": page.action(row.detail == null || row.detail.isEmpty() ? row.title : row.title + "\n" + row.detail,
                            () -> { if (row.action != null) row.action.tapped(); }); break;
                    case "toggle": page.toggle(row.key, row.title, row.flag); break;
                    case "choice": page.choice(row.key, row.title, row.options, row.initial); break;
                    case "slider": page.slider(row.key, row.title, row.min, row.max, row.initial); break;
                    default: break;
                }
            } catch (RuntimeException bad) {
                Diary.note("plugin " + ttcuz().id + ": settings row " + row.title + ": " + bad.getMessage());
            }
        }
        if (context.rows().isEmpty()) page.text(Text.t("У этого плагина нет настроек."));
    }

    // ------------------------------------------------------------ what the compatibility API asks of ttcuz

    private static final class Bridge implements PluginContext.Host {
        private static boolean installed;
        static synchronized void install() { if (!installed) { installed = true; PluginContext.host(new Bridge()); } }

        private static Context app() { return Ttcuz.context(); }

        @Override public SharedPreferences accountPrefs(String id) {
            String who = Account.liveId();
            return app().getSharedPreferences("ttcuz_plugin_" + id + "_" + (who == null || who.isEmpty() ? "guest" : who), 0);
        }
        @Override public String currentVideoId() { return VideoActions.currentId(); }
        @Override public long currentVideoPublishedTime() { return VideoActions.currentPublishedTime(); }
        @Override public void choose(String title, String[] options, PluginContext.Selected callback) {
            final Activity activity = Screen.now();
            main(() -> PluginBridge.menu(activity, title, options, index -> { if (callback != null) callback.selected(index); }));
        }
        @Override public String accountId() { return Account.liveId(); }
        @Override public int accentColour() { return Accent.colour(); }
        @Override public Activity screen() { return Screen.now(); }
        @Override public void copyText(String text) {
            main(() -> {
                android.content.ClipboardManager clip = (android.content.ClipboardManager) app().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clip != null) clip.setPrimaryClip(android.content.ClipData.newPlainText("ttcuz", text));
            });
        }
        private final Set<String> downloads = new HashSet<>();
        @Override public void download(String id, String url, String name, PluginContext.Downloaded callback) {
            final String job = id + "/" + name;
            synchronized (downloads) { if (!downloads.add(job)) throw new IllegalStateException("download already running"); }
            Net.away("plugin download " + job, () -> {
                File folder = new File(app().getFilesDir(), "ttcuz-plugin-files/" + id);
                File file = new File(folder, name);
                boolean ok = false;
                try { folder.mkdirs(); ok = Net.download(url, file, (percent, got, size) -> Screen.progress("plugin:" + id, name, percent)); }
                catch (Throwable error) { Diary.note("plugin " + id + " download: " + error); }
                finally { synchronized (downloads) { downloads.remove(job); } Screen.progressGone("plugin:" + id); }
                final boolean done = ok;
                main(() -> { if (callback != null) callback.finished(file, done); });
            });
        }
        @Override public void showWindow(String title, String message) {
            final Activity activity = Screen.now();
            main(() -> PluginBridge.dialog(activity, title, message, Text.t("Готово"), null, null));
        }
        @Override public void offer(String id, String label, PluginContext.Tapped action) {
            final Activity activity = Screen.now();
            main(() -> PluginBridge.dialog(activity, label, null, label, () -> { if (action != null) action.tapped(); }, Text.t("Отмена")));
        }
        @Override public void progress(String id, String what, int percent) { Screen.progress("plugin:" + id, what, percent); }
        @Override public void progressDone(String id) { Screen.progressDone("plugin:" + id); }
        @Override public void progressGone(String id) { Screen.progressGone("plugin:" + id); }
        @Override public byte[] fetch(String url) { return Net.bytes(url); }
        @Override public void away(String id, String what, Runnable work) { Net.away("plugin " + id + ": " + what, work); }

        private static void main(Runnable work) { new android.os.Handler(android.os.Looper.getMainLooper()).post(work); }
    }
}
