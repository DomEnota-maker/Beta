package ru.railbrake.calculator.diagnostics

import com.google.gson.Gson

const val DIAGNOSTIC_END_OF_FLOW = "__end__"

enum class ExecutableDiagnosticResponse {
    YES,
    NO,
    UNKNOWN,
}

data class ExecutableDiagnosticBranch(
    val meaning: String,
    val nextQuestionKey: String,
    val candidateCauseIds: List<String>,
)

data class ExecutableDiagnosticQuestion(
    val key: String,
    val text: String,
    val responses: Map<ExecutableDiagnosticResponse, ExecutableDiagnosticBranch>,
)

data class ExecutableDiagnosticCause(
    val id: String,
    val title: String,
    val explanation: String,
    val confidence: String,
)

data class ExecutableDiagnosticCheck(
    val title: String,
    val action: String,
    val expected: String,
    val ifAbnormal: String,
    val level: String,
)

data class ExecutableDiagnosticScenario(
    val legacyId: String,
    val id: String,
    val category: String,
    val title: String,
    val summary: String,
    val profileId: String,
    val applicableVariantIds: Set<String>,
    val informationConfidence: String,
    val severity: String,
    val applicability: String,
    val startQuestionKey: String?,
    val questions: List<ExecutableDiagnosticQuestion>,
    val diagnosticCauses: List<ExecutableDiagnosticCause>,
    val probableCauses: List<String>,
    val immediateActions: List<String>,
    val dangerSigns: List<String>,
    val checks: List<ExecutableDiagnosticCheck>,
    val prohibited: List<String>,
    val stopConditions: List<String>,
    val reportFields: List<String>,
    val relatedEquipment: List<String>,
    val sourceNote: String,
    val observableSigns: List<String>,
    val systemExplanation: List<String>,
    val operationalConsequences: List<String>,
    val trainingNotes: List<String>,
    val feedbackPrompts: List<String>,
    val relatedScenarioIds: List<String>,
) {
    private val questionsByKey = questions.associateBy { it.key }
    private val causesById = diagnosticCauses.associateBy { it.id }

    init {
        require(id.isNotBlank())
        require(legacyId.isNotBlank())
        require(title.isNotBlank())
        require(questionsByKey.size == questions.size) {
            "duplicate diagnostic question key in $id"
        }
        require(causesById.size == diagnosticCauses.size) {
            "duplicate diagnostic cause id in $id"
        }

        if (questions.isEmpty()) {
            require(startQuestionKey == null) {
                "scenario without questions cannot declare startQuestionKey: $id"
            }
        } else {
            require(startQuestionKey != null && startQuestionKey in questionsByKey) {
                "invalid startQuestionKey in $id: $startQuestionKey"
            }
        }

        questions.forEach { question ->
            require(question.responses.keys == ExecutableDiagnosticResponse.entries.toSet()) {
                "question ${question.key} in $id must define YES/NO/UNKNOWN"
            }
            question.responses.forEach { (response, branch) ->
                require(branch.meaning.isNotBlank()) {
                    "blank meaning for $id/${question.key}/$response"
                }
                require(
                    branch.nextQuestionKey == DIAGNOSTIC_END_OF_FLOW ||
                        branch.nextQuestionKey in questionsByKey
                ) {
                    "unknown next question for $id/${question.key}/$response: ${branch.nextQuestionKey}"
                }
                branch.candidateCauseIds.forEach { causeId ->
                    require(causeId in causesById) {
                        "unknown candidate cause for $id/${question.key}/$response: $causeId"
                    }
                }
            }
        }

        require(relatedScenarioIds.size == relatedScenarioIds.distinct().size) {
            "duplicate related diagnostic id in $id"
        }
    }

    fun question(key: String?): ExecutableDiagnosticQuestion? =
        key?.let(questionsByKey::get)

    fun cause(id: String): ExecutableDiagnosticCause? = causesById[id]
}

