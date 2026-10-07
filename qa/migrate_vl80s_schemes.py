#!/usr/bin/env python3
import json
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DONOR = ROOT / "build/vl80s-donor"
MODEL_ROOT = ROOT / "content-packs/electric/vl80s"
SCHEME_ROOT = MODEL_ROOT / "atlas/schemes"
EQUIPMENT_ROOT = MODEL_ROOT / "atlas/equipment"

electrical = json.loads((DONOR / "vl80s_electrical.json").read_text(encoding="utf-8"))
pneumatic = json.loads((DONOR / "vl80s_pneumatic.json").read_text(encoding="utf-8"))
electrical_variants = json.loads((DONOR / "vl80s_electrical_variants.json").read_text(encoding="utf-8"))
pneumatic_variants = json.loads((DONOR / "vl80s_pneumatic_variants.json").read_text(encoding="utf-8"))
profile = json.loads((MODEL_ROOT / "profile-catalog.json").read_text(encoding="utf-8"))
physical_profiles = [item["id"] for item in profile["physicalBuckets"]]
physical_profile_set = set(physical_profiles)

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

def source_ids(values):
    result = []
    for value in values or []:
        if isinstance(value, str):
            result.append(value)
        elif isinstance(value, dict):
            source_id = value.get("sourceId")
            if source_id:
                result.append(source_id)
    return unique(result)

def node_id(node):
    return node.get("equipmentId") or node.get("virtualNodeId") or node.get("id")

def normalize_node(node):
    result = {
        "label": node.get("label") or node_id(node),
    }
    if node.get("equipmentId"):
        result["equipmentId"] = node["equipmentId"]
    elif node.get("virtualNodeId"):
        result["virtualNodeId"] = node["virtualNodeId"]
    else:
        result["id"] = node_id(node)
    for key in ("kind", "domain", "systemIds"):
        if node.get(key):
            result[key] = node[key]
    return result

def normalize_electrical_edge(edge):
    return {
        "from": edge.get("fromEquipmentId") or edge.get("from") or edge.get("fromId"),
        "to": edge.get("toEquipmentId") or edge.get("to") or edge.get("toId"),
        "kind": edge.get("relationType") or edge.get("kind") or "functional_link",
        **({"direction": edge["direction"]} if edge.get("direction") else {}),
    }

def normalize_pneumatic_edge(edge):
    result = {
        "from": edge.get("from") or edge.get("fromId"),
        "to": edge.get("to") or edge.get("toId"),
        "kind": edge.get("kind") or edge.get("relationType") or "functional_link",
    }
    for key in ("status", "direction", "note"):
        if edge.get(key):
            result[key] = edge[key]
    return result

def scheme_entry(raw, profiles, publication_status):
    equipment_ids = unique(
        node.get("equipmentId")
        for node in raw.get("nodes", [])
        if node.get("equipmentId")
    )
    links = [
        {
            "type": "EQUIPMENT",
            "targetId": equipment_id,
            "role": "scheme-equipment",
        }
        for equipment_id in equipment_ids
    ]
    scheme_type = raw.get("schemeType") or raw.get("type") or "scheme"
    return {
        "id": raw["id"],
        "type": "ATLAS_SCHEME",
        "owner": {"kind": "MODEL", "modelId": "vl80s"},
        "applicability": {
            "modelIds": ["vl80s"],
            **({"variantIds": profiles} if profiles else {}),
        },
        "publicationStatus": publication_status,
        "provenance": "UNKNOWN",
        "sourceStatus": "UNKNOWN",
        "actionDisposition": "INFORMATION_ONLY",
        "title": raw["title"],
        "summary": raw.get("scopeNote"),
        "searchTerms": unique([raw["title"], scheme_type]),
        "sourceRefs": source_ids(raw.get("sourceRefs")),
        "links": links,
        "migrationRefs": {
            "originalStatus": raw.get("status"),
            "systems": raw.get("systems", []),
            "rawProfiles": raw.get("profiles", []),
        },
    }

