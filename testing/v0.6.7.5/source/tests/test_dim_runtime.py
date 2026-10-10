"""Execute the real Dim.java on small Android doubles; no phone or SDK needed."""
import pathlib
import shutil
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
STUBS = {
    'com/ss/android/ugc/aweme/im/sdk/chat/ui/singleactivity/BaseChatRootFragment.java':'package com.ss.android.ugc.aweme.im.sdk.chat.ui.singleactivity;public class BaseChatRootFragment {}',
    'com/ss/android/ugc/aweme/im/sdk/chat/ui/singleactivity/ChatRootFragment.java':'package com.ss.android.ugc.aweme.im.sdk.chat.ui.singleactivity;public class ChatRootFragment extends BaseChatRootFragment {}',
    'com/ss/android/ugc/aweme/im/ChatActivity.java':'package com.ss.android.ugc.aweme.im;public class ChatActivity extends android.app.Activity {}',
    'android/graphics/Color.java':'package android.graphics;public class Color {public static int BLACK=0xff000000;}',
    'android/graphics/drawable/Drawable.java':'package android.graphics.drawable;public class Drawable {public int alpha=255;public int getAlpha(){return alpha;}public void setAlpha(int v){alpha=v;}public Object getColorFilter(){return null;}public void clearColorFilter(){}}',
    'android/graphics/drawable/ColorDrawable.java':'package android.graphics.drawable;public class ColorDrawable extends Drawable {public int colour;public ColorDrawable(int c){colour=c;}public int getColor(){return colour;}public void setColor(int c){colour=c;}}',
    'android/graphics/drawable/LayerDrawable.java':'package android.graphics.drawable;public class LayerDrawable extends Drawable {public Drawable[] layers;public LayerDrawable(Drawable[] d){layers=d;}public void setAlpha(int v){for(Drawable d:layers)d.setAlpha(v);}public int getAlpha(){return layers[0].getAlpha();}}',
    'android/widget/TextView.java': 'package android.widget; public class TextView extends android.view.View {public CharSequence text;public CharSequence getText(){return text;}public CharSequence getHint(){return null;}}',
    'android/widget/EditText.java': 'package android.widget; public class EditText extends TextView {public CharSequence hint;public CharSequence getHint(){return hint;}}',
    'X/ABCD.java': 'package X;public class ABCD extends android.view.ViewGroup {}',
    'com/ss/android/ugc/aweme/detail/ui/DetailActivity.java':'package com.ss.android.ugc.aweme.detail.ui;public class DetailActivity extends android.app.Activity {}',
    'com/ss/android/ugc/aweme/main/MainActivity.java': 'package com.ss.android.ugc.aweme.main;public class MainActivity extends android.app.Activity {}',
    'android/content/Context.java': '''package android.content;
        public class Context { public static final int MODE_PRIVATE=0;
        public SharedPreferences getSharedPreferences(String n,int m){return null;} }''',
    'android/content/SharedPreferences.java': '''package android.content;
        public interface SharedPreferences {
        boolean getBoolean(String k,boolean d); int getInt(String k,int d);
        boolean contains(String k); Editor edit();
        interface Editor { Editor putBoolean(String k,boolean v);
        Editor putInt(String k,int v); void apply(); } }''',
    'android/content/res/Resources.java': '''package android.content.res;
        public class Resources { public static int reads;public String getResourceEntryName(int id){reads++;return "unknown";} public android.util.DisplayMetrics getDisplayMetrics(){
        return new android.util.DisplayMetrics();} }''',
    'android/util/DisplayMetrics.java': '''package android.util;
        public class DisplayMetrics { public float density=1; }''',
    'android/graphics/Rect.java': '''package android.graphics;
        public class Rect { public int left,top,right,bottom;
        public boolean contains(int x,int y){return x>=left&&x<right&&y>=top&&y<bottom;} }''',
    'android/os/SystemClock.java': '''package android.os;
        public class SystemClock { public static long now=123456L;
        public static long uptimeMillis(){return now;} }''',
    'android/R.java': '''package android;
        public class R { public static class id { public static int content=1; } }''',
    'android/view/View.java': '''package android.view;
        public class View {
        public int getId(){return 0;}public android.content.res.Resources getResources(){return new android.content.res.Resources();}
        public static final int VISIBLE=0; public ViewGroup parent;public android.graphics.drawable.Drawable background;public android.graphics.drawable.Drawable getBackground(){return background;}public void setBackgroundDrawable(android.graphics.drawable.Drawable d){background=d;}
        public int visibility=0; public java.util.List<Runnable> delayed=new java.util.ArrayList<>();
        public ViewTreeObserver observer=new ViewTreeObserver();
        public boolean selected=false,activated=false;public CharSequence description;public int alphaWrites=0;
        public boolean isSelected(){return selected;}public boolean isActivated(){return activated;}public CharSequence getContentDescription(){return description;}
        public float alpha=1,x=0,y=0; public int left=0,top=0,width=100,height=100;
        public float getAlpha(){return alpha;} public void setAlpha(float a){alpha=a;alphaWrites++;}
        public float getTranslationX(){return x;} public void setTranslationX(float a){x=a;}
        public float getTranslationY(){return y;} public void setTranslationY(float a){y=a;}
        public java.util.Map<Integer,Object> tags=new java.util.HashMap<>();
        public Object getTag(int key){return tags.get(key);} public void setTag(int key,Object value){tags.put(key,value);}
        public Object getParent(){return parent;} public int getVisibility(){return visibility;}
        public boolean isShown(){return visibility==VISIBLE&&(parent==null||parent.isShown());}
        public int getWidth(){return width;} public int getHeight(){return height;}
        public void getLocationOnScreen(int[] a){a[0]=left;a[1]=top;}
        public boolean getGlobalVisibleRect(android.graphics.Rect r){r.left=left;r.top=top;
        r.right=left+width;r.bottom=top+height;return true;}
        public boolean isAttachedToWindow(){return true;}
        public boolean layoutRequested=false;public boolean isLayoutRequested(){return layoutRequested;}
        public void postDelayed(Runnable r,long delay){delayed.add(r);}
        public void removeCallbacks(Runnable r){delayed.remove(r);}
        public ViewTreeObserver getViewTreeObserver(){return observer;}
        public View findViewById(int id){return this;}
        }''',
    'android/view/ViewGroup.java': '''package android.view;
        public class ViewGroup extends View {
        private java.util.List<View> children=new java.util.ArrayList<>();
        public void addView(View v){children.add(v);v.parent=this;}public void removeAllViews(){for(View v:children)v.parent=null;children.clear();}
        public static int childReads;public int getChildCount(){childReads++;return children.size();}
        public View getChildAt(int n){return children.get(n);} }''',
    'android/widget/ImageView.java': 'package android.widget; public class ImageView extends android.view.View {}',
    'android/view/ViewTreeObserver.java': '''package android.view;
        public class ViewTreeObserver {
        public interface OnGlobalLayoutListener { void onGlobalLayout(); }
        public interface OnPreDrawListener { boolean onPreDraw(); }
        public interface OnWindowFocusChangeListener { void onWindowFocusChanged(boolean focus); }
        public OnGlobalLayoutListener layout; public OnPreDrawListener draw;
        public OnWindowFocusChangeListener focus;
        public void addOnGlobalLayoutListener(OnGlobalLayoutListener v){layout=v;}
        public void addOnPreDrawListener(OnPreDrawListener v){draw=v;}
        public void addOnWindowFocusChangeListener(OnWindowFocusChangeListener v){focus=v;}
        }''',
    'android/view/SurfaceView.java': 'package android.view; public class SurfaceView extends View {}',
    'android/view/TextureView.java': 'package android.view; public class TextureView extends View {}',
    'android/view/Window.java': '''package android.view;
        public class Window { public View decor; public View getDecorView(){return decor;} }''',
    'android/app/Activity.java': '''package android.app;
        public class Activity { public android.view.Window window=new android.view.Window();
        public android.view.Window getWindow(){return window;}
        public boolean focused=true; public boolean hasWindowFocus(){return focused;}
        public android.content.res.Resources getResources(){return new android.content.res.Resources();} }''',
    'cat/narezany/margyt/Margy.java': '''package cat.narezany.margyt;
        public class Margy { public static final String PREFS="test";
        public static android.content.Context context(){return null;} }''',
    'cat/narezany/margyt/Screen.java': '''package cat.narezany.margyt;
        public class Screen { public static android.app.Activity now(){return null;} }''',
    'cat/narezany/margyt/Diary.java': '''package cat.narezany.margyt;
        public class Diary { public static void note(String s){throw new AssertionError(s);} }''',
    'cat/narezany/margyt/BurnEye.java': '''package cat.narezany.margyt;
        class BurnEye { static void configurationChanged(){} static android.view.View eye;static boolean owns(android.view.View v){return v!=null && v==eye;}static boolean gone;static boolean missing(android.view.View v){return gone;}static int installs;static android.view.View install(android.view.View v){installs++;return eye;}static android.view.View installFeed(android.view.View v){return install(v);}static android.view.View installVideo(android.view.View v){return install(v);}static int inboxInstalls,hidden;static android.view.View installInbox(android.view.View v){inboxInstalls++;return install(v);}static void hide(android.view.View v){hidden++;} static void hideSoon(android.view.View v){}
        static boolean isFeedRail(android.view.View v,int depth){return true;}
        static boolean header=true;static boolean hasFeedHeader(android.view.View v){return header;} }''',
}

