package com.progressvision.app

import android.app.Application
import com.progressvision.app.data.db.AppDatabase
import com.progressvision.app.data.repository.DayRepository
import com.progressvision.app.data.repository.SportRepository
import com.progressvision.app.data.repository.TaskRepository

class ProgressVisionApp : Application() {

    val db: AppDatabase by lazy { AppDatabase.get(this) }
    val sportRepo: SportRepository by lazy { SportRepository(db.sportDao(), db.dayDao()) }
    val taskRepo: TaskRepository by lazy { TaskRepository(db.taskDao(), db.dayDao()) }
    val dayRepo: DayRepository by lazy { DayRepository(db.dayDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ProgressVisionApp
            private set
    }
}
