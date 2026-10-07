package ru.railbrake.calculator.calculations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrakeCalculatorTest {
    @Test
    fun exactlyTenTonsUsesTenOrMoreByDefault() {
        val result = BrakeCalculator.calculateMass(
            MassCalculationInput(
                massTons = 1000.0,
                axleCount = 100,
                slopePermille = 0.0,
            )
        )

        assertTrue(result.isExactlyTenTons)
        assertTrue(result.heavyCategory)
        assertEquals(2, result.requiredShoes)
    }

    @Test
    fun wagonAxlesUsesCheckedArithmetic() {
        assertEquals(36, BrakeCalculator.wagonAxles(4, 2, 1, 0))
    }

    @Test
    fun lowSlopeAppendixUsesBasePair() {
        val result = BrakeCalculator.calculateAppendix12(
            Appendix12Input(
                axleCount = 200,
                profileMode = ProfileMode.NORMAL,
                slopePermille = 0.5,
                formula = null,
                oilyRails = false,
                windSpeedMs = 0.0,
                windDirectionMatchesPossibleMovement = false,
                availableShoes = 2,
                axlesPerHandBrakeUnit = 4,
            )
        )

        assertTrue(result.isLowSlopeRule)
        assertEquals(2, result.totalRequiredShoes)
    }
}
