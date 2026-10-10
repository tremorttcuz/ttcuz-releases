package cat.narezany.margyt;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import java.util.HashMap;
import java.util.Map;

/**
 * Which icon the app wears.
 *
 * Android decides that from the component the home screen opens, and there is
 * no way to change one while the app runs -- so the build ships one component
 * per icon, all pointing at the same activity, all switched off. Choosing an
 * icon turns one on and turns the others off, and because they all lead to the
 * same place the app opens exactly as it did.
 *
 * TikTok's own entry is itself one of these -- an alias in front of
 * MainActivity -- so the default is not a special case: it is just the one the
 * build found already there.
 *
 * The launcher usually notices within a second or two. Some of them want the
 * home screen redrawn before they will, which is a launcher's business and not
 * something an app is allowed to insist on.
 *
 * WHEN THE SWITCH HAPPENS. Flipping the components while the app is on screen
 * is what used to close TikTok on some phones: the launcher entry the running
 * task was started from goes away under it, and several system builds answer
 * that by killing the process even with DONT_KILL_APP. So the choice is
 * remembered at once -- the settings show it straight away -- but the
 * components are only switched when the app has gone to the background, with
 * the new entry turned on before any old one is turned off.
 */
public final class Launcher {

    private Launcher() {}

    public static final String KEY = "icon";

    /** The empty name is TikTok's own, the one the build did not add. */
    public static final String DEFAULT = "";

    /** The icon that wears the accent: the choice, not a component. */
    public static final String ACCENT = "accent";

    private static final String VARIANT_KEY = "icon_variant";

    /** What the strip offers: the default and every icon that is not a hue variant. */
    public static String[] all() {
        int shown = 1;
        for (int i = 0; i < Shots.KEYS.length; i++) if (!Shots.AUTO[i]) shown++;
        String[] out = new String[shown];
        out[0] = DEFAULT;
        int at = 1;
        for (int i = 0; i < Shots.KEYS.length; i++) if (!Shots.AUTO[i]) out[at++] = Shots.KEYS[i];
        return out;
    }

    public static String nameOf(String which) {
        int at = indexOf(which);
        return at < 0 ? Text.ICON_DEFAULT : Shots.LABELS[at];
    }

    public static String chosen() {
        resumePending();
        SharedPreferences prefs = prefs();
        String which = prefs == null ? DEFAULT : prefs.getString(KEY, DEFAULT);
        if (!DEFAULT.equals(which) && indexOf(which) < 0) {
            Context context = Margy.context();
            if (context != null) choose(context, DEFAULT);
            return DEFAULT;
        }
        return which;
    }

    /**
     * Remember the icon that was chosen and switch to it as soon as the app is
     * out of sight (see the note on the class).
     *
     * DONT_KILL_APP matters: without it Android stops the app the moment one
     * of its components is switched, which from the inside looks like the mod
     * crashing the phone's TikTok for changing a picture. With it, doing the
     * switch while the app is still on screen can still end the process on
     * some phones -- which is why it waits.
     */
    public static void choose(Context context, String which) {
        if (context == null) return;
        if (indexOf(which) < 0 || isVariant(which)) which = DEFAULT;
        // the accent icon is whichever hue variant is nearest the accent now
        String wear = ACCENT.equals(which) ? variantFor(Accent.colour()) : which;
        try {
            SharedPreferences prefs = prefs();
            if (prefs != null) {
                prefs.edit().putString(KEY, which).putString(VARIANT_KEY, wear)
                        .putBoolean(PENDING_KEY, true).apply();
            }
            Diary.note("icon: " + (DEFAULT.equals(which) ? "ttcuz" : wear) + " (when the app is in the background)");
            applyWhenHidden(context);
        } catch (Throwable error) {
            Diary.note("icon: " + error);
        }
    }

    // ------------------------------------------------ switching only out of sight

    private static final String PENDING_KEY = "icon_pending";

    private static final Handler main = new Handler(Looper.getMainLooper());
    private static boolean watching;
    private static int started;

    /** Register before any activity starts, so overlapping screens are counted too. */
    public static synchronized void watch(Application app) {
        if (watching || app == null) return;
        app.registerActivityLifecycleCallbacks(WATCH);
        watching = true;
    }

    /** Started activities of this process; the switch waits until there are none. */
    private static final Application.ActivityLifecycleCallbacks WATCH =
            new Application.ActivityLifecycleCallbacks() {
        @Override public void onActivityCreated(Activity a, Bundle b) { }
        @Override public void onActivityStarted(Activity a) {
            started++;
            main.removeCallbacks(settle);
        }
        @Override public void onActivityResumed(Activity a) { }
        @Override public void onActivityPaused(Activity a) { }
        @Override public void onActivityStopped(Activity a) {
            if (started > 0) started--;
            if (started == 0) main.postDelayed(settle, 700);
        }
        @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
        @Override public void onActivityDestroyed(Activity a) { }
    };

