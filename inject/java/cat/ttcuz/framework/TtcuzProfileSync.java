package cat.ttcuz.framework;

import java.util.LinkedHashMap;
import java.util.Map;
import java.io.File;

/** Opt-in profile metadata transport. A verified HTTPS service supplies Provider. */
public final class TtcuzProfileSync {
    private TtcuzProfileSync() {}

    public interface Provider {
        String badgeHost();
        Metadata fetch(String profileId) throws Exception;
        boolean publish(Metadata metadata, File badge) throws Exception;
        /** Accounts that have a profile, with when each changed; null when the service cannot say. */
        default Map<String, Long> directory() throws Exception { return null; }
        default boolean unpublish(String profileId) throws Exception {
            return publish(new Metadata(profileId, null, 0, 0,
                    System.currentTimeMillis()), null);
        }
    }

    public static final class Metadata {
        public final String profileId;
        public final String badgeUrl;
        public final int firstColour;
        public final int lastColour;
        public final long updatedAt;
        public final int middleColour;
        public final boolean threeColours;
        public final int direction;
        public final boolean bold;
        public final boolean glow;
        public final boolean animated;
        public final int speed;
        public String displayName="";
        public int gradientWidth=100,gradientStop=50,gradientMotion=0;
        /** the nickname's entrance, what floats around it, the colour of the person's comments (0: TikTok's) */
        public int appearEffect=0,particles=0,commentColour=0;
        /** The person's own particle effect (ParticleRecipe JSON) when particles is 5; "" otherwise. */
        public String particleRecipe="";
        /** a tag next to the nickname, held on the server so nobody else can take it */
        public String tag="";
        public int[] gradientColours;
        public int offsetX;
        public int offsetY;
        public int badgeSize = 100;
        public int crownOffsetX;
        public int crownOffsetY;
        public int profileLayout;
        /** The nickname font: a system family name or "file" with the SHA-256 of the uploaded font. */
        public String nicknameFont = "", nicknameFontHash = "";
        /** How strong the nickname glow is, 0..100. */
        public int glowStrength = 50;

        public Metadata(String profileId, String badgeUrl, int firstColour,
                        int lastColour, long updatedAt) {
            this(profileId, badgeUrl, firstColour, lastColour, updatedAt,
                    0, false, 0, false, false, false, 50);
        }

        public Metadata(String profileId, String badgeUrl, int firstColour,
                        int lastColour, long updatedAt, int middleColour,
                        boolean threeColours, int direction, boolean bold,
                        boolean glow, boolean animated, int speed) {
            this.profileId = profileId;
            this.badgeUrl = badgeUrl;
            this.firstColour = firstColour;
            this.lastColour = lastColour;
            this.updatedAt = updatedAt;
            this.middleColour = middleColour;
            this.threeColours = threeColours;
            this.direction = Math.max(0, Math.min(3, direction));
            this.bold = bold;
            this.glow = glow;
            this.animated = animated;
            this.speed = Math.max(0, Math.min(100, speed));
        }

        public boolean valid(String badgeHost) {
            if(gradientColours!=null && (gradientColours.length<2 || gradientColours.length>ProfileStyle.MAX_COLOURS))return false;
            if (profileId == null || !Ids.uid(profileId)) return false;
            if(gradientWidth<50 || gradientWidth>200 || gradientStop<5 || gradientStop>95 || gradientMotion<0 || gradientMotion>4)return false;
            if(displayName==null || displayName.length()>80 || displayName.matches("(?s).*[\\x00-\\x1f\\x7f].*"))return false;
            if(profileLayout<0 || profileLayout>2)return false;
            if(appearEffect<0 || appearEffect>4 || particles<0 || particles>5 || particleRecipe==null || particleRecipe.length()>ParticleRecipe.MOST_IMAGE+600 || tag==null || tag.length()>16)return false;
            if(glowStrength<0 || glowStrength>100)return false;
            if(nicknameFont==null || nicknameFontHash==null || !NicknameFont.validChoice(nicknameFont))return false;
            if(!nicknameFontHash.isEmpty() && !Ids.sha256(nicknameFontHash))return false;
            if(Fonts.FILE.equals(nicknameFont) && nicknameFontHash.isEmpty())return false;
            if (badgeUrl != null && badgeUrl.length() > 0) {
                if (badgeUrl.length() > 512 || !badgeUrl.startsWith("https://"))
                    return false;
                try {
                    java.net.URI uri = new java.net.URI(badgeUrl);
                    String host = uri.getHost();
                    boolean ours = "ttcuz.ttcuz1337.workers.dev".equalsIgnoreCase(host);
                    if (badgeHost == null || !(badgeHost.equalsIgnoreCase(host) || ours)
                            || uri.getUserInfo() != null) return false;
                } catch (Throwable error) { return false; }
            }
            return direction >= 0 && direction <= 3 && speed >= 0 && speed <= 100
                    && updatedAt > 0 && updatedAt <= System.currentTimeMillis() + 300000L;
        }
    }

