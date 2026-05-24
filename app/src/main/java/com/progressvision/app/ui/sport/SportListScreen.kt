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
import androidx.compose.material3.Switch
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
import com.progressvision.app.data.entity.SportEntry
import com.progressvision.app.data.entity.SportWorkout
import com.progressvision.app.data.entity.SportWorkoutType
import com.progressvision.app.ui.common.EmptyState
import com.progressvision.app.ui.common.LineChart
import com.progressvision.app.ui.common.SectionCard
import com.progressvision.app.ui.common.StatTile
import com.progressvision.app.util.Time
import com.progressvision.app.viewmodel.AppViewModelFactory
import com.progressvision.app.viewmodel.SportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportListScreen(onOpen: (Long) -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as ProgressVisionApp
    val vm: SportViewModel = viewModel(factory = AppViewModelFactory(app))
    val workouts by vm.workouts.collectAsState()
    val allEntries by vm.allEntries().collectAsState(initial = emptyList())

    var showCreate by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<SportWorkout?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_sport)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.sport_create_workout))
            }
        }
    ) { padding ->
        if (workouts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(stringResource(R.string.sport_no_workouts))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AllWorkoutsAnalyticsSection(workouts = workouts, entries = allEntries)
                }
                item {
                    MeasurementComparisonSection(workouts = workouts, entries = allEntries)
                }
                items(workouts, key = { it.id }) { workout ->
                    WorkoutCard(
                        workout = workout,
                        onClick = { onOpen(workout.id) },
                        onDelete = { pendingDelete = workout }
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreateWorkoutDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, type, proMode ->
                vm.createWorkout(name, type, proMode)
                showCreate = false
            }
        )
    }
    pendingDelete?.let { w ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(w.name) },
            text = { Text("Удалить тренировку вместе со всеми упражнениями?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteWorkout(w); pendingDelete = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun AllWorkoutsAnalyticsSection(
    workouts: List<SportWorkout>,
    entries: List<SportEntry>
) {
    val totalSets = entries.sumOf { it.sets ?: 0 }
    val totalTonnage = entries.sumOf { ((it.sets ?: 0) * (it.reps ?: 0) * (it.weightKg ?: 0f)).toDouble() }
    val totalWorkouts = workouts.size
    SectionCard(title = stringResource(R.string.sport_analytics_all)) {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile("Тренировок", totalWorkouts.toString(), Modifier.weight(1f))
                StatTile(stringResource(R.string.sport_sets_total), totalSets.toString(), Modifier.weight(1f))
                StatTile(
                    stringResource(R.string.sport_tonnage),
                    "${"%.0f".format(totalTonnage)} кг",
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MeasurementComparisonSection(
    workouts: List<SportWorkout>,
    entries: List<SportEntry>
) {
    val hasMeasurement = workouts.any { it.type == SportWorkoutType.MEASUREMENT }
    if (!hasMeasurement) return

    SectionCard(title = stringResource(R.string.sport_analytics_measurement)) {
        Column {
            Text(
                "Прогресс тренировок (синяя) сравнивается с замерными (зелёная).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            val sorted = entries.sortedBy { it.date }
            val daily = sorted.groupBy { Time.startOfDay(it.date) }
                .mapValues { (_, list) -> list.sumOf { it.primaryMetric(com.progressvision.app.data.entity.SportType.STRENGTH).toDouble() } }
            val seriesAll = daily.values.map { it.toFloat() }
            if (seriesAll.isNotEmpty()) {
                LineChart(points = seriesAll)
            } else {
                Text(
                    "Нет данных для сравнения.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WorkoutCard(workout: SportWorkout, onClick: () -> Unit, onDelete: () -> Unit) {
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
                Text(workout.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val typeLabel = when (workout.type) {
                    SportWorkoutType.REGULAR -> stringResource(R.string.sport_workout_type_regular)
                    SportWorkoutType.MEASUREMENT -> stringResource(R.string.sport_workout_type_measurement)
                }
                val proSuffix = if (workout.proMode) " • Pro" else ""
                Text(
                    text = "$typeLabel$proSuffix • ${Time.formatDate(workout.createdAt)}",
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
private fun CreateWorkoutDialog(
    onDismiss: () -> Unit,
    onCreate: (String, SportWorkoutType, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(SportWorkoutType.REGULAR) }
    var proMode by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sport_create_workout)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.sport_workout_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == SportWorkoutType.REGULAR,
                        onClick = { type = SportWorkoutType.REGULAR },
                        label = { Text(stringResource(R.string.sport_workout_type_regular)) }
                    )
                    FilterChip(
                        selected = type == SportWorkoutType.MEASUREMENT,
                        onClick = { type = SportWorkoutType.MEASUREMENT },
                        label = { Text(stringResource(R.string.sport_workout_type_measurement)) }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.sport_pro_mode), modifier = Modifier.weight(1f))
                    Switch(checked = proMode, onCheckedChange = { proMode = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, type, proMode) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
