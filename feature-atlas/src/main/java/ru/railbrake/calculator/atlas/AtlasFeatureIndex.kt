package ru.railbrake.calculator.atlas

import com.google.gson.Gson

data class AtlasFeatureIndex(
    val modelId: String,
    val layoutMaps: List<String>,
    val stepwiseFlows: List<String>,
    val functionalFlows: List<String>,
)

class AtlasFeatureIndexJsonLoader {
    private val gson = Gson()

    fun parse(json: String): AtlasFeatureIndex {
        val raw = gson.fromJson(json, JsonAtlasIndex::class.java)
            ?: error("atlas feature index is empty")

        val modelId = requireAtlasIndexText(raw.modelId, "atlasIndex.modelId")
        val layouts = raw.layoutMaps.orEmpty()
        require(layouts.isNotEmpty()) { "atlas feature index has no layout maps" }

        return AtlasFeatureIndex(
            modelId = modelId,
            layoutMaps = layouts,
            stepwiseFlows = raw.stepwiseFlows.orEmpty(),
            functionalFlows = raw.functionalFlows.orEmpty(),
        )
    }
}

private data class JsonAtlasIndex(
    val modelId: String? = null,
    val layoutMaps: List<String>? = null,
    val stepwiseFlows: List<String>? = null,
    val functionalFlows: List<String>? = null,
)

private fun requireAtlasIndexText(value: String?, field: String): String {
    require(!value.isNullOrBlank()) { "$field must not be blank" }
    return value
}
