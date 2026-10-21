import pathlib, tempfile, unittest
from ttcuz import dexpatch


class AvatarHooksTest(unittest.TestCase):
    def test_avatar_shape_lands_on_our_package_and_only_drawables(self):
        self.assertEqual("Lcat/ttcuz/framework/AvatarShape;", dexpatch.AVATAR_SHAPE)
        with tempfile.TemporaryDirectory() as folder:
            root = pathlib.Path(folder)
            owner = dexpatch.ROUND_DRAWABLES[0]
            method = """.method public draw(Landroid/graphics/Path;)V
 .registers 6
 invoke-virtual {p1, v0, v1, v2, v3}, Landroid/graphics/Path;->addCircle(FFFLandroid/graphics/Path$Direction;)V
 return-void
.end method
"""
            for name, descriptor in [("Avatar", owner), ("Unrelated", "Lother/Widget;")]:
                (root / (name + ".smali")).write_text(
                    ".class public " + descriptor + "\n.super Ljava/lang/Object;\n" + method
                )
            counts = dexpatch.rewrite_anchored(folder)
            self.assertEqual(1, sum(counts.values()))
            self.assertIn(
                dexpatch.AVATAR_SHAPE + "->addCircle", (root / "Avatar.smali").read_text()
            )
            self.assertNotIn(dexpatch.AVATAR_SHAPE, (root / "Unrelated.smali").read_text())
            self.assertFalse(dexpatch.rewrite_anchored(folder))
