package cat.narezany.margyt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/** Adds swipe-down lock and a small status pill to TikTok's native edge-speed gesture. */
public final class SpeedGesture {
    private SpeedGesture() {}

    private static final Map<Object, State> states = new WeakHashMap<Object, State>();
    private static final int DOWN_DISTANCE_DP = 40;
    static boolean captionsLow(View root) {
        synchronized(states){for(State state:states.values())if(state.pill!=null && state.pill.box.getRootView()==root)return true;}
        return false;
    }
    static String rateLabel(){float value=PlayerTools.speed();return (value==(int)value?Integer.toString((int)value):Float.toString(value))+"×";}
    static void refreshRate(){synchronized(states){for(State state:states.values())if(state.pill!=null){PlayerTools.applyHoldRate();break;}}}
    private static int onAccent() {
        int c=Accent.colour();return (((c>>16)&255)*299+((c>>8)&255)*587+(c&255)*114)/1000>150?0xFF171C24:0xFFFFFFFF;
    }
    
    /** Called from the 47.2.41 feed touch listener before its own gesture logic. */
    public static boolean onTouch(Object listener, View target, MotionEvent event) {
        if (listener == null || target == null || event == null) return false;
        try {
            Object component = read(listener, "l0");
            if (component == null) component = listener; // Native edge listener LX/16wu.
            Object ability = ability(component);
            int action = event.getActionMasked();
            State state;
            synchronized (states) {
                Object key = ability == null ? component : ability;
                state = states.get(key);
                if (state == null && ability != null) state = states.remove(component);
                if (state == null) {
                    state = new State();
                }
                states.put(key, state);
            }

            int video = videoIdentity(component);
            if (video != 0 && state.video != 0 && video != state.video) {
                if (state.locked && ability != null) stopSpeeding(ability);
                cancelSpeedupWatch(target, state);
                hidePill(state);
                state.resetForVideo();
            }
            if (video != 0) state.video = video;

            if (action == MotionEvent.ACTION_DOWN) {
                state.downX = event.getRawX();
                state.downY = event.getRawY();
                state.handled = false;
                state.choice = state.locked ? 1 : 0;
                state.wasPinned = state.locked;
                state.lockingGesture = false;
                state.hintShown = false;
                // TikTok can drop the speed on its own (app switch, video
                // restart). A lock that no longer matches reality must not
                // keep swallowing taps, so it is released here.
                if (state.locked && ability != null && !isSpeeding(ability)) {
                    state.locked = false;
                    state.lockingGesture = false;
                }
                state.sawSpeedup = state.locked || isSpeeding(ability);
                if (!state.locked) watchForSpeedup(target, component, state);
                else {
                    disallowIntercept(target);
                    return true; // Do not schedule another native long press while pinned.
                }
                return false;
            }

            boolean active = isSpeeding(ability);
            if (active) {
                if(!state.sawSpeedup)PlayerTools.applyHoldRate();
                state.sawSpeedup = true;
                if (!state.locked && !state.hintShown) {
                    state.hintShown = true;
                    showPill(target, HOLD, ability, state);
                }
            }

            if (action == MotionEvent.ACTION_MOVE && (active || state.sawSpeedup || state.locked)) {
                float dy=event.getRawY()-state.downY,dx=Math.abs(event.getRawX()-state.downX);
                float threshold=dp(target.getContext(),DOWN_DISTANCE_DP);
                if(dx>=threshold && dx>Math.abs(dy) && !state.handled){
                    if(state.locked && ability!=null)stopSpeeding(ability);
                    state.locked=false;state.sawSpeedup=false;hidePill(state);allowIntercept(target);return false;
                }
                // Consume vertical movement throughout the hold. The choice can
                // reverse before UP; the native player keeps running at 2x.
                disallowIntercept(target);state.handled=true;
                int choice=state.wasPinned
                        ? (dy>=threshold?-1:dy<=-threshold?1:state.choice)
                        : (dy>=threshold?1:dy<=-threshold?-1:state.choice);
                if(choice!=state.choice){state.choice=choice;state.locked=choice==1;
                    showPill(target,choice==1?LOCKED:RELEASE_ON_UP,ability,state);
                }
                return true;
            }
            if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL){
                cancelSpeedupWatch(target,state);
                boolean consumed=state.handled || state.locked;
                boolean keep=action==MotionEvent.ACTION_UP && state.locked && state.choice>=0;
                if(keep){state.sawSpeedup=true;state.handled=false;showPill(target,LOCKED_IDLE,ability,state);allowIntercept(target);return true;}
                if(consumed && ability!=null)stopSpeeding(ability);
                state.locked=false;state.choice=0;state.handled=false;state.lockingGesture=false;
                state.sawSpeedup=false;state.hintShown=false;hidePill(state);allowIntercept(target);
                return consumed;
            }

        } catch (Throwable ignored) {
            // This hook must never interfere with TikTok's original touch path.
        }
        return false;
    }

    /** The feed pager must not take a vertical swipe that belongs to the speed lock. */
    private static void disallowIntercept(View target) {
        try {
            for (android.view.ViewParent p = target.getParent(); p != null; p = p.getParent())
                p.requestDisallowInterceptTouchEvent(true);
        } catch (Throwable ignored) { }
    }

    private static void allowIntercept(View target) {
        try {
            for (android.view.ViewParent p = target.getParent(); p != null; p = p.getParent())
                p.requestDisallowInterceptTouchEvent(false);
        } catch (Throwable ignored) { }
    }

    private static Object ability(Object component) {
        Object edge = read(component, "LLJL"); // LX/16wu owns the actual speed touch path.
        if (edge != null) return edge;
        edge = read(component, "LLLILZJ");
        if (edge != null) return edge;
        // Older feed component layout keeps EdgeSpeedupAbility in a Kotlin lazy.
        return call(read(component, "LLLL"), "getValue");
    }

    private static boolean isSpeeding(Object ability) {
        Object value = call(ability, "R62");
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    /** TikTok 47.2.41 ends edge speed through EdgeSpeedupAssem.Hj(boolean). */
    private static boolean stopSpeeding(Object ability) {
        try {
            Method method = method(ability.getClass(), "Hj", Boolean.TYPE);
            method.invoke(ability, Boolean.FALSE);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** A long press has no MOVE events, so observe TikTok's own speed flag. */
    private static void watchForSpeedup(final View target, final Object component,
                                        final State state) {
        cancelSpeedupWatch(target, state);
        Runnable watcher = new Runnable() {
            private int checks;
            @Override public void run() {
                if (state.watcher != this || state.locked) return;
                Object ability = ability(component); // DOWN initializes native ability after our hook.
                if (isSpeeding(ability)) {
                    state.watcher = null;
                    disallowIntercept(target);
                    state.sawSpeedup = true;PlayerTools.applyHoldRate();
                    if (!state.hintShown) {
                        state.hintShown = true;
                        showPill(target, HOLD, ability, state);
                    }
                    return;
                }
                if (++checks < 40) target.postDelayed(this, 50L);
                else state.watcher = null;
            }
        };
        state.watcher = watcher;
        target.postDelayed(watcher, 50L);
    }

    private static void cancelSpeedupWatch(View target, State state) {
        Runnable watcher = state.watcher;
        if (watcher != null) target.removeCallbacks(watcher);
        state.watcher = null;
    }

    private static Object call(Object receiver, String name) {
        if (receiver == null) return null;
        try {
            return method(receiver.getClass(), name).invoke(receiver);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static final java.util.concurrent.ConcurrentHashMap<String,Method> methods=new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<String,Field> fields=new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Set<String> absent=java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String,Boolean>());
    private static Method method(Class<?> type, String name, Class<?>... parameters)
            throws NoSuchMethodException {
        String key=type.getName()+"#"+name+(parameters.length==0?"":parameters[0].getName());
        Method cached=methods.get(key);if(cached!=null)return cached;
        if(absent.contains(key))throw new NoSuchMethodException(name);
        Class<?> current = type;
        while (current != null) {
            try {
                Method found = current.getDeclaredMethod(name, parameters);
                found.setAccessible(true);methods.put(key,found);
                return found;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        absent.add(key);throw new NoSuchMethodException(name);
    }

    private static Object read(Object receiver, String name) {
        if (receiver == null) return null;
        String key=receiver.getClass().getName()+"@"+name;if(absent.contains(key))return null;
        Field cached=fields.get(key);if(cached!=null){try{return cached.get(receiver);}catch(Throwable ignored){return null;}}
        Class<?> current = receiver.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);fields.put(key,field);
                return field.get(receiver);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }
        absent.add(key);return null;
    }

    private static int videoIdentity(Object component) {
        Object params = read(component, "LLLLIILL");
        Object aweme = call(params, "getAweme");
        return aweme == null ? 0 : System.identityHashCode(aweme);
    }

    private static final int HOLD = 0;        // 2x while the finger is down
    private static final int LOCKED = 1;          // just pinned
    private static final int LOCKED_IDLE = 2;     // pinned, waiting for a swipe to release
    private static final int RELEASE_ON_UP = 4;
    private static final int UNLOCKED = 3;        // just released

    private static String label(int mode) {
        switch (mode) {
            case LOCKED: return "Отпустите · закрепить "+rateLabel();
            case RELEASE_ON_UP: return "Отпустите · без фиксации";
            case LOCKED_IDLE: return "Вниз · снять фиксацию";
            case UNLOCKED: return "Фиксация снята";
            default: return "Вниз · закрепить "+rateLabel();
        }
    }

    /**
     * One accent pill at the bottom of the feed. It lives as long as the speed-up
     * does; a change of state only swaps its text, and the pill grows or shrinks
     * from both sides to fit it.
     */
    private static void showPill(final View target, final int mode, final Object ability,
                                 final State state) {
        final View root = target.getRootView();
        if (!(root instanceof ViewGroup)) return;
        final Context context = target.getContext();
        final int token = ++state.pillToken;
        Pill pill = state.pill;
        if (pill == null || pill.box.getParent() == null) {
            pill = new Pill(context, label(mode), mode == UNLOCKED || mode == RELEASE_ON_UP);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    pill.targetWidth(label(mode)), ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            int nav = 0;
            try {
                int id = context.getResources().getIdentifier(
                        "navigation_bar_height", "dimen", "android");
                if (id != 0) nav = context.getResources().getDimensionPixelSize(id);
            } catch (Throwable ignored) { }
            params.bottomMargin = nav + dp(context, 44);
            ((ViewGroup) root).addView(pill.box, params);
            pill.box.bringToFront();
            pill.box.setAlpha(0f);
            pill.box.animate().alpha(1f).setDuration(120L).start();
            state.pill = pill;
        } else {
            pill.change(label(mode), mode == UNLOCKED || mode == RELEASE_ON_UP);
        }
        pill.clearTimers();
        final boolean pinned = mode == LOCKED || mode == LOCKED_IDLE;
        pill.box.setClickable(pinned && ability != null);
        if (pinned && ability != null) {
            pill.box.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    if (stopSpeeding(ability)) {
                        state.locked = false;
                        state.sawSpeedup = false;
                        state.lockingGesture = false;
                        showPill(target, UNLOCKED, null, state);
                    }
                }
            });
        } else {
            pill.box.setOnClickListener(null);
        }
        if (mode != UNLOCKED) {
            pill.monitor = new Runnable() {
                @Override public void run() {
                    if (state.pillToken != token || state.pill == null) return;
                    boolean visible = target.isAttachedToWindow()
                            && target.isShown() && target.getWindowVisibility() == View.VISIBLE;
                    if (!visible || !isSpeeding(ability)) {
                        if (!visible && state.locked) stopSpeeding(ability);
                        state.locked = false;
                        state.sawSpeedup = false;
                        hidePill(state);
                        return;
                    }
                    state.pill.box.postDelayed(this, 200L);
                    state.pill.recolour();
                }
            };
            pill.box.postDelayed(pill.monitor,200L);
        }
        if (mode == LOCKED) {
            // After a moment the confirmation gives way to the release hint.
            pill.transition = new Runnable() {
                @Override public void run() {
                    if (state.pillToken == token && state.locked)
                        showPill(target, LOCKED_IDLE, ability, state);
                }
            };
            pill.box.postDelayed(pill.transition,1300L);
        } else if (mode == UNLOCKED) {
            pill.transition = new Runnable() {
                @Override public void run() {
                    if (state.pillToken == token) hidePill(state);
                }
            };
            pill.box.postDelayed(pill.transition,1100L);
        }
    }

    private static void hidePill(final State state) {
        state.pillToken++;
        final Pill pill = state.pill;
        state.pill = null;
        if (pill == null) return;
        pill.cancel();
        pill.box.setOnClickListener(null);
        pill.box.animate().alpha(0f).setDuration(150L).withEndAction(new Runnable() {
            @Override public void run() {
                if (pill.box.getParent() instanceof ViewGroup)
                    ((ViewGroup) pill.box.getParent()).removeView(pill.box);
            }
        }).start();
    }

    private static final class Pill {
        final LinearLayout box;
        final LockIcon icon;
        final TextView text;
        final GradientDrawable background=new GradientDrawable();
        private int colour;
        private final float density;
        private final int padH;
        private final int gap;
        private final int iconSize;
        private Runnable monitor,transition;
        private String shown;

        Pill(Context context, String label, boolean open) {
            density = context.getResources().getDisplayMetrics().density;
            padH = dp(context, 18);
            gap = dp(context, 10);
            iconSize = dp(context, 22);
            shown = label;
            box = new LinearLayout(context);
            box.setOrientation(LinearLayout.HORIZONTAL);
            box.setGravity(Gravity.CENTER);
            box.setPadding(padH, dp(context, 8), padH, dp(context, 8));
            box.setClipChildren(true);
            colour=Accent.colour();background.setColor(colour);
            background.setCornerRadius(dp(context, 40));
            box.setBackgroundDrawable(background);
            box.setElevation(dp(context, 6));
            icon = new LockIcon(context, open);
            box.addView(icon, new LinearLayout.LayoutParams(
                    iconSize * LockArt.WIDTH / LockArt.HEIGHT, iconSize));
            text = new TextView(context);
            text.setText(label);
            text.setTextColor(onAccent());
            text.setTextSize(14f);
            text.setTypeface(null, android.graphics.Typeface.BOLD);
            text.setSingleLine(true);
            text.setEllipsize(android.text.TextUtils.TruncateAt.END);
            Fonts.apply(text);
            text.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textParams.leftMargin = gap;
            box.addView(text, textParams);
        }

        int targetWidth(String label) {
            return Math.min((int)(text.getResources().getDisplayMetrics().widthPixels-32*density), padH * 2 + iconSize * LockArt.WIDTH / LockArt.HEIGHT + gap + (int) Math.ceil(text.getPaint().measureText(label)));
        }
        void recolour(){int next=Accent.colour();if(next==colour)return;colour=next;background.setColor(next);text.setTextColor(onAccent());icon.paint.setColorFilter(new PorterDuffColorFilter(onAccent(),PorterDuff.Mode.SRC_IN));icon.invalidate();}

        void clearTimers(){
            if(monitor!=null)box.removeCallbacks(monitor);
            if(transition!=null)box.removeCallbacks(transition);
            monitor=transition=null;
        }
        void cancel() {
            clearTimers();
            text.animate().withEndAction(null).cancel();
            text.setAlpha(1f);
            box.animate().withEndAction(null).cancel();
            box.setScaleX(1f);
        }

        /** Fade the words over, and let the pill follow them out to both sides. */
        void change(final String label, boolean open) {
            icon.setOpen(open);
            if (label.equals(shown)) return;
            shown = label;
            cancel();
            box.setAlpha(1f);
            final ViewGroup.LayoutParams params = box.getLayoutParams();
            final int from = box.getWidth() > 0 ? box.getWidth() : params.width;
            final int to = targetWidth(label);
            params.width=to;box.setLayoutParams(params);
            // One layout per label change; the settling motion runs on view properties.
            box.animate().withEndAction(null).cancel();
            box.setScaleX(Math.max(.92f,Math.min(1.08f,(float)from/Math.max(1,to))));
            box.animate().scaleX(1f).setDuration(260L).setInterpolator(new DecelerateInterpolator()).start();
            text.animate().withEndAction(null).cancel();
            text.animate().alpha(0f).setDuration(90L).withEndAction(new Runnable() {
                @Override public void run() {
                    text.setText(label);
                    text.animate().alpha(1f).setDuration(140L).start();
                }
            }).start();
        }
    }

    /** The supplied padlock artwork (see LockArt), fitted into the view and tinted. */
    private static final class LockIcon extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Rect src = new Rect();
        private final RectF dst = new RectF();
        private boolean open;

        LockIcon(Context context, boolean open) {
            super(context);
            this.open = open;
            paint.setColorFilter(new PorterDuffColorFilter(onAccent(), PorterDuff.Mode.SRC_IN));
        }

        void setOpen(boolean value) {
            if (open != value) {
                open = value;
                invalidate();
            }
        }

        @Override protected void onDraw(Canvas canvas) {
            Bitmap art = LockArt.get(open);
            if (art == null) return;
            float scale = Math.min((float) getWidth() / art.getWidth(),
                    (float) getHeight() / art.getHeight());
            float w = art.getWidth() * scale;
            float h = art.getHeight() * scale;
            dst.set((getWidth() - w) / 2f, (getHeight() - h) / 2f,
                    (getWidth() + w) / 2f, (getHeight() + h) / 2f);
            src.set(0, 0, art.getWidth(), art.getHeight());
            canvas.drawBitmap(art, src, dst, paint);
        }
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static final class State {
        float downX;
        float downY;
        int video;
        int choice;
        boolean locked;
        boolean wasPinned;
        boolean handled;
        boolean lockingGesture;
        boolean sawSpeedup;
        boolean hintShown;
        Runnable watcher;
        Pill pill;
        int pillToken;

        void resetForVideo() {
            video = 0;
            locked = false;choice=0;
            handled = false;
            lockingGesture = false;
            sawSpeedup = false;
            hintShown = false;
            watcher = null;
        }
    }
}
