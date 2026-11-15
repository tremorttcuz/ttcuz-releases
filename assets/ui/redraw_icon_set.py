"""Refresh TTCuz's SVG symbols from a pinned Lucide revision."""
from __future__ import annotations

import concurrent.futures
import json
import math
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SNAPSHOT = ROOT / "outputs/ttcuz-source-v0.6.8.5-privacy"
REVISION = "1ef60af944de207e0d5176905a0759ba12e6ee8d"
BASE = f"https://raw.githubusercontent.com/lucide-icons/lucide/{REVISION}/icons/"

SETTINGS = {
    "account_circle": "user-round", "add": "plus", "animation": None,
    "arrow_forward": "arrow-right", "article": "file-text", "block": "ban",
    "bug_report": "bug", "chat": "message-circle", "chevron_right": "chevron-right",
    "cloud_upload": "cloud-upload", "contrast": "contrast", "delete": "trash",
    "download": "download", "edit": "pencil", "expand_more": "chevron-down",
    "extension": "puzzle", "eye_closed": "eye-closed", "eye_open": "eye",
    "fast_forward": "fast-forward", "fast_rewind": "rewind", "favorite_border": "heart",
    "fingerprint": None, "flare": "sparkles", "format_color_fill": "paint-bucket",
    "group": "users-round", "hd": None, "image": "image", "info": "info", "language": "languages",
    "link": "link", "lock": "lock", "mic": "mic", "music_note": None,
    "notifications_off": "bell-off", "notifications": "bell", "open_with": "move",
    "palette": "palette", "people": "users", "place": "map-pin", "play_arrow": "play",
    "play_circle": "circle-play", "public": "globe", "refresh": None,
    "remove": "circle-minus", "repeat": "repeat", "restore": None,
    "schedule": "clock-3", "school": "graduation-cap", "sd": None, "search": "search",
    "settings": "settings-2", "share": "share-2", "speed": "gauge", "star": "star",
    "swap_horiz": "arrow-left-right", "swap_vert": "arrow-up-down", "tag": "tag",
    "text_fields": "type", "timeline": "chart-gantt", "tune": "sliders-horizontal",
    "update": None, "upload": "upload", "verified": "badge-check",
    "view_compact": "panels-top-left", "visibility_off": "eye-off", "visibility": "eye",
    "volume_up": "volume-2", "wallpaper": None, "workspace_premium": "award",
    "video_library": None, "aspect_ratio": None, "blur_on": None,
}
MUSIC = {
    "add": "plus", "close": "x", "down": "chevron-down", "file": "file",
    "next": "skip-forward", "note": None, "pause": "pause", "play": "play",
    "prev": "skip-back", "queue": "list-music", "repeat": "repeat",
    "repeat_one": "repeat-1", "restore": "rotate-ccw", "settings": "settings-2",
    "shuffle": "shuffle",
}
SINGLE = {"assets/ui/chevron.svg": "chevron-right", "assets/ui/restart.svg": "rotate-ccw"}
CUSTOM_SETTINGS = {"hd", "sd", "fingerprint", "music_note", "animation", "aspect_ratio",
                   "blur_on", "wallpaper", "video_library", "refresh", "update", "restore"}
TOKEN = re.compile(r"[A-Za-z]|[-+]?(?:\d*\.\d+|\d+\.?\d*)(?:[eE][-+]?\d+)?")
SVG_NS = "{http://www.w3.org/2000/svg}"


def _cubic(p0, p1, p2, p3, n=8):
    out = []
    for step in range(1, n + 1):
        t = step / n
        u = 1 - t
        out.append((u**3*p0[0] + 3*u*u*t*p1[0] + 3*u*t*t*p2[0] + t**3*p3[0],
                    u**3*p0[1] + 3*u*u*t*p1[1] + 3*u*t*t*p2[1] + t**3*p3[1]))
    return out


