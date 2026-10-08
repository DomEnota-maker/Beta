#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / "content-packs/electric/vl80s"
DIAG = MODEL / "diagnostics"
INDEX = json.loads((DIAG / "index.json").read_text(encoding="utf-8"))
OBS_REL = INDEX.get("observationIndex")
assert OBS_REL == "electric/vl80s/diagnostics/observations/index.json"
OBS = json.loads((ROOT / "content-packs" / OBS_REL).read_text(encoding="utf-8"))

assert OBS["schemaVersion"] == 1
assert OBS["modelId"] == "vl80s"
assert OBS["sourceSnapshot"]["commit"] == "3176d6ee228f0ed4b78371f45209e10c1c724eff"
assert OBS["sourceSnapshot"]["blobSha"] == "a5d337adfda22161cc31779a93fdc04e18f5ad31"
assert OBS["semantics"]["searchOnly"] is True
assert OBS["semantics"]["actionAuthority"] == "NONE"
assert OBS["semantics"]["equipmentOwnership"] == "canonical-equipment-only"
assert OBS["semantics"]["scenarioAccess"] == "resolved-through-feature-diagnostics-access-policy"
assert OBS["observationCount"] == 18

observations = OBS["observations"]
assert OBS["observationCount"] == len(observations)
assert observations
assert len({item["id"] for item in observations}) == len(observations)
assert len({item["legacyId"] for item in observations}) == len(observations)

equipment_ids = set()
for path in sorted((MODEL / "atlas/equipment").glob("*.pack.json")):
    document = json.loads(path.read_text(encoding="utf-8"))
    equipment_ids.update(entry["id"] for entry in document["entries"])
assert len(equipment_ids) == 89

scenario_ids = set()
pack_paths = list(INDEX["recommendedPacks"])
for paths in INDEX["extendedCorpora"].values():
    pack_paths.extend(paths)
for relative in pack_paths:
    document = json.loads(
        (ROOT / "content-packs" / relative).read_text(encoding="utf-8")
    )
    scenario_ids.update(entry["id"] for entry in document["entries"])
assert len(scenario_ids) == 103

flow_rel = INDEX.get("executableFlow")
assert flow_rel == "electric/vl80s/diagnostics/runtime/executable-flow.json"
flow = json.loads(
    (ROOT / "content-packs" / flow_rel).read_text(encoding="utf-8")
)
assert flow["modelId"] == "vl80s"
assert flow["scenarioCount"] == 103
runtime_scenario_ids = {item["id"] for item in flow["scenarios"]}
assert runtime_scenario_ids == scenario_ids

for item in observations:
    assert item["id"].startswith("vl80s.obs.")
    assert item["title"].strip()
    assert item["description"].strip()
    assert item["scenarioIds"], item["id"]
    assert item["equipmentIds"], item["id"]
    assert len(item["scenarioIds"]) == len(set(item["scenarioIds"]))
    assert len(item["equipmentIds"]) == len(set(item["equipmentIds"]))
    assert set(item["scenarioIds"]) <= scenario_ids, item["id"]
    assert set(item["scenarioIds"]) <= runtime_scenario_ids, item["id"]
    assert set(item["equipmentIds"]) <= equipment_ids, item["id"]
    assert all(value.startswith("vl80s.diag.") for value in item["scenarioIds"])
    assert all(value.startswith("VL-EQ-") for value in item["equipmentIds"])

report = json.loads(
    (MODEL / "migration/observations-report.json").read_text(encoding="utf-8")
)
assert report["observationCount"] == len(observations)
assert report["unresolvedEquipment"] == {}
assert report["unresolvedScenarios"] == {}

print(
    "VL80S_OBSERVATION_INDEX_PASS",
    len(observations),
    len(equipment_ids),
    len(scenario_ids),
)
