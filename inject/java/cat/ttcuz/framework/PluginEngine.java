package cat.ttcuz.framework;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import cat.ttcuz.framework.PluginEngine;
import cat.ttcuz.framework.PluginPackage;
import cat.ttcuz.framework.plugin.PluginContext;
import cat.ttcuz.framework.plugin.PluginSettings;
import cat.ttcuz.framework.plugin.TtcuzPlugin;
import dalvik.system.DexClassLoader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class PluginEngine {
    private static Context app;
    private static SharedPreferences appearance;
    private static File codeRoot;
    private static File dataRoot;
    private static SharedPreferences options;
    private static volatile int revision;
    private static volatile boolean started;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(new ThreadFactory() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda15
        @Override // java.util.concurrent.ThreadFactory
        public final Thread newThread(Runnable runnable) {
            return PluginEngine.lambda$static$0(runnable);
        }
    });
    private static final Map<String, Info> installed = new LinkedHashMap();
    private static final Set<Runnable> observers = new HashSet();
    private static final ConcurrentHashMap<String, String> packageOwners = new ConcurrentHashMap<>();
    private static volatile Info[] running = new Info[0];
    private static volatile Info[] catalog = new Info[0];
    private static volatile Info[] colourHooks = new Info[0];
    private static volatile Info[] textHooks = new Info[0];
    private static volatile Info[] viewHooks = new Info[0];
    private static volatile Info[] nicknameHooks = new Info[0];
    private static volatile Info[] regionHooks = new Info[0];
    private static volatile Info[] feedHooks = new Info[0];
    private static volatile Info[] screenHooks = new Info[0];
    private static volatile Info[] tiktokHooks = new Info[0];
    private static volatile Info[] modScreenHooks = new Info[0], modPanelHooks = new Info[0], modSettingsHooks = new Info[0];
    private static final ThreadLocal<Boolean> transforming = new ThreadLocal<>();
    private static final SharedPreferences.OnSharedPreferenceChangeListener globalChanges = new SharedPreferences.OnSharedPreferenceChangeListener() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda16
        @Override // android.content.SharedPreferences.OnSharedPreferenceChangeListener
        public final void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
            PluginEngine.dispatch(new PluginEngine.Event() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda9
                @Override // cat.ttcuz.framework.PluginEngine.Event
                public final void accept(TtcuzPlugin ttcuzPlugin) {
                    ttcuzPlugin.onPreferenceChanged("host", str);
                }
            });
        }
    };
    private static final ThreadLocal<Set<String>> emitting = new ThreadLocal<>();

    public interface Completion {
        void done(String str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    interface Event {
        void accept(TtcuzPlugin ttcuzPlugin);
    }

    /* JADX INFO: Access modifiers changed from: private */
    interface Transform<T> {
        T apply(TtcuzPlugin ttcuzPlugin, T t);
    }

    private PluginEngine() {
    }

    public static final class Info {
        public final String author;
        private volatile PluginContext context;
        public final String description;
        public volatile boolean enabled;
        public volatile String error;
        private final File folder;
        private final Set<String> hooks;
        public final String id;
        public volatile boolean loaded;
        private final PluginPackage.Metadata metadata;
        public final String name;
        private volatile TtcuzPlugin plugin;
        public final String version;

        private Info(File file, PluginPackage.Metadata metadata) {
            this.error = "";
            this.hooks = new HashSet();
            this.folder = file;
            this.metadata = metadata;
            this.id = metadata.id;
            this.name = metadata.name;
            this.version = metadata.version;
            this.author = metadata.author;
            this.description = metadata.description;
        }

        /** The plugin API it was written for. */
        public int api() { return metadata.api; }

        /** What its manifest asks for. */
        public java.util.List<String> declared() { return metadata.permissions; }

        /** What the author says it does with a permission, or null. */
        public String reason(String permission) { return metadata.uses.get(permission); }

        /** What it can do: its list, and for API 1 what its hooks imply (known once it has loaded). */
        public java.util.List<String> permissions() {
            synchronized (hooks) { return PluginPermissions.effective(metadata.api, metadata.permissions, new HashSet<>(hooks)); }
        }

        /** The plugin's own picture from its package, or null. */
        public File icon() {
            File icon = new File(folder, "icon.png");
            return icon.isFile() && icon.length() <= 1048576 ? icon : null;
        }
    }

    static /* synthetic */ Thread lambda$static$0(Runnable runnable) {
        return new Thread(runnable, "ttcuz-plugins");
    }

    static String owner(String str) {
        String str2 = null;
        if (str == null) {
            return null;
        }
        int i = 0;
        for (Map.Entry<String, String> entry : packageOwners.entrySet()) {
            if (str.startsWith(entry.getKey()) && entry.getKey().length() > i) {
                str2 = entry.getValue();
                i = entry.getKey().length();
            }
        }
        return str2;
    }

    private static void claim(Info info) {
        int lastIndexOf = info.metadata.entry.lastIndexOf(46);
        if (lastIndexOf <= 0) {
            return;
        }
        String substring = info.metadata.entry.substring(0, lastIndexOf + 1);
        String[] strArr = {"cat.ttcuz.framework.", "android.", "androidx.", "java.", "kotlin.", "com.ss.", "com.bytedance.", "X."};
        for (int i = 0; i < 8; i++) {
            if (substring.startsWith(strArr[i])) {
                return;
            }
        }
        packageOwners.merge(substring, info.id, new BiFunction() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda27
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return PluginEngine.lambda$claim$1((String) obj, (String) obj2);
            }
        });
    }

    static /* synthetic */ String lambda$claim$1(String str, String str2) {
        if (str.equals(str2) || Arrays.asList(str.split(" / ")).contains(str2)) {
            return str;
        }
        return str + " / " + str2;
    }

    public static synchronized void start(Context context) {
        synchronized (PluginEngine.class) {
            if (started) {
                return;
            }
            app = context.getApplicationContext();
            codeRoot = new File(app.getFilesDir(), "ttcuz/plugins/code");
            dataRoot = new File(app.getFilesDir(), "ttcuz/plugins/data");
            codeRoot.mkdirs();
            dataRoot.mkdirs();
            options = app.getSharedPreferences("ttcuz_plugins", 0);
            SharedPreferences sharedPreferences = app.getSharedPreferences("ttcuz", 0);
            SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = globalChanges;
            sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
            SharedPreferences prefs = AccountAppearance.prefs(app);
            appearance = prefs;
            prefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
            started = true;
            IO.execute(new Runnable() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda19
                @Override // java.lang.Runnable
                public final void run() {
                    PluginEngine.lambda$start$4();
                }
            });
        }
    }

    static /* synthetic */ void lambda$start$4() {
        File[] listFiles = codeRoot.listFiles();
        if (listFiles == null) {
            return;
        }
        for (File file : listFiles) {
            if (file.isDirectory() && file.getName().startsWith(".")) {
                try {
                    if (file.getName().startsWith(".backup-")) {
                        File child = PluginPackage.child(codeRoot, PluginPackage.read(file).id);
                        if (!child.exists()) {
                            if (!file.renameTo(child)) {
                                throw new IOException("Cannot restore plugin backup");
                            }
                        } else {
                            PluginPackage.delete(file);
                        }
                    } else if (file.getName().startsWith(".incoming-")) {
                        PluginPackage.delete(file);
                    }
                } catch (Exception e) {
                    Diary.note("plugin recovery: " + String.valueOf(e));
                }
            }
        }
        File[] listFiles2 = codeRoot.listFiles();
        if (listFiles2 == null) {
            return;
        }
        Arrays.sort(listFiles2, Comparator.comparing(new Function() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda17
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((File) obj).getName();
            }
        }));
        for (File file2 : listFiles2) {
            if (file2.isDirectory() && !file2.getName().startsWith(".")) {
                try {
                    PluginPackage.Metadata read = PluginPackage.read(file2);
                    if (!file2.getName().equals(read.id)) {
                        throw new IOException(Text.t("ID не совпадает с папкой"));
                    }
                    Info info = new Info(file2, read);
                    info.enabled = options.getBoolean(info.id, true);
                    Map<String, Info> map = installed;
                    synchronized (map) {
                        map.put(info.id, info);
                        publish();
                    }
                    if (info.enabled && !safeMode()) {
                        load(info);
                    } else if (info.enabled) {
                        info.error = Text.t("Безопасный режим: плагины не запускаются при старте");
                    }
                } catch (Exception e2) {
                    Diary.note("plugin scan " + file2.getName() + ": " + String.valueOf(e2));
                }
            }
        }
        changed();
    }

    /**
     * What the confirmation promises: removing a plugin takes its data with it -- its data() folder and its settings,
     * the shared ones and each account's (ttcuz_plugin_<id> and ttcuz_plugin_<id>_<account>). An update keeps them.
     */
    private static void forget(String id) {
        try { PluginPackage.delete(PluginPackage.child(dataRoot, id)); } catch (Exception error) { Diary.note("plugin " + id + " data: " + error); }
        File shared = new File(app.getApplicationInfo().dataDir, "shared_prefs");
        String[] names = shared.list();
        if (names == null) return;
        String own = "ttcuz_plugin_" + id;
        for (String name : names) {
            if (!name.endsWith(".xml")) continue;
            String base = name.substring(0, name.length() - 4);
            // own file, or own + "_" + an account id ("guest" when signed out) -- never another plugin whose id
            // happens to start the same way (com.x and com.x_y)
            String rest = base.startsWith(own + "_") ? base.substring(own.length() + 1) : null;
            if (base.equals(own) || (rest != null && (rest.equals("guest") || rest.matches("\\d{1,25}")))) {
                app.getSharedPreferences(base, 0).edit().clear().commit();
                app.deleteSharedPreferences(base);
            }
        }
    }

    public static final String KEY_SAFE = "plugins_safe_mode";

    /** Safe mode: TikTok starts with no plugin running (a plugin that breaks the start can then be removed). */
    public static boolean safeMode() {
        Context context = app != null ? app : Ttcuz.context();
        return context != null && context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SAFE, false);
    }

    public static void setSafeMode(boolean on) {
        Context context = app != null ? app : Ttcuz.context();
        if (context != null) context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_SAFE, on).apply();
    }

    public static List<Info> list() {
        return Collections.unmodifiableList(Arrays.asList((Info[]) catalog.clone()));
    }

    public static void watch(Runnable runnable) {
        Set<Runnable> set = observers;
        synchronized (set) {
            set.add(runnable);
        }
    }

    public static void unwatch(Runnable runnable) {
        Set<Runnable> set = observers;
        synchronized (set) {
            set.remove(runnable);
        }
    }

    public static int revision() {
        return revision;
    }

    private static void changed() {
        revision++;
        MAIN.post(new Runnable() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                PluginEngine.lambda$changed$5();
            }
        });
    }

    static /* synthetic */ void lambda$changed$5() {
        int i;
        Runnable[] runnableArr;
        Themes.repaintVisible();
        Set<Runnable> set = observers;
        synchronized (set) {
            runnableArr = (Runnable[]) set.toArray(new Runnable[0]);
        }
        for (Runnable runnable : runnableArr) {
            try {
                runnable.run();
            } catch (Exception e) {
                Diary.note("plugin list: " + String.valueOf(e));
            }
        }
    }

    private static void publish() {
        Map<String, Info> map = installed;
        synchronized (map) {
            catalog = (Info[]) map.values().toArray(new Info[0]);
            ArrayList arrayList = new ArrayList();
            for (Info info : catalog) {
                if (info.loaded && info.plugin != null) {
                    arrayList.add(info);
                }
            }
            running = (Info[]) arrayList.toArray(new Info[0]);
            colourHooks = select("onColour");
            textHooks = select("onText");
            viewHooks = select("onView");
            nicknameHooks = select("onNickname");
            regionHooks = select("onRegion");
            feedHooks = select("onFeed");
            screenHooks = select("onScreen");
            modScreenHooks = select("onModScreen");
            modPanelHooks = select("onModPanel");
            modSettingsHooks = select("onModSettings");
            tiktokHooks = select("onTikTok");
        }
        Accent.forget();
    }

    private static Info[] select(String str) {
        ArrayList arrayList = new ArrayList();
        for (Info info : running) {
            if (info.hooks.contains(str)) {
                arrayList.add(info);
            }
        }
        return (Info[]) arrayList.toArray(new Info[0]);
    }

    private static Info info(String str) {
        Info info;
        Map<String, Info> map = installed;
        synchronized (map) {
            info = map.get(str);
        }
        return info;
    }

    private static void mainAndWait(final Runnable runnable) throws InterruptedException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
            return;
        }
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        MAIN.post(new Runnable() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda24
            @Override // java.lang.Runnable
            public final void run() {
                PluginEngine.lambda$mainAndWait$6(runnable, countDownLatch);
            }
        });
        countDownLatch.await();
    }

    static /* synthetic */ void lambda$mainAndWait$6(Runnable runnable, CountDownLatch countDownLatch) {
        try {
            runnable.run();
        } finally {
            countDownLatch.countDown();
        }
    }

    private static Class<?> entryType(File file, PluginPackage.Metadata metadata) throws Exception {
        String str;
        PluginPackage.compatible(metadata);
        StringBuilder sb = new StringBuilder();
        for (File file2 : PluginPackage.dexFiles(file)) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparator);
            }
            sb.append(file2.getAbsolutePath());
        }
        File file3 = new File(app.getCodeCacheDir(), "ttcuz-plugins");
        file3.mkdirs();
        String[] strArr = Build.SUPPORTED_ABIS;
        int length = strArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                str = null;
                break;
            }
            File child = PluginPackage.child(file, "lib/" + strArr[i]);
            if (child.isDirectory()) {
                str = child.getAbsolutePath();
                break;
            }
            i++;
        }
        Class<?> asSubclass = new DexClassLoader(sb.toString(), file3.getAbsolutePath(), str, app.getClassLoader()).loadClass(metadata.entry);
        // a ttcuz plugin, or one of the other format, run through GuestPlugin
        if (!TtcuzPlugin.class.isAssignableFrom(asSubclass) && !GuestPlugin.isGuest(asSubclass))
            throw new IOException(Text.t("Главный класс не является плагином"));
        if (Modifier.isAbstract(asSubclass.getModifiers())) {
            throw new IOException(Text.t("Класс плагина абстрактный"));
        }
        asSubclass.getDeclaredConstructor(null);
        return asSubclass;
    }

    /** Loads, attaches and starts one plugin; anything it throws marks it failed instead of reaching TikTok. */
    private static void load(final Info info) {
        try {
            Object made = entryType(info.folder, info.metadata).getDeclaredConstructor().newInstance();
            TtcuzPlugin plugin = made instanceof TtcuzPlugin ? (TtcuzPlugin) made : new GuestPlugin((cat.narezany.margyt.plugin.MargyPlugin) made);
            info.hooks.clear();
            if (plugin instanceof GuestPlugin) info.hooks.addAll(((GuestPlugin) plugin).hooks());
            else for (Method method : plugin.getClass().getMethods()) {
                if (method.getName().startsWith("on") && method.getDeclaringClass() != TtcuzPlugin.class) info.hooks.add(method.getName());
            }
            java.util.List<String> lacking = PluginPermissions.missing(info.metadata.api, info.metadata.permissions, info.hooks);
            if (!lacking.isEmpty()) {
                StringBuilder names = new StringBuilder();
                for (String name : lacking) names.append(names.length() == 0 ? "" : ", ").append('"').append(name).append('"');
                throw new IOException(Text.t("Плагину нужны разрешения в manifest.json") + ": " + names);
            }
            PluginContext pluginContext = new PluginContext(app, info.id, info.folder, PluginPackage.child(dataRoot, info.id), info.metadata.api, info.metadata.permissions);
            info.context = pluginContext;
            info.plugin = plugin;
            info.error = "";
            info.plugin.attach(pluginContext);
            claim(info);
            info.plugin.onLoad();
            mainAndWait(() -> lambda$load$7(info));
            changed();
        } catch (Throwable th) {
            fail(info, th);
        }
    }

    static /* synthetic */ void lambda$load$7(Info info) {
        try {
            if (!info.enabled) {
                stop(info);
                return;
            }
            info.plugin.onStart();
            info.plugin.onAccountChanged(Account.liveId());
            Activity now = Screen.now();
            if (now != null) {
                info.plugin.onActivityResumed(now);
            }
            info.loaded = true;
            publish();
        } catch (Throwable th) {
            fail(info, th);
        }
    }

    private static void stop(final Info info) {
        info.loaded = false;
        final TtcuzPlugin ttcuzPlugin = info.plugin;
        info.plugin = null;
        publish();
        try {
            mainAndWait(new Runnable() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda23
                @Override // java.lang.Runnable
                public final void run() {
                    PluginEngine.lambda$stop$8(ttcuzPlugin, info);
                }
            });
        } catch (InterruptedException unused) {
            Thread.currentThread().interrupt();
        }
    }

    /** The plugin's own goodbye, then everything it was given taken back (timers, threads, views, effects), always. */
    static /* synthetic */ void lambda$stop$8(TtcuzPlugin ttcuzPlugin, Info info) {
        try {
            if (ttcuzPlugin != null) ttcuzPlugin.onStop();
        } catch (Throwable th) {
            Diary.note("plugin stop " + info.id + ": " + String.valueOf(th));
        } finally {
            if (info.context != null) {
                try { info.context.close(); } catch (Throwable th) { Diary.note("plugin close " + info.id + ": " + th); }
                info.context = null;
            }
        }
    }

    private static void fail(final Info info, Throwable th) {
        StringWriter stringWriter = new StringWriter();
        th.printStackTrace(new PrintWriter(stringWriter));
        String stringWriter2 = stringWriter.toString();
        if (stringWriter2.length() > 8192) {
            stringWriter2 = stringWriter2.substring(0, 8192);
        }
        info.error = stringWriter2;
        info.loaded = false;
        publish();
        Diary.note("plugin " + info.id + ": " + String.valueOf(th));
        IO.execute(new Runnable() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda21
            @Override // java.lang.Runnable
            public final void run() {
                PluginEngine.lambda$fail$9(info);
            }
        });
    }

    static /* synthetic */ void lambda$fail$9(Info info) {
        stop(info);
        changed();
    }

    public static void enable(String str, boolean z) {
        final Info info = info(str);
        if (info == null) {
            return;
        }
        info.enabled = z;
        options.edit().putBoolean(str, z).apply();
        IO.execute(new Runnable() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                PluginEngine.lambda$enable$10(info);
            }
        });
    }

    static /* synthetic */ void lambda$enable$10(Info info) {
        stop(info);
        info.error = "";
        if (info.enabled) {
            load(info);
        }
        changed();
    }

    public static void reload(String str) {
        final Info info = info(str);
        if (info != null) {
            IO.execute(new Runnable() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    PluginEngine.lambda$reload$11(info);
                }
            });
        }
    }

    static /* synthetic */ void lambda$reload$11(Info info) {
        stop(info);
        if (info.enabled) {
            load(info);
        }
        changed();
    }

    public static void install(final Uri uri, final Completion completion) {
        if (!started) {
            start(Ttcuz.context());
        }
        IO.execute(new Runnable() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                PluginEngine.lambda$install$13(uri, completion);
            }
        });
    }

    /**
     * Installs or updates a plugin from a .ctt: unpacked and checked aside, the running version stopped and kept as a
     * backup, the new one moved in and started. Any failure puts the previous version back as it was.
     */
    static void lambda$install$13(Uri uri, final Completion completion) {
        File incoming = new File(codeRoot, ".incoming-" + UUID.randomUUID());
        File backup = new File(codeRoot, ".backup-" + UUID.randomUUID());
        String problem = null;
        Info previous = null, fresh = null;
        File target = null;
        boolean placed = false;
        try {
            PluginPackage.Metadata metadata;
            try (InputStream in = app.getContentResolver().openInputStream(uri)) {
                if (in == null) throw new IOException(Text.t("Не удалось открыть файл"));
                metadata = PluginPackage.extract(in, incoming);
            }
            entryType(incoming, metadata);
            target = PluginPackage.child(codeRoot, metadata.id);
            previous = info(metadata.id);
            if (previous != null) stop(previous);
            if (target.exists() && !target.renameTo(backup)) throw new IOException(Text.t("Не удалось сохранить предыдущую версию"));
            if (!incoming.renameTo(target)) {
                if (backup.exists() && !backup.renameTo(target)) throw new IOException(Text.t("Не удалось восстановить предыдущую версию"));
                throw new IOException(Text.t("Не удалось установить плагин"));
            }
            placed = true;
            fresh = new Info(target, metadata);
            fresh.enabled = previous == null || previous.enabled;
            options.edit().putBoolean(fresh.id, fresh.enabled).apply();
            synchronized (installed) { installed.put(fresh.id, fresh); publish(); }
            if (fresh.enabled) load(fresh);
            if (!fresh.error.isEmpty()) throw new IOException(Text.t("Не удалось запустить плагин: ") + fresh.error.split("\n", 2)[0]);
            if (backup.exists()) PluginPackage.delete(backup);
        } catch (Throwable error) {
            problem = error.toString();
            try {
                if (placed && target != null) {
                    if (fresh != null) stop(fresh);
                    PluginPackage.delete(target);
                    synchronized (installed) {
                        if (fresh != null) installed.remove(fresh.id);
                        if (previous != null) installed.put(previous.id, previous);
                        publish();
                    }
                    if (backup.exists() && !backup.renameTo(target)) throw new IOException(Text.t("Не удалось восстановить предыдущую версию"));
                }
                if (previous != null && previous.enabled && !previous.loaded) load(previous);
            } catch (Exception again) {
                problem += "; " + again;
            }
            Diary.note("plugin install: " + problem);
        } finally {
            try { if (incoming.exists()) PluginPackage.delete(incoming); }
            catch (Exception error) { Diary.note("plugin stage: " + error); }
        }
        final String result = problem;
        MAIN.post(() -> lambda$install$12(completion, result));
    }

    static /* synthetic */ void lambda$install$12(Completion completion, String str) {
        changed();
        completion.done(str);
    }

    public static void remove(final String str, final Completion completion) {
        IO.execute(new Runnable() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                PluginEngine.lambda$remove$15(str, completion);
            }
        });
    }

    static /* synthetic */ void lambda$remove$15(String str, final Completion completion) {
        String failure;
        try {
            Info info = info(str);
            if (info != null) {
                stop(info);
                PluginPackage.delete(PluginPackage.child(codeRoot, str));
                forget(str);
                Map<String, Info> map = installed;
                synchronized (map) {
                    map.remove(str);
                    publish();
                }
                options.edit().remove(str).apply();
            }
            failure = null;
        } catch (Exception e) {
            failure = e.toString();
        }
        final String exc = failure;
        MAIN.post(new Runnable() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda22
            @Override // java.lang.Runnable
            public final void run() {
                PluginEngine.lambda$remove$14(completion, exc);
            }
        });
    }

    static /* synthetic */ void lambda$remove$14(Completion completion, String str) {
        changed();
        completion.done(str);
    }

    public static void invoke(String str, Runnable runnable) {
        Info info = info(str);
        if (info == null || info.plugin == null) {
            return;
        }
        try {
            runnable.run();
        } catch (Throwable th) {
            fail(info, th);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void dispatch(Event event) {
        for (Info info : running) {
            TtcuzPlugin ttcuzPlugin = info.plugin;
            if (ttcuzPlugin != null) {
                try {
                    event.accept(ttcuzPlugin);
                } catch (Throwable th) {
                    fail(info, th);
                }
            }
        }
    }

    private static <T> T transform(Info[] infoArr, T t, Transform<T> transform) {
        if (infoArr.length != 0) {
            Boolean bool = Boolean.TRUE;
            ThreadLocal<Boolean> threadLocal = transforming;
            if (!bool.equals(threadLocal.get())) {
                threadLocal.set(Boolean.TRUE);
                try {
                    for (Info info : infoArr) {
                        TtcuzPlugin ttcuzPlugin = info.plugin;
                        if (ttcuzPlugin != null) {
                            try {
                                T apply = transform.apply(ttcuzPlugin, t);
                                if (apply != null) {
                                    t = apply;
                                }
                            } catch (Throwable th) {
                                fail(info, th);
                            }
                        }
                    }
                    return t;
                } finally {
                    transforming.set(Boolean.FALSE);
                }
            }
        }
        return t;
    }

    public static int colour(int i) {
        Info[] infoArr = colourHooks;
        if (infoArr.length != 0) {
            Boolean bool = Boolean.TRUE;
            ThreadLocal<Boolean> threadLocal = transforming;
            if (!bool.equals(threadLocal.get())) {
                threadLocal.set(Boolean.TRUE);
                try {
                    for (Info info : infoArr) {
                        TtcuzPlugin ttcuzPlugin = info.plugin;
                        if (ttcuzPlugin != null) {
                            try {
                                i = ttcuzPlugin.onColour(i);
                            } catch (Throwable th) {
                                fail(info, th);
                            }
                        }
                    }
                    return i;
                } finally {
                    transforming.set(Boolean.FALSE);
                }
            }
        }
        return i;
    }

    public static String region(final String str, String str2) {
        return regionHooks.length == 0 ? str2 : (String) transform(regionHooks, str2, new Transform() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda3
            @Override // cat.ttcuz.framework.PluginEngine.Transform
            public final Object apply(TtcuzPlugin ttcuzPlugin, Object obj) {
                String onRegion;
                onRegion = ttcuzPlugin.onRegion(str, (String) obj);
                return onRegion;
            }
        });
    }

    public static String nickname(final String str, String str2) {
        return nicknameHooks.length == 0 ? str2 : (String) transform(nicknameHooks, str2, new Transform() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda2
            @Override // cat.ttcuz.framework.PluginEngine.Transform
            public final Object apply(TtcuzPlugin ttcuzPlugin, Object obj) {
                String onNickname;
                onNickname = ttcuzPlugin.onNickname(str, (String) obj);
                return onNickname;
            }
        });
    }

    public static CharSequence text(final TextView textView, CharSequence charSequence) {
        return textHooks.length == 0 ? charSequence : (CharSequence) transform(textHooks, charSequence, new Transform() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda14
            @Override // cat.ttcuz.framework.PluginEngine.Transform
            public final Object apply(TtcuzPlugin ttcuzPlugin, Object obj) {
                CharSequence onText;
                onText = ttcuzPlugin.onText(textView, (CharSequence) obj);
                return onText;
            }
        });
    }

    public static List feed(List list) {
        return feedHooks.length == 0 ? list : (List) transform(feedHooks, list, new Transform() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda10
            @Override // cat.ttcuz.framework.PluginEngine.Transform
            public final Object apply(TtcuzPlugin ttcuzPlugin, Object obj) {
                List onFeed;
                onFeed = ttcuzPlugin.onFeed((List) obj);
                return onFeed;
            }
        });
    }

    public static boolean hasViewHooks() {
        return viewHooks.length != 0;
    }

    public static void view(View view) {
        if (hasViewHooks()) {
            Boolean bool = Boolean.TRUE;
            ThreadLocal<Boolean> threadLocal = transforming;
            if (bool.equals(threadLocal.get())) {
                return;
            }
            threadLocal.set(Boolean.TRUE);
            try {
                for (Info info : viewHooks) {
                    TtcuzPlugin ttcuzPlugin = info.plugin;
                    if (ttcuzPlugin != null) {
                        try {
                            ttcuzPlugin.onView(view);
                        } catch (Throwable th) {
                            fail(info, th);
                        }
                    }
                }
            } finally {
                transforming.set(Boolean.FALSE);
            }
        }
    }

    public static void created(final Activity activity) {
        dispatch(new Event() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda4
            @Override // cat.ttcuz.framework.PluginEngine.Event
            public final void accept(TtcuzPlugin ttcuzPlugin) {
                ttcuzPlugin.onActivityCreated(activity);
            }
        });
    }

    public static void resumed(final Activity activity) {
        // a store link from the browser (ttcuz://plugin/<id>) waits for the first TikTok screen
        PluginLinkActivity.resumed(activity);
        dispatch(new Event() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda1
            @Override // cat.ttcuz.framework.PluginEngine.Event
            public final void accept(TtcuzPlugin ttcuzPlugin) {
                ttcuzPlugin.onActivityResumed(activity);
            }
        });
        PluginKit.resumed(activity);
        // onScreen: once the screen has been laid out, so the plugin finds its views where they will be
        final View content = activity.findViewById(android.R.id.content);
        if (content instanceof android.view.ViewGroup && screenHooks.length > 0) content.post(() -> {
            if (activity.isFinishing()) return;
            for (final Info info : screenHooks) {
                final TtcuzPlugin plugin = info.plugin;
                if (plugin != null) invoke(info.id, () -> plugin.onScreen(activity, (android.view.ViewGroup) content));
            }
        });
    }

    private static boolean inModScreen;

    /** Whether any running plugin listens to TikTok's events (API 4, onTikTok). */
    public static boolean hasTikTokHooks() { return tiktokHooks.length > 0; }

    /** One of TikTok's events (API 4): handed to every plugin with onTikTok, on the main thread. */
    public static void tiktok(final String event, final java.util.Map<String, Object> data) {
        final Info[] hooks = tiktokHooks;
        if (hooks.length == 0) return;
        Runnable deliver = () -> {
            for (final Info info : hooks) {
                final TtcuzPlugin plugin = info.plugin;
                if (plugin != null) invoke(info.id, () -> plugin.onTikTok(event, data));
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) deliver.run(); else MAIN.post(deliver);
    }

    /** The mod's settings screen was drawn: plugins with onModScreen restyle it. Re-entry (a refresh from inside) is ignored. */
    public static void modScreen(final Activity activity, final android.view.ViewGroup root) {
        if (inModScreen || modScreenHooks.length == 0) return;
        inModScreen = true;
        try {
            for (final Info info : modScreenHooks) {
                final TtcuzPlugin plugin = info.plugin;
                if (plugin != null) invoke(info.id, () -> plugin.onModScreen(activity, root));
            }
        } finally { inModScreen = false; }
    }

    static boolean drawingModScreen() { return inModScreen; }

    /** A mod sheet is opening. */
    public static void modPanel(final String title, final android.view.ViewGroup content) {
        for (final Info info : modPanelHooks) {
            final TtcuzPlugin plugin = info.plugin;
            if (plugin != null) invoke(info.id, () -> plugin.onModPanel(title, content));
        }
    }

    /** The plugins that add rows to the mod's settings, each with its rows. */
    static java.util.List<Object[]> modSettings() {
        java.util.List<Object[]> out = new ArrayList<>();
        for (final Info info : modSettingsHooks) {
            final TtcuzPlugin plugin = info.plugin;
            if (plugin == null) continue;
            final cat.ttcuz.framework.plugin.PluginSettings page = new cat.ttcuz.framework.plugin.PluginSettings();
            invoke(info.id, () -> plugin.onModSettings(page));
            if (!page.rows().isEmpty()) out.add(new Object[]{info, page});
        }
        return out;
    }

    /** An Android permission a plugin asked for was answered: told to that plugin alone. */
    public static void permissionAnswered(String id, final String permission, final boolean granted) {
        final Info info = info(id);
        final TtcuzPlugin plugin = info == null ? null : info.plugin;
        if (plugin != null) invoke(id, () -> plugin.onPermission(permission, granted));
    }

    public static void paused(final Activity activity) {
        PluginKit.paused(activity);
        dispatch(new Event() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda26
            @Override // cat.ttcuz.framework.PluginEngine.Event
            public final void accept(TtcuzPlugin ttcuzPlugin) {
                ttcuzPlugin.onActivityPaused(activity);
            }
        });
    }

    public static void destroyed(final Activity activity) {
        dispatch(new Event() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda11
            @Override // cat.ttcuz.framework.PluginEngine.Event
            public final void accept(TtcuzPlugin ttcuzPlugin) {
                ttcuzPlugin.onActivityDestroyed(activity);
            }
        });
    }

    public static void accountChanged(final String str) {
        if (started) {
            SharedPreferences sharedPreferences = appearance;
            if (sharedPreferences != null) {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(globalChanges);
            }
            SharedPreferences prefs = AccountAppearance.prefs(app);
            appearance = prefs;
            prefs.registerOnSharedPreferenceChangeListener(globalChanges);
            dispatch(new Event() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda20
                @Override // cat.ttcuz.framework.PluginEngine.Event
                public final void accept(TtcuzPlugin ttcuzPlugin) {
                    PluginEngine.lambda$accountChanged$24(str, ttcuzPlugin);
                }
            });
        }
    }

    static /* synthetic */ void lambda$accountChanged$24(String str, TtcuzPlugin ttcuzPlugin) {
        ttcuzPlugin.ttcuz().accountChanged();
        ttcuzPlugin.onAccountChanged(str);
    }

    public static void preference(String str, final String str2, final String str3) {
        final Info info = info(str);
        if (info != null) {
            invoke(str, new Runnable() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda12
                @Override // java.lang.Runnable
                public final void run() {
                    info.plugin.onPreferenceChanged(str2, str3);
                }
            });
        }
    }

    public static void emit(final String str, final Object obj) {
        Objects.requireNonNull(str, "event");
        ThreadLocal<Set<String>> threadLocal = emitting;
        Set<String> set = threadLocal.get();
        if (set == null) {
            set = new HashSet<>();
            threadLocal.set(set);
        }
        if (set.add(str)) {
            try {
                dispatch(new Event() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda25
                    @Override // cat.ttcuz.framework.PluginEngine.Event
                    public final void accept(TtcuzPlugin ttcuzPlugin) {
                        ttcuzPlugin.onEvent(str, obj);
                    }
                });
            } finally {
                set.remove(str);
            }
        }
    }

    public static PluginSettings settings(String str, final Activity activity) {
        final Info info = info(str);
        final PluginSettings pluginSettings = new PluginSettings();
        final boolean[] zArr = {false};
        if (info != null && info.loaded) {
            invoke(str, new Runnable() { // from class: cat.ttcuz.framework.PluginEngine$$ExternalSyntheticLambda18
                @Override // java.lang.Runnable
                public final void run() {
                    PluginEngine.lambda$settings$27(zArr, info, activity, pluginSettings);
                }
            });
        }
        if (zArr[0]) {
            return null;
        }
        return pluginSettings;
    }

    static /* synthetic */ void lambda$settings$27(boolean[] zArr, Info info, Activity activity, PluginSettings pluginSettings) {
        boolean onOpenSettings = info.plugin.onOpenSettings(activity);
        zArr[0] = onOpenSettings;
        if (onOpenSettings) {
            return;
        }
        info.plugin.onSettings(pluginSettings);
    }

    public static SharedPreferences prefs(String str) {
        Info info = info(str);
        if (info == null || info.context == null) {
            return null;
        }
        return info.context.prefs();
    }

    /** Whether the plugin draws settings of its own: it overrides onSettings or onOpenSettings. Nothing is called. */
    public static boolean hasSettings(String id) {
        Info info = info(id);
        TtcuzPlugin plugin = info == null ? null : info.plugin;
        if (plugin == null) return false;
        if (plugin instanceof GuestPlugin) return ((GuestPlugin) plugin).hasSettings();
        try {
            Class<?> type = plugin.getClass();
            return type.getMethod("onSettings", PluginSettings.class).getDeclaringClass() != TtcuzPlugin.class
                    || type.getMethod("onOpenSettings", Activity.class).getDeclaringClass() != TtcuzPlugin.class;
        } catch (Throwable error) {
            return false;
        }
    }

    /** Packs an installed plugin back into a .ctt: its manifest, its dex files and its icon. */
    public static void pack(String id, java.io.OutputStream out) throws IOException {
        Info info = info(id);
        if (info == null) throw new IOException(Text.t("Плагин не найден"));
        File[] files = info.folder.listFiles();
        if (files == null) throw new IOException(Text.t("Плагин не найден"));
        java.util.Arrays.sort(files);
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(out)) {
            for (File file : files) {
                String name = file.getName();
                if (!file.isFile() || !(name.equals("manifest.json") || name.equals("icon.png") || name.matches("classes\\d*\\.dex"))) continue;
                zip.putNextEntry(new java.util.zip.ZipEntry(name));
                try (InputStream in = new java.io.FileInputStream(file)) {
                    byte[] buffer = new byte[65536];
                    for (int n; (n = in.read(buffer)) > 0; ) zip.write(buffer, 0, n);
                }
                zip.closeEntry();
            }
        }
    }
}
