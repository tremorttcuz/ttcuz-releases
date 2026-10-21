package ttcuz.examples;

import cat.ttcuz.framework.plugin.PluginSettings;
import cat.ttcuz.framework.plugin.TtcuzPlugin;

public final class Hello extends TtcuzPlugin {
    @Override public void onStart() { ttcuz().log("Плагин запущен"); }
    @Override public void onStop() { ttcuz().log("Плагин остановлен"); }
    @Override public void onSettings(PluginSettings settings) {
        settings.toggle("greeting", "Приветствие", false)
            .slider("strength", "Сила эффекта", 0, 100, 50)
            .choice("mode", "Режим", new String[]{"Обычный", "Компактный"}, 0)
            .action("Проверить", () -> ttcuz().toast("Привет от ttcuz"));
    }
    @Override public void onAccountChanged(String uid) {
        if(ttcuz().prefs().getBoolean("greeting",false))ttcuz().toast("Плагин готов");
    }
}
