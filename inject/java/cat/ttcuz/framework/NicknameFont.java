package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** A font span confined to the nickname. Files belong to the account's cloud backup. */
public final class NicknameFont {

    public static final String KEY = "profile_nickname_font";
    public static final String HASH = "profile_nickname_font_hash";
    public static final int LIMIT = 1024 * 1024;
    private static final Map<String, Typeface> faces = new HashMap<>();
    private static final Map<String, Long> requests = new HashMap<>();

    private NicknameFont() {}

    public static boolean validChoice(String name) {
        if (Fonts.FILE.equals(name)) return true;
        for (String family : Fonts.PRESETS) if (family.equals(name)) return true;
        return false;
    }

    public static String chosen() {
        Context context = Ttcuz.context();
        if (context == null) return "";
        try {
            String value = AccountAppearance.prefs(context).getString(KEY, "");
            return validChoice(value) ? value : "";
        } catch (RuntimeException error) {
            return "";
        }
    }

    static String hash() {
        try {
            String hash = AccountAppearance.prefs(Ttcuz.context()).getString(HASH, "");
            return hash.matches("[a-f0-9]{64}") ? hash : "";
        } catch (RuntimeException error) {
            return "";
        }
    }

    public static void choose(String name) {
        if (!validChoice(name)) return;
        Context context = Ttcuz.context();
        if (context == null) return;
        AccountAppearance.prefs(context).edit().putString(KEY, name).apply();
        changed();
    }

    static File file(Context context) {
        return AccountAppearance.file(context, "nickname-font");
    }

    static void changed() {
        CloudAppearance.changed();
        ProfilePreview.accountUpdated();
        Badge.refreshProfiles();
        TtcuzProfileSync.publishOwn();
    }

    /** Worker thread only. Validate before replacing a previously selected font. */
    public static boolean take(Context context, Uri uri, String expectedOwner) {
        SharedPreferences preferences;
        String owner;
        File target;
        synchronized (AccountAppearance.class) {
            owner = AccountAppearance.scope(context);
            if (!expectedOwner.equals(owner)) return false;
            preferences = AccountAppearance.prefs(context);
            target = file(context);
        }
        File temp = new File(target.getPath() + "." + java.util.UUID.randomUUID() + ".tmp");
        File previous = new File(
            target.getPath() + ".previous." + java.util.UUID.randomUUID() + ".tmp"
        );
        boolean installed = false,
            committed = false;
        try {
            if (
                !target.getParentFile().isDirectory() && !target.getParentFile().mkdirs()
            ) return false;
            try (
                InputStream in = context.getContentResolver().openInputStream(uri);
                FileOutputStream out = new FileOutputStream(temp)
            ) {
                if (in == null) return false;
                int n,
                    size = 0;
                byte[] block = new byte[16384];
                while ((n = in.read(block)) != -1) {
                    size += n;
                    if (size > LIMIT) return false;
                    out.write(block, 0, n);
                }
            }
            byte[] raw = Net.read(temp);
            if (!validFile(raw)) return false;
            Typeface face = Typeface.createFromFile(temp);
            if (face == null) return false;
            String sha = digest(raw);
            if (target.exists() && !target.renameTo(previous)) return false;
            if (!temp.renameTo(target)) return false;
            installed = true;
            String beforeChoice = preferences.getString(KEY, "");
            String beforeHash = preferences.getString(HASH, "");
            if (!preferences.edit().putString(KEY, Fonts.FILE).putString(HASH, sha).commit()) {
                // Android updates memory even when commit cannot persist to disk.
                preferences.edit().putString(KEY, beforeChoice).putString(HASH, beforeHash).apply();
                return false;
            }
            committed = true;
            synchronized (faces) {
                if (faces.size() >= 64) faces.clear();
                faces.put(owner + ":file:" + sha, face);
            }
            return true;
        } catch (Exception error) {
            Diary.note("nickname font: " + error);
            return false;
        } finally {
            temp.delete();
            if (committed) previous.delete();
            else {
                if (installed) target.delete();
                if (previous.exists()) previous.renameTo(target);
            }
        }
    }

