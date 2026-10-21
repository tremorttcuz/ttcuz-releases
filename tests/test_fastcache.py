"""Unchanged DEX skip work; unknown edits must never become cache hits."""
import ast
import hashlib
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from ttcuz import dexcache, fastcache


class FastCacheTest(unittest.TestCase):
    def test_reviewed_photo_rule_is_scoped_but_future_edits_are_not(self):
        current = Path(fastcache.__file__).with_name("dexpatch.py").read_bytes()
        tree = ast.parse(current)
        photo = next(n for n in tree.body if isinstance(n, ast.FunctionDef) and n.name == "rewrite_comment_photo_count")
        self.assertEqual(hashlib.sha256(ast.dump(photo, include_attributes=False).encode()).hexdigest(), fastcache.PHOTO_RULE)
        absent = fastcache.normalized(current, b"unrelated class", defined={"Unrelated"})
        self.assertNotIn("name='rewrite_comment_photo_count'", absent)
        self.assertIn("name='rewrite_comment_photo_count'", fastcache.normalized(current, b"", defined={"MvChoosePhotoScene"}))
        head, tail = current.split(b'def rewrite_comment_photo_count', 1)
        changed = head + b'def rewrite_comment_photo_count' + tail.replace(b'count = 0', b'count = 123', 1)
        self.assertTrue(absent != fastcache.normalized(changed, b"unrelated class", defined={"Unrelated"}))
        self.assertNotEqual(absent, fastcache.normalized(current + b'\nUNKNOWN_RULE = True\n', b"unrelated class", defined={"Unrelated"}))

    def test_saved_answer_checks_input_context_and_output_digest(self):
        with tempfile.TemporaryDirectory() as folder, patch.dict(os.environ, {}, clear=False):
            os.environ.pop("TTCUZ_CACHE_BASELINE", None)
            tool = Path(folder, "tool.jar")
            tool.write_bytes(b"tool")
            answers = fastcache.Answers(folder, str(tool), 23, {}, {})
            output = Path(folder, "o-" + "a" * 64 + ".dex")
            output.write_bytes(b"patched")
            answers.store(b"input", str(output), {"hooks": 2})
            self.assertEqual(answers.load(b"input"), (str(output), {"hooks": 2}))
            self.assertIsNone(answers.load(b"different input"))
            self.assertIsNone(fastcache.Answers(folder, "other-tool.jar", 23, {}, {}).load(b"input"))
            self.assertIsNone(fastcache.Answers(folder, str(tool), 23, {}, {"new": ("a", "b")}).load(b"input"))
            output.write_bytes(b"damaged")
            self.assertIsNone(answers.load(b"input"))


if __name__ == "__main__":
    unittest.main()
