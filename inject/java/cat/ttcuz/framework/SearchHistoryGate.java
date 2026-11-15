package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;

import java.lang.reflect.Method;

/**
 * ayumoments: searches are not written to the history on the phone.
 *
 * TikTok 47.2.41 records every search through one method of its history repository (X.0Erp.LIZ(SearchHistory,
 * source)); the build sends those calls here. With the switch on nothing new is recorded; what is already there stays
 * until it is cleared in the search page, as TikTok's own "clear" does it.
 */
public final class SearchHistoryGate {
    private SearchHistoryGate() {}

    static final String KEY = "privacy_no_search_history";
    private static volatile Method record;

    static boolean enabled() {
        Context context = Ttcuz.context();
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY, false);
    }

    static void set(boolean on) {
        Context context = Ttcuz.context();
        if (context != null) context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply();
    }

    /** X.0Erp.LIZ(SearchHistory, String): records one search, unless the switch is on. */
    public static void record(Object repository, Object history, String source) {
        if (PluginEngine.hasTikTokHooks()) PluginKit.event("search:query", "query", NativeRead.field(history, "keyword"), "source", source);
        if (repository == null || enabled()) return;
        try {
            Method method = record;
            if (method == null || !method.getDeclaringClass().isInstance(repository)) {
                method = null;
                for (Class<?> type = repository.getClass(); type != null && method == null; type = type.getSuperclass())
                    for (Method candidate : type.getDeclaredMethods())
                        if (candidate.getName().equals("LIZ") && candidate.getParameterTypes().length == 2
                                && candidate.getParameterTypes()[1] == String.class && history != null
                                && candidate.getParameterTypes()[0].isInstance(history)) { method = candidate; break; }
                if (method == null) return;
                method.setAccessible(true);
                record = method;
            }
            method.invoke(repository, history, source);
        } catch (Throwable error) {
            Diary.note("search history: " + error);
        }
    }
}
