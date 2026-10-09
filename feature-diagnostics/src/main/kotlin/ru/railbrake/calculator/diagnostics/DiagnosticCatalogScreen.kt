package ru.railbrake.calculator.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.railbrake.calculator.designsystem.RailFeatureScaffold
import ru.railbrake.calculator.designsystem.RailFeatureTemplate

/**
 * One shared diagnostics UI for model-owned, separately accepted scenarios.
 * All queries and opens go through PublishedDiagnosticCatalog; the raw donor
 * executable graph must never be passed directly to a user-facing catalog.
 */
@Composable
fun DiagnosticCatalogScreen(
    catalog: PublishedDiagnosticCatalog,
    modelTitle: String,
    onBack: () -> Unit,
) {
    var query by remember(catalog) { mutableStateOf("") }
    var selectedId by remember(catalog) { mutableStateOf<String?>(null) }
    val selected = selectedId?.let(catalog::open)
    var state by remember(selected?.id) {
        mutableStateOf(selected?.let(ExecutableDiagnosticEngine::start))
    }

    RailFeatureScaffold(
        template = RailFeatureTemplate.DIAGNOSTICS,
        title = "Диагностика",
        subtitle = "$modelTitle · только проверенные опубликованные сценарии",
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = {
                    if (selectedId == null) onBack() else selectedId = null
                },
            ) { Text(if (selectedId == null) "← Назад" else "← К списку сценариев") }

            if (selected == null) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Поиск по названию, признакам и системе") },
                    singleLine = true,
                )
                val results = catalog.search(query)
                if (results.isEmpty()) {
                    Text(
                        "Доступных опубликованных сценариев по этому запросу нет. " +
                            "Непроверенные кандидаты не открываются и не используются " +
                            "для рекомендаций по действиям.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                results.forEach { scenario ->
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { selectedId = scenario.id },
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(scenario.title, style = MaterialTheme.typography.titleSmall)
                            Text(scenario.category, style = MaterialTheme.typography.bodySmall)
                            Text(scenario.summary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                Text(selected.title, style = MaterialTheme.typography.titleMedium)
                Text(selected.summary)
                Text(
                    "Пошаговый опрос помогает систематизировать признаки неисправности. " +
                        "Он не является допуском к работам и не заменяет действующие инструкции.",
                    style = MaterialTheme.typography.bodySmall,
                )
                selected.dangerSigns.takeIf { it.isNotEmpty() }?.let { signs ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("Признаки опасности", style = MaterialTheme.typography.titleSmall)
                            signs.forEach { Text("• $it") }
                        }
                    }
                }
                selected.stopConditions.takeIf { it.isNotEmpty() }?.let { stops ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("Условия прекращения проверки", style = MaterialTheme.typography.titleSmall)
                            stops.forEach { Text("• $it") }
                        }
                    }
                }

                if (state != null) {
                    val result = ExecutableDiagnosticEngine.result(selected, requireNotNull(state))
                    result.nextQuestion?.let { question ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    "Вопрос ${result.state.answers.size + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                Text(
                                    question.text,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ExecutableDiagnosticResponse.entries.forEach { response ->
                                        Button(
                                            onClick = {
                                                state = ExecutableDiagnosticEngine.answer(
                                                    selected,
                                                    requireNotNull(state),
                                                    response,
                                                ).state
                                            },
                                        ) { Text(response.displayLabel()) }
                                    }
                                }
                            }
                        }
                    }
                    if (result.nextQuestion == null) {
                        Text("Опрос завершён", style = MaterialTheme.typography.titleMedium)
                        if (result.leadingCauses.isEmpty()) {
                            Text("По полученным ответам гипотезы не выделены.")
                        } else {
                            Text("Возможные причины (не окончательный диагноз):")
                            result.leadingCauses.forEach { cause ->
                                Text("• ${cause.title}")
                                Text(cause.explanation, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (result.state.answers.isNotEmpty()) {
                        Text("История ответов", style = MaterialTheme.typography.titleSmall)
                        result.state.answers.forEach { answer ->
                            Text(answer.questionText)
                            Text(
                                "${answer.response.displayLabel()}: ${answer.conclusion}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { state = ExecutableDiagnosticEngine.start(selected) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Начать опрос заново") }
                }
            }
        }
    }
}

private fun ExecutableDiagnosticResponse.displayLabel(): String = when (this) {
    ExecutableDiagnosticResponse.YES -> "Да"
    ExecutableDiagnosticResponse.NO -> "Нет"
    ExecutableDiagnosticResponse.UNKNOWN -> "Не знаю"
}
