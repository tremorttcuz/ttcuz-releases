import pathlib
import sys
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from ttcuz import privacy, dexpatch


class PrivacyHooksTest(unittest.TestCase):
    def rewrite(self, code):
        with tempfile.TemporaryDirectory() as folder:
            file = pathlib.Path(folder) / "case-collision.1.smali"
            file.write_text(code, encoding="utf-8")
            counts = privacy.rewrite(folder)
            output = file.read_text(encoding="utf-8")
            self.assertEqual({}, privacy.rewrite(folder), "patch must be idempotent")
            self.assertEqual(output, file.read_text(encoding="utf-8"))
            return output, counts

    def test_sender_guards_keep_native_disabled_branch_and_return_types(self):
        for owner, (signature, option, answer) in privacy.GUARDS.items():
            with self.subTest(owner=owner):
                params = signature.split("(")[1].split(")")[0]
                # Plenty of locals, with a parameter annotation before executable code.
                original = (
                    ".class public "
                    + owner
                    + "\n.super Ljava/lang/Object;\n.method public "
                    + signature
                    + '\n .registers 20\n .annotation runtime LExample;\n value = "sender"\n .end annotation\n const-string v1, "native send"\n return-void\n.end method\n'
                )
                out, counts = self.rewrite(original)
                self.assertEqual(1, counts["privacy " + option])
                self.assertIn(answer, out)
                self.assertLess(out.index(".end annotation"), out.index(privacy.PRIVACY))
                self.assertIn(':ttcuz_privacy_native\n const-string v1, "native send"', out)
                untouched, counts = self.rewrite(original.replace(owner, "LUnrelated;"))
                self.assertNotIn(privacy.PRIVACY, untouched)
                self.assertEqual({}, counts)

    def test_fetch_keeps_other_users_and_only_changes_publishing_argument(self):
        for regs, suffix, arg in [
            ("v0, v1, v2, v3, v4", "", "v3"),
            ("v20 .. v24", "/range", "v23"),
            ("p0 .. p4", "/range", "p3"),
        ]:
            with self.subTest(regs=regs):
                call = "invoke-interface" + suffix + " {" + regs + "}, " + privacy.FETCH
                out, counts = self.rewrite(".class public LX/Requester;\n" + call + "\n")
                self.assertEqual({"privacy fetch without presence": 1}, counts)
                self.assertIn(
                    "{" + arg + " .. " + arg + "}, " + privacy.PRIVACY + "->fetchAsReport", out
                )
                self.assertIn(call, out, "incoming status fetch must remain intact")

    def test_no_spare_register_fails_instead_of_overwriting_receiver(self):
        with self.assertRaises(ValueError):
            self.rewrite(
                ".class public LX/06W5;\n.method public LJIL(Ljava/lang/Long;)V\n .registers 2\n return-void\n.end method\n"
            )

    def test_missing_notification_strings_are_scoped(self):
        code = ".class public LX/0TFu;\ninvoke-virtual {v4, v0}, Landroid/content/Context;->getString(I)Ljava/lang/String;\n"
        out, counts = self.rewrite(code)
        self.assertEqual({"IM notification missing strings": 1}, counts)
        self.assertIn(privacy.STRINGS, out)
        out, counts = self.rewrite(code.replace("LX/0TFu;", "LX/Other;"))
        self.assertNotIn(privacy.STRINGS, out)

    def test_native_selection_and_required_targets_include_privacy(self):
        for owner in privacy.OWNERS:
            self.assertTrue(dexpatch.interesting(owner.encode()))
        self.assertTrue(set(privacy.TARGETS) <= set(dexpatch.rewrite_targets()))


if __name__ == "__main__":
    unittest.main()
