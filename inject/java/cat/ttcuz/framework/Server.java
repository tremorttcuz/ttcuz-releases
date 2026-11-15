package cat.ttcuz.framework;

/** The mod's own server, by one name. */
final class Server {
    static final String HOST = "ttcuz.ttcuz1337.workers.dev";
    static final String ROOT = "https://" + HOST;

    private Server() {}

    static boolean ours(String host) {
        return host != null && HOST.equalsIgnoreCase(host);
    }

    /** The address as it is: there is one host. Kept so Net reads the same way it did. */
    static String live(String url) { return url; }

    /** There is no second host to try. */
    static String other(String url) { return null; }

    static void answered(String url) {}
}
