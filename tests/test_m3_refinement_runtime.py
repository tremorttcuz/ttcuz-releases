"""Run the changed clock, nickname, shader and sticker logic without an Android device."""
import unittest
import test_settings_safety_runtime as safety


class MaterialRefinementTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_clock_chips_finish_colours_at_200ms_and_shape_at_300ms_without_restarting(self):
        chip = safety.method("TimeDial.java", "private final class Chip")
        self.run_java({
            "android/content/Context.java": "package android.content;public class Context{}",
            "android/graphics/drawable/GradientDrawable.java": """package android.graphics.drawable;
                public class GradientDrawable{public int colour;public float radius;
                public void setColor(int c){colour=c;}public void setCornerRadius(float r){radius=r;}}""",
            "android/widget/TextView.java": """package android.widget;public class TextView{
                public static final int VISIBLE=0;private boolean selected;public boolean shown=true;
                android.content.Context context;public TextView(android.content.Context c){context=c;}
                public void setBackground(android.graphics.drawable.GradientDrawable d){}public void setTextColor(int c){}
                public boolean isSelected(){return selected;}public void setSelected(boolean s){selected=s;}
                public boolean isAttachedToWindow(){return true;}public boolean isShown(){return shown;}
                public android.content.Context getContext(){return context;}
                protected void onWindowVisibilityChanged(int v){}protected void onDetachedFromWindow(){}}
            """,
            "android/view/animation/LinearInterpolator.java": "package android.view.animation;public class LinearInterpolator{}",
            "android/animation/ValueAnimator.java": """package android.animation;public class ValueAnimator{
                public static int builds;public long duration;float value;public boolean cancelled;
                public interface Listener{void update(ValueAnimator a);}Listener listener;
                public static ValueAnimator ofFloat(float... v){builds++;return new ValueAnimator();}
                public void setDuration(long d){duration=d;}public void setInterpolator(Object i){}
                public void addUpdateListener(Listener l){listener=l;}public Object getAnimatedValue(){return value;}
                public void start(){}public void cancel(){cancelled=true;}
                public void advance(long ms){value=Math.min(1,ms/(float)duration);listener.update(this);}}
            """,
            "Test.java": """import android.content.Context;import android.widget.TextView;
                import android.animation.ValueAnimator;import android.graphics.drawable.GradientDrawable;
                class Motion{static boolean enabled=true;static boolean enabled(Context c){return enabled;}}
                class Expressive{static int mix(int a,int b,float t){return Math.round(a+(b-a)*t);}}
                public class Test{
                    static class Skin{int text=10;}final Skin skin=new Skin();
                    static class Curve{float getInterpolation(float t){return t;}}
                    static final Curve COLOUR_EASE=new Curve(),SHAPE_SPRING=new Curve();
                    static void ck(boolean v,String m){if(!v)throw new AssertionError(m);}
                """ + chip + """
                    public static void main(String[] args){Test host=new Test();Chip chip=host.new Chip(new Context());
                        chip.select(false,16,100);ck(ValueAnimator.builds==0,"Initial display must not animate");
                        chip.select(true,12,200);ValueAnimator transition=chip.transition;
                        chip.select(true,12,200);ck(chip.transition==transition,"Repeated clock values restart animation");
                        transition.advance(200);ck(chip.colour==200&&chip.radius>12,"Colour finishes before shape");
                        transition.advance(300);ck(chip.radius==12,"Shape finishes at 300ms");
                        chip.select(false,16,100);chip.transition.advance(100);
                        int current=chip.colour;float radius=chip.radius;ValueAnimator interrupted=chip.transition;
                        chip.select(true,12,200);chip.transition.advance(0);
                        ck(interrupted.cancelled&&chip.colour==current&&chip.radius==radius,"Interruption must preserve displayed state");
                        chip.onWindowVisibilityChanged(8);ck(chip.transition==null&&chip.radius==12&&chip.colour==200,"Hidden chip retains no animation");
                        Motion.enabled=false;chip.select(false,16,100);
                        ck(chip.transition==null&&chip.radius==16&&chip.colour==100,"Reduced motion snaps");
                        chip.onDetachedFromWindow();ck(chip.transition==null,"Detach cancels motion");
                    }}""",
        })

    def test_clock_wraps_and_preserves_midnight(self):
        code = safety.method("TimeDial.java", "static float nearestAngle(")
        code += safety.method("TimeDial.java", "static int valueAtAngle(")
        code += safety.method("TimeDial.java", "int minuteOfDay(")
        self.run_java({"Test.java": """public class Test {
            int hour,minute; boolean afternoon;
            static void check(boolean v){if(!v)throw new AssertionError();}
            """ + code + """
            public static void main(String[] args){
                check(nearestAngle(350,0)==360 && nearestAngle(10,350)==-10);
                check(nearestAngle(720,30)==750 && nearestAngle(0,180)==-180);
                check(valueAtAngle(0,false)==12 && valueAtAngle(360,false)==12);
                for(int i=0;i<60;i++)check(valueAtAngle(i*6,true)==i);
                for(int i=1;i<=12;i++)check(valueAtAngle(i*30,false)==i);
                check(valueAtAngle(359,true)==0 && valueAtAngle(-6,true)==59);
                Test clock=new Test(); clock.hour=12;
                check(clock.minuteOfDay()==0);clock.afternoon=true;check(clock.minuteOfDay()==720);
                clock.hour=11;clock.minute=59;check(clock.minuteOfDay()==1439);
            }}"""})

    def test_bold_is_measured_glow_is_adjustable_and_preferences_are_cached(self):
        decoration = safety.method("ProfileStyle.java", "public static final class Decoration")
        self.run_java({
            "android/text/TextPaint.java": """package android.text; public class TextPaint {
                public boolean bold;public float radius;public int shadow;
                public int getColor(){return 0xffeeeeee;}public float getTextSize(){return 40;}
                public void setFakeBoldText(boolean v){bold=v;}
                public void setShadowLayer(float r,float x,float y,int c){radius=r;shadow=c;}
            }""",
            "android/text/style/MetricAffectingSpan.java": """package android.text.style;
                public abstract class MetricAffectingSpan {
                    public abstract void updateDrawState(android.text.TextPaint p);
                    public abstract void updateMeasureState(android.text.TextPaint p);
                }""",
            "Test.java": """import android.text.TextPaint;
                class ProfileStyle { static int rev,calls,power=100;static boolean heavy=true,halo=true,gradient=true;
                    static int revision(){return rev;}
                    static boolean bold(char c){calls++;return heavy;}
                    static boolean glow(char c){calls++;return halo;}
                    static int glowStrength(char c){calls++;return power;}
                    static boolean hasGradient(char c){calls++;return gradient;}
                    static int start(char c){calls++;return 0xff123456;}
                """ + decoration + """}
                public class Test {static void check(boolean b){if(!b)throw new AssertionError();}
                    public static void main(String[] args){
                        ProfileStyle.Decoration span=new ProfileStyle.Decoration('x');
                        TextPaint p=new TextPaint();span.updateMeasureState(p);check(p.bold);
                        span.updateDrawState(p);check(p.radius>9 && (p.shadow&0xffffff)==0x123456);
                        int calls=ProfileStyle.calls;for(int i=0;i<100;i++)span.updateDrawState(p);
                        check(calls==ProfileStyle.calls);
                        ProfileStyle.power=20;ProfileStyle.rev++;TextPaint soft=new TextPaint();
                        span.updateDrawState(soft);check(soft.radius<p.radius && soft.radius>0);
                        ProfileStyle.power=0;ProfileStyle.heavy=false;ProfileStyle.rev++;
                        TextPaint plain=new TextPaint();span.updateDrawState(plain);check(!plain.bold && plain.radius==0);
                    }}""",
        })

    def test_random_default_uses_favourites_and_sets_and_handles_an_empty_library(self):
        code = safety.method("Streaks.java", "public static StickerItem sticker(")
        self.run_java({"Test.java": """import java.util.*;
            class StickerItem{}class Streaks{
                static final Object seenStickers=new Object();
                static final Map<String,StickerItem> favourites=new LinkedHashMap<>();
                static final Map<String,StickerItem> setStickers=new LinkedHashMap<>();
            """ + code + """}
            public class Test{public static void main(String[] args){
                if(Streaks.sticker("")!=null)throw new AssertionError("Empty library fabricated a sticker");
                StickerItem one=new StickerItem(),two=new StickerItem();
                Streaks.favourites.put("one",one);Streaks.favourites.put("two",two);
                if(Streaks.sticker("two")!=two)throw new AssertionError("Explicit choice changed");
                for(int i=0;i<100;i++){StickerItem pick=Streaks.sticker("");
                    if(pick!=one && pick!=two)throw new AssertionError("Not from the user's library");}
                Streaks.favourites.clear();if(Streaks.sticker("")!=null)throw new AssertionError();
                Streaks.setStickers.put("set",two);
                if(Streaks.sticker("")!=two||Streaks.sticker("set")!=two)throw new AssertionError("Added set unavailable without favourites");
                Streaks.setStickers.clear();if(Streaks.sticker("")!=null)throw new AssertionError();
            }}"""})

    def test_accent_shader_reuses_unchanged_bounds_and_resets_when_disabled(self):
        fill = safety.method("AccentGradient.java", "static final class Fill")
        self.run_java({
            "android/graphics/Shader.java": "package android.graphics;public class Shader{public enum TileMode{CLAMP}}",
            "android/graphics/LinearGradient.java": """package android.graphics;
                public class LinearGradient extends Shader{public static int builds,end;
                public LinearGradient(float x,float y,float w,float h,int a,int b,TileMode t){builds++;end=b;}}""",
            "android/graphics/Paint.java": """package android.graphics;
                public class Paint{public Shader shader;public void setShader(Shader s){shader=s;}}""",
            "Test.java": """import android.graphics.*;
                class Accent{static int colour(){return 0xff112233;}}
                class Expressive{static int mix(int a,int b,float f){return b;}}
                class AccentGradient{static boolean on=true;static int last=0xff334455;
                static int degrees=45,spread=100;static int angle(){return degrees;}static int width(){return spread;}static boolean enabled(){return on;}static int end(){return last;}
                """ + safety.method("AccentGradient.java", "static float[] endpoints(") + fill + """}
                public class Test{public static void main(String[] args){
                    AccentGradient.Fill f=new AccentGradient.Fill();Paint p=new Paint();
                    for(int i=0;i<100;i++)f.apply(p,80,20,0x80112233);
                    if(LinearGradient.builds!=1 || LinearGradient.end!=0x80334455)throw new AssertionError();
                    f.apply(p,81,20,0x80112233);if(LinearGradient.builds!=2)throw new AssertionError();
                    AccentGradient.last=0xff556677;f.apply(p,81,20,0x80112233);
                    if(LinearGradient.builds!=3)throw new AssertionError();
                    AccentGradient.degrees=90;f.apply(p,81,20,0x80112233);
                    if(LinearGradient.builds!=4)throw new AssertionError("Angle did not invalidate shader");
                    AccentGradient.spread=50;f.apply(p,81,20,0x80112233);
                    if(LinearGradient.builds!=5)throw new AssertionError("Width did not invalidate shader");
                    AccentGradient.on=false;f.apply(p,81,20,0x80112233);
                    if(p.shader!=null)throw new AssertionError("Disabled gradient leaked a shader");
                }}""",
        })
