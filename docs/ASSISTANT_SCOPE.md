# Assistant module boundary

The assistant is an independent technical module and does not belong to any locomotive package.

## Default query scope

Normal assistant operation resolves to:

`Common + exactly one working locomotive model`

There is deliberately no `ALL_MODELS` query/index mode in the assistant scope contract.

## Explicit other-model query

If the user explicitly asks about another locomotive model, the assistant may resolve that request against:

`Common + explicitly requested model`

This does not mutate the stored working model.

Example:

- working model: VL80S;
- explicit question: “How is this arranged on Ermak?”;
- query scope: Common + Ermak;
- working model after the request: still VL80S.

## Separation from content links

Assistant query routing is not a persisted cross-model content link.

VL80S content never needs a stored link to Ermak content for the assistant to answer an explicit Ermak question.

## Dependency rule

`assistant-core` depends on shared contracts. It must not import feature-diagnostics, feature-atlas, feature-acceptance or a specific locomotive content package.
