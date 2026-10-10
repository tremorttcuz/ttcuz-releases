"""Native caption geometry and removal of only the speed feedback label."""
import pathlib,shutil,subprocess,tempfile,unittest
from test_date_overlay_runtime import STUBS as BASE
ROOT=pathlib.Path(__file__).resolve().parents[1]
STUBS=dict(BASE)
STUBS['android/view/View.java']=STUBS['android/view/View.java'].replace('public class View implements ViewParent {', 'public class View implements ViewParent {public float translation;public View found;public float getTranslationY(){return translation;}public void setTranslationY(float y){translation=y;}').replace('public void setAlpha(float v){}','public void setAlpha(float v){alpha=v;}').replace('p[1]=top;','p[1]=top+(int)translation;')
STUBS['android/view/ViewGroup.java']=STUBS['android/view/ViewGroup.java'].replace('public boolean intercepted;', 'public boolean intercepted,clip=true;public boolean getClipChildren(){return clip;}').replace('public void setClipChildren(boolean b){}','public void setClipChildren(boolean b){clip=b;}')
STUBS['android/view/View.java']=STUBS['android/view/View.java'].replace('public View findViewById(int id){return id==0x7f0a93a5?nativeDate:null;}', 'public View findViewById(int id){return found;}')
STUBS['cat/narezany/margyt/SpeedGesture.java']='package cat.narezany.margyt;class SpeedGesture {static boolean low;static boolean captionsLow(android.view.View root){return low;}}'
STUBS['android/view/ViewGroup.java']=STUBS['android/view/ViewGroup.java'].replace('public boolean intercepted,clip=true;', 'public boolean intercepted,clip=true,paddingClip=true;public boolean getClipToPadding(){return paddingClip;}public void setClipToPadding(boolean b){paddingClip=b;}')
HARNESS='''package cat.narezany.margyt;import android.view.*;import android.widget.*;
public class CaptionHarness {
 public static class Holder {View view;public Holder(View v){view=v;}public View getContentView(){return view;}}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  FrameLayout root=new FrameLayout();root.height=900;root.width=400;View caption=new View();caption.height=24;caption.top=600;root.addView(caption,new FrameLayout.LayoutParams(300,24));root.found=caption;
  FeedCaptions.bindNative(new Holder(root));SpeedGesture.low=true;caption.observer.draw.onPreDraw();check(caption.translation==264 && !root.clip,"caption bottom uses reserved native speed slot without parent clipping");
  caption.observer.draw.onPreDraw();check(caption.translation==264,"no cumulative drift");
  SpeedGesture.low=false;caption.observer.draw.onPreDraw();check(caption.translation==0 && root.clip,"native position and clipping restored");
  TextView speed=new TextView(new android.content.Context());FeedCaptions.speedLabel(speed,"Скорость: 2x ▶▶");check(speed.alpha==0,"redundant native label hidden");
  TextView accelerated=new TextView(new android.content.Context());FeedCaptions.speedLabel(accelerated,"Ускорено x2");check(accelerated.alpha==0,"alternate native feedback hidden");
  FrameLayout feedbackRoot=new FrameLayout();feedbackRoot.height=900;View nativeCanvas=new View();feedbackRoot.addView(nativeCanvas,new FrameLayout.LayoutParams(200,40));feedbackRoot.found=nativeCanvas;
  nativeCanvas.alpha=.8f;FeedCaptions.bindSpeed(feedbackRoot);SpeedGesture.low=true;feedbackRoot.observer.draw.onPreDraw();check(nativeCanvas.alpha==0,"custom-drawn native feedback hidden without TextView");SpeedGesture.low=false;feedbackRoot.observer.draw.onPreDraw();check(nativeCanvas.alpha==.8f,"native alpha restored after speed pill leaves");
  TextView other=new TextView(new android.content.Context());FeedCaptions.speedLabel(other,"2 комментария");check(other.alpha==1,"other text untouched");
 }
}'''
class CaptionRuntimeTest(unittest.TestCase):
 @unittest.skipUnless(shutil.which('javac') and shutil.which('java'),'JDK required')
 def test_caption_position_and_speed_label(self):
  with tempfile.TemporaryDirectory() as directory:
   work=pathlib.Path(directory);sources=dict(STUBS)
   sources['cat/narezany/margyt/FeedCaptions.java']=(ROOT/'inject/java/cat/narezany/margyt/FeedCaptions.java').read_text(encoding='utf-8')
   sources['cat/narezany/margyt/CaptionHarness.java']=HARNESS
   for name,content in sources.items():
    p=work/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content,encoding='utf-8')
   result=subprocess.run(['javac','-source','8','-target','8','-encoding','UTF-8','-d',str(work)]+[str(work/n) for n in sources],capture_output=True,text=True);self.assertEqual(0,result.returncode,result.stderr)
   result=subprocess.run(['java','-cp',str(work),'cat.narezany.margyt.CaptionHarness'],capture_output=True,text=True);self.assertEqual(0,result.returncode,result.stderr)
