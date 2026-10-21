package cat.ttcuz.framework;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;

/** Sparse decoration below the settings content, with no touch or accessibility targets. */
final class SettingsSnow extends View {

    private static final int COUNT = 48;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] x = new float[COUNT],
        y = new float[COUNT];
    private final float[] radius = new float[COUNT],
        speed = new float[COUNT],
        phase = new float[COUNT];
    private final int[] alpha = new int[COUNT];
    private final float unit;
    private final boolean limited;
    private final int count;
    private final Runnable frame = this::invalidate;
    private boolean active;
    private long previous = -1L;
    private float drift;

    SettingsSnow(Context context) {
        super(context);
        limited = DevicePerformance.limited(context);
        count = limited ? 28 : COUNT;
        unit = getResources().getDisplayMetrics().density;
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        paint.setColor(Color.WHITE);
        java.util.Random random = new java.util.Random(0x54544355L);
        for (int i = 0; i < count; i++) {
            x[i] = random.nextFloat();
            y[i] = random.nextFloat();
            radius[i] = (1.0f + random.nextFloat() * 1.1f) * unit;
            speed[i] = (12f + random.nextFloat() * 14f) * unit;
            phase[i] = random.nextFloat() * 6.2831853f;
            alpha[i] = 80 + random.nextInt(65);
        }
    }

    void setActive(boolean value) {
        active = value;
        previous = -1L;
        removeCallbacks(frame);
        if (value) invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        removeCallbacks(frame);
        int width = getWidth(),
            height = getHeight();
        if (width <= 0 || height <= 0) return;
        boolean moving =
            active && isShown() && getWindowVisibility() == VISIBLE && Motion.enabled(getContext());
        long now = SystemClock.uptimeMillis();
        float seconds =
            moving && previous >= 0L ? Math.max(0f, Math.min(0.05f, (now - previous) / 1000f)) : 0f;
        previous = moving ? now : -1L;
        drift = (drift + seconds * 0.10471976f) % 6.2831853f;
        for (int i = 0; i < count; i++) {
            y[i] = (y[i] + (speed[i] * seconds) / height) % 1f;
            paint.setAlpha(alpha[i]);
            canvas.drawCircle(
                x[i] * width + (float) Math.sin(drift + phase[i]) * 3f * unit,
                y[i] * height,
                radius[i],
                paint
            );
        }
        if (moving) postDelayed(frame, limited ? 50L : 33L);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        previous = -1L;
        removeCallbacks(frame);
        if (visibility == VISIBLE && active) invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(frame);
        previous = -1L;
        super.onDetachedFromWindow();
    }
}
