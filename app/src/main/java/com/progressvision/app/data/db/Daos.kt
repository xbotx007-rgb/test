package com.progressvision.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.progressvision.app.data.entities.BigTask
import com.progressvision.app.data.entities.DayActivity
import com.progressvision.app.data.entities.DayCategory
import com.progressvision.app.data.entities.SportEntry
import com.progressvision.app.data.entities.SportTracker
import com.progressvision.app.data.entities.SubTask
import kotlinx.coroutines.flow.Flow

@Dao
interface SportDao {
    @Query("SELECT * FROM sport_trackers ORDER BY createdAt DESC")
    fun observeTrackers(): Flow<List<SportTracker>>

    @Query("SELECT * FROM sport_trackers WHERE id = :id")
    suspend fun trackerById(id: Long): SportTracker?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracker(tracker: SportTracker): Long

    @Delete suspend fun deleteTracker(tracker: SportTracker)

    @Query("SELECT * FROM sport_entries WHERE trackerId = :trackerId ORDER BY performedAt ASC")
    fun observeEntries(trackerId: Long): Flow<List<SportEntry>>

    @Query("SELECT * FROM sport_entries WHERE performedAt BETWEEN :from AND :to ORDER BY performedAt ASC")
    fun observeEntriesInRange(from: Long, to: Long): Flow<List<SportEntry>>

    @Insert suspend fun insertEntry(entry: SportEntry): Long

    @Delete suspend fun deleteEntry(entry: SportEntry)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM big_tasks ORDER BY completedAt IS NOT NULL, createdAt DESC")
    fun observeBigTasks(): Flow<List<BigTask>>

    @Query("SELECT * FROM big_tasks WHERE id = :id")
    fun observeBigTask(id: Long): Flow<BigTask?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBigTask(task: BigTask): Long

    @Update suspend fun updateBigTask(task: BigTask)

    @Delete suspend fun deleteBigTask(task: BigTask)

    @Query("SELECT * FROM sub_tasks WHERE bigTaskId = :bigTaskId ORDER BY orderIndex, id")
    fun observeSubTasks(bigTaskId: Long): Flow<List<SubTask>>

    @Query("SELECT * FROM sub_tasks WHERE id = :id")
    suspend fun subTaskById(id: Long): SubTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubTask(sub: SubTask): Long

    @Update suspend fun updateSubTask(sub: SubTask)

    @Delete suspend fun deleteSubTask(sub: SubTask)
}

@Dao
interface DayDao {
    @Query("SELECT * FROM day_categories ORDER BY createdAt ASC")
    fun observeCategories(): Flow<List<DayCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: DayCategory): Long

    @Delete suspend fun deleteCategory(category: DayCategory)

    @Query("SELECT * FROM day_activities WHERE startedAt BETWEEN :from AND :to ORDER BY startedAt DESC")
    fun observeActivities(from: Long, to: Long): Flow<List<DayActivity>>

    @Query("SELECT * FROM day_activities ORDER BY startedAt DESC LIMIT 1")
    suspend fun lastActivity(): DayActivity?

    @Insert suspend fun insertActivity(activity: DayActivity): Long

    @Update suspend fun updateActivity(activity: DayActivity)

    @Delete suspend fun deleteActivity(activity: DayActivity)
}
