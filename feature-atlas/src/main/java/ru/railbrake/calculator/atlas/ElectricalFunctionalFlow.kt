package ru.railbrake.calculator.atlas

import com.google.gson.Gson
import ru.railbrake.calculator.domain.CanonicalId
import ru.railbrake.calculator.domain.ContentTarget
import ru.railbrake.calculator.domain.ContentType

data class ElectricalFunctionalInfo(
    val title: String,
    val subtitle: String,
    val purpose: String,
    val triggeredBy: String,
    val affects: String,
    val links: String,
    val principle: String,
    val faultSigns: String,
    val checks: String,
)

data class ElectricalFunctionalNode(
    val id: String,
    val equipmentId: CanonicalId?,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val info: ElectricalFunctionalInfo,
) {
    fun equipmentTarget(): ContentTarget? =
        equipmentId?.let { ContentTarget(it, ContentType.EQUIPMENT) }
}

data class ElectricalFunctionalEdge(
    val from: String,
    val to: String,
    val activationStep: Int,
)

data class ElectricalFunctionalStep(
    val title: String,
    val description: String,
)

data class ElectricalFunctionalScenario(
    val id: String,
    val title: String,
    val summary: String,
    val canvasWidth: Float,
    val canvasHeight: Float,
    val nodes: List<ElectricalFunctionalNode>,
    val edges: List<ElectricalFunctionalEdge>,
    val steps: List<ElectricalFunctionalStep>,
) {
    init {
        require(canvasWidth > 0f && canvasHeight > 0f)
        require(nodes.isNotEmpty())
        require(steps.isNotEmpty())

        val nodeIds = nodes.map { it.id }
        require(nodeIds.distinct().size == nodeIds.size) {
            "electrical functional node ids must be unique"
        }
        val knownNodes = nodeIds.toSet()

        require(nodes.all { node ->
            node.x >= 0f &&
                node.y >= 0f &&
                node.x + node.width <= canvasWidth &&
                node.y + node.height <= canvasHeight
        }) {
            "electrical functional node is outside the declared canvas"
        }

        require(edges.all { edge ->
            edge.from in knownNodes &&
                edge.to in knownNodes &&
                edge.activationStep in steps.indices
        }) {
            "electrical functional edge is invalid"
        }
    }
}

data class ElectricalFunctionalFlowDocument(
    val id: String,
    val modelId: String,
    val title: String,
    val coordinateClaim: String,
    val styleAuthority: String,
    val actionAuthority: String,
    val disclaimer: String,
    val scenarios: List<ElectricalFunctionalScenario>,
) {
    init {
        require(scenarios.isNotEmpty())
        require(scenarios.map { it.id }.distinct().size == scenarios.size)
    }

    fun scenario(id: String): ElectricalFunctionalScenario? =
        scenarios.firstOrNull { it.id == id }
}

class ElectricalFunctionalFlowJsonLoader {
    private val gson = Gson()

    fun parse(json: String): ElectricalFunctionalFlowDocument {
        val raw = gson.fromJson(json, JsonElectricalFlowDocument::class.java)
            ?: error("electrical functional flow document is empty")
        val scenarios = raw.scenarios.orEmpty().map { it.toDomain() }

        require(raw.scenarioCount == scenarios.size) {
            "scenarioCount does not match electrical flow payload"
        }
        require(raw.stepCount == scenarios.sumOf { it.steps.size }) {
            "stepCount does not match electrical flow payload"
        }
        require(
            raw.uniqueCanonicalEquipmentLinkCount ==
                scenarios
                    .flatMap { it.nodes }
                    .mapNotNull { it.equipmentId }
                    .toSet()
                    .size
        ) {
            "uniqueCanonicalEquipmentLinkCount does not match payload"
        }

        return ElectricalFunctionalFlowDocument(
            id = requireElectricalText(raw.id, "flow.id"),
            modelId = requireElectricalText(raw.modelId, "flow.modelId"),
            title = requireElectricalText(raw.title, "flow.title"),
            coordinateClaim = requireElectricalText(
                raw.semantics?.coordinateClaim,
                "flow.semantics.coordinateClaim",
            ),
            styleAuthority = requireElectricalText(
                raw.semantics?.styleAuthority,
                "flow.semantics.styleAuthority",
            ),
            actionAuthority = requireElectricalText(
                raw.semantics?.actionAuthority,
                "flow.semantics.actionAuthority",
            ),
            disclaimer = requireElectricalText(
                raw.semantics?.disclaimer,
                "flow.semantics.disclaimer",
            ),
            scenarios = scenarios,
        )
    }
}

