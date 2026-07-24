package com.example.a2ui_sample.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MenuItem represents a dish in the restaurant menu.
 */
data class MenuItem(
    val id: Int,
    val name: String,
    val category: String,
    val type: String, // e.g., "Veg", "Non-Veg"
    val price: Int,
    val image: String,
    val description: String = ""
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
 * Structured response from the Agent after reasoning and tool execution.
 */
sealed interface AgentResponse {
    data class MenuResults(val items: List<MenuItem>, val query: String) : AgentResponse
    data class Recommendations(val items: List<MenuItem>) : AgentResponse
    data class CartUpdate(val addedItem: MenuItem, val totalCount: Int) : AgentResponse
    data class CartView(val cartItems: List<CartItem>, val totalAmount: Int) : AgentResponse
    data class BookingRequest(val step: String, val query: String = "") : AgentResponse // "ask_people", "ask_time", "confirm"
    data class BookingConfirmation(val booking: TableBooking) : AgentResponse
    data class Error(val message: String) : AgentResponse
    data class Message(val content: String) : AgentResponse
}
