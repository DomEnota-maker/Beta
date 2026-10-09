package ru.railbrake.calculator.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.railbrake.calculator.domain.ActionDisposition
import ru.railbrake.calculator.domain.Applicability
import ru.railbrake.calculator.domain.CanonicalId
import ru.railbrake.calculator.domain.ContentEntry
import ru.railbrake.calculator.domain.ContentLayer
import ru.railbrake.calculator.domain.ContentOwner
import ru.railbrake.calculator.domain.ContentPackManifest
import ru.railbrake.calculator.domain.ContentType
import ru.railbrake.calculator.domain.ModelId
import ru.railbrake.calculator.domain.PackId
import ru.railbrake.calculator.domain.ProvenanceClass
import ru.railbrake.calculator.domain.PublicationStatus
import ru.railbrake.calculator.domain.RuntimeContext
import ru.railbrake.calculator.domain.SourceStatus
import ru.railbrake.calculator.domain.VariantId
import ru.railbrake.calculator.runtime.ContentPack
import ru.railbrake.calculator.runtime.ContentRegistry

class ExecutableDiagnosticFlowTest {
    private val fixture = """
        {
          "schemaVersion": 1,
          "modelId": "vl80s",
          "sourceSnapshot": {
            "repository": "DomEnota-maker/Test-",
            "commit": "3176d6ee",
            "runtimeSource": "DiagnosticRepository.scenarios"
          },
          "scenarioCount": 2,
          "questionCount": 2,
          "responseEdgeCount": 6,
          "scenarios": [
            {
              "legacyId": "one",
              "id": "vl80s.diag.one",
              "category": "Тест",
              "title": "Первый сценарий",
              "summary": "Тестовый сценарий",
              "profileId": "vl80s",
              "applicableVariantIds": ["vl80s-general"],
              "informationConfidence": "MANUFACTURER_OR_MANUAL",
              "severity": "ATTENTION",
              "applicability": "ВЛ80С",
              "startQuestionKey": "q1",
              "questions": [
                {
                  "key": "q1",
                  "text": "Первый вопрос?",
                  "responses": {
                    "YES": {
                      "meaning": "Да-ветка",
                      "nextQuestionKey": "q2",
                      "candidateCauseIds": ["cause-a"]
                    },
                    "NO": {
                      "meaning": "Нет-ветка",
                      "nextQuestionKey": "__end__",
                      "candidateCauseIds": []
                    },
                    "UNKNOWN": {
                      "meaning": "Неизвестно",
                      "nextQuestionKey": "q2",
                      "candidateCauseIds": ["cause-b"]
                    }
                  }
                },
                {
                  "key": "q2",
                  "text": "Второй вопрос?",
                  "responses": {
                    "YES": {
                      "meaning": "Финиш",
                      "nextQuestionKey": "__end__",
                      "candidateCauseIds": ["cause-a"]
                    },
                    "NO": {
                      "meaning": "Финиш",
                      "nextQuestionKey": "__end__",
                      "candidateCauseIds": ["cause-b"]
                    },
                    "UNKNOWN": {
                      "meaning": "Финиш",
                      "nextQuestionKey": "__end__",
                      "candidateCauseIds": []
                    }
                  }
                }
              ],
              "diagnosticCauses": [
                {
                  "id": "cause-a",
                  "title": "Причина А",
                  "explanation": "A",
                  "confidence": "REFERENCE"
                },
                {
                  "id": "cause-b",
                  "title": "Причина Б",
                  "explanation": "B",
                  "confidence": "REFERENCE"
                }
              ],
              "probableCauses": [],
              "immediateActions": [],
              "dangerSigns": [],
              "checks": [],
              "prohibited": [],
              "stopConditions": [],
              "reportFields": [],
              "relatedEquipment": [],
              "sourceNote": "source",
              "observableSigns": [],
              "systemExplanation": [],
              "operationalConsequences": [],
              "trainingNotes": [],
              "feedbackPrompts": [],
              "relatedScenarioIds": ["vl80s.diag.two"]
            },
            {
              "legacyId": "two",
              "id": "vl80s.diag.two",
              "category": "Тест",
              "title": "Второй сценарий",
              "summary": "Без вопросов",
              "profileId": "vl80s",
              "applicableVariantIds": ["vl80s-general"],
              "informationConfidence": "REFERENCE",
              "severity": "INFORMATION",
              "applicability": "ВЛ80С",
              "startQuestionKey": null,
              "questions": [],
              "diagnosticCauses": [],
              "probableCauses": [],
              "immediateActions": [],
              "dangerSigns": [],
              "checks": [],
              "prohibited": [],
              "stopConditions": [],
              "reportFields": [],
              "relatedEquipment": [],
              "sourceNote": "source",
              "observableSigns": [],
              "systemExplanation": [],
              "operationalConsequences": [],
              "trainingNotes": [],
              "feedbackPrompts": [],
              "relatedScenarioIds": []
            }
          ]
        }
    """.trimIndent()

