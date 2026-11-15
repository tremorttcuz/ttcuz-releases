import os
import tempfile
import unittest
from unittest import mock

from ttcuz import dexpatch

CHAT = """.class public Lcom/ss/android/ugc/aweme/im/sdk/chat/ui/singleactivity/BaseChatRootFragment;
.super Landroidx/fragment/app/SAFTikTokFragment;

.method public onResume()V
    .registers 2

    .prologue
    invoke-super {p0}, Landroidx/fragment/app/SAFTikTokFragment;->onResume()V

    return-void
.end method
"""

STORY = """.class public final Lcom/ss/android/ugc/aweme/story/api/StoryApi;
.super Ljava/lang/Object;

.method public reportStoryViewed(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;)LX/05nY;
    .registers 5
    .param p1    # Ljava/lang/String;
        .annotation runtime LX/05lW;
            value = "story_id"
        .end annotation
    .end param

    iget-object v0, p0, Lcom/ss/android/ugc/aweme/story/api/StoryApi;->LIZ:Ljava/lang/Object;

    return-object v0
.end method
"""

# a subclass names the base class too, and must not get the hook a second time
SUBCLASS = CHAT.replace(".class public Lcom/ss/android/ugc/aweme/im/sdk/chat/ui/singleactivity/BaseChatRootFragment;",
                        ".class public LX/0abc;").replace(".super Landroidx/fragment/app/SAFTikTokFragment;",
                        ".super Lcom/ss/android/ugc/aweme/im/sdk/chat/ui/singleactivity/BaseChatRootFragment;")


class PortedHooksTest(unittest.TestCase):
    def write(self, root, files):
        for name, text in files.items():
            with open(os.path.join(root, name), "w", encoding="utf-8") as handle:
                handle.write(text)

    def read(self, root, name):
        with open(os.path.join(root, name), encoding="utf-8") as handle:
            return handle.read()

    def test_ports_go_in_once_and_only_off_47_2_41(self):
        with tempfile.TemporaryDirectory() as root, mock.patch.dict(os.environ, {"TTCUZ_TIKTOK_VERSION": "47.7.3"}):
            self.write(root, {"chat.smali": CHAT, "story.smali": STORY, "sub.smali": SUBCLASS})
            counts = dexpatch.rewrite_ported(root)
            self.assertEqual(1, counts["ported chat dim"])
            self.assertEqual(1, counts["ported hide stories"])
            self.assertEqual({}, dexpatch.rewrite_ported(root))
            story = self.read(root, "story.smali")
            # the guard comes after the parameter's annotations, before TikTok's first instruction
            self.assertLess(story.index(".end param"), story.index("hideStories"))
            self.assertLess(story.index("hideStories"), story.index("iget-object v0, p0"))
            self.assertIn("LX/05nY;->LJIIZILJ(Ljava/lang/Throwable;)LX/1Rb4;", story)
            self.assertNotIn("Dim;", self.read(root, "sub.smali"))
        with tempfile.TemporaryDirectory() as root, mock.patch.dict(os.environ, {"TTCUZ_TIKTOK_VERSION": "47.2.41"}):
            self.write(root, {"chat.smali": CHAT})
            self.assertEqual({}, dexpatch.rewrite_ported(root))

    def test_targets_are_the_mods_statics(self):
        targets = dexpatch.ported_targets()
        self.assertIn("Lcat/ttcuz/framework/Privacy;->hideRead()Z", targets)
        self.assertTrue(all(target.startswith("Lcat/ttcuz/framework/") for target in targets))


if __name__ == "__main__":
    unittest.main()
