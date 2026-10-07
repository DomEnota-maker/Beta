#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL_ROOT = ROOT / "content-packs/electric/vl80s"
PROFILE_PATH = MODEL_ROOT / "profile-catalog.json"
RUNTIME_INDEX_PATH = MODEL_ROOT / "runtime-index.json"
SCHEME_INDEX_PATH = MODEL_ROOT / "atlas/schemes/index.json"

profile = json.loads(PROFILE_PATH.read_text(encoding="utf-8"))
profile_ids = {item["id"] for item in profile["physicalBuckets"]}

runtime_index = json.loads(RUNTIME_INDEX_PATH.read_text(encoding="utf-8"))
assert runtime_index["featureIndexes"]["atlas"] == (
    "electric/vl80s/atlas/schemes/index.json"
)

scheme_index = json.loads(SCHEME_INDEX_PATH.read_text(encoding="utf-8"))
assert scheme_index["modelId"] == "vl80s"
assert len(scheme_index["schemePacks"]) == 2
assert len(scheme_index["variantPolicies"]) == 2

runtime_entries = {}
for relative in runtime_index["packs"]:
    path = ROOT / "content-packs" / relative
    document = json.loads(path.read_text(encoding="utf-8"))
    for entry in document.get("entries", []):
        assert entry["id"] not in runtime_entries, entry["id"]
        runtime_entries[entry["id"]] = entry

equipment = {
    key: value
    for key, value in runtime_entries.items()
    if value["type"] == "EQUIPMENT"
}
assert len(equipment) == 89

all_schemes = {}
for relative in scheme_index["schemePacks"]:
    path = ROOT / "content-packs" / relative
    assert path.is_file(), relative
    document = json.loads(path.read_text(encoding="utf-8"))

    manifest_ids = set(document["manifest"]["entries"])
    entry_ids = {entry["id"] for entry in document["entries"]}
    graph_ids = {scheme["id"] for scheme in document["schemes"]}
    assert manifest_ids == entry_ids == graph_ids

    for entry in document["entries"]:
        assert entry["type"] == "ATLAS_SCHEME"
        assert entry["owner"] == {"kind": "MODEL", "modelId": "vl80s"}
        for variant_id in entry.get("applicability", {}).get("variantIds", []):
            assert variant_id in profile_ids
        all_schemes[entry["id"]] = entry

    for scheme in document["schemes"]:
        node_ids = set()
        for node in scheme["nodes"]:
            current = (
                node.get("equipmentId")
                or node.get("virtualNodeId")
                or node.get("id")
            )
            assert current
            assert current not in node_ids, (scheme["id"], current)
            node_ids.add(current)
            equipment_id = node.get("equipmentId")
            if equipment_id:
                assert equipment_id in equipment, (scheme["id"], equipment_id)

        for edge in scheme["edges"]:
            assert edge["from"] in node_ids, (scheme["id"], edge["from"])
            assert edge["to"] in node_ids, (scheme["id"], edge["to"])

assert len(all_schemes) == 16
electrical_ids = {
    entry["id"]
    for entry in all_schemes.values()
    if entry["id"].startswith("VL-SCH-EL-")
}
# BR electrical scheme IDs are still electrical donor schemes despite BR prefix.
electrical_ids |= {
    "VL-SCH-BR-RHEO-INTERLOCK"
} & set(all_schemes)
# We know the donor has exactly 8 electrical and 8 pneumatic views.
electrical_pack = json.loads(
    (MODEL_ROOT / "atlas/schemes/electrical.pack.json").read_text(encoding="utf-8")
)
pneumatic_pack = json.loads(
    (MODEL_ROOT / "atlas/schemes/pneumatic.pack.json").read_text(encoding="utf-8")
)
assert len(electrical_pack["entries"]) == 8
assert len(pneumatic_pack["entries"]) == 8

fire = all_schemes["VL-SCH-EL-FIRE-SIGNAL"]
assert fire["publicationStatus"] == "CANDIDATE"
assert all(
    entry["publicationStatus"] == "ACTIVE"
    for entry in electrical_pack["entries"]
    if entry["id"] != "VL-SCH-EL-FIRE-SIGNAL"
)
assert all(
    entry["publicationStatus"] == "ACTIVE"
    for entry in pneumatic_pack["entries"]
)
assert all(
    not entry.get("applicability", {}).get("variantIds")
    for entry in pneumatic_pack["entries"]
)

# Bidirectional model-local links: scheme -> equipment and equipment -> scheme.
for scheme_id, scheme in all_schemes.items():
    linked_equipment = {
        link["targetId"]
        for link in scheme["links"]
        if link["type"] == "EQUIPMENT"
    }
    assert linked_equipment
    for equipment_id in linked_equipment:
        assert any(
            link["type"] == "ATLAS_SCHEME"
            and link["targetId"] == scheme_id
            for link in equipment[equipment_id]["links"]
        ), (equipment_id, scheme_id)

policies = []
for relative in scheme_index["variantPolicies"]:
    path = ROOT / "content-packs" / relative
    document = json.loads(path.read_text(encoding="utf-8"))
    assert document["runtimeStatus"] == "PENDING_OVERLAY_ENGINE"
    policies.append(document)

electrical_policy = next(
    item for item in policies
    if item["sourceSnapshot"]["asset"] == "vl80s_electrical_variants.json.gz"
)
pneumatic_policy = next(
    item for item in policies
    if item["sourceSnapshot"]["asset"] == "vl80s_pneumatic_variants.json.gz"
)
assert len(electrical_policy["policy"]["variantOverlays"]) == 8
assert len(electrical_policy["policy"]["candidateVariantFragments"]) == 2
assert len(pneumatic_policy["policy"]["stateApplicability"]) == 13

print("VL80S_SCHEME_MIGRATION_PASS", len(all_schemes), len(equipment))
