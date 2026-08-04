package com.example.a2ui_sample.agent

import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.IntentResult
import com.example.a2ui_sample.domain.repository.MenuRepository

/**
 * CartAgent
 * Execution specialist for cart and checkout operations.
 */
class CartAgent(private val repository: MenuRepository) {

    suspend fun execute(intent: IntentResult): AgentResponse {
        return when (intent.intent) {
            com.example.a2ui_sample.domain.model.UserIntent.CART_VIEW -> {
                val items = repository.getCart()
                val subtotal = repository.getCartTotal()
                val tax = (subtotal * 0.05).toInt()
                val grandTotal = subtotal + tax
                AgentResponse.CartView(items, grandTotal)
            }
            com.example.a2ui_sample.domain.model.UserIntent.CART_ADD -> {
                val itemName = intent.entities["food_item"] as? String
                val quantity = (intent.entities["quantity"] as? Number)?.toInt() ?: 1
                
                if (itemName != null) {
                    val menuItem = repository.getMenuItems().find { it.name.contains(itemName, ignoreCase = true) }
                    if (menuItem != null) {
                        repository.addToCart(menuItem.id)
                        // Note: Ignoring quantity for now as repo might not support multiple add at once
                        val currentCart = repository.getCart()
                        AgentResponse.CartUpdate(menuItem, currentCart.sumOf { it.quantity })
                    } else {
                        AgentResponse.Message("I couldn't find '$itemName' in our menu.")
                    }
                } else {
                    AgentResponse.Message("What would you like to add to your cart?")
                }
            }
            com.example.a2ui_sample.domain.model.UserIntent.CART_REMOVE -> {
                val itemName = intent.entities["food_item"] as? String
                val cart = repository.getCart()
                
                if (cart.isEmpty()) {
                    return AgentResponse.Message("Your cart is already empty.")
                }

                if (itemName != null && !itemName.contains("this item", ignoreCase = true)) {
                    val cartItem = cart.find { it.menuItem.name.contains(itemName, ignoreCase = true) }
                    if (cartItem != null) {
                        repository.removeFromCart(cartItem.menuItem.id)
                        AgentResponse.Message("I've removed ${cartItem.menuItem.name} from your cart.")
                    } else {
                        AgentResponse.Message("I couldn't find '$itemName' in your cart.")
                    }
                } else {
                    // Fallback: Remove the most recently added item
                    val lastItem = cart.first() // Cart is usually 0-indexed with latest at top in this repo
                    repository.removeFromCart(lastItem.menuItem.id)
                    AgentResponse.Message("I've removed ${lastItem.menuItem.name} from your cart.")
                }
            }
            com.example.a2ui_sample.domain.model.UserIntent.CART_CLEAR -> {
                repository.clearCart()
                AgentResponse.Message("Your cart has been cleared.")
            }
            com.example.a2ui_sample.domain.model.UserIntent.CHECKOUT -> {
                android.util.Log.d("A2UI_FLOW", "[CHECKOUT] Intent Detected")
                val items = repository.getCart()
                android.util.Log.d("A2UI_FLOW", "[CHECKOUT] Cart Retrieved: ${items.size} items")
                
                if (items.isEmpty()) {
                    return AgentResponse.Message("Your cart is empty. Please add some items before checking out.")
                }

                val subtotal = repository.getCartTotal()
                val tax = (subtotal * 0.05).toInt()
                val grandTotal = subtotal + tax
                
                android.util.Log.d("A2UI_FLOW", "[CHECKOUT] Order Summary Generated: Subtotal=$subtotal, Total=$grandTotal")
                AgentResponse.OrderSummary(items, subtotal, tax, grandTotal)
            }
            else -> AgentResponse.Message("I can help you with your cart or checkout.")
        }
    }
}
