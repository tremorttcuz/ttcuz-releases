"""Native nickname resets, reused views and pending repair lifecycle."""
import pathlib,shutil,subprocess,tempfile,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
class ProfileResetTest(unittest.TestCase):
 @unittest.skipUnless(shutil.which('javac') and shutil.which('java'),'JDK required')
 def test_native_reset_repaired_without_loops_or_recycled_view_changes(self):
  full=(ROOT/'inject/java/cat/narezany/margyt/Badge.java').read_text(encoding='utf-8')
  watcher=full[full.index('    private static final int HEADER_WATCH_TAG'):full.index('    private static void previewMetrics')]
  match=full[full.index('    private static boolean matchesProfile'):full.index('    private static final long RECENT')]
  badge='''package cat.narezany.margyt;import android.view.View;import android.widget.TextView;class Badge {
static java.util.Map<TextView,ProfileBinding> profileBindings=new java.util.WeakHashMap<>();
static class ProfileBinding{String name,uid,originalName;boolean header=true;ProfileBinding(String n,String u){name=n;uid=u;originalName=Account.canonicalName(u,n);}}
static class MovableBadgeSpan{}static void refreshProfiles(){}
static String strip(String s){return s.replace("\\uE000","").trim();}
static String decorate(String name,String uid){return "Styled \\uE000";}
static CharSequence marked(TextView v,String s){return new android.text.Editable(strip(s),true);}
'''+watcher+match+'''
static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
public static void main(String[] args){
 TextView view=new TextView();profileBindings.put(view,new ProfileBinding("Styled","123"));watchHeader(view);watchHeader(view);
 check(view.watchers.size()==1,"One watcher per profile header");
 view.setText("Original✨",TextView.BufferType.SPANNABLE);check(view.pending.size()==1,"Native original name including emoji schedules repair");view.flush();
 check(view.text.styled && view.text.toString().equals("Styled"),"Native nickname reset gets the shared renderer");check(view.pending.isEmpty(),"Rendered text must not start another repair loop");
 view.setText("Unrelated user",TextView.BufferType.SPANNABLE);view.flush();check(view.text.toString().equals("Unrelated user"),"Recycled unrelated text is never overwritten");
 view.setText("Original✨",TextView.BufferType.SPANNABLE);profileBindings.put(view,new ProfileBinding("Other","456"));view.flush();check(!view.text.styled,"An obsolete queued repair cannot cross account bindings");
 profileBindings.put(view,new ProfileBinding("Styled","123"));view.setText("Original✨",TextView.BufferType.SPANNABLE);view.attached=false;view.flush();check(!view.text.styled,"Detached headers are not rendered");
}}
'''
  sources={
   'cat/narezany/margyt/Badge.java':badge,
   'cat/narezany/margyt/Account.java':'package cat.narezany.margyt;class Account{static String canonicalName(String uid,String fallback){return uid.equals("123")?"Original✨":fallback;}}',
   'cat/narezany/margyt/ProfileStyle.java':'package cat.narezany.margyt;class ProfileStyle{static class Gradient{}static class Decoration{}}',
   'android/view/View.java':'package android.view;public class View{public boolean attached=true;public java.util.Map<Integer,Object> tags=new java.util.HashMap<>();public Object getTag(int k){return tags.get(k);}public void setTag(int k,Object v){tags.put(k,v);}public boolean isAttachedToWindow(){return attached;}public interface OnAttachStateChangeListener{void onViewAttachedToWindow(View v);void onViewDetachedFromWindow(View v);}public void addOnAttachStateChangeListener(OnAttachStateChangeListener l){}}',
   'android/text/TextWatcher.java':'package android.text;public interface TextWatcher{void beforeTextChanged(CharSequence s,int start,int count,int after);void onTextChanged(CharSequence s,int start,int before,int count);void afterTextChanged(Editable s);}',
   'android/text/Editable.java':'''package android.text;public class Editable implements CharSequence{String value;public boolean styled;public Editable(String s,boolean b){value=s;styled=b;}public int length(){return value.length();}public char charAt(int i){return value.charAt(i);}public CharSequence subSequence(int a,int b){return value.substring(a,b);}public String toString(){return value;}public <T>T[] getSpans(int a,int b,Class<T> type){return (T[])java.lang.reflect.Array.newInstance(type,styled?1:0);}}''',
   'android/widget/TextView.java':'''package android.widget;public class TextView extends android.view.View{public enum BufferType{SPANNABLE}public android.text.Editable text=new android.text.Editable("",false);public java.util.List<android.text.TextWatcher> watchers=new java.util.ArrayList<>();public java.util.List<Runnable> pending=new java.util.ArrayList<>();public void addTextChangedListener(android.text.TextWatcher w){watchers.add(w);}public void post(Runnable r){pending.add(r);}public void flush(){while(!pending.isEmpty())pending.remove(0).run();}public CharSequence getText(){return text;}public void setText(CharSequence s,BufferType b){text=s instanceof android.text.Editable?(android.text.Editable)s:new android.text.Editable(s.toString(),false);for(android.text.TextWatcher w:watchers)w.afterTextChanged(text);}}'''
  }
  with tempfile.TemporaryDirectory() as directory:
   work=pathlib.Path(directory)
   for name,value in sources.items():
    p=work/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(value,encoding='utf-8')
   compile=subprocess.run(['javac','-encoding','UTF-8','-d',str(work)]+[str(work/name) for name in sources],capture_output=True,text=True)
   self.assertEqual(0,compile.returncode,compile.stderr)
   result=subprocess.run(['java','-cp',str(work),'cat.narezany.margyt.Badge'],capture_output=True,text=True)
   self.assertEqual(0,result.returncode,result.stderr)
