package cat.ttcuz.framework;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Choreographer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * The Liquid Glass island's selection, as iOS 26 draws its tab bar (only for the Liquid Glass style; the others keep
 * their pill).
 *
 * At rest the chosen tab sits in a faint capsule of glass, its icon and label in the accent. On a tab change the
 * capsule swells into a lens bigger than the island -- it reaches past its top and bottom -- and slides to the new
 * tab. The lens is clear: everything under it is magnified (the island, its icons and labels, drawn in the accent
 * inside it); near its rim what lies beyond it is squeezed in, with a cyan and a pink fringe, so the island's edge
 * shows through it as two dark arcs; and a thin light rim outlines it. On landing it shrinks back into the capsule.
 *
 * The flight is drawn on a thread of its own, into a surface over the island: TikTok switching pages blocks the main
 * thread just when the lens should move, and the surface does not wait for it. It starts on the finger's release.
 * The lens redraws what it magnifies itself (from a {@link Scene} of the island), so it needs nothing from the main
 * thread while it flies. At rest the island draws the capsule and the surface is clear and idle.
 */
final class IslandDrop {
    private IslandDrop() {}

    /**
     * How much the lens magnifies in its middle, and how its rim squeezes in what lies beyond it (the island's edge,
     * the neighbouring tabs): a real lens bends hardest at its edge and shows there what is outside it.
     */
    static final float MAGNIFY = 1.28f, RIM_MAGNIFY = .72f;

    private static float lerp(float a, float b, float k) { return a + (b - a) * k; }

    private static int argb(int rgb, float a) { return (Math.round(Math.max(0f, Math.min(1f, a)) * 255) << 24) | (rgb & 0xFFFFFF); }

    private static float smooth(float e0, float e1, float x) { float t = Math.max(0f, Math.min(1f, (x - e0) / (e1 - e0))); return t * t * (3f - 2f * t); }

    /** How much of the lens's own picture shows at a swell: none at rest (the island under it shows), all while it flies. */
    static float opacity(float swell) { return smooth(0f, .3f, swell); }

    /**
     * Where the lens is, as a body on springs: its centre and width follow their target with the speed they have,
     * and its swell follows how far it still has to go. A new tab only moves the target, so tapping quickly sends the
     * lens on from wherever it is, at the speed it has, never back to a start; nothing in it jumps. Time is real
     * time: after a missed frame it is where it would have been.
     */
    static final class Motion {
        float x, v, w, vw, s, vs, tx, tw;
        boolean live;
        /** Where the lens may be: the island, a little inside its edge. Empty: anywhere. */
        final RectF bounds = new RectF();

        /** Sends the lens to (toX, toW); from (fromX, fromW) only when it was at rest. */
        void start(float fromX, float fromW, float toX, float toW) {
            if (!live) { x = fromX; w = fromW; v = 0f; vw = 0f; s = 0f; vs = 0f; live = true; }
            tx = toX; tw = toW;
        }

        /** One step of dt seconds; false once it has come to rest on its target. */
        boolean step(float dt) {
            if (!live) return false;
            // the island's motion choice: how fast, how springy
            float sk = IslandMotion.stiffness(), damp = IslandMotion.damping();
            dt = Math.min(dt, .05f);
            int n = Math.max(1, (int) Math.ceil(dt / .004f));
            float h = dt / n;
            for (int i = 0; i < n; i++) {
                // the centre: a little under critical damping, a soft arrival with the faintest settle
                float k = 210f * sk, c = 2f * (float) Math.sqrt(k) * .86f * damp;
                v += (k * (tx - x) - c * v) * h; x += v * h;
                // the width: critically damped
                float kw = 260f * sk, cw = 2f * (float) Math.sqrt(kw) * Math.min(1f, damp * 1.1f);
                vw += (kw * (tw - w) - cw * vw) * h; w += vw * h;
                // the swell wants to be full while there is way to go or speed to lose, nothing at rest
                float want = Math.max(smooth(0f, IslandMotion.swellReach() * tw, Math.abs(tx - x)), Math.min(1f, Math.abs(v) / Math.max(1f, tw * 4f)));
                float ss = IslandMotion.swellStiffness(), ks = 240f * ss, cs = 2f * (float) Math.sqrt(ks);
                vs += (ks * (want - s) - cs * vs) * h; s += vs * h;
            }
            s = Math.max(0f, Math.min(1.15f * IslandMotion.swell(), s));
            if (Math.abs(tx - x) < .4f && Math.abs(v) < 4f && Math.abs(tw - w) < .4f && s < .004f && Math.abs(vs) < .03f) {
                x = tx; w = tw; v = vw = s = vs = 0f; live = false;
            }
            return live;
        }

        /**
         * The lens now: swollen while it moves -- a third taller than its capsule, so it stands a little past the
         * island's top and bottom -- and a little longer still while it moves fast.
         */
        void rect(RectF out, float cy, float restHeight) {
            float more = IslandMotion.swell(), sw = Math.min(1f, s), stretch = Math.min(.12f * more, Math.abs(v) / Math.max(1f, w * 40f) * more);
            float lw = w * (1f + .24f * sw * IslandMotion.wide() + stretch), lh = restHeight * (1f + .32f * sw * IslandMotion.tall());
            // the video's bubble: as it swells it turns from the resting capsule into a round drop 0.85 as wide as tall
            if (IslandMotion.round()) lw = w + (Math.min(w * 1.1f, Math.min(lh, bounds.isEmpty() ? lh : bounds.height()) * .85f) - w) * sw;
            out.set(x - lw / 2f, cy - lh / 2f, x + lw / 2f, cy + lh / 2f);
            inside(out, bounds);
        }
    }

