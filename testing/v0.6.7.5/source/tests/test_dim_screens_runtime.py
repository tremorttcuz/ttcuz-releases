"""Anti-burn must work on feed videos and stay out of the enlarged avatar, profiles and direct messages."""
import pathlib, unittest
from test_dim_runtime import STUBS
import test_settings_safety_runtime as safety

ROOT = pathlib.Path(__file__).resolve().parents[1]


class DimScreensTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_feed_stays_active_and_private_screens_stay_clean(self):
        files = dict(STUBS)
        for name in ('Dim', 'BadgeTarget', 'RecentProfiles', 'BottomChrome'):
            files['cat/narezany/margyt/' + name + '.java'] = (
                ROOT / 'inject/java/cat/narezany/margyt' / f'{name}.java').read_text(encoding='utf-8')
        files['android/view/ViewGroup.java'] = files['android/view/ViewGroup.java'].replace(
            'public void removeAllViews()',
            'public void removeView(View v){children.remove(v);v.parent=null;}public void removeAllViews()')
        old = 'public boolean getGlobalVisibleRect(android.graphics.Rect r){r.left=left;'
        assert old in files['android/view/View.java']
        files['android/view/View.java'] = files['android/view/View.java'].replace(
            old, 'public boolean offscreen;public boolean getGlobalVisibleRect(android.graphics.Rect r){if(offscreen)return false;r.left=left;')
        files['android/graphics/Rect.java'] = files['android/graphics/Rect.java']
        files['com/ss/android/ugc/profile/business/ur/enlarge/EnlargeAvatarActivity.java'] = (
            'package com.ss.android.ugc.profile.business.ur.enlarge;'
            'public class EnlargeAvatarActivity extends android.app.Activity {}')
        files['cat/narezany/margyt/Harness.java'] = '''package cat.narezany.margyt;import android.view.*;import android.widget.*;
public class Harness {
 static void close(float a,float b,String why){if(Math.abs(a-b)>.001f)throw new AssertionError(why+": "+a);}
 static TextView word(String text){TextView v=new TextView();v.text=text;return v;}
 static ViewGroup feed(com.ss.android.ugc.aweme.main.MainActivity a,ViewGroup[] controlsOut){
  ViewGroup root=new ViewGroup();root.width=400;root.height=900;a.window.decor=root;
  TextureView video=new TextureView();video.width=400;video.height=700;root.addView(video);
  TextView home=word("Главная");home.selected=true;home.top=860;home.height=40;root.addView(home);
  ViewGroup controls=new ViewGroup();controls.width=160;controls.height=80;controls.top=600;controls.addView(word("19,1 тыс."));root.addView(controls);
  controlsOut[0]=controls;return root;
 }
 public static void run(){
  Dim.setEnabled(true);Dim.setStrength(35);
  // 1. A feed video whose header carries a search field is still the feed: it must dim.
  com.ss.android.ugc.aweme.main.MainActivity a=new com.ss.android.ugc.aweme.main.MainActivity();
  ViewGroup[] c=new ViewGroup[1];ViewGroup root=feed(a,c);
  EditText search=new EditText();search.hint="Поиск";search.top=40;search.height=40;search.width=300;root.addView(search);
  Dim.applyNow(a);close(c[0].alpha,.65f,"feed video with a search button in its header dims");
  // 2. A comment panel that is merely 'shown' but not on screen is not an open sheet.
  View collapsed=new View();collapsed.setTag(0x5454434D,true);collapsed.offscreen=true;root.addView(collapsed);
  Dim.applyNow(a);close(c[0].alpha,.65f,"a collapsed comment panel does not switch the feed off");
  collapsed.offscreen=false;Dim.applyNow(a);close(c[0].alpha,1f,"an open comment panel still does");
  root.removeView(collapsed);Dim.applyNow(a);close(c[0].alpha,.65f,"closing it brings the effect back");
  // 3. A direct-message composer drawn as a plain text line.
  TextView composer=word("Написать...");composer.top=800;composer.height=40;composer.width=300;root.addView(composer);
  Dim.applyNow(a);close(c[0].alpha,1f,"chat composer text switches the effect off");
  root.removeView(composer);Dim.applyNow(a);close(c[0].alpha,.65f,"leaving the chat restores it");
  if(!Dim.forbidsEye(root)==false)throw new AssertionError("feed alone must not forbid the eye");
  root.addView(composer);if(!Dim.forbidsEye(root))throw new AssertionError("the eye must see the chat composer");root.removeView(composer);
  // 4. The enlarged avatar is a full-screen picture in its own activity: no eye, no dimming.
  int before=BurnEye.installs;
  com.ss.android.ugc.profile.business.ur.enlarge.EnlargeAvatarActivity avatar=new com.ss.android.ugc.profile.business.ur.enlarge.EnlargeAvatarActivity();
  ViewGroup full=new ViewGroup();full.width=400;full.height=900;avatar.window.decor=full;
  ImageView picture=new ImageView();picture.width=360;picture.height=360;picture.top=250;full.addView(picture);
  ViewGroup buttons=new ViewGroup();buttons.width=200;buttons.height=60;buttons.top=700;buttons.addView(word("Сохранить аватарку"));full.addView(buttons);
  Dim.applyNow(avatar);
  if(BurnEye.installs!=before)throw new AssertionError("the enlarged avatar must not get an eye");
  close(buttons.alpha,1f,"the enlarged avatar is never dimmed");
  if(!Dim.excludedActivity(avatar))throw new AssertionError("avatar activity is excluded");
 }
}'''
        files['Test.java'] = 'public class Test{public static void main(String[] args){cat.narezany.margyt.Harness.run();}}'
        self.run_java(files)


if __name__ == '__main__':
    unittest.main()
