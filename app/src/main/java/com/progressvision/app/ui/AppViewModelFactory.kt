package com.progressvision.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.progressvision.app.ProgressVisionApp
import com.progressvision.app.ui.screens.day.DayViewModel
import com.progressvision.app.ui.screens.sport.SportDetailViewModel
import com.progressvision.app.ui.screens.sport.SportListViewModel
import com.progressvision.app.ui.screens.task.TaskDetailViewModel
import com.progressvision.app.ui.screens.task.TaskListViewModel

class AppViewModelFactory(
    private val app: ProgressVisionApp,
    private val argLong: Long = 0L
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SportListViewModel::class.java) ->
                SportListViewModel(app.sportRepository) as T
            modelClass.isAssignableFrom(SportDetailViewModel::class.java) ->
                SportDetailViewModel(app.sportRepository, argLong) as T
            modelClass.isAssignableFrom(TaskListViewModel::class.java) ->
                TaskListViewModel(app.taskRepository) as T
            modelClass.isAssignableFrom(TaskDetailViewModel::class.java) ->
                TaskDetailViewModel(app.taskRepository, argLong) as T
            modelClass.isAssignableFrom(DayViewModel::class.java) ->
                DayViewModel(app.dayRepository) as T
            else -> error("Unknown ViewModel: $modelClass")
        }
    }
}