    private static final long TTL = 5 * 60 * 1000L;
    private static final int MAX = 256;
    private static volatile Provider provider;
    private static volatile String syncStatus = Text.t("Изменения ещё не отправлялись");
    private static volatile String autoVerifyUid;
    private static volatile long autoVerifyAt;
    public enum State { IDLE, WAITING, SENDING, SENT, ERROR, VERIFYING, HELD, PRIVATE }
    private static volatile State syncState=State.IDLE;
    public static State state(){return syncState;}
    public static String status() { return syncStatus; }
    private static void status(State state,String message){syncStatus=message;syncState=state;}
    public static boolean canRetryOwn(){return ProfileStyle.sharing() && provider!=null && Account.id()!=null && sendGate.canRetry(System.currentTimeMillis());}
    public static boolean retryOwn() {
        if(!ProfileStyle.sharing() || provider==null || Account.id()==null
                || !sendGate.retry(System.currentTimeMillis()))return false;
        publishOwn();return true;
    }
    private static final Map<String, CacheEntry> cache =
            new LinkedHashMap<String, CacheEntry>(MAX, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> oldest) {
                    return size() > MAX;
                }
            };

    private static final class CacheEntry {
        final Metadata metadata;
        final long until;
        final long version;  // the directory's stamp for this account when it was read; -1 unknown
        CacheEntry(Metadata metadata, long until) { this(metadata, until, -1L); }
        CacheEntry(Metadata metadata, long until, long version) {
            this.metadata = metadata;
            this.until = until;
            this.version = version;
        }
    }

    // The directory: which accounts have a profile at all. Without it every account on screen was asked about
    // every half minute, which spent the server's whole daily allowance; with it only ttcuz users are asked,
    // and only after their profile changed.
    private static final long DIRECTORY_EVERY = 3 * 60 * 1000L;
    private static volatile Map<String, Long> directory;
    private static volatile long directoryUntil;
    private static final java.util.concurrent.atomic.AtomicBoolean directoryLoading =
            new java.util.concurrent.atomic.AtomicBoolean();

    private static void refreshDirectory(final Provider active) {
        long now = System.currentTimeMillis();
        if (now < directoryUntil || !directoryLoading.compareAndSet(false, true)) return;
        Net.away("profile directory", () -> {
            try {
                Map<String, Long> fresh = active.directory();
                long next = System.currentTimeMillis();
                if (fresh != null) { directory = fresh; directoryUntil = next + DIRECTORY_EVERY; }
                else directoryUntil = next + 10 * 60 * 1000L;
            } catch (Throwable error) {
                directoryUntil = System.currentTimeMillis() + 10 * 60 * 1000L;
                Diary.note("profile directory: " + error);
            } finally {
                directoryLoading.set(false);
            }
        });
    }

    public static void use(Provider next) {
        provider = next;
        sendGate.reset();++publishRevision;
        synchronized (cache) { cache.clear(); }
    }

    private static volatile long accountRevision;
    static synchronized void accountChanged(String uid) {
        CloudAppearance.ensure(uid);
        ++accountRevision;++publishRevision;
        sendGate.reset();localSignature=null;autoVerifyUid=null;autoVerifyAt=0;retries=0;
        status(State.IDLE,Text.t("Изменения ещё не отправлялись"));
    }

    public static boolean available() { return provider != null; }

    public static void start(android.content.Context context) {
        use(new CloudProfileProvider(context));
        refreshVerified();
        CloudAppearance.ensure(Account.liveId());
        autoVerifyOwn();
        if (ProfileStyle.sharing()) publishOwn();
    }

    /**
     * Publish the style, and keep checking the public TikTok bio in the
     * background for as long as a proof is actually wanted: for a custom badge,
     * for the creator's tools, or to take an id back from another device.
     * Colours and effects need none, so for them the bio is never read.
     * A failed attempt is cooled down and retried, with a ceiling, so a missing
     * code or TikTok throttling cannot turn into a request loop.
     */
    /** Asked of the server at most this often: the free tier counts every request. */
    private static final long CHECK_EVERY = 6 * 3600_000L;

    /** Once per start (and at most every six hours), the server's word on whether this account is verified here. */
    static void refreshVerified() {
        final Provider active = provider;
        final String uid = Account.liveId() != null ? Account.liveId() : Account.id();
        if (!(active instanceof CloudProfileProvider) || uid == null || !Ids.uid(uid)) return;
        final CloudProfileProvider cloud = (CloudProfileProvider) active;
        if (System.currentTimeMillis() - cloud.lastChecked(uid) < CHECK_EVERY) return;
        publishQueue.execute(() -> {
            try { cloud.refreshVerified(uid); Diary.note("profile sync: server says " + (cloud.verified(uid) ? "verified" : "not verified")); }
            catch (Throwable error) { Diary.note("profile sync: check failed: " + error); }
        });
    }

    public static void autoVerifyOwn() { autoVerifyOwn(false); }

    /** `force` skips the cool-down: the person just added the code. */
    public static void autoVerifyOwn(boolean force) {
        final Provider active = provider;
        if (!(active instanceof CloudProfileProvider) || !ProfileStyle.sharing()) return;
        final String uid = Account.id();
        final String handle = Account.profileHandle();
        if (uid == null || !Ids.uid(uid) || handle == null
                || !handle.matches("[A-Za-z0-9_.]{2,24}")) return;
        final CloudProfileProvider cloud = (CloudProfileProvider) active;
        if (cloud.verified(uid)) {
            publishOwn();
            return;
        }
        // Colours go out at once; this is what claims the id.
        publishOwn();
        boolean wanted = force || cloud.needsProof(uid) || ProfileStyle.hasBadge()
                || Badges.hasCreatorBadge(uid);
        if (!wanted) return;
        final long now = System.currentTimeMillis();
        synchronized (TtcuzProfileSync.class) {
            if (force) retries = 0;
            if (!force && uid.equals(autoVerifyUid) && now - autoVerifyAt < 10 * 60 * 1000L) return;
            autoVerifyUid = uid;
            autoVerifyAt = now;
        }
        final long account=accountRevision;
        publishQueue.execute(() -> {
            if (account!=accountRevision || !uid.equals(Account.liveId()) || !ProfileStyle.sharing()) return;
            try {
                status(State.VERIFYING,Text.t("Проверяю описание TikTok"));
                boolean verified=cloud.verify(uid,handle);
                if(account!=accountRevision || !uid.equals(Account.liveId()))return;
                if (verified) {
                    sendGate.reset();
                    status(State.WAITING,Text.t("Профиль подтверждён. Отправляю оформление"));
                    publishOwn();
                } else {
                    status(State.HELD,Text.t("Код в описании TikTok пока не найден, проверю ещё раз"));
                    retryVerification(uid);
                }
            } catch (Throwable error) {
                if(account!=accountRevision)return;
                status(State.ERROR,Text.t("Проверка временно недоступна; повторю автоматически"));
                Diary.note("profile sync: automatic verification failed: " + error);
                retryVerification(uid);
            }
        });
    }

    private static int retries;
    private static final int MAX_RETRIES = 18;  // every ten minutes: three hours

    private static void retryVerification(final String uid) {
        synchronized (TtcuzProfileSync.class) {
            if (++retries > MAX_RETRIES) {
                status(State.HELD,Text.t("Код так и не найден. Добавьте его в описание и нажмите «Проверить»"));
                return;
            }
        }
        final long account=accountRevision;
        publishQueue.schedule(() -> {
            if (account==accountRevision && uid.equals(Account.liveId()) && ProfileStyle.sharing()) autoVerifyOwn();
        }, 10, java.util.concurrent.TimeUnit.MINUTES);
    }

    /** The drawing path never waits for network data. */
    public static Metadata cached(String uid) {
        if (uid == null) return null;
        CacheEntry entry;
        long now = System.currentTimeMillis();
        synchronized (cache) {
            entry = cache.get(uid);
        }
        if (entry == null) return null;
        // Keep the last confirmed style visible while refreshing it. Dropping
        // an expired entry before the request completed made profile marks
        // blink off on slow connections or while TikTok was throttling reads.
        if (entry.until <= now) request(uid);
        return entry.metadata;
    }

    public static void request(final String uid) {
        final Provider active = provider;
        if (active == null || uid == null || !Ids.uid(uid)) return;
        final long now = System.currentTimeMillis();
        refreshDirectory(active);
        final Map<String, Long> known = directory;
        final boolean own = uid.equals(Account.liveId());
        Metadata stale = null;
        synchronized (cache) {
            CacheEntry existing = cache.get(uid);
            if (existing != null && existing.until > now) return;
            if (known != null && !own) {
                Long stamp = known.get(uid);
                long until = Math.max(directoryUntil, now + 60000L);
                // not a ttcuz user: nothing to ask until the directory says otherwise
                if (stamp == null) { cache.put(uid, new CacheEntry(null, until)); return; }
                // a ttcuz user whose profile has not changed since it was read
                if (existing != null && existing.metadata != null && existing.version == stamp) {
                    cache.put(uid, new CacheEntry(existing.metadata, until, stamp));
                    return;
                }
            }
            if (existing != null) stale = existing.metadata;
            // A short in-flight/negative cache prevents repeated requests while
            // a feed binds, but preserves a confirmed style during refresh.
            cache.put(uid, new CacheEntry(stale, now + 30000L));
        }
        Net.away("profile fetch", () -> {
            try {
                Metadata result = active.fetch(uid);
                if (result != null && (!result.valid(active.badgeHost())
                        || !uid.equals(result.profileId)))
                    result = null;
                long stamp = -1L;
                if (known != null && known.get(uid) != null) stamp = known.get(uid);
                long done = System.currentTimeMillis();
                long until = known != null ? Math.max(directoryUntil, done + 60000L)
                        : done + (result == null ? 10 * 60 * 1000L : TTL);
                synchronized (cache) {
                    cache.put(uid, new CacheEntry(result, until, stamp));
                }
                Badge.refreshProfiles();
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> { ProfileLayout.reload(); });
                ProfilePreview.accountUpdated();
            } catch (Throwable error) {
                Diary.note("profile sync fetch: " + error);
            }
        });
    }

    private static final SyncGate sendGate=new SyncGate();
    private static volatile String localSignature;
    public static void publishOwn() {
        final Provider active = provider;
        final String uid = Account.liveId();
        if (uid == null) return;
        if (provider instanceof CloudProfileProvider && !CloudAppearance.ready(uid)) return;
        final int nickname = AppearanceColors.get(AppearanceColors.NICKNAME);
        final Metadata own = new Metadata(uid, null,
                ProfileStyle.gradient() ? ProfileStyle.start() : nickname,
                ProfileStyle.gradient() ? ProfileStyle.end() : nickname,
                System.currentTimeMillis(), ProfileStyle.middle(),
                ProfileStyle.gradient() && ProfileStyle.threeColours(), ProfileStyle.direction(), ProfileStyle.bold(),
                ProfileStyle.glow(), ProfileStyle.gradient() && ProfileStyle.animated(), ProfileStyle.speed());
        if(ProfileStyle.gradient())own.gradientColours=ProfileStyle.gradientColours();
        own.displayName=ProfileStyle.sharedName();
        own.gradientWidth=ProfileStyle.gradientWidth();own.gradientStop=ProfileStyle.gradientStop();own.gradientMotion=ProfileStyle.motion();
        own.appearEffect=ProfileStyle.appear();own.particles=ProfileStyle.particles();own.particleRecipe=own.particles==ProfileStyle.CUSTOM?ProfileStyle.particleRecipe():"";own.commentColour=ProfileStyle.commentColour();own.tag=ProfileStyle.tag();
        own.offsetX = ProfileStyle.offsetX(); own.offsetY = ProfileStyle.offsetY();
        own.badgeSize = ProfileStyle.badgeSize();
        own.crownOffsetX = ProfileStyle.crownOffsetX();
        own.crownOffsetY = ProfileStyle.crownOffsetY();
        own.profileLayout = ProfileLayout.mode();
        own.nicknameFont = NicknameFont.chosen();
        own.nicknameFontHash = Fonts.FILE.equals(own.nicknameFont) ? NicknameFont.hash() : "";
        own.glowStrength = ProfileStyle.glowStrength();
        final File badge=ProfileStyle.badgeFile();
        final String signature=uid+":"+own.firstColour+":"+own.lastColour+":"+own.middleColour+":"+own.threeColours
                +":"+own.direction+":"+own.bold+":"+own.glow+":"+own.animated+":"+own.speed+":"+own.offsetX
                +":"+own.offsetY+":"+own.badgeSize+":"+own.crownOffsetX+":"+own.crownOffsetY
                +":"+java.util.Arrays.toString(own.gradientColours)+":"+own.gradientWidth+":"+own.gradientStop+":"+own.gradientMotion+":"+own.appearEffect+":"+own.particles+":"+own.particleRecipe.hashCode()+":"+own.commentColour+":"+own.tag+":"+own.profileLayout+":"+own.nicknameFont+":"+own.nicknameFontHash+":"+own.glowStrength+":"+own.displayName.length()+":"+own.displayName+":"+(badge==null ? "none" : badge.lastModified()+":"+badge.length());
        if(!signature.equals(localSignature)) {
            localSignature=signature;Badge.refreshProfiles();ProfilePreview.accountUpdated();
        }
        if(active == null || !ProfileStyle.sharing())return;
        if(!sendGate.queue(signature))return;
        final long revision = ++publishRevision;
        status(State.WAITING,Text.t("Ожидает отправки"));
        publishQueue.schedule(() -> {
            if (revision != publishRevision || !ProfileStyle.sharing()
                    || !uid.equals(Account.liveId())) return;
            try {
                status(State.SENDING,Text.t("Отправляю оформление"));
                boolean okay=active.publish(own, badge);
                if(revision!=publishRevision || !uid.equals(Account.liveId()))return;
                sendGate.complete(signature,okay,System.currentTimeMillis());
                if (!okay) {
                    boolean taken = active instanceof CloudProfileProvider
                            && ((CloudProfileProvider) active).needsProof(uid);
                    status(taken ? State.HELD : State.ERROR, taken
                            ? Text.t("Этот профиль уже занят другим устройством: подтвердите его кодом в описании TikTok")
                            : Text.t("Не отправлено. Проверьте подключение; значок должен быть до 32 КБ"));
                    Diary.note("profile sync: publish rejected");
                } else {
                    boolean held=active instanceof CloudProfileProvider && ((CloudProfileProvider)active).badgeHeld(uid);
                    status(held ? State.HELD : State.SENT,held ? Text.t("Цвета отправлены. Свой значок появится после подтверждения") : Text.t("Оформление отправлено"));
                }
            } catch (Throwable error) {
                if(revision!=publishRevision || !uid.equals(Account.liveId()))return;
                sendGate.complete(signature,false,System.currentTimeMillis());
                status(State.ERROR,Text.t("Ошибка подключения. Повторная отправка временно отложена"));
                Diary.note("profile sync publish: " + error);
            }
        }, sendGate.delay(System.currentTimeMillis()), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /** Explicit privacy-off path; providers should delete the public record. */
    public static void unpublishOwn() {
        sendGate.reset();localSignature=null;
        status(State.PRIVATE,Text.t("Синхронизация выключена"));
        final Provider active = provider;
        final String uid = Account.id();
        if (active == null || uid == null) return;
        ++publishRevision;
        publishQueue.execute(() -> {
            try {
                if (!active.unpublish(uid)) {
                    status(State.ERROR,Text.t("Не удалось скрыть оформление на сервере. Проверьте подключение"));
                    Diary.note("profile sync: removal rejected");
                } else status(State.PRIVATE,Text.t("Публичное оформление скрыто"));
            } catch (Throwable error) {
                Diary.note("profile sync removal: " + error);
            }
        });
    }

    private static volatile long publishRevision;
    private static final java.util.concurrent.ScheduledExecutorService publishQueue =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(work -> {
                Thread thread = new Thread(work, "ttcuz-profile-sync");
                thread.setDaemon(true);
                return thread;
            });
}
