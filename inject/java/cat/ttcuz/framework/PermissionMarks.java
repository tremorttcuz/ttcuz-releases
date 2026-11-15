package cat.ttcuz.framework;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Where a plugin's code uses what it was allowed, line by line.
 *
 * The editor marks each such line (a call like ctx.http(...), a hook like onFeed) with the permission behind it, and
 * the author can say why right in the code, on the line above or at the end of the line:
 *
 *     // @uses network: loads the quote of the day
 *     String quote = ttcuz().http(URL);
 *
 * Those reasons go into manifest.json as "uses" ({"network": "loads the quote of the day"}) and are shown under the
 * key button next to each permission, so a person sees not only what a plugin may do but what it does with it.
 */
final class PermissionMarks {
    private PermissionMarks() {}

    /** One use: 0-based line, the permission, and the code that needs it (e.g. "http(", "onFeed"). */
    static final class Mark {
        final int line;
        final String permission;
        final String what;
        Mark(int line, String permission, String what) { this.line = line; this.permission = permission; this.what = what; }
    }

    private static final String[][] CALLS = {
            {"\\.http\\s*\\(", PluginPermissions.NETWORK, "http()"},
            {"\\.notify\\s*\\(", PluginPermissions.NOTIFICATIONS, "notify()"},
            {"\\.(?:copy|paste)\\s*\\(", PluginPermissions.CLIPBOARD, "copy()/paste()"},
            {"\\.saveDownload\\s*\\(", PluginPermissions.STORAGE, "saveDownload()"},
            {"\\.accountId\\s*\\(", PluginPermissions.ACCOUNT, "accountId()"},
            {"\\.(?:hostLoader\\s*\\(|(?:type|method|field)\\s*\\(\\s*\")", PluginPermissions.TIKTOK, "hostLoader()/type()/method()/field()"},
            {"\\.(?:overlay|removeOverlay|find|views)\\s*\\(", PluginPermissions.INTERFACE, "overlay()/find()/views()"},
            {"\\.vibrate\\s*\\(", "android.permission.VIBRATE", "vibrate()"},
            {"\\bon(?:View|Screen)\\s*\\(", PluginPermissions.INTERFACE, "onView/onScreen"},
            {"\\bon(?:Text|Nickname)\\s*\\(", PluginPermissions.TEXT, "onText/onNickname"},
            {"\\bonFeed\\s*\\(", PluginPermissions.FEED, "onFeed"},
            {"\\bonColour\\s*\\(", PluginPermissions.COLOURS, "onColour"},
            {"\\bonRegion\\s*\\(", PluginPermissions.REGION, "onRegion"},
            {"\\bonEvent\\s*\\(", PluginPermissions.EVENTS, "onEvent"},
            {"\\bonAccountChanged\\s*\\(", PluginPermissions.ACCOUNT, "onAccountChanged"},
            {"\\.(?:blur|motionBlur|colourMatrix|shader|uniform|clearEffect)\\s*\\(", PluginPermissions.EFFECTS, "blur()/motionBlur()/shader()"},
            {"\\.(?:modSettings?|setModSetting|refreshModScreen)\\s*\\(", PluginPermissions.MOD, "modSetting()/setModSetting()"},
            {"\\bonMod(?:Screen|Panel|Settings)\\s*\\(", PluginPermissions.MOD, "onModScreen/onModPanel/onModSettings"},
            {"ttcuz\\(\\)\\s*\\.\\s*(?:query|queryOne|tree|hide|unhide|inject)\\s*\\(", PluginPermissions.INTERFACE, "query()/tree()/hide()/inject()"},
            {"ttcuz\\(\\)\\s*\\.\\s*(?:click|longClick|tap|swipe|type)\\s*\\((?!\\s*\")", PluginPermissions.AUTOMATION, "click()/tap()/swipe()/type()"},
            {"ttcuz\\(\\)\\s*\\.\\s*(?:video|videoId|playing|pause|play|speed|saveVideo)\\s*\\(", PluginPermissions.PLAYER, "video()/pause()/play()/speed()"},
            {"ttcuz\\(\\)\\s*\\.\\s*call\\s*\\(", PluginPermissions.EVENTS, "call()"},
            {"\\bonTikTok\\s*\\(", PluginPermissions.WATCH, "onTikTok"},
    };
    private static final Pattern[] PATTERNS = new Pattern[CALLS.length];
    static {
        for (int i = 0; i < CALLS.length; i++) PATTERNS[i] = Pattern.compile(CALLS[i][0]);
    }
    /** has("android.permission.CAMERA"), request(...), Manifest.permission.CAMERA */
    private static final Pattern ANDROID = Pattern.compile("(?:\"android\\.permission\\.|Manifest\\.permission\\.)([A-Z_]{2,64})");
    /** // @uses network: why   (also @разрешение) */
    private static final Pattern REASON = Pattern.compile("//\\s*@(?:uses|разрешение)\\s+([A-Za-z][\\w.]{1,95})\\s*[:—-]?\\s*(.*)$");

