package cat.ttcuz.framework;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.view.View;

final class Dot extends View {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.RectF box = new android.graphics.RectF();
    private final android.graphics.Path outline = new android.graphics.Path();
    private boolean chosen;
    private float selection;
    private android.animation.ValueAnimator animation;

    Dot(Context context, int colour, boolean chosen) {
        super(context);
        this.chosen = chosen;
        selection = chosen ? 1f : 0f;
        fill.setColor(colour);
        ring.setColor(colour);
        ring.setStyle(Paint.Style.STROKE);
        setSelected(chosen);
        setFocusable(true);
    }

    private final Paint customInk = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float customProgress;
    private android.animation.ValueAnimator colourAnimation;

    int colour() {
        return fill.getColor();
    }

    void useSpectrum(int center) {
        fill.setShader(
            new SweepGradient(
                center,
                center,
                new int[] {
                    0xFFE53935,
                    0xFFFFB300,
                    0xFF43A047,
                    0xFF00ACC1,
                    0xFF1E88E5,
                    0xFF8E24AA,
                    0xFFE53935,
                },
                null
            )
        );
    }

    void reveal(int colour) {
        choose(true, true);
        ring.setColor(colour);
        if (!Motion.enabled(getContext())) {
            fill.setShader(null);
            fill.setColor(colour);
            invalidate();
            return;
        }
        if (colourAnimation != null) colourAnimation.cancel();
        customInk.setColor(colour);
        customProgress = 0;
        colourAnimation = android.animation.ValueAnimator.ofFloat(0, 1);
        colourAnimation.setDuration(Motion.MEDIUM);
        colourAnimation.setInterpolator(Motion.OUT);
        colourAnimation.addUpdateListener(a -> {
            customProgress = (Float) a.getAnimatedValue();
            invalidate();
        });
        colourAnimation.addListener(
            new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator a) {
                    fill.setShader(null);
                    fill.setColor(colour);
                    customProgress = 0;
                    colourAnimation = null;
                    invalidate();
                }
            }
        );
        colourAnimation.start();
    }

    void choose(boolean selected, boolean animate) {
        chosen = selected;
        setSelected(selected);
        if (animation != null) {
            animation.cancel();
            animation = null;
        }
        if (!animate || !Motion.enabled(getContext())) {
            selection = chosen ? 1f : 0f;
            invalidate();
            return;
        }
        animation = android.animation.ValueAnimator.ofFloat(selection, chosen ? 1f : 0f);
        animation.setDuration(Motion.MEDIUM - 60L);
        animation.setInterpolator(Motion.OUT);
        animation.addUpdateListener(a -> {
            selection = (Float) a.getAnimatedValue();
            invalidate();
        });
        animation.start();
    }

    @Override
    protected void onMeasure(int width, int height) {
        int size = Math.round(26 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(resolveSize(size, width), resolveSize(size, height));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float unit = getResources().getDisplayMetrics().density;
        float x = getWidth() / 2f,
            y = getHeight() / 2f;
        float radius = Math.max(1f, Math.min(x, y) - 6 * unit);
        // chosen: the circle grows a little, scallops into a cookie and turns one lobe
        Expressive.shape(
            outline,
            Expressive.COOKIE9,
            x,
            y,
            radius + 3 * unit * selection,
            selection,
            (selection * 360f) / Expressive.lobes(Expressive.COOKIE9)
        );
        canvas.drawPath(outline, fill);
        if (customProgress > 0) {
            customInk.setAlpha(Math.round(255 * customProgress));
            canvas.drawPath(outline, customInk);
        }
        if (selection > 0f) {
            int shade = fill.getColor();
            boolean light =
                (Color.red(shade) * 299 + Color.green(shade) * 587 + Color.blue(shade) * 114) /
                    1000 >
                150;
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeCap(Paint.Cap.ROUND);
            ring.setStrokeWidth(2.4f * unit);
            ring.setColor(light ? 0xFF111111 : 0xFFFFFFFF);
            ring.setAlpha(Math.round(255 * Math.min(1f, selection)));
            canvas.drawLine(x - 5 * unit, y + 0.5f * unit, x - 1.5f * unit, y + 4 * unit, ring);
            canvas.drawLine(x - 1.5f * unit, y + 4 * unit, x + 5.5f * unit, y - 3.5f * unit, ring);
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(
        android.view.accessibility.AccessibilityNodeInfo info
    ) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (hasOnClickListeners()) {
            info.setCheckable(true);
            info.setChecked(chosen);
            info.setClassName("android.widget.RadioButton");
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animation != null) {
            animation.cancel();
            animation = null;
        }
        if (colourAnimation != null) {
            colourAnimation.cancel();
            colourAnimation = null;
        }
        selection = chosen ? 1f : 0f;
        super.onDetachedFromWindow();
    }
}
