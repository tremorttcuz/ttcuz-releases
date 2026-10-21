package ttcuz.examples;

import cat.ttcuz.framework.plugin.PluginSettings;
import cat.ttcuz.framework.plugin.TtcuzPlugin;

public final class Commands extends TtcuzPlugin {
    @Override public void onSettings(PluginSettings settings) {
        settings.action("Показать сообщение", () -> ttcuz().toast("Команда выполнена"))
            .action("Отправить событие", () -> ttcuz().emit("ttcuz.example.ping", "Привет"));
    }
    @Override public void onEvent(String event,Object payload) {
        if("ttcuz.example.ping".equals(event))ttcuz().log(String.valueOf(payload));
    }
}
