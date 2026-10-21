package cat.ttcuz.framework;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

final class Check extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    Check(Context context, int colour) {
        super(context);
        paint.setColor(colour);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int size = Math.round(22 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(resolveSize(size, widthSpec), resolveSize(size, heightSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float unit = getWidth() / 22f;
        paint.setStrokeWidth(unit * 2.2f);
        float y = getHeight() / 2f;
        canvas.drawLine(unit * 4, y + unit, unit * 9, y + unit * 5.5f, paint);
        canvas.drawLine(unit * 9, y + unit * 5.5f, unit * 18, y - unit * 5f, paint);
    }
}
