"""Exercise the background lifecycle and visible launcher-art bounds."""
from pathlib import Path
import unittest
import test_settings_safety_runtime as safety

ROOT = Path(__file__).resolve().parents[1]
FRAMEWORK = ROOT / "inject/java/cat/ttcuz/framework"


class SettingsMenuRuntimeTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_snow_stops_when_hidden_paused_or_motion_disabled(self):
        sources = {
            "android/content/Context.java": "package android.content;public class Context{}",
            "android/content/res/Resources.java": "package android.content.res;public class Resources{public android.util.DisplayMetrics getDisplayMetrics(){return new android.util.DisplayMetrics();}}",
            "android/util/DisplayMetrics.java": "package android.util;public class DisplayMetrics{public float density=1;}",
            "android/graphics/Color.java": "package android.graphics;public class Color{public static final int WHITE=-1;}",
            "android/graphics/Paint.java": "package android.graphics;public class Paint{public static final int ANTI_ALIAS_FLAG=1;public int color,alpha;public Paint(int f){}public void setColor(int c){color=c;}public void setAlpha(int a){alpha=a;}}",
            "android/graphics/Canvas.java": """package android.graphics;public class Canvas{
                public java.util.List<float[]> points=new java.util.ArrayList<>();
                public void drawCircle(float x,float y,float r,Paint p){if(p.color!=-1||p.alpha<80||p.alpha>160||r<1||r>2.1f)throw new AssertionError("snow exceeds visibility budget");points.add(new float[]{x,y,r});}}""",
            "android/os/SystemClock.java": "package android.os;public class SystemClock{public static long now;public static long uptimeMillis(){return now;}}",
            "android/view/View.java": """package android.view;public class View{
                public static final int VISIBLE=0,GONE=8,IMPORTANT_FOR_ACCESSIBILITY_NO=2;
                public boolean shown=true,clickable,focusable;public int visibility,accessibility,invalidations;
                public java.util.List<Runnable> tasks=new java.util.ArrayList<>();android.content.Context context;
                public View(android.content.Context c){context=c;}
                public android.content.Context getContext(){return context;}
                public android.content.res.Resources getResources(){return new android.content.res.Resources();}
                public int getWidth(){return 400;}public int getHeight(){return 800;}
                public void setClickable(boolean b){clickable=b;}public void setFocusable(boolean b){focusable=b;}
                public void setImportantForAccessibility(int v){accessibility=v;}
                public boolean isShown(){return shown;}public int getWindowVisibility(){return visibility;}
                public void invalidate(){invalidations++;}public boolean removeCallbacks(Runnable r){return tasks.remove(r);}
                public boolean postDelayed(Runnable r,long delay){if(delay<33)throw new AssertionError("uncapped frame rate");tasks.add(r);return true;}
                protected void onDraw(android.graphics.Canvas c){}protected void onWindowVisibilityChanged(int v){}
                protected void onDetachedFromWindow(){}}""",
            "cat/ttcuz/framework/Motion.java": "package cat.ttcuz.framework;class Motion{static boolean on=true;static boolean enabled(android.content.Context c){return on;}}",
            "cat/ttcuz/framework/DevicePerformance.java": "package cat.ttcuz.framework;class DevicePerformance{static boolean limited(android.content.Context c){return false;}}",
            "cat/ttcuz/framework/SettingsSnow.java": (FRAMEWORK / "SettingsSnow.java").read_text(encoding="utf-8"),
            "cat/ttcuz/framework/Check.java": """package cat.ttcuz.framework;import android.graphics.Canvas;import android.os.SystemClock;
                public class Check{static void ck(boolean b,String m){if(!b)throw new AssertionError(m);}
                public static void run(){SettingsSnow snow=new SettingsSnow(new android.content.Context());
                ck(!snow.clickable&&!snow.focusable&&snow.accessibility==2,"decoration cannot take input or accessibility focus");
                snow.setActive(true);Canvas first=new Canvas();snow.onDraw(first);
                ck(first.points.size()>26&&first.points.size()<=64&&snow.tasks.size()==1,"visible bounded single animation loop");
                SystemClock.now=33;Canvas second=new Canvas();snow.onDraw(second);
                ck(second.points.get(0)[1]>first.points.get(0)[1]&&snow.tasks.size()==1,"falling without duplicate callbacks");
                snow.setActive(false);ck(snow.tasks.isEmpty(),"pause cancels pending frame");
                SystemClock.now=3000;Canvas paused=new Canvas();snow.onDraw(paused);ck(paused.points.get(0)[1]==second.points.get(0)[1],"pause preserves position");
                snow.setActive(true);Motion.on=false;snow.onDraw(new Canvas());ck(snow.tasks.isEmpty(),"reduced motion is static");
                Motion.on=true;snow.shown=false;snow.onDraw(new Canvas());ck(snow.tasks.isEmpty(),"hidden view does not animate");
                snow.shown=true;snow.visibility=8;snow.onWindowVisibilityChanged(8);snow.onDraw(new Canvas());ck(snow.tasks.isEmpty(),"hidden window stops");
                snow.visibility=0;snow.onWindowVisibilityChanged(0);snow.onDraw(new Canvas());ck(snow.tasks.size()==1,"visible window restarts");
                snow.onDetachedFromWindow();ck(snow.tasks.isEmpty(),"detached view cancels callbacks");}}
                """,
            "Test.java": "public class Test{public static void main(String[] args){cat.ttcuz.framework.Check.run();}}",
        }
        self.run_java(sources)

    def test_padded_and_full_bleed_icons_have_equal_visible_bounds(self):
        sources = {
            "android/graphics/Color.java": "package android.graphics;public class Color{public static final int WHITE=-1;public static int alpha(int c){return c>>>24;}}",
            "android/graphics/Bitmap.java": """package android.graphics;public class Bitmap{
                public java.awt.image.BufferedImage image;public enum Config{ARGB_8888}
                public Bitmap(int w,int h){image=new java.awt.image.BufferedImage(w,h,2);}
                public static Bitmap createBitmap(int w,int h,Config c){return new Bitmap(w,h);}
                public int getWidth(){return image.getWidth();}public int getHeight(){return image.getHeight();}
                public void getPixels(int[] p,int offset,int stride,int x,int y,int w,int h){image.getRGB(x,y,w,h,p,offset,stride);}}
                """,
            "android/graphics/Paint.java": "package android.graphics;public class Paint{public static final int ANTI_ALIAS_FLAG=1,FILTER_BITMAP_FLAG=2;public int color;public boolean srcIn;public Paint(int f){}public void setColor(int c){color=c;}public void setXfermode(PorterDuffXfermode x){srcIn=true;}}",
            "android/graphics/PorterDuff.java": "package android.graphics;public class PorterDuff{public enum Mode{SRC_IN}}",
            "android/graphics/PorterDuffXfermode.java": "package android.graphics;public class PorterDuffXfermode{public PorterDuffXfermode(PorterDuff.Mode m){}}",
            "android/graphics/Rect.java": "package android.graphics;public class Rect{public int left,top,right,bottom;public Rect(int a,int b,int c,int d){left=a;top=b;right=c;bottom=d;}public int width(){return right-left;}public int height(){return bottom-top;}}",
            "android/graphics/RectF.java": "package android.graphics;public class RectF{public float left,top,right,bottom;public RectF(float a,float b,float c,float d){left=a;top=b;right=c;bottom=d;}}",
            "android/graphics/Canvas.java": """package android.graphics;public class Canvas{
                java.awt.Graphics2D g;public Canvas(Bitmap b){g=b.image.createGraphics();}
                public void drawRoundRect(RectF r,float rx,float ry,Paint p){g.setColor(new java.awt.Color(p.color,true));g.fill(new java.awt.geom.RoundRectangle2D.Float(r.left,r.top,r.right-r.left,r.bottom-r.top,rx*2,ry*2));}
                public void drawBitmap(Bitmap b,Rect from,RectF to,Paint p){g.setComposite(java.awt.AlphaComposite.SrcIn);g.drawImage(b.image,Math.round(to.left),Math.round(to.top),Math.round(to.right),Math.round(to.bottom),from.left,from.top,from.right,from.bottom,null);}}
                """,
            "cat/ttcuz/framework/Accent.java": "package cat.ttcuz.framework;class Accent{static int color=0xff4285ff;static int colour(){return color;}}",
            "cat/ttcuz/framework/Launcher.java": """package cat.ttcuz.framework;import android.graphics.Bitmap;class Launcher{
                static final String ACCENT="accent";static int reads;static java.util.Map<String,Bitmap> images=new java.util.HashMap<>();
                static Bitmap preview(String id){reads++;return images.get(id);}}
                """,
            "cat/ttcuz/framework/LauncherPreview.java": (FRAMEWORK / "LauncherPreview.java").read_text(encoding="utf-8"),
            "cat/ttcuz/framework/Check.java": """package cat.ttcuz.framework;import android.graphics.Bitmap;public class Check{
                static void ck(boolean b,String m){if(!b)throw new AssertionError(m);}
                static Bitmap art(int size,int inset){Bitmap b=new Bitmap(size,size);for(int y=inset;y<size-inset;y++)for(int x=inset;x<size-inset;x++)b.image.setRGB(x,y,0xff80a0b0);return b;}
                static String bounds(Bitmap b){int left=b.getWidth(),top=b.getHeight(),right=-1,bottom=-1;for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++)if((b.image.getRGB(x,y)>>>24)>16){left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x);bottom=Math.max(bottom,y);}return left+","+top+","+right+","+bottom;}
                public static void run(){Bitmap padded=art(192,28),full=art(192,0);Launcher.images.put("padded",padded);Launcher.images.put("full",full);Launcher.images.put("accent",full);
                Bitmap a=LauncherPreview.get("padded"),b=LauncherPreview.get("full");ck(bounds(a).equals(bounds(b)),"visible bounds match despite source margins");
                ck(bounds(a).equals("28,28,163,163"),"same footprint as the first icons");ck(b.image.getRGB(28,28)==0,"preview corners are rounded");
                int reads=Launcher.reads;ck(LauncherPreview.get("full")==b&&Launcher.reads==reads,"normalized preview cached");
                Bitmap old=LauncherPreview.get("accent");Accent.color=0xffff8000;ck(LauncherPreview.get("accent")!=old,"accent change invalidates only accent preview");
                ck(bounds(padded).equals("28,28,163,163")&&bounds(full).equals("0,0,191,191"),"launcher artwork remains untouched");}}
                """,
            "Test.java": "public class Test{public static void main(String[] args){cat.ttcuz.framework.Check.run();}}",
        }
        self.run_java(sources)


if __name__ == "__main__":
    unittest.main()
