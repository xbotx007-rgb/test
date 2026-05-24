package com.progressvision.app.data.repository

import com.progressvision.app.data.db.SportDao
import com.progressvision.app.data.entities.SportEntry
import com.progressvision.app.data.entities.SportTracker

class SportRepository(
    private val dao: SportDao,
    private val dayRepository: DayRepository
) {
    fun trackers() = dao.observeTrackers()

    fun entries(trackerId: Long) = dao.observeEntries(trackerId)

    fun entriesInRange(from: Long, to: Long) = dao.observeEntriesInRange(from, to)

    suspend fun trackerById(id: Long) = dao.trackerById(id)

    suspend fun saveTracker(tracker: SportTracker) = dao.upsertTracker(tracker)

    suspend fun deleteTracker(tracker: SportTracker) = dao.deleteTracker(tracker)

    suspend fun logEntry(tracker: SportTracker, entry: SportEntry): Long {
        val id = dao.insertEntry(entry)
        val durationMs = entry.durationSec?.times(1000L)
            ?: estimateDurationMs(entry)
        if (durationMs > 0) {
            dayRepository.logFromSport(tracker.name, entry.performedAt, durationMs)
        }
        return id
    }

    suspend fun deleteEntry(entry: SportEntry) = dao.deleteEntry(entry)

    private fun estimateDurationMs(entry: SportEntry): Long {
        val sets = entry.sets ?: return 0L
        return sets * 90_000L
    }
}