data class ExecutableDiagnosticRuntime(
    val schemaVersion: Int,
    val modelId: String,
    val sourceRepository: String,
    val sourceCommit: String,
    val runtimeSource: String,
    val scenarioCount: Int,
    val questionCount: Int,
    val responseEdgeCount: Int,
    val scenarios: List<ExecutableDiagnosticScenario>,
) {
    private val scenariosById = scenarios.associateBy { it.id }

    init {
        require(schemaVersion > 0)
        require(modelId.isNotBlank())
        require(sourceCommit.isNotBlank())
        require(scenariosById.size == scenarios.size) {
            "duplicate executable diagnostic scenario id"
        }
        require(scenarioCount == scenarios.size) {
            "scenarioCount does not match executable runtime"
        }
        require(questionCount == scenarios.sumOf { it.questions.size }) {
            "questionCount does not match executable runtime"
        }
        require(
            responseEdgeCount ==
                scenarios.sumOf { scenario ->
                    scenario.questions.sumOf { it.responses.size }
                }
        ) {
            "responseEdgeCount does not match executable runtime"
        }

        val scenarioIds = scenariosById.keys
        scenarios.forEach { scenario ->
            scenario.relatedScenarioIds.forEach { relatedId ->
                require(relatedId in scenarioIds) {
                    "unknown related diagnostic: ${scenario.id} -> $relatedId"
                }
            }
        }
    }

    fun scenario(id: String): ExecutableDiagnosticScenario? = scenariosById[id]
}

