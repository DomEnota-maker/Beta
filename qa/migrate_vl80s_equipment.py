#!/usr/bin/env python3
import json
import re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DONOR = ROOT / "build/vl80s-donor"
MODEL_ROOT = ROOT / "content-packs/electric/vl80s"
EQUIPMENT_SOURCE = DONOR / "vl80s_equipment.json"
ELECTRICAL_SOURCE = DONOR / "vl80s_electrical.json"
PNEUMATIC_SOURCE = DONOR / "vl80s_pneumatic.json"

OLD_ATLAS = MODEL_ROOT / "atlas/pantograph.vertical.pack.json"
OLD_TECH = MODEL_ROOT / "technical-data/pantograph.vertical.pack.json"

equipment_doc = json.loads(EQUIPMENT_SOURCE.read_text(encoding="utf-8"))
records = equipment_doc["records"]
record_ids = {item["id"] for item in records}
equipment_name = {item["id"]: item["name"] for item in records}

scheme_name = {}
for path, key in (
    (ELECTRICAL_SOURCE, "baseSchemes"),
    (PNEUMATIC_SOURCE, "views"),
):
    document = json.loads(path.read_text(encoding="utf-8"))
    for item in document.get(key, []):
        scheme_name[item["id"]] = item.get("title", item["id"])

old_atlas = json.loads(OLD_ATLAS.read_text(encoding="utf-8")) if OLD_ATLAS.exists() else {"entries": []}
old_tech = json.loads(OLD_TECH.read_text(encoding="utf-8")) if OLD_TECH.exists() else {"entries": []}
old_atlas_by_id = {entry["id"]: entry for entry in old_atlas.get("entries", [])}
old_tech_by_id = {entry["id"]: entry for entry in old_tech.get("entries", [])}

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

def sanitize_user_text(value):
    if not isinstance(value, str):
        return value
    text = value
    replacements = {}
    replacements.update(equipment_name)
    replacements.update(scheme_name)
    for internal, human in sorted(replacements.items(), key=lambda item: -len(item[0])):
        text = text.replace(internal, human)

    # Service-only implementation vocabulary is retained in migrationRefs,
    # never in user-visible blocks.
    forbidden = (
        "Feature rule",
        "actualEquipmentOverrides",
        "VL-SRC-",
        "VL-SYS-",
        "VL-EQ-",
        "VL-SCH-",
    )
    if "vl80s_" in text or any(token in text for token in forbidden):
        return None
    return text

def user_lines(values):
    return unique(sanitize_user_text(value) for value in values)

def block(title, lines):
    lines = user_lines(lines)
    return {"title": title, "lines": lines} if lines else None

def tech_id(equipment_id):
    return "VL-TD-EQ-" + equipment_id.removeprefix("VL-EQ-")

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
            "requiresRuntime": "1.1.0",
            "sourceCatalogVersion": "test-3176d6ee-vl80s-equipment",
            "checksums": {},
        },
        "entries": entries,
    }

def merge_reference_equipment(entry, equipment_id):
    old = old_atlas_by_id.get(equipment_id)
    if not old:
        return
    entry["sourceRefs"] = unique(entry.get("sourceRefs", []) + old.get("sourceRefs", []))

    # Preserve meaningful cross-feature links already proven by the first slice.
    for link in old.get("links", []):
        if link.get("type") in {"RELATED_SCENARIO", "ACCEPTANCE_ITEM"}:
            if link not in entry["links"]:
                entry["links"].append(link)

    useful = []
    for line in old.get("details", []):
        if "canonical ID" in line or "reference pack" in line:
            continue
        cleaned = sanitize_user_text(line)
        if cleaned:
            useful.append(cleaned)
    if useful:
        entry["blocks"].append({"title": "Дополнительная профильная справка", "lines": unique(useful)})

def merge_reference_technical(entry, old_id):
    old = old_tech_by_id.get(old_id)
    if not old:
        return
    entry["aliases"] = unique(entry.get("aliases", []) + [old_id])
    entry["sourceRefs"] = unique(entry.get("sourceRefs", []) + old.get("sourceRefs", []))

    lines = []
    if old.get("summary"):
        lines.append(old["summary"])
    lines.extend(old.get("details", []))
    lines = user_lines(lines)
    if lines:
        entry["blocks"].append({"title": "Дополнительная профильная справка", "lines": lines})

    for link in old.get("links", []):
        if link.get("type") == "EQUIPMENT" and link.get("targetId") != entry["links"][0]["targetId"]:
            if link not in entry["links"]:
                entry["links"].append(link)

