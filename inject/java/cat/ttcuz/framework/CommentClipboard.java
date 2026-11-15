package cat.ttcuz.framework;
/** Native comment-copy formatter: the body without the author's name, unless that is switched off in the settings. */
public final class CommentClipboard {
    private CommentClipboard() {}
    static final String KEY = "comment_copy_without_author";
    private static volatile java.lang.reflect.Method theirs;
    static boolean enabled() {
        android.content.Context context = Ttcuz.context();
        return context == null || context.getSharedPreferences(Ttcuz.PREFS, android.content.Context.MODE_PRIVATE).getBoolean(KEY, true);
    }
    static void set(boolean on) {
        android.content.Context context = Ttcuz.context();
        if (context != null) context.getSharedPreferences(Ttcuz.PREFS, android.content.Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply();
    }
    public static android.content.ClipData LIZ(String author,String body,java.util.List mentions) {
        if (!enabled()) {
            try {
                java.lang.reflect.Method method = theirs;
                if (method == null) theirs = method = Class.forName("X.0sNn").getMethod("LIZ", String.class, String.class, java.util.List.class);
                return (android.content.ClipData) method.invoke(null, author, body, mentions);
            } catch (Throwable error) {
                Diary.note("comment copy: " + error);
            }
        }
        return android.content.ClipData.newPlainText("TikTok comment",body==null ? "" : body);
    }
}
