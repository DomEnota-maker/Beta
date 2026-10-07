# Shared Atlas feature

The Atlas is a shared feature implementation, not a private UI/runtime owned by each locomotive model.

Model packages provide:

- canonical equipment entries;
- scheme entries;
- typed graph nodes and edges;
- source/profile applicability metadata.

`feature-atlas` owns:

- graph parsing;
- validation of node targets and edge endpoints;
- later shared rendering/interaction behavior;
- node click routing through canonical equipment IDs.

A scheme graph may contain virtual nodes for junctions, atmosphere, contact network and other non-equipment graph points. Equipment nodes must resolve to a same-model canonical `EQUIPMENT` entry.

Electrical and pneumatic visuals may differ by scheme type, but locomotives do not provide separate UI implementations. Rendering rules remain shared.
