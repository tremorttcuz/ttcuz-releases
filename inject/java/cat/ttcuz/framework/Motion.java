package cat.ttcuz.framework;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

/** Respect the phone's animation scale, including disabled motion. */
final class Motion {
    private Motion() {}

    /**
     * One motion language for the whole app, the same one the navigation island uses: a fast start and a long,
     * soft landing, no bounce, no restart from zero. Every surface picks its curve and length from here, so
     * nothing moves faster, slower or sharper than its neighbours.
     */
    /** Arrival: ease-out quart (1-(1-t)^4), exactly the island's landing. */
    static final android.view.animation.PathInterpolator OUT = new android.view.animation.PathInterpolator(0.25f,1f,0.5f,1f);
    /** Departure: starts gently and leaves quickly, so a closing surface does not jolt. */
    static final android.view.animation.PathInterpolator IN = new android.view.animation.PathInterpolator(0.3f,0f,0.8f,0.15f);
    /** A change that begins and ends at rest (a size, a position that stays on screen). */
    static final android.view.animation.PathInterpolator BOTH = new android.view.animation.PathInterpolator(0.4f,0f,0.2f,1f);
    /** MD3 emphasized decelerate: a surface that enters the screen (a sheet rising) arrives fast and lands long. */
    static final android.view.animation.PathInterpolator EMPHASIZED = new android.view.animation.PathInterpolator(0.05f,0.7f,0.1f,1f);
    /** Fades and small colour changes. */
    static final long SHORT = 180L;
    /** Sheets, dialogs and disclosure bodies. */
    static final long MEDIUM = 300L;
    /** Large morphs (the island, the download pill). */
    static final long LONG = 340L;

