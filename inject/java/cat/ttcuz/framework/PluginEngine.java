package cat.ttcuz.framework;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import cat.ttcuz.framework.plugin.*;
import dalvik.system.DexClassLoader;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** ttcuz Plugin Engine: asynchronous loading, in-process Java API, independent lifecycle. */
public final class PluginEngine {

    private PluginEngine() {}

    public static final class Info {

        public final String id, name, version, author, description;
        public volatile boolean enabled, loaded;
        public volatile String error = "";
        private final File folder;
        private final PluginPackage.Metadata metadata;
        private volatile TtcuzPlugin plugin;
        private volatile PluginContext context;
        private final Set<String> hooks = new HashSet<>();

        private Info(File folder, PluginPackage.Metadata metadata) {
            this.folder = folder;
            this.metadata = metadata;
            id = metadata.id;
            name = metadata.name;
            version = metadata.version;
            author = metadata.author;
            description = metadata.description;
        }
    }

    public interface Completion {
        void done(String error);
    }

    private interface Event {
        void accept(TtcuzPlugin plugin);
    }

    private interface Transform<T> {
        T apply(TtcuzPlugin plugin, T value);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r ->
        new Thread(r, "ttcuz-plugins")
    );
    private static final Map<String, Info> installed = new LinkedHashMap<>();
    private static final Set<Runnable> observers = new HashSet<>();
    private static volatile Info[] running = new Info[0],
        catalog = new Info[0];
    private static volatile Info[] colourHooks = new Info[0],
        textHooks = new Info[0],
        viewHooks = new Info[0],
        nicknameHooks = new Info[0],
        regionHooks = new Info[0],
        feedHooks = new Info[0];
    private static final ThreadLocal<Boolean> transforming = new ThreadLocal<>();
    private static Context app;
    private static SharedPreferences options, appearance;
    private static File codeRoot, dataRoot;
    private static volatile boolean started;
    private static final SharedPreferences.OnSharedPreferenceChangeListener globalChanges = (
        p,
        k
    ) -> dispatch(x -> x.onPreferenceChanged("host", k));

    public static synchronized void start(Context context) {
        if (started) return;
        app = context.getApplicationContext();
        codeRoot = new File(app.getFilesDir(), "ttcuz/plugins/code");
        dataRoot = new File(app.getFilesDir(), "ttcuz/plugins/data");
        codeRoot.mkdirs();
        dataRoot.mkdirs();
        options = app.getSharedPreferences("ttcuz_plugins", 0);
        app.getSharedPreferences(Ttcuz.PREFS, 0).registerOnSharedPreferenceChangeListener(
            globalChanges
        );
        appearance = AccountAppearance.prefs(app);
        appearance.registerOnSharedPreferenceChangeListener(globalChanges);
        started = true;
        IO.execute(() -> {
            File[] folders = codeRoot.listFiles();
            if (folders == null) return;
            for (File folder : folders) {
                if (!folder.isDirectory() || !folder.getName().startsWith(".")) continue;
                try {
                    if (folder.getName().startsWith(".backup-")) {
                        PluginPackage.Metadata metadata = PluginPackage.read(folder);
                        File target = PluginPackage.child(codeRoot, metadata.id);
                        if (!target.exists()) {
                            if (!folder.renameTo(target)) throw new IOException(
                                "Cannot restore plugin backup"
                            );
                        } else PluginPackage.delete(folder);
                    } else if (folder.getName().startsWith(".incoming-")) PluginPackage.delete(
                        folder
                    );
                } catch (Exception error) {
                    Diary.note("plugin recovery: " + error);
                }
            }
            folders = codeRoot.listFiles();
            if (folders == null) return;
            Arrays.sort(folders, Comparator.comparing(File::getName));
            for (File folder : folders) {
                if (!folder.isDirectory() || folder.getName().startsWith(".")) continue;
                try {
                    PluginPackage.Metadata metadata = PluginPackage.read(folder);
                    if (!folder.getName().equals(metadata.id)) throw new IOException(
                        "ID не совпадает с папкой"
                    );
                    Info info = new Info(folder, metadata);
                    info.enabled = options.getBoolean(info.id, true);
                    synchronized (installed) {
                        installed.put(info.id, info);
                        publish();
                    }
                    if (info.enabled) load(info);
                } catch (Exception e) {
                    Diary.note("plugin scan " + folder.getName() + ": " + e);
                }
            }
            changed();
        });
    }

    public static List<Info> list() {
        return Collections.unmodifiableList(Arrays.asList(catalog.clone()));
    }

    public static void watch(Runnable observer) {
        synchronized (observers) {
            observers.add(observer);
        }
    }

