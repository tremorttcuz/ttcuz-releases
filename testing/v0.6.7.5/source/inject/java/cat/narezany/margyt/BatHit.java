package cat.narezany.margyt;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;

/**
 * A tap on the "ttcuz" heading: a steel bat swings in and hits the word. The sound is played on
 * the frame of the impact. The word is a row of letters and the blow runs along it -- the letter
 * under the bat is squashed at once, its neighbours a moment later and less, and each one then
 * stretches past where it was and settles on its own spring. The bat leaves a faint trail while
 * it is fast, the word crouches a little before the blow, and the page flinches on it.
 *
 * Everything is drawn from views laid over the page, so nothing is clipped by the list and
 * nothing is left behind: the bat, its trail, the ring and the sparks are taken off when it is over.
 */
final class BatHit {

    private BatHit() {}

    // ---- the timeline, in milliseconds from the tap
    private static final float WIND_END = 190f;               // the bat is lifted back
    private static final float HIT = WIND_END + 130f;         // ... and comes down: impact
    private static final float STOP_END = HIT + 55f;          // a held frame, as in a real hit
    private static final float FOLLOW_END = STOP_END + 90f;   // follows through
    private static final float END = FOLLOW_END + 340f;       // and leaves
    /** The sound is started a little early: audio takes a moment to come out of the phone. */
    private static final float SOUND_LEAD = 30f;

    // ---- the swing, in degrees; 0 is the bat pointing right, and it turns clockwise
    private static final float START_ANGLE = -58f, BACK_ANGLE = -80f, HIT_ANGLE = 40f, THROUGH_ANGLE = 52f, EXIT_ANGLE = -40f;

    /** The ghosts of the bat that trail it while it is fast: how many milliseconds behind, and how faint. */
    private static final float[] TRAIL_LAG = {14f, 28f, 42f}, TRAIL_ALPHA = {0.34f, 0.20f, 0.10f};
    /** How the blow travels along the word: milliseconds per dp, and how fast it dies away (dp). */
    private static final float WAVE_DELAY = 1.6f, WAVE_REACH = 80f;
    private static final float SPRING_MS = 1100f;

    private static final String ASSET_BAT = "margyt/bat.png";
    private static final String ASSET_SOUND = "margyt/bat_hit.mp3";

    private static Bitmap bat;
    private static boolean batTried;
    private static SoundPool pool;
    private static int soundId;
    private static volatile boolean soundLoaded;
    private static boolean busy;

    // -------------------------------------------------------------- the sound

