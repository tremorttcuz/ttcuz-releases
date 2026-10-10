package cat.narezany.margyt;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/** Geometry and spring timing ported from the user-supplied expressiveTok.java (leonidtsk). */
public final class LoadingIndicator {
    private LoadingIndicator() {}
    private static final java.util.Map<View,MD3Spinner> renderers = new java.util.WeakHashMap<>();

    private static final java.util.Map<Object,Boolean> lottieTypes = new java.util.WeakHashMap<>();

    /** TikTok still owns the node, layout, visibility and loading state. */
    public static void draw(View view, Canvas canvas) {
        if(view==null || canvas==null)return;
        draw(view,canvas,Math.min(view.getWidth(),view.getHeight()),true);
    }

    /** Native ball views also show a static preview while a list is pulled. */
    public static void draw(View view, Canvas canvas, int size, boolean running) {
        if(view==null || canvas==null)return;
        MD3Spinner renderer=renderers.get(view);
        if(renderer==null){renderer=new MD3Spinner();renderers.put(view,renderer);}
        renderer.draw(view,canvas,size,running);
    }

    /** A build marker works for assets, raw resources, streams and cached compositions. */
    public static boolean drawLottie(View view, Canvas canvas, Object composition, boolean running) {
        if(composition==null)return false;
        Boolean loading=lottieTypes.get(composition);
        if(loading==null){
            loading=composition.toString().contains("ttcuz_MD3_loader");
            lottieTypes.put(composition,loading);
        }
        if(!loading)return false;
        int size=Math.round(48f*view.getResources().getDisplayMetrics().density);
        draw(view,canvas,size,running);
        return true;
    }

    private static final class MD3Spinner {
        private static final long MORPH_INTERVAL_MS=650;
        private static final long ROTATION_PERIOD_MS=4666;
        private static final float ACTIVE_RATIO=38f/48f;
        private static final float DAMPING=0.6f, STIFFNESS=200f;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path=new Path();

        void draw(View view,Canvas canvas,int size,boolean running){
            int w=view.getWidth(),h=view.getHeight();
            if(w<=0 || h<=0 || size<=0)return;
            boolean animate=running && Motion.enabled(view.getContext());
            long now=animate?SystemClock.uptimeMillis():0L;
            float[][] table=M3Shapes.get();
            int step=(int)((now/MORPH_INTERVAL_MS)%table.length);
            float progress=spring((now%MORPH_INTERVAL_MS)/1000f);
            float cx=w/2f,cy=h/2f,unit=Math.min(Math.min(w,h),Math.max(0,size))*ACTIVE_RATIO;
            float[] a=table[step],b=table[(step+1)%table.length];
            path.reset();
            for(int j=0;j<M3Shapes.N;j++){
                float r=(a[j]+(b[j]-a[j])*progress)*unit;
                double angle=-Math.PI+2.0*Math.PI*j/M3Shapes.N;
                float x=cx+(float)Math.cos(angle)*r,y=cy+(float)Math.sin(angle)*r;
                if(j==0)path.moveTo(x,y);else path.lineTo(x,y);
            }
            path.close();paint.setColor(Accent.colour());
            int save=canvas.save();
            canvas.rotate((now%ROTATION_PERIOD_MS)/(float)ROTATION_PERIOD_MS*360f,cx,cy);
            canvas.drawPath(path,paint);canvas.restoreToCount(save);
            if(animate && view.isShown() && view.getWindowVisibility()==View.VISIBLE)view.postInvalidateOnAnimation();
        }

        private static float spring(float t){
            double w0=Math.sqrt(STIFFNESS),zeta=DAMPING,wd=w0*Math.sqrt(1.0-zeta*zeta);
            double decay=Math.exp(-zeta*w0*t);
            return (float)(1.0-decay*(Math.cos(wd*t)+(zeta*w0/wd)*Math.sin(wd*t)));
        }
    }

    private static final class M3Shapes {
        static final int N = 180;
        private static float[][] table;

        static synchronized float[][] get() {
            if (table == null) table = build();
            return table;
        }

        private static final class Outline {
            final ArrayList<float[]> pts = new ArrayList<>();
            float cx, cy;
        }

