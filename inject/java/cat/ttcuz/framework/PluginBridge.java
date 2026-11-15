package cat.ttcuz.framework;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import cat.ttcuz.framework.plugin.PluginContext;
import java.util.Arrays;

/**
 * The parts of the mod a plugin reaches through PluginContext that live in this package: the mod's own dialogs and
 * menus (in its look), Android permission requests, notifications and the overlay layer over a screen.
 */
public final class PluginBridge {
    private PluginBridge() {}

    // ------------------------------------------------------------ dialogs

    public static void dialog(Activity activity, String title, String message, String ok, final Runnable onOk, String cancel) {
        if (activity == null || activity.isFinishing()) return;
        Panel panel = Panel.with(activity, Skin.remembered(activity), title == null ? "" : title);
        if (message != null && !message.isEmpty()) panel.text(message);
        panel.primary(ok == null || ok.isEmpty() ? Text.t("Готово") : ok, onOk == null ? null : onOk::run);
        if (cancel != null && !cancel.isEmpty()) panel.quiet(cancel, null);
        panel.show();
    }

    public static void menu(Activity activity, String title, String[] items, final PluginContext.Pick pick) {
        if (activity == null || activity.isFinishing() || items == null) return;
        Panel panel = Panel.with(activity, Skin.remembered(activity), title == null ? "" : title);
        for (int i = 0; i < items.length; i++) {
            final int index = i;
            panel.option(items[i], false, false, () -> { if (pick != null) pick.picked(index); });
        }
        panel.quiet(Text.t("Отмена"), null);
        panel.show();
    }

    /** page, card, text: the colours of the mod's own screens right now. */
    public static int[] palette(Context context) {
        Skin skin = Skin.remembered(context);
        return new int[]{skin.page, skin.card, skin.text};
    }

    // ------------------------------------------------------------ Android permissions

    /** Whether the app itself declares it: Android grants nothing else, whatever a plugin asks. */
    public static boolean declared(Context context, String permission) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            return info.requestedPermissions != null && Arrays.asList(info.requestedPermissions).contains(permission);
        } catch (Throwable error) { return false; }
    }

    public static void request(Context context, String permission, StoragePermissionActivity.Answer answer) {
        if (!declared(context, permission)) {
            Diary.note("plugin permission " + permission + ": not declared by the app");
            answer.answered(false);
            return;
        }
        StoragePermissionActivity.ask(context, permission, answer);
    }

    // ------------------------------------------------------------ notifications

    private static final String CHANNEL = "ttcuz_plugins";

    public static void notify(Context context, String pluginId, String pluginName, String title, String text) {
        android.app.NotificationManager manager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (android.os.Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(CHANNEL) == null)
            manager.createNotificationChannel(new android.app.NotificationChannel(CHANNEL, Text.t("Плагины ttcuz"), android.app.NotificationManager.IMPORTANCE_DEFAULT));
        android.app.Notification.Builder builder = android.os.Build.VERSION.SDK_INT >= 26
                ? new android.app.Notification.Builder(context, CHANNEL) : new android.app.Notification.Builder(context);
        builder.setSmallIcon(context.getApplicationInfo().icon).setContentTitle(title).setContentText(text)
                .setSubText(pluginName).setAutoCancel(true).setStyle(new android.app.Notification.BigTextStyle().bigText(text));
        android.content.Intent open = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (open != null) builder.setContentIntent(android.app.PendingIntent.getActivity(context, pluginId.hashCode(), open, android.app.PendingIntent.FLAG_IMMUTABLE));
        try { manager.notify("plugin:" + pluginId, (int) (System.currentTimeMillis() & 0x7fffffff), builder.build()); }
        catch (SecurityException error) { Diary.note("plugin " + pluginId + ": notifications not allowed"); }
    }

    // ------------------------------------------------------------ overlay

    private static final int LAYER = 0x7f300f31;

    /** The plugin's own layer over a screen: full size, lets touches through wherever the plugin put nothing. */
    public static FrameLayout layer(Activity activity, String pluginId) {
        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        String tag = "ttcuz-plugin:" + pluginId;
        for (int i = 0; i < decor.getChildCount(); i++) {
            View child = decor.getChildAt(i);
            if (tag.equals(child.getTag(LAYER))) return (FrameLayout) child;
        }
        FrameLayout layer = new FrameLayout(activity);
        layer.setTag(LAYER, tag);
        layer.setClipChildren(false);
        layer.setFitsSystemWindows(false);
        decor.addView(layer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return layer;
    }

    /** Every layer the plugin made, on every screen still open. */
    /** Draws the mod's settings screen again, when it is open. */
    public static void refreshModScreen() { SettingsActivity.refreshFromPlugin(); }

    public static void removeLayers(String pluginId) {
        Activity activity = Screen.now();
        if (activity == null) return;
        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        String tag = "ttcuz-plugin:" + pluginId;
        for (int i = decor.getChildCount() - 1; i >= 0; i--)
            if (tag.equals(decor.getChildAt(i).getTag(LAYER))) decor.removeViewAt(i);
    }

    /** M3 entrance: fade, rise and settle. */
    public static void animateIn(View view) {
        if (view == null) return;
        if (!Motion.enabled(view.getContext())) { view.setAlpha(1f); return; }
        float density = view.getResources().getDisplayMetrics().density;
        view.setAlpha(0f); view.setTranslationY(12 * density); view.setScaleX(.96f); view.setScaleY(.96f);
        view.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(Motion.time(320L)).setInterpolator(Motion.OUT).start();
    }

    public static void animateOut(final View view, final Runnable after) {
        if (view == null) return;
        if (!Motion.enabled(view.getContext())) { if (after != null) after.run(); return; }
        view.animate().alpha(0f).scaleX(.96f).scaleY(.96f).setDuration(Motion.time(220L)).setInterpolator(Motion.BOTH)
                .withEndAction(() -> { if (after != null) after.run(); }).start();
    }
}
