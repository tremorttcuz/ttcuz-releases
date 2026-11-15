package cat.ttcuz.framework;

import android.graphics.Rect;
import android.view.View;
import java.util.Iterator;

final class RailPresence {
    private RailPresence() {
    }

    static boolean onScreen(View view) {
        if (view != null && view.getWidth() > 0 && view.getHeight() > 0) {
            Rect rect = new Rect();
            int[] iArr = new int[2];
            view.getLocationOnScreen(iArr);
            Iterator<View> it = FeedRail.views().iterator();
            while (it.hasNext()) {
                View next = it.next();
                if (next != null && next.isAttachedToWindow() && next.getRootView() == view) {
                    if (next.getVisibility() != 0 && (next.getParent() instanceof View)) {
                        next = (View) next.getParent();
                    }
                    if (next.isShown() && Dim.originalAlpha(next) > 0.01f && next.getGlobalVisibleRect(rect)) {
                        int i = rect.right - rect.left;
                        int i2 = rect.bottom - rect.top;
                        if (i > 0 && i2 > 0 && i >= next.getWidth() / 2 && i2 >= next.getHeight() / 2 && rect.left >= iArr[0] && rect.top >= iArr[1] && rect.right <= iArr[0] + view.getWidth() + 2 && rect.bottom <= iArr[1] + view.getHeight() + 2) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
