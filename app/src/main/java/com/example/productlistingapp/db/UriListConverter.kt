package com.example.productlistingapp.db

import androidx.room.TypeConverter

class UriListConverter {
    @TypeConverter
    fun fromString(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun toString(list: List<String>?): String? {
        return list?.joinToString(",")
    }
}
