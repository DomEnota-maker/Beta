package ru.railbrake.calculator.atlas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/**
 * A shared presentation of model-owned functional topology.
 *
 * The coordinates are legacy educational layout coordinates, never
 * physical component positions or installation/wiring dimensions.
 */
@Composable
fun AtlasElectricalDiagram(
    session: ElectricalFunctionalSession,
    selectedNodeId: String?,
    onNodeSelected: (String) -> Unit,
) {
    val scenario = session.scenario
    val activeEdges = session.activeEdges.toSet()
    val activeNodes = activeEdges.flatMap { listOf(it.from, it.to) }.toSet()
    val nodesById = scenario.nodes.associateBy { it.id }
    val primary = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surface
    val card = MaterialTheme.colorScheme.surface
    val activeCard = MaterialTheme.colorScheme.primaryContainer
    val onSurface = MaterialTheme.colorScheme.onSurface
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Функциональная схема · прокручивайте по горизонтали",
            style = MaterialTheme.typography.titleSmall,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)),
        ) {
            Box(
                modifier = Modifier
                    .width(scenario.canvasWidth.dp)
                    .height(scenario.canvasHeight.dp),
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    scenario.edges.forEach { edge ->
                        val from = nodesById.getValue(edge.from)
                        val to = nodesById.getValue(edge.to)
                        val start = Offset(
                            (from.x + from.width / 2f).dp.toPx(),
                            (from.y + from.height / 2f).dp.toPx(),
                        )
                        val end = Offset(
                            (to.x + to.width / 2f).dp.toPx(),
                            (to.y + to.height / 2f).dp.toPx(),
                        )
                        val active = edge in activeEdges
                        val strokeColor = if (active) primary else inactive.copy(alpha = 0.42f)
                        drawLine(
                            color = surface,
                            start = start,
                            end = end,
                            strokeWidth = 10.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = strokeColor,
                            start = start,
                            end = end,
                            strokeWidth = (if (active) 5.dp else 3.dp).toPx(),
                            cap = StrokeCap.Round,
                        )

                        val dx = end.x - start.x
                        val dy = end.y - start.y
                        val length = sqrt(dx * dx + dy * dy)
                        if (length > 1f) {
                            val ux = dx / length
                            val uy = dy / length
                            val tip = Offset(
                                start.x + dx * 0.68f,
                                start.y + dy * 0.68f,
                            )
                            val arrowLength = (if (active) 12.dp else 9.dp).toPx()
                            val arrowHalfWidth = (if (active) 7.dp else 5.dp).toPx()
                            val base = Offset(
                                tip.x - ux * arrowLength,
                                tip.y - uy * arrowLength,
                            )
                            val left = Offset(
                                base.x - uy * arrowHalfWidth,
                                base.y + ux * arrowHalfWidth,
                            )
                            val right = Offset(
                                base.x + uy * arrowHalfWidth,
                                base.y - ux * arrowHalfWidth,
                            )
                            drawLine(strokeColor, tip, left, strokeWidth = 2.5.dp.toPx())
                            drawLine(strokeColor, tip, right, strokeWidth = 2.5.dp.toPx())
                        }
                    }
                }

                scenario.nodes.forEach { node ->
                    val active = node.id in activeNodes
                    val selected = node.id == selectedNodeId
                    val shape = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .offset(node.x.dp, node.y.dp)
                            .size(node.width.dp, node.height.dp)
                            .clip(shape)
                            .background(if (active || selected) activeCard else card)
                            .border(
                                width = (if (selected) 3.dp else if (active) 2.dp else 1.dp),
                                color = if (active || selected) primary else inactive,
                                shape = shape,
                            )
                            .semantics {
                                contentDescription =
                                    node.info.title + ". " + node.info.subtitle
                            }
                            .clickable { onNodeSelected(node.id) }
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = node.info.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                            Text(
                                text = node.info.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = subtitleColor,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = "Цветом выделены связи, активные к выбранному шагу. " +
                "Стрелки обозначают функциональное воздействие, а не реальные провода.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
