package ru.railbrake.calculator.atlas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.railbrake.calculator.domain.ActionDisposition
import ru.railbrake.calculator.domain.Applicability
import ru.railbrake.calculator.domain.CanonicalId
import ru.railbrake.calculator.domain.ContentEntry
import ru.railbrake.calculator.domain.ContentOwner
import ru.railbrake.calculator.domain.ContentPackManifest
import ru.railbrake.calculator.domain.ContentType
import ru.railbrake.calculator.domain.ModelId
import ru.railbrake.calculator.domain.PackId
import ru.railbrake.calculator.domain.ProvenanceClass
import ru.railbrake.calculator.domain.SourceStatus
import ru.railbrake.calculator.runtime.ContentPack
import ru.railbrake.calculator.runtime.ContentRegistry

class AtlasSchemeGraphTest {
    @Test
    fun graphTargetsRegisteredEquipment() {
        val json = """
            {
              "schemes": [
                {
                  "id": "VL-SCH-TEST",
                  "title": "Тестовая схема",
                  "schemeType": "electrical",
                  "status": "TEST",
                  "profiles": ["vl80s-general"],
                  "systems": ["VL-SYS-HV"],
                  "sourceRefs": ["VL-SRC-001"],
                  "nodes": [
                    {"equipmentId": "VL-EQ-HV-002", "label": "Токоприёмник"},
                    {"virtualNodeId": "VL-VIRT-LINE", "label": "Контактная сеть"}
                  ],
                  "edges": [
                    {"from": "VL-VIRT-LINE", "to": "VL-EQ-HV-002", "kind": "energy_flow"}
                  ]
                }
              ]
            }
        """.trimIndent()

        val registry = ContentRegistry()
        val equipment = ContentEntry(
            id = CanonicalId("VL-EQ-HV-002"),
            type = ContentType.EQUIPMENT,
            owner = ContentOwner.Model(ModelId("vl80s")),
            applicability = Applicability(modelIds = setOf(ModelId("vl80s"))),
            provenance = ProvenanceClass.UNKNOWN,
            sourceStatus = SourceStatus.UNKNOWN,
            actionDisposition = ActionDisposition.INFORMATION_ONLY,
            title = "Токоприёмник",
        )
        val pack = ContentPack(
            manifest = ContentPackManifest(
                schemaVersion = 1,
                packId = PackId("vl80s.test"),
                packVersion = "1",
                family = "ELECTRIC",
                modelIds = setOf(ModelId("vl80s")),
                variantIds = emptySet(),
                locale = "ru-RU",
                entries = setOf(equipment.id),
                requiresRuntime = "1",
                sourceCatalogVersion = "1",
                checksums = emptyMap(),
            ),
            entries = listOf(equipment),
        )
        assertTrue(registry.install(pack).isEmpty())

        val atlas = AtlasSchemeJsonLoader().parse(json)
        assertEquals("VL-SCH-TEST", atlas.schemes.single().id.value)
        assertTrue(AtlasGraphValidator(registry).validate(atlas).isEmpty())
    }
}
