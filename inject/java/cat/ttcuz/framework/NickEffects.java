package cat.ttcuz.framework;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.style.ReplacementSpan;
import android.view.View;
import android.widget.TextView;

/**
 * What a ttcuz nickname can carry besides its colours, shown to everybody with the mod:
 *  - a tag after it, a small pill drawn by the mod (not text: it cannot be copied with the name, and the server makes
 *    sure only one account holds it);
 *  - particles floating over it (sparkles, snow, hearts, stars);
 *  - an entrance when the name comes on screen (fade, rise, pop, typing).
 *
 * All of it is drawn, never laid out, and nothing runs while it is off screen: the particles ask for their next frame
 * only from draw(), and the entrance is one short animation per name and view.
 */
final class NickEffects {
    private NickEffects() {}

    /** The placeholder the tag pill is drawn over; a thin space keeps it apart from the name. */
    static final String TAG_TEXT = " ￼";

    // ------------------------------------------------------------------ the tag

    /** A rounded pill with the tag in it, in the owner's own gradient. */
    static final class TagSpan extends ReplacementSpan {
        final String tag;
        final int[] colours;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), words = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF box = new RectF();
        private float width, size = -1;
        private Shader shader;

        /** The view the name is in, for a rare tag's stars to ask for their next frame. */
        java.lang.ref.WeakReference<View> owner = new java.lang.ref.WeakReference<>(null);
        private final Paint star = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path starShape = new Path();

        /** A tag of three characters or fewer is rare, as Telegram's short names are: stars come off it. */
        static boolean rare(String tag) { return tag != null && tag.codePointCount(0, tag.length()) <= 3; }

        /** The tag's number: the same on every phone, from the tag itself. */
        static String id(String tag) {
            java.util.zip.CRC32 crc = new java.util.zip.CRC32();
            crc.update(tag.toLowerCase(java.util.Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return String.format(java.util.Locale.ROOT, "#%06d", crc.getValue() % 1_000_000L);
        }

        TagSpan(String tag, int[] colours, View view) {
            this(tag, colours);
            owner = new java.lang.ref.WeakReference<>(view);
            if (view != null && rare(tag)) view.post(() -> Particles.unclipView(view));
        }

        TagSpan(String tag, int[] colours) {
            this.tag = tag;
            this.colours = colours == null || colours.length == 0 ? new int[]{Accent.colour(), Accent.colour()} : colours.length == 1 ? new int[]{colours[0], colours[0]} : colours;
        }

        private void measure(Paint paint) {
            if (size == paint.getTextSize()) return;
            size = paint.getTextSize();
            words.set(paint);
            words.setShader(null);
            words.setTextSize(size * .66f);
            words.setFakeBoldText(true);
            words.setLetterSpacing(.04f);
            width = words.measureText(tag) + size * .7f;
            shader = null;
        }

        @Override public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            measure(paint);
            if (fm != null) paint.getFontMetricsInt(fm);
            return Math.round(width);
        }

        @Override public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
            measure(paint);
            Paint.FontMetrics m = paint.getFontMetrics();
            float h = size * .95f, cy = y + (m.ascent + m.descent) / 2f;
            box.set(x + size * .08f, cy - h / 2f, x + width - size * .08f, cy + h / 2f);
            if (shader == null) shader = new LinearGradient(box.left, 0, box.right, 0, colours, null, Shader.TileMode.CLAMP);
            fill.setShader(shader);
            fill.setAlpha(paint.getAlpha());
            canvas.drawRoundRect(box, h / 2f, h / 2f, fill);
            // dark words on a light pill, light on a dark one
            words.setColor(light(colours[0]) && light(colours[colours.length - 1]) ? 0xFF111111 : 0xFFFFFFFF);
            words.setAlpha(paint.getAlpha());
            Paint.FontMetrics wm = words.getFontMetrics();
            canvas.drawText(tag, box.centerX() - words.measureText(tag) / 2f, cy - (wm.ascent + wm.descent) / 2f, words);
            if (rare(tag)) stars(canvas, paint.getAlpha());
        }

