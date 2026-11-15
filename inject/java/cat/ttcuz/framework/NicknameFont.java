package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.text.style.TypefaceSpan;
import cat.ttcuz.framework.TtcuzProfileSync;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class NicknameFont {
    public static final String HASH = "profile_nickname_font_hash";
    public static final String KEY = "profile_nickname_font";
    public static final int LIMIT = 1048576;
    private static final Map<String, Typeface> faces = new HashMap();
    private static final Map<String, Long> requests = new HashMap();

    private NicknameFont() {
    }

    public static boolean validChoice(String str) {
        if (Fonts.FILE.equals(str)) {
            return true;
        }
        for (String str2 : Fonts.PRESETS) {
            if (str2.equals(str)) {
                return true;
            }
        }
        return false;
    }

    public static String chosen() {
        Context context = Ttcuz.context();
        if (context == null) {
            return "";
        }
        try {
            String string = AccountAppearance.prefs(context).getString(KEY, "");
            return validChoice(string) ? string : "";
        } catch (RuntimeException unused) {
            return "";
        }
    }

    static String hash() {
        try {
            String string = AccountAppearance.prefs(Ttcuz.context()).getString(HASH, "");
            return Ids.sha256(string) ? string : "";
        } catch (RuntimeException unused) {
            return "";
        }
    }

    public static void choose(String str) {
        Context context;
        if (validChoice(str) && (context = Ttcuz.context()) != null) {
            AccountAppearance.prefs(context).edit().putString(KEY, str).apply();
            changed();
        }
    }

    /** Back to the nickname in the app's own font. */
    public static void clear() {
        Context context = Ttcuz.context();
        if (context == null) return;
        AccountAppearance.prefs(context).edit().remove(KEY).apply();
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

    /**
     * Imports a picked font file as the nickname font of account scope {@code scope}: at most 1 MiB, a real
     * TrueType/OpenType file that Android can load. The previous file is kept until the new one is in place and saved.
     */
    public static boolean take(Context context, Uri uri, String scope) {
        synchronized (AccountAppearance.class) {
            if (!scope.equals(AccountAppearance.scope(context))) return false;
            SharedPreferences prefs = AccountAppearance.prefs(context);
            File target = file(context);
            File temp = new File(target.getPath() + "." + UUID.randomUUID() + ".tmp");
            File previous = new File(target.getPath() + ".previous." + UUID.randomUUID() + ".tmp");
            boolean done = false;
            try {
                File parent = target.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) return false;
                try (InputStream in = context.getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(temp)) {
                    if (in == null) return false;
                    byte[] buffer = new byte[16384];
                    int total = 0, read;
                    while ((read = in.read(buffer)) != -1) {
                        total += read;
                        if (total > LIMIT) return false;
                        out.write(buffer, 0, read);
                    }
                }
                byte[] bytes = Net.read(temp);
                if (!validFile(bytes)) return false;
                Typeface face = Typeface.createFromFile(temp);
                if (face == null) return false;
                String hash = digest(bytes);
                if (target.exists() && !target.renameTo(previous)) return false;
                if (!temp.renameTo(target)) return false;
                String oldChoice = prefs.getString(KEY, ""), oldHash = prefs.getString(HASH, "");
                if (!prefs.edit().putString(KEY, Fonts.FILE).putString(HASH, hash).commit()) {
                    prefs.edit().putString(KEY, oldChoice).putString(HASH, oldHash).apply();
                    target.delete();
                    return false;
                }
                synchronized (faces) {
                    if (faces.size() >= 128) faces.clear();
                    faces.put(Account.liveId() + ":" + Fonts.FILE + ":" + hash, face);
                }
                done = true;
                return true;
            } catch (Exception error) {
                Diary.note("nickname font: " + error);
                return false;
            } finally {
                temp.delete();
                if (done) previous.delete();
                else if (previous.exists() && !target.exists()) previous.renameTo(target);
                else previous.delete();
            }
        }
    }

    static boolean validFile(byte[] bArr) {
        ByteBuffer order;
        int i;
        int i2;
        if (bArr != null && bArr.length >= 12 && bArr.length <= 1048576 && (((i = (order = ByteBuffer.wrap(bArr).order(ByteOrder.BIG_ENDIAN)).getInt()) == 65536 || i == 1330926671) && (i2 = order.getShort() & 65535) >= 1 && i2 <= 256)) {
            long j = (i2 * 16) + 12;
            if (j <= bArr.length) {
                for (int i3 = 0; i3 < i2; i3++) {
                    int i4 = i3 * 16;
                    long j2 = order.getInt(i4 + 20) & 4294967295L;
                    long j3 = 4294967295L & order.getInt(i4 + 24);
                    if (j2 < j || j2 + j3 > bArr.length) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    static String digest(byte[] bArr) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bArr);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(b & 255)));
        }
        return sb.toString();
    }

    static boolean active(char c) {
        String uidForMark = ProfileStyle.uidForMark(c);
        if (uidForMark == null) {
            return false;
        }
        if (uidForMark.equals(Account.liveId())) {
            return !chosen().isEmpty();
        }
        TtcuzProfileSync.Metadata cached = TtcuzProfileSync.cached(uidForMark);
        return (cached == null || cached.nicknameFont.isEmpty()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Typeface face(char c) {
        final String str;
        String str2;
        final File file;
        final String uidForMark = ProfileStyle.uidForMark(c);
        if (uidForMark == null) {
            return null;
        }
        final boolean equals = uidForMark.equals(Account.liveId());
        TtcuzProfileSync.Metadata cached = equals ? null : TtcuzProfileSync.cached(uidForMark);
        if (equals) {
            str = chosen();
        } else {
            str = cached == null ? "" : cached.nicknameFont;
        }
        if (equals) {
            str2 = hash();
        } else {
            str2 = cached == null ? "" : cached.nicknameFontHash;
        }
        final String str3 = str2;
        if (str.isEmpty() || !validChoice(str) || (Fonts.FILE.equals(str) && !Ids.sha256(str3))) {
            return null;
        }
        final String str4 = uidForMark + ":" + str + ":" + str3;
        Map<String, Typeface> map = faces;
        synchronized (map) {
            Typeface typeface = map.get(str4);
            if (typeface != null) {
                return typeface;
            }
            if (!Fonts.FILE.equals(str)) {
                Typeface create = Typeface.create(str, 0);
                map.put(str4, create);
                return create;
            }
            Map<String, Long> map2 = requests;
            Long l = map2.get(str4);
            long uptimeMillis = SystemClock.uptimeMillis();
            if (l != null && uptimeMillis - l.longValue() < 30000) {
                return null;
            }
            if (map2.size() >= 128) {
                map2.clear();
            }
            map2.put(str4, Long.valueOf(uptimeMillis));
            Context context = Ttcuz.context();
            if (context == null) {
                return null;
            }
            if (equals) {
                file = file(context);
            } else {
                file = new File(context.getFilesDir(), "ttcuz/nickname-fonts/" + str3);
            }
            Net.away("nickname font load", new Runnable() { // from class: cat.ttcuz.framework.NicknameFont$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    NicknameFont.lambda$face$0(str, file, str3, equals, uidForMark, str4);
                }
            });
            return null;
        }
    }

    static /* synthetic */ void lambda$face$0(String str, File file, String str2, boolean z, String str3, String str4) {
        Typeface createFromFile;
        try {
            if (Fonts.FILE.equals(str)) {
                byte[] read = Net.read(file);
                if (!validFile(read) || !str2.equals(digest(read))) {
                    if (z) {
                        return;
                    }
                    byte[] bytes = Net.bytes(CloudProfileProvider.fontAddress(str3, str2));
                    if (validFile(bytes) && str2.equals(digest(bytes))) {
                        Net.save(file, bytes);
                    }
                    return;
                }
                createFromFile = Typeface.createFromFile(file);
            } else {
                createFromFile = Typeface.create(str, 0);
            }
            Map<String, Typeface> map = faces;
            synchronized (map) {
                if (map.size() >= 64) {
                    map.clear();
                }
                map.put(str4, createFromFile);
            }
            Badge.refreshProfiles();
        } catch (Exception e) {
            Diary.note("nickname font load: " + String.valueOf(e));
        }
    }

    static void apply(Spannable spannable, int i, char c) {
        for (TypefaceSpan typefaceSpan : (TypefaceSpan[]) spannable.getSpans(0, i, TypefaceSpan.class)) {
            spannable.removeSpan(typefaceSpan);
        }
        for (Span span : (Span[]) spannable.getSpans(0, i, Span.class)) {
            spannable.removeSpan(span);
        }
        spannable.setSpan(new Span(c), 0, i, 33);
    }

    static final class Span extends MetricAffectingSpan {
        private final Typeface family;

        Span(char c) {
            this.family = NicknameFont.face(c);
        }

        private void apply(TextPaint textPaint) {
            Typeface typeface = this.family;
            if (typeface != null) {
                textPaint.setTypeface(typeface);
            }
        }

        @Override // android.text.style.CharacterStyle
        public void updateDrawState(TextPaint textPaint) {
            apply(textPaint);
        }

        @Override // android.text.style.MetricAffectingSpan
        public void updateMeasureState(TextPaint textPaint) {
            apply(textPaint);
        }
    }
}
