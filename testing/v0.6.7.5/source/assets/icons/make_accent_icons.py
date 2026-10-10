"""Builds the 'accent' launcher icon family from assets/icons/art/source.png.

One visible icon (`accent`) and 13 hidden hue variants (`accent_h00`..`h11`,
`accent_gray`). At runtime Launcher switches to the variant nearest the accent.
Colour formula must match Launcher.tone() in Java:
  bg = HSL(h, .50*k, .37), container = HSL(h, .78*k, .83), glyph = bg
  k = 1 normally, 0.12 for the grey variant.
"""
import colorsys, json, os
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "art", "source.png")


def layers():
    im = np.array(Image.open(SRC).convert("RGB")).astype(float)
    bg, fg = im[600, 60], im[300, 610]
    v = fg - bg
    t = np.clip(((im - bg) @ v) / (v @ v), 0, 1)
    dark = t < 0.5
    lab, _ = ndi.label(dark)
    outer = lab == lab[5, 5]
    inner = dark & ~outer
    shape = ndi.gaussian_filter((~outer).astype(float), 0.8)
    glyph = ndi.gaussian_filter(inner.astype(float), 0.8)
    # crop the square around the shape, so layers are centred
    ys, xs = np.where(shape > 0.5)
    cx, cy = (xs.min() + xs.max()) / 2, (ys.min() + ys.max()) / 2
    half = max(xs.max() - xs.min(), ys.max() - ys.min()) / 2 / 0.74  # shape = 74% of the 1024 icon
    box = (int(cx - half), int(cy - half), int(cx + half), int(cy + half))

    def img(a):
        o = np.zeros(a.shape + (4,), np.uint8)
        o[..., :3] = 255
        o[..., 3] = (a * 255).astype(np.uint8)
        return Image.fromarray(o).crop(box)
    return img(shape), img(glyph)


def hsl(h, s, l):
    r, g, b = colorsys.hls_to_rgb((h % 360) / 360.0, l, s)
    return (round(r * 255), round(g * 255), round(b * 255))


def tone(h, k):
    bg = hsl(h, .50 * k, .37)
    return bg, hsl(h, .78 * k, .83)


def tint(layer, rgb):
    a = np.array(layer)
    a[..., 0], a[..., 1], a[..., 2] = rgb
    return Image.fromarray(a)


def build(shape, glyph, key, h, k):
    bg, cont = tone(h, k)
    out = os.path.join(HERE, key)
    os.makedirs(out, exist_ok=True)
    # full 1024 icon
    S, G = shape.resize((1024, 1024), Image.LANCZOS), glyph.resize((1024, 1024), Image.LANCZOS)
    icon = Image.new("RGBA", (1024, 1024), bg + (255,))
    icon.alpha_composite(tint(S, cont)); icon.alpha_composite(tint(G, bg))
    icon.convert("RGB").save(os.path.join(out, "icon.png"), optimize=True)
    # adaptive foreground: shape inside the 66% safe zone of a 432 canvas
    def small(l, px):
        c = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
        r = l.resize((px, px), Image.LANCZOS)
        c.alpha_composite(r, ((432 - px) // 2, (432 - px) // 2))
        return c
    px = int(432 * 0.60 / 0.74)
    fs, fg = small(shape, px), small(glyph, px)
    fore = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
    fore.alpha_composite(tint(fs, cont)); fore.alpha_composite(tint(fg, bg))
    fore.save(os.path.join(out, "foreground.png"), optimize=True)
    m = np.array(fs); g = np.array(fg)
    m[..., 3] = np.clip(m[..., 3].astype(int) - g[..., 3].astype(int), 0, 255).astype(np.uint8)
    Image.fromarray(m).save(os.path.join(out, "monochrome.png"), optimize=True)
    return "#%02X%02X%02X" % bg


def main():
    shape, glyph = layers()
    for name, l in (("layer_shape", shape), ("layer_glyph", glyph)):
        l.resize((256, 256), Image.LANCZOS).save(os.path.join(HERE, "art", name + ".png"), optimize=True)
    entries = []
    variants = [("accent_h%02d" % i, i * 30, 1.0) for i in range(12)] + [("accent_gray", 0, 0.12)]
    for key, h, k in variants:
        bgc = build(shape, glyph, key, h, k)
        entries.append(dict(id=key, label="Accent", default=False, auto=True,
                            hue=(-1 if key == "accent_gray" else h), key=key, bg=bgc))
    bgc = build(shape, glyph, "accent", 270, 1.0)
    entries.insert(0, dict(id="accent", label="Accent", default=False, auto=False, hue=270, key="accent", bg=bgc))

    path = os.path.join(HERE, "manifest.json")
    data = json.load(open(path, encoding="utf-8"))
    data["icons"] = [i for i in data["icons"] if not i["id"].startswith("accent")]
    for e in entries:
        k = e["key"]
        d = {"id": e["id"], "label": e["label"], "default": False}
        if e["auto"]:
            d["auto"] = True
        d["hue"] = e["hue"]
        d.update({"icon": k + "/icon.png",
                  "adaptive": {"foreground": k + "/foreground.png", "foregroundFullBleed": True,
                               "monochrome": k + "/monochrome.png", "backgroundColor": e["bg"]}})
        data["icons"].append(d)
    json.dump(data, open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    open(path, "a").write("\n")


if __name__ == "__main__":
    main()
