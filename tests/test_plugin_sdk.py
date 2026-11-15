"""The plugin SDK (plugins/sdk) must expose exactly the loader's public API, and a plugin must build against it."""
import pathlib
import re
import shutil
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
REAL = ROOT / "inject/java/cat/ttcuz/framework/plugin"
SDK = ROOT / "plugins/sdk/cat/ttcuz/framework/plugin"


def public_api(text):
    """Public method and interface names with their parameter types, comments and bodies ignored."""
    text = re.sub(r"/\*.*?\*/|//[^\n]*", "", text, flags=re.S)
    found = set()
    for m in re.finditer(r"public\s+(?:static\s+|final\s+|abstract\s+)*([\w.<>\[\]]+\s+)?(\w+)\s*\(([^)]*)\)", text):
        params = ",".join(p.strip().rsplit(" ", 1)[0].replace("final ", "") for p in m.group(3).split(",") if p.strip())
        found.add((m.group(2), re.sub(r"\s+", "", params)))
    found |= {("interface", n) for n in re.findall(r"public\s+interface\s+(\w+)", text)}
    return found


class PluginSdkTest(unittest.TestCase):
    def test_sdk_matches_loader_api(self):
        for name in ("PluginContext.java", "TtcuzPlugin.java", "PluginSettings.java"):
            # what the decompiled loader shows of its lambdas and anonymous classes is not API
            real = {m for m in public_api((REAL / name).read_text(encoding="utf-8"))
                    if not m[0].startswith("lambda$") and m[0] not in ("run", "newThread", "onSharedPreferenceChanged", "PluginContext")}
            sdk = public_api((SDK / name).read_text(encoding="utf-8")) - {("PluginContext", "")}
            self.assertEqual(real, sdk, "%s: SDK differs from the loader API" % name)

    def test_manifest_rules(self):
        from ttcuz import plugin
        plugin.check_manifest({"id": "a.b", "name": "n", "version": "1", "author": "x", "entry": "a.b.C"})
        for bad in ({"id": "a b", "name": "n", "version": "1", "author": "x", "entry": "a.b.C"},
                    {"id": "a", "name": "n", "version": "1", "author": "x", "entry": "C"},
                    {"name": "n", "entry": "a.C"}, {"id": "a", "entry": "a.C", "api": 5},
                    {"id": "a", "entry": "a.C", "tiktok": "40.0.0"}):
            with self.assertRaises(plugin.PluginError):
                plugin.check_manifest(bad)

    @unittest.skipUnless(shutil.which("javac") and (ROOT / "tools/android.jar").is_file() and (ROOT / "tools/r8.jar").is_file(),
                         "JDK and downloaded tools required")
    def test_example_builds(self):
        from ttcuz import plugin
        import zipfile
        with tempfile.TemporaryDirectory() as d:
            out = plugin.build(str(ROOT / "examples/hello"), str(pathlib.Path(d) / "hello.ctt"), str(ROOT / "tools"))
            with zipfile.ZipFile(out) as z:
                self.assertEqual(sorted(z.namelist()), ["classes.dex", "manifest.json"])
                self.assertNotIn(b"Lcat/ttcuz/framework/plugin/TtcuzPlugin;" + b"\x00\x00\x00", z.read("classes.dex"))


if __name__ == "__main__":
    unittest.main()
