package cat.ttcuz.framework;
import java.util.ArrayList;
/**
 * The seven MaterialShapes of the M3 loading indicator and its timing, ported one to one from m3-motion-lab.html.
 *
 * Every shape is a radial profile of {@link #N} rays: the distance from its centre of mass to its outline,
 * measured at angle j*2π/N from the +x axis, in units where the longer side of the shape's box is 1.
 * Morphing is then a plain mix of two profiles, which is what the lab does.
 * Order: SoftBurst, Cookie9Sided, Pentagon, Pill, Sunny, Cookie4Sided, Oval.
 */
final class LabShapes {
    private LabShapes() {}
    static final int N = 240;
    static final int SUNNY = 4;
    private static final double TAU = Math.PI * 2;
    private static float[][] table;

    static synchronized float[][] get() {
        if (table == null) table = build();
        return table;
    }

    // ------------------------------------------------------------- timing

    /** Morph step 650 ms, full turn 4666 ms, +90 degrees per step, spring damping 0.6 and stiffness 200. */
    static final double STEP_MS = 650, TURN_MS = 4666;
    private static final double ZW = 0.6 * Math.sqrt(200), WD = Math.sqrt(200) * Math.sqrt(1 - 0.36);
    private static final double END = spring(0.65);

    static double spring(double t) {
        return 1 - Math.exp(-ZW * t) * (Math.cos(WD * t) + ZW / WD * Math.sin(WD * t));
    }

