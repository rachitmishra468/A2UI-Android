package com.example.a2ui_sample.agent

import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.IntentResult
import com.example.a2ui_sample.domain.repository.MenuRepository

/**
 * MenuAgent
 * Execution specialist for menu-related operations.
 * Redesigned to perform direct actions based on Gemini-extracted intents.
 */
class MenuAgent(private val repository: MenuRepository) {

    fun execute(intent: IntentResult): AgentResponse {
        return when (intent.intent) {
            com.example.a2ui_sample.domain.model.UserIntent.MENU_SEARCH -> {
                val category = intent.entities["category"] as? String
                val diet = intent.entities["diet"] as? String
                val maxPrice = (intent.entities["price_limit"] as? Number)?.toInt()
                
                val items = repository.searchMenu(category, diet, maxPrice)
                val message = if (items.isEmpty()) "I couldn't find anything matching your request." 
                             else "Here is what I found for you:"
                AgentResponse.MenuResults(items, message)
            }
            com.example.a2ui_sample.domain.model.UserIntent.MENU_RECOMMEND -> {
                val items = repository.getMenuItems().filter { it.isBestSeller }.take(5)
                AgentResponse.Recommendations(items)
            }
            else -> AgentResponse.Message("I'm not sure how to handle this menu request.")
        }
    }
}
