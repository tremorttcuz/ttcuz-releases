"""Comment-panel lifecycle hooks verified on the official TikTok 47.2.41 APK."""

import re
from pathlib import Path
from .privacy import _entry

OWNER = (
    "Lcom/ss/android/ugc/aweme/commentv2/commentpage/commonvideo/BaseCommentPageContainerFragment;"
)
JAVA = "Lcat/ttcuz/framework/CommentBackground;"
CALLS = {
    "onViewCreated(Landroid/view/View;Landroid/os/Bundle;)V": (
        "bind(Landroid/view/View;)V",
        "p1 .. p1",
    ),
    "onPause()V": ("pause(Ljava/lang/Object;)V", "p0 .. p0"),
    "onResume()V": ("resume(Ljava/lang/Object;)V", "p0 .. p0"),
    "onDestroyView()V": ("destroy(Ljava/lang/Object;)V", "p0 .. p0"),
}
TARGETS = [JAVA + "->" + call for call, _ in CALLS.values()]
REQUIRED = {"comment background " + call.split("(")[0]: 1 for call, _ in CALLS.values()}


def rewrite(root):
    counts = {}
    for file in Path(root).rglob("*.smali"):
        text = file.read_text(encoding="utf-8")
        if not re.search(r"(?m)^\.class [^\n]* " + re.escape(OWNER) + r"$", text):
            continue
        changed = text
        for signature, (call, registers) in CALLS.items():
            pattern = re.compile(
                r"(?m)^\.method ([^\n]*?" + re.escape(signature) + r")\r?\n(.*?)^\.end method", re.S
            )

            def insert(match):
                head, body = match.groups()
                target = JAVA + "->" + call
                if target in body:
                    return match[0]
                at = _entry(body)
                invoke = (
                    "    invoke-static"
                    + ("/range" if registers else "")
                    + " {"
                    + registers
                    + "}, "
                    + target
                    + "\n"
                )
                counts["comment background " + call.split("(")[0]] = 1
                return ".method " + head + "\n" + body[:at] + invoke + body[at:] + ".end method"

            changed = pattern.sub(insert, changed)
        if changed != text:
            file.write_text(changed, encoding="utf-8")
    return counts
