#!/usr/bin/env python3
import hashlib
import json
import urllib.request
from pathlib import Path

DONOR_REPO = "DomEnota-maker/Test-"
DONOR_SHA = "3176d6ee228f0ed4b78371f45209e10c1c724eff"

FILES = {
    "Vl80sObservationCatalog.kt": {
        "path": "app/src/main/java/ru/railbrake/calculator/core/Vl80sObservationCatalog.kt",
        "blobSha": "a5d337adfda22161cc31779a93fdc04e18f5ad31",
    },
    "LocomotiveProfile.kt": {
        "path": "app/src/main/java/ru/railbrake/calculator/core/LocomotiveProfile.kt",
        "blobSha": "2c056af506ba5e88ca6367ecd08eb6fbd090bdf9",
    },
}

SRC_OUT = Path("build/vl80s-observation-donor-src/ru/railbrake/calculator/core")
REPORT_OUT = Path("build/vl80s-observation-donor")
SRC_OUT.mkdir(parents=True, exist_ok=True)
REPORT_OUT.mkdir(parents=True, exist_ok=True)


def git_blob_sha(content: bytes) -> str:
    header = f"blob {len(content)}\0".encode("utf-8")
    return hashlib.sha1(header + content).hexdigest()


report = {
    "repository": DONOR_REPO,
    "commit": DONOR_SHA,
    "files": {},
}

for output_name, spec in FILES.items():
    source_path = spec["path"]
    expected_sha = spec["blobSha"]
    url = (
        f"https://raw.githubusercontent.com/{DONOR_REPO}/{DONOR_SHA}/"
        f"{source_path}"
    )
    print(f"Fetching {source_path}")
    with urllib.request.urlopen(url, timeout=60) as response:
        source = response.read()

    actual_sha = git_blob_sha(source)
    if actual_sha != expected_sha:
        raise SystemExit(
            f"Git blob SHA mismatch for {output_name}: "
            f"expected {expected_sha}, got {actual_sha}"
        )

    target = SRC_OUT / output_name
    target.write_bytes(source)
    report["files"][output_name] = {
        "sourcePath": source_path,
        "expectedBlobSha": expected_sha,
        "verifiedBlobSha": actual_sha,
        "bytes": len(source),
        "output": str(target),
    }

(REPORT_OUT / "source-report.json").write_text(
    json.dumps(report, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)

print("VL80S_OBSERVATION_DONOR_SOURCE_PASS", len(FILES))
