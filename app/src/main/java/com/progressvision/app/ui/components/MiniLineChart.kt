package com.progressvision.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MiniLineChart(
    points: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier.height(height)) {
        if (points.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val maxV = points.max()
        val minV = points.min()
        val range = (maxV - minV).coerceAtLeast(0.0001f)
        val stepX = w / (points.size - 1).coerceAtLeast(1)

        // baseline
        drawLine(
            color = gridColor,
            start = Offset(0f, h - 1),
            end = Offset(w, h - 1),
            strokeWidth = 2f
        )

        val path = Path()
        points.forEachIndexed { i, v ->
            val x = i * stepX
            val y = h - ((v - minV) / range) * (h * 0.85f) - h * 0.05f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )

        points.forEachIndexed { i, v ->
            val x = i * stepX
            val y = h - ((v - minV) / range) * (h * 0.85f) - h * 0.05f
            drawCircle(color = lineColor, radius = 6f, center = Offset(x, y))
        }
    }
}
