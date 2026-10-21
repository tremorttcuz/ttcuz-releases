package cat.ttcuz.framework;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Movie;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.io.File;

/** One muted decoder per visible panel. No per-frame bitmap copies or CPU blur. */
final class CommentMediaView extends FrameLayout implements TextureView.SurfaceTextureListener {

    private final File file;
    private final boolean gif;
    private final Runnable failed;
    private TextureView texture;
    private MediaPlayer player;
    private Surface surface;
    private View picture;
    private ImageView preview;
    private Drawable animation;
    private boolean paused, active, broken, loading, frameSeen;
    private int shade, blur, videoWidth, videoHeight, generation, scaleMode;

    CommentMediaView(Context context, File file, boolean gif, Runnable failed) {
        super(context);
        this.file = file;
        this.gif = gif;
        this.failed = failed;
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        setBackgroundColor(0xff17191d);
        setOutlineProvider(
            new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setRoundRect(
                        0,
                        0,
                        view.getWidth(),
                        view.getHeight(),
                        16 * getResources().getDisplayMetrics().density
                    );
                }
            }
        );
        setClipToOutline(true);
        if (!gif) {
            preview = new ImageView(context);
            openingFrame();
            CommentPoster.warm(file, this);
            texture = new TextureView(context);
            texture.setOpaque(false);
            texture.setSurfaceTextureListener(this);
            addView(texture, new LayoutParams(-1, -1));
            addView(preview, new LayoutParams(-1, -1));
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        // The native panel is wrap_content. A MATCH_PARENT background must not
        // supply its intrinsic height during the panel's first AT_MOST pass.
        // FrameLayout measures it again with the final native panel dimensions.
        int width =
            MeasureSpec.getMode(widthSpec) == MeasureSpec.EXACTLY
                ? MeasureSpec.getSize(widthSpec)
                : 0;
        int height =
            MeasureSpec.getMode(heightSpec) == MeasureSpec.EXACTLY
                ? MeasureSpec.getSize(heightSpec)
                : 0;
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        );
    }

    void scale(int mode) {
        scaleMode = Math.max(0, Math.min(2, mode));
        if (preview != null) preview.setScaleType(
            scaleMode == 2
                ? ImageView.ScaleType.FIT_XY
                : scaleMode == 1
                  ? ImageView.ScaleType.FIT_CENTER
                  : ImageView.ScaleType.CENTER_CROP
        );
        if (picture instanceof ImageView) ((ImageView) picture).setScaleType(
            scaleMode == 2
                ? ImageView.ScaleType.FIT_XY
                : scaleMode == 1
                  ? ImageView.ScaleType.FIT_CENTER
                  : ImageView.ScaleType.CENTER_CROP
        );
        if (picture instanceof LegacyGif) {
            ((LegacyGif) picture).mode = scaleMode;
            picture.invalidate();
        }
        crop();
    }

    void effects(int radius, int dim) {
        blur = radius;
        shade = Math.round((255 * dim) / 100f);
        applyBlur();
        invalidate();
    }

    private void applyBlur() {
        View child = gif ? picture : texture;
        if (preview != null && Build.VERSION.SDK_INT >= 31) Blur.apply(
            preview,
            blur * getResources().getDisplayMetrics().density
        );
        if (child != null && Build.VERSION.SDK_INT >= 31) Blur.apply(
            child,
            blur * getResources().getDisplayMetrics().density
        );
    }

    void setPaused(boolean value) {
        paused = value;
        updateVisibility();
    }

    private void updateVisibility() {
        boolean next =
            !paused &&
            !broken &&
            isAttachedToWindow() &&
            isShown() &&
            getWindowVisibility() == VISIBLE;
        // A visible comment sheet can be in a non-focusable window. Fragment
        // pause and window visibility, rather than keyboard focus, stop playback.
        if (next == active) {
            if (next && !gif && player == null && texture != null && texture.isAvailable()) play();
            return;
        }
        active = next;
        if (next) {
            if (gif) loadGif();
            else if (texture != null && texture.isAvailable()) play();
            if (animation instanceof android.graphics.drawable.Animatable) (
                (android.graphics.drawable.Animatable) animation
            ).start();
            if (picture instanceof LegacyGif) ((LegacyGif) picture).running = true;
            if (picture != null) picture.invalidate();
        } else suspend();
    }

    private final android.view.ViewTreeObserver.OnPreDrawListener visibility = () -> {
        openingFrame();
        updateVisibility();
        return true;
    };

    private void openingFrame() {
        if (preview != null) preview.setVisibility(frameSeen ? INVISIBLE : VISIBLE);
        if (preview != null && preview.getDrawable() == null) {
            android.graphics.Bitmap frame = CommentPoster.get(file);
            if (frame != null) preview.setImageBitmap(frame);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnPreDrawListener(visibility);
        post(this::updateVisibility);
    }

    @Override
    protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnPreDrawListener(visibility);
        generation++;
        loading = false;
        suspend();
        super.onDetachedFromWindow();
    }

    @Override
    public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus);
        updateVisibility();
    }

    @Override
    protected void onWindowVisibilityChanged(int value) {
        super.onWindowVisibilityChanged(value);
        updateVisibility();
    }

    void suspend() {
        active = false;
        MediaPlayer old = player;
        player = null;
        if (old != null) {
            old.setOnPreparedListener(null);
            old.setOnErrorListener(null);
            old.release();
        }
        if (surface != null) {
            surface.release();
            surface = null;
        }
        if (animation instanceof android.graphics.drawable.Animatable) (
            (android.graphics.drawable.Animatable) animation
        ).stop();
        if (picture instanceof LegacyGif) {
            ((LegacyGif) picture).running = false;
            picture.removeCallbacks(((LegacyGif) picture).frame);
        }
    }

    private void error(Throwable error) {
        if (broken) return;
        broken = true;
        suspend();
        Diary.note("comment media: " + error);
        post(() -> {
            failed.run();
            Screen.say(
                "Этот файл не воспроизводится на устройстве. Выберите другое видео или GIF."
            );
        });
    }

    private void play() {
        if (!active || player != null || !texture.isAvailable()) return;
        try {
            MediaPlayer next = new MediaPlayer();
            player = next;
            frameSeen = false;
            openingFrame();
            videoWidth = videoHeight = 0;
            surface = new Surface(texture.getSurfaceTexture());
            next.setSurface(surface);
            next.setVolume(0, 0);
            next.setLooping(true);
            next.setDataSource(file.getPath());
            next.setOnPreparedListener(ready -> {
                if (player != ready || !active) return;
                videoWidth = ready.getVideoWidth();
                videoHeight = ready.getVideoHeight();
                crop();
                try {
                    ready.start();
                    Diary.note("comment media: video started " + videoWidth + "x" + videoHeight);
                } catch (RuntimeException failure) {
                    error(failure);
                }
            });
            next.setOnErrorListener((ready, what, extra) -> {
                if (player == ready) error(
                    new IllegalStateException("decoder " + what + "/" + extra)
                );
                return true;
            });
            next.prepareAsync();
            Diary.note("comment media: preparing video");
        } catch (Exception | OutOfMemoryError failure) {
            error(failure);
        }
    }

    private void loadGif() {
        if (picture != null || loading) return;
        loading = true;
        final int ticket = generation;
        Net.away("comment GIF", () -> {
            try {
                Drawable decoded = null;
                Movie movie = null;
                if (Build.VERSION.SDK_INT >= 28) decoded = Gif.decode(file);
                else {
                    movie = Movie.decodeFile(file.getPath());
                    if (movie == null) throw new IllegalArgumentException("invalid GIF");
                }
                final Drawable result = decoded;
                final Movie legacy = movie;
                post(() -> {
                    if (ticket != generation) return;
                    loading = false;
                    if (!isAttachedToWindow()) return;
                    animation = result;
                    if (result != null) {
                        ImageView image = new ImageView(getContext());
                        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        image.setImageDrawable(result);
                        picture = image;
                    } else picture = new LegacyGif(getContext(), legacy);
                    addView(picture, new LayoutParams(-1, -1));
                    scale(scaleMode);
                    applyBlur();
                    if (active && result instanceof android.graphics.drawable.Animatable) (
                        (android.graphics.drawable.Animatable) result
                    ).start();
                    if (picture instanceof LegacyGif) ((LegacyGif) picture).running = active;
                });
            } catch (Exception | OutOfMemoryError failure) {
                post(() -> {
                    if (ticket != generation) return;
                    loading = false;
                    error(failure);
                });
            }
        });
    }

    private void crop() {
        if (
            texture == null ||
            videoWidth <= 0 ||
            videoHeight <= 0 ||
            getWidth() == 0 ||
            getHeight() == 0
        ) return;
        float x = getWidth() / (float) videoWidth;
        float y = getHeight() / (float) videoHeight;
        float scale = scaleMode == 1 ? Math.min(x, y) : Math.max(x, y);
        // Stretch uses an independent horizontal and vertical fit. This maps
        // every source aspect ratio to the complete comment-panel viewport.
        float scaleX = scaleMode == 2 ? x : scale;
        float scaleY = scaleMode == 2 ? y : scale;
        Matrix matrix = new Matrix();
        matrix.setScale(
            (videoWidth * scaleX) / getWidth(),
            (videoHeight * scaleY) / getHeight(),
            getWidth() / 2f,
            getHeight() / 2f
        );
        texture.setTransform(matrix);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        crop();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        canvas.drawColor(android.graphics.Color.argb(shade, 0, 0, 0));
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture value, int width, int height) {
        Diary.note("comment media: surface " + width + "x" + height);
        updateVisibility();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture value, int width, int height) {
        crop();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture value) {
        suspend();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture value) {
        if (!frameSeen && player != null && videoWidth > 0 && videoHeight > 0) {
            frameSeen = true;
            if (preview != null) preview.setVisibility(INVISIBLE);
            Diary.note("comment media: first frame rendered");
        }
    }

    private static final class Blur {

        static void apply(View view, float radius) {
            view.setRenderEffect(
                radius <= 0
                    ? null
                    : android.graphics.RenderEffect.createBlurEffect(
                          radius,
                          radius,
                          android.graphics.Shader.TileMode.CLAMP
                      )
            );
        }
    }

    private static final class Gif {

        static Drawable decode(File file) throws java.io.IOException {
            Drawable decoded = android.graphics.ImageDecoder.decodeDrawable(
                android.graphics.ImageDecoder.createSource(file),
                (decoder, info, source) -> {
                    android.util.Size size = info.getSize();
                    float scale = Math.min(
                        1f,
                        DevicePerformance.previewEdge() /
                            (float) Math.max(size.getWidth(), size.getHeight())
                    );
                    decoder.setTargetSize(
                        Math.max(1, Math.round(size.getWidth() * scale)),
                        Math.max(1, Math.round(size.getHeight() * scale))
                    );
                }
            );
            if (decoded instanceof android.graphics.drawable.AnimatedImageDrawable) (
                (android.graphics.drawable.AnimatedImageDrawable) decoded
            ).setRepeatCount(android.graphics.drawable.AnimatedImageDrawable.REPEAT_INFINITE);
            return decoded;
        }
    }

    private static final class LegacyGif extends View {

        final Movie movie;
        final long began = android.os.SystemClock.uptimeMillis();
        boolean running;
        int mode;
        final Runnable frame = this::invalidate;

        LegacyGif(Context context, Movie movie) {
            super(context);
            this.movie = movie;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (running) movie.setTime(
                (int) ((android.os.SystemClock.uptimeMillis() - began) %
                    Math.max(1, movie.duration()))
            );
            float x = getWidth() / (float) movie.width();
            float y = getHeight() / (float) movie.height();
            float scale = mode == 1 ? Math.min(x, y) : Math.max(x, y);
            float sx = mode == 2 ? x : scale,
                sy = mode == 2 ? y : scale;
            canvas.save();
            canvas.translate(
                (getWidth() - movie.width() * sx) / 2f,
                (getHeight() - movie.height() * sy) / 2f
            );
            canvas.scale(sx, sy);
            movie.draw(canvas, 0, 0);
            canvas.restore();
            removeCallbacks(frame);
            if (running) postDelayed(frame, 42);
        }
    }
}
