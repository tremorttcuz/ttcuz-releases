package cat.ttcuz.framework;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.Locale;

/**
 * One calm question on the first launch: may the mod send crash reports?
 *
 * The card is the mod's own Panel, so it takes the screen's colours and the
 * accent: a small icon, the question, what is sent, and the two answers side
 * by side. Back counts as "no". Nothing is sent before "yes", and the choice
 * lives in the mod settings.
 */
final class CrashConsent {
    private static WeakReference<Dialog> open = new WeakReference<Dialog>(null);
    private static WeakReference<Activity> owner = new WeakReference<Activity>(null);

    private CrashConsent() {}

    /** Asked already, or the switch was used in settings before this window existed. */
    static boolean asked(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(CrashReports.ASKED, false) || prefs.getBoolean(CrashReports.KEY, false);
    }

    /** Shows the window; `then` runs after an answer, never after a pause. */
    static void show(final Activity activity, final Runnable then) {
        Dialog previous = open.get();
        if (previous != null && previous.isShowing()) return;
        if (activity.isFinishing() || activity.isDestroyed() || asked(activity)) return;

        final Skin skin = Skin.remembered(activity);
        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);

        page.addView(icon(activity, skin), new LinearLayout.LayoutParams(dp(activity, 52), dp(activity, 52)));

        TextView title = text(activity, Text.CONSENT_TITLE, skin.text, 21f, true);
        title.setPadding(0, dp(activity, 14), 0, dp(activity, 8));
        page.addView(title, wide());

        TextView body = text(activity, Text.CONSENT_BODY, skin.muted(), 15f, false);
        body.setLineSpacing(0, 1.2f);
        page.addView(body, wide());

        TextView note = text(activity, Text.CONSENT_NOTE, skin.muted(), 12.5f, false);
        note.setAlpha(.8f);
        note.setPadding(dp(activity, 8), dp(activity, 12), dp(activity, 8), 0);
        page.addView(note, wide());

        // the two answers side by side, there at once: nothing to wait for
        LinearLayout answers = new LinearLayout(activity);
        answers.setOrientation(LinearLayout.HORIZONTAL);
        final TextView no = button(activity, skin, Text.CONSENT_NO, false);
        final TextView yes = button(activity, skin, Text.CONSENT_YES, true);
        answers.addView(no, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f);
        right.leftMargin = dp(activity, 10);
        answers.addView(yes, right);
        LinearLayout.LayoutParams answersWhere = wide();
        answersWhere.topMargin = dp(activity, 20);
        page.addView(answers, answersWhere);

        final Panel panel = Panel.with(activity, skin, null);
        panel.view(page);
        final Dialog dialog = panel.dialog();
        final boolean[] answered = {false};
        dialog.setOnDismissListener(d -> {
            if (open.get() == dialog) { open.clear(); owner.clear(); }
        });
        // back or a tap outside is a "no", not a trap: the choice can be changed in settings any time
        dialog.setOnCancelListener(d -> answer(activity, null, false, answered, then));
        open = new WeakReference<Dialog>(dialog);
        owner = new WeakReference<Activity>(activity);
        panel.show();
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);

        yes.setOnClickListener(v -> answer(activity, panel, true, answered, then));
        no.setOnClickListener(v -> answer(activity, panel, false, answered, then));
    }

    /** The window goes away with its screen; nothing is decided, so it comes back next time. */
    static void paused(Activity activity) {
        if (owner.get() != activity) return;
        Dialog dialog = open.get();
        open.clear();
        owner.clear();
        if (dialog != null) dialog.dismiss();
    }

    private static void answer(Activity activity, Panel panel, boolean on, boolean[] answered, Runnable then) {
        if (answered[0]) return;
        answered[0] = true;
        try {
            CrashReports.set(activity, on);   // also remembers that the question was asked
        } catch (Throwable error) {
            Diary.note("consent: " + error);
        }
        activity.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE).edit().putBoolean(CrashReports.ASKED, true).apply();
        if (panel != null) panel.close();
        if (then != null) then.run();
    }

    private static View icon(Activity activity, Skin skin) {
        FrameLayout frame = new FrameLayout(activity);
        GradientDrawable tile = new GradientDrawable();
        tile.setCornerRadius(dp(activity, 16));
        tile.setColor((Accent.colour() & 0xFFFFFF) | 0x24000000);
        frame.setBackground(tile);
        ImageView image = new ImageView(activity);
        image.setImageDrawable(SettingsGlyph.make("bug_report", Accent.colour()));
        frame.addView(image, new FrameLayout.LayoutParams(dp(activity, 28), dp(activity, 28), Gravity.CENTER));
        return frame;
    }

    private static TextView text(Context context, String words, int colour, float size, boolean medium) {
        TextView view = new TextView(context);
        view.setText(words);
        view.setTextColor(colour);
        view.setTextSize(size);
        view.setGravity(Gravity.CENTER_HORIZONTAL);
        if (medium) view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return view;
    }

    private static TextView button(Context context, Skin skin, String label, boolean main) {
        TextView button = new TextView(context);
        button.setText(label);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(15f);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        button.setMinHeight(dp(context, 48));
        int accent = Accent.colour();
        if (main) {
            button.setTextColor(onAccent(accent));
            button.setBackground(Expressive.tile(context, accent, 0x33FFFFFF, dp(context, 24), dp(context, 24), dp(context, 12)));
        } else {
            button.setTextColor(skin.text);
            button.setBackground(Expressive.tile(context, (skin.text & 0xFFFFFF) | 0x14000000,
                    (accent & 0xFFFFFF) | 0x33000000, dp(context, 24), dp(context, 24), dp(context, 12)));
        }
        button.setClickable(true);
        button.setFocusable(true);
        Motion.touch(button);
        return button;
    }

    private static GradientDrawable pill(int colour, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(colour);
        shape.setCornerRadius(radius);
        return shape;
    }

    private static LinearLayout.LayoutParams wide() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static int onAccent(int colour) {
        int red = (colour >> 16) & 0xFF, green = (colour >> 8) & 0xFF, blue = colour & 0xFF;
        return (red * 299 + green * 587 + blue * 114) / 1000 > 150 ? 0xFF111111 : 0xFFFFFFFF;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
