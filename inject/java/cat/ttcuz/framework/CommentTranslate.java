package cat.ttcuz.framework;

import java.lang.reflect.Method;

/**
 * TikTok's comment translation, switched on for everyone: the translate button in the comments' header (beside the
 * close button) and translating comments as they load.
 *
 * TikTok 47.2.41 asks three readers of one experiment, X.07oM: LIZIZ ("comment translation is on for this account"),
 * LIZJ ("show its button in the comments' header") and LIZ ("may this viewer have it on this video"). The build sends
 * every call to them here; all answer yes.
 * Should the mod ever need TikTok's own answer, it is still there: {@link #theirs}.
 */
public final class CommentTranslate {
    private CommentTranslate() {}

    /** X.07oM.LIZIZ(): comment translation is on. */
    public static boolean translationEnabled() { return true; }

    /** X.07oM.LIZJ(): the translate button shows in the comments' header. */
    public static boolean translationEntrance() { return true; }

    /**
     * X.07oM.LIZ(viewerIsAuthor): may this viewer have the button on this video. TikTok answers from a second
     * experiment (X.0H8z) that most accounts are outside of -- the button never showed for them.
     */
    public static boolean translationButton(boolean viewerIsAuthor) { return true; }

    /**
     * X.07oI.LIZ(): the second auto-translation experiment ("tt_comment_auto_translation_v2"). Where it reads true,
     * the comment list skips the translation the mod switched on (X.0sK5: "!09UL && 09UJ.LIZIZ" before it hands the
     * comments to the translator) -- the button showed and nothing was translated. It reads false, so the one
     * translation that is on does the work.
     */
    public static boolean translationV2() { return false; }

    private static volatile Method reader;

    /** TikTok's own answer for one of the two readers, or false when it cannot be asked. */
    static boolean theirs(String name) {
        try {
            Method m = reader;
            if (m == null || !m.getName().equals(name)) { m = Class.forName("X.07oM").getMethod(name); reader = m; }
            return Boolean.TRUE.equals(m.invoke(null));
        } catch (Throwable error) {
            return false;
        }
    }
}
