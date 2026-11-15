package cat.ttcuz.framework;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

public final class Brightness {
    static final String KEY = "own_brightness";
    private static volatile Boolean cached;
    private static volatile boolean told;

    private Brightness() {
    }

    public static boolean isEnabled() {
        Boolean bool = cached;
        if (bool != null) {
            return bool.booleanValue();
        }
        SharedPreferences prefs = prefs();
        if (prefs == null) {
            return true;
        }
        boolean z = prefs.getBoolean(KEY, true);
        cached = Boolean.valueOf(z);
        return z;
    }

    public static void setEnabled(boolean z) {
        cached = Boolean.valueOf(z);
        SharedPreferences prefs = prefs();
        if (prefs != null) {
            prefs.edit().putBoolean(KEY, z).apply();
        }
    }

    private static SharedPreferences prefs() {
        Context context = Ttcuz.context();
        if (context == null) {
            return null;
        }
        try {
            return context.getSharedPreferences("ttcuz", 0);
        } catch (Throwable unused) {
            return null;
        }
    }

    public static void setAttributes(Window window, WindowManager.LayoutParams layoutParams) {
        if (layoutParams != null) {
            try {
                if (layoutParams.screenBrightness >= 0.0f && isEnabled()) {
                    if (!told) {
                        told = true;
                        Diary.detail("brightness: a window asked for " + layoutParams.screenBrightness + " and was not given it");
                    }
                    layoutParams.screenBrightness = -1.0f;
                }
            } catch (Throwable unused) {
            }
        }
        window.setAttributes(layoutParams);
    }

    public static boolean flat() {
        return isEnabled();
    }

    static void apply(Activity activity) {
        if (activity == null || !isEnabled()) {
            return;
        }
        try {
            Window window = activity.getWindow();
            if (window == null) {
                return;
            }
            WindowManager.LayoutParams attributes = window.getAttributes();
            if (attributes != null && attributes.screenBrightness >= 0.0f) {
                attributes.screenBrightness = -1.0f;
                window.setAttributes(attributes);
            }
            if (Build.VERSION.SDK_INT >= 26 && window.getColorMode() == 2) {
                window.setColorMode(0);
            }
            if (Build.VERSION.SDK_INT >= 35) {
                window.setDesiredHdrHeadroom(1.0f);
            }
        } catch (Throwable th) {
            Diary.detail("brightness: " + String.valueOf(th));
        }
    }
}
