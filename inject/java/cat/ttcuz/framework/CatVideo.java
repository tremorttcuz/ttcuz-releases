package cat.ttcuz.framework;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.Window;
import android.widget.FrameLayout;

/**
 * The owner's cat: a short video that rides inside the apk (assets/ttcuz/cat.mp4), played in a small rounded window
 * after a long run of taps on the settings' title. It loops until the window is tapped or closed.
 */
final class CatVideo {
    private CatVideo() {}

    static void play(Context context) {
        if (!(context instanceof Activity) || ((Activity) context).isFinishing()) return;
        final Activity activity = (Activity) context;
        try {
            final AssetFileDescriptor file = activity.getAssets().openFd("ttcuz/cat.mp4");
            float d = activity.getResources().getDisplayMetrics().density;
            int width = Math.round(Math.min(activity.getResources().getDisplayMetrics().widthPixels * .7f, 300 * d));
            int height = Math.round(width * 638f / 480f);

            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            FrameLayout frame = new FrameLayout(activity);
            android.graphics.drawable.GradientDrawable round = new android.graphics.drawable.GradientDrawable();
            round.setColor(Color.BLACK);
            round.setCornerRadius(28 * d);
            frame.setBackground(round);
            frame.setClipToOutline(true);
            final TextureView screen = new TextureView(activity);
            frame.addView(screen, new FrameLayout.LayoutParams(width, height, Gravity.CENTER));
            dialog.setContentView(frame);
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setDimAmount(.6f);
            }
            final MediaPlayer player = new MediaPlayer();
            player.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            player.setLooping(true);
            screen.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override public void onSurfaceTextureAvailable(SurfaceTexture texture, int w, int h) {
                    try {
                        player.setSurface(new Surface(texture));
                        player.setOnPreparedListener(MediaPlayer::start);
                        player.prepareAsync();
                    } catch (Throwable error) {
                        Diary.note("cat video: " + error);
                    }
                }
                @Override public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int w, int h) {}
                @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) { return true; }
                @Override public void onSurfaceTextureUpdated(SurfaceTexture texture) {}
            });
            frame.setOnClickListener(v -> dialog.dismiss());
            dialog.setOnDismissListener(gone -> {
                try { player.release(); } catch (Throwable ignored) { }
                try { file.close(); } catch (Throwable ignored) { }
            });
            dialog.show();
            if (Motion.enabled(activity)) {
                frame.setScaleX(.85f); frame.setScaleY(.85f); frame.setAlpha(0f);
                frame.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(260L).setInterpolator(Motion.OUT).start();
            }
        } catch (Throwable error) {
            Diary.note("cat video: " + error);
        }
    }
}
