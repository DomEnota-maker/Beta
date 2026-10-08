# VL80S remaining migration audit

Pinned donor: `DomEnota-maker/Test-` @ `3176d6ee228f0ed4b78371f45209e10c1c724eff`.

This audit records what remains after the canonical VL80S equipment, technical-data, acceptance, scheme and diagnostic-catalog migrations.

## Already migrated into canonical modular ownership

- 89 equipment objects in Atlas packs.
- 89 corresponding equipment technical-data entries.
- section-aware VL80S physical profile catalog.
- 8 electrical and 8 pneumatic schemes.
- 81 acceptance items and 8 acceptance routes.
- 103 diagnostic catalog entries:
  - 44 recommended;
  - 59 extended / `SUPPLEMENTAL_OPERATIONAL`.
- Stage6 diagnostic cross-feature overlay as `relation-graph.json`.
- six reference/normal-value records from `Vl80sNormalValues.kt`.
- shared feature boundaries for Atlas, Acceptance and Diagnostics.

## Diagnostics: still not migrated as executable flow

The 1,181 Stage6 edges are relationship metadata, not the interactive YES/NO/UNKNOWN decision flow.

The donor itself says that Stage6 does not replace Stage1–5 source-of-truth data and identifies `DiagnosticRepository.scenarios` as the interactive UI source.

Therefore all 103 migrated diagnostic cards remain `CANDIDATE`.

Remaining work:

1. export the actual enriched `DiagnosticRepository.scenarios` runtime from the pinned donor;
2. preserve question keys, YES/NO/UNKNOWN routing, candidate causes, checks and safety boundaries;
3. compare the exported runtime against the 103 canonical catalog cards;
4. introduce executable-flow validation in `feature-diagnostics`;
5. activate only accepted scenarios;
6. keep `vl80s.diag.pantograph-no-rise` gated until its independent GOLDEN REFERENCE acceptance passes.

## Variant overlays

Electrical and pneumatic variant policy assets are migrated, but the overlay engine is not yet treated as implemented.

Do not claim section-specific rendered schemes until the runtime applies the policy and passes validation.

## Legacy KnowledgeRepository material

### `vl80-layout`

Unique value: interactive top-view equipment layout.

Status: preserve concept, do not migrate as a generic Knowledge article.

The current Test source references `R.drawable.vl80s_layout_section1`, but that resource is not present in the pinned Test Git tree. Recover the actual image/resource from a verified historical source before rebuilding the clickable layout.

Hotspot behavior should ultimately resolve to canonical equipment IDs instead of generating duplicate `vl80-detail-*` articles.

### `vl80-pneumatic-groups`

Mostly overlaps canonical pneumatic equipment and scheme data.

Do not create a second equipment catalog. Any useful explanatory text should be absorbed into model technical data or scheme presentation.

### `vl80-service-brake-route`

Unique value: seven-step educational air route.

This matches the desired step-through pneumatic experience and should be migrated as an Atlas/pneumatic flow, not as a Knowledge article.

### `vl80-pneumatic-simulator`

Legacy presentation of four pneumatic modes.

Do not copy its old UI. Compare its four learning flows against the new pneumatic scheme/state packs and preserve only information that is not already represented.

### `vl80-electrical-simulator`

Legacy functional electrical trainer.

Do not copy its old UI or duplicate its data. The new electrical scheme packs + shared Atlas feature are the target implementation.

### `vl80-fire-safety`

Contains model-specific operational fire-safety material and is not duplicated by the generic equipment catalog.

Ownership still requires an explicit architecture decision: keep a model-specific safe-content representation linked from VL80S, while avoiding a second independent implementation of the common Safety feature.

Do not silently move this article into global Common content or into Technical Data.

### generated `vl80-detail-*`

These are legacy generated equipment detail articles.

Do not migrate them. Canonical Atlas/Technical Data equipment cards replace them.

## Vl80sObservationCatalog

EquipmentReference entries overlap the new 89-object canonical equipment catalog and must not be migrated as a second catalog.

The observation layer itself remains useful: “what I see / hear / smell / measure” search phrases can become a model-specific diagnostic observation index used by Diagnostics and Assistant.

Before migration, map every old observation equipment token to a unique canonical equipment ID and every legacy scenario ID to its canonical `vl80s.diag.*` ID.

## Migration rule

No remaining legacy VL80S class is copied merely because it exists.

For every remaining source choose one of:

- canonical data migration;
- feature/runtime migration;
- absorb into an already migrated canonical object;
- superseded legacy presentation — do not migrate;
- unresolved ownership — keep gated until explicitly classified.


## Variant policy resolver update

The shared `feature-atlas` now has a section-aware variant policy resolver for the migrated electrical and pneumatic policy documents.

It resolves profile features and explicit equipment facts, selects applicable overlays, and fails closed for exact-detail rendering when the donor policy requires an actual/primary section drawing. The fire-signalization 2110/2210 conflict remains explicit-only and is never inferred from serial range.

This closes the policy-resolution part of the scheme migration. It does **not** invent missing contact-level replacement fragments; exact graphical fragment substitution remains gated by actual source material.


## VL80S top-view atlas migration

The legacy `vl80LayoutHotspots` content from the pinned Test snapshot is now represented as model-owned presentation data under `atlas/interactive/layout-hotspots.json`.

- 15 normalized training-reference hotspots are preserved.
- Exact canonical equipment links are added only where the legacy hotspot ID matches migrated equipment unambiguously.
- Aggregate zones such as VVK/BSA/cab areas remain layout hotspots and are not invented as equipment entities.
- The shared `feature-atlas` owns hit-testing and typed equipment navigation.


## VL80S stepwise pneumatic migration

The legacy VL80S pneumatic trainer is now represented as model-owned presentation data instead of hardcoded UI state.

- Four legacy modes are preserved: charging, service braking, release and auxiliary braking.
- Step counts are preserved exactly as 4 / 7 / 4 / 4.
- Step text comes from the pinned `KnowledgeRepository.kt` donor.
- Route coordinates come from the pinned `KnowledgeBaseScreen.kt` donor.
- Coordinates are explicitly presentation overlays, not exact pipe geometry.
- The asset has no action authority and remains a training/functional flow.
- The shared `feature-atlas` owns loading and step navigation.


## VL80S electrical functional-flow migration

The five legacy VL80S electrical trainer scenarios are now model-owned data instead of hardcoded Compose state:

- Тяга — 5 steps;
- Подъём ТП — 6 steps;
- Вспомогательные — 4 steps;
- Реостатный тормоз — 5 steps;
- Защита — 4 steps.

The migration preserves 50 node instances, 48 functional edges and 24 steps from the pinned Test source. Only exact legacy-id matches are linked to canonical equipment; unmatched legacy nodes remain virtual functional nodes.

Legacy per-scenario colors are retained only as migration metadata. Rendering style authority belongs to the shared design system.
