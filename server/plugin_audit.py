"""Publication gate. Static checks reject known concealment; not a malware proof."""

import hashlib, io, json, re, struct, zipfile, zlib

MAX_DEX = 16 * 1024 * 1024


class Dex:
    def __init__(self, data):
        self.data = data
        self.cache = {}
        self.text_bytes = 0
        self.uleb_reads = 0
        if (
            len(data) < 112
            or len(data) > MAX_DEX
            or data[:4] != b"dex\n"
            or data[4:8] not in [b"035\0", b"037\0", b"038\0", b"039\0", b"040\0"]
        ):
            raise ValueError("Некорректный DEX")
        if self.u32(32) != len(data) or self.u32(36) != 112 or self.u32(40) != 0x12345678:
            raise ValueError("Некорректный заголовок DEX")
        if hashlib.sha1(data[32:]).digest() != data[12:32] or zlib.adler32(
            data[12:]
        ) & 0xFFFFFFFF != self.u32(8):
            raise ValueError("Повреждён DEX")
        self.sc, self.so = self.u32(56), self.u32(60)
        self.tc, self.to = self.u32(64), self.u32(68)
        self.mc, self.mo = self.u32(88), self.u32(92)
        for off, count, size in [
            (self.so, self.sc, 4),
            (self.to, self.tc, 4),
            (self.mo, self.mc, 8),
            (self.u32(84), self.u32(80), 8),
            (self.u32(100), self.u32(96), 32),
        ]:
            self.table(off, count, size)
        for i in range(self.sc):
            self.string(i)

    def table(self, off, count, size):
        if count > 100000 or bool(off) != bool(count) or off + count * size > len(self.data):
            raise ValueError("Некорректная таблица DEX")

    def u32(self, p):
        if p < 0 or p + 4 > len(self.data):
            raise ValueError("Выход за границы DEX")
        return struct.unpack_from("<I", self.data, p)[0]

    def u16(self, p):
        if p < 0 or p + 2 > len(self.data):
            raise ValueError("Выход за границы DEX")
        return struct.unpack_from("<H", self.data, p)[0]

    def uleb(self, pos):
        value = 0
        for i in range(5):
            self.uleb_reads += 1
            if self.uleb_reads > 1000000 or pos >= len(self.data):
                raise ValueError("Слишком сложные данные DEX")
            byte = self.data[pos]
            pos += 1
            value |= (byte & 127) << (i * 7)
            if not byte & 128:
                return value, pos
        raise ValueError("Некорректный ULEB128")

    def check_class_data(self, pos):
        if not pos:
            return
        counts = []
        for _ in range(4):
            value, pos = self.uleb(pos)
            counts.append(value)
        if sum(counts) > 100000:
            raise ValueError("Слишком сложный класс")
        for _ in range(counts[0] + counts[1]):
            _, pos = self.uleb(pos)
            _, pos = self.uleb(pos)
        for _ in range(counts[2] + counts[3]):
            _, pos = self.uleb(pos)
            flags, pos = self.uleb(pos)
            _, pos = self.uleb(pos)

    def string(self, index):
        if index >= self.sc:
            raise ValueError("Некорректная строка DEX")
        pos = self.u32(self.so + index * 4)
        if pos in self.cache:
            return self.cache[pos]
        start = pos
        for _ in range(5):
            if pos >= len(self.data):
                raise ValueError("Некорректная строка DEX")
            value = self.data[pos]
            pos += 1
            if not value & 128:
                break
        else:
            raise ValueError("Некорректный ULEB128")
        end = self.data.find(b"\0", pos, min(len(self.data), pos + 65537))
        if end < 0:
            raise ValueError("Непроверяемая строка DEX")
        self.text_bytes += end - pos
        if self.text_bytes > MAX_DEX:
            raise ValueError("Слишком сложный DEX")
        value = self.data[pos:end].decode("utf-8", errors="replace")
        self.cache[start] = value
        return value

    def type(self, index):
        if index >= self.tc:
            raise ValueError("Некорректный тип DEX")
        return self.string(self.u32(self.to + index * 4))


def inspect_dex(data):
    d = Dex(data)
    classes, off = d.u32(96), d.u32(100)
    if not classes:
        raise ValueError("В плагине нет классов")
    if classes > 100000 or off + classes * 32 > len(data):
        raise ValueError("Некорректная таблица классов DEX")
    for i in range(classes):
        d.type(d.u32(off + i * 32))
        d.check_class_data(d.u32(off + i * 32 + 24))
    return d


def inspect_package(raw):
    if len(raw) > 18 * 1024 * 1024:
        raise ValueError("Слишком большой пакет")
    files = {}
    limits = {"manifest.json": 65536, "classes.dex": MAX_DEX, "icon.png": 1024 * 1024}
    with zipfile.ZipFile(io.BytesIO(raw)) as archive:
        for info in archive.infolist():
            if (
                info.filename not in limits
                or info.filename in files
                or info.is_dir()
                or info.file_size > limits[info.filename]
            ):
                raise ValueError("Лишние, повторяющиеся или слишком большие файлы плагина")
            with archive.open(info) as entry:
                content = entry.read(limits[info.filename] + 1)
            if len(content) > limits[info.filename]:
                raise ValueError("Слишком большой распакованный файл")
            files[info.filename] = content
    if not {"manifest.json", "classes.dex"} <= files.keys():
        raise ValueError("Нет manifest.json или classes.dex")
    manifest = json.loads(files["manifest.json"].decode("utf-8"))
    if not isinstance(manifest, dict) or not re.fullmatch(
        "[A-Za-z0-9][A-Za-z0-9._-]{0,79}", str(manifest.get("id", ""))
    ):
        raise ValueError("Некорректный ID плагина")
    dex = inspect_dex(files["classes.dex"])
    entry = "L" + str(manifest.get("entry", "")).replace(".", "/") + ";"
    if not any(dex.type(dex.u32(dex.u32(100) + i * 32)) == entry for i in range(dex.u32(96))):
        raise ValueError("Entry-класс отсутствует в DEX")
    return manifest, files.get("icon.png", b""), hashlib.sha256(raw).hexdigest()


def analyze_package(raw):
    """Compatibility schema for the catalog; no code capability policy."""
    manifest, icon, digest = inspect_package(raw)
    return {
        "manifest": manifest,
        "icon": icon,
        "sha256": digest,
        "blocks": [],
        "capabilities": [],
        "caps": [],
        "domains": [],
        "notes": [],
        "verdict": "ok",
    }
