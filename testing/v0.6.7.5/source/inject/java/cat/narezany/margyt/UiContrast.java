package cat.narezany.margyt;

import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.WeakHashMap;

/** Only neutral foregrounds in the four native controls reported in 47.2.41. */
final class UiContrast {
    private UiContrast() {}
    private static final WeakHashMap<TextView, Saved> texts = new WeakHashMap<>();
    private static final class Saved {
        final ColorStateList original;
        final ColorStateList applied;
        Saved(ColorStateList original, ColorStateList applied) { this.original=original; this.applied=applied; }
    }

    static int colour(View view, int colour) {
        if (view == null || !black(colour) || !target(view) || !darkPanel(view)) return colour;
        // Explicit appearance choices have priority, including intentionally dark text.
        if (AppearanceColors.get(view instanceof TextView ? AppearanceColors.TEXT
                : AppearanceColors.CONTROL) != 0) return colour;
        return (colour & 0xff000000) | 0xeeeeee;
    }

    private static final WeakHashMap<View,Integer> tints=new WeakHashMap<>();
    static void forgetTint(View view){tints.remove(view);}
    private static final int PENDING=0x7e0c0387;

    static int tint(com.bytedance.tux.icon.TuxIconView view,int source) {
        int corrected=colour(view,source);
        if(corrected!=source)tints.put(view,corrected);else tints.remove(view);
        defer(view,source);
        return corrected;
    }

    static void repaintTint(com.bytedance.tux.icon.TuxIconView view,int source) {
        int corrected=colour(view,source);
        Integer applied=tints.get(view);
        if(corrected!=source) {
            if(applied==null || applied!=corrected) {
                view.setTintColor(corrected);
                tints.put(view,corrected);
            }
        } else if(applied!=null) {
            view.setTintColor(source);
            tints.remove(view);
        }
    }