    /** Loads the sound ahead of time, so that it is ready on the frame it is wanted. */
    static void prepare(Context context) {
        if (pool != null || context == null) return;
        try {
            Context app = context.getApplicationContext() != null ? context.getApplicationContext() : context;
            File file = soundFile(app);
            if (file == null) return;
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            SoundPool made = new SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attributes).build();
            made.setOnLoadCompleteListener((loaded, id, status) -> soundLoaded = status == 0);
            soundId = made.load(file.getAbsolutePath(), 1);
            pool = made;
        } catch (Throwable error) {
            Diary.note("bat sound: " + error);
        }
    }

    /** The sound alone: the one that every tap on a heading makes. */
    static void play(Context context) {
        try {
            prepare(context);
            if (pool != null && soundLoaded) {
                pool.play(soundId, 1f, 1f, 1, 0, 1f);
                return;
            }
            // not loaded yet: play it once from the file, and let the player go afterwards
            File file = soundFile(context.getApplicationContext() != null ? context.getApplicationContext() : context);
            if (file == null) return;
            final android.media.MediaPlayer player = new android.media.MediaPlayer();
            player.setDataSource(file.getAbsolutePath());
            player.setOnCompletionListener(done -> done.release());
            player.setOnErrorListener((done, what, extra) -> { done.release(); return true; });
            player.setOnPreparedListener(ready -> ready.start());
            player.prepareAsync();
        } catch (Throwable error) {
            Diary.note("bat sound: " + error);
        }
    }

    /** The clip rides inside the apk; it is copied out once, because a stored asset cannot be opened as a path. */
    private static File soundFile(Context context) {
        try {
            File file = new File(context.getFilesDir(), ASSET_SOUND);
            if (file.isFile() && file.length() > 0) return file;
            java.io.InputStream in = context.getAssets().open(ASSET_SOUND);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
            in.close();
            Net.save(file, out.toByteArray());
            return file.isFile() ? file : null;
        } catch (Throwable error) {
            Diary.note("bat sound file: " + error);
            return null;
        }
    }

    private static Bitmap bat(Context context) {
        if (!batTried) {
            batTried = true;
            try {
                java.io.InputStream in = context.getAssets().open(ASSET_BAT);
                bat = BitmapFactory.decodeStream(in);
                in.close();
            } catch (Throwable error) {
                Diary.note("bat image: " + error);
            }
        }
        return bat;
    }

    // -------------------------------------------------------------- the swing

    static boolean busy() { return busy; }

    /**
     * Swings the bat at {@code target}; the app hits the "ttcuz" heading itself. With no
     * target the heading takes the blow. Returns false when a swing is
     * already under way or the page cannot be drawn on, and the caller then does the plain tap.
     */
    static boolean swing(final View heading, View target) {
        final View victim = target != null ? target : heading;
        final Activity activity = activityOf(heading.getContext());
        if (busy || activity == null || victim.getWidth() <= 0 || victim.getHeight() <= 0) return false;
        final ViewGroup root = (ViewGroup) activity.findViewById(android.R.id.content);
        if (!(root instanceof FrameLayout)) return false;
        final FrameLayout frame = (FrameLayout) root;
        busy = true;
        try {
            prepare(activity);
            final float density = activity.getResources().getDisplayMetrics().density;
            final float screen = activity.getResources().getDisplayMetrics().widthPixels;

            int[] at = new int[2], base = new int[2];
            victim.getLocationInWindow(at);
            frame.getLocationInWindow(base);
            final float hitX = at[0] - base[0] + victim.getWidth() * 0.5f;
            final float hitY = at[1] - base[1] + victim.getHeight() * 0.32f;
            // a target on the left half is struck from the right, so the swing stays on the screen
            final boolean flip = hitX < screen * 0.5f;
            final float side = flip ? -1f : 1f;

            final View page = frame.getChildCount() > 0 ? frame.getChildAt(0) : null;
            final ImageView[] ghosts = new ImageView[TRAIL_LAG.length];

            // ---- the bat: its handle is the pivot, and the angle at impact decides where the pivot is
            final Bitmap picture = bat(activity);
            final ImageView batView;
            final float pivotX, pivotY, handleX, handleY;
            if (picture != null) {
                float length = Math.min(300f * density, screen * 0.85f);
                float thick = length * picture.getHeight() / picture.getWidth();
                batView = new ImageView(activity);
                Bitmap shown = picture;
                if (flip) {   // the mirrored bat, so that its handle is on the right
                    Matrix mirror = new Matrix();
                    mirror.preScale(-1f, 1f);
                    shown = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), mirror, true);
                }
                batView.setImageBitmap(shown);
                batView.setScaleType(ImageView.ScaleType.FIT_XY);
                batView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                batView.setAlpha(0f);
                pivotX = flip ? length * 0.97f : length * 0.03f;
                pivotY = thick * 0.5f;
                float reach = length * 0.72f - length * 0.03f;   // from the handle to the part of the barrel that lands
                double angle = Math.toRadians(HIT_ANGLE);
                handleX = hitX - side * (float) (reach * Math.cos(angle));
                handleY = hitY - (float) (reach * Math.sin(angle));
                // the trail goes under the bat itself
                for (int g = 0; g < ghosts.length; g++) {
                    ImageView ghost = new ImageView(activity);
                    ghost.setImageBitmap(shown);
                    ghost.setScaleType(ImageView.ScaleType.FIT_XY);
                    ghost.setAlpha(0f);
                    frame.addView(ghost, new FrameLayout.LayoutParams(Math.round(length), Math.round(thick)));
                    ghost.setPivotX(pivotX);
                    ghost.setPivotY(pivotY);
                    ghosts[g] = ghost;
                }
                frame.addView(batView, new FrameLayout.LayoutParams(Math.round(length), Math.round(thick)));
                batView.setPivotX(pivotX);
                batView.setPivotY(pivotY);
                batView.setTranslationX(handleX - pivotX);
                batView.setTranslationY(handleY - pivotY);
                batView.setRotation(side * START_ANGLE);
            } else {
                batView = null;
                pivotX = pivotY = handleX = handleY = 0f;
            }

            // ---- the word: each letter takes the blow on its own, a moment after its neighbour
            final View[] letters = lettersOf(victim);
            final float[] amplitude = new float[letters.length], delay = new float[letters.length];
            for (int i = 0; i < letters.length; i++) {
                int[] where = new int[2];
                letters[i].getLocationInWindow(where);
                float centre = where[0] - base[0] + letters[i].getWidth() * 0.5f;
                float away = Math.abs(centre - hitX) / density;
                amplitude[i] = (float) Math.exp(-away / WAVE_REACH);
                delay[i] = away * WAVE_DELAY;
                letters[i].setPivotX(letters[i].getWidth() * 0.5f);
                letters[i].setPivotY(letters[i].getHeight() * 0.85f);
            }
            victim.setPivotX(victim.getWidth() * 0.5f);
            victim.setPivotY(victim.getHeight() * 0.85f);

            final float[] pose = new float[3];
            final boolean[] sounded = {false}, struck = {false};
            final ValueAnimator clock = ValueAnimator.ofFloat(0f, END);
            clock.setDuration((long) END);
            clock.setInterpolator(new LinearInterpolator());
            clock.addUpdateListener(a -> {
                float t = (Float) a.getAnimatedValue();
                if (!sounded[0] && t >= HIT - SOUND_LEAD) {
                    sounded[0] = true;
                    play(activity);
                }
                if (!struck[0] && t >= HIT) {
                    struck[0] = true;
                    victim.setScaleY(1f);
                    victim.setTranslationY(0f);
                    flash(frame, hitX, hitY, density, side);
                }

                // before the blow the word crouches a little, as if it knew
                if (t < HIT) {
                    float q = smooth(clamp(t / WIND_END));
                    victim.setTranslationY(2f * density * q);
                    victim.setScaleY(1f - 0.05f * q);
                } else {
                    // the letters on their springs: squashed at once, then stretched past rest, then settling
                    for (int i = 0; i < letters.length; i++) {
                        float u = t - HIT - delay[i];
                        if (u < 0f) continue;
                        float p = clamp(u / SPRING_MS);
                        float s = (float) (Math.exp(-4.6 * p) * Math.cos(2 * Math.PI * 2.6 * p)) * amplitude[i];
                        View letter = letters[i];
                        letter.setScaleY(1f - 0.5f * s);
                        letter.setScaleX(1f + 0.22f * s);
                        letter.setTranslationX(-7f * density * s * side);
                        letter.setTranslationY(13f * density * s);
                        letter.setRotation(-5f * s * side);
                    }
                    // the whole page flinches and settles
                    if (page != null) {
                        float u = t - HIT, fade = (float) Math.exp(-u / 70f);
                        boolean on = u < 260f;
                        page.setTranslationX(on ? (float) Math.sin(u * 0.9f) * 2.2f * density * fade * side : 0f);
                        page.setTranslationY(on ? (float) Math.cos(u * 0.7f) * 1.6f * density * fade : 0f);
                    }
                }

                if (batView == null) return;
                pose(t, density, pose);
                batView.setRotation(side * pose[0]);
                batView.setAlpha(pose[2]);
                batView.setTranslationX(handleX - pivotX + pose[1]);
                batView.setTranslationY(handleY - pivotY - pose[1] * 0.5f);

                // the trail: only while the bat is fast, each ghost a little behind the one before
                float fast = clamp((t - (WIND_END - 30f)) / 40f) * (t < HIT ? 1f : 1f - clamp((t - HIT) / 60f));
                for (int g = 0; g < ghosts.length; g++) {
                    if (ghosts[g] == null) continue;
                    float[] late = new float[3];
                    pose(Math.max(0f, t - TRAIL_LAG[g]), density, late);
                    ghosts[g].setRotation(side * late[0]);
                    ghosts[g].setAlpha(TRAIL_ALPHA[g] * fast);
                    ghosts[g].setTranslationX(handleX - pivotX + late[1]);
                    ghosts[g].setTranslationY(handleY - pivotY - late[1] * 0.5f);
                }
            });
            clock.addListener(new AnimatorListenerAdapter() {
                private boolean done;
                @Override public void onAnimationEnd(Animator animation) { finish(); }
                @Override public void onAnimationCancel(Animator animation) { finish(); }
                private void finish() {
                    if (done) return;
                    done = true;
                    busy = false;
                    try {
                        if (batView != null) frame.removeView(batView);
                        for (ImageView ghost : ghosts) if (ghost != null) frame.removeView(ghost);
                    } catch (Throwable ignored) { }
                    for (View letter : letters) {
                        letter.setScaleX(1f); letter.setScaleY(1f);
                        letter.setTranslationX(0f); letter.setTranslationY(0f);
                        letter.setRotation(0f);
                    }
                    victim.setScaleY(1f);
                    victim.setTranslationY(0f);
                    if (page != null) { page.setTranslationX(0f); page.setTranslationY(0f); }
                }
            });
            clock.start();
            return true;
        } catch (Throwable error) {
            busy = false;
            Diary.note("bat: " + error);
            return false;
        }
    }

    // ------------------------------------------------------------- the impact

    /** A ring that opens from the point of impact, and a few sparks thrown away from it. */
    private static void flash(final FrameLayout frame, float x, float y, float density, float side) {
        try {
            int ringSize = Math.round(46f * density);
            final View ring = new View(frame.getContext());
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(0x00FFFFFF);
            circle.setStroke(Math.max(1, Math.round(2.5f * density)), 0xFFFFFFFF);
            ring.setBackground(circle);
            frame.addView(ring, new FrameLayout.LayoutParams(ringSize, ringSize));
            ring.setTranslationX(x - ringSize / 2f);
            ring.setTranslationY(y - ringSize / 2f);
            ring.setScaleX(0.25f);
            ring.setScaleY(0.25f);
            ring.setAlpha(0.95f);
            ring.animate().scaleX(1.5f).scaleY(1.5f).alpha(0f).setDuration(320)
                    .setInterpolator(new DecelerateInterpolator(1.6f))
                    .withEndAction(() -> { try { frame.removeView(ring); } catch (Throwable ignored) { } }).start();

            // thrown the way the blow goes: down and to the left
            final int sparks = 8;
            for (int i = 0; i < sparks; i++) {
                final View spark = new View(frame.getContext());
                GradientDrawable dot = new GradientDrawable();
                dot.setShape(GradientDrawable.OVAL);
                dot.setColor(0xFFFFFFFF);
                spark.setBackground(dot);
                int size = Math.round((2.2f + (i % 3) * 0.9f) * density);
                frame.addView(spark, new FrameLayout.LayoutParams(size, size));
                spark.setTranslationX(x - size / 2f);
                spark.setTranslationY(y - size / 2f);
                double direction = Math.toRadians((side > 0 ? 140 : 40) - side * (i - sparks / 2f) * 26);
                float distance = (26f + (i * 7) % 22) * density;
                spark.animate()
                        .translationXBy((float) Math.cos(direction) * distance)
                        .translationYBy((float) Math.sin(direction) * distance)
                        .alpha(0f).setDuration(380 + (i % 3) * 40)
                        .setInterpolator(new DecelerateInterpolator(1.8f))
                        .withEndAction(() -> { try { frame.removeView(spark); } catch (Throwable ignored) { } }).start();
            }
        } catch (Throwable error) {
            Diary.note("bat flash: " + error);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static Activity activityOf(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /** The bat at time t: angle in degrees, a little shake in pixels, and how visible it is. */
    private static void pose(float t, float density, float[] out) {
        float angle, shake = 0f, visible = Math.min(1f, t / 90f);
        if (t < WIND_END) {
            float p = t / WIND_END;
            angle = lerp(START_ANGLE, BACK_ANGLE, 1f - (1f - p) * (1f - p));
        } else if (t < HIT) {
            float p = (t - WIND_END) / (HIT - WIND_END);
            angle = lerp(BACK_ANGLE, HIT_ANGLE, p * p);
        } else if (t < STOP_END) {
            angle = HIT_ANGLE;
            shake = (float) Math.sin(t * 1.1f) * 1.6f * density * (1f - (t - HIT) / (STOP_END - HIT));
        } else if (t < FOLLOW_END) {
            float p = (t - STOP_END) / (FOLLOW_END - STOP_END);
            angle = lerp(HIT_ANGLE, THROUGH_ANGLE, 1f - (1f - p) * (1f - p));
        } else {
            float p = (t - FOLLOW_END) / (END - FOLLOW_END);
            angle = lerp(THROUGH_ANGLE, EXIT_ANGLE, p * p * (3f - 2f * p));
            visible = 1f - clamp((p - 0.45f) / 0.55f);
        }
        out[0] = angle;
        out[1] = shake;
        out[2] = visible;
    }

    /** The letters of a word that is a row of views; a single view is its own only letter. */
    private static View[] lettersOf(View word) {
        if (word instanceof ViewGroup && ((ViewGroup) word).getChildCount() > 1) {
            ViewGroup row = (ViewGroup) word;
            View[] letters = new View[row.getChildCount()];
            for (int i = 0; i < letters.length; i++) letters[i] = row.getChildAt(i);
            return letters;
        }
        return new View[]{word};
    }

    private static float smooth(float p) { return p * p * (3f - 2f * p); }

    private static float lerp(float from, float to, float p) { return from + (to - from) * p; }

    private static float clamp(float value) { return value < 0f ? 0f : (value > 1f ? 1f : value); }
}
