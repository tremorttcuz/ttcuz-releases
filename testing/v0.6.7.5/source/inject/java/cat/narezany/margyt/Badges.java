package cat.narezany.margyt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The badges, and where they come from.
 *
 * They used to be one account and one picture, written into the code, which
 * meant a new badge was a new build of the mod for everybody. Now they are a
 * file in the repository -- `badges.json` -- read when the app starts and
 * again every five minutes. Adding a badge is editing that file.
 *
 * What arrives is kept on disk as well, so the badge is there on the next
 * start before the network has answered, and stays there if GitHub is not
 * reachable at all. Nothing waits on the network: the first draw uses whatever
 * is already known, and a refresh that finds something new simply applies from
 * then on.
 *
 * Pictures named by a badge are fetched the same way and cached beside the
 * file, by a name made from the path, so the same picture is never fetched
 * twice.
 */
public final class Badges {

    private Badges() {}

    /**
     * Where the badges live now.
     *
     * They used to be a file in the repository, which worked and cost nothing
     * and had two limits worth leaving it for: granting one meant a commit,
     * and nobody could decide anything about their own. So there is a small
     * service instead, and it answers with the same shape the file had.
     */
    // Private claim/profile and plugin-store routes are not available yet.
    public static final String SERVER = "";

    private static final String SOURCE =
            "https://ttcuz.daniilsolovatulin.workers.dev/badges";
    private static final String FILES = "https://ttcuz.daniilsolovatulin.workers.dev/";

    private static final long EVERY = 2 * 60 * 1000L;

    /** One badge, as the file describes it. */
    public static final class Badge {
        public final String id;
        public final String image;
        public final int colour;
        public final String title;
        public final String text;
        public final String button;

        Badge(String id, String image, int colour, String title, String text, String button) {
            this.id = id;
            this.image = image;
            this.colour = colour;
            this.title = title;
            this.text = text;
            this.button = button;
        }
    }

public static final String KEY = "badges_on";

    private static volatile Boolean cached;

