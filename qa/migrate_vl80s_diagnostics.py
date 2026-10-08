#!/usr/bin/env python3
import json
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DONOR = ROOT / "build/vl80s-donor/vl80s_diagnostics.json"
MODEL_ROOT = ROOT / "content-packs/electric/vl80s"
DIAG_ROOT = MODEL_ROOT / "diagnostics"
EQUIPMENT_ROOT = MODEL_ROOT / "atlas/equipment"
ACCEPTANCE_ROOT = MODEL_ROOT / "acceptance"
SCHEME_ROOT = MODEL_ROOT / "atlas/schemes"

SOURCE_COMMIT = "3176d6ee228f0ed4b78371f45209e10c1c724eff"
SOURCE_BLOB = "6ffdfdabf2f467dcdc4958768905f2b675ac0c26"

CATEGORY_SLUG = {
    "Тяга": "traction",
    "Вспомогательные машины": "auxiliary",
    "Высоковольтные цепи": "high-voltage",
    "Тормоза": "brakes",
    "Пневматика": "pneumatics",
    "Цепи управления": "control",
    "Ходовая часть": "running-gear",
    "Кабина и связь": "cab-communication",
    "Безопасность движения": "traffic-safety",
    "Пожарная безопасность": "fire-safety",
    "Электрическое торможение": "electric-braking",
    "Аварийные ситуации": "emergency",
    "Защиты": "protections",
    "Механическая часть": "mechanical",
    "Силовые цепи": "power-circuits",
}

def unique(values):
    result = []
    seen = set()
    for value in values:
        if value is None:
            continue
        if isinstance(value, str):
            value = value.strip()
        if not value or value in seen:
            continue
        seen.add(value)
        result.append(value)
    return result

def all_strings(value):
    result = []
    if isinstance(value, str):
        result.append(value)
    elif isinstance(value, list):
        for item in value:
            result.extend(all_strings(item))
    elif isinstance(value, dict):
        for item in value.values():
            result.extend(all_strings(item))
    return result

def source_ids(values):
    result = []
    for value in values or []:
        if isinstance(value, str):
            result.append(value)
        elif isinstance(value, dict):
            source_id = value.get("sourceId") or value.get("id")
            if source_id:
                result.append(source_id)
    return unique(result)

def canonical_diagnostic_id(legacy_id):
    return f"vl80s.diag.{legacy_id}"

def link_key(link):
    return (link.get("type"), link.get("targetId"), link.get("role"))

def add_link(entry, link):
    existing = {link_key(item) for item in entry.setdefault("links", [])}
    if link_key(link) not in existing:
        entry["links"].append(link)

def pack(pack_id, entries):
    return {
        "manifest": {
            "schemaVersion": 1,
            "packId": pack_id,
            "packVersion": "0.1.0",
            "family": "ELECTRIC",
            "modelIds": ["vl80s"],
            "variantIds": [],
            "locale": "ru-RU",
            "entries": [entry["id"] for entry in entries],
            "requiresRuntime": "1.2.0",
            "sourceCatalogVersion": "test-3176d6ee-vl80s-diagnostics",
            "checksums": {},
        },
        "entries": entries,
    }

root = json.loads(DONOR.read_text(encoding="utf-8"))
scenarios = root.get("scenarios", [])
edges = root.get("edges", [])
if len(scenarios) != 103:
    raise SystemExit(f"Expected 103 donor scenarios, got {len(scenarios)}")
if len(edges) != 1181:
    raise SystemExit(f"Expected 1181 donor diagnostic edges, got {len(edges)}")

legacy_ids = {item.get("id") for item in scenarios}
if None in legacy_ids or len(legacy_ids) != 103:
    raise SystemExit("Diagnostic IDs are missing or duplicated")
canonical_by_legacy = {
    legacy_id: canonical_diagnostic_id(legacy_id)
    for legacy_id in sorted(legacy_ids)
}

equipment_documents = {}
equipment = {}
for path in sorted(EQUIPMENT_ROOT.glob("*.pack.json")):
    document = json.loads(path.read_text(encoding="utf-8"))
    equipment_documents[path] = document
    for entry in document.get("entries", []):
        if entry.get("type") == "EQUIPMENT":
            equipment[entry["id"]] = entry
