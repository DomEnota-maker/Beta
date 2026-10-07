package ru.railbrake.calculator.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.railbrake.calculator.domain.ModelProfileSelectionResult
import ru.railbrake.calculator.domain.VariantId
import ru.railbrake.calculator.domain.select

class ModelProfileCatalogTest {
    @Test
    fun defaultUnknownProfileIsExplicitAndSelectable() {
        val json = """
            {
              "profileId": "vl80s",
              "resolution": "section_aware_feature_based",
              "sectionAware": true,
              "defaultVariantId": "vl80s_unknown",
              "physicalBuckets": [
                {
                  "id": "vl80s_unknown",
                  "displayTitle": "Исполнение не определено",
                  "range": "unknown",
                  "confidence": "FALLBACK",
                  "rules": ["no_exact_scheme_assumption"]
                },
                {
                  "id": "vl80s_697_1260",
                  "displayTitle": "Секции №697–1260",
                  "range": "697-1260",
                  "confidence": "CONFIRMED_GENERAL"
                }
              ]
            }
        """.trimIndent()

        val catalog = ModelProfileCatalogJsonLoader().parse(json)

        assertEquals(VariantId("vl80s_unknown"), catalog.defaultVariantId)
        assertTrue(
            catalog.select(VariantId("vl80s_unknown")) is
                ModelProfileSelectionResult.Selected
        )
        assertEquals(
            ModelProfileSelectionResult.UnknownVariant,
            catalog.select(VariantId("vl80s-general")),
        )
    }
}
