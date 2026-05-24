package com.progressvision.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.progressvision.app.data.entity.ActivityCategory
import com.progressvision.app.data.entity.ActivityLog
import com.progressvision.app.data.entity.SavedDay
import kotlinx.coroutines.flow.Flow

@Dao
interface DayDao {
    @Query("SELECT * FROM activity_category ORDER BY orderIndex ASC, id ASC")
    fun observeCategories(): Flow<List<ActivityCategory>>

    @Query("SELECT * FROM activity_category WHERE id = :id")
    suspend fun getCategory(id: Long): ActivityCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ActivityCategory): Long

    @Update
    suspend fun updateCategory(category: ActivityCategory)

    @Delete
    suspend fun deleteCategory(category: ActivityCategory)

    @Query("SELECT * FROM activity_log WHERE startedAt >= :from AND startedAt < :to ORDER BY startedAt DESC")
    fun observeLogsBetween(from: Long, to: Long): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_log WHERE startedAt >= :from AND startedAt < :to ORDER BY startedAt DESC")
    suspend fun getLogsBetween(from: Long, to: Long): List<ActivityLog>

    @Query("SELECT * FROM activity_log WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeRunning(): Flow<ActivityLog?>

    @Query("SELECT * FROM activity_log WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getRunning(): ActivityLog?

    @Query("SELECT * FROM activity_log WHERE id = :id")
    suspend fun getLog(id: Long): ActivityLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog): Long

    @Update
    suspend fun updateLog(log: ActivityLog)

    @Delete
    suspend fun deleteLog(log: ActivityLog)

    @Query("SELECT * FROM saved_day ORDER BY fromMs DESC")
    fun observeSavedDays(): Flow<List<SavedDay>>

    @Query("SELECT * FROM saved_day WHERE id = :id")
    suspend fun getSavedDay(id: Long): SavedDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedDay(day: SavedDay): Long

    @Update
    suspend fun updateSavedDay(day: SavedDay)

    @Delete
    suspend fun deleteSavedDay(day: SavedDay)
}
