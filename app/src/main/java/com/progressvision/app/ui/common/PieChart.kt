package com.progressvision.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PieChart(
    slices: List<CategorySlice>,
    modifier: Modifier = Modifier,
    diameter: Dp = 160.dp,
    strokeWidth: Dp = 28.dp
) {
    val total = slices.sumOf { it.seconds }.coerceAtLeast(1L)
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = Stroke(width = strokeWidth.toPx())
            val side = size.minDimension - strokeWidth.toPx()
            val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
            val arcSize = Size(side, side)
            var startAngle = -90f
            slices.forEach { slice ->
                if (slice.seconds <= 0L) return@forEach
                val sweep = 360f * slice.seconds.toFloat() / total.toFloat()
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
                startAngle += sweep
            }
        }
    }
}
