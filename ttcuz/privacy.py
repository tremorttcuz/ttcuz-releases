"""Verified TikTok 47.2.41 senders. No blanket network or status-reader patch."""

import re
from pathlib import Path

PRIVACY = "Lcat/ttcuz/framework/Privacy;"
STRINGS = "Lcat/ttcuz/framework/NotificationStrings;"
STORY = "Lcom/ss/android/ugc/aweme/story/api/StoryApi;"
READ = "Lcom/ss/android/ugc/aweme/im/readstate/ReadStateMarkDelegate;"
FETCH = "LX/12gZ;->LIZ(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;LX/01K8;)Ljava/lang/Object;"

# Returns match what each caller already expects (Unit, List, Single<BaseResponse>).
GUARDS = {
    "LX/1KSq;": (
        "invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;",
        "hideOnline",
        "    sget-object v0, Lkotlin/Unit;->LIZ:Lkotlin/Unit;\n    return-object v0\n",
    ),
    "LX/12gU;": (
        "LIZIZ(Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/String;LX/01K8;)Ljava/lang/Object;",
        "hideOnline",
        "    invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;\n    move-result-object v0\n    return-object v0\n",
    ),
    "LX/06W5;": ("LJIL(Ljava/lang/Long;)V", "hideRead", "    return-void\n"),
    READ: ("LJFF()V", "hideRead", "    return-void\n"),
    STORY: (
        "reportStoryViewed(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;)LX/19X3;",
        "hideStories",
        "    new-instance v0, Ljava/util/concurrent/CancellationException;\n    invoke-direct {v0}, Ljava/util/concurrent/CancellationException;-><init>()V\n    invoke-static {v0}, LX/19X3;->LJIILLIIL(Ljava/lang/Throwable;)LX/19Ud;\n    move-result-object v0\n    return-object v0\n",
    ),
}
OWNERS = tuple(GUARDS) + ("LX/0TFu;", "LX/12gZ;")
TARGETS = [PRIVACY + "->" + name + "()Z" for name in ("hideOnline", "hideStories", "hideRead")]
TARGETS += [
    PRIVACY + "->fetchAsReport(Ljava/lang/Integer;)Ljava/lang/Integer;",
    STRINGS + "->getString(Landroid/content/Context;I)Ljava/lang/String;",
]
REQUIRED = {
    "privacy hideOnline": 2,
    "privacy hideStories": 1,
    "privacy hideRead": 2,
    "privacy fetch without presence": 1,
    "IM notification missing strings": 1,
}


def _entry(body):
    """Keep annotations/parameter metadata before the inserted instructions."""
    depth = 0
    offset = 0
    for row in body.splitlines(keepends=True):
        word = row.strip()
        if word.startswith(".annotation"):
            depth += 1
        elif word.startswith(".end annotation"):
            depth -= 1
        elif not depth and word and not word.startswith((".", "#")):
            return offset
        offset += len(row)
    raise ValueError("Native privacy method has no instructions")


def _guard(text, signature, option, answer):
    pattern = re.compile(
        r"(?m)^\.method ([^\n]*?" + re.escape(signature) + r")\r?\n(.*?)^\.end method", re.S
    )
    hits = 0

    def insert(match):
        nonlocal hits
        head, body = match.groups()
        if " abstract " in " " + head + " " or PRIVACY in body:
            return match.group(0)
        registers = re.search(r"\.(registers|locals) (\d+)", body)
        if not registers:
            raise ValueError("Native privacy method has no registers: " + signature)
        params = signature[signature.index("(") + 1 : signature.index(")")]
        width = sum(
            2 if p in ("J", "D") else 1
            for p in re.findall(r"\[*L[^;]+;|\[+[ZBCSIJFD]|[ZBCSIJFD]", params)
        )
        width += 0 if "static " in head else 1
        locals_count = int(registers[2]) - (width if registers[1] == "registers" else 0)
        if locals_count < 1:
            raise ValueError("Native privacy method needs a spare local: " + signature)
        at = _entry(body)
        gate = (
            "    invoke-static {}, "
            + PRIVACY
            + "->"
            + option
            + "()Z\n    move-result v0\n    if-eqz v0, :ttcuz_privacy_native\n"
            + answer
            + "    :ttcuz_privacy_native\n"
        )
        hits += 1
        return ".method " + head + "\n" + body[:at] + gate + body[at:] + ".end method"

    return pattern.sub(insert, text), hits


def rewrite(root):
    counts = {}
    for file in Path(root).rglob("*.smali"):
        text = file.read_text(encoding="utf-8")
        owner = re.search(r"(?m)^\.class [^\n]* (L[^\s]+;)", text)
        if not owner:
            continue
        owner = owner[1]
        changed = text
        if owner in GUARDS:
            signature, option, answer = GUARDS[owner]
            changed, hits = _guard(changed, signature, option, answer)
            if hits:
                label = "privacy " + option
                counts[label] = counts.get(label, 0) + hits
        if owner == "LX/0TFu;":
            changed, hits = re.subn(
                r"invoke-virtual(/range)? (\{[^}]+\}), Landroid/content/Context;->getString\(I\)Ljava/lang/String;",
                lambda m: "invoke-static"
                + (m[1] or "")
                + " "
                + m[2]
                + ", "
                + STRINGS
                + "->getString(Landroid/content/Context;I)Ljava/lang/String;",
                changed,
            )
            if hits:
                counts["IM notification missing strings"] = hits
        # This exact Integer argument enables publishing our own presence on fetch.
        pattern = re.compile(
            r"(?m)^(\s*)invoke-interface(/range)? \{([^}]+)\}, " + re.escape(FETCH)
        )

        def fetch(match):
            registers = match[3]
            if match[2]:
                first, last = re.fullmatch(r"([vp]\d+) \.\. ([vp]\d+)", registers).groups()
                if first[0] != last[0] or int(last[1:]) - int(first[1:]) != 4:
                    raise ValueError("Unexpected native status fetch registers")
                arg = first[0] + str(int(first[1:]) + 3)
            else:
                args = [a.strip() for a in registers.split(",")]
                if len(args) != 5:
                    raise ValueError("Unexpected native status fetch arguments")
                arg = args[3]
            gate = (
                "invoke-static/range {"
                + arg
                + " .. "
                + arg
                + "}, "
                + PRIVACY
                + "->fetchAsReport(Ljava/lang/Integer;)Ljava/lang/Integer;"
            )
            # Existing patched calls already have their argument transform.
            if gate in changed[max(0, match.start() - 350) : match.start()]:
                return match.group(0)
            counts["privacy fetch without presence"] = (
                counts.get("privacy fetch without presence", 0) + 1
            )
            return match[1] + gate + "\n    move-result-object " + arg + "\n" + match.group(0)

        changed = pattern.sub(fetch, changed)
        if changed != text:
            file.write_text(changed, encoding="utf-8")
    return counts
