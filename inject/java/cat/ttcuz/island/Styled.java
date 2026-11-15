package cat.ttcuz.island;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A complete MD3 island, changed by setting its fields -- usually in the constructor of a class that extends it:
 * <pre>
 * public class MyIsland extends Styled {
 *     public MyIsland() {
 *         radius = 8;
 *         indicator = LINE;
 *         createShape = CIRCLE;
 *     }
 * }
 * </pre>
 * Every method can be overridden too: call super.draw(canvas, bar) and draw over it, or replace it entirely.
 * Sizes are dp, places inside the bar are fractions 0..1, shaders are AGSL (Android 13+; the colours are used before).
 */
public class Styled extends Island {
    public static final String NONE = "none";
    /** labels */
    public static final String BELOW = "below", BESIDE = "beside", SELECTED = "selected";
    /** indicator */
    public static final String PILL = "pill", CIRCLE = "circle", LINE = "line", DOT = "dot";
    /** createShape (and CIRCLE, PILL, NONE) */
    public static final String SUNNY = "sunny", SQUARE = "square", SQUIRCLE = "squircle";
    /** easing */
    public static final String SPRING = "spring", SMOOTH = "smooth", LINEAR = "linear", BOUNCE = "bounce";

    // ------------------------------------------------------------ the bar
    /** Height (0: TikTok's), distance from the screen's sides, lift above the bottom; corner radius (-1: capsule). */
    public float barHeight = 0, side = 12, lift = 6, radius = -1;
    /** The blurred video under the bar (when blur is on in the settings). */
    public boolean backdrop = true;
    public Ink fill = Ink.CARD.opacity(.72f), stroke = null, shadow = null;
    public float strokeWidth = 1, shadowRadius = 14, shadowY = 4;
    /** AGSL that fills the bar instead of fill; AGSL over the whole bar ("uniform shader content;"). */
    public String barShader, effect;

    // ------------------------------------------------------------ the buttons
    /** Which buttons there are, left to right: "home", "friends", "create", "inbox", "profile". */
    public String[] order = {"home", "friends", "create", "inbox", "profile"};
    /** Where each button of order stands, as a fraction of the bar's width (null: evenly). */
    public float[] positions;
    public float buttonY = .5f, iconSize = 24, pressScale = .88f, selectedScale = 1f;
    public Ink iconColor = Ink.MUTED, iconSelected = Ink.ACCENT;
    public boolean filledSelected = true;
    /** NONE, BELOW, SELECTED or BESIDE. */
    public String labels = NONE;
    public float labelSize = 11;
    public Ink labelColor = Ink.MUTED, labelSelected = Ink.TEXT;

    // ------------------------------------------------------------ the selection
    /** PILL, CIRCLE, LINE, DOT or NONE. */
    public String indicator = PILL;
    /** Width, height (-1: by shape), radius (-1: half the height), place in the bar's height (-1: by shape), stretch in flight. */
    public float indWidth = 56, indHeight = -1, indRadius = -1, indY = -1, stretch = .35f;
    public Ink indColor = Ink.ACCENT.opacity(.28f);
    public String indShader;

    // ------------------------------------------------------------ the create button
    /** SUNNY, CIRCLE, SQUARE, SQUIRCLE, PILL or NONE. */
    public String createShape = SUNNY;
    public float createSize = 44, plusSize = 24, spin = 45;
    public Ink createColor = Ink.ACCENT, plusColor = Ink.ON_ACCENT;
    public String createShader;

    // ------------------------------------------------------------ motion
    public long duration = 320;
    /** SPRING, SMOOTH, LINEAR or BOUNCE. */
    public String easing = SPRING;

