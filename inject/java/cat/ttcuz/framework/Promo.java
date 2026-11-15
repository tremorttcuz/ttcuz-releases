package cat.ttcuz.framework;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Videos the creator promotes, shown now and then in the feed of everybody with the mod.
 *
 * Quiet on purpose: one promoted video at most every few minutes (6, 12 or 25 by how often the creator asked for
 * it), the same one at most three times a day for one person, never twice in one page, and never at the top. The list
 * is read once an hour (the server keeps it cached at its edge); the video itself is fetched ahead, by TikTok's own
 * "video by id" request (DetailApi), so inserting it costs nothing when a page arrives. A fetched video is kept for
 * twenty minutes at most: its playing addresses expire.
 */
final class Promo {
    private Promo() {}

    static final String KEY = "promos_shown_on";
    private static final String LIST = "promos_list", LIST_AT = "promos_list_at", SEEN = "promos_seen";
    private static final long LIST_EVERY = 60 * 60 * 1000L, FRESH = 20 * 60 * 1000L;
    private static final Map<Object, Boolean> pages = new WeakHashMap<>();
    private static volatile Object ready;
    private static volatile String readyAid;
    private static volatile long readyAt, lastShown;
    private static volatile boolean fetching;

    private static SharedPreferences prefs() {
        android.content.Context c = Ttcuz.context();
        return c == null ? null : c.getSharedPreferences(Ttcuz.PREFS, 0);
    }

    static boolean enabled() { return true; } // always on: no switch any more
    static void setEnabled(boolean on) { SharedPreferences p = prefs(); if (p != null) p.edit().putBoolean(KEY, on).apply(); }

    /** A feed page passed through Feed.getItems: perhaps one promoted video goes into it. Never throws. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void into(Object page, List items) {
        try {
            if (page == null || items == null || items.size() < 3 || !enabled()) return;
            synchronized (pages) { if (pages.put(page, Boolean.TRUE) != null) return; }
            refresh();
            Object post = ready;
            String aid = readyAid;
            long now = System.currentTimeMillis();
            if (post == null || aid == null || now - readyAt > FRESH) { prefetch(); return; }
            JSONObject promo = find(aid);
            if (promo == null) { ready = null; prefetch(); return; }
            int weight = promo.optInt("weight", 2);
            long gap = weight >= 3 ? 6 * 60000L : weight == 2 ? 12 * 60000L : 25 * 60000L;
            if (now - lastShown < gap) return;
            for (Object item : items) if (aid.equals(NativeRead.aid(item instanceof com.ss.android.ugc.aweme.feed.model.Aweme ? item : NativeRead.get(item, "getAweme")))) return;
            // a page's items are of one kind: a bare video only where the page holds bare videos
            if (!(items.get(0) instanceof com.ss.android.ugc.aweme.feed.model.Aweme)) return;
            items.add(Math.min(items.size(), 2 + (int) (now % 3)), post);
            lastShown = now;
            seen(aid);
            ready = null;
            Diary.note("promo shown " + aid);
            prefetch();
        } catch (Throwable error) {
            Diary.note("promo: " + error);
        }
    }

    // ------------------------------------------------------------------ the list

    private static JSONArray list() {
        SharedPreferences p = prefs();
        try { return new JSONArray(p == null ? "[]" : p.getString(LIST, "[]")); } catch (Throwable e) { return new JSONArray(); }
    }

    private static JSONObject find(String aid) {
        JSONArray all = list();
        long now = System.currentTimeMillis();
        for (int i = 0; i < all.length(); i++) {
            JSONObject one = all.optJSONObject(i);
            if (one != null && aid.equals(one.optString("aid")) && one.optLong("until") > now) return one;
        }
        return null;
    }

    private static volatile boolean loading;

    /** Reads the list again when it is an hour old. */
    static void refresh() {
        SharedPreferences p = prefs();
        if (p == null || loading || System.currentTimeMillis() - p.getLong(LIST_AT, 0) < LIST_EVERY) return;
        loading = true;
        Net.away("promos", () -> {
            try {
                byte[] body = Net.bytes(CloudProfileProvider.ROOT + "/v1/promos");
                if (body != null && body.length < 16384) {
                    JSONArray promos = new JSONObject(new String(body, "UTF-8")).optJSONArray("promos");
                    p.edit().putString(LIST, promos == null ? "[]" : promos.toString()).putLong(LIST_AT, System.currentTimeMillis()).apply();
                    prefetch();
                }
            } catch (Throwable error) {
                Diary.note("promos list: " + error);
            } finally {
                loading = false;
            }
        });
    }

