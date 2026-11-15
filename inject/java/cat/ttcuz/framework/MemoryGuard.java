package cat.ttcuz.framework;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;

/**
 * The mod gives memory back when the phone asks for it.
 *
 * Some firmware keeps TikTok to a small heap (a 100 MB growth limit was reported on a LineageOS build with an
 * out-of-memory crash a little after start). TikTok itself takes most of that; what the mod keeps -- decoded badges,
 * plugin pictures, sticker thumbnails, friends' avatars -- is all cache and can be drawn again. When Android signals
 * memory pressure, or the heap is small to begin with, those caches are emptied and kept small.
 */
public final class MemoryGuard implements ComponentCallbacks2 {
    private MemoryGuard() {}

    private static volatile boolean installed;

    /** The Java heap the app may grow to, in MB. */
    static long heapMb() { return Runtime.getRuntime().maxMemory() / (1024 * 1024); }

    /** A small heap: caches stay at a quarter of their usual size. */
    static boolean tight() { return heapMb() < 256; }

    /** A cache budget in bytes: the usual one, or a quarter of it on a small heap. */
    static int budget(int usual) { return tight() ? usual / 4 : usual; }

    static final String KEY_NO_GC_BLOCKER = "tiktok_gc_blocker_off";

    /**
     * File.exists() as TikTok's GC blocker (X.05DQ) asks it, for "/data/local/tmp/disable-gcblocker", TikTok's own
     * off switch. Answered yes on custom firmware (LineageOS and what is built on it), where the blocker's guesses
     * about ART are wrong and end in OutOfMemoryError, and when the setting asks for it; any other file as it is.
     */
    public static boolean exists(java.io.File file) {
        if (file != null && file.getPath().endsWith("disable-gcblocker") && gcBlockerOff()) {
            Diary.note("memory: TikTok's start-up GC blocker kept off" + (customFirmware() ? " (custom firmware)" : ""));
            return true;
        }
        return file != null && file.exists();
    }

    static boolean gcBlockerOff() {
        Context context = Ttcuz.context();
        boolean asked = context != null && context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).getBoolean(KEY_NO_GC_BLOCKER, false);
        return asked || customFirmware();
    }

    private static volatile boolean heapNoted;

    /**
     * Whether TikTok's native heap tuners may run (com.bytedance.sysoptimizer: HeapClampHelper, HeapGcRegulator,
     * HeapGCOptimizer). Like the GC blocker they patch ART through offsets guessed for stock firmware; on custom
     * firmware the guess is wrong -- the heap's target is clamped near 90 MB under a much larger limit, GC waits for
     * seconds and an allocation of a few kilobytes fails. There, and when the setting asks, they are not started.
     */
    public static boolean heapTuning() {
        if (!gcBlockerOff()) return true;
        if (!heapNoted) { heapNoted = true; Diary.note("memory: TikTok's native heap tuning kept off" + (customFirmware() ? " (custom firmware)" : "")); }
        return false;
    }

    private static volatile Boolean custom;

    /** LineageOS or a firmware built on it: its own system properties say so. */
    static boolean customFirmware() {
        Boolean known = custom;
        if (known != null) return known;
        boolean found = false;
        try {
            Class<?> properties = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method get = properties.getMethod("get", String.class);
            for (String key : new String[]{"ro.lineage.version", "ro.lineage.build.version", "ro.modversion", "ro.crdroid.version",
                    "ro.lunaris.version", "ro.evolution.version", "ro.pixelos.version", "ro.derp.version"})
                found |= String.valueOf(get.invoke(null, key)).length() > 0;
        } catch (Throwable ignored) { }
        String display = (android.os.Build.DISPLAY + " " + android.os.Build.FINGERPRINT).toLowerCase(java.util.Locale.ROOT);
        found |= display.contains("lineage") || display.contains("lunaris");
        custom = found;
        return found;
    }

    static void install(Context context) {
        if (installed || context == null) return;
        installed = true;
        try {
            context.getApplicationContext().registerComponentCallbacks(new MemoryGuard());
            Runtime runtime = Runtime.getRuntime();
            Diary.note("memory: heap limit " + heapMb() + " MB, in use " + ((runtime.totalMemory() - runtime.freeMemory()) / 1048576) + " MB"
                    + (tight() ? " (small heap: mod caches kept small)" : ""));
        } catch (Throwable error) {
            Diary.note("memory guard: " + error);
        }
    }

    /** Every cache of the mod that holds pictures, emptied. */
    static void trim(String why) {
        try {
            ProfileStyle.trimMemory();
            PluginList.trimMemory();
            PluginWorkbench.trimMemory();
            Streaks.trimMemory();
            FriendAvatars.trimMemory();
            LauncherPreview.trimMemory();
            Fonts.releasePreviews();
            Diary.note("memory: caches emptied (" + why + ")");
        } catch (Throwable error) {
            Diary.note("memory trim: " + error);
        }
    }

    @Override public void onTrimMemory(int level) {
        // TikTok left the screen (UI_HIDDEN) or memory runs low: pictures are cheap to draw again, a killed TikTok is not
        if (level >= TRIM_MEMORY_RUNNING_LOW) trim("level " + level);
    }

    @Override public void onLowMemory() { trim("low memory"); }

    @Override public void onConfigurationChanged(Configuration configuration) { }
}
