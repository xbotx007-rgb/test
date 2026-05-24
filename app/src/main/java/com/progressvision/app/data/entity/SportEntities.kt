package com.progressvision.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SportType { STRENGTH, BODYWEIGHT, CARDIO }

enum class SportWorkoutType { REGULAR, MEASUREMENT }

@Entity(tableName = "sport_workout")
data class SportWorkout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: SportWorkoutType = SportWorkoutType.REGULAR,
    val proMode: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sport_exercise",
    foreignKeys = [
        ForeignKey(
            entity = SportWorkout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutId")]
)
data class SportExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val name: String,
    val type: SportType,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sport_entry",
    foreignKeys = [
        ForeignKey(
            entity = SportExercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId"), Index("date")]
)
data class SportEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val date: Long,
    val sets: Int? = null,
    val reps: Int? = null,
    val weightKg: Float? = null,
    val distanceKm: Float? = null,
    val durationSec: Int? = null,
    val note: String? = null,
    // Pro mode timing/pulse data (optional)
    val setDurationSec: Int? = null,
    val restDurationSec: Int? = null,
    val pulseBefore: Int? = null,
    val pulseAfter: Int? = null
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

    fun tonnage(type: SportType): Float = when (type) {
        SportType.STRENGTH -> (sets ?: 0) * (reps ?: 0) * (weightKg ?: 0f)
        else -> 0f
    }
}
