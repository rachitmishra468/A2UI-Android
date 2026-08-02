package com.example.a2ui_sample.domain.repository

import com.example.a2ui_sample.domain.model.*

/**
 * Interface defining the operations for Menu, Cart, and Bookings.
 * In Clean Architecture, the Domain layer defines this interface,
 * and the Data layer implements it.
 */
interface MenuRepository {
    fun getMenuItems(): List<MenuItem>
    fun searchMenu(category: String?, type: String?, maxPrice: Int?): List<MenuItem>
    fun addToCart(menuItemId: Int): CartItem?
    fun removeFromCart(menuItemId: Int): Boolean
    fun updateCartQuantity(menuItemId: Int, quantity: Int): CartItem?
    fun getCart(): List<CartItem>
    fun getCartTotal(): Int
    fun addBooking(booking: TableBooking)
    fun getBookings(): List<TableBooking>
    fun placeOrder(): Order?
    fun getCurrentOrders(): List<Order>
    fun getPastOrders(): List<Order>
    fun completeOrder(orderId: String)
}
