package cat.ttcuz.framework;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

final class DisplayPolicy implements Application.ActivityLifecycleCallbacks {

    private static boolean started;
    private static java.lang.ref.WeakReference<Activity> current =
        new java.lang.ref.WeakReference<Activity>(null);

    static Activity here() {
        return current.get();
    }

    static synchronized void start(Application app) {
        if (started) return;
        app.registerActivityLifecycleCallbacks(new DisplayPolicy());
        started = true;
    }

    public void onActivityResumed(Activity activity) {
        current = new java.lang.ref.WeakReference<Activity>(activity);
        Rate.apply(activity);
        Hdr.apply(activity);
    }

    public void onActivityCreated(Activity activity, Bundle state) {}

    public void onActivityStarted(Activity activity) {}

    public void onActivityPaused(Activity activity) {
        if (current.get() == activity) current.clear();
    }

    public void onActivityStopped(Activity activity) {}

    public void onActivitySaveInstanceState(Activity activity, Bundle state) {}

    public void onActivityDestroyed(Activity activity) {}
}
