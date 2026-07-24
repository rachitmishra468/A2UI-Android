package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.data.AgentResponse
import com.example.a2ui_sample.data.MenuRepository
import com.google.adk.kt.annotations.Tool

/**
 * RestaurantTools
 * Set of tools available to the ADK Agent for interacting with the menu and cart.
 */
class RestaurantTools(private val repository: MenuRepository) {

    private var lastResponse: AgentResponse? = null

    fun getLastResponse(): AgentResponse? = lastResponse

    fun reset() {
        lastResponse = null
    }


//    fun searchMenu(
//        category: String? = null,
//        type: String? = null,
//        maxPrice: Int? = null
//    ): String {
//        Log.d("A2UI_FLOW", "   >> Tool Executing: search_menu(category=$category, type=$type, maxPrice=$maxPrice)")
//        val results = repository.searchMenu(category, type, maxPrice)
//        lastResponse = AgentResponse.MenuResults(results, "Search: ${category ?: ""} ${type ?: ""} ${maxPrice ?: ""}")
//        return "Found ${results.size} items."
//    }


    @Tool(
        name = "search_menu",
        description =
            "Search food by category, food type, vegetarian preference, or price."
    )
    fun searchMenu(
        /** The category of food (e.g., 'burger', 'pizza', 'meal', 'dosa'). */
        category: String? = null,
        /** The dietary type (e.g., 'Veg', 'Non-Veg'). */
        type: String? = null,
        /** The maximum price the user is willing to pay. */
        maxPrice: Int? = null
    ): String {

        Log.d("A2UI_FLOW", "searchMenu entered")
        val results = repository.searchMenu(
            category,
            type,
            maxPrice
        )

        lastResponse =
            AgentResponse.MenuResults(
                results,
                "Search"
            )

        if (results.isEmpty()) {
            return "No menu items found."
        }

        return results.joinToString("\n") {
            "ID=${it.id}, Name=${it.name}, Price=${it.price}"
        }
    }


    @Tool(
        name = "get_recommendations",
        description =
            "Recommend food items based on user preferences."
    )
    fun getRecommendations(
        /** The criteria for recommendations (e.g., 'spicy', 'popular', 'veg'). Defaults to 'spicy'. */
        criteria: String? = "spicy"
    ): String {

        Log.d("A2UI_FLOW", "GET_RECOMMENDATIONS CALLED criteria=$criteria")
        Log.d(
            "A2UI_FLOW",
            ">> Tool Executing: get_recommendations(criteria=$criteria)"
        )

        val all = repository.getMenuItems()

        val items = if (criteria?.lowercase() == "spicy") {
            all.filter {
                it.name.contains("Masala", ignoreCase = true) ||
                        it.name.contains("Paneer", ignoreCase = true)
            }
        } else {
            all.take(3)
        }

        lastResponse = AgentResponse.Recommendations(items)

        if (items.isEmpty()) {
            return "No recommendations found for criteria=$criteria"
        }

        return buildString {
            appendLine("Recommended Items:")

            items.forEach {
                appendLine(
                    "ID=${it.id}, " +
                            "Name=${it.name}, " +
                            "Price=₹${it.price}"
                )
            }
        }
    }




    @Tool(
        name = "add_item_to_cart",
        description =
            "REQUIRED. Exact menu item name to add to cart. Extract this value from the user's request. Examples: Masala Dosa, Veg Burger, Paneer Burger, Veg Meal, Margherita Pizza."
    )
    fun addItemToCart(
        /** The exact name of the food item to add. Example: 'Masala Dosa'. REQUIRED. */
        itemName: String
    ): String {
        // Normalize the incoming itemName to reduce noise (trim, remove quotes, collapse spaces)
        val raw = itemName.trim().trim('"').trim('\'')
        val query = raw.replace(Regex("\\s+"), " ")

        Log.d("A2UI_FLOW", "add_item_to_cart CALLED itemName=$query")

        // Load available menu items
        val items = repository.getMenuItems()

        // 1) Exact match (case-insensitive)
        var item = items.firstOrNull { it.name.equals(query, ignoreCase = true) }

        // 2) Contains match (user typed a substring or slight reorder)
        if (item == null) {
            val containsMatches = items.filter {
                it.name.contains(itemName, ignoreCase = true) || itemName.contains(it.name, ignoreCase = true)
            }
            if (containsMatches.size == 1) {
                item = containsMatches.first()
            } else if (containsMatches.size > 1) {
                // Multiple possible matches: return search-like structured response
                lastResponse = AgentResponse.MenuResults(containsMatches, "Multiple matches for '$itemName'")
                return "Multiple matches found for '$itemName'"
            }
        }

        // 3) Fuzzy match (Levenshtein distance fallback for typos)
        if (item == null) {
            val scored = items.map { m -> m to levenshtein(m.name.lowercase(), itemName.lowercase()) }
                .sortedBy { it.second }
            val (best, dist) = scored.firstOrNull() ?: (null to Int.MAX_VALUE)
            // choose a conservative threshold: allow small typos (distance <= 3)
            if (best != null && dist <= 3) {
                item = best
            }
        }

        if (item == null) {
            Log.d("A2UI_FLOW", "add_item_to_cart: item not found for '$itemName'")
            return "Item not found"
        }

        // Add to cart and record structured response
        repository.addToCart(item.id)

        lastResponse = AgentResponse.CartUpdate(item, repository.getCart().sumOf { it.quantity })
        return "${item.name} added to cart"
    }


    // Simple Levenshtein distance implementation for small strings (sufficient for menu item fuzzy matching)
    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }


    @Tool(
        name = "view_cart",
        description =
            "Show all items currently in the shopping cart."
    )
    fun viewCart(): String {

        Log.d("A2UI_FLOW", "VIEW_CART CALLED")
        Log.d("A2UI_FLOW", ">> Tool Executing: view_cart()")

        val cartItems = repository.getCart()
        val total = repository.getCartTotal()

        lastResponse = AgentResponse.CartView(
            cartItems,
            total
        )

        if (cartItems.isEmpty()) {
            return "Cart is empty."
        }

        return buildString {
            appendLine("Cart Items:")

            cartItems.forEach {
                appendLine(
                    "ID=${it.menuItem.id}, " +
                            "Name=${it.menuItem.name}, " +
                            "Qty=${it.quantity}, " +
                            "Price=${it.menuItem.price}"
                )
            }

            appendLine("Total=₹$total")
        }
    }


    @Tool(
        name = "get_full_menu",
        description =
            "Show all available food items and restaurant menu. Use only for menu browsing requests."
    )
    fun getFullMenu(): String {

        Log.d("A2UI_FLOW", "get_full_menu CALLED")
        Log.d("A2UI_FLOW", ">> Tool Executing: get_full_menu()")
        val items = repository.getMenuItems()

        lastResponse = AgentResponse.MenuResults(
            items,
            "Menu"
        )

        if (items.isEmpty()) {
            return "The menu is currently empty."
        }

        return items.joinToString("\n") {
            "ID=${it.id}, Name=${it.name}, Price=${it.price}"
        }
    }
}