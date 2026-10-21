const encoder = new TextEncoder();
/** Uncompressed project ZIP, no dependency or executable binary in the browser. */
export function projectZip(project) {
  const entries = [
    { path: 'manifest.json', text: JSON.stringify(project.metadata, null, 2) },
    ...project.files,
  ];
  const chunks = [],
    central = [];
  let offset = 0,
    total = 0;
  function header(size) {
    const data = new Uint8Array(size);
    return [data, new DataView(data.buffer)];
  }
  function crc(bytes) {
    let value = 0xffffffff;
    for (const byte of bytes) {
      value ^= byte;
      for (let n = 0; n < 8; n++) value = (value >>> 1) ^ (value & 1 ? 0xedb88320 : 0);
    }
    return (value ^ 0xffffffff) >>> 0;
  }
  for (const entry of entries) {
    const name = encoder.encode(entry.path),
      bytes = encoder.encode(entry.text),
      hash = crc(bytes);
    const [head, v] = header(30);
    v.setUint32(0, 0x04034b50, true);
    v.setUint16(4, 20, true);
    v.setUint16(6, 0x800, true);
    v.setUint32(14, hash, true);
    v.setUint32(18, bytes.length, true);
    v.setUint32(22, bytes.length, true);
    v.setUint16(26, name.length, true);
    chunks.push(head, name, bytes);
    const [index, d] = header(46);
    d.setUint32(0, 0x02014b50, true);
    d.setUint16(4, 20, true);
    d.setUint16(6, 20, true);
    d.setUint16(8, 0x800, true);
    d.setUint32(16, hash, true);
    d.setUint32(20, bytes.length, true);
    d.setUint32(24, bytes.length, true);
    d.setUint16(28, name.length, true);
    d.setUint32(42, offset, true);
    central.push(index, name);
    total += 46 + name.length;
    offset += 30 + name.length + bytes.length;
  }
  const [end, v] = header(22);
  v.setUint32(0, 0x06054b50, true);
  v.setUint16(8, entries.length, true);
  v.setUint16(10, entries.length, true);
  v.setUint32(12, total, true);
  v.setUint32(16, offset, true);
  return new Blob([...chunks, ...central, end], { type: 'application/zip' });
}
export function download(blob, name) {
  const url = URL.createObjectURL(blob),
    link = document.createElement('a');
  link.href = url;
  link.download = name;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 10000);
}
