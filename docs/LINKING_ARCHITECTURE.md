# Typed cross-feature linking

The modular rebuild treats features as independently owned blocks connected by typed links.

## Core invariant

A feature must not import another feature merely to open its content.

Instead, content emits a `ContentLink` that contains a canonical target ID, a link type and an explicit scope. The shared runtime resolves the target and the navigation contract identifies the feature that owns the target content type.

Conceptually:

```
diagnostics
  -> ContentLink(EQUIPMENT, canonicalId)
  -> registry / LinkResolver
  -> NavigationTarget(ATLAS, canonicalId)
  -> app-shell navigation
```

The same mechanism applies to Atlas → diagnostics, Acceptance → equipment, Knowledge → source, Assistant → canonical content target, and other supported links.

## Link scopes

- `SAME_OWNER` — default. A model-owned object can link to another object of the same model or to common content.
- `COMMON_TARGET` — the target must be owned by the common block.
- `EXPLICIT_CROSS_MODEL` — the source and target must be owned by different locomotive models. This is never inferred from a matching title or equipment name.

An explicit cross-model navigation changes only the viewed model in the target context. It does not change the user's active/working locomotive.

## Fail-closed checks

Global graph validation reports:

- missing target IDs;
- target content-type mismatch;
- accidental cross-model links that were not explicitly declared.

Runtime resolution additionally denies:

- links whose own applicability does not match the current context;
- targets that do not match model/variant applicability;
- content layers that are not enabled.

## Feature ownership

Current neutral navigation mapping:

- diagnostic scenario → Diagnostics;
- equipment / atlas scheme → Atlas;
- acceptance item → Acceptance;
- knowledge / source → Reference;
- first aid → First Aid;
- safety → Safety.

This mapping is a navigation contract, not a direct Gradle dependency between those future feature modules.

## Architecture rule for future modules

Future `feature-diagnostics`, `feature-atlas`, `feature-acceptance`, `feature-reference`, `feature-first-aid` and `feature-safety` modules may emit canonical targets and consume navigation callbacks, but must not depend directly on one another.

The `app-shell` remains the composition root that performs actual screen navigation.
