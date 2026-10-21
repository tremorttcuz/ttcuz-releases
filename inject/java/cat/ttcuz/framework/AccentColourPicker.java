package cat.ttcuz.framework;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

final class AccentColourPicker extends View {

    interface Listener {
        void changed(int colour);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] hsv = new float[3];
    private final int[] rainbow = {
        Color.RED,
        Color.YELLOW,
        Color.GREEN,
        Color.CYAN,
        Color.BLUE,
        0xFFFF00FF,
        Color.RED,
    };
    private Listener changed;

    AccentColourPicker(Context context, int initial) {
        super(context);
        Color.colorToHSV(initial, hsv);
        setContentDescription(Text.ACCENT_PICKER);
    }

    void onColourChanged(Listener listener) {
        changed = listener;
    }

    int colour() {
        return Color.HSVToColor(hsv);
    }

    void setColour(int colour) {
        Color.colorToHSV(colour, hsv);
        invalidate();
        if (changed != null) changed.changed(colour);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float pad = dp(10);
        float left = pad,
            right = getWidth() - pad,
            top = pad;
        float bottom = getHeight() - dp(42);
        int pureHue = Color.HSVToColor(new float[] { hsv[0], 1f, 1f });
        paint.setShader(
            new LinearGradient(left, top, right, top, Color.WHITE, pureHue, Shader.TileMode.CLAMP)
        );
        canvas.drawRect(left, top, right, bottom, paint);
        paint.setShader(
            new LinearGradient(
                left,
                top,
                left,
                bottom,
                0x00000000,
                0xFF000000,
                Shader.TileMode.CLAMP
            )
        );
        canvas.drawRect(left, top, right, bottom, paint);
        paint.setShader(null);

        float markX = left + hsv[1] * (right - left);
        float markY = top + (1f - hsv[2]) * (bottom - top);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(Color.BLACK);
        canvas.drawCircle(markX, markY, dp(8), paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(markX, markY, dp(6), paint);

        float hueTop = bottom + dp(12),
            hueBottom = getHeight() - dp(8);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(
            new LinearGradient(left, hueTop, right, hueTop, rainbow, null, Shader.TileMode.CLAMP)
        );
        canvas.drawRoundRect(left, hueTop, right, hueBottom, dp(5), dp(5), paint);
        paint.setShader(null);
        float hueX = left + (hsv[0] / 360f) * (right - left);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setColor(Color.WHITE);
        canvas.drawCircle(hueX, (hueTop + hueBottom) / 2f, dp(8), paint);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (
            event.getAction() != android.view.MotionEvent.ACTION_DOWN &&
            event.getAction() != android.view.MotionEvent.ACTION_MOVE &&
            event.getAction() != android.view.MotionEvent.ACTION_UP
        ) return true;
        float pad = dp(10),
            left = pad,
            right = Math.max(left + 1, getWidth() - pad);
        float top = pad,
            bottom = getHeight() - dp(42);
        if (event.getY() <= bottom + dp(5)) {
            hsv[1] = Math.max(0f, Math.min(1f, (event.getX() - left) / (right - left)));
            hsv[2] =
                1f - Math.max(0f, Math.min(1f, (event.getY() - top) / Math.max(1f, bottom - top)));
        } else {
            hsv[0] = Math.max(0f, Math.min(1f, (event.getX() - left) / (right - left))) * 360f;
        }
        invalidate();
        if (changed != null) changed.changed(colour());
        return true;
    }
}
