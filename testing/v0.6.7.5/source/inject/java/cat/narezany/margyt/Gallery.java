package cat.narezany.margyt;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.OutputStream;

/**
 * Putting a file where the gallery will find it.
 *
 * Two ways, because Android changed its mind in the middle: on anything recent
 * a row in MediaStore, which needs no permission because the app is writing
 * its own entry, and on older ones a file in Pictures plus a word to the
 * scanner. TikTok itself holds the storage permission an older Android wants,
 * and the mod runs inside it.
 */
public final class Gallery {

    private Gallery() {}

    public static final String FOLDER = "ttcuz";

    /** Returns where it landed, or null. Never throws. */
    public static String save(Context context, byte[] data, String name, String mime) {
        if (data == null || data.length == 0) return null;
        try {
            File folder=context.getCacheDir();
            File temporary=File.createTempFile("ttcuz-gallery-",".tmp",folder);
            try{
                if(!Net.save(temporary,data))return null;
                String where=MediaFiles.save(context,temporary,name,mime,null);
                return where+"/"+name;
            }finally{temporary.delete();}
        } catch (Throwable error) {
            Diary.note("gallery: " + error);
            return null;
        }
    }

    /** A name nothing else will take, ending in the right extension. */
    public static String name(String prefix, String url, String fallback) {
        String extension = fallback;
        try {
            String path = url;
            int question = path.indexOf('?');
            if (question > 0) path = path.substring(0, question);
            int dot = path.lastIndexOf('.');
            if (dot > 0 && path.length() - dot <= 5) extension = path.substring(dot + 1);
        } catch (Throwable ignored) {
        }
        return prefix + "_" + System.currentTimeMillis() + "." + extension;
    }
}
