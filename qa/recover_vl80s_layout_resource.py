#!/usr/bin/env python3
import hashlib
import json
import sys
import zipfile
from pathlib import Path, PurePosixPath

ROOT = Path(__file__).resolve().parents[1]
ARCHIVE = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "build/vl80s-layout-legacy.zip"
LAYOUT = ROOT / "content-packs/electric/vl80s/atlas/interactive/layout-hotspots.json"
OUT_DIR = LAYOUT.parent

SOURCE_REPOSITORY = "DomEnota-maker/Beta"
SOURCE_COMMIT = "a3c93873bd128d4769d004098981235c169fd085"
SOURCE_ARCHIVE = "RailBrakeCalculator.zip"
SOURCE_ARCHIVE_BLOB_SHA = "6c3c7edc1713c1c901a566fd8a81a2494ee4ac58"
TARGET_BASENAME = "vl80s_layout_section1"
SUPPORTED = {".png", ".jpg", ".jpeg", ".webp"}

if not ARCHIVE.is_file():
    raise SystemExit(f"archive not found: {ARCHIVE}")

with zipfile.ZipFile(ARCHIVE) as zf:
    matches = []
    for name in zf.namelist():
        pure = PurePosixPath(name)
        if (
            pure.stem.lower() == TARGET_BASENAME
            and pure.suffix.lower() in SUPPORTED
        ):
            matches.append(name)

    if not matches:
        related = [
            name for name in zf.namelist()
            if "vl80" in name.lower() and "layout" in name.lower()
        ]
        print("No exact layout resource found.")
        print("Related archive entries:")
        for name in related[:100]:
            print(name)
        raise SystemExit("vl80s_layout_section1 resource is missing from legacy archive")

    payloads = {name: zf.read(name) for name in matches}

hashes = {name: hashlib.sha256(data).hexdigest() for name, data in payloads.items()}
unique_hashes = set(hashes.values())
if len(unique_hashes) != 1:
    raise SystemExit(
        "multiple non-identical vl80s_layout_section1 resources found: "
        + json.dumps(hashes, ensure_ascii=False)
    )

selected = sorted(matches, key=lambda item: (len(item), item))[0]
data = payloads[selected]
suffix = PurePosixPath(selected).suffix.lower()

if suffix == ".png" and not data.startswith(b"\x89PNG\r\n\x1a\n"):
    raise SystemExit("PNG signature mismatch")
if suffix in {".jpg", ".jpeg"} and not data.startswith(b"\xff\xd8"):
    raise SystemExit("JPEG signature mismatch")
if suffix == ".webp" and not (data.startswith(b"RIFF") and data[8:12] == b"WEBP"):
    raise SystemExit("WEBP signature mismatch")

OUT_DIR.mkdir(parents=True, exist_ok=True)
target = OUT_DIR / f"layout-section1{suffix}"
for old in OUT_DIR.glob("layout-section1.*"):
    if old != target and old.suffix.lower() in SUPPORTED:
        old.unlink()
target.write_bytes(data)

sha256 = hashlib.sha256(data).hexdigest()
document = json.loads(LAYOUT.read_text(encoding="utf-8"))
document["background"] = {
    "assetPath": str(target.relative_to(ROOT / "content-packs")).replace("\\", "/"),
    "status": "RECOVERED_FROM_VERIFIED_LEGACY_ARCHIVE",
    "sha256": sha256,
    "source": {
        "repository": SOURCE_REPOSITORY,
        "commit": SOURCE_COMMIT,
        "archive": SOURCE_ARCHIVE,
        "archiveBlobSha": SOURCE_ARCHIVE_BLOB_SHA,
        "entry": selected,
    },
}
LAYOUT.write_text(
    json.dumps(document, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)

print("VL80S_LAYOUT_RESOURCE_RECOVERED")
print("entry", selected)
print("output", target.relative_to(ROOT))
print("sha256", sha256)
print("bytes", len(data))
