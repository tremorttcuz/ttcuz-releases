package cat.ttcuz.framework;

import cat.ttcuz.framework.PrivacyImGate;
import cat.ttcuz.framework.PrivacyReflect;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

final class PrivacyAdapters {
    private volatile Object activityDelegate;
    private volatile boolean closed;
    private final Flags flags;
    private final Notice notice;
    private final PrivacyImGate transport;
    private final List<PrivacyReflect.Patch> patches = Collections.synchronizedList(new ArrayList());
    private final Map<String, String> status = new ConcurrentHashMap();
    private final Map<String, AtomicInteger> blocked = new LinkedHashMap();
    private final Map<Object, Boolean> installed = Collections.synchronizedMap(new IdentityHashMap());

    interface Flags {
        boolean on(String str);
    }

    interface Notice {
        void log(String str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    interface Work {
        void run() throws Exception;
    }

    static /* synthetic */ Object lambda$api$10(Object obj, Method method) {
        return obj;
    }

    PrivacyAdapters(Flags flags, Notice notice) {
        this.flags = flags;
        this.notice = notice;
        this.transport = new PrivacyImGate(new PrivacyImGate.Mode() { // from class: cat.ttcuz.framework.PrivacyAdapters$$ExternalSyntheticLambda15
            @Override // cat.ttcuz.framework.PrivacyImGate.Mode
            public final boolean on(String str) {
                return PrivacyAdapters.this.enabled(str);
            }
        }, new PrivacyImGate.Hit() { // from class: cat.ttcuz.framework.PrivacyAdapters$$ExternalSyntheticLambda16
            @Override // cat.ttcuz.framework.PrivacyImGate.Hit
            public final void accept(String str) {
                PrivacyAdapters.this.hit(str);
            }
        }, notice);
        String[] strArr = {"online", "stories", "read", "typing", "profiles"};
        for (int i = 0; i < 5; i++) {
            this.blocked.put(strArr[i], new AtomicInteger());
        }
    }

    boolean enabled(String str) {
        if (!this.closed && this.flags.on("ghost")) {
            if (this.flags.on("ghost_" + str)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hit(String str) {
        this.blocked.get(str).incrementAndGet();
    }

    private void attempt(String str, Work work) {
        try {
            work.run();
            this.status.put(str, "точка отправки подключена");
        } catch (Exception e) {
            String str2 = e.getClass().getSimpleName() + ": " + e.getMessage();
            if (str2.equals(this.status.put(str, str2))) {
                return;
            }
            this.notice.log("Ghost " + str + ": " + str2);
        }
    }

    void bootstrap() {
        if (this.closed || !this.flags.on("ghost")) {
            return;
        }
        attempt("online", new Work() { // from class: cat.ttcuz.framework.PrivacyAdapters$$ExternalSyntheticLambda3
            @Override // cat.ttcuz.framework.PrivacyAdapters.Work
            public final void run() throws Exception {
                PrivacyAdapters.this.lambda$bootstrap$2();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$bootstrap$2() throws Exception {
        final Object stat = PrivacyReflect.stat("X.0DH0", "LIZIZ");
        final Field field = PrivacyImGate.lazyValue(stat.getClass());
        synchronized (stat) {
            Object obj = field.get(stat);
            if (obj != PrivacyReflect.stat("X.02pl", "LIZ")) {
                this.activityDelegate = obj;
                return;
            }
            Field field2 = PrivacyReflect.field(stat.getClass(), "LL");
            final Object obj2 = field2.get(stat);
            Class<?> type = PrivacyReflect.type("kotlin.jvm.functions.Function0");
            patch(stat, field2, Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, new InvocationHandler() { // from class: cat.ttcuz.framework.PrivacyAdapters$$ExternalSyntheticLambda7
                @Override // java.lang.reflect.InvocationHandler
                public final Object invoke(Object obj3, Method method, Object[] objArr) throws Throwable {
                    Object lambda$bootstrap$1;
                    lambda$bootstrap$1 = PrivacyAdapters.this.lambda$bootstrap$1(obj2, stat, field, obj3, method, objArr);
                    return lambda$bootstrap$1;
                }
            }));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ Object lambda$bootstrap$1(Object obj, Object obj2, Field field, Object obj3, Method method, Object[] objArr) throws Throwable {
        final Object invoke = PrivacyReflect.invoke(method, obj, objArr);
        if (!method.getName().equals("invoke") || this.closed) {
            return invoke;
        }
        this.activityDelegate = invoke;
        Class<?> type = PrivacyReflect.type("com.ss.android.ugc.aweme.im.activity.status.sdk.api.ActivityStatusService");
        Object newProxyInstance = Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, new InvocationHandler() { // from class: cat.ttcuz.framework.PrivacyAdapters$$ExternalSyntheticLambda4
            @Override // java.lang.reflect.InvocationHandler
            public final Object invoke(Object obj4, Method method2, Object[] objArr2) throws Throwable {
                Object lambda$bootstrap$0;
                lambda$bootstrap$0 = PrivacyAdapters.this.lambda$bootstrap$0(invoke, obj4, method2, objArr2);
                return lambda$bootstrap$0;
            }
        });
        synchronized (this.patches) {
            if (this.closed) {
                return invoke;
            }
            this.patches.add(new PrivacyReflect.Patch(obj2, field, newProxyInstance, invoke));
            return newProxyInstance;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ Object lambda$bootstrap$0(Object obj, Object obj2, Method method, Object[] objArr) throws Throwable {
        if (enabled("online") && method.getName().equals("LJI")) { // ActivityStatusService activate(...) in TikTok 47.7.3
            attempt("online", this::online);
        }
        Object invoke = PrivacyReflect.invoke(method, obj, objArr);
        if (enabled("online") && method.getName().equals("LIZIZ")) {
            attempt("online", this::online);
        }
        return invoke;
    }

    void apply() {
        if (this.closed || !this.flags.on("ghost")) {
            return;
        }
        if (enabled("read") || enabled("typing")) {
            try {
                this.transport.install();
            } catch (Exception e) {
                this.transport.status = "ошибка: " + e.getClass().getSimpleName() + ": " + e.getMessage();
                this.notice.log("IM outbound: " + String.valueOf(e));
            }
        }
        if (enabled("typing")) {
            attempt("typing", new Work() { // from class: cat.ttcuz.framework.PrivacyAdapters$$ExternalSyntheticLambda10
                @Override // cat.ttcuz.framework.PrivacyAdapters.Work
                public final void run() throws Exception {
                    PrivacyAdapters.this.lambda$apply$3();
                }
            });
        }
        if (enabled("profiles")) {
            attempt("profiles", new Work() { // from class: cat.ttcuz.framework.PrivacyAdapters$$ExternalSyntheticLambda11
                @Override // cat.ttcuz.framework.PrivacyAdapters.Work
                public final void run() throws Exception {
                    PrivacyAdapters.this.lambda$apply$4();
                }
            });
        }
        if (enabled("stories")) {
            attempt("stories", new Work() { // from class: cat.ttcuz.framework.PrivacyAdapters$$ExternalSyntheticLambda12
                @Override // cat.ttcuz.framework.PrivacyAdapters.Work
                public final void run() throws Exception {
                    PrivacyAdapters.this.lambda$apply$5();
                }
            });
        }
        if (enabled("online")) {
            attempt("online", this::online);
        }
        if (enabled("read")) {
            attempt("read", new Work() { // from class: cat.ttcuz.framework.PrivacyAdapters$$ExternalSyntheticLambda13
                @Override // cat.ttcuz.framework.PrivacyAdapters.Work
                public final void run() throws Exception {
                    PrivacyAdapters.this.read();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$apply$3() throws Exception {
        lazy(PrivacyReflect.stat("X.0js1", "LIZIZ"), "com.ss.android.ugc.aweme.im.chatroom.api.typingindicator.ITypingStatusSenderTimer", "typing", "LIZ", false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$apply$4() throws Exception {
        api("com.ss.android.ugc.profile.business.ci.viewer.api.ProfileViewerApiService", "com.ss.android.ugc.profile.business.ci.viewer.api.IProfileViewerApi", "profiles", "reportView");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$apply$5() throws Exception {
        api("com.ss.android.ugc.aweme.story.api.StoryApi", "com.ss.android.ugc.aweme.story.api.IStoryApi", "stories", "reportStoryViewed");
    }

    static Object proxy(Class<?> cls, final Object obj, final BiFunction<Method, Object[], Boolean> biFunction, final Function<Method, Object> function) {
        return Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, new InvocationHandler() { // from class: cat.ttcuz.framework.PrivacyAdapters$$ExternalSyntheticLambda14
            @Override // java.lang.reflect.InvocationHandler
            public final Object invoke(Object obj2, Method method, Object[] objArr) throws Throwable {
                return PrivacyAdapters.lambda$proxy$6(biFunction, function, obj, obj2, method, objArr);
            }
        });
    }

    static /* synthetic */ Object lambda$proxy$6(BiFunction biFunction, Function function, Object obj, Object obj2, Method method, Object[] objArr) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            String name = method.getName();
            name.hashCode();
            switch (name) {
                case "toString":
                    return "ttcuz privacy adapter";
                case "equals":
                    return Boolean.valueOf(obj2 == objArr[0]);
                case "hashCode":
                    return Integer.valueOf(System.identityHashCode(obj2));
            }
        }
        return ((Boolean) biFunction.apply(method, objArr)).booleanValue() ? function.apply(method) : PrivacyReflect.invoke(method, obj, objArr);
    }

    private void patch(Object obj, Field field, Object obj2) throws Exception {
        synchronized (this.patches) {
            if (this.closed) {
                throw new IllegalStateException("adapter stopped");
            }
            this.patches.add(new PrivacyReflect.Patch(obj, field, obj2));
            this.installed.put(obj, Boolean.TRUE);
        }
    }

    private void lazy(Object obj, String str, final String str2, final String str3, final boolean z) throws Exception {
        if (this.installed.containsKey(obj)) {
            return;
        }
        Object call = PrivacyReflect.call(obj, "getValue");
        Class<?> type = PrivacyReflect.type(str);
        if (call == null || !type.isInstance(call)) {
            throw new IllegalStateException("sender unavailable");
        }
        Object proxy = proxy(type, call, new BiFunction() { // from class: cat.ttcuz.framework.PrivacyAdapters$$ExternalSyntheticLambda0
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj2, Object obj3) {
                Boolean lambda$lazy$7;
                lambda$lazy$7 = PrivacyAdapters.this.lambda$lazy$7(str3, str2, (Method) obj2, (Object[]) obj3);
                return lambda$lazy$7;
            }
        }, new Function() { // from class: cat.ttcuz.framework.PrivacyAdapters$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj2) {
                return PrivacyAdapters.lambda$lazy$8(z, (Method) obj2);
            }
        });
        synchronized (obj) {
            patch(obj, PrivacyImGate.lazyValue(obj.getClass()), proxy);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ Boolean lambda$lazy$7(String str, String str2, Method method, Object[] objArr) {
        boolean z = method.getName().equals(str) && enabled(str2);
        if (z) {
            hit(str2);
        }
        return Boolean.valueOf(z);
    }

    static /* synthetic */ Object lambda$lazy$8(boolean z, Method method) {
        if (z) {
            return Boolean.TRUE;
        }
        return null;
    }

    private void api(String str, String str2, final String str3, final String str4) throws Exception {
        final Object invoke;
        Object stat = PrivacyReflect.stat(str, "LIZIZ");
        if (this.installed.containsKey(stat)) {
            return;
        }
        Field field = PrivacyReflect.field(stat.getClass(), "LIZ");
        Object obj = field.get(stat);
        if (str3.equals("profiles")) {
            invoke = PrivacyReflect.type("X.03AK").getMethod("LJJJJL", Iterable.class).invoke(null, Collections.emptyList());
        } else {
            invoke = PrivacyReflect.type("X.05nY").getMethod("LJIIZILJ", Throwable.class).invoke(null, new CancellationException("ttcuz: story view not sent"));
        }
        patch(stat, field, proxy(PrivacyReflect.type(str2), obj, new BiFunction() { // from class: cat.ttcuz.framework.PrivacyAdapters$$ExternalSyntheticLambda5
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj2, Object obj3) {
                Boolean lambda$api$9;
                lambda$api$9 = PrivacyAdapters.this.lambda$api$9(str4, str3, (Method) obj2, (Object[]) obj3);
                return lambda$api$9;
            }
        }, new Function() { // from class: cat.ttcuz.framework.PrivacyAdapters$$ExternalSyntheticLambda6
            @Override // java.util.function.Function
            public final Object apply(Object obj2) {
                return PrivacyAdapters.lambda$api$10(invoke, (Method) obj2);
            }
        }));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ Boolean lambda$api$9(String str, String str2, Method method, Object[] objArr) {
        boolean z = method.getName().equals(str) && enabled(str2);
        if (z) {
            hit(str2);
        }
        return Boolean.valueOf(z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void online() throws Exception {
        Object invoke;
        if (this.activityDelegate != null) {
            invoke = this.activityDelegate;
        } else {
            invoke = PrivacyReflect.type("X.0DH0").getMethod("LIZ", null).invoke(null, null);
        }
        Object call = PrivacyReflect.call(invoke, "LJIIJJI");
        if (call == null) {
            throw new IllegalStateException("ожидание входа в аккаунт");
        }
        Object obj = PrivacyReflect.get(PrivacyReflect.call(call, "LIZLLL"), "LIZLLL");
        if (this.installed.containsKey(obj)) {
            return;
        }
        Field field = PrivacyReflect.field(obj.getClass(), "LIZ");
        patch(obj, field, proxy(field.getType(), field.get(obj), new BiFunction() { // from class: cat.ttcuz.framework.PrivacyAdapters$$ExternalSyntheticLambda8
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj2, Object obj3) {
                Boolean lambda$online$11;
                lambda$online$11 = PrivacyAdapters.this.lambda$online$11((Method) obj2, (Object[]) obj3);
                return lambda$online$11;
            }
        }, new Function() { // from class: cat.ttcuz.framework.PrivacyAdapters$$ExternalSyntheticLambda9
            @Override // java.util.function.Function
            public final Object apply(Object obj2) {
                Object obj3;
                obj3 = Boolean.TRUE;
                return obj3;
            }
        }));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ Boolean lambda$online$11(Method method, Object[] objArr) {
        boolean z = method.getName().equals("LIZ") && enabled("online");
        if (z) {
            hit("online");
        }
        return Boolean.valueOf(z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void read() throws Exception {
        Object call = PrivacyReflect.call(PrivacyReflect.call(PrivacyReflect.get(PrivacyReflect.call(PrivacyReflect.type("com.bytedance.ies.im.core.api.proxy.IIMCoreProxyService").getMethod("LIZJ", PrivacyReflect.type("X.0CaB")).invoke(PrivacyReflect.type("X.0CaC").getMethod("LIZ", null).invoke(null, null), PrivacyReflect.stat("X.0CaB", "TIKTOK_SOCIAL_IM")), "LJIIJ"), "LIZ"), "LJIIZILJ"), "getDomain");
        PrivacyReflect.call(call, "LIZLLL");
        if (!call.getClass().getName().equals("X.0Cfu")) {
            throw new IllegalStateException("неподдерживаемый тип сессии: " + call.getClass().getName());
        }
        lazy(PrivacyReflect.get(call, "LJI"), "X.0Ctn", "read", "LJLLJ", false);
    }

    String report() {
        StringBuilder sb = new StringBuilder("призрак: ");
        sb.append(this.flags.on("ghost") ? "включён" : "выключен");
        sb.append("\nотправка IM: ");
        sb.append(this.transport.report());
        for (String str : this.blocked.keySet()) {
            sb.append("\n");
            sb.append(str);
            sb.append(": ");
            sb.append(enabled(str) ? this.status.getOrDefault(str, "ожидание подключения") : "выключено");
            sb.append("; остановлено: ");
            sb.append(this.blocked.get(str).get());
        }
        return sb.toString();
    }

    void close() {
        this.closed = true;
        this.transport.close();
        synchronized (this.patches) {
            for (int size = this.patches.size() - 1; size >= 0; size--) {
                try {
                    this.patches.get(size).restore();
                } catch (Exception e) {
                    this.notice.log("Ghost restore: " + String.valueOf(e));
                }
            }
            this.patches.clear();
            this.installed.clear();
        }
        this.activityDelegate = null;
    }
}
