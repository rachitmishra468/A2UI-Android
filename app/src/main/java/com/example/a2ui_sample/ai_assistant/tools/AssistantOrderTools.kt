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
    private val deliveryRepository: DeliveryRepository,
    private val menuRepository: com.example.a2ui_sample.domain.repository.MenuRepository
) {
    @Tool(
        name = "track_order",
        description = "Track the status of an order."
    )
    suspend fun trackOrder(orderId: String?): Map<String, Any?> {
        val id = if (!orderId.isNullOrBlank()) {
            // Remove non-digit characters if model passed something like "ORD-123"
            val cleanId = orderId.replace(Regex("[^0-9]"), "")
            if (cleanId.isEmpty()) null else OrderId(cleanId)
        } else {
            orderRepository.getAllOrders().first().firstOrNull()?.id
        } ?: return mapOf("message" to "No orders found to track.")

        val order = orderRepository.getOrderById(id) ?: return mapOf("message" to "Order with ID ${id.value} not found.")
        val delivery = deliveryRepository.getDeliveryByOrderId(id)
        
        val eta = delivery?.estimatedArrivalAt?.let {
            java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(it))
        }

        return mapOf(
            "message" to "Here is the status of your order ${id.value}:",
            "orderStatus" to order.status.name,
            "deliveryStatus" to (delivery?.status?.name ?: "UNKNOWN"),
            "eta" to eta
        )
    }

    @Tool(
        name = "cancel_order",
        description = "Cancel an existing order by its ID."
    )
    suspend fun cancelOrder(orderId: String): Map<String, Any?> {
        val cleanId = orderId.replace(Regex("[^0-9]"), "")
        if (cleanId.isEmpty()) return mapOf("message" to "Invalid Order ID provided.", "success" to false)
        
        val id = OrderId(cleanId)
        val order = orderRepository.getOrderById(id) ?: return mapOf("message" to "Order $orderId not found.", "success" to false)
        
        return if (order.status == com.example.a2ui_sample.domain.valueobjects.OrderStatus.CANCELLED) {
            mapOf("message" to "Order ${id.value} is already cancelled.", "success" to true)
        } else if (order.status == com.example.a2ui_sample.domain.valueobjects.OrderStatus.DELIVERED || 
                   order.status == com.example.a2ui_sample.domain.valueobjects.OrderStatus.COMPLETED) {
            mapOf("message" to "Order ${id.value} cannot be cancelled as it is already ${order.status.name.lowercase()}.", "success" to false)
        } else {
            orderRepository.updateOrderStatus(id, com.example.a2ui_sample.domain.valueobjects.OrderStatus.CANCELLED)
            mapOf("message" to "Successfully cancelled your order ${id.value}.", "success" to true)
        }
    }

    @Tool(
        name = "get_order_history",
        description = "View your past orders. Use 'latest' parameter to only get the most recent order status."
    )
    suspend fun getOrderHistory(onlyLatest: Boolean): Map<String, Any?> {
        val orders = orderRepository.getOrderHistory(CustomerId("guest"))
        if (orders.isEmpty()) return mapOf("message" to "You haven't placed any orders yet.", "orders" to emptyList<Any>())
        
        return if (onlyLatest) {
            val latest = orders.first()
            mapOf(
                "message" to "Here is your latest order status:",
                "latestOrder" to latest,
                "isLatestOnly" to true
            )
        } else {
            mapOf(
                "message" to "Here is your order history:",
                "orders" to orders,
                "isLatestOnly" to false
            )
        }
    }

    @Tool(
        name = "reorder_last_order",
        description = "Add all items from your most recent order back into the shopping cart."
    )
    suspend fun reorderLastOrder(): Map<String, Any?> {
        val orders = orderRepository.getOrderHistory(CustomerId("guest"))
        if (orders.isEmpty()) return mapOf("message" to "You have no previous orders to reorder.", "success" to false)
        
        val lastOrder = orders.first()
        val itemsAdded = mutableListOf<String>()
        
        lastOrder.items.forEach { orderItem ->
            menuRepository.addToCart(orderItem.menuItemId)
            if (orderItem.quantity > 1) {
                menuRepository.updateCartQuantity(orderItem.menuItemId, orderItem.quantity)
            }
            itemsAdded.add("${orderItem.quantity}x ${orderItem.menuItemName}")
        }
        
        return mapOf(
            "message" to "Successfully reordered items from your last order: ${itemsAdded.joinToString(", ")}.",
            "success" to true,
            "items" to itemsAdded
        )
    }
}
