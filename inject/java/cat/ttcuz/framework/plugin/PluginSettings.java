package cat.ttcuz.framework.plugin;

import android.view.View;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PluginSettings {
    private final List<Row> rows = new ArrayList();

    public enum Kind {
        TEXT,
        ACTION,
        TOGGLE,
        CHOICE,
        SLIDER,
        VIEW
    }

    public static final class Row {
        public final String key;
        public final Kind kind;
        public final int max;
        public final int min;
        public final String title;
        public final Object value;

        private Row(Kind kind, String str, String str2, Object obj, int i, int i2) {
            this.kind = kind;
            this.title = str;
            this.key = str2;
            this.value = obj;
            this.min = i;
            this.max = i2;
        }
    }

    public List<Row> rows() {
        return Collections.unmodifiableList(this.rows);
    }

    public PluginSettings text(String str) {
        return add(Kind.TEXT, str, null, null, 0, 0);
    }

    public PluginSettings action(String str, Runnable runnable) {
        return add(Kind.ACTION, str, null, runnable, 0, 0);
    }

    public PluginSettings toggle(String str, String str2, boolean z) {
        return add(Kind.TOGGLE, str2, str, Boolean.valueOf(z), 0, 0);
    }

    public PluginSettings choice(String str, String str2, String[] strArr, int i) {
        if (strArr == null || strArr.length == 0 || i < 0 || i >= strArr.length) {
            throw new IllegalArgumentException("Invalid choice");
        }
        return add(Kind.CHOICE, str2, str, strArr.clone(), i, 0);
    }

    public PluginSettings slider(String str, String str2, int i, int i2, int i3) {
        if (i2 <= i || i2 - i > 2147483647L || i3 < i || i3 > i2) {
            throw new IllegalArgumentException("Invalid slider");
        }
        return add(Kind.SLIDER, str2, str, Integer.valueOf(i3), i, i2);
    }

    public PluginSettings view(View view) {
        return add(Kind.VIEW, "", null, view, 0, 0);
    }

    private PluginSettings add(Kind kind, String str, String str2, Object obj, int i, int i2) {
        if (str == null || (str2 != null && str2.isEmpty())) {
            throw new IllegalArgumentException("Invalid row");
        }
        this.rows.add(new Row(kind, str, str2, obj, i, i2));
        return this;
    }
}
