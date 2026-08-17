package com.example.a2ui_sample.infrastructure.service

import android.util.Log
import com.example.a2ui_sample.domain.repository.OrderRepository
import com.example.a2ui_sample.domain.valueobjects.OrderId
import com.example.a2ui_sample.domain.valueobjects.OrderStatus
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderStatusSimulator @Inject constructor(
    private val orderRepository: OrderRepository,
    private val notificationHelper: NotificationHelper
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeSimulations = mutableMapOf<String, Job>()

    fun startSimulation(orderId: OrderId) {
        if (activeSimulations.containsKey(orderId.value)) return

        val job = scope.launch {
            try {
                // 1. Initial Notification
                notificationHelper.showOrderNotification(
                    orderId.value, 
                    "Order Placed", 
                    "Your delicious meal is on the way! (ID: ${orderId.value.take(8)})"
                )

                // Define the status sequence
                val sequence = listOf(
                    OrderStatus.CONFIRMED to "Restaurant has confirmed your order.",
                    OrderStatus.PREPARING to "Chef is working their magic! 🍳",
                    OrderStatus.READY to "Food is packed and ready for pickup.",
                    OrderStatus.PICKED_UP to "Delivery partner is on their way to you! 🛵",
                    OrderStatus.DELIVERED to "Enjoy your meal! It has been delivered. 😋"
                )

                for ((status, message) in sequence) {
                    delay(30000) // 30 seconds per step for better demo experience (total ~2.5 mins)
                    
                    Log.d("OrderSim", "Updating order ${orderId.value} to $status")
                    orderRepository.updateOrderStatus(orderId, status)
                    
                    val displayStatus = when(status) {
                        OrderStatus.PENDING -> "Order Placed"
                        OrderStatus.CONFIRMED -> "Confirmed"
                        OrderStatus.PREPARING -> "Preparing"
                        OrderStatus.READY, OrderStatus.PICKED_UP -> "Out for Delivery"
                        OrderStatus.DELIVERED -> "Delivered"
                        else -> status.name.lowercase().replaceFirstChar { it.uppercase() }
                    }

                    notificationHelper.showOrderNotification(
                        orderId.value,
                        displayStatus,
                        message
                    )
                    
                    if (status == OrderStatus.DELIVERED) break
                }
            } catch (e: Exception) {
                Log.e("OrderSim", "Simulation failed for ${orderId.value}", e)
            } finally {
                activeSimulations.remove(orderId.value)
            }
        }
        activeSimulations[orderId.value] = job
    }
}
