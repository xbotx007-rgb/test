package com.progressvision.app.ui.screens.task

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.progressvision.app.ProgressVisionApp
import com.progressvision.app.data.entities.SubTask
import com.progressvision.app.ui.AppViewModelFactory
import com.progressvision.app.ui.components.ProgressRing
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(taskId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ProgressVisionApp
    val viewModel: TaskDetailViewModel = viewModel(
        factory = AppViewModelFactory(app, taskId),
        key = "task-detail-$taskId"
    )
    val task by viewModel.task.collectAsStateWithLifecycle()
    val subs by viewModel.subTasks.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val timerActive = subs.any { it.timerStartedAt != null }
    LaunchedEffect(timerActive) {
        while (timerActive) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val progress = remember(subs) {
        if (subs.isEmpty()) 0f
        else {
            val totalWeight = subs.sumOf { it.weight }.coerceAtLeast(1)
            val doneWeight = subs.filter { it.isDone }.sumOf { it.weight }
            doneWeight.toFloat() / totalWeight.toFloat()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(task?.title ?: "Задача") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить шаг")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ProgressRing(progress = progress)
                }
            }
            item {
                val done = subs.count { it.isDone }
                val totalSpent = subs.sumOf { it.totalSpentMs } +
                    subs.filter { it.timerStartedAt != null }
                        .sumOf { (now - (it.timerStartedAt ?: now)).coerceAtLeast(0L) }
                Column {
                    Text(
                        text = "Готово $done из ${subs.size}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Потрачено времени: ${formatHm(totalSpent)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (task?.description?.isNotBlank() == true) {
                        Spacer(Modifier.height(8.dp))
                        Text(task!!.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                Text("Подзадачи", style = MaterialTheme.typography.titleMedium)
            }
            if (subs.isEmpty()) {
                item {
                    Text(
                        "Разбей цель на конкретные шаги — каждый закрытый шаг даст +% к прогрессу.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(subs, key = { it.id }) { sub ->
                    SubTaskRow(
                        sub = sub,
                        now = now,
                        onToggle = { viewModel.toggle(sub) },
                        onTimer = { viewModel.toggleTimer(sub) },
                        onDelete = { viewModel.deleteSub(sub) }
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddSubTaskDialog(
            onDismiss = { showAdd = false },
            onConfirm = { title, weight ->
                viewModel.addSubTask(title, weight)
                showAdd = false
            }
        )
    }
}

@Composable
private fun SubTaskRow(
    sub: SubTask,
    now: Long,
    onToggle: () -> Unit,
    onTimer: () -> Unit,
    onDelete: () -> Unit
) {
    val running = sub.timerStartedAt != null
    val elapsed = sub.totalSpentMs +
        if (running) (now - (sub.timerStartedAt ?: now)).coerceAtLeast(0L) else 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (running)
                MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = sub.isDone, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(sub.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${formatHm(elapsed)}${if (sub.weight > 1) " · вес ${sub.weight}" else ""}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onTimer) {
                Icon(
                    if (running) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                    contentDescription = if (running) "Стоп" else "Старт"
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
            }
        }
    }
}

@Composable
private fun AddSubTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("1") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая подзадача") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Что нужно сделать") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weight, onValueChange = { weight = it.filter(Char::isDigit) },
                    label = { Text("Сложность (1-10)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(title, weight.toIntOrNull()?.coerceIn(1, 10) ?: 1)
            }) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

private fun formatHm(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return when {
        h > 0 -> "${h}ч ${m}м"
        m > 0 -> "${m}м ${s}с"
        else -> "${s}с"
    }
}
