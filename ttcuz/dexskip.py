"""Keep a dex's patched result across a change of the rules that cannot touch it.

The dex cache is keyed by the dex and the whole rule set, so editing one rule in
dexpatch.py used to send every dex through copy, rewrite and (sometimes)
assembly again. Most of them come out the same: a rule only acts on a dex that
names what the rule looks for.

So for every dex the last answer is remembered with the rules it was made by.
When the rules change, the two versions are compared, and the dex keeps its old
answer if it contains none of the names the changed parts work with. The test
is conservative: it reuses only when it can say why that is safe, and does the
full patch otherwise.

  * dexpatch.py is compared function by function (and statement by statement
    outside functions). A changed function counts with every quoted name in its
    whole body, old and new, because a line without names inside it (a loop, a
    counter) acts on what the rest of the function found. A change made of calls
    to other changed functions is covered by those. A change with no name to go
    by at all -- a reworked general pass -- means a full patch of every dex.
  * transplants.json is compared entry by entry: a changed entry counts with the
    class it replaces.
  * a name counts as present when every part of it the dex would store on its
    own (class descriptor, member name, string) is in the dex.
"""

from __future__ import annotations

import ast
import hashlib
import json
import os
import re
from typing import Dict, Iterable, List, Optional, Set, Tuple

RULE_FILES = ("dexpatch.py", os.path.join("data", "transplants.json"))
HERE = os.path.dirname(os.path.abspath(__file__))

_QUOTED = re.compile(r"""[bru]{0,2}("(?:[^"\\\n]|\\.){3,}"|'(?:[^'\\\n]|\\.){3,}')""")
_PIECE = re.compile(r"L[\w/$-]+;|<[\w$]+>|[A-Za-z_$][\w$]{3,}")
# words of the language and of the rules' own plumbing, never a reason to keep or redo a dex
_PLAIN = {"utf-8", "utf8", "smali", ".smali", "invoke-static", "invoke-virtual", "invoke-direct",
          "invoke-interface", "invoke-super", "move-result", "move-result-object", "return-void",
          ".method", ".end method", "const-string"}


#: Parts of dexpatch.py that act only on the classes listed in data/transplants.json. A change to them reaches the
#: dex files holding those classes, which changed_names counts from the json.
SCOPED = {"transplants", "transplant_targets", "carries_a_transplant", "rewrite_transplants",
          "TRANSPLANTS_FILE", "_transplants"}

#: smali's own words: a pattern built of them can match code in any dex
_SYNTAX = {"method", "end", "invoke", "virtual", "static", "direct", "interface", "super", "range", "move", "result",
           "object", "wide", "return", "void", "const", "string", "class", "field", "local", "locals", "registers",
           "line", "param", "annotation", "goto", "cond", "check", "cast", "instance", "sget", "sput", "iget", "iput",
           "aget", "aput", "new", "array", "throw", "catch", "catchall", "implements", "source", "packed", "sparse",
           "switch", "monitor", "exception", "nez", "eqz", "ltz", "gez", "gtz", "lez", "polymorphic", "custom", "smali"}


def rules_text() -> bytes:
    """Everything the rewrite depends on besides the dex: the rules and their data."""
    parts = []
    for name in RULE_FILES:
        path = os.path.join(HERE, name)
        try:
            with open(path, "rb") as handle:
                parts.append(name.encode() + b"\0" + handle.read())
        except OSError:
            parts.append(name.encode() + b"\0")
    return b"\1".join(parts)


def _split(text: bytes) -> Dict[str, bytes]:
    out = {}
    for chunk in text.split(b"\1"):
        name, _, body = chunk.partition(b"\0")
        out[name.decode()] = body
    return out


def _names(source: str) -> Set[str]:
    found = set()
    for match in _QUOTED.finditer(source):
        try:
            value = ast.literal_eval(match.group(0))
        except (ValueError, SyntaxError):
            value = match.group(1)[1:-1]
        if isinstance(value, bytes):
            value = value.decode("utf-8", "replace")
        value = str(value).strip()
        if len(value) >= 4 and value not in _PLAIN:
            found.add(value)
    return found


