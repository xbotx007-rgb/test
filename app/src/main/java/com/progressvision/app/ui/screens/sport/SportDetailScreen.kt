package com.progressvision.app.ui.screens.sport

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.progressvision.app.ProgressVisionApp
import com.progressvision.app.data.entities.SportEntry
import com.progressvision.app.data.entities.SportKind
import com.progressvision.app.ui.AppViewModelFactory
import com.progressvision.app.ui.components.MiniLineChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportDetailScreen(trackerId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ProgressVisionApp
    val viewModel: SportDetailViewModel = viewModel(
        factory = AppViewModelFactory(app, trackerId),
        key = "sport-detail-$trackerId"
    )
    val tracker by viewModel.tracker.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    var showLog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tracker?.name ?: "Трекер") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showLog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Записать")
            }
        }
    ) { padding ->
        val t = tracker
        if (t == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Трекер не найден")
            }
        } else {
            DetailBody(
                tracker = t.kind,
                entries = entries,
                padding = padding,
                onDelete = viewModel::deleteEntry
            )
        }
    }

    if (showLog && tracker != null) {
        LogEntryDialog(
            kind = tracker!!.kind,
            onDismiss = { showLog = false },
            onConfirm = { sets, reps, weight, distance, duration, notes ->
                viewModel.addEntry(sets, reps, weight, distance, duration, notes)
                showLog = false
            }
        )
    }
}

@Composable
private fun DetailBody(
    tracker: SportKind,
    entries: List<SportEntry>,
    padding: PaddingValues,
    onDelete: (SportEntry) -> Unit
) {
    val series = remember(entries) { entries.map { it.primaryMetric.toFloat() } }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Прогресс", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (series.size >= 2) {
                        MiniLineChart(points = series, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = comparisonText(series),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            "Запиши хотя бы две тренировки, чтобы увидеть рост.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        item {
            Text(
                "История",
                style = MaterialTheme.typography.titleMedium
            )
        }
        items(entries.reversed(), key = { it.id }) { entry ->
            EntryRow(entry = entry, onDelete = { onDelete(entry) })
        }
        if (entries.isEmpty()) {
            item {
                Text(
                    text = "Пока нет записей. Нажми + чтобы добавить тренировку.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun EntryRow(entry: SportEntry, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(fmt.format(Date(entry.performedAt)), style = MaterialTheme.typography.bodySmall)
                Text(entry.summary(), style = MaterialTheme.typography.bodyLarge)
            }
            TextButton(onClick = onDelete) { Text("Удалить") }
        }
    }
}

private fun SportEntry.summary(): String {
    val parts = buildList {
        if (sets != null && reps != null) add("$sets×$reps")
        else if (reps != null) add("$reps повт.")
        weightKg?.let { add("${it.formatNumber()} кг") }
        distanceKm?.let { add("${it.formatNumber()} км") }
        durationSec?.let { add(formatDuration(it)) }
    }
    return if (parts.isEmpty()) "Запись" else parts.joinToString(" · ")
}

private fun Double.formatNumber(): String =
    if (this % 1.0 == 0.0) "${this.toInt()}" else "%.1f".format(this)

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}м ${s}с" else "${s}с"
}

private fun comparisonText(series: List<Float>): String {
    if (series.size < 2) return ""
    val first = series.first()
    val last = series.last()
    if (first == 0f) return "Лучший показатель: ${last.formatFloat()}"
    val deltaPct = ((last - first) / first) * 100f
    val sign = if (deltaPct >= 0) "+" else ""
    return "Изменение: $sign${"%.1f".format(deltaPct)}% за период"
}

private fun Float.formatFloat(): String =
    if (this % 1f == 0f) "${this.toInt()}" else "%.1f".format(this)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogEntryDialog(
    kind: SportKind,
    onDismiss: () -> Unit,
    onConfirm: (sets: Int?, reps: Int?, weight: Double?, distance: Double?, duration: Long?, notes: String?) -> Unit
) {
    var sets by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая запись") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val numeric = KeyboardOptions(keyboardType = KeyboardType.Number)
                val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                when (kind) {
                    SportKind.STRENGTH -> {
                        OutlinedTextField(value = sets, onValueChange = { sets = it }, label = { Text("Подходы") }, keyboardOptions = numeric, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Повторения") }, keyboardOptions = numeric, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Вес, кг") }, keyboardOptions = decimal, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    SportKind.BODYWEIGHT -> {
                        OutlinedTextField(value = sets, onValueChange = { sets = it }, label = { Text("Подходы") }, keyboardOptions = numeric, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Повторения") }, keyboardOptions = numeric, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    SportKind.CARDIO -> {
                        OutlinedTextField(value = distance, onValueChange = { distance = it }, label = { Text("Дистанция, км") }, keyboardOptions = decimal, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = minutes, onValueChange = { minutes = it }, label = { Text("Время, мин") }, keyboardOptions = numeric, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    SportKind.TIME -> {
                        OutlinedTextField(value = minutes, onValueChange = { minutes = it }, label = { Text("Время, мин") }, keyboardOptions = numeric, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Заметка") }, singleLine = false, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val durationSec = minutes.toIntOrNull()?.times(60L)
                onConfirm(
                    sets.toIntOrNull(),
                    reps.toIntOrNull(),
                    weight.replace(',', '.').toDoubleOrNull(),
                    distance.replace(',', '.').toDoubleOrNull(),
                    durationSec,
                    notes.ifBlank { null }
                )
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
