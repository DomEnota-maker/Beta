package ru.railbrake.calculator.atlas

import com.google.gson.Gson
import ru.railbrake.calculator.domain.CanonicalId
import ru.railbrake.calculator.domain.ContentType
import ru.railbrake.calculator.runtime.ContentRegistry

data class AtlasNode(
    val id: String,
    val label: String,
    val equipmentId: CanonicalId? = null,
    val kind: String? = null,
    val domain: String? = null,
    val systemIds: Set<String> = emptySet(),
)

data class AtlasEdge(
    val fromId: String,
    val toId: String,
    val kind: String,
    val status: String? = null,
    val direction: String? = null,
    val note: String? = null,
)

data class AtlasSchemeGraph(
    val id: CanonicalId,
    val title: String,
    val schemeType: String,
    val status: String,
    val profiles: Set<String>,
    val systems: Set<String>,
    val sourceRefs: Set<String>,
    val scopeNote: String?,
    val nodes: List<AtlasNode>,
    val edges: List<AtlasEdge>,
)

data class AtlasPack(
    val schemes: List<AtlasSchemeGraph>,
)

class AtlasSchemeJsonLoader(
    private val gson: Gson = Gson(),
) {
    fun parse(json: String): AtlasPack {
        val document = gson.fromJson(json, JsonAtlasPack::class.java)
            ?: error("atlas pack document is empty")

        return AtlasPack(
            schemes = document.schemes.orEmpty().map { it.toDomain() },
        )
    }
}

data class AtlasGraphIssue(
    val schemeId: CanonicalId,
    val nodeId: String,
    val message: String,
)

class AtlasGraphValidator(
    private val registry: ContentRegistry,
) {
    fun validate(pack: AtlasPack): List<AtlasGraphIssue> =
        pack.schemes.flatMap { scheme ->
            val nodeIds = scheme.nodes.map { it.id }.toSet()
            buildList {
                for (node in scheme.nodes) {
                    val equipmentId = node.equipmentId ?: continue
                    val entry = registry.find(equipmentId)
                    when {
                        entry == null -> add(
                            AtlasGraphIssue(
                                schemeId = scheme.id,
                                nodeId = node.id,
                                message = "missing equipment target: ${equipmentId.value}",
                            )
                        )
                        entry.type != ContentType.EQUIPMENT -> add(
                            AtlasGraphIssue(
                                schemeId = scheme.id,
                                nodeId = node.id,
                                message = "target is not equipment: ${equipmentId.value}",
                            )
                        )
                    }
                }

                for (edge in scheme.edges) {
                    if (edge.fromId !in nodeIds) {
                        add(
                            AtlasGraphIssue(
                                schemeId = scheme.id,
                                nodeId = edge.fromId,
                                message = "edge source is not declared as a node",
                            )
                        )
                    }
                    if (edge.toId !in nodeIds) {
                        add(
                            AtlasGraphIssue(
                                schemeId = scheme.id,
                                nodeId = edge.toId,
                                message = "edge target is not declared as a node",
                            )
                        )
                    }
                }
            }
        }
}

private data class JsonAtlasPack(
    val schemes: List<JsonScheme>? = null,
)

private data class JsonScheme(
    val id: String? = null,
    val title: String? = null,
    val schemeType: String? = null,
    val status: String? = null,
    val profiles: List<String>? = null,
    val systems: List<String>? = null,
    val sourceRefs: List<Any>? = null,
    val scopeNote: String? = null,
    val nodes: List<JsonNode>? = null,
    val edges: List<JsonEdge>? = null,
) {
    fun toDomain(): AtlasSchemeGraph {
        val mappedNodes = nodes.orEmpty().map { it.toDomain() }
        return AtlasSchemeGraph(
            id = CanonicalId(requireText(id, "scheme.id")),
            title = requireText(title, "scheme.title"),
            schemeType = requireText(schemeType, "scheme.schemeType"),
            status = status ?: "UNKNOWN",
            profiles = profiles.orEmpty().toSet(),
            systems = systems.orEmpty().toSet(),
            sourceRefs = sourceRefs.orEmpty().mapNotNull(::sourceRefId).toSet(),
            scopeNote = scopeNote,
            nodes = mappedNodes,
            edges = edges.orEmpty().map { it.toDomain() },
        )
    }
}

private data class JsonNode(
    val id: String? = null,
    val equipmentId: String? = null,
    val virtualNodeId: String? = null,
    val label: String? = null,
    val kind: String? = null,
    val domain: String? = null,
    val systemIds: List<String>? = null,
) {
    fun toDomain(): AtlasNode {
        val equipment = equipmentId?.takeIf { it.isNotBlank() }?.let(::CanonicalId)
        val resolvedId = equipment?.value
            ?: virtualNodeId?.takeIf { it.isNotBlank() }
            ?: id?.takeIf { it.isNotBlank() }
            ?: error("atlas node requires id, equipmentId or virtualNodeId")

        return AtlasNode(
            id = resolvedId,
            label = label?.takeIf { it.isNotBlank() } ?: resolvedId,
            equipmentId = equipment,
            kind = kind,
            domain = domain,
            systemIds = systemIds.orEmpty().toSet(),
        )
    }
}

private data class JsonEdge(
    val fromId: String? = null,
    val toId: String? = null,
    val from: String? = null,
    val to: String? = null,
    val kind: String? = null,
    val relationType: String? = null,
    val status: String? = null,
    val direction: String? = null,
    val note: String? = null,
) {
    fun toDomain() = AtlasEdge(
        fromId = requireText(fromId ?: from, "edge.from"),
        toId = requireText(toId ?: to, "edge.to"),
        kind = requireText(kind ?: relationType, "edge.kind"),
        status = status,
        direction = direction,
        note = note,
    )
}

private fun sourceRefId(value: Any): String? = when (value) {
    is String -> value.takeIf { it.isNotBlank() }
    is Map<*, *> -> value["sourceId"]?.toString()?.takeIf { it.isNotBlank() }
    else -> null
}

private fun requireText(value: String?, field: String): String {
    require(!value.isNullOrBlank()) { "$field must not be blank" }
    return value
}
