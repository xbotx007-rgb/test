package com.progressvision.app.ui.screens.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.progressvision.app.data.entities.BigTask
import com.progressvision.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskListViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    val tasks: StateFlow<List<BigTask>> = repository.bigTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createTask(title: String, description: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.saveBigTask(BigTask(title = title.trim(), description = description.trim()))
        }
    }

    fun deleteTask(task: BigTask) {
        viewModelScope.launch { repository.deleteBigTask(task) }
    }
}
