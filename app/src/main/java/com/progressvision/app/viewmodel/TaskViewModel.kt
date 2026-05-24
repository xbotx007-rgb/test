package com.progressvision.app.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.progressvision.app.ProgressVisionApp
import com.progressvision.app.data.entity.BigTask
import com.progressvision.app.data.entity.SubTask
import com.progressvision.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(app: ProgressVisionApp) : AndroidViewModel(app) {
    private val repo: TaskRepository = app.taskRepo

    val bigTasks: StateFlow<List<BigTask>> = repo.bigTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createBigTask(title: String, description: String?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repo.upsertBigTask(BigTask(title = title.trim(), description = description?.trim()?.ifBlank { null }))
        }
    }

    fun deleteBigTask(task: BigTask) {
        viewModelScope.launch { repo.deleteBigTask(task) }
    }

    fun updateBigTask(task: BigTask) {
        viewModelScope.launch { repo.updateBigTask(task) }
    }

    fun bigTask(id: Long) = repo.bigTask(id)
    fun subTasks(bigTaskId: Long) = repo.subTasks(bigTaskId)

    fun addSubTask(bigTaskId: Long, title: String, weight: Int = 1) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repo.upsertSubTask(
                SubTask(bigTaskId = bigTaskId, title = title.trim(), weight = weight.coerceAtLeast(1))
            )
        }
    }

    fun toggleSubTask(sub: SubTask) {
        viewModelScope.launch { repo.toggleSubTask(sub) }
    }

    fun deleteSubTask(sub: SubTask) {
        viewModelScope.launch { repo.deleteSubTask(sub) }
    }

    fun recordSubTaskTime(sub: SubTask, bigTaskTitle: String, startedAt: Long, endedAt: Long) {
        viewModelScope.launch { repo.recordSubTaskTime(sub, bigTaskTitle, startedAt, endedAt) }
    }
}
