package ru.railbrake.calculator.diagnostics

import com.google.gson.Gson

enum class DiagnosticObservationKind {
    SIGNAL_LAMP,
    INSTRUMENT,
    SOUND,
    SMELL_OR_HEAT,
    BEHAVIOUR,
}

data class DiagnosticObservation(
    val id: String,
    val legacyId: String,
    val title: String,
    val kind: DiagnosticObservationKind,
    val description: String,
    val synonyms: List<String>,
    val scenarioIds: List<String>,
    val equipmentIds: List<String>,
    val confidence: String,
    val variantNote: String,
) {
    init {
        require(id.isNotBlank())
        require(legacyId.isNotBlank())
        require(title.isNotBlank())
        require(description.isNotBlank())
        require(scenarioIds.size == scenarioIds.distinct().size) {
            "duplicate scenario link in observation $id"
        }
        require(equipmentIds.size == equipmentIds.distinct().size) {
            "duplicate equipment link in observation $id"
        }
    }
}

data class DiagnosticObservationIndex(
    val schemaVersion: Int,
    val modelId: String,
    val sourceCommit: String,
    val observations: List<DiagnosticObservation>,
) {
    init {
        require(schemaVersion > 0)
        require(modelId.isNotBlank())
        require(sourceCommit.isNotBlank())
        require(observations.map { it.id }.distinct().size == observations.size) {
            "duplicate observation id"
        }
    }

    fun search(query: String): List<DiagnosticObservation> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return observations

        return observations.filter { observation ->
            buildList {
                add(observation.title)
                add(observation.description)
                addAll(observation.synonyms)
            }.joinToString(" ").lowercase().contains(normalized)
        }
    }
}

class DiagnosticObservationIndexJsonLoader(
    private val gson: Gson = Gson(),
) {
    fun parse(json: String): DiagnosticObservationIndex {
        val raw = gson.fromJson(json, JsonObservationDocument::class.java)
            ?: error("diagnostic observation index is empty")
        val source = requireNotNull(raw.sourceSnapshot) {
            "observation sourceSnapshot is required"
        }

        val observations = raw.observations.orEmpty().map { item ->
            DiagnosticObservation(
                id = requireObservationText(item.id, "observation.id"),
                legacyId = requireObservationText(
                    item.legacyId,
                    "observation.legacyId",
                ),
                title = requireObservationText(item.title, "observation.title"),
                kind = DiagnosticObservationKind.valueOf(
                    requireObservationText(item.kind, "observation.kind")
                ),
                description = requireObservationText(
                    item.description,
                    "observation.description",
                ),
                synonyms = item.synonyms.orEmpty(),
                scenarioIds = item.scenarioIds.orEmpty(),
                equipmentIds = item.equipmentIds.orEmpty(),
                confidence = requireObservationText(
                    item.confidence,
                    "observation.confidence",
                ),
                variantNote = requireObservationText(
                    item.variantNote,
                    "observation.variantNote",
                ),
            )
        }

        require(raw.observationCount == observations.size) {
            "observationCount does not match observation index"
        }

        return DiagnosticObservationIndex(
            schemaVersion = raw.schemaVersion,
            modelId = requireObservationText(raw.modelId, "observation.modelId"),
            sourceCommit = requireObservationText(
                source.commit,
                "observation.sourceSnapshot.commit",
            ),
            observations = observations,
        )
    }
}

private data class JsonObservationDocument(
    val schemaVersion: Int = 0,
    val modelId: String? = null,
    val sourceSnapshot: JsonObservationSource? = null,
    val observationCount: Int = 0,
    val observations: List<JsonObservation>? = null,
)

private data class JsonObservationSource(
    val commit: String? = null,
)

private data class JsonObservation(
    val id: String? = null,
    val legacyId: String? = null,
    val title: String? = null,
    val kind: String? = null,
    val description: String? = null,
    val synonyms: List<String>? = null,
    val scenarioIds: List<String>? = null,
    val equipmentIds: List<String>? = null,
    val confidence: String? = null,
    val variantNote: String? = null,
)

private fun requireObservationText(value: String?, field: String): String {
    require(!value.isNullOrBlank()) { "$field must not be blank" }
    return value
}