    public static void unwatch(Runnable observer) {
        synchronized (observers) {
            observers.remove(observer);
        }
    }

    private static volatile int revision;

    public static int revision() {
        return revision;
    }

    private static void changed() {
        revision++;
        MAIN.post(() -> {
            Themes.repaintVisible();
            Runnable[] copy;
            synchronized (observers) {
                copy = observers.toArray(new Runnable[0]);
            }
            for (Runnable listener : copy)
                try {
                    listener.run();
                } catch (Exception e) {
                    Diary.note("plugin list: " + e);
                }
        });
    }

    private static void publish() {
        synchronized (installed) {
            catalog = installed.values().toArray(new Info[0]);
            ArrayList<Info> active = new ArrayList<>();
            for (Info info : catalog) if (info.loaded && info.plugin != null) active.add(info);
            running = active.toArray(new Info[0]);
            colourHooks = select("onColour");
            textHooks = select("onText");
            viewHooks = select("onView");
            nicknameHooks = select("onNickname");
            regionHooks = select("onRegion");
            feedHooks = select("onFeed");
        }
        Accent.forget();
    }

    private static Info[] select(String hook) {
        ArrayList<Info> selected = new ArrayList<>();
        for (Info info : running) if (info.hooks.contains(hook)) selected.add(info);
        return selected.toArray(new Info[0]);
    }

    private static Info info(String id) {
        synchronized (installed) {
            return installed.get(id);
        }
    }

