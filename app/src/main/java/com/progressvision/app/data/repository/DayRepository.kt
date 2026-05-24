package com.progressvision.app.data.repository

import com.progressvision.app.data.db.DayDao
import com.progressvision.app.data.entities.DayActivity
import com.progressvision.app.data.entities.DayCategory
import kotlinx.coroutines.flow.Flow

class DayRepository(private val dao: DayDao) {
    fun categories(): Flow<List<DayCategory>> = dao.observeCategories()
    fun activities(from: Long, to: Long): Flow<List<DayActivity>> = dao.observeActivities(from, to)

    suspend fun saveCategory(category: DayCategory) = dao.upsertCategory(category)
    suspend fun deleteCategory(category: DayCategory) = dao.deleteCategory(category)

    suspend fun lastActivity(): DayActivity? = dao.lastActivity()
    suspend fun logActivity(activity: DayActivity) = dao.insertActivity(activity)
    suspend fun deleteActivity(activity: DayActivity) = dao.deleteActivity(activity)
    suspend fun updateActivity(activity: DayActivity) = dao.updateActivity(activity)

    suspend fun logFromSport(title: String, performedAt: Long, durationMs: Long) {
        dao.insertActivity(
            DayActivity(
                categoryId = null,
                title = title,
                startedAt = performedAt - durationMs,
                endedAt = performedAt,
                source = "sport"
            )
        )
    }

    suspend fun logFromTask(title: String, startedAt: Long, endedAt: Long) {
        dao.insertActivity(
            DayActivity(
                categoryId = null,
                title = title,
                startedAt = startedAt,
                endedAt = endedAt,
                source = "task"
            )
        )
    }
}