    /** Every use in one file, top to bottom; a line may carry several. Comments do not count. */
    static List<Mark> find(String code) {
        List<Mark> out = new ArrayList<>();
        if (code == null) return out;
        String[] lines = code.split("\n", -1);
        boolean block = false;
        for (int n = 0; n < lines.length && n < 20000; n++) {
            String line = lines[n];
            // drop block comments and the line comment, keep the code
            StringBuilder plain = new StringBuilder(), bare = new StringBuilder(); // bare: strings blanked
            char quote = 0;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (quote != 0) { // "https://..." is not a comment
                    plain.append(c);
                    if (c == '\\' && i + 1 < line.length()) plain.append(line.charAt(++i));
                    else if (c == quote) { quote = 0; bare.append(c); }
                    continue;
                }
                if (!block && (c == '"' || c == '\'')) { quote = c; plain.append(c); bare.append(c); continue; }
                if (block) {
                    if (line.startsWith("*/", i)) { block = false; i++; }
                    continue;
                }
                if (line.startsWith("/*", i)) { block = true; i++; continue; }
                if (line.startsWith("//", i)) break;
                plain.append(c);
                bare.append(c);
            }
            String text = plain.toString(), calls = bare.toString();
            if (text.trim().isEmpty()) continue;
            for (int i = 0; i < PATTERNS.length; i++) {
                if (PATTERNS[i].matcher(calls).find() && !has(out, n, CALLS[i][1])) out.add(new Mark(n, CALLS[i][1], CALLS[i][2]));
            }
            Matcher android = ANDROID.matcher(text);
            while (android.find()) {
                String name = "android.permission." + android.group(1);
                if (!has(out, n, name)) out.add(new Mark(n, name, android.group(1)));
            }
        }
        return out;
    }

    private static boolean has(List<Mark> marks, int line, String permission) {
        for (Mark mark : marks) if (mark.line == line && mark.permission.equals(permission)) return true;
        return false;
    }

    /** The reason the author wrote for this use: at the end of its line, or on the comment lines just above it. */
    static String reason(String code, Mark mark) {
        String[] lines = code.split("\n", -1);
        if (mark.line >= lines.length) return null;
        String own = reasonOn(lines[mark.line], mark.permission);
        if (own != null) return own;
        for (int n = mark.line - 1; n >= 0 && n >= mark.line - 4; n--) {
            String above = lines[n].trim();
            if (!above.startsWith("//") && !above.startsWith("@")) break;
            String found = reasonOn(above, mark.permission);
            if (found != null) return found;
        }
        return null;
    }

    private static String reasonOn(String line, String permission) {
        Matcher m = REASON.matcher(line);
        if (!m.find() || !matches(m.group(1), permission)) return null;
        String why = m.group(2).trim();
        return why.isEmpty() ? null : why;
    }

    /** "CAMERA" stands for android.permission.CAMERA. */
    private static boolean matches(String written, String permission) {
        return written.equals(permission) || ("android.permission." + written).equals(permission);
    }

    /** The comment that explains a use, ready to put above its line. */
    static String comment(String permission) {
        String name = permission.startsWith("android.permission.") ? permission.substring("android.permission.".length()) : permission;
        return "// @uses " + name + ": ";
    }

    /** All reasons in the project, permission → reason (several uses of one permission are joined); goes to manifest "uses". */
    static JSONObject reasons(JSONArray files) {
        Map<String, List<String>> found = new LinkedHashMap<>();
        for (int i = 0; files != null && i < files.length(); i++) {
            JSONObject file = files.optJSONObject(i);
            if (file == null) continue;
            String code = file.optString("text");
            // written reasons count even without a detected use, e.g. for reflection the scanner cannot see
            for (String line : code.split("\n")) {
                Matcher m = REASON.matcher(line);
                if (!m.find()) continue;
                String permission = m.group(1).matches("[A-Z_]{2,64}") ? "android.permission." + m.group(1) : m.group(1);
                String why = m.group(2).trim();
                if (why.isEmpty()) continue;
                List<String> list = found.get(permission);
                if (list == null) found.put(permission, list = new ArrayList<>());
                if (!list.contains(why) && list.size() < 4) list.add(why);
            }
        }
        JSONObject out = new JSONObject();
        for (Map.Entry<String, List<String>> entry : found.entrySet()) {
            String joined = String.join("; ", entry.getValue());
            try { out.put(entry.getKey(), joined.length() > 300 ? joined.substring(0, 300) : joined); } catch (Exception ignored) { }
        }
        return out;
    }

    /** Permissions the code uses that the manifest does not ask for. */
    static List<String> unasked(JSONArray files, List<String> asked) {
        List<String> out = new ArrayList<>();
        for (int i = 0; files != null && i < files.length(); i++) {
            JSONObject file = files.optJSONObject(i);
            if (file == null) continue;
            for (Mark mark : find(file.optString("text")))
                if (!asked.contains(mark.permission) && !out.contains(mark.permission)) out.add(mark.permission);
        }
        return out;
    }
}
