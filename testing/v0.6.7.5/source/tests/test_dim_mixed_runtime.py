"""Exercise the full dimmer on media, controls, recycled views and protected screens."""
import pathlib, unittest
from test_dim_runtime import STUBS
import test_settings_safety_runtime as safety

ROOT=pathlib.Path(__file__).resolve().parents[1]

class DimMixedTest(unittest.TestCase):
    run_java=safety.SafetyRuntimeTest.run_java

    def test_media_recycling_comments_chat_and_host_alpha(self):
        files=dict(STUBS)
        for name in ('Dim','BadgeTarget','RecentProfiles','BottomChrome'):
            files['cat/narezany/margyt/'+name+'.java']=(ROOT/'inject/java/cat/narezany/margyt'/f'{name}.java').read_text(encoding='utf-8')
        files['android/view/ViewGroup.java']=files['android/view/ViewGroup.java'].replace('public void removeAllViews()', 'public void removeView(View v){children.remove(v);v.parent=null;}public void removeAllViews()')
        files['cat/narezany/margyt/Harness.java']='''package cat.narezany.margyt;import android.view.*;import android.widget.*;
public class Harness {
 static void close(float a,float b,String why){if(Math.abs(a-b)>.001f)throw new AssertionError(why+": "+a);}
 static TextView word(String text){TextView v=new TextView();v.text=text;return v;}
 public static void run(){
  com.ss.android.ugc.aweme.main.MainActivity a=new com.ss.android.ugc.aweme.main.MainActivity();
  ViewGroup root=new ViewGroup();root.width=400;root.height=900;a.window.decor=root;
  TextureView video=new TextureView();video.width=400;video.height=900;root.addView(video);
  ImageView photo=new ImageView();photo.width=400;photo.height=600;root.addView(photo);
  ViewGroup controls=new ViewGroup();controls.width=160;controls.height=80;controls.top=700;TextView caption=word("Caption");controls.addView(caption);root.addView(controls);
  Dim.setEnabled(true);Dim.setStrength(35);Dim.applyNow(a);
  close(video.alpha,1,"video unchanged");close(photo.alpha,1,"photo unchanged");close(root.alpha,1,"media ancestors unchanged");close(controls.alpha,.65f,"controls faded");close(caption.alpha,1,"descendant must not fade twice");
  int writes=controls.alphaWrites;for(int i=0;i<60;i++)Dim.applyNow(a);if(controls.alphaWrites!=writes)throw new AssertionError("unchanged controls keep their alpha without writes");
  controls.setAlpha(Dim.alphaFor(controls,.4f));close(controls.alpha,.26f,"host alpha composes once");Dim.applyNow(a);close(controls.alpha,.26f,"no cumulative dimming");
  View comments=new View();comments.setTag(0x5454434D,true);root.addView(comments);Dim.applyNow(a);close(controls.alpha,.4f,"opening comments restores host alpha");root.removeView(comments);Dim.applyNow(a);close(controls.alpha,.26f,"closing comments restores effect");
  root.setTag(0x7f0a9dea,new com.ss.android.ugc.aweme.im.sdk.chat.ui.singleactivity.ChatRootFragment());Dim.applyNow(a);close(controls.alpha,.4f,"chat remains bright");root.setTag(0x7f0a9dea,null);Dim.applyNow(a);
  ProfileLayout.profile=true;Dim.applyNow(a);close(controls.alpha,.4f,"profile remains bright");ProfileLayout.profile=false;Dim.applyNow(a);
  a.focused=false;Dim.applyNow(a);close(controls.alpha,.26f,"focus loss preserves last frame");a.focused=true;
  Dim.watch(a);controls.removeAllViews();ImageView loadedPhoto=new ImageView();loadedPhoto.width=400;loadedPhoto.height=600;controls.addView(loadedPhoto);root.observer.draw.onPreDraw();
  close(controls.alpha,.4f,"recycled media container is restored before draw");close(loadedPhoto.alpha,1,"recycled photo remains bright");
  Dim.setEnabled(false);close(controls.alpha,.4f,"off preserves the native alpha");Dim.pause(a);
 }
}'''
        files['Test.java']='public class Test{public static void main(String[] args){cat.narezany.margyt.Harness.run();}}'
        self.run_java(files)
