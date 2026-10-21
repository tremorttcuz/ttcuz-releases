import struct, hashlib, zlib


def dex(owner="Ljava/lang/String;", method="valueOf", literal="", source=None, define=False):
    strings = [owner, method, literal, "V"]
    if source is not None:
        strings.append(source)
    data = bytearray(112 + len(strings) * 4 + 8 + 8 + 12)
    so = 112
    to = so + len(strings) * 4
    mo = to + 8
    po = mo + 8
    for i, text in enumerate(strings):
        struct.pack_into("<I", data, so + i * 4, len(data))
        raw = text.encode()
        length = len(raw)
        prefix = bytearray()
        while length >= 128:
            prefix.append((length & 127) | 128)
            length >>= 7
        prefix.append(length)
        data += prefix + raw + b"\0"
    struct.pack_into("<II", data, to, 0, 3)
    struct.pack_into("<HHI", data, mo, 0, 0, 1)
    struct.pack_into("<III", data, po, 3, 1, 0)
    if define:
        class_off = len(data)
        data += struct.pack(
            "<IIIIIIII", 0, 1, 0xFFFFFFFF, 0, 4 if source is not None else 0xFFFFFFFF, 0, 0, 0
        )
        struct.pack_into("<II", data, 96, 1, class_off)
    data[:8] = b"dex\n035\0"
    for offset, value in {
        32: len(data),
        36: 112,
        40: 0x12345678,
        56: len(strings),
        60: so,
        64: 2,
        68: to,
        72: 1,
        76: po,
        88: 1,
        92: mo,
        104: len(data) - (po + 12),
        108: po + 12,
    }.items():
        struct.pack_into("<I", data, offset, value)
    data[12:32] = hashlib.sha1(data[32:]).digest()
    struct.pack_into("<I", data, 8, zlib.adler32(data[12:]) & 0xFFFFFFFF)
    return data
