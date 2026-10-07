#!/usr/bin/env python3
import json
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DONOR = ROOT / "build/vl80s-donor/vl80s_diagnostics.json"
OUT = ROOT / "content-packs/electric/vl80s/migration/diagnostics-inventory.json"

SOURCE_COMMIT = "3176d6ee228f0ed4b78371f45209e10c1c724eff"
SOURCE_BLOB = "6ffdfdabf2f467dcdc4958768905f2b675ac0c26"

root = json.loads(DONOR.read_text(encoding="utf-8"))
scenarios = root.get("scenarios", [])
if len(scenarios) != 103:
    raise SystemExit(f"Expected 103 scenarios, got {len(scenarios)}")

top_level_keys = Counter()
scalar_values = defaultdict(Counter)
array_lengths = defaultdict(list)
object_keys = defaultdict(Counter)
records = []

for scenario in scenarios:
    for key, value in scenario.items():
        top_level_keys[key] += 1
        if isinstance(value, (str, int, float, bool)) or value is None:
            if isinstance(value, str) and len(value) <= 120:
                scalar_values[key][value] += 1
        elif isinstance(value, list):
            array_lengths[key].append(len(value))
        elif isinstance(value, dict):
            for nested_key in value:
                object_keys[key][nested_key] += 1

    descriptor = {
        "id": scenario.get("id"),
        "title": scenario.get("title"),
        "category": scenario.get("category"),
        "severity": scenario.get("severity"),
        "profileId": scenario.get("profileId"),
        "applicableVariantIds": scenario.get("applicableVariantIds") or [],
        "informationConfidence": scenario.get("informationConfidence"),
        "keys": sorted(scenario.keys()),
        "counts": {
            key: len(value)
            for key, value in scenario.items()
            if isinstance(value, list)
        },
        "objectKeys": {
            key: sorted(value.keys())
            for key, value in scenario.items()
            if isinstance(value, dict)
        },
    }
    records.append(descriptor)

def summarize_lengths(values):
    if not values:
        return None
    return {
        "min": min(values),
        "max": max(values),
        "total": sum(values),
        "nonZero": sum(1 for value in values if value > 0),
    }

report = {
    "schemaVersion": 1,
    "sourceSnapshot": {
        "repository": "DomEnota-maker/Test-",
        "commit": SOURCE_COMMIT,
        "asset": "app/src/main/assets/technical/vl80s_diagnostics.json.gz",
        "blobSha": SOURCE_BLOB,
    },
    "scenarioCount": len(scenarios),
    "rootKeys": sorted(root.keys()),
    "rootObjectKeys": {
        key: sorted(value.keys())
        for key, value in root.items()
        if isinstance(value, dict)
    },
    "rootArrayCounts": {
        key: len(value)
        for key, value in root.items()
        if isinstance(value, list)
    },
    "scenarioFieldPresence": dict(sorted(top_level_keys.items())),
    "scalarDistributions": {
        key: dict(sorted(values.items(), key=lambda item: (-item[1], item[0])))
        for key, values in sorted(scalar_values.items())
        if len(values) <= 80
    },
    "arrayLengthStats": {
        key: summarize_lengths(values)
        for key, values in sorted(array_lengths.items())
    },
    "objectFieldPresence": {
        key: dict(sorted(values.items()))
        for key, values in sorted(object_keys.items())
    },
    "scenarios": records,
}

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(
    json.dumps(report, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)

print(
    "VL80S_DIAGNOSTICS_INVENTORY_PASS",
    len(scenarios),
    len(top_level_keys),
)
