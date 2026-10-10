package cat.narezany.margyt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;

/** Only Android 6-9 needs a runtime permission to create shared media. */
public final class StoragePermissionActivity extends Activity {
    private static final String WRITE=android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final ArrayList<Runnable> pending=new ArrayList<>();
    private static boolean opening;
    private boolean completed;
    static boolean ensure(Context context,Runnable retry){
        if(Build.VERSION.SDK_INT>=29 || context.checkSelfPermission(WRITE)==PackageManager.PERMISSION_GRANTED)return true;
        new Handler(Looper.getMainLooper()).post(()->{
            boolean launch;
            synchronized(pending){
                if(pending.size()>=16){Screen.say("Дождитесь запроса доступа к файлам.");return;}
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
        synchronized(pending){if(pending.isEmpty()){completed=true;opening=false;finish();return;}}
        if(Build.VERSION.SDK_INT>=29 || checkSelfPermission(WRITE)==PackageManager.PERMISSION_GRANTED){completed=true;finish();complete(true);return;}
        if(state==null)requestPermissions(new String[]{WRITE},7301);
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(code,permissions,grants);
        if(code!=7301)return;
        completed=true;finish();complete(checkSelfPermission(WRITE)==PackageManager.PERMISSION_GRANTED);
    }
    @Override protected void onDestroy(){
        if(!completed && !isChangingConfigurations())complete(false);
        super.onDestroy();
    }
    private static void complete(boolean allowed){
        ArrayList<Runnable> jobs;
        synchronized(pending){jobs=new ArrayList<>(pending);pending.clear();opening=false;}
        if(!allowed){if(!jobs.isEmpty())Screen.say("Для сохранения разрешите доступ к файлам в настройках приложения.");return;}
        for(Runnable job:jobs)try{job.run();}catch(Throwable error){Diary.note("storage retry: "+error);}
    }
}
