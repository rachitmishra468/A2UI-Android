package com.example.a2ui_sample.ai_assistant.tools

import com.example.a2ui_sample.domain.repository.OrderRepository
import com.example.a2ui_sample.domain.repository.DeliveryRepository
import com.example.a2ui_sample.domain.valueobjects.CustomerId
import com.example.a2ui_sample.domain.valueobjects.OrderId
import com.google.adk.kt.annotations.Tool
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantOrderTools @Inject constructor(
    private val orderRepository: OrderRepository,
    private val deliveryRepository: DeliveryRepository
) {
    @Tool(
        name = "track_order",
        description = "Track the status of an order."
    )
    suspend fun trackOrder(orderId: String?): String {
        val id = if (orderId != null) OrderId(orderId) else {
            orderRepository.getAllOrders().first().firstOrNull()?.id
        } ?: return "No orders found to track."

        val order = orderRepository.getOrderById(id) ?: return "Order not found."
        val delivery = deliveryRepository.getDeliveryByOrderId(id)
        
        return "Order Status: ${order.status}\n" + (delivery?.let { "Delivery Status: ${it.status}" } ?: "")
    }

    @Tool(
        name = "get_order_history",
        description = "View your past orders."
    )
    suspend fun getOrderHistory(): String {
        val orders = orderRepository.getOrderHistory(CustomerId("guest"))
        if (orders.isEmpty()) return "You haven't placed any orders yet."
        return "Order History:\n" + orders.joinToString("\n") { 
            "- Order ₹${it.totalAmount} (Status: ${it.status})"
        }
    }
}
