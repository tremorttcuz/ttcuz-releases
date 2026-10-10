package cat.narezany.margyt;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.ss.android.ugc.aweme.profile.model.User;
import com.ss.android.ugc.profile.platform.base.data.UserProfileInfo;

/**
 * A mark after a name, wherever that name is written.
 *
 * There is no screen to patch for this. TikTok writes an account's name in a
 * dozen places -- the profile, every comment, the line under a video -- and
 * every one of them asks the same model the same question. So the answer
 * carries the mark: `getNickname()` comes back with one invisible character on
 * the end, and every one of those places puts it on screen without knowing.
 *
 * Then the text is caught on its way into a TextView and the character is
 * swapped for a picture. Nothing about the layout changes: an ImageSpan takes
 * the place of a character, so the name is measured and wrapped exactly as the
 * app intended.
 *
 * Which badge it is comes from `Badges`, which reads it out of the repository
 * rather than out of this file -- and the character itself says which one,
 * because by the time a view has the text there is nothing left to ask.
 */
public final class Badge {

    private Badge() {}

    /** The colour of the ttcuz emblem when a badge names none. */
    private static final int MINT = 0xFFFFFFFF;

    private static volatile Drawable note;
    private static volatile boolean noteTried;

    // ------------------------------------------------- where the name lands

    public static String getUniqueId(User user){
        if(user==null)return null;String handle=user.getUniqueId();RecentProfiles.remember(user.getUid(),handle);return handle;
    }
    public static String getNickname(User user) {
        if (user == null) return null;
        Account.rememberProfile(user);
        return marked(user.getNickname(), user.getUid(), false);
    }

    public static String getDisplayName(Object user) { return chatName(user,"getDisplayName"); }
    public static String getNickName(Object user) { return chatName(user,"getNickName"); }
    private static String chatName(Object user,String method) {
        if (user==null) return null;
        String original=null;
        try {
            Object value=user.getClass().getMethod(method).invoke(user);
            original=value instanceof String ? (String)value : null;
            String uid=(String)user.getClass().getMethod("getUid").invoke(user);
            return uid!=null && uid.matches("[0-9]{1,24}") ? marked(original,uid,false) : original;
        } catch (Throwable error) {return original;}
    }

    /** The same name, off the model a loaded profile uses instead. */
    public static String getNickname(UserProfileInfo user) {
        if (user == null) return null;
        Account.rememberProfile(user);
        return marked(user.getNickname(), user.getUid(), true);
    }

