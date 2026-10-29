#!/usr/bin/env python3
"""Rewrite one DT_NEEDED entry in an ELF file in place.

Android's APK packager only keeps native libraries named `lib*.so`, so Termux's
`libtalloc.so.2` cannot ship under its SONAME. proot is therefore patched to ask
the linker for `libtalloc.so` instead, and the library ships under that name.

The new name must be no longer than the old one: it is written into the existing
.dynstr slot, and the terminator is moved left. Nothing after the string moves.

Usage:
    python tools/patch_proot_needed.py libproot.so libtalloc.so.2 libtalloc.so
"""
import struct
import sys


def patch(path, old, new):
    if len(new) >= len(old):
        raise SystemExit("replacement must be shorter than the original")

    data = bytearray(open(path, "rb").read())
    if data[:4] != b"\x7fELF" or data[4] != 2:
        raise SystemExit(f"{path}: not a 64-bit ELF")

    phoff = struct.unpack_from("<Q", data, 0x20)[0]
    phentsize, phnum = struct.unpack_from("<HH", data, 0x36)

    segments = []
    dynamic = None
    for i in range(phnum):
        off = phoff + i * phentsize
        p_type = struct.unpack_from("<I", data, off)[0]
        p_offset, p_vaddr = struct.unpack_from("<QQ", data, off + 8)
        p_filesz = struct.unpack_from("<Q", data, off + 32)[0]
        if p_type == 1:  # PT_LOAD
            segments.append((p_vaddr, p_offset, p_filesz))
        elif p_type == 2:  # PT_DYNAMIC
            dynamic = (p_offset, p_filesz)
    if dynamic is None:
        raise SystemExit(f"{path}: no PT_DYNAMIC segment")

    def vaddr_to_offset(addr):
        for vaddr, offset, size in segments:
            if vaddr <= addr < vaddr + size:
                return offset + (addr - vaddr)
        return None

    entries = []
    for off in range(dynamic[0], dynamic[0] + dynamic[1], 16):
        tag, value = struct.unpack_from("<qQ", data, off)
        entries.append((tag, value))

    strtab, strsz = None, None
    for tag, value in entries:
        if tag == 5:  # DT_STRTAB
            strtab = value
        elif tag == 10:  # DT_STRSZ
            strsz = value
    if strtab is None:
        raise SystemExit(f"{path}: no DT_STRTAB")
    strtab_off = vaddr_to_offset(strtab)

    patched = 0
    for tag, value in entries:
        if tag != 1:  # DT_NEEDED
            continue
        start = strtab_off + value
        end = data.index(b"\0", start)
        if bytes(data[start:end]) != old.encode():
            continue
        replacement = new.encode()
        data[start:start + len(replacement)] = replacement
        data[start + len(replacement)] = 0
        patched += 1

    if patched == 0:
        raise SystemExit(f"{path}: DT_NEEDED entry {old!r} not found")

    open(path, "wb").write(data)
    print(f"patched {patched} DT_NEEDED entr{'y' if patched == 1 else 'ies'}: {old} -> {new}")


if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise SystemExit(__doc__)
    patch(sys.argv[1], sys.argv[2], sys.argv[3])
