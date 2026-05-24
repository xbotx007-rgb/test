package com.progressvision.app.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.progressvision.app.ProgressVisionApp
import com.progressvision.app.data.entity.SportEntry
import com.progressvision.app.data.entity.SportExercise
import com.progressvision.app.data.entity.SportType
import com.progressvision.app.data.entity.SportWorkout
import com.progressvision.app.data.entity.SportWorkoutType
import com.progressvision.app.data.repository.SportRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SportViewModel(app: ProgressVisionApp) : AndroidViewModel(app) {
    private val repo: SportRepository = app.sportRepo

    val workouts: StateFlow<List<SportWorkout>> = repo.workouts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createWorkout(
        name: String,
        type: SportWorkoutType = SportWorkoutType.REGULAR,
        proMode: Boolean = true
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.upsertWorkout(
                SportWorkout(name = name.trim(), type = type, proMode = proMode)
            )
        }
    }

    fun updateWorkout(workout: SportWorkout) {
        viewModelScope.launch { repo.upsertWorkout(workout) }
    }

    fun deleteWorkout(workout: SportWorkout) {
        viewModelScope.launch { repo.deleteWorkout(workout) }
    }

    suspend fun getWorkout(id: Long) = repo.getWorkout(id)

    fun exercises(workoutId: Long) = repo.exercises(workoutId)
    suspend fun getExercise(id: Long) = repo.getExercise(id)

    fun createExercise(workoutId: Long, name: String, type: SportType) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.upsertExercise(
                SportExercise(workoutId = workoutId, name = name.trim(), type = type)
            )
        }
    }

    fun deleteExercise(exercise: SportExercise) {
        viewModelScope.launch { repo.deleteExercise(exercise) }
    }

    fun entries(exerciseId: Long) = repo.entries(exerciseId)
    fun entriesSince(exerciseId: Long, since: Long) = repo.entriesSince(exerciseId, since)
    fun allEntries() = repo.allEntries()
    fun entriesForWorkout(workoutId: Long) = repo.entriesForWorkout(workoutId)

    fun addEntry(entry: SportEntry, label: String) {
        viewModelScope.launch { repo.addEntry(entry, label) }
    }

    fun deleteEntry(entry: SportEntry) {
        viewModelScope.launch { repo.deleteEntry(entry) }
    }
}
