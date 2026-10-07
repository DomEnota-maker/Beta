#!/usr/bin/env python3
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL_ROOT = ROOT / "content-packs/electric/vl80s"

atlas_files = sorted((MODEL_ROOT / "atlas/equipment").glob("*.pack.json"))
technical_files = sorted((MODEL_ROOT / "technical-data/equipment").glob("*.pack.json"))

assert len(atlas_files) == 11, len(atlas_files)
assert len(technical_files) == 11, len(technical_files)
assert not (MODEL_ROOT / "atlas/pantograph.vertical.pack.json").exists()
assert not (MODEL_ROOT / "technical-data/pantograph.vertical.pack.json").exists()

entries = {}
for path in atlas_files + technical_files:
    document = json.loads(path.read_text(encoding="utf-8"))
    manifest = document["manifest"]
    ids = [entry["id"] for entry in document["entries"]]
    assert set(ids) == set(manifest["entries"]), path
    assert manifest["modelIds"] == ["vl80s"]
    assert manifest["variantIds"] == []
    for entry in document["entries"]:
        assert entry["id"] not in entries, entry["id"]
        entries[entry["id"]] = entry

equipment = {key: value for key, value in entries.items() if value["type"] == "EQUIPMENT"}
technical = {key: value for key, value in entries.items() if value["type"] == "TECHNICAL_DATA"}

assert len(equipment) == 89
assert len(technical) == 89
assert all(entry["migrationRefs"]["evidenceStatus"] == "BASE_CONFIRMED" for entry in equipment.values())

for equipment_id, entry in equipment.items():
    tech_id = "VL-TD-EQ-" + equipment_id.removeprefix("VL-EQ-")
    assert tech_id in technical
    assert any(
        link["type"] == "TECHNICAL_DATA" and link["targetId"] == tech_id
        for link in entry["links"]
    )

for tech_id, entry in technical.items():
    equipment_id = entry["migrationRefs"]["equipmentId"]
    assert equipment_id in equipment
    assert any(
        link["type"] == "EQUIPMENT" and link["targetId"] == equipment_id
        for link in entry["links"]
    )

assert "vl80s-panto-tech-001" in technical["VL-TD-EQ-HV-002"]["aliases"]
assert "vl80s-panto-tech-002" in technical["VL-TD-EQ-PN-002"]["aliases"]

# The first proven cross-feature slice survives the replacement of temporary packs.
assert any(
    link["type"] == "ACCEPTANCE_ITEM" and link["targetId"] == "VL80-REQ-03"
    for link in equipment["VL-EQ-HV-002"]["links"]
)
assert any(
    link["type"] == "RELATED_SCENARIO" and link["targetId"] == "vl80s.diag.pantograph-no-rise"
    for link in equipment["VL-EQ-HV-002"]["links"]
)
assert any(
    link["type"] == "RELATED_SCENARIO" and link["targetId"] == "vl80s.diag.pantograph-no-rise"
    for link in equipment["VL-EQ-PN-002"]["links"]
)

# Raw implementation IDs are allowed in migrationRefs/sourceRefs, never in rendered text.
raw_pattern = re.compile(
    r"(VL-(?:EQ|SRC|SYS|SCH)-|vl80s_[a-z0-9_]+|actualEquipmentOverrides|Feature rule)"
)
for entry in entries.values():
    visible = [entry.get("title", ""), entry.get("summary") or ""]
    visible += entry.get("details", [])
    for block in entry.get("blocks", []):
        visible.append(block.get("title", ""))
        visible.extend(block.get("lines", []))
    rendered = " ".join(visible)
    match = raw_pattern.search(rendered)
    assert match is None, (entry["id"], match.group(0) if match else None)

index = json.loads((MODEL_ROOT / "runtime-index.json").read_text(encoding="utf-8"))
assert index["modelId"] == "vl80s"
assert len(index["packs"]) == len(set(index["packs"]))
assert len(index["packs"]) >= 24, len(index["packs"])
required_equipment_packs = {
    str(path.relative_to(ROOT / "content-packs")).replace("\\", "/")
    for path in atlas_files + technical_files
}
assert required_equipment_packs.issubset(set(index["packs"]))
for relative in index["packs"]:
    assert (ROOT / "content-packs" / relative).is_file(), relative

# Validate indexed cross-links by ID/type across all runtime packs.
all_runtime_entries = {}
for relative in index["packs"]:
    document = json.loads((ROOT / "content-packs" / relative).read_text(encoding="utf-8"))
    for entry in document.get("entries", []):
        assert entry["id"] not in all_runtime_entries, entry["id"]
        all_runtime_entries[entry["id"]] = entry

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
aliases = {}
for entry in all_runtime_entries.values():
    for alias in entry.get("aliases", []):
        assert alias not in aliases
        aliases[alias] = entry["id"]

for entry in all_runtime_entries.values():
    for link in entry.get("links", []):
        target_id = link["targetId"]
        canonical = target_id if target_id in all_runtime_entries else aliases.get(target_id)
        assert canonical is not None, (entry["id"], target_id)
        assert all_runtime_entries[canonical]["type"] == expected_type[link["type"]], (
            entry["id"],
            target_id,
            link["type"],
            all_runtime_entries[canonical]["type"],
        )

print("VL80S_EQUIPMENT_MIGRATION_PASS", len(equipment), len(technical), len(index["packs"]))