    /**
     * The lens never leaves the island: as tall and as wide as it may grow, it is fitted inside `bounds` (shrunk to fit,
     * then moved in), still centred where it was as far as the edges allow. A capsule standing past the island's edge
     * looked broken. Empty bounds leave it as it is.
     */
    static void inside(RectF out, RectF bounds) {
        if (bounds.isEmpty()) return;
        // at the island's end the drop does not hit a wall: what would go past the end spreads back inward instead (the
        // far edge stays at the end, the near one keeps going) and swells a little, then is drawn back in as the
        // spring returns -- a drop pressed against glass, not a ball against a wall
        float past = Math.max(out.right - bounds.right, bounds.left - out.left);
        if (past > 0f) {
            float spread = past * .7f, swell = past * .18f;
            if (out.right > bounds.right) { out.left -= spread; out.right = bounds.right; }
            else { out.right += spread; out.left = bounds.left; }
            out.top -= swell; out.bottom += swell;
        }
        float w = Math.min(out.width(), bounds.width()), h = Math.min(out.height(), bounds.height());
        float cx = Math.max(bounds.left + w / 2f, Math.min(bounds.right - w / 2f, out.centerX()));
        float cy = Math.max(bounds.top + h / 2f, Math.min(bounds.bottom - h / 2f, out.centerY()));
        out.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
    }

    // ------------------------------------------------------------------ what the lens magnifies

    /** The island as the lens sees it: copied on the main thread when a flight starts, read on the surface's. */
    static final class Scene {
        final RectF body = new RectF();
        float radius;
        int bodyFill, edge, ink, accent;
        final Path[] icons = new Path[5];
        final RectF[] places = {new RectF(), new RectF(), new RectF(), new RectF(), new RectF()};
        final String[] labels = new String[5];
        final float[] labelX = new float[5];
        float labelY, labelSize;
        Typeface face;
    }

    // ------------------------------------------------------------------ drawing

    /** Draws the capsule, the lens and the glass button; one per thread (it keeps its paths and paints). */
    static final class Painter {
        final float unit;
        final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), rim = new Paint(Paint.ANTI_ALIAS_FLAG),
                bloom = new Paint(Paint.ANTI_ALIAS_FLAG), ink = new Paint(Paint.ANTI_ALIAS_FLAG), text = new Paint(Paint.ANTI_ALIAS_FLAG),
                light = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path shape = new Path(), inner = new Path(), crescent = new Path();
        final RectF arc = new RectF(), box = new RectF();
        final android.graphics.Matrix turn = new android.graphics.Matrix();

        Painter(float unit) {
            this.unit = unit;
            rim.setStyle(Paint.Style.STROKE);
            text.setTextAlign(Paint.Align.CENTER);
        }

        private void capsule(Path into, RectF r) {
            into.rewind();
            float radius = Math.min(r.width(), r.height()) / 2f;
            into.addRoundRect(r, radius, radius, Path.Direction.CW);
        }