by_domain = defaultdict(list)
for record in records:
    by_domain[record["domain"]].append(record)

atlas_paths = []
technical_paths = []
all_equipment_ids = set()
all_technical_ids = set()

for domain in sorted(by_domain):
    equipment_entries = []
    technical_entries = []
    for record in sorted(by_domain[domain], key=lambda item: item["id"]):
        equipment_id = record["id"]
        technical_id = tech_id(equipment_id)
        all_equipment_ids.add(equipment_id)
        all_technical_ids.add(technical_id)

        search_terms = unique(
            [record.get("name"), record.get("slug")]
            + record.get("aliases", [])
            + record.get("modelNames", [])
            + record.get("schemeDesignations", [])
        )

        equipment_links = [
            {
                "type": "TECHNICAL_DATA",
                "targetId": technical_id,
                "role": "technical-description",
            }
        ]
        for relation in record.get("relations", []):
            target = relation.get("targetId")
            if target in record_ids:
                equipment_links.append(
                    {
                        "type": "EQUIPMENT",
                        "targetId": target,
                        "role": relation.get("type") or "related-equipment",
                    }
                )

        equipment_blocks = []
        location = (record.get("location") or {}).get("text")
        value = block("Расположение", [location])
        if value:
            equipment_blocks.append(value)

        installation = []
        if record.get("modelNames"):
            installation.append("Исполнения/обозначения: " + ", ".join(record["modelNames"]))
        if record.get("quantity"):
            installation.append(record["quantity"])
        if record.get("schemeDesignations"):
            installation.append("Обозначения на схемах: " + ", ".join(record["schemeDesignations"]))
        value = block("Исполнение", installation)
        if value:
            equipment_blocks.append(value)

        equipment_entry = {
            "id": equipment_id,
            "type": "EQUIPMENT",
            "owner": {"kind": "MODEL", "modelId": "vl80s"},
            "applicability": {"modelIds": ["vl80s"]},
            "publicationStatus": "ACTIVE",
            "provenance": "UNKNOWN",
            "sourceStatus": "UNKNOWN",
            "actionDisposition": "INFORMATION_ONLY",
            "title": record["name"],
            "summary": record.get("purpose"),
            "blocks": equipment_blocks,
            "searchTerms": search_terms,
            "sourceRefs": record.get("sourceRefs", []),
            "links": equipment_links,
            "migrationRefs": {
                "legacyId": record.get("legacyId"),
                "slug": record.get("slug"),
                "domain": domain,
                "systemIds": record.get("systemIds", []),
                "diagnosticScenarioIds": record.get("diagnosticScenarioIds", []),
                "schemeNodeIds": record.get("schemeNodeIds", []),
                "legacyHotspotIds": record.get("legacyHotspotIds", []),
                "evidenceStatus": record.get("evidenceStatus"),
                "relationCoverage": record.get("relationCoverage"),
                "featureRules": record.get("featureRules", []),
                "equipmentOverrides": record.get("equipmentOverrides", []),
                "unresolvedSystemConnections": record.get("unresolvedSystemConnections", []),
            },
        }
        merge_reference_equipment(equipment_entry, equipment_id)
        equipment_entries.append(equipment_entry)

        technical_blocks = []
        feature_notes = [
            item.get("note")
            for item in record.get("featureRules", [])
            if isinstance(item, dict)
        ] + [
            item.get("note")
            for item in record.get("equipmentOverrides", [])
            if isinstance(item, dict)
        ]
        for title, lines in (
            ("Принцип работы", [record.get("principle")]),
            ("Основные параметры", record.get("parameters", [])),
            ("Нормальное состояние", record.get("normalState", [])),
            ("Признаки отклонений", record.get("deviationSigns", [])),
            ("Примечания", record.get("notes", [])),
            ("Безопасность", record.get("safetyNotes", [])),
            ("Особенности исполнения", feature_notes),
        ):
            value = block(title, lines)
            if value:
                technical_blocks.append(value)

        aliases = []
        old_id = None
        if equipment_id == "VL-EQ-HV-002":
            old_id = "vl80s-panto-tech-001"
        elif equipment_id == "VL-EQ-PN-002":
            old_id = "vl80s-panto-tech-002"
        if old_id:
            aliases.append(old_id)

        technical_entry = {
            "id": technical_id,
            "aliases": aliases,
            "type": "TECHNICAL_DATA",
            "owner": {"kind": "MODEL", "modelId": "vl80s"},
            "applicability": {"modelIds": ["vl80s"]},
            "publicationStatus": "ACTIVE",
            "provenance": "UNKNOWN",
            "sourceStatus": "UNKNOWN",
            "actionDisposition": "INFORMATION_ONLY",
            "title": f"{record['name']} — технические данные",
            "summary": record.get("purpose"),
            "blocks": technical_blocks,
            "searchTerms": search_terms,
            "sourceRefs": record.get("sourceRefs", []),
            "links": [
                {
                    "type": "EQUIPMENT",
                    "targetId": equipment_id,
                    "role": "describes-equipment",
                }
            ],
            "migrationRefs": {
                "equipmentId": equipment_id,
                "domain": domain,
                "evidenceStatus": record.get("evidenceStatus"),
            },
        }
        if old_id:
            merge_reference_technical(technical_entry, old_id)
        technical_entries.append(technical_entry)

    suffix = domain.lower()
    atlas_path = MODEL_ROOT / f"atlas/equipment/{suffix}.pack.json"
    technical_path = MODEL_ROOT / f"technical-data/equipment/{suffix}.pack.json"
    atlas_path.parent.mkdir(parents=True, exist_ok=True)
    technical_path.parent.mkdir(parents=True, exist_ok=True)

    atlas_path.write_text(
        json.dumps(
            pack(f"vl80s.atlas.equipment.{suffix}", equipment_entries),
            ensure_ascii=False,
            indent=2,
        ) + "\n",
        encoding="utf-8",
    )
    technical_path.write_text(
        json.dumps(
            pack(f"vl80s.technical-data.equipment.{suffix}", technical_entries),
            ensure_ascii=False,
            indent=2,
        ) + "\n",
        encoding="utf-8",
    )
    atlas_paths.append(atlas_path)
    technical_paths.append(technical_path)

