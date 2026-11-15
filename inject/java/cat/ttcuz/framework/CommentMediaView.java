package cat.ttcuz.framework;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Movie;
import android.graphics.Outline;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.SystemClock;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import cat.ttcuz.framework.CommentMediaView;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

final class CommentMediaView extends FrameLayout implements TextureView.SurfaceTextureListener {
    private boolean active;
    private Drawable animation;
    private float appliedBlur;
    private int blur;
    private View blurPreview;
    private View blurTarget;
    private boolean broken;
    private final Runnable failed;
    private final File file;
    private boolean frameSeen;
    private int generation;
    private final boolean gif;
    private boolean loading;
    private boolean paused;
    private View picture;
    private MediaPlayer player;
    private ImageView preview;
    private int scaleMode;
    private int shade;
    private Surface surface;
    private TextureView texture;
    private int videoHeight;
    private int videoWidth;

    CommentMediaView(Context context, File file, boolean z, Runnable runnable) {
        super(context);
        this.appliedBlur = -1.0f;
        this.file = file;
        this.gif = z;
        this.failed = runnable;
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(2);
        setBackgroundColor(-15263459);
        setOutlineProvider(new ViewOutlineProvider() { // from class: cat.ttcuz.framework.CommentMediaView.1
            @Override // android.view.ViewOutlineProvider
            public void getOutline(View view, Outline outline) {
                // the sheet is rounded at the top only; rounding the bottom too left dark corners there
                float r = CommentMediaView.this.getResources().getDisplayMetrics().density * 16.0f;
                outline.setRoundRect(0, 0, view.getWidth(), Math.round(view.getHeight() + r), r);
            }
        });
        setClipToOutline(true);
        if (z) {
            return;
        }
        this.preview = new ImageView(context);
        openingFrame();
        CommentPoster.warm(file, this);
        TextureView textureView = new TextureView(context);
        this.texture = textureView;
        textureView.setOpaque(false);
        this.texture.setSurfaceTextureListener(this);
        addView(this.texture, new FrameLayout.LayoutParams(-1, -1));
        addView(this.preview, new FrameLayout.LayoutParams(-1, -1));
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getMode(i) == 1073741824 ? View.MeasureSpec.getSize(i) : 0, 1073741824), View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getMode(i2) == 1073741824 ? View.MeasureSpec.getSize(i2) : 0, 1073741824));
    }

    void scale(int i) {
        ImageView.ScaleType scaleType;
        ImageView.ScaleType scaleType2;
        int max = Math.max(0, Math.min(2, i));
        this.scaleMode = max;
        ImageView imageView = this.preview;
        if (imageView != null) {
            if (max == 2) {
                scaleType2 = ImageView.ScaleType.FIT_XY;
            } else if (max == 1) {
                scaleType2 = ImageView.ScaleType.FIT_CENTER;
            } else {
                scaleType2 = ImageView.ScaleType.CENTER_CROP;
            }
            imageView.setScaleType(scaleType2);
        }
        View view = this.picture;
        if (view instanceof ImageView) {
            ImageView imageView2 = (ImageView) view;
            int i2 = this.scaleMode;
            if (i2 == 2) {
                scaleType = ImageView.ScaleType.FIT_XY;
            } else if (i2 == 1) {
                scaleType = ImageView.ScaleType.FIT_CENTER;
            } else {
                scaleType = ImageView.ScaleType.CENTER_CROP;
            }
            imageView2.setScaleType(scaleType);
        }
        View view2 = this.picture;
        if (view2 instanceof LegacyGif) {
            ((LegacyGif) view2).mode = this.scaleMode;
            this.picture.invalidate();
        }
        crop();
    }

    void effects(int i, int i2) {
        int round = Math.round((i2 * 255) / 100.0f);
        if (this.blur == i && this.shade == round) {
            return;
        }
        this.blur = i;
        this.shade = round;
        applyBlur();
        crop();
        invalidate();
    }

    private void applyBlur() {
        View view = this.gif ? this.picture : this.texture;
        float f = this.blur * getResources().getDisplayMetrics().density;
        if (this.blurTarget == view && this.blurPreview == this.preview && this.appliedBlur == f) {
            return;
        }
        this.blurTarget = view;
        ImageView imageView = this.preview;
        this.blurPreview = imageView;
        this.appliedBlur = f;
        if (imageView != null && Build.VERSION.SDK_INT >= 31) {
            Blur.apply(this.preview, this.blur * getResources().getDisplayMetrics().density);
        }
        if (view == null || Build.VERSION.SDK_INT < 31) {
            return;
        }
        Blur.apply(view, this.blur * getResources().getDisplayMetrics().density);
    }

    void setPaused(boolean z) {
        this.paused = z;
        updateVisibility();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateVisibility() {
        TextureView textureView;
        boolean z = !this.paused && !this.broken && isAttachedToWindow() && isShown() && getWindowVisibility() == 0;
        if (z == this.active) {
            if (z && !this.gif && this.player == null && (textureView = this.texture) != null && textureView.isAvailable()) {
                removeCallbacks(starter);
                postDelayed(starter, 250L);
                return;
            }
            return;
        }
        this.active = z;
        if (z) {
            if (this.gif) {
                loadGif();
            } else {
                // a video starts only once its comment has stayed on screen for a moment: a fast scroll through
                // the comments made and threw away a player for every video it passed
                removeCallbacks(starter);
                postDelayed(starter, 250L);
            }
            Object obj = this.animation;
            if (obj instanceof Animatable) {
                ((Animatable) obj).start();
            }
            View view = this.picture;
            if (view instanceof LegacyGif) {
                ((LegacyGif) view).running = true;
            }
            View view2 = this.picture;
            if (view2 != null) {
                view2.invalidate();
                return;
            }
            return;
        }
        suspend();
    }

    /** Starts the video if its comment is still there and at least half of it shows. */
    private final Runnable starter = this::startIfShown;

    private void startIfShown() {
        if (!this.active || this.gif || this.player != null || this.texture == null || !this.texture.isAvailable()) return;
        android.graphics.Rect shown = new android.graphics.Rect();
        if (!getGlobalVisibleRect(shown) || shown.height() * 2 < getHeight()) { postDelayed(this.starter, 300L); return; }
        play();
    }

    /** At most two comment videos decode at once: the oldest gives way (its last frame stays). */
    private static final java.util.ArrayDeque<java.lang.ref.WeakReference<CommentMediaView>> running = new java.util.ArrayDeque<>();

    private void claim() {
        for (java.util.Iterator<java.lang.ref.WeakReference<CommentMediaView>> i = running.iterator(); i.hasNext(); ) {
            CommentMediaView v = i.next().get();
            if (v == null || v == this || v.player == null) i.remove();
        }
        while (running.size() >= 2) { CommentMediaView old = running.pollFirst().get(); if (old != null) old.suspend(true); }
        running.addLast(new java.lang.ref.WeakReference<>(this));
    }

    void posterReady() {
        openingFrame();
    }

    private void openingFrame() {
        Bitmap bitmap;
        int i = this.frameSeen ? 4 : 0;
        ImageView imageView = this.preview;
        if (imageView != null && imageView.getVisibility() != i) {
            this.preview.setVisibility(i);
        }
        ImageView imageView2 = this.preview;
        if (imageView2 == null || imageView2.getDrawable() != null || (bitmap = CommentPoster.get(this.file)) == null) {
            return;
        }
        this.preview.setImageBitmap(bitmap);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        openingFrame();
        post(new Runnable() { // from class: cat.ttcuz.framework.CommentMediaView$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                CommentMediaView.this.updateVisibility();
            }
        });
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        this.generation++;
        this.loading = false;
        removeCallbacks(starter);
        // scrolled away: no last frame to keep, and reading one back from the GPU cost every scroll
        suspend(false);
        super.onDetachedFromWindow();
    }

    @Override // android.view.View
    protected void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        updateVisibility();
    }

    @Override // android.view.View
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        updateVisibility();
    }

    @Override // android.view.View
    protected void onWindowVisibilityChanged(int i) {
        super.onWindowVisibilityChanged(i);
        updateVisibility();
    }

    void suspend() { suspend(true); }

    void suspend(boolean keepFrame) {
        removeCallbacks(starter);
        // Keep the last frame on screen: the texture loses its surface when the panel goes, and without this the
        // next open showed an empty panel until the video's first frame.
        try {
            if (keepFrame && this.frameSeen && this.texture != null && this.texture.isAvailable() && this.preview != null
                    && this.texture.getWidth() > 0 && this.texture.getHeight() > 0) {
                Bitmap last = this.texture.getBitmap(Math.max(1, this.texture.getWidth() / 2), Math.max(1, this.texture.getHeight() / 2));
                if (last != null) {
                    this.preview.setImageBitmap(last);
                    this.preview.setScaleType(ImageView.ScaleType.FIT_XY);   // the frame is already cropped as shown
                    this.preview.setVisibility(View.VISIBLE);
                    this.frameSeen = false;
                }
            }
        } catch (RuntimeException ignored) { }
        this.active = false;
        MediaPlayer mediaPlayer = this.player;
        this.player = null;
        if (mediaPlayer != null) {
            mediaPlayer.setOnPreparedListener(null);
            mediaPlayer.setOnErrorListener(null);
            mediaPlayer.release();
        }
        Surface surface = this.surface;
        if (surface != null) {
            CommentCadence.apply(this.texture, surface, 0.0f);
            this.surface.release();
            this.surface = null;
        }
        Object obj = this.animation;
        if (obj instanceof Animatable) {
            ((Animatable) obj).stop();
        }
        View view = this.picture;
        if (view instanceof LegacyGif) {
            ((LegacyGif) view).running = false;
            View view2 = this.picture;
            view2.removeCallbacks(((LegacyGif) view2).frame);
        }
    }

    private void error(Throwable th) {
        if (this.broken) {
            return;
        }
        this.broken = true;
        suspend();
        Diary.note("comment media: " + String.valueOf(th));
        post(new Runnable() { // from class: cat.ttcuz.framework.CommentMediaView$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                CommentMediaView.this.lambda$error$0();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$error$0() {
        this.failed.run();
        Screen.say(Text.t("Этот файл не воспроизводится на устройстве. Выберите другое видео или GIF."));
    }

    private void play() {
        if (this.active && this.player == null && this.texture.isAvailable()) {
            try {
                claim();
                final MediaPlayer mediaPlayer = new MediaPlayer();
                this.player = mediaPlayer;
                this.frameSeen = false;
                openingFrame();
                this.videoHeight = 0;
                this.videoWidth = 0;
                Surface surface = new Surface(this.texture.getSurfaceTexture());
                this.surface = surface;
                mediaPlayer.setSurface(surface);
                final Surface surface2 = this.surface;
                CommentCadence.request(this.file, new Consumer() { // from class: cat.ttcuz.framework.CommentMediaView$$ExternalSyntheticLambda3
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        CommentMediaView.this.lambda$play$2(surface2, mediaPlayer, (Float) obj);
                    }
                });
                mediaPlayer.setVolume(0.0f, 0.0f);
                mediaPlayer.setLooping(true);
                mediaPlayer.setDataSource(this.file.getPath());
                // some decoders only learn the size after the first frame; the crop follows it
                mediaPlayer.setOnVideoSizeChangedListener((player, w, h) -> {
                    if (this.player != player || w <= 0 || h <= 0) return;
                    int[] shown = displaySize(w, h);
                    if (shown[0] == this.videoWidth && shown[1] == this.videoHeight) return;
                    this.videoWidth = shown[0];
                    this.videoHeight = shown[1];
                    crop();
                });
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() { // from class: cat.ttcuz.framework.CommentMediaView$$ExternalSyntheticLambda4
                    @Override // android.media.MediaPlayer.OnPreparedListener
                    public final void onPrepared(MediaPlayer mediaPlayer2) {
                        CommentMediaView.this.lambda$play$3(mediaPlayer2);
                    }
                });
                mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() { // from class: cat.ttcuz.framework.CommentMediaView$$ExternalSyntheticLambda5
                    @Override // android.media.MediaPlayer.OnErrorListener
                    public final boolean onError(MediaPlayer mediaPlayer2, int i, int i2) {
                        boolean lambda$play$4;
                        lambda$play$4 = CommentMediaView.this.lambda$play$4(mediaPlayer2, i, i2);
                        return lambda$play$4;
                    }
                });
                mediaPlayer.prepareAsync();
            } catch (Exception | OutOfMemoryError e) {
                error(e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$play$2(final Surface surface, final MediaPlayer mediaPlayer, final Float f) {
        post(new Runnable() { // from class: cat.ttcuz.framework.CommentMediaView$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                CommentMediaView.this.lambda$play$1(surface, mediaPlayer, f);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$play$1(Surface surface, MediaPlayer mediaPlayer, Float f) {
        if (this.active && this.surface == surface && this.player == mediaPlayer) {
            CommentCadence.apply(this.texture, surface, f.floatValue());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$play$3(MediaPlayer mediaPlayer) {
        if (this.player == mediaPlayer && this.active) {
            int[] shown = displaySize(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight());
            this.videoWidth = shown[0];
            this.videoHeight = shown[1];
            crop();
            try {
                mediaPlayer.start();
            } catch (RuntimeException e) {
                error(e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$play$4(MediaPlayer mediaPlayer, int i, int i2) {
        if (this.player != mediaPlayer) {
            return true;
        }
        error(new IllegalStateException("decoder " + i + "/" + i2));
        return true;
    }

    private void loadGif() {
        if (this.picture != null || this.loading) {
            return;
        }
        this.loading = true;
        final int i = this.generation;
        Net.away("comment GIF", new Runnable() { // from class: cat.ttcuz.framework.CommentMediaView$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                CommentMediaView.this.lambda$loadGif$7(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadGif$7(final int i) {
        final Movie decodeFile;
        try {
            Drawable decoded = null;
            if (Build.VERSION.SDK_INT >= 28) {
                decoded = Gif.decode(this.file);
                decodeFile = null;
            } else {
                Movie movie = Movie.decodeFile(this.file.getPath());
                if (movie == null) {
                    // a still picture before Android 9: decoded at about the screen's size
                    android.graphics.BitmapFactory.Options look = new android.graphics.BitmapFactory.Options();
                    look.inJustDecodeBounds = true;
                    android.graphics.BitmapFactory.decodeFile(this.file.getPath(), look);
                    int edge = Math.max(look.outWidth, look.outHeight), want = Math.max(1, Math.max(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels));
                    android.graphics.BitmapFactory.Options read = new android.graphics.BitmapFactory.Options();
                    read.inSampleSize = 1;
                    while (edge / (read.inSampleSize * 2) >= want) read.inSampleSize *= 2;
                    Bitmap still = android.graphics.BitmapFactory.decodeFile(this.file.getPath(), read);
                    if (still == null) throw new IllegalArgumentException("invalid picture");
                    decoded = new android.graphics.drawable.BitmapDrawable(getResources(), still);
                }
                decodeFile = movie;
            }
            final Drawable drawable = decoded;
            post(new Runnable() { // from class: cat.ttcuz.framework.CommentMediaView$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    CommentMediaView.this.lambda$loadGif$5(i, drawable, decodeFile);
                }
            });
        } catch (Exception | OutOfMemoryError e) {
            post(new Runnable() { // from class: cat.ttcuz.framework.CommentMediaView$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    CommentMediaView.this.lambda$loadGif$6(i, e);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    public /* synthetic */ void lambda$loadGif$5(int i, Drawable drawable, Movie movie) {
        if (i != this.generation) {
            return;
        }
        this.loading = false;
        if (isAttachedToWindow()) {
            this.animation = drawable;
            if (drawable != null) {
                ImageView imageView = new ImageView(getContext());
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setImageDrawable(drawable);
                this.picture = imageView;
            } else {
                this.picture = new LegacyGif(getContext(), movie);
            }
            addView(this.picture, new FrameLayout.LayoutParams(-1, -1));
            scale(this.scaleMode);
            applyBlur();
            if (this.active && (drawable instanceof Animatable)) {
                ((Animatable) drawable).start();
            }
            View view = this.picture;
            if (view instanceof LegacyGif) {
                ((LegacyGif) view).running = this.active;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadGif$6(int i, Throwable th) {
        if (i != this.generation) {
            return;
        }
        this.loading = false;
        error(th);
    }

    private int[] knownSize;

    /**
     * The video's size as it is shown, with its rotation applied. Phones record portrait video as landscape
     * frames marked "rotate 90"; some players report the frame size, and cropping a portrait panel to a
     * landscape size letterboxed the video with black stripes once it replaced the still frame.
     */
    private int[] displaySize(int playerWidth, int playerHeight) {
        if (knownSize == null) {
            android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
            try {
                retriever.setDataSource(this.file.getPath());
                int w = Integer.parseInt(retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                int h = Integer.parseInt(retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                String turn = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                int rotation = turn == null ? 0 : Integer.parseInt(turn);
                knownSize = rotation % 180 != 0 ? new int[]{h, w} : new int[]{w, h};
            } catch (Throwable error) {
                knownSize = new int[]{0, 0};
            } finally {
                try { retriever.release(); } catch (Throwable ignored) { }
            }
        }
        if (knownSize[0] > 0 && knownSize[1] > 0) return knownSize;
        return new int[]{playerWidth, playerHeight};
    }

    private void crop() {
        if (this.texture == null || this.videoWidth <= 0 || this.videoHeight <= 0 || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        float width = getWidth() / this.videoWidth;
        float height = getHeight() / this.videoHeight;
        float min = this.scaleMode == 1 ? Math.min(width, height) : Math.max(width, height);
        int i = this.scaleMode;
        if (i != 2) {
            width = min;
        }
        if (i != 2) {
            height = min;
        }
        // a blurred picture is drawn a little larger, so its soft edge falls outside the panel
        float bleed = 1.0f + 2.0f * this.blur * getResources().getDisplayMetrics().density / Math.max(1, Math.min(getWidth(), getHeight()));
        Matrix matrix = new Matrix();
        matrix.setScale((this.videoWidth * width) / getWidth() * bleed, (this.videoHeight * height) / getHeight() * bleed, getWidth() / 2.0f, getHeight() / 2.0f);
        this.texture.setTransform(matrix);
        if (this.preview != null) { this.preview.setScaleX(bleed); this.preview.setScaleY(bleed); }
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        crop();
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        canvas.drawColor(Color.argb(this.shade, 0, 0, 0));
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
        updateVisibility();
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
        crop();
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        suspend();
        return true;
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        if (this.frameSeen || this.player == null || this.videoWidth <= 0 || this.videoHeight <= 0) {
            return;
        }
        this.frameSeen = true;
        ImageView imageView = this.preview;
        if (imageView != null) {
            imageView.setVisibility(4);
        }
        Diary.note("comment media: first frame rendered");
    }

    private static final class Blur {
        private Blur() {
        }

        static void apply(View view, float f) {
            // MIRROR: the blur reaches past the edge into a reflection of the picture, not into black
            view.setRenderEffect(f <= 0.0f ? null : RenderEffect.createBlurEffect(f, f, Shader.TileMode.MIRROR));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class Gif {
        private Gif() {
        }

        static Drawable decode(File file) throws IOException {
            Drawable decodeDrawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(file), new ImageDecoder.OnHeaderDecodedListener() { // from class: cat.ttcuz.framework.CommentMediaView$Gif$$ExternalSyntheticLambda0
                @Override // android.graphics.ImageDecoder.OnHeaderDecodedListener
                public final void onHeaderDecoded(ImageDecoder imageDecoder, ImageDecoder.ImageInfo imageInfo, ImageDecoder.Source source) {
                    CommentMediaView.Gif.lambda$decode$0(imageDecoder, imageInfo, source);
                }
            });
            if (decodeDrawable instanceof AnimatedImageDrawable) {
                ((AnimatedImageDrawable) decodeDrawable).setRepeatCount(-1);
            }
            return decodeDrawable;
        }

        static /* synthetic */ void lambda$decode$0(ImageDecoder imageDecoder, ImageDecoder.ImageInfo imageInfo, ImageDecoder.Source source) {
            Size size = imageInfo.getSize();
            float min = Math.min(1.0f, DevicePerformance.previewEdge() / Math.max(size.getWidth(), size.getHeight()));
            imageDecoder.setTargetSize(Math.max(1, Math.round(size.getWidth() * min)), Math.max(1, Math.round(size.getHeight() * min)));
        }
    }

    private static final class LegacyGif extends View {
        final long began;
        final Runnable frame;
        int mode;
        final Movie movie;
        boolean running;

        LegacyGif(Context context, Movie movie) {
            super(context);
            this.began = SystemClock.uptimeMillis();
            this.frame = new Runnable() { // from class: cat.ttcuz.framework.CommentMediaView$LegacyGif$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    CommentMediaView.LegacyGif.this.invalidate();
                }
            };
            this.movie = movie;
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            if (this.running) {
                this.movie.setTime((int) ((SystemClock.uptimeMillis() - this.began) % Math.max(1, this.movie.duration())));
            }
            float width = getWidth() / this.movie.width();
            float height = getHeight() / this.movie.height();
            float min = this.mode == 1 ? Math.min(width, height) : Math.max(width, height);
            int i = this.mode;
            if (i != 2) {
                width = min;
            }
            if (i != 2) {
                height = min;
            }
            canvas.save();
            canvas.translate((getWidth() - (this.movie.width() * width)) / 2.0f, (getHeight() - (this.movie.height() * height)) / 2.0f);
            canvas.scale(width, height);
            this.movie.draw(canvas, 0.0f, 0.0f);
            canvas.restore();
            removeCallbacks(this.frame);
            if (this.running) {
                postDelayed(this.frame, 42L);
            }
        }
    }
}
