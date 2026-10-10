"""Run the actual date row against the dimmer's alpha composition methods."""
import pathlib, unittest
import test_date_overlay_runtime as dates
import test_settings_safety_runtime as safety

ROOT=pathlib.Path(__file__).resolve().parents[1]

class DateDimTest(unittest.TestCase):
    run_java=safety.SafetyRuntimeTest.run_java

    def test_dimmed_date_is_stable_and_restores_host_alpha(self):
        files=dict(dates.STUBS)
        files['cat/narezany/margyt/DateOverlay.java']=(ROOT/'inject/java/cat/narezany/margyt/DateOverlay.java').read_text(encoding='utf-8')
        files['cat/narezany/margyt/Dim.java']='''package cat.narezany.margyt;import android.view.View;
class Dim {
 static boolean enabled=true;static boolean isEnabled(){return enabled;}static int strength(){return 35;}
 static boolean panelOpen(View root){return false;}static boolean panelContent(View v){return false;}
 static final java.util.Map<View,Dimmed> faded=new java.util.WeakHashMap<>();
 static final java.util.Map<View,Returning> returns=new java.util.WeakHashMap<>();
 static class Dimmed{float original,applied;}
 static class Returning{float original,last;android.animation.ValueAnimator animator;}
 static void track(View v){Dimmed d=new Dimmed();d.original=v.getAlpha();d.applied=d.original*.65f;faded.put(v,d);v.setAlpha(d.applied);}
'''+safety.method('Dim.java','    static float originalAlpha(')+safety.method('Dim.java','    public static float alphaFor(')+'}'
        files['cat/narezany/margyt/Harness.java']='''package cat.narezany.margyt;import android.widget.*;import android.view.*;
public class Harness {
 static class CountedDate extends TextView {int writes;CountedDate(){super(new android.content.Context());}public void setAlpha(float a){super.setAlpha(a);writes++;}}
 static void close(float a,float b,String why){if(Math.abs(a-b)>.001f)throw new AssertionError(why+": "+a);}
 public static void run(){
  FrameLayout root=new FrameLayout();root.width=400;root.height=900;
  TextView caption=new TextView(new android.content.Context());root.addView(caption,new FrameLayout.LayoutParams(100,40));
  CountedDate date=new CountedDate();date.setText("Yesterday");date.setAlpha(.8f);root.addView(date,new FrameLayout.LayoutParams(80,24));root.nativeDate=date;
  Dim.track(date);DateOverlay.bind(caption,"01.01.2021");close(date.getAlpha(),.6f*.65f,"date remains dimmed");
  int writes=date.writes;for(int i=0;i<120;i++)DateOverlay.update();if(date.writes!=writes)throw new AssertionError("still frames must not keep writing alpha");
  Dates.enabled=false;DateOverlay.update();close(date.getAlpha(),.8f*.65f,"disabling date restores host alpha composed once");
  Dim.enabled=false;date.setAlpha(Dim.faded.get(date).original);Dim.faded.clear();close(date.getAlpha(),.8f,"master off restores native alpha");
  Dates.enabled=true;DateOverlay.update();close(date.getAlpha(),.6f,"date without dimmer");Dates.enabled=false;DateOverlay.update();close(date.getAlpha(),.8f,"native alpha survives repeated switching");
 }
}'''
        files['Test.java']='public class Test{public static void main(String[] args){cat.narezany.margyt.Harness.run();}}'
        self.run_java(files)
