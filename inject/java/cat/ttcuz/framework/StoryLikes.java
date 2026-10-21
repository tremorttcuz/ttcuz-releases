package cat.ttcuz.framework;

import android.content.Context;
import android.view.View;

/** Reads TikTok's supplied statistics; never invents counts or publishes a view receipt. */
public final class StoryLikes {

    private StoryLikes() {}

    static long count(Object post) {
        Object stats = NativeRead.get(post, "getStatistics");
        Object value = NativeRead.get(stats, "getDiggCount");
        return value instanceof Number ? Math.max(-1L, ((Number) value).longValue()) : -1L;
    }

    public static void bind(Object component) {
        Object content = NativeRead.get(component, "getContentView");
        if (!(content instanceof View)) return;
        View root = (View) content;
        View.OnLongClickListener hold = view -> {
            Object post = NativeRead.get(NativeRead.item(component), "getAweme");
            if (post == null) return false;
            show(view.getContext(), count(post));
            return true;
        };
        root.setOnLongClickListener(hold);
        for (int id : new int[] { 0x7f0a2193, 0x7f0a219f }) {
            View heart = root.findViewById(id);
            if (heart != null) heart.setOnLongClickListener(hold);
        }
    }

    static void openDate(View row, View cell) {
        Object tag = cell == null ? null : cell.getTag(DateInfo.CELL_TAG);
        DateInfo.Data data =
            tag instanceof DateInfo.Data ? (DateInfo.Data) tag : Dates.selectedInfo;
        if (row != null) show(row.getContext(), data == null ? -1 : data.likes);
    }

    static void show(Context context, long likes) {
        String number =
            likes < 0 ? "Недоступно" : java.text.NumberFormat.getIntegerInstance().format(likes);
        Panel.with(context, Skin.remembered(context), "Количество лайков")
            .info(
                "favorite",
                number,
                likes < 0
                    ? "TikTok не передал число лайков для этой истории."
                    : "По данным TikTok для открытой публикации",
                null
            )
            .quiet("Понятно", null)
            .show();
    }
}