        /** Small four-point stars that rise off the pill and fade, in the tag's own colours. */
        private void stars(Canvas canvas, int alpha) {
            long now = SystemClock.uptimeMillis();
            float h = box.height();
            for (int i = 0; i < 7; i++) {
                float seed = (i * 0.6180339f) % 1f;
                float period = 1400f + seed * 900f;
                float t = ((now + i * 377L) % (long) period) / period;
                double angle = -Math.PI / 2 + (seed - .5f) * Math.PI * 1.5;
                float sx = box.left + box.width() * ((i + .5f) / 7f);
                float sy = box.centerY();
                float travel = h * (.4f + t * 1.1f);
                float x = sx + (float) Math.cos(angle) * travel * .6f, y = sy + (float) Math.sin(angle) * travel;
                float life = (float) Math.sin(t * Math.PI);
                float r = h * (.10f + .08f * seed) * (.5f + life * .5f);
                star.setColor(i % 2 == 0 ? 0xFFFFE082 : colours[i % colours.length]);
                star.setAlpha(Math.round(alpha * .95f * life));
                starShape.reset();
                starShape.moveTo(x, y - r * 2); starShape.quadTo(x, y, x + r * 2, y); starShape.quadTo(x, y, x, y + r * 2);
                starShape.quadTo(x, y, x - r * 2, y); starShape.quadTo(x, y, x, y - r * 2);
                canvas.drawPath(starShape, star);
            }
            View v = owner.get();
            if (v != null && v.isAttachedToWindow()) v.postInvalidateDelayed(33);
        }

        /** The tag alone, large, for its own window. */
        android.graphics.drawable.Drawable large(View view) {
            float d = view.getResources().getDisplayMetrics().density;
            Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
            base.setTextSize(34 * d);
            measure(base);
            int w = Math.round(width + 24 * d), hgt = Math.round(size * 2.2f);
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(w, hgt, android.graphics.Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bitmap);
            Paint.FontMetrics m = base.getFontMetrics();
            int baseline = Math.round(hgt / 2f - (m.ascent + m.descent) / 2f);
            shader = null;
            draw(c, TAG_TEXT, 0, 1, 12 * d, 0, baseline, hgt, base);
            shader = null;
            size = -1;
            return new android.graphics.drawable.BitmapDrawable(view.getResources(), bitmap);
        }

