package com.example.a2ui_sample.ai_assistant.tools

import com.example.a2ui_sample.domain.repository.MenuRepository
import com.google.adk.kt.annotations.Tool
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantCartTools @Inject constructor(
    private val repository: MenuRepository
) {
    @Tool(
        name = "view_cart",
        description = "Show the current items in the shopping cart."
    )
    suspend fun viewCart(): String {
        val cartItems = repository.getCart()
        if (cartItems.isEmpty()) return "Your cart is currently empty."
        val total = cartItems.sumOf { it.menuItem.price.amount * it.quantity }
        val itemsList = cartItems.joinToString("\n") { 
            "- ${it.menuItem.name} (Qty: ${it.quantity}, Price: ₹${it.menuItem.price.amount * it.quantity})"
        }
        return "Cart Items:\n$itemsList\n\nTotal: ₹$total"
    }

    @Tool(
        name = "remove_from_cart",
        description = "Remove a specific item from the shopping cart."
    )
    suspend fun removeFromCart(itemName: String): String {
        val cartItems = repository.getCart()
        val itemToRemove = cartItems.find { it.menuItem.name.contains(itemName, ignoreCase = true) }
        return if (itemToRemove != null) {
            repository.removeFromCart(itemToRemove.menuItem.id)
            "Successfully removed ${itemToRemove.menuItem.name} from cart."
        } else {
            "Item '$itemName' is not in your cart."
        }
    }

    @Tool(
        name = "update_cart_quantity",
        description = "Update the quantity of an item already in the cart."
    )
    suspend fun updateCartQuantity(itemName: String, quantity: Int): String {
        val cartItems = repository.getCart()
        val itemToUpdate = cartItems.find { it.menuItem.name.contains(itemName, ignoreCase = true) }
        return if (itemToUpdate != null) {
            repository.updateCartQuantity(itemToUpdate.menuItem.id, quantity)
            "Updated ${itemToUpdate.menuItem.name} quantity to $quantity."
        } else {
            "Item '$itemName' is not in your cart. Add it first!"
        }
    }

    @Tool(
        name = "clear_cart",
        description = "Remove all items from the shopping cart."
    )
    suspend fun clearCart(): String {
        repository.clearCart()
        return "Your cart has been cleared."
    }
}
