package cat.ttcuz.framework;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import cat.ttcuz.framework.plugin.PluginSettings;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The installed plugins as cards: the plugin's picture, its name, "v1.0 • @author" and a switch; what it does;
 * then a row of actions -- save as .ctt, pin to the top, details, its settings when it has any, and delete.
 *
 * Nothing here rebuilds the settings screen. A switch, a pin or a delete changes its own card and the list
 * animates the change; the engine's own updates (a plugin finished loading, or failed) refresh the cards in
 * place. Pictures are decoded once, off the main thread, and kept.
 */
final class PluginList {
    static final int SAVE = 3073;
    private static final String PREFS = "ttcuz_plugin_list";
    private static final LruCache<String, Bitmap> PICTURES = new LruCache<>(24);
    private static final LruCache<String, Gif.Frames> MOVING = new LruCache<>(8);
    private static String saving;

    static void trimMemory() { PICTURES.evictAll(); MOVING.evictAll(); }

    private PluginList() {}

    private static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    private static GradientDrawable shape(int colour, float radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(colour);
        shape.setCornerRadius(radius);
        return shape;
    }

    private static Set<String> pinned(Context c) {
        return new HashSet<>(c.getSharedPreferences(PREFS, 0).getStringSet("pinned", new HashSet<>()));
    }

    private static int danger(Skin skin) { return skin.dark() ? 0xFFF2B8B5 : 0xFFB3261E; }

    /** "v1.0 • @one, @two": the version, then every author as a nickname. */
    static String byline(String version, String author) {
        String v = version == null || version.trim().isEmpty() ? "" : "v" + version.trim().replaceFirst("^[vV]", "");
        StringBuilder handles = new StringBuilder();
        for (String one : (author == null ? "" : author).split("\\s*[,;]\\s*")) {
            one = one.trim();
            if (one.isEmpty()) continue;
            if (handles.length() > 0) handles.append(", ");
            handles.append(one.startsWith("@") || one.contains(" ") ? one : "@" + one);
        }
        if (v.isEmpty()) return handles.toString();
        return handles.length() == 0 ? v : v + " • " + handles;
    }

    /** A colour for the version and author: the accent, lifted towards the text so it reads on the card. */
    static int bylineColour(Skin skin) {
        return Expressive.mix(Accent.colour(), skin.dark() ? 0xFFFFFFFF : 0xFF000000, skin.dark() ? 0.45f : 0.25f);
    }

    /** The whole list, pinned plugins first, ready to sit in the settings column. */
    static View build(final Activity activity, final Skin skin) {
        final LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(activity, 16), 0, dp(activity, 16), dp(activity, 4));
        final Cards cards = new Cards(activity, skin, list);

        List<PluginEngine.Info> ordered = ordered(activity);
        // the installed plugins are simply there: no entrance
        for (PluginEngine.Info info : ordered) {
            Card card = new Card(activity, skin, info, cards);
            cards.all.add(card);
            list.addView(card.root, spaced(activity));
        }
        if (ordered.isEmpty()) list.addView(empty(activity, skin));

