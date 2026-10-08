package ru.railbrake.calculator.atlas

import com.google.gson.Gson
import ru.railbrake.calculator.domain.CanonicalId
import ru.railbrake.calculator.domain.ContentTarget
import ru.railbrake.calculator.domain.ContentType

data class NormalizedBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    init {
        require(left in 0f..1f && top in 0f..1f)
        require(right in 0f..1f && bottom in 0f..1f)
        require(left < right) { "left must be smaller than right" }
        require(top < bottom) { "top must be smaller than bottom" }
    }

    val area: Float
        get() = (right - left) * (bottom - top)

    fun contains(x: Float, y: Float): Boolean =
        x in left..right && y in top..bottom
}

data class AtlasLayoutHotspot(
    val id: String,
    val title: String,
    val subtitle: String,
    val details: String,
    val learnMore: List<String>,
    val bounds: NormalizedBounds,
    val equipmentId: CanonicalId?,
) {
    fun equipmentTarget(): ContentTarget? =
        equipmentId?.let { ContentTarget(it, ContentType.EQUIPMENT) }
}

data class AtlasLayoutBackground(
    val assetPath: String,
    val status: String,
    val sha256: String,
    val sourceRepository: String,
    val sourceCommit: String,
    val sourceArchive: String,
    val sourceArchiveBlobSha: String,
    val sourceEntry: String,
)

data class AtlasLayoutMap(
    val id: String,
    val modelId: String,
    val title: String,
    val summary: String,
    val spatialClaim: String,
    val background: AtlasLayoutBackground? = null,
    val hotspots: List<AtlasLayoutHotspot>,
) {
    fun hitTest(x: Float, y: Float): AtlasLayoutHotspot? =
        hotspots
            .filter { it.bounds.contains(x, y) }
            .minByOrNull { it.bounds.area }
}

class AtlasLayoutJsonLoader {
    private val gson = Gson()

    fun parse(json: String): AtlasLayoutMap {
        val raw = gson.fromJson(json, JsonLayoutDocument::class.java)
            ?: error("atlas layout document is empty")
        val hotspots = raw.hotspots.orEmpty().map { it.toDomain() }

        require(raw.hotspotCount == hotspots.size) {
            "hotspotCount does not match hotspot payload"
        }
        require(
            raw.canonicalEquipmentLinkCount ==
                hotspots.count { it.equipmentId != null }
        ) {
            "canonicalEquipmentLinkCount does not match hotspot payload"
        }

        return AtlasLayoutMap(
            id = requireLayoutText(raw.id, "layout.id"),
            modelId = requireLayoutText(raw.modelId, "layout.modelId"),
            title = requireLayoutText(raw.title, "layout.title"),
            summary = requireLayoutText(raw.summary, "layout.summary"),
            spatialClaim = requireLayoutText(
                raw.semantics?.spatialClaim,
                "layout.semantics.spatialClaim",
            ),
            background = raw.background?.toDomain(),
            hotspots = hotspots,
        )
    }
}

private data class JsonLayoutDocument(
    val id: String? = null,
    val modelId: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val hotspotCount: Int = -1,
    val canonicalEquipmentLinkCount: Int = -1,
    val semantics: JsonLayoutSemantics? = null,
    val background: JsonLayoutBackground? = null,
    val hotspots: List<JsonHotspot>? = null,
)

private data class JsonLayoutSemantics(
    val spatialClaim: String? = null,
)

private data class JsonLayoutBackground(
    val assetPath: String? = null,
    val status: String? = null,
    val sha256: String? = null,
    val source: JsonLayoutBackgroundSource? = null,
) {
    fun toDomain(): AtlasLayoutBackground {
        val sourceValue = requireNotNull(source) {
            "layout.background.source is missing"
        }
        return AtlasLayoutBackground(
            assetPath = requireLayoutText(assetPath, "layout.background.assetPath"),
            status = requireLayoutText(status, "layout.background.status"),
            sha256 = requireLayoutText(sha256, "layout.background.sha256"),
            sourceRepository = requireLayoutText(
                sourceValue.repository,
                "layout.background.source.repository",
            ),
            sourceCommit = requireLayoutText(
                sourceValue.commit,
                "layout.background.source.commit",
            ),
            sourceArchive = requireLayoutText(
                sourceValue.archive,
                "layout.background.source.archive",
            ),
            sourceArchiveBlobSha = requireLayoutText(
                sourceValue.archiveBlobSha,
                "layout.background.source.archiveBlobSha",
            ),
            sourceEntry = requireLayoutText(
                sourceValue.entry,
                "layout.background.source.entry",
            ),
        )
    }
}

private data class JsonLayoutBackgroundSource(
    val repository: String? = null,
    val commit: String? = null,
    val archive: String? = null,
    val archiveBlobSha: String? = null,
    val entry: String? = null,
)

private data class JsonHotspot(
    val id: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val details: String? = null,
    val learnMore: List<String>? = null,
    val bounds: JsonBounds? = null,
    val equipmentId: String? = null,
) {
    fun toDomain() = AtlasLayoutHotspot(
        id = requireLayoutText(id, "hotspot.id"),
        title = requireLayoutText(title, "hotspot.title"),
        subtitle = requireLayoutText(subtitle, "hotspot.subtitle"),
        details = requireLayoutText(details, "hotspot.details"),
        learnMore = learnMore.orEmpty(),
        bounds = requireNotNull(bounds) { "hotspot.bounds is missing" }.toDomain(),
        equipmentId = equipmentId
            ?.takeIf(String::isNotBlank)
            ?.let(::CanonicalId),
    )
}

private data class JsonBounds(
    val left: Float = -1f,
    val top: Float = -1f,
    val right: Float = -1f,
    val bottom: Float = -1f,
) {
    fun toDomain() = NormalizedBounds(left, top, right, bottom)
}


private fun requireLayoutText(value: String?, field: String): String {
    require(!value.isNullOrBlank()) { "$field must not be blank" }
    return value
}
