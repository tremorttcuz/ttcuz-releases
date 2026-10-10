"""The cyan, the online green and span colours follow the accent; both loading dots too."""
import pathlib, shutil, unittest
import test_settings_safety_runtime as safety
method = safety.method
ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / 'inject/java/cat/narezany/margyt'

COLOR = '''package android.graphics;public class Color {
 public static int alpha(int c){return c>>>24;}
 public static void colorToHSV(int c,float[] o){float r=((c>>16)&255)/255f,g=((c>>8)&255)/255f,b=(c&255)/255f;
  float max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b)),d=max-min,h=0;
  if(d!=0){if(max==r)h=((g-b)/d)%6;else if(max==g)h=(b-r)/d+2;else h=(r-g)/d+4;h*=60;if(h<0)h+=360;}
  o[0]=h;o[1]=max==0?0:d/max;o[2]=max;}
 public static int HSVToColor(int a,float[] h){float c=h[2]*h[1],x=c*(1-Math.abs((h[0]/60f)%2-1)),m=h[2]-c,r,g,b;int k=(int)(h[0]/60f)%6;
  if(k==0){r=c;g=x;b=0;}else if(k==1){r=x;g=c;b=0;}else if(k==2){r=0;g=c;b=x;}else if(k==3){r=0;g=x;b=c;}else if(k==4){r=x;g=0;b=c;}else{r=c;g=0;b=x;}
  return (a<<24)|(Math.round((r+m)*255)<<16)|(Math.round((g+m)*255)<<8)|Math.round((b+m)*255);}}'''


