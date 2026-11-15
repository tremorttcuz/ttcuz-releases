package cat.ttcuz.framework;
/**
 * One surface throughout loading and completion; no swapped background or glyph.
 *
 * Loading: a dark square island (96 dp, 28 dp corners) with the M3 circular wavy progress from the motion lab in its
 * top centre -- a wavy accent arc on a faint track, the percentage written inside the ring. There is no label beside it.
 * Completion: the island itself flows into the Sunny shape while the ring fades, and a check draws itself in.
 */
final class DownloadMark extends android.widget.LinearLayout {
    /** dp: the square island, and the box of the progress ring in its top centre. */
    static final int SIDE=96;
    /** MD3 dark surface container; lighter and translucent when the compositor blurs what is behind it. */
    private static final int SURFACE=0xE624262B,FROSTED=0xB824262B,LIQUID=0x3024262B;
    private boolean frosted;
    /** 0: the frosted see-through colour; 1: the solid one, used while the blur is off during motion. */
    private float solid;
    /** Shape size while the island grows inside an already resized window; -1 means the view's own size. */
    private float visualW=-1,visualH=-1;
    private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.Path contour=new android.graphics.Path(),ring=new android.graphics.Path(),check=new android.graphics.Path(),part=new android.graphics.Path();
    private final android.graphics.PathMeasure measure=new android.graphics.PathMeasure();
    private static final float[] X=new float[LabShapes.N],Y=new float[LabShapes.N];
    static{for(int i=0;i<LabShapes.N;i++){double angle=i*Math.PI*2/LabShapes.N;X[i]=(float)Math.cos(angle);Y[i]=(float)Math.sin(angle);}}
    private final float[] rays=new float[LabShapes.N];
    private final float density;
    private final int contrast;
    private float phase,startWidth,startHeight,shapeWidth=-1,shapeHeight=-1;
    /** The ring's smoothed progress and wave amplitude, as in the lab: sm follows the target with a 120 ms time constant. */
    private float smooth,amplitude;
    private long last=-1;
    private int percent=-1;
    private boolean completing;
    DownloadMark(android.content.Context context,int colour){super(context);density=getResources().getDisplayMetrics().density;contrast=colour;setWillNotDraw(false);}
    void progress(int value){int next=value<0?-1:Math.min(100,value);if(next==percent)return;percent=next;invalidate();}
    void frosted(boolean on){if(frosted!=on){frosted=on;invalidate();}}
    /** The glass under the shape: the mod's own engine, recorded while the island rests, paused while it moves. */
    private final Blur.Backdrop backdrop=Blur.Backdrop.create();
    private boolean moving;
    private final android.graphics.RectF shapeRect=new android.graphics.RectF();
    private final android.view.ViewTreeObserver.OnPreDrawListener glass=()->{record();return true;};
    void moving(boolean on){if(moving!=on){moving=on;invalidate();}}
    /** The glass style chosen in the settings: Liquid Glass (the lens), frosted, or plastic (no glass). */
    private boolean liquidNow;
    private void record(){
        if(backdrop==null||moving||completing||!isShown()||getWidth()==0){return;}
        if(Blur.plain()){frosted(false);return;}
        boolean liquid=Blur.liquid();
        if(liquid!=liquidNow){liquidNow=liquid;invalidate();}
        int tint=Blur.tint()==Blur.TINT_LIGHT?0x59FFFFFF:0x40000000;
        shape(shapeRect);
        frosted(backdrop.record(this,shapeRect,liquid?density:DownloadIsland_BLUR*density,28*density,liquid,tint));
    }
    /** Where the shape is in this view: top-centred, at its visual size. */
    private void shape(android.graphics.RectF out){
        float w=visualW>0?visualW:getWidth(),h=visualH>0?visualH:getHeight();
        out.set((getWidth()-w)/2f,0,(getWidth()+w)/2f,h);
    }
    private static final float DownloadIsland_BLUR=24f;
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();getViewTreeObserver().addOnPreDrawListener(glass);}
    @Override protected void onDetachedFromWindow(){getViewTreeObserver().removeOnPreDrawListener(glass);if(backdrop!=null)backdrop.release();super.onDetachedFromWindow();}
    /** Only the shape takes touches: everything around it, inside this (larger) view, belongs to the screen below. */
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){
        if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN){
            shape(shapeRect);
            if(!completing&&!shapeRect.contains(event.getX(),event.getY()))return false;
        }
        return super.dispatchTouchEvent(event);
    }
    void solid(float amount){amount=Math.max(0,Math.min(1,amount));if(amount!=solid){solid=amount;invalidate();}}
    /** Draw the island at this size, anchored at the top centre (children are clipped to it). */
    void visual(float width,float height){if(width==visualW&&height==visualH)return;visualW=width;visualH=height;invalidate();}
    /** The island opened with nothing to track (the task list): the ring is simply full. */
    void settle(){smooth=1f;invalidate();}
    void begin(int width){
        completing=true;percent=100;startWidth=width;startHeight=visualH>0?visualH:Math.max(getHeight(),Math.round(SIDE*density));
        setContentDescription(Text.t("Загрузка завершена"));setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);morph(0);
    }
    int initialHeight(){return Math.round(startHeight);}
    void morph(float value){phase=Math.max(0,Math.min(1,value));invalidate();}
    @Override protected void onDraw(android.graphics.Canvas canvas){
        float width=completing?startWidth:visualW>0?visualW:getWidth(),height=completing?startHeight:visualH>0?visualH:getHeight();if(width<=0 || height<=0)return;
        // Top-anchored: the shape grows down from the ring; on completion Sunny settles in the top 56 dp.
        float t=completing?phase:0,cx=getWidth()/2f,cy=completing?DownloadMorph.mix(height/2f,28*density,t):height/2f;
        int tint=Accent.colour();
        // the island: its outline is mixed ray by ray with Sunny, whose box is the 56 dp circle it ends as
        if(width!=shapeWidth || height!=shapeHeight){
            for(int i=0;i<LabShapes.N;i++)rays[i]=DownloadMorph.roundedDirection(X[i],Y[i],width,height,28*density);
            shapeWidth=width;shapeHeight=height;
        }
        float[] sunny=LabShapes.get()[LabShapes.SUNNY];float box=56*density;
        contour.reset();
        for(int i=0;i<LabShapes.N;i++){
            float r=DownloadMorph.mix(rays[i],sunny[i]*box,t);
            float x=cx+X[i]*r,y=cy+Y[i]*r;
            if(i==0)contour.moveTo(x,y);else contour.lineTo(x,y);
        }
        contour.close();
        if(frosted&&!moving&&!completing&&backdrop!=null){shape(shapeRect);backdrop.draw(canvas,shapeRect,contour);}
        paint.setStyle(android.graphics.Paint.Style.FILL);paint.setColor(DownloadMorph.colour(frosted?DownloadMorph.colour(liquidNow?LIQUID:FROSTED,SURFACE,solid):SURFACE,tint,t));canvas.drawPath(contour,paint);
        boolean moving=Motion.enabled(getContext());
        // the circular wavy progress, drawn in dp units on a 96 dp box whose centre is (width / 2, 48 dp)
        float fade=Math.max(0,1-t/0.28f);
        boolean live=false;
        if(fade>0)live=drawRing(canvas,tint,fade,moving,cx);
        // the check draws itself in once the shape is mostly Sunny
        float draw=Math.max(0,Math.min(1,(t-0.4f)/0.6f));
        if(draw>0){
            float k=1.15f*density;
            check.reset();check.moveTo(cx-10*k,cy);check.lineTo(cx-3*k,cy+6*k);check.lineTo(cx+11*k,cy-8*k);
            measure.setPath(check,false);part.reset();measure.getSegment(0,measure.getLength()*draw,part,true);
            paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
            paint.setStrokeWidth(3*density);paint.setColor(contrast);canvas.drawPath(part,paint);
        }
        // Only a moving wave or an easing figure asks for the next frame; a settled ring costs nothing.
        if(!completing && moving && live && isAttachedToWindow() && isShown())postInvalidateOnAnimation();
        else last=-1;
    }
    @Override protected void dispatchDraw(android.graphics.Canvas canvas){
        if(visualW>0&&!completing){int save=canvas.save();canvas.clipPath(contour);super.dispatchDraw(canvas);canvas.restoreToCount(save);}
        else super.dispatchDraw(canvas);
    }
    /** m3-motion-lab: R 36, stroke 4, wave 2.2 x sin(10 a - phase) at one cycle per second, 8 dp gaps, 15 px percentage. */
    /** Returns whether the ring is still moving (wave visible, figure easing or indeterminate). */
    private boolean drawRing(android.graphics.Canvas canvas,int tint,float fade,boolean moving,float cx){
        final double TAU=Math.PI*2;final float R=36f,gap=8f/R;
        long now=android.os.SystemClock.uptimeMillis();float dt=last<0?0:Math.min(.1f,(now-last)/1000f);last=now;
        boolean indeterminate=percent<0;
        if(!indeterminate)smooth=DownloadMorph.mix(smooth,percent/100f,1-(float)Math.exp(-dt/.12f));
        float calm=indeterminate?1f:clamp((smooth-.1f)/.08f)*clamp((.95f-smooth)/.08f);
        amplitude=DownloadMorph.mix(amplitude,calm,1-(float)Math.exp(-dt/.15f));
        double phaseShift=(moving?(now%1000)/1000.0:0)*TAU,start=-Math.PI/2+(indeterminate&&moving?now/1500.0*TAU:0);
        double length=indeterminate?DownloadMorph.mix(.12f,.72f,(1-(float)Math.cos(now/2200.0*TAU))/2)*TAU:smooth*TAU;
        double g=gap*clamp((float)(length*R/10));
        int save=canvas.save();canvas.translate(cx,48*density);canvas.scale(density,density);
        paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);paint.setStrokeWidth(4f);
        double rest=TAU-length-2*g;
        if(rest>.05){paint.setColor(withAlpha(tint,.25f*fade));arc(canvas,start+length+g,start+length+g+rest,0,phaseShift);}
        if(length*R>1){paint.setColor(withAlpha(tint,fade));arc(canvas,start,start+length,2.2f*(indeterminate?1f:amplitude)*clamp((float)(length*R/24)),phaseShift);}
        if(!indeterminate){
            paint.setStyle(android.graphics.Paint.Style.FILL);paint.setColor(withAlpha(0xFFF2F0F5,fade));paint.setTextSize(15f);paint.setTextAlign(android.graphics.Paint.Align.CENTER);paint.setTypeface(medium());
            android.graphics.Paint.FontMetrics m=paint.getFontMetrics();canvas.drawText(Math.round(smooth*100)+"%",0,1-(m.ascent+m.descent)/2f,paint);
        }
        canvas.restoreToCount(save);
        return indeterminate||amplitude>.01f||Math.abs(smooth-percent/100f)>.002f;
    }
    private void arc(android.graphics.Canvas canvas,double a0,double a1,float wave,double shift){
        final float R=36f;int n=Math.max(2,(int)Math.ceil((a1-a0)*R/2));ring.reset();
        for(int i=0;i<=n;i++){double a=a0+(a1-a0)*i/n,r=R+wave*Math.sin(a*10-shift);float x=(float)(Math.cos(a)*r),y=(float)(Math.sin(a)*r);if(i==0)ring.moveTo(x,y);else ring.lineTo(x,y);}
        canvas.drawPath(ring,paint);
    }
    private static android.graphics.Typeface face;
    private static android.graphics.Typeface medium(){
        if(face==null){try{face=android.os.Build.VERSION.SDK_INT>=28?android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT,600,false):android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL);}catch(Throwable error){face=android.graphics.Typeface.DEFAULT;}}
        return face;
    }
    private static float clamp(float v){return Math.max(0,Math.min(1,v));}
    private static int withAlpha(int colour,float alpha){return (colour&0xFFFFFF)|(Math.round(255*Math.max(0,Math.min(1,alpha)))<<24);}
}