    /** Forgets the list, so the next page reads it again (after the creator changed it). */
    static void reload() { SharedPreferences p = prefs(); if (p != null) p.edit().putLong(LIST_AT, 0).apply(); ready = null; refresh(); }

    // ------------------------------------------------------------------ the next video

    /** Picks the next promoted video (more often the ones asked for more often) and fetches it from TikTok. */
    private static void prefetch() {
        if (fetching) return;
        JSONArray all = list();
        long now = System.currentTimeMillis();
        List<String> pool = new ArrayList<>();
        for (int i = 0; i < all.length(); i++) {
            JSONObject one = all.optJSONObject(i);
            if (one == null || one.optLong("until") <= now) continue;
            String aid = one.optString("aid");
            if (!aid.matches("\\d{6,24}") || shownToday(aid) >= 3) continue;
            for (int w = Math.max(1, Math.min(3, one.optInt("weight", 2))); w > 0; w--) pool.add(aid);
        }
        if (pool.isEmpty()) return;
        final String aid = pool.get((int) (now % pool.size()));
        fetching = true;
        Net.away("promo video", () -> {
            try {
                // TikTok's own request for one video by its id: DetailApi.LIZ(preload, topview, aweme_id, enter_from)
                Object post = Class.forName("com.ss.android.ugc.aweme.detail.api.DetailApi")
                        .getMethod("LIZ", int.class, int.class, String.class, String.class).invoke(null, 0, 0, aid, "homepage_hot");
                if (post != null && aid.equals(NativeRead.aid(post))) { ready = post; readyAid = aid; readyAt = System.currentTimeMillis(); }
            } catch (Throwable error) {
                Diary.note("promo video " + aid + ": " + (error.getCause() != null ? error.getCause() : error));
            } finally {
                fetching = false;
            }
        });
    }

    // ------------------------------------------------------------------ how often each was shown

    private static int shownToday(String aid) {
        SharedPreferences p = prefs();
        if (p == null) return 0;
        try {
            JSONObject seen = new JSONObject(p.getString(SEEN, "{}"));
            JSONObject one = seen.optJSONObject(aid);
            return one == null || one.optLong("day") != day() ? 0 : one.optInt("n");
        } catch (Throwable e) { return 0; }
    }

    private static void seen(String aid) {
        SharedPreferences p = prefs();
        if (p == null) return;
        try {
            JSONObject seen = new JSONObject(p.getString(SEEN, "{}"));
            JSONObject one = seen.optJSONObject(aid);
            int n = one == null || one.optLong("day") != day() ? 0 : one.optInt("n");
            seen.put(aid, new JSONObject().put("day", day()).put("n", n + 1));
            if (seen.length() > 40) seen = new JSONObject().put(aid, seen.get(aid));
            p.edit().putString(SEEN, seen.toString()).apply();
        } catch (Throwable ignored) { }
    }

    private static long day() { return System.currentTimeMillis() / 86400000L; }

    // ------------------------------------------------------------------ the creator's side

