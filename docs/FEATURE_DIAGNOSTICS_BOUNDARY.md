# Shared Diagnostics feature

Diagnostics is a shared runtime/UI feature. Locomotive model packages own diagnostic data; they do not own separate diagnostic engines or separate visual implementations.

## Two independent gates

A scenario has two distinct states:

1. catalog visibility;
2. executable runtime availability.

A visible card is not automatically executable.

This is required because migrated content may already have stable IDs and cross-feature relations while its interactive flow is still awaiting migration or acceptance.

## Recommended and extended corpora

Recommended content belongs to the STANDARD layer.

Extended content is hidden by default and requires:

- the global extended-content setting;
- explicit enablement of its extended class.

For VL80S the first migrated extended class is `SUPPLEMENTAL_OPERATIONAL`. It represents the old `DiagnosticExtendedCatalog.kt`: additional observed-failure localization cards with explicit safety boundaries. It is not reclassified as historical, archived official or field practice.

## Relation graph is not an executable flow

The pinned VL80S donor contains 1,181 Stage6 edges. They are a canonical cross-link overlay between diagnostics, equipment, acceptance and other technical objects.

The donor explicitly states that this overlay does not replace Stage1–5 source-of-truth data and that the interactive UI source was `DiagnosticRepository.scenarios`.

Therefore:

- `relation-graph.json` is navigation/relationship metadata;
- it must never be interpreted as YES/NO/UNKNOWN diagnostic flow;
- `runtimePayloadStatus = RELATION_GRAPH_ONLY_NOT_EXECUTABLE` resolves to `RELATION_GRAPH_ONLY`;
- every current migrated VL80S diagnostic scenario remains `CANDIDATE`.

## Activation rule

A scenario may execute only when both are true:

- its publication status is `ACTIVE`;
- its model diagnostic index reports `EXECUTABLE_FLOW_AVAILABLE`.

The golden-reference candidate `vl80s.diag.pantograph-no-rise` remains non-executable until its separate acceptance gate passes.

## Model isolation

The feature consumes a selected model's diagnostic index. It does not import VL80S, Ermak, CHME3 or TEM2 content packages and it does not create cross-model content links.
