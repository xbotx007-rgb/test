package com.progressvision.app.ui.task

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.progressvision.app.ProgressVisionApp
import com.progressvision.app.R
import com.progressvision.app.data.entity.BigTask
import com.progressvision.app.data.repository.progressPercent
import com.progressvision.app.ui.common.EmptyState
import com.progressvision.app.ui.common.ProgressRing
import com.progressvision.app.util.Time
import com.progressvision.app.viewmodel.AppViewModelFactory
import com.progressvision.app.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(onOpen: (Long) -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as ProgressVisionApp
    val vm: TaskViewModel = viewModel(factory = AppViewModelFactory(app))
    val tasks by vm.bigTasks.collectAsState()

    var showCreate by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BigTask?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_task)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.task_create))
            }
        }
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(stringResource(R.string.task_no_tasks))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    BigTaskCard(
                        task = task,
                        vm = vm,
                        onOpen = { onOpen(task.id) },
                        onDelete = { pendingDelete = task }
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreateTaskDialog(
            onDismiss = { showCreate = false },
            onCreate = { title, desc, from, to ->
                vm.createBigTask(title, desc, from, to)
                showCreate = false
            }
        )
    }
    pendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(task.title) },
            text = { Text("Удалить задачу вместе со всеми подзадачами?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteBigTask(task); pendingDelete = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun BigTaskCard(task: BigTask, vm: TaskViewModel, onOpen: () -> Unit, onDelete: () -> Unit) {
    val subs by vm.subTasks(task.id).collectAsState(initial = emptyList())
    val percent = subs.progressPercent()
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProgressRing(
                progress = percent / 100f,
                size = 72.dp,
                strokeWidth = 8.dp,
                label = "$percent%"
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                val done = subs.count { it.isDone }
                Text(
                    if (subs.isEmpty()) "Нет подзадач — нажмите, чтобы добавить"
                    else "$done из ${subs.size} выполнено",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val meta = buildList {
                    add("${stringResource(R.string.task_created_at)}: ${Time.formatDateTime(task.createdAt)}")
                    val from = task.deadlineFrom
                    val to = task.deadlineTo
                    if (from != null || to != null) {
                        val left = from?.let { Time.formatDateTime(it) } ?: "—"
                        val right = to?.let { Time.formatDateTime(it) } ?: "—"
                        add("${stringResource(R.string.task_deadline)}: $left → $right")
                    }
                    task.completedAt?.let {
                        add("${stringResource(R.string.task_completed_at)}: ${Time.formatDateTime(it)}")
                    }
                }
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        meta.joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String, deadlineFrom: Long?, deadlineTo: Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var fromInput by remember { mutableStateOf("") }
    var toInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.task_create)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.task_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.task_description)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.task_deadline), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
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
                error?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val from = if (fromInput.isBlank()) null else Time.parseDateTime(fromInput)
                    val to = if (toInput.isBlank()) null else Time.parseDateTime(toInput)
                    if (fromInput.isNotBlank() && from == null) {
                        error = "Не удалось разобрать дату начала"; return@TextButton
                    }
                    if (toInput.isNotBlank() && to == null) {
                        error = "Не удалось разобрать дату окончания"; return@TextButton
                    }
                    onCreate(title, description, from, to)
                },
                enabled = title.isNotBlank()
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
