package cat.ttcuz.framework;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.util.Base64;
import java.io.File;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;

/** Keep the native task alive beneath the gate; remember trust for this installed APK. */
final class AppIntegrity implements Application.ActivityLifecycleCallbacks {

    private static final Handler main = new Handler(Looper.getMainLooper());
    private static volatile int state; // 0 pending, 1 authentic, 2 altered, 3 unreadable
    private static boolean started, opening;
    private static Context app;
    private static Runnable onTrusted;
    private static WeakReference<IntegrityActivity> screen = new WeakReference<>(null);

    private AppIntegrity() {}

    static void start(Context context, Runnable trusted) {
        if (started) return;
        started = true;
        Context application = context.getApplicationContext();
        if (!(application instanceof Application)) {
            android.util.Log.w("ttcuz", "integrity: Application context missing");
            started = false;
            return;
        }
        ((Application) application).registerActivityLifecycleCallbacks(new AppIntegrity());
        app = application;
        onTrusted = trusted;
        check();
    }

    static void retry() {
        if (state != 3) return;
        state = 0;
        IntegrityActivity activity = screen.get();
        if (activity != null) activity.result(0);
        check();
    }

    private static void check() {
        final Context application = app;
        Thread worker = new Thread(() -> {
            int result;
            try {
                PackageManager pm = application.getPackageManager();
                PackageInfo info = pm.getPackageInfo(
                    application.getPackageName(),
                    Build.VERSION.SDK_INT >= 28
                        ? PackageManager.GET_SIGNING_CERTIFICATES
                        : PackageManager.GET_SIGNATURES
                );
                android.content.pm.Signature[] signatures =
                    Build.VERSION.SDK_INT >= 28 && info.signingInfo != null
                        ? info.signingInfo.getApkContentsSigners()
                        : info.signatures;
                if (signatures == null || signatures.length != 1) throw new SecurityException(
                    "Unexpected APK signers"
                );
                ApplicationInfo app = application.getApplicationInfo();
                if (
                    app.splitSourceDirs != null && app.splitSourceDirs.length > 0
                ) throw new SecurityException("Split APK not supported");
                String certificate = IntegrityVerifier.hex(
                    MessageDigest.getInstance("SHA-256").digest(signatures[0].toByteArray())
                );
                File apk = new File(app.sourceDir);
                String identity =
                    application.getPackageName() +
                    "|" +
                    info.versionCode +
                    "|" +
                    info.lastUpdateTime +
                    "|" +
                    apk.getAbsolutePath() +
                    "|" +
                    apk.length() +
                    "|" +
                    apk.lastModified() +
                    "|" +
                    certificate +
                    "|" +
                    IntegrityKey.PUBLIC_KEY;
                SharedPreferences prefs = null;
                try {
                    prefs = application.getSharedPreferences(
                        "ttcuz_integrity",
                        Context.MODE_PRIVATE
                    );
                } catch (RuntimeException error) {
                    android.util.Log.w("ttcuz", "integrity cache unavailable", error);
                }
                boolean cached = false;
                if (prefs != null) try {
                    cached = identity.equals(prefs.getString("trusted_apk", null));
                } catch (RuntimeException error) {
                    android.util.Log.w("ttcuz", "integrity cache read", error);
                }
                if (!cached) {
                    IntegrityVerifier.verify(
                        apk,
                        application.getPackageName(),
                        certificate,
                        Base64.decode(IntegrityKey.PUBLIC_KEY, Base64.DEFAULT)
                    );
                    if (prefs != null) try {
                        prefs.edit().putString("trusted_apk", identity).commit();
                    } catch (RuntimeException error) {
                        android.util.Log.w("ttcuz", "integrity cache write", error);
                    }
                }
                result = 1;
            } catch (SecurityException error) {
                result = 2;
                android.util.Log.w("ttcuz", "integrity: altered", error);
            } catch (Throwable error) {
                result = 3;
                android.util.Log.w("ttcuz", "integrity: verification unavailable", error);
            }
            final int outcome = result;
            main.post(() -> {
                state = outcome;
                if (outcome == 1) try {
                    onTrusted.run();
                } catch (Throwable error) {
                    android.util.Log.w("ttcuz", "trusted startup", error);
                }
                IntegrityActivity activity = screen.get();
                if (activity != null) activity.result(outcome);
            });
        }, "ttcuz-integrity");
        worker.setDaemon(true);
        worker.start();
    }

    static void shown(IntegrityActivity activity) {
        opening = false;
        screen = new WeakReference<>(activity);
        activity.result(state);
    }

    static void gone(IntegrityActivity activity) {
        if (screen.get() == activity) screen.clear();
    }

    private static void gate(Activity activity) {
        if (state == 1 || activity instanceof IntegrityActivity || activity.isFinishing()) return;
        if (!opening && screen.get() == null) {
            opening = true;
            try {
                activity.startActivity(new Intent(activity, IntegrityActivity.class));
            } catch (RuntimeException error) {
                opening = false;
                android.util.Log.e("ttcuz", "integrity screen", error);
            }
        }
    }

    public void onActivityCreated(Activity activity, Bundle saved) {}

    public void onActivityResumed(Activity activity) {
        gate(activity);
    }

    public void onActivityStarted(Activity activity) {}

    public void onActivityPaused(Activity activity) {}

    public void onActivityStopped(Activity activity) {}

    public void onActivitySaveInstanceState(Activity activity, Bundle saved) {}

    public void onActivityDestroyed(Activity activity) {}
}
