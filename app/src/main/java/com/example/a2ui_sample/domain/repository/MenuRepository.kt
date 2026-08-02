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
    fun addToCart(menuItemId: Int): CartItem?
    fun getCart(): List<CartItem>
    fun getCartTotal(): Int
    fun updateCartQuantity(menuItemId: Int, quantity: Int): CartItem?
    fun removeFromCart(menuItemId: Int): Boolean
    fun clearCart()
    
    // Bookings
    fun addBooking(booking: TableBooking)
    fun getBookings(): List<TableBooking>
    
    // Orders
    fun placeOrder(order: Order): Boolean
    fun getCurrentOrders(): List<Order>
    fun getPastOrders(): List<Order>
    fun completeOrder(orderId: String)
}
