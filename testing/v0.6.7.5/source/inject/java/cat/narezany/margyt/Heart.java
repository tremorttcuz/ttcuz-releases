package cat.narezany.margyt;
/** Only the native double-tap heart host is routed here. */
public final class Heart {
    private Heart(){}
    // 47.2.41 DiggAnimationView's ordinary double-tap heart, prepared by
    // Accent's existing background worker at startup and on palette changes.
    private static final int NATIVE_HEART=0x7f0106a8;
    private static volatile Prepared prepared;
    private static final class Prepared {
        final int colour,density;final android.graphics.Bitmap image;
        Prepared(int colour,int density,android.graphics.Bitmap image){this.colour=colour;this.density=density;this.image=image;}
    }
    static void prewarm() {
        android.content.Context context=Margy.context();if(context==null)return;
        int colour=LikeColors.colour(),density=context.getResources().getDisplayMetrics().densityDpi;
        Prepared ready=prepared;if(ready!=null && ready.colour==colour && ready.density==density)return;
        try {
            Class<?> type=Class.forName("com.bytedance.tux.drawable.TuxIconDrawable");
            android.graphics.drawable.Drawable icon=(android.graphics.drawable.Drawable)type
                    .getConstructor(android.content.Context.class,int.class).newInstance(context,NATIVE_HEART);
            int size=Math.min(512,Math.max(1,Math.round(100f*density/160f)));
            type.getMethod("LJ",int.class).invoke(icon,size);
            type.getMethod("LJFF",int.class).invoke(icon,colour);
            android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(size,size,android.graphics.Bitmap.Config.ARGB_8888);
            image.setDensity(density);
            icon.setBounds(0,0,size,size);icon.draw(new android.graphics.Canvas(image));
            if(colour==LikeColors.colour())prepared=new Prepared(colour,density,image);
        }catch(Throwable error){Diary.note("prepared double-tap heart: "+error);}
    }
    private static boolean preparedIcon(com.bytedance.tux.icon.TuxIconView view,int resource) {
        Prepared ready=prepared;
        if(resource!=NATIVE_HEART || ready==null || ready.colour!=LikeColors.colour()
                || ready.density!=view.getResources().getDisplayMetrics().densityDpi)return false;
        android.widget.ImageView image=(android.widget.ImageView)(Object)view;
        android.graphics.drawable.Drawable original=image.getDrawable();
        final int width=original==null?ready.image.getWidth():original.getIntrinsicWidth();
        final int height=original==null?ready.image.getHeight():original.getIntrinsicHeight();
        image.clearColorFilter();
        image.setImageDrawable(new android.graphics.drawable.BitmapDrawable(view.getResources(),ready.image){
            public int getIntrinsicWidth(){return width;}
            public int getIntrinsicHeight(){return height;}
        });
        synchronized(views){views.put(view,ready.colour);}
        return true;
    }
    private static final java.util.Map<android.view.View,Integer> views=new java.util.WeakHashMap<android.view.View,Integer>();
    private static final java.util.Map<android.view.View,Integer> resources=new java.util.WeakHashMap<android.view.View,Integer>();
    private static void tint(android.view.View view) {
        int colour=LikeColors.colour();
        synchronized(views){Integer known=views.get(view);if(known!=null&&known.intValue()==colour)return;views.put(view,colour);}
        // A view-level filter also covers bitmap frames whose drawable filter is reset by the host.
        if(view instanceof android.widget.ImageView)((android.widget.ImageView)view).setColorFilter(colour,android.graphics.PorterDuff.Mode.SRC_IN);
    }
    static void refresh() {
        java.util.List<android.view.View> all;
        synchronized(views){all=new java.util.ArrayList<android.view.View>(views.keySet());}
        for(android.view.View view:all)if(view!=null && view.isAttachedToWindow())view.post(()->{
            Integer resource; synchronized(resources){resource=resources.get(view);}
            if(view instanceof com.bytedance.tux.icon.TuxIconView && resource!=null
                    && preparedIcon((com.bytedance.tux.icon.TuxIconView)view,resource))return;
            if(view instanceof com.bytedance.tux.icon.TuxIconView)((com.bytedance.tux.icon.TuxIconView)view).setTintColor(LikeColors.colour());
            tint(view);
        });
    }
    public static void setIconRes(com.bytedance.tux.icon.TuxIconView view,int resource) {
        if(view==null)return;
        view.setTag(0x54544C48,Boolean.TRUE);
        synchronized(resources){resources.put(view,resource);}
        if(preparedIcon(view,resource))return;
        synchronized(views){views.remove(view);}
        view.setIconRes(resource);
        view.setTintColor(LikeColors.colour());
        tint(view);
    }
    public static void setTintColor(com.bytedance.tux.icon.TuxIconView view,int ignored) {
        if(view==null)return;
        view.setTag(0x54544C48,Boolean.TRUE);
        synchronized(views){Integer known=views.get(view);if(known!=null && known==LikeColors.colour())return;}
        view.setTintColor(LikeColors.colour());
        tint(view);
    }
    public static void setImageDrawable(android.widget.ImageView view,android.graphics.drawable.Drawable image) {
        if(view==null)return;
        view.setTag(0x54544C48,Boolean.TRUE);
        synchronized(views){Integer known=views.get(view);
            if(view.getDrawable()==image && known!=null && known==LikeColors.colour())return;
            views.remove(view);
        }
        synchronized(resources){resources.remove(view);}
        view.setImageDrawable(image);
        tint(view);
    }
}
