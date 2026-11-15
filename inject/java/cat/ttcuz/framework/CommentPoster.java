package cat.ttcuz.framework;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

final class CommentPoster {
    private static Bitmap frame;
    private static File pending;
    private static File source;
    private static final ArrayList<WeakReference<View>> waiting = new ArrayList<>();

    CommentPoster() {
    }

    static synchronized Bitmap get(File file) {
        Bitmap bitmap;
        synchronized (CommentPoster.class) {
            bitmap = file.equals(source) ? frame : null;
        }
        return bitmap;
    }

    static synchronized void warm(final File file, View view) {
        synchronized (CommentPoster.class) {
            if (file != null) {
                if (!file.equals(source)) {
                    if (view != null) {
                        waiting.add(new WeakReference<>(view));
                    }
                    if (file.equals(pending)) {
                        return;
                    }
                    pending = file;
                    Net.away("comment opening frame", new Runnable() { // from class: cat.ttcuz.framework.CommentPoster$$ExternalSyntheticLambda1
                        @Override // java.lang.Runnable
                        public final void run() {
                            CommentPoster.lambda$warm$1(file);
                        }
                    });
                }
            }
        }
    }

    static /* synthetic */ void lambda$warm$1(File file) {
        Bitmap read = read(file);
        synchronized (CommentPoster.class) {
            if (file.equals(pending)) {
                pending = null;
                source = file;
                frame = read;
                ArrayList<WeakReference<View>> arrayList = waiting;
                final ArrayList arrayList2 = new ArrayList(arrayList);
                arrayList.clear();
                new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: cat.ttcuz.framework.CommentPoster$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        CommentPoster.lambda$warm$0(arrayList2);
                    }
                });
            }
        }
    }

    static /* synthetic */ void lambda$warm$0(ArrayList arrayList) {
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            View view = (View) ((WeakReference) it.next()).get();
            if (view != null && view.isAttachedToWindow()) {
                if (view instanceof CommentMediaView) {
                    ((CommentMediaView) view).posterReady();
                } else {
                    view.invalidate();
                }
            }
        }
    }

    private static int sample(int i, int i2, int i3) {
        int i4 = 1;
        while (Math.max(i, i2) / i4 > i3) {
            i4 *= 2;
        }
        return i4;
    }

    private static Bitmap read(File file) {
        Bitmap frameAtTime;
        File file2 = new File(file.getPath() + ".preview.jpg");
        if (file2.isFile()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file2.getPath(), options);
            options.inSampleSize = sample(options.outWidth, options.outHeight, DevicePerformance.previewEdge());
            options.inJustDecodeBounds = false;
            try {
                Bitmap decodeFile = BitmapFactory.decodeFile(file2.getPath(), options);
                if (decodeFile != null) {
                    return decodeFile;
                }
            } catch (OutOfMemoryError unused) {
                return null;
            }
        }
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            try {
                try {
                    mediaMetadataRetriever.setDataSource(file.getPath());
                    int parseInt = Integer.parseInt(mediaMetadataRetriever.extractMetadata(18));
                    int parseInt2 = Integer.parseInt(mediaMetadataRetriever.extractMetadata(19));
                    int previewEdge = DevicePerformance.previewEdge();
                    if (Build.VERSION.SDK_INT < 27 && parseInt * parseInt2 > 2097152) {
                        return null;
                    }
                    float f = previewEdge;
                    float min = Math.min(1.0f, f / Math.max(parseInt, parseInt2));
                    if (Build.VERSION.SDK_INT >= 27) {
                        frameAtTime = mediaMetadataRetriever.getScaledFrameAtTime(0L, 2, Math.max(1, Math.round(parseInt * min)), Math.max(1, Math.round(parseInt2 * min)));
                    } else {
                        frameAtTime = mediaMetadataRetriever.getFrameAtTime(0L, 2);
                    }
                    if (frameAtTime != null && Math.max(frameAtTime.getWidth(), frameAtTime.getHeight()) > previewEdge) {
                        float max = f / Math.max(frameAtTime.getWidth(), frameAtTime.getHeight());
                        Bitmap createScaledBitmap = Bitmap.createScaledBitmap(frameAtTime, Math.max(1, Math.round(frameAtTime.getWidth() * max)), Math.max(1, Math.round(frameAtTime.getHeight() * max)), true);
                        if (createScaledBitmap != frameAtTime) {
                            frameAtTime.recycle();
                        }
                        frameAtTime = createScaledBitmap;
                    }
                    if (frameAtTime != null) {
                        try {
                            FileOutputStream fileOutputStream = new FileOutputStream(file2);
                            try {
                                frameAtTime.compress(Bitmap.CompressFormat.JPEG, 85, fileOutputStream);
                                fileOutputStream.close();
                            } catch (Throwable th) {
                                try {
                                    fileOutputStream.close();
                                } catch (Throwable th2) {
                                    th.addSuppressed(th2);
                                }
                                throw th;
                            }
                        } catch (Exception unused2) {
                        }
                    }
                    try {
                        mediaMetadataRetriever.release();
                    } catch (Exception unused3) {
                    }
                    return frameAtTime;
                } finally {
                    try {
                        mediaMetadataRetriever.release();
                    } catch (Exception unused4) {
                    }
                }
            } catch (Exception e) {
                Diary.note("comment opening frame: " + e.getClass().getSimpleName());
                try {
                    mediaMetadataRetriever.release();
                } catch (Exception unused5) {
                }
                return null;
            }
        } catch (OutOfMemoryError e2) {
            Diary.note("comment opening frame: " + e2.getClass().getSimpleName());
            try { mediaMetadataRetriever.release(); } catch (Exception ignored) { }
            return null;
        }
    }

    static synchronized void discard(File file) {
        synchronized (CommentPoster.class) {
            if (file == null) {
                return;
            }
            new File(file.getPath() + ".preview.jpg").delete();
            if (file.equals(source)) {
                source = null;
                frame = null;
            }
            if (file.equals(pending)) {
                pending = null;
                waiting.clear();
            }
        }
    }
}
