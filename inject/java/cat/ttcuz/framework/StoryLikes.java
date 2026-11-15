package cat.ttcuz.framework;

import android.content.Context;
import android.view.View;
import cat.ttcuz.framework.DateInfo;
import java.text.NumberFormat;

public final class StoryLikes {
    private StoryLikes() {
    }

    static long count(Object obj) {
        Object obj2 = NativeRead.get(NativeRead.get(obj, "getStatistics"), "getDiggCount");
        if (obj2 instanceof Number) {
            return Math.max(-1L, ((Number) obj2).longValue());
        }
        return -1L;
    }

    public static void bind(final Object obj) {
        Object obj2 = NativeRead.get(obj, "getContentView");
        if (obj2 instanceof View) {
            View view = (View) obj2;
            View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() { // from class: cat.ttcuz.framework.StoryLikes$$ExternalSyntheticLambda0
                @Override // android.view.View.OnLongClickListener
                public final boolean onLongClick(View view2) {
                    return StoryLikes.lambda$bind$0(obj, view2);
                }
            };
            view.setOnLongClickListener(onLongClickListener);
            int[] iArr = {2131370368, 2131370380};
            for (int i = 0; i < 2; i++) {
                View findViewById = view.findViewById(iArr[i]);
                if (findViewById != null) {
                    findViewById.setOnLongClickListener(onLongClickListener);
                }
            }
        }
    }

    static /* synthetic */ boolean lambda$bind$0(Object obj, View view) {
        Object obj2 = NativeRead.get(NativeRead.item(obj), "getAweme");
        if (obj2 == null) {
            return false;
        }
        show(view.getContext(), count(obj2));
        return true;
    }

    static void openDate(View view, View view2) {
        Object tag = view2 == null ? null : view2.getTag(2114782005);
        DateInfo.Data data = tag instanceof DateInfo.Data ? (DateInfo.Data) tag : Dates.selectedInfo;
        if (view != null) {
            show(view.getContext(), data == null ? -1L : data.likes);
        }
    }

    static void show(Context context, long j) {
        String str;
        String format = j < 0 ? Text.t("Недоступно") : NumberFormat.getIntegerInstance().format(j);
        Panel with = Panel.with(context, Skin.remembered(context), Text.t("Количество лайков"));
        if (j < 0) {
            str = Text.t("TikTok не передал число лайков для этой истории.");
        } else {
            str = Text.t("По данным TikTok для открытой публикации");
        }
        with.info("favorite", format, str, null).quiet(Text.t("Понятно"), null).show();
    }
}
