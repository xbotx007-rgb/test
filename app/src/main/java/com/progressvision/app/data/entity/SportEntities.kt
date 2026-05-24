package com.progressvision.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SportType { STRENGTH, BODYWEIGHT, CARDIO }

@Entity(tableName = "sport_tracker")
data class SportTracker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: SportType,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sport_entry",
    foreignKeys = [
        ForeignKey(
            entity = SportTracker::class,
            parentColumns = ["id"],
            childColumns = ["trackerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("trackerId"), Index("date")]
)
data class SportEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackerId: Long,
    val date: Long,
    val sets: Int? = null,
    val reps: Int? = null,
    val weightKg: Float? = null,
    val distanceKm: Float? = null,
    val durationSec: Int? = null,
    val note: String? = null
) {
    fun primaryMetric(type: SportType): Float = when (type) {
        SportType.STRENGTH -> {
            val s = sets ?: 0
            val r = reps ?: 0
            val w = weightKg ?: 0f
            s * r * w
        }
        SportType.BODYWEIGHT -> {
            val s = sets ?: 0
            val r = reps ?: 0
            (s * r).toFloat()
        }
        SportType.CARDIO -> distanceKm ?: ((durationSec ?: 0) / 60f)
    }
}
