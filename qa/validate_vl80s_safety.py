#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / "content-packs/electric/vl80s/safety/fire-safety.pack.json"
INDEX = ROOT / "content-packs/electric/vl80s/safety/index.json"
RUNTIME_INDEX = ROOT / "content-packs/electric/vl80s/runtime-index.json"

pack = json.loads(PACK.read_text(encoding="utf-8"))
index = json.loads(INDEX.read_text(encoding="utf-8"))
runtime = json.loads(RUNTIME_INDEX.read_text(encoding="utf-8"))

assert pack["manifest"]["modelIds"] == ["vl80s"]
assert pack["manifest"]["variantIds"] == []
assert set(pack["manifest"]["entries"]) == {
    "vl80s.safety.fire",
    "vl80s.source.fire-safety-rzd-2580r",
}

entries = {item["id"]: item for item in pack["entries"]}
safety = entries["vl80s.safety.fire"]
source = entries["vl80s.source.fire-safety-rzd-2580r"]

assert safety["type"] == "SAFETY"
assert safety["owner"] == {"kind": "MODEL", "modelId": "vl80s"}
assert safety["applicability"] == {"modelIds": ["vl80s"]}
assert safety["actionDisposition"] == "INFORMATION_ONLY"
assert safety["sourceStatus"] == "UNKNOWN"
assert safety["provenance"] == "UNKNOWN"
assert "vl80-fire-safety" in safety["aliases"]

assert source["type"] == "SOURCE"
assert source["owner"] == {"kind": "MODEL", "modelId": "vl80s"}
assert source["sourceStatus"] == "UNKNOWN"
assert source["actionDisposition"] == "INFORMATION_ONLY"

assert safety["links"] == [{
    "type": "SOURCE",
    "targetId": "vl80s.source.fire-safety-rzd-2580r",
    "role": "primary-source",
}]
assert safety["sourceRefs"] == ["vl80s.source.fire-safety-rzd-2580r"]

body = " ".join(safety["details"])
for marker in [
    "один длинный и два коротких",
    "ближе 2 м",
    "ближе 8 м",
    "не менее чем на 50 м",
]:
    assert marker in body, marker

assert index["modelId"] == "vl80s"
assert index["feature"] == "safety"
assert index["ownership"] == "MODEL_CONTENT_SHARED_FEATURE"
assert index["actionAuthority"] == "NONE"
assert index["sourceStatusPolicy"] == "FAIL_CLOSED_UNTIL_VERIFIED"
assert index["packs"] == ["electric/vl80s/safety/fire-safety.pack.json"]
assert index["entryIds"] == ["vl80s.safety.fire"]

assert "electric/vl80s/safety/fire-safety.pack.json" in runtime["packs"]
assert runtime["featureIndexes"]["safety"] == "electric/vl80s/safety/index.json"

print("VL80S_MODEL_SAFETY_PASS")
