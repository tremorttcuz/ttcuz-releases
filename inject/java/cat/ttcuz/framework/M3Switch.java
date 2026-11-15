package cat.ttcuz.framework;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * A Material 3 switch, drawn here.
 *
 * The platform's own Switch is the one from 2014 and looks it. Material's is a
 * library this build cannot add -- adding one means adding resources, and
 * adding resources means rewriting a 25 MB resource table -- so the shape is
 * drawn instead: a 44 by 26 filled track, the accent (its gradient) when on,
 * and a plain white thumb of one size, without a check.
 */
public final class M3Switch extends View {
    /** Kept public so a screen that has to change its structure can wait for the motion. */
    public static final long ANIMATION_DURATION_MS = 300L;

    public interface OnChanged {
        void onChanged(boolean checked);
    }

    private static final int TRACK_WIDTH = 44;
    private static final int TRACK_HEIGHT = 26;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();

    // The palette follows the card the switch sits on, not the wallpaper, the
    // system theme or the TikTok accent (some devices feed a white accent back
    // in). On a dark card "on" is a white track; on a light card it is a black
    // one, because a white track on a white card cannot be told from nothing.
    private boolean lightSurface;
    private int trackOff, trackOn, thumbOff, thumbOn, outlineOff;

    {
        palette(false);
    }

    private void palette(boolean light) {
        lightSurface = light;
        if (light) {
            trackOff = 0x14000000; outlineOff = 0xFF8A8A92; thumbOff = 0xFF6E6E76;
            trackOn = 0xFF111111;  thumbOn = 0xFFFFFFFF;
        } else {
            trackOff = 0x22FFFFFF; outlineOff = 0xFF8E8E96; thumbOff = 0xFFB4B4BC;
            trackOn = 0xFFFFFFFF;  thumbOn = 0xFF000000;
        }
    }

    private boolean checked;
    private float position;  // 0 off, 1 on
    private OnChanged listener;
    private ValueAnimator animator;
    private boolean interruptingAnimation;

