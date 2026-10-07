package ru.railbrake.calculator.domain

@JvmInline
value class CanonicalId(val value: String) {
    init { require(value.isNotBlank()) { "canonical id must not be blank" } }
}

@JvmInline
value class PackId(val value: String) {
    init { require(value.isNotBlank()) { "pack id must not be blank" } }
}

@JvmInline
value class ModelId(val value: String) {
    init { require(value.isNotBlank()) { "model id must not be blank" } }
}

@JvmInline
value class VariantId(val value: String) {
    init { require(value.isNotBlank()) { "variant id must not be blank" } }
}

enum class ContentType {
    DIAGNOSTIC_SCENARIO,
    EQUIPMENT,
    ATLAS_SCHEME,
    ACCEPTANCE_ITEM,
    KNOWLEDGE,
    SOURCE,
    FIRST_AID,
    SAFETY,
}

enum class LinkType {
    RELATED_SCENARIO,
    EQUIPMENT,
    ATLAS_SCHEME,
    ACCEPTANCE_ITEM,
    KNOWLEDGE,
    SOURCE,
    FIRST_AID,
    SAFETY,
}

fun LinkType.expectedTargetType(): ContentType = when (this) {
    LinkType.RELATED_SCENARIO -> ContentType.DIAGNOSTIC_SCENARIO
    LinkType.EQUIPMENT -> ContentType.EQUIPMENT
    LinkType.ATLAS_SCHEME -> ContentType.ATLAS_SCHEME
    LinkType.ACCEPTANCE_ITEM -> ContentType.ACCEPTANCE_ITEM
    LinkType.KNOWLEDGE -> ContentType.KNOWLEDGE
    LinkType.SOURCE -> ContentType.SOURCE
    LinkType.FIRST_AID -> ContentType.FIRST_AID
    LinkType.SAFETY -> ContentType.SAFETY
}

enum class LinkScope {
    SAME_OWNER,
    COMMON_TARGET,
    EXPLICIT_CROSS_MODEL,
}

enum class ContentLayer {
    STANDARD,
    EXTENDED,
    RESTRICTED,
}

enum class ProvenanceClass {
    CURRENT_OFFICIAL,
    ARCHIVED_OFFICIAL,
    MANUFACTURER,
    MANUFACTURER_EXTENDED,
    HISTORICAL_TRAINING,
    FIELD_PRACTICE,
    UNKNOWN,
}

enum class SourceStatus {
    CURRENT,
    SUPERSEDED,
    ARCHIVED,
    UNKNOWN,
}

enum class ActionDisposition {
    INFORMATION_ONLY,
    CONDITIONAL_ACTION,
    PROHIBITED,
}

sealed interface ContentOwner {
    data object Common : ContentOwner
    data class Model(val modelId: ModelId) : ContentOwner
}

fun ContentOwner.modelIdOrNull(): ModelId? = when (this) {
    ContentOwner.Common -> null
    is ContentOwner.Model -> modelId
}

data class RuntimeContext(
    val activeModelId: ModelId? = null,
    val activeVariantId: VariantId? = null,
    val viewedModelId: ModelId? = null,
    val allowedLayers: Set<ContentLayer> = setOf(ContentLayer.STANDARD),
) {
    val contentModelId: ModelId?
        get() = viewedModelId ?: activeModelId
}

data class Applicability(
    val modelIds: Set<ModelId> = emptySet(),
    val variantIds: Set<VariantId> = emptySet(),
    val sectionIds: Set<String> = emptySet(),
    val equipmentIds: Set<CanonicalId> = emptySet(),
) {
    fun matches(context: RuntimeContext): Boolean {
        if (modelIds.isNotEmpty()) {
            val model = context.contentModelId ?: return false
            if (model !in modelIds) return false
        }
        if (variantIds.isNotEmpty()) {
            val variant = context.activeVariantId ?: return false
            if (variant !in variantIds) return false
        }
        return true
    }
}

data class ContentLink(
    val type: LinkType,
    val targetId: CanonicalId,
    val role: String? = null,
    val scope: LinkScope = LinkScope.SAME_OWNER,
    val applicability: Applicability = Applicability(),
)

data class ContentEntry(
    val id: CanonicalId,
    val aliases: Set<CanonicalId> = emptySet(),
    val type: ContentType,
    val owner: ContentOwner,
    val applicability: Applicability = Applicability(),
    val layer: ContentLayer = ContentLayer.STANDARD,
    val provenance: ProvenanceClass,
    val sourceStatus: SourceStatus,
    val actionDisposition: ActionDisposition,
    val links: List<ContentLink> = emptyList(),
)

data class ContentPackManifest(
    val schemaVersion: Int,
    val packId: PackId,
    val packVersion: String,
    val family: String?,
    val modelIds: Set<ModelId>,
    val variantIds: Set<VariantId>,
    val locale: String,
    val entries: Set<CanonicalId>,
    val requiresRuntime: String,
    val sourceCatalogVersion: String,
    val checksums: Map<String, String>,
) {
    init {
        require(schemaVersion > 0) { "schemaVersion must be positive" }
        require(packVersion.isNotBlank()) { "packVersion must not be blank" }
        require(locale.isNotBlank()) { "locale must not be blank" }
        require(requiresRuntime.isNotBlank()) { "requiresRuntime must not be blank" }
        require(sourceCatalogVersion.isNotBlank()) { "sourceCatalogVersion must not be blank" }
    }
}

data class ContentTarget(
    val id: CanonicalId,
    val expectedType: ContentType,
)
