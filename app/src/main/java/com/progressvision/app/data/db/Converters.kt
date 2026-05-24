package com.progressvision.app.data.db

import androidx.room.TypeConverter
import com.progressvision.app.data.entity.SportType
import com.progressvision.app.data.entity.SportWorkoutType

class Converters {
    @TypeConverter
    fun fromSportType(value: SportType): String = value.name

    @TypeConverter
    fun toSportType(value: String): SportType = runCatching { SportType.valueOf(value) }
        .getOrDefault(SportType.STRENGTH)

    @TypeConverter
    fun fromSportWorkoutType(value: SportWorkoutType): String = value.name

    @TypeConverter
    fun toSportWorkoutType(value: String): SportWorkoutType =
        runCatching { SportWorkoutType.valueOf(value) }.getOrDefault(SportWorkoutType.REGULAR)
}
