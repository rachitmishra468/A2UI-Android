package com.example.a2ui_sample.domain.model

import com.example.a2ui_sample.domain.valueobjects.DeliveryId
import com.example.a2ui_sample.domain.valueobjects.OrderId
import com.example.a2ui_sample.domain.valueobjects.DeliveryStatus

/**
 * Delivery Entity
 * Represents the delivery status of an order.
 */
data class Delivery(
    val id: DeliveryId,
    val orderId: OrderId,
    val courierName: String? = null,
    val courierPhone: String? = null,
    val estimatedArrivalAt: Long? = null,
    val actualArrivalAt: Long? = null,
    val status: DeliveryStatus = DeliveryStatus.PENDING,
    val deliveryAddress: String
)
