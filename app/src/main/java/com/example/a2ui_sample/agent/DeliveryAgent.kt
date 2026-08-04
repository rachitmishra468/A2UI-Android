package com.example.a2ui_sample.agent

import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.IntentResult
import com.example.a2ui_sample.domain.model.UserIntent
import com.example.a2ui_sample.domain.repository.MenuRepository

/**
 * DeliveryAgent
 * Specialist for real-time order tracking and delivery status.
 */
class DeliveryAgent(private val repository: MenuRepository) {

    fun execute(intent: IntentResult): AgentResponse {
        return when (intent.intent) {
            com.example.a2ui_sample.domain.model.UserIntent.ORDER_TRACKING -> {
                val orderId = intent.entities["order_id"] as? String
                
                if (orderId != null) {
                    val delivery = repository.getDeliveryStatus(orderId)
                    val order = repository.getCurrentOrders().find { it.id.value == orderId }
                             ?: repository.getPastOrders().find { it.id.value == orderId }

                    if (delivery != null && order != null) {
                        AgentResponse.DeliveryUpdate(delivery, order)
                    } else {
                        AgentResponse.Message("I couldn't find an order with ID $orderId. Please check the ID and try again.")
                    }
                } else {
                    val recentOrders = repository.getCurrentOrders()
                    if (recentOrders.isNotEmpty()) {
                        val latestOrder = recentOrders.first()
                        val delivery = repository.getDeliveryStatus(latestOrder.id.value)
                        if (delivery != null) {
                            AgentResponse.DeliveryUpdate(delivery, latestOrder)
                        } else {
                            AgentResponse.Message("I found your recent order ${latestOrder.id.value}, but delivery tracking is not available for it yet.")
                        }
                    } else {
                        AgentResponse.Message("Which order would you like to track? Please provide the Order ID.")
                    }
                }
            }
            else -> AgentResponse.Message("I can help you track your active orders.")
        }
    }
}
