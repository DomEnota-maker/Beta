package ru.railbrake.calculator.atlas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.railbrake.calculator.domain.CanonicalId
import ru.railbrake.calculator.domain.ContentType

class ElectricalFunctionalFlowTest {
    private val nodes = listOf(
        ElectricalFunctionalNode(
            id = "a",
            equipmentId = CanonicalId("VL-EQ-HV-002"),
            x = 0f,
            y = 0f,
            width = 10f,
            height = 10f,
            info = ElectricalFunctionalInfo(
                title = "A",
                subtitle = "A",
                purpose = "A",
                triggeredBy = "A",
                affects = "A",
                links = "A",
                principle = "",
                faultSigns = "",
                checks = "",
            ),
        ),
        ElectricalFunctionalNode(
            id = "b",
            equipmentId = null,
            x = 20f,
            y = 0f,
            width = 10f,
            height = 10f,
            info = ElectricalFunctionalInfo(
                title = "B",
                subtitle = "B",
                purpose = "B",
                triggeredBy = "B",
                affects = "B",
                links = "B",
                principle = "",
                faultSigns = "",
                checks = "",
            ),
        ),
    )

    private val scenario = ElectricalFunctionalScenario(
        id = "test",
        title = "Test",
        summary = "Test",
        canvasWidth = 100f,
        canvasHeight = 100f,
        nodes = nodes,
        edges = listOf(
            ElectricalFunctionalEdge("a", "b", 0),
            ElectricalFunctionalEdge("b", "a", 1),
        ),
        steps = listOf(
            ElectricalFunctionalStep("1", "one"),
            ElectricalFunctionalStep("2", "two"),
        ),
    )

    @Test
    fun sessionActivatesEdgesByStep() {
        val first = ElectricalFunctionalSession(scenario)
        assertEquals(1, first.activeEdges.size)
        assertTrue(first.hasNext)
        assertFalse(first.hasPrevious)

        val second = first.next()
        assertEquals(2, second.activeEdges.size)
        assertFalse(second.hasNext)
        assertTrue(second.hasPrevious)
    }

    @Test
    fun onlyCanonicalNodeCreatesEquipmentTarget() {
        val canonical = nodes.first().equipmentTarget()
        assertEquals(CanonicalId("VL-EQ-HV-002"), canonical?.id)
        assertEquals(ContentType.EQUIPMENT, canonical?.expectedType)
        assertNull(nodes.last().equipmentTarget())
    }
}
