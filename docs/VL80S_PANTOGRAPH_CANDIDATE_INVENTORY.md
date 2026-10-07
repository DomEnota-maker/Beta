# VL80S pantograph candidate inventory

This is an inventory record, not a content promotion.

Source snapshot:

- repository: `DomEnota-maker/Test-`
- branch: `feature/diagnostic-framework-v2`
- SHA: `3176d6ee228f0ed4b78371f45209e10c1c724eff`
- scenario: `pantograph-no-rise` — “Токоприёмник не поднимается”

Observed structure at the pinned SHA:

- 8 question nodes;
- 24 YES/NO/UNKNOWN response edges;
- 15 terminal edges and 9 continuing edges;
- 2 related systems;
- 5 related equipment/component IDs;
- reference pack: 19 entries and 12 registered sources;
- two separately restricted reference entries;
- three explicit conflict groups.

The Test reference pack itself declares `REFERENCE_ONLY_PENDING_HUMAN_REVIEW`. Therefore Beta records the source structure and identifiers but does not treat the scenario, its actions, thresholds or source interpretations as accepted production truth.

The migration status remains `CANDIDATE_NOT_RUNTIME` until the independent golden-reference acceptance is complete.
