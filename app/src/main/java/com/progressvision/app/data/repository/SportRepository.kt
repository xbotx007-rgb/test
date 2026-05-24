package com.progressvision.app.data.repository

import com.progressvision.app.data.db.DayDao
import com.progressvision.app.data.db.SportDao
import com.progressvision.app.data.entity.ActivityCategory
import com.progressvision.app.data.entity.ActivityLog
import com.progressvision.app.data.entity.SportEntry
import com.progressvision.app.data.entity.SportExercise
import com.progressvision.app.data.entity.SportWorkout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SportRepository(
    private val sportDao: SportDao,
    private val dayDao: DayDao
) {
    fun workouts(): Flow<List<SportWorkout>> = sportDao.observeWorkouts()
    suspend fun getWorkout(id: Long) = sportDao.getWorkout(id)
    suspend fun upsertWorkout(workout: SportWorkout): Long = sportDao.insertWorkout(workout)
    suspend fun deleteWorkout(workout: SportWorkout) = sportDao.deleteWorkout(workout)

    fun exercises(workoutId: Long): Flow<List<SportExercise>> = sportDao.observeExercises(workoutId)
    suspend fun getExercise(id: Long) = sportDao.getExercise(id)
    suspend fun upsertExercise(exercise: SportExercise): Long = sportDao.insertExercise(exercise)
    suspend fun deleteExercise(exercise: SportExercise) = sportDao.deleteExercise(exercise)

    fun entries(exerciseId: Long): Flow<List<SportEntry>> = sportDao.observeEntries(exerciseId)
    fun entriesSince(exerciseId: Long, since: Long): Flow<List<SportEntry>> =
        sportDao.observeEntriesSince(exerciseId, since)

    suspend fun addEntry(entry: SportEntry, label: String): Long {
        val id = sportDao.insertEntry(entry)
        val durationSec = entry.durationSec ?: estimateDuration(entry)
        if (durationSec > 0) {
            val started = entry.date - durationSec * 1000L
            val sportCat = ensureCategory("Спорт")
            dayDao.insertLog(
                ActivityLog(
                    categoryId = sportCat,
                    title = label,
                    startedAt = started,
                    endedAt = entry.date,
                    note = entry.note,
                    source = "sport"
                )
            )
        }
        return id
    }

    suspend fun deleteEntry(entry: SportEntry) = sportDao.deleteEntry(entry)

    private fun estimateDuration(entry: SportEntry): Int {
        val s = entry.sets ?: 0
        if (s > 0) return s * 60
        return 0
    }

    private suspend fun ensureCategory(name: String): Long {
        val current = dayDao.observeCategories().first()
        current.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it.id }
        return dayDao.insertCategory(ActivityCategory(name = name))
    }
}
