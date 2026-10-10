"""Exercise the real dimmer's queued work and imported preferences."""
import pathlib, unittest
from test_dim_runtime import STUBS
import test_settings_safety_runtime as safety

ROOT=pathlib.Path(__file__).resolve().parents[1]

class DimLifecycleTest(unittest.TestCase):
    run_java=safety.SafetyRuntimeTest.run_java

    def test_background_resume_disable_and_reload(self):
        files=dict(STUBS)
        for name in ('Dim','BadgeTarget','RecentProfiles','BottomChrome'):
            files['cat/narezany/margyt/'+name+'.java']=(ROOT/'inject/java/cat/narezany/margyt'/f'{name}.java').read_text(encoding='utf-8')
        files['android/view/View.java']=files['android/view/View.java'].replace('public boolean isAttachedToWindow(){return true;}', 'public boolean attached=true;public boolean isAttachedToWindow(){return attached;}').replace('delayed.remove(r);','delayed.removeIf(x->x==r);')
        files['android/view/ViewTreeObserver.java']=files['android/view/ViewTreeObserver.java'].replace('public OnGlobalLayoutListener layout;', 'public int layoutAdds,drawAdds,focusAdds;public OnGlobalLayoutListener layout;').replace('layout=v;', 'layout=v;layoutAdds++;').replace('draw=v;', 'draw=v;drawAdds++;').replace('focus=v;', 'focus=v;focusAdds++;')
        files['android/content/Context.java']=files['android/content/Context.java'].replace('return null;', 'return preferences;').replace('public class Context {', 'public class Context {public static SharedPreferences preferences;')
        files['cat/narezany/margyt/Margy.java']=files['cat/narezany/margyt/Margy.java'].replace('return null;', 'return new android.content.Context();')
        files['cat/narezany/margyt/Harness.java']='''package cat.narezany.margyt;
import android.view.*;import android.app.*;import android.content.*;
public class Harness {
 static void check(boolean b,String reason){if(!b)throw new AssertionError(reason);}
 static class Prefs implements SharedPreferences {
  boolean enabled;int strength=35;
  public boolean getBoolean(String k,boolean d){return enabled;}
  public int getInt(String k,int d){return strength;}
  public boolean contains(String k){return true;}
  public Editor edit(){return new Editor(){public Editor putBoolean(String k,boolean v){enabled=v;return this;}public Editor putInt(String k,int v){strength=v;return this;}public void apply(){}};}
 }
 public static void run() throws Exception {
  Prefs prefs=new Prefs();Context.preferences=prefs;
  Activity a=new com.ss.android.ugc.aweme.main.MainActivity();ViewGroup root=new ViewGroup();root.width=400;root.height=900;a.window.decor=root;
  Dim.watch(a);check(root.delayed.isEmpty(),"disabled feature must not poll every second");
  Dim.setEnabled(true);check(root.delayed.size()==1,"enabling starts exactly one task");
  Dim.watch(a);Dim.watch(a);check(root.delayed.size()==1,"resume must coalesce the periodic task");
  check(root.observer.layoutAdds==1&&root.observer.drawAdds==1&&root.observer.focusAdds==1,"resume must not duplicate listeners");
  Runnable queued=root.delayed.remove(0);queued.run();check(root.delayed.size()==1,"foreground task reschedules once");
  Dim.pause(a);check(root.delayed.isEmpty(),"background must cancel queued work");
  queued.run();check(root.delayed.isEmpty(),"stale background callback cannot reschedule");
  ViewGroup.childReads=0;root.observer.layout.onGlobalLayout();root.observer.draw.onPreDraw();root.observer.focus.onWindowFocusChanged(true);
  check(ViewGroup.childReads==0,"paused listeners must not traverse the screen");
  Dim.watch(a);check(root.delayed.size()==1,"return from background restarts the task");
  Dim.setEnabled(false);check(root.delayed.isEmpty(),"master off cancels work immediately");
  Dim.setEnabled(true);Dim.toggleOpacity();check(!Dim.isEnabled()&&Dim.isConfigured(),"eye pause keeps master preference");
  prefs.enabled=true;prefs.strength=72;Dim.reloadSettings();check(Dim.isEnabled()&&Dim.strength()==72,"import invalidates strength and temporary pause");
  prefs.enabled=false;prefs.strength=35;Dim.reloadSettings();check(!Dim.isConfigured()&&Dim.strength()==35&&root.delayed.isEmpty(),"reset invalidates cached master and strength");
  Dim.setEnabled(true);root.attached=false;queued=root.delayed.remove(0);queued.run();check(root.delayed.isEmpty(),"detached screen stops periodic work");
  root.attached=true;Dim.watch(a);check(root.delayed.size()==1&&root.observer.drawAdds==1,"reattaching reuses listeners and restarts one task");
  root.observer=new ViewTreeObserver();Dim.watch(a);check(root.observer.drawAdds==1&&root.observer.layoutAdds==1&&root.observer.focusAdds==1&&root.delayed.size()==1,"replaced observer is reconnected without an extra task");
  // No queued timer may strongly capture a decor view/activity.
  queued=root.delayed.get(0);for(java.lang.reflect.Field field:queued.getClass().getDeclaredFields())check(!View.class.isAssignableFrom(field.getType())&&!Activity.class.isAssignableFrom(field.getType()),"timer must use weak screen references");
  Dim.pause(a);
 }
}'''
        files['Test.java']='public class Test{public static void main(String[] args)throws Exception{cat.narezany.margyt.Harness.run();}}'
        self.run_java(files)

    def test_import_reset_and_activity_pause_reach_dimmer(self):
        backup=(ROOT/'inject/java/cat/narezany/margyt/Backup.java').read_text(encoding='utf-8')
        for start,end in [('public static boolean apply(', 'public static void reset('), ('public static void reset(', 'private static byte[] readSmall(')]:
            body=backup[backup.index(start):backup.index(end)]
            self.assertIn('Dim.reloadSettings();',body)
        row=(ROOT/'inject/java/cat/narezany/margyt/SettingsRow.java').read_text(encoding='utf-8')
        self.assertIn('Dim.pause(activity);',row[row.index('public void onActivityPaused('):row.index('public void onActivityStopped(')])
