package ru.railbrake.calculator.atlas

import org.junit.Assert.assertEquals
import org.junit.Test

class AtlasFeatureIndexTest {
    @Test
    fun loaderReadsCanonicalLayoutPath() {
        val json = """
            {
              "schemaVersion": 1,
              "modelId": "vl80s",
              "layoutMaps": [
                "electric/vl80s/atlas/interactive/layout-hotspots.json"
              ],
              "stepwiseFlows": [
                "electric/vl80s/atlas/interactive/pneumatic-flows.json"
              ],
              "functionalFlows": [
                "electric/vl80s/atlas/interactive/electrical-functional-flows.json"
              ]
            }
        """.trimIndent()

        val index = AtlasFeatureIndexJsonLoader().parse(json)

        assertEquals("vl80s", index.modelId)
        assertEquals(
            "electric/vl80s/atlas/interactive/layout-hotspots.json",
            index.layoutMaps.single(),
        )
    }
}