if len(equipment) != 89:
    raise SystemExit(f"Expected 89 equipment entries, got {len(equipment)}")

acceptance_documents = {}
acceptance = {}
for path in (
    ACCEPTANCE_ROOT / "required.pack.json",
    ACCEPTANCE_ROOT / "items.pack.json",
):
    document = json.loads(path.read_text(encoding="utf-8"))
    acceptance_documents[path] = document
    for entry in document.get("entries", []):
        acceptance[entry["id"]] = entry
if len(acceptance) != 81:
    raise SystemExit(f"Expected 81 acceptance items, got {len(acceptance)}")

scheme_documents = {}
schemes = {}
for path in (
    SCHEME_ROOT / "electrical.pack.json",
    SCHEME_ROOT / "pneumatic.pack.json",
):
    document = json.loads(path.read_text(encoding="utf-8"))
    scheme_documents[path] = document
    for entry in document.get("entries", []):
        schemes[entry["id"]] = entry
if len(schemes) != 16:
    raise SystemExit(f"Expected 16 scheme entries, got {len(schemes)}")

old_candidate_path = DIAG_ROOT / "recommended/pantograph-no-rise.candidate.pack.json"
old_candidate = None
if old_candidate_path.exists():
    old_doc = json.loads(old_candidate_path.read_text(encoding="utf-8"))
    old_candidate = next(
        (item for item in old_doc.get("entries", [])
         if item.get("id") == "vl80s.diag.pantograph-no-rise"),
        None,
    )

groups = {
    "recommended": defaultdict(list),
    "supplemental": defaultdict(list),
}
reverse_equipment = defaultdict(set)
reverse_acceptance = defaultdict(set)
reverse_scheme = defaultdict(set)

