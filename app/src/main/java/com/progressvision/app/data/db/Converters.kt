package com.progressvision.app.data.db

import androidx.room.TypeConverter
import com.progressvision.app.data.entity.SportType

class Converters {
    @TypeConverter
    fun fromSportType(value: SportType): String = value.name

    @TypeConverter
    fun toSportType(value: String): SportType = runCatching { SportType.valueOf(value) }
        .getOrDefault(SportType.STRENGTH)
}