    /** A colour: fixed, or one of the theme's (it follows the accent and the dark or light theme), with an opacity. */
    public static final class Ink {
        private static final int FIXED = 0, ACC = 1, ON_ACC = 2, TXT = 3, MUT = 4, CRD = 5;
        public static final Ink ACCENT = new Ink(ACC, 0, 1f), ON_ACCENT = new Ink(ON_ACC, 0, 1f), TEXT = new Ink(TXT, 0, 1f),
                MUTED = new Ink(MUT, 0, 1f), CARD = new Ink(CRD, 0, 1f);
        private final int kind, fixed;
        private final float opacity;
        private Ink(int kind, int fixed, float opacity) { this.kind = kind; this.fixed = fixed; this.opacity = opacity; }
        /** A fixed ARGB colour, like 0xFF1C1F2A. */
        public static Ink of(int argb) { return new Ink(FIXED, argb, 1f); }
        /** The same colour with its opacity multiplied (0..1). */
        public Ink opacity(float value) { return new Ink(kind, fixed, opacity * Math.max(0f, Math.min(1f, value))); }
        public int colour(Bar bar) {
            int base = kind == ACC ? bar.accent : kind == ON_ACC ? Bar.on(bar.accent) : kind == TXT ? bar.text
                    : kind == MUT ? bar.muted : kind == CRD ? bar.card : fixed;
            return opacity >= 1f ? base : Bar.fade(base, opacity);
        }
    }

    /** A shader's source with the uniforms it declares (only those are set). */
    static final class Shader {
        private static final Pattern UNIFORM = Pattern.compile("uniform\\s+\\w+\\s+(\\w+)");
        final String agsl;
        final Set<String> uniforms = new HashSet<>();
        Shader(String agsl) { this.agsl = agsl; Matcher m = UNIFORM.matcher(agsl); while (m.find()) uniforms.add(m.group(1)); }
    }

    private final Map<String, Shader> specs = new HashMap<>();
    private Shader spec(String agsl) { Shader s = specs.get(agsl); if (s == null) { s = new Shader(agsl); specs.put(agsl, s); } return s; }

    private float indH() { return indHeight > 0 ? indHeight : indicator.equals(LINE) ? 3 : indicator.equals(DOT) ? 5 : 32; }
    private float indYAt() { return indY >= 0 ? indY : indicator.equals(LINE) ? .92f : indicator.equals(DOT) ? .86f : .5f; }

    private static final String[] ROLES = {"home", "friends", "create", "inbox", "profile"};
    private final int[] slotOf = new int[5];
    private int slots;
    private boolean always;
    private Bar sunnyOf;

    /** order → each role's slot, and whether any shader moves with time; read again every frame (cheap). */
    private void prepare() {
        java.util.Arrays.fill(slotOf, -1);
        slots = 0;
        if (order != null) for (String name : order) for (int r = 0; r < 5; r++) if (ROLES[r].equals(name) && slotOf[r] < 0) slotOf[r] = slots++;
        always = timed(barShader) || timed(effect) || timed(indShader) || timed(createShader);
    }

