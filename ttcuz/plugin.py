"""Pack a folder into a .ctt plugin.

    python3 -m ttcuz.plugin examples/hello
    python3 -m ttcuz.plugin examples/hello -o out/hello.ctt

The folder holds `manifest.json`, the plugin's Java under `java/` and an
optional `icon.png`. Out comes a `.ctt`: a zip with exactly `manifest.json`,
`classes.dex` and, when present, `icon.png`, within the limits the loader
enforces (16 MiB dex, 64 KiB manifest, 1 MiB icon).

The plugin is compiled against `android.jar` and the plugin API kept in
`plugins/sdk/` (signatures of the loader API). The API classes are left out of the dex: the
app that loads the plugin provides them, and a second copy would be a different
class with the same name.

The toolchain is the one the APK build downloads into `tools/`, so whichever
runs first fetches it and the other reuses it.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import sys
import tempfile
import zipfile

from .toolchain import Toolchain, _java, _run

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# the full project keeps the API in plugins/sdk; the standalone plugin kit keeps it in sdk/
API_SOURCES = next((path for path in (os.path.join(ROOT, "plugins", "sdk"), os.path.join(ROOT, "sdk"))
                    if os.path.isdir(path)), os.path.join(ROOT, "plugins", "sdk"))

REQUIRED = ("id", "entry")
API = 4                 # cat.ttcuz.framework.plugin.TtcuzPlugin.API (1, 2 and 3 are still accepted)
TIKTOK = "47.7.3"
# plugins written for the previous release use the mod's API and still install
TIKTOK_ALSO = ("47.2.41",)
DEX_LIMIT = 16 * 1024 * 1024
MANIFEST_LIMIT = 64 * 1024
ICON_LIMIT = 1024 * 1024
PNG_MAGIC = b"\x89PNG\r\n\x1a\n"


class PluginError(Exception):
    pass


def _java_files(root: str) -> list:
    out = []
    for dirpath, _dirs, names in os.walk(root):
        out += [os.path.join(dirpath, n) for n in names if n.endswith(".java")]
    return sorted(out)


def _class_files(root: str) -> list:
    out = []
    for dirpath, _dirs, names in os.walk(root):
        out += [os.path.join(dirpath, n) for n in names if n.endswith(".class")]
    return sorted(out)


def _javac(classpath: str, out_dir: str, files: list) -> None:
    javac = shutil.which("javac")
    if not javac:
        raise PluginError("javac not found: install JDK 17 or newer")
    _run([javac, "-nowarn", "-Xlint:-options", "-encoding", "UTF-8",
          "-cp", classpath, "--release", "17", "-d", out_dir] + files)


def check_manifest(manifest: dict) -> dict:
    """The same rules the loader applies, so a plugin that packs is a plugin that installs."""
    missing = [field for field in REQUIRED if not str(manifest.get(field, "")).strip()]
    if missing:
        raise PluginError("manifest.json has no %s" % ", ".join(missing))
    if not re.fullmatch(r"[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}", str(manifest["id"])):
        raise PluginError("id %r: Latin letters, digits, '.', '-' and '_', starting with a letter or digit, "
                          "at most 128" % manifest["id"])
    if not re.fullmatch(r"[A-Za-z_$][\w$]*(\.[A-Za-z_$][\w$]*)+", str(manifest["entry"])):
        raise PluginError("entry %r is not a fully qualified class name" % manifest["entry"])
    api = manifest.get("api", API)
    if api not in range(1, API + 1):
        raise PluginError("api must be 1..%d: the loader in this ttcuz speaks Plugin API up to %d" % (API, API))
    permissions = manifest.get("permissions", [])
    if not isinstance(permissions, list) or len(permissions) > 32 or not all(
            isinstance(p, str) and re.fullmatch(r"[A-Za-z][\w.]{1,95}", p) for p in permissions):
        raise PluginError("permissions must be a list of names such as \"network\" or \"android.permission.CAMERA\"")
    uses = manifest.get("uses", {})
    if not isinstance(uses, dict) or not all(isinstance(k, str) and isinstance(v, str) for k, v in uses.items()):
        raise PluginError("uses must be an object: {\"network\": \"why the plugin needs it\"}")
    tiktok = manifest.get("tiktok", "")
    if tiktok not in ("", TIKTOK) + TIKTOK_ALSO:
        raise PluginError("tiktok %r: this ttcuz is TikTok %s" % (tiktok, TIKTOK))
    return manifest


# What in the code needs which permission; the same table the editor in the app marks lines with.
USES = [
    (r"\.http\s*\(", "network", "http()"),
    (r"\.notify\s*\(", "notifications", "notify()"),
    (r"\.(?:copy|paste)\s*\(", "clipboard", "copy()/paste()"),
    (r"\.saveDownload\s*\(", "storage", "saveDownload()"),
    (r"\.accountId\s*\(", "account", "accountId()"),
    (r"\.(?:hostLoader\s*\(|(?:type|method|field)\s*\(\s*\")", "tiktok", "hostLoader()/type()/method()/field()"),
    (r"\.(?:overlay|removeOverlay|find|views)\s*\(", "interface", "overlay()/find()/views()"),
    (r"\.vibrate\s*\(", "android.permission.VIBRATE", "vibrate()"),
    (r"\bon(?:View|Screen)\s*\(", "interface", "onView/onScreen"),
    (r"\bon(?:Text|Nickname)\s*\(", "text", "onText/onNickname"),
    (r"\bonFeed\s*\(", "feed", "onFeed"),
    (r"\bonColour\s*\(", "colours", "onColour"),
    (r"\bonRegion\s*\(", "region", "onRegion"),
    (r"\bonEvent\s*\(", "events", "onEvent"),
    (r"\bonAccountChanged\s*\(", "account", "onAccountChanged"),
    (r"\.(?:blur|motionBlur|colourMatrix|shader|uniform|clearEffect)\s*\(", "effects", "blur()/motionBlur()/shader()"),
    (r"\.(?:modSettings?|setModSetting|refreshModScreen)\s*\(", "mod", "modSetting()/setModSetting()"),
    (r"\bonMod(?:Screen|Panel|Settings)\s*\(", "mod", "onModScreen/onModPanel/onModSettings"),
    (r"ttcuz\(\)\s*\.\s*(?:query|queryOne|tree|hide|unhide|inject)\s*\(", "interface", "query()/tree()/hide()/inject()"),
    (r"ttcuz\(\)\s*\.\s*(?:click|longClick|tap|swipe|type)\s*\((?!\s*\")", "automation", "click()/tap()/swipe()/type()"),
    (r"ttcuz\(\)\s*\.\s*(?:video|videoId|playing|pause|play|speed|saveVideo)\s*\(", "player", "video()/pause()/play()/speed()"),
    (r"ttcuz\(\)\s*\.\s*call\s*\(", "events", "call()"),
    (r"\bonTikTok\s*\(", "watch", "onTikTok"),
]
ANDROID_USE = re.compile(r"(?:\"android\.permission\.|Manifest\.permission\.)([A-Z_]{2,64})")
REASON = re.compile(r"//\s*@(?:uses|разрешение)\s+([A-Za-z][\w.]{1,95})\s*[:—-]?\s*(.*)$")


def _code_of(lines):
    """Each line without comments: (with strings, with strings blanked)."""
    block = False
    for line in lines:
        plain, bare, quote, i = [], [], "", 0
        while i < len(line):
            c = line[i]
            if quote:
                plain.append(c)
                if c == "\\" and i + 1 < len(line):
                    i += 1
                    plain.append(line[i])
                elif c == quote:
                    quote = ""
                    bare.append(c)
            elif block:
                if line.startswith("*/", i):
                    block, i = False, i + 1
            elif line.startswith("/*", i):
                block, i = True, i + 1
            elif line.startswith("//", i):
                break
            else:
                if c in "\"'":
                    quote = c
                plain.append(c)
                bare.append(c)
            i += 1
        yield "".join(plain), "".join(bare)


def permission_uses(text: str):
    """[(line, permission, what)] for one Java file, 1-based lines."""
    out = []
    for number, (plain, bare) in enumerate(_code_of(text.split("\n")), 1):
        for pattern, permission, what in USES:
            if re.search(pattern, bare) and (number, permission) not in [(n, p) for n, p, _ in out]:
                out.append((number, permission, what))
        for match in ANDROID_USE.finditer(plain):
            name = "android.permission." + match.group(1)
            if (number, name) not in [(n, p) for n, p, _ in out]:
                out.append((number, name, match.group(1)))
    return out


def permission_reasons(texts):
    """{permission: reason} from // @uses comments, for manifest "uses"."""
    found = {}
    for text in texts:
        for line in text.split("\n"):
            match = REASON.search(line)
            if not match or not match.group(2).strip():
                continue
            name = match.group(1)
            if re.fullmatch(r"[A-Z_]{2,64}", name):
                name = "android.permission." + name
            reasons = found.setdefault(name, [])
            if match.group(2).strip() not in reasons and len(reasons) < 4:
                reasons.append(match.group(2).strip())
    return {name: "; ".join(reasons)[:300] for name, reasons in found.items()}


