# Architecture bootstrap

This document records the first implementation step of the modular architecture migration.

## Implemented boundaries

The first buildable baseline intentionally contains only four Gradle modules:

- `app-shell` — Android application boundary and future composition root.
- `domain-contracts` — stable IDs, content types, applicability, links, provenance and package manifest contracts.
- `content-runtime` — package installation, canonical/alias registry, global link validation and typed target resolution.
- `source-policy` — fail-closed policy for actionable material.

The remaining target boundaries from the architecture specification are not collapsed into these modules. They will be introduced only when a migrated vertical slice needs them and the dependency can be measured.

## Non-negotiable rules

1. There is one canonical source tree. The old ZIP + patch + mirrored app layout is not restored.
2. Test is a donor/reference repository, not a tree to merge wholesale.
3. Existing canonical IDs are preserved when they are globally unique. Aliases are explicit and must be unambiguous.
4. Model/variant applicability fails closed when required context is missing.
5. Unknown or non-current source state cannot authorize an executable action.
6. Feature modules will navigate through typed content targets rather than importing each other.
7. The VL80S “Токоприёмник не поднимается” scenario remains a structural candidate until its separate golden-reference acceptance passes.

## Bootstrap acceptance

The bootstrap is accepted only if CI can:

- enumerate the declared Gradle modules;
- run domain/runtime/policy unit tests;
- run Android lint for the shell;
- assemble a debug APK from the new tree without any legacy ZIP/patch overlay.

## Next slice

After this bootstrap is green, the next migration step is the canonical content schema plus the first read-only VL80S package inventory. No diagnostic action graph is promoted to a golden reference until the Test candidate gate is green.
