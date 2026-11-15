package cat.ttcuz.framework;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The phone's own lock (fingerprint, face, or its PIN) in front of TikTok, or in front of the chats only.
 *
 * Unlocked once, TikTok stays open for the visit: it locks again when the screen goes off or TikTok has been away
 * more than {@link #GRACE} (long enough to pick a photo in another app and come back). Until then a screen that is
 * locked is covered by an opaque window of the mod's own, over everything TikTok shows, sheets included.
 *
 * It never keeps anyone out of their own app: on a phone without a screen lock, or an Android that cannot ask
 * (below 9), or when asking fails, it opens.
 */
public final class AppLock {
    private AppLock() {}

    static final String APP = "lock_app", CHATS = "lock_chats";
    static final long GRACE = 30_000L;
    private static final int WHOLE = 0, MESSAGES = 1;

    private static final Handler main = new Handler(Looper.getMainLooper());
    private static volatile Boolean app, chats;

    // everything below: main thread only
    private static boolean open;
    private static long leftAt;
    private static boolean listening;
    private static final Map<Activity, Cover> covers = new WeakHashMap<>();
    /** The screen on top now (null while paused), and the last one that was (for the screen going off). */
    private static WeakReference<Activity> top = new WeakReference<>(null), last = new WeakReference<>(null);
    /** The prompt on screen: its number (0 none) and the screen it was asked for. */
    private static int asking, generation, retries;
    private static WeakReference<Activity> askingFor = new WeakReference<>(null);
    private static CancellationSignal signal;
    private static final List<Runnable> waiting = new ArrayList<>();
    private static Object focus;

    private static SharedPreferences prefs() {
        Context context = Ttcuz.context();
        return context == null ? null : context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
    }

    static boolean app() {
        Boolean known = app;
        if (known != null) return known;
        SharedPreferences p = prefs();
        boolean value = p != null && p.getBoolean(APP, false);
        app = value;
        return value;
    }

    static boolean chats() {
        Boolean known = chats;
        if (known != null) return known;
        SharedPreferences p = prefs();
        boolean value = p != null && p.getBoolean(CHATS, false);
        chats = value;
        return value;
    }

    // ------------------------------------------------- a code of four digits instead of the phone's lock

    static final String METHOD = "lock_method", PIN_SALT = "lock_pin_salt", PIN_HASH = "lock_pin_hash";
    static final int SYSTEM = 0, CODE = 1;
    static final String[] METHODS = {"Отпечаток или блокировка телефона", "Код из 4 цифр"};
    private static final int TRIES = 5;
    private static final long PAUSE = 30_000L;
    private static int wrongTries;
    private static long pausedUntil;

    static int method() {
        SharedPreferences p = prefs();
        return p != null && p.getInt(METHOD, SYSTEM) == CODE ? CODE : SYSTEM;
    }

    static boolean hasPin() {
        SharedPreferences p = prefs();
        return p != null && p.getString(PIN_HASH, null) != null && p.getString(PIN_SALT, null) != null;
    }

    /** The code is what unlocks: chosen, and one is set. */
    static boolean usesPin() { return method() == CODE && hasPin(); }

    static void setMethod(int value) {
        SharedPreferences p = prefs();
        if (p != null) p.edit().putInt(METHOD, value == CODE ? CODE : SYSTEM).apply();
    }

    /** Only a salted SHA-256 of the code is kept, never the digits. */
    static void setPin(String pin) {
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        SharedPreferences p = prefs();
        if (p == null) return;
        p.edit().putString(PIN_SALT, android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP))
                .putString(PIN_HASH, hash(salt, pin)).putInt(METHOD, CODE).apply();
        wrongTries = 0;
        pausedUntil = 0;
    }

    static void clearPin() {
        SharedPreferences p = prefs();
        if (p != null) p.edit().remove(PIN_SALT).remove(PIN_HASH).putInt(METHOD, SYSTEM).apply();
    }

    private static String hash(byte[] salt, String pin) {
        try {
            java.security.MessageDigest sha = java.security.MessageDigest.getInstance("SHA-256");
            sha.update(salt);
            sha.update(pin.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return android.util.Base64.encodeToString(sha.digest(), android.util.Base64.NO_WRAP);
        } catch (Exception error) { throw new IllegalStateException(error); }
    }

    /** The code is right; compared in constant time. */
    static boolean pinMatches(String pin) {
        SharedPreferences p = prefs();
        if (p == null || pin == null) return false;
        String salt = p.getString(PIN_SALT, null), kept = p.getString(PIN_HASH, null);
        if (salt == null || kept == null) return false;
        byte[] want = android.util.Base64.decode(kept, android.util.Base64.NO_WRAP);
        byte[] got = android.util.Base64.decode(hash(android.util.Base64.decode(salt, android.util.Base64.NO_WRAP), pin), android.util.Base64.NO_WRAP);
        return java.security.MessageDigest.isEqual(want, got);
    }

    /**
     * Setting the code: a full-screen keypad asks for four digits, then for the same four again; on a match the code
     * is kept (as a hash) and becomes the way in, and `done` runs. Back leaves it as it was.
     */
    static void choosePin(final Activity activity, final Runnable done) {
        final Skin skin = Skin.remembered(activity);
        final Dialog dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar);
        final String[] first = {null};
        final PinPad[] pad = new PinPad[1];
        pad[0] = new PinPad(activity, skin, Text.t("Придумай код из 4 цифр"), pin -> {
            if (first[0] == null) {
                first[0] = pin;
                main.postDelayed(() -> { pad[0].clear(); pad[0].heading(Text.t("Повтори код")); pad[0].note(""); }, 260L);
                return true;
            }
            if (first[0].equals(pin)) {
                setPin(pin);
                main.postDelayed(() -> { try { dialog.dismiss(); } catch (Throwable ignored) {} Screen.say(Text.t("Код сохранён")); if (done != null) done.run(); }, 320L);
                return true;
            }
            first[0] = null;
            main.postDelayed(() -> { pad[0].heading(Text.t("Придумай код из 4 цифр")); pad[0].note(Text.t("Коды не совпали, ещё раз")); }, 420L);
            return false;
        });
        FrameLayout root = new FrameLayout(activity);
        root.setBackgroundColor(skin.page | 0xFF000000);
        root.addView(pad[0], new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(skin.page | 0xFF000000));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
    }

    /** Android 9+ with a screen lock set: without one there is nothing to ask for. */
    static boolean possible(Context context) {
        if (Build.VERSION.SDK_INT < 28 || context == null) return false;
        try {
            KeyguardManager keyguard = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            return keyguard != null && keyguard.isDeviceSecure();
        } catch (Throwable error) { return false; }
    }

    /** Turns one of the two locks on or off; false (and a word why) when it cannot be turned on. */
    static boolean set(Context context, String key, boolean value) {
        if (value && !usesPin() && !possible(context)) {
            Screen.say(Build.VERSION.SDK_INT < 28 ? Text.t("Нужен Android 9 или новее")
                    : Text.t("Поставь блокировку экрана в телефоне или выбери вход по коду"));
            return false;
        }
        if (APP.equals(key)) app = value; else chats = value;
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean(key, value).apply();
        // the person turning it on is the owner, on screen now: it locks from the next visit
        if (value) open = true;
        Context application = context.getApplicationContext();
        if (value && application instanceof Application) listen((Application) application);
        return true;
    }

    static void start(Application application) {
        if (app() || chats()) listen(application);
    }

    /** The screen going off locks at once. */
    private static void listen(Application application) {
        if (listening) return;
        listening = true;
        try {
            BroadcastReceiver off = new BroadcastReceiver() {
                @Override public void onReceive(Context context, Intent intent) { lock(); }
            };
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            if (Build.VERSION.SDK_INT >= 33) application.registerReceiver(off, filter, Context.RECEIVER_NOT_EXPORTED);
            else application.registerReceiver(off, filter);
        } catch (Throwable error) { Diary.note("lock screen-off: " + error); }
    }

    private static final Runnable away = () -> { if (!Visits.visible()) lock(); };

    /** TikTok left the screen: it locks once it has been away long enough. */
    static void left() {
        leftAt = SystemClock.elapsedRealtime();
        main.removeCallbacks(away);
        if (app() || chats()) main.postDelayed(away, GRACE);
    }

    /** TikTok is back. (A frozen process misses the timer: the time away is counted here as well.) */
    static void came() {
        main.removeCallbacks(away);
        if (leftAt > 0 && SystemClock.elapsedRealtime() - leftAt > GRACE) lock();
        leftAt = 0;
    }

    private static void lock() {
        if (!app() && !chats()) return;
        open = false;
        stopAsking();
        // what is on screen is covered now, so it is not what shows when the screen comes back on
        Activity shown = last.get();
        if (app() && shown != null && !shown.isFinishing()) cover(shown, WHOLE, null);
    }

    /** Something to do once TikTok is unlocked (now, when it is not locked). */
    static void afterUnlock(Runnable action) {
        if (open || !app()) action.run();
        else waiting.add(action);
    }

    static void resumed(Activity activity) {
        top = new WeakReference<>(activity);
        last = top;
        retries = 0;
        boolean whole = app();
        // no picture of a locked TikTok in the recent apps
        if (Build.VERSION.SDK_INT >= 33) try { activity.setRecentsScreenshotEnabled(!whole); } catch (Throwable ignored) {}
        if (open || !whole) return;
        cover(activity, WHOLE, null);
        // the splash hands over to the main screen in a moment: the question waits for that one
        if (!activity.getClass().getName().endsWith(".SplashActivity")) askSoon(activity);
    }

    static void paused(Activity activity) {
        if (top.get() == activity) top = new WeakReference<>(null);
    }

    static void destroyed(Activity activity) {
        Cover cover = covers.remove(activity);
        if (cover != null) cover.dismiss();
    }

    // ------------------------------------------------- the chats (ported hooks on TikTok's fragments)

    /** BaseChatRootFragment.onResume: a conversation is on screen. */
    public static void chat(Object fragment) {
        try {
            if (open || !chats()) return;
            Activity activity = activityOf(fragment);
            if (activity == null) return;
            cover(activity, MESSAGES, fragment);
            askSoon(activity);
        } catch (Throwable error) { Diary.note("lock chat: " + error); }
    }

    /** InboxFragmentV2.onResume: every tab resumes with the main screen, so only an inbox actually showing counts. */
    public static void inbox(final Object fragment) {
        try {
            if (open || !chats()) return;
            final Object view = NativeRead.get(fragment, "getView");
            if (!(view instanceof View)) return;
            if (showing((View) view)) { inboxShown(fragment); return; }
            ((View) view).post(() -> { if (showing((View) view)) inboxShown(fragment); });
        } catch (Throwable error) { Diary.note("lock inbox: " + error); }
    }

    /** InboxFragmentV2.onHiddenChanged: the inbox tab was picked (false) or left (true). */
    public static void inboxHidden(Object fragment, boolean hidden) {
        if (hidden) gone(fragment);
        else try { if (!open && chats()) inboxShown(fragment); } catch (Throwable error) { Diary.note("lock inbox: " + error); }
    }

    /** The chat or the inbox paused (closed, another tab, TikTok away): its cover goes with it, and comes back on resume. */
    public static void gone(Object fragment) {
        try {
            for (Map.Entry<Activity, Cover> entry : new ArrayList<>(covers.entrySet()))
                if (entry.getValue().fragment.get() == fragment) {
                    covers.remove(entry.getKey());
                    entry.getValue().dismiss();
                }
        } catch (Throwable error) { Diary.note("lock gone: " + error); }
    }

    private static void inboxShown(Object fragment) {
        Activity activity = activityOf(fragment);
        if (activity == null) return;
        cover(activity, MESSAGES, fragment);
        askSoon(activity);
    }

    private static boolean showing(View view) {
        android.graphics.Rect seen = new android.graphics.Rect();
        return view.isShown() && view.getGlobalVisibleRect(seen) && seen.width() > 0 && seen.height() > 0;
    }

    private static Activity activityOf(Object fragment) {
        Object activity = NativeRead.get(fragment, "getActivity");
        return activity instanceof Activity && !((Activity) activity).isFinishing() ? (Activity) activity : null;
    }

    // ------------------------------------------------- asking

    private static void askSoon(final Activity activity) {
        main.postDelayed(() -> ask(activity), 250L);
    }

    /** The phone's lock asked to forget the code: success clears it instead of only opening. */
    private static boolean forgetting;

    private static void ask(final Activity activity) {
        if (open || activity.isFinishing() || top.get() != activity || !covers.containsKey(activity)) return;
        if (usesPin() && !forgetting) return;
        if (asking != 0) {
            if (askingFor.get() == activity) return;
            // a prompt left over from the screen before (the splash): this one asks instead
            stopAsking();
            main.postDelayed(() -> ask(activity), 300L);
            return;
        }
        if (!possible(activity)) {
            if (forgetting) { forgetting = false; Screen.say(Text.t("На телефоне нет блокировки: код можно сбросить только очисткой данных TikTok")); return; }
            Diary.note("lock: no screen lock, opening"); unlock(); return;
        }
        final int id = ++generation;
        asking = id;
        askingFor = new WeakReference<>(activity);
        signal = new CancellationSignal();
        java.util.concurrent.Executor here = main::post;
        Cover cover = covers.get(activity);
        BiometricPrompt.Builder builder = new BiometricPrompt.Builder(activity)
                .setTitle(cover != null && cover.kind == MESSAGES ? Text.t("Переписки") : "TikTok");
        if (Build.VERSION.SDK_INT >= 30) {
            builder.setAllowedAuthenticators(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK
                    | android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        } else if (Build.VERSION.SDK_INT >= 29) {
            builder.setDeviceCredentialAllowed(true);
        } else {
            builder.setNegativeButton(Text.t("Отмена"), here, (dialog, which) -> { if (asking == id) { asking = 0; refused(activity); } });
        }
        try {
            builder.build().authenticate(signal, here, new BiometricPrompt.AuthenticationCallback() {
                @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    if (asking != id) return;
                    asking = 0;
                    if (forgetting) {
                        forgetting = false;
                        clearPin();
                        Screen.say(Text.t("Код сброшен: вход снова по блокировке телефона"));
                    }
                    unlock(true);
                }

                @Override public void onAuthenticationError(int code, CharSequence message) {
                    if (asking != id) return;
                    asking = 0;
                    if (forgetting) { forgetting = false; return; }
                    answered(activity, code, message);
                }
            });
        } catch (Throwable error) {
            // never a TikTok nobody can open
            asking = 0;
            if (forgetting) { forgetting = false; Diary.note("lock forget: " + error); return; }
            Diary.note("lock ask: " + error);
            unlock();
        }
    }

    private static void answered(Activity activity, int code, CharSequence message) {
        switch (code) {
            case BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED:
            case 13: // BIOMETRIC_ERROR_NEGATIVE_BUTTON (Android 9 has it under no name)
                refused(activity);
                return;
            case BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE:
            case BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT:
            case BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS:
            case 14: // BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL
            case 15: // BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED
                // nothing on this phone can answer: the lock opens rather than keep the person out
                Diary.note("lock: cannot ask (" + code + "), opening");
                unlock();
                return;
            case BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT:
            case BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT_PERMANENT:
                if (message != null) Screen.say(message.toString());
                return;
            case BiometricPrompt.BIOMETRIC_ERROR_CANCELED:
                // Android took it away (the screen changed under it): asked again when a covered screen is on top
                Activity now = top.get();
                if (now != null && covers.containsKey(now) && retries++ < 2) askSoon(now);
                return;
            default:
                // a timeout and the like: the cover's button asks again
        }
    }

    /** The person closed the question: TikTok goes to the background, or the chat closes. */
    private static void refused(Activity activity) {
        Cover cover = covers.get(activity);
        try {
            if (cover == null || cover.kind == WHOLE) activity.moveTaskToBack(true);
            else activity.onBackPressed();
        } catch (Throwable error) { Diary.note("lock back: " + error); }
    }

    private static void stopAsking() {
        CancellationSignal s = signal;
        asking = 0;
        signal = null;
        if (s != null) try { s.cancel(); } catch (Throwable ignored) {}
    }

    private static void unlock() { unlock(false); }

    /** Opens everything; byFinger when the fingerprint (or the phone's own lock) did it. */
    private static void unlock(boolean byFinger) {
        open = true;
        if (!covers.isEmpty()) UiSound.unlocked();
        for (Cover cover : new ArrayList<>(covers.values())) cover.leave(byFinger);
        covers.clear();
        quiet(false);
        still(false);
        List<Runnable> ready = new ArrayList<>(waiting);
        waiting.clear();
        for (Runnable action : ready) try { action.run(); } catch (Throwable error) { Diary.note("after unlock: " + error); }
    }

    // ------------------------------------------------- the cover

    private static void cover(Activity activity, int kind, Object fragment) {
        Cover had = covers.get(activity);
        if (had != null && had.showing()) {
            // the whole of TikTok outranks the chats
            if (kind == WHOLE && had.kind != WHOLE) { had.kind = WHOLE; had.fragment = new WeakReference<>(null); had.words(); }
            else if (kind == MESSAGES && had.kind == MESSAGES) had.fragment = new WeakReference<>(fragment);
            return;
        }
        try {
            Cover cover = new Cover(activity, kind, fragment);
            covers.put(activity, cover);
            cover.show();
            if (kind == WHOLE) { quiet(true); still(true); }
        } catch (Throwable error) {
            covers.remove(activity);
            Diary.note("lock cover: " + error);
        }
    }

    /** While the whole of TikTok is locked, the video behind the cover is asked to be quiet (audio focus). */
    /** Whether all of TikTok is behind the lock now. */
    static boolean covering() {
        if (open) return false;
        for (Cover cover : covers.values()) if (cover.kind == WHOLE && cover.showing()) return true;
        return false;
    }

    /** The lock paused the video in front, and gives it back on opening. */
    private static boolean stilled;

    /**
     * Nothing plays behind the lock: TikTok's own player manager (X/0qWW in 47.7.3, got by LJIIL(): tryPausePlay is
     * LJLJI(Z), resumePlay LJIILLIIL()) is paused while the cover is up -- also when the feed starts a video later, see
     * PlayerTools.resumed -- and played again on opening, only if the lock was what paused it.
     */
    static void still(boolean on) {
        try {
            Class<?> type = Class.forName("X.0qWW");
            // its own getter (LJIIL), which makes it when nothing has yet: its static field is empty that early
            Object manager = type.getMethod("LJIIL").invoke(null);
            if (manager == null) { Diary.note("lock still: no player"); return; }
            if (on) { type.getMethod("LJLJI", boolean.class).invoke(manager, false); stilled = true; }
            else if (stilled) { stilled = false; type.getMethod("LJIILLIIL").invoke(manager); }
        } catch (Throwable error) { Diary.note("lock still: " + error); }
    }

    private static void quiet(boolean on) {
        try {
            Context context = Ttcuz.context();
            if (context == null || Build.VERSION.SDK_INT < 26) return;
            android.media.AudioManager audio = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (on && focus == null) {
                android.media.AudioFocusRequest request = new android.media.AudioFocusRequest.Builder(
                        android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).build();
                audio.requestAudioFocus(request);
                focus = request;
            } else if (!on && focus != null) {
                audio.abandonAudioFocusRequest((android.media.AudioFocusRequest) focus);
                focus = null;
            }
        } catch (Throwable error) { Diary.note("lock quiet: " + error); }
    }

    /** An opaque full-screen window over the screen, with a button that asks again. */
    private static final class Cover {
        final Activity activity;
        final Dialog dialog;
        int kind;
        WeakReference<Object> fragment;
        private TextView title;
        private final TextView back;
        /** The keypad, when the lock is a code. */
        private PinPad pad;
        private FrameLayout root;

        Cover(final Activity activity, int kind, Object fragment) {
            this.activity = activity;
            this.kind = kind;
            this.fragment = new WeakReference<>(fragment);
            Skin skin = Skin.remembered(activity);
            float density = activity.getResources().getDisplayMetrics().density;

            LinearLayout column = new LinearLayout(activity);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.CENTER);

            if (usesPin()) {
                // the code: four dots and the keypad stand where the lock and its button would
                pad = new PinPad(activity, skin, "", this::entered);
                column.addView(pad, new LinearLayout.LayoutParams(-2, -2));
                if (possible(activity)) {
                    TextView forgot = new TextView(activity);
                    forgot.setText(Text.t("Забыл код"));
                    forgot.setTextColor(Accent.colour());
                    forgot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                    forgot.setGravity(Gravity.CENTER);
                    forgot.setPadding(Math.round(24 * density), Math.round(10 * density), Math.round(24 * density), Math.round(10 * density));
                    forgot.setOnClickListener(v -> { forgetting = true; retries = 0; ask(activity); });
                    LinearLayout.LayoutParams forgotAt = new LinearLayout.LayoutParams(-2, -2);
                    forgotAt.topMargin = Math.round(8 * density);
                    column.addView(forgot, forgotAt);
                }
                if (SystemClock.elapsedRealtime() < pausedUntil) countdown();
            } else {
                ImageView lock = new ImageView(activity);
                lock.setImageDrawable(Symbols.drawable("lock", skin.text));
                int size = Math.round(48 * density);
                column.addView(lock, new LinearLayout.LayoutParams(size, size));

                title = new TextView(activity);
                title.setTextColor(skin.text);
                title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                title.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams titleAt = new LinearLayout.LayoutParams(-2, -2);
                titleAt.topMargin = Math.round(16 * density);
                column.addView(title, titleAt);

                TextView unlock = new TextView(activity);
                unlock.setText(Text.t("Разблокировать"));
                unlock.setTextColor(0xFFFFFFFF);
                unlock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                unlock.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                unlock.setGravity(Gravity.CENTER);
                unlock.setPadding(Math.round(24 * density), Math.round(12 * density), Math.round(24 * density), Math.round(12 * density));
                GradientDrawable pill = new GradientDrawable();
                pill.setColor(Accent.colour());
                pill.setCornerRadius(24 * density);
                unlock.setBackground(pill);
                unlock.setOnClickListener(v -> { retries = 0; ask(activity); });
                LinearLayout.LayoutParams unlockAt = new LinearLayout.LayoutParams(-2, -2);
                unlockAt.topMargin = Math.round(28 * density);
                column.addView(unlock, unlockAt);
            }

            back = new TextView(activity);
            back.setText(Text.t("Назад"));
            back.setTextColor(skin.muted());
            back.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            back.setGravity(Gravity.CENTER);
            back.setPadding(Math.round(24 * density), Math.round(12 * density), Math.round(24 * density), Math.round(12 * density));
            back.setOnClickListener(v -> refused(activity));
            LinearLayout.LayoutParams backAt = new LinearLayout.LayoutParams(-2, -2);
            backAt.topMargin = Math.round(4 * density);
            column.addView(back, backAt);

            root = new FrameLayout(activity);
            root.setBackgroundColor(skin.page | 0xFF000000);
            // every touch stays on the cover
            root.setClickable(true);
            root.addView(column, new FrameLayout.LayoutParams(-1, -1));
            // whose lock this is: a small lock and the mod's name at the top
            LinearLayout brand = new LinearLayout(activity);
            brand.setGravity(Gravity.CENTER_VERTICAL);
            ImageView brandLock = new ImageView(activity);
            brandLock.setImageDrawable(Symbols.drawable("lock_fill1", Accent.colour()));
            int brandSize = Math.round(18 * density);
            brand.addView(brandLock, new LinearLayout.LayoutParams(brandSize, brandSize));
            TextView name = new TextView(activity);
            name.setText("ttcuz");
            name.setTextColor(Accent.colour());
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            name.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            LinearLayout.LayoutParams nameAt = new LinearLayout.LayoutParams(-2, -2);
            nameAt.leftMargin = Math.round(6 * density);
            brand.addView(name, nameAt);
            FrameLayout.LayoutParams brandAt = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            brandAt.topMargin = Math.round(64 * density);
            root.addView(brand, brandAt);

            dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar);
            dialog.setContentView(root);
            dialog.setCancelable(false);
            dialog.setOnKeyListener((d, code, event) -> {
                if (code != KeyEvent.KEYCODE_BACK) return false;
                if (event.getAction() == KeyEvent.ACTION_UP) refused(activity);
                return true;
            });
            if (Build.VERSION.SDK_INT >= 33) {
                // predictive back, for a TikTok that opted in to it: the key above never arrives then
                dialog.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                        android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> refused(activity));
            }
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(skin.page | 0xFF000000));
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                window.setWindowAnimations(0);
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                // a chat's message field keeps its keyboard open: it stayed over the cover and hid the keypad's lower
                // keys, so the code could not be typed. The cover hides it and never lets it up.
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                if (Build.VERSION.SDK_INT >= 28) window.getAttributes().layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
            words();
        }

        void words() {
            String heading = kind == WHOLE ? Text.t("TikTok заблокирован") : Text.t("Переписки скрыты");
            if (pad != null) pad.heading(usesPin() ? Text.t("Введи код") : heading);
            if (title != null) title.setText(heading);
            back.setText(kind == WHOLE ? Text.t("Выйти") : Text.t("Назад"));
        }

        /** Four digits typed on the cover: right opens everything (after the dots turn to the accent), wrong counts. */
        private boolean entered(String pin) {
            if (SystemClock.elapsedRealtime() < pausedUntil) return false;
            if (pinMatches(pin)) {
                wrongTries = 0;
                main.postDelayed(AppLock::unlock, 240L);
                return true;
            }
            if (++wrongTries >= TRIES) {
                wrongTries = 0;
                pausedUntil = SystemClock.elapsedRealtime() + PAUSE;
                countdown();
            } else {
                pad.note(Text.t("Неверный код") + " · " + (TRIES - wrongTries));
            }
            return false;
        }

        /** Too many wrong codes: the keypad rests, and says for how long, each second. */
        private void countdown() {
            if (pad == null) return;
            long left = pausedUntil - SystemClock.elapsedRealtime();
            if (left <= 0) { pad.lock(false); pad.note(""); return; }
            pad.lock(true);
            pad.note(Text.t("Слишком много попыток. Подожди") + " " + ((left + 999) / 1000) + " " + Text.t("с"));
            main.postDelayed(this::countdown, Math.min(1000L, left));
        }

        void show() {
            try {
                View focused = activity.getCurrentFocus();
                android.view.inputmethod.InputMethodManager keys = (android.view.inputmethod.InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (keys != null) keys.hideSoftInputFromWindow((focused != null ? focused : activity.getWindow().getDecorView()).getWindowToken(), 0);
                if (focused != null) focused.clearFocus();
            } catch (Throwable error) { Diary.note("lock keyboard: " + error); }
            dialog.show();
        }

        /**
         * Unlocked: a welcome, not just a fade. The lock's content steps back (smaller, fading); a tick in an accent
         * circle springs up in the middle with "С возвращением" and the account's @handle; a soft ring of the accent
         * spreads from there; then the cover dissolves into TikTok. About 0.9 s, three or four views moving on the GPU.
         */
        void leave(boolean byFinger) {
            if (root == null || !showing() || !Motion.enabled(activity)) { dismiss(); return; }
            float d = activity.getResources().getDisplayMetrics().density;
            Skin skin = Skin.remembered(activity);
            int accent = Accent.colour();
            // the window's own background goes: the cover's root (opaque) is what fades at the end
            Window window = dialog.getWindow();
            if (window != null) window.setBackgroundDrawable(new ColorDrawable(0));
            if (Motion.haptics()) root.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
            // the tick springs past its size: nothing between it and the screen may cut it
            root.setClipChildren(false);
            for (int i = 0; i < root.getChildCount(); i++) root.getChildAt(i).animate().scaleX(.88f).scaleY(.88f).alpha(0f)
                    .setDuration(240L).setInterpolator(Motion.IN).withLayer().start();
            // the fingerprint's own beat first: it fills with the accent from below, then turns into the tick
            long late = byFinger ? 420L : 0L;

            // the ring: an accent circle as large as the screen's diagonal, spreading from nothing
            float diagonal = (float) Math.hypot(root.getWidth(), root.getHeight());
            View ring = new View(activity);
            GradientDrawable round = new GradientDrawable();
            round.setShape(GradientDrawable.OVAL);
            round.setColor((accent & 0xFFFFFF) | 0x33000000);
            ring.setBackground(round);
            int ringSize = Math.round(diagonal);
            root.addView(ring, new FrameLayout.LayoutParams(ringSize, ringSize, Gravity.CENTER));
            ring.setScaleX(0f); ring.setScaleY(0f);
            ring.animate().scaleX(1f).scaleY(1f).setStartDelay(260L + late).setDuration(560L).setInterpolator(Motion.EMPHASIZED).withLayer().start();

            // the welcome: a tick in an accent circle, the words, the handle
            LinearLayout hello = new LinearLayout(activity);
            hello.setOrientation(LinearLayout.VERTICAL);
            hello.setGravity(Gravity.CENTER);
            hello.setClipChildren(false);
            hello.setClipToPadding(false);
            FrameLayout badge = new FrameLayout(activity);
            badge.setClipChildren(false);
            GradientDrawable disc = new GradientDrawable();
            disc.setShape(GradientDrawable.OVAL);
            disc.setColor(accent);
            badge.setBackground(disc);
            ImageView tick = new ImageView(activity);
            tick.setImageDrawable(Symbols.drawable("check_circle_fill1", 0xFFFFFFFF));
            int badgeSize = Math.round(76 * d), tickSize = Math.round(44 * d);
            badge.addView(tick, new FrameLayout.LayoutParams(tickSize, tickSize, Gravity.CENTER));
            hello.addView(badge, new LinearLayout.LayoutParams(badgeSize, badgeSize));
            TextView words = new TextView(activity);
            words.setText(kind == WHOLE ? Text.t("С возвращением") : Text.t("Переписки открыты"));
            words.setTextColor(skin.text);
            words.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
            words.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            words.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams wordsAt = new LinearLayout.LayoutParams(-2, -2);
            wordsAt.topMargin = Math.round(22 * d);
            hello.addView(words, wordsAt);
            String handle = Account.profileHandle();
            if (handle != null && !handle.isEmpty()) {
                TextView who = new TextView(activity);
                who.setText("@" + handle);
                who.setTextColor(skin.muted());
                who.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                who.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams whoAt = new LinearLayout.LayoutParams(-2, -2);
                whoAt.topMargin = Math.round(6 * d);
                hello.addView(who, whoAt);
            }
            root.addView(hello, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
            if (byFinger) fingerprint(badge, disc, tick, accent, d);
            else {
                badge.setScaleX(.4f); badge.setScaleY(.4f); badge.setAlpha(0f);
                // no layer: a layer is the view's own size, and the overshoot past it was cut off
                badge.animate().scaleX(1f).scaleY(1f).alpha(1f).setStartDelay(150L).setDuration(420L)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(2.4f)).start();
                tick.setRotation(-90f);
                tick.animate().rotation(0f).setStartDelay(190L).setDuration(420L).setInterpolator(Motion.EMPHASIZED).start();
            }
            for (int i = 1; i < hello.getChildCount(); i++) {
                View line = hello.getChildAt(i);
                line.setAlpha(0f);
                line.setTranslationY(14 * d);
                line.animate().alpha(1f).translationY(0f).setStartDelay(260L + late + 70L * i).setDuration(360L).setInterpolator(Motion.EMPHASIZED).start();
            }
            // and the whole cover dissolves into TikTok, a little larger, as a lock screen lifts
            root.animate().alpha(0f).scaleX(1.04f).scaleY(1.04f).setStartDelay((kind == WHOLE ? 760L : 560L) + late).setDuration(260L)
                    .setInterpolator(Motion.IN).withLayer().withEndAction(this::dismiss).start();
        }

        /**
         * The fingerprint's welcome: the print appears faint in a quiet accent circle and fills with the accent from
         * below, as a scan; then the circle floods with the accent, the print shrinks away and the tick turns in.
         */
        private void fingerprint(FrameLayout badge, final GradientDrawable disc, ImageView tick, final int accent, float d) {
            int printSize = Math.round(44 * d);
            ImageView faint = new ImageView(activity);
            faint.setImageDrawable(Symbols.drawable("fingerprint", (accent & 0xFFFFFF) | 0x55000000));
            final android.graphics.drawable.ClipDrawable fill = new android.graphics.drawable.ClipDrawable(
                    Symbols.drawable("fingerprint", accent), Gravity.BOTTOM, android.graphics.drawable.ClipDrawable.VERTICAL);
            fill.setLevel(0);
            ImageView filled = new ImageView(activity);
            filled.setImageDrawable(fill);
            badge.addView(faint, 0, new FrameLayout.LayoutParams(printSize, printSize, Gravity.CENTER));
            badge.addView(filled, 1, new FrameLayout.LayoutParams(printSize, printSize, Gravity.CENTER));
            final int quiet = (accent & 0xFFFFFF) | 0x2E000000;
            disc.setColor(quiet);
            badge.setScaleX(.7f); badge.setScaleY(.7f); badge.setAlpha(0f);
            badge.animate().scaleX(1f).scaleY(1f).alpha(1f).setStartDelay(60L).setDuration(260L).setInterpolator(Motion.OUT).start();
            // the scan
            android.animation.ValueAnimator scan = android.animation.ValueAnimator.ofInt(0, 10000);
            scan.setStartDelay(160L);
            scan.setDuration(360L);
            scan.setInterpolator(Motion.BOTH);
            scan.addUpdateListener(a -> fill.setLevel((Integer) a.getAnimatedValue()));
            scan.start();
            // the flood: the circle turns to the accent with a little pulse
            android.animation.ValueAnimator flood = android.animation.ValueAnimator.ofObject(new android.animation.ArgbEvaluator(), quiet, accent);
            flood.setStartDelay(520L);
            flood.setDuration(220L);
            flood.addUpdateListener(a -> disc.setColor((Integer) a.getAnimatedValue()));
            flood.start();
            badge.animate().scaleX(1.12f).scaleY(1.12f).setStartDelay(520L).setDuration(160L).setInterpolator(Motion.OUT)
                    .withEndAction(() -> badge.animate().scaleX(1f).scaleY(1f).setDuration(260L)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(2f)).start()).start();
            faint.animate().alpha(0f).scaleX(.5f).scaleY(.5f).setStartDelay(520L).setDuration(200L).setInterpolator(Motion.IN).start();
            filled.animate().alpha(0f).scaleX(.5f).scaleY(.5f).setStartDelay(520L).setDuration(200L).setInterpolator(Motion.IN).start();
            tick.setAlpha(0f); tick.setScaleX(.3f); tick.setScaleY(.3f); tick.setRotation(-90f);
            tick.animate().alpha(1f).scaleX(1f).scaleY(1f).rotation(0f).setStartDelay(600L).setDuration(420L)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(2.2f)).start();
        }

        boolean showing() { return dialog.isShowing(); }

        void dismiss() {
            try { if (dialog.isShowing()) dialog.dismiss(); } catch (Throwable ignored) {}
        }
    }
}