    /** Basic SFNT bounds, followed by Android's font parser when imported/loaded. */
    static boolean validFile(byte[] raw) {
        if (raw == null || raw.length < 12 || raw.length > LIMIT) return false;
        java.nio.ByteBuffer bytes = java.nio.ByteBuffer.wrap(raw).order(
            java.nio.ByteOrder.BIG_ENDIAN
        );
        int signature = bytes.getInt();
        if (signature != 0x00010000 && signature != 0x4f54544f) return false;
        int tables = bytes.getShort() & 0xffff;
        if (tables < 1 || tables > 256 || 12L + 16L * tables > raw.length) return false;
        for (int i = 0; i < tables; i++) {
            long offset = bytes.getInt(12 + 16 * i + 8) & 0xffffffffL;
            long length = bytes.getInt(12 + 16 * i + 12) & 0xffffffffL;
            if (offset < 12L + 16L * tables || offset + length > raw.length) return false;
        }
        return true;
    }

    static String digest(byte[] raw) throws Exception {
        byte[] sha = java.security.MessageDigest.getInstance("SHA-256").digest(raw);
        StringBuilder text = new StringBuilder();
        for (byte b : sha) text.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return text.toString();
    }

    static boolean active(char mark) {
        String uid = ProfileStyle.uidForMark(mark);
        if (uid == null) return false;
        if (uid.equals(Account.liveId())) return !chosen().isEmpty();
        TtcuzProfileSync.Metadata data = TtcuzProfileSync.cached(uid);
        return data != null && !data.nicknameFont.isEmpty();
    }

    private static Typeface face(char mark) {
        String uid = ProfileStyle.uidForMark(mark);
        if (uid == null) return null;
        boolean own = uid.equals(Account.liveId());
        TtcuzProfileSync.Metadata data = own ? null : TtcuzProfileSync.cached(uid);
        String name = own ? chosen() : data == null ? "" : data.nicknameFont;
        String sha = own ? hash() : data == null ? "" : data.nicknameFontHash;
        if (
            name.isEmpty() ||
            !validChoice(name) ||
            (Fonts.FILE.equals(name) && !sha.matches("[a-f0-9]{64}"))
        ) return null;
        String key = uid + ":" + name + ":" + sha;
        synchronized (faces) {
            Typeface known = faces.get(key);
            if (known != null) return known;
            if (!Fonts.FILE.equals(name)) {
                Typeface preset = Typeface.create(name, Typeface.NORMAL);
                faces.put(key, preset);
                return preset;
            }
            Long asked = requests.get(key);
            long now = android.os.SystemClock.uptimeMillis();
            if (asked != null && now - asked < 30000L) return null;
            if (requests.size() >= 128) requests.clear();
            requests.put(key, now);
        }
        Context context = Ttcuz.context();
        if (context == null) return null;
        final File local = own
            ? file(context)
            : new File(context.getFilesDir(), "ttcuz/nickname-fonts/" + sha);
        Net.away("nickname font load", () -> {
            try {
                Typeface loaded;
                if (!Fonts.FILE.equals(name)) loaded = Typeface.create(name, Typeface.NORMAL);
                else {
                    byte[] raw = Net.read(local);
                    if (!validFile(raw) || !sha.equals(digest(raw))) {
                        if (own) return;
                        raw = Net.bytes(CloudProfileProvider.fontAddress(uid, sha));
                        if (!validFile(raw) || !sha.equals(digest(raw))) return;
                        Net.save(local, raw);
                    }
                    loaded = Typeface.createFromFile(local);
                }
                synchronized (faces) {
                    if (faces.size() >= 64) faces.clear();
                    faces.put(key, loaded);
                }
                Badge.refreshProfiles();
            } catch (Exception error) {
                Diary.note("nickname font load: " + error);
            }
        });
        return null;
    }

    static void apply(android.text.Spannable text, int end, char mark) {
        // The nickname owns its family; the surrounding UI keeps the reader's font.
        for (android.text.style.TypefaceSpan nativeFont : text.getSpans(
            0, end, android.text.style.TypefaceSpan.class
        )) text.removeSpan(nativeFont);
        for (Span old : text.getSpans(0, end, Span.class)) text.removeSpan(old);
        text.setSpan(new Span(mark), 0, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    static final class Span extends MetricAffectingSpan {

        private final Typeface family;

        Span(char mark) {
            family = face(mark);
        }

        private void apply(TextPaint paint) {
            if (family != null) paint.setTypeface(family);
        }

        public void updateDrawState(TextPaint paint) {
            apply(paint);
        }

        public void updateMeasureState(TextPaint paint) {
            apply(paint);
        }
    }
}
