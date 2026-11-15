package cat.ttcuz.framework;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * The editor of a person's own nickname particles: their name with the effect on it, live, at the top; ready-made
 * starting points; then what floats (a shape or a picture of their own), how it moves, its colour, and sliders for
 * the rest. Every change shows at once; "Save" makes it the nickname's effect and publishes it with the profile.
 */
final class ParticleEditor {
    private ParticleEditor() {}

    interface ColourAsk { void ask(int initial, java.util.function.IntConsumer chosen); }

    private static java.lang.ref.WeakReference<Session> open = new java.lang.ref.WeakReference<>(null);

    /** A picture was chosen for the open editor (from the settings screen's file picker). */
    static void picked(android.graphics.Bitmap bitmap) {
        Session session = open.get();
        if (session == null || bitmap == null) return;
        String image = ParticleRecipe.encodePicture(bitmap);
        if (image.isEmpty()) { Screen.say(Text.t("Не удалось прочитать изображение")); return; }
        session.recipe.image = image;
        session.recipe.shape = ParticleRecipe.PICTURE;
        if (session.recipe.colourMode == ParticleRecipe.NAME_COLOUR) session.recipe.colourMode = ParticleRecipe.PICTURE_COLOURS;
        session.rebuild();
    }

    static void open(Activity activity, Skin skin, String name, Runnable pickPicture, ColourAsk askColour, Runnable saved) {
        ParticleRecipe start = ParticleRecipe.parse(ProfileStyle.particleRecipe());
        Session session = new Session(activity, skin, name == null || name.isEmpty() ? "ttcuz" : name, start == null ? new ParticleRecipe() : start,
                pickPicture, askColour);
        open = new java.lang.ref.WeakReference<>(session);
        Panel panel = Panel.with(activity, skin, Text.t("Свои частицы")).fullScreen();
        panel.view(session.preview);
        panel.view(session.controls);
        panel.primary(Text.t("Сохранить"), () -> {
            ProfileStyle.setParticleRecipe(session.recipe.json());
            Badge.refreshProfiles();
            if (saved != null) saved.run();
        });
        panel.quiet(Text.t("Отмена"), null);
        session.rebuild();
        panel.show();
    }

    private static final class Session {
        final Activity activity;
        final Skin skin;
        final ParticleRecipe recipe;
        final Preview preview;
        final LinearLayout controls;
        final Runnable pickPicture;
        final ColourAsk askColour;
        final float d;

        Session(Activity activity, Skin skin, String name, ParticleRecipe recipe, Runnable pickPicture, ColourAsk askColour) {
            this.activity = activity;
            this.skin = skin;
            this.recipe = recipe;
            this.pickPicture = pickPicture;
            this.askColour = askColour;
            d = activity.getResources().getDisplayMetrics().density;
            preview = new Preview(activity, skin, name, recipe);
            controls = new LinearLayout(activity);
            controls.setOrientation(LinearLayout.VERTICAL);
        }

        /** The controls drawn again from the recipe (after a preset or a picture changed several things at once). */
        void rebuild() {
            controls.removeAllViews();
            heading(Text.t("Заготовки"));
            String[] presets = {Text.t("Искры"), Text.t("Снег"), Text.t("Сердечки"), Text.t("Звёзды"), Text.t("Конфетти"), Text.t("Орбита"), Text.t("Фейерверк")};
            chips(presets, -1, i -> { preset(i); rebuild(); });

            heading(Text.t("Что летает"));
            String[] shapes = {Text.t("Точки"), Text.t("Искры"), Text.t("Сердечки"), Text.t("Звёзды"), Text.t("Снежинки"), Text.t("Своя картинка")};
            chips(shapes, recipe.shape, i -> {
                if (i == ParticleRecipe.PICTURE) { if (recipe.image.isEmpty()) { pickPicture.run(); return; } }
                recipe.shape = i;
                rebuild();
            });
            if (recipe.shape == ParticleRecipe.PICTURE) action(Text.t("Выбрать другую картинку"), pickPicture);

            heading(Text.t("Как движутся"));
            String[] motions = {Text.t("Мерцают"), Text.t("Поднимаются"), Text.t("Падают"), Text.t("Плывут вбок"), Text.t("Кружат"), Text.t("Разлетаются")};
            chips(motions, recipe.motion, i -> { recipe.motion = i; rebuild(); });

            heading(Text.t("Цвет"));
            String[] colours = recipe.shape == ParticleRecipe.PICTURE
                    ? new String[]{Text.t("Как у ника"), Text.t("Свой"), Text.t("Радуга"), Text.t("Цвета картинки")}
                    : new String[]{Text.t("Как у ника"), Text.t("Свой"), Text.t("Радуга")};
            chips(colours, Math.min(recipe.colourMode, colours.length - 1), i -> {
                if (i == ParticleRecipe.OWN_COLOUR) askColour.ask(recipe.colour, c -> { recipe.colour = c | 0xFF000000; recipe.colourMode = ParticleRecipe.OWN_COLOUR; rebuild(); });
                else { recipe.colourMode = i; rebuild(); }
            });

            heading(Text.t("Тонкая настройка"));
            slider(Text.t("Количество"), recipe.count, 1, 24, "", v -> recipe.count = v);
            slider(Text.t("Размер"), recipe.size, 2, 28, "", v -> recipe.size = v);
            slider(Text.t("Разный размер"), recipe.vary, 0, 100, "%", v -> recipe.vary = v);
            slider(Text.t("Скорость"), recipe.speed, 10, 300, "%", v -> recipe.speed = v);
            slider(Text.t("Покачивание"), recipe.wiggle, 0, 100, "%", v -> recipe.wiggle = v);
            slider(Text.t("Вращение"), recipe.spin, -360, 360, "°/с", v -> recipe.spin = v);
            slider(Text.t("Непрозрачность"), recipe.opacity, 10, 100, "%", v -> recipe.opacity = v);
            slider(Text.t("Размах вокруг ника"), recipe.spread, 0, 100, "%", v -> recipe.spread = v);
            toggle(Text.t("Плавно появляются и гаснут"), recipe.fade, on -> recipe.fade = on);
            preview.invalidate();
        }

