"""Font validation and rollback execute the production importer without Android codecs."""

import unittest
import test_settings_safety_runtime as safety


class NicknameFontTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_import_rollback_account_change_and_font_bounds(self):
        methods = "\n".join(
            safety.method("NicknameFont.java", marker)
            for marker in [
                "    public static boolean take(",
                "    static boolean validFile(",
                "    static String digest(",
            ]
        )
        self.run_java(
            {
                "android/net/Uri.java": "package android.net;public class Uri{}",
                "android/content/SharedPreferences.java": "package android.content;public class SharedPreferences{public boolean fail;public java.util.Map<String,String> values=new java.util.HashMap<>();public String getString(String k,String d){return values.getOrDefault(k,d);}public Editor edit(){return new Editor();}public class Editor{java.util.Map<String,String> pending=new java.util.HashMap<>();public Editor putString(String k,String v){pending.put(k,v);return this;}public void apply(){values.putAll(pending);}public boolean commit(){apply();return !fail;}}}",
                "android/content/Context.java": "package android.content;public class Context{public byte[] bytes;public Context getContentResolver(){return this;}public java.io.InputStream openInputStream(android.net.Uri u){return new java.io.ByteArrayInputStream(bytes);}}",
                "android/graphics/Typeface.java": "package android.graphics;public class Typeface{public static Typeface createFromFile(java.io.File f){return new Typeface();}}",
                "cat/ttcuz/framework/AccountAppearance.java": 'package cat.ttcuz.framework;class AccountAppearance{static String uid="123";static java.io.File dir;static android.content.SharedPreferences preferences=new android.content.SharedPreferences();static String scope(android.content.Context c){return uid;}static android.content.SharedPreferences prefs(android.content.Context c){return preferences;}}',
                "cat/ttcuz/framework/Net.java": "package cat.ttcuz.framework;class Net{static byte[] read(java.io.File f)throws Exception{return java.nio.file.Files.readAllBytes(f.toPath());}}",
                "cat/ttcuz/framework/Diary.java": "package cat.ttcuz.framework;class Diary{static void note(String s){}}",
                "cat/ttcuz/framework/Fonts.java": 'package cat.ttcuz.framework;class Fonts{static final String FILE="file";}',
                "cat/ttcuz/framework/NicknameFont.java": 'package cat.ttcuz.framework;import android.content.*;import android.net.Uri;import android.graphics.Typeface;import java.io.*;import java.util.*;public class NicknameFont{static final int LIMIT=1048576;static final String KEY="font",HASH="hash";static Map<String,Typeface> faces=new HashMap<>();static File file(Context c){return new File(AccountAppearance.dir,AccountAppearance.uid+"/nickname-font");}'
                + methods
                + """static void check(boolean v){if(!v)throw new AssertionError();}public static void run()throws Exception{
 AccountAppearance.dir=java.nio.file.Files.createTempDirectory("nickname-font-test").toFile();Context c=new Context();Uri uri=new Uri();byte[] valid=new byte[32];java.nio.ByteBuffer b=java.nio.ByteBuffer.wrap(valid);b.putInt(0,0x00010000);b.putShort(4,(short)1);b.putInt(20,28);b.putInt(24,4);
 try{check(validFile(valid)&&digest(valid).matches("[a-f0-9]{64}"));byte[] bad=valid.clone();java.nio.ByteBuffer.wrap(bad).putInt(20,-1);check(!validFile(bad));bad=valid.clone();java.nio.ByteBuffer.wrap(bad).putInt(24,5);check(!validFile(bad));check(!validFile(null)&&!validFile(new byte[LIMIT+1]));
 c.bytes=valid;check(take(c,uri,"123"));File saved=file(c);check(Arrays.equals(Net.read(saved),valid));c.bytes=new byte[]{1,2,3};check(!take(c,uri,"123")&&Arrays.equals(Net.read(saved),valid));c.bytes=valid.clone();c.bytes[31]=42;AccountAppearance.preferences.fail=true;check(!take(c,uri,"123")&&Arrays.equals(Net.read(saved),valid)&&AccountAppearance.preferences.getString(HASH,"").equals(digest(valid)));AccountAppearance.preferences.fail=false;AccountAppearance.uid="456";check(!take(c,uri,"123")&&!file(c).exists());check(take(c,uri,"456")&&Arrays.equals(Net.read(saved),valid));
 }finally{try(java.util.stream.Stream<java.nio.file.Path> files=java.nio.file.Files.walk(AccountAppearance.dir.toPath())){files.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{java.nio.file.Files.delete(p);}catch(Exception e){throw new RuntimeException(e);}});}}}}
""",
                "Test.java": "public class Test{public static void main(String[]a)throws Exception{cat.ttcuz.framework.NicknameFont.run();}}",
            }
        )
