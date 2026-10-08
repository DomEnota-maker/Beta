#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / "content-packs/electric/vl80s"
DONOR = ROOT / "build/vl80s-observation-donor/observations.json"
SOURCE_REPORT = ROOT / "build/vl80s-observation-donor/source-report.json"
EQUIPMENT_ROOT = MODEL / "atlas/equipment"
DIAG_ROOT = MODEL / "diagnostics"
RELATION_GRAPH = DIAG_ROOT / "relation-graph.json"
INDEX = DIAG_ROOT / "index.json"
OUTPUT_REL = "electric/vl80s/diagnostics/observations/index.json"
OUTPUT = ROOT / "content-packs" / OUTPUT_REL
REPORT = MODEL / "migration/observations-report.json"

SOURCE_COMMIT = "3176d6ee228f0ed4b78371f45209e10c1c724eff"

donor = json.loads(DONOR.read_text(encoding="utf-8"))
source_report = json.loads(SOURCE_REPORT.read_text(encoding="utf-8"))
relation_graph = json.loads(RELATION_GRAPH.read_text(encoding="utf-8"))

assert donor["sourceSnapshot"]["commit"] == SOURCE_COMMIT
assert source_report["commit"] == SOURCE_COMMIT
scenario_map = relation_graph["scenarioIdMap"]

legacy_equipment_map = {}
canonical_equipment_ids = set()

for path in sorted(EQUIPMENT_ROOT.glob("*.pack.json")):
    document = json.loads(path.read_text(encoding="utf-8"))
    for entry in document.get("entries", []):
        equipment_id = entry["id"]
        canonical_equipment_ids.add(equipment_id)
        legacy_id = (entry.get("migrationRefs") or {}).get("legacyId")
        if not legacy_id:
            continue
        if legacy_id in legacy_equipment_map:
            raise SystemExit(
                f"Duplicate legacy equipment mapping: {legacy_id} -> "
                f"{legacy_equipment_map[legacy_id]}, {equipment_id}"
            )
        legacy_equipment_map[legacy_id] = equipment_id

observations = []
unresolved_equipment = {}
unresolved_scenarios = {}

for raw in donor["observations"]:
    legacy_id = raw["legacyId"]
    missing_equipment = sorted(
        set(raw["legacyEquipmentIds"]) - set(legacy_equipment_map)
    )
    missing_scenarios = sorted(
        set(raw["legacyScenarioIds"]) - set(scenario_map)
    )
    if missing_equipment:
        unresolved_equipment[legacy_id] = missing_equipment
    if missing_scenarios:
        unresolved_scenarios[legacy_id] = missing_scenarios

    if missing_equipment or missing_scenarios:
        continue

    equipment_ids = [
        legacy_equipment_map[value]
        for value in raw["legacyEquipmentIds"]
    ]
    scenario_ids = [
        scenario_map[value]
        for value in raw["legacyScenarioIds"]
    ]

    observations.append(
        {
            "id": f"vl80s.obs.{legacy_id}",
            "legacyId": legacy_id,
            "title": raw["title"],
            "kind": raw["kind"],
            "description": raw["description"],
            "synonyms": raw["synonyms"],
            "scenarioIds": scenario_ids,
            "equipmentIds": equipment_ids,
            "confidence": raw["confidence"],
            "variantNote": raw["variantNote"],
        }
    )

if unresolved_equipment or unresolved_scenarios:
    raise SystemExit(
        "Observation canonical mapping is incomplete: "
        + json.dumps(
            {
                "equipment": unresolved_equipment,
                "scenarios": unresolved_scenarios,
            },
            ensure_ascii=False,
            sort_keys=True,
        )
    )

if len(observations) != donor["observationCount"]:
    raise SystemExit(
        f"Expected {donor['observationCount']} observations, "
        f"mapped {len(observations)}"
    )

observations.sort(key=lambda item: item["id"])
observation_source = source_report["files"]["Vl80sObservationCatalog.kt"]

document = {
    "schemaVersion": 1,
    "modelId": "vl80s",
    "sourceSnapshot": {
        "repository": "DomEnota-maker/Test-",
        "commit": SOURCE_COMMIT,
        "source": donor["sourceSnapshot"]["source"],
        "blobSha": observation_source["verifiedBlobSha"],
    },
    "semantics": {
        "search": "case-insensitive substring over title + description + synonyms",
        "searchOnly": True,
        "actionAuthority": "NONE",
        "equipmentOwnership": "canonical-equipment-only",
        "scenarioAccess": "resolved-through-feature-diagnostics-access-policy",
    },
    "observationCount": len(observations),
    "observations": observations,
}

OUTPUT.parent.mkdir(parents=True, exist_ok=True)
OUTPUT.write_text(
    json.dumps(document, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)

index = json.loads(INDEX.read_text(encoding="utf-8"))
assert index["modelId"] == "vl80s"
index["observationIndex"] = OUTPUT_REL
INDEX.write_text(
    json.dumps(index, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)

REPORT.parent.mkdir(parents=True, exist_ok=True)
REPORT.write_text(
    json.dumps(
        {
            "sourceCommit": SOURCE_COMMIT,
            "observationCount": len(observations),
            "legacyEquipmentMappingCount": len(legacy_equipment_map),
            "canonicalEquipmentCount": len(canonical_equipment_ids),
            "scenarioMappingCount": len(scenario_map),
            "unresolvedEquipment": unresolved_equipment,
            "unresolvedScenarios": unresolved_scenarios,
            "output": str(OUTPUT.relative_to(ROOT)).replace("\\", "/"),
        },
        ensure_ascii=False,
        indent=2,
    ) + "\n",
    encoding="utf-8",
)

print(
    "VL80S_OBSERVATION_MIGRATION_PASS",
    len(observations),
    len(legacy_equipment_map),
    len(scenario_map),
)
