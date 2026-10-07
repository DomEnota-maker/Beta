#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
INVENTORY = ROOT / "content-packs/electric/vl80s/inventory/pantograph-no-rise.candidate.json"
SCHEMA = ROOT / "content-schema/content-pack-manifest.schema.json"

data = json.loads(INVENTORY.read_text(encoding="utf-8"))
schema = json.loads(SCHEMA.read_text(encoding="utf-8"))

assert data["schemaVersion"] == 1
assert data["migrationStatus"] == "CANDIDATE_NOT_RUNTIME"
assert data["acceptanceGate"] == {
    "required": "GOLDEN_REFERENCE_PASS",
    "status": "NOT_PASSED_AT_INVENTORY_TIME",
}

source = data["source"]
assert source["repository"] == "DomEnota-maker/Test-"
assert source["branch"] == "feature/diagnostic-framework-v2"
assert source["sha"] == "3176d6ee228f0ed4b78371f45209e10c1c724eff"

scenario = data["scenario"]
assert scenario["legacyScenarioId"] == "pantograph-no-rise"
assert scenario["startNodeId"] == "pnr-danger"
assert len(scenario["nodeIds"]) == 8
assert len(set(scenario["nodeIds"])) == 8
assert scenario["responseEdgeCount"] == 24
assert scenario["terminalEdgeCount"] + scenario["continuingEdgeCount"] == 24
assert set(scenario["systemIds"]) == {"VL-SYS-HV", "VL-SYS-PN"}
assert len(set(scenario["componentIds"])) == 5

reference = data["referencePack"]
assert reference["productionStatus"] == "REFERENCE_ONLY_PENDING_HUMAN_REVIEW"
assert reference["sourceCount"] == 12
assert reference["entryCount"] == 19
assert sum(reference["classifications"].values()) == 19
assert sum(reference["runtimeRoles"].values()) == 19
assert set(reference["restrictedEntryIds"]) == {
    "vl80s-panto-field-001",
    "vl80s-panto-unsafe-001",
}
assert {item["id"] for item in reference["conflictGroups"]} == {
    "PANTO-PRESSURE-POINT-001",
    "PANTO-RELAY248-VARIANT-001",
    "PANTO-FIELD-SAFETY-001",
}

required_manifest_fields = set(schema["required"])
assert required_manifest_fields == {
    "schemaVersion",
    "packId",
    "packVersion",
    "family",
    "modelIds",
    "variantIds",
    "locale",
    "entries",
    "requiresRuntime",
    "sourceCatalogVersion",
    "checksums",
}

serialized = INVENTORY.read_text(encoding="utf-8")
assert '"runtimeEnabled"' not in serialized
assert '"GOLDEN_REFERENCE_PASS"' in serialized
print("VL80S_CANDIDATE_INVENTORY_PASS")
