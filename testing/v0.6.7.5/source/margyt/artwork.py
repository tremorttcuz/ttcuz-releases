"""Launcher artwork and a vector fallback traced from the supplied icon."""

from __future__ import annotations

import os

from . import png

BLACK = 0xFF000000
WHITE = 0xFFFFFFFF
MINT = BLACK  # compatibility with the existing icon patcher
VIEWPORT = 108
GLYPH_SCALE = 0.82
MASTER_PNG = "assets/icons/ttcuz/icon.png"
BACKGROUND = [(BLACK, "M0,0 h108 v108 h-108 z")]


def _glyph():
    path = os.path.join(os.path.dirname(os.path.dirname(__file__)), MASTER_PNG)
    with open(path, "rb") as stream:
        image = png.decode(stream.read()).resized(216)
    pieces = []
    step = VIEWPORT / image.width
    for y in range(image.height):
        left = None
        for x in range(image.width + 1):
            at = (y * image.width + x) * 4
            lit = x < image.width and min(image.pixels[at:at + 3]) >= 100 and image.pixels[at + 3] >= 128
            if lit and left is None:
                left = x
            elif not lit and left is not None:
                sx = VIEWPORT / 2 + (left * step - VIEWPORT / 2) * GLYPH_SCALE
                sy = VIEWPORT / 2 + (y * step - VIEWPORT / 2) * GLYPH_SCALE
                pieces.append("M%.2f,%.2f h%.2f v%.2f h-%.2f z" %
                              (sx, sy, (x - left) * step * GLYPH_SCALE,
                               step * GLYPH_SCALE, (x - left) * step * GLYPH_SCALE))
                left = None
    if not pieces:
        raise ValueError("ttcuz icon glyph could not be traced")
    return [(WHITE, " ".join(pieces))]


GLYPH = _glyph()
COMBINED = BACKGROUND + GLYPH