        private static float[][] build() {
            List<Outline> shapes = new ArrayList<>();
            // SoftBurst
            shapes.add(custom(new float[][]{{0.193f, 0.277f, 0.053f}, {0.176f, 0.055f, 0.053f}}, 10, false));
            // Cookie9Sided
            shapes.add(star(9, 0.8f, 0.5f));
            // Pentagon (по памяти)
            shapes.add(custom(new float[][]{{0.5f, -0.009f, 0.172f}}, 5, false));
            // Pill
            shapes.add(custom(new float[][]{{0.961f, 0.039f, 0.426f}, {1.001f, 0.428f, 0f}, {1f, 0.609f, 1f}}, 2, true));
            // Sunny (по памяти)
            shapes.add(star(8, 0.8f, 0.15f));
            // Cookie4Sided
            shapes.add(custom(new float[][]{{1.237f, 1.236f, 0.258f}, {0.5f, 0.918f, 0.233f}}, 4, false));
            // Oval (по памяти): круг, сжатый по Y до 0.64 и повёрнутый на -45°
            shapes.add(oval());

            float[][] out = new float[shapes.size()][];
            for (int i = 0; i < out.length; i++) out[i] = sample(shapes.get(i));
            return out;
        }

        private static Outline oval() {
            Outline o = new Outline();
            double rot = -Math.PI / 4;
            for (int i = 0; i < 128; i++) {
                double t = 2 * Math.PI * i / 128;
                double x = Math.cos(t);
                double y = 0.64 * Math.sin(t);
                o.pts.add(new float[]{
                        (float) (x * Math.cos(rot) - y * Math.sin(rot)),
                        (float) (x * Math.sin(rot) + y * Math.cos(rot))});
            }
            return o;
        }

        /** RoundedPolygon.star: вершины чередуются внешний/внутренний радиус, старт сверху. */
        private static Outline star(int n, float innerRadius, float rounding) {
            ArrayList<float[]> verts = new ArrayList<>();
            for (int i = 0; i < n * 2; i++) {
                double ang = -Math.PI / 2 + i * Math.PI / n;
                float rad = (i % 2 == 0) ? 1f : innerRadius;
                verts.add(new float[]{(float) Math.cos(ang) * rad, (float) Math.sin(ang) * rad, rounding});
            }
            return rounded(verts, 0f, 0f);
        }

        /** customPolygon из MaterialShapes: повтор набора точек вокруг (0.5, 0.5), опционально с зеркалом. */
        private static Outline custom(float[][] p, int reps, boolean mirroring) {
            final float cx = 0.5f, cy = 0.5f;
            ArrayList<float[]> verts = new ArrayList<>();
            int np = p.length;
            if (mirroring) {
                float[] ang = new float[np];
                float[] dist = new float[np];
                for (int i = 0; i < np; i++) {
                    float dx = p[i][0] - cx, dy = p[i][1] - cy;
                    ang[i] = (float) Math.atan2(dy, dx);
                    dist[i] = (float) Math.hypot(dx, dy);
                }
                int actual = reps * 2;
                float section = (float) (2 * Math.PI / actual);
                for (int it = 0; it < actual; it++) {
                    for (int index = 0; index < np; index++) {
                        int i = (it % 2 == 0) ? index : np - index - 1;
                        if (i > 0 || it % 2 == 0) {
                            float a = section * it + ((it % 2 == 0) ? ang[i]
                                    : section - ang[i] + 2 * ang[0]);
                            verts.add(new float[]{
                                    cx + (float) Math.cos(a) * dist[i],
                                    cy + (float) Math.sin(a) * dist[i], p[i][2]});
                        }
                    }
                }
            } else {
                for (int i = 0; i < np * reps; i++) {
                    float[] q = p[i % np];
                    double rot = Math.toRadians((i / np) * 360.0 / reps);
                    float dx = q[0] - cx, dy = q[1] - cy;
                    verts.add(new float[]{
                            cx + (float) (dx * Math.cos(rot) - dy * Math.sin(rot)),
                            cy + (float) (dx * Math.sin(rot) + dy * Math.cos(rot)), q[2]});
                }
            }
            return rounded(verts, cx, cy);
        }