for raw in scenarios:
    legacy_id = raw["id"]
    canonical_id = canonical_by_legacy[legacy_id]
    category = raw.get("category")
    if category not in CATEGORY_SLUG:
        raise SystemExit(f"{legacy_id}: unknown category {category!r}")
    slug = CATEGORY_SLUG[category]

    legacy_variants = raw.get("applicableVariantIds") or []
    if "vl80s-general" not in legacy_variants:
        raise SystemExit(
            f"{legacy_id}: donor scenario lacks vl80s-general applicability"
        )

    is_supplemental = raw.get("originLayer") == "DiagnosticExtendedCatalog.kt"
    corpus = "EXTENDED" if is_supplemental else "RECOMMENDED"
    extended_class = "SUPPLEMENTAL_OPERATIONAL" if is_supplemental else None

    equipment_ids = unique(raw.get("equipmentIds") or [])
    unknown_equipment = sorted(set(equipment_ids) - set(equipment))
    if unknown_equipment:
        raise SystemExit(
            f"{legacy_id}: unknown equipment IDs {unknown_equipment}"
        )

    related_legacy = unique(raw.get("relatedScenarioIds") or [])
    unknown_related = sorted(set(related_legacy) - legacy_ids)
    if unknown_related:
        raise SystemExit(
            f"{legacy_id}: unknown related scenario IDs {unknown_related}"
        )

    acceptance_ids = sorted({
        value
        for value in all_strings(raw.get("acceptance") or {})
        if value.startswith("VL80-ACC-") or value.startswith("VL80-REQ-")
    })
    unknown_acceptance = sorted(set(acceptance_ids) - set(acceptance))
    if unknown_acceptance:
        raise SystemExit(
            f"{legacy_id}: unknown acceptance IDs {unknown_acceptance}"
        )

    scheme_ids = sorted({
        value
        for value in (
            all_strings(raw.get("electricalSchemes") or {})
            + all_strings(raw.get("pneumaticViews") or {})
        )
        if value.startswith("VL-SCH-")
    })
    unknown_schemes = sorted(set(scheme_ids) - set(schemes))
    if unknown_schemes:
        raise SystemExit(
            f"{legacy_id}: unknown scheme IDs {unknown_schemes}"
        )

    links = []
    for equipment_id in equipment_ids:
        links.append({
            "type": "EQUIPMENT",
            "targetId": equipment_id,
            "role": "diagnostic-equipment",
        })
        reverse_equipment[equipment_id].add(canonical_id)

    for target_legacy in related_legacy:
        links.append({
            "type": "RELATED_SCENARIO",
            "targetId": canonical_by_legacy[target_legacy],
            "role": "related-diagnostic",
        })

    for acceptance_id in acceptance_ids:
        links.append({
            "type": "ACCEPTANCE_ITEM",
            "targetId": acceptance_id,
            "role": "related-acceptance",
        })
        reverse_acceptance[acceptance_id].add(canonical_id)

    for scheme_id in scheme_ids:
        links.append({
            "type": "ATLAS_SCHEME",
            "targetId": scheme_id,
            "role": "related-scheme",
        })
        reverse_scheme[scheme_id].add(canonical_id)

    aliases = [legacy_id]
    source_refs = source_ids(raw.get("sourceRefs"))
    details = []
    if canonical_id == "vl80s.diag.pantograph-no-rise" and old_candidate:
        aliases = unique(aliases + old_candidate.get("aliases", []))
        source_refs = unique(source_refs + old_candidate.get("sourceRefs", []))
        details = unique(old_candidate.get("details", []))

    entry = {
        "id": canonical_id,
        "aliases": aliases,
        "type": "DIAGNOSTIC_SCENARIO",
        "owner": {"kind": "MODEL", "modelId": "vl80s"},
        "applicability": {"modelIds": ["vl80s"]},
        "layer": "EXTENDED" if is_supplemental else "STANDARD",
        # All migrated scenarios stay fail-closed until feature-diagnostics
        # can execute the canonical graph and acceptance is complete.
        "publicationStatus": "CANDIDATE",
        "provenance": "UNKNOWN",
        "sourceStatus": "UNKNOWN",
        "actionDisposition": "CONDITIONAL_ACTION",
        "title": raw.get("title") or legacy_id,
        "summary": raw.get("summary"),
        "details": details,
        "blocks": [
            {"title": "Категория", "lines": [category]},
        ],
        "searchTerms": unique([
            raw.get("title"),
            raw.get("summary"),
            category,
        ]),
        "sourceRefs": source_refs,
        "links": links,
        "diagnosticMeta": {
            "corpus": corpus,
            **({"extendedClass": extended_class} if extended_class else {}),
            "severity": raw.get("severity"),
            "systemIds": raw.get("systemIds") or [],
            "legacyApplicableVariantIds": legacy_variants,
            "articleTargets": raw.get("articleTargets") or [],
            "qna": raw.get("qna") or {},
            "acceptance": raw.get("acceptance") or {},
            "electricalSchemes": raw.get("electricalSchemes") or {},
            "pneumaticViews": raw.get("pneumaticViews") or {},
            "questionCount": raw.get("questionCount"),
            "checkCount": raw.get("checkCount"),
            "dangerCount": raw.get("dangerCount"),
            "acceptanceGate": (
                "GOLDEN_REFERENCE_PASS_NOT_PASSED"
                if canonical_id == "vl80s.diag.pantograph-no-rise"
                else "FEATURE_DIAGNOSTICS_RUNTIME_PENDING"
            ),
        },
        "migrationRefs": {
            "legacyId": legacy_id,
            "originLayer": raw.get("originLayer"),
            "originId": raw.get("originId"),
            "runtimeStatus": raw.get("runtimeStatus"),
            "linkProvenance": raw.get("linkProvenance"),
        },
    }

    target_group = groups["supplemental" if is_supplemental else "recommended"]
    target_group[slug].append(entry)

recommended_root = DIAG_ROOT / "recommended"
supplemental_root = DIAG_ROOT / "extended/supplemental-operational"
recommended_root.mkdir(parents=True, exist_ok=True)
supplemental_root.mkdir(parents=True, exist_ok=True)

for path in recommended_root.glob("*.pack.json"):
    path.unlink()
for path in supplemental_root.glob("*.pack.json"):
    path.unlink()

recommended_paths = []
extended_paths = []

for slug, entries in sorted(groups["recommended"].items()):
    entries.sort(key=lambda item: item["id"])
    path = recommended_root / f"{slug}.pack.json"
    path.write_text(
        json.dumps(
            pack(f"vl80s.diagnostics.recommended.{slug}", entries),
            ensure_ascii=False,
            indent=2,
        ) + "\n",
        encoding="utf-8",
    )
    recommended_paths.append(
        str(path.relative_to(ROOT / "content-packs")).replace("\\", "/")
    )