def _quadratic(p0, p1, p2, n=8):
    return [((1-t)**2*p0[0] + 2*(1-t)*t*p1[0] + t*t*p2[0],
             (1-t)**2*p0[1] + 2*(1-t)*t*p1[1] + t*t*p2[1]) for t in (step/n for step in range(1, n+1))]


def _arc(start, rx, ry, rotation, large, sweep, end):
    rx, ry = abs(rx), abs(ry)
    if rx == 0 or ry == 0 or start == end:
        return [end]
    phi = math.radians(rotation % 360)
    cp, sp = math.cos(phi), math.sin(phi)
    dx, dy = (start[0]-end[0])/2, (start[1]-end[1])/2
    xp, yp = cp*dx + sp*dy, -sp*dx + cp*dy
    scale = xp*xp/(rx*rx) + yp*yp/(ry*ry)
    if scale > 1:
        rx, ry = rx*math.sqrt(scale), ry*math.sqrt(scale)
    den = rx*rx*yp*yp + ry*ry*xp*xp
    factor = 0 if den == 0 else math.sqrt(max(0, (rx*rx*ry*ry-den)/den))
    if bool(large) == bool(sweep):
        factor = -factor
    cxp, cyp = factor*rx*yp/ry, factor*-ry*xp/rx
    cx, cy = cp*cxp-sp*cyp+(start[0]+end[0])/2, sp*cxp+cp*cyp+(start[1]+end[1])/2
    a = ((xp-cxp)/rx, (yp-cyp)/ry)
    b = ((-xp-cxp)/rx, (-yp-cyp)/ry)
    angle = lambda u, v: math.atan2(u[0]*v[1]-u[1]*v[0], u[0]*v[0]+u[1]*v[1])
    begin, delta = angle((1, 0), a), angle(a, b)
    if not sweep and delta > 0: delta -= 2*math.pi
    if sweep and delta < 0: delta += 2*math.pi
    count = max(2, math.ceil(abs(delta) / (math.pi/16)))
    return [(cx+rx*cp*math.cos(begin+delta*i/count)-ry*sp*math.sin(begin+delta*i/count),
             cy+rx*sp*math.cos(begin+delta*i/count)+ry*cp*math.sin(begin+delta*i/count))
            for i in range(1, count+1)]


def path_contours(data):
    tokens = TOKEN.findall(data)
    counts = {"M": 2, "L": 2, "H": 1, "V": 1, "C": 6, "S": 4, "Q": 4, "T": 2, "A": 7}
    contours, points, x, y, sx, sy = [], [], 0.0, 0.0, 0.0, 0.0
    command, last_cubic, last_quad = None, None, None
    i = 0
    while i < len(tokens):
        if tokens[i].isalpha():
            command = tokens[i]
            i += 1
        if command is None:
            raise ValueError("SVG path has values without a command")
        upper, rel = command.upper(), command.islower()
        if upper == "Z":
            if points and points[-1] != (sx, sy): points.append((sx, sy))
            if points: contours.append(points)
            points, x, y, last_cubic, last_quad = [], sx, sy, None, None
            command = None
            continue
        size = counts[upper]
        first = True
        while i < len(tokens) and not tokens[i].isalpha():
            v = [float(tokens[i+j]) for j in range(size)]
            i += size
            ox, oy = x, y
            if upper == "M":
                x, y = ((x+v[0], y+v[1]) if rel else (v[0], v[1]))
                if points: contours.append(points)
                points, sx, sy = [(x, y)], x, y
                upper = "L"
                size = 2
            elif upper == "L": x, y = ((x+v[0], y+v[1]) if rel else (v[0], v[1])); points.append((x, y))
            elif upper == "H": x = x+v[0] if rel else v[0]; points.append((x, y))
            elif upper == "V": y = y+v[0] if rel else v[0]; points.append((x, y))
            elif upper in ("C", "S"):
                if upper == "C":
                    p1 = (ox+v[0], oy+v[1]) if rel else (v[0], v[1])
                    p2 = (ox+v[2], oy+v[3]) if rel else (v[2], v[3])
                    p3 = (ox+v[4], oy+v[5]) if rel else (v[4], v[5])
                else:
                    p1 = (2*ox-last_cubic[0], 2*oy-last_cubic[1]) if last_cubic else (ox, oy)
                    p2 = (ox+v[0], oy+v[1]) if rel else (v[0], v[1])
                    p3 = (ox+v[2], oy+v[3]) if rel else (v[2], v[3])
                points.extend(_cubic((ox, oy), p1, p2, p3)); x, y = p3; last_cubic, last_quad = p2, None
            elif upper in ("Q", "T"):
                p1 = ((2*ox-last_quad[0], 2*oy-last_quad[1]) if last_quad else (ox, oy)) if upper == "T" else ((ox+v[0], oy+v[1]) if rel else (v[0], v[1]))
                p2 = ((ox+v[0], oy+v[1]) if rel else (v[0], v[1])) if upper == "T" else ((ox+v[2], oy+v[3]) if rel else (v[2], v[3]))
                points.extend(_quadratic((ox, oy), p1, p2)); x, y = p2; last_quad, last_cubic = p1, None
            else:
                p = (ox+v[5], oy+v[6]) if rel else (v[5], v[6])
                points.extend(_arc((ox, oy), *v[:5], p)); x, y = p; last_cubic = last_quad = None
            first = False
        if first:
            raise ValueError(f"SVG path command {command} is missing coordinates")
        if upper not in ("C", "S"): last_cubic = None
        if upper not in ("Q", "T"): last_quad = None
        if command in "Mm": command = "l" if rel else "L"
    if points: contours.append(points)
    return contours


