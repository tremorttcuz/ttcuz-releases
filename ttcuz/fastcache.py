"""Skip native DEX work when its applicable patch rules did not change.

Unknown rule changes still invalidate the answer. Only the two explicitly
scoped photo rules can be omitted, and only when their targets are absent.
TTCUZ_CACHE_BASELINE may point at an older source ZIP to adopt its answers.
The ZIP is read as data; none of its Python code is executed.
"""

import ast
import hashlib
import json
import os
import re
import struct
import zipfile
from pathlib import Path

MODULES = ("dexpatch", "inbox", "privacy", "comment_background", "story_likes", "profile_banner")
PHOTO_TARGETS = (b"MvChoosePhotoScene", b"CommentPublishRequestModel", b"KeyboardVMV2",
                 b"CommentInputContent", b"AwS142S0400000_22", b"AwS275S0300000_22")
PHOTO_CLASSES = {marker.decode() for marker in PHOTO_TARGETS}
PHOTO_KEYS = ("comment_multi_photo_group", "comment_multi_photo_v2_editor")
# The old answer writer used the same pathlib tracking and outcome format.
LEGACY_WRITER = "754dc077419a39b318c11a2c2e8ca32172ff0547bc0bc143caa8e5b5d4734c4c"
PHOTO_RULE = "1692c7fb3586ef769a6275293bfbb8a2367eda8994b94ad549486e493b05243d"


def classes_defined(dex):
    """Read only DEX class definitions; string references elsewhere are noise."""
    if len(dex) < 112 or dex[:3] != b"dex":
        return {"*unknown*"}
    try:
        string_size, string_off = struct.unpack_from("<2I", dex, 56)
        type_size, type_off = struct.unpack_from("<2I", dex, 64)
        class_size, class_off = struct.unpack_from("<2I", dex, 96)
        if max(string_off + string_size * 4, type_off + type_size * 4, class_off + class_size * 32) > len(dex):
            return {"*unknown*"}
        out = set()
        from .dexpatch import _uleb
        for i in range(class_size):
            class_index = struct.unpack_from("<I", dex, class_off + i * 32)[0]
            if class_index >= type_size:
                return {"*unknown*"}
            string_index = struct.unpack_from("<I", dex, type_off + class_index * 4)[0]
            if string_index >= string_size:
                return {"*unknown*"}
            at = struct.unpack_from("<I", dex, string_off + string_index * 4)[0]
            _length, at = _uleb(dex, at)
            end = dex.index(b"\0", at)
            descriptor = dex[at:end].decode("ascii")
            if descriptor.startswith("L") and descriptor.endswith(";"):
                out.add(descriptor[1:-1].rsplit("/", 1)[-1])
        return out
    except (IndexError, UnicodeDecodeError, struct.error, ValueError):
        return {"*unknown*"}


def normalized(source, dex, module="dexpatch", defined=None):
    tree = ast.parse(source)
    if module != "dexpatch":
        return ast.dump(tree, include_attributes=False)
    defined = defined if defined is not None else classes_defined(dex)
    photos = bool(PHOTO_CLASSES & defined)
    photo_flags = "AFwS227S0000000_3" in defined or "*unknown*" in defined
    photo_rule = next((n for n in tree.body if isinstance(n, ast.FunctionDef) and n.name == "rewrite_comment_photo_count"), None)
    # A future change to this function must be reviewed before its scope is trusted.
    scoped = photo_rule is None or hashlib.sha256(ast.dump(photo_rule, include_attributes=False).encode()).hexdigest() == PHOTO_RULE

    class Scoped(ast.NodeTransformer):
        def visit_FunctionDef(self, node):
            if node.name == "rewrite_comment_photo_count" and not photos and scoped:
                return None
            if node.name == "rewrite_native_flags":
                for child in ast.walk(node):
                    if isinstance(child, ast.Assign) and any(isinstance(t, ast.Name) and t.id == "keys" for t in child.targets):
                        if isinstance(child.value, ast.Tuple) and not photo_flags:
                            child.value.elts = [v for v in child.value.elts if not (
                                isinstance(v, ast.Constant) and any(v.value == '"' + key + '"' and key.encode() not in dex for key in PHOTO_KEYS))]
            if node.name in ("rewrite_everything", "rewrite_targets") and not photos and scoped:
                node.body = [statement for statement in node.body if not (
                    isinstance(statement, ast.Expr) and (
                        any(isinstance(n, ast.Call) and isinstance(n.func, ast.Name) and n.func.id == "rewrite_comment_photo_count" for n in ast.walk(statement))
                        or any(isinstance(n, ast.Constant) and isinstance(n.value, str) and n.value.startswith("Lcat/ttcuz/framework/CommentPhotos;->") for n in ast.walk(statement))))]
            return node

    return ast.dump(Scoped().visit(tree), include_attributes=False)


