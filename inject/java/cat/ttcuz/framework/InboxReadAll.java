package cat.ttcuz.framework;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Explicit inbox action. Uses TikTok's current-account SDK and authenticated notice API. */
public final class InboxReadAll {
    private static final AtomicBoolean busy = new AtomicBoolean();
    private static final int TAG = 0x54545241;
    private static final Handler main = new Handler(Looper.getMainLooper());

    private InboxReadAll() {}

    public static void addMenu(ViewGroup menu) {
        if (menu == null || Boolean.TRUE.equals(menu.getTag(TAG))) return;
        try {
            Context context = menu.getContext();
            Class<?> cell = Class.forName("X.0lnT");
            View row = (View) cell.getConstructor(Context.class, android.util.AttributeSet.class, int.class)
                .newInstance(context, null, 6);
            cell.getMethod("setTitle", CharSequence.class).invoke(row, "Прочитать все");
            // Official 47.2.41 raw asset: icon_bubble_right_tick.
            Class<?> icon = Class.forName("X.0GtW");
            Object picture = icon.getConstructor().newInstance();
            icon.getField("LIZ").setInt(picture, 0x7f010342);
            icon.getField("LJ").set(picture, Integer.valueOf(0x7f0603f2));
            cell.getMethod("setIcon", icon).invoke(row, picture);
            row.setContentDescription("Прочитать все сообщения и активность");
            bindClick(row, InboxReadAll::start);
            menu.addView(row, new ViewGroup.LayoutParams(-1, -2));
            menu.setTag(TAG, Boolean.TRUE);
        } catch (Exception error) {
            Diary.note("inbox read-all menu: " + error);
        }
    }

    private static void bindClick(View row, Runnable action) throws Exception {
        // TuxCell deliberately ignores View.OnClickListener; stock tools use Function1.
        Class<?> callback = Class.forName("kotlin.jvm.functions.Function1");
        // The shipped 47.2.41 Kotlin Unit singleton is renamed to LIZ.
        Object unit = Class.forName("kotlin.Unit").getField("LIZ").get(null);
        Object listener = Proxy.newProxyInstance(callback.getClassLoader(), new Class<?>[] {callback},
            (proxy, method, args) -> {
                if (method.getName().equals("invoke")) {
                    action.run();
                    return unit;
                }
                return objectMethod(proxy, method, args);
            });
        row.getClass().getMethod("setOnClickListener", callback).invoke(row, listener);
    }

    static void start() {
        String uid = Account.liveId();
        if (uid == null) {
            Screen.say("Сначала войдите в аккаунт TikTok");
            return;
        }
        if (!busy.compareAndSet(false, true)) return;
        Screen.say("Отмечаю сообщения и активность прочитанными…");
        Net.away("inbox read all", () -> {
            boolean chatsRead = false, noticesRead = false;
            try {
                try {
                    checkAccount(uid);
                    Object companion = Class.forName("X.06WS").getField("LIZ").get(null);
                    Object conversations = Class.forName("X.06WS")
                        .getMethod("LIZIZ", companion.getClass()).invoke(null, companion);
                    if (conversations == null) throw new IllegalStateException("IM SDK unavailable");
                    List<Object> unread = unread(conversations);
                    onMain(() -> {
                        checkAccount(uid);
                        conversations.getClass().getMethod("LJIJI", List.class, Class.forName("X.0Tf0"))
                            .invoke(conversations, unread, null);
                    });
                    long until = android.os.SystemClock.uptimeMillis() + 15000;
                    while (stillUnread(conversations, unread) && android.os.SystemClock.uptimeMillis() < until) {
                        checkAccount(uid);
                        Thread.sleep(150);
                    }
                    chatsRead = !stillUnread(conversations, unread);
                } catch (Exception error) {
                    failure("messages", error);
                }
                // Activity must run even when the IM SDK fails or read receipts are hidden.
                try {
                    readNotices(uid);
                    noticesRead = true;
                } catch (Exception error) {
                    failure("activity", error);
                }
                checkAccount(uid);
                final String message = resultMessage(chatsRead, noticesRead);
                main.post(() -> {
                    if (uid.equals(Account.liveId())) Screen.say(message);
                });
            } catch (Exception error) {
                failure("account", error);
            } finally {
                busy.set(false);
            }
        });
    }

