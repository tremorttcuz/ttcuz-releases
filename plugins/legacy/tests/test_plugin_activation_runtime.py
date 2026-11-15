"""Exercise actual loading and activation without static policy approval."""
import unittest
import test_settings_safety_runtime as safety

class PluginActivationTest(unittest.TestCase):
 def test_start_and_settings_activation(self):
  start=safety.method('Plugins.java','    private static boolean start(')
  activate=safety.method('SettingsActivity.java','    private void activatePlugin(')
  sources={
   'android/content/Context.java':'package android.content;public class Context{public java.io.File getCodeCacheDir(){return new java.io.File(System.getProperty("java.io.tmpdir"));}public Context getApplicationContext(){return this;}}',
   'dalvik/system/DexClassLoader.java':'package dalvik.system;public class DexClassLoader extends ClassLoader{public DexClassLoader(String dex,String cache,String lib,ClassLoader parent){super(parent);}}',
   'Test.java':'''import android.content.Context;import java.io.File;import dalvik.system.DexClassLoader;
public class Test {
 static final String DEX="classes.dex";
 static class Info{File folder;String entry="Test$SamplePlugin",id="demo",trouble="previous audit rejection";}
 static class Live{Info info=new Info();MargyPlugin instance,settings;}
 static class PluginContext{interface Diarist{void note(String t);}PluginContext(Context c,String id,File home,Diarist d){}}
 static class MargyPlugin{void attach(PluginContext c){}void onStart(Context c){}}
 public static class SamplePlugin extends MargyPlugin{static int starts;public SamplePlugin(){}void onStart(Context c){starts++;}}
 static class Plugins {static boolean enabled;static Context context=new Context();static Live live;static void setEnabled(String id,boolean value){enabled=value;if(value)start(context,live);}}
 static class Net{static void away(String label,Runnable r){r.run();}}static class Diary{static void note(String s){}}
 boolean isFinishing(){return false;}void runOnUiThread(Runnable r){r.run();}int builds;void rebuild(){builds++;}
 '''+start+activate.replace('Plugins.Info','Info')+'''
 static void check(boolean b,String t){if(!b)throw new AssertionError(t);}
 public static void main(String[] args)throws Exception{
  File folder=java.nio.file.Files.createTempDirectory("plugin-activation").toFile();File dex=new File(folder,DEX);java.nio.file.Files.write(dex.toPath(),new byte[]{1});
  try{Live live=new Live();live.info.folder=folder;Plugins.live=live;Test screen=new Test();
   screen.activatePlugin(live.info,true);check(Plugins.enabled&&SamplePlugin.starts==1&&live.instance!=null&&live.settings==live.instance,"activate and start without audit/approval");
   check(live.info.trouble==null&&screen.builds==1,"old policy error cleared after successful load");
   screen.activatePlugin(live.info,false);check(!Plugins.enabled&&screen.builds==2,"deactivation still works");
   live.info.entry="missing.Plugin";check(!start(Plugins.context,live)&&live.info.trouble!=null,"real loading errors still reported");
  }finally{dex.setWritable(true);dex.delete();folder.delete();}
 }
}'''}
  safety.SafetyRuntimeTest().run_java(sources)