    private static final Runnable settle = new Runnable() {
        @Override public void run() {
            try {
                if (started != 0) return;      // somebody came back: wait for the next time
                Context context = Margy.context();
                if (context != null) applyNow(context);
            } catch (Throwable error) {
                Diary.note("icon: " + error);
            }
        }
    };

    /**
     * Called from the screens that can be on top: it is only ever reached while
     * the app is in use, so one started activity is already running.
     */
    private static synchronized void applyWhenHidden(Context context) {
        try {
            if (!watching) {
                Context app = context.getApplicationContext();
                if (app instanceof Application) {
                    started = 1;   // the caller is on screen: its onStart has already gone by
                    ((Application) app).registerActivityLifecycleCallbacks(WATCH);
                    watching = true;
                    return;
                }
                // no way to see the app go away: switch now, in the safe order
                applyNow(context);
            } else if (started == 0) {
                // already in the background: nothing will stop it, so no need to wait
                main.removeCallbacks(settle);
                main.postDelayed(settle, 700);
            }
        } catch (Throwable error) {
            Diary.note("icon: " + error);
        }
    }

    /** A choice made earlier that never got switched (the process was killed first). */
    private static void resumePending() {
        try {
            SharedPreferences prefs = prefs();
            if (prefs == null || !prefs.getBoolean(PENDING_KEY, false)) return;
            Context context = Margy.context();
            if (context != null) applyWhenHidden(context);
        } catch (Throwable ignored) {
        }
    }

    /** The components, switched for real: the one to wear first, then everything else off. */
    private static synchronized void applyNow(Context context) {
        SharedPreferences prefs = prefs();
        if (prefs == null || !prefs.getBoolean(PENDING_KEY, false)) return;
        String which = prefs.getString(KEY, DEFAULT);
        String wear = prefs.getString(VARIANT_KEY, which);
        if (indexOf(which) < 0 && !DEFAULT.equals(which)) which = DEFAULT;
        String target;
        if (DEFAULT.equals(which)) {
            target = Shots.DEFAULT;
        } else {
            int at = indexOf(wear);
            target = at < 0 ? Shots.DEFAULT : Shots.COMPONENTS[at];
        }
        PackageManager packages = context.getPackageManager();
        String self = context.getPackageName();

        // never a moment without a launcher entry: the new one goes on before the old ones go off
        boolean ok = set(packages, self, target, true);
        if (!ok) {
            // the component is not there (an older build): leave everything as it was
            Diary.note("icon: " + target + " is missing, nothing switched");
            prefs.edit().putBoolean(PENDING_KEY, false).apply();
            return;
        }
        for (int i = 0; i < Shots.COMPONENTS.length; i++) {
            if (!Shots.COMPONENTS[i].equals(target)) set(packages, self, Shots.COMPONENTS[i], false);
        }
        for (int i = 0; i < 17; i++) {
            String legacy = "cat.narezany.margyt.Icon" + i;
            if (!legacy.equals(target)) set(packages, self, legacy, false);
        }
        if (!Shots.DEFAULT.equals(target)) set(packages, self, Shots.DEFAULT, false);
        prefs.edit().putBoolean(PENDING_KEY, false).apply();
        Diary.note("icon switched: " + (DEFAULT.equals(which) ? "ttcuz" : wear));
    }

    /** One component on or off; false when it does not exist or the system refused. */
    private static boolean set(PackageManager packages, String self,
                               String component, boolean on) {
        try {
            ComponentName name = new ComponentName(self, component);
            int want = on ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                          : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            // already in the wanted state: touching it again only makes the launcher redraw
            if (packages.getComponentEnabledSetting(name) == want) return true;
            packages.setComponentEnabledSetting(name, want, PackageManager.DONT_KILL_APP);
            return true;
        } catch (Throwable error) {
            Diary.note("icon " + component + ": " + error);
            return false;
        }
    }

    // ------------------------------------------------ the icon that follows the accent

    private static boolean isVariant(String which) {
        int at = indexOf(which);
        return at >= 0 && Shots.AUTO[at];
    }