def shape_contours(element):
    tag = element.tag.removeprefix(SVG_NS)
    a = element.attrib
    if tag == "path": return path_contours(a["d"])
    if tag == "line": return [[(float(a["x1"]), float(a["y1"])), (float(a["x2"]), float(a["y2"]))]]
    if tag in ("polyline", "polygon"):
        values = [float(v) for v in re.findall(r"[-+]?(?:\d*\.\d+|\d+\.?\d*)", a["points"])]
        pts = list(zip(values[::2], values[1::2]))
        if tag == "polygon" and pts: pts.append(pts[0])
        return [pts]
    if tag == "circle":
        cx, cy, r = float(a.get("cx", 0)), float(a.get("cy", 0)), float(a["r"])
        return [[(cx+r*math.cos(t*math.tau/48), cy+r*math.sin(t*math.tau/48)) for t in range(49)]]
    if tag == "ellipse":
        cx, cy, rx, ry = float(a.get("cx", 0)), float(a.get("cy", 0)), float(a["rx"]), float(a["ry"])
        return [[(cx+rx*math.cos(t*math.tau/48), cy+ry*math.sin(t*math.tau/48)) for t in range(49)]]
    if tag == "rect":
        x, y, w, h = (float(a.get(k, 0)) for k in ("x", "y", "width", "height"))
        rx, ry = min(float(a.get("rx", 0)), w/2), min(float(a.get("ry", a.get("rx", 0))), h/2)
        if not rx or not ry: return [[(x,y),(x+w,y),(x+w,y+h),(x,y+h),(x,y)]]
        pts = []
        for cx, cy, start in ((x+w-rx,y+ry,-90),(x+w-rx,y+h-ry,0),(x+rx,y+h-ry,90),(x+rx,y+ry,180)):
            for j in range(5):
                angle = math.radians(start + 90*j/4)
                pts.append((cx+rx*math.cos(angle), cy+ry*math.sin(angle)))
        pts.append(pts[0])
        return [pts]
    return []


