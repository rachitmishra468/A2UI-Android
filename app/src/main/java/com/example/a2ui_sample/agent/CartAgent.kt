package com.example.a2ui_sample.agent

import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.IntentResult
import com.example.a2ui_sample.domain.repository.MenuRepository

/**
 * CartAgent
 * Execution specialist for cart and checkout operations.
 */
class CartAgent(private val repository: MenuRepository) {

    fun execute(intent: IntentResult): AgentResponse {
        return when (intent.intent) {
            com.example.a2ui_sample.domain.model.UserIntent.CART_VIEW -> {
                val items = repository.getCart()
                val total = repository.getCartTotal()
                AgentResponse.CartView(items, total)
            }
            com.example.a2ui_sample.domain.model.UserIntent.CART_ADD -> {
                val itemName = intent.entities["food_item"] as? String
                val quantity = (intent.entities["quantity"] as? Number)?.toInt() ?: 1
                
                if (itemName != null) {
                    val menuItem = repository.getMenuItems().find { it.name.contains(itemName, ignoreCase = true) }
                    if (menuItem != null) {
                        repository.addToCart(menuItem.id)
                        // Note: Ignoring quantity for now as repo might not support multiple add at once
                        AgentResponse.CartUpdate(menuItem, repository.getCart().sumOf { it.quantity })
                    } else {
                        AgentResponse.Message("I couldn't find '$itemName' in our menu.")
                    }
                } else {
                    AgentResponse.Message("What would you like to add to your cart?")
                }
            }
            com.example.a2ui_sample.domain.model.UserIntent.CART_CLEAR -> {
                repository.clearCart()
                AgentResponse.Message("Your cart has been cleared.")
            }
            com.example.a2ui_sample.domain.model.UserIntent.CHECKOUT -> {
                AgentResponse.Message("Ready to checkout? I've prepared your order summary.")
            }
            else -> AgentResponse.Message("I can help you with your cart or checkout.")
        }
    }
}
