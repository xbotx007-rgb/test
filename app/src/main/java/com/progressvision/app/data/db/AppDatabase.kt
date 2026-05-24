package com.progressvision.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.progressvision.app.data.entities.BigTask
import com.progressvision.app.data.entities.DayActivity
import com.progressvision.app.data.entities.DayCategory
import com.progressvision.app.data.entities.SportEntry
import com.progressvision.app.data.entities.SportTracker
import com.progressvision.app.data.entities.SubTask

@Database(
    entities = [
        SportTracker::class,
        SportEntry::class,
        BigTask::class,
        SubTask::class,
        DayCategory::class,
        DayActivity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sportDao(): SportDao
    abstract fun taskDao(): TaskDao
    abstract fun dayDao(): DayDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "progressvision.db"
            ).build().also { instance = it }
        }
    }
}
