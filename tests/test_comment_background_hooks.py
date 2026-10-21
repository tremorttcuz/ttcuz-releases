import pathlib
import sys
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from ttcuz import comment_background, dexpatch


class CommentBackgroundHooksTest(unittest.TestCase):
    def test_lifecycle_is_scoped_idempotent_and_keeps_native_body(self):
        native = (
            ".class public "
            + comment_background.OWNER
            + "\n.super Landroidx/fragment/app/Fragment;\n"
        )
        for method in comment_background.CALLS:
            native += (
                ".method public "
                + method
                + '\n .registers 25\n .annotation runtime LTest;\n .end annotation\n const-string v0, "native"\n return-void\n.end method\n'
            )
        with tempfile.TemporaryDirectory() as root:
            file = pathlib.Path(root) / "native.smali"
            file.write_text(native, encoding="utf-8")
            counts = comment_background.rewrite(root)
            self.assertEqual(comment_background.REQUIRED, counts)
            patched = file.read_text(encoding="utf-8")
            self.assertEqual(4, patched.count('const-string v0, "native"'))
            self.assertIn("invoke-static/range {p1 .. p1}", patched)
            self.assertLess(
                patched.index(".end annotation"), patched.index(comment_background.JAVA)
            )
            self.assertEqual({}, comment_background.rewrite(root))
            self.assertEqual(patched, file.read_text(encoding="utf-8"))
            file.write_text(
                native.replace(comment_background.OWNER, "LUnrelated;"), encoding="utf-8"
            )
            self.assertEqual({}, comment_background.rewrite(root))
            self.assertNotIn(comment_background.JAVA, file.read_text(encoding="utf-8"))

    def test_selection_and_link_verification_require_the_lifecycle(self):
        self.assertTrue(dexpatch.interesting(comment_background.OWNER.encode()))
        self.assertTrue(set(comment_background.TARGETS) <= set(dexpatch.rewrite_targets()))
