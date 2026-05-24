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
    val source: String = "manual" // manual | sport | task
)