class ExecutableDiagnosticFlowJsonLoader(
    private val gson: Gson = Gson(),
) {
    fun parse(json: String): ExecutableDiagnosticRuntime {
        val raw = gson.fromJson(json, JsonRuntime::class.java)
            ?: error("executable diagnostic runtime is empty")

        val scenarios = raw.scenarios.orEmpty().map { scenario ->
            val questions = scenario.questions.orEmpty().map { question ->
                val rawResponses = question.responses.orEmpty()
                val responses = ExecutableDiagnosticResponse.entries.associateWith { response ->
                    val branch = rawResponses[response.name]
                        ?: throw IllegalArgumentException(
                            "missing $response branch for ${scenario.id}/${question.key}"
                        )
                    ExecutableDiagnosticBranch(
                        meaning = requireRuntimeText(
                            branch.meaning,
                            "branch.meaning",
                        ),
                        nextQuestionKey = requireRuntimeText(
                            branch.nextQuestionKey,
                            "branch.nextQuestionKey",
                        ),
                        candidateCauseIds = branch.candidateCauseIds.orEmpty(),
                    )
                }

                ExecutableDiagnosticQuestion(
                    key = requireRuntimeText(question.key, "question.key"),
                    text = requireRuntimeText(question.text, "question.text"),
                    responses = responses,
                )
            }

            ExecutableDiagnosticScenario(
                legacyId = requireRuntimeText(scenario.legacyId, "scenario.legacyId"),
                id = requireRuntimeText(scenario.id, "scenario.id"),
                category = requireRuntimeText(scenario.category, "scenario.category"),
                title = requireRuntimeText(scenario.title, "scenario.title"),
                summary = requireRuntimeText(scenario.summary, "scenario.summary"),
                profileId = requireRuntimeText(scenario.profileId, "scenario.profileId"),
                applicableVariantIds = scenario.applicableVariantIds.orEmpty().toSet(),
                informationConfidence = requireRuntimeText(
                    scenario.informationConfidence,
                    "scenario.informationConfidence",
                ),
                severity = requireRuntimeText(scenario.severity, "scenario.severity"),
                applicability = requireRuntimeText(
                    scenario.applicability,
                    "scenario.applicability",
                ),
                startQuestionKey = scenario.startQuestionKey?.takeIf(String::isNotBlank),
                questions = questions,
                diagnosticCauses = scenario.diagnosticCauses.orEmpty().map { cause ->
                    ExecutableDiagnosticCause(
                        id = requireRuntimeText(cause.id, "cause.id"),
                        title = requireRuntimeText(cause.title, "cause.title"),
                        explanation = requireRuntimeText(
                            cause.explanation,
                            "cause.explanation",
                        ),
                        confidence = requireRuntimeText(
                            cause.confidence,
                            "cause.confidence",
                        ),
                    )
                },
                probableCauses = scenario.probableCauses.orEmpty(),
                immediateActions = scenario.immediateActions.orEmpty(),
                dangerSigns = scenario.dangerSigns.orEmpty(),
                checks = scenario.checks.orEmpty().map { check ->
                    ExecutableDiagnosticCheck(
                        title = requireRuntimeText(check.title, "check.title"),
                        action = requireRuntimeText(check.action, "check.action"),
                        expected = requireRuntimeText(check.expected, "check.expected"),
                        ifAbnormal = requireRuntimeText(
                            check.ifAbnormal,
                            "check.ifAbnormal",
                        ),
                        level = requireRuntimeText(check.level, "check.level"),
                    )
                },
                prohibited = scenario.prohibited.orEmpty(),
                stopConditions = scenario.stopConditions.orEmpty(),
                reportFields = scenario.reportFields.orEmpty(),
                relatedEquipment = scenario.relatedEquipment.orEmpty(),
                sourceNote = requireRuntimeText(scenario.sourceNote, "scenario.sourceNote"),
                observableSigns = scenario.observableSigns.orEmpty(),
                systemExplanation = scenario.systemExplanation.orEmpty(),
                operationalConsequences = scenario.operationalConsequences.orEmpty(),
                trainingNotes = scenario.trainingNotes.orEmpty(),
                feedbackPrompts = scenario.feedbackPrompts.orEmpty(),
                relatedScenarioIds = scenario.relatedScenarioIds.orEmpty(),
            )
        }

        val source = requireNotNull(raw.sourceSnapshot) {
            "sourceSnapshot is required"
        }

        return ExecutableDiagnosticRuntime(
            schemaVersion = raw.schemaVersion,
            modelId = requireRuntimeText(raw.modelId, "runtime.modelId"),
            sourceRepository = requireRuntimeText(
                source.repository,
                "sourceSnapshot.repository",
            ),
            sourceCommit = requireRuntimeText(
                source.commit,
                "sourceSnapshot.commit",
            ),
            runtimeSource = requireRuntimeText(
                source.runtimeSource,
                "sourceSnapshot.runtimeSource",
            ),
            scenarioCount = raw.scenarioCount,
            questionCount = raw.questionCount,
            responseEdgeCount = raw.responseEdgeCount,
            scenarios = scenarios,
        )
    }
}

data class DiagnosticAnswerRecord(
    val questionKey: String,
    val questionText: String,
    val response: ExecutableDiagnosticResponse,
    val conclusion: String,
)

data class ExecutableDiagnosticState(
    val scenarioId: String,
    val currentQuestionKey: String?,
    val answers: List<DiagnosticAnswerRecord> = emptyList(),
    val candidateScores: Map<String, Int> = emptyMap(),
)

data class ExecutableDiagnosticResult(
    val state: ExecutableDiagnosticState,
    val nextQuestion: ExecutableDiagnosticQuestion?,
    val leadingCauses: List<ExecutableDiagnosticCause>,
)

object ExecutableDiagnosticEngine {
    fun start(scenario: ExecutableDiagnosticScenario): ExecutableDiagnosticState =
        ExecutableDiagnosticState(
            scenarioId = scenario.id,
            currentQuestionKey = scenario.startQuestionKey,
        )

