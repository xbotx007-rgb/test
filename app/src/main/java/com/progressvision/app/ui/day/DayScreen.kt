package com.progressvision.app.ui.day

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.progressvision.app.ProgressVisionApp
import com.progressvision.app.R
import com.progressvision.app.data.entity.ActivityCategory
import com.progressvision.app.data.entity.ActivityLog
import com.progressvision.app.data.entity.SavedDay
import com.progressvision.app.ui.common.CategorySlice
import com.progressvision.app.ui.common.CategoryStackBar
import com.progressvision.app.ui.common.PieChart
import com.progressvision.app.ui.common.SectionCard
import com.progressvision.app.ui.common.StatTile
import com.progressvision.app.util.Time
import com.progressvision.app.viewmodel.AppViewModelFactory
import com.progressvision.app.viewmodel.DayViewModel
import kotlinx.coroutines.delay

private sealed class PeriodChoice(val labelRes: Int) {
    object Today : PeriodChoice(R.string.day_today)
    object Week : PeriodChoice(R.string.day_week)
    object Month : PeriodChoice(R.string.day_month)
    object TwoMonths : PeriodChoice(R.string.day_two_months)
    object Custom : PeriodChoice(R.string.day_custom_period)
}

private fun PeriodChoice.rangeMs(customDays: Int): Pair<Long, Long> {
    val now = System.currentTimeMillis()
    val endOfToday = Time.endOfDay(now)
    return when (this) {
        PeriodChoice.Today -> Time.startOfDay(now) to endOfToday
        PeriodChoice.Week -> Time.startOfWeek(now) to endOfToday
        PeriodChoice.Month -> Time.daysAgo(30) to endOfToday
        PeriodChoice.TwoMonths -> Time.daysAgo(60) to endOfToday
        PeriodChoice.Custom -> Time.daysAgo(customDays.coerceAtLeast(1)) to endOfToday
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DayScreen() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as ProgressVisionApp
    val vm: DayViewModel = viewModel(factory = AppViewModelFactory(app))

    val categories by vm.categories.collectAsState()
    val running by vm.running.collectAsState()
    val savedDays by vm.savedDays.collectAsState()

    var showAddCategory by remember { mutableStateOf(false) }
    var showAddManual by remember { mutableStateOf(false) }
    var showCustomPeriod by remember { mutableStateOf(false) }
    var showSaveDay by remember { mutableStateOf(false) }
    var editingDay by remember { mutableStateOf<SavedDay?>(null) }
    var pendingDelete by remember { mutableStateOf<ActivityLog?>(null) }
    var pendingDeleteSaved by remember { mutableStateOf<SavedDay?>(null) }
    var selectedSavedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    var period by remember { mutableStateOf<PeriodChoice>(PeriodChoice.Today) }
    var customDays by remember { mutableStateOf(14) }

    val (from, to) = remember(period, customDays) { period.rangeMs(customDays) }
    val logsFlow = remember(from, to) { vm.logsBetween(from, to) }
    val displayLogs by logsFlow.collectAsState(initial = emptyList())

    // For combined stats across selected saved days.
    val selectedSaved = remember(savedDays, selectedSavedIds) {
        savedDays.filter { it.id in selectedSavedIds }
    }
    val combinedRange = remember(selectedSaved) {
        if (selectedSaved.isEmpty()) null
        else selectedSaved.minOf { it.fromMs } to selectedSaved.maxOf { it.toMs }
    }
    val combinedLogs by remember(combinedRange) {
        if (combinedRange == null) kotlinx.coroutines.flow.flowOf(emptyList<ActivityLog>())
        else vm.logsBetween(combinedRange.first, combinedRange.second)
    }.collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_day)) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            running?.let { current ->
                item {
                    RunningCard(
                        log = current,
                        onStop = { vm.stopRunning() },
                        onPause = { vm.pauseRunning() },
                        onResume = { vm.resumeRunning() }
                    )
                }
            }

            item {
                SectionCard(title = stringResource(R.string.day_period)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val choices = listOf(
                            PeriodChoice.Today, PeriodChoice.Week,
                            PeriodChoice.Month, PeriodChoice.TwoMonths,
                            PeriodChoice.Custom
                        )
                        choices.forEach { choice ->
                            FilterChip(
                                selected = period == choice,
                                onClick = {
                                    period = choice
                                    if (choice == PeriodChoice.Custom) showCustomPeriod = true
                                },
                                label = {
                                    val label = if (choice == PeriodChoice.Custom && period == PeriodChoice.Custom)
                                        stringResource(R.string.day_custom_n_days, customDays)
                                    else stringResource(choice.labelRes)
                                    Text(label)
                                }
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(
                    title = stringResource(R.string.day_balance),
                    trailing = {
                        TextButton(onClick = { showSaveDay = true }) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.day_save_day))
                        }
                    }
                ) {
                    StatsBlock(logs = displayLogs, categories = categories)
                }
            }

            item {
                SectionCard(
                    title = stringResource(R.string.day_quick_add),
                    trailing = {
                        TextButton(onClick = { showAddCategory = true }) {
                            Text(stringResource(R.string.day_create_category))
                        }
                    }
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categories.forEach { cat ->
                            AssistChip(
                                onClick = { vm.startQuick(cat) },
                                label = { Text(cat.name) },
                                leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = parseColor(cat.colorHex)
                                )
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(
                    title = stringResource(R.string.day_logs),
                    trailing = {
                        TextButton(onClick = { showAddManual = true }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.day_add_manual))
                        }
                    }
                ) {
                    if (displayLogs.isEmpty()) {
                        Text(
                            stringResource(R.string.day_no_logs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            displayLogs.forEach { log ->
                                LogRow(
                                    log = log,
                                    categories = categories,
                                    onDelete = { pendingDelete = log }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(
                    title = stringResource(R.string.day_saved_days),
                    trailing = {
                        if (savedDays.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (selectedSavedIds.size < savedDays.size) {
                                    TextButton(onClick = {
                                        selectedSavedIds = savedDays.map { it.id }.toSet()
                                    }) { Text(stringResource(R.string.day_saved_select_all)) }
                                }
                                if (selectedSavedIds.isNotEmpty()) {
                                    TextButton(onClick = { selectedSavedIds = emptySet() }) {
                                        Text(stringResource(R.string.day_saved_clear))
                                    }
                                }
                            }
                        }
                    }
                ) {
                    if (savedDays.isEmpty()) {
                        Text(
                            stringResource(R.string.day_saved_no_items),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            savedDays.forEach { sd ->
                                SavedDayRow(
                                    day = sd,
                                    selected = sd.id in selectedSavedIds,
                                    onToggle = {
                                        selectedSavedIds =
                                            if (sd.id in selectedSavedIds) selectedSavedIds - sd.id
                                            else selectedSavedIds + sd.id
                                    },
                                    onEdit = { editingDay = sd },
                                    onDelete = { pendingDeleteSaved = sd }
                                )
                            }
                        }
                    }
                }
            }

            if (selectedSaved.isNotEmpty()) {
                item {
                    SectionCard(title = stringResource(R.string.day_saved_combined_stats)) {
                        val days = selectedSaved
                        val filtered = combinedLogs.filter { log ->
                            days.any { d -> log.startedAt in d.fromMs until d.toMs }
                        }
                        CombinedStatsBlock(logs = filtered, categories = categories)
                    }
                }
            }
        }
    }

    if (showAddCategory) {
        CreateCategoryDialog(
            onDismiss = { showAddCategory = false },
            onCreate = { name -> vm.createCategory(name); showAddCategory = false }
        )
    }
    if (showAddManual) {
        AddManualDialog(
            categories = categories,
            onDismiss = { showAddManual = false },
            onAdd = { categoryId, title, startedAt, endedAt ->
                vm.addManual(categoryId, title, startedAt, endedAt)
                showAddManual = false
            }
        )
    }
    if (showCustomPeriod) {
        CustomPeriodDialog(
            initialDays = customDays,
            onDismiss = { showCustomPeriod = false },
            onConfirm = { d ->
                customDays = d.coerceAtLeast(1)
                period = PeriodChoice.Custom
                showCustomPeriod = false
            }
        )
    }
    if (showSaveDay) {
        SaveDayDialog(
            initialFrom = from,
            initialTo = to,
            onDismiss = { showSaveDay = false },
            onSave = { label, fromMs, toMs, note ->
                vm.saveDay(label = label, fromMs = fromMs, toMs = toMs, note = note)
                showSaveDay = false
            }
        )
    }
    editingDay?.let { day ->
        EditSavedDayDialog(
            day = day,
            onDismiss = { editingDay = null },
            onSave = { updated ->
                vm.updateSavedDay(updated)
                editingDay = null
            }
        )
    }
    pendingDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить запись?") },
            confirmButton = {
                TextButton(onClick = { vm.deleteLog(log); pendingDelete = null }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
    pendingDeleteSaved?.let { sd ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSaved = null },
            title = { Text(sd.label) },
            text = { Text("Удалить сохранённый день?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteSavedDay(sd)
                    selectedSavedIds = selectedSavedIds - sd.id
                    pendingDeleteSaved = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSaved = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun RunningCard(
    log: ActivityLog,
    onStop: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(log.id, log.pausedAt) {
        while (true) {
            delay(1000)
            nowMs = System.currentTimeMillis()
        }
    }
    val elapsedSec = log.activeSeconds(nowMs)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (log.isPaused) stringResource(R.string.day_paused)
                        else stringResource(R.string.day_running),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(log.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(Time.formatStopwatch(elapsedSec), style = MaterialTheme.typography.titleLarge)
                }
                if (log.isPaused) {
                    IconButton(onClick = onResume) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.action_resume))
                    }
                } else {
                    IconButton(onClick = onPause) {
                        Icon(Icons.Filled.Pause, contentDescription = stringResource(R.string.action_pause))
                    }
                }
                IconButton(onClick = onStop) {
                    Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.action_stop))
                }
            }
        }
    }
}

