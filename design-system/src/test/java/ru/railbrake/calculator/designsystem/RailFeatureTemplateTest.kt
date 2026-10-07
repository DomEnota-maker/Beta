package ru.railbrake.calculator.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.railbrake.calculator.navigation.FeatureDestination

class RailFeatureTemplateTest {
    @Test
    fun everyFeatureDestinationUsesSharedTemplateContract() {
        assertEquals(RailFeatureTemplate.ACCEPTANCE, FeatureDestination.ACCEPTANCE.visualTemplate())
        assertEquals(RailFeatureTemplate.ATLAS, FeatureDestination.ATLAS.visualTemplate())
        assertEquals(RailFeatureTemplate.DIAGNOSTICS, FeatureDestination.DIAGNOSTICS.visualTemplate())
        assertEquals(RailFeatureTemplate.TECHNICAL_DATA, FeatureDestination.TECHNICAL_DATA.visualTemplate())
        assertEquals(RailFeatureTemplate.REFERENCE, FeatureDestination.REFERENCE.visualTemplate())
        assertEquals(RailFeatureTemplate.FIRST_AID, FeatureDestination.FIRST_AID.visualTemplate())
        assertEquals(RailFeatureTemplate.SAFETY, FeatureDestination.SAFETY.visualTemplate())
    }
}
