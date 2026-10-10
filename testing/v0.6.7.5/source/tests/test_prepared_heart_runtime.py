"""Run the real heart cache with a native-like icon factory and count work."""
import unittest
from pathlib import Path
import test_settings_safety_runtime as safety
ROOT=Path(__file__).resolve().parents[1]

class PreparedHeartTest(unittest.TestCase):
 def test_prepared_pixels_reused_without_live_filters(self):
  sources={
   'android/util/DisplayMetrics.java':'package android.util;public class DisplayMetrics {public int densityDpi=320;}',
   'android/content/res/Resources.java':'package android.content.res;public class Resources {public android.util.DisplayMetrics getDisplayMetrics(){return new android.util.DisplayMetrics();}}',
   'android/content/Context.java':'package android.content;public class Context {public android.content.res.Resources getResources(){return new android.content.res.Resources();}}',
   'android/graphics/PorterDuff.java':'package android.graphics;public class PorterDuff {public enum Mode {SRC_IN}}',
   'android/graphics/Bitmap.java':'package android.graphics;public class Bitmap {public int size,density;public enum Config {ARGB_8888}public static Bitmap createBitmap(int w,int h,Config c){Bitmap b=new Bitmap();b.size=w;return b;}public void setDensity(int d){density=d;}public int getWidth(){return size;}public int getHeight(){return size;}}',
   'android/graphics/Canvas.java':'package android.graphics;public class Canvas {public Canvas(Bitmap b){}}',
   'android/graphics/drawable/Drawable.java':'package android.graphics.drawable;public class Drawable {public void setBounds(int a,int b,int c,int d){}public void draw(android.graphics.Canvas c){}public int getIntrinsicWidth(){return 48;}public int getIntrinsicHeight(){return 48;}}',
   'android/graphics/drawable/BitmapDrawable.java':'package android.graphics.drawable;public class BitmapDrawable extends Drawable {public android.graphics.Bitmap bitmap;public BitmapDrawable(android.content.res.Resources r,android.graphics.Bitmap b){bitmap=b;}}',
   'android/view/View.java':'package android.view;public class View {public void setTag(int k,Object v){}public boolean isAttachedToWindow(){return true;}public boolean post(Runnable r){r.run();return true;}public android.content.res.Resources getResources(){return new android.content.res.Resources();}}',
   'android/widget/ImageView.java':'package android.widget;public class ImageView extends android.view.View {public android.graphics.drawable.Drawable image=new android.graphics.drawable.Drawable();public int filters;public void setImageDrawable(android.graphics.drawable.Drawable d){image=d;}public android.graphics.drawable.Drawable getDrawable(){return image;}public void clearColorFilter(){}public void setColorFilter(int c,android.graphics.PorterDuff.Mode m){filters++;}}',
   'com/bytedance/tux/icon/TuxIconView.java':'package com.bytedance.tux.icon;public class TuxIconView extends android.widget.ImageView {public int icons,tints;public void setIconRes(int r){icons++;}public void setTintColor(int c){tints++;}}',
   'com/bytedance/tux/drawable/TuxIconDrawable.java':'package com.bytedance.tux.drawable;public class TuxIconDrawable extends android.graphics.drawable.Drawable {public static int draws,creates;public TuxIconDrawable(android.content.Context c,int r){creates++;}public void LJ(int size){}public void LJFF(int c){}public void draw(android.graphics.Canvas c){draws++;}}',
   'cat/narezany/margyt/Margy.java':'package cat.narezany.margyt;class Margy {static android.content.Context context(){return new android.content.Context();}}',
   'cat/narezany/margyt/LikeColors.java':'package cat.narezany.margyt;class LikeColors {static int value=0xff00ffff;static int colour(){return value;}}',
   'cat/narezany/margyt/Diary.java':'package cat.narezany.margyt;class Diary {static void note(String s){throw new AssertionError(s);}}',
   'cat/narezany/margyt/Heart.java':(ROOT/'inject/java/cat/narezany/margyt/Heart.java').read_text(encoding='utf-8'),
   'Test.java':'public class Test {public static void main(String[] args){cat.narezany.margyt.HeartTest.main(args);}}',
   'cat/narezany/margyt/HeartTest.java':'''package cat.narezany.margyt;import com.bytedance.tux.icon.TuxIconView;import com.bytedance.tux.drawable.TuxIconDrawable;import android.graphics.drawable.BitmapDrawable;public class HeartTest {static void check(boolean b,String s){if(!b)throw new AssertionError(s);}public static void main(String[] args){
Heart.prewarm();Heart.prewarm();check(TuxIconDrawable.draws==1,"prepare once");android.graphics.Bitmap first=null;
for(int i=0;i<100;i++){TuxIconView v=new TuxIconView();Heart.setIconRes(v,0x7f0106a8);Heart.setTintColor(v,123);check(v.icons==0 && v.tints==0 && v.filters==0,"tap only reuses prepared pixels");BitmapDrawable d=(BitmapDrawable)v.image;if(first==null)first=d.bitmap;check(first==d.bitmap,"all taps share pixels");check(d.getIntrinsicWidth()==48 && d.getIntrinsicHeight()==48,"native size preserved");check(d.bitmap.density==320,"native density preserved");}
check(TuxIconDrawable.draws==1,"no raster on taps");LikeColors.value=0xff123456;Heart.prewarm();check(TuxIconDrawable.draws==2,"new colour prepared once");
TuxIconView other=new TuxIconView();Heart.setIconRes(other,42);Heart.setTintColor(other,123);check(other.icons==1 && other.tints==1 && other.filters==1,"unknown icon safe fallback skips repeated tint");
}}'''
  }
  safety.SafetyRuntimeTest().run_java(sources)
