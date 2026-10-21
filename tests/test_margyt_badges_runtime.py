import pathlib
import unittest
import test_settings_safety_runtime as safety
from test_profile_compatibility_runtime import java, JSON_STUB, ARRAY_STUB

ROOT = pathlib.Path(__file__).resolve().parents[1]


class MargytBadgesRuntimeTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_two_feeds_collision_removal_limit_and_image_origin(self):
        signatures = [
            "    public static final class Badge",
            "    private static boolean apply(byte[] json)",
            "    private static synchronized boolean apply(byte[] json, boolean margyt)",
            "    private static void mergeFeeds()",
            "    private static String localised(",
            "    private static int colour(",
            "    static boolean isCreator(",
            "    static String pictureAddress(",
            "    public static Badge[] of(",
            "    public static String marksFor(",
            "    public static Badge byMark(",
        ]
        methods = "\n".join(safety.method("Badges.java", sig) for sig in signatures)
        own = {
            "badges": [
                {
                    "id": "owner",
                    "users": ["123", "123"],
                    "image": "badges/owner.png",
                    "title": "ttcuz",
                }
            ]
        }
        other = {
            "badges": [
                {
                    "id": "owner",
                    "users": ["123", "123"],
                    "image": "note.png?v=6812153f",
                    "title": "MargyT",
                }
            ]
        }
        many = {"badges": [{"id": "badge" + str(i), "users": ["123"]} for i in range(270)]}
        fixtures = {"own": own, "other": other, "empty": {"badges": []}, "bad": {}, "many": many}
        setup = "".join(
            'JSONObject.documents.put("' + k + '",' + java(v) + ");" for k, v in fixtures.items()
        )
        sources = {
            "org/json/JSONObject.java": JSON_STUB,
            "org/json/JSONArray.java": ARRAY_STUB,
            "android/os/SystemClock.java": "package android.os;public class SystemClock{public static long elapsedRealtime(){return 1000;}}",
            "cat/ttcuz/framework/Diary.java": "package cat.ttcuz.framework;class Diary{static void note(String s){throw new AssertionError(s);}}",
            "cat/ttcuz/framework/Badge.java": "package cat.ttcuz.framework;class Badge{static void refreshProfiles(){}}",
            "cat/ttcuz/framework/Badges.java": """package cat.ttcuz.framework;import java.util.*;import org.json.*;public class Badges{
 static final int MOST=256;static final char FIRST='\ue100';static final String CREATOR_UID="7491898648855512119";
 static final String MARGYT="https://margy.narez.xyz/api/",MARGYT_IMAGES="margyt:",FILES="https://ttcuz.daniilsolovatulin.workers.dev/";
 static final Badge CREATOR_PLACEHOLDER=new Badge("creator_trem0r1_1","",0,"","","");static long freeUntil,serverNow,readAt;
 static Map<String,Badge[]> known=new HashMap<>(),ownBadges=new HashMap<>(),margytBadges=new HashMap<>();
 static Badge[] numbered,ownOrder={CREATOR_PLACEHOLDER},margytOrder={},ttyOrder={};static Map<String,Badge[]> ttyBadges=new HashMap<>();
 static Map<String,Object> pictures=new HashMap<>();static Map<String,Long> pictureAsked=new HashMap<>();
 static boolean externalOn=true;static boolean isEnabled(){return true;}static boolean margytEnabled(){return externalOn;}
"""
            + methods
            + """
 public static void run()throws Exception{
  if(!apply("other".getBytes("UTF-8"),true)||!apply("own".getBytes("UTF-8")))throw new AssertionError();
  Badge[] user=of("123");if(user.length!=2||!user[0].id.equals("owner")||!user[1].id.equals("margyt.owner"))throw new AssertionError("feed collision");
  String marks=marksFor("123");if(marks.length()!=2||byMark(marks.charAt(0))!=user[0]||byMark(marks.charAt(1))!=user[1])throw new AssertionError("lost renderer markers");
  externalOn=false;if(marksFor("123").length()!=1||byMark(marks.charAt(1))!=null||byMark(marks.charAt(0))!=user[0])throw new AssertionError("external switch hid ttcuz badges or failed to hide MargyT");
  externalOn=true;if(!marksFor("123").equals(marks))throw new AssertionError("switch reordered cached badge marks");
  if(!pictureAddress(user[1].image).equals(MARGYT+"icon/note.png?v=6812153f"))throw new AssertionError("lost upstream cache revision");
  if(!pictureAddress(user[0].image).equals(FILES+"badges/owner.png"))throw new AssertionError("wrong own image origin");
  for(String image:new String[]{"margyt:https://evil/icon.png","margyt:../note.png","margyt:note.png?v=../../","margyt:note.png?other=1","https://evil/icon.png","badges/../x.png"})if(pictureAddress(image)!=null)throw new AssertionError("unsafe badge URL");
  if(apply("bad".getBytes("UTF-8"),true)||of("123").length!=2)throw new AssertionError("bad feed erased cache");
  pictures.put(user[1].image,new Object());pictureAsked.put(user[1].image,1L);
  if(!apply("empty".getBytes("UTF-8"),true)||of("123").length!=1||!pictures.isEmpty()||!pictureAsked.isEmpty())throw new AssertionError("revoked external badge survives");
  if(!apply("many".getBytes("UTF-8"),true)||numbered.length!=256||of("123").length!=255||marksFor("123").length()!=255)throw new AssertionError("unbounded or invisible badges");
  if(!apply("empty".getBytes("UTF-8"))||of("123").length!=255)throw new AssertionError("own feed erased external badges");
 }
}""",
            "Test.java": "import org.json.*;public class Test{public static void main(String[]a)throws Exception{"
            + setup
            + "cat.ttcuz.framework.Badges.run();}}",
        }
        self.run_java(sources)
