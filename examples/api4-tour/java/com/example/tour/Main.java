package com.example.tour;

import cat.ttcuz.framework.plugin.PluginSettings;
import cat.ttcuz.framework.plugin.TtcuzPlugin;

import java.util.Map;

/**
 * A short tour of Plugin API 4:
 * - onTikTok: counts the videos watched and pauses the video while its comments are open (if switched on);
 * - hide(): hides views by a selector on every screen;
 * - every(): a repeating task that stops with the plugin;
 * - expose(): another plugin may ask ttcuz().call("com.example.tour", "watched", null).
 */
public final class Main extends TtcuzPlugin {
    private int watched;
    private Runnable ticker;

    @Override
    public void onStart() {
        // @uses events: lets other plugins read how many videos were watched
        ttcuz().expose("watched", argument -> watched);
        if (ttcuz().prefs().getBoolean("hide_live", false)) {
            // @uses interface: hides views whose description mentions LIVE
            ttcuz().hide("~LIVE");
        }
        ticker = ttcuz().every(() -> ttcuz().log("watched so far: " + watched), 60_000);
    }

    @Override
    public void onStop() {
        if (ticker != null) ticker.run();
    }

    // @uses watch: counts videos and follows the comments being opened and closed
    @Override
    public void onTikTok(String event, Map<String, Object> data) {
        switch (event) {
            case "video:selected":
                watched++;
                break;
            case "comments:open":
                // @uses player: pauses the video while its comments are open
                if (ttcuz().prefs().getBoolean("pause", true) && ttcuz().playing()) ttcuz().pause();
                break;
            case "comments:close":
                if (ttcuz().prefs().getBoolean("pause", true)) ttcuz().play();
                break;
            default:
                break;
        }
    }

    @Override
    public void onSettings(PluginSettings s) {
        s.text("Видео за этот запуск: " + watched);
        s.toggle("pause", "Пауза, пока открыты комментарии", true);
        s.toggle("hide_live", "Прятать отметки LIVE (после перезапуска плагина)", false);
    }
}