    private static void mainAndWait(Runnable work) throws InterruptedException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            work.run();
            return;
        }
        CountDownLatch done = new CountDownLatch(1);
        MAIN.post(() -> {
            try {
                work.run();
            } finally {
                done.countDown();
            }
        });
        done.await();
    }

    private static Class<? extends TtcuzPlugin> entryType(
        File folder,
        PluginPackage.Metadata metadata
    ) throws Exception {
        PluginPackage.compatible(metadata);
        StringBuilder path = new StringBuilder();
        for (File file : PluginPackage.dexFiles(folder)) {
            if (path.length() > 0) path.append(File.pathSeparator);
            path.append(file.getAbsolutePath());
        }
        File optimized = new File(app.getCodeCacheDir(), "ttcuz-plugins");
        optimized.mkdirs();
        String nativePath = null;
        for (String abi : android.os.Build.SUPPORTED_ABIS) {
            File nativeFolder = PluginPackage.child(folder, "lib/" + abi);
            if (nativeFolder.isDirectory()) {
                nativePath = nativeFolder.getAbsolutePath();
                break;
            }
        }
        ClassLoader loader = new DexClassLoader(
            path.toString(),
            optimized.getAbsolutePath(),
            nativePath,
            app.getClassLoader()
        );
        Class<? extends TtcuzPlugin> type = loader
            .loadClass(metadata.entry)
            .asSubclass(TtcuzPlugin.class);
        if (java.lang.reflect.Modifier.isAbstract(type.getModifiers())) throw new IOException(
            "Класс плагина абстрактный"
        );
        type.getDeclaredConstructor();
        return type;
    }

    private static void load(Info info) {
        try {
            Object made = entryType(info.folder, info.metadata)
                .getDeclaredConstructor()
                .newInstance();
            info.hooks.clear();
            for (java.lang.reflect.Method method : made.getClass().getMethods())
                if (
                    method.getName().startsWith("on") &&
                    method.getDeclaringClass() != TtcuzPlugin.class
                ) info.hooks.add(method.getName());
            if (!(made instanceof TtcuzPlugin)) throw new IOException(
                "Класс должен наследовать TtcuzPlugin"
            );
            PluginContext context = new PluginContext(
                app,
                info.id,
                info.folder,
                PluginPackage.child(dataRoot, info.id)
            );
            info.context = context;
            info.plugin = (TtcuzPlugin) made;
            info.error = "";
            info.plugin.attach(context);
            info.plugin.onLoad();
            mainAndWait(() -> {
                try {
                    if (!info.enabled) {
                        stop(info);
                        return;
                    }
                    info.plugin.onStart();
                    info.plugin.onAccountChanged(Account.liveId());
                    Activity current = Screen.now();
                    if (current != null) info.plugin.onActivityResumed(current);
                    info.loaded = true;
                    publish();
                } catch (Throwable error) {
                    fail(info, error);
                }
            });
        } catch (Throwable error) {
            fail(info, error);
        }
        changed();
    }

    private static void stop(Info info) {
        info.loaded = false;
        TtcuzPlugin plugin = info.plugin;
        info.plugin = null;
        publish();
        try {
            mainAndWait(() -> {
                try {
                    if (plugin != null) plugin.onStop();
                } catch (Throwable e) {
                    Diary.note("plugin stop " + info.id + ": " + e);
                } finally {
                    if (info.context != null) {
                        info.context.close();
                        info.context = null;
                    }
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void fail(Info info, Throwable error) {
        StringWriter trace = new StringWriter();
        error.printStackTrace(new PrintWriter(trace));
        String detail = trace.toString();
        info.error = detail.length() > 8192 ? detail.substring(0, 8192) : detail;
        info.loaded = false;
        publish();
        Diary.note("plugin " + info.id + ": " + error);
        IO.execute(() -> {
            stop(info);
            changed();
        });
    }

    public static void enable(String id, boolean enabled) {
        Info info = info(id);
        if (info == null) return;
        info.enabled = enabled;
        options.edit().putBoolean(id, enabled).apply();
        IO.execute(() -> {
            stop(info);
            info.error = "";
            if (info.enabled) load(info);
            changed();
        });
    }

    public static void reload(String id) {
        Info info = info(id);
        if (info != null) IO.execute(() -> {
            stop(info);
            if (info.enabled) load(info);
            changed();
        });
    }

    public static void install(Uri uri, Completion completion) {
        if (!started) start(Ttcuz.context());
        IO.execute(() -> {
            String error = null;
            File stage = new File(codeRoot, ".incoming-" + UUID.randomUUID());
            File backup = new File(codeRoot, ".backup-" + UUID.randomUUID());
            File target = null;
            Info old = null;
            boolean swapped = false;
            try {
                InputStream input = app.getContentResolver().openInputStream(uri);
                if (input == null) throw new IOException("Не удалось открыть файл");
                PluginPackage.Metadata metadata = PluginPackage.extract(input, stage);
                entryType(stage, metadata); // Verify loadability before replacing an installed bundle.
                target = PluginPackage.child(codeRoot, metadata.id);
                old = info(metadata.id);
                if (old != null) stop(old);
                if (target.exists() && !target.renameTo(backup)) throw new IOException(
                    "Не удалось сохранить предыдущую версию"
                );
                if (!stage.renameTo(target)) {
                    if (backup.exists() && !backup.renameTo(target)) throw new IOException(
                        "Не удалось восстановить предыдущую версию"
                    );
                    throw new IOException("Не удалось установить плагин");
                }
                swapped = true;
                Info next = new Info(target, metadata);
                next.enabled = old == null || old.enabled;
                options.edit().putBoolean(next.id, next.enabled).apply();
                synchronized (installed) {
                    installed.put(next.id, next);
                    publish();
                }
                if (next.enabled) load(next);
                if (!next.error.isEmpty()) throw new IOException(
                    "Не удалось запустить плагин: " + next.error.split("\n", 2)[0]
                );
                if (backup.exists()) PluginPackage.delete(backup);
            } catch (Throwable failure) {
                error = failure.toString();
                try {
                    if (swapped && backup.exists()) {
                        Info bad = old == null ? null : info(old.id);
                        if (bad != null) stop(bad);
                        PluginPackage.delete(target);
                        if (!backup.renameTo(target)) throw new IOException(
                            "Не удалось восстановить предыдущую версию"
                        );
                        if (old != null) {
                            synchronized (installed) {
                                installed.put(old.id, old);
                                publish();
                            }
                            if (old.enabled) load(old);
                        }
                    } else if (!swapped && old != null && old.enabled && !old.loaded) load(old);
                } catch (Exception rollback) {
                    error += "; " + rollback;
                }
                Diary.note("plugin install: " + error);
            } finally {
                try {
                    if (stage.exists()) PluginPackage.delete(stage);
                } catch (Exception e) {
                    Diary.note("plugin stage: " + e);
                }
            }
            String result = error;
            MAIN.post(() -> {
                changed();
                completion.done(result);
            });
        });
    }

    public static void remove(String id, Completion completion) {
        IO.execute(() -> {
            String error = null;
            try {
                Info info = info(id);
                if (info != null) {
                    stop(info);
                    PluginPackage.delete(PluginPackage.child(codeRoot, id));
                    synchronized (installed) {
                        installed.remove(id);
                        publish();
                    }
                    options.edit().remove(id).apply();
                }
            } catch (Exception e) {
                error = e.toString();
            }
            String result = error;
            MAIN.post(() -> {
                changed();
                completion.done(result);
            });
        });
    }

    public static void invoke(String id, Runnable action) {
        Info info = info(id);
        if (info == null || info.plugin == null) return;
        try {
            action.run();
        } catch (Throwable error) {
            fail(info, error);
        }
    }

    private static void dispatch(Event event) {
        for (Info info : running) {
            TtcuzPlugin plugin = info.plugin;
            if (plugin != null) try {
                event.accept(plugin);
            } catch (Throwable error) {
                fail(info, error);
            }
        }
    }

    private static <T> T transform(Info[] active, T initial, Transform<T> action) {
        if (active.length == 0 || Boolean.TRUE.equals(transforming.get())) return initial;
        transforming.set(Boolean.TRUE);
        T value = initial;
        try {
            for (Info info : active) {
                TtcuzPlugin plugin = info.plugin;
                if (plugin != null) try {
                    T next = action.apply(plugin, value);
                    if (next != null) value = next;
                } catch (Throwable error) {
                    fail(info, error);
                }
            }
        } finally {
            transforming.set(Boolean.FALSE);
        }
        return value;
    }

    public static int colour(int value) {
        Info[] active = colourHooks;
        if (active.length == 0 || Boolean.TRUE.equals(transforming.get())) return value;
        transforming.set(Boolean.TRUE);
        try {
            for (Info info : active) {
                TtcuzPlugin plugin = info.plugin;
                if (plugin != null) try {
                    value = plugin.onColour(value);
                } catch (Throwable error) {
                    fail(info, error);
                }
            }
        } finally {
            transforming.set(Boolean.FALSE);
        }
        return value;
    }

    public static String region(String key, String value) {
        return regionHooks.length == 0
            ? value
            : transform(regionHooks, value, (p, v) -> p.onRegion(key, v));
    }

    public static String nickname(String uid, String value) {
        return nicknameHooks.length == 0
            ? value
            : transform(nicknameHooks, value, (p, v) -> p.onNickname(uid, v));
    }

    public static CharSequence text(TextView view, CharSequence value) {
        return textHooks.length == 0
            ? value
            : transform(textHooks, value, (p, v) -> p.onText(view, v));
    }

    public static List feed(List posts) {
        return feedHooks.length == 0 ? posts : transform(feedHooks, posts, (p, v) -> p.onFeed(v));
    }

    public static boolean hasViewHooks() {
        return viewHooks.length != 0;
    }

    public static void view(View view) {
        if (!hasViewHooks() || Boolean.TRUE.equals(transforming.get())) return;
        transforming.set(Boolean.TRUE);
        try {
            for (Info info : viewHooks) {
                TtcuzPlugin plugin = info.plugin;
                if (plugin != null) try {
                    plugin.onView(view);
                } catch (Throwable error) {
                    fail(info, error);
                }
            }
        } finally {
            transforming.set(Boolean.FALSE);
        }
    }

    public static void created(Activity activity) {
        dispatch(p -> p.onActivityCreated(activity));
    }

    public static void resumed(Activity activity) {
        dispatch(p -> p.onActivityResumed(activity));
    }

    public static void paused(Activity activity) {
        dispatch(p -> p.onActivityPaused(activity));
    }

    public static void destroyed(Activity activity) {
        dispatch(p -> p.onActivityDestroyed(activity));
    }

    public static void accountChanged(String uid) {
        if (!started) return;
        if (appearance != null) appearance.unregisterOnSharedPreferenceChangeListener(
            globalChanges
        );
        appearance = AccountAppearance.prefs(app);
        appearance.registerOnSharedPreferenceChangeListener(globalChanges);
        dispatch(p -> {
            p.ttcuz().accountChanged();
            p.onAccountChanged(uid);
        });
    }

    public static void preference(String id, String scope, String key) {
        Info info = info(id);
        if (info != null) invoke(id, () -> info.plugin.onPreferenceChanged(scope, key));
    }

    private static final ThreadLocal<Set<String>> emitting = new ThreadLocal<>();

    public static void emit(String event, Object payload) {
        Objects.requireNonNull(event, "event");
        Set<String> stack = emitting.get();
        if (stack == null) {
            stack = new HashSet<>();
            emitting.set(stack);
        }
        if (!stack.add(event)) return;
        try {
            dispatch(p -> p.onEvent(event, payload));
        } finally {
            stack.remove(event);
        }
    }

    public static PluginSettings settings(String id, Activity activity) {
        Info info = info(id);
        PluginSettings settings = new PluginSettings();
        boolean[] custom = { false };
        if (info != null && info.loaded) invoke(id, () -> {
            custom[0] = info.plugin.onOpenSettings(activity);
            if (!custom[0]) info.plugin.onSettings(settings);
        });
        return custom[0] ? null : settings;
    }

    public static SharedPreferences prefs(String id) {
        Info info = info(id);
        return info == null || info.context == null ? null : info.context.prefs();
    }
}