        private void preset(int which) {
            recipe.vary = 40; recipe.wiggle = 30; recipe.spin = 0; recipe.fade = true; recipe.opacity = 90; recipe.spread = 30; recipe.speed = 100;
            if (recipe.shape == ParticleRecipe.PICTURE && which != 6) recipe.colourMode = ParticleRecipe.NAME_COLOUR;
            switch (which) {
                case 0: recipe.shape = ParticleRecipe.SPARKLE; recipe.motion = ParticleRecipe.TWINKLE; recipe.count = 8; recipe.size = 7; recipe.colourMode = ParticleRecipe.NAME_COLOUR; break;
                case 1: recipe.shape = ParticleRecipe.FLAKE; recipe.motion = ParticleRecipe.FALL; recipe.count = 10; recipe.size = 6; recipe.colourMode = ParticleRecipe.OWN_COLOUR; recipe.colour = 0xFFFFFFFF; recipe.spin = 60; break;
                case 2: recipe.shape = ParticleRecipe.HEART; recipe.motion = ParticleRecipe.RISE; recipe.count = 7; recipe.size = 8; recipe.colourMode = ParticleRecipe.OWN_COLOUR; recipe.colour = 0xFFFF4D79; break;
                case 3: recipe.shape = ParticleRecipe.STAR; recipe.motion = ParticleRecipe.TWINKLE; recipe.count = 7; recipe.size = 9; recipe.colourMode = ParticleRecipe.OWN_COLOUR; recipe.colour = 0xFFFFD54F; recipe.spin = 40; break;
                case 4: recipe.shape = ParticleRecipe.DOT; recipe.motion = ParticleRecipe.FALL; recipe.count = 18; recipe.size = 5; recipe.colourMode = ParticleRecipe.RAINBOW; recipe.wiggle = 70; break;
                case 5: recipe.shape = ParticleRecipe.SPARKLE; recipe.motion = ParticleRecipe.ORBIT; recipe.count = 6; recipe.size = 8; recipe.colourMode = ParticleRecipe.NAME_COLOUR; recipe.fade = false; recipe.speed = 60; break;
                default: recipe.shape = recipe.image.isEmpty() ? ParticleRecipe.DOT : ParticleRecipe.PICTURE; recipe.motion = ParticleRecipe.BURST; recipe.count = 16; recipe.size = 6; recipe.colourMode = recipe.image.isEmpty() ? ParticleRecipe.RAINBOW : ParticleRecipe.PICTURE_COLOURS; recipe.speed = 140; break;
            }
        }

        // ------------------------------------------------------------ the parts of the editor

        private void heading(String text) {
            TextView view = new TextView(activity);
            view.setText(text);
            view.setTextColor(Expressive.mix(skin.text, Accent.colour(), .55f));
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            view.setPadding(dp(4), dp(18), dp(4), dp(8));
            controls.addView(view);
        }

