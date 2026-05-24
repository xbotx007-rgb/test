package com.progressvision.app.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.progressvision.app.ProgressVisionApp
import com.progressvision.app.data.entity.SportEntry
import com.progressvision.app.data.entity.SportTracker
import com.progressvision.app.data.entity.SportType
import com.progressvision.app.data.repository.SportRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SportViewModel(app: ProgressVisionApp) : AndroidViewModel(app) {
    private val repo: SportRepository = app.sportRepo

    val trackers: StateFlow<List<SportTracker>> = repo.trackers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createTracker(name: String, type: SportType) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.upsertTracker(SportTracker(name = name.trim(), type = type))
        }
    }

    fun deleteTracker(tracker: SportTracker) {
        viewModelScope.launch { repo.deleteTracker(tracker) }
    }

    fun entries(trackerId: Long) = repo.entries(trackerId)
    fun entriesSince(trackerId: Long, since: Long) = repo.entriesSince(trackerId, since)
    suspend fun getTracker(id: Long) = repo.getTracker(id)

    fun addEntry(entry: SportEntry, trackerName: String) {
        viewModelScope.launch { repo.addEntry(entry, trackerName) }
    }

    fun deleteEntry(entry: SportEntry) {
        viewModelScope.launch { repo.deleteEntry(entry) }
    }
}