def _units(source: str) -> Optional[Dict[str, str]]:
    """Top-level functions and classes by name; other statements by their text."""
    try:
        tree = ast.parse(source)
    except SyntaxError:
        return None
    lines = source.splitlines(keepends=True)
    units = {}
    for node in tree.body:
        text = "".join(lines[node.lineno - 1:node.end_lineno])
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)):
            units["def " + node.name] = text
        elif isinstance(node, ast.Expr) and isinstance(getattr(node, "value", None), ast.Constant):
            continue  # a docstring: no effect on any dex
        elif isinstance(node, (ast.Assign, ast.AnnAssign)):
            targets = node.targets if isinstance(node, ast.Assign) else [node.target]
            label = ",".join(ast.unparse(t) for t in targets)
            units["var " + label] = text
        else:
            units["stmt " + hashlib.sha1(text.encode()).hexdigest()] = text
    return units


def _strip_comments(source: str) -> str:
    return "\n".join(line.split("#", 1)[0].rstrip() if "#" in line and '"' not in line and "'" not in line
                     else line.rstrip() for line in source.splitlines())


def _real(piece: str) -> bool:
    """A piece a dex stores as it is: a class descriptor, <init>, or a name with a capital, digit, '_' or '$'.
    Plain lower-case words (method, invoke, move, result) are smali syntax, which no dex contains."""
    return piece.startswith("L") and piece.endswith(";") or piece.startswith("<") or bool(re.search(r"[A-Z0-9_$]", piece))


def _kinds(text: str) -> Tuple[Set[str], bool]:
    """The specific names in a piece of rules, and whether it also holds a general (syntax-only) pattern."""
    names, general = set(), False
    for value in _names(text):
        if any(_real(p) for p in _PIECE.findall(value)):
            names.add(value)
        elif _SYNTAX & set(re.findall(r"[a-z]+", value)) and re.search(r"[\\({^.\s]", value):
            general = True                     # a pattern over smali itself (".method", "invoke-virtual {")
        # else a plain word ("class", "body"): a dictionary key or a message, nothing a dex is matched by
    return names, general


def _python_names(old: str, new: str) -> Optional[Set[str]]:
    """The names a change of dexpatch.py works with, or None when it cannot be told."""
    import difflib
    a, b = _units(old), _units(new)
    if a is None or b is None:
        return None
    changed = {k for k in set(a) | set(b)
               if _strip_comments(a.get(k, "")) != _strip_comments(b.get(k, ""))}
    # the parts that work only on what transplants.json lists: their reach is its classes, counted from the json
    changed -= {"def " + n for n in SCOPED} | {"var " + n for n in SCOPED}
    defined = {k[4:] for k in changed if k.startswith(("def ", "var "))} | SCOPED
    names: Set[str] = set()
    for key in changed:
        if key in a and key in b and key.startswith("def ") and _same_calls(a[key], b[key], defined):
            continue                           # it only gained or lost calls to parts counted on their own
        whole_names, whole_general = _kinds(a.get(key, "") + "\n" + b.get(key, ""))
        if key not in a or key not in b:
            # a unit added or removed whole: everything in it is the change
            if whole_general or not (whole_names or _calls_only(a.get(key, "") + b.get(key, ""), defined - {key[4:]})):
                return None
            names |= whole_names
            continue
        for line in difflib.ndiff(_strip_comments(a[key]).splitlines(), _strip_comments(b[key]).splitlines()):
            if line[:2] not in ("+ ", "- ") or not line[2:].strip():
                continue
            text = line[2:]
            found, general = _kinds(text)
            if general:
                return None                    # a syntax-wide pattern changed: any dex may differ
            if found:
                names |= found
            elif _calls_only(text, defined - {key[4:]}):
                continue                       # a call to a changed function, counted with it
            elif whole_names and not whole_general:
                names |= whole_names           # glue inside a rule that only works on these names
            else:
                return None
    return names


def _same_calls(old: str, new: str, defined: Set[str]) -> bool:
    """Both versions do the same other things in the same order; only uses of changed parts differ."""
    def shape(text):
        try:
            tree = ast.parse(text)
        except SyntaxError:
            return None
        fn = tree.body[0]
        body = fn.body[1:] if fn.body and isinstance(fn.body[0], ast.Expr) and isinstance(fn.body[0].value, ast.Constant) else fn.body
        calls = []
        for node in ast.walk(ast.Module(body=body, type_ignores=[])):
            if isinstance(node, ast.Call):
                f = node.func
                label = f.id if isinstance(f, ast.Name) else f.attr if isinstance(f, ast.Attribute) else None
                calls.append(label)
            if isinstance(node, ast.Constant) and isinstance(node.value, (str, bytes)):
                calls.append(("constant", node.value))
            if isinstance(node, ast.Name) and node.id in defined:
                calls.append(None)
        # what is left once the changed parts and the plumbing around a call are taken out
        return [c for c in calls if c is not None and c not in defined and c not in ("update", "extend", "append")]
    left, right = shape(old), shape(new)
    return left is not None and left == right


