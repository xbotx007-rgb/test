package com.progressvision.app.ui.screens.sport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.progressvision.app.data.entities.SportEntry
import com.progressvision.app.data.entities.SportTracker
import com.progressvision.app.data.repository.SportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SportDetailViewModel(
    private val repository: SportRepository,
    private val trackerId: Long
) : ViewModel() {

    private val _tracker = MutableStateFlow<SportTracker?>(null)
    val tracker: StateFlow<SportTracker?> = _tracker.asStateFlow()

    val entries: StateFlow<List<SportEntry>> = repository.entries(trackerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _tracker.value = repository.trackerById(trackerId)
        }
    }

    fun addEntry(
        sets: Int?,
        reps: Int?,
        weightKg: Double?,
        distanceKm: Double?,
        durationSec: Long?,
        notes: String?
    ) {
        val current = _tracker.value ?: return
        viewModelScope.launch {
            repository.logEntry(
                current,
                SportEntry(
                    trackerId = trackerId,
                    sets = sets,
                    reps = reps,
                    weightKg = weightKg,
                    distanceKm = distanceKm,
                    durationSec = durationSec,
                    notes = notes
                )
            )
        }
    }

    fun deleteEntry(entry: SportEntry) {
        viewModelScope.launch { repository.deleteEntry(entry) }
    }
}
