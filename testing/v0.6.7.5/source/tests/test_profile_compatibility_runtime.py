"""Run archived readers and patched badge parsers on responses from the current Worker."""
import pathlib,json,re,unittest
import test_settings_safety_runtime as safety
ROOT=pathlib.Path(__file__).resolve().parents[1]
F=ROOT/'tests/fixtures/profile-compat'
def java(value):
 if isinstance(value,dict):return 'org.json.JSONObject.object('+','.join(java(k)+','+java(v) for k,v in value.items())+')'
 if isinstance(value,list):return 'new org.json.JSONArray(new Object[]{'+','.join(java(v) for v in value)+'})'
 if value is None:return 'null'
 if isinstance(value,bool):return str(value).lower()
 if isinstance(value,str):return json.dumps(value,ensure_ascii=False)
 return str(value)+('L' if abs(value)>2147483647 else '')
JSON_STUB='''package org.json;public class JSONObject{
 public static java.util.Map<String,JSONObject> documents=new java.util.HashMap<>();java.util.Map<String,Object> values=new java.util.LinkedHashMap<>();
 public JSONObject(){}public JSONObject(String text){JSONObject o=documents.get(text);if(o==null)throw new IllegalArgumentException("unknown fixture JSON");values=o.values;}
 public static JSONObject object(Object...kv){JSONObject o=new JSONObject();for(int i=0;i<kv.length;i+=2)o.values.put((String)kv[i],kv[i+1]);return o;}
 public JSONObject put(String k,Object v){values.put(k,v);return this;}public boolean isNull(String k){return values.get(k)==null;}
 public String getString(String k){return(String)values.get(k);}public int getInt(String k){return((Number)values.get(k)).intValue();}public long getLong(String k){return((Number)values.get(k)).longValue();}
 public String optString(String k,String d){Object v=values.get(k);return v instanceof String?(String)v:d;}
 public int optInt(String k){return optInt(k,0);}public int optInt(String k,int d){Object v=values.get(k);return v instanceof Number?((Number)v).intValue():d;}
 public long optLong(String k,long d){Object v=values.get(k);return v instanceof Number?((Number)v).longValue():d;}
 public boolean optBoolean(String k){return optBoolean(k,false);}public boolean optBoolean(String k,boolean d){Object v=values.get(k);return v instanceof Boolean?(Boolean)v:d;}
 public JSONObject optJSONObject(String k){Object v=values.get(k);return v instanceof JSONObject?(JSONObject)v:null;}public JSONArray optJSONArray(String k){Object v=values.get(k);return v instanceof JSONArray?(JSONArray)v:null;}
 public String toString(){return values.toString();}}
'''
ARRAY_STUB='''package org.json;public class JSONArray{Object[] values;public JSONArray(Object[] v){values=v;}public int length(){return values.length;}public JSONObject optJSONObject(int i){return values[i] instanceof JSONObject?(JSONObject)values[i]:null;}public String optString(int i,String d){return values[i] instanceof String?(String)values[i]:d;}}'''
class ProfileCompatibilityTest(unittest.TestCase):
 run_java=safety.SafetyRuntimeTest.run_java
 def common(self):
  return {'org/json/JSONObject.java':JSON_STUB,'org/json/JSONArray.java':ARRAY_STUB,
   'cat/narezany/margyt/Net.java':'''package cat.narezany.margyt;class Net{static String document;static byte[] bytes(String url)throws Exception{if(!url.equals("https://ttcuz.daniilsolovatulin.workers.dev/v1/profiles/12345678901234567890"))throw new AssertionError("wrong profile URL");return document.getBytes("UTF-8");}}''',
   'cat/narezany/margyt/MusicLogic.java':(F/'MusicLogic.java').read_text(encoding='utf-8')}
 def test_original_and_patched_profile_readers(self):
  sources=self.common();contracts=json.loads((F/'contracts.json').read_text(encoding='utf-8'))['profiles'];checks=[]
  setup=''.join('org.json.JSONObject.documents.put('+java(name)+','+java(json.loads((F/(name+'.json')).read_text(encoding='utf-8')))+');' for name in ['legacy','current','future'])
  foreign=json.loads((F/'current.json').read_text(encoding='utf-8'));foreign.update(badgeUrl='https://untrusted.example/icon.png',displayName='bad\nname',gradientWidth=0,gradientStop=200,musicLook={'padding':'x'*5000})
  setup+='org.json.JSONObject.documents.put("foreign",'+java(foreign)+');'
  for i,c in enumerate(contracts):
   if c['kind']=='patched' and 'ttcuz-project-v0.6.6.1.zip' in c['archives']:
    c={**c,'fetch':safety.method('CloudProfileProvider.java','    public TtcuzProfileSync.Metadata fetch('),'host':safety.method('CloudProfileProvider.java','    public String badgeHost()'),'helper':safety.method('CloudProfileProvider.java','    private String compatibleBadge('),'metadata':safety.method('TtcuzProfileSync.java','    public static final class Metadata'),'musicRead':''}
   music='static class MusicProfile{'+c['musicRead']+'}' if c['musicRead'] else ''
   code='package cat.narezany.margyt;import org.json.JSONObject;public class Reader'+str(i)+'{static final String ROOT="https://ttcuz.daniilsolovatulin.workers.dev";static class TtcuzProfileSync{'+c['metadata']+'}'+c['fetch']+c['host']+c['helper']+music+'''
    public static void run()throws Exception{Reader'''+str(i)+''' r=new Reader'''+str(i)+'''();for(String name:new String[]{"legacy","current","future"}){Net.document=name;TtcuzProfileSync.Metadata m=r.fetch("12345678901234567890");if(!m.valid(r.badgeHost())||!m.profileId.equals("12345678901234567890")||m.firstColour!=-2532037||m.lastColour!=-14222892||!m.threeColours||!m.bold||!m.glow||!m.animated||m.badgeUrl==null)throw new AssertionError("archived reader lost common style: "+name);}
   '''
   if c['helper']:
    code+='Net.document="foreign";TtcuzProfileSync.Metadata m=r.fetch("12345678901234567890");if(!m.valid(r.badgeHost())||m.badgeUrl!=null||m.firstColour!=-2532037)throw new AssertionError("unsupported extension hides gradient");'
   code+='}}';sources['cat/narezany/margyt/Reader'+str(i)+'.java']=code;checks.append('cat.narezany.margyt.Reader'+str(i)+'.run();')
  sources['Test.java']='public class Test{public static void main(String[] a)throws Exception{'+setup+''.join(checks)+'}}';self.run_java(sources)
 def test_patched_badge_parsers_keep_full_worker_catalog(self):
  sources=self.common();contracts=json.loads((F/'contracts.json').read_text(encoding='utf-8'))['badges'];checks=[]
  limit=re.search(r'MOST\s*=\s*(\d+)\s*;',(ROOT/'inject/java/cat/narezany/margyt/Badges.java').read_text(encoding='utf-8')).group(1)
  sources['android/os/SystemClock.java']='package android.os;public class SystemClock{public static long elapsedRealtime(){return 1000;}}'
  sources['cat/narezany/margyt/Badge.java']='package cat.narezany.margyt;class Badge{static void refreshProfiles(){}}'
  sources['cat/narezany/margyt/Diary.java']='package cat.narezany.margyt;class Diary{static void note(String s){throw new AssertionError(s);}}'
  for i,c in enumerate(contracts):
   name='BadgeReader'+str(i)
   code='package cat.narezany.margyt;import org.json.*;import java.util.*;public class '+name+'{'+c['badge']+c['apply']+c['localised']+c['colour']+c['creator']+'''
    static final int MOST=256;static final String CREATOR_UID="7491898648855512119";static final Badge CREATOR_PLACEHOLDER=new Badge("creator_trem0r1_1","",0,"","","");static long freeUntil,serverNow,readAt;static Map<String,Badge[]> known=new HashMap<>();static Badge[] numbered;
    public static void run()throws Exception{if(!apply("badges".getBytes("UTF-8")))throw new AssertionError("feed rejected");if(numbered.length!=201||known.get("12345678901234567890").length!=200)throw new AssertionError("large server catalog silently truncated");if(!numbered[200].image.endsWith(".png"))throw new AssertionError("PNG badge missing");}}'''
   code=code.replace('MOST=256;','MOST='+limit+';');sources['cat/narezany/margyt/'+name+'.java']=code;checks.append('cat.narezany.margyt.'+name+'.run();')
  fixture=json.loads((F/'badges.json').read_text(encoding='utf-8'));sources['Test.java']='public class Test{public static void main(String[] a)throws Exception{org.json.JSONObject.documents.put("badges",'+java(fixture)+');'+''.join(checks)+'}}';self.run_java(sources)
