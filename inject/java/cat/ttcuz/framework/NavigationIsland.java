package cat.ttcuz.framework;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.view.animation.PathInterpolator;
import android.widget.*;
import java.lang.ref.WeakReference;
import java.util.*;

/** Restyle the native five-tab row; TikTok keeps clicks, routing, badges and accessibility.
 *  The row's layout is never changed, so the seekbar and the touch area above the bar stay native. */
final class NavigationIsland {

    private static final Map<View, State> states = new WeakHashMap<>();
    private static WeakReference<View> active = new WeakReference<>(null);

    private NavigationIsland() {}

    static boolean isRail(View view) {
        return active.get() == view;
    }

    static void watch(Activity activity) {
        if (
            !activity.getClass().getName().equals("com.ss.android.ugc.aweme.main.MainActivity")
        ) return;
        View root = activity.getWindow().getDecorView();
        State state = states.get(root);
        if (state == null) {
            state = new State(root);
            states.put(root, state);
        }
        state.attach();
        IslandTouch.hook(activity);
    }

    static void stop(Activity activity) {
        State state = states.get(activity.getWindow().getDecorView());
        if (state != null) state.detach();
    }

    static void destroy(Activity activity) {
        State state = states.remove(activity.getWindow().getDecorView());
        if (state != null) {
            state.detach();
            if (active.get() == state.rail.get()) active.clear();
        }
    }

    private static State stateOf(Activity activity) {
        return states.get(activity.getWindow().getDecorView());
    }

    /** Hold-and-drag of the selection indicator (driven by IslandTouch; screen coordinates in, nothing native touched). */
    static boolean hit(Activity activity, float x, float y) {
        State state = stateOf(activity);
        return state != null && state.hit(x, y);
    }

    static void begin(Activity activity, float x) {
        State state = stateOf(activity);
        if (state != null) state.begin(x);
    }

    /** True when the nearest tab changed (the caller may tick). */
    static boolean move(Activity activity, float x) {
        State state = stateOf(activity);
        return state != null && state.move(x);
    }

    /** Screen centre of the tab to tap, or null when the native selection already matches. */
    static float[] end(Activity activity, boolean commit) {
        State state = stateOf(activity);
        return state == null ? null : state.end(commit);
    }

    static float stepOverride = 0f;

    static int role(String text) {
        if (text == null) return -1;
        String label = text.trim().toLowerCase(Locale.ROOT);
        if (label.equals("главная") || label.equals("home") || label.equals("головна")) return 0;
        if (
            label.equals("сообщество") ||
            label.equals("community") ||
            label.equals("друзья") ||
            label.equals("friends") ||
            label.equals("друзі") ||
            label.equals("explore")
        ) return 1;
        if (label.equals("входящие") || label.equals("inbox") || label.equals("вхідні")) return 3;
        if (label.equals("профиль") || label.equals("profile") || label.equals("профіль")) return 4;
        return -1;
    }