# The first two manual slice files are replaced by the complete domain packs.
if OLD_ATLAS.exists():
    OLD_ATLAS.unlink()
if OLD_TECH.exists():
    OLD_TECH.unlink()

relative_packs = [
    str(path.relative_to(ROOT / "content-packs")).replace("\\", "/")
    for path in atlas_paths + technical_paths
]
relative_packs += [
    "electric/vl80s/acceptance/pantograph.vertical.pack.json",
    "electric/vl80s/diagnostics/recommended/pantograph-no-rise.candidate.pack.json",
]

runtime_index = {
    "schemaVersion": 1,
    "modelId": "vl80s",
    "packs": relative_packs,
}
(MODEL_ROOT / "runtime-index.json").write_text(
    json.dumps(runtime_index, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)

report = {
    "sourceCommit": "3176d6ee228f0ed4b78371f45209e10c1c724eff",
    "equipmentCount": len(all_equipment_ids),
    "technicalDataCount": len(all_technical_ids),
    "domains": {
        domain: len(by_domain[domain])
        for domain in sorted(by_domain)
    },
    "atlasPacks": [str(path.relative_to(ROOT)).replace("\\", "/") for path in atlas_paths],
    "technicalDataPacks": [str(path.relative_to(ROOT)).replace("\\", "/") for path in technical_paths],
    "replacedTemporaryPacks": [
        "content-packs/electric/vl80s/atlas/pantograph.vertical.pack.json",
        "content-packs/electric/vl80s/technical-data/pantograph.vertical.pack.json",
    ],
}
migration_dir = MODEL_ROOT / "migration"
migration_dir.mkdir(parents=True, exist_ok=True)
(migration_dir / "equipment-report.json").write_text(
    json.dumps(report, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)

print("VL80S_EQUIPMENT_MIGRATION_GENERATED", len(all_equipment_ids), len(all_technical_ids))
