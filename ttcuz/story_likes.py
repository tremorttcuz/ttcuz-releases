"""Long-press hook for the immersive Story heart; ordinary feed hearts reuse FeedRail."""

import re
from pathlib import Path

OWNER = "Lcom/ss/android/ugc/aweme/story/feed/immersive/digg/ImmersiveStoryDiggAssem;"
TARGET = "Lcat/ttcuz/framework/StoryLikes;->bind(Ljava/lang/Object;)V"
REQUIRED = {"story likes hold": 1}


def rewrite(root):
    counts = {}
    for file in Path(root).rglob("*.smali"):
        text = file.read_text(encoding="utf-8")
        if not re.search(r"(?m)^\.class [^\n]* " + re.escape(OWNER) + r"$", text):
            continue
        method = re.compile(
            r"(?m)^\.method ([^\n]*onViewCreated\(Landroid/view/View;\)V)\r?\n(.*?)^\.end method",
            re.S,
        )

        def bind(match):
            if TARGET in match[2]:
                return match[0]
            body, hits = re.subn(
                r"(?m)^(\s*)return-void\s*$",
                "    invoke-static/range {p0 .. p0}, " + TARGET + "\n    return-void",
                match[2],
            )
            if not hits:
                raise ValueError("Native story heart has no normal return")
            counts["story likes hold"] = 1
            return ".method " + match[1] + "\n" + body + ".end method"

        changed = method.sub(bind, text)
        if changed != text:
            file.write_text(changed, encoding="utf-8")
    return counts
