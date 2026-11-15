package cat.ttcuz.framework;

import cat.ttcuz.framework.PrivacyAdapters;
import cat.ttcuz.framework.PrivacyReflect;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

final class PrivacyImGate {
    private Field body;
    private volatile String callbackIssue;
    private Class<?> callbackType;
    private volatile boolean closed;
    private Field cmd;
    private final Hit hit;
    private final PrivacyAdapters.Notice log;
    private final Mode mode;
    private PrivacyReflect.Patch patch;
    private Class<?> requestType;
    private Field seq;
    private final Map<Integer, String> commands = new HashMap();
    private final LinkedHashMap<String, String> denied = new LinkedHashMap<>();
    private final AtomicInteger[] observed = {new AtomicInteger(), new AtomicInteger(), new AtomicInteger(), new AtomicInteger()};
    String status = "не подключён";

    interface Cancel {
        void run(Method method, Object[] objArr, String str) throws Exception;
    }

    interface Decision {
        String channel(Method method, Object[] objArr) throws Exception;
    }

    interface Hit {
        void accept(String str);
    }

    interface Mode {
        boolean on(String str);
    }

    static /* synthetic */ void lambda$install$4(Method method, Object[] objArr, String str) throws Exception {
    }

    PrivacyImGate(Mode mode, Hit hit, PrivacyAdapters.Notice notice) {
        this.mode = mode;
        this.hit = hit;
        this.log = notice;
    }

