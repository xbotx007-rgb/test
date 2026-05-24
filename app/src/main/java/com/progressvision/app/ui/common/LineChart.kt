package com.progressvision.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LineChart(
    points: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = color.copy(alpha = 0.18f)
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(height),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Пока нет данных для графика",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val w = size.width
        val h = size.height
        val pad = 12f
        val maxV = (points.maxOrNull() ?: 1f).coerceAtLeast(0.0001f)
        val minV = (points.minOrNull() ?: 0f).coerceAtMost(maxV)
        val range = (maxV - minV).coerceAtLeast(0.0001f)
        val stepX = if (points.size > 1) (w - pad * 2) / (points.size - 1).toFloat() else 0f

        fun yOf(value: Float): Float {
            val frac = (value - minV) / range
            return pad + (1f - frac) * (h - pad * 2)
        }

        val path = Path()
        val fillPath = Path()
        points.forEachIndexed { i, v ->
            val x = pad + i * stepX
            val y = yOf(v)
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h - pad)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        val lastX = pad + (points.size - 1) * stepX
        fillPath.lineTo(lastX, h - pad)
        fillPath.close()

        drawPath(path = fillPath, color = fillColor)
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        // Dots
        points.forEachIndexed { i, v ->
            val x = pad + i * stepX
            val y = yOf(v)
            drawCircle(color = color, radius = 6f, center = Offset(x, y))
        }
    }
}
