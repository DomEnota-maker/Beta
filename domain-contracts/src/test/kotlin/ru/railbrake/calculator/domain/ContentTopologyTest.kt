package ru.railbrake.calculator.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ContentTopologyTest {
    @Test
    fun recommendedDiagnosticsCannotCarryExtendedClass() {
        val placement = DiagnosticPlacement(DiagnosticCorpus.RECOMMENDED)
        assertEquals(null, placement.extendedClass)
    }

    @Test
    fun extendedDiagnosticsRequireExplicitClass() {
        val placement = DiagnosticPlacement(
            corpus = DiagnosticCorpus.EXTENDED,
            extendedClass = ExtendedDiagnosticClass.HISTORICAL_TRAINING,
        )
        assertEquals(ExtendedDiagnosticClass.HISTORICAL_TRAINING, placement.extendedClass)
    }

    @Test(expected = IllegalArgumentException::class)
    fun extendedDiagnosticsFailWithoutClass() {
        DiagnosticPlacement(DiagnosticCorpus.EXTENDED)
    }
}
