"""Install the three generated settings icons and refresh their embedded PNGs."""
from __future__ import annotations

import base64
import io
import re
import shutil
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
GENERATED = Path.home() / ".codex/generated_images/01a12a7f-96da-73a1-accc-947a30cd2a0b"
CONCEPTS = ROOT / "outputs/imagegen-icon-concepts"
ICONS = ROOT / "icons"
SNAPSHOT = ROOT / "outputs/ttcuz-source-v0.6.8.5-privacy"
SOURCES = {
    "fingerprint": "exec-c1c0b9e0-4bef-4c1e-b9ab-222f5b29d816.png",
    "animation": "exec-0ae831d6-f9d2-4a14-9909-8992ddd94ff4.png",
    "blur_on": "exec-32c87e0a-f016-4968-80b5-a9a178b7d8fd.png",
}


def icon_png(source: Path) -> bytes:
    with Image.open(source) as image:
        alpha = image.convert("RGBA").getchannel("A")
        alpha = alpha.point(lambda value: value if value >= 10 else 0)
        bounds = alpha.getbbox()
        if bounds is None:
            raise ValueError(f"Generated icon has no visible pixels: {source}")
        alpha = alpha.crop(bounds)
        alpha.thumbnail((42, 42), Image.Resampling.LANCZOS)
        result = Image.new("RGBA", (48, 48), (0, 0, 0, 0))
        result.paste((255, 255, 255, 255),
                     ((48 - alpha.width) // 2, (48 - alpha.height) // 2,
                      (48 + alpha.width) // 2, (48 + alpha.height) // 2), alpha)
        buffer = io.BytesIO()
        result.save(buffer, format="PNG", optimize=True)
        return buffer.getvalue()


def write_icons_java(destination: Path, pngs: dict[str, bytes]) -> None:
    entries = []
    for name, data in sorted(pngs.items()):
        encoded = base64.b64encode(data).decode("ascii")
        chunks = [encoded[i:i + 72] for i in range(0, len(encoded), 72)]
        body = "\n                + ".join(f'"{chunk}"' for chunk in chunks)
        entries.append(f'        PNG.put("{name}",\n                {body});')
    destination.write_text(
        "package cat.ttcuz.framework;\n\n"
        "import java.util.HashMap;\nimport java.util.Map;\n\n"
        "/** Embedded settings PNGs generated from icons/. */\n"
        "final class Icons {\n    private Icons() {}\n"
        "    static final Map<String, String> PNG = new HashMap<String, String>();\n"
        f"    static {{\n{chr(10).join(entries)}\n    }}\n}}\n",
        encoding="utf-8",
    )


def write_blur_svg(destination: Path) -> None:
    icon = ROOT / "assets/ui/settings/blur_on.svg"
    match = re.search(r'<path\b[^>]*\bd="([^"]+)"', icon.read_text(encoding="utf-8"))
    if not match:
        raise ValueError("blur_on.svg must keep an outer vector path")
    outer = match.group(1)
    destination.write_text(
        '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="currentColor">\n'
        '  <defs>\n'
        '    <filter id="soft" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="0.9"/></filter>\n'
        '    <clipPath id="right"><rect x="12" y="0" width="12" height="24"/></clipPath>\n'
        '  </defs>\n'
        f'  <path d="{outer}"/>\n'
        '  <path d="M 12 3.8 L 12 20.2" fill="none" stroke="currentColor" stroke-width="1.15" stroke-linecap="round"/>\n'
        '  <circle cx="8.1" cy="8.3" r="1.1"/><circle cx="8.1" cy="15.7" r="1.1"/>\n'
        '  <g clip-path="url(#right)" filter="url(#soft)"><circle cx="15.9" cy="8.3" r="1.1"/><circle cx="15.9" cy="15.7" r="1.1"/></g>\n'
        '</svg>\n', encoding="utf-8")


def main() -> None:
    CONCEPTS.mkdir(parents=True, exist_ok=True)
    for existing in sorted(ICONS.glob("*.png")):
        (SNAPSHOT / "icons" / existing.name).write_bytes(existing.read_bytes())
    pngs: dict[str, bytes] = {p.stem: p.read_bytes() for p in ICONS.glob("*.png")}
    for name, filename in SOURCES.items():
        retained = CONCEPTS / f"{name}.png"
        source = retained if retained.is_file() else GENERATED / filename
        if not retained.is_file():
            shutil.copy2(source, retained)
        data = icon_png(source)
        pngs[name] = data
        (ICONS / f"{name}.png").write_bytes(data)
        (SNAPSHOT / "icons" / f"{name}.png").write_bytes(data)

    write_blur_svg(ROOT / "assets/ui/settings/blur_on.svg")
    write_blur_svg(SNAPSHOT / "assets/ui/settings/blur_on.svg")

    write_icons_java(ROOT / "inject/java/cat/ttcuz/framework/Icons.java", pngs)
    write_icons_java(SNAPSHOT / "inject/java/cat/ttcuz/framework/Icons.java", pngs)
    print("Installed generated icons: " + ", ".join(sorted(pngs)))


if __name__ == "__main__":
    main()
