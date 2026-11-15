package cat.ttcuz.framework;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;

/**
 * Only Android 6-9 needs a runtime permission to create shared media.
 *
 * The same invisible screen asks for any other Android permission a plugin requests (extra "ttcuz_permission"):
 * one question at a time, answered to whoever asked.
 */
public final class StoragePermissionActivity extends Activity {
    public interface Answer { void answered(boolean granted); }
    private static final String EXTRA = "ttcuz_permission";
    private static final java.util.LinkedHashMap<String, ArrayList<Answer>> asked = new java.util.LinkedHashMap<>();
    private String asking;

    /** Asks for one permission (already granted: answers at once). Answers arrive on the main thread. */
    static void ask(Context context, String permission, Answer answer) {
        final Handler main = new Handler(Looper.getMainLooper());
        if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) { main.post(() -> answer.answered(true)); return; }
        main.post(() -> {
            boolean launch;
            synchronized (asked) {
                ArrayList<Answer> waiting = asked.get(permission);
                launch = waiting == null;
                if (waiting == null) asked.put(permission, waiting = new ArrayList<>());
                waiting.add(answer);
            }
            if (launch) try {
                context.startActivity(new Intent(context, StoragePermissionActivity.class).putExtra(EXTRA, permission).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Throwable error) { Diary.note("plugin permission: " + error); answer(permission, false); }
        });
    }

    private static void answer(String permission, boolean granted) {
        ArrayList<Answer> waiting;
        synchronized (asked) { waiting = asked.remove(permission); }
        if (waiting == null) return;
        for (Answer answer : waiting) try { answer.answered(granted); } catch (Throwable error) { Diary.note("plugin permission answer: " + error); }
    }
    private static final String WRITE=android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final ArrayList<Runnable> pending=new ArrayList<>();
    private static boolean opening;
    private boolean completed;
    static boolean ensure(Context context,Runnable retry){
        if(Build.VERSION.SDK_INT>=29 || context.checkSelfPermission(WRITE)==PackageManager.PERMISSION_GRANTED)return true;
        new Handler(Looper.getMainLooper()).post(()->{
            boolean launch;
            synchronized(pending){
                if(pending.size()>=16){Screen.say(Text.t("Дождитесь запроса доступа к файлам."));return;}
                pending.add(retry);launch=!opening;opening=true;
            }
            if(launch)try{
                context.startActivity(new Intent(context,StoragePermissionActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }catch(Throwable error){Diary.note("storage permission: "+error);complete(false);}
        });
        return false;
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        asking=getIntent()==null?null:getIntent().getStringExtra(EXTRA);
        if(asking!=null){
            if(checkSelfPermission(asking)==PackageManager.PERMISSION_GRANTED){completed=true;finish();answer(asking,true);return;}
            if(state==null)requestPermissions(new String[]{asking},7302);
            return;
        }
        synchronized(pending){if(pending.isEmpty()){completed=true;opening=false;finish();return;}}
        if(Build.VERSION.SDK_INT>=29 || checkSelfPermission(WRITE)==PackageManager.PERMISSION_GRANTED){completed=true;finish();complete(true);return;}
        if(state==null)requestPermissions(new String[]{WRITE},7301);
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(code,permissions,grants);
        if(code==7302&&asking!=null){completed=true;finish();answer(asking,checkSelfPermission(asking)==PackageManager.PERMISSION_GRANTED);return;}
        if(code!=7301)return;
        completed=true;finish();complete(checkSelfPermission(WRITE)==PackageManager.PERMISSION_GRANTED);
    }
    @Override protected void onDestroy(){
        if(!completed && !isChangingConfigurations()){if(asking!=null)answer(asking,false);else complete(false);}
        super.onDestroy();
    }
    private static void complete(boolean allowed){
        ArrayList<Runnable> jobs;
        synchronized(pending){jobs=new ArrayList<>(pending);pending.clear();opening=false;}
        if(!allowed){if(!jobs.isEmpty())Screen.say(Text.t("Для сохранения разрешите доступ к файлам в настройках приложения."));return;}
        for(Runnable job:jobs)try{job.run();}catch(Throwable error){Diary.note("storage retry: "+error);}
    }
}
