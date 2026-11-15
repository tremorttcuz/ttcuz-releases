#!/usr/bin/env python3
"""Render a ttcuz banner from the supplied icon."""

import pathlib

from PIL import Image, ImageDraw, ImageFont

HERE = pathlib.Path(__file__).parent

W, H = 900, 260
MINT = (0, 0, 0)
INK = (255, 255, 255)
INK_SOFT = (200, 200, 200)
WHITE = (255, 255, 255)

BOLD = "C:/Windows/Fonts/segoeuib.ttf"
REGULAR = "C:/Windows/Fonts/segoeui.ttf"

WORDMARK = "ttcuz"
SUBTITLE = "TikTok mod for Android"


def main():
    im = Image.new("RGB", (W, H), MINT)
    d = ImageDraw.Draw(im)

    symbol = Image.open(HERE / "ttcuz-icon.png").convert("RGB")
    symbol.thumbnail((220, 220), Image.Resampling.LANCZOS)
    im.paste(symbol, (24, 20))

    wordmark = ImageFont.truetype(BOLD, 82)
    subtitle = ImageFont.truetype(REGULAR, 30)

    d.text((285, 74), WORDMARK, font=wordmark, fill=INK)
    d.text((288, 168), SUBTITLE, font=subtitle, fill=INK_SOFT)

    out = HERE / "banner.png"
    im.save(out)
    print("wrote", out, im.size)


if __name__ == "__main__":
    main()
