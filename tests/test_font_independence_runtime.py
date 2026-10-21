"""Execute the actual imports/selections and text-face builder together."""

import unittest
import test_settings_safety_runtime as safety


class FontIndependenceTest(unittest.TestCase):
    def test_text_font_survives_emoji_selection_import_and_reset(self):
        markers = (
            "public static String name()",
            "public static void choose(String",
            "public static String emoji()",
            "public static void chooseEmoji(",
            "private static void forget()",
            "static void reload()",
            "public static boolean take(Context context, android.net.Uri uri, boolean",
            "public static File file(",
            "static File emojiFile(",
            "public static Typeface chosenFace()",
            "private static Typeface build()",
            "private static Typeface build(Context",
            "public static Typeface preview(",
            "private static SharedPreferences prefs()",
        )
        body = "\n".join(safety.method("Fonts.java", m) for m in markers)
        sources = {
            "android/content/SharedPreferences.java": """package android.content;public class SharedPreferences {java.util.Map<String,String> m=new java.util.HashMap<>(),disk=new java.util.HashMap<>();public SharedPreferences restart(){SharedPreferences p=new SharedPreferences();p.disk.putAll(disk);p.m.putAll(disk);return p;}public String getString(String k,String d){return m.getOrDefault(k,d);}public Editor edit(){return new Editor();}public class Editor{public Editor putString(String k,String v){m.put(k,v);return this;}public void apply(){}public boolean commit(){disk.clear();disk.putAll(m);return true;}}}""",
            "android/net/Uri.java": "package android.net;public class Uri {public String text;public Uri(String t){text=t;}}",
            "android/content/Context.java": """package android.content;public class Context{public java.io.File root;public SharedPreferences prefs=new SharedPreferences();public Context(java.io.File r){root=r;}public java.io.File getFilesDir(){return root;}public Resolver getContentResolver(){return new Resolver();}public static class Resolver{public java.io.InputStream openInputStream(android.net.Uri uri){return new java.io.ByteArrayInputStream(uri.text.getBytes());}}}""",
            "android/graphics/Typeface.java": """package android.graphics;public class Typeface {public static final int NORMAL=0;public String name;Typeface(String n){name=n;}public static Typeface create(String n,int style){return new Typeface(n);}public static Typeface createFromFile(java.io.File f){try{String n=new String(java.nio.file.Files.readAllBytes(f.toPath()));if(n.equals("invalid"))throw new IllegalArgumentException();return new Typeface(n);}catch(java.io.IOException e){throw new RuntimeException(e);}}}""",
            "android/os/Looper.java": "package android.os;public class Looper{public static Looper getMainLooper(){return new Looper();}}",
            "android/os/Handler.java": "package android.os;public class Handler{public Handler(Looper l){}public void post(Runnable r){r.run();}}",
            "Test.java": """import android.content.*;import android.graphics.Typeface;import java.io.*;
public class Test{
 static final String SYSTEM="system",FILE="file",EMOJI_FILE="emoji_file",KEY="font",KEY_EMOJI="font_emoji";
 static String chosen,chosenEmoji;static Typeface face,styleBase,emojiTypeface;static boolean looked,emojiLoaded;static Typeface[] styled=new Typeface[4];
 static class Ttcuz {static Context context;static Context context(){return context;}}
 static class AccountAppearance {static String scope(Context c){return "123";}static SharedPreferences prefs(Context c){return c.prefs;}static File file(Context c,String n){return new File(c.root,n);}}
 static class EmojiPackManager {static Object find(Context c,String id){return java.util.Arrays.asList("system","twemoji","noto","blobmoji","apple_mffm","emoji_file").contains(id)?id:null;}}
 static class Diary {static void note(String t){}}static class Themes{static void repaintVisible(){}}
 """
            + body
            + """
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  File dir=java.nio.file.Files.createTempDirectory("font-independent").toFile();Ttcuz.context=new Context(dir);
  try{
   check(take(Ttcuz.context,new android.net.Uri("custom-text"),false),"import text font");
   byte[] original=java.nio.file.Files.readAllBytes(file(Ttcuz.context).toPath());
   for(String id:new String[]{"twemoji","noto","apple_mffm","blobmoji","system","emoji_file"}){
    chooseEmoji(Ttcuz.context,id);check(name().equals(FILE),"emoji selection preserves font preference");
    check(chosenFace().name.equals("custom-text"),"emoji choice preserves custom text family");
    check(preview(Ttcuz.context,id).name.equals("custom-text"),"preview preserves custom text");
    check(java.util.Arrays.equals(original,java.nio.file.Files.readAllBytes(file(Ttcuz.context).toPath())),"text font file unchanged");
   }
   check(take(Ttcuz.context,new android.net.Uri("custom-emoji"),true),"import custom emoji");
   check(name().equals(FILE)&&emoji().equals(EMOJI_FILE)&&chosenFace().name.equals("custom-text"),"both custom files independent");
   check(!take(Ttcuz.context,new android.net.Uri("invalid"),true),"reject unreadable emoji font");
   check(chosenFace().name.equals("custom-text")&&emoji().equals(EMOJI_FILE),"failed emoji import preserves both selections");
   Context saved=Ttcuz.context;saved.prefs=saved.prefs.restart();Ttcuz.context=null;reload();
   check(name().equals(SYSTEM)&&emoji().equals(SYSTEM)&&chosenFace()==null,"early reads do not cache system defaults");
   Ttcuz.context=saved;check(chosenFace().name.equals("custom-text")&&emoji().equals(EMOJI_FILE),"restart restores committed preferences after early reads");
   for(String preset:new String[]{"serif","monospace","sans-serif-light"}){choose(preset);chooseEmoji(Ttcuz.context,"twemoji");check(chosenFace().name.equals(preset),"preset family preserved");}
   choose(SYSTEM);check(chosenFace()==null,"emoji selection does not override native text face");
   choose(FILE);chooseEmoji(Ttcuz.context,"missing");check(emoji().equals(SYSTEM)&&chosenFace().name.equals("custom-text"),"missing emoji pack does not reset letters");
  }finally{for(File f:dir.listFiles())f.delete();dir.delete();}
 }
}""",
        }
        safety.SafetyRuntimeTest().run_java(sources)
