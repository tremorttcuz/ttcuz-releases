"""Small path primitives shared by the rounded TTCuz icon generator."""
import math, json, sys
R = 1.0
K = 0.5523

def f(v):
    s = f"{v:.2f}".rstrip('0').rstrip('.')
    return '0' if s in ('-0', '') else s

def capsule(p1, p2, r=R):
    dx, dy = p2[0]-p1[0], p2[1]-p1[1]
    L = math.hypot(dx, dy)
    if L < 1e-6: dx, dy, L = 1, 0, 1
    d = (dx/L, dy/L); n = (-d[1], d[0])
    def pt(c, a, b): return (c[0]+r*(a[0]*1)+0, c[1]+r*a[1]) if False else (c[0]+r*a[0], c[1]+r*a[1])
    def arc(c, u, v):
        a = (c[0]+r*u[0], c[1]+r*u[1]); b = (c[0]+r*v[0], c[1]+r*v[1])
        c1 = (c[0]+r*(u[0]+K*v[0]), c[1]+r*(u[1]+K*v[1]))
        c2 = (c[0]+r*(v[0]+K*u[0]), c[1]+r*(v[1]+K*u[1]))
        return c1, c2, b
    neg = lambda v: (-v[0], -v[1])
    out = []
    s = (p1[0]+r*n[0], p1[1]+r*n[1])
    out.append(('M', [s]))
    out.append(('L', [(p2[0]+r*n[0], p2[1]+r*n[1])]))
    for u, v in ((n, d), (d, neg(n))):
        c1, c2, b = arc(p2, u, v); out.append(('C', [c1, c2, b]))
    out.append(('L', [(p1[0]-r*n[0], p1[1]-r*n[1])]))
    for u, v in ((neg(n), neg(d)), (neg(d), n)):
        c1, c2, b = arc(p1, u, v); out.append(('C', [c1, c2, b]))
    out.append(('Z', []))
    return out

def area(pts): return sum(pts[i][0]*pts[(i+1) % len(pts)][1]-pts[(i+1) % len(pts)][0]*pts[i][1] for i in range(len(pts)))/2
CAP_SIGN = area([(0, 0), (1, 0), (1, -1), (0, -1)])  # placeholder, calibrated below
# calibrate: capsule corner polygon p1+n, p2+n, p2-n, p1-n for d=(1,0) -> n=(0,1)
CAP_SIGN = 1 if area([(0, 1), (1, 1), (1, -1), (0, -1)]) > 0 else -1

def poly_fill(points):
    pts = list(points)
    if (area(pts) > 0) != (CAP_SIGN > 0): pts.reverse()
    out = [('M', [pts[0]])] + [('L', [p]) for p in pts[1:]] + [('Z', [])]
    return out

class Icon:
    def __init__(self): self.cmds = []
    def line(self, *pts, r=R):
        for a, b in zip(pts, pts[1:]): self.cmds += capsule(a, b, r)
        return self
    def poly(self, *pts, r=R):
        return self.line(*pts, pts[0], r=r)
    def arc(self, c, rad, a0, a1, r=R, step=18, ry=None):
        ry = rad if ry is None else ry
        n = max(2, int(abs(a1-a0)/step)+1)
        pts = [(c[0]+rad*math.cos(math.radians(a0+(a1-a0)*i/n)), c[1]-ry*math.sin(math.radians(a0+(a1-a0)*i/n))) for i in range(n+1)]
        return self.line(*pts, r=r), pts
    def ring(self, c, rad, r=R, ry=None, step=24):
        self.arc(c, rad, 0, 360, r=r, step=step, ry=ry); return self
    def disc(self, c, rad):
        self.cmds += capsule(c, (c[0]+0.001, c[1]), rad); return self
    def fill(self, *pts): self.cmds += poly_fill(pts); return self
    def chev(self, tip, d, size=3.4, ang=45, r=R):
        L = math.hypot(*d); d = (d[0]/L, d[1]/L)
        arms = []
        for s in (1, -1):
            a = math.radians(ang*s); rx = d[0]*math.cos(a)-d[1]*math.sin(a); ry_ = d[0]*math.sin(a)+d[1]*math.cos(a)
            arms.append((tip[0]-size*rx, tip[1]-size*ry_))
        return self.line(arms[0], tip, arms[1], r=r)
    def d(self):
        parts = []
        for c, pts in self.cmds: parts.append(c + (' ' + ' '.join(f(x)+' '+f(y) for x, y in pts) if pts else ''))
        return ' '.join(parts)

