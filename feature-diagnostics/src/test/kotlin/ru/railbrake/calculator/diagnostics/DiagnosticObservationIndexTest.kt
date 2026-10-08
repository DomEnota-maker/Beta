package ru.railbrake.calculator.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticObservationIndexTest {
    private val fixture = """
        {
          "schemaVersion": 1,
          "modelId": "vl80s",
          "sourceSnapshot": {
            "commit": "3176d6ee"
          },
          "observationCount": 2,
          "observations": [
            {
              "id": "vl80s.obs.pantograph",
              "legacyId": "pantograph",
              "title": "Токоприёмник не поднимается",
              "kind": "BEHAVIOUR",
              "description": "Выбранный токоприёмник не поднимается.",
              "synonyms": ["пантограф", "рога"],
              "scenarioIds": ["vl80s.diag.pantograph-no-rise"],
              "equipmentIds": ["VL-EQ-HV-002"],
              "confidence": "REQUIRES_VARIANT_CHECK",
              "variantNote": "Проверить исполнение."
            },
            {
              "id": "vl80s.obs.air-leak",
              "legacyId": "air-leak",
              "title": "Сильное шипение воздуха",
              "kind": "SOUND",
              "description": "Слышен непрерывный выход воздуха.",
              "synonyms": ["шипит", "свист"],
              "scenarioIds": ["vl80s.diag.brake-pipe-leak"],
              "equipmentIds": ["VL-EQ-PN-001"],
              "confidence": "REQUIRES_VARIANT_CHECK",
              "variantNote": "Проверить исполнение."
            }
          ]
        }
    """.trimIndent()

    @Test
    fun searchPreservesDonorSubstringSemantics() {
        val index = DiagnosticObservationIndexJsonLoader().parse(fixture)

        assertEquals(
            listOf("vl80s.obs.pantograph"),
            index.search("РОГА").map { it.id },
        )
        assertEquals(
            listOf("vl80s.obs.air-leak"),
            index.search("непрерывный выход").map { it.id },
        )
        assertEquals(2, index.search("").size)
    }

    @Test
    fun observationCarriesCanonicalTargetsOnly() {
        val observation = DiagnosticObservationIndexJsonLoader()
            .parse(fixture)
            .search("пантограф")
            .single()

        assertTrue(observation.scenarioIds.single().startsWith("vl80s.diag."))
        assertTrue(observation.equipmentIds.single().startsWith("VL-EQ-"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateObservationIdFailsClosed() {
        DiagnosticObservationIndexJsonLoader().parse(
            fixture.replace(
                "\"vl80s.obs.air-leak\"",
                "\"vl80s.obs.pantograph\"",
            )
        )
    }
}
