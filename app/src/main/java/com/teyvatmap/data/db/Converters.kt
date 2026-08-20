package com.teyvatmap.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, String>>() {}.type

    @TypeConverter
    fun fromMapString(map: Map<String, String>?): String? {
        return map?.let { gson.toJson(it, mapType) }
    }

    @TypeConverter
    fun toMapString(json: String?): Map<String, String>? {
        return json?.let { gson.fromJson(it, mapType) }
    }
}