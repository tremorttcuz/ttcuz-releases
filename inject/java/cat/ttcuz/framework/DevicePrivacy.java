package cat.ttcuz.framework;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * What TikTok may read off the phone itself, part of ayumoments: the contacts, the list of installed apps, where the
 * phone is, and its Android ID.
 *
 * The build sends TikTok's calls to these framework methods here (dexpatch, MODEL_SOURCES / MODEL_STATICS). Each one
 * answers as the system would on a phone that has nothing to give -- no contacts, only TikTok installed, no location
 * provider, a permission not granted -- so no code path in TikTok meets something it was not written for. When a
 * switch is off the call goes through unchanged.
 */
public final class DevicePrivacy {
    private DevicePrivacy() {}

    static final String CONTACTS = "privacy_block_contacts", APPS = "privacy_block_apps",
            LOCATION = "privacy_block_location", ANDROID_ID = "privacy_random_android_id";
    private static final String OWN_ID = "privacy_android_id_value";

    private static volatile SharedPreferences store;

    private static SharedPreferences prefs() {
        SharedPreferences known = store;
        if (known != null) return known;
        Context context = Ttcuz.context();
        if (context == null) return null;
        return store = context.getSharedPreferences(Ttcuz.PREFS, Context.MODE_PRIVATE);
    }

    static boolean on(String key) {
        SharedPreferences prefs = prefs();
        return prefs != null && prefs.getBoolean(key, false);
    }

    static void set(String key, boolean value) {
        SharedPreferences prefs = prefs();
        if (prefs != null) prefs.edit().putBoolean(key, value).apply();
    }

    // ------------------------------------------------------------------ Android ID

    /** Settings.Secure.getString: the Android ID is a random one made once for this install. */
    public static String getString(ContentResolver resolver, String name) {
        if ("android_id".equals(name) && on(ANDROID_ID)) return randomId();
        return android.provider.Settings.Secure.getString(resolver, name);
    }

    private static synchronized String randomId() {
        SharedPreferences prefs = prefs();
        String id = prefs == null ? null : prefs.getString(OWN_ID, null);
        if (id == null || id.length() != 16) {
            java.security.SecureRandom random = new java.security.SecureRandom();
            StringBuilder hex = new StringBuilder(16);
            // a real Android ID never starts with a zero nibble in practice; keep the shape familiar
            hex.append("0123456789abcdef".charAt(1 + random.nextInt(15)));
            for (int i = 1; i < 16; i++) hex.append("0123456789abcdef".charAt(random.nextInt(16)));
            id = hex.toString();
            if (prefs != null) prefs.edit().putString(OWN_ID, id).apply();
        }
        return id;
    }

    // ------------------------------------------------------------------ contacts

    private static boolean contacts(Uri uri) {
        if (uri == null || !on(CONTACTS)) return false;
        String authority = uri.getAuthority();
        return authority != null && (authority.equals("com.android.contacts") || authority.equals("contacts")
                || authority.equals("icc") || authority.startsWith("com.android.contacts."));
    }

    private static Cursor empty(String[] projection) {
        return new MatrixCursor(projection != null ? projection : new String[]{"_id"}, 0);
    }

    public static Cursor query(ContentResolver resolver, Uri uri, String[] projection, String selection,
                               String[] arguments, String order) {
        if (contacts(uri)) return empty(projection);
        return resolver.query(uri, projection, selection, arguments, order);
    }

    public static Cursor query(ContentResolver resolver, Uri uri, String[] projection, String selection,
                               String[] arguments, String order, android.os.CancellationSignal cancel) {
        if (contacts(uri)) return empty(projection);
        return resolver.query(uri, projection, selection, arguments, order, cancel);
    }

    public static Cursor query(ContentResolver resolver, Uri uri, String[] projection, android.os.Bundle arguments,
                               android.os.CancellationSignal cancel) {
        if (contacts(uri)) return empty(projection);
        return resolver.query(uri, projection, arguments, cancel);
    }

    // ------------------------------------------------------------------ installed apps

    /** Only TikTok itself, as the list looks to an app that may not see the others (Android 11's default). */
    private static List own(PackageManager manager, boolean packages, int flags) {
        List out = new ArrayList();
        Context context = Ttcuz.context();
        if (context == null) return out;
        try {
            if (packages) out.add(manager.getPackageInfo(context.getPackageName(), flags));
            else out.add(manager.getApplicationInfo(context.getPackageName(), flags));
        } catch (Throwable ignored) { }
        return out;
    }

    /**
     * PackageManager.queryIntentActivities. TikTok asks the launcher's list (MAIN + LAUNCHER) to learn what is
     * installed; that one answers with TikTok alone. Every other question (who can share, open a link) goes through.
     */
    public static List queryIntentActivities(PackageManager manager, android.content.Intent intent, int flags) {
        if (on(APPS) && intent != null && android.content.Intent.ACTION_MAIN.equals(intent.getAction())
                && intent.hasCategory(android.content.Intent.CATEGORY_LAUNCHER) && intent.getPackage() == null
                && intent.getComponent() == null) {
            List out = new ArrayList();
            Context context = Ttcuz.context();
            if (context == null) return out;
            for (Object found : manager.queryIntentActivities(intent, flags)) {
                android.content.pm.ResolveInfo info = (android.content.pm.ResolveInfo) found;
                if (info.activityInfo != null && context.getPackageName().equals(info.activityInfo.packageName)) out.add(info);
            }
            return out;
        }
        return manager.queryIntentActivities(intent, flags);
    }

    // ------------------------------------------------------------------ location

    public static Location getLastKnownLocation(LocationManager manager, String provider) {
        return on(LOCATION) ? null : manager.getLastKnownLocation(provider);
    }

    public static boolean isProviderEnabled(LocationManager manager, String provider) {
        return !on(LOCATION) && manager.isProviderEnabled(provider);
    }

    public static boolean isLocationEnabled(LocationManager manager) {
        return !on(LOCATION) && manager.isLocationEnabled();
    }

    /** No answer ever comes, as when no provider is on. */
    public static void requestSingleUpdate(LocationManager manager, String provider, android.location.LocationListener listener,
                                           android.os.Looper looper) {
        if (!on(LOCATION)) manager.requestSingleUpdate(provider, listener, looper);
    }

    // ------------------------------------------------------------------ the permissions behind them

    /** A blocked kind of data reads as a permission that was never granted, so TikTok does not even try. */
    private static boolean denied(String permission) {
        if (permission == null) return false;
        switch (permission) {
            case "android.permission.READ_CONTACTS":
            case "android.permission.WRITE_CONTACTS":
            case "android.permission.GET_ACCOUNTS":
                return on(CONTACTS);
            case "android.permission.ACCESS_FINE_LOCATION":
            case "android.permission.ACCESS_COARSE_LOCATION":
            case "android.permission.ACCESS_BACKGROUND_LOCATION":
                return on(LOCATION);
            case "android.permission.QUERY_ALL_PACKAGES":
                return on(APPS);
            default:
                return false;
        }
    }

    /** ContextCompat.checkSelfPermission and the like: (Context, String) -> int. */
    public static int checkSelfPermission(Context context, String permission) {
        if (denied(permission)) return PackageManager.PERMISSION_DENIED;
        return context.checkSelfPermission(permission);
    }

    /** The same asked through a ContextWrapper reference. */
    public static int checkSelfPermission(android.content.ContextWrapper context, String permission) {
        if (denied(permission)) return PackageManager.PERMISSION_DENIED;
        return context.checkSelfPermission(permission);
    }
}
