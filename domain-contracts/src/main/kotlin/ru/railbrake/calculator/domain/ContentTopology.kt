package ru.railbrake.calculator.domain

enum class LocomotiveFamily {
    ELECTRIC,
    DIESEL,
}

enum class ModelSection {
    ACCEPTANCE,
    ATLAS,
    DIAGNOSTICS,
    TECHNICAL_DATA,
}

enum class CommonSection {
    FIRST_AID,
    KNOWLEDGE,
    SAFETY,
}

enum class DiagnosticCorpus {
    RECOMMENDED,
    EXTENDED,
}

enum class ExtendedDiagnosticClass {
    ARCHIVED_OFFICIAL,
    MANUFACTURER_EXTENDED,
    HISTORICAL_TRAINING,
    FIELD_PRACTICE,
}

data class DiagnosticPlacement(
    val corpus: DiagnosticCorpus,
    val extendedClass: ExtendedDiagnosticClass? = null,
) {
    init {
        require(
            (corpus == DiagnosticCorpus.RECOMMENDED && extendedClass == null) ||
                (corpus == DiagnosticCorpus.EXTENDED && extendedClass != null)
        ) {
            "extendedClass must exist only for EXTENDED diagnostic corpus"
        }
    }
}

data class ModelBlockDescriptor(
    val modelId: ModelId,
    val family: LocomotiveFamily,
    val variantIds: Set<VariantId>,
    val rootPath: String,
) {
    init {
        require(rootPath.isNotBlank()) { "rootPath must not be blank" }
    }
}


data class ModelProfileBucket(
    val id: VariantId,
    val displayTitle: String,
    val rangeLabel: String,
    val confidence: String,
    val features: Set<String> = emptySet(),
    val rules: Set<String> = emptySet(),
    val exceptions: Set<String> = emptySet(),
    val sourceTags: Set<String> = emptySet(),
) {
    init {
        require(displayTitle.isNotBlank()) { "profile display title must not be blank" }
        require(rangeLabel.isNotBlank()) { "profile range label must not be blank" }
        require(confidence.isNotBlank()) { "profile confidence must not be blank" }
    }
}

data class ModelProfileOverride(
    val rangeLabel: String,
    val feature: String,
    val confidence: String,
) {
    init {
        require(rangeLabel.isNotBlank())
        require(feature.isNotBlank())
        require(confidence.isNotBlank())
    }
}

data class ModelProfileCatalog(
    val modelId: ModelId,
    val resolution: String,
    val sectionAware: Boolean,
    val defaultVariantId: VariantId,
    val buckets: List<ModelProfileBucket>,
    val experimentalOverrides: List<ModelProfileOverride> = emptyList(),
    val nonMergeRules: Set<String> = emptySet(),
) {
    init {
        require(resolution.isNotBlank()) { "profile resolution must not be blank" }
        require(buckets.isNotEmpty()) { "profile catalog must contain buckets" }
        require(buckets.map { it.id }.distinct().size == buckets.size) {
            "profile bucket ids must be unique"
        }
        require(buckets.any { it.id == defaultVariantId }) {
            "default variant must exist in profile catalog"
        }
    }

    fun bucket(id: VariantId): ModelProfileBucket? =
        buckets.firstOrNull { it.id == id }
}

sealed interface ModelProfileSelectionResult {
    data class Selected(val bucket: ModelProfileBucket) : ModelProfileSelectionResult
    data object UnknownVariant : ModelProfileSelectionResult
}

fun ModelProfileCatalog.select(id: VariantId): ModelProfileSelectionResult =
    bucket(id)?.let(ModelProfileSelectionResult::Selected)
        ?: ModelProfileSelectionResult.UnknownVariant
