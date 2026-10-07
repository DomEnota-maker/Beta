package ru.railbrake.calculator.navigation

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.railbrake.calculator.domain.ContentType

class NavigationContractsTest {
    @Test
    fun contentTypesMapToOwningFeatureWithoutFeatureImports() {
        assertEquals(FeatureDestination.DIAGNOSTICS, ContentType.DIAGNOSTIC_SCENARIO.featureDestination())
        assertEquals(FeatureDestination.ATLAS, ContentType.EQUIPMENT.featureDestination())
        assertEquals(FeatureDestination.ATLAS, ContentType.ATLAS_SCHEME.featureDestination())
        assertEquals(FeatureDestination.ACCEPTANCE, ContentType.ACCEPTANCE_ITEM.featureDestination())
        assertEquals(FeatureDestination.REFERENCE, ContentType.KNOWLEDGE.featureDestination())
    }
}
