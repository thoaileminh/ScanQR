package com.noone.scanqr.data.databases

/**
 * Copyright (c) 2023 NoOne. All rights reserved.
 * Created by NoOne. on 15/05/2023.
 */

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object Converters {
    @TypeConverter
    fun fromString(value: String?): ArrayList<String> {
        if (value.isNullOrBlank() || value == "[]") {
            return arrayListOf()
        }

        var formatterValue = value
        if (value.contains("[")) {
            formatterValue = value.replace("[", "")
        }

        if (formatterValue.contains("]")) {
            formatterValue = formatterValue.replace("]", "")
        }
        val valueArrayList = arrayListOf<String>()
        val valueString = formatterValue.split(",")
        for (i in valueString.indices) {
            valueArrayList.add(valueString[i].trim())
        }
        return valueArrayList
    }

    @TypeConverter
    fun fromArrayList(list: ArrayList<String?>?): String {
        list?.let {
            return it.toString()
        }
        return ""
    }

    @TypeConverter
    fun fromStringToHashMap(value: String?): HashMap<String, Any>? {
        return if (value.isNullOrBlank()) {
            null
        } else {
            val type = object : TypeToken<HashMap<String, Any>>() {}.type
            Gson().fromJson(value, type)
        }
    }

    @TypeConverter
    fun fromHashMapToString(value: HashMap<String, Any>?): String? {
        return Gson().toJson(value)
    }
}