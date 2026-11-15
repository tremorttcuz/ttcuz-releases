package cat.ttcuz.framework;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;

final class SheetPost {
    private static final String AWEME = "com.ss.android.ugc.aweme.feed.model.Aweme";

    private SheetPost() {
    }

    /* JADX WARN: Code restructure failed: missing block: B:18:0x0028, code lost:
    
        r1 = "";
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    static Object of(Dialog dialog) {
        try {
            Activity ownerActivity = dialog.getOwnerActivity();
            Context context = dialog.getContext();
            for (int i = 0; ownerActivity == null && i < 12 && (context instanceof ContextWrapper); i++) {
                if (context instanceof Activity) {
                    ownerActivity = (Activity) context;
                } else {
                    context = ((ContextWrapper) context).getBaseContext();
                }
            }
            if (ownerActivity == null) return null;
            String name = ownerActivity.getClass().getName();
            if (name.equals("com.ss.android.ugc.aweme.main.MainActivity") || name.equals("com.ss.android.ugc.aweme.detail.ui.DetailActivity")) {
                return aweme(dialog, 0, new IdentityHashMap());
            }
            return null;
        } catch (Throwable th) {
            Diary.note("download sheet post: " + String.valueOf(th));
            return null;
        }
    }

    private static Object aweme(Object obj, int i, IdentityHashMap<Object, Boolean> identityHashMap) {
        Object aweme;
        if (obj != null && i <= 2 && identityHashMap.put(obj, Boolean.TRUE) == null) {
            for (Class<?> cls = obj.getClass(); cls != null && cls != Object.class && cls != Dialog.class; cls = cls.getSuperclass()) {
                for (Field field : cls.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                        Class<?> type = field.getType();
                        if (!View.class.isAssignableFrom(type) && !Context.class.isAssignableFrom(type) && type != String.class) {
                            try {
                                field.setAccessible(true);
                                Object obj2 = field.get(obj);
                                if (obj2 != null) {
                                    if (obj2.getClass().getName().equals(AWEME)) {
                                        return obj2;
                                    }
                                    String name = obj2.getClass().getName();
                                    if (i < 2 && ((name.startsWith("com.ss.") || name.startsWith("X.")) && (aweme = aweme(obj2, i + 1, identityHashMap)) != null)) {
                                        return aweme;
                                    }
                                }
                            } catch (Throwable unused) {
                                continue;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
