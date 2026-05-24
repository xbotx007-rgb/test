package com.progressvision.app.ui.screens.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.progressvision.app.data.entities.BigTask
import com.progressvision.app.data.entities.SubTask
import com.progressvision.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskDetailViewModel(
    private val repository: TaskRepository,
    private val taskId: Long
) : ViewModel() {

    val task: StateFlow<BigTask?> = repository.bigTask(taskId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val subTasks: StateFlow<List<SubTask>> = repository.subTasks(taskId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addSubTask(title: String, weight: Int) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addSubTask(
                SubTask(
                    bigTaskId = taskId,
                    title = title.trim(),
                    weight = weight.coerceAtLeast(1),
                    orderIndex = (subTasks.value.maxOfOrNull { it.orderIndex } ?: 0) + 1
                )
            )
        }
    }

    fun toggle(sub: SubTask) {
        viewModelScope.launch {
            repository.toggleSubTask(sub)
            maybeCompleteBigTask()
        }
    }

    fun deleteSub(sub: SubTask) {
        viewModelScope.launch {
            if (sub.timerStartedAt != null) {
                val title = task.value?.title.orEmpty()
                repository.stopTimer(sub, title)
            }
            repository.deleteSubTask(sub)
        }
    }

    fun toggleTimer(sub: SubTask) {
        viewModelScope.launch {
            if (sub.timerStartedAt == null) {
                repository.startTimer(sub)
            } else {
                repository.stopTimer(sub, task.value?.title.orEmpty())
            }
        }
    }

    private suspend fun maybeCompleteBigTask() {
        val current = task.value ?: return
        val all = subTasks.value
        if (all.isNotEmpty() && all.all { it.isDone } && current.completedAt == null) {
            repository.updateBigTask(current.copy(completedAt = System.currentTimeMillis()))
        } else if (current.completedAt != null && all.any { !it.isDone }) {
            repository.updateBigTask(current.copy(completedAt = null))
        }
    }
}
