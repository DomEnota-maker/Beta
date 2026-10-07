package ru.railbrake.calculator.runtime

import com.google.gson.Gson

data class ContentPackIndex(
    val schemaVersion: Int,
    val modelId: String,
    val packs: List<String>,
) {
    init {
        require(schemaVersion > 0) { "index schemaVersion must be positive" }
        require(modelId.isNotBlank()) { "index modelId must not be blank" }
        require(packs.isNotEmpty()) { "content pack index must not be empty" }
        require(packs.all { it.isNotBlank() }) { "content pack path must not be blank" }
        require(packs.size == packs.distinct().size) { "content pack paths must be unique" }
    }
}

class ContentPackIndexJsonLoader(
    private val gson: Gson = Gson(),
) {
    fun parse(json: String): ContentPackIndex {
        val raw = gson.fromJson(json, JsonIndex::class.java)
            ?: error("content pack index is empty")
        return ContentPackIndex(
            schemaVersion = raw.schemaVersion,
            modelId = requireText(raw.modelId, "index.modelId"),
            packs = raw.packs.orEmpty(),
        )
    }
}

private data class JsonIndex(
    val schemaVersion: Int = 0,
    val modelId: String? = null,
    val packs: List<String>? = null,
)

private fun requireText(value: String?, field: String): String {
    require(!value.isNullOrBlank()) { "$field must not be blank" }
    return value
}
