"""The public list supplies membership, never arbitrary badge content or URLs."""

import unittest
import test_settings_safety_runtime as safety
from test_profile_compatibility_runtime import java, JSON_STUB, ARRAY_STUB


class TtyBadgesTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_fixed_badge_and_revocation_without_erasing_valid_cache(self):
        rows = [
            {"uid": "123", "isDeveloper": False},
            {"uid": "456", "badgeUrl": "https://arbitrary.example/gift.png", "text": "arbitrary"},
            {"uid": "789", "isDeveloper": True},
            {"uid": "111", "badgeUrl": "etg.png"},
            {"uid": "not-uid"},
        ]
        array = ARRAY_STUB.replace(
            "Object[] values;",
            "static java.util.Map<String,JSONArray> documents=new java.util.HashMap<>();Object[] values;public JSONArray(String s){JSONArray a=documents.get(s);if(a==null)throw new IllegalArgumentException();values=a.values;}public static void fixture(String name,JSONArray a){documents.put(name,a);}",
        )
        methods = safety.method("TtyBadges.java", "    static boolean apply(byte[] bytes)")
        badge = safety.method("Badges.java", "    public static final class Badge")
        self.run_java(
            {
                "org/json/JSONObject.java": JSON_STUB,
                "org/json/JSONArray.java": array,
                "cat/ttcuz/framework/Diary.java": "package cat.ttcuz.framework;class Diary{static void note(String s){}}",
                "cat/ttcuz/framework/Badges.java": "package cat.ttcuz.framework;import java.util.*;class Badges{"
                + badge
                + "static Map<String,Badge[]> users;static void applyTty(Map<String,Badge[]> u,Badge b){users=u;}}",
                "cat/ttcuz/framework/TtyBadges.java": 'package cat.ttcuz.framework;import java.util.*;import org.json.*;public class TtyBadges{static final Badges.Badge SUPPORTER=new Badges.Badge("tty.supporter","tty:supporter",0,"Поддержал ТТЮ","Поддержал ТТЮ","Понятно");'
                + methods
                + """public static void run()throws Exception{
 if(!apply("members".getBytes("UTF-8"))||Badges.users.size()!=2)throw new AssertionError("wrong supporters");
 if(Badges.users.get("123")[0]!=Badges.users.get("456")[0]||!SUPPORTER.text.equals("Поддержал ТТЮ")||!SUPPORTER.image.equals("tty:supporter"))throw new AssertionError("arbitrary upstream content used");
 if(apply("broken".getBytes("UTF-8"))||Badges.users.size()!=2)throw new AssertionError("cache erased");
 if(apply(new byte[2097153])||Badges.users.size()!=2)throw new AssertionError("unbounded feed");
 if(!apply("empty".getBytes("UTF-8"))||!Badges.users.isEmpty())throw new AssertionError("revocation ignored");}}
""",
                "Test.java": 'import org.json.*;public class Test{public static void main(String[]a)throws Exception{JSONArray.fixture("members",'
                + java(rows)
                + ');JSONArray.fixture("empty",new JSONArray(new Object[]{}));cat.ttcuz.framework.TtyBadges.run();}}',
            }
        )
