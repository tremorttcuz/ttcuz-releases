import unittest
import test_settings_safety_runtime as runtime


class CrashOriginTest(unittest.TestCase):
    def test_real_cause_and_original_throw_site_survive_handler(self):
        code = 'public class Test {' + runtime.method('CrashReports.java', 'private static Throwable causeOf(') + runtime.method('CrashReports.java', 'private static String site(') + '''
        public static void main(String[] args) {
          Throwable inner = new IllegalArgumentException("real cause");
          inner.setStackTrace(new StackTraceElement[]{new StackTraceElement("plugin.Render", "draw", "Render.java", 42)});
          Throwable outer = new RuntimeException("wrapper", inner);
          if (causeOf(outer) != inner) throw new AssertionError("lost actual cause");
          if (!site(causeOf(outer)).equals("plugin.Render.draw(Render.java:42)")) throw new AssertionError("handler blamed instead of throw site");
          inner.initCause(outer);
          if (causeOf(outer) == null) throw new AssertionError("cyclic cause lost");
        }}'''
        runtime.SafetyRuntimeTest.run_java(self, {'Test.java': code})
