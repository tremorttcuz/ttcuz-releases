package cat.ttcuz.framework;

import android.app.Activity;
import android.animation.ValueAnimator;
import android.animation.LayoutTransition;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

/**
 * ttcuz's own screen, drawn in TikTok's settings language.
 *
 * Page, cards, title, grey section labels: the shapes are TikTok's, and so are
 * the colours -- not copied out of a screenshot but the ones the row measured
 * off the real settings screen and kept. Opened from the launcher with TikTok
 * never having been on screen, it falls back to plain black or white.
 *
 * Every view is built here in code. Adding a layout or a style would mean
 * adding resources, and adding resources means rewriting a 25 MB resource
 * table -- the one thing this build refuses to do.
 */
public class SettingsActivity extends Activity {
    void accountAppearanceChanged(){skin=Skin.remembered(this);rebuild();}

    private Skin skin;
    private FrameLayout root;
    private SettingsSnow snow;
    private final java.util.Map<String,View> settingTargets=new java.util.HashMap<>();
    private LinearLayout column;
    private View restartBar;
    private Scrim topScrim, bottomScrim;
    private RestartSymbol restartSymbol;
    private TextView restartWhy, restartAction;
    private boolean rebuildQueued;
    private boolean badgePlacementOpen,gradientFineOpen;
    private String disclosureKey,renderDisclosureKey;
    private boolean collapsing;
    private final java.util.Map<String,java.util.List<DisclosureBody>> disclosureBodies=new java.util.HashMap<>();
    private final java.util.Map<String,Chevron> disclosureArrows=new java.util.HashMap<>();
    private boolean revealAnimationRequested;
    private boolean revealAnimationUsed;
    private Runnable scrollRestore;
    private android.view.ViewTreeObserver.OnPreDrawListener scrollRestoreListener;
    private int requestedScrollY;
    private final Runnable delayedRebuild = new Runnable() {
        @Override
        public void run() {
            rebuildQueued = false;
            rebuild();
        }
    };

    /**
     * Whether a setting was changed in this process.
     *
     * Static, because the bar is about the process and not about the screen:
     * leaving the settings and coming back does not un-change what was changed,
     * and only a restart -- which is a new process, where this is false again --
     * does.
     */
    private static boolean pending;

    /** Where the mod, its people and its money live. */
    private static final String FORUM = "https://t.me/margeletforum";
    private static final String OWNER_TELEGRAM = "https://t.me/narezany";
    private static final String OWNER_TIKTOK =
            "https://tiktok.com/@narezany?_r=1&_t=ZT-99hPDJ26hji_";
    private static final String TTCUZ_SITE = Server.ROOT + "/";

    private boolean countriesOpen;
    private boolean accentOpen;
    private Dot accentCustomSwatch;
    private boolean textOpen;
    private boolean backgroundOpen;
    private boolean themeColoursOpen;
    private boolean fontOpen;
    private ScrollView page;
    private boolean thanksOpen;
    private int settingsGroup;
    /** The page is rebuilt while typing; the new search field takes the cursor back. */
    private boolean refocusSearch;
    /** The search field is open (the magnifier at the top was tapped); it closes with the magnifier or "back". */
    private boolean searchOpen;
    /** The home tiles are being arranged (the pencil at the top): they move under the finger, resize by their corner. */
    private boolean editingTiles;
    /** The next selected chip plays its morph (set when a chip is tapped). */
    /** Index of the first section in `column`: everything before it is the head of the page. */
    private int filterFrom = 3;
    private boolean mineOpen;
    private java.util.List<Mine.Held> ordering;
    private boolean streakOpen;
    private boolean diaryOpen;
    /** The code was copied: check the bio on its own when the person comes back. */
    private boolean verifyWaiting;
    private static final int PICK_CATALOG_BADGE = 0x4D63;
    private final Runnable badgeUpdates = () -> {
        if (!isFinishing() && !isDestroyed()) rebuild();
    };
    private Panel adminPanel;
    private boolean lastAdmin;
    private TextView syncTitle,syncDetails,syncRetry;
    private android.widget.ProgressBar syncSpinner;
    private final Runnable liveUiUpdates=new Runnable(){
        @Override public void run(){
            if(isFinishing() || isDestroyed() || root==null)return;
            boolean admin=AdminAccess.current();
            if(admin!=lastAdmin){
                if(!admin && adminPanel!=null){adminPanel.close();adminPanel=null;}
                rebuild();
            }
            updateSyncUi();root.postDelayed(this,600L);
        }
    };
    private String searchQuery = "";
    private final java.util.Map<String, View> sectionTargets =
            new java.util.HashMap<String, View>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Unsaved.look(this);
        Ttcuz.attach(this);
        ProfileStyle.recallTag(getApplicationContext());
        skin = Skin.tonal(this, Accent.colour());
        dressTheWindow();
        // the list may still be the one cached before this screen existed, so
        // redraw when the server answers rather than waiting for a tap
        Badges.tell(badgeUpdates);

        ScrollView scroll = page = new ScrollView(this);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        scroll.setFillViewport(true);

        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(0, statusBar(), 0, dp(32));
        scroll.addView(column, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root=new FrameLayout(this);
        root.setBackgroundColor(skin.page);
        snow = new SettingsSnow(this);
        root.addView(snow, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // Under the clock and the gesture pill the list used to run straight through them.
        topScrim = new Scrim(this, true);
        bottomScrim = new Scrim(this, false);
        root.addView(topScrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(24), Gravity.TOP));
        root.addView(bottomScrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(24), Gravity.BOTTOM));