STUBS['cat/narezany/margyt/ProfileLayout.java']='package cat.narezany.margyt;class ProfileLayout {static boolean profile;static boolean visible(android.view.View root){return profile;}}'
STUBS['android/view/View.java']=STUBS['android/view/View.java'].replace('public boolean isAttachedToWindow(){return true;}', 'public boolean isAttachedToWindow(){return true;}public boolean hasWindowFocus(){return true;}')

from animation_doubles import STUBS as ANIMATION
STUBS.update(ANIMATION)
STUBS['cat/narezany/margyt/BurnEye.java']=STUBS['cat/narezany/margyt/BurnEye.java'].replace('static boolean header=true;', 'static boolean friendsHeader(android.view.View v){return false;}static boolean videoBackHeader(android.view.View v){return false;}static android.view.View installFriends(android.view.View v){return install(v);}static boolean header=true;')
HARNESS = r"""package cat.narezany.margyt;
public class DimHarness {
 public static void main(String[] args) {
  if(Dim.isConfigured() || Dim.isEnabled())throw new AssertionError("Dimming is off until it is switched on");
  Dim.setEnabled(true);
  if(!Dim.isConfigured() || !Dim.isEnabled())throw new AssertionError("Dimming did not switch on");
  Dim.setStrength(90);
  if(Dim.strength()!=90)throw new AssertionError("Strength was not kept");
  Dim.setEnabled(true);Dim.toggleOpacity();
  if(!Dim.isConfigured() || Dim.isEnabled())throw new AssertionError("The eye should pause dimming and keep the setting");
  Dim.toggleOpacity();
  if(!Dim.isEnabled())throw new AssertionError("The eye should resume dimming");
  Dim.setEnabled(false);
  if(Dim.isConfigured() || Dim.isEnabled())throw new AssertionError("Dimming did not switch off");
  if(Dim.isShiftEnabled() || Dim.shiftDp()!=0)throw new AssertionError("Pixel shift re-enabled");
  System.out.println("Burn protection follows its switch");
 }
}"""


