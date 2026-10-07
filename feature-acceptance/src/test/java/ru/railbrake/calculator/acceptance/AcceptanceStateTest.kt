package ru.railbrake.calculator.acceptance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AcceptanceStateTest {
    @Test
    fun summaryKeepsFourUserFacingStatesSeparate() {
        val summary = acceptanceSummary(
            listOf(
                AcceptanceItemState(AcceptanceCheckState.CHECKED),
                AcceptanceItemState(AcceptanceCheckState.NOTE, "Осмотр"),
                AcceptanceItemState(AcceptanceCheckState.NOT_APPLICABLE),
                AcceptanceItemState(AcceptanceCheckState.NOT_CHECKED),
            )
        )

        assertEquals(4, summary.total)
        assertEquals(1, summary.checked)
        assertEquals(1, summary.notes)
        assertEquals(1, summary.notApplicable)
        assertEquals(1, summary.notChecked)
        assertFalse(summary.complete)
    }

    @Test
    fun completedSingleItemIsComplete() {
        assertTrue(
            acceptanceSummary(
                listOf(AcceptanceItemState(AcceptanceCheckState.CHECKED))
            ).complete
        )
    }
}