    public M3Switch(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);
    }

    public void colours(int accent, int off, int surface) {
        // The accent and the OS colours are ignored on purpose; only how light
        // the card is matters.
        float luma = (0.299f * Color.red(surface) + 0.587f * Color.green(surface)
                + 0.114f * Color.blue(surface)) / 255f;
        boolean light = luma > 0.55f;
        if (light != lightSurface) palette(light);
        invalidate();
    }

    public void setChecked(boolean value) {
        setChecked(value, false);
    }

    public void setChecked(boolean value, boolean animate) {
        if (checked == value && (position == (value ? 1f : 0f))) return;
        if (animator != null) {
            // Keep the interpolated position. Snapping to the old or new end
            // point here is visible when someone taps the row twice quickly.
            interruptingAnimation = true;
            animator.cancel();
            animator = null;
            interruptingAnimation = false;
        }
        checked = value;
        if (!animate || !Motion.enabled(getContext())) {
            position = value ? 1f : 0f;
            invalidate();
            return;
        }
        animator = ValueAnimator.ofFloat(position, value ? 1f : 0f);
        animator.setDuration(ANIMATION_DURATION_MS);
        animator.setInterpolator(Motion.OUT);
        animator.addUpdateListener(a -> {
            position = (Float) a.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (!interruptingAnimation && animation == animator) {
                    position = checked ? 1f : 0f;
                    animator = null;
                    invalidate();
                }
            }
        });
        animator.start();
    }

    public boolean isChecked() {
        return checked;
    }

    public void setOnChanged(OnChanged listener) {
        this.listener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                return true;
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                return true;
            case MotionEvent.ACTION_UP:
                boolean inside = event.getX() >= 0 && event.getX() < getWidth()
                        && event.getY() >= 0 && event.getY() < getHeight();
                setPressed(false);
                if (inside) performClick();
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        if (!isEnabled()) return false;
        setChecked(!checked, true);
        if (listener != null) listener.onChanged(checked);
        super.performClick();
        return true;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.CheckBox");
        info.setCheckable(true);
        info.setChecked(checked);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(dp(TRACK_WIDTH), dp(TRACK_HEIGHT));
    }

    @Override
    public void setPressed(boolean pressed) {
        boolean was = isPressed();
        super.setPressed(pressed);
        if (was != pressed) press(pressed ? 1f : 0f);
    }

    /** How far the thumb has grown under the finger: 0 resting, 1 held. */
    private float held;
    private ValueAnimator holding;

    private void press(float to) {
        if (holding != null) holding.cancel();
        if (!Motion.enabled(getContext())) { held = to; invalidate(); return; }
        holding = ValueAnimator.ofFloat(held, to);
        holding.setDuration(140L);
        holding.setInterpolator(Motion.OUT);
        holding.addUpdateListener(a -> { held = (Float) a.getAnimatedValue(); invalidate(); });
        holding.start();
    }

    private android.graphics.Shader track;
    private int trackFrom, trackTo;
    private float trackWidth;

    /**
     * The track when on: the accent gradient when there is one, otherwise the accent running into a lighter
     * shade of itself, so every switch on the page carries the same colour as the rest of the mod.
     */
    private android.graphics.Shader trackShader(float width, int accent) {
        int end = AccentGradient.enabled() ? (AccentGradient.end() & 0xFFFFFF) | 0xFF000000 : blend(accent, 0xFFFFFFFF, 0.28f);
        if (track == null || trackFrom != accent || trackTo != end || trackWidth != width) {
            trackFrom = accent; trackTo = end; trackWidth = width;
            track = new android.graphics.LinearGradient(0, 0, width, 0, accent, end, android.graphics.Shader.TileMode.CLAMP);
        }
        return track;
    }

    private final Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * A small, plain switch rather than Material's: a filled track with no outline -- a faint tone off, the accent
     * (its gradient) on -- and a round white thumb, always the same size, lifted by a soft shadow. Held, the thumb
     * widens a little towards where it is going.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float eased = position * position * (3f - 2f * position);
        int accent = Accent.colour();
        float r = h / 2f;

        box.set(0, 0, w, h);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(lightSurface ? 0x1F000000 : 0x29FFFFFF);
        canvas.drawRoundRect(box, r, r, paint);
        if (eased > 0f) {
            paint.setColor(accent);
            paint.setShader(trackShader(w, accent));
            paint.setAlpha(Math.round(255 * eased));
            canvas.drawRoundRect(box, r, r, paint);
            paint.setShader(null);
        }

        float inset = dp(2.5f), size = h - 2f * inset;
        float grow = dp(5f) * held;
        float left = inset + (w - 2f * inset - size) * eased - grow * eased;
        box.set(left, inset, left + size + grow, inset + size);
        if (shadeFor != size) { shadeFor = size; shade.setShadowLayer(dp(2f), 0f, dp(1f), 0x33000000); }
        int thumb = eased > .5f ? thumbOnTrack() : (lightSurface ? 0xFFFFFFFF : 0xFFE8E8EA);
        shade.setColor(thumb);
        canvas.drawRoundRect(box, size / 2f, size / 2f, shade);
    }

    private float shadeFor = -1f;

    /** White on the accent, unless the accent is so light that white would vanish into it. */
    private int thumbOnTrack() {
        int accent = Accent.colour();
        int luma = (Color.red(accent) * 299 + Color.green(accent) * 587 + Color.blue(accent) * 114) / 1000;
        return luma > 200 ? 0xFF1C1C1E : 0xFFFFFFFF;
    }

    private static int withAlpha(int colour, int alpha) {
        return (colour & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    private static int blend(int from, int to, float amount) {
        float t = Math.max(0f, Math.min(1f, amount));
        return Color.argb(
                Math.round(Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * t),
                Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * t),
                Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * t),
                Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
