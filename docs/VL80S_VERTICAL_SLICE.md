# First VL80S vertical slice

This slice proves the new modular path using real donor content without promoting the diagnostic candidate to production.

## Canonical files

- `content-packs/electric/vl80s/atlas/pantograph.vertical.pack.json`
- `content-packs/electric/vl80s/technical-data/pantograph.vertical.pack.json`
- `content-packs/electric/vl80s/diagnostics/recommended/pantograph-no-rise.candidate.pack.json`

The Android application packages `content-packs/` directly as an asset source directory. There is no generated or maintained second copy under `app-shell`.

## Real migrated objects

The slice uses donor material pinned to Test commit `3176d6ee228f0ed4b78371f45209e10c1c724eff`.

Confirmed equipment identifiers:

- `VL-EQ-HV-002` — токоприёмник;
- `VL-EQ-PN-002` — клапан 245.

Two technical-reference records are migrated from the reference pack:

- `vl80s-panto-tech-001`;
- `vl80s-panto-tech-002`.

Their source references are preserved as metadata.

## Diagnostic safety boundary

The scenario shell `vl80s.diag.pantograph-no-rise` is present so the Atlas can hold a real typed relation to it.

Its status is `CANDIDATE`, not `ACTIVE`.

The runtime therefore knows that the target exists and validates the graph, but refuses to open it through normal navigation until it receives separate golden-reference acceptance.

No diagnostic graph or actionable procedure is promoted by this slice.

## Runtime path

The implemented path is:

`content-packs JSON → ContentPackJsonLoader → ContentRegistry → LinkResolver → NavigationTarget → shared design-system UI`

The application shell is the composition root.

## UI rule

No raw canonical IDs or source IDs are rendered to the user. The screen uses common Atlas / Technical Data templates and human-readable relation labels.

## Acceptance

CI must prove:

1. modular topology is valid;
2. the three slice packs have a closed same-model link graph;
3. candidate diagnostics remain non-published;
4. runtime/parser/link tests pass;
5. design-system and application lint pass;
6. debug APK assembles;
7. each canonical VL80S slice file exists exactly once in the APK;
8. no legacy `RailBrakeCalculator.zip` or patch asset path is packaged.
