"""Execute the loading indicator, the pill to Sunny contour, the drawn-in check and the geometry endpoints."""
import pathlib,shutil,subprocess,tempfile,unittest
from test_screen_runtime import STUBS as BASE
ROOT=pathlib.Path(__file__).resolve().parents[1]
STUBS=dict(BASE)
STUBS.pop('cat/ttcuz/framework/DownloadMark.java')
STUBS['android/view/View.java']=STUBS['android/view/View.java'].replace('public static final int VISIBLE=0;', 'public static final int VISIBLE=0,INVISIBLE=4,IMPORTANT_FOR_ACCESSIBILITY_NO=2,ACCESSIBILITY_LIVE_REGION_POLITE=1;public void setVisibility(int v){}public float alpha=1;public int width=260,height=50;public void setWillNotDraw(boolean b){}public void setContentDescription(String s){}public void setAccessibilityLiveRegion(int n){}public void setImportantForAccessibility(int n){}public int getTop(){return 32;}').replace('public void setAlpha(float v){}','public void setAlpha(float v){alpha=v;}').replace('public int getWidth(){return 300;}public int getHeight(){return 50;}', 'public int getWidth(){return width;}public int getHeight(){return height;}')
STUBS['android/graphics/Paint.java']='''package android.graphics;public class Paint {public static int ANTI_ALIAS_FLAG=1;public enum Style {FILL,STROKE}public enum Cap {ROUND}public enum Join {ROUND}public enum Align {CENTER}public static class FontMetrics {public float ascent=-11,descent=3;}public int colour;public float thickness;public Paint(int f){}public void setColor(int c){colour=c;}public void setStyle(Style s){}public void setStrokeCap(Cap c){}public void setStrokeJoin(Join j){}public void setStrokeWidth(float n){thickness=n;}public void setTextSize(float n){}public void setTextAlign(Align a){}public void setTypeface(Typeface t){}public FontMetrics getFontMetrics(){return new FontMetrics();}public float measureText(String s){return s.length()*7;}}'''
STUBS['android/graphics/Path.java']="""package android.graphics;public class Path {public java.util.List<float[]> points=new java.util.ArrayList<>();public void reset(){points.clear();}public void moveTo(float x,float y){points.add(new float[]{x,y});}public void lineTo(float x,float y){points.add(new float[]{x,y});}public void close(){}}"""
STUBS['android/graphics/PathMeasure.java']="""package android.graphics;public class PathMeasure {java.util.List<float[]> p;public void setPath(Path path,boolean closed){p=path.points;}
public float getLength(){float l=0;for(int i=1;i<p.size();i++)l+=(float)Math.hypot(p.get(i)[0]-p.get(i-1)[0],p.get(i)[1]-p.get(i-1)[1]);return l;}
public boolean getSegment(float from,float to,Path out,boolean move){float run=0;out.moveTo(p.get(0)[0],p.get(0)[1]);for(int i=1;i<p.size();i++){float[] s=p.get(i-1),e=p.get(i);float len=(float)Math.hypot(e[0]-s[0],e[1]-s[1]);if(run+len<=to+1e-3f){out.lineTo(e[0],e[1]);run+=len;}else{float f=(to-run)/len;out.lineTo(s[0]+(e[0]-s[0])*f,s[1]+(e[1]-s[1])*f);break;}}return true;}}"""
STUBS['android/graphics/Canvas.java']="""package android.graphics;public class Canvas {public java.util.List<java.util.List<float[]>> paths=new java.util.ArrayList<>();public java.util.List<Integer> colours=new java.util.ArrayList<>();public java.util.List<float[]> circles=new java.util.ArrayList<>();public java.util.List<Integer> circleColours=new java.util.ArrayList<>();public float turned;
public void drawPath(Path p,Paint paint){paths.add(new java.util.ArrayList<>(p.points));colours.add(paint.colour);}
public void drawCircle(float x,float y,float r,Paint paint){circles.add(new float[]{x,y,r});circleColours.add(paint.colour);}
public boolean clipPath(Path p){return true;}public int save(){return 1;}public void restoreToCount(int n){}public void translate(float x,float y){}public void rotate(float degrees){turned=degrees;}public java.util.List<String> texts=new java.util.ArrayList<>();public void scale(float x,float y){}public void drawText(String s,float x,float y,Paint p){texts.add(s);}}"""
STUBS['android/view/ViewGroup.java']=STUBS['android/view/ViewGroup.java'].replace('public ArrayList<View> children','protected void dispatchDraw(android.graphics.Canvas c){}public ArrayList<View> children',1)
STUBS['android/os/SystemClock.java']='package android.os;public class SystemClock {public static long now=1000;public static long uptimeMillis(){return now;}}'
STUBS['android/graphics/Typeface.java']='package android.graphics;public class Typeface {public static final int NORMAL=0;public static final Typeface DEFAULT=new Typeface();public static Typeface create(Typeface f,int w,boolean i){return f;}public static Typeface create(String n,int s){return DEFAULT;}}'
STUBS['android/os/Build.java']='package android.os;public class Build {public static class VERSION {public static int SDK_INT=30;}}'
STUBS['cat/ttcuz/framework/Text.java']='package cat.ttcuz.framework;class Text {static String t(String s){return s;}}'
STUBS['cat/ttcuz/framework/Motion.java']='package cat.ttcuz.framework;class Motion {static final android.view.animation.PathInterpolator OUT=new android.view.animation.PathInterpolator(0,0,0,0),IN=OUT,BOTH=OUT;static final long SHORT=180L,MEDIUM=300L,LONG=340L;static boolean animate=true;static boolean enabled(android.content.Context c){return animate;}}'
STUBS['android/view/View.java']=STUBS['android/view/View.java'].replace('public static final int VISIBLE=0;', 'public static final int VISIBLE=0,INVISIBLE=4;')
STUBS['android/view/View.java']=STUBS['android/view/View.java'].replace('public void setWillNotDraw', 'public int invalidations;public boolean isShown(){return true;}public void postInvalidateOnAnimation(){invalidations++;}public int getPaddingLeft(){return 10;}public int getPaddingTop(){return 8;}public void setWillNotDraw')
HARNESS='''package cat.ttcuz.framework;import android.graphics.*;import android.widget.*;
public class MorphHarness {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}static void near(float a,float b,String s){check(Math.abs(a-b)<0.01,s);}
 public static void main(String[] args){
  // the seven MaterialShapes, as in the lab: 240 rays each, Sunny has eight tips on a 0.5 radius
  float[][] shapes=LabShapes.get();check(shapes.length==7,"seven shapes");
  for(float[] shape:shapes){check(shape.length==LabShapes.N,"240 rays");for(float r:shape)check(Float.isFinite(r)&&r>0.2f&&r<0.65f,"radius in range");}
  float low=9,high=0;for(float r:shapes[LabShapes.SUNNY]){low=Math.min(low,r);high=Math.max(high,r);}near(high,0.5f,"sunny tip");check(low>0.40f&&low<0.44f,"sunny valley");
  check(Math.abs(LabShapes.spring(0.65)-1)<0.01,"spring settles inside one step");check(LabShapes.spring(0)==0,"spring starts at rest");
  Path shape=new Path();double turn0=LabShapes.blob(shape,0,10);check(shape.points.size()==LabShapes.N+1,"closed outline");near((float)turn0,0,"no turn at the start");
  check(LabShapes.blob(shape,650,10)>1.4,"a step turns the shape by about a quarter");
  // loading: the square island, then the wavy ring with the percentage written inside it
  DownloadMark surface=new DownloadMark(new android.content.Context(),0xff1c2c24);surface.progress(35);Canvas loading=null;
  for(int frame=0;frame<14;frame++){android.os.SystemClock.now+=100;loading=new Canvas();surface.onDraw(loading);}
  check(loading.paths.size()==3 && loading.paths.get(0).size()==LabShapes.N,"island contour, track and wavy arc");
  check(loading.circles.size()==0,"no round container and no flowing shape");
  check(loading.texts.size()==1 && loading.texts.get(0).equals("35%"),"percentage written inside the ring");
  check(loading.colours.get(1)==(0x40000000|(Accent.colour()&0xFFFFFF)) && loading.colours.get(2)==Accent.colour(),"track at a quarter of the accent, arc in the accent");
  float[] begin=loading.paths.get(2).get(0);check(Math.abs(Math.hypot(begin[0],begin[1])-36)<=2.3,"ring radius 36 with a 2.2 wave");
  check(surface.invalidations==14,"frame loop while loading");
  DownloadMark spin=new DownloadMark(new android.content.Context(),0xff1c2c24);Canvas unknown=new Canvas();spin.onDraw(unknown);check(unknown.texts.isEmpty() && unknown.paths.size()==3,"unknown progress: no percentage, the arc keeps turning");
  // completion: the same surface, the ring fades, the island becomes Sunny and the check draws itself in
  surface.begin(260);
  Canvas start=new Canvas();surface.onDraw(start);check(start.paths.size()==3 && start.texts.size()==1,"nothing swapped at the first frame of completion");
  surface.morph(0.2f);Canvas early=new Canvas();surface.onDraw(early);check(early.paths.size()>=3,"ring still fading out");
  surface.morph(1);surface.width=surface.height=56;Canvas done=new Canvas();surface.onDraw(done);
  check(done.colours.get(0)==Accent.colour(),"completion uses the accent");
  check(done.texts.isEmpty() && done.paths.size()==2,"ring gone, contour and check remain");
  java.util.List<float[]> mark=done.paths.get(1);check(mark.size()==3,"check has three points");near(mark.get(0)[0],16.5f,"check start");near(mark.get(1)[1],34.9f,"check joint");near(mark.get(2)[0],40.65f,"check end");near(mark.get(2)[1],18.8f,"check end height");
  surface.morph(0.7f);Canvas half=new Canvas();surface.onDraw(half);java.util.List<float[]> partial=half.paths.get(half.paths.size()-1);near(partial.get(0)[0],16.5f,"drawn part starts at the same point");check(partial.size()<=3 && !(partial.get(partial.size()-1)[0]>=40.65f),"check is only partly drawn midway");
  int peaks=0;java.util.List<float[]> sun=done.paths.get(0);for(int i=0;i<sun.size();i++){float[] a=sun.get((i+sun.size()-1)%sun.size()),b=sun.get(i),c=sun.get((i+1)%sun.size());double ra=Math.hypot(a[0]-28,a[1]-28),rb=Math.hypot(b[0]-28,b[1]-28),rc=Math.hypot(c[0]-28,c[1]-28);if(rb>ra && rb>=rc)peaks++;}check(peaks==8,"Sunny has eight tips");
  for(int f=0;f<=100;f++){float t=f/100f,w=DownloadMorph.mix(260,56,t),h=56;for(int i=0;i<LabShapes.N;i++){float angle=(float)(i*Math.PI*2/LabShapes.N),dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);float ray=DownloadMorph.mix(DownloadMorph.roundedDirection(dx,dy,260,56,28),shapes[LabShapes.SUNNY][i]*56,t);check(Float.isFinite(ray)&&ray>0,"all intermediate contours finite");check(Math.abs(dx*ray)<=w/2+0.1 && Math.abs(dy*ray)<=h/2+0.1,"contour fits window throughout transform");}}
  Motion.animate=false;DownloadMark quiet=new DownloadMark(new android.content.Context(),0xff1c2c24);Canvas still=new Canvas();quiet.onDraw(still);check(quiet.invalidations==0,"no frame loop with animations disabled");
  check(DownloadMorph.colour(0xff24262b,0xffa4c9ff,0)==0xff24262b && DownloadMorph.colour(0xff24262b,0xffa4c9ff,1)==0xffa4c9ff,"colour endpoints exact");
 }
}'''
class DownloadMorphRuntimeTest(unittest.TestCase):
 @unittest.skipUnless(shutil.which('javac') and shutil.which('java'),'JDK required')
 def test_surface_and_contour(self):
  with tempfile.TemporaryDirectory() as directory:
   work=pathlib.Path(directory);sources=dict(STUBS)
   for name in ('DownloadMark','DownloadMorph','LabShapes'):sources['cat/ttcuz/framework/'+name+'.java']=(ROOT/('inject/java/cat/ttcuz/framework/'+name+'.java')).read_text(encoding='utf-8')
   sources['cat/ttcuz/framework/MorphHarness.java']=HARNESS
   for name,content in sources.items():
    path=work/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(content,encoding='utf-8')
   result=subprocess.run(['javac','-source','8','-target','8','-encoding','UTF-8','-d',str(work)]+[str(work/n) for n in sources],capture_output=True,text=True);self.assertEqual(0,result.returncode,result.stderr)
   result=subprocess.run(['java','-cp',str(work),'cat.ttcuz.framework.MorphHarness'],capture_output=True,text=True);self.assertEqual(0,result.returncode,result.stderr)
