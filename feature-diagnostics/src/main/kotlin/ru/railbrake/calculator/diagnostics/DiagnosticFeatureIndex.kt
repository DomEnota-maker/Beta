package ru.railbrake.calculator.diagnostics

import com.google.gson.Gson

data class DiagnosticFeatureIndex(
    val modelId: String,
    val publicationPolicy: String,
    val executableFlow: String,
) {
    init {
        require(modelId.isNotBlank())
        require(publicationPolicy.isNotBlank())
        require(executableFlow.isNotBlank())
        require(!executableFlow.startsWith("/") && ".." !in executableFlow) {
            "diagnostic runtime asset path is invalid"
        }
    }
}

class DiagnosticFeatureIndexJsonLoader(
    private val gson: Gson = Gson(),
) {
    fun parse(json: String): DiagnosticFeatureIndex {
        val raw = gson.fromJson(json, JsonDiagnosticFeatureIndex::class.java)
            ?: error("diagnostic feature index is empty")
        require(raw.schemaVersion == 1) {
            "unsupported diagnostic feature index schema"
        }
        return DiagnosticFeatureIndex(
            modelId = requireNotNull(raw.modelId).also { require(it.isNotBlank()) },
            publicationPolicy = requireNotNull(raw.publicationPolicy)
                .also { require(it.isNotBlank()) },
            executableFlow = requireNotNull(raw.executableFlow)
                .also { require(it.isNotBlank()) },
        )
    }
}

private data class JsonDiagnosticFeatureIndex(
    val schemaVersion: Int = 0,
    val modelId: String? = null,
    val publicationPolicy: String? = null,
    val executableFlow: String? = null,
)
