"""Saving a comment photo must not be hijacked by the feed video underneath."""

import unittest
import test_settings_safety_runtime as safety


class CommentPhotoContextTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_dialog_owner_keeps_comment_photo_save_native(self):
        methods = "\n".join(
            safety.method("VideoActions.java", m)
            for m in [
                " static Object downloadPost(Context from)",
                " private static android.app.Activity activity(",
                " static Object downloadPost()",
            ]
        )
        sources = {
            "android/content/Context.java": "package android.content;public class Context{}",
            "android/content/ContextWrapper.java": "package android.content;public class ContextWrapper extends Context{Context base;public ContextWrapper(Context c){base=c;}public Context getBaseContext(){return base;}}",
            "android/app/Activity.java": "package android.app;public class Activity extends android.content.Context{}",
            "android/view/View.java": "package android.view;public class View{android.content.Context c;public boolean shown=true,attached=true;public View(android.content.Context context){c=context;}public boolean isShown(){return shown;}public boolean isAttachedToWindow(){return attached;}public android.content.Context getContext(){return c;}}",
            "Test.java": """import android.content.Context;import android.view.View;import java.lang.ref.WeakReference;
public class Test{static Object selected;static WeakReference<View> selectedView;"""
            + methods
            + """
 static void check(boolean v){if(!v)throw new AssertionError();}
 public static void main(String[] args){
  android.app.Activity feed=new android.app.Activity(),photo=new android.app.Activity();
  selected=new Object();View view=new View(new android.content.ContextWrapper(feed));selectedView=new WeakReference<>(view);
  check(downloadPost(new android.content.ContextWrapper(feed))==selected);
  check(downloadPost(new android.content.ContextWrapper(photo))==null);
  view.shown=false;check(downloadPost(feed)==null);view.shown=true;view.attached=false;check(downloadPost(feed)==null);
 }
}""",
        }
        self.run_java(sources)