    private boolean timed(String agsl) { return agsl != null && spec(agsl).uniforms.contains("time"); }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG), line = new Paint(Paint.ANTI_ALIAS_FLAG),
            shade = new Paint(Paint.ANTI_ALIAS_FLAG), ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF(), ind = new RectF();
    private final Path sunny = new Path();
    private final android.graphics.Matrix move = new android.graphics.Matrix();
    private float sunnyFor = -1f;
    /** Each tab's centre this frame, and the selection moving between them. */
    private final float[] tabX = new float[5];
    private int lastSelected = -2, before = -1;
    private float fromX, fromW, toX, toW, nowX, nowW, t = 1f;
    private long start;
    private float spinFrom, spinTo, spinNow;
    private long spinStart;
    private boolean createWasDown;
    /** The frame the whole-bar shader was put on, and the shader there. */
    private Bar effectFrame;
    private Object effectMade;
    private float shownY;

    private float dp(Bar f, float v) { return v * f.density; }

    private float ease(float x) {
        if (x >= 1f) return 1f;
        switch (easing) {
            case "linear": return x;
            case "smooth": { float u = 1f - x; return 1f - u * u * u; }
            case "bounce": {
                float n = 7.5625f, d = 2.75f;
                if (x < 1 / d) return n * x * x;
                if (x < 2 / d) { x -= 1.5f / d; return n * x * x + .75f; }
                if (x < 2.5 / d) { x -= 2.25f / d; return n * x * x + .9375f; }
                x -= 2.625f / d; return n * x * x + .984375f;
            }
            default: return (float) (1 - Math.exp(-6.5 * x) * Math.cos(8.5 * x));
        }
    }

    private boolean shown(Bar f, int tab) { return tab >= 0 && tab < f.count && f.role[tab] >= 0 && slotOf[f.role[tab]] >= 0; }

    @Override public void layout(Bar f) {
        prepare();
        // the bar: a height of its own, or the mod's
        if (barHeight > 0) {
            float h = Math.min(dp(f, barHeight), f.height), bottom = Math.max(h, f.height - dp(f, lift));
            f.body.set(dp(f, side), bottom - h, f.width - dp(f, side), bottom);
        } else f.body.set(dp(f, side), f.body.top, f.width - dp(f, side), f.body.bottom);
        f.radius = radius < 0 ? f.body.height() / 2f : Math.min(dp(f, radius), f.body.height() / 2f);
        RectF b = f.body;
        float y = b.top + b.height() * buttonY;
        shownY = labels.equals("below") ? y - dp(f, labelSize) * .62f : y;
        // each button on its slot, the hidden ones nowhere
        for (int i = 0; i < 5; i++) {
            // a button left out of the order is gone, its unread count with it (moved off the bar)
            if (!shown(f, i)) { f.hit[i].setEmpty(); tabX[i] = -1; f.x[i] = -10000f; f.y[i] = -10000f; continue; }
            int slot = slotOf[f.role[i]];
            float x = positions != null && slot < positions.length ? b.left + b.width() * positions[slot] : b.left + b.width() * (slot + .5f) / slots;
            tabX[i] = x; f.x[i] = x; f.y[i] = i == f.create ? y : shownY;
        }
        // tap areas: halfway to the neighbours, the bar's whole height
        for (int i = 0; i < 5; i++) {
            if (tabX[i] < 0) continue;
            float left = b.left, right = b.right;
            for (int j = 0; j < 5; j++) {
                if (j == i || tabX[j] < 0) continue;
                if (tabX[j] < tabX[i]) left = Math.max(left, (tabX[i] + tabX[j]) / 2f);
                else if (tabX[j] > tabX[i]) right = Math.min(right, (tabX[i] + tabX[j]) / 2f);
            }
            f.hit[i].set(left, 0, right, f.height);
        }
        // the selection: where it goes, how wide, and the motion there
        int sel = shown(f, f.selected) && f.selected != f.create ? f.selected : -1;
        float wantX = sel >= 0 ? tabX[sel] : nowX, wantW = sel >= 0 ? width(f, sel) : nowW;
        if (sel != lastSelected || Math.abs(wantX - toX) > .5f && t >= 1f) {
            boolean first = lastSelected == -2 || nowW <= 0f || duration == 0;
            fromX = first ? wantX : nowX; fromW = first ? wantW : nowW;
            toX = wantX; toW = wantW; start = f.time; t = first ? 1f : 0f;
            before = first ? -1 : lastSelected; lastSelected = sel;
        } else if (t >= 1f) { toX = wantX; toW = wantW; }
        t = duration == 0 ? 1f : Math.min(1f, (f.time - start) / (float) duration);
        float e = ease(t);
        nowX = fromX + (toX - fromX) * e;
        nowW = fromW + (toW - fromW) * e + (float) Math.sin(Math.PI * Math.min(1f, t)) * Math.abs(toX - fromX) * stretch;
        // the create button turns a step on each press
        boolean down = f.create >= 0 && f.pressed[f.create];
        if (createWasDown && !down) { spinFrom = spinNow; spinTo += spin; spinStart = f.time; }
        createWasDown = down;
        float st = Math.min(1f, (f.time - spinStart) / 420f);
        spinNow = spinFrom + (spinTo - spinFrom) * ease(st);
        f.animate(always || t < 1f || st < 1f || down);
        // a whole-bar shader: put on once per bar, its uniforms each frame
        if (effect != null) {
            if (effectFrame != f) { effectFrame = f; effectMade = f.effect(effect); }
            if (effectMade != null) uniforms(f, effectMade, spec(effect), f.width, f.height);
        }
    }

    /** The selection's width under a tab: its own, or, with labels beside, the icon and the label in a capsule. */
    private float width(Bar f, int tab) {
        if (indicator.equals("circle") || indicator.equals("dot")) return dp(f, indH());
        if (labels.equals("beside") && f.label[tab] != null) {
            ink.setTextSize(dp(f, labelSize)); ink.setTypeface(Typeface.DEFAULT_BOLD);
            return Math.max(dp(f, indWidth), dp(f, 12 + iconSize + 6 + 14) + ink.measureText(f.label[tab]));
        }
        return dp(f, indWidth);
    }

    private float weight(Bar f, int tab) {
        float e = Math.max(0f, Math.min(1f, ease(t)));
        if (tab == lastSelected) return e;
        // the one left fades as the new one fills
        if (tab == before) return 1f - e;
        return 0f;
    }

    @Override public void draw(Canvas canvas, Bar f) {
        RectF b = f.body;
        float r = f.radius;
        // the bar: its shadow, what is behind it, its fill or shader, its edge
        if (shadow != null) {
            shade.setColor(shadow.colour(f));
            shade.setShadowLayer(dp(f, shadowRadius), 0, dp(f, shadowY), shadow.colour(f));
            canvas.drawRoundRect(b, r, r, shade);
        }
        if (backdrop) f.drawBackdrop(canvas, b, r);
        if (barShader != null && fillShader(f, spec(barShader), b)) canvas.drawRoundRect(b, r, r, paint);
        else if (fill != null) { paint.setShader(null); paint.setColor(fill.colour(f)); canvas.drawRoundRect(b, r, r, paint); }
        if (stroke != null && strokeWidth > 0) {
            line.setStyle(Paint.Style.STROKE); line.setStrokeWidth(dp(f, strokeWidth)); line.setColor(stroke.colour(f));
            float h = line.getStrokeWidth() / 2f; rect.set(b.left + h, b.top + h, b.right - h, b.bottom - h);
            canvas.drawRoundRect(rect, r - h, r - h, line);
        }
        // the selection
        if (lastSelected >= 0 && !indicator.equals("none")) {
            float h = dp(f, indH()), cy = b.top + b.height() * indYAt(), w = nowW;
            ind.set(nowX - w / 2f, cy - h / 2f, nowX + w / 2f, cy + h / 2f);
            float round = indRadius < 0 ? h / 2f : dp(f, indRadius);
            if (indShader != null && fillShader(f, spec(indShader), ind)) canvas.drawRoundRect(ind, round, round, paint);
            else if (indColor != null) { paint.setShader(null); paint.setColor(indColor.colour(f)); canvas.drawRoundRect(ind, round, round, paint); }
        }
        // the buttons, and their names
        float size = dp(f, iconSize);
        ink.setTextAlign(Paint.Align.CENTER); ink.setTextSize(dp(f, labelSize)); ink.setTypeface(Typeface.DEFAULT_BOLD);
        for (int i = 0; i < f.count; i++) {
            if (i == f.create || tabX[i] < 0) continue;
            float w = weight(f, i), x = tabX[i], y = shownY;
            boolean beside = labels.equals("beside") && i == lastSelected && f.label[i] != null;
            if (beside) x = x + (ind.left + dp(f, 12) + size / 2f - x) * w;
            f.x[i] = x;
            if (!f.picture[i]) {
                Path path = filledSelected && w > .5f ? f.iconFilled[i] : f.icon[i];
                if (path == null) path = f.iconFilled[i];
                if (path != null) {
                    float scale = (1f + (selectedScale - 1f) * w) * (f.pressed[i] ? pressScale : 1f), s = size * scale;
                    int save = canvas.save();
                    canvas.translate(x - s / 2f, y - s / 2f); canvas.scale(s / 24f, s / 24f);
                    paint.setShader(null);
                    paint.setColor(Bar.mix(iconColor.colour(f), iconSelected.colour(f), w));
                    canvas.drawPath(path, paint);
                    canvas.restoreToCount(save);
                }
            }
            String label = f.label[i];
            if (label == null) continue;
            int colour = Bar.mix(labelColor.colour(f), labelSelected.colour(f), w);
            if (labels.equals("below") || labels.equals("selected") && w > 0f) {
                ink.setColor(labels.equals("selected") ? (colour & 0xFFFFFF) | (Math.round(Color.alpha(colour) * w) << 24) : colour);
                canvas.drawText(label, tabX[i], y + size / 2f + dp(f, labelSize) + dp(f, 1), ink);
            } else if (beside && w > .3f) {
                ink.setTextAlign(Paint.Align.LEFT);
                ink.setColor((colour & 0xFFFFFF) | (Math.round(Color.alpha(colour) * Math.min(1f, (w - .3f) / .7f)) << 24));
                int save = canvas.save(); canvas.clipRect(ind);
                canvas.drawText(label, x + size / 2f + dp(f, 6), y - (ink.descent() + ink.ascent()) / 2f, ink);
                canvas.restoreToCount(save);
                ink.setTextAlign(Paint.Align.CENTER);
            }
        }
        // the create button
        if (f.create >= 0 && tabX[f.create] >= 0 && !createShape.equals("none")) {
            float s = dp(f, createSize) * (f.pressed[f.create] ? pressScale : 1f), cx = tabX[f.create], cy = b.top + b.height() * buttonY;
            int save = canvas.save();
            canvas.translate(cx, cy);
            canvas.rotate(spinNow);
            rect.set(-s / 2f, -s / 2f, s / 2f, s / 2f);
            sunnyOf = f;
            Path shape = shape(s);
            if (createShader == null || !fillShader(f, spec(createShader), rect)) { paint.setShader(null); paint.setColor(createColor.colour(f)); }
            drawShape(canvas, shape, s);
            paint.setShader(null);
            canvas.rotate(-spinNow);
            if (f.plus != null) {
                float p = dp(f, plusSize);
                canvas.translate(-p / 2f, -p / 2f); canvas.scale(p / 24f, p / 24f);
                paint.setColor(plusColor.colour(f));
                canvas.drawPath(f.plus, paint);
            }
            canvas.restoreToCount(save);
        }
    }

    private void drawShape(Canvas canvas, Path shape, float s) {
        if (shape != null) { canvas.drawPath(shape, paint); return; }
        float round = createShape.equals("square") ? s * .24f : createShape.equals("squircle") ? s * .38f : s / 2f;
        if (createShape.equals("pill")) rect.set(-s * .7f, -s / 2f, s * .7f, s / 2f);
        canvas.drawRoundRect(rect, round, round, paint);
    }

    /** The Sunny of MD3 for "sunny"; null for the shapes drawn as rounded rects. */
    private Path shape(float size) {
        if (!createShape.equals("sunny")) return null;
        if (size != sunnyFor) {
            sunnyFor = size; sunny.reset();
            sunny.set(sunnyOf.sunny(size));
        }
        return sunny;
    }

    /** Puts the shader on the paint for a shape at `area` (its p is the shape's own, from its top left). */
    private boolean fillShader(Bar f, Shader shader, RectF area) {
        if (!fillShader(f, shader, area.width(), area.height())) return false;
        move.setTranslate(area.left, area.top);
        ((android.graphics.Shader) paint.getShader()).setLocalMatrix(move);
        return true;
    }

    private boolean fillShader(Bar f, Shader shader, float w, float h) {
        Object made = f.shader(shader.agsl);
        if (made == null) { paint.setShader(null); return false; }
        uniforms(f, made, shader, w, h);
        paint.setShader((android.graphics.Shader) made);
        paint.setColor(0xFFFFFFFF);
        return true;
    }

    /** The uniforms every island shader may declare: size, time, progress, selected, pressed, dark, accent. */
    private void uniforms(Bar f, Object made, Shader shader, float w, float h) {
        set(f, made, shader, "size", w, h);
        set(f, made, shader, "time", (f.time % 3_600_000L) / 1000f);
        set(f, made, shader, "progress", Math.max(0f, Math.min(1f, ease(t))));
        set(f, made, shader, "selected", lastSelected < 0 ? -1 : slotOf[Math.max(0, f.role[lastSelected])]);
        set(f, made, shader, "pressed", anyPressed(f) ? 1f : 0f);
        set(f, made, shader, "dark", f.dark ? 1f : 0f);
        int a = f.accent;
        set(f, made, shader, "accent", Color.red(a) / 255f, Color.green(a) / 255f, Color.blue(a) / 255f, 1f);
    }

    private static boolean anyPressed(Bar f) { for (boolean one : f.pressed) if (one) return true; return false; }

    private static void set(Bar f, Object shader, Shader spec, String name, float... values) {
        if (spec.uniforms.contains(name)) f.uniform(shader, name, values);
    }

    @Override public void onRelease() { effectFrame = null; effectMade = null; lastSelected = -2; }

}
