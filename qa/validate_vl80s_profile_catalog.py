#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL_ROOT = ROOT / "content-packs/electric/vl80s"
PROFILE_PATH = MODEL_ROOT / "profile-catalog.json"
INDEX_PATH = MODEL_ROOT / "runtime-index.json"

profile = json.loads(PROFILE_PATH.read_text(encoding="utf-8"))
assert profile["schemaVersion"] == 1
assert profile["profileId"] == "vl80s"
assert profile["resolution"] == "section_aware_feature_based"
assert profile["sectionAware"] is True
assert profile["defaultVariantId"] == "vl80s_unknown"

buckets = profile["physicalBuckets"]
ids = [item["id"] for item in buckets]
assert len(ids) == 9
assert len(ids) == len(set(ids))
assert profile["defaultVariantId"] in ids
assert "vl80s-general" not in ids

required = {
    "vl80s_unknown",
    "vl80s_pre697",
    "vl80s_697_1260",
    "vl80s_1261_1405",
    "vl80s_1406_2318",
    "vl80s_2319_2348",
    "vl80s_2349_2653",
    "vl80s_2654plus",
    "vl80s_modified_or_mixed",
}
assert set(ids) == required
assert len(profile["experimentalOverrides"]) == 4
assert len(profile["nonMergeRules"]) == 6
assert "do_not_assume_same_variant_for_recombined_sections" in profile["nonMergeRules"]
assert "do_not_use_training_scheme_as_universal_mounting_scheme" in profile["nonMergeRules"]

index = json.loads(INDEX_PATH.read_text(encoding="utf-8"))
assert index["profileCatalog"] == "electric/vl80s/profile-catalog.json"

for relative in index["packs"]:
    document = json.loads((ROOT / "content-packs" / relative).read_text(encoding="utf-8"))
    for variant_id in document["manifest"].get("variantIds", []):
        assert variant_id in required, (relative, variant_id)
    for entry in document.get("entries", []):
        for variant_id in entry.get("applicability", {}).get("variantIds", []):
            assert variant_id in required, (entry["id"], variant_id)

catalog = json.loads((ROOT / "content-packs/catalog.json").read_text(encoding="utf-8"))
vl80s = next(item for item in catalog["models"] if item["modelId"] == "vl80s")
assert set(vl80s["variantIds"]) == required

print("VL80S_PROFILE_CATALOG_PASS", len(ids))
