package cat.ttcuz.framework;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The plugin store and the plugin editor, both as Material 3 sheets.
 *
 * The store lists the reviewed plugins for this TikTok as cards: an icon, the name, version and author, a
 * "Verified" chip, what it does, and one button that follows the install (Install, Installing…, Installed,
 * Update, Retry). Packages come only from the mod's own server over https and must match their SHA-256.
 *
 * The editor keeps a draft per account: the manifest as outlined fields, the Java files as chips, the selected
 * file in a code card. The draft saves itself half a second after the last change. From there it is built and
 * installed on the phone (PluginForge), exported as a source ZIP for python -m ttcuz.plugin, or sent for review.
 */
final class PluginWorkbench {
    static final int EXPORT = 3072;
    private static final String ROOT = Server.ROOT;


    private PluginWorkbench() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("ttcuz_plugin_editor", 0);
    }

    private static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    // ------------------------------------------------------------------ MD3 pieces

    private static int surface(Skin skin, float amount) { return Expressive.mix(skin.card, skin.text, amount); }

    private static GradientDrawable shape(int colour, float radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(colour);
        shape.setCornerRadius(radius);
        return shape;
    }

    private static TextView label(Context c, Skin skin, String words, float size, boolean strong, float alpha) {
        TextView view = new TextView(c);
        view.setText(words);
        view.setTextColor(skin.text);
        view.setAlpha(alpha);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        if (strong) view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        view.setLineSpacing(0, 1.1f);
        return view;
    }

    /** A pill: filled in the accent for the main action, tonal otherwise. */
    private static TextView pill(Context c, Skin skin, String words, boolean filled) {
        TextView pill = new TextView(c);
        pill.setText(words);
        pill.setGravity(Gravity.CENTER);
        pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        pill.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        pill.setPadding(dp(c, 20), dp(c, 10), dp(c, 20), dp(c, 10));
        pill.setMinHeight(dp(c, 40));
        int accent = Accent.colour();
        paintPill(c, skin, pill, filled, accent);
        pill.setFocusable(true);
        Motion.touch(pill);
        return pill;
    }

    private static void paintPill(Context c, Skin skin, TextView pill, boolean filled, int accent) {
        if (filled) {
            pill.setTextColor(onColour(accent));
            pill.setBackground(Expressive.tile(c, accent, 0x33FFFFFF, dp(c, 20), dp(c, 20), dp(c, 10)));
        } else {
            pill.setTextColor(skin.text);
            pill.setBackground(Expressive.tile(c, Expressive.mix(skin.card, accent, 0.18f),
                    (accent & 0xFFFFFF) | 0x33000000, dp(c, 20), dp(c, 20), dp(c, 10)));
        }
    }

    private static int onColour(int colour) {
        int r = (colour >> 16) & 255, g = (colour >> 8) & 255, b = colour & 255;
        return (r * 299 + g * 587 + b * 114) / 1000 > 150 ? 0xFF111111 : 0xFFFFFFFF;
    }

    /** A small rounded chip, e.g. "Verified" or a file name; selected ones are tonal in the accent. */
    private static TextView chip(Context c, Skin skin, String words, boolean selected) {
        TextView chip = new TextView(c);
        chip.setText(words);
        chip.setSingleLine(true);
        chip.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        chip.setPadding(dp(c, 12), dp(c, 6), dp(c, 12), dp(c, 6));
        chip.setMaxWidth(dp(c, 220));
        markChip(c, skin, chip, selected);
        return chip;
    }

    private static void markChip(Context c, Skin skin, TextView chip, boolean selected) {
        int accent = Accent.colour();
        GradientDrawable shape = shape(selected ? Expressive.mix(skin.card, accent, 0.30f) : 0, dp(c, 8));
        if (!selected) shape.setStroke(Math.max(1, dp(c, 1)), surface(skin, 0.28f));
        chip.setBackground(shape);
        chip.setTextColor(skin.text);
    }

    /** An outlined text field with its label above, MD3 style. */
    private static EditText field(Context c, Skin skin, LinearLayout into, String title, String value, boolean multi) {
        TextView head = label(c, skin, title, 12, true, 0.72f);
        head.setPadding(dp(c, 4), dp(c, 10), 0, dp(c, 4));
        into.addView(head);
        EditText input = new EditText(c);
        input.setText(value);
        input.setTextColor(skin.text);
        input.setHintTextColor((skin.text & 0xFFFFFF) | 0x66000000);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        input.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(multi ? 600 : 120)});
        if (multi) { input.setMinLines(2); input.setGravity(Gravity.TOP | Gravity.START); }
        else input.setSingleLine(true);
        GradientDrawable outline = shape(surface(skin, 0.03f), dp(c, 12));
        outline.setStroke(Math.max(1, dp(c, 1)), surface(skin, 0.30f));
        input.setBackground(outline);
        input.setOnFocusChangeListener((v, focused) ->
                outline.setStroke(dp(c, focused ? 2 : 1), focused ? Accent.colour() : surface(skin, 0.30f)));
        into.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return input;
    }

    private static LinearLayout column(Context c) {
        LinearLayout column = new LinearLayout(c);
        column.setOrientation(LinearLayout.VERTICAL);
        return column;
    }

    private static TextWatcher watch(final Runnable changed) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) {}
            @Override public void afterTextChanged(Editable s) { changed.run(); }
        };
    }

    // ------------------------------------------------------------------ the store

    /** The store as a window of its own: a top bar with a way back, then the plugins as cards. */
    static void store(final Activity activity, final Skin skin) { storeAt(activity, skin, null, false); }

    /**
     * The store; with focus, it scrolls to that plugin once loaded and offers to install it, or (review) opens its
     * reviews. A link from the store page in a browser (ttcuz://plugin/<id>) comes here.
     */
    static void storeAt(final Activity activity, final Skin skin, final String focus, final boolean review) {
        final android.app.Dialog window = new android.app.Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar);
        LinearLayout page = column(activity);
        page.setBackgroundColor(skin.page);
        page.setFitsSystemWindows(true);

        LinearLayout bar = new LinearLayout(activity);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 16), dp(activity, 8));
        ImageView back = new ImageView(activity);
        back.setImageDrawable(Symbols.drawable("arrow_forward", skin.text));
        back.setScaleX(-1f); // forward, mirrored: the way back
        int p = dp(activity, 12);
        back.setPadding(p, p, p, p);
        back.setBackground(Expressive.tile(activity, surface(skin, 0.06f), (Accent.colour() & 0xFFFFFF) | 0x33000000, dp(activity, 24), dp(activity, 24), dp(activity, 12)));
        back.setContentDescription(Text.CLOSE);
        back.setOnClickListener(v -> window.dismiss());
        Motion.touch(back);
        bar.addView(back, new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)));
        LinearLayout titles = column(activity);
        titles.setPadding(dp(activity, 14), 0, 0, 0);
        titles.addView(label(activity, skin, Text.t("Магазин плагинов"), 22, true, 1f));
        titles.addView(label(activity, skin, Text.t("Проверенные плагины для TikTok") + " " + PluginPackage.tiktokVersion(), 13, false, 0.7f));
        bar.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        // the mod's owner publishes straight to the store: a project ZIP (built here) or a ready .ctt
        final boolean owner = AdminAccess.current();
        if (owner) {
            ImageView publish = new ImageView(activity);
            publish.setImageDrawable(Symbols.drawable("publish", onColour(Accent.colour())));
            publish.setPadding(p, p, p, p);
            publish.setBackground(Expressive.tile(activity, Accent.colour(), 0x33FFFFFF, dp(activity, 24), dp(activity, 24), dp(activity, 12)));
            publish.setContentDescription(Text.t("Опубликовать в магазин"));
            publish.setTooltipText(Text.t("Опубликовать в магазин"));
            Motion.touch(publish);
            publish.setOnClickListener(v -> {
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
                        .putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/x-zip-compressed", "application/octet-stream"});
                try { activity.startActivityForResult(pick, PICK_PUBLISH); }
                catch (RuntimeException error) { Screen.say(Text.t("Не удалось открыть файлы")); }
            });
            bar.addView(publish, new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)));
        }
        page.addView(bar);

        final LinearLayout list = column(activity);
        list.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 24));
        android.widget.ScrollView scroll = new android.widget.ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        final TextView status = label(activity, skin, Text.t("Загрузка…"), 14, false, 0.8f);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(activity, 4), dp(activity, 32), dp(activity, 4), dp(activity, 16));
        list.addView(status);

        window.setContentView(page);
        if (window.getWindow() != null) {
            window.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(skin.page));
            window.getWindow().setStatusBarColor(skin.page);
            window.getWindow().setNavigationBarColor(skin.page);
            window.getWindow().setWindowAnimations(android.R.style.Animation_Activity);
        }
        Fonts.applyTree(page);

        final Runnable[] load = new Runnable[1];
        final boolean[] focused = {false};
        load[0] = () -> {
            list.removeAllViews();
            // the cards' shapes while the catalogue comes, not a line of text
            list.addView(Skeleton.storeCards(activity, skin, 3));
            Net.away("plugin catalog", () -> {
                try {
                    byte[] bytes = Net.bytes(ROOT + "/plugins/catalog.json");
                    if (bytes == null) throw new IOException(Text.t("Магазин недоступен. Повторите позже."));
                    loadMine(activity);
                    final JSONArray plugins = new JSONObject(new String(bytes, StandardCharsets.UTF_8)).optJSONArray("plugins");
                    activity.runOnUiThread(() -> {
                        show(activity, skin, list, status, plugins, owner, load[0]);
                        Fonts.applyTree(list);
                        if (focus != null && !focused[0]) { focused[0] = true; focusOn(activity, skin, scroll, list, plugins, focus, review); }
                    });
                } catch (final Exception error) {
                    activity.runOnUiThread(() -> {
                        if (activity.isFinishing()) return;
                        list.removeAllViews();
                        list.addView(status);
                        status.setText(error.getMessage() == null ? Text.t("Магазин недоступен. Повторите позже.") : error.getMessage());
                        TextView retry = pill(activity, skin, Text.t("Повторить"), true);
                        retry.setOnClickListener(v -> load[0].run());
                        LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        at.gravity = Gravity.CENTER_HORIZONTAL;
                        list.addView(retry, at);
                    });
                }
            });
        };
        openStore = new Store(activity, skin, load[0]);
        final Store opened = openStore;
        window.setOnDismissListener(d -> { if (openStore == opened) openStore = null; });
        window.show();
        load[0].run();
    }

    /** Scrolls to the plugin a link asked for, lights its card up, then offers to install it (or shows its reviews). */
    private static void focusOn(final Activity activity, final Skin skin, final android.widget.ScrollView scroll, LinearLayout list, JSONArray plugins, String id, boolean review) {
        JSONObject found = null;
        for (int i = 0; plugins != null && i < plugins.length(); i++) {
            JSONObject p = plugins.optJSONObject(i);
            if (p != null && id.equals(p.optString("id"))) found = p;
        }
        if (found == null) { Screen.say(Text.t("Такого плагина нет в магазине")); return; }
        final JSONObject plugin = found;
        final View card = list.findViewWithTag("plugin:" + id);
        final TextView button = (TextView) list.findViewWithTag("install:" + id);
        if (card != null) scroll.postDelayed(() -> {
            scroll.smoothScrollTo(0, Math.max(0, card.getTop() - dp(activity, 12)));
            card.animate().scaleX(1.03f).scaleY(1.03f).setDuration(Motion.time(180L)).withEndAction(() ->
                    card.animate().scaleX(1f).scaleY(1f).setDuration(Motion.time(320L)).setInterpolator(Motion.OUT).start()).start();
        }, 450L);
        scroll.postDelayed(() -> {
            if (activity.isFinishing()) return;
            if (review) { reviews(activity, skin, plugin, null); return; }
            String installed = installedVersion(id);
            if (installed != null && installed.equals(plugin.optString("version"))) { Screen.say(Text.t("Этот плагин уже установлен")); return; }
            preview(activity, skin, plugin, button);
        }, 900L);
    }

    // ------------------------------------------------------------------ reviews

    /**
     * Thumbs up / down under someone's review, with their counts. A tap on the lit one takes the vote back. The count changes
     * at once and the server is told after; if it refuses, the old state comes back.
     */
    private static View votes(final Activity activity, final Skin skin, final String plugin, final JSONObject review) {
        LinearLayout row = new LinearLayout(activity);
        row.setPadding(0, dp(activity, 8), 0, 0);
        final int[] state = {review.optInt("my"), review.optInt("up"), review.optInt("dn")};
        final TextView up = label(activity, skin, "", 13, true, 1f), down = label(activity, skin, "", 13, true, 1f);
        final int icon = dp(activity, 18);
        final Runnable paint = () -> {
            // Google's own thumb icons, in the accent when it is this person's vote
            for (int k = 0; k < 2; k++) {
                TextView chip = k == 0 ? up : down;
                boolean lit = state[0] == (k == 0 ? 1 : -1);
                int ink = lit ? Accent.colour() : (skin.text & 0xFFFFFF) | 0xB3000000;
                android.graphics.drawable.Drawable glyph = Symbols.drawable(k == 0 ? "thumb_up" : "thumb_down", ink);
                if (glyph != null) glyph.setBounds(0, 0, icon, icon);
                chip.setCompoundDrawables(glyph, null, null, null);
                chip.setCompoundDrawablePadding(dp(activity, 6));
                chip.setText(String.valueOf(state[k == 0 ? 1 : 2]));
                chip.setTextColor(ink);
                chip.setGravity(Gravity.CENTER_VERTICAL);
                chip.setBackground(shape(surface(skin, lit ? 0.22f : 0.08f), dp(activity, 14)));
            }
        };
        for (TextView chip : new TextView[]{up, down}) {
            chip.setPadding(dp(activity, 12), dp(activity, 5), dp(activity, 12), dp(activity, 5));
            LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            at.rightMargin = dp(activity, 8);
            row.addView(chip, at);
            Motion.touch(chip);
        }
        paint.run();
        final java.util.function.IntConsumer vote = wanted -> {
            final int[] before = state.clone();
            int value = state[0] == wanted ? 0 : wanted;
            if (state[0] == 1) state[1]--; else if (state[0] == -1) state[2]--;
            if (value == 1) state[1]++; else if (value == -1) state[2]++;
            state[0] = value;
            paint.run();
            if (Motion.haptics()) row.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            Net.away("review vote", () -> {
                String error = null;
                try {
                    String uid = Account.activeId();
                    if (uid == null || uid.isEmpty()) throw new IOException(Text.t("Войдите в аккаунт TikTok"));
                    JSONObject who = new CloudProfileProvider(activity).identity(uid);
                    JSONObject body = new JSONObject().put("uid", uid).put("token", who.optString("token")).put("ref", review.optString("ref")).put("value", value)
                            .put("handle", activity.getSharedPreferences(Ttcuz.PREFS, 0).getString("profile_preview_handle", ""));
                    String raw = Net.postResult(ROOT + "/plugins/reviews/" + Uri.encode(plugin) + "/vote", body.toString());
                    JSONObject answer = raw == null ? null : new JSONObject(raw);
                    if (answer == null || !answer.optBoolean("ok")) error = answer == null ? Text.t("Сервер не ответил") : answer.optString("hint", Text.t("Не удалось оценить"));
                } catch (Exception failure) { error = failure.getMessage(); }
                final String said = error;
                if (said != null) activity.runOnUiThread(() -> { System.arraycopy(before, 0, state, 0, 3); paint.run(); Screen.say(said); });
            });
        };
        up.setOnClickListener(v -> vote.accept(1));
        down.setOnClickListener(v -> vote.accept(-1));
        return row;
    }


    /** The count beside the reviews bubble under a card; nothing while there are none. */
    static String reviewsLabel(int count) { return count == 0 ? "" : String.valueOf(count); }

    /**
     * Likes and dislikes of a plugin with their counts, and its reviews, as one quiet row under its card. The counts come
     * with the catalog (no request per plugin); a tap counts at once and tells the server after, taking it back when the
     * server refuses. This phone's own vote is kept on the phone.
     */
    private static View reactions(final Activity activity, final Skin skin, final JSONObject plugin) {
        final String id = plugin.optString("id");
        final SharedPreferences prefs = activity.getSharedPreferences(Ttcuz.PREFS, 0);
        JSONObject rating = plugin.optJSONObject("rating");
        final int[] state = {prefs.getInt("plugin_like:" + id, 0), rating == null ? 0 : rating.optInt("up"), rating == null ? 0 : rating.optInt("dn")};
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        final TextView up = label(activity, skin, "", 13, true, 1f), down = label(activity, skin, "", 13, true, 1f);
        final TextView talk = label(activity, skin, reviewsLabel(rating == null ? 0 : rating.optInt("count")), 13, true, 1f);
        talk.setTag("reviews:" + id);
        final int icon = dp(activity, 17);
        final Runnable paint = () -> {
            for (int k = 0; k < 2; k++) {
                TextView chip = k == 0 ? up : down;
                boolean lit = state[0] == (k == 0 ? 1 : -1);
                int ink = lit ? Accent.colour() : (skin.text & 0xFFFFFF) | 0xA6000000;
                android.graphics.drawable.Drawable glyph = Symbols.drawable(k == 0 ? "thumb_up" : "thumb_down", ink);
                if (glyph != null) glyph.setBounds(0, 0, icon, icon);
                chip.setCompoundDrawables(glyph, null, null, null);
                int n = state[k == 0 ? 1 : 2];
                chip.setText(n == 0 ? "" : String.valueOf(n));
                chip.setCompoundDrawablePadding(n == 0 ? 0 : dp(activity, 6));
                chip.setTextColor(ink);
                chip.setBackground(shape(lit ? (Accent.colour() & 0xFFFFFF) | 0x26000000 : surface(skin, 0.05f), dp(activity, 14)));
            }
        };
        for (TextView chip : new TextView[]{up, down, talk}) {
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setPadding(dp(activity, 10), dp(activity, 6), dp(activity, 10), dp(activity, 6));
            LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            at.rightMargin = dp(activity, 6);
            row.addView(chip, at);
            Motion.touch(chip);
        }
        up.setContentDescription(Text.t("Нравится"));
        down.setContentDescription(Text.t("Не нравится"));
        int muted = (skin.text & 0xFFFFFF) | 0xA6000000;
        talk.setTextColor(muted);
        talk.setBackground(shape(surface(skin, 0.05f), dp(activity, 14)));
        talk.setContentDescription(Text.t("Отзывы"));
        android.graphics.drawable.Drawable bubble = Symbols.drawable("chat_bubble", muted);
        if (bubble != null) bubble.setBounds(0, 0, icon, icon);
        talk.setCompoundDrawables(bubble, null, null, null);
        talk.setCompoundDrawablePadding(talk.length() == 0 ? 0 : dp(activity, 6));
        talk.setOnClickListener(v -> reviews(activity, skin, plugin, talk));
        paint.run();
        final java.util.function.IntConsumer vote = wanted -> {
            final int[] before = state.clone();
            final int value = state[0] == wanted ? 0 : wanted;
            if (state[0] == 1) state[1]--; else if (state[0] == -1) state[2]--;
            if (value == 1) state[1]++; else if (value == -1) state[2]++;
            state[0] = value;
            paint.run();
            prefs.edit().putInt("plugin_like:" + id, value).apply();
            if (Motion.haptics()) row.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            Net.away("plugin like", () -> {
                String error = null;
                try {
                    String uid = Account.activeId();
                    if (uid == null || uid.isEmpty()) throw new IOException(Text.t("Войдите в аккаунт TikTok"));
                    JSONObject who = new CloudProfileProvider(activity).identity(uid);
                    String raw = Net.postResult(ROOT + "/plugins/reviews/" + Uri.encode(id) + "/like",
                            new JSONObject().put("uid", uid).put("token", who.optString("token")).put("value", value).toString());
                    JSONObject answer = raw == null ? null : new JSONObject(raw);
                    if (answer == null || !answer.optBoolean("ok")) error = answer == null ? Text.t("Сервер не ответил") : answer.optString("hint", Text.t("Не удалось оценить") + " (" + answer.optString("error", "?") + ")");
                    else {
                        final int u = answer.optInt("up", state[1]), d = answer.optInt("dn", state[2]);
                        activity.runOnUiThread(() -> { state[1] = u; state[2] = d; paint.run(); });
                    }
                } catch (Exception failure) { error = failure.getMessage(); }
                final String said = error;
                if (said != null) activity.runOnUiThread(() -> {
                    System.arraycopy(before, 0, state, 0, 3);
                    prefs.edit().putInt("plugin_like:" + id, before[0]).apply();
                    paint.run();
                    Screen.say(said);
                });
            });
        };
        up.setOnClickListener(v -> vote.accept(1));
        down.setOnClickListener(v -> vote.accept(-1));
        return row;
    }

    // ------------------------------------------------------------------ statistics (the plugin's author only)

    /**
     * A plugin's statistics, for its author (and the mod's creator): downloads, likes and dislikes, stars and today's
     * downloads as four tiles, then the days as charts in the manner of Telegram's (7, 30 or 90 days).
     * The server refuses anyone else; the button is only shown to the author anyway.
     */
    static void stats(final Activity activity, final Skin skin, final JSONObject plugin) {
        final android.app.Dialog window = new android.app.Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar);
        LinearLayout page = column(activity);
        page.setBackgroundColor(skin.page);
        page.setFitsSystemWindows(true);
        LinearLayout bar = new LinearLayout(activity);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 16), dp(activity, 8));
        ImageView back = new ImageView(activity);
        back.setImageDrawable(Symbols.drawable("arrow_forward", skin.text));
        back.setScaleX(-1f);
        int p = dp(activity, 12);
        back.setPadding(p, p, p, p);
        back.setBackground(Expressive.tile(activity, surface(skin, 0.06f), (Accent.colour() & 0xFFFFFF) | 0x33000000, dp(activity, 24), dp(activity, 24), dp(activity, 12)));
        back.setContentDescription(Text.CLOSE);
        back.setOnClickListener(v -> window.dismiss());
        Motion.touch(back);
        bar.addView(back, new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)));
        LinearLayout titles = column(activity);
        titles.setPadding(dp(activity, 14), 0, 0, 0);
        titles.addView(label(activity, skin, Text.t("Статистика"), 22, true, 1f));
        titles.addView(label(activity, skin, plugin.optString("name", plugin.optString("id")), 13, false, 0.7f));
        bar.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        page.addView(bar);
        final LinearLayout list = column(activity);
        list.setPadding(dp(activity, 16), dp(activity, 4), dp(activity, 16), dp(activity, 28));
        android.widget.ScrollView scroll = new android.widget.ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        final TextView status = label(activity, skin, Text.t("Загрузка…"), 14, false, 0.8f);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(activity, 40), 0, 0);
        list.addView(status);
        window.setContentView(page);
        if (window.getWindow() != null) {
            window.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(skin.page));
            window.getWindow().setStatusBarColor(skin.page);
            window.getWindow().setNavigationBarColor(skin.page);
            window.getWindow().setWindowAnimations(android.R.style.Animation_Activity);
        }
        window.show();
        final String id = plugin.optString("id");
        Net.away("plugin stats", () -> {
            String error = null;
            JSONObject answer = null;
            try {
                String uid = Account.activeId();
                if (uid == null || uid.isEmpty()) throw new IOException(Text.t("Войдите в аккаунт TikTok"));
                JSONObject who = new CloudProfileProvider(activity).identity(uid);
                String raw = Net.postResult(ROOT + "/plugins/stats/" + Uri.encode(id), new JSONObject().put("uid", uid).put("token", who.optString("token")).toString());
                answer = raw == null ? null : new JSONObject(raw);
                if (answer == null) error = Text.t("Сервер не ответил");
                else if (!answer.optBoolean("ok")) error = answer.optString("hint", Text.t("Статистика недоступна") + " (" + answer.optString("error", "?") + ")");
            } catch (Exception failure) { error = failure.getMessage(); }
            final String said = error;
            final JSONObject data = answer;
            activity.runOnUiThread(() -> {
                if (!window.isShowing()) return;
                if (said != null) { status.setText(said); return; }
                list.removeAllViews();
                fillStats(activity, skin, list, data);
                Fonts.applyTree(list);
            });
        });
    }

    private static final int GOOD = 0xFF34C759, BAD = 0xFFFF453A;

    private static void fillStats(final Activity activity, final Skin skin, LinearLayout list, JSONObject data) {
        JSONArray days = data.optJSONArray("days");
        int n = days == null ? 0 : days.length();
        long[] dayIds = new long[n];
        float[] downloads = new float[n], likes = new float[n], dislikes = new float[n];
        for (int i = 0; i < n; i++) {
            JSONObject d = days.optJSONObject(i);
            if (d == null) continue;
            dayIds[i] = d.optLong("day");
            downloads[i] = d.optInt("downloads");
            likes[i] = d.optInt("likes");
            dislikes[i] = d.optInt("dislikes");
        }
        int week = 0;
        for (int i = Math.max(0, n - 7); i < n; i++) week += (int) downloads[i];
        int up = data.optInt("up"), dn = data.optInt("dn"), reviews = data.optInt("reviews");
        // four tiles, two by two
        LinearLayout row1 = new LinearLayout(activity), row2 = new LinearLayout(activity);
        row1.addView(tile(activity, skin, Text.t("Скачивания"), TrendChart.short_(data.optInt("downloads")), "+" + week + " " + Text.t("за 7 дней"), -1f), tileAt(activity, false));
        row1.addView(tile(activity, skin, Text.t("Оценки"), up + " / " + dn, Text.t("нравится / нет"), up + dn == 0 ? -1f : up / (float) (up + dn)), tileAt(activity, true));
        row2.addView(tile(activity, skin, Text.t("Отзывы"), reviews == 0 ? "—" : "★ " + data.optDouble("average"), reviews + " " + plural(reviews, Text.t("отзыв"), Text.t("отзыва"), Text.t("отзывов")), -1f), tileAt(activity, false));
        int today = n == 0 ? 0 : (int) downloads[n - 1], month = 0;
        for (int i = Math.max(0, n - 30); i < n; i++) month += (int) downloads[i];
        row2.addView(tile(activity, skin, Text.t("Сегодня"), String.valueOf(today), month + " " + Text.t("за 30 дней"), -1f), tileAt(activity, true));
        list.addView(row1);
        LinearLayout.LayoutParams second = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        second.topMargin = dp(activity, 10);
        list.addView(row2, second);

        // the range, for both charts
        int text = skin.text, muted = (skin.text & 0xFFFFFF) | 0xA6000000;
        final TrendChart got = new TrendChart(activity, text, muted, skin.card);
        got.set(dayIds, new TrendChart.Series(Text.t("Скачивания"), Accent.colour(), downloads));
        final TrendChart liked = new TrendChart(activity, text, muted, skin.card);
        liked.set(dayIds, new TrendChart.Series(Text.t("Нравится"), GOOD, likes), new TrendChart.Series(Text.t("Не нравится"), BAD, dislikes));
        LinearLayout ranges = new LinearLayout(activity);
        final int[] spans = {7, 30, 90};
        final TextView[] chips = new TextView[spans.length];
        for (int i = 0; i < spans.length; i++) {
            final int span = spans[i];
            TextView chip = pill(activity, skin, span + " " + Text.t("дней"), span == 30);
            chips[i] = chip;
            chip.setOnClickListener(v -> {
                for (int k = 0; k < spans.length; k++) paintPill(activity, skin, chips[k], spans[k] == span, Accent.colour());
                got.range(span);
                liked.range(span);
            });
            LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            at.rightMargin = dp(activity, 8);
            ranges.addView(chip, at);
        }
        got.range(30);
        liked.range(30);
        LinearLayout.LayoutParams rangesAt = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rangesAt.topMargin = dp(activity, 18);
        list.addView(ranges, rangesAt);
        list.addView(chartCard(activity, skin, Text.t("Скачивания по дням"), got));
        list.addView(chartCard(activity, skin, Text.t("Лайки и дизлайки"), liked));

        if (Motion.enabled(activity)) for (int i = 0; i < list.getChildCount(); i++) {
            View one = list.getChildAt(i);
            one.setAlpha(0f);
            one.setTranslationY(dp(activity, 16));
            one.animate().alpha(1f).translationY(0f).setStartDelay(40L * i).setDuration(320L).setInterpolator(Motion.EMPHASIZED).start();
        }
    }

    private static LinearLayout.LayoutParams tileAt(Context c, boolean right) {
        LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (right) at.leftMargin = dp(c, 10);
        return at;
    }

    /** A tile: what, the big number, a line under it; with a share 0..1, a green/red bar of likes to dislikes. */
    private static View tile(Context c, Skin skin, String what, String big, String under, float share) {
        LinearLayout tile = column(c);
        tile.setBackground(shape(surface(skin, 0.05f), dp(c, 22)));
        tile.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
        tile.addView(label(c, skin, what, 13, false, 0.7f));
        TextView number = label(c, skin, big, 24, true, 1f);
        number.setSingleLine(true);
        number.setEllipsize(TextUtils.TruncateAt.END);
        tile.addView(number);
        TextView small = label(c, skin, under, 12, false, 0.65f);
        small.setSingleLine(true);
        small.setEllipsize(TextUtils.TruncateAt.END);
        tile.addView(small);
        if (share >= 0f) {
            LinearLayout bar = new LinearLayout(c);
            View good = new View(c), bad = new View(c);
            good.setBackground(shape(GOOD, dp(c, 3)));
            bad.setBackground(shape(BAD, dp(c, 3)));
            bar.addView(good, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, Math.max(.001f, share)));
            LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, Math.max(.001f, 1f - share));
            gap.leftMargin = dp(c, 3);
            bar.addView(bad, gap);
            LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 6));
            at.topMargin = dp(c, 8);
            tile.addView(bar, at);
        }
        return tile;
    }

    private static View chartCard(Context c, Skin skin, String title, TrendChart chart) {
        LinearLayout card = column(c);
        card.setBackground(shape(surface(skin, 0.05f), dp(c, 22)));
        card.setPadding(dp(c, 12), dp(c, 14), dp(c, 12), dp(c, 8));
        TextView head = label(c, skin, title, 16, true, 1f);
        head.setPadding(dp(c, 4), 0, 0, dp(c, 6));
        card.addView(head);
        card.addView(chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 220)));
        LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        at.topMargin = dp(c, 12);
        card.setLayoutParams(at);
        return card;
    }

    static String plural(int n, String one, String few, String many) {
        int t = n % 100, u = n % 10;
        return t > 10 && t < 20 ? many : u == 1 ? one : u >= 2 && u <= 4 ? few : many;
    }

    /**
     * A plugin's reviews: every review, and this account's own one to give or change -- a few words (the likes of the
     * plugin itself are on its card). The server ties a review to the account (its cloud profile), one per account.
     */
    static void reviews(final Activity activity, final Skin skin, final JSONObject plugin, final TextView line) {
        final String id = plugin.optString("id");
        final Panel panel = Panel.with(activity, skin, plugin.optString("name", id));
        panel.subtitle(Text.t("Отзывы"));
        final LinearLayout box = column(activity);
        final TextView summary = label(activity, skin, Text.t("Загрузка…"), 14, false, 0.8f);
        box.addView(summary);
        // the composer: stars, words, send
        final int[] chosen = {0};
        final Runnable paint = () -> { };
        paint.run();
        // the server still keeps stars with each review; the store no longer shows them
        chosen[0] = 5;
        final EditText words = field(activity, skin, box, Text.t("Ваш отзыв"), "", true);
        words.setFilters(new InputFilter[]{new InputFilter.LengthFilter(600)});
        final LinearLayout list = column(activity);
        list.setPadding(0, dp(activity, 10), 0, 0);
        box.addView(list);
        panel.view(box);

        final java.util.function.Consumer<JSONObject> showAll = answer -> {
            int count = answer.optInt("count");
            summary.setText(count == 0 ? Text.t("Отзывов пока нет. Будьте первым!") : count + " " + Text.t(plural(count, "отзыв", "отзыва", "отзывов")));
            JSONObject mine = answer.optJSONObject("mine");
            if (mine != null && words.length() == 0) words.setText(mine.optString("text"));
            list.removeAllViews();
            JSONArray all = answer.optJSONArray("reviews");
            for (int i = 0; all != null && i < all.length(); i++) {
                JSONObject r = all.optJSONObject(i);
                LinearLayout one = column(activity);
                one.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
                one.setBackground(shape(surface(skin, r.optBoolean("mine") ? 0.12f : 0.06f), dp(activity, 18)));
                String who = r.optString("handle").isEmpty() ? Text.t("Пользователь ttcuz") : "@" + r.optString("handle");
                one.addView(label(activity, skin, who + (r.optBoolean("mine") ? " · " + Text.t("вы") : ""), 14, true, 1f));
                if (!r.optString("text").isEmpty()) {
                    TextView t = label(activity, skin, r.optString("text"), 14, false, 0.86f);
                    t.setPadding(0, dp(activity, 6), 0, 0);
                    one.addView(t);
                }
                if (!r.optBoolean("mine")) one.addView(votes(activity, skin, id, r));
                LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                at.topMargin = dp(activity, 8);
                list.addView(one, at);
            }
            Fonts.applyTree(list);
            // the store card shows the new count at once
            if (line != null) { line.setText(reviewsLabel(count)); line.setCompoundDrawablePadding(count == 0 ? 0 : dp(activity, 6)); }
        };
        final String me = Account.activeId();
        Net.away("plugin reviews", () -> {
            try {
                byte[] bytes = Net.bytes(ROOT + "/plugins/reviews/" + Uri.encode(id) + (me == null || me.isEmpty() ? "" : "?uid=" + Uri.encode(me)));
                if (bytes == null) throw new IOException(Text.t("Сервер не ответил"));
                final JSONObject answer = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
                activity.runOnUiThread(() -> showAll.accept(answer));
            } catch (final Exception error) {
                activity.runOnUiThread(() -> summary.setText(Text.t("Не удалось загрузить отзывы")));
            }
        });
        final java.util.function.IntConsumer send = stars0 -> {
            if (stars0 < 0 && words.getText().toString().trim().isEmpty()) { panel.busy(false, Text.t("Напишите пару слов")); return; }
            panel.busy(true, stars0 == 0 ? Text.t("Удаление…") : Text.t("Отправка…"));
            final String text = words.getText().toString();
            Net.away("plugin review", () -> {
                try {
                    String uid = Account.activeId();
                    if (uid == null || uid.isEmpty()) throw new IOException(Text.t("Войдите в аккаунт TikTok"));
                    JSONObject who = new CloudProfileProvider(activity).identity(uid);
                    JSONObject body = new JSONObject().put("uid", uid).put("token", who.optString("token")).put("stars", stars0 < 0 ? chosen[0] : 0)
                            .put("text", text).put("handle", activity.getSharedPreferences(Ttcuz.PREFS, 0).getString("profile_preview_handle", ""));
                    String raw = Net.postLarge(ROOT + "/plugins/reviews/" + Uri.encode(id), body.toString());
                    if (raw == null) throw new IOException(Text.t("Сервер не ответил"));
                    final JSONObject answer = new JSONObject(raw);
                    if (answer.has("error")) throw new IOException(answer.optString("hint", answer.optString("error")));
                    activity.runOnUiThread(() -> {
                        if (stars0 == 0) words.setText("");
                        showAll.accept(answer);
                        panel.busy(false, stars0 == 0 ? Text.t("Отзыв удалён") : Text.t("Спасибо! Отзыв сохранён"));
                    });
                } catch (final Exception error) {
                    activity.runOnUiThread(() -> panel.busy(false, String.valueOf(error.getMessage())));
                }
            });
        };
        panel.primaryKeepOpen(Text.t("Отправить"), () -> send.accept(-1));
        panel.keepOpen(Text.t("Удалить мой отзыв"), () -> send.accept(0));
        panel.quiet(Text.CLOSE, null);
        panel.show();
    }

    static final int PICK_PUBLISH = 3076;
    /** The store on screen, for the owner's publish (its file comes back through the activity). */
    private static Store openStore;
    private static final class Store {
        final Activity activity; final Skin skin; final Runnable reload;
        Store(Activity activity, Skin skin, Runnable reload) { this.activity = activity; this.skin = skin; this.reload = reload; }
    }

    /** A request the server accepts only from the owner's phone: the account and its key ride along. */
    private static JSONObject ownerCall(Activity activity, JSONObject body) throws Exception {
        if (!AdminAccess.current()) throw new IOException(Text.t("Только для владельца мода"));
        JSONObject who = new CloudProfileProvider(activity).identity(Account.activeId());
        body.put("uid", who.optString("uid")).put("token", who.optString("token"));
        String raw = Net.postLarge(ROOT + "/plugins/admin", body.toString());
        if (raw == null) throw new IOException(Text.t("Сервер не ответил"));
        JSONObject answer = new JSONObject(raw);
        if (answer.has("error")) throw new IOException(answer.optString("hint", answer.optString("error")));
        return answer;
    }

    /** The owner's publish: a .ctt as it is, or a project ZIP built here first; then straight into the store. */
    private static void publish(final Store store, final Uri uri) {
        final Activity activity = store.activity;
        final Panel panel = Panel.with(activity, store.skin, Text.t("Публикация в магазин"));
        panel.text(Text.t("Плагин сразу появится в магазине с вашим ником и отметкой «Проверен»."));
        panel.quiet(Text.CLOSE, null);
        panel.show();
        panel.busy(true, Text.t("Чтение файла…"));
        Net.away("plugin publish", () -> {
            File built = null;
            try {
                byte[] bytes = readAll(activity, uri, 4 * 1048576);
                java.util.Map<String, byte[]> top = new java.util.HashMap<>();
                try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
                    for (java.util.zip.ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                        String name = entry.getName();
                        if (name.equals("manifest.json") || name.equals("classes.dex") || name.equals("icon.png")) top.put(name, readStream(zip, 2 * 1048576));
                    }
                }
                JSONObject manifest; byte[] archive; String icon = "";
                if (top.containsKey("manifest.json") && top.containsKey("classes.dex")) {
                    manifest = new JSONObject(new String(top.get("manifest.json"), StandardCharsets.UTF_8));
                    archive = bytes;
                    if (top.containsKey("icon.png")) icon = pngForStore(top.get("icon.png"));
                } else {
                    activity.runOnUiThread(() -> panel.busy(true, Text.t("Сборка из исходников…")));
                    JSONObject draft = importZip(activity, uri);
                    validate(draft);
                    built = PluginForge.build(activity, draft, words -> activity.runOnUiThread(() -> panel.busy(true, words)));
                    archive = readAll(activity, Uri.fromFile(built), 2 * 1048576);
                    manifest = draft.getJSONObject("metadata");
                    icon = draft.optString("icon");
                }
                if (archive.length > 1048576) throw new IOException(Text.t("Пакет больше 1 МБ"));
                String handle = activity.getSharedPreferences(Ttcuz.PREFS, 0).getString("profile_preview_handle", "");
                String author = manifest.optString("author").trim();
                JSONObject metadata = new JSONObject()
                        .put("id", manifest.getString("id"))
                        .put("name", manifest.optString("name", manifest.getString("id")).trim())
                        .put("version", manifest.optString("version", "1.0").trim())
                        .put("author", author.isEmpty() ? (handle.isEmpty() ? "ttcuz" : handle) : author)
                        .put("about", clip(manifest.optString("description", manifest.optString("about")).trim(), 240))
                        .put("tiktok", clip(manifest.optString("tiktok"), 32))
                        .put("permissions", manifest.optJSONArray("permissions") == null ? new JSONArray() : manifest.optJSONArray("permissions"));
                activity.runOnUiThread(() -> panel.busy(true, Text.t("Отправка…")));
                JSONObject body = new JSONObject().put("action", "publish-package").put("metadata", metadata)
                        .put("archive", android.util.Base64.encodeToString(archive, android.util.Base64.NO_WRAP)).put("by", handle);
                if (!icon.isEmpty()) body.put("icon", icon);
                ownerCall(activity, body);
                final String name = metadata.optString("name");
                activity.runOnUiThread(() -> { panel.busy(false, Text.t("Опубликован") + ": " + name); store.reload.run(); });
            } catch (final Throwable error) {
                Diary.note("plugin publish: " + error);
                final String why = error.getMessage() == null ? error.toString() : error.getMessage();
                activity.runOnUiThread(() -> panel.busy(false, Text.t("Не опубликован") + ": " + why));
            } finally {
                if (built != null) built.delete();
            }
        });
    }

    private static String clip(String text, int max) { return text == null ? "" : text.length() <= max ? text : text.substring(0, max); }

    private static byte[] readAll(Context context, Uri uri, int limit) throws IOException {
        try (java.io.InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException(Text.t("Файл не открывается"));
            return readStream(in, limit);
        }
    }

    private static byte[] readStream(java.io.InputStream in, int limit) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[16384];
        for (int n; (n = in.read(buffer)) > 0; ) {
            out.write(buffer, 0, n);
            if (out.size() > limit) throw new IOException(Text.t("Файл слишком большой"));
        }
        return out.toByteArray();
    }

    /** The package's own picture, as the store takes it: a PNG of at most 32 KB, or a GIF of at most 96 KB. */
    private static String pngForStore(byte[] png) throws IOException {
        if (Gif.isGif(png) && png.length <= ICON_GIF_MAX) return android.util.Base64.encodeToString(png, android.util.Base64.NO_WRAP);
        if (png.length <= 32768 && png.length > 8 && (png[0] & 255) == 137 && png[1] == 'P')
            return android.util.Base64.encodeToString(png, android.util.Base64.NO_WRAP);
        Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(png, 0, png.length);
        return bitmap == null ? "" : iconFrom(bitmap);
    }

    /** The plugin's banner (3:1), fading at its bottom into the colour under it, or null when it has none. */
    static View bannerView(final Activity activity, JSONObject plugin, int under, int radius) {
        String path = plugin.optString("banner");
        return path.isEmpty() ? null : bannerFrame(activity, under, radius, into -> picture(activity, path, into));
    }

    /** A 3:1 frame whose picture melts into `under` at the bottom; `radius` rounds it (0: the parent clips it). */
    static android.widget.FrameLayout bannerFrame(final Activity activity, int under, int radius, java.util.function.Consumer<ImageView> fill) {
        android.widget.FrameLayout frame = new android.widget.FrameLayout(activity) {
            @Override protected void onMeasure(int w, int h) {
                int width = MeasureSpec.getSize(w);
                super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(width / 3, MeasureSpec.EXACTLY));
            }
        };
        frame.setBackgroundColor(Expressive.mix(under, Accent.colour(), 0.14f));
        if (radius > 0) { frame.setBackground(shape(Expressive.mix(under, Accent.colour(), 0.14f), radius)); frame.setClipToOutline(true); }
        else if (radius < 0) {
            // the top of a card: its two upper corners rounded like the card, the bottom straight (it melts away anyway)
            final float r = -radius;
            frame.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), Math.round(view.getHeight() + r), r);
                }
            });
            frame.setClipToOutline(true);
        }
        ImageView picture = new ImageView(activity);
        picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
        picture.setTag("banner");
        frame.addView(picture, new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View fade = new View(activity);
        fade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{under & 0xFFFFFF, under & 0xFFFFFF, (under & 0xFFFFFF) | 0x8C000000, under}));
        frame.addView(fade, new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        fill.accept(picture);
        return frame;
    }

    /**
     * A plugin's page in the app: its banner across the top, the picture and name, what it does, what it may do, its
     * stars -- and installing it from there.
     */
    static void preview(final Activity activity, final Skin skin, final JSONObject plugin, final TextView button) {
        String id = plugin.optString("id");
        Panel page = Panel.with(activity, skin, plugin.optString("name", id));
        page.subtitle(PluginList.byline(plugin.optString("version"), plugin.optString("author")));
        View banner = bannerView(activity, plugin, skin.card, dp(activity, 20));
        if (banner != null) page.view(banner);
        String about = plugin.optString("about");
        page.text(about.isEmpty() ? Text.t("Автор не добавил описание.") : about);
        if (!plugin.optString("changes").isEmpty()) page.text(Text.t("Что нового") + ": " + plugin.optString("changes"));
        JSONArray asked = plugin.optJSONArray("permissions");
        for (int i = 0; asked != null && i < asked.length(); i++) {
            String permission = asked.optString(i);
            if (PluginPermissions.sensitive(permission)) page.warning(PluginPermissions.icon(permission), PluginPermissions.title(permission), PluginPermissions.detail(permission));
            else page.info(PluginPermissions.icon(permission), PluginPermissions.title(permission), PluginPermissions.detail(permission), null);
        }
        String installed = installedVersion(id);
        boolean current = installed != null && installed.equals(plugin.optString("version"));
        if (!current && button != null) page.primary(installed == null ? Text.t("Установить") : Text.t("Обновить"), () -> { if (button.isEnabled()) install(activity, skin, plugin, button); });
        page.quiet(Text.t("Отзывы"), () -> reviews(activity, skin, plugin, null));
        page.show();
    }

    /** Plugins whose banner this account may change (its own, or every one for the creator). */
    private static volatile java.util.Set<String> mine = new java.util.HashSet<>();
    private static volatile String bannerFor;

    private static void loadMine(Activity activity) {
        try {
            String uid = Account.activeId();
            if (uid == null || uid.isEmpty()) return;
            JSONObject who = new CloudProfileProvider(activity).identity(uid);
            String raw = Net.postResult(ROOT + "/plugins/banner/mine", new JSONObject().put("uid", uid).put("token", who.optString("token")).toString());
            JSONArray ids = raw == null ? null : new JSONObject(raw).optJSONArray("ids");
            java.util.Set<String> found = new java.util.HashSet<>();
            for (int i = 0; ids != null && i < ids.length(); i++) found.add(ids.optString(i));
            mine = found;
        } catch (Throwable ignored) { }
    }

    /** A published plugin gets a new banner: chosen in the frame, sent, and the store shows it at once. */
    private static void storeBanner(final Store store, final String id, final Uri uri) {
        BannerCrop.show(store.activity, store.skin, uri, encoded -> Net.away("plugin banner", () -> {
            try {
                String uid = Account.activeId();
                JSONObject who = new CloudProfileProvider(store.activity).identity(uid);
                String raw = Net.postLarge(ROOT + "/plugins/banner", new JSONObject().put("uid", uid).put("token", who.optString("token")).put("id", id).put("banner", encoded).toString());
                JSONObject answer = raw == null ? null : new JSONObject(raw);
                if (answer == null || !answer.optBoolean("ok")) throw new IOException(answer == null ? Text.t("Сервер не ответил") : answer.optString("hint", answer.optString("error")));
                store.activity.runOnUiThread(() -> { Screen.say(Text.t("Баннер обновлён")); store.reload.run(); });
            } catch (final Exception error) {
                store.activity.runOnUiThread(() -> Screen.say(Text.t("Не удалось") + ": " + error.getMessage()));
            }
        }));
    }

    /** The owner takes a plugin out of the store; the card folds away. */
    private static void unpublish(final Activity activity, final Skin skin, final JSONObject plugin, final View card) {
        Panel panel = Panel.with(activity, skin, Text.t("Убрать из магазина?"));
        panel.text("«" + plugin.optString("name", plugin.optString("id")) + "» " + Text.t("исчезнет из магазина у всех. У тех, кто его установил, он останется."));
        panel.primary(Text.t("Убрать"), () -> Net.away("plugin unpublish", () -> {
            try {
                ownerCall(activity, new JSONObject().put("action", "remove").put("kind", "package").put("id", plugin.optString("id")));
                activity.runOnUiThread(() -> { fold(card); Screen.say(Text.t("Убран из магазина")); });
            } catch (final Exception error) {
                activity.runOnUiThread(() -> Screen.say(Text.t("Не удалось убрать") + ": " + error.getMessage()));
            }
        }));
        panel.quiet(Text.t("Отмена"), null);
        panel.show();
    }

    /** A card shrinks and fades out of its list. */
    private static void fold(final View card) {
        final int height = card.getHeight();
        if (height <= 0 || !Motion.enabled(card.getContext())) { if (card.getParent() instanceof ViewGroup) ((ViewGroup) card.getParent()).removeView(card); return; }
        final ViewGroup.LayoutParams at = card.getLayoutParams();
        android.animation.ValueAnimator shrink = android.animation.ValueAnimator.ofFloat(1f, 0f);
        shrink.setDuration(Motion.time(340L));
        shrink.setInterpolator(Motion.BOTH);
        shrink.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue(), fade = Math.max(0f, (f - .35f) / .65f);
            card.setAlpha(fade); card.setScaleX(.92f + .08f * fade); card.setScaleY(.92f + .08f * fade);
            at.height = Math.max(1, Math.round(height * Math.min(1f, f * 1.25f)));
            card.setLayoutParams(at);
        });
        shrink.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) { if (card.getParent() instanceof ViewGroup) ((ViewGroup) card.getParent()).removeView(card); }
        });
        shrink.start();
    }

    private static void show(Activity activity, Skin skin, LinearLayout list, TextView status, JSONArray plugins, boolean owner, Runnable reload) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        list.removeAllViews();
        int shown = 0;
        for (int i = 0; plugins != null && i < plugins.length(); i++) {
            JSONObject plugin = plugins.optJSONObject(i);
            if (plugin == null) continue;
            String format = plugin.optString("format");
            if (!"ctt".equals(format) && !"ttplugin".equals(format)) continue;
            String tiktok = plugin.optString("tiktok");
            if (!PluginPackage.forThisTikTok(tiktok)) continue;
            LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            gap.bottomMargin = dp(activity, 10);
            View card = card(activity, skin, plugin, owner);
            list.addView(card, gap);
            shown++;
        }
        if (shown == 0) {
            status.setText(Text.t("Пока нет опубликованных плагинов"));
            list.addView(status);
        }
    }

    /** One plugin: icon, name, version and author, a Verified chip, what it does, and its install button. */
    private static View card(final Activity activity, final Skin skin, final JSONObject plugin, boolean owner) {
        final LinearLayout card = column(activity);
        card.setTag("plugin:" + plugin.optString("id"));
        // a tap on the card opens the plugin's page: banner, description, what it may do, install
        card.setOnClickListener(v -> preview(activity, skin, plugin, (TextView) card.findViewWithTag("install:" + plugin.optString("id"))));
        Motion.touch(card);
        card.setPadding(dp(activity, 14), dp(activity, 14), dp(activity, 14), dp(activity, 12));
        int cardColour = surface(skin, 0.06f);
        card.setBackground(shape(cardColour, dp(activity, 20)));
        card.setClipToOutline(true);
        // the banner reaches past the card's padding to its edges (negative margins): the padding must not cut it,
        // or its corners come out square inside the card; the card's own rounded outline is what trims it
        card.setClipToPadding(false);
        // the plugin's banner is the top of its card: edge to edge, its upper corners the card's, melting into the
        // card under the name
        View banner = bannerView(activity, plugin, cardColour, -dp(activity, 20));
        if (banner != null) {
            LinearLayout.LayoutParams wide = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            wide.setMargins(-dp(activity, 14), -dp(activity, 14), -dp(activity, 14), -dp(activity, 22));
            card.addView(banner, wide);
        }

        LinearLayout top = new LinearLayout(activity);
        top.setGravity(Gravity.CENTER_VERTICAL);
        final ImageView icon = new ImageView(activity);
        icon.setImageDrawable(Symbols.drawable("extension", Accent.colour()));
        int pad = dp(activity, 11);
        icon.setPadding(pad, pad, pad, pad);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        icon.setBackground(shape(Expressive.mix(skin.card, Accent.colour(), 0.22f), dp(activity, 14)));
        icon.setClipToOutline(true);
        top.addView(icon, new LinearLayout.LayoutParams(dp(activity, 46), dp(activity, 46)));
        picture(activity, plugin.optString("icon"), icon);
        LinearLayout words = column(activity);
        words.setPadding(dp(activity, 12), 0, 0, 0);
        TextView name = label(activity, skin, plugin.optString("name", plugin.optString("id")), 16, true, 1f);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        words.addView(name);
        // the people who made it, as nicknames; the one who sent it in counts too
        String authors = plugin.optString("author");
        String sender = plugin.optString("by").replaceFirst("^@", "");
        if (!sender.isEmpty() && !authors.toLowerCase(Locale.ROOT).contains(sender.toLowerCase(Locale.ROOT)))
            authors = authors.isEmpty() ? sender : authors + ", " + sender;
        TextView by = label(activity, skin, PluginList.byline(plugin.optString("version"), authors), 13, true, 1f);
        by.setTextColor(PluginList.bylineColour(skin));
        by.setSingleLine(true);
        by.setEllipsize(TextUtils.TruncateAt.END);
        words.addView(by);
        top.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (plugin.optBoolean("verified")) top.addView(chip(activity, skin, Text.t("Проверен"), true));
        if (owner) {
            ImageView trash = new ImageView(activity);
            int danger = skin.dark() ? 0xFFF2B8B5 : 0xFFB3261E;
            trash.setImageDrawable(Symbols.drawable("delete", danger));
            int tp = dp(activity, 10);
            trash.setPadding(tp, tp, tp, tp);
            trash.setBackground(Expressive.tile(activity, 0, (danger & 0xFFFFFF) | 0x33000000, dp(activity, 22), dp(activity, 22), dp(activity, 12)));
            trash.setContentDescription(Text.t("Убрать из магазина"));
            trash.setTooltipText(Text.t("Убрать из магазина"));
            Motion.touch(trash);
            trash.setOnClickListener(v -> unpublish(activity, skin, plugin, card));
            LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(dp(activity, 46), dp(activity, 46));
            at.leftMargin = dp(activity, 6);
            top.addView(trash, at);
        }
        card.addView(top);
        if (mine.contains(plugin.optString("id"))) {
            TextView change = pill(activity, skin, plugin.optString("banner").isEmpty() ? Text.t("Добавить баннер") : Text.t("Сменить баннер"), false);
            change.setOnClickListener(v -> {
                bannerFor = plugin.optString("id");
                try { activity.startActivityForResult(new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).addCategory(android.content.Intent.CATEGORY_OPENABLE).setType("image/*"), PICK_STORE_BANNER); }
                catch (RuntimeException error) { Screen.say(Text.t("Не удалось открыть файлы")); }
            });
            TextView numbers = pill(activity, skin, Text.t("Статистика"), false);
            numbers.setOnClickListener(v -> stats(activity, skin, plugin));
            LinearLayout mineRow = new LinearLayout(activity);
            mineRow.addView(change);
            LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            gap.leftMargin = dp(activity, 8);
            mineRow.addView(numbers, gap);
            LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            at.topMargin = dp(activity, 10);
            card.addView(mineRow, at);
        } else if (AdminAccess.current()) {
            // the mod's creator sees every plugin's numbers (the server allows it too)
            TextView numbers = pill(activity, skin, Text.t("Статистика"), false);
            numbers.setOnClickListener(v -> stats(activity, skin, plugin));
            LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            at.topMargin = dp(activity, 10);
            card.addView(numbers, at);
        }
        // a short card: what it does in two lines; what it may do and what is new are on its page
        String about = plugin.optString("about");
        if (!about.isEmpty()) {
            TextView text = label(activity, skin, about, 14, false, 0.84f);
            text.setPadding(0, dp(activity, 8), 0, 0);
            text.setMaxLines(2);
            text.setEllipsize(TextUtils.TruncateAt.END);
            card.addView(text);
        }

        final TextView install = pill(activity, skin, Text.t("Установить"), true);
        String installed = installedVersion(plugin.optString("id"));
        if (installed != null) {
            boolean same = installed.equals(plugin.optString("version"));
            install.setText(same ? Text.t("Установлен") : Text.t("Обновить"));
            if (same) { paintPill(activity, skin, install, false, Accent.colour()); install.setEnabled(false); }
        }
        install.setTag("install:" + plugin.optString("id"));
        install.setOnClickListener(v -> install(activity, skin, plugin, install));
        // likes, reviews and the install button share one row
        LinearLayout bottom = new LinearLayout(activity);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.addView(reactions(activity, skin, plugin), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        // how many times it was downloaded, just left of the button (the server counts each archive it hands out)
        if (plugin.has("downloads")) {
            TextView count = new TextView(activity);
            int muted = (skin.text & 0xFFFFFF) | 0xA6000000;
            count.setText(ModStats.number(Math.max(0, plugin.optLong("downloads"))));
            count.setTextColor(muted);
            count.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            android.graphics.drawable.Drawable arrow = Symbols.drawable("download", muted);
            if (arrow != null) arrow.setBounds(0, 0, dp(activity, 16), dp(activity, 16));
            count.setCompoundDrawables(arrow, null, null, null);
            count.setCompoundDrawablePadding(dp(activity, 3));
            count.setGravity(Gravity.CENTER_VERTICAL);
            count.setContentDescription(Text.t("Скачиваний") + ": " + plugin.optLong("downloads"));
            LinearLayout.LayoutParams beside = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            beside.rightMargin = dp(activity, 10);
            bottom.addView(count, beside);
        }
        bottom.addView(install, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams row = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        row.topMargin = dp(activity, 10);
        card.addView(bottom, row);
        return card;
    }

    /** The plugin's own picture from the store (the mod's server only), in place of the generic icon. */
    private static final android.util.LruCache<String, Object> pictures = new android.util.LruCache<String, Object>(MemoryGuard.budget(24 * 1048576)) {
        @Override protected int sizeOf(String key, Object value) { return value instanceof Gif.Frames ? ((Gif.Frames) value).bytes() : ((android.graphics.Bitmap) value).getByteCount(); }
    };

    static void trimMemory() { pictures.evictAll(); }

    private static void put(ImageView into, Object picture) {
        into.setPadding(0, 0, 0, 0);
        if (picture instanceof Gif.Frames) into.setImageDrawable(new Gif.Player((Gif.Frames) picture)); else into.setImageBitmap((android.graphics.Bitmap) picture);
    }

    private static void picture(final Activity activity, String path, final ImageView into) {
        if (path == null || path.isEmpty()) return;
        final String address = path.startsWith("/") ? ROOT + path : path;
        // seen before (the store reopened, the page of a plugin): at once, with no fade
        Object known = pictures.get(address);
        if (known != null) { put(into, known); return; }
        Uri uri = Uri.parse(address);
        if (!"https".equals(uri.getScheme()) || !Server.ours(uri.getHost())) return;
        Net.away("plugin picture", () -> {
            try {
                byte[] bytes = Net.bytes(address);
                if (bytes == null || bytes.length > 1048576) return;
                android.graphics.BitmapFactory.Options bounds = new android.graphics.BitmapFactory.Options();
                bounds.inSampleSize = 1;
                final Gif.Frames frames = Gif.isGif(bytes) ? Gif.decode(bytes, 0, 50, 50, 100) : null;
                final android.graphics.Bitmap bitmap = frames != null ? frames.frames[0] : android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
                if (bitmap == null) return;
                final Object found = frames != null && frames.frames.length > 1 ? frames : bitmap;
                pictures.put(address, found);
                activity.runOnUiThread(() -> {
                    put(into, found);
                    into.setAlpha(0f);
                    into.animate().alpha(1f).setDuration(160).start();
                });
            } catch (Throwable ignored) { }
        });
    }

    private static String installedVersion(String id) {
        for (PluginEngine.Info info : PluginEngine.list()) if (info.id.equals(id)) return info.version;
        return null;
    }

    private static void install(final Activity activity, final Skin skin, final JSONObject plugin, final TextView button) {
        button.setEnabled(false);
        button.setText(Text.t("Установка…"));
        Net.away("plugin download", () -> {
            File temp = null;
            try {
                String address = plugin.getString("url");
                if (address.startsWith("/")) address = ROOT + address;
                Uri uri = Uri.parse(address);
                if (!"https".equals(uri.getScheme()) || !Server.ours(uri.getHost()))
                    throw new IOException(Text.t("Некорректный адрес пакета"));
                byte[] bytes = Net.bytes(address);
                if (bytes == null) throw new IOException(Text.t("Не удалось скачать плагин"));
                StringBuilder digest = new StringBuilder();
                for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes))
                    digest.append(String.format(Locale.ROOT, "%02x", b & 255));
                if (!plugin.getString("sha256").equalsIgnoreCase(digest.toString()))
                    throw new IOException(Text.t("Пакет изменился. Обновите магазин."));
                temp = File.createTempFile("plugin-", ".ctt", activity.getCacheDir());
                try (FileOutputStream out = new FileOutputStream(temp)) { out.write(bytes); }
                final File file = temp;
                PluginEngine.install(Uri.fromFile(file), problem -> activity.runOnUiThread(() -> {
                    file.delete();
                    if (problem == null) {
                        button.setText(Text.t("Установлен"));
                        paintPill(activity, skin, button, false, Accent.colour());
                    } else {
                        button.setEnabled(true);
                        button.setText(Text.t("Повторить"));
                        Screen.say(problem);
                    }
                }));
            } catch (final Exception error) {
                if (temp != null) temp.delete();
                activity.runOnUiThread(() -> {
                    button.setEnabled(true);
                    button.setText(Text.t("Повторить"));
                    Screen.say(error.getMessage());
                });
            }
        });
    }

    // ------------------------------------------------------------------ the editor

    static final int PICK_ICON = 3074, PICK_ZIP = 3075, PICK_BANNER = 3078, PICK_STORE_BANNER = 3079;
    /** The editor on screen, so a picture or a ZIP picked in the file chooser lands in it. */
    private static Editor current;

    static void editor(final Activity activity, final Skin skin) {
        final String key = "draft_" + Account.id();
        JSONObject draft;
        try {
            draft = new JSONObject(prefs(activity).getString(key, "{}"));
            fillDefaults(activity, draft);
        } catch (Exception error) {
            Screen.say(Text.t("Не удалось открыть черновик") + ": " + error.getMessage());
            return;
        }
        new Editor(activity, skin, key, draft).show();
    }

    private static void fillDefaults(Activity activity, JSONObject draft) throws JSONException {
        JSONObject m = draft.optJSONObject("metadata");
        if (m == null) {
            m = new JSONObject().put("id", "ttcuz.my-plugin").put("name", "My plugin").put("version", "1.0.0")
                    .put("author", activity.getSharedPreferences(Ttcuz.PREFS, 0).getString("profile_preview_handle", "ttcuz"))
                    .put("entry", "ttcuz.plugins.MyPlugin").put("api", 4).put("tiktok", PluginPackage.tiktokVersion()).put("permissions", new JSONArray());
            draft.put("metadata", m);
        }
        JSONArray f = draft.optJSONArray("files");
        if (f == null || f.length() == 0) {
            f = new JSONArray().put(new JSONObject().put("path", "java/ttcuz/plugins/MyPlugin.java").put("text",
                    "package ttcuz.plugins;\n\nimport cat.ttcuz.framework.plugin.TtcuzPlugin;\nimport cat.ttcuz.framework.plugin.PluginSettings;\n\n"
                    + "public class MyPlugin extends TtcuzPlugin {\n    @Override public void onSettings(PluginSettings s) {\n"
                    + "        s.action(\"Hello\", () -> ttcuz().toast(\"It works!\"));\n    }\n}\n"));
            draft.put("files", f);
        }
    }

    /** Routes a file the person picked for the editor; true when it was the editor's. */
    static boolean picked(Activity activity, int request, Uri uri) {
        if (request == PICK_PUBLISH) {
            Store store = openStore;
            if (store != null && store.activity == activity && uri != null) publish(store, uri);
            return true;
        }
        if (request == PICK_STORE_BANNER) {
            Store store = openStore;
            if (store != null && store.activity == activity && uri != null && bannerFor != null) storeBanner(store, bannerFor, uri);
            return true;
        }
        if (request != PICK_ICON && request != PICK_ZIP && request != PICK_BANNER) return false;
        final Editor editor = current;
        if (editor == null || editor.activity != activity) return true;
        if (request == PICK_ICON) editor.takePicture(uri); else if (request == PICK_BANNER) editor.takeBanner(uri); else editor.takeZip(uri);
        return true;
    }

    /**
     * The editor sheet. Top: the plugin's picture (tap to change), its name and version. Then two ways in -- a
     * picture, or a whole project as a ZIP -- the manifest fields, the files as chips and the code of the chosen
     * one. At the bottom: build and install here, send for review, export the sources.
     */
    private static final class Editor {
        final Activity activity;
        final Skin skin;
        final String key;
        JSONObject draft;
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable save;
        Panel panel;
        LinearLayout body;
        ImageView picture, bannerPicture;
        View bannerBox;
        TextView pictureNote, note;
        final java.util.Map<String, EditText> inputs = new java.util.HashMap<>();
        LinearLayout chips;
        EditText code;
        /** under the code: each line that uses a permission, and why */
        LinearLayout marks;
        final java.util.Map<String, TextView> rights = new java.util.HashMap<>();
        final Runnable remark = this::remark;
        int selected;
        boolean switching;

        Editor(Activity activity, Skin skin, String key, JSONObject draft) {
            this.activity = activity;
            this.skin = skin;
            this.key = key;
            this.draft = draft;
            this.save = () -> prefs(activity).edit().putString(key, this.draft.toString()).apply();
        }

        JSONObject meta() { return draft.optJSONObject("metadata"); }
        JSONArray files() { return draft.optJSONArray("files"); }
        boolean asks(String permission) {
            JSONArray list = meta().optJSONArray("permissions");
            for (int i = 0; list != null && i < list.length(); i++) if (permission.equals(list.optString(i))) return true;
            return false;
        }

        /** Ticking a permission also moves the plugin to API 2, where the list counts. */
        void setAsks(String permission, boolean on) {
            try {
                JSONArray old = meta().optJSONArray("permissions"), next = new JSONArray();
                for (int i = 0; old != null && i < old.length(); i++) if (!permission.equals(old.optString(i))) next.put(old.optString(i));
                if (on) next.put(permission);
                meta().put("permissions", next);
                // effects and the mod's own screens came with API 3
                int needs = PluginPermissions.WATCH.equals(permission) || PluginPermissions.AUTOMATION.equals(permission) || PluginPermissions.PLAYER.equals(permission) ? 4
                        : PluginPermissions.EFFECTS.equals(permission) || PluginPermissions.MOD.equals(permission) ? 3 : 2;
                if (on && meta().optInt("api", 1) < needs) meta().put("api", needs);
                else if (meta().optInt("api", 1) < 2) meta().put("api", 2);
                changed();
            } catch (JSONException ignored) { }
        }

        java.util.List<String> asked() {
            java.util.List<String> out = new java.util.ArrayList<>();
            JSONArray list = meta().optJSONArray("permissions");
            for (int i = 0; list != null && i < list.length(); i++) out.add(list.optString(i));
            return out;
        }

        void changed() { handler.removeCallbacks(save); handler.postDelayed(save, 500L); }
        void saveNow() { handler.removeCallbacks(save); save.run(); }

        void show() {
            current = this;
            panel = Panel.with(activity, skin, Text.t("Редактор плагинов")).fullScreen();
            panel.subtitle(Text.t("Исходники на Java. Черновик сохраняется сам."));
            body = column(activity);
            body.setLayoutTransition(new android.animation.LayoutTransition());

            // the banner on top, as the store will show it; a tap chooses another
            bannerBox = bannerFrame(activity, skin.page, dp(activity, 22), into -> bannerPicture = into);
            bannerBox.setOnClickListener(v -> pick(PICK_BANNER, "image/*"));
            bannerBox.setContentDescription(Text.t("Баннер"));
            LinearLayout.LayoutParams wide = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            wide.bottomMargin = -dp(activity, 18);
            body.addView(bannerBox, wide);
            showBanner();

            // the picture and the two ways to bring something in
            LinearLayout head = new LinearLayout(activity);
            head.setGravity(Gravity.CENTER_VERTICAL);
            head.setPadding(0, dp(activity, 4), 0, dp(activity, 4));
            picture = new ImageView(activity);
            picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
            picture.setClipToOutline(true);
            picture.setBackground(shape(Expressive.mix(skin.card, Accent.colour(), 0.24f), dp(activity, 20)));
            picture.setContentDescription(Text.t("Картинка плагина"));
            picture.setOnClickListener(v -> pick(PICK_ICON, "image/*"));
            Motion.touch(picture);
            head.addView(picture, new LinearLayout.LayoutParams(dp(activity, 76), dp(activity, 76)));
            LinearLayout side = column(activity);
            side.setPadding(dp(activity, 14), 0, 0, 0);
            LinearLayout buttons = new LinearLayout(activity);
            TextView choose = pill(activity, skin, Text.t("Картинка"), false);
            choose.setOnClickListener(v -> pick(PICK_ICON, "image/*"));
            buttons.addView(choose);
            TextView zip = pill(activity, skin, Text.t("Открыть ZIP"), false);
            zip.setOnClickListener(v -> pick(PICK_ZIP, "application/zip"));
            LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            gap.leftMargin = dp(activity, 8);
            buttons.addView(zip, gap);
            TextView banner = pill(activity, skin, Text.t("Баннер"), false);
            banner.setOnClickListener(v -> pick(PICK_BANNER, "image/*"));
            LinearLayout.LayoutParams gap2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            gap2.leftMargin = dp(activity, 8);
            buttons.addView(banner, gap2);
            HorizontalScrollView strip = new HorizontalScrollView(activity);
            strip.setHorizontalScrollBarEnabled(false);
            strip.addView(buttons);
            side.addView(strip);
            pictureNote = label(activity, skin, "", 12, false, 0.7f);
            pictureNote.setPadding(dp(activity, 4), dp(activity, 6), 0, 0);
            side.addView(pictureNote);
            head.addView(side, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            body.addView(head);
            showPicture(false);

            // the manifest
            body.addView(heading(activity, skin, Text.t("О плагине")));
            String[][] fields = {{"name", Text.t("Название")}, {"id", "ID"}, {"version", Text.t("Версия")},
                    {"author", Text.t("Автор (ники через запятую)")}, {"entry", Text.t("Главный класс")}, {"description", Text.t("Описание")},
                    {"changes", Text.t("Что нового (для обновления в магазине)")}};
            for (final String[] f : fields) {
                final EditText input = field(activity, skin, body, f[1], meta().optString(f[0]), f[0].equals("description") || f[0].equals("changes"));
                inputs.put(f[0], input);
                input.addTextChangedListener(watch(() -> {
                    if (switching) return;
                    try { meta().put(f[0], input.getText().toString()); changed(); } catch (JSONException ignored) { }
                }));
            }

            // what the plugin may do: chosen here, written to manifest.json, shown under the key button
            body.addView(heading(activity, skin, Text.t("Разрешения")));
            TextView rightsNote = label(activity, skin, Text.t("Отметьте всё, чем пользуется плагин. Без нужного разрешения плагин API 2 не запустится."), 12, false, 0.7f);
            rightsNote.setPadding(dp(activity, 4), 0, 0, dp(activity, 6));
            body.addView(rightsNote);
            String[][] groups = {
                    {PluginPermissions.INTERFACE, PluginPermissions.EFFECTS, PluginPermissions.MOD, PluginPermissions.TEXT, PluginPermissions.FEED, PluginPermissions.COLOURS, PluginPermissions.REGION, PluginPermissions.EVENTS},
                    {PluginPermissions.WATCH, PluginPermissions.PLAYER, PluginPermissions.AUTOMATION},
                    {PluginPermissions.NETWORK, PluginPermissions.NOTIFICATIONS, PluginPermissions.CLIPBOARD, PluginPermissions.STORAGE, PluginPermissions.ACCOUNT, PluginPermissions.TIKTOK},
                    {"android.permission.CAMERA", "android.permission.RECORD_AUDIO", "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION",
                            "android.permission.READ_CONTACTS", "android.permission.READ_CALENDAR", "android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO",
                            "android.permission.READ_MEDIA_AUDIO", "android.permission.BLUETOOTH_CONNECT", "android.permission.VIBRATE"}};
            for (String[] group : groups) {
                LinearLayout row = new LinearLayout(activity);
                row.setGravity(Gravity.CENTER_VERTICAL);
                HorizontalScrollView rightsStrip = new HorizontalScrollView(activity);
                rightsStrip.setHorizontalScrollBarEnabled(false);
                rightsStrip.addView(row);
                for (final String permission : group) {
                    final TextView chip = chip(activity, skin, PluginPermissions.title(permission), asks(permission));
                    chip.setOnClickListener(v -> {
                        boolean on = !asks(permission);
                        setAsks(permission, on);
                        markChip(activity, skin, chip, on);
                        later(remark, 0L);
                        chip.setScaleX(.92f); chip.setScaleY(.92f);
                        chip.animate().scaleX(1f).scaleY(1f).setDuration(220).setInterpolator(Motion.OUT).start();
                    });
                    LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    at.rightMargin = dp(activity, 6);
                    row.addView(chip, at);
                    rights.put(permission, chip);
                }
                LinearLayout.LayoutParams rowGap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rowGap.bottomMargin = dp(activity, 6);
                body.addView(rightsStrip, rowGap);
            }

            // the files, as chips, and the code of the selected one
            body.addView(heading(activity, skin, Text.t("Файлы")));
            chips = new LinearLayout(activity);
            chips.setGravity(Gravity.CENTER_VERTICAL);
            HorizontalScrollView fileStrip = new HorizontalScrollView(activity);
            fileStrip.setHorizontalScrollBarEnabled(false);
            fileStrip.addView(chips);
            body.addView(fileStrip);

            code = new EditText(activity);
            code.setTypeface(Typeface.MONOSPACE);
            code.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            code.setTextColor(skin.text);
            code.setGravity(Gravity.TOP | Gravity.START);
            code.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            code.setHorizontallyScrolling(true);
            code.setMinLines(14);
            code.setFilters(new InputFilter[]{new InputFilter.LengthFilter(240000)});
            code.setPadding(dp(activity, 14), dp(activity, 14), dp(activity, 14), dp(activity, 14));
            code.setBackground(shape(skin.dark() ? 0xFF15171B : 0xFFF2F3F5, dp(activity, 16)));
            LinearLayout.LayoutParams codeAt = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            codeAt.topMargin = dp(activity, 10);
            body.addView(code, codeAt);
            code.addTextChangedListener(watch(() -> {
                if (switching) return;
                try { files().getJSONObject(selected).put("text", code.getText().toString()); changed(); }
                catch (JSONException ignored) { }
                later(remark, 350L);
            }));
            marks = column(activity);
            marks.setPadding(0, dp(activity, 6), 0, 0);
            body.addView(marks);
            select(0);

            note = label(activity, skin, PluginForge.needsDownload(activity)
                    ? Text.t("«Собрать и установить» компилирует плагин прямо на телефоне. Первая сборка один раз скачает android.jar с Google (25 МБ).")
                    : Text.t("«Собрать и установить» компилирует плагин прямо на телефоне."), 13, false, 0.7f);
            note.setPadding(dp(activity, 4), dp(activity, 12), dp(activity, 4), 0);
            body.addView(note);
            panel.view(body);

            panel.primaryKeepOpen(Text.t("Собрать и установить"), () -> build(false));
            panel.keepOpen(Text.t("Отправить на проверку"), () -> build(true));
            panel.keepOpen(Text.t("Экспорт исходников ZIP"), () -> {
                try {
                    validate(draft);
                    if (!prefs(activity).edit().putString("export_pending", draft.toString()).commit())
                        throw new IOException(Text.t("Не удалось сохранить проект"));
                    activity.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("application/zip").putExtra(Intent.EXTRA_TITLE, meta().optString("id") + "-source.zip"), EXPORT);
                } catch (Exception error) {
                    panel.busy(false, error.getMessage());
                }
            });
            panel.quiet(Text.CLOSE, this::saveNow);
            panel.show();
            if (panel.dialog() != null) panel.dialog().setOnDismissListener(d -> {
                saveNow();
                if (current == this) current = null;
            });
        }

        void pick(int request, String type) {
            try {
                Intent open = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(type);
                // a plugin's picture may also be a GIF or a video
                if (request == PICK_ICON) open.setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
                activity.startActivityForResult(open, request);
            } catch (RuntimeException error) {
                Screen.say(Text.t("Не удалось открыть файлы"));
            }
        }

        void drawChips() {
            chips.removeAllViews();
            JSONArray files = files();
            for (int i = 0; i < files.length(); i++) {
                final int index = i;
                String path = files.optJSONObject(i).optString("path");
                TextView c = chip(activity, skin, path.substring(path.lastIndexOf('/') + 1), i == selected);
                c.setContentDescription(path);
                c.setOnClickListener(v -> select(index));
                c.setOnLongClickListener(v -> { removeFile(index); return true; });
                LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                at.rightMargin = dp(activity, 8);
                chips.addView(c, at);
            }
            TextView add = chip(activity, skin, "+ " + Text.t("Файл"), false);
            add.setOnClickListener(v -> newFile(activity, skin, files(), () -> { select(files().length() - 1); changed(); }));
            chips.addView(add);
            Fonts.applyTree(chips);
        }

        void select(int index) {
            JSONArray files = files();
            selected = Math.max(0, Math.min(index, files.length() - 1));
            switching = true;
            code.setText(files.optJSONObject(selected).optString("text"));
            switching = false;
            drawChips();
            later(remark, 0L);
        }

        void later(Runnable work, long delay) { handler.removeCallbacks(work); handler.postDelayed(work, delay); }

        /** A tint behind the lines that use a permission: the accent, or red when the permission is not ticked. */
        private final class Lit extends android.text.style.BackgroundColorSpan { Lit(int colour) { super(colour); } }

        /**
         * Marks the open file: each line that uses a permission gets a tint and a row under the code -- line, permission,
         * what in the code needs it, and the author's reason from a "// @uses" comment. A tap ticks a missing permission,
         * then writes the comment above the line for the reason, then jumps to the line.
         */
        void remark() {
            if (code == null || marks == null) return;
            Editable text = code.getText();
            String source = text.toString();
            for (Lit old : text.getSpans(0, text.length(), Lit.class)) text.removeSpan(old);
            java.util.List<PermissionMarks.Mark> found = PermissionMarks.find(source);
            java.util.List<String> asked = asked();
            int accent = Accent.colour(), warn = 0xFFE5484D;
            int[] starts = lineStarts(source);
            marks.removeAllViews();
            if (found.isEmpty()) return;
            TextView title = label(activity, skin, Text.t("Что делает код с разрешениями"), 13, true, 0.85f);
            title.setPadding(dp(activity, 4), dp(activity, 6), 0, dp(activity, 4));
            marks.addView(title);
            int shown = 0;
            for (final PermissionMarks.Mark mark : found) {
                final boolean ok = asked.contains(mark.permission);
                final String why = PermissionMarks.reason(source, mark);
                if (mark.line < starts.length) {
                    int start = starts[mark.line], end = mark.line + 1 < starts.length ? starts[mark.line + 1] - 1 : source.length();
                    if (end > start && text.getSpans(start, end, Lit.class).length == 0)
                        text.setSpan(new Lit(((ok ? accent : warn) & 0x00FFFFFF) | 0x2E000000), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (++shown > 40) continue;
                String line = Text.t("Стр.") + " " + (mark.line + 1) + " · " + PluginPermissions.title(mark.permission) + " · " + mark.what;
                String under = !ok ? Text.t("Разрешение не отмечено — нажмите, чтобы отметить")
                        : why == null ? Text.t("Нажмите, чтобы написать, зачем (// @uses)") : Text.t("Зачем") + ": " + why;
                TextView row = label(activity, skin, line + "\n" + under, 12, false, 1f);
                row.setTextColor(!ok ? warn : skin.text);
                row.setAlpha(ok && why != null ? 0.75f : 1f);
                row.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 8));
                row.setBackground(shape(((ok ? accent : warn) & 0x00FFFFFF) | 0x1A000000, dp(activity, 12)));
                row.setOnClickListener(v -> {
                    if (!asks(mark.permission)) {
                        setAsks(mark.permission, true);
                        TextView chip = rights.get(mark.permission);
                        if (chip != null) markChip(activity, skin, chip, true);
                        remark();
                    } else if (PermissionMarks.reason(code.getText().toString(), mark) == null) {
                        explain(mark);
                    } else {
                        jump(mark.line);
                    }
                });
                Motion.touch(row);
                LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                at.bottomMargin = dp(activity, 6);
                marks.addView(row, at);
            }
            Fonts.applyTree(marks);
        }

        private int[] lineStarts(String source) {
            java.util.List<Integer> starts = new java.util.ArrayList<>();
            starts.add(0);
            for (int i = 0; i < source.length(); i++) if (source.charAt(i) == '\n') starts.add(i + 1);
            int[] out = new int[starts.size()];
            for (int i = 0; i < out.length; i++) out[i] = starts.get(i);
            return out;
        }

        /** Writes "// @uses network: " above the line, at its indent, and puts the cursor after it. */
        void explain(PermissionMarks.Mark mark) {
            String source = code.getText().toString();
            int[] starts = lineStarts(source);
            if (mark.line >= starts.length) return;
            int start = starts[mark.line], indent = start;
            while (indent < source.length() && (source.charAt(indent) == ' ' || source.charAt(indent) == '\t')) indent++;
            String insert = source.substring(start, indent) + PermissionMarks.comment(mark.permission) + "\n";
            code.getText().insert(start, insert);
            code.requestFocus();
            code.setSelection(start + insert.length() - 1);
            showKeyboard();
        }

        void jump(int line) {
            int[] starts = lineStarts(code.getText().toString());
            if (line >= starts.length) return;
            code.requestFocus();
            code.setSelection(starts[line]);
        }

        void showKeyboard() {
            android.view.inputmethod.InputMethodManager keys = (android.view.inputmethod.InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (keys != null) keys.showSoftInput(code, 0);
        }

        /** A long press on a file chip asks to delete that file; the last one stays. */
        void removeFile(final int index) {
            if (files().length() <= 1) { Screen.say(Text.t("Нужен хотя бы один файл")); return; }
            String path = files().optJSONObject(index).optString("path");
            Panel ask = Panel.with(activity, skin, Text.t("Удалить файл?"));
            ask.text(path);
            ask.primary(Text.t("Удалить"), () -> { files().remove(index); select(Math.min(selected, files().length() - 1)); changed(); });
            ask.quiet(Text.t("Отмена"), null);
            ask.show();
        }

        void showPicture(boolean animate) {
            Bitmap bitmap = null;
            String icon = draft.optString("icon");
            if (!icon.isEmpty()) {
                try {
                    byte[] png = android.util.Base64.decode(icon, android.util.Base64.DEFAULT);
                    Gif.Frames frames = Gif.isGif(png) ? Gif.decode(png, 0, 50, 50, 100) : null;
                    if (frames != null && frames.frames.length > 1) {
                        picture.setPadding(0, 0, 0, 0);
                        picture.setImageDrawable(new Gif.Player(frames));
                        pictureNote.setText(Text.t("Картинка добавлена. Долгое нажатие убирает её."));
                        bitmap = frames.frames[0];
                    } else bitmap = android.graphics.BitmapFactory.decodeByteArray(png, 0, png.length);
                } catch (Throwable ignored) { }
            }
            if (bitmap == null) {
                int p = dp(activity, 20);
                picture.setPadding(p, p, p, p);
                picture.setImageDrawable(Symbols.drawable("image", Accent.colour()));
                pictureNote.setText(Text.t("Нажмите, чтобы добавить картинку. Квадратная, будет уменьшена до 128×128."));
            } else if (!(picture.getDrawable() instanceof Gif.Player)) {
                picture.setPadding(0, 0, 0, 0);
                picture.setImageBitmap(bitmap);
                pictureNote.setText(Text.t("Картинка добавлена. Долгое нажатие убирает её."));
            }
            picture.setOnLongClickListener(v -> {
                if (draft.optString("icon").isEmpty()) return false;
                draft.remove("icon");
                changed();
                showPicture(true);
                return true;
            });
            if (animate) {
                picture.setScaleX(0.8f);
                picture.setScaleY(0.8f);
                picture.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(Motion.OUT).start();
            }
        }

        /** The draft's banner over the editor; hidden while there is none. */
        void showBanner() {
            if (bannerBox == null) return;
            String banner = draft.optString("banner");
            Bitmap picture = null;
            if (!banner.isEmpty()) try {
                byte[] bytes = android.util.Base64.decode(banner, android.util.Base64.DEFAULT);
                picture = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            } catch (IllegalArgumentException ignored) { }
            bannerPicture.setImageBitmap(picture);
            bannerBox.setVisibility(picture == null ? View.GONE : View.VISIBLE);
        }

        /** The store card's wide picture: chosen in a 3:1 frame, saved as a JPEG of at most 280 KB. */
        void takeBanner(final Uri uri) {
            BannerCrop.show(activity, skin, uri, encoded -> {
                try { draft.put("banner", encoded); } catch (JSONException ignored) { }
                showBanner();
                changed();
                Screen.say(Text.t("Баннер добавлен"));
            });
        }

        void takePicture(final Uri uri) {
            Net.away("plugin picture", () -> {
                try {
                    final String encoded = iconFrom(activity, uri);
                    activity.runOnUiThread(() -> {
                        try { draft.put("icon", encoded); } catch (JSONException ignored) { }
                        changed();
                        showPicture(true);
                    });
                } catch (final Exception error) {
                    activity.runOnUiThread(() -> Screen.say(Text.t("Не удалось открыть картинку") + ": " + error.getMessage()));
                }
            });
        }

        void takeZip(final Uri uri) {
            Net.away("plugin zip", () -> {
                try {
                    final JSONObject imported = importZip(activity, uri);
                    activity.runOnUiThread(() -> {
                        draft = imported;
                        switching = true;
                        for (java.util.Map.Entry<String, EditText> input : inputs.entrySet())
                            input.getValue().setText(meta().optString(input.getKey()));
                        switching = false;
                        select(0);
                        showPicture(true);
                        showBanner();
                        saveNow();
                        panel.busy(false, Text.t("Проект загружен из ZIP") + ": " + files().length() + " " + Text.t("файлов"));
                    });
                } catch (final Exception error) {
                    activity.runOnUiThread(() -> panel.busy(false, Text.t("ZIP не подошёл") + ": " + error.getMessage()));
                }
            });
        }

        /** Builds on the phone; then either installs the result or sends it, with its sources, for review. */
        void build(final boolean submit) {
            try {
                validate(draft);
            } catch (Exception error) {
                panel.busy(false, error.getMessage());
                return;
            }
            // API 2 holds a plugin to its list: a use the list lacks would fail on the phone, so it is caught here
            if (meta().optInt("api", 1) >= 2) {
                java.util.List<String> unasked = PermissionMarks.unasked(files(), asked());
                if (!unasked.isEmpty()) {
                    StringBuilder text = new StringBuilder(Text.t("Код пользуется разрешениями, которые не отмечены:")).append("\n");
                    for (String permission : unasked) text.append("\n• ").append(PluginPermissions.title(permission)).append(" (").append(permission).append(")");
                    text.append("\n\n").append(Text.t("Отметьте их в «Разрешениях» или нажмите на красную строку под кодом."));
                    panel.busy(false, Text.t("Не хватает разрешений"));
                    report(activity, skin, text.toString());
                    return;
                }
            }
            // the reasons from // @uses comments, for the key button
            try {
                JSONObject uses = PermissionMarks.reasons(files());
                if (uses.length() > 0) meta().put("uses", uses); else meta().remove("uses");
            } catch (JSONException ignored) { }
            saveNow();
            final JSONObject snapshot;
            try { snapshot = new JSONObject(draft.toString()); } catch (JSONException error) { return; }
            if (submit) {
                // obfuscated code, or code that loads other code, is never sent: a reviewer could not judge it
                java.util.List<String> reasons = SourceGuard.unreadable(snapshot.optJSONArray("files"));
                if (!reasons.isEmpty()) {
                    StringBuilder text = new StringBuilder(Text.t("Такой код нельзя отправить на проверку: он запутан или прячет другой код.")).append("\n");
                    for (String reason : reasons) text.append("\n• ").append(reason);
                    panel.busy(false, Text.t("Код не прошёл проверку"));
                    report(activity, skin, text.toString());
                    return;
                }
            }
            panel.busy(true, Text.t("Подготовка инструментов…"));
            Net.away("plugin build", () -> {
                File built = null;
                try {
                    built = PluginForge.build(activity, snapshot, words -> activity.runOnUiThread(() -> panel.busy(true, words)));
                    final File ctt = built;
                    if (submit) {
                        activity.runOnUiThread(() -> panel.busy(true, Text.t("Отправка…")));
                        String said = send(activity, snapshot, ctt);
                        ctt.delete();
                        activity.runOnUiThread(() -> panel.busy(false, said));
                        return;
                    }
                    activity.runOnUiThread(() -> panel.busy(true, Text.t("Установка…")));
                    PluginEngine.install(Uri.fromFile(ctt), problem -> {
                        ctt.delete();
                        if (problem == null) {
                            note.setText(Text.t("«Собрать и установить» компилирует плагин прямо на телефоне."));
                            panel.busy(false, Text.t("Готово: плагин собран и установлен") + " · " + snapshot.optJSONObject("metadata").optString("name"));
                        } else {
                            panel.busy(false, Text.t("Собран, но не установлен") + ": " + problem);
                        }
                    });
                } catch (final Throwable error) {
                    if (built != null) built.delete();
                    Diary.note("plugin build: " + error);
                    final String why = error.getMessage() == null ? error.toString() : error.getMessage();
                    activity.runOnUiThread(() -> {
                        panel.busy(false, Text.t("Сборка не удалась"));
                        report(activity, skin, why);
                    });
                }
            });
        }
    }

    /** Sends the sources, the picture and the package built from them; returns what to tell the person. */
    private static String send(Activity activity, JSONObject draft, File ctt) {
        try {
            if (ctt.length() > 1048576) throw new IOException(Text.t("Пакет больше 1 МБ"));
            byte[] archive = new byte[(int) ctt.length()];
            try (java.io.DataInputStream in = new java.io.DataInputStream(new java.io.FileInputStream(ctt))) { in.readFully(archive); }
            JSONObject body = new JSONObject()
                    .put("metadata", draft.getJSONObject("metadata"))
                    .put("files", draft.getJSONArray("files"))
                    .put("archive", android.util.Base64.encodeToString(archive, android.util.Base64.NO_WRAP))
                    .put("handle", activity.getSharedPreferences(Ttcuz.PREFS, 0).getString("profile_preview_handle", ""))
                    .put("uid", Account.id());
            if (!draft.optString("icon").isEmpty()) body.put("icon", draft.optString("icon"));
            if (!draft.optString("banner").isEmpty()) body.put("banner", draft.optString("banner"));
            String raw = Net.postLarge(ROOT + "/plugins/native/submit", body.toString());
            if (raw == null) throw new IOException(Text.t("Сервер не ответил"));
            JSONObject answer = new JSONObject(raw);
            if (answer.has("error")) throw new IOException(answer.optString("hint", answer.optString("error")));
            // the same ID is already in the store: this went in as its update
            JSONObject update = answer.optJSONObject("update");
            if (update != null)
                return Text.t("Обновление отправлено на проверку") + ": v" + update.optString("from") + " → v" + draft.getJSONObject("metadata").optString("version")
                        + ". " + Text.t("После одобрения у всех появится кнопка «Обновить».");
            return Text.t("Отправлено на проверку. Код проверит Gemini, потом модератор; плагин появится в магазине после одобрения.");
        } catch (Exception error) {
            return Text.t("Не удалось отправить") + ": " + error.getMessage();
        }
    }

    static final int ICON_GIF_MAX = 96 * 1024;

    /**
     * Any picture, cut to a centred square and saved as a PNG small enough for the store (32 KB). A GIF stays animated
     * and a video becomes one (its first three seconds): 128×128, up to 96 KB.
     */
    private static String iconFrom(Context context, Uri uri) throws Exception {
        String type = context.getContentResolver().getType(uri);
        Gif.Frames moving = null;
        if (type != null && type.startsWith("video/")) moving = Gif.fromVideo(context, uri, 128, 50, 50, 100, 3000);
        else if ("image/gif".equals(type)) {
            byte[] raw;
            try (java.io.InputStream in = context.getContentResolver().openInputStream(uri)) { raw = readStream(in, 4 * 1048576); }
            moving = Gif.decode(raw, 128, 50, 50, 100);
        }
        if (moving != null && moving.frames.length > 1) {
            byte[] gif = Gif.encode(moving, ICON_GIF_MAX);
            if (gif == null) throw new IOException(Text.t("Анимация слишком большая, выберите покороче"));
            return android.util.Base64.encodeToString(gif, android.util.Base64.NO_WRAP);
        }
        if (type != null && type.startsWith("video/")) throw new IOException(Text.t("Не удалось прочитать видео"));
        android.graphics.BitmapFactory.Options bounds = new android.graphics.BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (java.io.InputStream in = context.getContentResolver().openInputStream(uri)) {
            android.graphics.BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException(Text.t("Это не картинка"));
        android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
        options.inSampleSize = 1;
        while (Math.min(bounds.outWidth, bounds.outHeight) / (options.inSampleSize * 2) >= 256) options.inSampleSize *= 2;
        Bitmap source;
        try (java.io.InputStream in = context.getContentResolver().openInputStream(uri)) {
            source = android.graphics.BitmapFactory.decodeStream(in, null, options);
        }
        if (source == null) throw new IOException(Text.t("Это не картинка"));
        return iconFrom(source);
    }

    private static String iconFrom(Bitmap source) throws IOException {
        int side = Math.min(source.getWidth(), source.getHeight());
        Bitmap square = Bitmap.createBitmap(source, (source.getWidth() - side) / 2, (source.getHeight() - side) / 2, side, side);
        for (int size : new int[]{128, 96, 72}) {
            Bitmap scaled = Bitmap.createScaledBitmap(square, size, size, true);
            java.io.ByteArrayOutputStream png = new java.io.ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.PNG, 100, png);
            if (png.size() <= 32768) return android.util.Base64.encodeToString(png.toByteArray(), android.util.Base64.NO_WRAP);
        }
        throw new IOException(Text.t("Картинка слишком сложная, выберите попроще"));
    }

    /**
     * A project from a ZIP: manifest.json, the Java under java/ and icon.png, also when all of it sits in one folder
     * (as GitHub's "Download ZIP" does). Java found elsewhere is placed by its package line.
     */
    private static JSONObject importZip(Activity activity, Uri uri) throws Exception {
        java.util.Map<String, byte[]> entries = new java.util.LinkedHashMap<>();
        long total = 0;
        try (java.io.InputStream raw = activity.getContentResolver().openInputStream(uri);
             java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(raw)) {
            if (raw == null) throw new IOException(Text.t("Не удалось открыть файл"));
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                if (entry.isDirectory()) continue;
                String name = entry.getName().replace('\\', '/');
                if (name.contains("../") || name.startsWith("/") || name.startsWith("__MACOSX/")) continue;
                if (!(name.endsWith(".java") || name.endsWith("manifest.json") || name.endsWith("icon.png"))) continue;
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[16384];
                for (int n; (n = zip.read(buffer)) > 0; ) {
                    total += n;
                    if (total > 3000000) throw new IOException(Text.t("ZIP больше 3 МБ"));
                    bytes.write(buffer, 0, n);
                }
                entries.put(name, bytes.toByteArray());
            }
        }
        String manifestPath = null;
        for (String name : entries.keySet())
            if (name.endsWith("manifest.json") && (manifestPath == null || name.length() < manifestPath.length())) manifestPath = name;
        if (manifestPath == null) throw new IOException(Text.t("В ZIP нет manifest.json"));
        String prefix = manifestPath.substring(0, manifestPath.length() - "manifest.json".length());
        JSONObject meta = new JSONObject(new String(entries.get(manifestPath), StandardCharsets.UTF_8));
        JSONArray files = new JSONArray();
        HashSet<String> seen = new HashSet<>();
        for (java.util.Map.Entry<String, byte[]> entry : entries.entrySet()) {
            String name = entry.getKey();
            if (!name.endsWith(".java") || !name.startsWith(prefix)) continue;
            String text = new String(entry.getValue(), StandardCharsets.UTF_8);
            String inside = name.substring(prefix.length());
            String path;
            if (inside.startsWith("java/")) {
                path = inside;
            } else {
                java.util.regex.Matcher pkg = java.util.regex.Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;").matcher(text);
                String file = inside.substring(inside.lastIndexOf('/') + 1);
                path = "java/" + (pkg.find() ? pkg.group(1).replace('.', '/') + "/" : "") + file;
            }
            if (!seen.add(path)) continue;
            files.put(new JSONObject().put("path", path).put("text", text));
        }
        if (files.length() == 0) throw new IOException(Text.t("В ZIP нет Java-файлов"));
        JSONObject draft = new JSONObject().put("metadata", meta).put("files", files);
        byte[] icon = entries.get(prefix + "icon.png");
        if (icon != null) {
            Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(icon, 0, icon.length);
            if (bitmap != null) draft.put("icon", iconFrom(bitmap));
        }
        fillDefaults(activity, draft);
        validate(draft);
        return draft;
    }

    /** A build's errors in a sheet of their own, monospaced and selectable, so a line number can be read and copied. */
    private static void report(Activity activity, Skin skin, String text) {
        Panel panel = Panel.with(activity, skin, Text.t("Ошибки сборки"));
        TextView body = label(activity, skin, text, 12, false, 0.9f);
        body.setTypeface(Typeface.MONOSPACE);
        body.setTextIsSelectable(true);
        body.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
        body.setBackground(shape(skin.dark() ? 0xFF15171B : 0xFFF2F3F5, dp(activity, 16)));
        HorizontalScrollView wide = new HorizontalScrollView(activity);
        wide.addView(body);
        panel.view(wide);
        panel.keepOpen(Text.t("Скопировать"), () -> {
            android.content.ClipboardManager clip = (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clip != null) clip.setPrimaryClip(android.content.ClipData.newPlainText("ttcuz", text));
            Screen.say(Text.t("Скопировано"));
        });
        panel.quiet(Text.CLOSE, null);
        panel.show();
    }

    private static TextView heading(Context c, Skin skin, String words) {
        TextView head = label(c, skin, words, 14, true, 1f);
        head.setTextColor(Accent.colour());
        head.setPadding(dp(c, 4), dp(c, 16), 0, dp(c, 2));
        return head;
    }

    private static void newFile(Activity activity, Skin skin, JSONArray files, Runnable added) {
        Panel panel = Panel.with(activity, skin, Text.t("Новый файл"));
        LinearLayout box = column(activity);
        final EditText path = field(activity, skin, box, Text.t("Путь"), "java/ttcuz/plugins/Helper.java", false);
        panel.view(box);
        panel.primary(Text.t("Добавить"), () -> {
            String wanted = path.getText().toString().trim();
            boolean taken = false;
            for (int i = 0; i < files.length(); i++) if (wanted.equals(files.optJSONObject(i).optString("path"))) taken = true;
            if (!wanted.matches("java/(?:[A-Za-z_$][\\w$]*/)*[A-Za-z_$][\\w$]*\\.java") || taken || files.length() >= 32) {
                Screen.say(Text.t("Нужен уникальный путь java/…/Имя.java, до 32 файлов"));
                return;
            }
            String name = wanted.substring(wanted.lastIndexOf('/') + 1, wanted.length() - 5);
            String pkg = wanted.substring(5, Math.max(5, wanted.lastIndexOf('/'))).replace('/', '.');
            try {
                files.put(new JSONObject().put("path", wanted).put("text",
                        (pkg.isEmpty() ? "" : "package " + pkg + ";\n\n") + "final class " + name + " {\n}\n"));
                added.run();
            } catch (JSONException ignored) { }
        });
        panel.quiet(Text.t("Отмена"), null);
        panel.show();
    }

    private static void validate(JSONObject draft) throws Exception {
        JSONObject meta = draft.getJSONObject("metadata");
        if (!meta.optString("id").matches("[A-Za-z0-9][A-Za-z0-9._-]{0,79}")) throw new IOException(Text.t("Проверьте ID плагина"));
        if (!meta.optString("entry").matches("[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)+")) throw new IOException(Text.t("Проверьте главный класс"));
        for (String field : new String[]{"name", "author", "version"}) {
            String value = meta.optString(field).trim();
            if (value.isEmpty() || value.length() > 80) throw new IOException(Text.t("Заполните название, автора и версию (до 80 символов)"));
        }
        JSONArray files = draft.getJSONArray("files");
        if (files.length() == 0 || files.length() > 32) throw new IOException(Text.t("Нужно от 1 до 32 Java-файлов"));
        HashSet<String> paths = new HashSet<>();
        for (int i = 0; i < files.length(); i++) {
            JSONObject file = files.getJSONObject(i);
            String path = file.getString("path");
            if (!path.matches("java/(?:[A-Za-z_$][\\w$]*/)*[A-Za-z_$][\\w$]*\\.java") || !paths.add(path))
                throw new IOException(Text.t("Некорректный или повторяющийся путь Java-файла"));
            if (file.getString("text").length() > 240000) throw new IOException(Text.t("Java-файл больше 240000 символов"));
        }
        if (draft.toString().getBytes(StandardCharsets.UTF_8).length > 1000000) throw new IOException(Text.t("Проект больше 1 МБ"));
    }

    /** Writes the pending project as a source ZIP to the document the person picked. */
    static void export(final Activity activity, final Uri uri) {
        final String pending = prefs(activity).getString("export_pending", null);
        if (pending == null) { Screen.say(Text.t("Нет проекта для экспорта")); return; }
        Net.away("plugin export", () -> {
            try {
                JSONObject draft = new JSONObject(pending);
                validate(draft);
                try (OutputStream out = activity.getContentResolver().openOutputStream(uri, "wt")) {
                    if (out == null) throw new IOException(Text.t("Не удалось открыть файл"));
                    try (ZipOutputStream zip = new ZipOutputStream(out)) {
                        write(zip, "manifest.json", draft.getJSONObject("metadata").toString(2));
                        if (!draft.optString("icon").isEmpty()) {
                            zip.putNextEntry(new ZipEntry("icon.png"));
                            zip.write(android.util.Base64.decode(draft.optString("icon"), android.util.Base64.DEFAULT));
                            zip.closeEntry();
                        }
                        JSONArray files = draft.getJSONArray("files");
                        for (int i = 0; i < files.length(); i++) {
                            JSONObject file = files.getJSONObject(i);
                            write(zip, file.getString("path"), file.getString("text"));
                        }
                    }
                }
                activity.runOnUiThread(() -> Screen.say(Text.t("Исходники сохранены")));
            } catch (final Exception error) {
                activity.runOnUiThread(() -> Screen.say(Text.t("Ошибка экспорта") + ": " + error.getMessage()));
            }
        });
    }

    private static void write(ZipOutputStream zip, String path, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
