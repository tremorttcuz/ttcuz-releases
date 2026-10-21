// Privacy adapters adapted from TT Extras 1.0.1 (MargyT), supplied by the user.
package cat.ttcuz.framework;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

/** Version-scoped adapters of real outbound entry points. No invented network API.
 * All unrelated operations delegate unchanged; disabling restores owned caches. */
final class PrivacyAdapters {

    interface Flags {
        boolean on(String key);
    }

    interface Notice {
        void log(String message);
    }

    private final PrivacyImGate transport;
    private final Flags flags;
    private final Notice notice;
    private final List<PrivacyReflect.Patch> patches = Collections.synchronizedList(
        new ArrayList<>()
    );
    private final Map<String, String> status = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> blocked = new LinkedHashMap<>();
    private final Map<Object, Boolean> installed = Collections.synchronizedMap(
        new IdentityHashMap<>()
    );
    private volatile boolean closed;
    private volatile Object activityDelegate;

    PrivacyAdapters(Flags f, Notice n) {
        flags = f;
        notice = n;
        transport = new PrivacyImGate(this::enabled, this::hit, n);
        for (String k : new String[] { "online", "stories", "read", "typing", "profiles" })
            blocked.put(k, new AtomicInteger());
    }

    boolean enabled(String channel) {
        return !closed && flags.on("ghost") && flags.on("ghost_" + channel);
    }

    private void hit(String key) {
        blocked.get(key).incrementAndGet();
    }

    private interface Work {
        void run() throws Exception;
    }

    private void attempt(String key, Work work) {
        try {
            work.run();
            status.put(key, "точка отправки подключена");
        } catch (Exception e) {
            String s = e.getClass().getSimpleName() + ": " + e.getMessage();
            if (!s.equals(status.put(key, s))) notice.log("Ghost " + key + ": " + s);
        }
    }

    void bootstrap() {
        // Intercept the lazy initializer without constructing TikTok services before Application.onCreate.
        if (closed || !flags.on("ghost")) return;
        attempt("online", () -> {
            Object cache = PrivacyReflect.stat("X.12Qu", "LIZIZ");
            Field value = PrivacyReflect.field(cache.getClass(), "LLJJIII");
            synchronized (cache) {
                Object current = value.get(cache);
                Object sentinel = PrivacyReflect.stat("X.0CPd", "LIZ");
                if (current != sentinel) {
                    activityDelegate = current;
                    return;
                }
                Field initializer = PrivacyReflect.field(cache.getClass(), "LL");
                Object original = initializer.get(cache);
                Class<?> fn = PrivacyReflect.type("kotlin.jvm.functions.Function0");
                Object wrapper = Proxy.newProxyInstance(
                    fn.getClassLoader(),
                    new Class<?>[] { fn },
                    (p, m, a) -> {
                        Object result = PrivacyReflect.invoke(m, original, a);
                        if (!m.getName().equals("invoke") || closed) return result;
                        activityDelegate = result;
                        Class<?> service = PrivacyReflect.type(
                            "com.ss.android.ugc.aweme.im.activity.status.sdk.api.ActivityStatusService"
                        );
                        Object adapter = Proxy.newProxyInstance(
                            service.getClassLoader(),
                            new Class<?>[] { service },
                            (p2, method, args) -> {
                                if (enabled("online") && method.getName().equals("LIZJ")) attempt(
                                    "online",
                                    this::online
                                );
                                Object answer = PrivacyReflect.invoke(method, result, args);
                                if (enabled("online") && method.getName().equals("LIZIZ")) attempt(
                                    "online",
                                    this::online
                                );
                                return answer;
                            }
                        );
                        // The native lazy writes this after initializer returns. Register its ownership for stop.
                        synchronized (patches) {
                            if (closed) return result;
                            patches.add(new PrivacyReflect.Patch(cache, value, adapter, result));
                        }
                        return adapter;
                    }
                );
                patch(cache, initializer, wrapper);
            }
        });
    }

    void apply() {
        if (closed || !flags.on("ghost")) return;
        if (enabled("read") || enabled("typing")) try {
            transport.install();
        } catch (Exception e) {
            transport.status = "ошибка: " + e.getClass().getSimpleName() + ": " + e.getMessage();
            notice.log("IM outbound: " + e);
        }
        if (enabled("typing")) attempt("typing", () ->
            lazy(
                PrivacyReflect.stat("X.0TL0", "LIZIZ"),
                "com.ss.android.ugc.aweme.im.chatroom.api.typingindicator.ITypingStatusSenderTimer",
                "typing",
                "LIZ",
                false
            )
        );
        if (enabled("profiles")) attempt("profiles", () ->
            api(
                "com.ss.android.ugc.profile.business.ci.viewer.api.ProfileViewerApiService",
                "com.ss.android.ugc.profile.business.ci.viewer.api.IProfileViewerApi",
                "profiles",
                "reportView"
            )
        );
        if (enabled("stories")) attempt("stories", () ->
            api(
                "com.ss.android.ugc.aweme.story.api.StoryApi",
                "com.ss.android.ugc.aweme.story.api.IStoryApi",
                "stories",
                "reportStoryViewed"
            )
        );
        if (enabled("online")) attempt("online", this::online);
        if (enabled("read")) attempt("read", this::read);
    }

