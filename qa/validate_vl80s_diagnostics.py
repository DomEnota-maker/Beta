#!/usr/bin/env python3
# Validation revision 2: acceptance reverse links are now part of the closed graph.
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL_ROOT = ROOT / "content-packs/electric/vl80s"
DIAG_ROOT = MODEL_ROOT / "diagnostics"
INDEX = json.loads((DIAG_ROOT / "index.json").read_text(encoding="utf-8"))
GRAPH = json.loads((DIAG_ROOT / "graph.json").read_text(encoding="utf-8"))
REPORT = json.loads((MODEL_ROOT / "migration/diagnostics-report.json").read_text(encoding="utf-8"))
RUNTIME = json.loads((MODEL_ROOT / "runtime-index.json").read_text(encoding="utf-8"))

assert INDEX["modelId"] == "vl80s"
assert INDEX["sourceSnapshot"]["commit"] == "3176d6ee228f0ed4b78371f45209e10c1c724eff"
assert INDEX["sourceSnapshot"]["blobSha"] == "6ffdfdabf2f467dcdc4958768905f2b675ac0c26"
assert INDEX["publicationPolicy"] == "ALL_CANDIDATE_UNTIL_FEATURE_RUNTIME_ACCEPTANCE"
assert INDEX["goldenReferenceCandidate"] == "vl80s.diag.pantograph-no-rise"

recommended_paths = INDEX["recommendedPacks"]
extended_paths = INDEX["extendedCorpora"]["SUPPLEMENTAL_OPERATIONAL"]
assert recommended_paths
assert extended_paths
assert len(recommended_paths) == len(set(recommended_paths))
assert len(extended_paths) == len(set(extended_paths))

entries = {}
aliases = {}
recommended_ids = set()
extended_ids = set()

for relative in recommended_paths + extended_paths:
    path = ROOT / "content-packs" / relative
    assert path.is_file(), relative
    document = json.loads(path.read_text(encoding="utf-8"))
    manifest_ids = set(document["manifest"]["entries"])
    entry_ids = {entry["id"] for entry in document["entries"]}
    assert manifest_ids == entry_ids, relative

    is_extended_pack = relative in extended_paths
    for entry in document["entries"]:
        entry_id = entry["id"]
        assert entry_id not in entries, entry_id
        entries[entry_id] = entry
        assert entry_id.startswith("vl80s.diag.")
        assert entry["type"] == "DIAGNOSTIC_SCENARIO"
        assert entry["owner"] == {"kind": "MODEL", "modelId": "vl80s"}
        assert entry["applicability"] == {"modelIds": ["vl80s"]}
        assert entry["publicationStatus"] == "CANDIDATE"
        assert entry["actionDisposition"] == "CONDITIONAL_ACTION"
        assert "vl80s-general" in entry["diagnosticMeta"]["legacyApplicableVariantIds"]

        if is_extended_pack:
            extended_ids.add(entry_id)
            assert entry["layer"] == "EXTENDED"
            assert entry["diagnosticMeta"]["corpus"] == "EXTENDED"
            assert entry["diagnosticMeta"]["extendedClass"] == "SUPPLEMENTAL_OPERATIONAL"
            assert entry["migrationRefs"]["originLayer"] == "DiagnosticExtendedCatalog.kt"
        else:
            recommended_ids.add(entry_id)
            assert entry["layer"] == "STANDARD"
            assert entry["diagnosticMeta"]["corpus"] == "RECOMMENDED"
            assert entry["migrationRefs"]["originLayer"] != "DiagnosticExtendedCatalog.kt"

        for alias in entry.get("aliases", []):
            assert alias not in aliases, alias
            aliases[alias] = entry_id

assert len(entries) == 103, len(entries)
assert len(recommended_ids) == 44, len(recommended_ids)
assert len(extended_ids) == 59, len(extended_ids)
assert REPORT["scenarioCount"] == 103
assert REPORT["edgeCount"] == 1181
assert REPORT["recommendedCount"] == 44
assert REPORT["supplementalOperationalCount"] == 59
assert REPORT["publicationCandidateCount"] == 103
assert REPORT["legacyGeneralVariantCount"] == 103

assert len(aliases) == 103
assert set(GRAPH["scenarioIdMap"].values()) == set(entries)
assert set(GRAPH["scenarioIdMap"].keys()) == set(aliases)
assert len(GRAPH["runtimePayload"]["edges"]) == 1181

candidate = entries["vl80s.diag.pantograph-no-rise"]
assert candidate["diagnosticMeta"]["acceptanceGate"] == "GOLDEN_REFERENCE_PASS_NOT_PASSED"
assert "pantograph-no-rise" in candidate["aliases"]

assert not (DIAG_ROOT / "recommended/pantograph-no-rise.candidate.pack.json").exists()

# Build the full indexed target graph.
runtime_entries = {}
runtime_aliases = {}
for relative in RUNTIME["packs"]:
    path = ROOT / "content-packs" / relative
    document = json.loads(path.read_text(encoding="utf-8"))
    for entry in document.get("entries", []):
        assert entry["id"] not in runtime_entries, entry["id"]
        runtime_entries[entry["id"]] = entry
        for alias in entry.get("aliases", []):
            assert alias not in runtime_aliases, alias
            runtime_aliases[alias] = entry["id"]

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

for entry in runtime_entries.values():
    for link in entry.get("links", []):
        target_id = link["targetId"]
        canonical = (
            target_id
            if target_id in runtime_entries
            else runtime_aliases.get(target_id)
        )
        assert canonical is not None, (entry["id"], target_id)
        assert runtime_entries[canonical]["type"] == expected_type[link["type"]], (
            entry["id"],
            target_id,
            link["type"],
            runtime_entries[canonical]["type"],
        )

# Every direct diagnostic cross-feature relation is bidirectional.
for diagnostic_id, entry in entries.items():
    for link in entry.get("links", []):
        if link["type"] not in {"EQUIPMENT", "ACCEPTANCE_ITEM", "ATLAS_SCHEME"}:
            continue
        target = runtime_entries[link["targetId"]]
        assert any(
            reverse["type"] == "RELATED_SCENARIO"
            and reverse["targetId"] == diagnostic_id
            for reverse in target.get("links", [])
        ), (link["targetId"], diagnostic_id)

assert RUNTIME["featureIndexes"]["diagnostics"] == (
    "electric/vl80s/diagnostics/index.json"
)
for relative in recommended_paths + extended_paths:
    assert relative in RUNTIME["packs"]

print(
    "VL80S_DIAGNOSTICS_MIGRATION_PASS",
    len(entries),
    len(recommended_ids),
    len(extended_ids),
    len(GRAPH["runtimePayload"]["edges"]),
)
