package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewParent;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keep the place in a chat: while older messages are being read, a new or a sent message does not pull the list down
 * to the newest one. The "down" button still does, because what it asks for comes from a tap.
 *
 * The build sends TikTok's list scrolls (RecyclerView.scrollToPosition / smoothScrollToPosition and
 * LinearLayoutManager.scrollToPositionWithOffset) here. Only the lists MessageTimes has seen messages in are chats;
 * every other list scrolls as it always did.
 */
public final class ChatScroll {
    private ChatScroll() {}

    static final String KEY = "chat_keep_position";
    private static final Map<View, Boolean> chats = new WeakHashMap<>();
    private static final Map<Object, WeakReference<View>> managers = new WeakHashMap<>();
    private static final Map<Class<?>, Method[]> methods = new WeakHashMap<>();

    static boolean enabled() {
        Context context = Ttcuz.context();
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY, false);
    }

    static void set(boolean on) {
        Context context = Ttcuz.context();
        if (context != null) context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply();
    }

    /** A message row was bound: the list it is in is a chat. */
    static void track(View row) {
        for (ViewParent up = row.getParent(); up instanceof View; up = up.getParent()) {
            View view = (View) up;
            if (view.getClass().getName().contains("RecyclerView") || isRecycler(view.getClass())) {
                synchronized (chats) {
                    if (chats.put(view, Boolean.TRUE) == null) {
                        Object manager = call(view, "getLayoutManager");
                        if (manager != null) managers.put(manager, new WeakReference<>(view));
                    }
                }
                return;
            }
        }
    }

    private static boolean isRecycler(Class<?> type) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) if (c.getName().equals("androidx.recyclerview.widget.RecyclerView")) return true;
        return false;
    }

    /** Whether this scroll would take a chat being read back to its newest message, without a tap asking for it. */
    private static boolean skip(View list, int position) {
        if (list == null || !enabled()) return false;
        synchronized (chats) { if (!chats.containsKey(list)) return false; }
        // at the newest already: nothing to keep
        if (!list.canScrollVertically(1)) return false;
        Object adapter = call(list, "getAdapter");
        Object count = adapter == null ? null : call(adapter, "getItemCount");
        Object manager = call(list, "getLayoutManager");
        Object reverse = manager == null ? null : call(manager, "getReverseLayout");
        if (!(count instanceof Integer)) return false;
        int newest = Boolean.TRUE.equals(reverse) ? 0 : (Integer) count - 1;
        if (Math.abs(position - newest) > 1) return false;
        return !fromTap();
    }

    /** A click handler on the stack: the person asked for this scroll. */
    private static boolean fromTap() {
        for (StackTraceElement frame : new Throwable().getStackTrace()) {
            String name = frame.getMethodName();
            if (name.equals("performClick") || name.equals("performClickInternal") || name.equals("onClick")) return true;
        }
        return false;
    }

    public static void scrollToPosition(Object list, int position) {
        if (list instanceof View && skip((View) list, position)) return;
        invoke(list, "scrollToPosition", position);
    }

    public static void smoothScrollToPosition(Object list, int position) {
        if (list instanceof View && skip((View) list, position)) return;
        invoke(list, "smoothScrollToPosition", position);
    }

    /** LinearLayoutManager.scrollToPositionWithOffset, and RecyclerView.LayoutManager.scrollToPosition. */
    public static void scrollToPositionWithOffset(Object manager, int position, int offset) {
        if (skip(listOf(manager), position)) return;
        invoke(manager, "scrollToPositionWithOffset", position, offset);
    }

    public static void managerScrollToPosition(Object manager, int position) {
        if (skip(listOf(manager), position)) return;
        invoke(manager, "scrollToPosition", position);
    }

    private static View listOf(Object manager) {
        WeakReference<View> list;
        synchronized (chats) { list = managers.get(manager); }
        return list == null ? null : list.get();
    }

    private static Object call(Object target, String name) {
        try {
            Method method = find(target.getClass(), name, 0);
            return method == null ? null : method.invoke(target);
        } catch (Throwable error) {
            return null;
        }
    }

    private static void invoke(Object target, String name, int... args) {
        try {
            Method method = find(target.getClass(), name, args.length);
            if (method == null) throw new NoSuchMethodException(name);
            if (args.length == 1) method.invoke(target, args[0]);
            else method.invoke(target, args[0], args[1]);
        } catch (java.lang.reflect.InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
        } catch (Throwable error) {
            Diary.note("chat scroll: " + error);
        }
    }

    /** A public method by name and number of int arguments, found once per class (virtual dispatch is kept). */
    private static Method find(Class<?> type, String name, int ints) {
        String key = name + ints;
        synchronized (methods) {
            Method[] known = methods.get(type);
            if (known == null) { known = new Method[8]; methods.put(type, known); }
            int slot = (key.hashCode() & 0x7FFFFFFF) % 8;
            Method cached = known[slot];
            if (cached != null && cached.getName().equals(name) && cached.getParameterTypes().length == ints) return cached;
            for (Method method : type.getMethods()) {
                if (!method.getName().equals(name) || method.getParameterTypes().length != ints) continue;
                boolean plain = true;
                for (Class<?> p : method.getParameterTypes()) plain &= p == int.class;
                if (!plain) continue;
                known[slot] = method;
                return method;
            }
            return null;
        }
    }
}
