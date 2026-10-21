"""Geometry and text-cache regressions for native gradient accents."""
import unittest
import test_settings_safety_runtime as safety


class AccentGradientRuntimeTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_angles_and_transition_width_follow_control_bounds(self):
        code = safety.method("AccentGradient.java", "static float[] endpoints(")
        self.run_java({"Test.java": """public class Test {
            static void near(float a,float b){if(Math.abs(a-b)>.001f)throw new AssertionError(a+" != "+b);}
            """ + code + """
            public static void main(String[] args){
                float[] p=endpoints(200,40,0,100);near(p[0],0);near(p[2],200);near(p[1],20);near(p[3],20);
                p=endpoints(200,40,90,100);near(p[0],100);near(p[2],100);near(p[1],0);near(p[3],40);
                p=endpoints(200,40,180,100);near(p[0],200);near(p[2],0);
                p=endpoints(200,40,270,100);near(p[1],40);near(p[3],0);
                p=endpoints(200,40,0,50);near(p[0],50);near(p[2],150);
                p=endpoints(200,40,0,200);near(p[0],-100);near(p[2],300);
                p=endpoints(200,40,45,100);near(p[2]-p[0],p[3]-p[1]);
                for(int angle=0;angle<360;angle++)for(float v:endpoints(0,0,angle,10))
                    if(!Float.isFinite(v))throw new AssertionError("Unlaid-out control invalidated shader");
            }}"""})

    def test_animated_nickname_notifies_span_watcher_without_replacing_text(self):
        code = safety.method("ProfileStyle.java", "private static void redrawAnimatedName(")
        self.run_java({
            "android/text/Spannable.java": """package android.text;public interface Spannable extends CharSequence {
                <T>T[] getSpans(int start,int end,Class<T> type);int getSpanStart(Object span);
                int getSpanEnd(Object span);int getSpanFlags(Object span);void setSpan(Object span,int start,int end,int flags);}
            """,
            "android/widget/TextView.java": """package android.widget;public class TextView {
                public CharSequence text;public CharSequence getText(){return text;}}
            """,
            "Test.java": """import android.widget.TextView;import android.text.Spannable;
            public class Test { static boolean enabled=true;static boolean animated(char c){return enabled;}
                static class Gradient{char mark='x';}
            """ + code + """
                static class Text implements Spannable {
                    Gradient span=new Gradient();int notifications;
                    public int length(){return 4;}public char charAt(int i){return 'a';}
                    public CharSequence subSequence(int a,int b){return "name".substring(a,b);}
                    public <T>T[] getSpans(int a,int b,Class<T> t){return (T[])new Gradient[]{span};}
                    public int getSpanStart(Object o){return 0;}public int getSpanEnd(Object o){return 4;}
                    public int getSpanFlags(Object o){return 33;}
                    public void setSpan(Object o,int a,int b,int f){
                        if(o!=span||a!=0||b!=4||f!=33)throw new AssertionError("Style replaced");notifications++;}
                }
                public static void main(String[] args){TextView view=new TextView();Text text=new Text();view.text=text;
                    for(int i=0;i<120;i++)redrawAnimatedName(view);
                    if(view.text!=text||text.notifications!=120)throw new AssertionError("Text RenderNode not invalidated");
                    enabled=false;redrawAnimatedName(view);if(text.notifications!=120)throw new AssertionError("Disabled animation kept ticking");
                    view.text="plain";redrawAnimatedName(view);
                }}""",
        })


if __name__ == "__main__":
    unittest.main()
