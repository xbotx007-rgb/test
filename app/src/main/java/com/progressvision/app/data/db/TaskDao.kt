package com.progressvision.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.progressvision.app.data.entity.BigTask
import com.progressvision.app.data.entity.SubTask
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM big_task ORDER BY (completedAt IS NOT NULL), createdAt DESC")
    fun observeBigTasks(): Flow<List<BigTask>>

    @Query("SELECT * FROM big_task WHERE id = :id")
    suspend fun getBigTask(id: Long): BigTask?

    @Query("SELECT * FROM big_task WHERE id = :id")
    fun observeBigTask(id: Long): Flow<BigTask?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBigTask(task: BigTask): Long

    @Update
    suspend fun updateBigTask(task: BigTask)

    @Delete
    suspend fun deleteBigTask(task: BigTask)

    @Query("SELECT * FROM sub_task WHERE bigTaskId = :bigTaskId ORDER BY orderIndex ASC, id ASC")
    fun observeSubTasks(bigTaskId: Long): Flow<List<SubTask>>

    @Query("SELECT * FROM sub_task WHERE id = :id")
    suspend fun getSubTask(id: Long): SubTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(sub: SubTask): Long

    @Update
    suspend fun updateSubTask(sub: SubTask)

    @Delete
    suspend fun deleteSubTask(sub: SubTask)

    @Query("UPDATE sub_task SET timeSpentSec = timeSpentSec + :addSec WHERE id = :id")
    suspend fun addTimeToSubTask(id: Long, addSec: Long)
}
