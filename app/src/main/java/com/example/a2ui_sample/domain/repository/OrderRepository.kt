package com.example.a2ui_sample.domain.repository

import com.example.a2ui_sample.domain.model.Order
import com.example.a2ui_sample.domain.valueobjects.OrderId
import com.example.a2ui_sample.domain.valueobjects.OrderStatus
import com.example.a2ui_sample.domain.valueobjects.CustomerId
import kotlinx.coroutines.flow.Flow

/**
 * OrderRepository Contract
 * Handles lifecycle of food orders.
 */
interface OrderRepository {
    suspend fun placeOrder(order: Order): OrderId
    suspend fun getOrderById(id: OrderId): Order?
    fun getActiveOrders(): Flow<List<Order>>
    fun getAllOrders(): Flow<List<Order>>
    suspend fun updateOrderStatus(id: OrderId, status: OrderStatus)
    suspend fun getOrderHistory(customerId: CustomerId): List<Order>
}
