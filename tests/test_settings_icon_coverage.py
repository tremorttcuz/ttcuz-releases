from pathlib import Path
import json
import re
import struct
import unittest
import xml.etree.ElementTree as ET
import zipfile


ROOT = Path(__file__).resolve().parents[1]


class SettingsIconCoverage(unittest.TestCase):
    def test_complete_svg_collection_is_valid_and_synced(self):
        root_svgs = sorted((ROOT / "assets/ui").rglob("*.svg"))
        snapshot = ROOT / "outputs/ttcuz-source-v0.6.8.5-privacy"
        snapshot_svgs = sorted((snapshot / "assets/ui").rglob("*.svg"))
        self.assertEqual(len(root_svgs), 94)
        self.assertEqual(
            {p.relative_to(ROOT / "assets/ui").as_posix() for p in root_svgs},
            {p.relative_to(snapshot / "assets/ui").as_posix() for p in snapshot_svgs},
        )
        for source in root_svgs:
            ET.parse(source)
            copy = snapshot / "assets/ui" / source.relative_to(ROOT / "assets/ui")
            self.assertEqual(source.read_bytes(), copy.read_bytes(), f"snapshot {source.name} is stale")

        package = ROOT / "outputs/ttcuz-rounded-icons-2026-10-18.zip"
        with zipfile.ZipFile(package) as archive:
            packaged = [name for name in archive.namelist() if name.endswith(".svg")]
            self.assertEqual(len(packaged), 94)
            for shape in ("circle", "flower6", "cookie9", "square", "sunny"):
                self.assertIn(f"assets/ui/music/shapes/{shape}.svg", packaged)

    def test_literal_menu_icons_have_matching_vector_sources(self):
        activity = (ROOT / "inject/java/cat/ttcuz/framework/SettingsActivity.java").read_text(encoding="utf-8")
        glyphs = (ROOT / "inject/java/cat/ttcuz/framework/SettingsGlyph.java").read_text(encoding="utf-8")
        metadata = json.loads((ROOT / "assets/ui/settings/SOURCES.json").read_text(encoding="utf-8"))
        names = set()
        for method in ("toggleRow", "liveToggleRow", "actionRow", "icon"):
            names.update(re.findall(rf"\b{method}\(\s*\"([^\"]+)\"", activity))

        paths = dict(re.findall(r'paths\.put\("([^\"]+)","([^\"]+)"\)', glyphs))
        custom = set(metadata["custom"]["icons"])
        missing = names - paths.keys()
        self.assertFalse(missing, f"menu icons have no runtime vector: {sorted(missing)}")

        for name in names:
            svg_path = ROOT / "assets/ui/settings" / f"{name}.svg"
            self.assertTrue(svg_path.is_file(), f"{name} has no SVG source")
            svg = svg_path.read_text(encoding="utf-8")
            match = re.search(r'<path\b[^>]*\bd="([^"]+)"', svg)
            self.assertIsNotNone(match, f"{name}.svg has no drawable path")
            if name in custom:
                self.assertEqual(paths[name], match.group(1), f"{name}.svg and runtime path differ")
                commands = set(re.findall(r"[A-Za-z]", paths[name]))
                self.assertLessEqual(commands, set("MLCZ"), f"{name} uses unsupported path commands")

    def test_generated_icons_are_embedded_and_preferred_at_runtime(self):
        glyphs = (ROOT / "inject/java/cat/ttcuz/framework/SettingsGlyph.java").read_text(encoding="utf-8")
        embedded = (ROOT / "inject/java/cat/ttcuz/framework/Icons.java").read_text(encoding="utf-8")
        generated = {
            "delete", "refresh", "remove", "search",
            "video_library", "aspect_ratio", "blur_on", "fingerprint", "animation",
        }
        for name in generated:
            png = ROOT / "icons" / f"{name}.png"
            self.assertTrue(png.is_file(), f"{name} has no generated PNG")
            data = png.read_bytes()
            self.assertEqual(data[:8], b"\x89PNG\r\n\x1a\n", f"{name} is not a PNG")
            self.assertEqual(struct.unpack(">II", data[16:24]), (48, 48), f"{name} is not 48px")
            self.assertIn(f'PNG.put("{name}"', embedded, f"{name} is not embedded for runtime use")
            self.assertIn(f'"{name}".equals(name)', glyphs, f"{name} is missing its bitmap fallback mapping")
        self.assertIn('"fingerprint".equals(name)', glyphs)
        self.assertLess(glyphs.index('"fingerprint".equals(name)'), glyphs.index("String d=paths.get(name)"))
        self.assertIn("Icons.PNG.get(name)", glyphs)

    def test_generated_icons_and_blur_split_are_present(self):
        blur = (ROOT / "assets/ui/settings/blur_on.svg").read_text(encoding="utf-8")
        self.assertIn("feGaussianBlur", blur)
        self.assertIn('x="12"', blur)
        self.assertIn('<circle cx="8.1"', blur)
        self.assertIn('clip-path="url(#right)"', blur)
        for name in ("fingerprint", "animation", "blur_on"):
            original = ROOT / "outputs/imagegen-icon-concepts" / f"{name}.png"
            self.assertTrue(original.is_file(), f"{name} generated source image was not retained")
        with zipfile.ZipFile(ROOT / "outputs/ttcuz-rounded-icons-2026-10-18.zip") as archive:
            for name in ("fingerprint", "animation", "blur_on"):
                self.assertIn(f"imagegen-concepts/{name}.png", archive.namelist())

    def test_every_settings_svg_matches_the_runtime_vector(self):
        glyphs = (ROOT / "inject/java/cat/ttcuz/framework/SettingsGlyph.java").read_text(encoding="utf-8")
        paths = dict(re.findall(r'paths\.put\("([^\"]+)","([^\"]+)"\);', glyphs))
        svgs = sorted((ROOT / "assets/ui/settings").glob("*.svg"))
        self.assertGreaterEqual(len(svgs), 72)
        for svg_path in svgs:
            match = re.search(r'<path\b[^>]*\bd="([^"]+)"', svg_path.read_text(encoding="utf-8"))
            self.assertIsNotNone(match, f"{svg_path.name} has no path")
            self.assertEqual(paths.get(svg_path.stem), match.group(1), f"{svg_path.name} differs from the app vector")
            self.assertLessEqual(set(re.findall(r"[A-Za-z]", match.group(1))), set("MLCZ"), svg_path.name)

        snapshot = ROOT / "outputs/ttcuz-source-v0.6.8.5-privacy"
        snapshot_paths = dict(re.findall(
            r'paths\.put\("([^\"]+)","([^\"]+)"\);',
            (snapshot / "inject/java/cat/ttcuz/framework/SettingsGlyphPaths.java").read_text(encoding="utf-8"),
        ))
        self.assertEqual({p.name for p in svgs}, {p.name for p in (snapshot / "assets/ui/settings").glob("*.svg")})
        for svg_path in svgs:
            source = re.search(r'<path\b[^>]*\bd="([^"]+)"', svg_path.read_text(encoding="utf-8"))
            self.assertEqual(snapshot_paths.get(svg_path.stem), source.group(1), f"snapshot {svg_path.name} is stale")

    def test_music_and_shared_svg_icons_match_runtime_paths(self):
        glyphs = (ROOT / "inject/java/cat/ttcuz/framework/SettingsGlyph.java").read_text(encoding="utf-8")
        paths = dict(re.findall(r'paths\.put\("([^\"]+)","([^\"]+)"\);', glyphs))
        for svg_path in sorted((ROOT / "assets/ui/music").glob("*.svg")):
            match = re.search(r'<path\b[^>]*\bd="([^"]+)"', svg_path.read_text(encoding="utf-8"))
            self.assertIsNotNone(match, f"music/{svg_path.name} has no path")
            self.assertEqual(paths.get(f"m_{svg_path.stem}"), match.group(1), f"music/{svg_path.name} differs from runtime")
            self.assertLessEqual(set(re.findall(r"[A-Za-z]", match.group(1))), set("MLCZ"), svg_path.name)
        for key, source in (("chevron", ROOT / "assets/ui/chevron.svg"), ("restart", ROOT / "assets/ui/restart.svg")):
            match = re.search(r'<path\b[^>]*\bd="([^"]+)"', source.read_text(encoding="utf-8"))
            self.assertEqual(paths.get(key), match.group(1), f"{source.name} differs from runtime")

    def test_comment_background_menu_has_embedded_icons(self):
        latest = ROOT / "outputs/ttcuz-source-v0.6.8.5-privacy"
        activity = (latest / "inject/java/cat/ttcuz/framework/SettingsActivity.java").read_text(encoding="utf-8")
        icons = (latest / "inject/java/cat/ttcuz/framework/Icons.java").read_text(encoding="utf-8")
        factory = (latest / "inject/java/cat/ttcuz/framework/SettingsIconFactory.java").read_text(encoding="utf-8")
        for name in ("video_library", "wallpaper", "aspect_ratio", "contrast", "blur_on", "delete"):
            self.assertIn(f'"{name}"', activity, f"comment background menu does not use {name}")
            self.assertIn(f'PNG.put("{name}"', icons, f"comment background icon {name} is not embedded")
        paths = dict(re.findall(
            r'paths\.put\("([^\"]+)","([^\"]+)"\);',
            (latest / "inject/java/cat/ttcuz/framework/SettingsGlyphPaths.java").read_text(encoding="utf-8"),
        ))
        for name in ("video_library", "aspect_ratio", "blur_on", "delete"):
            data = (latest / "icons" / f"{name}.png").read_bytes()
            self.assertEqual(data[:8], b"\x89PNG\r\n\x1a\n", f"{name} is not a PNG")
            self.assertEqual(struct.unpack(">II", data[16:24]), (48, 48), f"{name} is not 48px")
            svg = (latest / "assets/ui/settings" / f"{name}.svg").read_text(encoding="utf-8")
            vector = re.search(r'<path\b[^>]*\bd="([^"]+)"', svg).group(1)
            self.assertEqual(paths.get(name), vector, f"{name} vector does not match its comment menu SVG")
        self.assertIn("pictureFor(activity, name)", factory)


if __name__ == "__main__":
    unittest.main()
