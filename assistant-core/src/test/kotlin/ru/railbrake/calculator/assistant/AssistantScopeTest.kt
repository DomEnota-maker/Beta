package ru.railbrake.calculator.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.railbrake.calculator.domain.ModelId

class AssistantScopeTest {
    private val vl80s = ModelId("vl80s")
    private val ermak = ModelId("ermak")

    @Test
    fun defaultScopeIndexesCommonAndExactlyOneWorkingModel() {
        val result = AssistantScopeResolver.resolve(workingModelId = vl80s)
        assertTrue(result is AssistantScopeResult.Ready)

        val scope = (result as AssistantScopeResult.Ready).scope
        assertEquals(AssistantQueryMode.WORKING_MODEL, scope.mode)
        assertEquals(vl80s, scope.modelId)
        assertEquals(
            setOf(
                AssistantIndexSource.Common,
                AssistantIndexSource.Model(vl80s),
            ),
            scope.indexSources,
        )
    }

    @Test
    fun explicitOtherModelQueryDoesNotChangeWorkingModel() {
        val result = AssistantScopeResolver.resolve(
            workingModelId = vl80s,
            explicitModelId = ermak,
        )
        val scope = (result as AssistantScopeResult.Ready).scope

        assertEquals(AssistantQueryMode.EXPLICIT_MODEL, scope.mode)
        assertEquals(ermak, scope.modelId)
        assertEquals(vl80s, scope.workingModelId)
        assertEquals(
            setOf(
                AssistantIndexSource.Common,
                AssistantIndexSource.Model(ermak),
            ),
            scope.indexSources,
        )
    }

    @Test
    fun missingWorkingModelFailsClosedForNormalQuery() {
        assertEquals(
            AssistantScopeResult.MissingWorkingModel,
            AssistantScopeResolver.resolve(workingModelId = null),
        )
    }

    @Test
    fun explicitModelCanBeQueriedWithoutCreatingAllModelsMode() {
        val result = AssistantScopeResolver.resolve(
            workingModelId = null,
            explicitModelId = ermak,
        )
        val scope = (result as AssistantScopeResult.Ready).scope

        assertEquals(2, scope.indexSources.size)
        assertTrue(AssistantIndexSource.Model(ermak) in scope.indexSources)
    }
}
