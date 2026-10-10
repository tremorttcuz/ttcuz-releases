"""Execute actual layout batching and span reuse without an Android device."""
import unittest
from pathlib import Path
import test_settings_safety_runtime as safety
from test_dim_runtime import STUBS
ROOT=Path(__file__).resolve().parents[1]

class UiPerformanceTest(unittest.TestCase):
 def test_layout_bursts_are_deferred_and_final_layout_is_kept(self):
  source=(ROOT/'inject/java/cat/narezany/margyt/Themes.java').read_text(encoding='utf-8')
  begin=source.index('            root.getViewTreeObserver().addOnGlobalLayoutListener(',source.index('public static void watch'))
  end=source.index('                    });',begin)+len('                    });')
  listener=source[begin:end]
  sources=dict(STUBS)
  sources['cat/narezany/margyt/Dim.java']='package cat.narezany.margyt;class Dim {static int shape;static int layoutFingerprint(android.view.View v,int d){return shape;}}'
  for name in ['Fonts','DateOverlay','AppearanceColors','Badge','Accent']:
   method={'Fonts':'applyTree','DateOverlay':'resume','AppearanceColors':'repaint','Badge':'rewrite','Accent':'repaintTree'}[name]
   sources['cat/narezany/margyt/'+name+'.java']='package cat.narezany.margyt;class '+name+' {static void '+method+'(android.view.View v){}}'
  sources['Test.java']="""import android.view.*;public class Test {
static class Dim {static int shape;static int layoutFingerprint(View v,int d){return shape;}}
static class Fonts {static void applyTree(View v){}}static class DateOverlay {static void resume(View v){}}static class AppearanceColors {static void repaint(View v){}}static class Badge {static void rewrite(View v){}}static class Accent {static void repaintTree(View v){}}static final long QUIET=400;static int paints;static void repaint(View v){paints++;}
static void check(boolean b,String s){if(!b)throw new AssertionError(s);}public static void main(String[] args){
View root=new View();"""+listener+"""
Dim.shape=1;for(int i=0;i<100;i++)root.observer.layout.onGlobalLayout();
check(paints==0 && root.delayed.size()==1,"100 layouts must enqueue one pass and do no drawing work inline");
root.delayed.remove(0).run();check(paints==1,"first changed layout is painted");
root.observer.layout.onGlobalLayout();Dim.shape=2;root.observer.layout.onGlobalLayout();
check(root.delayed.size()==1,"trailing burst retains one pending update");root.delayed.remove(0).run();check(paints==2,"last layout survives throttle");
root.observer.layout.onGlobalLayout();root.delayed.remove(0).run();check(paints==2,"unchanged shape does not repaint");
}}"""
  safety.SafetyRuntimeTest().run_java(sources)

 def test_owned_spans_do_not_rewrite_text_until_accent_changes(self):
  owned=safety.method('Accent.java','    private static final class OwnedText')
  paint=safety.method('Accent.java','    private static void paintSpans')
  sources={
   'android/text/Spanned.java':'package android.text;public interface Spanned extends CharSequence {<T>T[] getSpans(int a,int b,Class<T> t);int getSpanStart(Object s);int getSpanEnd(Object s);int getSpanFlags(Object s);}',
   'android/text/style/ForegroundColorSpan.java':'package android.text.style;public class ForegroundColorSpan {int c;public ForegroundColorSpan(int c){this.c=c;}public int getForegroundColor(){return c;}}',
   'android/text/SpannableStringBuilder.java':"""package android.text;import android.text.style.ForegroundColorSpan;public class SpannableStringBuilder implements Spanned {public String text;public ForegroundColorSpan span;public SpannableStringBuilder(CharSequence s){text=s.toString();if(s instanceof Spanned)span=((Spanned)s).getSpans(0,s.length(),ForegroundColorSpan.class)[0];}public int length(){return text.length();}public char charAt(int i){return text.charAt(i);}public CharSequence subSequence(int a,int b){return text.substring(a,b);}public String toString(){return text;}@SuppressWarnings("unchecked")public <T>T[] getSpans(int a,int b,Class<T> t){return (T[])new ForegroundColorSpan[]{span};}public int getSpanStart(Object s){return 0;}public int getSpanEnd(Object s){return length();}public int getSpanFlags(Object s){return 33;}public void removeSpan(Object s){}public void setSpan(Object s,int a,int b,int f){span=(ForegroundColorSpan)s;}}""",
   'android/widget/TextView.java':'package android.widget;public class TextView {public CharSequence text;public Object tag;public int writes;public CharSequence getText(){return text;}public void setText(CharSequence s){text=s;writes++;}public Object getTag(int k){return tag;}public void setTag(int k,Object v){tag=v;}}',
   'android/widget/EditText.java':'package android.widget;public class EditText extends TextView {}',
   'Test.java':"""import android.widget.TextView;public class Test {static int target=2;static int colour(){return target;}static int accent(){return target;}static long spanRevision;static boolean historyTextView(android.widget.TextView t){return false;}static int sourced(int c){return c==1?target:c;}static final int SPAN_KEY=1;
"""+owned+paint+"""
static void check(boolean b,String s){if(!b)throw new AssertionError(s);}public static void main(String[] args){
android.text.SpannableStringBuilder original=new android.text.SpannableStringBuilder("hello");original.span=new android.text.style.ForegroundColorSpan(1);
TextView view=new TextView();view.text=original;paintSpans(view);check(view.writes==1,"initial mapping");
for(int i=0;i<100;i++)paintSpans(view);check(view.writes==1,"stable spans must not request 100 extra text layouts");
spanRevision++;paintSpans(view);check(view.writes==2,"palette invalidation forces refresh even with same accent");
target=3;paintSpans(view);check(view.writes==3 && ((android.text.Spanned)view.text).getSpans(0,5,android.text.style.ForegroundColorSpan.class)[0].getForegroundColor()==3,"accent change uses original span");
target=1;paintSpans(view);check(view.text==original,"reset restores native text");
TextView recycled=new TextView();recycled.text=original;target=2;paintSpans(recycled);recycled.text=original;paintSpans(recycled);check(recycled.writes==2,"native text rebinding is still recoloured");
}}"""}
  safety.SafetyRuntimeTest().run_java(sources)

 def test_heart_sweeps_do_not_block_predraw_or_repeat_for_a_burst(self):
  watch=safety.method('PinkButtons.java',' static void watch')
  sources=dict(STUBS)
  sources['Test.java']='import android.view.*;import android.app.Activity;public class Test {static int TAG=1,sweeps;static class Accent {static int TIKTOK=1,selected=2;static int colour(){return selected;}}static class Diary {static void note(String s){throw new AssertionError(s);}}static void sweep(View v,int c,int[] b){sweeps++;}\n'+watch+'\nstatic void check(boolean b,String s){if(!b)throw new AssertionError(s);}public static void main(String[] args){Activity a=new Activity();View root=new View();a.window.decor=root;watch(a);for(int i=0;i<100;i++)root.observer.draw.onPreDraw();check(sweeps==0 && root.delayed.size()==1,"100 frames queue one sweep without a synchronous tree walk");root.delayed.remove(0).run();check(sweeps==1,"deferred sweep runs once");android.os.SystemClock.now+=400;root.observer.draw.onPreDraw();Accent.selected=Accent.TIKTOK;root.delayed.remove(0).run();check(sweeps==1,"default accent skips queued recolouring");for(int i=0;i<100;i++)root.observer.draw.onPreDraw();check(root.delayed.isEmpty(),"default accent enqueues no scans");}}\n'
  safety.SafetyRuntimeTest().run_java(sources)
