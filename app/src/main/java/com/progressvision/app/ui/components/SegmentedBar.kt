package com.progressvision.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class BarSegment(val value: Float, val color: Color, val label: String)

@Composable
fun SegmentedBar(
    segments: List<BarSegment>,
    modifier: Modifier = Modifier
) {
    val total = segments.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.0001f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        segments.forEach { seg ->
            val weight = (seg.value / total).coerceIn(0f, 1f)
            if (weight > 0f) {
                Row(
                    modifier = Modifier
                        .weight(weight)
                        .background(seg.color)
                ) {}
            }
        }
    }
}