    public static boolean isEnabled() {
        Boolean known = cached;
        if (known != null) return known;
        try {
            android.content.Context context = Margy.context();
            if (context == null) return true;  // on until there is somewhere to read from
            boolean on = context.getSharedPreferences(Margy.PREFS,
                    android.content.Context.MODE_PRIVATE).getBoolean(KEY, true);
            cached = on;
            return on;
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static void setEnabled(boolean enabled) {
        cached = enabled;
        try {
            android.content.Context context = Margy.context();
            if (context == null) return;
            context.getSharedPreferences(Margy.PREFS, android.content.Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY, enabled).apply();
        } catch (Throwable ignored) {
        }
    }

    /**
     * uid -> every badge that account has, in the order the file lists them.
     * Replaced wholesale on a refresh, never edited in place.
     */
    private static final String CREATOR_UID = "7491898648855512119";
    private static final Badge CREATOR_PLACEHOLDER = new Badge(
            "creator_trem0r1_1", "", 0, "@trem0r1_1",
            "Создатель ttcuz", "Профиль TikTok");
    private static volatile Map<String, Badge[]> known = initialBadges();

    static boolean isCreator(Badge badge) {
        return badge != null && "creator_trem0r1_1".equals(badge.id);
    }

    /** The creator badge is owned by this profile, not also by the remote list. */
    static boolean hasCreatorBadge(String uid) {
        return CREATOR_UID.equals(uid);
    }

    private static Map<String, Badge[]> initialBadges() {
        Map<String, Badge[]> badges = new HashMap<String, Badge[]>();
        badges.put(CREATOR_UID, new Badge[] { CREATOR_PLACEHOLDER });
        return badges;
    }

    /**
     * The same badges by number, because a name has to carry which one it has.
     *
     * The mark left on a name is a single character, and there is nothing else
     * about it that says whose badge it is -- the view that draws it never sees
     * the account. So the character *is* the number: the first badge is
     * U+E000, the second U+E001, and the list here says which is which. They
     * are private-use codepoints, so nothing else can collide with them, and
     * the numbering only has to hold for as long as the app is running.
     */
    private static volatile Badge[] numbered = new Badge[] { CREATOR_PLACEHOLDER };

    /**
     * Where the marks sit in the private-use area -- and it moves every start.
     *
     * A mark used to be U+E000 plus the badge's number, the same on every
     * phone and every run. Which meant anybody could put that character in
     * their own name and wear somebody else's badge: the drawing code sees a
     * character, not an account.
     *
     * Two things fix that, and this is the second one. The first is that a
     * name is stripped of every private-use character before the mod adds its
     * own, so nothing typed into a name survives. This covers everywhere else
     * -- a comment, a bio, anything that is not a name: the run picks a random
     * place in the private-use area, so a character copied out of somebody's
     * screenshot is not a mark on anybody else's phone, and not on the same
     * phone tomorrow.
     */
    private static final char FIRST = pick();

    private static final int MOST = 256;

    private static char pick() {
        // U+E000..U+F8FF is the private-use area; leave room for the whole run
        // E000 is reserved for the user's own ttcuz profile style.
        int room = 0xF8FF - 0xE100 - MOST;
        return (char) (0xE100 + (int) (Math.random() * room));
    }

    /** Anything in the private-use area, ours or not. Names are cleared of it. */
    public static boolean isPrivate(char c) {
        return c >= '\uE000' && c <= '\uF8FF';
    }

    private static volatile boolean started;

    /** The mod's own note, for anything that needs a picture and has none. */
    public static Bitmap note() {
        Bitmap known = ownNote;
        if (known != null) return known;
        try {
            byte[] png = android.util.Base64.decode(Emblem.PNG, android.util.Base64.DEFAULT);
            ownNote = BitmapFactory.decodeByteArray(png, 0, png.length);
        } catch (Throwable error) {
            Diary.note("badge: " + error);
        }
        return ownNote;
    }

    private static volatile Bitmap ownNote;

    /** One badge by the name the server gives it. */
    public static Badge byId(String id) {
        for (Badge badge : numbered) {
            if (badge != null && badge.id.equals(id)) return badge;
        }
        return null;
    }

    public static Badge[] of(String uid) {
        if (uid == null) return null;
        return known.get(uid);
    }

    /**
     * The characters that stand for this account's badges, in order.
     *
     * An account can hold several -- one for supporting the mod, one for
     * drawing an icon it ships with -- and each is its own character, so each
     * becomes its own picture and answers its own tap.
     */
    public static String marksFor(String uid) {
        if (!isEnabled()) return "";
        Badge[] held = of(uid);
        if (held == null || held.length == 0) return "";
        Badge[] list = numbered;
        StringBuilder out = new StringBuilder(held.length);
        for (Badge badge : held) {
            for (int i = 0; i < list.length; i++) {
                if (list[i] == badge) {
                    out.append((char) (FIRST + i));
                    break;
                }
            }
        }
        return out.toString();
    }

    /** Whether a character is one of ours, without looking anything up. */
    public static boolean isMark(char c) {
        return c >= FIRST && c < FIRST + MOST;
    }

    public static Badge byMark(char c) {
        Badge[] list = numbered;
        int at = c - FIRST;
        return at >= 0 && at < list.length ? list[at] : null;
    }

    static String uidForMark(char c) {
        Badge badge = byMark(c);
        if (badge == null) return null;
        for (Map.Entry<String, Badge[]> entry : known.entrySet()) {
            for (Badge held : entry.getValue()) if (held == badge) return entry.getKey();
        }
        return null;
    }

    // ------------------------------------------------------------- keeping up

    /**
     * Read what is on disk, then ask GitHub -- now and every five minutes.
     *
     * Called from the mod's start-up hook, so the first read happens before
     * TikTok has drawn anything.
     */
    public static synchronized void start(Context context) {
        if (started) return;
        started = true;

        byte[] cached = Net.read(file(context));
        if (cached != null) {
            apply(cached);
            prefetch(context);
        }

        if (SOURCE.length() == 0) {
            Diary.note("badges: offline cache only; feed not configured");
            return;
        }

        final Handler handler = new Handler(Looper.getMainLooper());
        final Context application = context.getApplicationContext();
        handler.post(new Runnable() {
            @Override
            public void run() {
                refresh(application);
                handler.postDelayed(this, EVERY);
            }
        });
    }

    /** What the server said last time, so it need not say it again. */
    private static volatile String tag;

    private static void refresh(final Context context) {
        Net.away("badges", new Runnable() {
            @Override
            public void run() {
                // The server answers 304 when nothing has changed, which is
                // most of the time -- so asking every two minutes costs a few
                // hundred bytes rather than the whole list.
                Net.Answer said = Net.fetch(SOURCE, tag);
                if (said == null || said.unchanged) return;
                tag = said.tag;
                if (said.body == null) return;
                if (apply(said.body)) {
                    Net.save(file(context), said.body);
                    prefetch(context);
                    Diary.note("badges: " + known.size() + " accounts, "
                            + numbered.length + " badges");
                    told();
                }
            }
        });
    }

    public static void refreshNow(Context context) {
        if (context == null) return;
        tag = null;
        refresh(context.getApplicationContext());
    }

    /**
     * Somebody waiting to hear that the list has arrived.
     *
     * The settings screen, and nothing else: it is built from what is known at
     * the moment it opens, and on the first run after an update what is known
     * is a file cached before any of this existed. Without this the free badge
     * would be offered a minute after the screen it is offered on was drawn.
     */
    private static volatile Runnable waiting;

    public static void tell(Runnable then) {
        waiting = then;
    }

    public static void stopTelling(Runnable then) {
        if (waiting == then) waiting = null;
    }

    private static void told() {
        final Runnable then = waiting;
        if (then == null) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).post(then);
    }

    /** When the free badge stops being given out, as the server reckons it. */
    private static volatile long freeUntil;
    private static volatile long serverNow;
    private static volatile long readAt;

    /** Whether the offer is still open, by the server's clock and not ours. */
    public static boolean freeStillOpen() {
        if (serverNow <= 0) return false;
        long since = (android.os.SystemClock.elapsedRealtime() - readAt) / 1000;
        return serverNow + since < freeUntil;
    }

    /**
     * Fetch every badge's picture now rather than when a name needs it.
     *
     * A picture asked for while a name is being drawn cannot be waited on --
     * the badge falls back to the mod's own note and only becomes itself the
     * next time that view is drawn, which is why badges used to appear a beat
     * late or not at all on a profile. There are a handful of pictures and
     * they are cached on disk, so fetching them all at the start costs one
     * round trip on the first run and nothing afterwards.
     */
    private static void prefetch(Context context) {
        try {
            for (Badge badge : numbered) {
                if (badge != null && badge.image.length() > 0) {
                    picture(context, badge.image);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean apply(byte[] json) {
        try {
            JSONObject root = new JSONObject(new String(json, "UTF-8"));
            JSONArray list = root.optJSONArray("badges");
            if (list == null) return false;
            long until = root.optLong("free_until", 0);
            long now = root.optLong("now", 0);
            if (until > 0 && now > 0) {
                freeUntil = until;
                serverNow = now;
                readAt = android.os.SystemClock.elapsedRealtime();
            }

            Map<String, java.util.List<Badge>> built =
                    new HashMap<String, java.util.List<Badge>>();
            Map<String, java.util.Set<String>> heldIds =
                    new HashMap<String, java.util.Set<String>>();
            java.util.LinkedHashMap<String, Badge> distinct =
                    new java.util.LinkedHashMap<String, Badge>();
            for (int i = 0; i < list.length(); i++) {
                JSONObject one = list.optJSONObject(i);
                if (one == null) continue;
                Badge badge = new Badge(
                        one.optString("id", "badge" + i),
                        one.optString("image", ""),
                        colour(one.optString("colour", "")),
                        localised(one, "title"),
                        localised(one, "text"),
                        localised(one, "button"));
                // The creator entry is shipped locally so it works before the
                // first network response. The server also lists it for public
                // profiles; keeping both made the same account show two crowns.
                if (isCreator(badge)) continue;
                if (!badge.id.matches("[A-Za-z0-9_.-]{1,64}")) continue;
                JSONArray users = one.optJSONArray("users");
                if (users == null) continue;
                Badge canonical = distinct.get(badge.id);
                if (canonical == null) {
                    canonical = badge;
                    distinct.put(badge.id, badge);
                }
                for (int u = 0; u < users.length(); u++) {
                    String uid = users.optString(u, "");
                    if (!uid.matches("[0-9]{1,24}")) continue;
                    java.util.List<Badge> theirs = built.get(uid);
                    if (theirs == null) {
                        theirs = new java.util.ArrayList<Badge>(2);
                        built.put(uid, theirs);
                    }
                    java.util.Set<String> ids = heldIds.get(uid);
                    if (ids == null) {
                        ids = new java.util.HashSet<String>();
                        heldIds.put(uid, ids);
                    }
                    if (ids.add(canonical.id)) theirs.add(canonical);
                }
            }
            Badge[] order = new Badge[Math.min(distinct.size() + 1, MOST)];
            order[0] = CREATOR_PLACEHOLDER;
            int at = 1;
            for (Badge badge : distinct.values()) {
                if (at == order.length) break;
                order[at++] = badge;
            }

            Map<String, Badge[]> settled = new HashMap<String, Badge[]>(built.size());
            for (Map.Entry<String, java.util.List<Badge>> entry : built.entrySet()) {
                settled.put(entry.getKey(), entry.getValue().toArray(new Badge[0]));
            }

            Badge[] remote = settled.get(CREATOR_UID);
            Badge[] creator = new Badge[(remote == null ? 0 : remote.length) + 1];
            creator[0] = CREATOR_PLACEHOLDER;
            if (remote != null) System.arraycopy(remote, 0, creator, 1, remote.length);
            settled.put(CREATOR_UID, creator);

            numbered = order;
            known = settled;
            cat.narezany.margyt.Badge.refreshProfiles();
            return true;
        } catch (Throwable error) {
            Diary.note("badges: unreadable, keeping the last ones -- " + error);
            return false;
        }
    }

    /** `text_ru` before `text`, so a badge can speak the phone's language. */
    private static String localised(JSONObject one, String field) {
        String language = Locale.getDefault().getLanguage();
        String translated = one.optString(field + "_" + language, "");
        if (translated.length() > 0) return translated;
        return one.optString(field, "");
    }

    private static int colour(String hex) {
        try {
            if (hex.length() == 0) return 0;
            return 0xFF000000 | Integer.parseInt(hex.replace("#", ""), 16);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static File file(Context context) {
        return new File(context.getFilesDir(), "margyt/badges.json");
    }

    // -------------------------------------------------------- their pictures

    private static final Map<String, Bitmap> pictures = new HashMap<String, Bitmap>();
    private static final Map<String, Long> pictureAsked = new HashMap<String, Long>();

    /**
     * The picture a badge names, or null for the mod's own note.
     *
     * Answers from memory or from the cache on disk, and never waits: a
     * picture that is not here yet is fetched on a thread, and the badge draws
     * the note until the next time it is drawn.
     */
    public static Bitmap picture(final Context context, final String path) {
        if (path == null || path.length() == 0) return null;
        // Only the server's own badge pictures: a flat folder of PNGs, nothing else.
        if (!path.matches("badges/[a-z0-9_-]{1,32}\\.png")) return null;
        synchronized (pictures) {
            if (pictures.containsKey(path)) {
                Bitmap known = pictures.get(path);
                Long asked = pictureAsked.get(path);
                if (known != null || (asked != null && android.os.SystemClock.uptimeMillis() - asked < 30000L))
                    return known;
            }
            pictures.put(path, null);  // asked for; do not ask again
            pictureAsked.put(path, android.os.SystemClock.uptimeMillis());
        }

        final File cache = new File(context.getFilesDir(), "margyt/badges/" + name(path));
        byte[] have = Net.read(cache);
        if (have != null) {
            Bitmap cached = remember(path, have);
            if (cached != null) return cached;
            cache.delete();
        }

        Net.away("badge picture", new Runnable() {
            @Override
            public void run() {
                byte[] raw = Net.bytes(FILES + path);
                if (raw == null) return;
                if (remember(path, raw) != null) {
                    Net.save(cache, raw);
                    cat.narezany.margyt.Badge.refreshProfiles();
                    told();
                }
            }
        });
        return null;
    }

    private static Bitmap remember(String path, byte[] raw) {
        try {
            if (raw.length > 65536) return null;
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(raw,0,raw.length,bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || bounds.outWidth > 1024 || bounds.outHeight > 1024)
                return null;
            Bitmap bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length);
            if (bitmap == null) return null;
            // trimmed the way the mod's own note is trimmed. A badge is drawn
            // into a box the height of the line it sits in, so a picture with
            // empty margin around it comes out smaller than one without --
            // which is why the server's badges looked shrunken beside it.
            // spelled out, because `Badge` inside this class means the one
            // nested in it rather than the class that draws them
            bitmap = cat.narezany.margyt.Badge.crop(bitmap);
            synchronized (pictures) {
                pictures.put(path, bitmap);
            }
            return bitmap;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** A file name that is a path's, without being a path. */
    private static String name(String path) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            out.append(Character.isLetterOrDigit(c) || c == '.' ? c : '_');
        }
        return out.toString();
    }
}
