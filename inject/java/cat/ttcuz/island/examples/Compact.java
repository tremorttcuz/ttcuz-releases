// @name Три кнопки
// @author ttcuz
package cat.ttcuz.island.examples;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import cat.ttcuz.island.Bar;
import cat.ttcuz.island.Island;

/**
 * Written from nothing, straight on Island: a narrow floating bar with three buttons (Home, "+", Profile) where it
 * wants them, a dot under the chosen one that bounces over, and its own taps. Use it to see the whole API at work.
 */
public class Compact extends Island {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF pill = new RectF();
    /** Where each role stands, as a fraction of the bar's width; -1: not on this bar. */
    private static final float[] PLACE = {.18f, -1f, .5f, -1f, .82f};
    private float dotX = -1, fromX, toX;
    private long movedAt;
    private int was = -2;

    @Override public void layout(Bar bar) {
        float h = bar.dp(56), bottom = bar.height - bar.dp(10);
        bar.body.set(bar.dp(70), bottom - h, bar.width - bar.dp(70), bottom);
        bar.radius = h / 2f;
        for (int i = 0; i < 5; i++) {
            float place = i < bar.count && bar.role[i] >= 0 ? PLACE[bar.role[i]] : -1f;
            if (place < 0) { bar.hit[i].setEmpty(); bar.x[i] = bar.y[i] = -10000f; continue; }
            float x = bar.body.left + bar.body.width() * place;
            bar.x[i] = x;
            bar.y[i] = bar.body.centerY();
            bar.hit[i].set(x - bar.body.width() / 6f, 0, x + bar.body.width() / 6f, bar.height);
        }
        // the dot: a bounce from where it was to the chosen button
        int chosen = bar.selected;
        float target = chosen >= 0 && chosen != bar.create && bar.hit[chosen].width() > 0 ? bar.x[chosen] : dotX;
        if (chosen != was) { fromX = dotX < 0 ? target : dotX; toX = target; movedAt = bar.time; was = chosen; }
        float t = Math.min(1f, (bar.time - movedAt) / 520f);
        dotX = fromX + (toX - fromX) * bounce(t);
        bar.animate(t < 1f);
    }

    private static float bounce(float x) {
        float n = 7.5625f, d = 2.75f;
        if (x < 1 / d) return n * x * x;
        if (x < 2 / d) { x -= 1.5f / d; return n * x * x + .75f; }
        if (x < 2.5 / d) { x -= 2.25f / d; return n * x * x + .9375f; }
        x -= 2.625f / d; return n * x * x + .984375f;
    }

    @Override public void draw(Canvas canvas, Bar bar) {
        // the bar
        paint.setColor(Bar.fade(bar.card, .92f));
        canvas.drawRoundRect(bar.body, bar.radius, bar.radius, paint);
        // the buttons
        float size = bar.dp(26);
        for (int i = 0; i < bar.count; i++) {
            if (bar.hit[i].width() <= 0 || i == bar.create) continue;
            boolean on = i == bar.selected;
            paint.setColor(on ? bar.accent : bar.muted);
            float s = size * (bar.pressed[i] ? .88f : 1f);
            if (!bar.picture[i]) bar.drawSymbol(canvas, on ? bar.iconFilled[i] : bar.icon[i], bar.x[i], bar.y[i], s, paint);
        }
        // the "+" as a small capsule
        if (bar.create >= 0 && bar.hit[bar.create].width() > 0) {
            float cx = bar.x[bar.create], cy = bar.y[bar.create], w = bar.dp(54), h = bar.dp(38);
            pill.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
            paint.setColor(bar.accent);
            canvas.drawRoundRect(pill, h / 2f, h / 2f, paint);
            paint.setColor(Bar.on(bar.accent));
            bar.drawSymbol(canvas, bar.plus, cx, cy, bar.dp(24), paint);
        }
        // the dot
        if (dotX >= 0) {
            paint.setColor(bar.accent);
            canvas.drawCircle(dotX, bar.body.top + bar.body.height() * .86f, bar.dp(3), paint);
        }
    }
}
