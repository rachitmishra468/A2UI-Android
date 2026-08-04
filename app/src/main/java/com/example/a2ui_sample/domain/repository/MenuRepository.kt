package com.example.a2ui_sample.domain.repository

import com.example.a2ui_sample.domain.model.*

/**
 * MenuRepository Contract
 * Defines operations for browsing and searching the restaurant menu.
 */
interface MenuRepository {
    // Menu
    fun getMenuItems(): List<MenuItem>
    fun searchMenu(category: String? = null, type: String? = null, maxPrice: Int? = null): List<MenuItem>
    
    // Cart
    suspend fun addToCart(menuItemId: Int): CartItem?
    suspend fun getCart(): List<CartItem>
    fun getCartFlow(): kotlinx.coroutines.flow.Flow<List<CartItem>>
    suspend fun getCartTotal(): Int
    suspend fun updateCartQuantity(menuItemId: Int, quantity: Int): CartItem?
    suspend fun removeFromCart(menuItemId: Int): Boolean
    suspend fun clearCart()
    
    // Bookings
    suspend fun addBooking(booking: TableBooking)
    suspend fun getBookings(): List<TableBooking>
    suspend fun cancelBooking(bookingId: String): Boolean
    
    // Orders
    suspend fun placeOrder(order: Order): Boolean
    suspend fun getCurrentOrders(): List<Order>
    suspend fun getPastOrders(): List<Order>
    suspend fun completeOrder(orderId: String)
    suspend fun getDeliveryStatus(orderId: String): Delivery?
}