    /** Render the settings preview through the same mark/span path as TikTok. */
    private static final char HEADER_MARK='\u2064';
    private static final int PREVIEW_TAG=0x54544350;
    private static volatile HeaderMetrics header;
    private static final class HeaderMetrics {
        String uid; float size,spacing,scale,lineExtra,lineMultiplier; boolean padding; int lines,width,left,top,right,bottom,gravity;
        android.graphics.Typeface font;
        android.text.TextUtils.TruncateAt ellipsis;
        HeaderMetrics(TextView view,String uid) {
            this.uid=uid;width=view.getWidth();left=view.getPaddingLeft();top=view.getPaddingTop();right=view.getPaddingRight();bottom=view.getPaddingBottom();gravity=view.getGravity();size=view.getTextSize();font=view.getTypeface();spacing=view.getLetterSpacing();
            scale=view.getTextScaleX();lineExtra=view.getLineSpacingExtra();lineMultiplier=view.getLineSpacingMultiplier();
            padding=view.getIncludeFontPadding();lines=view.getMaxLines();ellipsis=view.getEllipsize();
        }
    }
    private static void captureHeader(TextView view,String uid) {
        if (Boolean.TRUE.equals(view.getTag(PREVIEW_TAG))) return;
        watchHeader(view);
        if (!uid.equals(Account.id())) return;
        view.post(() -> {
            if (!view.isAttachedToWindow()) return;
            HeaderMetrics next=new HeaderMetrics(view,uid), previous=header;
            header=next;
            if (previous==null || !uid.equals(previous.uid) || previous.size!=next.size
                    || previous.font!=next.font || previous.spacing!=next.spacing
                    || previous.width!=next.width || previous.left!=next.left || previous.top!=next.top || previous.right!=next.right || previous.bottom!=next.bottom || previous.gravity!=next.gravity
                    || previous.padding!=next.padding || previous.lines!=next.lines
                    || previous.scale!=next.scale || previous.lineExtra!=next.lineExtra || previous.lineMultiplier!=next.lineMultiplier)
                ProfilePreview.accountUpdated();
        });
    }
    private static final int HEADER_WATCH_TAG=0x54544357;
    private static void watchHeader(TextView view){
        if(Boolean.TRUE.equals(view.getTag(HEADER_WATCH_TAG)))return;
        view.setTag(HEADER_WATCH_TAG,Boolean.TRUE);
        final java.lang.ref.WeakReference<TextView> held=new java.lang.ref.WeakReference<>(view);
        view.addTextChangedListener(new android.text.TextWatcher(){
            boolean queued,repairing;
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){}
            public void afterTextChanged(android.text.Editable s){
                if(queued || repairing)return;
                if(s.getSpans(0,s.length(),ProfileStyle.Gradient.class).length>0
                        || s.getSpans(0,s.length(),ProfileStyle.Decoration.class).length>0
                        || s.getSpans(0,s.length(),MovableBadgeSpan.class).length>0)return;
                TextView target=held.get();if(target==null)return;
                ProfileBinding binding; synchronized(profileBindings){binding=profileBindings.get(target);}
                if(binding==null || !binding.header)return;
                String plain=strip(s.toString());
                if(!matchesProfile(binding,plain))return;
                String expected=decorate(binding.name,binding.uid);
                if(expected.equals(plain))return;
                // Marked text is already rendered by our setText hook; repair only native resets.
                if(!s.toString().equals(plain))return;
                queued=true;target.post(()->{
                    queued=false;TextView current=held.get();if(current==null || !current.isAttachedToWindow())return;
                    synchronized(profileBindings){if(profileBindings.get(current)!=binding)return;}
                    if(!plain.equals(strip(current.getText().toString())))return;
                    repairing=true;try{current.setText(marked(current,expected),TextView.BufferType.SPANNABLE);}finally{repairing=false;}
                });
            }
        });
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            public void onViewAttachedToWindow(View v){refreshProfiles();}
            public void onViewDetachedFromWindow(View v){}
        });
    }
    private static void previewMetrics(TextView view,String uid) {
        HeaderMetrics source=header;
        if (source==null || uid==null || !uid.equals(source.uid)) return;
        view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,source.size);
        view.setTypeface(source.font);view.setLetterSpacing(source.spacing);
        view.setIncludeFontPadding(source.padding);view.setMaxLines(source.lines);
        view.setEllipsize(source.ellipsis);view.setTextScaleX(source.scale);
        view.setLineSpacing(source.lineExtra,source.lineMultiplier);
        view.setPadding(source.left,source.top,source.right,source.bottom);
        view.setGravity(source.gravity);
        // The preview scrolls horizontally rather than wrapping the nickname
        // into a narrower avatar column and moving its badge to another line.
        if(source.width>0)view.setWidth(source.width);
    }

    static void previewNickname(TextView view, String name, String uid) {
        if (view == null || name == null || name.length() == 0) return;
        view.setTag(PREVIEW_TAG,Boolean.TRUE);
        previewMetrics(view,uid);
        try {
            CharSequence drawn = marked(view, decorate(name, uid));
            view.setText(drawn, TextView.BufferType.SPANNABLE);
        } catch (Throwable error) {
            Diary.note("profile preview spans: " + error);
            view.setText(Plugins.name(uid, strip(name)));
        }
    }

    /**
     * What a name becomes: a word in front of it, and marks after it.
     *
     * Done once and not twice. A profile is built out of the model the feed
     * already had, so by the time the profile's own model is asked for the
     * name it is handing back a name this has already been through -- and
     * marking it again gave everybody two badges and two prefixes.
     */
    private static String marked(String name, String uid, boolean fromProfile) {
        if (name == null || name.length() == 0) return name;
        try {
            // Cleared first, always. Whatever marks are on the way in are
            // either ones the mod put there a moment ago -- in which case
            // adding them again is what gave everybody two badges -- or ones
            // somebody typed into their own name to wear a badge they were
            // never given. Neither survives; only what the account is owed is
            // put back.
            String own = ProfileStyle.displayName(uid,Plugins.name(uid,Account.canonicalName(uid,strip(name))));
            remember(uid, strip(name), fromProfile);
            String profile = ProfileStyle.mark(uid);
            String marks = Badges.marksFor(uid);
            if (profile.length() > 0 || marks.length() > 0)
                return own + ' ' + profile + marks + (fromProfile ? String.valueOf(HEADER_MARK) : "");
            return own;
        } catch (Throwable ignored) {
            return name;
        }
    }

    // ------------------------------------------- the name a profile just read

    /**
     * The one name worth putting back, and only for a moment.
     *
     * A profile writes its name again when it has finished loading, by a road
     * the mod does not stand on, and the mark goes with it. The first attempt
     * at fixing that remembered every name the mod had ever seen and put the
     * mark back on any text that matched one -- which marked the word in a
     * comment, in a bio, anywhere somebody wrote a name that happened to be
     * somebody's. That is not where a badge belongs.
     *
     * So what is kept is one name, the one a profile asked for in the last few
     * seconds, and nothing older. A badge still cannot appear anywhere except
     * where TikTok asked for a nickname -- it only survives the profile
     * finishing its work.
     */
    private static volatile String lastName;
    private static volatile String lastUid;
    private static volatile long lastAt;

    /** The actual profile header's text view; TikTok sometimes writes its raw
     * nickname into that same view again after its model has been decorated. */
    private static final java.util.Map<TextView, ProfileBinding> profileBindings =
            new java.util.WeakHashMap<TextView, ProfileBinding>();

    /**
     * Identity of any visible native profile header in this window, for when the header's own view
     * does not hold the name (TikTok splits the header over several components).
     */
    static String profileUidIn(View root) {
        if (root == null) return null;
        synchronized (profileBindings) {
            for (java.util.Map.Entry<TextView, ProfileBinding> entry : profileBindings.entrySet()) {
                View name = entry.getKey(); ProfileBinding binding = entry.getValue();
                if (name != null && binding != null && binding.header && ProfileLayout.onScreen(name, root)) return binding.uid;
            }
        }
        return null;
    }

    /** Identity from a visible native header, never the last visited profile. */
    static String profileUid(View anchor) {
        if (anchor == null) return null;
        synchronized (profileBindings) {
            for (java.util.Map.Entry<TextView, ProfileBinding> entry : profileBindings.entrySet()) {
                View name = entry.getKey(); ProfileBinding binding = entry.getValue();
                if (name == null || binding == null || !binding.header
                        || !ProfileLayout.onScreen(name, anchor.getRootView())) continue;
                for (View at = name; at != null;) {
                    if (at == anchor) return binding.uid;
                    Object parent = at.getParent(); at = parent instanceof View ? (View) parent : null;
                }
            }
        }
        return null;
    }

    private static final class ProfileBinding {
        final String name;
        final String uid;
        final String originalName;
        final boolean header;
        ProfileBinding(String name, String uid) { this(name,uid,true); }
        ProfileBinding(String name, String uid,boolean header) {
            this.name = name; this.uid = uid; this.header=header;
            this.originalName=header && uid.equals(lastUid) && lastName!=null
                    ?lastName:Account.canonicalName(uid,name);
        }
    }
    private static boolean matchesProfile(ProfileBinding binding,String plain){
        return binding.name.equals(plain) || binding.originalName.equals(plain)
                || strip(decorate(binding.name,binding.uid)).equals(plain)
                || strip(Account.canonicalName(binding.uid,binding.name)).equals(plain);
    }

    private static final long RECENT = 30000;

    /**
     * Only a profile's own model counts.
     *
     * A name read off `User` is read everywhere -- under every video, beside
     * every comment -- and remembering those is what put a badge on the word
     * somebody typed into a comment. `UserProfileInfo` is asked for a name by
     * one screen and one screen only.
     */
    private static void remember(String uid, String name, boolean fromProfile) {
        if (!fromProfile || uid == null || name == null || name.length() == 0) return;
        try {
            // Remember the real profile header even before its public style
            // has arrived. The first fetch is asynchronous, so otherwise the
            // nickname view has nothing to repaint when that response lands.
            lastName = strip(name);
            lastUid = uid;
            lastAt = android.os.SystemClock.uptimeMillis();
        } catch (Throwable ignored) {
        }
    }

    /** Put the mark back on the profile name that was read a moment ago. */
    public static void rewrite(View root) {
        String name = lastName;
        String uid = lastUid;
        if (root == null || name == null || uid == null) return;
        if (android.os.SystemClock.uptimeMillis() - lastAt > RECENT) return;
        try {
            seen = 0;
            walk(root, 0, name, uid);
        } catch (Throwable ignored) {
        }
    }

    /**
     * How deep to go, and how much to do at once.
     *
     * A profile or the inbox is a fragment inside a pager inside a list inside
     * a coordinator, and twenty levels is not unusual -- fourteen reached a
     * comment sheet and stopped well short of those.
     */
    private static final int DEEP = 40;
    private static final int BUDGET = 4000;

    private static boolean inAList(View view) {
        String name = view.getClass().getName();
        return name.contains("RecyclerView") || name.contains("ListView")
                || name.contains("ViewPager");
    }

    private static int seen;

    private static void walk(View view, int depth, String name, String uid) {
        if (view == null || depth > DEEP || ++seen > BUDGET) return;
        // A list is where the mod has no business rewriting text: a comment,
        // a message, a search result is somebody's words, and a word that
        // happens to be a name is still their word. A profile's own name is
        // not in a list -- it is in the header above one.
        if (inAList(view)) return;
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            CharSequence showing = text.getText();
            if (showing != null && name.contentEquals(showing)) {
                String out = marked(showing.toString(), uid, false);
                if (!out.equals(showing.toString())) setText(text, out);
            }
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            int many = group.getChildCount();
            for (int i = 0; i < many; i++) walk(group.getChildAt(i), depth + 1, name, uid);
        }
    }

    /**
     * Anything being typed, cleared of the characters a badge is made of.
     *
     * A mark is invisible, so one that ends up in a box is one nobody can see
     * to delete: edit a name that has a badge on it and the mark comes with
     * it, and what gets saved is a name with somebody's badge inside it --
     * after which the mod adds its own and there are two. Every box is watched
     * as well as cleared, because text can arrive in one by being pasted.
     */
    private static CharSequence typed(TextView view, CharSequence text) {
        watch(view);
        if (text == null) return text;
        String cleaned = strip(text.toString());
        return cleaned.equals(text.toString()) ? text : cleaned;
    }

    private static void watch(TextView view) {
        if (Boolean.TRUE.equals(view.getTag(WATCHED))) return;
        try {
            view.setTag(WATCHED, Boolean.TRUE);
            view.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {
                }

                @Override
                public void onTextChanged(CharSequence s, int a, int b, int c) {
                }

                @Override
                public void afterTextChanged(android.text.Editable text) {
                    try {
                        for (int i = text.length() - 1; i >= 0; i--) {
                            if ((Badges.isPrivate(text.charAt(i)) || ProfileStyle.isMark(text.charAt(i)) || text.charAt(i)==HEADER_MARK)) text.delete(i, i + 1);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    // another key of its own: "Marh" + 1
    private static final int WATCHED = 0x4D61726B;

    /**
     * Every piece of text on its way into a TextView passes here.
     *
     * Which is a great many of them, so the common case is a type check and a
     * scan of a short string, and nothing else.
     */
    public static void setText(TextView view, CharSequence text) {
        FeedCaptions.speedLabel(view,text);
        Fonts.apply(view);
        if (view instanceof android.widget.EditText) {
            view.setText(typed(view, text));
            return;
        }
        CharSequence out = marked(view, text);
        // asking for it to be kept spannable, because a TextView told to store
        // plain text copies the spans into an immutable SpannedString and the
        // tap has nothing left to find
        if (out != text) {
            view.setText(out, TextView.BufferType.SPANNABLE);
        } else {
            view.setText(out);
        }
        Accent.historyText(view);
    }

    public static void setText(TextView view, CharSequence text, TextView.BufferType type) {
        FeedCaptions.speedLabel(view,text);
        Fonts.apply(view);
        if (view instanceof android.widget.EditText) {
            view.setText(typed(view, text), type);
            return;
        }
        CharSequence out = marked(view, text);
        view.setText(out, out != text ? TextView.BufferType.SPANNABLE : type);
        Accent.historyText(view);
    }

    private static CharSequence marked(TextView view, CharSequence text) {
        if (text == null) {
            synchronized (profileBindings) {profileBindings.remove(view);}
            return text;
        }
        String identity=null;
        for (int i=text.length()-1;i>=0 && identity==null;i--)
            if (ProfileStyle.isMark(text.charAt(i))) identity=ProfileStyle.uidForMark(text.charAt(i));
        if (identity!=null && !Boolean.TRUE.equals(view.getTag(PREVIEW_TAG))) {
            String clean=strip(text.toString());
            synchronized (profileBindings) {
                ProfileBinding previous=profileBindings.get(view);
                boolean isHeader=text.toString().indexOf(HEADER_MARK)>=0
                        || (previous!=null && previous.header && identity.equals(previous.uid));
                profileBindings.put(view,new ProfileBinding(clean,identity,isHeader));
            }
            if(text.toString().indexOf(HEADER_MARK)>=0)captureHeader(view,identity);
        } else {
            synchronized (profileBindings) {
                ProfileBinding old=profileBindings.get(view);
                if (old!=null && !old.header) profileBindings.remove(view);
            }
        }

        boolean any = false;
        for (int i = text.length() - 1; i >= 0 && !any; i--) {
            any = Badges.isMark(text.charAt(i))
                    || ProfileStyle.isMark(text.charAt(i));
        }
        if (!any) {
            rememberProfileView(view, text);
            ProfileBinding binding = null;
            synchronized (profileBindings) { binding = profileBindings.get(view); }
            if (binding != null && binding.header
                    && (matchesProfile(binding,strip(text.toString()))
                        || (binding.uid.equals(Account.liveId()) && Account.profileHandle()!=null
                            && Account.profileHandle().equalsIgnoreCase(strip(text.toString()))))) {
                String restored = decorate(binding.name, binding.uid);
                if (!restored.equals(text.toString())) return marked(view, restored);
            }
            return text;
        }

        try {
            rememberProfileView(view, text);
            // Anything that is text, not only a plain String. A profile writes
            // the name once while it loads and again when it is loaded, and
            // the second time it is rich text -- which is why the badge used
            // to appear on the way in and then vanish: the mark was still
            // there, invisible, and nothing turned it into a picture.
            //
            // Walked backwards so that dropping a mark cannot move the ones
            // not yet looked at.
            SpannableStringBuilder out = new SpannableStringBuilder(text);
            for(int index=out.length()-1;index>=0;index--)
                if(out.charAt(index)==HEADER_MARK)out.delete(index,index+1);
            boolean drew = false;
            // Every badge written next to this name: they are all one account's,
            // and a badge object is shared by everyone who holds it, so the
            // owner can only be read from the name itself, not looked up.
            java.util.List<Badges.Badge> theirs = new java.util.ArrayList<Badges.Badge>();
            for (int i = 0; i < out.length(); i++) {
                if (!Badges.isMark(out.charAt(i))) continue;
                Badges.Badge one = Badges.byMark(out.charAt(i));
                if (one != null && !Badges.isCreator(one) && !theirs.contains(one))
                    theirs.add(one);
            }
            char placementMark=0;
            for(int p=0;p<out.length();p++)if(ProfileStyle.isMark(out.charAt(p))){placementMark=out.charAt(p);break;}
            for (int i = out.length() - 1; i >= 0; i--) {
                char c = out.charAt(i);
                if (ProfileStyle.isMark(c)) {
                    if (ProfileStyle.hasGradient(c) && i > 0) {
                        float width = view.getPaint().measureText(out, 0, i);
                        out.setSpan(new ProfileStyle.Gradient(c, width), 0, i,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        ProfileStyle.watchAnimation(view, c);
                    } else if (ProfileStyle.hasDecoration(c) && i > 0) {
                        out.setSpan(new ProfileStyle.Decoration(c), 0, i,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    Drawable custom = profilePicture(view, c);
                    if (custom == null) {
                        out.delete(i, i + 1);
                    } else {
                        out.setSpan(new MovableBadgeSpan(custom, view, c, false), i, i + 1,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        drew = true;
                    }
                    continue;
                }
                if (!Badges.isMark(c)) continue;
                Badges.Badge badge = Badges.byMark(c);
                Drawable picture = badge == null ? null : picture(view, badge);
                if (picture == null) {
                    out.delete(i, i + 1);
                    continue;
                }
                if (Badges.isCreator(badge)) {
                    out.setSpan(new MovableBadgeSpan(picture, view, c, true), i, i + 1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    if(placementMark!=0){
                        android.graphics.Rect bounds=picture.getBounds();int size=ProfileStyle.badgeSize(placementMark);
                        picture.setBounds(0,0,Math.max(1,bounds.width()*size/100),Math.max(1,bounds.height()*size/100));
                    }
                    out.setSpan(placementMark==0?new ImageSpan(picture,ImageSpan.ALIGN_BOTTOM):new MovableBadgeSpan(picture,view,placementMark,false), i, i + 1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                out.setSpan(new Tap(badge, theirs), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                drew = true;
            }
            if (!drew && out.getSpans(0, out.length(), ProfileStyle.Gradient.class).length == 0
                    && out.getSpans(0, out.length(), ProfileStyle.Decoration.class).length == 0)
                return tidy(out.toString());
            listen(view);
            return out;
        } catch (Throwable ignored) {
            // a name with a stray invisible character is bad; a name that
            // crashes the screen it is on is worse
            return strip(text.toString());
        }
    }

    private static void rememberProfileView(TextView view, CharSequence text) {
        if (view == null || text==null || Boolean.TRUE.equals(view.getTag(PREVIEW_TAG))) return;
        String name = lastName;
        String uid = lastUid;
        if (name == null || uid == null
                || android.os.SystemClock.uptimeMillis() - lastAt > RECENT) return;
        if (!name.contentEquals(strip(text.toString())) || insideList(view)) return;
        synchronized (profileBindings) {
            if (profileBindings.size() > 256) profileBindings.clear();
            profileBindings.put(view, new ProfileBinding(name, uid));
        }
        captureHeader(view,uid);
    }

    /** Reapply newly downloaded public styles and badges to profiles already open. */
    private static boolean profileRefreshQueued;
    public static void refreshProfiles() {
        synchronized(Badge.class){if(profileRefreshQueued)return;profileRefreshQueued=true;}
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->{
            synchronized(Badge.class){profileRefreshQueued=false;}
            refreshProfilesNow();
        },16L);
    }
    private static void refreshProfilesNow() {
        final java.util.ArrayList<java.util.Map.Entry<TextView, ProfileBinding>> pending =
                new java.util.ArrayList<java.util.Map.Entry<TextView, ProfileBinding>>();
        synchronized (profileBindings) {
            for (java.util.Map.Entry<TextView, ProfileBinding> entry : profileBindings.entrySet()) {
                TextView view = entry.getKey();
                ProfileBinding binding = entry.getValue();
                if (view != null && binding != null) {
                    pending.add(new java.util.AbstractMap.SimpleImmutableEntry<TextView, ProfileBinding>(
                            view, binding));
                }
            }
        }
        for (final java.util.Map.Entry<TextView, ProfileBinding> entry : pending) {
            final TextView view = entry.getKey();
            final ProfileBinding binding = entry.getValue();
            view.post(new Runnable() {
                @Override public void run() {
                    synchronized (profileBindings) {
                        if (profileBindings.get(view) != binding) return;
                    }
                    if (!view.isAttachedToWindow()) return;
                    CharSequence showing = view.getText();
                    if (showing == null || !matchesProfile(binding,strip(showing.toString())))
                        return;
                    String value = decorate(binding.name, binding.uid);
                    CharSequence drawn = marked(view, value);
                    if (drawn != null)
                        view.setText(drawn, TextView.BufferType.SPANNABLE);
                }
            });
        }
    }

    private static boolean insideList(View view) {
        View current = view;
        for (int depth = 0; current != null && depth < 40; depth++) {
            Object parent = current.getParent();
            if (!(parent instanceof View)) return false;
            current = (View) parent;
            String type = current.getClass().getName();
            if (type.contains("RecyclerView") || type.contains("ListView")) return true;
        }
        return false;
    }

    private static String decorate(String name, String uid) {
        String own = ProfileStyle.displayName(uid,Plugins.name(uid,Account.canonicalName(uid,strip(name))));
        String profile = ProfileStyle.mark(uid);
        String marks = Badges.marksFor(uid);
        return profile.length() > 0 || marks.length() > 0
                ? own + ' ' + profile + marks : own;
    }

    /**
     * The name with nothing of the private-use area left in it.
     *
     * Not only the marks this run happens to use: the whole area. A name is
     * text somebody chose, and none of that text has any business being an
     * invisible character that the mod might one day draw as a picture.
     */
    private static String strip(String plain) {
        boolean any = false;
        for (int i = 0; i < plain.length() && !any; i++) {
            any = Badges.isPrivate(plain.charAt(i)) || ProfileStyle.isMark(plain.charAt(i)) || plain.charAt(i)==HEADER_MARK;
        }
        if (!any) return tidy(plain);

        StringBuilder out = new StringBuilder(plain.length());
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (!Badges.isPrivate(c) && !ProfileStyle.isMark(c) && c!=HEADER_MARK) out.append(c);
        }
        return tidy(out.toString());
    }

    /** No dangling separator, for a name whose marks all went away. */
    private static String tidy(String name) {
        return name.endsWith(" ") || name.endsWith("\u2009")
                ? name.substring(0, name.length() - 1) : name;
    }

    private static Drawable profilePicture(TextView view, char mark) {
        Bitmap bitmap = ProfileStyle.badge(mark);
        if (bitmap == null) return null;
        Drawable picture = new BitmapDrawable(view.getResources(), bitmap);
        int size = Math.max(1, Math.round(view.getTextSize()
                * ProfileStyle.badgeSize(mark) / 100f));
        picture.setBounds(0, 0, size, size);
        return picture;
    }

    /** Find an adjustable mark in the live profile-name preview. */
    static int previewMarkAt(TextView view, float x, float y) {
        if (view == null || !(view.getText() instanceof Spanned)) return 0;
        Layout layout = view.getLayout();
        if (layout == null) return 0;
        Spanned text = (Spanned) view.getText();
        MovableBadgeSpan[] spans = text.getSpans(0, text.length(), MovableBadgeSpan.class);
        // Touch coordinates include TextView compound padding and scrolling;
        // Layout/span coordinates do not. Use the same origin as Android draws.
        x += view.getScrollX() - view.getTotalPaddingLeft();
        y += view.getScrollY() - view.getTotalPaddingTop();
        float padding = 6f * view.getResources().getDisplayMetrics().density;
        for (MovableBadgeSpan span : spans) {
            int start = text.getSpanStart(span);
            int end = text.getSpanEnd(span);
            if (start < 0 || end <= start) continue;
            int line = layout.getLineForOffset(start);
            float a = layout.getPrimaryHorizontal(start);
            float b = layout.getPrimaryHorizontal(end);
            float top = layout.getLineTop(line) + span.dy - padding;
            float bottom = layout.getLineBottom(line) + span.dy + padding;
            if (x >= Math.min(a, b) + span.dx - padding
                    && x <= Math.max(a, b) + span.dx + padding
                    && y >= top && y <= bottom)
                return span.crown ? 2 : 1;
        }
        return 0;
    }

    /** Move a preview mark as the finger moves; persist its position on release. */
    static void movePreviewMark(TextView view, int kind, int x, int y) {
        if (view == null || !(view.getText() instanceof Spanned)) return;
        Spanned text = (Spanned) view.getText();
        MovableBadgeSpan[] spans = text.getSpans(0, text.length(), MovableBadgeSpan.class);
        float density = view.getResources().getDisplayMetrics().density;
        for (MovableBadgeSpan span : spans) {
            if (span.crown == (kind == 2)) span.setOffset(x, y, density);
        }
        view.requestLayout();
        view.invalidate();
    }

    /** Move the owner's badge within the nickname row without moving its touch target. */
    private static final class MovableBadgeSpan extends ImageSpan {
        private final char mark;
        private final boolean crown;
        private int dx;
        private int dy;
        private float unit;
        private final java.lang.ref.WeakReference<TextView> owner;
        private int revision;

        MovableBadgeSpan(Drawable image, TextView view, int mark, boolean crown) {
            super(image, ImageSpan.ALIGN_BOTTOM);
            this.mark = (char) mark;
            this.crown = crown;
            owner=new java.lang.ref.WeakReference<TextView>(view);
            unit=view.getTextSize()/24f;
            revision=ProfileStyle.revision();
            float density = unit;
            dx = Math.round((crown ? ProfileStyle.crownOffsetX((char) mark)
                    : ProfileStyle.offsetX((char) mark)) * density);
            dy = Math.round((crown ? ProfileStyle.crownOffsetY((char) mark)
                    : ProfileStyle.offsetY((char) mark)) * density);
        }

        void setOffset(int x, int y, float density) {
            dx = Math.round(x * unit);
            dy = Math.round(y * unit);
        }

        private void syncPlacement(){
            TextView view=owner.get();float next=view==null?unit:view.getTextSize()/24f;
            int current=ProfileStyle.revision();if(current==revision&&next==unit)return;revision=current;unit=next;
            dx=Math.round((crown?ProfileStyle.crownOffsetX(mark):ProfileStyle.offsetX(mark))*unit);
            dy=Math.round((crown?ProfileStyle.crownOffsetY(mark):ProfileStyle.offsetY(mark))*unit);
            if(view!=null)view.requestLayout();
        }

        @Override public int getSize(android.graphics.Paint paint, CharSequence text,
                                     int start, int end, android.graphics.Paint.FontMetricsInt fm) {
            syncPlacement();
            int width=getDrawable().getBounds().right;
            if(fm!=null){android.graphics.Paint.FontMetricsInt base=paint.getFontMetricsInt();int height=getDrawable().getBounds().bottom;int center=(base.ascent+base.descent)/2;
                fm.ascent=Math.min(base.ascent,center-height/2+dy);fm.top=Math.min(base.top,fm.ascent);fm.descent=Math.max(base.descent,center+(height+1)/2+dy);fm.bottom=Math.max(base.bottom,fm.descent);
            }
            return Math.max(1, width + Math.max(0, dx));
        }

        @Override public void draw(android.graphics.Canvas canvas, CharSequence text,
                                   int start, int end, float x, int top, int y, int bottom,
                                   android.graphics.Paint paint) {
            syncPlacement();
            android.graphics.Paint.FontMetricsInt metrics=paint.getFontMetricsInt();int height=getDrawable().getBounds().bottom;
            float baseline=y+(metrics.ascent+metrics.descent-height)/2f+dy;canvas.save();canvas.translate(x+dx,baseline);getDrawable().draw(canvas);canvas.restore();
        }
    }

    /** The badge at a size worth looking at, for the window it opens. */
    private static Drawable large(View view, Badges.Badge badge) {
        try {
            if (Badges.isCreator(badge)) return null; // crown removed
            Bitmap bitmap = badge.image.length() == 0
                    ? null : Badges.picture(view.getContext(), badge.image);
            Drawable drawable;
            if (bitmap != null) {
                drawable = new BitmapDrawable(view.getResources(), bitmap);
                if (badge.colour != 0) {
                    drawable.setColorFilter(badge.colour, PorterDuff.Mode.SRC_IN);
                }
            } else {
                Drawable own = note();
                if (own == null) return null;
                drawable = own.getConstantState() == null
                        ? own : own.getConstantState().newDrawable().mutate();
                drawable.setColorFilter(badge.colour != 0 ? badge.colour : MINT,
                        PorterDuff.Mode.SRC_IN);
            }
            return drawable;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Sized to the text it sits in, so it matches whatever draws it. */
    private static Drawable picture(TextView view, Badges.Badge badge) {
        // The creator's crown is no longer drawn; the mark is simply dropped.
        if (Badges.isCreator(badge)) return null;
        Bitmap bitmap = badge.image.length() == 0
                ? null : Badges.picture(view.getContext(), badge.image);

        Drawable drawable;
        if (bitmap != null) {
            drawable = new BitmapDrawable(bitmap);
            if (badge.colour != 0) drawable.setColorFilter(badge.colour, PorterDuff.Mode.SRC_IN);
        } else {
            Drawable own = note();
            if (own == null) return null;
            drawable = own.getConstantState() == null
                    ? own : own.getConstantState().newDrawable().mutate();
            drawable.setColorFilter(badge.colour != 0 ? badge.colour : MINT,
                    PorterDuff.Mode.SRC_IN);
        }

        int size = Math.round(view.getTextSize());
        if (size <= 0) size = Math.round(14 * view.getResources().getDisplayMetrics().density);

        // as wide as it is tall only if the drawing is. Trimming a badge to
        // its content leaves a picture of whatever shape that content is --
        // the crown makes the owner's taller than it is wide -- and forcing
        // that into a square stretches it.
        int wide = drawable.getIntrinsicWidth();
        int tall = drawable.getIntrinsicHeight();
        int across = wide > 0 && tall > 0 ? Math.round(size * (float) wide / tall) : size;
        drawable.setBounds(0, 0, Math.max(1, across), size);
        return drawable;
    }

    /** The note that ships with the mod, decoded once. */
    private static Drawable note() {
        Drawable known = note;
        if (known != null) return known;
        if (noteTried) return null;
        noteTried = true;
        try {
            byte[] png = Base64.decode(Emblem.PNG, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(png, 0, png.length);
            if (bitmap == null) return null;
            known = new BitmapDrawable(crop(bitmap));
            note = known;
            return known;
        } catch (Throwable error) {
            Diary.note("badge: " + error);
            return null;
        }
    }

    // ------------------------------------------------------------- the tap

    private static final class Tap extends ClickableSpan {
        private final Badges.Badge badge;
        private final java.util.List<Badges.Badge> all;

        Tap(Badges.Badge badge, java.util.List<Badges.Badge> all) {
            this.badge = badge;
            this.all = all;
        }

        @Override
        public void onClick(View widget) {
            if (showAll(widget)) return;
            Popup.show(popupContext(widget), badge.title, badge.text, badge.button,
                    large(widget, badge));
        }

        /**
         * A tap on one badge opens every badge the same name carries.
         *
         * Only when there is more than one to show; a single badge keeps its
         * own window, with the large picture and its button.
         */
        private boolean showAll(View widget) {
            try {
                if (all == null || all.size() < 2) return false;
                Drawable[] pictures = new Drawable[all.size()];
                String[] titles = new String[all.size()];
                String[] texts = new String[all.size()];
                for (int i = 0; i < pictures.length; i++) {
                    Badges.Badge one = all.get(i);
                    pictures[i] = large(widget, one);
                    titles[i] = one.title;
                    texts[i] = one.text;
                }
                Popup.showList(popupContext(widget), Text.BADGES_OF_ACCOUNT, null,
                        pictures, titles, texts);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }

        @Override
        public void updateDrawState(android.text.TextPaint paint) {
            // no underline, no colour: the picture is the whole of it
        }
    }

    private static android.content.Context popupContext(View view) {
        android.app.Activity current=Screen.now();
        return current!=null && !current.isFinishing()?current:view.getContext();
    }

    /**
     * Take the badge's taps, and only the badge's.
     *
     * A movement method is the usual way and it is the wrong one here. By the
     * time TextView consults it, View.onTouchEvent has already run and already
     * decided a click happened -- so tapping the emblem fired both the emblem
     * and whatever the name does. A touch listener runs before all of that: it
     * swallows the press and the release when they land on the emblem, so the
     * view never sees a click, and answers false everywhere else, so the name
     * goes on behaving exactly as it did.
     */
    private static void listen(TextView view) {
        try {
            Touch touch=view.getTag(LISTENING) instanceof Touch ? (Touch)view.getTag(LISTENING) : new Touch();
            view.setTag(LISTENING,touch);
            view.setOnTouchListener(touch);
        } catch (Throwable ignored) {
        }
    }

    // setTag(int, ...) wants a key that looks like a resource id: "Marg" + 1
    private static final int LISTENING = 0x4D617268;

    /** Preserve native name gestures even when TikTok replaces its listener. */
    public static void setOnTouchListener(View view,View.OnTouchListener nativeListener) {
        if(!(view instanceof TextView)){view.setOnTouchListener(nativeListener);return;}
        Touch touch=view.getTag(LISTENING) instanceof Touch ? (Touch)view.getTag(LISTENING) : new Touch();
        if(nativeListener!=touch)touch.nativeListener=nativeListener;
        view.setTag(LISTENING,touch);
        view.setOnTouchListener(touch);
    }

    private static final class Touch implements View.OnTouchListener {
        private boolean pressed;
        private View.OnTouchListener nativeListener;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                pressed = span(v, event) != null;
                return pressed || (nativeListener!=null && nativeListener.onTouch(v,event));
            }
            if (!pressed) return nativeListener!=null && nativeListener.onTouch(v,event);
            if (action == MotionEvent.ACTION_UP) {
                pressed = false;
                ClickableSpan tapped = span(v, event);
                if (tapped != null) tapped.onClick(v);
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL) pressed = false;
            return true;
        }

        /** The badge under the finger, or null. */
        private ClickableSpan span(View v, MotionEvent event) {
            try {
                TextView view = (TextView) v;
                CharSequence text = view.getText();
                // Spanned, not Spannable: what comes back out of a TextView is
                // read-only, and asking for the writable interface was why this
                // answered "not mine" to every tap it should have taken
                if (!(text instanceof Spanned)) return null;
                Layout layout = view.getLayout();
                if (layout == null) return null;

                int x = (int) event.getX() - view.getTotalPaddingLeft() + view.getScrollX();
                int y = (int) event.getY() - view.getTotalPaddingTop() + view.getScrollY();
                Spanned spans=(Spanned)text;
                Tap[] found=spans.getSpans(0,text.length(),Tap.class);
                for(Tap tap:found) {
                    int start=spans.getSpanStart(tap),end=spans.getSpanEnd(tap);
                    if(start<0 || end<=start)continue;
                    int line=layout.getLineForOffset(start);
                    float a=layout.getPrimaryHorizontal(start),b=layout.getPrimaryHorizontal(end);
                    int dx=0,dy=0;
                    MovableBadgeSpan[] images=spans.getSpans(start,end,MovableBadgeSpan.class);
                    if(images.length>0){dx=Math.min(0,images[0].dx);dy=images[0].dy;}
                    if(BadgeHit.contains(x,y,Math.min(a,b)+dx,Math.max(a,b)+dx,
                            layout.getLineTop(line)+dy,layout.getLineBottom(line)+dy))return tap;
                }
                return null;
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    /**
     * Cut away what is fully transparent.
     *
     * An adaptive icon's foreground is drawn small inside a large square,
     * because the launcher masks and moves it. Beside a name none of that
     * applies and the empty margin is just a smaller emblem.
     */
    static Bitmap crop(Bitmap bitmap) {
        int width = bitmap.getWidth(), height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        int left = width, top = height, right = -1, bottom = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((pixels[y * width + x] >>> 24) < 8) continue;
                if (x < left) left = x;
                if (x > right) right = x;
                if (y < top) top = y;
                if (y > bottom) bottom = y;
            }
        }
        if (right < left || bottom < top) return bitmap;
        return Bitmap.createBitmap(bitmap, left, top, right - left + 1, bottom - top + 1);
    }
}
