package cat.ttcuz.framework;

/** The native multi-photo comment album, not the general video/photo uploader. */
public final class CommentPhotos {

    private CommentPhotos() {}

    public static java.util.List images(java.util.List images) {
        return images == null || images.size() <= 9
            ? images
            : new java.util.ArrayList(images.subList(0, 9));
    }

    public static int limit(Object scene, int requested) {
        try {
            Object session = NativeRead.field(scene, "sessionData");
            return "comment_multi_photo".equals(NativeRead.field(session, "businessKey"))
                ? 9
                : requested;
        } catch (Throwable ignored) {
            return requested;
        }
    }
}
