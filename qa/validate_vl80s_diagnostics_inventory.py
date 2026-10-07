#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "content-packs/electric/vl80s/migration/diagnostics-inventory.json"

report = json.loads(PATH.read_text(encoding="utf-8"))
assert report["scenarioCount"] == 103
assert report["sourceSnapshot"]["commit"] == "3176d6ee228f0ed4b78371f45209e10c1c724eff"
assert report["sourceSnapshot"]["blobSha"] == "6ffdfdabf2f467dcdc4958768905f2b675ac0c26"
assert len(report["scenarios"]) == 103

ids = [item["id"] for item in report["scenarios"]]
assert all(isinstance(value, str) and value for value in ids)
assert len(ids) == len(set(ids))

titles = [item["title"] for item in report["scenarios"]]
assert all(isinstance(value, str) and value for value in titles)

print("VL80S_DIAGNOSTICS_INVENTORY_VALID", len(ids))
