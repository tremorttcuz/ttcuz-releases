package cat.narezany.margyt;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The badges on your own name, and what you have decided about them.
 *
 * A badge used to be something done to an account rather than something an
 * account had any say in. Now it can be turned off, and several can be put in
 * the order their owner wants, and that choice is what everybody else sees --
 * so it has to live on the server rather than on the phone that made it.
 *
 * On authentication, plainly: there is none, and there cannot be. TikTok will
 * not tell a third party that somebody is who they say they are. So the first
 * phone to claim an account id is given a key for it and keeps it; that is
 * enough to stop a passer-by rearranging somebody else's badges, and not
 * enough to stop somebody determined. It is a picture beside a name.
 *
 * What the server does not trust is anything that decides what is given: which
 * badge the free one is, whether the day for it has passed, and how often
 * anything may be written. None of that is decided here, where it could be
 * edited out.
 */
public final class Mine {

    private Mine() {}

    private static final String KEY_TOKEN = "badge_token";
    private static final String KEY_UID = "badge_uid";

    /** One of the account's badges, as the server has it. */
    public static final class Held {
        public final String id;
        public boolean shown;

        Held(String id, boolean shown) {
            this.id = id;
            this.shown = shown;
        }
    }

    private static volatile List<Held> held = new ArrayList<Held>();
    private static volatile boolean asked;

    /**
     * Set when the server has just answered, and cleared by whoever reads it.
     *
     * The settings screen keeps its own copy of the order while it is being
     * rearranged, and that copy must be thrown away when the server says
     * something new -- otherwise the first, empty answer is what stays on
     * screen even after the real one arrives.
     */
    private static volatile boolean fresh;

    public static boolean tookFresh() {
        boolean was = fresh;
        fresh = false;
        return was;
    }

    public static List<Held> held() {
        return new ArrayList<Held>(held);
    }

    public static boolean anything() {
        return !held.isEmpty();
    }

    // ------------------------------------------------------------- the key

    private static String token() {
        SharedPreferences prefs = prefs();
        if (prefs == null) return "";
        String uid = Account.id();
        // a key belongs to one account; signing in as somebody else drops it
        if (uid != null && !uid.equals(prefs.getString(KEY_UID, ""))) return "";
        return prefs.getString(KEY_TOKEN, "");
    }

    private static void keep(String uid, String token) {
        SharedPreferences prefs = prefs();
        if (prefs != null) {
            prefs.edit().putString(KEY_UID, uid).putString(KEY_TOKEN, token).apply();
        }
    }

    /**
     * Say which account this is and get the key back, then read what it holds.
     *
     * Done once when the settings are opened rather than at start-up: it is
     * the only screen that can do anything with the answer, and an account id
     * is not known until the app has signed in anyway.
     */
    public static void ask(final Runnable then) {
        final String uid = Account.id();
        if (uid == null || uid.length() == 0) return;
        Net.away("badges: mine", new Runnable() {
            @Override
            public void run() {
                try {
                    String said = Net.post(Badges.SERVER + "/claim",
                            new JSONObject().put("uid", uid).toString());
                    if (said == null) return;
                    JSONObject answer = new JSONObject(said);
                    keep(uid, answer.optString("token", ""));
                    read(answer);
                    asked = true;
                    fresh = true;
                    if (then != null) {
                        new android.os.Handler(android.os.Looper.getMainLooper())
                                .post(then);
                    }
                } catch (Throwable error) {
                    Diary.note("badges: mine -- " + error);
                }
            }
        });
    }

    public static boolean everAsked() {
        return asked;
    }

    private static void read(JSONObject answer) {
        JSONArray list = answer.optJSONArray("badges");
        if (list == null) return;
        List<Held> built = new ArrayList<Held>();
        for (int i = 0; i < list.length(); i++) {
            JSONObject one = list.optJSONObject(i);
            if (one == null) continue;
            built.add(new Held(one.optString("id", ""), one.optBoolean("shown", true)));
        }
        held = built;
    }

    // --------------------------------------------------------- what is sent

    /** Save the order and what is shown. Answers on the main thread. */
    public static void save(final List<Held> order, final Said then) {
        final String uid = Account.id();
        final String token = token();
        if (uid == null || token.length() == 0) {
            if (then != null) then.said(false, "");
            return;
        }
        Net.away("badges: save", new Runnable() {
            @Override
            public void run() {
                boolean ok = false;
                String trouble = "";
                try {
                    JSONArray places = new JSONArray();
                    JSONArray hidden = new JSONArray();
                    for (Held one : order) {
                        places.put(one.id);
                        if (!one.shown) hidden.put(one.id);
                    }
                    String said = Net.post(Badges.SERVER + "/profile",
                            new JSONObject().put("uid", uid).put("token", token)
                                    .put("order", places).put("hidden", hidden)
                                    .toString());
                    if (said != null) {
                        read(new JSONObject(said));
                        ok = true;
                    }
                } catch (Throwable error) {
                    trouble = String.valueOf(error);
                    Diary.note("badges: save -- " + error);
                }
                answer(then, ok, trouble);
            }
        });
    }

    /** Take the badge that is free until the day it is not. */
    public static void takeFree(final Said then) {
        final String uid = Account.id();
        final String token = token();
        if (uid == null || token.length() == 0) {
            if (then != null) then.said(false, "");
            return;
        }
        Net.away("badges: free", new Runnable() {
            @Override
            public void run() {
                boolean ok = false;
                try {
                    String said = Net.post(Badges.SERVER + "/old",
                            new JSONObject().put("uid", uid).put("token", token)
                                    .toString());
                    if (said != null) {
                        read(new JSONObject(said));
                        ok = true;
                    }
                } catch (Throwable error) {
                    Diary.note("badges: free -- " + error);
                }
                answer(then, ok, "");
            }
        });
    }

    public interface Said {
        void said(boolean ok, String trouble);
    }

    private static void answer(final Said then, final boolean ok, final String trouble) {
        if (then == null) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                then.said(ok, trouble);
            }
        });
    }

    /** Whether this account already has the free one. */
    public static boolean hasFree() {
        for (Held one : held) {
            if ("old".equals(one.id)) return true;
        }
        return false;
    }

    private static SharedPreferences prefs() {
        Context context = Margy.context();
        if (context == null) return null;
        return context.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE);
    }
}
