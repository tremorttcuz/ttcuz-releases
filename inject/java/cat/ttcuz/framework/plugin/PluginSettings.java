package cat.ttcuz.framework.plugin;

import android.view.View;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Declarative rows rendered by the host's normal Material settings controls. */
public final class PluginSettings {

    public enum Kind {
        TEXT,
        ACTION,
        TOGGLE,
        CHOICE,
        SLIDER,
        VIEW,
    }

    public static final class Row {

        public final Kind kind;
        public final String title, key;
        public final Object value;
        public final int min, max;

        private Row(Kind kind, String title, String key, Object value, int min, int max) {
            this.kind = kind;
            this.title = title;
            this.key = key;
            this.value = value;
            this.min = min;
            this.max = max;
        }
    }

    private final List<Row> rows = new ArrayList<>();

    public List<Row> rows() {
        return Collections.unmodifiableList(rows);
    }

    public PluginSettings text(String text) {
        return add(Kind.TEXT, text, null, null, 0, 0);
    }

    public PluginSettings action(String title, Runnable action) {
        return add(Kind.ACTION, title, null, action, 0, 0);
    }

    public PluginSettings toggle(String key, String title, boolean fallback) {
        return add(Kind.TOGGLE, title, key, fallback, 0, 0);
    }

    public PluginSettings choice(String key, String title, String[] options, int fallback) {
        if (
            options == null || options.length == 0 || fallback < 0 || fallback >= options.length
        ) throw new IllegalArgumentException("Invalid choice");
        return add(Kind.CHOICE, title, key, options.clone(), fallback, 0);
    }

    public PluginSettings slider(String key, String title, int min, int max, int fallback) {
        if (
            max <= min || (long) max - min > Integer.MAX_VALUE || fallback < min || fallback > max
        ) throw new IllegalArgumentException("Invalid slider");
        return add(Kind.SLIDER, title, key, fallback, min, max);
    }

    public PluginSettings view(View view) {
        return add(Kind.VIEW, "", null, view, 0, 0);
    }

    private PluginSettings add(
        Kind kind,
        String title,
        String key,
        Object value,
        int min,
        int max
    ) {
        if (title == null || (key != null && key.isEmpty())) throw new IllegalArgumentException(
            "Invalid row"
        );
        rows.add(new Row(kind, title, key, value, min, max));
        return this;
    }
}
