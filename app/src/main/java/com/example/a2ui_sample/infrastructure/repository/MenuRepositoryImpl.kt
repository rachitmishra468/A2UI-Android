package com.example.a2ui_sample.infrastructure.repository

import android.content.Context
import com.example.a2ui_sample.domain.model.*
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.example.a2ui_sample.domain.valueobjects.CustomerId
import com.example.a2ui_sample.domain.valueobjects.OrderId
import com.example.a2ui_sample.domain.valueobjects.OrderStatus
import com.example.a2ui_sample.domain.valueobjects.Price
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MenuRepository {
    private val gson = Gson()
    private val cart = mutableListOf<CartItem>()
    private val bookings = mutableListOf<TableBooking>()
    private val orders = mutableListOf<Order>()
    private val defaultCustomerId = CustomerId("guest")

    private val menuItemsCache: List<MenuItem> by lazy {
        try {
            val jsonString = context.assets.open("menu.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<MenuItem>>() {}.type
            gson.fromJson<List<MenuItem>>(jsonString, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getMenuItems(): List<MenuItem> = menuItemsCache

    override fun searchMenu(category: String?, type: String?, maxPrice: Int?): List<MenuItem> {
        return menuItemsCache.filter { item ->
            val matchesCategory = category == null || item.category.contains(category, ignoreCase = true) || item.name.contains(category, ignoreCase = true)
            val matchesType = type == null || item.type.name.equals(type, ignoreCase = true)
            val matchesPrice = maxPrice == null || item.price.amount <= maxPrice
            matchesCategory && matchesType && matchesPrice
        }
    }

    override fun addToCart(menuItemId: Int): CartItem? {
        val item = menuItemsCache.find { it.id == menuItemId } ?: return null
        val existing = cart.find { it.menuItem.id == menuItemId }
        return if (existing != null) {
            existing.quantity++
            existing
        } else {
            val newCartItem = CartItem(item, 1)
            cart.add(0, newCartItem)
            newCartItem
        }
    }

    override fun getCart(): List<CartItem> = cart

    override fun getCartTotal(): Int = cart.sumOf { (it.menuItem.price.amount * it.quantity) }

    override fun updateCartQuantity(menuItemId: Int, quantity: Int): CartItem? {
        val existing = cart.find { it.menuItem.id == menuItemId } ?: return null
        if (quantity <= 0) {
            cart.remove(existing)
            return null
        }
        existing.quantity = quantity
        return existing
    }

    override fun removeFromCart(menuItemId: Int): Boolean {
        val existing = cart.find { it.menuItem.id == menuItemId } ?: return false
        return cart.remove(existing)
    }

    override fun clearCart() {
        cart.clear()
    }

    override fun addBooking(booking: TableBooking) {
        bookings.add(0, booking)
    }

    override fun getBookings(): List<TableBooking> = bookings.toList()

    override fun placeOrder(order: Order): Boolean {
        orders.add(0, order)
        clearCart()
        return true
    }

    override fun getCurrentOrders(): List<Order> = orders.filter { it.status != OrderStatus.COMPLETED }

    override fun getPastOrders(): List<Order> = orders.filter { it.status == OrderStatus.COMPLETED }

    override fun completeOrder(orderId: String) {
        val index = orders.indexOfFirst { it.id.value == orderId }
        if (index != -1) {
            orders[index] = orders[index].copy(status = OrderStatus.COMPLETED)
        }
    }
}
