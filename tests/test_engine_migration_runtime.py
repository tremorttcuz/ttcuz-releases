"""Run the actual startup migration against files and persistent preferences."""

from java_source import code_without_literals

from pathlib import Path
import unittest
import test_settings_safety_runtime as safety

ROOT = Path(__file__).resolve().parents[1]
PKG = "cat/ttcuz/framework/"


class EngineMigrationTest(unittest.TestCase):
    def test_new_launcher_survives_disabled_previous_component(self):
        from ttcuz import manifest
        from ttcuz.axml import Axml
        from ttcuz.apkzip import Apk
        from ttcuz.build import Build

        apk = Apk(str(ROOT / "tests/data/fixture.apk"))
        parsed = Axml.parse(apk.read("AndroidManifest.xml"))
        apk.close()
        old, target = manifest.launcher_entry(parsed)
        ids = manifest.icon_ids(parsed)
        before = len(list(parsed.elements("activity-alias")))
        manifest.remove_launcher_intents(parsed)
        self.assertEqual(before, len(list(parsed.elements("activity-alias"))))
        self.assertEqual([], manifest.launcher_elements(parsed))
        manifest.add_icon_alias(parsed, Build.DEFAULT_ALIAS, target, ids[0], "ttcuz", True)
        result = Axml.parse(parsed.build())
        self.assertEqual((Build.DEFAULT_ALIAS, target), manifest.launcher_entry(result))
        launchers = manifest.launcher_elements(result)
        self.assertEqual(1, len(launchers))
        self.assertTrue(result.attr(launchers[0], "enabled").data)
        # Android retains old per-component overrides; the new alias has none.
        overrides = {old: False}
        name = result.attr_string(launchers[0], "name")
        self.assertTrue(overrides.get(name, bool(result.attr(launchers[0], "enabled").data)))

    def test_previous_data_current_choices_and_retry(self):
        sources = {
            "android/content/SharedPreferences.java": """package android.content;
public class SharedPreferences {
 public java.util.Map<String,Object> values=new java.util.HashMap<>();
 public java.util.Map<String,?> getAll(){return new java.util.HashMap<>(values);}
 public boolean contains(String k){return values.containsKey(k);}
 public int getInt(String k,int d){Object v=values.get(k);return v instanceof Integer?(Integer)v:d;}
 public Editor edit(){return new Editor();}
 public class Editor {java.util.Map<String,Object> pending=new java.util.HashMap<>();java.util.Set<String> removed=new java.util.HashSet<>();
 public Editor putString(String k,String v){pending.put(k,v);return this;}
 public Editor putBoolean(String k,boolean v){pending.put(k,v);return this;}
 public Editor putInt(String k,int v){pending.put(k,v);return this;}
 public Editor putLong(String k,long v){pending.put(k,v);return this;}
 public Editor putFloat(String k,float v){pending.put(k,v);return this;}
 public Editor putStringSet(String k,java.util.Set<String> v){pending.put(k,v);return this;}
 public Editor remove(String k){removed.add(k);return this;}
 public boolean commit(){for(String k:removed)values.remove(k);for(java.util.Map.Entry<String,Object> e:pending.entrySet())if(!removed.contains(e.getKey()))values.put(e.getKey(),e.getValue());return true;}
 }}""",
            "android/content/Context.java": """package android.content;public class Context {
 public static final int MODE_PRIVATE=0;public java.io.File dir;
 public Context(java.io.File d){dir=d;}
 public java.io.File getFilesDir(){return dir;}
 private java.util.Map<String,SharedPreferences> prefs=new java.util.HashMap<>();
 public SharedPreferences getSharedPreferences(String n,int mode){return prefs.computeIfAbsent(n,k->new SharedPreferences());}
}""",
            PKG
            + "Ttcuz.java": 'package cat.ttcuz.framework;class Ttcuz {static String PREFS="ttcuz";}',
            PKG
            + "Diary.java": "package cat.ttcuz.framework;class Diary {static int errors;static void note(String s){errors++;}}",
            PKG
            + "SettingsSchema.java": (ROOT / "inject/java" / PKG / "SettingsSchema.java").read_text(
                encoding="utf8"
            ),
            PKG + "MigrationHarness.java": """package cat.ttcuz.framework;
import java.io.*;import java.nio.file.*;import android.content.*;
public class MigrationHarness {
 static void ck(boolean v,String m){if(!v)throw new AssertionError(m);}
 static void put(File root,String path,String value)throws Exception{File f=new File(root,path);f.getParentFile().mkdirs();Files.writeString(f.toPath(),value);}
 static void reset()throws Exception{java.lang.reflect.Field f=SettingsSchema.class.getDeclaredField("checked");f.setAccessible(true);f.setBoolean(null,false);}
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("ttcuz-migration").toFile();Context c=new Context(dir);
  String old=new String(new char[]{109,97,114,103,121,116});
  SharedPreferences prefs=c.getSharedPreferences("ttcuz",0),legacy=c.getSharedPreferences(old,0);
  prefs.edit().putInt("schemaVersion",1).putString("font","file").putString("font_emoji","apple_mffm").putBoolean("textures_on",true).putString("textures_pack","old").putString("title_note_text","old note").commit();
  legacy.edit().putString("font","system").putString("extra","copied").putStringSet("set",java.util.Set.of("a")).commit();
  put(dir,old+"/font","text font");put(dir,old+"/emoji-emoji_file","emoji font");put(dir,old+"/offline-comments-123.zip","archive");
  put(dir,old+"/badges.json","old cache");put(dir,"ttcuz/badges.json","new cache");put(dir,old+"/plugins/old.dex","old code");put(dir,old+"/textures/pack.zip","old pack");
  SettingsSchema.ensure(c);
  ck(prefs.getInt("schemaVersion",0)==2,"migration committed");ck(prefs.values.get("font").equals("file")&&prefs.values.get("font_emoji").equals("apple_mffm"),"text and emoji choices preserved");
  ck(Files.readString(new File(dir,"ttcuz/font").toPath()).equals("text font"),"font moved");ck(Files.readString(new File(dir,"ttcuz/emoji-emoji_file").toPath()).equals("emoji font"),"emoji moved");
  ck(new File(dir,"ttcuz/offline-comments-123.zip").isFile(),"archive preserved");ck(Files.readString(new File(dir,"ttcuz/badges.json").toPath()).equals("new cache"),"new cache never overwritten");
  ck(!new File(dir,"ttcuz/plugins").exists()&&!new File(dir,"ttcuz/textures").exists(),"removed subsystems not migrated");
  ck(!prefs.contains("textures_on")&&!prefs.contains("textures_pack")&&!prefs.contains("title_note_text"),"obsolete settings removed");ck(Boolean.TRUE.equals(prefs.values.get("icon_pending")),"chosen icon reapplied to new components");
  put(dir,"ttcuz/font","replacement");reset();SettingsSchema.ensure(c);ck(Files.readString(new File(dir,"ttcuz/font").toPath()).equals("replacement"),"restart does not overwrite font");
  Context failed=new Context(Files.createTempDirectory("ttcuz-retry").toFile());put(failed.dir,old+"/nested/file","safe");put(failed.dir,"ttcuz/nested","obstruction");reset();SettingsSchema.ensure(failed);
  ck(failed.getSharedPreferences("ttcuz",0).getInt("schemaVersion",0)==0&&Diary.errors==1,"failed copy does not mark migration finished");
  Files.delete(new File(failed.dir,"ttcuz/nested").toPath());SettingsSchema.ensure(failed);ck(new File(failed.dir,"ttcuz/nested/file").isFile()&&failed.getSharedPreferences("ttcuz",0).getInt("schemaVersion",0)==2,"failure retried and data preserved");
 }
}""",
            "Test.java": "public class Test {public static void main(String[] args)throws Exception{cat.ttcuz.framework.MigrationHarness.main(args);}}",
        }
        safety.SafetyRuntimeTest().run_java(sources)

    def test_removed_features_do_not_enter_the_app(self):
        java = ROOT / "inject/java/cat/ttcuz/framework"
        for p in java.rglob("*.java"):
            source = p.read_text(encoding="utf8")
            self.assertNotRegex(
                code_without_literals(source),
                r"(?i)margyprovider|\bPlugins\.|\bTextures\.|\bScriptPlugins\.",
            )
        for name in [
            "Textures.java",
            "Plugins.java",
            "ScriptPlugins.java",
            "StoreActivity.java",
            "PluginEditorActivity.java",
        ]:
            self.assertFalse((java / name).exists())
        settings = (java / "SettingsActivity.java").read_text(encoding="utf8")
        self.assertNotIn("attributionText", settings)
        self.assertNotIn("Text.PLUGINS", settings)
        self.assertNotIn("Text.TEXTURES", settings)
        from ttcuz import build, dexpatch

        self.assertEqual("cat.ttcuz.framework", build.PACKAGE)
        self.assertTrue(all("narezany" not in target for target in dexpatch.rewrite_targets()))
        self.assertTrue((ROOT / "plugins/legacy/runtime/Plugins.java").is_file())
        self.assertIn("Все права защищены", (ROOT / "LICENSE").read_text(encoding="utf8"))