    @Test
    fun loaderBuildsClosedRuntimeAndEngineUsesExportedEdges() {
        val runtime = ExecutableDiagnosticFlowJsonLoader().parse(fixture)
        val scenario = requireNotNull(runtime.scenario("vl80s.diag.one"))

        var state = ExecutableDiagnosticEngine.start(scenario)
        assertEquals("q1", state.currentQuestionKey)

        val first = ExecutableDiagnosticEngine.answer(
            scenario,
            state,
            ExecutableDiagnosticResponse.YES,
        )
        state = first.state
        assertEquals("q2", state.currentQuestionKey)
        assertEquals(1, state.candidateScores["cause-a"])
        assertEquals("Причина А", first.leadingCauses.first().title)

        val second = ExecutableDiagnosticEngine.answer(
            scenario,
            state,
            ExecutableDiagnosticResponse.UNKNOWN,
        )
        assertNull(second.state.currentQuestionKey)
        assertNull(second.nextQuestion)
        assertEquals(2, second.state.answers.size)
    }

    @Test
    fun unknownBranchIsFirstClassAndCanScoreDifferentCause() {
        val runtime = ExecutableDiagnosticFlowJsonLoader().parse(fixture)
        val scenario = requireNotNull(runtime.scenario("vl80s.diag.one"))

        val result = ExecutableDiagnosticEngine.answer(
            scenario,
            ExecutableDiagnosticEngine.start(scenario),
            ExecutableDiagnosticResponse.UNKNOWN,
        )

        assertEquals("q2", result.state.currentQuestionKey)
        assertEquals(1, result.state.candidateScores["cause-b"])
        assertEquals("Причина Б", result.leadingCauses.first().title)
    }

