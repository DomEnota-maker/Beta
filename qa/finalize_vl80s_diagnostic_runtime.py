#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FLOW_REL = "electric/vl80s/diagnostics/runtime/executable-flow.json"
FLOW = ROOT / "content-packs" / FLOW_REL
INDEX = ROOT / "content-packs/electric/vl80s/diagnostics/index.json"

flow = json.loads(FLOW.read_text(encoding="utf-8"))
assert flow["schemaVersion"] == 1
assert flow["modelId"] == "vl80s"
assert flow["sourceSnapshot"]["commit"] == "3176d6ee228f0ed4b78371f45209e10c1c724eff"

index = json.loads(INDEX.read_text(encoding="utf-8"))
assert index["modelId"] == "vl80s"
assert index["interactiveRuntimeSource"] == "DiagnosticRepository.scenarios"
assert index["publicationPolicy"] == "ALL_CANDIDATE_UNTIL_FEATURE_RUNTIME_ACCEPTANCE"

index["runtimePayloadStatus"] = "EXECUTABLE_FLOW_AVAILABLE"
index["executableFlow"] = FLOW_REL

INDEX.write_text(
    json.dumps(index, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)

print("VL80S_DIAGNOSTIC_RUNTIME_INDEX_UPDATED", flow["scenarioCount"])
