package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.Feedback
import com.example.a2ui_sample.domain.model.Reservation
import com.example.a2ui_sample.domain.model.TableBooking
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.example.a2ui_sample.domain.repository.ReservationRepository
import com.example.a2ui_sample.domain.repository.OrderRepository
import com.example.a2ui_sample.domain.repository.DeliveryRepository
import com.example.a2ui_sample.domain.repository.FeedbackRepository
import com.example.a2ui_sample.domain.valueobjects.ReservationId
import com.example.a2ui_sample.domain.valueobjects.CustomerId
import com.example.a2ui_sample.domain.valueobjects.RestaurantId
import com.example.a2ui_sample.domain.valueobjects.TableId
import com.example.a2ui_sample.domain.valueobjects.TimeSlot
import com.example.a2ui_sample.domain.valueobjects.ReservationStatus
import com.example.a2ui_sample.domain.valueobjects.BookingSource
import com.example.a2ui_sample.domain.valueobjects.FeedbackId
import com.example.a2ui_sample.domain.valueobjects.Rating
import com.example.a2ui_sample.domain.valueobjects.OrderId
import com.example.a2ui_sample.domain.valueobjects.OrderStatus
import com.google.adk.kt.annotations.Tool
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RestaurantTools
 * Specialist tools for the ADK-based Restaurant Agent.
 */
