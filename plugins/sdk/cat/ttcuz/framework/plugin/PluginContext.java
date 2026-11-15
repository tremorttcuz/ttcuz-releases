package cat.ttcuz.framework.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Compile-time signatures of what ttcuz hands a plugin. The app provides the real class at run time, so the
 * plugin builder leaves this one out of the plugin's dex; the bodies here are never run.
 *
 * Plugin API 4. What a plugin may use follows "permissions" in its manifest.json. A call it did not ask for throws
 * SecurityException naming the permission. ttcuz permissions: interface, text, feed, colours, region, events,
 * network, notifications, clipboard, storage, account, tiktok, effects, mod (API 3), watch, automation, player
 * (API 4); Android ones by their full name
 * (android.permission.CAMERA, RECORD_AUDIO, ACCESS_FINE_LOCATION, READ_CONTACTS, ...).
 */
public final class PluginContext implements AutoCloseable {
    /** A choice from menu(). */
    public interface Pick { void picked(int index); }
    /** The answer to request(). */
    public interface Result { void done(boolean granted); }
    /** Something a plugin exposes to the others (API 4). */
    public interface Callable { Object call(Object argument) throws Exception; }

    /** The plugin's id from its manifest. */
    public final String id = null;

    private PluginContext() { throw new UnsupportedOperationException("provided by ttcuz"); }

    // ---- permissions
    /** The plugin API from the manifest (1 to 4). */
    public int api() { throw new UnsupportedOperationException(); }
    /** The permissions from the manifest. */
    public List<String> permissions() { throw new UnsupportedOperationException(); }
    /** Whether the plugin may use this permission. */
    public boolean can(String permission) { throw new UnsupportedOperationException(); }

    // ---- basics
    /** The application context. */
    public Context app() { throw new UnsupportedOperationException(); }
    /** The activity on screen, or null. */
    public Activity screen() { throw new UnsupportedOperationException(); }
    /** The signed-in TikTok account id, or null. Needs "account". */
    public String accountId() { throw new UnsupportedOperationException(); }
    /** The accent colour the user chose. */
    public int accent() { throw new UnsupportedOperationException(); }
    /** The plugin's own settings, shared by every account. */
    public SharedPreferences prefs() { throw new UnsupportedOperationException(); }
    /** The plugin's settings for the account signed in now. */
    public SharedPreferences accountPrefs() { throw new UnsupportedOperationException(); }
    /** The unpacked .ctt (read only). */
    public File bundle() { throw new UnsupportedOperationException(); }
    /** A folder the plugin may write to. */
    public File data() { throw new UnsupportedOperationException(); }
    /** TikTok's class loader. Needs "tiktok". */
    public ClassLoader hostLoader() { throw new UnsupportedOperationException(); }
    /** A TikTok class by name. Needs "tiktok". */
    public Class<?> type(String name) throws ClassNotFoundException { throw new UnsupportedOperationException(); }
    /** A method of a TikTok class, made accessible. Needs "tiktok". */
    public Method method(String type, String name, Class<?>... parameters) throws ReflectiveOperationException { throw new UnsupportedOperationException(); }
    /** A field of a TikTok class, made accessible. Needs "tiktok". */
    public Field field(String type, String name) throws ReflectiveOperationException { throw new UnsupportedOperationException(); }
    /** A line in the mod's diary, marked with the plugin's id. */
    public void log(String message) { throw new UnsupportedOperationException(); }
    /** A short message on screen. */
    public void toast(String message) { throw new UnsupportedOperationException(); }
    /** Sends an event to every plugin's onEvent. */
    public void emit(String name, Object value) { throw new UnsupportedOperationException(); }
    /** Runs on the main thread. */
    public void ui(Runnable action) { throw new UnsupportedOperationException(); }
    /** Runs on the main thread after a delay in milliseconds; cancelled when the plugin stops. */
    public void later(Runnable action, long delay) { throw new UnsupportedOperationException(); }
    /** Runs on the plugin's background thread. */
    public void away(Runnable action) { throw new UnsupportedOperationException(); }

