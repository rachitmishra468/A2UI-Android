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
    suspend fun viewCart(): Map<String, Any?> {
        val cartItems = repository.getCart()
        val total = cartItems.sumOf { it.menuItem.price.amount * it.quantity }
        return mapOf(
            "items" to cartItems,
            "total" to total,
            "message" to if (cartItems.isEmpty()) "Your cart is currently empty." else "Here are the items in your cart."
        )
    }

    @Tool(
        name = "add_to_cart",
        description = "Add a specific menu item to the shopping cart by its name and quantity."
    )
    suspend fun addToCart(itemName: String, quantity: Int): Map<String, Any?> {
        val allItems = repository.getMenuItems()
        
        // Clean item name for better matching (remove common suffixes)
        val cleanName = itemName.replace(" burger", "", ignoreCase = true)
            .replace(" pizza", "", ignoreCase = true)
            .replace(" fries", "", ignoreCase = true)
            .trim()

        val itemToAdd = allItems.find { it.name.contains(cleanName, ignoreCase = true) } 
            ?: allItems.find { cleanName.contains(it.name, ignoreCase = true) }

        return if (itemToAdd != null) {
            val cartItems = repository.getCart()
            val existingItem = cartItems.find { it.menuItem.id == itemToAdd.id }
            val currentQty = existingItem?.quantity ?: 0
            val targetQty = currentQty + (if (quantity <= 0) 1 else quantity)

            if (currentQty == 0) {
                repository.addToCart(itemToAdd.id)
                if (targetQty > 1) {
                    repository.updateCartQuantity(itemToAdd.id, targetQty)
                }
            } else {
                repository.updateCartQuantity(itemToAdd.id, targetQty)
            }

            mapOf(
                "message" to "Successfully added ${if (quantity <= 0) 1 else quantity} x ${itemToAdd.name} to your cart.",
                "success" to true,
                "item" to itemToAdd,
                "quantity" to (if (quantity <= 0) 1 else quantity)
            )
        } else {
            mapOf(
                "message" to "Could not find '$itemName' in the menu. Try being more specific or browse the menu.",
                "success" to false
            )
        }
    }

    @Tool(
        name = "remove_from_cart",
        description = "Remove a specific item from the shopping cart."
    )
    suspend fun removeFromCart(itemName: String): Map<String, Any?> {
        val cartItems = repository.getCart()
        val itemToRemove = cartItems.find { it.menuItem.name.contains(itemName, ignoreCase = true) }
        return if (itemToRemove != null) {
            repository.removeFromCart(itemToRemove.menuItem.id)
            mapOf(
                "message" to "Successfully removed ${itemToRemove.menuItem.name} from cart.",
                "success" to true,
                "item" to itemToRemove.menuItem
            )
        } else {
            mapOf(
                "message" to "Item '$itemName' is not in your cart.",
                "success" to false
            )
        }
    }

    @Tool(
        name = "update_cart_quantity",
        description = "Update the quantity of an item already in the cart."
    )
    suspend fun updateCartQuantity(itemName: String, quantity: Int): Map<String, Any?> {
        val cartItems = repository.getCart()
        val itemToUpdate = cartItems.find { it.menuItem.name.contains(itemName, ignoreCase = true) }
        return if (itemToUpdate != null) {
            repository.updateCartQuantity(itemToUpdate.menuItem.id, quantity)
            mapOf(
                "message" to "Updated ${itemToUpdate.menuItem.name} quantity to $quantity.",
                "success" to true,
                "item" to itemToUpdate.menuItem,
                "quantity" to quantity
            )
        } else {
            mapOf(
                "message" to "Item '$itemName' is not in your cart. Add it first!",
                "success" to false
            )
        }
    }

    @Tool(
        name = "clear_cart",
        description = "Remove all items from the shopping cart."
    )
    suspend fun clearCart(): Map<String, Any?> {
        repository.clearCart()
        return mapOf("message" to "Your cart has been cleared.", "success" to true)
    }

    @Tool(
        name = "checkout",
        description = "Start the checkout process and show order summary."
    )
    suspend fun checkout(): Map<String, Any?> {
        val cartItems = repository.getCart()
        val total = cartItems.sumOf { it.menuItem.price.amount * it.quantity }
        return if (cartItems.isEmpty()) {
            mapOf(
                "message" to "Your cart is empty. Add some items before checking out!",
                "success" to false
            )
        } else {
            mapOf(
                "items" to cartItems,
                "total" to total,
                "message" to "Here is your order summary. Please choose a payment method to complete your order.",
                "success" to true
            )
        }
    }
}
