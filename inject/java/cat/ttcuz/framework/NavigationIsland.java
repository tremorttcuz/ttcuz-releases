package cat.ttcuz.framework;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.view.animation.PathInterpolator;
import android.widget.*;
import java.lang.ref.WeakReference;
import java.util.*;

/** Restyle the native five-tab row; TikTok keeps clicks, routing, badges and accessibility.
 *  The row's layout is never changed, so the seekbar and the touch area above the bar stay native. */
final class NavigationIsland {
    private static final Map<View,State> states=new WeakHashMap<>();
    private static WeakReference<View> active=new WeakReference<>(null);
    private NavigationIsland() {}
    static boolean isRail(View view){return active.get()==view;}

    /** Settings: the island can be switched off entirely; TikTok's own bar then comes back exactly as it was. */
    public static final String KEY="navigation_island";
    private static volatile Boolean on;
    static boolean isEnabled(){
        Boolean known=on;if(known!=null)return known;
        android.content.Context context=Ttcuz.context();if(context==null)return true;
        boolean value=true;try{value=context.getSharedPreferences(Ttcuz.PREFS,android.content.Context.MODE_PRIVATE).getBoolean(KEY,true);}catch(Throwable ignored){}
        on=value;return value;
    }
    static void setEnabled(boolean value){
        on=value;
        android.content.Context context=Ttcuz.context();
        if(context!=null)context.getSharedPreferences(Ttcuz.PREFS,android.content.Context.MODE_PRIVATE).edit().putBoolean(KEY,value).apply();
        // Applied on the next frame of each screen: off restores the native bar, on searches for it again.
        for(State state:new ArrayList<>(states.values())){if(!value)state.restoreNative();else state.nextSearch=0;View root=state.root.get();if(root!=null)root.invalidate();}
    }
    static void reloadSettings(){on=null;labelsHidden=null;smooth=null;setEnabled(isEnabled());}
    /** Settings: the island's motion at 120 frames a second where the screen can show them (on by default). */
    static final String KEY_FAST="island_120hz";
    private static volatile Boolean smooth;
    static boolean fast(){
        Boolean known=smooth;if(known!=null)return known;
        if(!Ttcuz.is64())return smooth=false;
        android.content.Context context=Ttcuz.context();if(context==null)return true;
        boolean value=context.getSharedPreferences(Ttcuz.PREFS,android.content.Context.MODE_PRIVATE).getBoolean(KEY_FAST,true);
        smooth=value;return value;
    }
    static void setFast(boolean value){
        smooth=value;
        android.content.Context context=Ttcuz.context();
        if(context!=null)context.getSharedPreferences(Ttcuz.PREFS,android.content.Context.MODE_PRIVATE).edit().putBoolean(KEY_FAST,value).apply();
    }
    /** Settings: only the icons on the island, no tab names (the unread counts stay). */
    static final String KEY_NO_LABELS="island_hide_labels";
    private static volatile Boolean labelsHidden;
    static boolean hideLabels(){
        Boolean known=labelsHidden;if(known!=null)return known;
        android.content.Context context=Ttcuz.context();if(context==null)return false;
        boolean value=context.getSharedPreferences(Ttcuz.PREFS,android.content.Context.MODE_PRIVATE).getBoolean(KEY_NO_LABELS,false);
        labelsHidden=value;return value;
    }
    static void setHideLabels(boolean value){
        labelsHidden=value;
        android.content.Context context=Ttcuz.context();
        if(context!=null)context.getSharedPreferences(Ttcuz.PREFS,android.content.Context.MODE_PRIVATE).edit().putBoolean(KEY_NO_LABELS,value).apply();
        for(State state:new ArrayList<>(states.values())){if(state.background!=null){for(int i=0;i<5;i++){state.background.captionFrom[i]=null;state.background.said[i]=null;}state.background.invalidateSelf();}View root=state.root.get();if(root!=null)root.invalidate();}
    }
    /** The icons changed (Material 3 or TikTok's): every island collects its tabs again on its next frame. */
    static void recollect(){
        for(State state:new ArrayList<>(states.values())){state.structure=0;state.nextRebuild=0;View root=state.root.get();if(root!=null)root.invalidate();}
    }
    /** True while the island hands a tap to TikTok's own tab, so the tap does not come back to the island. */
    static boolean replaying;
    private static boolean ownKnown;
    static void watch(Activity activity) {
        if(!ownKnown){ownKnown=true;IslandFiles.afterCrash(activity);ownIsland=IslandFiles.active();}
        // the feed's scroll effect goes on the main screen's feeds
        try{FeedScroll.attach(activity);}catch(Throwable error){Diary.note("feed scroll: "+error);}
        if(!activity.getClass().getName().equals("com.ss.android.ugc.aweme.main.MainActivity"))return;
        View root=activity.getWindow().getDecorView();State state=states.get(root);
        if(state==null){state=new State(root);states.put(root,state);}
        state.attach();
        IslandTouch.hook(activity);
    }
    static void stop(Activity activity){State state=states.get(activity.getWindow().getDecorView());if(state!=null)state.detach();}
    static void destroy(Activity activity){State state=states.remove(activity.getWindow().getDecorView());if(state!=null){state.detach();if(active.get()==state.rail.get())active.clear();}}
    private static State stateOf(Activity activity){return states.get(activity.getWindow().getDecorView());}
    /** Hold-and-drag of the selection indicator (driven by IslandTouch; screen coordinates in, nothing native touched). */
    static boolean hit(Activity activity,float x,float y){State state=stateOf(activity);return state!=null&&state.hit(x,y);}
    static void begin(Activity activity,float x){State state=stateOf(activity);if(state!=null)state.begin(x);}
    /** A tap was released on the island (screen coordinates): a tab tapped sends the drop on its way at once. */
    static void tap(Activity activity,float x,float y){State state=stateOf(activity);if(state!=null)state.tap(x,y);}
    /** True when the nearest tab changed (the caller may tick). */
    static boolean move(Activity activity,float x){State state=stateOf(activity);return state!=null&&state.move(x);}
    /** Screen centre of the tab to tap, or null when the native selection already matches. */
    static float[] end(Activity activity,boolean commit){State state=stateOf(activity);return state==null?null:state.end(commit);}
    static float stepOverride=0f;
    /** The drop of glass, its glass plus and its icon-only pills are the Liquid Glass style's; the others keep theirs. */
    static boolean dropStyle(){return Blur.style()==Blur.LIQUID&&ownIsland==null;}
    /** An island file chosen in the settings (IslandFiles), or null: then it draws everything and gets the touches on the bar. */
    static volatile cat.ttcuz.island.Island ownIsland;
    /** Another island file was chosen (or none): every island settles and is drawn by whoever draws now. */
    static void painterChanged(){
        cat.ttcuz.island.Island next=IslandFiles.active();
        for(State state:new ArrayList<>(states.values())){
            if(state.background==null)continue;
            if(next!=ownIsland)state.background.leaveOwn();
            state.background.settle();
            View root=state.root.get();if(root!=null)root.invalidate();
        }
        ownIsland=next;
    }
    /** A touch on the bar while an island file draws it: null when it is not on the bar (the usual way then), else taken. */
    static Boolean ownTouch(Activity activity,MotionEvent event,java.util.function.Consumer<MotionEvent> deliver){
        // the tap the island itself sends to TikTok's tab comes back through the window: it is TikTok's, not the island's
        if(replaying)return null;
        State state=stateOf(activity);if(state==null||state.background==null)return null;
        return state.background.touch(event);
    }
    static int role(String text) {
        if(text==null)return -1;String label=text.trim().toLowerCase(Locale.ROOT);
        if(label.equals("главная")||label.equals("home")||label.equals("for you")||label.equals("accueil")||label.equals("pour toi")||label.equals("головна"))return 0;
        if(label.equals("сообщество")||label.equals("community")||label.equals("друзья")||label.equals("friends")||label.equals("amis")||label.equals("communauté")||label.equals("communaute")||label.equals("découvrir")||label.equals("decouvrir")||label.equals("друзі")||label.equals("explore"))return 1;
        if(label.equals("входящие")||label.equals("inbox")||label.equals("boîte de réception")||label.equals("boite de reception")||label.equals("messages")||label.equals("вхідні"))return 3;
        if(label.equals("профиль")||label.equals("profile")||label.equals("profil")||label.equals("профіль"))return 4;
        return -1;
    }
    static int role(View view,int depth) {
        if(depth>5)return -1;
        int found=role(view instanceof TextView?String.valueOf(((TextView)view).getText()):String.valueOf(view.getContentDescription()));
        if(found>=0)return found;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){found=role(group.getChildAt(i),depth+1);if(found>=0)return found;}}
        return -1;
    }
    private static boolean selected(View view,int depth) {
        if(view.isSelected()||view.isActivated())return true;
        if(depth<5&&view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(selected(group.getChildAt(i),depth+1))return true;}
        return false;
    }
    private static ArrayList<View> cells(ViewGroup rail) {
        ArrayList<View> cells=new ArrayList<>();
        for(int i=0;i<rail.getChildCount();i++){View child=rail.getChildAt(i);if(child.getVisibility()==View.VISIBLE&&child.getWidth()>0)cells.add(child);}
        int count=cells.size();if(count<3||count>5)return null;
        // TikTok's own bar is Home, Friends, create, Inbox, Profile; IslandTabs may have hidden or moved some of them.
        // Every cell is one of the named tabs, at most one is the create button, and Home and Profile are always there.
        int mask=0,unnamed=0;float last=Float.NEGATIVE_INFINITY,widest=rail.getWidth()*.29f*5f/count;
        for(View cell:cells){if(cell.getWidth()>widest||cell.getHeight()<36*rail.getResources().getDisplayMetrics().density||cell.getX()<last)return null;last=cell.getX();int role=role(cell,0);
            if(role<0)unnamed++;else if((mask&1<<role)!=0)return null;else mask|=1<<role;}
        return unnamed<=1&&(mask&17)==17&&Integer.bitCount(mask)+unnamed==count?cells:null;
    }
    private static ViewGroup find(View view,int[] top,int height,int width,int depth,int[] budget) {
        if(depth>22||budget[0]--<=0||!view.isShown()||!(view instanceof ViewGroup))return null;
        ViewGroup group=(ViewGroup)view;int[] at=new int[2];view.getLocationOnScreen(at);
        if(at[1]+view.getHeight()<top[1]+height*.70f)return null;
        float unit=view.getResources().getDisplayMetrics().density;
        if(view.getWidth()>width*.75f&&view.getHeight()>=48*unit&&view.getHeight()<=120*unit
           &&at[1]+view.getHeight()/2f>top[1]+height*.80f&&cells(group)!=null)return group;
        for(int i=0;i<group.getChildCount();i++){ViewGroup found=find(group.getChildAt(i),top,height,width,depth+1,budget);if(found!=null)return found;}
        return null;
    }
    private static final class State implements ViewTreeObserver.OnPreDrawListener {
        final WeakReference<View> root;
        WeakReference<ViewGroup> rail=new WeakReference<>(null);
        IslandDrawable background;
        ArrayList<View> tabs;
        boolean attached;
        WeakReference<ViewTreeObserver> observer=new WeakReference<>(null);
        Drawable nativeBackground;
        long nextSearch,nextRebuild;
        /** The IslandTabs version the row was last arranged for. */
        int arranged=-1;
        int structure;
        State(View root){this.root=new WeakReference<>(root);}
        void attach(){View view=root.get();if(view==null)return;ViewTreeObserver next=view.getViewTreeObserver(),old=observer.get();if(attached&&next==old)return;if(old!=null&&old.isAlive())old.removeOnPreDrawListener(this);attached=next.isAlive();if(attached){next.addOnPreDrawListener(this);observer=new WeakReference<>(next);}}
        void detach(){ViewTreeObserver old=observer.get();if(old!=null&&old.isAlive())old.removeOnPreDrawListener(this);attached=false;if(background!=null){background.settle();background.release();}}
        private float[] origin(ViewGroup row){int[] at=new int[2];row.getLocationOnScreen(at);return new float[]{at[0],at[1]};}
        boolean hit(float x,float y){
            ViewGroup row=rail.get();if(row==null||background==null||tabs==null||!row.isShown())return false;
            float[] at=origin(row);return background.hit(x-at[0],y-at[1]);
        }
        void begin(float x){ViewGroup row=rail.get();if(row==null||background==null)return;background.beginDrag(x-origin(row)[0]);}
        void tap(float x,float y){
            ViewGroup row=rail.get();if(row==null||background==null||tabs==null||!row.isShown())return;
            float[] at=origin(row);float lx=x-at[0],ly=y-at[1];
            if(ly<0||ly>row.getHeight())return;
            for(int i=0;i<tabs.size()&&i<5;i++){View cell=tabs.get(i);if(i!=background.create&&lx>=cell.getX()&&lx<cell.getX()+cell.getWidth()){background.go(i);return;}}
        }
        /** The drop's own surface, laid exactly over the island's row in the window. */
        IslandDrop.Flight flight;
        private final int[] here=new int[2],base=new int[2];
        void placeFlight(View decor,ViewGroup row){
            // The lens is drawn by the island itself now (it reads the island live), not on a surface of its own;
            // its parents only let it reach a little past the row, where it swells.
            if(flight!=null){background.flight=null;dropFlight();}
            if(dropStyle())unclip(row);
            else return;
            if(flight==null||flight.getParent()!=decor){
                if(flight!=null&&flight.getParent() instanceof ViewGroup)((ViewGroup)flight.getParent()).removeView(flight);
                flight=new IslandDrop.Flight(decor.getContext());
                ((FrameLayout)decor).addView(flight,new FrameLayout.LayoutParams(1,1));
            }
            row.getLocationInWindow(here);decor.getLocationInWindow(base);
            int pad=Math.round(flight.pad),left=here[0]-base[0],top=here[1]-base[1]-pad,w=row.getWidth(),h=row.getHeight()+2*pad;
            FrameLayout.LayoutParams at=(FrameLayout.LayoutParams)flight.getLayoutParams();
            if(at.width!=w||at.height!=h||at.leftMargin!=left||at.topMargin!=top||at.gravity!=(Gravity.TOP|Gravity.LEFT)){
                at.width=w;at.height=h;at.leftMargin=left;at.topMargin=top;at.gravity=Gravity.TOP|Gravity.LEFT;flight.setLayoutParams(at);
            }
            background.flight=flight;
        }
        /** Parents whose clipping was lifted so the swollen lens may draw past the row, with what they had. */
        final ArrayList<ViewGroup> unclipped=new ArrayList<>();
        void unclip(ViewGroup row){
            if(!unclipped.isEmpty())return;
            Object up=row.getParent();
            for(int i=0;i<3&&up instanceof ViewGroup;i++){ViewGroup g=(ViewGroup)up;if(g.getClipChildren()){g.setClipChildren(false);unclipped.add(g);}up=g.getParent();}
            if(unclipped.isEmpty())unclipped.add(row); // nothing to lift: remember that it was looked at
        }
        void reclip(){for(ViewGroup g:unclipped)if(g!=rail.get())g.setClipChildren(true);unclipped.clear();}
        void dropFlight(){if(flight!=null&&flight.getParent() instanceof ViewGroup)((ViewGroup)flight.getParent()).removeView(flight);flight=null;}
        boolean move(float x){ViewGroup row=rail.get();return row!=null&&background!=null&&background.dragTo(x-origin(row)[0]);}
        float[] end(boolean commit){
            ViewGroup row=rail.get();if(row==null||background==null||tabs==null)return null;
            int index=background.drop(commit);if(!commit||index<0||index>=tabs.size())return null;
            View cell=tabs.get(index);if(selected(cell,0))return null;
            float[] at=origin(row);return new float[]{at[0]+cell.getX()+cell.getWidth()/2f,at[1]+cell.getY()+cell.getHeight()/2f};
        }
        /** Cheap per-frame check of the cached cells: no allocation, no text matching. */
        private boolean intact(ViewGroup row){
            if(tabs==null||tabs.size()<3||IslandTabs.version!=arranged)return false;
            for(int i=0;i<tabs.size();i++){View cell=tabs.get(i);if(cell.getParent()!=row||cell.getVisibility()!=View.VISIBLE||cell.getWidth()<=0)return false;}
            return true;
        }
        /** Puts TikTok's bar back exactly: its background, icons, labels, the create button and badge positions. */
        void restoreNative(){
            ViewGroup row=rail.get();
            if(background!=null){background.leaveOwn();background.settle();background.release();background.unhide();}
            if(row!=null&&row.getBackground()==background&&background!=null)row.setBackground(nativeBackground);
            if(background!=null)background.flight=null;dropFlight();reclip();
            if(row!=null)IslandTabs.restore(row);arranged=-1;
            if(active.get()==row)active.clear();
            FeedUnderBar.restore();
            background=null;tabs=null;nativeBackground=null;rail=new WeakReference<>(null);
        }
        public boolean onPreDraw() {android.os.Trace.beginSection("ttcuz island frame");try{
            View view=root.get();if(view==null||!view.hasWindowFocus())return true;
            if(!isEnabled()){if(background!=null)restoreNative();return true;}
            try {
                ViewGroup row=rail.get();
                if(row==null||!row.isAttachedToWindow()) {
                    long now=android.os.SystemClock.uptimeMillis();if(now<nextSearch)return true;nextSearch=now+1000;
                    int[] at=new int[2];view.getLocationOnScreen(at);
                    row=find(view,at,view.getHeight(),view.getWidth(),0,new int[]{3000});if(row==null)return true;
                    ArrayList<View> found=cells(row);if(found==null)return true;
                    arranged=IslandTabs.version;if(IslandTabs.apply(row)){found=cells(row);if(found==null){IslandTabs.restore(row);found=cells(row);if(found==null)return true;}}
                    tabs=found;nextRebuild=now+1500;
                    rail=new WeakReference<>(row);active=new WeakReference<>(row);
                    // The row's own layout (height, margins, padding, elevation) stays exactly as TikTok made it:
                    // the island is painted inside the native bounds, so nothing climbs over the seekbar and the
                    // touch area above the bar is never taken.
                    nativeBackground=row.getBackground();background=new IslandDrawable(row);row.setBackground(background);background.collect(tabs);structure=structure(row,0);
                    FeedUnderBar.apply(row);
                    Diary.note("navigation: MD3 island attached");
                }
                if(!row.isShown())return true;
                if(!intact(row)) {
                    // the arrangement is put back whenever TikTok undoes it or the settings change it
                    arranged=IslandTabs.version;IslandTabs.apply(row);
                    ArrayList<View> found=cells(row);if(found==null){tabs=null;return true;}
                    tabs=found;background.collect(tabs);structure=structure(row,0);
                }
                if(android.os.SystemClock.uptimeMillis()>=nextRebuild){
                    nextRebuild=android.os.SystemClock.uptimeMillis()+1500;
                    int next=structure(row,0);
                    if(next!=structure){structure=next;background.collect(tabs);FeedUnderBar.apply(row);}
                    else if(IslandMotion.tiktokIcons())background.retrace();
                    FeedScroll.check();
                }
                if(row.getBackground()!=background)row.setBackground(background);
                placeFlight(view,row);
                background.update(tabs);
                background.backdrop(row);
            }catch(RuntimeException error){Diary.note("navigation: "+error);}
            return true;
        }finally{android.os.Trace.endSection();}}
        private static int structure(View view,int depth){
            int hash=System.identityHashCode(view);
            if(depth<6&&view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;hash=31*hash+group.getChildCount();for(int i=0;i<group.getChildCount();i++)hash=31*hash+structure(group.getChildAt(i),depth+1);}
            return hash;
        }
    }
    /** A view inside a native tab, found once per rebuild instead of on every frame. */
    private static final class Part {
        final View view;final int tab;
        Part(View view,int tab){this.view=view;this.tab=tab;}
    }
    /** Google Material Symbols Rounded per tab: outlined when idle, filled when selected (MD3 navigation bar). */
    static final String[] SYMBOLS={"home","group",null,"chat_bubble","person"};
    /** Under-damped spring: a little overshoot, then rest (iOS pop with Material 3 Expressive bounce). */
    private static final class Spring {
        float v,t,vel;final float k,c,eps;
        Spring(float k,float c,float eps){this.k=k;this.c=c;this.eps=eps;}
        void set(float x){v=t=x;vel=0f;}
        boolean step(float dt){
            int n=(int)Math.ceil(dt/.008f);if(n<1)n=1;float h=dt/n;
            // the island's motion choice: stiffer is faster, less damping overshoots more (the ratio to sqrt(k) is kept)
            float sk=IslandMotion.stiffness(),kk=k*sk,cc=c*IslandMotion.damping()*(float)Math.sqrt(sk);
            for(int i=0;i<n;i++){vel+=(kk*(t-v)-cc*vel)*h;v+=vel*h;}
            if(Math.abs(t-v)>eps||Math.abs(vel)>eps*10f)return true;v=t;vel=0f;return false;
        }
    }
    /**
     * The island, drawn entirely here: tonal body (frosted over the blur engine when available), the active pill,
     * Google symbols for every tab, the selected tab's label sliding out to the right of its icon inside the pill,
     * and the create button as an MD3 Sunny with a plus. Native views stay in place for touch, routing, badges and
     * accessibility; only their pixels are hidden (label colour, image alpha), idempotently, so a steady frame writes
     * nothing. Every colour follows the accent; anti-burn-in dims the content but never the body.
     */
    private static final class IslandDrawable extends Drawable {
        final WeakReference<ViewGroup> rail;
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG),ink=new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF body=new RectF(),shape=new RectF(),pill=new RectF(),target=new RectF(),next=new RectF(),from=new RectF(),to=new RectF(),slot=new RectF();
        final Path outline=new Path(),sunny=new Path();
        final ArrayList<Part> parts=new ArrayList<>(),badges=new ArrayList<>();
        final ArrayList<ImageView> small=new ArrayList<>();
        /** What TikTok had before we hid or moved anything, so switching the island off restores it exactly. */
        final Map<TextView,ColorStateList> labelColours=new WeakHashMap<>();
        final Map<View,Float> createAlpha=new WeakHashMap<>();
        final Map<View,float[]> badgeOffset=new WeakHashMap<>();
        /** The main icon of each tab (its largest small ImageView), hidden natively and redrawn as a Google symbol. */
        final ImageView[] icons=new ImageView[5];
        /** The native drawable each traced icon was taken from: TikTok swapping it (another state, an update) is traced again. */
        final Drawable[] tracedFrom=new Drawable[5];
        /** TikTok changed a tab's picture since it was traced: trace it again. True when anything changed. */
        boolean retrace(){
            boolean changed=false;
            for(int i=0;i<n;i++){
                if(tracedFrom[i]==null||icons[i]==null||icons[i].getDrawable()==tracedFrom[i])continue;
                android.graphics.Path own=IslandMotion.trace(icons[i]);
                tracedFrom[i]=icons[i].getDrawable();
                if(own!=null){outlined[i]=filled[i]=own;changed=true;}
            }
            if(changed)invalidateSelf();
            return changed;
        }
        final TextView[] labels=new TextView[5];
        final String[] said=new String[5],shown=new String[5];
        final float[] labelWidth=new float[5],wide=new float[5],shift=new float[5],glyphX=new float[5];
        private final boolean[] pictured=new boolean[5];
        final RectF[] glyphs={new RectF(),new RectF(),new RectF(),new RectF(),new RectF()};
        final Path[] outlined=new Path[5],filled=new Path[5];
        Path plus;float sunnySize=-1f;
        Blur.Backdrop blur;boolean blurTried,blurred,liquid;
        /** Liquid Glass finish drawn at full resolution: the 1 dp specular hairline and the soft lift shadow. */
        final Paint rimPaint=new Paint(Paint.ANTI_ALIAS_FLAG),liftPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF rimRect=new RectF();float rimFor=-1f;int liftFor=1;
        final Path dropPath=new Path();final Paint dropRim=new Paint(Paint.ANTI_ALIAS_FLAG);float dropFor=-1f;
        ValueAnimator animation,dimming;float dimTarget=1f;
        /** The drop: its surface (null until there is one), its painter, when the flight began, and a tapped tab TikTok has not shown yet. */
        IslandDrop.Flight flight;IslandDrop.Painter painter;long flightStart;int asked=-1;long askedUntil;
        final RectF dropTail=new RectF(),dropHead=new RectF(),button=new RectF(),lensRect=new RectF();
        /** The scene the lens drawn on this thread reads, filled again each frame it is drawn. */
        final IslandDrop.Scene hereScene=new IslandDrop.Scene();
        int selected=-1,previous=-1,accent,muted,bodyColour,pillColour,onPill,onAccent,textColour,glassTint,tintMode=-1;
        float progress=1f,lift,pillTop,pillWidth=56f,pillHeight=40f,offset,dim=1f,labelRoom=-1f;
        final float[] centres=new float[5];
        /** How many tabs the bar shows, which of them is the create button (-1: none) and what each one is. */
        int n=5,create=2;final int[] roles={0,1,2,3,4};
        /** Each half of the island (Home/Friends left of the create button, Inbox/Profile right of it): its free span. */
        float leftFrom,leftTo,rightFrom,rightTo;
        final Spring px=new Spring(520f,30f,.05f),grow=new Spring(520f,26f,.002f),pw=new Spring(520f,30f,.05f);
        /** MD3 Expressive touches: the Sunny squeezes on press and turns one petal per tap; a tab shows a pressed state layer. */
        final Spring squeeze=new Spring(700f,28f,.002f),turn=new Spring(180f,18f,.05f),layer=new Spring(900f,60f,.004f);
        boolean createDown;int pressedTab=-1;
        boolean manual,dragging,looping;int near=-1;long last;
        final Runnable loop=new Runnable(){public void run(){frame();}};
        int alpha=255;
        Skin skin;
        ColorStateList iconTint;
        float unit,radius;
        IslandDrawable(ViewGroup rail){this.rail=new WeakReference<>(rail);unit=rail.getResources().getDisplayMetrics().density;}
        void cancel(){ValueAnimator old=animation;animation=null;if(old!=null)old.cancel();}
        void settle(){endDragSurface();cancel();ViewGroup row=rail.get();if(row!=null)row.removeCallbacks(loop);looping=false;manual=false;dragging=false;px.set((target.left+target.right)/2f);pw.set(target.width());grow.set(0f);progress=1f;previous=-1;lift=0f;pill.set(target);if(skin!=null)tint();invalidateSelf();}
        boolean hit(float x,float y){float slop=6*unit;return selected>=0&&x>=pill.left-slop&&x<=pill.right+slop&&y>=pill.top-slop&&y<=pill.bottom+slop;}
        private int nearest(float x){
            int best=0;float gap=Float.MAX_VALUE;
            for(int i=0;i<n;i++){if(i==create)continue;float d=Math.abs(centres[i]-x);if(d<gap){gap=d;best=i;}}
            return best;
        }
        void beginDrag(float x){
            // caught mid-flight: the drag starts from where the lens is, not from where it was going
            if(dropStyle()&&onSurface&&flight!=null&&flight.flying()){if(flight.peek(lensRect)>=0f){float cx=lensRect.centerX(),w=pill.width();pill.set(cx-w/2f,pill.top,cx+w/2f,pill.bottom);}flight.stop();onSurface=false;}
            if(dropStyle()&&lensMotion.live){float cx=lensMotion.x,w=pill.width();pill.set(cx-w/2f,pill.top,cx+w/2f,pill.bottom);lensMotion.live=false;}
            cancel();manual=true;dragging=true;progress=1f;previous=-1;lift=0f;
            // While held the pill is icon-sized; the label slides back out once it lands.
            // Held, the pill shrinks onto the icon under the finger: the drag is measured from that icon, not the wide pill.
            float icon=selected>=0&&selected!=create&&!glyphs[selected].isEmpty()?glyphX[selected]:(pill.left+pill.right)/2f;
            px.set((pill.left+pill.right)/2f);px.t=icon;pw.set(pill.width());pw.t=pillWidth;grow.t=1f;near=selected;offset=x-icon;
            // Liquid Glass: the held lens is drawn on the island's own surface from one picture of the island, so each
            // frame of the drag costs the main thread a few numbers instead of drawing the whole island again
            dragSurface=false;dragPicture=null;
            if(dropStyle()&&flight!=null&&flight.ready()&&IslandDrop.LensShader.available()){
                android.graphics.Rect b=getBounds();
                dragPicture=snapshot.take(Math.max(1,b.width()),Math.max(1,b.height()),Flight_PAD*unit,this::drawPlainGlass,this::drawPlainItems);
                dragGlass=dragPicture;
                dragSurface=dragPicture!=null;
            }
            kick();invalidateSelf();
        }
        /** The drag's lens is on the surface (see beginDrag), and the picture of the island it bends. */
        boolean dragSurface;android.graphics.Bitmap dragPicture;
        /** The island, 3 dp inside its edge: where the lens may be. */
        private final RectF lensRoom=new RectF();
        private RectF lensRoom(){
            float e=3*unit;
            // in flight the drop swells a little past the island's top and bottom (12 % of its height each), as on the
            // iPhone, then shrinks back into it; never past its ends
            float over=body.height()*.12f;
            lensRoom.set(body.left+e,body.top-over,body.right-e,body.bottom+over);
            if(lensRoom.width()<=0||lensRoom.height()<=0)lensRoom.setEmpty();return lensRoom;
        }
        private void heldLens(RectF out){
            float g=Math.max(0f,Math.min(1f,grow.v)),cx=(pill.left+pill.right)/2f,w=pill.width()*(1f+.24f*g*IslandMotion.wide()),h=pillHeight*(1f+.32f*g*IslandMotion.tall());
            if(IslandMotion.round()){RectF room=lensRoom();float fullH=room.isEmpty()?h:Math.min(h,room.height());w=pill.width()+(Math.min(pill.width()*1.1f,fullH*.85f)-pill.width())*g;}
            out.set(cx-w/2f,cy0()-h/2f,cx+w/2f,cy0()+h/2f);
            IslandDrop.inside(out,lensRoom());
        }
        private void endDragSurface(){
            if(!dragSurface)return;
            dragSurface=false;dragPicture=null;
            if(flight!=null)flight.stop();
        }
        boolean dragTo(float x){
            if(!dragging)return false;
            px.t=Math.max(centres[0],Math.min(centres[n-1],x-offset));int n=nearest(px.t);boolean changed=n!=near;near=n;kick();return changed;
        }
        /** Releases the indicator: it springs onto the nearest tab (or back when not committed). Returns that tab. */
        int drop(boolean commit){
            if(!dragging)return -1;
            dragging=false;int n=commit&&near>=0?near:selected;selected=n;
            targetFor(n,target);px.t=(target.left+target.right)/2f;pw.t=target.width();grow.t=0f;kick();return n;
        }
        /** The pill of a tab: icon plus its label, centred on the native tab and kept 6 dp inside the island. */
        private void targetFor(int tab,RectF out){
            float w=wide[tab]>0?wide[tab]:pillWidth,c=dropStyle()?centres[tab]:itemCentre(tab);
            // a capsule never stands past the island's round ends, in any style: no wider than the island allows, and
            // its edges at least 6 dp (3 dp for the lens's capsule) inside the island
            if(body.right>body.left){float edge=(dropStyle()?3:6)*unit;w=Math.min(w,Math.max(0f,body.width()-2*edge));c=Math.max(body.left+edge+w/2f,Math.min(body.right-edge-w/2f,c));}
            out.set(c-w/2f,pillTop,c+w/2f,pillTop+pillHeight);
        }
        private void kick(){
            ViewGroup row=rail.get();if(row==null||looping)return;
            looping=true;last=System.nanoTime();row.postOnAnimation(loop);
        }
        private void frame(){
            ViewGroup row=rail.get();if(row==null||!row.isAttachedToWindow()||!row.hasWindowFocus()){settle();return;}
            long now=System.nanoTime();// real time, up to a quarter of a second: after TikTok stalls a frame the island lands where it should be by now
            // instead of crawling on at 32 ms a frame behind everything else
            float dt=stepOverride>0f?stepOverride:Math.min(.25f,(now-last)/1e9f);last=now;
            boolean busy=false;
            if(manual){
                busy=px.step(dt);busy|=grow.step(dt);busy|=pw.step(dt);
                // Growth is bounded by the island: it widens freely but rises only into the room above the indicator,
                // and it stays 6dp clear of both side edges, so it never touches or leaves the island body.
                float g=Math.max(0f,grow.v),e=Math.min(g,1.1f),w=Math.min(Math.max(0f,body.width()-12*unit),Math.max(0f,pw.v)+18*unit*e+Math.min(26*unit,Math.abs(px.vel)*.035f));
                float room=Math.max(0f,2f*Math.min(pillTop-body.top-3*unit,body.bottom-pillTop-pillHeight-3*unit)),extra=Math.min(7*unit*e,room),h=pillHeight+extra,top=pillTop-extra/2f;
                float cx=px.v;if(body.right>body.left){float edge=6*unit;cx=Math.max(body.left+edge+w/2f,Math.min(body.right-edge-w/2f,cx));}
                pill.set(cx-w/2f,top,cx+w/2f,top+h);
                if(dragSurface&&flight!=null){heldLens(lensRect);flight.hold(lensRect,Math.max(0f,Math.min(1f,grow.v)),dim*alpha/255f,dragPicture,dragGlass,Flight_PAD*unit);}
            }
            busy|=squeeze.step(dt);busy|=turn.step(dt);busy|=layer.step(dt);
            if(skin!=null)tint();
            // a drag on the surface leaves the island as it is: nothing on it changes while the lens moves
            if(!(manual&&dragSurface))invalidateSelf();
            if(busy)row.postOnAnimation(loop);
            else{looping=false;float rest=turn.v%360f;turn.set(rest);if(manual&&!dragging){manual=false;pill.set(target);previous=-1;progress=1f;lift=0f;endDragSurface();}if(skin!=null)tint();invalidateSelf();}
        }
        /** A tab was tapped: the drop leaves now, without waiting for TikTok to show that tab. */
        void go(int tab){
            ViewGroup row=rail.get();
            if(row==null||tab==selected||selected<0||manual||!dropStyle()||!IslandMotion.moves(row.getContext()))return;
            previous=selected;selected=tab;targetFor(tab,target);asked=tab;askedUntil=android.os.SystemClock.uptimeMillis()+900L;
            cancel();move();layout();invalidateSelf();
        }
        /**
         * Sends the lens to the target: on its own surface when there is one, here otherwise. Sent again before it
         * lands (a quick second tap), it only turns: it goes on from where it is, at the speed it has.
         */
        private void move(){
            from.set(pill);to.set(target);progress=0f;
            // On its own surface when it can: the island taken as it is into one GPU picture, which the lens bends on
            // its own thread while TikTok switches pages; here (live) otherwise.
            boolean flying=onSurface&&flight!=null&&flight.flying();
            onSurface=false;
            if(flight!=null&&flight.ready()&&IslandDrop.LensShader.available()){
                android.graphics.Rect b=getBounds();
                // one GPU picture: the glass in its top half, what is on it in the bottom half
                android.graphics.Bitmap picture=snapshot.take(Math.max(1,b.width()),Math.max(1,b.height()),Flight_PAD*unit,this::drawPlainGlass,this::drawPlainItems),under=picture;
                if(picture!=null){
                    if(!flying&&lensMotion.live){from.set(lensRect);lensMotion.live=false;}
                    flight.bounds(lensRoom());flight.fly(from,to,dim*alpha/255f,picture,under,Flight_PAD*unit);onSurface=true;
                }
            }
            if(!onSurface){
                if(flying){flight.peek(lensRect);flight.stop();from.set(lensRect);}
                lensMotion.bounds.set(lensRoom());lensMotion.start(from.centerX(),from.width(),to.centerX(),to.width());lensScene=scene(new IslandDrop.Scene());lensLast=0L;
            }
            ViewGroup row=rail.get();if(row!=null&&!following){following=true;row.postOnAnimation(follow);}
        }
        /** Whether the moving lens is on the surface (its motion there) or drawn here (its motion here). */
        boolean onSurface,following,surfaceNow;long lensLast;float lensSwell=-1f;
        final IslandDrop.Snapshot snapshot=new IslandDrop.Snapshot();
        /** The island as it is at rest, for the lens's picture: no lens, no capsule following one. */
        private void drawPlain(Canvas c){boolean l=lensNow,f=surfaceNow;lensNow=false;surfaceNow=false;try{drawIsland(c);}finally{lensNow=l;surfaceNow=f;}}
        private void drawPlainGlass(Canvas c){int was=part;part=1;try{drawPlain(c);}finally{part=was;}}
        private void drawPlainItems(Canvas c){int was=part;part=2;try{drawPlain(c);}finally{part=was;}}
        private void drawItems(Canvas c){int was=part;part=2;try{drawIsland(c);}finally{part=was;}}
        /** The drag's picture again, as the glass the lens shows unbent (the same bitmap: glass above, items below). */
        android.graphics.Bitmap dragGlass;
        final IslandDrop.Motion lensMotion=new IslandDrop.Motion();IslandDrop.Scene lensScene;
        /** Each frame while the lens moves: redraw (to hide what it covers), and land it once it has come to rest. */
        final Runnable follow=new Runnable(){public void run(){
            ViewGroup row=rail.get();if(row==null){following=false;return;}
            // held mid-flight: the drag has the lens now, from where it was
            if(manual){following=false;return;}
            boolean moving;
            if(onSurface&&flight!=null){moving=flight.peek(lensRect)>=0f;}
            else{long now=System.nanoTime();float dt=lensLast==0L?1f/120f:(now-lensLast)/1e9f;lensLast=now;moving=lensMotion.step(dt);}
            if(!moving){following=false;progress=1f;pill.set(target);previous=-1;layout();invalidateSelf();return;}
            invalidateSelf();row.postOnAnimation(this);
        }};
        /** The island as the lens sees it: its body, edge, icons and labels, copied so another thread may read it. */
        private IslandDrop.Scene scene(IslandDrop.Scene out){
            boolean light=tintMode==Blur.TINT_LIGHT;
            out.body.set(body);out.radius=radius;
            out.bodyFill=light?0xF2F2F2F7:0xF2252528;out.edge=light?0x40000000:0xFF0C0C0E;out.accent=dropTint2();
            for(int i=0;i<5;i++){
                boolean shown=i<n&&i!=create&&!pictured[i]&&filled[i]!=null&&!glyphs[i].isEmpty();
                out.icons[i]=shown?filled[i]:null;out.places[i].set(glyphs[i]);
                out.labels[i]=i>=n||i==create?null:caption[i];out.labelX[i]=centres[i];
            }
            out.labelY=captionBaseline();out.labelSize=captionInk.getTextSize();out.face=medium();
            return out;
        }
        /** The Liquid Glass body: a darker smoked glass, as iOS 26 tints its tab bar. */
        /** Icons and labels on that glass: white on the smoked glass, near-black on the light one. */
        private int dropInk(){return tintMode==Blur.TINT_LIGHT?0xFF1C1C1E:0xFFFFFFFF;}
        private int dropTint(){return tintMode==Blur.TINT_LIGHT?0x99F2F2F7:0x991C1C1E;}
        /** The other styles' switch: a short slide with a soft landing, as it always was. */
        private void slide(){
            from.set(pill);to.set(target);progress=0f;
            animation=ValueAnimator.ofFloat(0f,1f);animation.setDuration(Math.round(220L/IslandMotion.stiffness()));animation.setInterpolator(LINEAR);
            animation.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){if(animation==a){animation=null;progress=1f;pill.set(target);previous=-1;layout();invalidateSelf();}}});
            animation.addUpdateListener(a->{progress=(Float)a.getAnimatedValue();step();layout();});animation.start();
        }
        /** Soft landing: fast start, long gentle stop. */
        static float ease(float t){float u=1f-t;return 1f-u*u*u*u;}
        /** A short slide keeps the indicator level even during rapid tab changes. */
        void step() {
            float t=progress;
            if(t>=1f){pill.set(to);lift=0f;previous=-1;}
            else {
                float move=ease(t);
                pill.set(from.left+(to.left-from.left)*move,from.top+(to.top-from.top)*move,
                         from.right+(to.right-from.right)*move,from.bottom+(to.bottom-from.bottom)*move);
                lift=0f;
            }
            if(skin!=null)tint();
            invalidateSelf();
        }
        void collect(ArrayList<View> tabs){
            loud(); // the cells may be new: their ripples are looked at again
            ImageView[] old=icons.clone();
            for(int i=0;i<5;i++){icons[i]=null;labels[i]=null;outlined[i]=filled[i]=null;glyphs[i].setEmpty();caption[i]=null;captionFrom[i]=null;shown[i]=null;said[i]=null;}
            parts.clear();badges.clear();plus=null;
            n=Math.min(5,tabs.size());create=-1;
            for(int i=0;i<n;i++){int role=role(tabs.get(i),0);roles[i]=role<0?2:role;if(role<0)create=i;}
            for(int i=0;i<n;i++){
                if(i==create){collectCreate(tabs.get(i));continue;}
                small.clear();collect(tabs.get(i),i,0);
                // Unread counts and dots follow the redrawn icon, so they stay on its corner.
                for(ImageView image:small)if(image!=icons[i])badges.add(new Part(image,i));
                String symbol=SYMBOLS[roles[i]];
                // TikTok's own icon, traced into a shape, when chosen; the Material symbol otherwise (or when tracing finds nothing)
                if(icons[i]!=null&&symbol!=null&&IslandMotion.tiktokIcons()){android.graphics.Path own=IslandMotion.trace(icons[i]);if(own!=null){outlined[i]=filled[i]=own;tracedFrom[i]=icons[i].getDrawable();}}
                if(icons[i]!=null&&symbol!=null&&outlined[i]==null){outlined[i]=Symbols.path(symbol);filled[i]=Symbols.path(symbol+"_fill1");tracedFrom[i]=null;}
                if(outlined[i]==null||filled[i]==null){outlined[i]=filled[i]=null;}
                if(icons[i]!=null)parts.add(new Part(icons[i],i));
            }
            // An icon that is no longer ours is shown natively again.
            for(ImageView view:old){if(view==null||view.getImageAlpha()!=0)continue;boolean kept=false;for(ImageView now:icons)kept|=now==view;if(!kept)view.setImageAlpha(255);}
        }
        /** The native create button keeps its touch; its children are hidden and the Sunny is painted instead. */
        private void collectCreate(View cell){
            plus=Symbols.path("add");
            if(plus!=null&&cell instanceof ViewGroup){ViewGroup group=(ViewGroup)cell;for(int i=0;i<group.getChildCount();i++)parts.add(new Part(group.getChildAt(i),create));}
        }
        private void collect(View view,int tab,int depth) {
            if(depth>5)return;
            if(view instanceof TextView){if(role(String.valueOf(((TextView)view).getText()))>=0){parts.add(new Part(view,tab));if(labels[tab]==null)labels[tab]=(TextView)view;}else badges.add(new Part(view,tab));}
            // Only the largest small image is the tab's icon; badges and dots keep their colour and only move with it.
            else if(view instanceof ImageView&&view.getWidth()<=40*unit&&view.getHeight()<=40*unit){small.add((ImageView)view);ImageView best=icons[tab];if(best==null||view.getWidth()*view.getHeight()>best.getWidth()*best.getHeight())icons[tab]=(ImageView)view;}
            if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)collect(group.getChildAt(i),tab,depth+1);}
        }
        /** How selected a tab looks right now (0..1), shared by pill, icon fill and label so they move together. */
        private float weight(int tab){
            if(manual)return Math.max(0f,1f-Math.abs(centres[tab]-px.v)/(64*unit));
            if(progress>=1f)return tab==selected?1f:0f;
            return tab==selected?progress:tab==previous?1f-progress:0f;
        }
        /** How far the label is out: follows the pill's width while it is being held or lands. */
        private float reveal(int tab){
            float w=weight(tab);if(!manual)return w;
            float span=wide[tab]-pillWidth;return span<=1f?0f:w*Math.max(0f,Math.min(1f,(pill.width()-pillWidth)/span));
        }
        /**
         * How much of a tab's written label shows. Never two at once: on a switch the old label is gone in the first
         * quarter and the new one comes in after a third, and a held pill shows none until it lands, so the words of
         * two tabs never share the moving pill and a half-open pill never shows a cut word.
         */
        private float labelShown(int tab){
            if(dropStyle()||hideLabels())return 0f;
            if(manual){if(dragging||tab!=selected)return 0f;return Math.max(0f,Math.min(1f,(reveal(tab)-.5f)/.5f));}
            if(progress>=1f)return reveal(tab);
            if(tab==selected)return Math.max(0f,Math.min(1f,(progress-.3f)/.7f));
            return tab==previous?Math.max(0f,1f-progress/.25f):0f;
        }
        /** Native labels: their text (for our label) and their measured width; ellipsized once per change. */
        private boolean measure(){
            boolean changed=false;
            // The label fits its half beside the other tab's icon; with too little room the pill stays icon-only.
            float max=Math.max(0f,Math.min(leftTo-leftFrom,rightTo-rightFrom)-PARTNER*unit-PADDING*unit);if(max<20*unit)max=0f;
            if(max!=labelRoom){labelRoom=max;for(int i=0;i<5;i++)said[i]=null;}
            for(int i=0;i<n;i++){
                if(i==create){wide[i]=0;continue;}
                // Compared in place: a steady frame allocates no strings.
                TextView label=labels[i];CharSequence raw=label==null?null:label.getText();
                if(raw==null?said[i]==null&&shown[i]==null:said[i]!=null&&said[i].contentEquals(raw))continue;
                if(!changed){ink.setTextSize(13*unit);ink.setTypeface(medium());}
                said[i]=raw==null?null:raw.toString();changed=true;String text=said[i]==null?null:said[i].trim();
                if(text==null||text.isEmpty()||outlined[i]==null||max<=0f){shown[i]=null;labelWidth[i]=0;continue;}
                String fit=text;float width=ink.measureText(fit);
                if(width>max){int n=ink.breakText(text,true,Math.max(0f,max-ink.measureText("…")),null);fit=text.substring(0,Math.max(0,n)).trim()+"…";width=ink.measureText(fit);}
                shown[i]=fit;labelWidth[i]=width;
            }
            return changed;
        }
        private static Typeface face;
        private static Typeface medium(){
            if(face==null){try{face=android.os.Build.VERSION.SDK_INT>=28?Typeface.create(Typeface.DEFAULT,600,false):Typeface.create("sans-serif-medium",Typeface.NORMAL);}catch(Throwable error){face=Typeface.DEFAULT_BOLD;}}
            return face;
        }
        /** Idempotent: writes only what TikTok (or a recolour) has changed, so a steady frame writes nothing. */
        private void tint() {
            for(int i=0;i<parts.size();i++){
                Part part=parts.get(i);
                if(part.tab==create){if(part.view.getAlpha()!=0f){if(!createAlpha.containsKey(part.view))createAlpha.put(part.view,part.view.getAlpha());part.view.setAlpha(0f);}continue;}
                if(part.view instanceof TextView){TextView title=(TextView)part.view;if(!labelColours.containsKey(title)&&title.getCurrentTextColor()!=Color.TRANSPARENT)labelColours.put(title,title.getTextColors());
                    // MD3 Expressive: no labels at rest; the selected one is drawn beside its icon instead.
                    int colour=outlined[part.tab]!=null?Color.TRANSPARENT:textColour;if(title.getCurrentTextColor()!=colour)title.setTextColor(colour);}
                else{ImageView icon=(ImageView)part.view;
                    if(outlined[part.tab]!=null){if(icon.getImageAlpha()!=0)icon.setImageAlpha(0);}
                    else if(icon.getImageTintList()!=iconTint)icon.setImageTintList(iconTint);}
            }
        }
        /** Where each symbol sits this frame: on its tab, or at the start of the pill, with neighbours making room. */
        private void layout(){
            float half=12*unit,cy=(body.top+body.bottom)/2f;
            if(create>=0)shift[create]=0f; // the create button never moves
            boolean drops=dropStyle();
            for(int j=0;j<n;j++){
                if(j==create)continue;
                if(drops){
                    // iOS 26: each tab's icon stays on its tab, its label under it
                    shift[j]=0f;glyphX[j]=centres[j];float iy=iconCentre(),g=ICON_DP/2f*unit;
                    glyphs[j].set(centres[j]-g,iy-g,centres[j]+g,iy+g);continue;
                }
                // Only the other tab of the same half makes room; a held pill pushes whatever it is over.
                float push=0f;
                for(int i=0;i<n;i++){
                    if(i==j||i==create||(!manual&&left(i)!=left(j)))continue;float w=weight(i);if(w<=0f)continue;
                    float c=manual?(pill.left+pill.right)/2f:itemCentre(i),reach=(manual?pill.width():wide[i])/2f+4*unit+half;
                    float d=centres[j]-c,need=reach-Math.abs(d);if(need>0f)push+=w*need*(d<0?-1f:1f);
                }
                float from=left(j)?leftFrom:rightFrom,to=left(j)?leftTo:rightTo;
                float rest=Math.max(from+half,Math.min(to-half,centres[j]+push));shift[j]=rest-centres[j];
                float out=itemCentre(j)-wide[j]/2f+LEAD*unit+half,w=reveal(j);
                glyphX[j]=rest+(out-rest)*w;
                glyphs[j].set(glyphX[j]-half,cy-half,glyphX[j]+half,cy+half);
            }
            // A friend's picture on a tab (TikTok shows one on Friends when someone posted): it takes the icon's place.
            for(int j=0;j<5;j++)pictured[j]=false;
            for(int b=0;b<badges.size();b++){
                View badge=badges.get(b).view;
                if(badge instanceof ImageView&&badge.isShown()&&badge.getAlpha()>.05f&&((ImageView)badge).getDrawable()!=null
                        &&badge.getWidth()>=16*unit&&badge.getHeight()>=16*unit)pictured[badges.get(b).tab]=true;
            }
            // an island file: it is told where the mod would put things, and puts them where it says
            if(ownIsland!=null)layoutOwn();
            ViewGroup row=rail.get();if(row==null)return;
            for(int b=0;b<badges.size();b++){
                Part part=badges.get(b);ImageView icon=icons[part.tab];if(icon==null||outlined[part.tab]==null)continue;
                float x=icon.getWidth()/2f-icon.getTranslationX(),y=icon.getHeight()/2f-icon.getTranslationY();View v=icon;
                for(int d=0;d<8&&v!=null&&v!=row;d++){x+=v.getX();y+=v.getY();Object up=v.getParent();v=up instanceof View?(View)up:null;}
                if(v!=row)continue;
                float dx=glyphX[part.tab]-x,dy=glyphs[part.tab].centerY()-y;View badge=part.view;
                if(!badgeOffset.containsKey(badge))badgeOffset.put(badge,new float[]{badge.getTranslationX(),badge.getTranslationY()});
                if(Math.abs(badge.getTranslationX()-dx)>.5f)badge.setTranslationX(dx);
                if(Math.abs(badge.getTranslationY()-dy)>.5f)badge.setTranslationY(dy);
            }
        }
        /** The Liquid Glass tab: a 23 dp icon above a 10.5 sp label, the pair centred on the island. */
        static final float ICON_DP=23f,CAPTION_SP=10.5f;
        private float iconCentre(){return (body.top+body.bottom)/2f-(hideLabels()?0f:6.5f*unit);}
        private float captionBaseline(){return (body.top+body.bottom)/2f+16f*unit;}
        /** Each tab's label for the Liquid Glass style, cut to its tab's width; measured only when a label changes. */
        final String[] caption=new String[5],captionFrom=new String[5];
        final Paint captionInk=new Paint(Paint.ANTI_ALIAS_FLAG);float captionWidth=-1f;
        private void captions(){
            float room=body.width()/n-4*unit;
            if(room!=captionWidth){captionWidth=room;for(int i=0;i<5;i++)captionFrom[i]=null;}
            captionInk.setTextSize(CAPTION_SP*unit*fontScale());captionInk.setTypeface(medium());captionInk.setTextAlign(Paint.Align.CENTER);
            for(int i=0;i<5;i++){
                String text=i>=n||said[i]==null?null:said[i].trim();
                if(i==create||text==null||text.isEmpty()||hideLabels()){caption[i]=null;captionFrom[i]=null;continue;}
                if(text.equals(captionFrom[i]))continue;
                captionFrom[i]=text;String fit=text;
                if(captionInk.measureText(fit)>room){int n=captionInk.breakText(text,true,Math.max(0f,room-captionInk.measureText("…")),null);fit=text.substring(0,Math.max(0,n)).trim()+"…";}
                caption[i]=fit;
            }
        }
        private float fontScale(){ViewGroup row=rail.get();return row==null?1f:Math.min(1.3f,row.getResources().getConfiguration().fontScale);}
        /** MD3 Expressive horizontal item: 12 dp lead, 24 dp icon, 6 dp gap, label, 14 dp trail; room kept for the half's other icon. */
        static final float LEAD=12f,GAP=6f,TRAIL=14f,PADDING=LEAD+24f+GAP+TRAIL,PARTNER=32f;
        /** Whether a tab stands left of the create button (left of the middle when there is none). */
        private boolean left(int tab){return create>=0?tab<create:tab<n/2;}
        /** The other tab of the same half, or -1 when it stands alone there. */
        private int partner(int tab){
            int found=-1;for(int i=0;i<n;i++)if(i!=tab&&i!=create&&left(i)==left(tab)){if(found>=0)return -1;found=i;}
            return found;
        }
        /** A settled pill's centre: on its native tab, inside its half, leaving the other tab's icon its 32 dp. */
        private float itemCentre(int tab){
            float w=wide[tab]>0?wide[tab]:pillWidth,c=centres[tab];if(body.right<=body.left)return c;
            boolean left=left(tab);float from=left?leftFrom:rightFrom,to=left?leftTo:rightTo;
            int partner=partner(tab);
            float lo=from+(partner>=0&&partner<tab?PARTNER*unit:0f)+w/2f,hi=to-(partner>tab?PARTNER*unit:0f)-w/2f;
            return lo>hi?(lo+hi)/2f:Math.max(lo,Math.min(hi,c));
        }
        void update(ArrayList<View> tabs) {
            ViewGroup row=rail.get();if(row==null)return;
            cells=tabs;
            int colour=Accent.colour(),mode=Blur.tint();boolean recolour=skin==null||colour!=accent||mode!=tintMode,changed=recolour;
            if(recolour){
                accent=colour;tintMode=mode;skin=Skin.tonal(row.getContext(),colour);
                if(mode==Blur.TINT_LIGHT){
                    // White glass (iOS light material): dark content, the accent darkened to stay readable on it.
                    bodyColour=0xFFF2F2F7;textColour=0xFF1C1C1E;muted=0x993C3C43;glassTint=0x59FFFFFF;
                    pillColour=blend(0xFFFFFFFF,accent,.24f);onPill=blend(accent,0xFF000000,.38f);
                }else if(mode==Blur.TINT_DARK){
                    // Smoked glass: neutral, whatever the accent.
                    bodyColour=0xFF1C1C1E;textColour=0xFFFFFFFF;muted=0x99EBEBF5;glassTint=0x66000000;
                    pillColour=blend(0xFF2C2C2E,accent,.38f);onPill=blend(accent,0xFFFFFFFF,.82f);
                }else{
                    bodyColour=skin.card;textColour=skin.text;muted=skin.muted();glassTint=(skin.card&0xFFFFFF)|0x40000000;
                    pillColour=blend(skin.card,accent,.38f);onPill=blend(accent,0xFFFFFFFF,.82f);
                }
                onAccent=luminance(accent)>.55f?0xFF1B1B1F:0xFFFFFFFF;
                iconTint=ColorStateList.valueOf(textColour);
            }
            // Anti-burn dims the island's content only where it dims the screen (the feed, a post), and eases
            // in and out instead of jumping on every tab change.
            float fade=Dim.dimming()?Math.max(.1f,1f-Dim.strength()/100f):1f;
            if(fade!=dimTarget){dimTarget=fade;
                if(dimming!=null)dimming.cancel();
                if(IslandMotion.moves(row.getContext())){
                    dimming=ValueAnimator.ofFloat(dim,fade);dimming.setDuration(320L);dimming.setInterpolator(Motion.BOTH);
                    dimming.addUpdateListener(a->{dim=(Float)a.getAnimatedValue();invalidateSelf();});dimming.start();
                }else{dim=fade;changed=true;}
            }
            // Island body: the native bounds, inset at the sides, lifted a little only when the native cells leave room for it.
            float top=Float.MAX_VALUE,bottom=-Float.MAX_VALUE,narrow=Float.MAX_VALUE;
            for(int i=0;i<tabs.size();i++){View cell=tabs.get(i);top=Math.min(top,cell.getY());bottom=Math.max(bottom,cell.getY()+cell.getHeight());narrow=Math.min(narrow,cell.getWidth());centres[i]=cell.getX()+cell.getWidth()/2f;}
            top=Math.max(0f,top);bottom=Math.min((float)row.getHeight(),bottom);
            bottom-=Math.min(6*unit,Math.max(0f,bottom-top-56*unit));
            float side=Math.min(12*unit,narrow/8f);
            next.set(side,top,row.getWidth()-side,bottom);
            if(!next.equals(body)){body.set(next);radius=Math.min(body.height()/2f,40*unit);outline.reset();outline.addRoundRect(body,radius,radius,Path.Direction.CW);changed=true;}
            // The icon-only pill: one width for every tab, fitted to the outer cells so their centres stay on the native buttons.
            float room=body.width()-12*unit;
            for(int i=0;i<n;i++)if(i!=create)room=Math.min(room,2f*Math.min(centres[i]-body.left-6*unit,body.right-6*unit-centres[i]));
            boolean drops=dropStyle();
            if(drops){
                // Liquid Glass (iOS 26): a capsule round the tab's icon and label, nearly the island's height
                pillHeight=Math.max(0f,body.height()-6*unit);
                pillWidth=Math.max(0f,Math.min(pillHeight*1.15f,Math.min(narrow+10*unit,room)));
            }else{
                pillWidth=Math.max(0f,Math.min(56*unit,Math.min(narrow-12*unit,room)));
                pillHeight=Math.max(0f,Math.min(40*unit,body.height()-12*unit));
            }
            pillTop=(body.top+body.bottom)/2f-pillHeight/2f;
            float s=Math.max(0f,Math.min(48*unit,body.height()-10*unit));
            if(s!=sunnySize){sunnySize=s;buildSunny(s);changed=true;}
            // The create button is fixed; each half lays out on its own side of it, 6 dp clear.
            float keep=plus!=null?sunnySize/2f+6*unit:0f;
            // with no create button the halves meet in the middle
            float middle=plus!=null&&create>=0?centres[create]:(body.left+body.right)/2f;
            leftFrom=body.left+6*unit;leftTo=middle-keep;
            rightFrom=middle+keep;rightTo=body.right-6*unit;
            changed|=measure();
            quiet(tabs,drops);
            float gap=body.width()/n;
            if(drops){
                captions();
                // Liquid Glass: the tabs are spread inside the island, clear of its round ends -- TikTok's outer cells
                // reach under them, and "Главная" or "Профиль" stood over the edge. The outer centres sit far enough in
                // for their labels and capsules, and the five are spaced evenly between.
                // With the labels hidden the capsule keeps the size it has with them, as iOS does: about a label's width
                // (a little more where there is room, a little less where there is not), so the icons stand in the
                // middle of capsules that look the same either way, and the row draws in from the island's ends.
                float bare=body.width()/Math.max(1,n);
                float unlabelled=Math.max(body.height()*.62f,Math.min(bare-30*unit,body.height()*1.05f));
                float outer=hideLabels()?unlabelled:0f;for(int i:new int[]{0,n-1})if(caption[i]!=null)outer=Math.max(outer,captionInk.measureText(caption[i]));
                float inset=Math.max(body.height()*.46f,outer/2f+15*unit);
                gap=Math.max(0f,(body.width()-2f*inset)/Math.max(1,n-1));
                for(int i=0;i<n;i++)centres[i]=body.left+inset+gap*i;
                // each capsule takes in its whole label, 11 dp clear either side, never wider than the space of its tab
                for(int i=0;i<n;i++){float label=caption[i]==null?(hideLabels()?unlabelled:0f):captionInk.measureText(caption[i]);wide[i]=i==create?pillWidth:Math.min(gap+6*unit,Math.max(pillWidth*.92f,label+22*unit));}
            }
            for(int i=0;i<n&&!drops;i++){if(i==create||shown[i]==null){wide[i]=pillWidth;continue;}float span=(left(i)?leftTo-leftFrom:rightTo-rightFrom)-PARTNER*unit;wide[i]=Math.max(pillWidth,Math.min(span,PADDING*unit+labelWidth[i]));}
            boolean down=create>=0&&create<tabs.size()&&tabs.get(create).isPressed();
            if(down!=createDown){createDown=down;squeeze.t=down?1f:0f;if(!down)turn.t+=45f;if(IslandMotion.moves(row.getContext()))kick();else{squeeze.set(squeeze.t);turn.set(turn.t%360f);}changed=true;}
            int touched=-1;for(int i=0;i<tabs.size()&&i<5;i++)if(i!=create&&tabs.get(i).isPressed()){touched=i;break;}
            if(touched>=0)pressedTab=touched;float lit=touched>=0?1f:0f;
            if(layer.t!=lit){layer.t=lit;if(IslandMotion.moves(row.getContext()))kick();else layer.set(lit);changed=true;}
            if(manual){layout();if(skin!=null)tint();if(changed)invalidateSelf();return;}
            int chosen=-1;for(int i=0;i<tabs.size()&&i<5;i++)if(i!=create&&selected(tabs.get(i),0)){chosen=i;break;}
            if(chosen>=0) {
                targetFor(chosen,next);
                // a tab was just tapped and the drop is on its way: TikTok showing the old tab a moment longer is not a change
                if(chosen!=selected&&asked==selected&&android.os.SystemClock.uptimeMillis()<askedUntil)chosen=selected;
                if(chosen==selected)asked=-1;
                if(chosen!=selected) {
                    // Only a different tab starts the motion; the indicator is never restarted by geometry alone.
                    boolean first=selected<0;previous=selected;selected=chosen;target.set(next);cancel();progress=1f;
                    if(first||!IslandMotion.moves(row.getContext())){pill.set(target);previous=-1;}
                    else if(dropStyle())move();
                    else slide();
                    changed=true;
                } else if(!next.equals(target)) {
                    // Native tab shifted a little (insets, badge, font scale, label): follow it in place, keep any running motion.
                    target.set(next);
                    if(animation!=null)to.set(target);else pill.set(target);
                    changed=true;
                }
            }
            layout();
            if(skin!=null)tint();
            if(changed)invalidateSelf();
        }
        /** MD3 Sunny of the given box, from the motion lab's radial table; rebuilt only when the size changes. */
        private void buildSunny(float size){
            sunny.reset();if(size<=0f)return;
            float[] rays=LabShapes.get()[LabShapes.SUNNY];int n=rays.length;
            for(int i=0;i<n;i++){double a=i*Math.PI*2/n;float r=rays[i]*size,x=(float)(Math.cos(a)*r),y=(float)(Math.sin(a)*r);if(i==0)sunny.moveTo(x,y);else sunny.lineTo(x,y);}
            sunny.close();
        }
        /** Records the blurred backdrop for this frame; called from pre-draw, never schedules a frame itself. */
        void backdrop(ViewGroup row){
            if(!blurTried){blurTried=true;blur=Blur.Backdrop.create();}
            // while the lens moves, what is behind the glass is caught a little less often: it is blurred, and the frame time goes to the lens
            if(blur!=null){blur.pace(manual||progress<1f);blur.light(IslandMotion.lightGlass());}
            // Liquid Glass (refracting lens, Android 13+) or frosted glass, as chosen in the settings.
            boolean glass=Blur.liquid();
            // Liquid Glass is clear glass: a 1 dp pre-blur only smooths the half-resolution capture, and a 25% tint keeps
            // the icons readable without turning it milky. The lens does the rest.
            // Plastic: nothing is recorded; the island draws its solid tonal surface.
            RectF area=ownIsland!=null&&frame!=null&&frame.ready?frame.body:body;float round=ownIsland!=null&&frame!=null&&frame.ready?frame.radius:radius;
            // the strip under the island is TikTok's black: the glass takes the bottom of the video (the page) just above the bar
            if(blur!=null)blur.lift(Math.max(0f,area.bottom));
            boolean now=!Blur.plain()&&blur!=null&&area.right>area.left&&blur.record(row,area,glass?1*unit:24*unit,round,glass,dropStyle()?dropTint():glassTint);
            if(now!=blurred||glass!=liquid){blurred=now;liquid=glass;invalidateSelf();}
        }
        void release(){if(blur!=null)blur.release();blurred=false;snapshot.release();}
        /** Every native pixel we hid comes back; nothing of ours is left on TikTok's views. */
        /**
         * TikTok's own press ripple on its tabs (a grey circle where the finger was) is put away while the Liquid
         * Glass island is up -- the lens is the feedback there -- and given back otherwise.
         */
        final Map<View,Drawable[]> ripples=new WeakHashMap<>();
        void quiet(ArrayList<View> tabs,boolean drops){
            if(!drops){if(!ripples.isEmpty())loud();return;}
            if(!ripples.isEmpty())return;
            for(View cell:tabs)quiet(cell,0);
            if(ripples.isEmpty())ripples.put(null,null); // looked, nothing to put away
        }
        private void quiet(View view,int depth){
            if(view==null||depth>3)return;
            Drawable back=view.getBackground(),front=android.os.Build.VERSION.SDK_INT>=23?view.getForeground():null;
            boolean b=back instanceof RippleDrawable,f=front instanceof RippleDrawable;
            if(b||f){ripples.put(view,new Drawable[]{b?back:null,f?front:null});if(b)view.setBackground(null);if(f)view.setForeground(null);}
            if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)quiet(g.getChildAt(i),depth+1);}
        }
        void loud(){
            for(Map.Entry<View,Drawable[]> e:ripples.entrySet()){View v=e.getKey();if(v==null)continue;Drawable[] d=e.getValue();
                if(d[0]!=null&&v.getBackground()==null)v.setBackground(d[0]);if(d[1]!=null&&android.os.Build.VERSION.SDK_INT>=23&&v.getForeground()==null)v.setForeground(d[1]);}
            ripples.clear();
        }
        void unhide(){
            loud();
            for(ImageView icon:icons)if(icon!=null&&icon.getImageAlpha()==0)icon.setImageAlpha(255);
            for(Map.Entry<TextView,ColorStateList> e:labelColours.entrySet())if(e.getKey()!=null&&e.getValue()!=null)e.getKey().setTextColor(e.getValue());
            for(Map.Entry<View,Float> e:createAlpha.entrySet())if(e.getKey()!=null)e.getKey().setAlpha(e.getValue());
            for(Map.Entry<View,float[]> e:badgeOffset.entrySet())if(e.getKey()!=null){e.getKey().setTranslationX(e.getValue()[0]);e.getKey().setTranslationY(e.getValue()[1]);}
            labelColours.clear();createAlpha.clear();badgeOffset.clear();
        }
        private int content(int colour,float amount){return (colour&0xFFFFFF)|(Math.round(Color.alpha(colour)*amount*dim*alpha/255f)<<24);}
        /** Where the lens is now, and whether there is one (moving, or held); worked out before the island is drawn. */
        boolean lensNow;
        /**
         * The island, and over it the lens when there is one. The lens reads the island itself: the island is drawn
         * once into a RenderNode, that node is shown, and the lens shader shows the same node again bent -- the real
         * glass, the video behind it and the icons where they are, magnified as it passes, live, with nothing hidden
         * or faded under it.
         */
        public void draw(Canvas canvas) {
            if(skin==null||body.right<=body.left)return;
            cat.ttcuz.island.Island own=ownIsland;
            if(own!=null&&frame!=null&&frame.ready){
                try{own.draw(canvas,frame);}catch(Throwable error){IslandFiles.fail(own,error);}
                return;
            }
            lensNow=false;surfaceNow=false;lensSwell=-1f;
            if(dropStyle()&&selected>=0){
                if(manual&&dragSurface){
                    // held, its lens on the surface: the island under it is drawn as at rest
                    heldLens(lensRect);lensSwell=Math.max(0f,Math.min(1f,grow.v));surfaceNow=true;
                }else if(manual){
                    // held and dragged: the capsule swells into the lens under the finger
                    float g=Math.max(0f,Math.min(1f,grow.v)),cx=(pill.left+pill.right)/2f,w=pill.width()*(1f+.24f*g*IslandMotion.wide()),h=pillHeight*(1f+.32f*g*IslandMotion.tall());
                    lensRect.set(cx-w/2f,cy0()-h/2f,cx+w/2f,cy0()+h/2f);lensSwell=g;lensNow=true;
                }else if(progress<1f&&onSurface&&flight!=null){
                    // the lens flies on its own surface: here only the capsule that waits for it, faded as it passes
                    lensSwell=Math.max(0f,flight.peek(lensRect));surfaceNow=true;
                }else if(progress<1f){
                    lensMotion.rect(lensRect,cy0(),pillHeight);lensSwell=Math.min(1f,lensMotion.s);lensNow=true;
                }
            }
            if(lensNow){
                if(painter==null)painter=new IslandDrop.Painter(unit);
                if(mainLens==null)mainLens=new IslandDrop.LensShader(painter);
                // the lens bends what is on the island, never the glass and the video behind it: the glass drawn as it
                // is, what is on it drawn around the lens, and through the lens bent
                part=1;drawIsland(canvas);
                int k=canvas.save();
                float r=Math.min(lensRect.width(),lensRect.height())/2f;
                lensClip.reset();lensClip.addRoundRect(lensRect,r,r,Path.Direction.CW);
                if(android.os.Build.VERSION.SDK_INT>=26)canvas.clipOutPath(lensClip);else canvas.clipPath(lensClip,android.graphics.Region.Op.DIFFERENCE);
                part=2;drawIsland(canvas);
                canvas.restoreToCount(k);part=0;
                android.graphics.Rect b=getBounds();
                if(mainLens.over(canvas,Math.max(1,b.width()),Math.max(1,b.height()),Flight_PAD*unit,this::drawItems,lensRect,lensSwell,dim*alpha/255f))return;
                // no shader here (Android 12 or older): the lens drawn with the canvas, from the island's icons alone
                painter.lens(canvas,lensRect,scene(hereScene),dim*alpha/255f*IslandDrop.opacity(lensSwell));
                return;
            }
            drawIsland(canvas);
        }
        /** Room the lens has above and below the island's row (its parents let it draw there). */
        static final float Flight_PAD=14f;
        /** What drawIsland draws: 0 all, 1 the glass alone, 2 what is on it alone (capsule, icons, labels). */
        private int part;
        private final Path lensClip=new Path();
        private void drawIsland(Canvas canvas){
            // Frosted MD3 surface: the blurred backdrop under a translucent tonal container; opaque when blur is unavailable.
            boolean glass=blurred&&liquid&&blur!=null&&blur.liquid();
            if(part!=2){
            if(glass){
                // iOS lifts glass off the content with a wide, faint shadow (cached by the renderer for a fixed rounded rect).
                int lift=bodyColour^alpha;if(lift!=liftFor){liftFor=lift;liftPaint.setColor(bodyColour);liftPaint.setShadowLayer(14*unit,0,4*unit,(Math.round(.22f*alpha)<<24));}
                canvas.drawRoundRect(body,radius,radius,liftPaint);
            }
            if(blurred&&blur!=null)blur.draw(canvas,body,outline);
            // Liquid Glass carries its tint and bevel in the shader; frosted glass gets the translucent container.
            if(!glass){paint.setColor(bodyColour);paint.setAlpha(blurred?alpha*184/255:alpha);canvas.drawRoundRect(body,radius,radius,paint);}
            else{
                // The specular edge: one crisp hairline that catches the light on top, fades along the sides and comes
                // back weaker at the bottom (light reflected inside the glass) -- not a white band.
                if(rimFor!=body.height()){
                    rimFor=body.height();
                    rimPaint.setStyle(Paint.Style.STROKE);rimPaint.setStrokeWidth(Math.max(1f,unit));
                    rimPaint.setShader(new LinearGradient(0,body.top,0,body.bottom,new int[]{0xB3FFFFFF,0x26FFFFFF,0x14FFFFFF,0x59FFFFFF},new float[]{0f,.32f,.62f,1f},Shader.TileMode.CLAMP));
                }
                float h=rimPaint.getStrokeWidth()/2f;rimRect.set(body.left+h,body.top+h,body.right-h,body.bottom-h);
                rimPaint.setAlpha(alpha);canvas.drawRoundRect(rimRect,radius-h,radius-h,rimPaint);
            }
            }
            if(part==1)return;
            boolean drops=dropStyle();
            if(drops&&painter==null)painter=new IslandDrop.Painter(unit);
            float strength=dim*alpha/255f;
            // Liquid Glass: at rest a faint capsule under the chosen tab. While the lens moves it is the lens (drawn over
            // all this by draw()); the capsule comes back where the lens lands, as the lens settles into it.
            if(drops&&selected>=0){
                if(!lensNow&&!surfaceNow){
                    painter.rest(canvas,pill,strength);
                    // the island shows the settled capsule now: the lens's surface can clear
                    if(flight!=null)flight.land();
                }else{
                    float a=1f-IslandDrop.opacity(lensSwell);
                    if(a>.01f){float cx=lensRect.centerX(),w=manual?pill.width():surfaceNow?(selected>=0&&wide[selected]>0?wide[selected]:pillWidth):lensMotion.w;slot.set(cx-w/2f,pillTop,cx+w/2f,pillTop+pillHeight);painter.rest(canvas,slot,strength*a);}
                }
            }
            int saved=canvas.save();canvas.clipPath(outline);
            if(!drops&&selected>=0){
                shape.set(pill.left,pill.top-lift,pill.right,pill.bottom+lift);float round=shape.height()/2f;
                int fill=blend(pillColour,accent,.35f*Math.min(1f,Math.max(0f,grow.v)));
                boolean drop=false;
                if(!drop){paint.setColor(fill);paint.setAlpha(Math.round(alpha*dim));canvas.drawRoundRect(shape,round,round,paint);}
            }
            // MD3 pressed state layer: 10% on-surface over the pressed tab (over its pill when it is the selected one).
            if(pressedTab>=0&&layer.v>.01f&&!glyphs[pressedTab].isEmpty()){
                float sel=weight(pressedTab);paint.setColor(content(textColour,.10f*Math.min(1f,layer.v)));
                if(sel>.5f)shape.set(pill);else{float x=glyphX[pressedTab];shape.set(x-pillWidth/2f,pillTop,x+pillWidth/2f,pillTop+pillHeight);}
                float round=shape.height()/2f;canvas.drawRoundRect(shape,round,round,paint);
            }
            canvas.restoreToCount(saved);
            float cy=(body.top+body.bottom)/2f;
            if(drops){
                // iOS 26: every tab a filled icon over its label; the chosen one in the accent once the lens has landed
                boolean settled=progress>=1f&&!manual;
                // what the lens shows of its own picture: the island's icons under it are hidden as much
                float shown=lensNow||surfaceNow?IslandDrop.opacity(lensSwell):0f;
                int tint=dropTint2();
                for(int i=0;i<n;i++){
                    RectF g=glyphs[i];if(i==create||filled[i]==null||g.isEmpty())continue;
                    // the chosen tab takes the accent as the lens over it fades into its capsule: no switch to see
                    int colour=i!=selected?dropInk():settled?tint:blend(dropInk(),tint,1f-shown);
                    float a=1f;
                    if(!pictured[i]){int k=canvas.save();canvas.translate(g.left,g.top);canvas.scale(g.width()/24f,g.height()/24f);paint.setColor(content(colour,a));canvas.drawPath(filled[i],paint);canvas.restoreToCount(k);}
                    if(caption[i]!=null){captionInk.setColor(content(colour,a));canvas.drawText(caption[i],centres[i],captionBaseline(),captionInk);}
                }
            }
            for(int i=0;i<n&&!drops;i++){
                RectF g=glyphs[i];if(i==create||outlined[i]==null||g.isEmpty())continue;
                float w=weight(i);
                if(!pictured[i]){
                    int s=canvas.save();canvas.translate(g.left,g.top);canvas.scale(g.width()/24f,g.height()/24f);
                    if(w<1f){paint.setColor(content(muted,1f-w));canvas.drawPath(outlined[i],paint);}
                    if(w>0f){paint.setColor(content(drops?textColour:onPill,w));canvas.drawPath(filled[i],paint);}
                    canvas.restoreToCount(s);
                }
                float r=labelShown(i);
                if(r>.01f&&shown[i]!=null){
                    // The label slides out of the icon to the right, clipped by the pill as it widens and by its own
                    // tab's place, so it never runs past the pill or over the next tab.
                    targetFor(i,slot);if(!slot.intersect(pill))continue;
                    int c=canvas.save();canvas.clipRect(slot);ink.setColor(content(onPill,r));
                    float x=g.right+GAP*unit-(1f-r)*10*unit;canvas.drawText(shown[i],x,cy-(ink.descent()+ink.ascent())/2f,ink);canvas.restoreToCount(c);
                }
            }
            if(plus!=null&&sunnySize>0f&&create>=0&&drops){
                // Liquid Glass: the create button is a small pane of dark glass with a white plus, squeezed while pressed
                painter.button(canvas,centres[create],cy,body.height(),1f-.08f*squeeze.v,plus,dropInk(),strength);
            }else if(plus!=null&&sunnySize>0f&&create>=0){
                float scale=1f-.1f*squeeze.v;int s=canvas.save();canvas.translate(centres[create],cy);canvas.scale(scale,scale);
                int r=canvas.save();canvas.rotate(turn.v);paint.setColor(content(accent,1f));canvas.drawPath(sunny,paint);canvas.restoreToCount(r);
                float k=sunnySize*.5f/24f;canvas.scale(k,k);canvas.translate(-12f,-12f);paint.setColor(content(onAccent,1f));canvas.drawPath(plus,paint);
                canvas.restoreToCount(s);
            }
        }
        private float cy0(){return (body.top+body.bottom)/2f;}
        IslandDrop.LensShader mainLens;
        /** The chosen tab's colour on the glass: the accent, lightened when it is too dark to read there. */
        private int dropTint2(){return tintMode!=Blur.TINT_LIGHT&&luminance(accent)<.45f?blend(accent,0xFFFFFFFF,.45f):accent;}
        // ------------------------------------------------------------ an island file (IslandFiles)
        ArrayList<View> cells;
        Frame frame;
        /** The frame for the painter: what the mod knows, then the painter's layout, then the icons placed where it said. */
        private void layoutOwn(){
            cat.ttcuz.island.Island own=ownIsland;if(own==null)return;
            ViewGroup row=rail.get();if(row==null)return;
            if(frame==null)frame=new Frame(this);
            Frame f=frame;
            f.width=row.getWidth();f.height=row.getHeight();f.density=unit;f.count=n;f.create=create;
            f.selected=selected;f.previous=previous;f.progress=progress;f.plus=plus;
            f.accent=accent;f.text=textColour;f.muted=muted;f.card=bodyColour;f.dark=luminance(bodyColour)<.5f;
            f.glass=Blur.style();f.time=android.os.SystemClock.uptimeMillis();
            f.body.set(body);f.radius=radius;
            float cy=(body.top+body.bottom)/2f,gap=body.width()/Math.max(1,n);
            for(int i=0;i<5;i++){
                boolean here=i<n;
                f.role[i]=here?roles[i]:-1;
                f.label[i]=here&&i!=create&&said[i]!=null?said[i].trim():null;
                f.icon[i]=here?outlined[i]:null;f.iconFilled[i]=here?filled[i]:null;
                f.pressed[i]=f.down==i;f.picture[i]=here&&pictured[i];
                f.x[i]=here?(i==create||glyphs[i].isEmpty()?centres[i]:glyphX[i]):0f;f.y[i]=cy;
                View cell=here&&cells!=null&&i<cells.size()?cells.get(i):null;
                if(cell!=null)f.hit[i].set(cell.getX(),Math.min(body.top,cell.getY()),cell.getX()+cell.getWidth(),Math.max(body.bottom,cell.getY()+cell.getHeight()));
                else if(here)f.hit[i].set(centres[i]-gap/2f,body.top,centres[i]+gap/2f,body.bottom);
                else f.hit[i].setEmpty();
            }
            try{own.layout(f);}catch(Throwable error){IslandFiles.fail(own,error);return;}
            if(!f.ready)IslandFiles.drawing();
            f.ready=true;
            // unread counts follow the icons wherever the painter put them
            float half=12*unit;
            for(int i=0;i<n;i++){if(i==create)continue;glyphX[i]=f.x[i];glyphs[i].set(f.x[i]-half,f.y[i]-half,f.x[i]+half,f.y[i]+half);}
        }
        /** The painter is gone or another one came: its shader effect off the bar, its frame forgotten. */
        void leaveOwn(){
            ViewGroup row=rail.get();
            if(frame!=null){frame.animating=false;frame.shaders.clear();if(frame.effectOn&&row!=null&&android.os.Build.VERSION.SDK_INT>=31)row.setRenderEffect(null);}
            frame=null;
            if(row!=null)for(int i=0;i<badges.size();i++){View badge=badges.get(i).view;float[] was=badgeOffset.get(badge);if(was!=null){badge.setTranslationX(was[0]);badge.setTranslationY(was[1]);}}
            invalidateSelf();
        }
        /** Opens a tab as a tap on TikTok's own cell would. */
        void open(int tab){
            ViewGroup row=rail.get();if(row==null||cells==null||tab<0||tab>=cells.size())return;
            View cell=cells.get(tab),decor=row.getRootView();
            int[] at=new int[2],base=new int[2];row.getLocationInWindow(at);decor.getLocationInWindow(base);
            float x=at[0]-base[0]+cell.getX()+cell.getWidth()/2f,y=at[1]-base[1]+cell.getY()+cell.getHeight()/2f;
            long now=android.os.SystemClock.uptimeMillis();
            MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0),up=MotionEvent.obtain(now,now+30,MotionEvent.ACTION_UP,x,y,0);
            // the decor hands these to the window's callback, which is ours: replaying keeps them away from the island
            replaying=true;
            try{decor.dispatchTouchEvent(down);decor.dispatchTouchEvent(up);}catch(Throwable error){Diary.note("island open: "+error);}
            finally{replaying=false;down.recycle();up.recycle();}
        }
        /** The bar's touches while an island file draws it: the file's buttons first, then a tap opens TikTok's tab. */
        private int ownTab=-1;private boolean ownMoved,ownHeld,ownTaken,owning;private float ownX,ownY;
        private final Runnable ownHold=new Runnable(){public void run(){
            cat.ttcuz.island.Island own=ownIsland;if(own==null||frame==null||!owning||ownMoved||ownTab<0)return;
            ownHeld=true;try{if(own.onHold(frame,ownTab))frame.tick();}catch(Throwable error){IslandFiles.fail(own,error);}
        }};
        Boolean touch(MotionEvent event){
            cat.ttcuz.island.Island own=ownIsland;ViewGroup row=rail.get();
            if(own==null||frame==null||!frame.ready||row==null||!row.isShown())return null;
            int[] at=new int[2];row.getLocationOnScreen(at);
            float x=event.getRawX()-at[0],y=event.getRawY()-at[1];
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){
                owning=x>=0&&y>=0&&x<row.getWidth()&&y<row.getHeight();if(!owning)return null;
                ownMoved=false;ownHeld=false;ownTaken=false;ownX=x;ownY=y;ownTab=-1;
                for(int i=0;i<frame.count;i++)if(frame.hit[i].contains(x,y)){ownTab=i;break;}
                frame.down=ownTab;
                row.postDelayed(ownHold,ViewConfiguration.getLongPressTimeout());
            }else if(!owning)return null;
            MotionEvent local=MotionEvent.obtain(event);local.setLocation(x,y);boolean broke=false;
            try{if(own.onTouch(frame,local))ownTaken=true;}catch(Throwable error){broke=true;IslandFiles.fail(own,error);}
            finally{local.recycle();}
            if(broke){owning=false;row.removeCallbacks(ownHold);return Boolean.TRUE;}
            if(action==MotionEvent.ACTION_MOVE&&Math.hypot(x-ownX,y-ownY)>ViewConfiguration.get(row.getContext()).getScaledTouchSlop()){ownMoved=true;row.removeCallbacks(ownHold);}
            else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
                row.removeCallbacks(ownHold);owning=false;frame.down=-1;
                if(action==MotionEvent.ACTION_UP&&!ownMoved&&!ownHeld&&!ownTaken&&ownTab>=0){
                    boolean handled=false;try{handled=own.onTap(frame,ownTab);}catch(Throwable error){IslandFiles.fail(own,error);}
                    if(!handled)open(ownTab);
                }
            }
            invalidateSelf();
            return Boolean.TRUE;
        }
        public void setAlpha(int alpha){this.alpha=alpha;invalidateSelf();}
        public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);ink.setColorFilter(filter);invalidateSelf();}
        public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    /** What an island file's painter sees of one island, and the help it gets: backdrop, shaders, opening a tab. */
    static final class Frame extends cat.ttcuz.island.Bar {
        final IslandDrawable island;
        boolean ready,animating,effectOn;int down=-1;Object effectShader;
        final Map<String,Object> shaders=new HashMap<>();
        private final Path clip=new Path();private final RectF clipFor=new RectF();private float clipRadius=-1f;
        Frame(IslandDrawable island){this.island=island;}
        private final Runnable again=new Runnable(){public void run(){if(!animating||island.frame!=Frame.this)return;island.invalidateSelf();ViewGroup row=island.rail.get();if(row!=null)row.postOnAnimation(this);}};
        @Override public void animate(boolean on){
            if(on==animating)return;animating=on;ViewGroup row=island.rail.get();
            if(row!=null){row.removeCallbacks(again);if(on)row.postOnAnimation(again);}
        }
        @Override public void drawBackdrop(Canvas canvas,RectF rect,float radius){
            if(!island.blurred||island.blur==null||rect==null)return;
            if(!rect.equals(clipFor)||radius!=clipRadius){clipFor.set(rect);clipRadius=radius;clip.reset();clip.addRoundRect(rect,radius,radius,Path.Direction.CW);}
            // the backdrop is caught under the island's body: drawn there, cut to what was asked
            island.blur.draw(canvas,body,clip);
        }
        @Override public Object shader(String agsl){
            if(agsl==null||android.os.Build.VERSION.SDK_INT<33)return null;
            Object made=shaders.get(agsl);if(made!=null)return made;
            made=new RuntimeShader(agsl);shaders.put(agsl,made);return made;
        }
        @Override public void uniform(Object shader,String name,float... values){
            if(!(shader instanceof RuntimeShader)||android.os.Build.VERSION.SDK_INT<33)return;
            ((RuntimeShader)shader).setFloatUniform(name,values);
            // a RenderEffect keeps the uniforms it was made with: the bar's effect is made again with the new ones
            ViewGroup row=island.rail.get();
            if(shader==effectShader&&effectOn&&row!=null)row.setRenderEffect(RenderEffect.createRuntimeShaderEffect((RuntimeShader)shader,"content"));
        }
        @Override public Object effect(String agsl){
            ViewGroup row=island.rail.get();if(row==null||android.os.Build.VERSION.SDK_INT<33)return null;
            if(agsl==null){row.setRenderEffect(null);effectOn=false;effectShader=null;return null;}
            RuntimeShader shader=(RuntimeShader)shader(agsl);effectShader=shader;
            row.setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader,"content"));effectOn=true;return shader;
        }
        @Override public void open(int tab){island.open(tab);}
        @Override public void tick(){ViewGroup row=island.rail.get();if(row!=null)row.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);}
        private final Path sunnyShape=new Path();private float sunnyFor=-1f;
        @Override public Path sunny(float size){
            if(size!=sunnyFor){sunnyFor=size;sunnyShape.reset();float[] rays=LabShapes.get()[LabShapes.SUNNY];
                for(int i=0;i<rays.length;i++){double a=i*Math.PI*2/rays.length;float r=rays[i]*size,x=(float)(Math.cos(a)*r),y=(float)(Math.sin(a)*r);if(i==0)sunnyShape.moveTo(x,y);else sunnyShape.lineTo(x,y);}
                sunnyShape.close();}
            return sunnyShape;
        }
    }
    /** The switch runs linear in time; the soft landing is applied to the position (see ease). Shared, not built per switch. */
    private static final PathInterpolator LINEAR=new PathInterpolator(0f,0f,1f,1f);
    static float luminance(int c){return (.299f*Color.red(c)+.587f*Color.green(c)+.114f*Color.blue(c))/255f;}
    static int blend(int a,int b,float amount){return Color.rgb(Math.round(Color.red(a)+(Color.red(b)-Color.red(a))*amount),Math.round(Color.green(a)+(Color.green(b)-Color.green(a))*amount),Math.round(Color.blue(a)+(Color.blue(b)-Color.blue(a))*amount));}
}
