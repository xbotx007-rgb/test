package com.progressvision.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "big_task")
data class BigTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val deadlineFrom: Long? = null,
    val deadlineTo: Long? = null
)

@Entity(
    tableName = "sub_task",
    foreignKeys = [
        ForeignKey(
            entity = BigTask::class,
            parentColumns = ["id"],
            childColumns = ["bigTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bigTaskId")]
)
data class SubTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bigTaskId: Long,
    val title: String,
    val weight: Int = 1,
    val isDone: Boolean = false,
    val timeSpentSec: Long = 0L,
    val orderIndex: Int = 0
)
