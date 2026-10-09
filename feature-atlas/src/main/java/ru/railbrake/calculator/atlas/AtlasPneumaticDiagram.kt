package ru.railbrake.calculator.atlas

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/** Render the donor route coordinates only over their recovered source image. */
@Composable
fun AtlasPneumaticDiagram(
    document: PneumaticFlowDocument,
    session: PneumaticFlowSession,
) {
    val background = document.background
    val context = LocalContext.current
    val image: ImageBitmap? = remember(background, context) {
        background?.let { source ->
            runCatching {
                val bitmap = context.assets.open(source.assetPath).use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
                if (bitmap != null &&
                    bitmap.width == source.width &&
                    bitmap.height == source.height
                ) {
                    bitmap.asImageBitmap()
                } else {
                    null
                }
            }.getOrNull()
        }
    }
    if (background == null || image == null) {
        Text(
            text = "Пневмосхема ещё не восстановлена или не соответствует " +
                "проверенной координатной подложке. Учебные шаги доступны текстом.",
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }

    val mainRouteColor = MaterialTheme.colorScheme.primary
    val releaseColor = MaterialTheme.colorScheme.tertiary
    val controlColor = MaterialTheme.colorScheme.secondary
    val visibleSegments = session.mode.steps
        .take(session.stepIndex + 1)
        .flatMap { it.segments }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Путь воздуха · схема прокручивается по горизонтали",
            style = MaterialTheme.typography.titleSmall,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Box(
                modifier = Modifier
                    .width(document.canvasWidth.dp)
                    .height(document.canvasHeight.dp),
            ) {
                Image(
                    bitmap = image,
                    contentDescription = "Учебная пневматическая схема локомотива",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.FillBounds,
                )
                Canvas(modifier = Modifier.matchParentSize()) {
                    visibleSegments.forEach { segment ->
                        val color = when (segment.kind) {
                            PneumaticRouteKind.FLOW -> mainRouteColor
                            PneumaticRouteKind.RELEASE -> releaseColor
                            PneumaticRouteKind.CONTROL -> controlColor
                        }
                        val dash = if (segment.kind == PneumaticRouteKind.FLOW) {
                            null
                        } else {
                            PathEffect.dashPathEffect(
                                floatArrayOf(14.dp.toPx(), 9.dp.toPx()),
                            )
                        }
                        val coordinates = segment.points.map { point ->
                            Offset(point.x.dp.toPx(), point.y.dp.toPx())
                        }
                        val path = Path().apply {
                            moveTo(coordinates.first().x, coordinates.first().y)
                            coordinates.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(
                            path = path,
                            color = color.copy(alpha = 0.34f),
                            style = Stroke(width = 11.dp.toPx(), pathEffect = dash),
                        )
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(width = 4.dp.toPx(), pathEffect = dash),
                        )
                        val tail = coordinates[coordinates.lastIndex - 1]
                        val tip = coordinates.last()
                        val dx = tip.x - tail.x
                        val dy = tip.y - tail.y
                        val length = sqrt(dx * dx + dy * dy)
                        if (length > 1f) {
                            val ux = dx / length
                            val uy = dy / length
                            val arrowLength = 12.dp.toPx()
                            val arrowWidth = 6.dp.toPx()
                            val base = Offset(
                                tip.x - ux * arrowLength,
                                tip.y - uy * arrowLength,
                            )
                            val left = Offset(base.x - uy * arrowWidth, base.y + ux * arrowWidth)
                            val right = Offset(base.x + uy * arrowWidth, base.y - ux * arrowWidth)
                            drawLine(color, tip, left, strokeWidth = 3.dp.toPx())
                            drawLine(color, tip, right, strokeWidth = 3.dp.toPx())
                        }
                    }
                }
            }
        }
        Text(
            text = "Сплошная линия — поток воздуха; пунктир — разрядка " +
                "или управляющее давление. Отображаются участки до выбранного шага.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