    void install() throws Exception {
        if (this.patch != null) {
            this.status = "HTTP и WebSocket подключены";
            return;
        }
        Class<?> type = PrivacyReflect.type("X.0Ctq");
        String[] strArr = {"MARK_CONVERSATION_READ", "MARK_CONVERSATION_READ_V3", "BATCH_MARK_CONVERSATION_READ", "MARK_STRANGER_CONVERSATION_READ", "MARK_ALL_STRANGER_CONVERSATIONS_READ", "MARK_MULTI_STRANGER_CONVERSATIONS_READ"};
        for (int i = 0; i < 6; i++) {
            this.commands.put(Integer.valueOf(((Number) PrivacyReflect.call(PrivacyReflect.field(type, strArr[i]).get(null), "getValue")).intValue()), "read");
        }
        this.commands.put(Integer.valueOf(((Number) PrivacyReflect.call(PrivacyReflect.field(type, "SEND_INPUT_STATUS").get(null), "getValue")).intValue()), "typing");
        Class<?> type2 = PrivacyReflect.type("com.bytedance.im.core.proto.Request");
        this.requestType = type2;
        this.cmd = PrivacyReflect.field(type2, "cmd");
        this.seq = PrivacyReflect.field(this.requestType, "sequence_id");
        this.body = PrivacyReflect.field(PrivacyReflect.type("X.0CiJ"), "LIZLLL");
        this.callbackType = PrivacyReflect.type("X.0DBp");
        Object stat = PrivacyReflect.stat("X.0CbC", "LIZIZ");
        Object call = PrivacyReflect.call(stat, "getValue");
        Class<?> type3 = PrivacyReflect.type("com.bytedance.ies.im.core.api.proxy.IIMCoreDependProxyService");
        Object invoke = type3.getMethod("LIZIZ", null).invoke(call, null);
        Object invoke2 = type3.getMethod("LIZJ", null).invoke(call, null);
        final Object outbound = outbound(PrivacyReflect.type("X.03Fq"), invoke, new Decision() { // from class: cat.ttcuz.framework.PrivacyImGate$$ExternalSyntheticLambda1
            @Override // cat.ttcuz.framework.PrivacyImGate.Decision
            public final String channel(Method method, Object[] objArr) throws Exception {
                String lambda$install$0;
                lambda$install$0 = PrivacyImGate.this.lambda$install$0(method, objArr);
                return lambda$install$0;
            }
        }, new Cancel() { // from class: cat.ttcuz.framework.PrivacyImGate$$ExternalSyntheticLambda2
            @Override // cat.ttcuz.framework.PrivacyImGate.Cancel
            public final void run(Method method, Object[] objArr, String str) throws Exception {
                PrivacyImGate.this.lambda$install$1(method, objArr, str);
            }
        }, new Hit() { // from class: cat.ttcuz.framework.PrivacyImGate$$ExternalSyntheticLambda3
            @Override // cat.ttcuz.framework.PrivacyImGate.Hit
            public final void accept(String str) {
                PrivacyImGate.this.lambda$install$2(str);
            }
        });
        final Object outbound2 = outbound(PrivacyReflect.type("com.bytedance.ies.im.core.api.depend.IWsDepend"), invoke2, new Decision() { // from class: cat.ttcuz.framework.PrivacyImGate$$ExternalSyntheticLambda4
            @Override // cat.ttcuz.framework.PrivacyImGate.Decision
            public final String channel(Method method, Object[] objArr) throws Exception {
                String lambda$install$3;
                lambda$install$3 = PrivacyImGate.this.lambda$install$3(method, objArr);
                return lambda$install$3;
            }
        }, new Cancel() { // from class: cat.ttcuz.framework.PrivacyImGate$$ExternalSyntheticLambda5
            @Override // cat.ttcuz.framework.PrivacyImGate.Cancel
            public final void run(Method method, Object[] objArr, String str) throws Exception {
                PrivacyImGate.lambda$install$4(method, objArr, str);
            }
        }, new Hit() { // from class: cat.ttcuz.framework.PrivacyImGate$$ExternalSyntheticLambda6
            @Override // cat.ttcuz.framework.PrivacyImGate.Hit
            public final void accept(String str) {
                PrivacyImGate.this.lambda$install$5(str);
            }
        });
        Object proxy = PrivacyAdapters.proxy(type3, call, (java.util.function.BiFunction<Method, Object[], Boolean>) (method, args) ->
                method.getName().equals("LIZIZ") || method.getName().equals("LIZJ"), new Function() { // from class: cat.ttcuz.framework.PrivacyImGate$$ExternalSyntheticLambda8
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return PrivacyImGate.lambda$install$7(outbound, outbound2, (Method) obj);
            }
        });
        synchronized (stat) {
            this.patch = new PrivacyReflect.Patch(stat, lazyValue(stat.getClass()), proxy);
        }
        this.status = "HTTP и WebSocket подключены";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ String lambda$install$0(Method method, Object[] objArr) throws Exception {
        Number number;
        if ((!method.getName().equals("LJ") && !method.getName().equals("LIZLLL")) || objArr == null || objArr.length != 2) {
            return null;
        }
        Object obj = this.body.get(objArr[0]);
        if (!this.requestType.isInstance(obj) || (number = (Number) this.cmd.get(obj)) == null || !this.commands.containsKey(Integer.valueOf(number.intValue()))) {
            return null;
        }
        Number number2 = (Number) this.seq.get(obj);
        return blocked(number.intValue(), number2 == null ? 0L : number2.longValue());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$install$1(Method method, Object[] objArr, String str) throws Exception {
        try {
            Object obj = PrivacyReflect.get(objArr[1], "l0");
            if (!this.callbackType.isInstance(obj)) {
                throw new IllegalStateException("unexpected HTTP callback");
            }
            this.callbackType.getMethod("LIZ", Exception.class, String.class, Integer.TYPE).invoke(obj, new CancellationException("ttcuz: " + str + " not sent"), "cancelled locally by ttcuz", -1000);
        } catch (Exception e) {
            this.callbackIssue = "пакет удержан; отмена callback: " + e.getClass().getSimpleName();
            this.log.log(this.callbackIssue);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$install$2(String str) {
        this.observed[!str.equals("read") ? 1 : 0].incrementAndGet();
        this.hit.accept(str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ String lambda$install$3(Method method, Object[] objArr) throws Exception {
        // IWsDepend's send(WsChannelMsg): LIZ in TikTok 47.7.3
        if (!method.getName().equals("LIZ") || objArr == null || objArr.length != 1) {
            return null;
        }
        Object obj = objArr[0];
        int intValue = ((Number) PrivacyReflect.get(obj, "service")).intValue();
        int intValue2 = ((Number) PrivacyReflect.get(obj, "method")).intValue();
        if (intValue == 5 && intValue2 == 1) {
            return blocked(commandHeader(PrivacyReflect.get(obj, "msgHeaders")), ((Number) PrivacyReflect.get(obj, "seqId")).longValue());
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$install$5(String str) {
        this.observed[str.equals("read") ? (char) 2 : (char) 3].incrementAndGet();
        this.hit.accept(str);
    }

    static /* synthetic */ Object lambda$install$7(Object obj, Object obj2, Method method) {
        return method.getName().equals("LIZIZ") ? obj : obj2;
    }

    /** Kotlin's synchronized lazy keeps what it made in its one volatile Object field; the field's name moves between releases. */
    static java.lang.reflect.Field lazyValue(Class<?> type) throws NoSuchFieldException {
        for (java.lang.reflect.Field field : type.getDeclaredFields())
            if (field.getType() == Object.class && java.lang.reflect.Modifier.isVolatile(field.getModifiers())) { field.setAccessible(true); return field; }
        throw new NoSuchFieldException(type.getName() + " has no lazy value");
    }

    Object outbound(Class<?> cls, final Object obj, final Decision decision, final Cancel cancel, final Hit hit) {
        return Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, new InvocationHandler() { // from class: cat.ttcuz.framework.PrivacyImGate$$ExternalSyntheticLambda0
            @Override // java.lang.reflect.InvocationHandler
            public final Object invoke(Object obj2, Method method, Object[] objArr) throws Throwable {
                Object lambda$outbound$8;
                lambda$outbound$8 = PrivacyImGate.this.lambda$outbound$8(decision, cancel, hit, obj, obj2, method, objArr);
                return lambda$outbound$8;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ Object lambda$outbound$8(Decision decision, Cancel cancel, Hit hit, Object obj, Object obj2, Method method, Object[] objArr) throws Throwable {
        String channel = this.closed ? null : decision.channel(method, objArr);
        if (channel != null) {
            cancel.run(method, objArr, channel);
            hit.accept(channel);
            return null;
        }
        return PrivacyReflect.invoke(method, obj, objArr);
    }

    String report() {
        String str;
        String str2 = this.status;
        int i = this.observed[0].get();
        int i2 = this.observed[1].get();
        int i3 = this.observed[2].get();
        int i4 = this.observed[3].get();
        if (this.callbackIssue == null) {
            str = "";
        } else {
            str = "; " + this.callbackIssue;
        }
        return str2 + "; HTTP прочитано/печатает: " + i + "/" + i2 + "; WS: " + i3 + "/" + i4 + str;
    }

    private int commandHeader(Object obj) throws Exception {
        if (!(obj instanceof List)) {
            return -1;
        }
        Iterator it = ((List) obj).iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Object next = it.next();
            if ("cmd".equals(PrivacyReflect.get(next, "key"))) {
                try {
                    return Integer.parseInt(String.valueOf(PrivacyReflect.get(next, "value")));
                } catch (NumberFormatException unused) {
                    return -1;
                }
            }
        }
        return -1;
    }

    synchronized String blocked(int i, long j) {
        String str = Account.liveId() + ":" + i + ":" + j;
        String str2 = j > 0 ? this.denied.get(str) : null;
        if (str2 != null) {
            return str2;
        }
        String str3 = this.commands.get(Integer.valueOf(i));
        if (str3 != null && this.mode.on(str3)) {
            if (j > 0) {
                this.denied.put(str, str3);
            }
            if (this.denied.size() > 8192) {
                LinkedHashMap<String, String> linkedHashMap = this.denied;
                linkedHashMap.remove(linkedHashMap.keySet().iterator().next());
            }
            return str3;
        }
        return null;
    }

    void defineForTest(int i, String str) {
        this.commands.put(Integer.valueOf(i), str);
    }

    void close() {
        this.closed = true;
        PrivacyReflect.Patch patch = this.patch;
        if (patch != null) {
            try {
                patch.restore();
            } catch (Exception e) {
                this.log.log("IM bridge restore: " + String.valueOf(e));
            }
        }
        this.patch = null;
        synchronized (this) {
            this.denied.clear();
        }
    }
}