        // the engine says when a plugin was installed, loaded, failed or went away: new cards grow in, removed ones
        // fold away, the rest redraw only what they show. Also run on return to the screen, so a plugin installed
        // from the store or the editor while the list was hidden is there too.
        final Runnable refresh = cards::sync;
        // while the store or the editor covers the list, the change waits for them to close, so it is seen
        final android.view.ViewTreeObserver.OnWindowFocusChangeListener focus = has -> { if (has) list.post(refresh); };
        list.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) {
                PluginEngine.watch(refresh);
                list.getViewTreeObserver().addOnWindowFocusChangeListener(focus);
                list.post(refresh);
            }
            @Override public void onViewDetachedFromWindow(View v) {
                PluginEngine.unwatch(refresh);
                list.getViewTreeObserver().removeOnWindowFocusChangeListener(focus);
            }
        });
        Fonts.applyTree(list);
        return list;
    }

    private static List<PluginEngine.Info> ordered(Context c) {
        List<PluginEngine.Info> all = new ArrayList<>(PluginEngine.list());
        Set<String> pins = pinned(c);
        List<PluginEngine.Info> ordered = new ArrayList<>();
        for (PluginEngine.Info info : all) if (pins.contains(info.id)) ordered.add(info);
        for (PluginEngine.Info info : all) if (!pins.contains(info.id)) ordered.add(info);
        return ordered;
    }

    /**
     * The cards on screen and the list holding them. Cards change height frame by frame while they grow or fold, so
     * the list's own layout animation is held for that time: two animations moving the same cards made them jump.
     */
    private static final class Cards {
        final Activity activity;
        final Skin skin;
        final LinearLayout list;
        final LayoutTransition motion;
        final List<Card> all = new ArrayList<>();
        private int held;

        Cards(Activity activity, Skin skin, LinearLayout list) {
            this.activity = activity;
            this.skin = skin;
            this.list = list;
            motion = new LayoutTransition();
            motion.enableTransitionType(LayoutTransition.CHANGING);
            motion.setDuration(Motion.time(320L));
            for (int type : new int[]{LayoutTransition.APPEARING, LayoutTransition.DISAPPEARING, LayoutTransition.CHANGE_APPEARING,
                    LayoutTransition.CHANGE_DISAPPEARING, LayoutTransition.CHANGING})
                motion.setInterpolator(type, Motion.BOTH);
            motion.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0);
            motion.setStartDelay(LayoutTransition.APPEARING, 0);
            list.setLayoutTransition(motion);
        }

        void hold() { if (held++ == 0) list.setLayoutTransition(null); }
        void release() { if (held > 0 && --held == 0) list.setLayoutTransition(motion); }

        /** Brings the cards in line with what the engine has now. */
        void sync() {
            if (activity.isFinishing() || !list.isAttachedToWindow()) return;
            List<PluginEngine.Info> now = ordered(activity);
            if (!list.hasWindowFocus()) {
                // only a change of state is drawn now; cards that come or go wait for the screen to be in front
                for (Card card : all) card.refresh();
                return;
            }
            java.util.Map<String, PluginEngine.Info> byId = new java.util.HashMap<>();
            for (PluginEngine.Info info : now) byId.put(info.id, info);
            for (Card card : new ArrayList<>(all)) {
                PluginEngine.Info fresh = byId.get(card.info.id);
                if (fresh == null) { card.collapse(); continue; }
                if (fresh != card.info) {
                    // reinstalled or updated: a new card takes the old one's place, crossfading
                    Card next = new Card(activity, skin, fresh, this);
                    int at = list.indexOfChild(card.root);
                    all.set(all.indexOf(card), next);
                    hold();
                    list.removeView(card.root);
                    list.addView(next.root, Math.max(0, at), spaced(activity));
                    release();
                    Fonts.applyTree(next.root);
                } else card.refresh();
            }
            for (int i = 0; i < now.size(); i++) {
                PluginEngine.Info info = now.get(i);
                if (find(info.id) != null) continue;
                View none = list.findViewWithTag("empty");
                if (none != null) list.removeView(none);
                Card card = new Card(activity, skin, info, this);
                // after the cards that come before it in the engine's order
                int at = 0;
                for (int j = 0; j < i; j++) { Card before = find(now.get(j).id); if (before != null) at = Math.max(at, list.indexOfChild(before.root) + 1); }
                all.add(card);
                list.addView(card.root, Math.min(at, list.getChildCount()), spaced(activity));
                Fonts.applyTree(card.root);
                card.grow();
            }
        }

        Card find(String id) { for (Card card : all) if (card.info.id.equals(id)) return card; return null; }
    }

    private static LinearLayout.LayoutParams spaced(Context c) {
        LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        at.bottomMargin = dp(c, 10);
        return at;
    }

    private static View empty(Context c, Skin skin) {
        TextView none = new TextView(c);
        none.setText(Text.t("Плагины не установлены. Их можно взять в магазине или собрать в редакторе."));
        none.setTextColor(skin.text);
        none.setAlpha(0.7f);
        none.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        none.setGravity(Gravity.CENTER);
        none.setPadding(dp(c, 16), dp(c, 20), dp(c, 16), dp(c, 20));
        none.setBackground(shape(skin.card, dp(c, 24)));
        none.setTag("empty");
        return none;
    }

    // ------------------------------------------------------------------ one card

    private static final class Card {
        final Activity activity;
        final Skin skin;
        final PluginEngine.Info info;
        final Cards cards;
        final LinearLayout list;
        final LinearLayout root;
        final ImageView picture;
        final TextView error;
        final ImageView settings, pin;
        final M3Switch toggle;
        final View body;

        Card(final Activity activity, final Skin skin, final PluginEngine.Info info, final Cards cards) {
            this.activity = activity;
            this.skin = skin;
            this.info = info;
            this.cards = cards;
            this.list = cards.list;
            Context c = activity;
            root = new LinearLayout(c);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(c, 18), dp(c, 18), dp(c, 14), dp(c, 8));
            root.setBackground(shape(skin.card, dp(c, 26)));
            root.setTag(info.id);
            LayoutTransition inside = new LayoutTransition();
            inside.enableTransitionType(LayoutTransition.CHANGING);
            inside.setDuration(Motion.time(220L));
            root.setLayoutTransition(inside);

            // the top: picture, name and byline, switch
            LinearLayout top = new LinearLayout(c);
            top.setGravity(Gravity.CENTER_VERTICAL);
            picture = new ImageView(c);
            picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
            picture.setBackground(shape(Expressive.mix(skin.card, Accent.colour(), 0.24f), dp(c, 18)));
            picture.setClipToOutline(true);
            top.addView(picture, new LinearLayout.LayoutParams(dp(c, 60), dp(c, 60)));
            showPicture();

            LinearLayout words = new LinearLayout(c);
            words.setOrientation(LinearLayout.VERTICAL);
            words.setPadding(dp(c, 16), 0, dp(c, 10), 0);
            TextView name = new TextView(c);
            name.setText(info.name);
            name.setTextColor(skin.text);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            name.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            words.addView(name);
            TextView by = new TextView(c);
            by.setText(byline(info));
            by.setTextColor(bylineColour(skin));
            by.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            by.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            by.setSingleLine(true);
            by.setEllipsize(TextUtils.TruncateAt.END);
            by.setPadding(0, dp(c, 2), 0, 0);
            words.addView(by);
            top.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            toggle = new M3Switch(c);
            toggle.colours(Accent.colour(), skin.muted(), skin.card);
            toggle.setChecked(info.enabled);
            toggle.setContentDescription(info.name);
            toggle.setOnChanged(on -> {
                PluginEngine.enable(info.id, on);
                dim(on, true);
            });
            // a 46 dp tall box, like the action buttons, so the switch lines up with them
            android.widget.FrameLayout toggleBox = new android.widget.FrameLayout(c);
            toggleBox.addView(toggle, new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            toggleBox.setOnClickListener(v -> toggle.performClick());
            top.addView(toggleBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(c, 46)));
            root.addView(top);

            // what it does: four lines, the rest a tap away
            final TextView about = new TextView(c);
            String description = info.description == null ? "" : info.description.trim();
            about.setText(description);
            about.setTextColor(skin.text);
            about.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            about.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            about.setLineSpacing(0, 1.12f);
            about.setMaxLines(4);
            about.setEllipsize(TextUtils.TruncateAt.END);
            about.setPadding(0, dp(c, 14), dp(c, 4), 0);
            about.setVisibility(description.isEmpty() ? View.GONE : View.VISIBLE);
            about.setOnClickListener(v -> about.setMaxLines(about.getMaxLines() == 4 ? 40 : 4));
            root.addView(about);
            body = about;

            error = new TextView(c);
            error.setTextColor(danger(skin));
            error.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            error.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
            error.setBackground(shape((danger(skin) & 0xFFFFFF) | 0x22000000, dp(c, 14)));
            error.setMaxLines(4);
            error.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams errorAt = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            errorAt.topMargin = dp(c, 12);
            root.addView(error, errorAt);

            // the actions, as in the reference: four on the left, delete on its own at the right
            LinearLayout actions = new LinearLayout(c);
            actions.setGravity(Gravity.CENTER_VERTICAL);
            actions.setPadding(0, dp(c, 8), 0, 0);
            actions.setLayoutTransition(new LayoutTransition());
            actions.addView(action("share", Text.t("Сохранить .ctt"), skin.text, v -> save()));
            pin = action(pinned(c).contains(info.id) ? "push_pin_fill1" : "push_pin", Text.t("Закрепить"), skin.text, v -> togglePin());
            actions.addView(pin);
            actions.addView(action("key", Text.t("О плагине"), skin.text, v -> details()));
            settings = action("settings", Text.t("Настройки плагина"), skin.text, v -> openSettings());
            actions.addView(settings);
            actions.addView(new View(c), new LinearLayout.LayoutParams(0, 1, 1f));
            ImageView trash = action("delete", Text.t("Удалить"), danger(skin), v -> confirmDelete());
            ((LinearLayout.LayoutParams) trash.getLayoutParams()).rightMargin = 0;
            actions.addView(trash);
            root.addView(actions);

            refresh();
            dim(info.enabled, false);
        }

        private String byline(PluginEngine.Info info) { return PluginList.byline(info.version, info.author); }

        private ImageView action(String icon, String label, int colour, View.OnClickListener click) {
            ImageView view = new ImageView(activity);
            view.setImageDrawable(Symbols.drawable(icon, colour));
            int p = dp(activity, 10);
            view.setPadding(p, p, p, p);
            view.setContentDescription(label);
            view.setBackground(Expressive.tile(activity, 0, (Accent.colour() & 0xFFFFFF) | 0x33000000, dp(activity, 22), dp(activity, 22), dp(activity, 12)));
            view.setOnClickListener(click);
            view.setTooltipText(label);
            Motion.touch(view);
            LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(dp(activity, 46), dp(activity, 46));
            at.rightMargin = dp(activity, 6);
            view.setLayoutParams(at);
            return view;
        }

        /** What the engine knows now: whether it runs, whether it failed, whether it has settings. */
        void refresh() {
            if (activity.isFinishing()) return;
            String problem = info.error == null ? "" : info.error.trim();
            if (!problem.isEmpty()) error.setText(problem.split("\n", 2)[0]);
            error.setVisibility(problem.isEmpty() ? View.GONE : View.VISIBLE);
            settings.setVisibility(PluginEngine.hasSettings(info.id) ? View.VISIBLE : View.GONE);
            if (toggle.isChecked() != info.enabled) { toggle.setChecked(info.enabled, true); dim(info.enabled, true); }
        }

        /** Off, the plugin's picture and words step back; the switch and the actions stay at full strength. */
        private void dim(boolean on, boolean animate) {
            float alpha = on ? 1f : 0.55f;
            for (View view : new View[]{picture, body}) {
                if (animate) view.animate().alpha(alpha).setDuration(Motion.time(220L)).setInterpolator(Motion.BOTH).start();
                else view.setAlpha(alpha);
            }
        }

        private void showPicture() {
            final File file = info.icon();
            final String key = file == null ? null : file.getPath() + ":" + file.lastModified();
            Bitmap known = key == null ? null : PICTURES.get(key);
            Gif.Frames moving = key == null ? null : MOVING.get(key);
            if (moving != null) { picture.setPadding(0, 0, 0, 0); picture.setImageDrawable(new Gif.Player(moving)); return; }
            if (known != null) { picture.setPadding(0, 0, 0, 0); picture.setImageBitmap(known); return; }
            int p = dp(activity, 15);
            picture.setPadding(p, p, p, p);
            picture.setImageDrawable(Symbols.drawable("extension", Accent.colour()));
            if (file == null) return;
            final int size = dp(activity, 60);
            Net.away("plugin picture", () -> {
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(file.getPath(), bounds);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;
                while (bounds.outWidth / (options.inSampleSize * 2) >= size && bounds.outHeight / (options.inSampleSize * 2) >= size)
                    options.inSampleSize *= 2;
                // an animated picture plays in the list (cut to frames once, at the list's size)
                byte[] raw = file.length() <= 1048576 ? Net.read(file) : null;
                final Gif.Frames frames = Gif.isGif(raw) ? Gif.decode(raw, Math.min(size * 2, 160), 50, 50, 100) : null;
                if (frames != null && frames.frames.length > 1) MOVING.put(key, frames);
                final Bitmap bitmap = frames != null ? frames.frames[0] : BitmapFactory.decodeFile(file.getPath(), options);
                if (bitmap == null) return;
                PICTURES.put(key, bitmap);
                activity.runOnUiThread(() -> {
                    picture.setPadding(0, 0, 0, 0);
                    if (frames != null && frames.frames.length > 1) picture.setImageDrawable(new Gif.Player(frames)); else picture.setImageBitmap(bitmap);
                });
            });
        }

        // ---------------------------------------------------------- actions

        private void save() {
            saving = info.id;
            // kept on disk too: TikTok may be closed while the file picker is open, and the id with it
            activity.getSharedPreferences(PREFS, 0).edit().putString("saving", info.id).apply();
            try {
                activity.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("application/octet-stream").putExtra(Intent.EXTRA_TITLE, info.id + ".ctt"), SAVE);
            } catch (RuntimeException error) {
                Screen.say(Text.t("Не удалось открыть файлы"));
            }
        }

        private void togglePin() {
            SharedPreferences prefs = activity.getSharedPreferences(PREFS, 0);
            Set<String> pins = pinned(activity);
            boolean now = !pins.contains(info.id);
            if (now) pins.add(info.id); else pins.remove(info.id);
            prefs.edit().putStringSet("pinned", pins).apply();
            pin.setImageDrawable(Symbols.drawable(now ? "push_pin_fill1" : "push_pin", skin.text));
            pin.setRotation(now ? -30f : 30f);
            pin.animate().rotation(0f).setDuration(Motion.time(300L)).setInterpolator(Motion.OUT).start();
            // move the card: pinned to the top, unpinned to just below the last pinned one
            int target = 0;
            for (int i = 0; i < list.getChildCount(); i++) {
                View child = list.getChildAt(i);
                if (child == root) continue;
                Object id = child.getTag();
                if (id instanceof String && pins.contains(id)) target++;
            }
            if (list.indexOfChild(root) != target) {
                list.removeView(root);
                list.addView(root, Math.min(target, list.getChildCount()), spaced(activity));
            }
            Screen.say(now ? Text.t("Закреплён") : Text.t("Откреплён"));
        }

        /** The key: what the plugin is, and what it may do -- each permission with what it means. */
        private void details() {
            Panel panel = Panel.with(activity, skin, info.name);
            panel.subtitle(byline(info));
            String state = !info.enabled ? Text.t("Выключен") : info.loaded ? Text.t("Работает") : Text.t("Не запущен");
            panel.info(info.loaded ? "check_circle" : "info", state, Text.t("Состояние"), null);
            panel.info("tag", info.id, "ID", null);
            panel.info("update", info.version + " · API " + info.api(), Text.t("Версия"), null);
            if (info.author != null && !info.author.isEmpty()) panel.info("person", info.author, Text.t("Автор"), null);
            java.util.List<String> can = info.permissions();
            panel.section(Text.t("Что может плагин"));
            // API 4: one word for how far the plugin reaches, before the list
            int tier = PluginPermissions.tier(can);
            if (tier >= 2) panel.warning("shield", PluginPermissions.tierName(tier), Text.t("Видит и делает в TikTok то же, что и вы. Ставьте, только если доверяете автору."));
            else if (tier == 1) panel.info("shield", PluginPermissions.tierName(tier), Text.t("Пользуется интернетом, аккаунтом или настройками."), null);
            if (can.isEmpty()) panel.info("shield", Text.t("Ничего особенного"), Text.t("Не просит разрешений и не меняет TikTok"), null);
            // the sensitive ones (network, account, TikTok's insides, Android permissions) in the warning colour
            for (String permission : can) {
                // what it may do, and below it what the author says it does with it (// @uses in its code)
                String why = info.reason(permission), detail = PluginPermissions.detail(permission)
                        + (why == null ? "" : "\n" + Text.t("Зачем") + ": " + why);
                if (PluginPermissions.sensitive(permission)) panel.warning(PluginPermissions.icon(permission), PluginPermissions.title(permission), detail);
                else panel.info(PluginPermissions.icon(permission), PluginPermissions.title(permission), detail, null);
            }
            if (info.api() < 2) panel.text(Text.t("Плагин старого API 1: список собран по тому, что он переопределяет. Такие плагины всегда могли обращаться к TikTok напрямую."));
            else if (!info.loaded && info.enabled) panel.text(Text.t("Список взят из manifest.json плагина."));
            if (info.error != null && !info.error.isEmpty()) panel.text(info.error);
            panel.quiet(Text.CLOSE, null);
            panel.show();
        }

        private void openSettings() {
            if (!info.enabled || !info.loaded) {
                Screen.say(Text.t("Включите плагин, чтобы открыть его настройки"));
                return;
            }
            PluginSettings rows = PluginEngine.settings(info.id, activity);
            if (rows == null) return; // the plugin opened its own screen
            PluginList.settings(activity, skin, info, rows);
        }

        private void confirmDelete() {
            Panel panel = Panel.with(activity, skin, Text.t("Удалить плагин?"));
            panel.text(info.name + " " + Text.t("будет удалён вместе с его данными."));
            panel.primary(Text.t("Удалить"), () -> PluginEngine.remove(info.id, problem -> activity.runOnUiThread(() -> {
                if (problem != null && !problem.isEmpty()) { Screen.say(problem); return; }
                collapse();
            })));
            panel.quiet(Text.t("Отмена"), null);
            panel.show();
        }

        /** A new card: it opens from nothing, pushing the ones below down, and rises into place. */
        void grow() {
            if (!Motion.enabled(activity)) return;
            int width = list.getWidth() - list.getPaddingLeft() - list.getPaddingRight();
            if (width <= 0) return;
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            final int height = root.getMeasuredHeight();
            final ViewGroup.LayoutParams at = root.getLayoutParams();
            at.height = 1;
            root.setLayoutParams(at);
            root.setAlpha(0f);
            root.setScaleX(0.92f);
            root.setScaleY(0.92f);
            root.setTranslationY(dp(activity, 16));
            cards.hold();
            ValueAnimator open = ValueAnimator.ofFloat(0f, 1f);
            open.setDuration(Motion.time(460L));
            open.setInterpolator(Motion.OUT);
            open.addUpdateListener(a -> {
                float f = (float) a.getAnimatedValue();
                at.height = Math.max(1, Math.round(height * Math.min(1f, f * 1.35f)));
                root.setLayoutParams(at);
                float late = Math.max(0f, (f - 0.25f) / 0.75f);
                root.setAlpha(late);
                root.setScaleX(0.92f + 0.08f * late);
                root.setScaleY(0.92f + 0.08f * late);
                root.setTranslationY(dp(activity, 16) * (1f - late));
            });
            open.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    at.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    root.setLayoutParams(at);
                    root.setAlpha(1f); root.setScaleX(1f); root.setScaleY(1f); root.setTranslationY(0f);
                    cards.release();
                }
            });
            open.start();
        }

        /** The card shrinks away, the ones below close the gap, and an empty list says so. */
        private boolean folding;
        void collapse() {
            if (folding) return;
            folding = true;
            cards.all.remove(this);
            final int height = root.getHeight();
            if (height <= 0 || !Motion.enabled(activity)) { list.removeView(root); emptied(); return; }
            cards.hold();
            ValueAnimator shrink = ValueAnimator.ofFloat(1f, 0f);
            shrink.setDuration(Motion.time(340L));
            shrink.setInterpolator(Motion.BOTH);
            final ViewGroup.LayoutParams at = root.getLayoutParams();
            shrink.addUpdateListener(a -> {
                float f = (float) a.getAnimatedValue();
                float fade = Math.max(0f, (f - 0.35f) / 0.65f);
                root.setAlpha(fade);
                root.setScaleX(0.92f + 0.08f * fade);
                root.setScaleY(0.92f + 0.08f * fade);
                at.height = Math.max(1, Math.round(height * Math.min(1f, f * 1.25f)));
                root.setLayoutParams(at);
            });
            shrink.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    list.removeView(root);
                    cards.release();
                    emptied();
                }
            });
            shrink.start();
        }

        private void emptied() {
            if (!cards.all.isEmpty() || list.findViewWithTag("empty") != null) return;
            View none = empty(activity, skin);
            none.setAlpha(0f);
            list.addView(none);
            none.animate().alpha(1f).setDuration(Motion.time(260L)).setInterpolator(Motion.BOTH).start();
        }
    }

    // ------------------------------------------------------------------ the plugin's settings

    /** The rows a plugin described, drawn in the mod's own style and stored in the plugin's preferences. */
    static void settings(final Activity activity, final Skin skin, final PluginEngine.Info info, PluginSettings rows) {
        final SharedPreferences prefs = PluginEngine.prefs(info.id);
        Panel panel = Panel.with(activity, skin, info.name);
        panel.subtitle(Text.t("Настройки плагина"));
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        if (rows.rows().isEmpty()) {
            box.addView(words(activity, skin, Text.t("У плагина нет настроек"), 14, 0.7f));
        }
        for (final PluginSettings.Row row : rows.rows()) {
            try {
                View view = row(activity, skin, prefs, row);
                if (view == null) continue;
                LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                at.topMargin = dp(activity, 6);
                box.addView(view, at);
            } catch (Throwable error) {
                Diary.note("plugin settings " + info.id + ": " + error);
            }
        }
        panel.view(box);
        panel.quiet(Text.CLOSE, null);
        panel.show();
    }

    /** One card per plugin that adds rows to the mod's settings: its name, then its rows (stored in its own prefs). */
    static java.util.List<View> modSettings(Activity activity, Skin skin) {
        java.util.List<View> out = new java.util.ArrayList<>();
        for (Object[] entry : PluginEngine.modSettings()) {
            PluginEngine.Info info = (PluginEngine.Info) entry[0];
            PluginSettings rows = (PluginSettings) entry[1];
            LinearLayout box = new LinearLayout(activity);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
            TextView name = words(activity, skin, info.name, 13, 0.7f);
            name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            name.setPadding(dp(activity, 4), 0, 0, dp(activity, 4));
            box.addView(name);
            SharedPreferences prefs = PluginEngine.prefs(info.id);
            for (PluginSettings.Row row : rows.rows()) {
                try {
                    View view = row(activity, skin, prefs, row);
                    if (view == null) continue;
                    LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    at.topMargin = dp(activity, 4);
                    box.addView(view, at);
                } catch (Throwable error) {
                    Diary.note("plugin mod settings " + info.id + ": " + error);
                }
            }
            out.add(box);
        }
        return out;
    }

    private static TextView words(Context c, Skin skin, String text, float size, float alpha) {
        TextView view = new TextView(c);
        view.setText(text);
        view.setTextColor(skin.text);
        view.setAlpha(alpha);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        view.setLineSpacing(0, 1.1f);
        return view;
    }

    private static View row(final Activity activity, final Skin skin, final SharedPreferences prefs, final PluginSettings.Row row) {
        final Context c = activity;
        switch (row.kind) {
            case TEXT: {
                TextView text = words(c, skin, row.title, 14, 0.78f);
                text.setPadding(dp(c, 4), dp(c, 6), dp(c, 4), dp(c, 6));
                return text;
            }
            case ACTION: {
                TextView button = words(c, skin, row.title, 15, 1f);
                button.setGravity(Gravity.CENTER);
                button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                button.setMinHeight(dp(c, 48));
                button.setPadding(dp(c, 16), dp(c, 10), dp(c, 16), dp(c, 10));
                button.setBackground(Expressive.tile(c, Expressive.mix(skin.card, Accent.colour(), 0.22f),
                        (Accent.colour() & 0xFFFFFF) | 0x33000000, dp(c, 24), dp(c, 24), dp(c, 12)));
                Motion.touch(button);
                button.setOnClickListener(v -> {
                    try { ((Runnable) row.value).run(); }
                    catch (Throwable error) { Screen.say(String.valueOf(error.getMessage())); Diary.note("plugin action: " + error); }
                });
                return button;
            }
            case TOGGLE: {
                LinearLayout line = new LinearLayout(c);
                line.setGravity(Gravity.CENTER_VERTICAL);
                line.setMinimumHeight(dp(c, 52));
                line.setPadding(dp(c, 4), 0, 0, 0);
                line.addView(words(c, skin, row.title, 16, 1f), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                final M3Switch toggle = new M3Switch(c);
                toggle.colours(Accent.colour(), skin.muted(), skin.card);
                toggle.setChecked(prefs == null ? (Boolean) row.value : prefs.getBoolean(row.key, (Boolean) row.value));
                toggle.setEnabled(prefs != null);
                toggle.setOnChanged(on -> { if (prefs != null) prefs.edit().putBoolean(row.key, on).apply(); });
                line.addView(toggle);
                line.setOnClickListener(v -> toggle.performClick());
                return line;
            }
            case CHOICE: {
                LinearLayout column = new LinearLayout(c);
                column.setOrientation(LinearLayout.VERTICAL);
                TextView title = words(c, skin, row.title, 16, 1f);
                title.setPadding(dp(c, 4), dp(c, 6), 0, dp(c, 8));
                column.addView(title);
                final String[] options = (String[]) row.value;
                final LinearLayout chips = new LinearLayout(c);
                HorizontalScrollView strip = new HorizontalScrollView(c);
                strip.setHorizontalScrollBarEnabled(false);
                strip.addView(chips);
                column.addView(strip);
                final int[] chosen = {prefs == null ? row.min : prefs.getInt(row.key, row.min)};
                final Runnable[] draw = new Runnable[1];
                draw[0] = () -> {
                    chips.removeAllViews();
                    for (int i = 0; i < options.length; i++) {
                        final int index = i;
                        TextView chip = words(c, skin, options[i], 14, 1f);
                        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                        chip.setPadding(dp(c, 14), dp(c, 8), dp(c, 14), dp(c, 8));
                        GradientDrawable shape = shape(index == chosen[0] ? Expressive.mix(skin.card, Accent.colour(), 0.32f) : 0, dp(c, 10));
                        if (index != chosen[0]) shape.setStroke(Math.max(1, dp(c, 1)), Expressive.mix(skin.card, skin.text, 0.28f));
                        chip.setBackground(shape);
                        chip.setOnClickListener(v -> {
                            chosen[0] = index;
                            if (prefs != null) prefs.edit().putInt(row.key, index).apply();
                            draw[0].run();
                        });
                        LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        at.rightMargin = dp(c, 8);
                        chips.addView(chip, at);
                    }
                    Fonts.applyTree(chips);
                };
                draw[0].run();
                return column;
            }
            case SLIDER: {
                LinearLayout column = new LinearLayout(c);
                column.setOrientation(LinearLayout.VERTICAL);
                LinearLayout head = new LinearLayout(c);
                head.setPadding(dp(c, 4), dp(c, 6), dp(c, 4), 0);
                head.addView(words(c, skin, row.title, 16, 1f), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                final int start = prefs == null ? (Integer) row.value : prefs.getInt(row.key, (Integer) row.value);
                final TextView value = words(c, skin, String.valueOf(start), 15, 0.8f);
                head.addView(value);
                column.addView(head);
                Expressive.M3Slider slider = Expressive.slider(c, Accent.colour(), Expressive.mix(skin.card, skin.text, 0.18f));
                slider.setMax(row.max - row.min);
                slider.setProgress(Math.max(0, Math.min(row.max - row.min, start - row.min)));
                slider.setEnabled(prefs != null);
                slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar bar, int progress, boolean user) { value.setText(String.valueOf(progress + row.min)); }
                    @Override public void onStartTrackingTouch(SeekBar bar) {}
                    @Override public void onStopTrackingTouch(SeekBar bar) {
                        if (prefs != null) prefs.edit().putInt(row.key, bar.getProgress() + row.min).apply();
                    }
                });
                column.addView(slider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 44)));
                return column;
            }
            case VIEW: {
                View view = (View) row.value;
                if (view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view);
                return view;
            }
            default:
                return null;
        }
    }

    // ------------------------------------------------------------------ saving a .ctt

    /** Writes the plugin picked for saving to the document the person chose. */
    static void save(final Activity activity, final Uri uri) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS, 0);
        final String id = saving != null ? saving : prefs.getString("saving", null);
        saving = null;
        prefs.edit().remove("saving").apply();
        if (id == null) { Screen.say(Text.t("Не удалось сохранить плагин") + ": " + Text.t("выберите плагин ещё раз")); return; }
        Net.away("plugin save", () -> {
            String said;
            try {
                // the package is made in memory first: a half-written file never takes the place of a good one
                java.io.ByteArrayOutputStream packed = new java.io.ByteArrayOutputStream();
                PluginEngine.pack(id, packed);
                if (packed.size() < 22) throw new java.io.IOException(Text.t("Плагин не найден"));
                OutputStream out;
                // "wt" truncates; some places (Downloads, cloud drives) only take "w"
                try { out = activity.getContentResolver().openOutputStream(uri, "wt"); }
                catch (Exception unsupported) { out = activity.getContentResolver().openOutputStream(uri, "w"); }
                if (out == null) throw new java.io.IOException(Text.t("Не удалось открыть файл"));
                try { packed.writeTo(out); out.flush(); } finally { out.close(); }
                said = Text.t("Плагин сохранён") + " · " + (packed.size() / 1024) + " КБ";
            } catch (Exception error) {
                said = Text.t("Не удалось сохранить плагин") + ": " + error.getMessage();
            }
            final String message = said;
            activity.runOnUiThread(() -> Screen.say(message));
        });
    }
}
