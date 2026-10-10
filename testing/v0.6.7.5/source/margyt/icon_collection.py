"""The ttcuz launcher icons supplied in assets/icons/manifest.json."""

from __future__ import annotations

import copy
import json
import os
from typing import List

from . import png
from .axml import Axml


def load(root: str) -> List[dict]:
    folder = os.path.join(root, "assets", "icons")
    with open(os.path.join(folder, "manifest.json"), encoding="utf-8") as stream:
        data = json.load(stream)
    if data.get("schemaVersion") != 1 or not isinstance(data.get("icons"), list):
        raise ValueError("unsupported icon manifest")
    icons = data["icons"]
    ids = set()
    for icon in icons:
        key = icon["id"]
        if not key.isascii() or not key.replace("_", "").isalnum() or key in ids:
            raise ValueError("invalid or duplicate icon id: %r" % key)
        ids.add(key)
        for relative, size in ((icon["icon"], 1024),
                               (icon["adaptive"]["foreground"], 432),
                               (icon["adaptive"]["monochrome"], 432)):
            if not relative.startswith(key + "/") or ".." in relative.split("/"):
                raise ValueError("icon path escapes its folder: %r" % relative)
            with open(os.path.join(folder, relative), "rb") as stream:
                image = png.decode(stream.read())
            if (image.width, image.height) != (size, size):
                raise ValueError("wrong size for %s" % relative)
        int(icon["adaptive"]["backgroundColor"].lstrip("#"), 16)
    if not 1 <= len(icons) <= 24 or sum(bool(x.get("default")) for x in icons) != 1:
        raise ValueError("icon manifest needs one default and at most 24 icons")
    return icons


def picture(root: str, icon: dict, part: str) -> bytes:
    relative = icon["icon"] if part == "icon" else icon["adaptive"][part]
    with open(os.path.join(root, "assets", "icons", relative), "rb") as stream:
        return stream.read()


def background(icon: dict) -> bytes:
    value = int(icon["adaptive"]["backgroundColor"].lstrip("#"), 16)
    pixel = bytearray(((value >> 16) & 255, (value >> 8) & 255,
                       value & 255, 255))
    return png.encode(png.Image(1, 1, pixel))


def adaptive_xml(template: bytes, background_id: int, foreground_id: int,
                 monochrome_id: int) -> bytes:
    xml = Axml.parse(template)
    for tag, resource in (("background", background_id),
                          ("foreground", foreground_id)):
        nodes = xml.elements(tag)
        if len(nodes) != 1:
            raise ValueError("adaptive icon template lacks %s" % tag)
        attr = xml.attr(nodes[0], "drawable")
        if attr is None:
            raise ValueError("adaptive icon %s lacks drawable" % tag)
        attr.data = resource
    monochrome = xml.elements("monochrome")
    if monochrome:
        attr = xml.attr(monochrome[0], "drawable")
        if attr is None:
            raise ValueError("adaptive icon monochrome lacks drawable")
        attr.data = monochrome_id
    else:
        foreground = xml.elements("foreground")[0]
        attr = copy.copy(xml.attr(foreground, "drawable"))
        attr.data = monochrome_id
        new = xml.make_element("monochrome")
        new.attributes.append(attr)
        root = xml.elements("adaptive-icon")[0]
        xml.insert_into(root, [new, xml.close_element(new)])
    return xml.build()
