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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.progressvision.app.ProgressVisionApp
import com.progressvision.app.data.entities.SportKind
import com.progressvision.app.ui.AppViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportListScreen(onTrackerClick: (Long) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ProgressVisionApp
    val viewModel: SportListViewModel = viewModel(factory = AppViewModelFactory(app))
    val trackers by viewModel.trackers.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Спорт") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить трекер")
            }
        }
    ) { padding ->
        if (trackers.isEmpty()) {
            EmptyState(padding)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trackers, key = { it.id }) { tracker ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onTrackerClick(tracker.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tracker.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = kindLabel(tracker.kind) +
                                        if (tracker.unit.isNotBlank()) " · ${tracker.unit}" else "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { viewModel.deleteTracker(tracker) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        NewTrackerDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name, kind, unit ->
                viewModel.createTracker(name, kind, unit)
                showDialog = false
            }
        )
    }
}

@Composable
private fun EmptyState(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Пока нет трекеров", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Создай первый трекер — например, «Подтягивания» или «Бег».",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewTrackerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, SportKind, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(SportKind.STRENGTH) }
    var kindMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый трекер") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = kindMenu,
                    onExpandedChange = { kindMenu = !kindMenu }
                ) {
                    OutlinedTextField(
                        value = kindLabel(kind),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Тип") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(kindMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    androidx.compose.material3.ExposedDropdownMenu(
                        expanded = kindMenu,
                        onDismissRequest = { kindMenu = false }
                    ) {
                        SportKind.entries.forEach { value ->
                            DropdownMenuItem(
                                text = { Text(kindLabel(value)) },
                                onClick = { kind = value; kindMenu = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Ед. изм. (необязательно)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, kind, unit) }) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

internal fun kindLabel(kind: SportKind): String = when (kind) {
    SportKind.STRENGTH -> "Силовое"
    SportKind.BODYWEIGHT -> "С собственным весом"
    SportKind.CARDIO -> "Кардио"
    SportKind.TIME -> "Время"
}
