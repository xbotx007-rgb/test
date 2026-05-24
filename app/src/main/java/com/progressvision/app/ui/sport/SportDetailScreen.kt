package com.progressvision.app.ui.sport

import androidx.compose.foundation.layout.Arrangement
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
import com.progressvision.app.data.entity.SportExercise
import com.progressvision.app.data.entity.SportType
import com.progressvision.app.ui.common.LineChart
import com.progressvision.app.ui.common.SectionCard
import com.progressvision.app.ui.common.StatTile
import com.progressvision.app.util.Time
import com.progressvision.app.viewmodel.AppViewModelFactory
import com.progressvision.app.viewmodel.SportViewModel

private sealed class Range(val days: Int, val labelRes: Int) {
    object D7 : Range(7, R.string.sport_progress_7d)
    object D14 : Range(14, R.string.sport_progress_14d)
    object D21 : Range(21, R.string.sport_progress_21d)
    object D30 : Range(30, R.string.sport_progress_30d)
    object D365 : Range(365, R.string.sport_progress_365d)
    object All : Range(36500, R.string.sport_progress_all)
    data class Custom(val customDays: Int) : Range(customDays, R.string.sport_progress_custom)
}

private val RangePresets: List<Range> = listOf(
    Range.D7, Range.D14, Range.D21, Range.D30, Range.D365, Range.All
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SportDetailScreen(exerciseId: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as ProgressVisionApp
    val vm: SportViewModel = viewModel(factory = AppViewModelFactory(app))

    var exercise by remember { mutableStateOf<SportExercise?>(null) }
    LaunchedEffect(exerciseId) { exercise = vm.getExercise(exerciseId) }
    val current = exercise ?: return

    var range by remember { mutableStateOf<Range>(Range.D30) }
    var customDays by remember { mutableStateOf(60) }
    var showCustomPeriod by remember { mutableStateOf(false) }
    val since = remember(range) { Time.daysAgo(range.days) }
    val rangeEntries by vm.entriesSince(exerciseId, since).collectAsState(initial = emptyList())
    val allEntries by vm.entries(exerciseId).collectAsState(initial = emptyList())

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
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RangePresets.forEach { r ->
                                FilterChip(
                                    selected = range == r,
                                    onClick = { range = r },
                                    label = { Text(stringResource(r.labelRes)) }
                                )
                            }
                            val isCustom = range is Range.Custom
                            FilterChip(
                                selected = isCustom,
                                onClick = {
                                    if (isCustom) showCustomPeriod = true
                                    else {
                                        range = Range.Custom(customDays)
                                        showCustomPeriod = true
                                    }
                                },
                                label = {
                                    Text(
                                        if (isCustom)
                                            stringResource(R.string.sport_progress_custom_n, customDays)
                                        else stringResource(R.string.sport_progress_custom)
                                    )
                                }
                            )
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
                        "Запишите первый подход, чтобы увидеть прогресс.",
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
            onSave = { entries ->
                entries.forEach { e ->
                    vm.addEntry(e.copy(exerciseId = exerciseId), current.name)
                }
                showAdd = false
            }
        )
    }
    if (showCustomPeriod) {
        CustomPeriodDialog(
            initialDays = customDays,
            onDismiss = { showCustomPeriod = false },
            onConfirm = { d ->
                customDays = d.coerceAtLeast(1)
                range = Range.Custom(customDays)
                showCustomPeriod = false
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
        StatTile(label = "Подходов", value = count.toString(), modifier = Modifier.weight(1f))
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

private data class SetRow(val reps: String = "", val weight: String = "")

@Composable
private fun AddEntryDialog(
    type: SportType,
    onDismiss: () -> Unit,
    onSave: (List<SportEntry>) -> Unit
) {
    var rows by remember { mutableStateOf(listOf(SetRow())) }
    var distance by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sport_add_entry)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (type) {
                    SportType.STRENGTH, SportType.BODYWEIGHT -> {
                        rows.forEachIndexed { index, row ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    "#${index + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = row.reps,
                                    onValueChange = { v ->
                                        rows = rows.toMutableList().also {
                                            it[index] = row.copy(reps = v.filter { c -> c.isDigit() })
                                        }
                                    },
                                    label = { Text(stringResource(R.string.sport_reps)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                if (type == SportType.STRENGTH) {
                                    OutlinedTextField(
                                        value = row.weight,
                                        onValueChange = { v ->
                                            rows = rows.toMutableList().also {
                                                it[index] = row.copy(
                                                    weight = v.filter { c -> c.isDigit() || c == '.' || c == ',' }
                                                )
                                            }
                                        },
                                        label = { Text(stringResource(R.string.sport_weight)) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                                if (rows.size > 1) {
                                    IconButton(onClick = {
                                        rows = rows.toMutableList().also { it.removeAt(index) }
                                    }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = {
                            val last = rows.lastOrNull() ?: SetRow()
                            rows = rows + SetRow(reps = last.reps, weight = last.weight)
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.sport_add_set))
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
                val baseDate = System.currentTimeMillis()
                val noteVal = note.ifBlank { null }
                val entries: List<SportEntry> = when (type) {
                    SportType.STRENGTH, SportType.BODYWEIGHT -> {
                        rows.mapIndexedNotNull { index, row ->
                            val reps = row.reps.toIntOrNull() ?: return@mapIndexedNotNull null
                            if (reps <= 0) return@mapIndexedNotNull null
                            SportEntry(
                                exerciseId = 0,
                                date = baseDate + index,
                                sets = 1,
                                reps = reps,
                                weightKg = if (type == SportType.STRENGTH)
                                    row.weight.replace(',', '.').toFloatOrNull()
                                else null,
                                note = if (index == 0) noteVal else null
                            )
                        }
                    }
                    SportType.CARDIO -> {
                        val d = distance.replace(',', '.').toFloatOrNull()
                        val durSec = duration.toIntOrNull()?.let { it * 60 }
                        if (d == null && durSec == null) emptyList()
                        else listOf(
                            SportEntry(
                                exerciseId = 0,
                                date = baseDate,
                                distanceKm = d,
                                durationSec = durSec,
                                note = noteVal
                            )
                        )
                    }
                }
                if (entries.isNotEmpty()) onSave(entries)
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun CustomPeriodDialog(
    initialDays: Int,
    onDismiss: () -> Unit,
    onConfirm: (days: Int) -> Unit
) {
    var days by remember { mutableStateOf(initialDays.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sport_progress_custom)) },
        text = {
            Column {
                Text(
                    "Покажем прогресс за указанное количество последних дней.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = days,
                    onValueChange = { days = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("Количество дней") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(days.toIntOrNull() ?: initialDays) },
                enabled = (days.toIntOrNull() ?: 0) > 0
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
