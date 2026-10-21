"""Execute the real emoji span adapter against all packaged Unicode sequences."""

import pathlib, subprocess, unittest
import test_settings_safety_runtime as safety

ROOT = pathlib.Path(__file__).resolve().parents[1]


class EmojiTextRuntimeTest(unittest.TestCase):
    def test_all_sequences_font_switch_images_editing_and_reset(self):
        sources = {
            "android/os/Build.java": "package android.os;public class Build{public static class VERSION{public static int SDK_INT=35;}}",
            "android/graphics/Typeface.java": "package android.graphics;public class Typeface{}",
            "android/content/res/AssetManager.java": 'package android.content.res;public class AssetManager{public java.io.InputStream open(String p)throws Exception{return new java.io.FileInputStream(System.getProperty("assets")+"/"+p.substring("ttcuz/".length()));}}',
            "android/content/Context.java": "package android.content;public class Context{public android.content.res.AssetManager getAssets(){return new android.content.res.AssetManager();}}",
            "android/text/Spanned.java": """package android.text;public interface Spanned extends CharSequence{int SPAN_EXCLUSIVE_EXCLUSIVE=33,SPAN_INCLUSIVE_INCLUSIVE=18;<T>T[] getSpans(int s,int e,Class<T> t);int getSpanStart(Object o);int getSpanEnd(Object o);int getSpanFlags(Object o);}""",
            "android/text/Spannable.java": "package android.text;public interface Spannable extends Spanned{void setSpan(Object o,int s,int e,int f);void removeSpan(Object o);}",
            "android/text/SpanStore.java": """package android.text;public class SpanStore implements Spanned {
                public String content;public java.util.List<Entry> spans=new java.util.ArrayList<>();
                static class Entry{Object o;int s,e,f;Entry(Object a,int b,int c,int d){o=a;s=b;e=c;f=d;}}
                public SpanStore(CharSequence text){content=text.toString();if(text instanceof Spanned){Spanned p=(Spanned)text;for(Object o:p.getSpans(0,text.length(),Object.class))spans.add(new Entry(o,p.getSpanStart(o),p.getSpanEnd(o),p.getSpanFlags(o)));}}
                public int length(){return content.length();}public char charAt(int i){return content.charAt(i);}public CharSequence subSequence(int s,int e){return content.substring(s,e);}public String toString(){return content;}
                public <T>T[] getSpans(int s,int e,Class<T> type){java.util.List<T> list=new java.util.ArrayList<>();for(Entry r:spans)if(type.isInstance(r.o)&&r.s<e&&r.e>s)list.add(type.cast(r.o));return list.toArray((T[])java.lang.reflect.Array.newInstance(type,list.size()));}
                private Entry entry(Object o){for(Entry r:spans)if(r.o==o)return r;return null;}
                public int getSpanStart(Object o){Entry r=entry(o);return r==null?-1:r.s;}public int getSpanEnd(Object o){Entry r=entry(o);return r==null?-1:r.e;}public int getSpanFlags(Object o){Entry r=entry(o);return r==null?0:r.f;}
                public void add(Object o,int s,int e,int f){remove(o);spans.add(new Entry(o,s,e,f));for(Entry r:new java.util.ArrayList<>(spans))if(r.o instanceof SpanWatcher)((SpanWatcher)r.o).onSpanAdded((Spannable)this,o,s,e);}public void remove(Object o){Entry old=entry(o);spans.removeIf(r->r.o==o);if(old!=null)for(Entry r:new java.util.ArrayList<>(spans))if(r.o instanceof SpanWatcher)((SpanWatcher)r.o).onSpanRemoved((Spannable)this,o,old.s,old.e);}
            }""",
            "android/text/SpannableString.java": "package android.text;public class SpannableString extends SpanStore implements Editable{public SpannableString(CharSequence s){super(s);}public void setSpan(Object o,int s,int e,int f){add(o,s,e,f);}public void removeSpan(Object o){remove(o);}}",
            "android/text/SpannedString.java": "package android.text;public class SpannedString extends SpanStore{public SpannedString(CharSequence s){super(s);}}",
            "android/text/style/ReplacementSpan.java": "package android.text.style;public class ReplacementSpan{}",
            "android/text/TextPaint.java": "package android.text;public class TextPaint{public android.graphics.Typeface face;public void setTypeface(android.graphics.Typeface f){face=f;}}",
            "android/text/style/MetricAffectingSpan.java": "package android.text.style;public abstract class MetricAffectingSpan{public abstract void updateMeasureState(android.text.TextPaint p);public abstract void updateDrawState(android.text.TextPaint p);}",
            "android/widget/TextView.java": """package android.widget;public class TextView{public enum BufferType{SPANNABLE}public CharSequence text="";public int writes,invalidations;public boolean nativeFilter;public android.content.Context getContext(){return new android.content.Context();}public CharSequence getText(){return text;}public void setText(CharSequence s){writes++;text=s instanceof android.text.Spanned?new android.text.SpannedString(s):s;}public void setText(CharSequence s,BufferType b){writes++;android.text.SpannableString result=new android.text.SpannableString(s);if(nativeFilter)result.setSpan(new android.text.style.ReplacementSpan(),0,2,33);text=result;}public void invalidate(){invalidations++;}}""",
            "cat/ttcuz/framework/Diary.java": "package cat.ttcuz.framework;class Diary{static void note(String s){throw new AssertionError(s);}}",
            "cat/ttcuz/framework/Fonts.java": 'package cat.ttcuz.framework;class Fonts{static String SYSTEM="";static String emoji(){return "twemoji";}}',
            "cat/ttcuz/framework/EmojiText.java": (
                ROOT / "inject/java/cat/ttcuz/framework/EmojiText.java"
            ).read_text(encoding="utf8"),
            "Test.java": """import android.text.*;import android.text.style.*;import android.widget.TextView;import android.graphics.Typeface;import cat.ttcuz.framework.EmojiText;
            public class Test{
                static void ck(boolean b,String s){if(!b)throw new AssertionError(s);}
                static MetricAffectingSpan[] emoji(TextView v){return ((Spanned)v.text).getSpans(0,v.text.length(),MetricAffectingSpan.class);}
                public static void main(String[] args)throws Exception{
                    System.setProperty("assets", "ASSET_ROOT");Typeface first=new Typeface(),second=new Typeface();TextView v=new TextView();
                    for(String sequence:java.nio.file.Files.readAllLines(java.nio.file.Paths.get(System.getProperty("assets"),"emoji/sequences-17.txt"))){
                        String source="abc "+sequence+" xyz";v.text=source;EmojiText.apply(v,first);MetricAffectingSpan[] spans=emoji(v);
                        ck(spans.length==1,"whole sequence: "+sequence);Spanned text=(Spanned)v.text;ck(text.getSpanStart(spans[0])==4&&text.getSpanEnd(spans[0])==4+sequence.length(),"letters untouched: "+sequence);
                        ck(source.equals(v.text.toString()),"text unchanged");
                    }
                    String source="Hi ❤️ 👩🏽‍💻 🇰🇿 1️⃣ 🙂‍↔️ 🫪";v.text=source;EmojiText.apply(v,first);ck(emoji(v).length==6,"mixed emoji categories");
                    int writes=v.writes;for(int i=0;i<100;i++)EmojiText.apply(v,first);ck(v.writes==writes,"unchanged text is not rewritten");
                    EmojiText.apply(v,second);for(MetricAffectingSpan span:emoji(v)){TextPaint draw=new TextPaint(),measure=new TextPaint();span.updateDrawState(draw);span.updateMeasureState(measure);ck(draw.face==second&&measure.face==second,"font switches for drawing and measurement");}
                    EmojiText.apply(v,null);ck(emoji(v).length==0&&source.equals(v.text.toString()),"system reset removes overrides");
                    SpannableString editable=new SpannableString("❤️ [smile] abc");ReplacementSpan nativeEmoji=new ReplacementSpan(),sticker=new ReplacementSpan();Object link=new Object();
                    editable.setSpan(nativeEmoji,0,2,33);editable.setSpan(sticker,3,10,33);editable.setSpan(link,11,14,17);v.text=editable;
                    EmojiText.apply(v,first);ck(v.text==editable,"editable and selection object preserved");ck(editable.getSpanStart(nativeEmoji)==-1,"native image emoji replaced");ck(editable.getSpanStart(sticker)==3&&editable.getSpanStart(link)==11,"sticker and link spans preserved");
                    int invalidations=v.invalidations;for(int i=0;i<100;i++)EmojiText.apply(v,first);ck(v.invalidations==invalidations,"unchanged editable is stable");
                    ReplacementSpan late=new ReplacementSpan();editable.setSpan(late,0,2,33);ck(editable.getSpanStart(late)==-1,"late EmojiCompat override replaced");
                    EmojiText.apply(v,second);EmojiText.apply(v,null);ck(editable.getSpanStart(nativeEmoji)==0&&editable.getSpanStart(late)==0,"native emoji spans restored on reset");
                    editable.content+=" 😁";EmojiText.apply(v,second);ck(emoji(v).length==2,"newly entered emoji styled");
                    v.text="plain 123 # * abc Привет";EmojiText.apply(v,first);ck(v.text instanceof String,"ordinary numbers and letters stay plain");
                    v.text="❤\\uFE0E";EmojiText.apply(v,first);ck(v.text instanceof String,"explicit text presentation preserved");
                    v.text="❤️";v.nativeFilter=true;EmojiText.apply(v,first);ck(((Spanned)v.text).getSpans(0,2,ReplacementSpan.class).length==0,"input filter cannot mask the selected emoji font");
                }
            }""".replace(
                "ASSET_ROOT", (ROOT / "assets").as_posix()
            ),
        }
        sources["android/text/Editable.java"]="package android.text;public interface Editable extends Spannable{}"
        sources["android/text/TextWatcher.java"]="package android.text;public interface TextWatcher{void beforeTextChanged(CharSequence s,int start,int count,int after);void onTextChanged(CharSequence s,int start,int before,int count);void afterTextChanged(Editable e);}"
        sources["android/text/SpanWatcher.java"]="package android.text;public interface SpanWatcher{void onSpanAdded(Spannable s,Object o,int start,int end);void onSpanRemoved(Spannable s,Object o,int start,int end);void onSpanChanged(Spannable s,Object o,int oldStart,int oldEnd,int start,int end);}"
        sources["android/widget/TextView.java"]=sources["android/widget/TextView.java"].replace('public enum BufferType', 'public java.util.List<android.text.TextWatcher> watchers=new java.util.ArrayList<>();public void addTextChangedListener(android.text.TextWatcher w){watchers.add(w);}public void removeTextChangedListener(android.text.TextWatcher w){watchers.remove(w);}public enum BufferType')
        try:
            safety.SafetyRuntimeTest().run_java(sources)
        except subprocess.CalledProcessError as error:
            self.fail((error.stdout or b"").decode() + (error.stderr or b"").decode())
