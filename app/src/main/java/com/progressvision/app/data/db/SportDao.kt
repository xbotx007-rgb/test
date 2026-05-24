package com.progressvision.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.progressvision.app.data.entity.SportEntry
import com.progressvision.app.data.entity.SportTracker
import kotlinx.coroutines.flow.Flow

@Dao
interface SportDao {
    @Query("SELECT * FROM sport_tracker ORDER BY createdAt DESC")
    fun observeTrackers(): Flow<List<SportTracker>>

    @Query("SELECT * FROM sport_tracker WHERE id = :id")
    suspend fun getTracker(id: Long): SportTracker?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracker(tracker: SportTracker): Long

    @Update
    suspend fun updateTracker(tracker: SportTracker)

    @Delete
    suspend fun deleteTracker(tracker: SportTracker)

    @Query("SELECT * FROM sport_entry WHERE trackerId = :trackerId ORDER BY date DESC")
    fun observeEntries(trackerId: Long): Flow<List<SportEntry>>

    @Query("SELECT * FROM sport_entry WHERE trackerId = :trackerId AND date >= :since ORDER BY date ASC")
    fun observeEntriesSince(trackerId: Long, since: Long): Flow<List<SportEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: SportEntry): Long

    @Delete
    suspend fun deleteEntry(entry: SportEntry)
}
