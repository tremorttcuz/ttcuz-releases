package cat.ttcuz.framework;

import android.content.Context;

/**
 * The results of a poll in the comments without voting first. The poll cell reads whether the viewer has voted
 * (PollInfo.userPollOption) to choose between the choices and the results; the build sends those reads in the
 * poll's own views here, and an unvoted poll answers an option that is not there: "voted, for none of them". Its percentages show, nothing is
 * marked as chosen, and nothing is sent anywhere. While this is on a vote can not be cast from the comments.
 */
public final class PollPeek {
    private PollPeek() {}

    static final String KEY = "poll_results_without_vote";
    private static final Integer NONE = 1_000_000;

    static boolean enabled() {
        Context context = Ttcuz.context();
        return context != null && context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false);
    }

    static void set(boolean on) {
        Context context = Ttcuz.context();
        if (context != null) context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply();
    }

    private static volatile java.lang.reflect.Field field;

    /** PollInfo.userPollOption, read in a poll view. */
    public static Integer option(Object poll) {
        Integer theirs = null;
        try {
            java.lang.reflect.Field f = field;
            if (f == null) { f = poll.getClass().getDeclaredField("userPollOption"); f.setAccessible(true); field = f; }
            theirs = (Integer) f.get(poll);
        } catch (Throwable error) {
            Diary.note("poll: " + error);
        }
        // not voted is null or below zero; results show for any option >= 0, so "voted" for one that is not there:
        // the percentages show and no choice is marked
        if ((theirs == null || theirs < 0) && enabled()) return NONE;
        return theirs;
    }
}
