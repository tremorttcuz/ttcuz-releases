package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * The shape a round picture is cut to.
 *
 * TikTok does not cut avatars round itself: the image loader does, and it
 * does it in three drawables that all come to the same line -- add a circle
 * to a path, then fill the path with the picture. The build sends that one
 * call here, and what is added instead is whatever shape was chosen. The
 * picture is still drawn by the loader, with its own smoothing and its own
 * border, which now follows the shape because it is built by the same call.
 *
 * It is every picture the loader cuts round, not only faces: the loader does
 * not know what it is drawing, and nearly everything round in the app is an
 * avatar.
 */
public final class AvatarShape {

    private AvatarShape() {}

    static final String KEY = "avatar_shape";
    /** Material 3 Expressive's shape set, plus the rounded square kept from before. */
    static final String[] ALL = {
            "circle", "rounded", "square", "slanted", "arch", "fan", "semicircle", "oval", "pill",
            "soft_triangle", "diamond", "pentagon", "gem", "very_sunny", "sun",
            "cookie4", "cookie6", "cookie7", "cookie", "cookie12", "clover", "clover8",
            "burst", "soft_burst", "boom", "soft_boom", "flower", "puffy", "puffy_diamond", "heart"};

    /** Shapes that are not in Material 3 any more, and the one each became. */
    private static String renamed(String name) {
        switch (name) {
            case "hexagon": return "gem";
            case "tulip": return "arch";
            case "pebble": return "puffy";
            case "squircle": return "rounded";
            default: return name;
        }
    }

    /** How finely an outline given as a distance in each direction is traced. */
    private static final int POINTS = 144;

    /** -1 until the setting has been read. */
    private static volatile int chosen = -1;
    private static final Path[] UNIT = new Path[ALL.length];
    private static final Matrix PLACE = new Matrix();

    static String chosen() {
        return ALL[index()];
    }

