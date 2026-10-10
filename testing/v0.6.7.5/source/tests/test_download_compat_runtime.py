"""Compatibility checks for update packages and legacy storage permission routing."""
import unittest
from pathlib import Path
import test_settings_safety_runtime as safety
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'inject/java/cat/narezany/margyt'
class DownloadCompatibilityTest(unittest.TestCase):
 def test_package_version_sdk_and_signing_rotation(self):
  sources={
   'android/os/Build.java':'package android.os;public class Build {public static String[] SUPPORTED_ABIS={"arm64-v8a","armeabi-v7a"};public static class VERSION {public static int SDK_INT=28;}}',
   'android/content/Context.java':'package android.content;public class Context {public android.content.pm.PackageManager manager=new android.content.pm.PackageManager();public android.content.pm.PackageManager getPackageManager(){return manager;}public String getPackageName(){return "test.app";}}',
   'android/content/pm/Signature.java':'package android.content.pm;public class Signature {String value;public Signature(String v){value=v;}public boolean equals(Object o){return o instanceof Signature && value.equals(((Signature)o).value);}public int hashCode(){return value.hashCode();}}',
   'android/content/pm/ApplicationInfo.java':'package android.content.pm;public class ApplicationInfo {public int minSdkVersion=23;}',
   'android/content/pm/SigningInfo.java':'package android.content.pm;public class SigningInfo {public Signature[] current,history;public boolean multiple;public SigningInfo(String v){current=new Signature[]{new Signature(v)};history=current;}public boolean hasMultipleSigners(){return multiple;}public Signature[] getApkContentsSigners(){return current;}public Signature[] getSigningCertificateHistory(){return history;}}',
   'android/content/pm/PackageInfo.java':'package android.content.pm;public class PackageInfo {public String packageName="test.app";public int versionCode=2;public ApplicationInfo applicationInfo=new ApplicationInfo();public Signature[] signatures=new Signature[]{new Signature("A")};public SigningInfo signingInfo=new SigningInfo("A");public long getLongVersionCode(){return versionCode;}}',
   'android/content/pm/PackageManager.java':'package android.content.pm;public class PackageManager {public static int GET_SIGNING_CERTIFICATES=1,GET_SIGNATURES=2;public PackageInfo next=new PackageInfo(),installed=new PackageInfo();public PackageInfo getPackageArchiveInfo(String p,int f){return next;}public PackageInfo getPackageInfo(String p,int f){return installed;}}',
   'cat/narezany/margyt/UpdatePackage.java':(JAVA/'UpdatePackage.java').read_text(encoding='utf-8'),
   'Test.java':'public class Test {public static void main(String[] args)throws Exception{cat.narezany.margyt.PackageHarness.main(args);}}',
   'cat/narezany/margyt/PackageHarness.java':"""package cat.narezany.margyt;import android.content.*;import android.content.pm.*;import java.io.*;public class PackageHarness {
static void reject(Context c,File f,String label)throws Exception{try{UpdatePackage.check(c,f);}catch(Exception e){return;}throw new AssertionError(label);}
public static void main(String[] a)throws Exception{
File f=File.createTempFile("test-update-",".apk");try{
try(java.util.zip.ZipOutputStream out=new java.util.zip.ZipOutputStream(new FileOutputStream(f))){for(String n:new String[]{"AndroidManifest.xml","classes.dex"}){out.putNextEntry(new java.util.zip.ZipEntry(n));out.write(new byte[]{1,2});out.closeEntry();}}
Context c=new Context();UpdatePackage.check(c,f);c.manager.next.packageName="other";reject(c,f,"wrong package");c.manager.next.packageName="test.app";
c.manager.next.applicationInfo.minSdkVersion=35;reject(c,f,"unsupported Android");c.manager.next.applicationInfo.minSdkVersion=23;
c.manager.next.versionCode=1;reject(c,f,"downgrade");c.manager.next.versionCode=3;
c.manager.next.signingInfo=new SigningInfo("B");reject(c,f,"wrong signing certificate");
c.manager.next.signingInfo.history=new Signature[]{new Signature("A"),new Signature("B")};UpdatePackage.check(c,f);
c.manager.next.signingInfo.multiple=true;reject(c,f,"multi-signer cannot borrow single signer history");c.manager.next.signingInfo.multiple=false;
try(java.util.zip.ZipOutputStream out=new java.util.zip.ZipOutputStream(new FileOutputStream(f))){for(String n:new String[]{"AndroidManifest.xml","classes.dex","lib/x86/libtest.so"}){out.putNextEntry(new java.util.zip.ZipEntry(n));out.write(new byte[]{1,2});out.closeEntry();}}
reject(c,f,"unsupported processor ABI");android.os.Build.SUPPORTED_ABIS=new String[]{"x86"};UpdatePackage.check(c,f);
android.os.Build.VERSION.SDK_INT=23;UpdatePackage.check(c,f);c.manager.next.signatures=new Signature[]{new Signature("B")};reject(c,f,"legacy wrong certificate");
try(FileOutputStream out=new FileOutputStream(f)){out.write("<html>server failure</html>".getBytes("UTF-8"));}reject(c,f,"HTML is not an APK");
}finally{f.delete();}}}"""}
  safety.SafetyRuntimeTest().run_java(sources)

 def test_permission_coalescing_grant_denial_and_modern_bypass(self):
  sources={
   'android/Manifest.java':'package android;public class Manifest {public static class permission {public static String WRITE_EXTERNAL_STORAGE="write";}}',
   'android/os/Build.java':'package android.os;public class Build {public static class VERSION {public static int SDK_INT=23;}}',
   'android/os/Bundle.java':'package android.os;public class Bundle {}',
   'android/os/Looper.java':'package android.os;public class Looper {public static Looper getMainLooper(){return new Looper();}}',
   'android/os/Handler.java':'package android.os;public class Handler {public Handler(Looper l){}public void post(Runnable r){r.run();}}',
   'android/content/pm/PackageManager.java':'package android.content.pm;public class PackageManager {public static int PERMISSION_GRANTED=0;}',
   'android/content/Intent.java':'package android.content;public class Intent {public static int FLAG_ACTIVITY_NEW_TASK=1;public Intent(Context c,Class<?> t){}public Intent addFlags(int f){return this;}}',
   'android/content/Context.java':'package android.content;public class Context {public static int permission=-1,launches;public int checkSelfPermission(String p){return permission;}public void startActivity(Intent i){launches++;}}',
   'android/app/Activity.java':'package android.app;public class Activity extends android.content.Context {public static int requests;protected void onCreate(android.os.Bundle b){}public void finish(){}public void requestPermissions(String[] p,int c){requests++;}public void onRequestPermissionsResult(int c,String[] p,int[] g){}protected void onDestroy(){}public boolean isChangingConfigurations(){return false;}}',
   'cat/narezany/margyt/Screen.java':'package cat.narezany.margyt;class Screen {static void say(String s){}}',
   'cat/narezany/margyt/Diary.java':'package cat.narezany.margyt;class Diary {static void note(String s){throw new AssertionError(s);}}',
   'cat/narezany/margyt/StoragePermissionActivity.java':(JAVA/'StoragePermissionActivity.java').read_text(encoding='utf-8'),
   'Test.java':'public class Test {public static void main(String[] args){cat.narezany.margyt.PermissionHarness.main(args);}}',
   'cat/narezany/margyt/PermissionHarness.java':"""package cat.narezany.margyt;import android.content.Context;public class PermissionHarness {static int runs;static void check(boolean b,String s){if(!b)throw new AssertionError(s);}public static void main(String[] a){
Context c=new Context();check(!StoragePermissionActivity.ensure(c,()->runs++),"legacy denied waits");check(!StoragePermissionActivity.ensure(c,()->runs++),"second save waits");check(Context.launches==1,"coalesce permission activity");
StoragePermissionActivity p=new StoragePermissionActivity();p.onCreate(null);check(android.app.Activity.requests==1 && runs==0,"permission requested before downloads");Context.permission=0;p.onRequestPermissionsResult(7301,new String[]{"write"},new int[]{0});check(runs==2,"grant resumes queued saves once");p.onDestroy();check(runs==2,"destroy does not repeat saves");
Context.permission=-1;StoragePermissionActivity.ensure(c,()->runs++);p=new StoragePermissionActivity();p.onCreate(null);p.onRequestPermissionsResult(7301,new String[]{"write"},new int[]{-1});p.onDestroy();check(runs==2,"denial does not attempt save");
android.os.Build.VERSION.SDK_INT=29;int launches=Context.launches;check(StoragePermissionActivity.ensure(c,()->runs++),"modern MediaStore needs no permission");check(Context.launches==launches,"modern phones bypass permission window");
android.os.Build.VERSION.SDK_INT=35;check(StoragePermissionActivity.ensure(c,()->runs++),"API 35 bypass");
}}"""}
  safety.SafetyRuntimeTest().run_java(sources)

 def test_installer_provider_metadata_and_mime(self):
  methods=''.join(safety.method('MargyProvider.java',m) for m in ['    public Cursor query','    private File sharedFile','    public String getType'])
  sources={
   'android/content/Context.java':'package android.content;public class Context {java.io.File files;public Context(java.io.File f){files=f;}public java.io.File getFilesDir(){return files;}}',
   'android/net/Uri.java':'package android.net;public class Uri {String name;public Uri(String n){name=n;}public String getLastPathSegment(){return name;}}',
   'android/database/Cursor.java':'package android.database;public interface Cursor {}',
   'android/database/MatrixCursor.java':'package android.database;public class MatrixCursor implements Cursor {public String[] columns;public Object[] row;public MatrixCursor(String[] c){columns=c;}public void addRow(Object[] r){row=r;}}',
   'android/provider/OpenableColumns.java':'package android.provider;public class OpenableColumns {public static String DISPLAY_NAME="name",SIZE="size";}',
   'Test.java':"""import java.io.*;import android.content.Context;import android.net.Uri;import android.database.Cursor;public class Test {
static Context context;Context getContext(){return context;}
"""+methods+"""
static void check(boolean b,String s){if(!b)throw new AssertionError(s);}public static void main(String[] args)throws Exception{
File folder=new File(System.getProperty("java.io.tmpdir"),"provider-"+java.util.UUID.randomUUID());File own=new File(folder,"margyt");own.mkdirs();File apk=new File(own,"update.apk");try{
try(FileOutputStream out=new FileOutputStream(apk)){out.write(new byte[]{1,2,3});}context=new Context(folder);Test provider=new Test();Uri uri=new Uri("update.apk");
check(provider.getType(uri).equals("application/vnd.android.package-archive"),"installer receives APK MIME");android.database.MatrixCursor c=(android.database.MatrixCursor)provider.query(uri,null,null,null,null);check(c.row[0].equals("update.apk") && c.row[1].equals(3L),"default name and size");
c=(android.database.MatrixCursor)provider.query(uri,new String[]{"size","name"},null,null,null);check(c.row[0].equals(3L) && c.row[1].equals("update.apk"),"projection order respected");
check(provider.query(new Uri("../update.apk"),null,null,null,null)==null,"traversal denied");check(provider.query(new Uri("missing.apk"),null,null,null,null)==null,"missing file not exposed");
}finally{apk.delete();own.delete();folder.delete();}}}"""}
  safety.SafetyRuntimeTest().run_java(sources)
