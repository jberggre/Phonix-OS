#!/usr/bin/env python3
"""Extract selected files from a Debian .deb (an `ar` archive) using only stdlib.

Used to pull the Termux-built `proot` binary and its shared libraries out of the
Termux apt packages, so Phonix can ship them without needing a Linux host.

Usage:
    python tools/deb_extract.py package.deb out_dir file1 [file2 ...]

Paths inside the package are given relative to the data root, e.g.
    data/data/com.termux/files/usr/bin/proot
"""
import io
import os
import sys
import tarfile


def iter_ar_members(path):
    """Yield (name, bytes) for each member of an `ar` archive."""
    with open(path, "rb") as handle:
        magic = handle.read(8)
        if magic != b"!<arch>\n":
            raise SystemExit(f"{path}: not an ar archive")
        while True:
            header = handle.read(60)
            if len(header) < 60:
                return
            # GNU ar pads names with spaces and deb members carry a trailing "/".
            name = header[0:16].decode("utf-8", "replace").strip().rstrip("/")
            size = int(header[48:58].decode("utf-8", "replace").strip())
            data = handle.read(size)
            if size % 2 == 1:          # members are 2-byte aligned
                handle.read(1)
            yield name, data


def main():
    if len(sys.argv) < 4:
        raise SystemExit(__doc__)

    deb_path, out_dir, wanted = sys.argv[1], sys.argv[2], sys.argv[3:]
    os.makedirs(out_dir, exist_ok=True)

    data_member = None
    for name, data in iter_ar_members(deb_path):
        if name.startswith("data.tar"):
            data_member = (name, data)
            break
    if data_member is None:
        raise SystemExit(f"{deb_path}: no data.tar member found")

    name, blob = data_member
    if name.endswith(".zst"):
        # Termux packages use zstd; Python 3.14 ships compression.zstd.
        import compression.zstd
        blob = compression.zstd.decompress(blob)
        mode = "r:"
    elif name.endswith(".xz"):
        mode = "r:xz"
    elif name.endswith(".gz"):
        mode = "r:gz"
    else:
        mode = "r:"
    extracted = []

    with tarfile.open(fileobj=io.BytesIO(blob), mode=mode) as tar:
        for entry in tar.getmembers():
            normalised = entry.name.lstrip("./")
            for target in wanted:
                if normalised == target and entry.isfile():
                    payload = tar.extractfile(entry).read()
                    destination = os.path.join(out_dir, os.path.basename(target))
                    with open(destination, "wb") as out:
                        out.write(payload)
                    os.chmod(destination, 0o755)
                    extracted.append(destination)
                    break

    if not extracted:
        raise SystemExit(f"{deb_path}: none of the requested files were present")

    for path in extracted:
        print(f"extracted {path} ({os.path.getsize(path)} bytes)")


if __name__ == "__main__":
    main()
