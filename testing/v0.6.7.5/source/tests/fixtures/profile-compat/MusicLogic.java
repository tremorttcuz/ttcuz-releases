package cat.narezany.margyt;

/**
 * Geometry, spring and queue rules of the music island, with no Android classes in it,
 * so the numbers can be run and checked on a plain JVM.
 */
final class MusicLogic {
    private MusicLogic() {}

    static final int REPEAT_OFF = 0, REPEAT_ALL = 1, REPEAT_ONE = 2;
    static final int PLACE_OFF = 0, PLACE_FREE = 1, PLACE_DOCK = 2;

    static boolean canMovePlayer(String viewedUid,String signedInUid){return viewedUid!=null&&viewedUid.matches("[0-9]{1,24}")&&viewedUid.equals(signedInUid);}
    static int profileCoordinate(int from,int to,int fraction){return from+Math.round(Math.max(0,to-from)*clamp(fraction,0,1000)/1000f);}

    static float glowLevel(float[] bars, int from, int to, int sensitivity) {
        if (bars == null || bars.length == 0) return 0f;
        int low=clamp(from,0,bars.length),high=clamp(to,low,bars.length);float sum=0;
        for(int i=low;i<high;i++)sum+=clamp(bars[i],0f,1f);
        return clamp(sum/Math.max(1,high-low)*clamp(sensitivity,25,250)/100f,0f,1f);
    }
    static float glowStep(float current,float target,float dt) {
        return current+(target-current)*(1f-(float)Math.exp(-clamp(dt,0f,.05f)*6f));
    }
    static float glowAdvance(float dt,int speed,float energy,boolean playing,boolean live,boolean reactive,boolean motion) {
        if(!playing||!motion||(reactive&&(!live||energy<=.015f)))return 0f;
        return clamp(dt,0f,.05f)*clamp(speed,0,200)/100f*(.18f+(reactive?clamp(energy,0f,1f)*.65f:.18f));
    }

    static int clamp(int value, int low, int high) {
        if (high < low) return low;
        return Math.max(low, Math.min(high, value));
    }

    static float clamp(float value, float low, float high) {
        return Math.max(low, Math.min(high, value));
    }

    /** Smoothstep: eases both ends, used to stagger the content in without a visible start or stop. */
    static float smooth(float x) {
        float u = clamp(x, 0f, 1f);
        return u * u * (3f - 2f * u);
    }

    static float mix(float from, float to, float t) {
        return from + (to - from) * t;
    }

    /**
     * Under-damped spring step from 0 to 1 over t in [0,1]: it passes 1 by about 1.5% and settles: a light bounce.
     * This is the "bounce" of the island opening; it is exactly 0 at the start and 1 at the end.
     */
    static float spring(float t) {
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        final double zeta = 0.8, omega = 14.0;
        double damped = omega * Math.sqrt(1 - zeta * zeta);
        double decay = Math.exp(-zeta * omega * t);
        double value = 1 - decay * (Math.cos(damped * t) + zeta * omega / damped * Math.sin(damped * t));
        // The remaining 0.03% is folded in so the animation lands on 1 exactly.
        return (float) (value + (1 - value) * t * t * t * t * t * t * 0.0003);
    }

    /** Closing has no bounce: a short ease-out, so the island tucks in. */
    static float tuck(float t) {
        float u = clamp(t, 0f, 1f);
        return 1f - (1f - u) * (1f - u) * (1f - u);
    }

    /** True when the island sits in the left half, so it opens towards the right. */
    static boolean opensRight(float centreX, float screenWidth) {
        return centreX < screenWidth / 2f;
    }

    /** True when the island sits in the upper half, so it opens downwards. */
    static boolean opensDown(float centreY, float screenHeight) {
        return centreY < screenHeight / 2f;
    }

    /**
     * Top-left of the expanded window that keeps the pill's anchored corner where it was.
     * The window has {@code pad} of room around the card for the spring's overshoot.
     */
    static int[] expandedOrigin(int px, int py, int pw, int ph, int ew, int eh,
                                int screenW, int screenH, int pad) {
        boolean right = opensRight(px + pw / 2f, screenW);
        boolean down = opensDown(py + ph / 2f, screenH);
        int x = right ? px - pad : px + pw + pad - ew;
        int y = down ? py - pad : py + ph + pad - eh;
        return new int[] {clamp(x, 0, screenW - ew), clamp(y, 0, screenH - eh)};
    }

