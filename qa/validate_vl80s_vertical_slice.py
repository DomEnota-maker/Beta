#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACKS = [
    ROOT / "content-packs/electric/vl80s/atlas/pantograph.vertical.pack.json",
    ROOT / "content-packs/electric/vl80s/technical-data/pantograph.vertical.pack.json",
    ROOT / "content-packs/electric/vl80s/diagnostics/recommended/pantograph-no-rise.candidate.pack.json",
]

documents = [json.loads(path.read_text(encoding="utf-8")) for path in PACKS]
entries = {}
types = {}

for document in documents:
    manifest = document["manifest"]
    assert manifest["modelIds"] == ["vl80s"]
    assert manifest["variantIds"] == ["vl80s-general"]
    assert manifest["family"] == "ELECTRIC"

    ids = [entry["id"] for entry in document["entries"]]
    assert set(ids) == set(manifest["entries"]), f"manifest mismatch: {manifest['packId']}"

    for entry in document["entries"]:
        entry_id = entry["id"]
        assert entry_id not in entries, f"duplicate canonical id: {entry_id}"
        assert entry["owner"] == {"kind": "MODEL", "modelId": "vl80s"}
        assert entry["applicability"]["modelIds"] == ["vl80s"]
        assert entry["applicability"]["variantIds"] == ["vl80s-general"]
        entries[entry_id] = entry
        types[entry_id] = entry["type"]

expected_type = {
    "RELATED_SCENARIO": "DIAGNOSTIC_SCENARIO",
    "EQUIPMENT": "EQUIPMENT",
    "ATLAS_SCHEME": "ATLAS_SCHEME",
    "ACCEPTANCE_ITEM": "ACCEPTANCE_ITEM",
    "TECHNICAL_DATA": "TECHNICAL_DATA",
    "KNOWLEDGE": "KNOWLEDGE",
    "SOURCE": "SOURCE",
    "FIRST_AID": "FIRST_AID",
    "SAFETY": "SAFETY",
}

for entry in entries.values():
    for link in entry.get("links", []):
        target = link["targetId"]
        assert target in entries, f"missing vertical-slice target: {entry['id']} -> {target}"
        assert types[target] == expected_type[link["type"]], (
            f"type mismatch: {entry['id']} -> {target}"
        )
        assert link.get("scope", "SAME_MODEL") == "SAME_MODEL"

assert entries["VL-EQ-HV-002"]["title"] == "Токоприёмник"
assert entries["VL-EQ-PN-002"]["title"] == "Клапан 245"
assert set(entries["vl80s-panto-tech-002"]["sourceRefs"]) == {
    "VL80S-PANTO-S02",
    "VL80S-PANTO-S03",
    "VL80S-PANTO-S06",
}

candidate = entries["vl80s.diag.pantograph-no-rise"]
assert candidate["publicationStatus"] == "CANDIDATE"
assert candidate["actionDisposition"] == "CONDITIONAL_ACTION"
assert "GOLDEN REFERENCE PASS" in " ".join(candidate["details"])
assert all(
    entry["publicationStatus"] == "ACTIVE"
    for entry_id, entry in entries.items()
    if entry_id != "vl80s.diag.pantograph-no-rise"
)

print("VL80S_VERTICAL_SLICE_PACK_PASS")
