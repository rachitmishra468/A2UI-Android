package com.example.a2ui_sample.data.repository

import android.content.Context
import kotlinx.coroutines.flow.asStateFlow
import com.example.a2ui_sample.domain.model.*
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.example.a2ui_sample.domain.valueobjects.OrderStatus
import com.example.a2ui_sample.domain.valueobjects.DeliveryId
import com.example.a2ui_sample.domain.valueobjects.DeliveryStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * MenuRepositoryImpl
 * Implementation of the MenuRepository interface using local assets and in-memory lists.
 */
class MenuRepositoryImpl private constructor(private val context: Context) : MenuRepository {
    private val gson = Gson()
    private val cart = mutableListOf<CartItem>()
    private val _cartFlow = kotlinx.coroutines.flow.MutableStateFlow<List<CartItem>>(emptyList())
    private val bookings = mutableListOf<TableBooking>()
    private val orders = mutableListOf<Order>()

    companion object {
        @Volatile
        private var INSTANCE: MenuRepositoryImpl? = null

        fun getInstance(context: Context): MenuRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MenuRepositoryImpl(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    override fun getMenuItems(): List<MenuItem> {
        return try {
            val jsonString = context.assets.open("menu.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<MenuItem>>() {}.type
            gson.fromJson(jsonString, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun searchMenu(category: String?, type: String?, maxPrice: Int?): List<MenuItem> {
        return getMenuItems().filter { item ->
            val matchesCategory = category == null || item.category.contains(category, ignoreCase = true) || item.name.contains(category, ignoreCase = true)
            val matchesType = type == null || item.type.name.equals(type, ignoreCase = true)
            val matchesPrice = maxPrice == null || item.price.amount <= maxPrice
            matchesCategory && matchesType && matchesPrice
        }
    }

    override suspend fun addToCart(menuItemId: Int): CartItem? {
        val item = getMenuItems().find { it.id == menuItemId } ?: return null
        val existing = cart.find { it.menuItem.id == menuItemId }
        val result = if (existing != null) {
            existing.quantity++
            existing
        } else {
            val newCartItem = CartItem(item, 1)
            cart.add(0, newCartItem)
            newCartItem
        }
        _cartFlow.value = cart.toList()
        return result
    }

    override suspend fun getCart(): List<CartItem> = cart

    override fun getCartFlow() = _cartFlow.asStateFlow()

    override suspend fun getCartTotal(): Int = cart.sumOf { (it.menuItem.price.amount * it.quantity) }

    override suspend fun addBooking(booking: TableBooking) {
        bookings.add(0, booking)
    }

    override suspend fun getBookings(): List<TableBooking> = bookings.toList()

    override suspend fun cancelBooking(bookingId: String): Boolean {
        return bookings.removeIf { it.id == bookingId || it.bookingId == bookingId }
    }

    override suspend fun updateCartQuantity(menuItemId: Int, quantity: Int): CartItem? {
        val existing = cart.find { it.menuItem.id == menuItemId } ?: return null
        val result = if (quantity <= 0) {
            cart.remove(existing)
            null
        } else {
            existing.quantity = quantity
            existing
        }
        _cartFlow.value = cart.toList()
        return result
    }

    override suspend fun removeFromCart(menuItemId: Int): Boolean {
        val existing = cart.find { it.menuItem.id == menuItemId } ?: return false
        val removed = cart.remove(existing)
        if (removed) {
            _cartFlow.value = cart.toList()
        }
        return removed
    }

    override suspend fun clearCart() {
        cart.clear()
        _cartFlow.value = emptyList()
    }

    override suspend fun placeOrder(order: Order): Boolean {
        orders.add(0, order)
        clearCart()
        return true
    }

    override suspend fun getCurrentOrders(): List<Order> = orders.filter { it.status != OrderStatus.COMPLETED }

    override suspend fun getPastOrders(): List<Order> = orders.filter { it.status == OrderStatus.COMPLETED }

    override suspend fun completeOrder(orderId: String) {
        val index = orders.indexOfFirst { it.id.value == orderId }
        if (index != -1) {
            orders[index] = orders[index].copy(status = OrderStatus.COMPLETED)
        }
    }

    override suspend fun getDeliveryStatus(orderId: String): Delivery? {
        val order = orders.find { it.id.value == orderId } ?: return null
        return Delivery(
            id = DeliveryId("DEL-${orderId.takeLast(4)}"),
            orderId = order.id,
            courierName = "Rahul Sharma",
            courierPhone = "+91 98765 43210",
            estimatedArrivalAt = System.currentTimeMillis() + 15 * 60 * 1000,
            status = DeliveryStatus.IN_TRANSIT,
            deliveryAddress = "123, Luxury Heights, Indiranagar, Bangalore"
        )
    }
}
