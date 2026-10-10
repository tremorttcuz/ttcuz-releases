import unittest,tempfile,shutil
from pathlib import Path
import test_settings_safety_runtime as safety
from margyt import dexpatch
ROOT=Path(__file__).resolve().parents[1]
class StableAccentTest(unittest.TestCase):
 def test_native_icon_binding_and_text_recycling_hooks(self):
  source='.class public Lcom/bytedance/tux/icon/TuxIconView;\n.super Landroid/widget/ImageView;\n.method public constructor <init>(Landroid/content/Context;)V\n    .registers 2\n    return-void\n.end method\n.method public final setTuxIcon(LX/0GtW;)V\n    .registers 2\n    return-void\n.end method\n'
  with tempfile.TemporaryDirectory() as folder:
   file=Path(folder)/'Icon.smali';file.write_text(source,encoding='utf-8')
   counts=dexpatch.rewrite_anchored(folder);self.assertEqual(2,counts['native icon bind contrast'])
   self.assertEqual({},dexpatch.rewrite_anchored(folder))
   for owner in ('Landroid/widget/TextView;','Lcom/bytedance/tux/input/TuxTextView;'):
    file.write_text('.class public LX/Row;\n.super Ljava/lang/Object;\n.method public bind()V\n    .registers 2\n    invoke-virtual {v0, v1}, '+owner+'->setText(Ljava/lang/CharSequence;)V\n    return-void\n.end method\n',encoding='utf-8')
    dexpatch.rewrite_accent(folder)
    self.assertIn('Accent;->setText(Landroid/widget/TextView;Ljava/lang/CharSequence;)V',file.read_text())
   file.write_text('.class public LX/StreakFactory;\n.super Ljava/lang/Object;\n.method public art()V\n    .registers 1\n    const-string v0, "streak_icon_inline_"\n    const v0, -0x1d3ab\n    return-void\n.end method\n',encoding='utf-8')
   self.assertEqual({},dexpatch.rewrite_accent(folder));self.assertNotIn('Accent;->accent',file.read_text())
 def test_native_streak_factory_is_scoped_and_idempotent(self):
  source='.class public LX/06pf;\n.super Ljava/lang/Object;\n.method public icon()Landroid/graphics/drawable/Drawable;\n    .registers 2\n    const-string v0, "streak_icon_inline_"\n    const/4 v1, 0x0\n    return-object v1\n.end method\n.method public other()Ljava/lang/Object;\n    .registers 1\n    const/4 v0, 0x0\n    return-object v0\n.end method\n'
  with tempfile.TemporaryDirectory() as folder:
   file=Path(folder)/'06pf.3.smali';file.write_text(source,encoding='utf-8')
   counts=dexpatch.rewrite_anchored(folder)
   self.assertEqual(1,counts['native streak artwork'])
   changed=file.read_text();self.assertEqual(1,changed.count('Accent;->keepNativeArtwork'))
   self.assertIn('    return-object v0',changed)
   self.assertEqual({},dexpatch.rewrite_anchored(folder))
   self.assertTrue(dexpatch.carries_an_anchor(b'streak_icon_inline_'))

 def test_bitmap_negative_cache_and_native_original_art(self):
  body=safety.method('Accent.java','    private static Drawable movedBitmap')
  keep=safety.method('Accent.java','    public static Drawable keepNativeArtwork')
  guard=safety.method('Accent.java','    static boolean nativeArtworkView')
  sources={
   'android/view/View.java':'package android.view;public class View {public View parent;public View getParent(){return parent;}}',
   'android/widget/TextView.java':'package android.widget;public class TextView extends android.view.View {}',
   'com/ss/android/ugc/aweme/im/streak/ArtworkView.java':'package com.ss.android.ugc.aweme.im.streak;public class ArtworkView extends android.view.View {}',
   'android/content/res/Resources.java':'package android.content.res;public class Resources {}',
   'android/content/Context.java':'package android.content;public class Context {public android.content.res.Resources getResources(){return null;}}',
   'android/graphics/drawable/Drawable.java':'package android.graphics.drawable;public class Drawable {}',
   'android/graphics/Bitmap.java':'package android.graphics;public class Bitmap {public int reads;public int[] pixels={7,7,7,7};public int getWidth(){return 2;}public int getHeight(){return 2;}public boolean isRecycled(){return false;}public void getPixels(int[] out,int o,int stride,int x,int y,int w,int h){reads++;System.arraycopy(pixels,0,out,0,4);}public void setPixels(int[] in,int o,int stride,int x,int y,int w,int h){pixels=in;}public static class Config {public static Config ARGB_8888=new Config();}public static Bitmap createBitmap(int w,int h,Config c){return new Bitmap();}}',
   'android/graphics/drawable/BitmapDrawable.java':'package android.graphics.drawable;public class BitmapDrawable extends Drawable {android.graphics.Bitmap b;public BitmapDrawable(android.content.res.Resources r,android.graphics.Bitmap b){this.b=b;}public android.graphics.Bitmap getBitmap(){return b;}}',
   'Test.java':"""import android.view.*;import android.widget.*;import android.content.*;import android.content.res.*;import android.graphics.*;import android.graphics.drawable.*;public class Test {
static final int TIKTOK=0xff000001,DISK_MIN=4096;static int target=2;static int colour(){return target;}static class Margy{static Context context(){return null;}}static class Palette{static int map(int c,int from,int to){return c==from?to:c;}}static class RecolourCache{static byte[] NONE=new byte[0];static String hash(int[] p,int w,int h){return "";}static byte[] get(String k){return null;}static Bitmap decode(byte[] b){return null;}static byte[] png(Bitmap b){return null;}static void put(String k,byte[] b){}}
static java.util.Map<Bitmap,Bitmap> recolouredBitmaps=new java.util.WeakHashMap<>();static java.util.Map<Bitmap,Boolean> plainBitmaps=new java.util.WeakHashMap<>();static java.util.Map<Drawable,Boolean> nativeArtwork=new java.util.WeakHashMap<>();static class Owned {Drawable original;Owned(Drawable d){original=d;}}static java.util.Map<Drawable,Owned> owned=new java.util.WeakHashMap<>();
"""+body+keep+guard+"""
static void check(boolean b,String s){if(!b)throw new AssertionError(s);}public static void main(String[] args){
Bitmap bitmap=new Bitmap();BitmapDrawable drawable=new BitmapDrawable(null,bitmap);for(int i=0;i<100;i++)check(movedBitmap(drawable)==drawable,"neutral art untouched");check(bitmap.reads==1,"neutral bitmap scanned once");
plainBitmaps.clear();target=3;movedBitmap(drawable);check(bitmap.reads==2,"palette invalidation permits reevaluation");
Bitmap red=new Bitmap();red.pixels=new int[]{TIKTOK,TIKTOK,TIKTOK,TIKTOK};BitmapDrawable redDrawable=new BitmapDrawable(null,red);Drawable result=movedBitmap(redDrawable);check(result!=redDrawable,"accent bitmap still maps");movedBitmap(redDrawable);check(red.reads==1,"positive bitmap cache preserved");
Drawable nativeIcon=new Drawable(),changed=new Drawable();owned.put(changed,new Owned(nativeIcon));check(keepNativeArtwork(changed)==nativeIcon && nativeArtwork.containsKey(nativeIcon),"factory restores native source artwork");check(keepNativeArtwork(null)==null,"null native drawable is safe");
View parent=new com.ss.android.ugc.aweme.im.streak.ArtworkView(),icon=new View();icon.parent=parent;check(nativeArtworkView(icon),"artwork subtree protected");TextView number=new TextView();number.parent=parent;check(!nativeArtworkView(number),"number can follow accent");check(!nativeArtworkView(new View()),"unrelated controls unchanged");
}}"""
  }
  safety.SafetyRuntimeTest().run_java(sources)
