package com.progressvision.app.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.progressvision.app.ProgressVisionApp
import com.progressvision.app.data.entity.ActivityCategory
import com.progressvision.app.data.entity.ActivityLog
import com.progressvision.app.data.repository.DayRepository
import com.progressvision.app.util.Time
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DayViewModel(app: ProgressVisionApp) : AndroidViewModel(app) {
    private val repo: DayRepository = app.dayRepo

    val categories: StateFlow<List<ActivityCategory>> = repo.categories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val running: StateFlow<ActivityLog?> = repo.running()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayLogs: StateFlow<List<ActivityLog>> = repo.logsBetween(Time.startOfDay(), Time.endOfDay())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weekLogs: StateFlow<List<ActivityLog>> = repo.logsBetween(Time.startOfWeek(), Time.endOfDay())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.upsertCategory(ActivityCategory(name = name.trim()))
        }
    }

    fun deleteCategory(category: ActivityCategory) {
        viewModelScope.launch { repo.deleteCategory(category) }
    }

    fun startQuick(category: ActivityCategory) {
        viewModelScope.launch { repo.startActivity(category) }
    }

    fun stopRunning() {
        viewModelScope.launch { repo.stopRunning() }
    }

    fun deleteLog(log: ActivityLog) {
        viewModelScope.launch { repo.deleteLog(log) }
    }

    fun addManual(categoryId: Long?, title: String, startedAt: Long, endedAt: Long) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repo.addManual(
                ActivityLog(
                    categoryId = categoryId,
                    title = title.trim(),
                    startedAt = startedAt,
                    endedAt = endedAt,
                    source = "manual"
                )
            )
        }
    }
}
