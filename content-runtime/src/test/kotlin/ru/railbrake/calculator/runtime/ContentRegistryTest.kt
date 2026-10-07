package ru.railbrake.calculator.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.railbrake.calculator.domain.ActionDisposition
import ru.railbrake.calculator.domain.Applicability
import ru.railbrake.calculator.domain.CanonicalId
import ru.railbrake.calculator.domain.ContentEntry
import ru.railbrake.calculator.domain.ContentLayer
import ru.railbrake.calculator.domain.ContentOwner
import ru.railbrake.calculator.domain.ContentPackManifest
import ru.railbrake.calculator.domain.ContentTarget
import ru.railbrake.calculator.domain.ContentType
import ru.railbrake.calculator.domain.ModelId
import ru.railbrake.calculator.domain.PackId
import ru.railbrake.calculator.domain.ProvenanceClass
import ru.railbrake.calculator.domain.RuntimeContext
import ru.railbrake.calculator.domain.SourceStatus

class ContentRegistryTest {
    private val model = ModelId("vl80s")
    private val entry = ContentEntry(
        id = CanonicalId("vl80s.pantograph.not_raise"),
        aliases = setOf(CanonicalId("pantograph-not-raise")),
        type = ContentType.DIAGNOSTIC_SCENARIO,
        owner = ContentOwner.Model(model),
        applicability = Applicability(modelIds = setOf(model)),
        layer = ContentLayer.STANDARD,
        provenance = ProvenanceClass.CURRENT_OFFICIAL,
        sourceStatus = SourceStatus.CURRENT,
        actionDisposition = ActionDisposition.CONDITIONAL_ACTION,
    )

    private fun pack() = ContentPack(
        manifest = ContentPackManifest(
            schemaVersion = 1,
            packId = PackId("vl80s.bootstrap"),
            packVersion = "0.1.0",
            family = "electric",
            modelIds = setOf(model),
            variantIds = emptySet(),
            locale = "ru-RU",
            entries = setOf(entry.id),
            requiresRuntime = "1",
            sourceCatalogVersion = "1",
            checksums = emptyMap(),
        ),
        entries = listOf(entry),
    )

    @Test
    fun resolvesAliasOnlyForApplicableModel() {
        val registry = ContentRegistry()
        assertTrue(registry.install(pack()).isEmpty())

        val target = ContentTarget(CanonicalId("pantograph-not-raise"), ContentType.DIAGNOSTIC_SCENARIO)
        val found = registry.resolve(target, RuntimeContext(workingModelId = model))
        assertTrue(found is ResolveResult.Found)

        val denied = registry.resolve(target, RuntimeContext(workingModelId = ModelId("ermak")))
        assertEquals(ResolveResult.Inapplicable, denied)
    }

    @Test
    fun duplicateCanonicalIdIsRejected() {
        val registry = ContentRegistry()
        assertTrue(registry.install(pack()).isEmpty())
        val issues = registry.install(pack())
        assertTrue(issues.any { it.code == ValidationIssueCode.DUPLICATE_CANONICAL_ID })
    }
}
