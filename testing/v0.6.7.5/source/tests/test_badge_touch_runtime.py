"""Run Badge's real listener section; model data and painting are irrelevant here."""
import pathlib,shutil,subprocess,tempfile,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
STUBS={
'android/view/View.java':'''package android.view;public class View {public Object tag;public OnTouchListener listener;public interface OnTouchListener {boolean onTouch(View v,MotionEvent e);}public Object getTag(int k){return tag;}public void setTag(int k,Object t){tag=t;}public void setOnTouchListener(OnTouchListener l){listener=l;}}''',
'android/view/MotionEvent.java':'package android.view;public class MotionEvent {public static final int ACTION_DOWN=0,ACTION_UP=1,ACTION_MOVE=2,ACTION_CANCEL=3;int a;float x,y;public MotionEvent(int a,float x,float y){this.a=a;this.x=x;this.y=y;}public int getAction(){return a;}public float getX(){return x;}public float getY(){return y;}}',
'android/widget/TextView.java':'package android.widget;public class TextView extends android.view.View {public int paddingLeft,paddingTop,scrollX,scrollY;public android.content.res.Resources getResources(){return new android.content.res.Resources();}public CharSequence text;public CharSequence getText(){return text;}public android.text.Layout getLayout(){return new android.text.Layout();}public int getTotalPaddingLeft(){return paddingLeft;}public int getTotalPaddingTop(){return paddingTop;}public int getScrollX(){return scrollX;}public int getScrollY(){return scrollY;}}',
'android/content/res/Resources.java':'package android.content.res;public class Resources {public static class Metrics {public float density=1;}public Metrics getDisplayMetrics(){return new Metrics();}}',
'android/text/Layout.java':'package android.text;public class Layout {public int getLineForOffset(int a){return 0;}public float getPrimaryHorizontal(int i){return i*10;}public int getLineTop(int i){return 0;}public int getLineBottom(int i){return 20;}}',
'android/text/Spanned.java':'package android.text;public interface Spanned extends CharSequence {<T>T[] getSpans(int a,int b,Class<T> c);int getSpanStart(Object s);int getSpanEnd(Object s);}',
'android/text/style/ClickableSpan.java':'package android.text.style;public abstract class ClickableSpan {public abstract void onClick(android.view.View v);}',
}
HARNESS='''package cat.narezany.margyt;import android.view.*;import android.widget.*;
class BadgeHarness {
 static class Name implements android.text.Spanned {Badge.Tap tap=new Badge.Tap();Badge.MovableBadgeSpan image=new Badge.MovableBadgeSpan();
  public int length(){return 5;}public char charAt(int i){return 'a';}public CharSequence subSequence(int a,int b){return "a";}
  public int getSpanStart(Object s){return 4;}public int getSpanEnd(Object s){return 5;}
  public <T>T[] getSpans(int a,int b,Class<T> type){T[] out=(T[])java.lang.reflect.Array.newInstance(type,1);out[0]=type.cast(type==Badge.Tap.class?tap:image);return out;}}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static boolean touch(View v,int action,float x,float y){return v.listener.onTouch(v,new MotionEvent(action,x,y));}
 public static void main(String[] args){
  TextView name=new TextView();Name text=new Name();name.text=text;int[] nativeCalls={0};
  Badge.setOnTouchListener(name,(v,e)->{nativeCalls[0]++;return false;});Badge.listen(name);
  check(touch(name,0,49,10),"right half takes down");check(touch(name,1,50,10),"exclusive text end still takes up");
  check(text.tap.clicks==1 && nativeCalls[0]==0,"badge opens once without native click");
  touch(name,0,10,10);touch(name,1,10,10);check(nativeCalls[0]==2,"name touches preserve native handler");
  Badge.setOnTouchListener(name,(v,e)->{nativeCalls[0]+=10;return false;});
  touch(name,0,45,10);touch(name,3,45,10);check(text.tap.clicks==1,"cancel never opens badge");
  touch(name,0,45,10);touch(name,1,45,10);check(text.tap.clicks==2,"listener replacement preserves badge");
  touch(name,0,45,35);check(nativeCalls[0]==12,"below line is a native touch");
  text.image.dx=-10;text.image.dy=30;touch(name,0,35,40);touch(name,1,35,40);check(text.tap.clicks==3,"moved badge uses visual bounds");
  name.paddingLeft=20;name.paddingTop=12;name.scrollX=5;name.scrollY=3;
  check(Badge.previewMarkAt(name,50,49)==1,"preview touch follows padded/scrolled visual badge");
  check(Badge.previewMarkAt(name,35,40)==0,"old untranslated position is outside the visual badge");
  text.image.crown=true;check(Badge.previewMarkAt(name,50,49)==2,"crown and badge share the visual coordinate system");
  View ordinary=new View();View.OnTouchListener original=(v,e)->true;Badge.setOnTouchListener(ordinary,original);check(ordinary.listener==original,"unrelated view listener unchanged");
  System.out.println("badge boundary and gesture checks passed");
 }
}'''
class BadgeTouchRuntimeTest(unittest.TestCase):
 @unittest.skipUnless(shutil.which('javac') and shutil.which('java'),'JDK required')
 def test_badge_taps_and_native_gestures(self):
  code=(ROOT/'inject/java/cat/narezany/margyt/Badge.java').read_text(encoding='utf-8')
  start=code.index('    private static void listen(TextView view)')
  end=code.index('    /**\n     * Cut away',start)
  section=code[start:end].replace('private static void listen','static void listen')
  preview=code[code.index('    static int previewMarkAt('):code.index('    /** Move a preview mark')]
  section+=preview
  badge='''package cat.narezany.margyt;import android.view.*;import android.widget.*;import android.text.*;import android.text.style.*;
class Badge {static class Tap extends ClickableSpan {int clicks;public void onClick(View v){clicks++;}}static class MovableBadgeSpan {int dx,dy;boolean crown;}'''+section+'}'
  with tempfile.TemporaryDirectory() as directory:
   work=pathlib.Path(directory);sources=dict(STUBS)
   sources['cat/narezany/margyt/Badge.java']=badge
   sources['cat/narezany/margyt/BadgeHit.java']=(ROOT/'inject/java/cat/narezany/margyt/BadgeHit.java').read_text(encoding='utf-8')
   sources['cat/narezany/margyt/BadgeHarness.java']=HARNESS
   for name,content in sources.items():
    target=work/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(content,encoding='utf-8')
   built=subprocess.run(['javac','-source','8','-target','8','-encoding','UTF-8','-d',str(work)]+[str(work/name) for name in sources],capture_output=True,text=True)
   self.assertEqual(0,built.returncode,built.stderr)
   run=subprocess.run(['java','-cp',str(work),'cat.narezany.margyt.BadgeHarness'],capture_output=True,text=True)
   self.assertEqual(0,run.returncode,run.stderr)
