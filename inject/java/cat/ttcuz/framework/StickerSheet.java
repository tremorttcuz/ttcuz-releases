package cat.ttcuz.framework;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * "Стикер автопродления": a bottom sheet with a centred title, a grey line
 * under it, a 4-column grid of favourite stickers and a Save button.
 * No drag handle on top.
 */
final class StickerSheet {

    private StickerSheet() { }

    static void show(final Context c, final Skin skin, final Runnable changed) {
        final Panel p = Panel.with(c, skin, null);

        // ---- header, centred
        LinearLayout head = new LinearLayout(c);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setGravity(Gravity.CENTER_HORIZONTAL);
        head.setPadding(0, dp(c, 14), 0, dp(c, 6));

        TextView title = new TextView(c);
        title.setText(Text.t("Стикер автопродления"));
        title.setTextColor(skin.text);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        head.addView(title);

        TextView sub = new TextView(c);
        sub.setText(Text.t("Огонёк или любой стикер из избранного"));
        sub.setTextColor(skin.muted());
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        sub.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(dp(c, 8), dp(c, 6), dp(c, 8), 0);
        head.addView(sub);
        p.view(head);

        // ---- grid
        final List<String> ids = Streaks.offered();
        if (ids.isEmpty()) {
            p.text(Text.STREAK_NOTHING);
            FavouriteStickers.load(() -> { });
            p.quiet(Text.CLOSE, null);
            p.show();
            return;
        }

        Streaks.pruneDisk(c, ids);
        final String[] picked = {Streaks.chosen()};
        if (picked[0] == null || !ids.contains(picked[0])) picked[0] = ids.get(0);
        final List<Tile> tiles = new ArrayList<Tile>();

        LinearLayout grid = new LinearLayout(c);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(0, dp(c, 8), 0, dp(c, 4));
        LinearLayout line = null;
        int columns = 4;
        int count = Math.min(ids.size(), 120);
        for (int i = 0; i < count; i++) {
            if (i % columns == 0) {
                line = new LinearLayout(c);
                line.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(line, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            final String id = ids.get(i);
            final Tile tile = new Tile(c, skin, id);
            tile.setChosen(id.equals(picked[0]));
            tile.setOnClickListener(v -> {
                if (id.equals(picked[0])) return;
                picked[0] = id;
                for (Tile t : tiles) t.setChosen(t.id.equals(id));
                if (Motion.haptics()) v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            });
            tiles.add(tile);
            // the tiles come in as a short wave, row after row
            if (Motion.enabled(c)) {
                tile.setAlpha(0f);
                tile.setScaleX(.86f); tile.setScaleY(.86f);
                tile.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(Math.min(i, 16) * 22L + 60L)
                        .setDuration(Motion.time(280L)).setInterpolator(Motion.OUT).start();
            }
            LinearLayout.LayoutParams where = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            int m = dp(c, 4);
            where.setMargins(m, m, m, m);
            line.addView(tile, where);
        }
        // keep the last row's tiles the same width as the others
        int rest = count % columns;
        if (rest != 0 && line != null) {
            for (int i = rest; i < columns; i++) {
                View gap = new View(c);
                LinearLayout.LayoutParams where = new LinearLayout.LayoutParams(0, 1, 1f);
                int m = dp(c, 4);
                where.setMargins(m, m, m, m);
                line.addView(gap, where);
            }
        }
        p.view(grid);

        p.primary(Text.t("Сохранить"), () -> {
            Streaks.choose(picked[0]);
            if (changed != null) changed.run();
        });
        p.show();
    }

    /**
     * One rounded square with the whole sticker inside it (never cropped: a tall or wide sticker keeps its shape);
     * the chosen one gets a ring, a check badge, and a little bounce.
     */
    private static final class Tile extends FrameLayout {
        final String id;
        private final ImageView picture;
        private final View badge;
        private final GradientDrawable ring;

        Tile(Context c, Skin skin, String id) {
            super(c);
            this.id = id;
            final int radius = dp(c, 18);

            GradientDrawable fill = new GradientDrawable();
            fill.setColor((skin.text & 0xFFFFFF) | 0x10000000);
            fill.setCornerRadius(radius);
            setBackground(fill);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                }
            });
            setClipToOutline(true);

            picture = new ImageView(c);
            picture.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int room = dp(c, 8);
            picture.setPadding(room, room, room, room);
            addView(picture, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            ring = new GradientDrawable();
            ring.setColor(0x00000000);
            ring.setCornerRadius(radius);
            ring.setStroke(dp(c, 3), (skin.text & 0xFFFFFF) | 0x66000000);

            badge = new Check(c, skin.text);
            int size = dp(c, 20);
            LayoutParams at = new LayoutParams(size, size, Gravity.TOP | Gravity.END);
            at.topMargin = dp(c, 8);
            at.rightMargin = dp(c, 8);
            addView(badge, at);

            final java.lang.ref.WeakReference<ImageView> target =
                    new java.lang.ref.WeakReference<ImageView>(picture);
            Bitmap ready = Streaks.thumbnail(c, id, () -> {
                ImageView view = target.get();
                if (view != null && view.isAttachedToWindow()) {
                    Bitmap loaded = Streaks.thumbnail(view.getContext(), id);
                    if (loaded != null) {
                        view.setImageBitmap(loaded);
                        // a picture that arrives late fades in instead of popping
                        if (Motion.enabled(view.getContext())) { view.setAlpha(0f); view.animate().alpha(1f).setDuration(Motion.time(200L)).start(); }
                    }
                }
            });
            if (ready != null) picture.setImageBitmap(ready);
            Motion.touch(this);
        }

        private boolean chosen;

        void setChosen(boolean chosen) {
            boolean changed = this.chosen != chosen;
            this.chosen = chosen;
            setForeground(chosen ? ring : null);
            badge.setVisibility(chosen ? VISIBLE : GONE);
            if (!changed || !isAttachedToWindow() || !Motion.enabled(getContext())) {
                float k = chosen ? .84f : 1f;
                picture.setScaleX(k); picture.setScaleY(k);
                return;
            }
            // the picture steps back inside the ring (a gap like in the reference), the check pops in
            picture.animate().scaleX(chosen ? .84f : 1f).scaleY(chosen ? .84f : 1f)
                    .setDuration(Motion.time(260L)).setInterpolator(new android.view.animation.OvershootInterpolator(2f)).start();
            if (chosen) {
                badge.setScaleX(0f); badge.setScaleY(0f);
                badge.animate().scaleX(1f).scaleY(1f).setDuration(Motion.time(240L)).setInterpolator(new android.view.animation.OvershootInterpolator(3f)).start();
            }
        }

        @Override protected void onMeasure(int w, int h) {
            super.onMeasure(w, w); // square
        }
    }

    /** Small round badge with a tick. */
    private static final class Check extends View {
        private final Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        Check(Context c, int ink) {
            super(c);
            disc.setColor((ink & 0xFFFFFF) | 0x99000000);
            tick.setColor(0xFFFFFFFF);
            tick.setStyle(Paint.Style.STROKE);
            tick.setStrokeCap(Paint.Cap.ROUND);
            tick.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override protected void onDraw(Canvas canvas) {
            float s = Math.min(getWidth(), getHeight());
            canvas.drawCircle(s / 2f, s / 2f, s / 2f, disc);
            tick.setStrokeWidth(s * 0.12f);
            path.reset();
            path.moveTo(s * 0.28f, s * 0.52f);
            path.lineTo(s * 0.44f, s * 0.67f);
            path.lineTo(s * 0.73f, s * 0.36f);
            canvas.drawPath(path, tick);
        }
    }

    private static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }
}