        private static boolean light(int c) {
            return ((c >> 16 & 255) * 299 + (c >> 8 & 255) * 587 + (c & 255) * 114) / 1000 > 165;
        }
    }

    // ------------------------------------------------------------------ particles

    private static final int PARTICLES_TAG = 0x7e0c0390;

    /** Puts the right particles over a nickname view, or takes them away when this name has none. */
    static void particles(TextView view, int kind) { particles(view, kind, ""); }

    static void particles(TextView view, int kind, String recipe) {
        Object old = view.getTag(PARTICLES_TAG);
        Particles current = old instanceof Particles ? (Particles) old : null;
        String made = kind == ProfileStyle.CUSTOM && recipe != null ? recipe : "";
        if (current != null && current.kind == kind && current.made.equals(made)) return;
        if (current != null) { view.getOverlay().remove(current); view.setTag(PARTICLES_TAG, null); }
        if (kind <= 0 || !Motion.enabled(view.getContext())) return;
        Particles fresh = new Particles(view, kind, ParticleRecipe.parse(made), made);
        view.getOverlay().add(fresh);
        fresh.hold();
        view.setTag(PARTICLES_TAG, fresh);
    }

    /** A few particles drifting over the view; one short loop per particle, nothing kept per frame. */
    static final class Particles extends Drawable implements Runnable {
        final int kind;
        /** The person's own effect, when kind is CUSTOM; and its JSON, to know when it changed. */
        final ParticleRecipe recipe;
        final String made;
        private final java.lang.ref.WeakReference<TextView> owner;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path shape = new Path();
        private final float[] seeds = new float[24 * 3];
        private boolean scheduled, unclipped;
        private Callback held;

        /**
         * The particles fly past the name's edges; the rows and cards the name sits in cut whatever their children draw
         * outside them, so the ones up to the screen's content are told not to (and not to cut at their padding).
         */
        static void unclipView(View view) { unclip(view); }

        private static boolean scrolls(android.view.ViewGroup group) {
            if (group instanceof android.widget.AbsListView || group instanceof android.widget.ScrollView
                    || group instanceof android.widget.HorizontalScrollView) return true;
            for (Class<?> c = group.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                String name = c.getName();
                if (name.endsWith(".RecyclerView") || name.endsWith(".NestedScrollView") || name.endsWith(".ViewPager")
                        || name.endsWith(".ViewPager2")) return true;
            }
            return false;
        }

        private static void unclip(View view) {
            android.view.ViewParent up = view.getParent();
            for (int depth = 0; depth < 12 && up instanceof android.view.ViewGroup; depth++) {
                android.view.ViewGroup group = (android.view.ViewGroup) up;
                if (group.getId() == android.R.id.content) break;
                // a list must keep cutting its rows at its edges, or a row scrolled past the top shows over what is
                // above the list: the unclipping stops at the row
                if (scrolls(group)) break;
                if (group.getClipChildren()) group.setClipChildren(false);
                if (group.getClipToPadding()) group.setClipToPadding(false);
                up = group.getParent();
            }
        }

        Particles(TextView view, int kind, ParticleRecipe recipe, String made) {
            this.kind = recipe == null && kind == ProfileStyle.CUSTOM ? ProfileStyle.SPARKLES : kind;
            this.recipe = this.kind == ProfileStyle.CUSTOM ? recipe : null;
            this.made = made == null ? "" : made;
            owner = new java.lang.ref.WeakReference<>(view);
            java.util.Random random = new java.util.Random(System.identityHashCode(view));
            for (int i = 0; i < seeds.length; i++) seeds[i] = random.nextFloat();
        }

        /** The overlay sets its own callback; a strong one to the view keeps the frames coming after that. */
        void hold() {
            held = new Callback() {
                @Override public void invalidateDrawable(Drawable who) { View v = owner.get(); if (v != null) v.invalidate(); }
                @Override public void scheduleDrawable(Drawable who, Runnable what, long when) { View v = owner.get(); if (v != null) v.postDelayed(what, Math.max(0, when - SystemClock.uptimeMillis())); }
                @Override public void unscheduleDrawable(Drawable who, Runnable what) { View v = owner.get(); if (v != null) v.removeCallbacks(what); }
            };
            setCallback(held);
        }

        @Override public void draw(Canvas canvas) {
            TextView view = owner.get();
            if (view == null) return;
            int w = view.getWidth(), h = view.getHeight();
            if (w <= 0 || h <= 0) return;
            if (!unclipped) { unclipped = true; unclip(view); }
            float d = view.getResources().getDisplayMetrics().density;
            long now = SystemClock.uptimeMillis();
            int colour = view.getCurrentTextColor();
            if (recipe != null) {
                recipe.draw(canvas, w, h, d, now, colour, seeds);
                if (!scheduled) { scheduled = true; scheduleSelf(this, now + 33); }
                return;
            }
            int count = Math.max(4, Math.min(10, w / Math.round(18 * d)));
            for (int i = 0; i < count; i++) {
                float sx = seeds[i * 3], sy = seeds[i * 3 + 1], sp = seeds[i * 3 + 2];
                float period = 2400f + sp * 2200f;
                float t = ((now + sp * 9000f) % period) / period;
                float x, y, r = (1.2f + sp * 1.6f) * d, alpha;
                if (kind == ProfileStyle.SNOW) { x = sx * w + (float) Math.sin((t + sx) * 6.28) * 3 * d; y = t * h; alpha = 1f - t * .6f; }
                else if (kind == ProfileStyle.HEARTS) { x = sx * w; y = h - t * h; alpha = (float) Math.sin(t * Math.PI); r *= 1.5f; }
                else { x = sx * w; y = sy * h; alpha = (float) Math.max(0, Math.sin(t * Math.PI * 2)); }
                paint.setColor(kind == ProfileStyle.SNOW ? 0xFFFFFFFF : kind == ProfileStyle.HEARTS ? 0xFFFF4D79 : kind == ProfileStyle.STARS ? 0xFFFFD54F : colour);
                paint.setAlpha(Math.round(220 * alpha));
                if (kind == ProfileStyle.HEARTS) heart(canvas, x, y, r);
                else if (kind == ProfileStyle.STARS || kind == ProfileStyle.SPARKLES) sparkle(canvas, x, y, r * (kind == ProfileStyle.STARS ? 1.6f : 1.2f) * (.6f + alpha * .4f));
                else canvas.drawCircle(x, y, r, paint);
            }
            // about 30 frames a second, and only while drawn: off screen nothing is asked for
            if (!scheduled) { scheduled = true; scheduleSelf(this, now + 33); }
        }

        private void sparkle(Canvas c, float x, float y, float r) {
            shape.reset();
            shape.moveTo(x, y - r * 2); shape.quadTo(x, y, x + r * 2, y); shape.quadTo(x, y, x, y + r * 2); shape.quadTo(x, y, x - r * 2, y); shape.quadTo(x, y, x, y - r * 2);
            c.drawPath(shape, paint);
        }

        private void heart(Canvas c, float x, float y, float r) {
            shape.reset();
            shape.moveTo(x, y + r);
            shape.cubicTo(x - r * 2, y - r * .2f, x - r, y - r * 1.6f, x, y - r * .5f);
            shape.cubicTo(x + r, y - r * 1.6f, x + r * 2, y - r * .2f, x, y + r);
            c.drawPath(shape, paint);
        }

        @Override public void run() { scheduled = false; invalidateSelf(); }
        @Override public void setAlpha(int a) { }
        @Override public void setColorFilter(ColorFilter f) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    // ------------------------------------------------------------------ entrance

    private static final int SHOWN_TAG = 0x7e0c0391;

    /**
     * The entrance, once for each name a view starts showing (a reused view showing someone new plays it again).
     * The anti-burn sees the alpha change and keeps its own share of it, so a dimmed name stays dimmed after it.
     */
    static void appear(final TextView view, int kind, String who) {
        if (kind <= 0 || who == null || who.equals(view.getTag(SHOWN_TAG)) || !Motion.enabled(view.getContext())) return;
        view.setTag(SHOWN_TAG, who);
        final float d = view.getResources().getDisplayMetrics().density;
        android.animation.ValueAnimator run = android.animation.ValueAnimator.ofFloat(0f, 1f);
        run.setDuration(kind == ProfileStyle.APPEAR_TYPE ? 520L : 380L);
        run.setInterpolator(kind == ProfileStyle.APPEAR_POP ? new android.view.animation.OvershootInterpolator(2.2f) : Motion.OUT);
        final int k = kind;
        run.addUpdateListener(a -> {
            float p = (Float) a.getAnimatedValue();
            switch (k) {
                case ProfileStyle.APPEAR_FADE: view.setAlpha(p); break;
                case ProfileStyle.APPEAR_RISE: view.setAlpha(p); view.setTranslationY((1f - p) * 8 * d); break;
                case ProfileStyle.APPEAR_POP: { float s = .7f + .3f * p; view.setScaleX(s); view.setScaleY(s); break; }
                case ProfileStyle.APPEAR_TYPE: view.setClipBounds(new android.graphics.Rect(0, 0, Math.max(1, Math.round(view.getWidth() * p)), view.getHeight())); break;
                default: break;
            }
        });
        run.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) { if (k == ProfileStyle.APPEAR_FADE || k == ProfileStyle.APPEAR_RISE) view.setAlpha(1f); view.setScaleX(1f); view.setScaleY(1f); view.setTranslationY(0f); view.setClipBounds(null); }
            @Override public void onAnimationCancel(android.animation.Animator a) { onAnimationEnd(a); }
        });
        if (k == ProfileStyle.APPEAR_FADE || k == ProfileStyle.APPEAR_RISE) view.setAlpha(0f);
        view.post(run::start);
    }
}