        /**
         * Скругление углов многоугольника дугами окружности (CornerRounding без smoothing).
         * Если радиусы соседних углов не помещаются в сторону — их срезы пропорционально
         * уменьшаются, как в androidx.graphics.shapes.
         */
        private static Outline rounded(List<float[]> v, float cx, float cy) {
            int n = v.size();
            float[] ux = new float[n], uy = new float[n], wx = new float[n], wy = new float[n];
            float[] half = new float[n];
            float[] cut = new float[n];
            boolean[] ok = new boolean[n];

            for (int i = 0; i < n; i++) {
                float[] p = v.get(i);
                float[] a = v.get((i + n - 1) % n);
                float[] b = v.get((i + 1) % n);
                float ax = a[0] - p[0], ay = a[1] - p[1];
                float bx = b[0] - p[0], by = b[1] - p[1];
                float la = (float) Math.hypot(ax, ay), lb = (float) Math.hypot(bx, by);
                if (la < 1e-6f || lb < 1e-6f) continue;
                ux[i] = ax / la; uy[i] = ay / la;
                wx[i] = bx / lb; wy[i] = by / lb;
                double cos = Math.max(-1.0, Math.min(1.0, ux[i] * wx[i] + uy[i] * wy[i]));
                half[i] = (float) (Math.acos(cos) / 2);
                float tn = (float) Math.tan(half[i]);
                if (p[2] > 0f && tn > 1e-4f && half[i] > 1e-4f) {
                    cut[i] = p[2] / tn;
                    ok[i] = true;
                }
            }

            float[] eff = cut.clone();
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                float[] pi = v.get(i), pj = v.get(j);
                float len = (float) Math.hypot(pj[0] - pi[0], pj[1] - pi[1]);
                float sum = cut[i] + cut[j];
                if (sum > len && sum > 0f) {
                    float ratio = len / sum;
                    eff[i] = Math.min(eff[i], cut[i] * ratio);
                    eff[j] = Math.min(eff[j], cut[j] * ratio);
                }
            }

            Outline o = new Outline();
            o.cx = cx;
            o.cy = cy;
            for (int i = 0; i < n; i++) {
                float[] p = v.get(i);
                if (!ok[i] || eff[i] < 1e-5f) {
                    o.pts.add(new float[]{p[0], p[1]});
                    continue;
                }
                float d = eff[i];
                float rad = d * (float) Math.tan(half[i]);
                float t1x = p[0] + ux[i] * d, t1y = p[1] + uy[i] * d;
                float t2x = p[0] + wx[i] * d, t2y = p[1] + wy[i] * d;
                float bx = ux[i] + wx[i], by = uy[i] + wy[i];
                float bl = (float) Math.hypot(bx, by);
                if (bl < 1e-6f) {
                    o.pts.add(new float[]{p[0], p[1]});
                    continue;
                }
                float dc = rad / (float) Math.sin(half[i]);
                float ccx = p[0] + bx / bl * dc, ccy = p[1] + by / bl * dc;
                double a1 = Math.atan2(t1y - ccy, t1x - ccx);
                double a2 = Math.atan2(t2y - ccy, t2x - ccx);
                double delta = a2 - a1;
                while (delta > Math.PI) delta -= 2 * Math.PI;
                while (delta <= -Math.PI) delta += 2 * Math.PI;
                int steps = 10;
                for (int s = 0; s <= steps; s++) {
                    double a = a1 + delta * s / steps;
                    o.pts.add(new float[]{ccx + rad * (float) Math.cos(a), ccy + rad * (float) Math.sin(a)});
                }
            }
            return o;
        }

        /** Таблица радиусов r(угол) от центра формы по лучам; масштаб — большая сторона габарита = 1. */
        private static float[] sample(Outline o) {
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (float[] p : o.pts) {
                minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]);
                minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]);
            }
            float side = Math.max(maxX - minX, maxY - minY);
            float[] r = new float[N];
            int m = o.pts.size();
            for (int j = 0; j < N; j++) {
                double ang = -Math.PI + 2.0 * Math.PI * j / N;
                float dx = (float) Math.cos(ang), dy = (float) Math.sin(ang);
                float best = 0f;
                for (int k = 0; k < m; k++) {
                    float[] p = o.pts.get(k);
                    float[] q = o.pts.get((k + 1) % m);
                    float ex = q[0] - p[0], ey = q[1] - p[1];
                    float den = dx * ey - dy * ex;
                    if (Math.abs(den) < 1e-9f) continue;
                    float px = p[0] - o.cx, py = p[1] - o.cy;
                    float t = (px * ey - py * ex) / den;
                    float s = (px * dy - py * dx) / den;
                    if (t > 0f && s >= -1e-4f && s <= 1f + 1e-4f && t > best) best = t;
                }
                r[j] = best / side;
            }
            return r;
        }
    }

}
