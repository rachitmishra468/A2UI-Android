package com.example.a2ui_sample.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * MenuRepository
 * Manages local menu data and cart state.
 */
class MenuRepository(private val context: Context) {
    private val gson = Gson()
    private val cart = mutableListOf<CartItem>()

    fun getMenuItems(): List<MenuItem> {
        return try {
            val jsonString = context.assets.open("menu.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<MenuItem>>() {}.type
            gson.fromJson(jsonString, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun searchMenu(category: String?, type: String?, maxPrice: Int?): List<MenuItem> {
        return getMenuItems().filter { item ->
            val matchesCategory = category == null || item.category.contains(category, ignoreCase = true) || item.name.contains(category, ignoreCase = true)
            val matchesType = type == null || item.type.equals(type, ignoreCase = true)
            val matchesPrice = maxPrice == null || item.price <= maxPrice
            matchesCategory && matchesType && matchesPrice
        }
    }

    fun addToCart(itemId: Int): CartItem? {
        val item = getMenuItems().find { it.id == itemId } ?: return null
        val existing = cart.find { it.menuItem.id == itemId }
        return if (existing != null) {
            existing.quantity++
            existing
        } else {
            val newCartItem = CartItem(item, 1)
            cart.add(newCartItem)
            newCartItem
        }
    }

    fun getCart(): List<CartItem> = cart

    fun getCartTotal(): Int = cart.sumOf { it.menuItem.price * it.quantity }
}