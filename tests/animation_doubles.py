STUBS = {
    "android/animation/Animator.java": "package android.animation;public class Animator{}",
    "android/animation/AnimatorListenerAdapter.java": "package android.animation;public class AnimatorListenerAdapter{public void onAnimationEnd(Animator a){}}",
    "android/animation/ValueAnimator.java": "package android.animation;public class ValueAnimator extends Animator{float value,to;AnimatorUpdateListener update;AnimatorListenerAdapter listener;public static boolean manual;public static ValueAnimator last;public boolean cancelled;public interface AnimatorUpdateListener{void onAnimationUpdate(ValueAnimator v);}public static ValueAnimator ofFloat(float a,float b){ValueAnimator v=new ValueAnimator();v.value=a;v.to=b;last=v;return v;}public void setDuration(long v){}public void setInterpolator(Object o){}public void addUpdateListener(AnimatorUpdateListener l){update=l;}public void addListener(AnimatorListenerAdapter l){listener=l;}public Object getAnimatedValue(){return Float.valueOf(value);}public void start(){if(!manual)finish();}public void finish(){if(cancelled)return;value=to;if(update!=null)update.onAnimationUpdate(this);if(listener!=null)listener.onAnimationEnd(this);}public void cancel(){cancelled=true;if(listener!=null)listener.onAnimationEnd(this);}}",
}
STUBS["android/view/animation/PathInterpolator.java"] = (
    "package android.view.animation;public class PathInterpolator {public PathInterpolator(float a,float b,float c,float d){}}"
)
STUBS["cat/ttcuz/framework/Motion.java"] = (
    "package cat.ttcuz.framework;class Motion {static final android.view.animation.PathInterpolator OUT=new android.view.animation.PathInterpolator(0,0,0,0),IN=OUT,BOTH=OUT;static final long SHORT=180L,MEDIUM=300L,LONG=340L;static boolean enabled(android.content.Context c){return true;}}"
)
