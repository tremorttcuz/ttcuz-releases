"""Source matching must ignore layout without changing Java string data."""

import unittest

from java_source import compact, extract_statement, find_code


class JavaSourceTest(unittest.TestCase):
    def test_layout_literals_and_nested_listener(self):
        self.assertEqual(compact('call( 1, "a b" )'), compact('call(1,"a b")'))
        self.assertNotEqual(compact('call("a b")'), compact('call("ab")'))
        self.assertEqual(compact("new int[]{1,}"), compact("new int[] {1}"))
        source = "static\nvoid run() {}"
        self.assertEqual(0, find_code(source, "static void run("))
        statement = 'root.listen(new Listener() { void run() { call("};"); } });'
        self.assertEqual(statement, extract_statement(statement + " next();", "root.listen("))
