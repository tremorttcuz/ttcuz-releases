"""Execute the production verifier against genuine and modified APK inventories."""

from pathlib import Path
import unittest
import test_settings_safety_runtime as safety

ROOT = Path(__file__).resolve().parents[1]


class IntegrityRuntimeTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_signed_inventory_rejects_changes_and_false_identity(self):
        java = (ROOT / "inject/java/cat/ttcuz/framework/IntegrityVerifier.java").read_text(
            encoding="utf8"
        )
        harness = r"""package cat.ttcuz.framework;
import java.io.*;import java.nio.file.*;import java.security.*;import java.util.*;import java.util.zip.*;
public class Harness {
 static KeyPair owner,other;static String pkg="com.zhiliaoapp.musically",cert="1234";static byte[] payload,sig;
 static Map<String,byte[]> files=new LinkedHashMap<>();static Path directory;
 static byte[] sign(byte[] value,KeyPair key)throws Exception{Signature s=Signature.getInstance("SHA256withRSA");s.initSign(key.getPrivate());s.update(value);return s.sign();}
 static File apk(Map<String,byte[]> entries,byte[] manifest,byte[] signature)throws Exception{
  File path=directory.resolve("app.apk").toFile();try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(path))){
   for(Map.Entry<String,byte[]> entry:entries.entrySet()){out.putNextEntry(new ZipEntry(entry.getKey()));out.write(entry.getValue());out.closeEntry();}
   out.putNextEntry(new ZipEntry(IntegrityVerifier.MANIFEST));out.write(manifest);out.closeEntry();
   out.putNextEntry(new ZipEntry(IntegrityVerifier.SIGNATURE));out.write(signature);out.closeEntry();
  }return path;
 }
 static void rejected(Map<String,byte[]> entries,byte[] manifest,byte[] signature,String packageName,String certificate)throws Exception{
  try{IntegrityVerifier.verify(apk(entries,manifest,signature),packageName,certificate,owner.getPublic().getEncoded());throw new AssertionError("Altered APK accepted");}catch(SecurityException|IOException expected){}
 }
 public static void run()throws Exception{
  directory=Files.createTempDirectory("seal-test");KeyPairGenerator gen=KeyPairGenerator.getInstance("RSA");gen.initialize(2048);owner=gen.generateKeyPair();other=gen.generateKeyPair();
  files.put("classes.dex",new byte[]{1,2,3});files.put("AndroidManifest.xml",new byte[]{4});files.put("resources.arsc",new byte[]{5});files.put("lib/arm64-v8a/native.so",new byte[]{6});files.put("assets/emoji.ttf",new byte[]{7});
  StringBuilder list=new StringBuilder("TTCUZ-INTEGRITY-1\npackage="+pkg+"\ncertificate="+cert+"\nlauncher=com.ss.android.ugc.aweme.main.MainActivity\n");
  for(Map.Entry<String,byte[]> entry:files.entrySet())list.append(IntegrityVerifier.hex(MessageDigest.getInstance("SHA-256").digest(entry.getValue()))).append('\t').append(entry.getKey()).append('\n');
  payload=list.toString().getBytes("UTF-8");sig=sign(payload,owner);
  if(!IntegrityVerifier.verify(apk(files,payload,sig),pkg,cert,owner.getPublic().getEncoded()).equals("com.ss.android.ugc.aweme.main.MainActivity"))throw new AssertionError("Genuine APK refused");
  Map<String,byte[]> modified=new LinkedHashMap<>(files);modified.put("classes.dex",new byte[]{0});rejected(modified,payload,sig,pkg,cert);
  modified=new LinkedHashMap<>(files);modified.put("resources.arsc",new byte[]{0});rejected(modified,payload,sig,pkg,cert);
  modified=new LinkedHashMap<>(files);modified.put("assets/emoji.ttf",new byte[]{0});rejected(modified,payload,sig,pkg,cert);
  modified=new LinkedHashMap<>(files);modified.remove("lib/arm64-v8a/native.so");rejected(modified,payload,sig,pkg,cert);
  modified=new LinkedHashMap<>(files);modified.put("classes99.dex",new byte[]{0});rejected(modified,payload,sig,pkg,cert);
  rejected(files,payload,sig,"com.changed",cert);rejected(files,payload,sig,pkg,"9999");
  rejected(files,payload,sign(payload,other),pkg,cert);
  byte[] edited=payload.clone();edited[edited.length-3]^=1;rejected(files,edited,sig,pkg,cert);
  modified=new LinkedHashMap<>(files);modified.put("META-INF/NEW.SF",new byte[]{0});modified.put("META-INF/NEW.RSA",new byte[]{0});
  IntegrityVerifier.verify(apk(modified,payload,sig),pkg,cert,owner.getPublic().getEncoded());
  for(File file:directory.toFile().listFiles())file.delete();directory.toFile().delete();
 }
}"""
        self.run_java(
            {
                "cat/ttcuz/framework/IntegrityVerifier.java": java,
                "cat/ttcuz/framework/Harness.java": harness,
                "Test.java": "public class Test { public static void main(String[] a)throws Exception {cat.ttcuz.framework.Harness.run();}}",
            }
        )

    def test_gate_preserves_native_task_and_original_intent(self):
        self.gate("valid")

    def test_gate_blocks_altered_and_unreadable_apks(self):
        self.gate("altered")
        self.gate("unreadable")

    def test_valid_check_survives_mod_startup_error(self):
        self.gate("startup_error")

    def test_trust_survives_restart_but_not_apk_update(self):
        self.gate("cached")

    def test_cache_failure_does_not_reject_genuine_apk(self):
        self.gate("cache_error")

    def test_verifier_linkage_error_is_recoverable(self):
        self.gate("linkage_error")

    def test_success_closes_only_gate_without_relaunching_native_activity(self):
        result = safety.method("IntegrityActivity.java", "    void result")
        back = safety.method("IntegrityActivity.java", "    @Override public void onBackPressed")
        self.run_java(
            {
                "Test.java": """class Activity{int finishes,backs;boolean isFinishing(){return finishes>0;}void finish(){finishes++;}public void onBackPressed(){backs++;}}
         class View{static int VISIBLE=0,GONE=8;void setVisibility(int n){}void setText(String s){}}
         public class Test extends Activity{boolean continued;View spinner=new View(),retry=new View(),text=new View();
         """
                + result
                + back
                + """
         public static void main(String[] args){Test gate=new Test();for(int state:new int[]{0,2,3}){gate.result(state);gate.onBackPressed();if(gate.finishes!=0||gate.backs!=0)throw new AssertionError("Unchecked task exposed by Back");}
         gate.result(1);gate.result(1);if(gate.finishes!=1)throw new AssertionError("Gate did not finish exactly once");}}
         """
            }
        )

    def gate(self, outcome):
        stubs = {
            "android/os/Looper.java": "package android.os;public class Looper {public static Looper getMainLooper(){return new Looper();}}",
            "android/os/Handler.java": "package android.os;public class Handler {public static final java.util.List<Runnable> queue=new java.util.concurrent.CopyOnWriteArrayList<>();public Handler(Looper l){}public void post(Runnable r){queue.add(r);}}",
            "android/os/Bundle.java": "package android.os;public class Bundle {}",
            "android/os/Build.java": "package android.os;public class Build {public static class VERSION{public static int SDK_INT=35;}}",
            "android/util/Base64.java": "package android.util;public class Base64{public static int DEFAULT=0;public static byte[] decode(String s,int flags){return new byte[]{1};}}",
            "android/util/Log.java": "package android.util;public class Log{public static void w(String a,String b){}public static void w(String a,String b,Throwable t){}public static void e(String a,String b,Throwable t){}}",
            "android/content/ComponentName.java": "package android.content;public class ComponentName{public ComponentName(Context c,Class<?> t){}}",
            "android/content/Intent.java": 'package android.content;public class Intent {public String route="original";public Intent(){}public Intent(Context c,Class<?> t){}public Intent(Intent i){route=i.route;}public Intent setComponent(ComponentName c){return this;}}',
            "android/content/Context.java": 'package android.content;public class Context {public static int MODE_PRIVATE=0;public static SharedPreferences prefs=new SharedPreferences();public SharedPreferences getSharedPreferences(String n,int m){return prefs;}public Context getApplicationContext(){return this;}public String getPackageName(){return "genuine";}public android.content.pm.PackageManager getPackageManager(){return new android.content.pm.PackageManager();}public android.content.pm.ApplicationInfo getApplicationInfo(){return new android.content.pm.ApplicationInfo();}}',
            "android/content/SharedPreferences.java": "package android.content;public class SharedPreferences {public String value;public int writes;public boolean fail;public String getString(String k,String d){if(fail)throw new IllegalStateException();return value==null?d:value;}public Editor edit(){return new Editor();}public class Editor{public Editor putString(String k,String v){if(fail)throw new IllegalStateException();value=v;return this;}public boolean commit(){writes++;return true;}}}",
            "android/content/pm/ApplicationInfo.java": 'package android.content.pm;public class ApplicationInfo {public String sourceDir="installed.apk";public String[] splitSourceDirs;}',
            "android/content/pm/Signature.java": "package android.content.pm;public class Signature {public byte[] toByteArray(){return new byte[]{7};}}",
            "android/content/pm/SigningInfo.java": "package android.content.pm;public class SigningInfo{public Signature[] getApkContentsSigners(){return new Signature[]{new Signature()};}}",
            "android/content/pm/PackageInfo.java": "package android.content.pm;public class PackageInfo{public static long updated=1;public int versionCode=1;public long lastUpdateTime=updated;public SigningInfo signingInfo=new SigningInfo();public Signature[] signatures=new Signature[]{new Signature()};}",
            "android/content/pm/PackageManager.java": "package android.content.pm;public class PackageManager{public static int GET_SIGNATURES=64,GET_SIGNING_CERTIFICATES=128;public PackageInfo getPackageInfo(String p,int f){return new PackageInfo();}}",
            "android/app/Activity.java": "package android.app;public class Activity extends android.content.Context{public boolean finishing;public int opened;public android.content.Intent intent=new android.content.Intent();public android.content.Intent getIntent(){return intent;}public boolean isFinishing(){return finishing;}public void finish(){finishing=true;}public void startActivity(android.content.Intent i){opened++;}}",
            "android/app/Application.java": "package android.app;public class Application extends android.content.Context{public ActivityLifecycleCallbacks callbacks;public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks c){callbacks=c;}public interface ActivityLifecycleCallbacks{void onActivityCreated(Activity a,android.os.Bundle b);void onActivityResumed(Activity a);void onActivityStarted(Activity a);void onActivityPaused(Activity a);void onActivityStopped(Activity a);void onActivitySaveInstanceState(Activity a,android.os.Bundle b);void onActivityDestroyed(Activity a);}}",
            "cat/ttcuz/framework/IntegrityKey.java": 'package cat.ttcuz.framework;class IntegrityKey{static String PUBLIC_KEY="public";}',
            "cat/ttcuz/framework/IntegrityActivity.java": "package cat.ttcuz.framework;class IntegrityActivity extends android.app.Activity{int result=-1;void result(int s){result=s;}}",
            "cat/ttcuz/framework/IntegrityVerifier.java": 'package cat.ttcuz.framework;class IntegrityVerifier{static int attempts;static volatile boolean checking=true;static String hex(byte[] b){return "hash";}static String verify(java.io.File f,String p,String c,byte[] k)throws Exception{while(checking)Thread.sleep(1);attempts++;'
            + (
                "throw new SecurityException();"
                if outcome == "altered"
                else (
                    'if(attempts==1)throw new java.io.IOException();return "native";'
                    if outcome == "unreadable"
                    else (
                        "throw new NoClassDefFoundError();"
                        if outcome == "linkage_error"
                        else 'return "native";'
                    )
                )
            )
            + "}}",
            "cat/ttcuz/framework/AppIntegrity.java": (
                ROOT / "inject/java/cat/ttcuz/framework/AppIntegrity.java"
            ).read_text(encoding="utf8"),
        }
        expected = (
            1
            if outcome in {"valid", "startup_error", "cached", "cache_error"}
            else 2 if outcome == "altered" else 3
        )
        stubs["cat/ttcuz/framework/Harness.java"] = (
            """package cat.ttcuz.framework;
public class Harness{
 static void drain()throws Exception{
  for(int i=0;i<1000&&android.os.Handler.queue.isEmpty();i++)Thread.sleep(2);
  if(android.os.Handler.queue.isEmpty())throw new AssertionError("Check did not complete");
  java.util.List<Runnable> tasks=new java.util.ArrayList<>(android.os.Handler.queue);
  android.os.Handler.queue.clear();for(Runnable r:tasks)r.run();
 }
 static void restart()throws Exception{
  for(String name:new String[]{"state","started","opening","screen"}){
   java.lang.reflect.Field f=AppIntegrity.class.getDeclaredField(name);f.setAccessible(true);
   if(name.equals("state"))f.setInt(null,0);else if(name.equals("screen"))f.set(null,new java.lang.ref.WeakReference<>(null));else f.setBoolean(null,false);
  }
 }
 public static void run()throws Exception{
  android.app.Application app=new android.app.Application();final int[] trusted={0};
  AppIntegrity.start(app,()->trusted[0]++);
  android.app.Activity first=new android.app.Activity();first.intent.route="deep-link";
  app.callbacks.onActivityCreated(first,null);
  if(first.finishing||first.opened!=0)throw new AssertionError("Native creation was interrupted");
  app.callbacks.onActivityResumed(first);
  if(first.finishing||first.opened!=1||trusted[0]!=0)throw new AssertionError("Gate destroyed the native task");
  app.callbacks.onActivityResumed(first);if(first.opened!=1)throw new AssertionError("Duplicate gate");
  IntegrityActivity screen=new IntegrityActivity();AppIntegrity.shown(screen);
  if(screen.result!=0)throw new AssertionError("Pending check was skipped");
  IntegrityVerifier.checking=false;drain();
  if(screen.result!="""
            + str(expected)
            + """)throw new AssertionError("Wrong blocking result");
  if(first.finishing||!"deep-link".equals(first.intent.route))throw new AssertionError("Original screen or intent lost");
  AppIntegrity.gone(screen);android.app.Activity next=new android.app.Activity();app.callbacks.onActivityResumed(next);
  """
            + (
                'if(trusted[0]!=1||next.finishing||next.opened!=0)throw new AssertionError("Genuine app remained gated");'
                if expected == 1
                else 'if(trusted[0]!=0||next.finishing||next.opened!=1||android.content.Context.prefs.value!=null)throw new AssertionError("Failure escaped or was cached");'
            )
            + "}}"
        )
        stubs["Test.java"] = (
            "public class Test{public static void main(String[] a)throws Exception{cat.ttcuz.framework.Harness.run();}}"
        )
        if outcome == "startup_error":
            stubs["cat/ttcuz/framework/Harness.java"] = stubs[
                "cat/ttcuz/framework/Harness.java"
            ].replace(
                "()->trusted[0]++", '()->{trusted[0]++;throw new RuntimeException("startup");}'
            )
        if outcome == "unreadable":
            code = """AppIntegrity.shown(screen);AppIntegrity.retry();if(screen.result!=0)throw new AssertionError("Retry did not enter pending state");drain();if(screen.result!=1||trusted[0]!=1||first.finishing||android.content.Context.prefs.value==null)throw new AssertionError("Recovery did not restore genuine app");"""
            stubs["cat/ttcuz/framework/Harness.java"] = (
                stubs["cat/ttcuz/framework/Harness.java"][:-2] + code + "}}"
            )
        if outcome == "cache_error":
            stubs["cat/ttcuz/framework/Harness.java"] = stubs[
                "cat/ttcuz/framework/Harness.java"
            ].replace(
                "AppIntegrity.start(app,",
                "android.content.Context.prefs.fail=true;AppIntegrity.start(app,",
            )
        if outcome == "cached":
            code = """if(IntegrityVerifier.attempts!=1||android.content.Context.prefs.writes!=1)throw new AssertionError("Trust not persisted");
             restart();AppIntegrity.start(app,()->trusted[0]++);drain();
             if(IntegrityVerifier.attempts!=1||trusted[0]!=2)throw new AssertionError("Successful check repeated after restart");
             android.content.pm.PackageInfo.updated=2;restart();AppIntegrity.start(app,()->trusted[0]++);drain();
             if(IntegrityVerifier.attempts!=2||trusted[0]!=3)throw new AssertionError("Updated APK inherited stale trust");
             restart();AppIntegrity.start(app,()->trusted[0]++);drain();
             if(IntegrityVerifier.attempts!=2||trusted[0]!=4)throw new AssertionError("Updated APK checked again");"""
            stubs["cat/ttcuz/framework/Harness.java"] = (
                stubs["cat/ttcuz/framework/Harness.java"][:-2] + code + "}}"
            )
        self.run_java(stubs)
