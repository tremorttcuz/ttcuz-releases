package cat.ttcuz.framework;

/**
 * Written by the build. Do not edit.
 *
 * Where the methods the mod hands calls back to actually live in the
 * apk it was built from. Their signatures are in the patcher and are
 * the same every release; these names are not, which is why they are
 * found rather than written down.
 */
final class Anchors {

    private Anchors() {}

    /** comment sticker tapped */
    static final String COMMENT_STICKER_TAPPED = "X.0iw1";
    static final String COMMENT_STICKER_TAPPED_METHOD = "LIZ";

    /** comment sticker sheet: not in this release */
    static final String COMMENT_STICKER_SHEET = "";
    static final String COMMENT_STICKER_SHEET_METHOD = "";

    /** comment sticker sheet long */
    static final String COMMENT_STICKER_SHEET_LONG = "X.0MG8";
    static final String COMMENT_STICKER_SHEET_LONG_METHOD = "LJI";
}
