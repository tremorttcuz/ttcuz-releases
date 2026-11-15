package cat.ttcuz.framework;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class InboxReadAll {
    private static final int TAG = 1414812225;
    private static final AtomicBoolean busy = new AtomicBoolean();
    private static final Handler main = new Handler(Looper.getMainLooper());

    /* JADX INFO: Access modifiers changed from: private */
    interface Task {
        void run() throws Exception;
    }

    private InboxReadAll() {
    }

    public static void addMenu(ViewGroup viewGroup) {
        if (viewGroup == null || Boolean.TRUE.equals(viewGroup.getTag(TAG))) {
            return;
        }
        try {
            Context context = viewGroup.getContext();
            Class<?> cls = Class.forName("X.1Cep");
            View view = (View) cls.getConstructor(Context.class, AttributeSet.class, Integer.TYPE).newInstance(context, null, 6);
            cls.getMethod("setTitle", CharSequence.class).invoke(view, Text.t("Прочитать все"));
            Class<?> cls2 = Class.forName("X.04cH");
            Object newInstance = cls2.getConstructor(null).newInstance(null);
            cls2.getField("LIZ").setInt(newInstance, 2130772815);
            cls2.getField("LJ").set(newInstance, 2131100658);
            cls.getMethod("setIcon", cls2).invoke(view, newInstance);
            view.setContentDescription(Text.t("Прочитать все сообщения и активность"));
            bindClick(view, new Runnable() { // from class: cat.ttcuz.framework.InboxReadAll$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    InboxReadAll.start();
                }
            });
            viewGroup.addView(view, new ViewGroup.LayoutParams(-1, -2));
            viewGroup.setTag(TAG, Boolean.TRUE);
        } catch (Exception e) {
            Diary.note("inbox read-all menu: " + String.valueOf(e));
        }
    }

    private static void bindClick(View view, final Runnable runnable) throws Exception {
        Class<?> cls = Class.forName("kotlin.jvm.functions.Function1");
        final Object obj = Class.forName("kotlin.Unit").getField("LIZ").get(null);
        view.getClass().getMethod("setOnClickListener", cls).invoke(view, Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, new InvocationHandler() { // from class: cat.ttcuz.framework.InboxReadAll$$ExternalSyntheticLambda7
            @Override // java.lang.reflect.InvocationHandler
            public final Object invoke(Object obj2, Method method, Object[] objArr) throws Throwable {
                return InboxReadAll.lambda$bindClick$0(runnable, obj, obj2, method, objArr);
            }
        }));
    }

    static /* synthetic */ Object lambda$bindClick$0(Runnable runnable, Object obj, Object obj2, Method method, Object[] objArr) throws Throwable {
        if (method.getName().equals("invoke")) {
            runnable.run();
            return obj;
        }
        return objectMethod(obj2, method, objArr);
    }

    static void start() {
        final String liveId = Account.liveId();
        if (liveId == null) {
            Screen.say(Text.t("Сначала войдите в аккаунт TikTok"));
        } else if (busy.compareAndSet(false, true)) {
            Screen.say(Text.t("Отмечаю сообщения и активность прочитанными…"));
            Net.away("inbox read all", new Runnable() { // from class: cat.ttcuz.framework.InboxReadAll$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    InboxReadAll.lambda$start$3(liveId);
                }
            });
        }
    }

    /** Marks every unread chat (via TikTok's IM SDK, waiting up to 15 s) and the activity feed as read. */
    static void lambda$start$3(final String str) {
        boolean messages = false, notices = true;
        try {
            try {
                checkAccount(str);
                Object holder = Class.forName("X.0Clj").getField("LIZ").get(null);
                final Object im = Class.forName("X.0Clj").getMethod("LIZIZ", holder.getClass()).invoke(null, holder);
                if (im == null) throw new IllegalStateException("IM SDK unavailable");
                final List<Object> unread = unread(im);
                onMain(() -> lambda$start$1(str, im, unread));
                long until = SystemClock.uptimeMillis() + 15000;
                while (stillUnread(im, unread) && SystemClock.uptimeMillis() < until) {
                    checkAccount(str);
                    Thread.sleep(150L);
                }
                messages = !stillUnread(im, unread);
            } catch (Exception e) {
                failure("messages", e);
            }
            try {
                readNotices(str);
            } catch (Exception e) {
                failure("activity", e);
                notices = false;
            }
            checkAccount(str);
            final String result = resultMessage(messages, notices);
            main.post(() -> lambda$start$2(str, result));
        } catch (Exception e) {
            failure("account", e);
        } finally {
            busy.set(false);
        }
    }

    static /* synthetic */ void lambda$start$1(String str, Object obj, List list) throws Exception {
        checkAccount(str);
        obj.getClass().getMethod("LJIILJJIL", List.class, Class.forName("X.0sBc")).invoke(obj, list, null);
    }

    static /* synthetic */ void lambda$start$2(String str, String str2) {
        if (str.equals(Account.liveId())) {
            Screen.say(str2);
        }
    }

    private static String resultMessage(boolean z, boolean z2) {
        if (z && z2) {
            return Text.t("Сообщения и активность отмечены прочитанными");
        }
        if (z) {
            return Text.t("Сообщения прочитаны. Активность прочитать не удалось — повторите.");
        }
        if (z2) {
            return Text.t("Активность прочитана. Часть сообщений пока остаётся непрочитанной.");
        }
        return Text.t("Не удалось прочитать сообщения и активность. Повторите; подробности в журнале мода.");
    }

    private static void failure(String str, Throwable th) {
        StringBuilder sb = new StringBuilder("inbox read all: ");
        sb.append(str);
        int i = 0;
        while (th != null && i < 6) {
            sb.append(" / ");
            sb.append(th);
            i++;
            th = th.getCause();
        }
        Diary.note(sb.toString());
    }

    private static Object noticeRequest() throws Exception {
        Object invoke;
        Class<?> cls = Class.forName("X.0M6u");
        if (Boolean.TRUE.equals(Class.forName("X.0M6m").getMethod("LIZ", null).invoke(null, null))) {
            invoke = cls.getMethod("LIZLLL", null).invoke(null, null);
        } else {
            invoke = cls.getMethod("LIZJ", null).invoke(cls.getField("LL").get(null), null);
        }
        String str = (String) Class.forName("X.0M8B").getMethod("LIZ", String.class).invoke(null, (String) invoke);
        String str2 = (String) Class.forName("X.02pj").getMethod("getValue", null).invoke(Class.forName("com.ss.android.ugc.aweme.inbox.widget.multi.MultiViewModel").getField("LLLLLZIL").get(null), null);
        String str3 = (String) Class.forName("X.0M8j").getMethod("LIZ", null).invoke(null, null);
        Object invoke2 = Class.forName("com.ss.android.ugc.aweme.inbox.api.MultiApiManager").getMethod("LIZ", null).invoke(null, null);
        Method method = Class.forName("com.ss.android.ugc.aweme.inbox.api.MultiApiManager$API").getMethod("LIZIZ", String.class, String.class, Integer.TYPE, Boolean.class, Boolean.class, Boolean.class, String.class);
        Boolean bool = Boolean.FALSE;
        return method.invoke(invoke2, str, str2, 1, Boolean.TRUE, bool, bool, str3);
    }

    private static void readNotices(final String str) throws Exception {
        checkAccount(str);
        Object await = await(noticeRequest());
        checkAccount(str);
        Object field = NativeRead.field(await, "status_code");
        if (!(field instanceof Number) || ((Number) field).intValue() != 0) {
            throw new IllegalStateException("Notice mark-read rejected: " + String.valueOf(field));
        }
        final Class<?> cls = Class.forName("X.0CwH");
        final Object obj = cls.getField("LL").get(null);
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        String[] strArr = {"LJJJJ", "LJJJJI"};
        int i = 0;
        for (int i2 = 0; i2 < 2; i2++) {
            for (Object obj2 : ((Map) cls.getMethod(strArr[i2], null).invoke(null, null)).keySet()) {
                if (obj2 instanceof Integer) {
                    linkedHashSet.add((Integer) obj2);
                }
            }
        }
        final int[] iArr = new int[linkedHashSet.size()];
        Iterator it = linkedHashSet.iterator();
        while (it.hasNext()) {
            iArr[i] = ((Integer) it.next()).intValue();
            i++;
        }
        onMain(new Task() { // from class: cat.ttcuz.framework.InboxReadAll$$ExternalSyntheticLambda4
            @Override // cat.ttcuz.framework.InboxReadAll.Task
            public final void run() throws Exception {
                InboxReadAll.lambda$readNotices$4(str, cls, obj, iArr);
            }
        });
    }

    static /* synthetic */ void lambda$readNotices$4(String str, Class cls, Object obj, int[] iArr) throws Exception {
        checkAccount(str);
        cls.getMethod("LJ", Class.forName("X.0CwT"), Boolean.TYPE, int[].class).invoke(obj, null, true, iArr);
    }

    /**
     * Every conversation id TikTok's IM keeps for this account, newest first: the same query as "read all", without
     * its unread filter. Empty when the IM is not up yet. Off the main thread.
     */
    static List<String> conversationIds() {
        List<String> out = new ArrayList<>();
        try {
            Object holder = Class.forName("X.0Clj").getField("LIZ").get(null);
            Object im = Class.forName("X.0Clj").getMethod("LIZIZ", holder.getClass()).invoke(null, holder);
            if (im == null) return out;
            for (Object one : all(im)) {
                Object id = one.getClass().getMethod("getConversationId").invoke(one);
                if (id instanceof String && !((String) id).isEmpty()) out.add((String) id);
            }
        } catch (Throwable error) { Diary.note("conversations: " + error); }
        return out;
    }

    private static List<Object> unread(Object obj) throws Exception {
        List<Object> arrayList = new ArrayList<>();
        for (Object next : all(obj)) if (count(next) > 0) arrayList.add(next);
        return arrayList;
    }

    private static List<Object> all(Object obj) throws Exception {
        Class<?> cls = Class.forName("X.0sBH");
        Class<?> cls2 = Class.forName("X.0hb0");
        Class<?> cls3 = Class.forName("X.0ViB");
        Class<?> cls4 = Class.forName("X.0hbB");
        Object invoke = obj.getClass().getMethod("LIZIZ", cls4).invoke(obj, cls4.getConstructor(List.class, cls2, Integer.TYPE, cls3, Boolean.TYPE, String.class).newInstance(Collections.singletonList(cls.getConstructor(null).newInstance(null)), cls2.getField("SORT_ORDER_DESC").get(null), -1, null, true, "ttcuz_read_all"));
        if (Boolean.TRUE.equals(invoke.getClass().getMethod("LIZJ", null).invoke(invoke, null))) {
            throw new IllegalStateException("Conversation query failed");
        }
        Object field = listIn(NativeRead.field(invoke, "LIZ"));
        if (!(field instanceof List)) {
            throw new IllegalStateException("Missing conversation list");
        }
        List<Object> conversations = new ArrayList<>();
        for (Object next : new ArrayList<Object>((List<?>) field)) if (next != null) conversations.add(next);
        return conversations;
    }

    /** The conversation list in the query's answer: its one public List field, whatever TikTok calls it. */
    private static Object listIn(Object page) {
        if (page == null) return null;
        for (java.lang.reflect.Field field : page.getClass().getFields()) {
            if (List.class.isAssignableFrom(field.getType()) && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                try {
                    return field.get(page);
                } catch (IllegalAccessException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static long count(Object obj) throws Exception {
        return ((Number) obj.getClass().getMethod("getUnreadCount", null).invoke(obj, null)).longValue();
    }

    private static boolean stillUnread(Object obj, List<Object> list) throws Exception {
        Iterator<Object> it = list.iterator();
        while (it.hasNext()) {
            Object next = it.next();
            Object invoke = obj.getClass().getMethod("LIZ", String.class).invoke(obj, (String) next.getClass().getMethod("getConversationId", null).invoke(next, null));
            if (invoke != null) {
                next = invoke;
            }
            if (count(next) > 0) {
                return true;
            }
        }
        return false;
    }

    private static void checkAccount(String str) {
        if (!str.equals(Account.liveId())) {
            throw new IllegalStateException("Account changed");
        }
    }

    private static void onMain(final Task task) throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final Exception[] excArr = {null};
        final AtomicBoolean atomicBoolean = new AtomicBoolean();
        main.post(new Runnable() { // from class: cat.ttcuz.framework.InboxReadAll$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                InboxReadAll.lambda$onMain$5(atomicBoolean, task, excArr, countDownLatch);
            }
        });
        if (!countDownLatch.await(20L, TimeUnit.SECONDS)) {
            atomicBoolean.set(true);
            throw new IllegalStateException("UI timeout");
        }
        Exception exc = excArr[0];
        if (exc != null) {
            throw exc;
        }
    }

    static /* synthetic */ void lambda$onMain$5(AtomicBoolean atomicBoolean, Task task, Exception[] excArr, CountDownLatch countDownLatch) {
        try {
            try {
                if (!atomicBoolean.get()) {
                    task.run();
                }
            } catch (Exception e) {
                excArr[0] = e;
            }
        } finally {
            countDownLatch.countDown();
        }
    }

    private static Object await(Object obj) throws Exception {
        Class<?> cls = Class.forName("X.02tu");
        final Object[] objArr = {null};
        final Throwable[] thArr = {null};
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Object newProxyInstance = Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, new InvocationHandler() { // from class: cat.ttcuz.framework.InboxReadAll$$ExternalSyntheticLambda2
            @Override // java.lang.reflect.InvocationHandler
            public final Object invoke(Object obj2, Method method, Object[] objArr2) throws Throwable {
                return InboxReadAll.lambda$await$6(objArr, countDownLatch, obj2, method, objArr2);
            }
        });
        Object newProxyInstance2 = Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, new InvocationHandler() { // from class: cat.ttcuz.framework.InboxReadAll$$ExternalSyntheticLambda3
            @Override // java.lang.reflect.InvocationHandler
            public final Object invoke(Object obj2, Method method, Object[] objArr2) throws Throwable {
                return InboxReadAll.lambda$await$7(thArr, countDownLatch, obj2, method, objArr2);
            }
        });
        Class<?> cls2 = Class.forName("X.03AK");
        Object invoke = cls2.getMethod("LJJZZIII", cls, cls).invoke(cls2.getMethod("LJLIIL", Class.forName("X.03pc")).invoke(obj, Class.forName("X.03p2").getMethod("LIZ", null).invoke(null, null)), newProxyInstance, newProxyInstance2);
        try {
            if (!countDownLatch.await(25L, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Notice timeout");
            }
            if (thArr[0] != null) {
                throw new IllegalStateException("Notice request failed", thArr[0]);
            }
            return objArr[0];
        } finally {
            if (invoke != null) {
                NativeRead.get(invoke, "dispose");
            }
        }
    }

    static /* synthetic */ Object lambda$await$6(Object[] objArr, CountDownLatch countDownLatch, Object obj, Method method, Object[] objArr2) throws Throwable {
        if (method.getName().equals("accept")) {
            objArr[0] = objArr2[0];
            countDownLatch.countDown();
        }
        return objectMethod(obj, method, objArr2);
    }

    static /* synthetic */ Object lambda$await$7(Throwable[] thArr, CountDownLatch countDownLatch, Object obj, Method method, Object[] objArr) throws Throwable {
        if (method.getName().equals("accept")) {
            thArr[0] = (Throwable) objArr[0];
            countDownLatch.countDown();
        }
        return objectMethod(obj, method, objArr);
    }

    private static Object objectMethod(Object obj, Method method, Object[] objArr) {
        if (method.getName().equals("hashCode")) {
            return Integer.valueOf(System.identityHashCode(obj));
        }
        if (method.getName().equals("equals")) {
            return Boolean.valueOf(obj == objArr[0]);
        }
        if (method.getName().equals("toString")) {
            return "ttcuz inbox callback";
        }
        return null;
    }
}