        private void glow(Canvas c, float x, float y, float r, int rgb, float a) {
            if (a <= .004f || r <= 0f) return;
            bloom.setShader(new RadialGradient(x, y, r, new int[]{argb(rgb, a), argb(rgb, a * .35f), argb(rgb, 0f)},
                    new float[]{0f, .45f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(x, y, r, bloom);
        }

        /** A highlight as glass shows one: a thin crescent along the edge, thickest in the middle, gone at both ends. */
        private void spec(Canvas c, float cx, float cy, float r, float a0, float a1, float thick, int rgb, float a) {
            if (a <= .004f || r <= thick) return;
            float mid = (a0 + a1) / 2f, dx = (float) Math.cos(mid), dy = (float) Math.sin(mid);
            float from = (float) Math.toDegrees(a0), sweep = (float) Math.toDegrees(a1 - a0);
            crescent.rewind();
            arc.set(cx - r, cy - r, cx + r, cy + r);
            crescent.arcTo(arc, from, sweep, true);
            float ix = cx - dx * thick, iy = cy - dy * thick;
            arc.set(ix - r, iy - r, ix + r, iy + r);
            crescent.arcTo(arc, from + sweep, -sweep, false);
            crescent.close();
            float span = Math.min(.999f, (a1 - a0) / (float) (Math.PI * 2));
            android.graphics.SweepGradient g = new android.graphics.SweepGradient(cx, cy,
                    new int[]{argb(rgb, 0f), argb(rgb, a * .75f), argb(rgb, a), argb(rgb, a * .75f), argb(rgb, 0f), argb(rgb, 0f)},
                    new float[]{0f, span * .3f, span * .5f, span * .7f, span, 1f});
            turn.setRotate(from, cx, cy);
            g.setLocalMatrix(turn);
            light.setShader(g);
            c.drawPath(crescent, light);
        }

        /** The chosen tab at rest: a faint capsule of glass with a thin light rim. */
        void rest(Canvas c, RectF p, float alpha) {
            if (alpha <= 0f || p.width() <= 0f) return;
            // the gradients are made once per height and strength, not once a frame (the island redraws often)
            if (restTop != p.top || restBottom != p.bottom || restAlpha != alpha) {
                restTop = p.top; restBottom = p.bottom; restAlpha = alpha;
                restFill = new LinearGradient(0, p.top, 0, p.bottom, argb(0xFFFFFF, .10f * alpha), argb(0xFFFFFF, .05f * alpha), Shader.TileMode.CLAMP);
                restRim = new LinearGradient(0, p.top, 0, p.bottom, new int[]{argb(0xFFFFFF, .30f * alpha), argb(0xFFFFFF, .08f * alpha), argb(0xFFFFFF, .20f * alpha)},
                        new float[]{0f, .5f, 1f}, Shader.TileMode.CLAMP);
            }
            boolean video = IslandMotion.round();
            if (video && (restVideo != alpha || restTop != p.top)) {
                // the video's resting selection: a grey square with straight sides, its rim barely there
                restVideo = alpha;
                restFill = new LinearGradient(0, p.top, 0, p.bottom, argb(0xFFFFFF, .26f * alpha), argb(0xFFFFFF, .19f * alpha), Shader.TileMode.CLAMP);
                restRim = new LinearGradient(0, p.top, 0, p.bottom, new int[]{argb(0xFFFFFF, .18f * alpha), argb(0xFFFFFF, .05f * alpha), argb(0xFFFFFF, .10f * alpha)},
                        new float[]{0f, .5f, 1f}, Shader.TileMode.CLAMP);
            } else if (!video && restVideo >= 0f) { restVideo = -1f; restTop = Float.NaN; rest(c, p, alpha); return; }
            float r = video ? Math.min(p.width(), p.height()) * .38f : Math.min(p.width(), p.height()) / 2f;
            fill.setShader(restFill);
            c.drawRoundRect(p, r, r, fill);
            rim.setStrokeWidth(Math.max(1f, unit * .9f));
            rim.setShader(restRim);
            c.drawRoundRect(p, r, r, rim);
        }
        private float restTop = Float.NaN, restBottom, restAlpha, buttonTop = Float.NaN, buttonBottom, buttonAlpha, restVideo = -1f;
        private Shader restFill, restRim, buttonFill, buttonRim;

        /** What the lens shows of the island, magnified m times about (cx, cy): its body, edge, icons and labels. */
        private void magnified(Canvas c, Scene s, float cx, float cy, float m, int iconColour, float alpha, float dx) { magnified(c, s, cx, cy, m, iconColour, alpha, dx, true); }

        /** The island as the lens shows it, unmagnified, without its fill: the shader's picture (see {@link LensShader}). */
        void scene(Canvas c, Scene s) { magnified(c, s, 0f, 0f, 1f, 0, 1f, 0f, false); }

        private void magnified(Canvas c, Scene s, float cx, float cy, float m, int iconColour, float alpha, float dx, boolean withFill) {
            int save = c.save();
            c.translate(cx + dx, cy);
            c.scale(m, m);
            c.translate(-cx, -cy);
            if (iconColour == 0) {
                float r = s.radius;
                if (withFill) {
                    fill.setShader(null);
                    fill.setColor(argb(s.bodyFill, ((s.bodyFill >>> 24) / 255f) * alpha));
                    c.drawRoundRect(s.body, r, r, fill);
                }
                // the island's dark edge, which the lens shows as two arcs near its top and bottom
                rim.setShader(null);
                rim.setStrokeWidth(2.2f * unit);
                rim.setColor(argb(s.edge, alpha));
                c.drawRoundRect(s.body, r, r, rim);
            }
            int colour = iconColour == 0 ? s.accent : iconColour;
            ink.setShader(null);
            ink.setColor(argb(colour, alpha));
            for (int i = 0; i < 5; i++) {
                Path icon = s.icons[i];
                RectF place = s.places[i];
                if (icon != null && !place.isEmpty()) {
                    int k = c.save();
                    c.translate(place.left, place.top);
                    c.scale(place.width() / 24f, place.height() / 24f);
                    c.drawPath(icon, ink);
                    c.restoreToCount(k);
                }
                if (s.labels[i] != null && s.labelSize > 0f) {
                    text.setTextSize(s.labelSize);
                    if (s.face != null) text.setTypeface(s.face);
                    text.setColor(argb(colour, alpha));
                    c.drawText(s.labels[i], s.labelX[i], s.labelY, text);
                }
            }
            c.restoreToCount(save);
        }

        /**
         * The lens at L over the island: clear, magnifying what is under it, pulled harder and fringed near its rim,
         * with the island's edge seen through it and a thin light rim.
         */
        void lens(Canvas c, RectF L, Scene s, float alpha) {
            if (alpha <= 0f || L.width() <= 0f || s == null) return;
            float cx = L.centerX(), cy = L.centerY(), band = Math.min(L.height() * .2f, 9f * unit);
            capsule(shape, L);
            int save = c.save();
            c.clipPath(shape);
            // the middle: the island magnified
            magnified(c, s, cx, cy, MAGNIFY, 0, alpha, 0f);
            // the rim: what lies beyond the lens squeezed in (the glass bends hardest there), with a cyan and a pink fringe
            box.set(L.left + band, L.top + band, L.right - band, L.bottom - band);
            capsule(inner, box);
            int ring = c.save();
            if (android.os.Build.VERSION.SDK_INT >= 26) c.clipOutPath(inner);
            else c.clipPath(inner, android.graphics.Region.Op.DIFFERENCE);
            magnified(c, s, cx, cy, RIM_MAGNIFY, 0, alpha, 0f);
            magnified(c, s, cx, cy, RIM_MAGNIFY, 0x38E8FF, .55f * alpha, 1.6f * unit);
            magnified(c, s, cx, cy, RIM_MAGNIFY, 0xFF4FD8, .40f * alpha, -1.6f * unit);
            c.restoreToCount(ring);
            // a little shade inside the top and the bottom of the glass
            fill.setShader(new LinearGradient(0, L.top, 0, L.bottom, new int[]{argb(0x000000, .22f * alpha), argb(0x000000, 0f), argb(0x000000, 0f), argb(0x000000, .18f * alpha)},
                    new float[]{0f, .14f, .86f, 1f}, Shader.TileMode.CLAMP));
            c.drawPath(shape, fill);
            c.restoreToCount(save);
            // the rim: a thin light line, brighter on the upper left and the lower right
            rim.setStrokeWidth(Math.max(1f, unit));
            rim.setShader(new LinearGradient(L.left, L.top, L.right, L.bottom, new int[]{argb(0xFFFFFF, .62f * alpha), argb(0xFFFFFF, .22f * alpha), argb(0xFFFFFF, .45f * alpha)},
                    new float[]{0f, .5f, 1f}, Shader.TileMode.CLAMP));
            c.drawPath(shape, rim);
            float r = L.height() / 2f;
            spec(c, L.left + r, cy, r - unit, (float) (Math.PI * 1.1), (float) (Math.PI * 1.55), 1.8f * unit, 0xFFFFFF, .7f * alpha);
            spec(c, L.right - r, cy, r - unit, (float) (Math.PI * .1), (float) (Math.PI * .5), 1.4f * unit, 0xFFFFFF, .35f * alpha);
        }

        /** The create button as a small pane of clear glass, lit at its top edge, with a plus. */
        void button(Canvas c, float cx, float cy, float islandHeight, float squeeze, Path plus, int plusColour, float alpha) {
            if (alpha <= 0f || islandHeight <= 0f) return;
            float h = islandHeight * .62f * squeeze, w = islandHeight * .8f * squeeze, r = h * .32f;
            box.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
            if (buttonTop != box.top || buttonBottom != box.bottom || buttonAlpha != alpha) {
                buttonTop = box.top; buttonBottom = box.bottom; buttonAlpha = alpha;
                buttonFill = new LinearGradient(0, box.top, 0, box.bottom, argb(0xFFFFFF, .16f * alpha), argb(0xFFFFFF, .06f * alpha), Shader.TileMode.CLAMP);
                buttonRim = new LinearGradient(0, box.top, 0, box.bottom, new int[]{argb(0xFFFFFF, .42f * alpha), argb(0xFFFFFF, .10f * alpha), argb(0xFFFFFF, .20f * alpha)},
                        new float[]{0f, .5f, 1f}, Shader.TileMode.CLAMP);
            }
            fill.setShader(buttonFill);
            c.drawRoundRect(box, r, r, fill);
            rim.setStrokeWidth(Math.max(1f, unit));
            rim.setShader(buttonRim);
            c.drawRoundRect(box, r, r, rim);
            if (plus != null) {
                int s = c.save();
                float k = h * .62f / 24f;
                c.translate(cx - 12f * k, cy - 12f * k);
                c.scale(k, k);
                ink.setShader(null);
                ink.setColor(argb(plusColour, .95f * alpha));
                c.drawPath(plus, ink);
                c.restoreToCount(s);
            }
        }
    }

    // ------------------------------------------------------------------ the shader

    /**
     * The lens as a shader (Android 13 and later): every pixel of it reads the island at a bent place.
     *
     * In the middle the island is magnified (1 / magnify of the way to the centre). Near the rim -- a band as wide as
     * the glass bevel -- the glass bends harder, more the closer to the edge (k squared), and reads further out along
     * the rim's normal: what lies beyond the lens is drawn into it, as iOS's lens shows the island's edge and the next
     * tab. Red and blue are read a little apart along that normal, which gives the cyan and pink fringes. The bevel
     * is shaded a little, and a hairline of light runs round the rim, brightest where it faces the upper left and the
     * lower right. Its picture is the island without its fill (so the glass under it is the island's own), shown as
     * much as the lens is swollen; at rest only a faint rim is left, over the island's own capsule.
     */
    static final String LENS_SHADER =
            "uniform shader content;uniform float2 center;uniform float2 halfSize;uniform float radius;uniform float magnify;"
          + "uniform float bezel;uniform float bend;uniform float rim;uniform float opacity;uniform float hair;uniform float veil;"
          + "float box(float2 p,float2 b,float r){float2 q=abs(p)-b+r;return length(max(q,0.0))+min(max(q.x,q.y),0.0)-r;}"
          + "half4 main(float2 xy){"
          + " float2 p=xy-center;float sd=box(p,halfSize,radius);"
          + " if(sd>1.0)return half4(0);"
          + " float inside=max(-sd,0.0);"
          + " float2 n=float2(box(p+float2(1,0),halfSize,radius)-sd,box(p+float2(0,1),halfSize,radius)-sd);"
          + " n=n/max(length(n),1e-4);"
          + " float k=1.0-clamp(inside/bezel,0.0,1.0);"
          + " float shift=bezel*k*k*bend*1.7;"
          + " float2 uv=center+p/magnify+n*shift;"
          + " half4 c=content.eval(uv);"
          // the fringes only on the bevel: the flat middle is one read
          + " if(k>0.0&&bend>0.0){float2 split=n*(1.5+shift*0.22)*bend;half4 cr=content.eval(uv+split);half4 cb=content.eval(uv-split);"
          + "  c=half4(cr.r,c.g,cb.b,max(c.a,max(cr.a,cb.a)));}"
          + " c*=half(opacity);"
          + " c.rgb*=half(1.0-k*k*0.22*bend);"
          + " float line=clamp(1.0-inside/hair,0.0,1.0);"
          + " float lit=0.35+0.65*max(0.0,dot(n,normalize(float2(-0.55,-0.83))))+0.45*max(0.0,dot(n,normalize(float2(0.6,0.8))));"
          + " float light=line*lit*rim;"
          + " c.rgb+=half3(light);c.a=max(c.a,half(light));"
          + " c+=half4(half(veil))*(1.0-c.a)*half(opacity);"
          + " return c*half(clamp(-sd+0.5,0.0,1.0));}";

    /**
     * The light lens (the island's "light" glass): the same bend, rim and hairline, but the rim's normal comes from the
     * capsule's nearest inner point instead of two more distances, and there are no colour fringes: one picture read
     * per pixel instead of three on the bevel.
     */
    static final String LENS_LIGHT = LENS_SHADER
            .replace(" float2 n=float2(box(p+float2(1,0),halfSize,radius)-sd,box(p+float2(0,1),halfSize,radius)-sd);",
                    " float2 core=max(halfSize-radius,float2(0.0));float2 n=p-clamp(p,-core,core);")
            .replace(" if(k>0.0&&bend>0.0){float2 split=n*(1.5+shift*0.22)*bend;half4 cr=content.eval(uv+split);half4 cb=content.eval(uv-split);"
                    + "  c=half4(cr.r,c.g,cb.b,max(c.a,max(cr.a,cb.a)));}", "");

    /**
     * The same lens over a picture of the island's glass (the {@code back} input), shown unbent under what the lens
     * bends: on the lens's own surface the glass is not drawn by anyone else.
     */
    static final String LENS_OVER_GLASS = overGlass(LENS_SHADER), LENS_OVER_GLASS_LIGHT = overGlass(LENS_LIGHT);

    private static String overGlass(String lens) {
        return lens.replace("uniform shader content;", "uniform shader content;uniform shader back;")
                .replace(" return c*half(clamp(-sd+0.5,0.0,1.0));}", " c=c+back.eval(xy)*(1.0-c.a);return c*half(clamp(-sd+0.5,0.0,1.0));}");
    }

    /** One per thread that draws a lens: the island recorded once per flight, and the lens drawn through the shader. */
    static final class LensShader {
        private Object scene, lens, shader; // RenderNode, RenderNode, RuntimeShader: types of Android 13
        /** The shader was built as the light lens. */
        private boolean builtLight;
        private Scene recorded;
        private float top;
        private int width, height;
        private boolean broken;
        private final Painter painter;
        LensShader(Painter painter) { this.painter = painter; }

        static boolean available() { return android.os.Build.VERSION.SDK_INT >= 33 && Ttcuz.is64(); }

        /** What the lens bends: the island drawn into a canvas, in its row's coordinates. */
        interface Content { void draw(Canvas c); }
        private Object island, bent; // RenderNode, RenderNode
        private final RectF local = new RectF();

        /**
         * The lens, from what is on the island (content: its icons, labels and capsule, no glass): drawn once into a
         * node (reaching extra above and below the row, where the swollen lens stands) and shown through the shader
         * inside the lens; the caller has drawn the glass and what lies outside the lens. False when it cannot (old Android, a software canvas, an error): draw it plainly.
         */
        boolean over(Canvas c, int width, int height, float extra, Content content, RectF L, float swell, float alpha) {
            if (broken || !available() || !c.isHardwareAccelerated() || L.width() <= 0f) return false;
            try {
                android.graphics.RenderNode node = (android.graphics.RenderNode) island, lensNode = (android.graphics.RenderNode) bent;
                if (node == null) {
                    node = new android.graphics.RenderNode("ttcuz-island");
                    // rendered once a frame into a layer, which both the island and the lens then show: the glass and
                    // everything behind it are not worked out twice (that was what made the lens stutter)
                    node.setUseCompositingLayer(true, null);
                    island = node;
                }
                if (lensNode == null) { lensNode = new android.graphics.RenderNode("ttcuz-island-lens"); bent = lensNode; }
                int h = Math.max(1, (int) Math.ceil(height + 2 * extra));
                node.setPosition(0, 0, width, h);
                Canvas r = node.beginRecording(width, h);
                try { r.translate(0f, extra); content.draw(r); } finally { node.endRecording(); }
                // The shader runs only round the lens (with room for what its rim draws in from outside), not over
                // the whole row: a fifth of the pixels, the same picture -- this is what keeps old phones smooth.
                float margin = Math.min(L.height() * .22f, 10f * painter.unit) * 2f + 4f;
                int left = Math.max(0, (int) Math.floor(L.left - margin)), top = Math.max(0, (int) Math.floor(L.top + extra - margin));
                int right = Math.min(width, (int) Math.ceil(L.right + margin)), bottom = Math.min(h, (int) Math.ceil(L.bottom + extra + margin));
                if (right <= left || bottom <= top) return false;
                lensNode.setPosition(left, top, right, bottom);
                Canvas o = lensNode.beginRecording(right - left, bottom - top);
                try { o.translate(-left, -top); o.drawRenderNode(node); } finally { lensNode.endRecording(); }
                local.set(L);
                local.offset(-left, extra - top);
                android.graphics.RuntimeShader lensShader = shader(local, swell, alpha, 1f, 0f);
                lensNode.setRenderEffect(android.graphics.RenderEffect.createRuntimeShaderEffect(lensShader, "content"));
                int save = c.save();
                c.translate(0f, -extra);
                c.drawRenderNode(lensNode);
                c.restoreToCount(save);
                return true;
            } catch (Throwable error) {
                broken = true;
                Diary.note("island lens shader: " + error);
                return false;
            }
        }

        /** The shader set for a lens at L (in the row's coordinates; the node starts offset above the row). */
        private android.graphics.RuntimeShader shader(RectF L, float swell, float alpha, float opacity, float offset) {
            android.graphics.RuntimeShader lensShader = (android.graphics.RuntimeShader) shader;
            if (lensShader == null || builtLight != IslandMotion.lightLens()) { builtLight = IslandMotion.lightLens(); lensShader = new android.graphics.RuntimeShader(builtLight ? LENS_LIGHT : LENS_SHADER); shader = lensShader; }
            uniforms(lensShader, painter.unit, L, swell, alpha, opacity, offset);
            return lensShader;
        }

        /** Draws the lens at L, swollen by swell; false when it cannot (old Android, software canvas, an error). */
        boolean draw(Canvas c, Scene s, RectF L, float swell, float alpha) {
            if (broken || !available() || !c.isHardwareAccelerated() || s == null || L.width() <= 0f) return false;
            try {
                float unit = painter.unit, extra = 26f * unit;
                android.graphics.RenderNode content = (android.graphics.RenderNode) scene, out = (android.graphics.RenderNode) lens;
                if (content == null) { content = new android.graphics.RenderNode("ttcuz-lens-scene"); scene = content; }
                if (out == null) { out = new android.graphics.RenderNode("ttcuz-lens"); lens = out; }
                if (s != recorded) {
                    // the island once per flight: its edge, its icons and labels in the accent, no fill
                    recorded = s;
                    top = s.body.top - extra;
                    width = Math.max(1, (int) Math.ceil(s.body.right + s.body.left));
                    height = Math.max(1, (int) Math.ceil(s.body.height() + 2 * extra));
                    content.setPosition(0, 0, width, height);
                    Canvas r = content.beginRecording(width, height);
                    try { r.translate(0f, -top); painter.scene(r, s); } finally { content.endRecording(); }
                    out.setPosition(0, 0, width, height);
                    Canvas o = out.beginRecording(width, height);
                    try { o.drawRenderNode(content); } finally { out.endRecording(); }
                }
                android.graphics.RuntimeShader lensShader = (android.graphics.RuntimeShader) shader;
                if (lensShader == null || builtLight != IslandMotion.lightLens()) { builtLight = IslandMotion.lightLens(); lensShader = new android.graphics.RuntimeShader(builtLight ? LENS_LIGHT : LENS_SHADER); shader = lensShader; }
                float sw = Math.max(0f, Math.min(1f, swell));
                lensShader.setFloatUniform("center", L.centerX(), L.centerY() - top);
                lensShader.setFloatUniform("halfSize", L.width() / 2f, L.height() / 2f);
                lensShader.setFloatUniform("radius", Math.min(L.width(), L.height()) * (IslandMotion.round() ? .38f + .12f * sw : .5f));
                lensShader.setFloatUniform("magnify", 1f + .28f * sw);
                lensShader.setFloatUniform("bezel", Math.max(2f, Math.min(L.height() * .22f, 10f * unit)));
                lensShader.setFloatUniform("bend", sw);
                lensShader.setFloatUniform("rim", (IslandMotion.round() ? .18f + .82f * sw : .3f + .55f * sw) * alpha);
                lensShader.setFloatUniform("opacity", opacity(sw) * alpha);
                lensShader.setFloatUniform("hair", (IslandMotion.round() ? 1.8f : 1.3f) * unit);
                lensShader.setFloatUniform("veil", IslandMotion.round() ? .24f : .035f);
                out.setRenderEffect(android.graphics.RenderEffect.createRuntimeShaderEffect(lensShader, "content"));
                int save = c.save();
                c.translate(0f, top);
                c.drawRenderNode(out);
                c.restoreToCount(save);
                return true;
            } catch (Throwable error) {
                broken = true;
                Diary.note("island lens shader: " + error);
                return false;
            }
        }
    }

    /** The lens shader's uniforms for a lens at L (y moved by offset), swollen by swell. */
    static void uniforms(Object runtimeShader, float unit, RectF L, float swell, float alpha, float opacity, float offset) {
        android.graphics.RuntimeShader lensShader = (android.graphics.RuntimeShader) runtimeShader;
        float sw = Math.max(0f, Math.min(1f, swell));
        lensShader.setFloatUniform("center", L.centerX(), L.centerY() + offset);
        lensShader.setFloatUniform("halfSize", L.width() / 2f, L.height() / 2f);
        lensShader.setFloatUniform("radius", Math.min(L.width(), L.height()) * (IslandMotion.round() ? .38f + .12f * sw : .5f));
        lensShader.setFloatUniform("magnify", 1f + .28f * sw);
        lensShader.setFloatUniform("bezel", Math.max(2f, Math.min(L.height() * .22f, 10f * unit)));
        lensShader.setFloatUniform("bend", sw);
        boolean video = IslandMotion.round();
        // in the video's style the rim fades to the resting square's faint one as the bubble settles: no step at landing
        lensShader.setFloatUniform("rim", (video ? .18f + .82f * sw : .3f + .55f * sw) * alpha);
        lensShader.setFloatUniform("opacity", opacity);
        lensShader.setFloatUniform("hair", (video ? 1.8f : 1.3f) * unit);
        // the video's bubble is grey glass (about a quarter white over the dark island); the design's is nearly clear
        lensShader.setFloatUniform("veil", video ? .24f : .035f);
    }

    /**
     * The island as it is, taken once into a GPU picture (a hardware bitmap) when a tab is tapped: the real glass,
     * what is behind it and the icons. The lens then bends that picture on its own thread, so it moves on while TikTok
     * holds the main thread switching pages -- and with one shader draw, no off-screen buffers, which is what keeps
     * slow phones smooth. Android 10 and later (the picture is drawn by a renderer of its own, into an image reader).
     */
    static final class Snapshot {
        private Object renderer, reader, root; // HardwareRenderer, ImageReader, RenderNode
        private android.media.Image held;
        private int width, height;
        private boolean broken;

        android.graphics.Bitmap take(int w, int h, float extra, LensShader.Content content) { return take(w, h, extra, content, null); }

        /**
         * Two pictures in one: first in the top half, second in the bottom half (each h + 2 * extra tall), so the
         * glass and what is on it cost one render and one wait, not two.
         */
        android.graphics.Bitmap take(int w, int h, float extra, LensShader.Content content, LensShader.Content second) {
            if (broken || android.os.Build.VERSION.SDK_INT < 29 || w <= 0 || h <= 0) return null;
            try {
                int one = (int) Math.ceil(h + 2 * extra), full = second == null ? one : one * 2;
                if (reader == null || width != w || height != full) {
                    release();
                    android.media.ImageReader r = android.media.ImageReader.newInstance(w, full, android.graphics.PixelFormat.RGBA_8888, 3,
                            android.hardware.HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE | android.hardware.HardwareBuffer.USAGE_GPU_COLOR_OUTPUT);
                    android.graphics.HardwareRenderer renderer = new android.graphics.HardwareRenderer();
                    android.graphics.RenderNode node = new android.graphics.RenderNode("ttcuz-island-picture");
                    renderer.setContentRoot(node);
                    renderer.setOpaque(false);
                    renderer.setSurface(r.getSurface());
                    reader = r; this.renderer = renderer; root = node; width = w; height = full;
                }
                android.graphics.RenderNode node = (android.graphics.RenderNode) root;
                node.setPosition(0, 0, width, height);
                Canvas c = node.beginRecording(width, height);
                try {
                    c.drawColor(0, PorterDuff.Mode.CLEAR);
                    int k = c.save();
                    c.translate(0f, extra); content.draw(c);
                    c.restoreToCount(k);
                    if (second != null) { c.translate(0f, one + extra); second.draw(c); }
                } finally { node.endRecording(); }
                ((android.graphics.HardwareRenderer) renderer).createRenderRequest().setWaitForPresent(true).syncAndDraw();
                android.media.Image image = ((android.media.ImageReader) reader).acquireLatestImage();
                if (image == null) return null;
                // the last picture is let go only now: the lens may still be drawing it until it has the new one
                if (held != null) held.close();
                held = image;
                android.hardware.HardwareBuffer buffer = image.getHardwareBuffer();
                android.graphics.Bitmap picture = android.graphics.Bitmap.wrapHardwareBuffer(buffer, android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB));
                buffer.close();
                return picture;
            } catch (Throwable error) {
                broken = true;
                Diary.note("island picture: " + error);
                return null;
            }
        }

        void release() {
            try { if (held != null) held.close(); } catch (Throwable ignored) { }
            held = null;
            try { if (renderer != null) ((android.graphics.HardwareRenderer) renderer).destroy(); } catch (Throwable ignored) { }
            try { if (reader != null) ((android.media.ImageReader) reader).close(); } catch (Throwable ignored) { }
            renderer = reader = root = null;
        }
    }

    // ------------------------------------------------------------------ the surface

    /**
     * A transparent surface over the island, drawn from its own thread at the display's rate while the lens moves,
     * clear and idle otherwise. It reaches PAD above and below the island's row, so the swollen lens is not cut off;
     * drawing is in the row's coordinates. The lens's motion lives here too (a {@link Motion}), so it keeps moving
     * while the main thread is held up; the island reads it back ({@link #peek}) to hide what the lens covers.
     */
    static final class Flight extends SurfaceView implements SurfaceHolder.Callback, Choreographer.FrameCallback {
        static final float PAD_DP = 14f;
        final float pad;
        private final Object lock = new Object();
        private final Motion motion = new Motion();
        private final RectF lens = new RectF();
        private float cy, restHeight;
        private Scene scene;
        /** The island's picture the lens bends, and how far above the row it starts; null: the drawn scene instead. */
        private android.graphics.Bitmap picture, glassPicture;
        private float pictureTop;
        private Object pictureShader, glassShader, lensPaint; // BitmapShader, BitmapShader, RuntimeShader
        private final Paint glass = new Paint();
        private HandlerThread thread;
        private Handler handler;
        private Choreographer choreographer;
        private volatile SurfaceHolder holder;
        private long last;
        private boolean landed = true, clear = true, posted;
        /** Held by a finger: the lens stands where the island says, frame by frame, with no motion of its own. */
        private boolean holding;
        private final RectF held = new RectF();
        private float heldSwell;
        /** The display rate the surface asked for: 120 while something moves (when allowed), back to the system's at rest. */
        private float rate = -1f;
        private float alpha = 1f;
        private final Painter painter;
        private final LensShader shaderLens;

        Flight(Context context) {
            super(context);
            float unit = context.getResources().getDisplayMetrics().density;
            pad = PAD_DP * unit;
            painter = new Painter(unit);
            shaderLens = new LensShader(painter);
            setZOrderOnTop(true);
            getHolder().setFormat(PixelFormat.TRANSLUCENT);
            getHolder().addCallback(this);
            setFocusable(false);
            setClickable(false);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        /** True once the surface can draw: before that the island draws the lens itself. */
        boolean ready() { return holder != null && handler != null; }

        /**
         * Sends the lens from the capsule a to the capsule b (row coordinates). Called again before it lands, it only
         * changes where it goes: the lens carries on from where it is.
         */
        /** As {@link #fly(RectF, RectF, float, Scene)}, bending a picture of the island (taken by a {@link Snapshot}). */
        void fly(RectF a, RectF b, float strength, android.graphics.Bitmap island, android.graphics.Bitmap glass, float top) {
            synchronized (lock) {
                if (island != picture || glass != glassPicture) pictureShader = null;
                picture = island;
                glassPicture = glass;
                pictureTop = top;
            }
            fly(a, b, strength, (Scene) null);
        }

        /**
         * The lens held under a finger at `at` (row coordinates), swollen by `swell`: drawn on this surface's thread
         * from one picture of the island, so a drag costs the main thread nothing but this call.
         */
        void hold(RectF at, float swell, float strength, android.graphics.Bitmap island, android.graphics.Bitmap glass, float top) {
            synchronized (lock) {
                if (island != picture || glass != glassPicture) pictureShader = null;
                picture = island;
                glassPicture = glass;
                pictureTop = top;
                scene = null;
                holding = true;
                motion.live = false;
                held.set(at);
                heldSwell = swell;
                alpha = strength;
                landed = false;
            }
            request();
        }

        /** Whether a finger holds the lens here now. */
        boolean holding() { synchronized (lock) { return holding && !landed; } }

        void fly(RectF a, RectF b, float strength, Scene what) {
            synchronized (lock) {
                holding = false;
                if (what != null) picture = null;
                if (!motion.live) last = 0;
                motion.start(a.centerX(), a.width(), b.centerX(), b.width());
                cy = b.centerY();
                restHeight = b.height();
                alpha = strength;
                scene = what;
                landed = false;
            }
            request();
        }

        /** The island's edge the lens stays inside (the island's own coordinates, as the rects given to fly). */
        void bounds(RectF island) { synchronized (lock) { motion.bounds.set(island); } }

        /** The lens as it is now into out, and its swell; -1 when it has come to rest (the island may draw its capsule). */
        float peek(RectF out) {
            synchronized (lock) {
                if (landed) return -1f;
                motion.rect(out, cy, restHeight);
                return motion.live ? Math.min(1f, motion.s) : -1f;
            }
        }

        /** Taken over (held by a finger): the lens leaves the surface at once, wherever it was. */
        void stop() {
            synchronized (lock) { motion.live = false; holding = false; landed = true; }
            request();
        }

        /** The island drew the settled capsule itself: the surface clears on its next frame. */
        /**
         * The island has drawn the settled capsule: the lens's surface clears -- two frames later, not at once. The
         * surface and the island's window reach the screen on their own schedules, and clearing at once sometimes beat
         * the capsule to the screen by a frame: a frame with neither, a blink. The lens is all but invisible by then.
         */
        private boolean landing;
        void land() {
            synchronized (lock) { if (landed || motion.live || landing) return; landing = true; }
            Handler h = handler;
            if (h == null) { synchronized (lock) { landing = false; landed = true; } request(); return; }
            h.postDelayed(() -> {
                synchronized (lock) { landing = false; if (landed || motion.live) return; landed = true; }
                request();
            }, 34L);
        }

        boolean flying() { synchronized (lock) { return !landed; } }

        private void request() {
            Handler h = handler;
            if (h == null) return;
            h.post(() -> { if (!posted && choreographer != null) { posted = true; choreographer.postFrameCallback(this); } });
        }

        @Override public void doFrame(long frameTime) {
            posted = false;
            SurfaceHolder target = holder;
            if (target == null) return;
            boolean wipe;
            float swell;
            synchronized (lock) {
                float dt = last == 0 ? 1f / 120f : (frameTime - last) / 1e9f;
                last = frameTime;
                wipe = landed;
                if (holding) {
                    lens.set(held);
                    swell = Math.min(1f, heldSwell);
                } else {
                    motion.step(Math.max(0f, dt));
                    motion.rect(lens, cy, restHeight);
                    swell = Math.min(1f, motion.s);
                }
            }
            frameRate(target, !wipe);
            if (wipe && clear) return;
            Canvas canvas = null;
            try {
                canvas = android.os.Build.VERSION.SDK_INT >= 26 ? target.getSurface().lockHardwareCanvas() : target.lockCanvas();
                if (canvas == null) return;
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                if (!wipe) {
                    canvas.translate(0, pad);
                    Scene what;
                    float a;
                    android.graphics.Bitmap island;
                    float top;
                    android.graphics.Bitmap under;
                    synchronized (lock) { what = scene; a = alpha; island = picture; under = glassPicture; top = pictureTop; }
                    if (island != null && under != null && bend(canvas, island, under, top, swell, a)) { /* one shader draw */ }
                    else if (what != null && !shaderLens.draw(canvas, what, lens, swell, a)) painter.lens(canvas, lens, what, a * opacity(swell));
                }
                clear = wipe;
            } catch (Throwable error) {
                return;
            } finally {
                if (canvas != null) try { target.getSurface().unlockCanvasAndPost(canvas); } catch (Throwable ignored) { }
            }
            // at rest it keeps its last frame (a faint rim over the capsule) until the island has drawn the capsule
            boolean moving;
            synchronized (lock) { moving = motion.live; }
            if (!wipe && moving) { posted = true; choreographer.postFrameCallback(this); }
        }

        /**
         * 120 Hz while the lens moves, when the setting allows it and the screen can; the system's own rate once it is
         * still, so a resting island never keeps the screen fast (and the battery busy).
         */
        private void frameRate(SurfaceHolder target, boolean moving) {
            if (android.os.Build.VERSION.SDK_INT < 30) return;
            float want = moving && NavigationIsland.fast() ? 120f : 0f;
            if (want == rate) return;
            rate = want;
            try { target.getSurface().setFrameRate(want, android.view.Surface.FRAME_RATE_COMPATIBILITY_DEFAULT); }
            catch (Throwable ignored) { }
        }

        /** The lens as one draw: the shader reads the island's picture directly, no off-screen buffer. */
        private boolean bend(Canvas canvas, android.graphics.Bitmap island, android.graphics.Bitmap under, float top, float swell, float alpha) {
            if (android.os.Build.VERSION.SDK_INT < 33 || !Ttcuz.is64()) return false;
            try {
                android.graphics.BitmapShader source = (android.graphics.BitmapShader) pictureShader;
                android.graphics.RuntimeShader bent = (android.graphics.RuntimeShader) lensPaint;
                if (source == null) {
                    // one picture holding both: the glass above, what is on it below
                    float split = under == island ? island.getHeight() / 2f : 0f;
                    android.graphics.Matrix glassAt = new android.graphics.Matrix(), itemsAt = new android.graphics.Matrix();
                    glassAt.setTranslate(0f, -top);
                    itemsAt.setTranslate(0f, -top - split);
                    source = new android.graphics.BitmapShader(island, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    source.setLocalMatrix(itemsAt);
                    android.graphics.BitmapShader glassSource = new android.graphics.BitmapShader(under, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    glassSource.setLocalMatrix(glassAt);
                    pictureShader = source;
                    glassShader = glassSource;
                    bent = null;
                }
                if (bent == null) {
                    bent = new android.graphics.RuntimeShader(IslandMotion.lightLens() ? LENS_OVER_GLASS_LIGHT : LENS_OVER_GLASS);
                    bent.setInputShader("content", source);
                    bent.setInputShader("back", (android.graphics.BitmapShader) glassShader);
                    lensPaint = bent;
                }
                uniforms(bent, painter.unit, lens, swell, alpha, 1f, 0f);
                glass.setShader(bent);
                canvas.drawRect(lens.left - 2f, lens.top - 2f, lens.right + 2f, lens.bottom + 2f, glass);
                return true;
            } catch (Throwable error) {
                Diary.note("island lens picture: " + error);
                synchronized (lock) { picture = null; glassPicture = null; }
                return false;
            }
        }

        @Override public void surfaceCreated(SurfaceHolder created) {
            if (thread == null) {
                thread = new HandlerThread("ttcuz-island", android.os.Process.THREAD_PRIORITY_DISPLAY);
                thread.start();
                handler = new Handler(thread.getLooper());
                handler.post(() -> choreographer = Choreographer.getInstance());
            }
            holder = created;
            clear = false;
            request();
        }

        @Override public void surfaceChanged(SurfaceHolder changed, int format, int width, int height) { holder = changed; clear = false; request(); }

        @Override public void surfaceDestroyed(SurfaceHolder destroyed) {
            holder = null;
            Handler h = handler;
            // wait for a frame that may be drawing into it right now
            if (h != null) { final Object done = new Object(); synchronized (done) { h.post(() -> { synchronized (done) { done.notify(); } }); try { done.wait(100); } catch (InterruptedException ignored) { } } }
        }

        @Override protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            HandlerThread t = thread;
            thread = null;
            handler = null;
            choreographer = null;
            if (t != null) t.quitSafely();
        }
    }
}
