package com.progressvision.app.data.repository

import com.progressvision.app.data.db.DayDao
import com.progressvision.app.data.entity.ActivityCategory
import com.progressvision.app.data.entity.ActivityLog
import kotlinx.coroutines.flow.Flow

class DayRepository(private val dao: DayDao) {
    fun categories(): Flow<List<ActivityCategory>> = dao.observeCategories()
    fun running(): Flow<ActivityLog?> = dao.observeRunning()
    fun logsBetween(from: Long, to: Long): Flow<List<ActivityLog>> = dao.observeLogsBetween(from, to)

    suspend fun upsertCategory(category: ActivityCategory): Long = dao.insertCategory(category)
    suspend fun deleteCategory(category: ActivityCategory) = dao.deleteCategory(category)

    suspend fun startActivity(category: ActivityCategory, title: String = category.name): Long {
        stopRunning()
        return dao.insertLog(
            ActivityLog(
                categoryId = category.id,
                title = title,
                startedAt = System.currentTimeMillis(),
                endedAt = null
            )
        )
    }

    suspend fun stopRunning(): Boolean {
        val current = dao.getRunning() ?: return false
        dao.updateLog(current.copy(endedAt = System.currentTimeMillis()))
        return true
    }

    suspend fun stopLog(log: ActivityLog) {
        dao.updateLog(log.copy(endedAt = System.currentTimeMillis()))
    }

    suspend fun addManual(log: ActivityLog): Long = dao.insertLog(log)
    suspend fun updateLog(log: ActivityLog) = dao.updateLog(log)
    suspend fun deleteLog(log: ActivityLog) = dao.deleteLog(log)
}