    /** The nearest side edge once a drag is let go. */
    static int snapX(int x, int width, int screenW, int margin) {
        return x + width / 2 < screenW / 2 ? margin : screenW - width - margin;
    }

    /** Left, centre or right slot of the bottom panel. */
    static int slotX(int slot, int width, int screenW, int margin) {
        if (slot <= 0) return margin;
        if (slot >= 2) return screenW - width - margin;
        return (screenW - width) / 2;
    }

    static int nearestSlot(int x, int width, int screenW) {
        float centre = x + width / 2f;
        if (centre < screenW / 3f) return 0;
        if (centre > screenW * 2f / 3f) return 2;
        return 1;
    }

    static float fraction(long position, long duration) {
        if (duration <= 0) return 0f;
        return clamp(position / (float) duration, 0f, 1f);
    }

    static String clock(long millis) {
        long total = Math.max(0, millis) / 1000;
        long minutes = total / 60, seconds = total % 60;
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }

    /** Height of the progress wave at x: flat when amplitude is 0 (paused). */
    static float wave(float x, float density, float phase, float amplitude) {
        return (float) Math.sin((x / (36f * density) - phase) * Math.PI * 2) * 3f * density * amplitude;
    }

    /**
     * The track that follows, or -1 when the queue ends.
     * {@code roll} is a number in [0,1) used for shuffle, so tests can pass a fixed one.
     */
    static int next(int size, int index, boolean shuffle, int repeat, boolean byUser, float roll) {
        if (size <= 0) return -1;
        if (repeat == REPEAT_ONE && !byUser) return clamp(index, 0, size - 1);
        if (shuffle && size > 1) {
            int pick = clamp((int) (roll * (size - 1)), 0, size - 2);
            return pick >= index ? pick + 1 : pick;
        }
        int following = index + 1;
        if (following < size) return following;
        return repeat == REPEAT_OFF && !byUser ? -1 : 0;
    }

    static int previous(int size, int index) {
        if (size <= 0) return -1;
        return index <= 0 ? size - 1 : index - 1;
    }

