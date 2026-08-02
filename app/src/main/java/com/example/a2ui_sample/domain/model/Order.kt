package com.example.a2ui_sample.domain.model

import com.example.a2ui_sample.domain.valueobjects.OrderId
import com.example.a2ui_sample.domain.valueobjects.OrderStatus
import com.example.a2ui_sample.domain.valueobjects.CustomerId
import com.example.a2ui_sample.domain.valueobjects.Price
import com.example.a2ui_sample.domain.valueobjects.Quantity

/**
 * OrderItem Entity (Part of Order Aggregate)
 */
data class OrderItem(
    val menuItemId: Int,
    val menuItemName: String,
    val quantity: Int,
    val unitPrice: Price
) {
    val totalPrice: Price get() = unitPrice.multiply(quantity)
}

/**
 * Order Aggregate Root
 * Represents a food order.
 */
data class Order(
    val id: OrderId,
    val customerId: CustomerId = CustomerId("guest"),
    val items: List<OrderItem>,
    val subtotal: Price,
    val tax: Price,
    val discount: Price = Price(0),
    val totalAmount: Price,
    val status: OrderStatus = OrderStatus.PENDING,
    val orderTime: Long = System.currentTimeMillis(),
    val estimatedTimeMinutes: Int = 25,
    val specialInstructions: String? = null
)
