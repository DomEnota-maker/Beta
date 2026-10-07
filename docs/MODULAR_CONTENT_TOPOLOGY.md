# Modular content topology

This document fixes the physical ownership model for RailBrakeCalculator content.

## 1. Common block

Content that does not belong to one locomotive model lives under:

- `content-packs/common/first-aid`
- `content-packs/common/knowledge`
- `content-packs/common/safety`

The common block must not own links into locomotive-specific equipment, diagnostics, acceptance or atlas objects.

## 2. Locomotive blocks

Locomotive content is divided first by family and then by model block.

### Electric

- `content-packs/electric/vl80s`
- `content-packs/electric/ermak`

### Diesel

- `content-packs/diesel/chme3-family`
- `content-packs/diesel/tem2-family`

Each model block owns its own:

- `acceptance/`
- `atlas/`
- `diagnostics/`
- `technical-data/`

A change to VL80S diagnostics therefore belongs to the VL80S diagnostic subtree and must not require editing the Ermak, CHME3 or TEM2 content trees.

## 3. Cross-feature links

Cross-feature links are allowed inside the same model block:

`diagnostics ↔ atlas ↔ acceptance ↔ technical/reference content`

A model-owned object may also link to genuinely common content.

Direct persisted links from one locomotive model to another locomotive model are forbidden.

## 4. Diagnostic separation

Each model diagnostic block is physically separated into:

- `diagnostics/recommended/`
- `diagnostics/extended/archived-official/`
- `diagnostics/extended/manufacturer-extended/`
- `diagnostics/extended/historical-training/`
- `diagnostics/extended/field-practice/`

The extended classification and the action permission are independent axes. For example, a FIELD_PRACTICE entry can still be INFORMATION_ONLY or PROHIBITED; being present in the extended corpus never makes an action permitted.

## 5. Assistant boundary

The assistant implementation does not live inside any locomotive content block.

By default, the assistant runtime uses:

`common + exactly one working model block`

An explicit user question about another model may query that model independently, but it does not create a persisted cross-model content link and does not silently replace the assistant's working model.

## 6. UI boundary

Feature UI is not stored separately per locomotive model. Model content supplies data and canonical targets; shared feature templates/design-system components determine how Acceptance, Atlas, Diagnostics and Technical Data are rendered.

This prevents model-specific content updates from silently creating a different UI implementation for one locomotive.
