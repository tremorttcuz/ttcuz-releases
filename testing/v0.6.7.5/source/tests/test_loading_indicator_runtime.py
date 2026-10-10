"""Run the ported seven-shape renderer and the native bytecode hook."""
import pathlib, tempfile, unittest
from test_expressive_runtime import SOURCES
import test_settings_safety_runtime as safety
from margyt import dexpatch
ROOT=pathlib.Path(__file__).resolve().parents[1]

class LoadingIndicatorTest(unittest.TestCase):
    run_java=safety.SafetyRuntimeTest.run_java

    def test_native_hook_is_scoped_and_idempotent(self):
        with tempfile.TemporaryDirectory() as d:
            p=pathlib.Path(d)/'com/bytedance/tux/status/loading/TuxSpinner.smali'
            p.parent.mkdir(parents=True)
            p.write_text('.class public Lcom/bytedance/tux/status/loading/TuxSpinner;\n.super Landroid/widget/ImageView;\n.method public final onDraw(Landroid/graphics/Canvas;)V\n    .registers 5\n    invoke-super {p0,p1}, Landroid/widget/ImageView;->onDraw(Landroid/graphics/Canvas;)V\n    return-void\n.end method\n',encoding='utf-8')
            self.assertEqual({dexpatch.loading_feature(dexpatch.LOADING_SPINNER):1},dexpatch.rewrite_loading_spinner(d))
            patched=p.read_text(encoding='utf-8')
            self.assertIn(dexpatch.LOADING_TARGET,patched)
            self.assertNotIn('invoke-super',patched)
            self.assertEqual({},dexpatch.rewrite_loading_spinner(d))
            self.assertEqual(patched,p.read_text(encoding='utf-8'))
            self.assertTrue(dexpatch.interesting(dexpatch.LOADING_SPINNER.encode()))
            self.assertIn(dexpatch.LOADING_TARGET,dexpatch.rewrite_targets())

    def test_all_ball_variants_keep_native_state_gates(self):
        with tempfile.TemporaryDirectory() as d:
            for owner,(preview,running,ready,size) in dexpatch.LOADING_BALLS.items():
                p=pathlib.Path(d)/(owner[1:-1]+'.smali');p.parent.mkdir(exist_ok=True,parents=True)
                fields=''.join('.field public '+f+':Z\n' for f in {preview,running,ready} if f)
                p.write_text('.class public '+owner+'\n.super Landroid/view/View;\n'+fields+'.field public '+size+':I\n.method public final onDraw(Landroid/graphics/Canvas;)V\n    .registers 2\n    return-void\n.end method\n')
            counts=dexpatch.rewrite_loading_spinner(d)
            self.assertEqual({dexpatch.loading_feature(o):1 for o in dexpatch.LOADING_BALLS},counts)
            for owner,(preview,running,ready,size) in dexpatch.LOADING_BALLS.items():
                body=(pathlib.Path(d)/(owner[1:-1]+'.smali')).read_text()
                self.assertIn('->'+(ready or running)+':Z',body)
                self.assertIn('if-eqz v0, :ttcuz_loading_end',body)
                self.assertIn('->'+size+':I',body)
                self.assertIn('->'+running+':Z',body)
                self.assertIn(dexpatch.LOADING_BALL_TARGET,body)
                self.assertTrue(dexpatch.interesting(owner.encode()))
            self.assertEqual({},dexpatch.rewrite_loading_spinner(d))
            p.write_text(p.read_text().replace(':Z',':I',1).replace(dexpatch.LOADING_BALL_TARGET,'Lother;->draw()V'))
            with self.assertRaisesRegex(RuntimeError,'loading flag changed'):dexpatch.rewrite_loading_spinner(d)

    def test_lottie_hook_preserves_other_animations(self):
        with tempfile.TemporaryDirectory() as d:
            p=pathlib.Path(d)/'X/1HkT.smali';p.parent.mkdir()
            p.write_text('.class public LX/1HkT;\n.super Landroid/widget/ImageView;\n.field public composition:LX/1Hl1;\n.method public isAnimating()Z\n    .registers 2\n    const/4 v0, 0x0\n    return v0\n.end method\n.method public static com_airbnb_lottie_LottieAnimationView_com_bytedance_tt_reliability_monitor_viewchecker_BitmapCrashChecker_onDraw(LX/1HkT;Landroid/graphics/Canvas;)V\n    .registers 2\n    return-void\n.end method\n.method public onDraw(Landroid/graphics/Canvas;)V\n    .registers 2\n    return-void\n.end method\n')
            self.assertEqual({dexpatch.loading_feature(dexpatch.LOADING_LOTTIE):1},dexpatch.rewrite_loading_spinner(d))
            s=p.read_text();self.assertIn(dexpatch.LOADING_LOTTIE_TARGET,s)
            self.assertIn('if-eqz v0, :ttcuz_native_lottie',s)
            self.assertIn('invoke-static {p0, p1}, LX/1HkT;->com_airbnb',s)
            self.assertEqual({},dexpatch.rewrite_loading_spinner(d))

    def test_render_geometry_accent_motion_and_visibility(self):
        sources={k:v for k,v in SOURCES.items() if k.startswith('android/')}
        sources['android/view/View.java']=sources['android/view/View.java'].replace('public int w=100,h=50;', 'public android.content.res.Resources getResources(){return getContext().getResources();}public int w=100,h=50,frames;public boolean shown=true;public boolean isShown(){return shown&&vis==VISIBLE;}public int getWindowVisibility(){return vis;}public void postInvalidateOnAnimation(){frames++;}')
        sources['android/graphics/Canvas.java']=sources['android/graphics/Canvas.java'].replace('public int paths,circles,lines;', 'public int paths,circles,lines,colour;public float rotation;public void rotate(float r,float x,float y){rotation=r;}').replace('paths++;last=p;', 'paths++;last=p;colour=q.colour;')
        sources['android/os/SystemClock.java']='package android.os;public class SystemClock{public static long now;public static long uptimeMillis(){return now;}}'
        sources['cat/narezany/margyt/Accent.java']='package cat.narezany.margyt;class Accent{static int colour=0xff427b6e;static int colour(){return colour;}}'
        sources['cat/narezany/margyt/Motion.java']='package cat.narezany.margyt;class Motion{static boolean on=true;static boolean enabled(android.content.Context c){return on;}}'
        sources['cat/narezany/margyt/LoadingIndicator.java']=(ROOT/'inject/java/cat/narezany/margyt/LoadingIndicator.java').read_text(encoding='utf-8')
        sources['cat/narezany/margyt/Check.java']='''package cat.narezany.margyt;
        import android.graphics.*;import android.view.*;import android.os.SystemClock;
        public class Check{static void ck(boolean b,String s){if(!b)throw new AssertionError(s);}
        public static void run(){View v=new View(new android.content.Context());v.w=48;v.h=48;
        java.util.Set<String> shapes=new java.util.HashSet<>();
        for(int i=0;i<7;i++){SystemClock.now=i*650;Canvas c=new Canvas();LoadingIndicator.draw(v,c);ck(c.paths==1&&c.circles==0&&c.last.pts.size()==180,"one complete MD3 path");StringBuilder key=new StringBuilder();for(float[] p:c.last.pts){ck(Float.isFinite(p[0])&&Float.isFinite(p[1])&&p[0]>=-2&&p[0]<=50&&p[1]>=-2&&p[1]<=50,"geometry bounded");key.append(p[0]).append(',').append(p[1]);}shapes.add(key.toString());ck(c.colour==Accent.colour,"current accent");}ck(shapes.size()==7,"seven distinct forms");
        Accent.colour=0xffed6f4a;SystemClock.now=333;Canvas c=new Canvas();LoadingIndicator.draw(v,c);ck(c.colour==Accent.colour&&c.rotation>0,"live accent and rotation");
        int frames=v.frames;v.shown=false;LoadingIndicator.draw(v,new Canvas());ck(v.frames==frames,"hidden view stops animation requests");
        v.shown=true;Motion.on=false;Canvas still=new Canvas();LoadingIndicator.draw(v,still);ck(v.frames==frames&&still.rotation==0,"reduced motion uses static form");
        Motion.on=true;Canvas preview=new Canvas();LoadingIndicator.draw(v,preview,24,false);ck(v.frames==frames&&preview.paths==1&&preview.rotation==0,"pull preview is static");
        for(int t=0;t<4550;t+=17){SystemClock.now=t;Canvas morph=new Canvas();LoadingIndicator.draw(v,morph);for(float[] point:morph.last.pts)ck(Float.isFinite(point[0])&&Float.isFinite(point[1])&&point[0]>=-2&&point[0]<=50&&point[1]>=-2&&point[1]<=50,"spring stays within the view");}
        class Composition{int reads;String name;Composition(String n){name=n;}public String toString(){reads++;return name;}}
        Composition ordinary=new Composition("heart");Canvas normal=new Canvas();ck(!LoadingIndicator.drawLottie(v,normal,ordinary,true)&&normal.paths==0,"heart keeps native renderer");
        Composition loader=new Composition("Layer: ttcuz_MD3_loader");Canvas lc=new Canvas();ck(LoadingIndicator.drawLottie(v,lc,loader,true)&&lc.paths==1,"raw or cached composition replaced");
        LoadingIndicator.drawLottie(v,new Canvas(),loader,true);ck(loader.reads==1,"composition inspected once");
        frames=v.frames;Canvas paused=new Canvas();LoadingIndicator.drawLottie(v,paused,loader,false);ck(v.frames==frames&&paused.rotation==0,"paused Lottie does not schedule frames");
        ck(!LoadingIndicator.drawLottie(v,new Canvas(),null,true),"no composition leaves native draw");
        v.w=400;v.h=800;Canvas large=new Canvas();LoadingIndicator.drawLottie(v,large,loader,true);for(float[] point:large.last.pts)ck(Math.abs(point[0]-200)<50&&Math.abs(point[1]-400)<50,"full screen loading stays compact");
        v.w=0;Canvas zero=new Canvas();LoadingIndicator.draw(v,zero);ck(zero.paths==0,"zero size safe");}}
        '''
        sources['Test.java']='public class Test{public static void main(String[] args){cat.narezany.margyt.Check.run();}}'
        self.run_java(sources)