    static void choose(Context context, String shape) {
        int found = 0;
        for (int i = 0; i < ALL.length; i++) if (ALL[i].equals(shape)) found = i;
        chosen = found;
        version++;
        context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY, ALL[found]).apply();
    }

    /** Counts the changes of shape, so each screen redraws its avatars once after one. */
    private static volatile int version;
    private static final int SEEN = 0x7e0c03b0;

    /**
     * A screen came to the front: if the shape changed since it last showed, every picture on it works out its
     * outline again. The image loader cuts a picture to its shape only when the picture's bounds change, so the
     * avatars already on screen kept the old one until they were loaded again.
     */
    static void resumed(android.app.Activity activity) {
        try {
            android.view.View decor = activity.getWindow().getDecorView();
            Object seen = decor.getTag(SEEN);
            if (seen instanceof Integer && (Integer) seen == version) return;
            decor.setTag(SEEN, version);
            if (version == 0) return;
            decor.post(() -> reshape(decor, 0));
        } catch (Throwable error) {
            Diary.note("avatar shape refresh: " + error);
        }
    }

    private static void reshape(android.view.View view, int depth) {
        if (depth > 40) return;
        if (view instanceof android.widget.ImageView) {
            android.graphics.drawable.Drawable d = ((android.widget.ImageView) view).getDrawable();
            if (d != null) {
                android.graphics.Rect was = new android.graphics.Rect(d.getBounds());
                if (!was.isEmpty()) { d.setBounds(was.left, was.top, was.right + 1, was.bottom); d.setBounds(was); }
            }
            view.invalidate();
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) reshape(group.getChildAt(i), depth + 1);
        }
    }

    static void reloadSettings() {
        chosen = -1;
    }

    private static int index() {
        int known = chosen;
        if (known >= 0) return known;
        Context context = Ttcuz.context();
        if (context == null) return 0;  // too early to know; a circle, and ask again
        int found = 0;
        try {
            SharedPreferences prefs = context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
            String name = renamed(prefs.getString(KEY, ALL[0]));
            for (int i = 0; i < ALL.length; i++) if (ALL[i].equals(name)) found = i;
        } catch (Throwable ignored) { }
        chosen = found;
        return found;
    }

    /** What the image loader calls where it used to add a circle to its path. */
    public static void addCircle(Path path, float x, float y, float radius, Path.Direction direction) {
        int shape = index();
        if (shape > 0 && radius > 0) {
            try {
                Path unit = unit(shape);
                synchronized (PLACE) {
                    PLACE.setScale(radius, radius);
                    PLACE.postTranslate(x, y);
                    path.addPath(unit, PLACE);
                }
                return;
            } catch (Throwable error) {
                // a shape that cannot be drawn is a circle
            }
        }
        path.addCircle(x, y, radius, direction);
    }

    // ------------------------------------------------------- the story ring

    private static final Path RING = new Path();
    private static final Path PART = new Path();
    private static final Matrix RING_PLACE = new Matrix();

    /**
     * What the view that draws a story ring calls where it used to draw an arc.
     *
     * The ring is not a picture and is not cut out of anything: it is a view
     * of its own over the avatar that strokes an arc round an oval, all the
     * way round for one story and in pieces for several or while one loads.
     * So the arc is what takes the avatar's shape -- the outline of the
     * chosen shape fitted to the same oval, or as much of it as the arc swept,
     * drawn with the same paint and so in the same colours.
     */
    public static void drawArc(Canvas canvas, RectF oval, float start, float sweep,
                               boolean useCentre, Paint paint) {
        int shape = index();
        if (shape > 0 && !useCentre && oval != null && sweep != 0) {
            try {
                synchronized (RING) {
                    RING.rewind();
                    RING_PLACE.setScale(oval.width() / 2f, oval.height() / 2f);
                    RING_PLACE.postTranslate(oval.centerX(), oval.centerY());
                    RING.addPath(unit(shape), RING_PLACE);
                    if (Math.abs(sweep) >= 359.5f) {
                        canvas.drawPath(RING, paint);
                        return;
                    }
                    // a piece of it: as far round the outline as the arc was round the circle.
                    // An arc is measured from three o'clock; the outlines here begin at the top.
                    android.graphics.PathMeasure measure = new android.graphics.PathMeasure(RING, true);
                    float length = measure.getLength();
                    float from = start + (sweep < 0 ? sweep : 0) + 90f;
                    from = ((from % 360f) + 360f) % 360f / 360f * length;
                    float span = Math.min(Math.abs(sweep), 360f) / 360f * length;
                    PART.rewind();
                    if (from + span <= length) {
                        measure.getSegment(from, from + span, PART, true);
                    } else {
                        measure.getSegment(from, length, PART, true);
                        measure.getSegment(0, from + span - length, PART, true);
                    }
                    canvas.drawPath(PART, paint);
                    return;
                }
            } catch (Throwable error) {
                // a ring left round is better than no ring
            }
        }
        canvas.drawArc(oval, start, sweep, useCentre, paint);
    }

    // ------------------------------------------------------- the live ring

    private static final Path LIVE = new Path();
    private static final Matrix LIVE_PLACE = new Matrix();

    /**
     * What TikTok's live ring calls where it used to draw a circle: the ring round the avatar of someone who is live
     * (in profiles, lists, the inbox and the notifications -- one view draws it everywhere), and the pulse that grows
     * out of it. The outline of the chosen shape is stroked with the same paint instead, so the ring sits on the
     * avatar's edge whatever its shape. Built from the shape kept in memory: a few microseconds a frame.
     */
    public static void drawCircle(Canvas canvas, float x, float y, float radius, Paint paint) {
        int shape = index();
        if (shape > 0 && radius > 0) {
            try {
                synchronized (LIVE) {
                    LIVE.rewind();
                    LIVE_PLACE.setScale(radius, radius);
                    LIVE_PLACE.postTranslate(x, y);
                    LIVE.addPath(unit(shape), LIVE_PLACE);
                    canvas.drawPath(LIVE, paint);
                    return;
                }
            } catch (Throwable error) {
                // a round ring is better than none
            }
        }
        canvas.drawCircle(x, y, radius, paint);
    }

    /** A shape in a square two across, centred on nothing. Built once and never changed. */
    static synchronized Path unit(int shape) {
        Path known = UNIT[shape];
        if (known != null) return known;
        Path made = new Path();
        switch (ALL[shape]) {
            case "rounded":
                made.addRoundRect(new RectF(-1, -1, 1, 1), 0.44f, 0.44f, Path.Direction.CW);
                break;
            case "square":
                // Material's square is soft-cornered too, only less than the rounded one
                made.addRoundRect(new RectF(-1, -1, 1, 1), 0.3f, 0.3f, Path.Direction.CW);
                break;
            case "slanted": {
                made.addRoundRect(new RectF(-0.86f, -1, 0.86f, 1), 0.32f, 0.32f, Path.Direction.CW);
                Matrix skew = new Matrix();
                skew.setSkew(-0.16f, 0f);
                made.transform(skew);
                break;
            }
            case "semicircle":
                made.addRoundRect(new RectF(-1, -0.66f, 1, 0.66f),
                        new float[]{1f, 1.3f, 1f, 1.3f, 0.2f, 0.2f, 0.2f, 0.2f}, Path.Direction.CW);
                break;
            case "oval": {
                made.addOval(new RectF(-1.08f, -0.72f, 1.08f, 0.72f), Path.Direction.CW);
                Matrix turn = new Matrix();
                turn.setRotate(-45f);
                made.transform(turn);
                break;
            }
            case "pill":
                made.addRoundRect(new RectF(-0.78f,-1f,0.78f,1f),0.78f,0.78f,Path.Direction.CW);break;
            case "arch":
                made.moveTo(-1,1);made.lineTo(-1,0);made.cubicTo(-1,-1.33f,1,-1.33f,1,0);made.lineTo(1,1);made.close();break;
            case "tulip":
                made.moveTo(-1,-1);made.cubicTo(-0.5f,-0.7f,-0.25f,-0.7f,0,-1);
                made.cubicTo(0.25f,-0.7f,0.5f,-0.7f,1,-1);made.lineTo(1,0);
                made.cubicTo(1,1.33f,-1,1.33f,-1,0);made.close();break;
            case "fan":
                made.moveTo(-1,1);made.lineTo(-1,0);made.cubicTo(-1,-1.33f,1,-1.33f,1,0);
                made.lineTo(1,1);made.quadTo(0,0.45f,-1,1);made.close();break;
            case "pebble":
                made.moveTo(0,-1);made.cubicTo(1,-1,1.1f,0.2f,0.65f,0.8f);
                made.cubicTo(0.1f,1.25f,-1,0.85f,-1,0);made.cubicTo(-1,-0.7f,-0.6f,-1,0,-1);made.close();break;
            case "heart":
                // Two circles that overlap at the top and a point at the
                // bottom, filling the square from edge to edge. Drawn as one
                // curve it left a quarter of the picture unused, and what was
                // left of a face in it was small.
                made.arcTo(new RectF(-1f, -1f, 0.1f, 0.1f), -35.1f, -174.9f, true);
                made.cubicTo(-0.70f, 0.25f, -0.28f, 0.70f, 0f, 1f);
                made.cubicTo(0.28f, 0.70f, 0.70f, 0.25f, 0.926f, -0.175f);
                made.arcTo(new RectF(-0.1f, -1f, 1f, 0.1f), 30f, -174.9f, false);
                made.close();
                break;
            case "circle":
                made.addCircle(0, 0, 1, Path.Direction.CW);
                break;
            default:
                for (int i = 0; i < POINTS; i++) {
                    double around = 2 * Math.PI * i / POINTS - Math.PI / 2;
                    float reach = reach(ALL[shape], around + Math.PI / 2);
                    float px = (float) (reach * Math.cos(around)), py = (float) (reach * Math.sin(around));
                    if (i == 0) made.moveTo(px, py);
                    else made.lineTo(px, py);
                }
                made.close();
        }
        UNIT[shape] = made;
        return made;
    }

    /** How far from the centre a shape reaches in a direction, out of 1. Up is zero. */
    static float reach(String shape, double around) {
        switch (shape) {
            case "cookie4": return scallop(4, around, 0.86f);
            case "cookie6": return scallop(6, around, 0.88f);
            case "cookie7": return scallop(7, around, 0.89f);
            case "cookie": return scallop(9, around, 0.91f);
            case "cookie12": return scallop(12, around, 0.93f);
            case "clover8": return 0.80f + 0.20f * (float) Math.pow(Math.abs(Math.cos(4 * around)), 0.6);
            case "very_sunny": return 0.80f + 0.20f * spike(8, around);
            case "soft_burst": return 0.90f + 0.10f * (float) Math.cos(10 * around);
            case "boom": return 0.72f + 0.28f * spike(15, around);
            case "soft_boom": return 0.84f + 0.16f * (float) Math.cos(16 * around);
            case "puffy": return 0.88f + 0.12f * (float) Math.abs(Math.cos(5 * around));
            case "puffy_diamond": return sided(4, around) * (0.93f + 0.07f * (float) Math.abs(Math.cos(6 * around)));
            case "clover": return 0.84f + 0.16f * (float) Math.cos(4 * around + Math.PI);
            case "burst": return 0.90f + 0.10f * (float) Math.cos(12 * around);
            case "sun": return 0.87f + 0.13f * (float) Math.cos(8 * around);
            case "diamond": return sided(4, around);
            case "gem": return sided(7, around);
            case "soft_triangle": return sided(3, around);
            case "flower": return 0.85f + 0.15f * (float)Math.cos(6*around);
            case "squircle": {
                double a=around-Math.PI/2;
                return (float)(1/Math.pow(Math.pow(Math.abs(Math.cos(a)),4)+Math.pow(Math.abs(Math.sin(a)),4),0.25));
            }
            case "pentagon": return sided(5, around);
            case "hexagon": return sided(6, around);
            default: return 1f;
        }
    }

    /** Round lobes with soft dips between them: Material's cookies (a cosine squared, so the lobes stay full). */
    private static float scallop(int lobes, double around, float inner) {
        float c = (float) Math.cos(lobes * around / 2.0);
        return inner + (1f - inner) * c * c;
    }

    /** Points with rounded tips: a cosine sharpened into a star. */
    private static float spike(int points, double around) {
        double c = Math.abs(Math.cos(points * around / 2.0));
        return (float) Math.pow(c, 3);
    }

    /** A polygon with its corners taken off, one corner pointing up. */
    private static float sided(int sides, double around) {
        double slice = 2 * Math.PI / sides;
        double within = around % slice;
        if (within < 0) within += slice;
        double flat = Math.cos(Math.PI / sides) / Math.cos(within - slice / 2);
        // the corner is where `flat` reaches 1; a soft minimum against a
        // little less than that rounds it without moving the sides
        double cap = 0.93, sharp = 36.0;
        double soft = -Math.log(Math.exp(-sharp * flat) + Math.exp(-sharp * cap)) / sharp;
        return (float) (soft / cap);
    }

    /** A shape filled with one colour, for the list it is chosen from. */
    static Bitmap picture(String shape, int size, int colour) {
        int which = 0;
        for (int i = 0; i < ALL.length; i++) if (ALL[i].equals(shape)) which = i;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(colour);
        Path placed = new Path();
        Matrix place = new Matrix();
        place.setScale(size / 2f - 1, size / 2f - 1);
        place.postTranslate(size / 2f, size / 2f);
        placed.addPath(unit(which), place);
        canvas.drawPath(placed, paint);
        return bitmap;
    }

    static String nameOf(String shape) {
        switch(shape) {
            case "rounded": return Text.t("Скруглённый квадрат");
            case "square": return Text.t("Квадрат");
            case "slanted": return Text.t("Наклонный");
            case "semicircle": return Text.t("Полукруг");
            case "oval": return Text.t("Овал");
            case "very_sunny": return Text.t("Яркое солнце");
            case "cookie4": return Text.t("Печенье 4");
            case "cookie6": return Text.t("Печенье 6");
            case "cookie7": return Text.t("Печенье 7");
            case "cookie12": return Text.t("Печенье 12");
            case "clover8": return Text.t("Клевер 8");
            case "soft_burst": return Text.t("Мягкая вспышка");
            case "boom": return Text.t("Взрыв");
            case "soft_boom": return Text.t("Мягкий взрыв");
            case "puffy": return Text.t("Пышный");
            case "puffy_diamond": return Text.t("Пышный ромб");
            case "cookie": return Text.t("Печенье 9");
            case "clover": return Text.t("Клевер 4");
            case "burst": return Text.t("Вспышка");
            case "sun": return Text.t("Солнце");
            case "pentagon": return Text.t("Пятиугольник");
            case "hexagon": return Text.t("Шестиугольник");
            case "heart": return Text.t("Сердце");
            case "pill": return Text.t("Капсула");
            case "diamond": return Text.t("Ромб");
            case "arch": return Text.t("Арка");
            case "gem": return Text.t("Самоцвет");
            case "flower": return Text.t("Цветок");
            case "tulip": return Text.t("Тюльпан");
            case "squircle": return Text.t("Суперэллипс");
            case "soft_triangle": return Text.t("Треугольник");
            case "fan": return Text.t("Веер");
            case "pebble": return Text.t("Камешек");
            default: return Text.t("Круг");
        }
    }
}
