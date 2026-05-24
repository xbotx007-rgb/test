package com.progressvision.app.ui.screens.sport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.progressvision.app.data.entities.SportKind
import com.progressvision.app.data.entities.SportTracker
import com.progressvision.app.data.repository.SportRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SportListViewModel(
    private val repository: SportRepository
) : ViewModel() {

    val trackers: StateFlow<List<SportTracker>> = repository.trackers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createTracker(name: String, kind: SportKind, unit: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.saveTracker(SportTracker(name = name.trim(), kind = kind, unit = unit))
        }
    }

    fun deleteTracker(tracker: SportTracker) {
        viewModelScope.launch { repository.deleteTracker(tracker) }
    }
}
