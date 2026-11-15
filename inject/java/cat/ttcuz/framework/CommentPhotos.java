package cat.ttcuz.framework;

import java.util.ArrayList;
import java.util.List;

public final class CommentPhotos {
    private CommentPhotos() {
    }

    public static List images(List list) {
        return (list == null || list.size() <= 9) ? list : new ArrayList(list.subList(0, 9));
    }

    public static int limit(Object obj, int i) {
        try {
            if ("comment_multi_photo".equals(NativeRead.field(NativeRead.field(obj, "sessionData"), "businessKey"))) {
                return 9;
            }
            return i;
        } catch (Throwable unused) {
            return i;
        }
    }
}
