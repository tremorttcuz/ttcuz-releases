package cat.ttcuz.framework;
import android.os.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;
/** Account-scoped, bounded reads through TikTok's signed-in friends API. */
final class SocialLibrary {
 private static final Handler main=new Handler(Looper.getMainLooper());
 private static final java.util.concurrent.atomic.AtomicBoolean busy=new java.util.concurrent.atomic.AtomicBoolean();
 private static String account;private static long last;private static boolean started;
 /** Failed loads in a row: the pause after a failure doubles from 5 minutes up to an hour. */
 private static int failures;
 /** The minute timer looks only while TikTok is on screen; in the background it does nothing at all. */
 static synchronized void start(){if(started)return;started=true;main.postDelayed(new Runnable(){public void run(){if(Screen.now()!=null){NativeStreaks.prepare();refresh(null);}main.postDelayed(this,60000);}},10000);}
 private static long pause(){return Math.min(3600000L,300000L<<Math.min(failures,4));}
 /** Loads friends and their chats now, ignoring the five-minute pause (a screen that needs them asked). */
 static void refreshNow(Runnable ready){synchronized(SocialLibrary.class){last=0;failures=0;}refresh(ready);}
 static void refresh(Runnable ready){String uid=Account.liveId();if(uid==null)return;
  synchronized(SocialLibrary.class){if(!uid.equals(account)){account=uid;last=0;PinnedFriends.reload();}if(last!=0&&android.os.SystemClock.uptimeMillis()-last<pause())return;if(!busy.compareAndSet(false,true))return;last=android.os.SystemClock.uptimeMillis();}
  main.post(()->{try{FavouriteStickers.load(ready);}catch(Throwable e){Diary.note("sticker library: "+e);}});
  Net.away("friends-library",()->{try{load(uid,ready);synchronized(SocialLibrary.class){failures=0;}}catch(Throwable e){
    // the pause grows instead of being reset: a failing list was asked for again every minute, for hours
    synchronized(SocialLibrary.class){failures++;}
    Diary.note("friends library: "+e.getClass().getSimpleName()+(e.getMessage()==null?"":": "+e.getMessage())+"; next try in "+pause()/60000+" min");
  }finally{busy.set(false);}});
 }
/** Kotlin's EmptyCoroutineContext. TikTok 47.2.41 ships Kotlin renamed: it is X.03Xi there. */
 private static Object emptyContext()throws Exception{
  for(String name:new String[]{"X.03Xi","kotlin.coroutines.EmptyCoroutineContext"}){
   try{return Class.forName(name).getField("INSTANCE").get(null);}catch(ClassNotFoundException|NoSuchFieldException ignored){}
  }
  throw new ClassNotFoundException("EmptyCoroutineContext");
 }
 static void load(String uid,Runnable ready)throws Exception{
  Class<?> api=Class.forName("X.0rQq"),continuation=Class.forName("X.03ve");Object service=api.getField("LIZIZ").get(null);
  Object scene=Class.forName("X.0sd4").getField("TT_RELATION_PAGE").get(null);int source=((Number)scene.getClass().getMethod("getValue").invoke(scene)).intValue();
  Object context=emptyContext();
  // the mutual-friends list: found by its parameters (scene, count, sec_user_id, page_token, continuation), not by
  // its name -- 47.7.3 calls it LIZJ where 47.2.41 had LIZIZ, and the name moves again with every release
  Method request=null;
  for(Method m:api.getMethods()){Class<?>[] p=m.getParameterTypes();
   if(p.length==5&&p[0]==int.class&&p[1]==int.class&&p[2]==String.class&&p[3]==String.class&&p[4]==continuation){request=m;break;}}
  if(request==null)throw new NoSuchMethodException("friends list request");
  Object accountService=Class.forName("X.041W").getMethod("LJFF").invoke(null);
  if(!uid.equals(NativeRead.string(NativeRead.get(accountService,"getCurUserId"))))return;
  String sec=NativeRead.string(NativeRead.get(accountService,"getCurSecUserId")),page="";Set<String> tokens=new HashSet<>();
  if(sec.length()==0)throw new java.io.IOException("account service not ready");
  for(int count=0;count<20&&uid.equals(Account.liveId());count++){
   CountDownLatch latch=new CountDownLatch(1);Object[] answer=new Object[1];
   Object callback=Proxy.newProxyInstance(continuation.getClassLoader(),new Class<?>[]{continuation},(proxy,m,args)->{
    if(m.getName().equals("getContext"))return context;
    if(m.getName().equals("resumeWith")){answer[0]=args[0];latch.countDown();return null;}
    if(m.getName().equals("toString"))return "ttcuz-friends";if(m.getName().equals("hashCode"))return System.identityHashCode(proxy);if(m.getName().equals("equals"))return proxy==args[0];return null;});
   Object immediate=request.invoke(service,source,30,sec,page,callback);
   if(immediate!=null&&immediate.getClass().getName().equals("com.ss.android.ugc.aweme.relation.model.FriendsListResponse"))answer[0]=immediate;
   else if(!latch.await(20,TimeUnit.SECONDS))throw new java.io.IOException("friends timeout");
   Object response=answer[0];if(response==null||!response.getClass().getName().equals("com.ss.android.ugc.aweme.relation.model.FriendsListResponse"))throw new java.io.IOException("friends response unavailable");
   if(!uid.equals(Account.liveId()))return;Object users=NativeRead.get(response,"getUserList");if(users instanceof List){PinnedFriends.observe((List)users);NativeStreaks.friends((List)users,uid);}
   if(ready!=null)main.post(()->{if(uid.equals(Account.liveId()))ready.run();});
   if(!Boolean.TRUE.equals(NativeRead.field(response,"hasMore")))break;
   String next=NativeRead.string(NativeRead.field(response,"nextPageToken"));if(next.isEmpty()||!tokens.add(next))break;page=next;
  }
 }
}
