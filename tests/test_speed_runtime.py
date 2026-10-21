"""Run the actual injected speed hook, including delayed native initialization."""

import pathlib, shutil, subprocess, tempfile, unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
STUBS = {
    "android/content/Context.java": "package android.content;public class Context {public android.content.res.Resources getResources(){return new android.content.res.Resources();}}",
    "android/content/res/Resources.java": "package android.content.res;public class Resources {public static class Metrics {public float density=1;}public Metrics getDisplayMetrics(){return new Metrics();}public int getIdentifier(String a,String b,String c){return 0;}public int getDimensionPixelSize(int a){return 0;}}",
    "android/view/ViewParent.java": "package android.view;public interface ViewParent {ViewParent getParent();void requestDisallowInterceptTouchEvent(boolean yes);}",
    "android/view/MotionEvent.java": "package android.view;public class MotionEvent {public static final int ACTION_DOWN=0,ACTION_UP=1,ACTION_MOVE=2,ACTION_CANCEL=3;int action;float x,y;public MotionEvent(int a,float x,float y){action=a;this.x=x;this.y=y;}public int getActionMasked(){return action;}public float getRawX(){return x;}public float getRawY(){return y;}}",
    "android/view/Gravity.java": "package android.view;public class Gravity {public static final int BOTTOM=1,CENTER_HORIZONTAL=2,CENTER=3,CENTER_VERTICAL=4;}",
    "android/view/View.java": "package android.view;import java.util.*;public class View implements ViewParent {\npublic static final int VISIBLE=0;public ViewGroup parent;public boolean attached=true,clickable;public int visibility=0;public OnClickListener click;\npublic ArrayList<Runnable> pending=new ArrayList<Runnable>();public interface OnClickListener {void onClick(View v);}public View(android.content.Context c){}public View(){this(new android.content.Context());}\npublic ViewParent getParent(){return parent;}public void requestDisallowInterceptTouchEvent(boolean yes){}public android.content.Context getContext(){return new android.content.Context();}public android.content.res.Resources getResources(){return getContext().getResources();}\npublic View getRootView(){return parent==null?this:parent.getRootView();}public boolean postDelayed(Runnable r,long d){pending.add(r);return true;}public boolean removeCallbacks(Runnable r){return pending.remove(r);}public void tick(){ArrayList<Runnable> list=new ArrayList<Runnable>(pending);pending.clear();for(Runnable r:list)r.run();}\npublic void setOnClickListener(OnClickListener l){click=l;}public void setClickable(boolean c){clickable=c;}public void setAlpha(float v){}public void bringToFront(){}public void setElevation(float v){}public void setBackgroundDrawable(Object b){}public void invalidate(){}\npublic ViewPropertyAnimator animate(){return new ViewPropertyAnimator();}public int getWidth(){return 300;}public int getHeight(){return 50;}public boolean isAttachedToWindow(){return attached;}public int getWindowVisibility(){return visibility;}\npublic ViewGroup.LayoutParams params;public ViewGroup.LayoutParams getLayoutParams(){return params;}public void setLayoutParams(ViewGroup.LayoutParams p){params=p;}protected void onDraw(android.graphics.Canvas c){}\n}",
    "android/view/ViewPropertyAnimator.java": "package android.view;public class ViewPropertyAnimator {Runnable end;public ViewPropertyAnimator alpha(float f){return this;}public ViewPropertyAnimator translationY(float f){return this;}public ViewPropertyAnimator setStartDelay(long d){return this;}public ViewPropertyAnimator setDuration(long d){return this;}public ViewPropertyAnimator withEndAction(Runnable r){end=r;return this;}public void cancel(){}public void start(){if(end!=null)end.run();}}",
    "android/view/ViewGroup.java": "package android.view;import java.util.*;public class ViewGroup extends View {public ArrayList<View> children=new ArrayList<View>();public boolean intercepted;public static class LayoutParams {public static int WRAP_CONTENT=-2;public int width,height;public LayoutParams(int w,int h){width=w;height=h;}}public ViewGroup(){super();}public ViewGroup(android.content.Context c){super(c);}public void addView(View v,LayoutParams p){children.add(v);v.parent=this;v.params=p;}public void removeView(View v){children.remove(v);v.parent=null;}public void requestDisallowInterceptTouchEvent(boolean yes){intercepted=yes;}public void setClipChildren(boolean b){}",
    "android/widget/FrameLayout.java": "package android.widget;public class FrameLayout extends android.view.ViewGroup {public static class LayoutParams extends android.view.ViewGroup.LayoutParams {public int bottomMargin;public LayoutParams(int w,int h,int gravity){super(w,h);}}}",
    "android/widget/LinearLayout.java": "package android.widget;public class LinearLayout extends android.view.ViewGroup {public static int HORIZONTAL=0;public LinearLayout(android.content.Context c){super(c);}public void setOrientation(int o){}public void setGravity(int g){}public void setPadding(int a,int b,int c,int d){}public static class LayoutParams extends android.view.ViewGroup.LayoutParams {public int leftMargin;public LayoutParams(int w,int h){super(w,h);}}}",
    "android/widget/TextView.java": "package android.widget;public class TextView extends android.view.View {public String text;public TextView(android.content.Context c){super(c);}public void setText(String s){text=s;}public void setTextColor(int c){}public void setTextSize(float f){}public void setTypeface(Object o,int i){}public void setSingleLine(boolean b){}public void setHorizontallyScrolling(boolean b){}public void setGravity(int i){}public android.graphics.Paint getPaint(){return new android.graphics.Paint(0);}}",
    "android/graphics/drawable/GradientDrawable.java": "package android.graphics.drawable;public class GradientDrawable {public void setColor(int c){}public void setCornerRadius(float f){}}",
    "android/graphics/Typeface.java": "package android.graphics;public class Typeface {public static int BOLD=1;}",
    "android/graphics/Paint.java": "package android.graphics;public class Paint {public static int ANTI_ALIAS_FLAG=1,FILTER_BITMAP_FLAG=2;public Paint(int f){}public void setColorFilter(Object o){}public float measureText(String s){return s.length()*7;}}",
    "android/graphics/Bitmap.java": "package android.graphics;public class Bitmap {public int getWidth(){return 20;}public int getHeight(){return 26;}}",
    "android/graphics/Canvas.java": "package android.graphics;public class Canvas {public void drawBitmap(Bitmap b,Rect r,RectF f,Paint p){}}",
    "android/graphics/Rect.java": "package android.graphics;public class Rect {public void set(int a,int b,int c,int d){}}",
    "android/graphics/RectF.java": "package android.graphics;public class RectF {public void set(float a,float b,float c,float d){}}",
    "android/graphics/PorterDuff.java": "package android.graphics;public class PorterDuff {public enum Mode {SRC_IN}}",
    "android/graphics/PorterDuffColorFilter.java": "package android.graphics;public class PorterDuffColorFilter {public PorterDuffColorFilter(int c,PorterDuff.Mode m){}}",
    "android/view/animation/DecelerateInterpolator.java": "package android.view.animation;public class DecelerateInterpolator {}",
    "android/animation/ValueAnimator.java": "package android.animation;public class ValueAnimator {int value;public interface AnimatorUpdateListener {void onAnimationUpdate(ValueAnimator v);}public static ValueAnimator ofInt(int a,int b){ValueAnimator v=new ValueAnimator();v.value=b;return v;}public void setDuration(long l){}public void setInterpolator(Object o){}public void addUpdateListener(AnimatorUpdateListener l){l.onAnimationUpdate(this);}public Object getAnimatedValue(){return value;}public void start(){}public void cancel(){}}",
    "cat/ttcuz/framework/Accent.java": "package cat.ttcuz.framework;class Accent {static int colour(){return 0xffa4c9ff;}}",
    "cat/ttcuz/framework/LockArt.java": "package cat.ttcuz.framework;class LockArt {static final int WIDTH=20,HEIGHT=26;static android.graphics.Bitmap get(boolean b){return null;}}",
}
STUBS["cat/ttcuz/framework/PlayerTools.java"] = (
    "package cat.ttcuz.framework;class PlayerTools{static float rate=2;static int writes;static float speed(){return rate;}static boolean applyHoldRate(){writes++;return true;}}"
)
STUBS["android/graphics/Path.java"] = "package android.graphics;public class Path {}"
STUBS["android/view/ViewGroup.java"] += "}"
HARNESS = """package external;import android.view.*;import cat.ttcuz.framework.SpeedGesture;
public class SpeedHarness {
 public static class Ability {boolean speed;public boolean R62(){return speed;}public void Hj(boolean b){speed=false;}}
 public static class Edge {public Ability LLJL=new Ability();}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void rate(float rate)throws Exception{Class<?> t=Class.forName("cat.ttcuz.framework.PlayerTools");java.lang.reflect.Field f=t.getDeclaredField("rate");f.setAccessible(true);f.setFloat(null,rate);}
 static int writes()throws Exception{java.lang.reflect.Field f=Class.forName("cat.ttcuz.framework.PlayerTools").getDeclaredField("writes");f.setAccessible(true);return f.getInt(null);}
 public static void main(String[] args)throws Exception{
  ViewGroup root=new ViewGroup();View a=new View(),b=new View();root.addView(a,new ViewGroup.LayoutParams(400,800));Edge edge=new Edge();
  for(int action:new int[]{0,2,1,0,2,1,3})check(!SpeedGesture.onTouch(edge,a,new MotionEvent(action,100,150)),"All lock and unlock gestures must reach TikTok");
  check(a.pending.isEmpty(),"Native 2x requires no polling");rate(1.5f);
  SpeedGesture.onTouch(edge,a,new MotionEvent(0,100,100));SpeedGesture.onTouch(edge,b,new MotionEvent(0,100,100));check(a.pending.isEmpty()&&b.pending.size()==1,"New target cancels previous watcher");
  edge.LLJL.speed=true;b.tick();check(writes()==1,"Configured hold rate applied after native initialization");
  edge.LLJL.speed=false;SpeedGesture.onTouch(edge,a,new MotionEvent(0,100,100));SpeedGesture.onTouch(edge,a,new MotionEvent(3,100,100));edge.LLJL.speed=true;a.tick();check(writes()==1,"Cancel cannot apply hold rate later");
  check(root.children.size()==1&&!root.intercepted,"No competing overlay or gesture interception");
  android.graphics.Paint p=new android.graphics.Paint(0);p.setColor(0x66ffffff);android.graphics.Canvas c=new android.graphics.Canvas();SpeedGesture.drawPath(c,new android.graphics.Path(),p);
  check(c.drawn==0x66a4c9ff&&p.getColor()==0x66ffffff,"Native ripple follows accent, preserves alpha and restores paint");
 }
}
"""


