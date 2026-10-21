"""Exercise the production fade against theme, width, RTL and lifecycle changes."""
import pathlib
import unittest
import test_settings_safety_runtime as safety

ROOT = pathlib.Path(__file__).resolve().parents[1]


class InboxFilterTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_surface_rtl_caching_and_detach(self):
        self.run_java({
            'android/graphics/Shader.java': 'package android.graphics;public class Shader{public enum TileMode{CLAMP}}',
            'android/graphics/Paint.java': 'package android.graphics;public class Paint{Shader shader;public Shader getShader(){return shader;}public void setShader(Shader s){shader=s;}}',
            'android/graphics/LinearGradient.java': 'package android.graphics;public class LinearGradient extends Shader{public static int made;public int[] colors;public float start,end;public LinearGradient(float a,float b,float c,float d,int[] colors,float[] positions,TileMode mode){made++;start=a;end=c;this.colors=colors;}}',
            'android/graphics/drawable/ColorDrawable.java': 'package android.graphics.drawable;public class ColorDrawable{int color;public ColorDrawable(int c){color=c;}public int getColor(){return color;}}',
            'android/view/ViewTreeObserver.java': 'package android.view;public class ViewTreeObserver{public interface OnPreDrawListener{boolean onPreDraw();}public OnPreDrawListener listener;public boolean isAlive(){return true;}public void addOnPreDrawListener(OnPreDrawListener l){listener=l;}public void removeOnPreDrawListener(OnPreDrawListener l){if(listener==l)listener=null;}}',
            'android/view/View.java': '''package android.view;public class View{public static final int LAYOUT_DIRECTION_RTL=1;public Object tag,background;public View parent;public int width=50,rtl;public OnAttachStateChangeListener attach;public ViewTreeObserver observer=new ViewTreeObserver();public interface OnAttachStateChangeListener{void onViewAttachedToWindow(View v);void onViewDetachedFromWindow(View v);}public Object getTag(int k){return tag;}public void setTag(int k,Object v){tag=v;}public void addOnAttachStateChangeListener(OnAttachStateChangeListener l){attach=l;}public boolean isAttachedToWindow(){return true;}public ViewTreeObserver getViewTreeObserver(){return observer;}public int getWidth(){return width;}public int getLayoutDirection(){return rtl;}public Object getBackground(){return background;}public Object getParent(){return parent;}}''',
            'cat/ttcuz/framework/NativeRead.java': (ROOT/'inject/java/cat/ttcuz/framework/NativeRead.java').read_text(),
            'cat/ttcuz/framework/InboxFilter.java': (ROOT/'inject/java/cat/ttcuz/framework/InboxFilter.java').read_text(),
            'cat/ttcuz/framework/Themes.java': 'package cat.ttcuz.framework;class Themes{static boolean dark=true;static boolean isDark(){return dark;}}',
            'cat/ttcuz/framework/Harness.java': '''package cat.ttcuz.framework;import android.view.*;import android.graphics.*;import android.graphics.drawable.*;
public class Harness {public static class Fade extends View{public Paint LL=new Paint();}public static class Holder{public Fade LLJLLIL=new Fade();}
static void check(boolean b){if(!b)throw new AssertionError();}public static void run(){Holder h=new Holder();Fade f=h.LLJLLIL;View bar=new View(),page=new View();bar.parent=page;page.background=new ColorDrawable(0xff000000);InboxFilter.bind(h,bar);
 LinearGradient s=(LinearGradient)f.LL.getShader();check(s.colors[0]==0xff000000&&s.colors[2]==0&&s.start==50&&s.end==0);
 int made=LinearGradient.made;for(int i=0;i<100;i++)f.observer.listener.onPreDraw();check(LinearGradient.made==made);InboxFilter.bind(h,bar);check(LinearGradient.made==made);
 page.background=new ColorDrawable(0xfffafafa);f.observer.listener.onPreDraw();s=(LinearGradient)f.LL.getShader();check(s.colors[0]==0xfffafafa&&s.colors[2]==0x00fafafa);
 f.rtl=1;f.width=80;f.observer.listener.onPreDraw();s=(LinearGradient)f.LL.getShader();check(s.start==0&&s.end==80);
 f.LL.setShader(new Shader());f.observer.listener.onPreDraw();check(f.LL.getShader() instanceof LinearGradient);
 f.attach.onViewDetachedFromWindow(f);check(f.observer.listener==null);f.attach.onViewAttachedToWindow(f);check(f.observer.listener!=null);
 bar.background=new ColorDrawable(0x55000000);page.background=null;Themes.dark=false;f.observer.listener.onPreDraw();check(((LinearGradient)f.LL.getShader()).colors[0]==0xffffffff);
 }}''',
            'Test.java': 'public class Test{public static void main(String[] a){cat.ttcuz.framework.Harness.run();}}',
        })
