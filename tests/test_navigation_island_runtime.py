"""Exercise native-tab preservation, selection motion and dimming compatibility."""

from pathlib import Path
import unittest
import subprocess
from test_dim_runtime import STUBS
import test_settings_safety_runtime as safety

ROOT = Path(__file__).resolve().parents[1]


class NavigationIslandTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_native_tabs_badges_motion_and_lifecycle(self):
        files = dict(STUBS)
        files["cat/ttcuz/framework/Diary.java"] = (
            'package cat.ttcuz.framework;class Diary{static void note(String s){if(!s.equals("navigation: MD3 island attached"))throw new AssertionError(s);}}'
        )
        files["android/view/View.java"] = files["android/view/View.java"].replace(
            "public int getId()",
            """public android.view.ViewGroup.LayoutParams params=new android.view.ViewGroup.MarginLayoutParams();
        public int pl,pr,pt,pb;public Runnable click;public boolean performClick(){if(click!=null)click.run();return true;}
        public float getX(){return left+x;}public float getY(){return top+y;}
        public android.content.Context getContext(){return new android.content.Context();}
        public ViewGroup.LayoutParams getLayoutParams(){return params;}public void setLayoutParams(ViewGroup.LayoutParams p){params=p;}
        public void setBackground(android.graphics.drawable.Drawable d){background=d;}public void setElevation(float e){}
        public void setPadding(int l,int t,int r,int b){pl=l;pt=t;pr=r;pb=b;}
        public int getPaddingLeft(){return pl;}public int getPaddingRight(){return pr;}public int getPaddingTop(){return pt;}public int getPaddingBottom(){return pb;}
        public java.util.ArrayList<Runnable> frames=new java.util.ArrayList<>();public void postOnAnimation(Runnable r){frames.add(r);}
        public boolean focused=true;public boolean hasWindowFocus(){return focused;}
        public int getId()""",
        )
        files["android/view/View.java"] = (
            files["android/view/View.java"]
            .replace("public boolean hasWindowFocus(){return true;}", "")
            .replace("delayed.remove(r);", "delayed.remove(r);frames.remove(r);")
        )
        files["android/view/ViewGroup.java"] = files["android/view/ViewGroup.java"].replace(
            "private java.util.List<View>",
            "public static class LayoutParams{public int width=-1,height=64;}public static class MarginLayoutParams extends LayoutParams{public int leftMargin,rightMargin,bottomMargin;}private java.util.List<View>",
        )
        files["android/view/ViewTreeObserver.java"] = files[
            "android/view/ViewTreeObserver.java"
        ].replace(
            "public OnGlobalLayoutListener layout;",
            "public boolean isAlive(){return true;}public void removeOnPreDrawListener(OnPreDrawListener l){if(draw==l)draw=null;}public OnGlobalLayoutListener layout;",
        )
        files["android/widget/TextView.java"] = files["android/widget/TextView.java"].replace(
            "public CharSequence text;",
            "public int colour=77;public int getCurrentTextColor(){return colour;}public void setTextColor(int c){colour=c;}public CharSequence text;",
        )
        files["android/widget/ImageView.java"] = (
            "package android.widget;public class ImageView extends android.view.View{public android.content.res.ColorStateList tint;public android.content.res.ColorStateList getImageTintList(){return tint;}public void setImageTintList(android.content.res.ColorStateList c){tint=c;}}"
        )
        files["android/content/res/ColorStateList.java"] = (
            "package android.content.res;public class ColorStateList{public static ColorStateList valueOf(int c){return new ColorStateList();}}"
        )
        files["android/graphics/Color.java"] = (
            "package android.graphics;public class Color{public static int BLACK=0xff000000;public static int rgb(int r,int g,int b){return 0xff000000|r<<16|g<<8|b;}public static int red(int c){return c>>>16&255;}public static int green(int c){return c>>>8&255;}public static int blue(int c){return c&255;}}"
        )
        files["android/graphics/RectF.java"] = (
            """package android.graphics;public class RectF{public float left,top,right,bottom;public RectF(){}public RectF(float l,float t,float r,float b){set(l,t,r,b);}public RectF(RectF r){set(r);}public RectF(Rect r){set(r.left,r.top,r.right,r.bottom);}public void set(float l,float t,float r,float b){left=l;top=t;right=r;bottom=b;}public void set(RectF r){set(r.left,r.top,r.right,r.bottom);}public float height(){return bottom-top;}public float width(){return right-left;}public boolean equals(Object o){if(!(o instanceof RectF))return false;RectF r=(RectF)o;return left==r.left&&top==r.top&&right==r.right&&bottom==r.bottom;}}"""
        )
        files["android/graphics/Paint.java"] = (
            "package android.graphics;public class Paint{public static int ANTI_ALIAS_FLAG=1;public Paint(int f){}public void setColor(int c){}public void setAlpha(int a){}public void setColorFilter(ColorFilter f){}}"
        )
        files["android/graphics/Path.java"] = (
            "package android.graphics;public class Path{public enum Direction{CW}public void reset(){}public void addRoundRect(RectF r,float x,float y,Direction d){}}"
        )
        files["android/graphics/Canvas.java"] = (
            "package android.graphics;public class Canvas{public int rounds,clips;public int save(){return 1;}public boolean clipPath(Path p){clips++;return true;}public void restoreToCount(int n){}public void drawRoundRect(RectF r,float x,float y,Paint p){rounds++;}}"
        )
        files["android/graphics/ColorFilter.java"] = (
            "package android.graphics;public class ColorFilter{}"
        )
        files["android/graphics/PixelFormat.java"] = (
            "package android.graphics;public class PixelFormat{public static int TRANSLUCENT=-3;}"
        )
        files["android/graphics/drawable/Drawable.java"] = files[
            "android/graphics/drawable/Drawable.java"
        ].replace(
            "public int alpha=255;",
            "public android.graphics.Rect getBounds(){return new android.graphics.Rect();}public void invalidateSelf(){}public int alpha=255;",
        )
        # ValueAnimator.setInterpolator now lives in the shared animation doubles.
        files["android/animation/ValueAnimator.java"] = files[
            "android/animation/ValueAnimator.java"
        ].replace(
            "public void start(){",
            "public void advance(float f){value=f;if(update!=null)update.onAnimationUpdate(this);}public void start(){",
        )
        files["android/view/animation/PathInterpolator.java"] = (
            "package android.view.animation;public class PathInterpolator{public PathInterpolator(float a,float b,float c,float d){}}"
        )
        files["cat/ttcuz/framework/Accent.java"] = (
            "package cat.ttcuz.framework;class Accent{static int colour(){return 0xffee4455;}}"
        )
        files["cat/ttcuz/framework/Skin.java"] = (
            "package cat.ttcuz.framework;class Skin{int text=-1,card=0xff222222;int muted(){return 0xffbbbbbb;}static Skin tonal(android.content.Context c,int a){return new Skin();}}"
        )
        files["cat/ttcuz/framework/Motion.java"] = (
            "package cat.ttcuz.framework;class Motion{static final android.view.animation.PathInterpolator OUT=new android.view.animation.PathInterpolator(0,0,0,0),IN=OUT,BOTH=OUT;static final long SHORT=180L,MEDIUM=300L,LONG=340L;static boolean enabled=true;static boolean enabled(android.content.Context c){return enabled;}}"
        )
        files["cat/ttcuz/framework/IslandTouch.java"] = (
            "package cat.ttcuz.framework;class IslandTouch{static void hook(android.app.Activity a){}}"
        )
        for name in ["NavigationIsland", "BottomChrome"]:
            files["cat/ttcuz/framework/" + name + ".java"] = (
                ROOT / "inject/java/cat/ttcuz/framework" / f"{name}.java"
            ).read_text(encoding="utf8")
        files[
            "cat/ttcuz/framework/Harness.java"
        ] = """package cat.ttcuz.framework;import android.view.*;import android.widget.*;import android.graphics.*;
public class Harness{
 static int chosen(Object drawable)throws Exception{java.lang.reflect.Field f=drawable.getClass().getDeclaredField("selected");f.setAccessible(true);return f.getInt(drawable);}
 static RectF body(Object drawable)throws Exception{java.lang.reflect.Field f=drawable.getClass().getDeclaredField("body");f.setAccessible(true);return (RectF)f.get(drawable);}
 static RectF pill(Object drawable)throws Exception{java.lang.reflect.Field f=drawable.getClass().getDeclaredField("pill");f.setAccessible(true);return (RectF)f.get(drawable);}
 public static void run()throws Exception{
  com.ss.android.ugc.aweme.main.MainActivity activity=new com.ss.android.ugc.aweme.main.MainActivity();ViewGroup root=new ViewGroup();root.width=400;root.height=900;activity.window.decor=root;
  ViewGroup row=new ViewGroup();row.width=400;row.height=64;row.top=820;root.addView(row);String[] labels={"Главная","Сообщество","+","Входящие","Профиль"};ViewGroup[] tabs=new ViewGroup[5];final int[] clicks={0};
  for(int i=0;i<5;i++){tabs[i]=new ViewGroup();tabs[i].width=80;tabs[i].height=64;tabs[i].left=i*80;TextView title=new TextView();title.width=70;title.height=16;title.text=labels[i];tabs[i].addView(title);ImageView icon=new ImageView();icon.width=24;icon.height=24;tabs[i].addView(icon);tabs[i].click=()->clicks[0]++;row.addView(tabs[i]);}
  TextView badge=new TextView();badge.text="99+";tabs[3].addView(badge);tabs[0].selected=true;Runnable nativeClick=tabs[4].click;
  NavigationIsland.watch(activity);root.observer.draw.onPreDraw();Object background=row.background;
  if(!NavigationIsland.isRail(row)||chosen(background)!=0||row.getChildCount()!=5||tabs[4].click!=nativeClick)throw new AssertionError("Native controls were replaced");
  ViewGroup.MarginLayoutParams margins=(ViewGroup.MarginLayoutParams)row.params;if(margins.leftMargin!=0||margins.rightMargin!=0||margins.bottomMargin!=0||margins.height!=64||row.pl!=0||row.pt!=0||row.pr!=0||row.pb!=0)throw new AssertionError("Island changed the native layout (it must only paint inside it)");
  RectF body=body(background);if(body.left!=10||body.right!=390||body.top!=0||body.bottom>row.height||body.bottom<row.height-8)throw new AssertionError("Island is not painted symmetrically inside the native bounds");
  float equalWidth=pill(background).width();for(int i:new int[]{0,1,3,4}){for(ViewGroup tab:tabs)tab.selected=false;tabs[i].selected=true;root.observer.draw.onPreDraw();RectF p=pill(background);if(p.width()!=equalWidth||(p.left+p.right)/2f!=tabs[i].getX()+tabs[i].getWidth()/2f)throw new AssertionError("Tab indicator is off-centre or has a different width");}tabs[4].selected=false;tabs[0].selected=true;root.observer.draw.onPreDraw();
  for(int i=0;i<5;i++)for(int c=0;c<tabs[i].getChildCount();c++)if(tabs[i].getChildAt(c) instanceof ImageView){ImageView icon=(ImageView)tabs[i].getChildAt(c);if(i==2?icon.tint!=null:icon.tint==null)throw new AssertionError("Create icon must stay native; other icons must be tinted");}
  Canvas canvas=new Canvas();java.lang.reflect.Method draw=background.getClass().getDeclaredMethod("draw",Canvas.class);draw.setAccessible(true);draw.invoke(background,canvas);if(canvas.rounds!=2||canvas.clips!=1)throw new AssertionError("Extra create backing was drawn, or indicator was not clipped");
  if(badge.colour!=77||tabs[3].getChildAt(2)!=badge)throw new AssertionError("Unread badge changed");
  tabs[4].performClick();tabs[0].selected=false;tabs[4].selected=true;root.observer.draw.onPreDraw();
  if(clicks[0]!=1||chosen(background)!=4||pill(background).left!=336)throw new AssertionError("Selection did not follow the centre of the native tab");
  tabs[4].left-=2;root.observer.draw.onPreDraw();if((pill(background).left+pill(background).right)/2f!=358)throw new AssertionError("Indicator lost its native centre after a small nudge");tabs[4].left+=2;
  BottomChrome.protect(row);BottomChrome.repair();if(row.background!=background)throw new AssertionError("Dimming erased floating corners");
  Motion.enabled=false;tabs[4].selected=false;tabs[3].selected=true;root.observer.draw.onPreDraw();if(chosen(background)!=3||pill(background).left!=256)throw new AssertionError("Reduced motion did not settle");
  Motion.enabled=true;android.animation.ValueAnimator.manual=true;tabs[3].selected=false;tabs[0].selected=true;root.observer.draw.onPreDraw();if(pill(background).left!=256)throw new AssertionError("Pending animation did not retain its initial position");
  tabs[0].selected=false;tabs[3].selected=true;android.animation.ValueAnimator.manual=false;root.observer.draw.onPreDraw();float steady=pill(background).left;for(int i=0;i<5;i++)root.observer.draw.onPreDraw();if(pill(background).left!=steady||row.background!=background||row.params!=margins)throw new AssertionError("Steady frames changed island state");
  tabs[3].selected=false;tabs[0].selected=true;android.animation.ValueAnimator.manual=true;root.observer.draw.onPreDraw();float held=pill(background).left;tabs[0].left=tabs[0].left+2;root.observer.draw.onPreDraw();root.observer.draw.onPreDraw();if(pill(background).left!=held)throw new AssertionError("A geometry nudge restarted or snapped the running motion");tabs[0].left=0;root.observer.draw.onPreDraw();
  NavigationIsland.stop(activity);if(root.observer.draw!=null||pill(background).left!=16)throw new AssertionError("Stopped transition did not settle or listener survived stop");NavigationIsland.watch(activity);if(root.observer.draw==null)throw new AssertionError("Listener did not resume");android.animation.ValueAnimator.manual=false;
  android.animation.ValueAnimator.manual=false;tabs[0].selected=true;tabs[3].selected=false;root.observer.draw.onPreDraw();NavigationIsland.stepOverride=.016f;
  if(!NavigationIsland.hit(activity,40f,830f))throw new AssertionError("Indicator was not hit");
  if(NavigationIsland.hit(activity,300f,830f)||NavigationIsland.hit(activity,40f,900f))throw new AssertionError("Touch outside the indicator was taken");
  NavigationIsland.begin(activity,40f);if(!NavigationIsland.move(activity,290f))throw new AssertionError("Nearest-tab change was not reported");
  for(int i=0;i<40&&!row.frames.isEmpty();i++)row.frames.remove(0).run();
  if(pill(background).right-pill(background).left<=equalWidth)throw new AssertionError("Indicator did not grow while held");
  for(int i=0;i<3000&&!row.frames.isEmpty();i++)row.frames.remove(0).run();if(!row.frames.isEmpty())throw new AssertionError("An idle held indicator kept scheduling frames");
  if(clicks[0]!=1||row.getChildCount()!=5)throw new AssertionError("Dragging touched the native tabs");
  float[] tap=NavigationIsland.end(activity,true);if(tap==null||tap[0]!=280f||tap[1]!=852f)throw new AssertionError("Release did not target the nearest tab");
  if(chosen(background)!=3)throw new AssertionError("Release did not choose the nearest tab");
  for(int i=0;i<3000&&!row.frames.isEmpty();i++)row.frames.remove(0).run();
  if(Math.abs(pill(background).left-256)>.01f||Math.abs(pill(background).right-304)>.01f)throw new AssertionError("Indicator did not settle on the tab");
  tabs[0].selected=false;tabs[3].selected=true;root.observer.draw.onPreDraw();
  NavigationIsland.begin(activity,300f);NavigationIsland.move(activity,60f);if(NavigationIsland.end(activity,false)!=null)throw new AssertionError("A cancelled drag asked for a tap");
  for(int i=0;i<3000&&!row.frames.isEmpty();i++)row.frames.remove(0).run();
  if(chosen(background)!=3||Math.abs(pill(background).left-256)>.01f)throw new AssertionError("Cancelled drag did not return");
  if(NavigationIsland.end(activity,true)!=null)throw new AssertionError("Release without a drag asked for a tap");
  for(float edgeX:new float[]{-500f,900f}){NavigationIsland.begin(activity,300f);NavigationIsland.move(activity,edgeX);for(int i=0;i<60&&!row.frames.isEmpty();i++)row.frames.remove(0).run();RectF b=body(background),p=pill(background);if(p.left<b.left+6-.01f||p.right>b.right-6+.01f||p.top<b.top-.01f||p.bottom>b.bottom+.01f)throw new AssertionError("Held indicator left the island");NavigationIsland.end(activity,false);for(int i=0;i<3000&&!row.frames.isEmpty();i++)row.frames.remove(0).run();}
  if(chosen(background)!=3||Math.abs(pill(background).left-256)>.01f)throw new AssertionError("Edge drag did not return");
  NavigationIsland.begin(activity,280f);NavigationIsland.move(activity,130f);for(int i=0;i<20;i++)if(!row.frames.isEmpty())row.frames.remove(0).run();
  if(NavigationIsland.end(activity,true)==null)throw new AssertionError("Drag to the other tab was not committed");
  for(int i=0;i<4;i++)if(!row.frames.isEmpty())row.frames.remove(0).run();
  float gliding=(pill(background).left+pill(background).right)/2f;
  NavigationIsland.begin(activity,gliding);if(!row.frames.isEmpty())row.frames.remove(0).run();
  if(Math.abs((pill(background).left+pill(background).right)/2f-gliding)>25f)throw new AssertionError("Re-grabbing a gliding indicator made it jump");
  NavigationIsland.end(activity,false);for(int i=0;i<3000&&!row.frames.isEmpty();i++)row.frames.remove(0).run();
  float level=pill(background).top;tabs[1].top=7;tabs[3].selected=false;tabs[1].selected=true;root.observer.draw.onPreDraw();if(pill(background).top!=level)throw new AssertionError("Uneven native cells tilted the indicator");tabs[1].top=0;
  tabs[0].top=7;root.observer.draw.onPreDraw();if(pill(background).top!=level)throw new AssertionError("First-tab padding shifted the shared indicator line");tabs[0].top=0;root.observer.draw.onPreDraw();
  // A finished animator must not freeze subsequent geometry updates.
  tabs[1].left+=3;root.observer.draw.onPreDraw();if(pill(background).left!=99)throw new AssertionError("Finished animation retained stale geometry");tabs[1].left-=3;
  // Narrow rail: side tabs and fast switching stay inside, without changing native children.
  row.width=280;for(int i=0;i<5;i++){tabs[i].width=56;tabs[i].left=i*56;}
  for(int i:new int[]{0,4,1,3,0}){for(ViewGroup tab:tabs)tab.selected=false;tabs[i].selected=true;root.observer.draw.onPreDraw();RectF b=body(background),p=pill(background);if(p.left<b.left+6-.01f||p.right>b.right-6+.01f||p.top<b.top||p.bottom>b.bottom||p.width()!=30||(p.left+p.right)/2f!=tabs[i].getX()+tabs[i].getWidth()/2f)throw new AssertionError("Narrow rail overflowed or lost uniform centring");}
  row.width=400;for(int i=0;i<5;i++){tabs[i].width=80;tabs[i].left=i*80;}root.observer.draw.onPreDraw();
  NavigationIsland.begin(activity,46f);NavigationIsland.move(activity,280f);if(row.frames.isEmpty())throw new AssertionError("Drag did not start frames");NavigationIsland.stop(activity);if(!row.frames.isEmpty())throw new AssertionError("Pause left island callbacks queued");NavigationIsland.watch(activity);
  NavigationIsland.begin(activity,46f);NavigationIsland.move(activity,280f);row.focused=false;row.frames.remove(0).run();if(!row.frames.isEmpty())throw new AssertionError("Background island continued animating");row.focused=true;
  android.animation.ValueAnimator.manual=true;for(ViewGroup tab:tabs)tab.selected=false;tabs[1].selected=true;root.observer.draw.onPreDraw();
  for(float f:new float[]{.1f,.4f,.7f}){android.animation.ValueAnimator.last.advance(f);RectF p=pill(background);if(Math.abs(p.width()-equalWidth)>.01f||p.top!=level||p.left<body(background).left||p.right>body(background).right)throw new AssertionError("A transition stretched, tilted or overflowed");}
  float moving=pill(background).left;tabs[1].selected=false;tabs[4].selected=true;root.observer.draw.onPreDraw();if(Math.abs(pill(background).left-moving)>.01f)throw new AssertionError("Rapid switching snapped the cancelled animation");android.animation.ValueAnimator.last.advance(.5f);android.animation.ValueAnimator.last.finish();android.animation.ValueAnimator.manual=false;
  NavigationIsland.stepOverride=0f;
  ViewTreeObserver oldObserver=root.observer;root.observer=new ViewTreeObserver();NavigationIsland.watch(activity);if(root.observer.draw==null||oldObserver.draw!=null)throw new AssertionError("Island did not reconnect to a replaced observer");
  NavigationIsland.destroy(activity);if(root.observer.draw!=null||NavigationIsland.isRail(row))throw new AssertionError("Destroyed screen retained island state");
  android.app.Activity other=new android.app.Activity();other.window.decor=new ViewGroup();NavigationIsland.watch(other);if(other.window.decor.observer.draw!=null)throw new AssertionError("Unrelated screen was styled");
 }
}"""
        files["Test.java"] = (
            "public class Test{public static void main(String[] a)throws Exception{cat.ttcuz.framework.Harness.run();}}"
        )
        try:
            self.run_java(files)
        except subprocess.CalledProcessError as error:
            self.fail(error.stderr.decode("utf8", "replace"))
