"""Overlapping activities must never switch launcher entries while still visible."""
import unittest
import test_settings_safety_runtime as safety

class LauncherLifecycleTest(unittest.TestCase):
    run_java=safety.SafetyRuntimeTest.run_java
    def test_switch_waits_for_all_activities_and_cancels_on_return(self):
        from pathlib import Path
        source=(Path(__file__).resolve().parents[1]/'inject/java/cat/narezany/margyt/Launcher.java').read_text()
        section=source[source.index('    private static final Handler main ='):source.index('    /** A choice made earlier')]
        sources={
            'android/os/Bundle.java':'package android.os;public class Bundle{}',
            'android/os/Looper.java':'package android.os;public class Looper{public static Looper getMainLooper(){return new Looper();}}',
            'android/os/Handler.java':'package android.os;public class Handler{public java.util.List<Runnable> tasks=new java.util.ArrayList<>();public Handler(Looper l){}public void removeCallbacks(Runnable r){tasks.removeIf(t->t==r);}public void postDelayed(Runnable r,long delay){tasks.add(r);}public void drain(){java.util.List<Runnable> copy=new java.util.ArrayList<>(tasks);tasks.clear();for(Runnable r:copy)r.run();}}',
            'android/content/Context.java':'package android.content;public class Context{public Context getApplicationContext(){return this;}}',
            'android/app/Activity.java':'package android.app;public class Activity{}',
            'android/app/Application.java':'''package android.app;import android.os.Bundle;
                public class Application extends android.content.Context{
                public ActivityLifecycleCallbacks callbacks;public int registrations;
                public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks c){callbacks=c;registrations++;}
                public interface ActivityLifecycleCallbacks{void onActivityCreated(Activity a,Bundle b);void onActivityStarted(Activity a);void onActivityResumed(Activity a);void onActivityPaused(Activity a);void onActivityStopped(Activity a);void onActivitySaveInstanceState(Activity a,Bundle b);void onActivityDestroyed(Activity a);}}''',
            'Test.java':'''import android.app.*;import android.content.*;import android.os.*;
                class Diary{static void note(String s){throw new AssertionError(s);}}
                class Margy{static Application app;static Context context(){return app;}}
                public class Test{static int changes;static void applyNow(Context c){changes++;}
                '''+section+'''
                static void ck(boolean b,String s){if(!b)throw new AssertionError(s);}
                public static void main(String[] args){Application app=new Application();Margy.app=app;
                watch(app);watch(app);ck(app.registrations==1,"one startup watcher");
                Activity feed=new Activity(),settings=new Activity();
                app.callbacks.onActivityStarted(feed);app.callbacks.onActivityStarted(settings);
                applyWhenHidden(app);ck(started==2,"both screens counted from startup");
                app.callbacks.onActivityStopped(settings);main.drain();ck(changes==0,"closing settings while feed visible does not switch");
                app.callbacks.onActivityStopped(feed);ck(main.tasks.size()==1,"background schedules once");
                app.callbacks.onActivityStarted(feed);main.drain();ck(changes==0,"quick return cancels switch");
                app.callbacks.onActivityStopped(feed);main.drain();ck(changes==1,"all screens stopped permits switch");
                applyWhenHidden(app);main.drain();ck(changes==2,"background accent change can apply");}}
                '''}
        self.run_java(sources)
