package cat.narezany.margyt;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.util.zip.*;

/** Uses the verified creator's per-device identity, never a global secret in the APK. */
final class PluginAdmin {
 private PluginAdmin(){}
 static JSONObject call(Context c,JSONObject body)throws Exception{
  String uid=Account.activeId();if(!AdminAccess.allowed(uid))throw new Exception("Нужен аккаунт создателя");
  JSONObject identity=new CloudProfileProvider(c).identity(uid);
  body.put("uid",uid).put("token",identity.getString("token"));
  String reply=Net.postPlugin(body.toString());if(reply==null)throw new Exception("Сервер недоступен");
  JSONObject result=new JSONObject(reply);if(result.has("error"))throw new Exception(result.optString("error").equals("creator_verification_required")?"Подтвердите профиль создателя в настройках оформления":result.getString("error"));
  return result;
 }
 static byte[] pack(Plugins.Info info,JSONObject manifest,byte[] icon)throws Exception{
  PluginAudit.Report report=PluginAudit.folder(info.folder);if(report.blocked)throw new Exception(report.summary());
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();
  try(ZipOutputStream zip=new ZipOutputStream(bytes)){
   entry(zip,"manifest.json",manifest.toString().getBytes("UTF-8"));
   entry(zip,"classes.dex",PluginAudit.read(new File(info.folder,"classes.dex"),PluginAudit.MAX_DEX));
   if(icon!=null)entry(zip,"icon.png",icon);
  }
  return bytes.toByteArray();
 }
 private static void entry(ZipOutputStream zip,String name,byte[] value)throws Exception{zip.putNextEntry(new ZipEntry(name));zip.write(value);zip.closeEntry();}
 static void publish(Context c,ScriptPlugins.Program program){
  task(c,()->call(c,new JSONObject().put("action","publish-script").put("source",program.source)),"Плагин опубликован");
 }
 interface Job{JSONObject run()throws Exception;}
 static void task(Context c,Job job,String success){Net.away("plugin admin",()->{try{JSONObject result=job.run();if(!result.optBoolean("ok"))throw new Exception("Сервер не подтвердил изменение");ScriptPlugins.ui(()->Screen.say(success));}catch(Exception e){ScriptPlugins.ui(()->Popup.show(c,"Магазин",e.getMessage()));}});}
 static void show(Context c){
  Net.away("plugin administration",()->{try{
   JSONObject result=call(c,new JSONObject().put("action","list"));
   ScriptPlugins.ui(()->{try{
    Panel panel=Panel.with(c,Skin.remembered(c),"Управление магазином").compact();
    panel.primary("Добавить файл .ctt",()->PluginEditorActivity.file(c));
    panel.quiet("Создать плагин из кнопок",()->PluginEditorActivity.script(c,ScriptPlugins.SAMPLE));
    JSONArray scripts=result.getJSONArray("scripts"),packages=result.getJSONArray("packages");
    if(scripts.length()+packages.length()==0)panel.text("В магазине пока нет плагинов");
    for(int i=0;i<scripts.length();i++){JSONObject row=scripts.getJSONObject(i);ScriptPlugins.Program p=ScriptPlugins.check(row.getString("source"));panel.quiet(p.name+(row.optString("state").equals("pending")?" · на проверке":""),()->{
     Panel actions=Panel.with(c,Skin.remembered(c),p.name).compact();
     actions.primary("Редактировать",()->PluginEditorActivity.script(c,p.source.toString()));
     actions.quiet("Опубликовать",()->publish(c,p));
     actions.quiet("Удалить из магазина",()->remove(c,"script",p.id));actions.quiet(Text.CLOSE,null);actions.show();
    });}
    for(int i=0;i<packages.length();i++){JSONObject row=packages.getJSONObject(i);String id=row.getString("id");panel.quiet(row.getString("name"),()->{Panel actions=Panel.with(c,Skin.remembered(c),row.optString("name")).compact();actions.text("Чтобы обновить плагин, загрузите новый файл .ctt с тем же ID.");actions.primary("Загрузить новый .ctt",()->PluginEditorActivity.file(c));actions.quiet("Удалить из магазина",()->remove(c,"package",id));actions.quiet(Text.CLOSE,null);actions.show();});}
    panel.quiet(Text.CLOSE,null);panel.show();
   }catch(Exception e){Screen.say(e.getMessage());}});
  }catch(Exception e){ScriptPlugins.ui(()->Popup.show(c,"Магазин",e.getMessage()));}});
 }
 static void remove(Context c,String kind,String id){Popup.ask(c,"Удалить из магазина?",id,"Удалить",()->task(c,()->call(c,new JSONObject().put("action","remove").put("kind",kind).put("id",id)),"Плагин удалён из магазина"),Text.CANCEL,null,null,null);}
}
