package com.raghu.businesscardscanner2.Converters

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromPhoneList(phones: List<String>): String {
        return phones.joinToString(separator = ",")
    }

    @TypeConverter
    fun toPhoneList(data: String): List<String> {
        return if (data.isEmpty()) emptyList() else data.split(",")
    }
}