package cat.ttcuz.framework;

import cat.ttcuz.framework.LagTally;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

final class LagTally {
    static final int PLACES = 40;
    static final String REST = "elsewhere";
    private final Map<String, Place> places = new HashMap();
    private final Map<String, Screen> screens = new HashMap();
    private final String[] stages;

    private static final class Place {
        String example;
        long longest;
        int times;
        long total;

        private Place() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class Screen {
        long frames;
        long slow;
        final long[] spent;
        long stuck;

        Screen(int i) {
            this.spent = new long[i];
        }
    }

    LagTally(String[] strArr) {
        this.stages = strArr;
    }

    synchronized void stall(String str, String str2, long j) {
        if (!this.places.containsKey(str) && this.places.size() >= PLACES) {
            str = REST;
        }
        Place place = this.places.get(str);
        if (place == null) {
            place = new Place();
            this.places.put(str, place);
        }
        place.times++;
        place.total += j;
        if (j > place.longest) {
            place.longest = j;
            place.example = str2;
        }
    }

    synchronized void frame(String str, boolean z, boolean z2, long[] jArr) {
        Screen screen = this.screens.get(str);
        if (screen == null) {
            if (this.screens.size() >= PLACES) {
                return;
            }
            screen = new Screen(this.stages.length);
            this.screens.put(str, screen);
        }
        screen.frames++;
        if (z) {
            screen.slow++;
            if (z2) {
                screen.stuck++;
            }
            for (int i = 0; i < this.stages.length && i < jArr.length; i++) {
                if (jArr[i] > 0) {
                    long[] jArr2 = screen.spent;
                    jArr2[i] = jArr2[i] + jArr[i];
                }
            }
        }
    }

    synchronized String report() {
        StringBuilder sb;
        sb = new StringBuilder();
        ArrayList<String> arrayList = new ArrayList(this.screens.keySet());
        Collections.sort(arrayList, new Comparator() { // from class: cat.ttcuz.framework.LagTally$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                int lambda$report$0;
                lambda$report$0 = LagTally.this.lambda$report$0((String) obj, (String) obj2);
                return lambda$report$0;
            }
        });
        for (String str : arrayList) {
            final Screen screen = this.screens.get(str);
            sb.append(String.format(Locale.US, "%s: %d frames, %d slow (%.1f%%), %d stuck", str, Long.valueOf(screen.frames), Long.valueOf(screen.slow), Double.valueOf((screen.slow * 100.0d) / Math.max(1L, screen.frames)), Long.valueOf(screen.stuck)));
            long j = 0;
            for (long j2 : screen.spent) {
                j += j2;
            }
            if (j > 0) {
                sb.append("; slow frames went on");
                ArrayList arrayList2 = new ArrayList();
                for (int i = 0; i < this.stages.length; i++) {
                    arrayList2.add(Integer.valueOf(i));
                }
                Collections.sort(arrayList2, (x, y) -> Long.compare(screen.spent[(Integer) y], screen.spent[(Integer) x]));
                Iterator it = arrayList2.iterator();
                int i2 = 0;
                while (it.hasNext()) {
                    int intValue = ((Integer) it.next()).intValue();
                    long round = Math.round((screen.spent[intValue] * 100.0d) / j);
                    if (round >= 5 && i2 < 4) {
                        sb.append(i2 == 0 ? " " : ", ");
                        sb.append(this.stages[intValue]);
                        sb.append(' ');
                        sb.append(round);
                        sb.append('%');
                        i2++;
                    }
                }
            }
            sb.append('\n');
        }
        ArrayList<String> arrayList3 = new ArrayList(this.places.keySet());
        Collections.sort(arrayList3, new Comparator() { // from class: cat.ttcuz.framework.LagTally$$ExternalSyntheticLambda2
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                int lambda$report$2;
                lambda$report$2 = LagTally.this.lambda$report$2((String) obj, (String) obj2);
                return lambda$report$2;
            }
        });
        if (arrayList3.isEmpty()) {
            sb.append("no stalls\n");
        }
        for (String str2 : arrayList3) {
            Place place = this.places.get(str2);
            sb.append(String.format(Locale.US, "%d ms in %d stalls, longest %d: %s", Long.valueOf(place.total), Integer.valueOf(place.times), Long.valueOf(place.longest), str2));
            if (place.example != null && !place.example.equals(str2)) {
                sb.append("  [");
                sb.append(place.example);
                sb.append(']');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ int lambda$report$0(String str, String str2) {
        return Long.compare(this.screens.get(str2).frames, this.screens.get(str).frames);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ int lambda$report$2(String str, String str2) {
        return Long.compare(this.places.get(str2).total, this.places.get(str).total);
    }
}