    /**
     * Fills {@code path} with the loading shape at time {@code ms}, centred on 0,0, scaled by {@code size}.
     * Returns the rotation in radians that the caller applies to the canvas.
     */
    static double blob(android.graphics.Path path, double ms, float size) {
        float[][] shapes = get();
        int n = shapes.length;
        double seg = ms / STEP_MS;
        int k = (int) Math.floor(seg);
        double e = Math.min(spring((seg - k) * 0.65) / END, 1.3);
        float[] a = shapes[Math.floorMod(k, n)], b = shapes[Math.floorMod(k + 1, n)];
        double rotation = ms / TURN_MS * TAU + (k + e) * Math.PI / 2;
        path.reset();
        for (int i = 0; i <= N; i++) {
            int j = i % N;
            double r = Math.max(0, a[j] + (b[j] - a[j]) * e) * size, angle = j * TAU / N;
            float x = (float) (Math.cos(angle) * r), y = (float) (Math.sin(angle) * r);
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        path.close();
        return rotation;
    }

    // ------------------------------------------------------------- shapes

    private static double[] rot(double x, double y, double a) {
        double s = Math.sin(a), k = Math.cos(a);
        return new double[]{x * k - y * s, x * s + y * k};
    }

    /** star(n, inner radius, corner rounding, turn). */
    private static double[][] star(int n, double inner, double round, double turn) {
        double[][] v = new double[n * 2][];
        for (int i = 0; i < n * 2; i++) {
            double a = Math.PI / n * i, r = i % 2 == 1 ? inner : 1;
            double[] p = rot(Math.cos(a) * r, Math.sin(a) * r, turn);
            v[i] = new double[]{p[0], p[1], round};
        }
        return v;
    }

    /** One group of vertices repeated {@code reps} times around the centre. */
    private static double[][] custom(double[][] pv, int reps) {
        double[][] v = new double[pv.length * reps][];
        for (int i = 0; i < v.length; i++) {
            double[] c = pv[i % pv.length];
            double[] p = rot(c[0], c[1], Math.floor((double) i / pv.length) * TAU / reps);
            v[i] = new double[]{p[0], p[1], c[2]};
        }
        return v;
    }

    private static float[][] build() {
        double up = -Math.PI / 2;
        double[][][] list = new double[7][][];
        list[0] = custom(new double[][]{{-0.307, -0.223, 0.053}, {-0.324, -0.445, 0.053}}, 10); // SoftBurst
        list[1] = star(9, 0.8, 0.5, up);                                                        // Cookie9Sided
        double[][] pentagon = new double[5][];                                                  // Pentagon
        for (int i = 0; i < 5; i++) pentagon[i] = new double[]{Math.cos(up + i * TAU / 5), Math.sin(up + i * TAU / 5), 0.172};
        list[2] = pentagon;
        list[3] = new double[][]{{-1, -0.56, 0.56}, {1, -0.56, 0.56}, {1, 0.56, 0.56}, {-1, 0.56, 0.56}}; // Pill
        list[4] = star(8, 0.8, 0.15, up);                                                       // Sunny
        list[5] = custom(new double[][]{{0.737, 0.736, 0.258}, {0, 0.418, 0.233}}, 4);          // Cookie4Sided
        double[][] oval = new double[90][];                                                     // Oval
        for (int i = 0; i < 90; i++) {
            double a = i * TAU / 90;
            double[] p = rot(Math.cos(a), 0.64 * Math.sin(a), -Math.PI / 4);
            oval[i] = new double[]{p[0], p[1], 0};
        }
        list[6] = oval;
        float[][] out = new float[list.length][];
        for (int i = 0; i < list.length; i++) out[i] = profile(outline(list[i]));
        return out;
    }

    /** RoundedPolygon: vertices [x, y, radius] become an outline with a circular arc at each rounded corner. */
    private static ArrayList<double[]> outline(double[][] v) {
        int n = v.length;
        double[][] u = new double[n][], w = new double[n][];
        double[] ang = new double[n], d = new double[n], len = new double[n];
        for (int i = 0; i < n; i++) {
            double[] c = v[i], p = v[(i + n - 1) % n], q = v[(i + 1) % n];
            double ax = p[0] - c[0], ay = p[1] - c[1], bx = q[0] - c[0], by = q[1] - c[1];
            double la = Math.hypot(ax, ay), lb = Math.hypot(bx, by);
            u[i] = new double[]{ax / la, ay / la};
            w[i] = new double[]{bx / lb, by / lb};
            ang[i] = Math.acos(Math.max(-1, Math.min(1, u[i][0] * w[i][0] + u[i][1] * w[i][1])));
            d[i] = c[2] > 0 ? c[2] / Math.tan(ang[i] / 2) : 0;
            len[i] = lb;
        }
        double[] sc = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = d[i] + d[(i + 1) % n];
            sc[i] = sum > len[i] && sum > 0 ? len[i] / sum : 1;
        }
        ArrayList<double[]> pts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double[] c = v[i];
            double s = Math.min(sc[i], sc[(i + n - 1) % n]), cut = d[i] * s;
            if (cut < 1e-4) { pts.add(new double[]{c[0], c[1]}); continue; }
            double r = cut * Math.tan(ang[i] / 2);
            double bx = u[i][0] + w[i][0], by = u[i][1] + w[i][1], bl = Math.hypot(bx, by), dist = r / Math.sin(ang[i] / 2);
            double cx = c[0] + bx / bl * dist, cy = c[1] + by / bl * dist;
            double t1x = c[0] + u[i][0] * cut, t1y = c[1] + u[i][1] * cut, t2x = c[0] + w[i][0] * cut, t2y = c[1] + w[i][1] * cut;
            double a1 = Math.atan2(t1y - cy, t1x - cx), a2 = Math.atan2(t2y - cy, t2x - cx);
            double da = ((a2 - a1 + 3 * Math.PI) % TAU) - Math.PI;
            for (int j = 0; j <= 12; j++) {
                double a = a1 + da * j / 12;
                pts.add(new double[]{cx + Math.cos(a) * r, cy + Math.sin(a) * r});
            }
        }
        return pts;
    }

    /** Size: the longer side of the box is 1. Centre and axis of rotation: the centre of mass, so the shape does not wander. */
    private static float[] profile(ArrayList<double[]> pts) {
        int m = pts.size();
        double x0 = 1e9, x1 = -1e9, y0 = 1e9, y1 = -1e9;
        for (double[] p : pts) { x0 = Math.min(x0, p[0]); x1 = Math.max(x1, p[0]); y0 = Math.min(y0, p[1]); y1 = Math.max(y1, p[1]); }
        double area = 0, gx = 0, gy = 0;
        for (int i = 0; i < m; i++) {
            double[] a = pts.get(i), b = pts.get((i + 1) % m);
            double w = a[0] * b[1] - b[0] * a[1];
            area += w; gx += (a[0] + b[0]) * w; gy += (a[1] + b[1]) * w;
        }
        double cx = gx / (3 * area), cy = gy / (3 * area), side = Math.max(x1 - x0, y1 - y0);
        double[][] q = new double[m][];
        for (int i = 0; i < m; i++) q[i] = new double[]{pts.get(i)[0] - cx, pts.get(i)[1] - cy};
        float[] out = new float[N];
        for (int i = 0; i < N; i++) {
            double a = i * TAU / N, dx = Math.cos(a), dy = Math.sin(a), best = 0;
            for (int j = 0; j < m; j++) {
                double[] p = q[j], r = q[(j + 1) % m];
                double ex = r[0] - p[0], ey = r[1] - p[1], den = dx * ey - dy * ex;
                if (Math.abs(den) < 1e-12) continue;
                double t = (p[0] * ey - p[1] * ex) / den, u = (p[0] * dy - p[1] * dx) / den;
                if (u >= -1e-9 && u <= 1 + 1e-9 && t > best) best = t;
            }
            out[i] = (float) (best / side);
        }
        return out;
    }
}
