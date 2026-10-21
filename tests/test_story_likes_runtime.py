"""Hold reads the current cell, including zero/large/absent counts and date fallback."""

import pathlib
import tempfile
import unittest
import test_settings_safety_runtime as safety
from ttcuz import story_likes

ROOT = pathlib.Path(__file__).resolve().parents[1]


class StoryLikesTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_current_model_and_unknown_statistics(self):
        sources = {
            "android/content/Context.java": "package android.content;public class Context{}",
            "android/view/View.java": """package android.view;public class View{public interface OnLongClickListener{boolean onLongClick(View v);}public OnLongClickListener held;public java.util.Map<Integer,View> children=new java.util.HashMap<>();public Object tag;public void setOnLongClickListener(OnLongClickListener l){held=l;}public View findViewById(int id){return children.get(id);}public android.content.Context getContext(){return new android.content.Context();}public Object getTag(int k){return tag;}}""",
            "cat/ttcuz/framework/NativeRead.java": (
                ROOT / "inject/java/cat/ttcuz/framework/NativeRead.java"
            ).read_text(encoding="utf-8"),
            "cat/ttcuz/framework/Panel.java": """package cat.ttcuz.framework;class Panel{static String number;static Panel with(android.content.Context c,Object skin,String title){return new Panel();}Panel info(String icon,String n,String caption,Object action){number=n;return this;}Panel quiet(String s,Object r){return this;}void show(){}}""",
            "cat/ttcuz/framework/Skin.java": "package cat.ttcuz.framework;class Skin{static Object remembered(android.content.Context c){return null;}}",
            "cat/ttcuz/framework/DateInfo.java": "package cat.ttcuz.framework;class DateInfo{static final int CELL_TAG=1;static class Data{long likes;Data(long n){likes=n;}}}",
            "cat/ttcuz/framework/Dates.java": "package cat.ttcuz.framework;class Dates{static DateInfo.Data selectedInfo;}",
            "cat/ttcuz/framework/StoryLikes.java": (
                ROOT / "inject/java/cat/ttcuz/framework/StoryLikes.java"
            ).read_text(encoding="utf-8"),
            "cat/ttcuz/framework/Harness.java": """package cat.ttcuz.framework;import android.view.View;public class Harness{
 public static class Stats{long likes;Stats(long v){likes=v;}public long getDiggCount(){return likes;}}
 public static class Post{Stats stats;Post(long v){stats=new Stats(v);}public Stats getStatistics(){return stats;}}
 public static class Payload{public Object LL;Payload(Object p){LL=p;}}
 public static class Component{public Payload LLJJJJLIIL=new Payload(this);View root=new View();Post post=new Post(0);public View getContentView(){return root;}public Post getAweme(){return post;}}
 static void check(boolean b){if(!b)throw new AssertionError();}public static void run(){java.util.Locale.setDefault(java.util.Locale.US);
 Component c=new Component();View heart=new View();c.root.children.put(0x7f0a2193,heart);StoryLikes.bind(c);check(heart.held!=null&&c.root.held!=null);check(heart.held.onLongClick(heart));check(Panel.number.equals("0"));
 c.post=new Post(3000000000L);heart.held.onLongClick(heart);check(Panel.number.equals("3,000,000,000"));c.post.stats=null;heart.held.onLongClick(heart);check(Panel.number.equals("Недоступно"));c.post=null;check(!heart.held.onLongClick(heart));
 check(StoryLikes.count(null)==-1&&StoryLikes.count(new Post(-5))==-1);View row=new View(),cell=new View();cell.tag=new DateInfo.Data(7);Dates.selectedInfo=new DateInfo.Data(9);StoryLikes.openDate(row,cell);check(Panel.number.equals("7"));StoryLikes.openDate(row,null);check(Panel.number.equals("9"));Dates.selectedInfo=null;StoryLikes.openDate(row,null);check(Panel.number.equals("Недоступно"));StoryLikes.bind(new Object());}}
""",
            "Test.java": "public class Test{public static void main(String[]a){cat.ttcuz.framework.Harness.run();}}",
        }
        self.run_java(sources)

    def test_immersive_hook_keeps_clicks_and_handles_all_returns(self):
        with tempfile.TemporaryDirectory() as folder:
            file = pathlib.Path(folder, "heart.smali")
            file.write_text(
                ".class public " + story_likes.OWNER + "\n.super Ljava/lang/Object;\n"
                ".method public onViewCreated(Landroid/view/View;)V\n .registers 25\n"
                " if-eqz p1, :done\n invoke-virtual {p1}, Landroid/view/View;->performClick()Z\n"
                " return-void\n :done\n return-void\n.end method\n",
                encoding="utf-8",
            )
            self.assertEqual(story_likes.REQUIRED, story_likes.rewrite(folder))
            self.assertEqual(2, file.read_text(encoding="utf-8").count(story_likes.TARGET))
            self.assertIn("performClick()Z", file.read_text(encoding="utf-8"))
            self.assertEqual({}, story_likes.rewrite(folder))
