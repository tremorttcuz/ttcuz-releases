"""Execute the real badge span across different nickname text sizes."""
import pathlib,shutil,subprocess,tempfile,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
class BadgePlacementRuntimeTest(unittest.TestCase):
 @unittest.skipUnless(shutil.which('javac') and shutil.which('java'),'JDK required')
 def test_relative_placement_and_live_revision(self):
  code=(ROOT/'inject/java/cat/narezany/margyt/Badge.java').read_text(encoding='utf-8')
  start=code.index('    private static final class MovableBadgeSpan');end=code.index('    /** The badge at a size',start)
  section=code[start:end].replace('private static final class','static final class')
  sources={
   'cat/narezany/margyt/Badge.java':'package cat.narezany.margyt;import android.graphics.drawable.Drawable;import android.widget.TextView;import android.text.style.ImageSpan;class Badge {'+section+'}',
   'cat/narezany/margyt/ProfileStyle.java':'package cat.narezany.margyt;class ProfileStyle {static int x=12,y=6,rev=1;static int revision(){return rev;}static int offsetX(char c){return x;}static int offsetY(char c){return y;}static int crownOffsetX(char c){return x;}static int crownOffsetY(char c){return y;}}',
   'android/widget/TextView.java':'package android.widget;public class TextView {public float size;public TextView(float s){size=s;}public float getTextSize(){return size;}public void requestLayout(){}}',
   'android/graphics/Paint.java':'package android.graphics;public class Paint {public FontMetricsInt getFontMetricsInt(){return new FontMetricsInt();}public static class FontMetricsInt {public int ascent=-20,top=-20,descent=4,bottom=4;}}',
   'android/graphics/Canvas.java':'package android.graphics;public class Canvas {public float x,y;public void save(){}public void restore(){}public void translate(float xx,float yy){x=xx;y=yy;}}',
   'android/graphics/drawable/Drawable.java':'package android.graphics.drawable;public class Drawable {public static class Bounds{public int right=20,bottom=20;}public Bounds getBounds(){return new Bounds();}public void draw(android.graphics.Canvas c){}}',
   'android/text/style/ImageSpan.java':'package android.text.style;import android.graphics.*;public class ImageSpan {public static final int ALIGN_BOTTOM=1;android.graphics.drawable.Drawable image;public ImageSpan(android.graphics.drawable.Drawable d,int a){image=d;}public android.graphics.drawable.Drawable getDrawable(){return image;}public int getSize(Paint p,CharSequence t,int s,int e,Paint.FontMetricsInt fm){return 20;}public void draw(Canvas c,CharSequence t,int s,int e,float x,int top,int y,int bottom,Paint p){c.x=x;c.y=y;}}',
   'cat/narezany/margyt/PlacementHarness.java':'''package cat.narezany.margyt;import android.graphics.*;import android.widget.*;class PlacementHarness {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    public static void main(String[] args){
      float[] sizes={12,24,48};for(float size:sizes){
       ProfileStyle.x=12;ProfileStyle.y=6;ProfileStyle.rev=1;
       TextView view=new TextView(size);
       Badge.MovableBadgeSpan span=new Badge.MovableBadgeSpan(new android.graphics.drawable.Drawable(),view,1,false);
       Canvas canvas=new Canvas();Paint paint=new Paint();span.draw(canvas,"x",0,1,100,0,20,24,paint);
       check(canvas.x==100+Math.round(12*size/24)&&canvas.y==2+Math.round(6*size/24),"same placement relative to name size");
       ProfileStyle.x=24;ProfileStyle.y=-12;ProfileStyle.rev++;
       int width=span.getSize(paint,"x",0,1,new Paint.FontMetricsInt());span.draw(canvas,"x",0,1,100,0,20,24,paint);
       check(canvas.x==100+Math.round(24*size/24)&&canvas.y==2+Math.round(-12*size/24),"existing span follows new placement");
       check(width==20+Math.round(24*size/24),"layout reserves positive offset");
       span.setOffset(-6,8,1);span.draw(canvas,"x",0,1,100,0,20,24,paint);check(canvas.x==100+Math.round(-6*size/24),"drag shares same reference units");
       view.size=size*2;span.draw(canvas,"x",0,1,100,0,20,24,paint);check(canvas.x==100+Math.round(24*view.size/24),"existing span follows text-size changes");
      }
    }}'''
  }
  with tempfile.TemporaryDirectory() as d:
   work=pathlib.Path(d)
   for name,text in sources.items():p=work/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf-8')
   built=subprocess.run(['javac','-source','8','-target','8','-encoding','UTF-8','-d',d]+[str(work/name) for name in sources],capture_output=True,text=True);self.assertEqual(0,built.returncode,built.stderr)
   run=subprocess.run(['java','-cp',d,'cat.narezany.margyt.PlacementHarness'],capture_output=True,text=True);self.assertEqual(0,run.returncode,run.stderr)
