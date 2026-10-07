# Typed cross-feature linking

The modular rebuild treats locomotive models as independent content blocks. Cross-feature links exist inside one locomotive model and from a model to truly common content.

## Core invariant

A model-specific feature must not import another feature merely to open its content.

Instead, model content emits a `ContentLink` containing a canonical target ID, a link type and an explicit scope. The shared runtime resolves the target and the navigation contract identifies the feature that owns the target content type.

Example inside VL80S:

```
VL80S diagnostics
  -> ContentLink(EQUIPMENT, VL80S canonicalId)
  -> registry / LinkResolver
  -> NavigationTarget(ATLAS, same VL80S canonicalId)
  -> app-shell navigation
```

The same mechanism supports VL80S Atlas → VL80S diagnostics, VL80S Acceptance → VL80S equipment, and analogous links inside Ermak, CHME3-family and TEM2-family packages.

## What is deliberately forbidden

There are no persisted direct content links between locomotive models.

Examples rejected by graph validation:

- VL80S diagnostics → Ermak equipment;
- Ermak Atlas → VL80S diagnostic scenario;
- common knowledge → a model-specific equipment object.

A user or assistant may explicitly request information about another locomotive model, but that is a separate query-routing operation against that model's package. It is not represented as a content link owned by the first model.

The assistant working model also remains separate from the model currently viewed in the UI.

## Link scopes

- `SAME_MODEL` — source and target belong to exactly the same locomotive model.
- `COMMON_TARGET` — a model-owned object links to content owned by the common block.

No cross-model link scope exists.

## Fail-closed checks

Global graph validation reports:

- missing target IDs;
- target content-type mismatch;
- any attempted cross-model link;
- any attempt by common content to own a link into a locomotive-specific package.

Runtime resolution additionally denies:

- links whose own applicability does not match current context;
- targets that do not match the viewed model/variant;
- content layers that are not enabled.

## Feature ownership

Neutral navigation mapping:

- diagnostic scenario → Diagnostics;
- equipment / atlas scheme → Atlas;
- acceptance item → Acceptance;
- knowledge / source → Reference;
- first aid → First Aid;
- safety → Safety.

This mapping is a navigation contract, not a direct Gradle dependency between future feature modules.

## Assistant boundary

The assistant is an independent technical module. It may index common content plus exactly one selected locomotive model by default.

If the user explicitly requests another model, the assistant may query that model's package for that request without creating or requiring a persisted link between the two locomotive packages.

## Architecture rule for future modules

Future `feature-diagnostics`, `feature-atlas`, `feature-acceptance`, `feature-reference`, `feature-first-aid` and `feature-safety` modules may emit canonical targets and consume navigation callbacks, but must not depend directly on one another.

The `app-shell` remains the composition root that performs actual screen navigation.
