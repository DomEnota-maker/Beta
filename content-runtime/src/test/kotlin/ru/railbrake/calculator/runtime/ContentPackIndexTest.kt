package ru.railbrake.calculator.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class ContentPackIndexTest {
    @Test
    fun parsesModelOwnedPackIndex() {
        val index = ContentPackIndexJsonLoader().parse(
            """
            {
              "schemaVersion": 1,
              "modelId": "vl80s",
              "packs": [
                "electric/vl80s/atlas/a.pack.json",
                "electric/vl80s/technical-data/b.pack.json"
              ]
            }
            """.trimIndent()
        )

        assertEquals("vl80s", index.modelId)
        assertEquals(2, index.packs.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicatePackPathsFailClosed() {
        ContentPackIndex(
            schemaVersion = 1,
            modelId = "vl80s",
            packs = listOf("same.json", "same.json"),
        )
    }
}
