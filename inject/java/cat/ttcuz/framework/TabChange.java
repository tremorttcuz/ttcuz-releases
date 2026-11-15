package cat.ttcuz.framework;

/**
 * TikTok's TabChangeManager writes two lines to its own log on every switch of the bottom tabs ("isSafeTabHost: ...",
 * "changeTab: last: ..."); nobody reads them on a phone. The build sends those two calls here, where they end.
 */
public final class TabChange {
    private TabChange() {}

    /** X.05f6.LIZ(message, extra): the tab log. */
    public static void LIZIZ(Object log, String message, Object extra) {}

    /** X.06np.LJI(tag, message): the feed tab's log. */
    public static void LJI(String tag, String message) {}
}
