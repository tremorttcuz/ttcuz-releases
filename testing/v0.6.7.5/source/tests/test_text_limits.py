"""Input limits from the actual 47.2.41 APK, with production debug directives."""
import pathlib
import re
import shutil
import subprocess
import tempfile
import unittest
from margyt import dexpatch

ROOT = pathlib.Path(__file__).resolve().parents[1]
FIXTURES = ROOT / 'tests/fixtures/text-limits-47.2.41'


class TextLimitTests(unittest.TestCase):
    def test_native_limits_cover_cached_values_both_reposts_and_long_comments(self):
        counts = {}
        for file in FIXTURES.glob('*.smali'):
            original = file.read_text(encoding='utf-8')
            self.assertIn('.line', original)
            patched, hits = dexpatch.text_limit_rules(original)
            self.assertTrue(hits, file.name)
            for key, value in hits.items(): counts[key] = counts.get(key, 0) + value
            self.assertEqual((patched, {}), dexpatch.text_limit_rules(patched))
            # No increased register budget or altered return/send payloads.
            self.assertEqual(re.findall(r'\.registers \d+', original), re.findall(r'\.registers \d+', patched))
            self.assertEqual(re.findall(r'(?m)^\s*return(?:-object|-void)?(?: [vp]\d+)?$', original),
                             re.findall(r'(?m)^\s*return(?:-object|-void)?(?: [vp]\d+)?$', patched))
        self.assertEqual({'native comment limit':1, 'native long comment limit':1,
                          'native repost limit':3, 'native repost note limit':1,
                          'native repost note fallback':1, 'native hashtag maximum':1,
                          'native hashtag gate':1}, counts)

    def test_scope_does_not_remove_all_text_filters_or_caption_length(self):
        unrelated = '''.class public LOther;
.super Ljava/lang/Object;
.method public count()I
 .registers 2
 const/16 v0, 0x1e
 invoke-direct {v1, v0}, Landroid/text/InputFilter$LengthFilter;-><init>(I)V
 return v0
.end method
'''
        self.assertEqual((unrelated, {}), dexpatch.text_limit_rules(unrelated))
        for marker in dexpatch.TEXT_LIMIT_MARKERS:
            self.assertTrue(dexpatch.carries_an_anchor(marker.encode()))
        for sig in ('comment(I)I','commentLong(J)J','repost(I)I','hashtags(I)I','hashtagGate(Z)Z'):
            self.assertIn(dexpatch.TEXT_LIMITS + '->' + sig, dexpatch.rewrite_targets())

    @unittest.skipUnless(shutil.which('javac') and shutil.which('java'), 'JDK required')
    def test_native_integer_bounds_independent_switches_and_restoration(self):
        sources = {
            'android/content/Context.java':'package android.content;public class Context {public static int MODE_PRIVATE=0;public static SharedPreferences prefs;public SharedPreferences getSharedPreferences(String n,int m){return prefs;}}',
            'android/content/SharedPreferences.java':'package android.content;public interface SharedPreferences {boolean getBoolean(String k,boolean d);Editor edit();interface Editor {Editor putBoolean(String k,boolean v);void apply();}}',
            'com/bytedance/ies/abmock/SettingsManager.java':'package com.bytedance.ies.abmock;public class SettingsManager {public static boolean LIZ(String k,boolean v){return v;}public static int LJ(String k,int v){return v;}public static long LJFF(String k,long v){return v;}public static String LJI(String k,String v){return v;}public static float LIZJ(String k,float v){return v;}public static double LIZIZ(String k,double v){return v;}}',
            'cat/narezany/margyt/Margy.java':'package cat.narezany.margyt;class Margy {static String PREFS="test";static android.content.Context context(){return new android.content.Context();}}',
            'cat/narezany/margyt/Feed.java':'package cat.narezany.margyt;class Feed {static boolean isEnabled(){return false;}}',
            'cat/narezany/margyt/LimitHarness.java':'''package cat.narezany.margyt;
import android.content.*;import java.util.*;
public class LimitHarness {
 static class Prefs implements SharedPreferences,SharedPreferences.Editor {
  Map<String,Boolean> values=new HashMap<>();public boolean getBoolean(String k,boolean d){return values.containsKey(k)?values.get(k):d;}
  public Editor edit(){return this;}public Editor putBoolean(String k,boolean v){values.put(k,v);return this;}public void apply(){}
 }
 static void check(boolean v){if(!v)throw new AssertionError();}
 public static void main(String[]a){
  Context.prefs=new Prefs();
  check(TextLimits.comment(2200)==2200 && TextLimits.commentLong(4000)==4000);
  check(TextLimits.repost(30)==30 && TextLimits.repost(100)==100);
  check(TextLimits.hashtags(5)==5 && TextLimits.hashtagGate(true) && !TextLimits.hashtagGate(false));
  Flags.set(TextLimits.COMMENTS,true);
  check(TextLimits.comment(2200)==Integer.MAX_VALUE && (int)TextLimits.commentLong(4000)==Integer.MAX_VALUE);
  check(TextLimits.repost(100)==100 && TextLimits.hashtags(5)==5);
  Flags.set(TextLimits.REPOSTS,true);check(TextLimits.repost(30)==Integer.MAX_VALUE);
  String text="";for(int i=0;i<2300;i++)text+="\\ud83d\\ude00";
  check(TextLimits.comment(2200)-text.length()>0 && TextLimits.repost(30)-text.length()>0);
  Flags.set(TextLimits.HASHTAGS,true);check(TextLimits.hashtags(5)==Integer.MAX_VALUE && !TextLimits.hashtagGate(true));
  Flags.set(TextLimits.COMMENTS,false);Flags.set(TextLimits.REPOSTS,false);Flags.set(TextLimits.HASHTAGS,false);
  check(TextLimits.comment(2200)==2200 && TextLimits.commentLong(4000)==4000 && TextLimits.repost(30)==30 && TextLimits.hashtagGate(true));
 }
}'''
        }
        for name in ('Flags','TextLimits'):
            sources['cat/narezany/margyt/'+name+'.java'] = (ROOT / ('inject/java/cat/narezany/margyt/'+name+'.java')).read_text(encoding='utf-8')
        with tempfile.TemporaryDirectory() as folder:
            room = pathlib.Path(folder)
            for name, source in sources.items():
                file = room/name; file.parent.mkdir(parents=True, exist_ok=True); file.write_text(source, encoding='utf-8')
            result = subprocess.run(['javac','-encoding','UTF-8','-d',folder]+[str(room/name) for name in sources],capture_output=True,text=True)
            self.assertEqual(0,result.returncode,result.stderr)
            result = subprocess.run(['java','-cp',folder,'cat.narezany.margyt.LimitHarness'],capture_output=True,text=True)
            self.assertEqual(0,result.returncode,result.stderr)
