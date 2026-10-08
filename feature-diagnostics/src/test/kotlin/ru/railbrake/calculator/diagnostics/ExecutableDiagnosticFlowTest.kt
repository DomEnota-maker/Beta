package ru.railbrake.calculator.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
                """                    "UNKNOWN": {
                      "meaning": "Неизвестно",
                      "nextQuestionKey": "q2",
                      "candidateCauseIds": ["cause-b"]
                    }""",
                "",
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
}
