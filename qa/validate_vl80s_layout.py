#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / "content-packs/electric/vl80s"
LAYOUT = MODEL / "atlas/interactive/layout-hotspots.json"
SCHEME_INDEX = MODEL / "atlas/schemes/index.json"

document = json.loads(LAYOUT.read_text(encoding="utf-8"))
assert document["schemaVersion"] == 1
assert document["modelId"] == "vl80s"
assert document["id"] == "vl80s.layout.top-view"
assert document["sourceSnapshot"] == {
    "repository": "DomEnota-maker/Test-",
    "commit": "3176d6ee228f0ed4b78371f45209e10c1c724eff",
    "source": "app/src/main/java/ru/railbrake/calculator/core/KnowledgeRepository.kt",
    "blobSha": "b37ead2d66c0c976bcc850f9cde4fddc0e198fb3",
}
assert document["semantics"]["coordinateSpace"] == "NORMALIZED_0_1"
assert document["semantics"]["spatialClaim"] == "TRAINING_REFERENCE_NOT_EXACT_MOUNTING"
assert document["semantics"]["canonicalLinkRule"] == "EXACT_LEGACY_ID_MATCH_ONLY"

hotspots = document["hotspots"]
assert document["hotspotCount"] == 15
assert len(hotspots) == 15
assert len({item["id"] for item in hotspots}) == 15

equipment_ids = set()
for path in sorted((MODEL / "atlas/equipment").glob("*.pack.json")):
    pack = json.loads(path.read_text(encoding="utf-8"))
    equipment_ids.update(item["id"] for item in pack["entries"])

linked = []
for item in hotspots:
    bounds = item["bounds"]
    assert 0 <= bounds["left"] < bounds["right"] <= 1, item["id"]
    assert 0 <= bounds["top"] < bounds["bottom"] <= 1, item["id"]
    assert item["title"].strip()
    assert item["subtitle"].strip()
    assert item["details"].strip()
    if item.get("equipmentId"):
        assert item["equipmentId"] in equipment_ids, item["id"]
        linked.append((item["id"], item["equipmentId"]))

assert document["canonicalEquipmentLinkCount"] == 4
assert set(linked) == {
    ("alsn", "VL-EQ-SF-001"),
    ("phase-splitter", "VL-EQ-AU-001"),
    ("transformer", "VL-EQ-TR-004"),
    ("burt", "VL-EQ-TR-005"),
}

index = json.loads(SCHEME_INDEX.read_text(encoding="utf-8"))
assert index["layoutMaps"] == [
    "electric/vl80s/atlas/interactive/layout-hotspots.json"
]

print("VL80S_LAYOUT_HOTSPOTS_PASS", len(hotspots), len(linked))
