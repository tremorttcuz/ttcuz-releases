package cat.ttcuz.framework;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Advertising that TikTok itself does not call an ad: casinos, betting, case-opening "drops", dropshipping and the
 * like, said in the video or written under it.
 *
 * Three things, each with its own switch, all driven by one list of words (built in, plus the person's own):
 *  - hide: a video whose description, hashtags or sound name has such a word, or that TikTok marks as a paid
 *    partnership, leaves the feed like a real ad (Feed.withoutAds);
 *  - skip: TikTok's own subtitles of the video (video.captionModel.captionList, WebVTT with a time for each line)
 *    are read once per video; where a line says such a word, that stretch counts as an ad and the player jumps
 *    over it every time the video reaches it (each loop, after seeking back), unless "Вернуть" asked to see it;
 *  - mark: the same stretches are drawn green on the video's seek bar (TikTok's own SeekBar, through its overlay).
 * A video without subtitles has nothing to find inside; the hide works on its text anyway.
 */
final class AdWords {
    private AdWords() {}

    static final String HIDE = "adwords_hide", SKIP = "adwords_skip", MARK = "adwords_mark", LIST = "adwords_list", DEFAULTS = "adwords_defaults", QUIET = "adwords_quiet";
    /**
     * The built-in words, Russian and English, lower case with ё as е. Strong ones name the thing (one is enough); weak ones are speech
     * advertising shares with everything else, and count only in twos (AdText). The person's own words are strong.
     */
    static final String[] STRONG = {
            // Russian: casinos, bookmakers, saying outright that it is an ad (erid: the mark Russian law puts on every ad)
            "казино", "игровые автоматы", "фриспин", "бездеп", "вейджер", "отыгрыш", "первый депозит", "первый деп", "букмекер",
            "ставки на спорт", "рекламная пауза", "рекламная интеграция", "на правах рекламы", "рекламодател", "erid",
            "спонсор этого видео", "спонсор выпуска", "спонсор ролика", "партнер этого видео", "партнер выпуска",
            // the brands as speech recognition writes them in Cyrillic
            "1хбет", "1икс бет", "один икс бет", "ван икс бет", "1вин", "1 вин", "один вин", "ван вин", "мостбет", "мост бет",
            "мелбет", "пин ап", "пинап", "пинко", "вавада", "старда", "джойказино", "покердом", "покер дом", "леон бет", "леонбет",
            "фонбет", "винлайн", "бетбум", "бет бум", "лаки джет", "чикен роуд", "чикен роад", "плинко", "свит бонанза",
            "гейтс оф олимпус", "шугар раш",
            // case sites and drops
            "булдроп", "догдроп", "мусордроп", "гг дроп", "ггдроп", "кей дроп", "кейдроп", "кейс батл", "кейсбатл", "открытие кейсов",
            "открываю кейсы", "дропшиппинг",
            // English and the brands in latin letters
            "casino", "slot machine", "free spins", "no deposit", "first deposit", "wager", "sportsbook", "sports betting", "gambling site",
            "1xbet", "1win", "mostbet", "melbet", "pin-up", "pin up", "pinup", "pinco", "vavada", "starda", "joycasino", "pokerdom", "leonbet",
            "fonbet", "winline", "betboom", "stake.com", "roobet", "rollbit", "gamdom", "duelbits", "bc.game", "bet365", "draftkings",
            "fanduel", "betway", "parimatch", "lucky jet", "chicken road", "plinko", "sweet bonanza", "gates of olympus",
            "ggdrop", "bulldrop", "dogdrop", "musordrop", "epicdrop", "forcedrop", "keydrop", "key-drop", "datdrop", "hellcase",
            "farmskins", "csgoroll", "csgo500", "csgoempire", "skinclub", "hypedrop", "clash.gg", "casebattle", "case battle",
            "ggstandoff", "csfail", "case opening", "dropshipping",
            "sponsored by", "this video is sponsored", "today's sponsor", "thanks to our sponsor", "brought to you by", "paid partnership"};
    static final String[] WEAK = {
            "промокод", "промо код", "депозит", "бонус", "ставк", "слот", "джекпот", "занос", "авиатор", "кейс", "скины", "голда",
            "регистрац", "ссылка в профиле", "ссылка в шапке", "ссылка в описании", "по моей ссылке", "по ссылке", "переходи",
            "телеграм", "тг канал", "тгк", "реферальн", "кэшбэк", "выигрыш", "вывод денег", "заработ", "пассивный доход", "арбитраж",
            "розыгрыш", "спонсор", "партнер", "реклам",
            "promo code", "promocode", "use code", "use my code", "discount code", "my link", "link in bio", "link in my bio", "link below",
            "sign up", "telegram", "referral", "deposit", "bonus", "jackpot", "slots", "aviator", "betting", "withdraw", "cashback",
            "skins", "cases", "giveaway", "sponsor", "partnered with"};
    /** All of them, for the list shown in the settings. */
    static final String[] BUILT_IN = concat(STRONG, WEAK);
    private static String[] concat(String[] a, String[] b) { String[] out = java.util.Arrays.copyOf(a, a.length + b.length); System.arraycopy(b, 0, out, a.length, b.length); return out; }