icons = {}
def make(name):
    def deco(fn):
        i = Icon(); fn(i); icons[name] = i.d(); return fn
    return deco

def tri(i, pts):
    i.fill(*pts); i.poly(*pts, r=0.9)

@make('add')
def _(i): i.line((12, 5), (12, 19)).line((5, 12), (19, 12))

@make('edit')
def _(i):
    i.poly((5.2, 18.8), (5.9, 14.7), (15.6, 5), (19, 8.4), (9.3, 18.1))
    i.line((13.3, 7.3), (16.7, 10.7))

@make('restore')
def _(i):
    i.arc((12, 12), 7.1, 35, 325, r=1.05, step=16)
    i.chev((6.2, 6.2), (-1, -1), size=2.8, r=1.05)
    i.line((12, 7.8), (12, 12.1), (15.1, 14), r=1.0)

@make('swap_horiz')
def _(i):
    i.line((4.5, 8), (19.5, 8)).chev((19.5, 8), (1, 0), 3.6)
    i.line((19.5, 16), (4.5, 16)).chev((4.5, 16), (-1, 0), 3.6)

@make('upload')
def _(i):
    i.line((12, 15.5), (12, 5)).chev((12, 5), (0, -1), 4.4)
    i.line((5, 15), (5, 19), (19, 19), (19, 15))

@make('flare')
def _(i):
    i.disc((12, 12), 2.6)
    for k in range(8):
        a = math.radians(45*k); lo, hi = (6.1, 9.6) if k % 2 == 0 else (5.9, 8.0)
        i.line((12+lo*math.cos(a), 12-lo*math.sin(a)), (12+hi*math.cos(a), 12-hi*math.sin(a)))

@make('animation')
def _(i):
    # Three rounded frames, with the enclosed mark changing over time.
    for left in (2.8, 10.0, 17.2):
        i.line((left+0.8, 5.2), (left+3.2, 5.2), (left+4.0, 6.0),
               (left+4.0, 18.0), (left+3.2, 18.8), (left+0.8, 18.8),
               (left, 18.0), (left, 6.0), (left+0.8, 5.2), r=0.65)
    i.disc((4.8, 12), 1.05)
    i.ring((12, 12), 1.3, ry=0.9, r=0.65, step=30)
    i.poly((19.1, 10.6), (20.5, 12), (19.1, 13.4), (17.7, 12), r=0.65)

@make('aspect_ratio')
def _(i):
    i.line((5, 6), (19, 6), (19, 18), (5, 18), (5, 6), r=1.1)
    i.line((8, 12), (16, 12), r=1.0)
    i.disc((8, 12), 1.2); i.disc((16, 12), 1.2)

@make('blur_on')
def _(i):
    # The clean outer ring is the SVG/runtime vector fallback; the SVG adds
    # crisp left samples and a filtered right half, while runtime prefers PNG.
    i.ring((12, 12), 8.6, r=1.15, step=12)

@make('fingerprint')
def _(i):
    i.arc((12, 13), 8.0, 215, -35, r=0.95, step=12)
    i.arc((12, 13), 6.1, 212, -32, r=0.95, step=12)
    i.arc((12, 13), 4.2, 205, -25, r=0.95, step=12)
    i.arc((12, 13), 2.2, 195, -15, r=0.95, step=12)
    i.line((12, 5.3), (12, 7.0), (13.2, 8.6), (14.0, 10.4),
           (14.0, 12.6), (13.5, 14.4), (12.8, 16.5), (12.8, 18.8), r=0.95)

@make('wallpaper')
def _(i):
    i.line((4, 4.5), (20, 4.5), (20, 19.5), (4, 19.5), (4, 4.5), r=1.05)
    i.ring((15.5, 9), 1.7, r=0.9, step=28)
    i.line((6.5, 16.5), (10.2, 12.6), (12.4, 14.5), (15, 11.5), (18, 15), r=1.0)

