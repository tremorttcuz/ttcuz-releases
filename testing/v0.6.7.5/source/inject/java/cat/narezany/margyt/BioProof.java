package cat.narezany.margyt;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.concurrent.atomic.AtomicBoolean;

/** Persist before the first save, restore only our exact temporary value. */
final class BioProof {
    private static final AtomicBoolean busy=new AtomicBoolean();
    private static final String PREFIX="bio_proof_";
    private static volatile String status="";
    static String status(){return status;}
    static String original(String uid){return prefs().getString(PREFIX+uid+"_original",null);}
    private static long recoverAt;
    static String temporary(String original,String code) {
        String result=original.length()==0 ? code : original+"\n"+code;
        // Native 47.2.41 validates 160 UTF-16 characters and at most four newlines.
        if (result.length()>160 || result.replace("\n","").length()<result.length()-4)
            throw new IllegalArgumentException("В описании не хватает места для кода. Используйте ручное подтверждение.");
        return result;
    }
    private static SharedPreferences prefs() {
        return Margy.context().getSharedPreferences(Margy.PREFS,Context.MODE_PRIVATE);
    }
    static boolean pending(String uid) {return uid!=null && prefs().contains(PREFIX+uid+"_original");}
    static void start() {
        final String uid=Account.liveId();
        if (uid==null) {
            Screen.say("Сначала откройте свой профиль TikTok.");return;
        }
        if (!busy.compareAndSet(false,true)) return;
        Net.away("automatic bio proof",() -> {
            try {
                String handle=NativeBio.handle(uid);
                if (handle==null || !handle.matches("[A-Za-z0-9_.]{2,24}"))
                    throw new IllegalArgumentException("Не удалось прочитать @ник текущего аккаунта. Откройте свой профиль.");
                CloudProfileProvider cloud=new CloudProfileProvider(Margy.context());
                if(cloud.verified(uid) && !pending(uid)) {
                    if(uid.equals(Account.liveId()))ProfileStyle.share(true);
                    status="Профиль уже подтверждён.";notifyVerified(uid);return;
                }
                String key=PREFIX+uid;
                if (pending(uid) && prefs().getBoolean(key+"_confirmed",false)
                        && prefs().getString(key+"_original","").equals(NativeBio.bio(uid))) clear(key);
                if (!pending(uid)) {
                    String original=NativeBio.bio(uid),extra=NativeBio.extras(uid);
                    String next=temporary(original,cloud.code(uid));
                    // commit: original text must survive app/process shutdown before changing the profile.
                    if (!prefs().edit().putString(key+"_original",original).putString(key+"_temporary",next)
                            .putString(key+"_extra",extra).commit()) throw new IllegalStateException("Не удалось сохранить описание");
                }
                String original=prefs().getString(key+"_original",""),next=prefs().getString(key+"_temporary","");
                String current=NativeBio.bio(uid);
                if (original.equals(current)) {
                    status="Добавляю код в описание…";
                    if (!NativeBio.save(uid,original,next,prefs().getString(key+"_extra","null"))) {
                        status="Сохранение не подтверждено. Исходное описание сохранено; можно повторить проверку.";
                        return;
                    }
                } else if (!next.equals(current)) {
                    status="Описание изменилось. Проверка остановлена; прежний текст сохранён.";
                    return;
                }
                prefs().edit().putBoolean(key+"_confirmed",true).commit();
                boolean verified=cloud.verified(uid);
                status="Проверяю профиль…";
                for (int attempt=0;attempt<24 && !verified && uid.equals(Account.liveId());attempt++) {
                    try {verified=cloud.verify(uid,handle);} catch (java.io.IOException error) {
                        Diary.note("bio verification attempt "+(attempt+1)+": "+error);
                        status="Сервер временно недоступен. Повторяю проверку…";
                    }
                    if (!verified && attempt<23 && uid.equals(Account.liveId())) Thread.sleep(5000L);
                }
                if (verified) prefs().edit().putBoolean(key+"_verified",true).commit();
                if (verified && uid.equals(Account.liveId())) ProfileStyle.share(true);
                restore(uid);
                if (!pending(uid)) status=verified ? "Профиль подтверждён. Описание восстановлено." : "Проверка не завершена. Описание восстановлено; можно повторить.";
            } catch (Throwable error) {
                status=error instanceof IllegalArgumentException ? error.getMessage() : "Проверка прервана. Исходное описание сохранено.";
                Diary.note("automatic bio proof: "+error);
                try {if (pending(uid))restore(uid);} catch (Throwable ignored){}
            } finally {
                busy.set(false);
                scheduleRecovery(uid, 0);
                Diary.note("bio proof: "+status);ProfilePreview.accountUpdated();
            }
        });
    }
    /** Repair late native saves without depending on reopening the profile. */
    private static void scheduleRecovery(final String uid, final int attempt) {
        if (attempt>=4 || !pending(uid)) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!uid.equals(Account.liveId()) || !pending(uid)) return;
            recover();
            scheduleRecovery(uid, attempt+1);
        }, 30000L);
    }
    /** Recovery on returning to the same account; does not restart a public edit. */
    static void recover() {
        final String uid=Account.liveId();
        if (uid==null || !pending(uid)) return;
        synchronized (BioProof.class) {
            long now=System.currentTimeMillis();
            if (now-recoverAt<30000L || !busy.compareAndSet(false,true)) return;
            recoverAt=now;
        }
        Net.away("bio recovery",() -> {
            try {restore(uid);} catch (Throwable error) {Diary.note("bio recovery: "+error);}
            finally {busy.set(false);}
        });
    }
    private static void restore(String uid) throws Exception {
        if (!uid.equals(Account.liveId())) {status="Вернитесь в исходный аккаунт для восстановления описания.";return;}
        String key=PREFIX+uid,original=prefs().getString(key+"_original",null),next=prefs().getString(key+"_temporary",null);
        if (original==null || next==null) return;
        String current=NativeBio.bio(uid);
        if (original.equals(current)) {
            if (prefs().getBoolean(key+"_confirmed",false)) clear(key);
            else status="Ожидаю подтверждение сохранения. Резервная копия описания сохранена.";
            return;
        }
        if (!next.equals(current)) {status="Описание изменилось во время проверки. Автоматическая замена остановлена; резервная копия сохранена.";return;}
        status="Восстанавливаю описание…";
        if (NativeBio.save(uid,next,original,prefs().getString(key+"_extra","null"))) clear(key);
        else status="Восстановление пока не подтверждено. Повторю при возвращении в аккаунт.";
    }
    private static void notifyVerified(String uid){
        if(uid==null||!uid.equals(Account.liveId())||prefs().getBoolean(PREFIX+uid+"_notified",false))return;
        prefs().edit().putBoolean(PREFIX+uid+"_notified",true).commit();VerificationNotice.success();
    }
    private static void clear(String key) {
        if(prefs().getBoolean(key+"_verified",false))notifyVerified(key.substring(PREFIX.length()));
        prefs().edit().remove(key+"_original").remove(key+"_temporary").remove(key+"_extra").remove(key+"_confirmed").remove(key+"_verified").commit();
    }
}