    // ---- interface
    /** px for dp on this phone. */
    public int dp(float value) { throw new UnsupportedOperationException(); }
    /** The colours of the mod's screens now: {page, card, text}. */
    public int[] palette() { throw new UnsupportedOperationException(); }
    /** Puts a view over the screen in the plugin's own layer (gravity: android.view.Gravity). Needs "interface". */
    public View overlay(Activity activity, View view, int gravity) { throw new UnsupportedOperationException(); }
    /** Puts a view over the screen, top left. Needs "interface". */
    public View overlay(Activity activity, View view) { throw new UnsupportedOperationException(); }
    /** Takes an overlay away. */
    public void removeOverlay(View view) { throw new UnsupportedOperationException(); }
    /** A view of the screen by its TikTok resource name. Needs "interface". */
    public View find(Activity activity, String idName) { throw new UnsupportedOperationException(); }
    /** Every view of this type under root. Needs "interface". */
    public <T extends View> List<T> views(View root, Class<T> type) { throw new UnsupportedOperationException(); }
    /** The mod's entrance animation. */
    public void animateIn(View view) { throw new UnsupportedOperationException(); }
    /** The mod's exit animation, then after (may be null). */
    public void animateOut(View view, Runnable after) { throw new UnsupportedOperationException(); }
    /** A dialog in the mod's style; cancel may be null. */
    public void dialog(String title, String message, String ok, Runnable onOk, String cancel) { throw new UnsupportedOperationException(); }
    /** A list to choose from, in the mod's style. */
    public void menu(String title, String[] items, Pick pick) { throw new UnsupportedOperationException(); }

    // ---- Android permissions
    /** Whether Android has granted it. */
    public boolean has(String androidPermission) { throw new UnsupportedOperationException(); }
    /** Asks the person for an Android permission listed in the manifest; the answer also reaches onPermission. */
    public void request(String androidPermission, Result result) { throw new UnsupportedOperationException(); }

    // ---- network, files, phone
    /** GET over https, from away(). Needs "network". */
    public String http(String url) throws java.io.IOException { throw new UnsupportedOperationException(); }
    /** Any method over https, from away(). Needs "network". */
    public String http(String method, String url, String body, String contentType) throws java.io.IOException { throw new UnsupportedOperationException(); }
    /** A notification. Needs "notifications". */
    public void notify(String title, String text) { throw new UnsupportedOperationException(); }
    /** Copies text. Needs "clipboard". */
    public void copy(String text) { throw new UnsupportedOperationException(); }
    /** What is copied now. Needs "clipboard". */
    public String paste() { throw new UnsupportedOperationException(); }
    /** Saves a file to Downloads/ttcuz/&lt;id&gt;/. Needs "storage". */
    public android.net.Uri saveDownload(String name, String mime, byte[] bytes) throws java.io.IOException { throw new UnsupportedOperationException(); }
    /** Opens a link. */
    public void open(String url) { throw new UnsupportedOperationException(); }
    /** Android's share sheet with this text. */
    public void share(String text) { throw new UnsupportedOperationException(); }
    /** A short vibration. Needs android.permission.VIBRATE. */
    public void vibrate(long millis) { throw new UnsupportedOperationException(); }

    // effects (API 3, "effects"): RenderEffect on Android 12+, nothing on older phones
    /** Called on every frame while it runs. */
    public interface Frame { void frame(long frameTimeNanos); }
    /** Whether this phone draws effects at all (Android 12+). */
    public boolean effectsSupported() { throw new UnsupportedOperationException(); }
    /** Whether shader() works here (Android 13+). */
    public boolean shadersSupported() { throw new UnsupportedOperationException(); }
    /** A Gaussian blur over the view, radius in px; 0 takes it off. Needs "effects". */
    public boolean blur(View view, float radius) { throw new UnsupportedOperationException(); }
    /** A blur stronger along one axis: blur(view, 30, 0) smears sideways. Needs "effects". */
    public boolean blur(View view, float radiusX, float radiusY) { throw new UnsupportedOperationException(); }
    /** Motion blur along the way the view moves on screen (strength 1 natural, up to 4); returns what stops it. Needs "effects". */
    public Runnable motionBlur(View view, float strength) { throw new UnsupportedOperationException(); }
    /** A 4x5 colour matrix (android.graphics.ColorMatrix layout) over the view. Needs "effects". */
    public boolean colourMatrix(View view, float[] matrix) { throw new UnsupportedOperationException(); }
    /** An AGSL shader over the view ("uniform shader content;"): the RuntimeShader, or null before Android 13. Needs "effects". */
    public Object shader(View view, String agsl) { throw new UnsupportedOperationException(); }
    /** Sets a float uniform of the view's shader and redraws. Needs "effects". */
    public boolean uniform(View view, String name, float... values) { throw new UnsupportedOperationException(); }
    /** Takes the effect off the view. Needs "effects". */
    public void clearEffect(View view) { throw new UnsupportedOperationException(); }
    /** A callback on every frame until the returned Runnable is run or the plugin stops. */
    public Runnable everyFrame(Frame frame) { throw new UnsupportedOperationException(); }