    static void defer(final View view,int colour) {
        if(!black(colour) || view.getParent()!=null || Boolean.TRUE.equals(view.getTag(PENDING)))return;
        view.setTag(PENDING,Boolean.TRUE);
        final Runnable repair=new Runnable(){public void run(){
            view.setTag(PENDING,null);
            repaint(view);
            Accent.repaintContrast(view);
        }};
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            public void onViewAttachedToWindow(View attached){
                attached.removeOnAttachStateChangeListener(this);
                attached.post(repair);
            }
            public void onViewDetachedFromWindow(View detached){}
        });
    }

    static void forgetText(TextView view) { texts.remove(view); }

    static void repaint(View view) {
        if (!(view instanceof TextView)) return;
        TextView text=(TextView)view;
        int current=text.getCurrentTextColor();
        Saved saved=texts.get(text);
        if (saved != null && text.getTextColors() != saved.applied) { texts.remove(text); saved=null; }
        int original=saved == null ? current : saved.original.getColorForState(
                text.getDrawableState(), saved.original.getDefaultColor());
        int corrected=colour(view, original);
        if (corrected != original) {
            if (saved == null) {
                ColorStateList before=text.getTextColors();
                ColorStateList after=textColours(text,before);
                if(after==before)return;
                texts.put(text,new Saved(before,after));
                text.setTextColor(after);
            }
        } else if (saved != null) {
            text.setTextColor(saved.original);
            texts.remove(text);
        }
    }

    private static ColorStateList textColours(TextView view,ColorStateList source) {
        if(!source.isStateful())return ColorStateList.valueOf(colour(view,source.getDefaultColor()));
        // Keep disabled/pressed states and opacity. Never flatten an unreadable list.
        try {
            int[][] states=(int[][])ColorStateList.class.getMethod("getStates").invoke(source);
            int[] colours=((int[])ColorStateList.class.getMethod("getColors").invoke(source)).clone();
            for(int i=0;i<colours.length;i++)colours[i]=colour(view,colours[i]);
            return new ColorStateList(states,colours);
        }catch(Throwable ignored){return source;}
    }

    static boolean target(View view) {
        int id=view.getId();
        // BaseInputAssem's mention, emoji and media buttons; VideoRepostAssem's label.
        if (id==0x7f0a4208 || id==0x7f0a401f || id==0x7f0a419d || id==0x7f0a4136)
            return view instanceof com.bytedance.tux.icon.TuxIconView;
        if (id==0x7f0a6d75) return view instanceof TextView;
        // The native playlist inflater separates the prefix and playlist title.
        // A title such as "Love Trend" cannot be recognised from its wording.
        if(id==0x7f0a8586 && view instanceof TextView) {
            View at=view;
            for(int i=0;at!=null && i<6;i++) {
                if(at.getId()==0x7f0a5ef9)return true;
                at=at.getParent() instanceof View?(View)at.getParent():null;
            }
        }
        if (view instanceof TextView) {
            CharSequence content=((TextView)view).getText();
            if (content == null || content.length()>180) return false;
            String label=content.toString().trim();
            if (label.startsWith("Плейлист") || label.startsWith("Playlist")
                    || label.startsWith("Playlist •") || label.startsWith("Playlist ·")
                    || label.equals("Репост подписчикам") || label.equals("Repost to followers")) return true;
            return false;
        }
        if (!(view instanceof com.bytedance.tux.icon.TuxIconView)) return false;
        // A compact action menu has multiple distinct action labels. A message body,
        // avatar, photograph or emoji drawable cannot qualify as a menu icon.
        View parent=view;
        for (int i=0;i<3;i++) {
            if (!(parent.getParent() instanceof ViewGroup)) break;
            parent=(View)parent.getParent();
            if (Integer.bitCount(menuLabels(parent,0,new int[]{0}))>=2) return true;
        }
        return false;
    }

    private static int menuLabels(View view,int depth,int[] seen) {
        if (depth>3 || ++seen[0]>40) return 0;
        if (view instanceof TextView) {
            CharSequence content=((TextView)view).getText();
            if (content==null || content.length()>40) return 0;
            String label=content.toString().trim();
            if(label.equals("Ответить") || label.equals("Reply"))return 1;
            if(label.equals("Переслать") || label.equals("Forward"))return 2;
            if(label.equals("Сохранить") || label.equals("Save"))return 4;
            if(label.equals("Удалить") || label.equals("Delete"))return 8;
            if(label.equals("Отменить отправку") || label.equals("Unsend"))return 16;
            return 0;
        }
        int count=0;
        if (view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount() && Integer.bitCount(count)<2;i++)
                count|=menuLabels(group.getChildAt(i),depth+1,seen);
        }
        return count;
    }

    private static boolean black(int colour) {
        int r=(colour>>>16)&255,g=(colour>>>8)&255,b=colour&255;
        return (colour>>>24)>=128 && Math.max(r,Math.max(g,b))<=64
                && Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<=24;
    }

    private static boolean darkPanel(View view) {
        int front=0;
        for(int depth=0;view!=null && depth<12;depth++) {
            Drawable background=view.getBackground();
            if(background!=null)background=background.getCurrent();
            ColorStateList tint=view.getBackgroundTintList();
            Integer colour=null;
            if (tint!=null) colour=tint.getColorForState(view.getDrawableState(),tint.getDefaultColor());
            else if (background instanceof ColorDrawable) colour=((ColorDrawable)background).getColor();
            else if (android.os.Build.VERSION.SDK_INT>=24 && background instanceof GradientDrawable) {
                ColorStateList fill=((GradientDrawable)background).getColor();
                if(fill!=null) colour=fill.getColorForState(view.getDrawableState(),fill.getDefaultColor());
            }
            // Composite translucent chips over their real parent, never over media pixels.
            if(colour!=null) front=over(front,colour);
            if((front>>>24)==255) return (((front>>>16)&255)*299
                    +((front>>>8)&255)*587+(front&255)*114)<112000;
            view=view.getParent() instanceof View ? (View)view.getParent() : null;
        }
        return false;
    }

    private static int over(int front,int back) {
        int a=front>>>24,b=back>>>24,alpha=a+(b*(255-a)+127)/255;
        if(alpha==0)return 0;
        int out=alpha<<24;
        for(int shift=0;shift<=16;shift+=8) {
            int channel=(((front>>>shift)&255)*a
                    +(((back>>>shift)&255)*b*(255-a)+127)/255+alpha/2)/alpha;
            out|=channel<<shift;
        }
        return out;
    }
}