for slug, entries in sorted(groups["supplemental"].items()):
    entries.sort(key=lambda item: item["id"])
    path = supplemental_root / f"{slug}.pack.json"
    path.write_text(
        json.dumps(
            pack(
                f"vl80s.diagnostics.extended.supplemental-operational.{slug}",
                entries,
            ),
            ensure_ascii=False,
            indent=2,
        ) + "\n",
        encoding="utf-8",
    )
    extended_paths.append(
        str(path.relative_to(ROOT / "content-packs")).replace("\\", "/")
    )

if old_candidate_path.exists():
    old_candidate_path.unlink()

# Reverse links: equipment / acceptance / scheme -> diagnostic.
for path, document in equipment_documents.items():
    changed = False
    for entry in document.get("entries", []):
        # Remove stale legacy diagnostic targets if they ever existed.
        original = entry.get("links", [])
        filtered = [
            link for link in original
            if not (
                link.get("type") == "RELATED_SCENARIO"
                and link.get("targetId") in legacy_ids
            )
        ]
        if filtered != original:
            entry["links"] = filtered
            changed = True
        for diagnostic_id in sorted(reverse_equipment.get(entry["id"], [])):
            before = len(entry.get("links", []))
            add_link(entry, {
                "type": "RELATED_SCENARIO",
                "targetId": diagnostic_id,
                "role": "related-diagnostic",
            })
            changed = changed or len(entry["links"]) != before
    if changed:
        path.write_text(
            json.dumps(document, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )

for path, document in acceptance_documents.items():
    changed = False
    for entry in document.get("entries", []):
        for diagnostic_id in sorted(reverse_acceptance.get(entry["id"], [])):
            before = len(entry.get("links", []))
            add_link(entry, {
                "type": "RELATED_SCENARIO",
                "targetId": diagnostic_id,
                "role": "related-diagnostic",
            })
            changed = changed or len(entry["links"]) != before
    if changed:
        path.write_text(
            json.dumps(document, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )

for path, document in scheme_documents.items():
    changed = False
    for entry in document.get("entries", []):
        for diagnostic_id in sorted(reverse_scheme.get(entry["id"], [])):
            before = len(entry.get("links", []))
            add_link(entry, {
                "type": "RELATED_SCENARIO",
                "targetId": diagnostic_id,
                "role": "related-diagnostic",
            })
            changed = changed or len(entry["links"]) != before
    if changed:
        path.write_text(
            json.dumps(document, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )

# Preserve the complete canonical graph separately from list-card content.
old_graph_path = DIAG_ROOT / "graph.json"
relation_graph_path = DIAG_ROOT / "relation-graph.json"
if old_graph_path.exists():
    old_graph_path.unlink()

runtime_payload = {
    key: value
    for key, value in root.items()
    if key != "scenarios"
}
relation_graph_path.write_text(
    json.dumps(
        {
            "schemaVersion": 1,
            "modelId": "vl80s",
            "sourceSnapshot": {
                "repository": "DomEnota-maker/Test-",
                "commit": SOURCE_COMMIT,
                "asset": "app/src/main/assets/technical/vl80s_diagnostics.json.gz",
                "blobSha": SOURCE_BLOB,
            },
            "scenarioIdMap": canonical_by_legacy,
            "runtimePayload": runtime_payload,
        },
        ensure_ascii=False,
        indent=2,
    ) + "\n",
    encoding="utf-8",
)

index_path = DIAG_ROOT / "index.json"
executable_flow_rel = "electric/vl80s/diagnostics/runtime/executable-flow.json"
executable_flow_path = ROOT / "content-packs" / executable_flow_rel
executable_runtime_available = False

if executable_flow_path.exists():
    executable_flow = json.loads(
        executable_flow_path.read_text(encoding="utf-8")
    )
    if executable_flow.get("modelId") != "vl80s":
        raise SystemExit("Executable diagnostic runtime has wrong modelId")
    if (
        executable_flow.get("sourceSnapshot", {}).get("commit")
        != SOURCE_COMMIT
    ):
        raise SystemExit(
            "Executable diagnostic runtime donor commit does not match catalog donor"
        )
    if executable_flow.get("scenarioCount") != len(scenarios):
        raise SystemExit(
            "Executable diagnostic runtime scenario count does not match catalog"
        )
    executable_runtime_available = True

index_document = {
    "schemaVersion": 1,
    "modelId": "vl80s",
    "sourceSnapshot": {
        "repository": "DomEnota-maker/Test-",
        "commit": SOURCE_COMMIT,
        "asset": "app/src/main/assets/technical/vl80s_diagnostics.json.gz",
        "blobSha": SOURCE_BLOB,
    },
    "recommendedPacks": recommended_paths,
    "extendedCorpora": {
        "SUPPLEMENTAL_OPERATIONAL": extended_paths,
    },
    "relationGraph": "electric/vl80s/diagnostics/relation-graph.json",
    "runtimePayloadStatus": (
        "EXECUTABLE_FLOW_AVAILABLE"
        if executable_runtime_available
        else "RELATION_GRAPH_ONLY_NOT_EXECUTABLE"
    ),
    "interactiveRuntimeSource": "DiagnosticRepository.scenarios",
    "publicationPolicy": "ALL_CANDIDATE_UNTIL_FEATURE_RUNTIME_ACCEPTANCE",
    "goldenReferenceCandidate": "vl80s.diag.pantograph-no-rise",
}
if executable_runtime_available:
    index_document["executableFlow"] = executable_flow_rel

index_path.write_text(
    json.dumps(index_document, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)

runtime_index_path = MODEL_ROOT / "runtime-index.json"
runtime_index = json.loads(runtime_index_path.read_text(encoding="utf-8"))
runtime_index["packs"] = [
    value
    for value in runtime_index["packs"]
    if not value.startswith("electric/vl80s/diagnostics/")
]
runtime_index["packs"].extend(recommended_paths)
runtime_index["packs"].extend(extended_paths)
runtime_index["packs"] = unique(runtime_index["packs"])
runtime_index.setdefault("featureIndexes", {})["diagnostics"] = (
    "electric/vl80s/diagnostics/index.json"
)
runtime_index_path.write_text(
    json.dumps(runtime_index, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)

recommended_count = sum(len(value) for value in groups["recommended"].values())
extended_count = sum(len(value) for value in groups["supplemental"].values())

report = {
    "sourceCommit": SOURCE_COMMIT,
    "scenarioCount": len(scenarios),
    "edgeCount": len(edges),
    "relationGraphOnly": True,
    "donorRuntimeBaselineScenarioCount": (
        root.get("runtimeBaseline", {}).get("canonicalStage6Scenarios")
    ),
    "catalogScenarioCount": len(scenarios),
    "runtimeBaselineMetadataMismatch": (
        root.get("runtimeBaseline", {}).get("canonicalStage6Scenarios")
        != len(scenarios)
    ),
    "recommendedCount": recommended_count,
    "supplementalOperationalCount": extended_count,
    "recommendedPackCount": len(recommended_paths),
    "supplementalOperationalPackCount": len(extended_paths),
    "publicationCandidateCount": len(scenarios),
    "equipmentWithDiagnosticLinks": len(reverse_equipment),
    "acceptanceItemsWithDiagnosticLinks": len(reverse_acceptance),
    "schemesWithDiagnosticLinks": len(reverse_scheme),
    "legacyGeneralVariantCount": sum(
        1 for item in scenarios
        if "vl80s-general" in (item.get("applicableVariantIds") or [])
    ),
    "goldenReferenceCandidate": "vl80s.diag.pantograph-no-rise",
    "replacedTemporaryPacks": [
        "content-packs/electric/vl80s/diagnostics/recommended/pantograph-no-rise.candidate.pack.json"
    ],
}
migration_dir = MODEL_ROOT / "migration"
migration_dir.mkdir(parents=True, exist_ok=True)
(migration_dir / "diagnostics-report.json").write_text(
    json.dumps(report, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)

print(
    "VL80S_DIAGNOSTICS_MIGRATION_GENERATED",
    recommended_count,
    extended_count,
    len(edges),
    len(recommended_paths),
    len(extended_paths),
)
