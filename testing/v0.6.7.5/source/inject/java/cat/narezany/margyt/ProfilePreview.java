package cat.narezany.margyt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/** Shows the signed-in TikTok account, updating as its real model is observed. */
final class ProfilePreview {
    private ProfilePreview() {}

    private static final int AVATAR_TAG = 0x54504341;
    private static String readyUrl;
    private static String loadingUrl;
    private static String failedUrl;
    private static long failedUntil;
    private static boolean refreshQueued;
    private static Bitmap ready;
    private static final List<Preview> previews = new ArrayList<Preview>();
    private static final List<WeakReference<Preview>> loading =
            new ArrayList<WeakReference<Preview>>();

    private static final class Preview {
        final WeakReference<Context> context;
        final WeakReference<ImageView> image;
        final WeakReference<TextView> initial;
        final WeakReference<TextView> nickname;
        final WeakReference<TextView> handle;
        final int nicknameColour;

        Preview(Context context, ImageView image, TextView initial,
                TextView nickname, TextView handle) {
            this.context = new WeakReference<Context>(context.getApplicationContext());
            this.image = new WeakReference<ImageView>(image);
            this.initial = new WeakReference<TextView>(initial);
            this.nickname = new WeakReference<TextView>(nickname);
            this.handle = new WeakReference<TextView>(handle);
            this.nicknameColour = nickname.getCurrentTextColor();
        }
    }

    static void bind(Context context, ImageView image, TextView placeholder,
                     TextView nickname, TextView handle) {
        if (context == null || image == null || placeholder == null
                || nickname == null || handle == null) return;
        Preview item = new Preview(context, image, placeholder, nickname, handle);
        synchronized (ProfilePreview.class) {
            for (int i = previews.size() - 1; i >= 0; i--) {
                Preview old = previews.get(i);
                if (old.image.get() == null || old.nickname.get() == null)
                    previews.remove(i);
                else if (old.image.get() == image) previews.set(i, item);
            }
            boolean present = false;
            for (Preview old : previews) if (old.image.get() == image) present = true;
            if (!present) previews.add(item);
        }
        accountUpdated();
    }

    /** Called whenever TikTok exposes a new name, account, or avatar. */
    static void accountUpdated() {
        synchronized (ProfilePreview.class) {
            if (refreshQueued) return;
            refreshQueued = true;
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            synchronized (ProfilePreview.class) { refreshQueued = false; }
            List<Preview> copy;
            synchronized (ProfilePreview.class) {
                copy = new ArrayList<Preview>(previews);
            }
            String name = Account.profileName();
            String handle = Account.profileHandle();
            String uid = Account.id();
            for (Preview item : copy) {
                ImageView image = item.image.get();
                TextView initial = item.initial.get();
                TextView nickname = item.nickname.get();
                TextView handleView = item.handle.get();
                if (image == null || nickname == null || handleView == null) {
                    synchronized (ProfilePreview.class) { previews.remove(item); }
                    continue;
                }
                if (name != null && name.length() > 0) {
                    if (initial != null) {
                        initial.setText(name.substring(0, 1).toUpperCase(
                                java.util.Locale.ROOT));
                    }
                    styleNickname(item, nickname, name);
                    handleView.setText(handle == null || handle.length() == 0
                            ? "TikTok ID · " + (uid == null ? "—" : uid)
                            : "@" + handle);
                }
                loadAvatar(item);
            }
        }, 32L);
    }

    private static void styleNickname(Preview item, TextView view, String name) {
        view.setTextColor(item.nicknameColour);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setShadowLayer(0f, 0f, 0f, 0);
        Badge.previewNickname(view, name, Account.id());
    }

    private static void loadAvatar(Preview item) {
        ImageView image = item.image.get();
        TextView initial = item.initial.get();
        Context context = item.context.get();
        if (image == null || initial == null || context == null) return;
        String address = Account.profileAvatar();
        if (address == null || address.length() == 0) return;
        image.setTag(AVATAR_TAG, address);
        synchronized (ProfilePreview.class) {
            if (address.equals(readyUrl) && ready != null) {
                show(item, ready);
                return;
            }
            if (address.equals(failedUrl) && failedUntil > System.currentTimeMillis()) return;
            if (!address.equals(loadingUrl)) {
                loading.clear();
                loadingUrl = address;
            }
            loading.add(new WeakReference<Preview>(item));
            if (!address.equals(loadingUrl)) return;
            if (loading.size() > 64) loading.remove(0);
            if (loading.size() > 1) return;
        }

        final String requested = address;
        final Context app = context.getApplicationContext();
        Net.away("profile preview", () -> {
            Bitmap bitmap = null;
            try {
                File cached = cacheFile(app, requested);
                byte[] bytes = cached.isFile() && cached.length() <= 2 * 1024 * 1024
                        ? Net.read(cached) : Net.bytes(requested);
                if (bytes != null && bytes.length > 0 && bytes.length <= 2 * 1024 * 1024) {
                    BitmapFactory.Options bounds = new BitmapFactory.Options();
                    bounds.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
                    if (bounds.outWidth > 0 && bounds.outHeight > 0
                            && bounds.outWidth <= 4096 && bounds.outHeight <= 4096) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = Math.max(1,
                                Math.max(bounds.outWidth, bounds.outHeight) / 256);
                        bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                        if (bitmap != null) Net.save(cached, bytes);
                    }
                }
            } catch (Throwable error) {
                Diary.note("profile preview avatar: " + error);
            }

            final Bitmap result = bitmap;
            final List<Preview> targets = new ArrayList<Preview>();
            synchronized (ProfilePreview.class) {
                if (!requested.equals(loadingUrl)) return;
                loadingUrl = null;
                if (result == null) {
                    failedUrl = requested;
                    failedUntil = System.currentTimeMillis() + 30000L;
                    loading.clear();
                    return;
                }
                readyUrl = requested;
                ready = result;
                for (WeakReference<Preview> ref : loading) {
                    Preview target = ref.get();
                    if (target != null) targets.add(target);
                }
                loading.clear();
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                for (Preview target : targets) {
                    ImageView view = target.image.get();
                    if (view != null && requested.equals(view.getTag(AVATAR_TAG)))
                        show(target, result);
                }
            });
        });
    }

    private static void show(Preview item, Bitmap bitmap) {
        ImageView image = item.image.get();
        TextView initial = item.initial.get();
        if (image == null || initial == null) return;
        image.setImageBitmap(bitmap);
        image.setVisibility(View.VISIBLE);
        initial.setVisibility(View.GONE);
    }

    private static File cacheFile(Context context, String address) throws Exception {
        byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                .digest(address.getBytes("UTF-8"));
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < 16; i++)
            name.append(String.format(java.util.Locale.ROOT, "%02x", hash[i] & 255));
        File directory = new File(context.getFilesDir(), "ttcuz/profile-cache");
        if (!directory.isDirectory()) directory.mkdirs();
        return new File(directory, "avatar-" + name + ".img");
    }
}
