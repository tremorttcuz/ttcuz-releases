package cat.narezany.margyt;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.net.Uri;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;
import android.view.View;
import android.widget.TextView;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;
import java.lang.ref.WeakReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Local profile appearance. Sharing requires a separately configured provider. */
public final class ProfileStyle {
    private ProfileStyle() {}

    public static final char MARK = '\u2063';
    private static final String SHARE = "profile_share";
    private static final String SHARE_DEFAULT_MIGRATED = "profile_share_default_migrated";
    private static final String START = "profile_gradient_start";
    private static final String END = "profile_gradient_end";
    private static final String MIDDLE = "profile_gradient_middle";
    private static final String MODE = "profile_gradient_on";
    private static final String THREE = "profile_gradient_three";
    private static final String WIDTH = "profile_gradient_width";
    private static final String STOP = "profile_gradient_stop";
    private static final String DIRECTION = "profile_gradient_direction";
    private static final String BOLD = "profile_nickname_bold";
    private static final String GLOW = "profile_nickname_glow";
    private static final String ANIMATED = "profile_gradient_animated";
    private static final String SPEED = "profile_gradient_speed";
    private static final String CROP_X = "profile_badge_x";
    private static final String CROP_Y = "profile_badge_y";
    private static final String CROP_ZOOM = "profile_badge_zoom";
    private static final String BADGE_OFFSET_X = "profile_badge_offset_x";
    private static final String BADGE_OFFSET_Y = "profile_badge_offset_y";
    private static final String BADGE_SIZE = "profile_badge_size";
    private static final String CROWN_OFFSET_X = "profile_crown_offset_x";
    private static final String CROWN_OFFSET_Y = "profile_crown_offset_y";
    private static final int MAX_SOURCE = 2 * 1024 * 1024;
    private static volatile Bitmap image;
    private static final Map<String, Character> marks = new HashMap<String, Character>();
    private static final Map<Character, TtcuzProfileSync.Metadata> styles =
            new HashMap<Character, TtcuzProfileSync.Metadata>();
    private static final Map<String, Bitmap> remoteImages = new HashMap<String, Bitmap>();
    private static final Set<String> fetching = new HashSet<String>();
    private static final Map<String, Long> failedImages = new HashMap<String, Long>();
    private static final Map<TextView, Character> animatedViews =
            new WeakHashMap<TextView, Character>();
    private static final Map<TextView, ValueAnimator> animators =
            new WeakHashMap<TextView, ValueAnimator>();
    private static final int ANIMATION_WATCHED = 0x5454435A;
    private static volatile int styleRevision;

    private static SharedPreferences prefs() {
        Context context = Margy.context();
        return context == null ? null
                : AccountAppearance.prefs(context);
    }

    public static boolean sharing() {
        SharedPreferences p = prefs();
        if (p == null) return true;
        // Earlier builds silently saved false when verification was pending.
        // Migrate that broken default once; keep a deliberate opt-out after it.
        if (!p.getBoolean(SHARE_DEFAULT_MIGRATED, false)) {
            p.edit().putBoolean(SHARE, true)
                    .putBoolean(SHARE_DEFAULT_MIGRATED, true).apply();
            return true;
        }
        return p.getBoolean(SHARE, true);
    }

