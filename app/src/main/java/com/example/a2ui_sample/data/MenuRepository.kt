package com.example.a2ui_sample.data.repository

import android.content.Context
import com.example.a2ui_sample.domain.model.CartItem
import com.example.a2ui_sample.domain.model.MenuItem
import com.example.a2ui_sample.domain.model.Order
import com.example.a2ui_sample.domain.model.OrderItem
import com.example.a2ui_sample.domain.model.OrderStatus
import com.example.a2ui_sample.domain.model.TableBooking
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * MenuRepository
 * Manages local menu data, cart state, and bookings.
 */
class MenuRepository private constructor(private val context: Context) {
    private val gson = Gson()
    private val cart = mutableListOf<CartItem>()
    private val bookings = mutableListOf<TableBooking>()
    private val orders = mutableListOf<Order>()

    companion object {
        @Volatile
        private var INSTANCE: MenuRepository? = null

        fun getInstance(context: Context): MenuRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MenuRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun getMenuItems(): List<MenuItem> {
        return try {
            val jsonString = context.assets.open("menu.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<MenuItem>>() {}.type
            gson.fromJson(jsonString, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun searchMenu(category: String?, type: String?, maxPrice: Int?): List<MenuItem> {
        return getMenuItems().filter { item ->
            val matchesCategory = category == null || item.category.contains(category, ignoreCase = true) || item.name.contains(category, ignoreCase = true)
            val matchesType = type == null || item.type.equals(type, ignoreCase = true)
            val matchesPrice = maxPrice == null || item.price <= maxPrice
            matchesCategory && matchesType && matchesPrice
        }
    }

    /**
     * Adds an item to the cart. Whether the item is brand new or already in the cart,
     * it is moved to index 0 so the most recently added item always shows at the top
     * of the cart list (both for manual "Add to Cart" taps and agent add_item_to_cart calls).
     */
    fun addToCart(itemId: Int): CartItem? {
        val item = getMenuItems().find { it.id == itemId } ?: return null
        val existing = cart.find { it.menuItem.id == itemId }
        return if (existing != null) {
            existing.quantity++
            cart.remove(existing)
            cart.add(0, existing)
            existing
        } else {
            val newCartItem = CartItem(item, 1)
            cart.add(0, newCartItem)
            newCartItem
        }
    }

    fun getCart(): List<CartItem> = cart

    fun getCartTotal(): Int = cart.sumOf { it.menuItem.price * it.quantity }

    fun addBooking(booking: TableBooking) {
        bookings.add(booking)
    }

    fun getBookings(): List<TableBooking> = bookings.toList()

    // Additional cart management helpers for UI
    fun updateCartQuantity(menuItemId: Int, quantity: Int): CartItem? {
        val existing = cart.find { it.menuItem.id == menuItemId } ?: return null
        if (quantity <= 0) {
            cart.remove(existing)
            return null
        }
        existing.quantity = quantity
        return existing
    }

    fun removeFromCart(menuItemId: Int): Boolean {
        val existing = cart.find { it.menuItem.id == menuItemId } ?: return false
        return cart.remove(existing)
    }

    fun clearCart() {
        cart.clear()
    }

    /**
     * Places an order from the current cart contents (a snapshot), then empties the cart.
     * Returns null if the cart is empty.
     */
    fun placeOrder(): Order? {
        if (cart.isEmpty()) return null
        val orderItems = cart.map { OrderItem(it.menuItem, it.quantity) }
        val order = Order(
            items = orderItems,
            totalAmount = getCartTotal(),
            status = OrderStatus.PREPARING
        )
        orders.add(0, order)
        cart.clear()
        return order
    }

    fun getAllOrders(): List<Order> = orders.toList()

    fun getCurrentOrders(): List<Order> = orders.filter { it.status != OrderStatus.COMPLETED }

    fun getPastOrders(): List<Order> = orders.filter { it.status == OrderStatus.COMPLETED }

    /** Marks a current order as completed, moving it into "past orders". */
    fun completeOrder(orderId: String): Boolean {
        val index = orders.indexOfFirst { it.orderId == orderId }
        if (index == -1) return false
        orders[index] = orders[index].copy(status = OrderStatus.COMPLETED)
        return true
    }
}