package com.progressvision.app.ui.task

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.progressvision.app.ProgressVisionApp
import com.progressvision.app.R
import com.progressvision.app.data.entity.BigTask
import com.progressvision.app.data.entity.SubTask
import com.progressvision.app.data.repository.progressPercent
import com.progressvision.app.data.repository.totalTimeSec
import com.progressvision.app.ui.common.ProgressRing
import com.progressvision.app.ui.common.SectionCard
import com.progressvision.app.util.Time
import com.progressvision.app.viewmodel.AppViewModelFactory
import com.progressvision.app.viewmodel.TaskViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(bigTaskId: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as ProgressVisionApp
    val vm: TaskViewModel = viewModel(factory = AppViewModelFactory(app))

    val task by vm.bigTask(bigTaskId).collectAsState(initial = null)
    val subs by vm.subTasks(bigTaskId).collectAsState(initial = emptyList())

    var showAdd by remember { mutableStateOf(false) }
    var timerSub by remember { mutableStateOf<SubTask?>(null) }

    val current = task ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.task_add_subtask))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ProgressHeader(task = current, subs = subs)
            }
            current.description?.takeIf { it.isNotBlank() }?.let {
                item {
                    SectionCard(title = "Описание") {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            items(subs, key = { it.id }) { sub ->
                SubTaskRow(
                    sub = sub,
                    onToggle = { vm.toggleSubTask(sub) },
                    onStartTimer = { timerSub = sub },
                    onDelete = { vm.deleteSubTask(sub) }
                )
            }
        }
    }

    if (showAdd) {
        AddSubTaskDialog(
            onDismiss = { showAdd = false },
            onAdd = { title, weight ->
                vm.addSubTask(bigTaskId, title, weight)
                showAdd = false
            }
        )
    }

    timerSub?.let { sub ->
        TimerDialog(
            sub = sub,
            onDismiss = { timerSub = null },
            onFinish = { started, ended ->
                vm.recordSubTaskTime(sub, current.title, started, ended)
                timerSub = null
            }
        )
    }
}

@Composable
private fun ProgressHeader(task: BigTask, subs: List<SubTask>) {
    val percent = subs.progressPercent()
    val total = subs.totalTimeSec()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProgressRing(progress = percent / 100f, size = 110.dp, strokeWidth = 12.dp, label = "$percent%")
            Spacer(Modifier.width(20.dp))
            Column {
                val done = subs.count { it.isDone }
                Text(
                    "$done из ${subs.size} шагов",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${stringResource(R.string.task_total_time)}: ${Time.formatDuration(total)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SubTaskRow(
    sub: SubTask,
    onToggle: () -> Unit,
    onStartTimer: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = sub.isDone, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(
                    sub.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (sub.isDone) TextDecoration.LineThrough else null
                )
                if (sub.timeSpentSec > 0 || sub.weight > 1) {
                    Row {
                        if (sub.timeSpentSec > 0) {
                            Text(
                                Time.formatDuration(sub.timeSpentSec),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (sub.weight > 1) {
                            if (sub.timeSpentSec > 0) Text(" • ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "вес ${sub.weight}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onStartTimer) {
                Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.task_timer))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun AddSubTaskDialog(onDismiss: () -> Unit, onAdd: (String, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("1") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.task_add_subtask)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.task_subtask_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() } },
                    label = { Text("Вес (1–10)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(title, weight.toIntOrNull() ?: 1) },
                enabled = title.isNotBlank()
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun TimerDialog(
    sub: SubTask,
    onDismiss: () -> Unit,
    onFinish: (startedAt: Long, endedAt: Long) -> Unit
) {
    var running by remember { mutableStateOf(true) }
    val startedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var elapsedSec by remember { mutableStateOf(0L) }

    LaunchedEffect(running) {
        while (running) {
            delay(1000)
            elapsedSec = (System.currentTimeMillis() - startedAt) / 1000
        }
    }

    AlertDialog(
        onDismissRequest = { /* keep timer running */ },
        title = { Text(sub.title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    Time.formatStopwatch(elapsedSec),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = { running = !running }) {
                        Icon(
                            if (running) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (running) stringResource(R.string.action_pause) else stringResource(R.string.action_resume))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onFinish(startedAt, System.currentTimeMillis()) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