    private static String resultMessage(boolean chats, boolean notices) {
        if (chats && notices) return "Сообщения и активность отмечены прочитанными";
        if (chats) return "Сообщения прочитаны. Активность прочитать не удалось — повторите.";
        if (notices) return "Активность прочитана. Часть сообщений пока остаётся непрочитанной.";
        return "Не удалось прочитать сообщения и активность. Повторите; подробности в журнале мода.";
    }

    private static void failure(String stage, Throwable error) {
        StringBuilder detail = new StringBuilder("inbox read all: ").append(stage);
        for (int i = 0; error != null && i < 6; i++, error = error.getCause())
            detail.append(" / ").append(error);
        Diary.note(detail.toString());
    }

    private static Object noticeRequest() throws Exception {
        // Match MultiViewModel's entrance request in official 47.2.41. The
        // experiment/business payloads and additional groups belong to TikTok.
        Class<?> experiments = Class.forName("X.0mFh");
        boolean cached = Boolean.TRUE.equals(Class.forName("X.09BV").getMethod("LIZ").invoke(null));
        String raw = (String) (cached ? experiments.getMethod("LIZLLL").invoke(null)
            : experiments.getMethod("LIZJ").invoke(experiments.getField("LL").get(null)));
        String params = (String) Class.forName("X.0mG6").getMethod("LIZ", String.class).invoke(null, raw);
        Object lazy = Class.forName("com.ss.android.ugc.aweme.inbox.widget.multi.MultiViewModel")
            .getField("LLLLLLZZ").get(null);
        String groups = (String) Class.forName("X.01SE").getMethod("getValue").invoke(lazy);
        String business = (String) Class.forName("X.05C0").getMethod("LIZ").invoke(null);
        Object api = Class.forName("com.ss.android.ugc.aweme.inbox.api.MultiApiManager")
            .getMethod("LIZ").invoke(null);
        return Class.forName("com.ss.android.ugc.aweme.inbox.api.MultiApiManager$API")
            .getMethod("LIZIZ", String.class, String.class, int.class,
                Boolean.class, Boolean.class, Boolean.class, String.class)
            .invoke(api, params, groups, 1, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, business);
    }

    private static void readNotices(String uid) throws Exception {
        checkAccount(uid);
        Object response = await(noticeRequest());
        checkAccount(uid);
        Object status = NativeRead.field(response, "status_code");
        if (!(status instanceof Number) || ((Number) status).intValue() != 0)
            throw new IllegalStateException("Notice mark-read rejected: " + status);
        Class<?> manager = Class.forName("X.1Gjy");
        Object notices = manager.getField("LL").get(null);
        // Include count-only groups as well as registered attributes (activity,
        // followers and other notification categories); snapshot before clearing.
        LinkedHashSet<Integer> groups = new LinkedHashSet<>();
        for (String getter : new String[] {"LJJJJ", "LJJJJI"}) {
            Map<?, ?> values = (Map<?, ?>) manager.getMethod(getter).invoke(null);
            for (Object key : values.keySet()) if (key instanceof Integer) groups.add((Integer) key);
        }
        int[] numbers = new int[groups.size()];
        int index = 0;
        for (int group : groups) numbers[index++] = group;
        onMain(() -> {
            checkAccount(uid);
            manager.getMethod("LJFF", Class.forName("X.1Exe"), boolean.class, int[].class)
                .invoke(notices, null, true, numbers);
        });
    }

