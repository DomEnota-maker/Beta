# Independent application blocks

The modular rebuild has two additional top-level functional blocks that are not locomotive content packs.

## Calculations

`feature-calculations` owns locomotive-independent calculation logic.

The first migrated engine is the existing Test `BrakeCalculator`, including:

- mass / axle-load calculation;
- supplement by manual-brake axles;
- Appendix 12 calculation;
- wind, oily-rail and low-slope rules already present in Test.

It does not depend on VL80S, Ermak, CHME3 or TEM2 model packages.

Calculation UI will use the shared design system when migrated.

## History

`feature-history` owns the user-facing calculation journal contract.

The existing record shape from Test is preserved:

- `timestampMillis`;
- `mode`;
- `title`;
- `summary`;
- `details`.

History is not locomotive content and is not stored in `content-packs/common`.

The old Test application already persists history. The modular rebuild must recover and verify that legacy storage format before replacing the storage implementation. Until that migration is verified, the new module exposes contracts only and does not write a new incompatible history format.

## Relationship

Calculations may emit a `HistoryRecord` to a history boundary supplied by the app shell. History does not own calculation formulas, and Calculations does not own persistence.

Both are top-level application modules alongside Common, Assistant, model blocks and shared UI infrastructure.
