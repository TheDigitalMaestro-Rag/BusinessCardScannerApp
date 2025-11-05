package com.project.businesscardscannerapp.Converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.project.businesscardscannerapp.RoomDB.Entity.PipelineStage

class Converters {

    private val gson = Gson()
    @TypeConverter
    fun fromPhoneList(phones: List<String>): String {
        return phones.joinToString(separator = ",")
    }

    @TypeConverter
    fun toPhoneList(data: String): List<String> {
        return if (data.isEmpty()) emptyList() else data.split(",")
    }

    @TypeConverter
    fun fromPipelineStage(stage: PipelineStage): String {
        return stage.name
    }

    @TypeConverter
    fun toPipelineStage(stage: String): PipelineStage {
        return try {
            PipelineStage.valueOf(stage)
        } catch (e: IllegalArgumentException) {
            PipelineStage.NEW // Default fallback
        }
    }
}