package cat.narezany.margyt;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.zip.ZipFile;

/** Reject incompatible downloads before handing them to the system installer. */
final class UpdatePackage {
    static void check(Context context,File apk)throws Exception{
        if(!apk.isFile() || apk.length()==0)throw new IOException("Файл обновления пуст.");
        try(ZipFile zip=new ZipFile(apk)){
            if(zip.getEntry("AndroidManifest.xml")==null || zip.getEntry("classes.dex")==null)
                throw new IOException("Сервер вернул файл, который не является APK.");
            boolean nativeCode=false,supported=false;
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries=zip.entries();
            while(entries.hasMoreElements()){
                String path=entries.nextElement().getName();
                if(!path.startsWith("lib/") || !path.endsWith(".so"))continue;
                nativeCode=true;
                for(String abi:Build.SUPPORTED_ABIS)if(path.startsWith("lib/"+abi+"/"))supported=true;
            }
            if(nativeCode && !supported)throw new IOException("В APK нет версии для процессора этого телефона.");
        }catch(java.util.zip.ZipException error){throw new IOException("Файл обновления повреждён. Повторите загрузку.",error);}
        PackageManager manager=context.getPackageManager();
        int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
        PackageInfo next=manager.getPackageArchiveInfo(apk.getAbsolutePath(),flags);
        if(next==null || !context.getPackageName().equals(next.packageName))throw new IOException("Этот файл не является обновлением установленного приложения.");
        if(Build.VERSION.SDK_INT>=24 && next.applicationInfo!=null && next.applicationInfo.minSdkVersion>Build.VERSION.SDK_INT)
            throw new IOException("Обновление требует более новой версии Android.");
        PackageInfo installed=manager.getPackageInfo(context.getPackageName(),flags);
        long nextCode=Build.VERSION.SDK_INT>=28?next.getLongVersionCode():next.versionCode;
        long currentCode=Build.VERSION.SDK_INT>=28?installed.getLongVersionCode():installed.versionCode;
        if(nextCode<currentCode)throw new IOException("Сервер предложил более старую сборку приложения.");
        if(!sameSigner(installed,next))throw new IOException("Подпись обновления отличается от установленной сборки. Нужен APK с той же подписью.");
    }
    private static boolean sameSigner(PackageInfo old,PackageInfo next){
        if(Build.VERSION.SDK_INT>=28){
            if(old.signingInfo==null || next.signingInfo==null)return false;
            Signature[] current=old.signingInfo.getApkContentsSigners();
            Signature[] incoming=next.signingInfo.getApkContentsSigners();
            if(equalSigners(current,incoming))return true;
            if(old.signingInfo.hasMultipleSigners() || next.signingInfo.hasMultipleSigners() || current==null || current.length!=1)return false;
            Signature[] history=next.signingInfo.getSigningCertificateHistory();
            return history!=null && Arrays.asList(history).contains(current[0]);
        }
        return equalSigners(old.signatures,next.signatures);
    }
    private static boolean equalSigners(Signature[] a,Signature[] b){
        return a!=null && b!=null && a.length>0 && a.length==b.length
                && new HashSet<>(Arrays.asList(a)).equals(new HashSet<>(Arrays.asList(b)));
    }
}
