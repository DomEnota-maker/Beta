package ru.railbrake.calculator.runtime

import com.google.gson.Gson
import ru.railbrake.calculator.domain.ActionDisposition
import ru.railbrake.calculator.domain.Applicability
import ru.railbrake.calculator.domain.CanonicalId
import ru.railbrake.calculator.domain.ContentEntry
import ru.railbrake.calculator.domain.ContentLayer
import ru.railbrake.calculator.domain.ContentLink
import ru.railbrake.calculator.domain.ContentOwner
import ru.railbrake.calculator.domain.ContentPackManifest
import ru.railbrake.calculator.domain.ContentType
import ru.railbrake.calculator.domain.LinkScope
import ru.railbrake.calculator.domain.LinkType
import ru.railbrake.calculator.domain.ModelId
import ru.railbrake.calculator.domain.PackId
import ru.railbrake.calculator.domain.ProvenanceClass
import ru.railbrake.calculator.domain.PublicationStatus
import ru.railbrake.calculator.domain.SourceStatus
import ru.railbrake.calculator.domain.VariantId

class ContentPackJsonLoader(
    private val gson: Gson = Gson(),
) {
    fun parse(json: String): ContentPack {
        val document = gson.fromJson(json, JsonContentPack::class.java)
            ?: error("content pack document is empty")

        val entries = document.entries.map { it.toDomain() }
        val manifest = document.manifest.toDomain()

        return ContentPack(
            manifest = manifest,
            entries = entries,
        )
    }
}

private data class JsonContentPack(
    val manifest: JsonManifest,
    val entries: List<JsonEntry> = emptyList(),
)

private data class JsonManifest(
    val schemaVersion: Int,
    val packId: String,
    val packVersion: String,
    val family: String?,
    val modelIds: List<String> = emptyList(),
    val variantIds: List<String> = emptyList(),
    val locale: String,
    val entries: List<String> = emptyList(),
    val requiresRuntime: String,
    val sourceCatalogVersion: String,
    val checksums: Map<String, String> = emptyMap(),
) {
    fun toDomain() = ContentPackManifest(
        schemaVersion = schemaVersion,
        packId = PackId(packId),
        packVersion = packVersion,
        family = family,
        modelIds = modelIds.map(::ModelId).toSet(),
        variantIds = variantIds.map(::VariantId).toSet(),
        locale = locale,
        entries = entries.map(::CanonicalId).toSet(),
        requiresRuntime = requiresRuntime,
        sourceCatalogVersion = sourceCatalogVersion,
        checksums = checksums,
    )
}

private data class JsonEntry(
    val id: String,
    val aliases: List<String> = emptyList(),
    val type: String,
    val owner: JsonOwner,
    val applicability: JsonApplicability = JsonApplicability(),
    val layer: String = "STANDARD",
    val publicationStatus: String = "ACTIVE",
    val provenance: String = "UNKNOWN",
    val sourceStatus: String = "UNKNOWN",
    val actionDisposition: String = "INFORMATION_ONLY",
    val title: String,
    val summary: String? = null,
    val details: List<String> = emptyList(),
    val sourceRefs: List<String> = emptyList(),
    val links: List<JsonLink> = emptyList(),
) {
    fun toDomain() = ContentEntry(
        id = CanonicalId(id),
        aliases = aliases.map(::CanonicalId).toSet(),
        type = ContentType.valueOf(type),
        owner = owner.toDomain(),
        applicability = applicability.toDomain(),
        layer = ContentLayer.valueOf(layer),
        publicationStatus = PublicationStatus.valueOf(publicationStatus),
        provenance = ProvenanceClass.valueOf(provenance),
        sourceStatus = SourceStatus.valueOf(sourceStatus),
        actionDisposition = ActionDisposition.valueOf(actionDisposition),
        title = title,
        summary = summary,
        details = details,
        sourceRefs = sourceRefs.toSet(),
        links = links.map { it.toDomain() },
    )
}

private data class JsonOwner(
    val kind: String,
    val modelId: String? = null,
) {
    fun toDomain(): ContentOwner = when (kind) {
        "COMMON" -> ContentOwner.Common
        "MODEL" -> ContentOwner.Model(ModelId(requireNotNull(modelId) {
            "MODEL owner requires modelId"
        }))
        else -> error("unknown owner kind: $kind")
    }
}

private data class JsonApplicability(
    val modelIds: List<String> = emptyList(),
    val variantIds: List<String> = emptyList(),
    val sectionIds: List<String> = emptyList(),
    val equipmentIds: List<String> = emptyList(),
) {
    fun toDomain() = Applicability(
        modelIds = modelIds.map(::ModelId).toSet(),
        variantIds = variantIds.map(::VariantId).toSet(),
        sectionIds = sectionIds.toSet(),
        equipmentIds = equipmentIds.map(::CanonicalId).toSet(),
    )
}

private data class JsonLink(
    val type: String,
    val targetId: String,
    val role: String? = null,
    val scope: String = "SAME_MODEL",
    val applicability: JsonApplicability = JsonApplicability(),
) {
    fun toDomain() = ContentLink(
        type = LinkType.valueOf(type),
        targetId = CanonicalId(targetId),
        role = role,
        scope = LinkScope.valueOf(scope),
        applicability = applicability.toDomain(),
    )
}
