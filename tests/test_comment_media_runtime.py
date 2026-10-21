import unittest
import test_settings_safety_runtime as safety


class CommentMediaRuntimeTest(unittest.TestCase):
    def test_opening_frame_stays_until_video_has_rendered(self):
        methods = '\n'.join(safety.method('CommentMediaView.java', m) for m in
                            ['    private void openingFrame(', '    public void onSurfaceTextureUpdated('])
        self.run_java({
            'android/graphics/Bitmap.java': 'package android.graphics;public class Bitmap{}',
            'android/graphics/SurfaceTexture.java': 'package android.graphics;public class SurfaceTexture{}',
            'android/widget/ImageView.java': 'package android.widget;public class ImageView{public Object image;public int visibility;public Object getDrawable(){return image;}public void setImageBitmap(android.graphics.Bitmap b){image=b;}public void setVisibility(int v){visibility=v;}}',
            'Test.java': '''import java.io.File;import android.widget.ImageView;import android.graphics.*;
                public class Test{static final int VISIBLE=0,INVISIBLE=4;File file=new File("video");
                ImageView preview=new ImageView();boolean frameSeen;Object player=new Object();int videoWidth,videoHeight=1280;
                static class CommentPoster{static Bitmap frame=new Bitmap();static Bitmap get(File f){return frame;}}
                static class Diary{static void note(String s){}}
                '''+methods+'''
                static void ck(boolean b,String m){if(!b)throw new AssertionError(m);}
                public static void main(String[] a){Test t=new Test();t.openingFrame();
                ck(t.preview.image!=null&&t.preview.visibility==VISIBLE,"opening frame shown immediately");
                t.onSurfaceTextureUpdated(new SurfaceTexture());ck(!t.frameSeen,"unprepared surface cannot expose a blank frame");
                t.videoWidth=720;t.onSurfaceTextureUpdated(new SurfaceTexture());ck(t.frameSeen&&t.preview.visibility==INVISIBLE,"video replaces poster on the first rendered frame");
                t.openingFrame();ck(t.preview.visibility==INVISIBLE,"pre-draw cannot flash poster over playing video");}}
            ''',
        })
    run_java = safety.SafetyRuntimeTest.run_java

    def test_background_does_not_grow_sheet_and_scaling_follows_bounds(self):
        methods = "\n".join(safety.method("CommentMediaView.java", marker) for marker in (
            "    protected void onMeasure(", "    void scale(", "    private void crop("))
        self.run_java({
            'android/graphics/Matrix.java': 'package android.graphics;public class Matrix{public float x,y,cx,cy;public void setScale(float a,float b,float c,float d){x=a;y=b;cx=c;cy=d;}}',
            'Test.java': '''import android.graphics.Matrix;
class Base {int width,height;protected void onMeasure(int w,int h){width=MeasureSpec.getSize(w);height=MeasureSpec.getSize(h);}int getWidth(){return width;}int getHeight(){return height;}}
class MeasureSpec {static final int EXACTLY=1<<30,AT_MOST=2<<30;static int getMode(int n){return n&0xc0000000;}static int getSize(int n){return n&0x3fffffff;}static int makeMeasureSpec(int size,int mode){return size|mode;}}
class ImageView {enum ScaleType{FIT_XY,FIT_CENTER,CENTER_CROP}ScaleType type;void setScaleType(ScaleType t){type=t;}}
class TextureView {Matrix matrix;void setTransform(Matrix m){matrix=m;}}
class LegacyGif {int mode;}
class Picture {void invalidate(){}}
public class Test extends Base {
 Object picture;ImageView preview;int scaleMode,videoWidth=720,videoHeight=1280;TextureView texture=new TextureView();
'''.replace('Object picture','Picture picture').replace('class ImageView {','class ImageView extends Picture {').replace('class LegacyGif {','class LegacyGif extends Picture {') + methods + '''
 static void ck(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  Test t=new Test();t.onMeasure(MeasureSpec.makeMeasureSpec(720,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(1500,MeasureSpec.AT_MOST));
  ck(t.height==0); // Background adds no screen-height demand to wrap_content.
  t.onMeasure(MeasureSpec.makeMeasureSpec(720,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(600,MeasureSpec.EXACTLY));
  ck(t.height==600);t.scale(0);ck(t.texture.matrix.x==1&&t.texture.matrix.y>2&&t.texture.matrix.cy==300);
  t.scale(1);ck(t.texture.matrix.y==1&&t.texture.matrix.x<1);
  t.scale(2);ck(t.texture.matrix.x==1&&t.texture.matrix.y==1);
  t.onMeasure(MeasureSpec.makeMeasureSpec(720,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(350,MeasureSpec.EXACTLY));t.scale(0);ck(t.height==350&&t.texture.matrix.cy==175);
  ImageView image=new ImageView();t.picture=image;t.scale(1);ck(image.type==ImageView.ScaleType.FIT_CENTER);
  t.scale(2);ck(image.type==ImageView.ScaleType.FIT_XY);LegacyGif gif=new LegacyGif();t.picture=gif;t.scale(1);ck(gif.mode==1);
 }
}'''})

    def test_muted_video_releases_when_hidden_and_rejects_stale_callbacks(self):
        methods = "\n".join(
            safety.method("CommentMediaView.java", signature)
            for signature in [
                "    void setPaused(",
                "    private void updateVisibility(",
                "    void suspend(",
                "    private void error(",
                "    private void play(",
                "    public void onSurfaceTextureAvailable(",
            ]
        )
        sources = {
            "android/graphics/SurfaceTexture.java": "package android.graphics;public class SurfaceTexture{}",
            "android/view/View.java": "package android.view;public class View{public static final int VISIBLE=0;public void invalidate(){}public void removeCallbacks(Runnable r){}}",
            "android/view/Surface.java": "package android.view;public class Surface{public boolean released;public Surface(android.graphics.SurfaceTexture t){}public void release(){released=true;}}",
            "android/view/TextureView.java": "package android.view;public class TextureView{public boolean available=true;public boolean isAvailable(){return available;}public android.graphics.SurfaceTexture getSurfaceTexture(){return new android.graphics.SurfaceTexture();}}",
            "android/graphics/drawable/Drawable.java": "package android.graphics.drawable;public class Drawable{}",
            "android/graphics/drawable/Animatable.java": "package android.graphics.drawable;public interface Animatable{void start();void stop();}",
            "android/media/MediaPlayer.java": """package android.media;public class MediaPlayer{
public interface Prepared{void onPrepared(MediaPlayer p);}public interface Error{boolean onError(MediaPlayer p,int what,int extra);}
public Prepared prepared;public Error error;public boolean loop,released,async;public float left=1,right=1;public int starts;
public void setSurface(android.view.Surface s){}public void setVolume(float l,float r){left=l;right=r;}public void setLooping(boolean b){loop=b;}public void setDataSource(String s){}
public void setOnPreparedListener(Prepared l){prepared=l;}public void setOnErrorListener(Error e){error=e;}public void prepareAsync(){async=true;}
public void start(){if(released)throw new IllegalStateException("released");starts++;}public int getVideoWidth(){return 720;}public int getVideoHeight(){return 1280;}public void release(){if(released)throw new AssertionError("double release");released=true;}
}""",
            "Test.java": """import android.media.*;import android.view.*;import android.graphics.*;import android.graphics.drawable.*;import java.io.*;public class Test{
static final int VISIBLE=0;File file=new File("local");boolean gif,paused,active,broken,loading,frameSeen;boolean attached=true,shown=true,focus=true;int window=VISIBLE,videoWidth,videoHeight;TextureView texture=new TextureView();MediaPlayer player;Surface surface;View picture;Drawable animation;Runnable failed=()->{};
static class LegacyGif extends View{boolean running;Runnable frame=()->{};}static class Diary{static void note(String s){}}static class Screen{static void say(String s){}}
boolean isAttachedToWindow(){return attached;}boolean isShown(){return shown;}boolean hasWindowFocus(){return focus;}int getWindowVisibility(){return window;}void openingFrame(){}void loadGif(){}void crop(){}void post(Runnable r){}
"""
            + methods
            + """
static void ck(boolean b,String s){if(!b)throw new AssertionError(s);}
public static void main(String[]a){Test waiting=new Test();waiting.texture.available=false;waiting.updateVisibility();ck(waiting.active&&waiting.player==null,"waiting surface started decoder");waiting.texture.available=true;waiting.updateVisibility();ck(waiting.player!=null,"ready surface never starts while already active");waiting.suspend();
Test t=new Test();t.focus=false;t.onSurfaceTextureAvailable(new android.graphics.SurfaceTexture(),720,1280);MediaPlayer first=t.player;Surface firstSurface=t.surface;
ck(first!=null&&first.async&&first.loop&&first.left==0&&first.right==0&&first.starts==0,"visible non-focused sheet did not prepare muted looping video");MediaPlayer.Prepared late=first.prepared;
t.setPaused(true);ck(t.player==null&&first.released&&firstSurface.released&&!t.active,"pause leaked decoder");late.onPrepared(first);ck(first.starts==0,"stale prepare restarted released decoder");
t.setPaused(false);MediaPlayer second=t.player;ck(second!=null&&second!=first,"resume did not prepare a fresh decoder");second.prepared.onPrepared(second);ck(second.starts==1,"ready video not started");
t.shown=false;t.updateVisibility();ck(second.released&&t.player==null,"hidden sheet still playing");t.updateVisibility();ck(t.player==null,"hidden sheet restarted");
t.shown=true;t.updateVisibility();MediaPlayer third=t.player;t.window=8;t.updateVisibility();ck(third.released&&t.player==null,"background activity playing");
t.window=0;t.focus=false;t.updateVisibility();MediaPlayer fourth=t.player;ck(fourth!=null,"visible non-focused sheet stays blank");t.focus=true;t.updateVisibility();ck(t.player==fourth,"focus change recreated player");t.setPaused(true);ck(fourth.released&&t.player==null,"fragment pause ignored");t.setPaused(false);fourth=t.player;ck(fourth!=null,"fragment resume did not restart");
ck(fourth.error.onError(fourth,100,1)&&t.broken&&fourth.released,"error did not stop decoder");t.updateVisibility();ck(t.player==null,"failed media automatically retried every frame");t.suspend();
}}
""",
        }
        self.run_java(sources)
