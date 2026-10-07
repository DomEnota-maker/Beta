# Shared UI / design-system boundary

RailBrakeCalculator uses one shared visual implementation per feature type. Locomotive model packages provide data; they do not provide their own UI implementations.

## Shared templates

The design system owns template identities for:

- Acceptance;
- Atlas;
- Diagnostics;
- Technical Data;
- Reference / knowledge;
- First Aid;
- Safety.

A VL80S Atlas item and a CHME3 Atlas item therefore resolve to the same `ATLAS` UI template. Differences in model data must not create a second Atlas screen implementation.

## Composition rule

The intended dependency direction is:

`model content -> typed content target -> feature module -> design-system`

and never:

`model content -> custom model-specific screen`

The app shell remains responsible for top-level navigation and composition.

## Initial scaffold

The first design-system implementation provides shared title/subtitle/content chrome without choosing model-specific colors, fonts or card variants. Feature-specific reusable components will be added as each real vertical slice is migrated.

This deliberately avoids copying the visual differences currently present between locomotive implementations in Test.
