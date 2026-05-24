package com.progressvision.app.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SportKind { STRENGTH, BODYWEIGHT, CARDIO, TIME }

@Entity(tableName = "sport_trackers")
data class SportTracker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: SportKind,
    val unit: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sport_entries",
    foreignKeys = [
        ForeignKey(
            entity = SportTracker::class,
            parentColumns = ["id"],
            childColumns = ["trackerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("trackerId"), Index("performedAt")]
)
data class SportEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackerId: Long,
    val performedAt: Long = System.currentTimeMillis(),
    val sets: Int? = null,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val distanceKm: Double? = null,
    val durationSec: Long? = null,
    val notes: String? = null
) {
    val primaryMetric: Double
        get() = when {
            weightKg != null && reps != null && sets != null -> weightKg * reps * sets
            reps != null && sets != null -> (reps * sets).toDouble()
            reps != null -> reps.toDouble()
            distanceKm != null -> distanceKm
            durationSec != null -> durationSec.toDouble()
            else -> 0.0
        }
}