    fun answer(
        scenario: ExecutableDiagnosticScenario,
        state: ExecutableDiagnosticState,
        response: ExecutableDiagnosticResponse,
    ): ExecutableDiagnosticResult {
        require(state.scenarioId == scenario.id) {
            "diagnostic state belongs to ${state.scenarioId}, not ${scenario.id}"
        }

        val currentKey = state.currentQuestionKey
            ?: return result(scenario, state)
        val question = scenario.question(currentKey)
            ?: error("current diagnostic question is missing: ${scenario.id}/$currentKey")
        val branch = question.responses.getValue(response)

        val scores = state.candidateScores.toMutableMap()
        branch.candidateCauseIds.forEach { causeId ->
            scores[causeId] = (scores[causeId] ?: 0) + 1
        }

        val nextKey = branch.nextQuestionKey
            .takeUnless { it == DIAGNOSTIC_END_OF_FLOW }

        val updated = state.copy(
            currentQuestionKey = nextKey,
            answers = state.answers + DiagnosticAnswerRecord(
                questionKey = question.key,
                questionText = question.text,
                response = response,
                conclusion = branch.meaning,
            ),
            candidateScores = scores,
        )

        return result(scenario, updated)
    }

    fun result(
        scenario: ExecutableDiagnosticScenario,
        state: ExecutableDiagnosticState,
    ): ExecutableDiagnosticResult {
        require(state.scenarioId == scenario.id)

        val leading = scenario.diagnosticCauses
            .withIndex()
            .filter { (_, cause) -> (state.candidateScores[cause.id] ?: 0) > 0 }
            .sortedWith(
                compareByDescending<IndexedValue<ExecutableDiagnosticCause>> {
                    state.candidateScores[it.value.id] ?: 0
                }.thenBy { it.index }
            )
            .map { it.value }

        return ExecutableDiagnosticResult(
            state = state,
            nextQuestion = scenario.question(state.currentQuestionKey),
            leadingCauses = leading,
        )
    }
}

private data class JsonRuntime(
    val schemaVersion: Int = 0,
    val modelId: String? = null,
    val sourceSnapshot: JsonSourceSnapshot? = null,
    val scenarioCount: Int = 0,
    val questionCount: Int = 0,
    val responseEdgeCount: Int = 0,
    val scenarios: List<JsonScenario>? = null,
)

private data class JsonSourceSnapshot(
    val repository: String? = null,
    val commit: String? = null,
    val runtimeSource: String? = null,
)

private data class JsonScenario(
    val legacyId: String? = null,
    val id: String? = null,
    val category: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val profileId: String? = null,
    val applicableVariantIds: List<String>? = null,
    val informationConfidence: String? = null,
    val severity: String? = null,
    val applicability: String? = null,
    val startQuestionKey: String? = null,
    val questions: List<JsonQuestion>? = null,
    val diagnosticCauses: List<JsonCause>? = null,
    val probableCauses: List<String>? = null,
    val immediateActions: List<String>? = null,
    val dangerSigns: List<String>? = null,
    val checks: List<JsonCheck>? = null,
    val prohibited: List<String>? = null,
    val stopConditions: List<String>? = null,
    val reportFields: List<String>? = null,
    val relatedEquipment: List<String>? = null,
    val sourceNote: String? = null,
    val observableSigns: List<String>? = null,
    val systemExplanation: List<String>? = null,
    val operationalConsequences: List<String>? = null,
    val trainingNotes: List<String>? = null,
    val feedbackPrompts: List<String>? = null,
    val relatedScenarioIds: List<String>? = null,
)

private data class JsonQuestion(
    val key: String? = null,
    val text: String? = null,
    val responses: Map<String, JsonBranch>? = null,
)

private data class JsonBranch(
    val meaning: String? = null,
    val nextQuestionKey: String? = null,
    val candidateCauseIds: List<String>? = null,
)

private data class JsonCause(
    val id: String? = null,
    val title: String? = null,
    val explanation: String? = null,
    val confidence: String? = null,
)

private data class JsonCheck(
    val title: String? = null,
    val action: String? = null,
    val expected: String? = null,
    val ifAbnormal: String? = null,
    val level: String? = null,
)

private fun requireRuntimeText(value: String?, field: String): String {
    require(!value.isNullOrBlank()) { "$field must not be blank" }
    return value
}
