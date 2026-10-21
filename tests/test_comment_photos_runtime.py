import pathlib, tempfile, unittest
import test_settings_safety_runtime as runtime
from ttcuz import dexpatch


class CommentPhotosTest(unittest.TestCase):
    def test_only_comment_album_is_limited_to_nine(self):
        source = (pathlib.Path(__file__).resolve().parents[1] / 'inject/java/cat/ttcuz/framework/CommentPhotos.java').read_text('utf8')
        runtime.SafetyRuntimeTest.run_java(self, {
            'cat/ttcuz/framework/CommentPhotos.java': source,
            'cat/ttcuz/framework/NativeRead.java': '''package cat.ttcuz.framework; class NativeRead {static Object field(Object o,String n)throws Exception{return o.getClass().getField(n).get(o);}}''',
            'Test.java': '''import cat.ttcuz.framework.CommentPhotos; public class Test {
             public static class Session {public String businessKey;Session(String k){businessKey=k;}}
             public static class Scene {public Session sessionData;Scene(String k){sessionData=new Session(k);}}
             public static void main(String[] args) {
              if(CommentPhotos.limit(new Scene("comment_multi_photo"),1)!=9)throw new AssertionError("single photo gate remains");
              if(CommentPhotos.limit(new Scene("comment_multi_photo"),9)!=9)throw new AssertionError("over nine allowed");
              if(CommentPhotos.limit(new Scene("normal_upload"),35)!=35)throw new AssertionError("video uploader changed");
              if(CommentPhotos.images(java.util.Arrays.asList(1,2,3,4,5,6,7,8,9,10)).size()!=9)throw new AssertionError("over-nine payload");
              if(CommentPhotos.limit(null,3)!=3)throw new AssertionError("missing context changed");
             }}''',
        })

    def test_patch_preserves_registers_and_does_not_touch_other_fields(self):
        owner = 'Lcom/ss/android/ugc/aweme/shortvideo/mvtemplate/choosemedia/MvChoosePhotoScene;'
        with tempfile.TemporaryDirectory() as folder:
            path = pathlib.Path(folder)/'MvChoosePhotoScene.smali'
            original = '.class public '+owner+'\n.super Ljava/lang/Object;\n.method public assign(I)V\n .registers 2\n iput p1, p0, '+owner+'->maxPhotoSelectCount:I\n iput p1, p0, '+owner+'->maxVideoSelectCount:I\n return-void\n.end method\n'
            path.write_text(original,'utf8')
            self.assertEqual({'native comment photo count':1},dexpatch.rewrite_comment_photo_count(folder))
            changed=path.read_text()
            self.assertIn('invoke-static {p0, p1}, Lcat/ttcuz/framework/CommentPhotos;->limit(Ljava/lang/Object;I)I\n move-result p1',changed)
            self.assertIn('iput p1, p0, '+owner+'->maxVideoSelectCount:I',changed)
            self.assertEqual({},dexpatch.rewrite_comment_photo_count(folder))
