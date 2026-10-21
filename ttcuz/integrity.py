"""Sign a complete APK content inventory with the author's separate private key."""

from pathlib import Path
import hashlib, os, re, subprocess
from .toolchain import _java, _keytool

MANIFEST = "assets/ttcuz/integrity.manifest"
SIGNATURE = "assets/ttcuz/integrity.sig"


def excluded(name):
    upper = name.upper()
    return (
        name in {MANIFEST, SIGNATURE}
        or upper == "META-INF/MANIFEST.MF"
        or (upper.startswith("META-INF/") and upper.endswith((".SF", ".RSA", ".DSA", ".EC")))
    )


def signer(root, workspace):
    folder = Path(workspace) / "integrity"
    folder.mkdir(parents=True, exist_ok=True)
    source = Path(root) / "ttcuz/SealSigner.java"
    javac = str(Path(_java()).with_name("javac.exe" if os.name == "nt" else "javac"))
    subprocess.run(
        [javac, "--release", "8", "-d", str(folder), str(source)], check=True, capture_output=True
    )
    key = Path(os.environ.get("TTCUZ_INTEGRITY_KEY", str(Path.home() / ".ttcuz/integrity.p12")))
    if not key.is_file():
        raise RuntimeError(
            "Author integrity key is missing; it must not be included in source archives"
        )
    command = [_java(), "-cp", str(folder), "SealSigner", str(key)]
    public = subprocess.run(command, check=True, capture_output=True, text=True).stdout.strip()
    java = (Path(root) / "inject/java/cat/ttcuz/framework/IntegrityKey.java").read_text(
        encoding="utf8"
    )
    pinned = re.search(r'PUBLIC_KEY\s*=\s*"([A-Za-z0-9+/=]+)"', java).group(1)
    if public != pinned:
        raise RuntimeError("Integrity key does not match the pinned author public key")
    return command, folder


def apk_certificate(key, alias, password):
    if not key:
        raise RuntimeError("Integrity-protected builds require an explicit APK signing keystore")
    env = dict(os.environ, TTCUZ_APK_CERT_PASSWORD=password)
    result = subprocess.run(
        [
            _keytool(),
            "-exportcert",
            "-keystore",
            str(key),
            "-alias",
            alias,
            "-storepass:env",
            "TTCUZ_APK_CERT_PASSWORD",
        ],
        env=env,
        check=True,
        capture_output=True,
    )
    return hashlib.sha256(result.stdout).hexdigest()


def seal(apk, root, workspace, package, launcher, certificate):
    command, folder = signer(root, workspace)
    lines = [
        "TTCUZ-INTEGRITY-1",
        "package=" + package,
        "certificate=" + certificate,
        "launcher=" + launcher,
    ]
    seen = set()
    for name in sorted(apk.names()):
        if name in seen:
            raise ValueError("Duplicate APK entry: " + name)
        seen.add(name)
        if name.endswith("/") or excluded(name):
            continue
        if any(c in name for c in "\n\r\t\\") or name.startswith("/") or "../" in name:
            raise ValueError("Invalid APK entry name")
        lines.append(hashlib.sha256(apk.read(name)).hexdigest() + "\t" + name)
    payload = ("\n".join(lines) + "\n").encode("utf8")
    if len(payload) > 4 * 1024 * 1024:
        raise ValueError("Integrity inventory exceeds 4 MiB")
    path = folder / "inventory"
    signature = folder / "signature"
    path.write_bytes(payload)
    subprocess.run(command + [str(path), str(signature)], check=True, capture_output=True)
    apk.add(MANIFEST, payload)
    apk.add(SIGNATURE, signature.read_bytes())
    return len(lines) - 4
