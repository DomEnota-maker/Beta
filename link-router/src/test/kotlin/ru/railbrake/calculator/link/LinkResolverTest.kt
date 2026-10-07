package ru.railbrake.calculator.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
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
import ru.railbrake.calculator.domain.RuntimeContext
import ru.railbrake.calculator.domain.SourceStatus
import ru.railbrake.calculator.navigation.FeatureDestination
import ru.railbrake.calculator.runtime.ContentPack
import ru.railbrake.calculator.runtime.ContentRegistry
import ru.railbrake.calculator.runtime.ValidationIssueCode

class LinkResolverTest {
    private val vl80s = ModelId("vl80s")
    private val ermak = ModelId("ermak")

    private fun entry(
        id: String,
        type: ContentType,
        owner: ContentOwner,
        links: List<ContentLink> = emptyList(),
    ) = ContentEntry(
        id = CanonicalId(id),
        type = type,
        owner = owner,
        applicability = when (owner) {
            ContentOwner.Common -> Applicability()
            is ContentOwner.Model -> Applicability(modelIds = setOf(owner.modelId))
        },
        layer = ContentLayer.STANDARD,
        provenance = ProvenanceClass.CURRENT_OFFICIAL,
        sourceStatus = SourceStatus.CURRENT,
        actionDisposition = ActionDisposition.INFORMATION_ONLY,
        links = links,
    )

    private fun pack(id: String, entries: List<ContentEntry>) = ContentPack(
        manifest = ContentPackManifest(
            schemaVersion = 1,
            packId = PackId(id),
            packVersion = "0.1.0",
            family = null,
            modelIds = entries.mapNotNull { (it.owner as? ContentOwner.Model)?.modelId }.toSet(),
            variantIds = emptySet(),
            locale = "ru-RU",
            entries = entries.map { it.id }.toSet(),
            requiresRuntime = "1",
            sourceCatalogVersion = "1",
            checksums = emptyMap(),
        ),
        entries = entries,
    )

    @Test
    fun graphLinksDiagnosticsAtlasAcceptanceAndCommonKnowledgeWithoutFeatureCoupling() {
        val equipmentLink = ContentLink(LinkType.EQUIPMENT, CanonicalId("vl80s.eq.pantograph"))
        val acceptanceLink = ContentLink(LinkType.ACCEPTANCE_ITEM, CanonicalId("vl80s.accept.pantograph"))
        val commonLink = ContentLink(
            type = LinkType.KNOWLEDGE,
            targetId = CanonicalId("common.knowledge.hv-safety"),
            scope = LinkScope.COMMON_TARGET,
        )

        val diagnostic = entry(
            id = "vl80s.diag.pantograph-no-rise",
            type = ContentType.DIAGNOSTIC_SCENARIO,
            owner = ContentOwner.Model(vl80s),
            links = listOf(equipmentLink, acceptanceLink, commonLink),
        )
        val equipment = entry("vl80s.eq.pantograph", ContentType.EQUIPMENT, ContentOwner.Model(vl80s))
        val acceptance = entry("vl80s.accept.pantograph", ContentType.ACCEPTANCE_ITEM, ContentOwner.Model(vl80s))
        val common = entry("common.knowledge.hv-safety", ContentType.KNOWLEDGE, ContentOwner.Common)

        val registry = ContentRegistry()
        assertTrue(registry.install(pack("vl80s.links", listOf(diagnostic, equipment, acceptance))).isEmpty())
        assertTrue(registry.install(pack("common.links", listOf(common))).isEmpty())
        assertTrue(registry.validateGlobalLinks().isEmpty())

        val resolver = LinkResolver(registry)
        val context = RuntimeContext(activeModelId = vl80s)

        val equipmentResult = resolver.resolve(diagnostic.id, equipmentLink, context)
        assertTrue(equipmentResult is LinkNavigationResult.Found)
        assertEquals(
            FeatureDestination.ATLAS,
            (equipmentResult as LinkNavigationResult.Found).target.destination,
        )

        val acceptanceResult = resolver.resolve(diagnostic.id, acceptanceLink, context)
        assertEquals(
            FeatureDestination.ACCEPTANCE,
            (acceptanceResult as LinkNavigationResult.Found).target.destination,
        )

        val commonResult = resolver.resolve(diagnostic.id, commonLink, context)
        assertEquals(
            FeatureDestination.REFERENCE,
            (commonResult as LinkNavigationResult.Found).target.destination,
        )
    }

    @Test
    fun accidentalCrossModelLinkFailsGraphValidation() {
        val wrongLink = ContentLink(
            type = LinkType.EQUIPMENT,
            targetId = CanonicalId("ermak.eq.pantograph"),
        )
        val source = entry(
            "vl80s.diag.pantograph-no-rise",
            ContentType.DIAGNOSTIC_SCENARIO,
            ContentOwner.Model(vl80s),
            links = listOf(wrongLink),
        )
        val target = entry("ermak.eq.pantograph", ContentType.EQUIPMENT, ContentOwner.Model(ermak))

        val registry = ContentRegistry()
        assertTrue(registry.install(pack("mixed.links", listOf(source, target))).isEmpty())

        assertTrue(
            registry.validateGlobalLinks().any { it.code == ValidationIssueCode.LINK_SCOPE_VIOLATION }
        )
    }

    @Test
    fun explicitCrossModelLinkChangesViewedModelButNotWorkingModel() {
        val link = ContentLink(
            type = LinkType.EQUIPMENT,
            targetId = CanonicalId("ermak.eq.pantograph"),
            scope = LinkScope.EXPLICIT_CROSS_MODEL,
        )
        val source = entry(
            "vl80s.diag.pantograph-no-rise",
            ContentType.DIAGNOSTIC_SCENARIO,
            ContentOwner.Model(vl80s),
            links = listOf(link),
        )
        val target = entry("ermak.eq.pantograph", ContentType.EQUIPMENT, ContentOwner.Model(ermak))

        val registry = ContentRegistry()
        assertTrue(registry.install(pack("cross.links", listOf(source, target))).isEmpty())
        assertTrue(registry.validateGlobalLinks().isEmpty())

        val original = RuntimeContext(activeModelId = vl80s)
        val result = LinkResolver(registry).resolve(source.id, link, original)

        assertTrue(result is LinkNavigationResult.Found)
        val found = result as LinkNavigationResult.Found
        assertEquals(vl80s, original.activeModelId)
        assertEquals(null, original.viewedModelId)
        assertEquals(vl80s, found.targetContext.activeModelId)
        assertEquals(ermak, found.targetContext.viewedModelId)
        assertEquals(ermak, found.target.viewedModelId)
        assertEquals(FeatureDestination.ATLAS, found.target.destination)
    }
}
