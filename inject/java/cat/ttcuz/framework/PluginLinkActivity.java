package cat.ttcuz.framework;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Where ttcuz://plugin/<id> lands: the store page in a browser follows that link on "Install in ttcuz".
 *
 * It shows nothing. It remembers which plugin was asked for, brings TikTok to the front (starting it when it is not
 * running) and closes; the first screen of TikTok that comes up opens the store at that plugin and offers to install
 * it, or, with ?review=1, its reviews. Only an id that looks like one is taken; the store itself says what it is.
 */
public final class PluginLinkActivity extends Activity {
    private static volatile String pending;
    private static volatile boolean pendingReview;
    private static volatile long pendingAt;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            Uri link = getIntent() == null ? null : getIntent().getData();
            String id = link == null || !"plugin".equals(link.getHost()) ? null : link.getLastPathSegment();
            if (id != null && id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,79}")) {
                pending = id;
                pendingReview = "1".equals(link.getQueryParameter("review"));
                pendingAt = System.currentTimeMillis();
                Diary.note("plugin link: " + id + (pendingReview ? " (review)" : ""));
                Activity open = Screen.now();
                if (open != null && !open.isFinishing()) {
                    // TikTok is already on screen behind this one: open the store there as soon as it is back
                    final Activity behind = open;
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> resumed(behind), 400L);
                } else {
                    Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    if (launch != null) startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
                }
            }
        } catch (Throwable error) {
            Diary.note("plugin link: " + error);
        }
        finish();
        overridePendingTransition(0, 0);
    }

    /** A TikTok screen came up: if a link is waiting (for at most two minutes), the store opens at its plugin. */
    static void resumed(final Activity activity) {
        final String id = pending;
        if (id == null || activity == null || activity instanceof PluginLinkActivity) return;
        if (System.currentTimeMillis() - pendingAt > 120000L) { pending = null; return; }
        // TikTok's splash comes and goes first: wait until a screen stays
        final android.view.View root = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
        if (root == null) return;
        root.postDelayed(() -> {
            if (pending == null || !id.equals(pending) || activity.isFinishing() || !activity.hasWindowFocus()) return;
            pending = null;
            PluginWorkbench.storeAt(activity, Skin.tonal(activity, Accent.colour()), id, pendingReview);
        }, 900L);
    }
}