    /** A small press response keeps the original click and scrolling intact. */
    static void press(android.view.View view) {
        view.setFocusable(true);
        touch(view);
        android.graphics.drawable.Drawable base = view.getBackground();
        if (base instanceof android.graphics.drawable.RippleDrawable) return;
        if (base == null) base = new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT);
        view.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf((Accent.colour() & 0xFFFFFF) | 0x22000000),
                base, new android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE)));
    }

    /** Non-consuming feedback: scrolling cancels it, and the original click still runs. */
    static void touch(android.view.View view) {
        view.setOnTouchListener((target,event) -> {
            int action=event.getActionMasked();
            if(action==android.view.MotionEvent.ACTION_DOWN || action==android.view.MotionEvent.ACTION_UP
                    || action==android.view.MotionEvent.ACTION_CANCEL) {
                boolean down=action==android.view.MotionEvent.ACTION_DOWN && target.isEnabled() && target.isClickable() && enabled(target.getContext());
                if(action==android.view.MotionEvent.ACTION_DOWN && target.isEnabled() && target.isClickable() && haptics())
                    target.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                // a tap (a release the list did not take for a scroll) clicks, softly and bony
                if(action==android.view.MotionEvent.ACTION_UP && target.isEnabled() && target.isClickable() && target.isPressed())
                    UiSound.tap();
                // Press sinks gently; release settles back slowly enough to read as one continuous gesture.
                target.animate().scaleX(down ? 0.975f : 1f).scaleY(down ? 0.975f : 1f)
                        .setDuration(time(down ? 120L : 260L))
                        .setInterpolator(OUT).start();
            }
            return false;
        });
    }
    static void change(android.view.View view) {
        if(!enabled(view.getContext()))return;
        view.setAlpha(0.8f);view.animate().alpha(1f).setDuration(time(SHORT+40L)).setInterpolator(OUT).start();
    }
    /**
     * Finish the outgoing page before entering the selected section. The page fades all the way out, so the swap
     * happens while it is invisible and the incoming page grows from the same alpha: there is no frame where the
     * old and the new content jump against each other.
     */
    static void navigate(android.view.View view,float direction,Runnable replace) {
        view.animate().withEndAction(null).cancel();
        if(!enabled(view.getContext())){replace.run();return;}
        view.animate().alpha(0f).translationX(-direction*8*view.getResources().getDisplayMetrics().density)
                .setDuration(time(120L)).setInterpolator(IN).withLayer().withEndAction(() -> {replace.run();enter(view,direction,0f);}).start();
    }

    /** Section navigation has one short directional transition, never per-row delays. */
    static void enter(android.view.View view, float direction) { enter(view,direction,0.4f); }

    /** The incoming page slides a short way and fades in over one soft landing; {@code from} is its starting alpha. */
    static void enter(android.view.View view, float direction, float from) {
        view.animate().withEndAction(null).cancel();
        if (!enabled(view.getContext())) { view.setAlpha(1f); view.setTranslationX(0f); return; }
        view.setAlpha(from);
        view.setTranslationX(direction * 16 * view.getResources().getDisplayMetrics().density);
        view.animate().alpha(1f).translationX(0f).setDuration(time(MEDIUM))
                .setInterpolator(OUT).withLayer().start();
    }

    /** Begin below the measured surface, then settle without overshoot. */
    static Runnable rise(android.view.View view) {
        view.animate().withEndAction(null).cancel();
        if (!enabled(view.getContext())) {
            view.setAlpha(1f); view.setTranslationY(0f); return null;
        }
        view.setAlpha(0f);
        Runnable start = () -> {
            if (!view.isAttachedToWindow()) return;
            view.setTranslationY(Math.max(view.getHeight(), 1));
            // drawn as one layer while it moves: a full sheet of rows rises without redrawing each row every frame
            view.animate().alpha(1f).translationY(0f).setDuration(time(MEDIUM+80L))
                    .setInterpolator(EMPHASIZED).withLayer()
                    .start();
        };
        view.post(start);
        return start;
    }

    /**
     * Soft entrance for a card or dialog: it grows from 92% and rises 16dp in one smooth landing
     * (no overshoot, so nothing wobbles at the end), fading in as it goes.
     */
    static void springIn(android.view.View view) {
        if (view == null || !enabled(view.getContext())) return;
        float unit = view.getResources().getDisplayMetrics().density;
        view.animate().cancel();
        view.setAlpha(0f);
        view.setScaleX(0.92f);
        view.setScaleY(0.92f);
        view.setTranslationY(16 * unit);
        view.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).setDuration(time(MEDIUM+40L))
                .setInterpolator(OUT).start();
    }

    // ------------------------------------------------------------ the person's choice (Settings → Оформление)

    static final String KEY_MODE = "motion_mode", KEY_HAPTICS = "motion_haptics",
            KEY_PERFORMANCE = "performance_mode";
    /** 0 as the phone does, 1 smoother and slower, 2 quicker, 3 none. */
    private static volatile int mode = -1;
    private static volatile Boolean haptics;
    private static volatile Boolean performance;

    static int mode() {
        int known = mode;
        if (known >= 0) return known;
        Context context = Ttcuz.context();
        if (context == null) return 0;
        known = Math.max(0, Math.min(3, context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).getInt(KEY_MODE, 0)));
        mode = known;
        return known;
    }

    static void setMode(int value) {
        mode = Math.max(0, Math.min(3, value));
        Context context = Ttcuz.context();
        if (context != null) context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_MODE, mode).apply();
    }

    static boolean performanceMode() {
        Boolean known = performance;
        if (known != null) return known;
        Context context = Ttcuz.context();
        if (context == null) return false;
        known = context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_PERFORMANCE, false);
        performance = known;
        return known;
    }

    static void setPerformanceMode(boolean enabled) {
        performance = enabled;
        Context context = Ttcuz.context();
        if (context != null) context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_PERFORMANCE, enabled).apply();
    }

    static boolean haptics() {
        Boolean known = haptics;
        if (known != null) return known;
        Context context = Ttcuz.context();
        if (context == null) return false;
        known = context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).getBoolean(KEY_HAPTICS, false);
        haptics = known;
        return known;
    }

    static void setHaptics(boolean on) {
        haptics = on;
        Context context = Ttcuz.context();
        if (context != null) context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_HAPTICS, on).apply();
    }

    static void reloadSettings() { mode = -1; haptics = null; performance = null; }

    /** A duration as the person likes it: a third longer when smooth, a third shorter when quick. */
    static long time(long millis) {
        switch (mode()) {
            case 1: return Math.round(millis * 1.3);
            case 2: return Math.round(millis * 0.7);
            default: return millis;
        }
    }

    static boolean enabled(Context context) {
        if (mode() == 3 || performanceMode()) return false;
        try {
            if (Build.VERSION.SDK_INT >= 26) return ValueAnimator.areAnimatorsEnabled();
            return Settings.Global.getFloat(context.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
        } catch (Throwable ignored) {
            return true;
        }
    }
}
