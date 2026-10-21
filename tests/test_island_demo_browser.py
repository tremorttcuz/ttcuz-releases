"""Hold-and-drag of the navigation island indicator in the browser demo (skipped without Playwright/Chromium)."""

from pathlib import Path
import unittest

try:
    from playwright.sync_api import sync_playwright
except ImportError:  # pragma: no cover
    sync_playwright = None

DEMO = Path(__file__).resolve().parents[1] / "docs" / "demo" / "island-demo.html"
MEASURE = """(()=>{const p=document.getElementById('pill').getBoundingClientRect(),i=document.getElementById('island').getBoundingClientRect();
return {l:p.left-i.left,r:i.right-p.right,t:p.top-i.top,b:i.bottom-p.bottom,w:p.width,x:p.left+p.width/2,y:p.top+p.height/2}})()"""


@unittest.skipIf(sync_playwright is None, "Playwright is not installed")
class IslandDemoBrowserTest(unittest.TestCase):
    def test_hold_drag_release_cancel_and_edges(self):
        with sync_playwright() as p:
            try:
                browser = p.chromium.launch()
            except Exception as error:  # no browser binary available
                self.skipTest(str(error))
            ctx = browser.new_context(
                viewport={"width": 420, "height": 1000}, has_touch=True, is_mobile=True
            )
            page = ctx.new_page()
            errors = []
            page.on("pageerror", lambda e: errors.append(str(e)))
            page.goto(DEMO.as_uri())
            page.wait_for_timeout(300)
            cdp = ctx.new_cdp_session(page)

            def touch(kind, x=0, y=0):
                points = [] if kind in ("touchEnd", "touchCancel") else [{"x": x, "y": y}]
                cdp.send("Input.dispatchTouchEvent", {"type": kind, "touchPoints": points})

            def drag(start, dx, steps, hold=450):
                touch("touchStart", start["x"], start["y"])
                page.wait_for_timeout(hold)
                worst = 99
                for k in range(1, steps + 1):
                    touch("touchMove", start["x"] + dx * k / steps, start["y"])
                    page.wait_for_timeout(16)
                    m = page.evaluate(MEASURE)
                    worst = min(worst, m["l"], m["r"], m["t"], m["b"])
                page.wait_for_timeout(300)
                return worst

            home = page.evaluate(MEASURE)
            # A short swipe before the hold fires never lifts the indicator.
            touch("touchStart", home["x"], home["y"])
            page.wait_for_timeout(80)
            touch("touchMove", home["x"] + 40, home["y"])
            page.wait_for_timeout(450)
            touch("touchEnd")
            page.wait_for_timeout(300)
            self.assertEqual(page.evaluate(MEASURE)["w"], 56)

            # Hold, drag to the far right and back, release: it lands on a tab, never touching the island edge.
            worst = drag(home, 240, 14)
            self.assertGreater(page.evaluate(MEASURE)["w"], 56, "indicator did not grow while held")
            worst = min(worst, drag_back(page, touch, home, 240, 40))
            self.assertGreaterEqual(worst, 3.9, "indicator touched or left the island")
            touch("touchEnd")
            page.wait_for_timeout(1000)
            end = page.evaluate(MEASURE)
            self.assertEqual(end["w"], 56)
            self.assertAlmostEqual(end["x"], home["x"], delta=1)

            # Hold, drag to another tab, cancel (system gesture): springs back to the start tab.
            start = page.evaluate(MEASURE)
            drag(start, 150, 8)
            touch("touchCancel")
            page.wait_for_timeout(1000)
            self.assertAlmostEqual(page.evaluate(MEASURE)["x"], start["x"], delta=1)

            # Re-grabbing the indicator right after a release (still gliding) must not teleport it.
            for delay in (120, 300):
                page.wait_for_timeout(delay)
                before = page.evaluate(MEASURE)
                touch("touchStart", before["x"], before["y"])
                page.wait_for_timeout(450)
                self.assertLess(
                    abs(page.evaluate(MEASURE)["x"] - before["x"]),
                    30,
                    "indicator jumped on re-grab",
                )
                touch("touchEnd")
                page.wait_for_timeout(900)

            # A plain tap still selects a tab.
            page.tap('.tab[data-i="4"]')
            page.wait_for_timeout(700)
            self.assertGreater(page.evaluate(MEASURE)["x"], start["x"] + 150)
            self.assertEqual(errors, [])
            browser.close()


def drag_back(page, touch, start, dx, steps):
    worst = 99
    for k in range(1, steps + 1):
        touch("touchMove", start["x"] + dx - dx * k / steps * 1.0, start["y"])
        page.wait_for_timeout(16)
        m = page.evaluate(MEASURE)
        worst = min(worst, m["l"], m["r"], m["t"], m["b"])
    page.wait_for_timeout(300)
    return worst


if __name__ == "__main__":
    unittest.main()
