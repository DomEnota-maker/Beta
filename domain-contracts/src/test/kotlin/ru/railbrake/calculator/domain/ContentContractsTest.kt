package ru.railbrake.calculator.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentContractsTest {
    @Test
    fun modelApplicabilityFailsClosedWithoutContentModel() {
        val scope = Applicability(modelIds = setOf(ModelId("vl80s")))

        assertFalse(scope.matches(RuntimeContext()))
        assertFalse(scope.matches(RuntimeContext(workingModelId = ModelId("ermak"))))
        assertTrue(scope.matches(RuntimeContext(workingModelId = ModelId("vl80s"))))
    }

    @Test
    fun viewedModelIsSeparateFromAssistantWorkingModel() {
        val context = RuntimeContext(
            workingModelId = ModelId("vl80s"),
            viewedModelId = ModelId("ermak"),
        )

        assertEquals(ModelId("vl80s"), context.workingModelId)
        assertEquals(ModelId("ermak"), context.contentModelId)
        assertTrue(
            Applicability(modelIds = setOf(ModelId("ermak")))
                .matches(context)
        )
    }

    @Test
    fun linkTypeHasSingleExpectedContentType() {
        assertEquals(ContentType.DIAGNOSTIC_SCENARIO, LinkType.RELATED_SCENARIO.expectedTargetType())
        assertEquals(ContentType.EQUIPMENT, LinkType.EQUIPMENT.expectedTargetType())
        assertEquals(ContentType.ATLAS_SCHEME, LinkType.ATLAS_SCHEME.expectedTargetType())
        assertEquals(ContentType.ACCEPTANCE_ITEM, LinkType.ACCEPTANCE_ITEM.expectedTargetType())
        assertEquals(ContentType.TECHNICAL_DATA, LinkType.TECHNICAL_DATA.expectedTargetType())
    }

    @Test(expected = IllegalArgumentException::class)
    fun canonicalIdCannotBeBlank() {
        CanonicalId(" ")
    }
}