class SpeedRuntimeTest(unittest.TestCase):
    @unittest.skipUnless(shutil.which("java") and shutil.which("javac"), "JDK required")
    def test_native_and_lazy_hold_lock_release(self):
        with tempfile.TemporaryDirectory() as directory:
            work = pathlib.Path(directory)
            sources = dict(STUBS)
            sources["android/graphics/Paint.java"] = sources["android/graphics/Paint.java"].replace(
                "class Paint {",
                "class Paint {public int colour;public int getColor(){return colour;}public void setColor(int c){colour=c;}",
            )
            sources["android/graphics/Canvas.java"] = sources[
                "android/graphics/Canvas.java"
            ].replace(
                "class Canvas {",
                "class Canvas {public int drawn;public void drawPath(Path p,Paint paint){drawn=paint.getColor();}",
            )
            sources["android/content/res/Resources.java"] = sources[
                "android/content/res/Resources.java"
            ].replace(
                "public float density=1;", "public float density=1;public int widthPixels=400;"
            )
            sources["android/text/TextUtils.java"] = (
                "package android.text;public class TextUtils {public enum TruncateAt {END}}"
            )
            sources["android/widget/TextView.java"] = sources[
                "android/widget/TextView.java"
            ].replace(
                "public void setText(String s)",
                "public void setEllipsize(android.text.TextUtils.TruncateAt t){}public void setText(String s)",
            )
            sources["android/widget/LinearLayout.java"] = sources[
                "android/widget/LinearLayout.java"
            ].replace(
                "public LayoutParams(int w,int h){super(w,h);}",
                "public LayoutParams(int w,int h){super(w,h);}public LayoutParams(int w,int h,float weight){super(w,h);}",
            )
            sources["cat/ttcuz/framework/Fonts.java"] = (
                "package cat.ttcuz.framework;class Fonts {static void apply(android.widget.TextView view){}}"
            )

            sources["cat/ttcuz/framework/SpeedGesture.java"] = (
                ROOT / "inject/java/cat/ttcuz/framework/SpeedGesture.java"
            ).read_text(encoding="utf-8")
            sources["android/view/View.java"] = sources["android/view/View.java"].replace(
                "public void setAlpha(float v){}",
                "public void setAlpha(float v){}public void setScaleX(float v){}public boolean isShown(){return visibility==0 && (parent==null || parent.isShown());}",
            )
            sources["android/view/ViewPropertyAnimator.java"] = sources[
                "android/view/ViewPropertyAnimator.java"
            ].replace(
                "public ViewPropertyAnimator alpha(",
                "public ViewPropertyAnimator scaleX(float v){return this;}public ViewPropertyAnimator setInterpolator(Object v){return this;}public ViewPropertyAnimator alpha(",
            )
            sources["external/SpeedHarness.java"] = HARNESS
            for name, value in sources.items():
                target = work / name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(value, encoding="utf-8")
            built = subprocess.run(
                ["javac", "-source", "8", "-target", "8", "-encoding", "UTF-8", "-d", str(work)]
                + [str(work / name) for name in sources],
                capture_output=True,
                text=True,
            )
            self.assertEqual(0, built.returncode, built.stderr)
            run = subprocess.run(
                ["java", "-cp", str(work), "external.SpeedHarness"], capture_output=True, text=True
            )
            self.assertEqual(0, run.returncode, run.stderr)


