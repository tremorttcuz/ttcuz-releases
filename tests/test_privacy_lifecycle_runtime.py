"""Switch changes install promptly; no retry timer runs behind a paused screen."""
import unittest
import test_settings_safety_runtime as safety


class PrivacyLifecycleTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_foreground_account_change_and_independent_flags(self):
        self.run_java({
            'android/app/Activity.java': 'package android.app;public class Activity{}',
            'android/os/Looper.java': 'package android.os;public class Looper{public static Looper getMainLooper(){return new Looper();}}',
            'android/os/Handler.java': '''package android.os;public class Handler{public static java.util.List<Runnable> pending=new java.util.ArrayList<>();public Handler(Looper l){}public void post(Runnable r){r.run();}public void postDelayed(Runnable r,long delay){pending.add(r);}public void removeCallbacks(Runnable r){pending.removeIf(x->x==r);}public static void tick(){Runnable r=pending.remove(0);r.run();}}''',
            'cat/ttcuz/framework/PrivacyRuntime.java': (safety.ROOT/'inject/java/cat/ttcuz/framework/PrivacyRuntime.java').read_text(encoding='utf8'),
            'cat/ttcuz/framework/Account.java': 'package cat.ttcuz.framework;class Account{static String uid="123";static String liveId(){return uid;}}',
            'cat/ttcuz/framework/Diary.java': 'package cat.ttcuz.framework;class Diary{static void note(String s){}}',
            'cat/ttcuz/framework/Privacy.java': '''package cat.ttcuz.framework;class Privacy{static boolean typing,profiles,online,stories,read;static boolean hideTyping(){return typing;}static boolean hideProfiles(){return profiles;}static boolean hideOnline(){return online;}static boolean hideStories(){return stories;}static boolean hideRead(){return read;}static boolean any(){return typing||profiles||online||stories||read;}}''',
            'cat/ttcuz/framework/PrivacyAdapters.java': '''package cat.ttcuz.framework;class PrivacyAdapters{interface Flags{boolean on(String k);}interface Notice{void log(String s);}static int made,closed,boot,applied;static Flags flags;PrivacyAdapters(Flags f,Notice n){flags=f;made++;}void bootstrap(){boot++;}void apply(){applied++;}void close(){closed++;}}''',
            'cat/ttcuz/framework/Harness.java': '''package cat.ttcuz.framework;import android.app.Activity;import android.os.Handler;
public class Harness {static void check(boolean b){if(!b)throw new AssertionError();}public static void run(){
 Activity a=new Activity(),b=new Activity();PrivacyRuntime.start();PrivacyRuntime.resumed(a);check(PrivacyAdapters.made==0&&Handler.pending.isEmpty());
 Privacy.typing=true;PrivacyRuntime.settingsChanged();check(PrivacyAdapters.made==1&&PrivacyAdapters.boot==1&&PrivacyAdapters.applied==1&&Handler.pending.size()==1);check(PrivacyAdapters.flags.on("ghost")&&PrivacyAdapters.flags.on("ghost_typing")&&!PrivacyAdapters.flags.on("ghost_read"));
 Privacy.profiles=true;PrivacyRuntime.settingsChanged();check(PrivacyAdapters.made==1&&Handler.pending.size()==1&&PrivacyAdapters.flags.on("ghost_profiles"));
 PrivacyRuntime.paused(b);check(Handler.pending.size()==1);PrivacyRuntime.paused(a);check(Handler.pending.isEmpty());
 PrivacyRuntime.resumed(b);check(Handler.pending.size()==1);Account.uid="456";Handler.tick();check(PrivacyAdapters.closed==1&&PrivacyAdapters.made==2&&Handler.pending.size()==1);
 Privacy.typing=Privacy.profiles=false;PrivacyRuntime.settingsChanged();check(Handler.pending.isEmpty()&&!PrivacyAdapters.flags.on("ghost"));
 PrivacyRuntime.paused(b);PrivacyRuntime.settingsChanged();check(Handler.pending.isEmpty());
 }}''',
            'Test.java': 'public class Test{public static void main(String[] args){cat.ttcuz.framework.Harness.run();}}',
        })
