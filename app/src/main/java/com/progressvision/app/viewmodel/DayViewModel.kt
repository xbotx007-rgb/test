package com.progressvision.app.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.progressvision.app.ProgressVisionApp
import com.progressvision.app.data.entity.ActivityCategory
import com.progressvision.app.data.entity.ActivityLog
import com.progressvision.app.data.entity.SavedDay
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

    val savedDays: StateFlow<List<SavedDay>> = repo.savedDays()
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

    fun updateLog(log: ActivityLog) {
        viewModelScope.launch { repo.updateLog(log) }
    }

    fun saveLog(log: ActivityLog) {
        viewModelScope.launch {
            if (log.id == 0L) repo.addManual(log)
            else repo.updateLog(log)
        }
    }

    fun addManual(categoryId: Long?, title: String, startedAt: Long, endedAt: Long) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val effectiveCategoryId = categoryId ?: repo.ensureCategory("Прочее")
            repo.addManual(
                ActivityLog(
                    categoryId = effectiveCategoryId,
                    title = title.trim(),
                    startedAt = startedAt,
                    endedAt = endedAt,
                    source = "manual"
                )
            )
        }
    }

    fun saveDay(label: String, fromMs: Long, toMs: Long, note: String? = null) {
        viewModelScope.launch {
            repo.saveDay(
                SavedDay(
                    label = label.ifBlank { Time.formatDate(fromMs) },
                    fromMs = fromMs,
                    toMs = toMs,
                    note = note?.ifBlank { null }
                )
            )
        }
    }

    fun saveToday() {
        val from = Time.startOfDay()
        val to = Time.endOfDay()
        saveDay(label = Time.formatDate(from), fromMs = from, toMs = to)
    }

    fun updateSavedDay(day: SavedDay) {
        viewModelScope.launch { repo.updateSavedDay(day) }
    }

    fun deleteSavedDay(day: SavedDay) {
        viewModelScope.launch { repo.deleteSavedDay(day) }
    }
}