def build(folder: str, out: str | None, tools_dir: str) -> str:
    folder = os.path.abspath(folder)
    manifest_path = os.path.join(folder, "manifest.json")
    if not os.path.isfile(manifest_path):
        raise PluginError("no manifest.json in %s" % folder)
    with open(manifest_path, "rb") as handle:
        raw_manifest = handle.read()
    if len(raw_manifest) > MANIFEST_LIMIT:
        raise PluginError("manifest.json is over 64 KiB")
    try:
        manifest = check_manifest(json.loads(raw_manifest.decode("utf-8")))
    except ValueError as error:
        raise PluginError("manifest.json is not valid JSON: %s" % error)

    sources = _java_files(os.path.join(folder, "java"))
    if not sources:
        raise PluginError("no .java under %s" % os.path.join(folder, "java"))

    # every line that uses a permission, with the reason written next to it; the reasons go into the manifest
    texts = {path: open(path, encoding="utf-8", errors="replace").read() for path in sources}
    asked = manifest.get("permissions", [])
    lacking = []
    print("==> What the code does with permissions")
    for path, text in texts.items():
        for number, permission, what in permission_uses(text):
            mark = "ok" if permission in asked else "NOT ASKED"
            print("    %s:%d  %s  %s  [%s]" % (os.path.relpath(path, folder), number, permission, what, mark))
            if permission not in asked and permission not in lacking:
                lacking.append(permission)
    if lacking and manifest.get("api", API) >= 2:
        raise PluginError("the code uses %s, which manifest.json \"permissions\" does not ask for" % ", ".join(lacking))
    reasons = permission_reasons(texts.values())
    if reasons:
        uses = dict(reasons)
        uses.update(manifest.get("uses") or {})  # what the manifest says itself wins
        manifest["uses"] = uses
        raw_manifest = json.dumps(manifest, ensure_ascii=False, indent=2).encode("utf-8")
        for name, why in uses.items():
            print("    why %s: %s" % (name, why))
    icon = os.path.join(folder, "icon.png")
    if os.path.isfile(icon):
        data = open(icon, "rb").read()
        if len(data) > ICON_LIMIT:
            raise PluginError("icon.png is over 1 MiB")
        if not data.startswith(PNG_MAGIC):
            raise PluginError("icon.png is not a PNG")
    else:
        icon = None
    if not os.path.isdir(API_SOURCES):
        raise PluginError("plugin API sources are missing: %s" % API_SOURCES)

    out = os.path.abspath(out or os.path.join(folder, "%s.ctt" % manifest["id"]))
    tools = Toolchain(tools_dir)
    room = tempfile.mkdtemp(prefix="ttcuz-plugin-")
    try:
        api, classes, dex = (os.path.join(room, n) for n in ("api", "classes", "dex"))
        for path in (api, classes, dex):
            os.makedirs(path)

        print("==> Compiling the plugin API")
        _javac(tools.android_jar, api, _java_files(API_SOURCES))

        print("==> Compiling %s %s" % (manifest.get("name", manifest["id"]), manifest.get("version", "1.0")))
        _javac(tools.android_jar + os.pathsep + api, classes, sources)
        entry = os.path.join(classes, *manifest["entry"].split(".")) + ".class"
        if not os.path.isfile(entry):
            raise PluginError("the entry class %s is not among the compiled classes" % manifest["entry"])

        print("==> One dex, without the API")
        _run([_java(), "-cp", tools.r8, "com.android.tools.r8.D8", "--release",
              "--min-api", "24", "--lib", tools.android_jar,
              "--classpath", api, "--output", dex] + _class_files(classes))
        dex_file = os.path.join(dex, "classes.dex")
        extra = [n for n in os.listdir(dex) if n != "classes.dex"]
        if extra:
            raise PluginError("the plugin needs more than one dex (%s); a .ctt holds one" % ", ".join(extra))
        if os.path.getsize(dex_file) > DEX_LIMIT:
            raise PluginError("classes.dex is over 16 MiB")

        print("==> Packing %s" % os.path.basename(out))
        os.makedirs(os.path.dirname(out), exist_ok=True)
        partial = out + ".part"
        with zipfile.ZipFile(partial, "w", zipfile.ZIP_DEFLATED) as zf:
            zf.writestr("manifest.json", raw_manifest)
            zf.write(dex_file, "classes.dex")
            if icon:
                zf.write(icon, "icon.png")
            else:
                print("    no icon.png: the list draws an accent dot instead")
        os.replace(partial, out)
        print("    %s, %d bytes" % (out, os.path.getsize(out)))
        return out
    finally:
        shutil.rmtree(room, ignore_errors=True)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(prog="python3 -m ttcuz.plugin",
                                     description="Pack a folder into a .ctt ttcuz plugin.")
    parser.add_argument("folder", help="the plugin's folder: manifest.json, java/, icon.png")
    parser.add_argument("-o", "--out", help="where the .ctt goes (default: <folder>/<id>.ctt)")
    parser.add_argument("--tools", default=os.path.join(ROOT, "tools"),
                        help="where the downloaded tools are kept")
    args = parser.parse_args(argv)
    try:
        build(args.folder, args.out, args.tools)
    except (PluginError, RuntimeError, SystemExit) as error:
        print("\n\033[1;31mPlugin build failed:\033[0m %s" % error, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
