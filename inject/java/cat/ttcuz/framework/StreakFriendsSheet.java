package cat.ttcuz.framework;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * "С кем продлевать серию": centred title and hint, a two-way switch
 * "Со всеми / Только выбранные", the friends with a flame as one
 * Material 3 segmented list (28dp outer corners, 4dp inner, 2dp gaps),
 * and a Save button. No drag handle.
 */
final class StreakFriendsSheet {

    private StreakFriendsSheet() { }

    static void show(final Context c, final Skin skin, final Runnable changed) {
        final Panel p = Panel.with(c, skin, null);

        // ---- header
        LinearLayout head = new LinearLayout(c);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setGravity(Gravity.CENTER_HORIZONTAL);
        head.setPadding(0, dp(c, 14), 0, dp(c, 4));
        TextView title = new TextView(c);
        title.setText(Text.t("С кем продлевать серию"));
        title.setTextColor(skin.text);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        head.addView(title);
        TextView hint = new TextView(c);
        hint.setText(Text.t("Снимите отметку с тех, кому не нужно автоматически отправлять серии. Зажмите друга, чтобы подменить огонёк."));
        hint.setTextColor(skin.muted());
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        hint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        hint.setGravity(Gravity.CENTER);
        hint.setLineSpacing(0, 1.1f);
        hint.setPadding(dp(c, 8), dp(c, 6), dp(c, 8), 0);
        head.addView(hint);
        p.view(head);

        // ---- state being edited (saved only by "Сохранить")
        final boolean[] everyone = {Streaks.withEveryone()};
        final Set<String> off = new HashSet<String>(Streaks.excluded());

        // ---- segmented switch
        final Switch toggle = new Switch(c, skin);
        p.view(toggle);

        // ---- list
        final Expressive.Group list = new Expressive.Group(c,
                (skin.text & 0xFFFFFF) | 0x0F000000,
                (Accent.colour() & 0xFFFFFF) | 0x33000000);
        final TextView waiting = new TextView(c);
        waiting.setText(Text.t("Загрузка…"));
        waiting.setTextColor(skin.muted());
        waiting.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        waiting.setGravity(Gravity.CENTER);
        waiting.setPadding(0, dp(c, 24), 0, dp(c, 24));
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(waiting);
        box.addView(list);
        p.view(box);

        final Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            toggle.set(everyone[0]);
            for (int i = 0; i < list.getChildCount(); i++) {
                View v = list.getChildAt(i);
                if (v instanceof Row) ((Row) v).set(everyone[0], !off.contains(((Row) v).friend.conversation));
            }
        };
        toggle.onPick = all -> { everyone[0] = all; refresh[0].run(); };

