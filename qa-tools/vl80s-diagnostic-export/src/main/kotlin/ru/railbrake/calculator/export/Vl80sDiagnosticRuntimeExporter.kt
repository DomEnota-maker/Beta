package ru.railbrake.calculator.export

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import ru.railbrake.calculator.core.DiagnosticRepository
import ru.railbrake.calculator.core.DiagnosticResponse
import java.io.File

private const val END = "__end__"
private const val DONOR_SHA = "3176d6ee228f0ed4b78371f45209e10c1c724eff"

fun main() {
    val root = File(System.getProperty("user.dir"))
    val relationPath = File(
        root,
        "content-packs/electric/vl80s/diagnostics/relation-graph.json"
    )
    val donorReportPath = File(root, "build/vl80s-donor/report.json")
    val outputPath = File(
        root,
        "content-packs/electric/vl80s/diagnostics/runtime/executable-flow.json"
    )

    val relationRoot = JsonParser.parseString(relationPath.readText()).asJsonObject
    val scenarioIdMapJson = relationRoot.getAsJsonObject("scenarioIdMap")
    val scenarioIdMap = scenarioIdMapJson.entrySet().associate { (legacy, value) ->
        legacy to value.asString
    }

    val scenarios = DiagnosticRepository.scenarios
    require(scenarios.map { it.id }.distinct().size == scenarios.size) {
        "donor runtime contains duplicate scenario IDs"
    }

    val runtimeLegacyIds = scenarios.map { it.id }.toSet()
    val missingCanonical = runtimeLegacyIds - scenarioIdMap.keys
    require(missingCanonical.isEmpty()) {
        "runtime scenarios missing canonical IDs: ${missingCanonical.sorted()}"
    }

    var questionCount = 0
    var edgeCount = 0

    val exported = scenarios.map { scenario ->
        val questionKeys = scenario.questions.map { it.key }
        require(questionKeys.size == questionKeys.distinct().size) {
            "duplicate question key in ${scenario.id}"
        }
        require(questionKeys.none { it.isBlank() }) {
            "blank question key in ${scenario.id}"
        }

        val causes = scenario.diagnosticCauses.map { cause ->
            linkedMapOf<String, Any?>(
                "id" to cause.id,
                "title" to cause.title,
                "explanation" to cause.explanation,
                "confidence" to cause.confidence.name,
            )
        }

        val questions = scenario.questions.map { question ->
            questionCount += 1
            val responses = linkedMapOf<String, Any?>()
            for (response in DiagnosticResponse.entries) {
                edgeCount += 1
                val next = DiagnosticRepository.nextQuestion(
                    scenario,
                    question.key,
                    response,
                )?.key ?: END

                val candidateCauseIds =
                    DiagnosticRepository.candidateCauseIds(question, response)

                responses[response.name] = linkedMapOf(
                    "meaning" to DiagnosticRepository.meaning(question, response),
                    "nextQuestionKey" to next,
                    "candidateCauseIds" to candidateCauseIds,
                )
            }

            linkedMapOf<String, Any?>(
                "key" to question.key,
                "text" to question.text,
                "responses" to responses,
            )
        }

        val canonicalRelated = scenario.relatedScenarioIds.map { related ->
            scenarioIdMap[related]
                ?: error(
                    "related scenario ${scenario.id} -> $related " +
                        "has no canonical ID"
                )
        }

        linkedMapOf<String, Any?>(
            "legacyId" to scenario.id,
            "id" to scenarioIdMap.getValue(scenario.id),
            "category" to scenario.category,
            "title" to scenario.title,
            "summary" to scenario.summary,
            "profileId" to scenario.profileId,
            "applicableVariantIds" to scenario.applicableVariantIds.sorted(),
            "informationConfidence" to scenario.informationConfidence.name,
            "severity" to scenario.severity.name,
            "applicability" to scenario.applicability,
            "startQuestionKey" to scenario.questions.firstOrNull()?.key,
            "questions" to questions,
            "diagnosticCauses" to causes,
            "probableCauses" to scenario.probableCauses,
            "immediateActions" to scenario.immediateActions,
            "dangerSigns" to scenario.dangerSigns,
            "checks" to scenario.checks.map { check ->
                linkedMapOf(
                    "title" to check.title,
                    "action" to check.action,
                    "expected" to check.expected,
                    "ifAbnormal" to check.ifAbnormal,
                    "level" to check.level.name,
                )
            },
            "prohibited" to scenario.prohibited,
            "stopConditions" to scenario.stopConditions,
            "reportFields" to scenario.reportFields,
            "relatedEquipment" to scenario.relatedEquipment,
            "sourceNote" to scenario.sourceNote,
            "observableSigns" to scenario.observableSigns,
            "systemExplanation" to scenario.systemExplanation,
            "operationalConsequences" to scenario.operationalConsequences,
            "trainingNotes" to scenario.trainingNotes,
            "feedbackPrompts" to scenario.feedbackPrompts,
            "relatedScenarioIds" to canonicalRelated,
        )
    }

    val donorReport = JsonParser.parseString(
        donorReportPath.readText()
    ).asJsonObject
    require(donorReport.get("commit").asString == DONOR_SHA)

    val sourceFiles = donorReport
        .getAsJsonObject("files")
        .entrySet()
        .filter { it.key.endsWith(".kt") }
        .associate { (name, value) ->
            name to value.asJsonObject.get("verifiedBlobSha").asString
        }

    val document = linkedMapOf<String, Any?>(
        "schemaVersion" to 1,
        "modelId" to "vl80s",
        "sourceSnapshot" to linkedMapOf(
            "repository" to "DomEnota-maker/Test-",
            "commit" to DONOR_SHA,
            "runtimeSource" to "DiagnosticRepository.scenarios",
            "sourceFiles" to sourceFiles,
        ),
        "runtimeSemantics" to linkedMapOf(
            "responses" to DiagnosticResponse.entries.map { it.name },
            "endOfFlow" to END,
            "transitionExport" to
                "effective DiagnosticRepository.nextQuestion result after all enrich layers",
        ),
        "scenarioCount" to exported.size,
        "questionCount" to questionCount,
        "responseEdgeCount" to edgeCount,
        "scenarios" to exported,
    )

    outputPath.parentFile.mkdirs()
    outputPath.writeText(
        GsonBuilder().setPrettyPrinting().create().toJson(document) + "\n"
    )

    println(
        "VL80S_DIAGNOSTIC_RUNTIME_EXPORT_PASS " +
            "scenarios=${exported.size} questions=$questionCount edges=$edgeCount"
    )
}