    static Object proxy(
        Class<?> face,
        Object delegate,
        java.util.function.BiFunction<Method, Object[], Boolean> suppress,
        java.util.function.Function<Method, Object> substitute
    ) {
        return Proxy.newProxyInstance(face.getClassLoader(), new Class<?>[] { face }, (p, m, a) -> {
            if (m.getDeclaringClass() == Object.class) {
                switch (m.getName()) {
                    case "equals":
                        return p == a[0];
                    case "hashCode":
                        return System.identityHashCode(p);
                    case "toString":
                        return "ttcuz privacy adapter";
                }
            }
            if (suppress.apply(m, a)) return substitute.apply(m);
            return PrivacyReflect.invoke(m, delegate, a);
        });
    }

    private void patch(Object owner, Field field, Object value) throws Exception {
        synchronized (patches) {
            if (closed) throw new IllegalStateException("adapter stopped");
            patches.add(new PrivacyReflect.Patch(owner, field, value));
            installed.put(owner, Boolean.TRUE);
        }
    }

    private void lazy(Object lazy, String face, String channel, String method, boolean answer)
        throws Exception {
        if (installed.containsKey(lazy)) return;
        Object original = PrivacyReflect.call(lazy, "getValue");
        Class<?> type = PrivacyReflect.type(face);
        if (original == null || !type.isInstance(original)) throw new IllegalStateException(
            "sender unavailable"
        );
        Object value = proxy(
            type,
            original,
            (m, a) -> {
                boolean stop = m.getName().equals(method) && enabled(channel);
                if (stop) hit(channel);
                return stop;
            },
            m -> answer ? Boolean.TRUE : null
        );
        synchronized (lazy) {
            patch(lazy, PrivacyReflect.field(lazy.getClass(), "LLJJIII"), value);
        }
    }

    private void api(String implementation, String face, String channel, String method)
        throws Exception {
        Object service = PrivacyReflect.stat(implementation, "LIZIZ");
        if (installed.containsKey(service)) return;
        Field field = PrivacyReflect.field(service.getClass(), "LIZ");
        Object original = field.get(service);
        // Resolve the local cancellation publisher before installing. Never return null or a fabricated server success.
        final Object canceled;
        if (channel.equals("profiles")) canceled = PrivacyReflect.type("X.19ZK")
            .getMethod("LJJJJJ", Iterable.class)
            .invoke(null, Collections.emptyList());
        else canceled = PrivacyReflect.type("X.19X3")
            .getMethod("LJIILLIIL", Throwable.class)
            .invoke(null, new CancellationException("ttcuz: story view not sent"));
        Object value = proxy(
            PrivacyReflect.type(face),
            original,
            (m, a) -> {
                boolean stop = m.getName().equals(method) && enabled(channel);
                if (stop) hit(channel);
                return stop;
            },
            m -> canceled
        );
        patch(service, field, value);
    }

    private void online() throws Exception {
        Object service =
            activityDelegate != null
                ? activityDelegate
                : PrivacyReflect.type("X.12Qu").getMethod("LIZ").invoke(null);
        Object session = PrivacyReflect.call(service, "LJIIJJI");
        if (session == null) throw new IllegalStateException("ожидание входа в аккаунт");
        Object reporter = PrivacyReflect.call(session, "LJ"),
            gate = PrivacyReflect.get(reporter, "LIZLLL");
        if (installed.containsKey(gate)) return;
        Field field = PrivacyReflect.field(gate.getClass(), "LIZ");
        Object original = field.get(gate);
        Object value = proxy(
            PrivacyReflect.type("X.1KSn"),
            original,
            (m, a) -> {
                boolean stop = m.getName().equals("LIZ") && enabled("online");
                if (stop) hit("online");
                return stop;
            },
            m -> Boolean.TRUE
        );
        patch(gate, field, value);
    }

    private void read() throws Exception {
        Object service = PrivacyReflect.type("X.06WV").getMethod("LIZ").invoke(null);
        Object kind = PrivacyReflect.stat("X.0Rub", "TIKTOK_SOCIAL_IM");
        Object client = PrivacyReflect.type(
            "com.bytedance.ies.im.core.api.proxy.IIMCoreProxyService"
        )
            .getMethod("LIZJ", PrivacyReflect.type("X.0Rub"))
            .invoke(service, kind);
        Object sdk = PrivacyReflect.call(client, "LJIIJ");
        Object root = PrivacyReflect.get(sdk, "LIZ");
        Object nativeRoot = PrivacyReflect.call(root, "LJIIZILJ");
        Object domain = PrivacyReflect.call(nativeRoot, "getDomain");
        // LIZLLL is the native use-case API; initialize its lazy, then replace only its interface value.
        PrivacyReflect.call(domain, "LIZLLL");
        if (!domain.getClass().getName().equals("X.0RgK")) throw new IllegalStateException(
            "неподдерживаемый тип сессии: " + domain.getClass().getName()
        );
        lazy(PrivacyReflect.get(domain, "LJI"), "X.0RiV", "read", "LJLL", false);
    }

    String report() {
        StringBuilder s = new StringBuilder("призрак: ")
            .append(flags.on("ghost") ? "включён" : "выключен")
            .append("\nотправка IM: ")
            .append(transport.report());
        for (String k : blocked.keySet())
            s.append("\n")
                .append(k)
                .append(": ")
                .append(enabled(k) ? status.getOrDefault(k, "ожидание подключения") : "выключено")
                .append("; остановлено: ")
                .append(blocked.get(k).get());
        return s.toString();
    }

    void close() {
        closed = true;
        transport.close();
        synchronized (patches) {
            for (int i = patches.size() - 1; i >= 0; i--) try {
                patches.get(i).restore();
            } catch (Exception e) {
                notice.log("Ghost restore: " + e);
            }
            patches.clear();
            installed.clear();
        }
        activityDelegate = null;
    }
}
