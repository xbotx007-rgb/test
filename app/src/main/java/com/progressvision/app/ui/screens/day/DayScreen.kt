package com.progressvision.app.ui.screens.day

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.progressvision.app.ProgressVisionApp
import com.progressvision.app.data.entities.DayActivity
import com.progressvision.app.data.entities.DayCategory
import com.progressvision.app.ui.AppViewModelFactory
import com.progressvision.app.ui.components.BarSegment
import com.progressvision.app.ui.components.SegmentedBar
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as ProgressVisionApp
    val viewModel: DayViewModel = viewModel(factory = AppViewModelFactory(app))

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val activities by viewModel.activitiesToday.collectAsStateWithLifecycle()
    val weekActivities by viewModel.activitiesWeek.collectAsStateWithLifecycle()
    val timer by viewModel.runningTimer.collectAsStateWithLifecycle()

    var showAddCategory by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(timer != null) {
        while (timer != null) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("День") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            timer?.let { running ->
                item {
                    RunningTimerCard(
                        categoryName = running.categoryName,
                        elapsedMs = (now - running.startedAt).coerceAtLeast(0L),
                        onStop = viewModel::stopTimer
                    )
                }
            }

            item {
                Column {
                    Text("Категории", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories, key = { it.id }) { cat ->
                            ElevatedAssistChip(
                                onClick = {
                                    if (timer == null) viewModel.startTimer(cat)
                                },
                                label = { Text(cat.name) },
                                leadingIcon = {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                }
                            )
                        }
                        item {
                            AssistChip(
                                onClick = { showAddCategory = true },
                                label = { Text("Добавить") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Add, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { showManual = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(" Внести вручную")
                    }
                    FilledTonalButton(onClick = viewModel::repeatLast) {
                        Icon(Icons.Filled.Repeat, contentDescription = null)
                        Text(" Повторить")
                    }
                }
            }

            item {
                StatsCard(title = "Сегодня", activities = activities)
            }
            item {
                StatsCard(title = "За неделю", activities = weekActivities)
            }

            item {
                Text("Записи дня", style = MaterialTheme.typography.titleMedium)
            }
            if (activities.isEmpty()) {
                item {
                    Text(
                        "Пока пусто. Запусти таймер для категории или внеси активность вручную.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(activities, key = { it.id }) { activity ->
                    ActivityRow(
                        activity = activity,
                        onDelete = { viewModel.deleteActivity(activity) }
                    )
                }
            }
        }
    }

    if (showAddCategory) {
        SimpleInputDialog(
            title = "Новая категория",
            label = "Название",
            onDismiss = { showAddCategory = false },
            onConfirm = {
                viewModel.addCategory(it)
                showAddCategory = false
            }
        )
    }

    if (showManual) {
        ManualEntryDialog(
            categories = categories,
            onDismiss = { showManual = false },
            onConfirm = { cat, title, mins ->
                viewModel.manualEntry(cat, title, mins)
                showManual = false
            }
        )
    }
}

@Composable
private fun RunningTimerCard(categoryName: String, elapsedMs: Long, onStop: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(categoryName, style = MaterialTheme.typography.titleMedium)
                Text("Идёт ${formatDuration(elapsedMs)}", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Filled.Stop, contentDescription = "Остановить")
            }
        }
    }
}

@Composable
private fun StatsCard(title: String, activities: List<DayActivity>) {
    val totals = remember(activities) {
        activities.groupBy { it.title }
            .mapValues { (_, v) -> v.sumOf { it.durationMs } }
            .toList()
            .sortedByDescending { it.second }
    }
    val totalMs = totals.sumOf { it.second }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Всего ${formatDuration(totalMs)}", style = MaterialTheme.typography.bodyMedium)
            if (totals.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                val palette = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary,
                    Color(0xFF9C6CFF),
                    Color(0xFFE5484D)
                )
                val segments = totals.take(5).mapIndexed { i, (name, ms) ->
                    BarSegment(ms.toFloat(), palette[i % palette.size], name)
                }
                SegmentedBar(segments = segments)
                Spacer(Modifier.height(8.dp))
                totals.take(5).forEachIndexed { i, (name, ms) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(formatDuration(ms), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(activity: DayActivity, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(activity.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${fmt.format(Date(activity.startedAt))} – ${fmt.format(Date(activity.endedAt))} · ${formatDuration(activity.durationMs)}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (activity.source != "manual" && activity.source != "timer") {
                    Text("Источник: ${sourceLabel(activity.source)}", style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
            }
        }
    }
}

private fun sourceLabel(source: String): String = when (source) {
    "sport" -> "Спорт"
    "task" -> "Задача"
    "timer" -> "Таймер"
    else -> "Вручную"
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return when {
        h > 0 -> "${h}ч ${m}м"
        m > 0 -> "${m}м"
        else -> "${s}с"
    }
}

@Composable
private fun SimpleInputDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("Создать") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualEntryDialog(
    categories: List<DayCategory>,
    onDismiss: () -> Unit,
    onConfirm: (DayCategory?, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<DayCategory?>(categories.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Внести активность") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (categories.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories, key = { it.id }) { cat ->
                            val isSelected = selected?.id == cat.id
                            AssistChip(
                                onClick = { selected = cat },
                                label = { Text(cat.name) },
                                colors = if (isSelected)
                                    androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                else
                                    androidx.compose.material3.AssistChipDefaults.assistChipColors()
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Название (необязательно)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = minutes, onValueChange = { minutes = it.filter(Char::isDigit) },
                    label = { Text("Минут") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(selected, title, minutes.toIntOrNull() ?: 0)
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
