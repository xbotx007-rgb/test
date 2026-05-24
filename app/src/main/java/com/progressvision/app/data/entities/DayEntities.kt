package com.progressvision.app.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "day_categories")
data class DayCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#4F6DF5",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "day_activities",
    foreignKeys = [
        ForeignKey(
            entity = DayCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("startedAt")]
)
data class DayActivity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long?,
    val title: String,
    val startedAt: Long,
    val endedAt: Long,
    val note: String? = null,
    val source: String = "manual"
) {
    val durationMs: Long get() = (endedAt - startedAt).coerceAtLeast(0L)
}