    private static final Handler main = new Handler(Looper.getMainLooper());
    private static volatile String[] words, weak;
    private static volatile Boolean hideOn, skipOn, markOn;

    private static SharedPreferences prefs() {
        android.content.Context c = Ttcuz.context();
        return c == null ? null : c.getSharedPreferences(Ttcuz.PREFS, 0);
    }
    private static boolean flag(String key, boolean fallback) { SharedPreferences p = prefs(); return p == null ? fallback : p.getBoolean(key, fallback); }
    // always on: these have no switches of their own any more
    static boolean hides() { return true; }
    static boolean skips() { return true; }
    static boolean marks() { return true; }
    /** Skip without saying so: never -- every skip shows its island with "Вернуть" (it had stopped showing at all). */
    static boolean quiet() { return false; }
    private static volatile Boolean quietOn;
    static void set(String key, boolean on) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean(key, on).apply();
        hideOn = skipOn = markOn = quietOn = null;
        if (key.equals(MARK) && !on) unmark();
    }

    /** The person's own words, one per line. */
    static String own() { SharedPreferences p = prefs(); return p == null ? "" : p.getString(LIST, ""); }
    static boolean defaults() { return flag(DEFAULTS, true); }
    static void setWords(String text, boolean builtIn) {
        SharedPreferences p = prefs();
        if (p == null) return;
        p.edit().putString(LIST, text.length() > 8000 ? text.substring(0, 8000) : text).putBoolean(DEFAULTS, builtIn).apply();
        words = null;
        synchronized (found) { found.clear(); settled.clear(); }
        synchronized (verdicts) { verdicts.clear(); }
    }

    /** The strong words: built in, and the person's own. */
    private static String[] words() {
        String[] w = words;
        if (w != null) return w;
        List<String> all = new ArrayList<>();
        if (defaults()) for (String s : STRONG) all.add(s);
        for (String line : own().split("[\\n,;]")) { String s = normal(line).trim(); if (s.length() >= 2 && !all.contains(s)) all.add(s); }
        weak = defaults() ? WEAK : new String[0];
        return words = all.toArray(new String[0]);
    }
    private static String[] weak() { words(); String[] w = weak; return w == null ? new String[0] : w; }

    static String normal(String text) { return AdText.normal(text); }

    /** The first word of the list in a text, or null. */
    static String match(String text) { return AdText.match(text, words()); }

    // ------------------------------------------------------------------ the feed

    /** Whether a post is advertising by its own text, or a paid partnership TikTok marks itself. */
    /** post → its verdict: TikTok reads a feed page many times over, the words are looked for once per post */
    private static final Map<Object, Boolean> verdicts = new java.util.WeakHashMap<>();

    static boolean hidesPost(Object post) {
        if (post == null || !hides()) return false;
        synchronized (verdicts) { Boolean known = verdicts.get(post); if (known != null) return known; }
        boolean verdict = judge(post);
        synchronized (verdicts) { verdicts.put(post, verdict); }
        return verdict;
    }

    private static boolean judge(Object post) {
        try {
            Object commerce = NativeRead.field(post, "mCommerceVideoAuthInfo");
            Object branded = commerce == null ? null : NativeRead.field(commerce, "brandedContentType");
            if (branded instanceof Number && ((Number) branded).longValue() > 0) return true;
            // TikTok's own mark for an ad that is not sold through it
            if (Boolean.TRUE.equals(NativeRead.field(post, "_isSoftAd"))) return true;
            return AdText.advertising(textOf(post), words(), weak());
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Everything a post says in words: the description, the sound, the author's name, the text and link stickers
     * on the video, and the links under it (anchors). Casino promoters keep the description clean and put the name on
     * the picture or behind a link, so all of these are read.
     */
    private static String textOf(Object post) {
        StringBuilder text = new StringBuilder(NativeRead.string(NativeRead.get(post, "getDesc")));
        add(text, NativeRead.field(post, "contentDesc"));
        Object music = NativeRead.get(post, "getMusic");
        if (music != null) add(text, NativeRead.get(music, "getTitle"));
        Object author = NativeRead.get(post, "getAuthor");
        if (author != null) add(text, NativeRead.get(author, "getNickname"));
        Object stickers = NativeRead.field(post, "interactStickerStructs");
        if (stickers instanceof List) for (Object sticker : new ArrayList<>((List<?>) stickers)) {
            add(text, NativeRead.field(sticker, "textStruct"));
            Object link = NativeRead.field(sticker, "urlLinkSticker");
            if (link != null) { add(text, NativeRead.field(link, "displayText")); add(text, NativeRead.field(link, "fullURL")); }
        }
        Object anchors = NativeRead.field(post, "anchors");
        if (anchors instanceof List) for (Object anchor : new ArrayList<>((List<?>) anchors)) {
            add(text, NativeRead.field(anchor, "keyword"));
            add(text, NativeRead.field(anchor, "description"));
            add(text, NativeRead.field(anchor, "caption"));
        }
        Object anchor = NativeRead.field(post, "anchorInfo");
        if (anchor != null) { add(text, NativeRead.field(anchor, "title")); add(text, NativeRead.field(anchor, "webUrl")); }
        return text.length() > 20000 ? text.substring(0, 20000) : text.toString();
    }

    private static void add(StringBuilder text, Object part) {
        if (part instanceof String && !((String) part).isEmpty()) text.append(" \n ").append((String) part);
    }

    /**
     * The creator's own captions put on the video as a sticker (auto_video_caption_info): lines with their times,
     * in milliseconds. Many videos have these and no TikTok subtitles at all.
     */
    private static void stickerCaptions(Object post, List<long[]> times, List<String> texts) {
        Object stickers = NativeRead.field(post, "interactStickerStructs");
        if (!(stickers instanceof List)) return;
        for (Object sticker : new ArrayList<>((List<?>) stickers)) {
            Object caption = NativeRead.field(sticker, "captionStruct");
            Object lines = caption == null ? null : NativeRead.field(caption, "utterances");
            if (!(lines instanceof List)) continue;
            for (Object line : (List<?>) lines) {
                long from = NativeRead.number(NativeRead.field(line, "startTime")), to = NativeRead.number(NativeRead.field(line, "endTime"));
                String said = NativeRead.string(NativeRead.field(line, "text"));
                if (to > from && !said.isEmpty()) { times.add(new long[]{from, to}); texts.add(said); }
            }
        }
    }

    // ------------------------------------------------------------------ inside a video

    /** video id → advertising stretches in milliseconds, [start, end] pairs (empty: none found, or no subtitles) */
    private static final Map<String, long[]> found = new LinkedHashMap<String, long[]>(64, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, long[]> e) { return size() > 80; }
    };
    /**
     * Videos whose verdict is final: their subtitles were read, or they have none after every look. A verdict made
     * without them (no subtitles yet, a failed download, only the creator's stickers) is used while it is the best
     * there is, and the video is read again the next time it plays -- coming back to a video never loses its ad.
     */
    private static final java.util.Set<String> settled = java.util.Collections.newSetFromMap(new LinkedHashMap<String, Boolean>(64, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Boolean> e) { return size() > 80; }
    });
    private static String playing = "", retrying = "";
    private static int retries;
    /** set by a scheduled look again, so it counts as the next try rather than a fresh start */
    private static boolean again;
    private static WeakReference<Object> latest = new WeakReference<>(null);
    private static WeakReference<Object> manager = new WeakReference<>(null);
    /** when the player was last moved past an ad: its new position takes a moment to read back */
    private static long lastSkip;
    /** a stretch the person asked to see ("Вернуть"), or -1 */
    private static int allowed = -1;
    private static final Runnable watch = AdWords::check;

    /** A video became the one playing (PlayerTools.resumed). */
    static void playing(Object controller, Object post, String aid, Object player) {
        if (aid == null || aid.isEmpty()) return;
        if (aid.equals(playing)) {
            // the same video again (opened once more, or back to it): watch it again with its player
            manager = new WeakReference<>(player);
            // TikTok may hand a fresh copy of the post once its details (and subtitles) arrive
            if (post != null) latest = new WeakReference<>(post);
            // the seek bar may be a new one: its marks are drawn again
            markDuration = 0;
            if (current.length > 0) { if (marks()) mark(); main.removeCallbacks(watch); main.postDelayed(watch, 250L); }
            boolean done;
            synchronized (found) { done = settled.contains(aid); }
            if (done) return;
            // its verdict was not final: read it again now that the details may be there
            playing = "";
        }
        playing = aid;
        latest = new WeakReference<>(post);
        allowed = -1;
        manager = new WeakReference<>(player);
        current = new long[0];
        main.removeCallbacks(watch);
        unmark();
        if (!skips() && !marks()) return;
        long[] known;
        boolean done;
        synchronized (found) { known = found.get(aid); done = settled.contains(aid); }
        if (known != null) start(known);
        if (known != null && done) return;
        final String caption = captionUrl(post);
        final List<long[]> times = new ArrayList<>();
        final List<String> texts = new ArrayList<>();
        try { stickerCaptions(post, times, texts); } catch (Throwable ignored) { }
        if (caption == null) {
            // what the creator's stickers say counts now; the subtitles may still come
            if (!times.isEmpty()) { long[] partial = AdText.stretches(sorted(times, texts), texts, words(), weak()); remember(aid, partial, false); start(partial); }
            // TikTok often fills in the subtitles a moment after the video starts: look again while it plays,
            // and only then take "no subtitles" as the answer
            final WeakReference<Object> later = new WeakReference<>(post);
            final Object own = controller, with = player;
            // a look of its own starts over: coming back to the video looks again as many times
            final int tries = aid.equals(retrying) && again ? retries : 0;
            again = false;
            if (tries < 5) {
                retrying = aid; retries = tries + 1;
                main.postDelayed(() -> {
                    Object p = latest.get();
                    if (p == null) p = later.get();
                    if (p == null || !aid.equals(playing)) return;
                    Object now = manager.get();
                    playing = "";
                    again = true;
                    playing(own, p, aid, now != null ? now : with);
                }, 1500L * (tries + 1));
            } else {
                // no subtitles after every look: what the stickers said (or nothing) is the answer
                long[] last;
                synchronized (found) { last = found.get(aid); }
                remember(aid, last == null ? new long[0] : last, true);
            }
            return;
        }
        if (aid.equals(retrying)) retrying = "";
        Net.away("ad subtitles", () -> {
            long[] stretches = new long[0];
            boolean read = false;
            try {
                byte[] bytes = Net.bytes(caption);
                // TikTok's subtitles and the creator's own, read as one: a name either of them caught counts
                if (bytes != null && bytes.length < 600000) { AdText.cues(new String(bytes, "UTF-8"), times, texts); read = true; }
                stretches = AdText.stretches(sorted(times, texts), texts, words(), weak());
            } catch (Throwable error) {
                Diary.note("ad subtitles: " + error);
            }
            final long[] result = stretches;
            // a failed download is not "no ad": the video is read again the next time it plays
            remember(aid, result, read);
            main.post(() -> { if (aid.equals(playing)) start(result); });
        });
    }

    private static void remember(String aid, long[] result, boolean final_) {
        synchronized (found) { found.put(aid, result); if (final_) settled.add(aid); else settled.remove(aid); }
        if (result.length > 0) Diary.note("ad inside " + aid + ": " + java.util.Arrays.toString(result));
    }

    /** Lines from two sources, in time order (texts reordered with them); returns the times. */
    private static List<long[]> sorted(List<long[]> times, List<String> texts) {
        Integer[] order = new Integer[times.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, (x, y) -> Long.compare(times.get(x)[0], times.get(y)[0]));
        List<long[]> t = new ArrayList<>();
        List<String> w = new ArrayList<>();
        for (Integer i : order) { t.add(times.get(i)); w.add(texts.get(i)); }
        texts.clear();
        texts.addAll(w);
        return t;
    }

    /** The subtitles in the video's own language (else the first there is). */
    private static String captionUrl(Object post) {
        try {
            Object video = NativeRead.get(post, "getVideo");
            Object model = video == null ? null : NativeRead.field(video, "captionModel");
            Object list = model == null ? null : NativeRead.field(model, "captionList");
            if (!(list instanceof List) || ((List<?>) list).isEmpty()) return null;
            // The video's own subtitles first: TikTok also sends machine translations, and a translation can put a
            // word in that was never said. In order: marked original; in the language the video is spoken in; not a
            // translation; and only then the first there is (so a video TikTok marks nothing on still gets read).
            Object pick = null;
            Object spoken = NativeRead.field(model, "originalCaptionLanguage");
            String language = spoken == null ? "" : NativeRead.string(NativeRead.field(spoken, "languageCode"));
            for (Object item : (List<?>) list) if (Boolean.TRUE.equals(NativeRead.field(item, "isOriginalCaption"))) { pick = item; break; }
            if (pick == null && !language.isEmpty())
                for (Object item : (List<?>) list) if (language.equalsIgnoreCase(NativeRead.string(NativeRead.field(item, "languageCode")))) { pick = item; break; }
            if (pick == null) for (Object item : (List<?>) list) {
                Object kind = NativeRead.field(item, "translationType");
                boolean translated = (kind instanceof Number && ((Number) kind).longValue() != 0) || Boolean.TRUE.equals(NativeRead.field(item, "isRealtimeTranslated"));
                if (!translated) { pick = item; break; }
            }
            if (pick == null) pick = ((List<?>) list).get(0);
            String url = NativeRead.string(NativeRead.field(pick, "url"));
            if (url.isEmpty()) {
                Object urls = NativeRead.field(pick, "urlList");
                if (urls instanceof List && !((List<?>) urls).isEmpty()) url = String.valueOf(((List<?>) urls).get(0));
            }
            return url.startsWith("https://") ? url : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static long[] stretches(String vtt) { return AdText.stretches(vtt, words(), weak()); }

    private static long[] current = new long[0];

    private static void start(long[] stretches) {
        current = stretches;
        if (stretches.length == 0) return;
        if (marks()) mark();
        main.removeCallbacks(watch);
        main.postDelayed(watch, 250L);
    }

    /** Eight times a second while a video with advertising plays: inside a stretch → jump to its end, once. */
    private static void check() {
        // the player given with this video first; TikTok's last resumed one otherwise
        Object player = manager.get();
        if (player == null) player = PlayerTools.player();
        long[] s = current;
        if (s.length == 0) return;
        if (player == null) { main.postDelayed(watch, 500L); return; }
        long position = NativeRead.number(NativeRead.get(player, "getCurrentPosition")), duration = NativeRead.number(NativeRead.get(player, "getDuration"));
        if (duration > 0 && position >= 0 && skips()) {
            long now = android.os.SystemClock.uptimeMillis();
            // every time the video reaches an ad -- on each loop, after seeking back into it -- it is jumped over
            for (int i = 0; i < s.length / 2 && now - lastSkip > 900; i++) {
                if (s[2 * i + 1] - s[2 * i] > duration * 7 / 10) continue;
                // "Вернуть" lets one stretch play: until the video leaves it, it is not skipped again
                if (i == allowed) { if (position < s[2 * i] - 500 || position >= s[2 * i + 1]) allowed = -1; else continue; }
                if (position >= s[2 * i] && position < s[2 * i + 1] - 300) {
                    lastSkip = now;
                    // a little past the end: the player lands on a key frame, and landing just before the end
                    // brought the video back into the stretch it left
                    long target = Math.min(duration - 200, s[2 * i + 1] + 300);
                    try {
                        NativeRead.floating(player, "seek", PlayerTools.percent(target, duration));
                        if (!quiet()) notice(player, i, s[2 * i], duration);
                    } catch (Throwable error) {
                        Diary.note("ad skip: " + error);
                    }
                }
            }
        }
        if (marks() && duration > 0 && markDuration != duration) {
            // the length is known only once the video plays: the marks are drawn from then on
            markDuration = duration;
            mark();
        }
        main.postDelayed(watch, 120L);
    }

    // ------------------------------------------------------------------ the notice

    private static WeakReference<View> shown = new WeakReference<>(null);
    private static final Runnable hideNotice = () -> {
        View n = shown.get();
        if (n == null) return;
        n.animate().alpha(0f).translationY(n.getHeight() / 2f).setDuration(Motion.time(220L)).setInterpolator(Motion.IN)
                .withEndAction(() -> { if (n.getParent() instanceof ViewGroup) ((ViewGroup) n.getParent()).removeView(n); }).start();
    };

    /**
     * "Реклама пропущена · Вернуть": a small island over the caption, never in the middle and never stopping the
     * video. "Вернуть" plays the stretch from its start, to see what was there; it goes on its own after 3.5 s.
     */
    private static void notice(final Object player, final int index, final long from, final long duration) {
        android.app.Activity activity = Screen.now();
        if (activity == null) return;
        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        float d = activity.getResources().getDisplayMetrics().density;
        main.removeCallbacks(hideNotice);
        View old = shown.get();
        if (old != null && old.getParent() instanceof ViewGroup) ((ViewGroup) old.getParent()).removeView(old);
        android.widget.LinearLayout pill = new android.widget.LinearLayout(activity);
        pill.setGravity(android.view.Gravity.CENTER_VERTICAL);
        pill.setPadding(Math.round(22 * d), Math.round(11 * d), Math.round(22 * d), Math.round(11 * d));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xE6242528);
        bg.setCornerRadius(28 * d);
        pill.setBackground(bg);
        pill.setElevation(6 * d);
        android.widget.TextView said = new android.widget.TextView(activity);
        long skipped = Math.max(1, Math.round((current.length > 2 * index + 1 ? current[2 * index + 1] - current[2 * index] : 0) / 1000f));
        said.setText(Text.t("Реклама пропущена") + " · " + skipped + " " + Text.t("с") + "   ");
        said.setTextColor(0xFFFFFFFF);
        said.setTextSize(16);
        pill.addView(said);
        android.widget.TextView back = new android.widget.TextView(activity);
        back.setText(Text.t("Вернуть"));
        back.setTextColor(0xFFFFFFFF);
        back.setTextSize(16);
        back.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        back.setOnClickListener(v -> {
            allowed = index;
            try { NativeRead.floating(player, "seek", PlayerTools.percent(Math.max(0, from), duration)); } catch (Throwable error) { Diary.note("ad back: " + error); }
            hideNotice.run();
        });
        pill.addView(back);
        Fonts.applyTree(pill);
        android.widget.FrameLayout.LayoutParams at = new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
        // over the author and the caption, above the seek bar and the comment field
        at.bottomMargin = Math.round(activity.getResources().getDisplayMetrics().heightPixels * 0.27f);
        decor.addView(pill, at);
        shown = new WeakReference<>(pill);
        pill.setAlpha(0f);
        pill.setScaleX(.85f); pill.setScaleY(.85f);
        pill.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(Motion.time(320L)).setInterpolator(Motion.OUT).start();
        main.postDelayed(hideNotice, 3500L);
    }

    // ------------------------------------------------------------------ the green marks on the seek bar

    private static long markDuration;
    private static final List<WeakReference<View>> marked = new ArrayList<>();
    private static final int TAG = 0x7e0c0370;

    private static void mark() {
        android.app.Activity activity = Screen.now();
        if (activity == null) return;
        List<View> bars = new ArrayList<>();
        seekBars(activity.getWindow().getDecorView(), bars, 0);
        for (View bar : bars) {
            Object tag = bar.getTag(TAG);
            Green green = tag instanceof Green ? (Green) tag : null;
            if (green == null) { green = new Green(bar); bar.setTag(TAG, green); bar.getOverlay().add(green); marked.add(new WeakReference<>(bar)); }
            green.stretches = current;
            green.invalidateSelf();
            bar.invalidate();
        }
    }

    private static void unmark() {
        for (WeakReference<View> ref : marked) {
            View bar = ref.get();
            Object tag = bar == null ? null : bar.getTag(TAG);
            if (tag instanceof Green) { bar.getOverlay().remove((Green) tag); bar.setTag(TAG, null); }
        }
        marked.clear();
    }

    /** TikTok's video seek bars on screen: SeekBars at least half as wide as the screen. */
    private static void seekBars(View view, List<View> out, int depth) {
        if (view == null || depth > 60 || !view.isShown()) return;
        if (view instanceof android.widget.SeekBar && view.getWidth() > view.getResources().getDisplayMetrics().widthPixels / 2) { out.add(view); return; }
        if (view instanceof ViewGroup) { ViewGroup g = (ViewGroup) view; for (int i = 0; i < g.getChildCount(); i++) seekBars(g.getChildAt(i), out, depth + 1); }
    }

    private static final class Green extends Drawable {
        final View bar;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        long[] stretches = new long[0];
        Green(View bar) { this.bar = bar; paint.setColor(0xE62ECC71); }
        @Override public void draw(Canvas c) {
            long duration = markDuration;
            if (duration <= 0 || stretches.length == 0 || !marks()) return;
            float d = bar.getResources().getDisplayMetrics().density;
            int left = bar.getPaddingLeft(), right = bar.getWidth() - bar.getPaddingRight();
            float top, bottom;
            Drawable track = bar instanceof android.widget.ProgressBar ? ((android.widget.ProgressBar) bar).getProgressDrawable() : null;
            android.graphics.Rect r = track == null ? null : track.getBounds();
            // a thin line on the middle of TikTok's own track, no thicker than the track itself
            float cy = r != null && r.height() > 0 ? bar.getPaddingTop() + r.exactCenterY() : bar.getHeight() / 2f;
            float h = Math.min(1.25f * d, r != null && r.height() > 0 ? r.height() / 2f : 1.25f * d);
            top = cy - h; bottom = cy + h;
            float half = (bottom - top) / 2f;
            for (int i = 0; i < stretches.length / 2; i++) {
                if (stretches[2 * i + 1] - stretches[2 * i] > duration * 7 / 10) continue;
                float a = left + (right - left) * Math.min(1f, stretches[2 * i] / (float) duration);
                float b = left + (right - left) * Math.min(1f, stretches[2 * i + 1] / (float) duration);
                c.drawRoundRect(a, top, Math.max(a + 2 * d, b), bottom, half, half, paint);
            }
        }
        @Override public void setAlpha(int a) { }
        @Override public void setColorFilter(android.graphics.ColorFilter f) { }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }
}
