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
                            AgentResponse.Message("I found your order! 📦 Order ID: ${order.id.value}\nStatus: ${order.status.name}\nDelivery tracking will be available soon!")
                        }
                    } else {
                        AgentResponse.Message("Hmm, I couldn't find order #$orderIdStr. 🤔 Could you double-check the order ID?")
                    }
                } else {
                    val allOrders = orderRepository.getAllOrders().first()
                    if (allOrders.isNotEmpty()) {
                        val latestOrder = allOrders.first()
                        if (latestOrder.status == com.example.a2ui_sample.domain.valueobjects.OrderStatus.CANCELLED) {
                            return AgentResponse.Message("❌ Your latest order was cancelled.\nOrder ID: ${latestOrder.id.value}")
                        }

                        val delivery = deliveryRepository.getDeliveryByOrderId(latestOrder.id)
                        if (delivery != null) {
                            AgentResponse.DeliveryUpdate(delivery, latestOrder)
                        } else {
                            AgentResponse.Message("I found your recent order! 📦 Order ID: ${latestOrder.id.value}\nStatus: ${latestOrder.status.name}\nTracking will be available soon!")
                        }
                    } else {
                        AgentResponse.Message("You don't have any active orders right now. 📦 Would you like to order something delicious?")
                    }
                }
            }
            com.example.a2ui_sample.domain.model.UserIntent.ORDER_HISTORY -> {
                val history = orderRepository.getOrderHistory(CustomerId("guest"))
                if (history.isNotEmpty()) {
                    AgentResponse.Message("You have ${history.size} past orders! 📜 Let me show you your order history.")
                } else {
                    AgentResponse.Message("You don't have any past orders yet. 📦 Ready to place your first order?")
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
                    val total = menuRepository.getCartTotal()
                    AgentResponse.Message("Great choice! 🔄 I've added all items from your last order back to your cart. Total: ₹$total. Ready to checkout?")
                } else {
                    AgentResponse.Message("I couldn't find any past orders to repeat. 🤔 Would you like to browse our menu?")
                }
            }
            com.example.a2ui_sample.domain.model.UserIntent.ORDER_CANCEL -> {
                val orderIdStr = intent.entities["order_id"] as? String
                if (orderIdStr != null) {
                    val orderId = OrderId(orderIdStr)
                    val order = orderRepository.getOrderById(orderId)
                    if (order != null) {
                        if (order.status == com.example.a2ui_sample.domain.valueobjects.OrderStatus.DELIVERED) {
                            AgentResponse.Message("Sorry! 😔 Order #$orderIdStr was already delivered and can't be cancelled. Need help with something else?")
                        } else if (order.status == com.example.a2ui_sample.domain.valueobjects.OrderStatus.CANCELLED) {
                            AgentResponse.Message("This order is already cancelled. ✅ Anything else I can help with?")
                        } else {
                            orderRepository.updateOrderStatus(orderId, com.example.a2ui_sample.domain.valueobjects.OrderStatus.CANCELLED)
                            AgentResponse.Message("Done! ✅ I've cancelled order #$orderIdStr. We hope to serve you again soon! 😊")
                        }
                    } else {
                        AgentResponse.Message("I couldn't find order #$orderIdStr. 🤔 Could you check the order ID?")
                    }
                } else {
                    val allOrders = orderRepository.getAllOrders().first()
                    if (allOrders.isNotEmpty()) {
                        val latestOrder = allOrders.first()
                        if (latestOrder.status != com.example.a2ui_sample.domain.valueobjects.OrderStatus.DELIVERED && 
                            latestOrder.status != com.example.a2ui_sample.domain.valueobjects.OrderStatus.CANCELLED) {
                            orderRepository.updateOrderStatus(latestOrder.id, com.example.a2ui_sample.domain.valueobjects.OrderStatus.CANCELLED)
                            AgentResponse.Message("All done! ✅ I've cancelled your latest order (${latestOrder.id.value}). Hope to see you again soon! 😊")
                        } else {
                            AgentResponse.Message("Your latest order (${latestOrder.id.value}) is already ${latestOrder.status.name.lowercase()} and can't be cancelled. 😔")
                        }
                    } else {
                        AgentResponse.Message("You don't have any active orders to cancel. 📦 Everything looks good!")
                    }
                }
            }
            else -> AgentResponse.Message("I can help you track your orders! 📦 Just ask me about any order ID.")
        }
    }
}
