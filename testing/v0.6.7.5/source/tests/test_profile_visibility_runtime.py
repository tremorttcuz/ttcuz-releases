import unittest
import test_settings_safety_runtime as safety
class ProfileVisibilityTest(unittest.TestCase):
 run_java=safety.SafetyRuntimeTest.run_java
 def test_cached_profile_is_not_visible_after_return(self):
  method=safety.method('ProfileLayout.java',' static boolean onScreen(')
  self.run_java({
   'android/graphics/Rect.java':'package android.graphics;public class Rect{public int left,top,right,bottom;public int width(){return right-left;}public int height(){return bottom-top;}}',
   'android/view/View.java':'''package android.view;public class View{public View parent;public boolean shown=true,attached=true;public float alpha=1;public int left=0,top=0,right=1000,bottom=1000;public boolean isShown(){return shown;}public boolean isAttachedToWindow(){return attached;}public View getRootView(){return parent==null?this:parent.getRootView();}public Object getParent(){return parent;}public float getAlpha(){return alpha;}public boolean getGlobalVisibleRect(android.graphics.Rect r){r.left=left;r.top=top;r.right=right;r.bottom=bottom;return shown;}}''',
   'Test.java':'''import android.view.View;public class Test{'''+method+'''static void check(boolean b){if(!b)throw new AssertionError();}public static void main(String[] a){View root=new View(),container=new View(),profile=new View();container.parent=root;profile.parent=container;check(onScreen(profile,root));profile.left=1100;profile.right=1400;check(!onScreen(profile,root));profile.left=0;profile.right=1000;container.alpha=0;check(!onScreen(profile,root));container.alpha=1;check(onScreen(profile,root));profile.attached=false;check(!onScreen(profile,root));}}'''
  })
