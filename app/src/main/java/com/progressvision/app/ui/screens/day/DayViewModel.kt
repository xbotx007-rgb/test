package com.progressvision.app.ui.screens.day

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.progressvision.app.data.entities.DayActivity
import com.progressvision.app.data.entities.DayCategory
import com.progressvision.app.data.repository.DayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class DayRange(val from: Long, val to: Long)

@OptIn(ExperimentalCoroutinesApi::class)
class DayViewModel(private val repository: DayRepository) : ViewModel() {

    private val _runningTimer = MutableStateFlow<RunningTimer?>(null)
    val runningTimer: StateFlow<RunningTimer?> = _runningTimer.asStateFlow()

    val categories: StateFlow<List<DayCategory>> = repository.categories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _range = MutableStateFlow(currentDayRange())
    val range: StateFlow<DayRange> = _range.asStateFlow()

    val activitiesToday: StateFlow<List<DayActivity>> = _range
        .flatMapLatest { repository.activities(it.from, it.to) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activitiesWeek: StateFlow<List<DayActivity>> = repository
        .activities(weekStart(), weekEnd())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.saveCategory(DayCategory(name = name.trim()))
        }
    }

    fun deleteCategory(category: DayCategory) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }

    fun startTimer(category: DayCategory) {
        if (_runningTimer.value != null) return
        _runningTimer.value = RunningTimer(
            categoryId = category.id,
            categoryName = category.name,
            startedAt = System.currentTimeMillis()
        )
    }

    fun stopTimer() {
        val timer = _runningTimer.value ?: return
        _runningTimer.value = null
        val now = System.currentTimeMillis()
        if (now - timer.startedAt < 1000L) return
        viewModelScope.launch {
            repository.logActivity(
                DayActivity(
                    categoryId = timer.categoryId,
                    title = timer.categoryName,
                    startedAt = timer.startedAt,
                    endedAt = now,
                    source = "timer"
                )
            )
        }
    }

    fun repeatLast() {
        viewModelScope.launch {
            val last = repository.lastActivity() ?: return@launch
            val now = System.currentTimeMillis()
            repository.logActivity(
                last.copy(
                    id = 0,
                    startedAt = now - last.durationMs,
                    endedAt = now
                )
            )
        }
    }

    fun manualEntry(category: DayCategory?, title: String, durationMinutes: Int) {
        if (durationMinutes <= 0) return
        val now = System.currentTimeMillis()
        val durationMs = durationMinutes * 60_000L
        viewModelScope.launch {
            repository.logActivity(
                DayActivity(
                    categoryId = category?.id,
                    title = title.ifBlank { category?.name ?: "Активность" },
                    startedAt = now - durationMs,
                    endedAt = now
                )
            )
        }
    }

    fun deleteActivity(activity: DayActivity) {
        viewModelScope.launch { repository.deleteActivity(activity) }
    }

    data class RunningTimer(val categoryId: Long, val categoryName: String, val startedAt: Long)
}

private fun startOfDay(time: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = time
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun currentDayRange(): DayRange {
    val from = startOfDay()
    return DayRange(from, from + 24L * 60 * 60 * 1000 - 1)
}

private fun weekStart(): Long {
    val cal = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        timeInMillis = startOfDay()
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }
    return cal.timeInMillis
}

private fun weekEnd(): Long = weekStart() + 7L * 24 * 60 * 60 * 1000 - 1
