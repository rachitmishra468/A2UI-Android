package com.example.a2ui_sample.ai_assistant.tools

import android.util.Log
import com.example.a2ui_sample.domain.model.MenuItem
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.google.adk.kt.annotations.Tool
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AssistantMenuTools
 * Stateless implementation of menu tools.
 * Returns structured maps for UI mapping.
 */
@Singleton
class AssistantMenuTools @Inject constructor(
    private val repository: MenuRepository
) {
    @Tool(
        name = "get_full_menu",
        description = "Show all available food items and restaurant menu. Use only for general menu browsing requests."
    )
    fun getFullMenu(): Map<String, Any?> {
        Log.d("AssistantFlow", "TOOL: getFullMenu() called")
        val items = repository.getMenuItems()
        return mapOf(
            "result" to items,
            "message" to if (items.isEmpty()) "The menu is currently empty." else "Here is our complete menu."
        )
    }

    @Tool(
        name = "assistant_search_menu",
        description = "Search the restaurant menu by category and dietary preference."
    )
    fun searchMenu(category: String?, diet: String?, maxPrice: Int?): Map<String, Any?> {
        // Pass maxPrice to repository search if provided. repository may filter by price.
        val items = repository.searchMenu(category = category, type = diet, maxPrice = maxPrice)
        val priceInfo = maxPrice?.let { " under ₹$it" } ?: ""
        return mapOf(
            "result" to items,
            "message" to "Found ${items.size} items for ${category ?: "all categories"} (${diet ?: "any diet"})$priceInfo."
        )
    }

    @Tool(
        name = "get_menu_details",
        description = "Get detailed information about a specific menu item by its name."
    )
    fun getMenuDetails(itemName: String): Map<String, Any?> {
        val item = repository.getMenuItems().find { 
            it.name.contains(itemName, ignoreCase = true) 
        }
        return mapOf(
            "result" to item,
            "message" to (item?.let { "Found details for ${it.name}" } ?: "Item not found: $itemName")
        )
    }

    @Tool(
        name = "get_recommendations",
        description = "Get popular menu recommendations and bestsellers."
    )
    fun getRecommendations(): Map<String, Any?> {
        val items = repository.getMenuItems().filter { it.isBestSeller }.take(5)
        return mapOf(
            "result" to items,
            "message" to "Here are our bestsellers."
        )
    }

    @Tool(
        name = "get_today_specials",
        description = "Get today's special items from the menu."
    )
    fun getTodaySpecials(): Map<String, Any?> {
        val items = repository.getMenuItems().filter { it.tags.contains("New") || it.isBestSeller }.take(3)
        return mapOf(
            "result" to items,
            "message" to "Check out today's specials!"
        )
    }

   /* @Tool(
        name = "add_to_cart",
        description = "Add a menu item to the shopping cart with a specific quantity."
    )
    suspend fun addToCart(itemName: String, quantity: Int): Map<String, Any?> {
        val item = repository.getMenuItems().find {
            it.name.contains(itemName, ignoreCase = true)
        }
        return if (item != null) {
            repository.addToCart(item.id)
            if (quantity > 1) {
                repository.updateCartQuantity(item.id, quantity)
            }
            mapOf(
                "item" to item,
                "quantity" to quantity,
                "message" to "Added $quantity x ${item.name} to cart."
            )
        } else {
            mapOf("message" to "Error: Item not found: $itemName")
        }
    }*/
}
