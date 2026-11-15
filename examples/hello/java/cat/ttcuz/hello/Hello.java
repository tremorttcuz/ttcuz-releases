package cat.ttcuz.hello;

import cat.ttcuz.framework.plugin.PluginSettings;
import cat.ttcuz.framework.plugin.TtcuzPlugin;

/**
 * The smallest plugin that does something you can see.
 *
 * It counts the starts of the app in its own settings and writes the count into the mod's diary, which is where a
 * plugin's output goes: a repacked TikTok has no log anyone is watching.
 *
 *     python3 -m ttcuz.plugin examples/hello
 *
 * Install the .ctt in the mod's settings: Plugins -> Install .ctt.
 */
public final class Hello extends TtcuzPlugin {

    private static final String STARTS = "starts";

    @Override
    public void onStart() {
        int starts = ttcuz().prefs().getInt(STARTS, 0) + 1;
        ttcuz().prefs().edit().putInt(STARTS, starts).apply();
        if (!ttcuz().prefs().getBoolean("log_starts", true)) return;
        int repeat = ttcuz().prefs().getInt("repeat", 1);
        for (int i = 0; i < repeat; i++)
            ttcuz().log(ttcuz().prefs().getInt("style", 0) == 0
                    ? "hello #" + starts
                    : "hello -- this is start number " + starts);
    }

    /** Rows of the plugin's own settings page; their values land in prefs() under the given keys. */
    @Override
    public void onSettings(PluginSettings settings) {
        settings.text("Журнал")
                .toggle("log_starts", "Писать при каждом запуске", true)
                .choice("style", "Стиль", new String[] {"Коротко", "Подробно"}, 0)
                .slider("repeat", "Строк за запуск", 1, 5, 1);
    }

    /** Every colour the mod redirects passes through here, on the drawing path: hand it back untouched. */
    // @uses colours: только показывает хук: все цвета остаются как были
    @Override
    public int onColour(int colour) {
        return colour;
    }
}
