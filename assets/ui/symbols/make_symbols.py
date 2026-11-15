"""Convert Material Symbols Rounded SVGs (viewBox 0 -960 960 960) into absolute
M/L/C/Z paths on a 24-unit grid and write inject/java/cat/ttcuz/framework/Symbols.java.

    python3 assets/ui/symbols/make_symbols.py

The SVGs next to this script come unchanged from google/material-design-icons
(symbols/web/<name>/materialsymbolsrounded/<name>[_fill1]_24px.svg, Apache 2.0).
"""
import math
import pathlib
import re

HERE = pathlib.Path(__file__).resolve().parent
OUT = HERE.parents[2] / "inject/java/cat/ttcuz/framework/Symbols.java"
TOKEN = re.compile(r"[MmLlHhVvCcSsQqTtAaZz]|[-+]?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?")


def _arc(x1, y1, rx, ry, phi, large, sweep, x2, y2):
    """SVG elliptical arc to cubic segments (W3C implementation notes, F.6)."""
    if rx == 0 or ry == 0:
        return [(x1, y1, x2, y2, x2, y2)]
    rx, ry = abs(rx), abs(ry)
    cp, sp = math.cos(math.radians(phi)), math.sin(math.radians(phi))
    dx, dy = (x1 - x2) / 2, (y1 - y2) / 2
    xp, yp = cp * dx + sp * dy, -sp * dx + cp * dy
    lam = xp * xp / (rx * rx) + yp * yp / (ry * ry)
    if lam > 1:
        rx, ry = rx * math.sqrt(lam), ry * math.sqrt(lam)
    num = rx * rx * ry * ry - rx * rx * yp * yp - ry * ry * xp * xp
    den = rx * rx * yp * yp + ry * ry * xp * xp
    co = math.sqrt(max(0.0, num / den)) * (-1 if large == sweep else 1)
    cxp, cyp = co * rx * yp / ry, -co * ry * xp / rx
    cx, cy = cp * cxp - sp * cyp + (x1 + x2) / 2, sp * cxp + cp * cyp + (y1 + y2) / 2

    def angle(ux, uy, vx, vy):
        a = math.atan2(ux * vy - uy * vx, ux * vx + uy * vy)
        return a

    t1 = angle(1, 0, (xp - cxp) / rx, (yp - cyp) / ry)
    dt = angle((xp - cxp) / rx, (yp - cyp) / ry, (-xp - cxp) / rx, (-yp - cyp) / ry)
    if not sweep and dt > 0:
        dt -= 2 * math.pi
    elif sweep and dt < 0:
        dt += 2 * math.pi
    n = max(1, int(math.ceil(abs(dt) / (math.pi / 2))))
    step = dt / n
    k = 4 / 3 * math.tan(step / 4)
    out = []
    for i in range(n):
        a1, a2 = t1 + i * step, t1 + (i + 1) * step
        e1x, e1y = math.cos(a1), math.sin(a1)
        e2x, e2y = math.cos(a2), math.sin(a2)
        pts = [(e1x - k * e1y, e1y + k * e1x), (e2x + k * e2y, e2y - k * e2x), (e2x, e2y)]
        seg = []
        for px, py in pts:
            px, py = px * rx, py * ry
            seg += [cp * px - sp * py + cx, sp * px + cp * py + cy]
        out.append(tuple(seg))
    return out


def absolute(d):
    """Any SVG path -> list of ('M',x,y) / ('L',x,y) / ('C',6 values) / ('Z',)."""
    tokens = TOKEN.findall(d)
    i, cmd = 0, None
    x = y = sx = sy = 0.0
    last_c = last_q = None
    out = []

    def num():
        nonlocal i
        v = float(tokens[i])
        i += 1
        return v

    while i < len(tokens):
        if tokens[i].isalpha():
            cmd = tokens[i]
            i += 1
            if cmd in "Zz":
                out.append(("Z",))
                x, y = sx, sy
                last_c = last_q = None
                continue
        rel = cmd.islower()
        c = cmd.upper()
        ox, oy = (x, y) if rel else (0.0, 0.0)
        if c == "M":
            x, y = num() + ox, num() + oy
            sx, sy = x, y
            out.append(("M", x, y))
            cmd = "l" if rel else "L"
            last_c = last_q = None
        elif c == "L":
            x, y = num() + ox, num() + oy
            out.append(("L", x, y))
            last_c = last_q = None
        elif c == "H":
            x = num() + (x if rel else 0.0)
            out.append(("L", x, y))
            last_c = last_q = None
        elif c == "V":
            y = num() + (y if rel else 0.0)
            out.append(("L", x, y))
            last_c = last_q = None
        elif c in "CS":
            if c == "C":
                x1, y1 = num() + ox, num() + oy
            else:
                x1, y1 = (2 * x - last_c[0], 2 * y - last_c[1]) if last_c else (x, y)
            x2, y2 = num() + ox, num() + oy
            ex, ey = num() + ox, num() + oy
            out.append(("C", x1, y1, x2, y2, ex, ey))
            last_c, last_q = (x2, y2), None
            x, y = ex, ey
        elif c in "QT":
            if c == "Q":
                qx, qy = num() + ox, num() + oy
            else:
                qx, qy = (2 * x - last_q[0], 2 * y - last_q[1]) if last_q else (x, y)
            ex, ey = num() + ox, num() + oy
            out.append(("C", x + 2 / 3 * (qx - x), y + 2 / 3 * (qy - y),
                        ex + 2 / 3 * (qx - ex), ey + 2 / 3 * (qy - ey), ex, ey))
            last_q, last_c = (qx, qy), None
            x, y = ex, ey
        elif c == "A":
            rx, ry, phi, large, sweep = num(), num(), num(), num(), num()
            ex, ey = num() + ox, num() + oy
            for seg in _arc(x, y, rx, ry, phi, int(large), int(sweep), ex, ey):
                out.append(("C",) + seg)
            x, y = ex, ey
            last_c = last_q = None
        else:
            raise ValueError("unsupported command " + cmd)
    return out


