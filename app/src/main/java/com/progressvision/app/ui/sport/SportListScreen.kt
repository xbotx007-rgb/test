package com.progressvision.app.ui.sport

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.progressvision.app.data.entity.SportTracker
import com.progressvision.app.data.entity.SportType
import com.progressvision.app.ui.common.EmptyState
import com.progressvision.app.viewmodel.AppViewModelFactory
import com.progressvision.app.viewmodel.SportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportListScreen(onOpen: (Long) -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as ProgressVisionApp
    val vm: SportViewModel = viewModel(factory = AppViewModelFactory(app))
    val trackers by vm.trackers.collectAsState()

    var showCreate by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<SportTracker?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_sport)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.sport_create_tracker))
            }
        }
    ) { padding ->
        if (trackers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(stringResource(R.string.sport_no_trackers))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trackers, key = { it.id }) { tracker ->
                    TrackerCard(
                        tracker = tracker,
                        onClick = { onOpen(tracker.id) },
                        onDelete = { pendingDelete = tracker }
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreateTrackerDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, type ->
                vm.createTracker(name, type)
                showCreate = false
            }
        )
    }
    pendingDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(t.name) },
            text = { Text("Удалить трекер вместе со всей историей?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteTracker(t); pendingDelete = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun TrackerCard(tracker: SportTracker, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(tracker.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = when (tracker.type) {
                        SportType.STRENGTH -> stringResource(R.string.sport_type_strength)
                        SportType.BODYWEIGHT -> stringResource(R.string.sport_type_bodyweight)
                        SportType.CARDIO -> stringResource(R.string.sport_type_cardio)
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
private fun CreateTrackerDialog(onDismiss: () -> Unit, onCreate: (String, SportType) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(SportType.STRENGTH) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sport_create_tracker)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.sport_tracker_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == SportType.STRENGTH,
                        onClick = { type = SportType.STRENGTH },
                        label = { Text(stringResource(R.string.sport_type_strength)) }
                    )
                    FilterChip(
                        selected = type == SportType.BODYWEIGHT,
                        onClick = { type = SportType.BODYWEIGHT },
                        label = { Text(stringResource(R.string.sport_type_bodyweight)) }
                    )
                    FilterChip(
                        selected = type == SportType.CARDIO,
                        onClick = { type = SportType.CARDIO },
                        label = { Text(stringResource(R.string.sport_type_cardio)) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, type) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
