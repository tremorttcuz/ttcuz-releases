package cat.narezany.margyt;

import android.app.Activity;
import android.animation.ValueAnimator;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

/**
 * MargyT's own screen, drawn in TikTok's settings language.
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
    private final java.util.Map<String,View> settingTargets=new java.util.HashMap<>();
    private LinearLayout column;
    private View restartBar;
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
    private static final String CHANNEL = "https://t.me/margytiktok";
    private static final String FORUM = "https://t.me/margeletforum";
    private static final String OWNER_TELEGRAM = "https://t.me/narezany";
    private static final String OWNER_TIKTOK =
            "https://tiktok.com/@narezany?_r=1&_t=ZT-99hPDJ26hji_";
    private static final String TTCUZ_SITE = "https://ttcuz.daniilsolovatulin.workers.dev/";
    private static final String DOCS_PLUGINS = TTCUZ_SITE + "guide.html#plugins";

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
        Margy.attach(this);
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
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);
        rebuild();
        if(getIntent().getBooleanExtra("ttcuz_tutorial",false))root.postDelayed(()->Tutorial.show(this),350L);
    }

    void tutorialStep(int step) {
        settingsGroup=step==1 ? 1 : step==2 ? 2 : 0;
        if(step==1)accentOpen=true;
        searchQuery="";rebuild();
        String section=step==1 ? Text.ACCENT : step==2 ? Text.PROFILE_STYLE : null;
        View target=section==null ? null : sectionTargets.get(section);
        if(target!=null)page.post(()->page.smoothScrollTo(0,Math.max(0,target.getTop()-dp(20))));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(Streaks.isEnabled()){StreakAlarm.schedule(this,false);Streaks.round(this);}
        if(root!=null && column!=null)rebuild();
        if(root!=null){root.removeCallbacks(liveUiUpdates);root.post(liveUiUpdates);}
        if (verifyWaiting) {
            verifyWaiting = false;
            final String uid = Account.id();
            if (uid != null && !new CloudProfileProvider(this).verified(uid)) {
                Toast.makeText(this, "Проверяю профиль…", Toast.LENGTH_SHORT).show();
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
        dressTheWindow();
        navigating=false;lastAdmin=AdminAccess.current();syncTitle=syncDetails=syncRetry=null;syncSpinner=null;
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
        if(settingsGroup==0 || searching)column.addView(searchBox());
        if (settingsGroup != 0 && !searching) column.addView(wrap(actionRow("info","Подсказки",null,this::showSectionHelp)));

        if (!searching && settingsGroup == 0) {
            column.addView(groupNavigation());
            column.addView(wrap(actionRow("info","Обучение",null,() -> Tutorial.show(this))));
            column.addView(versions());
            showRestartBar();
            restoreScroll(0);
            return;
        }

        filterFrom = column.getChildCount();
        column.addView(section(Text.APPEARANCE));
        LinearLayout icons = card();
        icons.addView(iconHead());
        icons.addView(line());
        icons.addView(iconChoices());
        icons.addView(caption(Text.ICON_NOTE));
        column.addView(wrap(icons));

        column.addView(section(Text.ACCENT));
        LinearLayout accent = card();
        accent.addView(accentHead());
        if (accentOpen) {
            accent.addView(line());
            accent.addView(appear(palette()));

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

        column.addView(section(Text.FONT));
        LinearLayout fonts = card();
        fonts.addView(fontHead());
        if (fontOpen) {
            fonts.addView(line());
            fonts.addView(appear(fontChoices()));
        }
        column.addView(wrap(fonts));

        // Profile appearance, in four short cards instead of one long list:
        // who you are (name, badge), how the nickname looks, sync with other ttcuz users, reset.
        column.addView(section(Text.PROFILE_STYLE));
        // The preview rides along while the settings scroll under it, so every change is seen at once.
        FrameLayout previewHolder = new FrameLayout(this);
        previewHolder.setClipChildren(false);
        previewHolder.setBackgroundColor(skin.page);
        previewHolder.setPadding(dp(12), dp(4), dp(12), dp(8));
        previewHolder.addView(realProfilePreview());
        column.addView(previewHolder, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pinHolder = previewHolder;
        LinearLayout profile = card();
        profile.addView(actionRow("edit","Имя в моде","Для этого аккаунта",()->{
            android.widget.EditText name=new android.widget.EditText(this);
            name.setText(ProfileStyle.displayName(Account.liveId(),Account.profileName()==null?"":Account.profileName()));
            name.setSingleLine(true);name.setTextColor(skin.text);
            Panel.with(this,skin,"Имя в моде").view(name).primary("Сохранить",()->{
                String value=name.getText().toString().trim();if(value.length()<=80){ProfileStyle.setDisplayName(value);rebuild();}
            }).quiet("Назад",null).show();
        }));
        if (AdminAccess.current()) {
            CloudProfileProvider badgeSync = new CloudProfileProvider(this);
            profile.addView(actionRow("workspace_premium", "Выдать значок",
                    badgeSync.verified(Account.id())
                            ? "Ник, ссылка или UID"
                            : "Сначала подтвердите аккаунт создателя",
                    this::changeCommunityBadge));
        }
        profile.addView(actionRow("image", Text.PROFILE_BADGE,
                ProfileStyle.hasBadge() ? Text.SAVED : null, this::pickProfileBadge));
        if (ProfileStyle.hasBadge() || (Badges.of(Account.id())!=null && Badges.of(Account.id()).length>0)) {
            LinearLayout placement=row();
            placement.addView(icon("open_with"));
            placement.addView(label("Положение · на всех экранах"),grow());
            profile.addView(sized(placement,56));
            if (true) {
            LinearLayout positions=new LinearLayout(this);positions.setOrientation(LinearLayout.VERTICAL);
            positions.addView(quiet("Перетащите значок в превью. Положение сохраняется относительно имени."));
            positions.addView(liveSlider("swap_horiz","По горизонтали", ProfileStyle.offsetX() + 80, 160, -80, "",
                    value -> { ProfileStyle.setBadgePlacement(value - 80,
                            ProfileStyle.offsetY(), ProfileStyle.badgeSize());
                        markChanged(); }));
            positions.addView(liveSlider("swap_vert","По вертикали", ProfileStyle.offsetY() + 50, 100, -50, "",
                    value -> { ProfileStyle.setBadgePlacement(ProfileStyle.offsetX(),
                            value - 50, ProfileStyle.badgeSize());
                        markChanged(); }));
            positions.addView(liveSlider("image","Размер значка",
                    ProfileStyle.badgeSize() - 50, 150, 50, "%",
                    value -> { ProfileStyle.setBadgePlacement(ProfileStyle.offsetX(),
                            ProfileStyle.offsetY(), value + 50);
                        markChanged(); }));
            positions.addView(actionRow("restore","Сбросить положение",null,()->{ProfileStyle.setBadgePlacement(0,0,100);rebuild();}));
            profile.addView(appear(positions));
            }
            if(ProfileStyle.hasBadge())profile.addView(actionRow("restore", Text.PROFILE_BADGE_REMOVE, null, () -> {
                ProfileStyle.removeBadge();
                markChanged();
                rebuild();
            }));
        }
        column.addView(wrap(profile));

        column.addView(section(Text.PROFILE_LOOK));
        LinearLayout look = card();
        look.addView(toggleRow("palette", Text.PROFILE_GRADIENT,
                ProfileStyle.gradient(), on -> {
                    ProfileStyle.setGradient(on);
                    markChanged();
                    rebuildAfterSwitchAnimation();
                }));
        if (ProfileStyle.gradient()) {
            look.addView(actionRow("palette", Text.PROFILE_START,
                    String.format(Locale.US, "#%06X", ProfileStyle.start() & 0xFFFFFF),
                    () -> chooseProfileColour(true)));
            look.addView(actionRow("palette", Text.PROFILE_END,
                    String.format(Locale.US, "#%06X", ProfileStyle.end() & 0xFFFFFF),
                    () -> chooseProfileColour(false)));
            look.addView(toggleRow("palette", Text.PROFILE_THREE,
                    ProfileStyle.threeColours(), on -> {
                        ProfileStyle.setThreeColours(on);
                        markChanged();
                        rebuildAfterSwitchAnimation();
                    }));
            if (ProfileStyle.threeColours()) {
                look.addView(actionRow("palette", Text.PROFILE_MIDDLE,
                        String.format(Locale.US, "#%06X", ProfileStyle.middle() & 0xFFFFFF),
                        this::chooseProfileMiddle));
            }
            // direction, width, motion: rarely touched, so folded away
            LinearLayout fine=row();
            fine.addView(icon("tune"));
            fine.addView(label(Text.PROFILE_FINE),grow());
            look.addView(sized(fine,56));
            if (true) {
                LinearLayout more=new LinearLayout(this);more.setOrientation(LinearLayout.VERTICAL);
                more.addView(actionRow("swap_horiz", Text.PROFILE_DIRECTION,
                        profileDirection(ProfileStyle.direction()), this::chooseProfileDirection));
                more.addView(liveSlider("palette","Ширина перехода · 50–200%", ProfileStyle.gradientWidth()-50,150,50,"%",value -> {
                    ProfileStyle.setGradientWidth(value+50);markChanged();
                }));
                if(ProfileStyle.threeColours())more.addView(liveSlider("palette","Положение среднего цвета",ProfileStyle.gradientStop()-5,90,5,"%",value -> {
                    ProfileStyle.setGradientStop(value+5);markChanged();
                }));
                more.addView(toggleRow("animation", Text.PROFILE_ANIMATED,
                        ProfileStyle.animated(), on -> {
                            ProfileStyle.setAnimated(on);
                            markChanged();
                            rebuildAfterSwitchAnimation();
                        }));
                if (ProfileStyle.animated()) {
                    more.addView(liveSlider("speed",Text.PROFILE_SPEED, ProfileStyle.speed(), 100, 0, "%", value -> {
                        ProfileStyle.setSpeed(value);
                        markChanged();
                    }));
                }
                look.addView(appear(more));
            }
        }
        look.addView(toggleRow("text_fields", Text.PROFILE_BOLD, ProfileStyle.bold(), on -> {
            ProfileStyle.setBold(on);
            markChanged();
            rebuildAfterSwitchAnimation();
        }));
        look.addView(toggleRow("flare", Text.PROFILE_GLOW, ProfileStyle.glow(), on -> {
            ProfileStyle.setGlow(on);
            markChanged();
            rebuildAfterSwitchAnimation();
        }));
        View lookBox = wrap(look);
        pinEnd = lookBox;
        column.addView(lookBox);
        if (page != null) page.post(this::pinPreview);

        column.addView(section(Text.PROFILE_SYNC));
        LinearLayout sync = card();
        if (TtcuzProfileSync.available()) {
            if (new CloudProfileProvider(this).verified(Account.id())) {
                sync.addView(actionRow("verified", "Профиль подтверждён",
                        "Синхронизация включена",
                        () -> {
                            TtcuzProfileSync.retryOwn();updateSyncUi();
                        }));
            } else {
                CloudProfileProvider cloud = new CloudProfileProvider(this);
                boolean taken = cloud.needsProof(Account.id());
                sync.addView(actionRow("verified",
                        taken ? "Профиль занят другим устройством" : "Подтвердить для своего значка",
                        taken ? "Один раз добавьте код в описание TikTok, чтобы вернуть профиль"
                              : "Нужно только для публикации значка",
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

        LinearLayout reset = card();
        reset.addView(actionRow("restore", Text.PROFILE_RESET, null, () -> {
            ProfileStyle.reset();markChanged();rebuild();
        }));
        column.addView(wrap(reset));

        column.addView(section(Text.ACCOUNT));
        LinearLayout account = card();
        account.addView(idRow(Text.ACCOUNT_ID, Account.id()));
        account.addView(line());
        account.addView(idRow(Text.ACCOUNT_SEC_ID, Account.secId()));
        column.addView(wrap(account));

        column.addView(section(Text.REGION));
        LinearLayout head = card();
        head.addView(switchRow());
        column.addView(wrap(head));

        column.addView(section(Text.COUNTRY));
        LinearLayout countries = card();
        countries.addView(countryHead());
        if (countriesOpen) {
            countries.addView(line());
            for (String[] country : Margy.COUNTRIES) {
                countries.addView(appear(countryRow(country)));
            }
        }
        column.addView(wrap(countries));

        column.addView(section(Text.FEED));
        LinearLayout feed = card();
        feed.addView(toggleRow("block", Text.HIDE_ADS, Feed.isEnabled(), Feed::setEnabled));
        feed.addView(line());
        feed.addView(toggleRow("visibility_off", Text.HIDE_LIVE, Feed.hides(Feed.KEY_LIVE),
                on -> Feed.setHides(Feed.KEY_LIVE, on)));
        feed.addView(line());
        feed.addView(toggleRow("image", Text.HIDE_PHOTOS, Feed.hides(Feed.KEY_PHOTOS),
                on -> Feed.setHides(Feed.KEY_PHOTOS, on)));
        feed.addView(line());
        feed.addView(liveToggleRow("visibility_off",Text.HIDE_STORIES,Feed.hides(Feed.KEY_STORIES),on->Feed.setHides(Feed.KEY_STORIES,on)));
        feed.addView(liveToggleRow("visibility_off",Text.HIDE_SUGGESTIONS,Feed.hides(Feed.KEY_SUGGESTIONS),on->Feed.setHides(Feed.KEY_SUGGESTIONS,on)));
        column.addView(wrap(feed));

        column.addView(section(Text.RAIL));
        LinearLayout rail=card();
        String[] railNames={Text.RAIL_LIKE,Text.RAIL_COMMENT,Text.RAIL_FAV,Text.RAIL_SHARE};
        String[] railIcons={"favorite_border","chat","star","share"};
        for(int i=0;i<railNames.length;i++){
            final int kind=i;
            rail.addView(liveToggleRow(railIcons[i],railNames[i],FeedRail.shown(i),on->FeedRail.setShown(kind,on)));
        }
        column.addView(wrap(rail));

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
        column.addView(wrap(video));

        column.addView(section(Text.COMMENTS));
        LinearLayout comments = card();
        comments.addView(liveToggleRow("article", Text.COMMENT_COPY_ON, Comments.isCopyEnabled(),
                Comments::setCopyEnabled));
        column.addView(wrap(comments));

        column.addView(section(Text.INPUT_LIMITS));
        LinearLayout limits = card();
        limits.addView(liveToggleRow("article",Text.UNLIMITED_COMMENTS,Flags.isOn(TextLimits.COMMENTS),on->Flags.set(TextLimits.COMMENTS,on)));
        limits.addView(line());
        limits.addView(liveToggleRow("share",Text.UNLIMITED_REPOSTS,Flags.isOn(TextLimits.REPOSTS),on->Flags.set(TextLimits.REPOSTS,on)));
        limits.addView(line());
        limits.addView(liveToggleRow("tag",Text.UNLIMITED_HASHTAGS,Flags.isOn(TextLimits.HASHTAGS),on->Flags.set(TextLimits.HASHTAGS,on)));
        limits.addView(caption(Text.INPUT_LIMITS_NOTE));
        column.addView(wrap(limits));

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
        column.addView(caption(Text.DIM_NOTE));

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

        column.addView(section(Text.TEXTURES));
        LinearLayout textures = card();
        textures.addView(toggleRow("image", Text.TEXTURES_ON, Textures.isEnabled(), on -> {
            Textures.setEnabled(on);
            markChanged();
        }));
        textures.addView(line());
        textures.addView(actionRow("download", Text.TEXTURES_EXPORT,
                Text.TEXTURES_EXPORT_NOTE, () -> exportTextures(false)));
        textures.addView(line());
        textures.addView(actionRow("article", Text.TEXTURES_EXPORT_XML,
                Text.TEXTURES_EXPORT_XML_NOTE, () -> exportTextures(true)));
        textures.addView(line());
        textures.addView(actionRow("extension", Text.TEXTURES_INSTALL,
                Text.TEXTURES_INSTALL_NOTE, this::pickTextures));
        textures.addView(line());
        textures.addView(linkRow("article", Text.TEXTURES_DOCS,
                Text.OPEN_TTCUZ_SITE, TEXTURE_DOCS));
        List<Textures.Pack> packs = Textures.installed(this);
        if (packs.isEmpty()) {
            textures.addView(line());
            textures.addView(quiet(Text.TEXTURES_NONE));
        } else {
            for (Textures.Pack one : packs) {
                textures.addView(line());
                textures.addView(packRow(one));
            }
        }
        column.addView(wrap(textures));

        column.addView(section(Text.PLUGINS));
        LinearLayout plugins = card();
        plugins.addView(actionRow("add", "Добавить плагин", null, this::showPluginAdd));
        if(AdminAccess.current())plugins.addView(actionRow("extension","Управление магазином",null,()->PluginAdmin.show(this)));
        List<Plugins.Info> installed = Plugins.list();
        List<ScriptPlugins.Program> commands=ScriptPlugins.list(this);
        if(installed.isEmpty()&&commands.isEmpty())plugins.addView(quiet("Пока нет установленных плагинов"));
        for(ScriptPlugins.Program program:commands){
            boolean enabled=getSharedPreferences(ScriptPlugins.STORE,0).getBoolean("on:"+program.id,false);
            plugins.addView(actionRow("extension",program.name,enabled?"Включён":"Выключен",()->ScriptPlugins.details(this,program,this::rebuild)));
        }
        for(Plugins.Info info:installed)plugins.addView(pluginRow(info));
        column.addView(wrap(plugins));

        column.addView(section(Text.PINNED_FRIENDS));
        LinearLayout friends=card();friends.addView(actionRow("people",Text.PINNED_FRIENDS,null,()->PinnedFriends.show(this)));
        column.addView(wrap(friends));

        column.addView(section(Text.STREAKS));
        LinearLayout streaks = card();
        streaks.addView(betaRow("repeat", Text.STREAK_AUTO, Streaks.isEnabled(),
                Streaks::setEnabled));
        streaks.addView(line());
        // What the schedule is standing on, said plainly: an inexact alarm, a
        // battery saver or a muted notification channel each stop the send, and
        // none of them is visible anywhere else in the app.
        streaks.addView(actionRow("bug_report", Text.STREAK_HEALTH, StreakAlarm.status(this), () -> {
            StreakAlarm.permissions(this);
            Toast.makeText(this, StreakAlarm.status(this), Toast.LENGTH_LONG).show();
        }));
        streaks.addView(line());
        streaks.addView(actionRow("schedule", "Время отправки",StreakSchedule.label(),()->StreakSchedule.choose(this,this::rebuild)));
        streaks.addView(stickerHead());
        if (streakOpen) {
            streaks.addView(line());
            streaks.addView(appear(stickerChoices()));
        }
        streaks.addView(line());
        streaks.addView(actionRow("play_circle", Text.STREAK_ALL, Text.STREAK_ALL_NOTE,
                () -> Popup.ask(this, Text.STREAK_ALL, Text.STREAK_ALL_ASK,
                        Text.STREAK_ALL_SEND, () -> Streaks.sendAll(this),
                        Text.CANCEL, null, null, null)));
        streaks.addView(line());
        streaks.addView(actionRow("play_circle", Text.STREAK_TEST, Text.STREAK_TEST_NOTE,
                () -> {
                    Streaks.test(this);
                    Toast.makeText(this, Text.STREAK_TEST_GOING, Toast.LENGTH_SHORT).show();
                }));
        column.addView(wrap(streaks));

        column.addView(section(Text.NOTIFICATIONS));
        LinearLayout notifications=card();
        notifications.addView(liveToggleRow("chat",Text.NOTIFY_MESSAGES,ModNotifications.option(0),on->{ModNotifications.set(ModNotifications.MESSAGES,on);rebuildAfterSwitchAnimation();}));
        notifications.addView(liveToggleRow("notifications",Text.NOTIFY_PROMOS,ModNotifications.option(1),on->{ModNotifications.set(ModNotifications.PROMOS,on);rebuildAfterSwitchAnimation();}));
        notifications.addView(liveToggleRow("notifications",Text.NOTIFY_OTHER,ModNotifications.option(2),on->{ModNotifications.set(ModNotifications.OTHER,on);rebuildAfterSwitchAnimation();}));
        notifications.addView(liveToggleRow("notifications_off",Text.NOTIFY_NO_PROMPTS,ModNotifications.noPrompts(),on->{AccountAppearance.prefs(this).edit().putBoolean(ModNotifications.NO_PROMPTS,on).apply();ModNotifications.reload();}));
        notifications.addView(actionRow("settings",Text.NOTIFY_SETTINGS,null,()->ModNotifications.settings(this)));
        column.addView(wrap(notifications));

        column.addView(section(Text.CHANNEL));
        column.addView(channelBanner());

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

        column.addView(section(Text.DIAGNOSTICS));
        LinearLayout diagnostics = card();
        diagnostics.addView(liveToggleRow("bug_report", "Отчёты о сбоях",CrashReports.enabled(this),on->CrashReports.set(this,on)));
        diagnostics.addView(quiet("Отправлять разработчику технический стек сбоя. Без сообщений, аккаунта и текста ошибки."));
        diagnostics.addView(quiet(Diagnostics.report()));
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

        column.addView(section(Text.THANKS));
        LinearLayout support = card();
        support.addView(thanksHead());
        if (thanksOpen) {
            support.addView(line());
            View details = thanks();
            support.addView(appear(details));
        }
        column.addView(wrap(support));

        if (searching) {
            showSearchResults();
            showRestartBar();
            restoreScroll(0);
            return;
        }
        filterGroup();
        column.addView(versions());

        showRestartBar();
        restoreScroll(previousScroll);
    }

    /** A row is something that can be tapped or has a switch or a slider in it. */
    private static boolean settingRow(View view) {
        if (view.hasOnClickListeners()) return true;
        if (view instanceof android.widget.SeekBar || view instanceof M3Switch) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof android.widget.SeekBar || child instanceof M3Switch) return true;
                if (child instanceof ViewGroup && settingRow(child) && !child.hasOnClickListeners()) return true;
            }
        }
        return false;
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
            if (words.toString().toLowerCase(Locale.ROOT).contains(searchQuery)
                    || section.toLowerCase(Locale.ROOT).contains(searchQuery)) {
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
        for (java.util.List<View> rows : hits.values()) {
            for (View row : rows) {
                if (row.getParent() instanceof ViewGroup) ((ViewGroup) row.getParent()).removeView(row);
            }
        }
        while (column.getChildCount() > filterFrom) column.removeViewAt(column.getChildCount() - 1);
        sectionTargets.clear();
        if (hits.isEmpty()) {
            LinearLayout none = card();
            none.addView(caption(Text.SEARCH_NONE));
            column.addView(wrap(none));
            return;
        }
        for (java.util.Map.Entry<String, java.util.List<View>> entry : hits.entrySet()) {
            column.addView(section(entry.getKey()));
            LinearLayout card = card();
            for (View row : entry.getValue()) card.addView(row);
            column.addView(wrap(card));
        }
    }

    private void restoreScroll(final int scrollY) {
        Fonts.applyTree(root);
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
        pending = true;
        showRestartBar();
    }

    /** A live preview based on the identity TikTok has loaded for this account. */
    private View realProfilePreview() {
        FrameLayout panel = new FrameLayout(this);
        panel.setClipChildren(false);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable surface = new GradientDrawable();
        surface.setColor(skin.page);
        surface.setCornerRadius(dp(18));
        surface.setStroke(Math.max(1, dp(1)), blend(skin.text, skin.page, 0.88f));
        panel.setBackground(surface);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipChildren(false);
        ImageView avatar = new ImageView(this);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(blend(skin.muted(), skin.page, 0.72f));
        avatar.setBackground(circle);
        avatar.setClipToOutline(true);
        avatar.setVisibility(View.INVISIBLE);

        TextView initial = new TextView(this);
        initial.setGravity(Gravity.CENTER);
        initial.setTextColor(skin.text);
        initial.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        String name = Account.profileName();
        String uid = Account.id();
        boolean live = name != null && name.length() > 0;
        initial.setText(live ? name.substring(0, 1).toUpperCase(Locale.ROOT) : "•");

        FrameLayout avatarFrame = new FrameLayout(this);
        GradientDrawable avatarBack = new GradientDrawable();
        avatarBack.setShape(GradientDrawable.OVAL);
        avatarBack.setColor(blend(skin.muted(), skin.page, 0.72f));
        avatarFrame.setBackground(avatarBack);
        avatarFrame.setClipToOutline(true);
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
        syncRetry=label("Повторить");syncRetry.setTextSize(13);syncRetry.setFocusable(true);syncRetry.setTextColor(Accent.colour());syncRetry.setGravity(Gravity.CENTER);syncRetry.setMinHeight(dp(48));syncRetry.setPadding(dp(12),0,0,0);Motion.touch(syncRetry);
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
        String title=!shared ? "Только на этом устройстве" : state==TtcuzProfileSync.State.SENT ? "Отправлено"
                : state==TtcuzProfileSync.State.ERROR ? "Не отправлено" : state==TtcuzProfileSync.State.HELD ? "Нужно подтверждение"
                : state==TtcuzProfileSync.State.VERIFYING ? "Проверяю профиль" : busy ? "Отправляется" : "Ожидает отправки";
        String handle=Account.profileHandle();String account=handle==null?"Откройте свой профиль":"@"+handle;
        String proof=Account.liveId()!=null&&new CloudProfileProvider(this).verified(Account.liveId())?"Подтверждён":"Не подтверждён";
        String details=account+" · "+proof+"\n"+(shared ? TtcuzProfileSync.status() : "Включите синхронизацию выше");
        if(!title.contentEquals(syncTitle.getText())){syncTitle.setText(title);if(syncTitle.isShown())Motion.change(syncTitle);}
        if(!details.contentEquals(syncDetails.getText()))syncDetails.setText(details);
        syncSpinner.setVisibility(busy && Motion.enabled(this) ? View.VISIBLE : View.GONE);
        syncRetry.setVisibility(shared && !busy ? View.VISIBLE : View.GONE);
        String retryText=state==TtcuzProfileSync.State.HELD ? "Проверить" : "Повторить";
        if(!retryText.contentEquals(syncRetry.getText()))syncRetry.setText(retryText);
        boolean canRetry=state==TtcuzProfileSync.State.HELD || TtcuzProfileSync.canRetryOwn();
        syncRetry.setEnabled(canRetry);syncRetry.setAlpha(canRetry ? 1f : 0.45f);
    }

    private void changeCommunityBadge() {
        final String creator = Account.activeId();
        if (!AdminAccess.allowed(creator)) return;
        final CloudProfileProvider sync = new CloudProfileProvider(this);
        if (!sync.verified(creator)) {
            Toast.makeText(this, "Сначала подтвердите аккаунт создателя", Toast.LENGTH_LONG).show();
            verifyProfile();
            return;
        }
        Toast.makeText(this, "Загружаю значки…", Toast.LENGTH_SHORT).show();
        Net.away("badge catalog", () -> {
            java.util.List<CloudProfileProvider.CatalogItem> items = null;
            try { items = sync.catalog(); } catch (Throwable error) {
                Diary.note("badge catalog: " + error);
            }
            final java.util.List<CloudProfileProvider.CatalogItem> found = items;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (found == null) {
                    Toast.makeText(this, "Не удалось загрузить значки. Проверьте сеть", Toast.LENGTH_LONG).show();
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
        final Panel panel=adminPanel=Panel.with(this,skin,"Выдать значок");
        final EditText who=panel.field("@ник, ссылка или UID");
        panel.view(actionRow("arrow_forward","Открыть профиль",null,()->{
            String target=BadgeTarget.parse(who.getText().toString());
            if(target==null || target.matches("[0-9]+")){panel.busy(false,"Введите @ник получателя");return;}
            try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.tiktok.com/@"+target)).setPackage(getPackageName()));}
            catch(Throwable error){panel.busy(false,"Не удалось открыть профиль в TikTok. Можно указать UID получателя");}
        }));
        panel.view(actionRow("account_circle","Недавние профили",null,()->{
            java.util.List<String[]> recent=RecentProfiles.choices();
            if(recent.isEmpty()){panel.busy(false,"Откройте профиль получателя в TikTok, затем вернитесь сюда");return;}
            CharSequence[] labels=new CharSequence[recent.size()];for(int i=0;i<labels.length;i++)labels[i]="@"+recent.get(i)[0];
            new ModDialog.Builder(this,skin).setTitle("Получатель").setItems(labels,(dialog,index)->who.setText(recent.get(index)[1])).show();
        }));
        LinearLayout preview=row();
        ImageView badgePicture=new ImageView(this);badgePicture.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams imageSize=new LinearLayout.LayoutParams(dp(36),dp(36));imageSize.rightMargin=dp(16);preview.addView(badgePicture,imageSize);
        LinearLayout caption=new LinearLayout(this);caption.setOrientation(LinearLayout.VERTICAL);
        TextView recipient=label("Получатель");recipient.setMaxLines(1);recipient.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView badgeTitle=detail(items.isEmpty() ? "Нет доступных значков" : items.get(0).title);
        caption.addView(recipient);caption.addView(badgeTitle);preview.addView(caption,grow());panel.view(preview);
        if(!items.isEmpty())badgePreview(badgePicture,items.get(0));
        who.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence text,int start,int count,int after){}
            public void onTextChanged(CharSequence text,int start,int before,int count){
                String target=BadgeTarget.parse(text.toString());
                recipient.setText(target==null ? "Получатель" : target.matches("[0-9]+") ? "UID "+target : "@"+target);
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
        panel.primaryKeepOpen("Выдать", () -> pickAndSend(panel, sync, chosen[0], who, true));
        panel.keepOpen("Снять этот значок", () -> pickAndSend(panel, sync, chosen[0], who, false));
        panel.quiet("Добавить свою картинку", this::pickCatalogBadge);
        panel.quiet("Закрыть", null);
        panel.show();
    }

    private void badgePreview(ImageView view,CloudProfileProvider.CatalogItem item) {
        view.clearColorFilter();view.setImageBitmap(item.picture);
        if(item.colour!=0)view.setColorFilter(item.colour,android.graphics.PorterDuff.Mode.SRC_IN);
        view.setContentDescription(item.title);
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
            Toast.makeText(this, "Сначала выберите значок", Toast.LENGTH_SHORT).show();
            return;
        }
        if (handle == null) {
            Toast.makeText(this, "Введите @ник, ссылку на профиль или UID", Toast.LENGTH_LONG).show();
            return;
        }
        submitBadge(panel, sync, badgeId, handle, grant);
    }

    private void submitBadge(Panel panel, CloudProfileProvider sync, final String badgeId,
                             final String handle, final boolean grant) {
        panel.busy(true, "Отправляю…");
        Net.away("badge grant", () -> {
            boolean okay = false;
            String failure = "Не удалось связаться с сервером";
            try { okay = sync.setBadge(badgeId, handle, grant); } catch (Throwable error) {
                Diary.note("badge grant: " + error);
                failure = error.getMessage() == null ? failure : error.getMessage();
            }
            final boolean result = okay;
            final String message = failure;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                panel.busy(false, result ? (grant ? "Значок выдан: " : "Значок снят: ") + handle : message);
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
        } catch (Throwable error) { Screen.say("Не удалось открыть выбор картинки"); }
    }

    private void importCatalogBadge(Uri source) {
        if(!AdminAccess.current())return;
        Net.away("catalog image", () -> {
            try {
                byte[] png = BadgeImage.read(getApplicationContext(), source);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || !AdminAccess.current()) return;
                    Panel panel = adminPanel=Panel.with(this, skin, "Новый значок");
                    ImageView preview = new ImageView(this);
                    preview.setImageBitmap(android.graphics.BitmapFactory.decodeByteArray(png,0,png.length));
                    preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    LinearLayout box = new LinearLayout(this); box.setGravity(Gravity.CENTER);
                    box.addView(preview, new LinearLayout.LayoutParams(dp(80),dp(80)));
                    panel.view(box).text("Картинка появится в каталоге. Её можно будет выдавать другим пользователям.");
                    EditText title = panel.field("Название значка");
                    title.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(40)});
                    panel.primaryKeepOpen("Добавить в каталог", () -> {
                        if(!AdminAccess.current()){panel.close();return;}
                        String name = title.getText().toString().trim();
                        if (name.length() == 0) { title.setError("Введите название"); return; }
                        panel.busy(true, "Загружаю значок…");
                        Net.away("catalog upload", () -> {
                            String failure = null;
                            try { new CloudProfileProvider(this).createBadge(name,png); }
                            catch (Throwable error) { failure = error.getMessage(); if (failure == null) failure = "Не удалось загрузить значок"; }
                            final String problem = failure;
                            runOnUiThread(() -> {
                                if (isFinishing() || isDestroyed() || !AdminAccess.current()) return;
                                boolean showing = panel.dialog().isShowing();
                                if (showing) panel.busy(false,problem);
                                if (problem == null) {
                                    Badges.refreshNow(getApplicationContext());
                                    if (showing) { panel.close(); changeCommunityBadge(); }
                                    else Screen.say("Значок добавлен в каталог");
                                } else if (!showing) Screen.say(problem);
                            });
                        });
                    });
                    panel.quiet(Text.CANCEL,null); panel.show();
                });
            } catch (Throwable error) { Screen.say(error.getMessage() == null ? "Не удалось открыть картинку" : error.getMessage()); }
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
        if (lead instanceof Expressive.Badge) ((Expressive.Badge) lead).set(Margy.isEnabled(), false);
        row.addView(lead);
        row.addView(label(Text.CHANGE_REGION), grow());

        final M3Switch toggle = new M3Switch(this);
        toggle.colours(Accent.colour(), skin.muted(), skin.card);
        toggle.setChecked(Margy.isEnabled());
        toggle.setOnChanged(checked -> {
            if (lead instanceof Expressive.Badge) ((Expressive.Badge) lead).set(checked, true);
            Margy.setEnabled(checked);
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
        String[] current = Margy.current();
        LinearLayout row = row();
        row.addView(flag(current[Margy.ISO]));
        row.setAlpha(Margy.isEnabled() ? 1f : 0.4f);

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(current[Margy.LABEL]));
        text.addView(detail(current[Margy.CARRIER] + "  ·  " + current[Margy.MCCMNC]
                + "  ·  " + current[Margy.ISO].toUpperCase(Locale.US)));
        row.addView(text, grow());

        row.addView(disclosure(countriesOpen,"countriesOpen"));

        if (Margy.isEnabled()) {
            row.setOnClickListener(v -> {
                disclosureKey = "countriesOpen";
                countriesOpen = !countriesOpen;
                rebuildAfterReveal(false);
            });
        }
        return sized(row, 60);
    }

    private View countryRow(final String[] country) {
        boolean selected = country[Margy.ISO].equals(Margy.iso());
        LinearLayout row = row();
        row.addView(flag(country[Margy.ISO]));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(country[Margy.LABEL]));
        text.addView(detail(country[Margy.CARRIER] + "  ·  " + country[Margy.MCCMNC]
                + "  ·  " + country[Margy.ISO].toUpperCase(Locale.US)));
        row.addView(text, grow());

        if (selected) row.addView(new Check(this, Accent.colour()));

        row.setOnClickListener(v -> {
            Margy.setIso(country[Margy.ISO]);
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

        row.addView(disclosure(accentOpen,"accentOpen"));

        row.setOnClickListener(v -> {
            disclosureKey = "accentOpen";
            accentOpen = !accentOpen;
            rebuildAfterReveal(accentOpen);
        });
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
        target.setContentDescription(up ? "Выше" : "Ниже");
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
        background.setColor(blend(0xFF40E0D0, skin.card, 0.82f));
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
        take.setTextColor(0xFF10221F);
        take.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        take.setTypeface(Typeface.DEFAULT_BOLD);
        take.setGravity(Gravity.CENTER);
        take.setPadding(0, dp(11), 0, dp(11));
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(0xFF40E0D0);
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
            if(arrow!=null)arrow.animate().rotation(0f).setDuration(180L).start();
            final int[] remaining={bodies.size()};
            for(DisclosureBody body:new java.util.ArrayList<>(bodies))body.collapse(()->{
                if(--remaining[0]==0){collapsing=false;revealAnimationRequested=false;disclosureKey=null;rebuild();}
            });
            return;
        }
        revealAnimationRequested=expanded;rebuild();disclosureKey=null;
    }

    // ------------------------------------------------------ the texture packs

    private static final int PICK_TEXTURES = 0x4D54;  // "MT"

    private static final String TEXTURE_DOCS =
            "https://ttcuz.daniilsolovatulin.workers.dev/guide.html#textures";

    private View packRow(final Textures.Pack one) {
        boolean chosen = one.file.equals(Textures.pack());
        LinearLayout row = row();
        row.addView(icon("image"));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(one.name));
        String under = one.author.length() > 0 ? one.author : one.about;
        if (!one.fits()) under = Text.TEXTURES_WRONG_VERSION + "  ·  " + one.tiktok;
        if (under.length() > 0) text.addView(detail(under));
        row.addView(text, grow());

        if (chosen) row.addView(new Check(this, Accent.colour()));
        row.setOnClickListener(v -> {
            Textures.choose(chosen ? "" : one.file);
            markChanged();
            rebuild();
        });
        row.setOnLongClickListener(v -> {
            Textures.remove(this, one.file);
            markChanged();
            rebuild();
            return true;
        });
        return sized(row, 60);
    }

    private void exportTextures(final boolean withXml) {
        Screen.progress(Text.TEXTURES_EXPORTING, 0);
        Net.away("textures", () -> {
            final java.io.File out = Textures.export(this, withXml);
            runOnUiThread(() -> Screen.say(out == null
                    ? Text.TEXTURES_FAILED : Text.TEXTURES_EXPORTED));
        });
    }

    private void pickTextures() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_TEXTURES);
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
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

    private static final int PICK_FONT = 0x4D46;  // "MF"
    private static final int PICK_EMOJI = 0x4D45;  // "ME"
    private static final int PICK_PROFILE_BADGE = 0x5450; // "TP"

    private void pickProfileBadge() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
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

        row.addView(disclosure(fontOpen,"fontOpen"));

        row.setOnClickListener(v -> {
            disclosureKey = "fontOpen";
            fontOpen = !fontOpen;
            rebuildAfterReveal(fontOpen);
        });
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

        String now = Fonts.name();
        for (int i = 0; i < Fonts.PRESETS.length; i++) {
            final String which = Fonts.PRESETS[i];
            if (i > 0) rows.addView(line());
            rows.addView(pickRow(fontName(which), which.equals(now),
                    Fonts.SYSTEM.equals(which) ? null : which, () -> {
                        Fonts.choose(which);
                        markChanged();
                        rebuild();
                    }));
        }
        rows.addView(line());
        rows.addView(actionRow("download", Text.FONT_PICK,
                Fonts.FILE.equals(now) ? Text.FONT_FILE : null, this::pickFont));

        rows.addView(line());
        rows.addView(section(Text.EMOJI));
        String emoji = Fonts.emoji();
        List<EmojiPackManager.Pack> packs = EmojiPackManager.packs(this);
        boolean firstPack = true;
        for (EmojiPackManager.Pack pack : packs) {
            final String which = pack.id;
            if (Fonts.EMOJI_FILE.equals(which)) continue;
            if (!firstPack) rows.addView(line());
            firstPack = false;
            rows.addView(pickRow(emojiName(pack), which.equals(emoji), null, () -> {
                Fonts.chooseEmoji(this, which);
                markChanged();
                rebuild();
            }));
        }
        rows.addView(line());
        rows.addView(actionRow("download", Text.EMOJI_FILE,
                Fonts.EMOJI_FILE.equals(emoji) ? Text.EMOJI_FILE : null, this::pickEmoji));
        TextView sample = new TextView(this);
        sample.setText(Text.EMOJI_PREVIEW);
        sample.setTextColor(skin.text);
        sample.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
        sample.setGravity(Gravity.CENTER);
        sample.setPadding(dp(16), dp(12), dp(16), dp(12));
        android.graphics.Typeface emojiPreview = Fonts.preview(this, emoji);
        if (emojiPreview != null) sample.setTypeface(emojiPreview);
        rows.addView(sample, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));
        rows.addView(quiet(Text.EMOJI_NOTE));
        return rows;
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
            android.graphics.Bitmap picture = Launcher.preview(which);
            if (picture != null) shot.setImageBitmap(picture);
            shot.setScaleType(ImageView.ScaleType.FIT_CENTER);
            shot.setPadding(dp(4), dp(4), dp(4), dp(4));
            android.graphics.drawable.GradientDrawable ring = new android.graphics.drawable.GradientDrawable();
            ring.setCornerRadius(dp(18));
            ring.setColor(0x00000000);
            ring.setStroke(dp(picked ? 3 : 1), picked ? Accent.colour() : Expressive.mix(skin.card, skin.text, 0.18f));
            shot.setBackground(ring);
            item.addView(shot, new LinearLayout.LayoutParams(dp(64), dp(64)));

            TextView name = label(Launcher.nameOf(which));
            name.setTextSize(12);
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
    private static final int[] DARKS = {
            0xFF000000, 0xFF0B0B0F, 0xFF121212, 0xFF161823, 0xFF1B1B1B,
            0xFF0D1B2A, 0xFF12232E, 0xFF1A1423, 0xFF14261C, 0xFF241A1A,
    };

    private static final int[] LIGHTS = {
            0xFFFFFFFF, 0xFFF6F6F6, 0xFFEDEDED, 0xFFE8E4DA, 0xFFDCDCDC,
            0xFFCFD8DC, 0xFFB0B8C4, 0xFF8A8A8A, 0xFF5A5A5A, 0xFF2E2E2E,
    };

    private View shades(final boolean forText) {
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(16), dp(12), dp(16), dp(16));

        int[] choices = forText ? LIGHTS : DARKS;
        int now = forText ? Themes.text() : Themes.background();
        LinearLayout line = null;
        for (int i = 0; i < choices.length; i++) {
            if (i % 5 == 0) {
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
                askColour(forText ? Text.THEME_TEXT : Text.THEME_BACKGROUND,
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
        row.setOnClickListener(v -> askColour(AppearanceColors.label(role),
                selected == 0 ? Accent.TIKTOK : selected, colour -> {
                    AppearanceColors.set(role, colour);
                    markChanged();
                    rebuild();
                }));
        return sized(row, 52);
    }

    private void askColour(String title, int initial, ColourPicked picked) {
        Panel panel = Panel.with(this,skin,title);
        EditText input = panel.field("#RRGGBB");
        input.setText(String.format(Locale.US, "#%06X", initial & 0xFFFFFF));
        panel.primaryKeepOpen(getString(android.R.string.ok), () -> {
                    try {
                        int colour = android.graphics.Color.parseColor(
                                input.getText().toString().trim());
                        picked.set(colour);
                        panel.close();
                    } catch (IllegalArgumentException error) {
                        input.setError("Укажите цвет в формате #RRGGBB");
                    }
                });
        panel.quiet(Text.CANCEL,null); panel.show();
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
        android.graphics.drawable.Drawable vector = name.equals("search")
                ? new Expressive.Magnifier(0xFFFFFFFF) : SettingsGlyph.make(name, 0xFFFFFFFF);
        if (vector == null) {
            Bitmap picture = pictureFor(name);
            if (picture != null) vector = new android.graphics.drawable.BitmapDrawable(getResources(), picture);
        }
        int accent = Accent.colour();
        int tonal = accentTonal();
        int ink = accentInk();
        Expressive.Badge badge = toggle
                ? new Expressive.Badge(this, shapeFor(name), vector, sizeDp,
                        Expressive.mix(skin.card, skin.text, 0.14f), accent, skin.muted(), onAccent())
                : new Expressive.Badge(this, shapeFor(name), vector, sizeDp, tonal, tonal, ink, ink);
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
        return sized(row, 48);
    }

    /**
     * An identifier, and a tap to copy it.
     *
     * The long one does not fit on a phone, so what is shown is the ends of it
     * and what is copied is all of it.
     */
    /**
     * A switch with a word beside it saying not to trust it yet.
     *
     * The only thing in the mod that acts on its own and the only one that
     * sends anything, so it says so on the row rather than in a note nobody
     * reads.
     */
    private View betaRow(String picture, String title, boolean on, final Setting setting) {
        LinearLayout row = row();
        final View lead = icon(picture, true, 40);
        if (lead instanceof Expressive.Badge) ((Expressive.Badge) lead).set(on, false);
        row.addView(lead);
        row.addView(label(title), grow());

        TextView beta = new TextView(this);
        beta.setText(Text.BETA);
        beta.setTextColor(onAccent());
        beta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        beta.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        beta.setPadding(dp(8), dp(3), dp(8), dp(3));
        GradientDrawable chip = new GradientDrawable();
        chip.setColor(Accent.colour());
        chip.setCornerRadius(dp(12));
        beta.setBackground(chip);
        LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        place.leftMargin = dp(8);
        row.addView(beta, place);

        final M3Switch toggle = new M3Switch(this);
        toggle.colours(Accent.colour(), skin.muted(), skin.card);
        toggle.setChecked(on);
        toggle.setOnChanged(checked -> {
            if (lead instanceof Expressive.Badge) ((Expressive.Badge) lead).set(checked, true);
            setting.set(checked);
            markChanged();
        });
        LinearLayout.LayoutParams togglePlace = new LinearLayout.LayoutParams(dp(52), dp(32));
        togglePlace.leftMargin = dp(10);
        row.addView(toggle, togglePlace);
        return sized(row, 52);
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
        boolean value = detail != null && detail.length() <= 14 && title.length() + detail.length() <= 28;

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(title));
        if (detail != null && !value) text.addView(detail(detail));
        row.addView(text, grow());

        if (value) {
            TextView shown = detail(detail);
            shown.setGravity(Gravity.CENTER_VERTICAL);
            shown.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);shown.setPadding(dp(8), 0, dp(6), 0);
            row.addView(shown);
        }
        ImageView chevron = new ImageView(this);
        chevron.setImageDrawable(SettingsGlyph.make("chevron_right", skin.muted()));
        row.addView(chevron, new LinearLayout.LayoutParams(dp(24), dp(24)));

        row.setOnClickListener(v -> action.run());
        return sized(row, detail == null || value ? 48 : 60);
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

    // ----------------------------------------------------------- the plugins

    private void showPluginAdd(){
        Panel panel=Panel.with(this,skin,"Добавить плагин");
        panel.primary("Создать из кнопок",()->ScriptPlugins.edit(this,ScriptPlugins.SAMPLE,this::rebuild));
        panel.quiet("Вставить JSON",()->ScriptPlugins.edit(this,"",this::rebuild));
        panel.quiet("Установить файл .ctt",this::pickPlugin);
        panel.quiet("Магазин",()->{
            Panel store=Panel.with(this,skin,"Магазин плагинов");
            store.primary("Плагины из кнопок",()->ScriptPlugins.catalog(this,this::rebuild));
            store.quiet("Плагины с кодом (.ctt)",()->startActivity(new Intent(this,StoreActivity.class)));
            store.quiet(Text.CLOSE,null);store.show();
        });
        panel.quiet("Документация",()->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(DOCS_PLUGINS))));
        panel.quiet(Text.CLOSE,null);panel.show();
    }

    private static final int PICK_PLUGIN = 0x4D50;  // "MP"

    /**
     * What plugins put into the settings: one block per switched-on plugin,
     * under its own name, in the same rows the mod's own settings use.
     */


    private View pluginSettingRow(final Plugins.Row item,
                                  final android.content.SharedPreferences store) {
        switch (item.kind) {
            case Plugins.Row.SECTION:
                return caption(item.title);
            case Plugins.Row.TOGGLE:
                return toggleRow("extension", item.title,
                        store.getBoolean(item.key, item.def != 0),
                        on -> {store.edit().putBoolean(item.key, on).apply();Plugins.settingsChanged(item.plugin,item.key);});
            case Plugins.Row.CHOICE: {
                final String[] options = item.options;
                if (options == null || options.length == 0) return null;
                int now = Math.max(0, Math.min(options.length - 1,
                        store.getInt(item.key, item.def)));
                return actionRow("tune", item.title, options[now], () ->
                        new ModDialog.Builder(this, skin)
                                .setTitle(item.title)
                                .setSingleChoiceItems(options,
                                        Math.max(0, Math.min(options.length - 1,
                                                store.getInt(item.key, item.def))),
                                        (dialog, which) -> {
                                            store.edit().putInt(item.key, which).apply();
                                            Plugins.settingsChanged(item.plugin,item.key);
                                            markChanged();
                                            dialog.dismiss();
                                            rebuild();
                                        })
                                .show());
            }
            case Plugins.Row.SLIDER: {
                int now = Math.max(item.min, Math.min(item.max,
                        store.getInt(item.key, item.def)));
                return slider(item.title, now - item.min, item.max - item.min, item.min, "",
                        value -> {
                            store.edit().putInt(item.key, value + item.min).apply();
                            Plugins.settingsChanged(item.plugin,item.key);
                            markChanged();
                        });
            }
            default:
                return actionRow("extension", item.title, item.detail, () -> {
                    try {
                        if (item.action != null) item.action.tapped();
                    } catch (Throwable error) {
                        Diary.note("plugin " + item.plugin + ": " + error);
                    }
                });
        }
    }

    private View installRow() {
        LinearLayout row = row();
        row.addView(icon("extension"));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(Text.PLUGIN_INSTALL));
        text.addView(detail(Text.PLUGIN_INSTALL_NOTE));
        row.addView(text, grow());

        TextView plus = new TextView(this);
        plus.setText("+");
        plus.setTextColor(Accent.colour());
        plus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        plus.setPadding(dp(12), 0, 0, 0);
        row.addView(plus);

        row.setOnClickListener(v -> pickPlugin());
        return sized(row, 60);
    }

    /**
     * One installed plugin: what it says about itself, and a switch.
     *
     * Not `sized()` like the other rows -- a description is as tall as it is,
     * and a plugin whose author wrote two sentences should not have the second
     * one clipped.
     */
    private View pluginRow(final Plugins.Info info) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));

        Bitmap icon = info.icon();
        if (icon != null) {
            ImageView view = new ImageView(this);
            view.setImageBitmap(icon);
            view.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams size =
                    new LinearLayout.LayoutParams(dp(40), dp(40));
            size.rightMargin = dp(14);
            row.addView(view, size);
        } else {
            LinearLayout.LayoutParams size =
                    new LinearLayout.LayoutParams(dp(40), dp(40));
            size.rightMargin = dp(14);
            row.addView(new Dot(this, Accent.colour(), false), size);
        }

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(info.name + "  " + info.version));

        String by = info.author;
        if (!info.description.isEmpty()) by = by + "  ·  " + info.description;
        text.addView(detail(by));
        if (info.trouble != null) text.addView(detail(info.trouble));
        if(info.safety!=null&&!info.safety.isEmpty())text.addView(detail("Статическая проверка · удерживайте для действий"));
        row.addView(text, grow());

        final M3Switch toggle = new M3Switch(this);
        toggle.colours(Accent.colour(), skin.muted(), skin.card);
        toggle.setChecked(Plugins.isEnabled(info.id));
        toggle.setEnabled(info.trouble == null || "Нужно подтвердить возможности плагина".equals(info.trouble) || Plugins.isEnabled(info.id));
        toggle.setOnChanged(checked -> {
            activatePlugin(info,checked);
        });
        row.addView(toggle);

        row.setOnClickListener(v -> openPluginSettings(info));
        Motion.press(row);
        row.setOnLongClickListener(v -> {
            Panel actions=Panel.with(this,skin,info.name).compact();
            actions.primary("Редактировать",()->PluginEditorActivity.binary(this,info.id));
            actions.quiet("Удалить с устройства",()->askToRemove(info));
            if(AdminAccess.current())actions.quiet("Управление магазином",()->PluginAdmin.show(this));
            actions.quiet(Text.CLOSE,null);actions.show();
            return true;
        });
        return row;
    }

    private void activatePlugin(final Plugins.Info info,boolean enabled){
        if(!enabled){Net.away("plugin stop",()->{Plugins.setEnabled(info.id,false);runOnUiThread(()->{if(!isFinishing())rebuild();});});return;}
        Net.away("plugin check",()->{
            try{final PluginAudit.Report report=Plugins.inspectInstalled(info.id);
                runOnUiThread(()->{if(isFinishing())return;
                    if(report.blocked){Panel.with(this,skin,"Плагин заблокирован").text(report.summary()).quiet("Назад",null).show();rebuild();return;}
                    Runnable enable=()->Net.away("plugin activation",()->{Plugins.approve(this,info.id,report.digest);Plugins.setEnabled(info.id,true);runOnUiThread(()->{if(!isFinishing())rebuild();});});
                    if(report.findings.isEmpty())enable.run();else Panel.with(this,skin,"Возможности плагина").text(report.summary()).primary("Разрешить и включить",()->enable.run()).quiet("Назад",()->rebuild()).show();
                });
            }catch(Throwable error){Screen.say(String.valueOf(error.getMessage()));runOnUiThread(()->{if(!isFinishing())rebuild();});}
        });
    }

    private void openPluginSettings(final Plugins.Info info) {
        if(Plugins.isEnabled(info.id) && Plugins.openSettings(info.id,this))return;
        Panel panel=Panel.with(this,skin,info.name);
        if(info.safety!=null&&!info.safety.isEmpty())panel.text(info.safety);
        LinearLayout body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        android.content.SharedPreferences store=getSharedPreferences("margyt_plugin_"+info.id,MODE_PRIVATE);
        int count=0;
        if(Plugins.isEnabled(info.id)) for(Plugins.Row item:Plugins.rows()) {
            if(!info.id.equals(item.plugin))continue;
            try { View control=pluginSettingRow(item,store); if(control!=null){body.addView(control);count++;} }
            catch(Throwable error){Diary.note("plugin settings "+info.id+": "+error);}
        }
        if(count==0){
            if(!Plugins.isEnabled(info.id))panel.text("Включите плагин, чтобы открыть его настройки.");
            else if(Plugins.isRunning(info.id))panel.text("Плагин не предоставил настройки. Версия "+info.version);
            else{
                String why=Plugins.problem(info.id);
                panel.text(why!=null?"Плагин включён, но не запущен: "+why:"Плагин включён, но ещё не запущен в этом сеансе.");
                panel.primary("Запустить заново",()->Net.away("plugin revive",()->{
                    final boolean ok=Plugins.revive(info.id);
                    runOnUiThread(()->{if(isFinishing())return;rebuild();if(ok)openPluginSettings(info);});
                }));
            }
        }else panel.view(body);
        panel.quiet("Назад",null).show();
    }

    private void pickPlugin() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            // .ctt is nobody's registered type, so the picker is shown everything
            intent.setType("*/*");
            startActivityForResult(intent, PICK_PLUGIN);
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null) return;
        Uri source = data.getData();
        if (source == null) return;

        if (request == PICK_CATALOG_BADGE) { if(AdminAccess.current())importCatalogBadge(source); return; }

        if (request == PICK_PROFILE_BADGE) {
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

        if (request == PICK_TEXTURES) {
            if (Textures.install(this, source)) markChanged();
            else Toast.makeText(this, Text.TEXTURES_FAILED, Toast.LENGTH_LONG).show();
            rebuild();
            return;
        }
        if (request == PICK_FONT || request == PICK_EMOJI) {
            if (Fonts.take(this, source, request == PICK_EMOJI)) markChanged();
            else Toast.makeText(this, Text.FONT_FAILED, Toast.LENGTH_LONG).show();
            rebuild();
            return;
        }
        if (request != PICK_PLUGIN) return;
        reviewPlugin(source);
    }
    private void reviewPlugin(final Uri source){
        Net.away("plugin inspection",()->{
            try {final PluginAudit.Report report=Plugins.inspect(this,source);
                runOnUiThread(()->{
                    if(isFinishing())return;
                    Panel panel=Panel.with(this,skin,"Проверка плагина").text(report.summary());
                    if(!report.blocked)panel.primary("Установить выключенным",()->Net.away("plugin install",()->{
                        try {Plugins.install(this,source,report.digest);runOnUiThread(()->{if(!isFinishing())rebuild();});Screen.say(Text.PLUGIN_INSTALLED);}
                        catch(Throwable error){Screen.say(String.valueOf(error.getMessage()));}
                    }));
                    panel.quiet("Назад",null).show();
                });
            }catch(Throwable error){runOnUiThread(()->{if(!isFinishing())Panel.with(this,skin,"Плагин не принят").text(String.valueOf(error.getMessage())).quiet("Назад",null).show();});}
        });
    }

    private void askToRemove(final Plugins.Info info) {
        try {
            new ModDialog.Builder(this, skin)
                    .setTitle(Text.PLUGIN_REMOVE_ASK)
                    .setMessage(info.name + "  " + info.version)
                    .setNegativeButton(Text.CANCEL, null)
                    .setPositiveButton(Text.PLUGIN_REMOVE, (dialog, which) -> {
                        Plugins.uninstall(this, info.id);
                        markChanged();
                        rebuild();
                    })
                    .show();
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
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
        rows.addView(owner());
        rows.addView(person("@dddrocka", Text.THANKS_ICONS, null));
        rows.addView(person("GPT 6 Sol", Text.THANKS_AI, null));
        rows.addView(person("@trem0r1_1", Text.THANKS_HELPER,
                "https://www.tiktok.com/@trem0r1_1"));

        return rows;
    }

    /** The one row with two places to go, so it asks which. */
    private View owner() {
        LinearLayout row = row();

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label("@narezany"));
        text.addView(detail(Text.THANKS_OWNER));
        row.addView(text, grow());
        row.addView(away());

        row.setOnClickListener(v -> {
            try {
                new ModDialog.Builder(this, skin)
                        .setTitle("@narezany")
                        .setItems(new CharSequence[]{"Telegram", "TikTok"}, (dialog, which) ->
                                open(which == 0 ? OWNER_TELEGRAM : OWNER_TIKTOK))
                        .setNegativeButton(Text.CANCEL, null)
                        .show();
            } catch (Throwable error) {
                open(OWNER_TELEGRAM);
            }
        });
        return sized(row, 60);
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
        try {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText(what, text));
            Toast.makeText(this, Text.COPIED, Toast.LENGTH_SHORT).show();
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
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
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("ttcuz", out.toString()));
            Toast.makeText(this, Text.COPIED, Toast.LENGTH_SHORT).show();
        } catch (Throwable error) {
            Toast.makeText(this, String.valueOf(error), Toast.LENGTH_LONG).show();
        }
    }

    // ---------------------------------------------------------- the parts

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
        name.setText(settingsGroup == 0 ? "TikTok" : "Настройки");
        name.setTextColor(skin.muted());
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        name.setPadding(dp(12), 0, 0, 0);
        bar.addView(name);
        bar.setContentDescription(settingsGroup == 0 ? "TikTok" : "Настройки");
        bar.setOnClickListener(v -> goBack());
        return bar;
    }

    private View title(String text) {
        final TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(skin.text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, settingsGroup == 0 ? 44 : 32);
        view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        if (settingsGroup != 0) {
            view.setPadding(dp(28), dp(8), dp(24), dp(16));
            // five taps in a row and a cat turns up; one by accident does nothing
            view.setOnClickListener(v -> {
                Cats.tapped(view);
                developerTap();
            });
            return view;
        }

        // The main page: "ttcuz" and the small note beside it are two separate views.
        // A tap on the name sends a bat at the name itself; holding the note opens its menu.
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setBaselineAligned(true);
        line.setPadding(dp(28), dp(8), dp(24), dp(16));
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

        String attribution = attributionText();
        final TextView note;
        if (attribution != null) {
            note = new TextView(this);
            note.setText(attribution);
            note.setTextColor((skin.text & 0x00FFFFFF) | 0x88000000);
            note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            note.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            note.setOnLongClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                attributionMenu();
                return true;
            });
            LinearLayout.LayoutParams where = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            where.leftMargin = dp(10);
            line.addView(note, where);
        } else {
            note = null;
            // nothing left to hold, so the name itself opens the menu: the note can be brought back
            word.setOnLongClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                attributionMenu();
                return true;
            });
        }

        // five taps in a row and a cat turns up; one by accident does nothing
        word.setOnClickListener(v -> {
            Cats.tapped(word, word);
            developerTap();
        });
        BatHit.prepare(this);
        return line;
    }

    private static final String ATTR_TEXT_KEY = "title_note_text", ATTR_HIDDEN_KEY = "title_note_hidden";

    private android.content.SharedPreferences notePrefs() {
        return getSharedPreferences(Margy.PREFS, MODE_PRIVATE);
    }

    /** The small note next to the title, or null when it is hidden. */
    private String attributionText() {
        try {
            android.content.SharedPreferences prefs = notePrefs();
            if (prefs.getBoolean(ATTR_HIDDEN_KEY, false)) return null;
            String own = prefs.getString(ATTR_TEXT_KEY, null);
            return own != null && own.trim().length() > 0 ? own.trim() : Text.ATTR_DEFAULT;
        } catch (Throwable error) {
            return Text.ATTR_DEFAULT;
        }
    }

    /** What happens when the title is held: edit the note, hide or show it, or put the default back. */
    private void attributionMenu() {
        final boolean hidden;
        boolean custom;
        try {
            android.content.SharedPreferences prefs = notePrefs();
            hidden = prefs.getBoolean(ATTR_HIDDEN_KEY, false);
            custom = prefs.getString(ATTR_TEXT_KEY, null) != null;
        } catch (Throwable error) {
            return;
        }
        Panel panel = Panel.with(this, skin, Text.ATTR_MENU).compact();
        panel.item("edit", Text.ATTR_EDIT, null, this::attributionEdit);
        panel.item(hidden ? "visibility" : "visibility_off", hidden ? Text.ATTR_SHOW : Text.ATTR_HIDE, null, () -> {
            notePrefs().edit().putBoolean(ATTR_HIDDEN_KEY, !hidden).apply();
            rebuild();
        });
        if (custom || hidden) panel.item("restore", Text.ATTR_RESET, null, () -> {
            notePrefs().edit().remove(ATTR_TEXT_KEY).remove(ATTR_HIDDEN_KEY).apply();
            rebuild();
        });
        panel.quiet(Text.CANCEL, null);
        panel.show();
    }

    private void attributionEdit() {
        final Panel panel = Panel.with(this, skin, Text.ATTR_EDIT).compact();
        final EditText input = panel.field(Text.ATTR_HINT);
        String now = attributionText();
        input.setText(now != null ? now : "");
        input.setSelection(input.getText().length());
        panel.primaryKeepOpen(Text.SAVE, () -> {
            String typed = input.getText().toString().trim();
            android.content.SharedPreferences.Editor edit = notePrefs().edit();
            if (typed.length() == 0) edit.putBoolean(ATTR_HIDDEN_KEY, true).remove(ATTR_TEXT_KEY);
            else edit.putBoolean(ATTR_HIDDEN_KEY, false).putString(ATTR_TEXT_KEY, typed);
            edit.apply();
            panel.close();
            rebuild();
        });
        panel.quiet(Text.CANCEL, null);
        panel.show();
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
        row.setPadding(dp(20), dp(4), dp(8), dp(4));
        GradientDrawable background = new GradientDrawable();
        background.setColor(skin.card);
        background.setCornerRadius(dp(28));
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

        TextView go = label(searchQuery.length() == 0 ? Text.SEARCH : Text.SEARCH_CLEAR);
        go.setTextColor(accentInk());
        go.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        go.setGravity(Gravity.CENTER);
        go.setPadding(dp(16), 0, dp(16), 0);
        go.setBackground(Expressive.tile(this, accentTonal(), (Accent.colour() & 0xFFFFFF) | 0x33000000,
                dp(20), dp(20), dp(10)));
        go.setOnClickListener(v -> {
            if (searchQuery.length() > 0) runSearch("");
            else runSearch(input.getText().toString());
        });
        row.addView(go, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)));
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(12), dp(4), dp(12), dp(8));
        box.addView(sized(row, 56));
        return box;
    }

    private void runSearch(String words) {
        searchQuery = words == null ? "" : words.trim().toLowerCase(Locale.ROOT);
        rebuild();
    }

    private void verifyProfile() { verifyProfile(false); }
    private void verifyProfile(boolean manual) {
        final String uid = Account.id();
        if (uid == null || !uid.matches("[0-9]{1,24}")) {
            Toast.makeText(this, "Сначала войдите в TikTok и откройте свой профиль", Toast.LENGTH_LONG).show();
            return;
        }
        final CloudProfileProvider sync = new CloudProfileProvider(this);
        if (sync.verified(uid) && !BioProof.pending(uid)) {
            Toast.makeText(this, "Профиль уже подтверждён", Toast.LENGTH_SHORT).show();
            return;
        }
        final String code;
        try { code = sync.code(uid); } catch (Throwable error) {
            Toast.makeText(this, "Не удалось подготовить проверку профиля", Toast.LENGTH_LONG).show();
            return;
        }
        final String known = Account.profileHandle();
        final boolean haveHandle = known != null && known.matches("[A-Za-z0-9_.]{2,24}");

        if (!manual && haveHandle && NativeBio.available()) {
            Panel automatic=Panel.with(this,skin,"Подтвердить @"+known);
            automatic.text("Код появится в описании на время проверки. Затем мод вернёт прежний текст.");
            if (BioProof.pending(uid)) {
                automatic.text(BioProof.status());
                automatic.keepOpen("Скопировать прежнее описание",() -> copy("Описание",BioProof.original(uid)));
            }
            automatic.primary("Подтвердить",BioProof::start);
            automatic.quiet("Вставить код вручную",() -> verifyProfile(true));
            automatic.quiet(Text.CANCEL,null);
            automatic.show();return;
        }
        final Panel panel = Panel.with(this, skin, "Подтверждение профиля");
        panel.text(haveHandle
                ? "@" + known + "\n1. Откройте профиль и вставьте код в описание.\n2. Сохраните и вернитесь — проверка начнётся сама."
                : "Вставьте код в описание TikTok и сохраните. Затем укажите свой @ник.");

        if (BioProof.pending(uid)) {
            panel.text(BioProof.status());
            panel.keepOpen("Скопировать прежнее описание",() -> copy("Описание",BioProof.original(uid)));
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
            Toast.makeText(this, "Код скопирован", Toast.LENGTH_SHORT).show();
        });
        panel.view(box);
        panel.text("После подтверждения код можно удалить. Повторная проверка не нужна.");

        final EditText handle = haveHandle ? null : panel.field("Ваш @ник в TikTok");
        final Panel.Click check = () -> {
            final String name = haveHandle ? known
                    : handleFrom(handle.getText().toString());
            if (name == null) {
                Toast.makeText(this, "Введите ник TikTok без ссылки", Toast.LENGTH_LONG).show();
                return;
            }
            panel.close();
            Toast.makeText(this, "Проверяю профиль…", Toast.LENGTH_SHORT).show();
            Net.away("profile verify", () -> {
                boolean verified = false;
                try { verified = sync.verify(uid, name); } catch (Throwable ignored) { }
                final boolean okay = verified;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (okay && uid.equals(Account.id())) ProfileStyle.share(true);
                    Toast.makeText(this, okay ? "Профиль подтверждён. Оформление отправляется."
                            : "Не нашли код в описании. Проверьте ник и код; TikTok иногда обновляет описание с задержкой, дальше проверю сам.",
                            Toast.LENGTH_LONG).show();
                    if (!okay) TtcuzProfileSync.autoVerifyOwn(true);
                    rebuild();
                });
            });
        };
        if (haveHandle) {
            // One tap: copy, open the profile to paste it, and the check runs on return.
            panel.keepOpen("Вставить код вручную", () -> {
                copy("ttcuz", code);
                verifyWaiting = true;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.tiktok.com/@" + known)));
                } catch (Throwable error) {
                    Toast.makeText(this, "Код скопирован. Вставьте его в описание TikTok", Toast.LENGTH_LONG).show();
                }
            });
            panel.keepOpen("Код уже в описании, проверить", check);
        } else {
            panel.primaryKeepOpen("Проверить", check);
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
        for(int i=0;i<4;i++)found+=searchControl(results,Text.RAIL,railNames[i],FeedRail.shown(i));
        if(matchesSearch(Text.HIDDEN_ITEMS)){
            results.addView(actionRow("visibility_off",Text.HIDDEN_ITEMS,Text.HIDDEN_ITEMS_NOTE,this::showHiddenItems));found++;
        }
        found+=searchControl(results,Text.DOWNLOADS,Text.NO_WATERMARK,Download.isEnabled());
        found+=searchControl(results,Text.VIDEO,Text.ALWAYS_DATE,Dates.isEnabled());
        found+=searchControl(results,Text.COMMENTS,Text.COMMENT_COPY_ON,Comments.isCopyEnabled());
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
        found += searchHit(results, Text.COMMENTS, Text.SEARCH_COMMENTS, () -> { });
        found += searchHit(results, Text.DOWNLOADS, Text.SEARCH_DOWNLOADS, () -> { });
        found += searchHit(results, Text.BACKUP, Text.SEARCH_BACKUP, () -> { });
        found += searchHit(results, Text.TEXTURES, Text.SEARCH_TEXTURES, () -> { });
        found += searchHit(results, Text.FONT, Text.SEARCH_FONT,
                () -> { fontOpen = true; });
        found += searchHit(results, Text.PROFILE_STYLE, "значок ник градиент синхронизация профиль", () -> { });
        }
        if (found == 0) results.addView(caption(Text.SEARCH_NONE));
        column.addView(wrap(results));
    }

    private int searchControl(LinearLayout into,String section,String title,boolean on){
        if(!matchesSearch(title))return 0;if(into.getChildCount()>0)into.addView(line());
        into.addView(actionRow("search",title,on?"Включено":"Выключено",()->{searchQuery="";settingsGroup=groupOf(section);rebuild();page.post(()->{View target=settingTargets.get(title);if(target==null)return;int offset=0;View cursor=target;while(cursor!=column){offset+=cursor.getTop();if(!(cursor.getParent() instanceof View))break;cursor=(View)cursor.getParent();}page.smoothScrollTo(0,Math.max(0,offset-dp(16)));Motion.change(target);});}));return 1;
    }
    private int searchHit(LinearLayout into, String title, String words, final Runnable open) {
        if (!matchesSearch(title + " " + words)) return 0;
        if (into.getChildCount() > 0) into.addView(line());
        into.addView(actionRow("search", title, currentValue(title), () -> {
            open.run();
            searchQuery = "";
            settingsGroup = groupOf(title);
            page.scrollTo(0, 0);
            rebuild();
            final View target = sectionTargets.get(title);
            if (target != null) page.post(() -> page.smoothScrollTo(0, target.getTop()));
        }));
        return 1;
    }

    private String currentValue(String title){
        if(title.equals(Text.ACCENT))return String.format(Locale.US,"#%06X",Accent.colour()&0xFFFFFF);
        if(title.equals(Text.THEME))return Themes.isEnabled()?"Включена":"Выключена";
        if(title.equals(Text.FEED))return Feed.isEnabled()?"Реклама скрыта":"Реклама разрешена";
        if(title.equals(Text.DOWNLOADS))return Download.isEnabled()?"Без водяного знака":"Обычное сохранение";
        if(title.equals(Text.PROFILE_STYLE))return Account.profileHandle()==null?"Откройте свой профиль":"@"+Account.profileHandle();
        return "Открыть настройки";
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
        view.setPadding(dp(28), dp(24), dp(16), dp(8));
        sectionTargets.put(text, view);
        return view;
    }

    private String groupName(int group) {
        switch (group) {
            case 1: return "Оформление";
            case 2: return "Профиль";
            case 3: return "Лента и видео";
            case 4: return "Инструменты";
            case 5: return "О проекте";
            case 6: return "Плагины";
            case 7: return "Друзья и чаты";
            default: return "ttcuz";
        }
    }

    private void showSectionHelp() {
        String[] notes = {"", "Акцент меняет цвет кнопок и вкладок. Готовые картинки могут сохранять свой цвет. Тема, шрифты и текстуры настраиваются отдельно.",
                "Стиль ника и личный значок видны в превью. Для публикации значка подтвердите аккаунт. Выдавать значки другим может администратор.",
                "Глаз меняет прозрачность кнопок поверх видео; «Защита экрана» находится ниже «Комментариев». Удержание «Отправить» открывает скачивание и перемотку. Удерживайте видео: вниз закрепить скорость, вверх отменить выбор; после фиксации вниз — снять её.",
                "Скачивание, резервные копии, диагностика и журнал. Сохраните копию перед экспериментами.",
                "Здесь находятся версия мода, обновления, журнал и участники.", "Магазин, установка и настройки плагинов. Плагины запускают дополнительный код; устанавливайте только те, которым доверяете.",
                "Закреплённые друзья, автопродление серий и уведомления мода."};
        Panel.with(this,skin,"Подсказки").text(notes[Math.max(0,Math.min(notes.length-1,settingsGroup))])
                .quiet("Обучение",() -> Tutorial.show(this)).primary("Понятно",null).show();
    }

    /** The shelves, in the order of the main list, with the icon each one has there. */
    private static final int[] SHELVES = {1, 2, 3, 7, 4, 6, 5};
    private static final String[] SHELF_ICONS = {"palette", "group", "play_circle", "tune", "info", "extension", "chat"};

    private View groupNavigation() {
        LinearLayout list = card();
        String[] notes = {"Акцент, тема, шрифты и текстуры",
                "Ник, значки и аккаунт",
                "Просмотр, кнопки, защита экрана и регион",
                "Скачивание, копии и диагностика",
                "Версия, обновления и участники", "Магазин и установленные",
                "Друзья, серии и уведомления"};
        // Shelf order on screen: what people touch daily first, the project itself last.
        for (int n = 0; n < SHELVES.length; n++) {
            final int i = SHELVES[n];
            final int chosen = i;
            if (n > 0) list.addView(line());
            LinearLayout entry = row();
            entry.setPadding(dp(16),dp(14),dp(20),dp(14));
            entry.addView(icon(SHELF_ICONS[i-1],false,52));
            LinearLayout words = new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL);
            TextView head = label(groupName(i)); head.setTextSize(19);
            head.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
            words.addView(head);entry.setContentDescription(groupName(i)+". "+notes[i-1]);
            entry.addView(words,grow()); entry.addView(away());
            entry.setMinimumHeight(dp(68));Motion.touch(entry);
            entry.setOnClickListener(v -> {
                if(navigating)return;navigating=true;
                Motion.navigate(column,1f,()->{settingsGroup=chosen;page.scrollTo(0,0);rebuild();navigating=false;});
            });
            list.addView(entry);
        }
        return wrap(list);
    }

    /**
     * Which shelf a section lives on.
     * 1 look, 2 profile, 3 feed/video, 4 tools, 5 about, 6 plugins, 7 chats.
     */
    private int groupOf(String section) {
        if (section.equals(Text.PLUGINS)) return 6;
        if (section.equals(Text.PROFILE_STYLE) || section.equals(Text.PROFILE_LOOK) || section.equals(Text.PROFILE_SYNC) || section.equals(Text.ACCOUNT)) return 2;
        if (section.equals(Text.APPEARANCE) || section.equals(Text.ACCENT)
                || section.equals(Text.THEME) || section.equals(Text.FONT)
                || section.equals(Text.TEXTURES)) return 1;
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
        for (int i = filterFrom; i < column.getChildCount(); i++) {
            View item = column.getChildAt(i);
            if (item instanceof TextView && sectionTargets.containsValue(item))
                current = groupOf(((TextView) item).getText().toString());
            item.setVisibility(current == settingsGroup ? View.VISIBLE : View.GONE);
        }
    }

    private View pinHolder, pinEnd;
    private boolean pinListening;

    /** Keeps the profile preview in view while the page scrolls, until the last look setting passes. */
    private void pinPreview() {
        View holder = pinHolder, end = pinEnd;
        if (holder == null || page == null || column == null || holder.getParent() != column) return;
        if (!pinListening) {
            pinListening = true;
            page.getViewTreeObserver().addOnScrollChangedListener(this::pinPreview);
        }
        int top = holder.getTop();
        int last = end != null && end.getParent() == column ? end.getBottom() : column.getHeight();
        float shift = Math.max(0, Math.min(last - top - holder.getHeight(), page.getScrollY() - top + dp(4)));
        holder.setTranslationY(shift);
        holder.setTranslationZ(shift > 0 ? dp(6) : 0);
    }

    private boolean navigating;
    private void goBack() {
        if(navigating)return;
        if (searchQuery.length() > 0) { searchQuery = ""; rebuild(); }
        else if (settingsGroup != 0) { navigating=true;Motion.navigate(column,-1f,()->{settingsGroup=0;rebuild();navigating=false;}); }
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
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        try {
            if (column != null && !navigating && !collapsing) {
                int width = column.getWidth(), edge = dp(56), slop = android.view.ViewConfiguration.get(this).getScaledTouchSlop();
                switch (ev.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        swipeTracking = ev.getX() < edge && width > 0; swiping = false; swipeDx = 0;
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
                                column.animate().translationX(width).alpha(.4f).setDuration(Motion.enabled(this) ? 180 : 0)
                                    .withEndAction(() -> {
                                        if (searchQuery.length() > 0) { searchQuery = ""; rebuild(); navigating = false; }
                                        else if (settingsGroup != 0) { settingsGroup = 0; rebuild(); navigating = false; }
                                        else finish();
                                    }).start();
                            } else {
                                column.animate().translationX(0f).alpha(1f).setDuration(Motion.enabled(this) ? 200 : 0).start();
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
        return super.dispatchTouchEvent(ev);
    }

    @Override protected void onDestroy() {
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

    private View caption(String message) {
        TextView view = new TextView(this);
        view.setText(message);
        view.setTextColor(skin.muted());
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        view.setPadding(dp(16), dp(12), dp(16), dp(4));
        return view;
    }

    /**
     * A segmented list in Material 3 Expressive: every row is a tile of its own,
     * with large outer corners, small inner ones and a thin gap between them.
     */
    private LinearLayout card() {
        return new Expressive.Group(this, skin.card, (Accent.colour() & 0xFFFFFF) | 0x33000000);
    }

    /** A lone row is a one-tile list, so it looks like every other group. */
    private View wrap(View card) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), 0, dp(12), dp(6));
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

    private View line() {
        View spacer=new View(this);spacer.setVisibility(View.GONE);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0,0));return spacer;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(4), dp(20), dp(4));
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
        return view;
    }

    private TextView detail(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(skin.muted());
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        view.setIncludeFontPadding(false);
        return view;
    }

    private LinearLayout.LayoutParams grow() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private View sized(View view, int height) {
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
            colourAnimation=android.animation.ValueAnimator.ofFloat(0,1);colourAnimation.setDuration(220L);
            colourAnimation.addUpdateListener(a->{customProgress=(Float)a.getAnimatedValue();invalidate();});
            colourAnimation.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){fill.setShader(null);fill.setColor(colour);customProgress=0;colourAnimation=null;invalidate();}});
            colourAnimation.start();
        }
        void choose(boolean selected, boolean animate) {
            chosen=selected; setSelected(selected);
            if (animation!=null) {animation.cancel();animation=null;}
            if (!animate || !Motion.enabled(getContext())) {selection=chosen?1f:0f;invalidate();return;}
            animation=android.animation.ValueAnimator.ofFloat(selection,chosen?1f:0f);
            animation.setDuration(180L);
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
}
