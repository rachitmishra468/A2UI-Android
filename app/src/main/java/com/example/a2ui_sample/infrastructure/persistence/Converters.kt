package com.example.a2ui_sample.infrastructure.persistence

import androidx.room.TypeConverter
import com.example.a2ui_sample.domain.valueobjects.MenuItemType

class Converters {
    @TypeConverter
    fun stringToStringList(value: String?): List<String> {
        return if (value.isNullOrEmpty()) emptyList() else value.split(",")
    }

    @TypeConverter
    fun stringListToString(list: List<String>?): String {
        return list?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun stringToMenuItemType(value: String?): MenuItemType {
        return try {
            value?.let { enumValueOf<MenuItemType>(it) } ?: MenuItemType.VEG
        } catch (_: Exception) {
            MenuItemType.VEG
        }
    }

    @TypeConverter
    fun menuItemTypeToString(type: MenuItemType?): String {
        return type?.name ?: MenuItemType.VEG.name
    }
}
