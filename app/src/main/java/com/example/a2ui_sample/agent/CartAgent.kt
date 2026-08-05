package com.example.a2ui_sample.agent

import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.IntentResultWrapper
import com.example.a2ui_sample.domain.repository.MenuRepository

/**
 * CartAgent
 * Specialist for cart and checkout operations.
 */
class CartAgent(private val repository: MenuRepository) {

    suspend fun execute(intent: IntentResultWrapper): AgentResponse {
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
                        val currentCart = repository.getCart()
                        val totalItems = currentCart.sumOf { it.quantity }
                        val total = repository.getCartTotal()
                        AgentResponse.CartUpdate(menuItem, totalItems, "Great choice! 🛒 I've added ${menuItem.name} to your cart. Your total is now ₹$total. Anything else? 😊")
                    } else {
                        AgentResponse.Message("Hmm, I couldn't find '$itemName' on our menu. 🤔 Would you like to browse the menu?")
                    }
                } else {
                    AgentResponse.Message("What delicious item would you like to add? 😋")
                }
            }
            com.example.a2ui_sample.domain.model.UserIntent.CART_REMOVE -> {
                val itemName = intent.entities["food_item"] as? String
                val cart = repository.getCart()
                
                if (cart.isEmpty()) {
                    return AgentResponse.Message("Your cart is empty already! 🛒 Would you like to browse our menu?")
                }

                if (itemName != null && !itemName.contains("this item", ignoreCase = true)) {
                    val cartItem = cart.find { it.menuItem.name.contains(itemName, ignoreCase = true) }
                    if (cartItem != null) {
                        repository.removeFromCart(cartItem.menuItem.id)
                        val newTotal = repository.getCartTotal()
                        AgentResponse.Message("Done! ✅ I've removed ${cartItem.menuItem.name} from your cart. Your new total is ₹$newTotal.")
                    } else {
                        AgentResponse.Message("I couldn't find '$itemName' in your cart. 🤔 Would you like to see what's in there?")
                    }
                } else {
                    val lastItem = cart.first()
                    repository.removeFromCart(lastItem.menuItem.id)
                    val newTotal = repository.getCartTotal()
                    AgentResponse.Message("Removed! ✅ ${lastItem.menuItem.name} is out of your cart. New total: ₹$newTotal.")
                }
            }
            com.example.a2ui_sample.domain.model.UserIntent.CART_CLEAR -> {
                val cart = repository.getCart()
                if (cart.isEmpty()) {
                    return AgentResponse.Message("Your cart is already empty! 🛒 Ready to order something delicious?")
                }
                repository.clearCart()
                AgentResponse.Message("All cleared! 🗑️ Your cart is now empty. What would you like to order?")
            }
            com.example.a2ui_sample.domain.model.UserIntent.CHECKOUT -> {
                val items = repository.getCart()
                if (items.isEmpty()) {
                    return AgentResponse.Message("Oops! Your cart is empty. 🛒 Let me help you find something tasty first! 😋")
                }

                val subtotal = repository.getCartTotal()
                val tax = (subtotal * 0.05).toInt()
                val grandTotal = subtotal + tax
                
                AgentResponse.OrderSummary(items, subtotal, tax, grandTotal, "Great! 🎉 Here's your order summary. Ready to proceed?")
            }
            else -> AgentResponse.Message("I can help you manage your cart! 🛒 Want to add items, view cart, or checkout?")
        }
    }
}
