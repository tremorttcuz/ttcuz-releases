package cat.ttcuz.framework;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.view.FrameMetrics;
import android.view.View;
import android.view.Window;
import java.util.HashMap;
import java.util.Map;

public final class Lag {
    private static final long ABSURD = 30000;
    private static final long BAD = 700;
    private static final int ENOUGH = 12;
    private static final long LOOK = 50;
    private static final long SHORT = 100;
    private static final String[] STAGES;
    private static final LagTally TALLY;
    private static final long TOO_MUCH = 3000;
    private static final int WATCHED = 1298231884;
    private static volatile long asked;
    private static volatile StackTraceElement[] early;
    private static Handler frames;
    private static volatile StackTraceElement[] late;
    private static volatile int looks;
    private static volatile boolean frameCaptureEnabled;
    private static boolean started;
    private static int told;
    private static volatile long worst;
    private static volatile int stuckTold;
    private static final long BEGAN = SystemClock.elapsedRealtime();
    private static final HashMap<String, long[]> PLUGIN_COST = new HashMap<>();

    static {
        // nothing asked yet: without this the first look read "stuck since 0" and wrote a false hang
        asked = BEGAN;
        String[] strArr = {"waiting for the main thread", "input", "animation", "layout", "draw", "sync", "commands", "swap", "gpu"};
        STAGES = strArr;
        TALLY = new LagTally(strArr);
    }

    private Lag() {
    }

    static boolean frameCaptureEnabled() {
        return frameCaptureEnabled;
    }

    static void setFrameCaptureEnabled(boolean enabled) {
        frameCaptureEnabled = enabled;
    }