def converted_icon(source, icon_type):
    from make_settings_icons import Icon, icons as drawn_icons
    if icon_type in drawn_icons:
        d = drawn_icons[icon_type]
        return f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><path fill="currentColor" d="{d}"/></svg>\n', d
    icon = Icon()
    if icon_type not in CUSTOM_SETTINGS:
        root = ET.fromstring(source)
        width = float(root.attrib.get("stroke-width", 2)) / 2
        for node in root.iter():
            if node is root: continue
            if node.tag.removeprefix(SVG_NS) not in {"path", "line", "polyline", "polygon", "circle", "ellipse", "rect"}: continue
            if node.attrib.get("stroke", root.attrib.get("stroke", "currentColor")) == "none": continue
            for contour in shape_contours(node):
                if len(contour) > 1: icon.line(*contour, r=width)
                elif contour: icon.disc(contour[0], width)
    if icon_type == "hd" or icon_type == "sd":
        icon = Icon()
        i = icon
        if icon_type == "hd":
            i.line((3,6),(3,18)).line((3,12),(9,12)).line((9,6),(9,18))
            i.line((13,6),(13,18)).arc((13,12),6,90,-90,step=10,ry=6)
        else:
            i.line((9,7),(7,6),(4,6),(3,8),(3,10),(8,13),(9,15),(8,18),(4,18),(3,17))
            i.line((14,6),(14,18)).arc((14,12),6,90,-90,step=10,ry=6)
    elif icon_type == "fingerprint":
        icon.arc((12, 13), 9, 218, 322, step=10)
        icon.arc((12, 13), 6.8, 206, 334, step=10)
        icon.arc((12, 13), 4.7, 194, 346, step=10)
        icon.line((12, 20), (12, 16), (10.7, 13.7), (10.9, 11.1), (12.5, 9.8), (14.1, 10.5), (14.8, 12.4), (14.3, 15.2))
    elif icon_type == "music_note":
        icon.line((9, 16), (9, 7), (17, 5), (17, 14))
        icon.disc((7, 17), 2)
        icon.disc((15, 15), 2)
    d = icon.d()
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><path fill="currentColor" d="{d}"/></svg>\n', d


def fetch(name):
    request = urllib.request.Request(BASE + name + ".svg", headers={"User-Agent": "TTCuz-icon-refresh"})
    try:
        with urllib.request.urlopen(request, timeout=25) as response:
            return name, response.read()
    except urllib.error.HTTPError as error:
        raise RuntimeError(f"Lucide icon is unavailable: {name} ({error.code})") from error


