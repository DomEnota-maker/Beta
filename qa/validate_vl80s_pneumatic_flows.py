#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / "content-packs/electric/vl80s"
FLOW = MODEL / "atlas/interactive/pneumatic-flows.json"
SCHEME_INDEX = MODEL / "atlas/schemes/index.json"

document = json.loads(FLOW.read_text(encoding="utf-8"))

assert document["schemaVersion"] == 1
assert document["modelId"] == "vl80s"
assert document["id"] == "vl80s.pneumatic.stepwise"
assert document["modeCount"] == 4
assert document["sourceSnapshots"] == [
    {
        "repository": "DomEnota-maker/Test-",
        "commit": "3176d6ee228f0ed4b78371f45209e10c1c724eff",
        "source": "app/src/main/java/ru/railbrake/calculator/core/KnowledgeRepository.kt",
        "blobSha": "b37ead2d66c0c976bcc850f9cde4fddc0e198fb3",
        "role": "step-text",
    },
    {
        "repository": "DomEnota-maker/Test-",
        "commit": "3176d6ee228f0ed4b78371f45209e10c1c724eff",
        "source": "app/src/main/java/ru/railbrake/calculator/ui/KnowledgeBaseScreen.kt",
        "blobSha": "0f18989087a7f4f2efcbdd46bdf05b1065d8f54c",
        "role": "route-coordinates",
    },
]

semantics = document["semantics"]
assert semantics["purpose"] == "TRAINING_FUNCTIONAL_FLOW"
assert semantics["actionAuthority"] == "NONE"
assert semantics["coordinateSpace"] == {"width": 1181, "height": 573}
assert semantics["coordinateClaim"] == "LEGACY_PRESENTATION_OVERLAY_NOT_EXACT_PIPE_GEOMETRY"

expected_steps = {
    "CHARGING": 4,
    "SERVICE_BRAKE": 7,
    "RELEASE": 4,
    "AUXILIARY_BRAKE": 4,
}
modes = {mode["id"]: mode for mode in document["modes"]}
assert set(modes) == set(expected_steps)

all_kinds = set()
segment_count = 0
for mode_id, expected_count in expected_steps.items():
    mode = modes[mode_id]
    assert len(mode["steps"]) == expected_count, mode_id
    assert mode["summary"].strip()
    assert mode["start"].strip()
    for step in mode["steps"]:
        assert step["title"].strip()
        assert step["description"].strip()
        assert step["segments"]
        for segment in step["segments"]:
            segment_count += 1
            all_kinds.add(segment["kind"])
            assert segment["kind"] in {"FLOW", "RELEASE", "CONTROL"}
            assert len(segment["points"]) >= 2
            for point in segment["points"]:
                assert 0 <= point["x"] <= 1181
                assert 0 <= point["y"] <= 573

service_kinds = {
    segment["kind"]
    for step in modes["SERVICE_BRAKE"]["steps"]
    for segment in step["segments"]
}
assert service_kinds == {"FLOW", "RELEASE", "CONTROL"}
assert all_kinds == {"FLOW", "RELEASE", "CONTROL"}
assert segment_count == 44

index = json.loads(SCHEME_INDEX.read_text(encoding="utf-8"))
assert index["stepwiseFlows"] == [
    "electric/vl80s/atlas/interactive/pneumatic-flows.json"
]

print("VL80S_PNEUMATIC_FLOWS_PASS", sum(expected_steps.values()), segment_count)
