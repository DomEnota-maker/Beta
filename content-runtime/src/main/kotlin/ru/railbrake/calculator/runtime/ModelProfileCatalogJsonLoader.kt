package ru.railbrake.calculator.runtime

import com.google.gson.Gson
import ru.railbrake.calculator.domain.ModelId
import ru.railbrake.calculator.domain.ModelProfileBucket
import ru.railbrake.calculator.domain.ModelProfileCatalog
import ru.railbrake.calculator.domain.ModelProfileOverride
import ru.railbrake.calculator.domain.VariantId

class ModelProfileCatalogJsonLoader {
    private val gson = Gson()

    fun parse(json: String): ModelProfileCatalog {
        val raw = gson.fromJson(json, JsonProfileCatalog::class.java)
            ?: error("profile catalog is empty")

        return ModelProfileCatalog(
            modelId = ModelId(requireText(raw.profileId, "profileId")),
            resolution = requireText(raw.resolution, "resolution"),
            sectionAware = raw.sectionAware,
            defaultVariantId = VariantId(
                requireText(raw.defaultVariantId, "defaultVariantId")
            ),
            buckets = raw.physicalBuckets.orEmpty().map { it.toDomain() },
            experimentalOverrides = raw.experimentalOverrides.orEmpty()
                .map { it.toDomain() },
            nonMergeRules = raw.nonMergeRules.orEmpty().toSet(),
        )
    }
}

private data class JsonProfileCatalog(
    val profileId: String? = null,
    val resolution: String? = null,
    val sectionAware: Boolean = false,
    val defaultVariantId: String? = null,
    val physicalBuckets: List<JsonProfileBucket>? = null,
    val experimentalOverrides: List<JsonProfileOverride>? = null,
    val nonMergeRules: List<String>? = null,
)

private data class JsonProfileBucket(
    val id: String? = null,
    val displayTitle: String? = null,
    val range: String? = null,
    val confidence: String? = null,
    val features: List<String>? = null,
    val rules: List<String>? = null,
    val exceptions: List<String>? = null,
    val sourceTags: List<String>? = null,
) {
    fun toDomain() = ModelProfileBucket(
        id = VariantId(requireText(id, "bucket.id")),
        displayTitle = requireText(displayTitle, "bucket.displayTitle"),
        rangeLabel = requireText(range, "bucket.range"),
        confidence = requireText(confidence, "bucket.confidence"),
        features = features.orEmpty().toSet(),
        rules = rules.orEmpty().toSet(),
        exceptions = exceptions.orEmpty().toSet(),
        sourceTags = sourceTags.orEmpty().toSet(),
    )
}

private data class JsonProfileOverride(
    val range: String? = null,
    val feature: String? = null,
    val confidence: String? = null,
) {
    fun toDomain() = ModelProfileOverride(
        rangeLabel = requireText(range, "override.range"),
        feature = requireText(feature, "override.feature"),
        confidence = requireText(confidence, "override.confidence"),
    )
}

private fun requireText(value: String?, field: String): String {
    require(!value.isNullOrBlank()) { "$field must not be blank" }
    return value
}