def normalized_scheme(raw, profiles, edge_key, edge_normalizer):
    scheme_type = raw.get("schemeType") or raw.get("type") or "scheme"
    return {
        "id": raw["id"],
        "title": raw["title"],
        "schemeType": scheme_type,
        "status": raw.get("status") or "UNKNOWN",
        "profiles": profiles,
        "systems": raw.get("systems", []),
        "sourceRefs": source_ids(raw.get("sourceRefs")),
        "scopeNote": raw.get("scopeNote"),
        "nodes": [normalize_node(node) for node in raw.get("nodes", [])],
        "edges": [
            edge_normalizer(edge)
            for edge in raw.get(edge_key, [])
        ],
    }

def combined_pack(pack_id, variant_ids, entries, schemes):
    return {
        "manifest": {
            "schemaVersion": 1,
            "packId": pack_id,
            "packVersion": "0.1.0",
            "family": "ELECTRIC",
            "modelIds": ["vl80s"],
            "variantIds": variant_ids,
            "locale": "ru-RU",
            "entries": [entry["id"] for entry in entries],
            "requiresRuntime": "1.1.0",
            "sourceCatalogVersion": "test-3176d6ee-vl80s-schemes",
            "checksums": {},
        },
        "entries": entries,
        "schemes": schemes,
    }

SCHEME_ROOT.mkdir(parents=True, exist_ok=True)

electrical_entries = []
electrical_graphs = []
for raw in electrical["baseSchemes"]:
    profiles = raw.get("profiles", [])
    unknown = set(profiles) - physical_profile_set
    if unknown:
        raise SystemExit(f"Unknown electrical profile for {raw['id']}: {sorted(unknown)}")

    publication_status = (
        "CANDIDATE"
        if raw.get("status") == "PROFILE_REQUIRED"
        else "ACTIVE"
    )
    electrical_entries.append(
        scheme_entry(raw, profiles, publication_status)
    )
    electrical_graphs.append(
        normalized_scheme(raw, profiles, "semanticEdges", normalize_electrical_edge)
    )

electrical_variant_ids = sorted({
    profile_id
    for entry in electrical_entries
    for profile_id in entry.get("applicability", {}).get("variantIds", [])
})
electrical_path = SCHEME_ROOT / "electrical.pack.json"
electrical_path.write_text(
    json.dumps(
        combined_pack(
            "vl80s.atlas.schemes.electrical",
            electrical_variant_ids,
            electrical_entries,
            electrical_graphs,
        ),
        ensure_ascii=False,
        indent=2,
    ) + "\n",
    encoding="utf-8",
)

# Pneumatic donor views are explicitly functional section-aware references.
# They remain informational for all physical buckets; exact mounting/valve claims
# are still governed by the preserved variant policy and scope notes.
pneumatic_entries = []
pneumatic_graphs = []
for raw in pneumatic["views"]:
    pneumatic_entries.append(
        scheme_entry(raw, [], "ACTIVE")
    )
    pneumatic_graphs.append(
        normalized_scheme(raw, physical_profiles, "edges", normalize_pneumatic_edge)
    )

pneumatic_path = SCHEME_ROOT / "pneumatic.pack.json"
pneumatic_path.write_text(
    json.dumps(
        combined_pack(
            "vl80s.atlas.schemes.pneumatic",
            [],
            pneumatic_entries,
            pneumatic_graphs,
        ),
        ensure_ascii=False,
        indent=2,
    ) + "\n",
    encoding="utf-8",
)

policy_dir = SCHEME_ROOT / "policies"
policy_dir.mkdir(parents=True, exist_ok=True)

electrical_policy_path = policy_dir / "electrical-variants.json"
electrical_policy_path.write_text(
    json.dumps(
        {
            "schemaVersion": 1,
            "sourceSnapshot": {
                "repository": "DomEnota-maker/Test-",
                "commit": "3176d6ee228f0ed4b78371f45209e10c1c724eff",
                "asset": "vl80s_electrical_variants.json.gz",
                "blobSha": "f9f7956f871cbb04d38b8315e1b902372d5f3f7b",
            },
            "runtimeStatus": "PENDING_OVERLAY_ENGINE",
            "policy": electrical_variants,
        },
        ensure_ascii=False,
        indent=2,
    ) + "\n",
    encoding="utf-8",
)

