"""Run the actual injected speed hook, including delayed native initialization."""
import pathlib, shutil, subprocess, tempfile, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
STUBS={
'android/content/Context.java':'package android.content;public class Context {public android.content.res.Resources getResources(){return new android.content.res.Resources();}}',
'android/content/res/Resources.java':'package android.content.res;public class Resources {public static class Metrics {public float density=1;}public Metrics getDisplayMetrics(){return new Metrics();}public int getIdentifier(String a,String b,String c){return 0;}public int getDimensionPixelSize(int a){return 0;}}',
'android/view/ViewParent.java':'package android.view;public interface ViewParent {ViewParent getParent();void requestDisallowInterceptTouchEvent(boolean yes);}',
'android/view/MotionEvent.java':'package android.view;public class MotionEvent {public static final int ACTION_DOWN=0,ACTION_UP=1,ACTION_MOVE=2,ACTION_CANCEL=3;int action;float x,y;public MotionEvent(int a,float x,float y){action=a;this.x=x;this.y=y;}public int getActionMasked(){return action;}public float getRawX(){return x;}public float getRawY(){return y;}}',
'android/view/Gravity.java':'package android.view;public class Gravity {public static final int BOTTOM=1,CENTER_HORIZONTAL=2,CENTER=3,CENTER_VERTICAL=4;}',
'android/view/View.java':"""package android.view;import java.util.*;public class View implements ViewParent {
public static final int VISIBLE=0;public ViewGroup parent;public boolean attached=true,clickable;public int visibility=0;public OnClickListener click;
public ArrayList<Runnable> pending=new ArrayList<Runnable>();public interface OnClickListener {void onClick(View v);}public View(android.content.Context c){}public View(){this(new android.content.Context());}
public ViewParent getParent(){return parent;}public void requestDisallowInterceptTouchEvent(boolean yes){}public android.content.Context getContext(){return new android.content.Context();}public android.content.res.Resources getResources(){return getContext().getResources();}
public View getRootView(){return parent==null?this:parent.getRootView();}public boolean postDelayed(Runnable r,long d){pending.add(r);return true;}public boolean removeCallbacks(Runnable r){return pending.remove(r);}public void tick(){ArrayList<Runnable> list=new ArrayList<Runnable>(pending);pending.clear();for(Runnable r:list)r.run();}
public void setOnClickListener(OnClickListener l){click=l;}public void setClickable(boolean c){clickable=c;}public void setAlpha(float v){}public void bringToFront(){}public void setElevation(float v){}public void setBackgroundDrawable(Object b){}public void invalidate(){}
public ViewPropertyAnimator animate(){return new ViewPropertyAnimator();}public int getWidth(){return 300;}public int getHeight(){return 50;}public boolean isAttachedToWindow(){return attached;}public int getWindowVisibility(){return visibility;}
public ViewGroup.LayoutParams params;public ViewGroup.LayoutParams getLayoutParams(){return params;}public void setLayoutParams(ViewGroup.LayoutParams p){params=p;}protected void onDraw(android.graphics.Canvas c){}
}""",
'android/view/ViewPropertyAnimator.java':'package android.view;public class ViewPropertyAnimator {Runnable end;public ViewPropertyAnimator alpha(float f){return this;}public ViewPropertyAnimator translationY(float f){return this;}public ViewPropertyAnimator setStartDelay(long d){return this;}public ViewPropertyAnimator setDuration(long d){return this;}public ViewPropertyAnimator withEndAction(Runnable r){end=r;return this;}public void cancel(){}public void start(){if(end!=null)end.run();}}',
'android/view/ViewGroup.java':'package android.view;import java.util.*;public class ViewGroup extends View {public ArrayList<View> children=new ArrayList<View>();public boolean intercepted;public static class LayoutParams {public static int WRAP_CONTENT=-2;public int width,height;public LayoutParams(int w,int h){width=w;height=h;}}public ViewGroup(){super();}public ViewGroup(android.content.Context c){super(c);}public void addView(View v,LayoutParams p){children.add(v);v.parent=this;v.params=p;}public void removeView(View v){children.remove(v);v.parent=null;}public void requestDisallowInterceptTouchEvent(boolean yes){intercepted=yes;}public void setClipChildren(boolean b){}',
'android/widget/FrameLayout.java':'package android.widget;public class FrameLayout extends android.view.ViewGroup {public static class LayoutParams extends android.view.ViewGroup.LayoutParams {public int bottomMargin;public LayoutParams(int w,int h,int gravity){super(w,h);}}}',
'android/widget/LinearLayout.java':'package android.widget;public class LinearLayout extends android.view.ViewGroup {public static int HORIZONTAL=0;public LinearLayout(android.content.Context c){super(c);}public void setOrientation(int o){}public void setGravity(int g){}public void setPadding(int a,int b,int c,int d){}public static class LayoutParams extends android.view.ViewGroup.LayoutParams {public int leftMargin;public LayoutParams(int w,int h){super(w,h);}}}',
'android/widget/TextView.java':'package android.widget;public class TextView extends android.view.View {public String text;public TextView(android.content.Context c){super(c);}public void setText(String s){text=s;}public void setTextColor(int c){}public void setTextSize(float f){}public void setTypeface(Object o,int i){}public void setSingleLine(boolean b){}public void setHorizontallyScrolling(boolean b){}public void setGravity(int i){}public android.graphics.Paint getPaint(){return new android.graphics.Paint(0);}}',
'android/graphics/drawable/GradientDrawable.java':'package android.graphics.drawable;public class GradientDrawable {public void setColor(int c){}public void setCornerRadius(float f){}}',
'android/graphics/Typeface.java':'package android.graphics;public class Typeface {public static int BOLD=1;}',
'android/graphics/Paint.java':'package android.graphics;public class Paint {public static int ANTI_ALIAS_FLAG=1,FILTER_BITMAP_FLAG=2;public Paint(int f){}public void setColorFilter(Object o){}public float measureText(String s){return s.length()*7;}}',
'android/graphics/Bitmap.java':'package android.graphics;public class Bitmap {public int getWidth(){return 20;}public int getHeight(){return 26;}}',
'android/graphics/Canvas.java':'package android.graphics;public class Canvas {public void drawBitmap(Bitmap b,Rect r,RectF f,Paint p){}}',
'android/graphics/Rect.java':'package android.graphics;public class Rect {public void set(int a,int b,int c,int d){}}',
'android/graphics/RectF.java':'package android.graphics;public class RectF {public void set(float a,float b,float c,float d){}}',
'android/graphics/PorterDuff.java':'package android.graphics;public class PorterDuff {public enum Mode {SRC_IN}}',
'android/graphics/PorterDuffColorFilter.java':'package android.graphics;public class PorterDuffColorFilter {public PorterDuffColorFilter(int c,PorterDuff.Mode m){}}',
'android/view/animation/DecelerateInterpolator.java':'package android.view.animation;public class DecelerateInterpolator {}',
'android/animation/ValueAnimator.java':'package android.animation;public class ValueAnimator {int value;public interface AnimatorUpdateListener {void onAnimationUpdate(ValueAnimator v);}public static ValueAnimator ofInt(int a,int b){ValueAnimator v=new ValueAnimator();v.value=b;return v;}public void setDuration(long l){}public void setInterpolator(Object o){}public void addUpdateListener(AnimatorUpdateListener l){l.onAnimationUpdate(this);}public Object getAnimatedValue(){return value;}public void start(){}public void cancel(){}}',
'cat/narezany/margyt/Accent.java':'package cat.narezany.margyt;class Accent {static int colour(){return 0xffa4c9ff;}}',
'cat/narezany/margyt/LockArt.java':'package cat.narezany.margyt;class LockArt {static final int WIDTH=20,HEIGHT=26;static android.graphics.Bitmap get(boolean b){return null;}}'
}
STUBS['cat/narezany/margyt/PlayerTools.java']='package cat.narezany.margyt;class PlayerTools{static float rate=2;static int writes;static float speed(){return rate;}static boolean applyHoldRate(){writes++;return true;}}'
STUBS['android/view/ViewGroup.java']+='}'
HARNESS="""package external;import android.view.*;import cat.narezany.margyt.SpeedGesture;
public class SpeedHarness {
public static class Ability {boolean speed;public boolean R62(){return speed;}public void Hj(boolean b){speed=false;}}
public static class Edge {public Ability LLJL=new Ability();}
public static class Component {public Ability LLLILZJ;public Object LLLLIILL;}
public static class Listener {public Component l0=new Component();}
static void check(boolean b,String s){if(!b)throw new AssertionError(s);}static MotionEvent event(int a,int y){return new MotionEvent(a,100,y);}
public static void main(String[] args){
ViewGroup root=new ViewGroup();View target=new View();root.addView(target,new ViewGroup.LayoutParams(400,800));
Edge edge=new Edge();check(!SpeedGesture.onTouch(edge,target,event(0,100)),"Native DOWN still starts long press");edge.LLJL.speed=true;target.tick();check(root.children.size()==2,"No MOVE needed to display padlock pill");check(root.intercepted,"Pager must not steal the first downward move");
check(SpeedGesture.onTouch(edge,target,event(2,145)),"Downward swipe locks native x2");check(SpeedGesture.onTouch(edge,target,event(2,155)),"Further MOVE events after locking must not reach native speed cancellation");check(SpeedGesture.onTouch(edge,target,event(1,145)),"Release must be consumed when pinned");check(edge.LLJL.speed,"Speed persists after finger release");
View pill=root.children.get(1);check(pill.clickable && pill.click!=null,"Pinned pill is an unlock button");pill.click.onClick(pill);check(!edge.LLJL.speed,"Pill restores speed");pill.tick();check(root.children.size()==1,"Unlocked pill leaves root");
Listener old=new Listener();SpeedGesture.onTouch(old,target,event(0,100));old.l0.LLLILZJ=new Ability();old.l0.LLLILZJ.speed=true;target.tick();check(root.children.size()==2,"Lazy ability created after native DOWN is observed");SpeedGesture.onTouch(old,target,event(1,100));check(root.children.size()==1,"Unpinned hold releases UI");
Edge next=new Edge();SpeedGesture.onTouch(next,target,event(0,100));next.LLJL.speed=true;target.tick();SpeedGesture.onTouch(next,target,event(2,145));SpeedGesture.onTouch(next,target,event(1,145));
check(SpeedGesture.onTouch(next,target,event(0,100)),"Locked DOWN cannot queue another native long press");check(SpeedGesture.onTouch(next,target,event(2,150)),"Down chooses release while pinned");check(next.LLJL.speed,"Native 2x stays active until finger release");check(SpeedGesture.onTouch(next,target,event(2,50)),"Up can reverse the choice before release");SpeedGesture.onTouch(next,target,event(1,50));check(next.LLJL.speed,"Reversed choice stays pinned");SpeedGesture.onTouch(next,target,event(0,100));SpeedGesture.onTouch(next,target,event(2,150));SpeedGesture.onTouch(next,target,event(1,150));check(!next.LLJL.speed&&root.children.size()==1,"Down selection releases on UP");
Edge changed=new Edge();SpeedGesture.onTouch(changed,target,event(0,100));changed.LLJL.speed=true;target.tick();SpeedGesture.onTouch(changed,target,event(2,145));SpeedGesture.onTouch(changed,target,event(1,145));changed.LLJL.speed=false;root.children.get(1).tick();check(root.children.size()==1,"Native reset/video change clears stale lock without a touch");
Edge hidden=new Edge();SpeedGesture.onTouch(hidden,target,event(0,100));hidden.LLJL.speed=true;target.tick();SpeedGesture.onTouch(hidden,target,event(2,145));SpeedGesture.onTouch(hidden,target,event(1,145));target.visibility=8;root.children.get(1).tick();check(root.children.size()==1 && !hidden.LLJL.speed,"Leaving the screen releases pinned playback and UI");target.visibility=0;
Edge scroll=new Edge();SpeedGesture.onTouch(scroll,target,event(0,100));scroll.LLJL.speed=true;target.tick();SpeedGesture.onTouch(scroll,target,event(2,145));SpeedGesture.onTouch(scroll,target,event(2,50));check(scroll.LLJL.speed,"Down/up decision does not prematurely stop native speed");SpeedGesture.onTouch(scroll,target,event(1,50));check(!scroll.LLJL.speed && !root.intercepted && root.children.size()==1,"Reversed up decision leaves no lock or interception");
Edge cancelLock=new Edge();SpeedGesture.onTouch(cancelLock,target,event(0,100));cancelLock.LLJL.speed=true;target.tick();SpeedGesture.onTouch(cancelLock,target,event(2,150));SpeedGesture.onTouch(cancelLock,target,event(3,150));check(!cancelLock.LLJL.speed&&root.children.size()==1,"CANCEL never pins speed");
Edge stress=new Edge();for(int n=0;n<2400;n++){SpeedGesture.onTouch(stress,target,event(0,100));stress.LLJL.speed=true;target.tick();View running=root.children.get(1);SpeedGesture.onTouch(stress,target,event(2,150));SpeedGesture.onTouch(stress,target,event(2,50));SpeedGesture.onTouch(stress,target,event(2,150));check(running.pending.size()<=2,"Only current monitor and transition are queued");SpeedGesture.onTouch(stress,target,event(3,150));check(running.pending.isEmpty() && root.children.size()==1,"No old pill tasks survive cancellation");}
Listener canceled=new Listener();SpeedGesture.onTouch(canceled,target,event(0,100));SpeedGesture.onTouch(canceled,target,event(3,100));canceled.l0.LLLILZJ=new Ability();canceled.l0.LLLILZJ.speed=true;target.tick();check(root.children.size()==1,"Canceled hold cannot resurrect a pill");
}}
"""
class SpeedRuntimeTest(unittest.TestCase):
 @unittest.skipUnless(shutil.which('java') and shutil.which('javac'),'JDK required')
 def test_native_and_lazy_hold_lock_release(self):
  with tempfile.TemporaryDirectory() as directory:
   work=pathlib.Path(directory);sources=dict(STUBS)
   sources['android/content/res/Resources.java']=sources['android/content/res/Resources.java'].replace('public float density=1;', 'public float density=1;public int widthPixels=400;')
   sources['android/text/TextUtils.java']='package android.text;public class TextUtils {public enum TruncateAt {END}}'
   sources['android/widget/TextView.java']=sources['android/widget/TextView.java'].replace('public void setText(String s)', 'public void setEllipsize(android.text.TextUtils.TruncateAt t){}public void setText(String s)')
   sources['android/widget/LinearLayout.java']=sources['android/widget/LinearLayout.java'].replace('public LayoutParams(int w,int h){super(w,h);}', 'public LayoutParams(int w,int h){super(w,h);}public LayoutParams(int w,int h,float weight){super(w,h);}')
   sources['cat/narezany/margyt/Fonts.java']='package cat.narezany.margyt;class Fonts {static void apply(android.widget.TextView view){}}'

   sources['cat/narezany/margyt/SpeedGesture.java']=(ROOT/'inject/java/cat/narezany/margyt/SpeedGesture.java').read_text(encoding='utf-8')
   sources['android/view/View.java']=sources['android/view/View.java'].replace('public void setAlpha(float v){}','public void setAlpha(float v){}public void setScaleX(float v){}public boolean isShown(){return visibility==0 && (parent==null || parent.isShown());}')
   sources['android/view/ViewPropertyAnimator.java']=sources['android/view/ViewPropertyAnimator.java'].replace('public ViewPropertyAnimator alpha(', 'public ViewPropertyAnimator scaleX(float v){return this;}public ViewPropertyAnimator setInterpolator(Object v){return this;}public ViewPropertyAnimator alpha(')
   sources['external/SpeedHarness.java']=HARNESS
   for name,value in sources.items():
    target=work/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(value,encoding='utf-8')
   built=subprocess.run(['javac','-source','8','-target','8','-encoding','UTF-8','-d',str(work)]+[str(work/name) for name in sources],capture_output=True,text=True)
   self.assertEqual(0,built.returncode,built.stderr)
   run=subprocess.run(['java','-cp',str(work),'external.SpeedHarness'],capture_output=True,text=True)
   self.assertEqual(0,run.returncode,run.stderr)

