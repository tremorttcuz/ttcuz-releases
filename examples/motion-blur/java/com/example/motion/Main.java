package com.example.motion;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import cat.ttcuz.framework.plugin.PluginSettings;
import cat.ttcuz.framework.plugin.TtcuzPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Motion blur on the feed: every item of every scrolling list on screen is blurred along its movement, as strongly
 * as it moved since the last frame. A still list costs nothing. The strength is a slider in the mod's own settings.
 */
public class Main extends TtcuzPlugin {
    private final WeakHashMap<View, Runnable> blurred = new WeakHashMap<>();
    private final WeakHashMap<View, Boolean> lists = new WeakHashMap<>();

    // @uses mod: ползунок «Сила размытия» в настройках мода
    @Override
    public void onModSettings(PluginSettings page) {
        page.text("Motion blur: размытие видео, пока листается лента")
            .toggle("on", "Включено", true)
            .slider("strength", "Сила размытия", 1, 4, 2);
    }

    // @uses interface: находит ленту и другие списки на открытом экране
    @Override
    public void onScreen(Activity activity, ViewGroup content) {
        for (ViewGroup group : ttcuz().views(content, ViewGroup.class)) {
            String type = group.getClass().getName();
            if (!type.endsWith("RecyclerView") && !type.endsWith("ViewPager2") && !type.endsWith("ViewPager")) continue;
            if (lists.put(group, Boolean.TRUE) != null) continue;
            // new items appear while scrolling: each layout pass picks them up
            group.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> cover((ViewGroup) v));
            cover(group);
        }
    }

    @Override
    public void onPreferenceChanged(String scope, String key) {
        // a new strength: start over with it
        List<Runnable> stops = new ArrayList<>(blurred.values());
        blurred.clear();
        for (Runnable stop : stops) stop.run();
        for (View list : new ArrayList<>(lists.keySet())) cover((ViewGroup) list);
    }

    private void cover(ViewGroup list) {
        if (!ttcuz().prefs().getBoolean("on", true)) return;
        float strength = ttcuz().prefs().getInt("strength", 2);
        for (int i = 0; i < list.getChildCount(); i++) {
            View item = list.getChildAt(i);
            // @uses effects: размывает видео по направлению прокрутки
            if (!blurred.containsKey(item)) blurred.put(item, ttcuz().motionBlur(item, strength * 0.5f));
        }
    }
}