class SecondaryAccentTest(unittest.TestCase):
    @unittest.skipUnless(shutil.which('javac') and shutil.which('java'), 'JDK required')
    def test_secondary_zones_measured_from_the_screens(self):
        palette = (JAVA / 'Palette.java').read_text(encoding='utf-8')
        sources = {
            'android/graphics/Color.java': COLOR,
            'cat/narezany/margyt/Palette.java': palette,
            'Test.java': '''import cat.narezany.margyt.Palette;
public class Test {
 static void yes(int c,String m){if(!Palette.secondary(c))throw new AssertionError(m);}
 static void no(int c,String m){if(Palette.secondary(c))throw new AssertionError(m);}
 public static void main(String[] a){
  yes(0xFF0FC1E5,"story plus");yes(0xFF20D5EC,"cyan");yes(0xFF25F4EE,"brand cyan");yes(0xFF00C5CD,"cyan 2");
  yes(0x330FC1E5,"cyan keeps working at low alpha (selected tab fill)");
  yes(0xFF1CD764,"online dot");yes(0xFF1BD864,"online dot, jpeg noise");
  no(0xFFFE2C55,"pink is the red family's, not this one");no(0xFFFE3B75,"hot pink");
  no(0xFF1E90FF,"plain blue");no(0xFF808080,"grey");no(0xFF000000,"black");no(0xFFFFFFFF,"white");
  no(0xFF146C2E,"dark green accent from the palette");no(0xFF006A6A,"dark teal accent from the palette");
  no(0xFF6750A4,"purple accent");no(0xFF0B57D0,"blue accent");
  if(Palette.toAccent(0x330FC1E5,0xFF6750A4)!=0x336750A4)throw new AssertionError("alpha kept");
  if(Palette.toAccent(0xFF1CD764,0xFF6750A4)!=0xFF6750A4)throw new AssertionError("opaque");
 }}'''}
        safety.SafetyRuntimeTest().run_java(sources)

    @unittest.skipUnless(shutil.which('javac') and shutil.which('java'), 'JDK required')
    def test_sourced_moves_cyan_and_green_only_for_a_chosen_accent(self):
        body = method('Accent.java', '    private static int sourced(')
        sources = {
            'android/graphics/Color.java': COLOR,
            'cat/narezany/margyt/Palette.java': (JAVA / 'Palette.java').read_text(encoding='utf-8'),
            'Test.java': '''import cat.narezany.margyt.Palette;
public class Test {
 static int TIKTOK=0xFFFE2C55,chosen=0xFF6750A4;
 static int colour(){return chosen;} static int accent(){return chosen;}
 static int swap(int c){return (c&0xFFFFFF)==0xFE2C55?(c&0xFF000000)|(chosen&0xFFFFFF):c;}
 static class Themes{static int recolour(int c,boolean b){return c;}}
 '''+body+'''
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a){
  check(sourced(0xFF0FC1E5)==chosen,"story plus -> accent");
  check(sourced(0xFF1CD764)==chosen,"online dot -> accent");
  check(sourced(0x2920D5EC)==0x296750A4,"cyan tab fill keeps alpha");
  check(sourced(0xFF25F4EE)==chosen,"loading dot 1 (cyan)");
  check(sourced(0xFFFE2C55)==chosen,"loading dot 2 (pink)");
  check(sourced(0xFF808080)==0xFF808080,"grey untouched");
  chosen=TIKTOK;
  check(sourced(0xFF0FC1E5)==0xFF0FC1E5,"nothing moves while the accent is TikTok's own");
  check(sourced(0xFF25F4EE)==TIKTOK,"the exact cyans behave as they did in 0.3.5");
 }}'''}
        safety.SafetyRuntimeTest().run_java(sources)

    @unittest.skipUnless(shutil.which('javac') and shutil.which('java'), 'JDK required')
    def test_span_colours_follow_the_accent_and_restore(self):
        spans = method('Accent.java', '    private static void paintSpans(')
        owned = method('Accent.java', '    private static final class OwnedText')
        history = method('Accent.java', '    static void historyText(')
        sources = {
            'android/graphics/drawable/Drawable.java':'package android.graphics.drawable;public class Drawable {public int colour,writes;public void setColorFilter(int c,android.graphics.PorterDuff.Mode m){colour=c;writes++;}}',
            'android/graphics/PorterDuff.java':'package android.graphics;public class PorterDuff {public enum Mode {SRC_IN}}',
            'android/text/style/DynamicDrawableSpan.java':'package android.text.style;public class DynamicDrawableSpan {android.graphics.drawable.Drawable d;public DynamicDrawableSpan(android.graphics.drawable.Drawable d){this.d=d;}public android.graphics.drawable.Drawable getDrawable(){return d;}}',

            'android/text/Spanned.java': 'package android.text;public interface Spanned extends CharSequence{<T> T[] getSpans(int s,int e,Class<T> k);int getSpanStart(Object o);int getSpanEnd(Object o);int getSpanFlags(Object o);}',
            'android/text/style/ForegroundColorSpan.java': 'package android.text.style;public class ForegroundColorSpan{int c;public ForegroundColorSpan(int c){this.c=c;}public int getForegroundColor(){return c;}}',
            'android/text/SpannableStringBuilder.java': '''package android.text;import java.util.*;
public class SpannableStringBuilder implements Spanned{String t;List<Object[]> sp=new ArrayList<>();
 public SpannableStringBuilder(CharSequence c){t=c.toString();if(c instanceof Spanned){Spanned s=(Spanned)c;for(Object o:s.getSpans(0,s.length(),Object.class))sp.add(new Object[]{o,s.getSpanStart(o),s.getSpanEnd(o),s.getSpanFlags(o)});}}
 public int length(){return t.length();}public char charAt(int i){return t.charAt(i);}public CharSequence subSequence(int a,int b){return t.substring(a,b);}public String toString(){return t;}
 public void removeSpan(Object o){sp.removeIf(x->x[0]==o);}
 public void setSpan(Object o,int s,int e,int f){sp.add(new Object[]{o,s,e,f});}
 @SuppressWarnings("unchecked")public <T> T[] getSpans(int s,int e,Class<T> k){List<Object> o=new ArrayList<>();for(Object[] x:sp)if(k.isInstance(x[0]))o.add(x[0]);return o.toArray((T[])java.lang.reflect.Array.newInstance(k,0));}
 int f(Object o,int i){for(Object[] x:sp)if(x[0]==o)return (Integer)x[i];return -1;}
 public int getSpanStart(Object o){return f(o,1);}public int getSpanEnd(Object o){return f(o,2);}public int getSpanFlags(Object o){return f(o,3);}}''',
            'android/widget/TextView.java': 'package android.widget;public class TextView{public int id;public int getId(){return id;}public void invalidate(){}CharSequence text="";java.util.Map<Integer,Object> tags=new java.util.HashMap<>();public CharSequence getText(){return text;}public void setText(CharSequence c){text=new android.text.SpannableStringBuilder(c);}public Object getTag(int k){return tags.get(k);}public void setTag(int k,Object v){tags.put(k,v);}}',
            'android/widget/EditText.java': 'package android.widget;public class EditText extends TextView{}',
            'android/content/res/ColorStateList.java':'package android.content.res;public class ColorStateList {int colour;public ColorStateList(int c){colour=c;}public int getDefaultColor(){return colour;}}',
            'Test.java': '''import android.widget.*;import android.text.*;import android.text.style.ForegroundColorSpan;import android.graphics.drawable.Drawable;import android.graphics.PorterDuff;import android.content.res.ColorStateList;
public class Test {
 static int chosen=0xFF6750A4;static int colour(){return chosen;}static int accent(){return chosen;}static long spanRevision;static boolean historyTextView(android.widget.TextView t){return t.getId()==0x7f0a91a0 || t.getId()==0x7f0a92e6;}static int sourced(int c){return (c&0xFFFFFF)==0xFE2C55?(c&0xFF000000)|(chosen&0xFFFFFF):c;}
 '''+owned+'''
 static final int SPAN_KEY=0x54545350,HISTORY_KEY=0x54544849,TIKTOK=0xFFFE2C55;
 static java.util.Map<android.graphics.drawable.Drawable,Boolean> nativeArtwork=new java.util.WeakHashMap<>();static java.util.Map<android.graphics.drawable.Drawable,Integer> historyIconColours=new java.util.WeakHashMap<>();
 static class NativeRead {static int reads;static Object field(Object o,String n){reads++;try{return o.getClass().getField(n).get(o);}catch(Exception e){return null;}}}
 static class Palette {static boolean captures(int c,int r){return c==r;}}static int swap(int c){return sourced(c);}
 public static class NativeIcon extends android.graphics.drawable.Drawable {public Integer LJIIZILJ=TIKTOK;}

 '''+spans+history+'''
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static int colourOf(TextView t){return ((Spanned)t.getText()).getSpans(0,t.getText().length(),ForegroundColorSpan.class)[0].getForegroundColor();}
 public static void main(String[] a){
  SpannableStringBuilder s=new SpannableStringBuilder("Уже есть аккаунт? Войти");s.setSpan(new ForegroundColorSpan(0xFFFE2C55),17,23,33);
  TextView t=new TextView();t.setText(s);
  paintSpans(t);check(colourOf(t)==chosen,"span moved onto the accent");
  check(t.getText().toString().equals("Уже есть аккаунт? Войти"),"text itself unchanged");
  Spanned out=(Spanned)t.getText();check(out.getSpanStart(out.getSpans(0,23,ForegroundColorSpan.class)[0])==17,"range kept");
  paintSpans(t);check(colourOf(t)==chosen,"second pass changes nothing");
  chosen=0xFF006A6A;paintSpans(t);check(colourOf(t)==chosen,"a new accent starts from the original, not the copy");
  chosen=0xFFFE2C55;paintSpans(t);check(colourOf(t)==0xFFFE2C55,"TikTok's own accent restores");
  class NativeHistoryColour extends ForegroundColorSpan {NativeHistoryColour(int c){super(c);}}
  SpannableStringBuilder history=new SpannableStringBuilder("user New posts");history.setSpan(new NativeHistoryColour(0xFFFE2C55),5,14,33);
  TextView h=new TextView();h.id=0x7f0a91a0;h.setText(history);chosen=0xFF6750A4;paintSpans(h);check(colourOf(h)==chosen,"native search tag subclass follows accent");
  TextView other=new TextView();other.setText(history);paintSpans(other);check(colourOf(other)==0xFFFE2C55,"same subclass outside history untouched");
  EditText e=new EditText();e.setText(s);chosen=0xFF6750A4;paintSpans(e);check(e.getTag(SPAN_KEY)==null,"input fields are left alone");
  NativeIcon icon=new NativeIcon();SpannableStringBuilder rich=new SpannableStringBuilder("user tag");rich.setSpan(new android.text.style.DynamicDrawableSpan(icon),5,6,33);
  TextView row=new TextView();row.id=0x7f0a91a0;row.setText(rich);for(int i=0;i<100;i++)historyText(row);
  check(icon.colour==chosen && icon.writes==1 && NativeRead.reads==1,"history icon updates once without repeated reflection");
  chosen=0xFF006A6A;historyText(row);check(icon.colour==chosen && icon.writes==2,"history icon follows changed accent");
  chosen=TIKTOK;historyText(row);check(icon.colour==TIKTOK,"native icon colour returns at default accent");
  nativeArtwork.put(icon,Boolean.TRUE);chosen=0xFF6750A4;historyText(row);check(icon.colour==TIKTOK,"protected streak art never tinted as search tag");
 }}'''}
        safety.SafetyRuntimeTest().run_java(sources)

    @unittest.skipUnless(shutil.which('javac') and shutil.which('java'), 'JDK required')
    def test_native_loading_dots_keep_the_two_colour_animation(self):
        accent = (JAVA / 'Accent.java').read_text(encoding='utf-8')
        colours = (JAVA / 'AppearanceColors.java').read_text(encoding='utf-8')
        tint = method('Accent.java', '    static int loadingDotTint(')
        self.assertIn('AppearanceColors.loadingDots(view)?loadingDotTint(colour):UiContrast.tint(view,sourced(colour))', accent)
        self.assertIn('return Palette.map(colour,TIKTOK,colour());', tint)
        self.assertIn('static boolean loadingDots(View view)', colours)
        self.assertIn('if (!animatedDots && view instanceof ImageView && !Accent.nativeArtworkView(view))', colours)
        self.assertIn('if (!animatedDots && name != null && !mediaName(name))', colours)
        sources = {
            'android/graphics/Color.java': COLOR,
            'cat/narezany/margyt/Palette.java': (JAVA / 'Palette.java').read_text(encoding='utf-8'),
            'Test.java': '''import cat.narezany.margyt.Palette;
public class Test {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a){int pink=0xFFFE2C55,cyan=0xFF25F4EE,accent=0xFF6750A4;
  check(Palette.map(pink,pink,accent)==accent,"pink animated dot follows the chosen accent");
  check(Palette.map(cyan,pink,accent)==cyan,"cyan animated dot remains native cyan");
  check(Palette.map(0x80FE2C55,pink,accent)==0x806750A4,"animated dot preserves alpha");
 }}'''}
        safety.SafetyRuntimeTest().run_java(sources)
