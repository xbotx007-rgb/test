package com.progressvision.app.data.repository

import com.progressvision.app.data.db.DayDao
import com.progressvision.app.data.db.TaskDao
import com.progressvision.app.data.entity.ActivityCategory
import com.progressvision.app.data.entity.ActivityLog
import com.progressvision.app.data.entity.BigTask
import com.progressvision.app.data.entity.SubTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TaskRepository(
    private val taskDao: TaskDao,
    private val dayDao: DayDao
) {
    fun bigTasks(): Flow<List<BigTask>> = taskDao.observeBigTasks()
    fun bigTask(id: Long): Flow<BigTask?> = taskDao.observeBigTask(id)
    fun subTasks(bigTaskId: Long): Flow<List<SubTask>> = taskDao.observeSubTasks(bigTaskId)

    suspend fun upsertBigTask(task: BigTask): Long = taskDao.insertBigTask(task)
    suspend fun updateBigTask(task: BigTask) = taskDao.updateBigTask(task)
    suspend fun deleteBigTask(task: BigTask) = taskDao.deleteBigTask(task)

    suspend fun upsertSubTask(sub: SubTask): Long = taskDao.insertSubTask(sub)
    suspend fun updateSubTask(sub: SubTask) = taskDao.updateSubTask(sub)
    suspend fun deleteSubTask(sub: SubTask) = taskDao.deleteSubTask(sub)
    suspend fun toggleSubTask(sub: SubTask) =
        taskDao.updateSubTask(sub.copy(isDone = !sub.isDone))

    /**
     * Record a timer session for a subtask. Adds [durationSec] to its time spent and
     * mirrors the session as an ActivityLog under the "Проекты" category so day stats include it.
     */
    suspend fun recordSubTaskTime(
        sub: SubTask,
        bigTaskTitle: String,
        startedAt: Long,
        endedAt: Long
    ) {
        val durationSec = ((endedAt - startedAt) / 1000L).coerceAtLeast(0)
        if (durationSec == 0L) return
        taskDao.addTimeToSubTask(sub.id, durationSec)
        val cat = ensureCategory("Проекты")
        dayDao.insertLog(
            ActivityLog(
                categoryId = cat,
                title = "$bigTaskTitle — ${sub.title}",
                startedAt = startedAt,
                endedAt = endedAt,
                source = "task"
            )
        )
    }

    private suspend fun ensureCategory(name: String): Long {
        val current = dayDao.observeCategories().first()
        current.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it.id }
        return dayDao.insertCategory(ActivityCategory(name = name))
    }
}

fun List<SubTask>.progressPercent(): Int {
    if (isEmpty()) return 0
    val total = sumOf { it.weight.coerceAtLeast(1) }
    val done = filter { it.isDone }.sumOf { it.weight.coerceAtLeast(1) }
    if (total == 0) return 0
    return ((done.toFloat() / total.toFloat()) * 100f).toInt().coerceIn(0, 100)
}

fun List<SubTask>.totalTimeSec(): Long = sumOf { it.timeSpentSec }