class NativeSpeedPatchTest(unittest.TestCase):
    def test_actual_edge_entry_is_required_and_patched_once(self):
        import sys

        sys.path.insert(0, str(ROOT))
        from ttcuz import dexpatch

        self.assertTrue(dexpatch.carries_an_anchor(b"LX/16wu;"))
        with tempfile.TemporaryDirectory() as directory:
            path = pathlib.Path(directory) / "Edge.smali"
            source = (
                ".class public final LX/16wu;\n.super Ljava/lang/Object;\n"
                + dexpatch.SPEED_EDGE_METHOD
                + "\n    .registers 11\n"
                + "    const/4 v5, 0x0\n    if-nez p2, :cond_4\n    return v5\n"
                + "    :cond_4\n    iget-object v4, p0, LX/16wu;->LLJL:Ljava/lang/Object;\n"
                + "    if-nez v4, :cond_9\n    return v5\n    :cond_9\n"
                + "    invoke-virtual {p2}, Landroid/view/MotionEvent;->getActionMasked()I\n"
                + "    move-result v1\n    return v5\n.end method\n"
            )
            path.write_text(source, encoding="utf-8")
            expected = {"native edge speed lock": 1}
            self.assertEqual(expected, dexpatch.rewrite_speed_touch(directory))
            patched = path.read_text(encoding="utf-8")
            self.assertLess(patched.index(":cond_9\n"), patched.index(dexpatch.SPEED_TOUCH_TARGET))
            self.assertLess(
                patched.index(dexpatch.SPEED_TOUCH_TARGET), patched.index("getActionMasked")
            )
            self.assertEqual(expected, dexpatch.rewrite_speed_touch(directory))
            self.assertEqual(1, path.read_text(encoding="utf-8").count(dexpatch.SPEED_TOUCH_TARGET))


STUBS["android/view/animation/PathInterpolator.java"] = (
    "package android.view.animation;public class PathInterpolator {public PathInterpolator(float a,float b,float c,float d){}}"
)
STUBS["cat/ttcuz/framework/Motion.java"] = (
    "package cat.ttcuz.framework;class Motion {static final android.view.animation.PathInterpolator OUT=new android.view.animation.PathInterpolator(0,0,0,0),IN=OUT,BOTH=OUT;static final long SHORT=180L,MEDIUM=300L,LONG=340L;static boolean enabled(android.content.Context c){return true;}}"
)
