package com.example.a2ui_sample.infrastructure.persistence.dao

import androidx.room.*
import com.example.a2ui_sample.infrastructure.persistence.entity.MenuItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {
    @Query("SELECT * FROM menu_items")
    suspend fun getAllMenuItems(): List<MenuItemEntity>

    @Query("SELECT * FROM menu_items WHERE id = :id")
    suspend fun getMenuItemById(id: Int): MenuItemEntity?

    @Query("SELECT * FROM menu_items WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    suspend fun searchMenu(query: String): List<MenuItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItems(items: List<MenuItemEntity>)
}
