package cat.ttcuz.framework;

import android.app.ActivityManager;
import android.content.Context;

final class DevicePerformance {
    private static volatile Boolean limited;

    DevicePerformance() {
    }

    /* JADX WARN: Code restructure failed: missing block: B:16:0x0023, code lost:
    
        if (r2.getMemoryClass() <= 128) goto L16;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    static boolean limited(Context context) {
        Boolean bool = limited;
        if (bool != null) {
            return bool.booleanValue();
        }
        boolean z = false;
        if (context == null) {
            return false;
        }
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
            // a low-RAM device, or one that gives an app 128 MB of heap or less
            if (activityManager != null) {
                z = activityManager.isLowRamDevice() || activityManager.getMemoryClass() <= 128;
            }
        } catch (Throwable unused) {
        }
        limited = Boolean.valueOf(z);
        return z;
    }

    static int previewEdge() {
        return limited(Ttcuz.context()) ? 480 : 720;
    }
}
