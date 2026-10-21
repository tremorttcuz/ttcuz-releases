import tempfile
import unittest
from pathlib import Path
from ttcuz import profile_banner, dexpatch


class ProfileBannerTest(unittest.TestCase):
    def test_native_player_gate_and_idempotence(self):
        with tempfile.TemporaryDirectory() as folder:
            file = Path(folder) / "video.smali"
            code = (
                ".class public final " + profile_banner.OWNER + "\n.super Ljava/lang/Object;\n"
                ".method public final getAutoPlay()Z\n .registers 2\n"
                " iget-boolean v0, p0, "
                + profile_banner.OWNER
                + "->autoPlay:Z\n return v0\n.end method\n"
            )
            file.write_text(code, encoding="utf-8")
            self.assertEqual(profile_banner.REQUIRED, profile_banner.rewrite(folder))
            result = file.read_text(encoding="utf-8")
            self.assertIn(profile_banner.TARGET, result)
            self.assertIn("if-eqz v0, :ttcuz_banner_native", result)
            self.assertIn("iget-boolean v0, p0", result)
            self.assertEqual({}, profile_banner.rewrite(folder))
            self.assertTrue(dexpatch.interesting(profile_banner.OWNER.encode()))

    def test_missing_local_fails_instead_of_overwriting_receiver(self):
        with tempfile.TemporaryDirectory() as folder:
            Path(folder, "video.smali").write_text(
                ".class public " + profile_banner.OWNER + "\n.super Ljava/lang/Object;\n"
                ".method public getAutoPlay()Z\n .registers 1\n return p0\n.end method\n",
                encoding="utf-8",
            )
            with self.assertRaises(ValueError):
                profile_banner.rewrite(folder)
