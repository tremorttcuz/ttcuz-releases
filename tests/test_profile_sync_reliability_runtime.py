"""An outage must neither erase a confirmed gradient nor silently stop publication."""
import unittest
import test_settings_safety_runtime as safety


class ProfileSyncReliabilityTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_profile_http_distinguishes_absence_from_outage(self):
        methods = safety.method('Net.java', '    private static boolean secure(')
        methods += safety.method('Net.java', '    static byte[] profile(')
        self.run_java({'Test.java': '''import java.net.*;import java.io.*;
public class Test {
 static final int TIMEOUT=15000;static class Diary{static void note(String s){}}
 static int status=200;static boolean timeout,disconnected;static byte[] payload="style".getBytes();
 static class Wire extends HttpURLConnection {Wire(URL u){super(u);}public void connect(){}public void disconnect(){disconnected=true;}public boolean usingProxy(){return false;}public int getResponseCode()throws IOException{if(timeout)throw new SocketTimeoutException();return status;}public InputStream getInputStream(){return new ByteArrayInputStream(payload);}}
''' + methods + '''
 public static void main(String[] args)throws Exception {
  URL.setURLStreamHandlerFactory(p->p.equals("https")?new URLStreamHandler(){protected URLConnection openConnection(URL u){disconnected=false;return new Wire(u);}}:null);
  if(!new String(profile("https://example.test/profile")).equals("style")||!disconnected)throw new AssertionError("valid style lost");
  status=404;if(profile("https://example.test/profile")!=null||!disconnected)throw new AssertionError("confirmed deletion retained");
  for(int code:new int[]{429,500,503,302}){status=code;try{profile("https://example.test/profile");throw new AssertionError("outage treated as missing: "+code);}catch(IOException expected){}if(!disconnected)throw new AssertionError("connection leaked");}
  timeout=true;try{profile("https://example.test/profile");throw new AssertionError();}catch(SocketTimeoutException expected){}timeout=false;status=200;
  payload=new byte[16385];try{profile("https://example.test/profile");throw new AssertionError("oversize JSON accepted");}catch(IOException expected){}
 }
}'''})

    def test_private_backup_does_not_block_deliberate_public_edit(self):
        method = safety.method('CloudAppearance.java', '    static synchronized boolean publicationReady(')
        self.run_java({'Test.java': '''public class Test {
 static String selected,ready;static Pref own=new Pref();static class Pref{boolean dirty;boolean getBoolean(String k,boolean d){return dirty;}}
 static void ensure(String uid){}''' + method + '''
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] a){check(!publicationReady(null));selected="123";
  check(!publicationReady("123"));own.dirty=true;check(publicationReady("123"));check(!publicationReady("456"));
  own.dirty=false;ready="123";check(publicationReady("123"));ready=null;check(!publicationReady("123"));
 }}'''})

    def test_retry_uses_backoff_and_cancels_stale_account_or_edit(self):
        method = safety.method('TtcuzProfileSync.java', '    private static void retryPublication(')
        gate = (safety.ROOT/'inject/java/cat/ttcuz/framework/SyncGate.java').read_text(encoding='utf8')
        self.run_java({
            'cat/ttcuz/framework/SyncGate.java': gate,
            'cat/ttcuz/framework/Harness.java': '''package cat.ttcuz.framework;
public class Harness {
 static SyncGate sendGate=new SyncGate();static long publishRevision=1;static int sent;
 static class Account{static String uid="123";static String liveId(){return uid;}}
 static class ProfileStyle{static boolean share=true;static boolean sharing(){return share;}}
 static class Queue{Runnable work;long delay;void schedule(Runnable r,long d,java.util.concurrent.TimeUnit unit){work=r;delay=d;}}
 static Queue publishQueue=new Queue();static void publishOwn(){sent++;}
''' + method + '''
 public static void run(){sendGate.complete("style",false,System.currentTimeMillis());retryPublication("123",1);
 if(publishQueue.delay<29000||publishQueue.delay>30000)throw new AssertionError("retry ignores backoff");
 publishQueue.work.run();if(sent!=1)throw new AssertionError("no automatic resend");
 publishRevision=2;publishQueue.work.run();if(sent!=1)throw new AssertionError("old style resent");
 publishRevision=1;Account.uid="456";publishQueue.work.run();if(sent!=1)throw new AssertionError("wrong account");
 Account.uid="123";ProfileStyle.share=false;publishQueue.work.run();if(sent!=1)throw new AssertionError("opt out ignored");
 }}''',
            'Test.java': 'public class Test{public static void main(String[] args){cat.ttcuz.framework.Harness.run();}}',
        })
