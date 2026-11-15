"""Package every TTCuz SVG and make review sheets for the icon set."""
from __future__ import annotations

import json
import math
import re
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "assets/ui"
OUT = ROOT / "outputs"
TOKEN = re.compile(r"[A-Za-z]|[-+]?(?:\d*\.\d+|\d+\.?\d*)(?:[eE][-+]?\d+)?")
NAVY = (18, 22, 31)
WHITE = (245, 247, 250)
MUTED = (164, 175, 191)


def flatten_path(data: str, steps: int = 12) -> list[list[tuple[float, float]]]:
    tokens = TOKEN.findall(data)
    contours: list[list[tuple[float, float]]] = []
    points: list[tuple[float, float]] = []
    x = y = sx = sy = 0.0
    command = ""
    i = 0
    while i < len(tokens):
        if tokens[i].isalpha():
            command = tokens[i]
            i += 1
            if command in "Zz":
                if points:
                    contours.append(points)
                    points = []
                x, y = sx, sy
                continue
        if command in "MmLl":
            nx, ny = float(tokens[i]), float(tokens[i + 1]); i += 2
            if command == "m": nx, ny = x + nx, y + ny
            if command in "Mm":
                if points: contours.append(points)
                points = [(nx, ny)]
                sx, sy = nx, ny
                command = "l" if command == "m" else "L"
            else:
                points.append((nx, ny))
            x, y = nx, ny
        elif command in "Cc":
            vals = [float(tokens[i + j]) for j in range(6)]; i += 6
            x1, y1, x2, y2, x3, y3 = vals
            if command == "c":
                x1, y1, x2, y2, x3, y3 = x+x1, y+y1, x+x2, y+y2, x+x3, y+y3
            ox, oy = x, y
            for step in range(1, steps + 1):
                t = step / steps; u = 1 - t
                points.append((u**3*ox + 3*u*u*t*x1 + 3*u*t*t*x2 + t**3*x3,
                               u**3*oy + 3*u*u*t*y1 + 3*u*t*t*y2 + t**3*y3))
            x, y = x3, y3
        else:
            raise ValueError(f"Unsupported SVG path command: {command}")
    if points: contours.append(points)
    return contours


def get_glyph(svg: Path):
    root = ET.parse(svg).getroot()
    vb = [float(x) for x in root.attrib.get("viewBox", "0 0 24 24").split()]
    paths = []
    for node in root.iter("{http://www.w3.org/2000/svg}path"):
        paths.extend(flatten_path(node.attrib["d"]))
    return vb, paths


def make_sheet(name: str, files: list[Path], destination: Path, columns: int, cell: int = 144):
    scale = 3
    pad = 12
    title_h = 58
    rows = math.ceil(len(files) / columns)
    width = columns * cell + 2 * pad
    height = title_h + rows * cell + pad
    image = Image.new("RGB", (width * scale, height * scale), NAVY)
    draw = ImageDraw.Draw(image)
    try:
        font = ImageFont.truetype("segoeui.ttf", 12 * scale)
        title_font = ImageFont.truetype("segoeui.ttf", 19 * scale)
    except OSError:
        font, title_font = ImageFont.load_default(), ImageFont.load_default()
    draw.text((pad * scale, 12 * scale), name, font=title_font, fill=WHITE)
    draw.text(((width - pad) * scale, 20 * scale), f"{len(files)} SVG icons",
              font=font, fill=MUTED, anchor="rt")
    for index, svg in enumerate(files):
        col, row = index % columns, index // columns
        x, y = pad + col * cell, title_h + row * cell
        draw.rounded_rectangle((x*scale, y*scale, (x+cell-6)*scale, (y+cell-8)*scale),
                               radius=13*scale, fill=(28, 34, 46), outline=(47, 56, 71), width=scale)
        vb, contours = get_glyph(svg)
        vx, vy, vw, vh = vb
        side = 62 * scale
        factor = side / max(vw, vh)
        cx = (x + (cell-6)/2) * scale
        cy = (y + 43) * scale
        generated = ROOT / "icons" / f"{svg.stem}.png"
        if svg.stem in {"fingerprint", "animation", "blur_on"} and generated.is_file():
            with Image.open(generated) as source:
                glyph = source.convert("RGBA").resize((side, side), Image.Resampling.LANCZOS)
            tinted = Image.new("RGBA", glyph.size, WHITE + (255,))
            tinted.putalpha(glyph.getchannel("A"))
            glyph = tinted
            image.paste(glyph, (round(cx-side/2), round(cy-side/2)), glyph)
        else:
            for contour in contours:
                pts = [(cx + (px-vx-vw/2)*factor, cy + (py-vy-vh/2)*factor) for px, py in contour]
                if len(pts) >= 3:
                    draw.polygon(pts, fill=WHITE)
        label = svg.stem
        draw.text(((x+cell/2)*scale, (y+91)*scale), label,
                  font=font, fill=MUTED, anchor="mm")
    destination.parent.mkdir(parents=True, exist_ok=True)
    image.save(destination, optimize=True)


def main():
    settings = sorted((ASSETS / "settings").glob("*.svg"))
    music = sorted((ASSETS / "music").glob("*.svg"))
    shapes = sorted((ASSETS / "music/shapes").glob("*.svg"))
    shared = [ASSETS / "chevron.svg", ASSETS / "restart.svg"]
    groups = [("01-settings", settings, 8), ("02-music-controls", music, 5),
              ("03-music-shapes", shapes, 5), ("04-shared", shared, 2)]
    svgs = settings + music + shapes + shared
    if len(svgs) != 94:
        raise SystemExit(f"Expected 94 icons, found {len(svgs)}")

    OUT.mkdir(exist_ok=True)
    for title, files, columns in groups:
        make_sheet(title, files, OUT / f"ttcuz-icons-{title}.png", columns)
    make_sheet("TTCuz · complete icon set", svgs, OUT / "ttcuz-icons-all.png", 10, 136)

    readme = """# TTCuz icon set\n\nComplete set of 94 SVG icons: 72 settings icons, 15 music controls, 5 expressive music shapes, and 2 shared interface symbols. Settings and music controls use a consistent rounded filled-path style. The five expressive silhouettes retain their TTCuz shapes.\n\nThe fingerprint, animation, and split blur icons were created from generated image concepts and are included in `imagegen-concepts/`. The blur symbol keeps its left half crisp and applies blur to the right half.\n\nSource metadata: `assets/ui/settings/SOURCES.json`. Lucide source license: `licenses/LUCIDE-ISC-LICENSE.txt`. All SVG files are included under `assets/ui/`.\n"""
    package = OUT / "ttcuz-rounded-icons-2026-10-18.zip"
    metadata = ASSETS / "settings/SOURCES.json"
    license_file = ROOT / "licenses/LUCIDE-ISC-LICENSE.txt"
    with zipfile.ZipFile(package, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        archive.writestr("README.md", readme)
        archive.write(metadata, "assets/ui/settings/SOURCES.json")
        if license_file.exists():
            archive.write(license_file, "licenses/LUCIDE-ISC-LICENSE.txt")
        for name in ("fingerprint", "animation", "blur_on"):
            source = OUT / "imagegen-icon-concepts" / f"{name}.png"
            if source.is_file():
                archive.write(source, f"imagegen-concepts/{name}.png")
        for svg in svgs:
            archive.write(svg, "assets/ui/" + svg.relative_to(ASSETS).as_posix())
    print(json.dumps({"icons": len(svgs), "package": str(package), "sheets": [str(OUT / f"ttcuz-icons-{title}.png") for title, _, _ in groups] + [str(OUT / "ttcuz-icons-all.png")]}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
