# VL80S diagnostic observation index

The legacy `Vl80sObservationCatalog` is migrated as a model-specific search index, not as a second equipment catalog.

## Ownership

- canonical equipment remains owned by the existing 89 `VL-EQ-*` Atlas entries;
- diagnostic scenarios remain owned by the existing 103 `vl80s.diag.*` cards;
- the observation index stores only search phrases and canonical references.

## Donor source

Pinned donor:

`DomEnota-maker/Test- @ 3176d6ee228f0ed4b78371f45209e10c1c724eff`

Source:

`Vl80sObservationCatalog.kt`

The source is compiled in a QA-only JVM module. Its git blob SHA is verified before export.

## Canonical mapping

Legacy scenario IDs are resolved only through the already migrated Stage6 `scenarioIdMap`.

Legacy equipment tokens are resolved only through `migrationRefs.legacyId` on the canonical 89-object equipment corpus.

No title-based or fuzzy mapping is allowed. Any missing or duplicate mapping fails the migration.

## Runtime semantics

Search preserves the donor behavior:

- trim query;
- lowercase;
- case-insensitive substring match over title, description and synonyms;
- blank query returns the full observation list.

The observation index has `actionAuthority = NONE`. A search hit can propose canonical content targets, but scenario visibility/execution still goes through `DiagnosticAccessPolicy`. The index therefore cannot bypass extended or candidate gates.
