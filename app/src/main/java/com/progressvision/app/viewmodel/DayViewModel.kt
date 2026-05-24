package com.progressvision.app.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.progressvision.app.ProgressVisionApp
import com.progressvision.app.data.entity.ActivityCategory
import com.progressvision.app.data.entity.ActivityLog
import com.progressvision.app.data.repository.DayRepository
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

    fun pauseRunning() {
        viewModelScope.launch { repo.pauseRunning() }
    }

    fun resumeRunning() {
        viewModelScope.launch { repo.resumeRunning() }
    }

    fun logsBetween(from: Long, to: Long) = repo.logsBetween(from, to)

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
