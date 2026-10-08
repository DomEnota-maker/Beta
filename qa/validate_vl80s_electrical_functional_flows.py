#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / "content-packs/electric/vl80s"
FLOW = MODEL / "atlas/interactive/electrical-functional-flows.json"
SCHEME_INDEX = MODEL / "atlas/schemes/index.json"

document = json.loads(FLOW.read_text(encoding="utf-8"))

assert document["schemaVersion"] == 1
assert document["modelId"] == "vl80s"
assert document["id"] == "vl80s.electrical.functional-flows"
assert document["sourceSnapshot"] == {
    "repository": "DomEnota-maker/Test-",
    "commit": "3176d6ee228f0ed4b78371f45209e10c1c724eff",
    "source": "patch/app/src/main/java/ru/railbrake/calculator/ui/InteractiveSchematics.kt",
    "blobSha": "2dd927c6ede60d63482a88f0df811f837a15d718",
}

semantics = document["semantics"]
assert semantics["purpose"] == "TRAINING_FUNCTIONAL_FLOW"
assert semantics["actionAuthority"] == "NONE"
assert semantics["coordinateClaim"] == "LEGACY_FUNCTIONAL_LAYOUT_NOT_MOUNTING_OR_WIRING"
assert semantics["styleAuthority"] == "SHARED_DESIGN_SYSTEM"
assert semantics["canonicalLinkRule"] == "EXACT_LEGACY_ID_MATCH_ONLY"

expected_steps = {
    "Тяга": 5,
    "Подъём ТП": 6,
    "Вспомогательные": 4,
    "Реостатный тормоз": 5,
    "Защита": 4,
}
assert document["scenarioCount"] == 5
assert document["stepCount"] == 24
scenarios = document["scenarios"]
assert [scenario["title"] for scenario in scenarios] == list(expected_steps)

equipment_ids = set()
for path in sorted((MODEL / "atlas/equipment").glob("*.pack.json")):
    pack = json.loads(path.read_text(encoding="utf-8"))
    equipment_ids.update(item["id"] for item in pack["entries"])

canonical_ids = set()
node_instance_count = 0
edge_count = 0
for scenario in scenarios:
    assert len(scenario["steps"]) == expected_steps[scenario["title"]]
    assert scenario["summary"].strip()
    node_ids = {node["id"] for node in scenario["nodes"]}
    assert len(node_ids) == len(scenario["nodes"])
    node_instance_count += len(scenario["nodes"])

    width = scenario["canvas"]["width"]
    height = scenario["canvas"]["height"]
    for node in scenario["nodes"]:
        assert node["info"]["title"].strip()
        assert node["info"]["purpose"].strip()
        assert 0 <= node["x"]
        assert 0 <= node["y"]
        assert node["x"] + node["width"] <= width
        assert node["y"] + node["height"] <= height
        if node.get("equipmentId"):
            assert node["equipmentId"] in equipment_ids
            canonical_ids.add(node["equipmentId"])

    for edge in scenario["edges"]:
        edge_count += 1
        assert edge["from"] in node_ids
        assert edge["to"] in node_ids
        assert 0 <= edge["activationStep"] < len(scenario["steps"])

assert node_instance_count == 50
assert edge_count == 48
assert document["uniqueCanonicalEquipmentLinkCount"] == 9
assert canonical_ids == {
    "VL-EQ-CT-001",
    "VL-EQ-HV-001",
    "VL-EQ-HV-002",
    "VL-EQ-PN-002",
    "VL-EQ-PR-001",
    "VL-EQ-TR-001",
    "VL-EQ-TR-003",
    "VL-EQ-TR-004",
    "VL-EQ-TR-005",
}

index = json.loads(SCHEME_INDEX.read_text(encoding="utf-8"))
assert index["functionalFlows"] == [
    "electric/vl80s/atlas/interactive/electrical-functional-flows.json"
]

print("VL80S_ELECTRICAL_FUNCTIONAL_FLOWS_PASS", len(scenarios), document["stepCount"])