        private void chips(String[] names, int chosen, java.util.function.IntConsumer pick) {
            HorizontalScrollView scroll = new HorizontalScrollView(activity);
            scroll.setHorizontalScrollBarEnabled(false);
            scroll.setClipToPadding(false);
            LinearLayout row = new LinearLayout(activity);
            for (int i = 0; i < names.length; i++) {
                final int index = i;
                TextView chip = new TextView(activity);
                chip.setText(names[i]);
                chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                chip.setGravity(Gravity.CENTER);
                chip.setPadding(dp(16), 0, dp(16), 0);
                boolean on = i == chosen;
                GradientDrawable back = new GradientDrawable();
                back.setCornerRadius(dp(18));
                back.setColor(on ? Accent.colour() : Expressive.mix(skin.card, skin.text, .08f));
                chip.setTextColor(on ? onAccent() : skin.text);
                chip.setBackground(back);
                chip.setOnClickListener(v -> { v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK); pick.accept(index); });
                Motion.touch(chip);
                LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
                if (i > 0) place.leftMargin = dp(8);
                row.addView(chip, place);
            }
            scroll.addView(row);
            controls.addView(scroll);
        }

        private void action(String text, Runnable then) {
            TextView view = new TextView(activity);
            view.setText(text);
            view.setTextColor(Expressive.mix(skin.text, Accent.colour(), .55f));
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            view.setPadding(dp(4), dp(10), dp(4), dp(4));
            view.setOnClickListener(v -> then.run());
            controls.addView(view);
        }

        private void slider(String title, int value, int lo, int hi, String unit, java.util.function.IntConsumer set) {
            LinearLayout box = new LinearLayout(activity);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(4), dp(6), dp(4), 0);
            LinearLayout head = new LinearLayout(activity);
            TextView name = new TextView(activity);
            name.setText(title);
            name.setTextColor(skin.text);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            head.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView number = new TextView(activity);
            number.setText(value + unit);
            number.setTextColor(Expressive.mix(skin.text, Accent.colour(), .55f));
            number.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            head.addView(number);
            box.addView(head);
            SeekBar bar = Expressive.slider(activity, Accent.colour(), Expressive.mix(skin.card, skin.text, .2f));
            bar.setMax(hi - lo);
            bar.setProgress(value - lo);
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seek, int progress, boolean human) {
                    number.setText((progress + lo) + unit);
                    if (human) { set.accept(progress + lo); preview.invalidate(); }
                }
                @Override public void onStartTrackingTouch(SeekBar seek) { }
                @Override public void onStopTrackingTouch(SeekBar seek) { }
            });
            box.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));
            controls.addView(box);
        }

        private void toggle(String title, boolean on, java.util.function.Consumer<Boolean> set) {
            LinearLayout row = new LinearLayout(activity);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(4), dp(12), dp(4), dp(12));
            TextView name = new TextView(activity);
            name.setText(title);
            name.setTextColor(skin.text);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            M3Switch toggle = new M3Switch(activity);
            toggle.colours(Accent.colour(), skin.muted(), skin.card);
            toggle.setChecked(on);
            toggle.setOnChanged(checked -> set.accept(checked));
            row.addView(toggle);
            row.setOnClickListener(v -> toggle.performClick());
            controls.addView(row);
        }

        private int onAccent() {
            int a = Accent.colour();
            int luma = (android.graphics.Color.red(a) * 299 + android.graphics.Color.green(a) * 587 + android.graphics.Color.blue(a) * 114) / 1000;
            return luma > 150 ? 0xFF111111 : 0xFFFFFFFF;
        }

        private int dp(int value) { return Math.round(value * d); }
    }

    /** The nickname with the effect running over it, as it will be seen. */
    private static final class Preview extends View {
        private final String name;
        private final ParticleRecipe recipe;
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint back = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] seeds = new float[24 * 3];
        private final float d;
        private final int card;

        Preview(Context context, Skin skin, String name, ParticleRecipe recipe) {
            super(context);
            this.name = name;
            this.recipe = recipe;
            d = getResources().getDisplayMetrics().density;
            card = Expressive.mix(skin.card, 0xFF000000, .35f);
            text.setColor(skin.text);
            text.setTextSize(26 * d);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setTextAlign(Paint.Align.CENTER);
            java.util.Random random = new java.util.Random(42);
            for (int i = 0; i < seeds.length; i++) seeds[i] = random.nextFloat();
            setContentDescription(Text.t("Превью частиц"));
        }

        @Override protected void onMeasure(int w, int h) { setMeasuredDimension(MeasureSpec.getSize(w), Math.round(150 * d)); }

        @Override protected void onDraw(Canvas canvas) {
            back.setColor(card);
            canvas.drawRoundRect(0, 0, getWidth(), getHeight(), 24 * d, 24 * d, back);
            float wName = text.measureText(name), hName = text.getTextSize() * 1.2f;
            float left = (getWidth() - wName) / 2f, top = (getHeight() - hName) / 2f;
            canvas.drawText(name, getWidth() / 2f, top + hName * .8f, text);
            int save = canvas.save();
            canvas.translate(left, top);
            recipe.draw(canvas, Math.round(wName), Math.round(hName), d, SystemClock.uptimeMillis(), text.getColor(), seeds);
            canvas.restoreToCount(save);
            if (isAttachedToWindow()) postInvalidateOnAnimation();
        }
    }
}
