package cat.ttcuz.framework;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.ss.android.ugc.aweme.im.common.model.StickerItem;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class FavouriteStickers {
    private static String account;
    private static Method convert;
    private static int generation;
    private static long lastLoad;
    private static long lastRead;
    private static Object repository;
    private static Method setFlow;
    private static final Map<Long, List<?>> sourceLists = new LinkedHashMap();
    private static final Map<Long, List<StickerItem>> setItems = new LinkedHashMap();

    FavouriteStickers() {
    }

    static synchronized boolean read() {
        synchronized (FavouriteStickers.class) {
            String liveId = Account.liveId();
            if (liveId == null) {
                return false;
            }
            if (!liveId.equals(account)) {
                account = liveId;
                repository = null;
                lastLoad = 0L;
                lastRead = 0L;
                setFlow = null;
                sourceLists.clear();
                setItems.clear();
                generation++;
                Streaks.clearStickers();
            }
            long uptimeMillis = SystemClock.uptimeMillis();
            if (uptimeMillis - lastRead < 300) {
                return false;
            }
            lastRead = uptimeMillis;
            try {
                if (repository == null) {
                    // 47.7.3: LLLLZI is the sticker repository (LLLLZ, its neighbour, is another service)
                    repository = Class.forName("X.03AE").getMethod("LLLLZI").invoke(null);
                }
                return readRepository(repository);
            } catch (Throwable unused) {
                return false;
            }
        }
    }

    private static List<?> items(Object obj) {
        Object obj2 = NativeRead.get(obj, "getValue");
        if (!(obj2 instanceof List)) {
            obj2 = NativeRead.get(obj2, "LIZ");
        }
        if (obj2 instanceof List) {
            return (List) obj2;
        }
        return null;
    }

    private static List<?> addedSets(Object obj) {
        return items(NativeRead.get(obj, "getAddedStickerSetsFlow"));
    }

    private static Long unlockedSet(Object obj) {
        if (Boolean.TRUE.equals(NativeRead.field(obj, "shouldLockSticker"))) {
            return null;
        }
        Object field = NativeRead.field(obj, "setId");
        if (!(field instanceof Long)) {
            return null;
        }
        Long l = (Long) field;
        if (l.longValue() > 0) {
            return l;
        }
        return null;
    }

    /**
     * Reads the favourites and the stickers of every unlocked added set from TikTok's sticker repository, as the
     * earlier build did (rewritten from its bytecode: the decompiler could not). True when anything changed.
     */
    static synchronized boolean readRepository(Object repo) throws Exception {
        if (repo == null) return false;
        List<?> favourites = items(NativeRead.get(repo, "getFavouriteStickersFlow"));
        boolean changed = favourites != null && Streaks.replaceFavourites(favourites);
        List<?> sets = addedSets(repo);
        if (sets == null) return changed;
        Set<Long> present = new HashSet<>();
        boolean setsChanged = false;
        for (Object set : sets) {
            Long id = unlockedSet(set);
            if (id == null) continue;
            present.add(id);
            if (setFlow == null || !setFlow.getDeclaringClass().isInstance(repo)) {
                setFlow = repo.getClass().getMethod("getSetStickersFlow", long.class);
                setFlow.setAccessible(true);
            }
            List<?> source = items(setFlow.invoke(repo, id));
            if (source == null || sourceLists.get(id) == source) continue;
            List<StickerItem> converted = new ArrayList<>();
            for (Object sticker : source) {
                if (sticker == null) continue;
                if (convert == null) {
                    Class<?> setSticker = Class.forName("com.ss.android.ugc.aweme.im.common.model.SetSticker");
                    convert = Class.forName("X.0MUG").getMethod("LJFF", setSticker, Long.class);
                }
                if (!convert.getParameterTypes()[0].isInstance(sticker)) continue;
                Object item = convert.invoke(null, sticker, null);
                if (item instanceof StickerItem) converted.add((StickerItem) item);
            }
            sourceLists.put(id, source);
            setItems.put(id, converted);
            setsChanged = true;
        }
        boolean dropped = sourceLists.keySet().retainAll(present);
        setItems.keySet().retainAll(present);
        if (!setsChanged && !dropped) return changed;
        List<StickerItem> all = new ArrayList<>();
        for (List<StickerItem> list : setItems.values()) all.addAll(list);
        return Streaks.replaceSetStickers(all) || changed;
    }

    private static void refresh(Object obj, String str) {
        try {
            Class<?> cls = obj.getClass();
            Class<?> cls2 = Boolean.TYPE;
            Method method = cls.getMethod(str, cls2, cls2, String.class);
            method.setAccessible(true);
            method.invoke(obj, false, false, "ttcuz_settings");
        } catch (ReflectiveOperationException e) {
            Diary.note("sticker library " + str + ": " + e.getClass().getSimpleName());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void requestSets(Object obj, Set<Long> set) {
        List<?> addedSets = addedSets(obj);
        if (addedSets == null) {
            return;
        }
        ArrayList arrayList = new ArrayList();
        Iterator<?> it = addedSets.iterator();
        while (it.hasNext()) {
            Long unlockedSet = unlockedSet(it.next());
            if (unlockedSet != null && set.add(unlockedSet)) {
                arrayList.add(unlockedSet);
            }
        }
        if (arrayList.isEmpty()) {
            return;
        }
        try {
            Method method = obj.getClass().getMethod("refreshSetStickers", List.class, String.class);
            method.setAccessible(true);
            method.invoke(obj, arrayList, "ttcuz_settings");
        } catch (ReflectiveOperationException e) {
            Diary.note("sticker set load: " + e.getClass().getSimpleName());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static synchronized void load(final Runnable runnable) {
        synchronized (FavouriteStickers.class) {
            read();
            if (repository == null) {
                return;
            }
            long uptimeMillis = SystemClock.uptimeMillis();
            if (uptimeMillis - lastLoad < 30000) {
                return;
            }
            lastLoad = uptimeMillis;
            final int i = generation;
            final Object obj = repository;
            refresh(obj, "refreshFavouriteStickers");
            refresh(obj, "refreshAddedStickerSets");
            final HashSet hashSet = new HashSet();
            requestSets(obj, hashSet);
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() { // from class: cat.ttcuz.framework.FavouriteStickers.1
                int tries;

                @Override // java.lang.Runnable
                public void run() {
                    Runnable runnable2;
                    if (i == FavouriteStickers.generation && Objects.equals(FavouriteStickers.account, Account.liveId())) {
                        try {
                            FavouriteStickers.requestSets(obj, hashSet);
                            if (FavouriteStickers.readRepository(obj) && (runnable2 = runnable) != null) {
                                runnable2.run();
                            }
                        } catch (Throwable unused) {
                        }
                        int i2 = this.tries + 1;
                        this.tries = i2;
                        if (i2 < 16) {
                            handler.postDelayed(this, 500L);
                        }
                    }
                }
            }, 500L);
        }
    }
}
