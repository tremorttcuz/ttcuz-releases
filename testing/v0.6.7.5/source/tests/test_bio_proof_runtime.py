"""Execute the real bio-proof transaction with a deterministic native gateway."""
import pathlib,shutil,subprocess,tempfile,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
STUBS={
'android/os/Looper.java':'package android.os;public class Looper{public static Looper getMainLooper(){return null;}}',
'android/os/Handler.java':'package android.os;public class Handler{public static java.util.List<Runnable> jobs=new java.util.ArrayList<>();public Handler(Looper l){}public boolean postDelayed(Runnable r,long delay){jobs.add(r);return true;}}',
'android/content/Context.java': 'package android.content; public class Context {public static final int MODE_PRIVATE=0; public SharedPreferences getSharedPreferences(String s,int i){return cat.narezany.margyt.Margy.p;}}',
'android/content/SharedPreferences.java': """package android.content; public class SharedPreferences {
 public java.util.Map<String,Object> data=new java.util.HashMap<>();
 public boolean contains(String k){return data.containsKey(k);} public String getString(String k,String d){Object o=data.get(k);return o==null?d:(String)o;}
 public boolean getBoolean(String k,boolean d){Object o=data.get(k);return o==null?d:(Boolean)o;}
 public SharedPreferences edit(){return this;} public SharedPreferences putString(String k,String v){data.put(k,v);return this;}
 public SharedPreferences putBoolean(String k,boolean v){data.put(k,v);return this;} public SharedPreferences remove(String k){data.remove(k);return this;}
 public boolean commit(){return true;} }""",
'cat/narezany/margyt/Margy.java':'package cat.narezany.margyt; public class Margy {public static android.content.SharedPreferences p=new android.content.SharedPreferences();static final String PREFS="m";static android.content.Context context(){return new android.content.Context();}}',
'cat/narezany/margyt/Net.java':'package cat.narezany.margyt; class Net {static void away(String s,Runnable r){r.run();}}',
'cat/narezany/margyt/Account.java':'package cat.narezany.margyt; class Account {static String uid="123";static String id(){return uid;}static String liveId(){return uid;}static String profileHandle(){return "test";}}',
'cat/narezany/margyt/VerificationNotice.java':'package cat.narezany.margyt;class VerificationNotice{static int count;static void success(){count++;}}',
'cat/narezany/margyt/Screen.java':'package cat.narezany.margyt; class Screen {static void say(String s){}}',
'cat/narezany/margyt/Diary.java':'package cat.narezany.margyt; class Diary {static void note(String s){}}',
'cat/narezany/margyt/ProfilePreview.java':'package cat.narezany.margyt; class ProfilePreview {static void accountUpdated(){}}',
'cat/narezany/margyt/ProfileStyle.java':'package cat.narezany.margyt; class ProfileStyle {static boolean sharing;static void share(boolean b){sharing=b;}}',
'cat/narezany/margyt/CloudProfileProvider.java':"""package cat.narezany.margyt; class CloudProfileProvider {
 static int failures,checks;static String lastHandle;static Runnable checking;CloudProfileProvider(android.content.Context c){}String code(String uid){return "ttcuz-1234567890123456";}
 boolean verified(String uid){return false;}boolean verify(String uid,String h)throws java.io.IOException{checks++;lastHandle=h;if(failures-->0)throw new java.io.IOException("temporary failure");if(checking!=null)checking.run();return true;}}
""",
'cat/narezany/margyt/NativeBio.java':"""package cat.narezany.margyt; class NativeBio {
 static String text="",extra="mentions";static int saves,failOn;static boolean fail;
 static String handle(String uid){return "renamed";}static String bio(String uid){return text;}static String extras(String uid){return extra;}
 static boolean save(String uid,String expected,String wanted,String e){saves++;
 if(!e.equals(extra))throw new AssertionError("mentions changed");
 if(fail||saves==failOn)return false;if(!uid.equals(Account.id()) || !expected.equals(text))throw new AssertionError("unsafe save");text=wanted;return true;}}
""",
'cat/narezany/margyt/BioHarness.java':r"""package cat.narezany.margyt; public class BioHarness {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void reset(){android.os.Handler.jobs.clear();Margy.p.data.clear();NativeBio.saves=0;NativeBio.failOn=0;NativeBio.fail=false;CloudProfileProvider.checking=null;CloudProfileProvider.failures=0;CloudProfileProvider.checks=0;Account.uid="123";VerificationNotice.count=0;}
 public static void main(String[] a)throws Exception{
 reset();NativeBio.text="Hello\n@friend";BioProof.start();
 check(NativeBio.text.equals("Hello\n@friend")&&NativeBio.saves==2,"restore original exactly");check(!BioProof.pending("123"),"clear after confirmed restore");check(VerificationNotice.count==1,"one success notice after restore");
 reset();NativeBio.text="";BioProof.start();check(NativeBio.text.equals("")&&NativeBio.saves==2,"empty original");
 reset();NativeBio.text=new String(new char[160]).replace('\0','a');BioProof.start();check(NativeBio.saves==0&&!BioProof.pending("123"),"no truncation");
 reset();NativeBio.text="before";CloudProfileProvider.checking=()->NativeBio.text="human edit";BioProof.start();
 check(NativeBio.text.equals("human edit")&&NativeBio.saves==1&&BioProof.pending("123"),"preserve concurrent edit and backup");
 check(BioProof.original("123").equals("before"),"backup recoverable");
 reset();NativeBio.text="before";NativeBio.fail=true;BioProof.start();check(BioProof.pending("123"),"uncertain save retains backup");
 NativeBio.fail=false;NativeBio.text="before\nttcuz-1234567890123456";BioProof.recover();check(NativeBio.text.equals("before")&&!BioProof.pending("123"),"recover after delayed save");
 reset();Account.uid=null;NativeBio.text="before";BioProof.start();check(NativeBio.saves==0,"logged out never edits last saved profile");
 reset();NativeBio.text="before";CloudProfileProvider.checking=()->Account.uid="456";BioProof.start();check(NativeBio.saves==1&&BioProof.pending("123"),"never restore another account");
 reset();NativeBio.text="retry original";NativeBio.fail=true;BioProof.start();NativeBio.fail=false;BioProof.start();check(NativeBio.saves==3 && !BioProof.pending("123") && NativeBio.text.equals("retry original"),"repeat after uncertain save reinserts code and restores original");
 reset();NativeBio.text="network original";CloudProfileProvider.failures=1;BioProof.start();check(CloudProfileProvider.checks==2 && !BioProof.pending("123") && NativeBio.text.equals("network original"),"temporary network error retries instead of abandoning proof");check("renamed".equals(CloudProfileProvider.lastHandle),"use native current handle instead of remembered handle");
 reset();NativeBio.text="late original";NativeBio.fail=true;BioProof.start();check(android.os.Handler.jobs.size()==1,"uncertain native response schedules bounded recovery");NativeBio.fail=false;NativeBio.text="late original\nttcuz-1234567890123456";java.lang.reflect.Field clock=BioProof.class.getDeclaredField("recoverAt");clock.setAccessible(true);clock.setLong(null,0L);android.os.Handler.jobs.remove(0).run();check(!BioProof.pending("123") && NativeBio.text.equals("late original") && android.os.Handler.jobs.isEmpty(),"late save restored without opening profile or endless retries");
 reset();NativeBio.text="server failure";CloudProfileProvider.checking=()->{throw new IllegalStateException("blocked server");};BioProof.start();check(NativeBio.text.equals("server failure")&&!BioProof.pending("123")&&VerificationNotice.count==0,"no false success on rejected proof");
 reset();NativeBio.text="delayed restore";NativeBio.failOn=2;BioProof.start();check(BioProof.pending("123")&&VerificationNotice.count==0,"wait for exact restoration before success notice");java.lang.reflect.Field retryClock=BioProof.class.getDeclaredField("recoverAt");retryClock.setAccessible(true);retryClock.setLong(null,0L);NativeBio.failOn=0;BioProof.recover();check(!BioProof.pending("123")&&NativeBio.text.equals("delayed restore")&&VerificationNotice.count==1,"one notification after late restoration");
 System.out.println("Bio proof transaction scenarios passed");}}
"""
}
@unittest.skipUnless(shutil.which('javac') and shutil.which('java'),'JDK required')
class BioProofRuntimeTest(unittest.TestCase):
 def test_transaction_restoration_and_recovery(self):
  with tempfile.TemporaryDirectory() as d:
   directory=pathlib.Path(d);sources=dict(STUBS)
   sources['cat/narezany/margyt/BioProof.java']=(ROOT/'inject/java/cat/narezany/margyt/BioProof.java').read_text(encoding='utf-8')
   for name,source in sources.items():
    f=directory/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(source,encoding='utf-8')
   result=subprocess.run(['javac','-source','8','-target','8','-encoding','UTF-8','-d',d]+[str(directory/n) for n in sources],capture_output=True,text=True)
   self.assertEqual(0,result.returncode,result.stderr)
   result=subprocess.run(['java','-cp',d,'cat.narezany.margyt.BioHarness'],capture_output=True,text=True)
   self.assertEqual(0,result.returncode,result.stderr)
