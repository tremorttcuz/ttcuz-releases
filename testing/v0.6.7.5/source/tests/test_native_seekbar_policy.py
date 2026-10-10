"""Seekbar gates extracted from the actual TikTok 47.2.41 APK."""
import pathlib
import re
import shutil
import tempfile
import unittest
from margyt import dexpatch

ROOT = pathlib.Path(__file__).resolve().parents[1]
FIXTURES = ROOT / 'tests/fixtures/seekbar-47.2.41'


class NativeSeekbarPolicyTest(unittest.TestCase):
    def test_patches_display_gates_after_native_content_checks(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            shutil.copytree(FIXTURES, root, dirs_exist_ok=True)
            originals = {p.name: p.read_text() for p in root.glob('*.smali')}
            counts = dexpatch.rewrite_anchored(str(root))
            self.assertEqual(1, counts['native seekbar eligibility'])
            self.assertEqual(2, counts['native seekbar progress policy'])
            for name, original in originals.items():
                patched = (root / name).read_text()
                # These three boolean hooks are the only mutations. In particular,
                # live/photo/ad, resumed/current-post and prohibited checks survive.
                stripped = re.sub(
                    r'\n\n    invoke-static/range \{([vp]\d+) \.\. \1\}, '
                    + re.escape(dexpatch.SEEKBAR) + r'->allow\(Z\)Z\n\n    move-result \1',
                    '', patched)
                self.assertEqual(original, stripped)
            eligibility = (root / '1Cda.smali').read_text()
            self.assertGreater(eligibility.index('Seekbar;->allow'), eligibility.index('->isPhotoMode('))
            self.assertGreater(eligibility.index('Seekbar;->allow'), eligibility.index('->isLive('))
            controller = (root / '1D3n.smali').read_text()
            self.assertGreater(controller.index('Seekbar;->allow'), controller.index('state: 2, can not drag'))
            self.assertEqual({}, dexpatch.rewrite_anchored(str(root)))

    def test_targets_and_dex_detection_include_policy(self):
        self.assertIn(dexpatch.SEEKBAR + '->allow(Z)Z', dexpatch.rewrite_targets())
        self.assertTrue(dexpatch.carries_an_anchor(b'can not show seekbar, state: 4'))
        self.assertTrue(dexpatch.carries_an_anchor(b'draftProgressBar'))

