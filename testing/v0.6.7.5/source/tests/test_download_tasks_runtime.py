"""Run actual task registry and HTTPS download cancellation using a local fake transport."""
import pathlib,tempfile,subprocess,shutil,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
class DownloadTasksRuntimeTest(unittest.TestCase):
 @unittest.skipUnless(shutil.which('javac') and shutil.which('java'),'JDK required')
 def test_registry_cancel_retry_and_atomic_output(self):
  with tempfile.TemporaryDirectory() as d:
   w=pathlib.Path(d);sources={}
   for n in ('DownloadTasks','Net'):sources['cat/narezany/margyt/'+n+'.java']=(ROOT/('inject/java/cat/narezany/margyt/'+n+'.java')).read_text(encoding='utf-8')
   sources['cat/narezany/margyt/CloudProfileProvider.java']='package cat.narezany.margyt;class CloudProfileProvider {static String ROOT="https://test.invalid";}'
   sources['cat/narezany/margyt/Diary.java']='package cat.narezany.margyt;class Diary {static void note(String s){}}'
   sources['cat/narezany/margyt/Account.java']='package cat.narezany.margyt;class Account {static String uid="111",name="alice";static String liveId(){return uid;}static String profileHandle(){return name;}}'
   sources['cat/narezany/margyt/TasksHarness.java']='''package cat.narezany.margyt;import java.net.*;import java.io.*;public class TasksHarness {
    static byte[] payload=new byte[200000];static int advertised=payload.length;static int retries,status=200,calls,failures;static String length,redirect;
    static class Connection extends HttpURLConnection {Connection(URL u){super(u);}public void connect(){}public void disconnect(){}public boolean usingProxy(){return false;}public int getResponseCode(){calls++;if(failures-->0)return 503;return status;}public String getHeaderField(String n){return n.equals("Content-Length")?length:n.equals("Location")?redirect:null;}public int getContentLength(){return advertised;}public InputStream getInputStream(){return new ByteArrayInputStream(payload);}}
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    public static void main(String[] a)throws Exception {
     URL.setURLStreamHandlerFactory(protocol->protocol.equals("https")?new URLStreamHandler(){protected URLConnection openConnection(URL u){return new Connection(u);}}:null);
     File file=new File(a[0],"download.bin");java.nio.file.Files.write(file.toPath(),new byte[]{9,8});
     DownloadTasks.Control c=DownloadTasks.begin("mid","Video","Plugin",()->retries++);
     Account.uid="222";Account.name="bob";check(DownloadTasks.snapshot().get(0).owner.equals("@alice"),"owner stable across account switch");
     boolean ok=Net.download("https://test.invalid/file",file,(p,g,t)->{DownloadTasks.progress("mid",p);DownloadTasks.cancel("mid");},c);
     check(!ok,"mid-stream cancellation");check(java.util.Arrays.equals(java.nio.file.Files.readAllBytes(file.toPath()),new byte[]{9,8}),"cancel preserves previous output");check(!new File(file+".part").exists(),"partial removed");DownloadTasks.finish("mid",false);
     check(DownloadTasks.snapshot().get(0).state.equals("Отменено")&&DownloadTasks.snapshot().get(0).retry,"cancel is retryable");DownloadTasks.retry("mid");DownloadTasks.retry("mid");check(retries==1,"retry dispatched once");
     file.delete();c=DownloadTasks.begin("good","Plugin","Store",null);ok=Net.download("https://test.invalid/file",file,null,c);DownloadTasks.finish("good",ok);check(ok&&file.length()==payload.length,"complete output published");
     c=DownloadTasks.begin("truncated","APK","Update",null);advertised=payload.length+1;check(!Net.download("https://test.invalid/file",new File(a[0],"broken.apk"),null,c),"truncated file rejected");DownloadTasks.finish("truncated",false);
     advertised=payload.length;failures=1;calls=0;File recovered=new File(a[0],"recovered.bin");check(Net.download("https://test.invalid/file",recovered,null,null)&&calls==2,"temporary server failure retried");
     failures=10;calls=0;check(!Net.download("https://test.invalid/file",recovered,null,null)&&calls==3,"retry limit is bounded");check(recovered.length()==payload.length,"failed retry preserves completed file");failures=0;
     status=404;calls=0;check(!Net.download("https://test.invalid/file",recovered,null,null)&&calls==1,"permanent failure is not retried");
     status=206;check(!Net.download("https://test.invalid/file",recovered,null,null),"unsolicited partial response rejected");status=204;check(!Net.download("https://test.invalid/file",recovered,null,null),"no-content response rejected");
     status=200;byte[] original=payload;payload=new byte[0];advertised=0;check(!Net.download("https://test.invalid/file",recovered,null,null),"empty payload rejected");payload=original;advertised=-1;
     File chunked=new File(a[0],"chunked.bin");check(Net.download("https://test.invalid/file",chunked,null,null)&&chunked.length()==payload.length,"missing content length supported");
     length="3000000000";c=DownloadTasks.begin("huge","Huge","API",null);c.limit=400000;calls=0;check(!Net.download("https://test.invalid/file",recovered,null,c)&&calls==1,"64-bit size header respects limit without downloading");DownloadTasks.finish("huge",false);length=null;
     status=302;redirect="http://test.invalid/file";check(!Net.download("https://test.invalid/file",recovered,null,null),"TLS downgrade remains forbidden");status=200;redirect=null;
     for(File leftover:new File(a[0]).listFiles())check(!leftover.getName().endsWith(".part"),"all uniquely named partial files removed");
     for(int n=0;n<40;n++){String key="task"+n;DownloadTasks.begin(key,key,"API",null);DownloadTasks.finish(key,true);}check(DownloadTasks.snapshot().size()<=16,"finished history bounded");
    }
   }'''
   for n,s in sources.items():p=w/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf-8')
   run=subprocess.run(['javac','-source','8','-target','8','-encoding','UTF-8','-d',d]+[str(w/n) for n in sources],capture_output=True,text=True);self.assertEqual(0,run.returncode,run.stderr)
   run=subprocess.run(['java','-cp',d,'cat.narezany.margyt.TasksHarness',d],capture_output=True,text=True);self.assertEqual(0,run.returncode,run.stderr)