    public static synchronized void watch() {
        synchronized (Lag.class) {
            if (started) {
                return;
            }
            started = true;
            try {
                android.content.Context context = Ttcuz.context();
                frameCaptureEnabled = context != null && context.getSharedPreferences(
                        Ttcuz.PREFS, android.content.Context.MODE_PRIVATE)
                        .getBoolean("lag_frame_stats", false);
            } catch (RuntimeException error) {
                Diary.note("lag: frame preference: " + error);
            }
            try {
                Thread thread = new Thread(new Runnable() { // from class: cat.ttcuz.framework.Lag$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        Lag.look();
                    }
                }, "ttcuz-lag");
                thread.setDaemon(true);
                thread.setPriority(1);
                thread.start();
            } catch (Throwable th) {
                Diary.note("lag: " + String.valueOf(th));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void look() {
        Thread thread = Looper.getMainLooper().getThread();
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() { // from class: cat.ttcuz.framework.Lag$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                Lag.answered();
            }
        };
        while (true) {
            try {
                if (DisplayPolicy.here() == null) {
                    Thread.sleep(1000L);
                } else {
                    Thread.sleep(LOOK);
                    long uptimeMillis = SystemClock.uptimeMillis();
                    long j = asked;
                    if (j == BEGAN) {
                        stuckTold = 0;
                        early = null;
                        late = null;
                        looks = 0;
                        asked = uptimeMillis;
                        handler.post(runnable);
                    } else {
                        looks++;
                        long j2 = uptimeMillis - j;
                        if (j2 >= SHORT && early == null) {
                            early = thread.getStackTrace();
                        }
                        if (j2 >= BAD && late == null) {
                            late = thread.getStackTrace();
                        }
                        // still stuck: written now, while it hangs -- a hang Android ends with "not responding" never
                        // comes back to answered(), so nothing would be written at all
                        if ((j2 >= 1500 && stuckTold == 0) || (j2 >= 4000 && stuckTold == 1)) {
                            stuckTold++;
                            StringBuilder trace = new StringBuilder();
                            StackTraceElement[] now = thread.getStackTrace();
                            for (int k = 0; k < now.length && k < 40; k++) trace.append("\n    at ").append(now[k]);
                            Diary.note("lag: main thread stuck for " + j2 + " ms" + trace);
                        }
                    }
                }
            } catch (InterruptedException unused) {
                return;
            } catch (Throwable unused2) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void answered() {
        int i;
        try {
            long uptimeMillis = SystemClock.uptimeMillis() - asked;
            StackTraceElement[] stackTraceElementArr = (uptimeMillis < BAD || late == null) ? early : late;
            int i2 = looks;
            asked = BEGAN;
            if (uptimeMillis < SHORT || uptimeMillis > ABSURD || stackTraceElementArr == null || DisplayPolicy.here() == null || i2 < uptimeMillis / 200) {
                return;
            }
            String where = where(stackTraceElementArr);
            if (where.length() == 0) {
                return;
            }
            String plugin = plugin(stackTraceElementArr);
            String mine = mine(stackTraceElementArr);
            LagTally lagTally = TALLY;
            if (plugin != null) {
                mine = "plugin " + plugin;
            } else if (mine == null) {
                mine = brief(where);
            }
            lagTally.stall(mine, where, uptimeMillis);
            if (plugin != null) {
                charge(plugin, uptimeMillis);
            }
            if (uptimeMillis > worst) {
                worst = uptimeMillis;
            }
            if (uptimeMillis < BAD || (i = told) >= ENOUGH) {
                return;
            }
            told = i + 1;
            Diary.note("lag: the app stopped for " + uptimeMillis + " ms in " + where);
        } catch (Throwable unused) {
            asked = BEGAN;
        }
    }

    static void frames(Activity activity) {
        float f;
        if (!frameCaptureEnabled || Build.VERSION.SDK_INT < 24 || activity == null) {
            return;
        }
        try {
            Window window = activity.getWindow();
            View decorView = window == null ? null : window.getDecorView();
            if (decorView != null && decorView.getTag(WATCHED) == null) {
                decorView.setTag(WATCHED, Boolean.TRUE);
                if (frames == null) {
                    HandlerThread handlerThread = new HandlerThread("ttcuz-frames", 10);
                    handlerThread.start();
                    frames = new Handler(handlerThread.getLooper());
                }
                final String simpleName = activity.getClass().getSimpleName();
                try {
                    f = activity.getWindowManager().getDefaultDisplay().getRefreshRate();
                } catch (Throwable unused) {
                    f = 60.0f;
                }
                final long max = (long) (2.0E9d / Math.max(30.0f, f));
                final long[] jArr = new long[STAGES.length];
                window.addOnFrameMetricsAvailableListener(new Window.OnFrameMetricsAvailableListener() { // from class: cat.ttcuz.framework.Lag$$ExternalSyntheticLambda2
                    @Override // android.view.Window.OnFrameMetricsAvailableListener
                    public final void onFrameMetricsAvailable(Window window2, FrameMetrics frameMetrics, int i) {
                        Lag.lambda$frames$0(max, simpleName, jArr, window2, frameMetrics, i);
                    }
                }, frames);
            }
        } catch (Throwable th) {
            Diary.detail("lag: frames: " + String.valueOf(th));
        }
    }

    static /* synthetic */ void lambda$frames$0(long j, String str, long[] jArr, Window window, FrameMetrics frameMetrics, int i) {
        try {
            if (!frameCaptureEnabled) return;
            long metric = frameMetrics.getMetric(8);
            if (metric <= j) {
                TALLY.frame(str, false, false, jArr);
                return;
            }
            jArr[0] = frameMetrics.getMetric(0);
            jArr[1] = frameMetrics.getMetric(1);
            jArr[2] = frameMetrics.getMetric(2);
            jArr[3] = frameMetrics.getMetric(3);
            jArr[4] = frameMetrics.getMetric(4);
            jArr[5] = frameMetrics.getMetric(5);
            jArr[6] = frameMetrics.getMetric(6);
            jArr[7] = frameMetrics.getMetric(7);
            jArr[8] = Build.VERSION.SDK_INT >= 31 ? frameMetrics.getMetric(ENOUGH) : BEGAN;
            TALLY.frame(str, true, metric > 100000000, jArr);
        } catch (Throwable unused) {
        }
    }

    static String report() {
        long elapsedRealtime = (SystemClock.elapsedRealtime() - BEGAN) / 1000;
        return "since the app started: " + (elapsedRealtime / 60) + " min " + (elapsedRealtime % 60) + " s\n" + TALLY.report();
    }

    static String plugin(StackTraceElement[] stackTraceElementArr) {
        if (stackTraceElementArr == null) {
            return null;
        }
        for (StackTraceElement stackTraceElement : stackTraceElementArr) {
            String owner = PluginEngine.owner(stackTraceElement.getClassName());
            if (owner != null) {
                return owner;
            }
        }
        return null;
    }

    private static void charge(String str, long j) {
        boolean z;
        HashMap<String, long[]> hashMap = PLUGIN_COST;
        synchronized (hashMap) {
            long[] jArr = hashMap.get(str);
            if (jArr == null) {
                jArr = new long[2];
                hashMap.put(str, jArr);
            }
            long j2 = jArr[0];
            z = j2 < TOO_MUCH && j2 + j >= TOO_MUCH;
            jArr[0] = j2 + j;
            jArr[1] = jArr[1] + 1;
        }
        if (z) {
            Diary.note("lag: plugin " + str + " has stalled the app for 3 s this run");
        }
    }

    static long cost(String str) {
        HashMap<String, long[]> hashMap = PLUGIN_COST;
        synchronized (hashMap) {
            // the plugin's own entry, or one it shares with another ("a / b")
            for (Map.Entry<String, long[]> entry : hashMap.entrySet()) {
                String key = entry.getKey();
                if (key.equals(str) || key.contains(str + " / ") || key.endsWith(" / " + str)) {
                    return entry.getValue()[0];
                }
            }
            return 0L;
        }
    }

    static String mine(StackTraceElement[] stackTraceElementArr) {
        if (stackTraceElementArr == null) {
            return null;
        }
        for (StackTraceElement stackTraceElement : stackTraceElementArr) {
            if (stackTraceElement.getClassName().startsWith("cat.ttcuz.framework.")) {
                return name(stackTraceElement);
            }
        }
        return null;
    }

    static String brief(String str) {
        int indexOf = str.indexOf(" < ");
        int indexOf2 = indexOf < 0 ? -1 : str.indexOf(" < ", indexOf + 3);
        return indexOf2 < 0 ? str : str.substring(0, indexOf2);
    }

    static String where(StackTraceElement[] stackTraceElementArr) {
        if (stackTraceElementArr == null || stackTraceElementArr.length == 0) {
            return "";
        }
        if ((stackTraceElementArr[0].getClassName() + "." + stackTraceElementArr[0].getMethodName()).equals("android.os.MessageQueue.nativePollOnce")) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String str = null;
        int i = 0;
        for (StackTraceElement stackTraceElement : stackTraceElementArr) {
            String className = stackTraceElement.getClassName();
            if (str == null && className.startsWith("cat.ttcuz.framework.")) {
                str = name(stackTraceElement);
            }
            if (i < 5) {
                if (sb.length() > 0) {
                    sb.append(" < ");
                }
                sb.append(name(stackTraceElement));
                i++;
            }
        }
        if (str != null && sb.indexOf(str) < 0) {
            sb.append(" << ");
            sb.append(str);
        }
        return sb.toString();
    }

    private static String name(StackTraceElement stackTraceElement) {
        String str;
        String className = stackTraceElement.getClassName();
        int lastIndexOf = className.lastIndexOf(46);
        if (lastIndexOf > 0 && !className.startsWith("X.")) {
            className = className.substring(lastIndexOf + 1);
        }
        String methodName = stackTraceElement.getMethodName();
        if (stackTraceElement.getLineNumber() <= 0 || stackTraceElement.getLineNumber() >= 100000) {
            str = "";
        } else {
            str = ":" + stackTraceElement.getLineNumber();
        }
        return className + "." + methodName + str;
    }

    public static long worst() {
        return worst;
    }
}
