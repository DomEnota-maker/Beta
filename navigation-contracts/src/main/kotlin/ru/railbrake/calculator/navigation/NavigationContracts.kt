package ru.railbrake.calculator.navigation

import ru.railbrake.calculator.domain.CanonicalId
import ru.railbrake.calculator.domain.ContentType
import ru.railbrake.calculator.domain.ModelId

enum class FeatureDestination {
    DIAGNOSTICS,
    ATLAS,
    ACCEPTANCE,
    TECHNICAL_DATA,
    REFERENCE,
    FIRST_AID,
    SAFETY,
}

fun ContentType.featureDestination(): FeatureDestination = when (this) {
    ContentType.DIAGNOSTIC_SCENARIO -> FeatureDestination.DIAGNOSTICS
    ContentType.EQUIPMENT,
    ContentType.ATLAS_SCHEME -> FeatureDestination.ATLAS
    ContentType.ACCEPTANCE_ITEM -> FeatureDestination.ACCEPTANCE
    ContentType.TECHNICAL_DATA -> FeatureDestination.TECHNICAL_DATA
    ContentType.KNOWLEDGE,
    ContentType.SOURCE -> FeatureDestination.REFERENCE
    ContentType.FIRST_AID -> FeatureDestination.FIRST_AID
    ContentType.SAFETY -> FeatureDestination.SAFETY
}

data class NavigationTarget(
    val id: CanonicalId,
    val contentType: ContentType,
    val destination: FeatureDestination,
    val viewedModelId: ModelId?,
)