@Composable
private fun StatsBlock(logs: List<ActivityLog>, categories: List<ActivityCategory>) {
    val now = System.currentTimeMillis()
    val byCategory = logs.groupBy { it.categoryId }
        .mapValues { (_, list) -> list.sumOf { it.activeSeconds(now) } }
    val totalSec = byCategory.values.sum()

    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatTile("Записей", logs.size.toString(), Modifier.weight(1f))
            StatTile(stringResource(R.string.day_total_hours), Time.formatDuration(totalSec), Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        val slices = categories.mapNotNull { cat ->
            val sec = byCategory[cat.id] ?: 0L
            if (sec > 0) CategorySlice(cat.name, parseColor(cat.colorHex), sec) else null
        }
        val uncategorized = byCategory[null] ?: 0L
        val allSlices = if (uncategorized > 0) {
            slices + CategorySlice("Без категории", MaterialTheme.colorScheme.outline, uncategorized)
        } else slices
        if (allSlices.isEmpty()) {
            Text(
                "Нет записей за выбранный период.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            CategoryStackBar(slices = allSlices)
        }
    }
}

@Composable
private fun CombinedStatsBlock(logs: List<ActivityLog>, categories: List<ActivityCategory>) {
    val now = System.currentTimeMillis()
    val byCategory = logs.groupBy { it.categoryId }
        .mapValues { (_, list) -> list.sumOf { it.activeSeconds(now) } }
    val totalSec = byCategory.values.sum()

    val slices = categories.mapNotNull { cat ->
        val sec = byCategory[cat.id] ?: 0L
        if (sec > 0) CategorySlice(cat.name, parseColor(cat.colorHex), sec) else null
    }
    val uncategorized = byCategory[null] ?: 0L
    val allSlices = if (uncategorized > 0) {
        slices + CategorySlice("Без категории", MaterialTheme.colorScheme.outline, uncategorized)
    } else slices

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("Записей", logs.size.toString(), Modifier.weight(1f))
            StatTile(stringResource(R.string.day_total_hours), Time.formatDuration(totalSec), Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        if (allSlices.isEmpty()) {
            Text(
                "Нет записей в выбранных днях.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PieChart(slices = allSlices)
                Column(Modifier.weight(1f)) {
                    allSlices.forEach { slice ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .height(10.dp)
                                    .width(10.dp)
                                    .padding(end = 8.dp)
                            ) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(slice.color)
                                }
                            }
                            Text(
                                slice.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                Time.formatDuration(slice.seconds),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogRow(log: ActivityLog, categories: List<ActivityCategory>, onDelete: () -> Unit) {
    val cat = categories.firstOrNull { it.id == log.categoryId }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(log.id, log.endedAt, log.pausedAt) {
        if (log.isRunning) {
            while (true) {
                delay(1000)
                nowMs = System.currentTimeMillis()
            }
        }
    }
    val durationSec = log.activeSeconds(nowMs)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(log.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        append(Time.formatDate(log.startedAt))
                        append(" • ")
                        append(Time.formatTime(log.startedAt))
                        log.endedAt?.let { append(" – ${Time.formatTime(it)}") }
                        append(" • ")
                        append(Time.formatDuration(durationSec))
                        if (log.isPaused) append(" • на паузе")
                        else if (log.isRunning) append(" • идёт")
                        cat?.let { append(" • ${it.name}") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun SavedDayRow(
    day: SavedDay,
    selected: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Checkbox(checked = selected, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(day.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${Time.formatDateTime(day.fromMs)} – ${Time.formatDateTime(day.toMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                day.note?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.day_edit_label))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun CreateCategoryDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.day_create_category)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.day_category_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddManualDialog(
    categories: List<ActivityCategory>,
    onDismiss: () -> Unit,
    onAdd: (categoryId: Long?, title: String, startedAt: Long, endedAt: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var durationMin by remember { mutableStateOf("") }
    // Default to "Прочее" if it exists, else first.
    var selectedCat by remember {
        mutableStateOf(
            categories.firstOrNull { it.name.equals("Прочее", ignoreCase = true) }?.id
                ?: categories.firstOrNull()?.id
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.day_add_manual)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = durationMin,
                    onValueChange = { durationMin = it.filter { c -> c.isDigit() } },
                    label = { Text("Длительность, мин") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Категория", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCat == cat.id,
                            onClick = { selectedCat = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val minutes = durationMin.toIntOrNull() ?: 0
                    if (minutes <= 0 || title.isBlank()) return@TextButton
                    val now = System.currentTimeMillis()
                    val started = now - minutes * 60_000L
                    onAdd(selectedCat, title, started, now)
                },
                enabled = title.isNotBlank() && (durationMin.toIntOrNull() ?: 0) > 0
            ) { Text(stringResource(R.string.action_save)) }
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
        title = { Text(stringResource(R.string.day_custom_period)) },
        text = {
            Column {
                Text(
                    "Покажем статистику за указанное количество последних дней.",
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

@Composable
private fun SaveDayDialog(
    initialFrom: Long,
    initialTo: Long,
    onDismiss: () -> Unit,
    onSave: (label: String, from: Long, to: Long, note: String?) -> Unit
) {
    var label by remember { mutableStateOf(Time.formatDate(initialFrom)) }
    var fromInput by remember { mutableStateOf(Time.formatDateTime(initialFrom)) }
    var toInput by remember { mutableStateOf(Time.formatDateTime(initialTo)) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.day_save_day)) },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.day_saved_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = fromInput,
                    onValueChange = { fromInput = it; error = null },
                    label = { Text(stringResource(R.string.task_deadline_from)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = toInput,
                    onValueChange = { toInput = it; error = null },
                    label = { Text(stringResource(R.string.task_deadline_to)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.day_saved_note)) },
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val from = Time.parseDateTime(fromInput)
                val to = Time.parseDateTime(toInput)
                if (from == null || to == null || to <= from) {
                    error = "Проверьте даты"
                    return@TextButton
                }
                onSave(label.ifBlank { Time.formatDate(from) }, from, to, note.ifBlank { null })
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun EditSavedDayDialog(
    day: SavedDay,
    onDismiss: () -> Unit,
    onSave: (SavedDay) -> Unit
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as ProgressVisionApp
    val vm: DayViewModel = viewModel(factory = AppViewModelFactory(app))
    val categories by vm.categories.collectAsState()
    val logs by remember(day.fromMs, day.toMs) {
        vm.logsBetween(day.fromMs, day.toMs)
    }.collectAsState(initial = emptyList())

    var label by remember { mutableStateOf(day.label) }
    var fromInput by remember { mutableStateOf(Time.formatDateTime(day.fromMs)) }
    var toInput by remember { mutableStateOf(Time.formatDateTime(day.toMs)) }
    var note by remember { mutableStateOf(day.note.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    var editingLog by remember { mutableStateOf<ActivityLog?>(null) }
    var addingLog by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text(
                    stringResource(R.string.day_edit_label),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.day_saved_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = fromInput,
                    onValueChange = { fromInput = it; error = null },
                    label = { Text(stringResource(R.string.task_deadline_from)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = toInput,
                    onValueChange = { toInput = it; error = null },
                    label = { Text(stringResource(R.string.task_deadline_to)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.day_saved_note)) },
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.day_edit_contents),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { addingLog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.day_edit_add_log))
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (logs.isEmpty()) {
                    Text(
                        stringResource(R.string.day_edit_no_logs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        logs.forEach { log ->
                            EditableLogRow(
                                log = log,
                                categories = categories,
                                onEdit = { editingLog = log },
                                onDelete = { vm.deleteLog(log) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        val from = Time.parseDateTime(fromInput)
                        val to = Time.parseDateTime(toInput)
                        if (from == null || to == null || to <= from) {
                            error = "Проверьте даты"
                            return@TextButton
                        }
                        onSave(
                            day.copy(
                                label = label.ifBlank { Time.formatDate(from) },
                                fromMs = from,
                                toMs = to,
                                note = note.ifBlank { null }
                            )
                        )
                    }) { Text(stringResource(R.string.action_save)) }
                }
            }
        }
    }

    editingLog?.let { log ->
        EditLogDialog(
            log = log,
            categories = categories,
            onDismiss = { editingLog = null },
            onSave = { updated ->
                vm.saveLog(updated)
                editingLog = null
            }
        )
    }
    if (addingLog) {
        EditLogDialog(
            log = ActivityLog(
                categoryId = categories.firstOrNull()?.id,
                title = "",
                startedAt = day.fromMs,
                endedAt = (day.fromMs + 60 * 60_000L).coerceAtMost(day.toMs)
            ),
            categories = categories,
            onDismiss = { addingLog = false },
            onSave = { newLog ->
                vm.saveLog(newLog)
                addingLog = false
            }
        )
    }
}

@Composable
private fun EditableLogRow(
    log: ActivityLog,
    categories: List<ActivityCategory>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cat = categories.firstOrNull { it.id == log.categoryId }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(log.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    buildString {
                        append(Time.formatTime(log.startedAt))
                        log.endedAt?.let { append("–${Time.formatTime(it)}") }
                        append(" • ")
                        append(Time.formatDuration(log.activeSeconds()))
                        cat?.let { append(" • ${it.name}") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = null)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun EditLogDialog(
    log: ActivityLog,
    categories: List<ActivityCategory>,
    onDismiss: () -> Unit,
    onSave: (ActivityLog) -> Unit
) {
    var title by remember { mutableStateOf(log.title) }
    var startedInput by remember { mutableStateOf(Time.formatDateTime(log.startedAt)) }
    var endedInput by remember {
        mutableStateOf(log.endedAt?.let { Time.formatDateTime(it) } ?: "")
    }
    var selectedCat by remember { mutableStateOf(log.categoryId) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (log.id == 0L) stringResource(R.string.day_edit_add_log) else stringResource(R.string.day_edit_label))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.day_edit_log_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = startedInput,
                    onValueChange = { startedInput = it; error = null },
                    label = { Text(stringResource(R.string.day_edit_log_started)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = endedInput,
                    onValueChange = { endedInput = it; error = null },
                    label = { Text(stringResource(R.string.day_edit_log_ended)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.day_edit_log_category),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCat == cat.id,
                            onClick = { selectedCat = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }
                error?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val started = Time.parseDateTime(startedInput)
                val ended = if (endedInput.isBlank()) null else Time.parseDateTime(endedInput)
                if (started == null) {
                    error = "Проверьте дату начала"; return@TextButton
                }
                if (ended != null && ended <= started) {
                    error = "Конец должен быть позже начала"; return@TextButton
                }
                if (title.isBlank()) {
                    error = "Укажите название"; return@TextButton
                }
                onSave(
                    log.copy(
                        title = title.trim(),
                        startedAt = started,
                        endedAt = ended,
                        categoryId = selectedCat,
                        // Reset paused state when editing - editor is for completed logs.
                        pausedAt = null
                    )
                )
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

private fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: IllegalArgumentException) {
    Color(0xFF6750A4)
}
