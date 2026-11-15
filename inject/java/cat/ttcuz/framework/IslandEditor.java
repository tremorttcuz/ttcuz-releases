package cat.ttcuz.framework;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * The island as it will look, with its buttons: a switch under each one shows or hides it, and holding a button
 * lifts it so it can be carried to another place. What is chosen is saved at once and the island on the main
 * screen follows (IslandTabs).
 */
final class IslandEditor extends View {
    private static final String[] NAMES = {"Главная", "Друзья", null, "Входящие", "Профиль"};

    private final float unit;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG), ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF body = new RectF(), box = new RectF(), stage = new RectF();
    private final Path[] icons = new Path[5];
    private final Path plus;
    private final int text, muted, card;
    private int[] order;
    private int hidden;
    /** Where each button (by role) is drawn now, and where it is going. */
    private final float[] at = new float[5], to = new float[5];
    /** How far each switch is on, 0..1, animated. */
    private final float[] lit = new float[5];
    private int dragged = -1;
    private float dragX, grabOffset, lift;
    /** The carried button's height and where on it the finger took it: it stays under the finger both ways. */
    private float dragY, grabOffsetY;
    private float downX, downY;
    private boolean pressing, carrying;
    private final int slop;
    private ValueAnimator settling;
    private final Runnable hold = this::pickUp;

    IslandEditor(Context context, Skin skin) {
        super(context);
        unit = getResources().getDisplayMetrics().density;
        text = skin.text;
        muted = skin.muted();
        card = skin.card;
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
        for (int role = 0; role < 5; role++) {
            String symbol = NavigationIsland.SYMBOLS[role];
            icons[role] = symbol == null ? null : Symbols.path(symbol + "_fill1");
        }
        plus = Symbols.path("add");
        order = IslandTabs.order();
        hidden = IslandTabs.hidden();
        for (int role = 0; role < 5; role++) lit[role] = (hidden & 1 << role) == 0 ? 1f : 0f;
        ink.setTextAlign(Paint.Align.CENTER);
        ink.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        setContentDescription(Text.t("Кнопки островка"));
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), Math.round(174 * unit));
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        float side = 16 * unit;
        stage.set(side, 8 * unit, w - side, 104 * unit);
        float inset = 10 * unit;
        body.set(stage.left + inset, stage.centerY() - 31 * unit, stage.right - inset, stage.centerY() + 31 * unit);
        for (int i = 0; i < 5; i++) to[order[i]] = slot(i);
        System.arraycopy(to, 0, at, 0, 5);
    }

    /** The centre of a place on the island: five evenly spread, clear of its round ends. */
    private float slot(int place) {
        // as the island spreads its tabs: the outer ones far enough in for their labels, the rest evenly between
        float outer = 0f;
        if (!NavigationIsland.hideLabels()) {
            ink.setTextSize(10.5f * unit * Math.min(1.3f, getResources().getConfiguration().fontScale));
            for (int i : new int[]{0, 4}) { String name = NAMES[order[i]]; if (name != null) outer = Math.max(outer, ink.measureText(Text.t(name))); }
        }
        float inset = Math.max(body.height() * .46f, outer / 2f + 15 * unit);
        float gap = (body.width() - 2f * inset) / 4f;
        return body.left + inset + gap * place;
    }

    private int place(float x) {
        int best = 0;
        float near = Float.MAX_VALUE;
        for (int i = 0; i < 5; i++) {
            float d = Math.abs(slot(i) - x);
            if (d < near) { near = d; best = i; }
        }
        return best;
    }

    private IslandDrop.Painter painter;
    private final Path[] filled = new Path[5];

    /** The capsule of the chosen tab as the island sizes it: its label and 11 dp either side, at most its tab's room. */
    private float capsuleWidth(int role, float gap) {
        float pillHeight = body.height() - 6 * unit, pillWidth = Math.min(pillHeight * 1.15f, gap + 10 * unit);
        float label = NavigationIsland.hideLabels() || NAMES[role] == null ? body.height() * .62f : ink.measureText(Text.t(NAMES[role]));
        return Math.min(gap + 6 * unit, Math.max(pillWidth * .92f, label + 22 * unit));
    }

    @Override protected void onDraw(Canvas canvas) {
        if (body.isEmpty()) return;
        if (painter == null) painter = new IslandDrop.Painter(unit);
        boolean light = Blur.tint() == Blur.TINT_LIGHT, liquid = Blur.style() == Blur.LIQUID;
        int accent = Accent.colour();
        // a little of a feed behind the glass, in the accent
        paint.setShader(new LinearGradient(stage.left, stage.top, stage.right, stage.bottom,
                Expressive.mix(card, accent, .55f), Expressive.mix(card, 0xFF000000, .35f), Shader.TileMode.CLAMP));
        canvas.drawRoundRect(stage, 24 * unit, 24 * unit, paint);
        paint.setShader(null);

        // the body, as the island draws it: smoked or white glass, or the solid surface of the other styles
        float r = body.height() / 2f;
        paint.setColor(liquid ? (light ? 0xE6F2F2F7 : 0xE61C1C1E) : light ? 0xFFF2F2F7 : Expressive.mix(card, 0xFF000000, .25f));
        canvas.drawRoundRect(body, r, r, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, unit));
        paint.setShader(new LinearGradient(0, body.top, 0, body.bottom, new int[]{light ? 0x33000000 : 0x59FFFFFF, light ? 0x14000000 : 0x14FFFFFF, light ? 0x26000000 : 0x2EFFFFFF},
                new float[]{0f, .5f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(body, r, r, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);

        int ink0 = light ? 0xFF1C1C1E : 0xFFFFFFFF;
        float cy = body.centerY(), gap = (slot(4) - slot(0)) / 4f;
        ink.setTextSize(10.5f * unit * Math.min(1.3f, getResources().getConfiguration().fontScale));
        // the chosen tab's capsule sits under Home, as the island shows it when the feed is open
        if (lit[IslandTabs.HOME] > 0f && dragged != IslandTabs.HOME) {
            float w = capsuleWidth(IslandTabs.HOME, gap), h = body.height() - 6 * unit, x = at[IslandTabs.HOME];
            box.set(x - w / 2f, cy - h / 2f, x + w / 2f, cy + h / 2f);
            if (liquid) painter.rest(canvas, box, 1f);
            else { paint.setColor(Expressive.mix(card, accent, .38f)); canvas.drawRoundRect(box, h / 2f, h / 2f, paint); }
        }
        for (int i = 0; i < 5; i++) {
            int role = order[i];
            if (role != dragged) drawButton(canvas, role, at[role], cy, 1f, ink0, accent, light);
        }
        if (dragged >= 0) drawButton(canvas, dragged, at[dragged], carrying ? dragY : cy + (dragY - cy) * lift, 1f + .14f * lift, ink0, accent, light);

        // a switch under each place, for the button standing there
        for (int i = 0; i < 5; i++) drawSwitch(canvas, order[i], dragged == order[i] ? at[order[i]] : slot(i), 128 * unit, accent);
        ink.setTextSize(12 * unit * Math.min(1.3f, getResources().getConfiguration().fontScale));
        ink.setColor(muted);
        canvas.drawText(Text.t("Удерживайте кнопку, чтобы переставить"), getWidth() / 2f, 162 * unit, ink);
    }

    private void drawButton(Canvas canvas, int role, float x, float cy, float scale, int colour, int accent, boolean light) {
        float alpha = .3f + .7f * lit[role];
        int s = canvas.save();
        canvas.translate(x, cy);
        canvas.scale(scale, scale);
        if (role == IslandTabs.CREATE) {
            // the island's own glass "+" button
            int k = canvas.save();
            canvas.translate(-x, -cy);
            painter.button(canvas, x, cy, body.height(), 1f, plus, colour, alpha);
            canvas.restoreToCount(k);
        } else {
            boolean chosen = role == IslandTabs.HOME;
            int tint = chosen ? glassAccent(accent, light) : colour;
            Path icon = chosen ? icons[role] : outlined(role);
            boolean names = !NavigationIsland.hideLabels();
            if (icon != null) {
                int k = canvas.save();
                float size = 23 * unit;
                canvas.translate(-size / 2f, (names ? -6.5f * unit : 0f) - size / 2f);
                canvas.scale(size / 24f, size / 24f);
                paint.setColor(withAlpha(tint, alpha));
                canvas.drawPath(icon, paint);
                canvas.restoreToCount(k);
            }
            if (names) {
                ink.setColor(withAlpha(tint, alpha));
                canvas.drawText(Text.t(NAMES[role]), 0, 16 * unit, ink);
            }
        }
        canvas.restoreToCount(s);
    }

    /** The tab's symbol as the island draws it when it is not chosen: outlined. */
    private Path outlined(int role) {
        if (filled[role] == null) {
            String symbol = NavigationIsland.SYMBOLS[role];
            filled[role] = symbol == null ? null : Symbols.path(symbol);
        }
        return filled[role] != null ? filled[role] : icons[role];
    }

    /** A small switch; Home and Profile always stay, so theirs is on and faded. */
    private void drawSwitch(Canvas canvas, int role, float x, float cy, int accent) {
        float w = 34 * unit, h = 20 * unit, on = lit[role];
        boolean fixed = !IslandTabs.canHide(role);
        float fade = fixed ? .38f : 1f;
        box.set(x - w / 2f, cy - h / 2f, x + w / 2f, cy + h / 2f);
        if (on < 1f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f * unit);
            paint.setColor(withAlpha(muted, (1f - on) * fade));
            box.inset(.75f * unit, .75f * unit);
            canvas.drawRoundRect(box, h / 2f, h / 2f, paint);
            box.inset(-.75f * unit, -.75f * unit);
            paint.setStyle(Paint.Style.FILL);
        }
        if (on > 0f) {
            int end = AccentGradient.enabled() ? (AccentGradient.end() & 0xFFFFFF) | 0xFF000000 : Expressive.mix(accent, 0xFFFFFFFF, .28f);
            paint.setShader(new LinearGradient(box.left, 0, box.right, 0, accent, end, Shader.TileMode.CLAMP));
            paint.setAlpha(Math.round(255 * on * fade));
            canvas.drawRoundRect(box, h / 2f, h / 2f, paint);
            paint.setShader(null);
        }
        float size = 10 * unit + 4 * unit * on, cx = box.left + h / 2f + (w - h) * on;
        paint.setColor(withAlpha(Expressive.mix(muted, 0xFFFFFFFF, on), fade));
        canvas.drawCircle(cx, cy, size / 2f, paint);
    }

    private static int glassAccent(int accent, boolean light) {
        if (light) return Expressive.mix(accent, 0xFF000000, .2f);
        int luma = (android.graphics.Color.red(accent) * 299 + android.graphics.Color.green(accent) * 587
                + android.graphics.Color.blue(accent) * 114) / 1000;
        return luma < 90 ? Expressive.mix(accent, 0xFFFFFFFF, .35f) : accent;
    }

    private static int withAlpha(int colour, float alpha) {
        int a = Math.round(((colour >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, alpha)));
        return (colour & 0xFFFFFF) | a << 24;
    }

    // ------------------------------------------------------------------ touch

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX(); downY = e.getY(); pressing = true;
                if (body.contains(downX, downY) || Math.abs(downY - body.centerY()) < body.height()) {
                    // the page must not take the finger while it decides between a hold and a drag
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    postDelayed(hold, ViewConfiguration.getLongPressTimeout() * 2 / 3);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (carrying) {
                    // exactly under the finger: no clamp to the places, no lift that moves it off the touch
                    dragX = Math.max(body.left, Math.min(body.right, e.getX() - grabOffset));
                    dragY = Math.max(stage.top, Math.min(stage.bottom, e.getY() - grabOffsetY));
                    at[dragged] = dragX;
                    reorder(place(dragX));
                    invalidate();
                    return true;
                }
                if (pressing) {
                    float dx = Math.abs(e.getX() - downX), dy = Math.abs(e.getY() - downY);
                    // sideways on a button: carried at once, no need to wait for the hold
                    if (dx > slop && dx > dy * 1.3f && Math.abs(downY - body.centerY()) < body.height()) { pickUp(); return true; }
                    // up or down: it was a scroll, the page has it back
                    if (dy > slop && dy >= dx) {
                        pressing = false;
                        removeCallbacks(hold);
                        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                removeCallbacks(hold);
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                if (carrying) { drop(); return true; }
                if (pressing && downY > body.bottom + 4 * unit && downY < 146 * unit) toggle(place(downX));
                pressing = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(hold);
                pressing = false;
                if (carrying) drop();
                return true;
            default:
                return true;
        }
    }

    private void pickUp() {
        if (!pressing) return;
        pressing = false;
        int place = place(downX);
        dragged = order[place];
        grabOffset = downX - slot(place);
        grabOffsetY = downY - body.centerY();
        dragX = slot(place);
        dragY = body.centerY();
        carrying = true;
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        animateLift(1f);
    }

    /** The carried button has crossed into another place: the others step aside. */
    private void reorder(int place) {
        int from = -1;
        for (int i = 0; i < 5; i++) if (order[i] == dragged) from = i;
        if (from < 0 || from == place) return;
        int[] next = order.clone();
        int moving = next[from];
        if (from < place) System.arraycopy(order, from + 1, next, from, place - from);
        else System.arraycopy(order, place, next, place + 1, from - place);
        next[place] = moving;
        order = next;
        for (int i = 0; i < 5; i++) to[order[i]] = slot(i);
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        settle();
    }

    private void drop() {
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
        carrying = false;
        for (int i = 0; i < 5; i++) to[order[i]] = slot(i);
        final int was = dragged;
        animateLift(0f);
        settle();
        postDelayed(() -> { if (dragged == was) dragged = -1; invalidate(); }, Motion.enabled(getContext()) ? 220L : 0L);
        IslandTabs.set(order, hidden);
    }

    private void toggle(int place) {
        int role = order[place];
        if (!IslandTabs.canHide(role)) {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            return;
        }
        hidden ^= 1 << role;
        IslandTabs.set(order, hidden);
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        settle();
    }

    /** Buttons glide to their places and switches slide, together, in one short motion. */
    private void settle() {
        if (settling != null) settling.cancel();
        final float[] fromAt = at.clone(), fromLit = lit.clone();
        final float[] toLit = new float[5];
        for (int role = 0; role < 5; role++) toLit[role] = (hidden & 1 << role) == 0 ? 1f : 0f;
        if (!Motion.enabled(getContext())) {
            for (int role = 0; role < 5; role++) { if (!carrying || role != dragged) at[role] = to[role]; lit[role] = toLit[role]; }
            invalidate();
            return;
        }
        settling = ValueAnimator.ofFloat(0f, 1f);
        settling.setDuration(240L);
        settling.setInterpolator(Motion.OUT);
        settling.addUpdateListener(a -> {
            float t = (Float) a.getAnimatedValue();
            for (int role = 0; role < 5; role++) {
                if (!carrying || role != dragged) at[role] = fromAt[role] + (to[role] - fromAt[role]) * t;
                lit[role] = fromLit[role] + (toLit[role] - fromLit[role]) * t;
            }
            invalidate();
        });
        settling.start();
    }

    private ValueAnimator lifting;

    private void animateLift(float target) {
        if (lifting != null) lifting.cancel();
        if (!Motion.enabled(getContext())) { lift = target; invalidate(); return; }
        lifting = ValueAnimator.ofFloat(lift, target);
        lifting.setDuration(180L);
        lifting.setInterpolator(Motion.OUT);
        lifting.addUpdateListener(a -> { lift = (Float) a.getAnimatedValue(); invalidate(); });
        lifting.start();
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(hold);
        if (settling != null) settling.cancel();
        if (lifting != null) lifting.cancel();
        super.onDetachedFromWindow();
    }
}
