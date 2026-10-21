"""Regressions for the separate native heart path and stale ImageView tints."""
import unittest
import test_settings_safety_runtime as safety


class NativeGradientRuntimeTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_explicit_accent_ignores_stale_white_tint_but_keeps_real_states(self):
        method = safety.method("AccentGradient.java", "static void image(")
        self.run_java({
            "android/graphics/drawable/Drawable.java": "package android.graphics.drawable;public class Drawable{public Object callback;public void setCallback(Object c){callback=c;}}",
            "android/content/res/ColorStateList.java": """package android.content.res;public class ColorStateList{
                public int colour;public boolean stateful;public ColorStateList(int c){colour=c;}
                public int getDefaultColor(){return colour;}public int getColorForState(int[] state,int fallback){return stateful&&state.length>0&&state[0]==0?0xff777777:colour;}}
            """,
            "android/widget/ImageView.java": """package android.widget;import android.graphics.drawable.Drawable;import android.content.res.ColorStateList;
                public class ImageView{public Drawable drawable=new Drawable();public ColorStateList tint;
                    public Drawable getDrawable(){return drawable;}public void setImageDrawable(Drawable d){drawable=d;}
                    public ColorStateList getImageTintList(){return tint;}public int[] getDrawableState(){return new int[]{1};}}
            """,
            "Test.java": """import android.widget.ImageView;import android.graphics.drawable.Drawable;import android.content.res.ColorStateList;
                class Accent{static boolean nativeArtworkView(Object v){return false;}}
                public class Test{
                    static boolean matches(int c){return c==0xff112233;}
                    static class Tint extends Drawable{Drawable source;Object callback=new Object();ColorStateList colours;Tint(Drawable s){source=s;}}
                    static Drawable wrap(Drawable d,int c){if(d instanceof Tint)d=((Tint)d).source;return matches(c)?new Tint(d):d;}
                """ + method + """
                    public static void main(String[] args){ImageView view=new ImageView();view.tint=new ColorStateList(0xffffffff);
                        image(view,0xff112233);if(!(view.drawable instanceof Tint)||((Tint)view.drawable).colours!=null)throw new AssertionError("white tint still suppresses explicit gradient");
                        view.tint=new ColorStateList(0xff112233);view.tint.stateful=true;image(view,0xff112233);
                        Tint tint=(Tint)view.drawable;if(tint.colours!=view.tint||tint.source.callback!=tint.callback)throw new AssertionError("state or native animation callback lost");
                        if(tint.colours.getColorForState(new int[]{0},0)!=0xff777777)throw new AssertionError("disabled state lost");
                        image(view,0xff777777);if(view.drawable instanceof Tint)throw new AssertionError("unliked icon kept gradient");
                    }}""",
        })

    def test_prepared_double_tap_and_same_colour_retints_apply_gradient(self):
        methods = "".join(safety.method("Heart.java", marker) for marker in (
            "private static boolean preparedIcon(", "private static void tint(",
            "public static void setIconRes(", "public static void setTintColor("))
        self.run_java({
            "android/view/View.java": "package android.view;public class View{public void setTag(int key,Object value){}}",
            "android/util/DisplayMetrics.java": "package android.util;public class DisplayMetrics{public int densityDpi=160;}",
            "android/content/res/Resources.java": "package android.content.res;public class Resources{public android.util.DisplayMetrics getDisplayMetrics(){return new android.util.DisplayMetrics();}}",
            "android/graphics/PorterDuff.java": "package android.graphics;public class PorterDuff{public enum Mode{SRC_IN}}",
            "android/graphics/Bitmap.java": "package android.graphics;public class Bitmap{public int getWidth(){return 100;}public int getHeight(){return 100;}}",
            "android/graphics/drawable/Drawable.java": "package android.graphics.drawable;public class Drawable{public int getIntrinsicWidth(){return 100;}public int getIntrinsicHeight(){return 100;}}",
            "android/graphics/drawable/BitmapDrawable.java": "package android.graphics.drawable;public class BitmapDrawable extends Drawable{public BitmapDrawable(android.content.res.Resources r,android.graphics.Bitmap b){}}",
            "android/widget/ImageView.java": """package android.widget;public class ImageView extends android.view.View{
                android.graphics.drawable.Drawable drawable=new android.graphics.drawable.Drawable();public int colour;
                public android.graphics.drawable.Drawable getDrawable(){return drawable;}public void setImageDrawable(android.graphics.drawable.Drawable d){drawable=d;}
                public void clearColorFilter(){}public void setColorFilter(int c,android.graphics.PorterDuff.Mode m){colour=c;}
                public android.content.res.Resources getResources(){return new android.content.res.Resources();}}
            """,
            "com/bytedance/tux/icon/TuxIconView.java": "package com.bytedance.tux.icon;public class TuxIconView extends android.widget.ImageView{public void setIconRes(int r){}public void setTintColor(int c){colour=c;}}",
            "Test.java": """import java.util.*;import android.view.View;import android.widget.ImageView;import com.bytedance.tux.icon.TuxIconView;
                class LikeColors{static int value=1;static int colour(){return value;}}
                class AccentGradient{static int calls,last;static void image(ImageView v,int c){calls++;last=c;}}
                public class Test{
                    static final int NATIVE_HEART=7;static class Prepared{int colour=1,density=160;android.graphics.Bitmap image=new android.graphics.Bitmap();}
                    static Prepared prepared=new Prepared();static Map<View,Integer> views=new WeakHashMap<>(),resources=new WeakHashMap<>();
                """ + methods + """
                    public static void main(String[] args){TuxIconView heart=new TuxIconView();setIconRes(heart,7);
                        if(AccentGradient.calls!=1)throw new AssertionError("prepared heart bypassed gradient");
                        setTintColor(heart,1);if(AccentGradient.calls!=2)throw new AssertionError("same primary colour bypasses changed gradient");
                        LikeColors.value=2;setTintColor(heart,2);if(AccentGradient.last!=2||heart.colour!=2)throw new AssertionError("palette change lost");
                    }}""",
        })


if __name__ == "__main__":
    unittest.main()
