#!/usr/bin/env python3
import gzip
import hashlib
import json
import urllib.request
from pathlib import Path

DONOR_REPO = "DomEnota-maker/Test-"
DONOR_SHA = "3176d6ee228f0ed4b78371f45209e10c1c724eff"

FILES = {
    "vl80s_acceptance.json.gz": "5174e8fa343bb4d5b61d4f541d4dffec2708127d",
    "vl80s_diagnostics.json.gz": "6ffdfdabf2f467dcdc4958768905f2b675ac0c26",
    "vl80s_electrical.json.gz": "6891068a9aeb92d6a33c445e1c7284f9a4c21ff9",
    "vl80s_electrical_variants.json.gz": "f9f7956f871cbb04d38b8315e1b902372d5f3f7b",
    "vl80s_equipment.json.gz": "a6d00459e18ca428601b4a0cd32ee95a7d1551e7",
    "vl80s_pneumatic.json.gz": "6adafce6307445d881a2f7cebde4837230431129",
    "vl80s_pneumatic_variants.json.gz": "3899e9e653f17c7b26e2c3d97a8852d4537a2537",
    "vl80s_variants.json.gz": "9a35df3df98173e7baa3edc05e4457c8f5eed6ea",
}

OUT = Path("build/vl80s-donor")
OUT.mkdir(parents=True, exist_ok=True)

def git_blob_sha(data: bytes) -> str:
    header = f"blob {len(data)}\0".encode()
    return hashlib.sha1(header + data).hexdigest()

def summarize(value):
    if isinstance(value, dict):
        summary = {"type": "object", "keys": sorted(value.keys())}
        for key, item in value.items():
            if isinstance(item, list):
                summary[f"{key}Count"] = len(item)
        return summary
    if isinstance(value, list):
        return {"type": "array", "count": len(value)}
    return {"type": type(value).__name__}

report = {
    "repository": DONOR_REPO,
    "commit": DONOR_SHA,
    "files": {},
}

for name, expected_sha in FILES.items():
    source_path = f"app/src/main/assets/technical/{name}"
    url = (
        f"https://raw.githubusercontent.com/{DONOR_REPO}/{DONOR_SHA}/"
        f"{source_path}"
    )
    print(f"Fetching {source_path}")
    with urllib.request.urlopen(url, timeout=60) as response:
        compressed = response.read()

    actual_sha = git_blob_sha(compressed)
    if actual_sha != expected_sha:
        raise SystemExit(
            f"Git blob SHA mismatch for {name}: expected {expected_sha}, got {actual_sha}"
        )

    decoded = gzip.decompress(compressed)
    data = json.loads(decoded.decode("utf-8"))
    output_name = name.removesuffix(".gz")
    output_path = OUT / output_name
    output_path.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    report["files"][name] = {
        "sourcePath": source_path,
        "expectedBlobSha": expected_sha,
        "verifiedBlobSha": actual_sha,
        "compressedBytes": len(compressed),
        "jsonBytes": len(decoded),
        "output": str(output_path),
        "summary": summarize(data),
    }

(OUT / "report.json").write_text(
    json.dumps(report, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)

print("VL80S_DONOR_EXPORT_PASS")
for name, info in report["files"].items():
    print(name, json.dumps(info["summary"], ensure_ascii=False))
