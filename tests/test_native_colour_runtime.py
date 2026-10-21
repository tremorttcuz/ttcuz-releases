"""Semantic Tux colours use the native resolver rather than Android resources."""
import unittest
import test_settings_safety_runtime as safety

class NativeColourTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_semantic_resource_ids_and_nullable_colour(self):
        methods = "\n".join(safety.method("Accent.java", marker) for marker in (
            "    private static Integer nativeColour(", "    public static void setTextColorRes(",
            "    public static void setTintColorRes("))
        methods = methods.replace("X.0GzZ", "Test$Colours")
        self.run_java({
            "Test.java": '''import java.lang.reflect.*;
class Context {}
class TextView {Context getContext(){return new Context();}}
class TuxIconView {Context getContext(){return new Context();}}
public class Test {
 static Method nativeColourResolver;static int text,tint,logs;
 public static class Colours {public static Integer LIZJ(int id,Context c){return id==1?0xff123456:null;}}
 static void setTextColor(TextView v,int c){text=c;}static void setTintColor(TuxIconView v,int c){tint=c;}
 static class Diary{static void note(String s){logs++;}}
''' + methods + '''
 public static void main(String[] args){setTextColorRes(new TextView(),1);setTintColorRes(new TuxIconView(),1);
 if(text!=0xff123456||tint!=text||logs!=0)throw new AssertionError("semantic colour failed");
 setTextColorRes(new TextView(),2);setTintColorRes(new TuxIconView(),2);
 if(text!=0xff123456||tint!=text||logs!=0)throw new AssertionError("unresolved colour overwritten");}
}'''})
