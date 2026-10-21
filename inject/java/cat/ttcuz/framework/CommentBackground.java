package cat.ttcuz.framework;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/** Local media only; no upload to the public profile or network fetch during drawing. */
public final class CommentBackground {

    public static final String SECTION = "Фон комментариев";
    public static final String ON = "comment_background_on";
    public static final String BLUR = "comment_background_blur";
    public static final String DIM = "comment_background_dim";
    // Use a new key so existing installs pick up the requested full-window
    // stretch once; users can still choose another mode afterward.
    private static final String SCALE = "comment_background_scale_v2";
    private static final String FILE = "comment_background_file";
    private static final String GIF = "comment_background_gif";
    private static final int TAG = 0x54544342;
    private static final ArrayList<WeakReference<Binding>> panels = new ArrayList<>();

    private CommentBackground() {}

    private static SharedPreferences prefs() {
        return Ttcuz.context().getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
    }

    public static boolean enabled() {
        try {
            return prefs().getBoolean(ON, false);
        } catch (RuntimeException error) {
            return false;
        }
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
        return new String[] {
            "Заполнить с обрезкой",
            "Показать целиком",
            "Растянуть на всё окно"
        };
    }

    public static String scaleName() {
        return scaleOptions()[scale()];
    }

    public static void setScale(int value) {
        prefs().edit().putInt(SCALE, Math.max(0, Math.min(2, value))).apply();
        refresh();
    }

    private static int number(String key, int fallback, int max) {
        try {
            return Math.max(0, Math.min(max, prefs().getInt(key, fallback)));
        } catch (RuntimeException error) {
            return fallback;
        }
    }

    static File file() {
        try {
            String name = prefs().getString(FILE, "");
            if (!name.matches("comments-[a-f0-9-]+\\.media")) return null;
            File file = new File(Ttcuz.context().getFilesDir(), "ttcuz/" + name);
            return file.isFile() ? file : null;
        } catch (RuntimeException error) {
            return null;
        }
    }

    static boolean gif() {
        try {
            return prefs().getBoolean(GIF, false);
        } catch (RuntimeException error) {
            return false;
        }
    }

    public static boolean hasMedia() {
        return file() != null;
    }

    static void preload() {
        if (enabled() && !gif()) CommentPoster.warm(file(), null);
    }

    public static String description() {
        return hasMedia() ? (gif() ? "GIF выбран" : "Видео выбрано") : "Выберите видео или GIF";
    }

    public static void setEnabled(boolean on) {
        prefs()
            .edit()
            .putBoolean(ON, on && hasMedia())
            .apply();
        refresh();
    }

    public static void setBlur(int value) {
        prefs()
            .edit()
            .putInt(BLUR, Math.max(0, Math.min(32, value)))
            .apply();
        refresh();
    }

    public static void setDim(int value) {
        prefs()
            .edit()
            .putInt(DIM, Math.max(0, Math.min(90, value)))
            .apply();
        refresh();
    }

    public static void clear() {
        File old = file();
        prefs().edit().remove(FILE).remove(GIF).putBoolean(ON, false).apply();
        refresh();
        if (old != null) { old.delete(); CommentPoster.discard(old); }
    }

