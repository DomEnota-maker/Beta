# RailBrakeCalculator — modular rebuild

This branch is the clean baseline for the modular architecture migration.

## Rules

- The legacy `RailBrakeCalculator.zip + patch/ + app/ mirror` build layout is not carried forward.
- Repository `DomEnota-maker/Test-` is a donor/reference source, not a tree to copy wholesale.
- Content and code are migrated by explicit architectural boundaries and validated vertical slices.
- The VL80S scenario “Токоприёмник не поднимается” remains a structural candidate until its golden-reference acceptance is complete.
- Historical Beta states are preserved in archive branches and are not runtime dependencies.

## Preserved archive refs

- `archive/pre-modular-dev14-2026-10-07`
- `archive/ermak-batch6a-2026-10-07`
- `archive/ermak-safety-batch7a-2026-10-07`
- `archive/vl80s-diagnostic-audit-2026-10-07`

## Donor reference

Current Test reference at cleanup start:

- repository: `DomEnota-maker/Test-`
- branch: `feature/diagnostic-framework-v2`
- observed SHA: `3176d6ee228f0ed4b78371f45209e10c1c724eff`

The modular architecture specification is maintained separately and will be committed as part of the architecture bootstrap.
