"""The owner font wins for nickname glyphs, in measurement as well as drawing."""
import unittest
import test_settings_safety_runtime as safety


class NicknameSpanTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_owner_family_overrides_native_span_and_preserves_style(self):
        methods = safety.method('NicknameFont.java', '    static void apply(android.text.Spannable')
        methods += safety.method('NicknameFont.java', '    static final class Span extends')
        self.run_java({
            'android/graphics/Typeface.java': 'package android.graphics;public class Typeface{public static int NORMAL=0;public String family;int style;public Typeface(String f,int s){family=f;style=s;}public int getStyle(){return style;}public static Typeface create(Typeface t,int s){return new Typeface(t.family,s);}}',
            'android/text/TextPaint.java': 'package android.text;public class TextPaint{android.graphics.Typeface face;public void setTypeface(android.graphics.Typeface f){face=f;}public android.graphics.Typeface getTypeface(){return face;}}',
            'android/text/style/MetricAffectingSpan.java': 'package android.text.style;public abstract class MetricAffectingSpan{public abstract void updateDrawState(android.text.TextPaint p);public abstract void updateMeasureState(android.text.TextPaint p);}',
            'android/text/style/TypefaceSpan.java': 'package android.text.style;public class TypefaceSpan{}',
            'android/text/Spanned.java': 'package android.text;public interface Spanned{int SPAN_EXCLUSIVE_EXCLUSIVE=33;}',
            'android/text/Spannable.java': 'package android.text;public interface Spannable{<T>T[] getSpans(int a,int b,Class<T> type);void removeSpan(Object s);void setSpan(Object s,int a,int b,int flags);}',
            'Test.java': '''import android.graphics.Typeface;import android.text.*;import android.text.style.*;import java.util.*;
public class Test {
 static Typeface chosen=new Typeface("owner-upload",0);static Typeface face(char mark){return chosen;}
 static class Text implements Spannable {List<Object> spans=new ArrayList<>();int end;public <T>T[] getSpans(int a,int b,Class<T> type){return spans.stream().filter(type::isInstance).map(type::cast).toArray(n->(T[])java.lang.reflect.Array.newInstance(type,n));}public void removeSpan(Object s){spans.remove(s);}public void setSpan(Object s,int a,int b,int flags){spans.add(s);end=b;}}
''' + methods + '''
 public static void main(String[] args){Text text=new Text();Object emoji=new Object();text.spans.add(emoji);text.spans.add(new TypefaceSpan());text.spans.add(new Span('x'));apply(text,5,'y');
 if(text.getSpans(0,5,TypefaceSpan.class).length!=0||text.getSpans(0,5,Span.class).length!=1||!text.spans.contains(emoji)||text.end!=5)throw new AssertionError("competing family span or non-font span lost");
 Span span=text.getSpans(0,5,Span.class)[0];TextPaint paint=new TextPaint();for(String reader:new String[]{"system","serif","custom-reader"}){paint.setTypeface(new Typeface(reader,3));span.updateMeasureState(paint);if(!paint.getTypeface().family.equals("owner-upload")||paint.getTypeface().getStyle()!=3)throw new AssertionError("reader font wins measurement");paint.setTypeface(new Typeface(reader,1));span.updateDrawState(paint);if(!paint.getTypeface().family.equals("owner-upload")||paint.getTypeface().getStyle()!=1)throw new AssertionError("reader font wins drawing");}
 chosen=null;paint.setTypeface(new Typeface("fallback",0));span.updateDrawState(paint);if(!paint.getTypeface().family.equals("fallback"))throw new AssertionError("missing download destroys fallback");
 }
}'''})