        setContentView(root);
        rebuild();
        if(getIntent().getBooleanExtra("ttcuz_tutorial",false))root.postDelayed(()->Tutorial.show(this),350L);
    }

    /** The tour shows, behind itself, the menu group each step is about. */
    void tutorialStep(int step) {
        int group=step>=0&&step<Text.TUTORIAL_GROUPS.length?Text.TUTORIAL_GROUPS[step]:0;
        if(group==settingsGroup&&searchQuery.isEmpty())return;
        settingsGroup=group;searchQuery="";rebuild();
        page.post(()->page.smoothScrollTo(0,0));
    }

    /** The settings screen in front, for a plugin's refreshModScreen(). */
    private static java.lang.ref.WeakReference<SettingsActivity> live;

    static void refreshFromPlugin() {
        final SettingsActivity screen = live == null ? null : live.get();
        if (screen == null || screen.isFinishing() || screen.column == null) return;
        screen.runOnUiThread(() -> { if (!PluginEngine.drawingModScreen()) screen.rebuild(); });
    }

    @Override
    protected void onResume() {
        super.onResume();
        live = new java.lang.ref.WeakReference<>(this);
        boolean captureFrames=getSharedPreferences(Ttcuz.PREFS,MODE_PRIVATE)
                .getBoolean("lag_frame_stats",false);
        Lag.setFrameCaptureEnabled(captureFrames);
        if(captureFrames)Lag.frames(this);
        if (snow != null) snow.setActive(true);
        if(Streaks.isEnabled()){StreakAlarm.schedule(this,false);Streaks.round(this);}
        if(root!=null && column!=null)rebuild();
        if(root!=null){root.removeCallbacks(liveUiUpdates);root.post(liveUiUpdates);}
        if (verifyWaiting) {
            verifyWaiting = false;
            final String uid = Account.id();
            if (uid != null && !new CloudProfileProvider(this).verified(uid)) {
                Toast.makeText(this, Text.t("Проверяю профиль…"), Toast.LENGTH_SHORT).show();
                TtcuzProfileSync.autoVerifyOwn(true);
                if (page != null) page.postDelayed(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, TtcuzProfileSync.status(), Toast.LENGTH_LONG).show();
                    rebuild();
                }, 5000L);
            }
        }
    }

    @Override protected void onPause(){
        if (snow != null) snow.setActive(false);
        if(root!=null)root.removeCallbacks(liveUiUpdates);
        navigating=false;
        if(column!=null){column.animate().withEndAction(null).cancel();column.setAlpha(1f);column.setTranslationX(0f);}
        super.onPause();
    }

    private void dressTheWindow() {
        try {
            getWindow().setStatusBarColor(skin.page);
            getWindow().setNavigationBarColor(skin.page);
            if (!skin.dark()) {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        } catch (Throwable ignored) {
        }
    }

    // --------------------------------------------------------- the screen

    private void rebuild() {
        // surfaces follow the accent, so a new accent tints the whole screen at once
        skin = Skin.tonal(this, Accent.colour());
        if (root != null) root.setBackgroundColor(skin.page);
        fitScrims();
        dressTheWindow();
        navigating=false;lastAdmin=AdminAccess.current();syncTitle=syncDetails=syncRetry=null;syncSpinner=null;
        homeTiles.clear();
        collapsing=false;
        column.animate().withEndAction(null).cancel(); column.setAlpha(1f); column.setTranslationX(0f);
        revealAnimationUsed = false;
        boolean animateReveal = revealAnimationRequested;
        revealAnimationRequested = false;
        revealAnimationUsed = !animateReveal;
        final int previousScroll = page == null ? 0 : page.getScrollY();
        sectionTargets.clear();settingTargets.clear();
        disclosureBodies.clear();disclosureArrows.clear();renderDisclosureKey=null;
        pinHolder = null; pinEnd = null;
        column.removeAllViews();
        column.addView(backArrow());
        column.addView(title(settingsGroup == 0 ? "ttcuz" : groupName(settingsGroup)));
        // While searching, the whole page is built and then cut down to the rows that match:
        // every setting is found, not only the ones somebody listed by hand, and what is
        // found is the real row, so a switch or a slider works right in the results.
        final boolean searching = searchQuery.length() > 0;
        if((searchOpen && settingsGroup==0) || searching)column.addView(searchBox());

        if (!searching && settingsGroup == 0) {
            // the own profile first: people looked for the badge and the nickname and did not find them;
            // then what is changed every day, then the rest
            // the hero card is gone; a waiting update still shows here, and only then
            if (Updater.newer()) column.addView(wrap(actionRow("download", Text.UPDATE_THERE_IS + " " + Updater.latest(), null, () -> Updater.check(this, true))));
            column.addView(wrap(myProfileCard()));
            column.addView(gap(8));
            column.addView(shelfTiles(homeTileOrder()));
            column.addView(gap(8));
            column.addView(versions());
            showRestartBar();
            restoreScroll(0);
            return;
        }

        filterFrom = column.getChildCount();
        column.addView(section(Text.FEED));
        LinearLayout feed = card();
        feed.addView(toggleRow("block", Text.HIDE_ADS, Feed.isEnabled(), Feed::setEnabled));
        feed.addView(line());
        // advertising TikTok does not call an ad (casinos, case drops, betting) is always hidden, skipped quietly and marked
        feed.addView(actionRow("edit",Text.t("Слова рекламы"),Text.t("Свои слова и встроенный список"),this::editAdWords));
        feed.addView(line());
        feed.addView(toggleRow("visibility_off", Text.HIDE_LIVE, Feed.hides(Feed.KEY_LIVE),
                on -> Feed.setHides(Feed.KEY_LIVE, on)));
        feed.addView(line());
        feed.addView(toggleRow("image", Text.HIDE_PHOTOS, Feed.hides(Feed.KEY_PHOTOS),
                on -> Feed.setHides(Feed.KEY_PHOTOS, on)));
        feed.addView(line());
        feed.addView(liveToggleRow("visibility_off",Text.HIDE_STORIES,Feed.hides(Feed.KEY_STORIES),on->Feed.setHides(Feed.KEY_STORIES,on)));
        feed.addView(liveToggleRow("visibility_off",Text.HIDE_SUGGESTIONS,Feed.hides(Feed.KEY_SUGGESTIONS),on->Feed.setHides(Feed.KEY_SUGGESTIONS,on)));
        feed.addView(line());
        // how one video turns into the next, like a launcher's scroll effects
        feed.addView(actionRow("swap_vert",Text.t("Эффект прокрутки"),FeedScroll.name(),this::chooseScrollEffect));
        if (FeedScroll.effect() != FeedScroll.NONE || FeedScroll.blur())
            feed.addView(slider("tune",Text.t("Сила эффекта"),FeedScroll.strength(),100,0,"%",FeedScroll::setStrength));
        feed.addView(liveToggleRow("animation",Text.t("Глубина при прокрутке"),FeedScroll.depth(),on->{FeedScroll.setDepth(on);rebuild();}));
        if (Blur.gpu())
            feed.addView(only64(liveToggleRow("blur_on",Text.t("Размытие при прокрутке"),FeedScroll.blur(),on->{FeedScroll.setBlur(on);rebuild();})));
        column.addView(wrap(feed));

        column.addView(section(Text.VIDEO));
        LinearLayout video = card();
        video.addView(actionRow("speed",Text.PLAYER_SPEED,Float.toString(PlayerTools.speed())+"×",()->PlayerTools.choose(this,this::rebuild)));
        video.addView(toggleRow("volume_up", Text.SOUND, Sound.isEnabled(), Sound::setEnabled));
        video.addView(line());
        video.addView(liveToggleRow("timeline", Text.SEEKBAR, Seekbar.isEnabled(),
                Seekbar::setEnabled));
        video.addView(line());
        video.addView(liveToggleRow("info", Text.ALWAYS_DATE, Dates.isEnabled(),
                Dates::setEnabled));
        video.addView(liveToggleRow("contrast",Text.t("Не давать TikTok менять яркость"),Brightness.isEnabled(),Brightness::setEnabled));
        video.addView(liveToggleRow("history",Text.t("Продолжать видео с того же места"),VideoResume.on(),VideoResume::set));
        video.addView(line());
        video.addView(liveToggleRow("vibration",Text.t("Вибрация в ритм музыки"),BeatHaptics.on(),this::setBeat));
        if (BeatHaptics.on()) {
            video.addView(actionRow("music_note",Text.t("Вибрировать на"),Text.t(BeatHaptics.MODES[BeatHaptics.mode()]),()->
                chooseLive(Text.t("Вибрировать на"),BeatHaptics.MODES,BeatHaptics.mode(),BeatHaptics::setMode)));
            video.addView(actionRow("tune",Text.t("Сила вибрации"),Text.t(BeatHaptics.POWERS[BeatHaptics.power()]),()->
                chooseLive(Text.t("Сила вибрации"),BeatHaptics.POWERS,BeatHaptics.power(),BeatHaptics::setPower)));
        }
        column.addView(wrap(video));

        column.addView(section(FEED_BUTTONS));
        LinearLayout buttons = card();
        buttons.addView(liveToggleRow("person",Text.t("Скрыть кнопку «Подписаться»"),FeedButtons.on(FeedButtons.HIDE_FOLLOW),on->FeedButtons.set(FeedButtons.HIDE_FOLLOW,on)));
        buttons.addView(liveToggleRow("add",Text.t("Подписка двойным нажатием"),FeedButtons.on(FeedButtons.DOUBLE_FOLLOW),on->FeedButtons.set(FeedButtons.DOUBLE_FOLLOW,on)));
        buttons.addView(liveToggleRow("favorite_border",Text.t("Без репоста по удержанию лайка"),FeedButtons.on(FeedButtons.NO_LIKE_HOLD),on->FeedButtons.set(FeedButtons.NO_LIKE_HOLD,on)));
        buttons.addView(liveToggleRow("chat",Text.t("Без эмодзи по удержанию комментариев"),FeedButtons.on(FeedButtons.NO_COMMENT_HOLD),on->FeedButtons.set(FeedButtons.NO_COMMENT_HOLD,on)));
        buttons.addView(liveToggleRow("star",Text.t("Без коллекций по удержанию избранного"),FeedButtons.on(FeedButtons.NO_FAV_HOLD),on->FeedButtons.set(FeedButtons.NO_FAV_HOLD,on)));
        buttons.addView(liveToggleRow("account_circle",Text.t("Скрыть кольцо историй на аватаре"),FeedButtons.on(FeedButtons.NO_STORY_RING),on->FeedButtons.set(FeedButtons.NO_STORY_RING,on)));
        String[] railNames={Text.RAIL_LIKE,Text.RAIL_COMMENT,Text.RAIL_FAV,Text.RAIL_SHARE};
        String[] railIcons={"favorite_border","chat","star","share"};
        for(int i=0;i<railNames.length;i++){
            final int kind=i;
            buttons.addView(liveToggleRow(railIcons[i],railNames[i],FeedRail.shown(i),on->FeedRail.setShown(kind,on)));
        }
        column.addView(wrap(buttons));

        column.addView(section(WATCHED));
        LinearLayout watched = card();
        watched.addView(liveToggleRow("visibility_off",Text.t("Скрывать просмотренные видео"),WatchedVideos.enabled(),on->{WatchedVideos.setEnabled(on);rebuildAfterSwitchAnimation();}));
        if (WatchedVideos.enabled()) {
            final String[] keep={Text.t("Навсегда"),Text.t("1 день"),Text.t("7 дней"),Text.t("30 дней")};
            final String[] after={Text.t("Сразу"),Text.t("3 секунды"),Text.t("5 секунд"),Text.t("10 секунд")};
            watched.addView(actionRow("history",Text.t("Запоминать"),keep[indexOf(WatchedVideos.KEEP_DAYS,WatchedVideos.keepDays())],()->
                choose(Text.t("Запоминать"),keep,indexOf(WatchedVideos.KEEP_DAYS,WatchedVideos.keepDays()),i->WatchedVideos.setKeepDays(WatchedVideos.KEEP_DAYS[i]))));
            watched.addView(actionRow("schedule",Text.t("Считать просмотренным"),after[indexOf(WatchedVideos.AFTER_SECONDS,WatchedVideos.afterSeconds())],()->
                choose(Text.t("Считать просмотренным"),after,indexOf(WatchedVideos.AFTER_SECONDS,WatchedVideos.afterSeconds()),i->WatchedVideos.setAfterSeconds(WatchedVideos.AFTER_SECONDS[i]))));
            watched.addView(actionRow("restore",Text.t("Забыть просмотренные"),String.valueOf(WatchedVideos.size()),()->{WatchedVideos.forget();Screen.say(Text.t("Готово"));rebuild();}));
        }
        column.addView(wrap(watched));


        column.addView(section(Text.DIM_SECTION));
        LinearLayout burn = card();
        burn.addView(liveToggleRow("visibility_off", Text.DIM, Dim.isConfigured(), on -> {
            Dim.setEnabled(on);
            rebuildAfterSwitchAnimation();
        }));
        if (Dim.isConfigured()) {
            burn.addView(line());
            burn.addView(liveSlider("contrast", Text.DIM_HOW, Dim.strength(), 90, 0, "%", value -> Dim.setStrength(value)));
        }
        column.addView(wrap(burn));

        column.addView(section(COMMENTS_SECTION));
        LinearLayout commentRows = card();
        commentRows.addView(liveToggleRow("thumb_up",Text.t("Лайк двойным нажатием"),CommentLike.enabled(),CommentLike::set));
        commentRows.addView(liveToggleRow("schedule",Text.t("Точное время комментариев"),ExactTime.on(ExactTime.COMMENTS),on->ExactTime.set(ExactTime.COMMENTS,on)));
        commentRows.addView(liveToggleRow("schedule",Text.t("Точное время репостов"),ExactTime.on(ExactTime.REPOSTS),on->ExactTime.set(ExactTime.REPOSTS,on)));
        commentRows.addView(liveToggleRow("timeline",Text.t("Результаты опроса без голосования"),PollPeek.enabled(),PollPeek::set));
        commentRows.addView(liveToggleRow("content_paste",Text.t("Копировать без имени автора"),CommentClipboard.enabled(),CommentClipboard::set));
        // the sheet over the video: TikTok shrinks the video upward for it; this keeps it whole under the sheet
        commentRows.addView(liveToggleRow("aspect_ratio",Text.t("Видео не уменьшается при комментариях"),CommentLook.keepVideo(),CommentLook::setKeepVideo));
        commentRows.addView(slider("blur_on",Text.t("Прозрачность комментариев"),CommentLook.transparency(),90,0,"%",CommentLook::setTransparency));
        column.addView(wrap(commentRows));

        column.addView(section(Text.INPUT_LIMITS));
        LinearLayout limits = card();
        limits.addView(liveToggleRow("article",Text.UNLIMITED_COMMENTS,Flags.isOn(TextLimits.COMMENTS),on->Flags.set(TextLimits.COMMENTS,on)));
        limits.addView(line());
        limits.addView(liveToggleRow("share",Text.UNLIMITED_REPOSTS,Flags.isOn(TextLimits.REPOSTS),on->Flags.set(TextLimits.REPOSTS,on)));
        limits.addView(line());
        limits.addView(liveToggleRow("tag",Text.UNLIMITED_HASHTAGS,Flags.isOn(TextLimits.HASHTAGS),on->Flags.set(TextLimits.HASHTAGS,on)));
        column.addView(wrap(limits));

        column.addView(section(Text.REGION));
        LinearLayout head = card();
        head.addView(switchRow());
        column.addView(wrap(head));

        column.addView(section(Text.COUNTRY));
        LinearLayout countries = card();
        countries.addView(countryHead());
        if (countriesOpen) {
            countries.addView(line());
            for (String[] country : Ttcuz.COUNTRIES) {
                countries.addView(appear(countryRow(country)));
            }
        }
        column.addView(wrap(countries));

        column.addView(section(Text.ACCENT));
        LinearLayout accent = card();
        accent.addView(stylePreview());
        // the colours are always laid out: choosing the accent is what this section is for
        accent.addView(accentHead());
        accent.addView(palette());
        // a second colour the accent flows into, on text, icons, likes and surfaces
        accent.addView(liveToggleRow("palette",Text.t("Градиент акцента"),AccentGradient.enabled(),on->{AccentGradient.set(on,AccentGradient.colour(),AccentGradient.mix());rebuildAfterSwitchAnimation();}));
        if (AccentGradient.enabled()) {
            accent.addView(actionRow(gradientOpen ? "expand_less" : "expand_more", Text.t("Настройки градиента"),
                    gradientOpen ? Text.t("Свернуть") : Text.t("Цвет, смешивание, угол и ширина"), () -> { gradientOpen = !gradientOpen; rebuild(); }));
        }
        if (AccentGradient.enabled() && gradientOpen) {
            accent.addView(actionRow("format_color_fill",Text.t("Второй цвет"),String.format("#%06X",AccentGradient.colour()&0xFFFFFF),()->
                showProfileColourPicker(Text.t("Второй цвет"),AccentGradient.colour(),c->AccentGradient.set(true,c,AccentGradient.mix()))));
            accent.addView(slider("tune",Text.t("Смешивание"),AccentGradient.mix(),100,0,"%",v->AccentGradient.set(true,AccentGradient.colour(),v)));
            accent.addView(slider("swap_horiz",Text.t("Угол"),AccentGradient.angle(),359,0,"°",v->AccentGradient.set(true,AccentGradient.colour(),AccentGradient.mix(),v,AccentGradient.width())));
            accent.addView(slider("open_with",Text.t("Ширина"),AccentGradient.width()-10,190,10,"%",v->AccentGradient.set(true,AccentGradient.colour(),AccentGradient.mix(),AccentGradient.angle(),v+10)));
        }
        column.addView(wrap(accent));

        column.addView(section(Text.THEME));
        LinearLayout theme = card();
        theme.addView(toggleRow("contrast", Text.THEME_ON, Themes.isEnabled(), on -> {
            Themes.setEnabled(on);
            rebuildAfterSwitchAnimation();
        }));
        if (Themes.isEnabled()) {
            theme.addView(line());
            theme.addView(toggleRow("wallpaper", Text.THEME_MATERIAL,
                    Themes.isMaterial(), on -> {
                        Themes.setMaterial(on);
                        rebuildAfterSwitchAnimation();
                    }));
            if (!Themes.isMaterial()) {
                theme.addView(line());
                theme.addView(shadeHead(true));
                if (textOpen) {
                    theme.addView(line());
                    theme.addView(appear(shades(true)));
                }
                theme.addView(line());
                theme.addView(shadeHead(false));
                if (backgroundOpen) {
                    theme.addView(line());
                    theme.addView(appear(shades(false)));
                }
                theme.addView(line());
                theme.addView(strengthRow());
            }
            theme.addView(line());
        }
        // the colour of the theme in one tap, as the accent is chosen: deep for the dark theme, pastel for the light one
        theme.addView(line());
        theme.addView(themePalette());
        theme.addView(line());
        theme.addView(actionRow("palette", Text.THEME_PRESETS, Text.THEME_PRESETS_NOTE,
                this::chooseThemePreset));
        theme.addView(line());
        theme.addView(themeColoursHead());
        if (themeColoursOpen) {
            theme.addView(line());
            for (String role : AppearanceColors.ROLES) {
                theme.addView(appearanceColourRow(role));
                theme.addView(line());
            }
        }
        column.addView(wrap(theme));

        column.addView(section(Text.GLASS_SECTION));
        LinearLayout glass = card();
        // Off: TikTok's own bottom bar, untouched. On: the MD3 island with Google icons and the Sunny create button.
        glass.addView(liveToggleRow("view_compact",Text.NAV_ISLAND,NavigationIsland.isEnabled(),on->{NavigationIsland.setEnabled(on);rebuildAfterSwitchAnimation();}));
        if (NavigationIsland.isEnabled()) {
            // the island as it is set up: a switch under each button, hold one to move it
            IslandEditor editor = new IslandEditor(this, skin);
            islandEditor = new java.lang.ref.WeakReference<>(editor);
            glass.addView(editor, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            if (IslandTabs.custom())
                glass.addView(actionRow("restore", Text.t("Кнопки как в TikTok"), null, () -> { IslandTabs.reset(); rebuild(); }));
        }
        if (NavigationIsland.isEnabled())
            glass.addView(liveToggleRow("text_fields",Text.t("Скрыть названия вкладок"),NavigationIsland.hideLabels(),on->{NavigationIsland.setHideLabels(on);rebuildAfterSwitchAnimation();}));
        if (NavigationIsland.isEnabled())
            glass.addView(only64(liveToggleRow("speed",Text.t("Островок 120 кадров/с"),NavigationIsland.fast(),NavigationIsland::setFast)));
        if (NavigationIsland.isEnabled())
            glass.addView(actionRow("home",Text.t("Иконки островка"),Text.t(IslandMotion.ICON_NAMES[IslandMotion.tiktokIcons()?1:0]),()->
                chooseLive(Text.t("Иконки островка"),IslandMotion.ICON_NAMES,IslandMotion.tiktokIcons()?1:0,i->IslandMotion.setTiktokIcons(i==1))));
        if (NavigationIsland.isEnabled())
            // islands people write themselves: .island files the mod draws (docs/islands.md)
            glass.addView(actionRow("palette",Text.t("Свои островки"),islandDescription(),this::islandMenu));
        glass.addView(glassChoice());
        final String[] tints={Text.GLASS_TINT_AUTO,Text.GLASS_TINT_DARK,Text.GLASS_TINT_LIGHT};
        glass.addView(actionRow("contrast",Text.GLASS_TINT,tints[Blur.tint()],()->
            new ModDialog.Builder(this).setItems(tints,(dialog,index)->{Blur.setTint(index);rebuild();}).show()));
        glass.addView(liveToggleRow("aspect_ratio",Text.VIDEO_CORNERS,VideoCorners.isEnabled(),VideoCorners::setEnabled));
        column.addView(wrap(glass));

        column.addView(section(Text.APPEARANCE));
        LinearLayout icons = card();
        icons.addView(iconHead());
        icons.addView(line());
        icons.addView(iconChoices());
        icons.addView(line());
        icons.addView(actionRow("account_circle",Text.t("Форма аватарок"),AvatarShape.nameOf(AvatarShape.chosen()),this::chooseAvatarShape));
        icons.addView(actionRow("animation",Text.t("Индикатор загрузки"),loaderName(),this::chooseLoader));
        icons.addView(actionRow("refresh",Text.t("Частота экрана"),Rate.wanted()==0?Text.t("Авто"):Rate.name()+Text.t(" Гц"),this::chooseRefreshRate));
        icons.addView(actionRow("tag",Text.t("Числа"),NumberStyle.names()[NumberStyle.style()],()->
            choose(Text.t("Числа"),NumberStyle.names(),NumberStyle.style(),i->NumberStyle.setStyle(i))));
        column.addView(wrap(icons));

        column.addView(section(Text.FONT));
        LinearLayout fonts = card();
        // the font has a page of its own: every font is there as it opens
        fonts.addView(fontHead());
        fonts.addView(fontChoices());
        column.addView(wrap(fonts));

        // how the mod's own screens move and answer a touch
        column.addView(section(Text.t("Интерфейс мода")));
        LinearLayout motion = card();
        final String[] motions={Text.t("Как в системе"),Text.t("Плавные"),Text.t("Быстрые"),Text.t("Выключены")};
        motion.addView(actionRow("animation",Text.t("Анимации"),motions[Motion.mode()],()->
            new ModDialog.Builder(this).setItems(motions,(dialog,index)->{Motion.setMode(index);rebuild();}).show()));
        motion.addView(line());
        motion.addView(liveToggleRow("speed",Text.t("Режим производительности"),Motion.performanceMode(),on->{
            Motion.setPerformanceMode(on);
            if(snow!=null)snow.invalidate();
            rebuild();
        }));
        motion.addView(liveToggleRow("fingerprint",Text.t("Отклик вибрацией на нажатия"),Motion.haptics(),Motion::setHaptics));
        motion.addView(liveToggleRow("volume_up",Text.t("Звуки нажатий"),UiSound.on(),UiSound::set));
        column.addView(wrap(motion));

        // Profile: one preview that stays in view while the rows under it change it, then the nickname, how it
        // looks, the badge, and sync. The ids and the reset are at the very end.
        column.addView(section(Text.PROFILE_STYLE));
        FrameLayout previewHolder = new FrameLayout(this);
        previewHolder.setClipChildren(false);
        previewHolder.setPadding(dp(16), 0, dp(16), dp(8));
        previewHolder.addView(realProfilePreview());
        column.addView(previewHolder, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pinHolder = previewHolder;

        LinearLayout nick = card();
        String ownName = ProfileStyle.displayName(Account.liveId(), "");
        nick.addView(actionRow("edit",Text.t("Имя в моде"),ownName == null || ownName.isEmpty() ? Text.t("Как в TikTok") : ownName,()->{
            android.widget.EditText name=new android.widget.EditText(this);
            name.setText(ProfileStyle.displayName(Account.liveId(),Account.profileName()==null?"":Account.profileName()));
            name.setSingleLine(true);name.setTextColor(skin.text);
            Panel.with(this,skin,Text.t("Имя в моде")).view(name).primary(Text.t("Сохранить"),()->{
                String value=name.getText().toString().trim();if(value.length()<=80){ProfileStyle.setDisplayName(value);rebuild();}
            }).quiet(Text.t("Назад"),null).show();
        }));
        nick.addView(actionRow("tag",Text.t("Тег"),ProfileStyle.tag().isEmpty()?Text.t("Нет"):ProfileStyle.tag(),this::chooseTag));
        nick.addView(actionRow("text_fields",Text.t("Шрифт ника"),nicknameFontName(),this::chooseNicknameFont));
        nick.addView(toggleRow("text_fields", Text.PROFILE_BOLD, ProfileStyle.bold(), on -> {
            ProfileStyle.setBold(on);
            markChanged();
            rebuildAfterSwitchAnimation();
        }));
        column.addView(wrap(nick));

        column.addView(section(Text.PROFILE_LOOK));
        LinearLayout look = card();
        look.addView(toggleRow("palette", Text.PROFILE_GRADIENT,
                ProfileStyle.gradient(), on -> {
                    ProfileStyle.setGradient(on);
                    markChanged();
                    rebuildAfterSwitchAnimation();
                }));
        if (ProfileStyle.gradient()) {
            look.addView(gradientPalette());
            // direction, width, motion: rarely touched, so folded away
            LinearLayout fine=row();
            fine.addView(icon("tune"));
            fine.addView(label(Text.PROFILE_FINE),grow());
            Chevron gradientArrow=new Chevron(this,skin.text,gradientFineOpen,false);fine.addView(gradientArrow,new LinearLayout.LayoutParams(dp(24),dp(24)));
            fine.setOnClickListener(v->{gradientFineOpen=!gradientFineOpen;rebuild();});
            look.addView(sized(fine,56));
            if (gradientFineOpen) {
                LinearLayout more=new LinearLayout(this);more.setOrientation(LinearLayout.VERTICAL);
                more.addView(actionRow("swap_horiz", Text.PROFILE_DIRECTION,
                        profileDirection(ProfileStyle.direction()), this::chooseProfileDirection));
                more.addView(liveSlider("palette",Text.t("Ширина перехода · 50–200%"), ProfileStyle.gradientWidth()-50,150,50,"%",value -> {
                    ProfileStyle.setGradientWidth(value+50);markChanged();
                }));
                if(ProfileStyle.gradientColours().length==3)more.addView(liveSlider("palette",Text.t("Положение среднего цвета"),ProfileStyle.gradientStop()-5,90,5,"%",value -> {
                    ProfileStyle.setGradientStop(value+5);markChanged();
                }));
                more.addView(toggleRow("animation", Text.PROFILE_ANIMATED,
                        ProfileStyle.animated(), on -> {
                            ProfileStyle.setAnimated(on);
                            markChanged();
                            rebuildAfterSwitchAnimation();
                        }));
                if (ProfileStyle.animated()) {
                    more.addView(actionRow("animation", Text.t("Анимация градиента"),
                            gradientMotion(ProfileStyle.motion()), this::chooseGradientMotion));
                    more.addView(liveSlider("speed",Text.PROFILE_SPEED, ProfileStyle.speed(), 100, 0, "%", value -> {
                        ProfileStyle.setSpeed(value);
                        markChanged();
                    }));
                }
                look.addView(appear(more));
            }
        }
        look.addView(toggleRow("flare", Text.PROFILE_GLOW, ProfileStyle.glow(), on -> {
            ProfileStyle.setGlow(on);
            markChanged();
            rebuildAfterSwitchAnimation();
        }));
        if (ProfileStyle.glow())
            look.addView(slider("flare",Text.t("Сила свечения"),ProfileStyle.glowStrength(),100,0,"%",ProfileStyle::setGlowStrength));
        look.addView(actionRow("animation",Text.t("Появление ника"),Text.t(APPEARS[ProfileStyle.appear()]),()->choose(Text.t("Появление ника"),APPEARS,ProfileStyle.appear(),ProfileStyle::setAppear)));
        look.addView(actionRow("star",Text.t("Частицы у ника"),Text.t(PARTICLE_NAMES[ProfileStyle.particles()]),()->choose(Text.t("Частицы у ника"),PARTICLE_NAMES,ProfileStyle.particles(),i->{
            if(i==ProfileStyle.CUSTOM&&ProfileStyle.particleRecipe().isEmpty())openParticleEditor();else ProfileStyle.setParticles(i);})));
        look.addView(actionRow("brush",Text.t("Свои частицы"),Text.t("Своя картинка, движение, цвет"),this::openParticleEditor));
        look.addView(actionRow("palette",Text.t("Цвет комментариев"),ProfileStyle.commentColour()==0?Text.t("Как в TikTok"):String.format(Locale.US,"#%06X",ProfileStyle.commentColour()&0xFFFFFF),this::chooseCommentColour));
        column.addView(wrap(look));

        column.addView(section(BADGE_SECTION));
        LinearLayout profile = card();
        profile.addView(actionRow("image", Text.PROFILE_BADGE,
                ProfileStyle.hasBadge() ? (ProfileStyle.frames() != null ? Text.t("Анимированный") : Text.SAVED) : Text.t("Картинка, GIF или видео"), this::pickProfileBadge));
        if (ProfileStyle.hasBadge() || (Badges.of(Account.id())!=null && Badges.of(Account.id()).length>0)) {
            LinearLayout placement=row();
            placement.addView(icon("open_with"));
            placement.addView(label(Text.t("Положение · на всех экранах")),grow());
            Chevron placementArrow=new Chevron(this,skin.text,badgePlacementOpen,false);placement.addView(placementArrow,new LinearLayout.LayoutParams(dp(24),dp(24)));
            placement.setOnClickListener(v->{badgePlacementOpen=!badgePlacementOpen;rebuild();});
            profile.addView(sized(placement,56));
            if (badgePlacementOpen) {
            LinearLayout positions=new LinearLayout(this);positions.setOrientation(LinearLayout.VERTICAL);
            positions.addView(quiet(Text.t("Перетащите значок в превью")));
            positions.addView(liveSlider("swap_horiz",Text.t("По горизонтали"), ProfileStyle.offsetX() + 80, 160, -80, "",
                    value -> { ProfileStyle.setBadgePlacement(value - 80,
                            ProfileStyle.offsetY(), ProfileStyle.badgeSize());
                        markChanged(); }));
            positions.addView(liveSlider("swap_vert",Text.t("По вертикали"), ProfileStyle.offsetY() + 50, 100, -50, "",
                    value -> { ProfileStyle.setBadgePlacement(ProfileStyle.offsetX(),
                            value - 50, ProfileStyle.badgeSize());
                        markChanged(); }));
            positions.addView(liveSlider("image",Text.t("Размер значка"),
                    ProfileStyle.badgeSize() - 50, 150, 50, "%",
                    value -> { ProfileStyle.setBadgePlacement(ProfileStyle.offsetX(),
                            ProfileStyle.offsetY(), value + 50);
                        markChanged(); }));
            positions.addView(actionRow("restore",Text.t("Сбросить положение"),null,()->{ProfileStyle.setBadgePlacement(0,0,100);rebuild();}));
            profile.addView(appear(positions));
            }
            if(ProfileStyle.hasBadge())profile.addView(actionRow("restore", Text.PROFILE_BADGE_REMOVE, null, () -> {
                ProfileStyle.removeBadge();
                markChanged();
                rebuild();
            }));
        }
        if (AdminAccess.current()) {
            CloudProfileProvider badgeSync = new CloudProfileProvider(this);
            profile.addView(actionRow("workspace_premium", Text.t("Выдать значок"),
                    badgeSync.verified(Account.id())
                            ? Text.t("Ник, ссылка или UID")
                            : Text.t("Сначала подтвердите аккаунт создателя"),
                    this::changeCommunityBadge));
        }
        profile.addView(liveToggleRow("verified",Text.t("Значки MargyT"),Badges.margytEnabled(),Badges::setMargytEnabled));
        View badgeBox = wrap(profile);
        pinEnd = badgeBox;
        column.addView(badgeBox);
        if (page != null) page.post(this::pinPreview);

        column.addView(section(Text.PROFILE_SYNC));
        LinearLayout sync = card();
        if (TtcuzProfileSync.available()) {
            if (new CloudProfileProvider(this).verified(Account.id())) {
                sync.addView(actionRow("verified", Text.t("Профиль подтверждён"),
                        Text.t("Синхронизация включена"),
                        () -> {
                            TtcuzProfileSync.retryOwn();updateSyncUi();
                        }));
            } else {
                CloudProfileProvider cloud = new CloudProfileProvider(this);
                boolean taken = cloud.needsProof(Account.id());
                sync.addView(actionRow("verified",
                        taken ? Text.t("Профиль занят другим устройством") : Text.t("Подтвердить для своего значка"),
                        taken ? Text.t("Один раз добавьте код в описание TikTok, чтобы вернуть профиль")
                              : Text.t("Нужно только для публикации значка"),
                        this::verifyProfile));
            }
            sync.addView(toggleRow("link", Text.PROFILE_SHARE,
                    ProfileStyle.sharing(), on -> {
                        ProfileStyle.share(on);
                        if (on) TtcuzProfileSync.autoVerifyOwn();
                        markChanged();
                        rebuildAfterSwitchAnimation();
                    }));
            sync.addView(syncStatusRow());
        } else {
            sync.addView(quiet(Text.PROFILE_SHARE_WAIT));
        }
        column.addView(wrap(sync));

        column.addView(section(Text.ACCOUNT));
        LinearLayout account = card();
        account.addView(idRow(Text.ACCOUNT_ID, Account.id()));
        account.addView(line());
        account.addView(idRow(Text.ACCOUNT_SEC_ID, Account.secId()));
        account.addView(actionRow("restore", Text.PROFILE_RESET, null, () -> {
            ProfileStyle.reset();markChanged();rebuild();
        }));
        column.addView(wrap(account));

        // ayumoments: what others and TikTok itself may learn about you
        column.addView(section(AYU_PEOPLE));
        LinearLayout privacy = card();
        privacy.addView(liveToggleRow("visibility_off",Text.t("Скрыть «в сети»"),Privacy.hideOnline(),on->Privacy.set(Privacy.ONLINE,on)));
        privacy.addView(liveToggleRow("chat",Text.t("Не отмечать прочтение"),Privacy.hideRead(),on->Privacy.set(Privacy.READ,on)));
        privacy.addView(liveToggleRow("edit",Text.t("Скрыть «печатает»"),Privacy.hideTyping(),on->Privacy.set(Privacy.TYPING,on)));
        privacy.addView(liveToggleRow("visibility_off",Text.t("Не отмечать просмотр историй"),Privacy.hideStories(),on->Privacy.set(Privacy.STORIES,on)));
        privacy.addView(liveToggleRow("account_circle",Text.t("Не отмечать просмотр профилей"),Privacy.hideProfiles(),on->Privacy.set(Privacy.PROFILES,on)));
        privacy.addView(liveToggleRow("trash_mod",Text.t("Читать удалённые сообщения"),DeletedMessages.enabled(),DeletedMessages::set));
        privacy.addView(line());
        privacy.addView(liveToggleRow("lock",Text.t("Защита входа в TikTok"),AppLock.app(),on->{if(!AppLock.set(this,AppLock.APP,on))rebuild();}));
        privacy.addView(liveToggleRow("chat",Text.t("Защита переписок"),AppLock.chats(),on->{if(!AppLock.set(this,AppLock.CHATS,on))rebuild();}));
        privacy.addView(actionRow(AppLock.usesPin()?"key":"fingerprint",Text.t("Чем открывать"),Text.t(AppLock.METHODS[AppLock.usesPin()?AppLock.CODE:AppLock.SYSTEM]),()->
            chooseLive(Text.t("Чем открывать"),AppLock.METHODS,AppLock.usesPin()?AppLock.CODE:AppLock.SYSTEM,i->{
                if(i==AppLock.CODE)AppLock.choosePin(this,this::rebuild);else AppLock.clearPin();})));
        if (AppLock.usesPin())
            privacy.addView(actionRow("key",Text.t("Сменить код"),null,()->AppLock.choosePin(this,this::rebuild)));
        column.addView(wrap(privacy));

        column.addView(section(AYU_DEVICE));
        LinearLayout device = card();
        device.addView(toggleRow("people",Text.t("Блокировать доступ к контактам"),DevicePrivacy.on(DevicePrivacy.CONTACTS),on->DevicePrivacy.set(DevicePrivacy.CONTACTS,on)));
        device.addView(toggleRow("dynamic_feed",Text.t("Блокировать доступ к списку приложений"),DevicePrivacy.on(DevicePrivacy.APPS),on->DevicePrivacy.set(DevicePrivacy.APPS,on)));
        device.addView(toggleRow("place",Text.t("Блокировать доступ к геопозиции"),DevicePrivacy.on(DevicePrivacy.LOCATION),on->DevicePrivacy.set(DevicePrivacy.LOCATION,on)));
        device.addView(toggleRow("fingerprint",Text.t("Случайный Android ID"),DevicePrivacy.on(DevicePrivacy.ANDROID_ID),on->DevicePrivacy.set(DevicePrivacy.ANDROID_ID,on)));
        device.addView(liveToggleRow("history",Text.t("Не сохранять историю поиска"),SearchHistoryGate.enabled(),SearchHistoryGate::set));
        column.addView(wrap(device));

        column.addView(section(CHATS));
        LinearLayout chats = card();
        chats.addView(liveToggleRow("push_pin",Text.t("Сохранять позицию в чате"),ChatScroll.enabled(),ChatScroll::set));
        chats.addView(liveToggleRow("star",Text.t("Подсказки стикеров при наборе"),StickerStrip.enabled(),StickerStrip::set));
        column.addView(wrap(chats));

        column.addView(section(Text.PINNED_FRIENDS));
        LinearLayout friends=card();friends.addView(actionRow("people",Text.PINNED_FRIENDS,null,()->PinnedFriends.show(this)));
        column.addView(wrap(friends));

        column.addView(section(Text.STREAKS));
        LinearLayout streaks = card();
        streaks.addView(toggleRow("repeat", Text.STREAK_AUTO, Streaks.isEnabled(),
                on -> { Streaks.setEnabled(on); rebuildAfterSwitchAnimation(); }));
        // Only the switch while it is off; the three choices that matter once it is on, and the rest under one "More".
        if (Streaks.isEnabled()) {
            streaks.addView(line());
            streaks.addView(actionRow("people", Text.t("С кем продлевать"),
                    Streaks.withEveryone() ? Text.t("Со всеми") : Text.t("Только выбранные"),
                    () -> StreakFriendsSheet.show(this, skin, this::rebuild)));
            streaks.addView(line());
            streaks.addView(actionRow("schedule", Text.t("Время отправки"),StreakSchedule.label(),()->StreakSchedule.choose(this,this::rebuild)));
            streaks.addView(line());
            int many = Streaks.offered().size();
            streaks.addView(actionRow("star", Text.STREAK_STICKER,
                    many == 0 ? Text.STREAK_NOTHING : String.valueOf(many),
                    () -> StickerSheet.show(this, skin, this::rebuild)));
            streaks.addView(line());
            streaks.addView(actionRow("tune", Text.t("Ещё"), Text.t("Проверка, отправить всем, тест"), () ->
                new ModDialog.Builder(this).setItems(new String[]{Text.STREAK_HEALTH + ": " + StreakAlarm.status(this), Text.STREAK_ALL, Text.STREAK_TEST}, (dialog, index) -> {
                    // What the schedule is standing on, said plainly: an inexact alarm, a battery saver or a muted
                    // notification channel each stop the send, and none of them is visible anywhere else.
                    if (index == 0) { StreakAlarm.permissions(this); Toast.makeText(this, StreakAlarm.status(this), Toast.LENGTH_LONG).show(); }
                    else if (index == 1) Popup.ask(this, Text.STREAK_ALL, Text.STREAK_ALL_ASK, Text.STREAK_ALL_SEND, () -> Streaks.sendAll(this), Text.CANCEL, null, null, null);
                    else { Streaks.test(this); Toast.makeText(this, Text.STREAK_TEST_GOING, Toast.LENGTH_SHORT).show(); }
                }).show()));
        }
        column.addView(wrap(streaks));

        column.addView(section(Text.NOTIFICATIONS));
        LinearLayout notifications=card();
        notifications.addView(liveToggleRow("chat",Text.NOTIFY_MESSAGES,ModNotifications.option(0),on->{ModNotifications.set(ModNotifications.MESSAGES,on);rebuildAfterSwitchAnimation();}));
        notifications.addView(liveToggleRow("notifications",Text.NOTIFY_PROMOS,ModNotifications.option(1),on->{ModNotifications.set(ModNotifications.PROMOS,on);rebuildAfterSwitchAnimation();}));
        notifications.addView(liveToggleRow("notifications",Text.NOTIFY_OTHER,ModNotifications.option(2),on->{ModNotifications.set(ModNotifications.OTHER,on);rebuildAfterSwitchAnimation();}));
        notifications.addView(liveToggleRow("notifications_off",Text.NOTIFY_NO_PROMPTS,ModNotifications.noPrompts(),on->{AccountAppearance.prefs(this).edit().putBoolean(ModNotifications.NO_PROMPTS,on).apply();ModNotifications.reload();}));
        // quiet hours: TikTok's notifications still come, without sound or vibration, at the chosen times
        notifications.addView(liveToggleRow("notifications_off",Text.t("Тихие часы"),QuietHours.enabled(),on->{QuietHours.set(on);rebuildAfterSwitchAnimation();}));
        if(QuietHours.enabled())notifications.addView(actionRow("schedule",Text.t("Время тишины"),QuietHours.range()+" · "+Text.t("без звука и вибрации"),this::chooseQuietHours));
        notifications.addView(actionRow("settings",Text.NOTIFY_SETTINGS,null,()->ModNotifications.settings(this)));
        column.addView(wrap(notifications));

        column.addView(section(Text.t("Плагины")));
        // installed plugins as cards of their own (PluginList), then the ways to get more
        column.addView(PluginList.build(this, skin));
        LinearLayout plugins = card();
        plugins.addView(actionRow("add",Text.t("Установить .ctt"),"",()->{
            Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
            try{startActivityForResult(pick,PICK_PLUGIN);}catch(RuntimeException e){Screen.say(Text.t("Не удалось открыть файлы"));}
        }));
        plugins.addView(actionRow("public",Text.t("Магазин плагинов"),"",()->PluginWorkbench.store(this,skin)));
        plugins.addView(actionRow("edit",Text.t("Редактор плагинов"),"",()->PluginWorkbench.editor(this,skin)));
        // a plugin that breaks the start of TikTok can be switched off or removed from here
        plugins.addView(liveToggleRow("lock",Text.t("Безопасный режим (не запускать плагины при старте)"),PluginEngine.safeMode(),PluginEngine::setSafeMode));
        column.addView(wrap(plugins));
        // rows plugins add to the mod's own settings (onModSettings), one card each under the plugin's name
        for(View added:PluginList.modSettings(this,skin)){LinearLayout box=card();box.addView(added);column.addView(wrap(box));}

        column.addView(section(Text.DOWNLOADS));
        LinearLayout downloads = card();
        downloads.addView(actionRow("image",Text.FRAME_QUALITY,Integer.toString(FrameCapture.quality()),()->{
            new ModDialog.Builder(this).setItems(new String[]{"480","720","1080"},(dialog,index)->{AccountAppearance.prefs(this).edit().putInt(FrameCapture.KEY,new int[]{480,720,1080}[index]).apply();rebuild();}).show();
        }));
        downloads.addView(toggleRow("image", Text.NO_WATERMARK, Download.isEnabled(),
                Download::setEnabled));
        downloads.addView(line());
        downloads.addView(toggleRow("download", Text.DOWNLOAD_ALWAYS, Download.isAlways(),
                Download::setAlways));
        downloads.addView(line());
        downloads.addView(toggleRow("place", Text.SAVE_AVATARS_ON, Avatars.isEnabled(),
                Avatars::setEnabled));
        downloads.addView(line());
        downloads.addView(toggleRow("star", Text.SAVE_STICKERS_ON, Stickers.isEnabled(),
                Stickers::setEnabled));
        column.addView(wrap(downloads));

        // how many people use ttcuz: the pill under the title, hidden by holding it and brought back here
        column.addView(section(Text.t("Сколько нас")));
        LinearLayout people = card();
        people.addView(liveToggleRow("group",Text.t("Показывать под заголовком"),ModStats.shown(this),on->ModStats.setShown(this,on)));
        people.addView(actionRow("bolt",Text.t("Открыть статистику"),null,this::statsSheet));
        column.addView(wrap(people));

        column.addView(section(Text.BACKUP));
        LinearLayout backup = card();
        backup.addView(actionRow("upload", Text.BACKUP_EXPORT, Text.BACKUP_EXPORT_NOTE,
                this::createBackup));
        backup.addView(line());
        backup.addView(actionRow("download", Text.BACKUP_IMPORT, Text.BACKUP_IMPORT_NOTE,
                this::pickBackup));
        backup.addView(line());
        backup.addView(actionRow("restore", Text.BACKUP_RESET, Text.BACKUP_RESET_NOTE,
                this::pickResetGroup));
        column.addView(wrap(backup));

        column.addView(section(Text.DIAGNOSTICS));
        LinearLayout diagnostics = card();
        diagnostics.addView(liveToggleRow("bug_report", Text.t("Отчёты о сбоях"),CrashReports.enabled(this),on->CrashReports.set(this,on)));
        diagnostics.addView(line());
        diagnostics.addView(liveToggleRow("speed",Text.t("Собирать статистику кадров"),
                getSharedPreferences(Ttcuz.PREFS,MODE_PRIVATE).getBoolean("lag_frame_stats",false),on->{
                    getSharedPreferences(Ttcuz.PREFS,MODE_PRIVATE).edit().putBoolean("lag_frame_stats",on).apply();
                    Lag.setFrameCaptureEnabled(on);
                    if(on)Lag.frames(this);
                }));
        diagnostics.addView(line());
        diagnostics.addView(actionRow("speed",Text.t("Показать статистику кадров"),null,()->{
            boolean capture=getSharedPreferences(Ttcuz.PREFS,MODE_PRIVATE)
                    .getBoolean("lag_frame_stats",false);
            Panel.with(this,skin,Text.t("Статистика кадров"))
                    .text(capture?Lag.report():Text.t("Сначала включите сбор статистики кадров"))
                    .primary(Text.t("Готово"),null).show();
        }));
        diagnostics.addView(line());
        // TikTok's own start-up GC blocker; off by itself on custom firmware, where it ends in out-of-memory crashes
        diagnostics.addView(toggleRow("speed",Text.t("Отключить ускоритель запуска TikTok"),MemoryGuard.gcBlockerOff(),on->getSharedPreferences(Ttcuz.PREFS,MODE_PRIVATE).edit().putBoolean(MemoryGuard.KEY_NO_GC_BLOCKER,on).apply()));
        diagnostics.addView(line());
        diagnostics.addView(actionRow("article", Text.COPY_DIAGNOSTICS, null,
                () -> copy("ttcuz diagnostics", DevMode.isOn()
                        ? Diagnostics.fullReport() : Diagnostics.report())));
        column.addView(wrap(diagnostics));

        column.addView(section(Text.DIARY));
        LinearLayout diary = card();
        diary.addView(diaryHead());
        if (diaryOpen) {
            diary.addView(line());
            diary.addView(appear(diaryLines()));
        }
        if (DevMode.isOn()) addDeveloperRows(diary);
        column.addView(wrap(diary));

        column.addView(section(Text.UPDATE));
        LinearLayout updates = card();
        updates.addView(actionRow("download", Text.UPDATE_CHECK,
                Updater.newer() ? Text.UPDATE_THERE_IS + " " + Updater.latest() : null,
                () -> Updater.check(this, true)));
        updates.addView(line());
        updates.addView(toggleRow("info", Text.UPDATE_REMIND, Updater.remind(this),
                on -> Updater.setRemind(this, on)));
        if (Updater.waiting(this)) {
            updates.addView(line());
            updates.addView(actionRow("extension", Text.UPDATE_INSTALL, null,
                    () -> Updater.install(this)));
        }
        column.addView(wrap(updates));


        if (searching) {
            showSearchResults();
            showRestartBar();
            restoreScroll(0);
            return;
        }
        arrangeSections();
        filterGroup();
        column.addView(versions());

        showRestartBar();
        restoreScroll(previousScroll);
    }

    /**
     * A row is something that can be tapped or holds a switch or a slider itself. A card that only contains such
     * rows is not one: it is searched row by row, so a hit brings back that row and not its whole card.
     */
    private static boolean settingRow(View view) {
        if (view.hasOnClickListeners()) return true;
        if (view instanceof android.widget.SeekBar || view instanceof M3Switch || view instanceof IslandEditor) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof android.widget.SeekBar || child instanceof M3Switch) return true;
            }
        }
        return false;
    }

    /** Lower case, and ё read as е: "чёрный" is found by "черный". */
    private static String folded(CharSequence text) {
        return text.toString().toLowerCase(Locale.ROOT).replace('ё', 'е');
    }

    private static void rowWords(View view, StringBuilder into) {
        if (view instanceof TextView) into.append(((TextView) view).getText()).append(' ');
        CharSequence description = view.getContentDescription();
        if (description != null) into.append(description).append(' ');
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) rowWords(group.getChildAt(i), into);
        }
    }

    private void findRows(View view, String section, java.util.LinkedHashMap<String, java.util.List<View>> hits) {
        if (view == null || view.getVisibility() == View.GONE) return;
        if (settingRow(view)) {
            StringBuilder words = new StringBuilder();
            rowWords(view, words);
            String wanted = folded(searchQuery);
            if (folded(words).contains(wanted) || folded(section).contains(wanted)) {
                java.util.List<View> list = hits.get(section);
                if (list == null) { list = new java.util.ArrayList<View>(); hits.put(section, list); }
                list.add(view);
            }
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) findRows(group.getChildAt(i), section, hits);
        }
    }

    /** Replace the freshly built page with the rows that match the query, grouped by section. */
    private void showSearchResults() {
        java.util.List<View> built = new java.util.ArrayList<View>();
        for (int i = filterFrom; i < column.getChildCount(); i++) built.add(column.getChildAt(i));
        java.util.LinkedHashMap<String, java.util.List<View>> hits =
                new java.util.LinkedHashMap<String, java.util.List<View>>();
        String section = "";
        for (View item : built) {
            if (item instanceof TextView && sectionTargets.containsValue(item)) {
                section = ((TextView) item).getText().toString();
                continue;
            }
            findRows(item, section, hits);
        }
        for (java.util.List<View> rows : hits.values()) for (View row : rows) detach(row);
        while (column.getChildCount() > filterFrom) column.removeViewAt(column.getChildCount() - 1);
        sectionTargets.clear();
        if (hits.isEmpty()) {
            LinearLayout none = card();
            none.addView(caption(Text.SEARCH_NONE));
            column.addView(wrap(none));
            return;
        }
        // a row can be found twice (inside another hit, or under two headings): each is shown once
        java.util.Set<View> placed = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<View, Boolean>());
        for (java.util.Map.Entry<String, java.util.List<View>> entry : hits.entrySet()) {
            LinearLayout card = card();
            for (View row : entry.getValue()) {
                if (!placed.add(row) || !detach(row)) continue;
                card.addView(row);
            }
            if (card.getChildCount() == 0) continue;
            column.addView(section(entry.getKey()));
            column.addView(wrap(card));
        }
    }

    /**
     * Takes a row out of where it is, for good: a row still sliding away (an animation, a layout transition) stays
     * its old parent's child until the motion ends, and adding it elsewhere then throws. False when it cannot be freed.
     */
    private static boolean detach(View row) {
        android.view.ViewParent up = row.getParent();
        if (!(up instanceof ViewGroup)) return up == null;
        ViewGroup parent = (ViewGroup) up;
        row.animate().cancel();
        row.clearAnimation();
        // a page change may have left it half-faded or mid-slide
        row.setAlpha(1f);row.setTranslationX(0f);row.setTranslationY(0f);
        android.animation.LayoutTransition motion = parent.getLayoutTransition();
        parent.setLayoutTransition(null);
        parent.removeView(row);
        parent.endViewTransition(row);
        parent.setLayoutTransition(motion);
        return row.getParent() == null;
    }

    private void restoreScroll(final int scrollY) {
        Fonts.applyTree(root);
        // every rebuild ends here: plugins with "mod" restyle the screen as it now is
        PluginEngine.modScreen(this, root);
        if (page == null) return;
        requestedScrollY = Math.max(0, scrollY);
        if (scrollRestore != null) page.removeCallbacks(scrollRestore);
        if (scrollRestoreListener != null && page.getViewTreeObserver().isAlive()) {
            page.getViewTreeObserver().removeOnPreDrawListener(scrollRestoreListener);
            scrollRestoreListener = null;
        }
        if (requestedScrollY == 0) {
            page.scrollTo(0, 0);
            scrollRestore = null;
            return;
        }
        // Restore before the next frame is drawn. A posted scroll runs after
        // the new list has briefly appeared at the top of the screen.
        scrollRestoreListener = new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                page.getViewTreeObserver().removeOnPreDrawListener(this);
                scrollRestoreListener = null;
                page.scrollTo(0, requestedScrollY);
                return true;
            }
        };
        page.getViewTreeObserver().addOnPreDrawListener(scrollRestoreListener);
    }

    /**
     * The bar that says a restart is due, pinned to the foot of the screen.
     *
     * It sits in the root frame rather than in the column, so it stays put
     * while the page scrolls under it, and it is built again on every rebuild
     * because the accent it is painted with may be what just changed.
     */
    private void showRestartBar() {
        if (pending && restartBar == null) {
            restartBar = restartBar();
            FrameLayout.LayoutParams floating = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
            floating.setMargins(dp(12), 0, dp(12), dp(12) + navigationBar());
            root.addView(restartBar, floating);
            Motion.rise(restartBar);
        }
        if (pending && restartBar instanceof LinearLayout) {
            LinearLayout bar = (LinearLayout)restartBar;
            styleRestartBar(bar);
            restartSymbol.colour(Accent.colour());
            restartWhy.setTextColor(skin.text);
            restartAction.setTextColor(onAccent());
            restartAction.setBackground(restartPill());
            restartAction.setClickable(true);
        }
        // Keep an existing bar in place: rebuilding rows must not replay its entrance.
        if (!pending && restartBar != null) {
            restartBar.animate().withEndAction(null).cancel();
            root.removeView(restartBar);
            restartBar = null;
            restartSymbol=null;restartWhy=null;restartAction=null;
        }
        column.setPadding(0, statusBar(), 0,
                dp(32) + (pending ? dp(92) + navigationBar() : 0));
    }

    /** The restart bar floats: a rounded tonal container with the accent laid over it. */
    private void styleRestartBar(LinearLayout bar) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Expressive.mix(skin.card, Accent.colour(), 0.22f));
        shape.setCornerRadius(dp(28));
        bar.setBackground(shape);
        bar.setElevation(dp(6));
    }

    /** A setting changed: redraw, and from now on the bar is up. */
    private void markChanged() {
        CloudAppearance.changed();
        pending = Unsaved.after(this);
        showRestartBar();
    }

    /** A live preview based on the identity TikTok has loaded for this account. */
    private View realProfilePreview() {
        FrameLayout panel = new FrameLayout(this);
        panel.setClipChildren(false);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        // the same surface and corners as the groups under it, so it lines up with them
        GradientDrawable surface = new GradientDrawable();
        surface.setColor(skin.card);
        surface.setCornerRadius(dp(28));
        panel.setBackground(surface);
        panel.setElevation(0f);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipChildren(false);
        ImageView avatar = new ImageView(this);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(blend(skin.muted(), skin.page, 0.72f));
        avatar.setBackground(circle);
        avatar.setClipToOutline(false);
        avatar.setVisibility(View.INVISIBLE);

        TextView initial = new TextView(this);
        initial.setGravity(Gravity.CENTER);
        initial.setTextColor(skin.text);
        initial.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        String name = Account.profileName();
        String uid = Account.id();
        boolean live = name != null && name.length() > 0;
        initial.setText(live ? name.substring(0, 1).toUpperCase(Locale.ROOT) : "•");

        FrameLayout avatarFrame = new FrameLayout(this){
            final android.graphics.Path shape=new android.graphics.Path();
            @Override public void draw(Canvas canvas){
                shape.reset();AvatarShape.addCircle(shape,getWidth()/2f,getHeight()/2f,Math.min(getWidth(),getHeight())/2f,android.graphics.Path.Direction.CW);
                int save=canvas.save();canvas.clipPath(shape);super.draw(canvas);canvas.restoreToCount(save);
            }
        };
        GradientDrawable avatarBack = new GradientDrawable();
        avatarBack.setShape(GradientDrawable.OVAL);
        avatarBack.setColor(blend(skin.muted(), skin.page, 0.72f));
        avatarFrame.setBackground(avatarBack);
        avatarFrame.setClipToOutline(false);
        avatarFrame.addView(initial, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        avatarFrame.addView(avatar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        row.addView(avatarFrame, new LinearLayout.LayoutParams(dp(58), dp(58)));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setGravity(Gravity.CENTER_VERTICAL);
        details.setClipChildren(false);
        LinearLayout.LayoutParams detailsPlace = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        detailsPlace.leftMargin = dp(14);
        row.addView(details, detailsPlace);

        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        nameRow.setClipChildren(false);
        nameRow.setClipToPadding(false);
        TextView nickname = label(live ? name : Text.PROFILE_PREVIEW_EMPTY);
        nickname.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        nickname.setTypeface(Typeface.DEFAULT_BOLD);
        nickname.setSingleLine(false);
        nickname.setMaxLines(2);
        nickname.setMinHeight(dp(30));
        nickname.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (live) Badge.previewNickname(nickname, name, uid);
        nameRow.addView(nickname, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        android.widget.HorizontalScrollView nameViewport=new android.widget.HorizontalScrollView(this);
        nameViewport.setHorizontalScrollBarEnabled(false);nameViewport.setClipChildren(false);
        nameViewport.setClipToPadding(false);
        nameViewport.addView(nameRow,new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        details.addView(nameViewport,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView handle = label(live
                ? (Account.profileHandle() == null
                    ? "TikTok ID · " + (uid == null ? "—" : uid)
                    : "@" + Account.profileHandle())
                : Text.PROFILE_PREVIEW_LOADING);
        handle.setTextColor(skin.muted());
        handle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        handle.setSingleLine(true);
        details.addView(handle);
        ProfilePreview.bind(this, avatar, initial, nickname, handle);
        dragProfileMarks(nickname);

        panel.addView(row, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return panel;
    }

    private void dragProfileMarks(TextView nickname) {
        nickname.setOnTouchListener(new View.OnTouchListener() {
            private float startX, startY, density;
            private int kind, originX, originY, chosenX, chosenY;
            @Override public boolean onTouch(View view, android.view.MotionEvent event) {
                switch (event.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        kind = Badge.previewMarkAt(nickname, event.getX(), event.getY());
                        if (kind == 0) return false;
                        density = nickname.getTextSize()/24f;
                        startX = event.getRawX(); startY = event.getRawY();
                        originX = chosenX = kind == 2
                                ? ProfileStyle.crownOffsetX() : ProfileStyle.offsetX();
                        originY = chosenY = kind == 2
                                ? ProfileStyle.crownOffsetY() : ProfileStyle.offsetY();
                        if (view.getParent() != null)
                            view.getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    case android.view.MotionEvent.ACTION_MOVE:
                        if (kind == 0) return false;
                        int max = kind == 2 ? 100 : 80;
                        chosenX = Math.max(-max, Math.min(max,
                                originX + Math.round((event.getRawX() - startX) / density)));
                        chosenY = Math.max(-max, Math.min(max,
                                originY + Math.round((event.getRawY() - startY) / density)));
                        Badge.movePreviewMark(nickname, kind, chosenX, chosenY);
                        return true;
                    case android.view.MotionEvent.ACTION_UP:
                        if (kind == 0) return false;
                        if (kind == 2) ProfileStyle.setCrownPlacement(chosenX, chosenY);
                        else ProfileStyle.setBadgePlacement(chosenX, chosenY,
                                ProfileStyle.badgeSize());
                        markChanged();
                        kind = 0;
                        if (view.getParent() != null)
                            view.getParent().requestDisallowInterceptTouchEvent(false);
                        return true;
                    case android.view.MotionEvent.ACTION_CANCEL:
                        if (kind == 0) return false;
                        Badge.movePreviewMark(nickname, kind, originX, originY);
                        kind = 0;
                        if (view.getParent() != null)
                            view.getParent().requestDisallowInterceptTouchEvent(false);
                        return true;
                    default:
                        return kind != 0;
                }
            }
        });
    }

    private View syncStatusRow() {
        LinearLayout row=row();row.addView(icon("cloud_upload"));
        LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);
        syncTitle=label("");syncTitle.setTextSize(14);syncTitle.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        syncDetails=detail("");syncDetails.setMaxLines(2);syncDetails.setEllipsize(android.text.TextUtils.TruncateAt.END);
        words.addView(syncTitle);words.addView(syncDetails);row.addView(words,grow());
        syncSpinner=new android.widget.ProgressBar(this);LinearLayout.LayoutParams spinnerSize=new LinearLayout.LayoutParams(dp(18),dp(18));spinnerSize.leftMargin=dp(12);row.addView(syncSpinner,spinnerSize);
        syncRetry=label(Text.t("Повторить"));syncRetry.setTextSize(13);syncRetry.setFocusable(true);syncRetry.setTextColor(Accent.colour());syncRetry.setGravity(Gravity.CENTER);syncRetry.setMinHeight(dp(48));syncRetry.setPadding(dp(12),0,0,0);Motion.touch(syncRetry);
        syncRetry.setOnClickListener(v->{
            if(TtcuzProfileSync.state()==TtcuzProfileSync.State.HELD)verifyProfile();
            else TtcuzProfileSync.retryOwn();
            updateSyncUi();
        });row.addView(syncRetry);updateSyncUi();return sized(row,64);
    }
    private void updateSyncUi() {
        if(syncTitle==null)return;
        boolean shared=ProfileStyle.sharing();TtcuzProfileSync.State state=TtcuzProfileSync.state();
        boolean busy=shared && (state==TtcuzProfileSync.State.WAITING || state==TtcuzProfileSync.State.SENDING || state==TtcuzProfileSync.State.VERIFYING);
        String title=!shared ? Text.t("Только на этом устройстве") : state==TtcuzProfileSync.State.SENT ? Text.t("Отправлено")
                : state==TtcuzProfileSync.State.ERROR ? Text.t("Не отправлено") : state==TtcuzProfileSync.State.HELD ? Text.t("Нужно подтверждение")
                : state==TtcuzProfileSync.State.VERIFYING ? Text.t("Проверяю профиль") : busy ? Text.t("Отправляется") : Text.t("Ожидает отправки");
        String handle=Account.profileHandle();String account=handle==null?Text.t("Откройте свой профиль"):"@"+handle;
        // the same account as the rest of the profile section: the live one, or the last one seen while TikTok has not
        // said yet (in the first seconds after a start it had not, and a verified account read "not verified")
        String own=Account.liveId()!=null?Account.liveId():Account.id();
        String proof=own!=null&&new CloudProfileProvider(this).verified(own)?Text.t("Подтверждён"):Text.t("Не подтверждён");
        String details=account+" · "+proof+"\n"+(shared ? TtcuzProfileSync.status() : Text.t("Включите синхронизацию выше"));
        if(!title.contentEquals(syncTitle.getText())){syncTitle.setText(title);if(syncTitle.isShown())Motion.change(syncTitle);}
        if(!details.contentEquals(syncDetails.getText()))syncDetails.setText(details);
        syncSpinner.setVisibility(busy && Motion.enabled(this) ? View.VISIBLE : View.GONE);
        syncRetry.setVisibility(shared && !busy ? View.VISIBLE : View.GONE);
        String retryText=state==TtcuzProfileSync.State.HELD ? Text.t("Проверить") : Text.t("Повторить");
        if(!retryText.contentEquals(syncRetry.getText()))syncRetry.setText(retryText);
        boolean canRetry=state==TtcuzProfileSync.State.HELD || TtcuzProfileSync.canRetryOwn();
        syncRetry.setEnabled(canRetry);syncRetry.setAlpha(canRetry ? 1f : 0.45f);
    }

    private void changeCommunityBadge() {
        final String creator = Account.activeId();
        if (!AdminAccess.allowed(creator)) return;
        final CloudProfileProvider sync = new CloudProfileProvider(this);
        Toast.makeText(this, Text.t("Загружаю значки…"), Toast.LENGTH_SHORT).show();
        Net.away("badge catalog", () -> {
            java.util.List<CloudProfileProvider.CatalogItem> items = null;
            try {
                sync.recover(creator);
                if(!sync.verified(creator)) {runOnUiThread(()->{Screen.say(Text.t("Не найден сохранённый ключ этого устройства"));verifyProfile();});return;}
                items = sync.catalog(); } catch (Throwable error) {
                Diary.note("badge catalog: " + error);
            }
            final java.util.List<CloudProfileProvider.CatalogItem> found = items;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (found == null) {
                    Toast.makeText(this, Text.t("Не удалось загрузить значки. Проверьте сеть"), Toast.LENGTH_LONG).show();
                    return;
                }
                if(AdminAccess.current())showBadgePicker(sync, found);
            });
        });
    }

    /** Pick a badge from pictures, type or paste who gets it, one tap to give or take back. */
    private void showBadgePicker(final CloudProfileProvider sync,
                                 final java.util.List<CloudProfileProvider.CatalogItem> items) {
        if(!AdminAccess.current())return;
        final Panel panel=adminPanel=Panel.with(this,skin,Text.t("Выдать значок"));
        final EditText who=panel.field(Text.t("@ник, ссылка или UID"));
        panel.view(actionRow("arrow_forward",Text.t("Открыть профиль"),null,()->{
            String target=BadgeTarget.parse(who.getText().toString());
            if(target==null || target.matches("[0-9]+")){panel.busy(false,Text.t("Введите @ник получателя"));return;}
            try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.tiktok.com/@"+target)).setPackage(getPackageName()));}
            catch(Throwable error){panel.busy(false,Text.t("Не удалось открыть профиль в TikTok. Можно указать UID получателя"));}
        }));
        panel.view(actionRow("account_circle",Text.t("Недавние профили"),null,()->{
            java.util.List<String[]> recent=RecentProfiles.choices();
            if(recent.isEmpty()){panel.busy(false,Text.t("Откройте профиль получателя в TikTok, затем вернитесь сюда"));return;}
            CharSequence[] labels=new CharSequence[recent.size()];for(int i=0;i<labels.length;i++)labels[i]="@"+recent.get(i)[0];
            new ModDialog.Builder(this,skin).setTitle(Text.t("Получатель")).setItems(labels,(dialog,index)->who.setText(recent.get(index)[1])).show();
        }));
        LinearLayout preview=row();
        ImageView badgePicture=new ImageView(this);badgePicture.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams imageSize=new LinearLayout.LayoutParams(dp(36),dp(36));imageSize.rightMargin=dp(16);preview.addView(badgePicture,imageSize);
        LinearLayout caption=new LinearLayout(this);caption.setOrientation(LinearLayout.VERTICAL);
        TextView recipient=label(Text.t("Получатель"));recipient.setMaxLines(1);recipient.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView badgeTitle=detail(items.isEmpty() ? Text.t("Нет доступных значков") : items.get(0).title);
        caption.addView(recipient);caption.addView(badgeTitle);preview.addView(caption,grow());panel.view(preview);
        if(!items.isEmpty())badgePreview(badgePicture,items.get(0));
        who.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence text,int start,int count,int after){}
            public void onTextChanged(CharSequence text,int start,int before,int count){
                String target=BadgeTarget.parse(text.toString());
                recipient.setText(target==null ? Text.t("Получатель") : target.matches("[0-9]+") ? "UID "+target : "@"+target);
            }
            public void afterTextChanged(android.text.Editable text){}
        });
        final String[] chosen = { items.isEmpty() ? null : items.get(0).id };
        final java.util.List<LinearLayout> tiles = new java.util.ArrayList<LinearLayout>();

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        final int perRow = getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density<360 ? 3 : 4;
        LinearLayout rowView = null;
        for (int i = 0; i < items.size(); i++) {
            if (i % perRow == 0) {
                rowView = new LinearLayout(this);
                rowView.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rp.topMargin = i == 0 ? 0 : dp(8);
                grid.addView(rowView, rp);
            }
            final CloudProfileProvider.CatalogItem item = items.get(i);
            final LinearLayout tile = Panel.tile(this, skin, item.picture, item.colour, item.title, i == 0);
            tiles.add(tile);Motion.touch(tile);
            tile.setOnClickListener(v -> {
                tile.setSelected(true);
                chosen[0] = item.id;
                badgeTitle.setText(item.title);badgePreview(badgePicture,item);Motion.change(preview);
                for (int k = 0; k < tiles.size(); k++) {
                    CloudProfileProvider.CatalogItem other = items.get(k);
                    LinearLayout t = tiles.get(k);
                    android.graphics.drawable.GradientDrawable back = new android.graphics.drawable.GradientDrawable();
                    boolean on = other.id.equals(item.id);
                    t.setSelected(on);
                    back.setCornerRadius(dp(14));
                    back.setColor(skin.page);
                    back.setStroke(dp(on ? 2 : 1), on ? Accent.colour() : skin.muted());
                    t.setBackground(back);
                }
            });
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i % perRow != 0) tp.leftMargin = dp(8);
            rowView.addView(tile, tp);
        }
        // Keep the last row's tiles the same width as the others.
        int rest = items.size() % perRow;
        if (rest != 0 && rowView != null) {
            for (int k = rest; k < perRow; k++) {
                View gap = new View(this);
                LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(0, 1, 1f);
                gp.leftMargin = dp(8);
                rowView.addView(gap, gp);
            }
        }

        panel.view(grid);
        panel.primaryKeepOpen(Text.t("Выдать"), () -> pickAndSend(panel, sync, chosen[0], who, true));
        panel.keepOpen(Text.t("Снять этот значок"), () -> pickAndSend(panel, sync, chosen[0], who, false));
        panel.keepOpen(Text.t("Выданные значки"), () -> showAwardedBadges(sync));
        panel.quiet(Text.t("Добавить свою картинку"), this::pickCatalogBadge);
        panel.quiet(Text.t("Закрыть"), null);
        panel.show();
    }

    private void badgePreview(ImageView view,CloudProfileProvider.CatalogItem item) {
        view.clearColorFilter();view.setImageBitmap(item.picture);
        if(item.colour!=0)view.setColorFilter(item.colour,android.graphics.PorterDuff.Mode.SRC_IN);
        view.setContentDescription(item.title);
    }

    private void showAwardedBadges(CloudProfileProvider sync) {
        Net.away("badge awards",()->{
            try {org.json.JSONArray awards=sync.awards();runOnUiThread(()->{
                if(isFinishing()||isDestroyed()||!AdminAccess.current())return;
                Panel panel=Panel.with(this,skin,Text.t("Выданные значки"));
                if(awards.length()==0)panel.view(detail(Text.t("Пока нет выданных значков")));
                for(int i=0;i<awards.length();i++) {org.json.JSONObject award=awards.optJSONObject(i);if(award==null)continue;
                    String uid=award.optString("uid"),badge=award.optString("badgeId");
                    panel.view(actionRow("delete",award.optString("title",badge),"UID "+uid,()->submitBadge(panel,sync,badge,uid,false)));
                }panel.quiet(Text.t("Закрыть"),null);panel.show();
            });}catch(Throwable error){runOnUiThread(()->Screen.say(error.getMessage()==null?Text.t("Не удалось загрузить выданные значки"):error.getMessage()));}
        });
    }

    /** "@name", "name" or a tiktok.com/@name link: the handle in it, or null. */
    private static String handleFrom(String raw) {
        return BadgeTarget.parse(raw);
    }

    private void pickAndSend(Panel panel, CloudProfileProvider sync, String badgeId,
                             EditText who, boolean grant) {
        if(!AdminAccess.current()){panel.close();return;}
        final String handle = handleFrom(who.getText().toString());
        if (badgeId == null) {
            Toast.makeText(this, Text.t("Сначала выберите значок"), Toast.LENGTH_SHORT).show();
            return;
        }
        if (handle == null) {
            Toast.makeText(this, Text.t("Введите @ник, ссылку на профиль или UID"), Toast.LENGTH_LONG).show();
            return;
        }
        String raw=who.getText().toString().trim();
        submitBadge(panel, sync, badgeId, raw.startsWith("@")||raw.contains("/@")?"@"+handle:handle, grant);
    }

    private void submitBadge(Panel panel, CloudProfileProvider sync, final String badgeId,
                             final String handle, final boolean grant) {
        panel.busy(true, Text.t("Отправляю…"));
        Net.away("badge grant", () -> {
            boolean okay = false;
            String failure = Text.t("Не удалось связаться с сервером");
            try { okay = sync.setBadge(badgeId, handle, grant); } catch (Throwable error) {
                Diary.note("badge grant: " + error);
                failure = error.getMessage() == null ? failure : error.getMessage();
            }
            final boolean result = okay;
            final String message = failure;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                panel.busy(false, result ? (grant ? Text.t("Значок выдан: ") : Text.t("Значок снят: ")) + handle : message);
                if (result) Badges.refreshNow(getApplicationContext());
            });
        });
    }

    private void pickCatalogBadge() {
        if(!AdminAccess.current())return;
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*"); intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, PICK_CATALOG_BADGE);
        } catch (Throwable error) { Screen.say(Text.t("Не удалось открыть выбор картинки")); }
    }

    private void importCatalogBadge(Uri source) {
        if(!AdminAccess.current())return;
        Net.away("catalog image", () -> {
            try {
                byte[] png = BadgeImage.read(getApplicationContext(), source);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || !AdminAccess.current()) return;
                    Panel panel = adminPanel=Panel.with(this, skin, Text.t("Новый значок"));
                    ImageView preview = new ImageView(this);
                    preview.setImageBitmap(android.graphics.BitmapFactory.decodeByteArray(png,0,png.length));
                    preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    LinearLayout box = new LinearLayout(this); box.setGravity(Gravity.CENTER);
                    box.addView(preview, new LinearLayout.LayoutParams(dp(80),dp(80)));
                    panel.view(box).text(Text.t("Картинка появится в каталоге. Её можно будет выдавать другим пользователям."));
                    EditText title = panel.field(Text.t("Название значка"));
                    title.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(40)});
                    panel.primaryKeepOpen(Text.t("Добавить в каталог"), () -> {
                        if(!AdminAccess.current()){panel.close();return;}
                        String name = title.getText().toString().trim();
                        if (name.length() == 0) { title.setError(Text.t("Введите название")); return; }
                        panel.busy(true, Text.t("Загружаю значок…"));
                        Net.away("catalog upload", () -> {
                            String failure = null;
                            try { new CloudProfileProvider(this).createBadge(name,png); }
                            catch (Throwable error) { failure = error.getMessage(); if (failure == null) failure = Text.t("Не удалось загрузить значок"); }
                            final String problem = failure;
                            runOnUiThread(() -> {
                                if (isFinishing() || isDestroyed() || !AdminAccess.current()) return;
                                boolean showing = panel.dialog().isShowing();
                                if (showing) panel.busy(false,problem);
                                if (problem == null) {
                                    Badges.refreshNow(getApplicationContext());
                                    if (showing) { panel.close(); changeCommunityBadge(); }
                                    else Screen.say(Text.t("Значок добавлен в каталог"));
                                } else if (!showing) Screen.say(problem);
                            });
                        });
                    });
                    panel.quiet(Text.CANCEL,null); panel.show();
                });
            } catch (Throwable error) { Screen.say(error.getMessage() == null ? Text.t("Не удалось открыть картинку") : error.getMessage()); }
        });
    }

    /**
     * A setting that reveals or removes rows needs a rebuild, but doing it in
     * the same click that changed a switch cuts its thumb animation short.
     */
    private void rebuildAfterSwitchAnimation() {
        if (rebuildQueued) column.removeCallbacks(delayedRebuild);
        else rebuildQueued = true;
        column.postDelayed(delayedRebuild, M3Switch.ANIMATION_DURATION_MS);
    }

    // ----------------------------------------------------------- the rows

    private View switchRow() {
        LinearLayout row = row();
        final View lead = icon("language", true, 40);
        if (lead instanceof Expressive.Badge) ((Expressive.Badge) lead).set(Ttcuz.isEnabled(), false);
        row.addView(lead);
        row.addView(label(Text.CHANGE_REGION), grow());

        final M3Switch toggle = new M3Switch(this);
        toggle.colours(Accent.colour(), skin.muted(), skin.card);
        toggle.setChecked(Ttcuz.isEnabled());
        toggle.setOnChanged(checked -> {
            if (lead instanceof Expressive.Badge) ((Expressive.Badge) lead).set(checked, true);
            Ttcuz.setEnabled(checked);
            markChanged();
            rebuildAfterSwitchAnimation();
        });
        row.addView(toggle);

        row.setOnClickListener(v -> {
            toggle.performClick();
        });
        return sized(row, 52);
    }

    private View countryHead() {
        String[] current = Ttcuz.current();
        LinearLayout row = row();
        row.addView(flag(current[Ttcuz.ISO]));
        row.setAlpha(Ttcuz.isEnabled() ? 1f : 0.4f);

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(current[Ttcuz.LABEL]));
        text.addView(detail(current[Ttcuz.CARRIER] + "  ·  " + current[Ttcuz.MCCMNC]
                + "  ·  " + current[Ttcuz.ISO].toUpperCase(Locale.US)));
        row.addView(text, grow());

        row.addView(disclosure(countriesOpen,"countriesOpen"));

        if (Ttcuz.isEnabled()) {
            row.setOnClickListener(v -> {
                disclosureKey = "countriesOpen";
                countriesOpen = !countriesOpen;
                rebuildAfterReveal(false);
            });
        }
        return sized(row, 60);
    }

    private View countryRow(final String[] country) {
        boolean selected = country[Ttcuz.ISO].equals(Ttcuz.iso());
        LinearLayout row = row();
        row.addView(flag(country[Ttcuz.ISO]));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(country[Ttcuz.LABEL]));
        text.addView(detail(country[Ttcuz.CARRIER] + "  ·  " + country[Ttcuz.MCCMNC]
                + "  ·  " + country[Ttcuz.ISO].toUpperCase(Locale.US)));
        row.addView(text, grow());

        if (selected) row.addView(new Check(this, Accent.colour()));

        row.setOnClickListener(v -> {
            Ttcuz.setIso(country[Ttcuz.ISO]);
            countriesOpen = false;
            markChanged();
            rebuild();
        });
        return sized(row, 60);
    }

    /**
     * A country's flag, spelled rather than drawn.
     *
     * The two letters of a country's code have twin characters in the regional
     * indicator block, and a pair of those is a flag -- so `ru` becomes the
     * Russian flag with no picture involved, in whichever emoji font is on.
     * Which is also why it is worth having: with an emoji pack chosen in the
     * settings above, these are that pack's flags.
     */
    private View flag(String iso) {
        TextView view = new TextView(this);
        StringBuilder out = new StringBuilder();
        String code = iso.toUpperCase(Locale.US);
        for (int i = 0; i < code.length() && i < 2; i++) {
            int letter = code.charAt(i) - 'A';
            if (letter < 0 || letter > 25) return spacer();
            out.appendCodePoint(0x1F1E6 + letter);
        }
        view.setText(out.toString());
        // always Twemoji, whatever is chosen for the app: a phone's own flag
        // emoji is often two letters in a box, and this one is always a flag
        android.graphics.Typeface flags = Fonts.twemoji(this);
        if (flags != null) view.setTypeface(flags);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        view.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams size =
                new LinearLayout.LayoutParams(dp(34), ViewGroup.LayoutParams.WRAP_CONTENT);
        size.rightMargin = dp(10);
        view.setLayoutParams(size);
        return view;
    }

    private View spacer() {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(34), 1));
        return view;
    }

    private View disclosure(boolean expanded, String key) {
        renderDisclosureKey=expanded ? key : null;
        Chevron arrow=new Chevron(this,skin.muted(),expanded,key.equals(disclosureKey));
        disclosureArrows.put(key,arrow);return arrow;
    }

    private View accentHead() {
        LinearLayout row = row();
        row.addView(icon("palette"));
        row.addView(label(Text.ACCENT_COLOUR), grow());
        row.addView(new Dot(this, Accent.colour(), false));
        return sized(row, 52);
    }

    private View themeColoursHead() {
        LinearLayout row = row();
        row.addView(icon("tune"));
        row.addView(label(Text.THEME_COLOURS), grow());
        row.addView(disclosure(themeColoursOpen,"themeColoursOpen"));
        row.setContentDescription(Text.THEME_COLOURS);
        row.setFocusable(true);
        row.setOnClickListener(v -> {
            disclosureKey = "themeColoursOpen";
            themeColoursOpen = !themeColoursOpen;
            rebuildAfterReveal(themeColoursOpen);
        });
        return sized(row, 52);
    }

    // ------------------------------------------------------------ own badges

    private View mineHead() {
        LinearLayout row = row();
        row.addView(icon("favorite_border"));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(Text.MINE));
        text.addView(detail(Text.MINE_NOTE));
        row.addView(text, grow());

        row.addView(disclosure(mineOpen,"mineOpen"));

        row.setOnClickListener(v -> {
            disclosureKey = "mineOpen";
            mineOpen = !mineOpen;
            if (mineOpen) {
                ordering = null;
                Mine.ask(this::rebuild);
            }
            rebuildAfterReveal(mineOpen);
        });
        return sized(row, 60);
    }

    private View mineRows() {
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);

        // the list the server last sent wins over whatever was being
        // rearranged: the first answer on a cold start is empty, and without
        // this that empty answer is what stayed on screen
        if (Mine.tookFresh()) ordering = null;
        if (ordering == null) ordering = Mine.held();
        if (ordering.isEmpty()) {
            rows.addView(quiet(Mine.everAsked() ? Text.MINE_NONE : Text.MINE_WAIT));
            return rows;
        }

        for (int i = 0; i < ordering.size(); i++) {
            final int at = i;
            final Mine.Held one = ordering.get(i);
            if (i > 0) rows.addView(line());

            LinearLayout row = row();
            row.setPadding(dp(12), 0, dp(12), 0);

            Badges.Badge badge = Badges.byId(one.id);
            row.addView(badgeDot(badge));
            row.addView(label(badge == null ? one.id : badge.text), grow());

            // up and down rather than dragging: a row that can be dragged has
            // to fight the page it is on for the same gesture
            row.addView(mover(true, at > 0, () -> {
                java.util.Collections.swap(ordering, at, at - 1);
                rebuild();
            }));
            row.addView(mover(false, at < ordering.size() - 1, () -> {
                java.util.Collections.swap(ordering, at, at + 1);
                rebuild();
            }));

            M3Switch toggle = new M3Switch(this);
            toggle.colours(Accent.colour(), skin.muted(), skin.card);
            toggle.setChecked(one.shown, false);
            toggle.setOnChanged(checked -> {
                one.shown = checked;
                markChanged();
            });
            LinearLayout.LayoutParams size =
                    new LinearLayout.LayoutParams(dp(52), dp(32));
            size.leftMargin = dp(10);
            row.addView(toggle, size);

            rows.addView(sized(row, 60));
        }

        rows.addView(button(Text.MINE_SAVE, () -> Mine.save(ordering,
                (ok, trouble) -> {
                    Screen.say(ok ? Text.MINE_SAVED : Text.MINE_TOO_OFTEN);
                    ordering = null;
                    rebuild();
                })));
        return rows;
    }

    /** Something to press, rather than a row that happens to do something. */
    private View button(String title, final Runnable action) {
        TextView view = new TextView(this);
        view.setText(title);
        view.setTextColor(onAccent());
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, dp(15), 0, dp(15));
        // a full pill that squares off a little while it is held
        view.setBackground(Expressive.tile(this, Accent.colour(), 0x33FFFFFF, dp(28), dp(28), dp(12)));
        view.setOnClickListener(v -> action.run());

        LinearLayout holder = new LinearLayout(this);
        holder.setPadding(dp(16), dp(12), dp(16), dp(16));
        holder.addView(view, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return holder;
    }

    private View mover(boolean up, boolean can, final Runnable action) {
        android.widget.FrameLayout target=new android.widget.FrameLayout(this);
        Chevron arrow=new Chevron(this,can?skin.text:skin.muted(),up,false);
        android.widget.FrameLayout.LayoutParams image=new android.widget.FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER);
        target.addView(arrow,image);
        target.setLayoutParams(new LinearLayout.LayoutParams(dp(48),dp(48)));
        target.setContentDescription(up ? Text.t("Выше") : Text.t("Ниже"));
        target.setEnabled(can);
        if (can) {Motion.press(target);target.setOnClickListener(v -> action.run());}
        return target;
    }

    private View badgeDot(Badges.Badge badge) {
        android.widget.ImageView view = new android.widget.ImageView(this);
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(26), dp(26));
        size.rightMargin = dp(12);
        view.setLayoutParams(size);
        if (badge == null) return view;
        android.graphics.Bitmap picture = badge.image.length() == 0
                ? null : Badges.picture(this, badge.image);
        if (picture != null) {
            view.setImageBitmap(picture);
        } else {
            // a badge that names no picture of its own is the mod's own note,
            // which is the same thing it is drawn as beside a name
            view.setImageBitmap(Badges.note());
        }
        if (badge.colour != 0) {
            view.setColorFilter(badge.colour, android.graphics.PorterDuff.Mode.SRC_IN);
        }
        return view;
    }

    /** The one that is free until it is not. */
    private View freeBanner() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(Math.max(skin.radius, dp(18)));
        background.setColor(Expressive.mix(skin.card, Accent.colour(), 0.18f));
        card.setBackground(background);

        TextView head = new TextView(this);
        head.setText(Text.FREE_BADGE);
        head.setTextColor(skin.text);
        head.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        head.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(head);

        TextView body = new TextView(this);
        body.setText(Text.FREE_BADGE_TEXT);
        body.setTextColor(skin.text);
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        body.setPadding(0, dp(4), 0, 0);
        card.addView(body);

        TextView take = new TextView(this);
        take.setText(Text.FREE_BADGE_TAKE);
        take.setTextColor(onAccent());
        take.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        take.setTypeface(Typeface.DEFAULT_BOLD);
        take.setGravity(Gravity.CENTER);
        take.setPadding(0, dp(11), 0, dp(11));
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(Accent.colour());
        pill.setCornerRadius(dp(14));
        take.setBackground(pill);
        LinearLayout.LayoutParams below = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        below.topMargin = dp(14);
        take.setOnClickListener(v -> {
            if (!Mine.everAsked()) {
                Mine.ask(() -> Mine.takeFree((ok, trouble) -> {
                    Screen.say(ok ? Text.FREE_BADGE_GOT : Text.MINE_TOO_OFTEN);
                    rebuild();
                }));
                return;
            }
            Mine.takeFree((ok, trouble) -> {
                Screen.say(ok ? Text.FREE_BADGE_GOT : Text.MINE_TOO_OFTEN);
                rebuild();
            });
        });
        card.addView(take, below);

        LinearLayout holder = new LinearLayout(this);
        holder.setPadding(dp(20), dp(14), dp(20), dp(2));
        holder.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return holder;
    }

    private static int blend(int colour, int into, float how) {
        int red = (int) (((colour >> 16) & 0xFF) * (1 - how) + ((into >> 16) & 0xFF) * how);
        int green = (int) (((colour >> 8) & 0xFF) * (1 - how) + ((into >> 8) & 0xFF) * how);
        int blue = (int) ((colour & 0xFF) * (1 - how) + (into & 0xFF) * how);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    // -------------------------------------------------------- the colours

    private View wallpaperRow() {
        final int colour = Accent.fromWallpaper();
        LinearLayout row = row();
        row.addView(icon("wallpaper"));
        row.addView(label(Text.ACCENT_WALLPAPER), grow());
        row.addView(new Dot(this, colour, colour == Accent.colour()));
        row.setOnClickListener(v -> {
            Accent.set(colour);
            markChanged();
            rebuild();
        });
        return sized(row, 52);
    }

    /** The inline body expands with the disclosure arrow, as one group. */
    private View appear(View view) {
        boolean animated=!revealAnimationUsed && Motion.enabled(this);
        if(animated)revealAnimationUsed=true;
        DisclosureBody body=new DisclosureBody(this,view,animated);
        if(renderDisclosureKey!=null) {
            java.util.List<DisclosureBody> list=disclosureBodies.get(renderDisclosureKey);
            if(list==null){list=new java.util.ArrayList<>();disclosureBodies.put(renderDisclosureKey,list);}
            list.add(body);
        }
        return body;
    }

    private void rebuildAfterReveal(boolean expanded) {
        if(collapsing)return;
        java.util.List<DisclosureBody> bodies=disclosureBodies.get(disclosureKey);
        if(!expanded && bodies!=null && !bodies.isEmpty() && Motion.enabled(this)) {
            collapsing=true;
            Chevron arrow=disclosureArrows.get(disclosureKey);
            if(arrow!=null)arrow.animate().rotation(0f).setDuration(Motion.MEDIUM-60L).setInterpolator(Motion.BOTH).start();
            final int[] remaining={bodies.size()};
            for(DisclosureBody body:new java.util.ArrayList<>(bodies))body.collapse(()->{
                if(--remaining[0]==0){collapsing=false;revealAnimationRequested=false;disclosureKey=null;rebuild();}
            });
            return;
        }
        revealAnimationRequested=expanded;rebuild();disclosureKey=null;
    }

    private static final int CREATE_BACKUP = 0x4D42; // "MB"
    private static final int PICK_BACKUP = 0x4D49;   // "MI"

    private void createBackup() {
        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "ttcuz-settings.json");
            startActivityForResult(intent, CREATE_BACKUP);
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }

    private void pickBackup() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            startActivityForResult(intent, PICK_BACKUP);
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }

    private void reviewBackup(final Uri source) {
        Screen.progress(Text.BACKUP_READING, 0);
        Net.away("backup", () -> {
            final Backup.Review review = Backup.review(this, source);
            if (review.fits()) Screen.progressDone(); else Screen.progressGone();
            runOnUiThread(() -> {
                if (!review.fits()) {
                    Screen.say(Text.BACKUP_FAILED);
                    return;
                }
                new ModDialog.Builder(this, skin)
                        .setTitle(Text.BACKUP_IMPORT)
                        .setMessage(Text.backupPreview(review.count()))
                        .setNegativeButton(Text.CANCEL, null)
                        .setPositiveButton(Text.BACKUP_APPLY, (dialog, which) -> {
                            if (Backup.apply(this, review)) {
                                markChanged();
                                rebuild();
                                Screen.say(Text.BACKUP_DONE);
                            } else Screen.say(Text.BACKUP_FAILED);
                        })
                        .show();
            });
        });
    }

    private void pickResetGroup() {
        final String[] groups = {
                Text.RESET_APPEARANCE, Text.RESET_PLAYER, Text.RESET_FEED, Text.RESET_ALL
        };
        try {
            new ModDialog.Builder(this, skin)
                    .setTitle(Text.BACKUP_RESET)
                    .setMessage(Text.BACKUP_RESET_ASK)
                    .setItems(groups, (dialog, chosen) -> askToReset(chosen + 1))
                    .setNegativeButton(Text.CANCEL, null)
                    .show();
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }

    private void askToReset(final int group) {
        new ModDialog.Builder(this, skin)
                .setTitle(Text.BACKUP_RESET)
                .setMessage(Text.BACKUP_RESET_ASK)
                .setNegativeButton(Text.CANCEL, null)
                .setPositiveButton(Text.BACKUP_APPLY, (dialog, which) -> {
                    Backup.reset(this, group);
                    markChanged();
                    rebuild();
                })
                .show();
    }

    /** A value between nothing and something, set by dragging. */
    private interface Chosen {
        void at(int value);
    }

    private View slider(String title, int now, int most, final Chosen chosen) {
        return slider(title, now, most, 0, "", chosen);
    }

    /** Ползунок с живым числом: показывает (положение + shift) и единицу измерения. */
    private View slider(String title, int now, int most, final int shift, final String unit, final Chosen chosen) {
        return slider("tune", title, now, most, shift, unit, chosen);
    }

    /** The same, with its own icon in front so no row of the list is left bare. */
    private View slider(String picture, String title, int now, int most, final int shift, final String unit, final Chosen chosen) {
        return slider(picture,title,now,most,shift,unit,chosen,false);
    }
    private View liveSlider(String picture,String title,int now,int most,int shift,String unit,Chosen chosen){
        return slider(picture,title,now,most,shift,unit,chosen,true);
    }
    private View slider(String picture, String title, int now, int most, final int shift, final String unit, final Chosen chosen, final boolean live) {
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(16), dp(8), dp(20), dp(4));
        rows.setClipChildren(false);rows.setClipToPadding(false);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(icon(picture));
        head.addView(label(title), new LinearLayout.LayoutParams(0,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView number = new TextView(this);
        number.setTextColor(accentInk());
        number.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
        number.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        number.setText((Math.min(now, most) + shift) + unit);
        number.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        number.setMinWidth(dp(48));
        head.addView(number);
        rows.addView(head);

        android.widget.SeekBar bar = Expressive.slider(this, Accent.colour(),
                Expressive.mix(skin.card, skin.text, 0.2f));
        bar.setMax(most);
        bar.setProgress(Math.min(now, most));
        bar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seek, int value, boolean human) {
                number.setText((value + shift) + unit);
                if(live && human)chosen.at(value);
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seek) {
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seek) {
                if(!live)chosen.at(seek.getProgress());
            }
        });
        // Same left edge as the title above it (icon 40dp + 16dp gap), same right edge as the number.
        LinearLayout.LayoutParams barSize = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        barSize.leftMargin = dp(56);
        rows.addView(bar, barSize);
        return rows;
    }

    /** How far the chosen background sits from the theme's own extreme. */
    private View strengthRow() {
        return slider("contrast", Text.THEME_STRENGTH, Themes.strength(), 100, 0, "%", value -> {
            Themes.setStrength(value);
            markChanged();
        });
    }

    // ---------------------------------------------------------- the font

    private String nickScope = "";
    /** The accent gradient's details are folded away until asked for. */
    private boolean gradientOpen;
    private static final int PICK_ISLAND = 0x4D53;  // "MS"
    private static final int PICK_COMMENT_BACKGROUND = 0x4D43, PICK_PLUGIN = 0x4D50, PICK_NICK_FONT = 0x4D4E;
    private static final int PICK_FONT = 0x4D46;  // "MF"
    private static final int PICK_EMOJI = 0x4D45;  // "ME"
    private static final int PICK_PROFILE_BADGE = 0x5450; // "TP"

    private void pickProfileBadge() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // a picture, a GIF (stays animated) or a video (its first three seconds become a GIF)
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        startActivityForResult(intent, PICK_PROFILE_BADGE);
    }

    private void chooseProfileColour(final boolean first) {
        showProfileColourPicker(first ? Text.PROFILE_START : Text.PROFILE_END,
                first ? ProfileStyle.start() : ProfileStyle.end(), colour -> {
                    ProfileStyle.colours(first ? colour : ProfileStyle.start(),
                            first ? ProfileStyle.end() : colour);
                    markChanged();
                    rebuild();
                });
    }

    private void chooseProfileMiddle() {
        showProfileColourPicker(Text.PROFILE_MIDDLE, ProfileStyle.middle(), colour -> {
            ProfileStyle.setMiddle(colour);
            markChanged();
            rebuild();
        });
    }

    private GradientDrawable colourPreview(int colour) {
        GradientDrawable preview = new GradientDrawable();
        preview.setColor(colour);
        preview.setCornerRadius(dp(12));
        preview.setStroke(dp(1), skin.muted());
        return preview;
    }

    private int contrastingText(int colour) {
        float luminance = (0.299f * Color.red(colour) + 0.587f * Color.green(colour)
                + 0.114f * Color.blue(colour)) / 255f;
        return luminance > 0.55f ? Color.BLACK : Color.WHITE;
    }

    /**
     * Every gradient colour -- first, middle, last -- is chosen on the same HSV
     * palette as the accent: no colour codes, swatches or eyedropper.
     */
    private String nicknameFontName() {
        String chosen = NicknameFont.chosen();
        if (chosen.isEmpty()) return Text.t("Как в приложении");
        return Fonts.FILE.equals(chosen) ? Text.t("Свой файл") : chosen;
    }

    private void chooseNicknameFont() {
        final String[] presets = java.util.Arrays.copyOfRange(Fonts.PRESETS, 1, Fonts.PRESETS.length);
        String[] names = new String[presets.length + 2];
        names[0] = Text.t("Как в приложении");
        System.arraycopy(presets, 0, names, 1, presets.length);
        names[names.length - 1] = Text.t("Свой файл…");
        new ModDialog.Builder(this).setItems(names, (dialog, index) -> {
            if (index == 0) NicknameFont.choose("");
            else if (index == names.length - 1) {
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
                nickScope = AccountAppearance.scope(this);
                try { startActivityForResult(pick, PICK_NICK_FONT); } catch (RuntimeException e) { Screen.say(Text.t("Не удалось открыть файлы")); }
                return;
            } else NicknameFont.choose(presets[index - 1]);
            markChanged(); rebuild();
        }).show();
    }

    private void showProfileColourPicker(String title, int initial, final ColourPicked picked) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(8));

        final AccentColourPicker picker = new AccentColourPicker(this, initial);
        final TextView sample = detail(String.format(Locale.US, "#%06X", initial & 0xFFFFFF));
        sample.setGravity(Gravity.CENTER);
        sample.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        sample.setTextColor(contrastingText(initial));
        sample.setBackground(colourPreview(initial));
        LinearLayout.LayoutParams previewSize = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        previewSize.bottomMargin = dp(12);
        content.addView(sample, previewSize);
        content.addView(picker, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(238)));
        picker.onColourChanged(colour -> {
            sample.setText(String.format(Locale.US, "#%06X", colour & 0xFFFFFF));
            sample.setTextColor(contrastingText(colour));
            sample.setBackground(colourPreview(colour));
        });

        new ModDialog.Builder(this, skin).setTitle(title).setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> picked.set(picker.colour()))
                .show();
    }

    private static final String[] APPEARS = {"Без эффекта", "Проявление", "Подъём", "Выпрыгивание", "Печать по буквам"};
    private static final String[] PARTICLE_NAMES = {"Без частиц", "Искры", "Снег", "Сердечки", "Звёзды", "Свои"};
    private static final int PICK_PARTICLE = 0x5051;

    /** The editor of the person's own particles; "Свои" in the list opens it too. */
    private void openParticleEditor() {
        String name = Account.profileName();
        ParticleEditor.open(this, skin, ProfileStyle.displayName(Account.liveId(), name == null ? "" : name),
                () -> {
                    Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");
                    try { startActivityForResult(pick, PICK_PARTICLE); } catch (RuntimeException e) { Screen.say(Text.t("Не удалось открыть файлы")); }
                },
                (initial, chosen) -> showProfileColourPicker(Text.t("Цвет частиц"), initial, chosen::accept),
                () -> { markChanged(); rebuild(); });
    }

    private interface Picked { void set(int which); }

    /** As choose(), for a setting that takes effect at once: no restart offered. */
    private void chooseLive(String title, String[] names, int current, Picked chosen) {
        String[] choices = new String[names.length];
        for (int i = 0; i < choices.length; i++) choices[i] = Text.t(names[i]);
        new ModDialog.Builder(this, skin).setTitle(title)
                .setSingleChoiceItems(choices, current, (dialog, which) -> { chosen.set(which); rebuild(); dialog.dismiss(); })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private static final int BEAT_PERMISSION = 0x7442;

    /** Beat vibration reads TikTok's sound through Android's Visualizer, which needs "record audio" granted first. */
    private void setBeat(boolean on) {
        if (on && !BeatHaptics.allowed(this)) {
            Screen.say(Text.t("Android спросит про микрофон: так мод слышит музыку TikTok. Сам микрофон не включается"));
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, BEAT_PERMISSION);
            return;
        }
        BeatHaptics.set(on);
        rebuild();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code != BEAT_PERMISSION) return;
        boolean granted = results.length > 0 && results[0] == android.content.pm.PackageManager.PERMISSION_GRANTED;
        BeatHaptics.set(granted);
        if (!granted) Screen.say(Text.t("Без этого разрешения вибрация в ритм не работает"));
        rebuild();
    }

    private void choose(String title, String[] names, int current, Picked chosen) {
        String[] choices = new String[names.length];
        for (int i = 0; i < choices.length; i++) choices[i] = Text.t(names[i]);
        new ModDialog.Builder(this, skin).setTitle(title)
                .setSingleChoiceItems(choices, current, (dialog, which) -> { chosen.set(which); markChanged(); rebuild(); dialog.dismiss(); })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    /** The own profile at the top of the settings: the nickname as others see it, one tap to all of its settings. */
    /** The own profile at the top of the main page: the avatar, the nickname as others see it, the handle. */
    private View myProfileCard() {
        LinearLayout box = card();
        final LinearLayout row = row();
        row.setPadding(dp(14), dp(12), dp(16), dp(12));

        ImageView avatar = new ImageView(this);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setVisibility(View.INVISIBLE);
        TextView initial = new TextView(this);
        initial.setGravity(Gravity.CENTER);
        initial.setTextColor(skin.text);
        initial.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        String name = Account.profileName();
        boolean live = name != null && !name.isEmpty();
        initial.setText(live ? name.substring(0, 1).toUpperCase(Locale.ROOT) : "•");
        FrameLayout face = new FrameLayout(this) {
            final android.graphics.Path shape = new android.graphics.Path();
            @Override public void draw(Canvas canvas) {
                shape.reset();
                AvatarShape.addCircle(shape, getWidth() / 2f, getHeight() / 2f, Math.min(getWidth(), getHeight()) / 2f, android.graphics.Path.Direction.CW);
                int save = canvas.save(); canvas.clipPath(shape); super.draw(canvas); canvas.restoreToCount(save);
            }
        };
        face.setBackgroundColor(blend(skin.muted(), skin.card, 0.72f));
        face.addView(initial, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        face.addView(avatar, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams faceSize = new LinearLayout.LayoutParams(dp(52), dp(52));
        faceSize.rightMargin = dp(14);
        row.addView(face, faceSize);

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        TextView nickname = label(live ? name : Text.t("Мой профиль"));
        nickname.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        nickname.setTypeface(Typeface.DEFAULT_BOLD);
        nickname.setSingleLine(true);
        nickname.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (live) Badge.previewNickname(nickname, name, Account.id());
        text.addView(nickname);
        TextView handle = detail(Account.profileHandle() == null ? Text.t("Значок, ник и эффекты") : "@" + Account.profileHandle());
        handle.setSingleLine(true);
        text.addView(handle);
        row.addView(text, grow());
        row.addView(away());
        ProfilePreview.bind(this, avatar, initial, nickname, handle);
        row.setContentDescription(Text.t("Профиль"));
        row.setOnClickListener(v -> openShelf(2, row));
        box.addView(sized(row, 76));
        return box;
    }

    private void chooseCommentColour() {
        final int now = ProfileStyle.commentColour();
        Runnable pick = () -> showProfileColourPicker(Text.t("Цвет комментариев"), now == 0 ? 0xFF25F4EE : now, colour -> {
            ProfileStyle.setCommentColour(colour);
            markChanged();
            rebuild();
        });
        if (now == 0) { pick.run(); return; }
        Panel.with(this, skin, Text.t("Цвет комментариев"))
                .primary(Text.t("Выбрать цвет"), pick::run)
                .quiet(Text.t("Как в TikTok"), () -> { ProfileStyle.setCommentColour(0); markChanged(); rebuild(); })
                .show();
    }

    /** A tag of one's own: the server says whether it is free, and keeps it for this account only. */
    private void chooseTag() {
        final android.widget.EditText field = new android.widget.EditText(this);
        field.setText(ProfileStyle.tag());
        field.setSingleLine(true);
        field.setTextColor(skin.text);
        field.setHint(Text.t("Например, ttcuz"));
        field.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(16)});
        Panel ask = Panel.with(this, skin, Text.t("Тег у ника"));
        ask.text(Text.t("Его увидят все с модом. Тег один на всех: если его уже кто-то занял, взять его нельзя. Нужен подтверждённый профиль."));
        ask.view(field);
        ask.primary(Text.t("Занять"), () -> claimTag(field.getText().toString().trim()));
        if (!ProfileStyle.tag().isEmpty()) ask.quiet(Text.t("Убрать тег"), () -> claimTag(""));
        else ask.quiet(Text.t("Назад"), null);
        ask.show();
    }

    private void claimTag(final String wanted) {
        final String uid = Account.id();
        if (uid == null) { Screen.say(Text.t("Войдите в аккаунт TikTok")); return; }
        Net.away("tag claim", () -> {
            try {
                String tag = new CloudProfileProvider(this).claimTag(uid, wanted);
                runOnUiThread(() -> {
                    ProfileStyle.setTag(tag);
                    Screen.say(tag.isEmpty() ? Text.t("Тег убран") : Text.t("Тег ваш") + ": " + tag);
                    markChanged();
                    rebuild();
                });
            } catch (final Exception error) {
                runOnUiThread(() -> Screen.say(error.getMessage()));
            }
        });
    }

    private static final String[] MOTIONS = {"Перелив", "Качание влево-вправо", "Дыхание", "Вращение", "Блик"};
    private String gradientMotion(int motion) { return Text.t(MOTIONS[Math.max(0, Math.min(MOTIONS.length - 1, motion))]); }

    private void chooseGradientMotion() {
        String[] choices = new String[MOTIONS.length];
        for (int i = 0; i < choices.length; i++) choices[i] = Text.t(MOTIONS[i]);
        new ModDialog.Builder(this, skin).setTitle(Text.t("Анимация градиента"))
                .setSingleChoiceItems(choices, ProfileStyle.motion(), (dialog, which) -> {
                    ProfileStyle.setMotion(which);
                    markChanged();
                    rebuild();
                    dialog.dismiss();
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private String profileDirection(int direction) {
        if (direction == 1) return Text.PROFILE_DIRECTION_DOWN;
        if (direction == 2) return Text.PROFILE_DIRECTION_DIAGONAL;
        if (direction == 3) return Text.PROFILE_DIRECTION_REVERSE;
        return Text.PROFILE_DIRECTION_RIGHT;
    }

    private void chooseProfileDirection() {
        String[] choices = {Text.PROFILE_DIRECTION_RIGHT, Text.PROFILE_DIRECTION_DOWN,
                Text.PROFILE_DIRECTION_DIAGONAL, Text.PROFILE_DIRECTION_REVERSE};
        new ModDialog.Builder(this, skin).setTitle(Text.PROFILE_DIRECTION)
                .setSingleChoiceItems(choices, ProfileStyle.direction(), (dialog, which) -> {
                    ProfileStyle.setDirection(which);
                    markChanged();
                    rebuild();
                    dialog.dismiss();
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private View fontHead() {
        LinearLayout row = row();
        row.addView(icon("text_fields"));
        row.addView(label(Text.FONT), grow());

        TextView now = detail(fontName(Fonts.name()));
        now.setTextColor(skin.text);
        row.addView(now);
        return sized(row, 52);
    }

    private static String fontName(String which) {
        if (Fonts.SANS.equals(which)) return Text.FONT_SANS;
        if (Fonts.SANS_LIGHT.equals(which)) return Text.FONT_SANS_LIGHT;
        if (Fonts.SANS_CONDENSED.equals(which)) return Text.FONT_SANS_CONDENSED;
        if (Fonts.SERIF.equals(which)) return Text.FONT_SERIF;
        if (Fonts.MONOSPACE.equals(which)) return Text.FONT_MONOSPACE;
        if (Fonts.CURSIVE.equals(which)) return Text.FONT_CURSIVE;
        if (Fonts.FILE.equals(which)) return Text.FONT_FILE;
        return Text.FONT_SYSTEM;
    }

    private View fontChoices() {
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(12), dp(4), dp(12), dp(12));

        // the fonts one under another, each showing itself: "Аа" big, its name, a line written in it
        String now = Fonts.name();
        java.util.List<View> fonts = new java.util.ArrayList<>();
        for (final String which : Fonts.PRESETS)
            fonts.add(fontTile(fontName(which), Fonts.SYSTEM.equals(which) ? null : which, which.equals(now), () -> {
                Fonts.choose(which);
                markChanged();
                rebuild();
            }));
        fonts.add(fontTile(Text.FONT_PICK, Fonts.FILE.equals(now) && Fonts.file(this).isFile() ? Fonts.FILE : "", Fonts.FILE.equals(now), this::pickFont));
        rows.addView(tileGrid(fonts, 1));

        // the emoji: a card per pack with its own emoji, downloaded on the spot when it is not here yet
        TextView head = detail(Text.EMOJI);
        head.setTextColor(accentInk());
        head.setPadding(dp(4), dp(18), dp(4), dp(8));
        rows.addView(head);
        emojiCards.clear();
        java.util.List<View> cards = new java.util.ArrayList<>();
        for (EmojiPackManager.Pack pack : EmojiPackManager.packs(this)) cards.add(emojiCard(pack));
        rows.addView(tileGrid(cards, 2));
        paintEmojiCards();
        TextView note = quiet(Text.EMOJI_NOTE);
        rows.addView(note);
        return rows;
    }

    /** Tiles in rows of `columns`, all the same width, 8 dp apart. */
    private View tileGrid(java.util.List<View> tiles, int columns) {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < tiles.size(); i += columns) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            for (int k = 0; k < columns; k++) {
                LinearLayout.LayoutParams cell = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                if (k > 0) cell.leftMargin = dp(8);
                line.addView(i + k < tiles.size() ? tiles.get(i + k) : new View(this), cell);
            }
            LinearLayout.LayoutParams row = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) row.topMargin = dp(8);
            grid.addView(line, row);
        }
        return grid;
    }

    /** A rounded tile: tonal when chosen, with a thin accent outline; a soft surface otherwise. */
    private android.graphics.drawable.Drawable tileBack(boolean chosen) {
        GradientDrawable frame = new GradientDrawable();
        frame.setCornerRadius(dp(20));
        frame.setColor(chosen ? accentTonal() : Expressive.mix(skin.card, skin.text, 0.05f));
        frame.setStroke(dp(chosen ? 2 : 1), chosen ? Accent.colour() : Expressive.mix(skin.card, skin.text, 0.08f));
        return new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf((Accent.colour() & 0xFFFFFF) | 0x33000000), frame, null);
    }

    /** "Аа" in the font itself over its name; `face` is a family, "" for a file not picked yet, null for the system's. */
    private View fontTile(String name, String face, boolean chosen, final Runnable action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(18), dp(14), dp(18), dp(14));
        tile.setMinimumHeight(dp(76));
        tile.setBackground(tileBack(chosen));
        TextView letters = new TextView(this);
        letters.setText(face != null && face.isEmpty() ? "+" : "Аа");
        letters.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        letters.setTextColor(chosen ? accentInk() : skin.text);
        letters.setGravity(Gravity.CENTER);
        try {
            if (Fonts.FILE.equals(face)) letters.setTypeface(android.graphics.Typeface.createFromFile(Fonts.file(this)));
            else if (face != null && !face.isEmpty()) letters.setTypeface(android.graphics.Typeface.create(face, android.graphics.Typeface.NORMAL));
        } catch (Throwable ignored) {}
        tile.addView(letters, new LinearLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(14), 0, 0, 0);
        TextView label = new TextView(this);
        label.setText(name);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        label.setTextColor(chosen ? accentInk() : skin.text);
        label.setSingleLine(true);
        words.addView(label);
        // a line in the font itself: how a comment or a caption will read in it
        TextView sample = new TextView(this);
        sample.setText(face != null && face.isEmpty() ? Text.t("Свой файл .ttf или .otf") : Text.t("Съешь же ещё этих мягких булок"));
        sample.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        sample.setTextColor(chosen ? accentInk() : skin.muted());
        sample.setSingleLine(true);
        sample.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (face == null || !face.isEmpty()) sample.setTypeface(letters.getTypeface());
        LinearLayout.LayoutParams under = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        under.topMargin = dp(2);
        words.addView(sample, under);
        tile.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (chosen) {
            ImageView tick = new ImageView(this);
            tick.setImageDrawable(Symbols.drawable("check_circle_fill1", Accent.colour()));
            tile.addView(tick, new LinearLayout.LayoutParams(dp(24), dp(24)));
        }
        tile.setContentDescription(name);
        tile.setOnClickListener(v -> action.run());
        Motion.press(tile);
        return tile;
    }

    /** The emoji cards on the page, to repaint the chosen one in place (no rebuild, so nothing jumps). */
    private final java.util.List<EmojiCard> emojiCards = new java.util.ArrayList<>();

    private final class EmojiCard {
        final EmojiPackManager.Pack pack;
        final LinearLayout view;
        final TextView sample, name, state;
        final Ring ring;
        EmojiCard(EmojiPackManager.Pack pack, LinearLayout view, TextView sample, TextView name, TextView state, Ring ring) {
            this.pack = pack; this.view = view; this.sample = sample; this.name = name; this.state = state; this.ring = ring;
        }
    }

    private static String megabytes(long bytes) {
        return String.format(java.util.Locale.ROOT, bytes >= 10L * 1048576 ? "%.0f" : "%.1f", bytes / 1048576f) + " " + Text.t("МБ");
    }

    /**
     * A pack's card: three of its emoji, its name, what it is (in the app, its size to download, downloaded), and in
     * the corner a ring that is a download button, then the download itself, then a check.
     */
    private View emojiCard(final EmojiPackManager.Pack pack) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(12), dp(14));
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView sample = new TextView(this);
        sample.setText(Fonts.EMOJI_FILE.equals(pack.id) ? "📁" : "😀😍🔥");
        sample.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        sample.setSingleLine(true);
        top.addView(sample, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Ring ring = new Ring(this, Accent.colour(), accentInk(), Expressive.mix(skin.card, skin.text, 0.12f));
        top.addView(ring, new LinearLayout.LayoutParams(dp(36), dp(36)));
        card.addView(top);
        TextView name = new TextView(this);
        name.setText(emojiName(pack));
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setPadding(0, dp(10), 0, 0);
        card.addView(name);
        TextView state = new TextView(this);
        state.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        state.setSingleLine(true);
        card.addView(state);
        final EmojiCard holder = new EmojiCard(pack, card, sample, name, state, ring);
        emojiCards.add(holder);
        // a download left running when the page was rebuilt goes on in its card
        if (pack.id.equals(EmojiPackManager.fetching)) follow(holder);
        card.setOnClickListener(v -> tapEmoji(holder));
        Motion.press(card);
        return card;
    }

    /** Every card as things stand: chosen outline and check, the size or "downloaded", the pack's own emoji. */
    private void paintEmojiCards() {
        String chosen = Fonts.emoji();
        for (EmojiCard c : emojiCards) {
            boolean on = c.pack.id.equals(chosen), ready = Fonts.EMOJI_FILE.equals(c.pack.id) || EmojiPackManager.ready(this, c.pack.id);
            c.view.setBackground(tileBack(on));
            c.name.setTextColor(on ? accentInk() : skin.text);
            String what = Fonts.SYSTEM.equals(c.pack.id) ? Text.t("Как в телефоне")
                    : Fonts.EMOJI_FILE.equals(c.pack.id) ? (ready && on ? Text.t("Выбран файл") : Text.t("Открыть свой .ttf"))
                    : c.pack.remote() ? (ready ? Text.t("Скачан") : megabytes(c.pack.size)) : Text.t("В приложении");
            if (!c.pack.id.equals(EmojiPackManager.fetching)) c.state.setText(what);
            c.state.setTextColor(skin.muted());
            android.graphics.Typeface face = Fonts.emojiPreview(this, c.pack.id);
            c.sample.setTypeface(face);
            c.sample.setAlpha(face != null || Fonts.SYSTEM.equals(c.pack.id) || Fonts.EMOJI_FILE.equals(c.pack.id) ? 1f : .45f);
            if (!c.pack.id.equals(EmojiPackManager.fetching))
                c.ring.state(on ? Ring.CHOSEN : c.pack.remote() && !ready ? Ring.GET : Ring.NONE);
        }
    }

    private void tapEmoji(final EmojiCard c) {
        if (Fonts.EMOJI_FILE.equals(c.pack.id)) { pickEmoji(); return; }
        if (c.pack.remote() && !EmojiPackManager.ready(this, c.pack.id)) {
            if (EmojiPackManager.fetching != null) { if (!c.pack.id.equals(EmojiPackManager.fetching)) Screen.say(Text.t("Уже скачивается другой набор")); return; }
            EmojiPackManager.download(getApplicationContext(), c.pack, null, ok -> {});
            follow(c);
            return;
        }
        Fonts.chooseEmoji(this, c.pack.id);
        markChanged();
        paintEmojiCards();
    }

    /** Follows the running download in its card: the ring fills smoothly, the percent under the name, then the check. */
    private void follow(final EmojiCard c) {
        c.ring.state(Ring.LOADING);
        c.ring.to(EmojiPackManager.fetched);
        final Runnable[] tick = new Runnable[1];
        tick[0] = () -> {
            if (!c.view.isAttachedToWindow()) return;
            if (c.pack.id.equals(EmojiPackManager.fetching)) {
                int at = EmojiPackManager.fetched;
                c.ring.to(at);
                c.state.setText(at + "% · " + megabytes(c.pack.size));
                c.view.postDelayed(tick[0], 120L);
                return;
            }
            if (EmojiPackManager.ready(this, c.pack.id)) {
                // in: the ring closes into a check, and the pack is the one in use
                c.ring.to(100);
                c.ring.finish(() -> {
                    Fonts.chooseEmoji(this, c.pack.id);
                    markChanged();
                    paintEmojiCards();
                    Motion.change(c.view);
                });
            } else {
                Screen.say(Text.t("Не удалось скачать эмодзи"));
                paintEmojiCards();
            }
        };
        c.view.postDelayed(tick[0], 120L);
    }

    /**
     * The corner of an emoji card. GET: a download arrow in a tonal circle. LOADING: an arc that eases toward the
     * percent by the frame's own time (no steps however the bytes come), with the number inside; while nothing has
     * come yet a short arc turns. Then the arc closes and becomes a check. CHOSEN: the check in the accent.
     */
    private static final class Ring extends View {
        static final int NONE = 0, GET = 1, LOADING = 2, CHOSEN = 3;
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Paint text = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.RectF arc = new android.graphics.RectF();
        private final int accent, ink, track;
        private int state = NONE;
        private float shown, wanted, spin, check;
        private long last;
        private Runnable done;

        Ring(Context context, int accent, int ink, int track) {
            super(context);
            this.accent = accent; this.ink = ink; this.track = track;
            text.setTextAlign(android.graphics.Paint.Align.CENTER);
            text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }

        void state(int value) {
            if (value == state) return;
            state = value;
            if (value == LOADING) { shown = 0f; wanted = 0f; check = 0f; done = null; }
            if (value == CHOSEN) check = 1f;
            last = 0L;
            invalidate();
        }

        void to(int percent) { wanted = Math.max(wanted, Math.max(0, Math.min(100, percent)) / 100f); postInvalidateOnAnimation(); }

        void finish(Runnable then) { done = then; postInvalidateOnAnimation(); }

        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight(), d = getResources().getDisplayMetrics().density;
            float cx = w / 2f, cy = h / 2f, r = Math.min(w, h) / 2f - 2.5f * d;
            long now = android.os.SystemClock.uptimeMillis();
            float dt = last == 0L ? 0.016f : Math.min(0.05f, (now - last) / 1000f);
            last = now;
            boolean again = false;
            if (state == GET) {
                paint.setStyle(android.graphics.Paint.Style.FILL);
                paint.setColor(Expressive.mix(track, accent, .35f));
                canvas.drawCircle(cx, cy, r + 1.5f * d, paint);
                android.graphics.Path arrow = Symbols.path("download");
                if (arrow != null) {
                    paint.setColor(ink);
                    float s = r * 1.15f;
                    int k = canvas.save();
                    canvas.translate(cx - s / 2f, cy - s / 2f);
                    canvas.scale(s / 24f, s / 24f);
                    canvas.drawPath(arrow, paint);
                    canvas.restoreToCount(k);
                }
            } else if (state == LOADING) {
                // eased by time: about a fifth of a second to catch up, whatever the frame rate
                shown += (wanted - shown) * (1f - (float) Math.exp(-dt * 9f));
                if (Math.abs(wanted - shown) < .001f) shown = wanted;
                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
                paint.setStrokeWidth(3f * d);
                arc.set(cx - r, cy - r, cx + r, cy + r);
                paint.setColor(track);
                canvas.drawCircle(cx, cy, r, paint);
                paint.setColor(accent);
                if (shown < .01f) {
                    spin = (spin + dt * 300f) % 360f;
                    canvas.drawArc(arc, spin - 90f, 70f, false, paint);
                    again = true;
                } else canvas.drawArc(arc, -90f, 360f * shown, false, paint);
                if (done != null && shown >= .999f) {
                    check = Math.min(1f, check + dt * 4f);
                    if (check >= 1f) { Runnable then = done; done = null; state = CHOSEN; post(then); }
                    again = true;
                }
                if (check < .01f) {
                    text.setColor(ink);
                    text.setTextSize(10.5f * d);
                    canvas.drawText(Math.round(shown * 100) + "", cx, cy - (text.descent() + text.ascent()) / 2f, text);
                } else drawCheck(canvas, cx, cy, r * .55f * check, d, ink);
                again |= shown != wanted;
            } else if (state == CHOSEN) {
                paint.setStyle(android.graphics.Paint.Style.FILL);
                paint.setColor(accent);
                canvas.drawCircle(cx, cy, r + 1.5f * d, paint);
                drawCheck(canvas, cx, cy, r * .55f, d, Expressive.mix(accent, 0xFFFFFFFF, .9f));
            }
            if (again) postInvalidateOnAnimation();
        }

        private void drawCheck(Canvas canvas, float cx, float cy, float size, float d, int colour) {
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
            paint.setStrokeWidth(2.4f * d);
            paint.setColor(colour);
            android.graphics.Path tick = new android.graphics.Path();
            tick.moveTo(cx - size, cy);
            tick.lineTo(cx - size * .3f, cy + size * .7f);
            tick.lineTo(cx + size, cy - size * .6f);
            canvas.drawPath(tick, paint);
        }
    }

    /** A thin MD3 linear progress bar whose fill eases to each new value by the frame's time and shimmers while it waits. */
    private static final class DownloadBar extends View {
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final int fill, track;
        private float shown, wanted;
        private long last, started = android.os.SystemClock.uptimeMillis();
        DownloadBar(Context context, int fill, int track) { super(context); this.fill = fill; this.track = track; }
        void set(int percent) { wanted = Math.max(0, Math.min(100, percent)) / 100f; invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            long now = android.os.SystemClock.uptimeMillis();
            float dt = last == 0L ? 0.016f : Math.min(0.05f, (now - last) / 1000f);
            last = now;
            float w = getWidth(), h = getHeight(), r = h / 2f;
            shown += (wanted - shown) * (1f - (float) Math.exp(-dt * 9f));
            if (Math.abs(wanted - shown) < 0.001f) shown = wanted;
            paint.setColor(track);
            canvas.drawRoundRect(0, 0, w, h, r, r, paint);
            paint.setColor(fill);
            float end = Math.max(h, w * shown);
            canvas.drawRoundRect(0, 0, end, h, r, r, paint);
            if (shown < 1f) {
                float t = ((now - started) % 1200L) / 1200f, x = end * t;
                paint.setColor(0x55FFFFFF);
                canvas.drawRoundRect(Math.max(0, x - h * 3), 0, Math.min(end, x + h * 3), h, r, r, paint);
            }
            if (getVisibility() == VISIBLE && (shown != wanted || shown < 1f)) postInvalidateOnAnimation();
        }
    }

    private String emojiName(EmojiPackManager.Pack pack) {
        if (Fonts.SYSTEM.equals(pack.id)) return Text.EMOJI_SYSTEM;
        return pack.name;
    }

    private void pickEmoji() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_EMOJI);
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * A row that is one of several answers to one question.
     *
     * `preview` is a typeface name to write the row in, so a font can be read
     * before it is chosen; null leaves the row in the app's own.
     */
    private View pickRow(String title, boolean chosen, String preview, final Runnable action) {
        LinearLayout row = row();
        row.setPadding(dp(12), 0, dp(12), 0);

        TextView name = label(title);
        if (preview != null) {
            try {
                name.setTypeface(android.graphics.Typeface.create(
                        preview, android.graphics.Typeface.NORMAL));
            } catch (Throwable ignored) {
            }
        }
        row.addView(name, grow());
        if (chosen) row.addView(new Check(this, Accent.colour()));
        row.setOnClickListener(v -> action.run());
        return sized(row, 52);
    }

    private void pickFont() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_FONT);
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }

    // ---------------------------------------------------------- the icon

    private View iconHead() {
        LinearLayout row = row();
        row.addView(icon("image"));
        row.addView(label(Text.ICON), grow());

        TextView now = detail(Launcher.nameOf(Launcher.chosen()));
        now.setTextColor(skin.text);
        row.addView(now);
        return sized(row, 52);
    }

    /**
     * The launcher icons side by side, always on screen: one tap picks one.
     * The picked icon gets a ring in the accent; the strip scrolls when the
     * icons do not fit the width.
     */
    private View iconChoices() {
        android.widget.HorizontalScrollView scroll = new android.widget.HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setPadding(dp(12), dp(12), dp(12), dp(14));

        String now = Launcher.chosen();
        String[] all = Launcher.all();
        for (int i = 0; i < all.length; i++) {
            final String which = all[i];
            final boolean picked = which.equals(now);

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setPadding(dp(4), dp(4), dp(4), dp(4));

            android.widget.ImageView shot = new android.widget.ImageView(this);
            android.graphics.Bitmap picture = LauncherPreview.get(which);
            if (picture != null) shot.setImageBitmap(picture);
            shot.setScaleType(ImageView.ScaleType.FIT_CENTER);
            shot.setPadding(dp(4), dp(4), dp(4), dp(4));
            // the ring follows the icon's own shape on this phone; a rounded square where Android has none
            android.graphics.Path outline = LauncherPreview.shape();
            int ringColour = picked ? Accent.colour() : Expressive.mix(skin.card, skin.text, 0.18f);
            if (outline != null) {
                android.graphics.drawable.ShapeDrawable ring = new android.graphics.drawable.ShapeDrawable(
                        new android.graphics.drawable.shapes.PathShape(outline, 100f, 100f));
                ring.getPaint().setStyle(Paint.Style.STROKE);
                // around the art (the 56 dp inside the padding, of which the art is ART_SHARE), 4 dp out from it
                float side = dp(56) * LauncherPreview.ART_SHARE + dp(8);
                int inset = Math.round((dp(64) - side) / 2f);
                // PathShape scales its 100-unit path to the bounds, and the stroke with it
                ring.getPaint().setStrokeWidth(dp(picked ? 3 : 1) * 100f / side);
                ring.getPaint().setColor(ringColour);
                ring.getPaint().setAntiAlias(true);
                shot.setBackground(new android.graphics.drawable.InsetDrawable(ring, Math.max(0, inset)));
            } else {
                android.graphics.drawable.GradientDrawable ring = new android.graphics.drawable.GradientDrawable();
                ring.setCornerRadius(dp(18));
                ring.setColor(0x00000000);
                ring.setStroke(dp(picked ? 3 : 1), ringColour);
                shot.setBackground(ring);
            }
            item.addView(shot, new LinearLayout.LayoutParams(dp(64), dp(64)));

            TextView name = label(Launcher.nameOf(which));
            name.setTextSize(12);
            name.setPadding(0, 0, 0, 0);
            name.setSingleLine(true);
            name.setGravity(Gravity.CENTER);
            name.setTextColor(picked ? skin.text : skin.muted());
            LinearLayout.LayoutParams nameSize = new LinearLayout.LayoutParams(
                    dp(64), ViewGroup.LayoutParams.WRAP_CONTENT);
            nameSize.topMargin = dp(6);
            item.addView(name, nameSize);

            Motion.touch(item);
            item.setOnClickListener(v -> {
                if (which.equals(Launcher.chosen())) return;
                Launcher.choose(this, which);
                rebuild();
            });
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            size.rightMargin = dp(6);
            strip.addView(item, size);
        }
        scroll.addView(strip);
        return scroll;
    }

    /** The row that opens one of the theme's two colours. */
    private View shadeHead(final boolean forText) {
        LinearLayout row = row();
        row.addView(icon(forText ? "text_fields" : "format_color_fill"));
        row.addView(label(forText ? Text.THEME_TEXT : Text.THEME_BACKGROUND), grow());
        row.addView(new Dot(this, forText ? Themes.text() : Themes.background(), false));

        boolean open = forText ? textOpen : backgroundOpen;
        row.addView(disclosure(open,forText ? "textOpen" : "backgroundOpen"));

        row.setOnClickListener(v -> {
            disclosureKey = forText ? "textOpen" : "backgroundOpen";
            if (forText) textOpen = !textOpen;
            else backgroundOpen = !backgroundOpen;
            rebuildAfterReveal(forText ? textOpen : backgroundOpen);
        });
        return sized(row, 52);
    }

    /**
     * What a text colour and a background colour may be.
     *
     * Two ramps rather than one palette: what a background wants is a set of
     * near-blacks and near-whites, and what text wants is the other end. The
     * accent's own dots are bright colours and would be no use for either.
     */
    /** The hues the theme's colours come in: red, orange, amber, green, teal, sky, blue, violet, purple, pink. */
    private static final float[] HUES = {0, 24, 42, 140, 172, 198, 218, 262, 290, 330};

    /**
     * Colours to choose from, made for the theme in use: a dark theme gets deep colours to sit on and pale ones to
     * read, a light theme pastel ones to sit on and deep ones to read -- each hue a real colour, not a near-black.
     */
    private static int[] themeChoices(boolean forText, boolean dark) {
        int[] out = new int[HUES.length + 2];
        if (forText) { out[0] = dark ? 0xFFFFFFFF : 0xFF161823; out[1] = dark ? 0xFFD6D6DE : 0xFF000000; }
        else { out[0] = dark ? 0xFF000000 : 0xFFFFFFFF; out[1] = dark ? 0xFF161823 : 0xFFF1F1F4; }
        for (int i = 0; i < HUES.length; i++) {
            float saturation = forText ? (dark ? .18f : .75f) : (dark ? .62f : .16f);
            float value = forText ? (dark ? 1f : .32f) : (dark ? .28f : .98f);
            out[i + 2] = android.graphics.Color.HSVToColor(new float[]{HUES[i], saturation, value});
        }
        return out;
    }

    private View shades(final boolean forText) {
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(16), dp(12), dp(16), dp(16));

        int[] choices = themeChoices(forText, Themes.isDark());
        int now = forText ? Themes.text() : Themes.background();
        LinearLayout line = null;
        for (int i = 0; i < choices.length; i++) {
            if (i % 6 == 0) {
                line = new LinearLayout(this);
                line.setOrientation(LinearLayout.HORIZONTAL);
                line.setPadding(0, dp(6), 0, dp(6));
                rows.addView(line);
            }
            final int colour = choices[i];
            Dot dot = new Dot(this, colour, colour == now);
            dot.setContentDescription(String.format(Locale.US, "#%06X", colour & 0xFFFFFF));
            dot.setOnClickListener(v -> {
                if (forText) Themes.setText(colour);
                else Themes.setBackground(colour);
                markChanged();
                rebuild();
            });
            line.addView(dot, new LinearLayout.LayoutParams(0, dp(48), 1f));
        }
        rows.addView(actionRow("palette", Text.CUSTOM_COLOUR, null, () ->
                showProfileColourPicker(forText ? Text.THEME_TEXT : Text.THEME_BACKGROUND,
                        forText ? Themes.text() : Themes.background(), colour -> {
                            if (forText) Themes.setText(colour);
                            else Themes.setBackground(colour);
                            markChanged();
                            rebuild();
                        })));
        return rows;
    }

    private interface ColourPicked { void set(int colour); }

    private View appearanceColourRow(final String role) {
        final int selected = AppearanceColors.ACCENT.equals(role)
                ? Accent.colour() : AppearanceColors.get(role);
        LinearLayout row = row();
        row.setPadding(dp(12), 0, dp(12), 0);
        row.addView(label(AppearanceColors.label(role)), grow());
        row.addView(new Dot(this, selected == 0 ? skin.muted() : selected, false));
        TextView value = detail(selected == 0 ? "TikTok" : String.format(Locale.US,
                "#%06X", selected & 0xFFFFFF));
        value.setTextColor(skin.muted());
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        valueParams.leftMargin = dp(10);
        row.addView(value, valueParams);
        row.setOnClickListener(v -> showProfileColourPicker(AppearanceColors.label(role),
                selected == 0 ? Accent.TIKTOK : selected, colour -> {
                    AppearanceColors.set(role, colour);
                    markChanged();
                    rebuild();
                }));
        return sized(row, 52);
    }

    /** A row of the theme's colours, like the accent's: a tap gives the screens that colour, light and dark alike. */
    private View themePalette() {
        android.widget.HorizontalScrollView scroll = new android.widget.HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        LinearLayout strip = new LinearLayout(this);
        strip.setPadding(dp(16), dp(12), dp(16), dp(14));
        boolean dark = Themes.isDark();
        int now = Themes.isEnabled() && !Themes.isMaterial() ? Themes.background() : 0;
        for (float hue : HUES) {
            final int shown = android.graphics.Color.HSVToColor(new float[]{hue, dark ? .62f : .16f, dark ? .28f : .98f});
            final int vivid = android.graphics.Color.HSVToColor(new float[]{hue, .7f, .85f});
            Dot dot = new Dot(this, shown, shown == now);
            dot.setContentDescription(String.format(Locale.US, "#%06X", shown & 0xFFFFFF));
            Motion.touch(dot);
            dot.setOnClickListener(v -> { Themes.hue(vivid); markChanged(); rebuildAfterSwitchAnimation(); });
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(44), dp(44));
            size.rightMargin = dp(8);
            strip.addView(dot, size);
        }
        Dot own = new Dot(this, now == 0 ? skin.card : now, false);
        own.fill.setShader(new android.graphics.SweepGradient(dp(22), dp(22), new int[]{0xFFE53935, 0xFFFFB300, 0xFF43A047, 0xFF00ACC1, 0xFF1E88E5, 0xFF8E24AA, 0xFFE53935}, null));
        own.setContentDescription(Text.CUSTOM_COLOUR);
        Motion.touch(own);
        own.setOnClickListener(v -> showProfileColourPicker(Text.THEME_BACKGROUND, now == 0 ? 0xFF1E88E5 : now, colour -> {
            Themes.hue(colour);
            markChanged();
            rebuild();
        }));
        strip.addView(own, new LinearLayout.LayoutParams(dp(44), dp(44)));
        scroll.addView(strip);
        return scroll;
    }

    /** Keep theme presets together in one menu row instead of three buttons. */
    private void chooseThemePreset() {
        final String[] choices = {Text.THEME_PRESET_DARK, Text.THEME_PRESET_BLUE,
                Text.THEME_RESET};
        new ModDialog.Builder(this, skin)
                .setTitle(Text.THEME_PRESETS)
                .setItems(choices, (dialog, index) -> {
                    
                    if (index == 0) Themes.preset(0);
                    else if (index == 1) Themes.preset(1);
                    else Themes.reset();
                    markChanged();
                    rebuild();
                })
                .setNegativeButton(Text.CANCEL, null)
                .show();
    }

    private void chooseAccentColour() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(8));
        final AccentColourPicker picker = new AccentColourPicker(this, Accent.colour());
        final View sample = new View(this);
        LinearLayout.LayoutParams sampleParams = new LinearLayout.LayoutParams(dp(56), dp(28));
        sampleParams.gravity = Gravity.CENTER_HORIZONTAL;
        content.addView(sample, sampleParams);
        content.addView(picker, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(238)));
        picker.onColourChanged(colour -> sample.setBackgroundColor(colour));
        sample.setBackgroundColor(Accent.colour());
        android.widget.HorizontalScrollView extras=new android.widget.HorizontalScrollView(this);extras.setHorizontalScrollBarEnabled(false);LinearLayout strip=new LinearLayout(this);strip.setOrientation(LinearLayout.HORIZONTAL);strip.setPadding(0,dp(8),0,dp(8));
        final java.util.List<Dot> extraDots=new java.util.ArrayList<>();for(int colour:new int[]{0xFFFFA726,0xFF44D65B,0xFFAA45EE,0xFFF343C9,0xFF405FE8,0xFF35B8E5,0xFFFA4654}){Dot dot=new Dot(this,colour,colour==Accent.colour());extraDots.add(dot);dot.setContentDescription(String.format(Locale.ROOT,"#%06X",colour&0xffffff));Motion.touch(dot);dot.setOnClickListener(v->{picker.setColour(colour);for(Dot other:extraDots)other.choose(other==dot,true);});LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(dp(44),dp(44));size.rightMargin=dp(8);strip.addView(dot,size);}extras.addView(strip);content.addView(extras);
        new ModDialog.Builder(this, skin).setTitle(Text.ACCENT_PICKER)
                .setView(content).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Accent.setCustom(picker.colour());
                    if(accentCustomSwatch!=null)accentCustomSwatch.reveal(picker.colour());
                    markChanged();
                    if(rebuildQueued)column.removeCallbacks(delayedRebuild);rebuildQueued=true;column.postDelayed(delayedRebuild,260L);
                }).show();
    }

    private View palette() {
        android.widget.HorizontalScrollView scroll=new android.widget.HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);scroll.setClipToPadding(false);
        LinearLayout strip=new LinearLayout(this);strip.setOrientation(LinearLayout.HORIZONTAL);strip.setPadding(dp(16),dp(12),dp(16),dp(16));
        int[] colours=Accent.palette();final java.util.List<Dot> swatches=new java.util.ArrayList<>();
        for(int colour:colours){Dot dot=new Dot(this,colour,colour==Accent.colour());swatches.add(dot);Motion.touch(dot);
            dot.setContentDescription(String.format(Locale.US,"#%06X",colour&0xFFFFFF));
            dot.setOnClickListener(v->{Accent.set(colour);for(Dot swatch:swatches)swatch.choose(swatch.fill.getColor()==colour,true);if(accentCustomSwatch!=null)accentCustomSwatch.choose(false,true);markChanged();rebuildAfterSwitchAnimation();});
            LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(dp(48),dp(48));size.rightMargin=dp(8);strip.addView(dot,size);
        }
        int custom=Accent.custom();
        Dot picker=new Dot(this,custom==0?Accent.colour():custom,custom!=0 && custom==Accent.colour());
        if(custom==0)picker.fill.setShader(new android.graphics.SweepGradient(dp(24),dp(24),new int[]{0xFFE53935,0xFFFFB300,0xFF43A047,0xFF00ACC1,0xFF1E88E5,0xFF8E24AA,0xFFE53935},null));
        accentCustomSwatch=picker;Motion.touch(picker);picker.setContentDescription(Text.ACCENT_PICKER);picker.setOnClickListener(v->chooseAccentColour());
        strip.addView(picker,new LinearLayout.LayoutParams(dp(48),dp(48)));scroll.addView(strip);return scroll;
    }

    /**
     * A Material icon, tinted to whatever this screen turned out to be.
     *
     * Decoded once per name and kept: the settings screen is rebuilt on every
     * tap, and decoding a dozen pngs each time would be felt.
     */
    private View icon(String name) {
        return icon(name, false, 40);
    }

    /** The shapes the first icons of the main list get; the rest are picked by name. */
    private static final java.util.Map<String, Integer> SHAPES = new java.util.HashMap<String, Integer>();
    static {
        SHAPES.put("palette", Expressive.COOKIE9);
        SHAPES.put("group", Expressive.CLOVER4);
        SHAPES.put("play_circle", Expressive.SUNNY);
        SHAPES.put("tune", Expressive.FLOWER6);
        SHAPES.put("info", Expressive.COOKIE9);
        SHAPES.put("extension", Expressive.COOKIE7);
        SHAPES.put("chat", Expressive.BURST12);
    }

    private static int shapeFor(String name) {
        Integer fixed = SHAPES.get(name);
        if (fixed != null) return fixed;
        // Only the round, evenly scalloped shapes: the pentagon and the three-lobed arch
        // look angular and sit off-centre next to the rest.
        int[] even = {Expressive.COOKIE9, Expressive.CLOVER4, Expressive.SUNNY, Expressive.FLOWER6,
                Expressive.BURST12, Expressive.COOKIE7};
        return even[(name.hashCode() & 0x7FFFFFFF) % even.length];
    }

    /**
     * An icon inside an expressive shape.
     *
     * A toggle's shape is quiet when it is off and fills with the accent, turning
     * by a lobe, when it is on; anything else is a tonal shape that stays put.
     */
    private View icon(String name, boolean toggle, int sizeDp) {
        // every icon of the menu sits filled inside an expressive shape in the accent: a toggle's is tonal while
        // off and turns to the full accent, by a lobe, when on; any other is the full accent with its glyph on it
        android.graphics.drawable.Drawable vector = SettingsGlyph.filled(name, 0xFFFFFFFF);
        if (vector == null) {
            Bitmap picture = pictureFor(name);
            if (picture != null) vector = new android.graphics.drawable.BitmapDrawable(getResources(), picture);
        }
        int accent = Accent.colour();
        int tonal = accentTonal();
        int ink = accentInk();
        Expressive.Badge badge = toggle
                ? new Expressive.Badge(this, Expressive.SQUARE, vector, sizeDp, tonal, accent, ink, onAccent())
                : new Expressive.Badge(this, Expressive.SQUARE, vector, sizeDp, tonal, tonal, ink, ink);
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp));
        size.rightMargin = dp(16);
        badge.setLayoutParams(size);
        return badge;
    }

    private Bitmap pictureFor(String name) {
        Bitmap bitmap = ICONS.get(name);
        if (bitmap == null) {
            try {
                String data = Icons.PNG.get(name);
                if (data != null) {
                    byte[] png = android.util.Base64.decode(data, android.util.Base64.DEFAULT);
                    bitmap = android.graphics.BitmapFactory.decodeByteArray(png, 0, png.length);
                    if (bitmap != null) ICONS.put(name, bitmap);
                }
            } catch (Throwable ignored) {
            }
        }
        return bitmap;
    }

    private static final java.util.Map<String, Bitmap> ICONS =
            new java.util.HashMap<String, Bitmap>();

    /** What is set by a setting: one line and a switch, wherever it lives. */
    /** The words that make a video or a part of it advertising: the person's own, one per line, and the built-in list. */
    private void editAdWords(){
        Panel p=Panel.with(this,skin,Text.t("Слова рекламы"));
        p.text(Text.t("Видео, в описании которого есть такое слово, скрывается, а где его произносят в субтитрах — пропускается. По одному слову или фразе в строке."));
        final android.widget.EditText words=p.field(Text.t("Например: ggdrop, промокод"));
        words.setSingleLine(false);words.setMinLines(4);words.setMaxLines(10);words.setGravity(Gravity.TOP|Gravity.START);
        words.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        words.setText(AdWords.own());
        final boolean[] builtIn={AdWords.defaults()};
        p.view(liveToggleRow("block",Text.t("Встроенный список")+" ("+AdWords.BUILT_IN.length+")",builtIn[0],on->builtIn[0]=on));
        p.text(Text.t("Встроенные")+": "+String.join(", ",AdWords.BUILT_IN));
        p.primary(Text.t("Сохранить"),()->{AdWords.setWords(words.getText().toString(),builtIn[0]);Screen.say(Text.t("Сохранено. Сработает с новыми видео."));});
        p.quiet(Text.t("Отмена"),null);
        p.show();
    }

    private interface Setting {
        void set(boolean on);
    }

    private View toggleRow(String picture, String title, boolean on, final Setting setting) {
        return toggleRow(picture,title,on,setting,true);
    }

    private View liveToggleRow(String picture, String title, boolean on, final Setting setting) {
        return toggleRow(picture,title,on,setting,false);
    }

    private View toggleRow(String picture, String title, boolean on, final Setting setting,
                           final boolean requiresRestart) {
        LinearLayout row = row();
        final View lead = icon(picture, true, 40);
        if (lead instanceof Expressive.Badge) ((Expressive.Badge) lead).set(on, false);
        row.addView(lead);
        row.addView(label(title), grow());

        settingTargets.put(title,row);
        final M3Switch toggle = new M3Switch(this);
        toggle.colours(Accent.colour(), skin.muted(), skin.card);
        toggle.setContentDescription(title);
        toggle.setChecked(on);
        toggle.setOnChanged(checked -> {
            if (lead instanceof Expressive.Badge) ((Expressive.Badge) lead).set(checked, true);
            setting.set(checked);
            if (requiresRestart) markChanged();
        });
        row.addView(toggle);

        row.setOnClickListener(v -> {
            toggle.performClick();
        });
        return sized(row, 56);
    }
    private View stickerHead() {
        LinearLayout row = row();
        row.addView(icon("star"));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(Text.STREAK_STICKER));
        int many = Streaks.offered().size();
        text.addView(detail(many == 0 ? Text.STREAK_NOTHING : String.valueOf(many)));
        row.addView(text, grow());

        row.addView(disclosure(streakOpen,"streakOpen"));

        row.setOnClickListener(v -> {
            disclosureKey = "streakOpen";
            streakOpen = !streakOpen;
            rebuildAfterReveal(streakOpen);
        });
        return sized(row, 60);
    }

    /** The stickers the app has drawn so far, as something to point at. */
    private View stickerChoices() {
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(16), dp(10), dp(16), dp(14));

        FavouriteStickers.load(()->{if(!isFinishing())rebuild();});
        java.util.List<String> ids = Streaks.offered();
        if (ids.isEmpty()) {
            rows.addView(quiet(Text.STREAK_NOTHING));
            return rows;
        }

        String chosen = Streaks.chosen();
        LinearLayout line = null;
        for (int i = 0; i < ids.size() && i < 120; i++) {
            if (i % 5 == 0) {
                line = new LinearLayout(this);
                line.setOrientation(LinearLayout.HORIZONTAL);
                line.setPadding(0, dp(5), 0, dp(5));
                rows.addView(line);
            }
            final String id = ids.get(i);
            View one = stickerTile(id, id.equals(chosen));
            LinearLayout.LayoutParams size =
                    new LinearLayout.LayoutParams(dp(52), dp(52), 1f);
            line.addView(one, size);
        }
        return rows;
    }

    private View stickerTile(final String id, boolean chosen) {
        ImageView view = new ImageView(this);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setPadding(dp(4), dp(4), dp(4), dp(4));
        if (chosen) {
            GradientDrawable ring = new GradientDrawable();
            ring.setColor(0x00000000);
            ring.setStroke(dp(2), Accent.colour());
            ring.setCornerRadius(dp(10));
            view.setBackground(ring);
        }
        final java.lang.ref.WeakReference<ImageView> target=new java.lang.ref.WeakReference<ImageView>(view);
        Bitmap picture = Streaks.thumbnail(this, id,()->{
            ImageView tile=target.get();if(tile!=null && tile.isAttachedToWindow()){
                Bitmap loaded=Streaks.thumbnail(tile.getContext(),id);if(loaded!=null)tile.setImageBitmap(loaded);
            }
        });
        if (picture != null) view.setImageBitmap(picture);
        view.setOnClickListener(v -> {
            Streaks.choose(id);
            rebuild();
        });
        return view;
    }

    /** A row that does something at once, rather than setting anything. */
    private View actionRow(String picture, String title, String detail, final Runnable action) {
        LinearLayout row = row();
        row.addView(icon(picture));
        boolean hasDetail = detail != null && !detail.isEmpty();

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(title));
        if (hasDetail) text.addView(detail(detail));
        row.addView(text, grow());

        ImageView chevron = new ImageView(this);
        chevron.setImageDrawable(SettingsGlyph.make("chevron_right", skin.muted()));
        row.addView(chevron, new LinearLayout.LayoutParams(dp(24), dp(24)));

        row.setOnClickListener(v -> action.run());
        return sized(row, hasDetail ? 72 : 56);
    }

    /**
     * What this is and what it was built from, at the very bottom.
     *
     * Small and grey on purpose: nobody needs it until something has gone
     * wrong, and then it is the first thing anybody will ask for.
     */
    private View versions() {
        TextView view = new TextView(this);
        view.setText("ttcuz " + Version.MOD + "  ·  TikTok " + Version.TIKTOK);
        view.setTextColor(skin.muted());
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(20), dp(24), dp(20), dp(8));
        return view;
    }

    private View idRow(String title, final String value) {
        LinearLayout row = row();
        row.addView(icon("fingerprint"));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(title));

        TextView shown = detail(value == null ? Text.ACCOUNT_UNKNOWN : shorten(value));
        if (value != null) shown.setTextColor(skin.text);
        text.addView(shown);
        row.addView(text, grow());

        if (value != null) {
            row.addView(away());
            row.setOnClickListener(v -> copy(title, value));
        }
        return sized(row, 60);
    }

    private static String shorten(String value) {
        if (value.length() <= 26) return value;
        return value.substring(0, 14) + "…" + value.substring(value.length() - 8);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null) return;
        Uri source = data.getData();
        if (source == null) return;

        if (request == PICK_PARTICLE) {
            Net.away("particle picture", () -> {
                android.graphics.Bitmap picture = null;
                try (java.io.InputStream in = getContentResolver().openInputStream(source)) {
                    android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
                    o.inSampleSize = 1;
                    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                    byte[] block = new byte[16384];
                    for (int n; (n = in.read(block)) > 0 && buffer.size() < 8 * 1024 * 1024; ) buffer.write(block, 0, n);
                    byte[] raw = buffer.toByteArray();
                    android.graphics.BitmapFactory.Options bounds = new android.graphics.BitmapFactory.Options();
                    bounds.inJustDecodeBounds = true;
                    android.graphics.BitmapFactory.decodeByteArray(raw, 0, raw.length, bounds);
                    while (Math.max(bounds.outWidth, bounds.outHeight) / o.inSampleSize > 512) o.inSampleSize *= 2;
                    picture = android.graphics.BitmapFactory.decodeByteArray(raw, 0, raw.length, o);
                } catch (Throwable error) { Diary.note("particle picture: " + error); }
                final android.graphics.Bitmap chosen = picture;
                runOnUiThread(() -> { if (chosen == null) Screen.say(Text.t("Не удалось прочитать изображение")); else ParticleEditor.picked(chosen); });
            });
            return;
        }

        if (request == PICK_ISLAND) {
            Net.away("island file", () -> {
                String text = null;
                try (java.io.InputStream in = getContentResolver().openInputStream(source)) {
                    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                    byte[] block = new byte[4096];
                    for (int n; (n = in.read(block)) > 0 && buffer.size() <= 64 * 1024; ) buffer.write(block, 0, n);
                    text = buffer.toString("UTF-8");
                } catch (Throwable error) { Diary.note("island file: " + error); }
                final String read = text;
                runOnUiThread(() -> { if (read == null) Screen.say(Text.t("Не удалось прочитать файл")); else takeIsland(null, read); });
            });
            return;
        }

        if (request == PICK_CATALOG_BADGE) { if(AdminAccess.current())importCatalogBadge(source); return; }

        if (request == PICK_PROFILE_BADGE) {
            Screen.say(Text.t("Готовим значок…"));
            Net.away("profile badge import", () -> {
                final boolean saved = ProfileStyle.take(this, source);
                runOnUiThread(() -> {
                    if (!saved) Toast.makeText(this, Text.PROFILE_BADGE_FAILED,
                            Toast.LENGTH_LONG).show();
                    else markChanged();
                    rebuild();
                });
            });
            return;
        }

        if (request == CREATE_BACKUP) {
            Screen.progress(Text.BACKUP_WRITING, 0);
            Net.away("backup", () -> {
                boolean done = Backup.write(this, source);
                if (done) Screen.progressDone(); else Screen.progressGone();
                runOnUiThread(() -> Screen.say(done ? Text.BACKUP_DONE : Text.BACKUP_FAILED));
            });
            return;
        }
        if (request == PICK_BACKUP) {
            reviewBackup(source);
            return;
        }

        if (request == PICK_COMMENT_BACKGROUND || request == PICK_NICK_FONT) {
            Net.away("restored import", () -> {
                final boolean ok = request == PICK_NICK_FONT ? NicknameFont.take(getApplicationContext(), source, nickScope) : CommentBackground.take(this, source);
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    if (ok) { if (request == PICK_COMMENT_BACKGROUND) CommentBackground.refresh(); markChanged(); }
                    else Toast.makeText(this, Text.FONT_FAILED, Toast.LENGTH_LONG).show();
                    rebuild();
                });
            });
            return;
        }
        if (request == PluginWorkbench.EXPORT) { PluginWorkbench.export(this, source); return; }
        if (request == PluginList.SAVE) { PluginList.save(this, source); return; }
        if (PluginWorkbench.picked(this, request, source)) return;
        if (request == PICK_PLUGIN) {
            PluginEngine.install(source, msg -> runOnUiThread(() -> {
                if (isFinishing()) return;
                if (msg != null && !msg.isEmpty()) Screen.say(msg);
                // the plugin list follows the engine itself and grows the new card in; no rebuild of the screen
            }));
            return;
        }

        if (request == PICK_FONT || request == PICK_EMOJI) {
            Net.away("appearance import", () -> {
                final boolean ok = Fonts.take(this, source, request == PICK_EMOJI);
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    if (ok) markChanged();
                    else Toast.makeText(this, Text.FONT_FAILED,
                            Toast.LENGTH_LONG).show();
                    rebuild();
                });
            });
            return;
        }
    }




    // ------------------------------------------------------------- the links

    private View linkRow(String picture, String title, String handle, final String url) {
        LinearLayout row = row();
        row.addView(icon(picture));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(title));
        text.addView(detail(handle));
        row.addView(text, grow());
        row.addView(away());

        row.setOnClickListener(v -> open(url));
        return sized(row, 60);
    }

    /** The channel card: the whole card is a button to the mod's Telegram channel. */
    private View channelBanner() {
        ChannelBanner banner = new ChannelBanner(this);
        banner.setOnClickListener(v -> open(ChannelBanner.URL));
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(12), dp(2), dp(12), dp(10));
        box.addView(banner, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return box;
    }

    private View thanksHead() {
        LinearLayout row = row();
        row.addView(icon("favorite_border"));
        row.addView(label(Text.THANKS), grow());

        row.addView(disclosure(thanksOpen,"thanksOpen"));

        row.setOnClickListener(v -> {
            disclosureKey = "thanksOpen";
            thanksOpen = !thanksOpen;
            rebuildAfterReveal(thanksOpen);
        });
        return sized(row, 52);
    }

    /** Project credits. */
    private View thanks() {
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(0, dp(6), 0, dp(10));

        rows.addView(quiet(Text.THANKS_NOTE));
        rows.addView(person("@narezany", Text.t("Основа, с которой всё началось"), OWNER_TELEGRAM));
        rows.addView(person("@dddrocka", Text.THANKS_ICONS, null));
        rows.addView(person("GPT 6 Sol", Text.THANKS_AI, null));
        rows.addView(person("@trem0r1_1", Text.THANKS_HELPER,
                "https://www.tiktok.com/@trem0r1_1"));

        return rows;
    }
    private View person(String name, String what, final String url) {
        LinearLayout row = row();

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(name));
        text.addView(detail(what));
        row.addView(text, grow());

        if (url != null) {
            row.addView(away());
            row.setOnClickListener(v -> open(url));
        }
        return sized(row, 60);
    }

    /** The mark on a row that leaves the app. */
    private View away() {
        Chevron arrow=new Chevron(this,skin.muted(),false,false);
        arrow.setRotation(-90f);
        return arrow;
    }

    /** A paragraph that is there to be read once and then ignored. */
    private TextView quiet(String message) {
        TextView view = new TextView(this);
        view.setText(message);
        view.setTextColor(skin.muted());
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        view.setPadding(dp(16), dp(2), dp(16), dp(8));
        return view;
    }

    /** A card number is read off the screen by a person, so it is grouped. */


    private void open(String url) {
        // a tiktok.com link belongs to the app this is running inside, so it is
        // offered there first: without this the browser opens, recognises the
        // link and hands it straight back, which is two screens for nothing
        url = inApp(url);
        if (url.contains("tiktok.com") && openWith(url, getPackageName())) return;
        if (openWith(url, null)) return;
        Toast.makeText(this, Text.NO_BROWSER, Toast.LENGTH_SHORT).show();
    }

    /**
     * The spelling of a link the app answers to.
     *
     * TikTok claims `www.tiktok.com` and a dozen others in its manifest, and
     * does not claim the bare domain -- so a link written without the `www`
     * resolves to nothing in the app, falls through to the browser, and the
     * browser hands it straight back. One prefix is the whole difference.
     */
    private static String inApp(String url) {
        if (url.startsWith("https://tiktok.com/")) {
            return "https://www.tiktok.com/" + url.substring("https://tiktok.com/".length());
        }
        return url;
    }

    private boolean openWith(String url, String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (packageName != null) intent.setPackage(packageName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void copy(String what, String text) {
        safeCopy(what, text);
    }

    /**
     * The clipboard goes through Binder, which refuses anything near 1 MB
     * (TransactionTooLargeException). Long text: the newest part goes to the
     * clipboard, the whole thing to a .txt file offered through "Share".
     */
    private static final int CLIP_LIMIT = 60000; // chars; ~120 KB as UTF-16

    private void safeCopy(String what, String text) {
        if (text == null) text = "";
        try {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (text.length() <= CLIP_LIMIT) {
                clipboard.setPrimaryClip(ClipData.newPlainText(what, text));
                Toast.makeText(this, Text.COPIED, Toast.LENGTH_SHORT).show();
                return;
            }
            int from = text.length() - CLIP_LIMIT;
            int newline = text.indexOf('\n', from);
            if (newline > 0 && newline - from < 2000) from = newline + 1;
            String tail = Text.t("…начало обрезано, полный текст — в файле…") + "\n" + text.substring(from);
            clipboard.setPrimaryClip(ClipData.newPlainText(what, tail));
            Toast.makeText(this, Text.t("Скопирована последняя часть. Полный текст — в файле"),
                    Toast.LENGTH_LONG).show();
            shareAsFile(text);
        } catch (Throwable error) {
            Diary.note("copy: " + error.getClass().getSimpleName());
            // even the trimmed copy failed: the file still gets everything out
            shareAsFile(text);
        }
    }

    private void shareAsFile(String text) {
        try {
            java.io.File dir = new java.io.File(getFilesDir(), "ttcuz");
            if (!dir.isDirectory()) dir.mkdirs();
            java.io.File file = new java.io.File(dir, "ttcuz-report.txt");
            java.io.FileOutputStream out = new java.io.FileOutputStream(file);
            try { out.write(text.getBytes("UTF-8")); } finally { out.close(); }
            android.net.Uri uri = TtcuzProvider.share(this, file);
            if (uri == null) return;
            android.content.Intent send = new android.content.Intent(android.content.Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(android.content.Intent.createChooser(send, Text.t("Журнал ttcuz")));
        } catch (Throwable error) {
            Toast.makeText(this, Text.t("Не удалось сохранить журнал в файл"), Toast.LENGTH_LONG).show();
        }
    }

    /** Five taps in a row on the heading switch the developer tools on. */
    private void developerTap() {
        if (!DevMode.tapped()) return;
        if (DevMode.isOn()) {
            Toast.makeText(this, Text.DEV_ALREADY, Toast.LENGTH_SHORT).show();
            return;
        }
        DevMode.set(true);
        Diary.note("developer mode on");
        Toast.makeText(this, Text.DEV_ON, Toast.LENGTH_SHORT).show();
        rebuild();
    }

    /** Under the diary: the full report to copy, and the way back out. */
    private void addDeveloperRows(LinearLayout into) {
        into.addView(line());
        into.addView(actionRow("article", Text.DEV_FULL_REPORT, Text.DEV_FULL_NOTE,
                () -> copy("ttcuz full report", Diagnostics.fullReport())));
        into.addView(line());
        into.addView(toggleRow("tune", Text.DEV_SWITCH, true, on -> {
            if (!on) {
                DevMode.set(false);
                rebuildAfterSwitchAnimation();
            }
        }));
    }

    /** The whole report, monospaced, so columns line up and it reads like a log. */
    private View fullReportView() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(8), dp(16), dp(12));
        TextView view = new TextView(this);
        view.setText(Diagnostics.fullReport());
        view.setTextColor(skin.muted());
        view.setTypeface(Typeface.MONOSPACE);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        view.setTextIsSelectable(true);
        box.addView(view);
        return box;
    }

    private View diaryHead() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        // The title gets the whole row; the two actions sit on a line of their own,
        // otherwise the long words squeeze the title down to one letter per line.
        LinearLayout row = row();
        row.addView(icon("article"));
        TextView title = label(Text.DIARY_TITLE);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(title, grow());
        row.addView(disclosure(diaryOpen,"diaryOpen"));
        row.setOnClickListener(v -> {
            disclosureKey = "diaryOpen";
            diaryOpen = !diaryOpen;
            rebuildAfterReveal(diaryOpen);
        });
        box.addView(sized(row, 52));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(dp(16), 0, dp(16), dp(8));

        TextView copy = diaryAction(Text.COPY);
        copy.setOnClickListener(v -> copyDiary());
        actions.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView clear = diaryAction(Text.CLEAR);
        clear.setOnClickListener(v -> {
            Diary.clear();
            Toast.makeText(this, Text.CLEARED, Toast.LENGTH_SHORT).show();
            rebuild();
        });
        LinearLayout.LayoutParams clearWhere = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clearWhere.leftMargin = dp(8);
        actions.addView(clear, clearWhere);
        box.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return box;
    }

    private TextView diaryAction(String text) {
        TextView action = new TextView(this);
        action.setText(text);
        action.setTextColor(Accent.colour());
        action.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        action.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        action.setGravity(Gravity.CENTER);
        action.setSingleLine(true);
        action.setEllipsize(android.text.TextUtils.TruncateAt.END);
        action.setPadding(dp(12), dp(10), dp(12), dp(10));
        action.setMinHeight(dp(44));
        action.setClickable(true);
        action.setFocusable(true);
        GradientDrawable pill = new GradientDrawable();
        pill.setCornerRadius(dp(22));
        pill.setColor((Accent.colour() & 0xFFFFFF) | 0x1F000000);
        action.setBackground(pill);
        Motion.press(action);
        return action;
    }

    private View diaryLines() {
        if (DevMode.isOn()) return fullReportView();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(8), dp(16), dp(12));
        for (String entry : Diary.lines()) {
            TextView view = new TextView(this);
            view.setText(entry);
            view.setTextColor(skin.muted());
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            view.setPadding(0, dp(2), 0, 0);
            box.addView(view);
        }
        return box;
    }

    /**
     * Start TikTok over, so everything is drawn again in the new colour.
     *
     * The launcher's own intent, then out: what comes back is a fresh process
     * with nothing of the old one's colours cached in it.
     */
    private void restartTikTok() {
        try {
            android.content.Intent intent = getPackageManager()
                    .getLaunchIntentForPackage(getPackageName());
            if (intent == null) return;
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            Runtime.getRuntime().exit(0);
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }

    private void copyDiary() {
        try {
            StringBuilder out = new StringBuilder("ttcuz\n");
            if (DevMode.isOn()) out.append(Diagnostics.fullReport());
            else for (String line : Diary.lines()) out.append(line).append('\n');
            safeCopy("ttcuz", out.toString());
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }

    // ---------------------------------------------------------- the parts

    /** The quiet hours' start, then their end: two clock pickers in a row, 24-hour. */
    private void chooseQuietHours() {
        final int from = QuietHours.from(), to = QuietHours.to();
        new android.app.TimePickerDialog(this, android.R.style.Theme_DeviceDefault_Dialog_Alert, (pickerA, h1, m1) ->
                new android.app.TimePickerDialog(this, android.R.style.Theme_DeviceDefault_Dialog_Alert, (pickerB, h2, m2) -> {
                    QuietHours.setRange(h1 * 60 + m1, h2 * 60 + m2);
                    rebuild();
                }, to / 60, to % 60, true) {{ setTitle(Text.t("Конец тишины")); }}.show(),
                from / 60, from % 60, true) {{ setTitle(Text.t("Начало тишины")); }}.show();
    }

    private View backArrow() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), dp(8), dp(16), dp(4));
        // a round tonal button that turns into a rounded square under the finger
        View button = Expressive.roundButton(this, SettingsGlyph.make("arrow_forward", skin.text),
                skin.card, (Accent.colour() & 0xFFFFFF) | 0x33000000, skin.text, 48);
        ((ViewGroup) button).getChildAt(0).setScaleX(-1f);
        bar.addView(button);
        TextView name = new TextView(this);
        name.setText(settingsGroup == 0 ? "ttcuz" : Text.t("Настройки"));
        name.setTextColor(skin.muted());
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        name.setPadding(dp(12), 0, 0, 0);
        bar.addView(name);
        bar.setContentDescription(settingsGroup == 0 ? "ttcuz" : Text.t("Настройки"));
        bar.setOnClickListener(v -> goBack());
        if (settingsGroup == 0) {
            // the main page: the search, a magnifier in the top right corner level with the way back (as the store is
            // on the plugins page); the field opens under it with the keyboard
            bar.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
            View find = Expressive.roundButton(this, SettingsGlyph.make("search", skin.text),
                    skin.card, (Accent.colour() & 0xFFFFFF) | 0x33000000, skin.text, 48);
            find.setContentDescription(Text.t("Поиск по настройкам"));
            find.setOnClickListener(v -> {
                searchOpen = !searchOpen;
                refocusSearch = searchOpen;
                if (!searchOpen) searchQuery = "";
                rebuild();
            });
            // the pencil: arrange the tiles; while arranging it is a tick that ends it
            android.graphics.drawable.Drawable pen = editingTiles ? Symbols.drawable("check_circle", skin.text) : SettingsGlyph.make("edit", skin.text);
            View edit = Expressive.roundButton(this, pen, editingTiles ? accentTonal() : skin.card,
                    (Accent.colour() & 0xFFFFFF) | 0x33000000, skin.text, 48);
            edit.setContentDescription(editingTiles ? Text.t("Готово") : Text.t("Изменить плитки"));
            edit.setOnClickListener(v -> {
                editingTiles = !editingTiles;
                if (editingTiles) { searchOpen = false; searchQuery = ""; }
                rebuild();
            });
            LinearLayout.LayoutParams penAt = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            penAt.rightMargin = dp(8);
            bar.addView(edit, penAt);
            bar.addView(find);
        }
        if (settingsGroup == 6) {
            // the plugins page: the store in its top right corner, level with the way back
            bar.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
            View store = Expressive.roundButton(this, SettingsGlyph.make("storefront", skin.text),
                    skin.card, (Accent.colour() & 0xFFFFFF) | 0x33000000, skin.text, 48);
            store.setContentDescription(Text.t("Магазин плагинов"));
            store.setOnClickListener(v -> PluginWorkbench.store(this, skin));
            bar.addView(store);
        }
        return bar;
    }

    private View title(String text) {
        final TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(skin.text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, settingsGroup == 0 ? 44 : 32);
        view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        if (settingsGroup != 0) {
            view.setPadding(dp(24), dp(8), dp(24), dp(16));
            view.setGravity(Gravity.CENTER_HORIZONTAL);
            // five taps in a row and a cat turns up; one by accident does nothing
            view.setOnClickListener(v -> {
                Cats.tapped(view);
                developerTap();
            });
            return view;
        }

        // The main page: the name and its small note are two separate views.
        // A tap on the name sends a bat at the name itself; holding the note opens its menu.
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setBaselineAligned(true);
        line.setGravity(Gravity.CENTER_HORIZONTAL);
        line.setPadding(dp(24), dp(8), dp(24), dp(16));
        // The name is a row of letters, one view each, so that the bat's blow can run along it.
        final LinearLayout word = new LinearLayout(this);
        word.setOrientation(LinearLayout.HORIZONTAL);
        word.setBaselineAligned(true);
        word.setContentDescription(text);
        for (int i = 0; i < text.length(); i++) {
            TextView letter = new TextView(this);
            letter.setText(String.valueOf(text.charAt(i)));
            letter.setTextColor(skin.text);
            letter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 44);
            letter.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            letter.setIncludeFontPadding(true);
            word.addView(letter, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        line.addView(word, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // five taps in a row and a cat turns up; one by accident does nothing
        word.setOnClickListener(v -> {
            Cats.tapped(word, word);
            developerTap();
        });
        BatHit.prepare(this);
        // under the name: how many people use ttcuz right now, with Android's ghost; a tap shows more
        line.setPadding(dp(24), dp(8), dp(24), dp(6));
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setGravity(Gravity.CENTER_HORIZONTAL);
        head.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams under = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        under.bottomMargin = dp(16);
        if (ModStats.shown(this)) head.addView(onlineChip(), under);
        else line.setPadding(dp(24), dp(8), dp(24), dp(16));
        return head;
    }

    /** A dot in the accent that breathes, as an "online" light does. */
    private View onlineDot(int size) {
        View dot = new View(this);
        GradientDrawable round = new GradientDrawable();
        round.setShape(GradientDrawable.OVAL);
        round.setColor(Accent.colour());
        dot.setBackground(round);
        if (Motion.enabled(this)) {
            android.animation.ObjectAnimator pulse = android.animation.ObjectAnimator.ofFloat(dot, "alpha", 1f, .45f, 1f);
            pulse.setDuration(1800L);
            pulse.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            dot.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override public void onViewAttachedToWindow(View v) { pulse.start(); }
                @Override public void onViewDetachedFromWindow(View v) { pulse.cancel(); }
            });
        }
        return dot;
    }

    /** "● 12 в сети": a small tonal pill; the number arrives from the server a moment later; a tap shows more. */
    private View onlineChip() {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(10), dp(6), dp(14), dp(6));
        GradientDrawable back = new GradientDrawable();
        back.setColor(accentTonal());
        back.setCornerRadius(dp(18));
        chip.setBackground(back);
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(9), dp(9));
        size.leftMargin = dp(4);
        size.rightMargin = dp(8);
        chip.addView(onlineDot(9), size);
        final TextView words = new TextView(this);
        words.setTextColor(accentInk());
        words.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        words.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        chip.addView(words);
        final Runnable show = () -> words.setText(ModStats.now < 0 ? Text.t("Сколько нас сейчас…")
                : ModStats.number(ModStats.now) + " " + Text.t("в сети"));
        show.run();
        ModStats.refresh(() -> { if (words.isAttachedToWindow() || ModStats.now >= 0) show.run(); });
        chip.setOnClickListener(v -> statsSheet());
        // held: it goes away (it comes back from Tools)
        chip.setOnLongClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
            ModStats.setShown(this, false);
            Screen.say(Text.t("Скрыто. Вернуть: Инструменты → Сколько нас"));
            v.animate().alpha(0f).scaleX(.8f).scaleY(.8f).setDuration(Motion.enabled(this) ? Motion.SHORT : 0L).withEndAction(this::rebuild).start();
            return true;
        });
        chip.setContentDescription(Text.t("Сколько людей в ttcuz"));
        Motion.press(chip);
        return chip;
    }

    /** The numbers in full: now, today, this week, plugin downloads. */
    private void statsSheet() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams big = new LinearLayout.LayoutParams(dp(28), dp(28));
        big.topMargin = dp(8);
        big.bottomMargin = dp(18);
        body.addView(onlineDot(28), big);
        final TextView[] values = new TextView[4];
        String[] names = {Text.t("Сейчас в ttcuz"), Text.t("За сутки"), Text.t("За неделю"), Text.t("Скачиваний плагинов")};
        String[] pictures = {"bolt", "schedule", "group", "download"};
        for (int i = 0; i < 4; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(10), dp(16), dp(10));
            GradientDrawable back = new GradientDrawable();
            back.setColor(i == 0 ? accentTonal() : Expressive.mix(skin.card, skin.text, 0.05f));
            back.setCornerRadius(dp(16));
            row.setBackground(back);
            ImageView mark = new ImageView(this);
            mark.setImageDrawable(Symbols.drawable(pictures[i], i == 0 ? accentInk() : skin.muted()));
            LinearLayout.LayoutParams markSize = new LinearLayout.LayoutParams(dp(20), dp(20));
            markSize.rightMargin = dp(12);
            row.addView(mark, markSize);
            TextView name = new TextView(this);
            name.setText(names[i]);
            name.setTextColor(skin.text);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            values[i] = new TextView(this);
            values[i].setTextColor(i == 0 ? accentInk() : skin.text);
            values[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, i == 0 ? 20 : 16);
            values[i].setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            row.addView(values[i]);
            LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            gap.topMargin = dp(6);
            body.addView(row, gap);
        }
        TextView note = new TextView(this);
        note.setText(Text.t("Считаются телефоны, где TikTok с ttcuz был открыт: «сейчас» — за последние 12 минут. Без аккаунтов и имён, только случайный номер телефона."));
        note.setTextColor(skin.muted());
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        note.setPadding(dp(4), dp(12), dp(4), 0);
        body.addView(note);
        final Runnable fill = () -> {
            values[0].setText(ModStats.number(ModStats.now));
            values[1].setText(ModStats.number(ModStats.day));
            values[2].setText(ModStats.number(ModStats.week));
            values[3].setText(ModStats.number(ModStats.downloads));
        };
        fill.run();
        ModStats.refresh(fill::run);
        new ModDialog.Builder(this).setTitle(Text.t("Сколько нас")).setView(body).setPositiveButton(Text.t("Готово"), null).show();
    }

    /**
     * Search is an explicit action rather than a TextWatcher rebuilding the
     * entire settings tree for every keystroke. The keyboard stays stable,
     * and the results are real shortcuts into matching setting groups.
     */
    private View searchBox() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), dp(2), dp(8), dp(2));
        GradientDrawable background = new GradientDrawable();
        background.setColor(skin.card);
        background.setCornerRadius(dp(26));
        row.setBackground(background);

        ImageView glass = new ImageView(this);
        glass.setImageDrawable(new Expressive.Magnifier(skin.muted()));
        LinearLayout.LayoutParams glassSize = new LinearLayout.LayoutParams(dp(24), dp(24));
        glassSize.rightMargin = dp(12);
        row.addView(glass, glassSize);

        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(searchQuery);
        input.setSelectAllOnFocus(false);
        input.setHint(Text.SEARCH_HINT);
        input.setHintTextColor(skin.muted());
        input.setTextColor(skin.text);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        input.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        input.setOnEditorActionListener((view, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                runSearch(input.getText().toString());
                return true;
            }
            return false;
        });
        row.addView(input, grow());
        if (refocusSearch) {
            refocusSearch = false;
            input.post(() -> {
                input.requestFocus();
                input.setSelection(input.getText().length());
                android.view.inputmethod.InputMethodManager keys =
                        (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (keys != null) keys.showSoftInput(input, 0);
            });
        }
        // Search as you type, a moment after the last key.
        final Runnable[] waiting = {null};
        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { }
            @Override public void afterTextChanged(android.text.Editable text) {
                final String typed = text.toString().trim().toLowerCase(Locale.ROOT);
                if (typed.equals(searchQuery)) return;
                if (waiting[0] != null) input.removeCallbacks(waiting[0]);
                waiting[0] = () -> { refocusSearch = true; runSearch(typed); };
                input.postDelayed(waiting[0], 350L);
            }
        });

        if (searchQuery.length() > 0) {
            ImageView clear = new ImageView(this);
            clear.setImageDrawable(SettingsGlyph.make("close", skin.muted()));
            clear.setScaleType(ImageView.ScaleType.CENTER);
            clear.setContentDescription(Text.SEARCH_CLEAR);
            clear.setOnClickListener(v -> runSearch(""));
            Motion.touch(clear);
            row.addView(clear, new LinearLayout.LayoutParams(dp(40), dp(40)));
        }
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(16), dp(4), dp(16), dp(12));
        box.addView(sized(row, 52));
        return box;
    }

    private void runSearch(String words) {
        searchQuery = words == null ? "" : words.trim().toLowerCase(Locale.ROOT);
        rebuild();
    }

    /**
     * While the profile is being proved: a spinner, what is happening now in plain words, and which check this is.
     * Closing the window does not stop it; it ends with the result in the same window.
     */
    private void proofProgress(String handle) {
        Panel panel = Panel.with(this, skin, Text.t("Подтверждение @") + handle);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(0, dp(8), 0, dp(4));
        final android.widget.ProgressBar spinner = new android.widget.ProgressBar(this);
        spinner.setIndeterminate(true);
        spinner.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Accent.colour()));
        box.addView(spinner, new LinearLayout.LayoutParams(dp(44), dp(44)));
        final TextView now = new TextView(this);
        now.setTextColor(skin.text);
        now.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        now.setGravity(Gravity.CENTER);
        now.setPadding(dp(8), dp(14), dp(8), dp(4));
        box.addView(now);
        final TextView which = new TextView(this);
        which.setTextColor(skin.muted());
        which.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        which.setGravity(Gravity.CENTER);
        box.addView(which);
        TextView note = new TextView(this);
        note.setText(Text.t("Это может занять несколько минут: TikTok обновляет страницу профиля не сразу. Окно можно закрыть — проверка продолжится, а описание вернётся само."));
        note.setTextColor(skin.muted());
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        note.setGravity(Gravity.CENTER);
        note.setPadding(dp(8), dp(12), dp(8), 0);
        box.addView(note);
        panel.view(box);
        panel.quiet(Text.CLOSE, null);
        panel.show();
        final long started = android.os.SystemClock.uptimeMillis();
        final Runnable[] tick = new Runnable[1];
        tick[0] = () -> {
            if (!box.isAttachedToWindow()) return;
            String said = BioProof.status();
            now.setText(said == null || said.isEmpty() ? Text.t("Готовлю проверку…") : said);
            int attempt = BioProof.attempt();
            long seconds = (android.os.SystemClock.uptimeMillis() - started) / 1000L;
            String clock = String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
            which.setText(attempt > 0 ? Text.t("Проверка") + " " + attempt + " " + Text.t("из") + " " + BioProof.WAITS.length + " · " + clock : clock);
            // a moment after the start BioProof is running; once it is not, the result is the last status
            if (BioProof.running() || android.os.SystemClock.uptimeMillis() - started < 1500L) { box.postDelayed(tick[0], 500L); return; }
            spinner.setVisibility(View.GONE);
            which.setText("");
            note.setVisibility(View.GONE);
            updateSyncUi();
        };
        box.post(tick[0]);
    }

    private void verifyProfile() { verifyProfile(false); }
    private void verifyProfile(boolean manual) {
        final String uid = Account.id();
        if (uid == null || !Ids.uid(uid)) {
            Toast.makeText(this, Text.t("Сначала войдите в TikTok и откройте свой профиль"), Toast.LENGTH_LONG).show();
            return;
        }
        final CloudProfileProvider sync = new CloudProfileProvider(this);
        if (sync.verified(uid) && !BioProof.pending(uid)) {
            Toast.makeText(this, Text.t("Профиль уже подтверждён"), Toast.LENGTH_SHORT).show();
            return;
        }
        final String code;
        try { code = sync.code(uid); } catch (Throwable error) {
            Toast.makeText(this, Text.t("Не удалось подготовить проверку профиля"), Toast.LENGTH_LONG).show();
            return;
        }
        final String known = Account.profileHandle();
        final boolean haveHandle = known != null && known.matches("[A-Za-z0-9_.]{2,24}");

        if (!manual && haveHandle && NativeBio.available()) {
            Panel automatic=Panel.with(this,skin,Text.t("Подтвердить @")+known);
            automatic.text(Text.t("Код появится в описании на время проверки. Затем мод вернёт прежний текст."));
            if (BioProof.pending(uid)) {
                automatic.text(BioProof.status());
                automatic.keepOpen(Text.t("Скопировать прежнее описание"),() -> copy(Text.t("Описание"),BioProof.original(uid)));
            }
            automatic.primary(Text.t("Подтвердить"),() -> { BioProof.start(); proofProgress(known); });
            automatic.quiet(Text.t("Вставить код вручную"),() -> verifyProfile(true));
            automatic.quiet(Text.CANCEL,null);
            automatic.show();return;
        }
        final Panel panel = Panel.with(this, skin, Text.t("Подтверждение профиля"));
        panel.text(haveHandle
                ? "@" + known + Text.t("\n1. Откройте профиль и вставьте код в описание.\n2. Сохраните и вернитесь — проверка начнётся сама.")
                : Text.t("Вставьте код в описание TikTok и сохраните. Затем укажите свой @ник."));

        if (BioProof.pending(uid)) {
            panel.text(BioProof.status());
            panel.keepOpen(Text.t("Скопировать прежнее описание"),() -> copy(Text.t("Описание"),BioProof.original(uid)));
        }
        // The code itself, one tap to copy.
        TextView box = new TextView(this);
        box.setText(code);
        box.setTextColor(skin.text);
        box.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        box.setTypeface(Typeface.MONOSPACE);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(12), dp(14), dp(12), dp(14));
        GradientDrawable boxBack = new GradientDrawable();
        boxBack.setColor(skin.page);
        boxBack.setCornerRadius(dp(12));
        boxBack.setStroke(dp(1), skin.muted());
        box.setBackground(boxBack);
        box.setOnClickListener(v -> {
            copy("ttcuz", code);
            Toast.makeText(this, Text.t("Код скопирован"), Toast.LENGTH_SHORT).show();
        });
        panel.view(box);
        panel.text(Text.t("После подтверждения код можно удалить. Повторная проверка не нужна."));

        final EditText handle = haveHandle ? null : panel.field(Text.t("Ваш @ник в TikTok"));
        final Panel.Click check = () -> {
            final String name = haveHandle ? known
                    : handleFrom(handle.getText().toString());
            if (name == null) {
                Toast.makeText(this, Text.t("Введите ник TikTok без ссылки"), Toast.LENGTH_LONG).show();
                return;
            }
            panel.close();
            Toast.makeText(this, Text.t("Проверяю профиль…"), Toast.LENGTH_SHORT).show();
            Net.away("profile verify", () -> {
                boolean verified = false;
                try { verified = sync.verify(uid, name); } catch (Throwable ignored) { }
                final boolean okay = verified;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (okay && uid.equals(Account.id())) ProfileStyle.share(true);
                    Toast.makeText(this, okay ? Text.t("Профиль подтверждён. Оформление отправляется.")
                            : Text.t("Не нашли код в описании. Проверьте ник и код; TikTok иногда обновляет описание с задержкой, дальше проверю сам."),
                            Toast.LENGTH_LONG).show();
                    if (!okay) TtcuzProfileSync.autoVerifyOwn(true);
                    rebuild();
                });
            });
        };
        if (haveHandle) {
            // One tap: copy, open the profile to paste it, and the check runs on return.
            panel.keepOpen(Text.t("Вставить код вручную"), () -> {
                copy("ttcuz", code);
                verifyWaiting = true;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.tiktok.com/@" + known)));
                } catch (Throwable error) {
                    Toast.makeText(this, Text.t("Код скопирован. Вставьте его в описание TikTok"), Toast.LENGTH_LONG).show();
                }
            });
            panel.keepOpen(Text.t("Код уже в описании, проверить"), check);
        } else {
            panel.primaryKeepOpen(Text.t("Проверить"), check);
        }
        panel.quiet(Text.CANCEL, null);
        panel.show();
    }

    private void chooseSeekStep(){
        new ModDialog.Builder(this).setItems(new String[]{"5","10","15","30","60",Text.CUSTOM_SPEED},(dialog,index)->{
            if(index<5){AccountAppearance.prefs(this).edit().putInt(PlayerTools.STEP,new int[]{5,10,15,30,60}[index]).apply();rebuild();return;}
            Panel panel=Panel.with(this,skin,Text.SEEK_STEP);android.widget.EditText input=panel.field("1–60");input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);input.setText(Integer.toString(PlayerTools.step()));
            panel.primaryKeepOpen(Text.APPLY,()->{try{int value=Integer.parseInt(input.getText().toString());if(value<1 || value>60){Screen.say("1–60");return;}AccountAppearance.prefs(this).edit().putInt(PlayerTools.STEP,value).apply();panel.close();rebuild();}catch(Throwable ignored){Screen.say("1–60");}});panel.quiet(Text.CLOSE,null);panel.show();
        }).show();
    }
    private void showHiddenItems() {
        Panel panel=Panel.with(this,skin,Text.HIDDEN_ITEMS);
        LinearLayout rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);
        String[] keys={Feed.KEY,Feed.KEY_LIVE,Feed.KEY_PHOTOS,Feed.KEY_STORIES,Feed.KEY_SUGGESTIONS};
        String[] labels={Text.HIDE_ADS,Text.HIDE_LIVE,Text.HIDE_PHOTOS,Text.HIDE_STORIES,Text.HIDE_SUGGESTIONS};
        int count=0;
        for(int i=0;i<keys.length;i++){
            final String key=keys[i];
            if(!(key.equals(Feed.KEY)?Feed.isEnabled():Feed.hides(key)))continue;
            count++;
            rows.addView(actionRow("visibility_off",labels[i],Text.HIDDEN_RESTORE,()->{
                if(key.equals(Feed.KEY))Feed.setEnabled(false);else Feed.setHides(key,false);
                panel.close();rebuild();Screen.say(Text.HIDDEN_NEXT_PAGE);
            }));
        }
        String[] railNames={Text.RAIL_LIKE,Text.RAIL_COMMENT,Text.RAIL_FAV,Text.RAIL_SHARE};
        for(int i=0;i<4;i++)if(!FeedRail.shown(i)){
            final int kind=i;count++;
            rows.addView(actionRow("visibility_off",railNames[i],Text.HIDDEN_RESTORE,()->{
                FeedRail.setShown(kind,true);panel.close();rebuild();
            }));
        }
        for(java.util.Map.Entry<Integer,String> card:FeedBlacklist.cards().entrySet()){
            final int type=card.getKey();count++;
            rows.addView(actionRow("group",card.getValue(),Text.HIDDEN_RESTORE,()->{FeedBlacklist.removeCard(type);panel.close();rebuild();}));
        }
        for(java.util.Map.Entry<String,String> author:FeedBlacklist.items().entrySet()){
            final String uid=author.getKey();count++;
            rows.addView(actionRow("group",author.getValue(),Text.HIDDEN_RESTORE,()->{FeedBlacklist.remove(uid);panel.close();rebuild();}));
        }
        if(count==0)rows.addView(caption(Text.HIDDEN_EMPTY));
        else {
            rows.addView(caption(Text.HIDDEN_NEXT_PAGE));
            panel.primary(Text.HIDDEN_RESTORE_ALL,()->{
                Feed.setEnabled(false);Feed.setHides(Feed.KEY_LIVE,false);Feed.setHides(Feed.KEY_PHOTOS,false);
                Feed.setHides(Feed.KEY_STORIES,false);Feed.setHides(Feed.KEY_SUGGESTIONS,false);FeedBlacklist.clear();
                for(int i=0;i<4;i++)FeedRail.setShown(i,true);
                rebuild();
            });
        }
        panel.view(rows);panel.quiet(Text.CLOSE,null);panel.show();
    }

    private void searchResults() {
        column.addView(section(Text.SEARCH_RESULTS));
        LinearLayout results = card();
        int found = 0;
        found+=searchControl(results,Text.FEED,Text.HIDE_ADS,Feed.isEnabled());
        found+=searchControl(results,Text.FEED,Text.HIDE_LIVE,Feed.hides(Feed.KEY_LIVE));
        found+=searchControl(results,Text.FEED,Text.HIDE_PHOTOS,Feed.hides(Feed.KEY_PHOTOS));
        found+=searchControl(results,Text.FEED,Text.HIDE_STORIES,Feed.hides(Feed.KEY_STORIES));
        found+=searchControl(results,Text.FEED,Text.HIDE_SUGGESTIONS,Feed.hides(Feed.KEY_SUGGESTIONS));
        found+=searchHit(results,Text.PINNED_FRIENDS,Text.PINNED_FRIENDS+" "+Text.MESSAGE_TIMES,()->{});
        found+=searchHit(results,Text.NOTIFICATIONS,Text.NOTIFY_MESSAGES+" "+Text.NOTIFY_PROMOS+" "+Text.NOTIFY_OTHER+" "+Text.NOTIFY_NO_PROMPTS+" "+Text.NOTIFY_SETTINGS,()->{});
        found+=searchHit(results,Text.VIDEO,Text.PLAYER_SPEED,()->{});
        found+=searchHit(results,Text.DOWNLOADS,Text.DL_QUALITY+" "+Text.FRAME_QUALITY,()->{});
        String[] railNames={Text.RAIL_LIKE,Text.RAIL_COMMENT,Text.RAIL_FAV,Text.RAIL_SHARE};
        for(int i=0;i<4;i++)found+=searchControl(results,FEED_BUTTONS,railNames[i],FeedRail.shown(i));
        if(matchesSearch(Text.HIDDEN_ITEMS)){
            results.addView(actionRow("visibility_off",Text.HIDDEN_ITEMS,Text.HIDDEN_ITEMS_NOTE,this::showHiddenItems));found++;
        }
        found+=searchControl(results,Text.DOWNLOADS,Text.NO_WATERMARK,Download.isEnabled());
        found+=searchControl(results,Text.VIDEO,Text.ALWAYS_DATE,Dates.isEnabled());
        found+=searchControl(results,Text.INPUT_LIMITS,Text.UNLIMITED_COMMENTS,Flags.isOn(TextLimits.COMMENTS));
        found+=searchControl(results,Text.INPUT_LIMITS,Text.UNLIMITED_REPOSTS,Flags.isOn(TextLimits.REPOSTS));
        found+=searchControl(results,Text.INPUT_LIMITS,Text.UNLIMITED_HASHTAGS,Flags.isOn(TextLimits.HASHTAGS));
        found+=searchControl(results,Text.DIM_SECTION,Text.DIM,Dim.isConfigured());
        found+=searchControl(results,Text.PROFILE_LOOK,Text.PROFILE_GRADIENT,ProfileStyle.gradient());
        found+=searchControl(results,Text.PROFILE_SYNC,Text.PROFILE_SHARE,ProfileStyle.sharing());
        if(found==0){
        found += searchHit(results, Text.REGION, Text.SEARCH_REGION,
                () -> { countriesOpen = true; });
        found += searchHit(results, Text.ACCENT, Text.SEARCH_ACCENT,
                () -> { accentOpen = true; });
        found += searchHit(results, Text.THEME, Text.SEARCH_THEME,
                () -> { textOpen = true; backgroundOpen = true; themeColoursOpen = true; });
        found += searchHit(results, Text.FEED, Text.SEARCH_FEED, () -> { });
        found += searchHit(results, Text.VIDEO, Text.SEARCH_VIDEO, () -> { });
        found += searchHit(results, Text.DOWNLOADS, Text.SEARCH_DOWNLOADS, () -> { });
        found += searchHit(results, Text.BACKUP, Text.SEARCH_BACKUP, () -> { });
        found += searchHit(results, Text.FONT, Text.SEARCH_FONT,
                () -> { fontOpen = true; });
        found += searchHit(results, Text.PROFILE_STYLE, Text.t("значок ник градиент синхронизация профиль"), () -> { });
        }
        if (found == 0) results.addView(caption(Text.SEARCH_NONE));
        column.addView(wrap(results));
    }

    private int searchControl(LinearLayout into,String section,String title,boolean on){
        if(!matchesSearch(title))return 0;if(into.getChildCount()>0)into.addView(line());
        into.addView(actionRow("search",title,on?Text.t("Включено"):Text.t("Выключено"),()->{searchQuery="";settingsGroup=groupOf(section);openSection=SUBPAGES.containsKey(section)?section:null;rebuild();page.post(()->{View target=settingTargets.get(title);if(target==null)return;int offset=0;View cursor=target;while(cursor!=column){offset+=cursor.getTop();if(!(cursor.getParent() instanceof View))break;cursor=(View)cursor.getParent();}page.smoothScrollTo(0,Math.max(0,offset-dp(16)));Motion.change(target);});}));return 1;
    }
    private int searchHit(LinearLayout into, String title, String words, final Runnable open) {
        if (!matchesSearch(title + " " + words)) return 0;
        if (into.getChildCount() > 0) into.addView(line());
        into.addView(actionRow("search", title, currentValue(title), () -> {
            open.run();
            searchQuery = "";
            settingsGroup = groupOf(title);
            openSection = SUBPAGES.containsKey(title) ? title : null;
            page.scrollTo(0, 0);
            rebuild();
            final View target = sectionTargets.get(title);
            if (target != null) page.post(() -> page.smoothScrollTo(0, target.getTop()));
        }));
        return 1;
    }

    private String currentValue(String title){
        if(title.equals(Text.ACCENT))return String.format(Locale.US,"#%06X",Accent.colour()&0xFFFFFF);
        if(title.equals(Text.THEME))return Themes.isEnabled()?Text.t("Включена"):Text.t("Выключена");
        if(title.equals(Text.FEED))return Feed.isEnabled()?Text.t("Реклама скрыта"):Text.t("Реклама разрешена");
        if(title.equals(Text.DOWNLOADS))return Download.isEnabled()?Text.t("Без водяного знака"):Text.t("Обычное сохранение");
        if(title.equals(Text.PROFILE_STYLE))return Account.profileHandle()==null?Text.t("Откройте свой профиль"):"@"+Account.profileHandle();
        return Text.t("Открыть настройки");
    }
    private boolean matchesSearch(String words) {
        return words.toLowerCase(Locale.ROOT).contains(searchQuery);
    }

    private View section(String text) {
        renderDisclosureKey=null;
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(accentInk());
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        view.setLetterSpacing(0.007f);
        view.setGravity(Gravity.CENTER_HORIZONTAL);
        view.setPadding(dp(24), dp(20), dp(24), dp(6));
        sectionTargets.put(text, view);
        return view;
    }

    private static final String BADGE_SECTION = Text.t("Значок у ника");

    private String groupName(int group) {
        switch (group) {
            case 1: return Text.t("Оформление");
            case 2: return Text.t("Профиль");
            case 3: return Text.t("Лента и видео");
            case 4: return Text.t("Инструменты");
            case 5: return "ttcuz";
            case 6: return Text.t("Плагины");
            case 7: return Text.t("Друзья и чаты");
            case 8: return "ayumoments";
            default: return "ttcuz";
        }
    }

    private void showSectionHelp() {
        String[] notes = {"", Text.t("Акцент меняет цвет кнопок и вкладок. Готовые картинки могут сохранять свой цвет. Тема, шрифты текста и эмодзи настраиваются отдельно."),
                Text.t("Стиль ника и личный значок видны в превью. Для публикации значка подтвердите аккаунт. Выдавать значки другим может администратор."),
                Text.t("Глаз меняет прозрачность кнопок поверх видео; «Защита экрана» находится ниже «Комментариев». Удержание «Отправить» открывает скачивание и перемотку. Удерживайте видео: вниз закрепить скорость, вверх отменить выбор; после фиксации вниз — снять её."),
                Text.t("Скачивание, резервные копии, диагностика и журнал. Сохраните копию перед экспериментами."),
                Text.t("Здесь находятся версия мода, обновления, журнал и участники."),
                Text.t("Плагины добавляют функции без пересборки мода. Ставьте только то, чему доверяете: плагин работает внутри TikTok со всеми его правами."),
                Text.t("Закреплённые друзья, автопродление серий, приватность в чатах и уведомления мода.")};
        Panel.with(this,skin,Text.t("Подсказки")).text(notes[Math.max(0,Math.min(notes.length-1,settingsGroup))])
                .quiet(Text.t("Обучение"),() -> Tutorial.show(this)).primary(Text.t("Понятно"),null).show();
    }

    /** The shelves, in the order of the main list, with the icon each one has there. */
    /** The home tiles, most used first: the feed, the look, friends, the profile, plugins, tools, ayumoments. */
    private static final int[] SHELVES = {3, 1, 7, 2, 6, 4, 8};
    private static final String[] SHELF_ICONS = {"palette", "group", "play_circle", "tune", "info", "extension", "chat", "ghost"};
    private static final String HOME_ORDER_KEY = "home_tile_order";
    private static final String HOME_SIZES_KEY = "home_tile_sizes";
    private static final String AYU_PEOPLE = Text.t("Невидимка"), AYU_DEVICE = Text.t("Телефон и поиск"),
            FEED_BUTTONS = Text.t("Кнопки в ленте"), WATCHED = Text.t("Просмотренные видео"),
            COMMENTS_SECTION = Text.t("Комментарии"), CHATS = Text.t("Чаты");

    private static int indexOf(int[] values, int value) {
        for (int i = 0; i < values.length; i++) if (values[i] == value) return i;
        return 0;
    }

    private View groupNavigation() {
        return wrap(shelfList(SHELVES));
    }

    private int[] homeTileOrder() {
        String saved = getSharedPreferences(Ttcuz.PREFS, MODE_PRIVATE).getString(HOME_ORDER_KEY, "");
        if (saved.isEmpty()) return SHELVES.clone();
        String[] parts = saved.split(",", -1);
        if (parts.length != SHELVES.length) return SHELVES.clone();
        int[] order = new int[SHELVES.length];
        boolean[] seen = new boolean[9];
        for (int i = 0; i < parts.length; i++) {
            try { order[i] = Integer.parseInt(parts[i]); }
            catch (NumberFormatException error) { return SHELVES.clone(); }
            if (order[i] < 1 || order[i] > 8 || order[i] == 5 || seen[order[i]])
                return SHELVES.clone();
            seen[order[i]] = true;
        }
        for (int group : SHELVES) if (!seen[group]) return SHELVES.clone();
        return order;
    }

    private void openShelf(final int chosen) { openShelf(chosen, null); }

    /**
     * Into a shelf: the tapped row stays a moment while everything else on screen slides off to the left, one row
     * after another; the shelf then comes in from the right the same way.
     */
    private void openShelf(final int chosen, final View tapped) {
        if(navigating)return;navigating=true;
        UiSound.open();
        openSection = null;
        // from a home tile: the page grows out of it, as Android 16 opens an app from its icon
        if (tapped != null && settingsGroup == 0 && Motion.enabled(this) && root.getWidth() > 0) {
            growFrom(tapped, () -> { settingsGroup = chosen; page.scrollTo(0, 0); rebuild(); });
            return;
        }
        long out = leave(-1f, tapped);
        column.postDelayed(()->{
            if(isFinishing()||isDestroyed())return;
            settingsGroup=chosen;page.scrollTo(0,0);rebuild();arrive(1f);navigating=false;
        }, out);
    }

    // ---------------------------------------------------- a page opened as Android 16 opens an app

    private static final long MORPH = 480L;
    /** iOS's own curve for an app flying out of its icon: quick, then a long soft landing. */
    private static final android.view.animation.PathInterpolator FLY = new android.view.animation.PathInterpolator(.32f, .72f, 0f, 1f);

    /**
     * Shows `view` (as big as root, w by h) as a card at `r` with corners `radius`: the whole view scaled down to the
     * card's width -- its content shrinks with it, it is not only cut -- and cut to the card's height from its top.
     */
    private static void place(View view, final android.graphics.RectF r, final float radius, float w, float h) {
        final float s = Math.max(.01f, r.width() / w), localH = Math.min(h, r.height() / s);
        scaleInto(view, r, w);
        final float w0 = w;
        view.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View v, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, Math.round(w0), Math.round(localH), radius / s);
            }
        });
        view.setClipToOutline(true);
    }

    /** Scales `view` (w wide) from its top left corner so that it starts at r's top left and is r's width. */
    private static void scaleInto(View view, android.graphics.RectF r, float w) {
        float s = Math.max(.01f, r.width() / w);
        view.setPivotX(0f); view.setPivotY(0f);
        view.setScaleX(s); view.setScaleY(s);
        view.setTranslationX(r.left); view.setTranslationY(r.top);
    }

    /** What is on screen now, as one picture (taken once per change of page; the animation itself draws nothing new). */
    private Bitmap shot() {
        try {
            Bitmap picture = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.RGB_565);
            root.draw(new android.graphics.Canvas(picture));
            return picture;
        } catch (Throwable error) { Diary.note("page shot: " + error); return null; }
    }

    /** A view's rectangle inside root. */
    private android.graphics.RectF inRoot(View view) {
        int[] a = new int[2], b = new int[2];
        view.getLocationInWindow(a);
        root.getLocationInWindow(b);
        return new android.graphics.RectF(a[0] - b[0], a[1] - b[1], a[0] - b[0] + view.getWidth(), a[1] - b[1] + view.getHeight());
    }

    private static float mix(float a, float b, float t) { return a + (b - a) * t; }

    /** A view as it looks now, as a picture of its own size (null when it has none or memory is short). */
    private static Bitmap picture(View view) {
        try {
            if (view == null || view.getWidth() <= 0 || view.getHeight() <= 0) return null;
            Bitmap picture = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            view.draw(new android.graphics.Canvas(picture));
            return picture;
        } catch (Throwable error) { Diary.note("tile shot: " + error); return null; }
    }

    /** Lays `picture` (an image of a view as big as `from`) exactly over the rectangle `r`, stretched to it. */
    private static void stretch(View picture, android.graphics.RectF from, android.graphics.RectF r) {
        picture.setPivotX(0f); picture.setPivotY(0f);
        picture.setTranslationX(r.left); picture.setTranslationY(r.top);
        picture.setScaleX(r.width() / Math.max(1f, from.width()));
        picture.setScaleY(r.height() / Math.max(1f, from.height()));
    }

    /**
     * Opening, as an app opens from its icon. Everything that moves is a picture, so nothing is laid out, rebuilt or
     * shown half-made while it moves, and nothing blinks: the screen before (with a gap where the tile was) stays
     * behind, sinking to 90 % and darkening; the new page, laid out but not shown, is drawn once into a picture, and
     * that picture flies out of the tile's exact rectangle to the whole screen -- its content scaled with it, its
     * corners going from the tile's 28 dp to none -- under a picture of the tile itself that fades away over the first
     * third, so the tile turns into the page. On the last frame the real page takes the picture's place: the same
     * pixels, in the same place.
     */
    private void growFrom(final View tile, Runnable swap) {
        final android.graphics.RectF from = inRoot(tile);
        final float w = root.getWidth(), h = root.getHeight(), full = dp(28);
        final Bitmap tilePicture = picture(tile);
        // as on the iPhone the icon leaves its place: the screen behind has a gap where the tile was
        tile.setAlpha(0f);
        final Bitmap before = shot();
        tile.setAlpha(1f);
        if (before == null || tilePicture == null) { swap.run(); navigating = false; return; }
        final ImageView under = new ImageView(this);
        under.setImageBitmap(before);
        final View dim = new View(this);
        dim.setBackgroundColor(0xFF000000);
        dim.setAlpha(0f);
        root.addView(under, 0, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(dim, 1, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // the real page is built now but stays unseen until the flight ends
        page.setAlpha(0f);
        swap.run();
        navigating = true; // rebuild() let go of it: no second change of page while this one moves
        final ImageView card = new ImageView(this);
        final ImageView face = new ImageView(this);
        face.setImageBitmap(tilePicture);
        face.setScaleType(ImageView.ScaleType.FIT_XY);
        root.addView(card, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(face, new FrameLayout.LayoutParams(Math.round(from.width()), Math.round(from.height()), Gravity.LEFT | Gravity.TOP));
        final android.graphics.RectF now = new android.graphics.RectF(from);
        place(card, now, full, w, h);
        stretch(face, from, now);
        // the new page's picture is taken once it is laid out: on the frame before it would first be drawn
        root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                root.getViewTreeObserver().removeOnPreDrawListener(this);
                Bitmap after;
                try {
                    after = Bitmap.createBitmap(Math.round(w), Math.round(h), Bitmap.Config.RGB_565);
                    android.graphics.Canvas canvas = new android.graphics.Canvas(after);
                    canvas.drawColor(skin.page);
                    if (snow != null) snow.draw(canvas);
                    page.draw(canvas); // its own alpha is the parent's business: drawn here in full
                } catch (Throwable error) {
                    Diary.note("page shot: " + error);
                    after = null;
                }
                if (after == null) {
                    root.removeView(card); root.removeView(face); root.removeView(under); root.removeView(dim);
                    page.setAlpha(1f); navigating = false;
                    return true;
                }
                card.setImageBitmap(after);
                ValueAnimator grow = ValueAnimator.ofFloat(0f, 1f);
                grow.setDuration(Motion.time(MORPH));
                grow.setInterpolator(FLY);
                grow.addUpdateListener(a -> {
                    float t = (float) a.getAnimatedValue();
                    now.set(mix(from.left, 0, t), mix(from.top, 0, t), mix(from.right, w, t), mix(from.bottom, h, t));
                    place(card, now, mix(full, 0f, t), w, h);
                    stretch(face, from, now);
                    face.setAlpha(Math.max(0f, 1f - t / .3f));
                    under.setScaleX(mix(1f, .9f, t)); under.setScaleY(mix(1f, .9f, t));
                    dim.setAlpha(.45f * t);
                });
                grow.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(android.animation.Animator animation) {
                        // the same pixels in the same place: the real page shows as the picture goes
                        page.setAlpha(1f);
                        root.removeView(card);
                        root.removeView(face);
                        root.removeView(under);
                        root.removeView(dim);
                        navigating = false;
                    }
                });
                grow.start();
                return true;
            }
        });
    }

    /**
     * Closing, the opening backwards: the page as a picture flies from the whole screen into its tile's exact
     * rectangle (the home page coming forward from 90 % and lightening behind it), and over its last part the picture
     * of the tile fades in over it, so the page turns back into the tile; then the real tile is shown under its picture.
     */
    private void shrinkInto(final int group, Runnable swap) {
        final Bitmap leaving = shot();
        if (leaving == null) { swap.run(); navigating = false; return; }
        final ImageView over = new ImageView(this);
        over.setImageBitmap(leaving);
        final View dim = new View(this);
        dim.setBackgroundColor(0xFF000000);
        dim.setAlpha(.45f);
        swap.run();
        navigating = true; // rebuild() let go of it: no second change of page while this one moves
        root.addView(dim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(over, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        page.setScaleX(.9f); page.setScaleY(.9f);
        final float w = root.getWidth(), h = root.getHeight(), full = dp(28);
        final android.graphics.RectF now = new android.graphics.RectF(0, 0, w, h);
        // the tile is where the home page lays it out: read on the first frame of that layout
        root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                root.getViewTreeObserver().removeOnPreDrawListener(this);
                final View tile = homeTiles.get(group);
                final boolean found = tile != null && tile.isAttachedToWindow();
                final Bitmap tilePicture = found ? picture(tile) : null;
                // the tile's place stays empty until the page lands in it
                if (found) tile.setAlpha(0f);
                final android.graphics.RectF to = found ? inRoot(tile)
                        : new android.graphics.RectF(w * .3f, h * .4f, w * .7f, h * .6f);
                // the home page is scaled now (90 % about its middle): where the tile's corner will be at full size;
                // its size is its own, unscaled
                float cx = w / 2f, cy = h / 2f, k = 1f / .9f;
                float left = cx + (to.left - cx) * k, top = cy + (to.top - cy) * k;
                if (found) to.set(left, top, left + tile.getWidth(), top + tile.getHeight());
                else to.set(left, top, cx + (to.right - cx) * k, cy + (to.bottom - cy) * k);
                final ImageView face = new ImageView(SettingsActivity.this);
                if (tilePicture != null) {
                    face.setImageBitmap(tilePicture);
                    face.setScaleType(ImageView.ScaleType.FIT_XY);
                    face.setAlpha(0f);
                    root.addView(face, new FrameLayout.LayoutParams(Math.round(to.width()), Math.round(to.height()), Gravity.LEFT | Gravity.TOP));
                }
                ValueAnimator shrink = ValueAnimator.ofFloat(0f, 1f);
                shrink.setDuration(Motion.time(MORPH));
                shrink.setInterpolator(FLY);
                shrink.addUpdateListener(a -> {
                    float t = (float) a.getAnimatedValue();
                    now.set(mix(0, to.left, t), mix(0, to.top, t), mix(w, to.right, t), mix(h, to.bottom, t));
                    place(over, now, mix(0f, full, t), w, h);
                    // the page turns into the tile over the last 40 %
                    float turn = Math.max(0f, Math.min(1f, (t - .6f) / .4f));
                    over.setAlpha(tilePicture != null ? 1f : 1f - turn);
                    if (tilePicture != null) { stretch(face, to, now); face.setAlpha(turn); }
                    page.setScaleX(mix(.9f, 1f, t)); page.setScaleY(mix(.9f, 1f, t));
                    dim.setAlpha(mix(.45f, 0f, t));
                });
                shrink.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(android.animation.Animator animation) {
                        page.setScaleX(1f); page.setScaleY(1f);
                        if (found) tile.setAlpha(1f);
                        root.removeView(over);
                        root.removeView(face);
                        root.removeView(dim);
                        navigating = false;
                    }
                });
                shrink.start();
                return true;
            }
        });
    }

    /** The rows on screen slide off towards `direction` (-1 left, 1 right), staggered; returns when they are gone. */
    private long leave(float direction, View tapped) {
        if (!Motion.enabled(this) || page == null) return 0L;
        java.util.List<View> rows = visibleRows();
        float width = column.getWidth();
        int k = 0;
        for (View row : rows) {
            boolean stays = tapped != null && (row == tapped || isAncestor(row, tapped));
            long delay = Math.min(k, 7) * 16L + (stays ? 90L : 0L);
            row.animate().cancel();
            row.animate().translationX(direction * width * (stays ? .08f : .22f)).alpha(0f)
                    .setStartDelay(delay).setDuration(Motion.time(160L)).setInterpolator(Motion.IN).start();
            if (!stays) k++;
        }
        return Motion.time(160L) + Math.min(k, 7) * 16L + 90L;
    }

    /** The new page's rows come in from `direction`, staggered, with one soft landing each. */
    private void arrive(float direction) {
        if (!Motion.enabled(this) || page == null) return;
        // the page is laid out on the next frame; until then nothing of it is drawn
        column.setAlpha(0f);
        column.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                column.getViewTreeObserver().removeOnPreDrawListener(this);
                column.setAlpha(1f);
                java.util.List<View> rows = visibleRows();
                float width = column.getWidth();
                int k = 0;
                for (View row : rows) {
                    row.setAlpha(0f);
                    row.setTranslationX(direction * width * .16f);
                    row.animate().alpha(1f).translationX(0f).setStartDelay(Math.min(k, 9) * 18L)
                            .setDuration(Motion.time(Motion.MEDIUM)).setInterpolator(Motion.OUT).start();
                    k++;
                }
                return true;
            }
        });
    }

    /** What moves in a page change: each row of a group on its own (its tile is its own), and everything else whole. */
    private java.util.List<View> visibleRows() {
        java.util.List<View> rows = new java.util.ArrayList<>();
        int top = page.getScrollY(), bottom = top + page.getHeight();
        for (int i = 0; i < column.getChildCount(); i++) {
            View child = column.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE || child.getBottom() < top || child.getTop() > bottom) continue;
            Expressive.Group group = groupIn(child);
            if (group == null) { rows.add(child); continue; }
            // a row may travel past its group's edge to the screen's
            group.setClipChildren(false);
            if (child instanceof ViewGroup) ((ViewGroup) child).setClipChildren(false);
            for (int j = 0; j < group.getChildCount(); j++) {
                View row = group.getChildAt(j);
                int at = child.getTop() + group.getTop() + row.getTop();
                if (row.getVisibility() == View.VISIBLE && at + row.getHeight() >= top && at <= bottom) rows.add(row);
            }
        }
        return rows;
    }

    private static Expressive.Group groupIn(View view) {
        if (view instanceof Expressive.Group) return (Expressive.Group) view;
        if (view instanceof ViewGroup && ((ViewGroup) view).getChildCount() == 1 && ((ViewGroup) view).getChildAt(0) instanceof Expressive.Group)
            return (Expressive.Group) ((ViewGroup) view).getChildAt(0);
        return null;
    }

    private static boolean isAncestor(View parent, View child) {
        for (Object up = child.getParent(); up instanceof View; up = ((View) up).getParent()) if (up == parent) return true;
        return false;
    }

    /** The shelves as a list of rows, in the order given: a tinted symbol and the name, nothing else. */
    private LinearLayout shelfList(int[] shelves) {
        LinearLayout list = card();
        for (int n = 0; n < shelves.length; n++) {
            final int i = shelves[n];
            final LinearLayout entry = row();
            entry.addView(shelfIcon(SHELF_ICONS[i-1]));
            TextView head = label(groupName(i)); head.setTextSize(17);
            entry.setContentDescription(groupName(i));
            entry.addView(head,grow()); entry.addView(away());
            entry.setMinimumHeight(dp(60));
            entry.setOnClickListener(v -> openShelf(i, entry));
            list.addView(entry);
        }
        return list;
    }

    private View shelfTiles(int[] shelves) {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setPadding(dp(16), dp(4), dp(16), dp(8));
        LayoutTransition transition = new LayoutTransition();
        transition.enableTransitionType(LayoutTransition.CHANGING);
        transition.setDuration(LayoutTransition.CHANGING, 180L);
        grid.setLayoutTransition(transition);

        final int[] order = shelves.clone();
        final int[][] sizes = homeTileSizes();
        for (int index = 0; index < order.length; index++) {
            final int group = order[index];
            final View tile = shelfTile(group, grid, order, sizes);
            tile.setTag(Integer.valueOf(group));
            grid.addView(tile, homeTileLayoutParams(index, order, sizes));
        }
        joinTiles(grid, order, sizes, false);
        return grid;
    }

    /** Large outer corners of the whole group, small ones where tiles meet (Material 3's connected containers). */
    private static final int TILE_OUTER = 28, TILE_INNER = 6;

    /**
     * Rounds each tile as one piece of a single block: a corner is large only where nothing sits on either side of it
     * (the block's own outline), small where it touches a neighbour. Moved or resized, the corners turn to their new
     * shape in 180 ms.
     */
    private void joinTiles(GridLayout grid, int[] order, int[][] sizes, boolean animate) {
        int rows = 0;
        int[][] cells = new int[9][];
        for (int group : order) {
            cells[group] = homeTileCell(order, sizes, group);
            rows = Math.max(rows, cells[group][0] + sizes[group][1]);
        }
        boolean[][] taken = new boolean[rows + 1][2];
        for (int group : order)
            for (int y = cells[group][0]; y < cells[group][0] + sizes[group][1]; y++)
                for (int x = cells[group][1]; x < cells[group][1] + sizes[group][0]; x++) taken[y][x] = true;
        for (int i = 0; i < grid.getChildCount(); i++) {
            View tile = grid.getChildAt(i);
            if (!(tile.getTag() instanceof Integer) || !(tile.getBackground() instanceof GradientDrawable)) continue;
            int group = (Integer) tile.getTag();
            int top = cells[group][0], left = cells[group][1], bottom = top + sizes[group][1] - 1, right = left + sizes[group][0] - 1;
            float big = dp(TILE_OUTER), small = dp(TILE_INNER);
            // each corner: large when the cell beside it and the cell above/below it are both empty
            float tl = !filled(taken, top, left - 1) && !filled(taken, top - 1, left) ? big : small;
            float tr = !filled(taken, top, right + 1) && !filled(taken, top - 1, right) ? big : small;
            float br = !filled(taken, bottom, right + 1) && !filled(taken, bottom + 1, right) ? big : small;
            float bl = !filled(taken, bottom, left - 1) && !filled(taken, bottom + 1, left) ? big : small;
            float[] want = {tl, tl, tr, tr, br, br, bl, bl};
            GradientDrawable shape = (GradientDrawable) tile.getBackground();
            Object had = tile.getTag(TILE_CORNERS);
            float[] from = had instanceof float[] ? (float[]) had : null;
            tile.setTag(TILE_CORNERS, want);
            if (!animate || from == null || !Motion.enabled(this)) { shape.setCornerRadii(want); continue; }
            if (java.util.Arrays.equals(from, want)) continue;
            android.animation.ValueAnimator morph = android.animation.ValueAnimator.ofFloat(0f, 1f);
            morph.setDuration(180L);
            morph.setInterpolator(Motion.OUT);
            final float[] start = from.clone(), now = new float[8];
            morph.addUpdateListener(a -> {
                float t = (Float) a.getAnimatedValue();
                for (int k = 0; k < 8; k++) now[k] = start[k] + (want[k] - start[k]) * t;
                shape.setCornerRadii(now);
            });
            morph.start();
        }
    }

    private static final int TILE_CORNERS = 0x7e0c03c0;

    private static boolean filled(boolean[][] taken, int row, int column) {
        return row >= 0 && row < taken.length && column >= 0 && column < 2 && taken[row][column];
    }

    private GridLayout.LayoutParams homeTileLayoutParams(int index, int[] order, int[][] sizes) {
        int[] size = sizes[order[index]];
        int width = size[0], height = size[1];
        int[] cell = homeTileCell(order, sizes, order[index]);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(cell[0], height, 1f),
                GridLayout.spec(cell[1], width, 1f));
        params.width = 0;
        // 1 dp around each: tiles 2 dp apart, read as one block
        params.height = dp(132 * height + 2 * (height - 1));
        params.leftMargin = dp(1);
        params.rightMargin = dp(1);
        params.topMargin = dp(1);
        params.bottomMargin = dp(1);
        return params;
    }

    private static int[] homeTileCell(int[] order, int[][] sizes, int targetGroup) {
        boolean[][] occupied = new boolean[32][2];
        for (int group : order) {
            int width = sizes[group][0], height = sizes[group][1];
            int row = 0, column = 0;
            boolean placed = false;
            while (!placed) {
                if (column + width > 2) {
                    row++;
                    column = 0;
                    continue;
                }
                boolean fits = true;
                for (int y = row; y < row + height && fits; y++) {
                    for (int x = column; x < column + width; x++) {
                        if (occupied[y][x]) { fits = false; break; }
                    }
                }
                if (!fits) {
                    column++;
                    if (column >= 2) { row++; column = 0; }
                    continue;
                }
                if (group == targetGroup) return new int[]{row, column};
                for (int y = row; y < row + height; y++)
                    for (int x = column; x < column + width; x++) occupied[y][x] = true;
                placed = true;
                column += width;
                if (column >= 2) { row++; column = 0; }
            }
        }
        throw new IllegalArgumentException("Unknown home tile: " + targetGroup);
    }

    private static int homeTileIndex(int[] order, int group) {
        for (int i = 0; i < order.length; i++) if (order[i] == group) return i;
        return -1;
    }

    private static void moveHomeTile(int[] order, int from, int to) {
        int moving = order[from];
        if (from < to) System.arraycopy(order, from + 1, order, from, to - from);
        else System.arraycopy(order, to, order, to + 1, from - to);
        order[to] = moving;
    }

    private void positionHomeTiles(GridLayout grid, int[] order, int[][] sizes) {
        for (int index = 0; index < order.length; index++) {
            int group = order[index];
            for (int child = 0; child < grid.getChildCount(); child++) {
                View tile = grid.getChildAt(child);
                if (((Integer) tile.getTag()).intValue() == group) {
                    tile.setLayoutParams(homeTileLayoutParams(index, order, sizes));
                    break;
                }
            }
        }
        joinTiles(grid, order, sizes, true);
    }

    private void saveHomeTileOrder(int[] order) {
        getSharedPreferences(Ttcuz.PREFS, MODE_PRIVATE).edit()
                .putString(HOME_ORDER_KEY, encodeHomeTileOrder(order)).apply();
    }

    private void saveHomeTileSizes(int[][] sizes) {
        getSharedPreferences(Ttcuz.PREFS, MODE_PRIVATE).edit()
                .putString(HOME_SIZES_KEY, encodeHomeTileSizes(sizes)).apply();
    }

    private void moveTileToPointer(GridLayout grid, int[] order, int[][] sizes, int group,
                                   float screenX, float screenY) {
        int from = homeTileIndex(order, group);
        if (from < 0) return;
        int[] location = new int[2];
        for (int i = 0; i < grid.getChildCount(); i++) {
            View target = grid.getChildAt(i);
            if (((Integer) target.getTag()).intValue() == group) continue;
            target.getLocationOnScreen(location);
            if (screenX < location[0] || screenX >= location[0] + target.getWidth()
                    || screenY < location[1] || screenY >= location[1] + target.getHeight())
                continue;
            int to = homeTileIndex(order, ((Integer) target.getTag()).intValue());
            if (to >= 0 && to != from) {
                final java.util.Map<View, int[]> before = new java.util.HashMap<>();
                for (int k = 0; k < grid.getChildCount(); k++) {
                    View other = grid.getChildAt(k);
                    if (((Integer) other.getTag()).intValue() == group) continue;
                    before.put(other, new int[]{other.getLeft(), other.getTop()});
                }
                moveHomeTile(order, from, to);
                positionHomeTiles(grid, order, sizes);
                grid.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
                    @Override public boolean onPreDraw() {
                        grid.getViewTreeObserver().removeOnPreDrawListener(this);
                        for (java.util.Map.Entry<View, int[]> e : before.entrySet()) {
                            View other = e.getKey();
                            float fx = e.getValue()[0] - other.getLeft(), fy = e.getValue()[1] - other.getTop();
                            if (fx == 0 && fy == 0) continue;
                            other.setTranslationX(fx);
                            other.setTranslationY(fy);
                            other.animate().translationX(0f).translationY(0f).setDuration(220L).setInterpolator(Motion.EMPHASIZED).start();
                        }
                        return true;
                    }
                });
            }
            return;
        }
    }

    private int[][] homeTileSizes() {
        int[][] sizes = new int[9][2];
        for (int group : SHELVES) {
            sizes[group][0] = 1;
            sizes[group][1] = 1;
        }
        String saved = getSharedPreferences(Ttcuz.PREFS, MODE_PRIVATE)
                .getString(HOME_SIZES_KEY, "");
        if (saved.isEmpty()) return sizes;
        if (!Backup.validHomeTileSizes(saved)) return defaultHomeTileSizes();
        String[] entries = saved.split(",", -1);
        boolean[] seen = new boolean[9];
        for (String entry : entries) {
            String[] pair = entry.split(":", -1);
            if (pair.length != 2) return defaultHomeTileSizes();
            try {
                int group = Integer.parseInt(pair[0]);
                String[] dimensions = pair[1].split("x", -1);
                if (group < 1 || group > 8 || group == 5 || seen[group]
                        || dimensions.length != 2) return defaultHomeTileSizes();
                int width = Integer.parseInt(dimensions[0]);
                int height = Integer.parseInt(dimensions[1]);
                if (width < 1 || width > 2 || height < 1 || height > 4)
                    return defaultHomeTileSizes();
                sizes[group][0] = width;
                sizes[group][1] = height;
                seen[group] = true;
            } catch (NumberFormatException error) {
                return defaultHomeTileSizes();
            }
        }
        for (int group : SHELVES) if (!seen[group]) return defaultHomeTileSizes();
        return sizes;
    }

    private static int[][] defaultHomeTileSizes() {
        int[][] defaults = new int[9][2];
        for (int group : SHELVES) {
            defaults[group][0] = 1;
            defaults[group][1] = 1;
        }
        return defaults;
    }

    private static String encodeHomeTileSizes(int[][] sizes) {
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < SHELVES.length; i++) {
            if (i > 0) encoded.append(',');
            int group = SHELVES[i];
            encoded.append(group).append(':').append(sizes[group][0])
                    .append('x').append(sizes[group][1]);
        }
        return encoded.toString();
    }

    private static String encodeHomeTileOrder(int[] order) {
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < order.length; i++) {
            if (i > 0) encoded.append(',');
            encoded.append(order[i]);
        }
        return encoded.toString();
    }

    /** The home tiles on screen now, by their shelf: where a closing page shrinks back into. */
    private final java.util.Map<Integer, View> homeTiles = new java.util.HashMap<>();

    private View shelfTile(final int group, GridLayout grid, int[] order, int[][] sizes) {
        LinearLayout tile = new LinearLayout(this);
        homeTiles.put(group, tile);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(skin.card);
        shape.setCornerRadius(dp(28));
        tile.setBackground(shape);
        FrameLayout top = new FrameLayout(this);
        top.addView(shelfIcon(SHELF_ICONS[group - 1]),
                new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.LEFT | Gravity.TOP));
        // the corner button, only while arranging: drag it to resize the tile
        ImageView handle = new ImageView(this);
        handle.setImageDrawable(SettingsGlyph.make("aspect_ratio", accentInk()));
        handle.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        handle.setPadding(dp(7), dp(7), dp(7), dp(7));
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(accentTonal());
        handle.setBackground(dot);
        handle.setContentDescription(Text.t("Изменить размер плитки"));
        handle.setVisibility(editingTiles ? View.VISIBLE : View.GONE);
        FrameLayout.LayoutParams handleParams = new FrameLayout.LayoutParams(
                dp(32), dp(32), Gravity.RIGHT | Gravity.TOP);
        top.addView(handle, handleParams);
        tile.addView(top, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
        TextView name = label(groupName(group));
        name.setTextSize(17);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams under = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        under.topMargin = dp(18);
        tile.addView(name, under);
        FrameLayout bottom = new FrameLayout(this);
        tile.addView(bottom, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        tile.setContentDescription(groupName(group));
        tile.setClickable(true);
        Motion.touch(tile);
        tile.setOnClickListener(v -> { if (!editingTiles) openShelf(group, tile); });
        if (editingTiles) {
            tile.setOnTouchListener(moveTile(tile, group, grid, order, sizes));
            // a tile being arranged shows it: a quiet pop as the mode starts
            if (Motion.enabled(this)) { tile.setScaleX(.96f); tile.setScaleY(.96f); tile.animate().scaleX(1f).scaleY(1f).setDuration(260L).setInterpolator(new android.view.animation.OvershootInterpolator(2f)).start(); }
            if (Motion.enabled(this)) jiggle(tile, group);
        }
        handle.setClickable(true);
        handle.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                if (view.getParent() != null)
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                final float downX = event.getRawX(), downY = event.getRawY();
                final int startWidth = sizes[group][0], startHeight = sizes[group][1];
                final int[] startOrder = order.clone();
                final boolean[] down = {true}, moving = {false}, resizing = {false};
                final android.os.Handler handler = new android.os.Handler(
                        android.os.Looper.getMainLooper());
                // the corner only resizes now: moving is the tile's own (while arranging)
                final Runnable moveHold = () -> { };
                view.setTag(new Object[]{downX, downY, startWidth, startHeight,
                        startOrder, down, moving, resizing, handler, moveHold});
                return true;
            }
            Object stored = view.getTag();
            if (!(stored instanceof Object[])) return true;
            Object[] gesture = (Object[]) stored;
            float downX = (Float) gesture[0], downY = (Float) gesture[1];
            int startWidth = (Integer) gesture[2], startHeight = (Integer) gesture[3];
            int[] startOrder = (int[]) gesture[4];
            boolean[] down = (boolean[]) gesture[5], moving = (boolean[]) gesture[6];
            boolean[] resizing = (boolean[]) gesture[7];
            android.os.Handler handler = (android.os.Handler) gesture[8];
            Runnable moveHold = (Runnable) gesture[9];

            if (action == android.view.MotionEvent.ACTION_MOVE) {
                float dx = event.getRawX() - downX, dy = event.getRawY() - downY;
                if (moving[0]) {
                    tile.setTranslationX(dx);
                    tile.setTranslationY(dy);
                    moveTileToPointer(grid, order, sizes, group, event.getRawX(),
                            event.getRawY());
                    return true;
                }
                int slop = android.view.ViewConfiguration.get(SettingsActivity.this)
                        .getScaledTouchSlop();
                if (!resizing[0] && (Math.abs(dx) > slop || Math.abs(dy) > slop)) {
                    handler.removeCallbacks(moveHold);
                    resizing[0] = true;
                }
                if (resizing[0]) {
                    int cellWidth = Math.max(1, grid.getWidth() / 2);
                    int cellHeight = dp(140);
                    sizes[group][0] = Math.max(1, Math.min(2,
                            Math.round(startWidth + dx / cellWidth)));
                    sizes[group][1] = Math.max(1, Math.min(4,
                            Math.round(startHeight + dy / (float) cellHeight)));
                    positionHomeTiles(grid, order, sizes);
                }
                return true;
            }
            if (action == android.view.MotionEvent.ACTION_UP
                    || action == android.view.MotionEvent.ACTION_CANCEL) {
                down[0] = false;
                handler.removeCallbacks(moveHold);
                if (action == android.view.MotionEvent.ACTION_CANCEL) {
                    if (moving[0]) System.arraycopy(startOrder, 0, order, 0, order.length);
                    sizes[group][0] = startWidth;
                    sizes[group][1] = startHeight;
                    positionHomeTiles(grid, order, sizes);
                } else if (moving[0]) {
                    saveHomeTileOrder(order);
                } else if (resizing[0]) {
                    saveHomeTileSizes(sizes);
                }
                handle.setAlpha(1f);
                for (int i = 0; i < grid.getChildCount(); i++)
                    grid.getChildAt(i).animate().scaleX(1f).scaleY(1f).alpha(1f)
                            .translationX(0f).translationY(0f).setDuration(140L).start();
                if (view.getParent() != null)
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                view.setTag(null);
                return true;
            }
            return true;
        });
        return tile;
    }

    /** A symbol in the accent on a soft square of it: the one mark of colour on a row of the main page. */
    /**
     * While arranging: the tile follows the finger as soon as it moves, lifted (a little larger, above the rest), and
     * the others make room as it passes over them; let go, it settles into its place and the order is kept.
     */
    private View.OnTouchListener moveTile(final View tile, final int group, final GridLayout grid, final int[] order, final int[][] sizes) {
        final float[] down = new float[2], grab = new float[2];
        final boolean[] moving = {false};
        final int[] start = order.clone(), at = new int[2];
        return (view, event) -> {
            switch (event.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    down[0] = event.getRawX(); down[1] = event.getRawY();
                    moving[0] = false;
                    System.arraycopy(order, 0, start, 0, order.length);
                    if (view.getParent() != null) view.getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                case android.view.MotionEvent.ACTION_MOVE: {
                    float dx = event.getRawX() - down[0], dy = event.getRawY() - down[1];
                    if (!moving[0]) {
                        int slop = android.view.ViewConfiguration.get(SettingsActivity.this).getScaledTouchSlop();
                        if (Math.abs(dx) < slop && Math.abs(dy) < slop) return true;
                        moving[0] = true;
                        // where the finger holds the tile: it stays under that point whatever its cell becomes
                        tile.getLocationOnScreen(at);
                        grab[0] = down[0] - (at[0] - tile.getTranslationX());
                        grab[1] = down[1] - (at[1] - tile.getTranslationY());
                        tile.bringToFront();
                        tile.animate().scaleX(1.05f).scaleY(1.05f).translationZ(dp(10)).setDuration(160L).start();
                        if (Motion.haptics()) tile.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                    }
                    moveTileToPointer(grid, order, sizes, group, event.getRawX(), event.getRawY());
                    tile.getLocationOnScreen(at);
                    float baseX = at[0] - tile.getTranslationX(), baseY = at[1] - tile.getTranslationY();
                    tile.setTranslationX(event.getRawX() - grab[0] - baseX);
                    tile.setTranslationY(event.getRawY() - grab[1] - baseY);
                    return true;
                }
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    if (view.getParent() != null) view.getParent().requestDisallowInterceptTouchEvent(false);
                    if (!moving[0]) return true;
                    moving[0] = false;
                    if (event.getActionMasked() == android.view.MotionEvent.ACTION_CANCEL) {
                        System.arraycopy(start, 0, order, 0, order.length);
                        positionHomeTiles(grid, order, sizes);
                    } else saveHomeTileOrder(order);
                    tile.animate().scaleX(1f).scaleY(1f).translationX(0f).translationY(0f).translationZ(0f)
                            .setDuration(260L).setInterpolator(Motion.EMPHASIZED).start();
                    return true;
                default:
                    return true;
            }
        };
    }

    /** A tile being arranged trembles a little, as icons do on the iPhone; each a little out of step with the rest. */
    private static void jiggle(final View tile, int seed) {
        final android.animation.ObjectAnimator shake = android.animation.ObjectAnimator.ofFloat(tile, View.ROTATION, -1.1f, 1.1f);
        shake.setDuration(130L + (seed * 17L) % 40L);
        shake.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        shake.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        shake.setStartDelay((seed * 53L) % 120L);
        tile.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View v) { if (!shake.isStarted()) shake.start(); }
            // the screen is rebuilt on every change: an endless animation must end with its view
            public void onViewDetachedFromWindow(View v) { shake.cancel(); v.setRotation(0f); }
        });
        if (tile.isAttachedToWindow()) shake.start();
    }

    private View shelfIcon(String name) {
        View badge = icon(name, false, 44);
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(44), dp(44));
        size.rightMargin = dp(16);
        badge.setLayoutParams(size);
        return badge;
    }

    static final String NOT_32 = Text.t("Не поддерживается на 32-битных");

    /** A row that the 32-bit build leaves off: grey, not pressable, with a small line under it saying why. */
    private View only64(View row) {
        if (Ttcuz.is64()) return row;
        disable(row);
        row.setAlpha(.45f);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(row);
        TextView note = new TextView(this);
        note.setText(NOT_32);
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        note.setTextColor(skin.muted());
        note.setPadding(dp(72), 0, dp(16), dp(10));
        box.addView(note);
        return box;
    }

    private static void disable(View view) {
        view.setEnabled(false);
        view.setClickable(false);
        view.setOnTouchListener((v, e) -> true);
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) disable(((ViewGroup) view).getChildAt(i));
    }

    private View gap(int height) {
        View space = new View(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height)));
        return space;
    }

    /**
     * Which shelf a section lives on.
     * 1 look, 2 profile, 3 feed/video, 4 tools, 5 about,  7 chats.
     */
    private int groupOf(String section) {
        if (section.equals(Text.PROFILE_STYLE) || section.equals(Text.PROFILE_LOOK) || section.equals(Text.PROFILE_SYNC)
                || section.equals(Text.ACCOUNT) || section.equals(BADGE_SECTION)) return 2;
        if (section.equals(Text.APPEARANCE) || section.equals(Text.ACCENT)
                || section.equals(Text.THEME) || section.equals(Text.FONT) || section.equals(Text.GLASS_SECTION)
                || section.equals(Text.t("Интерфейс мода"))) return 1;
        if (section.equals(Text.t("Плагины"))) return 6;
        if (section.equals(AYU_PEOPLE) || section.equals(AYU_DEVICE)) return 8;
        if (section.equals(CHATS)) return 7;
        if (section.equals(FEED_BUTTONS) || section.equals(WATCHED) || section.equals(COMMENTS_SECTION)) return 3;
        if (section.equals(Text.REGION) || section.equals(Text.COUNTRY)
                || section.equals(Text.FEED) || section.equals(Text.VIDEO)
                || section.equals(Text.RAIL) || section.equals(Text.COMMENTS) || section.equals(Text.INPUT_LIMITS)
                || section.equals(Text.DIM_SECTION)) return 3;
        if (section.equals(Text.PINNED_FRIENDS) || section.equals(Text.STREAKS)
                || section.equals(Text.NOTIFICATIONS)) return 7;
        if (section.equals(Text.THANKS) || section.equals(Text.UPDATE) || section.equals(Text.CHANNEL)) return 5;
        return 4; // downloads, backup, diagnostics, diary
    }

    private void filterGroup() {
        int current = 0;
        boolean advanced = false, anyAdvanced = false, sub = false;
        String name = "";
        // a subpage: only its own section, its heading as the page's title
        if (openSection != null) {
            for (int i = filterFrom; i < column.getChildCount(); i++) {
                View item = column.getChildAt(i);
                if (item instanceof TextView && sectionTargets.containsValue(item)) name = ((TextView) item).getText().toString();
                item.setVisibility(item.getTag() != MORE_TAG && openSection.equals(name) ? View.VISIBLE : View.GONE);
            }
            return;
        }
        // the shelf: a section with a subpage of its own is one row that opens it, where the section was
        java.util.List<Object[]> doors = new java.util.ArrayList<>();
        for (int i = filterFrom; i < column.getChildCount(); i++) {
            View item = column.getChildAt(i);
            if (item.getTag() == MORE_TAG) { item.setVisibility(View.GONE); continue; }
            if (item instanceof TextView && sectionTargets.containsValue(item)) {
                name = ((TextView) item).getText().toString();
                current = groupOf(name);
                advanced = ADVANCED.contains(name);
                sub = SUBPAGES.containsKey(name);
                if (advanced && current == settingsGroup) anyAdvanced = true;
                if (sub && current == settingsGroup) doors.add(new Object[]{i, name});
            }
            item.setVisibility(current == settingsGroup && !sub && (!advanced || advancedOpen) ? View.VISIBLE : View.GONE);
        }
        // the doors stand together in one list, where the first of their sections was
        if (!doors.isEmpty()) {
            LinearLayout list = card();
            for (Object[] door : doors) {
                final String section = (String) door[1];
                list.addView(actionRow(SUBPAGES.get(section), section, null, () -> openSubpage(section)));
            }
            View box = wrap(list);
            box.setTag(MORE_TAG);
            column.addView(box, (Integer) doors.get(0)[0]);
        }
        // the rarely needed sections wait under one row at the end of the shelf
        if (anyAdvanced) {
            View more = wrap(actionRow(advancedOpen ? "expand_less" : "expand_more",
                    advancedOpen ? Text.t("Скрыть дополнительное") : Text.t("Дополнительно"), null,
                    () -> { advancedOpen = !advancedOpen; rebuild(); }));
            more.setTag(MORE_TAG);
            int at = column.getChildCount();
            for (int i = filterFrom; i < column.getChildCount(); i++) {
                View item = column.getChildAt(i);
                if (item instanceof TextView && sectionTargets.containsValue(item) && ADVANCED.contains(((TextView) item).getText().toString())
                        && groupOf(((TextView) item).getText().toString()) == settingsGroup) { at = i; break; }
            }
            column.addView(more, at);
        }
    }

    private static final Object MORE_TAG = new Object();

    /** The section open as a page of its own, or null. */
    private String openSection;
    /** Sections long enough for a page of their own, with the icon of the row that opens it. */
    private static final java.util.Map<String, String> SUBPAGES = new java.util.LinkedHashMap<>();
    static {
        SUBPAGES.put(FEED_BUTTONS, "favorite_border");
        SUBPAGES.put(WATCHED, "visibility");
        SUBPAGES.put(COMMENTS_SECTION, "chat");
        SUBPAGES.put(Text.THEME, "contrast");
        SUBPAGES.put(Text.FONT, "text_fields");
        SUBPAGES.put(Text.GLASS_SECTION, "view_compact");
        SUBPAGES.put(Text.STREAKS, "flare");
    }

    private void openSubpage(String section) {
        if (navigating) return;
        navigating = true;
        UiSound.open();
        long out = leave(-1f, null);
        column.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            openSection = section; page.scrollTo(0, 0); rebuild(); arrive(1f); navigating = false;
        }, out);
    }
    /** Whether the shelf's extra sections are open; kept while moving between shelves. */
    private static boolean advancedOpen;

    /** Sections that are set once or rarely: under "Дополнительно" at the end of their shelf. */
    private static final java.util.Set<String> ADVANCED = new java.util.HashSet<>(java.util.Arrays.asList(
            Text.INPUT_LIMITS, Text.t("Интерфейс мода"), Text.PROFILE_SYNC, Text.DIAGNOSTICS, Text.DIARY));

    /** Within a shelf, what is used every day first; the rest keep the order they were written in. */
    private static final String[] FIRST = {
            // anti-burn second on its shelf, right after the feed
            Text.FEED, Text.DIM_SECTION, Text.VIDEO, FEED_BUTTONS, COMMENTS_SECTION, Text.COMMENTS,
            Text.ACCENT, Text.THEME, Text.GLASS_SECTION, Text.APPEARANCE, Text.FONT,
            Text.PROFILE_STYLE, Text.PROFILE_LOOK, Text.ACCOUNT,
            Text.DOWNLOADS, Text.BACKUP};

    /**
     * Puts each shelf's sections in order of use: the page is built as one long column (search needs every row), so
     * the sections are moved as whole blocks -- a heading and everything up to the next heading -- before the shelf
     * is cut out of it.
     */
    private void arrangeSections() {
        java.util.List<java.util.List<View>> blocks = new java.util.ArrayList<>();
        java.util.List<View> lead = new java.util.ArrayList<>();
        for (int i = filterFrom; i < column.getChildCount(); i++) {
            View item = column.getChildAt(i);
            if (item instanceof TextView && sectionTargets.containsValue(item)) blocks.add(new java.util.ArrayList<>());
            (blocks.isEmpty() ? lead : blocks.get(blocks.size() - 1)).add(item);
        }
        if (blocks.size() < 2) return;
        java.util.List<String> first = java.util.Arrays.asList(FIRST);
        java.util.List<java.util.List<View>> sorted = new java.util.ArrayList<>(blocks);
        java.util.Collections.sort(sorted, (a, b) -> Integer.compare(rank(a, first), rank(b, first)));
        if (sorted.equals(blocks)) return;
        while (column.getChildCount() > filterFrom) column.removeViewAt(column.getChildCount() - 1);
        for (View item : lead) column.addView(item);
        for (java.util.List<View> block : sorted) for (View item : block) column.addView(item);
    }

    private static int rank(java.util.List<View> block, java.util.List<String> first) {
        String name = ((TextView) block.get(0)).getText().toString();
        int at = first.indexOf(name);
        return ADVANCED.contains(name) ? 1000 : at < 0 ? 500 : at;
    }

    private View pinHolder, pinEnd;
    private boolean pinListening;

    /**
     * Keeps the profile preview in view while the page scrolls, until the badge settings pass.
     *
     * Moved from the ScrollView's own scroll callback, which runs while the scroll is applied and before that frame
     * is drawn: the tree observer's callback came a frame late, and the preview trembled behind the page.
     */
    private void pinPreview() {
        View holder = pinHolder, end = pinEnd;
        if (page == null || column == null) return;
        if (!pinListening) {
            pinListening = true;
            page.setOnScrollChangeListener((view, x, y, oldX, oldY) -> pinPreview());
        }
        if (holder == null || holder.getParent() != column) return;
        int top = holder.getTop();
        int last = end != null && end.getParent() == column ? end.getBottom() : column.getHeight();
        // pinned just under the status bar, never under it
        float shift = Math.max(0, Math.min(last - top - holder.getHeight(), page.getScrollY() + statusBar() - top));
        if (holder.getTranslationY() != shift) holder.setTranslationY(shift);
        // above the rows that pass under it (Z decides the order of drawing), on the page colour, without a shadow
        boolean pinned = shift > 0;
        if (pinned != (holder.getTranslationZ() > 0)) {
            holder.setOutlineProvider(null);
            holder.setTranslationZ(pinned ? dp(1) : 0);
            holder.setBackgroundColor(pinned ? skin.page : Color.TRANSPARENT);
        }
    }

    private boolean navigating;
    private void goBack() {
        if(navigating)return;
        if (searchQuery.length() > 0 || searchOpen) { searchQuery = ""; searchOpen = false; rebuild(); }
        else if (openSection != null) {
            navigating = true;
            long out = leave(1f, null);
            column.postDelayed(() -> { if (isFinishing() || isDestroyed()) return; openSection = null; page.scrollTo(0, 0); rebuild(); arrive(-1f); navigating = false; }, out);
        }
        else if (settingsGroup != 0) {
            navigating=true;
            // back to the home tiles: the page shrinks into its own tile
            final int from = settingsGroup;
            if (Motion.enabled(this) && root.getWidth() > 0 && java.util.Arrays.stream(SHELVES).anyMatch(g -> g == from)) {
                shrinkInto(from, () -> { settingsGroup = 0; rebuild(); });
                return;
            }
            long out = leave(1f, null);
            column.postDelayed(()->{ if(isFinishing()||isDestroyed())return; settingsGroup=0;rebuild();arrive(-1f);navigating=false; }, out);
        }
        else finish();
    }

    @Override public void onBackPressed() { goBack(); }

    // ---------------------------------------------------- swipe right to go back

    private boolean swipeTracking, swiping;
    private float swipeX, swipeY, swipeDx;
    private android.view.VelocityTracker swipeSpeed;

    /**
     * A drag to the right that starts near the left edge takes the page with the finger and goes back
     * when it is carried far enough or flicked, like a chat in Telegram. Anything else is left alone,
     * so sliders and the chip row keep their own horizontal drags.
     */
    /** The island editor on the page, if there is one: its drags are its own. */
    private java.lang.ref.WeakReference<View> islandEditor = new java.lang.ref.WeakReference<>(null);

    private boolean onIslandEditor(android.view.MotionEvent ev) {
        View editor = islandEditor.get();
        if (editor == null || !editor.isShown()) return false;
        android.graphics.Rect area = new android.graphics.Rect();
        return editor.getGlobalVisibleRect(area) && area.contains((int) ev.getRawX(), (int) ev.getRawY());
    }

    @Override public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if(ev!=null && ev.getActionMasked()==android.view.MotionEvent.ACTION_DOWN)Unsaved.look(this);
        try {
            if (column != null && !navigating && !collapsing) {
                int width = column.getWidth(), edge = dp(56), slop = android.view.ViewConfiguration.get(this).getScaledTouchSlop();
                switch (ev.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        // not on the island's buttons: carrying "Главная" to the right is not a way back
                        swipeTracking = ev.getX() < edge && width > 0 && !onIslandEditor(ev); swiping = false; swipeDx = 0;
                        swipeX = ev.getX(); swipeY = ev.getY();
                        if (swipeSpeed != null) swipeSpeed.recycle();
                        swipeSpeed = swipeTracking ? android.view.VelocityTracker.obtain() : null;
                        break;
                    case android.view.MotionEvent.ACTION_MOVE:
                        if (!swipeTracking) break;
                        if (swipeSpeed != null) swipeSpeed.addMovement(ev);
                        float dx = ev.getX() - swipeX, dy = ev.getY() - swipeY;
                        if (!swiping) {
                            if (Math.abs(dy) > slop && Math.abs(dy) > Math.abs(dx)) { swipeTracking = false; break; }
                            if (dx > slop && dx > Math.abs(dy) * 1.2f) {
                                swiping = true;
                                android.view.MotionEvent cancel = android.view.MotionEvent.obtain(ev);
                                cancel.setAction(android.view.MotionEvent.ACTION_CANCEL);
                                super.dispatchTouchEvent(cancel); cancel.recycle();
                                column.animate().cancel();
                            }
                        }
                        if (swiping) {
                            swipeDx = Math.max(0f, dx - slop);
                            column.setTranslationX(swipeDx);
                            column.setAlpha(1f - .45f * Math.min(1f, swipeDx / width));
                            return true;
                        }
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        if (swiping) {
                            swiping = false; swipeTracking = false;
                            float speed = 0f;
                            if (swipeSpeed != null) { swipeSpeed.addMovement(ev); swipeSpeed.computeCurrentVelocity(1000); speed = swipeSpeed.getXVelocity(); }
                            boolean leave = ev.getActionMasked() == android.view.MotionEvent.ACTION_UP && (swipeDx > width * .33f || speed > dp(700));
                            if (leave) {
                                navigating = true;
                                column.animate().translationX(width).alpha(.4f).setInterpolator(Motion.OUT).setDuration(Motion.enabled(this) ? Motion.MEDIUM-40L : 0)
                                    .withEndAction(() -> {
                                        if (searchQuery.length() > 0) { searchQuery = ""; rebuild(); navigating = false; }
                                        else if (openSection != null) { openSection = null; rebuild(); arrive(-1f); navigating = false; }
                                        else if (settingsGroup != 0) { settingsGroup = 0; rebuild(); arrive(-1f); navigating = false; }
                                        else finish();
                                    }).start();
                            } else {
                                column.animate().translationX(0f).alpha(1f).setInterpolator(Motion.OUT).setDuration(Motion.enabled(this) ? Motion.MEDIUM : 0).start();
                            }
                            return true;
                        }
                        swipeTracking = false;
                        break;
                    default:
                        break;
                }
            }
        } catch (Throwable error) {
            swiping = false; swipeTracking = false;
        }
        clickSound(ev);
        return super.dispatchTouchEvent(ev);
    }

    private float soundX, soundY;
    private long soundAt;

    /**
     * Every tap on anything pressable on this screen clicks -- not only the rows that set up their own press: a tap
     * is a finger that went down and up near the same place, quickly, over a view that takes clicks.
     */
    private void clickSound(android.view.MotionEvent ev) {
        if (ev == null) return;
        int action = ev.getActionMasked();
        if (action == android.view.MotionEvent.ACTION_DOWN) { soundX = ev.getRawX(); soundY = ev.getRawY(); soundAt = ev.getEventTime(); return; }
        if (action != android.view.MotionEvent.ACTION_UP || root == null) return;
        int slop = android.view.ViewConfiguration.get(this).getScaledTouchSlop();
        if (Math.abs(ev.getRawX() - soundX) > slop || Math.abs(ev.getRawY() - soundY) > slop
                || ev.getEventTime() - soundAt > android.view.ViewConfiguration.getLongPressTimeout()) return;
        if (pressableAt(root, Math.round(ev.getRawX()), Math.round(ev.getRawY()))) UiSound.tap();
    }

    /** Whether a shown view under the screen point (x, y) takes clicks. */
    private static boolean pressableAt(View view, int x, int y) {
        if (view.getVisibility() != View.VISIBLE || view.getAlpha() <= 0f) return false;
        int[] at = new int[2];
        view.getLocationOnScreen(at);
        if (x < at[0] || y < at[1] || x >= at[0] + view.getWidth() * view.getScaleX() || y >= at[1] + view.getHeight() * view.getScaleY()) return false;
        if (view.isClickable() && view.isEnabled() && !(view instanceof ScrollView)) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = group.getChildCount() - 1; i >= 0; i--) if (pressableAt(group.getChildAt(i), x, y)) return true;
        }
        return false;
    }

    @Override protected void onDestroy() {
        // the emoji cards' typefaces were the page's alone
        Fonts.releasePreviews();
        if(root!=null)root.removeCallbacks(liveUiUpdates);
        if(adminPanel!=null){adminPanel.close();adminPanel=null;}
        Badges.stopTelling(badgeUpdates);
        if (column != null) { column.removeCallbacks(delayedRebuild); column.animate().withEndAction(null).cancel(); }
        if (page != null) {
            if (scrollRestore != null) page.removeCallbacks(scrollRestore);
            if (scrollRestoreListener != null && page.getViewTreeObserver().isAlive())
                page.getViewTreeObserver().removeOnPreDrawListener(scrollRestoreListener);
        }
        super.onDestroy();
    }

    /** A note that sits inside a group as a tile of its own: even padding all round, never clipped. */
    private View note(String message) {
        TextView view = new TextView(this);
        view.setText(message);
        view.setTextColor(skin.muted());
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        view.setLineSpacing(0, 1.12f);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setIncludeFontPadding(true);
        view.setPadding(dp(16), dp(14), dp(16), dp(14));
        view.setMinHeight(dp(48));
        return view;
    }

    private View caption(String message) {
        TextView view = new TextView(this);
        view.setText(message);
        view.setTextColor(skin.muted());
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        view.setPadding(dp(16), dp(12), dp(16), dp(4));
        return view;
    }

    /**
     * A group of rows: one flat list on the page, a hairline between rows
     * after their icons, the whole row lit when pressed.
     */
    private LinearLayout card() {
        // one plain list rather than Material's tiles: rows on the page, a hairline between them
        return new Expressive.Group(this, skin.text & 0xFFFFFF, (Accent.colour() & 0xFFFFFF) | 0x26000000, true);
    }

    /** A lone row is a one-tile list, so it looks like every other group. */
    private View wrap(View card) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 0, 0, dp(4));
        if (!(card instanceof Expressive.Group)) {
            LinearLayout single = card();
            single.addView(card);
            card = single;
        }
        box.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return box;
    }

    /** The accent as text on the page: lighter on dark, darker on light. */
    private int accentInk() {
        return skin.dark() ? Expressive.mix(Accent.colour(), 0xFFFFFFFF, 0.45f)
                : Expressive.mix(Accent.colour(), 0xFF000000, 0.35f);
    }

    /** The accent laid thin over the card: the tonal container of M3. */
    private int accentTonal() {
        return Expressive.mix(skin.card, Accent.colour(), 0.24f);
    }

    /** Two illustrated choices side by side, MD3 style: the chosen one gets an accent outline and a check. */
    private View glassChoice() {
        LinearLayout row = new LinearLayout(this);
        // real screenshots are a whole island wide: one tile per line, so the island reads at full width
        boolean real = new GlassPreview(this, Blur.FROSTED).real();
        row.setOrientation(real ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        row.setPadding(dp(16), dp(12), dp(16), dp(16));
        int chosen = Blur.style();
        int[] styles = {Blur.LIQUID, Blur.FROSTED, Blur.PLASTIC};
        for (int i = 0; i < styles.length; i++) {
            if (i > 0) row.addView(new View(this), real ? new LinearLayout.LayoutParams(1, dp(8)) : new LinearLayout.LayoutParams(dp(8), 1));
            row.addView(glassTile(styles[i], chosen == styles[i], real), real
                    ? new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    : new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        return row;
    }

    private View glassTile(final int style, boolean chosen, boolean real) {
        final boolean liquid = style == Blur.LIQUID;
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = real ? dp(10) : dp(6);
        tile.setPadding(pad, pad, pad, dp(10));
        GradientDrawable frame = new GradientDrawable();
        frame.setCornerRadius(dp(22));
        frame.setColor(chosen ? accentTonal() : 0x00000000);
        frame.setStroke(dp(chosen ? 2 : 1), chosen ? Accent.colour() : Expressive.mix(skin.card, skin.text, 0.18f));
        tile.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf((Accent.colour() & 0xFFFFFF) | 0x33000000), frame, null));
        GlassPreview picture = new GlassPreview(this, style);
        tile.addView(picture, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, real ? ViewGroup.LayoutParams.WRAP_CONTENT : dp(96)));
        LinearLayout caption = new LinearLayout(this);
        caption.setOrientation(LinearLayout.HORIZONTAL);
        caption.setGravity(Gravity.CENTER);
        caption.setPadding(0, dp(10), 0, 0);
        if (chosen) {
            ImageView check = new ImageView(this);
            check.setImageDrawable(Symbols.drawable("check_circle_fill1", Accent.colour()));
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(18), dp(18));
            size.rightMargin = dp(6);
            caption.addView(check, size);
        }
        TextView name = new TextView(this);
        name.setText(liquid ? Text.GLASS_LIQUID : style == Blur.PLASTIC ? Text.t("Пластик") : Text.GLASS_FROSTED);
        name.setTextColor(chosen ? accentInk() : skin.text);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        name.setMaxLines(2);
        name.setGravity(Gravity.CENTER);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        caption.addView(name);
        tile.addView(caption);
        boolean available = !liquid || Blur.shaders();
        final String why = !Ttcuz.is64() ? NOT_32 : "Android 13+";
        if (!available) {
            tile.setAlpha(0.5f);
            tile.setContentDescription(name.getText() + ", " + why);
            TextView note = new TextView(this);
            note.setText(why);
            note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            note.setTextColor(skin.muted());
            note.setGravity(Gravity.CENTER);
            tile.addView(note);
        }
        else tile.setContentDescription(name.getText());
        tile.setOnClickListener(v -> {
            if (!available) { Screen.say(why); return; }
            Blur.setStyle(style);
            rebuild();
        });
        Motion.press(tile);
        return tile;
    }

    /** The feed's scroll effects, each with a little picture of what it does, and the one in use ticked. */
    private void chooseScrollEffect() {
        CharSequence[] names = new CharSequence[FeedScroll.NAMES.length];
        for (int i = 0; i < names.length; i++) names[i] = Text.t(FeedScroll.NAMES[i]);
        new ModDialog.Builder(this).setTitle(Text.t("Эффект прокрутки"))
                .setSingleChoiceItems(names, FeedScroll.effect(), (d, i) -> { FeedScroll.set(i); d.dismiss(); rebuild(); }).show();
    }

    /** Islands: the mod's own, the examples, one's own (written here in Java, pasted or opened, compiled on the phone). */
    private void islandMenu() {
        String chosen = IslandFiles.chosen();
        java.util.List<CharSequence> items = new java.util.ArrayList<>();
        final java.util.List<Runnable> actions = new java.util.ArrayList<>();
        items.add((chosen.isEmpty() ? "✓ " : "") + Text.t("Островок мода")); actions.add(() -> { IslandFiles.choose(""); rebuild(); });
        for (final String[] one : IslandFiles.EXAMPLES) {
            final String key = IslandFiles.BUILTIN + one[0];
            items.add((key.equals(chosen) ? "✓ " : "") + Text.t(one[1]) + " · " + Text.t("пример"));
            actions.add(() -> islandActions(key, Text.t(one[1]), false));
        }
        for (final String[] one : IslandFiles.installed()) {
            items.add((one[0].equals(chosen) ? "✓ " : "") + one[1] + (one[2].isEmpty() ? "" : " · " + one[2]));
            actions.add(() -> islandActions(one[0], one[1], true));
        }
        items.add(Text.t("Написать новый")); actions.add(() -> writeIsland(null, IslandFiles.source(this, IslandFiles.BUILTIN + "Starter")));
        items.add(Text.t("Вставить из буфера")); actions.add(() -> {
            CharSequence clip = null;
            try {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0)
                    clip = clipboard.getPrimaryClip().getItemAt(0).coerceToText(this);
            } catch (Throwable ignored) {}
            if (clip == null || clip.length() == 0) { Screen.say(Text.t("В буфере ничего нет")); return; }
            takeIsland(null, clip.toString());
        });
        items.add(Text.t("Открыть файл .java")); actions.add(() -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
            try { startActivityForResult(pick, PICK_ISLAND); } catch (RuntimeException e) { Screen.say(Text.t("Не удалось открыть файлы")); }
        });
        new ModDialog.Builder(this).setTitle(Text.t("Свои островки"))
                .setItems(items.toArray(new CharSequence[0]), (d, i) -> actions.get(i).run()).show();
    }

    private void islandActions(final String key, String name, boolean own) {
        java.util.List<CharSequence> items = new java.util.ArrayList<>();
        final java.util.List<Runnable> actions = new java.util.ArrayList<>();
        items.add(Text.t("Включить")); actions.add(() -> { IslandFiles.choose(key); rebuild(); });
        items.add(own ? Text.t("Изменить") : Text.t("Сделать свой на его основе")); actions.add(() -> writeIsland(own ? key : null, IslandFiles.source(this, key)));
        items.add(Text.t("Поделиться")); actions.add(() -> {
            String source = IslandFiles.source(this, key);
            Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, source);
            try { startActivity(Intent.createChooser(send, name)); } catch (RuntimeException e) { safeCopy("island", source); }
        });
        if (own) { items.add(Text.t("Удалить")); actions.add(() -> { IslandFiles.delete(key); rebuild(); }); }
        new ModDialog.Builder(this).setTitle(name).setItems(items.toArray(new CharSequence[0]), (d, i) -> actions.get(i).run()).show();
    }

    private void writeIsland(final String key, String start) {
        Popup.write(this, Text.t("Островок на Java"), start, Text.t("Собрать и включить"), true, text -> takeIsland(key, text));
    }

    /**
     * Compiles a written island on the phone (a sheet shows each step with a moving bar) and turns it on; when the Java
     * is wrong, the compiler's words are shown and the editor opens again with the text.
     */
    private void takeIsland(final String key, final String source) {
        try { IslandFiles.describe(source); }
        catch (IllegalArgumentException wrong) { Screen.say(wrong.getMessage()); writeIsland(key, source); return; }
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        final TextView step = detail(Text.t("Подготовка…"));
        step.setTextColor(skin.text);
        body.addView(step);
        final DownloadBar bar = new DownloadBar(this, Accent.colour(), Expressive.mix(skin.card, skin.text, 0.12f));
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4));
        size.topMargin = dp(12);
        body.addView(bar, size);
        final android.app.Dialog sheet = new ModDialog.Builder(this).setTitle(Text.t("Сборка островка")).setView(body).show();
        final int[] done = {0};
        Net.away("island build", () -> {
            String built = null; Throwable failed = null;
            try {
                built = IslandFiles.compile(this, key, source, words -> runOnUiThread(() -> { step.setText(words); bar.set(Math.min(90, done[0] += 25)); }));
            } catch (Throwable error) { failed = error; }
            final String result = built; final Throwable error = failed;
            runOnUiThread(() -> {
                try { if (sheet != null) sheet.dismiss(); } catch (Throwable ignored) {}
                if (result != null) {
                    IslandFiles.choose(result);
                    Screen.say(Text.t("Островок собран и включён"));
                    rebuild();
                } else {
                    String why = error == null ? "" : error.getMessage() == null ? String.valueOf(error) : error.getMessage();
                    new ModDialog.Builder(this).setTitle(Text.t("Островок не собрался")).setMessage(why)
                            .setPositiveButton(Text.t("Исправить"), (d, w) -> writeIsland(key, source))
                            .setNegativeButton(Text.t("Скопировать"), (d, w) -> safeCopy("island", why)).show();
                }
            });
        });
    }

    /** Under the row: the island drawing now, or the mod's own. */
    private String islandDescription() {
        return IslandFiles.nameOf(IslandFiles.chosen());
    }

    private View line() {
        View spacer=new View(this);spacer.setVisibility(View.GONE);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0,0));return spacer;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(8), dp(16), dp(8));
        row.setMinimumHeight(dp(56));
        row.setClipChildren(false);row.setClipToPadding(false);
        Motion.press(row);
        return row;
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(skin.text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        view.setIncludeFontPadding(false);view.setPadding(0,0,dp(12),0);
        view.setMaxLines(2);view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return view;
    }

    private TextView detail(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(skin.muted());
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        view.setIncludeFontPadding(false);
        view.setPadding(0, dp(3), dp(12), 0);
        // a second line, not a paragraph: two lines at most, the rest is in the setting itself
        view.setMaxLines(2);
        view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        view.setLineSpacing(0, 1.1f);
        return view;
    }

    private LinearLayout.LayoutParams grow() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private View sized(View view, int height) {
        // MD3 list items: 56 dp for one line, 72 dp for two.
        if (height > 1) view.setMinimumHeight(dp(Math.max(56, height)));
        view.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height <= 1 ? dp(height) : ViewGroup.LayoutParams.WRAP_CONTENT));
        return view;
    }

    /**
     * The bar itself: one line of why, and the button that does it.
     *
     * Painted in the card colour with a hairline above, so it reads as resting
     * on the page rather than floating over it, and padded underneath by
     * whatever the navigation bar takes -- otherwise the button sits under the
     * gesture pill.
     */
    private View restartBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        styleRestartBar(bar);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(20), dp(10), dp(12), dp(10));

        RestartSymbol symbol = new RestartSymbol(this, Accent.colour());
        LinearLayout.LayoutParams symbolSize = new LinearLayout.LayoutParams(dp(24),dp(24));
        symbolSize.rightMargin = dp(12);
        content.addView(symbol,symbolSize);

        TextView why = new TextView(this);
        why.setText(Text.RESTART_PENDING);
        why.setTextColor(skin.text);
        why.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        content.addView(why, grow());

        TextView button = new TextView(this);
        button.setText(Text.RESTART);
        button.setTextColor(onAccent());
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setIncludeFontPadding(false);button.setGravity(Gravity.CENTER);
        button.setPadding(dp(16),0,dp(16),0);button.setMinWidth(dp(132));
        button.setMinHeight(dp(48));
        button.setFocusable(true);
        button.setClickable(true);
        button.setBackground(restartPill());
        button.setOnClickListener(v -> restartTikTok());
        LinearLayout.LayoutParams actionSize=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,dp(48));actionSize.leftMargin=dp(12);content.addView(button,actionSize);
        why.setIncludeFontPadding(false);why.setMaxLines(2);why.setGravity(Gravity.CENTER_VERTICAL);
        restartSymbol=symbol;restartWhy=why;restartAction=button;
        Fonts.applyTree(content);
        bar.addView(content);
        return bar;
    }

    /** The restart button: a pill in the accent as it is now, painted fresh whenever it is shown. */
    private android.graphics.drawable.Drawable restartPill() {
        return Expressive.tile(this, Accent.colour(), 0x33FFFFFF, dp(24), dp(24), dp(24));
    }

    /**
     * What to write on the accent: the palette holds a mint and a near-white
     * as well as the pink, and white letters on either of those are unreadable.
     */
    private int onAccent() {
        int colour = Accent.colour();
        int red = (colour >> 16) & 0xFF, green = (colour >> 8) & 0xFF, blue = colour & 0xFF;
        int brightness = (red * 299 + green * 587 + blue * 114) / 1000;
        return brightness > 150 ? 0xFF1C2C24 : 0xFFFFFFFF;
    }

    /** Solid page colour behind the system bars, with a hard edge: no fade eats into the list. */
    private static final class Scrim extends View {
        private final boolean top;
        private final Paint paint = new Paint();
        private int colour, solid, fade;

        Scrim(Context context, boolean top) {
            super(context);
            this.top = top;
            setClickable(false);
            setFocusable(false);
        }

        void set(int colour, int solid, int fade) {
            this.colour = colour; this.solid = solid; this.fade = fade;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            if (w == 0 || h == 0) return;
            int clear = colour & 0x00FFFFFF;
            // top: solid first, then fade downwards; bottom: the mirror image
            float edge = top ? solid : h - solid;
            paint.setShader(null);
            paint.setColor(colour);
            if (top) canvas.drawRect(0, 0, w, edge, paint);
            else canvas.drawRect(0, edge, w, h, paint);
            if (fade <= 0) return;
            paint.setShader(top
                    ? new LinearGradient(0, edge, 0, edge + fade, colour, clear, Shader.TileMode.CLAMP)
                    : new LinearGradient(0, edge, 0, edge - fade, colour, clear, Shader.TileMode.CLAMP));
            if (top) canvas.drawRect(0, edge, w, h, paint);
            else canvas.drawRect(0, 0, w, edge, paint);
            paint.setShader(null);
        }
    }

    private void fitScrims() {
        if (topScrim == null || bottomScrim == null) return;
        int fade = 0;
        int top = statusBar(), bottom = navigationBar();
        FrameLayout.LayoutParams a = (FrameLayout.LayoutParams) topScrim.getLayoutParams();
        FrameLayout.LayoutParams b = (FrameLayout.LayoutParams) bottomScrim.getLayoutParams();
        if (a.height != top + fade || b.height != bottom + fade) {
            a.height = top + fade; b.height = bottom + fade;
            topScrim.setLayoutParams(a); bottomScrim.setLayoutParams(b);
        }
        topScrim.set(skin.page, top, fade);
        bottomScrim.set(skin.page, bottom, fade);
    }

    private int navigationBar() {
        try {
            android.view.WindowInsets insets = getWindow().getDecorView().getRootWindowInsets();
            if (insets != null) return insets.getSystemWindowInsetBottom();
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private int statusBar() {
        try {
            android.view.WindowInsets insets = getWindow().getDecorView().getRootWindowInsets();
            if (insets != null && insets.getSystemWindowInsetTop() > 0) {
                return insets.getSystemWindowInsetTop();
            }
        } catch (Throwable ignored) {
        }
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : dp(24);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // ----------------------------------------------------- the small shapes

    private interface ColourChanged { void changed(int colour); }

    /** A full hue and saturation/value picker, with no colour-code entry. */
    private static final class AccentColourPicker extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] hsv = new float[3];
        private final int[] rainbow = {Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN,
                Color.BLUE, 0xFFFF00FF, Color.RED};
        private ColourChanged changed;

        AccentColourPicker(Context context, int initial) {
            super(context);
            Color.colorToHSV(initial, hsv);
            setContentDescription(Text.ACCENT_PICKER);
        }

        void onColourChanged(ColourChanged listener) { changed = listener; }

        int colour() { return Color.HSVToColor(hsv); }
        void setColour(int colour){Color.colorToHSV(colour,hsv);invalidate();if(changed!=null)changed.changed(colour);}

        private float dp(float value) {
            return value * getResources().getDisplayMetrics().density;
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float pad = dp(10);
            float left = pad, right = getWidth() - pad, top = pad;
            float bottom = getHeight() - dp(42);
            int pureHue = Color.HSVToColor(new float[] {hsv[0], 1f, 1f});
            paint.setShader(new LinearGradient(left, top, right, top,
                    Color.WHITE, pureHue, Shader.TileMode.CLAMP));
            canvas.drawRect(left, top, right, bottom, paint);
            paint.setShader(new LinearGradient(left, top, left, bottom,
                    0x00000000, 0xFF000000, Shader.TileMode.CLAMP));
            canvas.drawRect(left, top, right, bottom, paint);
            paint.setShader(null);

            float markX = left + hsv[1] * (right - left);
            float markY = top + (1f - hsv[2]) * (bottom - top);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.BLACK);
            canvas.drawCircle(markX, markY, dp(8), paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(markX, markY, dp(6), paint);

            float hueTop = bottom + dp(12), hueBottom = getHeight() - dp(8);
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(left, hueTop, right, hueTop, rainbow,
                    null, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(left, hueTop, right, hueBottom, dp(5), dp(5), paint);
            paint.setShader(null);
            float hueX = left + hsv[0] / 360f * (right - left);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(3));
            paint.setColor(Color.WHITE);
            canvas.drawCircle(hueX, (hueTop + hueBottom) / 2f, dp(8), paint);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override public boolean onTouchEvent(android.view.MotionEvent event) {
            if (event.getAction() != android.view.MotionEvent.ACTION_DOWN
                    && event.getAction() != android.view.MotionEvent.ACTION_MOVE
                    && event.getAction() != android.view.MotionEvent.ACTION_UP) return true;
            float pad = dp(10), left = pad, right = Math.max(left + 1, getWidth() - pad);
            float top = pad, bottom = getHeight() - dp(42);
            if (event.getY() <= bottom + dp(5)) {
                hsv[1] = Math.max(0f, Math.min(1f, (event.getX() - left) / (right - left)));
                hsv[2] = 1f - Math.max(0f, Math.min(1f,
                        (event.getY() - top) / Math.max(1f, bottom - top)));
            } else {
                hsv[0] = Math.max(0f, Math.min(1f,
                        (event.getX() - left) / (right - left))) * 360f;
            }
            invalidate();
            if (changed != null) changed.changed(colour());
            return true;
        }
    }

    /** A tick, drawn rather than typed: the glyph fonts have is never the one. */
    private static final class Check extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Check(Context context, int colour) {
            super(context);
            paint.setColor(colour);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            int size = Math.round(22 * getResources().getDisplayMetrics().density);
            setMeasuredDimension(resolveSize(size, widthSpec), resolveSize(size, heightSpec));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float unit = getWidth() / 22f;
            paint.setStrokeWidth(unit * 2.2f);
            float y = getHeight() / 2f;
            canvas.drawLine(unit * 4, y + unit, unit * 9, y + unit * 5.5f, paint);
            canvas.drawLine(unit * 9, y + unit * 5.5f, unit * 18, y - unit * 5f, paint);
        }
    }

    /** Circle at rest; a rounded square and outline indicate the selected colour. */
    private static final class Dot extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.RectF box = new android.graphics.RectF();
        private final android.graphics.Path outline = new android.graphics.Path();
        private boolean chosen;
        private float selection;
        private android.animation.ValueAnimator animation;

        Dot(Context context, int colour, boolean chosen) {
            super(context);
            this.chosen=chosen; selection=chosen?1f:0f;
            fill.setColor(colour); ring.setColor(colour); ring.setStyle(Paint.Style.STROKE);
            setSelected(chosen); setFocusable(true);
        }
        private final Paint customInk=new Paint(Paint.ANTI_ALIAS_FLAG);
        private float customProgress;
        private android.animation.ValueAnimator colourAnimation;
        void reveal(int colour){
            choose(true,true);ring.setColor(colour);
            if(!Motion.enabled(getContext())){fill.setShader(null);fill.setColor(colour);invalidate();return;}
            if(colourAnimation!=null)colourAnimation.cancel();customInk.setColor(colour);customProgress=0;
            colourAnimation=android.animation.ValueAnimator.ofFloat(0,1);colourAnimation.setDuration(Motion.MEDIUM);colourAnimation.setInterpolator(Motion.OUT);
            colourAnimation.addUpdateListener(a->{customProgress=(Float)a.getAnimatedValue();invalidate();});
            colourAnimation.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){fill.setShader(null);fill.setColor(colour);customProgress=0;colourAnimation=null;invalidate();}});
            colourAnimation.start();
        }
        void choose(boolean selected, boolean animate) {
            chosen=selected; setSelected(selected);
            if (animation!=null) {animation.cancel();animation=null;}
            if (!animate || !Motion.enabled(getContext())) {selection=chosen?1f:0f;invalidate();return;}
            animation=android.animation.ValueAnimator.ofFloat(selection,chosen?1f:0f);
            animation.setDuration(Motion.MEDIUM-60L);animation.setInterpolator(Motion.OUT);
            animation.addUpdateListener(a -> {selection=(Float)a.getAnimatedValue();invalidate();});
            animation.start();
        }
        @Override protected void onMeasure(int width, int height) {
            int size=Math.round(26*getResources().getDisplayMetrics().density);
            setMeasuredDimension(resolveSize(size,width),resolveSize(size,height));
        }
        @Override protected void onDraw(Canvas canvas) {
            float unit=getResources().getDisplayMetrics().density;
            float x=getWidth()/2f,y=getHeight()/2f;
            float radius=Math.max(1f,Math.min(x,y)-6*unit);
            // chosen: the circle grows a little, scallops into a cookie and turns one lobe
            Expressive.shape(outline,Expressive.COOKIE9,x,y,radius+3*unit*selection,selection,selection*360f/Expressive.lobes(Expressive.COOKIE9));
            canvas.drawPath(outline,fill);
            if(customProgress>0){customInk.setAlpha(Math.round(255*customProgress));canvas.drawPath(outline,customInk);}
            if (selection>0f) {
                int shade=fill.getColor();
                boolean light=(Color.red(shade)*299+Color.green(shade)*587+Color.blue(shade)*114)/1000>150;
                ring.setStyle(Paint.Style.STROKE);ring.setStrokeCap(Paint.Cap.ROUND);ring.setStrokeWidth(2.4f*unit);
                ring.setColor(light ? 0xFF111111 : 0xFFFFFFFF);ring.setAlpha(Math.round(255*Math.min(1f,selection)));
                canvas.drawLine(x-5*unit,y+0.5f*unit,x-1.5f*unit,y+4*unit,ring);
                canvas.drawLine(x-1.5f*unit,y+4*unit,x+5.5f*unit,y-3.5f*unit,ring);
            }
        }
        @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            if (hasOnClickListeners()) {info.setCheckable(true);info.setChecked(chosen);info.setClassName("android.widget.RadioButton");}
        }
        @Override protected void onDetachedFromWindow() {
            if (animation!=null) {animation.cancel();animation=null;}
            if(colourAnimation!=null){colourAnimation.cancel();colourAnimation=null;}
            selection=chosen?1f:0f;super.onDetachedFromWindow();
        }
    }

    private String loaderName(){return new String[]{Text.t("Классические точки"),Text.t("Фигуры MD3"),Text.t("Кольцо")}[LoadingIndicator.style()];}
    /** What the chosen look comes to: the loading indicator running, and a button, a slider and a switch in the accent. */
    private View stylePreview() {
        LinearLayout box = new LinearLayout(this);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(16));
        View loader = new View(this) { @Override protected void onDraw(Canvas c) { LoadingIndicator.draw(this, c, Math.min(getWidth(), getHeight()), true); } };
        box.addView(loader, new LinearLayout.LayoutParams(dp(52), dp(52)));
        final int accentColour = Accent.colour(), tonal = accentTonal(), on = onAccent();
        final String button = Text.t("Кнопка");
        View samples = new View(this) {
            final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas c) {
                float w = getWidth(), u = dp(1);
                // a filled button
                paint.setColor(accentColour);
                c.drawRoundRect(0, 0, u * 96, u * 36, u * 18, u * 18, paint);
                paint.setColor(on); paint.setTextSize(u * 14); paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                c.drawText(button, u * 48, u * 23, paint);
                // a switch, on
                float sx = w - u * 52;
                paint.setColor(accentColour); c.drawRoundRect(sx, u * 4, w, u * 32, u * 14, u * 14, paint);
                paint.setColor(on); c.drawCircle(w - u * 14, u * 18, u * 10, paint);
                // a slider two thirds along
                float y = u * 56, at = w * .66f;
                paint.setColor(tonal); c.drawRoundRect(at, y - u * 3, w, y + u * 3, u * 3, u * 3, paint);
                paint.setColor(accentColour); c.drawRoundRect(0, y - u * 3, at, y + u * 3, u * 3, u * 3, paint);
                c.drawRoundRect(at - u * 2, y - u * 12, at + u * 2, y + u * 12, u * 2, u * 2, paint);
            }
        };
        LinearLayout.LayoutParams rest = new LinearLayout.LayoutParams(0, dp(70), 1f);
        rest.leftMargin = dp(20);
        box.addView(samples, rest);
        box.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        return box;
    }

    /** Each indicator running live in the accent, side by side: the chosen one is outlined; a tap picks it. */
    private void chooseLoader(){
        String[] names={Text.t("Точки"),Text.t("Фигуры MD3"),Text.t("Кольцо")};
        LinearLayout row=new LinearLayout(this);row.setPadding(dp(4),dp(8),dp(4),dp(4));
        final LinearLayout[] cards=new LinearLayout[names.length];
        for(int i=0;i<names.length;i++){
            final int kind=i;
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.CENTER);
            card.setPadding(dp(6),dp(14),dp(6),dp(12));
            View live=new View(this){@Override protected void onDraw(Canvas c){LoadingIndicator.draw(this,c,Math.min(getWidth(),getHeight()),true,kind);}};
            card.addView(live,new LinearLayout.LayoutParams(dp(48),dp(48)));
            TextView label=label(names[i]);label.setTextSize(13);label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams under=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);under.topMargin=dp(10);
            card.addView(label,under);
            card.setOnClickListener(v->{LoadingIndicator.choose(kind);for(int n=0;n<cards.length;n++)loaderCard(cards[n],n==kind);});
            LinearLayout.LayoutParams share=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);share.setMargins(dp(4),0,dp(4),0);
            row.addView(card,share);cards[i]=card;loaderCard(card,i==LoadingIndicator.style());
        }
        new ModDialog.Builder(this).setTitle(Text.t("Индикатор загрузки")).setView(row)
            .setPositiveButton(Text.t("Готово"),(dialog,which)->rebuild()).show();}
    private void loaderCard(View card,boolean chosen){
        GradientDrawable shape=new GradientDrawable();shape.setCornerRadius(dp(20));
        shape.setColor(chosen?accentTonal():skin.card);shape.setStroke(dp(2),chosen?Accent.colour():0);card.setBackground(shape);}
    private void chooseRefreshRate(){String[] labels={Text.t("Авто"),Text.t("60 Гц"),Text.t("90 Гц"),Text.t("120 Гц")};
        new ModDialog.Builder(this).setTitle(Text.t("Частота экрана")).setSingleChoiceItems(labels,java.util.Arrays.asList(Rate.CHOICES).indexOf(Rate.name()),(dialog,which)->{
            Rate.choose(Rate.CHOICES[which]);Rate.apply(this);dialog.dismiss();
            if(!Rate.reachable(this))Screen.say(Text.t("Экран не поддерживает выбранную частоту"));rebuild();}).show();}
    private void chooseAvatarShape(){
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
        android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.addView(list);
        final Panel picker=Panel.with(this,skin,Text.t("Форма аватарок"));
        for(int i=0;i<AvatarShape.ALL.length;i+=3){LinearLayout row=new LinearLayout(this);
            for(int k=i;k<Math.min(i+3,AvatarShape.ALL.length);k++){
                final String shape=AvatarShape.ALL[k];LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);
                item.setPadding(dp(4),dp(12),dp(4),dp(12));ImageView image=new ImageView(this);
                image.setImageBitmap(AvatarShape.picture(shape,dp(48),shape.equals(AvatarShape.chosen())?Accent.colour():skin.text));
                item.addView(image,new LinearLayout.LayoutParams(dp(48),dp(48)));TextView title=detail(AvatarShape.nameOf(shape));title.setGravity(Gravity.CENTER);title.setMaxLines(2);
                item.addView(title,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(44)));
                item.setContentDescription(AvatarShape.nameOf(shape));item.setOnClickListener(v->{AvatarShape.choose(this,shape);picker.close();markChanged();rebuild();});
                row.addView(item,new LinearLayout.LayoutParams(0,dp(116),1));
            }list.addView(row);
        }
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(360)));
        picker.view(scroll).quiet(Text.t("Готово"),null).show();
    }
    private View gradientPalette(){
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        final int[] colours=ProfileStyle.gradientColours();
        for(int start=0;start<colours.length;start+=4){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
            for(int i=start;i<Math.min(start+4,colours.length);i++){
                final int slot=i;LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);item.setPadding(dp(4),dp(8),dp(4),dp(8));
                Dot dot=new Dot(this,colours[i],false);item.addView(dot,new LinearLayout.LayoutParams(dp(40),dp(40)));
                TextView title=detail(Text.t("Цвет ")+(i+1));title.setGravity(Gravity.CENTER);item.addView(title);
                item.setOnClickListener(v->showProfileColourPicker(Text.t("Цвет ")+(slot+1),colours[slot],colour->{
                    int[] next=ProfileStyle.gradientColours();if(slot<next.length){next[slot]=colour;ProfileStyle.setGradientColours(next);rebuild();}}));
                row.addView(item,new LinearLayout.LayoutParams(0,dp(82),1));
            }body.addView(row);
        }
        if(colours.length<ProfileStyle.MAX_COLOURS)body.addView(actionRow("add",Text.t("Добавить цвет"),null,()->{
            int[] next=new int[colours.length+1];System.arraycopy(colours,0,next,0,colours.length-1);
            next[colours.length-1]=blend(colours[colours.length-2],colours[colours.length-1],0.5f);next[colours.length]=colours[colours.length-1];
            ProfileStyle.setGradientColours(next);rebuild();}));
        if(colours.length>2)body.addView(actionRow("remove",Text.t("Убрать предпоследний цвет"),null,()->{
            int[] next=new int[colours.length-1];System.arraycopy(colours,0,next,0,next.length-1);next[next.length-1]=colours[colours.length-1];
            ProfileStyle.setGradientColours(next);rebuild();}));
        return body;
    }
}
