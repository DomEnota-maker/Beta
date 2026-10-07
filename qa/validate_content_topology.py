#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG_PATH = ROOT / "content-packs/catalog.json"
catalog = json.loads(CATALOG_PATH.read_text(encoding="utf-8"))

assert catalog["schemaVersion"] == 1

common = catalog["common"]
assert common["root"] == "content-packs/common"
assert set(common["sections"]) == {"FIRST_AID", "KNOWLEDGE", "SAFETY"}
for path in common["sections"].values():
    assert path.startswith(common["root"] + "/")
    assert (ROOT / path).is_dir(), f"missing common section directory: {path}"

model_sections = catalog["modelSections"]
assert model_sections == {
    "ACCEPTANCE": "acceptance",
    "ATLAS": "atlas",
    "DIAGNOSTICS": "diagnostics",
    "TECHNICAL_DATA": "technical-data",
}

diagnostics = catalog["diagnostics"]
assert diagnostics["recommended"] == "diagnostics/recommended"
assert set(diagnostics["extended"]) == {
    "ARCHIVED_OFFICIAL",
    "MANUFACTURER_EXTENDED",
    "HISTORICAL_TRAINING",
    "FIELD_PRACTICE",
}

models = catalog["models"]
model_ids = [model["modelId"] for model in models]
assert len(model_ids) == len(set(model_ids)), "duplicate modelId in content catalog"

all_variants = set()
for model in models:
    family = model["family"]
    assert family in {"ELECTRIC", "DIESEL"}
    expected_prefix = "content-packs/electric/" if family == "ELECTRIC" else "content-packs/diesel/"
    assert model["root"].startswith(expected_prefix), (
        f"model root does not match family: {model['modelId']} -> {model['root']}"
    )

    variants = model["variantIds"]
    assert variants, f"model must declare at least one variant: {model['modelId']}"
    assert len(variants) == len(set(variants)), f"duplicate variant in {model['modelId']}"
    overlap = all_variants.intersection(variants)
    assert not overlap, f"variant reused across model blocks: {sorted(overlap)}"
    all_variants.update(variants)

    root = ROOT / model["root"]
    assert root.is_dir(), f"missing model root: {model['root']}"

    required_dirs = [
        root / "acceptance",
        root / "atlas",
        root / "diagnostics/recommended",
        root / "technical-data",
    ]
    required_dirs.extend(root / relative for relative in diagnostics["extended"].values())
    for path in required_dirs:
        assert path.is_dir(), f"missing model content boundary: {path.relative_to(ROOT)}"

print("CONTENT_TOPOLOGY_PASS")
