package com.progressvision.app

import android.app.Application
import com.progressvision.app.data.db.AppDatabase
import com.progressvision.app.data.repository.DayRepository
import com.progressvision.app.data.repository.SportRepository
import com.progressvision.app.data.repository.TaskRepository

class ProgressVisionApp : Application() {

    val database by lazy { AppDatabase.get(this) }
    val dayRepository by lazy { DayRepository(database.dayDao()) }
    val sportRepository by lazy { SportRepository(database.sportDao(), dayRepository) }
    val taskRepository by lazy { TaskRepository(database.taskDao(), dayRepository) }
}
