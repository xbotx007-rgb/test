package com.progressvision.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.progressvision.app.ProgressVisionApp

class AppViewModelFactory(private val app: ProgressVisionApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SportViewModel::class.java) -> SportViewModel(app) as T
            modelClass.isAssignableFrom(TaskViewModel::class.java) -> TaskViewModel(app) as T
            modelClass.isAssignableFrom(DayViewModel::class.java) -> DayViewModel(app) as T
            else -> throw IllegalArgumentException("Unknown VM: $modelClass")
        }
    }
}