    // the mod itself (API 3, "mod"); accounts, tokens, keys and the admin's settings stay closed
    /** The mod's settings open to plugins, with their values. Needs "mod". */
    public java.util.Map<String, Object> modSettings() { throw new UnsupportedOperationException(); }
    /** One mod setting (Boolean, Integer, Long, Float or String), or null. Needs "mod". */
    public Object modSetting(String key) { throw new UnsupportedOperationException(); }
    /** Changes a mod setting; null brings the default back. Other plugins hear the event "mod.setting". Needs "mod". */
    public void setModSetting(String key, Object value) { throw new UnsupportedOperationException(); }
    /** Draws the mod's settings screen again, if it is open. Needs "mod". */
    public void refreshModScreen() { throw new UnsupportedOperationException(); }

    // ---- API 4: selectors and the view tree ("interface")
    /** Views under root matching "#id", "@text", "@=exact text", "~description" or ".Class"; "|" joins alternatives. */
    public List<View> query(View root, String selector) { throw new UnsupportedOperationException(); }
    /** The first match, or null. Needs "interface". */
    public View queryOne(View root, String selector) { throw new UnsupportedOperationException(); }
    /** The view tree under root as indented text: class, #id, @text, ~description, size. Needs "interface". */
    public String tree(View root) { throw new UnsupportedOperationException(); }
    /** Where a view is on screen: {left, top, right, bottom}. */
    public int[] bounds(View view) { throw new UnsupportedOperationException(); }
    /** Views matching the selector stay hidden on every screen until unhide() or the plugin stops. Needs "interface". */
    public void hide(String selector) { throw new UnsupportedOperationException(); }
    public void unhide(String selector) { throw new UnsupportedOperationException(); }
    /** Puts a view into TikTok's layout right after (or before) an existing one. Needs "interface". */
    public void inject(View anchor, View view, boolean after) { throw new UnsupportedOperationException(); }

    // ---- API 4: acting for the person ("automation")
    public boolean click(View view) { throw new UnsupportedOperationException(); }
    public boolean longClick(View view) { throw new UnsupportedOperationException(); }
    /** A finger tap at a point of the screen. */
    public void tap(Activity screen, float x, float y) { throw new UnsupportedOperationException(); }
    /** A drag over the given milliseconds; returns at once. */
    public void swipe(Activity screen, float x1, float y1, float x2, float y2, long millis) { throw new UnsupportedOperationException(); }
    /** Replaces a field's text and moves the cursor to the end. */
    public void type(android.widget.TextView field, CharSequence text) { throw new UnsupportedOperationException(); }

    // ---- API 4: the video in front ("player")
    /** The post on screen (TikTok's Aweme) or null. */
    public Object video() { throw new UnsupportedOperationException(); }
    public String videoId() { throw new UnsupportedOperationException(); }
    public boolean playing() { throw new UnsupportedOperationException(); }
    public boolean pause() { throw new UnsupportedOperationException(); }
    public boolean play() { throw new UnsupportedOperationException(); }
    /** Playback speed, 0.5 to 3. */
    public boolean speed(float value) { throw new UnsupportedOperationException(); }
    /** Saves a post the way the mod's download does. Needs "player" and "storage". */
    public void saveVideo(Object post) { throw new UnsupportedOperationException(); }

    // ---- API 4: plugins together, repeating work
    /** Lets other plugins call this one: ttcuz().call(thisId, name, argument). null removes it. */
    public void expose(String name, Callable callable) { throw new UnsupportedOperationException(); }
    /** Calls what another running plugin exposed; null when it is not there. Needs "events". */
    public Object call(String plugin, String name, Object argument) throws Exception { throw new UnsupportedOperationException(); }
    /** Runs work on the main thread every millis (at least 250) while TikTok is on screen; the Runnable stops it. */
    public Runnable every(Runnable work, long millis) { throw new UnsupportedOperationException(); }

    public void accountChanged() { throw new UnsupportedOperationException(); }
    @Override public void close() { throw new UnsupportedOperationException(); }
}
