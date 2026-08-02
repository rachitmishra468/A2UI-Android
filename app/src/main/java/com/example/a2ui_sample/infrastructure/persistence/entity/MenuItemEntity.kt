package com.example.a2ui_sample.infrastructure.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.a2ui_sample.domain.valueobjects.MenuItemType

@Entity(tableName = "menu_items")
data class MenuItemEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val price: Int, // Stored as Int (cents/paisa)
    val category: String,
    val type: MenuItemType,
    val imageUrl: String,
    val rating: Double,
    val reviewCount: Int,
    val isAvailable: Boolean
)
