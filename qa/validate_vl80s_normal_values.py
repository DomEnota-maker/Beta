#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL_ROOT = ROOT / "content-packs/electric/vl80s"
PACK_PATH = MODEL_ROOT / "technical-data/normal-values.pack.json"
INDEX_PATH = MODEL_ROOT / "runtime-index.json"

document = json.loads(PACK_PATH.read_text(encoding="utf-8"))
index = json.loads(INDEX_PATH.read_text(encoding="utf-8"))
entries = {entry["id"]: entry for entry in document["entries"]}

expected = {
    "VL-TD-NORM-CONTROL-VOLTAGE": {
        "alias": "control-voltage",
        "targets": {"VL-EQ-TR-001", "VL-EQ-TR-002", "VL-EQ-PN-002", "VL-EQ-PR-001"},
    },
    "VL-TD-NORM-CONTACT-NETWORK": {
        "alias": "contact-network",
        "targets": {"VL-EQ-HV-002", "VL-EQ-HV-001", "VL-EQ-TR-004", "VL-EQ-HV-003"},
    },
    "VL-TD-NORM-GV-STATE": {
        "alias": "gv-state",
        "targets": {"VL-EQ-HV-001", "VL-EQ-PR-001"},
    },
    "VL-TD-NORM-COOLING": {
        "alias": "cooling",
        "targets": {"VL-EQ-AU-002", "VL-EQ-TR-003", "VL-EQ-TR-006", "VL-EQ-TR-005"},
    },
    "VL-TD-NORM-PNEUMATIC": {
        "alias": "pneumatic",
        "targets": {"VL-EQ-AU-003", "VL-EQ-PN-001", "VL-EQ-BR-003", "VL-EQ-BR-004", "VL-EQ-BR-001", "VL-EQ-BR-002"},
    },
    "VL-TD-NORM-TEMPERATURE": {
        "alias": "temperature",
        "targets": {"VL-EQ-TR-004", "VL-EQ-TR-003", "VL-EQ-TR-006", "VL-EQ-AU-003", "VL-EQ-MC-002"},
    },
}

assert set(entries) == set(expected)
assert set(document["manifest"]["entries"]) == set(entries)
assert document["manifest"]["modelIds"] == ["vl80s"]
assert document["manifest"]["family"] == "ELECTRIC"

equipment = {}
for relative in index["packs"]:
    if "/atlas/equipment/" not in relative:
        continue
    pack = json.loads((ROOT / "content-packs" / relative).read_text(encoding="utf-8"))
    for entry in pack["entries"]:
        equipment[entry["id"]] = entry
assert len(equipment) == 89

for entry_id, spec in expected.items():
    entry = entries[entry_id]
    assert entry["aliases"] == [spec["alias"]]
    assert entry["publicationStatus"] == "ACTIVE"
    assert entry["actionDisposition"] == "INFORMATION_ONLY"
    assert entry["migrationRefs"]["sourceFile"] == "Vl80sNormalValues.kt"
    assert entry["migrationRefs"]["sourceBlobSha"] == "9aacbb1982e0143d6efc40e3579c47c63074b176"

    targets = {
        link["targetId"]
        for link in entry["links"]
        if link["type"] == "EQUIPMENT"
    }
    assert targets == spec["targets"], (entry_id, targets)

    for equipment_id in targets:
        assert equipment_id in equipment
        assert any(
            link["type"] == "TECHNICAL_DATA"
            and link["targetId"] == entry_id
            and link.get("role") == "normal-parameter"
            for link in equipment[equipment_id].get("links", [])
        ), (equipment_id, entry_id)

relative = "electric/vl80s/technical-data/normal-values.pack.json"
assert relative in index["packs"]

print("VL80S_NORMAL_VALUES_PASS", len(entries), len(equipment))
