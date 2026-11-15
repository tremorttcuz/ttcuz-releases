package cat.ttcuz.framework.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import cat.ttcuz.framework.Accent;
import cat.ttcuz.framework.Account;
import cat.ttcuz.framework.Diary;
import cat.ttcuz.framework.PluginBridge;
import cat.ttcuz.framework.PluginEffects;
import cat.ttcuz.framework.PluginEngine;
import cat.ttcuz.framework.PluginPermissions;
import cat.ttcuz.framework.Screen;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Everything ttcuz hands a plugin.
 *
 * What a plugin may use follows the "permissions" list of its manifest.json (see PluginPermissions): a call it did
 * not ask for throws SecurityException naming the permission. Plugins of API 1 keep, without a list, everything
 * API 1 had (TikTok's classes and the account id); everything added in API 2 has to be asked for by every plugin.
 */
public final class PluginContext implements AutoCloseable {
    /** A choice from menu(). */
    public interface Pick { void picked(int index); }
    /** The answer to request(). */
    public interface Result { void done(boolean granted); }
    /** Something a plugin exposes to the others (API 4): ttcuz().expose(name, argument -> answer). */
    public interface Callable { Object call(Object argument) throws Exception; }

    private SharedPreferences account;
    private final SharedPreferences.OnSharedPreferenceChangeListener accountChanges;
    private final Context app;
    private final File bundle;
    private volatile boolean closed;
    private final File data;
    public final String id;
    private final int api;
    private final List<String> permissions;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final SharedPreferences.OnSharedPreferenceChangeListener ownChanges;
    private final SharedPreferences prefs;
    private final ExecutorService worker;
    private final List<java.lang.ref.WeakReference<View>> overlays = new ArrayList<>();

    /** API 1, no permission list: what plugins built before API 2 get. */
    public PluginContext(Context context, String id, File bundle, File data) {
        this(context, id, bundle, data, 1, Collections.<String>emptyList());
    }

    public PluginContext(Context context, final String id, File bundle, File data, int api, List<String> permissions) {
        this.ownChanges = (shared, key) -> PluginEngine.preference(id, "plugin", key);
        this.accountChanges = (shared, key) -> PluginEngine.preference(id, "plugin-account", key);
        this.app = context.getApplicationContext();
        this.id = id;
        this.bundle = bundle;
        this.data = data;
        this.api = api;
        this.permissions = Collections.unmodifiableList(new ArrayList<>(permissions));
        if (!data.isDirectory() && !data.mkdirs()) throw new IllegalStateException("Cannot create plugin data directory");
        this.worker = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "ttcuz-plugin-" + id));
        this.prefs = context.getSharedPreferences("ttcuz_plugin_" + id, 0);
        this.prefs.registerOnSharedPreferenceChangeListener(ownChanges);
        accountChanged();
    }

    // ------------------------------------------------------------ permissions

    /** The plugin API this plugin was written for (manifest "api"). */
    public int api() { return api; }

    /** The permissions from manifest.json. */
    public List<String> permissions() { return permissions; }

    /** Whether the plugin may use this: it asked for it, or (API 1) it is something API 1 always had. */
    public boolean can(String permission) {
        if (permissions.contains(permission)) return true;
        return api < 2 && (PluginPermissions.TIKTOK.equals(permission) || PluginPermissions.ACCOUNT.equals(permission));
    }

    private void require(String permission) {
        if (!can(permission))
            throw new SecurityException("plugin " + id + " needs \"" + permission + "\" in the permissions of manifest.json");
    }

    // ------------------------------------------------------------ what API 1 had

    public Context app() { return this.app; }

    public Activity screen() { if (closed) return null; return Screen.now(); }

    /** The signed-in account id. Needs "account" (API 2). */
    public String accountId() { require(PluginPermissions.ACCOUNT); return Account.liveId(); }

    public int accent() { return Accent.colour(); }

    public SharedPreferences prefs() { return this.prefs; }

    public SharedPreferences accountPrefs() { return this.account; }

    public File bundle() { return this.bundle; }

    public File data() { return this.data; }

    /** TikTok's class loader. Needs "tiktok" (API 2). */
    public ClassLoader hostLoader() { require(PluginPermissions.TIKTOK); return this.app.getClassLoader(); }

    public Class<?> type(String name) throws ClassNotFoundException { return Class.forName(name, true, hostLoader()); }

    public Method method(String type, String name, Class<?>... parameters) throws ReflectiveOperationException {
        for (Class<?> c = type(type); c != null; c = c.getSuperclass()) {
            try { Method m = c.getDeclaredMethod(name, parameters); m.setAccessible(true); return m; }
            catch (NoSuchMethodException ignored) { }
        }
        throw new NoSuchMethodException(type + "." + name);
    }

    public Field field(String type, String name) throws ReflectiveOperationException {
        for (Class<?> c = type(type); c != null; c = c.getSuperclass()) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(type + "." + name);
    }

    public void log(String message) { Diary.note("plugin " + this.id + ": " + message); }

    public void toast(String message) { if (closed) return; Screen.say(message); }

    /** Sends an event to every plugin's onEvent. */
    public void emit(String name, Object value) { if (closed) return; PluginEngine.emit(name, value); }

    public void ui(Runnable action) { later(action, 0L); }

    public void later(final Runnable action, long delay) {
        if (closed) return;
        main.postDelayed(() -> { if (!closed) PluginEngine.invoke(id, action); }, Math.max(0L, delay));
    }

    public void away(final Runnable action) {
        if (closed) return;
        try { worker.execute(() -> { if (!closed) PluginEngine.invoke(id, action); }); }
        catch (RejectedExecutionException ignored) { }
    }

    // ------------------------------------------------------------ interface (needs "interface")

    /** px for dp on this phone. */
    public int dp(float value) { return Math.round(value * app.getResources().getDisplayMetrics().density); }

    /** The colours of the mod's own screens now: {page, card, text}. */
    public int[] palette() { return PluginBridge.palette(app); }

    /**
     * Puts a view over the screen, in the plugin's own layer: touches go to it and pass through everywhere else.
     * gravity is android.view.Gravity (e.g. BOTTOM | END); the view is removed when the plugin stops.
     */
    public View overlay(Activity activity, View view, int gravity) { if (closed) return null;
        require(PluginPermissions.INTERFACE);
        FrameLayout layer = PluginBridge.layer(activity, id);
        FrameLayout.LayoutParams at = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, gravity);
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams own = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            at.width = own.width; at.height = own.height; at.setMargins(own.leftMargin, own.topMargin, own.rightMargin, own.bottomMargin);
        }
        if (view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view);
        layer.addView(view, at);
        synchronized (overlays) { overlays.add(new java.lang.ref.WeakReference<>(view)); }
        return view;
    }

    public View overlay(Activity activity, View view) { if (closed) return null; return overlay(activity, view, android.view.Gravity.TOP | android.view.Gravity.START); }

    public void removeOverlay(View view) {
        if (view != null && view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view);
    }

    /** A view of the screen by its resource name in TikTok ("title", or "com.zhiliaoapp.musically:id/title"). */
    public View find(Activity activity, String idName) {
        require(PluginPermissions.INTERFACE);
        if (activity == null || idName == null) return null;
        String name = idName.contains(":id/") ? idName.substring(idName.indexOf(":id/") + 4) : idName;
        int resource = activity.getResources().getIdentifier(name, "id", activity.getPackageName());
        return resource == 0 ? null : activity.findViewById(resource);
    }

    /** Every view of this type under root, in drawing order. */
    public <T extends View> List<T> views(View root, Class<T> type) {
        require(PluginPermissions.INTERFACE);
        List<T> out = new ArrayList<>();
        collect(root, type, out, 0);
        return out;
    }

    private static <T extends View> void collect(View view, Class<T> type, List<T> out, int depth) {
        if (view == null || depth > 64 || out.size() > 2000) return;
        if (type.isInstance(view)) out.add(type.cast(view));
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) collect(group.getChildAt(i), type, out, depth + 1);
        }
    }

    /** The mod's entrance: fade, rise and settle. */
    public void animateIn(View view) { if (closed) return; PluginBridge.animateIn(view); }

    /** The mod's exit; then runs after (may be null). */
    public void animateOut(View view, Runnable after) { if (closed) return; PluginBridge.animateOut(view, after); }

    /** A dialog in the mod's style. cancel may be null for one button. */
    public void dialog(String title, String message, String ok, Runnable onOk, String cancel) { if (closed) return;
        final Activity activity = Screen.now();
        main.post(() -> PluginBridge.dialog(activity, title, message, ok, onOk == null ? null : () -> PluginEngine.invoke(id, onOk), cancel));
    }

    /** A list to choose from, in the mod's style. */
    public void menu(String title, String[] items, Pick pick) { if (closed) return;
        final Activity activity = Screen.now();
        main.post(() -> PluginBridge.menu(activity, title, items, index -> PluginEngine.invoke(id, () -> pick.picked(index))));
    }

    // ------------------------------------------------------------ Android permissions

    /** Whether Android has granted it already. */
    public boolean has(String androidPermission) {
        return app.checkSelfPermission(androidPermission) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Asks the person for an Android permission the plugin lists in manifest.json (android.permission.CAMERA...).
     * The answer comes to result and to the plugin's onPermission, on the main thread.
     */
    public void request(final String androidPermission, final Result result) { if (closed) return;
        require(androidPermission);
        if (!androidPermission.startsWith("android.permission.")) throw new IllegalArgumentException("not an Android permission: " + androidPermission);
        PluginBridge.request(app, androidPermission, granted -> {
            if (closed) return;
            if (result != null) PluginEngine.invoke(id, () -> result.done(granted));
            PluginEngine.permissionAnswered(id, androidPermission, granted);
        });
    }

    // ------------------------------------------------------------ network (needs "network")

    /** GET over https, on a background thread (away()). Up to 2 MB of text. */
    public String http(String url) throws java.io.IOException { return http("GET", url, null, null); }

    /** Any method over https, on a background thread. body and contentType may be null. */
    public String http(String method, String url, String body, String contentType) throws java.io.IOException {
        require(PluginPermissions.NETWORK);
        if (Looper.myLooper() == Looper.getMainLooper()) throw new IllegalStateException("http() on the main thread: use away()");
        if (url == null || !url.startsWith("https://")) throw new java.io.IOException("only https:// addresses");
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        try {
            connection.setRequestMethod(method == null ? "GET" : method);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", "ttcuz-plugin/" + id);
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", contentType == null ? "application/json; charset=utf-8" : contentType);
                try (java.io.OutputStream out = connection.getOutputStream()) { out.write(body.getBytes("UTF-8")); }
            }
            int status = connection.getResponseCode();
            java.io.InputStream in = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (in == null) return "";
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int n; (n = in.read(buffer)) > 0; ) {
                bytes.write(buffer, 0, n);
                if (bytes.size() > 2 * 1048576) throw new java.io.IOException("answer larger than 2 MB");
            }
            in.close();
            if (status >= 400) throw new java.io.IOException("HTTP " + status + ": " + bytes.toString("UTF-8"));
            return bytes.toString("UTF-8");
        } finally {
            connection.disconnect();
        }
    }

    // ------------------------------------------------------------ the rest of the phone

    /** A notification with the plugin's name under it. Needs "notifications". */
    public void notify(String title, String text) { if (closed) return;
        require(PluginPermissions.NOTIFICATIONS);
        PluginBridge.notify(app, id, id, title, text);
    }

    /** Copies text. Needs "clipboard". */
    public void copy(String text) { if (closed) return;
        require(PluginPermissions.CLIPBOARD);
        main.post(() -> {
            android.content.ClipboardManager clip = (android.content.ClipboardManager) app.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clip != null) clip.setPrimaryClip(android.content.ClipData.newPlainText(id, text));
        });
    }

    /** What is copied now (Android answers only while TikTok is on screen). Needs "clipboard". */
    public String paste() {
        require(PluginPermissions.CLIPBOARD);
        android.content.ClipboardManager clip = (android.content.ClipboardManager) app.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clip == null || !clip.hasPrimaryClip() || clip.getPrimaryClip().getItemCount() == 0) return "";
        CharSequence text = clip.getPrimaryClip().getItemAt(0).coerceToText(app);
        return text == null ? "" : text.toString();
    }

    /** Saves a file in Downloads/ttcuz/<plugin id>/. Needs "storage". Returns where it went. */
    public android.net.Uri saveDownload(String name, String mime, byte[] bytes) throws java.io.IOException {
        require(PluginPermissions.STORAGE);
        String safe = name == null ? "file" : name.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, safe);
            values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mime == null ? "application/octet-stream" : mime);
            values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/ttcuz/" + id);
            android.net.Uri uri = app.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new java.io.IOException("Downloads refused the file");
            try (java.io.OutputStream out = app.getContentResolver().openOutputStream(uri)) { out.write(bytes); }
            return uri;
        }
        File folder = new File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "ttcuz/" + id);
        if (!folder.isDirectory() && !folder.mkdirs()) throw new java.io.IOException("no access to Downloads");
        File file = new File(folder, safe);
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) { out.write(bytes); }
        return android.net.Uri.fromFile(file);
    }

    /** Opens a link in the browser or the app that handles it. */
    public void open(String url) { if (closed) return;
        try { app.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (Throwable error) { log("open: " + error); }
    }

    /** Android's share sheet with this text. */
    public void share(String text) { if (closed) return;
        android.content.Intent send = new android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text);
        try { app.startActivity(android.content.Intent.createChooser(send, null).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (Throwable error) { log("share: " + error); }
    }

    /** A short vibration. Needs android.permission.VIBRATE in the list. */
    public void vibrate(long millis) { if (closed) return;
        require("android.permission.VIBRATE");
        android.os.Vibrator vibrator = (android.os.Vibrator) app.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) return;
        long length = Math.max(1, Math.min(2000, millis));
        if (android.os.Build.VERSION.SDK_INT >= 26) vibrator.vibrate(android.os.VibrationEffect.createOneShot(length, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
        else vibrator.vibrate(length);
    }

    // ------------------------------------------------------------ effects (API 3, "effects")

    /** Called on every frame while it runs, with the frame time Choreographer gives. */
    public interface Frame { void frame(long frameTimeNanos); }

    /** Whether this phone draws effects at all (Android 12+); on older ones the effect calls do nothing. */
    public boolean effectsSupported() { return PluginEffects.supported(); }
    /** Whether shader() works here (Android 13+). */
    public boolean shadersSupported() { return PluginEffects.shaders(); }

    /** A Gaussian blur over the view (radius in px, 0 takes it off). */
    public boolean blur(View view, float radius) { if (closed) return false; return blur(view, radius, radius); }

    /** A blur stronger along one axis: blur(view, 30, 0) smears sideways. */
    public boolean blur(View view, float radiusX, float radiusY) { if (closed) return false;
        require(PluginPermissions.EFFECTS);
        return PluginEffects.blur(id, view, radiusX, radiusY);
    }

    /**
     * Motion blur: the view is blurred along the way it moves on screen, as strongly as it moved since the last
     * frame (strength 1 is natural, up to 4). Costs nothing while the view stands still. Returns what stops it.
     */
    public Runnable motionBlur(View view, float strength) { if (closed) return () -> {};
        require(PluginPermissions.EFFECTS);
        return PluginEffects.motionBlur(id, view, strength);
    }

    /** A 4×5 colour matrix over the view (android.graphics.ColorMatrix layout): greyscale, sepia, contrast... */
    public boolean colourMatrix(View view, float[] matrix) { if (closed) return false;
        require(PluginPermissions.EFFECTS);
        return PluginEffects.matrix(id, view, matrix);
    }

    /**
     * An AGSL shader over the view, which it sees as "uniform shader content". Returns the android.graphics.RuntimeShader
     * (Android 13+), or null where shaders are not supported. Change its uniforms with uniform().
     */
    public Object shader(View view, String agsl) { if (closed) return null;
        require(PluginPermissions.EFFECTS);
        return PluginEffects.shader(id, view, agsl);
    }

    /** Sets a float uniform of the view's shader and redraws it with the new value. */
    public boolean uniform(View view, String name, float... values) { if (closed) return false;
        require(PluginPermissions.EFFECTS);
        return PluginEffects.uniform(view, name, values);
    }

    /** Takes the effect off the view. */
    public void clearEffect(View view) { require(PluginPermissions.EFFECTS); PluginEffects.clear(view); }

    /** Calls back on every frame until the returned Runnable is run, or the plugin stops. Keep it short. */
    public Runnable everyFrame(final Frame frame) { if (closed) return () -> {}; return PluginEffects.everyFrame(id, frame::frame); }

    // ------------------------------------------------------------ the mod itself (API 3, "mod")

    /** Settings that stay the mod's own: accounts, keys, tokens, the admin's things. */
    private static boolean closedSetting(String key) {
        return key == null || key.isEmpty() || key.length() > 120
                || key.toLowerCase(java.util.Locale.ROOT).matches(".*(token|secret|pin|pass|sec_id|session|cookie|admin|auth|account|uid|profile_preview|cloud|integrity|sign|backup|plugin).*");
    }

    /** The mod's settings a plugin may read and change, with their values. */
    public java.util.Map<String, Object> modSettings() {
        require(PluginPermissions.MOD);
        java.util.Map<String, Object> out = new java.util.TreeMap<>();
        for (java.util.Map.Entry<String, ?> e : app.getSharedPreferences(cat.ttcuz.framework.Ttcuz.PREFS, 0).getAll().entrySet())
            if (!closedSetting(e.getKey()) && !(e.getValue() instanceof java.util.Set)) out.put(e.getKey(), e.getValue());
        return out;
    }

    /** One mod setting (Boolean, Integer, Long, Float or String), or null. */
    public Object modSetting(String key) {
        require(PluginPermissions.MOD);
        if (closedSetting(key)) throw new SecurityException("plugin " + id + ": the mod setting \"" + key + "\" is not open to plugins");
        Object value = app.getSharedPreferences(cat.ttcuz.framework.Ttcuz.PREFS, 0).getAll().get(key);
        return value instanceof java.util.Set ? null : value;
    }

    /**
     * Changes a mod setting; null removes it, so the mod's default comes back. Most settings apply at once, some
     * after the screen is opened again or TikTok restarts. Other plugins hear it as the event "mod.setting".
     */
    public void setModSetting(String key, Object value) { if (closed) return;
        require(PluginPermissions.MOD);
        if (closedSetting(key)) throw new SecurityException("plugin " + id + ": the mod setting \"" + key + "\" is not open to plugins");
        SharedPreferences.Editor edit = app.getSharedPreferences(cat.ttcuz.framework.Ttcuz.PREFS, 0).edit();
        if (value == null) edit.remove(key);
        else if (value instanceof Boolean) edit.putBoolean(key, (Boolean) value);
        else if (value instanceof Integer) edit.putInt(key, (Integer) value);
        else if (value instanceof Long) edit.putLong(key, (Long) value);
        else if (value instanceof Float) edit.putFloat(key, (Float) value);
        else if (value instanceof String) edit.putString(key, (String) value);
        else throw new IllegalArgumentException("a mod setting is a Boolean, Integer, Long, Float or String");
        edit.apply();
        log("mod setting " + key + " = " + value);
        PluginEngine.emit("mod.setting", key);
    }

    /** Draws the mod's settings screen again, if it is open (after setModSetting, or to call onModScreen again). */
    public void refreshModScreen() { if (closed) return; require(PluginPermissions.MOD); PluginBridge.refreshModScreen(); }

    // ------------------------------------------------------------ API 4: selectors and the view tree ("interface")

    /**
     * Views under root that match a selector: "#name" resource id, "@text" text containing (any case), "@=text"
     * exactly that text, "~text" content description containing, ".Name" class; join several with "|".
     */
    public List<View> query(View root, String selector) { require(PluginPermissions.INTERFACE); return cat.ttcuz.framework.PluginKit.query(root, selector); }

    /** The first match, or null. */
    public View queryOne(View root, String selector) { List<View> found = query(root, selector); return found.isEmpty() ? null : found.get(0); }

    /** The view tree under root as indented text (class, #id, @text, ~description, size): to find selectors. */
    public String tree(View root) { require(PluginPermissions.INTERFACE); return cat.ttcuz.framework.PluginKit.tree(root, 40); }

    /** Where a view is on the screen: {left, top, right, bottom}. */
    public int[] bounds(View view) { return cat.ttcuz.framework.PluginKit.bounds(view); }

    /** Views matching the selector stay hidden on every screen until unhide() or until the plugin stops. */
    public void hide(String selector) { if (closed) return; require(PluginPermissions.INTERFACE); cat.ttcuz.framework.PluginKit.hide(id, selector); }

    public void unhide(String selector) { require(PluginPermissions.INTERFACE); cat.ttcuz.framework.PluginKit.unhide(id, selector); }

    /** Puts a view of the plugin's into TikTok's layout, right after (or before) an existing view, in its parent. */
    public void inject(View anchor, View view, boolean after) { if (closed) return;
        require(PluginPermissions.INTERFACE);
        if (!(anchor.getParent() instanceof ViewGroup)) throw new IllegalArgumentException("the anchor has no parent");
        ViewGroup parent = (ViewGroup) anchor.getParent();
        int at = parent.indexOfChild(anchor) + (after ? 1 : 0);
        if (view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view);
        parent.addView(view, at);
        synchronized (overlays) { overlays.add(new java.lang.ref.WeakReference<>(view)); }
    }

    // ------------------------------------------------------------ API 4: acting for the person ("automation")

    /** A tap on a view, as TikTok's own click. */
    public boolean click(View view) { if (closed) return false; require(PluginPermissions.AUTOMATION); return view.performClick(); }

    public boolean longClick(View view) { if (closed) return false; require(PluginPermissions.AUTOMATION); return view.performLongClick(); }

    /** A tap at a point of the screen, as a finger would make it. */
    public void tap(Activity screen, float x, float y) { if (closed) return; require(PluginPermissions.AUTOMATION); cat.ttcuz.framework.PluginKit.tap(screen, x, y); }

    /** A drag from one point to another over the given milliseconds; returns at once. */
    public void swipe(Activity screen, float x1, float y1, float x2, float y2, long millis) { if (closed) return;
        require(PluginPermissions.AUTOMATION);
        cat.ttcuz.framework.PluginKit.swipe(screen, x1, y1, x2, y2, millis);
    }

    /** Types into a text field: its text is replaced and the cursor goes to the end. */
    public void type(android.widget.TextView field, CharSequence text) { if (closed) return;
        require(PluginPermissions.AUTOMATION);
        field.setText(text);
        if (field instanceof android.widget.EditText) ((android.widget.EditText) field).setSelection(field.getText().length());
    }

    // ------------------------------------------------------------ API 4: the video in front ("player")

    /** The post on screen (TikTok's Aweme; read it with reflection, "tiktok"), or null. */
    public Object video() { require(PluginPermissions.PLAYER); return cat.ttcuz.framework.PluginKit.video(); }

    /** The id of the video on screen, or null. */
    public String videoId() { require(PluginPermissions.PLAYER); return cat.ttcuz.framework.PluginKit.videoId(); }

    public boolean playing() { require(PluginPermissions.PLAYER); return cat.ttcuz.framework.PluginKit.playing(); }

    public boolean pause() { if (closed) return false; require(PluginPermissions.PLAYER); return cat.ttcuz.framework.PluginKit.pause(); }

    public boolean play() { if (closed) return false; require(PluginPermissions.PLAYER); return cat.ttcuz.framework.PluginKit.play(); }

    /** Playback speed, 0.5 to 3. */
    public boolean speed(float value) { if (closed) return false; require(PluginPermissions.PLAYER); return cat.ttcuz.framework.PluginKit.speed(value); }

    /** Saves a post to the phone the way the mod's download does. Needs "player" and "storage". */
    public void saveVideo(Object post) { if (closed) return;
        require(PluginPermissions.PLAYER);
        require(PluginPermissions.STORAGE);
        cat.ttcuz.framework.PluginKit.save(post);
    }

    // ------------------------------------------------------------ API 4: plugins together, repeating work

    /** Something another plugin may call by name with ttcuz().call(thisPluginId, name, argument). null removes it. */
    public void expose(String name, Callable callable) { if (closed) return; cat.ttcuz.framework.PluginKit.expose(id, name, callable); }

    /** Calls what another running plugin exposed; null when it is not there or not running. Needs "events". */
    public Object call(String plugin, String name, Object argument) throws Exception {
        require(PluginPermissions.EVENTS);
        return cat.ttcuz.framework.PluginKit.call(plugin, name, argument);
    }

    /**
     * Runs work on the main thread every `millis` (at least 250) until the returned Runnable is run or the plugin
     * stops. Skipped while TikTok is not on screen.
     */
    public Runnable every(final Runnable work, final long millis) { if (closed) return () -> {};
        final long gap = Math.max(250L, millis);
        final boolean[] stopped = {false};
        final Runnable[] tick = new Runnable[1];
        tick[0] = () -> {
            if (stopped[0] || closed) return;
            if (screen() != null) PluginEngine.invoke(id, work);
            main.postDelayed(tick[0], gap);
        };
        main.postDelayed(tick[0], gap);
        return () -> { stopped[0] = true; main.removeCallbacks(tick[0]); };
    }

    // ------------------------------------------------------------ lifecycle

    public void accountChanged() {
        if (account != null) account.unregisterOnSharedPreferenceChangeListener(accountChanges);
        String who = Account.liveId();
        if (who == null || who.isEmpty()) who = "guest";
        account = app.getSharedPreferences("ttcuz_plugin_" + id + "_" + who, 0);
        account.registerOnSharedPreferenceChangeListener(accountChanges);
    }

    @Override
    public void close() {
        closed = true;
        main.removeCallbacksAndMessages(null);
        worker.shutdownNow();
        prefs.unregisterOnSharedPreferenceChangeListener(ownChanges);
        if (account != null) account.unregisterOnSharedPreferenceChangeListener(accountChanges);
        // whatever the plugin put on screen leaves with it
        final List<View> left = new ArrayList<>();
        synchronized (overlays) { for (java.lang.ref.WeakReference<View> ref : overlays) { View v = ref.get(); if (v != null) left.add(v); } overlays.clear(); }
        new Handler(Looper.getMainLooper()).post(() -> { for (View v : left) removeOverlay(v); PluginBridge.removeLayers(id); });
        PluginEffects.stop(id);
        cat.ttcuz.framework.PluginKit.forget(id);
    }
}
