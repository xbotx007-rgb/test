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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.progressvision.app.ui.common.CategorySlice
import com.progressvision.app.ui.common.CategoryStackBar
import com.progressvision.app.ui.common.EmptyState
import com.progressvision.app.ui.common.SectionCard
import com.progressvision.app.ui.common.StatTile
import com.progressvision.app.util.Time
import com.progressvision.app.viewmodel.AppViewModelFactory
import com.progressvision.app.viewmodel.DayViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DayScreen() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as ProgressVisionApp
    val vm: DayViewModel = viewModel(factory = AppViewModelFactory(app))

    val categories by vm.categories.collectAsState()
    val running by vm.running.collectAsState()
    val today by vm.todayLogs.collectAsState()
    val week by vm.weekLogs.collectAsState()

    var showAddCategory by remember { mutableStateOf(false) }
    var showAddManual by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ActivityLog?>(null) }
    var view by remember { mutableStateOf(View.TODAY) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_day)) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            running?.let { current ->
                item { RunningCard(log = current, onStop = { vm.stopRunning() }) }
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
                    Column {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categories.forEach { cat ->
                                AssistChip(
                                    onClick = { vm.startQuick(cat) },
                                    label = { Text(cat.name) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        labelColor = parseColor(cat.colorHex)
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { showAddManual = true },
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.day_add_manual))
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = view == View.TODAY,
                        onClick = { view = View.TODAY },
                        label = { Text(stringResource(R.string.day_today)) }
                    )
                    FilterChip(
                        selected = view == View.WEEK,
                        onClick = { view = View.WEEK },
                        label = { Text(stringResource(R.string.day_week)) }
                    )
                }
            }

            val displayLogs = if (view == View.TODAY) today else week
            item {
                SectionCard(title = stringResource(R.string.day_balance)) {
                    StatsBlock(logs = displayLogs, categories = categories)
                }
            }

            if (displayLogs.isEmpty()) {
                item { EmptyHint(stringResource(R.string.day_no_logs)) }
            } else {
                items(displayLogs, key = { it.id }) { log ->
                    LogRow(
                        log = log,
                        categories = categories,
                        onDelete = { pendingDelete = log }
                    )
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
}

private enum class View { TODAY, WEEK }

@Composable
private fun EmptyHint(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text,
            modifier = Modifier.padding(20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RunningCard(log: ActivityLog, onStop: () -> Unit) {
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(log.id) {
        while (true) { delay(1000); nowMs = System.currentTimeMillis() }
    }
    val elapsedSec = ((nowMs - log.startedAt) / 1000L).coerceAtLeast(0)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.day_running), style = MaterialTheme.typography.labelMedium)
                Text(log.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(Time.formatStopwatch(elapsedSec), style = MaterialTheme.typography.titleLarge)
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.action_stop))
            }
        }
    }
}

@Composable
private fun StatsBlock(logs: List<ActivityLog>, categories: List<ActivityCategory>) {
    val now = System.currentTimeMillis()
    val byCategory = logs.groupBy { it.categoryId }
        .mapValues { (_, list) -> list.sumOf { ((it.endedAt ?: now) - it.startedAt) / 1000L } }
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
        if (slices.isEmpty()) {
            Text(
                "Нет записей за выбранный период.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            CategoryStackBar(slices = slices)
        }
    }
}

@Composable
private fun LogRow(log: ActivityLog, categories: List<ActivityCategory>, onDelete: () -> Unit) {
    val cat = categories.firstOrNull { it.id == log.categoryId }
    val duration = log.endedAt?.let { (it - log.startedAt) / 1000L } ?: 0L
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
                        append(Time.formatTime(log.startedAt))
                        log.endedAt?.let { append(" – ${Time.formatTime(it)}") }
                        append(" • ")
                        append(Time.formatDuration(duration))
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
    var selectedCat by remember { mutableStateOf<Long?>(categories.firstOrNull()?.id) }
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

private fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: IllegalArgumentException) {
    Color(0xFF6750A4)
}
