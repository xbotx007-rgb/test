package com.progressvision.app.ui.sport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.progressvision.app.ProgressVisionApp
import com.progressvision.app.R
import com.progressvision.app.data.entity.SportEntry
import com.progressvision.app.data.entity.SportTracker
import com.progressvision.app.data.entity.SportType
import com.progressvision.app.ui.common.LineChart
import com.progressvision.app.ui.common.SectionCard
import com.progressvision.app.ui.common.StatTile
import com.progressvision.app.util.Time
import com.progressvision.app.viewmodel.AppViewModelFactory
import com.progressvision.app.viewmodel.SportViewModel
import kotlinx.coroutines.flow.collect

private enum class Range(val days: Int, val labelRes: Int) {
    D7(7, R.string.sport_progress_7d),
    D30(30, R.string.sport_progress_30d),
    D90(90, R.string.sport_progress_90d),
    ALL(36500, R.string.sport_progress_all)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportDetailScreen(trackerId: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as ProgressVisionApp
    val vm: SportViewModel = viewModel(factory = AppViewModelFactory(app))

    var tracker by remember { mutableStateOf<SportTracker?>(null) }
    LaunchedEffect(trackerId) { tracker = vm.getTracker(trackerId) }
    val current = tracker ?: return

    var range by remember { mutableStateOf(Range.D30) }
    val since = remember(range) { Time.daysAgo(range.days) }
    val rangeEntries by vm.entriesSince(trackerId, since).collectAsState(initial = emptyList())
    val allEntries by vm.entries(trackerId).collectAsState(initial = emptyList())

    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<SportEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.sport_add_entry))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard(title = "Прогресс") {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Range.values().forEach { r ->
                                FilterChip(
                                    selected = r == range,
                                    onClick = { range = r },
                                    label = { Text(stringResource(r.labelRes)) }
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        val points = rangeEntries
                            .sortedBy { it.date }
                            .map { it.primaryMetric(current.type) }
                        LineChart(points = points)
                        Spacer(Modifier.height(12.dp))
                        SummaryRow(entries = rangeEntries, allEntries = allEntries, type = current.type)
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.sport_history),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (allEntries.isEmpty()) {
                item {
                    Text(
                        "Запишите первую тренировку, чтобы увидеть прогресс.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(allEntries, key = { it.id }) { entry ->
                    EntryRow(
                        entry = entry,
                        type = current.type,
                        onDelete = { pendingDelete = entry }
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddEntryDialog(
            type = current.type,
            onDismiss = { showAdd = false },
            onSave = { e ->
                vm.addEntry(e.copy(trackerId = trackerId), current.name)
                showAdd = false
            }
        )
    }
    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить запись?") },
            confirmButton = {
                TextButton(onClick = { vm.deleteEntry(entry); pendingDelete = null }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun SummaryRow(
    entries: List<SportEntry>,
    allEntries: List<SportEntry>,
    type: SportType
) {
    val count = entries.size
    val best = allEntries.maxOfOrNull { it.primaryMetric(type) } ?: 0f
    val avg = if (entries.isNotEmpty()) entries.map { it.primaryMetric(type) }.average().toFloat() else 0f
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        StatTile(label = "Тренировок", value = count.toString(), modifier = Modifier.weight(1f))
        StatTile(label = "Среднее", value = formatNumber(avg), modifier = Modifier.weight(1f))
        StatTile(label = "Рекорд", value = formatNumber(best), modifier = Modifier.weight(1f))
    }
}

private fun formatNumber(v: Float): String {
    if (v == 0f) return "—"
    return if (v >= 100f) "%.0f".format(v) else "%.1f".format(v)
}

@Composable
private fun EntryRow(entry: SportEntry, type: SportType, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Time.formatDate(entry.date), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(describeEntry(entry, type), style = MaterialTheme.typography.bodyMedium)
                entry.note?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

private fun describeEntry(entry: SportEntry, type: SportType): String = when (type) {
    SportType.STRENGTH -> buildString {
        val s = entry.sets ?: 0
        val r = entry.reps ?: 0
        val w = entry.weightKg ?: 0f
        append("${s}×${r}")
        if (w > 0f) append(" • ${"%.1f".format(w)} кг")
    }
    SportType.BODYWEIGHT -> {
        val s = entry.sets ?: 0
        val r = entry.reps ?: 0
        "${s}×${r}"
    }
    SportType.CARDIO -> buildString {
        entry.distanceKm?.let { append("%.2f км".format(it)) }
        entry.durationSec?.let {
            if (isNotEmpty()) append(" • ")
            append("${it / 60} мин")
        }
    }
}

@Composable
private fun AddEntryDialog(
    type: SportType,
    onDismiss: () -> Unit,
    onSave: (SportEntry) -> Unit
) {
    var sets by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sport_add_entry)) },
        text = {
            Column {
                when (type) {
                    SportType.STRENGTH -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = sets,
                                onValueChange = { sets = it.filter { c -> c.isDigit() } },
                                label = { Text(stringResource(R.string.sport_sets)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = reps,
                                onValueChange = { reps = it.filter { c -> c.isDigit() } },
                                label = { Text(stringResource(R.string.sport_reps)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                            label = { Text(stringResource(R.string.sport_weight)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    SportType.BODYWEIGHT -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = sets,
                                onValueChange = { sets = it.filter { c -> c.isDigit() } },
                                label = { Text(stringResource(R.string.sport_sets)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = reps,
                                onValueChange = { reps = it.filter { c -> c.isDigit() } },
                                label = { Text(stringResource(R.string.sport_reps)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                    SportType.CARDIO -> {
                        OutlinedTextField(
                            value = distance,
                            onValueChange = { distance = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                            label = { Text(stringResource(R.string.sport_distance)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = duration,
                            onValueChange = { duration = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.sport_duration_min)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.sport_notes)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val entry = SportEntry(
                    trackerId = 0,
                    date = System.currentTimeMillis(),
                    sets = sets.toIntOrNull(),
                    reps = reps.toIntOrNull(),
                    weightKg = weight.replace(',', '.').toFloatOrNull(),
                    distanceKm = distance.replace(',', '.').toFloatOrNull(),
                    durationSec = duration.toIntOrNull()?.let { it * 60 },
                    note = note.ifBlank { null }
                )
                onSave(entry)
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