        // Chats load by themselves: what is known shows at once, then friends and their chats are fetched now
        // (not only when the inbox was opened) and the list fills in when they arrive.
        final Runnable[] fill = new Runnable[1];
        fill[0] = () -> Net.away("streak friends", () -> {
            final List<Streaks.Friend> friends = Streaks.friends();
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!box.isAttachedToWindow() && list.getChildCount() > 0) return;
                if (friends.isEmpty()) {
                    if (list.getChildCount() == 0) waiting.setText(Text.t("Загрузка чатов…"));
                    return;
                }
                waiting.setVisibility(View.GONE);
                list.removeAllViews();
                for (final Streaks.Friend f : friends) {
                    final Row row = new Row(c, skin, f);
                    // a long press: the fake flame for this chat (number of days, colour)
                    row.setOnLongClickListener(v -> { fake(c, skin, f, row); return true; });
                    row.setOnClickListener(v -> {
                        // a tap in "Со всеми" switches to choosing, then unticks
                        if (everyone[0]) everyone[0] = false;
                        if (!off.remove(f.conversation)) off.add(f.conversation);
                        refresh[0].run();
                    });
                    list.addView(row, new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                }
                refresh[0].run();
                Fonts.applyTree(list);
            });
        });
        fill[0].run();
        SocialLibrary.refreshNow(() -> {
            fill[0].run();
            // the last page came in and still nothing: only then say so
            box.postDelayed(() -> { if (list.getChildCount() == 0)
                waiting.setText(Text.t("Нет друзей с огоньком.")); }, 4000L);
        });
        refresh[0].run();

        p.primary(Text.t("Сохранить"), () -> {
            Streaks.setWho(everyone[0], off);
            if (changed != null) changed.run();
        });
        p.show();
    }

    interface Pick { void on(boolean everyone); }

    /** M3 segmented control: tonal track, the chosen half is a raised pill. */
    private static final class Switch extends LinearLayout {
        Pick onPick;
        private final TextView all, some;
        private final Skin skin;

        Switch(Context c, Skin skin) {
            super(c);
            this.skin = skin;
            setOrientation(HORIZONTAL);
            int pad = dp(c, 4);
            setPadding(pad, pad, pad, pad);
            GradientDrawable track = new GradientDrawable();
            track.setColor((skin.text & 0xFFFFFF) | 0x0F000000);
            track.setCornerRadius(dp(c, 28));
            setBackground(track);
            all = half(c, Text.t("Со всеми"), true);
            some = half(c, Text.t("Только выбранные"), false);
            setMinimumHeight(dp(c, 56));
        }

        private TextView half(Context c, String label, final boolean everyone) {
            TextView t = new TextView(c);
            t.setText(label);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.END);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            t.setPadding(dp(c, 12), 0, dp(c, 12), 0);
            t.setOnClickListener(v -> { if (onPick != null) onPick.on(everyone); });
            Motion.touch(t);
            addView(t, new LayoutParams(0, dp(c, 48), 1f));
            return t;
        }

        void set(boolean everyone) {
            paint(all, everyone);
            paint(some, !everyone);
        }

        private void paint(TextView t, boolean on) {
            int accent = Accent.colour();
            if (on) {
                GradientDrawable pill = new GradientDrawable();
                pill.setColor((accent & 0xFFFFFF) | 0x33000000); // secondary container
                pill.setCornerRadius(dp(getContext(), 24));
                t.setBackground(pill);
                t.setTextColor(skin.text);
                t.setAlpha(1f);
            } else {
                t.setBackground(null);
                t.setTextColor(skin.muted());
            }
        }
    }

    /** One friend: avatar, name, flame + days, round check. */
    private static final class Row extends LinearLayout {
        final Streaks.Friend friend;
        private final Tick tick;
        private final TextView name;

        Row(Context c, Skin skin, Streaks.Friend f) {
            super(c);
            friend = f;
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
            setMinimumHeight(dp(c, 72));
            setClickable(true);
            setFocusable(true);

            ImageView avatar = new ImageView(c);
            avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            GradientDrawable round = new GradientDrawable();
            round.setShape(GradientDrawable.OVAL);
            round.setColor((skin.text & 0xFFFFFF) | 0x1F000000);
            avatar.setBackground(round);
            avatar.setClipToOutline(true);
            FriendAvatars.bind(avatar, f.avatar);
            addView(avatar, new LayoutParams(dp(c, 40), dp(c, 40)));

            name = new TextView(c);
            name.setText(f.name);
            name.setTextColor(skin.text);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            LayoutParams nameAt = new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            nameAt.leftMargin = dp(c, 16);
            nameAt.rightMargin = dp(c, 8);
            addView(name, nameAt);

            int[] fake = StreakFake.of(f.conversation);
            if (f.days > 0 || fake != null) {
                Flame flame = new Flame(c, skin.muted());
                addView(flame, new LayoutParams(dp(c, 18), dp(c, 18)));
                TextView days = new TextView(c);
                days.setText(fake != null && fake[0] >= 0 ? fake[0] + " ✎" : fake != null ? f.days + " ✎" : String.valueOf(f.days));
                days.setTextColor(skin.muted());
                days.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                days.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                LayoutParams daysAt = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                daysAt.leftMargin = dp(c, 4);
                daysAt.rightMargin = dp(c, 16);
                addView(days, daysAt);
            }

            tick = new Tick(c, skin);
            addView(tick, new LayoutParams(dp(c, 24), dp(c, 24)));
            Motion.touch(this);
        }

        /** In "Со всеми" every row is ticked and quieter; in choosing mode the tick is the user's. */
        void set(boolean everyone, boolean on) {
            boolean ticked = everyone || on;
            tick.set(ticked, everyone);
            setAlpha(everyone ? 0.6f : (on ? 1f : 0.85f));
            setContentDescription(friend.name + (ticked ? Text.t(", выбран") : Text.t(", не выбран")));
        }
    }

    /** M3 round checkbox: filled accent circle with a tick, or an outlined ring. */
    private static final class Tick extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final Skin skin;
        private boolean on, quiet;

        Tick(Context c, Skin skin) {
            super(c);
            this.skin = skin;
            ink.setStyle(Paint.Style.STROKE);
            ink.setStrokeCap(Paint.Cap.ROUND);
            ink.setStrokeJoin(Paint.Join.ROUND);
        }

        void set(boolean on, boolean quiet) { this.on = on; this.quiet = quiet; invalidate(); }

        @Override protected void onDraw(Canvas canvas) {
            float s = Math.min(getWidth(), getHeight()), r = s / 2f;
            int accent = quiet ? skin.muted() : Accent.colour();
            if (on) {
                fill.setStyle(Paint.Style.FILL);
                fill.setColor(accent);
                canvas.drawCircle(r, r, r, fill);
                ink.setColor(onColour(accent));
                ink.setStrokeWidth(s * 0.11f);
                path.reset();
                path.moveTo(s * 0.28f, s * 0.52f);
                path.lineTo(s * 0.44f, s * 0.67f);
                path.lineTo(s * 0.73f, s * 0.36f);
                canvas.drawPath(path, ink);
            } else {
                fill.setStyle(Paint.Style.STROKE);
                fill.setStrokeWidth(s * 0.09f);
                fill.setColor(skin.muted());
                canvas.drawCircle(r, r, r - s * 0.06f, fill);
            }
        }

        private static int onColour(int c) {
            int lum = (((c >> 16) & 0xFF) * 299 + ((c >> 8) & 0xFF) * 587 + (c & 0xFF) * 114) / 1000;
            return lum > 160 ? 0xFF1B1B1F : 0xFFFFFFFF;
        }
    }

    /** A small flame drawn with two curves, so no image is needed. */
    private static final class Flame extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        Flame(Context c, int colour) { super(c); paint.setColor(colour); }

        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight();
            path.reset();
            path.moveTo(w * 0.55f, h * 0.04f);
            path.cubicTo(w * 0.62f, h * 0.30f, w * 0.90f, h * 0.42f, w * 0.88f, h * 0.66f);
            path.cubicTo(w * 0.86f, h * 0.86f, w * 0.70f, h * 0.98f, w * 0.50f, h * 0.98f);
            path.cubicTo(w * 0.28f, h * 0.98f, w * 0.12f, h * 0.84f, w * 0.12f, h * 0.64f);
            path.cubicTo(w * 0.12f, h * 0.46f, w * 0.26f, h * 0.36f, w * 0.32f, h * 0.24f);
            path.cubicTo(w * 0.36f, h * 0.36f, w * 0.40f, h * 0.44f, w * 0.48f, h * 0.48f);
            path.cubicTo(w * 0.56f, h * 0.36f, w * 0.58f, h * 0.20f, w * 0.55f, h * 0.04f);
            path.close();
            canvas.drawPath(path, paint);
        }
    }

    private static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /**
     * The fake flame of one chat: how many days TikTok shows and which of its flames, lit or fading. Only this phone
     * sees it; the real streak keeps being renewed as before.
     */
    static void fake(final Context c, final Skin skin, final Streaks.Friend f, final View row) {
        final int[] now = StreakFake.of(f.conversation);
        final int[] colour = {now == null ? StreakFake.AS_IS : now[1]};
        Panel p = Panel.with(c, skin, Text.t("Огонёк") + " · " + f.name);
        p.text(Text.t("Число дней и цвет огонька, которые TikTok покажет в этом чате. Видно только на вашем телефоне; настоящая серия продлевается как обычно."));
        final android.widget.EditText days = p.field(Text.t("Число дней (пусто — настоящее)") + (f.days > 0 ? ": " + f.days : ""));
        days.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        days.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(5)});
        if (now != null && now[0] >= 0) days.setText(String.valueOf(now[0]));
        final String[] names = {Text.t("Как есть"), Text.t("Оранжевый"), Text.t("Серый")};
        LinearLayout chips = new LinearLayout(c);
        chips.setPadding(0, dp(c, 10), 0, dp(c, 4));
        final TextView[] views = new TextView[names.length];
        final Runnable paint = () -> {
            for (int i = 0; i < views.length; i++) {
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                bg.setCornerRadius(dp(c, 12));
                bg.setColor(i == colour[0] ? (Accent.colour() & 0xFFFFFF) | 0x55000000 : 0);
                if (i != colour[0]) bg.setStroke(dp(c, 1), skin.muted());
                views[i].setBackground(bg);
            }
        };
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            TextView chip = new TextView(c);
            chip.setText(names[i]);
            chip.setTextColor(skin.text);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            chip.setPadding(dp(c, 14), dp(c, 8), dp(c, 14), dp(c, 8));
            chip.setOnClickListener(v -> { colour[0] = index; paint.run(); v.setScaleX(.92f); v.setScaleY(.92f); v.animate().scaleX(1f).scaleY(1f).setDuration(Motion.time(220L)).setInterpolator(Motion.OUT).start(); });
            Motion.touch(chip);
            LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            at.rightMargin = dp(c, 8);
            chips.addView(chip, at);
            views[i] = chip;
        }
        paint.run();
        p.view(chips);
        p.primary(Text.t("Сохранить"), () -> {
            String typed = days.getText().toString().trim();
            int value = -1;
            try { if (!typed.isEmpty()) value = Integer.parseInt(typed); } catch (NumberFormatException ignored) { }
            StreakFake.set(f.conversation, new int[]{value, colour[0]});
            Screen.say(Text.t("Огонёк подменён. Откройте чаты заново, чтобы увидеть."));
        });
        if (now != null) p.quiet(Text.t("Убрать подмену"), () -> { StreakFake.set(f.conversation, null); Screen.say(Text.t("Подмена убрана")); });
        p.quiet(Text.t("Отмена"), null);
        p.show();
    }
}
