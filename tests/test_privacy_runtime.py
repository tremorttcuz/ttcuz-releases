import pathlib
import unittest
import test_settings_safety_runtime as safety

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "inject/java/cat/ttcuz/framework"


class PrivacyRuntimeTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_switches_persist_and_receipt_options_are_independent(self):
        sources = {
            "android/content/Context.java": "package android.content;public class Context{public static final int MODE_PRIVATE=0;public SharedPreferences prefs=new SharedPreferences();public SharedPreferences getSharedPreferences(String k,int m){return prefs;}}",
            "android/content/SharedPreferences.java": "package android.content;public class SharedPreferences{java.util.Map<String,Boolean> values=new java.util.HashMap<>();public boolean getBoolean(String k,boolean d){return values.getOrDefault(k,d);}public Editor edit(){return new Editor();}public class Editor{public Editor putBoolean(String k,boolean v){values.put(k,v);return this;}public void apply(){}}}",
            "cat/ttcuz/framework/Ttcuz.java": 'package cat.ttcuz.framework;public class Ttcuz{public static String PREFS="test";public static android.content.Context ctx=new android.content.Context();public static android.content.Context context(){return ctx;}}',
            "cat/ttcuz/framework/PrivacyRuntime.java": "package cat.ttcuz.framework;class PrivacyRuntime{static void settingsChanged(){}}",
            "cat/ttcuz/framework/Privacy.java": (JAVA / "Privacy.java").read_text(encoding="utf-8"),
            "Test.java": """import cat.ttcuz.framework.*;public class Test{
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] a){
  check(!Privacy.hideOnline()&&!Privacy.hideRead()&&!Privacy.hideStories()&&!Privacy.hideTyping()&&!Privacy.hideProfiles());
  Integer value=Integer.valueOf(7);check(Privacy.fetchAsReport(value)==value);check(Privacy.fetchAsReport(null)==null);
  Privacy.set(Privacy.ONLINE,true);check(Privacy.hideOnline());check(Privacy.fetchAsReport(value)==0&&Privacy.fetchAsReport(null)==0);
  check(!Privacy.hideRead()&&!Privacy.hideStories());Privacy.set(Privacy.READ,true);Privacy.set(Privacy.STORIES,true);
  Privacy.set(Privacy.ONLINE,false);check(Privacy.hideRead()&&Privacy.hideStories());check(Privacy.fetchAsReport(value)==value);
  // Preferences edited outside this controller must take effect immediately.
  Ttcuz.ctx.prefs.edit().putBoolean(Privacy.READ,false).apply();check(!Privacy.hideRead());
  Privacy.set(Privacy.TYPING,true);Privacy.set(Privacy.PROFILES,true);check(Privacy.hideTyping()&&Privacy.hideProfiles());check(!Privacy.hideOnline());Privacy.set(Privacy.TYPING,false);check(!Privacy.hideTyping()&&Privacy.hideProfiles());
  Privacy.set("unknown",true);check(!Ttcuz.ctx.prefs.getBoolean("unknown",false));
  Ttcuz.ctx=null;check(!Privacy.hideOnline()&&!Privacy.hideRead()&&!Privacy.hideStories());Privacy.set(Privacy.READ,true);
 }}""",
        }
        self.run_java(sources)

    def test_notification_fallback_preserves_valid_text_and_propagates_other_errors(self):
        sources = {
            "android/content/res/Resources.java": "package android.content.res;public class Resources{public static class NotFoundException extends RuntimeException{}}",
            "android/content/Context.java": 'package android.content;public class Context{public String getString(int id){if(id==1)return "actual text";if(id==2)throw new IllegalStateException();throw new android.content.res.Resources.NotFoundException();}}',
            "cat/ttcuz/framework/NotificationStrings.java": (
                JAVA / "NotificationStrings.java"
            ).read_text(encoding="utf-8"),
            "Test.java": """import cat.ttcuz.framework.NotificationStrings;import android.content.Context;import java.util.Locale;
 public class Test{public static void main(String[]a){Context c=new Context();
 if(!"actual text".equals(NotificationStrings.getString(c,1)))throw new AssertionError();
 Locale.setDefault(new Locale("ru"));if(!"Новое сообщение".equals(NotificationStrings.getString(c,0x7f1140b9)))throw new AssertionError();
 Locale.setDefault(Locale.ENGLISH);if(!"New message".equals(NotificationStrings.getString(c,0x7f118e71)))throw new AssertionError();
 try{NotificationStrings.getString(c,0x7f060024);throw new AssertionError();}catch(android.content.res.Resources.NotFoundException expected){}
 try{NotificationStrings.getString(c,2);throw new AssertionError();}catch(IllegalStateException expected){}
 try{NotificationStrings.getString(null,1);throw new AssertionError();}catch(NullPointerException expected){}
 }}""",
        }
        self.run_java(sources)
