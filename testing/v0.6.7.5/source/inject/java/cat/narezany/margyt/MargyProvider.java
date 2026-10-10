package cat.narezany.margyt;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;

/**
 * The mod's way into a running TikTok, and it is not a patch at all.
 *
 * Android instantiates every content provider an app declares before the
 * application's own onCreate, whether anything ever queries it or not. So a
 * provider that answers nothing is a start-up hook TikTok's code knows nothing
 * about and cannot move: no Application class to patch, no method to find again
 * after the next release.
 *
 * All it does is remember the context and ask to be told when an activity
 * appears.
 */
public final class MargyProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context == null) return true;
        Margy.attach(context);
        try { CrashReports.start(context); } catch(Throwable ignored) {}
        try { TtcuzProfileSync.start(context); } catch(Throwable e) { Diary.note("profile start: "+e); }
        SocialLibrary.start();
        Diary.note("start-up hook ran");
        try {
            Context application = context.getApplicationContext();
            if (application instanceof Application) {
                Launcher.watch((Application) application);
                ((Application) application).registerActivityLifecycleCallbacks(new SettingsRow());
                Diary.note("watching for the settings screen");
            } else {
                Diary.note("no application yet: " + application);
            }
        } catch (Throwable error) {
            // the mod failing to start is not a reason for the app not to
            Diary.note("hook failed: " + error);
        }
        try {
            Badges.start(context);
        } catch (Throwable error) {
            Diary.note("badges failed to start: " + error);
        }
        try {
            Streaks.start(context);
        } catch (Throwable error) {
            Diary.note("streaks failed to start: " + error);
        }
        try {
            Updater.start(context);
        } catch (Throwable error) {
            Diary.note("updates failed to start: " + error);
        }
        try {
            Plugins.startAll(context);
        } catch (Throwable error) {
            Diary.note("plugins failed to start: " + error);
        }
        return true;
    }

    // ------------------------------------------------- handing over a file

    /**
     * A content uri for a file of the mod's own.
     *
     * Android will not install an apk from a path any more; it wants a uri it
     * can be granted read on. A FileProvider is the usual answer and it needs
     * an xml resource, which this build cannot add -- but a provider is a
     * provider, and this one is already declared, so it serves the file
     * itself.
     */
    public static Uri share(Context context, File file) {
        try {
            if (!file.isFile()) return null;
            return Uri.parse("content://" + context.getPackageName() + ".margyt/"
                    + file.getName());
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        try {
            Context context = getContext();
            if (context == null) return null;
            String name = uri.getLastPathSegment();
            // one directory, no traversal, read only: the installer needs the
            // update and has no business anywhere else
            if (name == null || name.contains("/") || name.contains("..")) return null;
            File file = new File(new File(context.getFilesDir(), "margyt"), name);
            if (!file.isFile()) return null;
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (Throwable error) {
            Diary.note("share: " + error);
            return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        File file=sharedFile(uri);if(file==null)return null;
        String[] columns=projection==null?new String[]{android.provider.OpenableColumns.DISPLAY_NAME,android.provider.OpenableColumns.SIZE}:projection;
        android.database.MatrixCursor cursor=new android.database.MatrixCursor(columns);
        Object[] values=new Object[columns.length];
        for(int i=0;i<columns.length;i++){
            if(android.provider.OpenableColumns.DISPLAY_NAME.equals(columns[i]))values[i]=file.getName();
            else if(android.provider.OpenableColumns.SIZE.equals(columns[i]))values[i]=file.length();
        }
        cursor.addRow(values);return cursor;
    }

    private File sharedFile(Uri uri){
        Context context=getContext();if(context==null || uri==null)return null;
        String name=uri.getLastPathSegment();
        if(name==null || name.contains("/") || name.contains(".."))return null;
        File file=new File(new File(context.getFilesDir(),"margyt"),name);
        return file.isFile()?file:null;
    }
    @Override
    public String getType(Uri uri) {
        File file=sharedFile(uri);if(file==null)return null;
        return file.getName().endsWith(".apk")?"application/vnd.android.package-archive":file.getName().endsWith(".zip")?"application/zip":"application/octet-stream";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        return 0;
    }
}
