package ru.railbrake.calculator.atlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import ru.railbrake.calculator.domain.ContentTarget

/** Shared, informational Atlas UI; the model supplies data, not screens. */
@Composable
fun AtlasPneumaticFlowScreen(
    document: PneumaticFlowDocument,
    modelTitle: String,
    onBack: () -> Unit,
) {
    var modeId by remember(document.id) { mutableStateOf(document.modes.first().id) }
    val mode = document.mode(modeId) ?: document.modes.first()
    var session by remember(document.id, mode.id) {
        mutableStateOf(PneumaticFlowSession(mode))
    }
    var selectedComponentId by remember(document.id) { mutableStateOf<String?>(null) }
    val selectedComponent = document.components.firstOrNull { it.id == selectedComponentId }

    RailFeatureScaffold(
        template = RailFeatureTemplate.ATLAS,
        title = document.title,
        subtitle = "$modelTitle · Пневматика · учебный маршрут",
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack) { Text("← В Атлас") }
            Text("Выберите режим", style = MaterialTheme.typography.titleMedium)
            document.modes.forEach { item ->
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { modeId = item.id },
                ) {
                    Text(if (item.id == mode.id) "● " + item.title else item.title)
                }
            }

            Text(mode.summary, style = MaterialTheme.typography.bodyMedium)
            Text(mode.start, style = MaterialTheme.typography.bodyMedium)
            mode.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

            AtlasPneumaticDiagram(
                document = document,
                session = session,
                selectedComponentId = selectedComponentId,
                onComponentSelected = { selectedComponentId = it },
            )
            if (document.components.isNotEmpty()) {
                Text(
                    "Оборудование на схеме · выберите прибор",
                    style = MaterialTheme.typography.titleSmall,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(document.components, key = { it.id }) { component ->
                        OutlinedButton(
                            onClick = { selectedComponentId = component.id },
                        ) {
                            Text(
                                (if (selectedComponentId == component.id) "● " else "") +
                                    component.title,
                            )
                        }
                    }
                }
            }
            selectedComponent?.let { component ->
                AlertDialog(
                    onDismissRequest = { selectedComponentId = null },
                    title = { Text(component.title) },
                    text = {
                        Column(
                            // Keep dialog actions visible even for long donor descriptions
                            // on compact devices; the information body scrolls independently.
                            modifier = Modifier
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(component.details)
                            component.principle.takeIf(String::isNotBlank)?.let {
                                Text("Как работает", style = MaterialTheme.typography.titleSmall)
                                Text(it)
                            }
                            component.faultSigns.takeIf(String::isNotBlank)?.let {
                                Text("Возможные признаки неисправности", style = MaterialTheme.typography.titleSmall)
                                Text(it)
                            }
                            component.checks.takeIf(String::isNotBlank)?.let {
                                Text(
                                    "Что проверяли в исходном тренажёре",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(it)
                            }
                            Text(
                                "Материал исходного учебного прототипа. " +
                                    "Любые действия должны соответствовать актуальной " +
                                    "документации и требованиям безопасности.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { selectedComponentId = null }) {
                            Text("Закрыть")
                        }
                    },
                )
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Шаг " + (session.stepIndex + 1) + " из " + mode.steps.size,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(session.currentStep.title, style = MaterialTheme.typography.titleMedium)
                    Text(session.currentStep.description)
                    Text(
                        text = "Условных участков на этом шаге: " +
                            session.currentStep.segments.size,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { session = session.previous() },
                            enabled = session.hasPrevious,
                        ) { Text("Назад") }
                        Button(
                            onClick = { session = session.next() },
                            enabled = session.hasNext,
                        ) { Text("Далее") }
                    }
                    TextButton(onClick = { session = session.reset() }) {
                        Text("С начала")
                    }
                }
            }

            Text(document.disclaimer, style = MaterialTheme.typography.bodySmall)
            Text(
                "Координаты из прежней учебной схемы не являются точной " +
                    "геометрией трубопроводов. Карточка не разрешает " +
                    "выполнять какие-либо операции.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun AtlasElectricalFlowScreen(
    document: ElectricalFunctionalFlowDocument,
    modelTitle: String,
    onBack: () -> Unit,
    onEquipmentTarget: (ContentTarget) -> Unit,
) {
    var scenarioId by remember(document.id) {
        mutableStateOf(document.scenarios.first().id)
    }
    val scenario = document.scenario(scenarioId) ?: document.scenarios.first()
    var session by remember(document.id, scenario.id) {
        mutableStateOf(ElectricalFunctionalSession(scenario))
    }
    var selectedNodeId by remember(document.id, scenario.id) {
        mutableStateOf<String?>(null)
    }
    val nodeById = remember(scenario.id) { scenario.nodes.associateBy { it.id } }
    val selected = nodeById[selectedNodeId]

    RailFeatureScaffold(
        template = RailFeatureTemplate.ATLAS,
        title = document.title,
        subtitle = "$modelTitle · Электрические цепи · учебный маршрут",
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack) { Text("← В Атлас") }
            Text("Выберите функциональную цепь", style = MaterialTheme.typography.titleMedium)
            document.scenarios.forEach { item ->
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { scenarioId = item.id },
                ) {
                    Text(if (item.id == scenario.id) "● " + item.title else item.title)
                }
            }
            Text(scenario.summary, style = MaterialTheme.typography.bodyMedium)
            AtlasElectricalDiagram(
                session = session,
                selectedNodeId = selectedNodeId,
                onNodeSelected = { selectedNodeId = it },
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Шаг " + (session.stepIndex + 1) + " из " + scenario.steps.size,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(session.currentStep.title, style = MaterialTheme.typography.titleMedium)
                    Text(session.currentStep.description)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { session = session.previous() },
                            enabled = session.hasPrevious,
                        ) { Text("Назад") }
                        Button(
                            onClick = { session = session.next() },
                            enabled = session.hasNext,
                        ) { Text("Далее") }
                    }
                    TextButton(onClick = { session = session.reset() }) { Text("С начала") }
                }
            }

            Text("Активные функциональные связи", style = MaterialTheme.typography.titleMedium)
            session.activeEdges.forEach { edge ->
                val from = nodeById[edge.from]?.info?.title
                val to = nodeById[edge.to]?.info?.title
                if (from != null && to != null) {
                    Text("• $from → $to", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Text("Оборудование и условные элементы", style = MaterialTheme.typography.titleMedium)
            scenario.nodes.forEach { node ->
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { selectedNodeId = node.id },
                ) { Text(node.info.title) }
            }

            if (selected != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(selected.info.title, style = MaterialTheme.typography.titleMedium)
                        Text(selected.info.subtitle)
                        Text(selected.info.purpose)
                        Text("Включается / зависит от: " + selected.info.triggeredBy)
                        Text("Влияет на: " + selected.info.affects)
                        Text("Связано: " + selected.info.links)
                        selected.info.principle.takeIf(String::isNotBlank)?.let { Text(it) }
                        selected.info.faultSigns.takeIf(String::isNotBlank)?.let { Text(it) }
                        selected.info.checks.takeIf(String::isNotBlank)?.let { Text(it) }
                        selected.equipmentTarget()?.let { target ->
                            Button(
                                onClick = { onEquipmentTarget(target) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Подробнее об оборудовании") }
                        }
                    }
                }
            }

            Text(document.disclaimer, style = MaterialTheme.typography.bodySmall)
            Text(
                "Связи представлены текстом, а не монтажной схемой. " +
                    "Учебная карточка не предоставляет допуск к работам.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
