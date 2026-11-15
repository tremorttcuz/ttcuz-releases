package cat.ttcuz.framework;

/**
 * Checks that run for every nickname, comment and touch, done by hand: String.matches compiles a regular expression
 * on each call, and on Android that is an ICU object in native memory freed only by a finalizer -- by the thousand a
 * second it filled the native heap (an out-of-memory crash) and kept the garbage collector busy.
 */
final class Ids {
    private Ids() {}

    /** An account id: 1 to 24 digits. */
    static boolean uid(String s) { return digits(s, 1, 24); }

    /** Only the digits 0-9, between min and max of them. */
    static boolean digits(String s, int min, int max) {
        if (s == null || s.length() < min || s.length() > max) return false;
        for (int i = 0; i < s.length(); i++) { char c = s.charAt(i); if (c < '0' || c > '9') return false; }
        return true;
    }

    /** 64 lower-case hex characters (a SHA-256). */
    static boolean sha256(String s) {
        if (s == null || s.length() != 64) return false;
        for (int i = 0; i < 64; i++) { char c = s.charAt(i); if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false; }
        return true;
    }
}
