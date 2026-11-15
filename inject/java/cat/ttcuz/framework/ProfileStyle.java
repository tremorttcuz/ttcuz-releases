package cat.ttcuz.framework;

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
    private static final String COLOURS="profile_gradient_colours";
    public static final int MAX_COLOURS=8;
    private static volatile int[] cachedColours;
    private static volatile String coloursOwner;
    private static final String THREE = "profile_gradient_three";
    private static final String WIDTH = "profile_gradient_width";
    private static final String STOP = "profile_gradient_stop";
    private static final String DIRECTION = "profile_gradient_direction";
    private static final String BOLD = "profile_nickname_bold";
    private static final String GLOW = "profile_nickname_glow";
    private static final String GLOW_STRENGTH = "profile_nickname_glow_strength";
    private static final String ANIMATED = "profile_gradient_animated";
    private static final String SPEED = "profile_gradient_speed";
    private static final String MOTION = "profile_gradient_motion";
    /** How an animated gradient moves: flowing along, swaying side to side, breathing, spinning, a passing shine. */
    public static final int FLOW = 0, SWAY = 1, BREATHE = 2, SPIN = 3, SHINE = 4;
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
        Context context = Ttcuz.context();
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
                || bold() || glow() || !NicknameFont.chosen().isEmpty();
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

    public static int[] gradientColours() {
        String owner=Account.liveId();
        int[] known=cachedColours;
        if(known!=null && java.util.Objects.equals(owner,coloursOwner))return known.clone();
        int[] values=null;SharedPreferences p=prefs();
        try {
            String raw=p==null?"":p.getString(COLOURS,"");
            if(!raw.isEmpty()) {
                org.json.JSONArray array=new org.json.JSONArray(raw);
                if(array.length()>=2 && array.length()<=MAX_COLOURS) {
                    values=new int[array.length()];
                    for(int i=0;i<values.length;i++)values[i]=0xFF000000|array.getInt(i);
                }
            }
        } catch(Exception ignored) {}
        if(values==null)values=threeColours()?new int[]{start(),middle(),end()}:new int[]{start(),end()};
        coloursOwner=owner;cachedColours=values;return values.clone();
    }
    public static void setGradientColours(int[] colours) {
        if(colours==null || colours.length<2 || colours.length>MAX_COLOURS)throw new IllegalArgumentException("2–8 colours required");
        int[] values=colours.clone();org.json.JSONArray array=new org.json.JSONArray();
        for(int i=0;i<values.length;i++){values[i]=0xFF000000|values[i];array.put(values[i]);}
        SharedPreferences p=prefs();
        if(p!=null)p.edit().putString(COLOURS,array.toString()).putInt(START,values[0]).putInt(END,values[values.length-1])
                .putInt(MIDDLE,values[values.length/2]).putBoolean(THREE,values.length>=3).apply();
        coloursOwner=Account.liveId();cachedColours=values;changed();
    }
    static int[] gradientColours(char mark) {
        if(mark==MARK) {
            if(!gradient()){int c=AppearanceColors.get(AppearanceColors.NICKNAME);return new int[]{c,c};}
            if(cachedColours==null || !java.util.Objects.equals(coloursOwner,Account.liveId()))gradientColours();return cachedColours;
        }
        synchronized(marks) {
            TtcuzProfileSync.Metadata data=styles.get(mark);
            if(data!=null && data.gradientColours!=null)return data.gradientColours;
        }
        return threeColours(mark)?new int[]{start(mark),middle(mark),end(mark)}:new int[]{start(mark),end(mark)};
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
        cachedColours=null;coloursOwner=null;
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
        if(p!=null)p.edit().remove(COLOURS).apply();cachedColours=null;changed();
    }

    public static int middle() {
        SharedPreferences p = prefs();
        return p == null ? 0xFFFE2C55 : p.getInt(MIDDLE, 0xFFFE2C55);
    }

    public static void setMiddle(int colour) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putInt(MIDDLE, 0xFF000000 | (colour & 0xFFFFFF)).apply();
        if(p!=null)p.edit().remove(COLOURS).apply();cachedColours=null;changed();
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

    // ------------------------------------------------------------------ entrance, particles, comments, tag

    private static final String APPEAR = "profile_appear", PARTICLES = "profile_particles", COMMENT_COLOUR = "profile_comment_colour", TAG = "profile_tag";
    public static final int APPEAR_NONE = 0, APPEAR_FADE = 1, APPEAR_RISE = 2, APPEAR_POP = 3, APPEAR_TYPE = 4;
    public static final int PARTICLES_NONE = 0, SPARKLES = 1, SNOW = 2, HEARTS = 3, STARS = 4, CUSTOM = 5;
    private static final String PARTICLE_RECIPE = "profile_particle_recipe";
    private static int number(String key, int fallback, int lo, int hi) { SharedPreferences p = prefs(); return p == null ? fallback : Math.max(lo, Math.min(hi, p.getInt(key, fallback))); }
    private static void number(String key, int value) { SharedPreferences p = prefs(); if (p != null) p.edit().putInt(key, value).apply(); changed(); }
    public static int appear() { return number(APPEAR, APPEAR_NONE, 0, 4); }
    public static void setAppear(int value) { number(APPEAR, Math.max(0, Math.min(4, value))); }
    public static int particles() { return number(PARTICLES, PARTICLES_NONE, 0, 5); }
    public static void setParticles(int value) { number(PARTICLES, Math.max(0, Math.min(5, value))); }
    /** The person's own effect (ParticleRecipe JSON), used when particles() is CUSTOM; "" when none was made. */
    public static String particleRecipe() { SharedPreferences p = prefs(); return p == null ? "" : p.getString(PARTICLE_RECIPE, ""); }
    static void setParticleRecipe(String json) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putString(PARTICLE_RECIPE, json == null ? "" : json).putInt(PARTICLES, CUSTOM).apply();
        changed();
    }
    /** The colour of the person's own comments as everybody with the mod sees them; 0 is TikTok's own. */
    public static int commentColour() { SharedPreferences p = prefs(); return p == null ? 0 : p.getInt(COMMENT_COLOUR, 0); }
    public static void setCommentColour(int colour) { number(COMMENT_COLOUR, colour == 0 ? 0 : 0xFF000000 | colour); }
    public static String tag() { SharedPreferences p = prefs(); return p == null ? "" : p.getString(TAG, ""); }
    /** The tag the server confirmed for this account. */
    static void setTag(String value) { SharedPreferences p = prefs(); if (p != null) p.edit().putString(TAG, value == null ? "" : value).apply(); changed(); }
    private static volatile boolean recalled;
    /** Once per run, while this phone has no tag: asks the server whether the account holds one. */
    static void recallTag(final android.content.Context context) {
        if (recalled || !tag().isEmpty()) return;
        recalled = true;
        final String uid = Account.id();
        if (uid == null || !Ids.uid(uid)) return;
        Net.away("recall tag", () -> { try { new CloudProfileProvider(context).fetch(uid); } catch (Throwable ignored) { } });
    }

    /** The server's record of this account's tag (after a reinstall, on another phone): taken back without a publish. */
    static void restoreTag(String value) {
        SharedPreferences p = prefs();
        if (p == null || value == null || value.equals(tag())) return;
        p.edit().putString(TAG, value).apply();
    }

    private static TtcuzProfileSync.Metadata remote(char mark) { synchronized (marks) { return styles.get(mark); } }
    static int appear(char mark) { if (mark == MARK) return appear(); TtcuzProfileSync.Metadata d = remote(mark); return d == null ? 0 : d.appearEffect; }
    static int particles(char mark) { if (mark == MARK) return particles(); TtcuzProfileSync.Metadata d = remote(mark); return d == null ? 0 : d.particles; }
    static String particleRecipe(char mark) { if (mark == MARK) return particleRecipe(); TtcuzProfileSync.Metadata d = remote(mark); return d == null || d.particleRecipe == null ? "" : d.particleRecipe; }
    static String tag(char mark) { if (mark == MARK) return tag(); TtcuzProfileSync.Metadata d = remote(mark); return d == null || d.tag == null ? "" : d.tag; }
    /** The comment colour of an account (own or someone else's with the mod), 0 when none. */
    static int commentColour(String uid) {
        if (uid == null) return 0;
        if (owns(uid)) return commentColour();
        TtcuzProfileSync.Metadata d = TtcuzProfileSync.cached(uid);
        return d == null ? 0 : d.commentColour;
    }

    public static int motion() {
        SharedPreferences p = prefs();
        return p == null ? FLOW : Math.max(0, Math.min(4, p.getInt(MOTION, FLOW)));
    }

    public static void setMotion(int value) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putInt(MOTION, Math.max(0, Math.min(4, value))).apply();
        changed();
        refreshAnimations();
    }

    static int motion(char mark) {
        if (mark == MARK) return motion();
        synchronized (marks) { TtcuzProfileSync.Metadata data = styles.get(mark); return data == null ? FLOW : data.gradientMotion; }
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
        if(p!=null)p.edit().remove(COLOURS).apply();cachedColours=null;changed();
    }

    public static void reset() {
        cachedColours=null;coloursOwner=null;
        SharedPreferences p = prefs();
        if (p != null) p.edit().remove(MODE).remove(START).remove(END).remove(MIDDLE)
                .remove(THREE).remove(COLOURS).remove(DIRECTION).remove(WIDTH).remove(STOP).remove(BOLD).remove(GLOW).remove(GLOW_STRENGTH)
                .remove(ANIMATED).remove(SPEED).remove(MOTION).remove(APPEAR).remove(PARTICLES).remove(COMMENT_COLOUR).remove(CROWN_OFFSET_X)
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

    /** An animated badge: the GIF people see, and its uncropped source (a GIF as chosen, or made from a video). */
    private static File gifFile(Context context) { return AccountAppearance.file(context,"profile-badge.gif"); }
    private static File gifSource(Context context) { return AccountAppearance.file(context,"profile-badge-source.gif"); }
    /** What the server takes for an animated badge. */
    static final int GIF_MAX = 110 * 1024;
    private static final int GIF_SIDE = 96;
    private static volatile Gif.Frames ownFrames;

    /** The own badge's frames when it is animated, else null. */
    public static Gif.Frames frames() {
        Gif.Frames known = ownFrames;
        if (known != null) return known;
        Context context = Ttcuz.context();
        if (context == null) return null;
        File gif = gifFile(context);
        if (!gif.isFile() || gif.length() > GIF_MAX) return null;
        try { return ownFrames = Gif.decode(Net.read(gif), 0, 50, 50, 100); } catch (Throwable error) { Diary.note("profile gif: " + error); return null; }
    }

    /** The cropped animation from its source: a small GIF for everyone, its first frame as the still picture. */
    private static boolean renderAnimated(Context context, byte[] source) throws Exception {
        Gif.Frames frames = Gif.decode(source, GIF_SIDE, cropX(), cropY(), cropZoom());
        if (frames == null) return false;
        byte[] gif = Gif.encode(frames, GIF_MAX);
        if (gif == null) return false;
        File destination = gifFile(context);
        File temporary = new File(destination.getParentFile(), "profile-badge-gif.tmp");
        Net.save(temporary, gif);
        if (!temporary.renameTo(destination)) { temporary.delete(); return false; }
        ownFrames = Gif.decode(gif, 0, 50, 50, 100);
        return true;
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
        Context context = Ttcuz.context();
        if (context == null) return;
        Net.away("profile crop", () -> {
            try {
                Bitmap original = BitmapFactory.decodeFile(
                        sourceFile(context).getAbsolutePath());
                File animation = gifSource(context);
                if (animation.isFile() && !renderAnimated(context, Net.read(animation))) Diary.note("profile crop: animation failed");
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
        Context context = Ttcuz.context();
        if (context == null) return null;
        File animated = gifFile(context);
        if (animated.isFile() && animated.length() <= GIF_MAX) return animated;
        File candidate = file(context);
        return candidate.isFile() ? candidate : null;
    }

    public static boolean hasBadge() {
        Context context = Ttcuz.context();
        return context != null && file(context).isFile();
    }

    public static Bitmap badge() {
        Bitmap known = image;
        if (known != null) return known;
        Context context = Ttcuz.context();
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
            String type = context.getContentResolver().getType(uri);
            if (type != null && type.startsWith("video/") || looksLikeVideo(context, uri)) return takeVideo(context, uri, owner);
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
            if (Gif.isGif(bytes)) return takeAnimation(context, bytes, owner);
            dropAnimation(context);
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

    /** A GIF chosen as the badge: kept as its source, then cropped like a picture. */
    private static boolean takeAnimation(Context context, byte[] gif, String owner) throws Exception {
        Gif.Frames whole = Gif.decode(gif, 0, 50, 50, 100);
        if (whole == null) return false;
        // the source is kept small: a GIF of at most 256 px a side
        byte[] source = gif;
        if (Math.max(whole.width(), whole.height()) > 256 || gif.length > MAX_SOURCE) {
            float k = 256f / Math.max(whole.width(), whole.height());
            Bitmap[] small = new Bitmap[whole.frames.length];
            for (int i = 0; i < small.length; i++) small[i] = Bitmap.createScaledBitmap(whole.frames[i], Math.max(16, Math.round(whole.width() * k)), Math.max(16, Math.round(whole.height() * k)), true);
            source = Gif.encode(new Gif.Frames(small, whole.frameMs), MAX_SOURCE);
            if (source == null) return false;
        }
        return keepAnimation(context, source, whole.frames[0], owner);
    }

    /**
     * Some file pickers name a video application/octet-stream or nothing at all: its first bytes say what it is
     * (an MP4/MOV "ftyp" box, a WebM/MKV header, a 3GP).
     */
    private static boolean looksLikeVideo(Context context, Uri uri) {
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) return false;
            byte[] head = new byte[12];
            int got = 0, n;
            while (got < head.length && (n = input.read(head, got, head.length - got)) > 0) got += n;
            if (got < 12) return false;
            if (head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p') {
                // HEIC and AVIF pictures are "ftyp" boxes too
                String brand = new String(head, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
                return !brand.startsWith("hei") && !brand.startsWith("hev") && !brand.equals("mif1")
                        && !brand.equals("msf1") && !brand.startsWith("avi");
            }
            return (head[0] & 0xFF) == 0x1A && (head[1] & 0xFF) == 0x45 && (head[2] & 0xFF) == 0xDF && (head[3] & 0xFF) == 0xA3;
        } catch (Throwable error) {
            return false;
        }
    }

    /** A video chosen as the badge: its first three seconds become a GIF. */
    private static boolean takeVideo(Context context, Uri uri, String owner) throws Exception {
        Gif.Frames frames = Gif.fromVideo(context, uri, 0, 50, 50, 100, 3000);
        if (frames == null) return false;
        byte[] source = Gif.encode(frames, MAX_SOURCE);
        return source != null && keepAnimation(context, source, frames.frames[0], owner);
    }

    private static boolean keepAnimation(Context context, byte[] source, Bitmap still, String owner) throws Exception {
        synchronized (AccountAppearance.class) {
            if (!owner.equals(AccountAppearance.scope(context))) return false;
            File kept = gifSource(context);
            if (kept.getParentFile() != null) kept.getParentFile().mkdirs();
            Net.save(kept, source);
            File stillSource = sourceFile(context);
            FileOutputStream output = new FileOutputStream(stillSource);
            try { still.compress(Bitmap.CompressFormat.PNG, 100, output); } finally { output.close(); }
            if (!renderAnimated(context, source)) { dropAnimation(context); return false; }
            return renderCrop(context, still);
        }
    }

    private static void dropAnimation(Context context) {
        ownFrames = null;
        gifFile(context).delete();
        gifSource(context).delete();
    }

    public static void removeBadge() {
        Context context = Ttcuz.context();
        if (context == null) return;
        image = null;
        dropAnimation(context);
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
        if (uid==null || !Ids.uid(uid)) return "";
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

    /** Nickname glow strength, 0..100 (50 by default). */
    public static int glowStrength() {
        SharedPreferences prefs = prefs();
        return prefs == null ? 50 : Math.max(0, Math.min(100, prefs.getInt(GLOW_STRENGTH, 50)));
    }

    public static void setGlowStrength(int value) {
        SharedPreferences prefs = prefs();
        if (prefs != null) prefs.edit().putInt(GLOW_STRENGTH, Math.max(0, Math.min(100, value))).apply();
        changed();
    }

    static int glowStrength(char value) {
        if (value == MARK) return glowStrength();
        synchronized (marks) {
            TtcuzProfileSync.Metadata data = styles.get(value);
            return data == null ? 50 : data.glowStrength;
        }
    }

    /** The halo of a nickname: its radius and opacity follow the chosen strength. */
    static void halo(TextPaint paint, char mark, int colour) {
        int strength = glowStrength(mark);
        if (strength <= 0) return;
        paint.setShadowLayer(paint.getTextSize() * (strength * .002f + .04f), 0, 0,
                (colour & 0xFFFFFF) | ((strength * 150 / 100 + 45) << 24));
    }

    public static boolean glow(char value) {
        if (value == MARK) return glow();
        synchronized (marks) {
            TtcuzProfileSync.Metadata data = styles.get(value);
            return data != null && data.glow;
        }
    }

    /** The gradient's line {x0, y0, x1, y1} for a nickname of this size: its direction, stretched by its width. */
    static float[] line(char value, float width, float height) {
        int direction = direction(value);
        float x0 = 0, y0 = 0, x1 = Math.max(1f, width), y1 = 0;
        if (direction == 1) { x1 = 0; y1 = Math.max(1f, height); }
        else if (direction == 2) y1 = Math.max(1f, height);
        else if (direction == 3) { x0 = Math.max(1f, width); x1 = 0; y1 = Math.max(1f, height); }
        float scale=gradientWidth(value)/100f;
        float cx=(x0+x1)*0.5f,cy=(y0+y1)*0.5f;
        return new float[]{cx+(x0-cx)*scale, cy+(y0-cy)*scale, cx+(x1-cx)*scale, cy+(y1-cy)*scale};
    }

    public static android.graphics.Shader shader(char value, float width, float height) {
        float[] l = line(value, width, height);
        float x0 = l[0], y0 = l[1], x1 = l[2], y1 = l[3];
        int[] colors = gradientColours(value);
        float[] positions=new float[colors.length];
        for(int i=0;i<positions.length;i++)positions[i]=i/(float)(positions.length-1);
        if(colors.length==3)positions[1]=gradientStop(value)/100f;
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
                Context context = Ttcuz.context();
                File cached = context == null ? null : remoteFile(context, url);
                boolean fresh = cached != null && cached.isFile()
                        && cached.length() <= 256 * 1024
                        && System.currentTimeMillis() - cached.lastModified() < 86400000L;
                byte[] raw = fresh ? Net.read(cached) : Net.bytes(url);
                if (raw == null || raw.length > 256 * 1024 || !imageFormat(raw)) return;
                if (Gif.isGif(raw)) {
                    // an animated badge: cut into frames once, at the size badges are drawn at
                    Gif.Frames frames = Gif.decode(raw, 0, 50, 50, 100);
                    if (frames == null || frames.width() > 256 || frames.height() > 256) return;
                    if (!fresh && cached != null) Net.save(cached, raw);
                    synchronized (remoteImages) {
                        if (remoteImages.size() >= (MemoryGuard.tight() ? 32 : 128)) remoteImages.clear();
                        remoteImages.put(url, frames.frames[0]);
                        remoteFrames.put(url, frames);
                        failedImages.remove(url);
                        loaded = true;
                    }
                    Badge.refreshProfiles();
                    return;
                }
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(raw, 0, raw.length, bounds);
                if (bounds.outWidth < 1 || bounds.outHeight < 1
                        || bounds.outWidth > 256 || bounds.outHeight > 256) return;
                Bitmap picture = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                if (!fresh && picture != null && cached != null) Net.save(cached, raw);
                if (picture != null) synchronized (remoteImages) {
                    if (remoteImages.size() >= (MemoryGuard.tight() ? 32 : 128)) remoteImages.clear();
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
        return png || webp || Gif.isGif(raw);
    }

    static void trimMemory() { synchronized (remoteImages) { remoteImages.clear(); remoteFrames.clear(); } }

    /** About 12 MB of animated badges at most (3 MB on a small heap); the least recently shown go first. */
    private static final Map<String, Gif.Frames> remoteFrames = new java.util.LinkedHashMap<String, Gif.Frames>(16, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Gif.Frames> eldest) {
            int total = 0;
            for (Gif.Frames f : values()) total += f.bytes();
            return total > MemoryGuard.budget(12 * 1024 * 1024) && size() > 1;
        }
    };

    /** A badge's frames when it is animated (own or someone else's), else null. */
    public static Gif.Frames frames(char value) {
        if (value == MARK) return frames();
        TtcuzProfileSync.Metadata data;
        synchronized (marks) { data = styles.get(value); }
        if (data == null || data.badgeUrl == null) return null;
        synchronized (remoteImages) { return remoteFrames.get(data.badgeUrl); }
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
        private float[] line;
        private int lineRevision = -1;
        private float lineSize;

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
                if (line == null || lineRevision != currentRevision || lineSize != size) { line = ProfileStyle.line(mark, width, size); lineRevision = currentRevision; lineSize = size; }
                move(matrix, line, ProfileStyle.motion(mark), speed(mark));
                shader.setLocalMatrix(matrix);
            } else {
                shader.setLocalMatrix(null);
            }
            paint.setShader(shader);
            if (ProfileStyle.bold(mark))
                paint.setTypeface(android.graphics.Typeface.create(paint.getTypeface(),
                        android.graphics.Typeface.BOLD));
            if (ProfileStyle.glow(mark)) halo(paint, mark, ProfileStyle.start(mark));
        }
    }

    /**
     * Where the gradient is at this moment. Every motion closes its loop exactly: the gradient repeats mirrored, so
     * moving it by twice its own length along its own line (whatever the direction and the width) lands on the frame
     * it started from -- no jump at the end of a round, at any setting.
     */
    static void move(Matrix matrix, float[] line, int motion, int speed) {
        float dx = line[2] - line[0], dy = line[3] - line[1];
        float cx = (line[0] + line[2]) / 2f, cy = (line[1] + line[3]) / 2f;
        float base = Math.max(2000f, 14000f - speed * 110f);
        // the calmer motions take longer for one round at the same speed
        float period = motion == SPIN ? base * 1.5f : motion == SHINE ? base * 1.2f : base;
        float t = (android.os.SystemClock.uptimeMillis() % (long) period) / period;
        double wave = Math.sin(2 * Math.PI * t);
        matrix.reset();
        switch (motion) {
            case SWAY: matrix.setTranslate((float) (dx * .5 * wave), (float) (dy * .5 * wave)); break;
            case BREATHE: { float k = (float) (1 + .35 * wave); matrix.setScale(k, k, cx, cy); break; }
            case SPIN: matrix.setRotate(360f * t, cx, cy); break;
            case SHINE: {
                // a quick pass, then a rest: eased so it starts and stops softly
                float p = Math.min(1f, t / .45f);
                float eased = p * p * (3 - 2 * p);
                matrix.setTranslate(-2 * dx * eased, -2 * dy * eased);
                break;
            }
            default: matrix.setTranslate(-2 * dx * t, -2 * dy * t);
        }
    }

    /** Optional nickname weight and restrained halo without changing its colour. */
    public static final class Decoration extends CharacterStyle implements UpdateAppearance {
        private final char mark;
        public Decoration(char mark) { this.mark = mark; }
        @Override public void updateDrawState(TextPaint paint) {
            if (ProfileStyle.bold(mark)) paint.setTypeface(android.graphics.Typeface.create(
                    paint.getTypeface(), android.graphics.Typeface.BOLD));
            if (ProfileStyle.glow(mark)) halo(paint, mark, paint.getColor());
        }
    }
}
