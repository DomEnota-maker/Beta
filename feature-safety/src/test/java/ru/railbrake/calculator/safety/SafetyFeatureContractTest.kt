package ru.railbrake.calculator.safety

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.railbrake.calculator.domain.ContentType
import ru.railbrake.calculator.navigation.FeatureDestination
import ru.railbrake.calculator.navigation.featureDestination

class SafetyFeatureContractTest {
    @Test
    fun safetyContentUsesSharedSafetyDestination() {
        assertEquals(
            FeatureDestination.SAFETY,
            ContentType.SAFETY.featureDestination(),
        )
    }
}
