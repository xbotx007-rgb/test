package com.progressvision.app.data.repository

import com.progressvision.app.data.db.DayDao
import com.progressvision.app.data.entity.ActivityCategory
import com.progressvision.app.data.entity.ActivityLog
import com.progressvision.app.data.entity.SavedDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class DayRepository(private val dao: DayDao) {
    fun categories(): Flow<List<ActivityCategory>> = dao.observeCategories()
    fun running(): Flow<ActivityLog?> = dao.observeRunning()
    fun logsBetween(from: Long, to: Long): Flow<List<ActivityLog>> = dao.observeLogsBetween(from, to)
    suspend fun fetchLogsBetween(from: Long, to: Long): List<ActivityLog> = dao.getLogsBetween(from, to)

    suspend fun upsertCategory(category: ActivityCategory): Long = dao.insertCategory(category)
    suspend fun deleteCategory(category: ActivityCategory) = dao.deleteCategory(category)

    suspend fun ensureCategory(name: String, colorHex: String = "#607D8B"): Long {
        val current = dao.observeCategories().first()
        current.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it.id }
        return dao.insertCategory(ActivityCategory(name = name, colorHex = colorHex))
    }

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
        val now = System.currentTimeMillis()
        val pauseExtra = current.pausedAt?.let { (now - it).coerceAtLeast(0L) } ?: 0L
        dao.updateLog(
            current.copy(
                endedAt = now,
                pausedAt = null,
                pauseAccumulatedMs = current.pauseAccumulatedMs + pauseExtra
            )
        )
        return true
    }

    suspend fun pauseRunning(): Boolean {
        val current = dao.getRunning() ?: return false
        if (current.pausedAt != null) return false
        dao.updateLog(current.copy(pausedAt = System.currentTimeMillis()))
        return true
    }

    suspend fun resumeRunning(): Boolean {
        val current = dao.getRunning() ?: return false
        val pausedAt = current.pausedAt ?: return false
        val now = System.currentTimeMillis()
        val pauseExtra = (now - pausedAt).coerceAtLeast(0L)
        dao.updateLog(
            current.copy(
                pausedAt = null,
                pauseAccumulatedMs = current.pauseAccumulatedMs + pauseExtra
            )
        )
        return true
    }

    suspend fun stopLog(log: ActivityLog) {
        val now = System.currentTimeMillis()
        val pauseExtra = log.pausedAt?.let { (now - it).coerceAtLeast(0L) } ?: 0L
        dao.updateLog(
            log.copy(
                endedAt = now,
                pausedAt = null,
                pauseAccumulatedMs = log.pauseAccumulatedMs + pauseExtra
            )
        )
    }

    suspend fun addManual(log: ActivityLog): Long = dao.insertLog(log)
    suspend fun updateLog(log: ActivityLog) = dao.updateLog(log)
    suspend fun deleteLog(log: ActivityLog) = dao.deleteLog(log)

    fun savedDays(): Flow<List<SavedDay>> = dao.observeSavedDays()
    suspend fun getSavedDay(id: Long): SavedDay? = dao.getSavedDay(id)
    suspend fun saveDay(day: SavedDay): Long = dao.insertSavedDay(day)
    suspend fun updateSavedDay(day: SavedDay) = dao.updateSavedDay(day)
    suspend fun deleteSavedDay(day: SavedDay) = dao.deleteSavedDay(day)
}
