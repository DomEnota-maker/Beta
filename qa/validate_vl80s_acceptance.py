#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL_ROOT = ROOT / "content-packs/electric/vl80s"
ACCEPTANCE_ROOT = MODEL_ROOT / "acceptance"
EQUIPMENT_ROOT = MODEL_ROOT / "atlas/equipment"

items_doc = json.loads((ACCEPTANCE_ROOT / "items.pack.json").read_text(encoding="utf-8"))
required_doc = json.loads((ACCEPTANCE_ROOT / "required.pack.json").read_text(encoding="utf-8"))
index = json.loads((ACCEPTANCE_ROOT / "index.json").read_text(encoding="utf-8"))
report = json.loads((MODEL_ROOT / "migration/acceptance-report.json").read_text(encoding="utf-8"))
runtime_index = json.loads((MODEL_ROOT / "runtime-index.json").read_text(encoding="utf-8"))

assert not (ACCEPTANCE_ROOT / "pantograph.vertical.pack.json").exists()

items = items_doc["entries"]
required = required_doc["entries"]
assert len(items) == 66, len(items)
assert len(required) == 15, len(required)
assert report["donorItemCount"] == 66
assert report["requiredItemCount"] == 15
assert report["totalItemCount"] == 81
assert report["donorRouteCount"] == 7
assert report["runtimeRouteCount"] == 8

assert all(entry["id"].startswith("VL80-ACC-") for entry in items)
assert all(entry["id"].startswith("VL80-REQ-") for entry in required)
all_entries = {entry["id"]: entry for entry in items + required}
assert len(all_entries) == 81

assert index["modelId"] == "vl80s"
assert index["sourceSnapshot"]["commit"] == "3176d6ee228f0ed4b78371f45209e10c1c724eff"
assert index["sourceSnapshot"]["blobSha"] == "5174e8fa343bb4d5b61d4f541d4dffec2708127d"
assert len(index["routes"]) == 8

required_route = next(route for route in index["routes"] if route["id"] == "VL80-ROUTE-required")
assert len(required_route["sequence"]) == 15
assert set(required_route["sequence"]) == {entry["id"] for entry in required}

canonical = next(
    route for route in index["routes"]
    if route.get("sourceRouteId") == "route_canonical"
)
assert len(canonical["sequence"]) == 81
assert set(canonical["sequence"]) == set(all_entries)

for route in index["routes"]:
    assert route["sequence"]
    assert len(route["sequence"]) == len(set(route["sequence"]))
    assert set(route["sequence"]).issubset(all_entries)

equipment = {}
for path in sorted(EQUIPMENT_ROOT.glob("*.pack.json")):
    document = json.loads(path.read_text(encoding="utf-8"))
    for entry in document["entries"]:
        equipment[entry["id"]] = entry
assert len(equipment) == 89

acceptance_links = 0
for entry in all_entries.values():
    assert entry["type"] == "ACCEPTANCE_ITEM"
    assert entry["owner"] == {"kind": "MODEL", "modelId": "vl80s"}
    assert entry["publicationStatus"] == "ACTIVE"
    assert all(link["type"] != "RELATED_SCENARIO" for link in entry.get("links", []))
    for link in entry.get("links", []):
        if link["type"] == "EQUIPMENT":
            acceptance_links += 1
            equipment_id = link["targetId"]
            assert equipment_id in equipment, (entry["id"], equipment_id)
            assert any(
                reverse["type"] == "ACCEPTANCE_ITEM"
                and reverse["targetId"] == entry["id"]
                for reverse in equipment[equipment_id]["links"]
            ), (equipment_id, entry["id"])

req03 = all_entries["VL80-REQ-03"]
assert req03["title"] == "Крышевое оборудование и токоприёмник"
assert req03["summary"] == (
    "Осмотреть с земли крышевое оборудование и проверить токоприёмник "
    "в установленном безопасном порядке."
)
assert any(
    link["type"] == "EQUIPMENT" and link["targetId"] == "VL-EQ-HV-002"
    for link in req03["links"]
)

runtime_packs = set(runtime_index["packs"])
assert "electric/vl80s/acceptance/required.pack.json" in runtime_packs
assert "electric/vl80s/acceptance/items.pack.json" in runtime_packs
assert "electric/vl80s/acceptance/pantograph.vertical.pack.json" not in runtime_packs
assert runtime_index["featureIndexes"]["acceptance"] == (
    "electric/vl80s/acceptance/index.json"
)

print(
    "VL80S_ACCEPTANCE_MIGRATION_PASS",
    len(items),
    len(required),
    len(index["routes"]),
    acceptance_links,
    report["pendingDiagnosticHintCount"],
)
