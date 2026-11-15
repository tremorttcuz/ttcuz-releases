package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import cat.ttcuz.framework.CommentBackground;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CommentBackground {
    public static final String BLUR = "comment_background_blur";
    public static final String DIM = "comment_background_dim";
    private static final String FILE = "comment_background_file";
    private static final String GIF = "comment_background_gif";
    /** The picture is a still image (shown the way a GIF is, as one picture). */
    private static final String STILL = "comment_background_still";
    public static final String ON = "comment_background_on";
    private static final String SCALE = "comment_background_scale_v2";
    public static final String SECTION = "Фон комментариев";
    private static final int TAG = 1414808386;
    private static final ArrayList<WeakReference<Binding>> panels = new ArrayList<>();

    private CommentBackground() {
    }

    private static SharedPreferences prefs() {
        return Ttcuz.context().getSharedPreferences("ttcuz", 0);
    }

    /** The background is gone from the settings: one chosen before stays off. */
    public static boolean enabled() {
        return false;
    }

    public static int blur() {
        return number(BLUR, 0, 32);
    }

    public static int dim() {
        return number(DIM, 45, 90);
    }

    public static int scale() {
        return number(SCALE, 2, 2);
    }

    public static String[] scaleOptions() {
        return new String[]{Text.t("Заполнить с обрезкой"), Text.t("Показать целиком"), Text.t("Растянуть на всё окно")};
    }

    public static String scaleName() {
        return scaleOptions()[scale()];
    }

    public static void setScale(int i) {
        prefs().edit().putInt(SCALE, Math.max(0, Math.min(2, i))).apply();
        refresh();
    }

    private static int number(String str, int i, int i2) {
        try {
            return Math.max(0, Math.min(i2, prefs().getInt(str, i)));
        } catch (RuntimeException unused) {
            return i;
        }
    }

    static File file() {
        try {
            String string = prefs().getString(FILE, "");
            if (!string.matches("comments-[a-f0-9-]+\\.media")) {
                return null;
            }
            File file = new File(Ttcuz.context().getFilesDir(), "ttcuz/" + string);
            if (file.isFile()) {
                return file;
            }
            return null;
        } catch (RuntimeException unused) {
            return null;
        }
    }

    static boolean gif() {
        try {
            return prefs().getBoolean(GIF, false);
        } catch (RuntimeException unused) {
            return false;
        }
    }

    public static boolean hasMedia() {
        return file() != null;
    }

    static void preload() {
        if (!enabled() || gif()) {
            return;
        }
        CommentPoster.warm(file(), null);
    }

    public static String description() {
        boolean still = prefs().getBoolean(STILL, false);
        return hasMedia() ? still ? Text.t("Картинка выбрана") : gif() ? Text.t("GIF выбран") : Text.t("Видео выбрано") : Text.t("Выберите картинку, GIF или видео");
    }

    public static void setEnabled(boolean z) {
        prefs().edit().putBoolean(ON, z && hasMedia()).apply();
        refresh();
    }

    public static void setBlur(int i) {
        prefs().edit().putInt(BLUR, Math.max(0, Math.min(32, i))).apply();
        refresh();
    }

    public static void setDim(int i) {
        prefs().edit().putInt(DIM, Math.max(0, Math.min(90, i))).apply();
        refresh();
    }

    public static void clear() {
        File file = file();
        prefs().edit().remove(FILE).remove(GIF).remove(STILL).putBoolean(ON, false).apply();
        refresh();
        if (file != null) {
            file.delete();
            CommentPoster.discard(file);
        }
    }

    /** Imports a picked picture, GIF or video as the comments background; false when it is not a usable one. */
    public static boolean take(Context context, Uri uri) {
        File file = new File(context.getFilesDir(), "ttcuz/comments-" + UUID.randomUUID() + ".media");
        boolean kept = false;
        try {
            File parent = file.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) return false;
            try (InputStream in = context.getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(file)) {
                if (in == null) return false;
                byte[] buffer = new byte[65536];
                long total = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    total += read;
                    if (total > 268435456L) return false;
                    out.write(buffer, 0, read);
                }
            }
            boolean gif = false;
            try (FileInputStream head = new FileInputStream(file)) {
                byte[] magic = new byte[6];
                if (head.read(magic) == 6) {
                    String kind = new String(magic, "US-ASCII");
                    gif = kind.equals("GIF89a") || kind.equals("GIF87a");
                }
            }
            // a still picture (JPEG, PNG, WebP, HEIF): shown the way a GIF is, as one picture
            boolean still = false;
            if (!gif) {
                android.graphics.BitmapFactory.Options look = new android.graphics.BitmapFactory.Options();
                look.inJustDecodeBounds = true;
                android.graphics.BitmapFactory.decodeFile(file.getPath(), look);
                if (look.outWidth > 0 && look.outHeight > 0) {
                    if (Math.max(look.outWidth, look.outHeight) > 12000) return false;
                    still = true;
                }
            }
            if (gif) {
                if (file.length() > 25165824L) return false;
                android.graphics.BitmapFactory.Options bounds = new android.graphics.BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                android.graphics.BitmapFactory.decodeFile(file.getPath(), bounds);
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || (long) bounds.outWidth * bounds.outHeight > 8000000L) return false;
                if (android.os.Build.VERSION.SDK_INT < 28 && (file.length() > 4194304L || Math.max(bounds.outWidth, bounds.outHeight) > 1024)) return false;
            } else if (!still) {
                android.media.MediaMetadataRetriever probe = new android.media.MediaMetadataRetriever();
                try {
                    probe.setDataSource(file.getPath());
                    int width = Integer.parseInt(probe.extractMetadata(18)), height = Integer.parseInt(probe.extractMetadata(19));
                    if (width <= 0 || height <= 0 || Math.max(width, height) > 3840) return false;
                } finally {
                    try { probe.release(); } catch (Exception ignored) { }
                }
            }
            File old = file();
            boolean saved = prefs().edit().putString(FILE, file.getName()).putBoolean(GIF, gif || still).putBoolean(STILL, still).putBoolean(ON, true).commit();
            if (!saved) return false;
            kept = true;
            if (old != null) { old.delete(); CommentPoster.discard(old); }
            if (!gif && !still) CommentPoster.warm(file, null);
            Diary.note("comment background: imported " + (still ? "picture" : gif ? "GIF" : "video") + ", " + file.length() + " bytes");
            refresh();
            return true;
        } catch (Exception error) {
            Diary.note("comment background import: " + error);
            return false;
        } finally {
            if (!kept) file.delete();
        }
    }

    public static void bind(final View view) {
        if (view == null) {
            return;
        }
        // the sheet's own transparency, chosen apart from the background picture
        CommentLook.apply(view);
        if (view.findViewById(2131373645) instanceof FrameLayout) {
            bindNow(view);
        } else {
            view.post(new Runnable() { // from class: cat.ttcuz.framework.CommentBackground$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    CommentBackground.bindNow(view);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void bindNow(View view) {
        try {
            View findViewById = view.findViewById(2131373645);
            if (!(findViewById instanceof FrameLayout)) {
                if (enabled()) {
                    Diary.note("comment background: panel missing in " + view.getClass().getName());
                    return;
                }
                return;
            }
            if (findViewById.getTag(TAG) instanceof Binding) {
                return;
            }
            Binding binding = new Binding(view, (FrameLayout) findViewById);
            findViewById.setTag(TAG, binding);
            panels.add(new WeakReference<>(binding));
            binding.update();
        } catch (RuntimeException e) {
            Diary.note("comment background: " + String.valueOf(e));
        }
    }

    public static void pause(Object obj) {
        lifecycle(obj, 0);
    }

    public static void resume(Object obj) {
        lifecycle(obj, 1);
    }

    public static void destroy(Object obj) {
        // the sheet is gone: nothing of its see-through look may stay on views TikTok reuses for other panels
        try { CommentLook.restore(); } catch (Throwable error) { Diary.note("comment look: " + error); }
        lifecycle(obj, 2);
    }

    private static void lifecycle(Object obj, int i) {
        Object obj2 = NativeRead.get(obj, "getView");
        if (obj2 instanceof View) {
            if (i == 1) {
                bind((View) obj2);
            }
            for (int size = panels.size() - 1; size >= 0; size--) {
                ArrayList<WeakReference<Binding>> arrayList = panels;
                Binding binding = arrayList.get(size).get();
                if (binding == null) {
                    arrayList.remove(size);
                } else if (binding.root == obj2) {
                    if (i == 2) {
                        binding.remove();
                        binding.host.setTag(TAG, null);
                        arrayList.remove(size);
                    } else {
                        binding.paused = i == 0;
                        if (i == 1) {
                            binding.update();
                        } else if (binding.media != null) {
                            binding.media.setPaused(true);
                        }
                    }
                }
            }
        }
    }

    public static void refresh() {
        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: cat.ttcuz.framework.CommentBackground$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                CommentBackground.visit();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void visit() {
        for (int size = panels.size() - 1; size >= 0; size--) {
            ArrayList<WeakReference<Binding>> arrayList = panels;
            Binding binding = arrayList.get(size).get();
            if (binding == null) {
                arrayList.remove(size);
            } else {
                binding.update();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class Binding {
        final FrameLayout host;
        CommentMediaView media;
        boolean paused;
        File rejected;
        final View root;
        File showing;
        boolean surfaceQueued;
        final Map<View, Drawable> backgrounds = new HashMap();
        final Map<View, Drawable> composers = new HashMap();
        final Map<FrameLayout, Drawable> fronts = new HashMap();
        final Map<Drawable, Integer> composerAlpha = new HashMap();
        final ViewTreeObserver.OnPreDrawListener surfaceCheck = new ViewTreeObserver.OnPreDrawListener() { // from class: cat.ttcuz.framework.CommentBackground$Binding$$ExternalSyntheticLambda0
            @Override // android.view.ViewTreeObserver.OnPreDrawListener
            public final boolean onPreDraw() {
                boolean lambda$new$0;
                lambda$new$0 = CommentBackground.Binding.this.lambda$new$0();
                return lambda$new$0;
            }
        };
        final Runnable discover = new Runnable() { // from class: cat.ttcuz.framework.CommentBackground$Binding$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                CommentBackground.Binding.this.discoverLater();
            }
        };
        final ViewTreeObserver.OnGlobalLayoutListener layoutCheck = new ViewTreeObserver.OnGlobalLayoutListener() { // from class: cat.ttcuz.framework.CommentBackground$Binding$$ExternalSyntheticLambda2
            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public final void onGlobalLayout() {
                CommentBackground.Binding.this.scheduleSurfaces();
            }
        };

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ boolean lambda$new$0() {
            if (this.media == null) {
                return true;
            }
            clearSurfaces();
            return true;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void discoverLater() {
            this.surfaceQueued = false;
            if (this.media == null || !this.host.isAttachedToWindow()) {
                return;
            }
            discoverSurfaces();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void scheduleSurfaces() {
            if (this.media == null || this.surfaceQueued) {
                return;
            }
            this.surfaceQueued = true;
            this.host.postDelayed(this.discover, 32L);
        }

        Binding(View view, FrameLayout frameLayout) {
            this.root = view;
            this.host = frameLayout;
            frameLayout.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() { // from class: cat.ttcuz.framework.CommentBackground.Binding.1
                @Override // android.view.View.OnAttachStateChangeListener
                public void onViewAttachedToWindow(View view2) {
                    Binding.this.update();
                }

                @Override // android.view.View.OnAttachStateChangeListener
                public void onViewDetachedFromWindow(View view2) {
                    // Only paused: the panel comes back on the next open, and rebuilding the media then is what
                    // made it flash (an empty frame, then the poster, then the video). It goes on destroy.
                    Binding.this.host.removeCallbacks(Binding.this.discover);
                    Binding.this.surfaceQueued = false;
                    if (Binding.this.media != null) Binding.this.media.setPaused(true);
                }
            });
        }

        void update() {
            final File file = CommentBackground.enabled() ? CommentBackground.file() : null;
            if (file == null) {
                remove();
                this.rejected = null;
                return;
            }
            if (file.equals(this.rejected)) {
                return;
            }
            if (this.media == null || !file.equals(this.showing)) {
                remove();
                this.showing = file;
                this.media = new CommentMediaView(this.host.getContext(), file, CommentBackground.gif(), new Runnable() { // from class: cat.ttcuz.framework.CommentBackground$Binding$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        CommentBackground.Binding.this.lambda$update$1(file);
                    }
                });
                discoverSurfaces();
                this.host.addView(this.media, 0, new FrameLayout.LayoutParams(-1, -1));
                Diary.note("comment background: media attached to panel");
                this.host.getViewTreeObserver().addOnPreDrawListener(this.surfaceCheck);
                this.host.getViewTreeObserver().addOnGlobalLayoutListener(this.layoutCheck);
            }
            this.media.scale(CommentBackground.scale());
            this.media.effects(CommentBackground.blur(), CommentBackground.dim());
            this.media.setPaused(this.paused);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$update$1(File file) {
            if (file.equals(this.showing)) {
                this.rejected = file;
                remove();
            }
        }

        void remove() {
            this.host.removeCallbacks(this.discover);
            this.surfaceQueued = false;
            if (this.media != null) {
                this.host.getViewTreeObserver().removeOnPreDrawListener(this.surfaceCheck);
                this.host.getViewTreeObserver().removeOnGlobalLayoutListener(this.layoutCheck);
                this.media.suspend();
                this.host.removeView(this.media);
                this.media = null;
            }
            for (Map.Entry<View, Drawable> entry : this.backgrounds.entrySet()) {
                entry.getKey().setBackground(entry.getValue());
            }
            this.backgrounds.clear();
            for (Map.Entry<FrameLayout, Drawable> front : this.fronts.entrySet()) front.getKey().setForeground(front.getValue());
            this.fronts.clear();
            for (Map.Entry<Drawable, Integer> entry2 : this.composerAlpha.entrySet()) {
                entry2.getKey().setAlpha(entry2.getValue().intValue());
            }
            for (Map.Entry<View, Drawable> entry3 : this.composers.entrySet()) {
                entry3.getKey().setBackground(entry3.getValue());
            }
            this.composers.clear();
            this.composerAlpha.clear();
            this.showing = null;
        }

        void clearSurfaces() {
            repairComposer();
            for (View view : this.backgrounds.keySet()) {
                Drawable background = view.getBackground();
                if (background != null) {
                    this.backgrounds.put(view, background);
                    view.setBackground(null);
                }
            }
        }

        void discoverSurfaces() {
            shadeComposer();
            int[] iArr = {2131373645, 2131381457, 2131368240, 2131368239, 2131368238, 2131368313};
            for (int i = 0; i < 6; i++) {
                View findViewById = this.host.findViewById(iArr[i]);
                if (findViewById != null) {
                    clearSurface(findViewById);
                }
            }
            View findViewById2 = this.host.findViewById(2131368240);
            if (findViewById2 != null) {
                discoverLists(findViewById2);
            }
            // The panels the fixed ids miss: the header's own layers and its dark gradient (l6g), the input bar's
            // container (i2g/lgn). Anything spanning the sheet that paints a flat or gradient colour is cleared;
            // the narrower "Add comment" pill and the comments themselves keep theirs.
            sweep(this.host, 0);
            clearStripBelow();
        }

        private static final int STRIP_BELOW = 0x7f0a248f;  // app:id/gry, the inset strip under the input bar
        private final int[] where = new int[2], hostAt = new int[2];

        private boolean painted(Drawable drawable) {
            if (drawable instanceof android.graphics.drawable.ColorDrawable)
                return (((android.graphics.drawable.ColorDrawable) drawable).getColor() >>> 24) != 0;
            return drawable instanceof android.graphics.drawable.GradientDrawable
                    || drawable instanceof android.graphics.drawable.LayerDrawable && !(drawable instanceof android.graphics.drawable.RippleDrawable);
        }

        private void sweep(View view, int depth) {
            if (view == null || view == this.media || depth > 14) return;
            if (view != this.host && view.getWidth() >= this.host.getWidth() * 0.9f && painted(view.getBackground())) {
                clearSurface(view);
            }
            if (view instanceof FrameLayout && view != this.host) {
                Drawable front = ((FrameLayout) view).getForeground();
                if (front != null && view.getWidth() >= this.host.getWidth() * 0.9f && painted(front)) {
                    this.fronts.put((FrameLayout) view, front);
                    ((FrameLayout) view).setForeground(null);
                }
            }
            if (!(view instanceof ViewGroup)) return;
            // the comments themselves: the list is cleared by discoverLists, its rows are left as TikTok drew them
            for (Class<?> type = view.getClass(); type != null; type = type.getSuperclass()) {
                String name = type.getName();
                if (name.equals("androidx.recyclerview.widget.RecyclerView") || name.equals("android.widget.AbsListView")) return;
            }
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) sweep(group.getChildAt(i), depth + 1);
        }

        /** The strip TikTok keeps under the input bar (the gesture area): cleared when it sits on the sheet's bottom. */
        private void clearStripBelow() {
            View up = this.host;
            this.host.getLocationOnScreen(hostAt);
            int bottom = hostAt[1] + this.host.getHeight();
            for (int depth = 0; depth < 8 && up != null; depth++) {
                View strip = up.findViewById(STRIP_BELOW);
                if (strip != null) {
                    strip.getLocationOnScreen(where);
                    if (Math.abs(where[1] + strip.getHeight() - bottom) <= 3 && strip.getWidth() >= this.host.getWidth() * 0.9f)
                        clearSurface(strip);
                    return;
                }
                Object parent = up.getParent();
                up = parent instanceof View ? (View) parent : null;
            }
        }

        void repairComposer() {
            for (Map.Entry<View, Drawable> entry : this.composers.entrySet()) {
                View key = entry.getKey();
                Drawable background = key.getBackground();
                if (background == entry.getValue()) {
                    Integer num = this.composerAlpha.get(background);
                    if (num != null) {
                        int min = Math.min(num.intValue(), (key.getId() == 2131384696 || key.getId() == 2131372063) ? 0 : 170);
                        if (background.getAlpha() != min) {
                            background.setAlpha(min);
                        }
                    }
                } else {
                    shadeComposer();
                    return;
                }
            }
        }

        void shadeComposer() {
            Drawable background;
            View findViewById = this.root.findViewById(2131384696);
            if (findViewById == null) {
                findViewById = this.root.findViewById(2131370318);
            }
            if (findViewById == null) {
                return;
            }
            int[] iArr = {2131384696, 2131368812, 2131370318, 2131372063};
            for (int i = 0; i < 4; i++) {
                int i2 = iArr[i];
                View findViewById2 = findViewById.findViewById(i2);
                if (findViewById2 != null && !this.backgrounds.containsKey(findViewById2) && (background = findViewById2.getBackground()) != null) {
                    this.composers.put(findViewById2, background);
                    if (!this.composerAlpha.containsKey(background)) {
                        this.composerAlpha.put(background, Integer.valueOf(background.getAlpha()));
                    }
                    background.mutate().setAlpha(Math.min((i2 == 2131384696 || i2 == 2131372063) ? 0 : 170, this.composerAlpha.get(background).intValue()));
                }
            }
        }

        void discoverLists(View view) {
            if (view instanceof ViewGroup) {
                for (Class<?> cls = view.getClass(); cls != null; cls = cls.getSuperclass()) {
                    String name = cls.getName();
                    if (name.equals("androidx.recyclerview.widget.RecyclerView") || name.equals("android.widget.AbsListView")) {
                        while (view != this.host) {
                            clearSurface(view);
                            Object parent = view.getParent();
                            if (!(parent instanceof View)) {
                                return;
                            } else {
                                view = (View) parent;
                            }
                        }
                        return;
                    }
                }
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    discoverLists(viewGroup.getChildAt(i));
                }
            }
        }

        void clearSurface(View view) {
            Drawable background = view.getBackground();
            if (!this.backgrounds.containsKey(view) || background != null) {
                this.backgrounds.put(view, background);
            }
            if (background != null) {
                view.setBackground(null);
            }
        }
    }
}