class Answers:
    def __init__(self, root, jar, api, literals, found):
        from . import dexcache
        self.cache = dexcache
        self.root = Path(root)
        self.current = {m: Path(__file__).with_name(m + ".py").read_bytes() for m in MODULES}
        self.context = repr((api, sorted((literals or {}).items()), sorted(found.items()))).encode()
        self.tool = dexcache._stamp(jar)
        self.baseline = None
        archive = os.environ.get("TTCUZ_CACHE_BASELINE")
        if archive:
            try:
                with zipfile.ZipFile(archive) as z:
                    def read(module):
                        matches = [n for n in z.namelist() if n == "ttcuz/" + module + ".py" or n.endswith("/ttcuz/" + module + ".py")]
                        if len(matches) != 1:
                            raise ValueError("ambiguous source archive")
                        return z.read(matches[0])
                    writer = read("dexcache")
                    if hashlib.sha256(writer).hexdigest() == LEGACY_WRITER:
                        self.baseline = {m: read(m) for m in MODULES}
                        self.legacy_rules = self.baseline["dexpatch"] + writer + b"".join(self.baseline[m] for m in MODULES[1:])
            except (OSError, ValueError, KeyError, zipfile.BadZipFile):
                pass
        self.version = Path(__file__).read_bytes() + Path(dexcache.__file__).read_bytes()

    def signature(self, sources, dex):
        defined = classes_defined(dex)
        return self.cache._sha(*(normalized(sources[m], dex, m, defined).encode() for m in MODULES))

    def key(self, dex):
        return self.cache._sha(b"fast-answer-1", self.version, hashlib.sha256(dex).digest(), self.tool,
                               self.context, self.signature(self.current, dex).encode())

    def load(self, dex):
        path = self.root / ("f-" + self.key(dex) + ".json")
        try:
            data = json.loads(path.read_text(encoding="utf8"))
            output = self.root / data["file"] if data["file"] else None
            if output and (not re.fullmatch(r"o-[0-9a-f]{64}\.dex", data["file"]) or hashlib.sha256(output.read_bytes()).hexdigest() != data["sha256"]):
                return None
            self.cache._touch(str(path))
            if output:
                self.cache._touch(str(output))
            return str(output) if output else None, data["counts"]
        except (OSError, ValueError, KeyError, TypeError):
            pass
        if self.baseline and self.signature(self.baseline, dex) == self.signature(self.current, dex):
            key = self.cache._sha(self.cache.SHAPE.encode(), hashlib.sha256(dex).hexdigest().encode(),
                                  self.legacy_rules, self.tool, self.context)
            try:
                data = json.loads((self.root / ("a-" + key + ".json")).read_text(encoding="utf8"))
                output = self.root / ("o-" + data["outcome"] + ".dex") if data["counts"] else None
                if output and (not re.fullmatch(r"o-[0-9a-f]{64}\.dex", output.name) or not output.is_file()):
                    return None
                self.store(dex, str(output) if output else None, data["counts"])
                return str(output) if output else None, data["counts"]
            except (OSError, ValueError, KeyError, TypeError):
                pass
        return None

    def store(self, dex, output, counts):
        path = Path(output) if output else None
        data = {"file": path.name if path else None, "counts": counts,
                "sha256": hashlib.sha256(path.read_bytes()).hexdigest() if path else None}
        self.cache._save(str(self.root / ("f-" + self.key(dex) + ".json")), json.dumps(data).encode())
