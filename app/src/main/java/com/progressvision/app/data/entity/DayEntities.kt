package com.progressvision.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "activity_category")
data class ActivityCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#6750A4",
    val orderIndex: Int = 0
)

@Entity(
    tableName = "activity_log",
    foreignKeys = [
        ForeignKey(
            entity = ActivityCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("startedAt")]
)
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long?,
    val title: String,
    val startedAt: Long,
    val endedAt: Long?,
    val note: String? = null,
    val source: String = "manual", // manual | sport | task
    val pausedAt: Long? = null,
    val pauseAccumulatedMs: Long = 0L
) {
    /** Effective active duration in seconds, excluding paused time. */
    fun activeSeconds(nowMs: Long = System.currentTimeMillis()): Long {
        val effectiveEnd = endedAt ?: nowMs
        val pauseExtra = pausedAt?.let { (effectiveEnd - it).coerceAtLeast(0L) } ?: 0L
        val totalPause = pauseAccumulatedMs + pauseExtra
        val ms = (effectiveEnd - startedAt - totalPause).coerceAtLeast(0L)
        return ms / 1000L
    }

    val isRunning: Boolean get() = endedAt == null
    val isPaused: Boolean get() = endedAt == null && pausedAt != null
}

/** A saved range of days the user wants to review (e.g. "Прошлая неделя"). */
@Entity(tableName = "saved_day")
data class SavedDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val fromMs: Long,
    val toMs: Long,
    val note: String? = null,
    val savedAt: Long = System.currentTimeMillis()
)