def _calls_only(text: str, defined: Set[str]) -> bool:
    """A line that uses functions whose change is already counted."""
    return bool(set(re.findall(r"[A-Za-z_]\w*", text)) & defined)


def _json_names(old: bytes, new: bytes) -> Optional[Set[str]]:
    try:
        a = json.loads(old.decode("utf-8")) if old.strip() else []
        b = json.loads(new.decode("utf-8")) if new.strip() else []
    except ValueError:
        return None

    def keyed(entries):
        out = {}
        for entry in entries:
            out[(entry.get("class"), entry.get("header"))] = json.dumps(entry, sort_keys=True)
        return out
    ka, kb = keyed(a), keyed(b)
    return {cls for (cls, header) in set(ka) | set(kb) if ka.get((cls, header)) != kb.get((cls, header)) and cls}


def changed_names(old: bytes, new: bytes) -> Optional[Set[str]]:
    """What the difference between two rule sets works with, or None: anything may differ."""
    a, b = _split(old), _split(new)
    names: Set[str] = set()
    for name in RULE_FILES:
        before, after = a.get(name, b""), b.get(name, b"")
        if before == after:
            continue
        if name.endswith(".py"):
            text_a, text_b = before.decode("utf-8", "replace"), after.decode("utf-8", "replace")
            found = _python_names(text_a, text_b)
            if found is not None and _scoped_changed(text_a, text_b):
                found |= _transplant_classes(a.get(RULE_FILES[1], b"")) | _transplant_classes(b.get(RULE_FILES[1], b""))
        else:
            found = _json_names(before, after)
        if found is None:
            return None
        names |= found
    return names


def _scoped_changed(old: str, new: str) -> bool:
    a, b = _units(old) or {}, _units(new) or {}
    keys = {"def " + n for n in SCOPED} | {"var " + n for n in SCOPED}
    return any(_strip_comments(a.get(k, "")) != _strip_comments(b.get(k, "")) for k in keys)


def _transplant_classes(data: bytes) -> Set[str]:
    try:
        return {entry["class"] for entry in json.loads(data.decode("utf-8")) if entry.get("class")}
    except (ValueError, TypeError, KeyError, AttributeError):
        return set()


def mentions(dex: bytes, names: Iterable[str]) -> bool:
    """True when the dex may hold any of the names, so rules about them may change it."""
    for name in names:
        pieces = [p for p in _PIECE.findall(name) if _real(p)]
        if not pieces:
            return True       # nothing to look for: assume it matters
        if all(p.encode("utf-8") in dex for p in pieces):
            return True
    return False


# ------------------------------------------------------------------ memory

def _index(root: str, dex_sha: str) -> str:
    return os.path.join(root, "last-%s.json" % dex_sha)


def _rules_path(root: str, rules_sha: str) -> str:
    return os.path.join(root, "rules-%s.bin" % rules_sha)


def remember(root: str, dex_sha: str, rules: bytes, context: str, answer: dict) -> None:
    """Notes the answer this dex got from these rules (in this context: api, literals, found targets)."""
    rules_sha = hashlib.sha256(rules).hexdigest()
    try:
        keep = _rules_path(root, rules_sha)
        if not os.path.isfile(keep):
            _write(keep, rules)
        _write(_index(root, dex_sha), json.dumps({"rules": rules_sha, "context": context,
                                                   "answer": answer}).encode("utf-8"))
    except OSError:
        pass


def recall(root: str, dex: bytes, dex_sha: str, rules: bytes, context: str) -> Tuple[Optional[dict], str]:
    """The earlier answer for this dex if the rule change cannot touch it, with the reason either way."""
    try:
        with open(_index(root, dex_sha), encoding="utf-8") as handle:
            last = json.load(handle)
        if last.get("context") != context:
            return None, "the build context changed"
        with open(_rules_path(root, last["rules"]), "rb") as handle:
            old = handle.read()
    except (OSError, ValueError, KeyError):
        return None, "no earlier answer"
    names = changed_names(old, rules)
    if names is None:
        return None, "a general part of the rules changed"
    if mentions(dex, names):
        return None, "the changed rules name what is in it"
    return last["answer"], "untouched by the changed rules"


def _write(path: str, data: bytes) -> None:
    part = "%s.%d.part" % (path, os.getpid())
    with open(part, "wb") as handle:
        handle.write(data)
    os.replace(part, path)
