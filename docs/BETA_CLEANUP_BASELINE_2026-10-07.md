# Beta cleanup baseline — 2026-10-07

## Purpose

This commit intentionally replaces the old working tree with a minimal baseline for the modular rebuild while retaining Git history through the parent commit.

## Old Beta baseline

- `dev14-integration`: `a3c93873bd128d4769d004098981235c169fd085`
- legacy build mechanism: unpack `RailBrakeCalculator.zip`, overlay `patch/`, build resulting Android project
- `app/` and `patch/app/` contained mirrored source files and were guarded by CI `cmp` checks

## Comparison result against Test

At cleanup analysis time:

- Beta `dev14-integration`: 434 files
- Test `feature/diagnostic-framework-v2`: 738 files
- files existing only in Beta at the selected baseline: 0
- byte-identical same-path files: 407
- changed same-path files: 27
- Test-only files: 304

The principal Test additions include Diagnostic Framework v2, locomotive registry/working-locomotive context, assistant runtime, extended-emergency policy/runtime, CHME3 and TEM2-family assets, and expanded QA.

## Divergent Beta lines preserved separately

Two Ermak branches contain historical diagnostic work not represented as identical final assets in Test and therefore remain archived for later content-level comparison:

- `archive/ermak-batch6a-2026-10-07`
- `archive/ermak-safety-batch7a-2026-10-07`

The VL80S diagnostic-audit asset and audit documents were observed in Test, but the source branch is also preserved for traceability.

## Cleanup rule

No legacy source file is considered canonical merely because it existed in Beta. New files enter this branch only through the modular architecture migration.