    private static int role(View view, int depth) {
        if (depth > 5) return -1;
        int found = role(
            view instanceof TextView
                ? String.valueOf(((TextView) view).getText())
                : String.valueOf(view.getContentDescription())
        );
        if (found >= 0) return found;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                found = role(group.getChildAt(i), depth + 1);
                if (found >= 0) return found;
            }
        }
        return -1;
    }

    private static boolean selected(View view, int depth) {
        if (view.isSelected() || view.isActivated()) return true;
        if (depth < 5 && view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) if (
                selected(group.getChildAt(i), depth + 1)
            ) return true;
        }
        return false;
    }

    private static ArrayList<View> cells(ViewGroup rail) {
        ArrayList<View> cells = new ArrayList<>();
        for (int i = 0; i < rail.getChildCount(); i++) {
            View child = rail.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE && child.getWidth() > 0) cells.add(child);
        }
        if (cells.size() != 5) return null;
        int mask = 0;
        float last = Float.NEGATIVE_INFINITY;
        for (View cell : cells) {
            if (
                cell.getWidth() > rail.getWidth() * .29f ||
                cell.getHeight() < 36 * rail.getResources().getDisplayMetrics().density ||
                cell.getX() < last
            ) return null;
            last = cell.getX();
            int role = role(cell, 0);
            if (role >= 0) mask |= 1 << role;
        }
        return (mask & 25) == 25 &&
            role(cells.get(0), 0) == 0 &&
            role(cells.get(3), 0) == 3 &&
            role(cells.get(4), 0) == 4
            ? cells
            : null;
    }

    private static ViewGroup find(
        View view,
        int[] top,
        int height,
        int width,
        int depth,
        int[] budget
    ) {
        if (
            depth > 22 || budget[0]-- <= 0 || !view.isShown() || !(view instanceof ViewGroup)
        ) return null;
        ViewGroup group = (ViewGroup) view;
        int[] at = new int[2];
        view.getLocationOnScreen(at);
        if (at[1] + view.getHeight() < top[1] + height * .70f) return null;
        float unit = view.getResources().getDisplayMetrics().density;
        if (
            view.getWidth() > width * .75f &&
            view.getHeight() >= 48 * unit &&
            view.getHeight() <= 120 * unit &&
            at[1] + view.getHeight() / 2f > top[1] + height * .80f &&
            cells(group) != null
        ) return group;
        for (int i = 0; i < group.getChildCount(); i++) {
            ViewGroup found = find(group.getChildAt(i), top, height, width, depth + 1, budget);
            if (found != null) return found;
        }
        return null;
    }

    private static final class State implements ViewTreeObserver.OnPreDrawListener {

        final WeakReference<View> root;
        WeakReference<ViewGroup> rail = new WeakReference<>(null);
        IslandDrawable background;
        ArrayList<View> tabs;
        boolean attached;
        WeakReference<ViewTreeObserver> observer = new WeakReference<>(null);
        long nextSearch, nextRebuild;
        int structure;

        State(View root) {
            this.root = new WeakReference<>(root);
        }

        void attach() {
            View view = root.get();
            if (view == null) return;
            ViewTreeObserver next = view.getViewTreeObserver(),
                old = observer.get();
            if (attached && next == old) return;
            if (old != null && old.isAlive()) old.removeOnPreDrawListener(this);
            attached = next.isAlive();
            if (attached) {
                next.addOnPreDrawListener(this);
                observer = new WeakReference<>(next);
            }
        }

        void detach() {
            ViewTreeObserver old = observer.get();
            if (old != null && old.isAlive()) old.removeOnPreDrawListener(this);
            attached = false;
            if (background != null) background.settle();
        }

        private float[] origin(ViewGroup row) {
            int[] at = new int[2];
            row.getLocationOnScreen(at);
            return new float[] { at[0], at[1] };
        }

        boolean hit(float x, float y) {
            ViewGroup row = rail.get();
            if (row == null || background == null || tabs == null || !row.isShown()) return false;
            float[] at = origin(row);
            return background.hit(x - at[0], y - at[1]);
        }

        void begin(float x) {
            ViewGroup row = rail.get();
            if (row == null || background == null) return;
            background.beginDrag(x - origin(row)[0]);
        }

        boolean move(float x) {
            ViewGroup row = rail.get();
            return row != null && background != null && background.dragTo(x - origin(row)[0]);
        }

        float[] end(boolean commit) {
            ViewGroup row = rail.get();
            if (row == null || background == null || tabs == null) return null;
            int index = background.drop(commit);
            if (!commit || index < 0 || index >= tabs.size()) return null;
            View cell = tabs.get(index);
            if (selected(cell, 0)) return null;
            float[] at = origin(row);
            return new float[] {
                at[0] + cell.getX() + cell.getWidth() / 2f,
                at[1] + cell.getY() + cell.getHeight() / 2f,
            };
        }

        /** Cheap per-frame check of the cached cells: no allocation, no text matching. */
        private boolean intact(ViewGroup row) {
            if (tabs == null || tabs.size() != 5) return false;
            for (int i = 0; i < 5; i++) {
                View cell = tabs.get(i);
                if (
                    cell.getParent() != row ||
                    cell.getVisibility() != View.VISIBLE ||
                    cell.getWidth() <= 0
                ) return false;
            }
            return true;
        }

        public boolean onPreDraw() {
            View view = root.get();
            if (view == null || !view.hasWindowFocus()) return true;
            try {
                ViewGroup row = rail.get();
                if (row == null || !row.isAttachedToWindow()) {
                    long now = android.os.SystemClock.uptimeMillis();
                    if (now < nextSearch) return true;
                    nextSearch = now + 1000;
                    int[] at = new int[2];
                    view.getLocationOnScreen(at);
                    row = find(view, at, view.getHeight(), view.getWidth(), 0, new int[] { 3000 });
                    if (row == null) return true;
                    ArrayList<View> found = cells(row);
                    if (found == null) return true;
                    tabs = found;
                    nextRebuild = now + 1500;
                    rail = new WeakReference<>(row);
                    active = new WeakReference<>(row);
                    // The row's own layout (height, margins, padding, elevation) stays exactly as TikTok made it:
                    // the island is painted inside the native bounds, so nothing climbs over the seekbar and the
                    // touch area above the bar is never taken.
                    background = new IslandDrawable(row);
                    row.setBackground(background);
                    background.collect(tabs);
                    structure = structure(row, 0);
                    Diary.note("navigation: MD3 island attached");
                }
                if (!row.isShown()) return true;
                if (!intact(row)) {
                    ArrayList<View> found = cells(row);
                    if (found == null) {
                        tabs = null;
                        return true;
                    }
                    tabs = found;
                    background.collect(tabs);
                    structure = structure(row, 0);
                }
                if (android.os.SystemClock.uptimeMillis() >= nextRebuild) {
                    nextRebuild = android.os.SystemClock.uptimeMillis() + 1500;
                    int next = structure(row, 0);
                    if (next != structure) {
                        structure = next;
                        background.collect(tabs);
                    }
                }
                if (row.getBackground() != background) row.setBackground(background);
                background.update(tabs);
            } catch (RuntimeException error) {
                Diary.note("navigation: " + error);
            }
            return true;
        }

        private static int structure(View view, int depth) {
            int hash = System.identityHashCode(view);
            if (depth < 6 && view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                hash = 31 * hash + group.getChildCount();
                for (int i = 0; i < group.getChildCount(); i++) hash =
                    31 * hash + structure(group.getChildAt(i), depth + 1);
            }
            return hash;
        }
    }

    /** A label or icon inside a native tab, found once per rebuild instead of on every frame. */
    private static final class Part {

        final View view;
        final int tab;

        Part(View view, int tab) {
            this.view = view;
            this.tab = tab;
        }
    }

    /** Under-damped spring: a little overshoot, then rest (iOS pop with Material 3 Expressive bounce). */
    private static final class Spring {

        float v, t, vel;
        final float k, c, eps;

        Spring(float k, float c, float eps) {
            this.k = k;
            this.c = c;
            this.eps = eps;
        }

        void set(float x) {
            v = t = x;
            vel = 0f;
        }

        boolean step(float dt) {
            int n = (int) Math.ceil(dt / .008f);
            if (n < 1) n = 1;
            float h = dt / n;
            for (int i = 0; i < n; i++) {
                vel += (k * (t - v) - c * vel) * h;
                v += vel * h;
            }
            if (Math.abs(t - v) > eps || Math.abs(vel) > eps * 10f) return true;
            v = t;
            vel = 0f;
            return false;
        }
    }

    private static final class IslandDrawable extends Drawable {

        final WeakReference<ViewGroup> rail;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF body = new RectF(),
            shape = new RectF(),
            pill = new RectF(),
            target = new RectF(),
            next = new RectF(),
            from = new RectF(),
            to = new RectF();
        final Path outline = new Path();
        final ArrayList<Part> parts = new ArrayList<>();
        ValueAnimator animation;
        int selected = -1,
            previous = -1,
            accent,
            muted,
            bodyColour,
            pillColour;
        float progress = 1f,
            lift,
            pillTop,
            pillWidth = 56f,
            pillHeight = 32f,
            offset;
        final float[] centres = new float[5];
        final Spring px = new Spring(520f, 30f, .05f),
            grow = new Spring(520f, 26f, .002f);
        boolean manual, dragging, looping;
        int near = -1;
        long last;
        final Runnable loop = new Runnable() {
            public void run() {
                frame();
            }
        };
        int alpha = 255;
        Skin skin;
        ColorStateList iconTint;
        float unit, radius;

        IslandDrawable(ViewGroup rail) {
            this.rail = new WeakReference<>(rail);
            unit = rail.getResources().getDisplayMetrics().density;
        }

        void cancel() {
            ValueAnimator old = animation;
            animation = null;
            if (old != null) old.cancel();
        }

        void settle() {
            cancel();
            ViewGroup row = rail.get();
            if (row != null) row.removeCallbacks(loop);
            looping = false;
            manual = false;
            dragging = false;
            px.set((target.left + target.right) / 2f);
            grow.set(0f);
            progress = 1f;
            previous = -1;
            lift = 0f;
            pill.set(target);
            if (skin != null) tint();
            invalidateSelf();
        }

        boolean hit(float x, float y) {
            float slop = 6 * unit;
            return (
                selected >= 0 &&
                x >= pill.left - slop &&
                x <= pill.right + slop &&
                y >= pill.top - slop &&
                y <= pill.bottom + slop
            );
        }

        private int nearest(float x) {
            int best = 0;
            float gap = Float.MAX_VALUE;
            for (int i = 0; i < 5; i++) {
                if (i == 2) continue;
                float d = Math.abs(centres[i] - x);
                if (d < gap) {
                    gap = d;
                    best = i;
                }
            }
            return best;
        }

        void beginDrag(float x) {
            cancel();
            manual = true;
            dragging = true;
            progress = 1f;
            previous = -1;
            lift = 0f;
            px.set((pill.left + pill.right) / 2f);
            grow.t = 1f;
            near = selected;
            offset = x - px.v;
            kick();
        }

        boolean dragTo(float x) {
            if (!dragging) return false;
            px.t = Math.max(centres[0], Math.min(centres[4], x - offset));
            int n = nearest(px.t);
            boolean changed = n != near;
            near = n;
            kick();
            return changed;
        }

        /** Releases the indicator: it springs onto the nearest tab (or back when not committed). Returns that tab. */
        int drop(boolean commit) {
            if (!dragging) return -1;
            dragging = false;
            int n = commit && near >= 0 ? near : selected;
            selected = n;
            px.t = centres[n];
            grow.t = 0f;
            targetFor(n, target);
            kick();
            return n;
        }

        private void targetFor(int tab, RectF out) {
            out.set(
                centres[tab] - pillWidth / 2f,
                pillTop,
                centres[tab] + pillWidth / 2f,
                pillTop + pillHeight
            );
        }

        private void kick() {
            ViewGroup row = rail.get();
            if (row == null || looping) return;
            looping = true;
            last = System.nanoTime();
            row.postOnAnimation(loop);
        }

        private void frame() {
            ViewGroup row = rail.get();
            if (row == null || !row.isAttachedToWindow() || !row.hasWindowFocus()) {
                settle();
                return;
            }
            long now = System.nanoTime();
            float dt = stepOverride > 0f ? stepOverride : Math.min(.032f, (now - last) / 1e9f);
            last = now;
            boolean busy = px.step(dt);
            busy |= grow.step(dt);
            // Growth is bounded by the island: it widens freely but rises only into the room above the indicator,
            // and it stays 6dp clear of both side edges, so it never touches or leaves the island body.
            float g = Math.max(0f, grow.v),
                e = Math.min(g, 1.1f),
                w = Math.min(
                    Math.max(0f, body.width() - 12 * unit),
                    pillWidth + 18 * unit * e + Math.min(26 * unit, Math.abs(px.vel) * .035f)
                );
            float room = Math.max(
                    0f,
                    2f *
                        Math.min(
                            pillTop - body.top - 3 * unit,
                            body.bottom - pillTop - pillHeight - 3 * unit
                        )
                ),
                extra = Math.min(7 * unit * e, room),
                h = pillHeight + extra,
                top = pillTop - extra / 2f;
            float cx = px.v;
            if (body.right > body.left) {
                float edge = 6 * unit;
                cx = Math.max(body.left + edge + w / 2f, Math.min(body.right - edge - w / 2f, cx));
            }
            pill.set(cx - w / 2f, top, cx + w / 2f, top + h);
            if (skin != null) tint();
            invalidateSelf();
            if (busy) row.postOnAnimation(loop);
            else {
                looping = false;
                if (!dragging) {
                    manual = false;
                    pill.set(target);
                    previous = -1;
                    progress = 1f;
                    lift = 0f;
                }
                if (skin != null) tint();
                invalidateSelf();
            }
        }

        /** Soft landing: fast start, long gentle stop. */
        static float ease(float t) {
            float u = 1f - t;
            return 1f - u * u * u * u;
        }

        /** A short slide keeps the indicator level even during rapid tab changes. */
        void step() {
            float t = progress;
            if (t >= 1f) {
                pill.set(to);
                lift = 0f;
                previous = -1;
            } else {
                float move = ease(t);
                pill.set(
                    from.left + (to.left - from.left) * move,
                    from.top + (to.top - from.top) * move,
                    from.right + (to.right - from.right) * move,
                    from.bottom + (to.bottom - from.bottom) * move
                );
                lift = 0f;
            }
            if (skin != null) tint();
            invalidateSelf();
        }

        void collect(ArrayList<View> tabs) {
            parts.clear();
            for (int i = 0; i < tabs.size(); i++) if (i != 2) collect(tabs.get(i), i, 0);
        }

        private void collect(View view, int tab, int depth) {
            if (depth > 5) return;
            if (
                view instanceof TextView && role(String.valueOf(((TextView) view).getText())) >= 0
            ) parts.add(new Part(view, tab));
            else if (
                view instanceof ImageView &&
                view.getWidth() <= 40 * unit &&
                view.getHeight() <= 40 * unit
            ) parts.add(new Part(view, tab));
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) collect(
                    group.getChildAt(i),
                    tab,
                    depth + 1
                );
            }
        }

        /** Idempotent: writes only what TikTok (or a recolour) has changed, so a steady frame writes nothing. */
        private void tint() {
            for (int i = 0; i < parts.size(); i++) {
                Part part = parts.get(i);
                if (part.view instanceof TextView) {
                    TextView title = (TextView) part.view;
                    int colour = manual
                        ? blend(
                              muted,
                              skin.text,
                              Math.max(0f, 1f - Math.abs(centres[part.tab] - px.v) / (64 * unit))
                          )
                        : progress >= 1f
                          ? part.tab == selected
                              ? skin.text
                              : muted
                          : part.tab == selected
                            ? blend(muted, skin.text, progress)
                            : part.tab == previous
                              ? blend(muted, skin.text, 1f - progress)
                              : muted;
                    if (title.getCurrentTextColor() != colour) title.setTextColor(colour);
                } else {
                    ImageView icon = (ImageView) part.view;
                    if (icon.getImageTintList() != iconTint) icon.setImageTintList(iconTint);
                }
            }
        }

        void update(ArrayList<View> tabs) {
            ViewGroup row = rail.get();
            if (row == null) return;
            int colour = Accent.colour();
            boolean recolour = skin == null || colour != accent,
                changed = recolour;
            if (recolour) {
                accent = colour;
                skin = Skin.tonal(row.getContext(), colour);
                iconTint = ColorStateList.valueOf(skin.text);
                muted = skin.muted();
                bodyColour = skin.card;
                pillColour = blend(skin.card, accent, .26f);
            }
            // Island body: the native bounds, inset at the sides, lifted a little only when the native cells leave room for it.
            float top = Float.MAX_VALUE,
                bottom = -Float.MAX_VALUE,
                narrow = Float.MAX_VALUE;
            for (int i = 0; i < tabs.size(); i++) {
                View cell = tabs.get(i);
                top = Math.min(top, cell.getY());
                bottom = Math.max(bottom, cell.getY() + cell.getHeight());
                narrow = Math.min(narrow, cell.getWidth());
                centres[i] = cell.getX() + cell.getWidth() / 2f;
            }
            top = Math.max(0f, top);
            bottom = Math.min((float) row.getHeight(), bottom);
            bottom -= Math.min(6 * unit, Math.max(0f, bottom - top - 56 * unit));
            float side = Math.min(12 * unit, narrow / 8f);
            next.set(side, top, row.getWidth() - side, bottom);
            if (!next.equals(body)) {
                body.set(next);
                radius = Math.min(body.height() / 2f, 40 * unit);
                outline.reset();
                outline.addRoundRect(body, radius, radius, Path.Direction.CW);
                changed = true;
            }
            // One width for every tab. Fit it to the outer cells rather than shifting
            // those indicators inward: their centres must stay on the native buttons.
            float room = body.width() - 12 * unit;
            for (int i = 0; i < 5; i++) if (i != 2) room = Math.min(
                room,
                2f * Math.min(centres[i] - body.left - 6 * unit, body.right - 6 * unit - centres[i])
            );
            pillWidth = Math.max(0f, Math.min(56 * unit, Math.min(narrow - 12 * unit, room)));
            pillHeight = Math.max(0f, Math.min(32 * unit, body.height() - 8 * unit));
            pillTop = body.top + 4 * unit;
            if (manual) {
                if (changed) invalidateSelf();
                return;
            }
            int chosen = -1;
            for (int i = 0; i < tabs.size(); i++) if (i != 2 && selected(tabs.get(i), 0)) {
                chosen = i;
                break;
            }
            if (chosen >= 0) {
                targetFor(chosen, next);
                if (chosen != selected) {
                    // Only a different tab starts the motion; the indicator is never restarted by geometry alone.
                    boolean first = selected < 0;
                    previous = selected;
                    selected = chosen;
                    target.set(next);
                    cancel();
                    progress = 1f;
                    if (first || !Motion.enabled(row.getContext())) {
                        pill.set(target);
                        previous = -1;
                    } else {
                        from.set(pill);
                        to.set(target);
                        progress = 0f;
                        animation = ValueAnimator.ofFloat(0f, 1f);
                        animation.setDuration(180L);
                        animation.setInterpolator(new PathInterpolator(0f, 0f, 1f, 1f));
                        animation.addListener(
                            new android.animation.AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(android.animation.Animator a) {
                                    if (animation == a) {
                                        animation = null;
                                        progress = 1f;
                                        pill.set(target);
                                        previous = -1;
                                        invalidateSelf();
                                    }
                                }
                            }
                        );
                        animation.addUpdateListener(a -> {
                            progress = (Float) a.getAnimatedValue();
                            step();
                        });
                        animation.start();
                    }
                    changed = true;
                } else if (!next.equals(target)) {
                    // Native tab shifted a little (insets, badge, font scale): follow it in place, keep any running motion.
                    target.set(next);
                    if (animation != null) to.set(target);
                    else pill.set(target);
                    changed = true;
                }
            }
            if (skin != null) tint();
            if (changed) invalidateSelf();
        }

        public void draw(Canvas canvas) {
            if (skin == null || body.right <= body.left) return;
            paint.setColor(bodyColour);
            paint.setAlpha(alpha);
            canvas.drawRoundRect(body, radius, radius, paint);
            int saved = canvas.save();
            canvas.clipPath(outline);
            if (selected >= 0) {
                paint.setColor(
                    blend(pillColour, accent, .45f * Math.min(1f, Math.max(0f, grow.v)))
                );
                paint.setAlpha(alpha);
                shape.set(pill.left, pill.top - lift, pill.right, pill.bottom + lift);
                float round = shape.height() / 2f;
                canvas.drawRoundRect(shape, round, round, paint);
            }
            canvas.restoreToCount(saved);
        }

        public void setAlpha(int alpha) {
            this.alpha = alpha;
            invalidateSelf();
        }

        public void setColorFilter(ColorFilter filter) {
            paint.setColorFilter(filter);
            invalidateSelf();
        }

        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    static int blend(int a, int b, float amount) {
        return Color.rgb(
            Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * amount),
            Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * amount),
            Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * amount)
        );
    }
}