data class ElectricalFunctionalSession(
    val scenario: ElectricalFunctionalScenario,
    val stepIndex: Int = 0,
) {
    init {
        require(stepIndex in scenario.steps.indices)
    }

    val currentStep: ElectricalFunctionalStep
        get() = scenario.steps[stepIndex]

    val activeEdges: List<ElectricalFunctionalEdge>
        get() = scenario.edges.filter { it.activationStep <= stepIndex }

    val hasPrevious: Boolean
        get() = stepIndex > 0

    val hasNext: Boolean
        get() = stepIndex < scenario.steps.lastIndex

    fun previous(): ElectricalFunctionalSession =
        if (hasPrevious) copy(stepIndex = stepIndex - 1) else this

    fun next(): ElectricalFunctionalSession =
        if (hasNext) copy(stepIndex = stepIndex + 1) else this

    fun reset(): ElectricalFunctionalSession =
        copy(stepIndex = 0)
}

private data class JsonElectricalFlowDocument(
    val id: String? = null,
    val modelId: String? = null,
    val title: String? = null,
    val scenarioCount: Int = -1,
    val stepCount: Int = -1,
    val uniqueCanonicalEquipmentLinkCount: Int = -1,
    val semantics: JsonElectricalSemantics? = null,
    val scenarios: List<JsonElectricalScenario>? = null,
)

private data class JsonElectricalSemantics(
    val coordinateClaim: String? = null,
    val styleAuthority: String? = null,
    val actionAuthority: String? = null,
    val disclaimer: String? = null,
)

private data class JsonElectricalScenario(
    val id: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val canvas: JsonElectricalCanvas? = null,
    val nodes: List<JsonElectricalNode>? = null,
    val edges: List<JsonElectricalEdge>? = null,
    val steps: List<JsonElectricalStep>? = null,
) {
    fun toDomain() = ElectricalFunctionalScenario(
        id = requireElectricalText(id, "scenario.id"),
        title = requireElectricalText(title, "scenario.title"),
        summary = requireElectricalText(summary, "scenario.summary"),
        canvasWidth = requireNotNull(canvas) {
            "scenario.canvas is missing"
        }.width,
        canvasHeight = canvas.height,
        nodes = nodes.orEmpty().map { it.toDomain() },
        edges = edges.orEmpty().map { it.toDomain() },
        steps = steps.orEmpty().map { it.toDomain() },
    )
}

private data class JsonElectricalCanvas(
    val width: Float = -1f,
    val height: Float = -1f,
)

private data class JsonElectricalNode(
    val id: String? = null,
    val equipmentId: String? = null,
    val x: Float = -1f,
    val y: Float = -1f,
    val width: Float = -1f,
    val height: Float = -1f,
    val info: JsonElectricalInfo? = null,
) {
    fun toDomain() = ElectricalFunctionalNode(
        id = requireElectricalText(id, "node.id"),
        equipmentId = equipmentId
            ?.takeIf(String::isNotBlank)
            ?.let(::CanonicalId),
        x = x,
        y = y,
        width = width,
        height = height,
        info = requireNotNull(info) { "node.info is missing" }.toDomain(),
    )
}

private data class JsonElectricalInfo(
    val title: String? = null,
    val subtitle: String? = null,
    val purpose: String? = null,
    val triggeredBy: String? = null,
    val affects: String? = null,
    val links: String? = null,
    val principle: String? = null,
    val faultSigns: String? = null,
    val checks: String? = null,
) {
    fun toDomain() = ElectricalFunctionalInfo(
        title = requireElectricalText(title, "node.info.title"),
        subtitle = requireElectricalText(subtitle, "node.info.subtitle"),
        purpose = requireElectricalText(purpose, "node.info.purpose"),
        triggeredBy = requireElectricalText(triggeredBy, "node.info.triggeredBy"),
        affects = requireElectricalText(affects, "node.info.affects"),
        links = requireElectricalText(links, "node.info.links"),
        principle = principle.orEmpty(),
        faultSigns = faultSigns.orEmpty(),
        checks = checks.orEmpty(),
    )
}

private data class JsonElectricalEdge(
    val from: String? = null,
    val to: String? = null,
    val activationStep: Int = -1,
) {
    fun toDomain() = ElectricalFunctionalEdge(
        from = requireElectricalText(from, "edge.from"),
        to = requireElectricalText(to, "edge.to"),
        activationStep = activationStep,
    )
}

private data class JsonElectricalStep(
    val title: String? = null,
    val description: String? = null,
) {
    fun toDomain() = ElectricalFunctionalStep(
        title = requireElectricalText(title, "step.title"),
        description = requireElectricalText(description, "step.description"),
    )
}

private fun requireElectricalText(value: String?, field: String): String {
    require(!value.isNullOrBlank()) { "$field must not be blank" }
    return value
}
