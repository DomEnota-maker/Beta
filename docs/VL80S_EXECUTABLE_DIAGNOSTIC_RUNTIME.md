# VL80S executable diagnostic runtime migration

The canonical diagnostic catalog and Stage6 relation graph are not the executable decision flow.

The executable flow is exported from the pinned donor runtime:

`DomEnota-maker/Test- @ 3176d6ee228f0ed4b78371f45209e10c1c724eff`

Source of truth:

`DiagnosticRepository.scenarios`

The exporter compiles the pinned donor Kotlin sources in a QA-only JVM module and serializes the effective runtime after:

1. base + expansion + extended + completion + research integration;
2. `DiagnosticDeepening.enrich`;
3. repository-specific `enrichScenario`;
4. `DiagnosticUnknownRouting.enrich`.

For every question, YES / NO / UNKNOWN edges are exported through the donor's own `DiagnosticRepository.nextQuestion()`. This preserves explicit routing and legacy sequential fallback without reimplementing the donor algorithm.

The generator maps donor scenario IDs to canonical `vl80s.diag.*` IDs only through the already migrated Stage6 scenario map. Missing mappings fail the migration.

Generating an executable flow does **not** activate a diagnostic card. Catalog publication status remains `CANDIDATE` until feature/runtime acceptance is completed. The independent `vl80s.diag.pantograph-no-rise` golden-reference gate remains mandatory.


## New modular execution engine

`feature-diagnostics` owns the executable JSON loader and pure decision engine.

The engine:

- resolves scenarios only by canonical `vl80s.diag.*` ID;
- treats YES, NO and UNKNOWN as equal first-class responses;
- follows only exported effective edges;
- accumulates exported candidate-cause scoring;
- fails closed on missing branches, unknown question targets, unknown cause IDs, duplicate keys or unknown related scenarios;
- does not change catalog publication status.

Executable runtime availability and scenario publication remain separate gates. A `CANDIDATE` scenario cannot execute through normal application access even when its flow is technically loadable.
