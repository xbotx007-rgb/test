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
import com.progressvision.app.data.entity.SportExercise
import com.progressvision.app.data.entity.SportType
import com.progressvision.app.data.entity.SportWorkout
import com.progressvision.app.data.entity.SportWorkoutType
import com.progressvision.app.ui.common.EmptyState
import com.progressvision.app.ui.common.SectionCard
import com.progressvision.app.ui.common.StatTile
import com.progressvision.app.viewmodel.AppViewModelFactory
import com.progressvision.app.viewmodel.SportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportWorkoutDetailScreen(
    workoutId: Long,
    onBack: () -> Unit,
    onOpenExercise: (Long) -> Unit
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as ProgressVisionApp
    val vm: SportViewModel = viewModel(factory = AppViewModelFactory(app))

    var workout by remember { mutableStateOf<SportWorkout?>(null) }
    LaunchedEffect(workoutId) { workout = vm.getWorkout(workoutId) }
    val current = workout ?: return

    val exercises by vm.exercises(workoutId).collectAsState(initial = emptyList())
    val entries by vm.entriesForWorkout(workoutId).collectAsState(initial = emptyList())

    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<SportExercise?>(null) }

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
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.sport_create_exercise))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                WorkoutAnalyticsCard(workout = current, exercises = exercises, entries = entries)
            }
            item {
                Text(
                    stringResource(R.string.sport_exercises),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (exercises.isEmpty()) {
                item {
                    Box(Modifier.fillMaxSize()) {
                        EmptyState(stringResource(R.string.sport_no_exercises))
                    }
                }
            } else {
                items(exercises, key = { it.id }) { ex ->
                    ExerciseCard(
                        exercise = ex,
                        onClick = { onOpenExercise(ex.id) },
                        onDelete = { pendingDelete = ex }
                    )
                }
            }
        }
    }

    if (showAdd) {
        CreateExerciseDialog(
            onDismiss = { showAdd = false },
            onCreate = { name, type ->
                vm.createExercise(workoutId, name, type)
                showAdd = false
            }
        )
    }
    pendingDelete?.let { ex ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(ex.name) },
            text = { Text("Удалить упражнение вместе со всеми подходами?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteExercise(ex); pendingDelete = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun WorkoutAnalyticsCard(
    workout: SportWorkout,
    exercises: List<SportExercise>,
    entries: List<SportEntry>
) {
    val totalSets = entries.sumOf { it.sets ?: 0 }
    val tonnage = entries.sumOf { ((it.sets ?: 0) * (it.reps ?: 0) * (it.weightKg ?: 0f)).toDouble() }
    val maxWeight = entries.mapNotNull { it.weightKg }.maxOrNull() ?: 0f
    val maxReps = entries.mapNotNull { it.reps }.maxOrNull() ?: 0
    val typeLabel = when (workout.type) {
        SportWorkoutType.REGULAR -> stringResource(R.string.sport_workout_type_regular)
        SportWorkoutType.MEASUREMENT -> stringResource(R.string.sport_workout_type_measurement)
    }
    SectionCard(title = "$typeLabel${if (workout.proMode) " • Pro" else ""}") {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile("Упражнений", exercises.size.toString(), Modifier.weight(1f))
                StatTile(stringResource(R.string.sport_sets_total), totalSets.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile(
                    stringResource(R.string.sport_tonnage),
                    "${"%.0f".format(tonnage)} кг",
                    Modifier.weight(1f)
                )
                StatTile(
                    stringResource(R.string.sport_record),
                    if (maxWeight > 0f) "${"%.1f".format(maxWeight)} кг" else "${maxReps} повт.",
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ExerciseCard(exercise: SportExercise, onClick: () -> Unit, onDelete: () -> Unit) {
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
                Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = when (exercise.type) {
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
private fun CreateExerciseDialog(onDismiss: () -> Unit, onCreate: (String, SportType) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(SportType.STRENGTH) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sport_create_exercise)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.sport_exercise_name)) },
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
