package cat.narezany.margyt.plugin;

import android.app.Activity;
import android.content.Context;

/**
 * Compatibility API. Plugins packaged for another mod's plugin format (a ZIP with manifest.json, classes.dex and
 * icon.png, the same layout as a .ctt) were compiled against a base class of exactly this name, and Android loads a
 * class only under the name its dex asks for -- so the name is theirs; everything here is ttcuz's own. ttcuz runs such
 * a plugin through cat.ttcuz.framework.GuestPlugin, which turns these hooks into the ttcuz plugin hooks.
 */
public abstract class MargyPlugin {
    /** The newest version of this API that ttcuz answers. */
    public static final int API = 5;

    private PluginContext context;

    public final void attach(PluginContext context) {
        this.context = context;
    }

    public final PluginContext margyt() {
        return context;
    }

    public void onStart(Context context) {}

    public void onActivityCreated(Activity activity) {}

    public void onActivityResumed(Activity activity) {}

    public void onActivityPaused(Activity activity) {}

    public boolean onOpenSettings(Activity activity) { return false; }

    public void onAccountChanged(String uid) {}

    public void onSettingsChanged(String key) {}

    public int onColour(int colour) { return colour; }

    public String onRegion(String key, String value) { return value; }

    public void onScreen(Activity activity, String name) {}

    public java.util.List onFeed(java.util.List posts) { return posts; }

    public String onName(String uid, String name) { return name; }

    public CharSequence onText(CharSequence text) { return text; }

    public void onStop() {}
}
