from pathlib import Path
import re
import unittest
import test_settings_safety_runtime as safety

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "inject/java/cat/ttcuz/framework/NavigationIsland.java"


class NavigationLabelsRuntimeTest(unittest.TestCase):
    def test_navigation_roles_cover_english_and_french_tabs(self):
        source = SOURCE.read_text(encoding="utf-8")
        declaration = re.search(r"static int role\(String text\)", source)
        start = declaration.start()
        opening = source.index("{", start)
        depth = 1
        end = opening + 1
        while depth:
            depth += (source[end] == "{") - (source[end] == "}")
            end += 1
        method = source[start:end]
        harness = """package cat.ttcuz.framework;
            import java.util.Locale;
            public final class NavigationLabels {
            %s
            public static void main(String[] args) {
                check("For You", 0);
                check("Accueil", 0);
                check("Amis", 1);
                check("Boîte de réception", 3);
                check("Profil", 4);
                check("Главная", 0);
            }
            private static void check(String label, int expected) {
                int actual = role(label);
                if (actual != expected)
                    throw new AssertionError(label + ": " + actual);
            }
        }""" % method
        safety.SafetyRuntimeTest.run_java(self, {
            "cat/ttcuz/framework/NavigationLabels.java": harness,
            "Test.java": "public class Test{public static void main(String[] args){cat.ttcuz.framework.NavigationLabels.main(args);}}",
        })


if __name__ == "__main__":
    unittest.main()
