package cat.ttcuz.framework;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public final class DownloadChoice {
    private static final Map<Dialog, Boolean> offlineSheets = new WeakHashMap();
    private static final Map<Window, Object> posts = new WeakHashMap();
    private static final Map<Window, Boolean> guarded = new WeakHashMap();

    public static void show(final Dialog dialog) {
        dialog.show();
        try { Stickers.sheetShown(dialog); } catch (Throwable error) { Diary.note("sticker sheet: " + error); }
        if (dialog.getClass().getName().startsWith("cat.ttcuz.framework.") || dialog.getWindow() == null) {
            return;
        }
        Object found = SheetPost.of(dialog);
        if (found == null) {
            found = VideoActions.downloadPost(dialog.getContext());
        }
        final Object of = found;
        if (of == null) {
            return;
        }
        guard(dialog, of);
        final View decorView = dialog.getWindow().getDecorView();
        decorView.post(new Runnable() { // from class: cat.ttcuz.framework.DownloadChoice$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DownloadChoice.bind(decorView, dialog, of, 0, new int[]{512});
            }
        });
        int[] iArr = {150, 500};
        for (int i = 0; i < 2; i++) {
            decorView.postDelayed(new Runnable() { // from class: cat.ttcuz.framework.DownloadChoice$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    DownloadChoice.lambda$show$1(dialog, decorView, of);
                }
            }, iArr[i]);
        }
    }

    static /* synthetic */ void lambda$show$1(Dialog dialog, View view, Object obj) {
        if (dialog.isShowing()) {
            bind(view, dialog, obj, 0, new int[]{512});
        }
    }

    static boolean saveLabel(CharSequence charSequence) {
        if (charSequence == null) {
            return false;
        }
        String lowerCase = charSequence.toString().trim().toLowerCase(Locale.ROOT);
        return lowerCase.equals("скачать") || lowerCase.equals("download") || lowerCase.equals("завантажити") || lowerCase.equals("сохранить видео") || lowerCase.equals("save video") || lowerCase.equals("скачать видео") || lowerCase.equals("завантажити відео") || lowerCase.equals("зберегти відео") || lowerCase.equals("download video") || lowerCase.equals("скачать фото") || lowerCase.equals("сохранить фото") || lowerCase.equals("download photo") || lowerCase.equals("save photo") || lowerCase.equals("завантажити фото") || lowerCase.equals("зберегти фото");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void bind(View view, final Dialog dialog, final Object obj, int i, int[] iArr) {
        if (obj == null || i > 20) {
            return;
        }
        int i2 = iArr[0];
        iArr[0] = i2 - 1;
        if (i2 <= 0) {
            return;
        }
        if (saveLabel(view instanceof TextView ? ((TextView) view).getText() : view.getContentDescription())) {
            View walk = view;
            for (int i3 = 0; i3 < 4 && !walk.hasOnClickListeners() && (walk.getParent() instanceof View); i3++) {
                walk = (View) walk.getParent();
            }
            final View view2 = walk;
            if (view2.hasOnClickListeners() && cell(view2) && !offline(dialog)) {
                // "Скачать" goes first in its row, once per sheet
                synchronized (movedFirst) { if (!movedFirst.containsKey(dialog)) { movedFirst.put(dialog, Boolean.TRUE); view2.post(() -> first(view2)); } }
                view2.setOnClickListener(new View.OnClickListener() { // from class: cat.ttcuz.framework.DownloadChoice$$ExternalSyntheticLambda3
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view3) {
                        DownloadChoice.lambda$bind$2(dialog, view2, obj, view3);
                    }
                });
                return;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i4 = 0; i4 < viewGroup.getChildCount(); i4++) {
                bind(viewGroup.getChildAt(i4), dialog, obj, i + 1, iArr);
            }
        }
    }

    private static final Map<Dialog, Boolean> movedFirst = new WeakHashMap<>();

    /**
     * Moves the share sheet's "Скачать" cell to the front of its row. A plain layout just takes it out and puts it at
     * index 0; TikTok's RecyclerView is changed in its adapter's own list (the one as long as the adapter), moved with
     * notifyItemMoved and scrolled back to the start, so the row stays TikTok's.
     */
    static void first(View cell) {
        try {
            View item = cell;
            Object up = item.getParent();
            for (int i = 0; i < 6 && up instanceof View; i++) {
                if (recycler(up)) break;
                item = (View) up;
                up = item.getParent();
            }
            if (recycler(up)) {
                Object list = up;
                int at = (Integer) list.getClass().getMethod("getChildAdapterPosition", View.class).invoke(list, item);
                if (at <= 0) return;
                Object adapter = list.getClass().getMethod("getAdapter").invoke(list);
                if (adapter == null) return;
                int count = (Integer) adapter.getClass().getMethod("getItemCount").invoke(adapter);
                java.util.List<Object> items = itemsOf(adapter, count);
                if (items == null || at >= items.size()) return;
                items.add(0, items.remove(at));
                adapter.getClass().getMethod("notifyItemMoved", int.class, int.class).invoke(adapter, at, 0);
                list.getClass().getMethod("scrollToPosition", int.class).invoke(list, 0);
                return;
            }
            if (cell.getParent() instanceof ViewGroup && !(cell.getParent() instanceof android.widget.AdapterView)) {
                ViewGroup row = (ViewGroup) cell.getParent();
                if (row.indexOfChild(cell) <= 0) return;
                row.removeView(cell);
                row.addView(cell, 0);
            }
        } catch (Throwable error) { Diary.note("download first: " + error); }
    }

    private static boolean recycler(Object view) {
        for (Class<?> type = view == null ? null : view.getClass(); type != null; type = type.getSuperclass())
            if (type.getName().equals("androidx.recyclerview.widget.RecyclerView")) return true;
        return false;
    }

    /** The adapter's list of items: a List field (here or in a superclass) exactly as long as the adapter says. */
    @SuppressWarnings("unchecked")
    private static java.util.List<Object> itemsOf(Object adapter, int count) throws IllegalAccessException {
        for (Class<?> type = adapter.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object value = field.get(adapter);
                if (value instanceof java.util.List && ((java.util.List<?>) value).size() == count) {
                    try { ((java.util.List<Object>) value).add(0, ((java.util.List<Object>) value).remove(0)); return (java.util.List<Object>) value; }
                    catch (UnsupportedOperationException fixed) { /* not a list it can change: look on */ }
                }
            }
        }
        return null;
    }

    static /* synthetic */ void lambda$bind$2(Dialog dialog, View view, Object obj, View view2) {
        dialog.dismiss();
        VideoActions.chooseDownload(view.getContext(), currentPost(dialog, obj));
    }

    static boolean cell(View view) {
        View rootView = view.getRootView();
        int width = rootView == null ? 0 : rootView.getWidth();
        return width <= 0 || view.getWidth() <= 0 || ((float) view.getWidth()) < ((float) width) * 0.45f;
    }

    private static boolean offline(Dialog dialog) {
        Map<Dialog, Boolean> map = offlineSheets;
        synchronized (map) {
            Boolean bool = map.get(dialog);
            if (bool != null) {
                return bool.booleanValue();
            }
            Window window = dialog.getWindow();
            if (window == null) {
                return false;
            }
            int[] iArr = {512};
            boolean offlineIn = offlineIn(window.getDecorView(), 0, iArr);
            if (offlineIn || iArr[0] < 500) {
                synchronized (map) {
                    map.put(dialog, Boolean.valueOf(offlineIn));
                }
            }
            return offlineIn;
        }
    }

    static boolean offlineWords(CharSequence charSequence) {
        if (charSequence == null || charSequence.length() < 6) {
            return false;
        }
        String lowerCase = charSequence.toString().toLowerCase(Locale.ROOT);
        return lowerCase.contains("офлайн") || lowerCase.contains("оффлайн") || lowerCase.contains("offline");
    }

    private static boolean offlineIn(View view, int i, int[] iArr) {
        if (view != null && i <= 24) {
            int i2 = iArr[0];
            iArr[0] = i2 - 1;
            if (i2 > 0) {
                if (view.getClass().getName().contains(".offlinemode.")) {
                    return true;
                }
                if ((view instanceof TextView) && offlineWords(((TextView) view).getText())) {
                    return true;
                }
                if (view instanceof ViewGroup) {
                    ViewGroup viewGroup = (ViewGroup) view;
                    for (int i3 = 0; i3 < viewGroup.getChildCount(); i3++) {
                        if (offlineIn(viewGroup.getChildAt(i3), i + 1, iArr)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static Object currentPost(Dialog dialog, Object obj) {
        Map<Window, Object> map = posts;
        synchronized (map) {
            Object obj2 = map.get(dialog.getWindow());
            if (obj2 != null) {
                obj = obj2;
            }
        }
        return obj;
    }

    private static void guard(final Dialog dialog, final Object obj) {
        final Window.Callback callback;
        final Window window = dialog.getWindow();
        if (window == null || (callback = window.getCallback()) == null) {
            return;
        }
        Map<Window, Object> map = posts;
        synchronized (map) {
            map.put(window, obj);
        }
        Map<Window, Boolean> map2 = guarded;
        synchronized (map2) {
            if (map2.containsKey(window)) {
                return;
            }
            map2.put(window, Boolean.TRUE);
            final float[] fArr = new float[2];
            final long[] jArr = {0};
            final Rect[] rectArr = {null};
            final int scaledTouchSlop = ViewConfiguration.get(dialog.getContext()).getScaledTouchSlop();
            window.setCallback((Window.Callback) Proxy.newProxyInstance(callback.getClass().getClassLoader(), new Class[]{Window.Callback.class}, new InvocationHandler() { // from class: cat.ttcuz.framework.DownloadChoice$$ExternalSyntheticLambda2
                @Override // java.lang.reflect.InvocationHandler
                public final Object invoke(Object obj2, Method method, Object[] objArr) throws Throwable {
                    return DownloadChoice.lambda$guard$4(rectArr, dialog, window, fArr, jArr, scaledTouchSlop, callback, obj, obj2, method, objArr);
                }
            }));
        }
    }

    static /* synthetic */ Object lambda$guard$4(Rect[] rectArr, final Dialog dialog, Window window, float[] fArr, long[] jArr, int i, Window.Callback callback, final Object obj, Object obj2, Method method, Object[] objArr) throws Throwable {
        if (method.getName().equals("dispatchTouchEvent") && objArr != null && objArr.length == 1) {
            Object obj3 = objArr[0];
            if (obj3 instanceof MotionEvent) {
                MotionEvent motionEvent = (MotionEvent) obj3;
                try {
                    int actionMasked = motionEvent.getActionMasked();
                    Rect rect = null;
                    if (actionMasked == 0) {
                        if (!offline(dialog)) {
                            rect = saveZone(window.getDecorView(), (int) motionEvent.getRawX(), (int) motionEvent.getRawY(), new int[]{512});
                        }
                        rectArr[0] = rect;
                        fArr[0] = motionEvent.getRawX();
                        fArr[1] = motionEvent.getRawY();
                        jArr[0] = motionEvent.getEventTime();
                    } else {
                        Rect rect2 = rectArr[0];
                        if (rect2 != null) {
                            if (actionMasked != 3 && actionMasked != 5) {
                                if (actionMasked == 2) {
                                    if (Math.hypot(motionEvent.getRawX() - fArr[0], motionEvent.getRawY() - fArr[1]) > i) {
                                        rectArr[0] = null;
                                    }
                                } else if (actionMasked == 1) {
                                    boolean z = rect2.contains((int) motionEvent.getRawX(), (int) motionEvent.getRawY()) && motionEvent.getEventTime() - jArr[0] < 600;
                                    rectArr[0] = null;
                                    if (z) {
                                        MotionEvent obtain = MotionEvent.obtain(motionEvent);
                                        obtain.setAction(3);
                                        try {
                                            method.invoke(callback, obtain);
                                            obtain.recycle();
                                            final Context context = dialog.getContext();
                                            window.getDecorView().post(new Runnable() { // from class: cat.ttcuz.framework.DownloadChoice$$ExternalSyntheticLambda4
                                                @Override // java.lang.Runnable
                                                public final void run() {
                                                    DownloadChoice.lambda$guard$3(dialog, context, obj);
                                                }
                                            });
                                            return Boolean.TRUE;
                                        } catch (Throwable th) {
                                            obtain.recycle();
                                            throw th;
                                        }
                                    }
                                }
                            }
                            rectArr[0] = null;
                        }
                    }
                } catch (Throwable th2) {
                    Diary.note("download guard: " + String.valueOf(th2));
                }
            }
        }
        try {
            return method.invoke(callback, objArr);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    static /* synthetic */ void lambda$guard$3(Dialog dialog, Context context, Object obj) {
        try {
            dialog.dismiss();
            VideoActions.chooseDownload(context, currentPost(dialog, obj));
        } catch (Throwable th) {
            Diary.note("download menu: " + String.valueOf(th));
        }
    }

    private static Rect saveZone(View view, int i, int i2, int[] iArr) {
        if (view != null) {
            int i3 = iArr[0];
            iArr[0] = i3 - 1;
            if (i3 > 0 && view.getVisibility() == 0) {
                if (saveLabel(view instanceof TextView ? ((TextView) view).getText() : view.getContentDescription())) {
                    View view2 = view;
                    for (int i4 = 0; i4 < 4 && !view2.isClickable() && !view2.hasOnClickListeners() && (view2.getParent() instanceof View); i4++) {
                        view2 = (View) view2.getParent();
                    }
                    if (!cell(view2)) {
                        return null;
                    }
                    int[] iArr2 = new int[2];
                    view2.getLocationOnScreen(iArr2);
                    int i5 = iArr2[0];
                    Rect rect = new Rect(i5, iArr2[1], view2.getWidth() + i5, iArr2[1] + view2.getHeight());
                    if (rect.contains(i, i2)) {
                        return rect;
                    }
                }
                if (view instanceof ViewGroup) {
                    ViewGroup viewGroup = (ViewGroup) view;
                    for (int i6 = 0; i6 < viewGroup.getChildCount(); i6++) {
                        Rect saveZone = saveZone(viewGroup.getChildAt(i6), i, i2, iArr);
                        if (saveZone != null) {
                            return saveZone;
                        }
                    }
                }
            }
        }
        return null;
    }
}
