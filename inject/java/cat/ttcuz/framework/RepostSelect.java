package cat.ttcuz.framework;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Removing many reposts at once, on the Reposts tab of one's own profile.
 *
 * Hold a video for a moment: the tab turns into a selection -- every card shrinks a little and gets a round
 * check, a bar at the bottom counts what is chosen and offers "All" and "Remove". Taps then tick and untick;
 * scrolling works as always. Remove calls TikTok's own request, the one its "Undo repost" uses
 * (UpvoteApi.deleteUpvote: item_id, carrier_type 0 = a video, no repost_id, the experiment map TikTok sends),
 * one after another; each removed video leaves the tab's own list model at once (RepostListViewModel), so the
 * cards after it slide up and a reload never brings it back.
 *
 * Kept light: the Reposts tab is recognised once per list, the card → video path is found once per class and
 * then read through cached fields, and nothing runs on a frame outside a selection.
 *
 * Touches arrive through IslandTouch's window hook. Outside a selection only a hold on a card of the
 * Reposts tab is taken; everything else goes to TikTok untouched.
 */
final class RepostSelect {
    private RepostSelect() {}

    private static final long HOLD_MS = 350L;
    private static WeakReference<Activity> owner = new WeakReference<>(null);
    private static WeakReference<ViewGroup> grid = new WeakReference<>(null);
    private static final Set<String> chosen = new LinkedHashSet<>();
    private static boolean active, busy;
    private static FrameLayout bar;
    private static android.widget.PopupWindow window;
    private static TextView count, all, remove;
    private static ViewTreeObserver.OnPreDrawListener painter;
    // the gesture in progress
    private static float downX, downY;
    private static int slop = -1;
    private static boolean moved, held;
    /** In a selection: the press kept from TikTok until it is clear whether it is a tap or a scroll. */
    private static MotionEvent held_down;
    private static WeakReference<View> pressed = new WeakReference<>(null);
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static final Runnable hold = RepostSelect::holdFired, promote = RepostSelect::promoteFired;
    /** The window's own touch delivery, to send TikTok a cancel when a hold or a tap is taken. */
    private static java.util.function.Consumer<MotionEvent> base;

    // ------------------------------------------------------------------ touches