    private static List<Object> unread(Object model) throws Exception {
        // Query the account database, including stranger requests and unloaded chats.
        // LIZLLL() is groups only; the visible inbox/cache is also incomplete.
        Class<?> filter = Class.forName("X.0Rr5");
        Class<?> order = Class.forName("X.0RxE");
        Class<?> cursor = Class.forName("X.06Yr");
        Class<?> query = Class.forName("X.06kl");
        Object all = query.getConstructor(List.class, order, int.class, cursor, boolean.class, String.class)
            .newInstance(java.util.Collections.singletonList(filter.getConstructor().newInstance()),
                order.getField("SORT_ORDER_DESC").get(null), -1, null, true, "ttcuz_read_all");
        Object result = model.getClass().getMethod("LJIIJ", query).invoke(model, all);
        if (Boolean.TRUE.equals(result.getClass().getMethod("LIZJ").invoke(result)))
            throw new IllegalStateException("Conversation query failed");
        Object page = NativeRead.field(result, "LIZ");
        Object value = NativeRead.field(page, "LIZIZ");
        if (!(value instanceof List<?>)) throw new IllegalStateException("Missing conversation list");
        List<Object> selected = new ArrayList<>();
        for (Object conversation : new ArrayList<>((List<?>) value)) {
            if (count(conversation) > 0) selected.add(conversation);
        }
        return selected;
    }

    private static long count(Object conversation) throws Exception {
        return ((Number) conversation.getClass().getMethod("getUnreadCount").invoke(conversation)).longValue();
    }

    private static boolean stillUnread(Object model, List<Object> conversations) throws Exception {
        for (Object conversation : conversations) {
            String id = (String) conversation.getClass().getMethod("getConversationId").invoke(conversation);
            Object current = model.getClass().getMethod("LIZ", String.class).invoke(model, id);
            if (count(current == null ? conversation : current) > 0) return true;
        }
        return false;
    }

    private static void checkAccount(String uid) {
        if (!uid.equals(Account.liveId())) throw new IllegalStateException("Account changed");
    }

    private interface Task { void run() throws Exception; }

    private static void onMain(Task task) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        Exception[] error = {null};
        AtomicBoolean cancelled = new AtomicBoolean();
        main.post(() -> {
            try { if (!cancelled.get()) task.run(); } catch (Exception failure) { error[0] = failure; }
            finally { done.countDown(); }
        });
        if (!done.await(20, TimeUnit.SECONDS)) {
            cancelled.set(true);
            throw new IllegalStateException("UI timeout");
        }
        if (error[0] != null) throw error[0];
    }

    private static Object await(Object observable) throws Exception {
        Class<?> consumer = Class.forName("X.05DQ");
        Object[] response = {null};
        Throwable[] failure = {null};
        CountDownLatch done = new CountDownLatch(1);
        Object success = Proxy.newProxyInstance(consumer.getClassLoader(), new Class<?>[] {consumer},
            (proxy, method, args) -> {
                if (method.getName().equals("accept")) { response[0] = args[0]; done.countDown(); }
                return objectMethod(proxy, method, args);
            });
        Object error = Proxy.newProxyInstance(consumer.getClassLoader(), new Class<?>[] {consumer},
            (proxy, method, args) -> {
                if (method.getName().equals("accept")) { failure[0] = (Throwable) args[0]; done.countDown(); }
                return objectMethod(proxy, method, args);
            });
        Class<?> single = Class.forName("X.19ZK");
        Object scheduler = Class.forName("X.19e5").getMethod("LIZ").invoke(null);
        Object scheduled = single.getMethod("LJLI", Class.forName("X.19do")).invoke(observable, scheduler);
        Object subscription = single.getMethod("LJJZ", consumer, consumer)
            .invoke(scheduled, success, error);
        try {
            if (!done.await(25, TimeUnit.SECONDS)) throw new IllegalStateException("Notice timeout");
            if (failure[0] != null) throw new IllegalStateException("Notice request failed", failure[0]);
            return response[0];
        } finally {
            if (subscription != null) NativeRead.get(subscription, "dispose");
        }
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args) {
        if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
        if (method.getName().equals("equals")) return proxy == args[0];
        if (method.getName().equals("toString")) return "ttcuz inbox callback";
        return null;
    }
}
