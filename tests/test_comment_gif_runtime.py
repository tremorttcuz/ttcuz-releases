"""Delayed GIF callbacks cannot strand or overwrite a recreated comment panel."""

import unittest
import test_settings_safety_runtime as safety


class CommentGifTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_detach_reopen_and_stale_success_or_failure(self):
        methods = safety.method(
            "CommentMediaView.java", "    private void loadGif("
        ) + safety.method("CommentMediaView.java", "    protected void onDetachedFromWindow(")
        self.run_java(
            {
                "android/content/Context.java": "package android.content;public class Context{}",
                "android/os/Build.java": "package android.os;public class Build{public static class VERSION{public static int SDK_INT=28;}}",
                "android/graphics/Movie.java": "package android.graphics;public class Movie{public static Movie decodeFile(String p){return new Movie();}}",
                "android/graphics/drawable/Drawable.java": "package android.graphics.drawable;public class Drawable{}",
                "android/graphics/drawable/Animatable.java": "package android.graphics.drawable;public interface Animatable{void start();void stop();}",
                "android/view/View.java": "package android.view;public class View{}",
                "android/widget/ImageView.java": "package android.widget;public class ImageView extends android.view.View{public enum ScaleType{CENTER_CROP}public ImageView(android.content.Context c){}public void setScaleType(ScaleType s){}public void setImageDrawable(android.graphics.drawable.Drawable d){}}",
                "Test.java": """import android.os.Build;import android.graphics.*;import android.graphics.drawable.*;import android.view.*;import android.widget.*;import java.io.*;import java.util.*;
class Base{protected void onDetachedFromWindow(){}}
public class Test extends Base{
 File file=new File("gif");View picture;Drawable animation;boolean loading,active=true,attached=true;int generation,errors,scaleMode;void scale(int n){}static class LayoutParams{LayoutParams(int a,int b){}}static class Observer{void removeOnPreDrawListener(Object o){}}Object visibility=new Object();Observer getViewTreeObserver(){return new Observer();}boolean isAttachedToWindow(){return attached;}android.content.Context getContext(){return new android.content.Context();}
 List<Runnable> posted=new ArrayList<>();List<View> children=new ArrayList<>();void post(Runnable r){posted.add(r);}void addView(View v,LayoutParams p){children.add(v);}void applyBlur(){}void suspend(){active=false;}void error(Throwable e){errors++;}void flush(){List<Runnable> q=new ArrayList<>(posted);posted.clear();for(Runnable r:q)r.run();}
 static class LegacyGif extends View{boolean running;LegacyGif(android.content.Context c,Movie m){}}
 static class Gif{static boolean fail;static Drawable decode(File f)throws Exception{if(fail)throw new IOException("broken");return new Drawable();}}
 static class Net{static List<Runnable> jobs=new ArrayList<>();static void away(String name,Runnable r){jobs.add(r);}}
"""
                + methods
                + """static void check(boolean v,String m){if(!v)throw new AssertionError(m);}public static void main(String[]a){
 for(boolean failed:new boolean[]{false,true}){Net.jobs.clear();Test t=new Test();t.loadGif();check(t.loading&&Net.jobs.size()==1,"first decode missing");Runnable old=Net.jobs.get(0);t.attached=false;t.onDetachedFromWindow();check(!t.loading&&!t.active,"detach stranded loader");t.attached=true;t.active=true;t.loadGif();check(t.loading&&Net.jobs.size()==2,"reopen failed to schedule new decode");Gif.fail=failed;old.run();t.flush();check(t.loading&&t.children.isEmpty()&&t.errors==0,"stale callback cleared current load or failed new panel");Gif.fail=false;Net.jobs.get(1).run();t.flush();check(!t.loading&&t.children.size()==1&&t.picture!=null,"new GIF did not render");t.loadGif();check(Net.jobs.size()==2,"loaded GIF decoded twice");}}
}""",
            }
        )