class NativeSpeedPatchTest(unittest.TestCase):
 def test_actual_edge_entry_is_required_and_patched_once(self):
  import sys
  sys.path.insert(0,str(ROOT))
  from margyt import dexpatch
  self.assertTrue(dexpatch.carries_an_anchor(b"LX/16wu;"))
  with tempfile.TemporaryDirectory() as directory:
   path=pathlib.Path(directory)/'Edge.smali'
   source=(".class public final LX/16wu;\n.super Ljava/lang/Object;\n"
           +dexpatch.SPEED_EDGE_METHOD+"\n    .registers 11\n"
           +"    const/4 v5, 0x0\n    if-nez p2, :cond_4\n    return v5\n"
           +"    :cond_4\n    iget-object v4, p0, LX/16wu;->LLJL:Ljava/lang/Object;\n"
           +"    if-nez v4, :cond_9\n    return v5\n    :cond_9\n"
           +"    invoke-virtual {p2}, Landroid/view/MotionEvent;->getActionMasked()I\n"
           +"    move-result v1\n    return v5\n.end method\n")
   path.write_text(source,encoding='utf-8')
   expected={"native edge speed lock":1}
   self.assertEqual(expected,dexpatch.rewrite_speed_touch(directory))
   patched=path.read_text(encoding='utf-8')
   self.assertLess(patched.index(':cond_9\n'),patched.index(dexpatch.SPEED_TOUCH_TARGET))
   self.assertLess(patched.index(dexpatch.SPEED_TOUCH_TARGET),patched.index('getActionMasked'))
   self.assertEqual(expected,dexpatch.rewrite_speed_touch(directory))
   self.assertEqual(1,path.read_text(encoding='utf-8').count(dexpatch.SPEED_TOUCH_TARGET))


