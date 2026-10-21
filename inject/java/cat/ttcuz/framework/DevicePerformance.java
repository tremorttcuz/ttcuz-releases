package cat.ttcuz.framework;

import android.app.ActivityManager;
import android.content.Context;

/** One platform check, shared by optional decoration and media previews. */
final class DevicePerformance {

    private static volatile Boolean limited;

    static boolean limited(Context context) {
        Boolean known = limited;
        if (known != null) return known;
        if (context == null) return false;
        boolean value = false;
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE
            );
            value =
                manager != null && (manager.isLowRamDevice() || manager.getMemoryClass() <= 128);
        } catch (Throwable ignored) {}
        limited = value;
        return value;
    }

    static int previewEdge() {
        return limited(Ttcuz.context()) ? 480 : 720;
    }
}
