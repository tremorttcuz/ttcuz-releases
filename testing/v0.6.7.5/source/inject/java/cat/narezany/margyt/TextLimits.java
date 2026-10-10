package cat.narezany.margyt;

/** Local input limits only; native publication and server responses stay intact. */
public final class TextLimits {
    public static final String COMMENTS = "unlimited_comments";
    public static final String REPOSTS = "unlimited_reposts";
    public static final String HASHTAGS = "unlimited_hashtags";
    private TextLimits() {}
    public static int comment(int original) { return Flags.isOn(COMMENTS) ? Integer.MAX_VALUE : original; }
    // The native keyboard casts this long to int. Long.MAX_VALUE would become -1.
    public static long commentLong(long original) { return Flags.isOn(COMMENTS) ? Integer.MAX_VALUE : original; }
    public static int repost(int original) { return Flags.isOn(REPOSTS) ? Integer.MAX_VALUE : original; }
    public static int hashtags(int original) { return Flags.isOn(HASHTAGS) ? Integer.MAX_VALUE : original; }
    public static boolean hashtagGate(boolean original) { return !Flags.isOn(HASHTAGS) && original; }
}
