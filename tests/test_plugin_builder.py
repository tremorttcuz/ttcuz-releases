import json
from pathlib import Path
import tempfile
import unittest
import zipfile
from ttcuz import plugin


class PluginBuilderTest(unittest.TestCase):
    def test_manifest_requires_a_real_api_and_safe_identity(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            base = {"id": "ttcuz.demo", "entry": "example.Demo", "api": 1}
            (root / "manifest.json").write_text(json.dumps(base), encoding="utf8")
            self.assertEqual(plugin.manifest(root), base)
            for key, value in [("id", "../escape"), ("id", ""), ("entry", ""), ("api", True), ("api", 2)]:
                (root / "manifest.json").write_text(json.dumps(dict(base, **{key: value})), encoding="utf8")
                with self.assertRaises(ValueError):
                    plugin.manifest(root)

    def test_bundle_keeps_multidex_assets_and_libraries(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            folder, dex = root / "plugin", root / "dex"
            (folder / "assets").mkdir(parents=True)
            (folder / "lib/arm64-v8a").mkdir(parents=True)
            dex.mkdir()
            (dex / "classes.dex").write_bytes(b"dex-one")
            (dex / "classes2.dex").write_bytes(b"dex-two")
            (folder / "assets/icon.png").write_bytes(b"icon")
            (folder / "lib/arm64-v8a/test.so").write_bytes(b"native")
            destination = root / "demo.ttplugin"
            metadata = {"id": "demo", "entry": "Demo", "api": 1}
            plugin.package(folder, dex, destination, metadata)
            with zipfile.ZipFile(destination) as archive:
                self.assertEqual(archive.testzip(), None)
                self.assertEqual(json.loads(archive.read("manifest.json")), metadata)
                self.assertEqual(archive.read("classes2.dex"), b"dex-two")
                self.assertEqual(archive.read("lib/arm64-v8a/test.so"), b"native")
                self.assertEqual(archive.read("assets/icon.png"), b"icon")
            self.assertFalse(destination.with_suffix(".ttplugin.tmp").exists())

    def test_sdk_only_exports_the_public_plugin_api(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / plugin.PREFIX).mkdir(parents=True)
            (root / plugin.PREFIX / "TtcuzPlugin.class").write_bytes(b"api")
            (root / "cat/ttcuz/framework/Private.class").write_bytes(b"host")
            destination = root / "sdk.jar"
            self.assertEqual(plugin.write_sdk(str(root), str(destination)), 1)
            with zipfile.ZipFile(destination) as archive:
                self.assertNotIn("cat/ttcuz/framework/Private.class", archive.namelist())
                self.assertEqual(archive.read(plugin.PREFIX + "TtcuzPlugin.class"), b"api")


if __name__ == "__main__":
    unittest.main()
