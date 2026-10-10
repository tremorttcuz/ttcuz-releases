package cat.narezany.margyt;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewGroup;
import android.widget.TextView;
/** Native caption placement only; lettering burned into a video is not a View. */
public final class FeedCaptions {
    private static final int WATCHED=0x4D634170;
    private static final int SPEED_WATCHED=0x4D735064;
    private static final java.util.regex.Pattern SPEED=java.util.regex.Pattern.compile("(?iu)(?:Скорость|Speed|Швидкість|Ускорено|Ускорение|Sped up|Accelerated|Прискорено)\\s*:?\\s*(?:2(?:[.,]0)?\\s*[xх×]|[xх×]\\s*2(?:[.,]0)?).*");
    private FeedCaptions(){}
    static void speedLabel(TextView view,CharSequence text) {
        if(text==null)return;
        String s=text.toString().trim();
        if(s.length()>64 || s.indexOf('2')<0 || !SPEED.matcher(s).matches() || Dim.panelContent(view))return;
        View root=view.getRootView();
        int[] at=new int[2],origin=new int[2];view.getLocationOnScreen(at);root.getLocationOnScreen(origin);
        if(root!=view && root.getHeight()>100*view.getResources().getDisplayMetrics().density
                && at[1]-origin[1]<root.getHeight()*0.65f)return;
        final float original=view.getAlpha();
        if(view.getAlpha()!=0f)view.setAlpha(0f); // Keep native spacing/state; the mod's lock pill gives feedback.
        if(Boolean.TRUE.equals(view.getTag(SPEED_WATCHED)))return;
        view.setTag(SPEED_WATCHED,Boolean.TRUE);
        final View parent=view.getParent() instanceof ViewGroup?(View)view.getParent():null;
        final boolean narrow=parent instanceof ViewGroup && parent.getHeight()>0
            && parent.getHeight()<=48*view.getResources().getDisplayMetrics().density
            && ((ViewGroup)parent).getChildCount()<=4;
        final float alpha=narrow?parent.getAlpha():1f;
        if(narrow)parent.setAlpha(0f);
        view.getViewTreeObserver().addOnPreDrawListener(()->{
            CharSequence current=view.getText();
            boolean speed=current!=null && current.length()<=64 && SPEED.matcher(current.toString().trim()).matches() && !Dim.panelContent(view);
            if(speed){if(view.getAlpha()!=0f)view.setAlpha(0f);if(narrow && parent.getAlpha()!=0f)parent.setAlpha(0f);}
            else {if(view.getAlpha()!=original)view.setAlpha(original);if(narrow && parent.getAlpha()!=alpha)parent.setAlpha(alpha);}
            return true;
        });
    }
    /** Watch the native edge-speed component even when it uses resource-id text setters. */
    public static void bindSpeed(View container){
        if(container==null || Boolean.TRUE.equals(container.getTag(0x4D735065)))return;
        container.setTag(0x4D735065,Boolean.TRUE);
        container.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
            long searched;
            View nativeFeedback;float nativeAlpha;boolean hidden;
            public boolean onPreDraw(){
                long now=android.os.SystemClock.uptimeMillis();View root=container.getRootView();
                // 47.2.41 draws the label inside X.0Gnl, not a TextView.
                boolean own=SpeedGesture.captionsLow(root);
                if(nativeFeedback==null && own && now-searched>=200)nativeFeedback=container.findViewById(0x7f0a6fa5);
                if(nativeFeedback!=null){
                    if(own){if(!hidden){nativeAlpha=nativeFeedback.getAlpha();hidden=true;}if(nativeFeedback.getAlpha()!=0f)nativeFeedback.setAlpha(0f);}
                    else if(hidden){if(nativeFeedback.getAlpha()==0f)nativeFeedback.setAlpha(nativeAlpha);hidden=false;}
                }
                if(nativeFeedback==null && container.isShown() && own && now-searched>=200){searched=now;scanSpeed(container,0,new int[]{128});}
                return true;
            }
        });
    }
    private static void scanSpeed(View view,int depth,int[] budget){
        if(view==null || !view.isShown() || depth>30 || budget[0]--<=0)return;
        if(view instanceof TextView)speedLabel((TextView)view,((TextView)view).getText());
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)scanSpeed(group.getChildAt(i),depth+1,budget);}
    }
    public static void bindNative(Object holder){
        try{Object view=holder.getClass().getMethod("getContentView").invoke(holder);if(view instanceof View)bind((View)view);}catch(Throwable ignored){}
    }
    public static void bind(View container) {
        if(container==null)return;
        // Exact native caption frame in TikTok 47.2.41, not the entire cell.
        View found=container.findViewById(0x7f0a1397);
        if(found==null)found=container.getRootView().findViewById(0x7f0a1397);
        final View caption=found;
        if(caption==null || Boolean.TRUE.equals(caption.getTag(WATCHED)))return;
        caption.setTag(WATCHED,Boolean.TRUE);
        final float original=caption.getTranslationY();
        caption.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
            boolean shifted;
            final int[] at=new int[2],origin=new int[2];
            final java.util.Map<ViewGroup,Boolean> paddingClips=new java.util.WeakHashMap<>();
            int navigation=-1;
            final java.util.Map<ViewGroup,Boolean> clips=new java.util.WeakHashMap<ViewGroup,Boolean>();
            public boolean onPreDraw(){
                View root=caption.getRootView();
                boolean low=SpeedGesture.captionsLow(root);
                if(!low || !caption.isShown()){
                    if(shifted){caption.setTranslationY(original);shifted=false;for(java.util.Map.Entry<ViewGroup,Boolean> entry:clips.entrySet())entry.getKey().setClipChildren(entry.getValue());clips.clear();for(java.util.Map.Entry<ViewGroup,Boolean> entry:paddingClips.entrySet())entry.getKey().setClipToPadding(entry.getValue());paddingClips.clear();}
                    return true;
                }
                android.view.ViewParent parent=caption.getParent();
                while(parent instanceof ViewGroup){ViewGroup group=(ViewGroup)parent;if(!clips.containsKey(group)){clips.put(group,group.getClipChildren());paddingClips.put(group,group.getClipToPadding());group.setClipChildren(false);group.setClipToPadding(false);}parent=group.getParent();}
                caption.getLocationOnScreen(at);root.getLocationOnScreen(origin);
                float dp=caption.getResources().getDisplayMetrics().density;int nav=0;
                if(navigation<0){int id=caption.getResources().getIdentifier("navigation_bar_height","dimen","android");navigation=id==0?0:caption.getResources().getDimensionPixelSize(id);}nav=navigation;
                float y=caption.getTranslationY()+root.getHeight()-nav-12*dp-caption.getHeight()-(at[1]-origin[1]);
                if(Math.abs(caption.getTranslationY()-y)>0.5f)caption.setTranslationY(y);
                shifted=true;return true;
            }
        });
    }
}
