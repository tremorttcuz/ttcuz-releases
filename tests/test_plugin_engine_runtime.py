"""Run the real engine transactions/lifecycle with a small simulated Android host.

DexClassLoader is a JVM test double; real DEX output is checked separately by
the plugin builder smoke test. No Android/device execution is implied.
"""
from pathlib import Path
import unittest
import test_settings_safety_runtime as safety

ROOT = Path(__file__).resolve().parents[1]
FRAMEWORK = ROOT / "inject/java/cat/ttcuz/framework"


class PluginEngineRuntimeTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_install_reload_account_events_failure_rollback_and_removal(self):
        sources = {
            "android/app/Activity.java": "package android.app;public class Activity{}",
            "android/view/View.java": "package android.view;public class View{}",
            "android/widget/TextView.java": "package android.widget;public class TextView extends android.view.View{}",
            "android/net/Uri.java": "package android.net;public class Uri{public final String path;private Uri(String p){path=p;}public static Uri parse(String p){return new Uri(p);}}",
            "android/os/Build.java": 'package android.os;public class Build{public static String[] SUPPORTED_ABIS={"arm64-v8a"};}',
            "android/os/Looper.java": 'package android.os;public class Looper{static final Looper MAIN=new Looper();public static Looper getMainLooper(){return MAIN;}public static Looper myLooper(){return Thread.currentThread().getName().equals("fake-main")?MAIN:null;}}',
            "android/os/Handler.java": """package android.os;import java.util.*;import java.util.concurrent.*;
                public class Handler{static final ScheduledExecutorService queue=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"fake-main");t.setDaemon(true);return t;});
                final List<Future<?>> pending=new ArrayList<>();public Handler(Looper l){}
                public boolean post(Runnable r){return postDelayed(r,0);}public boolean postDelayed(Runnable r,long d){synchronized(pending){pending.add(queue.schedule(r,d,TimeUnit.MILLISECONDS));}return true;}
                public void removeCallbacksAndMessages(Object token){synchronized(pending){for(Future<?> f:pending)f.cancel(false);pending.clear();}}} 
            """,
            "android/content/SharedPreferences.java": """package android.content;
                public interface SharedPreferences{boolean getBoolean(String k,boolean d);int getInt(String k,int d);String getString(String k,String d);Editor edit();
                void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l);void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l);
                interface OnSharedPreferenceChangeListener{void onSharedPreferenceChanged(SharedPreferences p,String k);}
                interface Editor{Editor putBoolean(String k,boolean v);Editor putInt(String k,int v);Editor putString(String k,String v);Editor remove(String k);void apply();}}
            """,
            "android/content/Context.java": """package android.content;import java.io.*;import java.util.*;
                public class Context{public static final int MODE_PRIVATE=0;public final File root;final Map<String,SharedPreferences> prefs=new java.util.concurrent.ConcurrentHashMap<>();
                public Context(File f){root=f;f.mkdirs();}public Context getApplicationContext(){return this;}public File getFilesDir(){return root;}
                public File getCodeCacheDir(){File f=new File(root,"cache");f.mkdirs();return f;}public ClassLoader getClassLoader(){return Context.class.getClassLoader();}
                public android.content.ContentResolver getContentResolver(){return new android.content.ContentResolver();}
                public SharedPreferences getSharedPreferences(String key,int mode){return prefs.computeIfAbsent(key,k->new Memory());}
                static class Memory implements SharedPreferences{
                    final Map<String,Object> values=new java.util.concurrent.ConcurrentHashMap<>();final Set<OnSharedPreferenceChangeListener> listeners=java.util.concurrent.ConcurrentHashMap.newKeySet();
                    public boolean getBoolean(String k,boolean d){return (Boolean)values.getOrDefault(k,d);}public int getInt(String k,int d){return (Integer)values.getOrDefault(k,d);}
                    public String getString(String k,String d){return (String)values.getOrDefault(k,d);}public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){listeners.add(l);}
                    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){listeners.remove(l);}public Editor edit(){return new Edit();}
                    class Edit implements Editor{Map<String,Object> next=new HashMap<>();public Editor putBoolean(String k,boolean v){next.put(k,v);return this;}
                        public Editor putInt(String k,int v){next.put(k,v);return this;}public Editor putString(String k,String v){next.put(k,v);return this;}public Editor remove(String k){next.put(k,null);return this;}
                        public void apply(){for(Map.Entry<String,Object> e:next.entrySet()){Object old=e.getValue()==null?values.remove(e.getKey()):values.put(e.getKey(),e.getValue());
                            if(!Objects.equals(old,e.getValue()))for(OnSharedPreferenceChangeListener l:listeners)l.onSharedPreferenceChanged(Memory.this,e.getKey());}}
                    }
                }}
            """,
            "android/content/ContentResolver.java": "package android.content;public class ContentResolver{public java.io.InputStream openInputStream(android.net.Uri uri)throws java.io.IOException{return new java.io.FileInputStream(uri.path);}}",
            "dalvik/system/DexClassLoader.java": "package dalvik.system;public class DexClassLoader extends ClassLoader{public DexClassLoader(String path,String cache,String lib,ClassLoader parent){super(parent);}}",
            "cat/ttcuz/framework/Accent.java": "package cat.ttcuz.framework;public class Accent{static void forget(){}public static int colour(){return 123;}}",
            "cat/ttcuz/framework/Themes.java": "package cat.ttcuz.framework;class Themes{static void repaintVisible(){}}",
            "cat/ttcuz/framework/Account.java": 'package cat.ttcuz.framework;public class Account{public static volatile String uid="111";public static String liveId(){return uid;}}',
            "cat/ttcuz/framework/Version.java": 'package cat.ttcuz.framework;class Version{static final String TIKTOK="47.2.41";}',
            "cat/ttcuz/framework/Diary.java": "package cat.ttcuz.framework;public class Diary{public static void note(String s){}}",
            "cat/ttcuz/framework/Screen.java": "package cat.ttcuz.framework;public class Screen{public static android.app.Activity now(){return null;}public static void say(String s){}}",
            "cat/ttcuz/framework/Ttcuz.java": 'package cat.ttcuz.framework;public class Ttcuz{public static android.content.Context app;static String PREFS="ttcuz";static android.content.Context context(){return app;}}',
            "cat/ttcuz/framework/AccountAppearance.java": 'package cat.ttcuz.framework;class AccountAppearance{static android.content.SharedPreferences prefs(android.content.Context c){return c.getSharedPreferences("appearance_"+Account.liveId(),0);}}',
            "testplugins/Good.java": """package testplugins;import cat.ttcuz.framework.plugin.*;import java.util.concurrent.atomic.AtomicInteger;
                public class Good extends TtcuzPlugin{public static final AtomicInteger starts=new AtomicInteger(),stops=new AtomicInteger(),events=new AtomicInteger(),delayed=new AtomicInteger();
                public void onLoad(){if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())throw new AssertionError("onLoad on main");}
                public void onStart(){if(android.os.Looper.myLooper()!=android.os.Looper.getMainLooper())throw new AssertionError("onStart off main");starts.incrementAndGet();}
                public void onStop(){stops.incrementAndGet();}public int onColour(int c){return cat.ttcuz.framework.PluginEngine.colour(c)+1;}
                public void onAccountChanged(String uid){ttcuz().accountPrefs().edit().putString("owner",uid).apply();}
                public void onEvent(String name,Object value){if(name.equals("first"))ttcuz().emit("second",value);if(name.equals("second")){events.incrementAndGet();ttcuz().emit("first",value);}}
                public void onSettings(PluginSettings rows){rows.toggle("on","Эффект",false).slider("power","Сила",0,100,50).action("Позже",()->ttcuz().later(()->delayed.incrementAndGet(),500));}}
            """,
            "testplugins/Bad.java": 'package testplugins;public class Bad extends cat.ttcuz.framework.plugin.TtcuzPlugin{public void onStart(){throw new IllegalStateException("test update failure");}}',
            "testplugins/Crash.java": 'package testplugins;public class Crash extends cat.ttcuz.framework.plugin.TtcuzPlugin{public int onColour(int c){throw new IllegalStateException("test callback failure");}}',
        }
        for name in ("PluginEngine.java", "plugin/TtcuzPlugin.java", "plugin/PluginContext.java", "plugin/PluginSettings.java"):
            sources["cat/ttcuz/framework/" + name] = (FRAMEWORK / name).read_text(encoding="utf8")
        # Actual extraction/path/DEX code, with a trivial metadata fixture reader.
        archive = """package cat.ttcuz.framework;import java.io.*;import java.util.*;import java.util.zip.*;
            class PluginPackage{static final long MAX_EXPANDED=512L*1024*1024;
            static class Metadata{String id,name,version,author="",description="",entry,tiktok="";int api=1;
                Metadata(Properties p){id=p.getProperty("id");name=id;entry=p.getProperty("entry");version=p.getProperty("version");}}
            static Metadata read(File folder)throws Exception{Properties p=new Properties();try(InputStream in=new FileInputStream(new File(folder,"manifest.json"))){p.load(in);}return new Metadata(p);}
        """
        for marker in ("static File child(", "static Metadata extract(", "static List<File> dexFiles(", "static void delete(", "static void compatible("):
            archive += safety.method("PluginPackage.java", marker)
        sources["cat/ttcuz/framework/PluginPackage.java"] = archive + "}"
        sources["Test.java"] = r"""import java.io.*;import java.util.*;import java.util.zip.*;import java.util.concurrent.*;
            import cat.ttcuz.framework.*;import cat.ttcuz.framework.plugin.*;
            public class Test{
                static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
                static void until(java.util.function.BooleanSupplier condition)throws Exception{long end=System.currentTimeMillis()+8000;while(!condition.getAsBoolean()){if(System.currentTimeMillis()>end)throw new AssertionError("engine timed out");Thread.sleep(10);}}
                static File bundle(File root,String id,String entry,String version)throws Exception{
                    File file=new File(root,id+version+entry.substring(entry.lastIndexOf('.')+1)+".ttplugin");
                    try(ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(file))){
                        zip.putNextEntry(new ZipEntry("manifest.json"));zip.write(("id="+id+"\nentry="+entry+"\nversion="+version).getBytes("UTF-8"));zip.closeEntry();
                        byte[] dex=new byte[112];System.arraycopy(new byte[]{'d','e','x','\n','0','3','5',0},0,dex,0,8);
                        zip.putNextEntry(new ZipEntry("classes.dex"));zip.write(dex);zip.closeEntry();
                    }return file;}
                static String install(File file)throws Exception{CountDownLatch done=new CountDownLatch(1);String[] error={null};PluginEngine.install(android.net.Uri.parse(file.getPath()),e->{error[0]=e;done.countDown();});check(done.await(8,TimeUnit.SECONDS),"install callback absent");return error[0];}
                public static void main(String[] args){try{
                    File root=new File(new File(Test.class.getProtectionDomain().getCodeSource().getLocation().toURI()),"state");root.mkdirs();
                    android.content.Context context=new android.content.Context(root);Ttcuz.app=context;PluginEngine.start(context);
                    check(install(bundle(root,"fixture","testplugins.Good","1"))==null,"good install failed");
                    check(PluginEngine.list().size()==1 && PluginEngine.list().get(0).loaded,"plugin not running");
                    check(PluginEngine.colour(10)==11,"colour hook or recursion guard failed");
                    PluginEngine.emit("first","hello");check(testplugins.Good.events.get()==1,"nested events lost or recursed");
                    PluginSettings settings=PluginEngine.settings("fixture",new android.app.Activity());check(settings.rows().size()==3,"settings unavailable");
                    File saved=new File(root,"ttcuz/plugins/data/fixture/keep");try(FileWriter out=new FileWriter(saved)){out.write("saved");}
                    int stopped=testplugins.Good.stops.get();
                    check(install(bundle(root,"fixture","testplugins.Absent","2"))!=null,"missing entry installed");
                    check(testplugins.Good.stops.get()==stopped,"preflight stopped working plugin");
                    check(install(bundle(root,"fixture","testplugins.Bad","3"))!=null,"failing update reported success");
                    check(PluginEngine.list().get(0).version.equals("1") && PluginEngine.colour(10)==11,"rollback lost working plugin");
                    check(saved.isFile(),"update erased data");
                    Account.uid="222";PluginEngine.accountChanged("222");
                    check(context.getSharedPreferences("ttcuz_plugin_fixture_222",0).getString("owner","").equals("222"),"account state not refreshed");
                    ((Runnable)settings.rows().get(2).value).run();PluginEngine.enable("fixture",false);
                    until(()->PluginEngine.colour(10)==10);Thread.sleep(650);check(testplugins.Good.delayed.get()==0,"stopped plugin task executed");
                    PluginEngine.enable("fixture",true);until(()->PluginEngine.colour(10)==11);
                    int starts=testplugins.Good.starts.get();PluginEngine.reload("fixture");until(()->testplugins.Good.starts.get()>starts && PluginEngine.list().get(0).loaded);
                    check(install(bundle(root,"crash","testplugins.Crash","1"))==null,"callback test plugin not loaded");
                    check(PluginEngine.colour(10)==11,"plugin exception changed host result");
                    until(()->{for(PluginEngine.Info i:PluginEngine.list())if(i.id.equals("crash"))return !i.loaded&&!i.error.isEmpty();return false;});
                    CountDownLatch removed=new CountDownLatch(1);PluginEngine.remove("fixture",error->{check(error==null,"remove failed");removed.countDown();});check(removed.await(8,TimeUnit.SECONDS),"remove timed out");
                    check(saved.isFile(),"removal erased saved settings/data");check(PluginEngine.colour(10)==10,"removed colour hook retained");
                    check(!PluginEngine.hasViewHooks(),"inherited no-op view hook registered");
                    System.exit(0);
                }catch(Throwable error){error.printStackTrace();System.exit(1);}}
            }"""
        self.run_java(sources)

    def test_archive_paths_and_dex_header(self):
        body = safety.method("PluginPackage.java", "static File child(") + safety.method("PluginPackage.java", "static List<File> dexFiles(")
        self.run_java({"Test.java": r"""import java.io.*;import java.util.*;
            public class Test{ """ + body + r"""
                static void reject(File root,String name)throws Exception{try{child(root,name);throw new AssertionError("accepted "+name);}catch(IOException expected){}}
                public static void main(String[] a)throws Exception{
                    File root=new File(new File(Test.class.getProtectionDomain().getCodeSource().getLocation().toURI()),"files");root.mkdirs();
                    for(String path:new String[]{"../escape","/escape","a/../../escape","a\\escape","C:escape","."})reject(root,path);
                    if(!child(root,"assets/icon.png").getCanonicalPath().startsWith(root.getCanonicalPath()+File.separator))throw new AssertionError();
                    File file=child(root,"classes.dex");byte[] dex=new byte[112];System.arraycopy(new byte[]{'d','e','x','\n','0','3','5',0},0,dex,0,8);
                    try(FileOutputStream out=new FileOutputStream(file)){out.write(dex);}if(dexFiles(root).size()!=1)throw new AssertionError();
                    file.setWritable(true,true);try(FileOutputStream out=new FileOutputStream(file)){out.write(new byte[5]);}
                    try{dexFiles(root);throw new AssertionError("short DEX accepted");}catch(IOException expected){}
                }}"""})


if __name__ == "__main__":
    unittest.main()
