package cat.ttcuz.framework.plugin;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;
import java.util.List;

/** ttcuz native plugin API. Code executes in the host process without a capability sandbox. */
public abstract class TtcuzPlugin {

    public static final int API = 1;
    private PluginContext context;

    public final void attach(PluginContext context) {
        if (this.context != null) throw new IllegalStateException("Already attached");
        this.context = context;
    }

    public final PluginContext ttcuz() {
        return context;
    }

    /** Loader thread: prepare files or data here. Never block the UI in onStart. */
    public void onLoad() throws Exception {}

    /** Main thread, after onLoad. */
    public void onStart() {}

    public void onStop() {}

    public void onActivityCreated(Activity activity) {}

    public void onActivityResumed(Activity activity) {}

    public void onActivityPaused(Activity activity) {}

    public void onActivityDestroyed(Activity activity) {}

    public void onAccountChanged(String uid) {}

    public void onPreferenceChanged(String scope, String key) {}

    public void onSettings(PluginSettings settings) {}

    /** Return true if the plugin displayed its own settings UI. */
    public boolean onOpenSettings(Activity activity) {
        return false;
    }

    /** Bind-time hook, not an every-frame traversal. Make view changes idempotent. */
    public void onView(View view) {}

    /** Transform hooks run on the caller's thread; keep them fast. */
    public int onColour(int colour) {
        return colour;
    }

    public String onRegion(String key, String value) {
        return value;
    }

    public String onNickname(String uid, String value) {
        return value;
    }

    public CharSequence onText(TextView view, CharSequence value) {
        return value;
    }

    public List onFeed(List posts) {
        return posts;
    }

    /** Custom events shared by plugins; payloads are ordinary Java objects. */
    public void onEvent(String event, Object payload) {}
}
