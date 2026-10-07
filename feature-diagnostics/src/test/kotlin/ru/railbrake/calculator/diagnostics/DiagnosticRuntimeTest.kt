package ru.railbrake.calculator.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.railbrake.calculator.domain.ContentLayer
import ru.railbrake.calculator.domain.ExtendedDiagnosticClass
import ru.railbrake.calculator.domain.PublicationStatus

class DiagnosticRuntimeTest {
    private val indexJson = """
        {
          "schemaVersion": 1,
          "modelId": "vl80s",
          "recommendedPacks": ["recommended.pack.json"],
          "extendedCorpora": {
            "SUPPLEMENTAL_OPERATIONAL": ["extended.pack.json"]
          },
          "relationGraph": "relation-graph.json",
          "runtimePayloadStatus": "RELATION_GRAPH_ONLY_NOT_EXECUTABLE",
          "interactiveRuntimeSource": "DiagnosticRepository.scenarios",
          "publicationPolicy": "ALL_CANDIDATE_UNTIL_FEATURE_RUNTIME_ACCEPTANCE",
          "goldenReferenceCandidate": "vl80s.diag.pantograph-no-rise"
        }
    """.trimIndent()

    @Test
    fun relationGraphNeverPretendsToBeExecutableFlow() {
        val index = DiagnosticFeatureIndexJsonLoader().parse(indexJson)

        assertEquals(
            DiagnosticRuntimeAvailability.RELATION_GRAPH_ONLY,
            index.runtimeAvailability,
        )
        assertEquals(
            "DiagnosticRepository.scenarios",
            index.interactiveRuntimeSource,
        )
    }

    @Test
    fun recommendedCandidateIsVisibleButCannotExecute() {
        val decision = DiagnosticAccessPolicy.evaluate(
            descriptor = DiagnosticDescriptor(
                id = "vl80s.diag.pantograph-no-rise",
                layer = ContentLayer.STANDARD,
                publicationStatus = PublicationStatus.CANDIDATE,
                corpus = "RECOMMENDED",
                extendedClass = null,
            ),
            runtimeAvailability = DiagnosticRuntimeAvailability.RELATION_GRAPH_ONLY,
        )

        assertTrue(decision.visibleInCatalog)
        assertFalse(decision.executable)
        assertEquals(
            DiagnosticAccessReason.CANDIDATE_NOT_PUBLISHED,
            decision.reason,
        )
    }

    @Test
    fun extendedCatalogIsHiddenUntilItsClassIsExplicitlyEnabled() {
        val descriptor = DiagnosticDescriptor(
            id = "vl80s.diag.pantograph-slow-rise",
            layer = ContentLayer.EXTENDED,
            publicationStatus = PublicationStatus.CANDIDATE,
            corpus = "EXTENDED",
            extendedClass = ExtendedDiagnosticClass.SUPPLEMENTAL_OPERATIONAL,
        )

        val disabled = DiagnosticAccessPolicy.evaluate(
            descriptor,
            DiagnosticRuntimeAvailability.RELATION_GRAPH_ONLY,
        )
        assertFalse(disabled.visibleInCatalog)
        assertEquals(
            DiagnosticAccessReason.EXTENDED_DISABLED,
            disabled.reason,
        )

        val enabled = DiagnosticAccessPolicy.evaluate(
            descriptor,
            DiagnosticRuntimeAvailability.RELATION_GRAPH_ONLY,
            DiagnosticVisibilitySettings(
                extendedEnabled = true,
                enabledExtendedClasses = setOf(
                    ExtendedDiagnosticClass.SUPPLEMENTAL_OPERATIONAL
                ),
            ),
        )
        assertTrue(enabled.visibleInCatalog)
        assertFalse(enabled.executable)
        assertEquals(
            DiagnosticAccessReason.CANDIDATE_NOT_PUBLISHED,
            enabled.reason,
        )
    }

    @Test
    fun activeScenarioStillCannotExecuteWithoutExecutableRuntime() {
        val decision = DiagnosticAccessPolicy.evaluate(
            descriptor = DiagnosticDescriptor(
                id = "vl80s.diag.example",
                layer = ContentLayer.STANDARD,
                publicationStatus = PublicationStatus.ACTIVE,
                corpus = "RECOMMENDED",
                extendedClass = null,
            ),
            runtimeAvailability = DiagnosticRuntimeAvailability.RELATION_GRAPH_ONLY,
        )

        assertTrue(decision.visibleInCatalog)
        assertFalse(decision.executable)
        assertEquals(
            DiagnosticAccessReason.EXECUTABLE_RUNTIME_UNAVAILABLE,
            decision.reason,
        )
    }

    @Test
    fun onlyActiveScenarioWithRealRuntimeCanExecute() {
        val decision = DiagnosticAccessPolicy.evaluate(
            descriptor = DiagnosticDescriptor(
                id = "vl80s.diag.example",
                layer = ContentLayer.STANDARD,
                publicationStatus = PublicationStatus.ACTIVE,
                corpus = "RECOMMENDED",
                extendedClass = null,
            ),
            runtimeAvailability = DiagnosticRuntimeAvailability.EXECUTABLE_FLOW_AVAILABLE,
        )

        assertTrue(decision.visibleInCatalog)
        assertTrue(decision.executable)
        assertEquals(DiagnosticAccessReason.AVAILABLE, decision.reason)
    }
}
