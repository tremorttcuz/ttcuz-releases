"""Build a native .ctt with the ttcuz SDK; no account, store or signing key needed."""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import subprocess
import tempfile
import zipfile

API = 1
PREFIX = "cat/ttcuz/framework/plugin/"


def write_sdk(classes: str, destination: str) -> int:
    root = Path(classes)
    files = sorted((root / PREFIX).rglob("*.class"))
    if not files:
        raise ValueError("Plugin API classes are missing")
    with zipfile.ZipFile(destination, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\nTtcuz-Plugin-API: 1\n\n")
        for file in files:
            archive.write(file, file.relative_to(root).as_posix())
    return len(files)


def manifest(folder: Path) -> dict:
    import re
    value = json.loads((folder / "manifest.json").read_text(encoding="utf-8-sig"))
    if not isinstance(value, dict):
        raise ValueError("manifest.json must contain an object")
    if not isinstance(value.get("id"), str) or not re.fullmatch(r"[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}", value["id"]):
        raise ValueError("Invalid plugin id")
    if not isinstance(value.get("entry"), str) or not value["entry"] or len(value["entry"]) > 512:
        raise ValueError("A plugin entry class is required")
    if type(value.get("api", API)) is not int or value.get("api", API) != API:
        raise ValueError("Unsupported ttcuz plugin API")
    value["api"] = API
    return value


def package(folder: Path, dex: Path, destination: Path, metadata: dict) -> None:
    temporary = destination.with_suffix(destination.suffix + ".tmp")
    destination.parent.mkdir(parents=True, exist_ok=True)
    try:
        with zipfile.ZipFile(temporary, "w", zipfile.ZIP_DEFLATED) as archive:
            archive.writestr("manifest.json", json.dumps(metadata, ensure_ascii=False, indent=2))
            files = sorted(dex.glob("classes*.dex"))
            if not files or not (dex / "classes.dex").is_file():
                raise ValueError("Compiler did not produce classes.dex")
            for file in files:
                archive.write(file, file.name)
            for name in ("assets", "lib"):
                for file in sorted((folder / name).rglob("*")):
                    if file.is_symlink():
                        raise ValueError("Bundle assets must not be symbolic links")
                    if file.is_file():
                        archive.write(file, file.relative_to(folder).as_posix())
            resources = folder / "resources.apk"
            if resources.is_file():
                archive.write(resources, "resources.apk")
        temporary.replace(destination)
    finally:
        temporary.unlink(missing_ok=True)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("folder", type=Path)
    parser.add_argument("--sdk", required=True, type=Path, help="*-plugin-sdk.jar emitted with the APK")
    parser.add_argument("--android", default="tools/android.jar", type=Path)
    parser.add_argument("--r8", default="tools/r8.jar", type=Path)
    parser.add_argument("--classpath", action="append", default=[])
    parser.add_argument("--include", action="append", default=[], help="JAR dependency to compile and include in the plugin DEX")
    parser.add_argument("--release", type=int, choices=[8, 11, 17], default=8)
    parser.add_argument("-o", "--out", type=Path)
    args = parser.parse_args(argv)
    try:
        folder = args.folder.resolve()
        metadata = manifest(folder)
        sources = sorted((folder / "java").rglob("*.java"))
        if not sources:
            raise ValueError("Put Java sources in java/")
        for file in (args.sdk, args.android, args.r8):
            if not file.is_file():
                raise ValueError("Missing tool or SDK: " + str(file))
        from .toolchain import _java
        java = Path(_java())
        javac = java.with_name("javac.exe" if os.name == "nt" else "javac")
        with tempfile.TemporaryDirectory(prefix="ttcuz-plugin-") as temporary:
            work = Path(temporary)
            classes, dex = work / "classes", work / "dex"
            classes.mkdir(); dex.mkdir()
            source_args = work / "sources.args"
            source_args.write_text("\n".join('"' + str(p).replace("\\", "/") + '"' for p in sources), encoding="utf8")
            cp = os.pathsep.join(map(str, [args.sdk.resolve(), args.android.resolve(), *args.classpath, *args.include]))
            subprocess.run([str(javac), "-encoding", "UTF-8", "--release", str(args.release), "-cp", cp,
                            "-d", str(classes), "@" + str(source_args)], check=True)
            program = work / "plugin.jar"
            with zipfile.ZipFile(program, "w") as archive:
                for file in sorted(classes.rglob("*.class")):
                    archive.write(file, file.relative_to(classes).as_posix())
            subprocess.run([str(java), "-cp", str(args.r8.resolve()), "com.android.tools.r8.D8", "--release",
                            "--min-api", "23", "--lib", str(args.android.resolve()), "--classpath", str(args.sdk.resolve()),
                            "--output", str(dex), str(program), *args.include], check=True)
            destination = args.out or folder.with_name(metadata["id"] + ".ctt")
            package(folder, dex, destination, metadata)
        print(str(destination.resolve()))
        return 0
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        print("Plugin build failed: " + str(error))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
