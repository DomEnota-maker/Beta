package ru.railbrake.calculator.export

import com.google.gson.GsonBuilder
import ru.railbrake.calculator.core.Vl80sObservationCatalog
import java.io.File

private const val DONOR_SHA = "3176d6ee228f0ed4b78371f45209e10c1c724eff"

fun main() {
    val observations = Vl80sObservationCatalog.observations
    require(observations.map { it.id }.distinct().size == observations.size) {
        "duplicate observation id in donor"
    }

    observations.forEach { observation ->
        require(observation.id.isNotBlank())
        require(observation.title.isNotBlank())
        require(observation.description.isNotBlank())
        require(
            observation.scenarioIds.size == observation.scenarioIds.distinct().size
        ) {
            "duplicate scenario reference in observation ${observation.id}"
        }
        require(
            observation.equipmentIds.size == observation.equipmentIds.distinct().size
        ) {
            "duplicate equipment reference in observation ${observation.id}"
        }
    }

    val document = linkedMapOf<String, Any?>(
        "schemaVersion" to 1,
        "sourceSnapshot" to linkedMapOf(
            "repository" to "DomEnota-maker/Test-",
            "commit" to DONOR_SHA,
            "source" to
                "app/src/main/java/ru/railbrake/calculator/core/Vl80sObservationCatalog.kt",
        ),
        "observationCount" to observations.size,
        "observations" to observations.map { observation ->
            linkedMapOf(
                "legacyId" to observation.id,
                "title" to observation.title,
                "kind" to observation.kind.name,
                "description" to observation.description,
                "synonyms" to observation.synonyms,
                "legacyScenarioIds" to observation.scenarioIds,
                "legacyEquipmentIds" to observation.equipmentIds,
                "confidence" to observation.confidence.name,
                "variantNote" to observation.variantNote,
            )
        },
    )

    val output = File(
        System.getProperty("user.dir"),
        "build/vl80s-observation-donor/observations.json",
    )
    output.parentFile.mkdirs()
    output.writeText(
        GsonBuilder().setPrettyPrinting().create().toJson(document) + "\n"
    )

    println(
        "VL80S_OBSERVATION_EXPORT_PASS observations=${observations.size}"
    )
}