@unittest.skipUnless(shutil.which('javac') and shutil.which('java'), 'JDK required')
class DimRuntimeTest(unittest.TestCase):
    def test_burn_protection_follows_its_switch(self):
        with tempfile.TemporaryDirectory() as directory:
            work = pathlib.Path(directory)
            sources = dict(STUBS)
            sources['cat/narezany/margyt/Dim.java'] = (
                ROOT / 'inject/java/cat/narezany/margyt/Dim.java').read_text(encoding='utf-8')
            for name in ('BadgeTarget', 'RecentProfiles', 'BottomChrome'):
                sources['cat/narezany/margyt/' + name + '.java'] = (
                    ROOT / ('inject/java/cat/narezany/margyt/' + name + '.java')).read_text(encoding='utf-8')
            sources['cat/narezany/margyt/DimHarness.java'] = HARNESS
            for name, content in sources.items():
                path = work / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(content, encoding='utf-8')
            compiled = subprocess.run(['javac', '-source', '8', '-target', '8',
                                      '-encoding', 'UTF-8', '-d', str(work)] +
                                      [str(work / name) for name in sources],
                                      capture_output=True, text=True)
            self.assertEqual(0, compiled.returncode, compiled.stderr)
            result = subprocess.run(['java', '-cp', str(work), 'cat.narezany.margyt.DimHarness'],
                                    capture_output=True, text=True)
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertIn('Burn protection follows its switch', result.stdout)
