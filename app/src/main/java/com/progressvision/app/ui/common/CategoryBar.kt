package com.progressvision.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.progressvision.app.util.Time

data class CategorySlice(val name: String, val color: Color, val seconds: Long)

@Composable
fun CategoryStackBar(
    slices: List<CategorySlice>,
    modifier: Modifier = Modifier
) {
    val total = slices.sumOf { it.seconds }.coerceAtLeast(1L)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            slices.forEach { slice ->
                val w = slice.seconds.toFloat() / total.toFloat()
                if (w > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(w)
                            .height(14.dp)
                            .background(slice.color)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        slices.filter { it.seconds > 0 }.forEach { slice ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(slice.color)
                )
                Spacer(Modifier.width(8.dp))
                Text(slice.name, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    Time.formatDuration(slice.seconds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