@Singleton
class RestaurantTools @Inject constructor(
    private val repository: MenuRepository,
    private val reservationRepository: ReservationRepository,
    private val orderRepository: OrderRepository,
    private val deliveryRepository: DeliveryRepository,
    private val feedbackRepository: FeedbackRepository
) {
    var lastResponse: AgentResponse? = null

    @Tool(
        name = "search_menu",
        description = "Search for food items by category, diet, or budget."
    )
    fun restaurantSearchMenu(
        category: String?,
        diet: String?,
        priceLimit: Int?,
        peopleCount: Int?
    ): String {
        Log.d("ADK_TOOLS", "TOOL: searchMenu started")
        
        val actualMaxPrice = if (priceLimit != null && peopleCount != null && peopleCount > 1) {
            priceLimit / (peopleCount ?: 1)
        } else priceLimit

        val items = repository.searchMenu(category, diet, actualMaxPrice)
        
        val message = when {
            items.isEmpty() -> "I couldn't find any matching items."
            priceLimit != null -> "For $peopleCount people within ₹$priceLimit, I suggest these dishes:"
            else -> "Here are some options for you:"
        }

        lastResponse = AgentResponse.MenuResults(items, message)
        return "Found ${items.size} items."
    }

    @Tool(
        name = "add_item_to_cart",
        description = "Add a food item to the customer's cart. Requires exact item name."
    )
    suspend fun restaurantAddItemToCart(itemName: String, quantity: Int): String {
        Log.d("ADK_TOOLS", "TOOL: addItemToCart started for '$itemName' (Qty: $quantity)")

        val query = itemName.trim().replace(Regex("\\s+"), " ")
        val items = repository.getMenuItems()

        var item = items.firstOrNull { it.name.equals(query, ignoreCase = true) }

        if (item == null) {
            Log.d("ADK_TOOLS", "TOOL: Exact match not found, trying fuzzy match")
            val containsMatches = items.filter {
                it.name.contains(itemName, ignoreCase = true) || itemName.contains(it.name, ignoreCase = true)
            }
            if (containsMatches.size == 1) {
                Log.d("ADK_TOOLS", "TOOL: Found single fuzzy match: ${containsMatches.first().name}")
                item = containsMatches.first()
            } else if (containsMatches.size > 1) {
                Log.d("ADK_TOOLS", "TOOL: Multiple matches found (${containsMatches.size})")
                lastResponse = AgentResponse.MenuResults(containsMatches, "Multiple items match '$itemName'. Which one?")
                return "Multiple items match '$itemName'. Which one?"
            }
        }

        if (item == null) {
            Log.d("ADK_TOOLS", "TOOL: Item not found at all")
            return "❌ Item '$itemName' not found in menu"
        }

        Log.d("ADK_TOOLS", "TOOL: Adding item ID ${item.id} to cart with quantity $quantity")
        repository.addToCart(item.id)
        if (quantity > 1) {
            repository.updateCartQuantity(item.id, quantity)
        }
        
        val currentCart = repository.getCart()
        val totalQuantity = currentCart.sumOf { it.quantity }
        lastResponse = AgentResponse.CartUpdate(item, totalQuantity)
        return "✅ ${item.name} added to cart (Qty: $quantity, Price: ₹${item.price.amount})"
    }

    @Tool(
        name = "manage_cart",
        description = "View, clear, or remove items from the cart. Action can be VIEW, CLEAR, or REMOVE."
    )
    suspend fun restaurantManageCart(action: String, itemName: String?): String {
        Log.d("ADK_TOOLS", "TOOL: manageCart('$action')")
        
        return when (action.uppercase()) {
            "VIEW" -> {
                val items = repository.getCart()
                val total = repository.getCartTotal()
                lastResponse = AgentResponse.CartView(items, (total * 1.05).toInt())
                "Cart has ${items.size} items."
            }
            "CLEAR" -> {
                repository.clearCart()
                lastResponse = AgentResponse.Message("Your cart has been cleared.")
                "Cart cleared."
            }
            "REMOVE" -> {
                if (itemName != null) {
                    val cart = repository.getCart()
                    val item = cart.find { it.menuItem.name.contains(itemName, ignoreCase = true) }
                    if (item != null) {
                        repository.removeFromCart(item.menuItem.id)
                        lastResponse = AgentResponse.Message("${item.menuItem.name} removed.")
                        "Removed ${item.menuItem.name}."
                    } else "Item not in cart."
                } else "What should I remove?"
            }
            else -> "Invalid cart action."
        }
    }

    @Tool(
        name = "restaurant_checkout",
        description = "Proceed to checkout and show order summary."
    )
    suspend fun restaurantCheckout(): String {
        val items = repository.getCart()
        if (items.isEmpty()) return "Your cart is empty."
        
        val subtotal = repository.getCartTotal()
        val tax = (subtotal * 0.05).toInt()
        lastResponse = AgentResponse.OrderSummary(items, subtotal, tax, subtotal + tax)
        return "Showing order summary for ₹${subtotal + tax}."
    }

    @Tool(
        name = "restaurant_track_order",
        description = "Track the status of the last order or a specific order ID."
    )
    suspend fun restaurantTrackOrder(orderId: String?): String {
        val id = if (orderId != null) OrderId(orderId) else {
            orderRepository.getAllOrders().first().firstOrNull()?.id
        }
        
        if (id == null) return "No orders found to track."
        
        val order = orderRepository.getOrderById(id)
        if (order?.status == OrderStatus.CANCELLED) {
            lastResponse = AgentResponse.Message("❌ Order Cancelled\nOrder ID: ${id.value}")
            return "Order ${id.value} is cancelled."
        }

        val delivery = deliveryRepository.getDeliveryByOrderId(id)
        if (delivery != null && order != null) {
            lastResponse = AgentResponse.DeliveryUpdate(delivery, order)
            return "Tracking order ${id.value}: ${delivery.status}."
        } else {
            return "Order status: ${order?.status ?: "Unknown"}."
        }
    }

    @Tool(
        name = "get_special_offers",
        description = "Get current special offers and discounts."
    )
    fun getSpecialOffers(): String {
        return "Currently we have 20% off on all North Indian main courses!"
    }

    @Tool(
        name = "get_menu_categories",
        description = "Get list of available food categories like Pizza, North Indian, Drinks."
    )
    fun getMenuCategories(): String {
        val categories = repository.getMenuItems().map { it.category }.distinct().joinToString(", ")
        return "Available categories: $categories"
    }

    @Tool(
        name = "book_table",
        description = "Create a table reservation with date, time, and people count."
    )
    suspend fun restaurantBookTable(date: String, time: String, peopleCount: Int): String {
        try {
            val reservation = Reservation(
                id = ReservationId(),
                customerId = CustomerId("guest"),
                restaurantId = RestaurantId("rest_1"),
                restaurantName = "Luxe Dining",
                tableId = TableId((1..20).random()),
                timeSlot = TimeSlot(System.currentTimeMillis(), System.currentTimeMillis() + 3600000),
                partySize = peopleCount,
                status = ReservationStatus.CONFIRMED,
                source = BookingSource.CHAT
            )
            reservationRepository.createReservation(reservation)
            lastResponse = AgentResponse.BookingConfirmation(TableBooking(
                reservation.id.value, peopleCount, date, time, reservation.tableId?.value ?: 0, reservation.status, reservation.createdAt
            ))
            return "✅ Table booked for $peopleCount on $date at $time."
        } catch (e: Exception) {
            return "❌ Booking failed."
        }
    }

    @Tool(
        name = "restaurant_submit_feedback",
        description = "Submit feedback or a rating for an order."
    )
    suspend fun restaurantSubmitFeedback(rating: Int, comment: String, orderId: String?): String {
        try {
            val r = Rating(rating.coerceIn(1, 5))
            val feedback = Feedback(
                id = FeedbackId(),
                orderId = OrderId(orderId ?: "unknown"),
                customerId = CustomerId("guest"),
                foodRating = r,
                deliveryRating = r,
                packagingRating = r,
                overallRating = r,
                comment = comment,
                createdAt = System.currentTimeMillis()
            )
            feedbackRepository.submitFeedback(feedback)
            lastResponse = AgentResponse.Message("Thank you for your feedback! ⭐ $rating/5")
            return "✅ Feedback submitted successfully."
        } catch (e: Exception) {
            return "❌ Failed to submit feedback: ${e.message}"
        }
    }
}
