package ru.railbrake.calculator.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.railbrake.calculator.domain.CanonicalId
import ru.railbrake.calculator.domain.ContentTarget
import ru.railbrake.calculator.domain.ContentType
import ru.railbrake.calculator.domain.ModelId
import ru.railbrake.calculator.domain.RuntimeContext
import ru.railbrake.calculator.domain.VariantId

class ContentPackJsonLoaderTest {
    @Test
    fun parsesActiveAndCandidateEntriesAndFailsClosedAtResolution() {
        val json = """
            {
              "manifest": {
                "schemaVersion": 1,
                "packId": "vl80s.test",
                "packVersion": "0.1.0",
                "family": "ELECTRIC",
                "modelIds": ["vl80s"],
                "variantIds": ["vl80s-general"],
                "locale": "ru-RU",
                "entries": ["VL-EQ-HV-002", "vl80s.diag.pantograph-no-rise"],
                "requiresRuntime": "1.1.0",
                "sourceCatalogVersion": "test",
                "checksums": {}
              },
              "entries": [
                {
                  "id": "VL-EQ-HV-002",
                  "type": "EQUIPMENT",
                  "owner": {"kind": "MODEL", "modelId": "vl80s"},
                  "applicability": {
                    "modelIds": ["vl80s"],
                    "variantIds": ["vl80s-general"]
                  },
                  "title": "Токоприёмник",
                  "links": [
                    {
                      "type": "RELATED_SCENARIO",
                      "targetId": "vl80s.diag.pantograph-no-rise"
                    }
                  ]
                },
                {
                  "id": "vl80s.diag.pantograph-no-rise",
                  "aliases": ["pantograph-no-rise"],
                  "type": "DIAGNOSTIC_SCENARIO",
                  "owner": {"kind": "MODEL", "modelId": "vl80s"},
                  "applicability": {
                    "modelIds": ["vl80s"],
                    "variantIds": ["vl80s-general"]
                  },
                  "publicationStatus": "CANDIDATE",
                  "actionDisposition": "CONDITIONAL_ACTION",
                  "title": "Токоприёмник не поднимается"
                }
              ]
            }
        """.trimIndent()

        val pack = ContentPackJsonLoader().parse(json)
        val registry = ContentRegistry()

        assertTrue(registry.install(pack).isEmpty())
        assertTrue(registry.validateGlobalLinks().isEmpty())
        assertEquals("Токоприёмник", registry.find(CanonicalId("VL-EQ-HV-002"))?.title)

        val context = RuntimeContext(
            workingModelId = ModelId("vl80s"),
            viewedModelId = ModelId("vl80s"),
            activeVariantId = VariantId("vl80s-general"),
        )
        assertTrue(
            registry.resolve(
                ContentTarget(CanonicalId("VL-EQ-HV-002"), ContentType.EQUIPMENT),
                context,
            ) is ResolveResult.Found
        )
        assertEquals(
            ResolveResult.NotPublished,
            registry.resolve(
                ContentTarget(
                    CanonicalId("pantograph-no-rise"),
                    ContentType.DIAGNOSTIC_SCENARIO,
                ),
                context,
            )
        )
    }
}
