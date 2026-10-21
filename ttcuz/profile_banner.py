"""Use TikTok's existing inline banner player and its pause/resume lifecycle."""

import re
from pathlib import Path
from .privacy import _entry

OWNER = "Lcom/ss/android/ugc/profile/platform/base/data/ProfileBgVideoInfo;"
TARGET = "Lcat/ttcuz/framework/Flags;->isOn(I)Z"
REQUIRED = {"profile banner autoplay": 1}


def rewrite(root):
    counts = {}
    for file in Path(root).rglob("*.smali"):
        text = file.read_text(encoding="utf-8")
        if not re.search(r"(?m)^\.class [^\n]* " + re.escape(OWNER) + r"$", text):
            continue

        def insert(match):
            body = match[2]
            if TARGET in body:
                return match[0]
            registers = re.search(r"\.(registers|locals) (\d+)", body)
            if not registers or int(registers[2]) < (2 if registers[1] == "registers" else 1):
                raise ValueError("Banner autoplay getter has no spare local")
            at = _entry(body)
            gate = (
                "    const/4 v0, 0x2\n"
                "    invoke-static {v0}, " + TARGET + "\n"
                "    move-result v0\n"
                "    if-eqz v0, :ttcuz_banner_native\n"
                "    const/4 v0, 0x1\n"
                "    return v0\n"
                "    :ttcuz_banner_native\n"
            )
            counts["profile banner autoplay"] = 1
            return ".method " + match[1] + "\n" + body[:at] + gate + body[at:] + ".end method"

        changed = re.sub(
            r"(?m)^\.method ([^\n]*getAutoPlay\(\)Z)\r?\n(.*?)^\.end method",
            insert,
            text,
            flags=re.S,
        )
        if changed != text:
            file.write_text(changed, encoding="utf-8")
    return counts
