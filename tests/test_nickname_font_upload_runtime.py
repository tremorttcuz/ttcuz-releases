"""The actual HTTP writer must send a full 1 MiB font without lifting other JSON limits."""
import unittest
import test_settings_safety_runtime as safety


class NicknameUploadTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_large_font_request_and_regular_request_bounds(self):
        methods = '\n'.join(safety.method('Net.java', marker) for marker in (
            '    private static boolean secure(', '    static String postFont(',
            '    public static String postResult(',
            '    private static String postWire(String url, String json, boolean includeErrors) {',
            '    private static String postWire(String url, String json, boolean includeErrors, int limit) {'))
        self.run_java({'Test.java': '''import java.net.*;import java.io.*;
public class Test {
 static int LIMIT=4*1024*1024;static class Diary{static void note(String s){}}
 static class Wire extends HttpURLConnection {ByteArrayOutputStream body=new ByteArrayOutputStream();Wire(URL u){super(u);}public void connect(){}public void disconnect(){}public boolean usingProxy(){return false;}public OutputStream getOutputStream(){return body;}public int getResponseCode(){return 200;}public InputStream getInputStream(){return new ByteArrayInputStream("{\\"ok\\":true}".getBytes());}}
 static Wire last;static int requests;
''' + methods + '''
 public static void main(String[] args)throws Exception {
  URL.setURLStreamHandlerFactory(p->p.equals("https")?new URLStreamHandler(){protected URLConnection openConnection(URL u){requests++;return last=new Wire(u);}}:null);
  String large="{\\"data\\":\\""+java.util.Base64.getEncoder().encodeToString(new byte[1048576])+"\\"}";
  if(postFont("https://example.test/font",large)==null||last.body.size()!=large.length())throw new AssertionError("1 MiB SFNT upload lost");
  int before=requests;if(postResult("https://example.test/profile",large)!=null||requests!=before)throw new AssertionError("ordinary JSON cap lifted");
  if(postFont("https://example.test/font",new String(new char[1500001]))!=null||requests!=before)throw new AssertionError("oversized upload accepted");
  if(postFont("http://example.test/font","{}")!=null||requests!=before)throw new AssertionError("plaintext endpoint accepted");
  if(postResult("https://example.test/profile","{}") == null)throw new AssertionError("normal JSON broken");
 }
}'''})