    /** Every touch of the window; true when it is taken. Outside a profile grid this returns at once. */
    static boolean touch(Activity activity, MotionEvent e, java.util.function.Consumer<MotionEvent> deliver) {
        int action = e.getActionMasked();
        if (busy && active) {
            // while removing, only the list itself is held still; the rest of the screen works
            if (action == MotionEvent.ACTION_DOWN) held = grid.get() != null && ProfileGrid.cellAt(activity.getWindow().getDecorView(), e.getRawX(), e.getRawY()) != null;
            boolean taken = held;
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) held = false;
            return taken;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            main.removeCallbacks(hold);
            main.removeCallbacks(promote);
            moved = held = false;
            if (held_down != null) { held_down.recycle(); held_down = null; }
            downX = e.getRawX(); downY = e.getRawY();
            View cell = ProfileGrid.cellAt(activity.getWindow().getDecorView(), downX, downY);
            pressed = new WeakReference<>(cell);
            if (cell == null) return false;
            ViewGroup list = cell.getParent() instanceof ViewGroup ? (ViewGroup) cell.getParent() : null;
            if (list == null) return false;
            if (!reposts(list)) {
                // the creator holding a video on any profile: promote it (only that account sees this)
                if (!active && AdminAccess.current()) { base = deliver; owner = new WeakReference<>(activity); if (slop < 0) slop = ViewConfiguration.get(activity).getScaledTouchSlop(); main.postDelayed(promote, 550L); }
                return false;
            }
            base = deliver;
            if (slop < 0) slop = ViewConfiguration.get(activity).getScaledTouchSlop();
            if (!active) { owner = new WeakReference<>(activity); grid = new WeakReference<>(list); main.postDelayed(hold, HOLD_MS); return false; }
            // In a selection TikTok does not get the press at all, so it can never open the video. If the finger
            // moves (a scroll), the press is handed to it then, and the list scrolls as usual.
            if (list != grid.get()) return false;
            held_down = MotionEvent.obtain(e);
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_POINTER_DOWN) {
            if (!moved && (action == MotionEvent.ACTION_POINTER_DOWN || Math.hypot(e.getRawX() - downX, e.getRawY() - downY) > Math.max(1, slop))) {
                moved = true;
                main.removeCallbacks(hold);
                main.removeCallbacks(promote);
                if (held_down != null) {
                    MotionEvent first = held_down;
                    held_down = null;
                    try { deliver.accept(first); } catch (Throwable ignored) { }
                    first.recycle();
                    return false; // this event goes on to TikTok after its press
                }
            }
            return held || held_down != null;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            main.removeCallbacks(hold);
            main.removeCallbacks(promote);
            if (held) { held = false; return true; }
            if (held_down != null) {
                // a tap in a selection: TikTok never saw it; it ticks the card
                held_down.recycle();
                held_down = null;
                View cell = pressed.get();
                if (action == MotionEvent.ACTION_UP && !moved && cell != null && cell.getParent() == grid.get()) toggle(cell);
                return true;
            }
        }
        return false;
    }

    private static void holdFired() {
        View cell = pressed.get();
        if (cell == null || moved || owner.get() == null || !cell.isAttachedToWindow()) return;
        held = true;
        // TikTok's own press (its long-press menu, the tap that opens the video) is called off
        long now = android.os.SystemClock.uptimeMillis();
        MotionEvent stop = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, downX, downY, 0);
        if (base != null) try { base.accept(stop); } catch (Throwable ignored) { }
        stop.recycle();
        if (Motion.haptics()) cell.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        if (!active) start();
        toggle(cell);
    }

    /** The creator held a video on a profile: TikTok's own press is called off and the promote panel opens. */
    private static void promoteFired() {
        View cell = pressed.get();
        Activity activity = owner.get();
        if (cell == null || moved || activity == null || !cell.isAttachedToWindow()) return;
        String aid = aidOf(cell);
        if (aid == null) return;
        held = true;
        long now = android.os.SystemClock.uptimeMillis();
        MotionEvent stop = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, downX, downY, 0);
        if (base != null) try { base.accept(stop); } catch (Throwable ignored) { }
        stop.recycle();
        if (Motion.haptics()) cell.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        Promo.ask(activity, aid);
    }

    // ------------------------------------------------------------------ which list, which video

    private static final int LIST_TAG = 0x7e0c0362;
    private static Method fragmentFinder;
    private static boolean noFinder;

    /** Whether a card list is the Reposts tab: its fragment is TikTok's RepostProfileListFragment (kept on the list). */
    private static boolean reposts(ViewGroup list) {
        Object known = list.getTag(LIST_TAG);
        if (known instanceof Boolean) return (Boolean) known;
        Object fragment = fragmentOf(list);
        if (fragment == null) return false; // not attached to its fragment yet: asked again next time
        boolean is = fragment.getClass().getName().endsWith("RepostProfileListFragment");
        list.setTag(LIST_TAG, is);
        return is;
    }

    private static Object fragmentOf(View view) {
        if (noFinder) return null;
        try {
            if (fragmentFinder == null) fragmentFinder = Class.forName("androidx.fragment.app.FragmentManager").getMethod("LJJJZ", View.class);
            return fragmentFinder.invoke(null, view);
        } catch (ClassNotFoundException | NoSuchMethodException missing) {
            noFinder = true;
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static final String AWEME = "com.ss.android.ugc.aweme.feed.model.Aweme";
    /** list class → its (View) → ViewHolder method; holder class → the fields that lead to its Aweme. Found once. */
    private static final java.util.Map<Class<?>, Method> holderMethods = new java.util.HashMap<>();
    private static final java.util.Map<Class<?>, Field[]> paths = new java.util.HashMap<>();
    private static final Field[] NO_PATH = new Field[0];

    /** The video a card shows now (read every time: the list reuses its cards for other videos). */
    static String aidOf(View cell) {
        Object post = awemeOf(holderOf(cell));
        String aid = NativeRead.aid(post);
        return aid.isEmpty() ? null : aid;
    }

    private static Object holderOf(View cell) {
        if (!(cell.getParent() instanceof ViewGroup)) return null;
        ViewGroup list = (ViewGroup) cell.getParent();
        Class<?> listType = list.getClass();
        Method m;
        if (holderMethods.containsKey(listType)) m = holderMethods.get(listType);
        else {
            m = null;
            // RecyclerView.getChildViewHolder(View), whatever it is called in this build
            for (Class<?> type = listType; m == null && type != null && type != ViewGroup.class; type = type.getSuperclass())
                for (Method each : type.getDeclaredMethods()) {
                    Class<?>[] p = each.getParameterTypes();
                    if (p.length == 1 && p[0] == View.class && !java.lang.reflect.Modifier.isStatic(each.getModifiers())
                            && each.getReturnType().getName().endsWith("ViewHolder")) {
                        if (each.getName().equals("getChildViewHolder")) { m = each; break; }
                        if (m == null) m = each;
                    }
                }
            if (m != null) m.setAccessible(true);
            holderMethods.put(listType, m);
        }
        try { return m == null ? null : m.invoke(list, cell); } catch (Throwable ignored) { return null; }
    }

    /** The Aweme a view holder carries: through the fields found the first time for its class. */
    private static Object awemeOf(Object holder) {
        if (holder == null) return null;
        Class<?> type = holder.getClass();
        Field[] path = paths.get(type);
        if (path != null && path != NO_PATH) {
            Object at = holder;
            try { for (Field f : path) { at = f.get(at); if (at == null) return null; } } catch (Throwable ignored) { at = null; }
            if (at != null && at.getClass().getName().equals(AWEME)) return at;
        }
        if (path == NO_PATH) return null;
        List<Field> found = new ArrayList<>();
        Object post = find(holder, 0, found);
        if (post != null) paths.put(type, found.toArray(new Field[0]));
        else if (path == null && paths.size() < 64) paths.put(type, NO_PATH);
        return post;
    }

    private static Object find(Object from, int depth, List<Field> path) {
        if (from == null || depth > 2) return null;
        for (Class<?> type = from.getClass(); type != null && type != Object.class && !type.getName().startsWith("android."); type = type.getSuperclass()) {
            for (Field f : type.getDeclaredFields()) {
                if (f.getType().isPrimitive() || View.class.isAssignableFrom(f.getType()) || java.lang.reflect.Modifier.isStatic(f.getModifiers())
                        || f.getType() == String.class || Number.class.isAssignableFrom(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    Object value = f.get(from);
                    if (value == null || value == from) continue;
                    path.add(f);
                    if (value.getClass().getName().equals(AWEME)) return value;
                    if (depth < 2 && !(value instanceof java.util.Collection) && !(value instanceof java.util.Map)) {
                        Object deeper = find(value, depth + 1, path);
                        if (deeper != null) return deeper;
                    }
                    path.remove(path.size() - 1);
                } catch (Throwable ignored) { }
            }
        }
        return null;
    }

    /** Every video the tab has loaded, from its list model (for "All"). */
    private static List<String> loaded() {
        List<String> out = new ArrayList<>();
        Object model = model();
        if (model != null) try {
            java.util.List<?> items = (java.util.List<?>) model.getClass().getMethod("listGetAll").invoke(model);
            Set<String> seen = new java.util.HashSet<>();
            for (Object item : items) { String aid = aidOfItem(model, item); if (!aid.isEmpty() && seen.add(aid)) out.add(aid); }
            if (!out.isEmpty()) return out;
        } catch (Throwable ignored) { }
        // else the cards on screen
        ViewGroup list = grid.get();
        if (list != null) for (int i = 0; i < list.getChildCount(); i++) { String aid = aidOf(list.getChildAt(i)); if (aid != null && !out.contains(aid)) out.add(aid); }
        return out;
    }

    // ------------------------------------------------------------------ the selection

    private static void start() {
        ViewGroup list = grid.get();
        Activity activity = owner.get();
        if (list == null || activity == null) return;
        active = true;
        chosen.clear();
        // Cards are dressed as the list draws them (it reuses them while scrolling); the work per frame is a
        // couple of cached field reads per card, nothing is searched.
        painter = () -> { paint(); return true; };
        list.getViewTreeObserver().addOnPreDrawListener(painter);
        showBar(activity);
        Diary.note("repost select: on");
    }

    static void stop() {
        main.removeCallbacks(hold);
        ViewGroup list = grid.get();
        if (list != null) {
            if (painter != null) try { list.getViewTreeObserver().removeOnPreDrawListener(painter); } catch (Throwable ignored) { }
            for (int i = 0; i < list.getChildCount(); i++) undress(list.getChildAt(i));
        }
        painter = null;
        active = false;
        busy = false;
        held = false;
        chosen.clear();
        if (held_down != null) { held_down.recycle(); held_down = null; }
        if (bar != null) {
            final View gone = bar.getChildCount() > 0 ? bar.getChildAt(0) : bar;
            final android.widget.PopupWindow closing = window;
            gone.animate().translationY(gone.getHeight() + 200f).alpha(0f).setDuration(Motion.time(220L)).setInterpolator(Motion.IN)
                    .withEndAction(() -> { try { if (closing != null && closing.isShowing()) closing.dismiss(); } catch (Throwable ignored) { } }).start();
            bar = null;
            window = null;
        }
        count = all = remove = null;
    }

    private static void toggle(View cell) {
        String aid = aidOf(cell);
        if (aid == null) { Screen.say(Text.t("Не удалось прочитать это видео")); return; }
        if (!chosen.remove(aid)) chosen.add(aid);
        if (chosen.isEmpty()) { stop(); return; }
        Mark mark = markOf(cell);
        if (mark != null) mark.pop();
        paint();
        counted();
    }

    private static final int TAG = 0x7e0c0360;

    /** The cards on screen: shrunk with a check when chosen, a hollow circle otherwise. */
    private static void paint() {
        ViewGroup list = grid.get();
        if (!active) return;
        if (list == null || !list.isAttachedToWindow()) { main.post(RepostSelect::stop); return; }
        for (int i = 0; i < list.getChildCount(); i++) {
            View cell = list.getChildAt(i);
            Mark mark = markOf(cell);
            boolean fresh = mark == null;
            if (fresh) {
                mark = new Mark(cell);
                cell.setTag(TAG, mark);
                cell.getOverlay().add(mark);
            }
            String aid = aidOf(cell);
            boolean on = aid != null && chosen.contains(aid);
            mark.set(on);
            float scale = on ? .88f : .96f;
            if (Math.abs(cell.getScaleX() - scale) <= .001f || mark.animating) continue;
            if (fresh) { cell.setScaleX(scale); cell.setScaleY(scale); continue; } // a card that just came on screen: no animation
            mark.animating = true;
            final Mark m = mark;
            cell.animate().scaleX(scale).scaleY(scale).setDuration(Motion.time(220L)).setInterpolator(Motion.OUT).withEndAction(() -> m.animating = false).start();
        }
    }

    private static Mark markOf(View cell) {
        Object tag = cell.getTag(TAG);
        return tag instanceof Mark ? (Mark) tag : null;
    }

    private static void undress(View cell) {
        Mark mark = markOf(cell);
        if (mark != null) cell.getOverlay().remove(mark);
        cell.setTag(TAG, null);
        // folded cards come back too: the list reuses them for the videos that stay
        cell.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(Motion.time(220L)).setInterpolator(Motion.OUT).start();
    }

    /** A round check in the top corner of a card, drawn over it (the card's own overlay). */
    private static final class Mark extends Drawable {
        final View cell;
        final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), ring = new Paint(Paint.ANTI_ALIAS_FLAG), tick = new Paint(Paint.ANTI_ALIAS_FLAG), dim = new Paint();
        final Path path = new Path();
        boolean on, animating;
        float grow = 0f;
        Mark(View cell) {
            this.cell = cell;
            setBounds(0, 0, Math.max(1, cell.getWidth()), Math.max(1, cell.getHeight()));
            ring.setStyle(Paint.Style.STROKE);
            ring.setColor(0xFFFFFFFF);
            tick.setStyle(Paint.Style.STROKE);
            tick.setStrokeCap(Paint.Cap.ROUND);
            tick.setStrokeJoin(Paint.Join.ROUND);
            tick.setColor(0xFFFFFFFF);
            dim.setColor(0x55000000);
        }
        void set(boolean value) { if (on != value) { on = value; grow = value ? 0f : 1f; invalidateSelf(); } else if (on && grow < 1f) invalidateSelf(); }
        void pop() { grow = 0f; invalidateSelf(); }
        @Override public void draw(Canvas c) {
            float d = cell.getResources().getDisplayMetrics().density;
            int w = cell.getWidth(), h = cell.getHeight();
            if (on) c.drawRect(0, 0, w, h, dim);
            float r = 11 * d, cx = w - 16 * d, cy = 16 * d;
            ring.setStrokeWidth(2 * d);
            if (on) {
                if (grow < 1f) { grow = Math.min(1f, grow + .16f); cell.postInvalidateOnAnimation(); }
                float s = .6f + .4f * grow + (float) Math.sin(grow * Math.PI) * .15f;
                fill.setColor(Accent.colour());
                c.drawCircle(cx, cy, r * s, fill);
                tick.setStrokeWidth(2.2f * d);
                path.reset();
                path.moveTo(cx - 5 * d, cy);
                path.lineTo(cx - 1.5f * d, cy + 3.5f * d);
                path.lineTo(cx + 5 * d, cy - 3.5f * d);
                c.drawPath(path, tick);
            } else {
                fill.setColor(0x33000000);
                c.drawCircle(cx, cy, r, fill);
                c.drawCircle(cx, cy, r, ring);
            }
        }
        @Override public void setAlpha(int a) { }
        @Override public void setColorFilter(android.graphics.ColorFilter f) { }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }

    // ------------------------------------------------------------------ the bar

    private static float density = 3f;
    private static int dp(float v) { return Math.round(v * density); }

    private static void showBar(Activity owner) {
        density = owner.getResources().getDisplayMetrics().density;
        Skin skin = Skin.tonal(owner, Accent.colour());
        FrameLayout holder = new FrameLayout(owner);
        LinearLayout row = new LinearLayout(owner);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), dp(10), dp(10), dp(10));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(skin.card);
        bg.setCornerRadius(dp(28));
        row.setBackground(bg);
        row.setElevation(dp(8));
        count = new TextView(owner);
        count.setTextColor(skin.text);
        count.setTextSize(15);
        count.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        row.addView(count, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = pill(owner, skin, "✕", false);
        close.setOnClickListener(v -> stop());
        all = pill(owner, skin, Text.t("Все"), false);
        all.setOnClickListener(v -> {
            List<String> every = loaded();
            if (every.isEmpty()) { Screen.say(Text.t("Не удалось прочитать список")); return; }
            boolean already = chosen.containsAll(every);
            if (already) { chosen.clear(); stop(); return; }
            chosen.addAll(every);
            paint();
            counted();
        });
        remove = pill(owner, skin, Text.t("Удалить"), true);
        remove.setOnClickListener(v -> confirm());
        row.addView(all);
        row.addView(remove);
        row.addView(close);
        FrameLayout.LayoutParams at = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        at.leftMargin = at.rightMargin = dp(12);
        at.topMargin = at.bottomMargin = dp(8);
        holder.addView(row, at);
        // its own small window over the screen: nothing of TikTok's (the video, its panels) can cover it
        holder.setPadding(0, 0, 0, 0);
        android.widget.PopupWindow popup = new android.widget.PopupWindow(holder, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        popup.setTouchable(true);
        popup.setOutsideTouchable(false);
        popup.setClippingEnabled(false);
        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
        popup.setElevation(dp(12));
        try { popup.showAtLocation(owner.getWindow().getDecorView(), Gravity.BOTTOM, 0, dp(96)); }
        catch (Throwable error) { Diary.note("repost bar: " + error); }
        window = popup;
        bar = holder;
        Fonts.applyTree(holder);
        row.setTranslationY(dp(120));
        row.setAlpha(0f);
        row.animate().translationY(0f).alpha(1f).setDuration(Motion.time(380L)).setInterpolator(Motion.OUT).start();
        counted();
    }

    private static TextView pill(Activity owner, Skin skin, String words, boolean main) {
        TextView t = new TextView(owner);
        t.setText(words);
        t.setTextSize(14);
        t.setGravity(Gravity.CENTER);
        t.setMinWidth(dp(44));
        t.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        t.setPadding(dp(14), dp(9), dp(14), dp(9));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor(main ? 0xFFE5484D : (Accent.colour() & 0xFFFFFF) | 0x33000000);
        t.setBackground(bg);
        t.setTextColor(main ? 0xFFFFFFFF : skin.text);
        Motion.touch(t);
        LinearLayout.LayoutParams at = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        at.leftMargin = dp(6);
        t.setLayoutParams(at);
        return t;
    }

    private static void counted() {
        if (count == null || remove == null) return;
        int n = chosen.size();
        count.setText(Text.t("Выбрано") + ": " + n);
        remove.setEnabled(n > 0);
        remove.setAlpha(n > 0 ? 1f : .5f);
    }

    // ------------------------------------------------------------------ removing

    private static void confirm() {
        final List<String> list = new ArrayList<>(chosen);
        Activity owner = RepostSelect.owner.get();
        if (list.isEmpty() || owner == null || busy) return;
        Panel ask = Panel.with(owner, Skin.tonal(owner, Accent.colour()), Text.t("Удалить репосты?"));
        ask.text(Text.t("Репостов будет удалено") + ": " + list.size() + ". " + Text.t("Видео останутся у авторов, пропадут только из ваших репостов."));
        ask.primary(Text.t("Удалить"), () -> run(list));
        ask.quiet(Text.t("Отмена"), null);
        ask.show();
    }

    private static void run(final List<String> list) {
        if (busy || !active) return;
        busy = true;
        if (remove != null) remove.setEnabled(false);
        if (all != null) all.setEnabled(false);
        Net.away("repost delete", () -> {
            int done = 0, failed = 0;
            String experiments = null;
            Object api = null;
            Method delete = null, wait = null;
            try {
                api = Class.forName("com.ss.android.ugc.aweme.upvote.api.UpvoteApi").getField("LIZIZ").get(null);
                delete = api.getClass().getMethod("deleteUpvote", String.class, Integer.class, String.class, String.class);
                try { experiments = (String) Class.forName("X.15As").getMethod("LJLLL").invoke(null); } catch (Throwable ignored) { }
            } catch (Throwable error) {
                Diary.note("repost delete: no api -- " + error);
            }
            for (int i = 0; i < list.size() && active; i++) {
                final int at = i + 1;
                main.post(() -> { if (count != null) count.setText(Text.t("Удаляем") + " " + at + " / " + list.size()); });
                boolean ok = false;
                if (api != null) try {
                    Object request = delete.invoke(api, list.get(i), 0, null, experiments);
                    // TikTok's Rx request: LJ() waits for its one answer
                    if (wait == null) wait = request.getClass().getMethod("LJ");
                    Object answer = wait.invoke(request);
                    Object code = NativeRead.field(answer, "status_code");
                    if (code == null) code = NativeRead.get(answer, "getStatusCode");
                    ok = code == null || (code instanceof Number && ((Number) code).intValue() == 0);
                } catch (Throwable error) {
                    Diary.note("repost delete " + list.get(i) + ": " + (error.getCause() != null ? error.getCause() : error));
                }
                if (ok) { done++; final String gone = list.get(i); main.post(() -> drop(gone)); } else failed++;
                // TikTok's server takes these one at a time; a short gap keeps it from refusing
                if (i + 1 < list.size()) try { Thread.sleep(200L); } catch (InterruptedException ignored) { }
            }
            final int removed = done, missed = failed;
            main.post(() -> {
                // every removed one already left the list one by one (drop); no reload, so nothing comes back
                main.postDelayed(RepostSelect::stop, 300L);
                Screen.say(missed == 0 ? Text.t("Удалено репостов") + ": " + removed
                        : Text.t("Удалено") + ": " + removed + ", " + Text.t("не удалось") + ": " + missed);
            });
        });
    }

    private static Method modelGetter, itemPost;
    private static Class<?> itemPostOwner;

    /** The Reposts tab's own list model (RepostListViewModel, a PowerList AssemListViewModel). */
    private static Object model() {
        ViewGroup list = grid.get();
        Object fragment = list == null ? null : fragmentOf(list);
        if (fragment == null) return null;
        try {
            if (modelGetter == null || !modelGetter.getDeclaringClass().isInstance(fragment)) modelGetter = fragment.getClass().getMethod("SX");
            return modelGetter.invoke(fragment);
        } catch (Throwable e) { return null; }
    }

    /** The video of one list item, through the model's own item → Aweme (b53). */
    private static String aidOfItem(Object model, Object item) {
        try {
            if (itemPostOwner != model.getClass() || itemPost == null || !itemPost.getParameterTypes()[0].isInstance(item)) {
                itemPost = null;
                itemPostOwner = model.getClass();
                for (Method m : itemPostOwner.getMethods())
                    if (m.getName().equals("b53") && m.getParameterTypes().length == 1 && m.getParameterTypes()[0].isInstance(item)) { itemPost = m; break; }
            }
            if (itemPost != null && itemPost.getParameterTypes()[0].isInstance(item)) {
                String aid = NativeRead.aid(itemPost.invoke(model, item));
                if (!aid.isEmpty()) return aid;
            }
        } catch (Throwable ignored) { }
        return NativeRead.aid(find(item, 0, new ArrayList<>()));
    }

    /**
     * A repost was removed: what TikTok's own "Undo repost" does after success. Its caches forget the video
     * (0pPn.LJI, "delete_success"), and the tab's list drops that one item, so the cards after it slide into place
     * and a reload never brings it back.
     */
    private static void drop(String aid) {
        try {
            Object cache = Class.forName("X.0A1H").getField("LIZ").get(null);
            cache.getClass().getMethod("LJI", String.class).invoke(cache, aid);
        } catch (Throwable error) {
            Diary.note("repost cache: " + error);
        }
        chosen.remove(aid);
        Object model = model();
        if (model == null) return;
        try {
            java.util.List<?> items = (java.util.List<?>) model.getClass().getMethod("listGetAll").invoke(model);
            for (int i = 0; items != null && i < items.size(); i++) {
                if (aid.equals(aidOfItem(model, items.get(i)))) {
                    model.getClass().getMethod("listRemoveItemAt", int.class).invoke(model, i);
                    return;
                }
            }
        } catch (Throwable error) {
            Diary.note("repost list: " + error);
        }
    }
}
