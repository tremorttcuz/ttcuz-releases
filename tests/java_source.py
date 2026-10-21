"""Java source matching that ignores layout while preserving string literals."""

import re

LEXER = re.compile(r'//[^\n]*|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|[\w$]+|[^\s]')


def tokens(source):
    return [match for match in LEXER.finditer(source) if not match[0].startswith(("//", "/*"))]


def compact(source):
    values = [match[0] for match in tokens(source)]
    # Java array initializers may have an optional trailing comma.
    return " ".join(
        value
        for i, value in enumerate(values)
        if not (value == "," and i + 1 < len(values) and values[i + 1] == "}")
    )


def assert_code_contains(test, expected, actual):
    test.assertIn(compact(expected), compact(actual))


def find_code(source, marker):
    pattern = r"\s*".join(re.escape(match[0]) for match in tokens(marker))
    found = re.search(pattern, source)
    if found is None:
        raise ValueError("Java fragment not found: " + marker.strip())
    return found.start()


def extract_statement(source, marker):
    start = find_code(source, marker)
    depth = 0
    for match in tokens(source[start:]):
        if match[0] in ("(", "{", "["):
            depth += 1
        elif match[0] in (")", "}", "]"):
            depth -= 1
        elif match[0] == ";" and depth == 0:
            return source[start : start + match.end()]
    raise ValueError("Unterminated Java statement: " + marker)


def replace_code(source, old, new):
    pattern = r"\s*".join(re.escape(match[0]) for match in tokens(old))
    result, count = re.subn(pattern, lambda _: new, source)
    if not count:
        raise ValueError("Java fragment not found: " + old)
    return result


def code_without_literals(source):
    return LEXER.sub(
        lambda match: " " if match[0].startswith(('"', "'", "//", "/*")) else match[0],
        source,
    )