    /** Called off the UI thread. A failed import preserves the previous selection. */
    public static boolean take(Context context, Uri source) {
        File target = new File(
            context.getFilesDir(),
            "ttcuz/comments-" + java.util.UUID.randomUUID() + ".media"
        );
        boolean saved = false;
        try {
            File folder = target.getParentFile();
            if (!folder.isDirectory() && !folder.mkdirs()) return false;
            try (
                InputStream in = context.getContentResolver().openInputStream(source);
                FileOutputStream out = new FileOutputStream(target)
            ) {
                if (in == null) return false;
                byte[] block = new byte[64 * 1024];
                long size = 0;
                int n;
                while ((n = in.read(block)) != -1) {
                    size += n;
                    if (size > 256L * 1024 * 1024) return false;
                    out.write(block, 0, n);
                }
                if (size == 0) return false;
            }
            boolean isGif;
            try (FileInputStream in = new FileInputStream(target)) {
                byte[] head = new byte[6];
                isGif =
                    in.read(head) == 6 &&
                    (new String(head, "US-ASCII").equals("GIF89a") ||
                        new String(head, "US-ASCII").equals("GIF87a"));
            }
            if (isGif) {
                if (target.length() > 24L * 1024 * 1024) return false;
                android.graphics.BitmapFactory.Options bounds =
                    new android.graphics.BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                android.graphics.BitmapFactory.decodeFile(target.getPath(), bounds);
                if (
                    bounds.outWidth <= 0 ||
                    bounds.outHeight <= 0 ||
                    (long) bounds.outWidth * bounds.outHeight > 8_000_000L
                ) return false;
                if (
                    android.os.Build.VERSION.SDK_INT < 28 &&
                    (target.length() > 4L * 1024 * 1024 ||
                        Math.max(bounds.outWidth, bounds.outHeight) > 1024)
                ) return false;
            } else {
                MediaMetadataRetriever reader = new MediaMetadataRetriever();
                try {
                    reader.setDataSource(target.getPath());
                    int width = Integer.parseInt(
                        reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    );
                    int height = Integer.parseInt(
                        reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    );
                    if (width <= 0 || height <= 0 || Math.max(width, height) > 3840) return false;
                } finally {
                    reader.release();
                }
            }
            File old = file();
            saved = prefs()
                .edit()
                .putString(FILE, target.getName())
                .putBoolean(GIF, isGif)
                .putBoolean(ON, true)
                .commit();
            if (saved && old != null) { old.delete(); CommentPoster.discard(old); }
            if (saved && !isGif) CommentPoster.warm(target, null);
            if (saved) Diary.note("comment background: imported " + (isGif ? "GIF" : "video") +
                ", " + target.length() + " bytes");
            return saved;
        } catch (Exception error) {
            Diary.note("comment background import: " + error);
            return false;
        } finally {
            if (!saved) target.delete();
        }
    }

    /** Hooked only to the native comment fragment, after its view has been populated. */
    public static void bind(View root) {
        if (root == null) return;
        if (root.findViewById(0x7f0a2e60) instanceof FrameLayout) bindNow(root);
        else root.post(() -> bindNow(root));
    }

    private static void bindNow(View root) {
            try {
                // Verified panel ID in TikTok 47.2.41 res/b/zg.xml. Never use the activity decor.
                View found = root.findViewById(0x7f0a2e60);
                if (!(found instanceof FrameLayout)) {
                    if (enabled()) Diary.note("comment background: panel missing in " + root.getClass().getName());
                    return;
                }
                if (found.getTag(TAG) instanceof Binding) return;
                Binding binding = new Binding(root, (FrameLayout) found);
                found.setTag(TAG, binding);
                panels.add(new WeakReference<>(binding));
                binding.update();
            } catch (RuntimeException error) {
                Diary.note("comment background: " + error);
            }
    }

    public static void pause(Object fragment) {
        lifecycle(fragment, 0);
    }

    public static void resume(Object fragment) {
        lifecycle(fragment, 1);
    }

    public static void destroy(Object fragment) {
        lifecycle(fragment, 2);
    }

    private static void lifecycle(Object fragment, int event) {
        Object view = NativeRead.get(fragment, "getView");
        if (!(view instanceof View)) return;
        if (event == 1) bind((View) view);
        for (int i = panels.size() - 1; i >= 0; i--) {
            Binding binding = panels.get(i).get();
            if (binding == null) panels.remove(i);
            else if (binding.root == view) {
                if (event == 2) {
                    binding.remove();
                    binding.host.setTag(TAG, null);
                    panels.remove(i);
                } else {
                    binding.paused = event == 0;
                    if (event == 1) binding.update();
                    else if (binding.media != null) binding.media.setPaused(true);
                }
            }
        }
    }

    public static void refresh() {
        new Handler(Looper.getMainLooper()).post(CommentBackground::visit);
    }

    private static void visit() {
        for (int i = panels.size() - 1; i >= 0; i--) {
            Binding binding = panels.get(i).get();
            if (binding == null) panels.remove(i);
            else binding.update();
        }
    }

    private static final class Binding {

        final FrameLayout host;
        final View root;
        boolean paused;
        final java.util.Map<View, Drawable> backgrounds = new java.util.HashMap<>();
        final java.util.Map<View, Drawable> composers = new java.util.HashMap<>();
        final java.util.Map<Drawable, Integer> composerAlpha = new java.util.HashMap<>();
        CommentMediaView media;
        File showing;
        File rejected;
        final android.view.ViewTreeObserver.OnPreDrawListener surfaceCheck = () -> {
            if (media != null) clearSurfaces();
            return true;
        };
        final android.view.ViewTreeObserver.OnGlobalLayoutListener layoutCheck = () -> {
            if (media != null) discoverSurfaces();
        };

