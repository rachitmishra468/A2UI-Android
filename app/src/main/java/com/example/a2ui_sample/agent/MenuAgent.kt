package com.example.a2ui_sample.agent

import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.IntentResultWrapper
import com.example.a2ui_sample.domain.repository.MenuRepository

/**
 * MenuAgent
 * Specialist for menu-related operations.
 */
class MenuAgent(private val repository: MenuRepository) {

    fun execute(intent: IntentResultWrapper): AgentResponse {
        return when (intent.intent) {
            com.example.a2ui_sample.domain.model.UserIntent.MENU_SEARCH -> {
                val category = intent.entities["category"] as? String
                val diet = intent.entities["diet"] as? String
                val rawMaxPrice = (intent.entities["price_limit"] as? Number)?.toInt()
                val peopleCount = (intent.entities["people_count"] as? Number)?.toInt() ?: 1
                
                val maxPrice = if (rawMaxPrice != null && peopleCount > 1 && rawMaxPrice > 500) {
                    rawMaxPrice / peopleCount
                } else {
                    rawMaxPrice
                }

                val items = repository.searchMenu(category, diet, maxPrice)
                
                val message = when {
                    items.isEmpty() -> "I couldn't find any $category items matching your request."
                    rawMaxPrice != null -> "For $peopleCount people within ₹$rawMaxPrice, I suggest these dishes (approx ₹$maxPrice each):"
                    peopleCount > 1 -> "For a group of $peopleCount, I suggest ordering 3-4 of these $category dishes to share:"
                    else -> "Here are some $category options for you:"
                }

                AgentResponse.MenuResults(items, message)
            }
            com.example.a2ui_sample.domain.model.UserIntent.MENU_RECOMMEND -> {
                val category = intent.entities["category"] as? String
                val rawMaxPrice = (intent.entities["price_limit"] as? Number)?.toInt()
                val peopleCount = (intent.entities["people_count"] as? Number)?.toInt() ?: 1
                
                val perPersonBudget = if (rawMaxPrice != null && peopleCount > 0) rawMaxPrice / peopleCount else null
                
                val items = repository.getMenuItems().filter { item ->
                    val matchesCategory = category == null || item.category.contains(category, ignoreCase = true)
                    val matchesPrice = perPersonBudget == null || item.price.amount <= perPersonBudget
                    item.isBestSeller && matchesCategory && matchesPrice
                }.take(5)

                if (items.isEmpty() && category != null) {
                    return execute(intent.copy(intent = com.example.a2ui_sample.domain.model.UserIntent.MENU_SEARCH))
                }

                AgentResponse.Recommendations(items)
            }
            else -> AgentResponse.Message("I'm not sure how to handle this menu request.")
        }
    }
}