@make('video_library')
def _(i):
    i.line((5, 4), (16, 4), (16, 6), r=1.0)
    i.line((4, 7), (4, 18.5), (15.5, 18.5), r=1.0)
    i.line((8.5, 6.5), (20, 6.5), (20, 19), (15.5, 19), r=1.0)
    tri(i, [(12, 10), (12, 16), (16.5, 13)])

@make('bug_report')
def _(i):
    i.ring((12, 14.2), 4.4, ry=5.6, step=26)
    i.line((12, 9.5), (12, 19.8)).line((7.8, 13.2), (16.2, 13.2))
    i.line((4, 13.2), (7.6, 13.2)).line((16.4, 13.2), (20, 13.2))
    i.line((4.6, 18.6), (8, 17)).line((19.4, 18.6), (16, 17))
    i.line((4.6, 8), (8.3, 10)).line((19.4, 8), (15.7, 10))
    i.line((9.3, 3.8), (10.7, 6)).line((14.7, 3.8), (13.3, 6))

@make('speed')
def _(i):
    i.arc((12, 13.5), 8.6, 205, -25, step=18)
    i.line((12, 13.5), (16.4, 8.9)); i.disc((12, 13.5), 1.9)

@make('fast_forward')
def _(i):
    tri(i, [(4.6, 7.2), (4.6, 16.8), (11.4, 12)]); tri(i, [(12.6, 7.2), (12.6, 16.8), (19.4, 12)])

@make('fast_rewind')
def _(i):
    tri(i, [(19.4, 7.2), (19.4, 16.8), (12.6, 12)]); tri(i, [(11.4, 7.2), (11.4, 16.8), (4.6, 12)])

@make('open_with')
def _(i):
    i.line((12, 5), (12, 19)).line((5, 12), (19, 12))
    for tip, d in (((12, 3.8), (0, -1)), ((12, 20.2), (0, 1)), ((3.8, 12), (-1, 0)), ((20.2, 12), (1, 0))): i.chev(tip, d, 3.2)

@make('delete')
def _(i):
    i.line((5, 6.5), (19, 6.5), r=1.25)
    i.line((9, 6.5), (9, 4.8), (10, 3.8), (14, 3.8), (15, 4.8), (15, 6.5), r=1.1)
    i.line((7.2, 8.2), (8, 18.8), (16, 18.8), (16.8, 8.2), r=1.15)
    i.line((10, 10), (10.3, 16.7), r=0.75)
    i.line((14, 10), (13.7, 16.7), r=0.75)

@make('refresh')
def _(i):
    _, pts = i.arc((12, 12), 7.2, 45, 315, r=1.15, step=14)
    tip = pts[-1]
    angle = math.radians(315)
    i.chev(tip, (-math.sin(angle), -math.cos(angle)), size=3.1, r=1.15)

@make('update')
def _(i):
    i.line((12, 4), (12, 14), r=1.15)
    i.chev((12, 14), (0, 1), size=3.2, r=1.15)
    i.line((5, 15.5), (5, 19), (19, 19), (19, 15.5), r=1.05)

@make('remove')
def _(i):
    i.ring((12, 12), 7.1, r=1.2, step=15)
    i.line((8, 12), (16, 12), r=1.3)

@make('search')
def _(i):
    i.ring((10.5, 10.5), 6.5, r=1.2, step=12)
    i.line((15.1, 15.1), (20.2, 20.2), r=1.2)

if __name__ == "__main__":
    from pathlib import Path
    mode = sys.argv[1] if len(sys.argv) > 1 else "json"
    source = Path(__file__).parent / "settings"
    if mode == "svg":
        from redraw_icon_set import refresh
        refresh()
    elif mode == "json":
        import re
        icons = {}
        for path in sorted(source.glob("*.svg")):
            match = re.search(r'<path\b[^>]*\bd="([^"]+)"', path.read_text(encoding="utf-8"))
            if match:
                icons[path.stem] = match.group(1)
        destination = Path(sys.argv[2]) if len(sys.argv) > 2 else source / "paths.json"
        destination.write_text(json.dumps(icons, indent=2) + "\n", encoding="utf-8")
    else:
        raise SystemExit("usage: make_settings_icons.py [svg|json [output.json]]")
