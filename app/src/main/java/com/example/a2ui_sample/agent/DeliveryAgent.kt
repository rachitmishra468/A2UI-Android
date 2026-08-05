package com.example.a2ui_sample.agent

import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.IntentResultWrapper
import com.example.a2ui_sample.domain.repository.DeliveryRepository
import com.example.a2ui_sample.domain.repository.OrderRepository
import com.example.a2ui_sample.domain.valueobjects.CustomerId
import com.example.a2ui_sample.domain.valueobjects.OrderId
import kotlinx.coroutines.flow.first

/**
 * DeliveryAgent
 * Specialist for real-time order tracking and delivery status.
 */
class DeliveryAgent(
    private val orderRepository: OrderRepository,
    private val deliveryRepository: DeliveryRepository,
    private val menuRepository: com.example.a2ui_sample.domain.repository.MenuRepository
) {

    suspend fun execute(intent: IntentResultWrapper): AgentResponse {
        return when (intent.intent) {
            com.example.a2ui_sample.domain.model.UserIntent.ORDER_TRACKING -> {
                val orderIdStr = intent.entities["order_id"] as? String
                
                if (orderIdStr != null) {
                    val orderId = OrderId(orderIdStr)
                    val order = orderRepository.getOrderById(orderId)
                    
                    if (order != null) {
                        if (order.status == com.example.a2ui_sample.domain.valueobjects.OrderStatus.CANCELLED) {
                            return AgentResponse.Message("❌ Order Cancelled\nOrder ID: ${order.id.value}")
                        }

                        val delivery = deliveryRepository.getDeliveryByOrderId(orderId)
                        if (delivery != null) {
                            AgentResponse.DeliveryUpdate(delivery, order)
                        } else {
                            AgentResponse.Message("I found your order ${order.id.value}, but delivery tracking is not available for it yet. Current status: ${order.status.name}")
                        }
                    } else {
                        AgentResponse.Message("I couldn't find an order with ID $orderIdStr. Please check the ID and try again.")
                    }
                } else {
                    val allOrders = orderRepository.getAllOrders().first()
                    if (allOrders.isNotEmpty()) {
                        val latestOrder = allOrders.first()
                        if (latestOrder.status == com.example.a2ui_sample.domain.valueobjects.OrderStatus.CANCELLED) {
                            return AgentResponse.Message("❌ Order Cancelled\nOrder ID: ${latestOrder.id.value}")
                        }

                        val delivery = deliveryRepository.getDeliveryByOrderId(latestOrder.id)
                        if (delivery != null) {
                            AgentResponse.DeliveryUpdate(delivery, latestOrder)
                        } else {
                            AgentResponse.Message("I found your recent order ${latestOrder.id.value}, but delivery tracking is not available for it yet. Current status: ${latestOrder.status.name}")
                        }
                    } else {
                        AgentResponse.Message("Which order would you like to track? Please provide the Order ID.")
                    }
                }
            }
            com.example.a2ui_sample.domain.model.UserIntent.ORDER_HISTORY -> {
                val history = orderRepository.getOrderHistory(CustomerId("guest"))
                if (history.isNotEmpty()) {
                    AgentResponse.Message("You have ${history.size} past orders. I've opened the Order History for you.")
                } else {
                    AgentResponse.Message("You don't have any past orders yet.")
                }
            }
            com.example.a2ui_sample.domain.model.UserIntent.ORDER_REPEAT -> {
                val allOrders = orderRepository.getAllOrders().first()
                if (allOrders.isNotEmpty()) {
                    val latestOrder = allOrders.first()
                    latestOrder.items.forEach { item ->
                        repeat(item.quantity) {
                            menuRepository.addToCart(item.menuItemId)
                        }
                    }
                    AgentResponse.Message("I've added the items from your last order (${latestOrder.id.value}) back to your cart. You can view your cart or checkout now.")
                } else {
                    AgentResponse.Message("I couldn't find any past orders to repeat.")
                }
            }
            com.example.a2ui_sample.domain.model.UserIntent.ORDER_CANCEL -> {
                val orderIdStr = intent.entities["order_id"] as? String
                if (orderIdStr != null) {
                    val orderId = OrderId(orderIdStr)
                    val order = orderRepository.getOrderById(orderId)
                    if (order != null) {
                        if (order.status == com.example.a2ui_sample.domain.valueobjects.OrderStatus.DELIVERED) {
                            AgentResponse.Message("Sorry, order $orderIdStr has already been delivered and cannot be cancelled.")
                        } else if (order.status == com.example.a2ui_sample.domain.valueobjects.OrderStatus.CANCELLED) {
                            AgentResponse.Message("Order $orderIdStr is already cancelled.")
                        } else {
                            orderRepository.updateOrderStatus(orderId, com.example.a2ui_sample.domain.valueobjects.OrderStatus.CANCELLED)
                            AgentResponse.Message("I've successfully cancelled your order $orderIdStr.")
                        }
                    } else {
                        AgentResponse.Message("I couldn't find order $orderIdStr to cancel.")
                    }
                } else {
                    val allOrders = orderRepository.getAllOrders().first()
                    if (allOrders.isNotEmpty()) {
                        val latestOrder = allOrders.first()
                        if (latestOrder.status != com.example.a2ui_sample.domain.valueobjects.OrderStatus.DELIVERED && 
                            latestOrder.status != com.example.a2ui_sample.domain.valueobjects.OrderStatus.CANCELLED) {
                            orderRepository.updateOrderStatus(latestOrder.id, com.example.a2ui_sample.domain.valueobjects.OrderStatus.CANCELLED)
                            AgentResponse.Message("I've successfully cancelled your most recent order (${latestOrder.id.value}).")
                        } else {
                            AgentResponse.Message("Your most recent order (${latestOrder.id.value}) is already ${latestOrder.status.name.lowercase()} and cannot be cancelled.")
                        }
                    } else {
                        AgentResponse.Message("You don't have any active orders to cancel.")
                    }
                }
            }
            else -> AgentResponse.Message("I can help you track your active orders.")
        }
    }
}
