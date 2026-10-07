#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL_ROOT = ROOT / "content-packs/electric/vl80s"
INDEX_PATH = MODEL_ROOT / "runtime-index.json"

index = json.loads(INDEX_PATH.read_text(encoding="utf-8"))
assert index["schemaVersion"] == 1
assert index["modelId"] == "vl80s"
assert len(index["packs"]) == len(set(index["packs"]))

entries = {}
aliases = {}

for relative in index["packs"]:
    path = ROOT / "content-packs" / relative
    assert path.is_file(), f"missing indexed pack: {relative}"
    document = json.loads(path.read_text(encoding="utf-8"))
    manifest = document["manifest"]

    assert manifest["family"] == "ELECTRIC", manifest["packId"]
    assert manifest["modelIds"] == ["vl80s"], manifest["packId"]

    ids = [entry["id"] for entry in document.get("entries", [])]
    assert set(ids) == set(manifest["entries"]), (
        f"manifest mismatch: {manifest['packId']}"
    )

    for entry in document.get("entries", []):
        entry_id = entry["id"]
        assert entry_id not in entries, f"duplicate canonical id: {entry_id}"
        assert entry["owner"] == {"kind": "MODEL", "modelId": "vl80s"}
        assert entry["applicability"]["modelIds"] == ["vl80s"]
        entries[entry_id] = entry

        for alias in entry.get("aliases", []):
            assert alias not in aliases, f"duplicate alias: {alias}"
            aliases[alias] = entry_id

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
        target_id = link["targetId"]
        canonical = target_id if target_id in entries else aliases.get(target_id)
        assert canonical is not None, (
            f"missing vertical-slice target: {entry['id']} -> {target_id}"
        )
        assert entries[canonical]["type"] == expected_type[link["type"]], (
            f"type mismatch: {entry['id']} -> {target_id}"
        )
        assert link.get("scope", "SAME_MODEL") == "SAME_MODEL"

# Preserve the first proven end-to-end slice after replacing its temporary packs.
pantograph = entries["VL-EQ-HV-002"]
valve245 = entries["VL-EQ-PN-002"]
acceptance = entries["VL80-REQ-03"]
candidate = entries["vl80s.diag.pantograph-no-rise"]

assert pantograph["title"] == "Токоприёмник"
assert valve245["title"] == "Клапан 245"
assert acceptance["title"] == "Крышевое оборудование и токоприёмник"
assert acceptance["summary"] == (
    "Осмотреть с земли крышевое оборудование и проверить токоприёмник "
    "в установленном безопасном порядке."
)
assert acceptance["actionDisposition"] == "INFORMATION_ONLY"

assert any(
    link["type"] == "ACCEPTANCE_ITEM" and link["targetId"] == "VL80-REQ-03"
    for link in pantograph["links"]
)
assert any(
    link["type"] == "EQUIPMENT" and link["targetId"] == "VL-EQ-HV-002"
    for link in acceptance["links"]
)

pantograph_tech = entries["VL-TD-EQ-HV-002"]
valve_tech = entries["VL-TD-EQ-PN-002"]
assert "vl80s-panto-tech-001" in pantograph_tech.get("aliases", [])
assert "vl80s-panto-tech-002" in valve_tech.get("aliases", [])
assert {
    "VL80S-PANTO-S02",
    "VL80S-PANTO-S03",
    "VL80S-PANTO-S06",
}.issubset(set(valve_tech.get("sourceRefs", [])))

assert any(
    link["type"] == "RELATED_SCENARIO"
    and link["targetId"] == "vl80s.diag.pantograph-no-rise"
    for link in pantograph["links"]
)
assert any(
    link["type"] == "RELATED_SCENARIO"
    and link["targetId"] == "vl80s.diag.pantograph-no-rise"
    for link in valve245["links"]
)

assert candidate["publicationStatus"] == "CANDIDATE"
assert candidate["actionDisposition"] == "CONDITIONAL_ACTION"
assert "GOLDEN REFERENCE PASS" in " ".join(candidate["details"])

# Everything except the deliberately gated diagnostic candidate is active.
for entry_id, entry in entries.items():
    if entry_id == "vl80s.diag.pantograph-no-rise":
        continue
    assert entry["publicationStatus"] == "ACTIVE", entry_id

print(
    "VL80S_VERTICAL_SLICE_PACK_PASS",
    len(entries),
    len(index["packs"]),
)