    /**
     * The creator holds a video on any profile: how long to promote it, and how often to show it. Only the creator's
     * proven account can do this; the server refuses everyone else.
     */
    static void ask(final android.app.Activity activity, final String aid) {
        if (activity == null || aid == null) return;
        Skin skin = Skin.tonal(activity, Accent.colour());
        final int[] days = {7}, weight = {2};
        final String[] dayNames = {Text.t("1 день"), Text.t("3 дня"), Text.t("Неделя"), Text.t("Месяц")};
        final int[] dayValues = {1, 3, 7, 30};
        final String[] weightNames = {Text.t("Редко"), Text.t("Иногда"), Text.t("Часто")};
        android.widget.LinearLayout box = new android.widget.LinearLayout(activity);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.addView(chips(activity, skin, Text.t("Сколько"), dayNames, 2, i -> days[0] = dayValues[i]));
        box.addView(chips(activity, skin, Text.t("Как часто"), weightNames, 1, i -> weight[0] = i + 1));
        Panel panel = Panel.with(activity, skin, Text.t("Продвинуть видео"));
        panel.text(Text.t("Видео будет тихо появляться в ленте у всех с модом: не чаще раза в несколько минут и не больше трёх раз в день одному человеку."));
        panel.view(box);
        panel.primary(Text.t("Продвинуть"), () -> send(aid, days[0], weight[0]));
        panel.quiet(Text.t("Убрать из продвижения"), () -> send(aid, 0, 2));
        panel.show();
    }

    private interface Pick { void on(int index); }

    private static android.view.View chips(android.app.Activity activity, Skin skin, String title, String[] names, int chosen, Pick pick) {
        float d = activity.getResources().getDisplayMetrics().density;
        android.widget.LinearLayout column = new android.widget.LinearLayout(activity);
        column.setOrientation(android.widget.LinearLayout.VERTICAL);
        column.setPadding(0, Math.round(10 * d), 0, 0);
        android.widget.TextView label = new android.widget.TextView(activity);
        label.setText(title);
        label.setTextColor(skin.muted());
        label.setTextSize(13);
        column.addView(label);
        android.widget.LinearLayout row = new android.widget.LinearLayout(activity);
        row.setPadding(0, Math.round(6 * d), 0, 0);
        final android.widget.TextView[] all = new android.widget.TextView[names.length];
        final int[] current = {chosen};
        Runnable paint = () -> {
            for (int i = 0; i < all.length; i++) {
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                bg.setCornerRadius(18 * d);
                bg.setColor(i == current[0] ? Accent.colour() : (skin.text & 0xFFFFFF) | 0x14000000);
                all[i].setBackground(bg);
                all[i].setTextColor(i == current[0] ? 0xFFFFFFFF : skin.text);
            }
        };
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            android.widget.TextView chip = new android.widget.TextView(activity);
            chip.setText(names[i]);
            chip.setTextSize(14);
            chip.setPadding(Math.round(14 * d), Math.round(8 * d), Math.round(14 * d), Math.round(8 * d));
            chip.setOnClickListener(v -> { current[0] = index; pick.on(index); paint.run(); });
            Motion.touch(chip);
            android.widget.LinearLayout.LayoutParams at = new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            at.rightMargin = Math.round(6 * d);
            row.addView(chip, at);
            all[i] = chip;
        }
        paint.run();
        android.widget.HorizontalScrollView strip = new android.widget.HorizontalScrollView(activity);
        strip.setHorizontalScrollBarEnabled(false);
        strip.addView(row);
        column.addView(strip);
        return column;
    }

    private static void send(final String aid, final int days, final int weight) {
        Net.away("promote", () -> {
            try {
                String uid = Account.id();
                android.content.Context c = Ttcuz.context();
                if (uid == null || c == null) throw new Exception(Text.t("Войдите в аккаунт TikTok"));
                String answer = new CloudProfileProvider(c).promote(uid, aid, days, weight);
                Screen.say(days == 0 ? Text.t("Убрано из продвижения") : Text.t("Видео продвигается") + " · " + answer);
                reload();
            } catch (Throwable error) {
                Screen.say(error.getMessage() == null ? Text.t("Не удалось") : error.getMessage());
            }
        });
    }
}
