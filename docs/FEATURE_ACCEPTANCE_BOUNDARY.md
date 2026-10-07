# Shared Acceptance feature

Acceptance behavior is shared by all locomotive model blocks.

A locomotive content package owns acceptance items and routes. It does not own a private implementation of acceptance UI or state storage.

The shared feature owns:

- user-visible states: `Не проверено`, `Проверено`, `Замечание`, `Не применяется`;
- note persistence;
- session/checklist behavior as it is migrated;
- rendering through the shared design system.

State keys use canonical content IDs, so the same physical acceptance item can be referenced by a required route and a full-inspection route without duplicating its state.

This module must not depend on VL80S, Ermak, CHME3 or TEM2 content packages.


## Route engine

Model content may define route indexes that contain only route metadata and ordered canonical acceptance IDs.

The shared feature resolves those IDs through the common ContentRegistry and owns:

- route order;
- search over title, summary, search terms and structured blocks;
- progress summary;
- final route result;
- opening an individual item while preserving the same canonical state.

The same item may appear in multiple routes. Its state is not duplicated because persistence is keyed by canonical content ID.

Missing, non-acceptance or non-active route targets fail closed and invalidate the route.
