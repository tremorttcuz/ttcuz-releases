"""Fail closed when the official inbox signatures change; never add duplicate hooks."""
import tempfile
import unittest
from pathlib import Path
from ttcuz import inbox, dexpatch


class InboxHooksTest(unittest.TestCase):
    def test_hooks_and_idempotence(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            chat = '.class public ' + inbox.CHAT + '\n.super Landroidx/fragment/app/Fragment;\n'
            for method in ('onResume', 'onStop', 'onDestroy'):
                chat += f'.method public {method}()V\n    .registers 1\n    return-void\n.end method\n'
            (root / 'Chat.smali').write_text(chat)
            (root / 'Menu.smali').write_text('.class public ' + inbox.MENU + '''
.super Ljava/lang/Object;
.method public static final LIZ$1(Lft6/AbS21S0300000_11;Landroid/view/View;)V
    .registers 25
    if-ne v0, v5, :cond_52
    return-void
    :cond_52
    :cond_159
    return-void
.end method
''')
            (root / 'Filter.smali').write_text('.class public ' + inbox.FILTER + '''
.super Ljava/lang/Object;
.method public constructor <init>(Landroidx/constraintlayout/widget/ConstraintLayout;)V
    .registers 8
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method
''')
            self.assertEqual(inbox.REQUIRED, inbox.rewrite(root))
            before = {p.name: p.read_bytes() for p in root.iterdir()}
            self.assertEqual({}, inbox.rewrite(root))
            self.assertEqual(before, {p.name: p.read_bytes() for p in root.iterdir()})
            self.assertIn('goto :cond_52', (root / 'Menu.smali').read_text())
            self.assertTrue(set(inbox.TARGETS).issubset(set(dexpatch.rewrite_targets())))

    def test_other_menus_untouched(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'Other.smali'
            text = '.class public Lft6/Unrelated;\n.super Ljava/lang/Object;\n'
            path.write_text(text)
            self.assertEqual({}, inbox.rewrite(folder))
            self.assertEqual(text, path.read_text())
