package cat.ttcuz.framework;

import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Which of the bottom bar's buttons show, and in what order: chosen in the settings under the island's preview.
 *
 * The buttons are TikTok's own cells, moved and hidden in their own row, so a tap still goes where TikTok sends it
 * and the island simply draws what is there. Home and Profile always stay: without them the feed or the settings
 * could not be reached again.
 */
final class IslandTabs {
    private IslandTabs() {}

    static final int HOME = 0, FRIENDS = 1, CREATE = 2, INBOX = 3, PROFILE = 4;
    private static final String ORDER = "island_tab_order", HIDDEN = "island_tab_hidden";
    private static final int[] DEFAULT = {HOME, FRIENDS, CREATE, INBOX, PROFILE};

    /** Goes up on every change, so the island arranges its row again. */
    static volatile int version;

    private static SharedPreferences prefs() {
        android.content.Context context = Ttcuz.context();
        return context == null ? null : context.getSharedPreferences(Ttcuz.PREFS, android.content.Context.MODE_PRIVATE);
    }

    /** Read once, kept until changed here: the island asks on every rearrangement. */
    private static volatile int[] cachedOrder;
    private static volatile int cachedHidden = -1;

    /** The five buttons, left to right. */
    static int[] order() {
        int[] known = cachedOrder;
        if (known != null) return known.clone();
        SharedPreferences store = prefs();
        String saved = store == null ? null : store.getString(ORDER, null);
        int[] out = parse(saved);
        if (store != null) cachedOrder = out;
        return out.clone();
    }

    private static int[] parse(String saved) {
        if (saved == null || saved.length() != 5) return DEFAULT.clone();
        int[] out = new int[5];
        int seen = 0;
        for (int i = 0; i < 5; i++) {
            int role = saved.charAt(i) - '0';
            if (role < 0 || role > 4 || (seen & 1 << role) != 0) return DEFAULT.clone();
            seen |= 1 << role;
            out[i] = role;
        }
        return out;
    }

    /** A bit per button that is hidden. */
    static int hidden() {
        int known = cachedHidden;
        if (known >= 0) return known;
        SharedPreferences store = prefs();
        int value = store == null ? 0 : store.getInt(HIDDEN, 0) & ~(1 << HOME | 1 << PROFILE) & 31;
        if (store != null) cachedHidden = value;
        return value;
    }

    static boolean shown(int role) { return (hidden() & 1 << role) == 0; }

    static boolean canHide(int role) { return role != HOME && role != PROFILE; }

    static void set(int[] order, int hidden) {
        SharedPreferences store = prefs();
        if (store == null) return;
        StringBuilder text = new StringBuilder(5);
        for (int role : order) text.append((char) ('0' + role));
        int kept = hidden & ~(1 << HOME | 1 << PROFILE) & 31;
        store.edit().putString(ORDER, text.toString()).putInt(HIDDEN, kept).apply();
        cachedOrder = parse(text.toString());
        cachedHidden = kept;
        version++;
    }

    static void reset() {
        SharedPreferences store = prefs();
        if (store != null) store.edit().remove(ORDER).remove(HIDDEN).apply();
        cachedOrder = DEFAULT.clone();
        cachedHidden = 0;
        version++;
    }

    static boolean custom() {
        return hidden() != 0 || !java.util.Arrays.equals(order(), DEFAULT);
    }

    // ------------------------------------------------------------------ the native row

    /** Each row as TikTok laid it out: its cells by role, and the child slots they stood in. */
    private static final class Native {
        final View[] cells = new View[5];
        final int[] visibility = new int[5];
    }
    private static final Map<ViewGroup, Native> rows = new WeakHashMap<>();

    /**
     * Puts the row's cells in the chosen order and hides the chosen ones. True when it changed anything.
     * Only a plain horizontal LinearLayout is rearranged: in any other layout the order of children is not the
     * order on screen, and the bar is left as TikTok made it.
     */
    static boolean apply(ViewGroup row) {
        if (!(row instanceof LinearLayout) || ((LinearLayout) row).getOrientation() != LinearLayout.HORIZONTAL) return false;
        Native known = rows.get(row);
        if (known == null) {
            if (!custom()) return false;
            known = learn(row);
            if (known == null) return false;
            rows.put(row, known);
        }
        for (View cell : known.cells) if (cell == null || cell.getParent() != row) { rows.remove(row); return false; }
        int[] order = custom() ? order() : DEFAULT;
        int hidden = custom() ? hidden() : 0;
        boolean changed = false;
        // the slots the five cells take among the row's children, left to right
        int[] slots = new int[5];
        for (int i = 0; i < 5; i++) slots[i] = row.indexOfChild(known.cells[i]);
        java.util.Arrays.sort(slots);
        boolean inOrder = true;
        for (int i = 0; i < 5; i++) if (row.getChildAt(slots[i]) != known.cells[order[i]]) { inOrder = false; break; }
        if (!inOrder) {
            ViewGroup.LayoutParams[] params = new ViewGroup.LayoutParams[5];
            for (int role = 0; role < 5; role++) params[role] = known.cells[role].getLayoutParams();
            for (int i = 4; i >= 0; i--) row.removeViewAt(slots[i]);
            for (int i = 0; i < 5; i++) row.addView(known.cells[order[i]], slots[i], params[order[i]]);
            changed = true;
        }
        for (int role = 0; role < 5; role++) {
            int want = (hidden & 1 << role) != 0 ? View.GONE : known.visibility[role];
            if (known.cells[role].getVisibility() != want) { known.cells[role].setVisibility(want); changed = true; }
        }
        if (!custom()) rows.remove(row);
        return changed;
    }

    /** TikTok's own order back, every cell shown as it was. */
    static void restore(ViewGroup row) {
        Native known = rows.remove(row);
        if (known == null) return;
        try {
            int[] slots = new int[5];
            for (int i = 0; i < 5; i++) {
                if (known.cells[i] == null || known.cells[i].getParent() != row) return;
                slots[i] = row.indexOfChild(known.cells[i]);
            }
            java.util.Arrays.sort(slots);
            ViewGroup.LayoutParams[] params = new ViewGroup.LayoutParams[5];
            for (int role = 0; role < 5; role++) params[role] = known.cells[role].getLayoutParams();
            for (int i = 4; i >= 0; i--) row.removeViewAt(slots[i]);
            for (int i = 0; i < 5; i++) row.addView(known.cells[DEFAULT[i]], slots[i], params[DEFAULT[i]]);
            for (int role = 0; role < 5; role++) known.cells[role].setVisibility(known.visibility[role]);
        } catch (RuntimeException error) {
            Diary.note("island tabs restore: " + error);
        }
    }

    /** The five cells of TikTok's untouched bar, by what they are. */
    private static Native learn(ViewGroup row) {
        Native known = new Native();
        int found = 0;
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE || child.getWidth() <= 0) continue;
            int role = NavigationIsland.role(child, 0);
            if (role < 0) role = CREATE;
            if (known.cells[role] != null) return null;
            known.cells[role] = child;
            known.visibility[role] = child.getVisibility();
            found++;
        }
        if (found != 5) {
            Diary.note("island tabs: " + found + " cells, not rearranged");
            return null;
        }
        return known;
    }
}
