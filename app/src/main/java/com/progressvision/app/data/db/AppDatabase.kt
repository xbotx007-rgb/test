package com.progressvision.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.progressvision.app.data.entity.ActivityCategory
import com.progressvision.app.data.entity.ActivityLog
import com.progressvision.app.data.entity.BigTask
import com.progressvision.app.data.entity.SavedDay
import com.progressvision.app.data.entity.SportEntry
import com.progressvision.app.data.entity.SportExercise
import com.progressvision.app.data.entity.SportWorkout
import com.progressvision.app.data.entity.SubTask

@Database(
    entities = [
        SportWorkout::class,
        SportExercise::class,
        SportEntry::class,
        BigTask::class,
        SubTask::class,
        ActivityCategory::class,
        ActivityLog::class,
        SavedDay::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sportDao(): SportDao
    abstract fun taskDao(): TaskDao
    abstract fun dayDao(): DayDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "progressvision.db"
            )
                .addCallback(SeedCallback)
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }

    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT INTO activity_category (name, colorHex, orderIndex) VALUES " +
                    "('Работа', '#1976D2', 0)," +
                    "('Учёба', '#7B1FA2', 1)," +
                    "('Спорт', '#388E3C', 2)," +
                    "('Отдых', '#F57C00', 3)," +
                    "('Сон', '#455A64', 4)," +
                    "('Хобби', '#C2185B', 5)," +
                    "('Прочее', '#607D8B', 6)"
            )
        }
    }
}
