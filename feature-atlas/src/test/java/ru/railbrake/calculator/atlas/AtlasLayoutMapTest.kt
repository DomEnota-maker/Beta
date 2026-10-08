package ru.railbrake.calculator.atlas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.railbrake.calculator.domain.CanonicalId
import ru.railbrake.calculator.domain.ContentType

class AtlasLayoutMapTest {
    @Test
    fun hitTestChoosesSmallestOverlappingHotspot() {
        val large = AtlasLayoutHotspot(
            id = "large",
            title = "Большая зона",
            subtitle = "Зона",
            details = "Описание",
            learnMore = emptyList(),
            bounds = NormalizedBounds(0f, 0f, 0.8f, 0.8f),
            equipmentId = null,
        )
        val small = AtlasLayoutHotspot(
            id = "small",
            title = "Малая зона",
            subtitle = "Оборудование",
            details = "Описание",
            learnMore = emptyList(),
            bounds = NormalizedBounds(0.2f, 0.2f, 0.4f, 0.4f),
            equipmentId = CanonicalId("VL-EQ-TR-004"),
        )
        val layout = AtlasLayoutMap(
            id = "test",
            modelId = "vl80s",
            title = "Test",
            summary = "Test",
            spatialClaim = "TRAINING_REFERENCE_NOT_EXACT_MOUNTING",
            hotspots = listOf(large, small),
        )

        assertEquals("small", layout.hitTest(0.3f, 0.3f)?.id)
        assertEquals("large", layout.hitTest(0.7f, 0.7f)?.id)
        assertNull(layout.hitTest(0.95f, 0.95f))
    }

    @Test
    fun canonicalEquipmentHotspotProducesTypedTarget() {
        val hotspot = AtlasLayoutHotspot(
            id = "transformer",
            title = "Тяговый трансформатор",
            subtitle = "ОДЦЭ",
            details = "Описание",
            learnMore = emptyList(),
            bounds = NormalizedBounds(0.1f, 0.1f, 0.2f, 0.2f),
            equipmentId = CanonicalId("VL-EQ-TR-004"),
        )

        val target = hotspot.equipmentTarget()
        assertEquals(CanonicalId("VL-EQ-TR-004"), target?.id)
        assertEquals(ContentType.EQUIPMENT, target?.expectedType)
    }
}
