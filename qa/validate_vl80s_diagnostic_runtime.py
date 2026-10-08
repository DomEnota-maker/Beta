#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / "content-packs/electric/vl80s"
DIAG = MODEL / "diagnostics"
FLOW = DIAG / "runtime/executable-flow.json"
INDEX = DIAG / "index.json"

flow = json.loads(FLOW.read_text(encoding="utf-8"))
index = json.loads(INDEX.read_text(encoding="utf-8"))

assert flow["schemaVersion"] == 1
assert flow["modelId"] == "vl80s"
assert flow["sourceSnapshot"]["repository"] == "DomEnota-maker/Test-"
assert flow["sourceSnapshot"]["commit"] == "3176d6ee228f0ed4b78371f45209e10c1c724eff"
assert flow["sourceSnapshot"]["runtimeSource"] == "DiagnosticRepository.scenarios"
assert set(flow["runtimeSemantics"]["responses"]) == {"YES", "NO", "UNKNOWN"}
assert flow["runtimeSemantics"]["endOfFlow"] == "__end__"

scenarios = flow["scenarios"]
assert flow["scenarioCount"] == len(scenarios)
assert len(scenarios) >= 92
assert len({item["id"] for item in scenarios}) == len(scenarios)
assert len({item["legacyId"] for item in scenarios}) == len(scenarios)

catalog = {}
pack_paths = list(index["recommendedPacks"])
for paths in index["extendedCorpora"].values():
    pack_paths.extend(paths)

for rel in pack_paths:
    document = json.loads((ROOT / "content-packs" / rel).read_text(encoding="utf-8"))
    for entry in document["entries"]:
        assert entry["id"] not in catalog, entry["id"]
        catalog[entry["id"]] = entry

runtime_ids = {item["id"] for item in scenarios}
assert runtime_ids <= set(catalog), sorted(runtime_ids - set(catalog))

# Executable runtime availability never publishes catalog cards by itself.
for scenario_id in runtime_ids:
    assert catalog[scenario_id]["publicationStatus"] == "CANDIDATE", scenario_id

questions_total = 0
edges_total = 0
for scenario in scenarios:
    questions = scenario["questions"]
    keys = [q["key"] for q in questions]
    assert len(keys) == len(set(keys)), scenario["id"]
    key_set = set(keys)

    start = scenario["startQuestionKey"]
    if questions:
        assert start in key_set, scenario["id"]
    else:
        assert start is None, scenario["id"]

    cause_ids = {cause["id"] for cause in scenario["diagnosticCauses"]}

    for question in questions:
        questions_total += 1
        responses = question["responses"]
        assert set(responses) == {"YES", "NO", "UNKNOWN"}, (
            scenario["id"],
            question["key"],
        )
        for response_name, branch in responses.items():
            edges_total += 1
            target = branch["nextQuestionKey"]
            assert target == "__end__" or target in key_set, (
                scenario["id"],
                question["key"],
                response_name,
                target,
            )
            for cause_id in branch["candidateCauseIds"]:
                assert cause_id in cause_ids, (
                    scenario["id"],
                    question["key"],
                    response_name,
                    cause_id,
                )

assert flow["questionCount"] == questions_total
assert flow["responseEdgeCount"] == edges_total
assert edges_total == questions_total * 3

assert index["runtimePayloadStatus"] == "EXECUTABLE_FLOW_AVAILABLE"
assert index["executableFlow"] == "electric/vl80s/diagnostics/runtime/executable-flow.json"
assert index["publicationPolicy"] == "ALL_CANDIDATE_UNTIL_FEATURE_RUNTIME_ACCEPTANCE"

golden = index["goldenReferenceCandidate"]
assert golden == "vl80s.diag.pantograph-no-rise"
assert catalog[golden]["publicationStatus"] == "CANDIDATE"

print(
    "VL80S_EXECUTABLE_DIAGNOSTIC_RUNTIME_PASS",
    len(scenarios),
    questions_total,
    edges_total,
)
