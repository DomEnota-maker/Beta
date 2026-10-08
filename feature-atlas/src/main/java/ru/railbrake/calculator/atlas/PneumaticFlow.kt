package ru.railbrake.calculator.atlas

import com.google.gson.Gson

enum class PneumaticRouteKind {
    FLOW,
    RELEASE,
    CONTROL,
}

data class PneumaticRoutePoint(
    val x: Float,
    val y: Float,
)

data class PneumaticRouteSegment(
    val kind: PneumaticRouteKind,
    val points: List<PneumaticRoutePoint>,
)

data class PneumaticFlowStep(
    val title: String,
    val description: String,
    val segments: List<PneumaticRouteSegment>,
)

data class PneumaticFlowMode(
    val id: String,
    val title: String,
    val summary: String,
    val start: String,
    val note: String?,
    val steps: List<PneumaticFlowStep>,
)

data class PneumaticFlowDocument(
    val id: String,
    val modelId: String,
    val title: String,
    val canvasWidth: Float,
    val canvasHeight: Float,
    val coordinateClaim: String,
    val actionAuthority: String,
    val disclaimer: String,
    val modes: List<PneumaticFlowMode>,
) {
    init {
        require(canvasWidth > 0f && canvasHeight > 0f)
        require(modes.isNotEmpty())
        require(modes.map { it.id }.distinct().size == modes.size)
        require(modes.all { it.steps.isNotEmpty() })
        require(
            modes
                .flatMap { it.steps }
                .flatMap { it.segments }
                .all { segment ->
                    segment.points.size >= 2 &&
                        segment.points.all { point ->
                            point.x in 0f..canvasWidth &&
                                point.y in 0f..canvasHeight
                        }
                }
        ) {
            "pneumatic route point is outside the declared canvas"
        }
    }

    fun mode(id: String): PneumaticFlowMode? =
        modes.firstOrNull { it.id == id }
}

class PneumaticFlowJsonLoader {
    private val gson = Gson()

    fun parse(json: String): PneumaticFlowDocument {
        val raw = gson.fromJson(json, JsonPneumaticFlowDocument::class.java)
            ?: error("pneumatic flow document is empty")
        val coordinateSpace = requireNotNull(raw.semantics?.coordinateSpace) {
            "pneumatic flow coordinateSpace is missing"
        }
        val modes = raw.modes.orEmpty().map { it.toDomain() }

        require(raw.modeCount == modes.size) {
            "modeCount does not match pneumatic mode payload"
        }

        return PneumaticFlowDocument(
            id = requirePneumaticText(raw.id, "flow.id"),
            modelId = requirePneumaticText(raw.modelId, "flow.modelId"),
            title = requirePneumaticText(raw.title, "flow.title"),
            canvasWidth = coordinateSpace.width,
            canvasHeight = coordinateSpace.height,
            coordinateClaim = requirePneumaticText(
                raw.semantics?.coordinateClaim,
                "flow.semantics.coordinateClaim",
            ),
            actionAuthority = requirePneumaticText(
                raw.semantics?.actionAuthority,
                "flow.semantics.actionAuthority",
            ),
            disclaimer = requirePneumaticText(
                raw.semantics?.disclaimer,
                "flow.semantics.disclaimer",
            ),
            modes = modes,
        )
    }
}

data class PneumaticFlowSession(
    val mode: PneumaticFlowMode,
    val stepIndex: Int = 0,
) {
    init {
        require(stepIndex in mode.steps.indices)
    }

    val currentStep: PneumaticFlowStep
        get() = mode.steps[stepIndex]

    val hasPrevious: Boolean
        get() = stepIndex > 0

    val hasNext: Boolean
        get() = stepIndex < mode.steps.lastIndex

    fun previous(): PneumaticFlowSession =
        if (hasPrevious) copy(stepIndex = stepIndex - 1) else this

    fun next(): PneumaticFlowSession =
        if (hasNext) copy(stepIndex = stepIndex + 1) else this

    fun reset(): PneumaticFlowSession =
        copy(stepIndex = 0)
}

private data class JsonPneumaticFlowDocument(
    val id: String? = null,
    val modelId: String? = null,
    val title: String? = null,
    val modeCount: Int = -1,
    val semantics: JsonPneumaticFlowSemantics? = null,
    val modes: List<JsonPneumaticMode>? = null,
)

private data class JsonPneumaticFlowSemantics(
    val actionAuthority: String? = null,
    val coordinateSpace: JsonPneumaticCoordinateSpace? = null,
    val coordinateClaim: String? = null,
    val disclaimer: String? = null,
)

private data class JsonPneumaticCoordinateSpace(
    val width: Float = -1f,
    val height: Float = -1f,
)

private data class JsonPneumaticMode(
    val id: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val start: String? = null,
    val note: String? = null,
    val steps: List<JsonPneumaticStep>? = null,
) {
    fun toDomain() = PneumaticFlowMode(
        id = requirePneumaticText(id, "mode.id"),
        title = requirePneumaticText(title, "mode.title"),
        summary = requirePneumaticText(summary, "mode.summary"),
        start = requirePneumaticText(start, "mode.start"),
        note = note?.takeIf(String::isNotBlank),
        steps = steps.orEmpty().map { it.toDomain() },
    )
}

private data class JsonPneumaticStep(
    val title: String? = null,
    val description: String? = null,
    val segments: List<JsonPneumaticSegment>? = null,
) {
    fun toDomain() = PneumaticFlowStep(
        title = requirePneumaticText(title, "step.title"),
        description = requirePneumaticText(description, "step.description"),
        segments = segments.orEmpty().map { it.toDomain() },
    )
}

private data class JsonPneumaticSegment(
    val kind: String? = null,
    val points: List<JsonPneumaticPoint>? = null,
) {
    fun toDomain() = PneumaticRouteSegment(
        kind = PneumaticRouteKind.valueOf(
            requirePneumaticText(kind, "segment.kind")
        ),
        points = points.orEmpty().map { it.toDomain() },
    )
}

private data class JsonPneumaticPoint(
    val x: Float = -1f,
    val y: Float = -1f,
) {
    fun toDomain() = PneumaticRoutePoint(x, y)
}


private fun requirePneumaticText(value: String?, field: String): String {
    require(!value.isNullOrBlank()) { "$field must not be blank" }
    return value
}
