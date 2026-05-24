package com.progressvision.app.data.repository

import com.progressvision.app.data.db.TaskDao
import com.progressvision.app.data.entities.BigTask
import com.progressvision.app.data.entities.SubTask

class TaskRepository(
    private val dao: TaskDao,
    private val dayRepository: DayRepository
) {
    fun bigTasks() = dao.observeBigTasks()
    fun bigTask(id: Long) = dao.observeBigTask(id)
    fun subTasks(bigTaskId: Long) = dao.observeSubTasks(bigTaskId)

    suspend fun saveBigTask(task: BigTask) = dao.upsertBigTask(task)
    suspend fun updateBigTask(task: BigTask) = dao.updateBigTask(task)
    suspend fun deleteBigTask(task: BigTask) = dao.deleteBigTask(task)

    suspend fun addSubTask(sub: SubTask) = dao.upsertSubTask(sub)
    suspend fun updateSubTask(sub: SubTask) = dao.updateSubTask(sub)
    suspend fun deleteSubTask(sub: SubTask) = dao.deleteSubTask(sub)

    suspend fun toggleSubTask(sub: SubTask) {
        dao.updateSubTask(sub.copy(isDone = !sub.isDone))
    }

    suspend fun startTimer(sub: SubTask) {
        if (sub.timerStartedAt != null) return
        dao.updateSubTask(sub.copy(timerStartedAt = System.currentTimeMillis()))
    }

    suspend fun stopTimer(sub: SubTask, parentTitle: String) {
        val startedAt = sub.timerStartedAt ?: return
        val now = System.currentTimeMillis()
        val elapsed = (now - startedAt).coerceAtLeast(0L)
        dao.updateSubTask(
            sub.copy(
                timerStartedAt = null,
                totalSpentMs = sub.totalSpentMs + elapsed
            )
        )
        if (elapsed > 0) {
            dayRepository.logFromTask(
                title = "$parentTitle: ${sub.title}",
                startedAt = startedAt,
                endedAt = now
            )
        }
    }
}
