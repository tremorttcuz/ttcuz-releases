"""Messaging hooks pinned to the official TikTok 47.2.41 classes."""

import re
from pathlib import Path
from .privacy import _entry

CHAT = 'Lcom/ss/android/ugc/aweme/im/sdk/chat/ui/singleactivity/BaseChatRootFragment;'
MENU = 'Lft6/AbS21S0300000_11;'
FILTER = 'LX/0TD8;'
OWNERS = (CHAT, MENU, FILTER)
HOOKS = {
    CHAT: {
        'onResume()V': ('Dim', 'chatOpened(Ljava/lang/Object;)V', 'p0 .. p0', 'native chat enter'),
        'onStop()V': ('Dim', 'chatClosed(Ljava/lang/Object;)V', 'p0 .. p0', 'native chat leave'),
        'onDestroy()V': ('Dim', 'chatClosed(Ljava/lang/Object;)V', 'p0 .. p0', 'native chat leave'),
    },
}
TARGETS = list(dict.fromkeys(
    'Lcat/ttcuz/framework/' + owner + ';->' + call
    for methods in HOOKS.values() for owner, call, _, _ in methods.values()
))
TARGETS += [
    'Lcat/ttcuz/framework/InboxReadAll;->addMenu(Landroid/view/ViewGroup;)V',
    'Lcat/ttcuz/framework/InboxFilter;->bind(Ljava/lang/Object;Landroid/view/View;)V',
]
REQUIRED = {'native chat enter': 1, 'native chat leave': 2,
            'native inbox read-all menu': 1, 'native inbox filter fade': 1}


def rewrite(root):
    counts = {}
    for path in Path(root).rglob('*.smali'):
        text = path.read_text(encoding='utf8')
        owner = re.search(r'(?m)^\.class [^\n]* (L[^\s]+;)', text)
        if not owner or owner[1] not in OWNERS:
            continue
        before = text
        for method, (java, call, registers, label) in HOOKS.get(owner[1], {}).items():
            pattern = re.compile(r'(?ms)^\.method ([^\n]*?\b' + re.escape(method) + r')\r?\n(.*?)^\.end method')
            target = 'Lcat/ttcuz/framework/' + java + ';->' + call

            def inject(match):
                head, body = match.groups()
                if target in body:
                    return match[0]
                at = _entry(body)
                body = body[:at] + '    invoke-static/range {' + registers + '}, ' + target + '\n' + body[at:]
                counts[label] = counts.get(label, 0) + 1
                return '.method ' + head + '\n' + body + '.end method'

            text = pattern.sub(inject, text)
        if owner[1] == MENU:
            pattern = re.compile(r'(?ms)^\.method public static final LIZ\$1\(Lft6/AbS21S0300000_11;Landroid/view/View;\)V\n.*?^\.end method')

            def menu(match):
                body = match[0]
                call = 'Lcat/ttcuz/framework/InboxReadAll;->addMenu(Landroid/view/ViewGroup;)V'
                if call in body:
                    return body
                if ':cond_159\n' not in body or 'if-ne v0, v5, :cond_52' not in body:
                    return body
                # Even a single stock tool now opens the sheet with our additional action.
                body = body.replace('if-ne v0, v5, :cond_52', 'goto :cond_52', 1)
                body = body.replace('    :cond_159\n', '    :cond_159\n    invoke-static {v4}, ' + call + '\n', 1)
                counts['native inbox read-all menu'] = 1
                return body

            text = pattern.sub(menu, text)
        if owner[1] == FILTER:
            pattern = re.compile(r'(?ms)^\.method public constructor <init>\(Landroidx/constraintlayout/widget/ConstraintLayout;\)V\n.*?^\.end method')

            def fade(match):
                body = match[0]
                call = 'Lcat/ttcuz/framework/InboxFilter;->bind(Ljava/lang/Object;Landroid/view/View;)V'
                if call in body:
                    return body
                body, hits = re.subn(r'(?m)^    return-void$', '    invoke-static/range {p0 .. p1}, ' + call + '\n    return-void', body)
                if hits:
                    counts['native inbox filter fade'] = hits
                return body

            text = pattern.sub(fade, text)
        if text != before:
            path.write_text(text, encoding='utf8')
    return counts
