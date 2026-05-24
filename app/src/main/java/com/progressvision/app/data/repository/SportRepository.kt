package com.progressvision.app.data.repository

import com.progressvision.app.data.db.DayDao
import com.progressvision.app.data.db.SportDao
import com.progressvision.app.data.entity.ActivityCategory
import com.progressvision.app.data.entity.ActivityLog
import com.progressvision.app.data.entity.SportEntry
import com.progressvision.app.data.entity.SportTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SportRepository(
    private val sportDao: SportDao,
    private val dayDao: DayDao
) {
    fun trackers(): Flow<List<SportTracker>> = sportDao.observeTrackers()
    fun entries(trackerId: Long): Flow<List<SportEntry>> = sportDao.observeEntries(trackerId)
    fun entriesSince(trackerId: Long, since: Long): Flow<List<SportEntry>> =
        sportDao.observeEntriesSince(trackerId, since)

    suspend fun getTracker(id: Long) = sportDao.getTracker(id)
    suspend fun upsertTracker(tracker: SportTracker): Long = sportDao.insertTracker(tracker)
    suspend fun deleteTracker(tracker: SportTracker) = sportDao.deleteTracker(tracker)

    suspend fun addEntry(entry: SportEntry, trackerName: String): Long {
        val id = sportDao.insertEntry(entry)
        val durationSec = entry.durationSec ?: estimateDuration(entry)
        if (durationSec > 0) {
            val started = entry.date - durationSec * 1000L
            val sportCat = ensureCategory("Спорт")
            dayDao.insertLog(
                ActivityLog(
                    categoryId = sportCat,
                    title = trackerName,
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
