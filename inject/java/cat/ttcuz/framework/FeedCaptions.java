package cat.ttcuz.framework;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

/** Native caption placement only; lettering burned into a video is not a View. */
public final class FeedCaptions {

    private static final int WATCHED = 0x4D634170;
    private static final int SPEED_WATCHED = 0x4D735064;
    private static final java.util.regex.Pattern SPEED = java.util.regex.Pattern.compile(
        "(?iu)(?:Скорость|Speed|Швидкість|Ускорено|Ускорение|Sped up|Accelerated|Прискорено)\\s*:?\\s*(?:2(?:[.,]0)?\\s*[xх×]|[xх×]\\s*2(?:[.,]0)?).*"
    );

    private FeedCaptions() {}

    static void speedLabel(TextView view, CharSequence text) {
        // Native lock feedback must stay visible and interactive.
    }

    /** Watch the native edge-speed component even when it uses resource-id text setters. */
    public static void bindSpeed(View container) {
        // The native component now owns both the feedback and the release gesture.
    }

    private static void scanSpeed(View view, int depth, int[] budget) {
        if (view == null || !view.isShown() || depth > 30 || budget[0]-- <= 0) return;
        if (view instanceof TextView) speedLabel((TextView) view, ((TextView) view).getText());
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) scanSpeed(
                group.getChildAt(i),
                depth + 1,
                budget
            );
        }
    }

    public static void bindNative(Object holder) {
        try {
            Object view = holder.getClass().getMethod("getContentView").invoke(holder);
            if (view instanceof View) bind((View) view);
        } catch (Throwable ignored) {}
    }

    public static void bind(View container) {
        if (container == null) return;
        // Exact native caption frame in TikTok 47.2.41, not the entire cell.
        View found = container.findViewById(0x7f0a1397);
        if (found == null) found = container.getRootView().findViewById(0x7f0a1397);
        final View caption = found;
        if (caption == null || Boolean.TRUE.equals(caption.getTag(WATCHED))) return;
        caption.setTag(WATCHED, Boolean.TRUE);
        final float original = caption.getTranslationY();
        caption.getViewTreeObserver().addOnPreDrawListener(
            new ViewTreeObserver.OnPreDrawListener() {
                boolean shifted;
                final int[] at = new int[2],
                    origin = new int[2];
                final java.util.Map<ViewGroup, Boolean> paddingClips =
                    new java.util.WeakHashMap<>();
                int navigation = -1;
                final java.util.Map<ViewGroup, Boolean> clips = new java.util.WeakHashMap<
                    ViewGroup,
                    Boolean
                >();

                public boolean onPreDraw() {
                    View root = caption.getRootView();
                    boolean low = SpeedGesture.captionsLow(root);
                    if (!low || !caption.isShown()) {
                        if (shifted) {
                            caption.setTranslationY(original);
                            shifted = false;
                            for (java.util.Map.Entry<ViewGroup, Boolean> entry : clips.entrySet())
                                entry.getKey().setClipChildren(entry.getValue());
                            clips.clear();
                            for (java.util.Map.Entry<
                                ViewGroup,
                                Boolean
                            > entry : paddingClips.entrySet())
                                entry.getKey().setClipToPadding(entry.getValue());
                            paddingClips.clear();
                        }
                        return true;
                    }
                    android.view.ViewParent parent = caption.getParent();
                    while (parent instanceof ViewGroup) {
                        ViewGroup group = (ViewGroup) parent;
                        if (!clips.containsKey(group)) {
                            clips.put(group, group.getClipChildren());
                            paddingClips.put(group, group.getClipToPadding());
                            group.setClipChildren(false);
                            group.setClipToPadding(false);
                        }
                        parent = group.getParent();
                    }
                    caption.getLocationOnScreen(at);
                    root.getLocationOnScreen(origin);
                    float dp = caption.getResources().getDisplayMetrics().density;
                    int nav = 0;
                    if (navigation < 0) {
                        int id = caption
                            .getResources()
                            .getIdentifier("navigation_bar_height", "dimen", "android");
                        navigation = id == 0 ? 0 : caption.getResources().getDimensionPixelSize(id);
                    }
                    nav = navigation;
                    float y =
                        caption.getTranslationY() +
                        root.getHeight() -
                        nav -
                        12 * dp -
                        caption.getHeight() -
                        (at[1] - origin[1]);
                    if (Math.abs(caption.getTranslationY() - y) > 0.5f) caption.setTranslationY(y);
                    shifted = true;
                    return true;
                }
            }
        );
    }
}
