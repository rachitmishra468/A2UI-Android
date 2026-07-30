package com.example.a2ui_sample.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ingredient - a single ingredient line shown on a recipe-style menu item card.
 */
data class Ingredient(
    val text: String
)

/**
 * InstructionStep - a single preparation step shown on a recipe-style menu item card.
 */
data class InstructionStep(
    val text: String
)

/**
 * MenuItem represents a dish in the restaurant menu.
 * Includes recipe-card style metadata (rating, prep/cook time, servings,
 * ingredients, instructions) in addition to ordering fields (price, category, type).
 */
data class MenuItem(
    val id: Int,
    val name: String,
    val category: String,
    val type: String, // e.g., "Veg", "Non-Veg"
    val price: Int,
    val image: String,
    val description: String? = "",
    val rating: String = "4.5",
    val reviewCount: Int = 0,
    val prepTime: String = "",
    val cookTime: String = "",
    val servings: String = "",
    val ingredients: List<Ingredient> = emptyList(),
    val instructions: List<InstructionStep> = emptyList()
)

/**
 * CartItem represents an item added to the user's shopping cart.
 */
data class CartItem(
    val menuItem: MenuItem,
    var quantity: Int
)

/**
 * TableBooking represents a confirmed table reservation.
 */
data class TableBooking(
    val bookingId: String = generateBookingId(),
    val numberOfPeople: Int,
    val bookingTime: String,
    val bookingTimestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun generateBookingId(): String {
            val format = SimpleDateFormat("HHmmss", Locale.US)
            val timeStr = format.format(Date())
            return "TB-${timeStr}"
        }
    }
}

/**
 * OrderStatus tracks the lifecycle of a placed order.
 * PLACED/PREPARING -> shown under "Current Orders", COMPLETED -> shown under "Past Orders".
 */
enum class OrderStatus {
    PREPARING,
    COMPLETED
}

/**
 * OrderItem is a snapshot line-item captured at the time an order was placed.
 */
data class OrderItem(
    val menuItem: MenuItem,
    val quantity: Int
)

/**
 * Order represents a placed (checked-out) order, built from the cart at checkout time.
 */
data class Order(
    val orderId: String = generateOrderId(),
    val items: List<OrderItem>,
    val totalAmount: Int,
    val status: OrderStatus = OrderStatus.PREPARING,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun generateOrderId(): String {
            val format = SimpleDateFormat("HHmmss", Locale.US)
            val timeStr = format.format(Date())
            return "ORD-${timeStr}"
        }
    }
}

/**
 * Structured response from the Agent after reasoning and tool execution.
 */
sealed interface AgentResponse {
    data class MenuResults(val items: List<MenuItem>, val query: String) : AgentResponse
    data class Recommendations(val items: List<MenuItem>) : AgentResponse
    data class CartUpdate(val addedItem: MenuItem, val totalCount: Int) : AgentResponse
    data class CartView(val cartItems: List<CartItem>, val totalAmount: Int) : AgentResponse
    data class BookingRequest(val step: String, val query: String = "") : AgentResponse // "ask_people", "ask_time", "confirm"
    data class BookingConfirmation(val booking: TableBooking) : AgentResponse
    data class OrderPlaced(val order: Order) : AgentResponse
    data class Error(val message: String) : AgentResponse
    data class Message(val content: String) : AgentResponse
}
