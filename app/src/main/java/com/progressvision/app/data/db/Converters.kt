package com.progressvision.app.data.db

import androidx.room.TypeConverter
import com.progressvision.app.data.entities.SportKind

class Converters {
    @TypeConverter fun fromKind(k: SportKind): String = k.name
    @TypeConverter fun toKind(s: String): SportKind = SportKind.valueOf(s)
}