def refresh():
    sys.path.insert(0, str(ROOT / "assets/ui"))
    sources = {**{k:v for k,v in SETTINGS.items() if v}, **{f"music/{k}": v for k, v in MUSIC.items() if v},
               "ui/chevron": "chevron-right", "ui/restart": "rotate-ccw"}
    fetched = {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=12) as pool:
        futures = [pool.submit(fetch, name) for name in sorted(set(sources.values()))]
        for future in concurrent.futures.as_completed(futures):
            name, body = future.result()
            fetched[name] = body

    settings_paths = {}
    for key, name in SETTINGS.items():
        text, d = converted_icon(fetched.get(name, b""), key if key in CUSTOM_SETTINGS else "")
        (ROOT / "assets/ui/settings" / f"{key}.svg").write_text(text, encoding="utf-8")
        settings_paths[key] = d
    settings_paths["favorite"] = settings_paths["favorite_border"]

    music_paths = {}
    for key, name in MUSIC.items():
        text, d = converted_icon(fetched.get(name, b""), "music_note" if key == "note" else "")
        (ROOT / "assets/ui/music" / f"{key}.svg").write_text(text, encoding="utf-8")
        music_paths[f"m_{key}"] = settings_paths["music_note"] if key == "note" else d
    shared_paths = {}
    for relative, name in SINGLE.items():
        text, d = converted_icon(fetched[name], "")
        (ROOT / relative).write_text(text, encoding="utf-8")
        shared_paths[Path(relative).stem] = d

    for folder in (ROOT / "inject/java/cat/ttcuz/framework",
                   SNAPSHOT / "inject/java/cat/ttcuz/framework"):
        path = folder / "SettingsGlyph.java"
        current = path.read_text(encoding="utf-8")
        if "paths.put(" not in current:
            path = folder / "SettingsGlyphPaths.java"
            current = path.read_text(encoding="utf-8")
        if current.count("paths.put(") < 60:
            raise RuntimeError(f"Could not find the complete icon map in {path}")
        block = re.search(r"(?m)^(\s*)static \{\n", current)
        if block is None: raise RuntimeError(f"Could not find the static icon map in {path}")
        closing = re.search(r"(?m)^\s*\}\s*$", current[block.end():])
        if closing is None: raise RuntimeError(f"Could not find the end of the icon map in {path}")
        indent = block.group(1) + "    "
        all_paths = {**settings_paths, **music_paths, **shared_paths}
        replacement = "\n".join(f'{indent}paths.put("{k}","{v}");' for k, v in sorted(all_paths.items()))
        close_start = block.end() + closing.start()
        close_end = block.end() + closing.end()
        current = current[:block.end()] + replacement + "\n" + current[close_start:]
        path.write_text(current, encoding="utf-8")

    snapshot_settings = SNAPSHOT / "assets/ui/settings"
    snapshot_settings.mkdir(parents=True, exist_ok=True)
    for path in (ROOT / "assets/ui/settings").glob("*.svg"):
        (snapshot_settings / path.name).write_bytes(path.read_bytes())
    snapshot_music = SNAPSHOT / "assets/ui/music"
    snapshot_music.mkdir(parents=True, exist_ok=True)
    for path in (ROOT / "assets/ui/music").glob("*.svg"):
        (snapshot_music / path.name).write_bytes(path.read_bytes())
    for relative in SINGLE:
        target = SNAPSHOT / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes((ROOT / relative).read_bytes())

    # Expressive music shapes are original TTCuz geometry rather than Lucide
    # glyphs, but they are part of the same visible icon set and source bundle.
    shape_sources = sorted((ROOT / "assets/ui/music/shapes").glob("*.svg"))
    snapshot_shapes = SNAPSHOT / "assets/ui/music/shapes"
    snapshot_shapes.mkdir(parents=True, exist_ok=True)
    for source in shape_sources:
        (snapshot_shapes / source.name).write_bytes(source.read_bytes())

    metadata = {
        "style": "TTCuz Rounded Line, 24px",
        "source": f"Lucide icons, pinned revision {REVISION}; converted to rounded filled paths for SettingsGlyph",
        "license": "ISC",
        "sources": {key: BASE + name + ".svg" for key, name in SETTINGS.items() if name},
        "music_sources": {key: BASE + name + ".svg" for key, name in MUSIC.items() if name},
        "shared_sources": {key: BASE + name + ".svg" for key, name in (("chevron", "chevron-right"), ("restart", "rotate-ccw"))},
        "custom": {"_note": "Original rounded symbols for HD, SD, fingerprint, animation, blur, aspect ratio, wallpaper, video, refresh, update, restore, and music note", "icons": sorted(CUSTOM_SETTINGS), "music_icons": ["note"]},
        "expressive_shapes": {"_note": "Original TTCuz music shape silhouettes, included in the complete icon set", "icons": [p.stem for p in shape_sources]},
        "unavailable": [],
    }
    for base in (ROOT / "assets/ui/settings", snapshot_settings):
        (base / "SOURCES.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    license_url = f"https://raw.githubusercontent.com/lucide-icons/lucide/{REVISION}/LICENSE"
    request = urllib.request.Request(license_url, headers={"User-Agent": "TTCuz-icon-refresh"})
    with urllib.request.urlopen(request, timeout=25) as response:
        license_text = response.read()
    for license_path in (ROOT / "licenses/LUCIDE-ISC-LICENSE.txt", SNAPSHOT / "licenses/LUCIDE-ISC-LICENSE.txt"):
        license_path.parent.mkdir(parents=True, exist_ok=True)
        license_path.write_bytes(license_text)
    print(f"Updated {len(SETTINGS)} settings icons, {len(MUSIC)} music controls, and 2 shared icons.")


if __name__ == "__main__":
    refresh()
