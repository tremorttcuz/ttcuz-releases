package cat.ttcuz.framework.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import cat.ttcuz.framework.PluginEngine;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Host access plus tasks and preference listeners owned by one plugin instance. */
public final class PluginContext implements AutoCloseable {

    public final String id;
    private final Context app;
    private final File bundle, data;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker;
    private final SharedPreferences prefs;
    private SharedPreferences account;
    private volatile boolean closed;
    private final SharedPreferences.OnSharedPreferenceChangeListener ownChanges = (p, k) ->
        PluginEngine.preference(id(), "plugin", k);
    private final SharedPreferences.OnSharedPreferenceChangeListener accountChanges = (p, k) ->
        PluginEngine.preference(id(), "plugin-account", k);

    public PluginContext(Context app, String id, File bundle, File data) {
        this.app = app.getApplicationContext();
        this.id = id;
        this.bundle = bundle;
        this.data = data;
        if (!data.isDirectory() && !data.mkdirs()) throw new IllegalStateException(
            "Cannot create plugin data directory"
        );
        worker = Executors.newSingleThreadExecutor(r -> new Thread(r, "ttcuz-plugin-" + id));
        prefs = app.getSharedPreferences("ttcuz_plugin_" + id, Context.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(ownChanges);
        accountChanged();
    }

    private String id() {
        return id;
    }

    public Context app() {
        return app;
    }

    public Activity screen() {
        return cat.ttcuz.framework.Screen.now();
    }

    public String accountId() {
        return cat.ttcuz.framework.Account.liveId();
    }

    public int accent() {
        return cat.ttcuz.framework.Accent.colour();
    }

    public SharedPreferences prefs() {
        return prefs;
    }

    public SharedPreferences accountPrefs() {
        return account;
    }

    public File bundle() {
        return bundle;
    }

    public File data() {
        return data;
    }

    public ClassLoader hostLoader() {
        return app.getClassLoader();
    }

    public Class<?> type(String name) throws ClassNotFoundException {
        return Class.forName(name, true, hostLoader());
    }

    public Method method(String owner, String name, Class<?>... parameters)
        throws ReflectiveOperationException {
        Class<?> type = type(owner);
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod(name, parameters);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException absent) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchMethodException(owner + "." + name);
    }

    public Field field(String owner, String name) throws ReflectiveOperationException {
        Class<?> type = type(owner);
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException absent) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(owner + "." + name);
    }

    public void log(String value) {
        cat.ttcuz.framework.Diary.note("plugin " + id + ": " + value);
    }

    public void toast(String value) {
        cat.ttcuz.framework.Screen.say(value);
    }

    public void emit(String event, Object payload) {
        PluginEngine.emit(event, payload);
    }

    public void ui(Runnable task) {
        later(task, 0);
    }

    public void later(Runnable task, long delayMillis) {
        if (!closed) main.postDelayed(
            () -> {
                if (!closed) PluginEngine.invoke(id, task);
            },
            Math.max(0, delayMillis)
        );
    }

    public void away(Runnable task) {
        if (!closed) try {
            worker.execute(() -> {
                if (!closed) PluginEngine.invoke(id, task);
            });
        } catch (java.util.concurrent.RejectedExecutionException ignored) {}
    }

    public void accountChanged() {
        if (account != null) account.unregisterOnSharedPreferenceChangeListener(accountChanges);
        String uid = accountId();
        account = app.getSharedPreferences(
            "ttcuz_plugin_" + id + "_" + (uid == null || uid.isEmpty() ? "guest" : uid),
            0
        );
        account.registerOnSharedPreferenceChangeListener(accountChanges);
    }

    @Override
    public void close() {
        closed = true;
        main.removeCallbacksAndMessages(null);
        worker.shutdownNow();
        prefs.unregisterOnSharedPreferenceChangeListener(ownChanges);
        if (account != null) account.unregisterOnSharedPreferenceChangeListener(accountChanges);
    }
}
