#!/usr/bin/env python3
"""Recover one source-pinned VL80S pneumatic raster without synthesizing geometry."""

import hashlib
import json
import sys
import zipfile
from pathlib import Path

from jpeg_dimensions import jpeg_dimensions

ROOT = Path(__file__).resolve().parents[1]
ARCHIVE = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "build/vl80s-pneumatic-legacy.zip"
SOURCE_COMMIT = "a3c93873bd128d4769d004098981235c169fd085"
SOURCE_ARCHIVE_BLOB = "6c3c7edc1713c1c901a566fd8a81a2494ee4ac58"
SOURCE_ENTRY = (
    "RailBrakeCalculator/app/src/main/res/drawable-nodpi/"
    "vl80s_pneumatic_scheme.jpg"
)
OUT = ROOT / "content-packs/electric/vl80s/atlas/interactive/pneumatic-scheme.jpg"
FLOW = ROOT / "content-packs/electric/vl80s/atlas/interactive/pneumatic-flows.json"

if not ARCHIVE.is_file():
    raise SystemExit(f"Legacy archive not found: {ARCHIVE}")
with zipfile.ZipFile(ARCHIVE) as archive:
    matching = [entry for entry in archive.namelist()
                if entry.endswith("/vl80s_pneumatic_scheme.jpg")]
    if matching != [SOURCE_ENTRY]:
        raise SystemExit(f"Pneumatic source path ambiguous: {matching}")
    payload = archive.read(SOURCE_ENTRY)

if not payload.startswith(b"\xff\xd8"):
    raise SystemExit("Recovered pneumatic image is not JPEG")
if len(payload) != 67881:
    raise SystemExit(f"Recovered JPEG size differs from pinned archive: {len(payload)}")
if jpeg_dimensions(payload) != (1181, 573):
    raise SystemExit(
        f"Legacy image does not match flow coordinates: {jpeg_dimensions(payload)}"
    )

document = json.loads(FLOW.read_text(encoding="utf-8"))
assert document["modelId"] == "vl80s"
assert document["semantics"]["coordinateSpace"] == {"width": 1181, "height": 573}
assert document["semantics"]["actionAuthority"] == "NONE"
assert document["semantics"]["coordinateClaim"] == (
    "LEGACY_PRESENTATION_OVERLAY_NOT_EXACT_PIPE_GEOMETRY"
)

sha256 = hashlib.sha256(payload).hexdigest()
background = {
    "assetPath": "electric/vl80s/atlas/interactive/pneumatic-scheme.jpg",
    "sha256": sha256,
    "width": 1181,
    "height": 573,
    "source": {
        "repository": "DomEnota-maker/Beta",
        "commit": SOURCE_COMMIT,
        "archive": "RailBrakeCalculator.zip",
        "archiveBlobSha": SOURCE_ARCHIVE_BLOB,
        "entry": SOURCE_ENTRY,
    },
}
document["semantics"]["background"] = background
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_bytes(payload)
FLOW.write_text(
    json.dumps(document, indent=2, ensure_ascii=False) + "\n",
    encoding="utf-8",
)
print("VL80S_PNEUMATIC_BACKGROUND_RECOVERED")
print("size", len(payload), "pixels", 1181, 573, "sha256", sha256)
print("source", SOURCE_ENTRY)