    @Test(expected = IllegalArgumentException::class)
    fun loaderFailsClosedWhenUnknownResponseBranchIsMissing() {
        ExecutableDiagnosticFlowJsonLoader().parse(
            fixture.replace(
                "\"UNKNOWN\": {",
                "\"REMOVED_UNKNOWN\": {",
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun loaderFailsClosedForUnknownTargetQuestion() {
        ExecutableDiagnosticFlowJsonLoader().parse(
            fixture.replace(
                "\"nextQuestionKey\": \"q2\"",
                "\"nextQuestionKey\": \"does-not-exist\"",
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun loaderFailsClosedForUnknownCandidateCause() {
        ExecutableDiagnosticFlowJsonLoader().parse(
            fixture.replace(
                "\"candidateCauseIds\": [\"cause-a\"]",
                "\"candidateCauseIds\": [\"missing-cause\"]",
            )
        )
    }

    @Test
    fun previousAnswerReplaysGraphAndRemovesDiscardedCauseScores() {
        val scenario = requireNotNull(
            ExecutableDiagnosticFlowJsonLoader().parse(fixture).scenario("vl80s.diag.one")
        )
        val started = ExecutableDiagnosticEngine.start(scenario)
        val first = ExecutableDiagnosticEngine.answer(
            scenario,
            started,
            ExecutableDiagnosticResponse.YES,
        ).state
        val finished = ExecutableDiagnosticEngine.answer(
            scenario,
            first,
            ExecutableDiagnosticResponse.NO,
        ).state
        assertEquals(2, finished.answers.size)
        val back = ExecutableDiagnosticEngine.previous(scenario, finished)
        assertEquals(first, back)
        assertEquals("q2", back.currentQuestionKey)
        assertEquals(1, back.candidateScores["cause-a"])
        assertEquals(started, ExecutableDiagnosticEngine.previous(scenario, back))
        assertEquals(started, ExecutableDiagnosticEngine.previous(scenario, started))
    }

    @Test
    fun answeringCompletedScenarioIsStableNoOpResult() {
        val runtime = ExecutableDiagnosticFlowJsonLoader().parse(fixture)
        val scenario = requireNotNull(runtime.scenario("vl80s.diag.two"))
        val state = ExecutableDiagnosticEngine.start(scenario)

        val result = ExecutableDiagnosticEngine.answer(
            scenario,
            state,
            ExecutableDiagnosticResponse.YES,
        )

        assertNull(result.nextQuestion)
        assertTrue(result.state.answers.isEmpty())
    }

    private fun registryFor(
        status: PublicationStatus,
        layer: ContentLayer = ContentLayer.STANDARD,
    ): ContentRegistry {
        val id = CanonicalId("vl80s.diag.one")
        val entry = ContentEntry(
            id = id,
            type = ContentType.DIAGNOSTIC_SCENARIO,
            owner = ContentOwner.Model(ModelId("vl80s")),
            applicability = Applicability(
                modelIds = setOf(ModelId("vl80s")),
                variantIds = setOf(VariantId("vl80s.first")),
            ),
            layer = layer,
            publicationStatus = status,
            provenance = ProvenanceClass.HISTORICAL_TRAINING,
            sourceStatus = SourceStatus.ARCHIVED,
            actionDisposition = ActionDisposition.INFORMATION_ONLY,
            title = "Первый сценарий",
        )
        val manifest = ContentPackManifest(
            schemaVersion = 1,
            packId = PackId("vl80s.test"),
            packVersion = "1",
            family = "electric",
            modelIds = setOf(ModelId("vl80s")),
            variantIds = emptySet(),
            locale = "ru",
            entries = setOf(id),
            requiresRuntime = "test",
            sourceCatalogVersion = "test",
            checksums = emptyMap(),
        )
        return ContentRegistry().also { registry ->
            assertTrue(registry.install(ContentPack(manifest, listOf(entry))).isEmpty())
        }
    }

    @Test
    fun catalogDoesNotExposeCandidateOrAbsentDonorScenarios() {
        val runtime = ExecutableDiagnosticFlowJsonLoader().parse(fixture)
        val registry = registryFor(PublicationStatus.CANDIDATE)
        val context = RuntimeContext(
            viewedModelId = ModelId("vl80s"),
            activeVariantId = VariantId("vl80s.first"),
        )
        val catalog = PublishedDiagnosticCatalog(runtime, registry, context)
        assertTrue(catalog.search("").isEmpty())
        assertNull(catalog.open("vl80s.diag.one"))
        assertNull(catalog.open("vl80s.diag.two"))
    }

    @Test
    fun catalogSearchShowsOnlyPublishedAndVariantApplicableScenarios() {
        val runtime = ExecutableDiagnosticFlowJsonLoader().parse(fixture)
        val registry = registryFor(PublicationStatus.ACTIVE)
        val context = RuntimeContext(
            viewedModelId = ModelId("vl80s"),
            activeVariantId = VariantId("vl80s.first"),
        )
        val catalog = PublishedDiagnosticCatalog(runtime, registry, context)
        assertEquals(listOf("vl80s.diag.one"), catalog.search("первый").map { it.id })
        assertEquals(listOf("vl80s.diag.one"), catalog.search("ТЕСТ").map { it.id })
        assertNull(catalog.open("vl80s.diag.two"))
        assertTrue(
            PublishedDiagnosticCatalog(
                runtime,
                registry,
                context.copy(activeVariantId = VariantId("vl80s.unknown")),
            ).search("").isEmpty(),
        )
        assertTrue(
            PublishedDiagnosticCatalog(
                runtime,
                registry,
                context.copy(viewedModelId = ModelId("ermak")),
            ).search("").isEmpty(),
        )
    }

    @Test
    fun publishedRestrictedScenarioStillNeedsLayerPermission() {
        val runtime = ExecutableDiagnosticFlowJsonLoader().parse(fixture)
        val registry = registryFor(PublicationStatus.ACTIVE, ContentLayer.RESTRICTED)
        val context = RuntimeContext(
            viewedModelId = ModelId("vl80s"),
            activeVariantId = VariantId("vl80s.first"),
        )
        assertTrue(
            PublishedDiagnosticCatalog(runtime, registry, context).search("").isEmpty()
        )
        assertEquals(
            1,
            PublishedDiagnosticCatalog(
                runtime,
                registry,
                context.copy(allowedLayers = setOf(ContentLayer.RESTRICTED)),
            ).search("").size,
        )
    }

}
