package ru.railbrake.calculator.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import ru.railbrake.calculator.navigation.FeatureDestination

enum class RailFeatureTemplate {
    ACCEPTANCE,
    ATLAS,
    DIAGNOSTICS,
    TECHNICAL_DATA,
    REFERENCE,
    FIRST_AID,
    SAFETY,
}

fun FeatureDestination.visualTemplate(): RailFeatureTemplate = when (this) {
    FeatureDestination.ACCEPTANCE -> RailFeatureTemplate.ACCEPTANCE
    FeatureDestination.ATLAS -> RailFeatureTemplate.ATLAS
    FeatureDestination.DIAGNOSTICS -> RailFeatureTemplate.DIAGNOSTICS
    FeatureDestination.TECHNICAL_DATA -> RailFeatureTemplate.TECHNICAL_DATA
    FeatureDestination.REFERENCE -> RailFeatureTemplate.REFERENCE
    FeatureDestination.FIRST_AID -> RailFeatureTemplate.FIRST_AID
    FeatureDestination.SAFETY -> RailFeatureTemplate.SAFETY
}

@Composable
fun RailFeatureScaffold(
    template: RailFeatureTemplate,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                stateDescription = "RailFeatureTemplate:${template.name}"
            },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
