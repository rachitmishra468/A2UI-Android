package com.example.a2ui_sample.infrastructure.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.a2ui_sample.infrastructure.persistence.dao.MenuDao
import com.example.a2ui_sample.infrastructure.persistence.entity.MenuItemEntity

@Database(
    entities = [MenuItemEntity::class],
    version = 1,
    exportSchema = false
)
//@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun menuDao(): MenuDao
}
