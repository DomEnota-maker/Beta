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

/**
 * The underlay must be recovered from a verifiable source and aligned to
 * the educational overlay's original coordinate space.
 */
data class PneumaticFlowBackground(
    val assetPath: String,
    val sha256: String,
    val width: Int,
    val height: Int,
    val sourceRepository: String,
    val sourceCommit: String,
    val sourceArchive: String,
    val sourceArchiveBlobSha: String,
    val sourceEntry: String,
) {
    init {
        require(assetPath.isNotBlank() && !assetPath.startsWith("/") &&
            ".." !in assetPath) { "invalid pneumatic background path" }
        require(sha256.matches(Regex("[0-9a-f]{64}"))) {
            "invalid pneumatic background sha256"
        }
        require(width > 0 && height > 0)
        require(sourceRepository.isNotBlank() && sourceCommit.isNotBlank())
        require(sourceArchive.isNotBlank() && sourceArchiveBlobSha.isNotBlank())
        require(sourceEntry.isNotBlank())
    }
}

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
    val background: PneumaticFlowBackground? = null,
) {
    init {
        require(canvasWidth > 0f && canvasHeight > 0f)
        require(
            background == null ||
                (background.width.toFloat() == canvasWidth &&
                    background.height.toFloat() == canvasHeight)
        ) { "pneumatic background is not aligned to the flow coordinate space" }
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
            background = raw.semantics?.background?.toDomain(),
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
    val background: JsonPneumaticBackground? = null,
)

private data class JsonPneumaticBackground(
    val assetPath: String? = null,
    val sha256: String? = null,
    val width: Int = -1,
    val height: Int = -1,
    val source: JsonPneumaticSource? = null,
) {
    fun toDomain(): PneumaticFlowBackground {
        val provenance = requireNotNull(source) {
            "pneumatic background provenance is required"
        }
        return PneumaticFlowBackground(
            assetPath = requirePneumaticText(assetPath, "background.assetPath"),
            sha256 = requirePneumaticText(sha256, "background.sha256"),
            width = width,
            height = height,
            sourceRepository = requirePneumaticText(provenance.repository, "background.source.repository"),
            sourceCommit = requirePneumaticText(provenance.commit, "background.source.commit"),
            sourceArchive = requirePneumaticText(provenance.archive, "background.source.archive"),
            sourceArchiveBlobSha = requirePneumaticText(provenance.archiveBlobSha, "background.source.archiveBlobSha"),
            sourceEntry = requirePneumaticText(provenance.entry, "background.source.entry"),
        )
    }
}

private data class JsonPneumaticSource(
    val repository: String? = null,
    val commit: String? = null,
    val archive: String? = null,
    val archiveBlobSha: String? = null,
    val entry: String? = null,
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