pneumatic_policy_path = policy_dir / "pneumatic-variants.json"
pneumatic_policy_path.write_text(
    json.dumps(
        {
            "schemaVersion": 1,
            "sourceSnapshot": {
                "repository": "DomEnota-maker/Test-",
                "commit": "3176d6ee228f0ed4b78371f45209e10c1c724eff",
                "asset": "vl80s_pneumatic_variants.json.gz",
                "blobSha": "3899e9e653f17c7b26e2c3d97a8852d4537a2537",
            },
            "runtimeStatus": "PENDING_OVERLAY_ENGINE",
            "policy": pneumatic_variants,
        },
        ensure_ascii=False,
        indent=2,
    ) + "\n",
    encoding="utf-8",
)

scheme_pack_paths = [
    "electric/vl80s/atlas/schemes/electrical.pack.json",
    "electric/vl80s/atlas/schemes/pneumatic.pack.json",
]
policy_paths = [
    "electric/vl80s/atlas/schemes/policies/electrical-variants.json",
    "electric/vl80s/atlas/schemes/policies/pneumatic-variants.json",
]
scheme_index_path = SCHEME_ROOT / "index.json"
scheme_index_path.write_text(
    json.dumps(
        {
            "schemaVersion": 1,
            "modelId": "vl80s",
            "schemePacks": scheme_pack_paths,
            "variantPolicies": policy_paths,
        },
        ensure_ascii=False,
        indent=2,
    ) + "\n",
    encoding="utf-8",
)

# Add reverse equipment -> scheme links to the canonical equipment packs.
reverse = defaultdict(set)
for entry in electrical_entries + pneumatic_entries:
    for link in entry["links"]:
        reverse[link["targetId"]].add(entry["id"])

for path in sorted(EQUIPMENT_ROOT.glob("*.pack.json")):
    document = json.loads(path.read_text(encoding="utf-8"))
    changed = False
    for entry in document["entries"]:
        for scheme_id in sorted(reverse.get(entry["id"], [])):
            link = {
                "type": "ATLAS_SCHEME",
                "targetId": scheme_id,
                "role": "scheme-membership",
            }
            if link not in entry["links"]:
                entry["links"].append(link)
                changed = True
    if changed:
        path.write_text(
            json.dumps(document, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )

# Preserve all previously indexed packs and model metadata.
runtime_index_path = MODEL_ROOT / "runtime-index.json"
runtime_index = json.loads(runtime_index_path.read_text(encoding="utf-8"))
runtime_index["packs"] = unique(runtime_index["packs"] + scheme_pack_paths)
runtime_index.setdefault("featureIndexes", {})["atlas"] = (
    "electric/vl80s/atlas/schemes/index.json"
)
runtime_index_path.write_text(
    json.dumps(runtime_index, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)

report = {
    "sourceCommit": "3176d6ee228f0ed4b78371f45209e10c1c724eff",
    "electricalSchemes": len(electrical_entries),
    "pneumaticSchemes": len(pneumatic_entries),
    "equipmentWithSchemeLinks": len(reverse),
    "candidateSchemes": [
        entry["id"]
        for entry in electrical_entries + pneumatic_entries
        if entry["publicationStatus"] == "CANDIDATE"
    ],
    "variantPolicies": policy_paths,
}
migration_dir = MODEL_ROOT / "migration"
migration_dir.mkdir(parents=True, exist_ok=True)
(migration_dir / "schemes-report.json").write_text(
    json.dumps(report, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)

print(
    "VL80S_SCHEME_MIGRATION_GENERATED",
    len(electrical_entries),
    len(pneumatic_entries),
    len(reverse),
)