def fmt(v):
    s = ("%.2f" % v).rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def to24(d, grid24=False):
    """Material's 960 grid (y from -960) to 24 units; an SVG already on a 24 grid is kept as it is."""
    parts = []
    for op in absolute(d):
        if op[0] == "Z":
            parts.append("Z")
            continue
        vals = op[1:]
        if grid24:
            pts = [fmt(v) for v in vals]
        else:
            pts = [fmt(vals[k] / 40) if k % 2 == 0 else fmt((vals[k] + 960) / 40) for k in range(len(vals))]
        parts.append(op[0] + " " + " ".join(pts))
    return " ".join(parts)


def main():
    entries = []
    for svg in sorted(HERE.glob("*.svg")):
        text = svg.read_text(encoding="utf8")
        # a few icons come from other open sets on a 24 grid (ghost: Tabler, MIT): every drawn path, the empty frame left out
        grid24 = re.search(r'viewBox="0 0 24 24"', text) is not None
        if grid24:
            ds = [m.group(2) for m in re.finditer(r'<path([^>]*?)\sd="([^"]+)"([^>]*)>', text) if 'fill="none"' not in m.group(1) + m.group(3)]
            entries.append((svg.stem, " ".join(to24(d, True) for d in ds)))
        else:
            d = re.search(r'\sd="([^"]+)"', text).group(1)
            entries.append((svg.stem, to24(d)))
    lines = ["package cat.ttcuz.framework;", "",
             "/** Material Symbols Rounded (Google, Apache 2.0), generated by assets/ui/symbols/make_symbols.py.",
             " *  Each entry is an absolute M/L/C/Z path on a 24-unit grid; \"_fill1\" is the filled variant. */",
             "final class Symbols {", "    private Symbols() {}",
             "    private static final java.util.Map<String,String> DATA=new java.util.HashMap<>();",
             "    private static final java.util.Map<String,android.graphics.Path> PATHS=new java.util.HashMap<>();",
             "    static {"]
    for name, d in entries:
        lines.append('        DATA.put("%s","%s");' % (name, d))
    lines += ["    }",
              "    /** Parsed once and shared: a Path per symbol, never rebuilt per frame. Main thread only. */",
              "    static android.graphics.Path path(String name){",
              "        android.graphics.Path cached=PATHS.get(name);if(cached!=null)return cached;",
              "        String d=DATA.get(name);if(d==null)return null;",
              "        android.graphics.Path path=new android.graphics.Path();String[] t=d.split(\" \");",
              "        for(int i=0;i<t.length;){",
              "            String c=t[i++];",
              "            if(c.equals(\"Z\")){path.close();continue;}",
              "            if(c.equals(\"M\")){path.moveTo(Float.parseFloat(t[i]),Float.parseFloat(t[i+1]));i+=2;}",
              "            else if(c.equals(\"L\")){path.lineTo(Float.parseFloat(t[i]),Float.parseFloat(t[i+1]));i+=2;}",
              "            else{path.cubicTo(Float.parseFloat(t[i]),Float.parseFloat(t[i+1]),Float.parseFloat(t[i+2]),Float.parseFloat(t[i+3]),Float.parseFloat(t[i+4]),Float.parseFloat(t[i+5]));i+=6;}",
              "        }",
              "        PATHS.put(name,path);return path;",
              "    }",
              "    static boolean has(String name){return DATA.containsKey(name);}",
              "    /** A tintable 24-unit glyph scaled to its bounds. */",
              "    static android.graphics.drawable.Drawable drawable(String name,int colour){android.graphics.Path p=path(name);return p==null?null:new Glyph(p,colour);}",
              "    static final class Glyph extends android.graphics.drawable.Drawable {",
              "        private final android.graphics.Path path;private final android.graphics.Paint ink=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);",
              "        Glyph(android.graphics.Path path,int colour){this.path=path;ink.setColor(colour);}",
              "        void colour(int colour){if(ink.getColor()!=colour){ink.setColor(colour);invalidateSelf();}}",
              "        @Override public void draw(android.graphics.Canvas canvas){android.graphics.Rect b=getBounds();if(b.width()<=0)return;int s=canvas.save();canvas.translate(b.left,b.top);canvas.scale(b.width()/24f,b.height()/24f);canvas.drawPath(path,ink);canvas.restoreToCount(s);}",
              "        @Override public void setAlpha(int alpha){ink.setAlpha(alpha);invalidateSelf();}",
              "        @Override public void setColorFilter(android.graphics.ColorFilter filter){ink.setColorFilter(filter);invalidateSelf();}",
              "        @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}",
              "        @Override public int getIntrinsicWidth(){return -1;}",
              "        @Override public int getIntrinsicHeight(){return -1;}",
              "    }",
              "}", ""]
    OUT.write_text("\n".join(lines), encoding="utf8")
    print("wrote %d symbols to %s" % (len(entries), OUT))


if __name__ == "__main__":
    main()