        Binding(View root, FrameLayout host) {
            this.root = root;
            this.host = host;
        }

        void update() {
            File next = enabled() ? file() : null;
            if (next == null) {
                remove();
                rejected = null;
                return;
            }
            if (next.equals(rejected)) return;
            if (media == null || !next.equals(showing)) {
                remove();
                showing = next;
                media = new CommentMediaView(host.getContext(), next, gif(), () -> {
                    if (next.equals(showing)) {
                        rejected = next;
                        remove();
                    }
                });
                discoverSurfaces();
                host.addView(media, 0, new FrameLayout.LayoutParams(-1, -1));
                Diary.note("comment background: media attached to panel");
                host.getViewTreeObserver().addOnPreDrawListener(surfaceCheck);
                host.getViewTreeObserver().addOnGlobalLayoutListener(layoutCheck);
            }
            media.scale(scale());
            media.effects(blur(), dim());
            media.setPaused(paused);
        }

        void remove() {
            if (media != null) {
                host.getViewTreeObserver().removeOnPreDrawListener(surfaceCheck);
                host.getViewTreeObserver().removeOnGlobalLayoutListener(layoutCheck);
                media.suspend();
                host.removeView(media);
                media = null;
            }
            for (java.util.Map.Entry<View, Drawable> entry : backgrounds.entrySet())
                entry.getKey().setBackground(entry.getValue());
            backgrounds.clear();
            for (java.util.Map.Entry<Drawable, Integer> entry : composerAlpha.entrySet())
                entry.getKey().setAlpha(entry.getValue());
            for (java.util.Map.Entry<View, Drawable> entry : composers.entrySet())
                entry.getKey().setBackground(entry.getValue());
            composers.clear();
            composerAlpha.clear();
            showing = null;
        }

        void clearSurfaces() {
            shadeComposer();
            // TikTok restores panel surfaces after loading comments or changing theme.
            for (View view : backgrounds.keySet()) {
                Drawable current = view.getBackground();
                if (current != null) {
                    backgrounds.put(view, current);
                    view.setBackground(null);
                }
            }
        }

        void discoverSurfaces() {
            shadeComposer();
            for (int id : new int[] {
                0x7f0a2e60, 0x7f0a4ce4, 0x7f0a1943,
                0x7f0a1942, 0x7f0a1941, 0x7f0a198c,
            }) {
                View view = host.findViewById(id);
                if (view != null) clearSurface(view);
            }
            // Comment pages arrive asynchronously inside this ViewPager. Clear only
            // list surfaces and their ancestors; never descend into recycled rows.
            View pages = host.findViewById(0x7f0a1943);
            if (pages != null) discoverLists(pages);
        }

        void shadeComposer() {
            // Official 47.2.41 layouts res/b/zi.xml and res/b/eo0.xml:
            // keyboard panel 0x598b and its input/body surfaces. Never fade controls or text.
            View keyboard = root.findViewById(0x7f0a598b);
            View scope = keyboard != null ? keyboard : root.findViewById(0x7f0a2161);
            if (scope == null) return;
            for (int id : new int[]{0x7f0a598b, 0x7f0a1b7f, 0x7f0a2161, 0x7f0a2832}) {
                View view = scope.findViewById(id);
                if (view == null || backgrounds.containsKey(view)) continue;
                Drawable current = view.getBackground();
                if (current == null) continue;
                composers.put(view, current);
                if (!composerAlpha.containsKey(current)) composerAlpha.put(current, current.getAlpha());
                int alpha = id == 0x7f0a598b || id == 0x7f0a2832 ? 0 : 170;
                current.mutate().setAlpha(Math.min(alpha, composerAlpha.get(current)));
            }
        }

        void discoverLists(View view) {
            if (!(view instanceof ViewGroup)) return;
            for (Class<?> type = view.getClass(); type != null; type = type.getSuperclass()) {
                String name = type.getName();
                if (name.equals("androidx.recyclerview.widget.RecyclerView") ||
                    name.equals("android.widget.AbsListView")) {
                    View surface = view;
                    while (surface != host) {
                        clearSurface(surface);
                        ViewParent parent = surface.getParent();
                        if (!(parent instanceof View)) return;
                        surface = (View) parent;
                    }
                    return;
                }
            }
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) discoverLists(group.getChildAt(i));
        }

        void clearSurface(View view) {
            Drawable current = view.getBackground();
            if (!backgrounds.containsKey(view) || current != null) backgrounds.put(view, current);
            if (current != null) view.setBackground(null);
        }
    }
}
