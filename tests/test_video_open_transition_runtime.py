"""Run the actual card hit-test, passive Window.Callback and launch options on a JVM."""

from pathlib import Path
import tempfile
import unittest
import test_settings_safety_runtime as safety
from ttcuz import dexpatch
from ttcuz.build import Build
from unittest.mock import patch
import hashlib
import json
import os
import subprocess

ROOT = Path(__file__).resolve().parents[1]


class VideoOpenTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_native_launch_gestures_and_clipped_card_coordinates(self):
        files = {
            "android/content/Context.java": 'package android.content;public class Context{public String getPackageName(){return "host";}}',
            "android/content/ContextWrapper.java": "package android.content;public class ContextWrapper extends Context{Context base;public ContextWrapper(Context c){base=c;}public Context getBaseContext(){return base;}}",
            "android/content/ComponentName.java": "package android.content;public class ComponentName{String p,n;public ComponentName(String p,String n){this.p=p;this.n=n;}public String getPackageName(){return p;}public String getClassName(){return n;}}",
            "android/content/Intent.java": "package android.content;public class Intent{public ComponentName component;public Intent(ComponentName c){component=c;}public ComponentName getComponent(){return component;}}",
            "android/os/Bundle.java": "package android.os;public class Bundle{public android.view.View source;public int x,y,w,h;}",
            "android/os/SystemClock.java": "package android.os;public class SystemClock{public static long now=1000;public static long uptimeMillis(){return now;}}",
            "android/util/DisplayMetrics.java": "package android.util;public class DisplayMetrics{public float density=1;}",
            "android/content/res/Resources.java": "package android.content.res;public class Resources{public android.util.DisplayMetrics getDisplayMetrics(){return new android.util.DisplayMetrics();}}",
            "android/graphics/Rect.java": """package android.graphics;public class Rect{public int left,top,right,bottom;public int width(){return right-left;}public int height(){return bottom-top;}public boolean isEmpty(){return width()<=0||height()<=0;}public boolean contains(int x,int y){return x>=left&&x<right&&y>=top&&y<bottom;}public boolean intersect(int l,int t,int r,int b){if(left<r&&l<right&&top<b&&t<bottom){left=Math.max(left,l);top=Math.max(top,t);right=Math.min(right,r);bottom=Math.min(bottom,b);return true;}return false;}}""",
            "android/graphics/Outline.java": "package android.graphics;public class Outline{public void setRoundRect(int l,int t,int r,int b,float radius){}}",
            "android/view/ViewOutlineProvider.java": "package android.view;public abstract class ViewOutlineProvider{public abstract void getOutline(View v,android.graphics.Outline o);}",
            "android/view/ViewTreeObserver.java": "package android.view;public class ViewTreeObserver{public interface OnPreDrawListener{boolean onPreDraw();}public void addOnPreDrawListener(OnPreDrawListener l){}}",
            "android/view/View.java": """package android.view;
public class View{public android.content.Context context;public View parent;public int x,y,w=100,h=150,clipTop=0,clipBottom=150;public boolean shown=true,attached=true;public float alpha=1;
java.util.Map<Integer,Object> tags=new java.util.HashMap<>();public View(android.content.Context c){context=c;}public android.content.Context getContext(){return context;}
public int getWidth(){return w;}public int getHeight(){return h;}public boolean isShown(){return shown&&(parent==null||parent.isShown());}public boolean isAttachedToWindow(){return attached&&(parent==null||parent.isAttachedToWindow());}
public View getRootView(){return parent==null?this:parent.getRootView();}public Object getTag(int id){return tags.get(id);}public void setTag(int id,Object v){tags.put(id,v);}
public Object getParent(){return parent;}public float getAlpha(){return alpha;}
public android.content.res.Resources getResources(){return new android.content.res.Resources();}public ViewTreeObserver getViewTreeObserver(){return new ViewTreeObserver();}
public void setOutlineProvider(ViewOutlineProvider p){}public void setClipToOutline(boolean b){}
public boolean getLocalVisibleRect(android.graphics.Rect r){r.left=0;r.top=clipTop;r.right=w;r.bottom=clipBottom;return isShown()&&!r.isEmpty();}
public boolean getGlobalVisibleRect(android.graphics.Rect r){boolean b=getLocalVisibleRect(r);int[] p=new int[2];getLocationOnScreen(p);r.left+=p[0];r.right+=p[0];r.top+=p[1];r.bottom+=p[1];return b;}
public void getLocationOnScreen(int[] p){p[0]=x;p[1]=y;for(View v=parent;v!=null;v=v.parent){p[0]+=v.x;p[1]+=v.y;}}
public boolean postDelayed(Runnable r,long ms){return true;}public boolean removeCallbacks(Runnable r){return true;}public boolean performHapticFeedback(int c){return true;}}
""",
            "android/view/ViewGroup.java": "package android.view;public class ViewGroup extends View{java.util.List<View> children=new java.util.ArrayList<>();public ViewGroup(android.content.Context c){super(c);}public void addView(View v){children.add(v);v.parent=this;}public int getChildCount(){return children.size();}public View getChildAt(int i){return children.get(i);}}",
            "androidx/recyclerview/widget/RecyclerView.java": "package androidx.recyclerview.widget;public class RecyclerView extends android.view.ViewGroup{public RecyclerView(android.content.Context c){super(c);}}",
            "android/view/Window.java": "package android.view;public class Window{public interface Callback{boolean dispatchTouchEvent(MotionEvent e);}public View decor;public Callback callback;public View getDecorView(){return decor;}public Callback getCallback(){return callback;}public void setCallback(Callback c){callback=c;}}",
            "android/view/MotionEvent.java": """package android.view;public class MotionEvent{public static final int ACTION_DOWN=0,ACTION_UP=1,ACTION_MOVE=2,ACTION_CANCEL=3,ACTION_POINTER_DOWN=5;public int action,pointers=1;public float x,y;public long down,time;public static MotionEvent obtain(long d,long t,int a,float x,float y,int flags){MotionEvent e=new MotionEvent();e.down=d;e.time=t;e.action=a;e.x=x;e.y=y;return e;}public int getActionMasked(){return action;}public int getPointerCount(){return pointers;}public float getRawX(){return x;}public float getRawY(){return y;}public long getDownTime(){return down;}public long getEventTime(){return time;}public void recycle(){}}""",
            "android/view/ViewConfiguration.java": "package android.view;public class ViewConfiguration{public static ViewConfiguration get(android.content.Context c){return new ViewConfiguration();}public int getScaledTouchSlop(){return 8;}public static int getLongPressTimeout(){return 500;}}",
            "android/view/HapticFeedbackConstants.java": "package android.view;public class HapticFeedbackConstants{public static final int LONG_PRESS=0,CLOCK_TICK=1;}",
            "android/app/Activity.java": "package android.app;public class Activity extends android.content.Context{public android.view.Window window=new android.view.Window();public android.view.Window getWindow(){return window;}}",
            "android/app/ActivityOptions.java": """package android.app;public class ActivityOptions{android.os.Bundle b=new android.os.Bundle();public static boolean fail;public static ActivityOptions makeScaleUpAnimation(android.view.View v,int x,int y,int w,int h){if(fail)throw new IllegalStateException("unsupported");ActivityOptions a=new ActivityOptions();a.b.source=v;a.b.x=x;a.b.y=y;a.b.w=w;a.b.h=h;return a;}public android.os.Bundle toBundle(){return b;}}""",
            "cat/ttcuz/framework/Motion.java": "package cat.ttcuz.framework;class Motion{static boolean enabled=true;static boolean enabled(android.content.Context c){return enabled;}}",
            "cat/ttcuz/framework/ProfileLayout.java": "package cat.ttcuz.framework;import android.view.View;class ProfileLayout{static boolean visible(android.view.View v){return true;}"
            + safety.method("ProfileLayout.java", " static boolean onScreen(")
            + "}",
            "cat/ttcuz/framework/Diary.java": "package cat.ttcuz.framework;class Diary{static int notes;static void note(String s){notes++;}}",
            "cat/ttcuz/framework/NavigationIsland.java": "package cat.ttcuz.framework;class NavigationIsland{static boolean hit(android.app.Activity a,float x,float y){return false;}static boolean move(android.app.Activity a,float x){return false;}static void begin(android.app.Activity a,float x){}static float[] end(android.app.Activity a,boolean b){return null;}}",
            "Test.java": "public class Test{public static void main(String[] args)throws Exception{cat.ttcuz.framework.TransitionChecks.run();}}",
            "cat/ttcuz/framework/TransitionChecks.java": """package cat.ttcuz.framework;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;
public class TransitionChecks{
public static class Route{public boolean result;public Bundle options;public boolean hasRequestCode(){return result;}public Bundle getAnimationBundle(){return options;}public void setAnimationBundle(Bundle b){options=b;}}
static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
static Intent player(){return new Intent(new ComponentName("host","com.ss.android.ugc.aweme.detail.ui.DetailActivity"));}
static MotionEvent event(int action,float x,float y,long duration){return MotionEvent.obtain(SystemClock.now,SystemClock.now+duration,action,x,y,0);}
static void send(Activity a,int action,float x,float y,long duration){a.window.callback.dispatchTouchEvent(event(action,x,y,duration));}
static void tap(Activity a,float x,float y){send(a,0,x,y,0);send(a,1,x,y,60);}
public static void run(){
Activity a=new Activity();ViewGroup root=new ViewGroup(a);root.w=300;root.h=900;root.clipBottom=900;root.y=24;a.window.decor=root;
final int[] dispatched={0};final Route[] launch={null};a.window.callback=e->{dispatched[0]++;if(e.action==1&&launch[0]!=null)VideoOpenTransition.prepare(a,player(),launch[0]);return false;};
ViewGroup grid=new ViewGroup(new ContextWrapper(a));grid.w=300;grid.y=200;root.addView(grid);
View[] cells={new View(a),new View(a),new View(a)};for(int i=0;i<3;i++){cells[i].x=i*100;grid.addView(cells[i]);}
View header=new View(a);header.w=300;header.h=50;header.y=160;grid.addView(header);
ProfileGrid.tick(root);check(ProfileGrid.cellAt(root,150,244)==cells[1],"cell bounds with status offset");check(ProfileGrid.cellAt(root,150,400)==null,"header excluded");
Route synchronous=new Route();launch[0]=synchronous;tap(a,150,244);launch[0]=null;
check(dispatched[0]==2,"native DOWN/UP preserved");check(synchronous.options!=null&&synchronous.options.source==cells[1],"armed before native click dispatch");
Route once=new Route();VideoOpenTransition.prepare(a,player(),once);check(once.options==null,"one launch per tap");
cells[1].clipTop=30;cells[1].clipBottom=120;tap(a,150,264);Route clipped=new Route();VideoOpenTransition.prepare(new ContextWrapper(a),player(),clipped);
check(clipped.options!=null&&clipped.options.x==0&&clipped.options.y==30&&clipped.options.w==100&&clipped.options.h==90,"visible clipped origin and dimensions");
tap(a,150,234);Route hidden=new Route();VideoOpenTransition.prepare(a,player(),hidden);check(hidden.options==null,"clipped area not clickable");
cells[1].clipTop=0;cells[1].clipBottom=150;
send(a,0,150,244,0);send(a,2,170,244,40);send(a,1,150,244,60);Route drag=new Route();VideoOpenTransition.prepare(a,player(),drag);check(drag.options==null,"scroll cancels even after returning to origin");
send(a,0,150,244,0);send(a,5,150,244,10);send(a,1,150,244,60);Route multi=new Route();VideoOpenTransition.prepare(a,player(),multi);check(multi.options==null,"second finger cancels");
send(a,0,150,244,0);send(a,1,150,244,550);Route held=new Route();VideoOpenTransition.prepare(a,player(),held);check(held.options==null,"long press not player opening");
send(a,0,150,244,0);send(a,3,150,244,10);send(a,1,150,244,60);Route cancel=new Route();VideoOpenTransition.prepare(a,player(),cancel);check(cancel.options==null,"cancel event");
tap(a,150,244);Route unrelated=new Route();VideoOpenTransition.prepare(a,new Intent(new ComponentName("host","Settings")),unrelated);VideoOpenTransition.prepare(a,player(),unrelated);check(unrelated.options==null,"unrelated launch consumes stale origin");
tap(a,150,244);Route external=new Route();VideoOpenTransition.prepare(a,new Intent(new ComponentName("other",player().component.getClassName())),external);check(external.options==null,"external package");
tap(a,150,244);Route result=new Route();result.result=true;VideoOpenTransition.prepare(a,player(),result);check(result.options==null,"result callback preserved");
tap(a,150,244);Route custom=new Route();Bundle old=new Bundle();custom.options=old;VideoOpenTransition.prepare(a,player(),custom);check(custom.options==old,"existing animation preserved");
Motion.enabled=false;tap(a,150,244);Route reduced=new Route();VideoOpenTransition.prepare(a,player(),reduced);check(reduced.options==null,"reduced motion");Motion.enabled=true;
tap(a,150,244);SystemClock.now+=701;Route stale=new Route();VideoOpenTransition.prepare(a,player(),stale);check(stale.options==null,"expired tap");
tap(a,150,244);VideoOpenTransition.clear(a);Route paused=new Route();VideoOpenTransition.prepare(a,player(),paused);check(paused.options==null,"pause clears origin");
tap(a,150,244);grid.alpha=0;Route transparent=new Route();VideoOpenTransition.prepare(a,player(),transparent);check(transparent.options==null,"transparent profile ancestor");tap(a,150,244);grid.alpha=1;VideoOpenTransition.prepare(a,player(),transparent);check(transparent.options==null,"hidden tab cannot arm origin");
tap(a,150,244);cells[1].attached=false;Route detached=new Route();VideoOpenTransition.prepare(a,player(),detached);check(detached.options==null,"recycled view");cells[1].attached=true;
Activity b=new Activity();b.window.decor=new ViewGroup(b);tap(a,150,244);Route wrongWindow=new Route();VideoOpenTransition.prepare(b,player(),wrongWindow);check(wrongWindow.options==null,"window isolation");VideoOpenTransition.clear(a);
tap(a,150,244);ActivityOptions.fail=true;Route fail=new Route();VideoOpenTransition.prepare(a,player(),fail);check(fail.options==null&&Diary.notes==1,"unsupported options fall back without crash");ActivityOptions.fail=false;
tap(a,50,244);Route photo=new Route();VideoOpenTransition.prepare(a,new Intent(new ComponentName("host","com.ss.android.ugc.aweme.ui.activity.PostModeDetailActivity")),photo);check(photo.options!=null&&photo.options.source==cells[0],"photo-mode card");
for(int tab=0;tab<5;tab++){SystemClock.now+=301;grid.shown=false;ViewGroup next=new ViewGroup(a);next.w=300;next.y=200;root.addView(next);View first=new View(a),second=new View(a);second.x=100;next.addView(first);next.addView(second);ProfileGrid.tick(root);tap(a,50,244);Route r=new Route();VideoOpenTransition.prepare(a,player(),r);check(r.options!=null&&r.options.source==first,"visible tab "+tab);grid=next;}
grid.shown=false;SystemClock.now+=301;ViewGroup single=new androidx.recyclerview.widget.RecyclerView(a);single.w=300;single.y=200;root.addView(single);View only=new View(a);single.addView(only);ProfileGrid.tick(root);tap(a,50,244);Route onePost=new Route();VideoOpenTransition.prepare(a,player(),onePost);check(onePost.options!=null&&onePost.options.source==only,"one private post in native RecyclerView");
check(Diary.notes==1,"no unexpected errors");
}}
""",
        }
        for name in ("VideoOpenTransition", "ProfileGrid", "IslandTouch", "NativeRead"):
            files[f"cat/ttcuz/framework/{name}.java"] = (
                ROOT / f"inject/java/cat/ttcuz/framework/{name}.java"
            ).read_text(encoding="utf-8")
        try:
            self.run_java(files)
        except subprocess.CalledProcessError as error:
            self.fail(error.stderr.decode("utf-8", errors="replace"))

    def test_route_hook_is_scoped_idempotent_and_keeps_native_launch(self):
        owner = dexpatch.VIDEO_ROUTE
        method = """.method public openComponent(Landroid/content/Context;Landroid/content/Intent;)V
    .registers 5
    invoke-virtual {p0}, Lcom/bytedance/router/route/BaseRoute;->getRouteIntent()Lcom/bytedance/router/RouteIntent;
    move-result-object v1
    invoke-virtual {v1}, Lcom/bytedance/router/RouteIntent;->getAnimationBundle()Landroid/os/Bundle;
    move-result-object v0
    invoke-direct {p0, p1, v1, p2}, Lcom/bytedance/router/route/ActivityRoute;->normalStart(Landroid/content/Context;Lcom/bytedance/router/RouteIntent;Landroid/content/Intent;)V
    return-void
.end method
"""
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "ActivityRoute.smali"
            path.write_text(".class public " + owner + "\n" + method, encoding="utf-8")
            other = Path(directory) / "Other.smali"
            original = ".class public Lother/ActivityRoute;\n" + method
            other.write_text(original, encoding="utf-8")
            counts = dexpatch.rewrite_anchored(directory)
            self.assertEqual(counts.get("native profile video open"), 1)
            changed = path.read_text(encoding="utf-8")
            self.assertIn("invoke-static {p1, p2, v1}, " + dexpatch.VIDEO_OPEN_TARGET, changed)
            self.assertIn(".registers 5", changed)
            self.assertEqual(other.read_text(encoding="utf-8"), original)
            self.assertEqual(
                changed.replace(
                    "    invoke-static {p1, p2, v1}, " + dexpatch.VIDEO_OPEN_TARGET + "\n\n", ""
                ),
                ".class public " + owner + "\n" + method,
            )
            self.assertFalse(dexpatch.rewrite_anchored(directory).get("native profile video open"))
            self.assertEqual(path.read_text(encoding="utf-8"), changed)
        self.assertTrue(dexpatch.carries_an_anchor(owner.encode()))
        self.assertIn(dexpatch.VIDEO_OPEN_TARGET, dexpatch.rewrite_targets())

    def test_previous_dex_cache_cannot_omit_new_route_hook(self):
        # Exercise cache selection with a fake patch function, without assembling DEX/APK.
        with tempfile.TemporaryDirectory() as directory:
            old_rules = Path(directory) / "old-rules.py"
            old_rules.write_bytes(b"previous rules")
            builder = Build.__new__(Build)
            builder.detail = lambda message: None
            for dex, should_patch in (
                (bytes(112) + dexpatch.VIDEO_ROUTE.encode(), True),
                (bytes(112) + b"unchanged input", False),
            ):
                digest = hashlib.sha256(dex)
                digest.update(old_rules.read_bytes())
                digest.update(repr((21, [], sorted(dexpatch.FOUND.items()))).encode("utf-8"))
                key = digest.hexdigest()
                (Path(directory) / (key + ".json")).write_text(
                    json.dumps({"previous hook": 1}), encoding="utf-8"
                )
                (Path(directory) / (key + ".dex")).write_bytes(b"previous patched dex")
                with (
                    patch.dict(
                        os.environ,
                        {"ttcuz_CACHE": directory, "ttcuz_CACHE_COMPAT_RULES": str(old_rules)},
                    ),
                    patch.object(
                        dexpatch,
                        "patch",
                        return_value=(b"new patched dex", {"native profile video open": 1}),
                    ) as rewrite,
                ):
                    result = builder._patch_cached(dex, "classes34.dex", None, directory, {}, 21)
                    self.assertEqual(rewrite.called, should_patch)
                    self.assertEqual(
                        result[0], b"new patched dex" if should_patch else b"previous patched dex"
                    )


if __name__ == "__main__":
    unittest.main()