    /** Titles come from other people's sounds: no control characters, bounded length. */
    static String clean(String text, int max) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder(Math.min(text.length(), max));
        for (int i = 0; i < text.length() && out.length() < max; i++) {
            char c = text.charAt(i);
            out.append(c < 32 || c == 127 ? ' ' : c);
        }
        return out.toString().trim();
    }

    /** Safe file name for a cached sound. */
    static String fileName(String id) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < id.length() && out.length() < 48; i++) {
            char c = id.charAt(i);
            out.append(Character.isLetterOrDigit(c) && c < 128 ? c : '_');
        }
        return out.length() == 0 ? "sound" : out.toString();
    }

    /**
     * A real damped spring, stepped with the frame time, for the island's motion. Unlike a fixed
     * curve it keeps its velocity when interrupted, so opening then closing mid-way stays smooth.
     */
    static final class Spring {
        private final float stiffness, damping;
        float x, v, target;

        Spring(float stiffness, float dampingRatio) {
            this.stiffness = stiffness;
            this.damping = (float) (2 * dampingRatio * Math.sqrt(stiffness));
        }

        void jump(float value) { x = value; target = value; v = 0f; }

        void step(float dt) {
            int n = Math.max(1, (int) Math.ceil(dt / 0.004f));
            float h = dt / n;
            for (int i = 0; i < n; i++) {
                v += (-stiffness * (x - target) - damping * v) * h;
                x += v * h;
            }
        }

        boolean atRest() { return Math.abs(x - target) < 0.0004f && Math.abs(v) < 0.005f; }
    }

    /**
     * Fold an Android Visualizer FFT capture (signed bytes: DC, Nyquist, then re/im pairs) into
     * log-spaced bars. Each bar rises at once and falls slowly. {@code out} holds the last frame.
     */
    static void bands(byte[] fft, float[] out) {
        int bins = fft == null ? 0 : fft.length / 2 - 1;
        if (bins < 4) return;
        int limit = Math.max(3, (int) (bins * 0.7f));
        int n = out.length;
        for (int b = 0; b < n; b++) {
            int lo = Math.max(1, (int) Math.round(Math.pow(limit, (double) b / n)));
            int hi = Math.max(lo + 1, (int) Math.round(Math.pow(limit, (double) (b + 1) / n)));
            double peak = 0;
            for (int k = lo; k < hi && k <= bins; k++) {
                peak = Math.max(peak, Math.hypot(fft[2 * k], fft[2 * k + 1]));
            }
            float level = clamp((float) (Math.log1p(peak) / Math.log1p(128.0)), 0f, 1f);
            out[b] = Math.max(level * level, out[b] * 0.86f);
        }
    }

    /** Let every bar fall when nothing is being captured. */
    static void settle(float[] out) {
        for (int i = 0; i < out.length; i++) out[i] *= 0.86f;
    }

    static final String LIST_HEADER = "ttcuz-music 2";
    static final int LIST_BYTES = 1048576;

    /**
     * A link the phone may fetch a sound from: https, a real host name (not an address, not
     * localhost), no user name in it, and of sane length. Anything else is treated as no link.
     */
    static boolean safeLink(String link) {
        if (link == null || link.length() == 0 || link.length() > 2048) return false;
        for (int i = 0; i < link.length(); i++) if (link.charAt(i) <= 32 || link.charAt(i) == 127) return false;
        if (!link.startsWith("https://")) return false;
        try {
            java.net.URI parsed = new java.net.URI(link);
            String host = parsed.getHost();
            return "https".equals(parsed.getScheme()) && parsed.getRawUserInfo() == null
                    && host != null && host.matches("[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+")
                    && !host.matches("[0-9.]+") && !host.equalsIgnoreCase("localhost")
                    && parsed.getPort() >= -1 && parsed.getPort() <= 65535;
        } catch (java.net.URISyntaxException error) { return false; }

    }

    /**
     * The music list as the plain text kept on the server: a header, then id, title, artist and link
     * per line. The link says where to fetch the sound again; the sound itself is never in it.
     */
    static String encodeList(java.util.List<String[]> tracks, int limit) {
        boolean covers=false;for(String[] row:tracks)if(row!=null&&row.length>4&&safeLink(row[4])){covers=true;break;}
        StringBuilder out = new StringBuilder(covers?"ttcuz-music 3":LIST_HEADER).append('\n');
        int count = 0;
        for (String[] t : tracks) {
            if (count >= limit) break;
            if (t == null || t.length < 3) continue;
            String id = t[0] == null ? "" : t[0];
            if (!id.matches("[A-Za-z0-9_.-]{1,64}")) continue;
            String link = t.length > 3 && safeLink(t[3]) ? t[3] : "";
            String line = id + '\t' + clean(t[1], 120).replace('\t', ' ') + '\t'
                    + clean(t[2], 120).replace('\t', ' ') + '\t' + link + (covers?"\t"+(t.length>4&&safeLink(t[4])?t[4]:""): "") + '\n';
            if (out.length() + line.length() > LIST_BYTES) break;
            out.append(line);
            count++;
        }
        return out.toString();
    }

    /** Read that text back (version 1 had no link). Anything that does not fit gives an empty list. */
    static java.util.List<String[]> decodeList(String text) {
        java.util.List<String[]> out = new java.util.ArrayList<String[]>();
        if (text == null || text.length() > LIST_BYTES) return out;
        String[] lines = text.split("\n");
        if (lines.length == 0) return out;
        int fields = "ttcuz-music 3".equals(lines[0]) ? 5 : "ttcuz-music 2".equals(lines[0]) ? 4 : "ttcuz-music 1".equals(lines[0]) ? 3 : 0;
        if (fields == 0) return out;
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (int i = 1; i < lines.length && out.size() < 200; i++) {
            String[] parts = lines[i].split("\t", -1);
            if (parts.length != fields || !parts[0].matches("[A-Za-z0-9_.-]{1,64}") || !seen.add(parts[0])) continue;
            String link = fields >= 4 && safeLink(parts[3]) ? parts[3] : "";
            out.add(new String[] {parts[0], clean(parts[1], 120), clean(parts[2], 120), link,fields==5&&safeLink(parts[4])?parts[4]:""});
        }
        return out;
    }
}
