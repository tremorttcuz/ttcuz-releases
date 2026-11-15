package cat.ttcuz.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The text side of AdWords, free of Android: words in a text, and advertising stretches in WebVTT subtitles.
 *
 * Words come in two kinds. A strong one names the thing itself (a casino or a drop site, "казино", the person's own
 * words): one is enough. A weak one is common speech that advertising also uses ("промокод", "депозит", "бонус"):
 * it counts only next to another hit, so "use my promo code" in an ordinary video marks nothing. A word matches at
 * the start of a word ("промокоду" has "промокод"), never inside one; brand names in latin letters also match with
 * the spaces taken out ("GG_DROP", "gg drop").
 */
final class AdText {
    private AdText() {}

    /**
     * Lower case, ё as е, anything but letters, digits and ".+-" as one space. A plain loop: this runs for every post
     * of the feed, and regular expressions here (compiled again on every call) were most of the feed's time.
     */
    static String normal(String text) {
        if (text == null || text.isEmpty()) return "";
        String lower = text.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());
        boolean space = false;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (c == 'ё') c = 'е';
            if (Character.isLetterOrDigit(c) || c == '.' || c == '+' || c == '-') { out.append(c); space = false; }
            else if (!space) { out.append(' '); space = true; }
        }
        return out.toString();
    }

    /** A word with everything about it worked out once (it used to be worked out, with regexes, for every post). */
    static final class Word {
        final String word, whole, start, key;
        final boolean latinShort, shortName, glueable;
        Word(String w) {
            word = w;
            whole = " " + w + " ";
            start = " " + w;
            key = w.replace(" ", "");
            boolean latin = !w.isEmpty(), lettersOnly = true, apart = false;
            for (int i = 0; i < w.length(); i++) {
                char c = w.charAt(i);
                boolean az = c >= 'a' && c <= 'z', digit = c >= '0' && c <= '9', mark = c == ' ' || c == '.' || c == '+' || c == '-';
                if (!(az || digit || mark)) latin = false;
                if (!az) lettersOnly = false;
                if (digit || mark) apart = true;
            }
            boolean plain = !w.isEmpty();
            for (int i = 0; i < w.length(); i++) { char c = w.charAt(i); if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))) plain = false; }
            latinShort = w.length() <= 5 && plain;
            int space = w.lastIndexOf(' ');
            shortName = w.length() <= 5 || w.length() - space - 1 <= 3;
            glueable = w.length() >= 4 && latin && (apart || (lettersOnly && w.length() >= 6));
        }
    }

    private static final java.util.Map<String[], Word[]> compiled = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** The words of a list, prepared once per list (the lists are kept and replaced whole, never changed in place). */
    static Word[] prepared(String[] words) {
        Word[] known = compiled.get(words);
        if (known != null) return known;
        known = new Word[words.length];
        for (int i = 0; i < words.length; i++) known[i] = new Word(words[i]);
        compiled.put(words, known);
        return known;
    }

    /** {strong hits, weak hits} of the words in a text. */
    static int[] hits(String text, String[] strong, String[] weak) {
        String t = " " + normal(text) + " ", glued = t.replace(" ", "");
        int s = 0, w = 0;
        for (Word word : prepared(strong)) if (has(t, glued, word, true)) s++;
        for (Word word : prepared(weak)) if (has(t, glued, word, false)) w++;
        return new int[]{s, w};
    }

    private static boolean has(String t, String glued, Word w, boolean brand) {
        if (w.word.isEmpty()) return false;
        // short latin words only whole ("bet" is not "better", "code" is not "codec")
        if (w.latinShort) return t.contains(w.whole) || (brand && w.glueable && glued.contains(w.key));
        // a short name, or one ending in a short word ("пинко", "1вин", "мост бет"), only whole: "пинкод", "один винт"
        // and "мост бетонный" are not ads
        if (brand && w.shortName) return t.contains(w.whole) || (w.glueable && glued.contains(w.key));
        if (t.contains(w.start)) return true;
        // a brand in latin letters or digits, written apart or joined ("gg drop", "gg_drop", "ggdrop")
        // (names only: a common word glued into its neighbours would be found everywhere)
        return brand && w.glueable && glued.contains(w.key);
    }

    /** Whether a text is advertising: a strong word, or two different weak ones. */
    static boolean advertising(String text, String[] strong, String[] weak) {
        int[] h = hits(text, strong, weak);
        return h[0] > 0 || h[1] >= 2;
    }

    /** The first strong word in a text, or null (for the person's own checks). */
    static String match(String text, String[] words) {
        String t = " " + normal(text) + " ", glued = t.replace(" ", "");
        for (Word w : prepared(words)) if (has(t, glued, w, true)) return w.word;
        return null;
    }

    /**
     * WebVTT → advertising stretches in milliseconds, [start, end] pairs.
     *
     * The lines that hit are grouped while they follow one another within 12 s (one read-out says the name, the
     * code and the bonus over several lines); a group counts if it has a strong word or two weak hits. The stretch
     * runs from the start of its first line to the end of its last, with a fraction of a second either side for
     * the subtitles being a little early or late -- not more, so the video around it is left alone.
     */
    static long[] stretches(String vtt, String[] strong, String[] weak) {
        List<long[]> times = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        cues(vtt, times, texts);
        return stretches(times, texts, strong, weak);
    }

    /** The lines of WebVTT: {start, end} in milliseconds and the words, markup taken out. */
    static void cues(String vtt, List<long[]> times, List<String> texts) {
        String[] lines = vtt.replace("\r", "").split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int arrow = line.indexOf("-->");
            if (arrow < 0) continue;
            long from = time(line.substring(0, arrow)), to = time(line.substring(arrow + 3));
            StringBuilder text = new StringBuilder();
            for (int j = i + 1; j < lines.length && !lines[j].trim().isEmpty() && !lines[j].contains("-->"); j++) text.append(lines[j]).append(' ');
            if (from < 0 || to <= from) continue;
            times.add(new long[]{from, to});
            texts.add(text.toString().replaceAll("<[^>]*>", " "));
        }
    }

    /**
     * What one line of subtitles says: how many strong words, and which weak ones. Weak words are kept by which
     * they are, so the same word said twice ("кейс ... кейс") is still one -- two different ones make an ad, one
     * repeated never does. Names are also found misspelt, as speech recognition writes a voice sped up, slowed down
     * or pitched (see {@link #near}).
     */
    static final class Hit {
        int strong;
        final java.util.BitSet weak = new java.util.BitSet();
        boolean any() { return strong > 0 || !weak.isEmpty(); }
    }

    static Hit scan(String text, String[] strong, String[] weak) {
        Hit h = new Hit();
        String t = " " + normal(text) + " ", glued = t.replace(" ", "");
        Word[] words = prepared(strong);
        for (Word word : words) if (has(t, glued, word, true)) h.strong++;
        if (h.strong == 0 && near(t, strong)) h.strong = 1;
        Word[] soft = prepared(weak);
        for (int i = 0; i < soft.length; i++) if (has(t, glued, soft[i], false)) h.weak.set(i);
        return h;
    }

    // ------------------------------------------------------------------ names misheard

    /**
     * A strong name said in a changed voice. Speech recognition hears a voice sped up, slowed down or pitched as
     * other letters: "казино" comes out "казина", "мостбет" "моздбет", "1win" "ван вин". Each name and each run of one
     * to three words of the line are folded the same way -- Cyrillic and Latin letters that sound alike made one,
     * doubled letters made one, spaces taken out -- and a run within one change of a name of six to eight letters,
     * or two of a longer one, counts as that name. Shorter names only ever match exactly: too many words lie one
     * letter away from them. Subtitles only; the feed's descriptions are matched exactly.
     */
    static boolean near(String line, String[] strong) {
        String[] tokens = line.trim().split(" ");
        if (tokens.length == 0 || tokens[0].isEmpty()) return false;
        String[] keys = folded(strong);
        String[] runs = new String[tokens.length * 3];
        for (int i = 0; i < tokens.length; i++) {
            StringBuilder run = new StringBuilder();
            for (int j = 0; j < 3 && i + j < tokens.length; j++) { run.append(tokens[i + j]); runs[i * 3 + j] = fold(run.toString()); }
        }
        for (String key : keys) {
            if (key == null) continue;
            int limit = key.length() >= 9 ? 2 : 1;
            for (String run : runs) {
                if (run == null || Math.abs(run.length() - key.length()) > limit || run.charAt(0) != key.charAt(0) && limit == 1) continue;
                if (distance(run, key, limit) <= limit) return true;
            }
        }
        return false;
    }

    private static final java.util.Map<String[], String[]> folds = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private static String[] folded(String[] words) {
        String[] known = folds.get(words);
        if (known != null) return known;
        known = new String[words.length];
        for (int i = 0; i < words.length; i++) { String f = fold(normal(words[i])); known[i] = f.length() >= 6 ? f : null; }
        folds.put(words, known);
        return known;
    }

    /** Letters folded by sound, Cyrillic and Latin alike, doubled letters made one, everything else taken out. */
    static String fold(String text) {
        StringBuilder out = new StringBuilder(text.length() + 4);
        for (int i = 0; i < text.length(); i++) {
            String sound = sound(text.charAt(i));
            for (int k = 0; k < sound.length(); k++) {
                char c = sound.charAt(k);
                if (out.length() == 0 || out.charAt(out.length() - 1) != c) out.append(c);
            }
        }
        return out.toString();
    }

    private static String sound(char c) {
        switch (c) {
            case 'а': return "a"; case 'б': return "b"; case 'в': case 'w': return "v"; case 'г': return "g"; case 'д': return "d";
            case 'е': case 'э': return "e"; case 'ж': return "zh"; case 'з': case 'z': case 'с': case 's': return "s";
            case 'и': case 'й': case 'ы': case 'y': case 'j': case 'i': return "i"; case 'к': case 'c': case 'q': case 'k': return "k";
            case 'л': return "l"; case 'м': return "m"; case 'н': return "n"; case 'о': return "o"; case 'п': return "p"; case 'р': return "r";
            case 'т': return "t"; case 'у': return "u"; case 'ф': return "f"; case 'х': case 'x': case 'h': return "h"; case 'ц': return "ts";
            case 'ч': return "ch"; case 'ш': case 'щ': return "sh"; case 'ю': return "iu"; case 'я': return "ia"; case 'ь': case 'ъ': return "";
            default:
                if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) return String.valueOf(c);
                return "";
        }
    }

    /** Edit distance with swapped neighbours as one edit; stops early past the limit (then returns limit + 1). */
    static int distance(String a, String b, int limit) {
        int n = a.length(), m = b.length();
        if (Math.abs(n - m) > limit) return limit + 1;
        int[] two = new int[m + 1], one = new int[m + 1], row = new int[m + 1];
        for (int j = 0; j <= m; j++) one[j] = j;
        for (int i = 1; i <= n; i++) {
            row[0] = i;
            int best = row[0];
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                int v = Math.min(Math.min(one[j] + 1, row[j - 1] + 1), one[j - 1] + cost);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) v = Math.min(v, two[j - 2] + 1);
                row[j] = v;
                best = Math.min(best, v);
            }
            if (best > limit) return limit + 1;
            int[] t = two; two = one; one = row; row = t;
        }
        return one[m];
    }

    /**
     * Timed lines → advertising stretches. Each line is read together with the one after it: speech recognition
     * splits a name across lines ("один" | "вин", "pin" | "up"), so a word cut in two is still found. A hit in the
     * pair is put on the line itself only when the line has some of it -- a neighbour's word never marks an
     * ordinary line.
     */
    static long[] stretches(List<long[]> times, List<String> texts, String[] strong, String[] weak) {
        List<long[]> cues = new ArrayList<>(); // start, end, strong
        List<java.util.BitSet> cueWeak = new ArrayList<>();
        for (int i = 0; i < times.size(); i++) {
            String self = texts.get(i);
            Hit h = scan(self, strong, weak);
            long end = times.get(i)[1];
            if (h.strong == 0 && i + 1 < times.size() && times.get(i + 1)[0] - end < 1500) {
                // a name cut between this line and the next: the stretch takes both lines
                Hit pair = scan(lastWords(self) + " " + firstWords(texts.get(i + 1)), strong, weak);
                Hit next = scan(texts.get(i + 1), strong, weak);
                if (pair.strong > next.strong) { h.strong += pair.strong - next.strong; end = Math.max(end, times.get(i + 1)[1]); }
            }
            if (h.any()) { cues.add(new long[]{times.get(i)[0], end, h.strong}); cueWeak.add(h.weak); }
        }
        List<long[]> groups = new ArrayList<>(); // start, end, strong
        List<java.util.BitSet> groupWeak = new ArrayList<>();
        for (int i = 0; i < cues.size(); i++) {
            long[] c = cues.get(i);
            long[] last = groups.isEmpty() ? null : groups.get(groups.size() - 1);
            if (last != null && c[0] - last[1] <= 12000) { last[1] = Math.max(last[1], c[1]); last[2] += c[2]; groupWeak.get(groupWeak.size() - 1).or(cueWeak.get(i)); }
            else { groups.add(c.clone()); groupWeak.add((java.util.BitSet) cueWeak.get(i).clone()); }
        }
        List<long[]> kept = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            long[] g = groups.get(i);
            // a strong word, or two different weak ones
            // an integration goes on a little after its last mention (the closing line, the link): 1.5 s more
            if (g[2] > 0 || groupWeak.get(i).cardinality() >= 2) kept.add(new long[]{Math.max(0, g[0] - 300), g[1] + 1500});
        }
        // stretches that touch become one
        List<long[]> merged = new ArrayList<>();
        for (long[] k : kept) {
            long[] last = merged.isEmpty() ? null : merged.get(merged.size() - 1);
            if (last != null && k[0] <= last[1]) last[1] = Math.max(last[1], k[1]); else merged.add(k);
        }
        long[] out = new long[Math.min(merged.size(), 16) * 2];
        for (int i = 0; i < out.length / 2; i++) { out[2 * i] = merged.get(i)[0]; out[2 * i + 1] = merged.get(i)[1]; }
        return out;
    }

    private static String lastWords(String text) { String[] w = normal(text).trim().split(" "); return w.length < 2 ? w[w.length - 1] : w[w.length - 2] + " " + w[w.length - 1]; }
    private static String firstWords(String text) { String[] w = normal(text).trim().split(" "); return w.length < 2 ? w[0] : w[0] + " " + w[1]; }

    /** "00:01:02.500" or "01:02.500" → milliseconds; -1 when it is not a time. */
    static long time(String text) {
        String t = text.trim().split("\\s")[0].replace(',', '.');
        String[] parts = t.split(":");
        try {
            double seconds = Double.parseDouble(parts[parts.length - 1]);
            long minutes = parts.length > 1 ? Long.parseLong(parts[parts.length - 2]) : 0, hours = parts.length > 2 ? Long.parseLong(parts[parts.length - 3]) : 0;
            return Math.round(((hours * 60 + minutes) * 60 + seconds) * 1000);
        } catch (Throwable e) {
            return -1;
        }
    }
}
