package ru.railbrake.calculator.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentContractsTest {
    @Test
    fun modelApplicabilityFailsClosedWithoutActiveModel() {
        val scope = Applicability(modelIds = setOf(ModelId("vl80s")))

        assertFalse(scope.matches(RuntimeContext()))
        assertFalse(scope.matches(RuntimeContext(activeModelId = ModelId("ermak"))))
        assertTrue(scope.matches(RuntimeContext(activeModelId = ModelId("vl80s"))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun canonicalIdCannotBeBlank() {
        CanonicalId(" ")
    }
}
