package cat.ttcuz.framework;

import android.os.Looper;
import android.view.Choreographer;

/**
 * How long the app stopped for, and what it was doing, written down.
 *
 * "It freezes" is not something anyone can act on, and neither is a guess
 * about why. The main thread draws a frame every sixteen milliseconds when
 * all is well, so a gap much larger than that is a stall somebody felt, and
 * the diary can say how long and how often.
 *
 * How long was all it used to say, and that turned out not to be enough: a
 * person whose feed stalls can copy a diary full of "stopped for 1800 ms" and
 * nobody is any the wiser. So a second thread looks in now and then, and when
 * no frame has been drawn for a while it asks the main thread where it is.
 * That is the line worth having -- and where the main thread turns out to be
 * waiting for something to do, it was not a stall at all, only a screen with
 * nothing to draw, and nothing is written.
 *
 * The cost is one comparison per frame and a wake-up four times a second
 * that reads one number. The stack is taken only during a stall.
 */
public final class Lag {

    /** Long enough that a person notices, rather than merely a dropped frame. */
    private static final long BAD = 700;

    /** Enough to show a pattern without filling the diary. */
    private static final int ENOUGH = 12;

    /** How often the other thread looks in. */
    private static final long LOOK = 250;

    private static long lastFrame;
    private static int told;
    private static long worst;

    /** When the last frame was drawn, by the clock the watcher reads. */
    private static volatile long drawn;
    /** Where the main thread was found during the stall now going on, or "" for idle. */
    private static volatile String caught;

    private Lag() {}

    public static void watch() {
        try {
            Choreographer.getInstance().postFrameCallback(
                new Choreographer.FrameCallback() {
                    @Override
                    public void doFrame(long nanos) {
                        long now = nanos / 1000000L;
                        long since = lastFrame == 0 ? 0 : now - lastFrame;
                        lastFrame = now;
                        String where = caught;
                        caught = null;
                        drawn = android.os.SystemClock.uptimeMillis();
                        // found idle: frames stopped because there was nothing to draw
                        if (since > BAD && !"".equals(where)) {
                            if (since > worst) worst = since;
                            if (told < ENOUGH) {
                                told++;
                                Diary.note(
                                    "lag: the app stopped for " +
                                        since +
                                        " ms" +
                                        (where == null ? "" : " in " + where)
                                );
                            }
                        }
                        Choreographer.getInstance().postFrameCallback(this);
                    }
                }
            );
            Thread watcher = new Thread(Lag::look, "ttcuz-lag");
            watcher.setDaemon(true);
            watcher.setPriority(Thread.MIN_PRIORITY);
            watcher.start();
        } catch (Throwable error) {
            Diary.note("lag: " + error);
        }
    }

    private static void look() {
        Thread main = Looper.getMainLooper().getThread();
        while (true) {
            try {
                Thread.sleep(LOOK);
                long last = drawn;
                if (last == 0 || caught != null || told >= ENOUGH) continue;
                if (android.os.SystemClock.uptimeMillis() - last < BAD) continue;
                caught = where(main.getStackTrace());
            } catch (InterruptedException over) {
                return;
            } catch (Throwable ignored) {
                // a stack that could not be read is a stall with no place to it
            }
        }
    }

    /**
     * A stack said in one line: the first few frames that are not Android's
     * own plumbing, and the mod's first frame if it has one further down.
     * Empty when the thread is only waiting for its next message.
     */
    static String where(StackTraceElement[] stack) {
        if (stack == null || stack.length == 0) return "";
        String top = stack[0].getClassName() + "." + stack[0].getMethodName();
        if (top.equals("android.os.MessageQueue.nativePollOnce")) return "";
        StringBuilder out = new StringBuilder();
        int said = 0;
        String mine = null;
        for (StackTraceElement frame : stack) {
            String type = frame.getClassName();
            if (mine == null && type.startsWith("cat.ttcuz.framework.")) mine = name(frame);
            if (said < 5) {
                if (out.length() > 0) out.append(" < ");
                out.append(name(frame));
                said++;
            }
        }
        if (mine != null && out.indexOf(mine) < 0) out.append(" << ").append(mine);
        return out.toString();
    }

    private static String name(StackTraceElement frame) {
        String type = frame.getClassName();
        int dot = type.lastIndexOf('.');
        // a class with a real name is said without its package; an obfuscated
        // one keeps its "X." so it can be told from a real one
        String shortened = dot > 0 && !type.startsWith("X.") ? type.substring(dot + 1) : type;
        return (
            shortened +
            "." +
            frame.getMethodName() +
            (frame.getLineNumber() > 0 && frame.getLineNumber() < 100000
                ? ":" + frame.getLineNumber()
                : "")
        );
    }

    /** The longest stall this run, for the settings to show. */
    public static long worst() {
        return worst;
    }
}