    /**
     * The hue variant nearest an accent: twelve hues thirty degrees apart, and a
     * grey one for accents with next to no colour in them.
     */
    static String variantFor(int accent) {
        float[] hsv = new float[3];
        Color.colorToHSV(accent, hsv);
        int hue = -1;
        if (hsv[1] >= GREY_BELOW) hue = (Math.round(hsv[0] / 30f) % 12) * 30;
        for (int i = 0; i < Shots.KEYS.length; i++) {
            if (Shots.AUTO[i] && Shots.HUE[i] == hue) return Shots.KEYS[i];
        }
        return ACCENT;
    }

    private static final float GREY_BELOW = 0.15f;

    private static final Handler later = new Handler(Looper.getMainLooper());

    private static final Runnable follow = new Runnable() {
        @Override public void run() {
            try {
                Context context = Margy.context();
                if (context == null || !ACCENT.equals(chosen())) return;
                SharedPreferences prefs = prefs();
                String now = prefs == null ? "" : prefs.getString(VARIANT_KEY, "");
                if (variantFor(Accent.colour()).equals(now)) return;  // already wearing it
                choose(context, ACCENT);
            } catch (Throwable error) {
                Diary.note("icon: " + error);
            }
        }
    };

    /**
     * The accent changed: if the accent icon is the one in use, quietly move to
     * the variant that matches. Nothing restarts. It waits a moment so dragging
     * a colour slider does not switch components on every step.
     */
    public static void followAccent() {
        later.removeCallbacks(follow);
        later.postDelayed(follow, 600);
    }

    /** The two tones of the accent icon: [background and glyph, shape]. Same recipe as the build. */
    static int[] tone(int accent) {
        float[] hsv = new float[3];
        Color.colorToHSV(accent, hsv);
        float k = hsv[1] >= GREY_BELOW ? 1f : 0.12f;
        return new int[] {hsl(hsv[0], 0.50f * k, 0.37f), hsl(hsv[0], 0.78f * k, 0.83f)};
    }

    private static int hsl(float hue, float s, float l) {
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((hue / 60f) % 2 - 1));
        float m = l - c / 2;
        float r, g, b;
        int sector = ((int) (hue / 60f)) % 6;
        switch (sector) {
            case 0: r = c; g = x; b = 0; break;
            case 1: r = x; g = c; b = 0; break;
            case 2: r = 0; g = c; b = x; break;
            case 3: r = 0; g = x; b = c; break;
            case 4: r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }
        return Color.rgb(Math.round((r + m) * 255), Math.round((g + m) * 255), Math.round((b + m) * 255));
    }

    private static Bitmap shape, glyph;

    private static Bitmap layer(String[] png) {
        StringBuilder whole = new StringBuilder();
        for (String piece : png) whole.append(piece);
        byte[] raw = Base64.decode(whole.toString(), Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(raw, 0, raw.length);
    }

    /** The accent icon drawn in the colours of an accent, for the settings to show. */
    static Bitmap accentPicture(int accent) {
        try {
            synchronized (drawn) {
                if (shape == null) shape = layer(Shots.ART_SHAPE);
                if (glyph == null) glyph = layer(Shots.ART_GLYPH);
            }
            int[] tone = tone(accent);
            int size = 192;
            Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(out);
            canvas.drawColor(tone[0]);
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
            android.graphics.Rect to = new android.graphics.Rect(0, 0, size, size);
            paint.setColorFilter(new PorterDuffColorFilter(tone[1], PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(shape, null, to, paint);
            paint.setColorFilter(new PorterDuffColorFilter(tone[0], PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(glyph, null, to, paint);
            return out;
        } catch (Throwable error) {
            Diary.note("icon: " + error);
            return null;
        }
    }

    /** The picture for the settings to show, decoded once and kept. */
    public static Bitmap preview(String which) {
        if (ACCENT.equals(which)) return accentPicture(Accent.colour());
        int at = indexOf(which);
        String[] png = at < 0 ? Shots.DEFAULT_PNG : Shots.PNG[at];
        if (which == null) return null;
        synchronized (drawn) {
            Bitmap known = drawn.get(which);
            if (known != null) return known;
            try {
                StringBuilder whole = new StringBuilder();
                for (String piece : png) whole.append(piece);
                byte[] raw = Base64.decode(whole.toString(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                if (bitmap != null) drawn.put(which, bitmap);
                return bitmap;
            } catch (Throwable error) {
                Diary.note("icon: " + error);
                return null;
            }
        }
    }

    private static final Map<String, Bitmap> drawn = new HashMap<String, Bitmap>();

    private static int indexOf(String which) {
        if (which == null) return -1;
        for (int i = 0; i < Shots.KEYS.length; i++) {
            if (Shots.KEYS[i].equals(which)) return i;
        }
        return -1;
    }

    private static SharedPreferences prefs() {
        Context context = Margy.context();
        if (context == null) return null;
        return context.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE);
    }
}
