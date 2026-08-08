package com.example.a2ui_sample.domain.model

import com.example.a2ui_sample.domain.valueobjects.Price
import com.example.a2ui_sample.domain.valueobjects.MenuItemType

/**
 * MenuItem Entity
 * Represents a dish or item on the restaurant menu.
 */
data class MenuItem(
    val id: Int,
    val name: String,
    val description: String,
    val price: Price,
    val category: String,
    val type: MenuItemType,
    val image: String,
    val rating: Double = 4.5,
    val reviewCount: Int = 0,
    val calories: Int = 0,
    val tags: List<String> = emptyList(),
    val isBestSeller: Boolean = false,
    val isAvailable: Boolean = true,
    val cartQuantity: Int = 0
) {
    val imageUrl: String get() = image
}