    public static void share(boolean enabled) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean(SHARE, enabled)
                .putBoolean(SHARE_DEFAULT_MIGRATED, true).apply();
        if (enabled) {
            TtcuzProfileSync.autoVerifyOwn();
            TtcuzProfileSync.publishOwn();
        }
        else TtcuzProfileSync.unpublishOwn();
    }

    public static boolean gradient() {
        SharedPreferences p = prefs();
        return p != null && p.getBoolean(MODE, false);
    }

    public static boolean styled() {
        return gradient() || AppearanceColors.get(AppearanceColors.NICKNAME) != 0
                || bold() || glow();
    }

    public static void setGradient(boolean enabled) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean(MODE, enabled).apply();
        changed();
    }

    private static void changed() {
        styleRevision++;
        ProfilePreview.accountUpdated();
        Badge.refreshProfiles();
        TtcuzProfileSync.publishOwn();
    }

    public static int revision() { return styleRevision; }
    static String displayName(String uid,String original) {
        if(uid==null)return original;
        if(!uid.equals(Account.liveId())){TtcuzProfileSync.Metadata remote=TtcuzProfileSync.cached(uid);return remote!=null && remote.displayName!=null && !remote.displayName.isEmpty()?remote.displayName:original;}
        SharedPreferences store=prefs();if(store==null)return original;
        String name=store.getString("profile_display_name","");
        // Remove the previous release's example-specific automatic override once.
        if(!store.getBoolean("profile_name_example_removed",false)) {
            if("Lolipuzik".equals(name) && "lolipuzik1".equalsIgnoreCase(Account.profileHandle())) {
                name="";store.edit().remove("profile_display_name").apply();
            }
            store.edit().putBoolean("profile_name_example_removed",true).apply();
        }
        return name.isEmpty()?original:name;
    }
    static String sharedName(){SharedPreferences store=prefs();return store==null?"":store.getString("profile_display_name","").trim();}
    static void setDisplayName(String name){SharedPreferences store=prefs();if(store!=null)store.edit().putString("profile_display_name",name.trim()).putBoolean("profile_name_example_removed",true).apply();changed();Badge.refreshProfiles();}
    static void reload() {
        image=null;styleRevision++;
        new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{
            for(ValueAnimator animator:animators.values())animator.cancel();
            animators.clear();animatedViews.clear();
        });
    }

    public static boolean threeColours() {
        SharedPreferences p = prefs();
        return p != null && p.getBoolean(THREE, false);
    }

    public static void setThreeColours(boolean enabled) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean(THREE, enabled).apply();
        changed();
    }

    public static int middle() {
        SharedPreferences p = prefs();
        return p == null ? 0xFFFE2C55 : p.getInt(MIDDLE, 0xFFFE2C55);
    }

    public static void setMiddle(int colour) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putInt(MIDDLE, 0xFF000000 | (colour & 0xFFFFFF)).apply();
        changed();
    }

    public static int direction() {
        SharedPreferences p = prefs();
        return p == null ? 0 : Math.max(0, Math.min(3, p.getInt(DIRECTION, 0)));
    }

    public static void setDirection(int value) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putInt(DIRECTION, Math.max(0, Math.min(3, value))).apply();
        changed();
    }

    public static int gradientWidth() {
        SharedPreferences p=prefs();return p==null?100:Math.max(50,Math.min(200,p.getInt(WIDTH,100)));
    }
    public static int gradientStop() {
        SharedPreferences p=prefs();return p==null?50:Math.max(5,Math.min(95,p.getInt(STOP,50)));
    }
    public static void setGradientWidth(int value) {
        SharedPreferences p=prefs();if(p!=null)p.edit().putInt(WIDTH,Math.max(50,Math.min(200,value))).apply();changed();
    }
    public static void setGradientStop(int value) {
        SharedPreferences p=prefs();if(p!=null)p.edit().putInt(STOP,Math.max(5,Math.min(95,value))).apply();changed();
    }
    private static int gradientWidth(char mark) {
        if(mark==MARK)return gradientWidth();
        synchronized(marks){TtcuzProfileSync.Metadata data=styles.get(mark);return data==null?100:data.gradientWidth;}
    }
    private static int gradientStop(char mark) {
        if(mark==MARK)return gradientStop();
        synchronized(marks){TtcuzProfileSync.Metadata data=styles.get(mark);return data==null?50:data.gradientStop;}
    }

    public static boolean bold() {
        SharedPreferences p = prefs();
        return p != null && p.getBoolean(BOLD, false);
    }

    public static void setBold(boolean value) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean(BOLD, value).apply();
        changed();
    }

    public static boolean glow() {
        SharedPreferences p = prefs();
        return p != null && p.getBoolean(GLOW, false);
    }

    public static void setGlow(boolean value) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean(GLOW, value).apply();
        changed();
    }

    public static boolean animated() {
        SharedPreferences p = prefs();
        return p != null && p.getBoolean(ANIMATED, false);
    }

    public static void setAnimated(boolean value) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean(ANIMATED, value).apply();
        changed();
        refreshAnimations();
    }

    public static int speed() {
        SharedPreferences p = prefs();
        return p == null ? 50 : Math.max(0, Math.min(100, p.getInt(SPEED, 50)));
    }

    public static void setSpeed(int value) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putInt(SPEED, Math.max(0, Math.min(100, value))).apply();
        changed();
        refreshAnimations();
    }

    public static int start() {
        SharedPreferences p = prefs();
        return p == null ? 0xFFFFFFFF : p.getInt(START, 0xFFFFFFFF);
    }

    public static int end() {
        SharedPreferences p = prefs();
        return p == null ? 0xFF25F4EE : p.getInt(END, 0xFF25F4EE);
    }

    public static void colours(int first, int last) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putInt(START, first).putInt(END, last).apply();
        changed();
    }

    public static void reset() {
        SharedPreferences p = prefs();
        if (p != null) p.edit().remove(MODE).remove(START).remove(END).remove(MIDDLE)
                .remove(THREE).remove(DIRECTION).remove(WIDTH).remove(STOP).remove(BOLD).remove(GLOW)
                .remove(ANIMATED).remove(SPEED).remove(CROWN_OFFSET_X)
                .remove(CROWN_OFFSET_Y).apply();
        removeBadge();
        changed();
    }

    private static File file(Context context) {
        return AccountAppearance.file(context,"profile-badge.png");
    }

    private static File sourceFile(Context context) {
        return AccountAppearance.file(context,"profile-badge-source.png");
    }

    public static int cropX() {
        SharedPreferences p = prefs();
        return p == null ? 50 : p.getInt(CROP_X, 50);
    }

    public static int cropY() {
        SharedPreferences p = prefs();
        return p == null ? 50 : p.getInt(CROP_Y, 50);
    }

    public static int cropZoom() {
        SharedPreferences p = prefs();
        return p == null ? 100 : p.getInt(CROP_ZOOM, 100);
    }

    public static int offsetX() {
        SharedPreferences p = prefs();
        return p == null ? 0 : Math.max(-80, Math.min(80, p.getInt(BADGE_OFFSET_X, 0)));
    }

    public static int offsetY() {
        SharedPreferences p = prefs();
        return p == null ? 0 : Math.max(-50, Math.min(50, p.getInt(BADGE_OFFSET_Y, 0)));
    }

    public static int badgeSize() {
        SharedPreferences p = prefs();
        return p == null ? 100 : Math.max(50, Math.min(200, p.getInt(BADGE_SIZE, 100)));
    }

    public static int crownOffsetX() {
        SharedPreferences p = prefs();
        return p == null ? 0 : Math.max(-100, Math.min(100, p.getInt(CROWN_OFFSET_X, 0)));
    }

    public static int crownOffsetY() {
        SharedPreferences p = prefs();
        return p == null ? 0 : Math.max(-100, Math.min(100, p.getInt(CROWN_OFFSET_Y, 0)));
    }

    public static void setCrownPlacement(int x, int y) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putInt(CROWN_OFFSET_X, Math.max(-100, Math.min(100, x)))
                .putInt(CROWN_OFFSET_Y, Math.max(-100, Math.min(100, y))).apply();
        changed();
    }

    public static int crownOffsetX(char mark) {
        if (mark == MARK) return crownOffsetX();
        if (Badges.isMark(mark) && Badges.isCreator(Badges.byMark(mark))) {
            String uid = Badges.uidForMark(mark);
            if (owns(uid)) return crownOffsetX();
            TtcuzProfileSync.Metadata remote = TtcuzProfileSync.cached(uid);
            return remote == null ? 0 : remote.crownOffsetX;
        }
        synchronized (marks) {
            TtcuzProfileSync.Metadata data = styles.get(mark);
            return data == null ? 0 : data.crownOffsetX;
        }
    }

    public static int crownOffsetY(char mark) {
        if (mark == MARK) return crownOffsetY();
        if (Badges.isMark(mark) && Badges.isCreator(Badges.byMark(mark))) {
            String uid = Badges.uidForMark(mark);
            if (owns(uid)) return crownOffsetY();
            TtcuzProfileSync.Metadata remote = TtcuzProfileSync.cached(uid);
            return remote == null ? 0 : remote.crownOffsetY;
        }
        synchronized (marks) {
            TtcuzProfileSync.Metadata data = styles.get(mark);
            return data == null ? 0 : data.crownOffsetY;
        }
    }

    public static void setBadgePlacement(int x, int y, int size) {
        SharedPreferences p = prefs();
        if (p != null) p.edit()
                .putInt(BADGE_OFFSET_X, Math.max(-80, Math.min(80, x)))
                .putInt(BADGE_OFFSET_Y, Math.max(-50, Math.min(50, y)))
                .putInt(BADGE_SIZE, Math.max(50, Math.min(200, size))).apply();
        changed();
    }

    public static void setCrop(int x, int y, int zoom, Runnable done) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putInt(CROP_X, Math.max(0, Math.min(100, x)))
                .putInt(CROP_Y, Math.max(0, Math.min(100, y)))
                .putInt(CROP_ZOOM, Math.max(100, Math.min(300, zoom))).apply();
        Context context = Margy.context();
        if (context == null) return;
        Net.away("profile crop", () -> {
            try {
                Bitmap original = BitmapFactory.decodeFile(
                        sourceFile(context).getAbsolutePath());
                if (original != null && !renderCrop(context, original))
                    Diary.note("profile crop: invalid result");
            } catch (Throwable error) {
                Diary.note("profile crop: " + error);
            }
            if (done != null) new android.os.Handler(android.os.Looper.getMainLooper()).post(done);
        });
    }

    private static synchronized boolean renderCrop(Context context, Bitmap original)
            throws Exception {
        int side = Math.min(original.getWidth(), original.getHeight());
        if (side < 16) return false;
        int window = Math.max(16, side * 100 / Math.max(100, cropZoom()));
        int left = (original.getWidth() - window) * cropX() / 100;
        int top = (original.getHeight() - window) * cropY() / 100;
        Bitmap square = Bitmap.createBitmap(original, left, top, window, window);
        Bitmap scaled = Bitmap.createScaledBitmap(square, 128, 128, true);
        File destination = file(context);
        File temporary = new File(destination.getParentFile(), "profile-badge.tmp");
        FileOutputStream output = new FileOutputStream(temporary);
        try { scaled.compress(Bitmap.CompressFormat.PNG, 100, output); }
        finally { output.close(); }
        if (temporary.length() > 256 * 1024 || !temporary.renameTo(destination)) {
            temporary.delete();
            return false;
        }
        image = scaled;
        TtcuzProfileSync.publishOwn();
        return true;
    }

    public static File badgeFile() {
        Context context = Margy.context();
        if (context == null) return null;
        File candidate = file(context);
        return candidate.isFile() ? candidate : null;
    }

    public static boolean hasBadge() {
        Context context = Margy.context();
        return context != null && file(context).isFile();
    }

    public static Bitmap badge() {
        Bitmap known = image;
        if (known != null) return known;
        Context context = Margy.context();
        if (context == null) return null;
        try {
            File path = file(context);
            if (!path.isFile() || path.length() > 256 * 1024) return null;
            image = BitmapFactory.decodeFile(path.getAbsolutePath());
            return image;
        } catch (Throwable error) {
            Diary.note("profile badge: " + error);
            return null;
        }
    }

    /** Import a bounded image, crop to a square and store only a small PNG. */
    public static boolean take(Context context, Uri uri) {
        final String owner=AccountAppearance.scope(context);
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            if (input == null) return false;
            java.io.ByteArrayOutputStream raw = new java.io.ByteArrayOutputStream();
            byte[] block = new byte[16384];
            int read;
            while ((read = input.read(block)) != -1) {
                if (raw.size() + read > MAX_SOURCE) {
                    input.close();
                    return false;
                }
                raw.write(block, 0, read);
            }
            input.close();
            byte[] bytes = raw.toByteArray();
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
            if (bounds.outWidth < 16 || bounds.outHeight < 16
                    || bounds.outWidth > 4096 || bounds.outHeight > 4096) return false;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.max(1,
                    Math.max(bounds.outWidth, bounds.outHeight) / 512);
            Bitmap decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            if (decoded == null) return false;
            int largest = Math.max(decoded.getWidth(), decoded.getHeight());
            if (largest > 512) {
                decoded = Bitmap.createScaledBitmap(decoded,
                        decoded.getWidth() * 512 / largest,
                        decoded.getHeight() * 512 / largest, true);
            }
            synchronized(AccountAppearance.class) {
            if(!owner.equals(AccountAppearance.scope(context)))return false;
            File originalFile = sourceFile(context);
            File parent = originalFile.getParentFile();
            if (parent != null) parent.mkdirs();
            File temporary = new File(parent, "profile-badge-source.tmp");
            FileOutputStream output = new FileOutputStream(temporary);
            try { decoded.compress(Bitmap.CompressFormat.PNG, 100, output); }
            finally { output.close(); }
            if (temporary.length() > MAX_SOURCE || !renderCrop(context, decoded)) {
                temporary.delete();
                return false;
            }
            if (!temporary.renameTo(originalFile)) {
                temporary.delete();
                Diary.note("profile badge source: save failed");
            }
            return true;
            }
        } catch (Throwable error) {
            Diary.note("profile badge import: " + error);
            return false;
        }
    }

    public static void removeBadge() {
        Context context = Margy.context();
        if (context == null) return;
        image = null;
        File badge = file(context);
        if (badge.isFile() && !badge.delete()) Diary.note("profile badge: remove failed");
        File original = sourceFile(context);
        if (original.isFile() && !original.delete())
            Diary.note("profile badge source: remove failed");
        TtcuzProfileSync.publishOwn();
    }

    public static boolean owns(String uid) {
        return uid != null && uid.equals(Account.id());
    }

    public static String mark(String uid) {
        if (uid==null || !uid.matches("[0-9]{1,24}")) return "";
        if (owns(uid)) return String.valueOf(MARK);
        TtcuzProfileSync.Metadata metadata = TtcuzProfileSync.cached(uid);
        if (metadata == null) {
            TtcuzProfileSync.request(uid);
        }
        synchronized (marks) {
            Character assigned = marks.get(uid);
            if (assigned == null) {
                if (marks.size() >= 255) return "";
                assigned = Character.valueOf((char) (0xE001 + marks.size()));
                marks.put(uid, assigned);
            }
            if(styles.get(assigned)!=metadata)styleRevision++;
            if (metadata==null) styles.remove(assigned); else styles.put(assigned, metadata);
            return assigned.toString();
        }
    }

    static String uidForMark(char value) {
        if (value==MARK) return Account.id();
        synchronized (marks) {
            for (Map.Entry<String,Character> entry:marks.entrySet())
                if (entry.getValue().charValue()==value) return entry.getKey();
        }
        return null;
    }

    public static boolean isMark(char value) {
        return value == MARK || value >= 0xE001 && value <= 0xE0FF;
    }

    public static boolean hasGradient(char value) {
        if (value == MARK) return gradient()
                || AppearanceColors.get(AppearanceColors.NICKNAME) != 0;
        synchronized (marks) {
            TtcuzProfileSync.Metadata data = styles.get(value);
            return data != null && (data.firstColour != 0 || data.lastColour != 0);
        }
    }

    public static boolean hasDecoration(char value) {
        if (value == MARK) return bold() || glow();
        synchronized (marks) {
            TtcuzProfileSync.Metadata data = styles.get(value);
            return data != null && (data.bold || data.glow);
        }
    }

    public static int start(char value) {
        if (value == MARK) return gradient() ? start() : AppearanceColors.get(
                AppearanceColors.NICKNAME);
        synchronized (marks) {
            TtcuzProfileSync.Metadata data = styles.get(value);
            return data == null ? 0xFFFFFFFF : data.firstColour;
        }
    }

    public static int end(char value) {
        if (value == MARK) return gradient() ? end() : AppearanceColors.get(
                AppearanceColors.NICKNAME);
        synchronized (marks) {
            TtcuzProfileSync.Metadata data = styles.get(value);
            return data == null ? 0xFFFFFFFF : data.lastColour;
        }
    }

    public static int middle(char value) {
        if (value == MARK) return middle();
        synchronized (marks) {
            TtcuzProfileSync.Metadata data = styles.get(value);
            return data == null ? 0xFFFE2C55 : data.middleColour;
        }
    }

    public static boolean threeColours(char value) {
        if (value == MARK) return threeColours();
        synchronized (marks) {
            TtcuzProfileSync.Metadata data = styles.get(value);
            return data != null && data.threeColours;
        }
    }

    public static int direction(char value) {
        if (value == MARK) return direction();
        synchronized (marks) {
            TtcuzProfileSync.Metadata data = styles.get(value);
            return data == null ? 0 : data.direction;
        }
    }

    public static boolean bold(char value) {
        if (value == MARK) return bold();
        synchronized (marks) {
            TtcuzProfileSync.Metadata data = styles.get(value);
            return data != null && data.bold;
        }
    }

    public static boolean glow(char value) {
        if (value == MARK) return glow();
        synchronized (marks) {
            TtcuzProfileSync.Metadata data = styles.get(value);
            return data != null && data.glow;
        }
    }

    public static android.graphics.Shader shader(char value, float width, float height) {
        int direction = direction(value);
        float x0 = 0, y0 = 0, x1 = Math.max(1f, width), y1 = 0;
        if (direction == 1) { x1 = 0; y1 = Math.max(1f, height); }
        else if (direction == 2) y1 = Math.max(1f, height);
        else if (direction == 3) { x0 = Math.max(1f, width); x1 = 0; y1 = Math.max(1f, height); }
        float scale=gradientWidth(value)/100f;
        float cx=(x0+x1)*0.5f,cy=(y0+y1)*0.5f;
        x0=cx+(x0-cx)*scale;y0=cy+(y0-cy)*scale;
        x1=cx+(x1-cx)*scale;y1=cy+(y1-cy)*scale;
        int[] colors = threeColours(value)
                ? new int[] {start(value), middle(value), end(value)}
                : new int[] {start(value), end(value)};
        float[] positions = colors.length == 3
                ? new float[] {0f, gradientStop(value)/100f, 1f} : new float[] {0f, 1f};
        return new LinearGradient(x0, y0, x1, y1, colors, positions,
                animated(value) ? Shader.TileMode.MIRROR : Shader.TileMode.CLAMP);
    }

    public static boolean animated(char mark) {
        if (mark == MARK) return gradient() && animated();
        synchronized (marks) {
            TtcuzProfileSync.Metadata data = styles.get(mark);
            return data != null && data.animated;
        }
    }

    public static int speed(char mark) {
        if (mark == MARK) return speed();
        synchronized (marks) {
            TtcuzProfileSync.Metadata data = styles.get(mark);
            return data == null ? 50 : data.speed;
        }
    }

    /** Keep only visible nickname views ticking, and release them when detached. */
    public static void watchAnimation(final TextView view, final char mark) {
        if (view == null) return;
        synchronized (animatedViews) {
            if (animatedViews.containsKey(view)) {
                animatedViews.put(view, Character.valueOf(mark));
                if (animated(mark)) startAnimation(view, mark);
                else stopAnimation(view);
                return;
            }
            animatedViews.put(view, Character.valueOf(mark));
        }
        if (!Boolean.TRUE.equals(view.getTag(ANIMATION_WATCHED))) {
            view.setTag(ANIMATION_WATCHED, Boolean.TRUE);
            final WeakReference<TextView> target = new WeakReference<TextView>(view);
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override public void onViewAttachedToWindow(View attached) {
                    TextView name = target.get();
                    if (name != null) {
                        Character current;
                        synchronized (animatedViews) { current = animatedViews.get(name); }
                        if (current != null) startAnimation(name, current.charValue());
                    }
                }
                @Override public void onViewDetachedFromWindow(View detached) {
                    TextView name = target.get();
                    if (name != null) stopAnimation(name);
                }
            });
        }
        if (animated(mark) && view.isAttachedToWindow()) startAnimation(view, mark);
    }

    private static void refreshAnimations() {
        synchronized (animatedViews) {
            for (Map.Entry<TextView, Character> entry : animatedViews.entrySet()) {
                TextView view = entry.getKey();
                if (view == null) continue;
                char mark = entry.getValue().charValue();
                if (animated(mark) && view.isAttachedToWindow()) startAnimation(view, mark);
                else stopAnimation(view);
            }
        }
    }

    private static ValueAnimator animationPulse;
    private static long animationFrame;
    private static void startAnimation(final TextView view,final char mark){
        if(!Motion.enabled(view.getContext())||!animated(mark)){stopAnimation(view);return;}
        synchronized(animators){
            if(animationPulse==null||!animationPulse.isStarted()){
                animationPulse=ValueAnimator.ofFloat(0f,1f);animationPulse.setDuration(4000L);animationPulse.setRepeatCount(ValueAnimator.INFINITE);animationPulse.setInterpolator(new LinearInterpolator());
                animationPulse.addUpdateListener(value->{long now=android.os.SystemClock.uptimeMillis();if(now-animationFrame<16L)return;animationFrame=now;
                    synchronized(animators){java.util.Iterator<TextView> iterator=animators.keySet().iterator();while(iterator.hasNext()){TextView name=iterator.next();if(name==null||!name.isAttachedToWindow()){iterator.remove();continue;}if(name.isShown())name.invalidate();}if(animators.isEmpty())value.cancel();}
                });
                animators.put(view,animationPulse);animationPulse.start();
            }else animators.put(view,animationPulse);
        }
    }
    private static void stopAnimation(TextView view){synchronized(animators){animators.remove(view);if(animators.isEmpty()&&animationPulse!=null){animationPulse.cancel();animationPulse=null;}}}

    public static Bitmap badge(char value) {
        if (value == MARK) return badge();
        TtcuzProfileSync.Metadata data;
        synchronized (marks) { data = styles.get(value); }
        if (data == null || data.badgeUrl == null || data.badgeUrl.length() == 0)
            return null;
        final String url = data.badgeUrl;
        synchronized (remoteImages) {
            Bitmap ready = remoteImages.get(url);
            if (ready != null) return ready;
            Long failedUntil = failedImages.get(url);
            if (failedUntil != null && failedUntil > System.currentTimeMillis())
                return null;
            if (!fetching.add(url)) return null;
        }
        Net.away("profile badge", () -> {
            boolean loaded = false;
            try {
                Context context = Margy.context();
                File cached = context == null ? null : remoteFile(context, url);
                boolean fresh = cached != null && cached.isFile()
                        && cached.length() <= 256 * 1024
                        && System.currentTimeMillis() - cached.lastModified() < 86400000L;
                byte[] raw = fresh ? Net.read(cached) : Net.bytes(url);
                if (raw == null || raw.length > 256 * 1024 || !imageFormat(raw)) return;
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(raw, 0, raw.length, bounds);
                if (bounds.outWidth < 1 || bounds.outHeight < 1
                        || bounds.outWidth > 256 || bounds.outHeight > 256) return;
                Bitmap picture = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                if (!fresh && picture != null && cached != null) Net.save(cached, raw);
                if (picture != null) synchronized (remoteImages) {
                    if (remoteImages.size() >= 128) remoteImages.clear();
                    remoteImages.put(url, picture);
                    failedImages.remove(url);
                    loaded = true;
                }
            } catch (Throwable error) {
                Diary.note("profile badge download: " + error);
            } finally {
                synchronized (remoteImages) {
                    if (!loaded) {
                        if (failedImages.size() >= 128) failedImages.clear();
                        failedImages.put(url, System.currentTimeMillis() + 30000L);
                    }
                    fetching.remove(url);
                }
            }
        });
        return null;
    }

    public static int badgeSize(char mark) {
        if (mark == MARK) return badgeSize();
        synchronized (marks) { TtcuzProfileSync.Metadata data = styles.get(mark);
            return data == null ? 100 : data.badgeSize; }
    }

    public static int offsetX(char mark) {
        if (mark == MARK) return offsetX();
        synchronized (marks) { TtcuzProfileSync.Metadata data = styles.get(mark);
            return data == null ? 0 : data.offsetX; }
    }
    public static int offsetY(char mark) {
        if (mark == MARK) return offsetY();
        synchronized (marks) { TtcuzProfileSync.Metadata data = styles.get(mark);
            return data == null ? 0 : data.offsetY; }
    }

    private static boolean imageFormat(byte[] raw) {
        boolean png = raw.length >= 8 && raw[0] == (byte) 0x89
                && raw[1] == 0x50 && raw[2] == 0x4E && raw[3] == 0x47;
        boolean webp = raw.length >= 12 && raw[0] == 'R' && raw[1] == 'I'
                && raw[2] == 'F' && raw[3] == 'F'
                && raw[8] == 'W' && raw[9] == 'E'
                && raw[10] == 'B' && raw[11] == 'P';
        return png || webp;
    }

    private static File remoteFile(Context context, String url) throws Exception {
        byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                .digest(url.getBytes("UTF-8"));
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < 16; i++)
            name.append(String.format(java.util.Locale.US, "%02x", hash[i] & 0xFF));
        return new File(context.getFilesDir(), "ttcuz/profile-cache/" + name + ".img");
    }

    /** A cached shader per span, created only when the text size changes. */
    public static final class Gradient extends CharacterStyle implements UpdateAppearance {
        private final char mark;
        private final float width;
        private float size;
        private Shader shader;
        private boolean wasAnimated;
        private int revision = -1;
        private final Matrix matrix = new Matrix();

        public Gradient(char mark, float width) {
            this.mark = mark;
            this.width = Math.max(1f, width);
        }

        @Override public void updateDrawState(TextPaint paint) {
            boolean moving = ProfileStyle.animated(mark);
            int currentRevision = ProfileStyle.revision();
            if (shader == null || size != paint.getTextSize() || wasAnimated != moving
                    || revision != currentRevision) {
                size = paint.getTextSize();
                wasAnimated = moving;
                revision = currentRevision;
                shader = ProfileStyle.shader(mark, width, size);
            }
            if (moving) {
                float period = Math.max(2000f, 14000f - speed(mark) * 110f);
                float travel = ((android.os.SystemClock.uptimeMillis() % (long) period)
                        / period) * width * 2f;
                int flow = direction(mark);
                if (flow == 1 || flow == 2) matrix.setTranslate(0f, -travel);
                else matrix.setTranslate(-travel, 0f);
                shader.setLocalMatrix(matrix);
            } else {
                shader.setLocalMatrix(null);
            }
            paint.setShader(shader);
            if (ProfileStyle.bold(mark))
                paint.setTypeface(android.graphics.Typeface.create(paint.getTypeface(),
                        android.graphics.Typeface.BOLD));
            if (ProfileStyle.glow(mark)) paint.setShadowLayer(1.25f, 0, 0,
                    ProfileStyle.start(mark));
        }
    }

    /** Optional nickname weight and restrained halo without changing its colour. */
    public static final class Decoration extends CharacterStyle implements UpdateAppearance {
        private final char mark;
        public Decoration(char mark) { this.mark = mark; }
        @Override public void updateDrawState(TextPaint paint) {
            if (ProfileStyle.bold(mark)) paint.setTypeface(android.graphics.Typeface.create(
                    paint.getTypeface(), android.graphics.Typeface.BOLD));
            if (ProfileStyle.glow(mark)) paint.setShadowLayer(1.25f, 0, 0,
                    paint.getColor());
        }
    }
}
