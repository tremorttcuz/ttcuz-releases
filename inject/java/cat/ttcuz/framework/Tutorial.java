package cat.ttcuz.framework;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.ref.WeakReference;

/** The crash-report question first, then six short steps, each opening the menu group it is about. */
final class Tutorial {
    private static final String DONE = "tutorial_done_v2";
    private static WeakReference<Dialog> open = new WeakReference<Dialog>(null);
    private static WeakReference<Activity> owner = new WeakReference<Activity>(null);
    private static final String[] TITLES = Text.TUTORIAL_TITLES;
    private static final String[] WORDS = Text.TUTORIAL_WORDS;
    private Tutorial() {}

    static void maybeShow(Activity activity) {
        String name = activity.getClass().getName();
        if (!"com.ss.android.ugc.aweme.main.MainActivity".equals(name)
                && !SettingsRow.SETTINGS_ACTIVITY.equals(name)
                && !(activity instanceof SettingsActivity)) return;
        if (finished(activity) && CrashConsent.asked(activity)) return;
        WeakReference<Activity> target = new WeakReference<Activity>(activity);
        activity.getWindow().getDecorView().postDelayed(() -> {
            Activity current=target.get();
            if (current == null || current != Screen.now() || current.isFinishing() || current.isDestroyed()
                    || !current.getWindow().getDecorView().hasWindowFocus()) return;
            if (!CrashConsent.asked(current)) { CrashConsent.show(current,() -> afterConsent(current)); return; }
            if (!finished(current)) offer(current);
        },900L);
    }

    private static boolean finished(Activity activity) {
        return activity.getSharedPreferences(Ttcuz.PREFS,Context.MODE_PRIVATE).getBoolean(DONE,false);
    }

    /** The question is answered; the walkthrough follows unless it was seen before. */
    private static void afterConsent(Activity activity) {
        if (finished(activity) || activity.isFinishing() || activity.isDestroyed()) return;
        offer(activity);
    }

    /** Asked, never pushed: "Not now" is final, and the tour stays in the mod's About section. */
    static void offer(Activity activity) {
        activity.getWindow().getDecorView().postDelayed(() -> {
            if (activity.isFinishing() || activity.isDestroyed() || finished(activity)) return;
            Popup.ask(activity, Text.t("Пройти обучение?"),
                    Text.t("Шесть коротких шагов: где меню мода и что в каждом разделе. Его всегда можно открыть в «О проекте»."),
                    Text.t("Пройти"), () -> show(activity),
                    Text.t("Не сейчас"), () -> finish(activity),
                    null, null);
        }, 250L);
    }

    static void show(Activity activity) {
        if(!(activity instanceof SettingsActivity)) {
            activity.startActivity(new android.content.Intent(activity,SettingsActivity.class).putExtra("ttcuz_tutorial",true));
            return;
        }
        Dialog previous=open.get();
        if (previous != null && previous.isShowing()) return;
        if (activity.isFinishing() || activity.isDestroyed()) return;
        Skin skin=Skin.remembered(activity);
        float d=activity.getResources().getDisplayMetrics().density;
        Panel panel=Panel.with(activity,skin,null);
        LinearLayout page=new LinearLayout(activity); page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        // progress: one slim bar per step, the ones reached in the accent
        LinearLayout progress=new LinearLayout(activity); progress.setOrientation(LinearLayout.HORIZONTAL);
        final View[] bars=new View[TITLES.length];
        for (int i=0;i<TITLES.length;i++) {
            View bar=new View(activity); bars[i]=bar;
            LinearLayout.LayoutParams at=new LinearLayout.LayoutParams(0,Math.round(3*d),1f);
            if(i>0)at.leftMargin=Math.round(4*d);
            progress.addView(bar,at);
        }
        page.addView(progress,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        android.widget.ImageView picture=new android.widget.ImageView(activity);
        LinearLayout.LayoutParams pictureAt=new LinearLayout.LayoutParams(Math.round(52*d),Math.round(52*d));
        pictureAt.topMargin=Math.round(26*d);
        page.addView(picture,pictureAt);
        TextView title=new TextView(activity); title.setTextColor(skin.text); title.setTextSize(24);
        title.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));
        title.setGravity(android.view.Gravity.CENTER);
        title.setPadding(0,Math.round(14*d),0,Math.round(8*d));
        TextView body=new TextView(activity); body.setTextColor(skin.muted()); body.setTextSize(15);
        body.setGravity(android.view.Gravity.CENTER);
        body.setLineSpacing(0,1.2f);
        body.setPadding(Math.round(4*d),0,Math.round(4*d),Math.round(10*d));
        TextView counter=new TextView(activity); counter.setTextColor(skin.muted()); counter.setTextSize(12);
        counter.setGravity(android.view.Gravity.CENTER);
        page.addView(title); page.addView(body); page.addView(counter); panel.view(page);
        final int[] step={0};
        TextView next=panel.action(Text.TUTORIAL_NEXT,true);
        TextView back=panel.action(Text.TUTORIAL_BACK,false);
        Runnable render=() -> {
            ((SettingsActivity)activity).tutorialStep(step[0]);
            counter.setText((step[0]+1)+" / "+TITLES.length);
            for (int i=0;i<bars.length;i++) {
                android.graphics.drawable.GradientDrawable fill=new android.graphics.drawable.GradientDrawable();
                fill.setCornerRadius(2*d);
                fill.setColor(i<=step[0]?Accent.colour():(skin.text&0xFFFFFF)|0x26000000);
                bars[i].setBackground(fill);
            }
            android.graphics.drawable.Drawable glyph=SettingsGlyph.make(Text.TUTORIAL_ICONS[step[0]],Accent.colour());
            picture.setImageDrawable(glyph);picture.setVisibility(glyph==null?View.GONE:View.VISIBLE);
            title.setText(TITLES[step[0]]); body.setText(WORDS[step[0]]);
            next.setText(step[0]==TITLES.length-1 ? Text.TUTORIAL_DONE : Text.TUTORIAL_NEXT);
            back.setVisibility(step[0]==0 ? View.GONE : View.VISIBLE);
            title.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        };
        render.run();
        next.setOnClickListener(v -> {
            if (step[0]==TITLES.length-1) { finish(activity); panel.close(); }
            else { step[0]++; render.run(); Motion.enter(page,1f); }
        });
        back.setOnClickListener(v -> {
            if (step[0]>0) { step[0]--; render.run(); Motion.enter(page,-1f); }
        });
        panel.quiet(Text.TUTORIAL_SKIP,() -> finish(activity));
        Dialog dialog=panel.dialog();
        dialog.setOnCancelListener(gone -> finish(activity));
        dialog.setOnDismissListener(gone -> {
            if (open.get()==dialog) {open.clear();owner.clear();}
        });
        open=new WeakReference<Dialog>(dialog); owner=new WeakReference<Activity>(activity);
        panel.show();
    }

    private static void finish(Activity activity) {
        activity.getSharedPreferences(Ttcuz.PREFS,Context.MODE_PRIVATE).edit().putBoolean(DONE,true).apply();
    }

    static void paused(Activity activity) {
        CrashConsent.paused(activity);
        if (owner.get()!=activity) return;
        Dialog dialog=open.get(); open.clear(); owner.clear();
        if (dialog!=null) dialog.dismiss();
    }
}
