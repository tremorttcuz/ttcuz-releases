package cat.ttcuz.framework;

import android.os.Handler;
import android.os.Looper;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import com.ss.android.ugc.aweme.im.common.model.StickerItem;

/** TikTok 47.2.41 favourites and added sets, using the native panel's repository. */
final class FavouriteStickers {

    private static long lastRead, lastLoad;
    private static String account;
    private static Object repository;
    private static int generation;
    private static Method setFlow, convert;
    private static final Map<Long, List<?>> sourceLists = new LinkedHashMap<>();
    private static final Map<Long, List<StickerItem>> setItems = new LinkedHashMap<>();

    static synchronized boolean read() {
        String uid = Account.liveId();
        if (uid == null) return false;
        if (!uid.equals(account)) {
            account = uid;
            repository = null;
            lastRead = lastLoad = 0;
            setFlow = null;
            sourceLists.clear(); setItems.clear();
            generation++;
            Streaks.clearStickers();
        }
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastRead < 300) return false;
        lastRead = now;
        try {
            if (repository == null) {
                Method getter = Class.forName("X.1HIS").getMethod("LLLLLZIL");
                repository = getter.invoke(null);
            }
            return readRepository(repository);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static List<?> items(Object flow) {
        Object state = NativeRead.get(flow, "getValue");
        Object value = state instanceof List ? state : NativeRead.get(state, "LIZ");
        return value instanceof List ? (List<?>) value : null;
    }

    private static List<?> addedSets(Object repo) {
        return items(NativeRead.get(repo, "getAddedStickerSetsFlow"));
    }

    private static Long unlockedSet(Object info) {
        if (Boolean.TRUE.equals(NativeRead.field(info, "shouldLockSticker"))) return null;
        Object id = NativeRead.field(info, "setId");
        return id instanceof Long && (Long) id > 0 ? (Long) id : null;
    }

    static synchronized boolean readRepository(Object repo) throws Exception {
        if (repo == null) return false;
        List<?> favourites = items(NativeRead.get(repo, "getFavouriteStickersFlow"));
        boolean changed = favourites != null && Streaks.replaceFavourites(favourites);
        List<?> sets = addedSets(repo);
        if (sets == null) return changed; // loading must not erase the last successful snapshot
        Set<Long> available = new HashSet<>();
        boolean updated = false;
        for (Object info : sets) {
            Long id = unlockedSet(info);
            if (id == null) continue;
            available.add(id);
            if (setFlow == null || !setFlow.getDeclaringClass().isInstance(repo)) {
                setFlow = repo.getClass().getMethod("getSetStickersFlow", long.class);
                setFlow.setAccessible(true);
            }
            List<?> content = items(setFlow.invoke(repo, id.longValue()));
            if (content == null || sourceLists.get(id) == content) continue;
            List<StickerItem> converted = new ArrayList<>();
            for (Object sticker : content) {
                if (sticker == null) continue;
                if (convert == null) {
                    Class<?> model = Class.forName("com.ss.android.ugc.aweme.im.common.model.SetSticker");
                    convert = Class.forName("X.13J4").getMethod("LJFF", model, Long.class);
                }
                if (!convert.getParameterTypes()[0].isInstance(sticker)) continue;
                Object item = convert.invoke(null, sticker, null);
                if (item instanceof StickerItem) converted.add((StickerItem) item);
            }
            sourceLists.put(id, content); setItems.put(id, converted);
            updated = true;
        }
        boolean removed = sourceLists.keySet().retainAll(available);
        setItems.keySet().retainAll(available);
        if (!updated && !removed) return changed;
        List<StickerItem> all = new ArrayList<>();
        for (List<StickerItem> set : setItems.values()) all.addAll(set);
        return Streaks.replaceSetStickers(all) || changed;
    }

    private static void refresh(Object repo, String name) {
        try {
            Method method = repo.getClass().getMethod(name, boolean.class, boolean.class, String.class);
            method.setAccessible(true);
            method.invoke(repo, false, false, "ttcuz_settings");
        } catch (ReflectiveOperationException error) {
            Diary.note("sticker library " + name + ": " + error.getClass().getSimpleName());
        }
    }

    private static void requestSets(Object repo, Set<Long> requested) {
        List<?> added = addedSets(repo);
        if (added == null) return;
        List<Long> pending = new ArrayList<>();
        for (Object info : added) {
            Long id = unlockedSet(info);
            if (id != null && requested.add(id)) pending.add(id);
        }
        if (pending.isEmpty()) return;
        try {
            Method method = repo.getClass().getMethod("refreshSetStickers", List.class, String.class);
            method.setAccessible(true); method.invoke(repo, pending, "ttcuz_settings");
        } catch (ReflectiveOperationException error) {
            Diary.note("sticker set load: " + error.getClass().getSimpleName());
        }
    }

    static synchronized void load(Runnable ready) {
        read();
        if (repository == null) return;
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastLoad < 30000) return;
        lastLoad = now;
        final int token = generation;
        final Object repo = repository;
        refresh(repo, "refreshFavouriteStickers");
        refresh(repo, "refreshAddedStickerSets");
        final Set<Long> requested = new HashSet<>();
        requestSets(repo, requested);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(
            new Runnable() {
                int tries;

                public void run() {
                    if (
                        token != generation || !java.util.Objects.equals(account, Account.liveId())
                    ) return;
                    try {
                        requestSets(repo, requested);
                        if (readRepository(repo) && ready != null) ready.run();
                    } catch (Throwable ignored) {}
                    if (++tries < 16) handler.postDelayed(this, 500L);
                }
            },
            500L
        );
    }
}
