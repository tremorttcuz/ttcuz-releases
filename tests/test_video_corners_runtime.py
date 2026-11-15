"""Rounded videos: the clip lands on the video's own rectangle, never on the page or the list, and comes off again."""
import pathlib
import shutil
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]

STUBS = {
    'android/content/Context.java': 'package android.content;public class Context{public static final int MODE_PRIVATE=0;public android.content.res.Resources getResources(){return new android.content.res.Resources();}public SharedPreferences getSharedPreferences(String n,int m){return null;}}',
    'android/content/SharedPreferences.java': 'package android.content;public interface SharedPreferences{boolean getBoolean(String k,boolean d);String getString(String k,String d);Editor edit();interface Editor{Editor putBoolean(String k,boolean v);Editor putString(String k,String v);void apply();}}',
    'android/content/res/Resources.java': 'package android.content.res;public class Resources{public static class Metrics{public float density=2;}public Metrics getDisplayMetrics(){return new Metrics();}}',
    'android/graphics/Outline.java': 'package android.graphics;public class Outline{public float radius;public int r,b;public void setRoundRect(int l,int t,int r,int b,float radius){this.r=r;this.b=b;this.radius=radius;}}',
    'android/os/SystemClock.java': 'package android.os;public class SystemClock{public static long now=1000;public static long uptimeMillis(){return now;}}',
    'android/view/ViewOutlineProvider.java': 'package android.view;public abstract class ViewOutlineProvider{public static final ViewOutlineProvider BACKGROUND=new ViewOutlineProvider(){public void getOutline(View v,android.graphics.Outline o){}};public abstract void getOutline(View view,android.graphics.Outline outline);}',
    'android/view/ViewTreeObserver.java': '''package android.view;public class ViewTreeObserver{public interface OnPreDrawListener{boolean onPreDraw();}public interface OnGlobalLayoutListener{void onGlobalLayout();}
        public OnPreDrawListener draw;public OnGlobalLayoutListener layout;public boolean isAlive(){return true;}
        public void addOnPreDrawListener(OnPreDrawListener l){draw=l;}public void removeOnPreDrawListener(OnPreDrawListener l){if(draw==l)draw=null;}
        public void addOnGlobalLayoutListener(OnGlobalLayoutListener l){layout=l;}public void removeOnGlobalLayoutListener(OnGlobalLayoutListener l){if(layout==l)layout=null;}}''',
    'android/view/View.java': '''package android.view;public class View{public static final int VISIBLE=0;public int width=100,height=100,visibility=0;public Object parent;
        public ViewTreeObserver observer=new ViewTreeObserver();public ViewOutlineProvider provider=ViewOutlineProvider.BACKGROUND;public boolean clip;
        public int getWidth(){return width;}public int getHeight(){return height;}public int getVisibility(){return visibility;}public Object getParent(){return parent;}
        public ViewTreeObserver getViewTreeObserver(){return observer;}public boolean hasWindowFocus(){return true;}public void invalidate(){}
        public android.content.res.Resources getResources(){return new android.content.res.Resources();}
        public ViewOutlineProvider getOutlineProvider(){return provider;}public void setOutlineProvider(ViewOutlineProvider p){provider=p;}
        public boolean getClipToOutline(){return clip;}public void setClipToOutline(boolean c){clip=c;}}''',
    'android/view/ViewGroup.java': '''package android.view;public class ViewGroup extends View{private java.util.List<View> children=new java.util.ArrayList<>();
        public void addView(View v){children.add(v);v.parent=this;}public int getChildCount(){return children.size();}public View getChildAt(int i){return children.get(i);}}''',
    'android/view/TextureView.java': 'package android.view;public class TextureView extends View{}',
    'android/view/SurfaceView.java': 'package android.view;public class SurfaceView extends View{}',
    'android/view/Window.java': 'package android.view;public class Window{public View decor;public View getDecorView(){return decor;}}',
    'android/app/Activity.java': 'package android.app;public class Activity{public android.view.Window window=new android.view.Window();public android.view.Window getWindow(){return window;}}',
    'com/ss/android/ugc/aweme/main/MainActivity.java': 'package com.ss.android.ugc.aweme.main;public class MainActivity extends android.app.Activity{}',
    'androidx/recyclerview/widget/RecyclerView.java': 'package androidx.recyclerview.widget;public class RecyclerView extends android.view.ViewGroup{}',
    'cat/ttcuz/framework/Ttcuz.java': 'package cat.ttcuz.framework;class Ttcuz{static final String PREFS="test";static android.content.Context context(){return null;}}',
    'cat/ttcuz/framework/Diary.java': 'package cat.ttcuz.framework;class Diary{static void note(String s){throw new AssertionError(s);}}',
    'cat/ttcuz/framework/Harness.java': '''package cat.ttcuz.framework;import android.view.*;
public class Harness{static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static ViewGroup group(int w,int h){ViewGroup g=new ViewGroup();g.width=w;g.height=h;return g;}
 public static void main(String[] a){
  com.ss.android.ugc.aweme.main.MainActivity activity=new com.ss.android.ugc.aweme.main.MainActivity();
  ViewGroup decor=group(400,900);activity.window.decor=decor;
  androidx.recyclerview.widget.RecyclerView list=new androidx.recyclerview.widget.RecyclerView();list.width=400;list.height=800;decor.addView(list);
  // a portrait clip: page -> player frame -> texture, all page-sized; the list itself must stay square
  ViewGroup page=group(400,800);list.addView(page);ViewGroup frame=group(400,800);page.addView(frame);TextureView video=new TextureView();video.width=400;video.height=800;frame.addView(video);
  ViewGroup overlay=group(400,800);page.addView(overlay);
  // a landscape clip inside a page: only its 400x225 box is the card
  ViewGroup page2=group(400,800);list.addView(page2);ViewGroup box=group(400,225);page2.addView(box);SurfaceView wide=new SurfaceView();wide.width=400;wide.height=225;box.addView(wide);
  ViewOutlineProvider own=new ViewOutlineProvider(){public void getOutline(View v,android.graphics.Outline o){}};page.provider=own;
  check(VideoCorners.card(video)==page,"Portrait clip: the outermost same-sized ancestor below the list is the card");
  check(VideoCorners.card(wide)==box,"Landscape clip: the card is the video's own box, not the page");
  VideoCorners.watch(activity);check(decor.observer.draw!=null,"Watching the feed");
  decor.observer.draw.onPreDraw();
  check(page.clip&&page.provider!=own,"Portrait card not rounded");check(box.clip,"Landscape card not rounded");
  check(!list.clip&&!overlay.clip&&!page2.clip&&!decor.clip,"Something other than a video card was clipped");
  android.graphics.Outline o=new android.graphics.Outline();page.provider.getOutline(page,o);check(o.radius==56f,"MD3 extra-large corner is 28 dp, got "+o.radius);
  decor.observer.draw.onPreDraw();check(page.clip,"A steady frame changed the rounding");
  VideoCorners.setEnabled(false);check(page.provider==own&&!page.clip&&!box.clip,"Switching off did not restore the views");
  SystemClock();VideoCorners.setEnabled(true);android.os.SystemClock.now+=1000;decor.observer.draw.onPreDraw();check(page.clip,"Switching on did not round again");
  VideoCorners.stop(activity);check(decor.observer.draw==null&&decor.observer.layout==null,"Listeners survived stop");
 }
 static void SystemClock(){}
}''',
}
STUBS['android/os/Trace.java'] = 'package android.os;public final class Trace{public static void beginSection(String s){}public static void endSection(){}}'


class VideoCornersRuntimeTest(unittest.TestCase):
    @unittest.skipUnless(shutil.which('javac') and shutil.which('java'), 'JDK required')
    def test_cards_rounding_and_restore(self):
        with tempfile.TemporaryDirectory() as d:
            work = pathlib.Path(d)
            sources = dict(STUBS)
            sources['cat/ttcuz/framework/VideoCorners.java'] = (ROOT / 'inject/java/cat/ttcuz/framework/VideoCorners.java').read_text(encoding='utf-8')
            for name, text in sources.items():
                p = work / name; p.parent.mkdir(parents=True, exist_ok=True); p.write_text(text, encoding='utf-8')
            built = subprocess.run(['javac', '-source', '8', '-target', '8', '-encoding', 'UTF-8', '-d', d] + [str(work / n) for n in sources], capture_output=True, text=True)
            self.assertEqual(0, built.returncode, built.stderr)
            run = subprocess.run(['java', '-cp', d, 'cat.ttcuz.framework.Harness'], capture_output=True, text=True)
            self.assertEqual(0, run.returncode, run.stderr)


if __name__ == '__main__':
    unittest.main()
