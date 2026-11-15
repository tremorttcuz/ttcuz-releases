package cat.ttcuz.framework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The editor's own look at code before it goes for review: the same rules as the server (server/cloudflare/guard.mjs),
 * so obfuscated code or code that loads other code is stopped on the phone with the reasons, not after the upload.
 */
final class SourceGuard {
    private SourceGuard() {}

    private static final Set<String> SHORT_OK = new HashSet<>(Arrays.asList("i", "j", "k", "n", "m", "x", "y", "z", "e", "t", "c", "v", "s", "r", "g", "b", "a", "w", "h", "p",
            "id", "ok", "in", "on", "to", "at", "dp", "px", "db", "io", "up", "it", "ex", "el", "fn", "cb", "os", "is", "ms", "dt", "dx", "dy"));
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList("if", "for", "while", "switch", "catch", "return", "new", "else", "do", "try", "synchronized", "super", "this", "throw", "case", "assert"));
    private static final Pattern LITERALS = Pattern.compile("/\\*[\\s\\S]*?\\*/|//[^\\n]*|\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*'");
    private static final Pattern TYPES = Pattern.compile("\\b(?:class|interface|enum|record)\\s+([A-Za-z_$][\\w$]*)");
    private static final Pattern METHODS = Pattern.compile("[\\w>\\]]\\s+([A-Za-z_$][\\w$]*)\\s*\\([^;{)]*\\)\\s*(?:throws\\s+[\\w.,\\s]+)?\\{");
    private static final Pattern FIELDS = Pattern.compile("\\b(?:final\\s+)?(?:int|long|short|byte|char|float|double|boolean|var|String|Object|[A-Z][\\w$]*)(?:<[^<>;]*>)?(?:\\[\\])*\\s+([A-Za-z_$][\\w$]*)\\s*(?=[=;,):])");
    private static final Pattern STRINGS = Pattern.compile("\"((?:\\\\.|[^\"\\\\\\n])*)\"");
    private static final Object[][] HIDDEN = {
            {Pattern.compile("\\bDexClassLoader\\b|\\bInMemoryDexClassLoader\\b|\\bPathClassLoader\\b|\\bBaseDexClassLoader\\b|\\bDexFile\\b"), "загружает чужой код (DexClassLoader)"},
            {Pattern.compile("\\bdefineClass\\s*\\("), "создаёт классы на лету (defineClass)"},
            {Pattern.compile("\\bSystem\\s*\\.\\s*(?:load|loadLibrary)\\s*\\("), "загружает нативную библиотеку"},
            {Pattern.compile("\\bRuntime\\s*\\.\\s*getRuntime\\s*\\(\\s*\\)\\s*\\.\\s*exec\\s*\\(|\\bProcessBuilder\\b"), "запускает команды системы"},
    };

    /** Comments blanked, string and char literals emptied; line breaks kept. */
    static String codeOnly(String text) {
        Matcher m = LITERALS.matcher(text);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String found = m.group();
            String replacement = found.startsWith("\"") ? "\"\"" : found.startsWith("'") ? "''" : found.replaceAll("[^\\n]", " ");
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** Why the project cannot go for review; empty when it reads like code a person wrote. */
    static List<String> unreadable(JSONArray files) {
        Set<String> reasons = new LinkedHashSet<>();
        List<String> names = new ArrayList<>();
        long lines = 0, chars = 0;
        for (int i = 0; files != null && i < files.length(); i++) {
            JSONObject file = files.optJSONObject(i);
            if (file == null) continue;
            String path = file.optString("path"), text = file.optString("text");
            for (String line : text.split("\n", -1)) {
                lines++; chars += line.length();
                if (line.length() > 1500) reasons.add(Text.t("строки длиной в тысячи символов (сжатый код)") + ": " + path);
            }
            String code = codeOnly(text);
            for (int c = 0; c < code.length(); c++) if (code.charAt(c) > 127) { reasons.add(Text.t("имена не латиницей") + ": " + path); break; }
            if (code.contains("\\u")) reasons.add(Text.t("код спрятан в \\u-последовательностях") + ": " + path);
            for (Pattern p : new Pattern[]{TYPES, METHODS, FIELDS}) {
                Matcher m = p.matcher(code);
                while (m.find()) if (!KEYWORDS.contains(m.group(1))) names.add(m.group(1));
            }
            Matcher xor = Pattern.compile("\\(\\s*char\\s*\\)\\s*\\(?[^;\\n]*\\^").matcher(code);
            int xors = 0; while (xor.find()) xors++;
            if (xors >= 2) reasons.add(Text.t("строки расшифровываются через XOR") + ": " + path);
            Matcher strings = STRINGS.matcher(text);
            while (strings.find()) {
                String value = strings.group(1);
                if (value.length() > 600 && value.matches("[A-Za-z0-9+/=_-]+")) { reasons.add(Text.t("длинная закодированная строка") + ": " + path); break; }
            }
            for (Object[] hidden : HIDDEN) if (((Pattern) hidden[0]).matcher(code).find()) reasons.add(Text.t((String) hidden[1]) + ": " + path);
        }
        if (lines > 0 && chars / lines > 220) reasons.add(Text.t("средняя строка длиннее 220 символов — код сжат"));
        if (names.size() >= 8) {
            int shortNames = 0;
            for (String name : names) if (name.length() <= 2 && !SHORT_OK.contains(name.toLowerCase(java.util.Locale.ROOT))) shortNames++;
            if (shortNames * 10 > names.size() * 4) reasons.add(Text.t("большинство имён из одной-двух букв") + " (" + shortNames + " / " + names.size() + ")");
        }
        int confusing = 0;
        for (String name : names) if (name.matches("[Il1|]{3,}|[O0o]{3,}|_{2,}|[Il1O0]{5,}")) confusing++;
        if (confusing >= 2) reasons.add(Text.t("похожие друг на друга имена (IlIl, O0O0)"));
        return new ArrayList<>(reasons);
    }
}
